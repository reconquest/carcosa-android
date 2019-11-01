package io.reconquest.carcosa;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

public class FatalErrorDialog {
  AlertDialog dialog;

  FatalErrorDialog(final Activity activity, Exception e) {
    Log.e(activity.getClass().getName(), "fatal exception", e);
    init(activity, e.getMessage());
  }

  FatalErrorDialog(final Activity activity, String error, Exception e) {
    Log.e(activity.getClass().getName(), "fatal error: " + error, e);
    init(activity, error);
  }

  FatalErrorDialog(final Activity activity, String error) {
    Log.e(activity.getClass().getName(), "fatal error: " + error);

    init(activity, error);
  }

  private void init(final Activity activity, String error) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    builder.setMessage(error).setTitle("Fatal Error");

    builder.setNeutralButton("Exit App", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
      }
    });

    this.dialog = builder.create();
  }

  public void show() {
    dialog.show();
  }
}
