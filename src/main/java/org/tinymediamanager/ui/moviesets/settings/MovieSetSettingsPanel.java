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
package org.tinymediamanager.ui.moviesets.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSetScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.connector.MovieSetConnectors;
import org.tinymediamanager.core.movie.filenaming.MovieSetNfoNaming;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class MovieSetSettingsPanel is used for displaying some movie set related settings
 * 
 * @author Manuel Laggner
 */
public class MovieSetSettingsPanel extends JPanel {
  private static final int                                    COL_COUNT = 7;

  private final MovieSettings                                 settings  = MovieModuleManager.getInstance().getSettings();

  private JCheckBox                                           chckbxShowMissingMovies;
  private JCheckBox                                           chckbxTvShowTableTooltips;
  private JHintCheckBox                                       chckbxDisplayAllMissingMetadata;
  private JHintCheckBox                                       chckbxDisplayAllMissingArtwork;
  private JTextField                                          tfMovieSetArtworkFolder;
  private JButton                                             btnSelectFolder;
  private JComboBox                                           cbNfoFormat;
  private JCheckBox                                           cbMovieNfoFilename1;
  private JCheckBox                                           cbMovieNfoFilename2;
  private JCheckBox                                           cbMovieNfoFilename3;

  private JCheckBox                                           chckbxStoreFilter;

  private final Map<MovieSetScraperMetadataConfig, JCheckBox> metadataCheckBoxes;
  private final Map<MovieSetScraperMetadataConfig, JCheckBox> artworkCheckBoxes;
  private final ItemListener                                  checkBoxListener;

  public MovieSetSettingsPanel() {
    metadataCheckBoxes = new LinkedHashMap<>();
    artworkCheckBoxes = new LinkedHashMap<>();
    checkBoxListener = e -> checkChanges();

    // UI initializations
    initComponents();
    initDataBindings();

    // further initializations
    btnSelectFolder.addActionListener(arg0 -> {
      String path = TmmProperties.getInstance().getProperty("movieset.folderchooser.path");
      Path file = TmmUIHelper.selectDirectory(TmmResourceBundle.getString("Settings.movieset.folderchooser"), path);
      if (file != null && Files.isDirectory(file)) {
        tfMovieSetArtworkFolder.setText(file.toAbsolutePath().toString());
        TmmProperties.getInstance().putProperty("movieset.folderchooser.path", file.toAbsolutePath().toString());
      }
    });

    buildCheckBoxes();
  }

  private void checkChanges() {
    // metadata
    settings.clearMovieSetCheckMetadata();
    for (Map.Entry<MovieSetScraperMetadataConfig, JCheckBox> entry : metadataCheckBoxes.entrySet()) {
      MovieSetScraperMetadataConfig key = entry.getKey();
      JCheckBox value = entry.getValue();
      if (value.isSelected()) {
        settings.addMovieSetCheckMetadata(key);
      }
    }

    // artwork
    settings.clearMovieSetCheckArtwork();
    for (Map.Entry<MovieSetScraperMetadataConfig, JCheckBox> entry : artworkCheckBoxes.entrySet()) {
      MovieSetScraperMetadataConfig key = entry.getKey();
      JCheckBox value = entry.getValue();
      if (value.isSelected()) {
        settings.addMovieSetCheckArtwork(key);
      }
    }

    // NFO
    settings.clearMovieSetNfoFilenames();
    if (cbMovieNfoFilename1.isSelected()) {
      settings.addMovieSetNfoFilename(MovieSetNfoNaming.KODI_NFO);
    }
    if (cbMovieNfoFilename2.isSelected()) {
      settings.addMovieSetNfoFilename(MovieSetNfoNaming.AUTOMATOR_NFO);
    }
    if (cbMovieNfoFilename3.isSelected()) {
      settings.addMovieSetNfoFilename(MovieSetNfoNaming.EMBY_NFO);
    }
  }

  private void buildCheckBoxes() {
    // NFO filenames
    List<MovieSetNfoNaming> movieSetNfoFilenames = settings.getMovieSetNfoFilenames();
    if (movieSetNfoFilenames.contains(MovieSetNfoNaming.KODI_NFO)) {
      cbMovieNfoFilename1.setSelected(true);
    }
    if (movieSetNfoFilenames.contains(MovieSetNfoNaming.AUTOMATOR_NFO)) {
      cbMovieNfoFilename2.setSelected(true);
    }
    if (movieSetNfoFilenames.contains(MovieSetNfoNaming.EMBY_NFO)) {
      cbMovieNfoFilename3.setSelected(true);
    }

    cbMovieNfoFilename1.addItemListener(checkBoxListener);
    cbMovieNfoFilename2.addItemListener(checkBoxListener);
    cbMovieNfoFilename3.addItemListener(checkBoxListener);

    // metadata
    for (MovieSetScraperMetadataConfig value : settings.getMovieSetCheckMetadata()) {
      JCheckBox checkBox = metadataCheckBoxes.get(value);
      if (checkBox != null) {
        checkBox.setSelected(true);
      }
    }

    // set the checkbox listener at the end!
    for (JCheckBox checkBox : metadataCheckBoxes.values()) {
      checkBox.addItemListener(checkBoxListener);
    }

    // artwork
    for (MovieSetScraperMetadataConfig value : settings.getMovieSetCheckArtwork()) {
      JCheckBox checkBox = artworkCheckBoxes.get(value);
      if (checkBox != null) {
        checkBox.setSelected(true);
      }
    }

    // set the checkbox listener at the end!
    for (JCheckBox checkBox : artworkCheckBoxes.values()) {
      checkBox.addItemListener(checkBoxListener);
    }
  }

  private void clearSelection(JCheckBox... checkBoxes) {
    for (JCheckBox checkbox : checkBoxes) {
      checkbox.setSelected(false);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][][15lp!][]"));
    {
      JPanel panelUiSettings = new JPanel();
      panelUiSettings.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][]")); // 16lp ~ width of the

      JLabel lblUiSettings = new TmmLabel(TmmResourceBundle.getString("Settings.ui"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelUiSettings, lblUiSettings, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#ui-settings"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");
      {
        chckbxStoreFilter = new JCheckBox(TmmResourceBundle.getString("Settings.movie.persistuifilter"));
        panelUiSettings.add(chckbxStoreFilter, "cell 1 0 2 1");
      }
      {
        chckbxShowMissingMovies = new JCheckBox(TmmResourceBundle.getString("Settings.movieset.showmissingmovies"));
        panelUiSettings.add(chckbxShowMissingMovies, "cell 1 1 2 1");
      }
      {
        chckbxTvShowTableTooltips = new JCheckBox(TmmResourceBundle.getString("Settings.movieset.showtabletooltips"));
        panelUiSettings.add(chckbxTvShowTableTooltips, "cell 1 2 2 1");
      }
    }
    {
      JPanel panelData = new JPanel();
      panelData.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!,grow][grow]", "[][grow][10lp!][][grow]")); // 16lp ~ width of the

      JLabel lblMiscT = new TmmLabel(TmmResourceBundle.getString("Settings.movieset.data"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelData, lblMiscT, true);

      add(collapsiblePanel, "cell 0 2,growx,wmin 0");
      {
        JLabel lblFoldername = new TmmLabel(TmmResourceBundle.getString("Settings.movieset.datafolder"));
        panelData.add(lblFoldername, "cell 1 0 2 1");

        tfMovieSetArtworkFolder = new JTextField();
        panelData.add(tfMovieSetArtworkFolder, "cell 1 0 2 1");
        tfMovieSetArtworkFolder.setColumns(40);

        btnSelectFolder = new JButton(TmmResourceBundle.getString("Settings.movieset.buttonselect"));
        panelData.add(btnSelectFolder, "cell 1 0 2 1");

        JTextArea taHint = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.movieset.datafolder.hint"));
        panelData.add(taHint, "cell 1 1 2 1,grow, wmin 0");

        JLabel lblNfoFormat = new JLabel(TmmResourceBundle.getString("Settings.nfoFormat"));
        panelData.add(lblNfoFormat, "flowx,cell 1 3 2 1");

        cbNfoFormat = new JComboBox(MovieSetConnectors.values());
        panelData.add(cbNfoFormat, "cell 1 3 2 1");

        JPanel panelNfoFilenames = new JPanel();
        panelData.add(panelNfoFilenames, "cell 1 4 2 1,grow");
        panelNfoFilenames.setLayout(new MigLayout("insets 0", "[][]", "[][][]"));

        JLabel lblNewLabel = new JLabel(TmmResourceBundle.getString("Settings.nofFileNaming"));
        panelNfoFilenames.add(lblNewLabel, "cell 0 0");

        cbMovieNfoFilename1 = new JCheckBox(TmmResourceBundle.getString("Settings.movieset.moviesetname") + "/"
            + TmmResourceBundle.getString("Settings.movieset.moviesetname") + ".nfo");
        panelNfoFilenames.add(cbMovieNfoFilename1, "cell 1 0");

        cbMovieNfoFilename2 = new JCheckBox(TmmResourceBundle.getString("Settings.movieset.moviesetname") + ".nfo");
        panelNfoFilenames.add(cbMovieNfoFilename2, "cell 1 1");

        cbMovieNfoFilename3 = new JCheckBox(TmmResourceBundle.getString("Settings.movieset.moviesetname") + "/collection.nfo");
        panelNfoFilenames.add(cbMovieNfoFilename3, "cell 1 2");
      }
    }
    {
      JPanel panelMisc = new JPanel();
      panelMisc.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][][]")); // 16lp ~ width of the

      JLabel lblMiscT = new TmmLabel(TmmResourceBundle.getString("Settings.misc"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMisc, lblMiscT, true);
      add(collapsiblePanel, "cell 0 4,growx,wmin 0");
      {
        JLabel lblCheckMetadata = new JLabel(TmmResourceBundle.getString("Settings.checkmetadata"));
        panelMisc.add(lblCheckMetadata, "cell 1 0 2 1");

        JPanel panelCheckMetadata = new JPanel();
        panelCheckMetadata.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.ipadx = 10;

        // Metadata
        for (MovieSetScraperMetadataConfig value : MovieSetScraperMetadataConfig.values()) {
          if (value.isMetaData()) {
            addMetadataCheckbox(panelCheckMetadata, value, metadataCheckBoxes, gbc);
          }
        }
        panelMisc.add(panelCheckMetadata, "cell 2 1");

        chckbxDisplayAllMissingMetadata = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkmetadata.displayall"));
        chckbxDisplayAllMissingMetadata.setToolTipText(TmmResourceBundle.getString("Settings.checkmetadata.displayall.desc"));
        chckbxDisplayAllMissingMetadata.setHintIcon(IconManager.HINT);
        panelMisc.add(chckbxDisplayAllMissingMetadata, "cell 2 2");
      }
      {
        JLabel lblCheckImages = new JLabel(TmmResourceBundle.getString("Settings.checkimages"));
        panelMisc.add(lblCheckImages, "cell 1 4 2 1");

        JPanel panelCheckImages = new JPanel();
        panelCheckImages.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.ipadx = 10;

        // Artwork
        for (MovieSetScraperMetadataConfig value : MovieSetScraperMetadataConfig.values()) {
          if (value.isArtwork()) {
            addMetadataCheckbox(panelCheckImages, value, artworkCheckBoxes, gbc);
          }
        }

        panelMisc.add(panelCheckImages, "cell 2 5");

        chckbxDisplayAllMissingArtwork = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkimages.displayall"));
        chckbxDisplayAllMissingArtwork.setToolTipText(TmmResourceBundle.getString("Settings.checkimages.displayall.desc"));
        chckbxDisplayAllMissingArtwork.setHintIcon(IconManager.HINT);
        panelMisc.add(chckbxDisplayAllMissingArtwork, "cell 2 6");
      }
    }
  }

  private void addMetadataCheckbox(JPanel panel, MovieSetScraperMetadataConfig config, Map<MovieSetScraperMetadataConfig, JCheckBox> map,
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
    Property movieSettingsBeanProperty = BeanProperty.create("displayMovieSetMissingMovies");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, chckbxShowMissingMovies,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property movieSettingsBeanProperty_1 = BeanProperty.create("showMovieSetTableTooltips");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_1,
        chckbxTvShowTableTooltips, jCheckBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property movieSettingsBeanProperty_2 = BeanProperty.create("storeMovieSetUiFilters");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_2, chckbxStoreFilter,
        jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property movieSettingsBeanProperty_3 = BeanProperty.create("movieSetDataFolder");
    Property jTextFieldBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_3, tfMovieSetArtworkFolder,
        jTextFieldBeanProperty);
    autoBinding_3.bind();
    //
    Property movieSettingsBeanProperty_4 = BeanProperty.create("movieSetConnector");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_4, cbNfoFormat,
        jComboBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property movieSettingsBeanProperty_5 = BeanProperty.create("movieSetDisplayAllMissingMetadata");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_5,
        chckbxDisplayAllMissingMetadata, jCheckBoxBeanProperty);
    autoBinding_5.bind();
    //
    Property movieSettingsBeanProperty_6 = BeanProperty.create("movieSetDisplayAllMissingArtwork");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_6,
        chckbxDisplayAllMissingArtwork, jCheckBoxBeanProperty);
    autoBinding_6.bind();
  }
}
