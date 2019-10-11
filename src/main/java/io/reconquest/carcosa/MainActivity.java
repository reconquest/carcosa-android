package io.reconquest.carcosa;

import java.nio.file.Paths;
import java.util.ArrayList;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.reconquest.carcosa.lib.Carcosa;
import io.reconquest.carcosa.lib.ListResult;
import io.reconquest.carcosa.lib.Repo;
import io.reconquest.carcosa.lib.Token;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getName();
  private Carcosa carcosa;
  private RepoList repoList;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main);

    carcosa = new Carcosa();
    Maybe<Void> init =
        carcosa.init(Paths.get(getApplicationContext().getFilesDir().getPath()).toString(), "1234");
    if (init.error != null) {
      new FatalErrorDialog(this, init.error).show();
    }

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
    toolbar.setSubtitle("secrets");
    setSupportActionBar(toolbar);
    list();
  }

  protected void list() {
    Maybe<ListResult> list = carcosa.list();
    if (list.error != null) {
      new FatalErrorDialog(this, list.error).show();
    } else {
      repoList = new RepoList(this, list.result.repos);
      ((ListView) findViewById(R.id.repo_list)).setAdapter(repoList);
    }
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

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.toolbar_main_action_sync:
        new SyncThread(this).start();
        break;
      case R.id.toolbar_main_action_add_repo:
        Intent intent = new Intent(this, RepoActivity.class);
        intent.putExtra("Carcosa", carcosa);
        startActivity(intent);
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

  public class RepoList extends BaseAdapter {
    Activity activity;
    ArrayList<Repo> repos;

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
    public View getView(int position, View view, ViewGroup container) {
      if (view == null) {
        view = getLayoutInflater().inflate(R.layout.repo_list_item, container, false);
      }

      UI ui = new UI(view);

      Repo repo = getItem(position);

      ui.text(R.id.repo_list_item_name, repo.name);
      ui.text(R.id.repo_list_item_sync_stat_date, repo.syncStat.date);

      if (repo.syncStat.added > 0) {
        ui.text(R.id.repo_list_item_sync_stat_added, String.format("+%d", repo.syncStat.added));
        ui.show(R.id.repo_list_item_sync_stat_added);
      }

      if (repo.syncStat.deleted > 0) {
        ui.text(R.id.repo_list_item_sync_stat_deleted, String.format("−%d", repo.syncStat.deleted));
        ui.show(R.id.repo_list_item_sync_stat_deleted);
      }

      if (repo.syncStat.added + repo.syncStat.deleted == 0) {
        ui.show(R.id.repo_list_item_sync_stat_uptodate);
      }

      ListView tokensView = (ListView) view.findViewById(R.id.repo_token_list);
      ListAdapter tokensAdapter = new RepoTokenList(activity, repo.tokens);
      ViewGroup.LayoutParams params = tokensView.getLayoutParams();

      params.height = (tokensView.getDividerHeight() * (tokensAdapter.getCount() - 1));

      for (int i = 0; i < tokensAdapter.getCount(); i++) {
        View listItem = tokensAdapter.getView(i, null, tokensView);
        listItem.measure(0, 0);
        params.height += listItem.getMeasuredHeight();
      }

      tokensView.setLayoutParams(params);
      tokensView.setAdapter(tokensAdapter);

      return view;
    }
  }

  public class RepoTokenList extends BaseAdapter {
    ArrayList<Token> tokens;
    Activity activity;

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

    RepoTokenList(Activity activity, ArrayList<Token> tokens) {
      this.activity = activity;
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
    public View getView(int position, View view, ViewGroup container) {
      if (view == null) {
        view = getLayoutInflater().inflate(R.layout.repo_token_list_item, container, false);
      }

      UI ui = new UI(view);

      Token token = getItem(position);

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

      ui.onClick(R.id.repo_token_list_item_copy, new CopyButton(token.name, token.payload));

      return view;
    }
  }
}
