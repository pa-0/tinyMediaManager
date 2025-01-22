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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import org.tinymediamanager.ui.components.FlatButton;

public class ToolbarButton extends FlatButton {
  private JPopupMenu popupMenu;

  ToolbarButton(Icon baseIcon) {
    this(baseIcon, null, null);
  }

  public ToolbarButton(Icon baseIcon, Icon hoverIcon) {
    this(baseIcon, hoverIcon, null);
  }

  public ToolbarButton(Icon baseIcon, Icon hoverIcon, JPopupMenu popupMenu) {
    super(baseIcon);
    this.popupMenu = popupMenu;

    setVerticalAlignment(CENTER);
    setHorizontalAlignment(CENTER);
    setVerticalTextPosition(SwingConstants.CENTER);
    setHorizontalTextPosition(SwingConstants.CENTER);
    setHideActionText(true);
    setRolloverIcon(hoverIcon);
    setPressedIcon(hoverIcon);
    updateUI();

    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent arg0) {
        mouseClicked(arg0);
      }

      @Override
      public void mouseClicked(MouseEvent arg0) {
        if (ToolbarButton.this.popupMenu != null) {
          ToolbarButton.this.popupMenu.show(ToolbarButton.this, ToolbarButton.this.getWidth(), 0);
        }
      }
    });
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    this.popupMenu = popupMenu;
  }

  @Override
  public void setAction(Action a) {
    super.setAction(a);
    setEnabled(a != null);
  }

  @Override
  protected void configurePropertiesFromAction(Action a) {
    // only set tooltip from action
    setToolTipText(a != null ? (String) a.getValue(Action.SHORT_DESCRIPTION) : null);
  }

  void setIcons(Icon baseIcon, Icon hoverIcon) {
    setIcon(baseIcon);
    setRolloverIcon(hoverIcon);
    setPressedIcon(hoverIcon);
  }
}
