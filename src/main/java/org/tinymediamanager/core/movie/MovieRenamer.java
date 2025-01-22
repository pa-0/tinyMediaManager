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
package org.tinymediamanager.core.movie;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.IFileNaming;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.LanguageStyle;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntityFilenameHistory;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.jmte.JmteUtils;
import org.tinymediamanager.core.jmte.NamedArrayRenderer;
import org.tinymediamanager.core.jmte.NamedArrayUniqueRenderer;
import org.tinymediamanager.core.jmte.NamedBitrateRenderer;
import org.tinymediamanager.core.jmte.NamedDateRenderer;
import org.tinymediamanager.core.jmte.NamedFilesizeRenderer;
import org.tinymediamanager.core.jmte.NamedFramerateRenderer;
import org.tinymediamanager.core.jmte.NamedLowerCaseRenderer;
import org.tinymediamanager.core.jmte.NamedNumberRenderer;
import org.tinymediamanager.core.jmte.NamedReplacementRenderer;
import org.tinymediamanager.core.jmte.NamedSplitRenderer;
import org.tinymediamanager.core.jmte.NamedTitleCaseRenderer;
import org.tinymediamanager.core.jmte.NamedUpperCaseRenderer;
import org.tinymediamanager.core.jmte.RegexpProcessor;
import org.tinymediamanager.core.jmte.TmmModelAdaptor;
import org.tinymediamanager.core.jmte.TmmOutputAppender;
import org.tinymediamanager.core.jmte.ZeroNumberRenderer;
import org.tinymediamanager.core.movie.connector.MovieConnectors;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.filenaming.MovieBannerNaming;
import org.tinymediamanager.core.movie.filenaming.MovieClearartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieClearlogoNaming;
import org.tinymediamanager.core.movie.filenaming.MovieDiscartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieExtraFanartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieFanartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieKeyartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieNfoNaming;
import org.tinymediamanager.core.movie.filenaming.MoviePosterNaming;
import org.tinymediamanager.core.movie.filenaming.MovieThumbNaming;
import org.tinymediamanager.core.movie.filenaming.MovieTrailerNaming;
import org.tinymediamanager.core.movie.jmte.MovieNamedFirstCharacterRenderer;
import org.tinymediamanager.core.movie.jmte.MovieNamedIndexOfMovieSetRenderer;
import org.tinymediamanager.core.movie.jmte.MovieNamedIndexOfMovieSetWithDummyRenderer;
import org.tinymediamanager.core.threading.ThreadUtils;
import org.tinymediamanager.scraper.util.StrgUtils;

import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.extended.ChainedNamedRenderer;
import com.floreysoft.jmte.message.DefaultErrorHandler;
import com.floreysoft.jmte.message.ErrorMessage;
import com.floreysoft.jmte.message.ParseException;
import com.floreysoft.jmte.message.ResourceBundleMessage;
import com.floreysoft.jmte.token.Token;

/**
 * The Class MovieRenamer.
 *
 * @author Manuel Laggner / Myron Boyle
 */
public class MovieRenamer {
  private static final Logger              LOGGER                      = LoggerFactory.getLogger(MovieRenamer.class);
  private static final List<String>        KNOWN_IMAGE_FILE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "bmp", "tbn", "gif", "webp");

  // to not use posix here
  private static final Pattern             TITLE_PATTERN               = Pattern.compile("\\$\\{.*?title.*?\\}", Pattern.CASE_INSENSITIVE);
  private static final Pattern             YEAR_ID_PATTERN             = Pattern.compile("\\$\\{.*?(year|imdb|tmdb).*?\\}", Pattern.CASE_INSENSITIVE);
  private static final Pattern             ORIGINAL_FILENAME_PATTERN   = Pattern.compile("\\$\\{.*?originalFilename.*?\\}", Pattern.CASE_INSENSITIVE);
  private static final Pattern             TRAILER_STACKING_PATTERN    = Pattern.compile(".*?(\\d)$");

  private static final Map<String, String> TOKEN_MAP                   = createTokenMap();

  private MovieRenamer() {
    throw new IllegalAccessError();
  }

  /**
   * initialize the token map for the renamer
   *
   * @return the token map
   */
  private static Map<String, String> createTokenMap() {
    Map<String, String> tokenMap = new HashMap<>();
    tokenMap.put("title", "movie.title");
    tokenMap.put("originalTitle", "movie.originalTitle");
    tokenMap.put("originalFilename", "movie.originalFilename");
    tokenMap.put("originalBasename", "movie.originalBasename");
    tokenMap.put("sorttitle", "movie.sortTitle");
    tokenMap.put("year", "movie.year");
    tokenMap.put("releaseDate", "movie.releaseDate;date(yyyy-MM-dd)");
    tokenMap.put("titleSortable", "movie.titleSortable");
    tokenMap.put("rating", "movie.rating.rating");
    tokenMap.put("imdb", "movie.imdbId");
    tokenMap.put("tmdb", "movie.tmdbId");
    tokenMap.put("certification", "movie.certification");
    tokenMap.put("language", "movie.spokenLanguages");

    tokenMap.put("genres", "movie.genres");
    tokenMap.put("genresAsString", "movie.genresAsString");
    tokenMap.put("tags", "movie.tags");
    tokenMap.put("actors", "movie.actors");
    tokenMap.put("producers", "movie.producers");
    tokenMap.put("directors", "movie.directors");
    tokenMap.put("writers", "movie.writers");
    tokenMap.put("productionCompany", "movie.productionCompany");
    tokenMap.put("productionCompanyAsArray", "movie.productionCompanyAsArray");

    tokenMap.put("videoCodec", "movie.mediaInfoVideoCodec");
    tokenMap.put("videoFormat", "movie.mediaInfoVideoFormat");
    tokenMap.put("aspectRatio", "movie.mediaInfoAspectRatioAsString");
    tokenMap.put("aspectRatio2", "movie.mediaInfoAspectRatio2AsString");
    tokenMap.put("videoResolution", "movie.mediaInfoVideoResolution");
    tokenMap.put("videoBitDepth", "movie.mediaInfoVideoBitDepth");
    tokenMap.put("videoBitRate", "movie.mediaInfoVideoBitrate;bitrate");
    tokenMap.put("framerate", "movie.mediaInfoFrameRate;framerate");

    tokenMap.put("audioCodec", "movie.mediaInfoAudioCodec");
    tokenMap.put("audioCodecList", "movie.mediaInfoAudioCodecList");
    tokenMap.put("audioCodecsAsString", "movie.mediaInfoAudioCodecList;array");
    tokenMap.put("audioChannels", "movie.mediaInfoAudioChannels");
    tokenMap.put("audioChannelList", "movie.mediaInfoAudioChannelList");
    tokenMap.put("audioChannelsAsString", "movie.mediaInfoAudioChannelList;array");
    tokenMap.put("audioChannelsDot", "movie.mediaInfoAudioChannelsDot");
    tokenMap.put("audioChannelDotList", "movie.mediaInfoAudioChannelDotList");
    tokenMap.put("audioChannelsDotAsString", "movie.mediaInfoAudioChannelDotList;array");
    tokenMap.put("audioLanguage", "movie.mediaInfoAudioLanguage");
    tokenMap.put("audioLanguageList", "movie.mediaInfoAudioLanguageList");
    tokenMap.put("audioLanguagesAsString", "movie.mediaInfoAudioLanguageList;array");

    tokenMap.put("subtitleLanguageList", "movie.mediaInfoSubtitleLanguageList");
    tokenMap.put("subtitleLanguagesAsString", "movie.mediaInfoSubtitleLanguageList;array");
    tokenMap.put("3Dformat", "movie.video3DFormat");
    tokenMap.put("hdr", "movie.videoHDR");
    tokenMap.put("hdrformat", "movie.videoHDRFormat");
    tokenMap.put("filesize", "movie.videoFilesize;filesize");

    tokenMap.put("mediaSource", "movie.mediaSource");
    tokenMap.put("edition", "movie.edition");
    tokenMap.put("parent", "movie.parent");
    tokenMap.put("note", "movie.note");
    tokenMap.put("decadeLong", "movie.decadeLong");
    tokenMap.put("decadeShort", "movie.decadeShort");
    tokenMap.put("movieSetIndex", "movie;indexOfMovieSet");
    tokenMap.put("movieSetIndex2", "movie;indexOfMovieSetWithDummy");

    tokenMap.put("crc32", "movie.CRC32");

    return tokenMap;
  }

  public static Map<String, String> getTokenMap() {
    return Collections.unmodifiableMap(TOKEN_MAP);
  }

  public static Map<String, String> getTokenMapReversed() {
    return Collections.unmodifiableMap(TOKEN_MAP.entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey)));
  }

  /**
   * remove empty subfolders in this folder after renaming; only valid if we're in a single movie folder!
   *
   * @param movie
   *          the movie to clean
   */
  private static void removeEmptySubfolders(Movie movie) {
    if (movie.isMultiMovieDir()) {
      return;
    }

    // check all subfolders if they're empty (recursively)
    try {
      Utils.deleteEmptyDirectoryRecursive(movie.getPathNIO());
    }
    catch (IOException e) {
      LOGGER.warn("could not delete empty subfolders: {}", e.getMessage());
    }
  }

  /**
   * Deletes "unwanted files" according to settings. Same as the action, but w/o GUI.
   * 
   * @param movie
   *          the {@link Movie} to clean up
   */
  private static void cleanupUnwantedFiles(Movie movie) {
    if (movie.isMultiMovieDir()) {
      return;
    }
    if (MovieModuleManager.getInstance().getSettings().renamerCleanupUnwanted) {
      Utils.deleteUnwantedFilesAndFoldersFor(movie);
    }
  }

  /**
   * Rename movie inside the actual datasource.
   *
   * @param movie
   *          the movie
   */
  public static void renameMovie(Movie movie) {
    // skip renamer, if all templates are empty!
    if (MovieModuleManager.getInstance().getSettings().getRenamerPathname().isEmpty()
        && MovieModuleManager.getInstance().getSettings().getRenamerFilename().isEmpty()) {
      LOGGER.info("NOT renaming Movie '{}' - renaming patterns are empty!", movie.getTitle());
      return;
    }

    // FIXME: what? when?
    boolean posterRenamed = false;
    boolean fanartRenamed = false;

    // check if a datasource is set
    if (StringUtils.isEmpty(movie.getDataSource())) {
      LOGGER.error("no Datasource set");
      return;
    }

    if (movie.getTitle().isEmpty()) {
      LOGGER.error("won't rename movie '{}' / '{}' not even title is set?", movie.getPathNIO(), movie.getTitle());
      return;
    }

    // all the good & needed mediafiles & hbistory
    List<MediaFile> needed = new ArrayList<>();
    List<MediaFile> cleanup = new ArrayList<>();
    MediaEntityFilenameHistory fileNameHistory = new MediaEntityFilenameHistory();

    LOGGER.info("Renaming movie: {}", movie.getTitle());
    LOGGER.debug("movie year: {}", movie.getYear());
    LOGGER.debug("movie path: {}", movie.getPathNIO());
    LOGGER.debug("movie isDisc?: {}", movie.isDisc());
    LOGGER.debug("movie isMulti?: {}", movie.isMultiMovieDir());
    if (movie.getMovieSet() != null) {
      LOGGER.debug("movieset: {}", movie.getMovieSet().getTitle());
    }
    LOGGER.debug("path expression: {}", MovieModuleManager.getInstance().getSettings().getRenamerPathname());
    LOGGER.debug("file expression: {}", MovieModuleManager.getInstance().getSettings().getRenamerFilename());

    String newPathname = createDestinationForFoldername(MovieModuleManager.getInstance().getSettings().getRenamerPathname(), movie);
    String oldPathname = movie.getPathNIO().toString();

    if (!newPathname.isEmpty()) {
      try {
        newPathname = Paths.get(movie.getDataSource(), newPathname).toString();
        if (!renameMovieFolder(movie, newPathname)) {
          return;
        }
      }
      catch (Exception e) {
        LOGGER.warn("new movie folder name is illegal - '{}'", e.getMessage());
        newPathname = movie.getPathNIO().toString();
      }
    } // folder pattern empty
    else {
      LOGGER.info("Folder rename settings were empty - NOT renaming folder");
      // set it to current for file renaming
      newPathname = movie.getPathNIO().toString();
    }

    // make sure we have actual stacking markers
    movie.reEvaluateStacking();

    // ######################################################################
    // ## mark ALL existing and known files for cleanup (clone!!)
    // ######################################################################
    for (MovieNfoNaming s : MovieNfoNaming.values()) {
      String nfoFilename = movie.getNfoFilename(s);
      if (StringUtils.isBlank(nfoFilename)) {
        continue;
      }
      // mark all known variants for cleanup
      MediaFile del = new MediaFile(movie.getPathNIO().resolve(nfoFilename), MediaFileType.NFO);
      cleanup.add(del);
    }
    List<IFileNaming> fileNamings = new ArrayList<>();
    fileNamings.addAll(Arrays.asList(MoviePosterNaming.values()));
    fileNamings.addAll(Arrays.asList(MovieFanartNaming.values()));
    fileNamings.addAll(Arrays.asList(MovieBannerNaming.values()));
    fileNamings.addAll(Arrays.asList(MovieClearartNaming.values()));
    fileNamings.addAll(Arrays.asList(MovieClearlogoNaming.values()));
    fileNamings.addAll(Arrays.asList(MovieThumbNaming.values()));
    fileNamings.addAll(Arrays.asList(MovieDiscartNaming.values()));
    fileNamings.addAll(Arrays.asList(MovieKeyartNaming.values()));

    for (IFileNaming fileNaming : fileNamings) {
      for (String ext : KNOWN_IMAGE_FILE_EXTENSIONS) {
        MediaFile del = new MediaFile(movie.getPathNIO().resolve(MovieArtworkHelper.getArtworkFilename(movie, fileNaming, ext)));
        cleanup.add(del);
      }
    }

    // cleanup ALL MFs
    for (MediaFile del : movie.getMediaFiles()) {
      cleanup.add(new MediaFile(del));
    }
    cleanup.removeAll(Collections.singleton(null)); // remove all NULL ones!

    // BASENAME
    String newVideoBasename = "";
    String oldVideoBasename = Utils.cleanStackingMarkers(movie.getMainVideoFile().getBasename());
    if (!isFilePatternValid()) {
      // Template empty or not even title set, so we are NOT renaming any files
      // we keep the same name on renaming ;)
      newVideoBasename = movie.getVideoBasenameWithoutStacking();
      LOGGER.warn("Filepattern is not valid - NOT renaming files!");
    }
    else {
      // since we rename, generate the new basename
      MediaFile ftr = generateFilename(movie, movie.getMediaFiles(MediaFileType.VIDEO).get(0), newVideoBasename, oldVideoBasename).get(0);
      newVideoBasename = FilenameUtils.getBaseName(ftr.getFilenameWithoutStacking());
    }
    LOGGER.debug("Our new basename for renaming: {}", newVideoBasename);

    // ######################################################################
    // ## rename VIDEO (move 1:1)
    // ######################################################################
    for (MediaFile vid : movie.getMediaFiles(MediaFileType.VIDEO)) {
      LOGGER.trace("Rename 1:1 {} - {}", vid.getType(), vid.getFileAsPath());
      MediaFile newMF = generateFilename(movie, vid, newVideoBasename).get(0); // there can be only one
      boolean ok = moveFile(vid.getFileAsPath(), newMF.getFileAsPath());
      if (ok) {
        fileNameHistory.addFilenameHistory(createFilenameHistory(newPathname, vid.getFileAsPath(), newMF.getFileAsPath()));
        vid.setFile(newMF.getFileAsPath()); // update
      }
      else {
        LOGGER.error("could not movie video file of movie '{}' - abort renaming", movie.getTitle());
        // could not move main video file - abort!
        // if we're in a MMD, we did not do anything before, just reset the path
        if (movie.isMultiMovieDir()) {
          movie.setPath(oldPathname);
        }

        return;
      }
      needed.add(vid); // add vid, since we're updating existing MF object
    }

    // ######################################################################
    // ## rename POSTER, FANART, BANNER, CLEARART, THUMB, LOGO, CLEARLOGO, DISCART, KEYART (copy 1:N)
    // ######################################################################
    // we can have multiple ones, just get the newest one and copy(overwrite) them to all needed
    List<MediaFile> mfs = new ArrayList<>();
    mfs.add(movie.getNewestMediaFilesOfType(MediaFileType.FANART));
    mfs.add(movie.getNewestMediaFilesOfType(MediaFileType.POSTER));
    mfs.add(movie.getNewestMediaFilesOfType(MediaFileType.BANNER));
    mfs.add(movie.getNewestMediaFilesOfType(MediaFileType.CLEARART));
    mfs.add(movie.getNewestMediaFilesOfType(MediaFileType.THUMB));
    mfs.add(movie.getNewestMediaFilesOfType(MediaFileType.LOGO));
    mfs.add(movie.getNewestMediaFilesOfType(MediaFileType.CLEARLOGO));
    mfs.add(movie.getNewestMediaFilesOfType(MediaFileType.DISC));
    mfs.add(movie.getNewestMediaFilesOfType(MediaFileType.KEYART));
    mfs.removeAll(Collections.singleton(null)); // remove all NULL ones!
    for (MediaFile mf : mfs) {
      LOGGER.trace("Rename 1:N {} - {}", mf.getType(), mf.getFileAsPath());
      List<MediaFile> newMFs = generateFilename(movie, mf, newVideoBasename); // 1:N
      for (MediaFile newMF : newMFs) {
        posterRenamed = true;
        fanartRenamed = true;
        boolean ok = copyFile(mf.getFileAsPath(), newMF.getFileAsPath());
        if (ok) {
          needed.add(newMF);
          fileNameHistory.addFilenameHistory(createFilenameHistory(newPathname, mf.getFileAsPath(), newMF.getFileAsPath()));

          // update the cached image by just COPYing it around (1:N)
          if (ImageCache.isImageCached(mf.getFileAsPath())) {
            Path oldCache = ImageCache.getAbsolutePath(mf);
            Path newCache = ImageCache.getAbsolutePath(newMF);
            LOGGER.trace("updating imageCache {} -> {}", oldCache, newCache);
            // just use plain copy here, since we do not need all the safety checks done in our method
            try {
              Files.copy(oldCache, newCache);
            }
            catch (IOException e) {
              LOGGER.warn("Error moving cached file - '{}'", e.getMessage());
            }
          }
        }
      }
    }

    // ######################################################################
    // ## rename NFO (copy 1:N) - only TMM NFOs
    // ######################################################################
    // we need to find the newest, valid TMM NFO
    MediaFile nfo = MediaFile.EMPTY_MEDIAFILE;
    for (MediaFile mf : movie.getMediaFiles(MediaFileType.NFO)) {
      if (mf.getFiledate() >= nfo.getFiledate() && MovieConnectors.isValidNFO(mf.getFileAsPath())) {
        nfo = new MediaFile(mf);
      }
    }

    if (nfo != MediaFile.EMPTY_MEDIAFILE) { // one valid found? copy our NFO to all variants
      List<MediaFile> newNFOs = generateFilename(movie, nfo, newVideoBasename); // 1:N
      if (!newNFOs.isEmpty()) {
        // ok, at least one has been set up
        for (MediaFile newNFO : newNFOs) {
          boolean ok = copyFile(nfo.getFileAsPath(), newNFO.getFileAsPath());
          if (ok) {
            needed.add(newNFO);
            fileNameHistory.addFilenameHistory(createFilenameHistory(newPathname, nfo.getFileAsPath(), newNFO.getFileAsPath()));
          }
        }
      }
      else {
        // list was empty, so even remove this NFO
        cleanup.add(nfo);
      }
    }
    else {
      LOGGER.trace("No valid NFO found for this movie");
    }

    // now iterate over all non-tmm NFOs, and add them for cleanup or not
    for (MediaFile mf : movie.getMediaFiles(MediaFileType.NFO)) {
      if (MovieConnectors.isValidNFO(mf.getFileAsPath())) {
        cleanup.add(mf);
      }
      else {
        if (MovieModuleManager.getInstance().getSettings().isRenamerNfoCleanup()) {
          cleanup.add(mf);
        }
        else {
          needed.add(mf);
          fileNameHistory.addFilenameHistory(createFilenameHistory(newPathname, mf.getFileAsPath(), mf.getFileAsPath()));
        }
      }
    }

    // ######################################################################
    // ## rename all other types (copy 1:1)
    // ######################################################################
    mfs = new ArrayList<>(movie.getMediaFilesExceptType(MediaFileType.VIDEO, MediaFileType.NFO, MediaFileType.POSTER, MediaFileType.FANART,
        MediaFileType.BANNER, MediaFileType.CLEARART, MediaFileType.THUMB, MediaFileType.LOGO, MediaFileType.CLEARLOGO, MediaFileType.DISC,
        MediaFileType.KEYART, MediaFileType.SUBTITLE));
    mfs.removeAll(Collections.singleton(null)); // remove all NULL ones!
    for (MediaFile other : mfs) {
      LOGGER.trace("Rename 1:1 {} - {}", other.getType(), other.getFileAsPath());

      List<MediaFile> newMFs = generateFilename(movie, other, newVideoBasename, oldVideoBasename); // 1:N
      newMFs.removeAll(Collections.singleton(null)); // remove all NULL ones!
      for (MediaFile newMF : newMFs) {
        boolean ok = copyFile(other.getFileAsPath(), newMF.getFileAsPath());
        if (ok) {
          fileNameHistory.addFilenameHistory(createFilenameHistory(newPathname, other.getFileAsPath(), newMF.getFileAsPath()));
          needed.add(newMF);
        }
        else {
          // FIXME: what to do? not copied/exception... keep it for now...
          fileNameHistory.addFilenameHistory(createFilenameHistory(newPathname, other.getFileAsPath(), other.getFileAsPath()));
          needed.add(other);
        }
      }
    }

    // ######################################################################
    // ## rename SUBTITLEs (copy 1:1)
    // ######################################################################
    for (MediaFile sub : movie.getMediaFiles(MediaFileType.SUBTITLE)) {
      LOGGER.trace("Rename 1:1 {} - {}", sub.getType(), sub.getFileAsPath());
      MediaFile newMF = generateFilename(movie, sub, newVideoBasename, oldVideoBasename).get(0);
      boolean ok = moveFile(sub.getFileAsPath(), newMF.getFileAsPath());
      if (ok) {
        if (sub.getFilename().endsWith(".sub")) {
          // when having a .sub, also rename .idx (don't care if error)
          try {
            Path oldidx = sub.getFileAsPath().resolveSibling(sub.getFilename().replaceFirst("sub$", "idx"));
            Path newidx = newMF.getFileAsPath().resolveSibling(newMF.getFilename().toString().replaceFirst("sub$", "idx"));
            Utils.moveFileSafe(oldidx, newidx);
            fileNameHistory.addFilenameHistory(createFilenameHistory(newPathname, oldidx, newidx));
          }
          catch (Exception e) {
            // no idx found or error - ignore
          }
        }
        needed.add(newMF);
        fileNameHistory.addFilenameHistory(createFilenameHistory(newPathname, sub.getFileAsPath(), newMF.getFileAsPath()));
      }
      else {
        LOGGER.error("could not rename subtitle file '{}'", sub.getFileAsPath());
        needed.add(sub);
      }
    }

    // ######################################################################
    // ## invalidate image cache
    // ######################################################################
    for (MediaFile gfx : movie.getMediaFiles()) {
      if (gfx.isGraphic() && !needed.contains(gfx)) {
        ImageCache.invalidateCachedImage(gfx);
      }
    }

    // remove duplicate MediaFiles
    Set<MediaFile> newMFs = new LinkedHashSet<>(needed);
    needed.clear();
    needed.addAll(newMFs);

    movie.removeAllMediaFiles();

    // ######################################################################
    // ## build up image cache
    // ######################################################################
    if (Settings.getInstance().isImageCache()) {
      for (MediaFile gfx : needed) {
        ImageCache.cacheImageSilently(gfx, false);
      }
    }

    // give the file system a bit to write the files
    ThreadUtils.sleep(250);

    movie.addToMediaFiles(needed);
    movie.setPath(newPathname);

    movie.gatherMediaFileInformation(false);

    // rewrite NFO if it's a MP NFO and there was a change with poster/fanart
    if (MovieModuleManager.getInstance().getSettings().getMovieConnector() == MovieConnectors.MP && (posterRenamed || fanartRenamed)) {
      movie.writeNFO();
    }

    // ######################################################################
    // ## CLEANUP - delete all files marked for cleanup, which are not "needed"
    // ######################################################################
    LOGGER.info("Cleanup...");

    // get all existing files in the movie dir, since Files.exist is not reliable in OSX
    List<Path> existingFiles;
    if (movie.isMultiMovieDir()) {
      // no recursive search in MMD needed
      existingFiles = Utils.listFiles(movie.getPathNIO());
    }
    else {
      // search all files recursive for deeper cleanup
      existingFiles = Utils.listFilesRecursive(movie.getPathNIO());
    }

    // also add all files from the old path (if upgraded from MMD)
    existingFiles.addAll(Utils.listFiles(Paths.get(oldPathname)));

    for (int i = cleanup.size() - 1; i >= 0; i--) {
      MediaFile cl = cleanup.get(i);

      // cleanup files which are not needed
      if (!needed.contains(cl)) {
        if (cl.getFileAsPath().equals(Paths.get(movie.getDataSource())) || cl.getFileAsPath().equals(movie.getPathNIO())
            || cl.getFileAsPath().equals(Paths.get(oldPathname))) {
          LOGGER.warn("Wohoo! We tried to remove complete datasource / movie folder. Nooo way...! {}: {}", cl.getType(), cl.getFileAsPath());
          // happens when iterating eg over the getNFONaming and we return a "" string.
          // then the path+filename = movie path and we want to delete :/
          continue;
        }

        movie.removeFromMediaFiles(cl);

        if (existingFiles.contains(cl.getFileAsPath())) {
          LOGGER.debug("Deleting {}", cl.getFileAsPath());
          Utils.deleteFileWithBackup(cl.getFileAsPath(), movie.getDataSource());
          // also cleanup the cache for deleted mfs
          if (cl.isGraphic()) {
            ImageCache.invalidateCachedImage(cl);
          }
        }

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(cl.getFileAsPath().getParent())) {
          if (!directoryStream.iterator().hasNext()) {
            // no iterator = empty
            LOGGER.debug("Deleting empty Directory {}", cl.getFileAsPath().getParent());
            Files.delete(cl.getFileAsPath().getParent()); // do not use recursive her
          }
        }
        catch (IOException e) {
          LOGGER.warn("could not search for empty dir: {}", e.getMessage());
        }
      }
    }

    cleanupUnwantedFiles(movie);
    removeEmptySubfolders(movie);

    // rename history
    fileNameHistory.setOldPath(oldPathname);
    fileNameHistory.setNewPath(newPathname);
    movie.setRenameHistory(fileNameHistory);

    movie.saveToDb();
  }

  public static void undoRename(Movie movie) {
    if (movie.getRenameHistory() == null) {
      LOGGER.debug("could not undo rename - no history available");
      return;
    }

    List<MediaFile> needed = new ArrayList<>();

    Path oldMoviePath = Paths.get(movie.getRenameHistory().getOldPath());
    Path newMoviePath = Paths.get(movie.getRenameHistory().getNewPath());

    // try the VIDEO file(s) first
    for (MediaFile vid : movie.getMediaFiles(MediaFileType.VIDEO)) {
      MediaEntityFilenameHistory.FilenameHistory filenameHistory = findFilenameHistoryForMediaFile(movie, vid);
      if (filenameHistory == null) {
        LOGGER.debug("could not undo rename - VIDEO file history not found");
        return;
      }

      LOGGER.trace("Rename 1:1 {} - {}", vid.getType(), vid.getFileAsPath());
      MediaFile oldMF = new MediaFile(vid);
      oldMF.replacePathForRenamedFolder(newMoviePath, oldMoviePath);
      oldMF.setFile(oldMoviePath.resolve(filenameHistory.oldFilename()));

      if (movie.isDisc() && Files.isDirectory(vid.getFile())) {
        boolean ok = moveDirectory(vid.getFileAsPath(), oldMF.getFileAsPath());
        if (ok) {
          vid.setFile(oldMF.getFileAsPath()); // update
        }
        else {
          LOGGER.error("could not move video file of movie '{}' - abort renaming", movie.getTitle());
          return;
        }
      }
      else {
        boolean ok = moveFile(vid.getFileAsPath(), oldMF.getFileAsPath());
        if (ok) {
          vid.setFile(oldMF.getFileAsPath()); // update
        }
        else {
          LOGGER.error("could not move video file of movie '{}' - abort renaming", movie.getTitle());
          return;
        }
      }

      needed.add(vid); // add vid, since we're updating existing MF object
    }

    // and all others
    for (MediaFile mediaFile : movie.getMediaFilesExceptType(MediaFileType.VIDEO)) {
      MediaEntityFilenameHistory.FilenameHistory filenameHistory = findFilenameHistoryForMediaFile(movie, mediaFile);
      if (filenameHistory == null) {
        LOGGER.debug("could not undo rename for '{}' - history not found", mediaFile.getFilename());
        continue;
      }

      LOGGER.trace("Rename 1:1 {} - {}", mediaFile.getType(), mediaFile.getFileAsPath());
      MediaFile oldMF = new MediaFile(mediaFile);
      oldMF.replacePathForRenamedFolder(newMoviePath, oldMoviePath);
      oldMF.setFile(oldMoviePath.resolve(filenameHistory.oldFilename()));

      if (movie.isDisc() && !Files.exists(mediaFile.getFileAsPath())) {
        // this file probably has been merged into the disc folder -> we need to adopt the path
        Path newPath = movie.getMainVideoFile().getFileAsPath().resolve(mediaFile.getFilename());
        mediaFile.setFile(newPath);
      }

      boolean ok = moveFile(mediaFile.getFileAsPath(), oldMF.getFileAsPath());
      if (ok) {
        mediaFile.setFile(oldMF.getFileAsPath()); // update
        needed.add(mediaFile);
      }
      else {
        // probably a 1:n rename - just remove that file
        Utils.deleteFileWithBackup(mediaFile.getFileAsPath(), movie.getDataSource());
      }
    }

    // remove duplicate MediaFiles
    Set<MediaFile> newMFs = new LinkedHashSet<>(needed);
    needed.clear();
    needed.addAll(newMFs);

    movie.removeAllMediaFiles();

    // ######################################################################
    // ## build up image cache
    // ######################################################################
    if (Settings.getInstance().isImageCache()) {
      for (MediaFile gfx : needed) {
        ImageCache.cacheImageSilently(gfx, false);
      }
    }

    // give the file system a bit to write the files
    ThreadUtils.sleep(250);

    movie.addToMediaFiles(needed);
    movie.setPath(movie.getRenameHistory().getOldPath());
    movie.gatherMediaFileInformation(false);

    // remove history
    movie.setRenameHistory(null);

    movie.saveToDb();

    // cleanup old path
    try {
      Utils.deleteEmptyDirectoryRecursive(newMoviePath);
    }
    catch (IOException e) {
      LOGGER.warn("could not delete empty subfolders: {}", e.getMessage());
    }
  }

  private static MediaEntityFilenameHistory.FilenameHistory createFilenameHistory(String newMoviePath, Path oldFilePath, Path newFilePath) {
    String oldFilename = Paths.get(newMoviePath).relativize(oldFilePath).toString(); // already changed in the generate filename logic
    String newFilename = Paths.get(newMoviePath).relativize(newFilePath).toString();
    return new MediaEntityFilenameHistory.FilenameHistory(oldFilename, newFilename);
  }

  private static MediaEntityFilenameHistory.FilenameHistory findFilenameHistoryForMediaFile(Movie movie, MediaFile mediaFile) {
    if (movie.getRenameHistory() == null) {
      return null;
    }

    Path moviePath = movie.getPathNIO();

    for (MediaEntityFilenameHistory.FilenameHistory filenameHistory : movie.getRenameHistory().getFilenameHistory()) {
      if (filenameHistory.newFilename().equals(moviePath.relativize(mediaFile.getFileAsPath()).toString())) {
        return filenameHistory;
      }
    }

    return null;
  }

  private static boolean renameMovieFolder(Movie movie, String newPathname) {
    Path srcDir = movie.getPathNIO();
    Path destDir = Paths.get(newPathname);
    if (!srcDir.toAbsolutePath().toString().equals(destDir.toAbsolutePath().toString())) {
      boolean newDestIsMultiMovieDir = false;
      // re-evaluate multiMovieDir based on renamer settings
      // folder MUST BE UNIQUE, we need at least a T/E-Y combo or IMDBid
      // so if renaming just to a fixed pattern (eg "$S"), movie will downgrade to a MMD
      if (!isFolderPatternUnique(MovieModuleManager.getInstance().getSettings().getRenamerPathname())) {
        newDestIsMultiMovieDir = true;
      }
      else {
        // check if the target folder already exists (and is not empty)
        // check if the user wants this behaviour
        try {
          if (Files.exists(destDir) && !Utils.isFolderEmpty(destDir)
              && MovieModuleManager.getInstance().getSettings().isAllowMultipleMoviesInSameDir()) {
            // destination folder exists and is not empty - assume there is another movie -> MMD = true
            newDestIsMultiMovieDir = true;
            MessageManager.instance
                .pushMessage(new Message(MessageLevel.INFO, srcDir, "message.renamer.mergetommd", new String[] { movie.getTitle() }));
          }
        }
        catch (Exception e) {
          LOGGER.warn("could not check if dir '{}' exists/is empty - '{}'", destDir, e.getMessage());
        }
      }
      LOGGER.debug("movie willBeMulti?: {}", newDestIsMultiMovieDir);

      // ######################################################################
      // ## 1) old = separate movie dir, and new too -> move folder
      // ######################################################################
      if (!movie.isMultiMovieDir() && !newDestIsMultiMovieDir) {
        boolean ok;
        try {
          ok = Utils.moveDirectorySafe(srcDir, destDir);
          if (ok) {
            movie.setMultiMovieDir(false);
            movie.updateMediaFilePath(srcDir, destDir);
            movie.setPath(newPathname);
            movie.saveToDb(); // since we moved already, save it
          }
        }
        catch (Exception e) {
          LOGGER.error("error moving folder: ", e);
          MessageManager.instance
              .pushMessage(new Message(MessageLevel.ERROR, srcDir, "message.renamer.failedrename", new String[] { ":", e.getLocalizedMessage() }));
          return false;
        }
        if (!ok) {
          MessageManager.instance
              .pushMessage(new Message(MessageLevel.ERROR, srcDir, "message.renamer.failedrename", new String[] { movie.getTitle() }));
          LOGGER.error("Could not move to destination '{}' - NOT renaming folder", destDir);
          return false;
        }
      }
      else if (movie.isMultiMovieDir() && !newDestIsMultiMovieDir) {
        // ######################################################################
        // ## 2) MMD movie -> normal movie (upgrade)
        // ######################################################################
        LOGGER.trace("Upgrading movie into it's own dir :) - {}", newPathname);
        if (!Files.exists(destDir)) {
          try {
            Files.createDirectories(destDir);
          }
          catch (Exception e) {
            LOGGER.error("Could not create destination '{}' - NOT renaming folder ('upgrade' movie) - {}", destDir, e.getMessage());
            // well, better not to rename
            return false;
          }
        }
        else {
          LOGGER.error("Directory already exists! '{}' - NOT renaming folder ('upgrade' movie)", destDir);
          // well, better not to rename
          return false;
        }
        movie.setMultiMovieDir(false);
      }
      else {
        // ######################################################################
        // ## Can be
        // ## 3) MMD movie -> MMD movie (but foldername possible changed)
        // ## 4) normal movie -> MMD movie (downgrade)
        // ## either way - check & create dest folder
        // ######################################################################
        LOGGER.trace("New movie path is a MMD :( - {}", newPathname);
        if (!Files.exists(destDir)) { // if existent, all is good -> MMD
          try {
            Files.createDirectories(destDir);
          }
          catch (Exception e) {
            LOGGER.error("Could not create destination '{}' - NOT renaming folder ('MMD' movie) - {}", destDir, e.getMessage());
            // well, better not to rename
            return false;
          }
        }
        movie.setMultiMovieDir(true);
      }
    } // src == dest

    return true;
  }

  /**
   * generates renamed filename(s) per MF
   *
   * @param movie
   *          the movie (for datasource, path)
   * @param mf
   *          the MF
   * @param newVideoFileName
   *          the basename of the renamed videoFileName (saved earlier)
   * @return list of renamed filename
   */
  public static List<MediaFile> generateFilename(Movie movie, MediaFile mf, String newVideoFileName) {
    return generateFilename(movie, mf, newVideoFileName, "");
  }

  /**
   * generates renamed filename(s) per MF
   *
   * @param movie
   *          the movie (for datasource, path)
   * @param mf
   *          the MF
   * @param newVideoFileName
   *          the basename of the renamed videoFileName (saved earlier)
   * @param oldVideoFileName
   *          the basename of the ORIGINAL videoFileName (saved earlier)
   * @return list of renamed filename
   */
  public static List<MediaFile> generateFilename(Movie movie, MediaFile mf, String newVideoFileName, String oldVideoFileName) {
    // return list of all generated MFs
    List<MediaFile> newFiles = new ArrayList<>();
    boolean newDestIsMultiMovieDir = movie.isMultiMovieDir();
    String newPathname = "";

    String pattern = MovieModuleManager.getInstance().getSettings().getRenamerPathname();
    // keep MMD setting unless renamer pattern is not empty
    if (!pattern.isEmpty()) {
      // re-evaluate multiMovieDir based on renamer settings
      // folder MUST BE UNIQUE, so we need at least a T/E-Y combo or IMDBid
      // If renaming just to a fixed pattern (eg "$S"), movie will downgrade to a MMD
      newDestIsMultiMovieDir = !MovieRenamer.isFolderPatternUnique(pattern);
      newPathname = MovieRenamer.createDestinationForFoldername(pattern, movie);
    }
    else {
      // keep same dir
      // Path relativize(Path other)
      newPathname = Utils.relPath(Paths.get(movie.getDataSource()), movie.getPathNIO());
    }
    Path newMovieDir = movie.getPathNIO();
    try {
      newMovieDir = Paths.get(movie.getDataSource(), newPathname);
    }
    catch (Exception e) {
      LOGGER.warn("new movie folder name is illegal - '{}'", e.getMessage());
    }

    String newFilename = newVideoFileName;
    if (newFilename == null || newFilename.isEmpty()) {
      // empty only when first generating basename, so generation here is OK
      newFilename = createDestinationForFilename(MovieModuleManager.getInstance().getSettings().getRenamerFilename(), movie);
    }
    // when renaming with $originalFilename, we get already the extension added!
    if (newFilename.endsWith(mf.getExtension())) {
      newFilename = FilenameUtils.getBaseName(newFilename);
    }

    // happens, when renaming pattern returns nothing (empty field like originalTitle)
    // just return same file
    if (newFilename.isEmpty()) {
      newFiles.add(mf);
      return newFiles;
    }

    // extra clone, just for easy adding the "default" ones ;)
    MediaFile defaultMF = new MediaFile(mf);
    defaultMF.replacePathForRenamedFolder(movie.getPathNIO(), newMovieDir);

    Path relativePathOfMediafile = movie.getPathNIO().relativize(mf.getFileAsPath());

    if (!isFilePatternValid() && !movie.isDisc()) {
      // not renaming files, but IF we have a folder pattern, we need to move around! (but NOT disc movies!)
      newFiles.add(defaultMF);
      return newFiles;
    }

    switch (mf.getType()) {
      case VIDEO:
        MediaFile vid = new MediaFile(mf);
        if (movie.isDisc() || mf.isDiscFile()) {
          // just replace new path and return file (do not change names!)
          vid.replacePathForRenamedFolder(movie.getPathNIO(), newMovieDir);
        }
        else {
          newFilename += getStackingString(mf);
          newFilename += "." + mf.getExtension();
          vid.setFile(newMovieDir.resolve(newFilename));
        }
        newFiles.add(vid);
        break;

      case TRAILER:
        // if the trailer is in a /trailer subfolder, just move it to the destination
        if (relativePathOfMediafile.getNameCount() > 1
            && MediaFileHelper.TRAILER_FOLDERS.contains(relativePathOfMediafile.subpath(0, 1).toString().toLowerCase(Locale.ROOT))) {
          // the trailer is in a /trailer(s) subfolder
          newFiles.add(defaultMF);
        }
        else {
          // not in a /trailer(s) subfolder
          List<MovieTrailerNaming> trailernames = new ArrayList<>();
          if (newDestIsMultiMovieDir) {
            // Fixate the name regardless of setting
            trailernames.add(MovieTrailerNaming.FILENAME_TRAILER);
          }
          else if (movie.isDisc()) {
            trailernames.add(MovieTrailerNaming.FILENAME_TRAILER);
          }
          else {
            trailernames.addAll(MovieModuleManager.getInstance().getSettings().getTrailerFilenames());
            if (trailernames.isEmpty()) {
              // we have a trailer, but no settings for it?! don't delete it, just rename it to the default
              trailernames.add(MovieTrailerNaming.FILENAME_TRAILER);
            }
          }

          // check if the trailer ends with a "stacking" marker
          String stackingMarker = "";
          Matcher matcher = TRAILER_STACKING_PATTERN.matcher(mf.getBasename());
          if (matcher.matches()) {
            stackingMarker = matcher.group(1);
          }

          // getTrailerFilename NEEDS extension - so add it here default, and overwrite it in isDisc()
          newFilename += ".avi";
          // DVD/BluRay folders can have trailers within!
          // check for discFolders and/or files
          Path outputFolder;
          if (movie.isDisc() && MovieModuleManager.getInstance().getSettings().isTrailerDiscFolderInside()) {
            MediaFile main = movie.getMainFile();
            if (MediaFileHelper.isDiscFolder(main.getFilename())) {
              Path mainFile = main.getFileAsPath();
              Path rel = movie.getPathNIO().relativize(mainFile);
              outputFolder = newMovieDir.resolve(rel);
            }
            else {
              outputFolder = newMovieDir; // not a virtual "MF folder"? use default
            }
            // since we ARE in a disc structure, we have to name it accordingly.... (folder or not)
            newFilename = movie.findDiscMainFile(); // with ext
          }
          else {
            outputFolder = newMovieDir; // default
          }

          for (MovieTrailerNaming name : trailernames) {
            String newTrailerName = movie.getTrailerFilename(name, newFilename); // basename used, so add fake extension
            if (newTrailerName.isEmpty()) {
              continue;
            }
            MediaFile trail = new MediaFile(mf);
            if (StringUtils.isNotBlank(stackingMarker)) {
              trail.setFile(outputFolder.resolve(newTrailerName + "." + stackingMarker + "." + mf.getExtension())); // get w/o extension to add same
            }
            else {
              trail.setFile(outputFolder.resolve(newTrailerName + "." + mf.getExtension())); // get w/o extension to add same
            }
            newFiles.add(trail);
          }
        }
        break;

      case EXTRA:
      case VIDEO_EXTRA:
        // this extra is for an episode -> move it at least to the season folder and try to replace the episode tokens
        MediaFile extra = new MediaFile(mf);
        if (MediaFileHelper.isExtraInDedicatedFolder(mf, movie)) {
          // do nothing
          newFiles.add(defaultMF);
        }
        else {
          // try to detect the title of the extra file
          String extraTitle = mf.getBasename().replace(oldVideoFileName, "");
          extra.setFile(newMovieDir.resolve(newFilename + extraTitle + "." + mf.getExtension()));
          newFiles.add(extra);
        }
        break;

      case SAMPLE:
        MediaFile sample = new MediaFile(mf);
        newFilename += "-sample." + mf.getExtension();
        sample.setFile(newMovieDir.resolve(newFilename));
        newFiles.add(sample);
        break;

      case MEDIAINFO:
        MediaFile mi = new MediaFile(mf);
        if (movie.isDisc()) {
          // hmm.. dunno, keep at least 1:1
          mi.replacePathForRenamedFolder(movie.getPathNIO(), newMovieDir);
          newFiles.add(mi);
        }
        else {
          newFilename += getStackingString(mf);
          newFilename += "-mediainfo." + mf.getExtension();
          mi.setFile(newMovieDir.resolve(newFilename));
          newFiles.add(mi);
        }
        break;

      case DOUBLE_EXT:
      case VSMETA:
        MediaFile doubleExt = new MediaFile(mf);
        if (movie.isDisc()) {
          // keep 1:1
          doubleExt.setFile(newMovieDir.resolve(doubleExt.getFilename()));
        }
        else {
          newFilename += getStackingString(mf);
          // HACK: get video extension from "old" name, eg video.avi.vsmeta
          String videoExt = FilenameUtils.getExtension(FilenameUtils.getBaseName(mf.getFilename()));
          newFilename += "." + videoExt + "." + FilenameUtils.getExtension(mf.getFilename());
          doubleExt.setFile(newMovieDir.resolve(newFilename));
        }
        newFiles.add(doubleExt);
        break;

      case SUBTITLE:
        List<MediaFileSubtitle> subtitles = mf.getSubtitles();
        newFilename += getStackingString(mf);
        String subtitleFilename = newFilename;
        if (subtitles != null && !subtitles.isEmpty()) {
          MediaFileSubtitle sub = mf.getSubtitles().get(0);
          if (sub != null) {
            if (!sub.getLanguage().isEmpty()) {
              String lang = LanguageStyle.getLanguageCodeForStyle(sub.getLanguage(),
                  MovieModuleManager.getInstance().getSettings().getSubtitleLanguageStyle());
              if (StringUtils.isBlank(lang)) {
                lang = sub.getLanguage();
              }
              subtitleFilename += "." + lang;
            }

            if (sub.isForced()) {
              subtitleFilename += ".forced";
            }
            if (sub.isSdh()) {
              subtitleFilename += ".sdh"; // double possible?! should be no prob...
            }
            if (StringUtils.isNotBlank(sub.getTitle())) {
              subtitleFilename += "." + sub.getTitle().strip();
            }
          }
        }

        // still no subtitle filename? take at least the whole filename
        if (StringUtils.isBlank(subtitleFilename)) {
          subtitleFilename = newFilename;
        }

        if (StringUtils.isNotBlank(subtitleFilename)) {
          MediaFile subtitle = new MediaFile(mf);
          subtitle.setFile(newMovieDir.resolve(subtitleFilename + "." + mf.getExtension()));
          newFiles.add(subtitle);
        }
        break;

      case NFO:
        if (MovieModuleManager.getInstance().getSettings().getNfoFilenames().isEmpty()) {
          // we do not want NFO to be renamed? so they will be removed....
          break;
        }

        if (MovieConnectors.isValidNFO(mf.getFileAsPath())) {
          List<MovieNfoNaming> nfonames = new ArrayList<>();
          if (newDestIsMultiMovieDir) {
            // Fixate the name regardless of setting
            nfonames.add(MovieNfoNaming.FILENAME_NFO);
          }
          else if (movie.isDisc()) {
            nfonames.add(MovieNfoNaming.FILENAME_NFO);
          }
          else {
            nfonames = MovieModuleManager.getInstance().getSettings().getNfoFilenames();
          }
          for (MovieNfoNaming name : nfonames) {
            String newNfoName = movie.getNfoFilename(name, newFilename + ".avi"); // basename used, so add fake extension
            if (newNfoName.isEmpty()) {
              continue;
            }
            MediaFile nfo = new MediaFile(mf);
            nfo.setFile(newMovieDir.resolve(newNfoName));
            newFiles.add(nfo);
          }
        }
        else {
          // not a TMM NFO
          if (!MovieModuleManager.getInstance().getSettings().isRenamerNfoCleanup()) {
            newFiles.add(new MediaFile(mf));
          }

        }
        break;

      case POSTER:
        for (MoviePosterNaming name : MovieArtworkHelper.getPosterNamesForMovie(movie)) {
          String newPosterName = name.getFilename(newFilename, getArtworkExtension(mf));
          if (StringUtils.isNotBlank(newPosterName)) {
            MediaFile pos = new MediaFile(mf);
            pos.setFile(newMovieDir.resolve(newPosterName));
            newFiles.add(pos);
          }
        }
        break;

      case FANART:
        for (MovieFanartNaming name : MovieArtworkHelper.getFanartNamesForMovie(movie)) {
          String newFanartName = name.getFilename(newFilename, getArtworkExtension(mf));
          if (StringUtils.isNotBlank(newFanartName)) {
            MediaFile fan = new MediaFile(mf);
            fan.setFile(newMovieDir.resolve(newFanartName));
            newFiles.add(fan);
          }
        }
        break;

      case BANNER:
        for (MovieBannerNaming name : MovieArtworkHelper.getBannerNamesForMovie(movie)) {
          String newBannerName = name.getFilename(newFilename, getArtworkExtension(mf));
          if (StringUtils.isNotBlank(newBannerName)) {
            MediaFile banner = new MediaFile(mf);
            banner.setFile(newMovieDir.resolve(newBannerName));
            newFiles.add(banner);
          }
        }
        break;

      case CLEARART:
        for (MovieClearartNaming name : MovieArtworkHelper.getClearartNamesForMovie(movie)) {
          String newClearartName = name.getFilename(newFilename, getArtworkExtension(mf));
          if (StringUtils.isNotBlank(newClearartName)) {
            MediaFile clearart = new MediaFile(mf);
            clearart.setFile(newMovieDir.resolve(newClearartName));
            newFiles.add(clearart);
          }
        }
        break;

      case DISC:
        for (MovieDiscartNaming name : MovieArtworkHelper.getDiscartNamesForMovie(movie)) {
          String newDiscartName = name.getFilename(newFilename, getArtworkExtension(mf));
          if (StringUtils.isNotBlank(newDiscartName)) {
            MediaFile discart = new MediaFile(mf);
            discart.setFile(newMovieDir.resolve(newDiscartName));
            newFiles.add(discart);
          }
        }
        break;

      case CLEARLOGO:
      case LOGO:
        for (MovieClearlogoNaming name : MovieArtworkHelper.getClearlogoNamesForMovie(movie)) {
          String newClearlogoName = name.getFilename(newFilename, getArtworkExtension(mf));
          if (StringUtils.isNotBlank(newClearlogoName)) {
            MediaFile clearlogo = new MediaFile(mf);
            clearlogo.setFile(newMovieDir.resolve(newClearlogoName));
            newFiles.add(clearlogo);
          }
        }
        break;

      case THUMB:
        for (MovieThumbNaming name : MovieArtworkHelper.getThumbNamesForMovie(movie)) {
          String newThumbName = name.getFilename(newFilename, getArtworkExtension(mf));
          if (StringUtils.isNotBlank(newThumbName)) {
            MediaFile thumb = new MediaFile(mf);
            thumb.setFile(newMovieDir.resolve(newThumbName));
            newFiles.add(thumb);
          }
        }
        break;

      case KEYART:
        for (MovieKeyartNaming name : MovieArtworkHelper.getKeyartNamesForMovie(movie)) {
          String newKeyartName = name.getFilename(newFilename, getArtworkExtension(mf));
          if (StringUtils.isNotBlank(newKeyartName)) {
            MediaFile key = new MediaFile(mf);
            key.setFile(newMovieDir.resolve(newKeyartName));
            newFiles.add(key);
          }
        }
        break;

      case EXTRAFANART:
        // get the index (is always the last digit before the extension)
        int index = MovieArtworkHelper.getIndexOfArtwork(mf.getFilename());
        if (index > 0) {
          // at the moment, we just support 1 naming scheme here! if we decide to enhance that, we will need to enhance the MovieExtraImageFetcherTask
          // too
          List<MovieExtraFanartNaming> extraFanartNamings = MovieArtworkHelper.getExtraFanartNamesForMovie(movie);
          if (!extraFanartNamings.isEmpty()) {
            MovieExtraFanartNaming fileNaming = extraFanartNamings.get(0);

            String newExtraFanartFilename = fileNaming.getFilename(newFilename, getArtworkExtension(mf));
            if (StringUtils.isNotBlank(newExtraFanartFilename)) {
              // split the filename again and attach the counter
              String basename = FilenameUtils.getBaseName(newExtraFanartFilename);
              newExtraFanartFilename = basename + index + "." + getArtworkExtension(mf);

              // create an empty extrafanarts folder if the right naming has been chosen
              Path folder;
              if (fileNaming == MovieExtraFanartNaming.FOLDER_EXTRAFANART) {
                folder = newMovieDir.resolve("extrafanart");
                try {
                  if (!Files.exists(folder)) {
                    Files.createDirectory(folder);
                  }
                }
                catch (IOException e) {
                  LOGGER.error("could not create extrafanarts folder: {}", e.getMessage());
                }
              }
              else {
                folder = newMovieDir;
              }

              MediaFile extrafanart = new MediaFile(mf);
              extrafanart.setFile(folder.resolve(newExtraFanartFilename));
              newFiles.add(extrafanart);
            }
          }
        }
        break;

      // *************
      // OK, from here we check only the settings
      // *************
      case EXTRATHUMB:
        // pass the file regardless of the settings (they're here so we just rename them)
        newFiles.add(defaultMF);
        break;

      // *************
      // here we add all others
      // *************
      default:
        newFiles.add(defaultMF);
        break;
    }

    return newFiles;
  }

  private static String getArtworkExtension(MediaFile mf) {
    String ext = mf.getExtension().replace("jpeg", "jpg"); // we only have one constant and only write jpg
    if (ext.equalsIgnoreCase("tbn")) {
      String cont = mf.getContainerFormat();
      if (cont.equalsIgnoreCase("PNG")) {
        ext = "png";
      }
      else if (cont.equalsIgnoreCase("JPEG")) {
        ext = "jpg";
      }
    }
    return ext;
  }

  /**
   * returns "delimiter + stackingString" for use in filename
   *
   * @param mf
   *          a mediaFile
   * @return eg ".CD1" dependent of settings
   */
  private static String getStackingString(MediaFile mf) {
    String delimiter = " ";
    if (MovieModuleManager.getInstance().getSettings().isRenamerFilenameSpaceSubstitution()) {
      delimiter = MovieModuleManager.getInstance().getSettings().getRenamerFilenameSpaceReplacement();
    }
    if (!mf.getStackingMarker().isEmpty()) {
      return delimiter + mf.getStackingMarker();
    }
    else if (mf.getStacking() != 0) {
      return delimiter + "CD" + mf.getStacking();
    }
    return "";
  }

  /**
   * Creates the new filename according to template string
   *
   * @param template
   *          the template
   * @param movie
   *          the movie
   * @return the string
   */
  public static String createDestinationForFilename(String template, Movie movie) {
    return createDestination(template, movie, true);
  }

  /**
   * Creates the new filename according to template string
   *
   * @param template
   *          the template
   * @param movie
   *          the movie
   * @return the string
   */
  public static String createDestinationForFoldername(String template, Movie movie) {
    return createDestination(template, movie, false);
  }

  /**
   * gets the token value (${x}) from specified movie object with all renamer related replacements!
   *
   * @param movie
   *          our movie
   * @param token
   *          the ${x} token
   * @return value or empty string
   */
  public static String getTokenValue(Movie movie, String token) {
    try {
      Engine engine = createEngine();
      engine.setModelAdaptor(new TmmModelAdaptor());

      engine.setOutputAppender(new TmmOutputAppender() {
        @Override
        protected String replaceInvalidCharacters(String text) {
          return MovieRenamer.replaceInvalidCharacters(text);
        }
      });

      Map<String, Object> root = new HashMap<>();
      root.put("movie", movie);

      // only offer movie set for movies with more than 1 movies or if setting is set
      if (movie.getMovieSet() != null
          && (movie.getMovieSet().getMovies().size() > 1 || MovieModuleManager.getInstance().getSettings().isRenamerCreateMoviesetForSingleMovie())) {
        root.put("movieSet", movie.getMovieSet());
      }

      return engine.transform(JmteUtils.morphTemplate(token, TOKEN_MAP), root);
    }
    catch (Exception e) {
      LOGGER.warn("unable to process token: {} - {}", token, e.getMessage());
      return token;
    }
  }

  /**
   * create the {@link Engine} to be used with JMTE
   *
   * @return the pre-created Engine
   */
  public static Engine createEngine() {
    Engine engine = Engine.createEngine();
    engine.registerRenderer(Number.class, new ZeroNumberRenderer());
    engine.registerNamedRenderer(new MovieNamedFirstCharacterRenderer());
    engine.registerNamedRenderer(new MovieNamedIndexOfMovieSetRenderer());
    engine.registerNamedRenderer(new MovieNamedIndexOfMovieSetWithDummyRenderer());
    engine.registerNamedRenderer(new NamedArrayRenderer());
    engine.registerNamedRenderer(new NamedArrayUniqueRenderer());
    engine.registerNamedRenderer(new NamedBitrateRenderer());
    engine.registerNamedRenderer(new NamedDateRenderer());
    engine.registerNamedRenderer(new NamedFilesizeRenderer());
    engine.registerNamedRenderer(new NamedFramerateRenderer());
    engine.registerNamedRenderer(new NamedLowerCaseRenderer());
    engine.registerNamedRenderer(new NamedNumberRenderer());
    engine.registerNamedRenderer(new NamedReplacementRenderer());
    engine.registerNamedRenderer(new NamedSplitRenderer());
    engine.registerNamedRenderer(new NamedTitleCaseRenderer());
    engine.registerNamedRenderer(new NamedUpperCaseRenderer());
    engine.registerNamedRenderer(new ChainedNamedRenderer(engine.getAllNamedRenderers()));

    engine.registerAnnotationProcessor(new RegexpProcessor());
    engine.setErrorHandler(new DefaultErrorHandler() {
      @Override
      public void error(ErrorMessage errorMessage, Token token, Map<String, Object> parameters) throws ParseException {
        throw new ParseException(new ResourceBundleMessage(errorMessage.key).withModel(parameters).onToken(token));
      }
    });

    return engine;
  }

  /**
   * Creates the new file/folder name according to template string
   *
   * @param template
   *          the template
   * @param movie
   *          the movie
   * @param forFilename
   *          replace for filename (=true)? or for a foldername (=false)<br>
   *          Former does replace ALL directory separators
   * @return the string
   */
  public static String createDestination(String template, Movie movie, boolean forFilename) {

    String newDestination = getTokenValue(movie, template);

    // replace empty brackets
    newDestination = newDestination.replaceAll("\\([ ]?\\)", "");
    newDestination = newDestination.replaceAll("\\[[ ]?\\]", "");
    newDestination = newDestination.replaceAll("\\{[ ]?\\}", "");

    // if there are multiple file separators in a row - strip them out
    if (SystemUtils.IS_OS_WINDOWS) {
      if (!forFilename) {
        // trim whitespace around directory sep
        newDestination = newDestination.replaceAll("\\s+\\\\", "\\\\");
        newDestination = newDestination.replaceAll("\\\\\\s+", "\\\\");
        // remove separators in front of path separators
        newDestination = newDestination.replaceAll("[ \\.\\-_]+\\\\", "\\\\");
      }
      // we need to mask it in windows
      newDestination = newDestination.replaceAll("\\\\{2,}", "\\\\");
      newDestination = newDestination.replaceAll("^\\\\", "");
    }
    else {
      if (!forFilename) {
        // trim whitespace around directory sep
        newDestination = newDestination.replaceAll("\\s+/", "/");
        newDestination = newDestination.replaceAll("/\\s+", "/");
        // remove separators in front of path separators
        newDestination = newDestination.replaceAll("[ \\.\\-_]+/", "/");
      }
      newDestination = newDestination.replaceAll("/{2,}", "/");
      newDestination = newDestination.replaceAll("^/", "");
    }

    // replace ALL directory separators, if we generate this for filenames!
    if (forFilename) {
      newDestination = replacePathSeparators(newDestination);
    }

    // replace spaces with underscores if needed (filename only)
    if (forFilename && MovieModuleManager.getInstance().getSettings().isRenamerFilenameSpaceSubstitution()) {
      String replacement = MovieModuleManager.getInstance().getSettings().getRenamerFilenameSpaceReplacement();
      newDestination = newDestination.replace(" ", replacement);

      // also replace now multiple replacements with one to avoid strange looking results
      // example:
      // Abraham Lincoln - Vampire Hunter -> Abraham-Lincoln---Vampire-Hunter
      newDestination = newDestination.replaceAll(Pattern.quote(replacement) + "+", replacement);
    }
    else if (!forFilename && MovieModuleManager.getInstance().getSettings().isRenamerPathnameSpaceSubstitution()) {
      String replacement = MovieModuleManager.getInstance().getSettings().getRenamerPathnameSpaceReplacement();
      newDestination = newDestination.replace(" ", replacement);

      // also replace now multiple replacements with one to avoid strange looking results
      // example:
      // Abraham Lincoln - Vapire Hunter -> Abraham-Lincoln---Vampire-Hunter
      newDestination = newDestination.replaceAll(Pattern.quote(replacement) + "+", replacement);
    }

    // replace all leading/trailing separators (except the underscore which could be valid in the front)
    newDestination = newDestination.replaceAll("^[ \\.\\-]+", "");
    newDestination = newDestination.replaceAll("[ \\.\\-_]+$", "");

    // ASCII replacement
    if (MovieModuleManager.getInstance().getSettings().isAsciiReplacement()) {
      newDestination = StrgUtils.convertToAscii(newDestination, false);
    }

    // the colon is handled by JMTE, but it looks like some users are stupid enough to add this to the pattern itself
    newDestination = newDestination.replace(": ", " - "); // nicer
    newDestination = newDestination.replace(":", "-"); // nicer

    // replace new lines
    newDestination = newDestination.replaceAll("\r?\n", " ");

    // replace multiple spaces with a single one
    newDestination = newDestination.replaceAll(" +", " ");

    if (SystemUtils.IS_OS_WINDOWS) {
      // remove illegal characters on Windows
      newDestination = newDestination.replace("\"", " ");
    }

    return newDestination.strip();
  }

  /**
   * moves a file.
   *
   * @param oldFilename
   *          the old filename
   * @param newFilename
   *          the new filename
   * @return true, when we moved file
   */
  static boolean moveFile(Path oldFilename, Path newFilename) {
    try {
      // create parent if needed
      if (!Files.exists(newFilename.getParent())) {
        Files.createDirectory(newFilename.getParent());
      }
      boolean ok = Utils.moveFileSafe(oldFilename, newFilename);
      if (ok) {
        return true;
      }
      else {
        LOGGER.error("Could not move MF '{}' to '{}'", oldFilename, newFilename);
        return false; // rename failed
      }
    }
    catch (Exception e) {
      LOGGER.error("error moving file '{}' - '{}'", oldFilename.toAbsolutePath(), e.getMessage());
      MessageManager.instance
          .pushMessage(new Message(MessageLevel.ERROR, oldFilename, "message.renamer.failedrename", new String[] { ":", e.getLocalizedMessage() }));
      return false; // rename failed
    }
  }

  /**
   * moves a directory.
   *
   * @param oldName
   *          the old filename
   * @param newName
   *          the new filename
   * @return true, when we moved file
   */
  static boolean moveDirectory(Path oldName, Path newName) {
    try {
      // create parent if needed
      if (!Files.exists(newName.getParent())) {
        Files.createDirectory(newName.getParent());
      }
      boolean ok = Utils.moveDirectorySafe(oldName, newName);
      if (ok) {
        return true;
      }
      else {
        LOGGER.error("Could not move folder '{}' to '{}'", oldName, newName);
        return false; // rename failed
      }
    }
    catch (Exception e) {
      LOGGER.error("error moving folder '{}' - '{}'", oldName.toAbsolutePath(), e.getMessage());
      MessageManager.instance
          .pushMessage(new Message(MessageLevel.ERROR, oldName, "message.renamer.failedrename", new String[] { ":", e.getLocalizedMessage() }));
      return false; // rename failed
    }
  }

  /**
   * copies a file.
   *
   * @param oldFilename
   *          the old filename
   * @param newFilename
   *          the new filename
   * @return true, when we copied file OR DEST IS EXISTING
   */
  static boolean copyFile(Path oldFilename, Path newFilename) {
    if (!oldFilename.toAbsolutePath().toString().equals(newFilename.toAbsolutePath().toString())) {
      LOGGER.debug("copy file {} to {}", oldFilename, newFilename);
      if (oldFilename.equals(newFilename)) {
        // windows: name differs, but File() is the same!!!
        // use move in this case, which handles this
        return moveFile(oldFilename, newFilename);
      }
      try {
        // create parent if needed
        if (!Files.exists(newFilename.getParent())) {
          Files.createDirectory(newFilename.getParent());
        }
        Utils.copyFileSafe(oldFilename, newFilename, true);
        return true;
      }
      catch (Exception e) {
        return false;
      }
    }
    else { // file is the same, return true to keep file
      return true;
    }
  }

  /**
   * Check if the folder rename pattern is unique<br>
   * Unique true, when having at least a $T/$E-$Y combo or $I imdbId<br>
   *
   * @param pattern
   *          the pattern to check the uniqueness for
   * @return true/false
   */
  public static boolean isFolderPatternUnique(String pattern) {
    return TITLE_PATTERN.matcher(pattern).find() && YEAR_ID_PATTERN.matcher(pattern).find();
  }

  /**
   * Check if the FILE rename pattern is valid<br>
   * What means, pattern has at least title set (${title}|${originalTitle}|${titleSortable})<br>
   * "empty" is considered as invalid - so not renaming files
   *
   * @return true/false
   */
  public static boolean isFilePatternValid() {
    return isFilePatternValid(MovieModuleManager.getInstance().getSettings().getRenamerFilename());
  }

  /**
   * Check if the FILE rename pattern is valid<br>
   * What means, that at least title (${title}|${originalTitle}|${titleSortable})<br>
   * or original filename has been set "empty" is considered as invalid - so not renaming files
   *
   * @return true/false
   */
  public static boolean isFilePatternValid(String pattern) {
    return TITLE_PATTERN.matcher(pattern).find() || ORIGINAL_FILENAME_PATTERN.matcher(pattern).find();
  }

  /**
   * replaces all invalid/illegal characters for filenames/foldernames with ""<br>
   * except the colon, which will be changed to a dash
   *
   * @param source
   *          string to clean
   * @return cleaned string
   */
  public static String replaceInvalidCharacters(String source) {
    String result = source;

    if ("-".equals(MovieModuleManager.getInstance().getSettings().getRenamerColonReplacement())) {
      result = result.replace(": ", " - "); // nicer
      result = result.replace(":", "-"); // nicer
    }
    else {
      result = result.replace(":", MovieModuleManager.getInstance().getSettings().getRenamerColonReplacement());
    }

    return result.replaceAll("([\":<>|?*])", "");
  }

  /**
   * replace all path separators in the given {@link String} with a space
   *
   * @param source
   *          the original {@link String}
   * @return the cleaned {@link String}
   */
  public static String replacePathSeparators(String source) {
    String result = source.replaceAll("\\/", " "); // NOSONAR
    return result.replaceAll("\\\\", " "); // NOSONAR
  }
}
