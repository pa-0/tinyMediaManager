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
package org.tinymediamanager.core.movie.tasks;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.movie.MovieHelpers;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.TrailerSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieTrailerProvider;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.thirdparty.trakttv.MovieSyncTraktTvTask;
import org.tinymediamanager.ui.movies.dialogs.MovieChooserDialog;

/**
 * The Class MovieScrapeTask.
 *
 * @author Manuel Laggner
 */
public class MovieScrapeTask extends TmmThreadPool {
  private static final Logger     LOGGER = LoggerFactory.getLogger(MovieScrapeTask.class);

  private final MovieScrapeParams movieScrapeParams;
  private final List<Movie>       smartScrapeList;
  private boolean                 runInBackground;

  public MovieScrapeTask(final MovieScrapeParams movieScrapeParams) {
    super(TmmResourceBundle.getString("movie.scraping"));
    this.movieScrapeParams = movieScrapeParams;
    this.smartScrapeList = new ArrayList<>(0);
    this.runInBackground = false;
  }

  public void setRunInBackground(boolean runInBackground) {
    this.runInBackground = runInBackground;
  }

  @Override
  protected void doInBackground() {
    MediaScraper mediaMetadataScraper = movieScrapeParams.searchAndScrapeOptions.getMetadataScraper();
    if (!mediaMetadataScraper.isEnabled()) {
      return;
    }

    initThreadPool(3, "scrape");
    start();

    for (Movie movie : movieScrapeParams.moviesToScrape) {
      submitTask(new Worker(movie));
    }
    waitForCompletionOrCancel();

    // initiate smart scrape
    if (!smartScrapeList.isEmpty() && !GraphicsEnvironment.isHeadless() && !runInBackground) {
      try {
        SwingUtilities.invokeAndWait(() -> {
          int selectedCount = smartScrapeList.size();
          int index = 0;

          do {
            Movie movie = smartScrapeList.get(index);
            MovieChooserDialog dialogMovieChooser = new MovieChooserDialog(movie, index, selectedCount);
            dialogMovieChooser.setVisible(true);

            if (!dialogMovieChooser.isContinueQueue()) {
              break;
            }

            if (dialogMovieChooser.isNavigateBack()) {
              index -= 1;
            }
            else {
              index += 1;
            }

          } while (index < selectedCount);
        });
      }
      catch (Exception e) {
        LOGGER.error("SmartScrape crashed: {}", e.getMessage());
      }
    }

    if (MovieModuleManager.getInstance().getSettings().getSyncTrakt()) {
      MovieSyncTraktTvTask task = new MovieSyncTraktTvTask(movieScrapeParams.moviesToScrape);
      task.setSyncCollection(MovieModuleManager.getInstance().getSettings().getSyncTraktCollection());
      task.setSyncWatched(MovieModuleManager.getInstance().getSettings().getSyncTraktWatched());
      task.setSyncRating(MovieModuleManager.getInstance().getSettings().getSyncTraktRating());

      TmmTaskManager.getInstance().addUnnamedTask(task);
    }

    LOGGER.info("Done scraping movies)");
  }

  @Override
  public void callback(Object obj) {
    // do not publish task description here, because with different workers the text is never right
    publishState(progressDone);
  }

  /****************************************************************************************
   * Helper classes
   ****************************************************************************************/
  private class Worker implements Runnable {
    private MovieList   movieList;
    private final Movie movie;

    public Worker(Movie movie) {
      this.movie = movie;
    }

    @Override
    public void run() {
      try {
        movieList = MovieModuleManager.getInstance().getMovieList();
        // set up scrapers
        MediaScraper mediaMetadataScraper = movieScrapeParams.searchAndScrapeOptions.getMetadataScraper();
        List<MediaScraper> artworkScrapers = movieScrapeParams.searchAndScrapeOptions.getArtworkScrapers();
        List<MediaScraper> trailerScrapers = movieScrapeParams.searchAndScrapeOptions.getTrailerScrapers();

        // search movie
        MediaSearchResult result1 = null;
        if (movieScrapeParams.doSearch) {
          result1 = searchForMovie(mediaMetadataScraper);
          if (result1 == null) {
            // append this search request to the UI with search & scrape dialog
            synchronized (smartScrapeList) {
              smartScrapeList.add(movie);
              return;
            }
          }
        }

        // get metadata, artwork and trailers
        if ((movieScrapeParams.doSearch && result1 != null) || !movieScrapeParams.doSearch) {
          MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions(movieScrapeParams.searchAndScrapeOptions);
          options.setSearchResult(result1);

          // we didn't do a search - pass imdbid and tmdbid from movie object
          if (movieScrapeParams.doSearch) {
            options.setIds(result1.getIds());
            // override scraper with one from search result
            mediaMetadataScraper = movieList.getMediaScraperById(result1.getProviderId());
          }
          else {
            options.setIds(movie.getIds());
          }

          // scrape metadata if wanted
          MediaMetadata md = null;

          if (mediaMetadataScraper != null && mediaMetadataScraper.getMediaProvider() != null) {
            LOGGER.info("=====================================================");
            LOGGER.info("Scrape movie metadata with scraper: " + mediaMetadataScraper.getMediaProvider().getProviderInfo().getId() + ", "
                + mediaMetadataScraper.getMediaProvider().getProviderInfo().getVersion());
            LOGGER.info(options.toString());
            LOGGER.info("=====================================================");
            try {
              md = ((IMovieMetadataProvider) mediaMetadataScraper.getMediaProvider()).getMetadata(options);

              if (movieScrapeParams.scraperMetadataConfig.contains(MovieScraperMetadataConfig.COLLECTION)
                  && md.getIdAsInt(MediaMetadata.TMDB_SET) == 0 && !mediaMetadataScraper.getId().equals(MediaMetadata.TMDB)) {
                int movieSetId = MetadataUtil.getMovieSetId(md.getIds());
                if (movieSetId > 0) {
                  md.setId(MediaMetadata.TMDB_SET, movieSetId);
                }
              }

              // also inject other ids
              MediaIdUtil.injectMissingIds(md.getIds(), MediaType.MOVIE);

              // also fill other ratings if ratings are requested
              if (MovieModuleManager.getInstance().getSettings().isFetchAllRatings()
                  && movieScrapeParams.scraperMetadataConfig.contains(MovieScraperMetadataConfig.RATING)) {
                for (MediaRating rating : ListUtils.nullSafe(RatingProvider.getRatings(md.getIds(), MediaType.MOVIE))) {
                  if (!md.getRatings().contains(rating)) {
                    md.addRating(rating);
                  }
                }
              }
            }
            catch (MissingIdException e) {
              LOGGER.warn("missing id for scrape");
              MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, movie, "scraper.error.missingid"));
            }
            catch (ScrapeException e) {
              LOGGER.error("searchMovieFallback", e);
              MessageManager.instance.pushMessage(
                  new Message(MessageLevel.ERROR, movie, "message.scrape.metadatamoviefailed", new String[] { ":", e.getLocalizedMessage() }));
            }

            if (md != null && (ScraperMetadataConfig.containsAnyMetadata(movieScrapeParams.scraperMetadataConfig)
                || ScraperMetadataConfig.containsAnyCast(movieScrapeParams.scraperMetadataConfig))) {
              movie.setMetadata(md, movieScrapeParams.scraperMetadataConfig, movieScrapeParams.overwriteExistingItems);
              movie.setLastScraperId(movieScrapeParams.searchAndScrapeOptions.getMetadataScraper().getId());
              movie.setLastScrapeLanguage(movieScrapeParams.searchAndScrapeOptions.getLanguage().name());
            }

            // scrape artwork if wanted
            if (ScraperMetadataConfig.containsAnyArtwork(movieScrapeParams.scraperMetadataConfig)) {
              movie.setArtwork(getArtwork(movie, md, artworkScrapers), movieScrapeParams.scraperMetadataConfig,
                  movieScrapeParams.overwriteExistingItems);
            }

            // scrape trailer if wanted
            if (movieScrapeParams.scraperMetadataConfig.contains(MovieScraperMetadataConfig.TRAILER)) {
              movie.setTrailers(getTrailers(movie, md, trailerScrapers));
              movie.saveToDb();
              movie.writeNFO();

              // start automatic movie trailer download
              MovieHelpers.startAutomaticTrailerDownload(movie);
            }
          }
        }
      }
      catch (Exception e) {
        LOGGER.error("Thread crashed", e);
        MessageManager.instance.pushMessage(
            new Message(MessageLevel.ERROR, "MovieScraper", "message.scrape.threadcrashed", new String[] { ":", e.getLocalizedMessage() }));
      }
    }

    private MediaSearchResult searchForMovie(MediaScraper mediaMetadataProvider) throws ScrapeException {
      List<MediaSearchResult> results = movieList.searchMovie(movie.getTitle(), movie.getYear(), movie.getIds(), mediaMetadataProvider);
      MediaSearchResult result = null;

      if (ListUtils.isNotEmpty(results)) {
        result = results.get(0);
        // check if there is an other result with 100% score
        if (results.size() > 1) {
          MediaSearchResult result2 = results.get(1);
          // if both results have the same score - do not take any result
          if (result.getScore() == result2.getScore()) {
            LOGGER.info("two identical results, can't decide which to take - ignore result");
            MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, movie, "movie.scrape.toosimilar"));
            return null;
          }
        }

        // get threshold from settings (default 0.75) - to minimize false positives
        final double scraperTreshold = MovieModuleManager.getInstance().getSettings().getScraperThreshold();
        LOGGER.info("using treshold from settings of {}", scraperTreshold);
        if (result.getScore() < scraperTreshold) {
          LOGGER.info("score is lower than {} ({}) - ignore result", scraperTreshold, result.getScore());
          MessageManager.instance.pushMessage(
              new Message(MessageLevel.ERROR, movie, "movie.scrape.toolowscore", new String[] { String.format("%.2f", scraperTreshold) }));
          return null;
        }
      }
      else {
        LOGGER.info("no result found for {}", movie.getTitle());
        MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, movie, "movie.scrape.nomatchfound"));
      }

      return result;
    }

    private List<MediaArtwork> getArtwork(Movie movie, MediaMetadata metadata, List<MediaScraper> artworkScrapers) {
      ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
      List<MediaArtwork> artwork = new ArrayList<>();

      ArtworkSearchAndScrapeOptions options = new ArtworkSearchAndScrapeOptions(MediaType.MOVIE);
      options.setDataFromOtherOptions(movieScrapeParams.searchAndScrapeOptions);
      options.setArtworkType(MediaArtworkType.ALL);
      options.setMetadata(metadata);
      if (metadata != null) {
        options.addIds(metadata.getIds());
      }
      if (movie.isStacked()) {
        options.setId("mediaFile", new ArrayList<>(movie.getMediaFiles(MediaFileType.VIDEO)));
      }
      else {
        options.setId("mediaFile", movie.getMainFile());
      }
      options.setLanguage(MovieModuleManager.getInstance().getSettings().getDefaultImageScraperLanguage());
      options.setFanartSize(MovieModuleManager.getInstance().getSettings().getImageFanartSize());
      options.setPosterSize(MovieModuleManager.getInstance().getSettings().getImagePosterSize());

      // scrape providers
      artworkScrapers.parallelStream().forEach(scraper -> {
        if ("ffmpeg".equals(scraper.getId())) {
          // do not use FFmpeg here
          return;
        }

        IMovieArtworkProvider artworkProvider = (IMovieArtworkProvider) scraper.getMediaProvider();
        try {
          lock.writeLock().lock();
          artwork.addAll(artworkProvider.getArtwork(options));
          lock.writeLock().unlock();
        }
        catch (MissingIdException ignored) {
          // no need to log here
        }
        catch (ScrapeException e) {
          LOGGER.error("getArtwork", e);
          MessageManager.instance.pushMessage(
              new Message(MessageLevel.ERROR, movie, "message.scrape.movieartworkfailed", new String[] { ":", e.getLocalizedMessage() }));
        }
      });

      return artwork;
    }

    private List<MediaTrailer> getTrailers(Movie movie, MediaMetadata metadata, List<MediaScraper> trailerScrapers) {
      ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
      List<MediaTrailer> trailers = new ArrayList<>();

      TrailerSearchAndScrapeOptions options = new TrailerSearchAndScrapeOptions(MediaType.MOVIE);
      options.setDataFromOtherOptions(movieScrapeParams.searchAndScrapeOptions);
      options.setMetadata(metadata);
      if (metadata != null) {
        options.addIds(metadata.getIds());
      }

      // scrape trailers
      trailerScrapers.parallelStream().forEach(trailerScraper -> {
        IMovieTrailerProvider trailerProvider = (IMovieTrailerProvider) trailerScraper.getMediaProvider();
        try {
          lock.writeLock().lock();
          trailers.addAll(trailerProvider.getTrailers(options));
          lock.writeLock().unlock();
        }
        catch (MissingIdException e) {
          LOGGER.debug("no usable ID found for scraper {}", trailerScraper.getMediaProvider().getProviderInfo().getId());
        }
        catch (ScrapeException e) {
          LOGGER.error("getTrailers", e);
          MessageManager.instance
              .pushMessage(new Message(MessageLevel.ERROR, movie, "message.scrape.trailerfailed", new String[] { ":", e.getLocalizedMessage() }));
        }
      });

      return trailers;
    }
  }

  public static class MovieScrapeParams {
    private final List<Movie>                      moviesToScrape;
    private final List<MovieScraperMetadataConfig> scraperMetadataConfig;
    private final MovieSearchAndScrapeOptions      searchAndScrapeOptions;

    private boolean                                doSearch;
    private boolean                                overwriteExistingItems;

    public MovieScrapeParams(final List<Movie> moviesToScrape, final MovieSearchAndScrapeOptions searchAndScrapeOptions,
        final List<MovieScraperMetadataConfig> scraperMetadataConfig) {
      this.moviesToScrape = new ArrayList<>(moviesToScrape);
      this.searchAndScrapeOptions = searchAndScrapeOptions;
      this.scraperMetadataConfig = new ArrayList<>(scraperMetadataConfig);

      this.doSearch = true;
      this.overwriteExistingItems = true;
    }

    public MovieScrapeParams setDoSearch(boolean doSearch) {
      this.doSearch = doSearch;
      return this;
    }

    public MovieScrapeParams setOverwriteExistingItems(boolean overwriteExistingItems) {
      this.overwriteExistingItems = overwriteExistingItems;
      return this;
    }
  }
}
