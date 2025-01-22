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
package org.tinymediamanager.core.movie.entities;

import static org.tinymediamanager.core.Constants.HAS_NFO_FILE;
import static org.tinymediamanager.core.Constants.SORT_TITLE;
import static org.tinymediamanager.core.Constants.TITLE_FOR_UI;
import static org.tinymediamanager.core.Constants.TITLE_SORTABLE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieMediaFileComparator;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSetArtworkHelper;
import org.tinymediamanager.core.movie.MovieSetScraperMetadataConfig;
import org.tinymediamanager.core.movie.connector.IMovieSetConnector;
import org.tinymediamanager.core.movie.connector.MovieSetToEmbyConnector;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.ParserUtils;
import org.tinymediamanager.scraper.util.StrgUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Class MovieSet. This class is used to represent a movie set (which means a "collection" of n movies)
 * 
 * @author Manuel Laggner
 */
public class MovieSet extends MediaEntity {
  private static final Logger                LOGGER                = LoggerFactory.getLogger(MovieSet.class);
  private static final Comparator<Movie>     MOVIE_SET_COMPARATOR  = new MovieInMovieSetComparator();
  private static final Comparator<MediaFile> MEDIA_FILE_COMPARATOR = new MovieMediaFileComparator();

  @JsonProperty
  private String                             sortTitle             = "";

  @JsonProperty
  private final List<UUID>                   movieIds              = new ArrayList<>(0);
  @JsonProperty
  private final List<MovieSetMovie>          dummyMovies           = new CopyOnWriteArrayList<>();

  private final List<Movie>                  movies                = new CopyOnWriteArrayList<>();
  private String                             titleSortable         = "";

  /**
   * Instantiates a new movieset. To initialize the propertychangesupport after loading
   */
  public MovieSet() {
    // register for dirty flag listener
    super();
  }

  public MovieSet(String title) {
    this();
    setTitle(title);

    // search for artwork
    MovieSetArtworkHelper.updateArtwork(this);
  }

  @Override
  protected Comparator<MediaFile> getMediaFileComparator() {
    return MEDIA_FILE_COMPARATOR;
  }

  @Override
  public void initializeAfterLoading() {
    super.initializeAfterLoading();

    // link with movies
    for (UUID uuid : movieIds) {
      Movie movie = MovieModuleManager.getInstance().getMovieList().lookupMovie(uuid);
      if (movie != null && movie.getMovieSet() == this) {
        movies.add(movie);
      }
    }
    try {
      movies.sort(MOVIE_SET_COMPARATOR);
    }
    catch (Exception e) {
      LOGGER.debug("could not sort movies - '{}'", e.getMessage());
    }

    // rebuild the ID table the same way
    movieIds.clear();
    for (Movie movie : movies) {
      movieIds.add(movie.getDbId());
    }

    // set the movie set reference in the dummy movies
    for (MovieSetMovie movieSetMovie : dummyMovies) {
      movieSetMovie.setMovieSet(this);
    }
  }

  @Override
  public void setTitle(String newValue) {
    String oldValue = this.title;
    super.setTitle(newValue);

    firePropertyChange(TITLE_FOR_UI, oldValue, newValue);

    String oldValueTitleSortable = this.titleSortable;
    titleSortable = "";
    firePropertyChange(TITLE_SORTABLE, oldValueTitleSortable, titleSortable);

    if (!StringUtils.equals(oldValue, newValue)) {
      synchronized (movies) {
        for (Movie movie : movies) {
          movie.movieSetTitleChanged();
        }
      }
    }
  }

  /**
   * Returns the sortable variant of title<br>
   * eg "The Terminator Collection" -> "Terminator Collection, The".
   * 
   * @return the title in its sortable format
   */
  public String getTitleSortable() {
    if (StringUtils.isEmpty(titleSortable)) {
      titleSortable = Utils.getSortableName(this.getTitle());
    }
    return titleSortable;
  }

  /**
   * Gets the sort title.
   *
   * @return the sort title
   */
  public String getSortTitle() {
    return sortTitle;
  }

  /**
   * Sets the sort title.
   *
   * @param newValue
   *          the new sort title
   */
  public void setSortTitle(String newValue) {
    String oldValue = this.sortTitle;
    this.sortTitle = newValue;
    firePropertyChange(SORT_TITLE, oldValue, newValue);
  }

  /**
   * get the TMDB Id for this {@link MovieSet}
   * 
   * @return the TMDB Id or 0
   */
  public int getTmdbId() {
    int id;
    try {
      id = (Integer) ids.get(MediaMetadata.TMDB_SET);
    }
    catch (Exception e) {
      return 0;
    }
    return id;
  }

  /**
   * set the TMDB Id for this {@link MovieSet}
   * 
   * @param newValue
   *          the new TMDB Id
   */
  public void setTmdbId(int newValue) {
    int oldValue = getTmdbId();
    ids.put(MediaMetadata.TMDB_SET, newValue);
    firePropertyChange(MediaMetadata.TMDB + "Id", oldValue, newValue);
  }

  /**
   * Gets the genres.
   *
   * @return the genres
   */
  public List<MediaGenres> getGenres() {
    // just a list of all genres from the movies
    Set<MediaGenres> genres = new TreeSet<>(new MediaGenres.MediaGenresComparator());

    for (Movie movie : movies) {
      genres.addAll(movie.getGenres());
    }

    return new ArrayList<>(genres);
  }

  @Override
  public String getProductionCompany() {
    Set<String> productionCompanies = new HashSet<>();

    for (Movie movie : movies) {
      List<String> movieProductionCompanies = ParserUtils.split(movie.getProductionCompany());
      productionCompanies.addAll(movieProductionCompanies);
    }

    return String.join(", ", productionCompanies);
  }

  @Override
  public void setProductionCompany(String newValue) {
    // do nothing since we do not store that on movie set level
  }

  @Override
  public void setArtworkUrl(String url, MediaFileType type) {
    super.setArtworkUrl(url, type);
    MovieSetArtworkHelper.downloadArtwork(this, type);
  }

  /**
   * <b>PHYSICALLY</b> deletes all {@link MediaFile}s of the given type
   *
   * @param type
   *          the {@link MediaFileType} for all {@link MediaFile}s to delete
   */
  @Override
  public void deleteMediaFiles(MediaFileType type) {
    getMediaFiles(type).forEach(mediaFile -> {
      Utils.deleteFileSafely(mediaFile.getFile());
      removeFromMediaFiles(mediaFile);
    });
  }

  /**
   * Sets the artwork.
   *
   * @param artwork
   *          the artwork
   * @param config
   *          the config
   */
  public void setArtwork(List<MediaArtwork> artwork, List<MovieSetScraperMetadataConfig> config) {
    MovieSetArtworkHelper.setArtwork(this, artwork, config);
  }

  @Override
  public String getArtworkFilename(final MediaFileType type) {
    String artworkFilename = super.getArtworkFilename(type);

    // we did not find an image - get the cached file from the url
    if (StringUtils.isBlank(artworkFilename)) {
      Path cachedFile = ImageCache.getCachedFile(getArtworkUrl(type));
      if (cachedFile != null && Files.exists(cachedFile)) {
        return cachedFile.toAbsolutePath().toString();
      }
    }

    return artworkFilename;
  }

  /**
   * Inserts the movie into the right position of the list
   * 
   * @param movie
   *          the movie to insert into the movie set
   */
  public void insertMovie(Movie movie) {
    if (movie instanceof MovieSetMovie) {
      return;
    }

    synchronized (movies) {
      if (movies.contains(movie)) {
        return;
      }

      int index = Collections.binarySearch(movies, movie, MOVIE_SET_COMPARATOR);
      if (index < 0) {
        movies.add(-index - 1, movie);
        movieIds.add(-index - 1, movie.getDbId());
      }
      else {
        movies.add(index, movie);
        movieIds.add(index, movie.getDbId());
      }

      // update artwork
      MovieSetArtworkHelper.updateArtwork(this);
    }

    // write images
    MovieSetArtworkHelper.writeImagesToMovieFolder(this, Collections.singletonList(movie));

    firePropertyChange(Constants.ADDED_MOVIE, null, movie);

    // and remove the dummy for the same movie
    for (MovieSetMovie movieSetMovie : dummyMovies) {
      boolean found = false;

      if (movie.getTmdbId() > 0 && movie.getTmdbId() == movieSetMovie.getTmdbId()) {
        found = true;
      }
      if (MediaIdUtil.isValidImdbId(movie.getImdbId()) && movie.getImdbId().equals(movieSetMovie.getImdbId())) {
        found = true;
      }

      if (found) {
        firePropertyChange(Constants.REMOVED_MOVIE, null, movieSetMovie);
      }
    }
  }

  /**
   * Removes the movie from the list.
   * 
   * @param movie
   *          the movie
   * @param doCleanup
   *          do an artwork cleanup or not
   */
  public void removeMovie(Movie movie, boolean doCleanup) {
    // clean images files
    if (doCleanup) {
      MovieSetArtworkHelper.cleanMovieSetArtworkInMovieFolder(movie);
    }

    if (movie.getMovieSet() != null) {
      movie.setMovieSet(null);
      movie.saveToDb();
    }

    synchronized (movies) {
      movies.remove(movie);
      movieIds.remove(movie.getDbId());

      // update artwork
      if (doCleanup) {
        MovieSetArtworkHelper.updateArtwork(this);
      }

      saveToDb();
    }

    firePropertyChange(Constants.REMOVED_MOVIE, null, movie);

    // and mixin die missing movie
    for (MovieSetMovie movieSetMovie : dummyMovies) {
      boolean found = false;

      if (movie.getTmdbId() > 0 && movie.getTmdbId() == movieSetMovie.getTmdbId()) {
        found = true;
      }
      if (MediaIdUtil.isValidImdbId(movie.getImdbId()) && movie.getImdbId().equals(movieSetMovie.getImdbId())) {
        found = true;
      }

      if (found) {
        firePropertyChange(Constants.ADDED_MOVIE, null, movieSetMovie);
      }
    }
  }

  public List<Movie> getMovies() {
    return Collections.unmodifiableList(movies);
  }

  /**
   * build a list of <br>
   * a) available movies along with<br>
   * b) missing movies <br>
   * for display in the movie set tree
   *
   * @return a list of _all_ movies
   */
  public List<Movie> getMoviesForDisplay() {
    List<Movie> moviesForDisplay = new ArrayList<>(getMovies());

    // now mix in all missing movies
    if (MovieModuleManager.getInstance().getSettings().isDisplayMovieSetMissingMovies() && ListUtils.isNotEmpty(dummyMovies)) {
      for (MovieSetMovie movieSetMovie : dummyMovies) {
        boolean found = false;

        for (Movie movie : movies) {
          if (movie.getTmdbId() > 0 && movie.getTmdbId() == movieSetMovie.getTmdbId()) {
            found = true;
            break;
          }
          if (MediaIdUtil.isValidImdbId(movie.getImdbId()) && movie.getImdbId().equals(movieSetMovie.getImdbId())) {
            found = true;
            break;
          }
        }

        if (!found) {
          moviesForDisplay.add(movieSetMovie);
        }
      }
      moviesForDisplay.sort(MOVIE_SET_COMPARATOR);
    }

    return moviesForDisplay;
  }

  /**
   * Removes the all movies from this movie set.
   */
  public void removeAllMovies() {
    // store all old movies to remove the nodes in the tree
    List<Movie> oldValue = new ArrayList<>(movies);
    // remove images from movie folder
    synchronized (movies) {
      for (Movie movie : movies) {
        // clean images files
        MovieSetArtworkHelper.cleanMovieSetArtworkInMovieFolder(movie);

        if (movie.getMovieSet() != null) {
          movie.setMovieSet(null);
          movie.writeNFO();
          movie.saveToDb();
        }
      }
      movies.clear();
      movieIds.clear();

      // update artwork
      MovieSetArtworkHelper.updateArtwork(this);

      saveToDb();
    }

    firePropertyChange("removedAllMovies", oldValue, movies);
  }

  /**
   * toString. used for JComboBox in movie editor
   * 
   * @return the string
   */
  @Override
  public String toString() {
    return getTitle();
  }

  /**
   * gets the index of the movies from this movie set
   * 
   * @param movie
   *          the {@link Movie} to get the index of
   * @return the index
   */
  public int getMovieIndex(Movie movie) {
    return movies.indexOf(movie);
  }

  /**
   * gets the index of the movies from this movie set respecting dummies too
   * 
   * @param movie
   *          the {@link Movie} to get the index of
   * @return the index
   */
  public int getMovieIndexWithDummy(Movie movie) {
    return getMoviesForDisplay().indexOf(movie);
  }

  public void rewriteAllImages() {
    List<MediaFileType> types = Arrays.asList(MediaFileType.POSTER, MediaFileType.FANART, MediaFileType.BANNER, MediaFileType.LOGO,
        MediaFileType.CLEARLOGO, MediaFileType.CLEARART);

    for (MediaFileType type : types) {
      MovieSetArtworkHelper.downloadArtwork(this, type);
    }
  }

  public Boolean isWatched() {
    if (movies.isEmpty()) {
      return false;
    }

    for (Movie movie : movies) {
      if (!movie.isWatched()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public List<MediaFile> getImagesToCache() {
    // get files to cache
    List<MediaFile> filesToCache = new ArrayList<>();

    List<MediaFileType> types = Arrays.asList(MediaFileType.POSTER, MediaFileType.FANART, MediaFileType.BANNER, MediaFileType.LOGO,
        MediaFileType.CLEARLOGO, MediaFileType.CLEARART);

    for (MediaFileType type : types) {
      String filename = getArtworkFilename(type);
      if (StringUtils.isNotBlank(filename)) {
        filesToCache.add(new MediaFile(Paths.get(filename)));
      }
    }

    return filesToCache;
  }

  @Override
  public MediaFile getMainFile() {
    return MediaFile.EMPTY_MEDIAFILE;
  }

  @Override
  public void callbackForGatheredMediainformation(MediaFile mediaFile) {
    // nothing to do here
  }

  @Override
  public synchronized void callbackForWrittenArtwork(MediaArtworkType type) {
    // nothing to do here
  }

  @Override
  public void saveToDb() {
    MovieModuleManager.getInstance().getMovieList().persistMovieSet(this);
  }

  /**
   * Sets the metadata.
   *
   * @param metadata
   *          the new metadata
   * @param config
   *          the config
   */
  public void setMetadata(MediaMetadata metadata, List<MovieSetScraperMetadataConfig> config) {
    if (locked) {
      LOGGER.debug("movie set locked, but setMetadata has been called!");
      return;
    }

    if (metadata == null) {
      LOGGER.error("metadata was null");
      return;
    }

    // check if metadata has at least an id (aka it is not empty)
    if (metadata.getIds().isEmpty()) {
      LOGGER.warn("wanted to save empty metadata for {}", getTitle());
      return;
    }

    // populate ids

    // here we have two flavors:
    // a) we did a search, so all existing ids should be different to to new ones -> remove old ones
    // b) we did just a scrape (probably with another scraper). we should have at least one id in the movie which matches the ids from the metadata ->
    // merge ids

    // search for existing ids
    boolean matchFound = false;
    for (Map.Entry<String, Object> entry : metadata.getIds().entrySet()) {
      if (entry.getValue() != null && entry.getValue().equals(getId(entry.getKey()))) {
        matchFound = true;
        break;
      }
    }

    if (!matchFound) {
      // clear the old ids/tags to set only the new ones
      ids.clear();
    }

    setIds(metadata.getIds());

    // set chosen metadata
    if (config.contains(MovieSetScraperMetadataConfig.TITLE)) {
      // Capitalize first letter of title if setting is set!
      if (MovieModuleManager.getInstance().getSettings().getCapitalWordsInTitles()) {
        setTitle(StrgUtils.capitalize(metadata.getTitle()));
      }
      else {
        setTitle(metadata.getTitle());
      }
    }

    if (config.contains(MovieSetScraperMetadataConfig.PLOT)) {
      setPlot(metadata.getPlot());
    }

    // not available at the moment
    // if (config.contains(MovieSetScraperMetadataConfig.RATING)) {
    // Map<String, MediaRating> newRatings = new HashMap<>();
    //
    // if (matchFound) {
    // // only update new ratings, but let the old ones survive
    // newRatings.putAll(getRatings());
    // }
    //
    // for (MediaRating mediaRating : metadata.getRatings()) {
    // newRatings.put(mediaRating.getId(), mediaRating);
    // }
    //
    // setRatings(newRatings);
    // }

    // update DB
    writeNFO();
    saveToDb();

    MovieSetArtworkHelper.cleanupArtwork(this);
  }

  /**
   * Write nfo.
   */
  public void writeNFO() {
    if (MovieModuleManager.getInstance().getSettings().getMovieSetNfoFilenames().isEmpty()) {
      LOGGER.debug("Not writing any NFO file, because NFO filename preferences were empty...");
      return;
    }

    IMovieSetConnector connector;

    switch (MovieModuleManager.getInstance().getSettings().getMovieSetConnector()) {
      case EMBY:
      default:
        connector = new MovieSetToEmbyConnector(this);
    }

    try {
      connector.write(MovieModuleManager.getInstance().getSettings().getMovieSetNfoFilenames());
      firePropertyChange(HAS_NFO_FILE, false, true);
    }
    catch (Exception e) {
      LOGGER.error("could not write NFO file - '{}'", e.getMessage());
    }
  }

  public void setDummyMovies(List<MovieSetMovie> dummyMovies) {
    this.dummyMovies.clear();
    dummyMovies.forEach(dummy -> {
      dummy.setMovieSet(this);
      this.dummyMovies.add(dummy);
    });

    firePropertyChange("dummyMovies", null, dummyMovies);
  }

  public List<MovieSetMovie> getDummyMovies() {
    return dummyMovies;
  }

  @Override
  public Date getReleaseDate() {
    Date firstReleaseDate = null;

    for (Movie movie : getMoviesForDisplay()) {
      if (firstReleaseDate == null) {
        firstReleaseDate = movie.getReleaseDate();
      }
      else if (movie.getReleaseDate() != null && firstReleaseDate.after(movie.getReleaseDate())) {
        firstReleaseDate = movie.getReleaseDate();
      }
    }

    return firstReleaseDate;
  }

  @Override
  public MediaRating getRating() {
    return MediaMetadata.EMPTY_RATING;
  }

  public String getYears() {
    List<Integer> years = new ArrayList<>();

    movies.forEach(movie -> years.add(movie.getYear()));
    dummyMovies.forEach(dummy -> years.add(dummy.getYear()));

    Collections.sort(years);
    if (!years.isEmpty() && years.size() >= 2) {
      if (years.get(0).equals(years.get(years.size() - 1))) {
        return String.valueOf(years.get(0));
      }
      else {
        return years.get(0) + " - " + (years.get(years.size() - 1));
      }
    }
    else if (years.size() == 1) {
      return String.valueOf(years.get(0));
    }

    return "";
  }

  public Object getValueForMetadata(MovieSetScraperMetadataConfig metadataConfig) {
    return switch (metadataConfig) {
      case ID -> getIds();
      case TITLE -> getTitle();
      case PLOT -> getPlot();
      case RATING -> getRatings();
      case POSTER -> getMediaFiles(MediaFileType.POSTER);
      case FANART -> getMediaFiles(MediaFileType.FANART);
      case BANNER -> getMediaFiles(MediaFileType.BANNER);
      case CLEARART -> getMediaFiles(MediaFileType.CLEARART);
      case THUMB -> getMediaFiles(MediaFileType.THUMB);
      case LOGO -> getMediaFiles(MediaFileType.LOGO);
      case CLEARLOGO -> getMediaFiles(MediaFileType.CLEARLOGO);
      case DISCART -> getMediaFiles(MediaFileType.DISC);
    };

  }

  /*******************************************************************************
   * helper classses
   *******************************************************************************/
  private static class MovieInMovieSetComparator implements Comparator<Movie> {
    private static final Comparator<Date> DATE_COMPARATOR = Comparator.nullsLast(Date::compareTo);

    @Override
    public int compare(Movie o1, Movie o2) {
      if (o1 == null || o2 == null) {
        return 0;
      }

      // sort with year if available
      int result = 0;
      if (o1.getYear() > 0 && o2.getYear() > 0) {
        result = o1.getYear() - o2.getYear();
      }
      if (result != 0) {
        return result;
      }

      // sort with release date if available
      if (o1.getReleaseDate() != null && o2.getReleaseDate() != null) {
        result = DATE_COMPARATOR.compare(o1.getReleaseDate(), o2.getReleaseDate());
        if (result != 0) {
          return result;
        }
      }

      // fallback: sort via title
      return o1.getTitleForUi().compareTo(o2.getTitleForUi());
    }
  }

  /**
   * the class {@link MovieSetMovie} is used to indicate that this is a missing movie in this movie set
   */
  public static class MovieSetMovie extends Movie {
    @Override
    public void writeNFO() {
      // do nothing here
    }

    @Override
    public void saveToDb() {
      // do nothing here
    }

    @Override
    protected void postProcess(List<MovieScraperMetadataConfig> config, boolean overwriteExistingItems) {
      // no postprocessing needed
    }

    @Override
    protected List<MediaFile> listActorFiles() {
      return Collections.emptyList();
    }

    @Override
    public MediaFile getMainVideoFile() {
      // per se no video file here
      return MediaFile.EMPTY_MEDIAFILE;
    }

    @Override
    public void writeActorImages(boolean overwriteExistingItems) {
      // do nothing here
    }

    @Override
    public void setMetadata(MediaMetadata metadata, List<MovieScraperMetadataConfig> config, boolean overwriteExistingItems) {
      // do not set movie set assignment since that could create new movie sets in the DB
      List<MovieScraperMetadataConfig> newConfig = new ArrayList<>(config);
      newConfig.remove(MovieScraperMetadataConfig.COLLECTION);
      super.setMetadata(metadata, newConfig, overwriteExistingItems);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      return title.equals(((MovieSetMovie) o).getTitle());
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder().append(title).build();
    }
  }
}
