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

import static javax.swing.SwingConstants.HORIZONTAL;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.plaf.ComponentUI;

import com.formdev.flatlaf.ui.FlatProgressBarUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;

/**
 * Provides the Flat LaF UI delegate for {@link JProgressBar}.
 *
 * <!-- BasicProgressBarUI -->
 *
 * @uiDefault ProgressBar.font Font
 * @uiDefault ProgressBar.background Color
 * @uiDefault ProgressBar.foreground Color
 * @uiDefault ProgressBar.selectionBackground Color
 * @uiDefault ProgressBar.selectionForeground Color
 * @uiDefault ProgressBar.border Border
 * @uiDefault ProgressBar.horizontalSize Dimension default is 146,12
 * @uiDefault ProgressBar.verticalSize Dimension default is 12,146
 * @uiDefault ProgressBar.repaintInterval int default is 50 milliseconds
 * @uiDefault ProgressBar.cycleTime int default is 3000 milliseconds
 *
 *            <!-- FlatProgressBarUI -->
 *
 * @uiDefault ProgressBar.arc int
 *
 * @author Manuel Laggner
 */
public class TmmProgressBarUI extends FlatProgressBarUI {

  public static ComponentUI createUI(JComponent c) {
    return new TmmProgressBarUI();
  }

  /**
   * Delegates painting to one of two methods: paintDeterminate or paintIndeterminate.
   */
  public void paint(Graphics g, JComponent c) {
    if (progressBar.isIndeterminate()) {
      paintIndeterminate(g, c);
    }
    else {
      paintDeterminate(g, c);
    }
  }

  @Override
  protected void paintDeterminate(Graphics g, JComponent c) {
    if (!(g instanceof Graphics2D)) {
      return;
    }

    Graphics2D g2D = (Graphics2D) g.create();
    try {
      FlatUIUtils.setRenderingHints(g2D);

      Insets b = progressBar.getInsets(); // area for border
      int w = progressBar.getWidth() - (b.right + b.left);
      int h = progressBar.getHeight() - (b.top + b.bottom);

      // amount of progress to draw
      int amountFull = getAmountFull(b, w, h);

      if (progressBar.getOrientation() == HORIZONTAL) {
        // calculate the origin for the progress bar
        int y = b.top + (h - horizontalSize.height) / 2;

        // draw background
        g2D.setColor(progressBar.getBackground());
        g2D.fillRoundRect(b.left, y, w, horizontalSize.height, arc, arc);

        g2D.setColor(progressBar.getForeground());
        g2D.fillRoundRect(b.left, y, amountFull, horizontalSize.height, arc, arc);

      }
      else { // VERTICAL
        // calculate the origin for the progress bar
        int x = b.left + (w - verticalSize.width) / 2;

        // draw background
        g2D.setColor(progressBar.getBackground());
        g2D.fillRoundRect(x, b.top, verticalSize.width, h, arc, arc);

        g2D.setColor(progressBar.getForeground());
        g2D.fillRoundRect(x, b.top, w, h - amountFull, arc, arc);
      }
    }
    finally {
      g2D.dispose();
    }
  }

  @Override
  protected void paintIndeterminate(Graphics g, JComponent c) {
    if (!(g instanceof Graphics2D)) {
      return;
    }

    Graphics2D g2D = (Graphics2D) g.create();
    try {
      FlatUIUtils.setRenderingHints(g2D);

      Insets b = progressBar.getInsets(); // area for border
      int w = progressBar.getWidth() - (b.right + b.left);
      int h = progressBar.getHeight() - (b.top + b.bottom);

      if (progressBar.getOrientation() == HORIZONTAL) {
        // calculate the origin for the progress bar
        int y = b.top + (h - horizontalSize.height) / 2;

        // draw background
        g2D.setColor(progressBar.getForeground());
        Area background = new Area(new RoundRectangle2D.Float(b.left, y, w, horizontalSize.height, arc, arc));
        g2D.fill(background);

        // Paint the striped box.
        boxRect = getBox(boxRect);
        if (boxRect != null) {
          w = UIScale.scale(20);
          int x = getAnimationIndex();
          GeneralPath p = new GeneralPath();

          p.moveTo(boxRect.x, boxRect.y + (float) boxRect.height);
          p.lineTo(boxRect.x + w * .5f, boxRect.y + (float) boxRect.height);
          p.lineTo(boxRect.x + (float) w, boxRect.y);
          p.lineTo(boxRect.x + w * .5f, boxRect.y);

          p.closePath();
          g2D.setColor(progressBar.getBackground());

          for (int i = boxRect.width + x; i > -w; i -= w) {
            Area bar = new Area(AffineTransform.getTranslateInstance(i, 0).createTransformedShape(p));
            bar.intersect(background);
            g2D.fill(bar);
          }
        }
      }
      else { // VERTICAL
        // calculate the origin for the progress bar
        int x = b.left + (w - verticalSize.width) / 2;

        // not implemented

        // draw background
        g2D.setColor(progressBar.getForeground());
        g2D.fillRoundRect(x, b.top, verticalSize.width, h, arc, arc);
      }
    }
    finally {
      g2D.dispose();
    }
  }

  @Override
  protected int getBoxLength(int availableLength, int otherDimension) {
    return availableLength;
  }
}
