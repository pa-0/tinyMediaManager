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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieArtworkHelper;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSetScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSetSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.interfaces.IMovieSetMetadataProvider;

/**
 * the class {@link MovieSetScrapeTask} is used to automatically scrape movie sets
 * 
 * @author Manuel Laggner
 */
public class MovieSetScrapeTask extends TmmThreadPool {
  private static final Logger                       LOGGER = LoggerFactory.getLogger(MovieSetScrapeTask.class);

  private final List<MovieSet>                      movieSetsToScrape;
  private final MovieSetSearchAndScrapeOptions      scrapeOptions;
  private final List<MovieSetScraperMetadataConfig> scraperMetadataConfig;

  public MovieSetScrapeTask(List<MovieSet> movieSetsToScrape, MovieSetSearchAndScrapeOptions scrapeOptions,
      List<MovieSetScraperMetadataConfig> movieSetScraperMetadataConfig) {
    super(TmmResourceBundle.getString("movieset.scraping"));
    this.movieSetsToScrape = movieSetsToScrape;
    this.scrapeOptions = scrapeOptions;
    this.scraperMetadataConfig = movieSetScraperMetadataConfig;
  }

  @Override
  protected void doInBackground() {
    LOGGER.debug("start scraping movie sets...");
    start();

    initThreadPool(3, "scrape");
    for (MovieSet movieSet : movieSetsToScrape) {
      submitTask(new Worker(movieSet));
    }

    waitForCompletionOrCancel();

    LOGGER.debug("done scraping movie sets...");
  }

  @Override
  public void callback(Object obj) {
    // do not publish task description here, because with different workers the text is never right
    publishState(progressDone);
  }

  private class Worker implements Runnable {
    private final MovieSet movieSet;

    private Worker(MovieSet movieSet) {
      this.movieSet = movieSet;
    }

    @Override
    public void run() {
      try {
        MediaScraper mediaMetadataScraper = scrapeOptions.getMetadataScraper();
        List<MediaScraper> artworkScrapers = scrapeOptions.getArtworkScrapers();

        MovieSetSearchAndScrapeOptions options = new MovieSetSearchAndScrapeOptions(scrapeOptions);
        options.setIds(movieSet.getIds());

        MediaMetadata metadata = ((IMovieSetMetadataProvider) mediaMetadataScraper.getMediaProvider()).getMetadata(options);

        if (ScraperMetadataConfig.containsAnyMetadata(scraperMetadataConfig) || ScraperMetadataConfig.containsAnyCast(scraperMetadataConfig)) {
          movieSet.setMetadata(metadata, scraperMetadataConfig);
        }

        // scrape artwork if wanted
        if (ScraperMetadataConfig.containsAnyArtwork(scraperMetadataConfig)) {
          movieSet.setArtwork(getArtwork(movieSet, metadata, artworkScrapers), scraperMetadataConfig);
        }

        // fill missing movie data
        List<MovieSet.MovieSetMovie> movieSetMovies = createMovieSetMovies(metadata);
        if (!movieSetMovies.isEmpty()) {
          movieSet.setDummyMovies(movieSetMovies);
          movieSet.saveToDb();
        }
      }
      catch (Exception e) {
        LOGGER.error("getMetadata", e);
      }
    }

    private List<MediaArtwork> getArtwork(MovieSet movieSet, MediaMetadata metadata, List<MediaScraper> artworkScrapers) {
      List<MediaArtwork> artwork = new ArrayList<>();

      ArtworkSearchAndScrapeOptions options = new ArtworkSearchAndScrapeOptions(MediaType.MOVIE_SET);
      options.setDataFromOtherOptions(scrapeOptions);
      options.setArtworkType(MediaArtwork.MediaArtworkType.ALL);
      options.setMetadata(metadata);
      if (metadata != null) {
        options.addIds(metadata.getIds());
      }
      options.setLanguage(MovieModuleManager.getInstance().getSettings().getDefaultImageScraperLanguage());
      options.setFanartSize(MovieModuleManager.getInstance().getSettings().getImageFanartSize());
      options.setPosterSize(MovieModuleManager.getInstance().getSettings().getImagePosterSize());

      // scrape providers till one artwork has been found
      for (MediaScraper scraper : artworkScrapers) {
        IMovieArtworkProvider artworkProvider = (IMovieArtworkProvider) scraper.getMediaProvider();
        try {
          artwork.addAll(artworkProvider.getArtwork(options));
        }
        catch (MissingIdException ignored) {
          // nothing to do
        }
        catch (ScrapeException e) {
          LOGGER.error("getArtwork", e);
          MessageManager.instance.pushMessage(
              new Message(Message.MessageLevel.ERROR, movieSet, "message.scrape.movieartworkfailed", new String[] { ":", e.getLocalizedMessage() }));
        }
      }

      return artwork;
    }

    private List<MovieSet.MovieSetMovie> createMovieSetMovies(MediaMetadata info) {
      if (info == null) {
        return Collections.emptyList();
      }

      List<MovieSet.MovieSetMovie> movieSetMovies = new ArrayList<>();

      for (MediaMetadata item : info.getSubItems()) {
        // mix in the dummy movie
        MovieSet.MovieSetMovie movieSetMovie = new MovieSet.MovieSetMovie();
        movieSetMovie.setMetadata(item, Arrays.asList(MovieScraperMetadataConfig.values()), true);
        movieSetMovie.setLastScraperId(scrapeOptions.getMetadataScraper().getId());
        movieSetMovie.setLastScrapeLanguage(scrapeOptions.getLanguage().name());

        // POSTER
        if (!item.getMediaArt(MediaArtwork.MediaArtworkType.POSTER).isEmpty()) {
          int preferredSizeOrder = MovieModuleManager.getInstance().getSettings().getImagePosterSize().getOrder();
          List<MediaArtwork.ImageSizeAndUrl> sortedPosters = MovieArtworkHelper.sortArtworkUrls(item.getMediaArt(),
              MediaArtwork.MediaArtworkType.POSTER, preferredSizeOrder);
          if (!sortedPosters.isEmpty()) {
            movieSetMovie.setArtworkUrl(sortedPosters.get(0).getUrl(), MediaFileType.POSTER);
          }
        }

        // FANART
        if (!item.getMediaArt(MediaArtwork.MediaArtworkType.BACKGROUND).isEmpty()) {
          int preferredSizeOrder = MovieModuleManager.getInstance().getSettings().getImageFanartSize().getOrder();
          List<MediaArtwork.ImageSizeAndUrl> sortedFanarts = MovieArtworkHelper.sortArtworkUrls(item.getMediaArt(),
              MediaArtwork.MediaArtworkType.BACKGROUND, preferredSizeOrder);
          if (!sortedFanarts.isEmpty()) {
            movieSetMovie.setArtworkUrl(sortedFanarts.get(0).getUrl(), MediaFileType.FANART);
          }
        }

        movieSetMovies.add(movieSetMovie);
      }

      return movieSetMovies;
    }
  }
}
