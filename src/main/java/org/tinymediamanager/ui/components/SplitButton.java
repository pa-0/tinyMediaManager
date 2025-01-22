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

import java.awt.BorderLayout;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.tinymediamanager.ui.IconManager;

public class SplitButton extends JPanel {
  private static final String uiClassID = "SplitButtonUI";

  protected final JButton     actionButton;
  protected final JButton     menuButton;
  protected JPopupMenu        popupMenu;

  /**
   * Creates a button with initial text and an icon.
   *
   * @param text
   *          the text of the button
   * @param icon
   *          the Icon image to display on the button
   */
  public SplitButton(String text, Icon icon) {
    setLayout(new BorderLayout());

    actionButton = new JButton(text, icon);
    add(actionButton, BorderLayout.CENTER);

    menuButton = new JButton(IconManager.ARROW_DOWN_INV);
    menuButton.addActionListener(e -> {
      int x = -actionButton.getWidth();
      int y = menuButton.getHeight();

      popupMenu.show(menuButton, x, y);
    });
    add(menuButton, BorderLayout.EAST);

    popupMenu = new JPopupMenu();
    popupMenu.setLightWeightPopupEnabled(true);

    updateUI();
  }

  /**
   * Creates a button with text.
   *
   * @param text
   *          the text of the button
   */
  public SplitButton(String text) {
    this(text, null);
  }

  /**
   * Creates a button with an icon.
   *
   * @param icon
   *          the Icon image to display on the button
   */
  public SplitButton(Icon icon) {
    this(null, icon);
  }

  /**
   * Creates a button with no set text or icon.
   */
  public SplitButton() {
    this(null, null);
  }

  @Override
  public String getUIClassID() {
    return uiClassID;
  }

  /**
   * Returns the {@link JButton} (left side)
   * 
   * @return the {@link JButton}
   */
  public JButton getActionButton() {
    return actionButton;
  }

  /**
   * Returns the {@link JButton} (right side)
   *
   * @return the {@link JButton}
   */
  public JButton getMenuButton() {
    return menuButton;
  }

  /**
   * Returns the {@link JPopupMenu} (invoked when clicking on the right side)
   * 
   * @return the {@link JPopupMenu}
   */
  public JPopupMenu getPopupMenu() {
    return popupMenu;
  }
}
