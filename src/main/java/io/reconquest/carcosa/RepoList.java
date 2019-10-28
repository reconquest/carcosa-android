package io.reconquest.carcosa;

import java.util.ArrayList;
import java.util.Locale;

import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import io.reconquest.carcosa.lib.Repo;

public class RepoList extends BaseAdapter implements Filterable {
  MainActivity activity;
  ArrayList<Repo> repos;
  ArrayList<RepoTokenList> tokens = new ArrayList<RepoTokenList>();
  RepoListFilter filter = new RepoListFilter();

  RepoList(MainActivity activity, ArrayList<Repo> repos) {
    this.activity = activity;
    this.repos = repos;

    for (int i = 0; i < repos.size(); i++) {
      RepoTokenList tokenList = new RepoTokenList(activity, repos.get(i).tokens);
      tokens.add(tokenList);
    }
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
  public Filter getFilter() {
    return filter;
  }

  @Override
  public View getView(int position, View view, ViewGroup container) {
    RepoListViewHolder holder;

    Repo repo = getItem(position);
    if (view == null) {
      view = activity.getLayoutInflater().inflate(R.layout.repo_list_item, container, false);

      RepoTokenList tokenList = tokens.get(position);
      ((ListView) view.findViewById(R.id.repo_token_list)).setAdapter(tokenList);

      holder = new RepoListViewHolder(view);

      view.setTag(holder);
    } else {
      holder = (RepoListViewHolder) view.getTag();
    }

    holder.draw(tokens.get(position), repo);

    return view;
  }

  private class RepoListViewHolder {
    UI ui;
    View view;

    int tokenItemHeight;

    public RepoListViewHolder(View view) {
      this.ui = new UI(view);
      this.view = view;
    }

    public void draw(RepoTokenList adapter, Repo repo) {
      ListView tokensView = (ListView) view.findViewById(R.id.repo_token_list);

      ui.text(R.id.repo_list_item_name, repo.name);
      ui.text(R.id.repo_list_item_sync_stat_date, repo.syncStat.date);

      if (repo.syncStat.added > 0) {
        ui.text(
            R.id.repo_list_item_sync_stat_added,
            String.format(Locale.getDefault(), "+%d", repo.syncStat.added));
        ui.show(R.id.repo_list_item_sync_stat_added);
      }

      if (repo.syncStat.deleted > 0) {
        ui.text(
            R.id.repo_list_item_sync_stat_deleted,
            String.format(Locale.getDefault(), "−%d", repo.syncStat.deleted));
        ui.show(R.id.repo_list_item_sync_stat_deleted);
      }

      if (repo.syncStat.added + repo.syncStat.deleted == 0) {
        ui.show(R.id.repo_list_item_sync_stat_uptodate);
      }

      if (repo.isLocked) {
        ui.show(R.id.repo_list_item_unlock);
        ui.onClick(R.id.repo_list_item_unlock, new UnlockButton(repo));
      }

      ViewGroup.LayoutParams params = tokensView.getLayoutParams();

      tokensView.measure(
          MeasureSpec.makeMeasureSpec(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED),
          MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

      int tokenItemHeight = tokensView.getMeasuredHeight();

      params.height = tokenItemHeight * adapter.getCount();
      params.height += (tokensView.getDividerHeight() * (adapter.getCount()));

      tokensView.setLayoutParams(params);
    }
  }

  private class RepoListFilter extends Filter {
    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
      for (int i = 0; i < tokens.size(); i++) {
        tokens.get(i).getFilter().filter(constraint);
      }
      return new FilterResults();
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
      notifyDataSetChanged();
    }
  }

  public class UnlockButton implements OnClickListener {
    Repo repo;

    UnlockButton(Repo repo) {
      this.repo = repo;
    }

    public void onClick(View v) {
      activity.gotoRepoScreen(repo);
    }
  }
}
