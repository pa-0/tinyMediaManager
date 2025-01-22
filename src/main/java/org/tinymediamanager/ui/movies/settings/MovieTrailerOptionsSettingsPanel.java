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
package org.tinymediamanager.ui.movies.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;
import static org.tinymediamanager.ui.TmmFontHelper.L2;

import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.addon.YtDlpAddon;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.TrailerQuality;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.filenaming.MovieTrailerNaming;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.ScraperInTable;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class {@link MovieTrailerOptionsSettingsPanel}. To maintain advanced trailer settings
 *
 * @author Manuel Laggner
 */
class MovieTrailerOptionsSettingsPanel extends JPanel {
  private final MovieSettings        settings                   = MovieModuleManager.getInstance().getSettings();
  private final List<ScraperInTable> scrapers                   = new ArrayList<>();
  private final ItemListener         checkBoxListener;

  private final ButtonGroup          trailerFilenameButtonGroup = new ButtonGroup();

  private JComboBox<TrailerQuality>  cbTrailerQuality;
  private JCheckBox                  checkBox;
  private JCheckBox                  chckbxAutomaticTrailerDownload;
  private JCheckBox                  cbTrailerFilename1;
  private JCheckBox                  cbTrailerFilename2;
  private JCheckBox                  cbTrailerFilename3;
  private JCheckBox                  cbTrailerFilename4;
  private JLabel                     lblAutomaticTrailerDownloadHint;
  private JCheckBox                  chckbxTrailerDiscKodiStyle;
  private JHintCheckBox              chckbxYtDlp;

  MovieTrailerOptionsSettingsPanel() {
    checkBoxListener = e -> checkChanges();

    // implement checkBoxListener for preset events
    settings.addPropertyChangeListener(evt -> {
      if ("preset".equals(evt.getPropertyName())) {
        buildCheckBoxes();
      }
    });

    // UI init
    initComponents();
    initDataBindings();

    buildCheckBoxes();

    chckbxYtDlp.setEnabled(new YtDlpAddon().isAvailable());
  }

  private void buildCheckBoxes() {
    cbTrailerFilename1.removeItemListener(checkBoxListener);
    cbTrailerFilename2.removeItemListener(checkBoxListener);
    cbTrailerFilename3.removeItemListener(checkBoxListener);
    cbTrailerFilename4.removeItemListener(checkBoxListener);
    clearSelection(cbTrailerFilename1, cbTrailerFilename2, cbTrailerFilename3, cbTrailerFilename4);

    // trailer filenames
    List<MovieTrailerNaming> movieTrailerFilenames = settings.getTrailerFilenames();
    if (movieTrailerFilenames.contains(MovieTrailerNaming.FILENAME_TRAILER)) {
      cbTrailerFilename1.setSelected(true);
    }
    else if (movieTrailerFilenames.contains(MovieTrailerNaming.MOVIE_TRAILER)) {
      cbTrailerFilename2.setSelected(true);
    }
    else if (movieTrailerFilenames.contains(MovieTrailerNaming.TRAILERS_FILENAME_TRAILER)) {
      cbTrailerFilename3.setSelected(true);
    }
    else if (movieTrailerFilenames.contains(MovieTrailerNaming.TRAILER)) {
      cbTrailerFilename4.setSelected(true);
    }

    cbTrailerFilename1.addItemListener(checkBoxListener);
    cbTrailerFilename2.addItemListener(checkBoxListener);
    cbTrailerFilename3.addItemListener(checkBoxListener);
    cbTrailerFilename4.addItemListener(checkBoxListener);
  }

  private void clearSelection(JCheckBox... checkBoxes) {
    for (JCheckBox checkBox : checkBoxes) {
      checkBox.setSelected(false);
    }
  }

  /**
   * check changes of checkboxes
   */
  private void checkChanges() {
    // set trailer filenames
    settings.clearTrailerFilenames();
    if (cbTrailerFilename1.isSelected()) {
      settings.addTrailerFilename(MovieTrailerNaming.FILENAME_TRAILER);
    }
    if (cbTrailerFilename2.isSelected()) {
      settings.addTrailerFilename(MovieTrailerNaming.MOVIE_TRAILER);
    }
    if (cbTrailerFilename3.isSelected()) {
      settings.addTrailerFilename(MovieTrailerNaming.TRAILERS_FILENAME_TRAILER);
    }
    if (cbTrailerFilename4.isSelected()) {
      settings.addTrailerFilename(MovieTrailerNaming.TRAILER);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("hidemode 0", "[600lp,grow]", "[]"));
    {
      JPanel panelOptions = new JPanel();
      panelOptions.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][][][]")); // 16lp ~ width of the

      JLabel lblOptionsT = new TmmLabel(TmmResourceBundle.getString("Settings.advancedoptions"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelOptions, lblOptionsT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#advanced-options-2"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");

      {
        chckbxYtDlp = new JHintCheckBox(TmmResourceBundle.getString("Settings.trailer.ytdlp"));
        chckbxYtDlp.setToolTipText(TmmResourceBundle.getString("Settings.trailer.ytdlp.desc"));
        chckbxYtDlp.setHintIcon(IconManager.HINT);
        panelOptions.add(chckbxYtDlp, "cell 1 0 2 1");

        checkBox = new JCheckBox(TmmResourceBundle.getString("Settings.trailer.preferred"));
        panelOptions.add(checkBox, "cell 1 1 2 1");

        JLabel lblTrailerQuality = new JLabel(TmmResourceBundle.getString("Settings.trailer.quality"));
        panelOptions.add(lblTrailerQuality, "cell 2 3");

        cbTrailerQuality = new JComboBox();
        cbTrailerQuality.setModel(new DefaultComboBoxModel<>(TrailerQuality.values()));
        panelOptions.add(cbTrailerQuality, "cell 2 3");

        chckbxAutomaticTrailerDownload = new JCheckBox(TmmResourceBundle.getString("Settings.trailer.automaticdownload"));
        panelOptions.add(chckbxAutomaticTrailerDownload, "cell 2 4");

        lblAutomaticTrailerDownloadHint = new JLabel(TmmResourceBundle.getString("Settings.trailer.automaticdownload.hint"));
        panelOptions.add(lblAutomaticTrailerDownloadHint, "cell 2 5");
        TmmFontHelper.changeFont(lblAutomaticTrailerDownloadHint, L2);

        JPanel panelTrailerFilenames = new JPanel();
        panelOptions.add(panelTrailerFilenames, "cell 1 6 2 1");
        panelTrailerFilenames.setLayout(new MigLayout("insets 0", "[][]", "[][][]"));

        JLabel lblTrailerFileNaming = new JLabel(TmmResourceBundle.getString("Settings.trailerFileNaming"));
        panelTrailerFilenames.add(lblTrailerFileNaming, "cell 0 0");

        cbTrailerFilename1 = new JCheckBox(
            TmmResourceBundle.getString("Settings.moviefilename") + "-trailer." + TmmResourceBundle.getString("Settings.artwork.extension"));
        trailerFilenameButtonGroup.add(cbTrailerFilename1);
        panelTrailerFilenames.add(cbTrailerFilename1, "cell 1 0");

        cbTrailerFilename2 = new JCheckBox("movie-trailer." + TmmResourceBundle.getString("Settings.artwork.extension"));
        trailerFilenameButtonGroup.add(cbTrailerFilename2);
        panelTrailerFilenames.add(cbTrailerFilename2, "cell 1 1");

        cbTrailerFilename3 = new JCheckBox("trailers/" + TmmResourceBundle.getString("Settings.moviefilename") + "-trailer."
            + TmmResourceBundle.getString("Settings.artwork.extension"));
        trailerFilenameButtonGroup.add(cbTrailerFilename3);
        panelTrailerFilenames.add(cbTrailerFilename3, "cell 1 2");

        cbTrailerFilename4 = new JCheckBox("trailer." + TmmResourceBundle.getString("Settings.artwork.extension"));
        trailerFilenameButtonGroup.add(cbTrailerFilename4);
        panelTrailerFilenames.add(cbTrailerFilename4, "cell 1 3");
      }

      chckbxTrailerDiscKodiStyle = new JCheckBox(TmmResourceBundle.getString("Settings.trailerDiscFolder"));
      panelOptions.add(chckbxTrailerDiscKodiStyle, "cell 2 7");
    }
  }

  protected void initDataBindings() {
    Property movieSettingsBeanProperty_1 = BeanProperty.create("trailerQuality");
    Property jComboBoxBeanProperty_1 = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_1, cbTrailerQuality,
        jComboBoxBeanProperty_1);
    autoBinding_2.bind();
    //
    Property movieSettingsBeanProperty_2 = BeanProperty.create("useTrailerPreference");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_2, checkBox,
        jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    Property movieSettingsBeanProperty_3 = BeanProperty.create("automaticTrailerDownload");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_3,
        chckbxAutomaticTrailerDownload, jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property jCheckBoxBeanProperty_1 = BeanProperty.create("enabled");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ, checkBox, jCheckBoxBeanProperty, chckbxAutomaticTrailerDownload,
        jCheckBoxBeanProperty_1);
    autoBinding_5.bind();
    //
    Property jLabelBeanProperty = BeanProperty.create("enabled");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ, checkBox, jCheckBoxBeanProperty, lblAutomaticTrailerDownloadHint,
        jLabelBeanProperty);
    autoBinding_6.bind();
    //
    Property movieSettingsBeanProperty_4 = BeanProperty.create("trailerDiscFolderInside");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_4,
        chckbxTrailerDiscKodiStyle, jCheckBoxBeanProperty);
    autoBinding_7.bind();
    //
    Property movieSettingsBeanProperty = BeanProperty.create("useYtDlp");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, chckbxYtDlp,
        jCheckBoxBeanProperty);
    autoBinding_1.bind();
  }
}
