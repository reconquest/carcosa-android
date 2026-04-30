package io.reconquest.carcosa;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import co.infinum.goldfinger.Goldfinger;

public class LoginActivity extends AppCompatActivity {
  private static final String KEY_NAME = "io.reconquest.carcosa.key.pin";

  private static final String FALLBACK_PIN_FILE = "pin.fallback";
  private static final String FALLBACK_PIN_PREFS = "pin.fallback";
  private static final String FALLBACK_PIN_MAGIC = "carcosa-pin-v1";
  private static final int FALLBACK_PIN_MIN_DIGITS = 8;
  private static final int FALLBACK_PIN_SALT_BYTES = 16;
  private static final int FALLBACK_PIN_IV_BYTES = 12;
  private static final int FALLBACK_PIN_KEY_BITS = 256;
  private static final int FALLBACK_PIN_GCM_TAG_BITS = 128;
  private static final int FALLBACK_PIN_PBKDF2_ITERATIONS = 600000;
  private static final int FALLBACK_PIN_MIN_PBKDF2_ITERATIONS = 100000;
  private static final int PIN_FAILURES_BEFORE_LOCK = 5;
  private static final long PIN_LOCK_BASE_MS = TimeUnit.SECONDS.toMillis(30);
  private static final long PIN_LOCK_MAX_MS = TimeUnit.MINUTES.toMillis(15);

  private UI ui;
  private Goldfinger goldfinger;
  private boolean prompt;
  private boolean pinFallback;
  private boolean pinFallbackSetup;
  private Handler handler;
  private Runnable lockoutTicker;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);

    setContentView(R.layout.login);

    goldfinger = new Goldfinger.Builder(this).logEnabled(BuildConfig.DEBUG).build();

    ui = new UI(this);
    handler = new Handler(Looper.getMainLooper());

    prompt = false;

    bindView();
    authenticate();
  }

  protected void onResume() {
    super.onResume();

    if (pinFallback) {
      renderPinFallbackState();
      return;
    }

    if (!prompt) {
      ui.show(R.id.login);
    }
  }

  private void authenticate() {
    if (!goldfinger.canAuthenticate()
        || (!getPinFile().exists() && getFallbackPinFile().exists())) {
      showPinFallback();
      return;
    }

    pinFallback = false;
    prompt = true;
    ui.hide(R.id.pin_form);
    ui.hide(R.id.login);
    ui.show(R.id.login_progress);
    ui.text(R.id.login, "AUTHENTICATE");
    ui.text(R.id.login_status, "encrypted pin cache requires live biometric session");

    if (getPinFile().exists()) {
      this.decryptPin();
    } else {
      this.resetPin();
    }
  }

  private void bindView() {
    Button loginButton = (Button) findViewById(R.id.login);
    loginButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        if (pinFallback) {
          submitPinFallback();
          return;
        }

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
        .description("Confirm fingerprint to continue")
        .build();
  }

  private File getPinFile() {
    return new File(getApplicationContext().getFilesDir(), "pin");
  }

  private File getFallbackPinFile() {
    return new File(getApplicationContext().getFilesDir(), FALLBACK_PIN_FILE);
  }

  private SharedPreferences getFallbackPinPreferences() {
    return getSharedPreferences(FALLBACK_PIN_PREFS, MODE_PRIVATE);
  }

  private void decryptPin() {
    try {
      goldfinger.decrypt(
          buildPromptParams(), KEY_NAME, this.readEncryptedPin(), new Goldfinger.Callback() {
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

      goldfinger.encrypt(buildPromptParams(), KEY_NAME, pin, new Goldfinger.Callback() {
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
    writePrivateFile("pin", pin.getBytes(StandardCharsets.UTF_8));
  }

  private String readEncryptedPin() throws IOException {
    byte[] data = Files.readAllBytes(getPinFile().toPath());
    return new String(data, StandardCharsets.UTF_8);
  }

  private void showPinFallback() {
    pinFallback = true;
    pinFallbackSetup = !getFallbackPinFile().exists();
    prompt = false;

    clearPinInputs();
    clearPinError();
    ui.hide(R.id.login_progress);
    ui.show(R.id.pin_form);
    ui.show(R.id.login);
    ui.text(R.id.login_status, "biometric auth unavailable; numeric PIN fallback active");

    renderPinFallbackState();
    ui.focus(R.id.pin_code);
  }

  private void renderPinFallbackState() {
    if (!pinFallback) {
      return;
    }

    cancelLockoutTicker();
    ui.hide(R.id.login_progress);
    ui.show(R.id.pin_form);
    ui.show(R.id.login);

    if (pinFallbackSetup) {
      ui.text(
          R.id.pin_hint,
          "Create a numeric PIN with at least %d digits. It will unlock this app on devices without biometrics.",
          FALLBACK_PIN_MIN_DIGITS);
      ui.text(R.id.login, "CREATE PIN");
      ui.show(R.id.pin_confirm);
      ui.enable(R.id.login);
      ui.enable(R.id.pin_code);
      ui.enable(R.id.pin_confirm);
      return;
    }

    ui.text(R.id.pin_hint, "Enter your carcosa PIN to unlock the local session cache.");
    ui.text(R.id.login, "UNLOCK");
    ui.hide(R.id.pin_confirm);

    long remaining = pinLockoutRemainingMillis();
    if (remaining > 0) {
      ui.disable(R.id.login);
      ui.disable(R.id.pin_code);
      showPinError("Too many failed attempts. Try again in " + formatLockout(remaining) + ".");
      scheduleLockoutTicker(remaining);
      return;
    }

    clearExpiredLockoutError();
    ui.enable(R.id.login);
    ui.enable(R.id.pin_code);
  }

  private void submitPinFallback() {
    if (!pinFallback) {
      authenticate();
      return;
    }

    if (!pinFallbackSetup && pinLockoutRemainingMillis() > 0) {
      renderPinFallbackState();
      return;
    }

    final String pin = ui.text(R.id.pin_code).trim();
    String validation = validateFallbackPin(pin);
    if (validation != null) {
      showPinError(validation);
      return;
    }

    if (pinFallbackSetup) {
      final String confirmation = ui.text(R.id.pin_confirm).trim();
      if (!pin.equals(confirmation)) {
        showPinError("PIN confirmation does not match.");
        return;
      }
    }

    clearPinError();
    setPinFallbackBusy(true);

    final boolean setup = pinFallbackSetup;
    new Thread(new Runnable() {
      public void run() {
        try {
          final String vaultPin;
          if (setup) {
            vaultPin = generateRandomPin();
            writeFallbackPin(pin, vaultPin);
          } else {
            vaultPin = readFallbackPin(pin);
          }

          clearPinFailures();
          runOnUiThread(new Runnable() {
            public void run() {
              prompt = false;
              switchActivity(vaultPin);
            }
          });
        } catch (InvalidFallbackPinException e) {
          runOnUiThread(new Runnable() {
            public void run() {
              handleInvalidFallbackPin();
            }
          });
        } catch (final Exception e) {
          runOnUiThread(new Runnable() {
            public void run() {
              onFatalError(e);
            }
          });
        }
      }
    }, "pin-fallback-auth").start();
  }

  private String validateFallbackPin(String pin) {
    if (pin.length() < FALLBACK_PIN_MIN_DIGITS) {
      return String.format(
          Locale.ROOT, "PIN must contain at least %d digits.", FALLBACK_PIN_MIN_DIGITS);
    }

    for (int i = 0; i < pin.length(); i++) {
      char digit = pin.charAt(i);
      if (digit < '0' || digit > '9') {
        return "PIN must contain digits only.";
      }
    }

    return null;
  }

  private void setPinFallbackBusy(boolean busy) {
    prompt = busy;

    if (busy) {
      ui.hide(R.id.login);
      ui.show(R.id.login_progress);
      ui.disable(R.id.pin_code);
      ui.disable(R.id.pin_confirm);
      return;
    }

    ui.hide(R.id.login_progress);
    ui.show(R.id.login);
    ui.enable(R.id.pin_code);
    ui.enable(R.id.pin_confirm);
  }

  private void handleInvalidFallbackPin() {
    long remaining = recordPinFailure();
    ui.text(R.id.pin_code, "");
    setPinFallbackBusy(false);
    renderPinFallbackState();

    if (remaining <= 0) {
      showPinError("Invalid PIN.");
    }
  }

  private void clearPinInputs() {
    ui.text(R.id.pin_code, "");
    ui.text(R.id.pin_confirm, "");
  }

  private void showPinError(String error) {
    ui.text(R.id.pin_error, error);
    ui.show(R.id.pin_error);
  }

  private void clearPinError() {
    ui.text(R.id.pin_error, "");
    ui.hide(R.id.pin_error);
  }

  private void clearExpiredLockoutError() {
    String error = ui.text(R.id.pin_error);
    if (error.startsWith("Too many failed attempts.")) {
      clearPinError();
    }
  }

  private long recordPinFailure() {
    SharedPreferences preferences = getFallbackPinPreferences();
    int attempts = preferences.getInt("failed_attempts", 0) + 1;
    long lockedUntil = 0;

    if (attempts >= PIN_FAILURES_BEFORE_LOCK) {
      int lockStep = Math.min(attempts - PIN_FAILURES_BEFORE_LOCK, 8);
      long delay = Math.min(PIN_LOCK_MAX_MS, PIN_LOCK_BASE_MS << lockStep);
      lockedUntil = System.currentTimeMillis() + delay;
    }

    preferences
        .edit()
        .putInt("failed_attempts", attempts)
        .putLong("locked_until", lockedUntil)
        .apply();

    return Math.max(0, lockedUntil - System.currentTimeMillis());
  }

  private void clearPinFailures() {
    getFallbackPinPreferences()
        .edit()
        .putInt("failed_attempts", 0)
        .putLong("locked_until", 0)
        .apply();
  }

  private long pinLockoutRemainingMillis() {
    if (pinFallbackSetup) {
      return 0;
    }

    long lockedUntil = getFallbackPinPreferences().getLong("locked_until", 0);
    return Math.max(0, lockedUntil - System.currentTimeMillis());
  }

  private void scheduleLockoutTicker(long remaining) {
    long delay = Math.min(remaining, TimeUnit.SECONDS.toMillis(1));
    lockoutTicker = new Runnable() {
      public void run() {
        renderPinFallbackState();
      }
    };
    handler.postDelayed(lockoutTicker, delay);
  }

  private void cancelLockoutTicker() {
    if (lockoutTicker == null) {
      return;
    }

    handler.removeCallbacks(lockoutTicker);
    lockoutTicker = null;
  }

  private String formatLockout(long millis) {
    long seconds = TimeUnit.MILLISECONDS.toSeconds(millis + 999);
    if (seconds < 60) {
      return seconds + "s";
    }

    long minutes = (seconds + 59) / 60;
    return minutes + "m";
  }

  private void writeFallbackPin(String userPin, String vaultPin)
      throws IOException, GeneralSecurityException, NoSuchAlgorithmException {
    byte[] salt = randomBytes(FALLBACK_PIN_SALT_BYTES);
    byte[] iv = randomBytes(FALLBACK_PIN_IV_BYTES);
    SecretKeySpec key = deriveFallbackKey(userPin, salt, FALLBACK_PIN_PBKDF2_ITERATIONS);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(FALLBACK_PIN_GCM_TAG_BITS, iv));
    cipher.updateAAD(FALLBACK_PIN_MAGIC.getBytes(StandardCharsets.UTF_8));
    byte[] ciphertext = cipher.doFinal(vaultPin.getBytes(StandardCharsets.UTF_8));

    String payload = String.join(
        "\n",
        FALLBACK_PIN_MAGIC,
        Integer.toString(FALLBACK_PIN_PBKDF2_ITERATIONS),
        encodeBase64(salt),
        encodeBase64(iv),
        encodeBase64(ciphertext));

    writePrivateFile(FALLBACK_PIN_FILE, payload.getBytes(StandardCharsets.UTF_8));
  }

  private void writePrivateFile(String name, byte[] payload) throws IOException {
    try (FileOutputStream output = openFileOutput(name, MODE_PRIVATE)) {
      output.write(payload);
    }
  }

  private String readFallbackPin(String userPin)
      throws IOException, GeneralSecurityException, InvalidFallbackPinException {
    FallbackPinPayload payload = readFallbackPinPayload();
    SecretKeySpec key = deriveFallbackKey(userPin, payload.salt, payload.iterations);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(
        Cipher.DECRYPT_MODE,
        key,
        new GCMParameterSpec(FALLBACK_PIN_GCM_TAG_BITS, payload.iv));
    cipher.updateAAD(FALLBACK_PIN_MAGIC.getBytes(StandardCharsets.UTF_8));

    try {
      byte[] plaintext = cipher.doFinal(payload.ciphertext);
      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (AEADBadTagException e) {
      throw new InvalidFallbackPinException();
    }
  }

  private FallbackPinPayload readFallbackPinPayload() throws IOException {
    String data = new String(Files.readAllBytes(getFallbackPinFile().toPath()), StandardCharsets.UTF_8);
    String[] fields = data.split("\n", -1);
    if (fields.length != 5 || !FALLBACK_PIN_MAGIC.equals(fields[0])) {
      throw new IOException("Invalid PIN fallback file");
    }

    int iterations;
    try {
      iterations = Integer.parseInt(fields[1]);
    } catch (NumberFormatException e) {
      throw new IOException("Invalid PIN fallback file", e);
    }

    if (iterations < FALLBACK_PIN_MIN_PBKDF2_ITERATIONS) {
      throw new IOException("PIN fallback file uses too few key derivation iterations");
    }

    byte[] salt;
    byte[] iv;
    byte[] ciphertext;
    try {
      salt = decodeBase64(fields[2]);
      iv = decodeBase64(fields[3]);
      ciphertext = decodeBase64(fields[4]);
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid PIN fallback file", e);
    }

    if (salt.length < FALLBACK_PIN_SALT_BYTES
        || iv.length != FALLBACK_PIN_IV_BYTES
        || ciphertext.length == 0) {
      throw new IOException("Invalid PIN fallback file");
    }

    return new FallbackPinPayload(iterations, salt, iv, ciphertext);
  }

  private SecretKeySpec deriveFallbackKey(String pin, byte[] salt, int iterations)
      throws GeneralSecurityException {
    PBEKeySpec spec = new PBEKeySpec(
        pin.toCharArray(),
        salt,
        iterations,
        FALLBACK_PIN_KEY_BITS);

    byte[] key = null;
    try {
      key = SecretKeyFactory
          .getInstance("PBKDF2WithHmacSHA256")
          .generateSecret(spec)
          .getEncoded();
      return new SecretKeySpec(key, "AES");
    } finally {
      spec.clearPassword();
      if (key != null) {
        Arrays.fill(key, (byte) 0);
      }
    }
  }

  private String encodeBase64(byte[] data) {
    return Base64.encodeToString(data, Base64.NO_WRAP);
  }

  private byte[] decodeBase64(String data) {
    return Base64.decode(data, Base64.DEFAULT);
  }

  private String generateRandomPin() throws NoSuchAlgorithmException {
    return encodeBase64(randomBytes(32));
  }

  private byte[] randomBytes(int size) throws NoSuchAlgorithmException {
    byte[] payload = new byte[size];
    SecureRandom.getInstanceStrong().nextBytes(payload);
    return payload;
  }

  @Override
  protected void onStop() {
    super.onStop();
    cancelLockoutTicker();
    goldfinger.cancel();
  }

  private void onFatalError(@NonNull Exception e) {
    new FatalErrorDialog(this, e.getMessage(), e).show();
  }

  private static class InvalidFallbackPinException extends Exception {}

  private static class FallbackPinPayload {
    final int iterations;
    final byte[] salt;
    final byte[] iv;
    final byte[] ciphertext;

    FallbackPinPayload(int iterations, byte[] salt, byte[] iv, byte[] ciphertext) {
      this.iterations = iterations;
      this.salt = salt;
      this.iv = iv;
      this.ciphertext = ciphertext;
    }
  }
}
