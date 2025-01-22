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

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 * The Class {@link TmmEditorTable} can be used to display the date in a table format, but offering a
 *
 * @author Manuel Laggner
 */
public abstract class TmmEditorTable extends TmmTable {

  public TmmEditorTable() {
    super();

    TableButtonListener listener = new TableButtonListener(this);
    addMouseListener(listener);
    addMouseMotionListener(listener);
  }

  public TmmEditorTable(TableModel tableModel) {
    this();
    setModel(tableModel);
  }

  /**
   * check whether this column is the edit column
   *
   * @param column
   *          the column index
   * @return true/false
   */
  protected boolean isEditorColumn(int column) {
    if (column < 0) {
      return false;
    }
    return "edit".equals(getColumnModel().getColumn(column).getIdentifier());
  }

  /**
   * react on the button click of the edit button
   *
   * @param row
   *          the row index where the edit button has been clicked
   */
  protected abstract void editButtonClicked(int row);

  /**
   * provides an extra check for further linked cells
   *
   * @param row
   *          the row index
   * @param column
   *          the column index
   * @return true if the cell contains any link
   */
  protected boolean isLinkCell(int row, int column) {
    return isEditorColumn(column);
  }

  /**
   * callback if another link has been clicked
   *
   * @param row
   *          the row index
   * @param column
   *          the column index
   */
  protected void linkClicked(int row, int column) {
    // empty default impl.
  }

  /**
   * helper class for listening to the edit button
   */
  private class TableButtonListener extends MouseAdapter {
    private final JTable table;

    private TableButtonListener(JTable table) {
      this.table = table;
    }

    @Override
    public void mousePressed(MouseEvent arg0) {
      click(arg0);
    }

    private void click(MouseEvent arg0) {
      int row = table.rowAtPoint(arg0.getPoint());
      int col = table.columnAtPoint(arg0.getPoint());

      if (isEditorColumn(col)) {
        row = table.convertRowIndexToModel(row);
        editButtonClicked(row);
      }
      else if (isLinkCell(row, col)) {
        row = table.convertRowIndexToModel(row);
        linkClicked(row, col);
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      Point point = new Point(e.getX(), e.getY());
      int row = table.rowAtPoint(point);
      int col = table.columnAtPoint(point);

      if (row == -1 || col == -1) {
        table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        return;
      }

      if (isLinkCell(row, col)) {
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      Point point = new Point(e.getX(), e.getY());
      int row = table.rowAtPoint(point);
      int col = table.columnAtPoint(point);

      if (row == -1 || col == -1) {
        table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        return;
      }

      if (!isLinkCell(row, col)) {
        table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      Point point = new Point(e.getX(), e.getY());
      int row = table.rowAtPoint(point);
      int col = table.columnAtPoint(point);

      if (row == -1 || col == -1) {
        table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        return;
      }

      if (!isLinkCell(row, col) && table.getCursor().getType() == Cursor.HAND_CURSOR) {
        table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
      if (isLinkCell(row, col) && table.getCursor().getType() == Cursor.DEFAULT_CURSOR) {
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));
      }
    }
  }
}