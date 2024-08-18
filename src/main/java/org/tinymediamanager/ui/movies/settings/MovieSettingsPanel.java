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
package org.tinymediamanager.ui.movies.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.GridBagConstraints;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.MovieSettingsDefaults;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.thirdparty.trakttv.MovieClearTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.dialogs.SettingsDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The class MovieSettingsPanel is used for displaying some movie related settings
 * 
 * @author Manuel Laggner
 */
public class MovieSettingsPanel extends JPanel {
  private static final int    COL_COUNT = 7;

  private final MovieSettings settings  = MovieModuleManager.getInstance().getSettings();

  private JButton             btnClearTraktData;
  private JCheckBox           chckbxTraktSync;
  private JCheckBox           chckbxRenameAfterScrape;
  private JCheckBox           chckbxAutoUpdateOnStart;
  private JCheckBox           chckbxBuildImageCache;
  private JCheckBox           chckbxExtractArtworkFromVsmeta;
  private JCheckBox           chckbxRuntimeFromMi;
  private JButton             btnPresetKodi;
  private JButton             btnPresetXbmc;
  private JButton             btnPresetMediaPortal1;
  private JButton             btnPresetMediaPortal2;
  private JButton             btnPresetPlex;
  private JButton             btnPresetMediaPig;
  private JCheckBox           chckbxIncludeExternalAudioStreams;
  private JCheckBox           chckbxUseMediainfoMetadata;

  private JCheckBox           chckbxTraktSyncWatched;
  private JCheckBox           chckbxTraktSyncRating;
  private JCheckBox           chckbxTraktSyncCollection;
  private JButton             btnPresetJellyfin;
  private JButton             btnPresetEmby;
  private JCheckBox           chckbxResetNewFlag;

  public MovieSettingsPanel() {
    // UI initializations
    initComponents();
    initDataBindings();

    btnClearTraktData.addActionListener(e -> {
      Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
      int confirm = JOptionPane.showOptionDialog(SettingsDialog.getInstance(), TmmResourceBundle.getString("Settings.trakt.clearmovies.hint"),
          TmmResourceBundle.getString("Settings.trakt.clearmovies"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
      if (confirm == JOptionPane.YES_OPTION) {
        TmmTask task = new MovieClearTraktTvTask();
        TmmTaskManager.getInstance().addUnnamedTask(task);
      }
    });

    btnPresetXbmc.addActionListener(evt -> {
      MovieSettingsDefaults.setDefaultSettingsForXbmc();
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.preset.message").replace("{}", "XBMC"),
          TmmResourceBundle.getString("Settings.preset"), JOptionPane.INFORMATION_MESSAGE);
    });
    btnPresetKodi.addActionListener(evt -> {
      MovieSettingsDefaults.setDefaultSettingsForKodi();
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.preset.message").replace("{}", "Kodi"),
          TmmResourceBundle.getString("Settings.preset"), JOptionPane.INFORMATION_MESSAGE);
    });
    btnPresetJellyfin.addActionListener(evt -> {
      MovieSettingsDefaults.setDefaultSettingsForJellyfin();
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.preset.message").replace("{}", "Jellyfin"),
          TmmResourceBundle.getString("Settings.preset"), JOptionPane.INFORMATION_MESSAGE);
    });
    btnPresetEmby.addActionListener(evt -> {
      MovieSettingsDefaults.setDefaultSettingsForEmby();
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.preset.message").replace("{}", "Emby"),
          TmmResourceBundle.getString("Settings.preset"), JOptionPane.INFORMATION_MESSAGE);
    });
    btnPresetPlex.addActionListener(evt -> {
      MovieSettingsDefaults.setDefaultSettingsForPlex();
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.preset.message").replace("{}", "Plex"),
          TmmResourceBundle.getString("Settings.preset"), JOptionPane.INFORMATION_MESSAGE);
    });
    btnPresetMediaPig.addActionListener(evt -> {
      MovieSettingsDefaults.setDefaultSettingsForMediaPig();
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.preset.message").replace("{}", "MediaPIG"),
          TmmResourceBundle.getString("Settings.preset"), JOptionPane.INFORMATION_MESSAGE);
    });
    btnPresetMediaPortal1.addActionListener(evt -> {
      MovieSettingsDefaults.setDefaultSettingsForMediaPortal1();
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.preset.message").replace("{}", "MediaPortal 1.x"),
          TmmResourceBundle.getString("Settings.preset"), JOptionPane.INFORMATION_MESSAGE);
    });
    btnPresetMediaPortal2.addActionListener(evt -> {
      MovieSettingsDefaults.setDefaultSettingsForMediaPortal2();
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.preset.message").replace("{}", "MediaPortal 2.x"),
          TmmResourceBundle.getString("Settings.preset"), JOptionPane.INFORMATION_MESSAGE);
    });
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[grow]", "[][15lp!][][15lp!][]"));
    {
      JPanel panelAutomaticTasks = new JPanel();
      // 16lp ~ width of the checkbox
      panelAutomaticTasks.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][][15lp!][]"));

      JLabel lblAutomaticTasksT = new TmmLabel(TmmResourceBundle.getString("Settings.automatictasks"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelAutomaticTasks, lblAutomaticTasksT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#automatic-tasks"));
      {
        JPanel panelPresets = new JPanel(
            new MigLayout("hidemode 1, insets 0", "[20lp!][15lp][120lp:n][15lp!][120lp:n][15lp!][120lp:n][15lp!][120lp:n][grow]", "[][][][]"));

        JLabel lblPresets = new TmmLabel(TmmResourceBundle.getString("Settings.preset"), H3);
        CollapsiblePanel collapsiblePanel_1 = new CollapsiblePanel(panelPresets, lblPresets, true);
        collapsiblePanel_1.addExtraTitleComponent(new DocsButton("/movies/settings#media-center-presets"));
        add(collapsiblePanel_1, "cell 0 0,growx,wmin 0");
        {
          JLabel lblPresetHintT = new JLabel(TmmResourceBundle.getString("Settings.preset.desc"));
          panelPresets.add(lblPresetHintT, "cell 1 0 7 1");
        }
        {
          btnPresetKodi = new JButton("Kodi v17+");
          panelPresets.add(btnPresetKodi, "cell 2 1,growx");

          btnPresetXbmc = new JButton("XBMC/Kodi <v17");
          panelPresets.add(btnPresetXbmc, "cell 4 1,growx");
        }
        {
          btnPresetPlex = new JButton("Plex");
          panelPresets.add(btnPresetPlex, "cell 2 2,growx");

          btnPresetJellyfin = new JButton("Jellyfin");
          panelPresets.add(btnPresetJellyfin, "cell 4 2,growx");

          btnPresetEmby = new JButton("Emby");
          panelPresets.add(btnPresetEmby, "cell 6 2,growx");

          btnPresetMediaPig = new JButton("MediaPIG");
          panelPresets.add(btnPresetMediaPig, "cell 8 2,growx");
        }
        {
          btnPresetMediaPortal1 = new JButton("MediaPortal 1.x");
          panelPresets.add(btnPresetMediaPortal1, "cell 2 3,growx");

          btnPresetMediaPortal2 = new JButton("MediaPortal 2.x");
          panelPresets.add(btnPresetMediaPortal2, "cell 4 3,growx");
        }
      }
      add(collapsiblePanel, "cell 0 2,growx,wmin 0");
      {
        chckbxRenameAfterScrape = new JCheckBox(TmmResourceBundle.getString("Settings.movie.automaticrename"));
        panelAutomaticTasks.add(chckbxRenameAfterScrape, "cell 1 0 2 1");

        JLabel lblAutomaticRenameHint = new JLabel(IconManager.HINT);
        lblAutomaticRenameHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.automaticrename.desc"));
        panelAutomaticTasks.add(lblAutomaticRenameHint, "cell 1 0 2 1");

        chckbxTraktSync = new JCheckBox(TmmResourceBundle.getString("Settings.trakt"));
        panelAutomaticTasks.add(chckbxTraktSync, "cell 1 2 2 1");

        btnClearTraktData = new JButton(TmmResourceBundle.getString("Settings.trakt.clearmovies"));
        panelAutomaticTasks.add(btnClearTraktData, "cell 1 2 2 1");

        chckbxTraktSyncCollection = new JCheckBox(TmmResourceBundle.getString("Settings.trakt.collection"));
        panelAutomaticTasks.add(chckbxTraktSyncCollection, "cell 2 3");

        chckbxTraktSyncWatched = new JCheckBox(TmmResourceBundle.getString("Settings.trakt.watched"));
        panelAutomaticTasks.add(chckbxTraktSyncWatched, "cell 2 4");

        chckbxTraktSyncRating = new JCheckBox(TmmResourceBundle.getString("Settings.trakt.rating"));
        panelAutomaticTasks.add(chckbxTraktSyncRating, "cell 2 5");

        chckbxAutoUpdateOnStart = new JCheckBox(TmmResourceBundle.getString("Settings.movie.automaticupdate"));
        panelAutomaticTasks.add(chckbxAutoUpdateOnStart, "cell 1 7 2 1");

        JLabel lblAutomaticUpdateHint = new JLabel(IconManager.HINT);
        lblAutomaticUpdateHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.automaticupdate.desc"));
        panelAutomaticTasks.add(lblAutomaticUpdateHint, "cell 1 7 2 1");

      }
    }
    {
      JPanel panelMisc = new JPanel();
      panelMisc.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][][]")); // 16lp ~ width of the

      JLabel lblMiscT = new TmmLabel(TmmResourceBundle.getString("Settings.misc"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMisc, lblMiscT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#misc-settings"));
      add(collapsiblePanel, "cell 0 4,growx,wmin 0");

      chckbxResetNewFlag = new JCheckBox(TmmResourceBundle.getString("Settings.resetnewflag"));
      panelMisc.add(chckbxResetNewFlag, "cell 1 0 2 1");

      chckbxUseMediainfoMetadata = new JCheckBox(TmmResourceBundle.getString("Settings.usemediainfometadata"));
      panelMisc.add(chckbxUseMediainfoMetadata, "cell 1 1 2 1");
      {
        chckbxExtractArtworkFromVsmeta = new JCheckBox(TmmResourceBundle.getString("Settings.extractartworkfromvsmeta"));
        panelMisc.add(chckbxExtractArtworkFromVsmeta, "cell 1 2 2 1");

        chckbxBuildImageCache = new JCheckBox(TmmResourceBundle.getString("Settings.imagecacheimport"));
        panelMisc.add(chckbxBuildImageCache, "cell 1 3 2 1");

        JLabel lblBuildImageCacheHint = new JLabel(IconManager.HINT);
        lblBuildImageCacheHint.setToolTipText(TmmResourceBundle.getString("Settings.imagecacheimporthint"));
        panelMisc.add(lblBuildImageCacheHint, "cell 1 3 2 1");

        chckbxRuntimeFromMi = new JCheckBox(TmmResourceBundle.getString("Settings.runtimefrommediafile"));
        panelMisc.add(chckbxRuntimeFromMi, "cell 1 4 2 1");

        chckbxIncludeExternalAudioStreams = new JCheckBox(TmmResourceBundle.getString("Settings.includeexternalstreamsinnfo"));
        panelMisc.add(chckbxIncludeExternalAudioStreams, "cell 1 5 2 1");
      }
    }
  }

  private void addMetadataCheckbox(JPanel panel, MovieScraperMetadataConfig config, Map<MovieScraperMetadataConfig, JCheckBox> map,
      GridBagConstraints gbc) {
    JCheckBox checkBox;
    if (StringUtils.isNotBlank(config.getToolTip())) {
      checkBox = new JHintCheckBox(config.getDescription());
      checkBox.setToolTipText(config.getToolTip());
      ((JHintCheckBox) checkBox).setHintIcon(IconManager.HINT);
    }
    else {
      checkBox = new JCheckBox(config.getDescription());
    }
    map.put(config, checkBox);

    if (gbc.gridx >= COL_COUNT) {
      gbc.gridx = 0;
      gbc.gridy++;
    }
    panel.add(checkBox, gbc);

    gbc.gridx++;
  }

  protected void initDataBindings() {
    Property movieSettingsBeanProperty_1 = BeanProperty.create("renameAfterScrape");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_1, chckbxRenameAfterScrape,
        jCheckBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property movieSettingsBeanProperty_2 = BeanProperty.create("syncTrakt");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_2, chckbxTraktSync,
        jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property movieSettingsBeanProperty_3 = BeanProperty.create("buildImageCacheOnImport");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_3, chckbxBuildImageCache,
        jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    Property movieSettingsBeanProperty_4 = BeanProperty.create("runtimeFromMediaInfo");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_4, chckbxRuntimeFromMi,
        jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property movieSettingsBeanProperty_9 = BeanProperty.create("includeExternalAudioStreams");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_9,
        chckbxIncludeExternalAudioStreams, jCheckBoxBeanProperty);
    autoBinding_9.bind();
    //
    Property movieSettingsBeanProperty_11 = BeanProperty.create("extractArtworkFromVsmeta");
    AutoBinding autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_11,
        chckbxExtractArtworkFromVsmeta, jCheckBoxBeanProperty);
    autoBinding_11.bind();
    //
    Property movieSettingsBeanProperty_12 = BeanProperty.create("updateOnStart");
    AutoBinding autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_12,
        chckbxAutoUpdateOnStart, jCheckBoxBeanProperty);
    autoBinding_12.bind();
    //
    Property movieSettingsBeanProperty_6 = BeanProperty.create("useMediainfoMetadata");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_6,
        chckbxUseMediainfoMetadata, jCheckBoxBeanProperty);
    autoBinding_5.bind();
    //
    Property jCheckBoxBeanProperty_1 = BeanProperty.create("enabled");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxTraktSync, jCheckBoxBeanProperty, chckbxTraktSyncCollection,
        jCheckBoxBeanProperty_1);
    autoBinding_6.bind();
    //
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxTraktSync, jCheckBoxBeanProperty, chckbxTraktSyncWatched,
        jCheckBoxBeanProperty_1);
    autoBinding_7.bind();
    //
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxTraktSync, jCheckBoxBeanProperty, chckbxTraktSyncRating,
        jCheckBoxBeanProperty_1);
    autoBinding_10.bind();
    //
    Property movieSettingsBeanProperty_7 = BeanProperty.create("syncTraktRating");
    AutoBinding autoBinding_18 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_7, chckbxTraktSyncRating,
        jCheckBoxBeanProperty);
    autoBinding_18.bind();
    //
    Property movieSettingsBeanProperty_10 = BeanProperty.create("syncTraktWatched");
    AutoBinding autoBinding_19 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_10, chckbxTraktSyncWatched,
        jCheckBoxBeanProperty);
    autoBinding_19.bind();
    //
    Property movieSettingsBeanProperty_18 = BeanProperty.create("syncTraktCollection");
    AutoBinding autoBinding_20 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_18,
        chckbxTraktSyncCollection, jCheckBoxBeanProperty);
    autoBinding_20.bind();
    //
    Property movieSettingsBeanProperty = BeanProperty.create("resetNewFlagOnUds");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, chckbxResetNewFlag,
        jCheckBoxBeanProperty);
    autoBinding.bind();
  }
}
