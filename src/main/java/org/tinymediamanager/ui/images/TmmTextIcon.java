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
package org.tinymediamanager.ui.images;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

public class TmmTextIcon extends ImageIcon {

  private final String text;
  private float        fontSize;
  private Color        color;

  public TmmTextIcon(String text, float fontSize, Color color) {
    this.text = text;
    this.fontSize = fontSize;
    this.color = color;

    update();
  }

  public void setFontSize(float size) {
    this.fontSize = size;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  /**
   * update the image with the default settings
   */
  public void update() {
    setImage(createImage());
  }

  private Image createImage() {
    try {
      Font font = UIManager.getFont("Label.font").deriveFont(Font.BOLD, fontSize);

      // calculate icon size
      BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2 = GraphicsEnvironment.getLocalGraphicsEnvironment().createGraphics(tmp);
      g2.setFont(font);

      // get the visual bounds of the string (this is more realiable than the string bounds)
      Rectangle2D defaultBounds = g2.getFontMetrics().getStringBounds("M", g2);
      Rectangle2D bounds = font.createGlyphVector(g2.getFontRenderContext(), text).getVisualBounds();
      int iconWidth = (int) Math.ceil(bounds.getWidth()) + 2; // +2 to avoid clipping problems
      int iconHeight = (int) Math.ceil(bounds.getHeight()) + 2; // +2 to avoid clipping problems

      if (iconHeight < defaultBounds.getHeight()) {
        iconHeight = (int) Math.ceil(defaultBounds.getHeight());
      }

      g2.dispose();

      // if width is less than height, increase the width to be at least a square
      if (iconWidth < iconHeight) {
        iconWidth = iconHeight;
      }

      // and draw it
      BufferedImage buffer = new BufferedImage(iconWidth, iconHeight, BufferedImage.TYPE_INT_ARGB);
      g2 = (Graphics2D) buffer.getGraphics();
      // g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      // g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      Map<?, ?> desktopHints = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
      if (desktopHints != null) {
        g2.setRenderingHints(desktopHints);
      }

      g2.setFont(font);
      g2.setColor(color);

      // draw the glyhps centered
      int y = (int) Math.floor(bounds.getY() - (defaultBounds.getHeight() - bounds.getHeight()) / 2);
      g2.drawString(text, (int) ((iconWidth - Math.ceil(bounds.getWidth())) / 2), -y);
      g2.dispose();
      return buffer;
    }
    catch (Exception ignored) {
    }
    return null;
  }
}
