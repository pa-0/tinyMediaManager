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
package org.tinymediamanager.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ExportTemplate;
import org.tinymediamanager.core.MediaEntityExporter;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.movie.MovieComparator;
import org.tinymediamanager.core.movie.MovieExporter;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.tasks.MovieARDetectorTask;
import org.tinymediamanager.core.movie.tasks.MovieReloadMediaInformationTask;
import org.tinymediamanager.core.movie.tasks.MovieRenameTask;
import org.tinymediamanager.core.movie.tasks.MovieScrapeTask;
import org.tinymediamanager.core.movie.tasks.MovieSubtitleSearchAndDownloadTask;
import org.tinymediamanager.core.movie.tasks.MovieTrailerDownloadTask;
import org.tinymediamanager.core.movie.tasks.MovieUpdateDatasourceTask;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.util.ListUtils;

import picocli.CommandLine;

// @formatter:off
@CommandLine.Command(
        name = "movie",
        mixinStandardHelpOptions = true,
        sortOptions = false,
        synopsisHeading = "%n",
        footerHeading = "%nExamples:%n",
        footer = {
                "  tinyMediaManager movie -u -n -r",
                "    to find/scrape and rename new movies%n",
                "  tinyMediaManager movie -t -s",
                "    to download missing trailer/subtitles%n",
                "  tinyMediaManager movie -e -eT=ExcelXml -eP=/user/export/movies",
                "    to export the movie list with the ExcelXml template to /user/export/movies"
        }
)
// @formatter:on
/**
 * the class {@link MovieCommand} implements all movie related logic for the command line interface
 *
 * @author Wolfgang Janes, Manuel Laggner
 */
class MovieCommand implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(MovieCommand.class);

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

  @CommandLine.Option(names = { "-w", "--rewriteNFO" }, description = "Rewrite NFO files of all movies")
  boolean                     rewriteNFO;

  @CommandLine.ArgGroup
  AspectRatioDetect           ard;

  @CommandLine.ArgGroup
  MediaInfo                   mediaInfo;

  @Override
  public void run() {
    // update data sources
    if (datasource != null) {
      updateDataSources();
    }

    List<Movie> moviesToScrape = new ArrayList<>();

    // scrape movies
    if (scrape != null) {
      scrapeMovies(moviesToScrape);
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
      renameMovies(moviesToScrape);
    }

    // export
    if (export != null) {
      exportMovies();
    }

    // rewrite NFO
    if (rewriteNFO) {
      rewriteNfoFiles();
    }
  }

  private void updateDataSources() {
    LOGGER.info("updating movie data sources...");
    if (datasource.updateAll) {
      Runnable task = new MovieUpdateDatasourceTask();
      task.run(); // blocking
    }
    else {
      List<String> dataSources = new ArrayList<>(MovieModuleManager.getInstance().getSettings().getMovieDataSource());
      if (ListUtils.isNotEmpty(dataSources)) {
        for (Integer i : datasource.indices) {
          if (dataSources.size() >= i - 1) {
            Runnable task = new MovieUpdateDatasourceTask(dataSources.get(i - 1));
            task.run(); // blocking
          }
        }
      }
    }
    LOGGER.info("Found {} new movies", MovieModuleManager.getInstance().getMovieList().getNewMovies().size());

  }

  private void scrapeMovies(List<Movie> moviesToScrape) {
    Set<Movie> movies = new LinkedHashSet<>();

    if (scrape.scrapeAll) {
      LOGGER.info("scraping ALL movies...");
      movies.addAll(MovieModuleManager.getInstance().getMovieList().getMovies());
    }
    else if (scrape.scrapeNew) {
      LOGGER.info("scraping NEW movies...");
      movies.addAll(MovieModuleManager.getInstance().getMovieList().getNewMovies());
    }
    else if (scrape.scrapeUnscraped) {
      LOGGER.info("scraping UNSCRAPED movies...");
      movies.addAll(MovieModuleManager.getInstance().getMovieList().getUnscrapedMovies());
    }

    if (!movies.isEmpty()) {
      moviesToScrape.addAll(movies);

      MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
      List<MovieScraperMetadataConfig> config = MovieModuleManager.getInstance().getSettings().getScraperMetadataConfig();

      MovieScrapeTask.MovieScrapeParams movieScrapeParams = new MovieScrapeTask.MovieScrapeParams(moviesToScrape, options, config);
      movieScrapeParams.setOverwriteExistingItems(!MovieModuleManager.getInstance().getSettings().isDoNotOverwriteExistingData());
      MovieScrapeTask task = new MovieScrapeTask(movieScrapeParams);
      task.setRunInBackground(true); // to avoid smart scrape dialog
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

  private void detectAspectRatio() {
    List<Movie> moviesToDetect = new ArrayList<>();
    if (ard.ardAll) {
      moviesToDetect = MovieModuleManager.getInstance().getMovieList().getMovies();
    }
    else if (ard.ardNew) {
      moviesToDetect = MovieModuleManager.getInstance().getMovieList().getNewMovies();
    }

    if (!moviesToDetect.isEmpty()) {
      TmmTask task = new MovieARDetectorTask(moviesToDetect);
      task.run();
    }
  }

  private void gatherMediaInfo() {
    List<Movie> movies = MovieModuleManager.getInstance().getMovieList().getMovies();

    if (!movies.isEmpty()) {
      TmmTask task = new MovieReloadMediaInformationTask(movies);
      task.run();
    }
  }

  private void downloadTrailer() {
    LOGGER.info("downloading missing trailers...");
    List<Movie> moviesWithoutTrailer = MovieModuleManager.getInstance()
        .getMovieList()
        .getMovies()
        .stream()
        .filter(movie -> movie.getMediaFiles(MediaFileType.TRAILER).isEmpty())
        .collect(Collectors.toList());

    for (Movie movie : moviesWithoutTrailer) {
      TmmTask task = new MovieTrailerDownloadTask(movie);
      task.run(); // blocking
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
      languages.add(MovieModuleManager.getInstance().getSettings().getSubtitleScraperLanguage());
    }

    List<Movie> moviesWithoutSubtitle = MovieModuleManager.getInstance()
        .getMovieList()
        .getMovies()
        .stream()
        .filter(movie -> !movie.getHasSubtitles())
        .collect(Collectors.toList());

    for (MediaLanguages language : languages) {
      Runnable task = new MovieSubtitleSearchAndDownloadTask(moviesWithoutSubtitle, language);
      task.run(); // blocking
    }
  }

  private void renameMovies(List<Movie> moviesToScrape) {
    List<Movie> moviesToRename = new ArrayList<>();
    if (rename.renameNew) {
      LOGGER.info("renaming NEW/RECENTLY SCRAPED movies...");
      moviesToRename.addAll(moviesToScrape);
    }
    else if (rename.renameAll) {
      LOGGER.info("renaming ALL movies...");
      moviesToRename.addAll(MovieModuleManager.getInstance().getMovieList().getMovies());
    }

    if (!moviesToRename.isEmpty()) {
      Runnable task = new MovieRenameTask(moviesToScrape);
      task.run(); // blocking
    }
  }

  private void exportMovies() {
    for (ExportTemplate exportTemplate : MediaEntityExporter.findTemplates(MediaEntityExporter.TemplateType.MOVIE)) {
      if (exportTemplate.getPath().endsWith(export.template)) {
        // ok, our template has been found under templates
        LOGGER.info("exporting movies...");

        MovieExporter ex = null;
        try {
          ex = new MovieExporter(Paths.get(exportTemplate.getPath()));

          List<Movie> movies = MovieModuleManager.getInstance().getMovieList().getMovies();
          movies.sort(new MovieComparator());
          ex.export(movies, export.path);
        }
        catch (Exception e) {
          LOGGER.error("could not export movies - {}", e.getMessage());
        }

        return;
      }
    }
  }

  private void rewriteNfoFiles() {
    if (MovieModuleManager.getInstance().getSettings().getNfoFilenames().isEmpty()) {
      LOGGER.info("Not writing any NFO file, because NFO filename preferences were empty...");
      return;
    }

    for (Movie movie : MovieModuleManager.getInstance().getMovieList().getMovies()) {
      movie.writeNFO();
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
    @CommandLine.Option(names = { "-n", "--scrapeNew", }, description = "Scrape new movies (found with the update options)")
    boolean scrapeNew;

    @CommandLine.Option(names = { "--scrapeUnscraped", }, description = "Scrape all unscraped movies")
    boolean scrapeUnscraped;

    @CommandLine.Option(names = { "--scrapeAll", }, description = "Scrape all movies")
    boolean scrapeAll;
  }

  static class AspectRatioDetect {
    @CommandLine.Option(names = { "-d", "--ardNew", }, description = "Detect aspect ratio of new movies (found with the update options)")
    boolean ardNew;

    @CommandLine.Option(names = { "-dA", "--ardAll", }, description = "Detect aspect ratio of all movies")
    boolean ardAll;
  }

  static class Subtitle {
    @CommandLine.Option(names = { "-s", "--downloadSubtitles" }, required = true, description = "Download missing subtitles")
    boolean  download;

    @CommandLine.Option(names = { "-sL", "--subtitleLanguage" }, paramLabel = "<language>", description = "Desired subtitle language(s) (optional)")
    String[] languages;
  }

  static class Rename {
    @CommandLine.Option(names = { "-r", "--renameNew", }, required = true, description = "Rename & cleanup all movies from former scrape command")
    boolean renameNew;

    @CommandLine.Option(names = { "--renameAll", }, required = true, description = "Rename & cleanup all movies")
    boolean renameAll;
  }

  static class Export {
    @CommandLine.Option(names = { "-e", "--export" }, required = true, description = "Export your movie list using a specified template")
    boolean export;

    @CommandLine.Option(names = { "-eT",
        "--exportTemplate" }, required = true, description = "The export template to use. Use the folder name from the templates folder")
    String  template;

    @CommandLine.Option(names = { "-eP", "--exportPath" }, required = true, description = "The path to export your movie list to")
    Path    path;
  }

  static class MediaInfo {
    @CommandLine.Option(names = { "-mi", "--mediainfo" }, description = "Update mediainfo")
    boolean medainfo;

    @CommandLine.Option(names = { "-mix", "--mediainfoXml" }, description = "Update medianifo - ignore XML")
    boolean mediainfoXml;
  }
}
