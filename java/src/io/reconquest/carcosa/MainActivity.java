package io.reconquest.carcosa;

import android.app.Activity;
import android.os.Bundle;
import io.reconquest.carcosa.Carcosa;

public class MainActivity extends Activity {
  private Carcosa carcosa;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    carcosa = new Carcosa();
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    carcosa.sync();
  }
}
