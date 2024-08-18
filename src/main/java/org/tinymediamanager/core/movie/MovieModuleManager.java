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
package org.tinymediamanager.core.movie;

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
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.movie.http.MovieCommandHandler;
import org.tinymediamanager.scraper.util.MetadataUtil;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;

/**
 * The class MovieModuleManager. Used to manage the movies module
 * 
 * @author Manuel Laggner
 */
public final class MovieModuleManager implements ITmmModule {

  private static final String          MODULE_TITLE         = "Movie management";
  private static final String          MOVIE_DB             = "movies.db";
  private static final Logger          LOGGER               = LoggerFactory.getLogger(MovieModuleManager.class);
  private static final int             COMMIT_DELAY         = 2000;

  private static final String          METADATA_VERSION     = "VERSION";

  private static MovieModuleManager    instance;

  private final List<String>           startupMessages;
  private final Map<MediaEntity, Long> pendingChanges;
  private final ReentrantReadWriteLock lock;

  private boolean                      enabled;
  private int                          autoCommitBufferSize = 8192;
  private MVStore                      mvStore;
  private ObjectWriter                 movieObjectWriter;
  private ObjectReader                 movieObjectReader;
  private ObjectWriter                 movieSetObjectWriter;
  private ObjectReader                 movieSetObjectReader;

  private MVMap<UUID, String>          movieMap;
  private MVMap<UUID, String>          movieSetMap;
  private MVMap<String, String>        metadataMap;

  private Timer                        databaseTimer;

  private MovieModuleManager() {
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

  public static MovieModuleManager getInstance() {
    if (instance == null) {
      instance = new MovieModuleManager();
    }
    return instance;
  }

  /**
   * removes the active instance <br>
   * <b>Should only be used for unit testing et all!</b><br>
   */
  static void clearInstances() {
    instance = null;
    MovieSettings.clearInstance();
    MovieList.clearInstance();
  }

  public MovieSettings getSettings() {
    return MovieSettings.getInstance();
  }

  public MovieList getMovieList() {
    return MovieList.getInstance();
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

    movieObjectWriter = objectMapper.writerFor(Movie.class);
    movieObjectReader = objectMapper.readerFor(Movie.class);
    movieSetObjectWriter = objectMapper.writerFor(MovieSet.class);
    movieSetObjectReader = objectMapper.readerFor(MovieSet.class);

    // open database
    openDatabaseAndLoadMovies();
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
      TmmHttpServer.getInstance().createContext("movie", new MovieCommandHandler());
    }
    catch (Exception e) {
      LOGGER.warn("could not register movie API - '{}'", e.getMessage());
    }
  }

  /**
   * open the database<BR/>
   * 1. try to open the actual one<BR/>
   * 2. try to open from backups<BR/>
   * 3. open a new one
   */
  private void openDatabaseAndLoadMovies() {
    Path databaseFile = Paths.get(Settings.getInstance().getSettingsFolder(), MOVIE_DB);
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
      Utils.deleteFileSafely(Paths.get(Globals.BACKUP_FOLDER, MOVIE_DB + ".corrupted"));
      Utils.moveFileSafe(databaseFile, Paths.get(Globals.BACKUP_FOLDER, MOVIE_DB + ".corrupted"));
    }
    catch (Exception e) {
      LOGGER.error("Could not move corrupted database to '{}' - '{}", MOVIE_DB + ".corrupted", e.getMessage());
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
        Utils.unzipFile(backup, Paths.get("/", "data", MOVIE_DB), databaseFile);
        loadDatabase(databaseFile);
        startupMessages.add(TmmResourceBundle.getString("movie.loaddb.failed.restore"));

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
      startupMessages.add(TmmResourceBundle.getString("movie.loaddb.failed"));
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
            Utils.deleteFileSafely(Paths.get(Globals.BACKUP_FOLDER, MOVIE_DB + ".corrupted"));
            Utils.moveFileSafe(databaseFile, Paths.get(Globals.BACKUP_FOLDER, MOVIE_DB + ".corrupted"));
          }
          catch (Exception e1) {
            LOGGER.error("Could not move corrupted database to '{}' - '{}", MOVIE_DB + ".corrupted", e1.getMessage());
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

          movieMap = mvStore.openMap("movies");
          movieSetMap = mvStore.openMap("movieSets");
          metadataMap = mvStore.openMap("metadata");

          for (Movie movie : getMovieList().getMovies()) {
            persistMovie(movie);
          }

          for (MovieSet movieSet : getMovieList().getMovieSetList()) {
            persistMovieSet(movieSet);
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

    movieMap = mvStore.openMap("movies");
    movieSetMap = mvStore.openMap("movieSets");
    metadataMap = mvStore.openMap("metadata");

    getMovieList().loadMoviesFromDatabase(movieMap);
    getMovieList().loadMovieSetsFromDatabase(movieSetMap);
    getMovieList().initDataAfterLoading();
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
      for (String ds : getSettings().getMovieDataSource()) {
        Path file = Paths.get(ds, Constants.DS_TRASH_FOLDER);
        Utils.deleteDirectoryRecursive(file);
      }
    }
  }

  private void writePendingChanges() {
    writePendingChanges(false);
  }

  private synchronized void writePendingChanges(boolean force) {
    if (mvStore == null || mvStore.isClosed()) {
      return;
    }

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
          if (entry.getValue() instanceof Movie movie) {
            // only diffs
            String oldValue = movieMap.get(movie.getDbId());
            String newValue = movieObjectWriter.writeValueAsString(movie);
            if (!StringUtils.equals(oldValue, newValue)) {
              movieMap.put(movie.getDbId(), newValue);
            }
          }
          else if (entry.getValue() instanceof MovieSet movieSet) {
            // only diffs
            String oldValue = movieSetMap.get(movieSet.getDbId());
            String newValue = movieSetObjectWriter.writeValueAsString(movieSet);
            if (!StringUtils.equals(oldValue, newValue)) {
              movieSetMap.put(movieSet.getDbId(), newValue);
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
   * dumps a whole movie to logfile
   * 
   * @param movie
   *          the movie to make the dump for
   */
  public void dump(Movie movie) {
    String d = getMovieJsonFromDB(movie);
    if (!d.isEmpty()) {
      LOGGER.info("Dumping Movie: {}\n{}", movie.getDbId(), d);
    }
  }

  /**
   * dumps a whole movieset to logfile
   * 
   * @param movieSet
   *          the movieset to make the dump for
   */
  public void dump(MovieSet movieSet) {
    String d = getMovieSetJsonFromDB(movieSet);
    if (!d.isEmpty()) {
      LOGGER.info("Dumping MovieSet: {}\n{}", movieSet.getDbId(), d);
    }
  }

  /**
   * gets the JSON out of the DB from specified movie
   * 
   * @param movie
   *          the {@link Movie} to dump the data for
   * @return JSON string
   */
  public String getMovieJsonFromDB(Movie movie) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      Object json = mapper.readValue(movieMap.get(movie.getDbId()), Object.class);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }
    catch (Exception e) {
      LOGGER.error("Cannot parse JSON!", e);
    }
    return "";
  }

  /**
   * gets the JSON out of the DB from specified movieSet
   * 
   * @param movieSet
   *          the {@link MovieSet} to dump the data for
   * @return JSON string
   */
  public String getMovieSetJsonFromDB(MovieSet movieSet) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      Object json = mapper.readValue(movieSetMap.get(movieSet.getDbId()), Object.class);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }
    catch (Exception e) {
      LOGGER.error("Cannot parse JSON!", e);
    }
    return "";
  }

  void persistMovie(Movie movie) {
    // write movie to DB
    try {
      lock.writeLock().lock();
      pendingChanges.put(movie, System.currentTimeMillis());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  void removeMovieFromDb(Movie movie) {
    try {
      lock.writeLock().lock();
      pendingChanges.remove(movie);
      movieMap.remove(movie.getDbId());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  void persistMovieSet(MovieSet movieSet) {
    try {
      lock.writeLock().lock();
      pendingChanges.put(movieSet, System.currentTimeMillis());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  void removeMovieSetFromDb(MovieSet movieSet) {
    try {
      lock.writeLock().lock();
      movieSetMap.remove(movieSet.getDbId());
      pendingChanges.remove(movieSet);
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void initializeDatabase() {
    Utils.deleteFileSafely(Paths.get(Settings.getInstance().getSettingsFolder(), MOVIE_DB));
  }

  @Override
  public void saveSettings() {
    getSettings().saveSettings();
  }

  @Override
  public List<String> getStartupMessages() {
    return startupMessages;
  }

  ObjectReader getMovieObjectReader() {
    return movieObjectReader;
  }

  ObjectReader getMovieSetObjectReader() {
    return movieSetObjectReader;
  }

  public int getDbVersion() {
    return MetadataUtil.parseInt(metadataMap.get(METADATA_VERSION), 0);
  }

  public void setDbVersion(int ver) {
    metadataMap.put(METADATA_VERSION, String.valueOf(ver));
  }
}
