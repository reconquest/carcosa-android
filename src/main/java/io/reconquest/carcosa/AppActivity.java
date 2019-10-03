package io.reconquest.carcosa;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

public class AppActivity extends AppCompatActivity {
  void onClick(int id, OnClickListener listener) {
    findViewById(id).setOnClickListener(listener);
  }

  void hide(int id) {
    findViewById(id).setVisibility(View.GONE);
  }

  void show(int id) {
    findViewById(id).setVisibility(View.VISIBLE);
  }

  void disable(int id) {
    findViewById(id).setEnabled(false);
  }

  void enable(int id) {
    findViewById(id).setEnabled(true);
  }

  void focus(int id) {
    if (findViewById(id).requestFocus()) {
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
  }
}
