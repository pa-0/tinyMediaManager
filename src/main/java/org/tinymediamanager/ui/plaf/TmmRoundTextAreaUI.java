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
import java.awt.Graphics2D;
import java.awt.event.FocusListener;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;

import com.formdev.flatlaf.ui.FlatTextAreaUI;
import com.formdev.flatlaf.ui.FlatTextBorder;
import com.formdev.flatlaf.ui.FlatUIUtils;

/**
 * The class {@link TmmRoundTextAreaUI} is used to create a JTextArea (TmmRoundTextArea) with rounded borders
 *
 * @author Manuel Laggner
 */
public class TmmRoundTextAreaUI extends FlatTextAreaUI {

  private FocusListener    focusListener;
  private DocumentListener documentListener;

  public static ComponentUI createUI(JComponent c) {
    return new TmmRoundTextAreaUI();
  }

  @Override
  protected void installDefaults() {
    super.installDefaults();

    LookAndFeel.installProperty(getComponent(), "opaque", false);
    getComponent().setBorder(new FlatTextBorder());
  }

  @Override
  protected void installListeners() {
    super.installListeners();

    // necessary to update focus border and background
    focusListener = new FlatUIUtils.RepaintFocusListener(getComponent(), null);
    getComponent().addFocusListener(focusListener);
  }

  @Override
  protected void uninstallListeners() {
    super.uninstallListeners();

    getComponent().removeFocusListener(focusListener);
    focusListener = null;

    if (documentListener != null) {
      getComponent().getDocument().removeDocumentListener(documentListener);
      documentListener = null;
    }
  }

  @Override
  protected void paintSafely(Graphics g) {
    paintBackground(g, getComponent(), focusedBackground);
    super.paintSafely(g);
  }

  @Override
  protected void paintBackground(Graphics g) {
    // painted somehere else
  }

  static void paintBackground(Graphics g, JTextComponent c, Color focusedBackground) {
    // do not paint background if:
    // - not opaque and
    // - border is not a flat border and
    // - opaque was explicitly set (to false)
    // (same behavior as in AquaTextFieldUI)
    if (!c.isOpaque() && FlatUIUtils.getOutsideFlatBorder(c) == null && FlatUIUtils.hasOpaqueBeenExplicitlySet(c)) {
      return;
    }

    float focusWidth = FlatUIUtils.getBorderFocusWidth(c);
    float arc = FlatUIUtils.getBorderArc(c);

    // fill background if opaque to avoid garbage if user sets opaque to true
    if (c.isOpaque() && (focusWidth > 0 || arc > 0)) {
      FlatUIUtils.paintParentBackground(g, c);
    }

    // paint background
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      FlatUIUtils.setRenderingHints(g2);

      g2.setColor(getBackground(c, focusedBackground));
      FlatUIUtils.paintComponentBackground(g2, 0, 0, c.getWidth(), c.getHeight(), focusWidth, arc);
    }
    finally {
      g2.dispose();
    }
  }

  private static Color getBackground(JTextComponent c, Color focusedBackground) {
    Color background = c.getBackground();

    // always use explicitly set color
    if (!(background instanceof UIResource)) {
      return background;
    }

    // focused
    if (focusedBackground != null && FlatUIUtils.isPermanentFocusOwner(c)) {
      return focusedBackground;
    }

    return background;
  }
}
