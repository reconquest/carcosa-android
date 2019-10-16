package io.reconquest.carcosa;

import java.nio.file.Paths;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.reconquest.carcosa.lib.Carcosa;
import io.reconquest.carcosa.lib.ListResult;
import io.reconquest.carcosa.lib.Repo;

public class MainActivity extends AppCompatActivity implements Lister {
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

  public void list() {
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
        new SyncThread(this, carcosa).start();
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
}
