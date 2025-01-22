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

import static org.tinymediamanager.core.Constants.ACTORS;
import static org.tinymediamanager.core.Constants.ACTORS_AS_STRING;
import static org.tinymediamanager.core.Constants.CERTIFICATION;
import static org.tinymediamanager.core.Constants.COUNTRY;
import static org.tinymediamanager.core.Constants.DIRECTORS;
import static org.tinymediamanager.core.Constants.DIRECTORS_AS_STRING;
import static org.tinymediamanager.core.Constants.EDITION;
import static org.tinymediamanager.core.Constants.EDITION_AS_STRING;
import static org.tinymediamanager.core.Constants.GENRE;
import static org.tinymediamanager.core.Constants.GENRES_AS_STRING;
import static org.tinymediamanager.core.Constants.HAS_NFO_FILE;
import static org.tinymediamanager.core.Constants.MEDIA_SOURCE;
import static org.tinymediamanager.core.Constants.MOVIESET;
import static org.tinymediamanager.core.Constants.MOVIESET_TITLE;
import static org.tinymediamanager.core.Constants.PRODUCERS;
import static org.tinymediamanager.core.Constants.PRODUCERS_AS_STRING;
import static org.tinymediamanager.core.Constants.RELEASE_DATE;
import static org.tinymediamanager.core.Constants.RELEASE_DATE_AS_STRING;
import static org.tinymediamanager.core.Constants.RUNTIME;
import static org.tinymediamanager.core.Constants.SORT_TITLE;
import static org.tinymediamanager.core.Constants.SPOKEN_LANGUAGES;
import static org.tinymediamanager.core.Constants.TITLE_FOR_UI;
import static org.tinymediamanager.core.Constants.TITLE_SORTABLE;
import static org.tinymediamanager.core.Constants.TOP250;
import static org.tinymediamanager.core.Constants.TRAILER;
import static org.tinymediamanager.core.Constants.VIDEO_IN_3D;
import static org.tinymediamanager.core.Constants.WATCHED;
import static org.tinymediamanager.core.Constants.WRITERS;
import static org.tinymediamanager.core.Constants.WRITERS_AS_STRING;
import static org.tinymediamanager.core.Utils.returnOneWhenFilled;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.IMediaInformation;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.TrailerQuality;
import org.tinymediamanager.core.TrailerSources;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieArtworkHelper;
import org.tinymediamanager.core.movie.MovieEdition;
import org.tinymediamanager.core.movie.MovieMediaFileComparator;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSetScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSetSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.connector.IMovieConnector;
import org.tinymediamanager.core.movie.connector.MovieConnectors;
import org.tinymediamanager.core.movie.connector.MovieToEmbyConnector;
import org.tinymediamanager.core.movie.connector.MovieToJellyfinConnector;
import org.tinymediamanager.core.movie.connector.MovieToKodiConnector;
import org.tinymediamanager.core.movie.connector.MovieToMpLegacyConnector;
import org.tinymediamanager.core.movie.connector.MovieToMpMovingPicturesConnector;
import org.tinymediamanager.core.movie.connector.MovieToMpMyVideoConnector;
import org.tinymediamanager.core.movie.connector.MovieToXbmcConnector;
import org.tinymediamanager.core.movie.filenaming.MovieNfoNaming;
import org.tinymediamanager.core.movie.filenaming.MovieTrailerNaming;
import org.tinymediamanager.core.movie.tasks.MovieActorImageFetcherTask;
import org.tinymediamanager.core.movie.tasks.MovieRenameTask;
import org.tinymediamanager.core.movie.tasks.MovieSetScrapeTask;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskChain;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.ParserUtils;
import org.tinymediamanager.scraper.util.StrgUtils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * The main class for movies.
 * 
 * @author Manuel Laggner / Myron Boyle
 */
public class Movie extends MediaEntity implements IMediaInformation {
  private static final Logger                   LOGGER                     = LoggerFactory.getLogger(Movie.class);
  private static final Comparator<MediaFile>    MEDIA_FILE_COMPARATOR      = new MovieMediaFileComparator();
  private static final Comparator<MediaTrailer> TRAILER_QUALITY_COMPARATOR = new MediaTrailer.QualityComparator();

  @JsonProperty
  private String                                sortTitle                  = "";
  @JsonProperty
  private String                                tagline                    = "";
  @JsonProperty
  private int                                   runtime                    = 0;
  @JsonProperty
  private boolean                               watched                    = false;
  @JsonProperty
  private int                                   playcount                  = 0;
  @JsonProperty
  private boolean                               isDisc                     = false;
  @JsonProperty
  private String                                spokenLanguages            = "";
  @JsonProperty
  private String                                country                    = "";
  @JsonProperty
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private Date                                  releaseDate                = null;
  @JsonProperty
  private boolean                               multiMovieDir              = false;                               // we detected more movies in
                                                                                                                  // same folder
  @JsonProperty
  private int                                   top250                     = 0;
  @JsonProperty
  private MediaSource                           mediaSource                = MediaSource.UNKNOWN;                 // DVD, Bluray, etc
  @JsonProperty
  private boolean                               videoIn3D                  = false;
  @JsonProperty
  private MediaCertification                    certification              = MediaCertification.UNKNOWN;
  @JsonProperty
  private UUID                                  movieSetId;
  @JsonProperty
  private MovieEdition                          edition                    = MovieEdition.NONE;
  @JsonProperty
  private boolean                               stacked                    = false;
  @JsonProperty
  private boolean                               offline                    = false;

  @JsonProperty
  private final List<MediaGenres>               genres                     = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<String>                    extraThumbs                = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<String>                    extraFanarts               = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<Person>                    actors                     = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<Person>                    producers                  = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<Person>                    directors                  = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<Person>                    writers                    = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<MediaTrailer>              trailer                    = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<String>                    showlinks                  = new CopyOnWriteArrayList<>();

  private MovieSet                              movieSet;
  private String                                titleSortable              = "";
  private String                                originalTitleSortable      = "";
  private String                                otherIds                   = "";
  private Date                                  lastWatched                = null;
  private String                                localizedSpokenLanguages   = "";

  /**
   * Instantiates a new movie. To initialize the propertychangesupport after loading
   */
  public Movie() {
    // register for dirty flag listener
    super();
  }

  /**
   * Overwrites all null/empty elements with "other" value (but might be also empty)<br>
   * For lists, check with 'contains' and add.<br>
   * Do NOT merge path, dateAdded, scraped, mediaFiles and other crucial properties!
   * 
   * @param other
   *          the movie to merge in
   */
  public void merge(Movie other) {
    merge(other, false);
  }

  /**
   * Overwrites all elements with "other" value<br>
   * Do NOT merge path, dateAdded, scraped, mediaFiles and other crucial properties!
   *
   * @param other
   *          the movie to merge in
   */
  public void forceMerge(Movie other) {
    merge(other, true);
  }

  void merge(Movie other, boolean force) {
    if (locked || other == null) {
      return;
    }
    super.merge(other, force);

    setSortTitle(StringUtils.isEmpty(sortTitle) || force ? other.sortTitle : sortTitle);
    setTagline(StringUtils.isEmpty(tagline) || force ? other.tagline : tagline);
    setSpokenLanguages(StringUtils.isEmpty(spokenLanguages) || force ? other.spokenLanguages : spokenLanguages);
    setCountry(StringUtils.isEmpty(country) || force ? other.country : country);
    setWatched(!watched || force ? other.watched : watched);
    setPlaycount(playcount == 0 || force ? other.playcount : playcount);
    setRuntime(runtime == 0 || force ? other.runtime : runtime);
    setTop250(top250 == 0 || force ? other.top250 : top250);
    setReleaseDate(releaseDate == null || force ? other.releaseDate : releaseDate);
    setMovieSet(movieSet == null || force ? other.movieSet : movieSet);
    setMediaSource(mediaSource == MediaSource.UNKNOWN || force ? other.mediaSource : mediaSource);
    setCertification(certification == MediaCertification.UNKNOWN || force ? other.certification : certification);
    setEdition(edition == MovieEdition.NONE || force ? other.edition : edition);

    // when force is set, clear the lists/maps and add all other values
    if (force) {
      genres.clear();
      actors.clear();
      producers.clear();
      directors.clear();
      writers.clear();
      trailer.clear();
      extraFanarts.clear();
      extraThumbs.clear();
    }

    setGenres(other.genres);
    setActors(other.actors);
    setProducers(other.producers);
    setDirectors(other.directors);
    setWriters(other.writers);
    setShowlinks(other.showlinks);
    setExtraFanarts(other.extraFanarts);
    setExtraThumbs(other.extraThumbs);

    List<MediaTrailer> mergedTrailers = new ArrayList<>(trailer);
    ListUtils.mergeLists(mergedTrailers, other.trailer);
    setTrailers(mergedTrailers);
  }

  @Override
  protected Comparator<MediaFile> getMediaFileComparator() {
    return MEDIA_FILE_COMPARATOR;
  }

  @Override
  public void setId(String key, Object value) {
    super.setId(key, value);

    otherIds = "";
    firePropertyChange("otherIds", null, key + ":" + value);
  }

  public String getOtherIds() {
    if (StringUtils.isNotBlank(otherIds)) {
      return otherIds;
    }

    for (Map.Entry<String, Object> entry : getIds().entrySet()) {
      switch (entry.getKey()) {
        case MediaMetadata.IMDB:
        case MediaMetadata.TMDB:
        case MediaMetadata.TRAKT_TV:
          // already in UI - skip
          continue;

        case "tmdbId":
        case "imdbId":
        case "traktId":
          // legacy format
          continue;

        case MediaMetadata.TMDB_SET:
          // not needed
          continue;

        default:
          if (StringUtils.isNotBlank(otherIds)) {
            otherIds += "; ";
          }
          otherIds += entry.getKey() + ": " + entry.getValue();
      }
    }

    return otherIds;
  }

  @Override
  public void addToMediaFiles(MediaFile mediaFile) {
    super.addToMediaFiles(mediaFile);

    // if we have added a trailer, also update the trailer list
    if (mediaFile.getType() == MediaFileType.TRAILER) {
      mixinLocalTrailers();
    }
  }

  @Override
  protected float calculateScrapeScore() {
    float score = super.calculateScrapeScore();

    score = score + returnOneWhenFilled(tagline);
    score = score + returnOneWhenFilled(spokenLanguages);
    score = score + returnOneWhenFilled(country);
    score = score + returnOneWhenFilled(top250);
    score = score + returnOneWhenFilled(runtime);
    score = score + returnOneWhenFilled(releaseDate);
    if (certification != MediaCertification.UNKNOWN) {
      score = score + 1;
    }

    score = score + returnOneWhenFilled(actors);
    score = score + returnOneWhenFilled(directors);
    score = score + returnOneWhenFilled(writers);
    score = score + returnOneWhenFilled(producers);
    score = score + returnOneWhenFilled(trailer);

    return score;
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
   * Returns the sortable variant of title<br>
   * eg "The Bourne Legacy" -> "Bourne Legacy, The".
   * 
   * @return the title in its sortable format
   */
  public String getTitleSortable() {
    if (StringUtils.isBlank(titleSortable)) {
      titleSortable = Utils.getSortableName(getTitle());
    }
    return titleSortable;
  }

  /**
   * Returns the sortable variant of the original title<br>
   * eg "The Bourne Legacy" -> "Bourne Legacy, The".
   *
   * @return the original title in its sortable format
   */
  public String getOriginalTitleSortable() {
    if (StringUtils.isBlank(originalTitleSortable)) {
      originalTitleSortable = Utils.getSortableName(getOriginalTitle());
    }
    return originalTitleSortable;
  }

  public void clearTitleSortable() {
    titleSortable = "";
    originalTitleSortable = "";
  }

  /**
   * Gets the checks for nfo file.
   * 
   * @return the checks for nfo file
   */
  public Boolean getHasNfoFile() {
    List<MediaFile> mf = getMediaFiles(MediaFileType.NFO);

    return mf != null && !mf.isEmpty();
  }

  /**
   * do we have basic metadata filled?<br>
   * If you want to have the configurable check, use {@link org.tinymediamanager.core.movie.MovieList}.detectMissingMetadata()
   *
   * @return true/false
   */
  @Deprecated // is still used in some export templates
  public Boolean getHasMetadata() {
    return !plot.isEmpty() && year != 0;
  }

  /**
   * do we have basic images? Poster and Fanart is checked.<br>
   * If you want to have the configurable check, use {@link org.tinymediamanager.core.movie.MovieList}.detectMissingArtwork()
   *
   * @return the checks for images
   */
  @Deprecated // is still used in some export templates
  public Boolean getHasImages() {
    for (MediaArtworkType type : Arrays.asList(MediaArtworkType.POSTER, MediaArtworkType.BACKGROUND)) {
      if (StringUtils.isEmpty(getArtworkFilename(MediaFileType.getMediaFileType(type)))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Gets the checks for trailer.
   * 
   * @return the checks for trailer
   */
  public Boolean getHasTrailer() {
    // check if there is a mediafile (trailer)
    return !getMediaFiles(MediaFileType.TRAILER).isEmpty();
  }

  /**
   * Check if Movie has a note
   *
   * @return the check for the note
   */
  public Boolean getHasNote() {
    return StringUtils.isNotBlank(note);
  }

  /**
   * Gets the title for ui.
   * 
   * @return the title for ui
   */
  public String getTitleForUi() {
    String titleForUi = title;
    if (year > 0) {
      titleForUi += " (" + year + ")";
    }
    return titleForUi;
  }

  /**
   * Initialize after loading.
   */
  @Override
  public void initializeAfterLoading() {
    super.initializeAfterLoading();

    // link with movie set
    if (movieSetId != null) {
      movieSet = MovieModuleManager.getInstance().getMovieList().lookupMovieSet(movieSetId);
    }
  }

  /**
   * Gets the trailers
   * 
   * @return the trailers
   */
  public List<MediaTrailer> getTrailer() {
    return this.trailer;
  }

  /**
   * Adds the trailer.
   * 
   * @param newTrailers
   *          a {@link Collection} of trailers to be added
   */
  public void addToTrailer(Collection<MediaTrailer> newTrailers) {
    Set<MediaTrailer> newItems = new LinkedHashSet<>();

    // do not accept duplicates or null values
    for (MediaTrailer trailer : ListUtils.nullSafe(newTrailers)) {
      if (trailer == null || this.trailer.contains(trailer)) {
        continue;
      }
      newItems.add(trailer);
    }

    if (newItems.isEmpty()) {
      return;
    }

    trailer.addAll(newItems);
    firePropertyChange(TRAILER, null, trailer);
  }

  /**
   * Removes the all trailers.
   */
  public void removeAllTrailers() {
    trailer.clear();
    firePropertyChange(TRAILER, null, trailer);
  }

  /**
   * get all associated TV show names (showlinks)
   * 
   * @return a {@link List} of all associated TV show names
   */
  public List<String> getShowlinks() {
    return showlinks;
  }

  /**
   * set the associated TV show names (showlinks)
   * 
   * @param newShowlinks
   *          a {@link List} of all associated TV show names to set
   */
  public void setShowlinks(List<String> newShowlinks) {
    ListUtils.mergeLists(showlinks, newShowlinks);
    Utils.removeEmptyStringsFromList(showlinks);

    firePropertyChange("showlinks", null, showlinks);
    firePropertyChange("showlinksAsString", null, showlinks);
  }

  /**
   * adds the given showlinks (TV show name) to this movie
   * 
   * @param newShowlinks
   *          a {@link Collection} of TV show names
   */
  public void addShowlinks(Collection<String> newShowlinks) {
    Set<String> newItems = new LinkedHashSet<>(1);

    // do not accept duplicates or null values
    for (String showlink : ListUtils.nullSafe(newShowlinks)) {
      if (StringUtils.isBlank(showlink) || showlinks.contains(showlink)) {
        continue;
      }
      newItems.add(showlink);
    }

    if (newItems.isEmpty()) {
      return;
    }

    showlinks.addAll(newItems);
    firePropertyChange("newShowlinks", null, newShowlinks);
    firePropertyChange("showlinksAsString", null, newShowlinks);
  }

  /**
   * remove the given showlink (TV show name)
   * 
   * @param showlink
   *          the TV show name
   */
  public void removeShowlink(String showlink) {
    if (showlinks.remove(showlink)) {
      firePropertyChange("showlinks", null, showlinks);
      firePropertyChange("showlinksAsString", null, showlinks);
    }
  }

  public String getShowlinksAsString() {
    return String.join(", ", showlinks);
  }

  /** has movie local (or any mediafile inline) subtitles? */
  public boolean getHasSubtitles() {
    if (!getMediaFiles(MediaFileType.SUBTITLE).isEmpty()) {
      return true;
    }

    for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO, MediaFileType.AUDIO)) {
      if (mf.hasSubtitles()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Gets the imdb id.
   * 
   * @return the imdb id
   */
  public String getImdbId() {
    return this.getIdAsString(MediaMetadata.IMDB);
  }

  /**
   * Gets the tmdb id.
   * 
   * @return the tmdb id
   */
  public int getTmdbId() {
    return this.getIdAsInt(MediaMetadata.TMDB);
  }

  /**
   * Sets the tmdb id.
   * 
   * @param newValue
   *          the new tmdb id
   */
  public void setTmdbId(int newValue) {
    this.setId(MediaMetadata.TMDB, newValue);
  }

  /**
   * Gets the trakt.tv id.
   *
   * @return the trakt.tv id
   */
  public int getTraktId() {
    return this.getIdAsInt(MediaMetadata.TRAKT_TV);
  }

  /**
   * Sets the trakt.tv id.
   *
   * @param newValue
   *          the new trakt.tv id
   */
  public void setTraktId(int newValue) {
    this.setId(MediaMetadata.TRAKT_TV, newValue);
  }

  /**
   * Gets the runtime in minutes
   * 
   * @return the runtime
   */
  public int getRuntime() {
    int runtimeFromMi = getRuntimeFromMediaFilesInMinutes();
    if (MovieModuleManager.getInstance().getSettings().isRuntimeFromMediaInfo() && runtimeFromMi > 0) {
      return runtimeFromMi;
    }
    return runtime == 0 ? runtimeFromMi : runtime;
  }

  /**
   * Gets the tagline.
   * 
   * @return the tagline
   */
  public String getTagline() {
    return tagline;
  }

  /**
   * Checks for file.
   * 
   * @param filename
   *          the filename
   * @return true, if successful
   */
  public boolean hasFile(String filename) {
    if (StringUtils.isEmpty(filename)) {
      return false;
    }

    for (MediaFile file : new ArrayList<>(getMediaFiles())) {
      if (filename.compareTo(file.getFilename()) == 0) {
        return true;
      }
    }

    return false;
  }

  /**
   * Gets the extra thumbs.
   * 
   * @return the extra thumbs
   */
  public List<String> getExtraThumbs() {
    return extraThumbs;
  }

  /**
   * Sets the extra thumbs.
   * 
   * @param extraThumbs
   *          the new extra thumbs
   */
  @JsonSetter
  public void setExtraThumbs(List<String> extraThumbs) {
    this.extraThumbs.clear();
    this.extraThumbs.addAll(extraThumbs);
    firePropertyChange("extraThumbs", null, this.extraThumbs);
  }

  /**
   * Gets the extra fanarts.
   * 
   * @return the extra fanarts
   */
  public List<String> getExtraFanarts() {
    return extraFanarts;
  }

  /**
   * Sets the extra fanarts.
   * 
   * @param extraFanarts
   *          the new extra fanarts
   */
  @JsonSetter
  public void setExtraFanarts(List<String> extraFanarts) {
    this.extraFanarts.clear();
    this.extraFanarts.addAll(extraFanarts);
    firePropertyChange("extraFanarts", null, this.extraFanarts);
  }

  /**
   * Sets the imdb id.
   * 
   * @param newValue
   *          the new imdb id
   */
  public void setImdbId(String newValue) {
    this.setId(MediaMetadata.IMDB, newValue);
  }

  /**
   * Sets the metadata.
   * 
   * @param metadata
   *          the new metadata
   * @param config
   *          the config
   */
  public void setMetadata(MediaMetadata metadata, List<MovieScraperMetadataConfig> config, boolean overwriteExistingItems) {
    if (locked) {
      LOGGER.debug("movie locked, but setMetadata has been called!");
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

    MovieSettings settings = MovieModuleManager.getInstance().getSettings();

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

    // when there are no ids, we must assume that this is the first scrape of the movie - treat it like a matched one (to prevent existing/manually
    // entered data from being overwritten)
    boolean newEntity = false;
    if (ids.isEmpty()) {
      matchFound = true;
      newEntity = true;
    }

    if (!matchFound && overwriteExistingItems) {
      // clear the old ids/tags to set only the new ones
      ids.clear();
    }

    if (overwriteExistingItems) {
      setIds(metadata.getIds());
    }
    else {
      for (Map.Entry<String, Object> entry : metadata.getIds().entrySet()) {
        if (!ids.containsKey(entry.getKey())) {
          setId(entry.getKey(), entry.getValue());
        }
      }
    }

    // set chosen metadata
    if (config.contains(MovieScraperMetadataConfig.TITLE) && StringUtils.isNotBlank(metadata.getTitle())
        && (overwriteExistingItems || newEntity || StringUtils.isBlank(getTitle()))) {
      // Capitalize first letter of title if setting is set!
      if (settings.getCapitalWordsInTitles()) {
        setTitle(StrgUtils.capitalize(metadata.getTitle()));
      }
      else {
        setTitle(metadata.getTitle());
      }
    }

    if (config.contains(MovieScraperMetadataConfig.ORIGINAL_TITLE) && (overwriteExistingItems || StringUtils.isBlank(getOriginalTitle()))) {
      // Capitalize first letter of original title if setting is set!
      if (settings.getCapitalWordsInTitles()) {
        setOriginalTitle(StrgUtils.capitalize(metadata.getOriginalTitle()));
      }
      else {
        setOriginalTitle(metadata.getOriginalTitle());
      }
    }

    if (config.contains(MovieScraperMetadataConfig.TAGLINE) && (overwriteExistingItems || StringUtils.isBlank(getTagline()))) {
      setTagline(metadata.getTagline());
    }

    if (config.contains(MovieScraperMetadataConfig.PLOT) && (overwriteExistingItems || StringUtils.isBlank(getPlot()))) {
      setPlot(metadata.getPlot());
    }

    if (config.contains(MovieScraperMetadataConfig.YEAR) && (overwriteExistingItems || getYear() <= 0)) {
      setYear(metadata.getYear());
    }

    if (config.contains(MovieScraperMetadataConfig.RELEASE_DATE) && (overwriteExistingItems || getReleaseDate() == null)) {
      setReleaseDate(metadata.getReleaseDate());
    }

    if (config.contains(MovieScraperMetadataConfig.RATING)) {
      Map<String, MediaRating> newRatings = new HashMap<>();

      if (matchFound || !overwriteExistingItems) {
        // only update new ratings, but let the old ones survive
        newRatings.putAll(getRatings());
      }

      for (MediaRating mediaRating : metadata.getRatings()) {
        if (overwriteExistingItems) {
          newRatings.put(mediaRating.getId(), mediaRating);
        }
        else {
          newRatings.putIfAbsent(mediaRating.getId(), mediaRating);
        }
      }

      setRatings(newRatings);
    }

    if (config.contains(MovieScraperMetadataConfig.TOP250) && (overwriteExistingItems || getTop250() <= 0)) {
      setTop250(metadata.getTop250());
    }

    if (config.contains(MovieScraperMetadataConfig.RUNTIME) && (overwriteExistingItems || getRuntime() <= 0)) {
      setRuntime(metadata.getRuntime());
    }

    if (config.contains(MovieScraperMetadataConfig.SPOKEN_LANGUAGES) && (overwriteExistingItems || StringUtils.isBlank(getSpokenLanguages()))) {
      setSpokenLanguages(StringUtils.join(metadata.getSpokenLanguages(), ", "));
    }

    // country
    if (config.contains(MovieScraperMetadataConfig.COUNTRY) && (overwriteExistingItems || StringUtils.isBlank(getCountry()))) {
      setCountry(StringUtils.join(metadata.getCountries(), ", "));
    }

    // certifications
    if (config.contains(MovieScraperMetadataConfig.CERTIFICATION)
        && (overwriteExistingItems || getCertification() == null || getCertification() == MediaCertification.UNKNOWN)) {
      if (!metadata.getCertifications().isEmpty()) {
        if (metadata.getCertifications().size() > 1) {
          // US has 2 rating systems - MPAA, Parental TV guide - see tt0092644
          // try to find MPAA first...
          MediaCertification mpaa = null;
          for (MediaCertification cert : metadata.getCertifications()) {
            if (!cert.name().startsWith("US_TV")) {
              mpaa = cert;
              break;
            }
          }
          if (mpaa != null) {
            setCertification(mpaa);
          }
          else {
            setCertification(metadata.getCertifications().get(0));
          }
        }
        else {
          setCertification(metadata.getCertifications().get(0));
        }
      }
    }

    // studio
    if (config.contains(MovieScraperMetadataConfig.PRODUCTION_COMPANY) && (overwriteExistingItems || StringUtils.isBlank(getProductionCompany()))) {
      setProductionCompany(StringUtils.join(metadata.getProductionCompanies(), ", "));
    }

    // 1:n relations are either merged (no overwrite) or completely set with the new data

    // cast
    if (config.contains(MovieScraperMetadataConfig.ACTORS)) {
      if (!matchFound || overwriteExistingItems) {
        actors.clear();
      }
      setActors(metadata.getCastMembers(Person.Type.ACTOR));
    }
    if (config.contains(MovieScraperMetadataConfig.DIRECTORS)) {
      if (!matchFound || overwriteExistingItems) {
        directors.clear();
      }
      setDirectors(metadata.getCastMembers(Person.Type.DIRECTOR));
    }
    if (config.contains(MovieScraperMetadataConfig.WRITERS)) {
      if (!matchFound || overwriteExistingItems) {
        writers.clear();
      }
      setWriters(metadata.getCastMembers(Person.Type.WRITER));
    }
    if (config.contains(MovieScraperMetadataConfig.PRODUCERS)) {
      if (!matchFound || overwriteExistingItems) {
        producers.clear();
      }
      setProducers(metadata.getCastMembers(Person.Type.PRODUCER));
    }

    // genres
    if (config.contains(MovieScraperMetadataConfig.GENRES)) {
      if (!matchFound || overwriteExistingItems) {
        genres.clear();
      }
      setGenres(metadata.getGenres());
    }

    // tags
    if (config.contains(MovieScraperMetadataConfig.TAGS)) {
      if (!matchFound || overwriteExistingItems) {
        removeAllTags();
      }

      addToTags(metadata.getTags());
    }

    // create MovieSet
    if (config.contains(MovieScraperMetadataConfig.COLLECTION) && (overwriteExistingItems || getMovieSet() == null)) {
      int col = 0;
      try {
        col = (int) metadata.getId(MediaMetadata.TMDB_SET);
      }
      catch (Exception ignored) {
        // no need to log here
      }
      if (col != 0) {
        boolean created = false;
        MovieSet movieSet = MovieModuleManager.getInstance().getMovieList().findMovieSet(metadata.getCollectionName(), col);

        if (movieSet == null && StringUtils.isNotBlank(metadata.getCollectionName())) {
          // no movie set here yet
          movieSet = new MovieSet(metadata.getCollectionName());
          movieSet.setTmdbId(col);
          movieSet.saveToDb();
          MovieModuleManager.getInstance().getMovieList().addMovieSet(movieSet);
          created = true;
        }

        // add movie to movieset
        if (movieSet != null) {
          // first remove from "old" movieset
          setMovieSet(null);

          // add to new movieset
          setMovieSet(movieSet);
          movieSet.insertMovie(this);
          movieSet.saveToDb();

          // and scrape if unscraped
          if (created) {
            // get movieset metadata
            List<MediaScraper> movieSetMediaScrapers = MediaScraper.getMediaScrapers(ScraperType.MOVIE_SET);
            if (!movieSetMediaScrapers.isEmpty()) {

              // get movieset metadata (async to do not block here)
              MovieSetSearchAndScrapeOptions options = new MovieSetSearchAndScrapeOptions();
              options.setTmdbId(col);
              if (metadata.getScrapeOptions() != null) {
                options.setLanguage(metadata.getScrapeOptions().getLanguage());
              }
              else {
                options.setLanguage(MovieModuleManager.getInstance().getSettings().getScraperLanguage());
              }
              options.setMetadataScraper(movieSetMediaScrapers.get(0));
              options.setArtworkScraper(MovieModuleManager.getInstance().getMovieList().getDefaultArtworkScrapers());
              MovieSetScrapeTask task = new MovieSetScrapeTask(Collections.singletonList(movieSet), options,
                  Arrays.asList(MovieSetScraperMetadataConfig.values()));
              TmmTaskManager.getInstance().addUnnamedTask(task);
            }
          }
        }
      }
    }

    // update DB
    writeNFO();
    saveToDb();

    postProcess(config, overwriteExistingItems);
  }

  /**
   * Sets the trailers; first one is "inNFO" if not a local one.
   * 
   * @param trailers
   *          the new trailers
   */
  @JsonSetter
  public void setTrailers(List<MediaTrailer> trailers) {
    MediaTrailer preferredTrailer = null;
    removeAllTrailers();

    List<MediaTrailer> newItems = new ArrayList<>();

    // set preferred trailer
    if (MovieModuleManager.getInstance().getSettings().isUseTrailerPreference()) {
      TrailerQuality desiredQuality = MovieModuleManager.getInstance().getSettings().getTrailerQuality();
      TrailerSources desiredSource = TrailerSources.YOUTUBE;

      // search for quality and provider
      for (MediaTrailer trailer : trailers) {
        if (desiredQuality.containsQuality(trailer.getQuality()) && desiredSource.containsSource(trailer.getProvider())) {
          trailer.setInNfo(Boolean.TRUE);
          preferredTrailer = trailer;
          break;
        }
      }

      // search for quality
      if (preferredTrailer == null) {
        for (MediaTrailer trailer : trailers) {
          if (desiredQuality.containsQuality(trailer.getQuality())) {
            trailer.setInNfo(Boolean.TRUE);
            preferredTrailer = trailer;
            break;
          }
        }
      }

      // if not yet one has been found; sort by quality descending and take the first one which is lower or equal to the desired quality
      if (preferredTrailer == null) {
        List<MediaTrailer> sortedTrailers = new ArrayList<>(trailers);
        sortedTrailers.sort(TRAILER_QUALITY_COMPARATOR);
        for (MediaTrailer trailer : sortedTrailers) {
          if (desiredQuality.ordinal() >= TrailerQuality.getTrailerQuality(trailer.getQuality()).ordinal()) {
            trailer.setInNfo(Boolean.TRUE);
            preferredTrailer = trailer;
            break;
          }
        }
      }
    } // end if MovieModuleManager.getInstance().getSettings().isUseTrailerPreference()

    // if not yet one has been found; sort by quality descending and take the first one
    if (preferredTrailer == null && !trailers.isEmpty()) {
      List<MediaTrailer> sortedTrailers = new ArrayList<>(trailers);
      sortedTrailers.sort(TRAILER_QUALITY_COMPARATOR);
      preferredTrailer = sortedTrailers.get(0);
      preferredTrailer.setInNfo(Boolean.TRUE);
    }

    // add trailers
    if (preferredTrailer != null) {
      newItems.add(preferredTrailer);
    }
    for (MediaTrailer trailer : trailers) {
      // preferred trailer has already been added
      if (preferredTrailer != null && preferredTrailer == trailer) {
        continue;
      }

      // if still no preferred trailer has been set, then mark the first one
      if (preferredTrailer == null && this.trailer.isEmpty() && trailer.getUrl().startsWith("http")) {
        trailer.setInNfo(Boolean.TRUE);
      }

      newItems.add(trailer);
    }

    addToTrailer(newItems);

    // mix in local trailer
    mixinLocalTrailers();
  }

  /**
   * Sets the artwork.
   * 
   * @param md
   *          the md
   * @param config
   *          the config
   * @param overwrite
   *          should we overwrite existing artwork
   */
  public void setArtwork(MediaMetadata md, List<MovieScraperMetadataConfig> config, boolean overwrite) {
    setArtwork(md.getMediaArt(MediaArtworkType.ALL), config, overwrite);
  }

  /**
   * Sets the artwork.
   * 
   * @param artwork
   *          the artwork
   * @param config
   *          the config
   * @param overwrite
   *          should we overwrite existing artwork
   */
  public void setArtwork(List<MediaArtwork> artwork, List<MovieScraperMetadataConfig> config, boolean overwrite) {
    MovieArtworkHelper.setArtwork(this, artwork, config, overwrite);
  }

  @Override
  public void setTitle(String newValue) {
    String oldValue = this.title;
    super.setTitle(newValue);

    firePropertyChange(TITLE_FOR_UI, oldValue, newValue);

    oldValue = this.titleSortable;
    titleSortable = "";
    firePropertyChange(TITLE_SORTABLE, oldValue, titleSortable);
  }

  @Override
  public void setOriginalTitle(String newValue) {
    String oldValue = this.originalTitle;
    super.setOriginalTitle(newValue);

    firePropertyChange(TITLE_FOR_UI, oldValue, newValue);

    oldValue = this.originalTitleSortable;
    originalTitleSortable = "";
    firePropertyChange(TITLE_SORTABLE, oldValue, originalTitleSortable);
  }

  /**
   * Sets the runtime in minutes
   * 
   * @param newValue
   *          the new runtime
   */
  public void setRuntime(int newValue) {
    int oldValue = this.runtime;
    this.runtime = newValue;
    firePropertyChange(RUNTIME, oldValue, newValue);
  }

  /**
   * Sets the tagline.
   * 
   * @param newValue
   *          the new tagline
   */
  public void setTagline(String newValue) {
    String oldValue = this.tagline;
    this.tagline = newValue;
    firePropertyChange("tagline", oldValue, newValue);
  }

  /**
   * Sets the year.
   * 
   * @param newValue
   *          the new year
   */
  @Override
  public void setYear(int newValue) {
    int oldValue = year;
    super.setYear(newValue);

    firePropertyChange(TITLE_FOR_UI, oldValue, newValue);
  }

  /**
   * all XBMC supported NFO names. (without path!)
   * 
   * @param nfo
   *          the nfo
   * @return the nfo filename
   */
  public String getNfoFilename(MovieNfoNaming nfo) {
    String filename = "";

    MediaFile mainFile = getMainFile();
    if (mainFile != null) {
      filename = mainFile.getFilename();
    }

    if (isStacked()) {
      // when movie IS stacked, remove stacking marker, else keep it!
      filename = Utils.cleanStackingMarkers(filename);
    }

    filename = getNfoFilename(nfo, filename);
    return filename;
  }

  /**
   * all XBMC supported NFO names. (without path!)
   * 
   * @param nfo
   *          the nfo file naming
   * @param newMovieFilename
   *          the new/desired movie filename (stacking marker should already be set correct here!)
   * @return the nfo filename
   */
  public String getNfoFilename(MovieNfoNaming nfo, String newMovieFilename) {
    String filename;

    switch (nfo) {
      case FILENAME_NFO:
        if (isDisc()) {
          // in case of disc, this is the name of the "main" disc identifier file!
          if (MovieModuleManager.getInstance().getSettings().isNfoDiscFolderInside()) {
            filename = FilenameUtils.removeExtension(findDiscMainFile());
          }
          else {
            filename = "movie";
          }
        }
        else {
          filename = FilenameUtils.removeExtension(newMovieFilename);
        }
        if (!filename.isEmpty()) {
          filename += ".nfo";
        }
        break;

      case MOVIE_NFO:
        filename = "movie.nfo";
        break;

      default:
        filename = "";
        break;
    }

    LOGGER.trace("getNfoFilename: '{}' / '{}' -> '{}'", newMovieFilename, nfo, filename);
    return filename;
  }

  /**
   * all supported TRAILER names. (without path!)
   *
   * @param trailer
   *          trailer naming enum
   * @return the associated trailer filename
   */
  public String getTrailerFilename(MovieTrailerNaming trailer) {
    String filename = "";

    if (isDisc) {
      filename = findDiscMainFile();
    }
    else {
      MediaFile mainFile = getMainFile();
      if (mainFile != null) {
        filename = mainFile.getFilename();
      }
    }

    if (isStacked()) {
      // when movie IS stacked, remove stacking marker, else keep it!
      filename = Utils.cleanStackingMarkers(filename);
    }
    filename = getTrailerFilename(trailer, filename);

    LOGGER.trace("getTrailerFilename: {} -> '{}'", trailer, filename);
    return filename;
  }

  /**
   * all supported TRAILER names. (without path!)
   * 
   * @param trailer
   *          trailer naming enum
   * @param newMovieFilename
   *          the new/desired movie filename (stacking marker should already be set correct here!)
   * @return the associated trailer filename <b>(WITHOUT EXTENSION!!!!)</b>
   */
  public String getTrailerFilename(MovieTrailerNaming trailer, String newMovieFilename) {
    String filename = trailer.getFilename(FilenameUtils.getBaseName(newMovieFilename), FilenameUtils.getExtension(newMovieFilename));

    // remove the extension - will be re-added later
    filename = FilenameUtils.removeExtension(filename);

    LOGGER.trace("getTrailerFilename: '{}' / {} -> '{}'", newMovieFilename, trailer, filename);
    return filename;
  }

  /**
   * download the specified type of artwork for this movie
   *
   * @param type
   *          the chosen artwork type to be downloaded
   */
  public void downloadArtwork(MediaFileType type) {
    MovieArtworkHelper.downloadArtwork(this, type);
  }

  /**
   * Write actor images.
   */
  public void writeActorImages(boolean overwriteExistingItems) {
    MovieActorImageFetcherTask task = new MovieActorImageFetcherTask(this);
    task.setOverwriteExistingItems(overwriteExistingItems);
    TmmTaskManager.getInstance().addImageDownloadTask(task);
  }

  /**
   * Write nfo.
   */
  public void writeNFO() {
    if (MovieModuleManager.getInstance().getSettings().getNfoFilenames().isEmpty()) {
      LOGGER.info("Not writing any NFO file, because NFO filename preferences were empty...");
      return;
    }

    IMovieConnector connector;

    switch (MovieModuleManager.getInstance().getSettings().getMovieConnector()) {
      case MP:
        connector = new MovieToMpLegacyConnector(this);
        break;

      case MP_MP:
        connector = new MovieToMpMovingPicturesConnector(this);
        break;

      case MP_MV:
        connector = new MovieToMpMyVideoConnector(this);
        break;

      case XBMC:
        connector = new MovieToXbmcConnector(this);
        break;

      case EMBY:
        connector = new MovieToEmbyConnector(this);
        break;

      case JELLYFIN:
        connector = new MovieToJellyfinConnector(this);
        break;

      case KODI:
      case PLEX:
      case DVR_3:
      default:
        connector = new MovieToKodiConnector(this);
        break;
    }

    List<MovieNfoNaming> nfonames = new ArrayList<>();
    if (isMultiMovieDir() || isDisc) {
      // Fixate the name regardless of setting
      nfonames.add(MovieNfoNaming.FILENAME_NFO);
    }
    else {
      nfonames = MovieModuleManager.getInstance().getSettings().getNfoFilenames();
    }

    try {
      connector.write(nfonames);
      firePropertyChange(HAS_NFO_FILE, false, true);
    }
    catch (Exception e) {
      LOGGER.error("could not write NFO file - '{}'", e.getMessage());
    }
  }

  /**
   * Gets the genres.
   * 
   * @return the genres
   */
  public List<MediaGenres> getGenres() {
    return genres;
  }

  /**
   * Adds the given genres
   * 
   * @param newGenres
   *          a {@link Collection} with the new genres
   */
  public void addToGenres(Collection<MediaGenres> newGenres) {
    Set<MediaGenres> newItems = new LinkedHashSet<>();

    // do not accept duplicates or null values
    for (MediaGenres genre : ListUtils.nullSafe(newGenres)) {
      if (genre == null || genres.contains(genre)) {
        continue;
      }
      newItems.add(genre);
    }

    if (newItems.isEmpty()) {
      return;
    }

    genres.addAll(newItems);
    firePropertyChange(GENRE, null, newGenres);
    firePropertyChange(GENRES_AS_STRING, null, newGenres);
  }

  /**
   * Sets the genres.
   * 
   * @param newGenres
   *          the new genres
   */
  @JsonSetter
  public void setGenres(List<MediaGenres> newGenres) {
    // two way sync of genres
    ListUtils.mergeLists(genres, newGenres);

    firePropertyChange(GENRE, null, genres);
    firePropertyChange(GENRES_AS_STRING, null, genres);
  }

  /**
   * Removes the genre.
   * 
   * @param genre
   *          the genre
   */
  public void removeGenre(MediaGenres genre) {
    if (genres.contains(genre)) {
      genres.remove(genre);
      firePropertyChange(GENRE, null, genre);
      firePropertyChange(GENRES_AS_STRING, null, genre);
    }
  }

  /**
   * Remove all genres from list
   */
  public void removeAllGenres() {
    genres.clear();
    firePropertyChange(GENRE, null, genres);
    firePropertyChange(GENRES_AS_STRING, null, genres);
  }

  /**
   * Gets the certifications.
   * 
   * @return the certifications
   */
  public MediaCertification getCertification() {
    return certification;
  }

  /**
   * Sets the certifications.
   * 
   * @param newValue
   *          the new certifications
   */
  public void setCertification(MediaCertification newValue) {
    this.certification = newValue;
    firePropertyChange(CERTIFICATION, null, newValue);
  }

  /**
   * get the "main" rating
   *
   * @return the main (preferred) rating
   */
  @Override
  public MediaRating getRating() {
    MediaRating mediaRating = null;

    for (String ratingSource : MovieModuleManager.getInstance().getSettings().getRatingSources()) {
      // prevent crashing with null values
      if (StringUtils.isBlank(ratingSource)) {
        continue;
      }

      mediaRating = ratings.get(ratingSource);
      if (mediaRating != null) {
        break;
      }
    }

    if (mediaRating == null) {
      mediaRating = MediaMetadata.EMPTY_RATING;
    }

    return mediaRating;
  }

  /**
   * Gets the checks for rating.
   * 
   * @return the checks for rating
   */
  public boolean getHasRating() {
    return !ratings.isEmpty();
  }

  /**
   * Gets the user rating
   * 
   * @return User Rating as String
   */
  public MediaRating getUserRating() {
    MediaRating mediaRating = ratings.get(MediaRating.USER);

    if (mediaRating == null) {
      mediaRating = MediaMetadata.EMPTY_RATING;
    }
    return mediaRating;
  }

  /**
   * Gets the genres as string.
   * 
   * @return the genres as string
   */
  public String getGenresAsString() {
    StringBuilder sb = new StringBuilder();
    for (MediaGenres genre : genres) {
      if (!StringUtils.isEmpty(sb)) {
        sb.append(", ");
      }
      sb.append(genre != null ? genre.getLocalizedName() : "null");
    }
    return sb.toString();
  }

  /**
   * Checks if is watched.
   * 
   * @return true, if is watched
   */
  public boolean isWatched() {
    return watched;
  }

  /**
   * Sets the watched.
   * 
   * @param newValue
   *          the new watched
   */
  public void setWatched(boolean newValue) {
    boolean oldValue = this.watched;
    this.watched = newValue;
    firePropertyChange(WATCHED, oldValue, newValue);
  }

  /**
   * get the play count (mainly passed from/to trakt.tv/NFO)
   * 
   * @return the play count of this movie
   */
  public int getPlaycount() {
    return playcount;
  }

  /**
   * sets the play count
   * 
   * @param newValue
   *          the play count of this movie
   */
  public void setPlaycount(int newValue) {
    int oldValue = this.playcount;
    this.playcount = newValue;
    firePropertyChange("playcount", oldValue, newValue);
  }

  /**
   * Checks if this movie is in a folder with other movies and not in an own folder<br>
   * so disable everything except renaming
   * 
   * @return true, if in datasource root
   */
  public boolean isMultiMovieDir() {
    return multiMovieDir;
  }

  /**
   * Sets the flag, that the movie is not in an own folder<br>
   * so disable everything except renaming
   * 
   * @param multiDir
   *          true/false
   */
  public void setMultiMovieDir(boolean multiDir) {
    this.multiMovieDir = multiDir;
  }

  /**
   * more sophisticated check, if a movie in its current naming form might also be a "multiMovie"<br>
   * If ALL filenames start with video filename, the chances are good :)<br>
   * used in ChangeDatasource action, where we could rename as MMD, and not whole folder.<br>
   * (isMMD is only evaluated on import - this is dynamic)
   * 
   * @return true if that is the case, false otherwise
   */
  public boolean hasMultiMovieNaming() {
    if (isDisc()) {
      return false;
    }

    MediaFile vid = getMainFile();
    String name = FilenameUtils.getBaseName(vid.getFilenameWithoutStacking());
    if (name.isEmpty()) {
      return false;
    }

    for (MediaFile mf : getMediaFiles()) {
      // if we have a single file not starting with our video filename, it cannot be a MMD
      if (!mf.getFilename().startsWith(name) || mf.isDiscFile()) {
        return false;
      }
    }
    // all files good
    return true;
  }

  /**
   * Gets the movie set.
   * 
   * @return the movieset
   */
  public MovieSet getMovieSet() {
    return movieSet;
  }

  /**
   * Sets the movie set.
   * 
   * @param newValue
   *          the new movie set
   */
  public void setMovieSet(MovieSet newValue) {
    MovieSet oldValue = this.movieSet;
    this.movieSet = newValue;

    if (newValue == null) {
      movieSetId = null;
    }
    else {
      movieSetId = newValue.getDbId();
    }

    firePropertyChange(MOVIESET, oldValue, newValue);
    firePropertyChange(MOVIESET_TITLE, oldValue, newValue);
  }

  public void movieSetTitleChanged() {
    firePropertyChange(MOVIESET_TITLE, null, "");
  }

  public String getMovieSetTitle() {
    if (movieSet != null) {
      return movieSet.getTitle();
    }
    return "";
  }

  /**
   * Removes the from movie set.
   */
  public void removeFromMovieSet() {
    if (movieSet != null) {
      movieSet.removeMovie(this, true);
    }
    setMovieSet(null);
  }

  /**
   * is this a disc movie folder (video_ts / bdmv)?.
   * 
   * @return true, if is disc
   */
  public boolean isDisc() {
    return isDisc;
  }

  /**
   * is this a disc movie folder (video_ts / bdmv)?.
   * 
   * @param isDisc
   *          the new disc
   */
  public void setDisc(boolean isDisc) {
    this.isDisc = isDisc;
  }

  public String findDiscMainFile() {
    MediaFile mainVideoFile = getMainVideoFile();

    String filename = "";

    if (mainVideoFile.isBlurayFile()) {
      filename = "index.bdmv";
    }
    if (mainVideoFile.isDVDFile()) {
      filename = "VIDEO_TS.ifo";
    }
    if (mainVideoFile.isHDDVDFile()) {
      filename = "HVDVD_TS.ifo";
    }

    if (StringUtils.isNotBlank(filename) && mainVideoFile.getFile().toFile().isDirectory()) {
      filename = mainVideoFile.getFilename() + File.separator + filename;
    }

    return filename;
  }

  public int getMediaInfoVideoBitrate() {
    return getMainVideoFile().getVideoBitRate();
  }

  @Override
  public int getMediaInfoVideoBitDepth() {
    return getMainVideoFile().getBitDepth();
  }

  /**
   * Gets the media info audio codec (i.e mp3) and channels (i.e. 6 at 5.1 sound)
   */
  public String getMediaInfoAudioCodecAndChannels() {
    MediaFile mf = getMainVideoFile();
    if (!mf.getAudioCodec().isEmpty()) {
      return mf.getAudioCodec() + "_" + mf.getAudioChannels();
    }
    return "";
  }

  public void setSpokenLanguages(String newValue) {
    String oldValue = this.spokenLanguages;
    this.spokenLanguages = newValue;
    firePropertyChange(SPOKEN_LANGUAGES, oldValue, newValue);

    localizedSpokenLanguages = "";
    firePropertyChange("localizedSpokenLanguages", oldValue, newValue);
  }

  public String getSpokenLanguages() {
    return this.spokenLanguages;
  }

  public String getLocalizedSpokenLanguages() {
    if (StringUtils.isBlank(localizedSpokenLanguages)) {
      List<String> translatedLanguages = new ArrayList<>();
      for (String langu : ParserUtils.split(getSpokenLanguages())) {
        String translated = LanguageUtils
            .getLocalizedLanguageNameFromLocalizedString(Utils.getLocaleFromLanguage(Settings.getInstance().getLanguage()), langu.strip());
        translatedLanguages.add(translated);
      }

      localizedSpokenLanguages = String.join(", ", translatedLanguages);
    }

    // prepare the languages to be printed in localized form
    return localizedSpokenLanguages;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String newValue) {
    String oldValue = this.country;
    this.country = newValue;
    firePropertyChange(COUNTRY, oldValue, newValue);
  }

  public MediaSource getMediaSource() {
    return mediaSource;
  }

  public void setMediaSource(MediaSource newValue) {
    MediaSource oldValue = this.mediaSource;
    this.mediaSource = newValue;
    firePropertyChange(MEDIA_SOURCE, oldValue, newValue);
  }

  /**
   * Gets the images to cache.
   */
  @Override
  public List<MediaFile> getImagesToCache() {
    // image files
    List<MediaFile> filesToCache = new ArrayList<>();
    for (MediaFile mf : getMediaFiles()) {
      if (mf.isGraphic()) {
        filesToCache.add(mf);
      }
    }

    // getting all scraped actors (= possible to cache)
    // and having never ever downloaded any pic is quite slow.
    // (Many invalid cache requests and exists() checks)
    // Better get a listing of existent actor images directly!
    if (MovieModuleManager.getInstance().getSettings().isWriteActorImages() && !isMultiMovieDir()) {
      // and only for normal movies - MMD should not have .actors folder!
      for (MediaFile mf : listActorFiles()) {
        if (mf.isGraphic()) {
          filesToCache.add(mf);
        }
      }
    } // check against actors and trigger a download? - NO, only via scrape/missingImagesTask

    return filesToCache;
  }

  /**
   * @return list of actor images on filesystem
   */
  protected List<MediaFile> listActorFiles() {
    if (getPathNIO() == null || !Files.exists(getPathNIO().resolve(Person.ACTOR_DIR))) {
      return Collections.emptyList();
    }

    List<MediaFile> fileNames = new ArrayList<>();

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(getPathNIO().resolve(Person.ACTOR_DIR))) {
      for (Path path : directoryStream) {
        if (Utils.isRegularFile(path)) {
          // only get graphics
          MediaFile mf = new MediaFile(path);
          if (mf.isGraphic()) {
            fileNames.add(mf);
          }
        }
      }
    }
    catch (IOException e) {
      LOGGER.debug("Cannot get actors: {}", getPathNIO().resolve(Person.ACTOR_DIR));
    }

    return fileNames;
  }

  public List<MediaFile> getMediaFilesContainingAudioStreams() {
    List<MediaFile> mediaFilesWithAudioStreams = new ArrayList<>(1);

    // get the audio streams from the first video file
    List<MediaFile> videoFiles = getMediaFiles(MediaFileType.VIDEO);
    if (!videoFiles.isEmpty()) {
      MediaFile videoFile = videoFiles.get(0);
      mediaFilesWithAudioStreams.add(videoFile);
    }

    // get all extra audio streams
    mediaFilesWithAudioStreams.addAll(getMediaFiles(MediaFileType.AUDIO));

    return mediaFilesWithAudioStreams;
  }

  public List<MediaFile> getMediaFilesContainingSubtitles() {
    List<MediaFile> mediaFilesWithSubtitles = new ArrayList<>(1);

    for (MediaFile mediaFile : getMediaFiles(MediaFileType.VIDEO, MediaFileType.AUDIO, MediaFileType.SUBTITLE)) {
      if (mediaFile.hasSubtitles()) {
        mediaFilesWithSubtitles.add(mediaFile);
      }
    }

    return mediaFilesWithSubtitles;
  }

  @Deprecated
  private int getRuntimeFromDvdFiles() {
    int rtifo = 0;
    MediaFile ifo = null;

    // loop over all IFOs, and find the one with longest runtime == main video file?
    for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO)) {
      if (mf.getFilename().toLowerCase(Locale.ROOT).endsWith("ifo")) {
        if (mf.getDuration() > rtifo) {
          rtifo = mf.getDuration();
          ifo = mf; // need the prefix
        }
      }
    }

    if (ifo != null) {
      LOGGER.trace("Found longest IFO:{} duration:{}", ifo.getFilename(), runtime);

      // check DVD VOBs
      String prefix = StrgUtils.substr(ifo.getFilename(), "(?i)^(VTS_\\d+).*");
      if (prefix.isEmpty()) {
        // check HD-DVD
        prefix = StrgUtils.substr(ifo.getFilename(), "(?i)^(HV\\d+)I.*");
      }

      if (!prefix.isEmpty()) {
        int rtvob = 0;
        for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO)) {
          if (mf.getFilename().startsWith(prefix) && !ifo.getFilename().equals(mf.getFilename())) {
            rtvob += mf.getDuration();
            LOGGER.trace("VOB:{} duration:{} accumulated:{}", mf.getFilename(), mf.getDuration(), rtvob);
          }
        }
        if (rtvob > rtifo) {
          rtifo = rtvob;
        }
      }
      else {
        // no IFO? must be bluray
        LOGGER.trace("TODO: bluray");
      }
    }

    return rtifo;
  }

  // do not break existing templates et all
  @Deprecated
  public int getRuntimeFromMediaFiles() {
    return getRuntimeFromMediaFilesInSeconds();
  }

  public int getRuntimeFromMediaFilesInSeconds() {
    int runtime = 0;
    if (isDisc) {
      // FIXME: does not work with our fake folder MF anylonger
      // and no, we should not parse IFOs/MPLS files here (IO)
      // runtime = getRuntimeFromDvdFiles();
      runtime = this.runtime * 60;
    }

    // accumulate old version
    if (runtime < 10) {
      for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO)) {
        if (!mf.isMainDiscIdentifierFile() && !mf.getFilename().toLowerCase(Locale.ROOT).endsWith("ifo")) { // exclude all IFOs
          runtime += mf.getDuration();
        }
      }
    }
    return runtime;
  }

  public int getRuntimeFromMediaFilesInMinutes() {
    return getRuntimeFromMediaFilesInSeconds() / 60;
  }

  @Override
  public Date getReleaseDate() {
    return releaseDate;
  }

  @JsonIgnore
  public void setReleaseDate(Date newValue) {
    Date oldValue = this.releaseDate;
    this.releaseDate = newValue;
    firePropertyChange(RELEASE_DATE, oldValue, newValue);
    firePropertyChange(RELEASE_DATE_AS_STRING, oldValue, newValue);
  }

  /**
   * release date as yyyy-mm-dd<br>
   * https://xkcd.com/1179/ :P
   */
  public String getReleaseDateFormatted() {
    if (this.releaseDate == null) {
      return "";
    }
    return new SimpleDateFormat("yyyy-MM-dd").format(this.releaseDate);
  }

  /**
   * Gets the first aired as a string, formatted in the system locale.
   */
  public String getReleaseDateAsString() {
    if (this.releaseDate == null) {
      return "";
    }
    return TmmDateFormat.MEDIUM_DATE_FORMAT.format(releaseDate);
  }

  /**
   * convenient method to set the release date (parsed from string).
   */
  public void setReleaseDate(String dateAsString) {
    try {
      setReleaseDate(StrgUtils.parseDate(dateAsString));
    }
    catch (ParseException ignored) {
      // ignored
    }
  }

  public Date getLastWatched() {
    return lastWatched;
  }

  public void setLastWatched(Date lastWatched) {
    this.lastWatched = lastWatched;
  }

  @Override
  public void saveToDb() {
    // update/insert this movie to the database
    MovieModuleManager.getInstance().getMovieList().persistMovie(this);
  }

  @Override
  public synchronized void callbackForWrittenArtwork(MediaArtworkType type) {
    if (MovieModuleManager.getInstance().getSettings().getMovieConnector() == MovieConnectors.MP) {
      writeNFO();
    }
  }

  /**
   * get all video files for that movie
   *
   * @return a list of all video files
   */
  public List<MediaFile> getVideoFiles() {
    return getMediaFiles(MediaFileType.VIDEO);
  }

  /**
   * gets the basename (without stacking)
   * 
   * @return the video base name (without stacking)
   */
  public String getVideoBasenameWithoutStacking() {
    MediaFile mf = getMediaFiles(MediaFileType.VIDEO).get(0);
    return FilenameUtils.getBaseName(mf.getFilenameWithoutStacking());
  }

  public int getTop250() {
    return top250;
  }

  public void setVideoIn3D(boolean newValue) {
    boolean oldValue = this.videoIn3D;
    this.videoIn3D = newValue;
    firePropertyChange(VIDEO_IN_3D, oldValue, newValue);
  }

  public boolean isVideoIn3D() {
    return videoIn3D || StringUtils.isNotBlank(getMainVideoFile().getVideo3DFormat());
  }

  public void setTop250(int newValue) {
    int oldValue = this.top250;
    this.top250 = newValue;
    firePropertyChange(TOP250, oldValue, newValue);
  }

  /**
   * adds the given actors
   *
   * @param newActors
   *          a {@link Collection} of all actors to be added
   */
  public void addToActors(Collection<Person> newActors) {
    Set<Person> newItems = new LinkedHashSet<>();

    // do not accept duplicates or null values
    for (Person person : ListUtils.nullSafe(newActors)) {
      if (person == null || actors.contains(person)) {
        continue;
      }
      if (person.getType() != Person.Type.ACTOR && person.getType() != Person.Type.GUEST) {
        return;
      }

      newItems.add(person);
    }

    if (newItems.isEmpty()) {
      return;
    }

    actors.addAll(newItems);
    firePropertyChange(ACTORS, null, getActors());
    firePropertyChange(ACTORS_AS_STRING, null, getActorsAsString());
  }

  /**
   * remove all actors.
   */
  public void removeActors() {
    actors.clear();
    firePropertyChange(ACTORS, null, getActors());
    firePropertyChange(ACTORS_AS_STRING, null, getActorsAsString());
  }

  /**
   * set the actors. This will do a two way sync of the list to be added and the given list, to do not re-add existing actors (better for binding)
   *
   * @param newActors
   *          the new actors to be set
   */
  @JsonSetter
  public void setActors(List<Person> newActors) {
    // two way sync of actors
    mergePersons(actors, newActors);
    firePropertyChange(ACTORS, null, getActors());
    firePropertyChange(ACTORS_AS_STRING, null, getActorsAsString());
  }

  /**
   * get the actors
   *
   * @return the actors
   */
  public List<Person> getActors() {
    return this.actors;
  }

  /**
   * get the actors as string
   *
   * @return a string containing all actors; separated by ,
   */
  public String getActorsAsString() {
    List<String> actorNames = new ArrayList<>();
    for (Person actor : actors) {
      actorNames.add(actor.getName());
    }
    return StringUtils.join(actorNames, ", ");
  }

  /**
   * adds the given producers
   *
   * @param newProducers
   *          a {@link Collection} of all producers to be added
   */
  public void addToProducers(Collection<Person> newProducers) {
    Set<Person> newItems = new LinkedHashSet<>();

    // do not accept duplicates or null values
    for (Person person : ListUtils.nullSafe(newProducers)) {
      if (person == null || producers.contains(person)) {
        continue;
      }
      if (person.getType() != Person.Type.PRODUCER) {
        return;
      }

      newItems.add(person);
    }

    if (newItems.isEmpty()) {
      return;
    }

    producers.addAll(newItems);
    firePropertyChange(PRODUCERS, null, getProducers());
    firePropertyChange(PRODUCERS_AS_STRING, null, getProducersAsString());
  }

  /**
   * remove all producers
   */
  public void removeProducers() {
    producers.clear();
    firePropertyChange(PRODUCERS, null, getProducers());
    firePropertyChange(PRODUCERS_AS_STRING, null, getProducersAsString());
  }

  /**
   * set the producers. This will do a two-way sync of the list to be added and the given list, to do not re-add existing producers (better for
   * binding)
   *
   * @param newProducers
   *          the new producers to be set
   */
  @JsonSetter
  public void setProducers(List<Person> newProducers) {
    // two way sync of producers
    mergePersons(producers, newProducers);
    firePropertyChange(PRODUCERS, null, getProducers());
    firePropertyChange(PRODUCERS_AS_STRING, null, getProducersAsString());
  }

  /**
   * get the producers
   *
   * @return the producers
   */
  public List<Person> getProducers() {
    return this.producers;
  }

  /**
   * get the producers as string
   *
   * @return a string containing all producers; separated by ,
   */
  public String getProducersAsString() {
    List<String> producerNames = new ArrayList<>();
    for (Person producer : producers) {
      producerNames.add(producer.getName());
    }
    return StringUtils.join(producerNames, ", ");
  }

  /**
   * add the given list of directors
   *
   * @param newDirectors
   *          a {@link Collection} of directors to be added
   */
  public void addToDirectors(Collection<Person> newDirectors) {
    Set<Person> newItems = new LinkedHashSet<>();

    // do not accept duplicates or null values
    for (Person person : ListUtils.nullSafe(newDirectors)) {
      if (person == null || directors.contains(person)) {
        continue;
      }
      if (person.getType() != Person.Type.DIRECTOR) {
        return;
      }

      newItems.add(person);
    }

    if (newItems.isEmpty()) {
      return;
    }

    directors.addAll(newItems);
    firePropertyChange(DIRECTORS, null, directors);
    firePropertyChange(DIRECTORS_AS_STRING, null, getDirectorsAsString());
  }

  /**
   * remove all directors.
   */
  public void removeDirectors() {
    directors.clear();
    firePropertyChange(DIRECTORS, null, directors);
    firePropertyChange(DIRECTORS_AS_STRING, null, getDirectorsAsString());
  }

  /**
   * Sets the directors.
   *
   * @param newDirectors
   *          the new directors
   */
  @JsonSetter
  public void setDirectors(List<Person> newDirectors) {
    // two way sync of directors
    mergePersons(directors, newDirectors);

    firePropertyChange(DIRECTORS, null, directors);
    firePropertyChange(DIRECTORS_AS_STRING, null, getDirectorsAsString());
  }

  /**
   * get the directors.
   *
   * @return the directors
   */
  public List<Person> getDirectors() {
    return directors;
  }

  /**
   * get the directors as string
   *
   * @return a string containing all directors; separated by ,
   */
  public String getDirectorsAsString() {
    List<String> directorNames = new ArrayList<>();
    for (Person director : directors) {
      directorNames.add(director.getName());
    }
    return StringUtils.join(directorNames, ", ");
  }

  /**
   * add the given writers
   *
   * @param newWriters
   *          a {@link Collection} of the writers to be added
   */
  public void addToWriters(Collection<Person> newWriters) {
    Set<Person> newItems = new LinkedHashSet<>();

    // do not accept duplicates or null values
    for (Person person : ListUtils.nullSafe(newWriters)) {
      if (person == null || writers.contains(person)) {
        continue;
      }
      if (person.getType() != Person.Type.WRITER) {
        return;
      }

      newItems.add(person);
    }

    if (newItems.isEmpty()) {
      return;
    }

    writers.addAll(newItems);
    firePropertyChange(WRITERS, null, getWriters());
    firePropertyChange(WRITERS_AS_STRING, null, getWritersAsString());
  }

  /**
   * remove all writers.
   */
  public void removeWriters() {
    writers.clear();
    firePropertyChange(WRITERS, null, getWriters());
    firePropertyChange(WRITERS_AS_STRING, null, getWritersAsString());
  }

  /**
   * Sets the writers.
   *
   * @param newWriters
   *          the new writers
   */
  @JsonSetter
  public void setWriters(List<Person> newWriters) {
    // two way sync of writers
    mergePersons(writers, newWriters);

    firePropertyChange(WRITERS, null, this.getWriters());
    firePropertyChange(WRITERS_AS_STRING, null, getWritersAsString());
  }

  /**
   * Gets the writers.
   *
   * @return the writers
   */
  public List<Person> getWriters() {
    return writers;
  }

  /**
   * get the writers as string
   *
   * @return a string containing all writers; separated by ,
   */
  public String getWritersAsString() {
    List<String> writerNames = new ArrayList<>();
    for (Person writer : writers) {
      writerNames.add(writer.getName());
    }
    return StringUtils.join(writerNames, ", ");
  }

  /**
   * Is the movie "stacked" (more than one video file)
   * 
   * @return true if the movie is stacked; false otherwise
   */
  public boolean isStacked() {
    return stacked;
  }

  public void setStacked(boolean stacked) {
    this.stacked = stacked;
  }

  /**
   * when exchanging the video from a disc folder to a file, we have to re-evaluate our "disc" folder flag<br>
   * Just evaluate VIDEO files - not ALL!!!
   */
  public void reEvaluateDiscfolder() {
    boolean disc = false;
    for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO)) {
      if (mf.isDiscFile()) {
        disc = true;
      }
    }
    setDisc(disc);
  }

  /**
   * ok, we might have detected some stacking MFs.<br>
   * But if we only have ONE video file, reset stacking markers in this case<br>
   * eg: "Harry Potter 7 - Part 1" is not stacked<br>
   * CornerCase: what if HP7 has more files...?
   */
  public void reEvaluateStacking() {
    List<MediaFile> mfs = getMediaFiles(MediaFileType.VIDEO);
    if (mfs.size() > 1 && !isDisc()) {
      // ok, more video files means stacking (if not a disc folder)
      // not always true - having a duplicated video with other extension - check explicitly
      boolean stacked = false;
      for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO, MediaFileType.AUDIO, MediaFileType.SUBTITLE, MediaFileType.MEDIAINFO,
          MediaFileType.VSMETA, MediaFileType.DOUBLE_EXT)) {
        mf.detectStackingInformation();
        if (mf.getStacking() > 0) {
          stacked = true;
        }
      }
      this.setStacked(stacked);
    }
    else {
      // only ONE video? remove any stacking markers from MFs
      this.setStacked(false);
      for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO, MediaFileType.AUDIO, MediaFileType.SUBTITLE, MediaFileType.MEDIAINFO,
          MediaFileType.VSMETA, MediaFileType.DOUBLE_EXT)) {
        mf.removeStackingInformation();
      }
    }
  }

  /**
   * <b>PHYSICALLY</b> deletes a complete Movie by moving it to datasource backup folder<br>
   * DS\.backup\&lt;moviename&gt;
   */
  public boolean deleteFilesSafely() {
    // backup
    if (isMultiMovieDir()) {
      boolean ok = true;
      for (MediaFile mf : getMediaFiles()) {
        if (!mf.deleteSafely(getDataSource())) {
          ok = false;
        }
      }

      // also try to remove the folder (if it is empty)
      try {
        Utils.deleteEmptyDirectoryRecursive(getPathNIO());
      }
      catch (Exception ignored) {
        // just ignore
      }

      return ok;
    }
    else {
      return Utils.deleteDirectorySafely(getPathNIO(), getDataSource());
    }
  }

  @Override
  public MediaFile getMainVideoFile() {
    MediaFile vid = null;

    if (stacked) {
      // search the first stacked media file (e.g. CD1)
      vid = getMediaFiles(MediaFileType.VIDEO).stream().min(Comparator.comparingInt(MediaFile::getStacking)).orElse(MediaFile.EMPTY_MEDIAFILE);
    }
    else {
      // try to find correct main movie file (DVD only)
      if (isDisc()) {
        vid = getMainDVDVideoFile();
      }
    }
    // we didn't find one, so get the biggest one
    if (vid == null || vid.getFilename().isEmpty()) {
      vid = getBiggestMediaFile(MediaFileType.VIDEO);
    }

    if (vid != null) {
      return vid;
    }

    LOGGER.warn("Movie without video file? {} | {}", getPathNIO(), getTitle());
    // cannot happen - movie MUST always have a video file
    return MediaFile.EMPTY_MEDIAFILE;
  }

  public MediaFile getMainDVDVideoFile() {
    MediaFile vid = null;

    // find IFO file with the longest duration
    for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO)) {
      if (mf.getExtension().equalsIgnoreCase("ifo")) {
        if (vid == null || mf.getDuration() > vid.getDuration()) {
          vid = mf;
        }
      }
    }
    // find the vob matching to our ifo
    if (vid != null) {
      // check DVD VOBs
      String prefix = StrgUtils.substr(vid.getFilename(), "(?i)^(VTS_\\d+).*");
      if (prefix.isEmpty()) {
        // check HD-DVD
        prefix = StrgUtils.substr(vid.getFilename(), "(?i)^(HV\\d+)I.*");
      }
      for (MediaFile mif : getMediaFiles(MediaFileType.VIDEO)) {
        // TODO: check HD-DVD
        if (mif.getFilename().startsWith(prefix) && !mif.getFilename().endsWith("IFO")) {
          vid = mif;
          // take last to not get the menu one...
        }
      }
    }

    // no IFO/VOB? - might be bluray
    if (vid == null) {
      for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO)) {
        if (mf.getExtension().equalsIgnoreCase("m2ts")) {
          if (vid == null || mf.getDuration() > vid.getDuration()) {
            vid = mf;
          }
        }
      }
    }

    return vid;
  }

  @Override
  public MediaFile getMainFile() {
    return getMainVideoFile();
  }

  @Override
  public String getMediaInfoVideoResolution() {
    return getMainVideoFile().getVideoResolution();
  }

  @Override
  public String getMediaInfoVideoFormat() {
    return getMainVideoFile().getVideoFormat();
  }

  @Override
  public String getMediaInfoVideoCodec() {
    return getMainVideoFile().getVideoCodec();
  }

  @Override
  public double getMediaInfoFrameRate() {
    return getMainVideoFile().getFrameRate();
  }

  @Override
  public float getMediaInfoAspectRatio() {
    return getMainVideoFile().getAspectRatio();
  }

  public String getMediaInfoAspectRatioAsString() {
    DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
    return df.format(getMainVideoFile().getAspectRatio()).replaceAll("\\.", "");
  }

  @Override
  public Float getMediaInfoAspectRatio2() {
    return getMainVideoFile().getAspectRatio2();
  }

  public String getMediaInfoAspectRatio2AsString() {
    Float aspectRatio2 = getMediaInfoAspectRatio2();
    String ar2AsString = "";
    if (aspectRatio2 != null) {
      DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
      ar2AsString = df.format(getMainVideoFile().getAspectRatio2()).replaceAll("\\.", "");
    }
    return ar2AsString;
  }

  public boolean isMultiFormat() {
    return getMainVideoFile().getAspectRatio2() != null;
  }

  @Override
  public String getMediaInfoAudioCodec() {
    return getMainVideoFile().getAudioCodec();
  }

  @Override
  public List<String> getMediaInfoAudioCodecList() {
    List<String> lang = new ArrayList<String>();
    lang.addAll(getMainVideoFile().getAudioCodecList());

    for (MediaFile mf : getMediaFiles(MediaFileType.AUDIO)) {
      lang.addAll(mf.getAudioCodecList());
    }
    return lang;
  }

  @Override
  public String getMediaInfoAudioChannels() {
    return getMainVideoFile().getAudioChannels();
  }

  @Override
  public List<String> getMediaInfoAudioChannelList() {
    List<String> lang = new ArrayList<String>();
    lang.addAll(getMainVideoFile().getAudioChannelsList());

    for (MediaFile mf : getMediaFiles(MediaFileType.AUDIO)) {
      lang.addAll(mf.getAudioChannelsList());
    }
    return lang;
  }

  @Override
  public String getMediaInfoAudioChannelsDot() {
    return getMainVideoFile().getAudioChannelsDot();
  }

  @Override
  public List<String> getMediaInfoAudioChannelDotList() {
    List<String> lang = new ArrayList<String>();
    lang.addAll(getMainVideoFile().getAudioChannelsDotList());

    for (MediaFile mf : getMediaFiles(MediaFileType.AUDIO)) {
      lang.addAll(mf.getAudioChannelsDotList());
    }
    return lang;
  }

  @Override
  public String getMediaInfoAudioLanguage() {
    return getMainVideoFile().getAudioLanguage();
  }

  @Override
  public List<String> getMediaInfoAudioLanguageList() {
    List<String> lang = new ArrayList<>(getMainVideoFile().getAudioLanguagesList());

    for (MediaFile mf : getMediaFiles(MediaFileType.AUDIO)) {
      lang.addAll(mf.getAudioLanguagesList());
    }
    return lang;
  }

  @Override
  public List<String> getMediaInfoSubtitleLanguageList() {
    List<String> lang = new ArrayList<>(getMainVideoFile().getSubtitleLanguagesList());

    for (MediaFile mf : getMediaFiles(MediaFileType.AUDIO, MediaFileType.SUBTITLE)) {
      lang.addAll(mf.getSubtitleLanguagesList());
    }
    return lang;
  }

  @Override
  public List<String> getMediaInfoSubtitleCodecList() {
    List<String> codecs = new ArrayList<>(getMainVideoFile().getSubtitleCodecList());

    for (MediaFile mf : getMediaFiles(MediaFileType.AUDIO, MediaFileType.SUBTITLE)) {
      codecs.addAll(mf.getSubtitleCodecList());
    }
    return codecs;
  }

  @Override
  public String getMediaInfoContainerFormat() {
    return getMainVideoFile().getContainerFormat();
  }

  @Override
  public MediaSource getMediaInfoSource() {
    return getMediaSource();
  }

  @Override
  public long getVideoFilesize() {
    long filesize = 0;
    for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO)) {
      filesize += mf.getFilesize();
    }
    return filesize;
  }

  public String getVideo3DFormat() {
    MediaFile mediaFile = getMainVideoFile();
    if (StringUtils.isNotBlank(mediaFile.getVideo3DFormat())) {
      return mediaFile.getVideo3DFormat();
    }

    if (isVideoIn3D()) { // no MI info, but flag set from user
      return MediaFileHelper.VIDEO_3D;
    }

    return "";
  }

  @Override
  public String getVideoHDRFormat() {
    return getMainVideoFile().getHdrFormat();
  }

  public Boolean isVideoInHDR() {
    return StringUtils.isNotEmpty(getMainVideoFile().getHdrFormat());
  }

  public String getVideoHDR() {
    return isVideoInHDR() ? "HDR" : "";
  }

  public MovieEdition getEdition() {
    return edition;
  }

  public String getEditionAsString() {
    return edition.toString();
  }

  public void setOffline(boolean newValue) {
    boolean oldValue = this.offline;
    this.offline = newValue;
    firePropertyChange("offline", oldValue, newValue);
  }

  public boolean isOffline() {
    return offline;
  }

  public String getCRC32() {
    return getMainVideoFile().getCRC32();
  }

  public void setEdition(MovieEdition newValue) {
    MovieEdition oldValue = this.edition;
    this.edition = newValue;
    firePropertyChange(EDITION, oldValue, newValue);
    firePropertyChange(EDITION_AS_STRING, oldValue, newValue);
  }

  @Override
  public void removeFromMediaFiles(MediaFile mediaFile) {
    super.removeFromMediaFiles(mediaFile);

    boolean dirty = false;

    // also remove from our trailer list
    if (mediaFile.getType() == MediaFileType.TRAILER) {
      for (int i = trailer.size() - 1; i >= 0; i--) {
        MediaTrailer mediaTrailer = trailer.get(i);
        if (mediaTrailer.getUrl().equals(mediaFile.getFileAsPath().toUri().toString())) {
          trailer.remove(mediaTrailer);
          dirty = true;
        }
      }
    }

    if (dirty) {
      firePropertyChange(TRAILER, null, trailer);
    }
  }

  @Override
  protected void fireAddedEventForMediaFile(MediaFile mediaFile) {
    super.fireAddedEventForMediaFile(mediaFile);

    // movie related media file types
    switch (mediaFile.getType()) {
      case TRAILER:
        firePropertyChange(TRAILER, false, true);
        break;

      case SUBTITLE:
        firePropertyChange("hasSubtitle", false, true);
        break;

      default:
        break;

    }
  }

  @Override
  protected void fireRemoveEventForMediaFile(MediaFile mediaFile) {
    super.fireRemoveEventForMediaFile(mediaFile);

    // movie related media file types
    switch (mediaFile.getType()) {
      case TRAILER:
        firePropertyChange(TRAILER, true, false);
        break;

      case SUBTITLE:
        firePropertyChange("hasSubtitle", true, false);
        break;

      default:
        break;

    }
  }

  private void mixinLocalTrailers() {
    // remove local ones
    for (int i = trailer.size() - 1; i >= 0; i--) {
      MediaTrailer mediaTrailer = trailer.get(i);
      if ("downloaded".equalsIgnoreCase(mediaTrailer.getProvider())) {
        trailer.remove(i);
      }
    }

    // add local trailers (in the front)!
    for (MediaFile mf : getMediaFiles(MediaFileType.TRAILER)) {
      LOGGER.debug("adding local trailer {}", mf.getFilename());
      MediaTrailer mt = new MediaTrailer();
      mt.setName(mf.getFilename());
      mt.setProvider("downloaded");
      mt.setQuality(mf.getVideoFormat());
      mt.setInNfo(false);
      mt.setUrl(mf.getFile().toUri().toString());
      trailer.add(0, mt);
      firePropertyChange(TRAILER, null, trailer);
    }
  }

  @Override
  public void callbackForGatheredMediainformation(MediaFile mediaFile) {
    boolean dirty = false;

    // upgrade MediaSource to UHD bluray, if video format says so
    if (getMediaSource() == MediaSource.BLURAY && getMainVideoFile().getVideoDefinitionCategory().equals(MediaFileHelper.VIDEO_FORMAT_UHD)) {
      setMediaSource(MediaSource.UHD_BLURAY);
      dirty = true;
    }

    // did we get meta data via the video media file?
    if (mediaFile.getType() == MediaFileType.VIDEO && MovieModuleManager.getInstance().getSettings().isUseMediainfoMetadata()
        && getMediaFiles(MediaFileType.NFO).isEmpty() && !mediaFile.getExtraData().isEmpty()) {

      String title = mediaFile.getExtraData().get("title");
      if (StringUtils.isNotBlank(title)) {
        setTitle(title);
        dirty = true;
      }

      String originalTitle = mediaFile.getExtraData().get("originalTitle");
      if (StringUtils.isNotBlank(originalTitle)) {
        setOriginalTitle(originalTitle);
        dirty = true;
      }

      String year = mediaFile.getExtraData().get("year");
      if (StringUtils.isNotBlank(year)) {
        try {
          int y = Integer.parseInt(year);
          if (y > 1900 && y < 2100) {
            setYear(y);
          }
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      String plot = mediaFile.getExtraData().get("plot");
      if (StringUtils.isNotBlank(plot)) {
        setPlot(plot);
        dirty = true;
      }

      String genre = mediaFile.getExtraData().get("genre");
      if (StringUtils.isNotBlank(genre)) {
        List<MediaGenres> genres = new ArrayList<>();
        for (String part : ParserUtils.split(genre)) {
          genres.add(MediaGenres.getGenre(part));
        }
        addToGenres(genres);
      }

      String date = mediaFile.getExtraData().get("releaseDate");
      if (StringUtils.isNotBlank(date)) {
        setReleaseDate(date);
      }
    }

    if (dirty) {
      saveToDb();
    }

    if (mediaFile.getType() == MediaFileType.TRAILER) {
      // re-write the trailer list
      mixinLocalTrailers();
    }
  }

  public Object getValueForMetadata(MovieScraperMetadataConfig metadataConfig) {

    switch (metadataConfig) {
      case ID:
        return getIds();

      case TITLE:
        return getTitle();

      case ORIGINAL_TITLE:
        return getOriginalTitle();

      case TAGLINE:
        return getTagline();

      case PLOT:
        return getPlot();

      case YEAR:
        return getYear();

      case RELEASE_DATE:
        return getReleaseDate();

      case RATING:
        return getRatings();

      case TOP250:
        return getTop250();

      case RUNTIME:
        return getRuntime();

      case CERTIFICATION:
        return getCertification();

      case GENRES:
        return getGenres();

      case SPOKEN_LANGUAGES:
        return getSpokenLanguages();

      case COUNTRY:
        return getCountry();

      case PRODUCTION_COMPANY:
        return getProductionCompany();

      case TAGS:
        return getTags();

      case COLLECTION:
        return getMovieSet();

      case TRAILER:
        return getTrailer();

      case ACTORS:
        return getActors();

      case PRODUCERS:
        return getProducers();

      case DIRECTORS:
        return getDirectors();

      case WRITERS:
        return getWriters();

      case POSTER:
        return getMediaFiles(MediaFileType.POSTER);

      case FANART:
        return getMediaFiles(MediaFileType.FANART);

      case BANNER:
        return getMediaFiles(MediaFileType.BANNER);

      case CLEARART:
        return getMediaFiles(MediaFileType.CLEARART);

      case THUMB:
        return getMediaFiles(MediaFileType.THUMB);

      case LOGO, CLEARLOGO: // LOGO = legacy
        return getMediaFiles(MediaFileType.CLEARLOGO);

      case DISCART:
        return getMediaFiles(MediaFileType.DISC);

      case KEYART:
        return getMediaFiles(MediaFileType.KEYART);

      case EXTRAFANART:
        return getMediaFiles(MediaFileType.EXTRAFANART);

      case EXTRATHUMB:
        return getMediaFiles(MediaFileType.EXTRATHUMB);
    }

    return null;
  }

  protected void postProcess(List<MovieScraperMetadataConfig> config, boolean overwriteExistingItems) {
    TmmTaskChain taskChain = TmmTaskChain.getInstance(this);

    if (MovieModuleManager.getInstance().getSettings().isRenameAfterScrape()) {
      taskChain.add(new MovieRenameTask(Collections.singletonList(this)));
    }

    // write actor images after possible rename (to have a good folder structure)
    if (ScraperMetadataConfig.containsAnyCast(config) && MovieModuleManager.getInstance().getSettings().isWriteActorImages()) {
      taskChain.add(new TmmTask(TmmResourceBundle.getString("movie.downloadactorimages"), 1, TmmTaskHandle.TaskType.BACKGROUND_TASK) {
        @Override
        protected void doInBackground() {
          writeActorImages(overwriteExistingItems);
        }
      });
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Movie movie = (Movie) o;
    return path.equals(movie.path) && getMainFile().getFile().equals(movie.getMainFile().getFile());
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, getMainFile().getFile());
  }
}
