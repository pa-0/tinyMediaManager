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
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.FocusListener;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;

import org.tinymediamanager.ui.components.datepicker.DatePicker;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;

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
public class TmmDatePickerUI extends TmmPanelUI {
  protected boolean     isIntelliJTheme;
  protected int         minimumWidth;
  protected Color       buttonBackground;
  protected Color       borderColor;

  private DatePicker    datePicker;
  private FocusListener focusListener;

  public static ComponentUI createUI(JComponent c) {
    return (FlatUIUtils.canUseSharedUI(c) ? FlatUIUtils.createSharedUI(TmmDatePickerUI.class, () -> new TmmDatePickerUI(true))
        : new TmmDatePickerUI(false));
  }

  protected TmmDatePickerUI(boolean shared) {
    super(shared);
  }

  protected String getPropertyPrefix() {
    return "DatePicker";
  }

  @Override
  public void installUI(JComponent c) {
    datePicker = (DatePicker) c;
    super.installUI(datePicker);
    installDefaults(datePicker);

    if (datePicker.getDateEditor() != null) {
      Insets insets = datePicker.getDateEditor().getInsets();
      datePicker.getDateEditor().setBorder(BorderFactory.createEmptyBorder(insets.top - 1, insets.left, insets.bottom / 2 + 1, insets.right));
      datePicker.getDateEditor().setOpaque(false);
    }
    if (datePicker.getCalendarButton() != null) {
      Insets insets = datePicker.getCalendarButton().getInsets();
      datePicker.getCalendarButton()
          .setBorder(BorderFactory.createEmptyBorder(insets.top - 1, insets.left + 3, insets.bottom / 2 + 1, insets.right + 2));
      datePicker.getCalendarButton().setContentAreaFilled(false);
      datePicker.getCalendarButton().setOpaque(false);
      datePicker.getCalendarButton().setBorderPainted(false);
    }
  }

  @Override
  protected void installDefaults(JPanel p) {
    super.installDefaults(p);

    String prefix = getPropertyPrefix();

    this.buttonBackground = UIManager.getColor(prefix + ".buttonBackground");
    this.minimumWidth = UIManager.getInt("Component.minimumWidth");
    this.borderColor = UIManager.getColor("Component.borderColor");
    this.isIntelliJTheme = UIManager.getBoolean("Component.isIntelliJTheme");

    Color bg = p.getBackground();
    if ((bg == null) || (bg instanceof UIResource)) {
      p.setBackground(UIManager.getColor(prefix + ".background"));
    }

    Border b = p.getBorder();
    if ((b == null) || (b instanceof UIResource)) {
      p.setBorder(UIManager.getBorder(prefix + ".border"));
    }
  }

  @Override
  public void uninstallUI(JComponent c) {
    DatePicker p = (DatePicker) c;
    uninstallDefaults(p);
    super.uninstallUI(c);

    c.removeFocusListener(this.focusListener);
    this.focusListener = null;
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    paintBackground(g, c);
    super.paint(g, c);
  }

  void paintBackground(Graphics g, JComponent c) {
    if (c.isOpaque() || FlatUIUtils.getOutsideFlatBorder(c) != null || !FlatUIUtils.hasOpaqueBeenExplicitlySet(c)) {
      float focusWidth = FlatUIUtils.getBorderFocusWidth(datePicker);
      float arc = FlatUIUtils.getBorderArc(c);
      if (c.isOpaque() && (focusWidth > 0.0F || arc > 0.0F)) {
        FlatUIUtils.paintParentBackground(g, c);
      }

      Graphics2D g2 = (Graphics2D) g.create();

      try {
        int width = c.getWidth();
        int height = c.getHeight();

        FlatUIUtils.setRenderingHints(g2);
        Color background = c.getBackground();
        g2.setColor(!(background instanceof UIResource) ? background
            : (isIntelliJTheme && (!c.isEnabled() || !c.isEnabled()) ? FlatUIUtils.getParentBackground(c) : background));
        FlatUIUtils.paintComponentBackground(g2, 0, 0, width, height, focusWidth, arc);

        int arrowX = width - datePicker.getCalendarButton().getWidth() - (int) focusWidth;

        g2.setColor(buttonBackground);
        Shape oldClip = g2.getClip();
        g2.clipRect(arrowX, 0, width - arrowX, height);

        FlatUIUtils.paintComponentBackground(g2, 0, 0, width, height, focusWidth, arc);
        g2.setClip(oldClip);

        g2.setColor(this.borderColor);
        float lw = UIScale.scale(1.0F);
        float lx = arrowX;
        g2.fill(new Rectangle2D.Float(lx, focusWidth, lw, (float) (height - 1) - focusWidth * 2.0F));
      }
      finally {
        g2.dispose();
      }
    }
  }
}
