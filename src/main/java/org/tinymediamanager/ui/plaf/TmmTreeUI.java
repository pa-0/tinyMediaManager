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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import com.formdev.flatlaf.ui.FlatTreeUI;
import com.formdev.flatlaf.ui.FlatUIUtils;

/**
 * The class TmmTreeUI. Render the JTree nicely
 *
 * @author Manuel Laggner
 */
public class TmmTreeUI extends FlatTreeUI {

  private Color  gridColor;
  private Color  gridColor2;
  private Insets margins;

  public static ComponentUI createUI(JComponent c) {
    return new TmmTreeUI();
  }

  @Override
  protected void installDefaults() {
    super.installDefaults();
    gridColor = UIManager.getColor("Table.gridColor");
    gridColor2 = UIManager.getColor("Table.gridColor2");
    margins = UIManager.getInsets("Tree.rendererMargins");
  }

  protected TreeCellRenderer createDefaultCellRenderer() {
    // inject the border
    return new TmmTreeCellRenderer();
  }

  private class TmmTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public void updateUI() {
      super.updateUI();
      setBorder(new BottomBorderBorder());
    }
  }

  /*
   * expand the tree background to the whole tree width
   */
  @Override
  protected void paintRow(Graphics g, Rectangle clipBounds, Insets insets, Rectangle bounds, TreePath path, int row, boolean isExpanded,
      boolean hasBeenExpanded, boolean isLeaf) {
    if (editingComponent != null && editingRow == row) {
      return;
    }

    bounds.width = tree.getWidth() - bounds.x;
    super.paintRow(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
  }

  private class BottomBorderBorder extends AbstractBorder implements UIResource {
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      Graphics2D g2d = (Graphics2D) g.create();
      try {
        FlatUIUtils.setRenderingHints(g2d);

        g.setColor(gridColor);
        g.drawLine(g.getClipBounds().x, height - 2, g.getClipBounds().width, height - 2);

        g.setColor(gridColor2);
        g.drawLine(g.getClipBounds().x, height - 1, g.getClipBounds().width, height - 1);
      }
      finally {
        g2d.dispose();
      }
    }

    @Override
    public Insets getBorderInsets(Component c) {
      if (margins != null) {
        return margins;
      }
      else {
        return super.getBorderInsets(c);
      }
    }
  } // class BottomBorderBorder
}
