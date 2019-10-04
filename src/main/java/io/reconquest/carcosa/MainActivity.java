package io.reconquest.carcosa;

import java.nio.file.Paths;
import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {
  private Carcosa carcosa;

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

    Maybe<Carcosa.ListResult> list = carcosa.list();
    if (list.error != null) {
      new FatalErrorDialog(this, list.error).show();
    } else {
      ((ListView) findViewById(R.id.repo_list)).setAdapter(new RepoList(list.result.repos));
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
    ArrayList<Carcosa.Repo> repos;

    RepoList(ArrayList<Carcosa.Repo> repos) {
      this.repos = repos;
    }

    @Override
    public int getCount() {
      return repos.size();
    }

    @Override
    public Carcosa.Repo getItem(int i) {
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

      ui.text(R.id.repo_list_item_name, getItem(position).name);
      System.err.printf(
          "!!! src/main/java/io/reconquest/carcosa/MainActivity.java:101 %d\n",
          getItem(position).tokens.size());

      return view;
    }
  }
}