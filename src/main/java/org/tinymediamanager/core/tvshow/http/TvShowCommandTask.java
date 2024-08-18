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
package org.tinymediamanager.core.tvshow.http;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ExportTemplate;
import org.tinymediamanager.core.MediaEntityExporter;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.http.AbstractCommandHandler;
import org.tinymediamanager.core.http.AbstractCommandHandler.CommandScope;
import org.tinymediamanager.core.tasks.ExportTask;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowExporter;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.tasks.TvShowARDetectorTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowEpisodeScrapeTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowFetchRatingsTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowMissingArtworkDownloadTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowReloadMediaInformationTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowRenameTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowScrapeTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowSubtitleSearchAndDownloadTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowTrailerDownloadTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowUpdateDatasourceTask;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.scraper.util.ListUtils;

/**
 * the class {@link TvShowCommandTask} handles movie related API calls
 * 
 * @author Manuel Laggner
 */
class TvShowCommandTask extends TmmThreadPool {
  private static final Logger                        LOGGER         = LoggerFactory.getLogger(TvShowCommandTask.class);

  private final List<AbstractCommandHandler.Command> commands;
  private final TvShowList                           tvShowList     = TvShowModuleManager.getInstance().getTvShowList();
  private final TvShowSettings                       tvShowSettings = TvShowModuleManager.getInstance().getSettings();
  private final List<TvShow>                         newTvShows     = new ArrayList<>();
  private final List<TvShowEpisode>                  newEpisodes    = new ArrayList<>();

  private TmmTask                                    activeTask;

  public TvShowCommandTask(List<AbstractCommandHandler.Command> commands) {
    super("TV show - HTTP commands");
    this.commands = commands;
  }

  @Override
  protected void doInBackground() {
    // 1. update commands
    updateDataSources();
    reloadMediaInfo();
    aspectRatioDetection();

    // 2. scrape commands
    scrape();

    // 2.1 fetch ratings
    fetchRatings();

    // 3. download trailer
    downloadTrailer();

    // 4. download subtitles
    downloadSubtitles();

    // 5. download missing artwork
    downloadMissingArtwork();

    // 6. rename
    rename();

    // 7. export
    export();
  }

  private void updateDataSources() {
    Set<String> dataSources = new TreeSet<>();
    Set<TvShow> tvShowFolders = new HashSet<>();

    List<TvShow> existingTvShows = new ArrayList<>(tvShowList.getTvShows());
    List<TvShowEpisode> existingEpisodes = new ArrayList<>();
    for (TvShow tvShow : existingTvShows) {
      existingEpisodes.addAll(tvShow.getEpisodes());
    }

    for (AbstractCommandHandler.Command command : commands) {
      if ("update".equals(command.action)) {
        dataSources.addAll(getDataSourcesForScope(command.scope));
        tvShowFolders.addAll(getTvShowFoldersForScope(command.scope));
      }
    }

    if (!dataSources.isEmpty()) {
      setTaskName(TmmResourceBundle.getString("update.datasource"));
      publishState(TmmResourceBundle.getString("update.datasource"), getProgressDone());

      activeTask = new TvShowUpdateDatasourceTask(dataSources);
      activeTask.run(); // blocking

      // done
      activeTask = null;
    }

    if (!tvShowFolders.isEmpty()) {
      setTaskName(TmmResourceBundle.getString("update.datasource"));
      publishState(TmmResourceBundle.getString("update.datasource"), getProgressDone());

      activeTask = new TvShowUpdateDatasourceTask(new ArrayList<>(tvShowFolders));
      activeTask.run(); // blocking

      // done
      activeTask = null;
    }

    // store all new movies from this run
    for (TvShow tvShow : tvShowList.getTvShows()) {
      if (!existingTvShows.contains(tvShow)) {
        newTvShows.add(tvShow);
        continue;
      }
      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        if (!existingEpisodes.contains(episode)) {
          newEpisodes.add(episode);
        }
      }
    }
  }

  private List<String> getDataSourcesForScope(CommandScope scope) {
    List<String> dataSources = new ArrayList<>();

    if (StringUtils.isBlank(scope.name)) {
      scope.name = "all";
    }

    switch (scope.name) {
      case "all":
        dataSources.addAll(tvShowSettings.getTvShowDataSource());
        break;

      case "single":
        for (String index : ListUtils.nullSafe(Arrays.asList(scope.args))) {
          try {
            int i = Integer.parseInt(index);
            if (tvShowSettings.getTvShowDataSource().size() >= i - 1) {
              dataSources.add(tvShowSettings.getTvShowDataSource().get(i - 1));
            }

          }
          catch (Exception e) {
            LOGGER.error("Could not parse index from command - {}", e.getMessage());
          }
        }
        break;

    }

    return dataSources;
  }

  public void reloadMediaInfo() {
    for (AbstractCommandHandler.Command command : commands) {
      if ("reloadMediaInfo".equals(command.action)) {

        LOGGER.info("reload media info... - {}", command);
        List<TvShow> tvshows = getTvShowsForScope(command.scope);
        List<TvShowEpisode> tvShowEpisodes = getEpisodesForScope(command.scope);

        if (!tvshows.isEmpty() && !tvShowEpisodes.isEmpty()) {

          setTaskName(TmmResourceBundle.getString("tvshow.updatemediainfo"));
          publishState(TmmResourceBundle.getString("tvshow.updatemediainfo"), getProgressDone());

          activeTask = new TvShowReloadMediaInformationTask(tvshows, tvShowEpisodes);
          activeTask.run();

          activeTask = null;
        }
      }
    }
  }

  public void aspectRatioDetection() {
    for (AbstractCommandHandler.Command command : commands) {
      if ("detectAspectRatio".equals(command.action)) {

        LOGGER.info("detecting aspect ratio... - {}", command);
        List<TvShowEpisode> tvShowEpisodes = getEpisodesForScope(command.scope);

        if (!tvShowEpisodes.isEmpty()) {

          setTaskName(TmmResourceBundle.getString("tvshow.ard"));
          publishState(TmmResourceBundle.getString("tvshow.ard"), getProgressDone());

          activeTask = new TvShowARDetectorTask(tvShowEpisodes);
          activeTask.run();

          activeTask = null;
        }
      }
    }
  }

  private List<TvShow> getTvShowFoldersForScope(CommandScope scope) {
    List<TvShow> tvShows = new ArrayList<>();

    switch (scope.name) {
      case "show":
        for (String path : ListUtils.nullSafe(Arrays.asList(scope.args))) {
          for (TvShow tvShow : tvShowList.getTvShows()) {
            if (tvShow.getPathNIO().toAbsolutePath().toString().equals(path)) {
              tvShows.add(tvShow);
              break;
            }
          }
        }
        break;
    }

    return tvShows;
  }

  private void scrape() {
    Set<TvShow> tvShowsToScrape = new LinkedHashSet<>();
    Set<TvShowEpisode> episodesToScrape = new LinkedHashSet<>();
    for (AbstractCommandHandler.Command command : commands) {
      if ("scrape".equals(command.action)) {
        tvShowsToScrape.addAll(getTvShowsForScope(command.scope));
        episodesToScrape.addAll(getEpisodesForScope(command.scope));
      }
    }

    // if we scrape already the whole show, no need to scrape dedicated episodes for it
    Set<TvShowEpisode> removedEpisode = new HashSet<>(); // no dupes
    for (TvShowEpisode ep : episodesToScrape) {
      if (tvShowsToScrape.contains(ep.getTvShow())) {
        removedEpisode.add(ep);
      }
    }
    episodesToScrape.removeAll(removedEpisode);

    if (!tvShowsToScrape.isEmpty()) {
      setTaskName(TmmResourceBundle.getString("tvshow.scraping"));
      publishState(TmmResourceBundle.getString("tvshow.scraping"), getProgressDone());

      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      List<TvShowScraperMetadataConfig> tvShowScraperMetadataConfig = tvShowSettings.getTvShowScraperMetadataConfig();
      List<TvShowEpisodeScraperMetadataConfig> episodeScraperMetadataConfig = tvShowSettings.getEpisodeScraperMetadataConfig();
      options.loadDefaults();

      TvShowScrapeTask.TvShowScrapeParams tvShowScrapeParams = new TvShowScrapeTask.TvShowScrapeParams(new ArrayList<>(tvShowsToScrape), options,
          tvShowScraperMetadataConfig, episodeScraperMetadataConfig);
      tvShowScrapeParams.setOverwriteExistingItems(!tvShowSettings.isDoNotOverwriteExistingData());

      activeTask = new TvShowScrapeTask(tvShowScrapeParams);
      activeTask.run(); // blocking

      // wait for all image downloads!
      while (TmmTaskManager.getInstance().isImageDownloadsRunning()) {
        try {
          Thread.sleep(2000);
        }
        catch (Exception e) {
          break;
        }
      }

      // done
      activeTask = null;
    }

    if (!episodesToScrape.isEmpty()) {
      setTaskName(TmmResourceBundle.getString("tvshow.scraping"));
      publishState(TmmResourceBundle.getString("tvshow.scraping"), getProgressDone());

      // re-group the episodes. If there is a "last used" scraper set for the show also take this into account for the episode
      Map<TvShow, List<TvShowEpisode>> groupedEpisodes = new HashMap<>();
      for (TvShowEpisode episode : episodesToScrape) {
        List<TvShowEpisode> episodes = groupedEpisodes.computeIfAbsent(episode.getTvShow(), k -> new ArrayList<>());
        episodes.add(episode);
      }

      // scrape new episodes
      for (Map.Entry<TvShow, List<TvShowEpisode>> entry : groupedEpisodes.entrySet()) {
        TvShow tvShow = entry.getKey();

        TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
        options.loadDefaults();

        // so for the known ones, we can directly start scraping
        if (StringUtils.isNoneBlank(tvShow.getLastScraperId(), tvShow.getLastScrapeLanguage())) {
          options.setMetadataScraper(MediaScraper.getMediaScraperById(tvShow.getLastScraperId(), ScraperType.TV_SHOW));
          options.setLanguage(MediaLanguages.valueOf(tvShow.getLastScrapeLanguage()));
        }

        List<TvShowEpisodeScraperMetadataConfig> episodeScraperMetadataConfig = TvShowModuleManager.getInstance()
            .getSettings()
            .getEpisodeScraperMetadataConfig();

        activeTask = new TvShowEpisodeScrapeTask(entry.getValue(), options, episodeScraperMetadataConfig,
            !TvShowModuleManager.getInstance().getSettings().isDoNotOverwriteExistingData());
        activeTask.run(); // blocking

        // wait for other tmm threads (artwork download et all)
        while (TmmTaskManager.getInstance().isPoolRunning()) {
          try {
            Thread.sleep(2000);
          }
          catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }

        // done
        activeTask = null;
      }
    }
  }

  private void fetchRatings() {
    Set<TvShow> tvShowsToScrape = new LinkedHashSet<>();
    Set<TvShowEpisode> episodesToScrape = new LinkedHashSet<>();
    for (AbstractCommandHandler.Command command : commands) {
      if ("fetchRatings".equals(command.action)) {
        tvShowsToScrape.addAll(getTvShowsForScope(command.scope));
        episodesToScrape.addAll(getEpisodesForScope(command.scope));
      }
    }

    if (!tvShowsToScrape.isEmpty() || !episodesToScrape.isEmpty()) {
      setTaskName(TmmResourceBundle.getString("tvshow.fetchratings"));
      publishState(TmmResourceBundle.getString("tvshow.fetchratings"), getProgressDone());

      activeTask = new TvShowFetchRatingsTask(tvShowsToScrape, episodesToScrape, RatingProvider.RatingSource.getRatingSourcesForTvShows());
      activeTask.run(); // blocking

      // wait for other tmm threads (artwork download et all)
      while (TmmTaskManager.getInstance().isPoolRunning()) {
        try {
          Thread.sleep(2000);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      // done
      activeTask = null;
    }
  }

  private void downloadTrailer() {
    Set<TvShow> tvShowsToProcess = new LinkedHashSet<>();

    for (AbstractCommandHandler.Command command : commands) {
      if ("downloadTrailer".equals(command.action)) {
        LOGGER.info("downloading trailers... - {}", command);

        // get movies with missing trailer in this language
        boolean onlyMissingTrailer = true;

        if (StringUtils.isNotBlank(command.args.get("onlyMissing"))) {
          onlyMissingTrailer = Boolean.parseBoolean(command.args.get("onlyMissing"));
        }

        for (TvShow tvShow : getTvShowsForScope(command.scope)) {
          if (onlyMissingTrailer) {
            if (tvShow.getMediaFiles(MediaFileType.TRAILER).isEmpty()) {
              tvShowsToProcess.add(tvShow);
            }
          }
          else {
            tvShowsToProcess.add(tvShow);
          }
        }
      }
    }

    if (!tvShowsToProcess.isEmpty()) {
      setTaskName(TmmResourceBundle.getString("trailer.download"));
      publishState(TmmResourceBundle.getString("trailer.download"), getProgressDone());

      for (TvShow tvShow : tvShowsToProcess) {
        activeTask = new TvShowTrailerDownloadTask(tvShow);
        activeTask.run(); // blocking

        // done
        activeTask = null;
      }
    }
  }

  private void downloadSubtitles() {
    for (AbstractCommandHandler.Command command : commands) {
      if ("downloadSubtitle".equals(command.action)) {
        String language = command.args.get("language");
        MediaLanguages mediaLanguages = null;

        if (StringUtils.isNotBlank(language)) {
          mediaLanguages = MediaLanguages.get(language);
        }

        // no language yet? take the setting
        if (mediaLanguages == null) {
          mediaLanguages = tvShowSettings.getScraperLanguage();
        }

        List<TvShowEpisode> episodesToProcess = new ArrayList<>();
        LOGGER.info("downloading missing subtitles in - '{}'...", command);

        // get movies with missing subtitles in this language
        boolean onlyMissingSubs = true;

        if (StringUtils.isNotBlank(command.args.get("onlyMissing"))) {
          onlyMissingSubs = Boolean.parseBoolean(command.args.get("onlyMissing"));
        }

        for (TvShowEpisode episode : getEpisodesForScope(command.scope)) {
          if (onlyMissingSubs) {
            boolean subtitleFound = false;
            for (MediaFile mf : episode.getMediaFiles(MediaFileType.VIDEO, MediaFileType.SUBTITLE)) {
              for (MediaFileSubtitle subtitle : mf.getSubtitles()) {
                if (StringUtils.isNotBlank(subtitle.getLanguage())) {
                  MediaLanguages subtitleLanguage = MediaLanguages.get(subtitle.getLanguage());
                  if (subtitleLanguage == mediaLanguages) {
                    subtitleFound = true;
                    break;
                  }
                }
              }

              if (subtitleFound) {
                break;
              }
            }

            if (!subtitleFound) {
              episodesToProcess.add(episode);
            }
          }
          else {
            episodesToProcess.add(episode);
          }
        }

        if (!episodesToProcess.isEmpty()) {
          setTaskName(TmmResourceBundle.getString("tvshow.download.subtitles"));
          publishState(TmmResourceBundle.getString("tvshow.download.subtitles"), getProgressDone());

          activeTask = new TvShowSubtitleSearchAndDownloadTask(episodesToProcess, mediaLanguages);
          activeTask.run();

          // done
          activeTask = null;
        }
      }
    }
  }

  private void downloadMissingArtwork() {
    for (AbstractCommandHandler.Command command : commands) {
      if ("downloadMissingArtwork".equals(command.action)) {
        setTaskName(TmmResourceBundle.getString("tvshow.downloadmissingartwork"));
        publishState(TmmResourceBundle.getString("tvshow.downloadmissingartwork"), getProgressDone());

        TvShowSearchAndScrapeOptions tvShowSearchAndScrapeConfig = new TvShowSearchAndScrapeOptions();
        tvShowSearchAndScrapeConfig.setCertificationCountry(TvShowModuleManager.getInstance().getSettings().getCertificationCountry());
        tvShowSearchAndScrapeConfig.setReleaseDateCountry(TvShowModuleManager.getInstance().getSettings().getReleaseDateCountry());

        // artwork scrapers
        List<MediaScraper> selectedArtworkScrapers = new ArrayList<>();
        for (MediaScraper artworkScraper : TvShowModuleManager.getInstance().getTvShowList().getAvailableArtworkScrapers()) {
          if (TvShowModuleManager.getInstance().getSettings().getArtworkScrapers().contains(artworkScraper.getId())) {
            selectedArtworkScrapers.add(artworkScraper);
          }
        }
        tvShowSearchAndScrapeConfig.setArtworkScraper(selectedArtworkScrapers);

        activeTask = new TvShowMissingArtworkDownloadTask(getTvShowsForScope(command.scope), getEpisodesForScope(command.scope),
            tvShowSearchAndScrapeConfig, TvShowModuleManager.getInstance().getSettings().getTvShowScraperMetadataConfig(),
            TvShowModuleManager.getInstance().getSettings().getEpisodeScraperMetadataConfig());
        activeTask.run();

        // done
        activeTask = null;

        // wait for other tmm threads (artwork download)
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

  private void rename() {
    Set<TvShow> tvShowsToRename = new LinkedHashSet<>();
    Set<TvShowEpisode> episodesToRename = new LinkedHashSet<>();
    for (AbstractCommandHandler.Command command : commands) {
      if ("rename".equals(command.action)) {
        tvShowsToRename.addAll(getTvShowsForScope(command.scope));
        episodesToRename.addAll(getEpisodesForScope(command.scope));
      }
    }

    if (!tvShowsToRename.isEmpty()) {
      setTaskName(TmmResourceBundle.getString("tvshow.rename"));
      publishState(TmmResourceBundle.getString("tvshow.rename"), getProgressDone());

      activeTask = new TvShowRenameTask(tvShowsToRename, episodesToRename);
      activeTask.run(); // blocking

      // done
      activeTask = null;
    }
  }

  private void export() {
    for (AbstractCommandHandler.Command command : commands) {
      if ("export".equals(command.action)) {
        List<TvShow> toExport = getTvShowsForScope(command.scope);

        if (toExport.isEmpty()) {
          continue;
        }

        String templateName = command.args.get("template");
        ExportTemplate template = MediaEntityExporter.findTemplates(MediaEntityExporter.TemplateType.TV_SHOW)
            .stream()
            .filter(t -> t.getPath().endsWith(templateName))
            .findFirst()
            .orElse(null);

        if (template == null) {
          continue;
        }

        String exportPath = command.args.get("exportPath");
        if (StringUtils.isBlank(exportPath)) {
          continue;
        }

        try {
          LOGGER.info("exporting TV shows - '{}'...", command);
          setTaskName(TmmResourceBundle.getString("tvshow.export"));
          publishState(TmmResourceBundle.getString("tvshow.export"), getProgressDone());

          activeTask = new ExportTask(TmmResourceBundle.getString("tvshow.export"), new TvShowExporter(Paths.get(template.getPath())), toExport,
              Paths.get(exportPath));
          activeTask.run(); // blocking

        }
        catch (Exception e) {
          LOGGER.error("could not export - '{}", e.getMessage());
        }

        // done
        activeTask = null;
      }
    }
  }

  private List<TvShow> getTvShowsForScope(CommandScope scope) {
    List<TvShow> tvShowsToProcess = new ArrayList<>();
    if (StringUtils.isBlank(scope.name)) {
      scope.name = "new";
    }

    switch (scope.name) {
      case "path":
        if (scope.args != null && scope.args.length > 0) {
          List<Path> paths = new ArrayList<>();
          for (String path : scope.args) {
            paths.add(Path.of(path).toAbsolutePath());
          }

          tvShowsToProcess.addAll(
              tvShowList.getTvShows().stream().filter(movie -> paths.contains(movie.getPathNIO().toAbsolutePath())).collect(Collectors.toList()));
        }
        break;

      case "dataSource":
        if (scope.args != null && scope.args.length > 0) {
          List<String> dataSources = Arrays.asList(scope.args);
          tvShowsToProcess
              .addAll(tvShowList.getTvShows().stream().filter(movie -> dataSources.contains(movie.getDataSource())).collect(Collectors.toList()));
        }
        break;

      case "all":
        tvShowsToProcess.addAll(tvShowList.getTvShows());
        break;

      case "unscraped":
        tvShowsToProcess.addAll(tvShowList.getUnscrapedTvShows());
        break;

      case "new":
      default:
        tvShowsToProcess.addAll(newTvShows);
        break;
    }

    return tvShowsToProcess;
  }

  private List<TvShowEpisode> getEpisodesForScope(CommandScope scope) {
    List<TvShowEpisode> episodesToProcess = new ArrayList<>();
    if (StringUtils.isBlank(scope.name)) {
      scope.name = "new";
    }

    switch (scope.name) {
      case "path":
        if (scope.args != null && scope.args.length > 0) {
          List<Path> paths = new ArrayList<>();
          for (String path : scope.args) {
            paths.add(Path.of(path).toAbsolutePath());
          }

          for (TvShow tvShow : tvShowList.getTvShows().stream().filter(tvShow -> paths.contains(tvShow.getPathNIO().toAbsolutePath())).toList()) {
            episodesToProcess.addAll(tvShow.getEpisodes());
          }
        }
        break;

      case "dataSource":
        if (scope.args != null && scope.args.length > 0) {
          List<String> dataSources = Arrays.asList(scope.args);

          for (TvShow tvShow : tvShowList.getTvShows().stream().filter(tvShow -> dataSources.contains(tvShow.getDataSource())).toList()) {
            episodesToProcess.addAll(tvShow.getEpisodes());
          }
        }
        break;

      case "unscraped":
        episodesToProcess.addAll(tvShowList.getUnscrapedEpisodes());
        break;

      case "all":
        for (TvShow tvShow : tvShowList.getTvShows()) {
          episodesToProcess.addAll(tvShow.getEpisodes());
        }
        break;

      case "new":
      default:
        episodesToProcess.addAll(newEpisodes);
        break;
    }

    return episodesToProcess;
  }

  @Override
  public void cancel() {
    super.cancel();

    if (activeTask != null) {
      activeTask.cancel();
    }
  }

  @Override
  public void callback(Object obj) {
    publishState(progressDone);
  }
}
