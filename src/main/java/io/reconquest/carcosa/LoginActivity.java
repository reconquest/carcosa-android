package io.reconquest.carcosa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import co.infinum.goldfinger.Goldfinger;

public class LoginActivity extends AppCompatActivity {
  // private static final String TAG = LoginActivity.class.getName();
  private UI ui;
  private Goldfinger goldfinger;
  private boolean prompt;

  private String KEY_NAME = "io.reconquest.carcosa.key.pin";

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);

    setContentView(R.layout.login);

    goldfinger = new Goldfinger.Builder(this).logEnabled(BuildConfig.DEBUG).build();

    ui = new UI(this);

    prompt = false;

    bindView();
    authenticate();
  }

  protected void onResume() {
    super.onResume();
    if (!prompt) {
      ui.show(R.id.login);
    }
  }

  private void authenticate() {
    if (!goldfinger.canAuthenticate()) {
      new FatalErrorDialog(this, "The device does not support biometric authentication").show();
      return;
    }

    prompt = true;
    ui.hide(R.id.login);
    ui.show(R.id.login_progress);

    if (getPinFile().exists()) {
      this.decryptPin();
    } else {
      this.resetPin();
    }
  }

  private void bindView() {
    Button loginButton = (Button) findViewById(R.id.login);
    loginButton.setOnClickListener(
        new OnClickListener() {
          public void onClick(View v) {
            authenticate();
          }
        });
  }

  private void switchActivity(String pin) {
    Intent intent = new Intent(this, MainActivity.class);
    intent.putExtra("pin", pin);
    startActivity(intent);
  }

  private Goldfinger.PromptParams buildPromptParams() {
    return new Goldfinger.PromptParams.Builder(this)
        .title("Carcosa")
        .negativeButtonText("Cancel")
        .description("Confirm fignerprint to continue")
        .build();
  }

  private File getPinFile() {
    return new File(getApplicationContext().getFilesDir(), "pin");
  }

  private void decryptPin() {
    try {
      goldfinger.decrypt(
          buildPromptParams(),
          KEY_NAME,
          this.readEncryptedPin(),
          new Goldfinger.Callback() {
            @Override
            public void onError(@NonNull Exception e) {
              onFatalError(e);
            }

            @Override
            public void onResult(@NonNull Goldfinger.Result result) {
              if (result.type() != Goldfinger.Type.SUCCESS) {
                if (result.type() == Goldfinger.Type.ERROR) {
                  ui.show(R.id.login);
                  ui.hide(R.id.login_progress);
                }

                return;
              }

              prompt = false;

              String pin = result.value();
              switchActivity(pin);
            }
          });
    } catch (Exception e) {
      onFatalError(e);
    }
  }

  private void resetPin() {
    try {
      final String pin = this.generateRandomPin();

      goldfinger.encrypt(
          buildPromptParams(),
          KEY_NAME,
          pin,
          new Goldfinger.Callback() {
            @Override
            public void onError(@NonNull Exception e) {
              onFatalError(e);
            }

            @Override
            public void onResult(@NonNull Goldfinger.Result result) {
              // onGoldfingerResult(result);
              if (result.type() != Goldfinger.Type.SUCCESS) {
                if (result.type() == Goldfinger.Type.ERROR) {
                  ui.show(R.id.login);
                  ui.hide(R.id.login_progress);
                }
                return;
              }

              prompt = false;

              try {
                writeEncryptedPin(result.value());
              } catch (IOException e) {
                onFatalError(e);
              }

              switchActivity(pin);
            }
          });
    } catch (Exception e) {
      onFatalError(e);
    }
  }

  private void writeEncryptedPin(String pin) throws IOException {
    Files.write(
        getPinFile().toPath(),
        pin.getBytes(),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }

  private String readEncryptedPin() throws IOException {
    byte[] data = Files.readAllBytes(getPinFile().toPath());
    String pin = new String(data);
    return pin;
  }

  private String generateRandomPin() throws NoSuchAlgorithmException {
    byte[] payload = new byte[16];
    SecureRandom.getInstanceStrong().nextBytes(payload);
    return Base64.encodeToString(payload, Base64.DEFAULT);
  }

  @Override
  protected void onStop() {
    super.onStop();
    goldfinger.cancel();
  }

  private void onFatalError(@NonNull Exception e) {
    new FatalErrorDialog(this, e.getMessage(), e).show();
  }

  @SuppressWarnings("unused")
  private void onGoldfingerResult(Goldfinger.Result result) {
    // System.err.printf("XXXXXXX goldfinger result.type() %s \n", result.type());
    // System.err.printf("XXXXXXX goldfinger result.reason() %s \n", result.reason());
    // System.err.printf("XXXXXXX goldfinger result.value() %s \n", result.value());
    // System.err.printf("XXXXXXX goldfinger result.message() %s \n", result.message());
  }
}
