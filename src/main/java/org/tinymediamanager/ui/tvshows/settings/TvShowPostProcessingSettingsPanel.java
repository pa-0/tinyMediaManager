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
package org.tinymediamanager.ui.tvshows.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.swingbinding.JTableBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.PostProcess;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowPostProcessDialog;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link TvShowPostProcessingSettingsPanel} holds the settings for post process action for movies
 *
 * @author Wolfgang Janes
 */
public class TvShowPostProcessingSettingsPanel extends JPanel {

  private final TvShowSettings settings = TvShowModuleManager.getInstance().getSettings();

  private TmmTable             tablePostProcessesEpisode;
  private TmmTable             tablePostProcessesTvShow;
  private JButton              btnRemoveProcessTvShow;
  private JButton              btnAddProcessTvShow;
  private JButton              btnEditProcessTvShow;
  private JButton              btnAddProcessEpisode;
  private JButton              btnEditProcessEpisode;
  private JButton              btnRemoveProcessEpisode;

  TvShowPostProcessingSettingsPanel() {

    initComponents();
    initDataBindings();

    // button listeners
    btnAddProcessTvShow.addActionListener(e -> {
      TvShowPostProcessDialog.showTvShowPostProcessDialog();
      tablePostProcessesTvShow.adjustColumnPreferredWidths(5);
    });

    btnRemoveProcessTvShow.addActionListener(e -> {
      int row = tablePostProcessesTvShow.getSelectedRow();
      row = tablePostProcessesTvShow.convertRowIndexToModel(row);

      if (row != -1) {
        PostProcess process = settings.getPostProcessTvShow().get(row);
        settings.removePostProcessTvShow(process);
        tablePostProcessesTvShow.adjustColumnPreferredWidths(5);
      }
    });

    btnEditProcessTvShow.addActionListener(e -> {
      int row = tablePostProcessesTvShow.getSelectedRow();
      row = tablePostProcessesTvShow.convertRowIndexToModel(row);

      if (row != -1) {
        PostProcess process = settings.getPostProcessTvShow().get(row);
        if (process != null) {
          TvShowPostProcessDialog.showTvShowPostProcessDialog(process);
          tablePostProcessesTvShow.adjustColumnPreferredWidths(5);
        }
      }
    });

    btnAddProcessEpisode.addActionListener(e -> {
      TvShowPostProcessDialog.showEpisodePostProcessDialog();
      tablePostProcessesEpisode.adjustColumnPreferredWidths(5);
    });

    btnRemoveProcessEpisode.addActionListener(e -> {
      int row = tablePostProcessesEpisode.getSelectedRow();
      row = tablePostProcessesEpisode.convertRowIndexToModel(row);

      if (row != -1) {
        PostProcess process = settings.getPostProcessEpisode().get(row);
        settings.removePostProcessEpisode(process);
      }
    });

    btnEditProcessEpisode.addActionListener(e -> {
      int row = tablePostProcessesEpisode.getSelectedRow();
      row = tablePostProcessesEpisode.convertRowIndexToModel(row);

      if (row != -1) {
        PostProcess process = settings.getPostProcessEpisode().get(row);
        if (process != null) {
          TvShowPostProcessDialog.showEpisodePostProcessDialog(process);
          tablePostProcessesEpisode.adjustColumnPreferredWidths(5);
        }
      }
    });

    // set Column Headers
    tablePostProcessesTvShow.getColumnModel().getColumn(0).setHeaderValue(TmmResourceBundle.getString("Settings.processname"));
    tablePostProcessesTvShow.getColumnModel().getColumn(1).setHeaderValue(TmmResourceBundle.getString("metatag.path"));
    tablePostProcessesTvShow.getColumnModel().getColumn(2).setHeaderValue(TmmResourceBundle.getString("Settings.commandname"));
    tablePostProcessesTvShow.adjustColumnPreferredWidths(5);

    tablePostProcessesEpisode.getColumnModel().getColumn(0).setHeaderValue(TmmResourceBundle.getString("Settings.processname"));
    tablePostProcessesEpisode.getColumnModel().getColumn(1).setHeaderValue(TmmResourceBundle.getString("metatag.path"));
    tablePostProcessesEpisode.getColumnModel().getColumn(2).setHeaderValue(TmmResourceBundle.getString("Settings.commandname"));
    tablePostProcessesEpisode.adjustColumnPreferredWidths(5);
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[]"));
    {
      JPanel panelProcess = new JPanel(
          new MigLayout("hidemode 1, insets 0", "[20lp!][300lp:600lp,grow][]", "[][grow][150lp:200lp,grow][][][grow][150lp:200lp,grow]"));
      JLabel lblProcess = new TmmLabel(TmmResourceBundle.getString("Settings.postprocessing"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelProcess, lblProcess, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#post-processing"));
      add(collapsiblePanel, "growx,wmin 0");

      {
        JLabel lblTvShow = new TmmLabel(TmmResourceBundle.getString("metatag.tvshow"));
        panelProcess.add(lblTvShow, "cell 1 0");

        JTextPane tpTvShowDescription = new ReadOnlyTextPane(TmmResourceBundle.getString("Settings.tvshow.postprocess.tvshowhint"));
        panelProcess.add(tpTvShowDescription, "cell 1 1 2 1,grow");

        {
          JScrollPane spProcesses = new JScrollPane();
          panelProcess.add(spProcesses, "cell 1 2,grow");
          tablePostProcessesTvShow = new TmmTable();
          tablePostProcessesTvShow.configureScrollPane(spProcesses);

          btnAddProcessTvShow = new JButton(TmmResourceBundle.getString("Button.add"));
          panelProcess.add(btnAddProcessTvShow, "flowy,cell 2 2,growx,aligny top");

          btnEditProcessTvShow = new JButton(TmmResourceBundle.getString("Button.edit"));
          panelProcess.add(btnEditProcessTvShow, "cell 2 2,growx");

          btnRemoveProcessTvShow = new JButton(TmmResourceBundle.getString("Button.remove"));
          panelProcess.add(btnRemoveProcessTvShow, "cell 2 2,growx");
        }
      }

      JSeparator separator = new JSeparator();
      panelProcess.add(separator, "cell 1 3 2 1,growx");

      {
        JLabel lblEpisode = new TmmLabel(TmmResourceBundle.getString("metatag.episode"));
        panelProcess.add(lblEpisode, "cell 1 4");

        JTextPane tpEpisodeDescription = new ReadOnlyTextPane(TmmResourceBundle.getString("Settings.tvshow.postprocess.episodehint"));
        panelProcess.add(tpEpisodeDescription, "cell 1 5 2 1,grow");

        {
          JScrollPane spProcesses = new JScrollPane();
          panelProcess.add(spProcesses, "cell 1 6,grow");
          tablePostProcessesEpisode = new TmmTable();
          tablePostProcessesEpisode.configureScrollPane(spProcesses);

          btnAddProcessEpisode = new JButton(TmmResourceBundle.getString("Button.add"));
          panelProcess.add(btnAddProcessEpisode, "flowy,cell 2 6,growx,aligny top");

          btnEditProcessEpisode = new JButton(TmmResourceBundle.getString("Button.edit"));
          panelProcess.add(btnEditProcessEpisode, "cell 2 6,growx");

          btnRemoveProcessEpisode = new JButton(TmmResourceBundle.getString("Button.remove"));
          panelProcess.add(btnRemoveProcessEpisode, "cell 2 6,growx");
        }
      }
    }
  }

  protected void initDataBindings() {
    Property settingsBeanProperty = BeanProperty.create("postProcessTvShow");
    JTableBinding jTableBinding = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty,
        tablePostProcessesTvShow);
    //
    Property wolBeanProperty_1 = BeanProperty.create("name");
    jTableBinding.addColumnBinding(wolBeanProperty_1);
    //
    Property wolBeanProperty_3 = BeanProperty.create("path");
    jTableBinding.addColumnBinding(wolBeanProperty_3);
    //
    Property wolBeanProperty_4 = BeanProperty.create("command");
    jTableBinding.addColumnBinding(wolBeanProperty_4);
    //
    jTableBinding.setEditable(false);
    jTableBinding.bind();
    //
    Property tvShowSettingsBeanProperty = BeanProperty.create("postProcessEpisode");
    JTableBinding jTableBinding_1 = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty,
        tablePostProcessesEpisode);
    //
    Property postProcessBeanProperty = BeanProperty.create("name");
    jTableBinding_1.addColumnBinding(postProcessBeanProperty);
    //
    Property postProcessBeanProperty_1 = BeanProperty.create("path");
    jTableBinding_1.addColumnBinding(postProcessBeanProperty_1);
    //
    Property postProcessBeanProperty_2 = BeanProperty.create("command");
    jTableBinding_1.addColumnBinding(postProcessBeanProperty_2);
    //
    jTableBinding_1.bind();
  }
}
