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
package org.tinymediamanager.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ExportTemplate;
import org.tinymediamanager.core.MediaEntityExporter;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowComparator;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowExporter;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.core.tvshow.tasks.TvShowARDetectorTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowEpisodeScrapeTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowReloadMediaInformationTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowRenameTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowScrapeTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowSubtitleSearchAndDownloadTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowUpdateDatasourceTask;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.util.ListUtils;

import picocli.CommandLine;

// @formatter:off
@CommandLine.Command(
        name = "tvshow",
        mixinStandardHelpOptions = true,
        sortOptions = false,
        synopsisHeading = "%n",
        footerHeading = "%nExamples:%n",
        footer = {
                "  tinyMediaManager tvshow -u -n -r",
                "    to find/scrape and rename new TV shows/episodes%n",
                "  tinyMediaManager tvshow -t -s",
                "    to download missing trailer/subtitles%n",
                "  tinyMediaManager tvshow -e -eT=TvShowDetailExampleXml -eP=/user/export/tv",
                "    to export the TV show list with the TvShowDetailExampleXml template to /user/export/tv"
        }
)
// @formatter:on
/**
 * the class {@link TvShowCommand} implements all TV show related logic for the command line interface
 *
 * @author Wolfgang Janes, Manuel Laggner
 */
class TvShowCommand implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowCommand.class);

  @CommandLine.ArgGroup
  Datasource                  datasource;

  @CommandLine.ArgGroup
  Scrape                      scrape;

  @CommandLine.Option(names = { "-t", "--downloadTrailer" }, description = "Download missing trailers")
  boolean                     downloadTrailer;

  @CommandLine.ArgGroup(exclusive = false)
  Subtitle                    subtitle;

  @CommandLine.ArgGroup
  Rename                      rename;

  @CommandLine.ArgGroup(exclusive = false)
  Export                      export;

  @CommandLine.Option(names = { "-w", "--rewriteNFO" }, description = "Rewrite NFO files of all TV shows/episodes")
  boolean                     rewriteNFO;

  @CommandLine.ArgGroup
  AspectRatioDetect           ard;

  @CommandLine.ArgGroup
  MovieCommand.MediaInfo      mediaInfo;

  @Override
  public void run() {
    // update data sources
    if (datasource != null) {
      updateDataSources();
    }

    List<TvShow> showsToScrape = new ArrayList<>();
    List<TvShowEpisode> episodesToScrape = new ArrayList<>();

    // scrape tv shows/episodes
    if (scrape != null) {
      scrapeTvShows(showsToScrape, episodesToScrape);
    }

    if (mediaInfo != null) {
      gatherMediaInfo();
    }

    if (ard != null) {
      detectAspectRatio();
    }

    // download trailer
    if (downloadTrailer) {
      downloadTrailer();
    }

    // download subtitles
    if (subtitle != null && subtitle.download) {
      downloadSubtitles();
    }

    // rename
    if (rename != null) {
      renameTvShows(showsToScrape, episodesToScrape);
    }

    // export
    if (export != null) {
      exportTvShows();
    }

    // rewrite NFO
    if (rewriteNFO) {
      rewriteNfoFiles();
    }
  }

  private void updateDataSources() {
    LOGGER.info("updating TV show data sources...");
    if (datasource.updateAll) {
      Runnable task = new TvShowUpdateDatasourceTask();
      task.run(); // blocking
    }
    else {
      List<String> dataSources = new ArrayList<>(TvShowModuleManager.getInstance().getSettings().getTvShowDataSource());
      if (ListUtils.isNotEmpty(dataSources)) {
        for (Integer i : datasource.indices) {
          if (dataSources.size() >= i - 1) {
            Runnable task = new TvShowUpdateDatasourceTask(dataSources.get(i - 1));
            task.run(); // blocking
          }
        }
      }
    }
    LOGGER.info("Found {} new TV shows / {} new episodes", TvShowModuleManager.getInstance().getTvShowList().getNewTvShows().size(),
        TvShowModuleManager.getInstance().getTvShowList().getNewEpisodes().size());
  }

  private void scrapeTvShows(List<TvShow> showsToScrape, List<TvShowEpisode> episodesToScrape) {
    Set<TvShow> scrapeShow = new HashSet<>(); // no dupes
    Set<TvShowEpisode> scrapeEpisode = new HashSet<>(); // no dupes

    if (scrape.scrapeAll) {
      LOGGER.info("scraping ALL tv shows/episodes...");
      scrapeShow.addAll(TvShowModuleManager.getInstance().getTvShowList().getTvShows());
    }
    else if (scrape.scrapeNew) {
      LOGGER.info("scraping NEW tv shows/episodes...");
      scrapeShow.addAll(TvShowModuleManager.getInstance().getTvShowList().getNewTvShows());
      scrapeEpisode.addAll(TvShowModuleManager.getInstance().getTvShowList().getNewEpisodes());
    }
    else if (scrape.scrapeUnscraped) {
      LOGGER.info("scraping UNSCRAPED tv shows/episodes...");
      scrapeShow.addAll(TvShowModuleManager.getInstance().getTvShowList().getUnscrapedTvShows());
      scrapeEpisode.addAll(TvShowModuleManager.getInstance().getTvShowList().getUnscrapedEpisodes());
    }

    // if we scrape already the whole show, no need to scrape dedicated episodes for it
    Set<TvShowEpisode> removedEpisode = new HashSet<>(); // no dupes
    for (TvShowEpisode ep : scrapeEpisode) {
      if (scrapeShow.contains(ep.getTvShow())) {
        removedEpisode.add(ep);
      }
    }
    scrapeEpisode.removeAll(removedEpisode);
    showsToScrape.addAll(scrapeShow);
    episodesToScrape.addAll(scrapeEpisode);

    if (!showsToScrape.isEmpty()) {
      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.loadDefaults();

      List<TvShowScraperMetadataConfig> tvShowScraperMetadataConfig = TvShowModuleManager.getInstance()
          .getSettings()
          .getTvShowScraperMetadataConfig();
      List<TvShowEpisodeScraperMetadataConfig> episodeScraperMetadataConfig = TvShowModuleManager.getInstance()
          .getSettings()
          .getEpisodeScraperMetadataConfig();

      TvShowScrapeTask.TvShowScrapeParams tvShowScrapeParams = new TvShowScrapeTask.TvShowScrapeParams(showsToScrape, options,
          tvShowScraperMetadataConfig, episodeScraperMetadataConfig);
      tvShowScrapeParams.setOverwriteExistingItems(!TvShowModuleManager.getInstance().getSettings().isDoNotOverwriteExistingData());

      Runnable task = new TvShowScrapeTask(tvShowScrapeParams);
      task.run(); // blocking

      // wait for other tmm threads (artwork download et all)
      while (TmmTaskManager.getInstance().isPoolRunning()) {
        try {
          Thread.sleep(2000);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    if (!episodesToScrape.isEmpty()) {
      // re-group the episodes. If there is a "last used" scraper set for the show also take this into account for the episode
      Map<TvShow, List<TvShowEpisode>> newEpisodes = new HashMap<>();
      for (TvShowEpisode episode : episodesToScrape) {
        List<TvShowEpisode> episodes = newEpisodes.computeIfAbsent(episode.getTvShow(), k -> new ArrayList<>());
        episodes.add(episode);
      }

      // scrape new episodes
      for (Map.Entry<TvShow, List<TvShowEpisode>> entry : newEpisodes.entrySet()) {
        TvShow tvShow = entry.getKey();

        TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
        options.loadDefaults();

        // so for the known ones, we can directly start scraping
        if (StringUtils.isNoneBlank(tvShow.getLastScraperId(), tvShow.getLastScrapeLanguage())) {
          options.setMetadataScraper(MediaScraper.getMediaScraperById(tvShow.getLastScraperId(), ScraperType.TV_SHOW));
          options.setLanguage(MediaLanguages.valueOf(tvShow.getLastScrapeLanguage()));
        }
        else {
          LOGGER.warn("Could not scrape new episodes of show '{}' - show has not been scraped yet!", tvShow.getTitle());
          continue;
        }

        List<TvShowEpisodeScraperMetadataConfig> episodeScraperMetadataConfig = TvShowModuleManager.getInstance()
            .getSettings()
            .getEpisodeScraperMetadataConfig();

        Runnable task = new TvShowEpisodeScrapeTask(entry.getValue(), options, episodeScraperMetadataConfig,
            !TvShowModuleManager.getInstance().getSettings().isDoNotOverwriteExistingData());
        task.run(); // blocking

        // wait for other tmm threads (artwork download et all)
        while (TmmTaskManager.getInstance().isPoolRunning()) {
          try {
            Thread.sleep(2000);
          }
          catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }
  }

  private void detectAspectRatio() {
    List<TvShowEpisode> episodesToDetect = new ArrayList<>();
    if (ard.ardAll) {
      episodesToDetect.addAll(TvShowModuleManager.getInstance().getTvShowList().getEpisodes());
    }
    else if (ard.ardNew) {
      episodesToDetect.addAll(TvShowModuleManager.getInstance().getTvShowList().getNewEpisodes());
    }

    if (!episodesToDetect.isEmpty()) {
      TmmTask task = new TvShowARDetectorTask(episodesToDetect);
      task.run();
    }
  }

  private void gatherMediaInfo() {
    List<TvShow> tvShows = TvShowModuleManager.getInstance().getTvShowList().getTvShows();

    if (!tvShows.isEmpty()) {
      TmmTask task = new TvShowReloadMediaInformationTask(tvShows, Collections.emptyList());
      task.run();
    }
  }

  private void downloadTrailer() {
    LOGGER.info("downloading missing trailers...");
    List<TvShow> tvShowsWithoutTrailer = TvShowModuleManager.getInstance()
        .getTvShowList()
        .getTvShows()
        .stream()
        .filter(tvShow -> tvShow.getMediaFiles(MediaFileType.TRAILER).isEmpty())
        .collect(Collectors.toList());

    for (TvShow tvShow : tvShowsWithoutTrailer) {
      TvShowHelpers.downloadBestTrailer(tvShow);
    }

    // wait for other download threads
    while (TmmTaskManager.getInstance().isPoolRunning()) {
      try {
        Thread.sleep(2000);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void downloadSubtitles() {
    LOGGER.info("downloading missing subtitles...");

    List<MediaLanguages> languages = new ArrayList<>();

    if (subtitle.languages != null) {
      // download in specific languages
      for (String language : subtitle.languages) {
        MediaLanguages mediaLanguage = MediaLanguages.get(language);
        if (!languages.contains(mediaLanguage)) {
          languages.add(mediaLanguage);
        }
      }
    }
    else {
      // download in "main" language
      languages.add(TvShowModuleManager.getInstance().getSettings().getSubtitleScraperLanguage());
    }

    List<TvShowEpisode> episodesWithoutSubtitles = new ArrayList<>();

    TvShowModuleManager.getInstance().getTvShowList().getTvShows().forEach(tvShow -> tvShow.getEpisodes().forEach(episode -> {
      if (!episode.getHasSubtitles()) {
        episodesWithoutSubtitles.add(episode);
      }
    }));

    for (MediaLanguages language : languages) {
      Runnable task = new TvShowSubtitleSearchAndDownloadTask(episodesWithoutSubtitles, language);
      task.run(); // blocking
    }
  }

  private void renameTvShows(List<TvShow> showsToScrape, List<TvShowEpisode> episodesToScrape) {
    List<TvShow> tvShowsToRename = new ArrayList<>();
    List<TvShowEpisode> episodesToRename = new ArrayList<>();

    if (rename.renameNew) {
      LOGGER.info("renaming NEW/RECENTLY SCRAPED tv shows...");
      tvShowsToRename.addAll(showsToScrape);
      episodesToRename.addAll(episodesToScrape);
    }
    else if (rename.renameAll) {
      LOGGER.info("renaming ALL tv shows...");
      tvShowsToRename.addAll(TvShowModuleManager.getInstance().getTvShowList().getTvShows());
      episodesToRename.addAll(TvShowModuleManager.getInstance().getTvShowList().getEpisodes());
    }

    if (!tvShowsToRename.isEmpty() || !episodesToRename.isEmpty()) {
      // rename tvShows
      Runnable task = new TvShowRenameTask(tvShowsToRename, episodesToRename);
      task.run(); // blocking
    }
  }

  private void exportTvShows() {
    for (ExportTemplate exportTemplate : MediaEntityExporter.findTemplates(MediaEntityExporter.TemplateType.TV_SHOW)) {
      if (exportTemplate.getPath().endsWith(export.template)) {
        // ok, our template has been found under templates
        LOGGER.info("exporting tv shows...");

        TvShowExporter ex = null;
        try {
          ex = new TvShowExporter(Paths.get(exportTemplate.getPath()));

          List<TvShow> tvShows = TvShowModuleManager.getInstance().getTvShowList().getTvShows();
          tvShows.sort(new TvShowComparator());
          ex.export(tvShows, export.path);
        }
        catch (Exception e) {
          LOGGER.error("could not export tv shows - {}", e.getMessage());
        }

        return;
      }
    }
  }

  private void rewriteNfoFiles() {
    if (TvShowModuleManager.getInstance().getSettings().getNfoFilenames().isEmpty()) {
      LOGGER.info("Not writing any NFO file, because NFO filename preferences were empty...");
      return;
    }

    for (TvShow tvShow : TvShowModuleManager.getInstance().getTvShowList().getTvShows()) {
      tvShow.writeNFO();
      for (TvShowSeason season : tvShow.getSeasons()) {
        season.writeNfo();
      }
      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        episode.writeNFO();
      }
    }
  }

  /*
   * helper classes for managing nested options
   */
  static class Datasource {
    @CommandLine.Option(names = { "-u", "--updateAll" }, required = true, description = "Scan all data sources for new content")
    boolean updateAll;

    @CommandLine.Option(names = {
        "--updateX" }, required = true, paramLabel = "<index>", description = "Scan the given data sources for new content. The indices are in the same order as in the UI/settings")
    int[]   indices;
  }

  static class Scrape {
    @CommandLine.Option(names = { "-n", "--scrapeNew", }, description = "Scrape new TV shows/episodes (found with the update options)")
    boolean scrapeNew;

    @CommandLine.Option(names = { "--scrapeUnscraped", }, description = "Scrape all unscraped TV shows/episodes")
    boolean scrapeUnscraped;

    @CommandLine.Option(names = { "--scrapeAll", }, description = "Scrape all TV shows/episodes")
    boolean scrapeAll;
  }

  static class AspectRatioDetect {
    @CommandLine.Option(names = { "-d", "--ardNew", }, description = "Detect aspect ratio of new TV shows/episodes (found with the update options)")
    boolean ardNew;

    @CommandLine.Option(names = { "-dA", "--ardAll", }, description = "Detect aspect ratio of all TV shows/episodes")
    boolean ardAll;
  }

  static class Subtitle {
    @CommandLine.Option(names = { "-s", "--downloadSubtitles" }, required = true, description = "Download missing subtitles")
    boolean  download;

    @CommandLine.Option(names = { "-sL", "--subtitleLanguage" }, paramLabel = "<language>", description = "Desired subtitle language(s) (optional)")
    String[] languages;
  }

  static class Rename {
    @CommandLine.Option(names = { "-r",
        "--renameNew", }, required = true, description = "Rename & cleanup all TV shows/episodes from former scrape command")
    boolean renameNew;

    @CommandLine.Option(names = { "--renameAll", }, required = true, description = "Rename & cleanup all TV shows/episodes")
    boolean renameAll;
  }

  static class Export {
    @CommandLine.Option(names = { "-e", "--export" }, required = true, description = "Export your TV show list using a specified template")
    boolean export;

    @CommandLine.Option(names = { "-eT",
        "--exportTemplate" }, required = true, description = "The export template to use. Use the folder name from the templates folder")
    String  template;

    @CommandLine.Option(names = { "-eP", "--exportPath" }, required = true, description = "The path to export your TV show list to")
    Path    path;
  }

  static class MediaInfo {
    @CommandLine.Option(names = { "-mi", "--mediainfo" }, description = "Update mediainfo")
    boolean medainfo;

    @CommandLine.Option(names = { "-mix", "--mediainfoXml" }, description = "Update medianifo - ignore XML")
    boolean mediainfoXml;
  }
}
