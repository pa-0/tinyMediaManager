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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import com.formdev.flatlaf.ui.FlatUIUtils;

/**
 * The Class TmmTabbedPaneUI.
 *
 * @author Manuel Laggner
 */
public class TmmTabbedPaneUI extends FlatTabbedPaneUI {
  protected static final int BORDER_RADIUS = 15;
  protected Color            contentBackgroundColor;

  public static ComponentUI createUI(JComponent c) {
    return new TmmTabbedPaneUI();
  }

  public TmmTabbedPaneUI() {
    super();
  }

  @Override
  protected void installDefaults() {
    // just take the original font and scale it
    Font defaultFont = UIManager.getFont("defaultFont");
    if (defaultFont != null) {
      tabPane.setFont(null); // need to re-set the font for online font changing
      UIManager.put("TabbedPane.font", scale(defaultFont, 1.1667).deriveFont(Font.BOLD));
    }

    super.installDefaults();

    contentBackgroundColor = UIManager.getColor("TabbedPane.contentBackgroundColor");
  }

  @Override
  protected void paintTabSelection(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h) {
    // not needed
  }

  @Override
  protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
    Graphics2D g2D = (Graphics2D) g.create();
    try {
      FlatUIUtils.setRenderingHints(g);

      Insets insets = tabPane.getInsets();

      int x = insets.left;
      int y = insets.top;
      int w = tabPane.getWidth() - insets.right - insets.left;
      int h = tabPane.getHeight() - insets.top - insets.bottom;

      g2D.setColor(contentAreaColor);
      g2D.fillRect(x, y, w, h);

      if (contentBackgroundColor != null) {
        g2D.setColor(contentBackgroundColor);

        if (drawFullWidth()) {
          x = 0;
          y = 0;
        }
        else {
          Insets contentInsets = getContentBorderInsets(tabPlacement);

          x = insets.left + contentInsets.left;
          y = 0;
          w = w - contentInsets.left - contentInsets.right;
        }

        if (drawRoundEdge()) {
          g2D.fillRoundRect(x, y, w, h, BORDER_RADIUS, BORDER_RADIUS);
        }
        else {
          g2D.fillRect(x, y, w, h);
        }
      }

      // repaint selection because the arrow is painted in the content area
      if (selectedIndex >= 0) {
        Rectangle tabRect = getTabBounds(tabPane, selectedIndex);
        Insets tabInsets = getTabInsets(tabPlacement, selectedIndex);
        if (tabRect.width > (tabInsets.left + tabInsets.right)) {
          g2D.setColor(selectedBackground);
          int[] xPoints = { tabRect.x + (tabRect.width / 2 + 10), tabRect.x + (tabRect.width / 2 - 10), tabRect.x + (tabRect.width / 2) };
          int[] yPoints = { tabRect.y + tabRect.height, tabRect.y + tabRect.height, tabRect.y + tabRect.height + 10 };
          g2D.fillPolygon(xPoints, yPoints, xPoints.length);
        }
      }

    }
    finally {
      g2D.dispose();
    }
  }

  protected Font scale(Font font, double factor) {
    int newSize = Math.round((float) (font.getSize() * factor));
    return font.deriveFont((float) newSize);
  }

  @Override
  protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect,
      boolean isSelected) {
    // the focus indicator is drawn with the tab background
  }

  @Override
  protected Insets getContentBorderInsets(int tabPlacement) {
    Insets insets = new Insets(contentBorderInsets.top, contentBorderInsets.left, contentBorderInsets.bottom, contentBorderInsets.right);

    if (Boolean.FALSE.equals(this.tabPane.getClientProperty("rightBorder"))) {
      insets.right = 0;
    }

    if ("half".equals(this.tabPane.getClientProperty("rightBorder"))) {
      insets.right = contentBorderInsets.right / 2;
    }

    if (Boolean.FALSE.equals(this.tabPane.getClientProperty("leftBorder"))) {
      insets.left = 0;
    }

    if ("half".equals(this.tabPane.getClientProperty("leftBorder"))) {
      insets.left = contentBorderInsets.left / 2;
    }

    if (drawRoundEdge()) {
      insets.bottom = 10;
    }
    else {
      insets.bottom = 0;
    }

    return insets;
  }

  private boolean drawRoundEdge() {
    Object clientProperty = tabPane.getClientProperty("roundEdge");
    if (clientProperty == null) {
      return true;
    }
    return Boolean.parseBoolean(clientProperty.toString());
  }

  private boolean drawFullWidth() {
    Object clientProperty = tabPane.getClientProperty("fullWidth");
    if (clientProperty == null) {
      return false;
    }
    return Boolean.parseBoolean(clientProperty.toString());
  }

  @Override
  protected Insets getTabAreaInsets(int tabPlacement) {
    Insets insets = new Insets(tabAreaInsets.top, tabAreaInsets.left, tabAreaInsets.bottom, tabAreaInsets.right);

    // overrides
    if (Boolean.FALSE.equals(this.tabPane.getClientProperty("rightBorder"))) {
      insets.right = 0;
    }

    if (Boolean.FALSE.equals(this.tabPane.getClientProperty("leftBorder"))) {
      insets.left = 0;
    }

    return insets;
  }

  @Override
  protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
    // redraw the black border
    Rectangle clipRect = g.getClipBounds();
    if (clipRect.y < maxTabHeight) {
      g.setColor(tabPane.getBackground());
      g.fillRect(0, -1, tabPane.getWidth(), maxTabHeight + 1);
    }

    super.paintTabArea(g, tabPlacement, selectedIndex);
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    super.paint(g, c);

    // redraw the black border
    Rectangle clipRect = g.getClipBounds();
    if (clipRect.y < maxTabHeight) {
      g.setColor(tabPane.getBackground());
      g.fillRect(0, -1, tabPane.getWidth(), maxTabHeight + 1);
    }
  }
}
