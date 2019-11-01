package io.reconquest.carcosa;

import java.nio.file.Paths;
import java.util.ArrayList;

import com.google.android.material.navigation.NavigationView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import androidx.annotation.NonNull;
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
  private SecretsList secrets;

  private Carcosa carcosa = new Carcosa();
  private Session session;

  ListView secretsList;

  private DrawerLayout drawerLayout;
  private Toolbar toolbar;
  private ActionBarDrawerToggle drawerToggle;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);

    setContentView(R.layout.main);

    session =
        new Session(
            getBaseContext(),
            carcosa,
            () -> {
              secrets = new SecretsList(this, new ArrayList<Repo>());
              secretsList.setAdapter(secrets);
            });

    initCarcosa();
    initUI();
  }

  @Override
  protected void onPause() {
    super.onPause();
    session.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    session.onResume();
  }

  private void initCarcosa() {
    if (carcosa.hasState()) {
      return;
    }

    String pin = getIntent().getStringExtra("pin");
    if (pin == null) {
      Log.e(TAG, "pin was not passed to intent");
      Intent intent = new Intent(this, LoginActivity.class);
      startActivity(intent);
      return;
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

    bindViews();

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
              secrets = new SecretsList(this, list.result.repos);
              secretsList.setAdapter(secrets);
            });
      }
    }
  }

  protected void bindViews() {
    toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle("Secrets");
    toolbar.setTitleTextColor(0xFF000000);
    setSupportActionBar(toolbar);

    drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawerToggle = setupDrawerToggle();
    drawerLayout.addDrawerListener(drawerToggle);

    secretsList = (ListView) findViewById(R.id.secrets_list);

    ui.onClick(
        R.id.toolbar_main_action_sync,
        (View v) -> {
          new SyncThread(this, carcosa).start();
        });

    ui.onClick(
        R.id.toolbar_main_action_add_repo,
        (View v) -> {
          gotoRepoActivity(null);
        });

    ui.onEdit(
        R.id.search_query,
        new UI.OnTextChangedListener() {
          public void onTextChanged(CharSequence chars, int start, int count, int after) {
            secrets.filter(ui.text(R.id.search_query).toLowerCase());
          }
        });

    NavigationView navbar = (NavigationView) findViewById(R.id.navbar);
    navbar.setNavigationItemSelectedListener(
        new NavigationView.OnNavigationItemSelectedListener() {
          public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
              case R.id.nav_settings:
                gotoSettingsActivity();
                break;

              case R.id.nav_about:
                gotoAboutActivity();
                break;
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
          }
        });
  }

  void gotoRepoActivity(Repo repo) {
    Intent intent = new Intent(this, RepoActivity.class);
    intent.putExtra("carcosa", carcosa);
    intent.putExtra("repo", repo);
    startActivity(intent);
  }

  void gotoSettingsActivity() {
    Intent intent = new Intent(this, SettingsActivity.class);
    intent.putExtra("carcosa", carcosa);
    startActivity(intent);
  }

  void gotoAboutActivity() {
    Intent intent = new Intent(this, AboutActivity.class);
    intent.putExtra("carcosa", carcosa);
    startActivity(intent);
  }
}
