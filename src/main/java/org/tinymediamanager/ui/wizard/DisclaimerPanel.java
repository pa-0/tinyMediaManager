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
package org.tinymediamanager.ui.wizard;

import java.awt.Font;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;

import net.miginfocom.swing.MigLayout;

/**
 * The class DisclaimerPanel is used to display a disclaimer to the users
 * 
 * @author Manuel Laggner
 */
class DisclaimerPanel extends JPanel {
  private final TinyMediaManagerWizard wizard;

  private JCheckBox                    chckbxAccept;

  public DisclaimerPanel(TinyMediaManagerWizard wizard) {
    this.wizard = wizard;
    initComponents();
  }

  /*
   * init UI components
   */
  private void initComponents() {
    setLayout(new MigLayout("", "[400lp:400lp,grow]", "[][100lp:150lp,grow][20lp:n]"));
    {
      JLabel lblDisclaimer = new JLabel(TmmResourceBundle.getString("wizard.disclaimer"));
      TmmFontHelper.changeFont(lblDisclaimer, 1.3333, Font.BOLD);
      add(lblDisclaimer, "cell 0 0,growx, wmin 0");
    }
    {
      JScrollPane scrollPane = new NoBorderScrollPane();
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      add(scrollPane, "cell 0 1,grow");

      JTextArea taDisclaimer = new ReadOnlyTextArea(TmmResourceBundle.getString("wizard.disclaimer.long"));
      scrollPane.setViewportView(taDisclaimer);
    }

    chckbxAccept = new JCheckBox(TmmResourceBundle.getString("wizard.disclaimer.accept"));
    chckbxAccept.addActionListener(l -> {
      if (chckbxAccept.isSelected()) {
        wizard.getBtnNext().setEnabled(true);
      }
      else {
        wizard.getBtnNext().setEnabled(false);
      }
    });
    add(chckbxAccept, "cell 0 2");
  }

  @Override
  public void setVisible(boolean aFlag) {
    super.setVisible(aFlag);
    if (aFlag) {
      if (chckbxAccept.isSelected()) {
        wizard.getBtnNext().setEnabled(true);
      }
      else {
        wizard.getBtnNext().setEnabled(false);
      }
    }
    else {
      wizard.getBtnNext().setEnabled(true);
    }
  }
}
