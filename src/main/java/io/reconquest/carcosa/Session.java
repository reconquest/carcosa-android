package io.reconquest.carcosa;

import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import io.reconquest.carcosa.lib.Carcosa;

public class Session {
  private boolean paused = false;
  private Date pauseDate = null;
  private SharedPreferences preferences;
  private Context context;
  private Carcosa carcosa;
  private SessionResetter resetter;

  Session(Context context, Carcosa carcosa, SessionResetter resetter) {
    this.context = context;
    this.resetter = resetter;
    this.carcosa = carcosa;

    this.preferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
  }

  public void onPause() {
    paused = true;
    pauseDate = new Date();
  }

  public void onResume() {
    // onResume() is also called after onCreate()
    if (!paused) {
      return;
    }

    final long seconds = preferences.getLong(
        CarcosaApplication.SESSION_TTL_KEY, CarcosaApplication.SESSION_TTL_VALUE_DEFAULT);

    Date expireDate = new Date(pauseDate.getTime() + (seconds * 1000));
    Date now = new Date();

    if (now.after(expireDate)) {
      if (carcosa != null) {
        carcosa.destroy();
      }

      if (this.resetter != null) {
        this.resetter.reset();
      }

      Intent intent = new Intent(context, LoginActivity.class);
      context.startActivity(intent);
    }
  }
}
