package io.reconquest.carcosa;

import java.util.Arrays;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

public class UI {
  public interface Searchable {
    public View findViewById(int id);
  }

  public interface OnTextChangedListener {
    void onTextChanged(CharSequence chars, int start, int count, int after);
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
    if (Looper.myLooper() == Looper.getMainLooper()) {
      runnable.run();
    } else {
      handler.post(runnable);
    }
  }

  void onClick(final int id, OnClickListener listener) {
    root.findViewById(id).setOnClickListener(listener);
  }

  void onEdit(final int id, final OnTextChangedListener listener) {
    ((EditText) root.findViewById(id))
        .addTextChangedListener(
            new TextWatcher() {
              @Override
              public void onTextChanged(CharSequence chars, int start, int count, int after) {
                listener.onTextChanged(chars, start, count, after);
              }

              @Override
              public void beforeTextChanged(CharSequence chars, int start, int count, int after) {}

              @Override
              public void afterTextChanged(Editable editable) {}
            });
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
            text.setTextAppearance(R.style.Readonly);
          }
        });
  }

  void disableHelp(final int id) {
    ui(
        new Runnable() {
          public void run() {
            TextInputLayout layout = (TextInputLayout) root.findViewById(id);
            layout.setHelperTextEnabled(false);

            View view = layout.getChildAt(0);
            ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, 0);
            view.setLayoutParams(params);
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

    if (view instanceof RadioGroup) {
      return ((RadioButton)
              root.findViewById(((RadioGroup) root.findViewById(id)).getCheckedRadioButtonId()))
          .getText()
          .toString();
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
