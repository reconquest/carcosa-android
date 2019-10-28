package io.reconquest.carcosa;

import java.util.ArrayList;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import io.reconquest.carcosa.lib.Repo;
import io.reconquest.carcosa.lib.Token;

public class SecretsList extends BaseAdapter {
  final int ITEM_HIDDEN = 0;
  final int ITEM_REPO = 1;
  final int ITEM_TOKEN = 2;

  public class Item {
    Object object;
    boolean visible;

    Item(Object object) {
      this.object = object;
      this.visible = true;
    }
  }

  MainActivity activity;

  ArrayList<Object> items;
  String constraint;

  SecretsList(MainActivity activity, ArrayList<Repo> repos) {
    this.activity = activity;
    this.items = new ArrayList<Object>();

    for (Repo repo : repos) {
      items.add(repo);

      for (Token token : repo.tokens) {
        items.add(token);
      }
    }
  }

  public void filter(String constraint) {
    this.constraint = constraint;

    notifyDataSetInvalidated();
  }

  public int getViewTypeCount() {
    return 3;
  }

  @Override
  public int getItemViewType(int i) {
    if (items.get(i) instanceof Repo) {
      return ITEM_REPO;
    } else {
      if (constraint != null) {
        if (((Token) items.get(i)).name.toLowerCase().contains(constraint)) {
          return ITEM_TOKEN;
        } else {
          return ITEM_HIDDEN;
        }
      }

      return ITEM_TOKEN;
    }
  }

  @Override
  public int getCount() {
    return items.size();
  }

  @Override
  public Object getItem(int i) {
    return items.get(i);
  }

  @Override
  public long getItemId(int i) {
    return i;
  }

  @Override
  public View getView(int position, View view, ViewGroup group) {
    Object item = getItem(position);

    LayoutInflater inflater = activity.getLayoutInflater();

    switch (getItemViewType(position)) {
      case ITEM_HIDDEN:
        if (view == null) {
          view = new View(activity.getApplicationContext());
        }

        break;

      case ITEM_REPO:
        {
          RepoViewHolder holder;
          if (view == null) {
            view = inflater.inflate(R.layout.repo_list_item, null);
            holder = new RepoViewHolder(activity, view);
            view.setTag(holder);
          } else {
            holder = (RepoViewHolder) view.getTag();
          }

          holder.draw((Repo) item);
        }

        break;

      case ITEM_TOKEN:
        {
          TokenViewHolder holder;
          if (view == null) {
            view = inflater.inflate(R.layout.repo_token_list_item, null);
            holder = new TokenViewHolder(activity, view);
            view.setTag(holder);
          } else {
            holder = (TokenViewHolder) view.getTag();
          }

          holder.draw((Token) item);
        }

        break;
    }

    return view;
  }
}
