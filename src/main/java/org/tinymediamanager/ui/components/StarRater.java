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
package org.tinymediamanager.ui.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Shape;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.tinymediamanager.ui.IconManager;

/**
 * The star rater panel.
 * 
 * @author noblemaster
 * @since August 30, 2010
 */
public class StarRater extends JPanel {
  private static final ImageIcon STAR_BACKGROUND_IMAGE = IconManager.STAR_EMPTY;
  private static final ImageIcon STAR_FOREGROUND_IMAGE = IconManager.STAR_FILLED;

  private final int              stars;
  private final int              factor;
  private float                  rating;
  private float                  paintRating;

  /**
   * The constructor.
   */
  public StarRater() {
    this(5, 1);
  }

  /**
   * The constructor.
   * 
   * @param stars
   *          The number of stars n.
   * @param factor
   *          the factor
   */
  public StarRater(int stars, int factor) {
    this(stars, factor, 0f);
  }

  /**
   * The constructor.
   * 
   * @param stars
   *          The number of stars n.
   * @param factor
   *          the factor
   * @param rating
   *          The rating [0, n]. 0 = no rating.
   */
  public StarRater(int stars, int factor, float rating) {
    this.stars = stars;
    this.rating = rating;
    if (factor > 0) {
      this.factor = factor;
    }
    else {
      this.factor = 1;
    }
    this.paintRating = this.rating / this.factor;

    // set look
    setOpaque(false);
    setLayout(null);
  }

  /**
   * Returns the rating.
   * 
   * @return The rating [0, n]. 0 = no rating.
   */
  public float getRating() {
    return rating;
  }

  /**
   * Sets the rating.
   * 
   * @param rating
   *          The rating [0, n]. 0 = no rating.
   */
  public void setRating(float rating) {
    this.rating = rating;
    this.paintRating = this.rating / this.factor;
    repaint();
  }

  /**
   * Returns the preferred size.
   * 
   * @return The preferred size.
   */
  @Override
  public Dimension getPreferredSize() {
    return new Dimension(stars * STAR_BACKGROUND_IMAGE.getIconWidth(), STAR_BACKGROUND_IMAGE.getIconHeight());
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  /**
   * Paints this component.
   * 
   * @param g
   *          Where to paint to.
   */
  @Override
  protected void paintComponent(Graphics g) {
    if (rating >= 0) {

      // draw stars
      int w = STAR_BACKGROUND_IMAGE.getIconWidth();
      int h = STAR_BACKGROUND_IMAGE.getIconHeight();
      int x = 0;
      for (int i = 0; i < stars; i++) {
        STAR_BACKGROUND_IMAGE.paintIcon(this, g, x, 0);

        if (paintRating > i) {
          int dw = (paintRating >= (i + 1)) ? w : Math.round((paintRating - i) * w);
          Shape oldCLip = g.getClip();
          g.setClip(x, 0, dw, h);
          STAR_FOREGROUND_IMAGE.paintIcon(this, g, x, 0);
          g.setClip(oldCLip);
        }

        x += w;
      }
    }
  }
}
