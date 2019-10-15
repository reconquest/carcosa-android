package io.reconquest.carcosa;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.reconquest.carcosa.lib.Carcosa;
import io.reconquest.carcosa.lib.ListResult;
import io.reconquest.carcosa.lib.Repo;
import io.reconquest.carcosa.lib.Token;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getName();
  private RepoList repoList;

  private Carcosa carcosa = new Carcosa();
  private boolean searchEnabled;

  TextView searchField;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);

    initCarcosa();
  }

  @Override
  protected void onStart() {
    super.onStart();

    initUI();
  }

  private void initCarcosa() {
    if (carcosa.hasState()) {
      return;
    }

    String pin = getIntent().getStringExtra("pin");
    if (pin == null) {
      Log.e(TAG, "pin was not passed to intent");
    }

    Maybe<Void> init =
        carcosa.init(Paths.get(getApplicationContext().getFilesDir().getPath()).toString(), pin);
    if (init.error != null) {
      new FatalErrorDialog(this, init.error).show();
      return;
    }
  }

  private void initUI() {
    setContentView(R.layout.main);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
    toolbar.setSubtitle("secrets");
    setSupportActionBar(toolbar);

    bindSearch();
    list();
  }

  protected void list() {
    searchEnabled = false;

    searchField.setText(null);

    Maybe<ListResult> list = carcosa.list();
    if (list.error != null) {
      new FatalErrorDialog(this, list.error).show();
    } else {
      repoList = new RepoList(this, list.result.repos);
      ((ListView) findViewById(R.id.repo_list)).setAdapter(repoList);
    }

    searchEnabled = true;
  }

  protected void bindSearch() {
    searchField = (TextView) findViewById(R.id.search_query);
    searchField.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          public void afterTextChanged(Editable s) {
            if (!searchEnabled) {
              return;
            }

            final String query = searchField.getText().toString();
            repoList.getFilter().filter(query.toLowerCase());
          }
        });
  }

  class SyncThread extends Thread implements Runnable {
    Activity activity;

    SyncThread(Activity activity) {
      this.activity = activity;
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
                list();
              }
            }
          });
    }
  }

  void gotoRepoScreen(Repo repo) {
    Intent intent = new Intent(this, RepoActivity.class);
    intent.putExtra("Carcosa", carcosa);
    intent.putExtra("Repo", repo);
    startActivity(intent);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.toolbar_main_action_sync:
        new SyncThread(this).start();
        break;
      case R.id.toolbar_main_action_add_repo:
        gotoRepoScreen(null);
        break;
      default:
        break;
    }

    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.toolbar_main, menu);
    return true;
  }

  public class RepoList extends BaseAdapter implements Filterable {
    Activity activity;
    ArrayList<Repo> repos;
    ArrayList<RepoTokenList> tokens = new ArrayList<RepoTokenList>();
    RepoListFilter filter = new RepoListFilter();

    RepoList(Activity activity, ArrayList<Repo> repos) {
      this.activity = activity;
      this.repos = repos;
    }

    @Override
    public int getCount() {
      return repos.size();
    }

    @Override
    public Repo getItem(int i) {
      return repos.get(i);
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
      if (view != null) {
        return view;
      }

      view = getLayoutInflater().inflate(R.layout.repo_list_item, container, false);

      UI ui = new UI(view);

      Repo repo = getItem(position);

      ui.text(R.id.repo_list_item_name, repo.name);
      ui.text(R.id.repo_list_item_sync_stat_date, repo.syncStat.date);

      if (repo.syncStat.added > 0) {
        ui.text(
            R.id.repo_list_item_sync_stat_added,
            String.format(Locale.getDefault(), "+%d", repo.syncStat.added));
        ui.show(R.id.repo_list_item_sync_stat_added);
      }

      if (repo.syncStat.deleted > 0) {
        ui.text(
            R.id.repo_list_item_sync_stat_deleted,
            String.format(Locale.getDefault(), "−%d", repo.syncStat.deleted));
        ui.show(R.id.repo_list_item_sync_stat_deleted);
      }

      if (repo.syncStat.added + repo.syncStat.deleted == 0) {
        ui.show(R.id.repo_list_item_sync_stat_uptodate);
      }

      if (repo.isLocked) {
        ui.show(R.id.repo_list_item_unlock);
        ui.onClick(R.id.repo_list_item_unlock, new UnlockButton(repo));
      }

      ListView tokensView = (ListView) view.findViewById(R.id.repo_token_list);

      RepoTokenList tokenList = new RepoTokenList(activity, repo.tokens);

      ViewGroup.LayoutParams params = tokensView.getLayoutParams();

      params.height = (tokensView.getDividerHeight() * (tokenList.getCount() - 1));

      for (int i = 0; i < tokenList.getCount(); i++) {
        View listItem = tokenList.getView(i, null, tokensView);
        listItem.measure(0, 0);
        params.height += listItem.getMeasuredHeight();
      }

      tokensView.setLayoutParams(params);
      tokensView.setAdapter(tokenList);

      tokens.add(tokenList);

      return view;
    }

    private class RepoListFilter extends Filter {
      @Override
      protected FilterResults performFiltering(CharSequence constraint) {
        for (int i = 0; i < tokens.size(); i++) {
          tokens.get(i).getFilter().filter(constraint);
        }
        return new FilterResults();
      }

      @Override
      protected void publishResults(CharSequence constraint, FilterResults results) {
        notifyDataSetChanged();
      }
    }

    public class UnlockButton implements OnClickListener {
      Repo repo;

      UnlockButton(Repo repo) {
        this.repo = repo;
      }

      public void onClick(View v) {
        gotoRepoScreen(repo);
      }
    }
  }

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
        view = getLayoutInflater().inflate(R.layout.repo_token_list_item, container, false);
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
        for (int i = 0; i < getCount(); i++) {
          Token token = getItem(i);
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
}
