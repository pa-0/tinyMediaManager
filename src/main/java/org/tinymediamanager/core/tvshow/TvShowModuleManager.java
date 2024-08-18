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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.CustomNullStringSerializerProvider;
import org.tinymediamanager.core.ITmmModule;
import org.tinymediamanager.core.NullKeySerializer;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.http.TmmHttpServer;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.core.tvshow.http.TvShowCommandHandler;
import org.tinymediamanager.scraper.util.MetadataUtil;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;

/**
 * The class TvShowModuleManager. Used to manage the tv show module
 * 
 * @author Manuel Laggner
 */
public final class TvShowModuleManager implements ITmmModule {

  private static final String          MODULE_TITLE         = "TV show management";
  private static final String          TV_SHOW_DB           = "tvshows.db";
  private static final Logger          LOGGER               = LoggerFactory.getLogger(TvShowModuleManager.class);
  private static final int             COMMIT_DELAY         = 2000;

  private static final String          METADATA_VERSION     = "VERSION";

  private static TvShowModuleManager   instance;

  private final List<String>           startupMessages;
  private final Map<MediaEntity, Long> pendingChanges;
  private final ReentrantReadWriteLock lock;

  private boolean                      enabled;
  private int                          autoCommitBufferSize = 8192;
  private MVStore                      mvStore;
  private ObjectWriter                 tvShowObjectWriter;
  private ObjectReader                 tvShowObjectReader;
  private ObjectWriter                 seasonObjectWriter;
  private ObjectReader                 seasonObjectReader;
  private ObjectWriter                 episodeObjectWriter;
  private ObjectReader                 episodeObjectReader;

  private MVMap<UUID, String>          tvShowMap;
  private MVMap<UUID, String>          seasonMap;
  private MVMap<UUID, String>          episodeMap;
  private MVMap<String, String>        metadataMap;

  private Timer                        databaseTimer;

  private TvShowModuleManager() {
    enabled = false;
    startupMessages = new ArrayList<>();
    pendingChanges = new HashMap<>();
    lock = new ReentrantReadWriteLock();

    // check if a custom autocommit buffer size has been set via jvm args
    int bufferSize = Integer.getInteger("tmm.mvstore.buffersize", 8);
    if (2 <= bufferSize && bufferSize <= 64) {
      autoCommitBufferSize = 1024 * bufferSize;
    }
  }

  public static TvShowModuleManager getInstance() {
    if (instance == null) {
      instance = new TvShowModuleManager();
    }
    return instance;
  }

  /**
   * removes the active instance <br>
   * <b>Should only be used for unit testing et all!</b><br>
   */
  static void clearInstances() {
    instance = null;
    TvShowSettings.clearInstance();
    TvShowList.clearInstance();
  }

  public TvShowSettings getSettings() {
    return TvShowSettings.getInstance();
  }

  public TvShowList getTvShowList() {
    return TvShowList.getInstance();
  }

  @Override
  public String getModuleTitle() {
    return MODULE_TITLE;
  }

  @Override
  public void startUp() {
    // configure JSON
    ObjectMapper objectMapper = JsonMapper.builder()
        .configure(MapperFeature.AUTO_DETECT_GETTERS, false)
        .configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
        .configure(MapperFeature.AUTO_DETECT_SETTERS, false)
        .configure(MapperFeature.AUTO_DETECT_FIELDS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .addModule(new BlackbirdModule())
        .build();
    objectMapper.setTimeZone(TimeZone.getDefault());
    objectMapper.setSerializationInclusion(Include.NON_DEFAULT);
    objectMapper.setSerializerProvider(new CustomNullStringSerializerProvider());
    objectMapper.getSerializerProvider().setNullKeySerializer(new NullKeySerializer());

    tvShowObjectWriter = objectMapper.writerFor(TvShow.class);
    tvShowObjectReader = objectMapper.readerFor(TvShow.class);
    seasonObjectWriter = objectMapper.writerFor(TvShowSeason.class);
    seasonObjectReader = objectMapper.readerFor(TvShowSeason.class);
    episodeObjectWriter = objectMapper.writerFor(TvShowEpisode.class);
    episodeObjectReader = objectMapper.readerFor(TvShowEpisode.class);

    // open database
    openDatabaseAndLoadTvShows();
    enabled = true;

    TimerTask databaseWriteTask = new TimerTask() {
      @Override
      public void run() {
        writePendingChanges();
      }
    };
    databaseTimer = new Timer();
    databaseTimer.schedule(databaseWriteTask, COMMIT_DELAY, COMMIT_DELAY);

    try {
      TmmHttpServer.getInstance().createContext("tvshow", new TvShowCommandHandler());
    }
    catch (Exception e) {
      LOGGER.warn("could not register TV show API - '{}'", e.getMessage());
    }
  }

  /**
   * open the database<BR/>
   * 1. try to open the actual one<BR/>
   * 2. try to open from backups<BR/>
   * 3. open a new one
   */
  private void openDatabaseAndLoadTvShows() {
    Path databaseFile = Paths.get(Settings.getInstance().getSettingsFolder(), TV_SHOW_DB);
    try {
      loadDatabase(databaseFile);
      return;
    }
    catch (Exception e) {
      // look if the file is locked by another process (rethrow rather than delete the db file)
      if (e instanceof IllegalStateException && e.getMessage().contains("file is locked")) {
        throw e;
      }

      if (mvStore != null && !mvStore.isClosed()) {
        mvStore.close();
      }
      LOGGER.error("Could not open database file: {}", e.getMessage());
    }

    try {
      Utils.deleteFileSafely(Paths.get(Globals.BACKUP_FOLDER, TV_SHOW_DB + ".corrupted"));
      Utils.moveFileSafe(databaseFile, Paths.get(Globals.BACKUP_FOLDER, TV_SHOW_DB + ".corrupted"));
    }
    catch (Exception e) {
      LOGGER.error("Could not move corrupted database to '{}' - '{}", TV_SHOW_DB + ".corrupted", e.getMessage());
    }

    LOGGER.info("try to restore the database from the backups");

    // get backups
    List<Path> backups = Utils.listFiles(Paths.get(Globals.BACKUP_FOLDER));
    backups.sort(Comparator.reverseOrder());

    // load movies.db from the backup
    boolean first = true;
    for (Path backup : backups) {
      if (!backup.getFileName().toString().startsWith("data.")) {
        continue;
      }

      // but not the first one which contains the damaged database
      if (first) {
        first = false;
        continue;
      }

      try {
        Utils.unzipFile(backup, Paths.get("/", "data", TV_SHOW_DB), databaseFile);
        loadDatabase(databaseFile);
        startupMessages.add(TmmResourceBundle.getString("tvshow.loaddb.failed.restore"));

        return;
      }
      catch (Exception e) {
        if (mvStore != null && !mvStore.isClosed()) {
          mvStore.close();
        }
        LOGGER.error("Could not open database file from backup: {}", e.getMessage());
      }
    }

    LOGGER.info("starting over with an empty database file");

    try {
      Utils.deleteFileSafely(databaseFile);
      loadDatabase(databaseFile);
      startupMessages.add(TmmResourceBundle.getString("tvshow.loaddb.failed"));
    }
    catch (Exception e1) {
      LOGGER.error("could not move old database file and create a new one: {}", e1.getMessage());
    }
  }

  private void loadDatabase(Path databaseFile) {
    Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
      private int counter = 0;

      @Override
      public void uncaughtException(Thread t, Throwable e) {
        if (e instanceof IllegalStateException) {
          // wait up to 10 times, then try to recover
          if (counter < 10) {
            counter++;
            return;
          }

          LOGGER.error("database corruption detected - try to recover");

          // try to in-memory fix the DB
          mvStore.close();

          try {
            Utils.deleteFileSafely(Paths.get(Globals.BACKUP_FOLDER, TV_SHOW_DB + ".corrupted"));
            Utils.moveFileSafe(databaseFile, Paths.get(Globals.BACKUP_FOLDER, TV_SHOW_DB + ".corrupted"));
          }
          catch (Exception e1) {
            LOGGER.error("Could not move corrupted database to '{}' - '{}", TV_SHOW_DB + ".corrupted", e1.getMessage());
          }

          mvStore = new MVStore.Builder().fileName(databaseFile.toString())
              .compressHigh()
              .autoCommitBufferSize(autoCommitBufferSize)
              .backgroundExceptionHandler(this)
              .open();
          mvStore.setAutoCommitDelay(2000); // 2 sec
          mvStore.setRetentionTime(0);
          mvStore.setReuseSpace(true);
          mvStore.setCacheSize(8);

          tvShowMap = mvStore.openMap("tvshows");
          seasonMap = mvStore.openMap("seasons");
          episodeMap = mvStore.openMap("episodes");
          metadataMap = mvStore.openMap("metadata");

          for (TvShow tvShow : getTvShowList().getTvShows()) {
            persistTvShow(tvShow);

            for (TvShowSeason season : tvShow.getSeasons()) {
              persistSeason(season);
            }

            for (TvShowEpisode episode : tvShow.getEpisodes()) {
              persistEpisode(episode);
            }
          }

          counter = 0;
        }
      }
    };

    mvStore = new MVStore.Builder().fileName(databaseFile.toString())
        .compressHigh()
        .autoCommitBufferSize(autoCommitBufferSize)
        .backgroundExceptionHandler(exceptionHandler)
        .open();
    mvStore.setAutoCommitDelay(2000); // 2 sec
    mvStore.setRetentionTime(0);
    mvStore.setReuseSpace(true);
    mvStore.setCacheSize(8);

    tvShowMap = mvStore.openMap("tvshows");
    seasonMap = mvStore.openMap("seasons");
    episodeMap = mvStore.openMap("episodes");
    metadataMap = mvStore.openMap("metadata");

    getTvShowList().loadTvShowsFromDatabase(tvShowMap, seasonMap, episodeMap);
    getTvShowList().initDataAfterLoading();
  }

  @Override
  public synchronized void shutDown() throws Exception {
    if (!isEnabled()) {
      return;
    }

    enabled = false;

    databaseTimer.cancel();

    // write pending changes
    if (mvStore != null && !mvStore.isClosed()) {
      writePendingChanges(true);
      mvStore.commit();

      mvStore.compactMoveChunks();
      mvStore.close();
    }

    if (Settings.getInstance().isDeleteTrashOnExit()) {
      for (String ds : getSettings().getTvShowDataSource()) {
        Path file = Paths.get(ds, Constants.DS_TRASH_FOLDER);
        Utils.deleteDirectoryRecursive(file);
      }
    }
  }

  private void writePendingChanges() {
    writePendingChanges(false);
  }

  private synchronized void writePendingChanges(boolean force) {
    if (force) {
      // force write - wait until the lock is released
      lock.writeLock().lock();
    }
    else {
      // lock if there is no other task running
      if (!lock.writeLock().tryLock()) {
        return;
      }
    }

    try {
      long now = System.currentTimeMillis();

      // filter on commit delay, and put into ordered TreeMap
      Map<UUID, MediaEntity> mapToSave = new TreeMap<>();
      for (Map.Entry<MediaEntity, Long> entry : pendingChanges.entrySet()) {
        if (force || entry.getValue() < (now - COMMIT_DELAY)) {
          mapToSave.put(entry.getKey().getDbId(), entry.getKey());
        }
      }

      for (Map.Entry<UUID, MediaEntity> entry : mapToSave.entrySet()) {
        try {
          if (entry.getValue() instanceof TvShow tvShow) {
            // only diffs
            String oldValue = tvShowMap.get(tvShow.getDbId());
            String newValue = tvShowObjectWriter.writeValueAsString(tvShow);
            if (!StringUtils.equals(oldValue, newValue)) {
              tvShowMap.put(tvShow.getDbId(), newValue);
            }
          }
          else if (entry.getValue() instanceof TvShowSeason season) {
            // only diffs
            String oldValue = seasonMap.get(season.getDbId());
            String newValue = seasonObjectWriter.writeValueAsString(season);
            if (!StringUtils.equals(oldValue, newValue)) {
              seasonMap.put(season.getDbId(), newValue);
            }
          }
          else if (entry.getValue() instanceof TvShowEpisode episode) {
            // only diffs
            String oldValue = episodeMap.get(episode.getDbId());
            String newValue = episodeObjectWriter.writeValueAsString(episode);
            if (!StringUtils.equals(oldValue, newValue)) {
              episodeMap.put(episode.getDbId(), newValue);
            }
          }
        }
        catch (Exception e) {
          LOGGER.warn("could not store '{}' - '{}'", entry.getKey().getClass().getName(), e.getMessage());
        }
        finally {
          pendingChanges.remove(entry.getValue());
        }
      }
    }
    finally {
      mvStore.commit();
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * gets the JSON out of the DB from specified tvShow
   * 
   * @param tvShow
   *          the {@link TvShow} to dump
   * @param withChilds
   *          should we also dump all children?
   * @return JSON string
   */
  public String getTvShowJsonFromDB(TvShow tvShow, boolean withChilds) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      ObjectNode showNode = mapper.readValue(tvShowMap.get(tvShow.getDbId()), ObjectNode.class);

      if (withChilds) {
        ArrayNode seasons = JsonNodeFactory.instance.arrayNode();
        for (TvShowSeason se : tvShow.getSeasons()) {
          ObjectNode seasonNode = mapper.readValue(seasonMap.get(se.getDbId()), ObjectNode.class);

          ArrayNode episodes = JsonNodeFactory.instance.arrayNode();
          for (TvShowEpisode ep : se.getEpisodes()) {
            ObjectNode epNode = mapper.readValue(episodeMap.get(ep.getDbId()), ObjectNode.class);
            episodes.add(epNode);
          }

          seasonNode.set("episodes", episodes);
          seasons.add(seasonNode);
        }
        showNode.set("seasons", seasons);
      }
      String s = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(showNode);
      return s;
    }
    catch (Exception e) {
      LOGGER.error("Cannot parse JSON!", e);
    }
    return "";
  }

  /**
   * dumps a whole TV show to logfile
   * 
   * @param tvShow
   *          the TV show to dump the data for
   */
  public void dump(TvShow tvShow, boolean withChilds) {
    String d = getTvShowJsonFromDB(tvShow, withChilds);
    if (!d.isEmpty()) {
      LOGGER.info("Dumping TvShow: {}\n{}", tvShow.getDbId(), d);
    }
  }

  /**
   * dumps a whole season to logfile
   * 
   * @param season
   *          the TV show season to dump the data for
   */
  public void dump(TvShowSeason season, boolean withChilds) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      ObjectNode seasonNode = mapper.readValue(seasonMap.get(season.getDbId()), ObjectNode.class);
      if (withChilds) {
        ArrayNode episodes = JsonNodeFactory.instance.arrayNode();
        for (TvShowEpisode ep : season.getEpisodes()) {
          ObjectNode epNode = mapper.readValue(episodeMap.get(ep.getDbId()), ObjectNode.class);
          episodes.add(epNode);
        }
        seasonNode.set("episodes", episodes);
      }
      String s = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(seasonNode);
      LOGGER.info("Dumping TvShowSeason: {}\n{}", season.getDbId(), s);
    }
    catch (Exception e) {
      LOGGER.error("Cannot parse JSON!", e);
    }
  }

  /**
   * dumps a single episode to logfile
   * 
   * @param episode
   *          the TV show episode to dump the data for
   */
  public void dump(TvShowEpisode episode) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      if (episodeMap.get(episode.getDbId()) != null) {
        ObjectNode epNode = mapper.readValue(episodeMap.get(episode.getDbId()), ObjectNode.class);
        String s = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(epNode);
        LOGGER.info("Dumping TvShowEpisode: {}\n{}", episode.getDbId(), s);
      }
      else {
        // dummy?
        LOGGER.warn("Cannot dump DummyEpisode - check them on show level!");
      }
    }
    catch (Exception e) {
      LOGGER.error("Cannot parse JSON!", e);
    }
  }

  void persistTvShow(TvShow tvShow) {
    // write movie to DB
    try {
      lock.writeLock().lock();
      pendingChanges.put(tvShow, System.currentTimeMillis());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  void removeTvShowFromDb(TvShow tvShow) {
    try {
      lock.writeLock().lock();
      pendingChanges.remove(tvShow);
      tvShowMap.remove(tvShow.getDbId());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  void persistSeason(TvShowSeason season) {
    try {
      lock.writeLock().lock();
      pendingChanges.put(season, System.currentTimeMillis());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  void removeSeasonFromDb(TvShowSeason season) {
    try {
      lock.writeLock().lock();
      pendingChanges.remove(season);
      seasonMap.remove(season.getDbId());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  void persistEpisode(TvShowEpisode episode) {
    try {
      lock.writeLock().lock();
      pendingChanges.put(episode, System.currentTimeMillis());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  void removeEpisodeFromDb(TvShowEpisode episode) {
    try {
      lock.writeLock().lock();
      pendingChanges.remove(episode);
      episodeMap.remove(episode.getDbId());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void initializeDatabase() {
    Utils.deleteFileSafely(Paths.get(Settings.getInstance().getSettingsFolder(), TV_SHOW_DB));
  }

  @Override
  public void saveSettings() {
    getSettings().saveSettings();
  }

  @Override
  public List<String> getStartupMessages() {
    return startupMessages;
  }

  public ObjectReader getTvShowObjectReader() {
    return tvShowObjectReader;
  }

  public ObjectReader getEpisodeObjectReader() {
    return episodeObjectReader;
  }

  public ObjectReader getSeasonObjectReader() {
    return seasonObjectReader;
  }

  public int getDbVersion() {
    return MetadataUtil.parseInt(metadataMap.get(METADATA_VERSION), 0);
  }

  public void setDbVersion(int ver) {
    metadataMap.put(METADATA_VERSION, String.valueOf(ver));
  }

}
