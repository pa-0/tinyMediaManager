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

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.formdev.flatlaf.ui.FlatTableHeaderUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;

/**
 * the class TmmTableHeaderUI is used to render the table header including icons
 *
 * @author Manuel Laggner
 */
public class TmmTableHeaderUI extends FlatTableHeaderUI {

  public static ComponentUI createUI(JComponent c) {
    return new TmmTableHeaderUI();
  }

  @Override
  protected void uninstallDefaults() {
    super.uninstallDefaults();

    bottomSeparatorColor = null;
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    super.paint(g, c);

    TableCellRenderer defaultRenderer = this.header.getDefaultRenderer();
    boolean paintBorders = this.isDefaultRenderer(defaultRenderer);
    if (!paintBorders && this.header.getColumnModel().getColumnCount() > 0) {
      Component rendererComponent = defaultRenderer.getTableCellRendererComponent(this.header.getTable(), "", false, false, -1, 0);
      paintBorders = this.isDefaultRenderer(rendererComponent);
    }

    // re-paint the column borders
    if (paintBorders) {
      this.paintMyColumnBorders(g, c);
    }

    if (paintBorders) {
      this.paintMyDraggedColumnBorders(g, c);
    }
  }

  private boolean isDefaultRenderer(Object headerRenderer) {
    String rendererClassName = headerRenderer.getClass().getName();
    return rendererClassName.equals("sun.swing.table.DefaultTableCellHeaderRenderer")
        || rendererClassName.equals("sun.swing.FilePane$AlignableTableHeaderRenderer");
  }

  private void paintMyColumnBorders(Graphics g, JComponent c) {
    int width = c.getWidth();
    int height = c.getHeight();
    float lineWidth = UIScale.scale(1.0F);
    float topLineIndent = lineWidth;
    float bottomLineIndent = 0;
    TableColumnModel columnModel = this.header.getColumnModel();
    int columnCount = columnModel.getColumnCount();
    Graphics2D g2 = (Graphics2D) g.create();

    try {
      FlatUIUtils.setRenderingHints(g2);

      // horizontal
      g2.setColor(this.bottomSeparatorColor);
      g2.fill(new Rectangle2D.Float(0.0F, (float) height - lineWidth, (float) width, lineWidth));

      // vertical
      int sepCount = columnCount - 1; // do not draw the rightmost vertical border

      ArrayList<Integer> colsWoRightGrid = new ArrayList<>();
      if (this.header.getTable().getClientProperty("borderNotToDraw") != null) {
        colsWoRightGrid = (ArrayList<Integer>) this.header.getTable().getClientProperty("borderNotToDraw");
      }

      int x;
      int i;
      if (this.header.getComponentOrientation().isLeftToRight()) {
        x = 0;

        for (i = 0; i < sepCount; ++i) {
          x += columnModel.getColumn(i).getWidth();

          if (colsWoRightGrid.contains(i)) {
            continue;
          }

          g2.drawLine(x - 1, (int) topLineIndent, x - 1, (int) (height - bottomLineIndent));
        }
      }
      else {
        x = width;

        for (i = 0; i < sepCount; ++i) {
          x -= columnModel.getColumn(i).getWidth();
          g2.fill(new Rectangle2D.Float(x - (i < sepCount - 1 ? lineWidth : 0.0F), topLineIndent, lineWidth, height - bottomLineIndent));
        }
      }
    }
    finally {
      g2.dispose();
    }

  }

  private void paintMyDraggedColumnBorders(Graphics g, JComponent c) {
    TableColumn draggedColumn = this.header.getDraggedColumn();
    if (draggedColumn != null) {
      TableColumnModel columnModel = this.header.getColumnModel();
      int columnCount = columnModel.getColumnCount();
      int draggedColumnIndex = -1;

      for (int i = 0; i < columnCount; ++i) {
        if (columnModel.getColumn(i) == draggedColumn) {
          draggedColumnIndex = i;
          break;
        }
      }

      if (draggedColumnIndex >= 0) {
        float lineWidth = UIScale.scale(1.0F);
        float topLineIndent = lineWidth;
        float bottomLineIndent = lineWidth * 3.0F;
        Rectangle r = this.header.getHeaderRect(draggedColumnIndex);
        r.x += this.header.getDraggedDistance();
        Graphics2D g2 = (Graphics2D) g.create();

        try {
          FlatUIUtils.setRenderingHints(g2);
          g2.setColor(this.bottomSeparatorColor);
          g2.fill(new Rectangle2D.Float((float) r.x, (float) (r.y + r.height) - lineWidth, (float) r.width, lineWidth));
          g2.fill(new Rectangle2D.Float((float) r.x, topLineIndent, lineWidth, (float) r.height - bottomLineIndent));
          g2.fill(new Rectangle2D.Float((float) (r.x + r.width) - lineWidth, (float) r.y + topLineIndent, lineWidth,
              (float) r.height - bottomLineIndent));
        }
        finally {
          g2.dispose();
        }

      }
    }
  }

  private JScrollPane getMyScrollPane() {
    Container parent = this.header.getParent();
    if (parent == null) {
      return null;
    }
    else {
      parent = parent.getParent();
      return parent instanceof JScrollPane ? (JScrollPane) parent : null;
    }
  }
}
