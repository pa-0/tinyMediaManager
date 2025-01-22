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

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import org.apache.commons.lang3.StringUtils;

/**
 * the class {@link TmmObligatoryTextArea} is used to indicate that the text must not be empty
 *
 * @author Manuel Laggner
 */
public class TmmObligatoryTextArea extends TmmRoundTextArea {
  public TmmObligatoryTextArea() {
    super();
  }

  public TmmObligatoryTextArea(String text) {
    super(text);
  }

  @Override
  protected void init() {
    super.init();

    addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        if (StringUtils.isBlank(getText())) {
          putClientProperty("JComponent.outline", "error");
        }
        else {
          putClientProperty("JComponent.outline", null);
        }
        revalidate();
      }
    });
  }
}
