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
package org.tinymediamanager.ui.movies.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.MediaScraperCheckComboBox;
import org.tinymediamanager.ui.components.combobox.TmmCheckComboBox;
import org.tinymediamanager.ui.dialogs.TmmDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieDownloadSubtitleDialog. Download subtitles via file hash
 * 
 * @author Manuel Laggner
 */
public class MovieDownloadSubtitleDialog extends TmmDialog {
  private final MediaScraperCheckComboBox        cbSubtitleScraper;
  private final TmmCheckComboBox<MediaLanguages> cbLanguage;
  private final JCheckBox                        chckbxForceBestSubtitle;

  private boolean                                startDownload = false;

  public MovieDownloadSubtitleDialog(String title) {
    super(title, "downloadSubtitle");
    setMinimumSize(new Dimension(getWidth(), getHeight()));
    {

      JPanel panelScraper = new JPanel();
      getContentPane().add(panelScraper, BorderLayout.CENTER);
      panelScraper.setLayout(new MigLayout("", "[16lp!][][300lp:300lp,grow]", "[][][][10lp:n][]"));

      JLabel lblScraper = new TmmLabel(TmmResourceBundle.getString("scraper"));
      panelScraper.add(lblScraper, "cell 0 0 2 1,alignx right");

      cbSubtitleScraper = new MediaScraperCheckComboBox(MovieModuleManager.getInstance().getMovieList().getAvailableSubtitleScrapers());
      panelScraper.add(cbSubtitleScraper, "cell 2 0,growx,wmin 0");

      JLabel lblLanguage = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
      panelScraper.add(lblLanguage, "cell 0 1 2 1,alignx right");

      cbLanguage = new TmmCheckComboBox(MediaLanguages.valuesSorted());
      panelScraper.add(cbLanguage, "cell 2 1,growx,wmin 0");

      JTextArea taHint = new ReadOnlyTextArea(TmmResourceBundle.getString("movie.download.subtitles.hint"));
      panelScraper.add(taHint, "cell 1 2 2 1,wmin 0,grow");

      chckbxForceBestSubtitle = new JCheckBox(TmmResourceBundle.getString("subtitle.download.force"));
      chckbxForceBestSubtitle.setToolTipText(TmmResourceBundle.getString("subtitle.download.force.desc"));
      panelScraper.add(chckbxForceBestSubtitle, "cell 1 4 2 1");
    }

    {
      JButton btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
      btnCancel.setIcon(IconManager.CANCEL_INV);
      btnCancel.addActionListener(e -> {
        startDownload = false;
        setVisible(false);
      });

      addButton(btnCancel);

      JButton btnStart = new JButton(TmmResourceBundle.getString("scraper.start"));
      btnStart.setIcon(IconManager.APPLY_INV);
      btnStart.addActionListener(e -> {
        startDownload = true;
        setVisible(false);
      });
      addDefaultButton(btnStart);
    }
    // set data

    // scraper
    List<MediaScraper> selectedSubtitleScrapers = new ArrayList<>();
    for (MediaScraper subtitleScraper : MovieModuleManager.getInstance().getMovieList().getAvailableSubtitleScrapers()) {
      if (MovieModuleManager.getInstance().getSettings().getSubtitleScrapers().contains(subtitleScraper.getId())) {
        selectedSubtitleScrapers.add(subtitleScraper);
      }
    }
    if (!selectedSubtitleScrapers.isEmpty()) {
      cbSubtitleScraper.setSelectedItems(selectedSubtitleScrapers);
    }

    cbLanguage.setSelectedItems(Collections.singletonList(MovieModuleManager.getInstance().getSettings().getSubtitleScraperLanguage()));

    chckbxForceBestSubtitle.setSelected(MovieModuleManager.getInstance().getSettings().isSubtitleForceBestMatch());
  }

  /**
   * Get the selected scrapers
   * 
   * @return the selected subtitle scrapers
   */
  public List<MediaScraper> getSubtitleScrapers() {
    // scrapers
    return new ArrayList<>(cbSubtitleScraper.getSelectedItems());
  }

  /**
   * Get the selected Languages
   *
   * @return the selected languages
   */
  public List<MediaLanguages> getLanguages() {
    return cbLanguage.getSelectedItems();
  }

  /**
   * Should we force the best match if there is no hash-match?
   *
   * @return true/false
   */
  public boolean isForceBestMatch() {
    return chckbxForceBestSubtitle.isSelected();
  }

  /**
   * Should start download.
   * 
   * @return true, if successful
   */
  public boolean shouldStartDownload() {
    return startDownload;
  }
}
