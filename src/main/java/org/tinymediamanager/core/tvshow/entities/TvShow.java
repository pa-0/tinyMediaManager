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
package org.tinymediamanager.core.tvshow.entities;

import static org.tinymediamanager.core.Constants.ACTORS;
import static org.tinymediamanager.core.Constants.ACTORS_AS_STRING;
import static org.tinymediamanager.core.Constants.ADDED_EPISODE;
import static org.tinymediamanager.core.Constants.ADDED_SEASON;
import static org.tinymediamanager.core.Constants.CERTIFICATION;
import static org.tinymediamanager.core.Constants.COUNTRY;
import static org.tinymediamanager.core.Constants.EPISODE_COUNT;
import static org.tinymediamanager.core.Constants.FIRST_AIRED;
import static org.tinymediamanager.core.Constants.FIRST_AIRED_AS_STRING;
import static org.tinymediamanager.core.Constants.GENRE;
import static org.tinymediamanager.core.Constants.GENRES_AS_STRING;
import static org.tinymediamanager.core.Constants.HAS_NFO_FILE;
import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;
import static org.tinymediamanager.core.Constants.REMOVED_EPISODE;
import static org.tinymediamanager.core.Constants.REMOVED_SEASON;
import static org.tinymediamanager.core.Constants.RUNTIME;
import static org.tinymediamanager.core.Constants.SEASON;
import static org.tinymediamanager.core.Constants.SEASON_COUNT;
import static org.tinymediamanager.core.Constants.SORT_TITLE;
import static org.tinymediamanager.core.Constants.STATUS;
import static org.tinymediamanager.core.Constants.SUBTITLES;
import static org.tinymediamanager.core.Constants.TAGS;
import static org.tinymediamanager.core.Constants.TITLE_SORTABLE;
import static org.tinymediamanager.core.Constants.TOP250;
import static org.tinymediamanager.core.Constants.TRAILER;
import static org.tinymediamanager.core.Utils.returnOneWhenFilled;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.IMediaInformation;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.ScraperMetadataConfig;
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
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskChain;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowArtworkHelper;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowMediaFileComparator;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowRenamer;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.connector.ITvShowConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowToEmbyConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowToJellyfinConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowToKodiConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowToXbmcConnector;
import org.tinymediamanager.core.tvshow.filenaming.TvShowNfoNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowTrailerNaming;
import org.tinymediamanager.core.tvshow.tasks.TvShowActorImageFetcherTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowRenameTask;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.scraper.util.TvUtils;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * The Class TvShow.
 *
 * @author Manuel Laggner
 */
public class TvShow extends MediaEntity implements IMediaInformation {
  private static final Logger                     LOGGER                     = LoggerFactory.getLogger(TvShow.class);
  private static final Comparator<MediaFile>      MEDIA_FILE_COMPARATOR      = new TvShowMediaFileComparator();

  public static final Pattern                     SEASON_ONLY_PATTERN        = Pattern.compile("^(s|staffel|season|series)[\\s_.-]*(\\d{1,4})$",
      Pattern.CASE_INSENSITIVE);

  @JsonProperty
  private int                                     runtime                    = 0;
  @JsonProperty
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private Date                                    firstAired                 = null;
  @JsonProperty
  private MediaAiredStatus                        status                     = MediaAiredStatus.UNKNOWN;
  @JsonProperty
  private String                                  sortTitle                  = "";
  @JsonProperty
  private MediaCertification                      certification              = MediaCertification.UNKNOWN;
  @JsonProperty
  private String                                  country                    = "";
  @JsonProperty
  private MediaEpisodeGroup                       episodeGroup               = MediaEpisodeGroup.DEFAULT_AIRED;
  @JsonProperty
  private final List<MediaGenres>                 genres                     = new CopyOnWriteArrayList<>();
  @JsonProperty
  private int                                     top250                     = 0;
  @JsonProperty
  private final List<Person>                      actors                     = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<TvShowEpisode>               dummyEpisodes              = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<String>                      extraFanartUrls            = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<MediaTrailer>                trailer                    = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<MediaEpisodeGroup>           episodeGroups              = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final Map<String, Map<Integer, String>> seasonNames                = new HashMap<>();
  @JsonProperty
  private final Map<String, Map<Integer, String>> seasonOverviews            = new HashMap<>();

  private final List<TvShowSeason>                seasons                    = new CopyOnWriteArrayList<>();
  private final List<TvShowEpisode>               episodes                   = new CopyOnWriteArrayList<>();
  private String                                  titleSortable              = "";
  private String                                  otherIds                   = "";
  private Date                                    lastWatched                = null;

  private final PropertyChangeListener            propertyChangeListener;

  private static final Comparator<MediaTrailer>   TRAILER_QUALITY_COMPARATOR = new MediaTrailer.QualityComparator();

  /**
   * Instantiates a tv show. To initialize the propertychangesupport after loading
   */
  public TvShow() {
    // register for dirty flag listener
    super();

    propertyChangeListener = evt -> {
      if (evt.getSource() instanceof TvShowEpisode episode) {

        switch (evt.getPropertyName()) {
          case TAGS, MEDIA_INFORMATION, MEDIA_FILES, SUBTITLES, "hasSubtitles":
            firePropertyChange(evt);
            break;

          case SEASON:
            // remove from any season which is not the desired season
            for (TvShowSeason season : seasons) {
              if (season.getEpisodes().contains(episode) && season.getSeason() != episode.getSeason()) {
                season.removeEpisode(episode);
              }
            }
            addToSeason(episode);
            break;
        }
      }
    };
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
        case MediaMetadata.TVDB, MediaMetadata.IMDB, MediaMetadata.TMDB:
          // already in UI - skip
          continue;

        case "imdbId", "traktId", "tvShowSeason":
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
   * Initialize after loading.
   */
  @Override
  public void initializeAfterLoading() {
    super.initializeAfterLoading();

    // load dummy episodes
    for (TvShowEpisode episode : dummyEpisodes) {
      episode.setTvShow(this);
      addToSeason(episode);
    }

    for (TvShowEpisode episode : episodes) {
      episode.addPropertyChangeListener(propertyChangeListener);
    }
  }

  /**
   * Overwrites all null/empty elements with "other" value (but might be empty also)<br>
   * For lists, check with 'contains' and add.<br>
   * Do NOT merge path, dateAdded, scraped, mediaFiles and other crucial properties!
   *
   * @param other
   *          the other Tv show to merge in
   */
  public void merge(TvShow other) {
    merge(other, false);
  }

  /**
   * Overwrites all elements with "other" value<br>
   * Do NOT merge path, dateAdded, scraped, mediaFiles and other crucial properties!
   *
   * @param other
   *          the other TV show to merge in
   */
  public void forceMerge(TvShow other) {
    merge(other, true);
  }

  void merge(TvShow other, boolean force) {
    if (locked || other == null) {
      return;
    }

    super.merge(other, force);

    setEpisodeGroup(episodeGroup == MediaEpisodeGroup.DEFAULT_AIRED || force ? other.episodeGroup : episodeGroup);
    setSortTitle(StringUtils.isEmpty(sortTitle) || force ? other.sortTitle : sortTitle);
    setRuntime(runtime == 0 || force ? other.runtime : runtime);
    setFirstAired(firstAired == null || force ? other.firstAired : firstAired);
    setStatus(status == MediaAiredStatus.UNKNOWN || force ? other.status : status);
    setCertification(certification == MediaCertification.UNKNOWN || force ? other.certification : certification);
    setCountry(StringUtils.isEmpty(country) || force ? other.country : country);
    setTop250(top250 == 0 || force ? other.top250 : top250);

    // when force is set, clear the lists/maps and add all other values
    if (force) {
      genres.clear();
      actors.clear();
      extraFanartUrls.clear();
      episodeGroups.clear();
      seasonNames.clear();
      seasonOverviews.clear();
    }

    setGenres(other.genres);
    setActors(other.actors);
    setExtraFanartUrls(other.extraFanartUrls);
    setEpisodeGroups(other.episodeGroups);

    for (var entry : other.seasonNames.entrySet()) {
      seasonNames.putIfAbsent(entry.getKey(), entry.getValue());
    }

    for (var entry : other.seasonOverviews.entrySet()) {
      seasonOverviews.putIfAbsent(entry.getKey(), entry.getValue());
    }

    // seasons
    for (TvShowSeason otherSeason : other.getSeasons()) {
      TvShowSeason ourSeason = getSeason(otherSeason.getSeason());
      if (ourSeason == null) {
        addSeason(new TvShowSeason(otherSeason));
      }
      else {
        ourSeason.merge(otherSeason);
      }
    }

    // get ours, and merge other values
    for (TvShowEpisode ep : episodes) {
      TvShowEpisode otherEP = other.getEpisode(ep.getSeason(), ep.getEpisode()).stream().findFirst().orElse(null);
      ep.merge(otherEP, force);
    }

    // get others, and simply add
    for (TvShowEpisode otherEp : other.getEpisodes()) {
      TvShowEpisode ourEP = getEpisode(otherEp.getSeason(), otherEp.getEpisode()).stream().findFirst().orElse(null); // do not do a contains check!
      if (ourEP == null) {
        TvShowEpisode clone = new TvShowEpisode(otherEp);
        clone.setTvShow(this); // yes!
        addEpisode(clone);
      }
    }
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
  public void setTitle(String newValue) {
    super.setTitle(newValue);

    String oldValue = this.titleSortable;
    titleSortable = "";
    firePropertyChange(TITLE_SORTABLE, oldValue, titleSortable);
  }

  /**
   * Returns the sortable variant of title<br>
   * eg "The Big Bang Theory" -> "Big Bang Theory, The".
   *
   * @return the title in its sortable format
   */
  public String getTitleSortable() {
    if (StringUtils.isEmpty(titleSortable)) {
      titleSortable = Utils.getSortableName(this.getTitle());
    }
    return titleSortable;
  }

  public void clearTitleSortable() {
    titleSortable = "";
  }

  public String getSortTitle() {
    return sortTitle;
  }

  public void setSortTitle(String newValue) {
    String oldValue = this.sortTitle;
    this.sortTitle = newValue;
    firePropertyChange(SORT_TITLE, oldValue, newValue);
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
   * get the {@link MediaEpisodeGroup} this {@link TvShow} holds its {@link TvShowEpisode}s
   *
   * @return the {@link MediaEpisodeGroup} which has been set while scraping or a default one (aired order)
   */
  public MediaEpisodeGroup getEpisodeGroup() {
    return episodeGroup;
  }

  /**
   * Set the {@link MediaEpisodeGroup} this {@link TvShow} uses for its {@link TvShowEpisode#}s
   *
   * @param newValue
   *          the {@link MediaEpisodeGroup} to be set
   */
  public void setEpisodeGroup(MediaEpisodeGroup newValue) {
    MediaEpisodeGroup oldValue = this.episodeGroup;
    this.episodeGroup = newValue;
    firePropertyChange(Constants.EPISODE_GROUP, oldValue, newValue);

    // also rebuild the seasons and fire the event for all episodes too
    if (!oldValue.equals(newValue)) {
      LOGGER.info("Switched episodeGroup '{}' -> '{}' for show {}", oldValue, newValue, getTitle());
      // remove all episodes from all seasons
      seasons.forEach(TvShowSeason::removeAllEpisodes);

      // and rebuild
      for (TvShowEpisode episode : getEpisodesForDisplay()) {
        // add to new season
        TvShowSeason season = getOrCreateSeason(episode.getSeason());

        // make sure the season is in the UI
        firePropertyChange(ADDED_SEASON, null, season);

        // add the episode to it
        season.addEpisode(episode);
        episode.firePropertyChange(Constants.EPISODE_GROUP, oldValue, newValue);
      }

      // remove empty seasons
      for (TvShowSeason season : new ArrayList<>(seasons)) {
        if (season.isEmpty()) {
          removeSeason(season);
        }
      }

      // update the season names/overviews
      for (TvShowSeason season : getSeasons()) {
        season.setTitle(getSeasonName(season.getSeason()));
        season.setPlot(getSeasonOverview(season.getSeason()));
      }
    }
  }

  /**
   * get all available "named" {@link MediaEpisodeGroup}s for this TV show
   *
   * @return a {@link List} of all names {@link MediaEpisodeGroup}s
   */
  public List<MediaEpisodeGroup> getEpisodeGroups() {
    return Collections.unmodifiableList(episodeGroups);
  }

  /**
   * set the list of "named" {@link MediaEpisodeGroup}s for this TV show
   *
   * @param newValues
   *          the {@link List} of all named {@link MediaEpisodeGroup}s
   */
  public void setEpisodeGroups(Collection<MediaEpisodeGroup> newValues) {
    episodeGroups.clear();

    // sort by episode groups (same order as in MediaEpisodeGroup.EpisodeGroup)
    for (MediaEpisodeGroup.EpisodeGroupType eg : MediaEpisodeGroup.EpisodeGroupType.values()) {
      // special logic: the chosen episode group must be the first one in the list
      if (episodeGroup != null && episodeGroup.getEpisodeGroupType() == eg) {
        if (newValues.contains(episodeGroup)) {
          episodeGroups.add(episodeGroup);
        }
      }

      // add all (remaining) episode numbers for this type
      newValues.forEach(group -> {
        if (group.getEpisodeGroupType() == eg && !episodeGroups.contains(group)) {
          episodeGroups.add(group);
        }
      });
    }

    firePropertyChange("episodeGroups", null, episodeGroups);
  }

  /**
   * lazily add a {@link MediaEpisodeGroup} to this TV show. This will only be called from within the
   * {@link TvShowEpisode#setMetadata(MediaMetadata, List, boolean)} method
   * 
   * @param episodeGroup
   *          the {@link MediaEpisodeGroup} to add
   */
  public void addEpisodeGroup(MediaEpisodeGroup episodeGroup) {
    episodeGroups.add(episodeGroup);
  }

  /**
   * set all season names
   *
   * @param newValue
   *          a {@link Map} containing all season names
   */
  public void setSeasonNames(Map<MediaEpisodeGroup, Map<Integer, String>> newValue) {
    seasonNames.clear();
    for (var entry : newValue.entrySet()) {
      seasonNames.putIfAbsent(entry.getKey().toString(), entry.getValue());
    }
    firePropertyChange("seasonNames", null, seasonNames);
  }

  /**
   * get all season names
   *
   * @return a {@link Map} containing all season names
   */
  public Map<String, Map<Integer, String>> getSeasonNames() {
    return Collections.unmodifiableMap(seasonNames);
  }

  /**
   * get the season name for the chosen {@link MediaEpisodeGroup}
   *
   * @param season
   *          the season to get the name for
   * @return the found season name or an empty {@link String}
   */
  String getSeasonName(int season) {
    Map<Integer, String> seasonNamesForEpisodeGroup = seasonNames.get(episodeGroup.toString());
    if (seasonNamesForEpisodeGroup != null) {
      String seasonName = seasonNamesForEpisodeGroup.get(season);
      if (StringUtils.isNotBlank(seasonName)) {
        return seasonName;
      }
    }

    return "";
  }

  /**
   * set all season overviews
   *
   * @param newValue
   *          a {@link Map} containing all season overviews
   */
  public void setSeasonOverviews(Map<MediaEpisodeGroup, Map<Integer, String>> newValue) {
    seasonOverviews.clear();
    for (var entry : newValue.entrySet()) {
      seasonOverviews.putIfAbsent(entry.getKey().toString(), entry.getValue());
    }
    firePropertyChange("seasonOverviews", null, seasonOverviews);
  }

  /**
   * get all season overviews
   *
   * @return a {@link Map} containing all season overviews
   */
  public Map<String, Map<Integer, String>> getSeasonOverviews() {
    return Collections.unmodifiableMap(seasonOverviews);
  }

  /**
   * get the season overview for the chosen {@link MediaEpisodeGroup}
   *
   * @param season
   *          the season to get the overview for
   * @return the found season name or an empty {@link String}
   */
  String getSeasonOverview(int season) {
    Map<Integer, String> seasonOverviewForEpisodeGroup = seasonOverviews.get(episodeGroup.toString());
    if (seasonOverviewForEpisodeGroup != null) {
      String seasonName = seasonOverviewForEpisodeGroup.get(season);
      if (StringUtils.isNotBlank(seasonName)) {
        return seasonName;
      }
    }

    return "";
  }

  /**
   * Gets the episodes.
   *
   * @return the episodes
   */
  public List<TvShowEpisode> getEpisodes() {
    return Collections.unmodifiableList(episodes);
  }

  /**
   * Adds the episode.
   *
   * @param episode
   *          the episode
   */
  public synchronized void addEpisode(TvShowEpisode episode) {
    int oldValue = episodes.size();
    episodes.add(episode);
    episode.addPropertyChangeListener(propertyChangeListener);
    addToSeason(episode);

    episodes.sort(TvShowEpisode::compareTo);

    firePropertyChange(ADDED_EPISODE, null, episode);
    firePropertyChange(EPISODE_COUNT, oldValue, episodes.size());

    // probably also add the episode group from this episode
    for (MediaEpisodeNumber episodeNumber : episode.getEpisodeNumbers()) {
      if (!episodeGroups.contains(episodeNumber.episodeGroup())) {
        episodeGroups.add(episodeNumber.episodeGroup());
      }
    }
  }

  public List<TvShowEpisode> getDummyEpisodes() {
    return Collections.unmodifiableList(dummyEpisodes);
  }

  public void setDummyEpisodes(List<TvShowEpisode> dummyEpisodes) {
    // remove all previous existing dummy movies from the UI
    for (TvShowEpisode episode : this.dummyEpisodes) {
      firePropertyChange(REMOVED_EPISODE, null, episode);
    }
    this.dummyEpisodes.clear();
    this.dummyEpisodes.addAll(dummyEpisodes);

    // link with TV show
    for (TvShowEpisode dummy : this.dummyEpisodes) {
      dummy.setTvShow(this);
    }

    // also mix in the episodes if activated
    if (TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()) {
      seasons.forEach(TvShowSeason::removeDummyEpisodes);

      for (TvShowEpisode episode : dummyEpisodes) {
        TvShowSeason season = getSeasonForEpisode(episode);
        season.addEpisode(episode);
        if (season.getEpisodesForDisplay().contains(episode)) {
          firePropertyChange(ADDED_EPISODE, null, episode);
        }
      }
    }

    this.dummyEpisodes.sort(TvShowEpisode::compareTo);

    firePropertyChange("dummyEpisodes", null, dummyEpisodes);
    firePropertyChange(EPISODE_COUNT, 0, episodes.size());
  }

  /**
   * completely remove one dummy episode from this TV show
   * 
   * @param episode
   *          the dummy episode
   */
  public void removeDummyEpisode(@NotNull TvShowEpisode episode) {
    if (!episode.isDummy()) {
      return;
    }

    // remove from UI
    removeEpisode(episode);

    // remove from DB
    dummyEpisodes.remove(episode);
    saveToDb();
  }

  /**
   * build a list of <br>
   * a) available episodes along with<br>
   * b) missing episodes <br>
   * for display in the TV show list
   *
   * @return a list of _all_ episodes
   */
  public List<TvShowEpisode> getEpisodesForDisplay() {
    return TvShowHelpers.getEpisodesForDisplay(episodes, dummyEpisodes);
  }

  /**
   * get all episodes for the given season
   *
   * @param season
   *          the season to get all episodes for
   * @return a {@link List} of all episodes
   */
  public List<TvShowEpisode> getEpisodesForSeason(int season) {
    List<TvShowEpisode> eps = new ArrayList<>();

    for (TvShowEpisode episode : episodes) {
      if (episode.getSeason() == season) {
        eps.add(episode);
      }
    }

    return eps;
  }

  /**
   * Gets the episode count.
   *
   * @return the episode count
   */
  public int getEpisodeCount() {
    return episodes.size();
  }

  /**
   * Gets the dummy episode count
   *
   * @return the dummy episode count
   */
  public int getDummyEpisodeCount() {
    int count = 0;

    if (!TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()) {
      return count;
    }

    for (TvShowSeason season : seasons) {
      for (TvShowEpisode episode : season.getEpisodesForDisplay()) {
        if (episode.isDummy()) {
          if (!TvShowHelpers.shouldAddDummyEpisode(episode)) {
            continue;
          }

          count++;
        }
      }
    }
    return count;
  }

  /**
   * Adds the to season.
   *
   * @param episode
   *          the episode
   */
  private void addToSeason(TvShowEpisode episode) {
    TvShowSeason season = getSeasonForEpisode(episode);
    season.addEpisode(episode);
  }

  private void removeFromSeason(TvShowEpisode episode) {
    TvShowSeason season = getSeasonForEpisode(episode);
    season.removeEpisode(episode);
  }

  /**
   * Gets the season for episode.
   *
   * @param episode
   *          the episode
   * @return the season for episode
   */
  public synchronized TvShowSeason getSeasonForEpisode(TvShowEpisode episode) {
    TvShowSeason season = null;

    // search for an existing season
    for (TvShowSeason s : seasons) {
      if (s.getSeason() == episode.getSeason()) {
        season = s;
        break;
      }
    }

    // no one found - create one
    if (season == null) {
      season = new TvShowSeason(episode.getSeason(), this);
      season.setTitle(getSeasonName(episode.getSeason()));
      season.setPlot(getSeasonOverview(episode.getSeason()));
      addSeason(season);
    }

    return season;
  }

  public int getSeasonCount() {
    int count = 0;
    for (TvShowSeason season : seasons) {
      if (!season.isDummy()) {
        count++;
      }
    }
    return count;
  }

  /**
   * gets the season object for the given season number or null
   *
   * @param seasonNumber
   *          the season number
   * @return the TvShowSeason object or null
   */
  public TvShowSeason getSeason(int seasonNumber) {
    return seasons.parallelStream().filter(season -> season.getSeason() == seasonNumber).findFirst().orElse(null);
  }

  /**
   * gets or creates a new season. This can be handy if you have metadata/artwork for a _new_ season
   *
   * @param seasonNumber
   *          the season number
   * @return the {@link TvShowSeason}
   */
  public TvShowSeason getOrCreateSeason(int seasonNumber) {
    TvShowSeason season = getSeason(seasonNumber);

    if (season == null) {
      season = new TvShowSeason(seasonNumber, this);
      season.setTitle(getSeasonName(seasonNumber));
      season.setPlot(getSeasonOverview(seasonNumber));
      addSeason(season);
    }

    return season;
  }

  /**
   * Removes the episode.
   *
   * @param episode
   *          the episode
   */
  public void removeEpisode(TvShowEpisode episode) {
    if (episodes.contains(episode)) {
      int oldValue = episodes.size();
      episode.removePropertyChangeListener(propertyChangeListener);
      removeFromSeason(episode);
      episodes.remove(episode);
      TvShowModuleManager.getInstance().getTvShowList().removeEpisodeFromDb(episode);

      // and remove the image cache
      for (MediaFile mf : episode.getMediaFiles()) {
        if (mf.isGraphic()) {
          ImageCache.invalidateCachedImage(mf);
        }
      }

      saveToDb();

      firePropertyChange(REMOVED_EPISODE, null, episode);
      firePropertyChange(EPISODE_COUNT, oldValue, episodes.size());

      // and mix in the dummy one again
      // check if there is no other episode available (e.g. exchanged media file)
      if (TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()
          && ListUtils.isEmpty(getEpisode(episode.getSeason(), episode.getEpisode()))) {
        for (TvShowEpisode dummy : dummyEpisodes) {
          if (!TvShowHelpers.shouldAddDummyEpisode(dummy)) {
            continue;
          }

          if (dummy.getSeason() == episode.getSeason() && dummy.getEpisode() == episode.getEpisode()) {
            addToSeason(dummy);
            firePropertyChange(ADDED_EPISODE, null, dummy);
            break;
          }
        }
      }
    }
    else if (dummyEpisodes.contains(episode)) {
      // just fire the event for updating the UI
      removeFromSeason(episode);
      firePropertyChange(REMOVED_EPISODE, null, episode);
      firePropertyChange(EPISODE_COUNT, 0, episodes.size());
    }
  }

  /**
   * Removes an episode from tmm and deletes it from the data source
   *
   * @param episode
   *          the episode to be removed
   */
  public void deleteEpisode(TvShowEpisode episode) {
    if (episodes.contains(episode)) {
      int oldValue = episodes.size();
      episode.removePropertyChangeListener(propertyChangeListener);
      episode.deleteFilesSafely();
      removeFromSeason(episode);
      episodes.remove(episode);
      TvShowModuleManager.getInstance().getTvShowList().removeEpisodeFromDb(episode);

      // and remove the image cache
      for (MediaFile mf : episode.getMediaFiles()) {
        if (mf.isGraphic()) {
          ImageCache.invalidateCachedImage(mf);
        }
      }

      saveToDb();

      firePropertyChange(REMOVED_EPISODE, null, episode);
      firePropertyChange(EPISODE_COUNT, oldValue, episodes.size());
    }
  }

  public void addSeason(TvShowSeason season) {
    if (!seasons.contains(season)) {
      int seasonCount = seasons.size();
      seasons.add(season);
      seasons.sort(TvShowSeason::compareTo);

      firePropertyChange(ADDED_SEASON, null, season);
      firePropertyChange(SEASON_COUNT, seasonCount, seasons.size());
    }
  }

  void removeSeason(TvShowSeason season) {
    int seasonCount = seasons.size();

    if (seasons.remove(season)) {
      firePropertyChange(REMOVED_SEASON, null, season);
      firePropertyChange(SEASON_COUNT, seasonCount, seasons.size());
    }
  }

  /**
   * Gets the seasons.
   *
   * @return the seasons
   */
  public List<TvShowSeason> getSeasons() {
    return Collections.unmodifiableList(seasons);
  }

  public int getTop250() {
    return top250;
  }

  public void setTop250(int newValue) {
    int oldValue = this.top250;
    this.top250 = newValue;
    firePropertyChange(TOP250, oldValue, newValue);
  }

  /**
   * Gets the genres.
   *
   * @return the genres
   */
  public List<MediaGenres> getGenres() {
    return Collections.unmodifiableList(genres);
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
   * Sets the metadata.
   *
   * @param metadata
   *          the new metadata
   * @param config
   *          the config
   */
  public void setMetadata(MediaMetadata metadata, List<TvShowScraperMetadataConfig> config, boolean overwriteExistingItems) {
    if (locked) {
      LOGGER.debug("TV show locked, but setMetadata has been called!");
      return;
    }

    // check against null metadata (e.g. aborted request)
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
    // b) we did just a scrape (probably with another scraper). we should have at least one id in the TV show which matches the ids from the metadata
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

    // when there are no ids, we must assume that this is the first scrape of the TV show - treat it like a matched one (to prevent existing/manually
    // entered data from being overwritten)
    boolean newEntity = false;
    if (ids.isEmpty()) {
      matchFound = true;
      newEntity = true;
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

    if (config.contains(TvShowScraperMetadataConfig.TITLE) && StringUtils.isNotBlank(metadata.getTitle())
        && (overwriteExistingItems || newEntity || StringUtils.isBlank(getTitle()))) {
      // Capitalize first letter of original title if setting is set!
      if (TvShowModuleManager.getInstance().getSettings().getCapitalWordsInTitles()) {
        setTitle(StrgUtils.capitalize(metadata.getTitle()));
      }
      else {
        setTitle(metadata.getTitle());
      }
    }

    if (config.contains(TvShowScraperMetadataConfig.ORIGINAL_TITLE) && (overwriteExistingItems || StringUtils.isBlank(getOriginalTitle()))) {
      // Capitalize first letter of original title if setting is set!
      if (TvShowModuleManager.getInstance().getSettings().getCapitalWordsInTitles()) {
        setOriginalTitle(StrgUtils.capitalize(metadata.getOriginalTitle()));
      }
      else {
        setOriginalTitle(metadata.getOriginalTitle());
      }
    }

    if (config.contains(TvShowScraperMetadataConfig.PLOT) && (overwriteExistingItems || StringUtils.isBlank(getPlot()))) {
      setPlot(metadata.getPlot());
    }

    if (config.contains(TvShowScraperMetadataConfig.YEAR) && (overwriteExistingItems || getYear() <= 0)) {
      setYear(metadata.getYear());
    }

    if (config.contains(TvShowScraperMetadataConfig.RATING)) {
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

    if (config.contains(TvShowScraperMetadataConfig.AIRED) && (overwriteExistingItems || getFirstAired() == null)) {
      setFirstAired(metadata.getReleaseDate());
    }

    if (config.contains(TvShowScraperMetadataConfig.TOP250) && (overwriteExistingItems || getTop250() <= 0)) {
      setTop250(metadata.getTop250());
    }

    if (config.contains(TvShowScraperMetadataConfig.STATUS)
        && (overwriteExistingItems || getStatus() == null || getStatus() == MediaAiredStatus.UNKNOWN)) {
      setStatus(metadata.getStatus());
    }

    if (config.contains(TvShowScraperMetadataConfig.RUNTIME) && (overwriteExistingItems || getRuntime() <= 0)) {
      setRuntime(metadata.getRuntime());
    }

    if (config.contains(TvShowScraperMetadataConfig.COUNTRY) && (overwriteExistingItems || StringUtils.isBlank(getCountry()))) {
      setCountry(StringUtils.join(metadata.getCountries(), ", "));
    }

    if (config.contains(TvShowScraperMetadataConfig.STUDIO) && (overwriteExistingItems || StringUtils.isBlank(getProductionCompany()))) {
      setProductionCompany(StringUtils.join(metadata.getProductionCompanies(), ", "));
    }

    if (config.contains(TvShowScraperMetadataConfig.CERTIFICATION)
        && (overwriteExistingItems || getCertification() == null || getCertification() == MediaCertification.UNKNOWN)) {
      if (!metadata.getCertifications().isEmpty()) {
        if (metadata.getCertifications().size() > 1) {
          // US has 2 rating systems - MPAA, Parental TV guide - see tt0092644
          // we take the MPAA for movies.
          // movie even has 4 different ratings for Canada ... wuah
          // simple approach
          if (!metadata.getCertifications().get(0).name().startsWith("US_TV")) {
            setCertification(metadata.getCertifications().get(1));
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

    // 1:n relations are either merged (no overwrite) or completely set with the new data

    if (config.contains(TvShowScraperMetadataConfig.ACTORS)) {
      if (!matchFound || overwriteExistingItems) {
        actors.clear();
      }
      setActors(metadata.getCastMembers(Person.Type.ACTOR));
    }

    if (config.contains(TvShowScraperMetadataConfig.GENRES)) {
      if (!matchFound || overwriteExistingItems) {
        genres.clear();
      }
      setGenres(metadata.getGenres());
    }

    if (config.contains(TvShowScraperMetadataConfig.TAGS)) {
      if (!matchFound || overwriteExistingItems) {
        removeAllTags();
      }

      addToTags(metadata.getTags());
    }

    if (config.contains(TvShowScraperMetadataConfig.SEASON_NAMES)) {
      if (!matchFound || overwriteExistingItems) {
        seasonNames.clear();
        seasons.forEach(season -> season.setTitle(""));
      }

      for (var episodeGroupEntry : metadata.getSeasonNames().entrySet()) {
        // only set season names for stored episode groups
        if (!episodeGroups.contains(episodeGroupEntry.getKey())) {
          continue;
        }

        Map<Integer, String> seasonNamesForEpisodeGroup = seasonNames.computeIfAbsent(episodeGroupEntry.getKey().toString(), k -> new HashMap<>());

        for (var entry : episodeGroupEntry.getValue().entrySet()) {
          // "Season XX" does not match (and not needed to set)
          // "Season XX - some name" should be set, so the pattern checks the complete string!
          Matcher matcher = SEASON_ONLY_PATTERN.matcher(entry.getValue());
          if (!matcher.find()) {
            if (overwriteExistingItems) {
              seasonNamesForEpisodeGroup.put(entry.getKey(), entry.getValue());
            }
            else {
              seasonNamesForEpisodeGroup.putIfAbsent(entry.getKey(), entry.getValue());
            }
          }

          // live update of the season
          if (episodeGroupEntry.getKey().equals(episodeGroup)) {
            TvShowSeason season = getSeason(entry.getKey());
            if (season != null) {
              String seasonName = seasonNamesForEpisodeGroup.get(entry.getKey());
              if (StringUtils.isNotBlank(seasonName)) {
                season.setTitle(seasonName);
              }
              else {
                season.setTitle("");
              }
            }
          }
        }

      }
    }

    if (config.contains(TvShowScraperMetadataConfig.SEASON_OVERVIEW)) {
      if (!matchFound || overwriteExistingItems) {
        seasonOverviews.clear();
        seasons.forEach(season -> season.setPlot(""));
      }

      for (var episodeGroupEntry : metadata.getSeasonOverview().entrySet()) {
        // only set season names for stored episode groups
        if (!episodeGroups.contains(episodeGroupEntry.getKey())) {
          continue;
        }

        Map<Integer, String> seasonOverViewsForEpisodeGroup = seasonOverviews.computeIfAbsent(episodeGroupEntry.getKey().toString(),
            k -> new HashMap<>());

        for (var entry : episodeGroupEntry.getValue().entrySet()) {
          if (overwriteExistingItems) {
            seasonOverViewsForEpisodeGroup.put(entry.getKey(), entry.getValue());
          }
          else {
            seasonOverViewsForEpisodeGroup.putIfAbsent(entry.getKey(), entry.getValue());
          }

          // live update of the season
          if (episodeGroupEntry.getKey().equals(episodeGroup)) {
            TvShowSeason season = getSeason(entry.getKey());
            if (season != null) {
              String seasonPlot = seasonOverViewsForEpisodeGroup.get(entry.getKey());
              if (StringUtils.isNotBlank(seasonPlot)) {
                season.setPlot(seasonPlot);
              }
              else {
                season.setPlot("");
              }
            }
          }
        }
      }
    }

    // update DB
    writeNFO();

    // also force to write all season NFO files
    seasons.forEach(TvShowSeason::writeNfo);

    saveToDb();

    // and post-process
    postProcess(config, overwriteExistingItems);
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
  public void setArtwork(List<MediaArtwork> artwork, List<TvShowScraperMetadataConfig> config, boolean overwrite) {
    TvShowArtworkHelper.setArtwork(this, artwork, config, overwrite);
  }

  /**
   * download the specified type of artwork for this TV show
   *
   * @param type
   *          the chosen artwork type to be downloaded
   */
  public void downloadArtwork(MediaFileType type) {
    TvShowArtworkHelper.downloadArtwork(this, type);
  }

  /**
   * Write nfo.
   */
  public void writeNFO() {
    ITvShowConnector connector;

    List<TvShowNfoNaming> nfoNamings = TvShowModuleManager.getInstance().getSettings().getNfoFilenames();
    if (!nfoNamings.isEmpty()) {
      connector = switch (TvShowModuleManager.getInstance().getSettings().getTvShowConnector()) {
        case XBMC, MEDIAPORTAL -> new TvShowToXbmcConnector(this);
        case EMBY -> new TvShowToEmbyConnector(this);
        case JELLYFIN -> new TvShowToJellyfinConnector(this);
        default -> new TvShowToKodiConnector(this);
      };

      try {
        connector.write(nfoNamings);
        firePropertyChange(HAS_NFO_FILE, false, true);
      }
      catch (Exception e) {
        LOGGER.error("could not write NFO file - '{}'", e.getMessage());
      }
    }
  }

  private void postProcess(List<TvShowScraperMetadataConfig> config, boolean overwriteExistingItems) {
    TmmTaskChain taskChain = TmmTaskChain.getInstance(this);

    // rename the TV show if that has been chosen in the settings
    if (TvShowModuleManager.getInstance().getSettings().isRenameAfterScrape()) {
      taskChain.add(new TvShowRenameTask(this));
    }

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

  /**
   * Gets the checks for nfo file.
   *
   * @return the checks for nfo file
   */
  public Boolean getHasNfoFile() {
    List<MediaFile> nfos = getMediaFiles(MediaFileType.NFO);
    return nfos != null && !nfos.isEmpty();
  }

  /**
   * Gets the checks for trailer.
   *
   * @return the checks for trailer
   */
  public Boolean getHasTrailer() {
    if (trailer != null && !trailer.isEmpty()) {
      return true;
    }

    // check if there is a mediafile (trailer)
    if (!getMediaFiles(MediaFileType.TRAILER).isEmpty()) {
      return true;
    }

    return false;
  }

  /**
   * Check if Tv show has a Music Theme File
   * 
   * @return the check for the musictheme file
   */
  public Boolean getHasMusicTheme() {
    return (!getMediaFiles(MediaFileType.THEME).isEmpty());
  }

  public Boolean getHasNote() {
    return StringUtils.isNotBlank(note);
  }

  /**
   * Checks if all seasons and episodes of that TV show have artwork assigned
   *
   * @return true if artwork is available
   */
  public Boolean getHasSeasonAndEpisodeImages() {
    TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();
    for (TvShowSeason season : seasons) {
      if (season.isDummy()) {
        continue;
      }
      if (!tvShowList.detectMissingArtwork(season).isEmpty() || !season.getHasEpisodeImages()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if all episodes of that TV show has metadata
   *
   * @return true if NFO files are available
   */
  public Boolean getHasEpisodeMetadata() {
    TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

    for (TvShowEpisode episode : episodes) {
      if (!tvShowList.detectMissingMetadata(episode).isEmpty()) {
        return false;
      }
    }

    return true;
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
   * Sets the imdb id.
   *
   * @param newValue
   *          the new imdb id
   */
  public void setImdbId(String newValue) {
    this.setId(MediaMetadata.IMDB, newValue);
  }

  /**
   * Gets the tvdb id.
   *
   * @return the tvdb id
   */
  public String getTvdbId() {
    return this.getIdAsString(MediaMetadata.TVDB);
  }

  /**
   * Sets the tvdb id.
   *
   * @param newValue
   *          the new tvdb id
   */
  public void setTvdbId(String newValue) {
    this.setId(MediaMetadata.TVDB, newValue);
  }

  /**
   * Gets the TraktTV id.
   *
   * @return the TraktTV id
   */
  public int getTraktId() {
    return this.getIdAsInt(MediaMetadata.TRAKT_TV);
  }

  /**
   * Sets the TraktTv id.
   *
   * @param newValue
   *          the new TraktTV id
   */
  public void setTraktId(int newValue) {
    this.setId(MediaMetadata.TRAKT_TV, newValue);
  }

  /**
   * Gets the TMDB id.
   *
   * @return the TTMDB id
   */
  public int getTmdbId() {
    return this.getIdAsInt(MediaMetadata.TMDB);
  }

  /**
   * Sets the TMDB id.
   *
   * @param newValue
   *          the new TMDB id
   */
  public void setTmdbId(int newValue) {
    this.setId(MediaMetadata.TMDB, newValue);
  }

  /**
   * first aired date.
   *
   * @return the date
   */
  public Date getFirstAired() {
    return firstAired;
  }

  @JsonIgnore
  public void setFirstAired(Date newValue) {
    Date oldValue = this.firstAired;
    this.firstAired = newValue;
    firePropertyChange(FIRST_AIRED, oldValue, newValue);
    firePropertyChange(FIRST_AIRED_AS_STRING, oldValue, newValue);
  }

  @Override
  public Date getReleaseDate() {
    return firstAired;
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

  /**
   * Gets the status.
   *
   * @return the status
   */
  public MediaAiredStatus getStatus() {
    return status;
  }

  /**
   * Sets the status.
   *
   * @param newValue
   *          the new status
   */
  public void setStatus(MediaAiredStatus newValue) {
    MediaAiredStatus oldValue = this.status;
    this.status = newValue;
    firePropertyChange(STATUS, oldValue, newValue);
  }

  /**
   * Gets the runtime.
   *
   * @return the runtime
   */
  public int getRuntime() {
    return runtime;
  }

  /**
   * Sets the runtime.
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
   * Gets the actors.
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
   * Removes all actors.
   */
  public void removeActors() {
    actors.clear();
    firePropertyChange(ACTORS, null, getActors());
    firePropertyChange(ACTORS_AS_STRING, null, getActorsAsString());
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
   * Gets the certifications.
   *
   * @return the certifications
   */
  @Override
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
   * get the country
   *
   * @return the countries in which this TV show has been produced
   */
  public String getCountry() {
    return country;
  }

  /**
   * set the country
   *
   * @param newValue
   *          the country in which this TV show has been produced
   */
  public void setCountry(String newValue) {
    String oldValue = this.country;
    this.country = newValue;
    firePropertyChange(COUNTRY, oldValue, newValue);
  }

  /**
   * get all episodes to scraper (with season or ep number == -1)
   *
   * @return a list of all episodes to scrape
   */
  public List<TvShowEpisode> getEpisodesToScrape() {
    List<TvShowEpisode> episodesToScrape = new ArrayList<>();
    for (TvShowEpisode episode : this.episodes) {
      if (episode.getFirstAired() != null || !episode.getEpisodeNumbers().isEmpty()) {
        episodesToScrape.add(episode);
      }
    }
    return episodesToScrape;
  }

  /**
   * Checks if all episodes are watched.
   *
   * @return true, if all episodes are watched
   */
  public boolean isWatched() {
    boolean episodeFound = false;
    boolean watched = true;

    for (TvShowEpisode episode : episodes) {
      if (!episode.isDummy()) {
        episodeFound = true;
        watched = watched && episode.isWatched();
      }
    }

    // at least 1 non-dummy found -> pass the collected watched state
    if (episodeFound) {
      return watched;
    }

    // only dummy episodes -> return false
    return false;
  }

  /**
   * checks if all episode has subtitles
   *
   * @return true, is all episodes have subtitles
   */
  public boolean hasEpisodeSubtitles() {
    boolean subtitles = true;

    for (TvShowEpisode episode : episodes) {
      if (!episode.getHasSubtitles()) {
        subtitles = false;
        break;
      }
    }

    return subtitles;
  }

  public Date getLastWatched() {
    return lastWatched;
  }

  public void setLastWatched(Date lastWatched) {
    this.lastWatched = lastWatched;
  }

  /**
   * Gets the trailers
   *
   * @return the trailers
   */
  public List<MediaTrailer> getTrailer() {
    return Collections.unmodifiableList(trailer);
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

  public void setTrailers(List<MediaTrailer> trailers) {
    MediaTrailer preferredTrailer = null;
    removeAllTrailers();

    List<MediaTrailer> newItems = new ArrayList<>();

    // set preferred trailer
    if (TvShowModuleManager.getInstance().getSettings().isUseTrailerPreference()) {
      TrailerQuality desiredQuality = TvShowModuleManager.getInstance().getSettings().getTrailerQuality();
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

      newItems.add(preferredTrailer);
    }

    addToTrailer(newItems);

    // mix in local trailers
    mixinLocalTrailers();
  }

  /**
   * all supported TRAILER names. (without path, without extension!)
   *
   * @param trailer
   *          trailer naming enum
   * @return the associated trailer filename
   */
  public String getTrailerFilename(@NotNull TvShowTrailerNaming trailer) {
    // basename is the TV show title itself
    return FilenameUtils.removeExtension(TvShowRenamer.replaceInvalidCharacters(trailer.getFilename(getTitle(), "ext")));
  }

  /**
   * Gets the extra fanart urls.
   *
   * @return the extra fanart urls
   */
  public List<String> getExtraFanartUrls() {
    return extraFanartUrls;
  }

  /**
   * Sets the extra fanart urls.
   *
   * @param extraFanartUrls
   *          the new extra fanart urls
   */
  @JsonSetter
  public void setExtraFanartUrls(List<String> extraFanartUrls) {
    this.extraFanartUrls.clear();
    this.extraFanartUrls.addAll(extraFanartUrls);
    firePropertyChange("extraFanartUrls", null, this.extraFanartUrls);
  }

  /**
   * Gets the media files of all episodes.<br>
   * (without the TV show MFs like poster/banner/...)
   *
   * @return the media files
   */
  public List<MediaFile> getEpisodesMediaFiles() {
    List<MediaFile> mediaFiles = new ArrayList<>();
    for (TvShowEpisode episode : this.episodes) {
      for (MediaFile mf : episode.getMediaFiles()) {
        if (!mediaFiles.contains(mf)) {
          mediaFiles.add(mf);
        }
      }
    }
    return mediaFiles;
  }

  /**
   * Gets the media files of all seasons.<br>
   * (without the TV show MFs like poster/banner/...)
   *
   * @return the media files
   */
  public List<MediaFile> getSeasonMediaFiles() {
    List<MediaFile> mediaFiles = new ArrayList<>();
    for (TvShowSeason season : this.seasons) {
      for (MediaFile mf : season.getMediaFiles()) {
        if (!mediaFiles.contains(mf)) {
          mediaFiles.add(mf);
        }
      }
    }
    return mediaFiles;
  }

  /**
   * Gets the images to cache.
   *
   * @return the images to cache
   */
  @Override
  public List<MediaFile> getImagesToCache() {
    // get files to cache
    List<MediaFile> filesToCache = new ArrayList<>();

    for (MediaFile mf : getMediaFiles()) {
      if (mf.isGraphic()) {
        filesToCache.add(mf);
      }
    }

    if (TvShowModuleManager.getInstance().getSettings().isWriteActorImages()) {
      filesToCache.addAll(listActorFiles());
    }

    for (TvShowSeason season : new ArrayList<>(getSeasons())) {
      filesToCache.addAll(season.getImagesToCache());
    }

    for (TvShowEpisode episode : new ArrayList<>(this.episodes)) {
      filesToCache.addAll(episode.getImagesToCache());
    }

    return filesToCache;
  }

  @Override
  public synchronized void callbackForWrittenArtwork(MediaArtworkType type) {
    // nothing needed here
  }

  @Override
  public void saveToDb() {
    // update/insert this TV show to the database
    TvShowModuleManager.getInstance().getTvShowList().persistTvShow(this);

    // also save seasons (because most of the seasons metadata is set from within the TV show itself)
    seasons.forEach(TvShowSeason::saveToDb);
  }

  /**
   * Returns a list of [season, episode] array of duplicate episodes with same S/EE number
   * 
   * @return list of duplicated episodes [season, episode] - use {@link TvShow#getEpisode(int, int)} for getting them<br>
   *         or an empty list
   */
  public List<int[]> getDuplicateEpisodes() {
    List<Integer> see = new ArrayList<>();
    List<int[]> dupes = new ArrayList<>();
    for (TvShowEpisode ep : episodes) {
      if (ep.getSeason() == -1 || ep.getEpisode() == -1) {
        continue;
      }
      int num = ep.getSeason() * 10000 + ep.getEpisode();
      if (!see.contains(num)) {
        see.add(num);
      }
      else {
        // already in there? we have a dupe!
        int[] dupe = { ep.getSeason(), ep.getEpisode() };
        // deep array equals check - contains does not work
        if (!dupes.stream().anyMatch(c -> Arrays.equals(c, dupe))) {
          dupes.add(dupe);
        }
      }
    }
    return dupes;
  }

  public List<TvShowEpisode> getEpisode(final int season, final int episode) {
    if (season == -1 || episode == -1) {
      return Collections.emptyList();
    }

    List<TvShowEpisode> eps = new ArrayList<>();

    for (TvShowEpisode ep : episodes) {
      if (ep.getSeason() == season && ep.getEpisode() == episode) {
        eps.add(ep);
      }
    }

    return eps;
  }

  /**
   * check if one of the tv shows episode is newly added
   *
   * @return true/false
   */
  public boolean hasNewlyAddedEpisodes() {
    for (TvShowEpisode episode : new ArrayList<>(this.episodes)) {
      if (episode.isNewlyAdded()) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected float calculateScrapeScore() {
    float score = super.calculateScrapeScore();

    score = score + returnOneWhenFilled(runtime);
    score = score + returnOneWhenFilled(firstAired);
    if (status != MediaAiredStatus.UNKNOWN) {
      score = score + 1;
    }
    if (certification != MediaCertification.UNKNOWN) {
      score = score + 1;
    }
    score = score + returnOneWhenFilled(country);

    for (TvShowSeason season : getSeasons()) {
      score = score + returnOneWhenFilled(season.getTitle());
      score = score + returnOneWhenFilled(season.getArtworkUrls());
    }

    score = score + returnOneWhenFilled(extraFanartUrls);
    score = score + returnOneWhenFilled(actors);
    score = score + returnOneWhenFilled(trailer);

    return score;
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
   * @return list of actor images on filesystem
   */
  private List<MediaFile> listActorFiles() {
    if (!Files.exists(getPathNIO().resolve(Person.ACTOR_DIR))) {
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

  /**
   * <b>PHYSICALLY</b> deletes a complete TV show by moving it to datasource backup folder<br>
   * DS\.backup\&lt;moviename&gt;
   */
  public boolean deleteFilesSafely() {
    return Utils.deleteDirectorySafely(getPathNIO(), getDataSource());
  }

  @Override
  public MediaFile getMainFile() {
    return getMainVideoFile();
  }

  @Override
  public MediaFile getMainVideoFile() {
    return MediaFile.EMPTY_MEDIAFILE;
  }

  @Override
  public String getMediaInfoVideoFormat() {
    return "";
  }

  @Override
  public String getMediaInfoVideoResolution() {
    return "";
  }

  @Override
  public float getMediaInfoAspectRatio() {
    return 0;
  }

  @Override
  public Float getMediaInfoAspectRatio2() {
    return null;
  }

  @Override
  public String getMediaInfoVideoCodec() {
    return "";
  }

  @Override
  public double getMediaInfoFrameRate() {
    return 0;
  }

  @Override
  public String getVideoHDRFormat() {
    return "";
  }

  @Override
  public boolean isVideoIn3D() {
    return false;
  }

  @Override
  public String getMediaInfoAudioCodec() {
    return "";
  }

  @Override
  public List<String> getMediaInfoAudioCodecList() {
    return Collections.emptyList();
  }

  @Override
  public String getMediaInfoAudioChannels() {
    return "";
  }

  @Override
  public List<String> getMediaInfoAudioChannelList() {
    return Collections.emptyList();
  }

  @Override
  public String getMediaInfoAudioChannelsDot() {
    return "";
  }

  @Override
  public List<String> getMediaInfoAudioChannelDotList() {
    return Collections.emptyList();
  }

  @Override
  public String getMediaInfoAudioLanguage() {
    return "";
  }

  @Override
  public List<String> getMediaInfoAudioLanguageList() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getMediaInfoSubtitleLanguageList() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getMediaInfoSubtitleCodecList() {
    return Collections.emptyList();
  }

  @Override
  public String getMediaInfoContainerFormat() {
    return "";
  }

  @Override
  public int getMediaInfoVideoBitDepth() {
    return 0;
  }

  @Override
  public MediaSource getMediaInfoSource() {
    return MediaSource.UNKNOWN;
  }

  @Override
  public long getVideoFilesize() {
    long filesize = 0;
    for (TvShowEpisode episode : episodes) {
      filesize += episode.getVideoFilesize();
    }
    return filesize;
  }

  @Override
  public long getTotalFilesize() {
    long filesize = 0;
    for (TvShowEpisode episode : episodes) {
      filesize += episode.getTotalFilesize();
    }
    return filesize;
  }

  @Override
  protected void fireAddedEventForMediaFile(MediaFile mediaFile) {
    super.fireAddedEventForMediaFile(mediaFile);

    // TV show related media file types
    if (mediaFile.getType() == MediaFileType.TRAILER) {
      firePropertyChange(TRAILER, false, true);
    }
  }

  @Override
  protected void fireRemoveEventForMediaFile(MediaFile mediaFile) {
    super.fireRemoveEventForMediaFile(mediaFile);

    // TV show related media file types
    if (mediaFile.getType() == MediaFileType.TRAILER) {
      firePropertyChange(TRAILER, true, false);
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
    if (mediaFile.getType() == MediaFileType.TRAILER) {
      // re-write the trailer list
      mixinLocalTrailers();
    }
  }

  public Object getValueForMetadata(TvShowScraperMetadataConfig metadataConfig) {

    switch (metadataConfig) {
      case ID:
        return getIds();

      case TITLE:
        return getTitle();

      case ORIGINAL_TITLE:
        return getOriginalTitle();

      case PLOT:
        return getPlot();

      case YEAR:
        return getYear();

      case AIRED:
        return getFirstAired();

      case RATING:
        return getRatings();

      case RUNTIME:
        return getRuntime();

      case CERTIFICATION:
        return getCertification();

      case GENRES:
        return getGenres();

      case COUNTRY:
        return getCountry();

      case STUDIO:
        return getProductionCompany();

      case STATUS:
        return getStatus();

      case TAGS:
        return getTags();

      case TRAILER:
        return getTrailer();

      case ACTORS:
        return getActors();

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

      case CHARACTERART:
        return getMediaFiles(MediaFileType.CHARACTERART);

      case SEASON_NAMES:
        // if matches, we have all season titles
        for (TvShowSeason season : seasons) {
          if (season.getSeason() < 0) {
            continue;
          }

          if (season.isDummy()
              || season.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isEpisodeSpecialsCheckMissingMetadata()) {
            continue;
          }

          if (StringUtils.isBlank(season.getTitle())) {
            return null;
          }
        }
        return "all seasonnames found"; // dummy non-null

      case SEASON_OVERVIEW:
        // if matches, we have all season overviews
        for (TvShowSeason season : seasons) {
          if (season.getSeason() < 0) {
            continue;
          }

          if (season.isDummy()
              || season.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isEpisodeSpecialsCheckMissingMetadata()) {
            continue;
          }

          if (StringUtils.isBlank(season.getPlot())) {
            return null;
          }
        }
        return "all seasonoverviews found"; // dummy non-null

      case SEASON_POSTER:
        return getMediaFiles(MediaFileType.SEASON_POSTER);

      case SEASON_FANART:
        return getMediaFiles(MediaFileType.SEASON_FANART);

      case SEASON_BANNER:
        return getMediaFiles(MediaFileType.SEASON_BANNER);

      case SEASON_THUMB:
        return getMediaFiles(MediaFileType.SEASON_THUMB);

      case THEME:
        return getMediaFiles(MediaFileType.THEME);
    }

    return null;
  }

  /**
   * used to migrate values to their new location
   *
   * @param property
   *          the property/value name
   * @param value
   *          the value itself
   */
  @JsonAnySetter
  public void setUnknownFields(String property, Object value) {
    if (value == null) {
      return;
    }

    if ("seasonTitleMap".equals(property) && value instanceof Map<?, ?> seasonTitleMap) {
      for (var entry : seasonTitleMap.entrySet()) {
        int seasonNumber = TvUtils.parseInt(entry.getKey());
        if (seasonNumber > -1 && entry.getValue() instanceof String seasonTitle) {
          TvShowSeason season = getOrCreateSeason(seasonNumber);
          season.setTitle(seasonTitle);
        }
      }
    }
    else if ("seasonPosterUrlMap".equals(property) && value instanceof Map<?, ?> seasonPosterUrlMap) {
      for (var entry : seasonPosterUrlMap.entrySet()) {
        int seasonNumber = TvUtils.parseInt(entry.getKey());
        if (seasonNumber > -1 && entry.getValue() instanceof String seasonPosterUrl) {
          TvShowSeason season = getOrCreateSeason(seasonNumber);
          season.setArtworkUrl(seasonPosterUrl, MediaFileType.SEASON_POSTER);
        }
      }
    }
    else if ("seasonBannerUrlMap".equals(property) && value instanceof Map<?, ?> seasonBannerUrlMap) {
      for (var entry : seasonBannerUrlMap.entrySet()) {
        int seasonNumber = TvUtils.parseInt(entry.getKey());
        if (seasonNumber > -1 && entry.getValue() instanceof String seasonBannerUrl) {
          TvShowSeason season = getOrCreateSeason(seasonNumber);
          season.setArtworkUrl(seasonBannerUrl, MediaFileType.SEASON_BANNER);
        }
      }
    }
    else if ("seasonThumbUrlMap".equals(property) && value instanceof Map<?, ?> seasonThumbUrlMap) {
      for (var entry : seasonThumbUrlMap.entrySet()) {
        int seasonNumber = TvUtils.parseInt(entry.getKey());
        if (seasonNumber > -1 && entry.getValue() instanceof String seasonThumbUrl) {
          TvShowSeason season = getOrCreateSeason(seasonNumber);
          season.setArtworkUrl(seasonThumbUrl, MediaFileType.SEASON_THUMB);
        }
      }
    }
    else if ("seasonFanartUrlMap".equals(property) && value instanceof Map<?, ?> seasonFanartUrlMap) {
      for (var entry : seasonFanartUrlMap.entrySet()) {
        int seasonNumber = TvUtils.parseInt(entry.getKey());
        if (seasonNumber > -1 && entry.getValue() instanceof String seasonFanartUrl) {
          TvShowSeason season = getOrCreateSeason(seasonNumber);
          season.setArtworkUrl(seasonFanartUrl, MediaFileType.SEASON_FANART);
        }
      }
    }
  }
}
