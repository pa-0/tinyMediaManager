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
package org.tinymediamanager.core.tvshow;

import org.tinymediamanager.core.CertificationStyle;
import org.tinymediamanager.core.tvshow.connector.TvShowConnectors;
import org.tinymediamanager.core.tvshow.filenaming.TvShowBannerNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowCharacterartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowClearartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowClearlogoNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowDiscartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowEpisodeNfoNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowEpisodeThumbNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowExtraFanartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowFanartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowKeyartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowNfoNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowPosterNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonBannerNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonFanartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonNfoNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonPosterNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonThumbNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowThumbNaming;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;

/**
 * the class {@link TvShowSettingsDefaults} is a helper class for default settings of the TV show module
 *
 * @author Manuel Laggner
 */
public class TvShowSettingsDefaults {

  private TvShowSettingsDefaults() {
    throw new IllegalAccessError();
  }

  /**
   * XBMC/Kodi <17 defaults
   */
  public static void setDefaultSettingsForXbmc() {
    TvShowSettings tvShowSettings = TvShowSettings.getInstance();

    tvShowSettings.nfoFilenames.clear();
    tvShowSettings.nfoFilenames.add(TvShowNfoNaming.TV_SHOW);

    tvShowSettings.posterFilenames.clear();
    tvShowSettings.posterFilenames.add(TvShowPosterNaming.POSTER);

    tvShowSettings.fanartFilenames.clear();
    tvShowSettings.fanartFilenames.add(TvShowFanartNaming.FANART);

    tvShowSettings.extraFanartFilenames.clear();
    tvShowSettings.extraFanartFilenames.add(TvShowExtraFanartNaming.FOLDER_EXTRAFANART);

    tvShowSettings.bannerFilenames.clear();
    tvShowSettings.bannerFilenames.add(TvShowBannerNaming.BANNER);

    tvShowSettings.discartFilenames.clear();
    tvShowSettings.discartFilenames.add(TvShowDiscartNaming.DISCART);

    tvShowSettings.clearartFilenames.clear();
    tvShowSettings.clearartFilenames.add(TvShowClearartNaming.CLEARART);

    tvShowSettings.clearlogoFilenames.clear();
    tvShowSettings.clearlogoFilenames.add(TvShowClearlogoNaming.CLEARLOGO);

    tvShowSettings.characterartFilenames.clear();
    tvShowSettings.characterartFilenames.add(TvShowCharacterartNaming.CHARACTERART);

    tvShowSettings.thumbFilenames.clear();
    tvShowSettings.thumbFilenames.add(TvShowThumbNaming.THUMB);

    tvShowSettings.keyartFilenames.clear();
    tvShowSettings.keyartFilenames.add(TvShowKeyartNaming.KEYART);

    tvShowSettings.seasonPosterFilenames.clear();
    tvShowSettings.seasonPosterFilenames.add(TvShowSeasonPosterNaming.SEASON_POSTER);

    tvShowSettings.seasonFanartFilenames.clear();
    tvShowSettings.seasonFanartFilenames.add(TvShowSeasonFanartNaming.SEASON_FANART);

    tvShowSettings.seasonBannerFilenames.clear();
    tvShowSettings.seasonBannerFilenames.add(TvShowSeasonBannerNaming.SEASON_BANNER);

    tvShowSettings.seasonThumbFilenames.clear();
    tvShowSettings.seasonThumbFilenames.add(TvShowSeasonThumbNaming.SEASON_THUMB);

    tvShowSettings.episodeNfoFilenames.clear();
    tvShowSettings.episodeNfoFilenames.add(TvShowEpisodeNfoNaming.FILENAME);

    tvShowSettings.episodeThumbFilenames.clear();
    tvShowSettings.episodeThumbFilenames.add(TvShowEpisodeThumbNaming.FILENAME_THUMB);

    // other settings
    tvShowSettings.setTvShowConnector(TvShowConnectors.XBMC);
    tvShowSettings.setCertificationStyle(CertificationStyle.LARGE);

    tvShowSettings.firePropertyChange("preset", false, true);
  }

  /**
   * Kodi 17+ defaults
   */
  public static void setDefaultSettingsForKodi() {
    TvShowSettings tvShowSettings = TvShowSettings.getInstance();

    tvShowSettings.nfoFilenames.clear();
    tvShowSettings.nfoFilenames.add(TvShowNfoNaming.TV_SHOW);

    tvShowSettings.posterFilenames.clear();
    tvShowSettings.posterFilenames.add(TvShowPosterNaming.POSTER);

    tvShowSettings.fanartFilenames.clear();
    tvShowSettings.fanartFilenames.add(TvShowFanartNaming.FANART);

    tvShowSettings.extraFanartFilenames.clear();
    tvShowSettings.extraFanartFilenames.add(TvShowExtraFanartNaming.EXTRAFANART);

    tvShowSettings.bannerFilenames.clear();
    tvShowSettings.bannerFilenames.add(TvShowBannerNaming.BANNER);

    tvShowSettings.discartFilenames.clear();
    tvShowSettings.discartFilenames.add(TvShowDiscartNaming.DISCART);

    tvShowSettings.clearartFilenames.clear();
    tvShowSettings.clearartFilenames.add(TvShowClearartNaming.CLEARART);

    tvShowSettings.clearlogoFilenames.clear();
    tvShowSettings.clearlogoFilenames.add(TvShowClearlogoNaming.CLEARLOGO);

    tvShowSettings.characterartFilenames.clear();
    tvShowSettings.characterartFilenames.add(TvShowCharacterartNaming.CHARACTERART);

    tvShowSettings.thumbFilenames.clear();
    tvShowSettings.thumbFilenames.add(TvShowThumbNaming.LANDSCAPE);

    tvShowSettings.keyartFilenames.clear();
    tvShowSettings.keyartFilenames.add(TvShowKeyartNaming.KEYART);

    tvShowSettings.seasonPosterFilenames.clear();
    tvShowSettings.seasonPosterFilenames.add(TvShowSeasonPosterNaming.SEASON_POSTER);

    tvShowSettings.seasonFanartFilenames.clear();
    tvShowSettings.seasonFanartFilenames.add(TvShowSeasonFanartNaming.SEASON_FANART);

    tvShowSettings.seasonBannerFilenames.clear();
    tvShowSettings.seasonBannerFilenames.add(TvShowSeasonBannerNaming.SEASON_BANNER);

    tvShowSettings.seasonThumbFilenames.clear();
    tvShowSettings.seasonThumbFilenames.add(TvShowSeasonThumbNaming.SEASON_THUMB);

    tvShowSettings.episodeNfoFilenames.clear();
    tvShowSettings.episodeNfoFilenames.add(TvShowEpisodeNfoNaming.FILENAME);

    tvShowSettings.episodeThumbFilenames.clear();
    tvShowSettings.episodeThumbFilenames.add(TvShowEpisodeThumbNaming.FILENAME_THUMB);

    // other settings
    tvShowSettings.setTvShowConnector(TvShowConnectors.KODI);
    tvShowSettings.setCertificationStyle(CertificationStyle.LARGE);
    tvShowSettings.setNfoWriteEpisodeguide(false);
    tvShowSettings.setNfoWriteNewEpisodeguideStyle(true);

    tvShowSettings.firePropertyChange("preset", false, true);
  }

  /**
   * MediaPortal defaults
   */
  public static void setDefaultSettingsForMediaPortal() {
    TvShowSettings tvShowSettings = TvShowSettings.getInstance();

    tvShowSettings.nfoFilenames.clear();
    tvShowSettings.nfoFilenames.add(TvShowNfoNaming.TV_SHOW);

    tvShowSettings.posterFilenames.clear();
    tvShowSettings.posterFilenames.add(TvShowPosterNaming.POSTER);

    tvShowSettings.fanartFilenames.clear();
    tvShowSettings.fanartFilenames.add(TvShowFanartNaming.FANART);

    tvShowSettings.extraFanartFilenames.clear();
    tvShowSettings.extraFanartFilenames.add(TvShowExtraFanartNaming.FOLDER_EXTRAFANART);

    tvShowSettings.bannerFilenames.clear();
    tvShowSettings.bannerFilenames.add(TvShowBannerNaming.BANNER);

    tvShowSettings.discartFilenames.clear();
    tvShowSettings.discartFilenames.add(TvShowDiscartNaming.DISCART);

    tvShowSettings.clearartFilenames.clear();
    tvShowSettings.clearartFilenames.add(TvShowClearartNaming.CLEARART);

    tvShowSettings.clearlogoFilenames.clear();
    tvShowSettings.clearlogoFilenames.add(TvShowClearlogoNaming.CLEARLOGO);

    tvShowSettings.characterartFilenames.clear();
    tvShowSettings.characterartFilenames.add(TvShowCharacterartNaming.CHARACTERART);

    tvShowSettings.thumbFilenames.clear();
    tvShowSettings.thumbFilenames.add(TvShowThumbNaming.THUMB);

    tvShowSettings.keyartFilenames.clear();
    tvShowSettings.keyartFilenames.add(TvShowKeyartNaming.KEYART);

    tvShowSettings.seasonPosterFilenames.clear();
    tvShowSettings.seasonPosterFilenames.add(TvShowSeasonPosterNaming.SEASON_POSTER);

    tvShowSettings.seasonFanartFilenames.clear();
    tvShowSettings.seasonFanartFilenames.add(TvShowSeasonFanartNaming.SEASON_FANART);

    tvShowSettings.seasonBannerFilenames.clear();
    tvShowSettings.seasonBannerFilenames.add(TvShowSeasonBannerNaming.SEASON_BANNER);

    tvShowSettings.seasonThumbFilenames.clear();
    tvShowSettings.seasonThumbFilenames.add(TvShowSeasonThumbNaming.SEASON_THUMB);

    tvShowSettings.episodeNfoFilenames.clear();
    tvShowSettings.episodeNfoFilenames.add(TvShowEpisodeNfoNaming.FILENAME);

    tvShowSettings.episodeThumbFilenames.clear();
    tvShowSettings.episodeThumbFilenames.add(TvShowEpisodeThumbNaming.FILENAME);

    // other settings
    tvShowSettings.setTvShowConnector(TvShowConnectors.XBMC);
    tvShowSettings.setCertificationStyle(CertificationStyle.TECHNICAL);

    tvShowSettings.firePropertyChange("preset", false, true);
  }

  /**
   * Plex defaults
   */
  public static void setDefaultSettingsForPlex() {
    TvShowSettings tvShowSettings = TvShowSettings.getInstance();

    tvShowSettings.nfoFilenames.clear();
    tvShowSettings.nfoFilenames.add(TvShowNfoNaming.TV_SHOW);

    tvShowSettings.posterFilenames.clear();
    tvShowSettings.posterFilenames.add(TvShowPosterNaming.POSTER);

    tvShowSettings.fanartFilenames.clear();
    tvShowSettings.fanartFilenames.add(TvShowFanartNaming.FANART);

    tvShowSettings.extraFanartFilenames.clear();
    tvShowSettings.extraFanartFilenames.add(TvShowExtraFanartNaming.EXTRAFANART);

    tvShowSettings.bannerFilenames.clear();
    tvShowSettings.bannerFilenames.add(TvShowBannerNaming.BANNER);

    tvShowSettings.discartFilenames.clear();
    tvShowSettings.discartFilenames.add(TvShowDiscartNaming.DISCART);

    tvShowSettings.clearartFilenames.clear();
    tvShowSettings.clearartFilenames.add(TvShowClearartNaming.CLEARART);

    tvShowSettings.clearlogoFilenames.clear();
    tvShowSettings.clearlogoFilenames.add(TvShowClearlogoNaming.CLEARLOGO);

    tvShowSettings.characterartFilenames.clear();
    tvShowSettings.characterartFilenames.add(TvShowCharacterartNaming.CHARACTERART);

    tvShowSettings.thumbFilenames.clear();
    tvShowSettings.thumbFilenames.add(TvShowThumbNaming.THUMB);

    tvShowSettings.keyartFilenames.clear();
    tvShowSettings.keyartFilenames.add(TvShowKeyartNaming.KEYART);

    tvShowSettings.seasonPosterFilenames.clear();
    tvShowSettings.seasonPosterFilenames.add(TvShowSeasonPosterNaming.SEASON_FOLDER);

    tvShowSettings.seasonFanartFilenames.clear();
    tvShowSettings.seasonFanartFilenames.add(TvShowSeasonFanartNaming.SEASON_FANART);

    tvShowSettings.seasonBannerFilenames.clear();
    tvShowSettings.seasonBannerFilenames.add(TvShowSeasonBannerNaming.SEASON_FOLDER);

    tvShowSettings.seasonThumbFilenames.clear();
    tvShowSettings.seasonThumbFilenames.add(TvShowSeasonThumbNaming.SEASON_FOLDER);

    tvShowSettings.episodeNfoFilenames.clear();
    tvShowSettings.episodeNfoFilenames.add(TvShowEpisodeNfoNaming.FILENAME);

    tvShowSettings.episodeThumbFilenames.clear();
    tvShowSettings.episodeThumbFilenames.add(TvShowEpisodeThumbNaming.FILENAME);

    // other settings
    tvShowSettings.setTvShowConnector(TvShowConnectors.XBMC);
    tvShowSettings.setCertificationStyle(CertificationStyle.SHORT);

    tvShowSettings.firePropertyChange("preset", false, true);
  }

  /**
   * Plex defaults
   */
  public static void setDefaultSettingsForJellyfin() {
    TvShowSettings tvShowSettings = TvShowSettings.getInstance();

    tvShowSettings.nfoFilenames.clear();
    tvShowSettings.nfoFilenames.add(TvShowNfoNaming.TV_SHOW);

    tvShowSettings.posterFilenames.clear();
    tvShowSettings.posterFilenames.add(TvShowPosterNaming.POSTER);

    tvShowSettings.fanartFilenames.clear();
    tvShowSettings.fanartFilenames.add(TvShowFanartNaming.BACKDROP);

    tvShowSettings.extraFanartFilenames.clear();
    tvShowSettings.extraFanartFilenames.add(TvShowExtraFanartNaming.EXTRABACKDROP);

    tvShowSettings.bannerFilenames.clear();
    tvShowSettings.bannerFilenames.add(TvShowBannerNaming.BANNER);

    tvShowSettings.discartFilenames.clear();
    tvShowSettings.discartFilenames.add(TvShowDiscartNaming.DISCART);

    tvShowSettings.clearartFilenames.clear();
    tvShowSettings.clearartFilenames.add(TvShowClearartNaming.CLEARART);

    tvShowSettings.clearlogoFilenames.clear();
    tvShowSettings.clearlogoFilenames.add(TvShowClearlogoNaming.CLEARLOGO);

    tvShowSettings.characterartFilenames.clear();
    tvShowSettings.characterartFilenames.add(TvShowCharacterartNaming.CHARACTERART);

    tvShowSettings.thumbFilenames.clear();
    tvShowSettings.thumbFilenames.add(TvShowThumbNaming.THUMB);

    tvShowSettings.keyartFilenames.clear();
    tvShowSettings.keyartFilenames.add(TvShowKeyartNaming.KEYART);

    tvShowSettings.seasonPosterFilenames.clear();
    tvShowSettings.seasonPosterFilenames.add(TvShowSeasonPosterNaming.SEASON_FOLDER);

    tvShowSettings.seasonFanartFilenames.clear();
    tvShowSettings.seasonFanartFilenames.add(TvShowSeasonFanartNaming.SEASON_FANART);

    tvShowSettings.seasonBannerFilenames.clear();
    tvShowSettings.seasonBannerFilenames.add(TvShowSeasonBannerNaming.SEASON_FOLDER);

    tvShowSettings.seasonThumbFilenames.clear();
    tvShowSettings.seasonThumbFilenames.add(TvShowSeasonThumbNaming.SEASON_FOLDER);

    tvShowSettings.episodeNfoFilenames.clear();
    tvShowSettings.episodeNfoFilenames.add(TvShowEpisodeNfoNaming.FILENAME);

    tvShowSettings.episodeThumbFilenames.clear();
    tvShowSettings.episodeThumbFilenames.add(TvShowEpisodeThumbNaming.FILENAME);

    // other settings
    tvShowSettings.setTvShowConnector(TvShowConnectors.JELLYFIN);
    tvShowSettings.setCertificationStyle(CertificationStyle.SHORT);

    tvShowSettings.firePropertyChange("preset", false, true);
  }

  /**
   * Emby defaults
   */
  public static void setDefaultSettingsForEmby() {
    TvShowSettings tvShowSettings = TvShowSettings.getInstance();

    tvShowSettings.nfoFilenames.clear();
    tvShowSettings.nfoFilenames.add(TvShowNfoNaming.TV_SHOW);

    tvShowSettings.posterFilenames.clear();
    tvShowSettings.posterFilenames.add(TvShowPosterNaming.POSTER);

    tvShowSettings.fanartFilenames.clear();
    tvShowSettings.fanartFilenames.add(TvShowFanartNaming.FANART);

    tvShowSettings.extraFanartFilenames.clear();
    tvShowSettings.extraFanartFilenames.add(TvShowExtraFanartNaming.EXTRAFANART);

    tvShowSettings.bannerFilenames.clear();
    tvShowSettings.bannerFilenames.add(TvShowBannerNaming.BANNER);

    tvShowSettings.discartFilenames.clear();
    tvShowSettings.discartFilenames.add(TvShowDiscartNaming.DISCART);

    tvShowSettings.clearartFilenames.clear();
    tvShowSettings.clearartFilenames.add(TvShowClearartNaming.CLEARART);

    tvShowSettings.clearlogoFilenames.clear();
    tvShowSettings.clearlogoFilenames.add(TvShowClearlogoNaming.CLEARLOGO);

    tvShowSettings.characterartFilenames.clear();
    tvShowSettings.characterartFilenames.add(TvShowCharacterartNaming.CHARACTERART);

    tvShowSettings.thumbFilenames.clear();
    tvShowSettings.thumbFilenames.add(TvShowThumbNaming.THUMB);

    tvShowSettings.keyartFilenames.clear();
    tvShowSettings.keyartFilenames.add(TvShowKeyartNaming.KEYART);

    tvShowSettings.seasonNfoFilenames.clear();
    tvShowSettings.seasonNfoFilenames.add(TvShowSeasonNfoNaming.SEASON_FOLDER);

    tvShowSettings.seasonPosterFilenames.clear();
    tvShowSettings.seasonPosterFilenames.add(TvShowSeasonPosterNaming.SEASON_FOLDER);

    tvShowSettings.seasonFanartFilenames.clear();
    tvShowSettings.seasonFanartFilenames.add(TvShowSeasonFanartNaming.SEASON_FANART);

    tvShowSettings.seasonBannerFilenames.clear();
    tvShowSettings.seasonBannerFilenames.add(TvShowSeasonBannerNaming.SEASON_FOLDER);

    tvShowSettings.seasonThumbFilenames.clear();
    tvShowSettings.seasonThumbFilenames.add(TvShowSeasonThumbNaming.SEASON_FOLDER);

    tvShowSettings.episodeNfoFilenames.clear();
    tvShowSettings.episodeNfoFilenames.add(TvShowEpisodeNfoNaming.FILENAME);

    tvShowSettings.episodeThumbFilenames.clear();
    tvShowSettings.episodeThumbFilenames.add(TvShowEpisodeThumbNaming.FILENAME);

    // other settings
    tvShowSettings.setTvShowConnector(TvShowConnectors.EMBY);
    tvShowSettings.setCertificationStyle(CertificationStyle.SHORT);

    tvShowSettings.firePropertyChange("preset", false, true);
  }

  /**
   * MediaPIG defaults
   */
  public static void setDefaultSettingsForMediaPig() {
    TvShowSettings tvShowSettings = TvShowSettings.getInstance();

    tvShowSettings.nfoFilenames.clear();
    tvShowSettings.nfoFilenames.add(TvShowNfoNaming.TV_SHOW);

    tvShowSettings.posterFilenames.clear();
    tvShowSettings.posterFilenames.add(TvShowPosterNaming.POSTER);

    tvShowSettings.fanartFilenames.clear();
    tvShowSettings.fanartFilenames.add(TvShowFanartNaming.FANART);

    tvShowSettings.extraFanartFilenames.clear();
    tvShowSettings.extraFanartFilenames.add(TvShowExtraFanartNaming.FOLDER_EXTRAFANART);

    tvShowSettings.bannerFilenames.clear();
    tvShowSettings.bannerFilenames.add(TvShowBannerNaming.BANNER);

    tvShowSettings.discartFilenames.clear();
    tvShowSettings.discartFilenames.add(TvShowDiscartNaming.DISCART);

    tvShowSettings.clearartFilenames.clear();
    tvShowSettings.clearartFilenames.add(TvShowClearartNaming.CLEARART);

    tvShowSettings.clearlogoFilenames.clear();
    tvShowSettings.clearlogoFilenames.add(TvShowClearlogoNaming.CLEARLOGO);

    tvShowSettings.characterartFilenames.clear();
    tvShowSettings.characterartFilenames.add(TvShowCharacterartNaming.CHARACTERART);

    tvShowSettings.thumbFilenames.clear();
    tvShowSettings.thumbFilenames.add(TvShowThumbNaming.THUMB);

    tvShowSettings.keyartFilenames.clear();
    tvShowSettings.keyartFilenames.add(TvShowKeyartNaming.KEYART);

    tvShowSettings.seasonPosterFilenames.clear();
    tvShowSettings.seasonPosterFilenames.add(TvShowSeasonPosterNaming.SEASON_POSTER);

    tvShowSettings.seasonFanartFilenames.clear();
    tvShowSettings.seasonFanartFilenames.add(TvShowSeasonFanartNaming.SEASON_FANART);

    tvShowSettings.seasonBannerFilenames.clear();
    tvShowSettings.seasonBannerFilenames.add(TvShowSeasonBannerNaming.SEASON_BANNER);

    tvShowSettings.seasonThumbFilenames.clear();
    tvShowSettings.seasonThumbFilenames.add(TvShowSeasonThumbNaming.SEASON_THUMB);

    tvShowSettings.episodeNfoFilenames.clear();
    tvShowSettings.episodeNfoFilenames.add(TvShowEpisodeNfoNaming.FILENAME);

    tvShowSettings.episodeThumbFilenames.clear();
    tvShowSettings.episodeThumbFilenames.add(TvShowEpisodeThumbNaming.FILENAME_THUMB);

    // other settings
    tvShowSettings.setTvShowConnector(TvShowConnectors.KODI);
    tvShowSettings.setCertificationStyle(CertificationStyle.LARGE);
    tvShowSettings.setNfoWriteEpisodeguide(true);
    tvShowSettings.setNfoWriteNewEpisodeguideStyle(true);

    tvShowSettings.firePropertyChange("preset", false, true);
  }

  /**
   * set the default scrapers for the movie module
   */
  public static void setDefaultScrapers() {
    TvShowSettings tvShowSettings = TvShowSettings.getInstance();

    // activate default scrapers (hand curated list of defaults)
    tvShowSettings.artworkScrapers.clear();
    for (MediaScraper ms : MediaScraper.getMediaScrapers(ScraperType.TVSHOW_ARTWORK)) {
      switch (ms.getId()) {
        case "tmdb":
        case "fanarttv":
        case "tvdb":
          tvShowSettings.addTvShowArtworkScraper(ms.getId());
          break;

        default:
          break;
      }
    }

    tvShowSettings.trailerScrapers.clear();
    for (MediaScraper ms : MediaScraper.getMediaScrapers(ScraperType.TVSHOW_TRAILER)) {
      tvShowSettings.addTvShowTrailerScraper(ms.getId());
    }

    tvShowSettings.subtitleScrapers.clear();
    for (MediaScraper ms : MediaScraper.getMediaScrapers(ScraperType.TVSHOW_SUBTITLE)) {
      tvShowSettings.addTvShowSubtitleScraper(ms.getId());
    }
  }
}
