package io.reconquest.carcosa;

import com.bugsnag.android.Bugsnag;

import android.app.Application;

public class CarcosaApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    Bugsnag.init(this);
  }
}
