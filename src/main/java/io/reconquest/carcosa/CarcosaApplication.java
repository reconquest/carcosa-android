package io.reconquest.carcosa;

import android.app.Application;
import com.bugsnag.android.Bugsnag;

public class CarcosaApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    // Bugsnag.init(this);
  }
}
