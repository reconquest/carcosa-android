package io.reconquest.carcosa;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

public class FatalErrorDialog {
  AlertDialog dialog;

  FatalErrorDialog(final Activity activity, String error) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    builder.setMessage(error).setTitle("Fatal Error");

    builder.setNeutralButton(
        "Exit App",
        new DialogInterface.OnClickListener() {
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
