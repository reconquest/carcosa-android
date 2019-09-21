package io.reconquest.carcosa;

import java.io.File;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MainActivity extends ListActivity {
  private Carcosa carcosa;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    carcosa = new Carcosa();
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    setListAdapter(new List());
    File dir = getApplicationContext().getDir("secrets", 0);
    carcosa.sync(dir.getPath());
  }

  public class List extends BaseAdapter {
    @Override
    public int getCount() {
      // TODO Auto-generated method stub
      return 1;
    }

    @Override
    public String getItem(int position) {
      // TODO Auto-generated method stub
      return "test";
    }

    @Override
    public long getItemId(int arg0) {
      // TODO Auto-generated method stub
      return "test".hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
      if (convertView == null) {
        convertView = getLayoutInflater().inflate(R.layout.list_item, container, false);
      }

      ((TextView) convertView.findViewById(R.id.text)).setText(getItem(position));

      return convertView;
    }
  }
}
