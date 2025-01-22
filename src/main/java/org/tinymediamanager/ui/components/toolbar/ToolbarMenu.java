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

package org.tinymediamanager.ui.components.toolbar;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.ui.IconManager;

public class ToolbarMenu extends ToolbarLabel {
  public static final Color COLOR       = Color.GRAY;
  public static final Color COLOR_HOVER = Color.WHITE;

  protected JPopupMenu      popupMenu   = null;

  public ToolbarMenu(String text) {
    super(text);
  }

  public ToolbarMenu(String text, JPopupMenu popupMenu) {
    this(text);

    if (popupMenu != null) {
      setPopupMenu(popupMenu);
    }
  }

  @Override
  protected void setMouseListener() {
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent arg0) {
        setForeground(COLOR);
        if (popupMenu != null) {
          setIcon(IconManager.TOOLBAR_MENU_INDICATOR);
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
      }

      @Override
      public void mouseEntered(MouseEvent arg0) {
        setForeground(COLOR_HOVER);
        if (popupMenu != null) {
          setIcon(IconManager.TOOLBAR_MENU_INDICATOR_HOVER);
          setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
      }

      @Override
      public void mousePressed(MouseEvent e) {
        mouseClicked(e);
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (popupMenu != null) {
          int x = ToolbarMenu.this.getWidth() - (int) popupMenu.getPreferredSize().getWidth();
          // prevent the popupmenu from being displayed on another screen if x < 0
          if (x < 0) {
            x = 0;
          }
          int y = ToolbarMenu.this.getHeight();
          popupMenu.show(ToolbarMenu.this, x, y);
        }
      }
    });
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    this.popupMenu = popupMenu;
    if (popupMenu != null) {
      setComponentPopupMenu(popupMenu);
    }

    if (popupMenu == null || StringUtils.isBlank(popupMenu.getLabel())) {
      setText(defaultText);
    }
    else {
      setText(popupMenu.getLabel());
    }

    if (popupMenu != null) {
      setIcon(IconManager.TOOLBAR_MENU_INDICATOR);
    }
    else {
      setIcon(null);
    }
  }

  public JPopupMenu getPopupMenu() {
    return popupMenu;
  }
}
