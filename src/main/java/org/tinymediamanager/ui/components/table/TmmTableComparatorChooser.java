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
package org.tinymediamanager.ui.components.table;

import java.awt.AWTEventMulticaster;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.tinymediamanager.ui.IconManager;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.impl.gui.SortingStrategy;
import ca.odell.glazedlists.swing.AdvancedTableModel;
import ca.odell.glazedlists.swing.SortableRenderer;

/**
 * basically a copy of {@link ca.odell.glazedlists.swing.TableComparatorChooser} but with hidpi icons
 * 
 * @param <E>
 * @author Jesse Wilson, Manuel Laggner
 */
public class TmmTableComparatorChooser<E> extends AbstractTableComparatorChooser<E> {

  /** the sort icons to use */
  private static final Icon[]        ICONS                = loadIcons();

  /**
   * the header renderer which decorates an underlying renderer (the table header's default renderer) with a sort arrow icon.
   */
  private SortArrowHeaderRenderer    sortArrowHeaderRenderer;

  /** listen for UI delegate changes to the table header */
  private final TableHeaderUIHandler tableHeaderUIHandler = new TableHeaderUIHandler();

  /** listen for table and property change events */
  private final TableModelHandler    tableModelHandler    = new TableModelHandler();

  /** the table being sorted */
  private JTable                     table;

  /** listeners to sort change events */
  private ActionListener             sortListener;

  /** when somebody clicks on the header, update the sorting state */
  private final HeaderClickHandler   headerClickHandler;

  /**
   * Creates and installs a TableComparatorChooser.
   *
   * @param table
   *          the table with headers that can be clicked on
   * @param sortedList
   *          the sorted list to update
   * @param strategy
   *          an implementations of {@link ca.odell.glazedlists.impl.gui.SortingStrategy}, typically one of
   *          <ul>
   *          <li>{@link ca.odell.glazedlists.gui.AbstractTableComparatorChooser#SINGLE_COLUMN}
   *          <li>{@link ca.odell.glazedlists.gui.AbstractTableComparatorChooser#MULTIPLE_COLUMN_MOUSE}
   *          <li>{@link ca.odell.glazedlists.gui.AbstractTableComparatorChooser#MULTIPLE_COLUMN_KEYBOARD}
   *          <li>{@link ca.odell.glazedlists.gui.AbstractTableComparatorChooser#MULTIPLE_COLUMN_MOUSE_WITH_UNDO}
   * @param tableFormat
   *          the TableFormat providing the columns for the table
   */
  protected TmmTableComparatorChooser(JTable table, SortedList<E> sortedList, Object strategy, TableFormat<? super E> tableFormat) {
    super(sortedList, tableFormat);
    validateSortingStrategy(strategy);

    // save the Swing-specific state
    this.table = table;
    this.table.addPropertyChangeListener("model", tableModelHandler);
    this.table.getTableHeader().addPropertyChangeListener("UI", tableHeaderUIHandler);

    // wrap the default table header with logic that decorates it with a sorting icon
    wrapDefaultTableHeaderRenderer();

    // also wrap all column renderers
    wrapColumnHeaderRenderer();

    // listen for events on the specified table
    table.getModel().addTableModelListener(tableModelHandler);

    // install the sorting strategy to interpret clicks
    headerClickHandler = new HeaderClickHandler(table, (SortingStrategy) strategy);
  }

  /**
   * create the icon array in the order glazedlists expects:
   *
   * <ul>
   * <li>unsorted</li>
   * <li>primary_sorted</li>
   * <li>primary_sorted_reverse</li>
   * <li>primary_sorted_alternate</li>
   * <li>primary_sorted_alternate_reverse</li>
   * <li>secondary_sorted</li>
   * <li>secondary_sorted_reverse</li>
   * <li>secondary_sorted_alternate</li>
   * <li>secondary_sorted_alternate_reverse</li>
   *
   * @return the Icon array
   */
  private static Icon[] loadIcons() {
    Icon[] icons = new Icon[9];

    icons[0] = null;
    icons[1] = icons[3] = IconManager.SORT_UP_PRIMARY;
    icons[2] = icons[4] = IconManager.SORT_DOWN_PRIMARY;
    icons[5] = icons[7] = IconManager.SORT_UP_SECONDARY;
    icons[6] = icons[8] = IconManager.SORT_DOWN_SECONDARY;

    return icons;
  }

  /**
   * A method to wrap the default renderer of the JTableHeader if it does not appear to be wrapped already. This is particularly useful when the UI
   * delegate of the table header changes.
   */
  private void wrapDefaultTableHeaderRenderer() {
    final TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
    final Class<?> defaultRendererType = defaultRenderer == null ? null : defaultRenderer.getClass();

    // if the renderer does not appear to be wrapped, do it
    if (defaultRendererType != SortArrowHeaderRenderer.class && defaultRendererType != null) {
      // decorate the default table header renderer with sort arrows
      sortArrowHeaderRenderer = new SortArrowHeaderRenderer(defaultRenderer);
      table.getTableHeader().setDefaultRenderer(sortArrowHeaderRenderer);
    }
  }

  /**
   * A method to wrap all set column header renderers
   */
  private void wrapColumnHeaderRenderer() {
    // also install the renderer for all columns
    Iterator<TableColumn> columns = table.getTableHeader().getColumnModel().getColumns().asIterator();
    while (columns.hasNext()) {
      TableColumn column = columns.next();

      TableCellRenderer columnHeaderRenderer = column.getHeaderRenderer();
      if (columnHeaderRenderer != null && columnHeaderRenderer.getClass() != SortArrowHeaderRenderer.class) {
        column.setHeaderRenderer(new SortArrowHeaderRenderer(columnHeaderRenderer));
      }
    }
  }

  /**
   * Installs a new TableComparatorChooser that responds to clicks on the header of the specified table and uses them to sort the specified
   * <code>sortedList</code> by delegating to the given <code>strategy</code> If at any time the table should no longer sort, the behaviour can be
   * removed calling {@link #dispose()} on the object returned by this method.
   *
   * <p>
   * This method assumes that the JTable is backed by an AdvancedTableModel and it is from that AdvancedTableModel that the TableFormat should be
   * extracted. This is, by far, the typical case and so we provide this simpler install method for convenience.
   *
   * <p>
   * Note, that the default table header renderer will be wrapped with a {@link TmmTableComparatorChooser.SortArrowHeaderRenderer}, which takes care
   * of rendering the sort icons. Please see the docu of {@link #createSortArrowHeaderRenderer(TableCellRenderer)} for details how this works.
   *
   * @param table
   *          the table with headers that can be clicked on
   * @param sortedList
   *          the sorted list to update
   * @param strategy
   *          an implementations of {@link SortingStrategy}, typically one of
   *          <ul>
   *          <li>{@link AbstractTableComparatorChooser#SINGLE_COLUMN}
   *          <li>{@link AbstractTableComparatorChooser#MULTIPLE_COLUMN_MOUSE}
   *          <li>{@link AbstractTableComparatorChooser#MULTIPLE_COLUMN_KEYBOARD}
   *          <li>{@link AbstractTableComparatorChooser#MULTIPLE_COLUMN_MOUSE_WITH_UNDO}
   *          </ul>
   * @return TableComparatorChooser object that is responsible for translating mouse clicks on the table header into sorting actions on the
   *         sortedList.
   */
  public static <E> TmmTableComparatorChooser<E> install(JTable table, SortedList<E> sortedList, Object strategy) {
    return install(table, sortedList, strategy, ((AdvancedTableModel<E>) table.getModel()).getTableFormat());
  }

  /**
   * Installs a new TableComparatorChooser that responds to clicks on the header of the specified table and uses them to sort the specified
   * <code>sortedList</code> by delegating to the given <code>strategy</code> If at any time the table should no longer sort, the behaviour can be
   * removed calling {@link #dispose()} on the object returned by this method.
   *
   * <p>
   * This method makes no assumptions about the TableModel implementation that backs the JTable. As such, it requires the TableFormat as an explicit
   * parameter and expects the TableFormat to be constant (i.e. never changes) for the life of the TableComparatorChooser.
   *
   * <p>
   * Note, that the default table header renderer will be wrapped with a {@link TmmTableComparatorChooser.SortArrowHeaderRenderer}, which takes care
   * of rendering the sort icons. Please see the docu of {@link #createSortArrowHeaderRenderer(TableCellRenderer)} for details how this works.
   *
   * @param table
   *          the table with headers that can be clicked on
   * @param tableFormat
   *          the TableFormat providing the columns for the table
   * @param sortedList
   *          the sorted list to update
   * @param strategy
   *          an implementations of {@link SortingStrategy}, typically one of
   *          <ul>
   *          <li>{@link AbstractTableComparatorChooser#SINGLE_COLUMN}
   *          <li>{@link AbstractTableComparatorChooser#MULTIPLE_COLUMN_MOUSE}
   *          <li>{@link AbstractTableComparatorChooser#MULTIPLE_COLUMN_KEYBOARD}
   *          <li>{@link AbstractTableComparatorChooser#MULTIPLE_COLUMN_MOUSE_WITH_UNDO}
   *          </ul>
   * @return TableComparatorChooser object that is responsible for translating mouse clicks on the table header into sorting actions on the
   *         sortedList.
   */
  public static <E> TmmTableComparatorChooser<E> install(JTable table, SortedList<E> sortedList, Object strategy,
      TableFormat<? super E> tableFormat) {
    return new TmmTableComparatorChooser<>(table, sortedList, strategy, tableFormat);
  }

  /**
   * Ensures the given <code>strategy</code> is an accepted value. It is possible for people to define their own sorting strategies, so this
   * validation can only ensure that the given <code>strategy</code> implements the {@link SortingStrategy} interface.
   *
   * @throws IllegalArgumentException
   *           if <code>strategy</code> is not an accepted value
   */
  private static void validateSortingStrategy(Object strategy) {
    if (!(strategy instanceof SortingStrategy)) {
      throw new IllegalArgumentException("Unrecognized sorting strategy, \"" + strategy
          + "\", use one of AbstractTableComparatorChooser.SINGLE_COLUMN, AbstractTableComparatorChooser.MULTIPLE_COLUMN_MOUSE, or AbstractTableComparatorChooser.MULTIPLE_COLUMN_KEYBOARD");
    }
  }

  /**
   * Registers the specified {@link ActionListener} to receive notification whenever the {@link JTable} is sorted by this
   * {@link TmmTableComparatorChooser}.
   */
  public void addSortActionListener(ActionListener sortActionListener) {
    sortListener = AWTEventMulticaster.add(sortListener, sortActionListener);
  }

  /**
   * Deregisters the specified {@link ActionListener} to no longer receive action events.
   */
  public void removeSortActionListener(ActionListener sortActionListener) {
    sortListener = AWTEventMulticaster.remove(sortListener, sortActionListener);
  }

  /**
   * Decorates and returns the given <code>delegateRenderer</code> with functionality that attempts to install a sorting icon into the Component
   * returned by the <code>delegateRenderer</code>. In particular, the <code>delegateRenderer</code> will be decorated with a sorting icon in one of
   * two scenarios:
   *
   * <ul>
   * <li>the delegateRenderer implements {@link SortableRenderer} - in this case {@link SortableRenderer#setSortIcon setSortIcon} is called on the
   * delegateRenderer and it is expected to place the icon anywhere it desires on the Component it returns. This allows maximum flexibility when
   * displaying the sort icon.
   *
   * <li>the Component returned by the delegateRenderer is a JLabel - in this case {@link JLabel#setIcon setIcon} is called on the JLabel with the
   * sort icon. This caters to the typical case when a {@link javax.swing.table.DefaultTableCellRenderer} is used as the delegateRenderer.
   * </ul>
   *
   * If neither of these scenarios are true of the given delegateRenderer then no sort indicator arrows will be added to the renderer's component.
   *
   * @param delegateRenderer
   *          the TableCellRenderer acting as a table header renderer and to which a sort icon should be added
   * @return a TableCellRenderer that attempts to decorate the given <code>delegateRenderer</code> with a sort icon
   */
  public TableCellRenderer createSortArrowHeaderRenderer(TableCellRenderer delegateRenderer) {
    return new SortArrowHeaderRenderer(delegateRenderer);
  }

  /**
   * Examines the current {@link Comparator} of the SortedList and adds icons to the table header renderers in response.
   *
   * <p>
   * To do this, clicks are injected into each of the corresponding <code>ColumnClickTracker</code>s.
   */
  @Override
  protected void redetectComparator(Comparator<? super E> currentComparator) {
    super.redetectComparator(currentComparator);

    // force the table header to redraw itself
    table.getTableHeader().revalidate();
    table.getTableHeader().repaint();
  }

  /**
   * Updates the comparator in use and applies it to the table.
   */
  @Override
  protected final void rebuildComparator() {
    super.rebuildComparator();

    // force the table header to redraw itself
    table.getTableHeader().revalidate();
    table.getTableHeader().repaint();

    // notify interested listeners that the sorting has changed
    if (sortListener != null) {
      sortListener.actionPerformed(new ActionEvent(this, 0, "sort"));
    }
  }

  /**
   * Gets the sorting style currently applied to the specified column.
   */
  @Override
  protected final int getSortingStyle(int column) {
    return super.getSortingStyle(table.convertColumnIndexToModel(column));
  }

  /**
   * Determines if the specified mouse event shall be handled by this {@link TmmTableComparatorChooser}. The default implementation handles only
   * clicks with the left mouse button. Extending classes can customize which mouse events the table comparator chooser responds to by overriding this
   * method.
   *
   * <p>
   * As of 2005/12/20, this method is no longer called when the corresponding mouse press event was a popup trigger. In effect, if this is a
   * right-click on Windows or a 'control-click' on the Mac.
   *
   * <p>
   * As of 2008/02/05, this method is no longer called when the Cursor over the JTableHeader indicates a column resize is expected to take place,
   * rather than a change in sort.
   */
  protected boolean isSortingMouseEvent(MouseEvent e) {
    // skip the sort if it's not button 1
    if (e.getButton() != MouseEvent.BUTTON1) {
      return false;
    }

    // we have no reason to dislike this mouse event!
    return true;
  }

  /**
   * Releases the resources consumed by this {@link TmmTableComparatorChooser} so that it may eventually be garbage collected.
   *
   * <p>
   * A {@link TmmTableComparatorChooser} will be garbage collected without a call to {@link #dispose()}, but not before its source {@link EventList}
   * is garbage collected. By calling {@link #dispose()}, you allow the {@link TmmTableComparatorChooser} to be garbage collected before its source
   * {@link EventList}. This is necessary for situations where an {@link TmmTableComparatorChooser} is short-lived but its source {@link EventList} is
   * long-lived.
   *
   * <p>
   * <strong><font color="#FF0000">Warning:</font></strong> It is an error to call any method on a {@link TmmTableComparatorChooser} after it has been
   * disposed.
   */
  @Override
  public void dispose() {
    super.dispose();
    headerClickHandler.dispose();

    // if the default renderer within the table header is our sort arrow renderer,
    // uninstall it by restoring the table header's original default renderer
    if (table.getTableHeader().getDefaultRenderer() == sortArrowHeaderRenderer) {
      table.getTableHeader().setDefaultRenderer(sortArrowHeaderRenderer.getDelegateRenderer());
    }

    // remove our listeners from the table's header and model
    table.getModel().removeTableModelListener(tableModelHandler);
    table.removePropertyChangeListener("model", tableModelHandler);
    table.getTableHeader().removePropertyChangeListener("UI", tableHeaderUIHandler);

    // null out our table reference for safety's sake
    table = null;
  }

  /**
   * Nested Listener class handles changes in the UI delegate for the table's header. It responds by rewrapping the default renderer for the table
   * header, if it was replaced in the course of installing the new TableHeaderUI.
   */
  private class TableHeaderUIHandler implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      wrapDefaultTableHeaderRenderer();
    }
  }

  /**
   * Nested Listener class handles TableModelEvents and PropertyChangeEvents. TableModelEvents tell us when the TableModel's data changes in place.
   * PropertyChangeEvents tell us when the TableModel has been replaced.
   */
  private class TableModelHandler implements TableModelListener, PropertyChangeListener {

    /**
     * This method is only called when the TableModel of the JTable is changed. It allows us to stop listening to the previous EventTableModel and
     * start listening to the new one. It also resets the sorting state of this TableComparatorChooser.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      // get the two EventTableModels
      final AdvancedTableModel<E> oldModel = evt.getOldValue() instanceof AdvancedTableModel ? (AdvancedTableModel<E>) evt.getOldValue() : null;
      final AdvancedTableModel<E> newModel = evt.getNewValue() instanceof AdvancedTableModel ? (AdvancedTableModel<E>) evt.getNewValue() : null;

      // stop listening for TableModelEvents in the oldModel and start for the newModel, if possible
      if (oldModel != null) {
        oldModel.removeTableModelListener(this);
      }
      if (newModel != null) {
        newModel.addTableModelListener(this);

        // the table structure has probably changed due to the new EventTableModel
        // so we reset the TableFormat (which clears the sorting state)
        setTableFormat(newModel.getTableFormat());
      }
    }

    /**
     * When the number of columns changes in the table, we need to clear the comparators and columns.
     */
    @Override
    public void tableChanged(TableModelEvent event) {
      if (event.getFirstRow() == TableModelEvent.HEADER_ROW && event.getColumn() == TableModelEvent.ALL_COLUMNS) {
        if (table.getModel() instanceof AdvancedTableModel) {
          // the table structure may have changed due to a change in the table format
          // so we conservatively reset the TableFormat on this TableComparatorChooser
          setTableFormat(((AdvancedTableModel<E>) table.getModel()).getTableFormat());
        }
      }

      // if the comparator has changed
      final Comparator<? super E> currentComparator = sortedList.getComparator();
      if (currentComparator != sortedListComparator) {
        redetectComparator(currentComparator);
      }
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

      final Icon sortIcon = ICONS[getSortingStyle(column)];
      final Component rendered;

      // 1. look for our custom SortableRenderer interface
      if (delegateRenderer instanceof SortableRenderer sortableRenderer) {
        sortableRenderer.setSortIcon(sortIcon);
        rendered = getDelegateTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // 2. Otherwise check whether the rendered component is a JLabel (this is the case of the default header renderer)
      }
      else {
        rendered = getDelegateTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // we check for a JLabel rather than a DefaultTableCellRenderer to support WinLAF,
        // which installs a decorator over the DefaultTableCellRenderer
        if (rendered instanceof JLabel label) {
          label.setIcon(sortIcon);
          label.setHorizontalTextPosition(SwingConstants.LEADING);
        }
      }

      return rendered;
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

  /**
   * Handle clicks to the table's header by adjusting the sorting state.
   */
  private class HeaderClickHandler extends MouseAdapter {
    private final JTable          table;
    private final SortingStrategy delegate;
    private boolean               mouseEventIsPerformingPopupTrigger = false;

    public HeaderClickHandler(JTable table, SortingStrategy delegate) {
      this.table = table;
      this.delegate = delegate;
      table.getTableHeader().addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      // if the MouseEvent is popping up a context menu, do not sort
      if (mouseEventIsPerformingPopupTrigger) {
        return;
      }

      // if the cursor indicates we're resizing columns, do not sort
      if (table.getTableHeader().getCursor() == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)) {
        return;
      }

      // check if there is any other reason to ignore this MouseEvent
      if (!isSortingMouseEvent(e)) {
        return;
      }

      final TableColumnModel columnModel = table.getColumnModel();
      final int viewColumn = columnModel.getColumnIndexAtX(e.getX());
      final int column = table.convertColumnIndexToModel(viewColumn);
      final int clicks = e.getClickCount();

      if (clicks >= 1 && column != -1) {
        final boolean shift = e.isShiftDown();
        final boolean control = e.isControlDown() || e.isMetaDown();
        delegate.columnClicked(sortingState, column, clicks, shift, control);
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
}
