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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ExportTemplate;
import org.tinymediamanager.core.MediaEntityExporter;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.LinkTextArea;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.ReadOnlyTextPaneHTML;
import org.tinymediamanager.ui.components.TmmLabel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import net.miginfocom.swing.MigLayout;

/**
 * the class {@link ExporterPanel} is used to set the export template and path
 * 
 * @author Manuel Laggner
 */
public abstract class ExporterPanel extends AbstractModalInputPanel {
  private static final Logger           LOGGER = LoggerFactory.getLogger(ExporterPanel.class);

  protected final JTextField            tfExportDir;
  protected final JList<ExportTemplate> list;

  protected final String                panelId;

  public ExporterPanel(MediaEntityExporter.TemplateType type) {
    EventList<ExportTemplate> templatesFound = new BasicEventList<>();

    panelId = type.name() + ".exporter";

    setLayout(new MigLayout("", "[800lp,grow]", "[450lp,grow][]"));

    JSplitPane splitPane = new JSplitPane();
    splitPane.setName(panelId + ".splitPane");
    TmmUILayoutStore.getInstance().install(splitPane);
    splitPane.setResizeWeight(0.7);
    add(splitPane, "cell 0 0,grow");

    JScrollPane scrollPane = new JScrollPane();
    splitPane.setLeftComponent(scrollPane);

    list = new JList();
    scrollPane.setViewportView(list);

    JPanel panelExporterDetails = new JPanel();
    splitPane.setRightComponent(panelExporterDetails);
    panelExporterDetails.setLayout(new MigLayout("", "[100lp,grow]", "[][][][200lp,grow][]"));

    JLabel lblTemplateName = new JLabel("");
    TmmFontHelper.changeFont(lblTemplateName, TmmFontHelper.H1);
    panelExporterDetails.add(lblTemplateName, "cell 0 0,growx");

    JLabel lblUrl = new JLabel("");
    panelExporterDetails.add(lblUrl, "cell 0 1,growx");

    JCheckBox chckbxTemplateWithDetail = new JCheckBox("");
    chckbxTemplateWithDetail.setEnabled(false);
    panelExporterDetails.add(chckbxTemplateWithDetail, "flowx,cell 0 2");

    JScrollPane scrollPaneDescription = new NoBorderScrollPane();
    panelExporterDetails.add(scrollPaneDescription, "cell 0 3,grow");

    JTextPane taDescription = new ReadOnlyTextPaneHTML();
    scrollPaneDescription.setViewportView(taDescription);

    JLabel lblDetails = new TmmLabel(TmmResourceBundle.getString("export.detail"));
    panelExporterDetails.add(lblDetails, "cell 0 2,growx,aligny center");

    LinkTextArea taSource = new LinkTextArea();
    panelExporterDetails.add(taSource, "cell 0 4,growx");

    taSource.addActionListener(arg0 -> {
      if (!StringUtils.isEmpty(taSource.getText())) {
        // get the location from the label
        Path path = Paths.get(taSource.getText());
        TmmUIHelper.openFolder(path);
      }
    });

    splitPane.setDividerLocation(300);

    tfExportDir = new JTextField(TmmProperties.getInstance().getProperty(panelId + ".path"));
    add(tfExportDir, "flowx,cell 0 1,growx");
    tfExportDir.setColumns(10);

    JButton btnSetDestination = new JButton(TmmResourceBundle.getString("export.setdestination"));
    add(btnSetDestination, "cell 0 1");
    btnSetDestination.addActionListener(e -> {
      Path file = TmmUIHelper.selectDirectory(TmmResourceBundle.getString("export.selectdirectory"), tfExportDir.getText());
      if (file != null) {
        tfExportDir.setText(file.toAbsolutePath().toString());
        TmmProperties.getInstance().putProperty(panelId + ".path", tfExportDir.getText());
      }
    });

    btnClose.setText(TmmResourceBundle.getString("Toolbar.export"));

    templatesFound.addAll(MediaEntityExporter.findTemplates(type));

    list.setModel(new DefaultEventListModel<>(templatesFound));

    // set the last used template as default
    String lastTemplateName = TmmProperties.getInstance().getProperty(panelId + ".template");
    if (StringUtils.isNotBlank(lastTemplateName)) {
      list.setSelectedValue(lastTemplateName, true);
    }

    list.addListSelectionListener(e -> {
      ExportTemplate exportTemplate = list.getSelectedValue();
      if (exportTemplate == null) {
        lblTemplateName.setText("");
        lblDetails.setText("");
        taDescription.setText("");
        lblUrl.setText("");
        chckbxTemplateWithDetail.setSelected(false);
        taSource.setText("");
      }
      else {
        lblTemplateName.setText(exportTemplate.getName());
        taDescription.setText(exportTemplate.getDescription());
        lblUrl.setText(exportTemplate.getUrl());
        chckbxTemplateWithDetail.setSelected(exportTemplate.isDetail());
        taSource.setText(exportTemplate.getPath());
      }
    });
  }

  protected Path getExportPath() throws IOException {
    // check whether the chosen export path exists/is empty or not
    Path exportPath = Paths.get(tfExportDir.getText());
    if (!Files.exists(exportPath)) {
      // export dir does not exist
      JOptionPane.showMessageDialog(this, TmmResourceBundle.getString("export.foldernotfound"));
      throw new FileNotFoundException();
    }

    if (!Utils.isFolderEmpty(exportPath)) {
      Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
      int decision = JOptionPane.showOptionDialog(this, TmmResourceBundle.getString("export.foldernotempty"), "", JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE, null, options, null);// $NON-NLS-1$
      if (decision == JOptionPane.NO_OPTION) {
        throw new FileAlreadyExistsException(exportPath.toString());
      }
    }

    return exportPath;
  }
}
