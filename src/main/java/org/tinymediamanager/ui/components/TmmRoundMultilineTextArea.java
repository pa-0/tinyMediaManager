/*
 * Copyright 2012 - 2024 Manuel Laggner
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

import java.beans.BeanProperty;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.UIDefaults;

/**
 * The class {@link TmmRoundMultilineTextArea} is used to create a {@link JTextArea} with rounded borders
 * 
 * @author Manuel Laggner
 */
public class TmmRoundMultilineTextArea extends JTextArea {

  private static final String uiClassID = "RoundTextAreaUI";

  public TmmRoundMultilineTextArea() {
    super();
    init();
  }

  public TmmRoundMultilineTextArea(String text) {
    super(text);
    init();
  }

  @Override
  public void setCaretPosition(int position) {
    if (getCaret() != null) {
      super.setCaretPosition(position);
    }
  }

  protected void init() {
    setLineWrap(true);
    setWrapStyleWord(true);
  }

  /**
   * Returns the class ID for the UI.
   *
   * @return the string "TextAreaUI"
   * @see JComponent#getUIClassID
   * @see UIDefaults#getUI
   */
  @Override
  @BeanProperty(bound = false)
  public String getUIClassID() {
    return uiClassID;
  }
}
