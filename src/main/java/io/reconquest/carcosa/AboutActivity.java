package io.reconquest.carcosa;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.reconquest.carcosa.lib.Carcosa;

public class AboutActivity extends AppCompatActivity {
  private Session session;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.about);

    session =
        new Session(getBaseContext(), (Carcosa) getIntent().getSerializableExtra("carcosa"), null);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_about);
    toolbar.setTitle("About");
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
}
