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

import static org.tinymediamanager.core.MediaFileType.SEASON_BANNER;
import static org.tinymediamanager.core.MediaFileType.SEASON_FANART;
import static org.tinymediamanager.core.MediaFileType.SEASON_POSTER;
import static org.tinymediamanager.core.MediaFileType.SEASON_THUMB;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
import org.tinymediamanager.core.threading.ThreadUtils;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.core.tvshow.filenaming.TvShowEpisodeThumbNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowExtraFanartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonBannerNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonFanartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonNfoNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonPosterNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonThumbNaming;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.StrgUtils;

import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.NamedRenderer;
import com.floreysoft.jmte.RenderFormatInfo;
import com.floreysoft.jmte.extended.ChainedNamedRenderer;

/**
 * The TvShowRenamer Works on per MediaFile basis
 * 
 * @author Myron Boyle
 */
public class TvShowRenamer {
  private static final Logger              LOGGER         = LoggerFactory.getLogger(TvShowRenamer.class);
  private static final Map<String, String> TOKEN_MAP      = createTokenMap();

  private static final String[]            seasonNumbers  = { "seasonNr", "seasonNr2", "seasonNrDvd", "seasonNrDvd2", "episode.season",
      "episode.dvdSeason" };
  private static final String[]            episodeNumbers = { "episodeNr", "episodeNr2", "episodeNrDvd", "episodeNrDvd2", "episode.episode",
      "episode.dvdEpisode", "absoluteNr", "absoluteNr2", "episode.absoluteNumber" };
  private static final String[]            episodeTitles  = { "title", "originalTitle", "titleSortable", "episode.title", "episode.originalTitle",
      "episode.titleSortable" };
  private static final String[]            episodeAired   = { "airedDate", "episode.firstAired" };

  private static final Pattern             epDelimiter    = Pattern.compile("(\\s?(folge|episode|[epx]+)\\s?)\\$\\{.*?\\}", Pattern.CASE_INSENSITIVE);
  private static final Pattern             seDelimiter    = Pattern.compile("((staffel|season|s)\\s?)\\$\\{.*?\\}", Pattern.CASE_INSENSITIVE);

  private static final List<String>        DISC_FOLDERS   = Arrays.asList("bdmv", "video_ts", "hvdvd_ts");

  private TvShowRenamer() {
    throw new IllegalAccessError();
  }

  /**
   * initialize the token map for the renamer
   *
   * @return the token map
   */
  private static Map<String, String> createTokenMap() {
    Map<String, String> tokenMap = new HashMap<>();
    // TV show tags
    tokenMap.put("showTitle", "tvShow.title");
    tokenMap.put("showOriginalTitle", "tvShow.originalTitle");
    tokenMap.put("showTitleSortable", "tvShow.titleSortable");
    tokenMap.put("showYear", "tvShow.year");
    tokenMap.put("parent", "tvShow.parent");
    tokenMap.put("showNote", "tvShow.note");
    tokenMap.put("showStatus", "tvShow.status");
    tokenMap.put("showRating", "tvShow.rating.rating");
    tokenMap.put("showImdb", "tvShow.imdbId");
    tokenMap.put("showTmdb", "tvShow.tmdbId");
    tokenMap.put("showTvdb", "tvShow.tvdbId");
    tokenMap.put("showCertification", "tvShow.certification");
    tokenMap.put("showTags", "tvShow.tags");
    tokenMap.put("showGenres", "tvShow.genres");
    tokenMap.put("showGenresAsString", "tvShow.genresAsString");
    tokenMap.put("showProductionCompany", "tvShow.productionCompany");
    tokenMap.put("showProductionCompanyAsArray", "tvShow.productionCompanyAsArray");

    // Season tags
    tokenMap.put("seasonName", "season.title");

    // episode tags
    tokenMap.put("episodeNr", "episode.episode");
    tokenMap.put("episodeNr2", "episode.episode;number(%02d)");
    tokenMap.put("episodeNrAired", "episode.airedEpisode");
    tokenMap.put("episodeNrAired2", "episode.airedEpisode;number(%02d)");
    tokenMap.put("episodeNrDvd", "episode.dvdEpisode");
    tokenMap.put("episodeNrDvd2", "episode.dvdEpisode;number(%02d)");
    tokenMap.put("seasonNr", "episode.season;number(%d)");
    tokenMap.put("seasonNr2", "episode.season;number(%02d)");
    tokenMap.put("seasonNrAired", "episode.airedSeason;number(%d)");
    tokenMap.put("seasonNrAired2", "episode.airedSeason;number(%02d)");
    tokenMap.put("seasonNrDvd", "episode.dvdSeason;number(%d)");
    tokenMap.put("seasonNrDvd2", "episode.dvdSeason;number(%02d)");
    tokenMap.put("absoluteNr", "episode.absoluteNumber;number(%d)");
    tokenMap.put("absoluteNr2", "episode.absoluteNumber;number(%02d)");
    tokenMap.put("title", "episode.title");
    tokenMap.put("originalTitle", "episode.originalTitle");
    tokenMap.put("originalFilename", "episode.originalFilename");
    tokenMap.put("originalBasename", "episode.originalBasename");
    tokenMap.put("titleSortable", "episode.titleSortable");
    tokenMap.put("year", "episode.year");
    tokenMap.put("airedDate", "episode.firstAired;date(yyyy-MM-dd)");
    tokenMap.put("episodeRating", "episode.rating.rating");
    tokenMap.put("episodeImdb", "episode.imdbId");
    tokenMap.put("episodeTmdb", "episode.tmdbId");
    tokenMap.put("episodeTvdb", "episode.tvdbId");
    tokenMap.put("episodeTags", "episode.tags");

    tokenMap.put("videoCodec", "episode.mediaInfoVideoCodec");
    tokenMap.put("videoFormat", "episode.mediaInfoVideoFormat");
    tokenMap.put("videoResolution", "episode.mediaInfoVideoResolution");
    tokenMap.put("aspectRatio", "episode.mediaInfoAspectRatioAsString");
    tokenMap.put("aspectRatio2", "episode.mediaInfoAspectRatio2AsString");
    tokenMap.put("videoBitDepth", "episode.mediaInfoVideoBitDepth");
    tokenMap.put("videoBitRate", "episode.mediaInfoVideoBitrate;bitrate");
    tokenMap.put("framerate", "episode.mediaInfoFrameRate;framerate");

    tokenMap.put("audioCodec", "episode.mediaInfoAudioCodec");
    tokenMap.put("audioCodecList", "episode.mediaInfoAudioCodecList");
    tokenMap.put("audioCodecsAsString", "episode.mediaInfoAudioCodecList;array");
    tokenMap.put("audioChannels", "episode.mediaInfoAudioChannels");
    tokenMap.put("audioChannelList", "episode.mediaInfoAudioChannelList");
    tokenMap.put("audioChannelsAsString", "episode.mediaInfoAudioChannelList;array");
    tokenMap.put("audioChannelsDot", "episode.mediaInfoAudioChannelsDot");
    tokenMap.put("audioChannelDotList", "episode.mediaInfoAudioChannelDotList");
    tokenMap.put("audioChannelsDotAsString", "episode.mediaInfoAudioChannelDotList;array");
    tokenMap.put("audioLanguage", "episode.mediaInfoAudioLanguage");
    tokenMap.put("audioLanguageList", "episode.mediaInfoAudioLanguageList");
    tokenMap.put("audioLanguagesAsString", "episode.mediaInfoAudioLanguageList;array");

    tokenMap.put("subtitleLanguageList", "episode.mediaInfoSubtitleLanguageList");
    tokenMap.put("subtitleLanguagesAsString", "episode.mediaInfoSubtitleLanguageList;array");
    tokenMap.put("3Dformat", "episode.video3DFormat");
    tokenMap.put("hdr", "episode.videoHDR");
    tokenMap.put("hdrformat", "episode.videoHDRFormat");
    tokenMap.put("filesize", "episode.videoFilesize;filesize");

    tokenMap.put("mediaSource", "episode.mediaSource");
    tokenMap.put("note", "episode.note");

    return tokenMap;
  }

  /**
   * get the token map in an unmodifiable variant
   *
   * @return the token map
   */
  public static Map<String, String> getTokenMap() {
    return Collections.unmodifiableMap(TOKEN_MAP);
  }

  public static Map<String, String> getTokenMapReversed() {
    return Collections.unmodifiableMap(TOKEN_MAP.entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey)));
  }

  /**
   * add leadingZero if only 1 char
   *
   * @param num
   *          the number
   * @return the string with a leading 0
   */
  private static String lz(int num) {
    return String.format("%02d", num);
  }

  /**
   * renames the TvShow root folder and updates all TvShow related mediaFiles
   *
   * @param tvShow
   *          the show
   */
  public static void renameTvShow(TvShow tvShow) {
    // rename the TV show folder
    renameTvShowRoot(tvShow);

    // rename TV show media files
    renameTvShowMediaFiles(tvShow);

    // rename the season media files
    renameSeasonMediaFiles(tvShow);

    // cleanup
    cleanupUnwantedFiles(tvShow);

    tvShow.saveToDb();
  }

  /**
   * renames the TvShow root folder and updates all mediaFiles
   *
   * @param show
   *          the show
   */
  private static void renameTvShowRoot(TvShow show) {
    // skip renamer, if all templates are empty!
    if (TvShowModuleManager.getInstance().getSettings().getRenamerFilename().isEmpty()
        && TvShowModuleManager.getInstance().getSettings().getRenamerSeasonFoldername().isEmpty()
        && TvShowModuleManager.getInstance().getSettings().getRenamerTvShowFoldername().isEmpty()) {
      LOGGER.info("NOT renaming TvShow '{}' - renaming patterns are empty!", show.getTitle());
      return;
    }

    LOGGER.debug("TV show year: {}", show.getYear());
    LOGGER.debug("TV show path: {}", show.getPathNIO());
    String newPathname = getTvShowFoldername(TvShowModuleManager.getInstance().getSettings().getRenamerTvShowFoldername(), show);
    String oldPathname = show.getPathNIO().toString();

    if (!newPathname.isEmpty()) {
      Path srcDir = Paths.get(oldPathname);
      Path destDir = Paths.get(newPathname);
      // move directory if needed
      if (!srcDir.toAbsolutePath().toString().equals(destDir.toAbsolutePath().toString())) {
        try {
          // create parent if needed
          if (!Files.exists(destDir.getParent())) {
            Files.createDirectory(destDir.getParent());
          }
          boolean ok = Utils.moveDirectorySafe(srcDir, destDir);
          if (ok) {
            show.updateMediaFilePath(srcDir, destDir); // TvShow MFs
            show.setPath(newPathname);

            for (TvShowSeason tvShowSeason : show.getSeasons()) {
              tvShowSeason.updateMediaFilePath(srcDir, destDir);
            }

            for (TvShowEpisode episode : show.getEpisodes()) {
              episode.replacePathForRenamedTvShowRoot(srcDir, destDir);
              episode.updateMediaFilePath(srcDir, destDir);
            }

            // ######################################################################
            // ## build up image cache
            // ######################################################################
            if (Settings.getInstance().isImageCache()) {
              for (MediaFile gfx : show.getMediaFiles()) {
                ImageCache.cacheImageSilently(gfx, false);
              }
            }
          }
        }
        catch (Exception e) {
          LOGGER.error("error moving folder: {}", e.getMessage());
          MessageManager.instance
              .pushMessage(new Message(MessageLevel.ERROR, srcDir, "message.renamer.failedrename", new String[] { ":", e.getLocalizedMessage() }));
        }
      }
    }
  }

  /**
   * rename all artwork for this TV show
   *
   * @param tvShow
   *          the TV show to rename the artwork for
   */
  private static void renameTvShowMediaFiles(TvShow tvShow) {
    // all the good & needed mediafiles
    List<MediaFile> needed = new ArrayList<>();
    List<MediaFile> cleanup = new ArrayList<>(tvShow.getMediaFiles());
    cleanup.removeAll(Collections.singleton((MediaFile) null)); // remove all NULL ones!

    // ######################################################################
    // ## rename NFO (copy 1:N) - only TMM NFOs
    // ######################################################################
    // we need to find the newest, valid TMM NFO
    MediaFile nfo = MediaFile.EMPTY_MEDIAFILE;
    for (MediaFile mf : tvShow.getMediaFiles(MediaFileType.NFO)) {
      if (mf.getFiledate() >= nfo.getFiledate()) {
        nfo = new MediaFile(mf);
      }
    }

    if (nfo != MediaFile.EMPTY_MEDIAFILE) { // one valid found? copy our NFO to all variants
      List<MediaFile> newMFs = generateFilename(tvShow, nfo); // 1:N
      for (MediaFile newMF : newMFs) {
        boolean ok = copyFile(nfo.getFileAsPath(), newMF.getFileAsPath());
        if (ok) {
          needed.add(newMF);
        }
        else {
          // FIXME: what to do? not copied/exception... keep it for now...
          needed.add(nfo);
        }
      }
    }
    else {
      LOGGER.trace("No valid NFO found for this TV show");
    }

    // ######################################################################
    // ## rename well known types
    // ######################################################################
    for (MediaFile mf : tvShow.getMediaFiles()) {
      if (mf == null) {
        continue;
      }

      LOGGER.trace("Rename 1:N {} {}", mf.getType(), mf.getFileAsPath());
      List<MediaFile> newMFs = generateFilename(tvShow, mf); // 1:N
      for (MediaFile newMF : newMFs) {
        boolean ok = copyFile(mf.getFileAsPath(), newMF.getFileAsPath());
        if (ok) {
          needed.add(newMF);
        }
        else {
          // FIXME: what to do? not copied/exception... keep it for now...
          needed.add(mf);
        }
      }
    }

    // ######################################################################
    // ## invalidate image cache
    // ######################################################################
    for (MediaFile gfx : tvShow.getMediaFiles()) {
      ImageCache.invalidateCachedImage(gfx);
    }

    // remove duplicate MediaFiles
    Set<MediaFile> newMFs = new LinkedHashSet<>(needed);
    needed.clear();
    needed.addAll(newMFs);

    // ######################################################################
    // ## CLEANUP - delete all files marked for cleanup, which are not "needed"
    // ######################################################################
    LOGGER.info("Cleanup...");
    for (int i = cleanup.size() - 1; i >= 0; i--) {
      // cleanup files which are not needed
      if (!needed.contains(cleanup.get(i))) {
        MediaFile cl = cleanup.get(i);
        if (Files.exists(cl.getFileAsPath())) { // unneeded, but for not displaying wrong deletes in logger...
          LOGGER.debug("Deleting {}", cl.getFileAsPath());
          Utils.deleteFileWithBackup(cl.getFileAsPath(), tvShow.getDataSource());
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
          LOGGER.error("cleanup of {} - {}", cl.getFileAsPath(), e.getMessage());
        }
      }
    }

    // delete empty subfolders
    try {
      Utils.deleteEmptyDirectoryRecursive(tvShow.getPathNIO());
    }
    catch (Exception e) {
      LOGGER.warn("could not delete empty subfolders: {}", e.getMessage());
    }

    tvShow.removeAllMediaFiles();

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

    tvShow.addToMediaFiles(needed);
  }

  /**
   * generate all possible file names (as {@link MediaFile}s) for the given {@link MediaFile} according to the settings
   *
   * @param tvShow
   *          the {@link TvShow}
   * @param original
   *          the original {@link MediaFile}
   * @return a {@link List} of all {@link MediaFile}s which are needed according to the settings
   */
  public static List<MediaFile> generateFilename(TvShow tvShow, MediaFile original) {
    List<MediaFile> neededMediaFiles = new ArrayList<>();
    List<? extends IFileNaming> filenamings = null;

    switch (original.getType()) {
      case POSTER:
        filenamings = TvShowModuleManager.getInstance().getSettings().getPosterFilenames();
        break;

      case FANART:
        filenamings = TvShowModuleManager.getInstance().getSettings().getFanartFilenames();
        break;

      case BANNER:
        filenamings = TvShowModuleManager.getInstance().getSettings().getBannerFilenames();
        break;

      case CLEARLOGO:
      case LOGO:
        filenamings = TvShowModuleManager.getInstance().getSettings().getClearlogoFilenames();
        break;

      case CLEARART:
        filenamings = TvShowModuleManager.getInstance().getSettings().getClearartFilenames();
        break;

      case THUMB:
        filenamings = TvShowModuleManager.getInstance().getSettings().getThumbFilenames();
        break;

      case DISC:
        filenamings = TvShowModuleManager.getInstance().getSettings().getDiscartFilenames();
        break;

      case CHARACTERART:
        filenamings = TvShowModuleManager.getInstance().getSettings().getCharacterartFilenames();
        break;

      case KEYART:
        filenamings = TvShowModuleManager.getInstance().getSettings().getKeyartFilenames();
        break;

      case TRAILER:
        filenamings = TvShowModuleManager.getInstance().getSettings().getTrailerFilenames();
        break;

      case NFO:
        filenamings = TvShowModuleManager.getInstance().getSettings().getNfoFilenames();
        break;

      case EXTRAFANART:
        neededMediaFiles.addAll(generateExtrafanartFilename(tvShow, original));
        break;

      default:
        neededMediaFiles.add(original);
        break;

    }

    if (filenamings != null) {
      for (IFileNaming name : filenamings) {
        String newFilename = name.getFilename(tvShow.getFoldername(), getMediaFileExtension(original));

        if (StringUtils.isNotBlank(newFilename)) {
          MediaFile newMediaFile = new MediaFile(original);
          newMediaFile.setFile(tvShow.getPathNIO().resolve(newFilename));
          neededMediaFiles.add(newMediaFile);
        }
      }
    }

    return neededMediaFiles;
  }

  /**
   * copy the given extrafanarts
   *
   * @param tvShow
   *          the {@link TvShow} to generate the extrafanart filenames for
   *
   * @param original
   *          the original {@link MediaFile} to copy
   *
   * @return a {@link List} of all {@link MediaFile}s created while copying (or the original if no copy needed)
   */
  private static List<MediaFile> generateExtrafanartFilename(TvShow tvShow, MediaFile original) {
    if (TvShowModuleManager.getInstance().getSettings().getExtraFanartFilenames().isEmpty()) {
      return Collections.emptyList();
    }

    int index = TvShowArtworkHelper.getIndexOfArtwork(original.getFilename());
    if (index > 0) {
      // at the moment, we just support 1 naming scheme here! if we decide to enhance that, we will need to enhance the TvShowExtraImageFetcherTask
      // too
      TvShowExtraFanartNaming name = TvShowModuleManager.getInstance().getSettings().getExtraFanartFilenames().get(0);

      String newFilename = name.getFilename("", getMediaFileExtension(original));
      if (StringUtils.isNotBlank(newFilename)) {

        String basename = FilenameUtils.getBaseName(newFilename);
        newFilename = basename + index + "." + getMediaFileExtension(original);

        // create an empty extrafanarts folder if the right naming has been chosen
        Path folder;
        if (name == TvShowExtraFanartNaming.FOLDER_EXTRAFANART) {
          folder = tvShow.getPathNIO().resolve("extrafanart");
        }
        else {
          folder = tvShow.getPathNIO();
        }

        MediaFile newMediaFile = new MediaFile(original);
        newMediaFile.setFile(folder.resolve(newFilename));
        return Collections.singletonList(newMediaFile);
      }
    }

    return Collections.emptyList();
  }

  /**
   * rename the season artwork for this TV show
   * 
   * @param tvShow
   *          the TV show to rename the season artwork for
   */
  private static void renameSeasonMediaFiles(TvShow tvShow) {
    // all the good & needed mediafiles
    Set<MediaFile> needed = new LinkedHashSet<>();
    List<MediaFile> cleanup = new ArrayList<>();

    // NFO
    for (var tvShowSeason : tvShow.getSeasons()) {
      Set<MediaFile> neededSeason = new LinkedHashSet<>();
      List<MediaFile> cleanupSeason = new ArrayList<>();

      MediaFile nfo = MediaFile.EMPTY_MEDIAFILE;
      for (MediaFile mf : tvShowSeason.getMediaFiles(MediaFileType.NFO)) {
        cleanupSeason.add(mf);

        if (mf.getFiledate() >= nfo.getFiledate()) {
          nfo = new MediaFile(mf);
        }
      }

      // one valid found? copy our NFO to all variants
      // and do we want to write those files?
      if (nfo != MediaFile.EMPTY_MEDIAFILE
          && (!tvShowSeason.getEpisodes().isEmpty() || TvShowModuleManager.getInstance().getSettings().isCreateMissingSeasonItems())) {

        for (TvShowSeasonNfoNaming naming : TvShowModuleManager.getInstance().getSettings().getSeasonNfoFilenames()) {
          String filename = naming.getFilename(tvShowSeason, "nfo");
          if (StringUtils.isNotBlank(filename)) {
            MediaFile newMf = new MediaFile(nfo);
            newMf.setFile(Paths.get(tvShow.getPath(), filename));
            boolean ok = copyFile(nfo.getFileAsPath(), newMf.getFileAsPath());
            if (ok) {
              neededSeason.add(newMf);
            }
          }
        }
      }

      cleanupSeason.forEach(tvShowSeason::removeFromMediaFiles);
      neededSeason.forEach(tvShowSeason::addToMediaFiles);

      needed.addAll(neededSeason);
      cleanup.addAll(cleanupSeason);
    }

    // artwork
    List<MediaFileType> types = Arrays.asList(SEASON_POSTER, SEASON_FANART, SEASON_BANNER, SEASON_THUMB);

    for (var type : types) {
      for (var tvShowSeason : tvShow.getSeasons()) {
        Set<MediaFile> neededSeason = new LinkedHashSet<>();
        List<MediaFile> cleanupSeason = new ArrayList<>();

        MediaFile artworkFile = null;

        for (var mf : tvShowSeason.getMediaFiles(type)) {
          cleanupSeason.add(mf);

          if (artworkFile == null) {
            artworkFile = mf;
          }
        }

        // do we ant to write the artwork file at all?
        if (artworkFile != null
            && (!tvShowSeason.getEpisodes().isEmpty() || TvShowModuleManager.getInstance().getSettings().isCreateMissingSeasonItems())) {
          String filename;
          switch (type) {
            case SEASON_POSTER -> {
              for (TvShowSeasonPosterNaming naming : TvShowModuleManager.getInstance().getSettings().getSeasonPosterFilenames()) {
                filename = naming.getFilename(tvShowSeason, artworkFile.getExtension());
                if (StringUtils.isNotBlank(filename)) {
                  MediaFile newMf = new MediaFile(artworkFile);
                  newMf.setFile(Paths.get(tvShow.getPath(), filename));
                  boolean ok = copyFile(artworkFile.getFileAsPath(), newMf.getFileAsPath());
                  if (ok) {
                    neededSeason.add(newMf);
                  }
                }
              }
            }
            case SEASON_FANART -> {
              for (TvShowSeasonFanartNaming naming : TvShowModuleManager.getInstance().getSettings().getSeasonFanartFilenames()) {
                filename = naming.getFilename(tvShowSeason, artworkFile.getExtension());
                if (StringUtils.isNotBlank(filename)) {
                  MediaFile newMf = new MediaFile(artworkFile);
                  newMf.setFile(Paths.get(tvShow.getPath(), filename));
                  boolean ok = copyFile(artworkFile.getFileAsPath(), newMf.getFileAsPath());
                  if (ok) {
                    neededSeason.add(newMf);
                  }
                }
              }
            }
            case SEASON_BANNER -> {
              for (TvShowSeasonBannerNaming naming : TvShowModuleManager.getInstance().getSettings().getSeasonBannerFilenames()) {
                filename = naming.getFilename(tvShowSeason, artworkFile.getExtension());
                if (StringUtils.isNotBlank(filename)) {
                  MediaFile newMf = new MediaFile(artworkFile);
                  newMf.setFile(Paths.get(tvShow.getPath(), filename));
                  boolean ok = copyFile(artworkFile.getFileAsPath(), newMf.getFileAsPath());
                  if (ok) {
                    neededSeason.add(newMf);
                  }
                }
              }
            }
            case SEASON_THUMB -> {
              for (TvShowSeasonThumbNaming naming : TvShowModuleManager.getInstance().getSettings().getSeasonThumbFilenames()) {
                filename = naming.getFilename(tvShowSeason, artworkFile.getExtension());
                if (StringUtils.isNotBlank(filename)) {
                  MediaFile newMf = new MediaFile(artworkFile);
                  newMf.setFile(Paths.get(tvShow.getPath(), filename));
                  boolean ok = copyFile(artworkFile.getFileAsPath(), newMf.getFileAsPath());
                  if (ok) {
                    neededSeason.add(newMf);
                  }
                }
              }
            }
            default -> {
              // do nothing
            }
          }
        }

        cleanupSeason.forEach(tvShowSeason::removeFromMediaFiles);
        neededSeason.forEach(tvShowSeason::addToMediaFiles);

        cleanup.addAll(cleanupSeason);
        needed.addAll(neededSeason);
      }
    }

    // ######################################################################
    // ## invalidate image cache
    // ######################################################################
    for (MediaFile gfx : tvShow.getMediaFiles()) {
      ImageCache.invalidateCachedImage(gfx);
    }

    // ######################################################################
    // ## CLEANUP - delete all files marked for cleanup, which are not "needed"
    // ######################################################################
    LOGGER.info("Cleanup...");
    List<Path> existingFiles = Utils.listFilesRecursive(tvShow.getPathNIO());
    for (int i = cleanup.size() - 1; i >= 0; i--) {
      // cleanup files which are not needed
      if (!needed.contains(cleanup.get(i))) {
        MediaFile cl = cleanup.get(i);
        if (existingFiles.contains(cl.getFileAsPath())) {
          LOGGER.debug("Deleting {}", cl.getFileAsPath());
          tvShow.removeFromMediaFiles(cl);

          for (TvShowSeason season : tvShow.getSeasons()) {
            season.removeFromMediaFiles(cl);
          }

          Utils.deleteFileWithBackup(cl.getFileAsPath(), tvShow.getDataSource());
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
          LOGGER.error("cleanup of {} - {}", cl.getFileAsPath(), e.getMessage());
        }
      }
    }

    // delete empty subfolders
    try {
      Utils.deleteEmptyDirectoryRecursive(tvShow.getPathNIO());
    }
    catch (Exception e) {
      LOGGER.warn("could not delete empty subfolders: {}", e.getMessage());
    }

    // and rebuild the whole artwork maps
    for (MediaFile mf : needed) {
      String foldername = tvShow.getPathNIO().relativize(mf.getFileAsPath().getParent()).toString();
      int season = TvShowHelpers.detectSeasonFromFileAndFolder(mf.getFilename(), foldername);

      tvShow.removeFromMediaFiles(mf);

      for (TvShowSeason tvShowSeason : tvShow.getSeasons()) {
        tvShowSeason.removeFromMediaFiles(mf);
      }

      if (season != Integer.MIN_VALUE) {
        TvShowSeason tvShowSeason = tvShow.getSeason(season);
        tvShowSeason.addToMediaFiles(mf);
      }
    }

    // ######################################################################
    // ## build up image cache
    // ######################################################################
    if (Settings.getInstance().isImageCache()) {
      for (MediaFile gfx : tvShow.getMediaFiles()) {
        ImageCache.cacheImageSilently(gfx, false);
      }
    }
  }

  /**
   * Rename Episode (PLUS all Episodes having the same MediaFile!!!).
   * 
   * @param episode
   *          the Episode
   */
  public static void renameEpisode(TvShowEpisode episode) {
    // skip renamer, if all episode related templates are empty!
    if (TvShowModuleManager.getInstance().getSettings().getRenamerFilename().isEmpty()
        && TvShowModuleManager.getInstance().getSettings().getRenamerSeasonFoldername().isEmpty()
        && TvShowModuleManager.getInstance().getSettings().getRenamerTvShowFoldername().isEmpty()) {
      LOGGER.info("NOT renaming TvShow '{}' Episode {} - renaming patterns are empty!", episode.getTvShow().getTitle(), episode.getEpisode());
      return;
    }

    MediaFile originalVideoMediaFile = new MediaFile(episode.getMainVideoFile());
    // test for valid season/episode number
    if (episode.getSeason() < 0 || episode.getEpisode() < 0) {
      LOGGER.warn("failed to rename episode {} (TV show {}) - invalid season/episode number", episode.getTitle(), episode.getTvShow().getTitle());
      MessageManager.instance.pushMessage(
          new Message(MessageLevel.ERROR, episode.getTvShow().getTitle(), "tvshow.renamer.failedrename", new String[] { episode.getTitle() }));
      return;
    }

    LOGGER.debug("Renaming TvShow '{}', S{} E{}", episode.getTvShow().getTitle(), episode.getSeason(), episode.getEpisode());

    if (episode.isDisc()) {
      renameEpisodeAsDisc(episode);
      return;
    }

    // make sure we have actual stacking markers
    episode.reEvaluateStacking();

    // all the good & needed mediafiles
    List<MediaFile> needed = new ArrayList<>();
    List<MediaFile> cleanup = new ArrayList<>(episode.getMediaFiles());
    cleanup.removeAll(Collections.singleton((MediaFile) null)); // remove all NULL ones!

    String seasonFoldername = getSeasonFoldername(episode.getTvShow(), episode);
    Path seasonFolder = episode.getTvShow().getPathNIO();
    if (StringUtils.isNotBlank(seasonFoldername)) {
      seasonFolder = episode.getTvShow().getPathNIO().resolve(seasonFoldername);
      if (!Files.exists(seasonFolder)) {
        try {
          Files.createDirectory(seasonFolder);
        }
        catch (IOException ignored) {
        }
      }
    }

    // BASENAME
    String oldVideoBasename = episode.getVideoBasenameWithoutStacking();

    // ######################################################################
    // ## rename VIDEO (move 1:1)
    // ######################################################################
    for (MediaFile vid : episode.getMediaFiles(MediaFileType.VIDEO)) {
      LOGGER.trace("Rename 1:1 {} {}", vid.getType(), vid.getFileAsPath());
      MediaFile newMF = generateEpisodeFilenames(episode.getTvShow(), vid, "").get(0); // there can be only one
      boolean ok = moveFile(vid.getFileAsPath(), newMF.getFileAsPath());
      if (ok) {
        vid.setFile(newMF.getFileAsPath()); // update
        // if we move the episode in its own folder, we might need to upgrade the path as well!
        episode.setPath(newMF.getPath());
      }
      else {
        // not OK? just abort!
        return;
      }
      needed.add(vid); // add vid, since we're updating existing MF object
    }

    // ######################################################################
    // ## rename POSTER, FANART, BANNER, CLEARART, THUMB, LOGO, CLEARLOGO, DISCART (copy 1:N)
    // ######################################################################
    // we can have multiple ones, just get the newest one and copy(overwrite) them to all needed
    List<MediaFile> mfs = new ArrayList<>();
    mfs.add(episode.getNewestMediaFilesOfType(MediaFileType.FANART));
    mfs.add(episode.getNewestMediaFilesOfType(MediaFileType.POSTER));
    mfs.add(episode.getNewestMediaFilesOfType(MediaFileType.BANNER));
    mfs.add(episode.getNewestMediaFilesOfType(MediaFileType.CLEARART));
    mfs.add(episode.getNewestMediaFilesOfType(MediaFileType.THUMB));
    mfs.add(episode.getNewestMediaFilesOfType(MediaFileType.LOGO));
    mfs.add(episode.getNewestMediaFilesOfType(MediaFileType.CLEARLOGO));
    mfs.add(episode.getNewestMediaFilesOfType(MediaFileType.DISC));
    mfs.add(episode.getNewestMediaFilesOfType(MediaFileType.CHARACTERART));
    mfs.add(episode.getNewestMediaFilesOfType(MediaFileType.KEYART));
    mfs.removeAll(Collections.singleton((MediaFile) null)); // remove all NULL ones!
    for (MediaFile mf : mfs) {
      LOGGER.trace("Rename 1:N {} {}", mf.getType(), mf.getFileAsPath());
      List<MediaFile> newMFs = generateEpisodeFilenames(episode.getTvShow(), mf, oldVideoBasename); // 1:N
      for (MediaFile newMF : newMFs) {
        boolean ok = copyFile(mf.getFileAsPath(), newMF.getFileAsPath());
        if (ok) {
          needed.add(newMF);

          // update the cached image by just COPYing it around
          if (ImageCache.isImageCached(mf.getFileAsPath())) {
            Path oldCache = ImageCache.getAbsolutePath(mf);
            Path newCache = ImageCache.getAbsolutePath(newMF);
            LOGGER.trace("updating imageCache {} -> {}", oldCache, newCache);
            // just use plain copy here, since we do not need all the safety checks done in our method
            try {
              Files.copy(oldCache, newCache);
            }
            catch (IOException e) {
              LOGGER.warn("Error moving cached file", e.getMessage());
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
    for (MediaFile mf : episode.getMediaFiles(MediaFileType.NFO)) {
      if (mf.getFiledate() >= nfo.getFiledate()) {// && TvShowEpisodeConnectors.isValidNFO(mf.getFileAsPath())) { //FIXME
        nfo = new MediaFile(mf);
      }
    }

    if (nfo != MediaFile.EMPTY_MEDIAFILE) { // one valid found? copy our NFO to all variants
      List<MediaFile> newNFOs = generateEpisodeFilenames(episode.getTvShow(), nfo, oldVideoBasename); // 1:N
      if (!newNFOs.isEmpty()) {
        // ok, at least one has been set up
        for (MediaFile newNFO : newNFOs) {
          boolean ok = copyFile(nfo.getFileAsPath(), newNFO.getFileAsPath());
          if (ok) {
            needed.add(newNFO);
          }
        }
      }
      else {
        // list was empty, so even remove this NFO
        cleanup.add(nfo);
      }
    }
    else {
      LOGGER.trace("No valid NFO found for this episode");
    }

    // ######################################################################
    // ## rename subtitles (copy 1:1)
    // ######################################################################
    for (MediaFile subtitle : episode.getMediaFiles(MediaFileType.SUBTITLE)) {
      LOGGER.trace("Rename 1:1 {} {}", subtitle.getType(), subtitle.getFileAsPath());
      MediaFile sub = generateEpisodeFilenames(episode.getTvShow(), subtitle, oldVideoBasename).get(0); // there can be only one
      boolean ok = moveFile(subtitle.getFileAsPath(), sub.getFileAsPath());
      if (ok) {
        if (sub.getFilename().endsWith(".sub")) {
          // when having a .sub, also rename .idx (don't care if error)
          try {
            Path oldidx = subtitle.getFileAsPath().resolveSibling(subtitle.getFilename().replaceFirst("sub$", "idx"));
            Path newidx = sub.getFileAsPath().resolveSibling(sub.getFilename().replaceFirst("sub$", "idx"));
            Utils.moveFileSafe(oldidx, newidx);
          }
          catch (Exception e) {
            // no idx found or error - ignore
          }
        }
        subtitle.setFile(sub.getFileAsPath()); // update
      }
      needed.add(subtitle); // add vid, since we're updating existing MF object
    }

    // ######################################################################
    // ## rename all other types (copy 1:1)
    // ######################################################################
    mfs = new ArrayList<>(episode.getMediaFilesExceptType(MediaFileType.VIDEO, MediaFileType.NFO, MediaFileType.POSTER, MediaFileType.FANART,
        MediaFileType.BANNER, MediaFileType.CLEARART, MediaFileType.THUMB, MediaFileType.LOGO, MediaFileType.CLEARLOGO, MediaFileType.DISC,
        MediaFileType.CHARACTERART, MediaFileType.KEYART, MediaFileType.SUBTITLE));
    mfs.removeAll(Collections.singleton((MediaFile) null)); // remove all NULL ones!
    for (MediaFile other : mfs) {
      LOGGER.trace("Rename 1:1 {} - {}", other.getType(), other.getFileAsPath());

      List<MediaFile> newMFs = generateEpisodeFilenames(episode.getTvShow(), other, oldVideoBasename); // 1:N
      newMFs.removeAll(Collections.singleton((MediaFile) null)); // remove all NULL ones!

      for (MediaFile newMF : newMFs) {
        boolean ok = copyFile(other.getFileAsPath(), newMF.getFileAsPath());
        if (ok) {
          needed.add(newMF);
        }
        else {
          // FIXME: what to do? not copied/exception... keep it for now...
          needed.add(other);
        }
      }
    }

    // ######################################################################
    // ## invalidate image cache
    // ######################################################################
    for (MediaFile gfx : episode.getMediaFiles()) {
      ImageCache.invalidateCachedImage(gfx);
    }

    // remove duplicate MediaFiles
    Set<MediaFile> newMFs = new LinkedHashSet<>(needed);
    needed.clear();
    needed.addAll(newMFs);

    // ######################################################################
    // ## CLEANUP - delete all files marked for cleanup, which are not "needed"
    // ######################################################################
    LOGGER.debug("Cleanup...");
    for (int i = cleanup.size() - 1; i >= 0; i--) {
      // cleanup files which are not needed
      if (!needed.contains(cleanup.get(i))) {
        MediaFile cl = cleanup.get(i);
        if (Files.exists(cl.getFileAsPath())) { // unneeded, but for not displaying wrong deletes in logger...
          LOGGER.debug("Deleting {}", cl.getFileAsPath());
          Utils.deleteFileWithBackup(cl.getFileAsPath(), episode.getTvShow().getDataSource());
        }

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(cl.getFileAsPath().getParent())) {
          if (!directoryStream.iterator().hasNext()) {
            // no iterator = empty
            LOGGER.debug("Deleting empty Directory {}", cl.getFileAsPath().getParent());
            Files.delete(cl.getFileAsPath().getParent()); // do not use recursive her
          }
        }
        catch (IOException e) {
          LOGGER.error("cleanup of {} - {}", cl.getFileAsPath(), e.getMessage());
        }
      }
    }

    // update paths/mfs for the episode
    List<TvShowEpisode> eps = new ArrayList<>();
    eps.add(episode);

    // if the files are multi EP files, change all other episodes too
    eps.addAll(TvShowList.getTvEpisodesByFile(episode.getTvShow(), originalVideoMediaFile.getFile()));
    for (TvShowEpisode e : eps) {
      e.removeAllMediaFiles();
      e.addToMediaFiles(needed);
      e.setPath(episode.getPath());
      e.gatherMediaFileInformation(false);
      e.saveToDb();

      // ######################################################################
      // ## build up image cache
      // ######################################################################
      if (Settings.getInstance().isImageCache()) {
        for (MediaFile gfx : e.getMediaFiles()) {
          ImageCache.cacheImageSilently(gfx, false);
        }
      }
    }
  }

  /**
   * renames the episode as disc
   * 
   * @param episode
   *          the episode to be renamed
   */
  private static void renameEpisodeAsDisc(TvShowEpisode episode) {
    // get the first MF of this episode
    MediaFile mf = episode.getMainVideoFile();
    List<TvShowEpisode> eps = TvShowList.getTvEpisodesByFile(episode.getTvShow(), mf.getFile());

    // and do some checks
    if (!episode.isDisc() || !mf.isDiscFile()) {
      return;
    }

    Path disc;
    Path epFolder;

    if (DISC_FOLDERS.contains(mf.getFileAsPath().getFileName().toString().toLowerCase(Locale.ROOT))) {
      // hit on new structure
      // \Season 1\S01E02E03\VIDEO_TS
      // ........ \epFolder \disc-mf
      disc = mf.getFileAsPath();
      epFolder = disc.getParent();
    }
    else if (DISC_FOLDERS.contains(mf.getFileAsPath().getParent().getFileName().toString().toLowerCase(Locale.ROOT))) {
      // hit on old structure
      // \Season 1\S01E02E03\VIDEO_TS\VIDEO_TS.VOB
      // ........ \epFolder \disc... \mf
      disc = mf.getFileAsPath().getParent();
      epFolder = disc.getParent();
    }
    else {
      // invalid/no structure - what to do with em? renaming EP folder might work/be enough...
      // \Season 1\S01E02E03\VIDEO_TS.VOB
      // ........ \epFolder \mf
      LOGGER.warn("Episode is labeled as 'on BD/DVD', but structure seems not to match. Try our best to get this right... o_O");
      disc = mf.getFileAsPath().getParent();
      epFolder = disc;
      // return;
    }

    // create SeasonDir
    String seasonFoldername = getSeasonFoldername(episode.getTvShow(), episode);
    Path seasonFolder = episode.getTvShow().getPathNIO();
    if (StringUtils.isNotBlank(seasonFoldername)) {
      seasonFolder = episode.getTvShow().getPathNIO().resolve(seasonFoldername);
      if (!Files.exists(seasonFolder)) {
        try {
          Files.createDirectory(seasonFolder);
        }
        catch (IOException ignored) {
        }
      }
    }

    // rename epFolder accordingly
    String newFoldername = FilenameUtils.getBaseName(generateFoldername(episode.getTvShow(), mf)); // w/o extension
    if (StringUtils.isBlank(newFoldername)) {
      LOGGER.warn("empty disc folder name - exiting");
      return;
    }

    Path newEpFolder = seasonFolder.resolve(newFoldername);
    Path newDisc = newEpFolder.resolve(disc.getFileName()); // old disc name

    try {
      if (!epFolder.toAbsolutePath().toString().equals(newEpFolder.toAbsolutePath().toString())) {
        boolean ok = false;
        try {
          // create parent if needed
          if (!Files.exists(newEpFolder.getParent())) {
            Files.createDirectory(newEpFolder.getParent());
          }
          ok = Utils.moveDirectorySafe(epFolder, newEpFolder);
        }
        catch (Exception e) {
          LOGGER.error(e.getMessage());
          MessageManager.instance
              .pushMessage(new Message(MessageLevel.ERROR, epFolder, "message.renamer.failedrename", new String[] { ":", e.getLocalizedMessage() }));
        }
        if (ok) {
          // iterate over all EPs & MFs and fix new path
          LOGGER.debug("updating *all* MFs for new path -> {}", newEpFolder);
          for (TvShowEpisode e : eps) {
            e.updateMediaFilePath(epFolder, newEpFolder);
            e.setPath(newEpFolder.toAbsolutePath().toString());
            e.saveToDb();
          }
        }
        // and cleanup
        cleanEmptyDir(epFolder);
      }
      else {
        // old and new folder are equal, do nothing
      }
    }
    catch (Exception e) {
      LOGGER.error("error moving video file " + disc + " to " + newFoldername, e);
      MessageManager.instance.pushMessage(
          new Message(MessageLevel.ERROR, mf.getFilename(), "message.renamer.failedrename", new String[] { ":", e.getLocalizedMessage() }));
    }
  }

  private static void cleanEmptyDir(Path dir) {
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
      if (!directoryStream.iterator().hasNext()) {
        // no iterator = empty
        LOGGER.debug("Deleting empty Directory - {}", dir);
        Files.delete(dir); // do not use recursive her
        return;
      }
    }
    catch (IOException ignored) {
    }

    // FIXME: recursive backward delete?! why?!
    // if (Files.isDirectory(dir)) {
    // cleanEmptyDir(dir.getParent());
    // }
  }

  /**
   * generates the foldername of a TvShow MediaFile according to settings <b>(without path)</b><br>
   * Mainly for DISC files
   * 
   * @param tvShow
   *          the tvShow
   * @param mf
   *          the MF for multiepisode
   * @return the file name for media file
   */
  public static String generateFoldername(TvShow tvShow, MediaFile mf) {
    List<TvShowEpisode> eps = TvShowList.getTvEpisodesByFile(tvShow, mf.getFile());
    if (ListUtils.isEmpty(eps)) {
      return "";
    }

    return createDestination(TvShowModuleManager.getInstance().getSettings().getRenamerFilename(), eps);
  }

  /**
   * generates a list of filenames of a TvShow MediaFile according to settings
   *
   * @param tvShow
   *          the tvShow
   * @param mf
   *          the MF for multiepisode
   * @param videoBasename
   *          the original video file name
   * @return the file name for the media file
   */
  public static List<MediaFile> generateEpisodeFilenames(TvShow tvShow, MediaFile mf, String videoBasename) {
    return generateEpisodeFilenames("", tvShow, mf, videoBasename);
  }

  /**
   * generates a list of filenames of a TvShow MediaFile according to settings
   *
   * @param template
   *          the renaming template
   * @param tvShow
   *          the tvShow
   * @param mf
   *          the MF for multiepisode
   * @param oldVideoBasename
   *          the original video file name
   * @return the file name for the media file
   */
  public static List<MediaFile> generateEpisodeFilenames(String template, TvShow tvShow, MediaFile mf, String oldVideoBasename) {
    // return list of all generated MFs
    ArrayList<MediaFile> newFiles = new ArrayList<>();

    List<TvShowEpisode> eps = TvShowList.getTvEpisodesByFile(tvShow, mf.getFile());
    if (ListUtils.isEmpty(eps)) {
      return newFiles;
    }

    // sort the episodes
    eps.sort((ep1, ep2) -> {
      if (ep1.getSeason() != ep2.getSeason()) {
        return Integer.compare(ep1.getSeason(), ep2.getSeason());
      }
      return Integer.compare(ep1.getEpisode(), ep2.getEpisode());
    });

    String newFilename = "";
    // FIXME: check, where/when the stacking marker gets added, and WHICH (from which stacked video)
    if (StringUtils.isBlank(template)) {
      newFilename = createDestination(TvShowModuleManager.getInstance().getSettings().getRenamerFilename(), eps);
    }
    else {
      newFilename = createDestination(template, eps);
    }
    if (mf.getStacking() > 0) {
      // remove the stacking here, so we have a correct basename
      newFilename = Utils.cleanFolderStackingMarkers(newFilename); // i know, but this needs no extension ;)
    }

    // when renaming with $originalFilename, we get already the extension added!
    if (newFilename.endsWith(mf.getExtension())) {
      newFilename = FilenameUtils.getBaseName(newFilename);
    }

    String seasonFoldername = getSeasonFoldername(tvShow, eps.get(0));
    Path seasonFolder = tvShow.getPathNIO();
    if (StringUtils.isNotBlank(seasonFoldername)) {
      seasonFolder = tvShow.getPathNIO().resolve(seasonFoldername);
    }

    // no new filename? just move the file
    if (StringUtils.isBlank(newFilename)) {
      MediaFile mediaFile = new MediaFile(mf);
      mediaFile.setFile(seasonFolder.resolve(mf.getFilename()));
      newFiles.add(mediaFile);
      return newFiles;
    }

    switch (mf.getType()) {
      ////////////////////////////////////////////////////////////////////////
      // VIDEO
      ////////////////////////////////////////////////////////////////////////
      case VIDEO:
        MediaFile video = new MediaFile(mf);
        newFilename += getStackingString(mf); // ToDo
        newFilename += "." + mf.getExtension();
        video.setFile(seasonFolder.resolve(newFilename));
        newFiles.add(video);
        break;

      ////////////////////////////////////////////////////////////////////////
      // NFO
      ////////////////////////////////////////////////////////////////////////
      case NFO:
        MediaFile nfo = new MediaFile(mf);
        newFilename += "." + mf.getExtension();
        nfo.setFile(seasonFolder.resolve(newFilename));
        newFiles.add(nfo);
        break;

      ////////////////////////////////////////////////////////////////////////
      // THUMB
      ////////////////////////////////////////////////////////////////////////
      case THUMB:
        for (TvShowEpisodeThumbNaming thumbNaming : TvShowModuleManager.getInstance().getSettings().getEpisodeThumbFilenames()) {
          String thumbFilename = thumbNaming.getFilename(newFilename, getMediaFileExtension(mf));
          MediaFile thumb = new MediaFile(mf);
          thumb.setFile(seasonFolder.resolve(thumbFilename));
          newFiles.add(thumb);
        }
        break;

      ////////////////////////////////////////////////////////////////////////
      // SUBTITLE
      ////////////////////////////////////////////////////////////////////////
      case SUBTITLE:
        List<MediaFileSubtitle> subtitles = mf.getSubtitles();
        newFilename += getStackingString(mf);
        String subtitleFilename = newFilename;
        if (subtitles != null && !subtitles.isEmpty()) {
          MediaFileSubtitle sub = mf.getSubtitles().get(0);
          if (sub != null) {
            if (!sub.getLanguage().isEmpty()) {
              String lang = LanguageStyle.getLanguageCodeForStyle(sub.getLanguage(),
                  TvShowModuleManager.getInstance().getSettings().getSubtitleLanguageStyle());
              if (StringUtils.isBlank(lang)) {
                lang = sub.getLanguage();
              }
              subtitleFilename += "." + lang;
            }

            if (sub.isForced()) {
              subtitleFilename += ".forced";
            }
            if (sub.isSdh()) {
              subtitleFilename += ".sdh"; // double possible?!
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
          subtitle.setFile(seasonFolder.resolve(subtitleFilename + "." + mf.getExtension()));
          newFiles.add(subtitle);
        }
        break;

      ////////////////////////////////////////////////////////////////////////
      // FANART
      ////////////////////////////////////////////////////////////////////////
      case FANART:
        MediaFile fanart = new MediaFile(mf);
        fanart.setFile(seasonFolder.resolve(newFilename + "-fanart." + getMediaFileExtension(mf)));
        newFiles.add(fanart);
        break;

      ////////////////////////////////////////////////////////////////////////
      // TRAILER
      ////////////////////////////////////////////////////////////////////////
      case TRAILER:
        MediaFile trailer = new MediaFile(mf);
        trailer.setFile(seasonFolder.resolve(newFilename + "-trailer." + mf.getExtension()));
        newFiles.add(trailer);
        break;

      ////////////////////////////////////////////////////////////////////////
      // MEDIAINFO
      ////////////////////////////////////////////////////////////////////////
      case MEDIAINFO:
        MediaFile mediainfo = new MediaFile(mf);
        newFilename += getStackingString(mf); // ToDo
        mediainfo.setFile(seasonFolder.resolve(newFilename + "-mediainfo." + mf.getExtension()));
        newFiles.add(mediainfo);
        break;

      ////////////////////////////////////////////////////////////////////////
      // VSMETA
      ////////////////////////////////////////////////////////////////////////
      case VSMETA:
        MediaFile vsmeta = new MediaFile(mf);
        // HACK: get video extension from "old" name, eg video.avi.vsmeta
        String videoExt = FilenameUtils.getExtension(FilenameUtils.getBaseName(mf.getFilename()));
        newFilename += "." + videoExt + ".vsmeta";
        vsmeta.setFile(seasonFolder.resolve(newFilename));
        newFiles.add(vsmeta);
        break;

      ////////////////////////////////////////////////////////////////////////
      // VIDEO_EXTRA
      ////////////////////////////////////////////////////////////////////////
      case EXTRA:
      case VIDEO_EXTRA:
        // this extra is for an episode -> move it at least to the season folder and try to replace the episode tokens
        MediaFile extra = new MediaFile(mf);
        if (MediaFileHelper.isExtraInDedicatedFolder(mf, tvShow)) {
          // do nothing
          newFiles.add(extra);
        }
        else {
          // try to detect the title of the extra file
          String extraTitle = mf.getBasename().replace(oldVideoBasename, "");
          extra.setFile(seasonFolder.resolve(newFilename + extraTitle + "." + mf.getExtension()));
          newFiles.add(extra);
        }
        break;

      ////////////////////////////////////////////////////////////////////////
      // SAMPLE
      ////////////////////////////////////////////////////////////////////////
      case SAMPLE:
        MediaFile sample = new MediaFile(mf);
        sample.setFile(seasonFolder.resolve(newFilename + "-sample." + mf.getExtension()));
        newFiles.add(sample);
        break;

      ////////////////////////////////////////////////////////////////////////
      // AUDIO / TEXT / UNKNOWN / VIDEO_EXTRA
      // take the unknown part of the file name and attach it to the new file name
      ////////////////////////////////////////////////////////////////////////
      case AUDIO:
      case TEXT:
      case UNKNOWN:
        // this is something extra for an episode -> try to replace the episode tokens and preserve the extra in the filename
        // try to detect the title of the extra file
        MediaFile other = new MediaFile(mf);
        boolean spaceSubstitution = TvShowModuleManager.getInstance().getSettings().isRenamerFilenameSpaceSubstitution();
        String spaceReplacement = TvShowModuleManager.getInstance().getSettings().getRenamerFilenameSpaceReplacement();
        String destination = cleanupDestination(newFilename + StringUtils.difference(oldVideoBasename, FilenameUtils.getBaseName(mf.getFilename())),
            spaceSubstitution, spaceReplacement);
        other.setFile(seasonFolder.resolve(destination + "." + mf.getExtension()));
        newFiles.add(other);
        break;

      // missing enums
      case BANNER:
      case CLEARART:
      case CLEARLOGO:
      case DISC:
      case EXTRATHUMB:
      case GRAPHIC:
      case LOGO:
      case POSTER:
      case CHARACTERART:
      case KEYART:
      case SEASON_POSTER:
      case SEASON_FANART:
      case SEASON_BANNER:
      case SEASON_THUMB:
      default:
        break;
    }

    return newFiles;
  }

  /**
   * generate the season folder name according to the settings
   *
   * @param show
   *          the TV show to generate the season folder for
   * @param episode
   *          the episode to generate the season folder name for
   * @return the folder name of that season
   */
  public static String getSeasonFoldername(TvShow show, TvShowEpisode episode) {
    return getSeasonFoldername(TvShowModuleManager.getInstance().getSettings().getRenamerSeasonFoldername(), show, episode);
  }

  /**
   * generate the season folder name with the given template
   *
   * @param template
   *          the given template
   * @param show
   *          the TV show to generate the season folder for
   * @param episode
   *          the episode to generate the season folder name for
   * @return the folder name of that season
   */
  public static String getSeasonFoldername(String template, TvShow show, TvShowEpisode episode) {
    String seasonFolderName = template;
    TvShowSeason tvShowSeason = show.getSeason(episode.getSeason());

    // should not happen, but check it
    if (tvShowSeason == null) {
      // return an empty string
      return "";
    }

    // season 0 = Specials
    if (tvShowSeason.getSeason() == 0 && TvShowModuleManager.getInstance().getSettings().isSpecialSeason()
        && !StringUtils.isBlank(TvShowModuleManager.getInstance().getSettings().getRenamerSeasonFoldername())) {
      seasonFolderName = "Specials";
    }
    else {
      // replace all other tokens
      seasonFolderName = createDestination(seasonFolderName, tvShowSeason, episode);
    }

    // only allow empty season dir if the season is in the filename (aka recommended)
    if (StringUtils.isBlank(seasonFolderName)
        && !TvShowRenamer.isRecommended(template, TvShowModuleManager.getInstance().getSettings().getRenamerFilename())) {
      seasonFolderName = "Season " + tvShowSeason.getSeason();
    }

    return seasonFolderName;
  }

  /**
   * generate the TV show folder name according to the settings
   * 
   * @param tvShow
   *          the TV show to generate the folder name for
   * @return the folder name
   */
  public static String getTvShowFoldername(TvShow tvShow) {
    return getTvShowFoldername(TvShowModuleManager.getInstance().getSettings().getRenamerTvShowFoldername(), tvShow);
  }

  /**
   * generate the TV show folder name according to the given template
   *
   * @param template
   *          the template to generate the folder name for
   * @param tvShow
   *          the TV show to generate the folder name for
   * @return the folder name
   */
  public static String getTvShowFoldername(String template, TvShow tvShow) {
    String newPathname;

    try {
      if (StringUtils.isNotBlank(TvShowModuleManager.getInstance().getSettings().getRenamerTvShowFoldername())) {
        newPathname = Paths.get(tvShow.getDataSource(), createDestination(template, tvShow)).toString();
      }
      else {
        newPathname = tvShow.getPathNIO().toString();
      }
    }
    catch (Exception e) {
      // could not create a new pathname - stick to old
      newPathname = tvShow.getPathNIO().toString();
    }

    return newPathname;
  }

  /**
   * gets the token value ($x) from specified object
   * 
   * @param show
   *          our show
   * @param episode
   *          our episode
   * @param token
   *          the $x token
   * @return value or empty string
   */
  public static String getTokenValue(TvShow show, TvShowEpisode episode, String token) {
    try {
      Engine engine = createEngine();

      engine.setOutputAppender(new TmmOutputAppender() {
        @Override
        protected String replaceInvalidCharacters(String text) {
          return TvShowRenamer.replaceInvalidCharacters(text);
        }
      });

      Map<String, Object> root = new HashMap<>();
      if (episode != null) {
        root.put("episode", episode);
        root.put("season", episode.getTvShowSeason());
      }
      root.put("tvShow", show);
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
    engine.registerNamedRenderer(new TvShowNamedFirstCharacterRenderer());
    engine.registerNamedRenderer(new ChainedNamedRenderer(engine.getAllNamedRenderers()));

    engine.registerAnnotationProcessor(new RegexpProcessor());

    engine.setModelAdaptor(new TmmModelAdaptor());

    return engine;
  }

  /**
   * Creates the new TV show folder name according to template string
   *
   * @param template
   *          the template string
   * @param show
   *          the TV show to generate the folder name for
   * @return the TV show folder name
   */
  public static String createDestination(String template, TvShow show) {
    if (StringUtils.isBlank(template)) {
      return "";
    }

    boolean spaceSubstitution = TvShowModuleManager.getInstance().getSettings().isRenamerShowPathnameSpaceSubstitution();
    String spaceReplacement = TvShowModuleManager.getInstance().getSettings().getRenamerShowPathnameSpaceReplacement();

    return cleanupDestination(getTokenValue(show, null, template), spaceSubstitution, spaceReplacement);
  }

  /**
   * Creates the new season folder name according to template string
   *
   * @param template
   *          the template string
   * @param season
   *          the season to generate the folder name for
   * @return the season folder name
   */
  public static String createDestination(String template, TvShowSeason season, TvShowEpisode episode) {
    if (StringUtils.isBlank(template)) {
      return "";
    }

    String newDestination = getTokenValue(season.getTvShow(), episode, template);
    boolean spaceSubstitution = TvShowModuleManager.getInstance().getSettings().isRenamerSeasonPathnameSpaceSubstitution();
    String spaceReplacement = TvShowModuleManager.getInstance().getSettings().getRenamerSeasonPathnameSpaceReplacement();

    newDestination = cleanupDestination(newDestination, spaceSubstitution, spaceReplacement);
    return newDestination;
  }

  /**
   * Creates the new file/folder name according to template string
   * 
   * @param template
   *          the template
   * @param episodes
   *          the TV show episodes; nullable for TV show root foldername
   * @return the string
   */
  public static String createDestination(String template, List<TvShowEpisode> episodes) {
    if (StringUtils.isBlank(template)) {
      return "";
    }

    String newDestination = template;

    if (episodes.size() == 1) {
      // single episode
      TvShowEpisode firstEp = episodes.get(0);

      newDestination = getTokenValue(firstEp.getTvShow(), firstEp, template);
    }
    else {
      // multi episodes
      TvShowEpisode firstEp = episodes.get(0);
      String loopNumbers = "";

      // *******************
      // LOOP 1 - season/episode
      // *******************
      String seasonToken = getTokenFromTemplate(newDestination, seasonNumbers);
      if (StringUtils.isNotBlank(seasonToken)) {
        String seasonPart = "";
        Matcher matcher = seDelimiter.matcher(newDestination);
        if (matcher.find()) {
          seasonPart = matcher.group(0);
        }
        else {
          // no season info found? search for the token itself
          Pattern pattern = Pattern.compile("\\$\\{" + Pattern.quote(seasonToken) + ".*?\\}");
          matcher = pattern.matcher(newDestination);
          if (matcher.find()) {
            seasonPart = matcher.group(0);
          }
        }
        loopNumbers += seasonPart;
      }

      String episodeToken = getTokenFromTemplate(newDestination, episodeNumbers);
      if (StringUtils.isNotBlank(episodeToken)) {
        String episodePart = "";
        Matcher matcher = epDelimiter.matcher(newDestination);
        if (matcher.find()) {
          episodePart += matcher.group(0);
        }
        else {
          // no episode info found? search for the token itself
          Pattern pattern = Pattern.compile("\\$\\{" + Pattern.quote(episodeToken) + ".*?\\}");
          matcher = pattern.matcher(newDestination);
          if (matcher.find()) {
            episodePart = matcher.group(0);
          }
        }

        loopNumbers += episodePart;
      }
      loopNumbers = loopNumbers.strip();

      // foreach episode, replace and append pattern:
      StringBuilder episodeParts = new StringBuilder();
      for (TvShowEpisode episode : episodes) {
        String episodePart = getTokenValue(episode.getTvShow(), episode, loopNumbers);
        episodeParts.append(" ").append(episodePart);
      }

      // replace original pattern, with our combined
      if (StringUtils.isNotBlank(loopNumbers)) {
        newDestination = newDestination.replace(loopNumbers, episodeParts.toString().strip());
      }

      // *******************
      // LOOP 2 - title
      // *******************
      String loopTitles = "";
      String titleToken = getTokenFromTemplate(template, episodeTitles);
      if (StringUtils.isNotBlank(titleToken)) {
        Pattern pattern = Pattern.compile("\\$\\{" + Pattern.quote(titleToken) + ".*?\\}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(template);
        if (matcher.find()) {
          loopTitles += matcher.group(0);
        }
      }

      loopTitles = loopTitles.strip();

      // foreach episode, replace and append pattern:
      if (StringUtils.isNotBlank(loopTitles)) {
        episodeParts = new StringBuilder();
        String previous = "";
        for (TvShowEpisode episode : episodes) {
          String episodePart = getTokenValue(episode.getTvShow(), episode, loopTitles);

          // do not add the same title twice!
          if (!episodePart.equals(previous)) {
            // separate multiple titles via -
            if (StringUtils.isNotBlank(episodeParts.toString())) {
              episodeParts.append(" -");
            }
            episodeParts.append(" ").append(episodePart);
          }
          previous = episodePart;
        }

        newDestination = newDestination.replace(loopTitles, episodeParts.toString().strip());
      }

      // *******************
      // LOOP 3 - aired
      // *******************
      String loopAired = "";
      String airedToken = getTokenFromTemplate(template, episodeAired);
      if (StringUtils.isNotBlank(airedToken)) {
        Pattern pattern = Pattern.compile("\\$\\{" + Pattern.quote(airedToken) + ".*?\\}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(template);
        if (matcher.find()) {
          loopAired += matcher.group(0);
        }
      }

      loopAired = loopAired.strip();

      // foreach episode, replace and append pattern:
      if (StringUtils.isNotBlank(loopAired)) {
        episodeParts = new StringBuilder();
        for (TvShowEpisode episode : episodes) {
          String episodePart = getTokenValue(episode.getTvShow(), episode, loopAired);

          // separate multiple titles via -
          if (StringUtils.isNotBlank(episodeParts.toString())) {
            episodeParts.append(" -");
          }
          episodeParts.append(" ").append(episodePart);
        }

        newDestination = newDestination.replace(loopAired, episodeParts.toString().strip());
      }

      newDestination = getTokenValue(firstEp.getTvShow(), firstEp, newDestination);
    } // end multi episodes

    boolean spaceSubstitution = TvShowModuleManager.getInstance().getSettings().isRenamerFilenameSpaceSubstitution();
    String spaceReplacement = TvShowModuleManager.getInstance().getSettings().getRenamerFilenameSpaceReplacement();

    newDestination = cleanupDestination(newDestination, spaceSubstitution, spaceReplacement);

    return newDestination;
  }

  /**
   * cleanup the destination (remove empty brackets, space substitution, ..)
   * 
   * @param destination
   *          the string to be cleaned up
   * @param spaceSubstitution
   *          replace spaces (=true)? or not (=false)
   * @param spaceReplacement
   *          the replacement string for spaces
   * @return the cleaned up string
   */
  private static String cleanupDestination(String destination, Boolean spaceSubstitution, String spaceReplacement) {
    // replace empty brackets
    destination = destination.replaceAll("\\([ ]?\\)", "");
    destination = destination.replaceAll("\\[[ ]?\\]", "");
    destination = destination.replaceAll("\\{[ ]?\\}", "");

    // if there are multiple file separators in a row - strip them out
    if (SystemUtils.IS_OS_WINDOWS) {
      // we need to mask it in windows
      destination = destination.replaceAll("\\\\{2,}", "\\\\");
      destination = destination.replaceAll("^\\\\", "");
      // trim whitespace around directory sep
      destination = destination.replaceAll("\\s+\\\\", "\\\\");
      destination = destination.replaceAll("\\\\\\s+", "\\\\");
      // remove separators in front of path separators
      destination = destination.replaceAll("[ \\.\\-_]+\\\\", "\\\\");
    }
    else {
      destination = destination.replaceAll(File.separator + "{2,}", File.separator);
      destination = destination.replaceAll("^" + File.separator, "");
      // trim whitespace around directory sep
      destination = destination.replaceAll("\\s+/", "/");
      destination = destination.replaceAll("/\\s+", "/");
      // remove separators in front of path separators
      destination = destination.replaceAll("[ \\.\\-_]+/", "/");
    }

    // replace spaces with underscores if needed (filename only)
    if (spaceSubstitution) {
      destination = destination.replace(" ", spaceReplacement);

      // also replace now multiple replacements with one to avoid strange looking results
      // example:
      // Abraham Lincoln - Vampire Hunter -> Abraham-Lincoln---Vampire-Hunter
      destination = destination.replaceAll(Pattern.quote(spaceReplacement) + "+", spaceReplacement);
    }

    // ASCII replacement
    if (TvShowModuleManager.getInstance().getSettings().isAsciiReplacement()) {
      destination = StrgUtils.convertToAscii(destination, false);
    }

    // replace all leading/trailing separators
    destination = destination.replaceAll("^[ \\.\\-_]+", "");
    destination = destination.replaceAll("[ \\.\\-_]+$", "");

    // the colon is handled by JMTE but it looks like some users are stupid enough to add this to the pattern itself
    destination = destination.replace(": ", " - "); // nicer
    destination = destination.replace(":", "-"); // nicer

    // replace new lines
    destination = destination.replaceAll("\r?\n", " ");

    // trim out unnecessary whitespaces
    destination = destination.replaceAll(" +", " ");

    return destination.strip();
  }

  /**
   * checks, if the pattern has a recommended structure (S/E numbers, title filled)<br>
   * when false, it might lead to some unpredictable renamings...
   * 
   * @param seasonPattern
   *          the season pattern
   * @param filePattern
   *          the file pattern
   * @return true/false
   */
  public static boolean isRecommended(String seasonPattern, String filePattern) {
    // count em
    int epCnt = count(filePattern, episodeNumbers);
    int titleCnt = count(filePattern, episodeTitles);
    int seCnt = count(filePattern, seasonNumbers);
    int seFolderCnt = count(seasonPattern, seasonNumbers);// check season folder pattern

    // check rules
    if (epCnt != 1 || titleCnt > 1 || seCnt > 1 || seFolderCnt > 1 || (seCnt + seFolderCnt) == 0) {
      LOGGER.debug("Too many/less episode/season/title replacer patterns");
      return false;
    }

    int epPos = getPatternPos(filePattern, episodeNumbers);
    int sePos = getPatternPos(filePattern, seasonNumbers);
    int titlePos = getPatternPos(filePattern, episodeTitles);

    if (sePos > epPos) {
      LOGGER.debug("Season pattern should be before episode pattern!");
      return false;
    }

    // check if title not in-between season/episode pattern in file
    if (titleCnt == 1 && seCnt == 1) {
      if (titlePos < epPos && titlePos > sePos) {
        LOGGER.debug("Title should not be between season/episode pattern");
        return false;
      }
    }

    return true;
  }

  /**
   * Count the amount of renamer tokens per group
   * 
   * @param pattern
   *          the pattern to analyze
   * @param possibleValues
   *          an array of possible values
   * @return 0, or amount
   */
  private static int count(String pattern, String[] possibleValues) {
    int count = 0;
    for (String r : possibleValues) {
      if (containsToken(pattern, r)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Returns first position of any matched patterns
   * 
   * @param pattern
   *          the pattern to get the position for
   * @param possibleValues
   *          an array of all possible values
   * @return the position of the first occurrence
   */
  private static int getPatternPos(String pattern, String[] possibleValues) {
    int pos = -1;
    for (String r : possibleValues) {
      if (containsToken(pattern, r)) {
        pos = pattern.indexOf(r);
      }
    }
    return pos;
  }

  /**
   * returns the first found token from the matched pattern
   *
   * @param template
   *          the template to be searched for
   * @param possibleTokens
   *          the tokens to look for
   * @return the found token or an emtpy string
   */
  private static String getTokenFromTemplate(String template, String[] possibleTokens) {
    for (String token : possibleTokens) {
      if (containsToken(template, token)) {
        return token;
      }
    }
    return "";
  }

  private static boolean containsToken(String template, String token) {
    Pattern pattern = Pattern.compile("\\$\\{" + token + "[\\[\\};]");
    Matcher matcher = pattern.matcher(template);
    return matcher.find();
  }

  private static String getMediaFileExtension(MediaFile mf) {
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
   * Deletes "unwanted files" according to settings. Same as the action, but w/o GUI.
   * 
   * @param show
   *          the {@link TvShow} to clean up
   */
  private static void cleanupUnwantedFiles(TvShow show) {
    if (TvShowModuleManager.getInstance().getSettings().renamerCleanupUnwanted) {
      Utils.deleteUnwantedFilesAndFoldersFor(show);
    }
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
  private static boolean moveFile(Path oldFilename, Path newFilename) {
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
      LOGGER.error("error moving file", e);
      MessageManager.instance
          .pushMessage(new Message(MessageLevel.ERROR, oldFilename, "message.renamer.failedrename", new String[] { ":", e.getLocalizedMessage() }));
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
  private static boolean copyFile(Path oldFilename, Path newFilename) {
    if (!oldFilename.toAbsolutePath().toString().equals(newFilename.toAbsolutePath().toString())) {
      LOGGER.debug("copy file '{}' to '{}'", oldFilename, newFilename);
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
   * returns "delimiter + stackingString" for use in filename
   *
   * @param mf
   *          a mediaFile
   * @return eg ".CD1" dependent of settings
   */
  private static String getStackingString(MediaFile mf) {
    String delimiter = ".";
    if (TvShowModuleManager.getInstance().getSettings().isRenamerFilenameSpaceSubstitution()) {
      delimiter = TvShowModuleManager.getInstance().getSettings().getRenamerFilenameSpaceReplacement();
    }
    if (!mf.getStackingMarker().isEmpty()) {
      return delimiter + mf.getStackingMarker();
    }
    else if (mf.getStacking() != 0) {
      return delimiter + "CD" + mf.getStacking();
    }
    return "";
  }

  public static String replaceInvalidCharacters(String source) {
    String result = source;

    if ("-".equals(TvShowModuleManager.getInstance().getSettings().getRenamerColonReplacement())) {
      result = result.replace(": ", " - "); // nicer
      result = result.replace(":", "-"); // nicer
    }
    else {
      result = result.replace(":", TvShowModuleManager.getInstance().getSettings().getRenamerColonReplacement());
    }

    return result.replaceAll("([\":<>|?*])", "");
  }

  /**
   * checks supplied renamer pattern against our tokenmap, if everything could be found
   * 
   * @param pattern
   * @return error string, what token(s) are wrong
   */
  public static String isPatternValid(String pattern) {
    String err = "";
    Pattern p = Pattern.compile("\\$\\{(.*?)\\}");
    Matcher matcher = p.matcher(pattern);
    while (matcher.find()) {
      String fulltoken = matcher.group(1);
      String token = "";
      if (fulltoken.contains(",")) {
        // split additional like ${-,token,replace}
        String[] split = fulltoken.split(",");
        token = split[1];
      }
      else if (fulltoken.contains("[")) {
        // strip all after parenthesis
        token = fulltoken.substring(0, fulltoken.indexOf('['));
      }
      else if (fulltoken.contains(";")) {
        // strip all after semicolon like ${title;first}
        token = fulltoken.substring(0, fulltoken.indexOf(';'));
        String first = fulltoken.substring(fulltoken.indexOf(';') + 1);
        if (!first.equals("first")) {
          err += "  " + matcher.group(); // "first" is missing
        }
      }
      else {
        token = fulltoken;
      }
      String tok = TOKEN_MAP.get(token.strip());
      if (tok == null) {
        err += "  " + matcher.group(); // complete token with ${}
      }
    }
    return err;
  }

  public static class TvShowNamedFirstCharacterRenderer implements NamedRenderer {
    private static final Pattern FIRST_ALPHANUM_PATTERN = Pattern.compile("[\\p{L}\\d]");

    @Override
    public String render(Object o, String s, Locale locale, Map<String, Object> map) {
      if (o instanceof String && StringUtils.isNotBlank((String) o)) {
        String source = StrgUtils.convertToAscii((String) o, false);
        Matcher matcher = FIRST_ALPHANUM_PATTERN.matcher(source);
        if (matcher.find()) {
          String first = matcher.group();

          if (first.matches("\\p{L}")) {
            return first.toUpperCase(Locale.ROOT);
          }
          else {
            return TvShowModuleManager.getInstance().getSettings().getRenamerFirstCharacterNumberReplacement();
          }
        }
      }
      if (o instanceof Number) {
        return TvShowModuleManager.getInstance().getSettings().getRenamerFirstCharacterNumberReplacement();
      }
      if (o instanceof Date) {
        return TvShowModuleManager.getInstance().getSettings().getRenamerFirstCharacterNumberReplacement();
      }
      return "";
    }

    @Override
    public String getName() {
      return "first";
    }

    @Override
    public RenderFormatInfo getFormatInfo() {
      return null;
    }

    @Override
    public Class<?>[] getSupportedClasses() {
      return new Class[] { Date.class, String.class, Integer.class, Long.class };
    }
  }
}
