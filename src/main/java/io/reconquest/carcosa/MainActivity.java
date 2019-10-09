package io.reconquest.carcosa;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import io.reconquest.carcosa.lib.Carcosa;
import io.reconquest.carcosa.lib.ListResult;
import io.reconquest.carcosa.lib.Repo;
import io.reconquest.carcosa.lib.Token;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getName();
  private Carcosa carcosa;

  private Handler handler = new Handler();

  private Executor executor =
      new Executor() {
        @Override
        public void execute(Runnable command) {
          handler.post(command);
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main);

    carcosa = new Carcosa();
    Maybe<Void> init =
        carcosa.init(Paths.get(getApplicationContext().getFilesDir().getPath()).toString());
    if (init.error != null) {
      new FatalErrorDialog(this, init.error).show();
    }

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
    toolbar.setSubtitle("secrets");
    setSupportActionBar(toolbar);

    Maybe<ListResult> list = carcosa.list();
    if (list.error != null) {
      new FatalErrorDialog(this, list.error).show();
    } else {
      ((ListView) findViewById(R.id.repo_list)).setAdapter(new RepoList(list.result.repos));
    }

    BiometricManager biometricManager = BiometricManager.from(this);
    switch (biometricManager.canAuthenticate()) {
      case BiometricManager.BIOMETRIC_SUCCESS:
        Log.d(TAG, "App can authenticate using biometrics.");
        break;
      case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
        Log.d(TAG, "No biometric features available on this device.");
        break;
      case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
        Log.d(TAG, "Biometric features are currently unavailable.");
        break;
      case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
        Log.d(TAG, "The user hasn't associated any biometric credentials with their account.");
        break;
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
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
    ArrayList<Repo> repos;

    RepoList(ArrayList<Repo> repos) {
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
      ListAdapter tokensAdapter = new RepoTokenList(repo.tokens);
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
    public class CopyButton implements OnClickListener {
      String secret;

      CopyButton(String secret) {
        this.secret = secret;
      }

      public void onClick(View v) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, secret, duration);
        toast.show();
      }
    }

    ArrayList<Token> tokens;

    RepoTokenList(ArrayList<Token> tokens) {
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

      ui.onClick(R.id.repo_token_list_item_copy, new CopyButton(token.payload));

      return view;
    }
  }
}
