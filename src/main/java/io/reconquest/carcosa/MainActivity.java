package io.reconquest.carcosa;

import java.nio.file.Paths;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends AppActivity {
  private Carcosa carcosa;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    carcosa = new Carcosa();
    carcosa.init(Paths.get(getApplicationContext().getFilesDir().getPath()).toString());

    setContentView(R.layout.main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
    toolbar.setSubtitle("secrets");
    setSupportActionBar(toolbar);
    // setListAdapter(new List());
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

  // public class List extends BaseAdapter {
  //  @Override
  //  public int getCount() {
  //    // TODO Auto-generated method stub
  //    return 1;
  //  }

  //  @Override
  //  public String getItem(int position) {
  //    // TODO Auto-generated method stub
  //    return "test";
  //  }

  //  @Override
  //  public long getItemId(int arg0) {
  //    // TODO Auto-generated method stub
  //    return "test".hashCode();
  //  }

  //  @Override
  //  public View getView(int position, View convertView, ViewGroup container) {
  //    if (convertView == null) {
  //      convertView = getLayoutInflater().inflate(R.layout.list_item, container, false);
  //    }

  //    ((TextView) convertView.findViewById(R.id.text)).setText(getItem(position));

  //    return convertView;
  //  }
  // }
}
