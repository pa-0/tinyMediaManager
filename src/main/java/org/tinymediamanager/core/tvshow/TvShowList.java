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
package org.tinymediamanager.core.tvshow;

import static org.tinymediamanager.core.Constants.ADDED_TV_SHOW;
import static org.tinymediamanager.core.Constants.EPISODE_COUNT;
import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;
import static org.tinymediamanager.core.Constants.REMOVED_TV_SHOW;
import static org.tinymediamanager.core.Constants.TAGS;
import static org.tinymediamanager.core.Constants.TV_SHOWS;
import static org.tinymediamanager.core.Constants.TV_SHOW_COUNT;

import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.h2.mvstore.MVMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.TmmOsUtils;
import org.tinymediamanager.UpgradeTasks;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.tvshow.TvShowEpisodeAndSeasonParser.EpisodeMatchingResult;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.license.License;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;

import com.fasterxml.jackson.databind.ObjectReader;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;

/**
 * The Class TvShowList.
 * 
 * @author Manuel Laggner
 */
public final class TvShowList extends AbstractModelObject {
  private static final Logger                            LOGGER        = LoggerFactory.getLogger(TvShowList.class);
  private static TvShowList                              instance      = null;

  private final List<TvShow>                             tvShows;

  private final CopyOnWriteArrayList<String>             tagsInTvShows;
  private final CopyOnWriteArrayList<String>             tagsInEpisodes;
  private final CopyOnWriteArrayList<String>             videoCodecsInEpisodes;
  private final CopyOnWriteArrayList<String>             videoContainersInEpisodes;
  private final CopyOnWriteArrayList<String>             audioCodecsInEpisodes;
  private final CopyOnWriteArrayList<Integer>            audioChannelsInEpisodes;
  private final CopyOnWriteArrayList<Double>             frameRatesInEpisodes;
  private final CopyOnWriteArrayList<MediaCertification> certificationsInTvShows;
  private final CopyOnWriteArrayList<Integer>            audioStreamsInEpisodes;
  private final CopyOnWriteArrayList<String>             audioLanguagesInEpisodes;
  private final CopyOnWriteArrayList<Integer>            subtitlesInEpisodes;
  private final CopyOnWriteArrayList<String>             subtitleLanguagesInEpisodes;
  private final CopyOnWriteArrayList<String>             subtitleFormatsInEpisodes;

  private final CopyOnWriteArrayList<String>             hdrFormatInEpisodes;
  private final CopyOnWriteArrayList<String>             audioTitlesInEpisodes;

  private final PropertyChangeListener                   propertyChangeListener;
  private final ReadWriteLock                            readWriteLock = new ReentrantReadWriteLock();

  /**
   * Instantiates a new TvShowList.
   */
  private TvShowList() {
    // create the lists
    tvShows = new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()), GlazedLists.beanConnector(TvShow.class));
    tagsInTvShows = new CopyOnWriteArrayList<>();
    tagsInEpisodes = new CopyOnWriteArrayList<>();
    videoCodecsInEpisodes = new CopyOnWriteArrayList<>();
    videoContainersInEpisodes = new CopyOnWriteArrayList<>();
    audioCodecsInEpisodes = new CopyOnWriteArrayList<>();
    audioChannelsInEpisodes = new CopyOnWriteArrayList<>();
    frameRatesInEpisodes = new CopyOnWriteArrayList<>();
    certificationsInTvShows = new CopyOnWriteArrayList<>();
    audioStreamsInEpisodes = new CopyOnWriteArrayList<>();
    audioLanguagesInEpisodes = new CopyOnWriteArrayList<>();
    subtitlesInEpisodes = new CopyOnWriteArrayList<>();
    subtitleLanguagesInEpisodes = new CopyOnWriteArrayList<>();
    hdrFormatInEpisodes = new CopyOnWriteArrayList<>();
    audioTitlesInEpisodes = new CopyOnWriteArrayList<>();
    subtitleFormatsInEpisodes = new CopyOnWriteArrayList<>();

    // the tag listener: it's used to always have a full list of all tags used in tmm
    propertyChangeListener = evt -> {
      // listen to changes of tags
      if (Constants.TAGS.equals(evt.getPropertyName()) && evt.getSource() instanceof TvShow tvShow) {
        updateTvShowTags(Collections.singleton(tvShow));
      }
      if (Constants.TAGS.equals(evt.getPropertyName()) && evt.getSource() instanceof TvShowEpisode episode) {
        updateEpisodeTags(Collections.singleton(episode));
      }
      if ((MEDIA_FILES.equals(evt.getPropertyName()) || MEDIA_INFORMATION.equals(evt.getPropertyName()))
          && evt.getSource() instanceof TvShowEpisode episode) {
        updateMediaInformationLists(Collections.singleton(episode));
      }
      if (EPISODE_COUNT.equals(evt.getPropertyName())) {
        firePropertyChange(EPISODE_COUNT, evt.getOldValue(), evt.getNewValue());
      }
    };

    License.getInstance().addEventListener(() -> {
      firePropertyChange(TV_SHOW_COUNT, 0, tvShows.size());
      firePropertyChange(EPISODE_COUNT, 0, 1);
    });
  }

  /**
   * Gets the single instance of TvShowList.
   * 
   * @return single instance of TvShowList
   */
  static synchronized TvShowList getInstance() {
    if (instance == null) {
      instance = new TvShowList();
    }

    return instance;
  }

  /**
   * removes the active instance <br>
   * <b>Should only be used for unit testing et all!</b><br>
   */
  static void clearInstance() {
    instance = null;
  }

  /**
   * Gets the tv shows.
   *
   * @return the tv shows
   */
  public List<TvShow> getTvShows() {
    return tvShows;
  }

  /**
   * Gets all episodes
   *
   * @return all episodes
   */
  public List<TvShowEpisode> getEpisodes() {
    List<TvShowEpisode> newEp = new ArrayList<>();
    for (TvShow show : tvShows) {
      newEp.addAll(show.getEpisodes());
    }
    return newEp;
  }

  public void debugListDuplicateEpisode() {
    int cnt = 0;
    for (TvShow show : tvShows) {
      List<int[]> dupes = show.getDuplicateEpisodes();
      cnt += dupes.size();
      if (!dupes.isEmpty()) {
        System.out.println("---------------------");
        System.out.println("Dupes found! - " + show.getTitle());
        for (int[] se : dupes) {
          List<TvShowEpisode> eps = show.getEpisode(se[0], se[1]);
          for (TvShowEpisode ep : eps) {
            System.out.println(Arrays.toString(se) + ((eps.size() > 2) ? " MULTI" + eps.size() : "") + " - "
                + Utils.relPath(show.getPathNIO(), ep.getMainFile().getFileAsPath()));
          }
        }
      }
    }
    System.out.println("Found " + cnt + " episodes with same number!");
  }

  public void debugFindWronglyMatchedEpisodes() {
    for (TvShow show : tvShows) {
      boolean first = true;
      for (TvShowEpisode ep : show.getEpisodes()) {
        MediaFile mf = ep.getMainFile();
        String rel = show.getPathNIO().relativize(mf.getFileAsPath()).toString();
        EpisodeMatchingResult result = TvShowEpisodeAndSeasonParser.detectEpisodeFromFilename(rel, show.getTitle());
        if (!result.episodes.contains(ep.getEpisode()) || result.season != ep.getSeason()) {
          if (first) {
            System.out.println("---------------------");
            System.out.println("Episode matching found some differences! - " + show.getTitle());
            first = false;
          }
          System.out.println(
              "S:" + ep.getSeason() + " E:" + ep.getEpisode() + " - " + rel + "   now detected as S:" + result.season + " E:" + result.episodes);
        }
      }
    }
  }

  /**
   * get all specified trailer scrapers.
   *
   * @param providerIds
   *          the scrapers
   * @return the trailer providers
   */
  public List<MediaScraper> getTrailerScrapers(List<String> providerIds) {
    List<MediaScraper> trailerScrapers = new ArrayList<>();

    for (String providerId : providerIds) {
      if (StringUtils.isBlank(providerId)) {
        continue;
      }
      MediaScraper trailerScraper = MediaScraper.getMediaScraperById(providerId, ScraperType.TVSHOW_TRAILER);
      if (trailerScraper != null) {
        trailerScrapers.add(trailerScraper);
      }
    }

    return trailerScrapers;
  }

  /**
   * all available trailer scrapers.
   *
   * @return the trailer scrapers
   */
  public List<MediaScraper> getAvailableTrailerScrapers() {
    return MediaScraper.getMediaScrapers(ScraperType.TVSHOW_TRAILER);
  }

  /**
   * Gets the unscraped TvShows
   *
   * @return the unscraped TvShows
   */
  public List<TvShow> getUnscrapedTvShows() {
    return tvShows.parallelStream().filter(tvShow -> !tvShow.isScraped()).collect(Collectors.toList());
  }

  /**
   * Adds the tv show.
   * 
   * @param newValue
   *          the new value
   */
  public void addTvShow(TvShow newValue) {
    readWriteLock.writeLock().lock();
    int oldValue = tvShows.size();
    tvShows.add(newValue);
    readWriteLock.writeLock().unlock();

    newValue.addPropertyChangeListener(propertyChangeListener);
    firePropertyChange(TV_SHOWS, null, tvShows);
    firePropertyChange(ADDED_TV_SHOW, null, newValue);
    firePropertyChange(TV_SHOW_COUNT, oldValue, tvShows.size());
  }

  /**
   * Removes the datasource.
   * 
   * @param path
   *          the path
   */
  public void removeDatasource(String path) {
    if (StringUtils.isEmpty(path)) {
      return;
    }

    for (int i = tvShows.size() - 1; i >= 0; i--) {
      TvShow tvShow = tvShows.get(i);
      if (Paths.get(path).equals(Paths.get(tvShow.getDataSource()))) {
        removeTvShow(tvShow);
      }
    }
  }

  /**
   * exchanges the given datasource in the entities/database with a new one
   */
  void exchangeDatasource(String oldDatasource, String newDatasource) {
    Path oldPath = Paths.get(oldDatasource);
    List<TvShow> tvShowsToChange = tvShows.stream().filter(tvShow -> oldPath.equals(Paths.get(tvShow.getDataSource()))).toList();
    List<MediaFile> imagesToCache = new ArrayList<>();

    for (TvShow tvShow : tvShowsToChange) {
      Path oldTvShowPath = tvShow.getPathNIO();
      Path newTvShowPath;

      try {
        // try to _cleanly_ calculate the relative path
        newTvShowPath = Paths.get(newDatasource, Paths.get(tvShow.getDataSource()).relativize(oldTvShowPath).toString());
      }
      catch (Exception e) {
        // if that fails (maybe migrate from windows to linux/macos), just try a simple string replacement
        newTvShowPath = Paths.get(newDatasource, FilenameUtils.separatorsToSystem(tvShow.getPath().replace(tvShow.getDataSource(), "")));
      }

      tvShow.setDataSource(newDatasource);
      tvShow.setPath(newTvShowPath.toAbsolutePath().toString());
      tvShow.updateMediaFilePath(oldTvShowPath, newTvShowPath);

      for (TvShowEpisode episode : new ArrayList<>(tvShow.getEpisodes())) {
        episode.setDataSource(newDatasource);
        episode.replacePathForRenamedTvShowRoot(oldTvShowPath, newTvShowPath);
        episode.updateMediaFilePath(oldTvShowPath, newTvShowPath);
        episode.saveToDb();

        // re-build the image cache afterwards in an own thread
        imagesToCache.addAll(episode.getImagesToCache());
      }

      tvShow.saveToDb(); // since we moved already, save it

      // re-build the image cache afterwards in an own thread
      imagesToCache.addAll(tvShow.getImagesToCache());
    }

    if (!imagesToCache.isEmpty()) {
      imagesToCache.forEach(ImageCache::cacheImageAsync);
    }
  }

  /**
   * Removes the tv show.
   * 
   * @param tvShow
   *          the tvShow
   */
  public void removeTvShow(TvShow tvShow) {
    readWriteLock.writeLock().lock();
    int oldValue = tvShows.size();
    // first remove the TV show itself to deregister the events in the UI (no more UI handling of the tbe removed episodes needed)
    tvShows.remove(tvShow);
    readWriteLock.writeLock().unlock();

    firePropertyChange(TV_SHOWS, null, tvShows);
    firePropertyChange(REMOVED_TV_SHOW, null, tvShow);
    firePropertyChange(TV_SHOW_COUNT, oldValue, tvShows.size());

    // last but not least - remove all episodes
    for (TvShowEpisode episode : tvShow.getEpisodes()) {
      TvShowModuleManager.getInstance().getTvShowList().removeEpisodeFromDb(episode);

      // and remove the image cache
      for (MediaFile mf : episode.getMediaFiles()) {
        if (mf.isGraphic()) {
          ImageCache.invalidateCachedImage(mf);
        }
      }
    }

    // and remove it and all seasons from the DB
    for (TvShowSeason season : tvShow.getSeasons()) {
      try {
        TvShowModuleManager.getInstance().removeSeasonFromDb(season);
      }
      catch (Exception e) {
        LOGGER.error("problem removing season from DB: {}", e.getMessage());
      }
    }

    try {
      TvShowModuleManager.getInstance().removeTvShowFromDb(tvShow);
    }
    catch (Exception e) {
      LOGGER.error("problem removing TV show from DB: {}", e.getMessage());
    }

    // and remove the image cache
    for (MediaFile mf : tvShow.getMediaFiles()) {
      if (mf.isGraphic()) {
        ImageCache.invalidateCachedImage(mf);
      }
    }
  }

  /**
   * Removes the tv show from tmm and deletes all files from the data source
   * 
   * @param tvShow
   *          the tvShow
   */
  public void deleteTvShow(TvShow tvShow) {
    readWriteLock.writeLock().lock();
    int oldValue = tvShows.size();
    tvShows.remove(tvShow);
    readWriteLock.writeLock().unlock();

    tvShow.deleteFilesSafely();

    for (TvShowEpisode episode : tvShow.getEpisodes()) {
      TvShowModuleManager.getInstance().getTvShowList().removeEpisodeFromDb(episode);

      // and remove the image cache
      for (MediaFile mf : episode.getMediaFiles()) {
        if (mf.isGraphic()) {
          ImageCache.invalidateCachedImage(mf);
        }
      }
    }

    // and remove it and all seasons from the DB
    for (TvShowSeason season : tvShow.getSeasons()) {
      try {
        TvShowModuleManager.getInstance().removeSeasonFromDb(season);
      }
      catch (Exception e) {
        LOGGER.error("problem removing season from DB: {}", e.getMessage());
      }
    }

    try {
      TvShowModuleManager.getInstance().removeTvShowFromDb(tvShow);
    }
    catch (Exception e) {
      LOGGER.error("problem removing TV show from DB: {}", e.getMessage());
    }

    // and remove the image cache
    for (MediaFile mf : tvShow.getMediaFiles()) {
      if (mf.isGraphic()) {
        ImageCache.invalidateCachedImage(mf);
      }
    }

    firePropertyChange(TV_SHOWS, null, tvShows);
    firePropertyChange(REMOVED_TV_SHOW, null, tvShow);
    firePropertyChange(TV_SHOW_COUNT, oldValue, tvShows.size());
  }

  /**
   * Gets the tv show count.
   * 
   * @return the tv show count
   */
  public int getTvShowCount() {
    return tvShows.size();
  }

  /**
   * Gets the episode count.
   * 
   * @return the episode count
   */
  public int getEpisodeCount() {
    int count = 0;
    for (TvShow tvShow : tvShows) {
      count += tvShow.getEpisodeCount();
    }

    return count;
  }

  /**
   * Gets the dummy episode count
   *
   * @return the dummy episode count
   */
  public int getDummyEpisodeCount() {
    int count = 0;
    for (TvShow tvShow : tvShows) {
      count += tvShow.getDummyEpisodeCount();
    }

    return count;
  }

  /**
   * are there any dummy episodes?
   * 
   * @return true/false
   */
  public boolean hasDummyEpisodes() {
    for (TvShow tvShow : tvShows) {
      if (tvShow.getDummyEpisodeCount() > 0) {
        return true;
      }
    }
    return false;
  }

  public TvShow lookupTvShow(UUID uuid) {
    for (TvShow tvShow : tvShows) {
      if (tvShow.getDbId().equals(uuid)) {
        return tvShow;
      }
    }
    return null;
  }

  /**
   * Load tv shows from database.
   */
  void loadTvShowsFromDatabase(MVMap<UUID, String> tvShowMap, MVMap<UUID, String> seasonMap, MVMap<UUID, String> episodesMap) {
    LOGGER.info("Loading {} TvShows from database...", tvShowMap.size());
    TvShowModuleManager module = TvShowModuleManager.getInstance();

    //////////////////////////////////////////////////
    // load all TV shows from the database
    //////////////////////////////////////////////////
    Set<TvShow> tvShowsFromDb = new HashSet<>();
    ObjectReader tvShowObjectReader = TvShowModuleManager.getInstance().getTvShowObjectReader();
    long dummyCnt = 0;

    List<UUID> toRemove = new ArrayList<>();
    long start = System.nanoTime();
    new ArrayList<>(tvShowMap.keyList()).forEach(uuid -> {
      String json = "";
      try {
        json = tvShowMap.get(uuid);
        TvShow tvShow = tvShowObjectReader.readValue(json);
        tvShow.setDbId(uuid);

        // some sanity checks
        if (isCorrupt(tvShow)) {
          LOGGER.error("Removing corrupt show: {}", json);
          toRemove.add(uuid);
          return;
        }

        // inline upgrade from v4 - performance!
        if (module.getDbVersion() < 5002) {
          tvShow.getDummyEpisodes().forEach(UpgradeTasks::upgradeEpisodeNumbers);
        }

        // for performance reasons we add tv shows after loading the episodes
        if (!tvShowsFromDb.add(tvShow)) {
          // already in there?! remove dupe
          LOGGER.info("removed duplicate '{}'", tvShow.getTitle());
          toRemove.add(uuid);
        }
      }
      catch (Exception e) {
        LOGGER.warn("problem decoding TV show json string: {}", e.getMessage());
        LOGGER.info("dropping corrupt TV show: {}", json);
        toRemove.add(uuid);
      }
    });
    long end = System.nanoTime();
    // remove orphaned defect TV shows
    for (UUID uuid : toRemove) {
      tvShowMap.remove(uuid);
    }
    LOGGER.debug("took {} ms", (end - start) / 1000000);

    // build a map for faster show lookup
    Map<UUID, TvShow> tvShowUuidMap = new HashMap<>();
    for (TvShow tvShow : tvShowsFromDb) {
      tvShowUuidMap.put(tvShow.getDbId(), tvShow);
      dummyCnt += tvShow.getDummyEpisodes().size(); // want the RAW entries
    }

    //////////////////////////////////////////////////
    // load all seasons from the database
    //////////////////////////////////////////////////
    LOGGER.info("Loading {} seasons from database...", seasonMap.size());
    toRemove.clear();
    ObjectReader seasonObjectReader = TvShowModuleManager.getInstance().getSeasonObjectReader();

    start = System.nanoTime();
    new ArrayList<>(seasonMap.keyList()).forEach(uuid -> {
      String json = "";
      try {
        json = seasonMap.get(uuid);
        TvShowSeason season = seasonObjectReader.readValue(json);
        season.setDbId(uuid);

        // assign it to the right TV show
        TvShow tvShow = tvShowUuidMap.get(season.getTvShowDbId());
        if (tvShow != null) {
          season.setTvShow(tvShow);
          tvShow.addSeason(season);
        }
        else {
          // or remove orphans
          toRemove.add(uuid);
        }
      }
      catch (Exception e) {
        LOGGER.warn("problem decoding episode json string: {}", e.getMessage());
        LOGGER.info("dropping corrupt episode: {}", json);
        toRemove.add(uuid);
      }
    });
    end = System.nanoTime();
    // remove orphaned seasons
    for (UUID uuid : toRemove) {
      seasonMap.remove(uuid);
    }
    LOGGER.debug("took {} ms", (end - start) / 1000000);

    //////////////////////////////////////////////////
    // load all episodes from the database
    //////////////////////////////////////////////////
    LOGGER.info("Loading {} episodes from database... (+ {} dummy w/o physical file)", episodesMap.size(), dummyCnt);
    toRemove.clear();
    ObjectReader episodeObjectReader = TvShowModuleManager.getInstance().getEpisodeObjectReader();

    start = System.nanoTime();
    new ArrayList<>(episodesMap.keyList()).forEach(uuid -> {
      String json = "";
      try {
        json = episodesMap.get(uuid);
        TvShowEpisode episode = episodeObjectReader.readValue(json);
        episode.setDbId(uuid);

        // some sanity checks
        if (isCorrupt(episode)) {
          LOGGER.error("Removing corrupt episode: {}", json);
          toRemove.add(uuid);
          return;
        }

        // inline upgrade from v4 - performance!
        if (module.getDbVersion() < 5002) {
          UpgradeTasks.upgradeEpisodeNumbers(episode);
        }

        // assign it to the right TV show
        TvShow tvShow = tvShowUuidMap.get(episode.getTvShowDbId());
        if (tvShow != null) {
          episode.setTvShow(tvShow);
          tvShow.addEpisode(episode);
        }
        else {
          // or remove orphans
          toRemove.add(uuid);
        }
      }
      catch (Exception e) {
        LOGGER.warn("problem decoding episode json string: {}", e.getMessage());
        LOGGER.info("dropping corrupt episode: {}", json);
        toRemove.add(uuid);
      }
    });
    end = System.nanoTime();
    // remove orphaned episodes
    for (UUID uuid : toRemove) {
      episodesMap.remove(uuid);
    }
    LOGGER.debug("took {} ms", (end - start) / 1000000);

    //////////////////////////////////////////////////
    // cleanup: remove shows with empty episodes
    //////////////////////////////////////////////////
    toRemove.clear();
    for (TvShow tvShow : tvShowsFromDb) {
      if (tvShow.getEpisodeCount() == 0) {
        toRemove.add(tvShow.getDbId());
      }
    }
    for (UUID uuid : toRemove) {
      TvShow tvShow = tvShowUuidMap.get(uuid);
      tvShowsFromDb.remove(tvShow);
    }

    // and add all TV shows to the UI
    tvShows.addAll(tvShowsFromDb);
  }

  void initDataAfterLoading() {
    // check for corrupted media entities
    checkAndCleanupMediaFiles();

    List<TvShowEpisode> episodes = new ArrayList<>();

    // init everything after loading
    for (TvShow tvShow : tvShows) {
      tvShow.initializeAfterLoading();

      for (TvShowSeason season : tvShow.getSeasons()) {
        season.initializeAfterLoading();
      }

      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        episode.initializeAfterLoading();
        episodes.add(episode);
      }

      tvShow.addPropertyChangeListener(propertyChangeListener);
    }

    updateTvShowTags(tvShows);
    updateCertification(tvShows);

    updateEpisodeTags(episodes);
    updateMediaInformationLists(episodes);
  }

  private boolean isCorrupt(TvShow show) {
    if (StringUtils.isBlank(show.getPath())) {
      LOGGER.error("TvShow without path - dropping");
      return true;
    }
    if (StringUtils.isBlank(show.getDataSource())) {
      LOGGER.error("TvShow without datasource - dropping");
      return true;
    }
    if (TmmOsUtils.hasInvalidCharactersForFilesystem(show.getPath())) {
      LOGGER.error("TvShow with invalid characters for this OS - dropping");
      return true;
    }
    return false;
  }

  private boolean isCorrupt(TvShowEpisode episode) {
    if (episode.getMediaFiles(MediaFileType.VIDEO).isEmpty()) {
      LOGGER.error("Episode S{} E{} without MediaFiles - dropping", episode.getSeason(), episode.getEpisode());
      return true;
    }
    if (StringUtils.isBlank(episode.getPath())) {
      LOGGER.error("Episode S{} E{} without path - dropping", episode.getSeason(), episode.getEpisode());
      return true;
    }
    if (TmmOsUtils.hasInvalidCharactersForFilesystem(episode.getPath())) {
      LOGGER.error("Episode S{} E{} with invalid characters for this OS - dropping", episode.getSeason(), episode.getEpisode());
      return true;
    }
    return false;
  }

  public void persistTvShow(TvShow tvShow) {
    // sanity checks
    try {
      if (!tvShows.contains(tvShow)) {
        throw new IllegalArgumentException(tvShow.getPathNIO().toString());
      }
    }
    catch (Exception e) {
      LOGGER.debug("not persisting TV show - not in tvShowList", e);
      return;
    }

    // update/insert this TV show to the database
    try {
      TvShowModuleManager.getInstance().persistTvShow(tvShow);
    }
    catch (Exception e) {
      LOGGER.error("failed to persist episode: {} - {}", tvShow.getTitle(), e.getMessage());
    }
  }

  public void persistSeason(TvShowSeason season) {
    // update/insert this episode to the database
    try {
      TvShowModuleManager.getInstance().persistSeason(season);
    }
    catch (Exception e) {
      LOGGER.error("failed to persist season: {} - S{} : {}", season.getTvShow().getTitle(), season.getSeason(), e.getMessage());
    }
  }

  public void persistEpisode(TvShowEpisode episode) {
    // sanity check
    if (isCorrupt(episode)) {
      // remove corrupt episode
      LOGGER.info("Cannot persist episode {} - \"S{}E{}\" - dropping", episode.getTvShow().getTitle(), episode.getSeason(), episode.getEpisode());
      removeEpisodeFromDb(episode);
    }
    else {
      // update/insert this episode to the database
      try {
        TvShowModuleManager.getInstance().persistEpisode(episode);
      }
      catch (Exception e) {
        LOGGER.error("failed to persist episode: {} - S{}E{} - {} : {}", episode.getTvShow().getTitle(), episode.getSeason(), episode.getEpisode(),
            episode.getTitle(), e.getMessage());
      }
    }
  }

  public void removeEpisodeFromDb(TvShowEpisode episode) {
    // delete this episode from the database
    try {
      TvShowModuleManager.getInstance().removeEpisodeFromDb(episode);
    }
    catch (Exception e) {
      LOGGER.error("failed to remove episode: {} - S{}E{} - {} : {}", episode.getTvShow().getTitle(), episode.getSeason(), episode.getEpisode(),
          episode.getTitle(), e.getMessage());
    }
  }

  public MediaScraper getDefaultMediaScraper() {
    MediaScraper scraper = MediaScraper.getMediaScraperById(TvShowModuleManager.getInstance().getSettings().getScraper(), ScraperType.TV_SHOW);
    if (scraper == null || !scraper.isEnabled()) {
      scraper = MediaScraper.getMediaScraperById(MediaMetadata.TMDB, ScraperType.TV_SHOW);
    }
    return scraper;
  }

  public MediaScraper getMediaScraperById(String providerId) {
    return MediaScraper.getMediaScraperById(providerId, ScraperType.TV_SHOW);
  }

  public List<MediaScraper> getAvailableMediaScrapers() {
    List<MediaScraper> availableScrapers = MediaScraper.getMediaScrapers(ScraperType.TV_SHOW);
    availableScrapers.sort(new TvShowMediaScraperComparator());
    return availableScrapers;
  }

  /**
   * Gets all available artwork scrapers.
   * 
   * @return the artwork scrapers
   */
  public List<MediaScraper> getAvailableArtworkScrapers() {
    List<MediaScraper> availableScrapers = MediaScraper.getMediaScrapers(ScraperType.TVSHOW_ARTWORK);
    // we can use the TvShowMediaScraperComparator here too, since TheTvDb should also be first
    availableScrapers.sort(new TvShowMediaScraperComparator());
    return availableScrapers;
  }

  /**
   * get all specified artwork scrapers
   * 
   * @return the specified artwork scrapers
   */
  public List<MediaScraper> getArtworkScrapers(List<String> providerIds) {
    List<MediaScraper> artworkScrapers = new ArrayList<>();

    for (String providerId : providerIds) {
      if (StringUtils.isBlank(providerId)) {
        continue;
      }
      MediaScraper artworkScraper = MediaScraper.getMediaScraperById(providerId, ScraperType.TVSHOW_ARTWORK);
      if (artworkScraper != null) {
        artworkScrapers.add(artworkScraper);
      }
    }

    return artworkScrapers;
  }

  /**
   * get all default (specified via settings) artwork scrapers
   * 
   * @return the specified artwork scrapers
   */
  public List<MediaScraper> getDefaultArtworkScrapers() {
    List<MediaScraper> defaultScrapers = getArtworkScrapers(TvShowModuleManager.getInstance().getSettings().getArtworkScrapers());
    return defaultScrapers.stream().filter(MediaScraper::isActive).toList();
  }

  /**
   * Search tv show with the default language.
   * 
   * @param searchTerm
   *          the search term
   * @param year
   *          the year of the TV show (if available, otherwise <= 0)
   * @param ids
   *          a map of all available ids of the TV show or null if no id based search is requested
   * @param mediaScraper
   *          the media scraper
   * @throws ScrapeException
   *           any {@link ScrapeException} occurred while searching
   * @return the list
   */
  public List<MediaSearchResult> searchTvShow(String searchTerm, int year, Map<String, Object> ids, MediaScraper mediaScraper)
      throws ScrapeException {
    return searchTvShow(searchTerm, year, ids, mediaScraper, TvShowModuleManager.getInstance().getSettings().getScraperLanguage());
  }

  /**
   * Search tv show with the chosen language.
   * 
   * @param searchTerm
   *          the search term
   * @param year
   *          the year of the TV show (if available, otherwise <= 0)
   * @param ids
   *          a map of all available ids of the TV show or null if no id based search is requested
   * @param mediaScraper
   *          the media scraper
   * @param language
   *          the language to search with
   * @throws ScrapeException
   *           any {@link ScrapeException} occurred while searching
   * @return the list
   */
  public List<MediaSearchResult> searchTvShow(String searchTerm, int year, Map<String, Object> ids, MediaScraper mediaScraper,
      MediaLanguages language) throws ScrapeException {

    if (mediaScraper == null || !mediaScraper.isEnabled()) {
      return Collections.emptyList();
    }

    Set<MediaSearchResult> results = new TreeSet<>();
    ITvShowMetadataProvider provider = (ITvShowMetadataProvider) mediaScraper.getMediaProvider();

    Pattern tmdbPattern = Pattern.compile("https://www.themoviedb.org/tv/(.*?)-.*");

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setSearchQuery(searchTerm);
    options.setLanguage(language);
    options.setCertificationCountry(TvShowModuleManager.getInstance().getSettings().getCertificationCountry());
    options.setReleaseDateCountry(TvShowModuleManager.getInstance().getSettings().getReleaseDateCountry());

    if (ids != null) {
      options.setIds(ids);
    }

    if (!searchTerm.isEmpty()) {
      String query = searchTerm.toLowerCase(Locale.ROOT);

      if (MediaIdUtil.isValidImdbId(query)) {
        options.setImdbId(query);
      }
      else if (query.startsWith("imdb:")) {
        String imdbId = query.replace("imdb:", "");
        if (MediaIdUtil.isValidImdbId(imdbId)) {
          options.setImdbId(imdbId);
        }
      }
      else if (query.startsWith("https://www.imdb.com/title/")) {
        String imdbId = query.split("/")[4];
        if (MediaIdUtil.isValidImdbId(imdbId)) {
          options.setImdbId(imdbId);
        }
      }
      else if (query.startsWith("tmdb:")) {
        try {
          int tmdbId = Integer.parseInt(query.replace("tmdb:", ""));
          if (tmdbId > 0) {
            options.setTmdbId(tmdbId);
          }
        }
        catch (Exception e) {
          // ignored
        }
      }
      else if (tmdbPattern.matcher(query).matches()) {
        try {
          int tmdbId = Integer.parseInt(tmdbPattern.matcher(query).replaceAll("$1"));
          if (tmdbId > 0) {
            options.setTmdbId(tmdbId);
          }
        }
        catch (Exception e) {
          // ignored
        }
      }
      else if (query.startsWith("tvdb:")) {
        try {
          int tvdbId = Integer.parseInt(query.replace("tvdb:", ""));
          if (tvdbId > 0) {
            options.setId(MediaMetadata.TVDB, tvdbId);
          }
        }
        catch (Exception e) {
          // ignored
        }
      }
      options.setSearchQuery(searchTerm);
    }

    if (year > 0) {
      options.setSearchYear(year);
    }

    LOGGER.info("=====================================================");
    LOGGER.info("Searching with scraper: {}", provider.getProviderInfo().getId());
    LOGGER.info("options: {}", options);
    LOGGER.info("=====================================================");
    results.addAll(provider.search(options));

    // Fallback:
    // check if title starts with a year, and remove/retry...
    if (results.isEmpty() && options.getSearchQuery().matches("^\\d{4}.*")) {
      TvShowSearchAndScrapeOptions o = new TvShowSearchAndScrapeOptions(options); // copy
      o.setSearchQuery(options.getSearchQuery().substring(4));
      LOGGER.info("=====================================================");
      LOGGER.info("Searching again without year in title: {}", provider.getProviderInfo().getId());
      LOGGER.info("options: {}", o);
      LOGGER.info("=====================================================");
      results.addAll(provider.search(o));
    }

    return new ArrayList<>(results);
  }

  private void updateTvShowTags(Collection<TvShow> tvShows) {
    Set<String> tags = new HashSet<>();
    tvShows.forEach(tvShow -> tags.addAll(tvShow.getTags()));

    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(tagsInTvShows, tags)) {
      Utils.removeDuplicateStringFromCollectionIgnoreCase(tagsInTvShows);
      firePropertyChange(TAGS, null, tagsInTvShows);
    }
  }

  private void updateCertification(Collection<TvShow> tvShows) {
    Set<MediaCertification> certifications = new HashSet<>();
    tvShows.forEach(tvShow -> certifications.add(tvShow.getCertification()));

    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(certificationsInTvShows, certifications)) {
      firePropertyChange(Constants.CERTIFICATION, null, certificationsInTvShows);
    }
  }

  public List<String> getTagsInTvShows() {
    return tagsInTvShows;
  }

  private void updateEpisodeTags(Collection<TvShowEpisode> episodes) {
    Set<String> tags = new HashSet<>();
    episodes.forEach(episode -> tags.addAll(episode.getTags()));

    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(tagsInEpisodes, tags)) {
      Utils.removeDuplicateStringFromCollectionIgnoreCase(tagsInEpisodes);
      firePropertyChange(TAGS, null, tagsInEpisodes);
    }
  }

  public Collection<String> getTagsInEpisodes() {
    return tagsInEpisodes;
  }

  private void updateMediaInformationLists(Collection<TvShowEpisode> episodes) {
    Set<String> videoCodecs = new HashSet<>();
    Set<Double> frameRates = new HashSet<>();
    Map<String, String> videoContainers = new HashMap<>();
    Set<String> audioCodecs = new HashSet<>();
    Set<Integer> audioChannels = new HashSet<>();
    Set<Integer> audioStreamCount = new HashSet<>();
    Set<String> audioLanguages = new HashSet<>();
    Set<Integer> subtitleStreamCount = new HashSet<>();
    Set<String> subtitleLanguages = new HashSet<>();
    Set<String> hdrFormat = new HashSet<>();
    Set<String> audioTitles = new HashSet<>();
    Set<String> subtitleFormats = new HashSet<>();

    for (TvShowEpisode episode : episodes) {
      int audioCount = 0;
      int subtitleCount = 0;

      boolean first = true;

      for (MediaFile mf : episode.getMediaFiles(MediaFileType.VIDEO)) {
        // video codec
        if (StringUtils.isNotBlank(mf.getVideoCodec())) {
          videoCodecs.add(mf.getVideoCodec());
        }

        // frame rate
        if (mf.getFrameRate() > 0) {
          frameRates.add(mf.getFrameRate());
        }

        // video container
        if (StringUtils.isNotBlank(mf.getContainerFormat())) {
          videoContainers.putIfAbsent(mf.getContainerFormat().toLowerCase(Locale.ROOT), mf.getContainerFormat());
        }

        // audio codec+channels
        for (MediaFileAudioStream audio : mf.getAudioStreams()) {
          if (StringUtils.isNotBlank(audio.getCodec())) {
            audioCodecs.add(audio.getCodec());
          }
          audioChannels.add(audio.getAudioChannels());
        }

        if (first) {
          // audio stream count
          audioCount = mf.getAudioStreams().size();

          // audio languages
          audioLanguages.addAll(mf.getAudioLanguagesList());

          // audio titles
          audioTitles.addAll(mf.getAudioTitleList());

          // subtitles stream count
          subtitleCount = mf.getSubtitles().size();

          // HDR Format
          if (!mf.getHdrFormat().isEmpty()) {
            String[] hdrs = mf.getHdrFormat().split(", ");
            for (String hdr : hdrs) {
              hdrFormat.add(hdr);
            }
          }
        }

        first = false;
      }

      // get subtitle language/format from video files and subtitle files
      for (MediaFile mf : episode.getMediaFiles(MediaFileType.VIDEO, MediaFileType.SUBTITLE)) {
        // subtitle language
        if (!mf.getSubtitleLanguagesList().isEmpty()) {
          subtitleLanguages.addAll(mf.getSubtitleLanguagesList());
        }
        // subtitle formats
        for (MediaFileSubtitle subtitle : mf.getSubtitles()) {
          subtitleCount++;
          subtitleFormats.add(subtitle.getCodec());
        }
      }

      // get audio data also from audio files
      for (MediaFile mf : episode.getMediaFiles(MediaFileType.AUDIO)) {
        audioCount++;
        audioLanguages.addAll(mf.getAudioLanguagesList());
        audioTitles.addAll(mf.getAudioTitleList());
      }

      audioStreamCount.add(audioCount);
      subtitleStreamCount.add(subtitleCount);
    }

    // video codecs
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(videoCodecsInEpisodes, videoCodecs)) {
      firePropertyChange(Constants.VIDEO_CODEC, null, videoCodecsInEpisodes);

    }

    // frame rate
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(frameRatesInEpisodes, frameRates)) {
      firePropertyChange(Constants.FRAME_RATE, null, frameRatesInEpisodes);
    }

    // video container
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(videoContainersInEpisodes, videoContainers.values())) {
      firePropertyChange(Constants.VIDEO_CONTAINER, null, videoContainersInEpisodes);
    }

    // audio codec
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(audioCodecsInEpisodes, audioCodecs)) {
      firePropertyChange(Constants.AUDIO_CODEC, null, audioCodecsInEpisodes);
    }

    // audio channels
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(audioChannelsInEpisodes, audioChannels)) {
      firePropertyChange(Constants.AUDIO_CHANNEL, null, audioChannelsInEpisodes);
    }

    // audio streams
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(audioStreamsInEpisodes, audioStreamCount)) {
      firePropertyChange(Constants.AUDIOSTREAMS_COUNT, null, audioStreamsInEpisodes);
    }

    // audio languages
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(audioLanguagesInEpisodes, audioLanguages)) {
      firePropertyChange(Constants.SUBTITLE_LANGUAGES, null, audioLanguagesInEpisodes);
    }

    // subtitles
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(subtitlesInEpisodes, subtitleStreamCount)) {
      firePropertyChange(Constants.SUBTITLES_COUNT, null, subtitlesInEpisodes);
    }

    // subtitle languages
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(subtitleLanguagesInEpisodes, subtitleLanguages)) {
      firePropertyChange(Constants.SUBTITLE_LANGUAGES, null, subtitleLanguagesInEpisodes);
    }

    // subtitle formats
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(subtitleFormatsInEpisodes, subtitleFormats)) {
      firePropertyChange(Constants.SUBTITLE_FORMATS, null, subtitleFormatsInEpisodes);
    }

    // HDR Format
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(hdrFormatInEpisodes, hdrFormat)) {
      firePropertyChange(Constants.HDR_FORMAT, null, hdrFormatInEpisodes);
    }

    // audio titles
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(audioTitlesInEpisodes, audioTitles)) {
      firePropertyChange(Constants.AUDIO_TITLE, null, audioTitlesInEpisodes);
    }
  }

  public Collection<String> getVideoCodecsInEpisodes() {
    return Collections.unmodifiableList(videoCodecsInEpisodes);
  }

  public Collection<String> getVideoContainersInEpisodes() {
    return Collections.unmodifiableList(videoContainersInEpisodes);
  }

  public Collection<Double> getFrameRatesInEpisodes() {
    return Collections.unmodifiableList(frameRatesInEpisodes);
  }

  public Collection<String> getAudioCodecsInEpisodes() {
    return Collections.unmodifiableList(audioCodecsInEpisodes);
  }

  public Collection<Integer> getAudioChannelsInEpisodes() {
    return Collections.unmodifiableList(audioChannelsInEpisodes);
  }

  public Collection<MediaCertification> getCertification() {
    return Collections.unmodifiableList(certificationsInTvShows);
  }

  public Collection<Integer> getAudioStreamsInEpisodes() {
    return Collections.unmodifiableList(audioStreamsInEpisodes);
  }

  public Collection<Integer> getSubtitlesInEpisodes() {
    return Collections.unmodifiableList(subtitlesInEpisodes);
  }

  public Collection<String> getAudioLanguagesInEpisodes() {
    return Collections.unmodifiableList(audioLanguagesInEpisodes);
  }

  public Collection<String> getSubtitleLanguagesInEpisodes() {
    return Collections.unmodifiableList(subtitleLanguagesInEpisodes);
  }

  public Collection<String> getSubtitleFormatsInEpisodes() {
    return Collections.unmodifiableList(subtitleFormatsInEpisodes);
  }

  public Collection<String> getHdrFormatInEpisodes() {
    return Collections.unmodifiableList(hdrFormatInEpisodes);
  }

  public Collection<String> getAudioTitlesInEpisodes() {
    return Collections.unmodifiableList(audioTitlesInEpisodes);
  }

  /**
   * Gets the TV show by path.
   * 
   * @param path
   *          path
   * @return the TV show by path
   */
  public TvShow getTvShowByPath(Path path) {
    // iterate over all tv shows and check whether this path is being owned by one
    for (TvShow tvShow : this.tvShows) {
      if (tvShow.getPathNIO().compareTo(path.toAbsolutePath()) == 0) {
        return tvShow;
      }
    }

    return null;
  }

  /**
   * Gets the episodes by file. Filter out all episodes from the Database which are part of this file
   * 
   * @param file
   *          the file
   * @return the tv episodes by file
   */
  public static List<TvShowEpisode> getTvEpisodesByFile(TvShow tvShow, Path file) {
    List<TvShowEpisode> episodes = new ArrayList<>(1);
    // validy check
    if (file == null) {
      return episodes;
    }

    // check if that file is in this tv show/episode (iterating thread safe)
    for (TvShowEpisode episode : new ArrayList<>(tvShow.getEpisodes())) {
      for (MediaFile mediaFile : new ArrayList<>(episode.getMediaFiles())) {
        if (file.equals(mediaFile.getFile())) {
          episodes.add(episode);
        }
      }
    }

    return episodes;
  }

  /**
   * invalidate the title sortable upon changes to the sortable prefixes
   */
  public void invalidateTitleSortable() {
    tvShows.stream().parallel().forEach(tvShow -> {
      tvShow.clearTitleSortable();
      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        episode.clearTitleSortable();
      }
    });
  }

  /**
   * Gets the new TvShows or TvShows with new episodes
   * 
   * @return the new TvShows
   */
  public List<TvShow> getNewTvShows() {
    List<TvShow> newShows = new ArrayList<>();
    for (TvShow show : tvShows) {
      if (show.isNewlyAdded()) {
        newShows.add(show);
      }
    }
    return newShows;
  }

  /**
   * Gets the new episodes
   * 
   * @return the new episodes
   */
  public List<TvShowEpisode> getNewEpisodes() {
    List<TvShowEpisode> newEp = new ArrayList<>();
    for (TvShow show : tvShows) {
      for (TvShowEpisode ep : show.getEpisodes()) {
        if (ep.isNewlyAdded()) {
          newEp.add(ep);
        }
      }
    }
    return newEp;
  }

  /**
   * Gets the unscraped episodes
   * 
   * @return the unscraped episodes
   */
  public List<TvShowEpisode> getUnscrapedEpisodes() {
    List<TvShowEpisode> newEp = new ArrayList<>();
    for (TvShow show : tvShows) {
      for (TvShowEpisode ep : show.getEpisodes()) {
        if (!ep.isScraped()) {
          newEp.add(ep);
        }
      }
    }
    return newEp;
  }

  /**
   * check if there are episodes without (at least) one VIDEO mf
   */
  private void checkAndCleanupMediaFiles() {
    boolean problemsDetected = false;
    for (TvShow tvShow : tvShows) {
      for (TvShowEpisode episode : new ArrayList<>(tvShow.getEpisodes())) {
        List<MediaFile> mfs = episode.getMediaFiles(MediaFileType.VIDEO);
        if (mfs.isEmpty()) {
          tvShow.removeEpisode(episode);
          problemsDetected = true;
        }
      }
    }

    if (problemsDetected) {
      LOGGER.warn("episodes without VIDEOs detected");

      // and push a message
      // also delay it so that the UI has time to start up
      Thread thread = new Thread(() -> {
        try {
          Thread.sleep(15000);
        }
        catch (Exception ignored) {
          // ignored
        }
        Message message = new Message(MessageLevel.SEVERE, "tmm.tvshows", "message.database.corrupteddata");
        MessageManager.instance.pushMessage(message);
      });
      thread.start();
    }
  }

  /**
   * all available subtitle scrapers.
   *
   * @return the subtitle scrapers
   */
  public List<MediaScraper> getAvailableSubtitleScrapers() {
    List<MediaScraper> availableScrapers = MediaScraper.getMediaScrapers(ScraperType.TVSHOW_SUBTITLE);
    availableScrapers.sort(new TvShowMediaScraperComparator());
    return availableScrapers;
  }

  /**
   * get all default (specified via settings) subtitle scrapers
   *
   * @return the specified subtitle scrapers
   */
  public List<MediaScraper> getDefaultSubtitleScrapers() {
    List<MediaScraper> defaultScrapers = getSubtitleScrapers(TvShowModuleManager.getInstance().getSettings().getSubtitleScrapers());
    return defaultScrapers.stream().filter(MediaScraper::isActive).toList();
  }

  /**
   * get all default (specified via settings) trailer scrapers
   *
   * @return the specified trailer scrapers
   */
  public List<MediaScraper> getDefaultTrailerScrapers() {
    List<MediaScraper> defaultScrapers = getTrailerScrapers(TvShowModuleManager.getInstance().getSettings().getTrailerScrapers());
    return defaultScrapers.stream().filter(MediaScraper::isActive).toList();
  }

  /**
   * get all specified subtitle scrapers.
   *
   * @param providerIds
   *          the scrapers
   * @return the subtitle scrapers
   */
  public List<MediaScraper> getSubtitleScrapers(List<String> providerIds) {
    List<MediaScraper> subtitleScrapers = new ArrayList<>();

    for (String providerId : providerIds) {
      if (StringUtils.isBlank(providerId)) {
        continue;
      }
      MediaScraper subtitleScraper = MediaScraper.getMediaScraperById(providerId, ScraperType.TVSHOW_SUBTITLE);
      if (subtitleScraper != null) {
        subtitleScrapers.add(subtitleScraper);
      }
    }

    return subtitleScrapers;
  }

  /**
   * search all episodes of all TV shows for duplicates (duplicate S/E)
   */
  public void searchDuplicateEpisodes() {
    Map<String, TvShow> showMap = new HashMap<>();
    for (TvShow tvShow : getTvShows()) {
      Map<String, Object> ids = tvShow.getIds();
      for (var entry : ids.entrySet()) {
        // ignore collection "IDs"
        if (MediaMetadata.TMDB_SET.equalsIgnoreCase(entry.getKey()) || "tmdbcol".equalsIgnoreCase(entry.getKey())) {
          continue;
        }
        String id = entry.getKey() + entry.getValue();

        if (showMap.containsKey(id)) {
          // yes - set duplicate flag on both tvShows
          tvShow.setDuplicate();
          TvShow show2 = showMap.get(id);
          show2.setDuplicate();
        }
        else {
          // no, store show
          showMap.put(id, tvShow);
        }
      }

      // check for every episode
      Map<String, TvShowEpisode> episodeMap = new HashMap<>();
      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        if (episode.getSeason() == -1 || episode.getEpisode() == -1) {
          continue;
        }
        String se = "S" + episode.getSeason() + "E" + episode.getEpisode();

        TvShowEpisode duplicate = episodeMap.get(se);
        if (duplicate != null) {
          duplicate.setDuplicate();
          episode.setDuplicate();
        }
        else {
          episodeMap.put(se, episode);
        }
      }
    }
  }

  public List<TvShowScraperMetadataConfig> detectMissingMetadata(TvShow tvShow) {
    return detectMissingFields(tvShow, TvShowModuleManager.getInstance().getSettings().getTvShowCheckMetadata());
  }

  public List<TvShowScraperMetadataConfig> detectMissingArtwork(TvShow tvShow) {
    return detectMissingFields(tvShow, TvShowModuleManager.getInstance().getSettings().getTvShowCheckArtwork());
  }

  public List<TvShowScraperMetadataConfig> detectMissingFields(TvShow tvshow, List<TvShowScraperMetadataConfig> toCheck) {
    List<TvShowScraperMetadataConfig> missingMetadata = new ArrayList<>();

    for (TvShowScraperMetadataConfig metadataConfig : toCheck) {
      Object value = tvshow.getValueForMetadata(metadataConfig);
      if (value == null) {
        missingMetadata.add(metadataConfig);
      }
      else if (value instanceof String string && StringUtils.isBlank(string)) {
        missingMetadata.add(metadataConfig);
      }
      else if (value instanceof Number number && number.intValue() <= 0) {
        missingMetadata.add(metadataConfig);
      }
      else if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
        missingMetadata.add(metadataConfig);
      }
      else if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) {
        missingMetadata.add(metadataConfig);
      }
      else if (value == MediaCertification.UNKNOWN) {
        missingMetadata.add(metadataConfig);
      }
    }

    return missingMetadata;
  }

  public List<TvShowScraperMetadataConfig> detectMissingMetadata(TvShowSeason season) {
    if (season.isDummy()) {
      return Collections.emptyList();
    }

    List<TvShowScraperMetadataConfig> seasonValues = new ArrayList<>();
    for (TvShowScraperMetadataConfig config : TvShowModuleManager.getInstance().getSettings().getTvShowCheckMetadata()) {
      if (config.isMetaData() && config.name().startsWith("SEASON")) {
        seasonValues.add(config);
      }
    }

    return detectMissingFields(season, seasonValues);
  }

  public List<TvShowScraperMetadataConfig> detectMissingArtwork(TvShowSeason season) {
    return detectMissingFields(season, TvShowModuleManager.getInstance().getSettings().getSeasonCheckArtwork());
  }

  public List<TvShowScraperMetadataConfig> detectMissingFields(TvShowSeason season, List<TvShowScraperMetadataConfig> toCheck) {
    List<TvShowScraperMetadataConfig> missingMetadata = new ArrayList<>();

    for (TvShowScraperMetadataConfig metadataConfig : toCheck) {
      Object value = season.getValueForMetadata(metadataConfig);
      if (value == null) {
        missingMetadata.add(metadataConfig);
      }
      else if (value instanceof String string && StringUtils.isBlank(string)) {
        missingMetadata.add(metadataConfig);
      }
      else if (value instanceof Number number && number.intValue() <= 0) {
        missingMetadata.add(metadataConfig);
      }
      else if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
        missingMetadata.add(metadataConfig);
      }
      else if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) {
        missingMetadata.add(metadataConfig);
      }
      else if (value == MediaCertification.UNKNOWN) {
        missingMetadata.add(metadataConfig);
      }
    }

    return missingMetadata;
  }

  public List<TvShowEpisodeScraperMetadataConfig> detectMissingMetadata(TvShowEpisode episode) {
    if (episode.isDummy() || (episode.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isEpisodeSpecialsCheckMissingMetadata())) {
      return Collections.emptyList();
    }
    return detectMissingFields(episode, TvShowModuleManager.getInstance().getSettings().getEpisodeCheckMetadata());
  }

  public List<TvShowEpisodeScraperMetadataConfig> detectMissingArtwork(TvShowEpisode episode) {
    if (episode.isDummy() || (episode.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isEpisodeSpecialsCheckMissingArtwork())) {
      return Collections.emptyList();
    }
    return detectMissingFields(episode, TvShowModuleManager.getInstance().getSettings().getEpisodeCheckArtwork());
  }

  public List<TvShowEpisodeScraperMetadataConfig> detectMissingFields(TvShowEpisode episode, List<TvShowEpisodeScraperMetadataConfig> toCheck) {
    List<TvShowEpisodeScraperMetadataConfig> missingMetadata = new ArrayList<>();

    for (TvShowEpisodeScraperMetadataConfig metadataConfig : toCheck) {
      Object value = episode.getValueForMetadata(metadataConfig);
      if (value == null) {
        missingMetadata.add(metadataConfig);
      }
      else if (value instanceof String string && StringUtils.isBlank(string)) {
        missingMetadata.add(metadataConfig);
      }
      else if (value instanceof Number number && number.intValue() <= 0) {
        missingMetadata.add(metadataConfig);
      }
      else if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
        missingMetadata.add(metadataConfig);
      }
      else if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) {
        missingMetadata.add(metadataConfig);
      }
      else if (value == MediaCertification.UNKNOWN) {
        missingMetadata.add(metadataConfig);
      }
    }

    return missingMetadata;
  }

  private static class TvShowMediaScraperComparator implements Comparator<MediaScraper> {
    @Override
    public int compare(MediaScraper o1, MediaScraper o2) {
      if (o1.getPriority() == o2.getPriority()) {
        return o1.getId().compareTo(o2.getId()); // samne prio? alphabetical
      }
      else {
        return Integer.compare(o2.getPriority(), o1.getPriority()); // highest first
      }
    }
  }
}
