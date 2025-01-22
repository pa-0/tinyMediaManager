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
package org.tinymediamanager.ui.moviesets;

import static org.tinymediamanager.scraper.util.MediaIdUtil.getMovieImdbIdViaTmdbId;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.observablecollections.ObservableCollections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSetScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSetSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.interfaces.IMovieSetMetadataProvider;
import org.tinymediamanager.scraper.util.MediaIdUtil;

/**
 * The Class MovieSetChooserModel.
 */
public class MovieSetChooserModel extends AbstractModelObject {

  private static final Logger                LOGGER         = LoggerFactory.getLogger(MovieSetChooserModel.class);
  public static final MovieSetChooserModel   EMPTY_RESULT   = new MovieSetChooserModel();

  private final List<MovieInSet>             movies         = ObservableCollections.observableList(new ArrayList<>());
  private final List<MovieSet.MovieSetMovie> movieSetMovies = new ArrayList<>();

  private MediaScraper                       scraper;
  private MediaSearchResult                  result         = null;
  private MediaMetadata                      metadata       = null;
  private String                             name           = "";
  private String                             posterUrl      = "";
  private String                             fanartUrl      = "";
  private String                             overview       = "";
  private int                                tmdbId         = 0;
  private boolean                            scraped;

  public MovieSetChooserModel(MediaSearchResult result) {
    this.result = result;

    setName(result.getTitle());
    setTmdbId(result.getIdAsInt(result.getProviderId()));
    setPosterUrl(result.getPosterUrl());

    try {
      List<MediaScraper> sets = MediaScraper.getMediaScrapers(ScraperType.MOVIE_SET);
      if (!sets.isEmpty()) {
        scraper = sets.get(0); // just get first
      }
    }
    catch (Exception e) {
      scraper = null;
    }
  }

  /**
   * create the empty search result.
   */
  private MovieSetChooserModel() {
    setName(TmmResourceBundle.getString("chooser.nothingfound"));
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
    firePropertyChange("name", "", name);
  }

  public void setOverview(String overview) {
    this.overview = overview;
    firePropertyChange("overview", "", overview);
  }

  public int getTmdbId() {
    return tmdbId;
  }

  public void setTmdbId(int tmdbId) {
    this.tmdbId = tmdbId;
  }

  public void setPosterUrl(String posterUrl) {
    this.posterUrl = posterUrl;
    firePropertyChange("posterUrl", "", posterUrl);
  }

  public void setFanartUrl(String fanartUrl) {
    this.fanartUrl = fanartUrl;
    firePropertyChange("fanartUrl", "", fanartUrl);
  }

  public boolean isScraped() {
    return scraped;
  }

  public String getPosterUrl() {
    return posterUrl;
  }

  public String getFanartUrl() {
    return fanartUrl;
  }

  /**
   * Match with existing movies.
   */
  public void matchWithExistingMovies() {
    List<Movie> moviesFromMovieList = MovieModuleManager.getInstance().getMovieList().getMovies();
    for (MovieInSet mis : movies) {
      // try to match via tmdbid
      if (mis.tmdbId > 0) {
        for (Movie movie : moviesFromMovieList) {
          if (movie.getTmdbId() == mis.tmdbId) {
            mis.setMovie(movie);
            break;
          }
        }
      }

      // try to match via imdbid if nothing has been found
      if (mis.getMovie() == null) {
        if (!MediaIdUtil.isValidImdbId(mis.imdbId) && mis.tmdbId > 0) {
          // get imdbid for this tmdbid
          String imdbId = getMovieImdbIdViaTmdbId(mis.tmdbId);
          if (MediaIdUtil.isValidImdbId(imdbId)) {
            mis.imdbId = imdbId;
          }
        }

        if (MediaIdUtil.isValidImdbId(mis.imdbId)) {
          for (Movie movie : moviesFromMovieList) {
            if (mis.imdbId.equals(movie.getImdbId())) {
              mis.setMovie(movie);
              break;
            }
          }
        }
      }
    }
  }

  /**
   * Scrape metadata.
   */
  public void scrapeMetadata() {
    try {
      if (scraper.getMediaProvider() != null) {
        MovieSetSearchAndScrapeOptions options = new MovieSetSearchAndScrapeOptions();
        options.setSearchResult(result);
        options.setTmdbId(result.getIdAsInt(result.getProviderId()));
        options.setLanguage(MovieModuleManager.getInstance().getSettings().getScraperLanguage());

        MediaMetadata info;

        try {
          info = ((IMovieSetMetadataProvider) scraper.getMediaProvider()).getMetadata(options);
        }
        catch (MissingIdException e) {
          LOGGER.warn("missing id for scrape");
          MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "MovieSetChooser", "scraper.error.missingid"));
          return;
        }
        catch (ScrapeException e) {
          LOGGER.error("getMetadata", e);
          MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "MovieSetChooser", "message.scrape.metadatamoviesetfailed",
              new String[] { ":", e.getLocalizedMessage() }));
          return;
        }

        if (info != null) {
          this.metadata = info;
          if (!info.getMediaArt(MediaArtworkType.BACKGROUND).isEmpty()) {
            setFanartUrl(info.getMediaArt(MediaArtworkType.BACKGROUND).get(0).getOriginalUrl());
          }

          setName(info.getTitle());
          setOverview(info.getPlot());

          for (MediaMetadata item : info.getSubItems()) {
            MovieInSet movie = new MovieInSet(item.getTitle());
            movie.setYear(item.getYear());
            try {
              movie.setTmdbId(Integer.parseInt(item.getId(MediaMetadata.TMDB).toString()));
            }
            catch (NumberFormatException ignored) {
            }
            if (item.getReleaseDate() != null) {
              movie.setReleaseDate(new SimpleDateFormat("yyyy-MM-dd").format(item.getReleaseDate()));
            }
            movies.add(movie);

            // mix in the dummy movie
            MovieSet.MovieSetMovie movieSetMovie = new MovieSet.MovieSetMovie();
            movieSetMovie.setMetadata(item, new ArrayList<>(Arrays.asList(MovieScraperMetadataConfig.values())), true);
            movieSetMovie.setLastScraperId(scraper.getMediaProvider().getId());
            movieSetMovie.setLastScrapeLanguage(options.getLanguage().name());

            // POSTER
            if (!item.getMediaArt(MediaArtworkType.POSTER).isEmpty()) {
              movieSetMovie.setArtworkUrl(item.getMediaArt(MediaArtworkType.POSTER).get(0).getOriginalUrl(), MediaFileType.POSTER);
            }

            // FANART
            if (!item.getMediaArt(MediaArtworkType.BACKGROUND).isEmpty()) {
              movieSetMovie.setArtworkUrl(item.getMediaArt(MediaArtworkType.BACKGROUND).get(0).getOriginalUrl(), MediaFileType.FANART);
            }

            movieSetMovies.add(movieSetMovie);
          }

          Collections.sort(movies);

          // try to match movies
          matchWithExistingMovies();

          this.scraped = true;
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("scrapeMedia", e);
      MessageManager.instance.pushMessage(
          new Message(Message.MessageLevel.ERROR, "MovieSetChooser", "message.scrape.threadcrashed", new String[] { ":", e.getLocalizedMessage() }));
    }
  }

  public String getOverview() {
    return overview;
  }

  public List<MovieInSet> getMovies() {
    return movies;
  }

  public List<MovieSet.MovieSetMovie> getMovieSetMovies() {
    return movieSetMovies;
  }

  public void startArtworkScrapeTask(MovieSet movieSet, List<MovieSetScraperMetadataConfig> config) {
    TmmTaskManager.getInstance().addUnnamedTask(new ArtworkScrapeTask(movieSet, config));
  }

  private class ArtworkScrapeTask extends TmmTask {
    private final MovieSet                            movieSetToScrape;
    private final List<MovieSetScraperMetadataConfig> config;

    public ArtworkScrapeTask(MovieSet movieSet, List<MovieSetScraperMetadataConfig> config) {
      super(TmmResourceBundle.getString("message.scrape.artwork") + " " + movieSet.getTitle(), 0, TaskType.BACKGROUND_TASK);
      this.movieSetToScrape = movieSet;
      this.config = config;
    }

    @Override
    protected void doInBackground() {
      if (!scraped) {
        return;
      }

      List<MediaArtwork> artwork = new ArrayList<>();

      ArtworkSearchAndScrapeOptions options = new ArtworkSearchAndScrapeOptions(MediaType.MOVIE_SET);
      options.setArtworkType(MediaArtwork.MediaArtworkType.ALL);
      options.setMetadata(metadata);
      options.setId(MediaMetadata.IMDB, String.valueOf(metadata.getId(MediaMetadata.IMDB)));
      try {
        options.setTmdbId(Integer.parseInt(String.valueOf(metadata.getId(MediaMetadata.TMDB_SET))));
      }
      catch (Exception e) {
        options.setTmdbId(0);
      }
      options.setLanguage(MovieModuleManager.getInstance().getSettings().getDefaultImageScraperLanguage());
      options.setFanartSize(MovieModuleManager.getInstance().getSettings().getImageFanartSize());
      options.setPosterSize(MovieModuleManager.getInstance().getSettings().getImagePosterSize());

      // scrape providers till one artwork has been found
      for (MediaScraper artworkScraper : MovieModuleManager.getInstance().getMovieList().getDefaultArtworkScrapers()) {
        IMovieArtworkProvider artworkProvider = (IMovieArtworkProvider) artworkScraper.getMediaProvider();
        try {
          artwork.addAll(artworkProvider.getArtwork(options));
        }
        catch (MissingIdException e) {
          LOGGER.debug("could not get artwork: {}", e.getMessage());
        }
        catch (ScrapeException e) {
          LOGGER.error("getArtwork", e);
          MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, movieSetToScrape, "message.scrape.movieartworkfailed",
              new String[] { ":", e.getLocalizedMessage() }));
        }
      }

      // at last take the poster from the result
      if (StringUtils.isNotBlank(getPosterUrl())) {
        MediaArtwork ma = new MediaArtwork(result.getProviderId(), MediaArtwork.MediaArtworkType.POSTER);
        ma.setOriginalUrl(getPosterUrl());
        ma.setPreviewUrl(getPosterUrl());
        ma.addImageSize(0, 0, getPosterUrl(), 0);
        artwork.add(ma);
      }

      movieSetToScrape.setArtwork(artwork, config);
    }
  }

  public MediaMetadata getMetadata() {
    return metadata;
  }

  public static class MovieInSet extends AbstractModelObject implements Comparable<MovieInSet> {
    private final String name;

    private int          tmdbId      = 0;
    private String       imdbId      = "";
    private String       releaseDate = "";
    private int          year        = 0;
    private Movie        movie       = null;

    public MovieInSet(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public int getYear() {
      return year;
    }

    public int getTmdbId() {
      return tmdbId;
    }

    public String getImdbId() {
      return imdbId;
    }

    public String getReleaseDate() {
      return releaseDate;
    }

    public Movie getMovie() {
      return movie;
    }

    public void setYear(int year) {
      this.year = year;
    }

    public void setTmdbId(int tmdbId) {
      this.tmdbId = tmdbId;
    }

    public void setImdbId(String imdbId) {
      this.imdbId = imdbId;
    }

    public void setReleaseDate(String releaseDate) {
      this.releaseDate = releaseDate;
    }

    public void setMovie(Movie movie) {
      this.movie = movie;
      firePropertyChange("movie", null, movie);
    }

    @Override
    public int compareTo(MovieInSet o) {
      return releaseDate.compareTo(o.releaseDate);
    }
  }
}
