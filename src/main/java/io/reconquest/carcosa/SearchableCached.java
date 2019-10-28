package io.reconquest.carcosa;

import java.util.HashMap;

import android.view.View;
import io.reconquest.carcosa.UI.Searchable;

public class SearchableCached implements Searchable {
  final View root;
  HashMap<Integer, View> children;

  SearchableCached(final View root) {
    this.root = root;
    this.children = new HashMap<Integer, View>();
  }

  public View findViewById(int id) {
    View view = children.get(id);
    if (view == null) {
      view = this.root.findViewById(id);
      children.put(id, view);
    }

    return view;
  }
}
