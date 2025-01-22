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
package org.tinymediamanager.ui.panels;

import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;

import net.miginfocom.swing.MigLayout;

public class RegexInputPanel extends AbstractModalInputPanel {
  private String           regularExpression;

  private final JTextField tfExpression;
  private final JLabel     lblInvalidExpression;

  public RegexInputPanel() {
    setLayout(new MigLayout("", "[300lp,grow]", "[][][]"));
    {
      JLabel lblDescription = new JLabel(TmmResourceBundle.getString("tmm.regexp"));
      add(lblDescription, "cell 0 0");
    }
    {
      tfExpression = new JTextField();
      add(tfExpression, "cell 0 1,growx");
      tfExpression.setColumns(10);
    }
    {
      lblInvalidExpression = new JLabel(TmmResourceBundle.getString("tmm.regexp.invalid"));
      add(lblInvalidExpression, "cell 0 2");
    }

    lblInvalidExpression.setVisible(false);
    btnClose.setEnabled(false);

    tfExpression.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent arg0) {
        checkExpression();
      }

      @Override
      public void insertUpdate(DocumentEvent arg0) {
        checkExpression();
      }

      @Override
      public void changedUpdate(DocumentEvent arg0) {
        checkExpression();
      }

      private void checkExpression() {
        String text = tfExpression.getText();
        if (StringUtils.isBlank(text)) {
          lblInvalidExpression.setVisible(false);
          btnClose.setEnabled(false);
        }
        else {
          try {
            Pattern.compile(text);
            lblInvalidExpression.setVisible(false);
            btnClose.setEnabled(true);
          }
          catch (Exception e) {
            lblInvalidExpression.setVisible(true);
            btnClose.setEnabled(false);
          }
        }
      }
    });

    SwingUtilities.invokeLater(tfExpression::requestFocus);
  }

  @Override
  protected void onClose() {
    regularExpression = tfExpression.getText();
    setVisible(false);
  }

  public String getRegularExpression() {
    return regularExpression;
  }
}
