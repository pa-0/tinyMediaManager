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

import org.tinymediamanager.core.CertificationStyle;
import org.tinymediamanager.core.movie.connector.MovieConnectors;
import org.tinymediamanager.core.movie.filenaming.MovieBannerNaming;
import org.tinymediamanager.core.movie.filenaming.MovieClearartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieClearlogoNaming;
import org.tinymediamanager.core.movie.filenaming.MovieDiscartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieExtraFanartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieFanartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieKeyartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieNfoNaming;
import org.tinymediamanager.core.movie.filenaming.MoviePosterNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetBannerNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetClearartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetClearlogoNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetDiscartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetFanartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetNfoNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetPosterNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetThumbNaming;
import org.tinymediamanager.core.movie.filenaming.MovieThumbNaming;
import org.tinymediamanager.core.movie.filenaming.MovieTrailerNaming;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;

/**
 * the class {@link MovieSettingsDefaults} is a helper class for default settings of the movie module
 * 
 * @author Manuel Laggner
 */
public class MovieSettingsDefaults {

  private MovieSettingsDefaults() {
    throw new IllegalAccessError();
  }

  /**
   * set the default scrapers for the movie module
   */
  public static void setDefaultScrapers() {
    MovieSettings movieSettings = MovieSettings.getInstance();

    // activate default scrapers (hand curated list of defaults)
    movieSettings.artworkScrapers.clear();
    for (MediaScraper ms : MediaScraper.getMediaScrapers(ScraperType.MOVIE_ARTWORK)) {
      switch (ms.getId()) {
        case "tmdb":
        case "fanarttv":
          movieSettings.addMovieArtworkScraper(ms.getId());
          break;

        default:
          break;
      }
    }

    movieSettings.trailerScrapers.clear();
    for (MediaScraper ms : MediaScraper.getMediaScrapers(ScraperType.MOVIE_TRAILER)) {
      switch (ms.getId()) {
        case "tmdb":
        case "hd-trailers":
        case "davesTrailer":
          movieSettings.addMovieTrailerScraper(ms.getId());
          break;

        default:
          break;
      }
    }

    movieSettings.subtitleScrapers.clear();
    for (MediaScraper ms : MediaScraper.getMediaScrapers(ScraperType.MOVIE_SUBTITLE)) {
      movieSettings.addMovieSubtitleScraper(ms.getId());
    }
  }

  /**
   * XBMC/Kodi <17 defaults
   */
  public static void setDefaultSettingsForXbmc() {
    MovieSettings movieSettings = MovieSettings.getInstance();

    // file names
    movieSettings.nfoFilenames.clear();
    movieSettings.addNfoFilename(MovieNfoNaming.FILENAME_NFO);

    movieSettings.posterFilenames.clear();
    movieSettings.addPosterFilename(MoviePosterNaming.FILENAME_POSTER);

    movieSettings.fanartFilenames.clear();
    movieSettings.addFanartFilename(MovieFanartNaming.FILENAME_FANART);

    movieSettings.extraFanartFilenames.clear();
    movieSettings.addExtraFanartFilename(MovieExtraFanartNaming.FOLDER_EXTRAFANART);

    movieSettings.bannerFilenames.clear();
    movieSettings.addBannerFilename(MovieBannerNaming.FILENAME_BANNER);

    movieSettings.clearartFilenames.clear();
    movieSettings.addClearartFilename(MovieClearartNaming.FILENAME_CLEARART);

    movieSettings.thumbFilenames.clear();
    movieSettings.addThumbFilename(MovieThumbNaming.FILENAME_LANDSCAPE);

    movieSettings.clearlogoFilenames.clear();
    movieSettings.addClearlogoFilename(MovieClearlogoNaming.FILENAME_CLEARLOGO);

    movieSettings.discartFilenames.clear();
    movieSettings.addDiscartFilename(MovieDiscartNaming.FILENAME_DISC);

    movieSettings.keyartFilenames.clear();
    movieSettings.addKeyartFilename(MovieKeyartNaming.FILENAME_KEYART);

    movieSettings.movieSetNfoFilenames.clear();

    movieSettings.movieSetPosterFilenames.clear();
    movieSettings.addMovieSetPosterFilename(MovieSetPosterNaming.MOVIE_POSTER);

    movieSettings.movieSetFanartFilenames.clear();
    movieSettings.addMovieSetFanartFilename(MovieSetFanartNaming.MOVIE_FANART);

    movieSettings.movieSetBannerFilenames.clear();
    movieSettings.addMovieSetBannerFilename(MovieSetBannerNaming.MOVIE_BANNER);

    movieSettings.movieSetClearartFilenames.clear();
    movieSettings.addMovieSetClearartFilename(MovieSetClearartNaming.MOVIE_CLEARART);

    movieSettings.movieSetThumbFilenames.clear();
    movieSettings.addMovieSetThumbFilename(MovieSetThumbNaming.MOVIE_LANDSCAPE);

    movieSettings.movieSetClearlogoFilenames.clear();
    movieSettings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.MOVIE_CLEARLOGO);

    movieSettings.movieSetDiscartFilenames.clear();
    movieSettings.addMovieSetDiscartFilename(MovieSetDiscartNaming.MOVIE_DISCART);

    movieSettings.trailerFilenames.clear();
    movieSettings.addTrailerFilename(MovieTrailerNaming.FILENAME_TRAILER);

    // other settings
    movieSettings.setMovieConnector(MovieConnectors.XBMC);
    movieSettings.setCertificationStyle(CertificationStyle.LARGE);
    movieSettings.setNfoDiscFolderInside(true);
    movieSettings.setTrailerDiscFolderInside(true);

    movieSettings.firePropertyChange("preset", false, true);
  }

  /**
   * Kodi 17+ defaults
   */
  public static void setDefaultSettingsForKodi() {
    MovieSettings movieSettings = MovieSettings.getInstance();

    // file names
    movieSettings.nfoFilenames.clear();
    movieSettings.addNfoFilename(MovieNfoNaming.FILENAME_NFO);

    movieSettings.posterFilenames.clear();
    movieSettings.addPosterFilename(MoviePosterNaming.FILENAME_POSTER);

    movieSettings.fanartFilenames.clear();
    movieSettings.addFanartFilename(MovieFanartNaming.FILENAME_FANART);

    movieSettings.extraFanartFilenames.clear();
    movieSettings.addExtraFanartFilename(MovieExtraFanartNaming.FILENAME_EXTRAFANART);

    movieSettings.bannerFilenames.clear();
    movieSettings.addBannerFilename(MovieBannerNaming.FILENAME_BANNER);

    movieSettings.clearartFilenames.clear();
    movieSettings.addClearartFilename(MovieClearartNaming.FILENAME_CLEARART);

    movieSettings.thumbFilenames.clear();
    movieSettings.addThumbFilename(MovieThumbNaming.FILENAME_LANDSCAPE);

    movieSettings.clearlogoFilenames.clear();
    movieSettings.addClearlogoFilename(MovieClearlogoNaming.FILENAME_CLEARLOGO);

    movieSettings.discartFilenames.clear();
    movieSettings.addDiscartFilename(MovieDiscartNaming.FILENAME_DISCART);

    movieSettings.keyartFilenames.clear();
    movieSettings.addKeyartFilename(MovieKeyartNaming.FILENAME_KEYART);

    movieSettings.movieSetNfoFilenames.clear();
    movieSettings.addMovieSetNfoFilename(MovieSetNfoNaming.KODI_NFO);

    movieSettings.movieSetPosterFilenames.clear();
    movieSettings.addMovieSetPosterFilename(MovieSetPosterNaming.KODI_POSTER);

    movieSettings.movieSetFanartFilenames.clear();
    movieSettings.addMovieSetFanartFilename(MovieSetFanartNaming.KODI_FANART);

    movieSettings.movieSetBannerFilenames.clear();
    movieSettings.addMovieSetBannerFilename(MovieSetBannerNaming.KODI_BANNER);

    movieSettings.movieSetClearartFilenames.clear();
    movieSettings.addMovieSetClearartFilename(MovieSetClearartNaming.KODI_CLEARART);

    movieSettings.movieSetThumbFilenames.clear();
    movieSettings.addMovieSetThumbFilename(MovieSetThumbNaming.KODI_LANDSCAPE);

    movieSettings.movieSetClearlogoFilenames.clear();
    movieSettings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.KODI_CLEARLOGO);

    movieSettings.movieSetDiscartFilenames.clear();
    movieSettings.addMovieSetDiscartFilename(MovieSetDiscartNaming.KODI_DISCART);

    movieSettings.trailerFilenames.clear();
    movieSettings.addTrailerFilename(MovieTrailerNaming.FILENAME_TRAILER);

    // other settings
    movieSettings.setMovieConnector(MovieConnectors.KODI);
    movieSettings.setCertificationStyle(CertificationStyle.LARGE);
    movieSettings.setNfoDiscFolderInside(true);
    movieSettings.setTrailerDiscFolderInside(true);

    movieSettings.firePropertyChange("preset", false, true);
  }

  /**
   * MediaPortal 1 defaults
   */
  public static void setDefaultSettingsForMediaPortal1() {
    MovieSettings movieSettings = MovieSettings.getInstance();

    // file names
    movieSettings.nfoFilenames.clear();
    movieSettings.addNfoFilename(MovieNfoNaming.FILENAME_NFO);

    movieSettings.posterFilenames.clear();
    movieSettings.addPosterFilename(MoviePosterNaming.POSTER);

    movieSettings.fanartFilenames.clear();
    movieSettings.addFanartFilename(MovieFanartNaming.FANART);

    movieSettings.extraFanartFilenames.clear();
    movieSettings.addExtraFanartFilename(MovieExtraFanartNaming.FOLDER_EXTRAFANART);

    movieSettings.bannerFilenames.clear();
    movieSettings.addBannerFilename(MovieBannerNaming.BANNER);

    movieSettings.clearartFilenames.clear();
    movieSettings.addClearartFilename(MovieClearartNaming.CLEARART);

    movieSettings.thumbFilenames.clear();
    movieSettings.addThumbFilename(MovieThumbNaming.THUMB);

    movieSettings.clearlogoFilenames.clear();
    movieSettings.addClearlogoFilename(MovieClearlogoNaming.CLEARLOGO);

    movieSettings.discartFilenames.clear();
    movieSettings.addDiscartFilename(MovieDiscartNaming.DISC);

    movieSettings.keyartFilenames.clear();
    movieSettings.addKeyartFilename(MovieKeyartNaming.KEYART);

    movieSettings.movieSetNfoFilenames.clear();
    movieSettings.addMovieSetNfoFilename(MovieSetNfoNaming.KODI_NFO);

    movieSettings.movieSetPosterFilenames.clear();
    movieSettings.addMovieSetPosterFilename(MovieSetPosterNaming.MOVIE_POSTER);

    movieSettings.movieSetFanartFilenames.clear();
    movieSettings.addMovieSetFanartFilename(MovieSetFanartNaming.MOVIE_FANART);

    movieSettings.movieSetBannerFilenames.clear();
    movieSettings.addMovieSetBannerFilename(MovieSetBannerNaming.MOVIE_BANNER);

    movieSettings.movieSetClearartFilenames.clear();
    movieSettings.addMovieSetClearartFilename(MovieSetClearartNaming.MOVIE_CLEARART);

    movieSettings.movieSetThumbFilenames.clear();
    movieSettings.addMovieSetThumbFilename(MovieSetThumbNaming.MOVIE_LANDSCAPE);

    movieSettings.movieSetClearlogoFilenames.clear();
    movieSettings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.MOVIE_CLEARLOGO);

    movieSettings.movieSetDiscartFilenames.clear();
    movieSettings.addMovieSetDiscartFilename(MovieSetDiscartNaming.MOVIE_DISCART);

    movieSettings.trailerFilenames.clear();
    movieSettings.addTrailerFilename(MovieTrailerNaming.FILENAME_TRAILER);

    // other settings
    movieSettings.setMovieConnector(MovieConnectors.MP);
    movieSettings.setCertificationStyle(CertificationStyle.TECHNICAL);
    movieSettings.setNfoDiscFolderInside(true);
    movieSettings.setTrailerDiscFolderInside(true);

    movieSettings.firePropertyChange("preset", false, true);
  }

  /**
   * MediaPortal 2 defaults
   */
  public static void setDefaultSettingsForMediaPortal2() {
    MovieSettings movieSettings = MovieSettings.getInstance();

    // file names
    movieSettings.nfoFilenames.clear();
    movieSettings.addNfoFilename(MovieNfoNaming.FILENAME_NFO);

    movieSettings.posterFilenames.clear();
    movieSettings.addPosterFilename(MoviePosterNaming.POSTER);

    movieSettings.fanartFilenames.clear();
    movieSettings.addFanartFilename(MovieFanartNaming.FANART);

    movieSettings.extraFanartFilenames.clear();
    movieSettings.addExtraFanartFilename(MovieExtraFanartNaming.FOLDER_EXTRAFANART);

    movieSettings.bannerFilenames.clear();
    movieSettings.addBannerFilename(MovieBannerNaming.BANNER);

    movieSettings.clearartFilenames.clear();
    movieSettings.addClearartFilename(MovieClearartNaming.CLEARART);

    movieSettings.thumbFilenames.clear();
    movieSettings.addThumbFilename(MovieThumbNaming.THUMB);

    movieSettings.clearlogoFilenames.clear();
    movieSettings.addClearlogoFilename(MovieClearlogoNaming.CLEARLOGO);

    movieSettings.discartFilenames.clear();
    movieSettings.addDiscartFilename(MovieDiscartNaming.DISC);

    movieSettings.keyartFilenames.clear();
    movieSettings.addKeyartFilename(MovieKeyartNaming.KEYART);

    movieSettings.movieSetNfoFilenames.clear();
    movieSettings.addMovieSetNfoFilename(MovieSetNfoNaming.KODI_NFO);

    movieSettings.movieSetPosterFilenames.clear();
    movieSettings.addMovieSetPosterFilename(MovieSetPosterNaming.MOVIE_POSTER);

    movieSettings.movieSetFanartFilenames.clear();
    movieSettings.addMovieSetFanartFilename(MovieSetFanartNaming.MOVIE_FANART);

    movieSettings.movieSetBannerFilenames.clear();
    movieSettings.addMovieSetBannerFilename(MovieSetBannerNaming.MOVIE_BANNER);

    movieSettings.movieSetClearartFilenames.clear();
    movieSettings.addMovieSetClearartFilename(MovieSetClearartNaming.MOVIE_CLEARART);

    movieSettings.movieSetThumbFilenames.clear();
    movieSettings.addMovieSetThumbFilename(MovieSetThumbNaming.MOVIE_LANDSCAPE);

    movieSettings.movieSetClearlogoFilenames.clear();
    movieSettings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.MOVIE_CLEARLOGO);

    movieSettings.movieSetDiscartFilenames.clear();
    movieSettings.addMovieSetDiscartFilename(MovieSetDiscartNaming.MOVIE_DISCART);

    movieSettings.trailerFilenames.clear();
    movieSettings.addTrailerFilename(MovieTrailerNaming.FILENAME_TRAILER);

    // other settings
    movieSettings.setMovieConnector(MovieConnectors.KODI);
    movieSettings.setCertificationStyle(CertificationStyle.TECHNICAL);
    movieSettings.setNfoDiscFolderInside(true);
    movieSettings.setTrailerDiscFolderInside(true);

    movieSettings.firePropertyChange("preset", false, true);
  }

  /**
   * Plex defaults
   */
  public static void setDefaultSettingsForPlex() {
    MovieSettings movieSettings = MovieSettings.getInstance();

    // file names
    movieSettings.nfoFilenames.clear();
    movieSettings.addNfoFilename(MovieNfoNaming.FILENAME_NFO);

    movieSettings.posterFilenames.clear();
    movieSettings.addPosterFilename(MoviePosterNaming.POSTER);

    movieSettings.fanartFilenames.clear();
    movieSettings.addFanartFilename(MovieFanartNaming.FANART);

    movieSettings.extraFanartFilenames.clear();
    movieSettings.addExtraFanartFilename(MovieExtraFanartNaming.FILENAME_EXTRAFANART);

    movieSettings.bannerFilenames.clear();
    movieSettings.addBannerFilename(MovieBannerNaming.BANNER);

    movieSettings.clearartFilenames.clear();
    movieSettings.addClearartFilename(MovieClearartNaming.CLEARART);

    movieSettings.thumbFilenames.clear();
    movieSettings.addThumbFilename(MovieThumbNaming.THUMB);

    movieSettings.clearlogoFilenames.clear();
    movieSettings.addClearlogoFilename(MovieClearlogoNaming.CLEARLOGO);

    movieSettings.discartFilenames.clear();
    movieSettings.addDiscartFilename(MovieDiscartNaming.DISC);

    movieSettings.keyartFilenames.clear();
    movieSettings.addKeyartFilename(MovieKeyartNaming.KEYART);

    movieSettings.movieSetNfoFilenames.clear();

    movieSettings.movieSetPosterFilenames.clear();
    movieSettings.addMovieSetPosterFilename(MovieSetPosterNaming.MOVIE_POSTER);

    movieSettings.movieSetFanartFilenames.clear();
    movieSettings.addMovieSetFanartFilename(MovieSetFanartNaming.MOVIE_FANART);

    movieSettings.movieSetBannerFilenames.clear();
    movieSettings.addMovieSetBannerFilename(MovieSetBannerNaming.MOVIE_BANNER);

    movieSettings.movieSetClearartFilenames.clear();
    movieSettings.addMovieSetClearartFilename(MovieSetClearartNaming.MOVIE_CLEARART);

    movieSettings.movieSetThumbFilenames.clear();
    movieSettings.addMovieSetThumbFilename(MovieSetThumbNaming.MOVIE_LANDSCAPE);

    movieSettings.movieSetClearlogoFilenames.clear();
    movieSettings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.MOVIE_CLEARLOGO);

    movieSettings.movieSetDiscartFilenames.clear();
    movieSettings.addMovieSetDiscartFilename(MovieSetDiscartNaming.MOVIE_DISCART);

    movieSettings.trailerFilenames.clear();
    movieSettings.addTrailerFilename(MovieTrailerNaming.FILENAME_TRAILER);

    // other settings
    movieSettings.setMovieConnector(MovieConnectors.KODI);
    movieSettings.setCertificationStyle(CertificationStyle.SHORT);
    movieSettings.setNfoDiscFolderInside(true);
    movieSettings.setTrailerDiscFolderInside(true);

    movieSettings.firePropertyChange("preset", false, true);
  }

  /**
   * Emby defaults
   */
  public static void setDefaultSettingsForEmby() {
    MovieSettings movieSettings = MovieSettings.getInstance();

    // file names
    movieSettings.nfoFilenames.clear();
    movieSettings.addNfoFilename(MovieNfoNaming.FILENAME_NFO);

    movieSettings.posterFilenames.clear();
    movieSettings.addPosterFilename(MoviePosterNaming.POSTER);

    movieSettings.fanartFilenames.clear();
    movieSettings.addFanartFilename(MovieFanartNaming.FANART);

    movieSettings.extraFanartFilenames.clear();
    movieSettings.addExtraFanartFilename(MovieExtraFanartNaming.FILENAME_EXTRAFANART);

    movieSettings.bannerFilenames.clear();
    movieSettings.addBannerFilename(MovieBannerNaming.BANNER);

    movieSettings.clearartFilenames.clear();
    movieSettings.addClearartFilename(MovieClearartNaming.CLEARART);

    movieSettings.thumbFilenames.clear();
    movieSettings.addThumbFilename(MovieThumbNaming.THUMB);

    movieSettings.clearlogoFilenames.clear();
    movieSettings.addClearlogoFilename(MovieClearlogoNaming.CLEARLOGO);

    movieSettings.discartFilenames.clear();
    movieSettings.addDiscartFilename(MovieDiscartNaming.DISC);

    movieSettings.keyartFilenames.clear();
    movieSettings.addKeyartFilename(MovieKeyartNaming.KEYART);

    movieSettings.movieSetNfoFilenames.clear();

    movieSettings.movieSetPosterFilenames.clear();
    movieSettings.addMovieSetPosterFilename(MovieSetPosterNaming.MOVIE_POSTER);

    movieSettings.movieSetFanartFilenames.clear();
    movieSettings.addMovieSetFanartFilename(MovieSetFanartNaming.MOVIE_FANART);

    movieSettings.movieSetBannerFilenames.clear();
    movieSettings.addMovieSetBannerFilename(MovieSetBannerNaming.MOVIE_BANNER);

    movieSettings.movieSetClearartFilenames.clear();
    movieSettings.addMovieSetClearartFilename(MovieSetClearartNaming.MOVIE_CLEARART);

    movieSettings.movieSetThumbFilenames.clear();
    movieSettings.addMovieSetThumbFilename(MovieSetThumbNaming.MOVIE_LANDSCAPE);

    movieSettings.movieSetClearlogoFilenames.clear();
    movieSettings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.MOVIE_CLEARLOGO);

    movieSettings.movieSetDiscartFilenames.clear();
    movieSettings.addMovieSetDiscartFilename(MovieSetDiscartNaming.MOVIE_DISCART);

    movieSettings.trailerFilenames.clear();
    movieSettings.addTrailerFilename(MovieTrailerNaming.FILENAME_TRAILER);

    // other settings
    movieSettings.setMovieConnector(MovieConnectors.EMBY);
    movieSettings.setCertificationStyle(CertificationStyle.SHORT);
    movieSettings.setNfoDiscFolderInside(true);
    movieSettings.setTrailerDiscFolderInside(true);

    movieSettings.firePropertyChange("preset", false, true);
  }

  /**
   * Jellyfin defaults
   */
  public static void setDefaultSettingsForJellyfin() {
    MovieSettings movieSettings = MovieSettings.getInstance();

    // file names
    movieSettings.nfoFilenames.clear();
    movieSettings.addNfoFilename(MovieNfoNaming.FILENAME_NFO);

    movieSettings.posterFilenames.clear();
    movieSettings.addPosterFilename(MoviePosterNaming.POSTER);

    movieSettings.fanartFilenames.clear();
    movieSettings.addFanartFilename(MovieFanartNaming.BACKDROP);

    movieSettings.extraFanartFilenames.clear();
    movieSettings.addExtraFanartFilename(MovieExtraFanartNaming.EXTRABACKDROP);

    movieSettings.bannerFilenames.clear();
    movieSettings.addBannerFilename(MovieBannerNaming.BANNER);

    movieSettings.clearartFilenames.clear();
    movieSettings.addClearartFilename(MovieClearartNaming.CLEARART);

    movieSettings.thumbFilenames.clear();
    movieSettings.addThumbFilename(MovieThumbNaming.THUMB);

    movieSettings.clearlogoFilenames.clear();
    movieSettings.addClearlogoFilename(MovieClearlogoNaming.CLEARLOGO);

    movieSettings.discartFilenames.clear();
    movieSettings.addDiscartFilename(MovieDiscartNaming.DISC);

    movieSettings.keyartFilenames.clear();
    movieSettings.addKeyartFilename(MovieKeyartNaming.KEYART);

    movieSettings.movieSetNfoFilenames.clear();

    movieSettings.movieSetPosterFilenames.clear();
    movieSettings.addMovieSetPosterFilename(MovieSetPosterNaming.MOVIE_POSTER);

    movieSettings.movieSetFanartFilenames.clear();
    movieSettings.addMovieSetFanartFilename(MovieSetFanartNaming.MOVIE_FANART);

    movieSettings.movieSetBannerFilenames.clear();
    movieSettings.addMovieSetBannerFilename(MovieSetBannerNaming.MOVIE_BANNER);

    movieSettings.movieSetClearartFilenames.clear();
    movieSettings.addMovieSetClearartFilename(MovieSetClearartNaming.MOVIE_CLEARART);

    movieSettings.movieSetThumbFilenames.clear();
    movieSettings.addMovieSetThumbFilename(MovieSetThumbNaming.MOVIE_LANDSCAPE);

    movieSettings.movieSetClearlogoFilenames.clear();
    movieSettings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.MOVIE_CLEARLOGO);

    movieSettings.movieSetDiscartFilenames.clear();
    movieSettings.addMovieSetDiscartFilename(MovieSetDiscartNaming.MOVIE_DISCART);

    movieSettings.trailerFilenames.clear();
    movieSettings.addTrailerFilename(MovieTrailerNaming.FILENAME_TRAILER);

    // other settings
    movieSettings.setMovieConnector(MovieConnectors.JELLYFIN);
    movieSettings.setCertificationStyle(CertificationStyle.SHORT);
    movieSettings.setNfoDiscFolderInside(true);
    movieSettings.setTrailerDiscFolderInside(true);

    movieSettings.firePropertyChange("preset", false, true);
  }

  /**
   * MediaPIG defaults
   */
  public static void setDefaultSettingsForMediaPig() {
    MovieSettings movieSettings = MovieSettings.getInstance();

    // file names
    movieSettings.nfoFilenames.clear();
    movieSettings.addNfoFilename(MovieNfoNaming.FILENAME_NFO);
    movieSettings.addNfoFilename(MovieNfoNaming.MOVIE_NFO);

    movieSettings.posterFilenames.clear();
    movieSettings.addPosterFilename(MoviePosterNaming.POSTER);

    movieSettings.fanartFilenames.clear();
    movieSettings.addFanartFilename(MovieFanartNaming.FANART);

    movieSettings.extraFanartFilenames.clear();
    movieSettings.addExtraFanartFilename(MovieExtraFanartNaming.FOLDER_EXTRAFANART);

    movieSettings.bannerFilenames.clear();
    movieSettings.addBannerFilename(MovieBannerNaming.BANNER);

    movieSettings.clearartFilenames.clear();
    movieSettings.addClearartFilename(MovieClearartNaming.CLEARART);

    movieSettings.thumbFilenames.clear();
    movieSettings.addThumbFilename(MovieThumbNaming.THUMB);

    movieSettings.clearlogoFilenames.clear();
    movieSettings.addClearlogoFilename(MovieClearlogoNaming.CLEARLOGO);

    movieSettings.discartFilenames.clear();
    movieSettings.addDiscartFilename(MovieDiscartNaming.DISC);

    movieSettings.keyartFilenames.clear();
    movieSettings.addKeyartFilename(MovieKeyartNaming.KEYART);

    movieSettings.movieSetNfoFilenames.clear();
    movieSettings.addMovieSetNfoFilename(MovieSetNfoNaming.KODI_NFO);

    movieSettings.movieSetPosterFilenames.clear();
    movieSettings.addMovieSetPosterFilename(MovieSetPosterNaming.KODI_POSTER);

    movieSettings.movieSetFanartFilenames.clear();
    movieSettings.addMovieSetFanartFilename(MovieSetFanartNaming.KODI_FANART);

    movieSettings.movieSetBannerFilenames.clear();
    movieSettings.addMovieSetBannerFilename(MovieSetBannerNaming.KODI_BANNER);

    movieSettings.movieSetClearartFilenames.clear();
    movieSettings.addMovieSetClearartFilename(MovieSetClearartNaming.KODI_CLEARART);

    movieSettings.movieSetThumbFilenames.clear();
    movieSettings.addMovieSetThumbFilename(MovieSetThumbNaming.KODI_LANDSCAPE);

    movieSettings.movieSetClearlogoFilenames.clear();
    movieSettings.addMovieSetClearlogoFilename(MovieSetClearlogoNaming.KODI_CLEARLOGO);

    movieSettings.movieSetDiscartFilenames.clear();
    movieSettings.addMovieSetDiscartFilename(MovieSetDiscartNaming.KODI_DISCART);

    movieSettings.trailerFilenames.clear();
    movieSettings.addTrailerFilename(MovieTrailerNaming.FILENAME_TRAILER);

    // other settings
    movieSettings.setMovieConnector(MovieConnectors.KODI);
    movieSettings.setCertificationStyle(CertificationStyle.LARGE);
    movieSettings.setNfoDiscFolderInside(true);
    movieSettings.setTrailerDiscFolderInside(true);

    movieSettings.firePropertyChange("preset", false, true);
  }
}
