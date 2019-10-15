package io.reconquest.carcosa;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.Arrays;

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

  UI(final Activity view) {
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

  void readonly(final int id) {
    ui(
        new Runnable() {
          public void run() {
            EditText text = (EditText) root.findViewById(id);
            text.setInputType(InputType.TYPE_NULL);
            text.setKeyListener(null);
          }
        });
  }

  @SuppressWarnings("unchecked")
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
      final Spinner spinner = (Spinner) view;

      if (params.length == 1) {
        ui(
            new Runnable() {
              public void run() {
                spinner.setSelection(
                    ((ArrayAdapter<String>) spinner.getAdapter()).getPosition((String) params[0]));
              }
            });
      }

      return spinner.getSelectedItem().toString();
    }

    return null;
  }
}
