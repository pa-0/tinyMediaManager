package org.tinymediamanager.ui.components.treetable;

public interface ITmmTreeTableSortingStrategy {
  enum SortDirection {
    ASCENDING,
    DESCENDING
  }

  void columnClicked(int column, boolean shift, boolean control);

  SortDirection getSortDirection(int column);

  void fromString(String stringEncoded);
}
