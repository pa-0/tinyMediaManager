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

import static java.util.Locale.ROOT;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.kodi.KodiMovieMetadataProvider;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.MediaScraperCheckComboBox;
import org.tinymediamanager.ui.components.combobox.MediaScraperComboBox;
import org.tinymediamanager.ui.components.combobox.ScraperMetadataConfigCheckComboBox;
import org.tinymediamanager.ui.dialogs.TmmDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieScrapeMetadataDialog. Rescrape metadata
 * 
 * @author Manuel Laggner
 */
public class MovieScrapeMetadataDialog extends TmmDialog {
  private final JComboBox<MediaLanguages>                                      cbLanguage;
  private final MediaScraperComboBox                                           cbMetadataScraper;
  private final MediaScraperCheckComboBox                                      cbArtworkScraper;
  private final MediaScraperCheckComboBox                                      cbTrailerScraper;
  private final ScraperMetadataConfigCheckComboBox<MovieScraperMetadataConfig> cbScraperConfig;
  private final JHintCheckBox                                                  chckbxDoNotOverwrite;

  private boolean                                                              startScrape = false;

  /**
   * Instantiates a new movie scrape metadata.
   * 
   * @param title
   *          the title
   */
  public MovieScrapeMetadataDialog(String title) {
    super(title, "updateMetadata");

    // metadataprovider
    MediaScraper defaultScraper = MediaScraper.getMediaScraperById(MovieModuleManager.getInstance().getSettings().getMovieScraper(),
        ScraperType.MOVIE);

    // artwork scraper
    List<MediaScraper> selectedArtworkScrapers = new ArrayList<>();
    for (MediaScraper artworkScraper : MovieModuleManager.getInstance().getMovieList().getAvailableArtworkScrapers()) {
      if (MovieModuleManager.getInstance().getSettings().getArtworkScrapers().contains(artworkScraper.getId())) {
        selectedArtworkScrapers.add(artworkScraper);
      }
    }

    // trailer scraper
    List<MediaScraper> selectedTrailerScrapers = new ArrayList<>();
    for (MediaScraper trailerScraper : MovieModuleManager.getInstance().getMovieList().getAvailableTrailerScrapers()) {
      if (MovieModuleManager.getInstance().getSettings().getTrailerScrapers().contains(trailerScraper.getId())) {
        selectedTrailerScrapers.add(trailerScraper);
      }
    }

    {
      JPanel panelCenter = new JPanel();
      getContentPane().add(panelCenter, BorderLayout.CENTER);
      panelCenter.setLayout(new MigLayout("", "[][600lp:800lp,grow]", "[][][][][shrink 0][150lp:n, grow]"));

      JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
      panelCenter.add(lblLanguageT, "cell 0 0,alignx trailing");

      cbLanguage = new JComboBox(MediaLanguages.valuesSorted());
      cbLanguage.setSelectedItem(MovieModuleManager.getInstance().getSettings().getScraperLanguage());
      panelCenter.add(cbLanguage, "cell 1 0,growx");

      JLabel lblMetadataScraperT = new TmmLabel(TmmResourceBundle.getString("scraper.metadata"));
      panelCenter.add(lblMetadataScraperT, "cell 0 1,alignx right");

      cbMetadataScraper = new MediaScraperComboBox(MovieModuleManager.getInstance()
          .getMovieList()
          .getAvailableMediaScrapers()
          .stream()
          .filter(scraper -> !(scraper.getMediaProvider() instanceof KodiMovieMetadataProvider))
          .collect(Collectors.toList()));
      panelCenter.add(cbMetadataScraper, "cell 1 1,growx");
      cbMetadataScraper.setSelectedItem(defaultScraper);

      JLabel lblArtworkScraper = new TmmLabel(TmmResourceBundle.getString("scraper.artwork"));
      panelCenter.add(lblArtworkScraper, "cell 0 2,alignx right");

      cbArtworkScraper = new MediaScraperCheckComboBox(MovieModuleManager.getInstance().getMovieList().getAvailableArtworkScrapers());
      panelCenter.add(cbArtworkScraper, "cell 1 2,growx");

      JLabel lblTrailerScraper = new TmmLabel(TmmResourceBundle.getString("scraper.trailer"));
      panelCenter.add(lblTrailerScraper, "cell 0 3,alignx right");

      cbTrailerScraper = new MediaScraperCheckComboBox(MovieModuleManager.getInstance().getMovieList().getAvailableTrailerScrapers());
      panelCenter.add(cbTrailerScraper, "cell 1 3,growx");

      JSeparator separator = new JSeparator();
      panelCenter.add(separator, "cell 0 4 2 1,growx");

      JPanel panelScraperConfig = new JPanel();
      panelCenter.add(panelScraperConfig, "cell 0 5 2 1,grow");
      panelScraperConfig.setLayout(new MigLayout("", "[300lp:500lp,grow]", "[][][]"));
      {
        JLabel lblScrapeFollowingItems = new TmmLabel(TmmResourceBundle.getString("scraper.metadata.select"));
        panelScraperConfig.add(lblScrapeFollowingItems, "cell 0 0");

        cbScraperConfig = new ScraperMetadataConfigCheckComboBox(MovieScraperMetadataConfig.getValuesWithout(MovieScraperMetadataConfig.ID));
        cbScraperConfig.enableFilter(
            (movieScraperMetadataConfig, s) -> movieScraperMetadataConfig.getDescription().toLowerCase(ROOT).startsWith(s.toLowerCase(ROOT)));
        panelScraperConfig.add(cbScraperConfig, "cell 0 1 ,wmin 0,grow");
      }
      {
        chckbxDoNotOverwrite = new JHintCheckBox(TmmResourceBundle.getString("message.scrape.donotoverwrite"));
        chckbxDoNotOverwrite.setToolTipText(TmmResourceBundle.getString("message.scrape.donotoverwrite.desc"));
        chckbxDoNotOverwrite.setHintIcon(IconManager.HINT);
        panelScraperConfig.add(chckbxDoNotOverwrite, "cell 0 2");
      }
    }
    {
      JButton btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
      btnCancel.setIcon(IconManager.CANCEL_INV);
      btnCancel.addActionListener(e -> {
        startScrape = false;
        setVisible(false);
      });
      addButton(btnCancel);

      JButton btnStart = new JButton(TmmResourceBundle.getString("scraper.start"));
      btnStart.setIcon(IconManager.APPLY_INV);
      btnStart.addActionListener(e -> {
        startScrape = true;
        setVisible(false);
      });
      addDefaultButton(btnStart);
    }
    // set data

    // default scrapers
    if (!selectedArtworkScrapers.isEmpty()) {
      cbArtworkScraper.setSelectedItems(selectedArtworkScrapers);
    }
    if (!selectedTrailerScrapers.isEmpty()) {
      cbTrailerScraper.setSelectedItems(selectedTrailerScrapers);
    }

    // pre-set config
    List<MovieScraperMetadataConfig> configs = MovieModuleManager.getInstance().getSettings().getScraperMetadataConfig();
    // if automatic artwork scrape is not wanted, strip out artwork options
    if (!MovieModuleManager.getInstance().getSettings().isScrapeBestImage()) {
      configs.removeAll(MovieScraperMetadataConfig.valuesForType(ScraperMetadataConfig.Type.ARTWORK));
    }

    cbScraperConfig.setSelectedItems(configs);
    chckbxDoNotOverwrite.setSelected(MovieModuleManager.getInstance().getSettings().isDoNotOverwriteExistingData());
  }

  /**
   * Pass the movie search and scrape config to the caller.
   * 
   * @return the movie search and scrape config
   */
  public MovieSearchAndScrapeOptions getMovieSearchAndScrapeOptions() {
    MovieSearchAndScrapeOptions movieSearchAndScrapeConfig = new MovieSearchAndScrapeOptions();
    movieSearchAndScrapeConfig.setCertificationCountry(MovieModuleManager.getInstance().getSettings().getCertificationCountry());
    movieSearchAndScrapeConfig.setReleaseDateCountry(MovieModuleManager.getInstance().getSettings().getReleaseDateCountry());

    // language
    movieSearchAndScrapeConfig.setLanguage((MediaLanguages) cbLanguage.getSelectedItem());

    // metadata provider
    movieSearchAndScrapeConfig.setMetadataScraper((MediaScraper) cbMetadataScraper.getSelectedItem());

    // artwork scrapers
    movieSearchAndScrapeConfig.setArtworkScraper(cbArtworkScraper.getSelectedItems());

    // tailer scraper
    movieSearchAndScrapeConfig.setTrailerScraper(cbTrailerScraper.getSelectedItems());

    return movieSearchAndScrapeConfig;
  }

  /**
   * Pass the movie meta data config to the caller
   * 
   * @return the movie meta data config
   */
  public List<MovieScraperMetadataConfig> getMovieScraperMetadataConfig() {
    return cbScraperConfig.getSelectedItems();
  }

  /**
   * should the scrape overwrite existing items
   * 
   * @return true/false
   */
  public boolean getOverwriteExistingItems() {
    return !chckbxDoNotOverwrite.isSelected();
  }

  /**
   * Should start scrape.
   * 
   * @return true, if successful
   */
  public boolean shouldStartScrape() {
    return startScrape;
  }
}
