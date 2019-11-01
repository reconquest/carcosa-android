package io.reconquest.carcosa;

import java.util.Locale;

import android.view.View;
import android.view.View.OnClickListener;
import io.reconquest.carcosa.lib.Repo;

public class RepoViewHolder {
  MainActivity activity;
  UI ui;

  public RepoViewHolder(MainActivity activity, View view) {
    this.activity = activity;
    this.ui = new UI(view);
  }

  public void draw(Repo repo) {
    ui.text(R.id.repo_list_item_name, repo.name);
    ui.text(R.id.repo_list_item_sync_stat_date, repo.syncStat.date);

    if (repo.syncStat.added > 0) {
      ui.text(R.id.repo_list_item_sync_stat_added, String.format(
          Locale.getDefault(), "+%d", repo.syncStat.added));
      ui.show(R.id.repo_list_item_sync_stat_added);
    }

    if (repo.syncStat.deleted > 0) {
      ui.text(R.id.repo_list_item_sync_stat_deleted, String.format(
          Locale.getDefault(), "−%d", repo.syncStat.deleted));
      ui.show(R.id.repo_list_item_sync_stat_deleted);
    }

    if (repo.syncStat.added + repo.syncStat.deleted == 0) {
      ui.show(R.id.repo_list_item_sync_stat_uptodate);
    }

    if (repo.isLocked) {
      ui.show(R.id.repo_list_item_unlock);
      ui.onClick(R.id.repo_list_item_unlock, new UnlockButton(repo));
    }
  }

  public class UnlockButton implements OnClickListener {
    Repo repo;

    UnlockButton(Repo repo) {
      this.repo = repo;
    }

    public void onClick(View v) {
      activity.gotoRepoActivity(repo);
    }
  }
}
