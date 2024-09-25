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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.IFileNaming;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.filenaming.MovieBannerNaming;
import org.tinymediamanager.core.movie.filenaming.MovieClearartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieClearlogoNaming;
import org.tinymediamanager.core.movie.filenaming.MovieDiscartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieExtraFanartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieFanartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieKeyartNaming;
import org.tinymediamanager.core.movie.filenaming.MoviePosterNaming;
import org.tinymediamanager.core.movie.filenaming.MovieThumbNaming;
import org.tinymediamanager.core.movie.tasks.MovieExtraImageFetcherTask;
import org.tinymediamanager.core.tasks.MediaEntityImageFetcherTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.thirdparty.VSMeta;

/**
 * The class MovieArtworkHelper. A helper class for managing movie artwork
 * 
 * @author Manuel Laggner
 */
public class MovieArtworkHelper {
  private static final Logger  LOGGER        = LoggerFactory.getLogger(MovieArtworkHelper.class);
  private static final Pattern INDEX_PATTERN = Pattern.compile(".*?(\\d+)$");

  private MovieArtworkHelper() {
    throw new IllegalAccessError();
  }

  /**
   * Manage downloading of the chosen artwork type
   * 
   * @param movie
   *          the movie for which artwork has to be downloaded
   * @param type
   *          the type of artwork to be downloaded
   */
  public static void downloadArtwork(Movie movie, MediaFileType type) {

    // extra handling for extrafanart & extrathumbs
    if (type == MediaFileType.EXTRAFANART || type == MediaFileType.EXTRATHUMB) {
      downloadExtraArtwork(movie, type);
      return;
    }

    String url = movie.getArtworkUrl(type);
    try {
      if (StringUtils.isBlank(url)) {
        return;
      }

      List<IFileNaming> fileNamings = getFileNamingsForMediaFileType(movie, type);
      if (fileNamings.isEmpty()) {
        return;
      }

      List<String> filenames = new ArrayList<>();
      for (IFileNaming fileNaming : fileNamings) {
        String filename = getArtworkFilename(movie, fileNaming, Utils.getArtworkExtensionFromUrl(url));
        if (StringUtils.isBlank(filename)) {
          continue;
        }

        filenames.add(filename);
      }

      if (!filenames.isEmpty()) {
        // get images in thread
        MediaEntityImageFetcherTask task = new MediaEntityImageFetcherTask(movie, url, MediaFileType.getMediaArtworkType(type), filenames);
        TmmTaskManager.getInstance().addImageDownloadTask(task);
      }
    }
    finally {
      // if that has been a local file, remove it from the artwork urls after we've already started the download(copy) task
      if (url.startsWith("file:")) {
        movie.removeArtworkUrl(type);
      }
    }
  }

  /**
   * downloads all missing artworks<br>
   * adheres the user artwork settings
   * 
   * @param movie
   *          for specified movie
   */
  public static void downloadMissingArtwork(Movie movie) {
    downloadMissingArtwork(movie, false);
  }

  /**
   * downloads all missing artworks
   * 
   * @param movie
   *          for specified movie
   * @param force
   *          if forced, we ignore the artwork settings and download all known
   */
  public static void downloadMissingArtwork(Movie movie, boolean force) {
    MediaFileType[] mfts = MediaFileType.getGraphicMediaFileTypes();

    // do for all known graphical MediaFileTypes
    for (MediaFileType mft : mfts) {

      List<MediaFile> mfs = movie.getMediaFiles(mft);
      if (mfs.isEmpty()) {
        boolean download = false;
        // not in our list? get'em!
        switch (mft) {
          case FANART:
            if (!MovieModuleManager.getInstance().getSettings().getFanartFilenames().isEmpty() || force) {
              download = true;
            }
            break;

          case POSTER:
            if (!MovieModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty() || force) {
              download = true;
            }
            break;

          case BANNER:
            if (!MovieModuleManager.getInstance().getSettings().getBannerFilenames().isEmpty() || force) {
              download = true;
            }
            break;

          case CLEARART:
            if (!MovieModuleManager.getInstance().getSettings().getClearartFilenames().isEmpty() || force) {
              download = true;
            }
            break;

          case DISC:
            if (!MovieModuleManager.getInstance().getSettings().getDiscartFilenames().isEmpty() || force) {
              download = true;
            }
            break;

          case CLEARLOGO:
          case LOGO:
            if (!MovieModuleManager.getInstance().getSettings().getClearlogoFilenames().isEmpty() || force) {
              download = true;
            }
            break;

          case THUMB:
            if (!MovieModuleManager.getInstance().getSettings().getThumbFilenames().isEmpty() || force) {
              download = true;
            }
            break;

          case KEYART:
            if (!MovieModuleManager.getInstance().getSettings().getKeyartFilenames().isEmpty() || force) {
              download = true;
            }
            break;

          case EXTRAFANART:
            if (MovieModuleManager.getInstance().getSettings().isImageExtraFanart() || force) {
              download = true;
            }
            break;

          case EXTRATHUMB:
            if (MovieModuleManager.getInstance().getSettings().isImageExtraThumbs() || force) {
              download = true;
            }
            break;

          default:
            break;
        }

        if (download) {
          downloadArtwork(movie, mft);
        }
      }
    }
  }

  /**
   * set & download missing artwork for the given movie
   *
   * @param movie
   *          the movie to set the artwork for
   * @param artwork
   *          a list of all artworks to be set
   * @param metadataConfig
   *          the config which artwork to download
   */
  public static void downloadMissingArtwork(Movie movie, List<MediaArtwork> artwork, List<MovieScraperMetadataConfig> metadataConfig) {

    // poster
    if (metadataConfig.contains(MovieScraperMetadataConfig.POSTER) && movie.getMediaFiles(MediaFileType.POSTER).isEmpty()) {
      setBestPoster(movie, artwork, false);
    }

    // fanart
    if (metadataConfig.contains(MovieScraperMetadataConfig.FANART) && movie.getMediaFiles(MediaFileType.FANART).isEmpty()) {
      setBestFanart(movie, artwork, false);
    }

    // clearlogo
    if (metadataConfig.contains(MovieScraperMetadataConfig.CLEARLOGO) && movie.getMediaFiles(MediaFileType.CLEARLOGO).isEmpty()) {
      setBestArtwork(movie, artwork, MediaArtworkType.CLEARLOGO, !MovieModuleManager.getInstance().getSettings().getClearlogoFilenames().isEmpty(),
          false);
    }

    // clearart
    if (metadataConfig.contains(MovieScraperMetadataConfig.CLEARART) && movie.getMediaFiles(MediaFileType.CLEARART).isEmpty()) {
      setBestArtwork(movie, artwork, MediaArtworkType.CLEARART, !MovieModuleManager.getInstance().getSettings().getClearartFilenames().isEmpty(),
          false);
    }

    // banner
    if (metadataConfig.contains(MovieScraperMetadataConfig.BANNER) && movie.getMediaFiles(MediaFileType.BANNER).isEmpty()) {
      setBestArtwork(movie, artwork, MediaArtworkType.BANNER, !MovieModuleManager.getInstance().getSettings().getBannerFilenames().isEmpty(), false);
    }

    // thumb
    if (metadataConfig.contains(MovieScraperMetadataConfig.THUMB) && movie.getMediaFiles(MediaFileType.THUMB).isEmpty()) {
      setBestArtwork(movie, artwork, MediaArtworkType.THUMB, !MovieModuleManager.getInstance().getSettings().getThumbFilenames().isEmpty(), false);
    }

    // discart
    if (metadataConfig.contains(MovieScraperMetadataConfig.DISCART) && movie.getMediaFiles(MediaFileType.DISC).isEmpty()) {
      setBestArtwork(movie, artwork, MediaArtworkType.DISC, !MovieModuleManager.getInstance().getSettings().getDiscartFilenames().isEmpty(), false);
    }

    // keyart
    if (metadataConfig.contains(MovieScraperMetadataConfig.KEYART) && movie.getMediaFiles(MediaFileType.KEYART).isEmpty()) {
      setBestArtwork(movie, artwork, MediaArtworkType.KEYART, !MovieModuleManager.getInstance().getSettings().getKeyartFilenames().isEmpty(), false);
    }

    // extrathumbs
    List<String> extrathumbs = new ArrayList<>();
    if (metadataConfig.contains(MovieScraperMetadataConfig.EXTRATHUMB) && movie.getMediaFiles(MediaFileType.EXTRATHUMB).isEmpty()
        && MovieModuleManager.getInstance().getSettings().isImageExtraThumbs()
        && MovieModuleManager.getInstance().getSettings().getImageExtraThumbsCount() > 0) {
      for (MediaArtwork art : artwork.stream().filter(mediaArtwork -> mediaArtwork.getType() == MediaArtworkType.BACKGROUND).toList()) {
        // only get artwork in desired resolution
        for (MediaArtwork.ImageSizeAndUrl imageSizeAndUrl : art.getImageSizes()) {
          if (imageSizeAndUrl.getSizeOrder() == MovieModuleManager.getInstance().getSettings().getImageFanartSize().getOrder()) {
            extrathumbs.add(imageSizeAndUrl.getUrl());
            break;
          }
        }

        if (extrathumbs.size() >= MovieModuleManager.getInstance().getSettings().getImageExtraThumbsCount()) {
          break;
        }
      }
      movie.setExtraThumbs(extrathumbs);
      if (!extrathumbs.isEmpty()) {
        if (!movie.isMultiMovieDir()) {
          downloadArtwork(movie, MediaFileType.EXTRATHUMB);
        }
      }
    }

    // extrafanarts
    List<String> extrafanarts = new ArrayList<>();
    if (metadataConfig.contains(MovieScraperMetadataConfig.EXTRAFANART) && MovieModuleManager.getInstance().getSettings().isImageExtraFanart()
        && MovieModuleManager.getInstance().getSettings().getImageExtraFanartCount() > 0) {
      for (MediaArtwork art : artwork.stream().filter(mediaArtwork -> mediaArtwork.getType() == MediaArtworkType.BACKGROUND).toList()) {
        // only get artwork in desired resolution
        for (MediaArtwork.ImageSizeAndUrl imageSizeAndUrl : art.getImageSizes()) {
          if (imageSizeAndUrl.getSizeOrder() == MovieModuleManager.getInstance().getSettings().getImageFanartSize().getOrder()) {
            extrafanarts.add(imageSizeAndUrl.getUrl());
            break;
          }
        }

        if (extrafanarts.size() >= MovieModuleManager.getInstance().getSettings().getImageExtraFanartCount()) {
          break;
        }
      }
      movie.setExtraFanarts(extrafanarts);
      if (!extrafanarts.isEmpty()) {
        if (!movie.isMultiMovieDir()) {
          downloadArtwork(movie, MediaFileType.EXTRAFANART);
        }
      }
    }

    // update DB
    movie.saveToDb();
    movie.writeNFO(); // rewrite NFO to get the urls into the NFO
  }

  /**
   * detect if there is missing artwork for the given movie
   *
   * @param movie
   *          the movie to check artwork for
   * @return true/false
   */
  public static boolean hasMissingArtwork(Movie movie, List<MovieScraperMetadataConfig> config) {
    if (config.contains(MovieScraperMetadataConfig.POSTER) && !MovieModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty()
        && movie.getMediaFiles(MediaFileType.POSTER).isEmpty()) {
      return true;
    }
    if (config.contains(MovieScraperMetadataConfig.FANART) && !MovieModuleManager.getInstance().getSettings().getFanartFilenames().isEmpty()
        && movie.getMediaFiles(MediaFileType.FANART).isEmpty()) {
      return true;
    }
    if (config.contains(MovieScraperMetadataConfig.BANNER) && !MovieModuleManager.getInstance().getSettings().getBannerFilenames().isEmpty()
        && movie.getMediaFiles(MediaFileType.BANNER).isEmpty()) {
      return true;
    }
    if (config.contains(MovieScraperMetadataConfig.DISCART) && !MovieModuleManager.getInstance().getSettings().getDiscartFilenames().isEmpty()
        && movie.getMediaFiles(MediaFileType.DISC).isEmpty()) {
      return true;
    }
    if (config.contains(MovieScraperMetadataConfig.CLEARLOGO) && !MovieModuleManager.getInstance().getSettings().getClearlogoFilenames().isEmpty()
        && movie.getMediaFiles(MediaFileType.CLEARLOGO).isEmpty()) {
      return true;
    }
    if (config.contains(MovieScraperMetadataConfig.CLEARART) && !MovieModuleManager.getInstance().getSettings().getClearartFilenames().isEmpty()
        && movie.getMediaFiles(MediaFileType.CLEARART).isEmpty()) {
      return true;
    }
    if (config.contains(MovieScraperMetadataConfig.THUMB) && !MovieModuleManager.getInstance().getSettings().getThumbFilenames().isEmpty()
        && movie.getMediaFiles(MediaFileType.THUMB).isEmpty()) {
      return true;
    }
    if (config.contains(MovieScraperMetadataConfig.KEYART) && !MovieModuleManager.getInstance().getSettings().getKeyartFilenames().isEmpty()
        && movie.getMediaFiles(MediaFileType.KEYART).isEmpty()) {
      return true;
    }
    if (config.contains(MovieScraperMetadataConfig.EXTRAFANART) && MovieModuleManager.getInstance().getSettings().isImageExtraFanart()
        && !MovieModuleManager.getInstance().getSettings().getExtraFanartFilenames().isEmpty()
        && movie.getMediaFiles(MediaFileType.EXTRAFANART).isEmpty()) {
      return true;
    }
    if (config.contains(MovieScraperMetadataConfig.EXTRATHUMB) && MovieModuleManager.getInstance().getSettings().isImageExtraThumbs()
        && movie.getMediaFiles(MediaFileType.EXTRATHUMB).isEmpty()) {
      return true;
    }

    return false;
  }

  /**
   * get the artwork filename for the given movie, naming scheme and extension
   * 
   * @param movie
   *          the movie
   * @param fileNaming
   *          the naming scheme
   * @param extension
   *          the extension
   * @return the full artwork filename
   */
  public static String getArtworkFilename(Movie movie, IFileNaming fileNaming, String extension) {
    List<MediaFile> mfs = movie.getMediaFiles(MediaFileType.VIDEO);
    if (mfs != null && !mfs.isEmpty()) {
      return fileNaming.getFilename(movie.getVideoBasenameWithoutStacking(), extension);
    }
    else {
      return fileNaming.getFilename("", extension); // no video files
    }
  }

  /**
   * Fanart format is not empty, so we want at least one ;)<br>
   * Idea is, to check whether the preferred format is set in settings<br>
   * and if not, take some default (since we want fanarts)
   * 
   * @param movie
   *          the movie to get the fanart names for
   * @return List of MovieFanartNaming (can be empty!)
   */
  public static List<MovieFanartNaming> getFanartNamesForMovie(Movie movie) {
    List<MovieFanartNaming> fanartnames = new ArrayList<>();
    if (MovieModuleManager.getInstance().getSettings().getFanartFilenames().isEmpty()) {
      return fanartnames;
    }
    if (movie.isMultiMovieDir()) {
      if (MovieModuleManager.getInstance().getSettings().getFanartFilenames().contains(MovieFanartNaming.FILENAME_FANART)) {
        fanartnames.add(MovieFanartNaming.FILENAME_FANART);
      }
      if (MovieModuleManager.getInstance().getSettings().getFanartFilenames().contains(MovieFanartNaming.FILENAME_FANART2)) {
        fanartnames.add(MovieFanartNaming.FILENAME_FANART2);
      }
      if (fanartnames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getFanartFilenames().isEmpty()) {
        fanartnames.add(MovieFanartNaming.FILENAME_FANART);
      }
    }
    else if (movie.isDisc()) {
      fanartnames.add(MovieFanartNaming.FANART);
    }
    else {
      fanartnames = MovieModuleManager.getInstance().getSettings().getFanartFilenames();
    }
    return fanartnames;
  }

  /**
   * Poster format is not empty, so we want at least one ;)<br>
   * Idea is, to check whether the preferred format is set in settings<br>
   * and if not, take some default (since we want posters)
   * 
   * @param movie
   *          the movie to get the poster names for
   * @return list of MoviePosterNaming (can be empty!)
   */
  public static List<MoviePosterNaming> getPosterNamesForMovie(Movie movie) {
    List<MoviePosterNaming> posternames = new ArrayList<>();
    if (MovieModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty()) {
      return posternames;
    }
    if (movie.isMultiMovieDir()) {
      if (MovieModuleManager.getInstance().getSettings().getPosterFilenames().contains(MoviePosterNaming.FILENAME_POSTER)) {
        posternames.add(MoviePosterNaming.FILENAME_POSTER);
      }
      if (MovieModuleManager.getInstance().getSettings().getPosterFilenames().contains(MoviePosterNaming.FILENAME)) {
        posternames.add(MoviePosterNaming.FILENAME);
      }
      if (posternames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty()) {
        posternames.add(MoviePosterNaming.FILENAME_POSTER);
      }
    }
    else if (movie.isDisc()) {
      if (MovieModuleManager.getInstance().getSettings().getPosterFilenames().contains(MoviePosterNaming.FOLDER)) {
        posternames.add(MoviePosterNaming.FOLDER);
      }
      if (MovieModuleManager.getInstance().getSettings().getPosterFilenames().contains(MoviePosterNaming.POSTER)
          || (posternames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty())) {
        posternames.add(MoviePosterNaming.POSTER);
      }
    }
    else {
      posternames.addAll(MovieModuleManager.getInstance().getSettings().getPosterFilenames());
    }
    return posternames;
  }

  /**
   * Banner format is not empty, so we want at least one ;)<br>
   * Idea is, to check whether the preferred format is set in settings<br>
   * and if not, take some default (since we want banners)
   *
   * @param movie
   *          the movie to get the banner names for
   * @return list of MovieBannerNaming (can be empty!)
   */
  public static List<MovieBannerNaming> getBannerNamesForMovie(Movie movie) {
    List<MovieBannerNaming> bannernames = new ArrayList<>();
    if (MovieModuleManager.getInstance().getSettings().getBannerFilenames().isEmpty()) {
      return bannernames;
    }

    if (movie.isMultiMovieDir()) {
      if (MovieModuleManager.getInstance().getSettings().getBannerFilenames().contains(MovieBannerNaming.FILENAME_BANNER)) {
        bannernames.add(MovieBannerNaming.FILENAME_BANNER);
      }
      if (bannernames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getBannerFilenames().isEmpty()) {
        bannernames.add(MovieBannerNaming.FILENAME_BANNER);
      }
    }
    else if (movie.isDisc()) {
      if (MovieModuleManager.getInstance().getSettings().getBannerFilenames().contains(MovieBannerNaming.BANNER)) {
        bannernames.add(MovieBannerNaming.BANNER);
      }
      if (bannernames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getBannerFilenames().isEmpty()) {
        bannernames.add(MovieBannerNaming.BANNER);
      }
    }
    else {
      bannernames.addAll(MovieModuleManager.getInstance().getSettings().getBannerFilenames());
    }
    return bannernames;
  }

  /**
   * Clearart format is not empty, so we want at least one ;)<br>
   * Idea is, to check whether the preferred format is set in settings<br>
   * and if not, take some default (since we want cleararts)
   *
   * @param movie
   *          the movie to get the clearart names for
   * @return list of MovieClearartNaming (can be empty!)
   */
  public static List<MovieClearartNaming> getClearartNamesForMovie(Movie movie) {
    List<MovieClearartNaming> clearartnames = new ArrayList<>();
    if (MovieModuleManager.getInstance().getSettings().getClearartFilenames().isEmpty()) {
      return clearartnames;
    }

    if (movie.isMultiMovieDir()) {
      if (MovieModuleManager.getInstance().getSettings().getClearartFilenames().contains(MovieClearartNaming.FILENAME_CLEARART)) {
        clearartnames.add(MovieClearartNaming.FILENAME_CLEARART);
      }
      if (clearartnames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getClearartFilenames().isEmpty()) {
        clearartnames.add(MovieClearartNaming.FILENAME_CLEARART);
      }
    }
    else if (movie.isDisc()) {
      if (MovieModuleManager.getInstance().getSettings().getClearartFilenames().contains(MovieClearartNaming.CLEARART)) {
        clearartnames.add(MovieClearartNaming.CLEARART);
      }
      if (clearartnames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getClearartFilenames().isEmpty()) {
        clearartnames.add(MovieClearartNaming.CLEARART);
      }
    }
    else {
      clearartnames.addAll(MovieModuleManager.getInstance().getSettings().getClearartFilenames());
    }
    return clearartnames;
  }

  /**
   * Discart format is not empty, so we want at least one ;)<br>
   * Idea is, to check whether the preferred format is set in settings<br>
   * and if not, take some default (since we want discarts)
   *
   * @param movie
   *          the movie to get the discart names for
   * @return list of MovieDiscartNaming (can be empty!)
   */
  public static List<MovieDiscartNaming> getDiscartNamesForMovie(Movie movie) {
    List<MovieDiscartNaming> discartnames = new ArrayList<>();
    if (MovieModuleManager.getInstance().getSettings().getDiscartFilenames().isEmpty()) {
      return discartnames;
    }

    if (movie.isMultiMovieDir()) {
      // all *DISC namings should resolve in FILENAME_DISC
      if (MovieModuleManager.getInstance().getSettings().getDiscartFilenames().contains(MovieDiscartNaming.FILENAME_DISC)
          || MovieModuleManager.getInstance().getSettings().getDiscartFilenames().contains(MovieDiscartNaming.DISC)) {
        discartnames.add(MovieDiscartNaming.FILENAME_DISC);
      }
      // all *DISCART namings should resolve in FILENAME_DISCART
      if (MovieModuleManager.getInstance().getSettings().getDiscartFilenames().contains(MovieDiscartNaming.FILENAME_DISCART)
          || MovieModuleManager.getInstance().getSettings().getDiscartFilenames().contains(MovieDiscartNaming.DISCART)) {
        discartnames.add(MovieDiscartNaming.FILENAME_DISCART);
      }
    }
    else if (movie.isDisc()) {
      // all *DISC namings should resolve in DISC
      if (MovieModuleManager.getInstance().getSettings().getDiscartFilenames().contains(MovieDiscartNaming.FILENAME_DISC)
          || MovieModuleManager.getInstance().getSettings().getDiscartFilenames().contains(MovieDiscartNaming.DISC)) {
        discartnames.add(MovieDiscartNaming.DISC);
      }
      // all *DISCART namings should resolve in DISCART
      if (MovieModuleManager.getInstance().getSettings().getDiscartFilenames().contains(MovieDiscartNaming.FILENAME_DISCART)
          || MovieModuleManager.getInstance().getSettings().getDiscartFilenames().contains(MovieDiscartNaming.DISCART)) {
        discartnames.add(MovieDiscartNaming.DISCART);
      }
    }
    else {
      discartnames.addAll(MovieModuleManager.getInstance().getSettings().getDiscartFilenames());
    }
    return discartnames;
  }

  /**
   * Keyart format is not empty, so we want at least one ;)<br>
   * Idea is, to check whether the preferred format is set in settings<br>
   * and if not, take some default (since we want keyarts)
   *
   * @param movie
   *          the movie to get the keyart names for
   * @return list of MovieKeyartNaming (can be empty!)
   */
  public static List<MovieKeyartNaming> getKeyartNamesForMovie(Movie movie) {
    List<MovieKeyartNaming> keyartnames = new ArrayList<>();
    if (MovieModuleManager.getInstance().getSettings().getKeyartFilenames().isEmpty()) {
      return keyartnames;
    }

    if (movie.isMultiMovieDir()) {
      if (MovieModuleManager.getInstance().getSettings().getKeyartFilenames().contains(MovieKeyartNaming.FILENAME_KEYART)) {
        keyartnames.add(MovieKeyartNaming.FILENAME_KEYART);
      }
      if (keyartnames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getKeyartFilenames().isEmpty()) {
        keyartnames.add(MovieKeyartNaming.FILENAME_KEYART);
      }
    }
    else if (movie.isDisc()) {
      if (MovieModuleManager.getInstance().getSettings().getKeyartFilenames().contains(MovieKeyartNaming.KEYART)) {
        keyartnames.add(MovieKeyartNaming.KEYART);
      }
      if (keyartnames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getKeyartFilenames().isEmpty()) {
        keyartnames.add(MovieKeyartNaming.KEYART);
      }
    }
    else {
      keyartnames.addAll(MovieModuleManager.getInstance().getSettings().getKeyartFilenames());
    }
    return keyartnames;
  }

  /**
   * Clearlogo format is not empty, so we want at least one ;)<br>
   * Idea is, to check whether the preferred format is set in settings<br>
   * and if not, take some default (since we want clearlogos)
   *
   * @param movie
   *          the movie to get the clearlogo names for
   * @return list of MovieClearlogoNaming (can be empty!)
   */
  public static List<MovieClearlogoNaming> getClearlogoNamesForMovie(Movie movie) {
    List<MovieClearlogoNaming> clearlogonames = new ArrayList<>();
    if (MovieModuleManager.getInstance().getSettings().getClearlogoFilenames().isEmpty()) {
      return clearlogonames;
    }

    if (movie.isMultiMovieDir()) {
      if (MovieModuleManager.getInstance().getSettings().getClearlogoFilenames().contains(MovieClearlogoNaming.FILENAME_CLEARLOGO)) {
        clearlogonames.add(MovieClearlogoNaming.FILENAME_CLEARLOGO);
      }
      else if (MovieModuleManager.getInstance().getSettings().getClearlogoFilenames().contains(MovieClearlogoNaming.FILENAME_LOGO)) {
        clearlogonames.add(MovieClearlogoNaming.FILENAME_LOGO);
      }

      if (clearlogonames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getClearlogoFilenames().isEmpty()) {
        clearlogonames.add(MovieClearlogoNaming.FILENAME_CLEARLOGO);
      }
    }
    else if (movie.isDisc()) {
      if (MovieModuleManager.getInstance().getSettings().getClearlogoFilenames().contains(MovieClearlogoNaming.CLEARLOGO)) {
        clearlogonames.add(MovieClearlogoNaming.CLEARLOGO);
      }
      else if (MovieModuleManager.getInstance().getSettings().getClearlogoFilenames().contains(MovieClearlogoNaming.LOGO)) {
        clearlogonames.add(MovieClearlogoNaming.LOGO);
      }

      if (clearlogonames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getClearlogoFilenames().isEmpty()) {
        clearlogonames.add(MovieClearlogoNaming.CLEARLOGO);
      }
    }
    else {
      clearlogonames.addAll(MovieModuleManager.getInstance().getSettings().getClearlogoFilenames());
    }
    return clearlogonames;
  }

  /**
   * Thumb format is not empty, so we want at least one ;)<br>
   * Idea is, to check whether the preferred format is set in settings<br>
   * and if not, take some default (since we want thumbs)
   *
   * @param movie
   *          the movie to get the thumb names for
   * @return list of MovieThumbNaming (can be empty!)
   */
  public static List<MovieThumbNaming> getThumbNamesForMovie(Movie movie) {
    List<MovieThumbNaming> thumbnames = new ArrayList<>();
    if (MovieModuleManager.getInstance().getSettings().getThumbFilenames().isEmpty()) {
      return thumbnames;
    }

    if (movie.isMultiMovieDir()) {
      if (MovieModuleManager.getInstance().getSettings().getThumbFilenames().contains(MovieThumbNaming.FILENAME_THUMB)) {
        thumbnames.add(MovieThumbNaming.FILENAME_THUMB);
      }
      if (MovieModuleManager.getInstance().getSettings().getThumbFilenames().contains(MovieThumbNaming.FILENAME_LANDSCAPE)) {
        thumbnames.add(MovieThumbNaming.FILENAME_LANDSCAPE);
      }
      if (thumbnames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getThumbFilenames().isEmpty()
          && MovieModuleManager.getInstance().getSettings().getThumbFilenames().contains(MovieThumbNaming.THUMB)) {
        thumbnames.add(MovieThumbNaming.FILENAME_THUMB);
      }
      if (thumbnames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getThumbFilenames().isEmpty()) {
        thumbnames.add(MovieThumbNaming.FILENAME_LANDSCAPE);
      }
    }
    else if (movie.isDisc()) {
      if (MovieModuleManager.getInstance().getSettings().getThumbFilenames().contains(MovieThumbNaming.THUMB)) {
        thumbnames.add(MovieThumbNaming.THUMB);
      }
      if (MovieModuleManager.getInstance().getSettings().getThumbFilenames().contains(MovieThumbNaming.LANDSCAPE)) {
        thumbnames.add(MovieThumbNaming.LANDSCAPE);
      }
      if (thumbnames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getThumbFilenames().isEmpty()
          && MovieModuleManager.getInstance().getSettings().getThumbFilenames().contains(MovieThumbNaming.FILENAME_THUMB)) {
        thumbnames.add(MovieThumbNaming.THUMB);
      }
      if (thumbnames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getThumbFilenames().isEmpty()) {
        thumbnames.add(MovieThumbNaming.LANDSCAPE);
      }
    }
    else {
      thumbnames.addAll(MovieModuleManager.getInstance().getSettings().getThumbFilenames());
    }
    return thumbnames;
  }

  /**
   * Idea is, to check whether the preferred format is set in settings<br>
   * and if not, take some default (since we want extrafanarts)
   *
   * @param movie
   *          the movie to get the fanart names for
   * @return List of MovieFanartNaming (can be empty!)
   */
  public static List<MovieExtraFanartNaming> getExtraFanartNamesForMovie(Movie movie) {
    List<MovieExtraFanartNaming> fanartnames = new ArrayList<>();
    if (MovieModuleManager.getInstance().getSettings().getExtraFanartFilenames().isEmpty()) {
      return fanartnames;
    }
    if (movie.isMultiMovieDir()) {
      if (MovieModuleManager.getInstance().getSettings().getExtraFanartFilenames().contains(MovieExtraFanartNaming.FILENAME_EXTRAFANART)) {
        fanartnames.add(MovieExtraFanartNaming.FILENAME_EXTRAFANART);
      }
      if (MovieModuleManager.getInstance().getSettings().getExtraFanartFilenames().contains(MovieExtraFanartNaming.FILENAME_EXTRAFANART2)) {
        fanartnames.add(MovieExtraFanartNaming.FILENAME_EXTRAFANART2);
      }
      if (fanartnames.isEmpty() && !MovieModuleManager.getInstance().getSettings().getExtraFanartFilenames().isEmpty()) {
        fanartnames.add(MovieExtraFanartNaming.FILENAME_EXTRAFANART);
      }
    }
    else if (movie.isDisc()) {
      fanartnames.add(MovieExtraFanartNaming.EXTRAFANART);
    }
    else {
      fanartnames = MovieModuleManager.getInstance().getSettings().getExtraFanartFilenames();
    }

    return fanartnames;
  }

  private static void downloadExtraArtwork(Movie movie, MediaFileType type) {
    // get images in thread
    MovieExtraImageFetcherTask task = new MovieExtraImageFetcherTask(movie, type);
    TmmTaskManager.getInstance().addImageDownloadTask(task);
  }

  /**
   * set the found artwork for the given movie
   * 
   * @param movie
   *          the movie to set the artwork for
   * @param artwork
   *          a list of all artworks to be set
   * @param config
   *          the config which artwork to set
   * @param overwrite
   *          should we overwrite existing artwork
   */
  public static void setArtwork(Movie movie, List<MediaArtwork> artwork, List<MovieScraperMetadataConfig> config, boolean overwrite) {
    if (!ScraperMetadataConfig.containsAnyArtwork(config)) {
      return;
    }

    // poster
    if (config.contains(MovieScraperMetadataConfig.POSTER) && (overwrite || StringUtils.isBlank(movie.getArtworkFilename(MediaFileType.POSTER)))) {
      setBestPoster(movie, artwork, overwrite);
    }

    // fanart
    if (config.contains(MovieScraperMetadataConfig.FANART) && (overwrite || StringUtils.isBlank(movie.getArtworkFilename(MediaFileType.FANART)))) {
      setBestFanart(movie, artwork, overwrite);
    }

    // works now for single & multimovie
    if (config.contains(MovieScraperMetadataConfig.CLEARLOGO)
        && (overwrite || StringUtils.isBlank(movie.getArtworkFilename(MediaFileType.CLEARLOGO)))) {
      setBestArtwork(movie, artwork, MediaArtworkType.CLEARLOGO, !MovieModuleManager.getInstance().getSettings().getClearlogoFilenames().isEmpty(),
          overwrite);
    }

    if (config.contains(MovieScraperMetadataConfig.CLEARART)
        && (overwrite || StringUtils.isBlank(movie.getArtworkFilename(MediaFileType.CLEARART)))) {
      setBestArtwork(movie, artwork, MediaArtworkType.CLEARART, !MovieModuleManager.getInstance().getSettings().getClearartFilenames().isEmpty(),
          overwrite);
    }

    if (config.contains(MovieScraperMetadataConfig.BANNER) && (overwrite || StringUtils.isBlank(movie.getArtworkFilename(MediaFileType.BANNER)))) {
      setBestArtwork(movie, artwork, MediaArtworkType.BANNER, !MovieModuleManager.getInstance().getSettings().getBannerFilenames().isEmpty(),
          overwrite);
    }

    if (config.contains(MovieScraperMetadataConfig.THUMB) && (overwrite || StringUtils.isBlank(movie.getArtworkFilename(MediaFileType.THUMB)))) {
      setBestArtwork(movie, artwork, MediaArtworkType.THUMB, !MovieModuleManager.getInstance().getSettings().getThumbFilenames().isEmpty(),
          overwrite);
    }

    if (config.contains(MovieScraperMetadataConfig.DISCART) && (overwrite || StringUtils.isBlank(movie.getArtworkFilename(MediaFileType.DISC)))) {
      setBestArtwork(movie, artwork, MediaArtworkType.DISC, !MovieModuleManager.getInstance().getSettings().getDiscartFilenames().isEmpty(),
          overwrite);
    }

    if (config.contains(MovieScraperMetadataConfig.KEYART) && (overwrite || StringUtils.isBlank(movie.getArtworkFilename(MediaFileType.KEYART)))) {
      setBestArtwork(movie, artwork, MediaArtworkType.KEYART, !MovieModuleManager.getInstance().getSettings().getKeyartFilenames().isEmpty(),
          overwrite);
    }

    // extrathumbs
    if (config.contains(MovieScraperMetadataConfig.EXTRATHUMB)
        && (overwrite || StringUtils.isBlank(movie.getArtworkFilename(MediaFileType.EXTRATHUMB)))) {
      List<String> extrathumbs = new ArrayList<>();
      if (MovieModuleManager.getInstance().getSettings().isImageExtraThumbs()
          && MovieModuleManager.getInstance().getSettings().getImageExtraThumbsCount() > 0) {
        // sort the fanarts
        List<MediaArtwork.ImageSizeAndUrl> sortedFanarts = sortArtworkUrls(artwork, MediaArtworkType.BACKGROUND,
            MovieModuleManager.getInstance().getSettings().getImageFanartSize().getOrder());

        // and add them according to the want amount
        for (MediaArtwork.ImageSizeAndUrl art : sortedFanarts) {
          extrathumbs.add(art.getUrl());
          if (extrathumbs.size() >= MovieModuleManager.getInstance().getSettings().getImageExtraThumbsCount()) {
            break;
          }
        }

        movie.setExtraThumbs(extrathumbs);
        if (!extrathumbs.isEmpty()) {
          if (!movie.isMultiMovieDir()) {
            downloadArtwork(movie, MediaFileType.EXTRATHUMB);
          }
        }
      }
    }

    // extrafanarts
    if (config.contains(MovieScraperMetadataConfig.EXTRAFANART)
        && (overwrite || StringUtils.isBlank(movie.getArtworkFilename(MediaFileType.EXTRAFANART)))) {
      List<String> extrafanarts = new ArrayList<>();
      if (MovieModuleManager.getInstance().getSettings().isImageExtraFanart()
          && MovieModuleManager.getInstance().getSettings().getImageExtraFanartCount() > 0) {
        // sort the fanarts
        List<MediaArtwork.ImageSizeAndUrl> sortedFanarts = sortArtworkUrls(artwork, MediaArtworkType.BACKGROUND,
            MovieModuleManager.getInstance().getSettings().getImageFanartSize().getOrder());

        // and add them according to the want amount
        for (MediaArtwork.ImageSizeAndUrl art : sortedFanarts) {
          extrafanarts.add(art.getUrl());
          if (extrafanarts.size() >= MovieModuleManager.getInstance().getSettings().getImageExtraFanartCount()) {
            break;
          }
        }

        movie.setExtraFanarts(extrafanarts);
        if (!extrafanarts.isEmpty()) {
          if (!movie.isMultiMovieDir()) {
            downloadArtwork(movie, MediaFileType.EXTRAFANART);
          }
        }
      }
    }

    // update DB
    movie.saveToDb();
    movie.writeNFO(); // rewrite NFO to get the urls into the NFO
  }

  /*
   * find the "best" poster in the list of artwork, assign it to the movie and download it
   */
  private static void setBestPoster(Movie movie, List<MediaArtwork> artwork, boolean overwrite) {
    boolean posterFound = false;

    // use existing data if available
    if (!overwrite && StringUtils.isNotBlank(movie.getArtworkUrl(MediaFileType.POSTER))) {
      posterFound = true;
    }
    else {
      // sort artwork due to our preferences
      int preferredSizeOrder = MovieModuleManager.getInstance().getSettings().getImagePosterSize().getOrder();
      List<MediaArtwork.ImageSizeAndUrl> sortedPosters = sortArtworkUrls(artwork, MediaArtworkType.POSTER, preferredSizeOrder);

      // assign and download the poster
      if (!sortedPosters.isEmpty()) {
        MediaArtwork.ImageSizeAndUrl foundPoster = sortedPosters.get(0);
        movie.setArtworkUrl(foundPoster.getUrl(), MediaFileType.POSTER);
        posterFound = true;
      }
    }

    // and download
    if (posterFound && !MovieModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty()) {
      downloadArtwork(movie, MediaFileType.POSTER);
    }
  }

  /*
   * find the "best" fanart in the list of artwork, assign it to the movie and download it
   */
  private static void setBestFanart(Movie movie, List<MediaArtwork> artwork, boolean overwrite) {
    boolean fanartFound = false;

    // use existing data if available
    if (!overwrite && StringUtils.isNotBlank(movie.getArtworkUrl(MediaFileType.POSTER))) {
      fanartFound = true;
    }
    else {
      int preferredSizeOrder = MovieModuleManager.getInstance().getSettings().getImageFanartSize().getOrder();

      // according to the kodi specifications the fanart _should_ be without any text on it - so we try to get the text-less image (in the right
      // resolution) first
      // https://kodi.wiki/view/Artwork_types#fanart
      MediaArtwork.ImageSizeAndUrl fanartWoText = null;

      if (MovieModuleManager.getInstance().getSettings().isImageScraperPreferFanartWoText()) {
        for (MediaArtwork art : artwork) {
          if (art.getType() == MediaArtworkType.BACKGROUND && StringUtils.isBlank(art.getLanguage())) {
            // right type
            for (MediaArtwork.ImageSizeAndUrl imageSizeAndUrl : art.getImageSizes()) {
              // right size
              if (imageSizeAndUrl.getSizeOrder() == preferredSizeOrder) {
                fanartWoText = imageSizeAndUrl;
                break;
              }
            }
          }
        }
      }

      // sort artwork due to our preferences
      List<MediaArtwork.ImageSizeAndUrl> sortedFanarts = sortArtworkUrls(artwork, MediaArtworkType.BACKGROUND, preferredSizeOrder);

      // and insert the text-less in the front
      if (fanartWoText != null) {
        sortedFanarts.add(0, fanartWoText);
      }

      // assign and download the fanart
      if (!sortedFanarts.isEmpty()) {
        MediaArtwork.ImageSizeAndUrl foundfanart = sortedFanarts.get(0);
        movie.setArtworkUrl(foundfanart.getUrl(), MediaFileType.FANART);
        fanartFound = true;
      }
    }
    // and download
    if (fanartFound && !MovieModuleManager.getInstance().getSettings().getFanartFilenames().isEmpty()) {
      downloadArtwork(movie, MediaFileType.FANART);
    }
  }

  /**
   * sort the artwork according to our preferences: <br/>
   * 1) the right language and the right resolution<br/>
   * 2) the right language and down to 2 resolutions lower (if language has priority)<br/>
   * 3) the right resolution<br/>
   * 4) down to 2 resolutions lower<br/>
   * 5) the first we find
   *
   * @param artwork
   *          the artworks to search in
   * @param type
   *          the artwork type
   * @param sizeOrder
   *          the preferred size
   * @return the found artwork or null
   */
  public static List<MediaArtwork.ImageSizeAndUrl> sortArtworkUrls(List<MediaArtwork> artwork, MediaArtworkType type, int sizeOrder) {
    List<MediaArtwork> artworkForType = new ArrayList<>(artwork.stream().filter(art -> art.getType() == type).toList());

    if (artwork.isEmpty()) {
      return Collections.emptyList();
    }

    List<MediaArtwork.ImageSizeAndUrl> sortedArtwork = new ArrayList<>();

    if (sizeOrder == 0) {
      // we do not have any sizeOrder -> we're sorting an artwork without a setting. So pre-sort by biggest artwork first
      artworkForType.sort((o1, o2) -> Integer.compare(o2.getBiggestArtwork().getWidth(), o1.getBiggestArtwork().getWidth()));
    }

    List<MediaLanguages> languages = MovieSettings.getInstance().getImageScraperLanguages();

    // get the artwork in the chosen language priority
    for (MediaLanguages language : languages) {
      // the right language and the right resolution
      for (MediaArtwork art : artworkForType.stream().filter(art -> art.getLanguage().equals(language.getLanguage())).toList()) {
        for (MediaArtwork.ImageSizeAndUrl imageSizeAndUrl : art.getImageSizes()) {
          if (imageSizeAndUrl.getSizeOrder() == sizeOrder && !sortedArtwork.contains(imageSizeAndUrl)) {
            sortedArtwork.add(imageSizeAndUrl);
          }
        }
      }
    }

    // nothing with the chosen language(s) found - just re-try with empty language in the right resolution
    if (sortedArtwork.isEmpty()) {
      for (MediaArtwork art : artworkForType.stream().filter(art -> art.getLanguage().equals("")).toList()) {
        for (MediaArtwork.ImageSizeAndUrl imageSizeAndUrl : art.getImageSizes()) {
          if (imageSizeAndUrl.getSizeOrder() == sizeOrder && !sortedArtwork.contains(imageSizeAndUrl)) {
            sortedArtwork.add(imageSizeAndUrl);
          }
        }
      }
    }

    // do we want to take other resolution artwork?
    if (MovieModuleManager.getInstance().getSettings().isImageScraperOtherResolutions()) {
      int newOrder = MediaArtwork.MAX_IMAGE_SIZE_ORDER;
      while (newOrder > 1) {
        newOrder = newOrder / 2;
        for (MediaLanguages language : languages) {
          // the right language and the right resolution
          for (MediaArtwork art : artworkForType.stream().filter(art -> art.getLanguage().equals(language.getLanguage())).toList()) {
            for (MediaArtwork.ImageSizeAndUrl imageSizeAndUrl : art.getImageSizes()) {
              if (imageSizeAndUrl.getSizeOrder() == newOrder && !sortedArtwork.contains(imageSizeAndUrl)) {
                sortedArtwork.add(imageSizeAndUrl);
              }
            }
          }
        }
      }
    }

    // FFmpeg - is there has been no hit, use FFmpeg when available
    if (sortedArtwork.isEmpty()) {
      for (MediaArtwork art : artworkForType) {
        if ("ffmpeg".equalsIgnoreCase(art.getProviderId())) {
          sortedArtwork.addAll(art.getImageSizes());
        }
      }
    }

    // should we fall back to _any_ artwork?
    if (MovieModuleManager.getInstance().getSettings().isImageScraperFallback()) {
      for (MediaArtwork art : artworkForType) {
        for (MediaArtwork.ImageSizeAndUrl imageSizeAndUrl : art.getImageSizes()) {
          if (!sortedArtwork.contains(imageSizeAndUrl)) {
            sortedArtwork.add(imageSizeAndUrl);
          }
        }
      }
    }

    return sortedArtwork;
  }

  public static int getMatchingScoreAccordingPreferences(MediaArtwork ma) {

    List<MediaLanguages> languages = MovieModuleManager.getInstance().getSettings().getImageScraperLanguages();
    int size = 0;
    switch (ma.getType()) {
      case POSTER:
      case KEYART:
        size = MovieModuleManager.getInstance().getSettings().getImagePosterSize().getOrder();
        break;

      // all other use fanart size (as seen in Fanart.Tv metadata provider imageType mapping
      default:
        size = MovieModuleManager.getInstance().getSettings().getImageFanartSize().getOrder();
        break;
    }

    boolean preferFanartWoText = MovieModuleManager.getInstance().getSettings().isImageScraperPreferFanartWoText();
    boolean otherResolutions = MovieModuleManager.getInstance().getSettings().isImageScraperOtherResolutions();
    int score = ma.getMatchingScoreAccordingPreferences(size, languages, preferFanartWoText, otherResolutions);
    return score;
  }

  /**
   * choose the best artwork for this movie
   * 
   * @param movie
   *          our movie
   * @param artwork
   *          the artwork list
   * @param type
   *          the type to download
   * @param download
   *          indicates, whether to download and add, OR JUST SAVE THE URL for a later download
   * @param overwrite
   *          indicates that we might be able to overwrite existing artwork
   */
  private static void setBestArtwork(Movie movie, List<MediaArtwork> artwork, MediaArtworkType type, boolean download, boolean overwrite) {
    boolean artworkFound = false;

    // use existing data if available
    if (!overwrite && StringUtils.isNotBlank(movie.getArtworkUrl(MediaFileType.getMediaFileType(type)))) {
      artworkFound = true;
    }
    else {
      // sort artwork due to our preferences
      // this is everything but the poster/fanart - so we must not use the fanart size here
      int preferredSizeOrder = MediaArtwork.MAX_IMAGE_SIZE_ORDER; // big enough to catch _all_ sizes
      List<MediaArtwork.ImageSizeAndUrl> sortedArtworks = sortArtworkUrls(artwork, type, preferredSizeOrder);

      if (!sortedArtworks.isEmpty()) {
        MediaArtwork.ImageSizeAndUrl bestArtwork = sortedArtworks.get(0);
        movie.setArtworkUrl(bestArtwork.getUrl(), MediaFileType.getMediaFileType(type));
        artworkFound = true;
      }
    }

    // and download
    if (download && artworkFound) {
      downloadArtwork(movie, MediaFileType.getMediaFileType(type));
    }
  }

  /**
   * cleanup the artwork files for the given movies and config. This is also done in the rename&cleanup function, but if users want to
   * download/cleanup the missing artwork, they expect us to do this work too (without rename the whole movie)
   * 
   * @param movie
   *          the movie to do the cleanup for
   * @param metadataConfig
   *          the config which artwork should be cleaned up
   */
  public static void cleanupArtwork(Movie movie, List<MovieScraperMetadataConfig> metadataConfig) {
    if (!ScraperMetadataConfig.containsAnyArtwork(metadataConfig)) {
      return;
    }

    // do the cleanup for every given type; there _should_ be at least one artwork for every given type
    // if there is no artwork available we cannot do any cleanup here - obviously
    for (MovieScraperMetadataConfig config : metadataConfig) {
      // we need to get the artwork type for this config type
      MediaFileType type = getMediaFileTypeForConfig(config);
      if (type == null) {
        continue;
      }

      // get all available artwork files for the given type
      List<MediaFile> mediaFiles = movie.getMediaFiles(type);
      if (mediaFiles.isEmpty()) {
        // no media files? we cannot do any cleanup without artwork files here
        continue;
      }

      // get all expected file namings
      List<IFileNaming> fileNamings = getFileNamingsForMediaFileType(movie, type);

      // now check if there is a media file for all _needed_ file naming (copy the existing otherwise)
      // take the first mediafile as template
      MediaFile baseArtwork = mediaFiles.get(0);

      for (IFileNaming fileNaming : fileNamings) {
        String filename = getArtworkFilename(movie, fileNaming, baseArtwork.getExtension());
        if (StringUtils.isNotBlank(filename)) {
          MediaFile otherArtwork = new MediaFile(movie.getPathNIO().resolve(filename));
          if (!mediaFiles.contains(otherArtwork)) {
            // not existing? copy it
            boolean ok = MovieRenamer.copyFile(baseArtwork.getFileAsPath(), otherArtwork.getFileAsPath());
            if (ok) {
              movie.addToMediaFiles(otherArtwork);
            }
          }
        }
      }
    }
  }

  /**
   * get the corresponding {@link MediaFileType} for the given {@link MovieScraperMetadataConfig}
   * 
   * @param config
   *          the {@link MovieScraperMetadataConfig} to get the {@link MediaFileType} for
   * @return the {@link MediaFileType} or null (if no valid config has been passed)
   */
  private static MediaFileType getMediaFileTypeForConfig(MovieScraperMetadataConfig config) {
    if (!config.isArtwork()) {
      return null;
    }

    // try to get it dynamically (because most enums have the same name)
    MediaFileType type = null;

    try {
      type = MediaFileType.valueOf(config.name());
    }
    catch (Exception ignored) {
      // no corresponding enum found
      if (config == MovieScraperMetadataConfig.DISCART) {
        type = MediaFileType.DISC;
      }
    }

    return type;
  }

  /**
   * get all configured {@link IFileNaming}s for the given {@link Movie} and {@link MediaFileType}
   * 
   * @param movie
   *          the movie
   * @param type
   *          the file type
   * @return a list of all configured file namings
   */
  private static List<IFileNaming> getFileNamingsForMediaFileType(Movie movie, MediaFileType type) {
    List<IFileNaming> fileNamings = new ArrayList<>(0);

    switch (type) {
      case FANART:
        fileNamings.addAll(getFanartNamesForMovie(movie));
        break;

      case POSTER:
        fileNamings.addAll(getPosterNamesForMovie(movie));
        break;

      case CLEARLOGO:
        fileNamings.addAll(getClearlogoNamesForMovie(movie));
        break;

      case BANNER:
        fileNamings.addAll(getBannerNamesForMovie(movie));
        break;

      case CLEARART:
        fileNamings.addAll(getClearartNamesForMovie(movie));
        break;

      case THUMB:
        fileNamings.addAll(getThumbNamesForMovie(movie));
        break;

      case DISC:
        fileNamings.addAll(getDiscartNamesForMovie(movie));
        break;

      case KEYART:
        fileNamings.addAll(getKeyartNamesForMovie(movie));
        break;

      default:
        break;
    }

    return fileNamings;
  }

  /**
   * If found, copy the bluray metadata poster files into movieDir and add them as MF
   * 
   * @param movie
   *          the movie to extract BluRay posters
   * @return true if ok or none found, false otherwise
   */
  public static boolean extractBlurayPosters(Movie movie) {
    Path dlFolder = movie.getPathNIO().resolve("BDMV/META/DL/");
    if (Files.exists(dlFolder)) {

      List<Path> files = Utils.listFiles(dlFolder);
      MediaFile largestPoster = null;
      for (Path file : files) {
        MediaFile mf = new MediaFile(file);
        if (mf.isGraphic()) {
          MediaFileHelper.gatherFileInformation(mf); // basics as size
          if (largestPoster == null || largestPoster.getFilesize() < mf.getFilesize()) {
            largestPoster = mf;
          }
        }
      }

      // we found some graphic - add it to our MFs as kickstart
      if (largestPoster != null) {
        // since we do not want the original to be renamed/removed, we make a copy
        if (MovieModuleManager.getInstance().getSettings().isExtractArtworkFromVsmeta()) {
          try {
            Path newFile = movie.getPathNIO().resolve("BDMV-poster." + largestPoster.getExtension());
            Utils.copyFileSafe(largestPoster.getFileAsPath(), newFile);
            MediaFile poster = new MediaFile(newFile);
            movie.addToMediaFiles(poster);
          }
          catch (IOException e) {
            LOGGER.trace("Could not extract Bluray poster: {}", e.getMessage());
            return false;
          }
        }
      }

    } // end meta/dl files
    return true;
  }

  /**
   * extract embedded artwork from a VSMETA file to the destinations specified in the settings
   * 
   * @param movie
   *          the {@link Movie} to assign the new {@link MediaFile}s to
   * @param vsMetaFile
   *          the VSMETA {@link MediaFile}
   * @param artworkType
   *          the {@link MediaArtworkType}
   * @return true if extraction was successful, false otherwise
   */
  public static boolean extractArtworkFromVsmeta(Movie movie, MediaFile vsMetaFile, MediaArtworkType artworkType) {
    VSMeta vsmeta = new VSMeta(vsMetaFile.getFileAsPath());
    vsmeta.parseFile();
    List<? extends IFileNaming> fileNamings;
    byte[] bytes;

    switch (artworkType) {
      case POSTER:
        fileNamings = MovieModuleManager.getInstance().getSettings().getPosterFilenames();
        bytes = vsmeta.getPosterBytes();
        break;

      case BACKGROUND:
        fileNamings = MovieModuleManager.getInstance().getSettings().getFanartFilenames();
        bytes = vsmeta.getBackdropBytes();
        break;

      default:
        return false;
    }

    if (fileNamings.isEmpty() || bytes.length == 0) {
      return false;
    }

    // remove .ext.vsmeta
    String basename = FilenameUtils.getBaseName(FilenameUtils.getBaseName(vsMetaFile.getFilename()));

    for (IFileNaming fileNaming : fileNamings) {
      try {
        String filename = fileNaming.getFilename(basename, "jpg"); // need to force jpg here since we do know it better
        MediaFile mf = new MediaFile(vsMetaFile.getFileAsPath().getParent().resolve(filename), MediaFileType.getMediaFileType(artworkType));
        Files.write(mf.getFileAsPath(), bytes);
        movie.addToMediaFiles(mf);
      }
      catch (Exception e) {
        LOGGER.warn("could not extract VSMETA artwork: {}", e.getMessage());
      }
    }

    return true;
  }

  /**
   * parse out the last number of the filename which is the index of extra artwork
   * 
   * @param filename
   *          the filename containing the index
   * @return the detected index or -1
   */
  public static int getIndexOfArtwork(String filename) {
    String basename = FilenameUtils.getBaseName(filename);
    Matcher matcher = INDEX_PATTERN.matcher(basename);
    if (matcher.find() && matcher.groupCount() == 1) {
      try {
        return Integer.parseInt(matcher.group(1));
      }
      catch (Exception e) {
        LOGGER.debug("could not parse index of '{}'- {}", filename, e.getMessage());
      }
    }

    return -1;
  }
}
