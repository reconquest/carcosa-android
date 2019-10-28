package io.reconquest.carcosa;

import java.util.ArrayList;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import androidx.appcompat.app.AlertDialog;
import io.reconquest.carcosa.lib.Token;

public class RepoTokenList extends BaseAdapter implements Filterable {
  ArrayList<Token> originTokens;
  ArrayList<Token> tokens;
  Activity activity;
  RepoTokenListFilter filter = new RepoTokenListFilter();

  RepoTokenList(Activity activity, ArrayList<Token> tokens) {
    this.activity = activity;
    this.originTokens = tokens;
    this.tokens = tokens;
  }

  @Override
  public int getCount() {
    return tokens.size();
  }

  @Override
  public Token getItem(int i) {
    return tokens.get(i);
  }

  @Override
  public long getItemId(int i) {
    return getItem(i).name.hashCode();
  }

  @Override
  public Filter getFilter() {
    return filter;
  }

  @Override
  public View getView(int position, View view, ViewGroup container) {
    TokenViewHolder holder;
    if (view == null) {
      view = activity.getLayoutInflater().inflate(R.layout.repo_token_list_item, container, false);
      holder = new TokenViewHolder(view);
      view.setTag(holder);
    } else {
      holder = (TokenViewHolder) view.getTag();
    }

    holder.draw(tokens.get(position));

    return view;
  }

  private class TokenViewHolder {
    UI ui;

    public TokenViewHolder(View view) {
      ui = new UI(view);
    }

    public void draw(Token token) {
      if (token.resource.equals("")) {
        ui.text(R.id.repo_token_list_item_name, token.name);
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
  }

  private class RepoTokenListFilter extends Filter {
    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
      FilterResults results = new FilterResults();
      if (constraint.length() == 0) {
        results.values = originTokens;
        results.count = originTokens.size();
        return results;
      }

      ArrayList<Token> filtered = new ArrayList<Token>();
      for (int i = 0; i < originTokens.size(); i++) {
        Token token = originTokens.get(i);
        if (token.name.toLowerCase().contains(constraint)) {
          filtered.add(token);
        }
      }
      results.values = filtered;
      results.count = filtered.size();
      return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
      tokens = (ArrayList<Token>) results.values;
      notifyDataSetChanged();
    }
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
