package io.reconquest.carcosa;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;

import com.google.android.material.navigation.NavigationView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import io.reconquest.carcosa.lib.Carcosa;
import io.reconquest.carcosa.lib.ListResult;
import io.reconquest.carcosa.lib.Repo;

public class MainActivity extends AppCompatActivity implements Lister {
  private static final String TAG = MainActivity.class.getName();
  private UI ui;
  private RepoList repoList;

  private Carcosa carcosa = new Carcosa();

  TextView searchField;
  ListView repoListView;

  private DrawerLayout drawerLayout;
  private Toolbar toolbar;
  private NavigationView navigationView;
  private ActionBarDrawerToggle drawerToggle;

  private boolean paused = false;
  private Date pauseDate = null;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);

    setContentView(R.layout.main);
    initCarcosa();
    initUI();
  }

  @Override
  protected void onPause() {
    super.onPause();

    paused = true;
    pauseDate = new Date();
  }

  @Override
  protected void onResume() {
    super.onResume();

    // onResume() is also called after onCreate()
    if (!paused) {
      return;
    }

    final long ONE_SECOND = 1000;
    // TODO: move to sharedPreferences
    final long seconds = 3;

    Date expireDate = new Date(pauseDate.getTime() + (seconds * ONE_SECOND));
    Date now = new Date();

    if (now.after(expireDate)) {
      carcosa.destroy();
      resetRepoList();

      Intent intent = new Intent(this, LoginActivity.class);
      startActivity(intent);
    }
  }

  private void resetRepoList() {
    repoList = new RepoList(this, new ArrayList<Repo>());
    repoListView.setAdapter(repoList);
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
    ui = new UI(this);

    setContentView(R.layout.main);

    toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle("Secrets");
    toolbar.setTitleTextColor(0xFF000000);
    setSupportActionBar(toolbar);

    drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawerToggle = setupDrawerToggle();
    drawerLayout.addDrawerListener(drawerToggle);

    bindViews();
    bindSearch();

    new Thread(
            () -> {
              list();
            })
        .start();
  }

  private ActionBarDrawerToggle setupDrawerToggle() {
    // NOTE: Make sure you pass in a valid toolbar reference.  ActionBarDrawToggle() does not
    // require it
    // and will not render the hamburger icon without it.
    ActionBarDrawerToggle toggle =
        new ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);

    toggle.getDrawerArrowDrawable().setColor(0xFF000000);
    toggle.setDrawerIndicatorEnabled(true);
    toggle.syncState();

    return toggle;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // The action bar home/up action should open or close the drawer.
    switch (item.getItemId()) {
      case android.R.id.home:
        drawerLayout.openDrawer(GravityCompat.START);
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public void list() {
    Maybe<ListResult> list = carcosa.list();
    if (list.error != null) {
      new FatalErrorDialog(this, list.error).show();
    } else {
      ui.hide(R.id.list_progress);
      if (list.result.repos.size() > 0) {
        ui.show(R.id.search_query_panel);
        ui.ui(
            () -> {
              repoList = new RepoList(this, list.result.repos);
              repoListView.setAdapter(repoList);
            });
      }
    }
  }

  protected void bindViews() {
    searchField = (TextView) findViewById(R.id.search_query);
    repoListView = ((ListView) findViewById(R.id.repo_list));

    ui.onClick(
        R.id.toolbar_main_action_sync,
        (View v) -> {
          new SyncThread(this, carcosa).start();
        });

    ui.onClick(
        R.id.toolbar_main_action_add_repo,
        (View v) -> {
          gotoRepoScreen(null);
        });
  }

  protected void bindSearch() {
    ui.onEdit(
        R.id.search_query,
        new UI.OnTextChangedListener() {
          public void onTextChanged(CharSequence chars, int start, int count, int after) {
            repoList.getFilter().filter(ui.text(R.id.search_query).toLowerCase());
          }
        });
  }

  void gotoRepoScreen(Repo repo) {
    Intent intent = new Intent(this, RepoActivity.class);
    intent.putExtra("Carcosa", carcosa);
    intent.putExtra("Repo", repo);
    startActivity(intent);
  }
}
