package io.reconquest.carcosa;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
  private SharedPreferences preferences;
  private UI ui;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.settings);

    ui = new UI(this);

    ui.hide(R.id.settings_saved);

    preferences =
        getBaseContext()
            .getSharedPreferences(getBaseContext().getPackageName(), Context.MODE_PRIVATE);

    ui.text(
        R.id.session_ttl,
        preferences.getLong(
            CarcosaApplication.SESSION_TTL_KEY, CarcosaApplication.SESSION_TTL_VALUE_DEFAULT));

    ui.onClick(
        R.id.save,
        (View v) -> {
          updatePreferences();
        });
  }

  private void updatePreferences() {
    String sessionTTL = ui.text(R.id.session_ttl);

    Editor editor = preferences.edit();
    editor.putLong(
        CarcosaApplication.SESSION_TTL_KEY,
        toLong(sessionTTL, CarcosaApplication.SESSION_TTL_VALUE_DEFAULT));
    editor.commit();

    ui.show(R.id.settings_saved);
  }

  private Long toLong(String value, Long defaultValue) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
