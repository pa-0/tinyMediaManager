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

import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.UIManager;

/**
 * an own label class which has bold set and the scaling factor applied
 */
public class TmmMenuLabel extends JLabel {

  public TmmMenuLabel() {
    super();
    setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 0));
  }

  /**
   * create a new label with the given text
   *
   * @param text
   *          the text to be set
   */
  public TmmMenuLabel(String text) {
    super(text);
    setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 0));
  }

  @Override
  public void updateUI() {
    super.updateUI();
    // to re-set the font after UI change
    setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD));
  }

  protected Font scale(Font font, double factor) {
    int newSize = Math.round((float) (font.getSize() * factor));
    return font.deriveFont((float) newSize);
  }
}
