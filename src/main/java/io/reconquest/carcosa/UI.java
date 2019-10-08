package io.reconquest.carcosa;

import java.util.Arrays;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class UI {
  public interface Searchable {
    public View findViewById(int id);
  }

  Searchable root;
  Handler handler;

  UI(final View view) {
    this(
        new Searchable() {
          public View findViewById(int id) {
            return view.findViewById(id);
          }
        });
  }

  UI(final AppCompatActivity view) {
    this(
        new Searchable() {
          public View findViewById(int id) {
            return view.findViewById(id);
          }
        });
  }

  UI(Searchable root) {
    this.root = root;
    this.handler = new Handler(Looper.getMainLooper());
  }

  void ui(Runnable runnable) {
    handler.post(runnable);
  }

  void onClick(final int id, OnClickListener listener) {
    root.findViewById(id).setOnClickListener(listener);
  }

  void hide(final int id) {
    ui(
        new Runnable() {
          public void run() {
            root.findViewById(id).setVisibility(View.GONE);
          }
        });
  }

  void show(final int id) {
    ui(
        new Runnable() {
          public void run() {
            root.findViewById(id).setVisibility(View.VISIBLE);
          }
        });
  }

  void disable(final int id) {
    ui(
        new Runnable() {
          public void run() {
            root.findViewById(id).setEnabled(false);
          }
        });
  }

  void enable(final int id) {
    ui(
        new Runnable() {
          public void run() {
            root.findViewById(id).setEnabled(true);
          }
        });
  }

  void focus(final int id) {
    ui(
        new Runnable() {
          public void run() {
            root.findViewById(id).requestFocus();
          }
        });
  }

  void animate(final int id, final Animation animation) {
    ui(
        new Runnable() {
          public void run() {
            root.findViewById(id).startAnimation(animation);
          }
        });
  }

  String text(final int id, final Object... params) {
    View view = root.findViewById(id);

    if (view instanceof TextView) {
      final TextView text = (TextView) view;

      if (params.length > 0) {
        ui(
            new Runnable() {
              public void run() {
                switch (params.length) {
                  case 1:
                    text.setText((String) params[0]);
                    break;
                  default:
                    text.setText(
                        String.format(
                            (String) params[0], Arrays.copyOfRange(params, 1, params.length)));
                }
              }
            });
      }

      return text.getText().toString();
    }

    if (view instanceof Spinner) {
      Spinner spinner = (Spinner) view;

      return spinner.getSelectedItem().toString();
    }

    return null;
  }
}
