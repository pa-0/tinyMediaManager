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

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

import org.tinymediamanager.ui.components.table.TmmTable;

import com.formdev.flatlaf.ui.FlatScrollBarUI;

/**
 * The Class TmmScrollBarUI.
 *
 * @author Manuel Laggner
 */
public class TmmScrollBarUI extends FlatScrollBarUI {

  private int   width;
  private int   thumbWidth;
  private int   gap;

  private Color borderColor;

  public static ComponentUI createUI(JComponent c) {
    return new TmmScrollBarUI();
  }

  public TmmScrollBarUI() {
    super();
  }

  @Override
  protected void installDefaults() {
    super.installDefaults();

    width = UIManager.getInt("ScrollBar.width");
    thumbWidth = UIManager.getInt("ScrollBar.thumbWidth");
    if (thumbWidth == 0) {
      thumbWidth = width / 2;
    }
    gap = UIManager.getInt("ScrollBar.gap");
    borderColor = UIManager.getColor("Component.borderColor");
  }

  @Override
  protected void layoutVScrollbar(JScrollBar sb) {
    super.layoutVScrollbar(sb);

    // special case: no scrolling is needed; the logic above will paint the thumb at bottom
    // paint it on top
    if (sb.getValue() == 0) {
      thumbRect.setBounds(thumbRect.x, -1 + gap, thumbRect.width, thumbRect.height - 2 * gap);
    }
    else {
      thumbRect.setBounds(thumbRect.x, thumbRect.y + gap, thumbRect.width, thumbRect.height - 2 * gap);
    }
    trackRect.setBounds(trackRect.x, trackRect.y + gap, trackRect.width, trackRect.height - 2 * gap);
  }

  @Override
  protected void layoutHScrollbar(JScrollBar sb) {
    super.layoutHScrollbar(sb);

    trackRect.setBounds(trackRect.x + gap, trackRect.y, trackRect.width - 2 * gap, trackRect.height);
    thumbRect.setBounds(thumbRect.x + gap, thumbRect.y, thumbRect.width - 2 * gap, thumbRect.height);
  }

  @Override
  protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
    // do not paint the track if there is no thumb
    if (getThumbBounds().isEmpty()) {
      return;
    }

    g.setColor(trackColor);

    // track
    if (scrollbar.isEnabled()) {
      if (scrollbar.getOrientation() == Adjustable.VERTICAL) {
        int x = (width - thumbWidth) / 2;
        g.fillRoundRect(trackBounds.x + x, trackBounds.y, thumbWidth, trackBounds.height, thumbWidth, thumbWidth);
      }
      else {
        int y = (width - thumbWidth) / 2;
        g.fillRoundRect(trackBounds.x, trackBounds.y + y, trackBounds.width, thumbWidth, thumbWidth, thumbWidth);
      }
    }

    if (c.getParent() instanceof JScrollPane
        && (((JScrollPane) c.getParent()).getBorder() != null || ((JScrollPane) c.getParent()).getViewport().getView() instanceof TmmTable)) {
      g.setColor(borderColor);
      if (scrollbar.getOrientation() == Adjustable.VERTICAL) {
        g.drawLine(0, 0, 0, c.getHeight());
      }
      else {
        g.drawLine(0, 0, c.getWidth(), 0);
      }
    }
  }

  @Override
  protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
    if (!c.isEnabled()) {
      return;
    }

    if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
      return;
    }

    Graphics2D g2d = (Graphics2D) g;
    g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

    g.setColor(thumbColor);
    g.translate(thumbBounds.x, thumbBounds.y);

    if (scrollbar.getOrientation() == Adjustable.VERTICAL) {
      int x = (width - thumbWidth) / 2;
      g.fillRoundRect(x + 1, 2, thumbWidth - 2, thumbBounds.height - 4, thumbWidth - 2, thumbWidth - 2);
    }
    else {
      int y = (width - thumbWidth) / 2;
      g.fillRoundRect(2, y + 1, thumbBounds.width - 4, thumbWidth - 2, thumbWidth - 2, thumbWidth - 2);
    }
  }

  @Override
  protected Dimension getMinimumThumbSize() {

    if (scrollbar.getOrientation() == Adjustable.VERTICAL) {
      return new Dimension(thumbWidth - 2, width * 2);
    }
    else {
      return new Dimension(width * 2, thumbWidth - 2);
    }
  }

  @Override
  protected Dimension getMaximumThumbSize() {
    if (scrollbar.getOrientation() == Adjustable.VERTICAL) {
      return new Dimension(thumbWidth - 2, width * 3);
    }
    else {
      return new Dimension(width * 3, thumbWidth - 2);
    }
  }
}
