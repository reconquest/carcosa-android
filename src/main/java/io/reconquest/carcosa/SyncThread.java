package io.reconquest.carcosa;

import android.content.DialogInterface;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import androidx.appcompat.app.AlertDialog;
import io.reconquest.carcosa.lib.Carcosa;

interface Lister {
  public void list();
}

class SyncThread extends Thread implements Runnable {
  private MainActivity activity;
  private Carcosa carcosa;

  SyncThread(MainActivity activity, Carcosa carcosa) {
    this.activity = activity;
    this.carcosa = carcosa;
  }

  public void run() {
    UI ui = new UI(activity);

    ui.disable(R.id.toolbar_main_action_sync);

    final RotateAnimation animation =
        new RotateAnimation(
            0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

    animation.setDuration(1000);
    animation.setFillAfter(true);
    animation.setRepeatCount(Animation.INFINITE);

    ui.animate(R.id.toolbar_main_action_sync, animation);

    final Maybe<Void> sync = carcosa.sync();

    ui.enable(R.id.toolbar_main_action_sync);
    ui.ui(
        new Runnable() {
          public void run() {
            animation.cancel();

            if (sync.error != null) {
              AlertDialog.Builder builder = new AlertDialog.Builder(activity);

              builder.setMessage(sync.error).setTitle("Error");

              builder.setNegativeButton(
                  "OK",
                  new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                  });

              builder.create().show();
            } else {
              activity.list();
            }
          }
        });
  }
}
