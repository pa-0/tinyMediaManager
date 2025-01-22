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
package org.tinymediamanager.ui;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * the class {@link TmmLazyMenuAdapter} offers a generic class for a {@link java.awt.PopupMenu} and {@link javax.swing.JMenu} being populates lazily
 * 
 * @author Manuel Laggner
 */
public abstract class TmmLazyMenuAdapter implements PopupMenuListener, MenuListener {

  protected abstract void menuWillBecomeVisible(JMenu menu);

  @Override
  public void menuSelected(MenuEvent e) {
    if (e.getSource() instanceof JMenu menu) {
      menuWillBecomeVisible(menu);
    }
  }

  @Override
  public void menuDeselected(MenuEvent e) {
    // not needed
  }

  @Override
  public void menuCanceled(MenuEvent e) {
    // not needed
  }

  @Override
  public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    if (e.getSource() instanceof JPopupMenu popupMenu && popupMenu.getInvoker() instanceof JMenu menu) {
      menuWillBecomeVisible(menu);
    }
  }

  @Override
  public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    // not needed
  }

  @Override
  public void popupMenuCanceled(PopupMenuEvent e) {
    // not needed
  }
}
