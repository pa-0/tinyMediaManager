/*
 * Copyright 2012 - 2025 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.ui.components.treetable;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.tree.TmmTreeModel;

import ca.odell.glazedlists.swing.SortableRenderer;

public class TmmTreeTableComparatorChooser {
  /** The TmmTreeTable to install the comparator chooser for */
  private final TmmTreeTable                 treeTable;

  private final ITmmTreeTableSortingStrategy sortingStrategy;

  /** The header renderer which decorates an underlying renderer (the table header's default renderer) with a sort arrow icon. */
  private SortArrowHeaderRenderer            sortArrowHeaderRenderer;

  /** when somebody clicks on the header, update the sorting state */
  private final HeaderClickHandler           headerClickHandler;

  public static void install(TmmTreeTable treeTable) {
    new TmmTreeTableComparatorChooser(treeTable);
  }

  private TmmTreeTableComparatorChooser(TmmTreeTable treeTable) {
    this.treeTable = treeTable;

    this.sortingStrategy = (ITmmTreeTableSortingStrategy) ((TmmTreeModel<?>) treeTable.getTreeTableModel().getTreeModel()).getDataProvider()
        .getTreeComparator();

    // wrap the default table header with logic that decorates it with a sorting icon
    wrapDefaultTableHeaderRenderer();

    // also wrap all column renderers
    wrapColumnHeaderRenderer();

    // install the sorting strategy to interpret clicks
    headerClickHandler = new HeaderClickHandler(treeTable);
  }

  /**
   * A method to wrap the default renderer of the JTableHeader if it does not appear to be wrapped already. This is particularly useful when the UI
   * delegate of the table header changes.
   */
  private void wrapDefaultTableHeaderRenderer() {
    final TableCellRenderer defaultRenderer = treeTable.getTableHeader().getDefaultRenderer();
    final Class<?> defaultRendererType = defaultRenderer == null ? null : defaultRenderer.getClass();

    // if the renderer does not appear to be wrapped, do it
    if (defaultRendererType != SortArrowHeaderRenderer.class && defaultRendererType != null) {
      // decorate the default table header renderer with sort arrows
      sortArrowHeaderRenderer = new SortArrowHeaderRenderer(defaultRenderer);
      treeTable.getTableHeader().setDefaultRenderer(sortArrowHeaderRenderer);
    }
  }

  /**
   * A method to wrap all set column header renderers
   */
  private void wrapColumnHeaderRenderer() {
    // also install the renderer for all columns
    Iterator<TableColumn> columns = treeTable.getTableHeader().getColumnModel().getColumns().asIterator();
    while (columns.hasNext()) {
      TableColumn column = columns.next();

      TableCellRenderer columnHeaderRenderer = column.getHeaderRenderer();
      if (columnHeaderRenderer != null && columnHeaderRenderer.getClass() != SortArrowHeaderRenderer.class) {
        column.setHeaderRenderer(new SortArrowHeaderRenderer(columnHeaderRenderer));
      }
    }
  }

  /**
   * Handle clicks to the table's header by adjusting the sorting state.
   */
  private class HeaderClickHandler extends MouseAdapter {
    private final JTable table;
    private boolean      mouseEventIsPerformingPopupTrigger = false;

    public HeaderClickHandler(TmmTreeTable treeTable) {
      this.table = treeTable;
      treeTable.getTableHeader().addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      // if the MouseEvent is popping up a context menu, do not sort
      if (mouseEventIsPerformingPopupTrigger)
        return;

      // if the cursor indicates we're resizing columns, do not sort
      if (table.getTableHeader().getCursor() == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR))
        return;

      // check if there is any other reason to ignore this MouseEvent
      if (e.getButton() != MouseEvent.BUTTON1) {
        return;
      }

      final TableColumnModel columnModel = table.getColumnModel();
      final int viewColumn = columnModel.getColumnIndexAtX(e.getX());
      final int column = table.convertColumnIndexToModel(viewColumn);
      final int clicks = e.getClickCount();

      if (clicks >= 1 && column != -1) {
        final boolean shift = e.isShiftDown();
        final boolean control = e.isControlDown() || e.isMetaDown();

        sortingStrategy.columnClicked(column, shift, control);
        treeTable.updateFiltering();
        // make sure the header also gets redrawn (this may not happen if the structure does not change)
        treeTable.getTableHeader().repaint();
      }
    }

    /**
     * Keep track of whether the mouse is triggering a popup, so we can avoid sorting the table when the poor user just wants to show a context menu.
     */
    @Override
    public void mousePressed(MouseEvent mouseEvent) {
      this.mouseEventIsPerformingPopupTrigger = mouseEvent.isPopupTrigger();
    }

    public void dispose() {
      table.getTableHeader().removeMouseListener(this);
    }
  }

  /**
   * The SortArrowHeaderRenderer simply delegates most of the rendering to a given delegate renderer, and adds an icon to indicate sorting direction.
   * This allows TableComparatorChooser to work equally well with any custom TableCellRenderers that are used as the default table header renderer.
   *
   * <p>
   * This class fails to add indicator arrows on table headers where the default table header render does not return a JLabel or does not implement
   * {@link SortableRenderer}.
   *
   * <p>
   * We implement UIResource here so that changes to the UI delegate of the JTableHeader will replace our renderer with a new one that is appropriate
   * for the new LaF. We, in turn, react to the change of UI delegates by *re-wrapping* the new default renderer with our sort icon injection logic.
   */
  class SortArrowHeaderRenderer implements TableCellRenderer, UIResource {

    /** the renderer to which we delegate */
    private TableCellRenderer delegateRenderer;

    /**
     * Creates a new SortArrowHeaderRenderer that attempts to decorate the given <code>delegateRenderer</code> which a sorting icon.
     */
    public SortArrowHeaderRenderer(TableCellRenderer delegateRenderer) {
      this.delegateRenderer = delegateRenderer;
    }

    /**
     * Returns the delegate renderer that is decorated with sort arrows.
     */
    public TableCellRenderer getDelegateRenderer() {
      return delegateRenderer;
    }

    /**
     * Renders the header in the default way but with the addition of an icon to indicate sorting state.
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      // if column index is negative, just call the delegate renderer
      // this is a special case for JideTable with nested table columns
      if (column < 0)
        return getDelegateTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      final int modelColumn = table.convertColumnIndexToModel(column);
      final Icon sortIcon = getSortIcon(modelColumn);
      final Component rendered;

      // 1. look for our custom SortableRenderer interface
      if (delegateRenderer instanceof SortableRenderer) {
        ((SortableRenderer) delegateRenderer).setSortIcon(sortIcon);
        rendered = getDelegateTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // 2. Otherwise check whether the rendered component is a JLabel (this is the case of the default header renderer)
      }
      else {
        rendered = getDelegateTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // we check for a JLabel rather than a DefaultTableCellRenderer to support WinLAF,
        // which installs a decorator over the DefaultTableCellRenderer
        if (rendered instanceof JLabel) {
          final JLabel label = (JLabel) rendered;
          label.setIcon(sortIcon);
          label.setHorizontalTextPosition(SwingConstants.LEADING);
        }
      }

      return rendered;
    }

    private Icon getSortIcon(int column) {
      ITmmTreeTableSortingStrategy.SortDirection sortDirection = sortingStrategy.getSortDirection(column);

      if (sortDirection == ITmmTreeTableSortingStrategy.SortDirection.ASCENDING) {
        return IconManager.SORT_UP_PRIMARY;
      }
      else if (sortDirection == ITmmTreeTableSortingStrategy.SortDirection.DESCENDING) {
        return IconManager.SORT_DOWN_PRIMARY;
      }

      return null;
    }

    /**
     * Attempts to retrieve the decorated Component from the delegate renderer. If a RuntimeException occurs, this method replaces the delegate
     * renderer with a {@link DefaultTableCellRenderer} and requests the Component from it. This exists because our decorating approach is the victim
     * of a SUN bug in WindowsTableHeaderUI: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6429812
     *
     * See also more information reported by Eric Burke here: http://stuffthathappens.com/blog/2007/10/02/rich-client-developers-avoid-java-6/
     */
    private Component getDelegateTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      try {
        return delegateRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      }
      catch (RuntimeException e) {
        delegateRenderer = new DefaultTableCellRenderer();
        return delegateRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      }
    }
  }
}
