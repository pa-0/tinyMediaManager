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
package org.tinymediamanager.ui.plaf;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ViewportUI;
import javax.swing.table.TableColumn;

import com.formdev.flatlaf.ui.FlatViewportUI;

public class TmmViewportUI extends FlatViewportUI {
  // Shared UI object
  private static ViewportUI viewportUI;

  private boolean           paintTmmGrid;
  private Color             gridColor;
  private Color             gridColor2;

  public static ComponentUI createUI(JComponent c) {
    if (viewportUI == null) {
      viewportUI = new TmmViewportUI();
    }
    return viewportUI;
  }

  @Override
  public void installUI(JComponent c) {
    super.installUI(c);

    paintTmmGrid = UIManager.getBoolean("Table.paintTmmGrid");
    if (paintTmmGrid) {
      gridColor = UIManager.getColor("Table.gridColor");
      gridColor2 = UIManager.getColor("Table.gridColor2");
    }
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    super.paint(g, c);

    if (paintTmmGrid) {
      JViewport viewport = (JViewport) c;
      JTable table = null;

      // paint vertical grid lines for all tables except the TmmTable
      if (viewport.getView() instanceof JTable) {
        table = (JTable) ((JViewport) c).getView();
      }

      if (table != null) {
        paintVerticalGridLines(g, c, table);
        paintHorizontalGridLines(g, c, table);
      }
    }
  }

  private void paintVerticalGridLines(Graphics g, JComponent c, JTable fTable) {
    JViewport viewport = (JViewport) c;
    int offset = viewport.getViewPosition().x;
    int x = -offset;

    for (int i = 0; i < fTable.getColumnCount() - 1; i++) {
      TableColumn column = fTable.getColumnModel().getColumn(i);
      // increase the x position by the width of the current column.
      x += column.getWidth();

      if (x >= 0) {
        g.setColor(gridColor);
        // draw the grid line (not sure what the -1 is for, but BasicTableUI also does it.
        g.drawLine(x - 1, 0, x - 1, viewport.getHeight());
      }
    }
  }

  private void paintHorizontalGridLines(Graphics g, JComponent c, JTable fTable) {
    JViewport viewport = (JViewport) c;

    // get position
    Point viewPosition = viewport.getViewPosition();
    g.translate(0, -viewPosition.y);

    // get the row index at the top of the clip bounds (the first row to paint).
    int rowAtPoint = fTable.rowAtPoint(g.getClipBounds().getLocation());
    // get the y coordinate of the first row to paint. if there are no
    // rows in the table, start painting at the top of the supplied clipping bounds.
    int topY = rowAtPoint < 0 ? g.getClipBounds().y : fTable.getCellRect(rowAtPoint, 0, true).y;

    // create a counter variable to hold the current row. if there are no
    // rows in the table, start the counter at 0.
    int currentRow = Math.max(rowAtPoint, 0);
    while (topY < g.getClipBounds().y + g.getClipBounds().height) {
      int bottomY = topY + fTable.getRowHeight(currentRow);
      g.setColor(gridColor);
      g.drawLine(5, bottomY - 1, viewport.getWidth(), bottomY - 1);
      g.setColor(gridColor2);
      g.drawLine(5, bottomY, viewport.getWidth(), bottomY);
      topY = bottomY;
      currentRow++;
    }

    g.translate(0, viewPosition.y);
  }
}
