package io.reconquest.carcosa;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import androidx.appcompat.app.AlertDialog;
import io.reconquest.carcosa.lib.Token;

public class TokenViewHolder {
  Activity activity;
  UI ui;

  public TokenViewHolder(Activity activity, View view) {
    this.activity = activity;
    this.ui = new UI(view);
  }

  public void draw(Token token) {
    if (token.resource.equals("")) {
      ui.hide(R.id.repo_token_list_item_resource_panel);
      ui.text(R.id.repo_token_list_item_name, token.name);
      ui.show(R.id.repo_token_list_item_name_panel);
    } else {
      ui.hide(R.id.repo_token_list_item_name_panel);
      if (!token.login.equals("")) {
        ui.text(R.id.repo_token_list_item_login, token.login);
        ui.show(R.id.repo_token_list_item_login_panel);
      }

      ui.text(R.id.repo_token_list_item_resource, token.resource);
      ui.show(R.id.repo_token_list_item_resource_panel);
    }

    ui.onClick(R.id.repo_token_list_item_view, new ViewButton(token.name, token.payload));
    ui.onClick(R.id.repo_token_list_item_copy, new CopyButton(token.name, token.payload));
  }

  public class ViewButton implements OnClickListener {
    String secret;
    String token;

    ViewButton(String token, String secret) {
      this.token = token;
      this.secret = secret;
    }

    public void onClick(View v) {
      AlertDialog.Builder builder = new AlertDialog.Builder(activity);

      builder.setTitle(token);
      builder.setMessage(secret);

      builder.setNeutralButton(
          "Close",
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
          });

      builder.create().show();
    }
  }

  public class CopyButton implements OnClickListener {
    String secret;
    String token;

    CopyButton(String token, String secret) {
      this.token = token;
      this.secret = secret;
    }

    public void onClick(View v) {
      new Clipboard(activity).clip(token, secret, "Secret copied to clipboard.");
    }
  }
}
