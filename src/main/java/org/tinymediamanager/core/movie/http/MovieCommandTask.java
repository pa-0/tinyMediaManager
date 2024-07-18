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
package org.tinymediamanager.core.movie.http;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.tinymediamanager.core.movie.MovieExporter;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.tasks.MovieFetchRatingsTask;
import org.tinymediamanager.core.movie.tasks.MovieMissingArtworkDownloadTask;
import org.tinymediamanager.core.movie.tasks.MovieRenameTask;
import org.tinymediamanager.core.movie.tasks.MovieScrapeTask;
import org.tinymediamanager.core.movie.tasks.MovieSubtitleSearchAndDownloadTask;
import org.tinymediamanager.core.movie.tasks.MovieTrailerDownloadTask;
import org.tinymediamanager.core.movie.tasks.MovieUpdateDatasourceTask;
import org.tinymediamanager.core.tasks.ExportTask;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.scraper.util.ListUtils;

/**
 * the class {@link MovieCommandTask} handles movie related API calls
 * 
 * @author Manuel Laggner
 */
class MovieCommandTask extends TmmThreadPool {
  private static final Logger                        LOGGER        = LoggerFactory.getLogger(MovieCommandTask.class);

  private final List<AbstractCommandHandler.Command> commands;
  private final MovieList                            movieList     = MovieModuleManager.getInstance().getMovieList();
  private final MovieSettings                        movieSettings = MovieModuleManager.getInstance().getSettings();
  private final List<Movie>                          newMovies     = new ArrayList<>();

  private TmmTask                                    activeTask;

  public MovieCommandTask(List<AbstractCommandHandler.Command> commands) {
    super("Movie - HTTP commands");
    this.commands = commands;
  }

  @Override
  protected void doInBackground() {
    // 1. update commands
    updateDataSources();

    // 2. scrape commands
    scrape();

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
    List<Movie> existingMovies = new ArrayList<>(movieList.getMovies());

    for (AbstractCommandHandler.Command command : commands) {
      if ("update".equals(command.action)) {
        dataSources.addAll(getDataSourcesForScope(command.scope));
      }
    }

    if (!dataSources.isEmpty()) {
      setTaskName(TmmResourceBundle.getString("update.datasource"));
      publishState(TmmResourceBundle.getString("update.datasource"), getProgressDone());

      activeTask = new MovieUpdateDatasourceTask(dataSources);
      activeTask.run(); // blocking

      // done
      activeTask = null;
    }

    // store all new movies from this run
    for (Movie movie : movieList.getMovies()) {
      if (!existingMovies.contains(movie)) {
        newMovies.add(movie);
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
        dataSources.addAll(movieSettings.getMovieDataSource());
        break;

      case "single":
        for (String index : ListUtils.nullSafe(Arrays.asList(scope.args))) {
          try {
            int i = Integer.parseInt(index);
            if (movieSettings.getMovieDataSource().size() >= i - 1) {
              dataSources.add(movieSettings.getMovieDataSource().get(i - 1));
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

  private void scrape() {
    for (AbstractCommandHandler.Command command : commands) {

      if ("scrape".equals(command.action)) {
        // scrape
        List<Movie> moviesToScrape = getMoviesForScope(command.scope);

        if (!moviesToScrape.isEmpty()) {
          setTaskName(TmmResourceBundle.getString("movie.scraping"));
          publishState(TmmResourceBundle.getString("movie.scraping"), getProgressDone());

          MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
          List<MovieScraperMetadataConfig> config = movieSettings.getScraperMetadataConfig();

          // override default scraper?
          if (StringUtils.isNotBlank(command.args.get("scraper"))) {
            String scraperId = command.args.get("scraper");
            MediaScraper scraper = MediaScraper.getMediaScraperById(scraperId, ScraperType.MOVIE);
            if (scraper != null) {
              options.setMetadataScraper(scraper);
            }
          }

          MovieScrapeTask.MovieScrapeParams movieScrapeParams = new MovieScrapeTask.MovieScrapeParams(new ArrayList<>(moviesToScrape), options,
              config);
          movieScrapeParams.setOverwriteExistingItems(!movieSettings.isDoNotOverwriteExistingData());
          MovieScrapeTask task = new MovieScrapeTask(movieScrapeParams);
          task.setRunInBackground(true); // to avoid smart scrape dialog

          activeTask = task;
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
      else if ("fetchRatings".equals(command.action)) {
        // fetchRatings
        List<Movie> moviesToScrape = getMoviesForScope(command.scope);
        if (!moviesToScrape.isEmpty()) {
          setTaskName(TmmResourceBundle.getString("movie.fetchratings"));
          publishState(TmmResourceBundle.getString("movie.fetchratings"), getProgressDone());

          MovieFetchRatingsTask task = new MovieFetchRatingsTask(moviesToScrape, RatingProvider.RatingSource.getRatingSourcesForMovies());

          activeTask = task;
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
  }

  private void downloadTrailer() {
    Set<Movie> moviesToProcess = new LinkedHashSet<>();

    for (AbstractCommandHandler.Command command : commands) {
      if ("downloadTrailer".equals(command.action)) {
        LOGGER.info("downloading trailers... - {}", command);

        // get movies with missing trailer in this language
        boolean onlyMissingTrailer = true;

        if (StringUtils.isNotBlank(command.args.get("onlyMissing"))) {
          onlyMissingTrailer = Boolean.parseBoolean(command.args.get("onlyMissing"));
        }

        for (Movie movie : getMoviesForScope(command.scope)) {
          if (onlyMissingTrailer) {
            if (movie.getMediaFiles(MediaFileType.TRAILER).isEmpty()) {
              moviesToProcess.add(movie);
            }
          }
          else {
            moviesToProcess.add(movie);
          }
        }
      }
    }

    if (!moviesToProcess.isEmpty()) {
      setTaskName(TmmResourceBundle.getString("trailer.download"));
      publishState(TmmResourceBundle.getString("trailer.download"), getProgressDone());

      for (Movie movie : moviesToProcess) {
        activeTask = new MovieTrailerDownloadTask(movie);
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
          mediaLanguages = movieSettings.getScraperLanguage();
        }

        List<Movie> moviesToProcess = new ArrayList<>();
        LOGGER.info("downloading missing subtitles in - '{}'...", command);

        // get movies with missing subtitles in this language
        boolean onlyMissingSubs = true;

        if (StringUtils.isNotBlank(command.args.get("onlyMissing"))) {
          onlyMissingSubs = Boolean.parseBoolean(command.args.get("onlyMissing"));
        }

        for (Movie movie : getMoviesForScope(command.scope)) {
          if (onlyMissingSubs) {
            boolean subtitleFound = false;
            for (MediaFile mf : movie.getMediaFiles(MediaFileType.VIDEO, MediaFileType.SUBTITLE)) {
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
              moviesToProcess.add(movie);
            }
          }
          else {
            moviesToProcess.add(movie);
          }
        }

        if (!moviesToProcess.isEmpty()) {
          setTaskName(TmmResourceBundle.getString("movie.download.subtitles"));
          publishState(TmmResourceBundle.getString("movie.download.subtitles"), getProgressDone());

          activeTask = new MovieSubtitleSearchAndDownloadTask(moviesToProcess, mediaLanguages);
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
        setTaskName(TmmResourceBundle.getString("movie.downloadmissingartwork"));
        publishState(TmmResourceBundle.getString("movie.downloadmissingartwork"), getProgressDone());

        MovieSearchAndScrapeOptions movieSearchAndScrapeConfig = new MovieSearchAndScrapeOptions();
        movieSearchAndScrapeConfig.setCertificationCountry(MovieModuleManager.getInstance().getSettings().getCertificationCountry());
        movieSearchAndScrapeConfig.setReleaseDateCountry(MovieModuleManager.getInstance().getSettings().getReleaseDateCountry());

        // artwork scrapers
        List<MediaScraper> selectedArtworkScrapers = new ArrayList<>();
        for (MediaScraper artworkScraper : MovieModuleManager.getInstance().getMovieList().getAvailableArtworkScrapers()) {
          if (MovieModuleManager.getInstance().getSettings().getArtworkScrapers().contains(artworkScraper.getId())) {
            selectedArtworkScrapers.add(artworkScraper);
          }
        }
        movieSearchAndScrapeConfig.setArtworkScraper(selectedArtworkScrapers);

        activeTask = new MovieMissingArtworkDownloadTask(getMoviesForScope(command.scope), movieSearchAndScrapeConfig,
            MovieModuleManager.getInstance().getSettings().getScraperMetadataConfig());
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
    Set<Movie> moviesToRename = new LinkedHashSet<>();
    for (AbstractCommandHandler.Command command : commands) {
      if ("rename".equals(command.action)) {
        moviesToRename.addAll(getMoviesForScope(command.scope));
      }
    }

    if (!moviesToRename.isEmpty()) {
      setTaskName(TmmResourceBundle.getString("movie.rename"));
      publishState(TmmResourceBundle.getString("movie.rename"), getProgressDone());

      activeTask = new MovieRenameTask(new ArrayList<>(moviesToRename));
      activeTask.run(); // blocking

      // done
      activeTask = null;
    }
  }

  private void export() {
    for (AbstractCommandHandler.Command command : commands) {
      if ("export".equals(command.action)) {
        List<Movie> toExport = getMoviesForScope(command.scope);

        if (toExport.isEmpty()) {
          continue;
        }

        String templateName = command.args.get("template");
        ExportTemplate template = MediaEntityExporter.findTemplates(MediaEntityExporter.TemplateType.MOVIE)
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
          LOGGER.info("exporting movies - '{}'...", command);
          setTaskName(TmmResourceBundle.getString("movie.export"));
          publishState(TmmResourceBundle.getString("movie.export"), getProgressDone());

          activeTask = new ExportTask(TmmResourceBundle.getString("movie.export"), new MovieExporter(Paths.get(template.getPath())), toExport,
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

  private List<Movie> getMoviesForScope(CommandScope scope) {
    List<Movie> moviesToProcess = new ArrayList<>();
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

          moviesToProcess.addAll(
              movieList.getMovies().stream().filter(movie -> paths.contains(movie.getPathNIO().toAbsolutePath())).collect(Collectors.toList()));
        }
        break;

      case "dataSource":
        if (scope.args != null && scope.args.length > 0) {
          List<String> dataSources = new ArrayList<>();
          for (String arg : scope.args) {
            // check if this could be an index
            try {
              dataSources.add(movieSettings.getMovieDataSource().get(Integer.parseInt(arg)));
            }
            catch (Exception e) {
              // just add it as a path
              dataSources.add(arg);
            }
          }

          moviesToProcess
              .addAll(movieList.getMovies().stream().filter(movie -> dataSources.contains(movie.getDataSource())).collect(Collectors.toList()));
        }
        break;

      case "new":
        moviesToProcess.addAll(newMovies);
        break;

      case "unscraped":
        moviesToProcess.addAll(movieList.getUnscrapedMovies());
        break;

      case "all":
      default:
        moviesToProcess.addAll(movieList.getMovies());
        break;
    }

    return moviesToProcess;
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
