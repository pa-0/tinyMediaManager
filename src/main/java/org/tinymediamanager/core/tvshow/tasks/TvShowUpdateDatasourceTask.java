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
package org.tinymediamanager.core.tvshow.tasks;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static org.tinymediamanager.core.Utils.DISC_FOLDER_REGEX;
import static org.tinymediamanager.core.Utils.SEASON_NFO_PATTERN;
import static org.tinymediamanager.core.Utils.containsSkipFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.AbstractFileVisitor;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.tasks.MediaFileInformationFetcherTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.TvShowArtworkHelper;
import org.tinymediamanager.core.tvshow.TvShowEpisodeAndSeasonParser;
import org.tinymediamanager.core.tvshow.TvShowEpisodeAndSeasonParser.EpisodeMatchingResult;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.connector.TvShowEpisodeNfoParser;
import org.tinymediamanager.core.tvshow.connector.TvShowEpisodeNfoParser.Episode;
import org.tinymediamanager.core.tvshow.connector.TvShowNfoParser;
import org.tinymediamanager.core.tvshow.connector.TvShowSeasonNfoParser;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.ParserUtils;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.thirdparty.VSMeta;
import org.tinymediamanager.thirdparty.trakttv.TvShowSyncTraktTvTask;

/**
 * The Class TvShowUpdateDataSourcesTask.
 * 
 * @author Manuel Laggner
 */

public class TvShowUpdateDatasourceTask extends TmmThreadPool {
  private static final Logger          LOGGER        = LoggerFactory.getLogger(TvShowUpdateDatasourceTask.class);

  // skip well-known, but unneeded folders (UPPERCASE)
  private static final List<String>    SKIP_FOLDERS  = Arrays.asList(".", "..", "CERTIFICATE", "$RECYCLE.BIN", "RECYCLER",
      "SYSTEM VOLUME INFORMATION", "@EADIR", "ADV_OBJ", "EXTRATHUMB", "PLEX VERSIONS");

  // skip folders starting with a SINGLE "." or "._"
  private static final String          SKIP_REGEX    = "^[.][\\w@]+.*";

  private static long                  preDir        = 0;
  private static long                  postDir       = 0;
  private static long                  visFile       = 0;

  private final List<String>           dataSources   = new ArrayList<>();
  private final List<Pattern>          skipFolders   = new ArrayList<>();
  private final List<TvShow>           showsToUpdate = new ArrayList<>();
  private final TvShowList             tvShowList;
  private final Set<Path>              filesFound    = new HashSet<>();
  private final ReentrantReadWriteLock fileLock      = new ReentrantReadWriteLock();

  /**
   * Instantiates a new scrape task - to update all datasources
   * 
   */
  public TvShowUpdateDatasourceTask() {
    super(TmmResourceBundle.getString("update.datasource"));
    this.tvShowList = TvShowModuleManager.getInstance().getTvShowList();
    this.dataSources.addAll(TvShowModuleManager.getInstance().getSettings().getTvShowDataSource());

    init();
  }

  /**
   * Instantiates a new scrape task - to update a single datasource
   * 
   * @param datasource
   *          the data source to start the task for
   */
  public TvShowUpdateDatasourceTask(String datasource) {
    super(TmmResourceBundle.getString("update.datasource") + " (" + datasource + ")");
    this.tvShowList = TvShowModuleManager.getInstance().getTvShowList();
    this.dataSources.add(datasource);

    init();
  }

  /**
   * Instantiates a new scrape task - to update a single datasource
   *
   * @param datasources
   *          the data sources to start the task for
   */
  public TvShowUpdateDatasourceTask(Collection<String> datasources) {
    this(datasources, Collections.emptyList());
  }

  /**
   * Instantiates a new scrape task - to update given tv shows
   * 
   * @param tvShowFolders
   *          a list of TV show folders to start the task for
   */
  public TvShowUpdateDatasourceTask(List<TvShow> tvShowFolders) {
    this(Collections.emptyList(), tvShowFolders);
  }

  private TvShowUpdateDatasourceTask(Collection<String> dataSources, List<TvShow> tvShowFolders) {
    super(TmmResourceBundle.getString("update.datasource"));
    this.tvShowList = TvShowModuleManager.getInstance().getTvShowList();
    this.dataSources.addAll(dataSources);
    this.showsToUpdate.addAll(tvShowFolders);

    init();
  }

  private void init() {
    for (String skipFolder : TvShowModuleManager.getInstance().getSettings().getSkipFolder()) {
      try {
        Pattern pattern = Pattern.compile(skipFolder);
        skipFolders.add(pattern);
      }
      catch (Exception e) {
        try {
          LOGGER.debug("no valid skip pattern - '{}'", skipFolder);

          Pattern pattern = Pattern.compile(Pattern.quote(skipFolder));
          skipFolders.add(pattern);
        }
        catch (Exception ignored) {
          // just ignore
        }
      }
    }
  }

  @Override
  public void doInBackground() {
    // check if there is at least one DS to update
    Utils.removeEmptyStringsFromList(dataSources);
    if (dataSources.isEmpty() && showsToUpdate.isEmpty()) {
      LOGGER.info("no datasource to update");
      MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.nonespecified"));
      return;
    }

    resetCounters();

    try {
      StopWatch stopWatch = new StopWatch();
      stopWatch.start();
      start();

      // get existing show folders
      List<Path> existing = new ArrayList<>();
      for (TvShow show : tvShowList.getTvShows()) {
        existing.add(show.getPathNIO());
      }

      // here we have 2 ways of updating:
      // - per datasource -> update ds / remove orphaned / update MFs
      // - per TV show -> udpate TV show / update MFs
      if (showsToUpdate.isEmpty()) {
        // should we re-set all new flags?
        if (TvShowModuleManager.getInstance().getSettings().isResetNewFlagOnUds()) {
          for (TvShow tvShow : tvShowList.getTvShows()) {
            tvShow.setNewlyAdded(false);

            for (TvShowEpisode episode : tvShow.getEpisodes()) {
              episode.setNewlyAdded(false);
            }
          }
        }

        // update selected data sources
        for (String ds : dataSources) {
          Path dsAsPath = Paths.get(ds);

          // check the special case, that the data source is also an ignore folder
          if (isInSkipFolder(dsAsPath)) {
            LOGGER.debug("datasource '{}' is also a skipfolder - skipping", ds);
            continue;
          }

          LOGGER.info("Start UDS on datasource: {}", ds);
          initThreadPool(3, "update");
          setTaskName(TmmResourceBundle.getString("update.datasource") + " '" + ds + "'");
          publishState();

          // first of all check if the DS is available; we can take the
          // Files.exist here:
          // if the DS exists (and we have access to read it): Files.exist =
          // true
          if (!Files.exists(dsAsPath)) {
            // error - continue with next datasource
            LOGGER.warn("Datasource not available/empty {}", ds);
            MessageManager.instance
                .pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.unavailable", new String[] { ds }));
            continue;
          }
          publishState();

          List<Path> newTvShowDirs = new ArrayList<>();
          List<Path> existingTvShowDirs = new ArrayList<>();
          List<Path> rootList = listFilesAndDirs(dsAsPath);

          // when there is _nothing_ found in the ds root, it might be offline -
          // skip further processing
          // not in Windows since that won't happen there
          if (rootList.isEmpty() && !SystemUtils.IS_OS_WINDOWS) {
            // re-check if the folder is completely empty
            boolean isEmpty = true;
            try {
              isEmpty = Utils.isFolderEmpty(dsAsPath);
            }
            catch (Exception e) {
              LOGGER.warn("could not check folder '{}' for emptiness - {}", dsAsPath, e.getMessage());
            }

            if (isEmpty) {
              // error - continue with next datasource
              MessageManager.instance
                  .pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.unavailable", new String[] { ds }));
              continue;
            }
          }

          for (Path path : rootList) {
            if (Files.isDirectory(path)) {

              // additional datasource/A/show sub dirs!
              if (path.getFileName().toString().length() == 1) {
                List<Path> subList = listFilesAndDirs(path);
                for (Path sub : subList) {
                  if (Files.isDirectory(sub)) {
                    if (existing.contains(sub)) {
                      existingTvShowDirs.add(sub);
                    }
                    else {
                      newTvShowDirs.add(sub);
                    }
                  }
                }
              }

              // normal datasource/show folder
              else {
                if (existing.contains(path)) {
                  existingTvShowDirs.add(path);
                }
                else {
                  newTvShowDirs.add(path);
                }
              }
            }
            else {
              // File in root folder - not possible for TV datasource (at least, for videos ;)
              String ext = FilenameUtils.getExtension(path.getFileName().toString()).toLowerCase(Locale.ROOT);
              if (Settings.getInstance().getVideoFileType().contains("." + ext)) {
                MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.episodeinroot",
                    new String[] { path.getFileName().toString() }));
              }
            }
          }

          for (Path subdir : newTvShowDirs) {
            submitTask(new FindTvShowTask(subdir, dsAsPath.toAbsolutePath()));
          }
          for (Path subdir : existingTvShowDirs) {
            submitTask(new FindTvShowTask(subdir, dsAsPath.toAbsolutePath()));
          }
          waitForCompletionOrCancel();

          // print stats
          LOGGER.info("FilesFound: {}", filesFound.size());
          LOGGER.info("tvShowsFound: {}", tvShowList.getTvShowCount());
          LOGGER.info("episodesFound: {}", tvShowList.getEpisodeCount());
          LOGGER.debug("PreDir: {}", preDir);
          LOGGER.debug("PostDir: {}", postDir);
          LOGGER.debug("VisFile: {}", visFile);

          if (cancel) {
            break;
          }

          cleanupDatasource(ds);
          waitForCompletionOrCancel();
          if (cancel) {
            break;
          }
        } // end foreach datasource
      }
      else { // for each selected show
        initThreadPool(3, "update");

        // get distinct data sources
        Set<String> showDatasources = new HashSet<>();
        showsToUpdate.stream().filter(show -> !show.isLocked()).forEach(show -> showDatasources.add(show.getDataSource()));

        List<TvShow> showsToCleanup = new ArrayList<>();

        // update shows grouped by data source
        for (String ds : showDatasources) {
          Path dsAsPath = Paths.get(ds);
          // first of all check if the DS is available; we can take the
          // Files.exist here:
          // if the DS exists (and we have access to read it): Files.exist = true
          if (!Files.exists(dsAsPath)) {
            // error - continue with next datasource
            MessageManager.instance
                .pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.unavailable", new String[] { ds }));
            continue;
          }

          List<Path> rootList = listFilesAndDirs(dsAsPath);

          // when there is _nothing_ found in the ds root, it might be offline - skip further processing
          // not in Windows since that won't happen there
          if (rootList.isEmpty() && !SystemUtils.IS_OS_WINDOWS) {
            // re-check if the folder is completely empty
            boolean isEmpty = true;
            try {
              isEmpty = Utils.isFolderEmpty(dsAsPath);
            }
            catch (Exception e) {
              LOGGER.warn("could not check folder '{}' for emptiness - {}", dsAsPath, e.getMessage());
            }

            if (isEmpty) {
              // error - continue with next datasource
              MessageManager.instance
                  .pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.unavailable", new String[] { ds }));
              continue;
            }
          }

          // update selected TV shows
          for (TvShow show : showsToUpdate) {
            if (!show.getDataSource().equals(ds)) {
              continue;
            }
            showsToCleanup.add(show);
            submitTask(new FindTvShowTask(show.getPathNIO(), Paths.get(ds)));
          }
        }
        waitForCompletionOrCancel();

        // print stats
        LOGGER.info("FilesFound: {}", filesFound.size());
        LOGGER.info("tvShowsFound: {}", tvShowList.getTvShowCount());
        LOGGER.info("episodesFound: {}", tvShowList.getEpisodeCount());
        LOGGER.debug("PreDir: {}", preDir);
        LOGGER.debug("PostDir: {}", postDir);
        LOGGER.debug("VisFile: {}", visFile);

        if (!cancel) {
          cleanup(showsToCleanup);
          waitForCompletionOrCancel();
        }
      }

      if (cancel) {
        return;
      }

      // map Kodi entries
      if (StringUtils.isNotBlank(Settings.getInstance().getKodiHost())) {
        // call async to avoid slowdown of UDS
        TmmTaskManager.getInstance().addUnnamedTask(() -> KodiRPC.getInstance().updateTvShowMappings());
      }

      LOGGER.info("getting Mediainfo...");

      initThreadPool(1, "mediainfo");
      setTaskName(TmmResourceBundle.getString("update.mediainfo"));
      setTaskDescription(null);
      setProgressDone(0);
      // gather MediaInformation for ALL shows - TBD
      if (!cancel) {
        if (showsToUpdate.isEmpty()) {
          // get MI for selected DS
          for (int i = tvShowList.getTvShows().size() - 1; i >= 0; i--) {
            if (cancel) {
              break;
            }
            TvShow tvShow = tvShowList.getTvShows().get(i);

            // do not process locked TV shows
            if (tvShow.isLocked()) {
              continue;
            }

            if (dataSources.contains(tvShow.getDataSource())) {
              gatherMediaInformationForUngatheredMediaFiles(tvShow);
            }
          }
        }
        else {
          // get MI for selected TV shows
          for (int i = tvShowList.getTvShows().size() - 1; i >= 0; i--) {
            if (cancel) {
              break;
            }
            TvShow tvShow = tvShowList.getTvShows().get(i);

            // do not process locked TV shows
            if (tvShow.isLocked()) {
              continue;
            }

            if (showsToUpdate.contains(tvShow)) {
              gatherMediaInformationForUngatheredMediaFiles(tvShow);
            }
          }
        }
        waitForCompletionOrCancel();
      }

      if (cancel) {
        return;
      }

      if (TvShowModuleManager.getInstance().getSettings().getSyncTrakt()) {
        TvShowSyncTraktTvTask task = new TvShowSyncTraktTvTask(TvShowModuleManager.getInstance().getTvShowList().getTvShows());
        task.setSyncCollection(TvShowModuleManager.getInstance().getSettings().getSyncTraktCollection());
        task.setSyncWatched(TvShowModuleManager.getInstance().getSettings().getSyncTraktWatched());
        task.setSyncRating(TvShowModuleManager.getInstance().getSettings().getSyncTraktRating());

        TmmTaskManager.getInstance().addUnnamedTask(task);
      }

      stopWatch.stop();
      LOGGER.info("Done updating datasource :) - took {}", stopWatch);

      resetCounters();
    }
    catch (

    Exception e) {
      LOGGER.error("Thread crashed", e);
      MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "message.update.threadcrashed"));
    }
  }

  private void cleanup(List<TvShow> shows) {
    setTaskName(TmmResourceBundle.getString("update.cleanup"));
    setTaskDescription(null);
    setProgressDone(0);
    setWorkUnits(0);
    publishState();

    LOGGER.info("removing orphaned movies/files...");
    for (int i = shows.size() - 1; i >= 0; i--) {
      if (cancel) {
        break;
      }
      TvShow tvShow = shows.get(i);

      // do not process locked TV shows (because filesFound has not been filled for them)
      if (tvShow.isLocked()) {
        continue;
      }

      if (!Files.exists(tvShow.getPathNIO())) {
        tvShowList.removeTvShow(tvShow);
      }
      else {
        cleanup(tvShow);
      }
    }
  }

  private void cleanupDatasource(String datasource) {
    setTaskName(TmmResourceBundle.getString("update.cleanup"));
    setTaskDescription(null);
    setProgressDone(0);
    setWorkUnits(0);
    publishState();
    LOGGER.info("removing orphaned tv shows/files...");

    for (int i = tvShowList.getTvShows().size() - 1; i >= 0; i--) {
      if (cancel) {
        break;
      }
      TvShow tvShow = tvShowList.getTvShows().get(i);

      // check only TV shows matching datasource
      if (!Paths.get(datasource).toAbsolutePath().equals(Paths.get(tvShow.getDataSource()).toAbsolutePath())) {
        continue;
      }

      // do not process locked TV shows (because filesFound has not been filled for them)
      if (tvShow.isLocked()) {
        continue;
      }

      if (!Files.exists(tvShow.getPathNIO())) {
        tvShowList.removeTvShow(tvShow);
      }
      else {
        cleanup(tvShow);
      }
    }
  }

  private void cleanup(TvShow tvShow) {
    boolean dirty = false;
    if (!tvShow.isNewlyAdded() || tvShow.hasNewlyAddedEpisodes()) {
      // check and delete all not found MediaFiles
      for (MediaFile mf : tvShow.getMediaFiles()) {
        boolean fileFound = filesFound.contains(mf.getFileAsPath());

        if (!fileFound) {
          LOGGER.debug("removing orphaned file: {}", mf.getFileAsPath());
          tvShow.removeFromMediaFiles(mf);
          // and remove the image cache
          if (mf.isGraphic()) {
            ImageCache.invalidateCachedImage(mf);
          }

          dirty = true;
        }
      }

      for (TvShowSeason season : tvShow.getSeasons()) {
        // check and delete all not found MediaFiles
        for (MediaFile mf : season.getMediaFiles()) {
          boolean fileFound = filesFound.contains(mf.getFileAsPath());

          if (!fileFound) {
            LOGGER.debug("removing orphaned file: {}", mf.getFileAsPath());
            season.removeFromMediaFiles(mf);
            // and remove the image cache
            if (mf.isGraphic()) {
              ImageCache.invalidateCachedImage(mf);
            }

            dirty = true;
          }
        }
      }

      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        for (MediaFile mf : episode.getMediaFiles()) {
          boolean fileFound = filesFound.contains(mf.getFileAsPath());

          if (!fileFound) {
            LOGGER.debug("removing orphaned file: {}", mf.getFileAsPath());
            episode.removeFromMediaFiles(mf);
            // and remove the image cache
            if (mf.isGraphic()) {
              ImageCache.invalidateCachedImage(mf);
            }

            dirty = true;
          }
        }

        // let's have a look if there is at least one video file for this episode
        List<MediaFile> mfs = episode.getMediaFiles(MediaFileType.VIDEO);
        if (mfs.isEmpty()) {
          tvShow.removeEpisode(episode);
          dirty = true;
        }
      }

      // check, if some episode MFs are assigned also to tvshows...!
      List<MediaFile> episodeFiles = tvShow.getEpisodesMediaFiles();
      List<MediaFile> cleanup = new ArrayList<>();
      for (MediaFile showFile : tvShow.getMediaFiles()) {
        if (episodeFiles.contains(showFile)) {
          cleanup.add(showFile);
          dirty = true;
        }
      }
      for (MediaFile mf : cleanup) {
        tvShow.removeFromMediaFiles(mf);
        LOGGER.debug("Removed duplicate show file {}", mf);
      }
    }

    if (dirty) {
      tvShow.saveToDb();
    }
  }

  /*
   * detect which mediafiles has to be parsed and start a thread to do that
   */
  private void gatherMediaInformationForUngatheredMediaFiles(TvShow tvShow) {
    // get mediainfo for tv show (fanart/poster..)
    for (MediaFile mf : tvShow.getMediaFiles()) {
      if (StringUtils.isBlank(mf.getContainerFormat())) {
        submitTask(new TvShowMediaFileInformationFetcherTask(mf, tvShow, false));
      }
      else {
        // // did the file dates/size change?
        if (MediaFileHelper.gatherFileInformation(mf)) {
          // okay, something changed with that show file - force fetching mediainfo and drop medianfo.xml
          tvShow.getMediaFiles(MediaFileType.MEDIAINFO).forEach(mediaFile -> {
            Utils.deleteFileSafely(mediaFile.getFileAsPath());
            tvShow.removeFromMediaFiles(mediaFile);
          });
          submitTask(new TvShowMediaFileInformationFetcherTask(mf, tvShow, true));
        }
      }
    }

    // get mediainfo for all episodes within this tv show
    for (TvShowEpisode episode : new ArrayList<>(tvShow.getEpisodes())) {
      for (MediaFile mf : episode.getMediaFiles()) {
        if (StringUtils.isBlank(mf.getContainerFormat())) {
          submitTask(new TvShowMediaFileInformationFetcherTask(mf, episode, false));
        }
        else {
          // at least update the file dates
          if (MediaFileHelper.gatherFileInformation(mf)) {
            // okay, something changed with that show file - force fetching mediainfo and drop medianfo.xml
            episode.getMediaFiles(MediaFileType.MEDIAINFO).forEach(mediaFile -> {
              Utils.deleteFileSafely(mediaFile.getFileAsPath());
              episode.removeFromMediaFiles(mediaFile);
            });
            submitTask(new TvShowMediaFileInformationFetcherTask(mf, episode, true));
          }
        }
      }
    }
  }

  /**
   * The Class FindTvShowTask.
   * 
   * @author Manuel Laggner
   */
  private class FindTvShowTask implements Callable<Object> {
    private final Path showDir;
    private final Path datasource;
    private final long uniqueId;

    /**
     * Instantiates a new find tv show task.
     * 
     * @param showDir
     *          the subdir
     * @param datasource
     *          the datasource
     */
    public FindTvShowTask(Path showDir, Path datasource) {
      this.showDir = showDir;
      this.datasource = datasource;
      this.uniqueId = TmmTaskManager.getInstance().GLOB_THRD_CNT.incrementAndGet();
    }

    @Override
    public String call() throws Exception {
      String name = Thread.currentThread().getName();
      if (!name.contains("-G")) {
        name = name + "-G0";
      }
      name = name.replaceAll("\\-G\\d+", "-G" + uniqueId);
      Thread.currentThread().setName(name);

      if (showDir.getFileName().toString().matches(SKIP_REGEX)) {
        LOGGER.debug("Skipping dir: {}", showDir);
        return "";
      }

      TvShow tvShow = tvShowList.getTvShowByPath(showDir);
      if (tvShow != null && tvShow.isLocked()) {
        LOGGER.info("TV show '{}' found in uds, but is locked", tvShow.getPath());
        return "";
      }

      Set<Path> allFiles = getAllFilesRecursive(showDir, Integer.MAX_VALUE);
      if (allFiles == null || allFiles.isEmpty()) {
        LOGGER.info("skip empty directory: {}", showDir);
        return "";
      }

      if (cancel) {
        return null;
      }

      LOGGER.debug("start parsing {}", showDir);
      publishState(showDir.toString());

      fileLock.writeLock().lock();
      filesFound.add(showDir.toAbsolutePath()); // our global cache
      filesFound.addAll(allFiles); // our global cache
      fileLock.writeLock().unlock();

      // convert to MFs (we need it anyway at the end)
      List<MediaFile> mfs = new ArrayList<>();
      for (Path file : allFiles) {
        if (!file.getFileName().toString().matches(SKIP_REGEX)) {
          MediaFile mf = new MediaFile(file);
          mfs.add(mf);
        }
      }
      allFiles.clear();
      Collections.sort(mfs);

      if (getMediaFiles(mfs, MediaFileType.VIDEO).isEmpty()) {
        LOGGER.info("no video file found in directory {}", showDir);
        return "";
      }

      // ******************************
      // STEP 1 - get (or create) TvShow object
      // ******************************

      // SHOW_NFO
      MediaFile showNFO = new MediaFile(showDir.resolve("tvshow.nfo"), MediaFileType.NFO); // fixate
      if (tvShow == null) {
        // tvShow did not exist - try to parse a NFO file in parent folder
        if (Files.exists(showNFO.getFileAsPath())) {
          try {
            TvShowNfoParser parser = TvShowNfoParser.parseNfo(showNFO.getFileAsPath());
            tvShow = parser.toTvShow();
          }
          catch (Exception e) {
            LOGGER.warn("problem parsing NFO: {}", e.getMessage());
          }
        }
        if (tvShow == null) {
          // create new one
          tvShow = new TvShow();
        }

        if (StringUtils.isBlank(tvShow.getTitle()) || tvShow.getYear() <= 0) {
          // we have a tv show object, but without title or year; try to parse that our of the folder/filename
          String[] ty = ParserUtils.detectCleanTitleAndYear(showDir.getFileName().toString(),
              TvShowModuleManager.getInstance().getSettings().getBadWord());
          if (StringUtils.isBlank(tvShow.getTitle()) && StringUtils.isNotBlank(ty[0])) {
            tvShow.setTitle(ty[0]);
          }
          if (tvShow.getYear() <= 0 && !ty[1].isEmpty()) {
            try {
              tvShow.setYear(Integer.parseInt(ty[1]));
            }
            catch (Exception e) {
              LOGGER.trace("could not parse int: {}", e.getMessage());
            }
          }
        }

        // was NFO, but parsing exception. try to find at least imdb id within
        if ((tvShow.getImdbId().isEmpty() || tvShow.getTmdbId() == 0) && Files.exists(showNFO.getFileAsPath())) {
          try {
            String content = Utils.readFileToString(showNFO.getFileAsPath());
            String imdb = ParserUtils.detectImdbId(content);
            if (!imdb.isEmpty()) {
              LOGGER.debug("| Found IMDB id: {}", imdb);
              tvShow.setImdbId(imdb);
            }

            String tmdb = StrgUtils.substr(content, "themoviedb\\.org\\/tv\\/(\\d+)");
            if (tvShow.getTmdbId() == 0 && !tmdb.isEmpty()) {
              LOGGER.debug("| Found TMDB id: {}", tmdb);
              tvShow.setTmdbId(MetadataUtil.parseInt(tmdb, 0));
            }

            String tvdb = StrgUtils.substr(content, "thetvdb\\.com\\/series\\/(\\d+)");
            if (tvShow.getTvdbId().isEmpty() && !tvdb.isEmpty()) {
              LOGGER.debug("| Found TVDB id: {}", tmdb);
              tvShow.setTvdbId(tvdb);
            }
          }
          catch (IOException e) {
            LOGGER.warn("| couldn't read NFO {}", showNFO);
          }
        }

        tvShow.setPath(showDir.toAbsolutePath().toString());
        tvShow.setDataSource(datasource.toString());
        tvShow.setNewlyAdded(true);
        tvShowList.addTvShow(tvShow);
      }

      // detect some IDs from show folder
      if (!MediaIdUtil.isValidImdbId(tvShow.getImdbId())) {
        tvShow.setId(MediaMetadata.IMDB, ParserUtils.detectImdbId(showDir.getFileName().toString()));
      }
      if (tvShow.getTmdbId() == 0) {
        tvShow.setId(MediaMetadata.TMDB, ParserUtils.detectTmdbId(showDir.getFileName().toString()));
      }
      if (tvShow.getTvdbId().isEmpty()) {
        tvShow.setId(MediaMetadata.TVDB, ParserUtils.detectTvdbId(showDir.getFileName().toString()));
      }

      // ******************************
      // STEP 1.1 - get all season NFO files
      // ******************************
      for (MediaFile mf : getMediaFiles(mfs, MediaFileType.NFO)) {
        Matcher matcher = SEASON_NFO_PATTERN.matcher(mf.getFilename());
        if (matcher.matches()) {
          // season NFO found - get the season number
          // this NFO must offer _at least_ the season number to be valid
          TvShowSeasonNfoParser parser = TvShowSeasonNfoParser.parseNfo(mf.getFileAsPath());
          if (parser.season > -1) {
            TvShowSeason tvShowSeason = tvShow.getOrCreateSeason(parser.season);
            tvShowSeason.merge(parser.toTvShowSeason());
            tvShowSeason.addToMediaFiles(mf);
          }
        }
      }

      // ******************************
      // STEP 2 - get all video MFs and get (or create) episodes
      // ******************************

      Set<Path> discFolders = new HashSet<>();
      for (MediaFile vid : getMediaFiles(mfs, MediaFileType.VIDEO)) {
        if (cancel) {
          return null;
        }

        // build an array of MFs, which might be in same episode
        List<MediaFile> epFiles = new ArrayList<>();

        if (vid.isDiscFile()) {
          // find EP root folder, and do not walk lower than showDir!
          Path discRoot = vid.getFileAsPath().toAbsolutePath(); // folder
          if (!discRoot.getFileName().toString().matches(DISC_FOLDER_REGEX)) {
            discRoot = discRoot.getParent();
          }
          if (discFolders.contains(discRoot)) {
            // we already parsed one disc file (which adds all other videos), so
            // break here already
            continue;
          }
          discFolders.add(discRoot);
          // add all known files starting with same discRootDir
          for (MediaFile em : mfs) {
            if (em.getFileAsPath().startsWith(discRoot)) {
              if (em.getType() != MediaFileType.UNKNOWN) {
                epFiles.add(em);
              }
            }
          }
        }
        else {
          // epFiles.add(vid); // add ourself

          // normal episode file - get all same named files (in same directory!)
          String vidBasename = FilenameUtils.getBaseName(Utils.cleanStackingMarkers(vid.getFilename()));
          vidBasename = showDir.relativize(vid.getFileAsPath().getParent()) + "/" + vidBasename;
          LOGGER.trace("UDS: video basename {} - {}", vidBasename, vid.getFile());
          for (MediaFile other : mfs) {
            // change asdf-poster.jpg -> asdf.jpg, to ease basename matching ;)
            String imgBasename = FilenameUtils.getBaseName(Utils.cleanStackingMarkers(getMediaFileNameWithoutType(other)));
            imgBasename = showDir.relativize(other.getFileAsPath().getParent()) + "/" + imgBasename;

            // we now got a match with same (generated) basename!
            if (vidBasename.equalsIgnoreCase(imgBasename)) {
              if (other.getType() == MediaFileType.POSTER || other.getType() == MediaFileType.GRAPHIC) {
                // re-type posters to EP "posters" (=thumb)
                other.setType(MediaFileType.THUMB);
              }
              epFiles.add(other);
              LOGGER.trace("UDS: found matching {} - {}", imgBasename, other.getFile());
            }
          } // end inner MF loop over all non videos
        } // end MF nodisc file

        // ******************************
        // STEP 2.1 - is this file already assigned to another episode?
        // ******************************
        List<TvShowEpisode> episodes = TvShowList.getTvEpisodesByFile(tvShow, vid.getFile());
        if (episodes.isEmpty()) {

          // ******************************
          // STEP 2.1.1 - parse EP NFO (has precedence over files)
          // ******************************

          // meta data from VSMETA files
          MediaFile meta = getMediaFile(epFiles, MediaFileType.VSMETA);
          TvShowEpisode vsMetaEP = null;
          if (meta != null) {
            VSMeta vsmeta = new VSMeta(meta.getFileAsPath());
            vsmeta.parseFile();
            vsMetaEP = vsmeta.getTvShowEpisode();
          }

          // meta data from XML files
          TvShowEpisode xmlEP = null;
          for (MediaFile xmlMf : epFiles) {
            if ("xml".equalsIgnoreCase(xmlMf.getExtension()) && !xmlMf.getFilename().endsWith("mediainfo.xml")) {
              try {
                TvShowEpisodeNfoParser nfoParser = TvShowEpisodeNfoParser.parseNfo(xmlMf.getFileAsPath());
                List<TvShowEpisode> epsInXml = nfoParser.toTvShowEpisodes();
                if (!epsInXml.isEmpty()) {
                  xmlEP = epsInXml.get(0);
                }
              }
              catch (Exception e) {
                // ignored
              }
            }
          }

          // drop all unknown EP files
          epFiles = epFiles.stream().filter(mediaFile -> mediaFile.getType() != MediaFileType.UNKNOWN).collect(Collectors.toList());

          MediaFile epNfo = getMediaFile(epFiles, MediaFileType.NFO);
          if (epNfo != null) {
            LOGGER.debug("found episode NFO - try to parse '{}'", showDir.relativize(epNfo.getFileAsPath()));
            List<TvShowEpisode> episodesInNfo = new ArrayList<>();

            try {
              TvShowEpisodeNfoParser parser = TvShowEpisodeNfoParser.parseNfo(epNfo.getFileAsPath());

              // ALL episodes detected with -1? try to parse from filename...
              boolean allUnknown = !parser.episodes.isEmpty() && parser.episodes.stream().allMatch(ep -> ep.episode == -1);
              if (allUnknown) {
                EpisodeMatchingResult result = TvShowEpisodeAndSeasonParser
                    .detectEpisodeFromFilename(showDir.relativize(epNfo.getFileAsPath()).toString(), tvShow.getTitle());
                if (parser.episodes.size() == result.episodes.size()) {
                  int i = 0;
                  for (Episode ep : parser.episodes) {
                    ep.episode = result.episodes.get(i);
                    ep.season = result.season;
                    i++;
                  }
                }
              }

              if (parser.isValidNfo()) {
                episodesInNfo.addAll(parser.toTvShowEpisodes());
              }
            }
            catch (Exception e) {
              LOGGER.debug("could not parse episode NFO: {}", e.getMessage());
            }

            // did we find any episodes in the NFO?
            if (!episodesInNfo.isEmpty()) {
              // these have priority!
              for (TvShowEpisode episode : episodesInNfo) {
                episode.setPath(vid.getPath());
                episode.setTvShow(tvShow);

                if (episode.getMediaSource() == MediaSource.UNKNOWN) {
                  episode.setMediaSource(MediaSource.parseMediaSource(vid.getBasename()));
                }
                episode.setNewlyAdded(true);

                // remember the filename the first time the show gets added to tmm
                if (StringUtils.isBlank(episode.getOriginalFilename())) {
                  episode.setOriginalFilename(vid.getFilename());
                }

                episode.addToMediaFiles(epFiles); // all found EP MFs

                if (vid.isDiscFile()) {
                  episode.setDisc(true);

                  // disc files should be inside a discFolder - if we have one, set the path a level higher:
                  Path discRoot = vid.getFileAsPath().toAbsolutePath();
                  if (discRoot.getFileName().toString().matches(DISC_FOLDER_REGEX)) {
                    // name of video file matches a disc folder? (eg when having already a virtual one)
                    discRoot = discRoot.getParent();
                    episode.setPath(discRoot.toString());
                  }
                  else if (discRoot.getParent().getFileName().toString().matches(DISC_FOLDER_REGEX)) {
                    // video file not in its dedicated folder
                    discRoot = discRoot.getParent();
                    episode.setPath(discRoot.toString());
                  }
                  // else keep the current video path as episode root (set above)
                }

                if (episodesInNfo.size() > 1) {
                  episode.setMultiEpisode(true);
                }
                else {
                  episode.setMultiEpisode(false);
                }
                episode.merge(vsMetaEP); // merge VSmeta infos
                episode.merge(xmlEP); // merge XML infos

                episode.saveToDb();
                tvShow.addEpisode(episode);
              }
              continue; // with next video MF
            }
          } // end parse NFO

          // ******************************
          // STEP 2.1.2 - no NFO? try to parse episode/season
          // ******************************
          String relativePath = showDir.relativize(vid.getFileAsPath()).toString();
          EpisodeMatchingResult result = TvShowEpisodeAndSeasonParser.detectEpisodeFromFilename(relativePath, tvShow.getTitle());

          // second check: is the detected episode (>-1; season >-1) already in
          // tmm and any valid stacking markers found?
          if (result.episodes.size() == 1 && result.season > -1 && result.stackingMarkerFound) {
            // get any assigned episode
            List<TvShowEpisode> eps = tvShow.getEpisode(result.season, result.episodes.get(0));
            if (!eps.isEmpty()) {
              // okay, at least one existing episode found.. just check if there is the same base name without stacking markers
              boolean found = false;
              for (TvShowEpisode ep : eps) {
                // need to call Utils.cleanStackingMarkers() because the MF stacking markers aren't detected yet
                String episodeBasenameWoStackingMarker = FilenameUtils.getBaseName(Utils.cleanStackingMarkers(ep.getMainVideoFile().getFilename()));
                String mfBasenameWoStackingMarker = FilenameUtils.getBaseName(Utils.cleanStackingMarkers(vid.getFilename()));

                if (episodeBasenameWoStackingMarker.equals(mfBasenameWoStackingMarker)) {
                  if (ep.getMediaSource() == MediaSource.UNKNOWN) {
                    ep.setMediaSource(MediaSource.parseMediaSource(ep.getMainVideoFile().getBasename()));
                  }

                  ep.setNewlyAdded(true);

                  // remember the filename the first time the show gets added to tmm
                  if (StringUtils.isBlank(ep.getOriginalFilename())) {
                    ep.setOriginalFilename(vid.getFilename());
                  }

                  ep.addToMediaFiles(vid);
                  found = true;
                  break;
                }
              }
              if (found) {
                continue;
              }
            }
          }
          if (!result.episodes.isEmpty()) {
            // something found with the season detection?
            for (int ep : result.episodes) {
              TvShowEpisode episode = new TvShowEpisode();
              if (tvShow.getEpisodeGroup() != null) {
                // the TV show already has an assigned episode group - assign the S/E to the same group
                episode.setEpisode(new MediaEpisodeNumber(tvShow.getEpisodeGroup(), result.season, ep));
              }
              else {
                episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, result.season, ep));
              }

              episode.setFirstAired(result.date);
              if (result.name.isEmpty()) {
                result.name = FilenameUtils.getBaseName(vid.getFilename());
              }
              episode.setTitle(TvShowEpisodeAndSeasonParser.cleanEpisodeTitle(result.name, tvShow.getTitle()));
              episode.setPath(vid.getPath());
              episode.setTvShow(tvShow);
              episode.addToMediaFiles(epFiles); // all found EP MFs

              // try to parse the imdb id from the filename
              if (!MediaIdUtil.isValidImdbId(episode.getImdbId())) {
                episode.setId(MediaMetadata.IMDB, ParserUtils.detectImdbId(Utils.relPath(showDir, vid.getFileAsPath()).toString()));
              }
              // try to parse the Tmdb id from the filename
              if (episode.getTmdbId().isEmpty()) {
                episode.setId(MediaMetadata.TMDB, ParserUtils.detectTmdbId(Utils.relPath(showDir, vid.getFileAsPath()).toString()));
              }
              // try to parse the Tvdb id from the filename
              if (episode.getTvdbId().isEmpty()) {
                episode.setId(MediaMetadata.TVDB, ParserUtils.detectTvdbId(Utils.relPath(showDir, vid.getFileAsPath()).toString()));
              }
              if (episode.getMediaSource() == MediaSource.UNKNOWN) {
                episode.setMediaSource(MediaSource.parseMediaSource(vid.getBasename()));
              }
              episode.setNewlyAdded(true);

              // remember the filename the first time the show gets added to tmm
              if (StringUtils.isBlank(episode.getOriginalFilename())) {
                episode.setOriginalFilename(vid.getFilename());
              }

              if (vid.isDiscFile()) {
                episode.setDisc(true);

                // disc files should be inside a discFolder - if we have one, set the path a level higher:
                Path discRoot = vid.getFileAsPath().toAbsolutePath();
                if (discRoot.getFileName().toString().matches(DISC_FOLDER_REGEX)) {
                  // name of video file matches a disc folder? (eg when having already a virtual one)
                  discRoot = discRoot.getParent();
                  episode.setPath(discRoot.toString());
                }
                else if (discRoot.getParent().getFileName().toString().matches(DISC_FOLDER_REGEX)) {
                  // video file not in its dedicated folder
                  discRoot = discRoot.getParent();
                  episode.setPath(discRoot.toString());
                }
                // else keep the current video path as episode root (set above)
              }

              if (result.episodes.size() > 1) {
                episode.setMultiEpisode(true);
              }
              else {
                episode.setMultiEpisode(false);
              }
              episode.merge(vsMetaEP); // merge VSmeta infos

              // force title from xml
              if (xmlEP != null && StringUtils.isNotBlank(xmlEP.getTitle())) {
                episode.merge(xmlEP); // merge XML infos
                episode.setTitle(xmlEP.getTitle());
              }

              episode.saveToDb();
              tvShow.addEpisode(episode);
            }
          }
          else {
            // ******************************
            // STEP 2.1.3 - episode detection found nothing - simply add this
            // video as -1/-1 (or at least with year as season, if detected
            // ******************************
            TvShowEpisode episode = new TvShowEpisode();
            episode.setPath(vid.getPath());

            if (vid.isDiscFile()) {
              episode.setDisc(true);

              // disc files should be inside a discFolder - if we have one, set the path a level higher:
              Path discRoot = vid.getFileAsPath().toAbsolutePath();
              if (discRoot.getFileName().toString().matches(DISC_FOLDER_REGEX)) {
                // name of video file matches a disc folder? (eg when having already a virtual one)
                discRoot = discRoot.getParent();
                episode.setPath(discRoot.toString());
              }
              else if (discRoot.getParent().getFileName().toString().matches(DISC_FOLDER_REGEX)) {
                // video file not in its dedicated folder
                discRoot = discRoot.getParent();
                episode.setPath(discRoot.toString());
              }
              // else keep the current video path as episode root (set above)
            }

            episode.setTitle(TvShowEpisodeAndSeasonParser.cleanEpisodeTitle(FilenameUtils.getBaseName(vid.getFilename()), tvShow.getTitle()));
            episode.setTvShow(tvShow);
            if (result.date != null) {
              // if we have a date, we also have a season (=year)
              episode.setFirstAired(result.date);
              episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, result.season, -1));
            }
            episode.addToMediaFiles(epFiles); // all found EP MFs

            // try to parse the imdb id from the filename
            if (!MediaIdUtil.isValidImdbId(episode.getImdbId())) {
              episode.setId(MediaMetadata.IMDB, ParserUtils.detectImdbId(Utils.relPath(showDir, vid.getFileAsPath()).toString()));
            }
            // try to parse the Tmdb id from the filename
            if (episode.getTmdbId().isEmpty()) {
              episode.setId(MediaMetadata.TMDB, ParserUtils.detectTmdbId(Utils.relPath(showDir, vid.getFileAsPath()).toString()));
            }
            // try to parse the Tvdb id from the filename
            if (episode.getTvdbId().isEmpty()) {
              episode.setId(MediaMetadata.TVDB, ParserUtils.detectTvdbId(Utils.relPath(showDir, vid.getFileAsPath()).toString()));
            }
            if (episode.getMediaSource() == MediaSource.UNKNOWN) {
              episode.setMediaSource(MediaSource.parseMediaSource(vid.getBasename()));
            }
            episode.setNewlyAdded(true);

            // remember the filename the first time the show gets added to tmm
            if (StringUtils.isBlank(episode.getOriginalFilename())) {
              episode.setOriginalFilename(vid.getFilename());
            }

            episode.merge(vsMetaEP); // merge VSmeta infos

            // force title from xml
            if (xmlEP != null && StringUtils.isNotBlank(xmlEP.getTitle())) {
              episode.merge(xmlEP); // merge XML infos
              episode.setTitle(xmlEP.getTitle());
            }

            episode.saveToDb();
            tvShow.addEpisode(episode);
          }
        } // end creation of new episodes
        else {
          // ******************************
          // STEP 2.2 - video MF was already found in DB - just add all
          // non-video MFs
          // ******************************
          for (TvShowEpisode episode : episodes) {
            episode.addToMediaFiles(epFiles); // add all (dupes will be filtered)
            episode.setDisc(vid.isDiscFile());
            if (episodes.size() > 1) {
              episode.setMultiEpisode(true);
            }
            else {
              episode.setMultiEpisode(false);
            }
            episode.saveToDb();
          }
        }
      } // end for all video MFs loop

      // ******************************
      // STEP 3 - now we have a working show/episode object
      // remove all used episode/season MFs, rest must be show MFs ;)
      // ******************************
      mfs.removeAll(tvShow.getEpisodesMediaFiles()); // remove EP files

      for (TvShowSeason season : tvShow.getSeasons()) {
        mfs.removeAll(season.getMediaFiles());
      }

      // tvShow.addToMediaFiles(mfs); // add remaining
      // not so fast - try to parse S/E from remaining first!
      for (MediaFile mf : mfs) {
        // case poster.ext -> do not add to the TV show itself when it is NOT in the TV show root!
        if (mf.getType() == MediaFileType.POSTER && !mf.getPath().equals(tvShow.getPath())) {
          // probably season poster
          mf.setType(MediaFileType.SEASON_POSTER);
        }

        // a season poster/fanart/banner/thumb does not belong to any episode - they need to be added to a TvShowSeason
        if (mf.getType() == MediaFileType.SEASON_POSTER || mf.getType() == MediaFileType.SEASON_FANART || mf.getType() == MediaFileType.SEASON_BANNER
            || mf.getType() == MediaFileType.SEASON_THUMB) {

          String foldername = tvShow.getPathNIO().relativize(mf.getFileAsPath().getParent()).toString();
          int season = TvShowHelpers.detectSeasonFromFileAndFolder(mf.getFilename(), foldername);
          if (season != Integer.MIN_VALUE) {
            TvShowSeason tvShowSeason = tvShow.getOrCreateSeason(season);
            tvShowSeason.addToMediaFiles(mf);
          }

          continue;
        }

        String relativePath = showDir.relativize(mf.getFileAsPath()).toString();
        EpisodeMatchingResult result = TvShowEpisodeAndSeasonParser.detectEpisodeFromFilename(relativePath, tvShow.getTitle());
        if (result.season > -1 && !result.episodes.isEmpty()) {
          for (int epnr : result.episodes) {
            // get any assigned episode
            List<TvShowEpisode> eps = tvShow.getEpisode(result.season, epnr);
            if (eps.size() == 1) {
              // just one episode for that S/E found -> we can blindly assign it
              eps.get(0).addToMediaFiles(mf);
            }
            else if (eps.size() > 1) {
              for (TvShowEpisode ep : eps) {
                String episodeBasenameWoStackingMarker = FilenameUtils.getBaseName(Utils.cleanStackingMarkers(ep.getMainVideoFile().getFilename()));
                // okay, at least one existing episode found... just check if there is the same base name without stacking markers
                if (FilenameUtils.getBaseName(Utils.cleanStackingMarkers(mf.getFilename())).startsWith(episodeBasenameWoStackingMarker)) {
                  ep.addToMediaFiles(mf);
                  break;
                }
                // or if the mf is in a subfolder with the base name of the video file
                if (episodeBasenameWoStackingMarker.equals(mf.getFileAsPath().getParent().getFileName().toString())) {
                  ep.addToMediaFiles(mf);
                  break;
                }
              }
            }
          }
        }
      }

      // remove EP files
      mfs.removeAll(tvShow.getEpisodesMediaFiles());

      // remove season files
      tvShow.getSeasons().forEach(tvShowSeason -> mfs.removeAll(tvShowSeason.getMediaFiles()));

      // now add remaining
      tvShow.addToMediaFiles(mfs);

      // re-evaluate stacking markers & disc folders
      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        episode.reEvaluateDiscfolder();
        episode.reEvaluateStacking();
        episode.saveToDb();
      }

      // if there is missing artwork AND we do have a VSMETA file, we probably can extract an artwork from there
      if (!TvShowModuleManager.getInstance().getSettings().isExtractArtworkFromVsmeta()) {
        // TV show
        boolean missingTvShowPosters = tvShow.getMediaFiles(MediaFileType.POSTER).isEmpty();
        boolean missingTvShowFanarts = tvShow.getMediaFiles(MediaFileType.FANART).isEmpty();

        for (TvShowEpisode episode : tvShow.getEpisodes()) {
          List<MediaFile> episodeVsmetas = episode.getMediaFiles(MediaFileType.VSMETA);
          if (episodeVsmetas.isEmpty()) {
            continue;
          }

          if (episode.getMediaFiles(MediaFileType.THUMB).isEmpty()
              && !TvShowModuleManager.getInstance().getSettings().getSeasonThumbFilenames().isEmpty()) {
            LOGGER.debug("extracting episode THUMBs from VSMETA for {}", episode.getMainFile().getFileAsPath());
            boolean ok = TvShowArtworkHelper.extractArtworkFromVsmeta(episode, episodeVsmetas.get(0), MediaArtwork.MediaArtworkType.THUMB);
            if (ok) {
              episode.saveToDb();
            }
          }

          if (missingTvShowFanarts && !TvShowModuleManager.getInstance().getSettings().getFanartFilenames().isEmpty()) {
            LOGGER.debug("extracting TV show FANARTs from VSMETA for {}", episode.getMainFile().getFileAsPath());
            boolean ok = TvShowArtworkHelper.extractArtworkFromVsmeta(tvShow, episodeVsmetas.get(0), MediaArtwork.MediaArtworkType.BACKGROUND);
            if (ok) {
              missingTvShowFanarts = false;
            }
          }

          if (missingTvShowPosters && !TvShowModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty()) {
            LOGGER.debug("extracting TV show POSTERs from VSMETA for {}", episode.getMainFile().getFileAsPath());
            boolean ok = TvShowArtworkHelper.extractArtworkFromVsmeta(tvShow, episodeVsmetas.get(0), MediaArtwork.MediaArtworkType.POSTER);
            if (ok) {
              missingTvShowPosters = false;
            }
          }
        }
      }

      tvShow.saveToDb();

      return showDir.getFileName().toString();
    }

    /**
     * gets the filename of the MF, reduced by type<br>
     * episode1-poster.jpg -> episode1.jpg<br>
     * also remove named extra files like '*-behinthescenes'
     *
     * @param mf
     * @return
     */
    private String getMediaFileNameWithoutType(MediaFile mf) {
      String ret = mf.getFilename();
      // does not work for extrafanarts/landscape - but thats mostly not used on episode level
      ret = ret.replaceFirst("(?i)[_.-]" + mf.getType() + "[.]" + mf.getExtension(), "." + mf.getExtension());

      for (String extr : MediaFileHelper.EXTRA_FOLDERS) {
        ret = ret.replaceFirst("(?i)[_.-]" + extr + "\\d?[.]" + mf.getExtension(), "." + mf.getExtension());
      }
      return ret;
    }

    /**
     * gets mediaFile of specific type
     *
     * @param mfs
     *          the MF list to search
     * @param types
     *          the MediaFileTypes
     * @return MF or NULL
     */
    private MediaFile getMediaFile(List<MediaFile> mfs, MediaFileType... types) {
      MediaFile mf = null;
      for (MediaFile mediaFile : mfs) {
        boolean match = false;
        for (MediaFileType type : types) {
          if (mediaFile.getType().equals(type)) {
            match = true;
          }
        }
        if (match) {
          mf = new MediaFile(mediaFile);
        }
      }
      return mf;
    }

    /**
     * gets all mediaFiles of specific type
     *
     * @param mfs
     *          the MF list to search
     * @param types
     *          the MediaFileTypes
     * @return list of matching MFs
     */
    private List<MediaFile> getMediaFiles(List<MediaFile> mfs, MediaFileType... types) {
      List<MediaFile> mf = new ArrayList<>();
      for (MediaFile mediaFile : mfs) {
        boolean match = false;
        for (MediaFileType type : types) {
          if (mediaFile.getType().equals(type)) {
            match = true;
          }
        }
        if (match) {
          mf.add(new MediaFile(mediaFile));
        }
      }
      return mf;
    }

    /**
     * returns all MFs NOT matching specified type
     *
     * @param mfs
     *          array to search
     * @param types
     *          MF types to exclude
     * @return list of matching MFs
     */
    private List<MediaFile> getMediaFilesExceptType(List<MediaFile> mfs, MediaFileType... types) {
      List<MediaFile> mf = new ArrayList<>();
      for (MediaFile mediaFile : mfs) {
        boolean match = false;
        for (MediaFileType type : types) {
          if (mediaFile.getType().equals(type)) {
            match = true;
          }
        }
        if (!match) {
          mf.add(new MediaFile(mediaFile));
        }
      }
      return mf;
    }

    /**
     * gets all files recursive
     * 
     * @param path
     *          the folder to search for
     * @param deep
     *          how much levels to search for
     * @return a {@link Set} of all found {@link Path}s
     */
    private Set<Path> getAllFilesRecursive(Path path, int deep) {
      Path folder = path.toAbsolutePath();
      AllFilesRecursive visitor = new AllFilesRecursive();
      try {
        Files.walkFileTree(folder, EnumSet.of(FileVisitOption.FOLLOW_LINKS), deep, visitor);
      }
      catch (IOException e) {
        // can not happen, since we've overridden visitFileFailed, which throws no exception ;)
      }
      return visitor.fFound;
    }

  }

  @Override
  public void callback(Object obj) {
    // do not publish task description here, because with different workers the
    // text is never right
    publishState(progressDone);
  }

  /**
   * simple NIO File.listFiles() replacement<br>
   * returns all files & folders in specified dir (NOT recursive)
   *
   * @param directory
   *          the folder to list the items for
   * @return list of files&folders
   */
  private List<Path> listFilesAndDirs(Path directory) {
    List<Path> fileNames = new ArrayList<>();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
      for (Path path : directoryStream) {
        if (isInSkipFolder(path)) {
          LOGGER.debug("Skipping: {}", path);
        }
        else {
          fileNames.add(path.toAbsolutePath());
        }
      }
    }
    catch (IOException e) {
      LOGGER.error("error on listFilesAndDirs", e);
      LOGGER.debug("falling back to the alternate coding");
      fileNames = listFilesAndDirs2(directory);
    }

    // return sorted
    Collections.sort(fileNames);

    return fileNames;
  }

  /**
   * check if the given folder is a skip folder
   *
   * @param dir
   *          the folder to check
   * @return true/false
   */
  private boolean isInSkipFolder(Path dir) {
    if (dir == null || dir.getFileName() == null) {
      return false;
    }

    String dirName = dir.getFileName().toString();
    String dirNameUppercase = dirName.toUpperCase(Locale.ROOT);
    String fullPath = dir.toAbsolutePath().toString();

    // hard coded skip folders
    if (SKIP_FOLDERS.contains(dirNameUppercase) || dirName.matches(SKIP_REGEX)) {
      return true;
    }

    // skip folders from regexp
    for (Pattern pattern : skipFolders) {
      Matcher matcher = pattern.matcher(dirName);
      if (matcher.matches()) {
        return true;
      }

      // maybe the regexp is a full path
      if (pattern.toString().replace("\\Q", "").replace("\\E", "").equals(fullPath)) {
        return true;
      }
    }

    return false;
  }

  /**
   * simple NIO File.listFiles() replacement<br>
   * returns all folders in specified dir (NOT recursive)
   * 
   * @param directory
   *          the folder to list the items for
   * @return list of files&folders
   */
  private List<Path> listFilesAndDirs2(Path directory) {
    List<Path> fileNames = new ArrayList<>();
    try (Stream<Path> directoryStream = Files.walk(directory, 1, FileVisitOption.FOLLOW_LINKS)) {
      List<Path> allElements = directoryStream.filter(Files::isDirectory).collect(Collectors.toList());
      for (Path path : allElements) {
        if (directory.toAbsolutePath().equals(path.toAbsolutePath())) {
          continue;
        }
        String fn = path.getFileName().toString().toUpperCase(Locale.ROOT);
        if (!SKIP_FOLDERS.contains(fn) && !fn.matches(SKIP_REGEX) && !skipFolders.contains(path.toFile().getAbsolutePath())) {
          fileNames.add(path.toAbsolutePath());
        }
        else {
          LOGGER.debug("Skipping: {}", path);
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("error on listFilesAndDirs2", e);
    }

    // return sorted
    Collections.sort(fileNames);

    return fileNames;
  }

  private class AllFilesRecursive extends AbstractFileVisitor {
    private final Set<Path> fFound = new HashSet<>();

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
      if (cancel) {
        return TERMINATE;
      }

      incVisFile();

      if (file.getFileName() == null) {
        return CONTINUE;
      }

      try {

        String filename = file.getFileName().toString();
        String path = "";
        if (file.getParent() != null && file.getParent().getFileName() != null) {
          path = file.getParent().getFileName().toString();
        }

        // in a disc folder we only accept NFO files
        if (Utils.isRegularFile(attr) && path.matches(DISC_FOLDER_REGEX)) {
          if (FilenameUtils.getExtension(filename).equalsIgnoreCase("nfo")) {
            fFound.add(file.toAbsolutePath());
          }
          return CONTINUE;
        }

        // check if we're in dirty disc folder
        if (MediaFileHelper.isMainDiscIdentifierFile(filename)) {
          fFound.add(file.toAbsolutePath());
          return CONTINUE;
        }

        if (Utils.isRegularFile(attr) && !filename.matches(SKIP_REGEX)) {
          fFound.add(file.toAbsolutePath());
          return CONTINUE;
        }
      }
      catch (Exception e) {
        LOGGER.debug("could not analyze file '{}' - '{}'", file.toAbsolutePath(), e.getMessage());
      }

      return CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
      if (cancel) {
        return TERMINATE;
      }

      incPreDir();

      try {
        // getFilename returns null on DS root!
        if (dir.getFileName() != null && (isInSkipFolder(dir) || containsSkipFile(dir))) {
          LOGGER.debug("Skipping dir: {}", dir);
          return SKIP_SUBTREE;
        }

        // add the disc folder itself (clean disc folder)
        if (dir.getFileName() != null && dir.getFileName().toString().matches(DISC_FOLDER_REGEX)) {
          fFound.add(dir.toAbsolutePath());
          return CONTINUE;
        }

        // don't go below a disc folder
        if (dir.getParent() != null && dir.getParent().getFileName() != null && dir.getParent().getFileName().toString().matches(DISC_FOLDER_REGEX)) {
          return SKIP_SUBTREE;
        }
      }
      catch (Exception e) {
        LOGGER.debug("could not analyze folder '{}' - '{}'", dir.toAbsolutePath(), e.getMessage());
      }

      return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
      if (cancel) {
        return TERMINATE;
      }

      incPostDir();

      return CONTINUE;
    }
  }

  private static void resetCounters() {
    visFile = 0;
    preDir = 0;
    postDir = 0;
  }

  /**
   * synchronized increment of visFile
   */
  private static synchronized void incVisFile() {
    visFile++;
  }

  /**
   * synchronized increment of preDir
   */
  private static synchronized void incPreDir() {
    preDir++;
  }

  /**
   * synchronized increment of postDir
   */
  private static synchronized void incPostDir() {
    postDir++;
  }

  /**
   * helper class just do inject the file name in the task description
   */
  private class TvShowMediaFileInformationFetcherTask extends MediaFileInformationFetcherTask {
    public TvShowMediaFileInformationFetcherTask(MediaFile mediaFile, MediaEntity mediaEntity, boolean forceUpdate) {
      super(mediaFile, mediaEntity, forceUpdate);
    }

    @Override
    public void run() {
      // pass the filename to the task description
      publishState(mediaEntity.getTitle() + " - " + mediaFile.getFilename());
      super.run();
    }
  }
}
