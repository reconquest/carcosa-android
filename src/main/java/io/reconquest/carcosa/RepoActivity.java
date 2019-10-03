package io.reconquest.carcosa;

import java.util.Arrays;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.TextView;

public class RepoActivity extends AppActivity {
  private Carcosa carcosa;

  private String repoID;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.repo);

    carcosa = (Carcosa) getIntent().getSerializableExtra("Carcosa");

    ((Spinner) findViewById(R.id.repo_protocol)).setOnItemSelectedListener(new ProtocolSelect());

    onClick(R.id.repo_ssh_key_generate, new GenerateKeyButton());
    onClick(R.id.repo_connect, new ConnectButton());
    onClick(R.id.repo_unlock, new UnlockButton());
    onClick(R.id.repo_advanced, new AdvancedSettingsPanel());

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_repo);
    toolbar.setSubtitle("add repository");
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  String text(int id, Object... params) {
    View view = findViewById(id);

    if (view instanceof TextView) {
      TextView text = (TextView) view;

      switch (params.length) {
        case 0:
          break;
        case 1:
          text.setText((String) params[0]);
          break;
        default:
          text.setText(
              String.format((String) params[0], Arrays.copyOfRange(params, 1, params.length)));
      }

      return text.getText().toString();
    }

    if (view instanceof Spinner) {
      Spinner spinner = (Spinner) view;

      return spinner.getSelectedItem().toString();
    }

    return "<n/a>";
  }

  void ui(Runnable runnable) {
    new Handler(Looper.getMainLooper()).post(runnable);
  }

  void animate(int id, Animation animation) {
    findViewById(id).startAnimation(animation);
  }

  public class AdvancedSettingsPanel implements OnClickListener {
    Boolean shown;

    AdvancedSettingsPanel() {
      shown = false;
    }

    public void onClick(View v) {
      RotateAnimation animation;

      if (shown) {
        hide(R.id.repo_advanced_panel);
        animation = new RotateAnimation(90, 0, 30, 30);
      } else {
        show(R.id.repo_advanced_panel);
        animation = new RotateAnimation(0, 90, 30, 30);
      }

      animation.setDuration(100);
      animation.setFillAfter(true);
      animate(R.id.repo_advanced_icon, animation);

      shown = !shown;
    }
  }

  public class ProtocolSelect implements OnItemSelectedListener {
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
      String protocol = parent.getItemAtPosition(pos).toString();

      if (protocol.equals("git+ssh")) {
        show(R.id.repo_ssh_key_panel);
      } else {
        hide(R.id.repo_ssh_key_panel);
      }
    }

    public void onNothingSelected(AdapterView<?> parent) {}
  }

  public class GenerateKeyButton implements OnClickListener {
    public void onClick(View v) {
      show(R.id.repo_ssh_key_generate_progress_panel);
      hide(R.id.repo_ssh_key_fingerprint_panel);
      hide(R.id.repo_ssh_key_copy);
      disable(R.id.repo_ssh_key_generate);

      new KeygenThread().start();
    }

    class KeygenThread extends Thread implements Runnable {
      public void run() {
        final Carcosa.SSHKey key = carcosa.keygen();

        ui(new KeygenThreadSuccess(key));
      }
    }

    class KeygenThreadSuccess implements Runnable {
      Carcosa.SSHKey key;

      KeygenThreadSuccess(Carcosa.SSHKey key) {
        this.key = key;
      }

      public void run() {
        hide(R.id.repo_ssh_key_generate_progress_panel);
        show(R.id.repo_ssh_key_fingerprint_panel);
        show(R.id.repo_ssh_key_copy);
        enable(R.id.repo_ssh_key_generate);
        text(R.id.repo_ssh_key_fingerprint, key.fingerprint);
      }
    }
  }

  public class ConnectButton implements OnClickListener {
    class ConnectThreadError implements Runnable {
      String error;

      ConnectThreadError(String error) {
        this.error = error;
      }

      public void run() {
        hide(R.id.repo_connect_progress_panel);
        text(R.id.repo_error, error);
        show(R.id.repo_error);
      }
    }

    class ConnectThreadSuccess implements Runnable {
      Carcosa.ConnectResult result;

      ConnectThreadSuccess(Carcosa.ConnectResult result) {
        this.result = result;
      }

      public void run() {
        hide(R.id.repo_connect_progress_panel);
        show(R.id.repo_stat_panel);
        show(R.id.repo_unlock_panel);
        focus(R.id.repo_master_password);
        hide(R.id.repo_connect);
        text(R.id.repo_stat, "%d secret tokens available.", result.tokens);
      }
    }

    class ConnectThread extends Thread implements Runnable {
      public void run() {
        String protocol = text(R.id.repo_protocol);
        String address = text(R.id.repo_address);
        String namespace = text(R.id.repo_token_namespace);
        String filter = text(R.id.repo_token_filter);

        Maybe<Carcosa.ConnectResult> connect =
            carcosa.connect(protocol, address, namespace, filter);
        if (connect.error != null) {
          ui(new ConnectThreadError(connect.error));
        } else {
          repoID = connect.result.id;
          ui(new ConnectThreadSuccess(connect.result));
        }
      }
    }

    public void onClick(View v) {
      hide(R.id.repo_error);
      show(R.id.repo_connect_progress_panel);
      disable(R.id.repo_protocol);
      disable(R.id.repo_address);
      disable(R.id.repo_token_namespace);
      disable(R.id.repo_connect);

      new ConnectThread().start();
    }
  }

  public class UnlockButton implements OnClickListener {
    public void onClick(View v) {
      disable(R.id.repo_master_password);
      disable(R.id.repo_master_password_cache);
      disable(R.id.repo_unlock);

      String key = text(R.id.repo_master_password);

      Maybe<Carcosa.UnlockResult> unlock = carcosa.unlock(repoID, key, false);
      if (unlock.error != null) {
        text(R.id.repo_error, unlock.error);
        show(R.id.repo_error);
      } else {
        if (unlock.result.tokens == 0) {
          enable(R.id.repo_master_password);
          enable(R.id.repo_master_password_cache);
          enable(R.id.repo_unlock);
          show(R.id.repo_unlock_wrong_master_password);
        } else {
          hide(R.id.repo_unlock_panel);
          show(R.id.repo_unlock_done_panel);
          text(R.id.repo_unlock_done_stat, "%d tokens unlocked!", unlock.result.tokens);
        }
      }
    }
  }
}
