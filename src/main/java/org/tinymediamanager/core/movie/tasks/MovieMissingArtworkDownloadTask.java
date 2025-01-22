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
package org.tinymediamanager.core.movie.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieArtworkHelper;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;

/**
 * The Class MovieMissingArtworkDownloadTask. Used to find and download missing for the given movies
 * 
 * @author Manuel Laggner
 */
public class MovieMissingArtworkDownloadTask extends TmmThreadPool {
  private static final Logger                    LOGGER = LoggerFactory.getLogger(MovieMissingArtworkDownloadTask.class);

  private final List<Movie>                      moviesToScrape;
  private final MovieSearchAndScrapeOptions      scrapeOptions;
  private final List<MovieScraperMetadataConfig> metadataConfig;

  public MovieMissingArtworkDownloadTask(List<Movie> moviesToScrape, MovieSearchAndScrapeOptions scrapeOptions,
      List<MovieScraperMetadataConfig> metadataConfig) {
    super(TmmResourceBundle.getString("task.missingartwork"));
    this.moviesToScrape = moviesToScrape;
    this.scrapeOptions = scrapeOptions;
    this.metadataConfig = metadataConfig;
  }

  @Override
  protected void doInBackground() {
    LOGGER.info("Getting missing artwork");
    initThreadPool(3, "scrapeMissingMovieArtwork");
    start();

    for (Movie movie : moviesToScrape) {
      submitTask(new Worker(movie, scrapeOptions, metadataConfig));
    }
    waitForCompletionOrCancel();
    LOGGER.info("Done getting missing artwork");
  }

  @Override
  public void callback(Object obj) {
    // do not publish task description here, because with different workers the text is never right
    publishState(progressDone);
  }

  /****************************************************************************************
   * Helper classes
   ****************************************************************************************/
  private static class Worker implements Runnable {
    private final Movie                            movie;
    private final MediaSearchAndScrapeOptions      scrapeOptions;
    private final List<MovieScraperMetadataConfig> metadataConfig;

    private Worker(Movie movie, MediaSearchAndScrapeOptions scrapeOptions, List<MovieScraperMetadataConfig> metadataConfig) {
      this.movie = movie;
      this.scrapeOptions = scrapeOptions;
      this.metadataConfig = metadataConfig;
    }

    @Override
    public void run() {
      // a) is there missing artwork according to the config? (to download them)
      if (MovieArtworkHelper.hasMissingArtwork(movie, metadataConfig)) {
        try {
          // set up scrapers
          ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
          List<MediaArtwork> artwork = new ArrayList<>();

          ArtworkSearchAndScrapeOptions options = new ArtworkSearchAndScrapeOptions(MediaType.MOVIE);
          options.setDataFromOtherOptions(scrapeOptions);
          options.setArtworkType(MediaArtworkType.ALL);
          options.setIds(movie.getIds());
          if (movie.isStacked()) {
            options.setId("mediaFile", new ArrayList<>(movie.getMediaFiles(MediaFileType.VIDEO)));
          }
          else {
            options.setId("mediaFile", movie.getMainFile());
          }
          options.setLanguage(MovieModuleManager.getInstance().getSettings().getDefaultImageScraperLanguage());
          options.setFanartSize(MovieModuleManager.getInstance().getSettings().getImageFanartSize());
          options.setPosterSize(MovieModuleManager.getInstance().getSettings().getImagePosterSize());

          // scrape selected providers
          options.getArtworkScrapers().parallelStream().forEach(scraper -> {
            if ("ffmpeg".equals(scraper.getId())) {
              // do not use FFmpeg here
              return;
            }

            IMovieArtworkProvider artworkProvider = (IMovieArtworkProvider) scraper.getMediaProvider();
            try {
              lock.writeLock().lock();
              artwork.addAll(artworkProvider.getArtwork(options));
            }
            catch (MissingIdException e) {
              LOGGER.debug("missing ID for scraper {}", artworkProvider.getProviderInfo().getId());
            }
            catch (ScrapeException e) {
              LOGGER.error("getArtwork", e);
              MessageManager.instance.pushMessage(
                  new Message(MessageLevel.ERROR, movie, "message.scrape.moviesetartworkfailed", new String[] { ":", e.getLocalizedMessage() }));
            }
            finally {
              lock.writeLock().unlock();
            }
          });

          // now set & download the artwork
          if (!artwork.isEmpty()) {
            MovieArtworkHelper.downloadMissingArtwork(movie, artwork, metadataConfig);
          }
        }
        catch (Exception e) {
          LOGGER.error("Thread crashed", e);
          MessageManager.instance.pushMessage(
              new Message(MessageLevel.ERROR, "MovieMissingArtwork", "message.scrape.threadcrashed", new String[] { ":", e.getLocalizedMessage() }));
        }
      }

      // b) do we need to cleanup artwork filenames?
      MovieArtworkHelper.cleanupArtwork(movie, metadataConfig);
    }
  }
}
