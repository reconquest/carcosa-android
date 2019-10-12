package io.reconquest.carcosa;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricPrompt.CryptoObject;
import io.reconquest.carcosa.lib.Carcosa;
import io.reconquest.carcosa.lib.ListResult;
import io.reconquest.carcosa.lib.Repo;
import io.reconquest.carcosa.lib.Token;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getName();
  private RepoList repoList;

  private static final String BIOMETRIC_KEY_NAME = "io.reconquest.carcosa";
  private BiometricPrompt biometricPrompt;
  private final BiometricPrompt.PromptInfo biometricPromptInfo =
      new BiometricPrompt.PromptInfo.Builder()
          .setTitle("Carcosa")
          .setDescription("Confirm fingerpring to continue")
          .setNegativeButtonText("Cancel")
          .build();

  private Cipher cipher;
  private CryptoObject cryptoObject;

  private BiometricPrompt.AuthenticationCallback biometricCallback;

  private Handler handler = new Handler();
  private Executor executor =
      new Executor() {
        @Override
        public void execute(Runnable command) {
          handler.post(command);
        }
      };

  private Carcosa carcosa = new Carcosa();

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);

    if (!carcosa.hasState()) {
      this.initBiometrics();
    } else {
      this.initUI();
    }
  }

  private void initCarcosa() {
    byte[] encrypted = null;
    try {
      encrypted = cryptoObject.getCipher().doFinal("carcosa".getBytes(Charset.defaultCharset()));
    } catch (BadPaddingException | IllegalBlockSizeException e) {
      new FatalErrorDialog(this, "Unable to encrypt using internal key", e);
    }

    Maybe<Void> init =
        carcosa.init(
            Paths.get(getApplicationContext().getFilesDir().getPath()).toString(),
            String.valueOf(encrypted));
    if (init.error != null) {
      new FatalErrorDialog(this, init.error).show();
      return;
    }
  }

  private void initUI() {
    setContentView(R.layout.main);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
    toolbar.setSubtitle("secrets");
    setSupportActionBar(toolbar);
    list();
  }

  private void initBiometrics() {
    setContentView(R.layout.login);

    BiometricManager biometricManager = BiometricManager.from(this);
    switch (biometricManager.canAuthenticate()) {
      case BiometricManager.BIOMETRIC_SUCCESS:
        // all good
        break;
      case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
        new FatalErrorDialog(this, "No biometric features available on this device.").show();
        return;
      case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
        new FatalErrorDialog(this, "Biometric features are currently unavailable.").show();
        return;
      case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
        new FatalErrorDialog(
                this, "The user hasn't associated any biometric credentials with their account.")
            .show();
        return;
    }

    final MainActivity activity = this;
    biometricCallback =
        new BiometricPrompt.AuthenticationCallback() {
          @Override
          public void onAuthenticationError(int err, @NonNull CharSequence message) {
            if (err == BiometricPrompt.ERROR_USER_CANCELED) {
              this.onAuthenticationFailed();
              return;
            }

            String msg = String.valueOf(message);
            if (msg.length() == 0) {
              this.onAuthenticationFailed();
              return;
            }

            new FatalErrorDialog(activity, msg).show();
          }

          @Override
          public void onAuthenticationSucceeded(
              @NonNull BiometricPrompt.AuthenticationResult result) {
            final BiometricPrompt.CryptoObject cryptoObject = result.getCryptoObject();
            if (cryptoObject == null) {
              new FatalErrorDialog(activity, "Biometrics: unable to locate internal private key")
                  .show();
            }

            activity.initCarcosa();
            activity.initUI();
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
      new FatalErrorDialog(activity, "Biometrics: unable to retreive internal secret key", e)
          .show();
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
      cryptoObject = new BiometricPrompt.CryptoObject(cipher);
      biometricPrompt.authenticate(biometricPromptInfo, cryptoObject);
    } catch (InvalidKeyException e) {
      new FatalErrorDialog(activity, "Unable to init biometric prompt").show();
      return;
    }

    Button loginButton = (Button) findViewById(R.id.login);
    loginButton.setOnClickListener(
        new OnClickListener() {
          public void onClick(View v) {
            biometricPrompt.authenticate(biometricPromptInfo, cryptoObject);
          }
        });
  }

  protected void list() {
    Maybe<ListResult> list = carcosa.list();
    if (list.error != null) {
      new FatalErrorDialog(this, list.error).show();
    } else {
      repoList = new RepoList(this, list.result.repos);
      ((ListView) findViewById(R.id.repo_list)).setAdapter(repoList);
    }
  }

  class SyncThread extends Thread implements Runnable {
    Activity activity;

    SyncThread(Activity activity) {
      this.activity = activity;
    }

    public void run() {
      UI ui = new UI(activity);

      ui.disable(R.id.toolbar_main_action_sync);

      final RotateAnimation animation =
          new RotateAnimation(
              0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

      animation.setDuration(1000);
      animation.setFillAfter(true);
      animation.setRepeatCount(Animation.INFINITE);

      ui.animate(R.id.toolbar_main_action_sync, animation);

      final Maybe<Void> sync = carcosa.sync();

      ui.enable(R.id.toolbar_main_action_sync);
      ui.ui(
          new Runnable() {
            public void run() {
              animation.cancel();

              if (sync.error != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                builder.setMessage(sync.error).setTitle("Error");

                builder.setNegativeButton(
                    "OK",
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int id) {}
                    });

                builder.create().show();
              } else {
                list();
              }
            }
          });
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.toolbar_main_action_sync:
        new SyncThread(this).start();
        break;
      case R.id.toolbar_main_action_add_repo:
        Intent intent = new Intent(this, RepoActivity.class);
        intent.putExtra("Carcosa", carcosa);
        startActivity(intent);
        break;
      default:
        break;
    }

    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.toolbar_main, menu);
    return true;
  }

  public class RepoList extends BaseAdapter {
    Activity activity;
    ArrayList<Repo> repos;

    RepoList(Activity activity, ArrayList<Repo> repos) {
      this.activity = activity;
      this.repos = repos;
    }

    @Override
    public int getCount() {
      return repos.size();
    }

    @Override
    public Repo getItem(int i) {
      return repos.get(i);
    }

    @Override
    public long getItemId(int i) {
      return getItem(i).name.hashCode();
    }

    @Override
    public View getView(int position, View view, ViewGroup container) {
      if (view == null) {
        view = getLayoutInflater().inflate(R.layout.repo_list_item, container, false);
      }

      UI ui = new UI(view);

      Repo repo = getItem(position);

      ui.text(R.id.repo_list_item_name, repo.name);
      ui.text(R.id.repo_list_item_sync_stat_date, repo.syncStat.date);

      if (repo.syncStat.added > 0) {
        ui.text(
            R.id.repo_list_item_sync_stat_added,
            String.format(Locale.getDefault(), "+%d", repo.syncStat.added));
        ui.show(R.id.repo_list_item_sync_stat_added);
      }

      if (repo.syncStat.deleted > 0) {
        ui.text(
            R.id.repo_list_item_sync_stat_deleted,
            String.format(Locale.getDefault(), "−%d", repo.syncStat.deleted));
        ui.show(R.id.repo_list_item_sync_stat_deleted);
      }

      if (repo.syncStat.added + repo.syncStat.deleted == 0) {
        ui.show(R.id.repo_list_item_sync_stat_uptodate);
      }

      ListView tokensView = (ListView) view.findViewById(R.id.repo_token_list);
      ListAdapter tokensAdapter = new RepoTokenList(activity, repo.tokens);
      ViewGroup.LayoutParams params = tokensView.getLayoutParams();

      params.height = (tokensView.getDividerHeight() * (tokensAdapter.getCount() - 1));

      for (int i = 0; i < tokensAdapter.getCount(); i++) {
        View listItem = tokensAdapter.getView(i, null, tokensView);
        listItem.measure(0, 0);
        params.height += listItem.getMeasuredHeight();
      }

      tokensView.setLayoutParams(params);
      tokensView.setAdapter(tokensAdapter);

      return view;
    }
  }

  public class RepoTokenList extends BaseAdapter {
    ArrayList<Token> tokens;
    Activity activity;

    public class ViewButton implements OnClickListener {
      String secret;
      String token;

      ViewButton(String token, String secret) {
        this.token = token;
        this.secret = secret;
      }

      public void onClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setMessage(secret);

        builder.setNeutralButton(
            "Close",
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {}
            });

        builder.create().show();
      }
    }

    public class CopyButton implements OnClickListener {
      String secret;
      String token;

      CopyButton(String token, String secret) {
        this.token = token;
        this.secret = secret;
      }

      public void onClick(View v) {
        new Clipboard(activity).clip(token, secret, "Secret copied to clipboard.");
      }
    }

    RepoTokenList(Activity activity, ArrayList<Token> tokens) {
      this.activity = activity;
      this.tokens = tokens;
    }

    @Override
    public int getCount() {
      return tokens.size();
    }

    @Override
    public Token getItem(int i) {
      return tokens.get(i);
    }

    @Override
    public long getItemId(int i) {
      return getItem(i).name.hashCode();
    }

    @Override
    public View getView(int position, View view, ViewGroup container) {
      if (view == null) {
        view = getLayoutInflater().inflate(R.layout.repo_token_list_item, container, false);
      }

      UI ui = new UI(view);

      Token token = getItem(position);

      if (token.resource.equals("")) {
        ui.text(R.id.repo_token_list_item_name, token.name);
      } else {
        ui.hide(R.id.repo_token_list_item_name_panel);
        if (!token.login.equals("")) {
          ui.text(R.id.repo_token_list_item_login, token.login);
          ui.show(R.id.repo_token_list_item_login_panel);
        }

        ui.text(R.id.repo_token_list_item_resource, token.resource);
        ui.show(R.id.repo_token_list_item_resource_panel);
      }

      ui.onClick(R.id.repo_token_list_item_view, new ViewButton(token.name, token.payload));
      ui.onClick(R.id.repo_token_list_item_copy, new CopyButton(token.name, token.payload));

      return view;
    }
  }
}
