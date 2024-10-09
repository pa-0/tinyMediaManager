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
package org.tinymediamanager.core.tvshow.entities;

import static org.tinymediamanager.core.Constants.ACTORS;
import static org.tinymediamanager.core.Constants.ACTORS_AS_STRING;
import static org.tinymediamanager.core.Constants.DIRECTORS;
import static org.tinymediamanager.core.Constants.DIRECTORS_AS_STRING;
import static org.tinymediamanager.core.Constants.EDITION;
import static org.tinymediamanager.core.Constants.EDITION_AS_STRING;
import static org.tinymediamanager.core.Constants.EPISODE;
import static org.tinymediamanager.core.Constants.FIRST_AIRED;
import static org.tinymediamanager.core.Constants.FIRST_AIRED_AS_STRING;
import static org.tinymediamanager.core.Constants.HAS_NFO_FILE;
import static org.tinymediamanager.core.Constants.MEDIA_SOURCE;
import static org.tinymediamanager.core.Constants.SEASON;
import static org.tinymediamanager.core.Constants.SEASON_BANNER;
import static org.tinymediamanager.core.Constants.SEASON_POSTER;
import static org.tinymediamanager.core.Constants.SEASON_THUMB;
import static org.tinymediamanager.core.Constants.TITLE_FOR_UI;
import static org.tinymediamanager.core.Constants.TITLE_SORTABLE;
import static org.tinymediamanager.core.Constants.TV_SHOW;
import static org.tinymediamanager.core.Constants.WATCHED;
import static org.tinymediamanager.core.Constants.WRITERS;
import static org.tinymediamanager.core.Constants.WRITERS_AS_STRING;
import static org.tinymediamanager.core.Utils.returnOneWhenFilled;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroupType.ABSOLUTE;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroupType.AIRED;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroupType.DISPLAY;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroupType.DVD;

import java.io.File;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.IMediaInformation;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tasks.MediaEntityImageFetcherTask;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskChain;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowEpisodeEdition;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowMediaFileComparator;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.connector.ITvShowEpisodeConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowEpisodeToEmbyConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowEpisodeToJellyfinConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowEpisodeToKodiConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowEpisodeToXbmcConnector;
import org.tinymediamanager.core.tvshow.filenaming.TvShowEpisodeNfoNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowEpisodeThumbNaming;
import org.tinymediamanager.core.tvshow.tasks.TvShowActorImageFetcherTask;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.StrgUtils;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * The Class TvShowEpisode.
 * 
 * @author Manuel Laggner
 */
public class TvShowEpisode extends MediaEntity implements Comparable<TvShowEpisode>, IMediaInformation {
  private static final Logger                LOGGER                = LoggerFactory.getLogger(TvShowEpisode.class);
  private static final Comparator<MediaFile> MEDIA_FILE_COMPARATOR = new TvShowMediaFileComparator();

  @JsonProperty
  private final List<MediaEpisodeNumber>     episodeNumbers        = new CopyOnWriteArrayList<>();
  @JsonProperty
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private Date                               firstAired            = null;
  @JsonProperty
  private boolean                            disc                  = false;
  @JsonProperty
  private boolean                            multiEpisode          = false;
  @JsonProperty
  private boolean                            watched               = false;
  @JsonProperty
  private int                                playcount             = 0;
  @JsonProperty
  private UUID                               tvShowId              = null;
  @JsonProperty
  private MediaSource                        mediaSource           = MediaSource.UNKNOWN;
  @JsonProperty
  private TvShowEpisodeEdition               edition               = TvShowEpisodeEdition.NONE;
  @JsonProperty
  private boolean                            stacked               = false;

  @JsonProperty
  private final List<Person>                 actors                = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<Person>                 directors             = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<Person>                 writers               = new CopyOnWriteArrayList<>();

  private TvShow                             tvShow                = null;
  private String                             titleSortable         = "";
  private String                             otherIds              = "";
  private Date                               lastWatched           = null;
  private boolean                            dummy                 = false;
  private MediaEpisodeNumber                 mainEpisodeNumber     = null;

  // LEGACY
  @JsonIgnore
  public Map<String, Object>                 additionalProperties  = new HashMap<>();

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  /**
   * Instantiates a new tv show episode. To initialize the propertychangesupport after loading
   */
  public TvShowEpisode() {
    // register for dirty flag listener
    super();
  }

  /**
   * create a deep copy of this episode
   *
   * @param source
   *          the source episode
   */
  public TvShowEpisode(@NotNull TvShowEpisode source) {
    // the reference to the tv show and the media files are the only things we don't
    // copy
    tvShow = source.tvShow;
    tvShowId = source.tvShowId;

    // clone media files
    for (MediaFile mf : source.getMediaFiles()) {
      addToMediaFiles(new MediaFile(mf));
    }

    // clone the rest
    path = source.path;
    title = source.title;
    originalTitle = source.originalTitle;
    year = source.year;
    plot = source.plot;

    artworkUrlMap.putAll(source.artworkUrlMap);

    dateAdded = new Date(source.dateAdded.getTime());
    ids.putAll(source.ids);
    mediaSource = source.mediaSource;
    edition = source.edition;

    episodeNumbers.addAll(source.episodeNumbers);

    if (source.firstAired != null) {
      firstAired = new Date(source.firstAired.getTime());
    }

    disc = source.disc;
    stacked = source.stacked;
    multiEpisode = source.multiEpisode;
    titleSortable = source.titleSortable;
    otherIds = source.otherIds;
    lastWatched = source.lastWatched;
    watched = source.watched;
    playcount = source.playcount;

    for (Person actor : source.getActors()) {
      actors.add(new Person(actor));
    }
    for (Person director : source.getDirectors()) {
      directors.add(new Person(director));
    }
    for (Person writer : source.getWriters()) {
      writers.add(new Person(writer));
    }
    for (MediaRating mediaRating : source.getRatings().values()) {
      ratings.put(mediaRating.getId(), new MediaRating(mediaRating));
    }
    tags.addAll(source.tags);
    originalFilename = source.originalFilename;
    productionCompany = source.productionCompany;
    dummy = source.dummy;
    note = source.note;
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
        case MediaMetadata.TVDB, MediaMetadata.IMDB, MediaMetadata.TMDB, MediaMetadata.TRAKT_TV:
          // already in UI - skip
          continue;

        case "imdbId", "tvShowSeason":
          // legacy format
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

  /**
   * Overwrites all null/empty elements with "other" value (but might be also empty)<br>
   * For lists, check with 'contains' and add.<br>
   * Do NOT merge path, dateAdded, scraped, mediaFiles and other crucial properties!
   *
   * @param other
   *          the other episode to merge in
   */
  public void merge(TvShowEpisode other) {
    merge(other, false);
  }

  /**
   * Overwrites all elements with "other" value<br>
   * Do NOT merge path, dateAdded, scraped, mediaFiles and other crucial properties!
   *
   * @param other
   *          the other episode to merge in
   */
  public void forceMerge(TvShowEpisode other) {
    merge(other, true);
  }

  void merge(TvShowEpisode other, boolean force) {
    if (locked || other == null) {
      return;
    }
    super.merge(other, force);

    setFirstAired(firstAired == null || force ? other.firstAired : firstAired);
    setWatched(!watched || force ? other.watched : watched);
    setPlaycount(playcount == 0 || force ? other.playcount : playcount);
    setMediaSource(mediaSource == MediaSource.UNKNOWN || force ? other.mediaSource : mediaSource);
    setEdition(edition == TvShowEpisodeEdition.NONE || force ? other.edition : edition);

    if (force) {
      actors.clear();
      directors.clear();
      writers.clear();
    }

    setActors(other.actors);
    setDirectors(other.directors);
    setWriters(other.writers);

    episodeNumbers.clear();
    episodeNumbers.addAll(other.episodeNumbers);
  }

  @Override
  protected Comparator<MediaFile> getMediaFileComparator() {
    return MEDIA_FILE_COMPARATOR;
  }

  /**
   * checks whether the parent {@link TvShow} is locked
   *
   * @return true/false
   */
  @Override
  public boolean isLocked() {
    return getTvShow() == null || getTvShow().isLocked();
  }

  /**
   * (re)sets the path (when renaming tv show folder).<br>
   * Exchanges the beginning path from oldPath with newPath<br>
   */
  public void replacePathForRenamedTvShowRoot(Path oldPath, Path newPath) {
    if (oldPath == null || newPath == null) {
      return;
    }

    Path newPathToSet;
    if (oldPath.equals(getPathNIO())) {
      // episode is in TV show root -> just exchange
      newPathToSet = newPath;
    }
    else {
      Path subPath = oldPath.relativize(getPathNIO()); // path relative to the TV show root
      newPathToSet = newPath.resolve(subPath);
    }

    LOGGER.trace("EP replace: ({}, {}) -> {} results in {}", oldPath, newPath, getPath(), newPathToSet);
    setPath(newPathToSet.toAbsolutePath().toString());
  }

  /**
   * Returns the sortable variant of title<br>
   * eg "The Luminous Fish Effect" -> "Luminous Fish Effect, The".
   *
   * @return the title in its sortable format
   */
  public String getTitleSortable() {
    if (StringUtils.isBlank(titleSortable)) {
      titleSortable = Utils.getSortableName(getTitle());
    }
    return titleSortable;
  }

  public void clearTitleSortable() {
    titleSortable = "";
  }

  public Date getFirstAired() {
    return firstAired;
  }

  @JsonIgnore
  public void setFirstAired(Date newValue) {
    Date oldValue = this.firstAired;
    this.firstAired = newValue;
    firePropertyChange(FIRST_AIRED, oldValue, newValue);
    firePropertyChange(FIRST_AIRED_AS_STRING, oldValue, newValue);

    // also set the year
    if (firstAired != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(firstAired);
      setYear(calendar.get(Calendar.YEAR));
    }
  }

  @Override
  public Date getReleaseDate() {
    return getFirstAired();
  }

  public TvShowSeason getTvShowSeason() {
    if (tvShow == null) {
      return null;
    }
    return tvShow.getSeasonForEpisode(this);
  }

  public void setTvShowSeason() {
    // dummy for beansbinding
  }

  /**
   * Is this episode in a disc folder structure?.
   * 
   * @return true/false
   */
  public boolean isDisc() {
    return disc;
  }

  /**
   * This episode is in a disc folder structure.
   * 
   * @param disc
   *          true/false
   */
  public void setDisc(boolean disc) {
    this.disc = disc;
  }

  /**
   * is this Episode a MultiEpisode? (same files added to another episode?)
   * 
   * @return true/false
   */
  public boolean isMultiEpisode() {
    return multiEpisode;
  }

  public void setMultiEpisode(boolean multiEpisode) {
    this.multiEpisode = multiEpisode;
  }

  /**
   * first aired date as yyyy-mm-dd<br>
   * https://xkcd.com/1179/ :P
   * 
   * @return the date or empty string
   */
  public String getFirstAiredFormatted() {
    if (this.firstAired == null) {
      return "";
    }
    return new SimpleDateFormat("yyyy-MM-dd").format(this.firstAired);
  }

  /**
   * Gets the first aired as a string, formatted in the system locale.
   * 
   * @return the first aired as string
   */
  public String getFirstAiredAsString() {
    if (this.firstAired == null) {
      return "";
    }
    return TmmDateFormat.MEDIUM_DATE_FORMAT.format(firstAired);
  }

  /**
   * convenient method to set the first aired date (parsed from string).
   * 
   * @param aired
   *          the new first aired
   */
  public void setFirstAired(String aired) {
    try {
      setFirstAired(StrgUtils.parseDate(aired));
    }
    catch (ParseException ignored) {
    }
  }

  public TvShow getTvShow() {
    return tvShow;
  }

  public void setTvShow(TvShow newValue) {
    TvShow oldValue = this.tvShow;
    this.tvShow = newValue;
    this.tvShowId = tvShow.getDbId();
    firePropertyChange(TV_SHOW, oldValue, newValue);
  }

  public UUID getTvShowDbId() {
    return tvShowId;
  }

  @Override
  public String getDataSource() {
    return tvShow.getDataSource();
  }

  public List<MediaEpisodeNumber> getEpisodeNumbers() {
    return Collections.unmodifiableList(episodeNumbers);
  }

  public void setEpisodeNumbers(Map<MediaEpisodeGroup, MediaEpisodeNumber> newValues) {
    episodeNumbers.clear();
    mainEpisodeNumber = null;

    if (newValues != null) {
      MediaEpisodeGroup tvShowEpisodeGroup = tvShow != null ? tvShow.getEpisodeGroup() : null;

      // sort by episode groups (same order as in MediaEpisodeGroup.EpisodeGroup)
      for (MediaEpisodeGroup.EpisodeGroupType eg : MediaEpisodeGroup.EpisodeGroupType.values()) {
        List<MediaEpisodeNumber> episodeNumbersForType = new ArrayList<>();

        // special logic: the chosen episode group must be the first one in the list
        if (tvShowEpisodeGroup != null && tvShowEpisodeGroup.getEpisodeGroupType() == eg) {
          MediaEpisodeNumber episodeNumber = newValues.get(tvShowEpisodeGroup);
          if (episodeNumber != null) {
            episodeNumbersForType.add(episodeNumber);
          }
        }

        // add all (remaining) episode numbers for this type
        newValues.forEach((group, episodeNumber) -> {
          if (group.getEpisodeGroupType() == eg && !episodeNumbersForType.contains(episodeNumber)) {
            episodeNumbersForType.add(episodeNumber);
          }
        });

        // and set them in the right order to the episode
        episodeNumbersForType.forEach(this::setEpisode);
      }
    }
  }

  public MediaEpisodeGroup getEpisodeGroup() {
    if (getTvShow() != null) {
      return getTvShow().getEpisodeGroup();
    }
    else {
      return MediaEpisodeGroup.DEFAULT_AIRED;
    }
  }

  /**
   * get the S/E number with the default {@link MediaEpisodeGroup}
   *
   * @return the S/E number (or null when not available)
   */
  public MediaEpisodeNumber getEpisodeNumber() {
    // use cache for faster lookup!
    if (mainEpisodeNumber == null) {
      mainEpisodeNumber = getEpisodeNumber(getEpisodeGroup());
    }

    return mainEpisodeNumber;
  }

  /**
   * get the S/E number for the given {@link MediaEpisodeGroup}
   *
   * @return the S/E number (or null when not available)
   */
  public MediaEpisodeNumber getEpisodeNumber(@NotNull MediaEpisodeGroup episodeGroup) {
    MediaEpisodeNumber episodeNumber = null;

    for (MediaEpisodeNumber mediaEpisodeNumber : episodeNumbers) {
      if (mediaEpisodeNumber.episodeGroup().equals(episodeGroup)) {
        episodeNumber = mediaEpisodeNumber;
        break;
      }
    }

    // legacy fallback
    if (episodeNumber == null && episodeGroup.getEpisodeGroupType() == AIRED) {
      for (MediaEpisodeNumber mediaEpisodeNumber : episodeNumbers) {
        if (mediaEpisodeNumber.episodeGroup().getEpisodeGroupType() == AIRED) {
          episodeNumber = mediaEpisodeNumber;
          break;
        }
      }
    }
    else if (episodeNumber == null && episodeGroup.getEpisodeGroupType() == ABSOLUTE) {
      for (MediaEpisodeNumber mediaEpisodeNumber : episodeNumbers) {
        if (mediaEpisodeNumber.episodeGroup().getEpisodeGroupType() == ABSOLUTE) {
          episodeNumber = mediaEpisodeNumber;
          break;
        }
      }
    }

    return episodeNumber;
  }

  /**
   * get the S/E number for the given {@link MediaEpisodeGroup.EpisodeGroupType}
   *
   * @return the S/E number (or null when not available)
   */
  private MediaEpisodeNumber getEpisodeNumber(@NotNull MediaEpisodeGroup.EpisodeGroupType episodeGroupType) {
    for (MediaEpisodeNumber mediaEpisodeNumber : episodeNumbers) {
      if (mediaEpisodeNumber.episodeGroup().getEpisodeGroupType() == episodeGroupType) {
        return mediaEpisodeNumber;
      }
    }

    return null;
  }

  /**
   * get the episode number in the given {@link MediaEpisodeGroup.EpisodeGroupType}
   *
   * @param episodeGroupType
   *          the {@link MediaEpisodeGroup.EpisodeGroupType} to get the episode number for
   * @return the episode number in the given {@link MediaEpisodeGroup.EpisodeGroupType} or -1
   */
  public int getEpisode(MediaEpisodeGroup.EpisodeGroupType episodeGroupType) {
    MediaEpisodeNumber episodeNumber = getEpisodeNumber(episodeGroupType);

    if (episodeNumber != null) {
      return episodeNumber.episode();
    }
    return -1;
  }

  /**
   * get the episode number in the given {@link MediaEpisodeGroup}
   *
   * @param mediaEpisodeGroup
   *          the {@link MediaEpisodeGroup} to get the episode number for
   * @return the episode number in the given {@link MediaEpisodeGroup} or -1
   */
  public int getEpisode(MediaEpisodeGroup mediaEpisodeGroup) {
    MediaEpisodeNumber episodeNumber = getEpisodeNumber(mediaEpisodeGroup);

    if (episodeNumber != null) {
      return episodeNumber.episode();
    }
    return -1;
  }

  /**
   * get the episode number in the default {@link MediaEpisodeGroup.EpisodeGroupType}
   *
   * @return the episode number in the default {@link MediaEpisodeGroup.EpisodeGroupType} or -1
   */
  public int getEpisode() {
    return getEpisode(getEpisodeGroup());
  }

  /**
   * get the season number in the default {@link MediaEpisodeGroup.EpisodeGroupType}
   *
   * @return the season number in the default {@link MediaEpisodeGroup.EpisodeGroupType} or -1
   */
  public int getSeason() {
    return getSeason(getEpisodeGroup());
  }

  public void setEpisode(@NotNull MediaEpisodeNumber episode) {
    if (!episode.containsAnyNumber()) {
      List<MediaEpisodeNumber> toRemove = new ArrayList<>();

      for (MediaEpisodeNumber mediaEpisodeNumber : episodeNumbers) {
        if (mediaEpisodeNumber.episodeGroup().equals(episode.episodeGroup())) {
          toRemove.add(mediaEpisodeNumber);
        }
      }

      if (!toRemove.isEmpty()) {
        episodeNumbers.removeAll(toRemove);
        firePropertyChange(EPISODE, 0, -1);
        firePropertyChange(SEASON, 0, -1);
        firePropertyChange(TITLE_FOR_UI, -1, episode.episode());
        return;
      }
    }

    // remove the given EG is needed
    MediaEpisodeNumber existingEpisodeNumber = null;
    for (MediaEpisodeNumber mediaEpisodeNumber : episodeNumbers) {
      if (mediaEpisodeNumber.episodeGroup().equals(episode.episodeGroup())) {
        existingEpisodeNumber = mediaEpisodeNumber;
        break;
      }

    }

    if (existingEpisodeNumber != null) {
      int index = episodeNumbers.indexOf(existingEpisodeNumber);
      episodeNumbers.remove(existingEpisodeNumber);
      if (index >= 0) {
        episodeNumbers.add(index, episode);
      }
      else {
        episodeNumbers.add(episode);
      }
    }
    else {
      episodeNumbers.add(episode);
    }
    firePropertyChange(EPISODE, -1, episode.episode());
    firePropertyChange(SEASON, -1, episode.season());
    firePropertyChange(TITLE_FOR_UI, -1, episode.episode());
  }

  /**
   * convenience method to get the episode number in AIRED order
   *
   * @return the episode number (if found) or -1
   */
  public int getAiredEpisode() {
    return getEpisode(AIRED);
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

  /**
   * get the season number in the given {@link MediaEpisodeGroup.EpisodeGroupType}
   *
   * @param episodeGroupType
   *          the {@link MediaEpisodeGroup.EpisodeGroupType} to get the season number for
   * @return the season number in the given {@link MediaEpisodeGroup.EpisodeGroupType} or -1
   */
  public int getSeason(MediaEpisodeGroup.EpisodeGroupType episodeGroupType) {
    MediaEpisodeNumber episodeNumber = getEpisodeNumber(episodeGroupType);

    if (episodeNumber != null) {
      return episodeNumber.season();
    }
    return -1;
  }

  /**
   * get the season number in the given {@link MediaEpisodeGroup}
   *
   * @param mediaEpisodeGroup
   *          the {@link MediaEpisodeGroup} to get the season number for
   * @return the season number in the given {@link MediaEpisodeGroup} or -1
   */
  public int getSeason(MediaEpisodeGroup mediaEpisodeGroup) {
    MediaEpisodeNumber episodeNumber = getEpisodeNumber(mediaEpisodeGroup);

    if (episodeNumber != null) {
      return episodeNumber.season();
    }
    return -1;
  }

  /**
   * get the Trakt ID
   *
   * @return the Trakt ID
   */
  public int getTraktTvId() {
    return this.getIdAsInt(MediaMetadata.TRAKT_TV);
  }

  /**
   * get the IMDB ID
   *
   * @return IMDB ID
   */
  public String getImdbId() {
    return this.getIdAsString(MediaMetadata.IMDB);
  }

  /**
   * Get the TMDB ID
   *
   * @return the TMDB ID
   */
  public String getTmdbId() {
    return this.getIdAsString(MediaMetadata.TMDB);
  }

  /**
   * convenience method to get the season number in AIRED order
   *
   * @return the season number (if found) or -1
   */
  public int getAiredSeason() {
    return getSeason(AIRED);
  }

  /**
   * get the "main" rating
   *
   * @return the main (preferred) rating
   */
  @Override
  public MediaRating getRating() {
    MediaRating mediaRating = null;

    for (String ratingSource : TvShowModuleManager.getInstance().getSettings().getRatingSources()) {
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
   * Gets the title for ui.
   * 
   * @return the title for ui
   */
  public String getTitleForUi() {
    StringBuilder titleForUi = new StringBuilder();
    int episode = getEpisode();
    int season = getSeason();
    if (episode > 0 && season > 0) {
      titleForUi.append(String.format("S%02dE%02d - ", season, episode));
    }
    titleForUi.append(title);
    return titleForUi.toString();
  }

  /**
   * download the specified type of artwork for this episode - async per default
   *
   * @param type
   *          the chosen artwork type to be downloaded
   */
  public void downloadArtwork(MediaFileType type) {
    downloadArtwork(type, true);
  }

  /**
   * download the specified type of artwork for this episode
   *
   * @param type
   *          the chosen artwork type to be downloaded
   * @param async
   *          download the artwork sync or async
   */
  public void downloadArtwork(MediaFileType type, boolean async) {
    switch (type) {
      case THUMB:
        writeThumbImage(async);
        break;

      default:
        break;
    }
  }

  /**
   * Write thumb image.
   */
  private void writeThumbImage(boolean async) {
    String thumbUrl = getArtworkUrl(MediaFileType.THUMB);
    if (StringUtils.isNotBlank(thumbUrl)) {
      // create correct filename
      MediaFile mf = getMediaFiles(MediaFileType.VIDEO).get(0);
      String basename = FilenameUtils.getBaseName(mf.getFilename());

      List<String> filenames = new ArrayList<>();
      for (TvShowEpisodeThumbNaming thumbNaming : TvShowModuleManager.getInstance().getSettings().getEpisodeThumbFilenames()) {
        String filename = thumbNaming.getFilename(basename, Utils.getArtworkExtensionFromUrl(thumbUrl));
        if (StringUtils.isBlank(filename)) {
          continue;
        }
        if (isDisc()) {
          filename = "thumb." + FilenameUtils.getExtension(thumbUrl); // DVD/BluRay fixate to thumb.ext
        }

        filenames.add(filename);
      }

      if (!filenames.isEmpty()) {
        // get images in thread
        MediaEntityImageFetcherTask task = new MediaEntityImageFetcherTask(this, thumbUrl, MediaArtworkType.THUMB, filenames);
        if (async) {
          TmmTaskManager.getInstance().addImageDownloadTask(task);
        }
        else {
          task.run();
        }
      }
    }

    // if that has been a local file, remove it from the artwork urls after we've
    // already started the download(copy) task
    if (thumbUrl.startsWith("file:")) {
      removeArtworkUrl(MediaFileType.THUMB);
    }
  }

  /**
   * Sets the metadata.
   * 
   * @param metadata
   *          the new metadata
   */
  public void setMetadata(MediaMetadata metadata, List<TvShowEpisodeScraperMetadataConfig> config, boolean overwriteExistingItems) {
    if (locked) {
      LOGGER.debug("episode locked, but setMetadata has been called!");
      return;
    }

    // check against null metadata (e.g. aborted request)
    if (metadata == null) {
      LOGGER.error("metadata was null");
      return;
    }

    // populate ids

    // here we have two flavors:
    // a) we did a search, so all existing ids should be different to to new ones -> remove old ones
    // b) we did just a scrape (probably with another scraper). we should have at least one id in the episode which matches the ids from the metadata
    // ->
    // merge ids

    // search for existing ids
    boolean matchFound = false;
    for (Map.Entry<String, Object> entry : metadata.getIds().entrySet()) {
      if (entry.getValue() != null && entry.getValue().equals(getId(entry.getKey()))) {
        matchFound = true;
        break;
      }
    }

    if (!matchFound && overwriteExistingItems) {
      // clear the old ids to set only the new ones
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

    if (config.contains(TvShowEpisodeScraperMetadataConfig.TITLE) && StringUtils.isNotBlank(metadata.getTitle())
        && (overwriteExistingItems || StringUtils.isBlank(getTitle()))) {
      // Capitalize first letter of original title if setting is set!
      if (TvShowModuleManager.getInstance().getSettings().getCapitalWordsInTitles()) {
        setTitle(WordUtils.capitalize(metadata.getTitle()));
      }
      else {
        setTitle(metadata.getTitle());
      }
    }

    if (config.contains(TvShowEpisodeScraperMetadataConfig.ORIGINAL_TITLE) && (overwriteExistingItems || StringUtils.isBlank(getOriginalTitle()))) {
      // Capitalize first letter of original title if setting is set!
      if (TvShowModuleManager.getInstance().getSettings().getCapitalWordsInTitles()) {
        setOriginalTitle(WordUtils.capitalize(metadata.getOriginalTitle()));
      }
      else {
        setOriginalTitle(metadata.getOriginalTitle());
      }
    }

    if (config.contains(TvShowEpisodeScraperMetadataConfig.PLOT) && (overwriteExistingItems || StringUtils.isBlank(getPlot()))) {
      setPlot(metadata.getPlot());
    }

    if (config.contains(TvShowEpisodeScraperMetadataConfig.SEASON_EPISODE) && (overwriteExistingItems || getEpisodeNumbers().isEmpty())) {
      // only set episode groups which are available in the TV show itself (e.g. when scraped via different scraper or w/o scraping TV show prior)
      Map<MediaEpisodeGroup, MediaEpisodeNumber> newEpisodeNumbers = new HashMap<>();
      for (var entry : metadata.getEpisodeNumbers().entrySet()) {
        if (!tvShow.getEpisodeGroups().contains(entry.getKey())) {
          // add this EG to the show too (not for AIRED! we use the fallback here)
          if (entry.getKey().getEpisodeGroupType() == AIRED) {
            continue;
          }

          tvShow.addEpisodeGroup(entry.getKey());
        }
        newEpisodeNumbers.put(entry.getKey(), entry.getValue());
      }

      if (newEpisodeNumbers.isEmpty()) {
        // try at least to match the AIRED order
        for (var entry : metadata.getEpisodeNumbers().entrySet()) {
          if (entry.getKey().getEpisodeGroupType() == AIRED) {
            MediaEpisodeGroup aired = null;
            for (MediaEpisodeGroup episodeGroup : tvShow.getEpisodeGroups()) {
              if (episodeGroup.getEpisodeGroupType() == AIRED) {
                aired = episodeGroup;
                break;
              }
            }

            if (aired != null) {
              newEpisodeNumbers.put(aired, entry.getValue());
            }
          }
        }
      }

      setEpisodeNumbers(newEpisodeNumbers);
    }

    if (config.contains(TvShowEpisodeScraperMetadataConfig.AIRED) && (overwriteExistingItems || getFirstAired() == null)) {
      setFirstAired(metadata.getReleaseDate());
    }

    if (config.contains(TvShowEpisodeScraperMetadataConfig.RATING)) {
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

    // 1:n relations are either merged (no overwrite) or completely set with the new data

    if (config.contains(TvShowEpisodeScraperMetadataConfig.TAGS)) {
      if (!matchFound || overwriteExistingItems) {
        removeAllTags();
      }

      addToTags(metadata.getTags());
    }

    if (config.contains(TvShowEpisodeScraperMetadataConfig.ACTORS)) {
      if (!matchFound || overwriteExistingItems) {
        actors.clear();
      }
      setActors(metadata.getCastMembers(Person.Type.ACTOR));
    }

    if (config.contains(TvShowEpisodeScraperMetadataConfig.DIRECTORS)) {
      if (!matchFound || overwriteExistingItems) {
        directors.clear();
      }
      setDirectors(metadata.getCastMembers(Person.Type.DIRECTOR));
    }

    if (config.contains(TvShowEpisodeScraperMetadataConfig.WRITERS)) {
      if (!matchFound || overwriteExistingItems) {
        writers.clear();
      }
      setWriters(metadata.getCastMembers(Person.Type.WRITER));
    }

    // update DB
    writeNFO();
    saveToDb();

    postProcess(config, overwriteExistingItems);
  }

  /**
   * Write nfo.
   */
  public void writeNFO() {
    List<TvShowEpisodeNfoNaming> nfoNamings = TvShowModuleManager.getInstance().getSettings().getEpisodeNfoFilenames();
    if (nfoNamings.isEmpty()) {
      LOGGER.info("Not writing any NFO file, because NFO filename preferences were empty...");
      return;
    }

    List<TvShowEpisode> episodesInNfo = new ArrayList<>(1);

    LOGGER.debug("write nfo: " + getTvShow().getTitle() + " S" + getSeason() + "E" + getEpisode());
    // worst case: multi episode in multiple files
    // e.g. warehouse13.s01e01e02.Part1.avi/warehouse13.s01e01e02.Part2.avi
    for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO)) {
      List<TvShowEpisode> eps = new ArrayList<>(TvShowList.getTvEpisodesByFile(tvShow, mf.getFile()));
      for (TvShowEpisode ep : eps) {
        if (!episodesInNfo.contains(ep)) {
          episodesInNfo.add(ep);
        }
      }
    }

    ITvShowEpisodeConnector connector = switch (TvShowModuleManager.getInstance().getSettings().getTvShowConnector()) {
      case XBMC -> new TvShowEpisodeToXbmcConnector(episodesInNfo);
      case EMBY -> new TvShowEpisodeToEmbyConnector(episodesInNfo);
      case JELLYFIN -> new TvShowEpisodeToJellyfinConnector(episodesInNfo);
      default -> new TvShowEpisodeToKodiConnector(episodesInNfo);
    };

    try {
      connector.write(Collections.singletonList(TvShowEpisodeNfoNaming.FILENAME));

      firePropertyChange(HAS_NFO_FILE, false, true);
    }
    catch (Exception e) {
      LOGGER.error("could not write NFO file - '{}'", e.getMessage());
    }
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
    firePropertyChange(ACTORS, null, actors);
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
   * get the actors. These are the main actors of the TV show inclusive the guests of this episode
   *
   * @return the actors of this episode
   */
  public List<Person> getActors() {
    return actors;
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
   * Sets the actors.
   * 
   * @param newActors
   *          the new actors
   */
  @JsonSetter
  public void setActors(List<Person> newActors) {
    // two way sync of actors
    mergePersons(actors, newActors);
    firePropertyChange(ACTORS, null, getActors());
    firePropertyChange(ACTORS_AS_STRING, null, getActorsAsString());
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
   * remove the all directors.
   */
  public void removeDirectors() {
    directors.clear();
    firePropertyChange(DIRECTORS, null, this.getDirectors());
    firePropertyChange(DIRECTORS_AS_STRING, null, this.getDirectorsAsString());
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

    firePropertyChange(DIRECTORS, null, this.getDirectors());
    firePropertyChange(DIRECTORS_AS_STRING, null, this.getDirectorsAsString());
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
    firePropertyChange(WRITERS, null, this.getWriters());
    firePropertyChange(WRITERS_AS_STRING, null, this.getWritersAsString());
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
    firePropertyChange(WRITERS_AS_STRING, null, this.getWritersAsString());
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
   * @return the play count of this episode
   */
  public int getPlaycount() {
    return playcount;
  }

  /**
   * sets the play count
   * 
   * @param newValue
   *          the play count of this episode
   */
  public void setPlaycount(int newValue) {
    int oldValue = this.playcount;
    this.playcount = newValue;
    firePropertyChange("playcount", oldValue, newValue);
  }

  public Date getLastWatched() {
    return lastWatched;
  }

  public void setLastWatched(Date lastWatched) {
    this.lastWatched = lastWatched;
  }

  /**
   * Gets the media info audio codec (i.e mp3) and channels (i.e. 6 at 5.1 sound)
   * 
   * @return the media info audio codec
   */
  public String getMediaInfoAudioCodecAndChannels() {
    List<MediaFile> videos = getMediaFiles(MediaFileType.VIDEO);
    if (!videos.isEmpty()) {
      MediaFile mediaFile = videos.get(0);
      return mediaFile.getAudioCodec() + "_" + mediaFile.getAudioChannels();
    }

    return "";
  }

  /**
   * get all video files for that episode
   *
   * @return a list of all video files
   */
  public List<MediaFile> getVideoFiles() {
    return getMediaFiles(MediaFileType.VIDEO);
  }

  /**
   * Gets the images to cache.
   * 
   * @return the images to cache
   */
  @Override
  public List<MediaFile> getImagesToCache() {
    return getMediaFiles().stream().filter(MediaFile::isGraphic).toList();
  }

  @Override
  public int compareTo(@NotNull TvShowEpisode otherTvShowEpisode) {
    if (getTvShow() != otherTvShowEpisode.getTvShow()) {
      return getTvShow().getTitle().compareTo(otherTvShowEpisode.getTvShow().getTitle());
    }

    if (getSeason() != otherTvShowEpisode.getSeason()) {
      return getSeason() - otherTvShowEpisode.getSeason();
    }

    if (getEpisode() != otherTvShowEpisode.getEpisode()) {
      return getEpisode() - otherTvShowEpisode.getEpisode();
    }

    // still nothing found? wtf - maybe some of those -1/-1 eps
    String filename1 = "";
    try {
      filename1 = getMediaFiles(MediaFileType.VIDEO).get(0).getFilename();
    }
    catch (Exception ignored) {
    }

    String filename2 = "";
    try {
      filename2 = otherTvShowEpisode.getMediaFiles(MediaFileType.VIDEO).get(0).getFilename();
    }
    catch (Exception ignored) {
    }
    return filename1.compareTo(filename2);
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

  public int getRuntimeFromMediaFiles() {
    int runtime = 0;
    for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO)) {
      runtime += mf.getDuration();
    }

    // this EP is in a multi EP file, we calculate the avg. runtime
    if (isMultiEpisode()) {
      List<TvShowEpisode> eps = TvShowList.getTvEpisodesByFile(tvShow, getMainVideoFile().getFile());
      if (!eps.isEmpty()) {
        runtime = (int) (runtime / (float) eps.size());
      }
    }

    return runtime;
  }

  public int getRuntimeFromMediaFilesInMinutes() {
    return getRuntimeFromMediaFiles() / 60;
  }

  @Override
  public synchronized void callbackForWrittenArtwork(MediaArtworkType type) {
    // nothing to do here
  }

  @Override
  public void callbackForGatheredMediainformation(MediaFile mediaFile) {
    boolean dirty = false;

    // upgrade MediaSource to UHD bluray, if video format says so
    if (getMediaSource() == MediaSource.BLURAY && getMainVideoFile().getVideoDefinitionCategory().equals(MediaFileHelper.VIDEO_FORMAT_UHD)) {
      setMediaSource(MediaSource.UHD_BLURAY);
      dirty = true;
    }

    // did we get metadata via the video media file?
    if (mediaFile.getType() == MediaFileType.VIDEO && TvShowModuleManager.getInstance().getSettings().isUseMediainfoMetadata()
        && getMediaFiles(MediaFileType.NFO).isEmpty() && !mediaFile.getExtraData().isEmpty()) {

      if (getAiredEpisode() == -1 || getAiredSeason() == -1) {
        String e = mediaFile.getExtraData().get("episode");
        String s = mediaFile.getExtraData().get("season");
        if (StringUtils.isNoneBlank(e, s)) {
          try {
            setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, Integer.parseInt(s), Integer.parseInt(e)));
            dirty = true;
          }
          catch (Exception ignored) {
            // ignored
          }
        }
      }

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

      String plot = mediaFile.getExtraData().get("plot");
      if (StringUtils.isNotBlank(plot)) {
        setPlot(plot);
        dirty = true;
      }
    }

    if (dirty) {
      saveToDb();
    }
  }

  @Override
  public void saveToDb() {
    // update/insert this episode to the database
    TvShowModuleManager.getInstance().getTvShowList().persistEpisode(this);
  }

  /**
   * Event to trigger a season artwork changed for the UI
   */
  void setSeasonArtworkChanged(MediaArtworkType type) {
    switch (type) {
      case SEASON_POSTER:
        firePropertyChange(SEASON_POSTER, null, "");
        break;

      case SEASON_BANNER:
        firePropertyChange(SEASON_BANNER, null, "");
        break;

      case SEASON_THUMB:
        firePropertyChange(SEASON_THUMB, null, "");
        break;

      default:
        break;
    }
  }

  @Override
  protected float calculateScrapeScore() {
    float score = super.calculateScrapeScore();

    // some fields count multiple times to reach the threshold
    score = score + returnOneWhenFilled(plot);
    score = score + returnOneWhenFilled(originalTitle);
    score = score + returnOneWhenFilled(ratings);

    score = score + 2 * returnOneWhenFilled(firstAired);
    score = score + 2 * returnOneWhenFilled(actors);
    score = score + returnOneWhenFilled(directors);
    score = score + returnOneWhenFilled(writers);
    score = score + returnOneWhenFilled(artworkUrlMap);

    return score;
  }

  /**
   * Gets the tvdb id.
   * 
   * @return the tvdb id
   */
  public String getTvdbId() {
    Object obj = ids.get(MediaMetadata.TVDB);
    if (obj == null) {
      return "";
    }
    return obj.toString();
  }

  public int getDvdSeason() {
    return getSeason(DVD);
  }

  public int getDvdEpisode() {
    return getEpisode(DVD);
  }

  public int getDisplaySeason() {
    return getSeason(DISPLAY);
  }

  public int getDisplayEpisode() {
    return getEpisode(DISPLAY);
  }

  public int getAbsoluteNumber() {
    return getEpisode(ABSOLUTE);
  }

  public MediaSource getMediaSource() {
    return mediaSource;
  }

  public void setMediaSource(MediaSource newValue) {
    MediaSource oldValue = this.mediaSource;
    this.mediaSource = newValue;
    firePropertyChange(MEDIA_SOURCE, oldValue, newValue);
  }

  public TvShowEpisodeEdition getEdition() {
    return edition;
  }

  public String getEditionAsString() {
    return edition.toString();
  }

  public void setEdition(TvShowEpisodeEdition newValue) {
    TvShowEpisodeEdition oldValue = this.edition;
    this.edition = newValue;
    firePropertyChange(EDITION, oldValue, newValue);
    firePropertyChange(EDITION_AS_STRING, oldValue, newValue);
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

  /**
   * <b>PHYSICALLY</b> deletes a complete episode by moving it to datasource backup folder<br>
   * DS\.backup\&lt;name&gt;
   */
  public boolean deleteFilesSafely() {
    boolean result = true;

    List<MediaFile> mediaFiles = getMediaFiles();
    for (MediaFile mf : mediaFiles) {
      if (!mf.deleteSafely(tvShow.getDataSource())) {
        result = false;
      }
    }

    return result;
  }

  @Override
  public MediaCertification getCertification() {
    // we do not have a dedicated certification for the episode
    return null;
  }

  @Override
  public MediaFile getMainVideoFile() {
    MediaFile vid = null;

    if (stacked) {
      // search the first stacked media file (e.g. CD1)
      vid = getMediaFiles(MediaFileType.VIDEO).stream().min(Comparator.comparingInt(MediaFile::getStacking)).orElse(MediaFile.EMPTY_MEDIAFILE);
    }
    else {
      // get the biggest one
      vid = getBiggestMediaFile(MediaFileType.VIDEO);
    }

    if (vid != null) {
      return vid;
    }

    // dummy episodes might not have a video file
    return MediaFile.EMPTY_MEDIAFILE;
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
  public float getMediaInfoAspectRatio() {
    return getMainVideoFile().getAspectRatio();
  }

  public String getMediaInfoAspectRatioAsString() {
    DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
    return df.format(getMainVideoFile().getAspectRatio()).replace(".", "");
  }

  @Override
  public Float getMediaInfoAspectRatio2() {
    return getMainVideoFile().getAspectRatio2();
  }

  public String getMediaInfoAspectRatio2AsString() {
    Float aspectRatio2 = getMainVideoFile().getAspectRatio2();
    String formatedValue = "";
    if (aspectRatio2 != null) {
      DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
      formatedValue = df.format(aspectRatio2).replace(".", "");
    }
    return formatedValue;
  }

  @Override
  public String getMediaInfoAudioCodec() {
    return getMainVideoFile().getAudioCodec();
  }

  @Override
  public List<String> getMediaInfoAudioCodecList() {
    List<String> lang = new ArrayList<>(getMainVideoFile().getAudioCodecList());

    for (MediaFile mf : getMediaFiles(MediaFileType.AUDIO)) {
      lang.addAll(mf.getAudioCodecList());
    }
    return lang;
  }

  @Override
  public double getMediaInfoFrameRate() {
    return getMainVideoFile().getFrameRate();
  }

  @Override
  public String getMediaInfoAudioChannels() {
    return getMainVideoFile().getAudioChannels();
  }

  @Override
  public List<String> getMediaInfoAudioChannelList() {
    List<String> lang = new ArrayList<>(getMainVideoFile().getAudioChannelsList());

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
    List<String> lang = new ArrayList<>(getMainVideoFile().getAudioChannelsDotList());

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
  public int getMediaInfoVideoBitDepth() {
    return getMainVideoFile().getBitDepth();
  }

  public int getMediaInfoVideoBitrate() {
    return getMainVideoFile().getVideoBitRate();
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
    List<MediaFile> videos = getMediaFiles(MediaFileType.VIDEO);
    if (!videos.isEmpty()) {
      MediaFile mediaFile = videos.get(0);
      return mediaFile.getContainerFormat();
    }

    return "";
  }

  @Override
  public MediaSource getMediaInfoSource() {
    return getMediaSource();
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

  @Override
  public boolean isVideoIn3D() {
    String video3DFormat = "";
    List<MediaFile> videos = getMediaFiles(MediaFileType.VIDEO);
    if (!videos.isEmpty()) {
      MediaFile mediaFile = videos.get(0);
      video3DFormat = mediaFile.getVideo3DFormat();
    }

    return StringUtils.isNotBlank(video3DFormat);
  }

  @Override
  public long getVideoFilesize() {
    long filesize = 0;
    for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO)) {
      filesize += mf.getFilesize();
    }
    return filesize;
  }

  public boolean isDummy() {
    return dummy || !hasMediaFiles();
  }

  public void setDummy(boolean dummy) {
    this.dummy = dummy;
  }

  public String getNfoFilename(TvShowEpisodeNfoNaming nfoNaming) {
    MediaFile mainVideoFile = getMainVideoFile();

    String baseName;
    if (isDisc()) {
      // https://kodi.wiki/view/NFO_files/TV_shows#nfo_Name_and_Location_2
      baseName = FilenameUtils.removeExtension(findDiscMainFile());
    }
    else {
      baseName = mainVideoFile.getBasename();
    }

    String filename = nfoNaming.getFilename(baseName, "nfo");
    LOGGER.trace("getNfoFilename: '{}' -> '{}'", mainVideoFile.getFilename(), filename);
    return filename;
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

  /**
   * Is the epsiode "stacked" (more than one video file)
   *
   * @return true if the episode is stacked; false otherwise
   */
  public boolean isStacked() {
    return stacked;
  }

  public void setStacked(boolean stacked) {
    this.stacked = stacked;
  }

  /**
   * get the runtime. Just a wrapper to tvShow.getRuntime() until we support separate runtimes for episodes
   *
   * @return the runtime in minutes
   */
  public int getRuntime() {
    return tvShow.getRuntime();
  }

  /**
   * return the TV shows production company if no one is filled for this episode
   *
   * @return the production company
   */
  @Override
  public String getProductionCompany() {
    if (StringUtils.isNotBlank(productionCompany)) {
      return productionCompany;
    }
    if (tvShow != null) {
      return tvShow.getProductionCompany();
    }
    return "";
  }

  /**
   * Write actor images.
   */
  public void writeActorImages(boolean overwriteExistingItems) {
    TvShowActorImageFetcherTask task = new TvShowActorImageFetcherTask(this);
    task.setOverwriteExistingItems(overwriteExistingItems);
    TmmTaskManager.getInstance().addImageDownloadTask(task);
  }

  /**
   * when exchanging the video from a disc folder to a file, we have to re-evaluate our "disc" folder flag
   */
  public void reEvaluateDiscfolder() {
    boolean disc = false;
    for (MediaFile mf : getMediaFiles()) {
      if (mf.isDiscFile()) {
        disc = true;
      }
    }
    setDisc(disc);
  }

  /**
   * ok, we might have detected some stacking MFs.<br>
   * But if we only have ONE video file, reset stacking markers in this case<br>
   */
  public void reEvaluateStacking() {
    List<MediaFile> mfs = getMediaFiles(MediaFileType.VIDEO);
    if (mfs.size() > 1 && !isDisc()) {
      // ok, more video files means stacking (if not a disc folder)
      this.setStacked(true);
      for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO, MediaFileType.AUDIO, MediaFileType.SUBTITLE)) {
        mf.detectStackingInformation();
      }
    }
    else {
      // only ONE video? remove any stacking markers from MFs
      this.setStacked(false);
      for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO, MediaFileType.AUDIO, MediaFileType.SUBTITLE)) {
        mf.removeStackingInformation();
      }
    }
  }

  @Override
  protected void fireAddedEventForMediaFile(MediaFile mediaFile) {
    super.fireAddedEventForMediaFile(mediaFile);

    // episode related media file types
    if (mediaFile.getType() == MediaFileType.SUBTITLE) {
      firePropertyChange("hasSubtitle", false, true);
    }
  }

  @Override
  protected void fireRemoveEventForMediaFile(MediaFile mediaFile) {
    super.fireRemoveEventForMediaFile(mediaFile);

    // episode related media file types
    if (mediaFile.getType() == MediaFileType.SUBTITLE) {
      firePropertyChange("hasSubtitle", true, false);
    }
  }

  public boolean isUncategorized() {
    return episodeNumbers.isEmpty();
  }

  public boolean getHasNote() {
    return StringUtils.isNotBlank(note);
  }

  public Object getValueForMetadata(TvShowEpisodeScraperMetadataConfig metadataConfig) {

    switch (metadataConfig) {
      case SEASON_EPISODE: {
        return getEpisodeNumbers();
      }

      case TITLE:
        return getTitle();

      case ORIGINAL_TITLE:
        return getOriginalTitle();

      case PLOT:
        return getPlot();

      case AIRED:
        return getFirstAired();

      case RATING:
        return getRatings();

      case TAGS:
        return getTags();

      case ACTORS:
        return getActors();

      case DIRECTORS:
        return getDirectors();

      case WRITERS:
        return getWriters();

      case THUMB:
        return getMediaFiles(MediaFileType.THUMB);
    }

    return null;
  }

  protected void postProcess(List<TvShowEpisodeScraperMetadataConfig> config, boolean overwriteExistingItems) {
    TmmTaskChain taskChain = TmmTaskChain.getInstance(tvShow != null ? tvShow : this);

    // write actor images after possible rename (to have a good folder structure)
    if (ScraperMetadataConfig.containsAnyCast(config) && TvShowModuleManager.getInstance().getSettings().isWriteActorImages()) {
      taskChain.add(new TmmTask(TmmResourceBundle.getString("tvshow.downloadactorimages"), 1, TmmTaskHandle.TaskType.BACKGROUND_TASK) {
        @Override
        protected void doInBackground() {
          writeActorImages(overwriteExistingItems);
        }
      });
    }
  }
}
