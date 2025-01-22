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
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.swingbinding.JListBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.tasks.TvShowRemoveDatasourceTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.SquareIconButton;

import net.miginfocom.swing.MigLayout;

/**
 * The class TvShowSourcePanel is used to maintain the TV show data sources in the wizard
 * 
 * @author Manuel Laggner
 */
class TvShowSourcePanel extends JPanel {
  private final TvShowSettings settings = TvShowModuleManager.getInstance().getSettings();

  private JList<String>        listDataSources;

  public TvShowSourcePanel() {
    initComponents();
    initDataBindings();
  }

  /*
   * init components
   */
  private void initComponents() {
    setLayout(new MigLayout("", "[grow]", "[][grow]"));

    JLabel lblDataSource = new JLabel(TmmResourceBundle.getString("wizard.tvshow.datasources"));
    TmmFontHelper.changeFont(lblDataSource, 1.3333, Font.BOLD);
    add(lblDataSource, "cell 0 0");

    JPanel panelTvShowDataSources = new JPanel();
    add(panelTvShowDataSources, "cell 0 1,grow");
    panelTvShowDataSources.setLayout(new MigLayout("", "[grow][]", "[][grow]"));

    JTextArea tpDatasourceHint = new ReadOnlyTextArea(TmmResourceBundle.getString("wizard.datasource.hint"));
    panelTvShowDataSources.add(tpDatasourceHint, "cell 0 0 2 1,growx");

    JScrollPane scrollPaneDataSources = new JScrollPane();
    panelTvShowDataSources.add(scrollPaneDataSources, "cell 0 1,grow");

    listDataSources = new JList<>();
    scrollPaneDataSources.setViewportView(listDataSources);

    JButton btnAdd = new SquareIconButton(IconManager.ADD_INV);
    panelTvShowDataSources.add(btnAdd, "flowy,cell 1 1,aligny top");
    btnAdd.setToolTipText(TmmResourceBundle.getString("Button.add"));
    btnAdd.addActionListener(arg0 -> {
      String path = TmmProperties.getInstance().getProperty("tvshow.datasource.path");
      Path file = TmmUIHelper.selectDirectory(TmmResourceBundle.getString("Settings.tvshowdatasource.folderchooser"), path);
      if (file != null && Files.isDirectory(file)) {
        settings.addTvShowDataSources(file.toAbsolutePath().toString());
        TmmProperties.getInstance().putProperty("tvshow.datasource.path", file.toAbsolutePath().toString());
      }
    });

    JButton btnRemove = new SquareIconButton(IconManager.REMOVE_INV);
    panelTvShowDataSources.add(btnRemove, "cell 1 1");
    btnRemove.setToolTipText(TmmResourceBundle.getString("Button.remove"));
    btnRemove.addActionListener(arg0 -> {
      int row = listDataSources.getSelectedIndex();
      if (row != -1) { // nothing selected
        String path = settings.getTvShowDataSource().get(row);
        String[] choices = { TmmResourceBundle.getString("Button.continue"), TmmResourceBundle.getString("Button.abort") };
        int decision = JOptionPane.showOptionDialog(this, String.format(TmmResourceBundle.getString("Settings.tvshowdatasource.remove.info"), path),
            TmmResourceBundle.getString("Settings.datasource.remove"), JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, choices,
            TmmResourceBundle.getString("Button.abort"));
        if (decision == JOptionPane.YES_OPTION) {
          // to avoid interfering with active running tasks, we remove the data source as soon as all running main tasks are finished
          if (TmmTaskManager.getInstance().isMainTaskRunning()) {
            JOptionPane.showMessageDialog(this, TmmResourceBundle.getString("Settings.datasource.remove.hint"));
          }
          TmmTaskManager.getInstance().addMainTask(new TvShowRemoveDatasourceTask(path));
        }
      }
    });
  }

  protected void initDataBindings() {
    Property settingsBeanProperty_2 = BeanProperty.create("tvShowDataSource");
    JListBinding jListBinding = SwingBindings.createJListBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_2, listDataSources);
    jListBinding.bind();
  }
}
