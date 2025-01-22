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

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

/**
 * this kind of button is intended to be drawn square with just a icon in it
 * 
 * @author Manuel Laggner
 */
public class SquareIconButton extends JButton {

  public SquareIconButton(Icon icon) {
    super(icon);
    putClientProperty("JButton.squareSize", true);
  }

  public SquareIconButton(Action action) {
    super(action);
    putClientProperty("JButton.squareSize", true);
  }
}
