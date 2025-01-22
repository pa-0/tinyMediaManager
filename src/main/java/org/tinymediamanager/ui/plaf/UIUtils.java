
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

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;

/**
 * the class {@link UIUtils} offer some UI related helper functions
 * 
 * @author Manuel Laggner
 */
public class UIUtils {

  private UIUtils() {
    throw new IllegalAccessError();
  }

  /**
   * re-calculate the given {@link Rectangle} to fit on the screen
   * 
   * @param r
   *          the {@link Rectangle} to be re-calculated
   */
  public static void fitToScreen(Rectangle r) {
    Rectangle screen = getScreenRectangle(r.x, r.y);

    int xOverdraft = r.x + r.width - screen.x - screen.width;
    if (xOverdraft > 0) {
      int shift = Math.min(xOverdraft, r.x - screen.x);
      xOverdraft -= shift;
      r.x -= shift;
      if (xOverdraft > 0) {
        r.width -= xOverdraft;
      }

    }

    int yOverdraft = r.y + r.height - screen.y - screen.height;
    if (yOverdraft > 0) {
      int shift = Math.min(yOverdraft, r.y - screen.y);
      yOverdraft -= shift;
      r.y -= shift;
      if (yOverdraft > 0) {
        r.height -= yOverdraft;
      }

    }

  }

  /**
   * Returns a visible area for a graphics device that is the closest to the specified point.
   *
   * @param x
   *          the X coordinate of the specified point
   * @param y
   *          the Y coordinate of the specified point
   * @return a visible area rectangle
   */
  public static Rectangle getScreenRectangle(int x, int y) {
    GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
    if (devices.length == 0) {
      return new Rectangle(x, y, 0, 0);
    }

    if (devices.length == 1) {
      return getScreenRectangle(devices[0]);
    }

    Rectangle[] rectangles = new Rectangle[devices.length];
    for (int i = 0; i < devices.length; i++) {
      GraphicsConfiguration configuration = devices[i].getDefaultConfiguration();
      Rectangle bounds = configuration.getBounds();
      rectangles[i] = applyInsets(bounds, getScreenInsets(configuration));
      if (bounds.contains(x, y)) {
        return rectangles[i];
      }

    }

    Rectangle bounds = rectangles[0];
    int minimum = distance(bounds, x, y);
    if (bounds.width == 0 || bounds.height == 0) {
      // Screen is invalid, give maximum score
      minimum = Integer.MAX_VALUE;
    }

    for (int i = 1; i < rectangles.length; i++) {
      if (rectangles[i].width == 0 || rectangles[i].height == 0) {
        // Screen is invalid
        continue;
      }

      int distance = distance(rectangles[i], x, y);
      if (minimum > distance) {
        minimum = distance;
        bounds = rectangles[i];
      }

    }

    if (bounds.width == 0 || bounds.height == 0) {
      // All screens were invalid, return sensible default
      return new Rectangle(x, y, 0, 0);
    }

    return bounds;
  }

  /**
   * Returns a visible area for the specified graphics device.
   *
   * @param device
   *          one of available devices
   * @return a visible area rectangle
   */
  private static Rectangle getScreenRectangle(GraphicsDevice device) {
    return getScreenRectangle(device.getDefaultConfiguration());
  }

  /**
   * Returns a visible area for the specified graphics configuration.
   *
   * @param configuration
   *          one of available configurations
   * @return a visible area rectangle
   */
  public static Rectangle getScreenRectangle(GraphicsConfiguration configuration) {
    return applyInsets(configuration.getBounds(), getScreenInsets(configuration));
  }

  private static Rectangle applyInsets(Rectangle rect, Insets i) {
    rect = new Rectangle(rect);

    if (i != null) {
      rect.width -= i.left + i.right;
      rect.height -= i.top + i.bottom;
    }

    return rect;
  }

  public static Insets getScreenInsets(final GraphicsConfiguration gc) {
    return calcInsets(gc);
  }

  private static Insets calcInsets(GraphicsConfiguration gc) {
    return Toolkit.getDefaultToolkit().getScreenInsets(gc);
  }

  /**
   * Returns a square of the distance from the specified point to the specified rectangle, which does not contain the specified point.
   *
   * @param x
   *          the X coordinate of the specified point
   * @param y
   *          the Y coordinate of the specified point
   * @return a square of the distance
   */
  private static int distance(Rectangle bounds, int x, int y) {
    x -= normalize(x, bounds.x, bounds.x + bounds.width);
    y -= normalize(y, bounds.y, bounds.y + bounds.height);
    return x * x + y * y;
  }

  /**
   * Normalizes a specified value in the specified range. If value less than the minimal value, the method returns the minimal value. If value greater
   * than the maximal value, the method returns the maximal value.
   *
   * @param value
   *          the value to normalize
   * @param min
   *          the minimal value of the range
   * @param max
   *          the maximal value of the range
   * @return a normalized value
   */
  private static int normalize(int value, int min, int max) {
    return value < min ? min : Math.min(value, max);
  }
}
