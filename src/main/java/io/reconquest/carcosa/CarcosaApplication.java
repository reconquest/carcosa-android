package io.reconquest.carcosa;

import android.app.Application;
import com.bugsnag.android.Bugsnag;

public class CarcosaApplication extends Application {
  public static String SESSION_TTL_KEY = "session.ttl";
  public static long SESSION_TTL_VALUE_DEFAULT = 10;

  @Override
  public void onCreate() {
    super.onCreate();
    // Bugsnag.init(this);
  }
}
