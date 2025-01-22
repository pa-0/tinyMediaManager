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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

import com.formdev.flatlaf.ui.FlatPanelUI;
import com.formdev.flatlaf.ui.FlatUIUtils;

/**
 * Provides the Flat LaF UI delegate for {@link javax.swing.JPanel}.
 *
 * <!-- BasicPanelUI -->
 *
 * @uiDefault Panel.font Font unused
 * @uiDefault Panel.background Color only used if opaque
 * @uiDefault Panel.foreground Color
 * @uiDefault Panel.border Border
 *
 * @author Manuel Laggner
 */
public class TmmPanelUI extends FlatPanelUI {
  protected static String CLASS         = "class";
  protected static String ROUNDED_PANEL = "roundedPanel";
  protected static String BORDER_RADIUS = "borderRadius";

  public static ComponentUI createUI(JComponent c) {
    return (FlatUIUtils.canUseSharedUI(c) ? FlatUIUtils.createSharedUI(TmmPanelUI.class, () -> new TmmPanelUI(true)) : new TmmPanelUI(false));
  }

  protected TmmPanelUI(boolean shared) {
    super(shared);
  }

  @Override
  public void update(Graphics g, JComponent c) {
    if (c.isOpaque()) {
      Object panelClass = c.getClientProperty(CLASS);
      if (panelClass != null && panelClass instanceof String && ROUNDED_PANEL.equals(panelClass.toString())) {
        // draw a rounded panel
        updateRoundedPanel(g, c);
      }
      else {
        // default drawing
        super.update(g, c);
      }
    }
  }

  private void updateRoundedPanel(Graphics g, JComponent c) {
    int radius = 15;
    Object borderRadius = c.getClientProperty(BORDER_RADIUS);
    if (borderRadius != null && borderRadius instanceof Integer) {
      radius = (Integer) borderRadius;
    }

    Graphics2D g2D = (Graphics2D) g;
    RenderingHints savedRenderingHints = g2D.getRenderingHints();
    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g.setColor(c.getBackground());
    g.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), radius, radius);

    g2D.setRenderingHints(savedRenderingHints);
  }
}
