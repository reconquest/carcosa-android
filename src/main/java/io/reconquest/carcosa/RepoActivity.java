package io.reconquest.carcosa;

import com.google.android.material.textfield.TextInputLayout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.reconquest.carcosa.lib.Carcosa;
import io.reconquest.carcosa.lib.ConnectResult;
import io.reconquest.carcosa.lib.Repo;
import io.reconquest.carcosa.lib.RepoConfig;
import io.reconquest.carcosa.lib.SSHKey;
import io.reconquest.carcosa.lib.UnlockResult;

public class RepoActivity extends AppCompatActivity {
  private UI ui;
  private Carcosa carcosa;
  private Repo repo;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ui = new UI(this);

    setContentView(R.layout.repo);

    carcosa = (Carcosa) getIntent().getSerializableExtra("Carcosa");
    repo = (Repo) getIntent().getSerializableExtra("Repo");

    if (repo != null) {
      ui.text(R.id.repo_address, repo.config.address);
      ui.text(R.id.repo_token_namespace, repo.config.namespace);
      ui.text(R.id.repo_token_filter, repo.config.filter);

      if (repo.config.protocol.equals("ssh")) {
        ((RadioButton) findViewById(R.id.repo_protocol_ssh)).setChecked(true);
        ui.text(R.id.repo_ssh_key_fingerprint_left, repo.sshKey.fingerprint.substring(0, 5));
        ui.text(
            R.id.repo_ssh_key_fingerprint_right,
            repo.sshKey.fingerprint.substring(repo.sshKey.fingerprint.length() - 5));
        ui.enable(R.id.repo_ssh_key_copy);
        ui.show(R.id.repo_ssh_key_fingerprint_panel);
        ui.show(R.id.repo_ssh_key_copy);
        ui.show(R.id.repo_ssh_key_panel);
      }

      ui.disable(R.id.repo_protocol_label);
      ui.disable(R.id.repo_protocol_git);
      ui.disable(R.id.repo_protocol_ssh);

      ui.readonly(R.id.repo_token_namespace);
      ui.readonly(R.id.repo_token_filter);

      ui.readonly(R.id.repo_address);
      ui.disableHelp(R.id.repo_address_panel);
      ui.disableHelp(R.id.repo_token_filter_panel);
      ui.disable(R.id.repo_ssh_key_label);
      ui.readonly(R.id.repo_token_namespace);
      ui.readonly(R.id.repo_token_filter);

      ui.hide(R.id.repo_ssh_key_help);
      ui.hide(R.id.repo_connected_panel);
      ui.hide(R.id.repo_stat);

      ui.hide(R.id.repo_connect_progress_panel);
      ui.show(R.id.repo_unlock_panel);
      ui.focus(R.id.repo_master_password);
      ui.hide(R.id.repo_connect);
    } else {
      repo = new Repo();
    }

    // ((Spinner) findViewById(R.id.repo_protocol)).setOnItemSelectedListener(new ProtocolSelect());

    ((RadioGroup) findViewById(R.id.repo_protocol))
        .setOnCheckedChangeListener(new ProtocolSelect());

    ui.onEdit(
        R.id.repo_address,
        new UI.OnTextChangedListener() {
          public void onTextChanged(CharSequence chars, int start, int count, int after) {
            if (chars.toString().length() == 0) {
              ui.disable(R.id.repo_connect);
            } else {
              ui.enable(R.id.repo_connect);
            }
          }
        });

    ui.onEdit(
        R.id.repo_master_password,
        new UI.OnTextChangedListener() {
          public void onTextChanged(CharSequence chars, int start, int count, int after) {
            if (chars.toString().length() == 0) {
              ui.disable(R.id.repo_unlock);
            } else {
              ui.enable(R.id.repo_unlock);
            }
          }
        });

    // ui.onClick(R.id.repo_ssh_key_generate, new GenerateKeyButton());
    ui.onClick(R.id.repo_ssh_key_copy, new CopyKeyButton(this));
    ui.onClick(R.id.repo_connect, new ConnectButton());
    ui.onClick(R.id.repo_unlock, new UnlockButton());
    ui.onClick(R.id.repo_advanced, new AdvancedSettingsPanel());

    ui.onClick(
        R.id.repo_done,
        new OnClickListener() {
          public void onClick(View v) {
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            startActivity(intent);
          }
        });

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
        animation =
            new RotateAnimation(
                90, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
      } else {
        ui.show(R.id.repo_advanced_panel);
        animation =
            new RotateAnimation(
                0, 90, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
      }

      animation.setDuration(100);
      animation.setFillAfter(true);
      ui.animate(R.id.repo_advanced_icon, animation);

      shown = !shown;
    }
  }

  public class ProtocolSelect implements OnCheckedChangeListener {
    public void onCheckedChanged(RadioGroup group, int checkedId) {
      RadioButton button = (RadioButton) findViewById(checkedId);

      if (button.getText().equals("ssh")) {
        ui.show(R.id.repo_ssh_key_panel);
        ui.show(R.id.repo_ssh_key_help);
        new KeygenThread().start();
      } else {
        ui.hide(R.id.repo_ssh_key_panel);
        ui.hide(R.id.repo_ssh_key_help);
      }
    }

    public void onNothingSelected(AdapterView<?> parent) {}
  }

  public class CopyKeyButton implements OnClickListener {
    Activity activity;

    CopyKeyButton(Activity activity) {
      this.activity = activity;
    }

    public void onClick(View v) {
      new Clipboard(activity)
          .clip(
              "ssh " + repo.sshKey.fingerprint,
              repo.sshKey.publicKey,
              "Public key copied to clipboard.");
    }
  }

  class KeygenThread extends Thread implements Runnable {
    public void run() {
      ui.disable(R.id.repo_ssh_key_copy);
      ui.hide(R.id.repo_ssh_key_fingerprint_panel);
      ui.show(R.id.repo_ssh_key_generate_progress_panel);

      Maybe<SSHKey> keygen = carcosa.keygen();

      ui.hide(R.id.repo_ssh_key_generate_progress_panel);

      if (keygen.error != null) {
        ui.text(R.id.repo_error, keygen.error);
        ui.show(R.id.repo_error);
      } else {
        ui.text(R.id.repo_ssh_key_fingerprint_left, keygen.result.fingerprint.substring(0, 5));
        ui.text(
            R.id.repo_ssh_key_fingerprint_right,
            keygen.result.fingerprint.substring(keygen.result.fingerprint.length() - 5));
        ui.enable(R.id.repo_ssh_key_copy);
        ui.show(R.id.repo_ssh_key_fingerprint_panel);
        ui.show(R.id.repo_ssh_key_copy);

        repo.sshKey = keygen.result;
      }
    }
  }

  public class ConnectButton implements OnClickListener {
    class ConnectThread extends Thread implements Runnable {
      public void run() {
        RepoConfig config = new RepoConfig();

        config.protocol = ui.text(R.id.repo_protocol);
        config.address = ui.text(R.id.repo_address);
        config.namespace = ui.text(R.id.repo_token_namespace);
        config.filter = ui.text(R.id.repo_token_filter);

        Maybe<ConnectResult> connect = carcosa.connect(config, repo.sshKey);
        if (connect.error != null) {
          ui.enable(R.id.repo_protocol_label);
          ui.enable(R.id.repo_protocol_git);
          ui.enable(R.id.repo_protocol_ssh);
          ui.enable(R.id.repo_address);
          ui.enable(R.id.repo_token_namespace);
          ui.enable(R.id.repo_token_filter);
          ui.enable(R.id.repo_connect);

          ui.hide(R.id.repo_connect_progress_panel);
          ui.text(R.id.repo_error, connect.error);
          ui.show(R.id.repo_error);
        } else {
          repo.id = connect.result.id;
          ui.hide(R.id.repo_connect_progress_panel);
          ui.hide(R.id.repo_connect);
          ui.text(R.id.repo_stat, "%d secret tokens available.", connect.result.tokens);
          ui.disableHelp(R.id.repo_address_panel);
          ui.disableHelp(R.id.repo_token_filter_panel);
          ui.disable(R.id.repo_ssh_key_label);
          ui.readonly(R.id.repo_address);
          ui.readonly(R.id.repo_token_namespace);
          ui.readonly(R.id.repo_token_filter);
          ui.hide(R.id.repo_ssh_key_copy);
          ui.hide(R.id.repo_ssh_key_help);
          ui.show(R.id.repo_unlock_panel);
          ui.focus(R.id.repo_master_password);
        }
      }
    }

    public void onClick(View v) {
      ui.hide(R.id.repo_error);
      ui.show(R.id.repo_connect_progress_panel);
      ui.disable(R.id.repo_protocol_label);
      ui.disable(R.id.repo_protocol_git);
      ui.disable(R.id.repo_protocol_ssh);
      ui.disable(R.id.repo_address);
      ui.disable(R.id.repo_token_namespace);
      ui.disable(R.id.repo_token_filter);
      ui.disable(R.id.repo_connect);

      new ConnectThread().start();
    }
  }

  public class UnlockButton implements OnClickListener {
    public void onClick(View v) {
      ui.disable(R.id.repo_master_password);
      ui.disable(R.id.repo_unlock);

      String key = ui.text(R.id.repo_master_password);
      String filter = ui.text(R.id.repo_token_filter);

      Maybe<UnlockResult> unlock = carcosa.unlock(repo.id, key, filter, true);
      if (unlock.error != null) {
        ui.text(R.id.repo_error, unlock.error);
        ui.show(R.id.repo_error);
      } else {
        if (unlock.result.tokens == 0) {
          ui.enable(R.id.repo_master_password);
          ui.enable(R.id.repo_unlock);

          ((TextInputLayout) findViewById(R.id.repo_master_password_panel))
              .setError("Master password is invalid!");
          // ui.show(R.id.repo_unlock_wrong_master_password);
        } else {
          ui.hide(R.id.repo_unlock_panel);
          ui.show(R.id.repo_unlock_done_panel);
          ui.text(R.id.repo_unlock_done_stat, "%d tokens unlocked!", unlock.result.tokens);
        }
      }
    }
  }
}
