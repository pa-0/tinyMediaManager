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
package org.tinymediamanager.core.movie.tasks;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static org.tinymediamanager.core.MediaFileHelper.BDMV;
import static org.tinymediamanager.core.MediaFileHelper.HVDVD_TS;
import static org.tinymediamanager.core.MediaFileHelper.VIDEO_TS;
import static org.tinymediamanager.core.MediaFileType.BANNER;
import static org.tinymediamanager.core.MediaFileType.CLEARART;
import static org.tinymediamanager.core.MediaFileType.CLEARLOGO;
import static org.tinymediamanager.core.MediaFileType.DISC;
import static org.tinymediamanager.core.MediaFileType.FANART;
import static org.tinymediamanager.core.MediaFileType.GRAPHIC;
import static org.tinymediamanager.core.MediaFileType.KEYART;
import static org.tinymediamanager.core.MediaFileType.LOGO;
import static org.tinymediamanager.core.MediaFileType.POSTER;
import static org.tinymediamanager.core.MediaFileType.VIDEO;
import static org.tinymediamanager.core.Utils.DISC_FOLDER_REGEX;
import static org.tinymediamanager.core.Utils.containsSkipFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.text.WordUtils;
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
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.movie.MovieArtworkHelper;
import org.tinymediamanager.core.movie.MovieEdition;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieMediaFileComparator;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.connector.MovieNfoParser;
import org.tinymediamanager.core.movie.connector.MovieSetNfoParser;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.tasks.MediaFileInformationFetcherTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.ParserUtils;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.thirdparty.VSMeta;
import org.tinymediamanager.thirdparty.trakttv.MovieSyncTraktTvTask;

/**
 * The Class UpdateDataSourcesTask.
 * 
 * @author Myron Boyle
 */
public class MovieUpdateDatasourceTask extends TmmThreadPool {
  private static final Logger          LOGGER           = LoggerFactory.getLogger(MovieUpdateDatasourceTask.class);

  private static long                  preDir           = 0;
  private static long                  postDir          = 0;
  private static long                  visFile          = 0;
  private static long                  preDirAll        = 0;
  private static long                  postDirAll       = 0;
  private static long                  visFileAll       = 0;

  // skip well-known, but unneeded folders (UPPERCASE)
  private static final List<String>    SKIP_FOLDERS     = Arrays.asList(".", "..", "CERTIFICATE", "$RECYCLE.BIN", "RECYCLER",
      "SYSTEM VOLUME INFORMATION", "@EADIR", "ADV_OBJ", "PLEX VERSIONS", "LOST.DIR");

  // skip folders starting with a SINGLE "." or "._" (exception for movie ".45")
  private static final String          SKIP_REGEX       = "(?i)^[.@](?!45|buelos)[\\w@]+.*";
  // MMD detected as single movie in a structured folder such as /A/, /2010/ or decade
  public static final String           FOLDER_STRUCTURE = "(?i)^(\\w|\\d{4}|\\d{4}s|\\d{4}\\-\\d{4})$";
  private static final Pattern         VIDEO_3D_PATTERN = Pattern.compile("(?i)[ .,_\\(\\[-]3D[ .,_\\)\\]-]?");

  private final List<String>           dataSources;
  private final List<Pattern>          skipFolders      = new ArrayList<>();
  private final List<Movie>            moviesToUpdate   = new ArrayList<>();
  private final MovieList              movieList        = MovieModuleManager.getInstance().getMovieList();
  private final Set<Path>              filesFound       = new HashSet<>();
  private final ReentrantReadWriteLock fileLock         = new ReentrantReadWriteLock();
  private final List<Runnable>         miTasks          = Collections.synchronizedList(new ArrayList<>());
  private final List<Path>             existingMovies   = new ArrayList<>();
  private final List<MediaFile>        imageFiles       = new ArrayList<>();

  public MovieUpdateDatasourceTask() {
    this(MovieModuleManager.getInstance().getSettings().getMovieDataSource());
  }

  public MovieUpdateDatasourceTask(Collection<String> datasources) {
    super(TmmResourceBundle.getString("update.datasource"));
    dataSources = new ArrayList<>(datasources);

    init();
  }

  public MovieUpdateDatasourceTask(String datasource) {
    super(TmmResourceBundle.getString("update.datasource") + " (" + datasource + ")");
    dataSources = new ArrayList<>(1);
    dataSources.add(datasource);

    init();
  }

  public MovieUpdateDatasourceTask(List<Movie> movies) {
    super(TmmResourceBundle.getString("update.datasource"));
    dataSources = new ArrayList<>(0);
    moviesToUpdate.addAll(movies);

    init();
  }

  private void init() {
    for (String skipFolder : MovieModuleManager.getInstance().getSettings().getSkipFolder()) {
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
    if (dataSources.isEmpty() && moviesToUpdate.isEmpty()) {
      LOGGER.info("no datasource to update");
      MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.nonespecified"));
      return;
    }
    preDir = 0;
    postDir = 0;
    visFile = 0;
    preDirAll = 0;
    postDirAll = 0;
    visFileAll = 0;

    // get existing movie folders
    for (Movie movie : movieList.getMovies()) {
      existingMovies.add(movie.getPathNIO());
    }

    try {
      StopWatch stopWatch = new StopWatch();
      stopWatch.start();

      // find movie set NFOs
      updateMovieSets();

      if (moviesToUpdate.isEmpty()) {
        updateDatasource();
      }
      else {
        updateMovies();
      }

      if (!imageFiles.isEmpty()) {
        imageFiles.forEach(ImageCache::cacheImageAsync);
      }

      if (MovieModuleManager.getInstance().getSettings().getSyncTrakt()) {
        MovieSyncTraktTvTask task = new MovieSyncTraktTvTask(MovieModuleManager.getInstance().getMovieList().getMovies());
        task.setSyncCollection(MovieModuleManager.getInstance().getSettings().getSyncTraktCollection());
        task.setSyncWatched(MovieModuleManager.getInstance().getSettings().getSyncTraktWatched());
        task.setSyncRating(MovieModuleManager.getInstance().getSettings().getSyncTraktRating());

        TmmTaskManager.getInstance().addUnnamedTask(task);
      }

      movieList.reevaluateMMD();
      stopWatch.stop();
      LOGGER.info("Done updating datasource :) - took {}", stopWatch);
    }
    catch (Exception e) {
      LOGGER.error("Thread crashed", e);
      MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "message.update.threadcrashed"));
    }
  }

  private void updateDatasource() {
    // should we re-set all new flags?
    if (MovieModuleManager.getInstance().getSettings().isResetNewFlagOnUds()) {
      movieList.getMovies().forEach(movie -> movie.setNewlyAdded(false));
    }

    for (String ds : dataSources) {
      Path dsAsPath = Paths.get(ds);

      // check the special case, that the data source is also an ignore folder
      if (isInSkipFolder(dsAsPath)) {
        LOGGER.debug("datasource '{}' is also a skipfolder - skipping", ds);
        continue;
      }

      LOGGER.info("Start UDS on datasource: {}", ds);
      miTasks.clear();
      initThreadPool(3, "update");
      setTaskName(TmmResourceBundle.getString("update.datasource") + " '" + ds + "'");
      publishState();

      // first of all check if the DS is available; we can take the
      // Files.exist here:
      // if the DS exists (and we have access to read it): Files.exist = true
      if (!Files.exists(dsAsPath)) {
        // error - continue with next datasource
        MessageManager.instance
            .pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.unavailable", new String[] { ds }));
        continue;
      }

      publishState();

      // just check datasource folder, parse NEW folders first
      List<Path> newMovieDirs = new ArrayList<>();
      List<Path> existingMovieDirs = new ArrayList<>();
      List<Path> rootList = listFilesAndDirs(dsAsPath);

      LOGGER.debug("Found '{}' files/folders in the data source", rootList.size());

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

      Set<Path> rootFiles = new TreeSet<>(); // avoid duplicates
      for (Path path : rootList) {
        if (Files.isDirectory(path)) {
          String name = path.getFileName().toString();
          if (name.equalsIgnoreCase(MediaFileHelper.BDMV) || name.equalsIgnoreCase(MediaFileHelper.VIDEO_TS)
              || name.equalsIgnoreCase(MediaFileHelper.HVDVD_TS)) {
            // there cannot be a disc folder in root! Everything breaks...
            MessageManager.instance.pushMessage(
                new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.discfolderinroot", new String[] { path.toString() }));
            continue;
          }
          if (existingMovies.contains(path)) {
            existingMovieDirs.add(path);
          }
          else {
            newMovieDirs.add(path);
          }
        }
        else {
          rootFiles.add(path);
        }
      }
      rootList.clear();
      publishState();

      LOGGER.debug("Processing '{}' existing, '{}' new movies and '{}' root files", existingMovieDirs.size(), newMovieDirs.size(), rootFiles.size());

      for (Path path : newMovieDirs) {
        searchAndParse(dsAsPath.toAbsolutePath(), path, Integer.MAX_VALUE);
      }
      for (Path path : existingMovieDirs) {
        searchAndParse(dsAsPath.toAbsolutePath(), path, Integer.MAX_VALUE);
      }
      if (!rootFiles.isEmpty()) {
        submitTask(new ParseMultiMovieDirTask(dsAsPath.toAbsolutePath(), dsAsPath.toAbsolutePath(), new ArrayList<>(rootFiles)));
      }

      waitForCompletionOrCancel();

      // print stats
      LOGGER.info("FilesFound: {}", filesFound.size());
      LOGGER.info("moviesFound: {}", movieList.getMovieCount());
      LOGGER.debug("PreDir: {}", preDir);
      LOGGER.debug("PostDir: {}", postDir);
      LOGGER.debug("VisFile: {}", visFile);
      LOGGER.debug("PreDirAll: {}", preDirAll);
      LOGGER.debug("PostDirAll: {}", postDirAll);
      LOGGER.debug("VisFileAll: {}", visFileAll);

      newMovieDirs.clear();
      existingMovieDirs.clear();
      rootFiles.clear();

      if (cancel) {
        break;
      }

      // cleanup
      cleanup(ds);

      // map Kodi entries
      if (StringUtils.isNotBlank(Settings.getInstance().getKodiHost())) {
        // call async to avoid slowdown of UDS
        TmmTaskManager.getInstance().addUnnamedTask(() -> KodiRPC.getInstance().updateMovieMappings());
      }

      // mediainfo
      gatherMediainfo(ds);

      if (cancel) {
        break;
      }

      // build image cache on import
      if (Settings.getInstance().isImageCache() && MovieModuleManager.getInstance().getSettings().isBuildImageCacheOnImport()) {
        for (Movie movie : movieList.getMovies()) {
          if (!dsAsPath.equals(Paths.get(movie.getDataSource()))) {
            // check only movies matching datasource
            continue;
          }
          for (MediaFile imageFile : movie.getImagesToCache()) {
            // only cache if not cached already
            if (!ImageCache.isImageCached(imageFile.getFileAsPath())) {
              imageFiles.add(imageFile);
            }
          }
        }
      }
    } // END datasource loop
  }

  private void updateMovieSets() {
    if (StringUtils.isBlank(MovieModuleManager.getInstance().getSettings().getMovieSetDataFolder())) {
      return;
    }

    LOGGER.info("Start UDS for movie sets");

    Set<Path> movieSetFiles = getAllFilesRecursiveButNoDiscFiles(Paths.get(MovieModuleManager.getInstance().getSettings().getMovieSetDataFolder()));

    for (Path path : movieSetFiles) {
      if (FilenameUtils.isExtension(path.getFileName().toString(), "nfo")) {
        try {
          MovieSetNfoParser nfoParser = MovieSetNfoParser.parseNfo(path);
          MovieSet movieSet = nfoParser.toMovieSet();
          if (StringUtils.isBlank(movieSet.getTitle())) {
            // a movie set needs a title!
            movieSet.setTitle(FilenameUtils.getBaseName(path.getFileName().toString()));
          }

          // look if that movie set is already in our database
          MovieSet movieSetInDb = matchMovieSetInDb(path, movieSet);

          if (movieSetInDb != null) {
            // just add the media file if needed - if it is not locked
            if (!movieSetInDb.isLocked()) {
              MediaFile mf = new MediaFile(path);
              movieSetInDb.addToMediaFiles(mf);
              miTasks.add(new MovieMediaFileInformationFetcherTask(mf, movieSetInDb, true));
            }
          }
          else {
            // add this new one
            MediaFile mf = new MediaFile(path);
            movieSet.addToMediaFiles(mf);
            miTasks.add(new MovieMediaFileInformationFetcherTask(mf, movieSet, true));
            MovieModuleManager.getInstance().getMovieList().addMovieSet(movieSet);
          }
        }
        catch (Exception e) {
          LOGGER.debug("Could not parse movie set NFO '{}' - '{}'", path.getFileName(), e.getMessage());
        }
      }
    }
  }

  private MovieSet matchMovieSetInDb(Path nfoFile, MovieSet movieSetFromNfo) {
    List<MovieSet> existingMovieSets = MovieModuleManager.getInstance().getMovieList().getMovieSetList();

    // look if that movie set is already in our database

    // match by media file
    MediaFile foundNfo = new MediaFile(nfoFile);
    for (MovieSet movieSetInDb : existingMovieSets) {
      List<MediaFile> nfos = movieSetInDb.getMediaFiles(MediaFileType.NFO);
      if (nfos.contains(foundNfo)) {
        return movieSetInDb;
      }
    }

    // match by id
    int tmdbId = movieSetFromNfo.getTmdbId();
    if (tmdbId > 0) {
      for (MovieSet movieSetInDb : existingMovieSets) {
        int id = movieSetInDb.getTmdbId();
        if (id == tmdbId) {
          return movieSetInDb;
        }
      }
    }

    // match by title
    for (MovieSet movieSetInDb : existingMovieSets) {
      if (movieSetFromNfo.getTitle().equals(movieSetInDb.getTitle())) {
        return movieSetInDb;
      }
    }

    return null;
  }

  private void updateMovies() {
    LOGGER.info("Start UDS for selected movies");
    initThreadPool(3, "update");
    setTaskName(TmmResourceBundle.getString("update.datasource"));
    publishState();

    // get distinct data sources
    Set<String> movieDatasources = new HashSet<>();
    moviesToUpdate.stream().filter(movie -> !movie.isLocked()).forEach(movie -> movieDatasources.add(movie.getDataSource()));

    List<Movie> moviesToCleanup = new ArrayList<>();

    // update movies grouped by data source
    for (String ds : movieDatasources) {
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

      // no dupes b/c of possible MMD movies with same path
      Set<Path> movieDirs = new LinkedHashSet<>();
      for (Movie movie : moviesToUpdate) {
        if (!movie.getDataSource().equals(ds)) {
          continue;
        }

        movieDirs.add(movie.getPathNIO());
        moviesToCleanup.add(movie);

        // should we re-set all new flags?
        if (MovieModuleManager.getInstance().getSettings().isResetNewFlagOnUds()) {
          movie.setNewlyAdded(false);
        }
      }

      for (Path path : movieDirs) {
        submitTask(new FindMovieTask(path, Paths.get(ds)));
      }
    }

    waitForCompletionOrCancel();

    // print stats
    LOGGER.info("FilesFound: {}", filesFound.size());
    LOGGER.info("moviesFound: {}", movieList.getMovieCount());
    LOGGER.debug("PreDir: {}", preDir);
    LOGGER.debug("PostDir: {}", postDir);
    LOGGER.debug("VisFile: {}", visFile);
    LOGGER.debug("PreDirAll: {}", preDirAll);
    LOGGER.debug("PostDirAll: {}", postDirAll);
    LOGGER.debug("VisFileAll: {}", visFileAll);

    // cleanup
    cleanup(moviesToCleanup);

    // mediainfo
    gatherMediainfo(moviesToCleanup);
  }

  /**
   * ThreadpoolWorker to work off ONE possible movie from root datasource directory
   * 
   * @author Myron Boyle
   * @version 1.0
   */
  private class FindMovieTask implements Callable<Object> {

    private final Path subdir;
    private final Path datasource;
    private final long uniqueId;

    public FindMovieTask(Path subdir, Path datasource) {
      this.subdir = subdir;
      this.datasource = datasource;
      this.uniqueId = TmmTaskManager.getInstance().GLOB_THRD_CNT.incrementAndGet();
    }

    @Override
    public String call() {
      String name = Thread.currentThread().getName();
      if (!name.contains("-G")) {
        name = name + "-G0";
      }
      name = name.replaceAll("\\-G\\d+", "-G" + uniqueId);
      Thread.currentThread().setName(name);

      parseMovieDirectory(subdir, datasource);
      return subdir.toString();
    }
  }

  /**
   * ThreadpoolWorker just for spawning a MultiMovieDir parser directly
   * 
   * @author Myron Boyle
   * @version 1.0
   */
  private class ParseMultiMovieDirTask implements Callable<Object> {

    private final Path       movieDir;
    private final Path       datasource;
    private final List<Path> allFiles;

    public ParseMultiMovieDirTask(Path dataSource, Path movieDir, List<Path> allFiles) {
      this.datasource = dataSource;
      this.movieDir = movieDir;
      this.allFiles = allFiles;
    }

    @Override
    public String call() {
      createMultiMovieFromDir(datasource, movieDir, allFiles);
      return movieDir.toString();
    }
  }

  private void parseMovieDirectory(Path movieDir, Path dataSource) {
    LOGGER.trace("Parsing '{}'", movieDir);
    publishState(movieDir.toString());

    List<Path> movieDirList = listFilesAndDirs(movieDir);
    List<Path> files = new ArrayList<>();
    Set<String> normalizedVideoFiles = new HashSet<>(); // just for identifying MMD

    boolean isDiscFolder = false;
    boolean isMultiMovieDir = false;
    boolean videoFileFound = false;
    Path movieRoot = movieDir; // root set to current dir - might be adjusted by disc folders

    for (Path path : movieDirList) {
      LOGGER.trace("found file {}", path);
      if (Utils.isRegularFile(path)) {
        files.add(path.toAbsolutePath());

        // do not construct a fully MF yet
        // just minimal to get the type out of filename
        MediaFile mf = new MediaFile();
        mf.setPath(path.getParent().toString());
        mf.setFilename(path.getFileName().toString());
        mf.setType(MediaFileHelper.parseMediaFileType(path, movieDir));
        LOGGER.trace("identified as {}", mf.getType());

        if (mf.getType() == MediaFileType.VIDEO) {
          videoFileFound = true;
          if (mf.isDiscFile()) {
            isDiscFolder = true;
            break; // step out - this is all we need to know
          }
          else {
            // detect unique basename, without stacking etc
            String basename = FilenameUtils.getBaseName(mf.getFilename());
            String stack = Utils.getStackingMarker(mf.getFilename()); // need extension for check
            if (!stack.isEmpty()) {
              // we are a video file, and therefore the stacking marker MUST be at end, to be removed. NOT in the middle!
              if (basename.endsWith(stack)) {
                LOGGER.trace("Found stacking marker {} inf file {} - removing", stack, mf.getFilename());
                basename = FilenameUtils.getBaseName(Utils.cleanStackingMarkers(mf.getFilename()));
              }
            }
            if (basename.isEmpty()) {
              // happens when having only a "dics1.iso" file
              basename = "empty"; // to at least have on entry!
            }
            normalizedVideoFiles.add(basename);
          }
        }
      }
      else {
        String name = path.getFileName().toString();
        // disc folders?
        if (name.equalsIgnoreCase(MediaFileHelper.BDMV) || name.equalsIgnoreCase(MediaFileHelper.VIDEO_TS)
            || name.equalsIgnoreCase(MediaFileHelper.HVDVD_TS)) {
          videoFileFound = true;
          isDiscFolder = true;
        }
      }
    }

    if (!videoFileFound) {
      // hmm... we never found a video file (but maybe others, trailers) so NO need to parse THIS folder
      LOGGER.trace("hmm - we found no video file in this folder: '{}'", movieDir);
      return;
    }

    if (isDiscFolder) {
      // if inside own DiscFolder, walk backwards till movieRoot folder
      // BUT NOT IF DATASOURCE!
      Path relative = dataSource.relativize(movieDir);
      String folder = relative.toString().toUpperCase(Locale.ROOT); // relative
      while (relative.getNameCount() > 1 && (folder.contains(VIDEO_TS) || folder.contains(BDMV) || folder.contains(HVDVD_TS))) {
        movieDir = movieDir.getParent();
        relative = dataSource.relativize(movieDir);
        folder = relative.toString().toUpperCase(Locale.ROOT);
      }
      movieRoot = movieDir;
    }
    else {
      // no VIDEO files in this dir - skip this folder
      if (normalizedVideoFiles.isEmpty()) {
        return;
      }
      // more than one (unstacked) movie file in directory (or DS root) -> must parsed as multiMovieDir
      if (normalizedVideoFiles.size() > 1 || movieDir.equals(dataSource)) {
        isMultiMovieDir = true;
      }
    }

    if (cancel) {
      return;
    }

    // ok, we're ready to parse :)
    if (isMultiMovieDir) {
      createMultiMovieFromDir(dataSource, movieRoot, files);
    }
    else {
      createSingleMovieFromDir(dataSource, movieRoot, isDiscFolder);
    }
  }

  /**
   * Parses ALL NFO MFs (merged together) and create a movie<br>
   *
   * @param mfs
   * @return Movie or NULL
   */
  public static Movie parseNFOs(List<MediaFile> mfs) {
    Movie movie = null;
    for (MediaFile mf : mfs) {

      if (mf.getType() == MediaFileType.NFO) {
        LOGGER.info("| parsing NFO {}", mf.getFileAsPath());
        Movie nfo = null;
        try {
          MovieNfoParser movieNfoParser = MovieNfoParser.parseNfo(mf.getFileAsPath());
          nfo = movieNfoParser.toMovie();
        }
        catch (Exception e) {
          LOGGER.warn("problem parsing NFO: {}", e.getMessage());
        }

        if (movie == null) {
          movie = (nfo == null) ? new Movie() : nfo;
        }
        else {
          movie.merge(nfo);
        }

        // was NFO, but parsing exception. try to find at least imdb id within
        if (movie.getImdbId().isEmpty() || movie.getTmdbId() == 0) {
          try {
            String content = Utils.readFileToString(mf.getFileAsPath());

            String imdb = ParserUtils.detectImdbId(content);
            if (movie.getImdbId().isEmpty() && !imdb.isEmpty()) {
              LOGGER.debug("| Found IMDB id: {}", imdb);
              movie.setImdbId(imdb);
            }

            String tmdb = StrgUtils.substr(content, "themoviedb\\.org\\/movie\\/(\\d+)");
            if (movie.getTmdbId() == 0 && !tmdb.isEmpty()) {
              LOGGER.debug("| Found TMDB id: {}", tmdb);
              movie.setTmdbId(MetadataUtil.parseInt(tmdb, 0));
            }
          }
          catch (IOException e) {
            LOGGER.warn("| couldn't read NFO {}", mf);
          }
        }
      } // end NFO
    } // end MFs

    for (MediaFile mf : mfs) {
      if (mf.getType() == MediaFileType.VSMETA) {
        if (movie == null) {
          movie = new Movie();
        }
        VSMeta vsmeta = new VSMeta(mf.getFileAsPath());
        vsmeta.parseFile();
        movie.merge(vsmeta.getMovie());
      }
    }

    // just try XML files too
    if (movie == null) {
      for (MediaFile mf : mfs) {
        if ("xml".equalsIgnoreCase(mf.getExtension())) {
          try {
            MovieNfoParser movieNfoParser = MovieNfoParser.parseNfo(mf.getFileAsPath());
            if (StringUtils.isNotBlank(movieNfoParser.title)) {
              movie = movieNfoParser.toMovie();
            }
          }
          catch (Exception e) {
            // ignored
          }
        }
      }
    }

    return movie;
  }

  /**
   * for SingleMovie or DiscFolders
   *
   * @param dataSource
   *          the data source
   * @param movieDir
   *          the movie folder
   * @param isDiscFolder
   *          is the movie in a disc folder?
   */
  private void createSingleMovieFromDir(Path dataSource, Path movieDir, boolean isDiscFolder) {
    LOGGER.debug("Parsing single movie directory: {}, (are we a disc folder? {})", movieDir, isDiscFolder);

    Path relative = dataSource.relativize(movieDir);
    // STACKED FOLDERS - go up ONE level (only when the stacked folder ==
    // stacking marker)
    // movie/CD1/ & /movie/CD2 -> go up
    // movie CD1/ & /movie CD2 -> NO - there could be other files/folders there

    if (!Utils.getFolderStackingMarker(relative.toString()).isEmpty()
        && Utils.getFolderStackingMarker(relative.toString()).equals(movieDir.getFileName().toString())) {
      movieDir = movieDir.getParent();
    }

    Movie movie = movieList.getMovieByPath(movieDir);
    if (movie != null && movie.isLocked()) {
      LOGGER.info("movie '{}' found in uds, but is locked", movie.getPath());
      return;
    }

    Set<Path> allFiles = getAllFilesRecursiveButNoDiscFiles(movieDir);
    fileLock.writeLock().lock();
    filesFound.add(movieDir.toAbsolutePath()); // our global cache
    filesFound.addAll(allFiles); // our global cache
    fileLock.writeLock().unlock();

    // convert to MFs (we need it anyways at the end)
    List<MediaFile> mfs = new ArrayList<>();
    for (Path file : allFiles) {
      mfs.add(new MediaFile(file));
    }

    // ***************************************************************
    // first round - try to parse NFO(s) first
    // ***************************************************************
    if (movie == null) {
      LOGGER.debug("| movie not found; looking for NFOs");
      movie = parseNFOs(mfs);
      if (movie == null) {
        movie = new Movie();
      }
      movie.setNewlyAdded(true);
    }
    movie.setDisc(isDiscFolder);

    // ***************************************************************
    // second round - try to parse additional files
    // ***************************************************************
    String bdinfoTitle = ""; // title parsed out of BDInfo
    String bdmtTitle = ""; // title parsed out of BDMV/META/DL/*.xml
    String videoName = ""; // title from file
    for (MediaFile mf : mfs) {
      if (mf.getType() == MediaFileType.TEXT) {
        try {
          String txtFile = Utils.readFileToString(mf.getFileAsPath());

          String bdinfo = StrgUtils.substr(txtFile, ".*Disc Title:\\s+(.*?)[\\n\\r]");
          if (!bdinfo.isEmpty()) {
            LOGGER.debug("| Found Disc Title in BDInfo.txt: {}", bdinfo);
            bdinfoTitle = WordUtils.capitalizeFully(bdinfo);
          }

          String imdb = ParserUtils.detectImdbId(txtFile);
          if (movie.getImdbId().isEmpty() && !imdb.isEmpty()) {
            LOGGER.debug("| Found IMDB id: {}", imdb);
            movie.setImdbId(imdb);
          }
        }
        catch (Exception e) {
          LOGGER.debug("| couldn't read TXT {}", mf.getFilename());
        }
      }
      else if (mf.getType() == MediaFileType.VIDEO) {
        videoName = mf.getBasename();
      }
    }
    if (isDiscFolder) {
      // check for Bluray title info on disc
      // have to do this direct, since we no longer get all disc MFs...
      Path bdmt = movieDir.resolve("BDMV/META/DL/bdmt_eng.xml");
      if (Files.exists(bdmt)) {
        try {
          // cannot parse XML with such weird namespaces - use the "simple" method
          String xml = Utils.readFileToString(bdmt);
          String name = StrgUtils.substr(xml, "di:name\\>(.*?)\\<");
          if (!name.isEmpty()) {
            LOGGER.debug("| Found Disc Title in Bluray META folder: {}", name);
            bdmtTitle = name;
          }
        }
        catch (IOException e) {
          // ignore
        }
      }
      videoName = movieDir.getFileName().toString();
    }

    if (movie.getTitle().isEmpty()) {
      String[] video = null;
      // Fact: we came into this method, because only one video file is in this folder.
      // Fact: for single folder movies, we usually take the folder name as title
      // BUT: when having such file in some structure like /A/, /2010/, decade or genre folder
      // it is better to take the filename. And it has to be threaten as MMD
      // Known issues: movies folders like "2012" are now also being imported with filename
      // CAUTION: also check MovieList.reevaluateMMD(), IF the logic changes here!
      if (movieDir.getFileName().toString().matches(FOLDER_STRUCTURE) || MediaGenres.containsGenre(movieDir.getFileName().toString())) {
        createMultiMovieFromDir(dataSource, movieDir, new ArrayList<>(allFiles));
        return;
      }
      else {
        video = ParserUtils.detectCleanTitleAndYear(movieDir.getFileName().toString(), MovieModuleManager.getInstance().getSettings().getBadWord());
      }

      // get the "cleaner" name/year combo from
      movie.setTitle(Utils.removeSortableName(video[0]));
      if (!video[1].isEmpty()) {
        try {
          movie.setYear(Integer.parseInt(video[1]));
        }
        catch (Exception ignored) {
          // nothing to be done here
        }
      }

      // overwrite title from within Bluray (trust the authoring more than the folder)
      if (StringUtils.isNotBlank(bdmtTitle)) {
        video = ParserUtils.detectCleanTitleAndYear(bdmtTitle, MovieModuleManager.getInstance().getSettings().getBadWord());
        if (!video[0].isEmpty()) {
          movie.setTitle(Utils.removeSortableName(video[0]));
        }
        if (!video[1].isEmpty()) {
          try {
            movie.setYear(Integer.parseInt(video[1]));
          }
          catch (Exception ignored) {
            // nothing to be done here
          }
        }
      }
    }

    allFiles.clear(); // moved down here, since we need that for MMD detection

    if (StringUtils.isBlank(movie.getTitle()) && StringUtils.isNotBlank(bdinfoTitle)) {
      movie.setTitle(bdinfoTitle);
    }
    else if (StringUtils.isBlank(movie.getTitle())) {
      // .45 for ex
      movie.setTitle(Utils.removeSortableName(videoName));
    }

    movie.setPath(movieDir.toAbsolutePath().toString());
    movie.setDataSource(dataSource.toString());

    // ***************************************************************
    // third round - check for UNKNOWN, if they match a video file name - we might keep them
    // ***************************************************************
    for (MediaFile mf : getMediaFiles(mfs, MediaFileType.UNKNOWN)) {
      for (MediaFile vid : getMediaFiles(mfs, MediaFileType.VIDEO)) {
        if (mf.getFilename().startsWith(vid.getFilename())) {
          mf.setType(MediaFileType.DOUBLE_EXT);
        }
      }
    }

    // ***************************************************************
    // fourth round - now add all the other known files
    // ***************************************************************
    addMediafilesToMovie(movie, mfs);

    // ***************************************************************
    // fifth round - try to match unknown graphics like title.ext or
    // filename.ext as poster
    // ***************************************************************
    for (MediaFile mf : mfs) {
      if (mf.getType() == MediaFileType.GRAPHIC) {
        LOGGER.debug("| parsing unknown graphic: {}", mf.getFilename());
        List<MediaFile> vid = movie.getMediaFiles(MediaFileType.VIDEO);
        if (vid != null && !vid.isEmpty()) {
          String vfilename = vid.get(0).getFilename();
          if (FilenameUtils.getBaseName(vfilename).equals(FilenameUtils.getBaseName(mf.getFilename())) // basename match
              // basename w/o stacking
              || FilenameUtils.getBaseName(Utils.cleanStackingMarkers(vfilename)).strip().equals(FilenameUtils.getBaseName(mf.getFilename()))
              || movie.getTitle().equals(FilenameUtils.getBaseName(mf.getFilename()))) { // title match
            mf.setType(POSTER);
            movie.addToMediaFiles(mf);
          }
        }
      }
    }

    // ***************************************************************
    // sixth round - remove files which are not here any more
    // ***************************************************************
    boolean videoRemoved = false;

    for (MediaFile mediaFile : movie.getMediaFiles()) {
      if (!Files.exists(mediaFile.getFile(), LinkOption.NOFOLLOW_LINKS)) {
        if (mediaFile.getType() == MediaFileType.VIDEO) {
          videoRemoved = true;
        }
        movie.removeFromMediaFiles(mediaFile);
      }
    }

    // VIDEO file exchanged - treat this movie as new
    if (videoRemoved && !movie.getMediaFiles(MediaFileType.VIDEO).isEmpty()) {
      movie.setNewlyAdded(true);
    }

    // set the 3D flag/edition/source from the file/folder name ONLY at first import
    if (movie.isNewlyAdded()) {
      // if the String 3D is in the movie dir, assume it is a 3D movie
      Matcher matcher = VIDEO_3D_PATTERN.matcher(movieDir.getFileName().toString());
      if (matcher.find()) {
        movie.setVideoIn3D(true);
      }
      // same for first video file; not necessarily the main file, but we have no file size yet to determine...
      MediaFile vid = getMediaFile(mfs, MediaFileType.VIDEO);
      if (vid != null) {
        matcher = VIDEO_3D_PATTERN.matcher(vid.getFilename());
        if (matcher.find()) {
          movie.setVideoIn3D(true);
        }

        // remember the filename the first time the movie gets added to tmm
        if (StringUtils.isBlank(movie.getOriginalFilename())) {
          movie.setOriginalFilename(vid.getFilename());
        }

        // if the new file has a source identifier in its filename, update the source too (but only if there is noone yet)
        if (movie.getMediaSource() == MediaSource.UNKNOWN) {
          MediaSource newSource = MediaSource.parseMediaSource(Utils.relPath(movie.getDataSource(), vid.getFileAsPath().toString()));
          if (newSource != MediaSource.UNKNOWN) {
            // source has been found in the filename - update
            movie.setMediaSource(newSource);
          }
        }
      }

      // get edition from name if no edition has been set via NFO
      if (movie.getEdition() == MovieEdition.NONE) {
        movie.setEdition(MovieEdition.getMovieEditionFromString(movieDir.getFileName().toString()));
      }
    }

    // ***************************************************************
    // check if that movie is an offline movie
    // ***************************************************************
    boolean isOffline = false;
    boolean videoAvailable = false;
    for (MediaFile mf : movie.getMediaFiles(MediaFileType.VIDEO)) {
      videoAvailable = true;
      if ("disc".equalsIgnoreCase(mf.getExtension())) {
        isOffline = true;
      }
    }
    if (videoAvailable) {
      LOGGER.debug("| store movie into DB as: {}", movie.getTitle());
      movieList.addMovie(movie);

      movie.setOffline(isOffline);
      movie.reEvaluateDiscfolder();
      movie.reEvaluateStacking();
      movie.saveToDb();
    }
    else {
      LOGGER.error("could not add '{}' because no VIDEO file found", movieDir);
      return;
    }

    // if there is missing artwork AND we do have a VSMETA file, we probably can extract an artwork from there
    if (MovieModuleManager.getInstance().getSettings().isExtractArtworkFromVsmeta()) {
      List<MediaFile> vsmetas = movie.getMediaFiles(MediaFileType.VSMETA);

      if (movie.getMediaFiles(POSTER).isEmpty() && !vsmetas.isEmpty()
          && !MovieModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty()) {
        LOGGER.debug("extracting POSTERs from VSMETA for {}", movie.getMainFile().getFileAsPath());
        MovieArtworkHelper.extractArtworkFromVsmeta(movie, vsmetas.get(0), MediaArtwork.MediaArtworkType.POSTER);
      }
      if (movie.getMediaFiles(FANART).isEmpty() && !vsmetas.isEmpty()
          && !MovieModuleManager.getInstance().getSettings().getFanartFilenames().isEmpty()) {
        LOGGER.debug("extracting FANARTs from VSMETA for {}", movie.getMainFile().getFileAsPath());
        MovieArtworkHelper.extractArtworkFromVsmeta(movie, vsmetas.get(0), MediaArtwork.MediaArtworkType.BACKGROUND);
      }

      // Check if Bluray, and if it has some inlined posters
      if (movie.getMediaFiles(POSTER).isEmpty()) {
        MovieArtworkHelper.extractBlurayPosters(movie);
      }
    }

    // last but not least attach it to the movie set (this is the last step - otherwise the movie set gets too much events)
    if (movie.getMovieSet() != null) {
      LOGGER.debug("| movie is part of a movieset");
      movie.getMovieSet().insertMovie(movie);
      movie.getMovieSet().saveToDb();
    }
  }

  /**
   * more than one movie in dir? Then use that with already known files
   * 
   * @param dataSource
   *          the data source
   * @param movieDir
   *          the movie folder
   * @param allFiles
   *          just use this files, do not list again
   */
  private void createMultiMovieFromDir(Path dataSource, Path movieDir, List<Path> allFiles) {
    LOGGER.debug("Parsing multi  movie directory: {}", movieDir); // double space is for log alignment ;)
    publishState(movieDir.toString());

    List<Movie> movies = movieList.getMoviesByPath(movieDir);

    fileLock.writeLock().lock();
    filesFound.add(movieDir); // our global cache
    filesFound.addAll(allFiles); // our global cache
    fileLock.writeLock().unlock();

    // convert to MFs
    ArrayList<MediaFile> mfs = new ArrayList<>();
    for (Path file : allFiles) {
      mfs.add(new MediaFile(file));
    }
    // allFiles.clear(); // might come handy

    // just compare filename length, start with longest b/c of overlapping names
    mfs.sort((file1, file2) -> file2.getFileAsPath().getFileName().toString().length() - file1.getFileAsPath().getFileName().toString().length());

    for (MediaFile mf : getMediaFiles(mfs, MediaFileType.VIDEO)) {
      Movie movie = null;

      String basename = FilenameUtils.getBaseName(mf.getFilename());
      String stack = Utils.getStackingMarker(mf.getFilename()); // need extension for check
      if (!stack.isEmpty()) {
        LOGGER.trace("Found stacking marker {} inf file {} - removing", stack, mf.getFilename());
        // we are a video file, and therefore the stacking marker MUST be at end, to be removed. NOT in the middle!
        if (basename.endsWith(stack)) {
          basename = FilenameUtils.getBaseName(Utils.cleanStackingMarkers(mf.getFilename()));
        }
      }

      // get all MFs with same basename
      List<MediaFile> sameName = new ArrayList<>();
      LOGGER.trace("UDS: basename: {}", basename);
      for (MediaFile sm : mfs) {
        String smBasename = FilenameUtils.getBaseName(sm.getFilename());
        String smNameRegexp = Pattern.quote(basename) + "[\\s.,_-].*";
        if (smBasename.equals(basename) || smBasename.matches(smNameRegexp)) {
          if (sm.getType() == MediaFileType.GRAPHIC) {
            // same named graphics (unknown, not detected without postfix) treated as posters
            sm.setType(POSTER);
          }
          sameName.add(sm);
          LOGGER.trace("UDS: found matching MF: {}", sm);
        }
      }

      // 1) check if MF is already assigned to a movie within path
      for (Movie m : movies) {
        for (MediaFile mfile : m.getMediaFiles(MediaFileType.VIDEO)) {
          if (mfile.equals(mfs) || mfile.getBasename().equalsIgnoreCase(mf.getBasename())) {
            // ok, our MF is already in an movie
            LOGGER.debug("| found movie '{}' from MediaFile {}", m.getTitle(), mf);
            movie = m;
          }
        }
        // NOPE - the cleaned filename might be found - but might be a different version!!
        // so only match strictly with filename above, not loose with clean name
        // for (MediaFile mfile : m.getMediaFiles(MediaFileType.VIDEO)) {
        // // try to match like if we would create a new movie
        // String[] mfileTY = ParserUtils.detectCleanMovienameAndYear(FilenameUtils.getBaseName(Utils.cleanStackingMarkers(mfile.getFilename())));
        // String[] mfTY = ParserUtils.detectCleanMovienameAndYear(FilenameUtils.getBaseName(Utils.cleanStackingMarkers(mf.getFilename())));
        // if (mfileTY[0].equals(mfTY[0]) && mfileTY[1].equals(mfTY[1])) { // title AND year (even empty) match
        // LOGGER.debug("| found possible movie '" + m.getTitle() + "' from filename " + mf);
        // movie = m;
        // break;
        // }
        // }
      }

      if (movie != null && movie.isLocked()) {
        LOGGER.info("movie '{}' found in uds, but is locked", movie.getPath());
        continue;
      }

      if (movie == null) {
        // 2) create if not found
        movie = parseNFOs(sameName);

        if (movie == null) {
          // still NULL, create new movie movie from file
          LOGGER.debug("| Create new movie from file: {}", mf);
          movie = new Movie();
          String[] ty = ParserUtils.detectCleanTitleAndYear(basename, MovieModuleManager.getInstance().getSettings().getBadWord());
          movie.setTitle(ty[0]);
          if (!ty[1].isEmpty()) {
            try {
              movie.setYear(Integer.parseInt(ty[1]));
            }
            catch (Exception ignored) {
              // nothing to do
            }
          }
          // get edition from name
          movie.setEdition(MovieEdition.getMovieEditionFromString(basename));

          // if the String 3D is in the movie file name, assume it is a 3D movie
          Matcher matcher = VIDEO_3D_PATTERN.matcher(basename);
          if (matcher.find()) {
            movie.setVideoIn3D(true);
          }
        }
        movie.setDataSource(dataSource.toString());
        movie.setNewlyAdded(true);
        movie.setPath(mf.getPath());

        // remember the filename the first time the movie gets added to tmm
        if (StringUtils.isBlank(movie.getOriginalFilename())) {
          movie.setOriginalFilename(mf.getFilename());
        }

        movies.add(movie); // add to our cached copy
      }

      // try to parse the imdb id from the filename
      if (!MediaIdUtil.isValidImdbId(movie.getImdbId())) {
        movie.setImdbId(ParserUtils.detectImdbId(mf.getFileAsPath().toString()));
      }
      // try to parse the Tmdb id from the filename
      if (movie.getTmdbId() == 0) {
        movie.setTmdbId(ParserUtils.detectTmdbId(mf.getFileAsPath().toString()));
      }
      if (movie.getMediaSource() == MediaSource.UNKNOWN) {
        movie.setMediaSource(MediaSource.parseMediaSource(Utils.relPath(movie.getDataSource(), mf.getFileAsPath().toString())));
      }
      LOGGER.debug("| parsing video file {}", mf.getFilename());
      movie.setMultiMovieDir(true);

      // ***************************************************************
      // third round - check for UNKNOWN, if they match a video file name - we might keep them
      // ***************************************************************
      for (MediaFile unk : getMediaFiles(sameName, MediaFileType.UNKNOWN)) {
        for (MediaFile vid : getMediaFiles(mfs, MediaFileType.VIDEO)) {
          if (unk.getFilename().startsWith(vid.getFilename())) {
            unk.setType(MediaFileType.DOUBLE_EXT);
          }
        }
      }
      addMediafilesToMovie(movie, sameName);
      mfs.removeAll(sameName);

      // ***************************************************************
      // fifth round - remove files which are not here any more
      // ***************************************************************
      for (MediaFile mediaFile : movie.getMediaFiles()) {
        if (!Files.exists(mediaFile.getFile())) {
          movie.removeFromMediaFiles(mediaFile);
        }
      }

      // check if that movie is an offline movie
      boolean isOffline = false;
      for (MediaFile mediaFiles : movie.getMediaFiles(MediaFileType.VIDEO)) {
        if ("disc".equalsIgnoreCase(mediaFiles.getExtension())) {
          isOffline = true;
        }
      }
      movie.setOffline(isOffline);

      if (movie.getMovieSet() != null) {
        LOGGER.debug("| movie is part of a movieset");
        movie.getMovieSet().insertMovie(movie);
        movie.getMovieSet().saveToDb();
      }

      // if there is missing artwork AND we do have a VSMETA file, we probably can extract an artwork from there
      if (MovieModuleManager.getInstance().getSettings().isExtractArtworkFromVsmeta()) {
        List<MediaFile> vsmetas = movie.getMediaFiles(MediaFileType.VSMETA);

        if (movie.getMediaFiles(POSTER).isEmpty() && !vsmetas.isEmpty()
            && !MovieModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty()) {
          LOGGER.debug("extracting POSTERs from VSMETA for {}", movie.getMainFile().getFileAsPath());
          MovieArtworkHelper.extractArtworkFromVsmeta(movie, vsmetas.get(0), MediaArtwork.MediaArtworkType.POSTER);
        }
        if (movie.getMediaFiles(FANART).isEmpty() && !vsmetas.isEmpty()
            && !MovieModuleManager.getInstance().getSettings().getFanartFilenames().isEmpty()) {
          LOGGER.debug("extracting FANARTs from VSMETA for {}", movie.getMainFile().getFileAsPath());
          MovieArtworkHelper.extractArtworkFromVsmeta(movie, vsmetas.get(0), MediaArtwork.MediaArtworkType.BACKGROUND);
        }
      }

      movieList.addMovie(movie);
      movie.saveToDb();
    } // end foreach VIDEO MF

    // check stacking on all movie from this dir (it might have changed!)
    for (Movie m : movieList.getMoviesByPath(movieDir)) {
      m.reEvaluateDiscfolder();
      m.reEvaluateStacking();
      m.saveToDb();
    }
  }

  private void addMediafilesToMovie(Movie movie, List<MediaFile> mediaFiles) {
    List<MediaFile> current = new ArrayList<>(movie.getMediaFiles());

    // sort the given media files to bring the video to the front
    mediaFiles.sort(new MovieMediaFileComparator());

    // remember the first video file (existing first)
    MediaFile mainVideoFile = null;

    if (!movie.getMediaFiles(VIDEO).isEmpty()) {
      mainVideoFile = movie.getMainVideoFile();
    }

    for (MediaFile mf : mediaFiles) {
      if (!current.contains(mf)) { // a new mediafile was found!
        if (mf.getPath().toUpperCase(Locale.ROOT).contains("BDMV") || mf.getPath().toUpperCase(Locale.ROOT).contains("VIDEO_TS")
            || mf.getPath().toUpperCase(Locale.ROOT).contains("HVDVD_TS") || mf.isDiscFile()) {
          if (movie.getMediaSource() == MediaSource.UNKNOWN) {
            String folderNameComplete = Paths.get(movie.getPath()).relativize(mf.getFileAsPath()).toString();
            movie.setMediaSource(MediaSource.parseMediaSource(folderNameComplete));
          }
        }
        // try to parse the imdb id from the filename
        if (!MediaIdUtil.isValidImdbId(movie.getImdbId())) {
          movie.setImdbId(ParserUtils.detectImdbId(mf.getFilename()));
        }
        // try to parse the tmdb id from the filename
        if (movie.getTmdbId() == 0) {
          movie.setTmdbId(ParserUtils.detectTmdbId(mf.getFilename()));
        }

        LOGGER.debug("| parsing {} {}", mf.getType().name(), mf.getFileAsPath());
        switch (mf.getType()) {
          case VIDEO:
            if (mainVideoFile == null) {
              mainVideoFile = mf;
            }
            movie.addToMediaFiles(mf);
            break;

          case SUBTITLE:
            if (!mf.isPacked()) {
              movie.addToMediaFiles(mf);
            }
            break;

          case FANART:
            if (mf.getPath().toLowerCase(Locale.ROOT).contains("extrafanart")) {
              // there shouldn't be any files here
              LOGGER.warn("problem: detected media file type FANART in extrafanart folder: {}", mf.getPath());
              continue;
            }

            // if the fanart ends with -fanart, but does not start with the same base name, we recategorize it as graphic
            if (hasInvalidBasename(mainVideoFile, mf, "fanart")) {
              mf.setType(MediaFileType.GRAPHIC);
            }

            movie.addToMediaFiles(mf);
            break;

          case THUMB:
            if (mf.getPath().toLowerCase(Locale.ROOT).contains("extrathumbs")) { //
              // there shouldn't be any files here
              LOGGER.warn("| problem: detected media file type THUMB in extrathumbs folder: {}", mf.getPath());
              continue;
            }

            // if the thumb ends with -thumb, but does not start with the same base name, we recategorize it as graphic
            if (hasInvalidBasename(mainVideoFile, mf, "thumb")) {
              mf.setType(MediaFileType.GRAPHIC);
            }

            movie.addToMediaFiles(mf);
            break;

          case POSTER:
          case BANNER:
          case CLEARLOGO:
          case LOGO:
          case CLEARART:
          case KEYART:
            // if the artwork ends with -<type>, but does not start with the same base name, we recategorize it as graphic
            if (hasInvalidBasename(mainVideoFile, mf, mf.getType().name().toLowerCase(Locale.ROOT))) {
              mf.setType(MediaFileType.GRAPHIC);
            }

            movie.addToMediaFiles(mf);
            break;

          case DISC:
            // if the artwork ends with -disc or -discart, but does not start with the same base name, we recategorize it as graphic
            if (hasInvalidBasename(mainVideoFile, mf, "disc") || hasInvalidBasename(mainVideoFile, mf, "discart")) {
              mf.setType(MediaFileType.GRAPHIC);
            }

            movie.addToMediaFiles(mf);
            break;

          case TRAILER:
          case EXTRA:
          case SAMPLE:
          case NFO:
          case TEXT:
          case EXTRAFANART:
          case EXTRATHUMB:
          case AUDIO:
          case MEDIAINFO:
          case VSMETA:
          case THEME:
          case CHARACTERART:
          case DOUBLE_EXT:
            movie.addToMediaFiles(mf);
            break;

          case GRAPHIC:
          case UNKNOWN:
          case SEASON_POSTER:
          case SEASON_FANART:
          case SEASON_BANNER:
          case SEASON_THUMB:
          case VIDEO_EXTRA:
          default:
            LOGGER.debug("| NOT adding unknown media file type: {}", mf.getFileAsPath());
            // movie.addToMediaFiles(mf); // DO NOT ADD UNKNOWN
            continue;
        } // end switch type

        // debug
        if (mf.getType() != MediaFileType.GRAPHIC && mf.getType() != MediaFileType.UNKNOWN && mf.getType() != MediaFileType.NFO
            && !movie.getMediaFiles().contains(mf)) {
          LOGGER.warn("| Movie not added mf: {}", mf.getFileAsPath());
        }

      } // end new MF found
    } // end MF loop

    // second loop: check if there is at least one type of every artwork. If not, try to re-parse GRAPHIC MFs less strictly
    for (MediaFileType type : Arrays.asList(FANART, POSTER, BANNER, CLEARLOGO, LOGO, CLEARART, KEYART, DISC)) {
      if (movie.getMediaFiles(type).isEmpty()) {
        for (MediaFile mf : movie.getMediaFiles(GRAPHIC)) {
          // re-evaluate the mf type
          MediaFileType parsedType = MediaFileHelper.parseImageType(mf.getFile());
          if (parsedType == type) {
            mf.setType(type);
            break;
          }
        }
      }
    }
  }

  private boolean hasInvalidBasename(MediaFile mainVideoFile, MediaFile toCheck, String suffix) {
    if (mainVideoFile == null) {
      return false;
    }

    String toCheckBasename = FilenameUtils.getBaseName(toCheck.getFilename());

    if (!toCheckBasename.endsWith("-" + suffix)) {
      return false;
    }

    toCheckBasename = toCheckBasename.replaceAll("-" + suffix + "$", "");

    String mainVideoFileBasename = FilenameUtils.getBaseName(mainVideoFile.getFilename());

    if (!toCheckBasename.equals(mainVideoFileBasename)) {
      return true;
    }

    return false;
  }

  /*
   * cleanup database - remove orphaned movies/files
   */
  private void cleanup(String datasource) {
    List<Movie> moviesToclean = new ArrayList<>();
    for (Movie movie : movieList.getMovies()) {
      // check only movies matching datasource
      if (!Paths.get(datasource).equals(Paths.get(movie.getDataSource()))) {
        continue;
      }
      moviesToclean.add(movie);
    }
    cleanup(moviesToclean);
  }

  private void cleanup(List<Movie> movies) {
    setTaskName(TmmResourceBundle.getString("update.cleanup"));
    setTaskDescription(null);
    setProgressDone(0);
    setWorkUnits(0);
    publishState();

    LOGGER.info("removing orphaned movies/files...");
    List<Movie> moviesToRemove = new ArrayList<>();
    for (int i = movies.size() - 1; i >= 0; i--) {
      if (cancel) {
        break;
      }

      Movie movie = movies.get(i);

      // do not process locked movies (because filesFound has not been filled for them)
      if (movie.isLocked()) {
        continue;
      }

      boolean dirty = false;

      Path movieDir = movie.getPathNIO();
      boolean dirFound = filesFound.contains(movieDir);

      if (!dirFound) {
        // dir is not in hashset - check with exists to be sure it is not here
        if (!Files.exists(movieDir)) {
          LOGGER.debug("movie directory '{}' not found, removing from DB...", movieDir);
          moviesToRemove.add(movie);
        }
        else {
          // can be; MMD and/or dir=DS root
          LOGGER.warn("dir {} not in hashset, but on hdd!", movieDir);
        }
      }

      // have a look if that movie has just been added -> so we don't need any
      // cleanup
      if (!movie.isNewlyAdded()) {
        // check and delete all not found MediaFiles
        List<MediaFile> mediaFiles = new ArrayList<>(movie.getMediaFiles());
        for (MediaFile mf : mediaFiles) {
          boolean fileFound = filesFound.contains(mf.getFileAsPath());

          if (!fileFound) {
            LOGGER.debug("removing orphaned file from DB: {}", mf.getFileAsPath());
            movie.removeFromMediaFiles(mf);
            // invalidate the image cache
            if (mf.isGraphic()) {
              ImageCache.invalidateCachedImage(mf);
            }
            dirty = true;
          }
        }

        if (dirty && !movie.getMediaFiles(MediaFileType.VIDEO).isEmpty()) {
          movie.saveToDb();
        }
      }

      if (movie.getMediaFiles(MediaFileType.VIDEO).isEmpty()) {
        LOGGER.debug("Movie ({}) without VIDEO files detected, removing from DB...", movie.getTitle());
        moviesToRemove.add(movie);
      }
    }
    movieList.removeMovies(moviesToRemove);
    moviesToRemove.clear();

    // cleanup disc folder - there needs to be only ONE video MF to make the struct work...
    for (Movie movie : movies) {
      boolean dirty = false;
      if (movie.isDisc()) {
        if (movie.getDataSource().equals(movie.getPath())) {
          // uh-oh some (disc) movie was in root dir - ignore from checking
          continue;
        }

        // If we are a disc movie, remove all other video MFs
        for (MediaFile mf : movie.getMediaFiles(MediaFileType.VIDEO)) {
          if (mf.getFilename().matches(DISC_FOLDER_REGEX) || mf.isDiscFile()) {
            continue;
          }
          else {
            LOGGER.warn("DISC folder detected - remove VIDEO {}", mf.getFileAsPath());
            movie.removeFromMediaFiles(mf);
            dirty = true;
          }
        }

        // remove all "movies", which have been additionally found inside disc folders
        for (Movie sub : movieList.getMovies()) {
          if (movie.equals(sub)) {
            continue; // do not remove self
          }
          if (sub.getPathNIO().startsWith(movie.getPathNIO())) {
            LOGGER.warn("Movie {} inside DISC folder of {} - removing", sub.getMainFile().getFileAsPath(), movie.getPath());
            moviesToRemove.add(sub);
          }
        }

        if (dirty && !movie.getMediaFiles(MediaFileType.VIDEO).isEmpty()) {
          movie.saveToDb();
        }
      }
    }

    movieList.removeMovies(moviesToRemove);
  }

  /*
   * gather mediainfo for ungathered movies
   */
  private void gatherMediainfo(String datasource) {
    // start MI
    setTaskName(TmmResourceBundle.getString("update.mediainfo"));
    publishState();

    initThreadPool(1, "mediainfo");

    LOGGER.info("getting Mediainfo...");

    // first insert all collected MI tasks
    for (Runnable task : miTasks) {
      submitTask(task);
    }

    // and now get all mediafile from the movies to gather
    for (Movie movie : movieList.getMovies()) {
      if (cancel) {
        break;
      }

      // check only movies matching datasource
      if (!Paths.get(datasource).equals(Paths.get(movie.getDataSource()))) {
        continue;
      }

      // do not process locked movies
      if (movie.isLocked()) {
        continue;
      }

      for (MediaFile mf : new ArrayList<>(movie.getMediaFiles())) {
        if (StringUtils.isBlank(mf.getContainerFormat())) {
          submitTask(new MovieMediaFileInformationFetcherTask(mf, movie, false));
        }
        else {
          // at least update the file dates
          if (MediaFileHelper.gatherFileInformation(mf)) {
            // okay, something changed with that movie file - force fetching mediainfo
            submitTask(new MovieMediaFileInformationFetcherTask(mf, movie, true));
          }
        }
      }
    }
    waitForCompletionOrCancel();
  }

  private void gatherMediainfo(List<Movie> movies) {
    // start MI
    setTaskName(TmmResourceBundle.getString("update.mediainfo"));
    publishState();

    initThreadPool(1, "mediainfo");

    LOGGER.info("getting Mediainfo...");
    for (Movie movie : movies) {
      if (cancel) {
        break;
      }

      // do not process locked movies
      if (movie.isLocked()) {
        continue;
      }

      for (MediaFile mf : new ArrayList<>(movie.getMediaFiles())) {
        if (StringUtils.isBlank(mf.getContainerFormat())) {
          submitTask(new MovieMediaFileInformationFetcherTask(mf, movie, false));
        }
        else {
          // did the file dates/size change?
          if (MediaFileHelper.gatherFileInformation(mf)) {
            // okay, something changed with that movie file - force fetching mediainfo and drop medianfo.xml
            movie.getMediaFiles(MediaFileType.MEDIAINFO).forEach(mediaFile -> {
              Utils.deleteFileSafely(mediaFile.getFileAsPath());
              movie.removeFromMediaFiles(mediaFile);
            });
            submitTask(new MovieMediaFileInformationFetcherTask(mf, movie, true));
          }
        }
      }

      // upgrade MediaSource to UHD bluray, if video format says so
      if (movie.getMediaSource() == MediaSource.BLURAY
          && movie.getMainVideoFile().getVideoDefinitionCategory().equals(MediaFileHelper.VIDEO_FORMAT_UHD)) {
        movie.setMediaSource(MediaSource.UHD_BLURAY);
        movie.saveToDb();
      }
    }
    waitForCompletionOrCancel();
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
    List<MediaFileType> mediaFileTypes = Arrays.asList(types);
    return mfs.stream().filter(mf -> mediaFileTypes.contains(mf.getType())).toList();
  }

  @Override
  public void callback(Object obj) {
    // do not publish task description here, because with different workers the
    // text is never right
    publishState(progressDone);
  }

  /**
   * simple NIO File.listFiles() replacement<br>
   * returns all files & folders in specified dir, filtering against our skip folders (NOT recursive)
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
          if (fileNames.contains(path.toAbsolutePath())) {
            throw new IOException("duplicate found: '" + path.getFileName() + "' - fall back to the alternate method");
          }
          fileNames.add(path.toAbsolutePath());
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("error on listFilesAndDirs", e);
      LOGGER.debug("falling back to the alternate coding");
      fileNames = listFilesAndDirs2(directory);
    }
    if (fileNames.isEmpty()) {
      LOGGER.warn("Tried to list {}, but it was empty?!", directory);
    }

    // return sorted
    Collections.sort(fileNames);

    return fileNames;
  }

  /**
   * simple NIO File.listFiles() replacement<br>
   * returns all files & folders in specified dir, filtering against our skip folders (NOT recursive)
   *
   * @param directory
   *          the folder to list the items for
   * @return list of files&folders
   */
  private List<Path> listFilesAndDirs2(Path directory) {
    List<Path> fileNames = new ArrayList<>();

    try (Stream<Path> directoryStream = Files.walk(directory, 1, FileVisitOption.FOLLOW_LINKS)) {
      List<Path> allElements = directoryStream.toList();
      for (Path path : allElements) {
        if (directory.toAbsolutePath().equals(path.toAbsolutePath())) {
          continue;
        }
        if (isInSkipFolder(path)) {
          LOGGER.debug("Skipping: {}", path);
        }
        else {
          fileNames.add(path.toAbsolutePath());
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

  /**
   * gets all files recursive, but doesn't list files inside a disc folder structure
   * 
   * @param folder
   * @return
   */
  private Set<Path> getAllFilesRecursiveButNoDiscFiles(Path folder) {
    folder = folder.toAbsolutePath();
    AllFilesRecursive visitor = new AllFilesRecursive();
    try {
      Files.walkFileTree(folder, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, visitor);
    }
    catch (IOException e) {
      // can not happen, since we have overridden visitFileFailed, which throws no exception ;)
    }
    return visitor.fFound;
  }

  private class AllFilesRecursive extends AbstractFileVisitor {
    private final Set<Path> fFound = new HashSet<>();
    private int             deep   = 0;

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

        // in a disc folder we only accept NFO (and trailer) files
        if (Utils.isRegularFile(attr) && path.matches(DISC_FOLDER_REGEX)) {
          if (FilenameUtils.getExtension(filename).equalsIgnoreCase("nfo") || FilenameUtils.getBaseName(filename).endsWith("trailer")) {
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
      deep++;

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
        // so a /ds/movie/BDMV/STREAM folder would skip, b/c parent is a known disc folder
        // unfortunately, if your datasource is named like that, it won't see the disc at all!
        // so the workaround is, to not execute this check on disc root (deep = 1)
        if (deep > 1 && dir.getParent() != null && dir.getParent().getFileName() != null
            && dir.getParent().getFileName().toString().matches(DISC_FOLDER_REGEX)) {
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
      deep--;

      return CONTINUE;
    }
  }

  // **************************************
  // gets all files recursive,
  // detects movieRootDir (in case of stacked/disc folder)
  // and starts parsing directory immediately
  // **************************************
  public void searchAndParse(Path datasource, Path folder, int deep) {
    folder = folder.toAbsolutePath();
    SearchAndParseVisitor visitor = new SearchAndParseVisitor(datasource);
    try {
      Files.walkFileTree(folder, EnumSet.of(FileVisitOption.FOLLOW_LINKS), deep, visitor);
    }
    catch (IOException e) {
      // can not happen, since we override visitFileFailed, which throws no exception ;)
    }
  }

  private class SearchAndParseVisitor extends AbstractFileVisitor {
    private final Path         datasource;
    private final List<String> unstackedRoot = new ArrayList<>(); // only for folderstacking
    private final Set<Path>    videofolders  = new HashSet<>();   // all found video folders
    private final Set<Path>    visited       = new HashSet<>();

    SearchAndParseVisitor(Path datasource) {
      this.datasource = datasource;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
      if (cancel) {
        return TERMINATE;
      }

      if (visited.contains(file)) {
        // already visited? must be some sort of endless-loop - maybe from recursive symlinks
        // --> ABORT
        LOGGER.debug("visiting already visited file '{}'", file.toAbsolutePath());
        return SKIP_SIBLINGS;
      }

      incVisFile();

      visited.add(file);

      try {
        if (Utils.isRegularFile(attr) && !file.getFileName().toString().matches(SKIP_REGEX)) {
          // check for video?
          if (Settings.getInstance().getVideoFileType().contains("." + FilenameUtils.getExtension(file.toString()).toLowerCase(Locale.ROOT))) {
            // check if file is a VIDEO type - only scan those folders (and not extras/trailer folders)!
            MediaFile mf = new MediaFile(file);
            if (mf.getType() == MediaFileType.VIDEO) {
              videofolders.add(file.getParent());
            }
            else {
              LOGGER.debug("no VIDEO (is {}) - do not parse {}", mf.getType(), file);
            }
          }
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

      if (visited.contains(dir)) {
        // already visited? must be some sort of endless-loop - maybe from recursive symlinks
        // --> ABORT
        LOGGER.debug("visiting already visited folder '{}'", dir.toAbsolutePath());
        return SKIP_SUBTREE;
      }

      incPreDir();

      String parent = "";
      if (!dir.equals(datasource) && !dir.getParent().equals(datasource)) {
        parent = dir.getParent().getFileName().toString().toUpperCase(Locale.ROOT); // skip all subdirs of disc folders
      }

      visited.add(dir);

      try {
        if (dir.getFileName() != null && (isInSkipFolder(dir) || containsSkipFile(dir) || parent.matches(DISC_FOLDER_REGEX))) {
          LOGGER.debug("Skipping dir: {}", dir);
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

      if (this.videofolders.contains(dir)) {
        boolean update = true;
        // quick fix for folder stacking
        // name = stacking marker & parent has already been processed - skip
        Path relative = datasource.relativize(dir);
        if (!Utils.getFolderStackingMarker(relative.toString()).isEmpty()
            && Utils.getFolderStackingMarker(relative.toString()).equals(dir.getFileName().toString())) {
          if (unstackedRoot.contains(dir.getParent().toString())) {
            update = false;
          }
          else {
            unstackedRoot.add(dir.getParent().toString());
          }
        }
        if (update) {
          // check if any existing movie has already the same (sub)dir
          // IF we already have a movie a level deeper, we HAVE TO treat this folder as MMD!
          // we always start to parse from deepest level down to root, so they should be all already populated
          for (Path sub : this.videofolders) {
            if (sub.equals(dir)) {
              continue; // don't check ourself ;)
            }
            if (sub.startsWith(dir)) {
              // ka-ching! parse this now as MMD and return
              List<Path> rootFiles = listFilesOnly(dir); // get all files and dirs
              submitTask(new ParseMultiMovieDirTask(datasource.toAbsolutePath(), dir, rootFiles));
              publishState();
              return CONTINUE;
            }
          }
          submitTask(new FindMovieTask(dir, datasource));
        }
      }
      else {
        LOGGER.trace("not containing any video files - '{}'", dir);
      }
      return CONTINUE;
    }

    /**
     * simple NIO File.listFiles() replacement<br>
     * returns ONLY regular files (NO folders, NO hidden) in specified dir, filtering against our badwords (NOT recursive)
     *
     * @param directory
     *          the folder to list the files for
     * @return list of files&folders
     */
    private List<Path> listFilesOnly(Path directory) {
      List<Path> fileNames = new ArrayList<>();
      try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
        for (Path path : directoryStream) {
          if (Utils.isRegularFile(path) || path.getFileName().toString().matches(DISC_FOLDER_REGEX)) {
            if (isInSkipFolder(path)) {
              LOGGER.debug("Skipping: {}", path);
            }
            else {
              fileNames.add(path.toAbsolutePath());
            }
          }
        }
      }
      catch (IOException e) {
        LOGGER.error("error on listFilesOnly: {}", e.getMessage());
      }

      // return sorted
      Collections.sort(fileNames);

      return fileNames;
    }
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
  private class MovieMediaFileInformationFetcherTask extends MediaFileInformationFetcherTask {
    public MovieMediaFileInformationFetcherTask(MediaFile mediaFile, MediaEntity mediaEntity, boolean forceUpdate) {
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
