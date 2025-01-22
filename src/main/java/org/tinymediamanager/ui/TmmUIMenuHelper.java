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

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuListener;

/**
 * the class {@link TmmUIMenuHelper} offers helper methods for building menus
 * 
 * @author Manuel Laggner
 */
public class TmmUIMenuHelper {
  private TmmUIMenuHelper() {
    throw new IllegalAccessError();
  }

  public static JMenu cloneMenu(final JMenu menu) {
    final JMenu destination = new JMenu(menu.getText());
    destination.setIcon(menu.getIcon());

    for (Component component : menu.getMenuComponents()) {
      if (component instanceof JMenuItem menuItem) {
        destination.add(cloneMenuItem(menuItem));
      }
      else if (component instanceof JSeparator) {
        destination.addSeparator();
      }
    }

    return destination;
  }

  public static JMenu morphJPopupMenuToJMenu(final JPopupMenu source, String title) {
    return morphJPopupMenuToJMenu(source, title, null);
  }

  public static JMenu morphJPopupMenuToJMenu(final JPopupMenu source, String title, Icon icon) {
    final JMenu destination = new JMenu(title);

    if (icon != null) {
      destination.setIcon(icon);
    }

    for (Component component : source.getComponents()) {
      if (component instanceof JMenuItem menuItem) {
        destination.add(cloneMenuItem(menuItem));
      }
      else if (component instanceof JSeparator) {
        destination.addSeparator();
      }
    }

    return destination;
  }

  static JMenuItem cloneMenuItem(final JMenuItem item) {
    if (item == null) {
      return null;
    }

    JMenuItem jmi;
    if (item instanceof JMenu menu) {
      final JMenu jm = new JMenu();
      final int count = menu.getItemCount();

      jm.setText(menu.getText());
      jm.setIcon(menu.getIcon());

      for (PopupMenuListener popupMenuListener : menu.getPopupMenu().getPopupMenuListeners()) {
        if (popupMenuListener instanceof MenuListener menuListener) {
          jm.addMenuListener(menuListener);
        }
      }

      for (int i = 0; i < count; i++) {
        final JMenuItem ijmi = cloneMenuItem(menu.getItem(i));
        if (ijmi == null) {
          jm.addSeparator();
        }
        else {
          jm.add(ijmi);
        }
      }
      jmi = jm;
    }
    else {
      jmi = new JMenuItem();
      jmi.setText(item.getText());
      jmi.setIcon(item.getIcon());
    }

    for (ActionListener actionListener : item.getActionListeners()) {
      jmi.addActionListener(actionListener);
    }

    if (item.getAction() != null) {
      jmi.setAction(item.getAction());
    }

    return jmi;
  }
}
