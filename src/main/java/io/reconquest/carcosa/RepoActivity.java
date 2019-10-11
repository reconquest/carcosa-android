package io.reconquest.carcosa;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.concurrent.Executor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import io.reconquest.carcosa.lib.Carcosa;
import io.reconquest.carcosa.lib.ConnectResult;
import io.reconquest.carcosa.lib.SSHKey;
import io.reconquest.carcosa.lib.UnlockResult;

public class RepoActivity extends AppCompatActivity {
  private static final String TAG = RepoActivity.class.getName();
  private static final String BIOMETRIC_KEY_NAME = "carcosa_2";
  private BiometricPrompt biometricPrompt;
  private final BiometricPrompt.PromptInfo biometricPromptInfo =
      new BiometricPrompt.PromptInfo.Builder()
          .setTitle("Confirm your identity")
          .setNegativeButtonText("Cancel")
          .build();

  private BiometricPrompt.AuthenticationCallback biometricCallback;

  private UI ui;
  private Carcosa carcosa;

  private String repoID;
  private SSHKey repoSSHKey;

  private Handler handler = new Handler();
  private Executor executor =
      new Executor() {
        @Override
        public void execute(Runnable command) {
          handler.post(command);
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ui = new UI(this);

    setContentView(R.layout.repo);

    carcosa = (Carcosa) getIntent().getSerializableExtra("Carcosa");

    ((Spinner) findViewById(R.id.repo_protocol)).setOnItemSelectedListener(new ProtocolSelect());

    ui.onClick(R.id.repo_ssh_key_generate, new GenerateKeyButton());
    ui.onClick(R.id.repo_ssh_key_copy, new CopyKeyButton());
    ui.onClick(R.id.repo_connect, new ConnectButton());
    ui.onClick(R.id.repo_unlock, new UnlockButton());
    ui.onClick(R.id.repo_advanced, new AdvancedSettingsPanel());

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_repo);
    toolbar.setSubtitle("add repository");
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    this.initBiometrics();
  }

  private void initBiometrics() {
    BiometricManager biometricManager = BiometricManager.from(this);
    switch (biometricManager.canAuthenticate()) {
      case BiometricManager.BIOMETRIC_SUCCESS:
        Log.d(TAG, "App can authenticate using biometrics.");
        break;
      case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
        Log.d(TAG, "No biometric features available on this device.");
        break;
      case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
        Log.d(TAG, "Biometric features are currently unavailable.");
        break;
      case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
        Log.d(TAG, "The user hasn't associated any biometric credentials with their account.");
        break;
    }

    final Activity activity = this;
    biometricCallback =
        new BiometricPrompt.AuthenticationCallback() {
          @Override
          public void onAuthenticationError(int err, @NonNull CharSequence message) {
            String msg = String.valueOf(message);
            if (msg.length() == 0) {
              this.onAuthenticationFailed();
            } else {
              new FatalErrorDialog(activity, msg).show();
            }
          }

          @Override
          public void onAuthenticationSucceeded(
              @NonNull BiometricPrompt.AuthenticationResult result) {
            final BiometricPrompt.CryptoObject cryptoObject = result.getCryptoObject();
            if (cryptoObject == null) {
              new FatalErrorDialog(activity, "Unable to locate internal private key").show();
            }

            try {
              byte[] encrypted =
                  cryptoObject.getCipher().doFinal("payload".getBytes(Charset.defaultCharset()));
              Log.d(TAG, "Encrypted payload: " + Arrays.toString(encrypted));
            } catch (BadPaddingException | IllegalBlockSizeException e) {
              new FatalErrorDialog(activity, "Unable to use internal private key", e).show();
            }
          }

          @Override
          public void onAuthenticationFailed() {
            biometricPrompt.cancelAuthentication();
          }
        };

    SecretKey secretKey = null;
    try {
      secretKey = BiometricPromptDemoSecretKeyHelper.getSecretKey(BIOMETRIC_KEY_NAME);
    } catch (KeyStoreException
        | CertificateException
        | NoSuchAlgorithmException
        | IOException
        | UnrecoverableKeyException e) {
      new FatalErrorDialog(activity, "Unable to retreive internal secret key", e).show();
      return;
    }

    if (secretKey == null) {
      try {
        BiometricPromptDemoSecretKeyHelper.generateBiometricBoundKey(
            BIOMETRIC_KEY_NAME, true /* invalidatedByBiometricEnrollment */);

        secretKey = BiometricPromptDemoSecretKeyHelper.getSecretKey(BIOMETRIC_KEY_NAME);
      } catch (InvalidAlgorithmParameterException
          | CertificateException
          | IOException
          | KeyStoreException
          | UnrecoverableKeyException
          | NoSuchAlgorithmException
          | NoSuchProviderException e) {
        new FatalErrorDialog(activity, "Unable to generate internal secret key", e).show();
        return;
      }
    }

    if (secretKey == null) {
      new FatalErrorDialog(activity, "Bug: unable to get internal secret key").show();
      return;
    }

    Cipher cipher = null;
    try {
      cipher = BiometricPromptDemoSecretKeyHelper.getCipher();
    } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
      new FatalErrorDialog(activity, "Unable to get internal cipher", e).show();
      return;
    }

    if (cipher == null) {
      new FatalErrorDialog(activity, "Bug: unable to get internal cipher").show();
      return;
    }

    biometricPrompt = new BiometricPrompt(this, executor, biometricCallback);

    try {
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      biometricPrompt.authenticate(biometricPromptInfo, new BiometricPrompt.CryptoObject(cipher));
    } catch (InvalidKeyException e) {
      new FatalErrorDialog(activity, "Unable to init biometric prompt").show();
      return;
    }
  }

  public class AdvancedSettingsPanel implements OnClickListener {
    Boolean shown;

    AdvancedSettingsPanel() {
      shown = false;
    }

    public void onClick(View v) {
      RotateAnimation animation;

      if (shown) {
        ui.hide(R.id.repo_advanced_panel);
        animation =
            new RotateAnimation(
                90, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
      } else {
        ui.show(R.id.repo_advanced_panel);
        animation =
            new RotateAnimation(
                0, 90, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
      }

      animation.setDuration(100);
      animation.setFillAfter(true);
      ui.animate(R.id.repo_advanced_icon, animation);

      shown = !shown;
    }
  }

  public class ProtocolSelect implements OnItemSelectedListener {
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
      String protocol = parent.getItemAtPosition(pos).toString();

      if (protocol.equals("ssh")) {
        ui.show(R.id.repo_ssh_key_panel);
      } else {
        ui.hide(R.id.repo_ssh_key_panel);
      }
    }

    public void onNothingSelected(AdapterView<?> parent) {}
  }

  public class CopyKeyButton implements OnClickListener {
    public void onClick(View v) {
      ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
      ClipData clip = ClipData.newPlainText(repoSSHKey.fingerprint, repoSSHKey.publicKey);
      clipboard.setPrimaryClip(clip);

      Toast toast =
          Toast.makeText(
              getApplicationContext(), "Public key copied to clipboard.", Toast.LENGTH_LONG);
      toast.show();
    }
  }

  public class GenerateKeyButton implements OnClickListener {
    public void onClick(View v) {
      ui.show(R.id.repo_ssh_key_generate_progress_panel);
      ui.hide(R.id.repo_ssh_key_fingerprint_panel);
      ui.hide(R.id.repo_ssh_key_copy);
      ui.disable(R.id.repo_ssh_key_generate);

      new KeygenThread().start();
    }

    class KeygenThread extends Thread implements Runnable {
      public void run() {
        Maybe<SSHKey> keygen = carcosa.keygen();

        if (keygen.error != null) {
          ui.hide(R.id.repo_ssh_key_generate_progress_panel);
          ui.text(R.id.repo_error, keygen.error);
          ui.show(R.id.repo_error);
        } else {
          ui.hide(R.id.repo_ssh_key_generate_progress_panel);
          ui.show(R.id.repo_ssh_key_fingerprint_panel);
          ui.show(R.id.repo_ssh_key_copy);
          ui.enable(R.id.repo_ssh_key_generate);
          ui.text(R.id.repo_ssh_key_fingerprint, keygen.result.fingerprint);

          repoSSHKey = keygen.result;
        }
      }
    }
  }

  public class ConnectButton implements OnClickListener {
    class ConnectThread extends Thread implements Runnable {
      public void run() {
        String protocol = ui.text(R.id.repo_protocol);
        String address = ui.text(R.id.repo_address);
        String namespace = ui.text(R.id.repo_token_namespace);

        Maybe<ConnectResult> connect = carcosa.connect(protocol, address, namespace, repoSSHKey);
        if (connect.error != null) {
          ui.enable(R.id.repo_protocol);
          ui.enable(R.id.repo_address);
          ui.enable(R.id.repo_token_namespace);
          ui.enable(R.id.repo_connect);

          ui.hide(R.id.repo_connect_progress_panel);
          ui.text(R.id.repo_error, connect.error);
          ui.show(R.id.repo_error);
        } else {
          repoID = connect.result.id;
          ui.hide(R.id.repo_connect_progress_panel);
          ui.show(R.id.repo_stat_panel);
          ui.show(R.id.repo_unlock_panel);
          ui.focus(R.id.repo_master_password);
          ui.hide(R.id.repo_connect);
          ui.text(R.id.repo_stat, "%d secret tokens available.", connect.result.tokens);
        }
      }
    }

    public void onClick(View v) {
      ui.hide(R.id.repo_error);
      ui.show(R.id.repo_connect_progress_panel);
      ui.disable(R.id.repo_protocol);
      ui.disable(R.id.repo_address);
      ui.disable(R.id.repo_token_namespace);
      ui.disable(R.id.repo_connect);

      new ConnectThread().start();
    }
  }

  public class UnlockButton implements OnClickListener {
    public void onClick(View v) {
      ui.disable(R.id.repo_master_password);
      ui.disable(R.id.repo_unlock);

      String key = ui.text(R.id.repo_master_password);
      String filter = ui.text(R.id.repo_token_filter);

      Maybe<UnlockResult> unlock = carcosa.unlock(repoID, key, filter, true);
      if (unlock.error != null) {
        ui.text(R.id.repo_error, unlock.error);
        ui.show(R.id.repo_error);
      } else {
        if (unlock.result.tokens == 0) {
          ui.enable(R.id.repo_master_password);
          ui.enable(R.id.repo_unlock);
          ui.show(R.id.repo_unlock_wrong_master_password);
        } else {
          ui.hide(R.id.repo_unlock_panel);
          ui.show(R.id.repo_unlock_done_panel);
          ui.text(R.id.repo_unlock_done_stat, "%d tokens unlocked!", unlock.result.tokens);
        }
      }
    }
  }
}
