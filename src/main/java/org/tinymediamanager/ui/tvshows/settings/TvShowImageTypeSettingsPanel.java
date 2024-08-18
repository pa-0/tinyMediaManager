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
package org.tinymediamanager.ui.tvshows.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.filenaming.TvShowBannerNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowCharacterartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowClearartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowClearlogoNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowDiscartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowEpisodeThumbNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowFanartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowKeyartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowPosterNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonBannerNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonFanartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonPosterNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonThumbNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowThumbNaming;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TvShowImageSettingsPanel} is used to display image file name settings.
 *
 * @author Manuel Laggner
 */
class TvShowImageTypeSettingsPanel extends JPanel {
  private final TvShowSettings settings = TvShowModuleManager.getInstance().getSettings();
  private final ItemListener   checkBoxListener;

  private JCheckBox            chckbxEpisodeThumb1;
  private JCheckBox            chckbxEpisodeThumb3;
  private JCheckBox            chckbxEpisodeThumb4;

  private JCheckBox            chckbxPoster1;
  private JCheckBox            chckbxPoster2;
  private JCheckBox            chckbxPoster3;
  private JCheckBox            chckbxFanart1;
  private JCheckBox            chckbxFanart2;
  private JCheckBox            chckbxBanner1;
  private JCheckBox            chckbxClearart1;
  private JCheckBox            chckbxThumb1;
  private JCheckBox            chckbxThumb2;
  private JCheckBox            chckbxLogo1;
  private JCheckBox            chckbxClearlogo1;
  private JCheckBox            chckbxCharacterart1;
  private JCheckBox            chckbxSeasonPoster1;
  private JCheckBox            chckbxSeasonPoster2;
  private JCheckBox            chckbxSeasonPoster3;
  private JCheckBox            chckbxSeasonFanart1;
  private JCheckBox            chckbxSeasonFanart2;
  private JCheckBox            chckbxSeasonBanner1;
  private JCheckBox            chckbxSeasonBanner2;
  private JCheckBox            chckbxSeasonThumb1;
  private JCheckBox            chckbxSeasonThumb2;
  private JCheckBox            chckbxSeasonThumb3;
  private JCheckBox            chckbxSeasonThumb4;
  private JCheckBox            chckbxKeyart1;
  private JCheckBox            chckbxDiscart1;
  private JCheckBox            chckbxDiscart2;

  /**
   * Instantiates a new movie scraper settings panel.
   */
  TvShowImageTypeSettingsPanel() {
    checkBoxListener = e -> checkChanges();

    // UI init
    initComponents();

    // implement checkBoxListener for preset events
    settings.addPropertyChangeListener(evt -> {
      if ("preset".equals(evt.getPropertyName())) {
        buildCheckBoxes();
      }
    });

    buildCheckBoxes();
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[]"));
    {
      JPanel panelFileNaming = new JPanel(new MigLayout("insets 0", "[20lp!][right][][50lp!][right][grow]",
          "[][][][25lp][10lp][][25lp][10lp][25lp][10lp][][25lp][10lp][25lp][][10lp][][25lp][][][][][25lp][10lp][][10lp][25lp!][20lp]"));

      JLabel lblFiletypes = new TmmLabel(TmmResourceBundle.getString("Settings.artwork.naming"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelFileNaming, lblFiletypes, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#artwork-filenames"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        JLabel lblPosterT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.poster"));
        panelFileNaming.add(lblPosterT, "cell 1 0");

        chckbxPoster1 = new JCheckBox("poster." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxPoster1, "cell 2 0");

        JLabel lblThumbT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.thumb"));
        panelFileNaming.add(lblThumbT, "cell 4 0");

        chckbxThumb1 = new JCheckBox("thumb." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxThumb1, "cell 5 0");

        chckbxPoster2 = new JCheckBox("folder." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxPoster2, "cell 2 1");

        chckbxThumb2 = new JCheckBox("landscape." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxThumb2, "cell 5 1");

        chckbxPoster3 = new JCheckBox("<tv_show_folder_name>." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxPoster3, "cell 2 2");

        JLabel lblFanartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.fanart"));
        panelFileNaming.add(lblFanartT, "cell 1 4");

        chckbxFanart1 = new JCheckBox("fanart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxFanart1, "cell 2 4");

        chckbxFanart2 = new JCheckBox("backdrop." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxFanart2, "cell 2 5");

        JLabel lblBannerT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.banner"));
        panelFileNaming.add(lblBannerT, "cell 4 4");

        chckbxBanner1 = new JCheckBox("banner." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxBanner1, "cell 5 4");

        JLabel lblClearartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.clearart"));
        panelFileNaming.add(lblClearartT, "cell 1 7");

        chckbxClearart1 = new JCheckBox("clearart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxClearart1, "cell 2 7");

        JLabel lblLblcharacterartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.characterart"));
        panelFileNaming.add(lblLblcharacterartT, "cell 4 7");

        chckbxCharacterart1 = new JCheckBox("characterart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxCharacterart1, "cell 5 7");

        JLabel lblClearlogoT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.clearlogo"));
        panelFileNaming.add(lblClearlogoT, "cell 1 9");

        chckbxClearlogo1 = new JCheckBox("clearlogo." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxClearlogo1, "cell 2 9");

        JLabel lblDiscartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.disc"));
        panelFileNaming.add(lblDiscartT, "cell 4 9");

        chckbxDiscart1 = new JCheckBox("discart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxDiscart1, "cell 5 9");

        chckbxLogo1 = new JCheckBox("logo." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxLogo1, "cell 2 10");

        chckbxDiscart2 = new JCheckBox("disc." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxDiscart2, "cell 5 10");

        JLabel lblKeyartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.keyart"));
        panelFileNaming.add(lblKeyartT, "cell 1 12");

        chckbxKeyart1 = new JCheckBox("keyart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxKeyart1, "cell 2 12");

        JLabel lblSeasonPosterT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.season_poster"));
        panelFileNaming.add(lblSeasonPosterT, "cell 1 14");

        chckbxSeasonPoster1 = new JCheckBox("seasonXX-poster." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxSeasonPoster1, "cell 2 14");

        JLabel lblSeasonFanartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.season_fanart"));
        panelFileNaming.add(lblSeasonFanartT, "cell 4 14");

        chckbxSeasonFanart1 = new JCheckBox("seasonXX-fanart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxSeasonFanart1, "cell 5 14");

        chckbxSeasonPoster2 = new JCheckBox(
            "<season_folder>" + File.separator + "seasonXX-poster." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxSeasonPoster2, "cell 2 15");

        chckbxSeasonFanart2 = new JCheckBox("<season_folder>/seasonXX-fanart." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxSeasonFanart2, "cell 5 15");

        chckbxSeasonPoster3 = new JCheckBox(
            "<season_folder>" + File.separator + "folder." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxSeasonPoster3, "cell 2 16");

        JLabel lblSeasonThumbT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.season_thumb"));
        panelFileNaming.add(lblSeasonThumbT, "cell 1 18");

        chckbxSeasonThumb1 = new JCheckBox("seasonXX-thumb." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxSeasonThumb1, "cell 2 18");

        JLabel lblSeasonBannerT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.season_banner"));
        panelFileNaming.add(lblSeasonBannerT, "cell 4 18");

        chckbxSeasonBanner1 = new JCheckBox("seasonXX-banner." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxSeasonBanner1, "cell 5 18");

        chckbxSeasonThumb3 = new JCheckBox("seasonXX-landscape." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxSeasonThumb3, "cell 2 19");

        chckbxSeasonBanner2 = new JCheckBox("<season_folder>/seasonXX-banner." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxSeasonBanner2, "cell 5 19");

        chckbxSeasonThumb2 = new JCheckBox("<season_folder>/seasonXX-thumb." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxSeasonThumb2, "cell 2 20");

        chckbxSeasonThumb4 = new JCheckBox("<season_folder>/seasonXX-landscape." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxSeasonThumb4, "cell 2 21");

        JLabel lblThumbNaming = new TmmLabel(TmmResourceBundle.getString("mediafiletype.episode_thumb"));
        panelFileNaming.add(lblThumbNaming, "cell 1 23");

        chckbxEpisodeThumb1 = new JCheckBox("<dynamic>-thumb." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxEpisodeThumb1, "cell 2 23");

        chckbxEpisodeThumb3 = new JCheckBox("<dynamic>." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelFileNaming.add(chckbxEpisodeThumb3, "cell 2 24");

        chckbxEpisodeThumb4 = new JCheckBox("<dynamic>.tbn");
        panelFileNaming.add(chckbxEpisodeThumb4, "cell 2 25");

        JTextArea tpFileNamingHint = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.naming.info"));
        panelFileNaming.add(tpFileNamingHint, "cell 1 27 5 1,growx,wmin 0");
        TmmFontHelper.changeFont(tpFileNamingHint, 0.833);
      }
    }
  }

  private void buildCheckBoxes() {
    chckbxPoster1.removeItemListener(checkBoxListener);
    chckbxPoster2.removeItemListener(checkBoxListener);
    chckbxPoster3.removeItemListener(checkBoxListener);
    clearSelection(chckbxPoster1, chckbxPoster2, chckbxPoster3);

    chckbxFanart1.removeItemListener(checkBoxListener);
    chckbxFanart2.removeItemListener(checkBoxListener);
    clearSelection(chckbxFanart1, chckbxFanart2);

    chckbxBanner1.removeItemListener(checkBoxListener);
    clearSelection(chckbxBanner1);

    chckbxClearart1.removeItemListener(checkBoxListener);
    clearSelection(chckbxClearart1);

    chckbxThumb1.removeItemListener(checkBoxListener);
    chckbxThumb2.removeItemListener(checkBoxListener);
    clearSelection(chckbxThumb1, chckbxThumb2);

    chckbxLogo1.removeItemListener(checkBoxListener);
    clearSelection(chckbxLogo1);

    chckbxClearlogo1.removeItemListener(checkBoxListener);
    clearSelection(chckbxClearlogo1);

    chckbxDiscart1.removeItemListener(checkBoxListener);
    chckbxDiscart2.removeItemListener(checkBoxListener);
    clearSelection(chckbxDiscart1, chckbxDiscart2);

    chckbxCharacterart1.removeItemListener(checkBoxListener);
    clearSelection(chckbxCharacterart1);

    chckbxKeyart1.removeItemListener(checkBoxListener);
    clearSelection(chckbxKeyart1);

    chckbxSeasonPoster1.removeItemListener(checkBoxListener);
    chckbxSeasonPoster2.removeItemListener(checkBoxListener);
    chckbxSeasonPoster3.removeItemListener(checkBoxListener);
    clearSelection(chckbxSeasonPoster1, chckbxSeasonPoster2, chckbxSeasonPoster3);

    chckbxSeasonFanart1.removeItemListener(checkBoxListener);
    chckbxSeasonFanart2.removeItemListener(checkBoxListener);
    clearSelection(chckbxSeasonFanart1, chckbxSeasonFanart2);

    chckbxSeasonBanner1.removeItemListener(checkBoxListener);
    chckbxSeasonBanner2.removeItemListener(checkBoxListener);
    clearSelection(chckbxSeasonBanner1, chckbxSeasonBanner2);

    chckbxSeasonThumb1.removeItemListener(checkBoxListener);
    chckbxSeasonThumb2.removeItemListener(checkBoxListener);
    chckbxSeasonThumb3.removeItemListener(checkBoxListener);
    chckbxSeasonThumb4.removeItemListener(checkBoxListener);
    clearSelection(chckbxSeasonThumb1, chckbxSeasonThumb2, chckbxSeasonThumb3, chckbxSeasonThumb4);

    chckbxEpisodeThumb1.removeItemListener(checkBoxListener);
    chckbxEpisodeThumb3.removeItemListener(checkBoxListener);
    chckbxEpisodeThumb4.removeItemListener(checkBoxListener);
    clearSelection(chckbxEpisodeThumb1, chckbxEpisodeThumb3, chckbxEpisodeThumb4);

    for (TvShowPosterNaming posterNaming : settings.getPosterFilenames()) {
      switch (posterNaming) {
        case POSTER:
          chckbxPoster1.setSelected(true);
          break;

        case FOLDER:
          chckbxPoster2.setSelected(true);
          break;

        case FOLDER_NAME:
          chckbxPoster3.setSelected(true);
          break;
      }
    }

    for (TvShowFanartNaming fanartNaming : settings.getFanartFilenames()) {
      switch (fanartNaming) {
        case FANART:
          chckbxFanart1.setSelected(true);
          break;

        case BACKDROP:
          chckbxFanart2.setSelected(true);
          break;
      }
    }

    for (TvShowBannerNaming bannerNaming : settings.getBannerFilenames()) {
      switch (bannerNaming) {
        case BANNER:
          chckbxBanner1.setSelected(true);
          break;
      }
    }

    for (TvShowClearartNaming clearartNaming : settings.getClearartFilenames()) {
      switch (clearartNaming) {
        case CLEARART:
          chckbxClearart1.setSelected(true);
          break;
      }
    }

    for (TvShowDiscartNaming discartNaming : settings.getDiscartFilenames()) {
      switch (discartNaming) {
        case DISCART:
          chckbxDiscart1.setSelected(true);
          break;

        case DISC:
          chckbxDiscart2.setSelected(true);
          break;
      }
    }

    for (TvShowThumbNaming thumbNaming : settings.getThumbFilenames()) {
      switch (thumbNaming) {
        case THUMB:
          chckbxThumb1.setSelected(true);
          break;

        case LANDSCAPE:
          chckbxThumb2.setSelected(true);
          break;
      }
    }

    for (TvShowClearlogoNaming clearlogoNaming : settings.getClearlogoFilenames()) {
      switch (clearlogoNaming) {
        case CLEARLOGO:
          chckbxClearlogo1.setSelected(true);
          break;

        case LOGO:
          chckbxLogo1.setSelected(true);
          break;
      }
    }

    for (TvShowCharacterartNaming characterartNaming : settings.getCharacterartFilenames()) {
      switch (characterartNaming) {
        case CHARACTERART:
          chckbxCharacterart1.setSelected(true);
          break;
      }
    }

    for (TvShowKeyartNaming keyartNaming : settings.getKeyartFilenames()) {
      switch (keyartNaming) {
        case KEYART:
          chckbxKeyart1.setSelected(true);
          break;
      }
    }

    for (TvShowSeasonPosterNaming seasonPosterNaming : settings.getSeasonPosterFilenames()) {
      switch (seasonPosterNaming) {
        case SEASON_POSTER:
          chckbxSeasonPoster1.setSelected(true);
          break;

        case SEASON_FOLDER:
          chckbxSeasonPoster2.setSelected(true);
          break;

        case FOLDER:
          chckbxSeasonPoster3.setSelected(true);
          break;
      }
    }

    for (TvShowSeasonBannerNaming seasonBannerNaming : settings.getSeasonBannerFilenames()) {
      switch (seasonBannerNaming) {
        case SEASON_BANNER:
          chckbxSeasonBanner1.setSelected(true);
          break;

        case SEASON_FOLDER:
          chckbxSeasonBanner2.setSelected(true);
          break;
      }
    }

    for (TvShowSeasonFanartNaming seasonFanartNaming : settings.getSeasonFanartFilenames()) {
      switch (seasonFanartNaming) {
        case SEASON_FANART:
          chckbxSeasonFanart1.setSelected(true);
          break;

        case SEASON_FOLDER:
          chckbxSeasonFanart2.setSelected(true);
          break;
      }
    }

    for (TvShowSeasonThumbNaming seasonThumbNaming : settings.getSeasonThumbFilenames()) {
      switch (seasonThumbNaming) {
        case SEASON_THUMB:
          chckbxSeasonThumb1.setSelected(true);
          break;

        case SEASON_FOLDER:
          chckbxSeasonThumb2.setSelected(true);
          break;

        case SEASON_LANDSCAPE:
          chckbxSeasonThumb3.setSelected(true);
          break;

        case SEASON_FOLDER_LANDSCAPE:
          chckbxSeasonThumb4.setSelected(true);
          break;
      }
    }

    for (TvShowEpisodeThumbNaming thumbNaming : settings.getEpisodeThumbFilenames()) {
      switch (thumbNaming) {
        case FILENAME_THUMB:
          chckbxEpisodeThumb1.setSelected(true);
          break;

        case FILENAME:
          chckbxEpisodeThumb3.setSelected(true);
          break;

        case FILENAME_TBN:
          chckbxEpisodeThumb4.setSelected(true);
          break;
      }
    }

    chckbxPoster1.addItemListener(checkBoxListener);
    chckbxPoster2.addItemListener(checkBoxListener);
    chckbxPoster3.addItemListener(checkBoxListener);

    chckbxFanart1.addItemListener(checkBoxListener);
    chckbxFanart2.addItemListener(checkBoxListener);

    chckbxBanner1.addItemListener(checkBoxListener);

    chckbxClearart1.addItemListener(checkBoxListener);

    chckbxThumb1.addItemListener(checkBoxListener);
    chckbxThumb2.addItemListener(checkBoxListener);

    chckbxLogo1.addItemListener(checkBoxListener);

    chckbxClearlogo1.addItemListener(checkBoxListener);

    chckbxDiscart1.addItemListener(checkBoxListener);
    chckbxDiscart2.addItemListener(checkBoxListener);

    chckbxCharacterart1.addItemListener(checkBoxListener);

    chckbxSeasonPoster1.addItemListener(checkBoxListener);
    chckbxSeasonPoster2.addItemListener(checkBoxListener);
    chckbxSeasonPoster3.addItemListener(checkBoxListener);

    chckbxSeasonBanner1.addItemListener(checkBoxListener);
    chckbxSeasonBanner2.addItemListener(checkBoxListener);

    chckbxSeasonFanart1.addItemListener(checkBoxListener);
    chckbxSeasonFanart2.addItemListener(checkBoxListener);

    chckbxSeasonThumb1.addItemListener(checkBoxListener);
    chckbxSeasonThumb2.addItemListener(checkBoxListener);
    chckbxSeasonThumb3.addItemListener(checkBoxListener);
    chckbxSeasonThumb4.addItemListener(checkBoxListener);

    chckbxEpisodeThumb1.addItemListener(checkBoxListener);
    chckbxEpisodeThumb3.addItemListener(checkBoxListener);
    chckbxEpisodeThumb4.addItemListener(checkBoxListener);

    chckbxKeyart1.addItemListener(checkBoxListener);
  }

  private void clearSelection(JCheckBox... checkBoxes) {
    for (JCheckBox checkBox : checkBoxes) {
      checkBox.setSelected(false);
    }
  }

  /**
   * Check changes.
   */
  private void checkChanges() {
    settings.clearPosterFilenames();
    if (chckbxPoster1.isSelected()) {
      settings.addPosterFilename(TvShowPosterNaming.POSTER);
    }
    if (chckbxPoster2.isSelected()) {
      settings.addPosterFilename(TvShowPosterNaming.FOLDER);
    }
    if (chckbxPoster3.isSelected()) {
      settings.addPosterFilename(TvShowPosterNaming.FOLDER_NAME);
    }

    settings.clearFanartFilenames();
    if (chckbxFanart1.isSelected()) {
      settings.addFanartFilename(TvShowFanartNaming.FANART);
    }
    if (chckbxFanart2.isSelected()) {
      settings.addFanartFilename(TvShowFanartNaming.BACKDROP);
    }

    settings.clearBannerFilenames();
    if (chckbxBanner1.isSelected()) {
      settings.addBannerFilename(TvShowBannerNaming.BANNER);
    }

    settings.clearClearartFilenames();
    if (chckbxClearart1.isSelected()) {
      settings.addClearartFilename(TvShowClearartNaming.CLEARART);
    }

    settings.clearThumbFilenames();
    if (chckbxThumb1.isSelected()) {
      settings.addThumbFilename(TvShowThumbNaming.THUMB);
    }
    if (chckbxThumb2.isSelected()) {
      settings.addThumbFilename(TvShowThumbNaming.LANDSCAPE);
    }

    settings.clearClearlogoFilenames();
    if (chckbxClearlogo1.isSelected()) {
      settings.addClearlogoFilename(TvShowClearlogoNaming.CLEARLOGO);
    }
    if (chckbxLogo1.isSelected()) {
      settings.addClearlogoFilename(TvShowClearlogoNaming.LOGO);
    }

    settings.clearDiscartFilenames();
    if (chckbxDiscart1.isSelected()) {
      settings.addDiscartFilename(TvShowDiscartNaming.DISCART);
    }
    if (chckbxDiscart2.isSelected()) {
      settings.addDiscartFilename(TvShowDiscartNaming.DISC);
    }

    settings.clearCharacterartFilenames();
    if (chckbxCharacterart1.isSelected()) {
      settings.addCharacterartFilename(TvShowCharacterartNaming.CHARACTERART);
    }

    settings.clearKeyartFilenames();
    if (chckbxKeyart1.isSelected()) {
      settings.addKeyartFilename(TvShowKeyartNaming.KEYART);
    }

    settings.clearSeasonPosterFilenames();
    if (chckbxSeasonPoster1.isSelected()) {
      settings.addSeasonPosterFilename(TvShowSeasonPosterNaming.SEASON_POSTER);
    }
    if (chckbxSeasonPoster2.isSelected()) {
      settings.addSeasonPosterFilename(TvShowSeasonPosterNaming.SEASON_FOLDER);
    }
    if (chckbxSeasonPoster3.isSelected()) {
      settings.addSeasonPosterFilename(TvShowSeasonPosterNaming.FOLDER);
    }

    settings.clearSeasonBannerFilenames();
    if (chckbxSeasonBanner1.isSelected()) {
      settings.addSeasonBannerFilename(TvShowSeasonBannerNaming.SEASON_BANNER);
    }
    if (chckbxSeasonBanner2.isSelected()) {
      settings.addSeasonBannerFilename(TvShowSeasonBannerNaming.SEASON_FOLDER);
    }

    settings.clearSeasonFanartFilenames();
    if (chckbxSeasonFanart1.isSelected()) {
      settings.addSeasonFanartFilename(TvShowSeasonFanartNaming.SEASON_FANART);
    }
    if (chckbxSeasonFanart2.isSelected()) {
      settings.addSeasonFanartFilename(TvShowSeasonFanartNaming.SEASON_FOLDER);
    }

    settings.clearSeasonThumbFilenames();
    if (chckbxSeasonThumb1.isSelected()) {
      settings.addSeasonThumbFilename(TvShowSeasonThumbNaming.SEASON_THUMB);
    }
    if (chckbxSeasonThumb2.isSelected()) {
      settings.addSeasonThumbFilename(TvShowSeasonThumbNaming.SEASON_FOLDER);
    }
    if (chckbxSeasonThumb3.isSelected()) {
      settings.addSeasonThumbFilename(TvShowSeasonThumbNaming.SEASON_LANDSCAPE);
    }
    if (chckbxSeasonThumb4.isSelected()) {
      settings.addSeasonThumbFilename(TvShowSeasonThumbNaming.SEASON_FOLDER_LANDSCAPE);
    }

    settings.clearEpisodeThumbFilenames();
    if (chckbxEpisodeThumb1.isSelected()) {
      settings.addEpisodeThumbFilename(TvShowEpisodeThumbNaming.FILENAME_THUMB);
    }
    if (chckbxEpisodeThumb3.isSelected()) {
      settings.addEpisodeThumbFilename(TvShowEpisodeThumbNaming.FILENAME);
    }
    if (chckbxEpisodeThumb4.isSelected()) {
      settings.addEpisodeThumbFilename(TvShowEpisodeThumbNaming.FILENAME_TBN);
    }
  }
}
