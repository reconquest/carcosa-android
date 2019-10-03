package io.reconquest.carcosa;

import java.nio.file.Paths;

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

public class MainActivity extends AppCompatActivity {
  private Carcosa carcosa;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    carcosa = new Carcosa();
    Maybe<Void> init =
        carcosa.init(Paths.get(getApplicationContext().getFilesDir().getPath()).toString());
    if (init.error != null) {
      new FatalErrorDialog(this, init.error).show();
    }

    setContentView(R.layout.main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
    toolbar.setSubtitle("secrets");
    setSupportActionBar(toolbar);
    // setListAdapter(new List());

    Maybe<Carcosa.ListResult> list = carcosa.list();
    if (list.error != null) {
      new FatalErrorDialog(this, list.error).show();
    } else {
      for (Carcosa.Repo repo : list.result.repos) {
        System.err.printf(
            "!!! src/main/java/io/reconquest/carcosa/MainActivity.java:55 %s\n", repo.name);
      }
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

  // public class RepoList extends BaseAdapter {
  //  @Override
  //  public int getCount() {
  //    return 1;
  //  }

  //  @Override
  //  public String getItem(int position) {
  //    return "test";
  //  }

  //  @Override
  //  public long getItemId(int arg0) {
  //    return "test".hashCode();
  //  }

  //  @Override
  //  public View getView(int position, View convertView, ViewGroup container) {
  //    if (convertView == null) {
  //      convertView = getLayoutInflater().inflate(R.layout.repo_list_item, container, false);
  //    }

  //    text()

  //    ((TextView) convertView.findViewById(R.id.text)).setText(getItem(position));

  //    return convertView;
  //  }
  // }
}
