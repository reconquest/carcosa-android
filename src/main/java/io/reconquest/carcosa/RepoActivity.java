package io.reconquest.carcosa;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import io.reconquest.carcosa.lib.*;

public class RepoActivity extends AppCompatActivity implements UI.Searchable {
  private UI ui;
  private Carcosa carcosa;

  private String repoID;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ui = new UI(this);

    setContentView(R.layout.repo);

    carcosa = (Carcosa) getIntent().getSerializableExtra("Carcosa");

    ((Spinner) findViewById(R.id.repo_protocol)).setOnItemSelectedListener(new ProtocolSelect());

    ui.onClick(R.id.repo_ssh_key_generate, new GenerateKeyButton());
    ui.onClick(R.id.repo_connect, new ConnectButton());
    ui.onClick(R.id.repo_unlock, new UnlockButton());
    ui.onClick(R.id.repo_advanced, new AdvancedSettingsPanel());

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_repo);
    toolbar.setSubtitle("add repository");
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  public class AdvancedSettingsPanel implements OnClickListener {
    Boolean shown;

    AdvancedSettingsPanel() {
      shown = false;
    }

    public void onClick(View v) {
      RotateAnimation animation;

      if (shown) {
        ui.hide(R.id.repo_advanced_panel);
        animation = new RotateAnimation(90, 0, 30, 30);
      } else {
        ui.show(R.id.repo_advanced_panel);
        animation = new RotateAnimation(0, 90, 30, 30);
      }

      animation.setDuration(100);
      animation.setFillAfter(true);
      ui.animate(R.id.repo_advanced_icon, animation);

      shown = !shown;
    }
  }

  public class ProtocolSelect implements OnItemSelectedListener {
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
      String protocol = parent.getItemAtPosition(pos).toString();

      if (protocol.equals("git+ssh")) {
        ui.show(R.id.repo_ssh_key_panel);
      } else {
        ui.hide(R.id.repo_ssh_key_panel);
      }
    }

    public void onNothingSelected(AdapterView<?> parent) {}
  }

  public class GenerateKeyButton implements OnClickListener {
    public void onClick(View v) {
      ui.show(R.id.repo_ssh_key_generate_progress_panel);
      ui.hide(R.id.repo_ssh_key_fingerprint_panel);
      ui.hide(R.id.repo_ssh_key_copy);
      ui.disable(R.id.repo_ssh_key_generate);

      new KeygenThread().start();
    }

    class KeygenThread extends Thread implements Runnable {
      public void run() {
        final SSHKey key = carcosa.keygen();

        ui.hide(R.id.repo_ssh_key_generate_progress_panel);
        ui.show(R.id.repo_ssh_key_fingerprint_panel);
        ui.show(R.id.repo_ssh_key_copy);
        ui.enable(R.id.repo_ssh_key_generate);
        ui.text(R.id.repo_ssh_key_fingerprint, key.fingerprint);
      }
    }
  }

  public class ConnectButton implements OnClickListener {
    class ConnectThread extends Thread implements Runnable {
      public void run() {
        String protocol = ui.text(R.id.repo_protocol);
        String address = ui.text(R.id.repo_address);
        String namespace = ui.text(R.id.repo_token_namespace);
        String filter = ui.text(R.id.repo_token_filter);

        Maybe<ConnectResult> connect = carcosa.connect(protocol, address, namespace, filter);
        if (connect.error != null) {
          ui.hide(R.id.repo_connect_progress_panel);
          ui.text(R.id.repo_error, connect.error);
          ui.show(R.id.repo_error);
        } else {
          repoID = connect.result.id;
          ui.hide(R.id.repo_connect_progress_panel);
          ui.show(R.id.repo_stat_panel);
          ui.show(R.id.repo_unlock_panel);
          ui.focus(R.id.repo_master_password);
          ui.hide(R.id.repo_connect);
          ui.text(R.id.repo_stat, "%d secret tokens available.", connect.result.tokens);
        }
      }
    }

    public void onClick(View v) {
      ui.hide(R.id.repo_error);
      ui.show(R.id.repo_connect_progress_panel);
      ui.disable(R.id.repo_protocol);
      ui.disable(R.id.repo_address);
      ui.disable(R.id.repo_token_namespace);
      ui.disable(R.id.repo_connect);

      new ConnectThread().start();
    }
  }

  public class UnlockButton implements OnClickListener {
    public void onClick(View v) {
      ui.disable(R.id.repo_master_password);
      ui.disable(R.id.repo_master_password_cache);
      ui.disable(R.id.repo_unlock);

      String key = ui.text(R.id.repo_master_password);

      Maybe<UnlockResult> unlock = carcosa.unlock(repoID, key, false);
      if (unlock.error != null) {
        ui.text(R.id.repo_error, unlock.error);
        ui.show(R.id.repo_error);
      } else {
        if (unlock.result.tokens == 0) {
          ui.enable(R.id.repo_master_password);
          ui.enable(R.id.repo_master_password_cache);
          ui.enable(R.id.repo_unlock);
          ui.show(R.id.repo_unlock_wrong_master_password);
        } else {
          ui.hide(R.id.repo_unlock_panel);
          ui.show(R.id.repo_unlock_done_panel);
          ui.text(R.id.repo_unlock_done_stat, "%d tokens unlocked!", unlock.result.tokens);
        }
      }
    }
  }
}
