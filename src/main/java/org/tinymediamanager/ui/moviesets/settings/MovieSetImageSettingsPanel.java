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

import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.filenaming.MovieSetBannerNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetClearartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetClearlogoNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetDiscartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetFanartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetPosterNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetThumbNaming;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieImageSettingsPanel.
 * 
 * @author Manuel Laggner
 */
class MovieSetImageSettingsPanel extends JPanel {
  private final MovieSettings settings             = MovieModuleManager.getInstance().getSettings();
  private final ItemListener  checkBoxListener;
  private final List<String>  characterReplacement = new ArrayList<>(Arrays.asList(" ", "_"));

  private JCheckBox           chckbxPoster1;
  private JCheckBox           chckbxPoster2;
  private JCheckBox           chckbxPoster3;
  private JCheckBox           chckbxPoster4;
  private JCheckBox           chckbxFanart1;
  private JCheckBox           chckbxFanart2;
  private JCheckBox           chckbxFanart3;
  private JCheckBox           chckbxFanart4;
  private JCheckBox           chckbxBanner1;
  private JCheckBox           chckbxBanner2;
  private JCheckBox           chckbxBanner3;
  private JCheckBox           chckbxBanner4;
  private JCheckBox           chckbxLogo1;
  private JCheckBox           chckbxLogo2;
  private JCheckBox           chckbxLogo3;
  private JCheckBox           chckbxLogo4;
  private JCheckBox           chckbxClearlogo1;
  private JCheckBox           chckbxClearlogo2;
  private JCheckBox           chckbxClearlogo3;
  private JCheckBox           chckbxClearlogo4;
  private JCheckBox           chckbxClearart1;
  private JCheckBox           chckbxClearart2;
  private JCheckBox           chckbxClearart3;
  private JCheckBox           chckbxClearart4;
  private JCheckBox           chckbxThumb1;
  private JCheckBox           chckbxThumb2;
  private JCheckBox           chckbxThumb3;
  private JCheckBox           chckbxThumb4;
  private JCheckBox           chckbxThumb5;
  private JCheckBox           chckbxThumb6;
  private JCheckBox           chckbxThumb7;
  private JCheckBox           chckbxThumb8;
  private JCheckBox           chckbxDiscart1;
  private JCheckBox           chckbxDiscart2;
  private JCheckBox           chckbxDiscart3;
  private JCheckBox           chckbxDiscart4;
  private JCheckBox           chckbxDiscart5;
  private JCheckBox           chckbxDiscart6;
  private JCheckBox           chckbxDiscart7;
  private JCheckBox           chckbxDiscart8;
  private JCheckBox           chckbxAutomaticScrape;
  private JComboBox           cbReplacement;

  /**
   * Instantiates a new movie image settings panel.
   */
  MovieSetImageSettingsPanel() {
    checkBoxListener = e -> checkChanges();

    // UI init
    initComponents();
    initDataBindings();

    buildCheckBoxes();

    // colon replacement
    String replacement = settings.getMovieSetTitleCharacterReplacement();
    int index = characterReplacement.indexOf(replacement);
    if (index >= 0) {
      cbReplacement.setSelectedIndex(index);
    }
  }

  private void buildCheckBoxes() {
    // initialize
    clearSelection(chckbxFanart1, chckbxFanart2, chckbxFanart3, chckbxFanart4);
    clearSelection(chckbxPoster1, chckbxPoster2, chckbxPoster3, chckbxPoster4);
    clearSelection(chckbxBanner1, chckbxBanner2, chckbxBanner3, chckbxBanner4);
    clearSelection(chckbxClearart1, chckbxClearart2, chckbxClearart3, chckbxClearart4);
    clearSelection(chckbxClearlogo1, chckbxClearlogo2, chckbxClearlogo3, chckbxClearlogo4);
    clearSelection(chckbxLogo1, chckbxLogo2, chckbxLogo3, chckbxLogo4);
    clearSelection(chckbxThumb1, chckbxThumb2, chckbxThumb3, chckbxThumb4, chckbxThumb5, chckbxThumb6, chckbxThumb7, chckbxThumb8);
    clearSelection(chckbxDiscart1, chckbxDiscart2, chckbxDiscart3, chckbxDiscart4, chckbxDiscart5, chckbxDiscart6, chckbxDiscart7, chckbxDiscart8);

    // poster filenames
    for (MovieSetPosterNaming poster : settings.getMovieSetPosterFilenames()) {
      switch (poster) {
        case MOVIE_POSTER:
          chckbxPoster1.setSelected(true);
          break;

        case KODI_POSTER:
          chckbxPoster2.setSelected(true);
          break;

        case AUTOMATOR_POSTER:
          chckbxPoster3.setSelected(true);
          break;

        case MOVIESET_POSTER:
          chckbxPoster4.setSelected(true);
          break;
      }
    }

    // fanart filenames
    for (MovieSetFanartNaming fanart : settings.getMovieSetFanartFilenames()) {
      switch (fanart) {
        case MOVIE_FANART:
          chckbxFanart1.setSelected(true);
          break;

        case KODI_FANART:
          chckbxFanart2.setSelected(true);
          break;

        case AUTOMATOR_FANART:
          chckbxFanart3.setSelected(true);
          break;

        case MOVIESET_FANART:
          chckbxFanart4.setSelected(true);
          break;
      }
    }

    // banner filenames
    for (MovieSetBannerNaming banner : settings.getMovieSetBannerFilenames()) {
      switch (banner) {
        case MOVIE_BANNER:
          chckbxBanner1.setSelected(true);
          break;

        case KODI_BANNER:
          chckbxBanner2.setSelected(true);
          break;

        case AUTOMATOR_BANNER:
          chckbxBanner3.setSelected(true);
          break;

        case MOVIESET_BANNER:
          chckbxBanner4.setSelected(true);
          break;
      }
    }

    // clearart filenames
    for (MovieSetClearartNaming clearart : settings.getMovieSetClearartFilenames()) {
      switch (clearart) {
        case MOVIE_CLEARART:
          chckbxClearart1.setSelected(true);
          break;

        case KODI_CLEARART:
          chckbxClearart2.setSelected(true);
          break;

        case AUTOMATOR_CLEARART:
          chckbxClearart3.setSelected(true);
          break;

        case MOVIESET_CLEARART:
          chckbxClearart4.setSelected(true);
          break;
      }
    }

    // thumb filenames
    for (MovieSetThumbNaming thumb : settings.getMovieSetThumbFilenames()) {
      switch (thumb) {
        case MOVIE_THUMB:
          chckbxThumb1.setSelected(true);
          break;

        case KODI_THUMB:
          chckbxThumb2.setSelected(true);
          break;

        case AUTOMATOR_THUMB:
          chckbxThumb3.setSelected(true);
          break;

        case MOVIE_LANDSCAPE:
          chckbxThumb4.setSelected(true);
          break;

        case KODI_LANDSCAPE:
          chckbxThumb5.setSelected(true);
          break;

        case AUTOMATOR_LANDSCAPE:
          chckbxThumb6.setSelected(true);
          break;

        case MOVIESET_THUMB:
          chckbxThumb7.setSelected(true);
          break;

        case MOVIESET_LANDSCAPE:
          chckbxThumb8.setSelected(true);
          break;
      }
    }

    // clearlogo filenames
    for (MovieSetClearlogoNaming clearlogo : settings.getMovieSetClearlogoFilenames()) {
      switch (clearlogo) {
        case MOVIE_CLEARLOGO:
          chckbxClearlogo1.setSelected(true);
          break;

        case KODI_CLEARLOGO:
          chckbxClearlogo2.setSelected(true);
          break;

        case AUTOMATOR_CLEARLOGO:
          chckbxClearlogo3.setSelected(true);
          break;

        case MOVIESET_CLEARLOGO:
          chckbxClearlogo4.setSelected(true);
          break;

        case MOVIE_LOGO:
          chckbxLogo1.setSelected(true);
          break;

        case KODI_LOGO:
          chckbxLogo2.setSelected(true);
          break;

        case AUTOMATOR_LOGO:
          chckbxLogo3.setSelected(true);
          break;

        case MOVIESET_LOGO:
          chckbxLogo4.setSelected(true);
          break;
      }
    }

    // discart filenames
    for (MovieSetDiscartNaming disc : settings.getMovieSetDiscartFilenames()) {
      switch (disc) {
        case MOVIE_DISC:
          chckbxDiscart1.setSelected(true);
          break;

        case KODI_DISC:
          chckbxDiscart2.setSelected(true);
          break;

        case AUTOMATOR_DISC:
          chckbxDiscart3.setSelected(true);
          break;

        case MOVIE_DISCART:
          chckbxDiscart4.setSelected(true);
          break;

        case KODI_DISCART:
          chckbxDiscart5.setSelected(true);
          break;

        case AUTOMATOR_DISCART:
          chckbxDiscart6.setSelected(true);
          break;

        case MOVIESET_DISC:
          chckbxDiscart7.setSelected(true);
          break;

        case MOVIESET_DISCART:
          chckbxDiscart8.setSelected(true);
          break;
      }
    }

    // listen to changes of the checkboxes
    chckbxPoster1.addItemListener(checkBoxListener);
    chckbxPoster2.addItemListener(checkBoxListener);
    chckbxPoster3.addItemListener(checkBoxListener);
    chckbxPoster4.addItemListener(checkBoxListener);

    chckbxFanart1.addItemListener(checkBoxListener);
    chckbxFanart2.addItemListener(checkBoxListener);
    chckbxFanart3.addItemListener(checkBoxListener);
    chckbxFanart4.addItemListener(checkBoxListener);

    chckbxBanner1.addItemListener(checkBoxListener);
    chckbxBanner2.addItemListener(checkBoxListener);
    chckbxBanner3.addItemListener(checkBoxListener);
    chckbxBanner4.addItemListener(checkBoxListener);

    chckbxClearart1.addItemListener(checkBoxListener);
    chckbxClearart2.addItemListener(checkBoxListener);
    chckbxClearart3.addItemListener(checkBoxListener);
    chckbxClearart4.addItemListener(checkBoxListener);

    chckbxClearlogo1.addItemListener(checkBoxListener);
    chckbxClearlogo2.addItemListener(checkBoxListener);
    chckbxClearlogo3.addItemListener(checkBoxListener);
    chckbxClearlogo4.addItemListener(checkBoxListener);

    chckbxLogo1.addItemListener(checkBoxListener);
    chckbxLogo2.addItemListener(checkBoxListener);
    chckbxLogo3.addItemListener(checkBoxListener);
    chckbxLogo4.addItemListener(checkBoxListener);

    chckbxThumb1.addItemListener(checkBoxListener);
    chckbxThumb2.addItemListener(checkBoxListener);
    chckbxThumb3.addItemListener(checkBoxListener);
    chckbxThumb4.addItemListener(checkBoxListener);
    chckbxThumb5.addItemListener(checkBoxListener);
    chckbxThumb6.addItemListener(checkBoxListener);
    chckbxThumb7.addItemListener(checkBoxListener);
    chckbxThumb8.addItemListener(checkBoxListener);

    chckbxDiscart1.addItemListener(checkBoxListener);
    chckbxDiscart2.addItemListener(checkBoxListener);
    chckbxDiscart3.addItemListener(checkBoxListener);
    chckbxDiscart4.addItemListener(checkBoxListener);
    chckbxDiscart5.addItemListener(checkBoxListener);
    chckbxDiscart6.addItemListener(checkBoxListener);
    chckbxDiscart7.addItemListener(checkBoxListener);
    chckbxDiscart8.addItemListener(checkBoxListener);
  }

  private void clearSelection(JCheckBox... checkBoxes) {
    for (JCheckBox checkBox : checkBoxes) {
      checkBox.removeItemListener(checkBoxListener);
      checkBox.setSelected(false);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[900lp,grow]", "[]"));
    {

      JPanel panelMovieSet = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][][20lp!][][20lp!][][20lp!][grow]",
          "[][][10lp!][][10lp!][][10lp!][][20lp!][10lp!][][][10lp!][][][10lp!][][][10lp!][][][10lp!][][][10lp!][][][][][10lp!][][][][][10lp!][][][][][20lp!][]"));

      JLabel lblTitle = new TmmLabel(TmmResourceBundle.getString("Settings.movieset"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMovieSet, lblTitle, true);
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");

      {
        {
          chckbxAutomaticScrape = new JCheckBox(TmmResourceBundle.getString("Settings.default.autoscrape"));
          panelMovieSet.add(chckbxAutomaticScrape, "cell 1 0 8 1");
        }
        {
          JLabel lblReplacementT = new JLabel(TmmResourceBundle.getString("Settings.movieset.replacement"));
          panelMovieSet.add(lblReplacementT, "cell 1 1 8 1");

          cbReplacement = new JComboBox(characterReplacement.toArray());
          panelMovieSet.add(cbReplacement, "cell 1 1 8 1");
        }
        {
          JTextArea readOnlyTextArea = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.movieset.artwork.desc"));
          panelMovieSet.add(readOnlyTextArea, "cell 1 3 8 1,wmin 0,grow");
        }
        {
          JPanel panelFolderSettings = new JPanel();
          panelMovieSet.add(panelFolderSettings, "cell 1 5 8 1,grow");
          panelFolderSettings.setLayout(new MigLayout("insets 0", "[][grow]", "[][][]"));

          {
            JLabel lblMovieFolderT = new TmmLabel(TmmResourceBundle.getString("Settings.movieset.moviefolder") + ":");
            panelFolderSettings.add(lblMovieFolderT, "cell 0 0");

            JLabel lblMovieFolder = new JLabel(TmmResourceBundle.getString("Settings.movieset.moviefolder.example"));
            panelFolderSettings.add(lblMovieFolder, "cell 1 0");
          }

          {
            JLabel lblKodiFolderT = new TmmLabel(TmmResourceBundle.getString("Settings.movieset.foldername.kodi") + ":");
            panelFolderSettings.add(lblKodiFolderT, "cell 0 1");

            JLabel lblKodiFolder = new JLabel(TmmResourceBundle.getString("Settings.movieset.foldername.kodi.example"));
            panelFolderSettings.add(lblKodiFolder, "cell 1 1");
          }

          {
            JLabel lblAutomatorFolderT = new TmmLabel(TmmResourceBundle.getString("Settings.movieset.foldername.automator") + ":");
            panelFolderSettings.add(lblAutomatorFolderT, "cell 0 2");

            JLabel lblAutomatorFolder = new JLabel(TmmResourceBundle.getString("Settings.movieset.foldername.automator.example"));
            panelFolderSettings.add(lblAutomatorFolder, "cell 1 2");
          }
        }
      }

      {
        JLabel lblHintIcon = new JLabel(IconManager.HINT);
        panelMovieSet.add(lblHintIcon, "cell 1 7");

        JTextArea taHint = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.movieset.artwork.hint"));
        panelMovieSet.add(taHint, "cell 2 7 7 1,wmin 0,grow");
      }

      {
        JLabel lblMovieFolderT = new TmmLabel(TmmResourceBundle.getString("Settings.movieset.moviefolder"));
        panelMovieSet.add(lblMovieFolderT, "cell 4 10");

        JLabel lblKodiStyleT = new TmmLabel("Kodi/Artwork Beef style");
        panelMovieSet.add(lblKodiStyleT, "cell 6 10");

        JLabel lblMovieSetAutomatorT = new TmmLabel("Movie Set Artwork Automator style");
        panelMovieSet.add(lblMovieSetAutomatorT, "cell 8 10");

        JLabel lblMovieFolder2T = new JLabel(TmmResourceBundle.getString("Settings.moviefolder"));
        panelMovieSet.add(lblMovieFolder2T, "flowx,cell 4 11");

        JLabel lblArtworkFolder2T = new JLabel(TmmResourceBundle.getString("Settings.movieset.moviesetartworkfolder"));
        panelMovieSet.add(lblArtworkFolder2T, "cell 6 11");

        JLabel lblArtworkFolder3T = new JLabel(TmmResourceBundle.getString("Settings.movieset.moviesetartworkfolder"));
        panelMovieSet.add(lblArtworkFolder3T, "cell 8 11");
      }

      {
        JLabel lblPosterFilenameT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.poster"));
        panelMovieSet.add(lblPosterFilenameT, "cell 1 13 2 1");

        chckbxPoster1 = new JCheckBox("movieset-poster." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxPoster1, "cell 4 13");

        chckbxPoster2 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "/poster." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxPoster2, "cell 6 13");

        chckbxPoster3 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-poster." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxPoster3, "cell 8 13");

        chckbxPoster4 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-poster." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxPoster4, "cell 4 14");
      }

      {
        JLabel lblFanartFilenameT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.fanart"));
        panelMovieSet.add(lblFanartFilenameT, "cell 1 16 2 1");

        chckbxFanart1 = new JCheckBox("movieset-fanart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxFanart1, "cell 4 16");

        chckbxFanart2 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "/fanart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxFanart2, "cell 6 16");

        chckbxFanart3 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-fanart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxFanart3, "cell 8 16");

        chckbxFanart4 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-fanart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxFanart4, "cell 4 17");
      }

      {
        JLabel lblBannerT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.banner"));
        panelMovieSet.add(lblBannerT, "cell 1 19 2 1");

        chckbxBanner1 = new JCheckBox("movieset-banner." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxBanner1, "cell 4 19");

        chckbxBanner2 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "/banner." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxBanner2, "cell 6 19");

        chckbxBanner3 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-banner." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxBanner3, "cell 8 19");

        chckbxBanner4 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-banner." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxBanner4, "cell 4 20");
      }

      {
        JLabel lblClearartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.clearart"));
        panelMovieSet.add(lblClearartT, "cell 1 22 2 1");

        chckbxClearart1 = new JCheckBox("movieset-clearart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxClearart1, "cell 4 22");

        chckbxClearart2 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "/clearart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxClearart2, "cell 6 22");

        chckbxClearart3 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-clearart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxClearart3, "cell 8 22");

        chckbxClearart4 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-clearart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxClearart4, "cell 4 23");
      }

      {
        JLabel lblThumbT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.thumb"));
        panelMovieSet.add(lblThumbT, "cell 1 25 2 1");

        chckbxThumb1 = new JCheckBox("movieset-thumb." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxThumb1, "cell 4 25");

        chckbxThumb2 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "/thumb." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxThumb2, "cell 6 25");

        chckbxThumb3 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-thumb." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxThumb3, "cell 8 25");

        chckbxThumb4 = new JCheckBox("movieset-landscape." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxThumb4, "cell 4 26");

        chckbxThumb5 = new JCheckBox(TmmResourceBundle.getString("Settings.movieset.moviesetname") + "/landscape."
            + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxThumb5, "cell 6 26");

        chckbxThumb6 = new JCheckBox(TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-landscape."
            + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxThumb6, "cell 8 26");

        chckbxThumb7 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-thumb." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxThumb7, "cell 4 27");

        chckbxThumb8 = new JCheckBox(TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-landscape."
            + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxThumb8, "cell 4 28");
      }

      {
        chckbxLogo1 = new JCheckBox("movieset-logo." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxLogo1, "cell 4 32");

        chckbxLogo2 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "/logo." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxLogo2, "cell 6 32");

        chckbxLogo3 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-logo." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxLogo3, "cell 8 32");

        chckbxLogo4 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-logo." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxLogo4, "cell 4 33");
      }

      {
        JLabel lblClearlogoT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.clearlogo"));
        panelMovieSet.add(lblClearlogoT, "cell 1 30 2 1");

        chckbxClearlogo1 = new JCheckBox("movieset-clearlogo." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxClearlogo1, "cell 4 30");

        chckbxClearlogo2 = new JCheckBox(TmmResourceBundle.getString("Settings.movieset.moviesetname") + "/clearlogo."
            + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxClearlogo2, "cell 6 30");

        chckbxClearlogo3 = new JCheckBox(TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-clearlogo."
            + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxClearlogo3, "cell 8 30");

        chckbxClearlogo4 = new JCheckBox(TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-clearlogo."
            + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxClearlogo4, "cell 4 31,aligny center");
      }

      {
        JLabel lblDiscartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.disc"));
        panelMovieSet.add(lblDiscartT, "cell 1 35 2 1");

        chckbxDiscart1 = new JCheckBox("movieset-disc." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxDiscart1, "cell 4 35");

        chckbxDiscart2 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "/disc." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxDiscart2, "cell 6 35");

        chckbxDiscart3 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-disc." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxDiscart3, "cell 8 35");

        chckbxDiscart4 = new JCheckBox("movieset-discart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxDiscart4, "cell 4 36");

        chckbxDiscart5 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "/discart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxDiscart5, "cell 6 36");

        chckbxDiscart6 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-discart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxDiscart6, "cell 8 36");

        chckbxDiscart7 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-disc." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxDiscart7, "cell 4 37");

        chckbxDiscart8 = new JCheckBox(
            TmmResourceBundle.getString("Settings.movieset.moviesetname") + "-discart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelMovieSet.add(chckbxDiscart8, "cell 4 38");
      }

      {
        ReadOnlyTextArea tpFileNamingHint = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.naming.info"));
        panelMovieSet.add(tpFileNamingHint, "cell 2 40 7 1,growx,wmin 0");
        TmmFontHelper.changeFont(tpFileNamingHint, 0.833);
      }
    }
  }

  /**
   * Check changes.
   */
  private void checkChanges() {
    // set poster filenames
    settings.clearMovieSetPosterFilenames();

    if (chckbxPoster1.isSelected()) {
      settings.addMovieSetPosterFilename(MovieSetPosterNaming.MOVIE_POSTER);
    }
    if (chckbxPoster2.isSelected()) {
      settings.addMovieSetPosterFilename(MovieSetPosterNaming.KODI_POSTER);
    }
    if (chckbxPoster3.isSelected()) {
      settings.addMovieSetPosterFilename(MovieSetPosterNaming.AUTOMATOR_POSTER);
    }
    if (chckbxPoster4.isSelected()) {
      settings.addMovieSetPosterFilename(MovieSetPosterNaming.MOVIESET_POSTER);
    }

    // set fanart filenames
    settings.clearMovieSetFanartFilenames();
    if (chckbxFanart1.isSelected()) {
      settings.addMovieSetFanartFilename(MovieSetFanartNaming.MOVIE_FANART);
    }
    if (chckbxFanart2.isSelected()) {
      settings.addMovieSetFanartFilename(MovieSetFanartNaming.KODI_FANART);
    }
    if (chckbxFanart3.isSelected()) {
      settings.addMovieSetFanartFilename(MovieSetFanartNaming.AUTOMATOR_FANART);
    }
    if (chckbxFanart4.isSelected()) {
      settings.addMovieSetFanartFilename(MovieSetFanartNaming.MOVIESET_FANART);
    }

    // set banner filenames
    settings.clearMovieSetBannerFilenames();
    if (chckbxBanner1.isSelected()) {
      settings.addMovieSetBannerFilename(MovieSetBannerNaming.MOVIE_BANNER);
    }
    if (chckbxBanner2.isSelected()) {
      settings.addMovieSetBannerFilename(MovieSetBannerNaming.KODI_BANNER);
    }
    if (chckbxBanner3.isSelected()) {
      settings.addMovieSetBannerFilename(MovieSetBannerNaming.AUTOMATOR_BANNER);
    }
    if (chckbxBanner4.isSelected()) {
      settings.addMovieSetBannerFilename(MovieSetBannerNaming.MOVIESET_BANNER);
    }

    // set clearart filenames
    settings.clearMovieSetClearartFilenames();
    if (chckbxClearart1.isSelected()) {
      settings.addMovieSetClearartFilename(MovieSetClearartNaming.MOVIE_CLEARART);
    }
    if (chckbxClearart2.isSelected()) {
      settings.addMovieSetClearartFilename(MovieSetClearartNaming.KODI_CLEARART);
    }
    if (chckbxClearart3.isSelected()) {
      settings.addMovieSetClearartFilename(MovieSetClearartNaming.AUTOMATOR_CLEARART);
    }
    if (chckbxClearart4.isSelected()) {
      settings.addMovieSetClearartFilename(MovieSetClearartNaming.MOVIESET_CLEARART);
    }

    // set thumb filenames
    settings.clearMovieSetThumbFilenames();
    if (chckbxThumb1.isSelected()) {
      settings.addMovieSetThumbFilename(MovieSetThumbNaming.MOVIE_THUMB);
    }
    if (chckbxThumb2.isSelected()) {
      settings.addMovieSetThumbFilename(MovieSetThumbNaming.KODI_THUMB);
    }
    if (chckbxThumb3.isSelected()) {
      settings.addMovieSetThumbFilename(MovieSetThumbNaming.AUTOMATOR_THUMB);
    }
    if (chckbxThumb4.isSelected()) {
      settings.addMovieSetThumbFilename(MovieSetThumbNaming.MOVIE_LANDSCAPE);
    }
    if (chckbxThumb5.isSelected()) {
      settings.addMovieSetThumbFilename(MovieSetThumbNaming.KODI_LANDSCAPE);
    }
    if (chckbxThumb6.isSelected()) {
      settings.addMovieSetThumbFilename(MovieSetThumbNaming.AUTOMATOR_LANDSCAPE);
    }
    if (chckbxThumb7.isSelected()) {
      settings.addMovieSetThumbFilename(MovieSetThumbNaming.MOVIESET_THUMB);
    }
    if (chckbxThumb8.isSelected()) {
      settings.addMovieSetThumbFilename(MovieSetThumbNaming.MOVIESET_LANDSCAPE);
    }

    // set clearlogo filenames
    settings.clearMovieSetClearlogoFilenames();
    if (chckbxClearlogo1.isSelected()) {
      settings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.MOVIE_CLEARLOGO);
    }
    if (chckbxClearlogo2.isSelected()) {
      settings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.KODI_CLEARLOGO);
    }
    if (chckbxClearlogo3.isSelected()) {
      settings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.AUTOMATOR_CLEARLOGO);
    }
    if (chckbxClearlogo4.isSelected()) {
      settings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.MOVIESET_CLEARLOGO);
    }
    if (chckbxLogo1.isSelected()) {
      settings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.MOVIE_LOGO);
    }
    if (chckbxLogo2.isSelected()) {
      settings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.KODI_LOGO);
    }
    if (chckbxLogo3.isSelected()) {
      settings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.AUTOMATOR_LOGO);
    }
    if (chckbxLogo4.isSelected()) {
      settings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.MOVIESET_LOGO);
    }

    // set discart filenames
    settings.clearMovieSetDiscartFilenames();
    if (chckbxDiscart1.isSelected()) {
      settings.addMovieSetDiscartFilename(MovieSetDiscartNaming.MOVIE_DISC);
    }
    if (chckbxDiscart2.isSelected()) {
      settings.addMovieSetDiscartFilename(MovieSetDiscartNaming.KODI_DISC);
    }
    if (chckbxDiscart3.isSelected()) {
      settings.addMovieSetDiscartFilename(MovieSetDiscartNaming.AUTOMATOR_DISC);
    }
    if (chckbxDiscart4.isSelected()) {
      settings.addMovieSetDiscartFilename(MovieSetDiscartNaming.MOVIE_DISCART);
    }
    if (chckbxDiscart5.isSelected()) {
      settings.addMovieSetDiscartFilename(MovieSetDiscartNaming.KODI_DISCART);
    }
    if (chckbxDiscart6.isSelected()) {
      settings.addMovieSetDiscartFilename(MovieSetDiscartNaming.AUTOMATOR_DISCART);
    }
    if (chckbxDiscart7.isSelected()) {
      settings.addMovieSetDiscartFilename(MovieSetDiscartNaming.MOVIESET_DISC);
    }
    if (chckbxDiscart8.isSelected()) {
      settings.addMovieSetDiscartFilename(MovieSetDiscartNaming.MOVIESET_DISCART);
    }
  }

  protected void initDataBindings() {
    Property movieSettingsBeanProperty = BeanProperty.create("scrapeBestImageMovieSet");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, chckbxAutomaticScrape,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property movieSettingsBeanProperty_1 = BeanProperty.create("movieSetTitleCharacterReplacement");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_1, cbReplacement,
        jComboBoxBeanProperty);
    autoBinding_1.bind();
  }
}
