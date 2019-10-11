package io.reconquest.carcosa;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

public class Clipboard {
  Context context;
  ClipboardManager manager;

  Clipboard(Activity activity) {
    this.manager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
    this.context = activity.getApplicationContext();
  }

  public void clip(String label, String data, String message) {
    ClipData clip = ClipData.newPlainText(label, data);
    manager.setPrimaryClip(clip);

    Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
    toast.show();
  }
}
