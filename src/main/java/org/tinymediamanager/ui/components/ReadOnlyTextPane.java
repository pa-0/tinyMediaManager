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

import java.awt.ComponentOrientation;

import javax.swing.JTextPane;
import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;

/**
 * A readonly variant of the JTextPane
 *
 * @author Manuel Laggner
 */
public class ReadOnlyTextPane extends JTextPane {
  public ReadOnlyTextPane() {
    this("");
  }

  public ReadOnlyTextPane(String text) {
    setOpaque(false);
    setBorder(null);
    setEditable(false);
    setText(text);
    setFocusable(false);
    setForeground(UIManager.getColor("Label.foreground"));
  }

  @Override
  public void setText(String t) {
    // set the componentorientation if needed (switch from LTR -> RTL and vice versa)
    ComponentOrientation oldComponentOrientation = getComponentOrientation();
    ComponentOrientation newComponentOrientation;
    if (isRTL(t)) {
      newComponentOrientation = ComponentOrientation.RIGHT_TO_LEFT;
    }
    else {
      newComponentOrientation = ComponentOrientation.LEFT_TO_RIGHT;
    }

    if (newComponentOrientation != oldComponentOrientation) {
      setComponentOrientation(newComponentOrientation);
    }

    super.setText(t);
  }

  private boolean isRTL(String s) {
    if (StringUtils.isBlank(s)) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      byte d = Character.getDirectionality(s.charAt(i));
      if (d == Character.DIRECTIONALITY_RIGHT_TO_LEFT || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
          || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE) {
        return true;
      }
    }

    return false;
  }
}
