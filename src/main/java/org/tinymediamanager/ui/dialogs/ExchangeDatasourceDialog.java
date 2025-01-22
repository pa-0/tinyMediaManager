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
package org.tinymediamanager.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link ExchangeDatasourceDialog} is used to exchange a given data source in the settings with a new one
 *
 * @author Manuel Laggner
 */
public class ExchangeDatasourceDialog extends TmmDialog {

  private JLabel lblNewDatasource;

  public ExchangeDatasourceDialog(String datasource) {
    super(SettingsDialog.getInstance(), TmmResourceBundle.getString("Settings.exchangedatasource"), "exchangeDatasource");

    JPanel panelContent = new JPanel();
    getContentPane().add(panelContent, BorderLayout.CENTER);
    panelContent.setLayout(new MigLayout("", "[20lp!][400lp,grow]", "[][10lp!][][][10lp!][][][20lp!]"));
    {
      JTextPane tpDescription = new ReadOnlyTextPane(TmmResourceBundle.getString("Settings.exchangedatasource.desc"));
      panelContent.add(tpDescription, "cell 0 0 2 1,grow");

      JLabel lblOldDatasourceT = new TmmLabel(TmmResourceBundle.getString("Settings.exchangedatasource.old"));
      panelContent.add(lblOldDatasourceT, "cell 0 2 2 1");

      JLabel lblOldDatasource = new JLabel(datasource);
      panelContent.add(lblOldDatasource, "cell 1 3,growx");

      JLabel lblNewDatasourceT = new TmmLabel(TmmResourceBundle.getString("Settings.exchangedatasource.new"));
      panelContent.add(lblNewDatasourceT, "flowx,cell 0 5");

      JButton btnChooseNewDatasource = new SquareIconButton(IconManager.FILE_OPEN_INV);
      btnChooseNewDatasource.addActionListener(e -> {
        Path file = TmmUIHelper.selectDirectory(TmmResourceBundle.getString("Settings.datasource.folderchooser"), datasource);
        if (file != null && Files.isDirectory(file)) {
          lblNewDatasource.setText(file.toString());
        }
      });

      Component horizontalStrut = Box.createHorizontalStrut(30);
      panelContent.add(horizontalStrut, "cell 0 5");
      panelContent.add(btnChooseNewDatasource, "cell 0 5");

      lblNewDatasource = new JLabel("");
      panelContent.add(lblNewDatasource, "cell 1 6");
    }
    {
      JButton cancelButton = new JButton(TmmResourceBundle.getString("Button.cancel"));
      cancelButton.setIcon(IconManager.CANCEL_INV);
      cancelButton.addActionListener(e -> {
        lblNewDatasource.setText("");
        setVisible(false);
      });
      addButton(cancelButton);

      JButton okButton = new JButton(TmmResourceBundle.getString("Button.ok"));
      okButton.addActionListener(e -> {
        if (StringUtils.isBlank(lblNewDatasource.getText())) {
          JOptionPane.showMessageDialog(ExchangeDatasourceDialog.this, TmmResourceBundle.getString("Settings.exchangedatasource.error"),
              TmmResourceBundle.getString("Settings.exchangedatasource"), JOptionPane.ERROR_MESSAGE);
          return;
        }
        setVisible(false);
      });
      okButton.setIcon(IconManager.APPLY_INV);

      addButton(okButton);
    }
  }

  public String getNewDatasource() {
    return lblNewDatasource.getText();
  }
}
