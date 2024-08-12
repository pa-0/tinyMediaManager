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

import static org.tinymediamanager.core.Constants.ADDED_EPISODE;
import static org.tinymediamanager.core.Constants.ADDED_SEASON;
import static org.tinymediamanager.core.Constants.FIRST_AIRED;
import static org.tinymediamanager.core.Constants.HAS_NFO_FILE;
import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.REMOVED_EPISODE;
import static org.tinymediamanager.core.Constants.REMOVED_SEASON;
import static org.tinymediamanager.core.Constants.SEASON;
import static org.tinymediamanager.core.Constants.TV_SHOW;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.connector.ITvShowSeasonConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowSeasonToEmbyConnector;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonNfoNaming;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.util.ListUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Class TvShowSeason.
 * 
 * @author Manuel Laggner
 */
public class TvShowSeason extends MediaEntity implements Comparable<TvShowSeason> {
  @JsonProperty
  private UUID                      tvShowDbId    = null;

  @JsonProperty
  private final int                 season;

  private TvShow                    tvShow        = null;
  private final List<TvShowEpisode> episodes      = new CopyOnWriteArrayList<>();
  private final List<TvShowEpisode> dummyEpisodes = new CopyOnWriteArrayList<>();

  private PropertyChangeListener    listener;

  /**
   * Instantiates a new tv show episode. To initialize the propertychangesupport after loading
   */
  public TvShowSeason(@JsonProperty("season") int season) {
    // register for dirty flag listener
    super();

    this.season = season;

    init();
  }

  public TvShowSeason(int season, @NotNull TvShow tvShow) {
    this(season);

    this.tvShow = tvShow;
    this.tvShowDbId = tvShow.getDbId();
  }

  /**
   * copy constructor
   * 
   * @param source
   *          the other {@link TvShowSeason} to copy
   */
  public TvShowSeason(TvShowSeason source) {
    super.merge(source);

    this.tvShow = source.tvShow;
    this.tvShowDbId = source.tvShowDbId;

    this.season = source.season;

    init();
  }

  private void init() {
    listener = evt -> {
      if (evt.getSource() instanceof TvShowEpisode episode) {
        switch (evt.getPropertyName()) {
          case MEDIA_FILES -> firePropertyChange(MEDIA_FILES, null, evt.getNewValue());
          case SEASON -> {
            if (episode.getSeason() != season) {
              removeEpisode(episode);
            }
          }
          case FIRST_AIRED -> firePropertyChange(FIRST_AIRED, null, evt.getNewValue());
        }
      }
    };
  }

  public int getSeason() {
    return season;
  }

  @Override
  public MediaFile getMainFile() {
    return MediaFile.EMPTY_MEDIAFILE;
  }

  @Override
  public String getDataSource() {
    return tvShow == null ? "" : tvShow.getDataSource();
  }

  @Override
  public Date getReleaseDate() {
    return null;
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

  @Override
  public MediaRating getRating() {
    return MediaMetadata.EMPTY_RATING;
  }

  public void setTvShow(TvShow newValue) {
    TvShow oldValue = this.tvShow;
    this.tvShow = newValue;
    this.tvShowDbId = newValue.getDbId();
    firePropertyChange(TV_SHOW, oldValue, newValue);
  }

  public TvShow getTvShow() {
    return tvShow;
  }

  public UUID getTvShowDbId() {
    return tvShowDbId;
  }

  public void removeAllEpisodes() {
    List<TvShowEpisode> removedEpisodes = new ArrayList<>(episodes);
    removedEpisodes.addAll(dummyEpisodes);

    episodes.clear();
    dummyEpisodes.clear();

    for (TvShowEpisode episode : removedEpisodes) {
      episode.removePropertyChangeListener(listener);
      firePropertyChange(REMOVED_EPISODE, null, episode);
      firePropertyChange(FIRST_AIRED, null, getFirstAired());
    }
  }

  void removeDummyEpisodes() {
    dummyEpisodes.clear();
  }

  boolean isEmpty() {
    return episodes.isEmpty() && dummyEpisodes.isEmpty();
  }

  public synchronized void addEpisode(TvShowEpisode episode) {
    // do not add empty episodes
    if (episode == null) {
      return;
    }

    // to find out the delta (dummy)
    List<TvShowEpisode> episodesForDisplayBefore = null;
    if (TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()) {
      episodesForDisplayBefore = getEpisodesForDisplay();
    }

    if (episode.isDummy()) {
      if (!dummyEpisodes.contains(episode)) {
        dummyEpisodes.add(episode);
        dummyEpisodes.sort(TvShowEpisode::compareTo);
      }
    }
    else {
      if (!episodes.contains(episode)) {
        episodes.add(episode);
        episodes.sort(TvShowEpisode::compareTo);
      }
    }

    episode.addPropertyChangeListener(listener);

    // only fire the event when this episode is displayed
    List<TvShowEpisode> episodesForDisplayNow = getEpisodesForDisplay();
    if (episodesForDisplayNow.contains(episode)) {
      // upon the _first_ added episode, we do add the season itself
      if (episodesForDisplayNow.size() == 1) {
        firePropertyChange(ADDED_SEASON, null, this);
      }

      firePropertyChange(ADDED_EPISODE, null, episode);
      firePropertyChange(FIRST_AIRED, null, getFirstAired());
    }

    if (TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()) {
      for (TvShowEpisode e : ListUtils.nullSafe(episodesForDisplayBefore)) {
        if (!episodesForDisplayNow.contains(e)) {
          firePropertyChange(REMOVED_EPISODE, null, e);
          firePropertyChange(FIRST_AIRED, null, getFirstAired());
        }
      }
    }
  }

  public void removeEpisode(TvShowEpisode episode) {
    if (episode == null) {
      return;
    }

    // to find out the delta (dummy)
    List<TvShowEpisode> episodesForDisplayBefore = episodesForDisplayBefore = getEpisodesForDisplay();

    if (episode.isDummy()) {
      dummyEpisodes.remove(episode);
    }
    else {
      episodes.remove(episode);
    }

    episode.removePropertyChangeListener(listener);
    if (episodesForDisplayBefore.contains(episode)) {
      firePropertyChange(REMOVED_EPISODE, null, episode);
      firePropertyChange(FIRST_AIRED, null, getFirstAired());
    }

    if (TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()) {
      List<TvShowEpisode> episodesForDisplayNow = getEpisodesForDisplay();
      for (TvShowEpisode e : episodesForDisplayNow) {
        if (!episodesForDisplayBefore.contains(e)) {
          firePropertyChange(ADDED_EPISODE, null, e);
          firePropertyChange(FIRST_AIRED, null, getFirstAired());
        }
      }
    }

    if (getEpisodesForDisplay().isEmpty()) {
      firePropertyChange(REMOVED_SEASON, null, this);
    }
  }

  public List<TvShowEpisode> getEpisodes() {
    return Collections.unmodifiableList(episodes);
  }

  /**
   * get the firstAired of the first episode here
   * 
   * @return the first aired date of the first episode or null
   */
  public Date getFirstAired() {
    Date firstAired = null;

    for (TvShowEpisode episode : getEpisodesForDisplay()) {
      if (firstAired == null) {
        firstAired = episode.getFirstAired();
      }
      else if (episode.getFirstAired() != null && firstAired.after(episode.getFirstAired())) {
        firstAired = episode.getFirstAired();
      }
    }

    return firstAired;
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
      if (!episode.isDummy() && !episode.getHasSubtitles()) {
        subtitles = false;
        break;
      }
    }

    return subtitles;
  }

  /**
   * Checks if all episodes of that season have artwork assigned
   *
   * @return true if artwork is available
   */
  public Boolean getHasEpisodeImages() {
    TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

    for (TvShowEpisode episode : episodes) {
      if (!episode.isDummy() && !tvShowList.detectMissingArtwork(episode).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if all episodes of that season have valis metadata
   *
   * @return true if all chosen metadata fields are filled
   */
  public Boolean getHasEpisodeMetadata() {
    TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

    for (TvShowEpisode episode : episodes) {
      if (!episode.isDummy() && !tvShowList.detectMissingMetadata(episode).isEmpty()) {
        return false;
      }
    }

    return true;
  }

  public boolean isDummy() {
    return episodes.isEmpty();
  }

  public List<TvShowEpisode> getEpisodesForDisplay() {
    List<TvShowEpisode> episodes = new ArrayList<>(getEpisodes());

    // mix in unavailable episodes if the user wants to
    if (TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()) {
      // build up a set which holds a string representing the S/E indicator
      Set<String> availableEpisodes = new HashSet<>();

      for (TvShowEpisode episode : episodes) {
        if (episode.getSeason() > -1 && episode.getEpisode() > -1) {
          availableEpisodes.add("A" + episode.getSeason() + "." + episode.getEpisode());
        }
        if (episode.getDvdSeason() > -1 && episode.getDvdEpisode() > -1) {
          availableEpisodes.add("D" + episode.getSeason() + "." + episode.getEpisode());
        }
        if (episode.getAbsoluteNumber() > -1) {
          availableEpisodes.add("ABS" + episode.getAbsoluteNumber());
        }
      }

      // and now mix in unavailable ones
      for (TvShowEpisode episode : dummyEpisodes) {
        if (!TvShowHelpers.shouldAddDummyEpisode(episode)) {
          continue;
        }

        if (!availableEpisodes.contains("A" + episode.getSeason() + "." + episode.getEpisode())
            && !availableEpisodes.contains("D" + episode.getDvdSeason() + "." + episode.getDvdEpisode())
            && !availableEpisodes.contains("ABS" + episode.getAbsoluteNumber())) {
          episodes.add(episode);
        }
      }
    }

    episodes.sort(Comparator.comparingInt(TvShowEpisode::getEpisode));

    return episodes;
  }

  /**
   * Gets the dummy episode count
   *
   * @return the dummy episode count
   */
  public int getDummyEpisodeCount() {
    int count = 0;

    for (TvShowEpisode episode : getEpisodesForDisplay()) {
      if (episode.isDummy()) {
        if (!TvShowHelpers.shouldAddDummyEpisode(episode)) {
          continue;
        }

        count++;
      }
    }

    return count;
  }

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

  /**
   * gets all {@link MediaFile}s recursively (season + episodes)
   * 
   * @return a {@link List} of all {@link MediaFile}s
   */
  public List<MediaFile> getMediaFilesRecursive() {
    Set<MediaFile> unique = new LinkedHashSet<>(super.getMediaFiles());

    for (TvShowEpisode episode : episodes) {
      unique.addAll(episode.getMediaFiles());
    }

    return new ArrayList<>(unique);
  }

  /**
   * gets all {@link MediaFile}s recursively for the given type (season + episodes)
   *
   * @return a {@link List} of all {@link MediaFile}s
   */
  public List<MediaFile> getMediaFilesRecursive(MediaFileType type) {
    Set<MediaFile> unique = new LinkedHashSet<>(super.getMediaFiles(type));

    for (TvShowEpisode episode : episodes) {
      unique.addAll(episode.getMediaFiles(type));
    }

    return new ArrayList<>(unique);
  }

  @Override
  public boolean isNewlyAdded() {
    for (TvShowEpisode episode : episodes) {
      if (episode.isNewlyAdded()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void saveToDb() {
    TvShowModuleManager.getInstance().getTvShowList().persistSeason(this);
  }

  @Override
  public void callbackForGatheredMediainformation(MediaFile mediaFile) {
    // nothing to do
  }

  public void writeNfo() {
    ITvShowSeasonConnector connector;

    // only write the NFO if there is at least one episode existing (or the setting activated)
    if (!episodes.isEmpty() || TvShowModuleManager.getInstance().getSettings().isCreateMissingSeasonItems()) {
      List<TvShowSeasonNfoNaming> nfoNamings = TvShowModuleManager.getInstance().getSettings().getSeasonNfoFilenames();
      if (!nfoNamings.isEmpty()) {
        connector = switch (TvShowModuleManager.getInstance().getSettings().getTvShowConnector()) {
          default -> new TvShowSeasonToEmbyConnector(this);
        };

        connector.write(nfoNamings);

        firePropertyChange(HAS_NFO_FILE, false, true);
      }
    }
  }

  @Override
  public void callbackForWrittenArtwork(MediaArtworkType type) {
    // nothing to do
  }

  @Override
  protected Comparator<MediaFile> getMediaFileComparator() {
    return null;
  }

  /**
   * Gets the images to cache.
   *
   * @return the images to cache
   */
  @Override
  public List<MediaFile> getImagesToCache() {
    // get files to cache
    return getMediaFiles().stream().filter(MediaFile::isGraphic).toList();
  }

  public Object getValueForMetadata(TvShowScraperMetadataConfig metadataConfig) {
    return switch (metadataConfig) {
      case TITLE, SEASON_NAMES -> getTitle();
      case SEASON_OVERVIEW -> getPlot();
      case SEASON_POSTER -> getArtworkFilename(MediaFileType.SEASON_POSTER);
      case SEASON_FANART -> getArtworkFilename(MediaFileType.SEASON_FANART);
      case SEASON_BANNER -> getArtworkFilename(MediaFileType.SEASON_BANNER);
      case SEASON_THUMB -> getArtworkFilename(MediaFileType.SEASON_THUMB);
      default -> null;
    };

  }

  @Override
  public String getTitle() {
    if (this.title.isEmpty() && tvShow != null) {
      return tvShow.getSeasonName(season); // could also be blank, but handled in TvShowTreeDataProvider
    }
    return this.title;
  }

  @Override
  public int compareTo(TvShowSeason o) {
    if (getTvShow() != o.getTvShow()) {
      return getTvShow().getTitle().compareTo(o.getTvShow().getTitle());
    }
    return Integer.compare(getSeason(), o.getSeason());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TvShowSeason that = (TvShowSeason) o;
    return season == that.season && Objects.equals(tvShow, that.tvShow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tvShow, season);
  }
}
