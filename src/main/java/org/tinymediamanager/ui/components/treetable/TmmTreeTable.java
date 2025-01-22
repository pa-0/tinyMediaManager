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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.event.TreeModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.ui.ITmmUIFilter;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableColumnModel;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.tree.ITmmTreeFilter;
import org.tinymediamanager.ui.components.tree.TmmTreeDataProvider;
import org.tinymediamanager.ui.components.tree.TmmTreeModel;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;

/**
 * The class TmmTreeTable provides a combination of a tree and a table
 * 
 * @author Manuel Laggner
 */
public class TmmTreeTable extends TmmTable {

  protected final TmmTreeDataProvider<TmmTreeNode> dataProvider;
  protected final Set<ITmmTreeFilter<TmmTreeNode>> treeFilters;

  protected TmmTreeTableRenderDataProvider         renderDataProvider = null;
  protected int                                    selectedRow        = -1;
  protected Boolean                                cachedRootVisible  = true;
  protected ITmmTreeTableModel                     treeTableModel;
  protected PropertyChangeListener                 filterChangeListener;

  private int[]                                    lastEditPosition;

  public TmmTreeTable(TmmTreeDataProvider<TmmTreeNode> dataProvider, TmmTreeTableFormat<TmmTreeNode> tableFormat) {
    this.dataProvider = dataProvider;
    this.treeFilters = new CopyOnWriteArraySet<>();
    this.treeTableModel = new TmmTreeTableModel(new TmmTreeModelConnector<>(dataProvider), tableFormat);
    this.filterChangeListener = evt -> {
      updateFiltering();
      storeFilters();
    };
    setModel(treeTableModel);
    initTreeTable();
  }

  @Override
  public void addColumn(TableColumn aColumn) {
    if (aColumn.getIdentifier() == null && getModel() instanceof TmmTreeTableModel) {
      aColumn.setHeaderRenderer(new SortableIconHeaderRenderer());

      TmmTreeTableModel tableModel = ((TmmTreeTableModel) getModel());
      tableModel.setUpColumn(aColumn);
    }
    super.addColumn(aColumn);
  }

  protected void initTreeTable() {
    getSelectionModel().addListSelectionListener(e -> {
      if (getSelectedRowCount() == 1) {
        selectedRow = getSelectedRow();
      }
      else {
        selectedRow = -1;
      }
    });

    // setTableHeader(createTableHeader());
    getTableHeader().setReorderingAllowed(false);
    // getTableHeader().setOpaque(false);
    // setOpaque(false);
    // turn off grid painting as we'll handle this manually in order to paint grid lines over the entire viewport.
    setShowGrid(false);

    // install the keyadapter for navigation
    addKeyListener(new TmmTreeTableKeyAdapter(this));
  }

  @Override
  public TableCellRenderer getCellRenderer(int row, int column) {
    int c = convertColumnIndexToModel(column);
    TableCellRenderer result;
    if (c == 0) {
      TableColumn tableColumn = getColumnModel().getColumn(column);
      TableCellRenderer renderer = tableColumn.getCellRenderer();
      if (renderer == null) {
        result = getDefaultRenderer(Object.class);
      }
      else {
        result = renderer;
      }
    }
    else {
      result = super.getCellRenderer(row, column);
    }
    return result;
  }

  /**
   * Get the RenderDataProvider which is providing text, icons and tooltips for items in the tree column. The default property for this value is null,
   * in which case standard JTable/JTree object -> icon/string conventions are used
   */
  public TmmTreeTableRenderDataProvider getRenderDataProvider() {
    return renderDataProvider;
  }

  /**
   * Set the RenderDataProvider which will provide text, icons and tooltips for items in the tree column. The default is null. If null, the data
   * displayed will be generated in the standard JTable/JTree way - calling <code>toString()</code> on objects in the tree model and using the look
   * and feel's default tree folder and tree leaf icons.
   */
  public void setRenderDataProvider(TmmTreeTableRenderDataProvider provider) {
    if (provider != renderDataProvider) {
      TmmTreeTableRenderDataProvider old = renderDataProvider;
      renderDataProvider = provider;
      firePropertyChange("renderDataProvider", old, provider);
    }
  }

  /**
   * Get the TreePathSupport object which manages path expansion for this Treetable
   */
  TmmTreeTableTreePathSupport getTreePathSupport() {
    TmmTreeTableModel mdl = getTreeTableModel();
    if (mdl != null) {
      return mdl.getTreePathSupport();
    }
    else {
      return null;
    }
  }

  public TmmTreeTableModel getTreeTableModel() {
    TableModel mdl = getModel();
    if (mdl instanceof TmmTreeTableModel) {
      return (TmmTreeTableModel) getModel();
    }
    else {
      return null;
    }
  }

  @Override
  public void setDefaultHiddenColumns() {
    if (getColumnModel() instanceof TmmTableColumnModel && getModel() instanceof TmmTreeTableModel) {
      TmmTreeTableModel tableModel = (TmmTreeTableModel) getModel();
      TmmTableFormat<TmmTreeNode> tableFormat = tableModel.getTableModel().getTableFormat();

      List<String> hiddenColumns = new ArrayList<>();

      for (int i = 0; i < tableFormat.getColumnCount(); i++) {
        if (tableFormat.isColumnDefaultHidden(i)) {
          hiddenColumns.add(tableFormat.getColumnIdentifier(i));
        }
      }

      readHiddenColumns(hiddenColumns);
    }
  }

  @Override
  protected boolean useColumnConfigurator() {
    return getModel() instanceof TmmTreeTableModel;
  }

  public DefaultMutableTreeNode getTreeNode(int row) {
    Object obj = getTreeTableModel().getValueAt(row, 0);
    if (obj instanceof DefaultMutableTreeNode) {
      return (DefaultMutableTreeNode) obj;
    }
    return null;
  }

  public void expandRow(int row) {
    expandPath(getRowPath(row));
  }

  public void collapseRow(int row) {
    collapsePath(getRowPath(row));
  }

  public void expandPath(TreePath path) {
    getTreePathSupport().expandPath(path);
  }

  public boolean isExpanded(TreePath path) {
    return getTreePathSupport().isExpanded(path);
  }

  public boolean isExpanded(int row) {
    return isExpanded(getRowPath(row));
  }

  public boolean isCollapsed(int row) {
    return !isExpanded(row);
  }

  public boolean isLeaf(TreePath path) {
    return getTreePathSupport().isLeaf(path);
  }

  public boolean isLeaf(int row) {
    return isLeaf(getRowPath(row));
  }

  public boolean isBranch(int row) {
    return !isLeaf(row);
  }

  public void collapsePath(TreePath path) {
    getTreePathSupport().collapsePath(path);
  }

  TreePath getRowPath(int row) {
    return treeTableModel.getLayout().getPathForRow(row);
  }

  TreePath[] getSelectedTreePaths() {
    return IntStream.of(getSelectedRows()).mapToObj(this::getRowPath).toArray(TreePath[]::new);
  }

  boolean isTreeColumnIndex(int column) {
    int columnIndex = convertColumnIndexToModel(column);
    return columnIndex == 0;
  }

  public final AbstractLayoutCache getLayoutCache() {
    TmmTreeTableModel model = getTreeTableModel();
    if (model != null) {
      return model.getLayout();
    }
    else {
      return null;
    }
  }

  public void setRootVisible(boolean val) {
    if (getTreeTableModel() == null) {
      cachedRootVisible = val;
    }
    if (val != isRootVisible()) {
      AbstractLayoutCache layoutCache = getLayoutCache();
      if (layoutCache != null) {
        layoutCache.setRootVisible(val);
        if (layoutCache.getRowCount() > 0) {
          TreePath rootPath = layoutCache.getPathForRow(0);
          if (null != rootPath)
            layoutCache.treeStructureChanged(new TreeModelEvent(this, rootPath));
        }
        firePropertyChange("rootVisible", !val, val); // NOI18N
      }
    }
  }

  public boolean isRootVisible() {
    if (getLayoutCache() == null) {
      return cachedRootVisible;
    }
    else {
      return getLayoutCache().isRootVisible();
    }
  }

  @Override
  public boolean editCellAt(int row, int column, EventObject e) {
    // If it was on column 0, it may be a request to expand a tree node - check for that first.
    boolean isTreeColumn = isTreeColumnIndex(column);
    if (isTreeColumn && e instanceof MouseEvent) {
      MouseEvent me = (MouseEvent) e;
      AbstractLayoutCache layoutCache = getLayoutCache();
      if (layoutCache != null) {
        TreePath path = layoutCache.getPathForRow(convertRowIndexToModel(row));
        if (path != null && !getTreeTableModel().isLeaf(path.getLastPathComponent())) {
          int handleWidth = TmmTreeTableCellRenderer.getExpansionHandleWidth();
          Insets ins = getInsets();
          int nd = path.getPathCount() - (isRootVisible() ? 1 : 2);
          if (nd < 0) {
            nd = 0;
          }
          int handleStart = ins.left + (nd * TmmTreeTableCellRenderer.getNestingWidth());
          int handleEnd = ins.left + handleStart + handleWidth;
          // Translate 'x' to position of column if non-0:
          int columnStart = getCellRect(row, column, false).x;
          handleStart += columnStart;
          handleEnd += columnStart;

          TableColumn tableColumn = getColumnModel().getColumn(column);
          if ((me.getX() > ins.left && me.getX() >= handleStart && me.getX() <= handleEnd)) {
            boolean expanded = layoutCache.isExpanded(path);
            if (!expanded) {
              getTreePathSupport().expandPath(path);

              Object ourObject = path.getLastPathComponent();
              int cCount = getTreeTableModel().getChildCount(ourObject);
              if (cCount > 0) {
                int lastRow = row;
                for (int i = 0; i < cCount; i++) {
                  Object child = getTreeTableModel().getChild(ourObject, i);
                  TreePath childPath = path.pathByAddingChild(child);
                  int childRow = layoutCache.getRowForPath(childPath);
                  childRow = convertRowIndexToView(childRow);
                  if (childRow > lastRow) {
                    lastRow = childRow;
                  }
                }
                int firstRow = row;
                Rectangle rectLast = getCellRect(lastRow, 0, true);
                Rectangle rectFirst = getCellRect(firstRow, 0, true);
                Rectangle rectFull = new Rectangle(rectFirst.x, rectFirst.y, rectLast.x + rectLast.width - rectFirst.x,
                    rectLast.y + rectLast.height - rectFirst.y);
                scrollRectToVisible(rectFull);
              }

            }
            else {
              getTreePathSupport().collapsePath(path);
            }
            return false;
          }
        }
        // It may be a request to check/uncheck a check-box
        if (checkAt(row, column, me)) {
          return false;
        }
      }
    }

    boolean res = false;
    if (!isTreeColumn || e instanceof MouseEvent && row >= 0 && isEditEvent(row, column, (MouseEvent) e)) {
      res = super.editCellAt(row, column, e);
    }
    if (res && isTreeColumn && row >= 0 && null != getEditorComponent()) {
      configureTreeCellEditor(getEditorComponent(), row, column);
    }
    if (e == null && !res && isTreeColumn) {
      // Handle SPACE
      checkAt(row, column, null);
    }
    return res;
  }

  private boolean isEditEvent(int row, int column, MouseEvent me) {
    if (me.getClickCount() > 1) {
      return true;
    }
    boolean noModifiers = me.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK;
    if (lastEditPosition != null && selectedRow == row && noModifiers && lastEditPosition[0] == row && lastEditPosition[1] == column) {

      int handleWidth = TmmTreeTableCellRenderer.getExpansionHandleWidth();
      Insets ins = getInsets();
      AbstractLayoutCache layoutCache = getLayoutCache();
      if (layoutCache != null) {
        TreePath path = layoutCache.getPathForRow(convertRowIndexToModel(row));
        int nd = path.getPathCount() - (isRootVisible() ? 1 : 2);
        if (nd < 0) {
          nd = 0;
        }
        int handleStart = ins.left + (nd * TmmTreeTableCellRenderer.getNestingWidth());
        int handleEnd = ins.left + handleStart + handleWidth;
        // Translate 'x' to position of column if non-0:
        int columnStart = getCellRect(row, column, false).x;
        handleStart += columnStart;
        handleEnd += columnStart;
        if (me.getX() >= handleEnd) {
          lastEditPosition = null;
          return true;
        }
      }
    }
    lastEditPosition = new int[] { row, column };
    return false;
  }

  protected final boolean checkAt(int row, int column, MouseEvent me) {
    TmmTreeTableRenderDataProvider render = getRenderDataProvider();
    TableCellRenderer tcr = getDefaultRenderer(Object.class);
    if (render instanceof TmmTreeTableCheckRenderDataProvider && tcr instanceof TmmTreeTableCellRenderer) {
      TmmTreeTableCheckRenderDataProvider crender = (TmmTreeTableCheckRenderDataProvider) render;
      TmmTreeTableCellRenderer ocr = (TmmTreeTableCellRenderer) tcr;
      Object value = getValueAt(row, column);
      if (value != null && crender.isCheckable(value) && crender.isCheckEnabled(value)) {
        boolean chBoxPosition = false;
        if (me == null) {
          chBoxPosition = true;
        }
        else {
          int handleWidth = TmmTreeTableCellRenderer.getExpansionHandleWidth();
          int chWidth = ocr.getTheCheckBoxWidth();
          Insets ins = getInsets();
          AbstractLayoutCache layoutCache = getLayoutCache();
          if (layoutCache != null) {
            TreePath path = layoutCache.getPathForRow(convertRowIndexToModel(row));
            int nd = path.getPathCount() - (isRootVisible() ? 1 : 2);
            if (nd < 0) {
              nd = 0;
            }
            int chStart = ins.left + (nd * TmmTreeTableCellRenderer.getNestingWidth()) + handleWidth;
            int chEnd = chStart + chWidth;

            chBoxPosition = (me.getX() > ins.left && me.getX() >= chStart && me.getX() <= chEnd);
          }
        }
        if (chBoxPosition) {
          Boolean selected = crender.isSelected(value);
          if (selected == null || Boolean.TRUE.equals(selected)) {
            crender.setSelected(value, Boolean.FALSE);
          }
          else {
            crender.setSelected(value, Boolean.TRUE);
          }
          Rectangle r = getCellRect(row, column, true);
          repaint(r.x, r.y, r.width, r.height);
          return true;
        }
      }
    }
    return false;
  }

  protected void configureTreeCellEditor(Component editor, int row, int column) {
    if (!(editor instanceof JComponent)) {
      return;
    }
    TreeCellEditorBorder b = new TreeCellEditorBorder();

    AbstractLayoutCache layoutCache = getLayoutCache();
    if (layoutCache != null) {
      TreePath path = layoutCache.getPathForRow(convertRowIndexToModel(row));
      Object o = getValueAt(row, column);
      TmmTreeTableRenderDataProvider rdp = getRenderDataProvider();
      TableCellRenderer tcr = getDefaultRenderer(Object.class);
      if (rdp instanceof TmmTreeTableCheckRenderDataProvider && tcr instanceof TmmTreeTableCellRenderer) {
        TmmTreeTableCheckRenderDataProvider crender = (TmmTreeTableCheckRenderDataProvider) rdp;
        TmmTreeTableCellRenderer ocr = (TmmTreeTableCellRenderer) tcr;
        Object value = getValueAt(row, column);
        if (value != null && crender.isCheckable(value) && crender.isCheckEnabled(value)) {
          b.checkWidth = ocr.getTheCheckBoxWidth();
          b.checkBox = ocr.setUpCheckBox(crender, value, ocr.createCheckBox());
        }
      }
      b.icon = rdp.getIcon(o);
      b.nestingDepth = Math.max(0, path.getPathCount() - (isRootVisible() ? 1 : 2));
      b.isLeaf = getTreeTableModel().isLeaf(o);
      b.isExpanded = layoutCache.isExpanded(path);

      ((JComponent) editor).setBorder(b);
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    calcRowHeight();
  }

  private void calcRowHeight() {
    // Users of themes can set an explicit row height, so check for it

    int rHeight = 20;
    // Derive a row height to accommodate the font and expand icon
    Font f = getFont();
    FontMetrics fm = getFontMetrics(f);
    int h = Math.max(fm.getHeight() + fm.getMaxDescent(), TmmTreeTableCellRenderer.getExpansionHandleHeight());
    rHeight = Math.max(rHeight, h) + 2;

    setRowHeight(rHeight);
  }

  /**
   * Returns all set tree nodes filter.
   *
   * @return a list of all set tree nodes filters
   */
  public List<ITmmTreeFilter<TmmTreeNode>> getFilters() {
    return new ArrayList<>(treeFilters);
  }

  /**
   * Removes any applied tree nodes filter.
   */
  public void clearFilter() {
    // remove our filter listener
    for (ITmmTreeFilter<TmmTreeNode> filter : treeFilters) {
      if (filter instanceof ITmmUIFilter) {
        ITmmUIFilter<?> tmmUIFilter = (ITmmUIFilter<?>) filter;

        tmmUIFilter.setFilterState(ITmmUIFilter.FilterState.INACTIVE);
        tmmUIFilter.clearFilter();
      }
      filter.removePropertyChangeListener(filterChangeListener);
    }

    updateFiltering();
    storeFilters();
  }

  /**
   * add a new filter to this tree
   *
   * @param newFilter
   *          the new filter to be added
   */
  public void addFilter(ITmmTreeFilter<TmmTreeNode> newFilter) {
    // add our filter listener
    newFilter.addPropertyChangeListener(ITmmTreeFilter.TREE_FILTER_CHANGED, filterChangeListener);
    treeFilters.add(newFilter);
  }

  /**
   * removes the given filter from this tree
   *
   * @param filter
   *          the filter to be removed
   */
  public void removeFilter(ITmmTreeFilter<TmmTreeNode> filter) {
    // remove our filter listener
    filter.removePropertyChangeListener(filterChangeListener);
    treeFilters.remove(filter);
  }

  /**
   * Updates nodes sorting and filtering for all loaded nodes.
   */
  void updateFiltering() {
    // re-evaluate active filters
    Set<ITmmTreeFilter<TmmTreeNode>> activeTreeFilters = new HashSet<>();

    for (ITmmTreeFilter<TmmTreeNode> filter : treeFilters) {
      if (filter.isActive()) {
        activeTreeFilters.add(filter);
      }
    }

    dataProvider.setTreeFilters(activeTreeFilters);

    // and update the UI
    final TreeModel model = treeTableModel.getTreeModel();
    if (model instanceof TmmTreeModel) {
      ((TmmTreeModel<?>) model).invalidateFilterCache();
      ((TmmTreeModel<?>) model).updateSortingAndFiltering();
    }

    firePropertyChange("filterChanged", null, treeFilters);
  }

  /**
   * to be overridden to provide storing of filters
   */
  public void storeFilters() {
    // to be overridden in implementations
  }

  public void setFilterValues(List<AbstractSettings.UIFilters> values) {
    if (values == null) {
      values = Collections.emptyList();
    }

    for (ITmmTreeFilter<?> filter : treeFilters) {
      if (filter instanceof ITmmUIFilter) {
        ITmmUIFilter<?> tmmUIFilter = (ITmmUIFilter<?>) filter;
        AbstractSettings.UIFilters uiFilters = values.stream().filter(uiFilter -> uiFilter.id.equals(filter.getId())).findFirst().orElse(null);

        if (uiFilters != null) {
          tmmUIFilter.setFilterState(uiFilters.state);
          tmmUIFilter.setFilterValue(uiFilters.filterValue);
          tmmUIFilter.setFilterOption(uiFilters.option);
        }
        else {
          tmmUIFilter.setFilterState(ITmmUIFilter.FilterState.INACTIVE);
        }
      }
    }

    updateFiltering();
  }

  /**
   * set whether all filters are active or not
   *
   * @param filtersActive
   *          true if all filters should be active; false otherwise
   */
  public void setFiltersActive(boolean filtersActive) {
    final TreeModel model = treeTableModel.getTreeModel();
    if (model instanceof TmmTreeModel) {
      ((TmmTreeModel<?>) model).getDataProvider().setFiltersActive(filtersActive);
    }

    updateFiltering();
    storeFilters();
  }

  /**
   * check whether all filters are active or not
   *
   * @return true if not all filters are deaktivates
   */
  public boolean isFiltersActive() {
    final TreeModel model = treeTableModel.getTreeModel();
    if (model instanceof TmmTreeModel) {
      return ((TmmTreeModel<?>) model).getDataProvider().isFiltersActive();
    }

    return true;
  }

  /**
   * provide table cell tooltips via our table model
   *
   * @param e
   *          the mouse event
   * @return the tooltip or null
   */
  public String getToolTipText(@NotNull MouseEvent e) {
    if (!(getModel() instanceof TmmTreeTableModel)) {
      return null;
    }

    Point p = e.getPoint();
    int rowIndex = rowAtPoint(p);
    int colIndex = columnAtPoint(p);
    int realColumnIndex = convertColumnIndexToModel(colIndex) - 1; // first column is the tree

    if (colIndex == 0) {
      // tree
      return super.getToolTipText(e);
    }
    else if (colIndex > 0) {
      // table
      TmmTreeTableModel treeTableModel = ((TmmTreeTableModel) getModel());
      ConnectorTableModel tableModel = treeTableModel.getTableModel();

      return tableModel.getTooltipAt(rowIndex, realColumnIndex);
    }

    return null;
  }

  private static class TreeCellEditorBorder implements Border {
    private final Insets insets      = new Insets(0, 0, 0, 0);
    private final int    iconTextGap = new JLabel().getIconTextGap();

    private boolean      isLeaf;
    private boolean      isExpanded;
    private Icon         icon;
    private int          nestingDepth;
    private int          checkWidth;
    private JCheckBox    checkBox;

    @Override
    public Insets getBorderInsets(Component c) {
      insets.left = (nestingDepth * TmmTreeTableCellRenderer.getNestingWidth()) + TmmTreeTableCellRenderer.getExpansionHandleWidth() + 1;
      insets.left += checkWidth + ((icon != null) ? icon.getIconWidth() + iconTextGap : 0);
      insets.top = 1;
      insets.right = 1;
      insets.bottom = 1;
      return insets;
    }

    @Override
    public boolean isBorderOpaque() {
      return false;
    }

    @Override
    public void paintBorder(Component c, java.awt.Graphics g, int x, int y, int width, int height) {
      int iconY;
      int iconX = nestingDepth * TmmTreeTableCellRenderer.getNestingWidth();
      if (!isLeaf) {
        Icon expIcon = isExpanded ? TmmTreeTableCellRenderer.getExpandedIcon() : TmmTreeTableCellRenderer.getCollapsedIcon();
        if (expIcon.getIconHeight() < height) {
          iconY = (height / 2) - (expIcon.getIconHeight() / 2);
        }
        else {
          iconY = 0;
        }
        expIcon.paintIcon(c, g, iconX, iconY);
      }
      iconX += TmmTreeTableCellRenderer.getExpansionHandleWidth() + 1;

      if (null != checkBox) {
        java.awt.Graphics chbg = g.create(iconX, y, checkWidth, height);
        checkBox.paint(chbg);
        chbg.dispose();
      }
      iconX += checkWidth;

      if (null != icon) {
        if (icon.getIconHeight() < height) {
          iconY = (height / 2) - (icon.getIconHeight() / 2);
        }
        else {
          iconY = 0;
        }
        icon.paintIcon(c, g, iconX, iconY);
      }
    }
  }

  public boolean isAdjusting() {
    return ((TmmTreeModel) treeTableModel.getTreeModel()).isAdjusting();
  }

  private void setSelectedRows(int[] selectedRows) {
    ((TmmTreeModel) treeTableModel.getTreeModel()).setAdjusting(true);
    clearSelection();
    ((TmmTreeModel) treeTableModel.getTreeModel()).setAdjusting(false);
    for (int selectedRow : selectedRows) {
      getSelectionModel().addSelectionInterval(selectedRow, selectedRow);
    }
  }

  private class TmmTreeModelConnector<E extends TmmTreeNode> extends TmmTreeModel {

    /**
     * Create a new instance of the TmmTreeModel for the given TmmTree and data provider
     *
     * @param dataProvider
     *          the data provider to create the model for
     */
    public TmmTreeModelConnector(final TmmTreeDataProvider<E> dataProvider) {
      super(null, dataProvider);
    }

    @Override
    public void updateSortingAndFiltering() {
      long now = System.currentTimeMillis();

      if (now > nextNodeStructureChanged) {
        // store selected nodes
        TreePath[] selectedPaths = getSelectedTreePaths();

        setAdjusting(true);

        // Updating root node children
        boolean structureChanged = performFilteringAndSortingRecursively(getRoot());
        if (structureChanged) {
          nodeStructureChanged();

          // Restoring tree state including all selections and expansions
          setAdjusting(false);

          setSelectedRows(treeTableModel.getLayout().getRowsForPaths(selectedPaths));
        }
        else {
          setAdjusting(false);
        }
        long end = System.currentTimeMillis();

        if ((end - now) < TIMER_DELAY) {
          // logic has been run within the delay time
          nextNodeStructureChanged = end + TIMER_DELAY;
        }
        else {
          // logic was slower than the interval - increase the interval adaptively
          nextNodeStructureChanged = end + (end - now) * 2;
        }
      }
      else {
        startUpdateSortAndFilterTimer();
      }
    }
  }

  public ITmmTreeTableSortingStrategy getSortStrategy() {
    Comparator<?> comparator = ((TmmTreeModel<?>) getTreeTableModel().getTreeModel()).getDataProvider().getTreeComparator();
    if (comparator instanceof ITmmTreeTableSortingStrategy sortStrategy) {
      return sortStrategy;
    }
    return null;
  }

  public void setSortStrategy(String stringEncoded) {
    ITmmTreeTableSortingStrategy sortingStrategy = getSortStrategy();

    if (sortingStrategy == null) {
      return;
    }

    sortingStrategy.fromString(stringEncoded);

    updateFiltering();
    // make sure the header also gets redrawn (this may not happen if the structure does not change)
    getTableHeader().repaint();
  }

  private class TmmTreeTableKeyAdapter extends KeyAdapter {
    final TmmTreeTable treeTable;

    TmmTreeTableKeyAdapter(TmmTreeTable treeTable) {
      this.treeTable = treeTable;
    }

    private int[] getParentRows(int[] childRows) {
      return IntStream.of(childRows).map(leafRow -> {
        @Nonnull
        TreePath leafPath = getRowPath(leafRow);
        @Nullable
        TreePath parentPath = leafPath.getParentPath();
        @Nonnull
        TreePath pathToReturn = parentPath != null ? parentPath : leafPath;
        int parentRow = treeTableModel.getLayout().getRowForPath(pathToReturn);
        return parentRow > -1 ? parentRow : leafRow;
      }).toArray();
    }

    private void toggleRows(int[] rows, boolean expand) {
      int[] collapsedAndLeafRows = IntStream.of(rows).filter(treeTable::isCollapsed).toArray();

      if (!expand && collapsedAndLeafRows.length == rows.length) {
        int[] parentRows = getParentRows(rows);
        setSelectedRows(parentRows);
        return;
      }

      // Expand/collapse rows in reverse order to ensure that row indexes don't change.
      int[] reversedBranchRows = new int[rows.length];
      for (int i = 0; i < rows.length; i++) {
        reversedBranchRows[rows.length - i - 1] = rows[i];
      }

      // Save the list of currently-selected row paths so that we can restore the user's selection after toggling.
      TreePath[] selectedPaths = getSelectedTreePaths();

      for (int row : reversedBranchRows) {
        if (expand) {
          treeTable.expandRow(row);
        }
        else {
          treeTable.collapseRow(row);
        }
      }

      setSelectedRows(treeTableModel.getLayout().getRowsForPaths(selectedPaths));
    }

    @Override
    public void keyPressed(KeyEvent e) {
      int[] selectedRows = treeTable.getSelectedRows();
      if (selectedRows.length == 0) {
        return;
      }

      try {
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
          toggleRows(selectedRows, true);
        }
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
          toggleRows(selectedRows, false);
        }
        else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          int[] branchRows = IntStream.of(selectedRows).filter(treeTable::isBranch).toArray();
          if (branchRows.length > 0) {
            boolean areAnyBranchesCollapsed = IntStream.of(branchRows).anyMatch(treeTable::isCollapsed);
            toggleRows(selectedRows, areAnyBranchesCollapsed);
          }
        }
      }
      catch (Exception ex) {
        // just not crash the UI!
      }
    }
  }
}
