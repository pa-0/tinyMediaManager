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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.observablecollections.ObservableCollections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.CertificationStyle;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.DateField;
import org.tinymediamanager.core.LanguageStyle;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.PostProcess;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TrailerQuality;
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
import org.tinymediamanager.core.tvshow.filenaming.TvShowTrailerNaming;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaLanguages;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * The Class TvShowSettings.
 *
 * @author Manuel Laggner
 */
public final class TvShowSettings extends AbstractSettings {
  private static final Logger                    LOGGER                                 = LoggerFactory.getLogger(TvShowSettings.class);
  private static final String                    CONFIG_FILE                            = "tvShows.json";

  public static final String                     DEFAULT_RENAMER_FOLDER_PATTERN         = "${showTitle} (${showYear})";
  public static final String                     DEFAULT_RENAMER_SEASON_PATTERN         = "Season ${seasonNr}";
  public static final String                     DEFAULT_RENAMER_FILE_PATTERN           = "${showTitle} - S${seasonNr2}E${episodeNr2} - ${title}";

  private static TvShowSettings                  instance;

  /**
   * Constants mainly for events
   */
  static final String                            TV_SHOW_DATA_SOURCE                    = "tvShowDataSource";
  static final String                            ARTWORK_SCRAPERS                       = "artworkScrapers";
  static final String                            TRAILER_SCRAPERS                       = "trailerScrapers";
  static final String                            TRAILER_FILENAME                       = "trailerFilename";

  static final String                            CERTIFICATION_COUNTRY                  = "certificationCountry";
  static final String                            RENAMER_SEASON_FOLDER                  = "renamerSeasonFoldername";
  static final String                            BAD_WORD                               = "badWord";
  static final String                            SKIP_FOLDER                            = "skipFolder";
  static final String                            SUBTITLE_SCRAPERS                      = "subtitleScrapers";
  static final String                            NFO_FILENAME                           = "nfoFilename";
  static final String                            POSTER_FILENAME                        = "posterFilename";
  static final String                            FANART_FILENAME                        = "fanartFilename";
  static final String                            EXTRAFANART_FILENAME                   = "extraFanartFilename";
  static final String                            BANNER_FILENAME                        = "bannerFilename";
  static final String                            DISCART_FILENAME                       = "discartFilename";
  static final String                            CLEARART_FILENAME                      = "clearartFilename";
  static final String                            THUMB_FILENAME                         = "thumbFilename";
  static final String                            CLEARLOGO_FILENAME                     = "clearlogoFilename";
  static final String                            CHARACTERART_FILENAME                  = "characterartFilename";
  static final String                            KEYART_FILENAME                        = "keyartFilename";
  static final String                            SEASON_NFO_FILENAME                    = "seasonNfoFilename";
  static final String                            SEASON_POSTER_FILENAME                 = "seasonPosterFilename";
  static final String                            SEASON_FANART_FILENAME                 = "seasonFanartFilename";
  static final String                            SEASON_BANNER_FILENAME                 = "seasonBannerFilename";
  static final String                            SEASON_THUMB_FILENAME                  = "seasonThumbFilename";
  static final String                            EPISODE_NFO_FILENAME                   = "episodeNfoFilename";
  static final String                            EPISODE_THUMB_FILENAME                 = "episodeThumbFilename";
  static final String                            TVSHOW_CHECK_METADATA                  = "tvShowCheckMetadata";
  static final String                            TVSHOW_CHECK_ARTWORK                   = "tvShowCheckArtwork";
  static final String                            SEASON_CHECK_ARTWORK                   = "seasonCheckArtwork";
  static final String                            EPISODE_CHECK_METADATA                 = "episodeCheckMetadata";
  static final String                            EPISODE_CHECK_ARTWORK                  = "episodeCheckArtwork";

  static final String                            NODE                                   = "node";
  static final String                            TITLE                                  = "title";
  static final String                            ORIGINAL_TITLE                         = "originalTitle";
  static final String                            NOTE                                   = "note";

  final List<String>                             tvShowDataSources                      = ObservableCollections.observableList(new ArrayList<>());
  final List<String>                             badWords                               = ObservableCollections.observableList(new ArrayList<>());
  final List<String>                             artworkScrapers                        = ObservableCollections.observableList(new ArrayList<>());
  final List<String>                             trailerScrapers                        = ObservableCollections.observableList(new ArrayList<>());
  final List<String>                             skipFolders                            = ObservableCollections.observableList(new ArrayList<>());
  final List<String>                             subtitleScrapers                       = ObservableCollections.observableList(new ArrayList<>());
  final List<PostProcess>                        postProcessTvShow                      = ObservableCollections.observableList(new ArrayList<>());
  final List<PostProcess>                        postProcessEpisode                     = ObservableCollections.observableList(new ArrayList<>());
  final List<TvShowNfoNaming>                    nfoFilenames                           = new ArrayList<>();
  final List<TvShowPosterNaming>                 posterFilenames                        = new ArrayList<>();
  final List<TvShowFanartNaming>                 fanartFilenames                        = new ArrayList<>();
  final List<TvShowExtraFanartNaming>            extraFanartFilenames                   = new ArrayList<>();
  final List<TvShowBannerNaming>                 bannerFilenames                        = new ArrayList<>();
  final List<TvShowDiscartNaming>                discartFilenames                       = new ArrayList<>();
  final List<TvShowClearartNaming>               clearartFilenames                      = new ArrayList<>();
  final List<TvShowThumbNaming>                  thumbFilenames                         = new ArrayList<>();
  final List<TvShowClearlogoNaming>              clearlogoFilenames                     = new ArrayList<>();
  final List<TvShowCharacterartNaming>           characterartFilenames                  = new ArrayList<>();
  final List<TvShowKeyartNaming>                 keyartFilenames                        = new ArrayList<>();
  final List<TvShowSeasonNfoNaming>              seasonNfoFilenames                     = new ArrayList<>();
  final List<TvShowSeasonPosterNaming>           seasonPosterFilenames                  = new ArrayList<>();
  final List<TvShowSeasonFanartNaming>           seasonFanartFilenames                  = new ArrayList<>();
  final List<TvShowSeasonBannerNaming>           seasonBannerFilenames                  = new ArrayList<>();
  final List<TvShowSeasonThumbNaming>            seasonThumbFilenames                   = new ArrayList<>();
  final List<TvShowEpisodeNfoNaming>             episodeNfoFilenames                    = new ArrayList<>();
  final List<TvShowEpisodeThumbNaming>           episodeThumbFilenames                  = new ArrayList<>();
  final List<TvShowTrailerNaming>                trailerFilenames                       = new ArrayList<>();

  final Map<String, List<UIFilters>>             uiFilterPresets                        = new HashMap<>();

  // data sources / NFO settings
  TvShowConnectors                               tvShowConnector                        = TvShowConnectors.KODI;
  CertificationStyle                             certificationStyle                     = CertificationStyle.LARGE;
  boolean                                        writeCleanNfo                          = false;
  DateField                                      nfoDateAddedField                      = DateField.DATE_ADDED;
  Locale                                         nfoLanguage                            = Locale.ENGLISH;
  boolean                                        nfoWriteEpisodeguide                   = true;
  boolean                                        nfoWriteNewEpisodeguideStyle           = true;
  boolean                                        nfoWriteDateEnded                      = false;
  boolean                                        nfoWriteAllActors                      = false;
  boolean                                        nfoWriteSingleStudio                   = false;
  boolean                                        nfoWriteLockdata                       = false;

  // renamer
  boolean                                        renameAfterScrape                      = false;
  boolean                                        updateOnStart                          = false;
  String                                         renamerTvShowFoldername                = DEFAULT_RENAMER_FOLDER_PATTERN;
  String                                         renamerSeasonFoldername                = DEFAULT_RENAMER_SEASON_PATTERN;
  String                                         renamerFilename                        = DEFAULT_RENAMER_FILE_PATTERN;
  boolean                                        renamerShowPathnameSpaceSubstitution   = false;
  String                                         renamerShowPathnameSpaceReplacement    = "_";
  boolean                                        renamerSeasonPathnameSpaceSubstitution = false;
  String                                         renamerSeasonPathnameSpaceReplacement  = "_";
  boolean                                        renamerFilenameSpaceSubstitution       = false;
  String                                         renamerFilenameSpaceReplacement        = "_";
  String                                         renamerColonReplacement                = "";
  boolean                                        renamerCleanupUnwanted                 = false;
  String                                         renamerFirstCharacterNumberReplacement = "#";
  boolean                                        asciiReplacement                       = false;
  boolean                                        specialSeason                          = true;
  boolean                                        createMissingSeasonItems               = false;

  // meta data scraper
  String                                         scraper                                = MediaMetadata.TVDB;
  MediaLanguages                                 scraperLanguage                        = MediaLanguages.en;
  CountryCode                                    certificationCountry                   = CountryCode.US;
  String                                         releaseDateCountry                     = "";
  final List<TvShowScraperMetadataConfig>        tvShowScraperMetadataConfig            = new ArrayList<>();
  final List<TvShowEpisodeScraperMetadataConfig> episodeScraperMetadataConfig           = new ArrayList<>();
  boolean                                        doNotOverwriteExistingData             = false;
  boolean                                        fetchAllRatings                        = false;

  // artwork scraper
  final List<MediaLanguages>                     imageScraperLanguages                  = ObservableCollections.observableList(new ArrayList<>());

  boolean                                        imageScraperOtherResolutions           = true;
  boolean                                        imageScraperFallback                   = true;
  boolean                                        imageScraperPreferFanartWoText         = true;
  MediaArtwork.PosterSizes                       imagePosterSize                        = MediaArtwork.PosterSizes.LARGE;
  MediaArtwork.FanartSizes                       imageFanartSize                        = MediaArtwork.FanartSizes.LARGE;
  MediaArtwork.ThumbSizes                        imageThumbSize                         = MediaArtwork.ThumbSizes.MEDIUM;
  boolean                                        scrapeBestImage                        = true;
  boolean                                        writeActorImages                       = false;
  boolean                                        imageExtraFanart                       = false;
  int                                            imageExtraFanartCount                  = 5;

  // trailer scraper
  boolean                                        useYtDlp                               = false;
  boolean                                        useTrailerPreference                   = true;
  boolean                                        automaticTrailerDownload               = false;
  TrailerQuality                                 trailerQuality                         = TrailerQuality.HD_720;

  // subtitle scraper
  MediaLanguages                                 subtitleScraperLanguage                = MediaLanguages.en;
  LanguageStyle                                  subtitleLanguageStyle                  = LanguageStyle.ISO3T;
  boolean                                        subtitleForceBestMatch                 = false;

  // misc
  boolean                                        buildImageCacheOnImport                = true;
  boolean                                        syncTrakt                              = false;
  boolean                                        syncTraktCollection                    = true;
  boolean                                        syncTraktWatched                       = true;
  boolean                                        syncTraktRating                        = true;
  String                                         preferredRating                        = "tvdb";
  boolean                                        extractArtworkFromVsmeta               = false;
  boolean                                        useMediainfoMetadata                   = false;

  // ui
  final List<MediaFileType>                      showTvShowArtworkTypes                 = ObservableCollections.observableList(new ArrayList<>());
  final List<MediaFileType>                      showSeasonArtworkTypes                 = ObservableCollections.observableList(new ArrayList<>());
  final List<MediaFileType>                      showEpisodeArtworkTypes                = ObservableCollections.observableList(new ArrayList<>());
  boolean                                        displayMissingEpisodes                 = false;
  boolean                                        displayMissingSpecials                 = false;
  boolean                                        displayMissingNotAired                 = false;
  boolean                                        capitalWordsinTitles                   = false;
  boolean                                        showTvShowTableTooltips                = true;
  boolean                                        seasonArtworkFallback                  = false;
  boolean                                        storeUiFilters                         = false;
  boolean                                        resetNewFlagOnUds                      = true;

  final List<UIFilters>                          uiFilters                              = new ArrayList<>();
  final List<UniversalFilterFields>              universalFilterFields                  = new ArrayList<>();
  final List<TvShowScraperMetadataConfig>        tvShowCheckMetadata                    = new ArrayList<>();
  boolean                                        tvShowDisplayAllMissingMetadata        = false;
  final List<TvShowScraperMetadataConfig>        tvShowCheckArtwork                     = new ArrayList<>();
  boolean                                        tvShowDisplayAllMissingArtwork         = false;
  final List<TvShowScraperMetadataConfig>        seasonCheckArtwork                     = new ArrayList<>();
  boolean                                        seasonDisplayAllMissingArtwork         = false;
  final List<TvShowEpisodeScraperMetadataConfig> episodeCheckMetadata                   = new ArrayList<>();
  boolean                                        episodeDisplayAllMissingMetadata       = false;
  boolean                                        episodeSpecialsCheckMissingMetadata    = false;
  final List<TvShowEpisodeScraperMetadataConfig> episodeCheckArtwork                    = new ArrayList<>();
  boolean                                        episodeDisplayAllMissingArtwork        = false;
  boolean                                        episodeSpecialsCheckMissingArtwork     = false;

  // Quick Search filter
  boolean                                        node                                   = true;
  boolean                                        title                                  = true;
  boolean                                        originalTitle                          = true;
  boolean                                        note                                   = false;
  final List<String>                             ratingSources                          = ObservableCollections.observableList(new ArrayList<>());

  public TvShowSettings() {
    super();

    // add default entries to the lists - they will be overwritten by jackson later
    addDefaultEntries();

    addPropertyChangeListener(evt -> setDirty());
  }

  private void addDefaultEntries() {
    // default skip folders
    skipFolders.clear();
    addSkipFolder("MAKEMKV");

    // file names
    nfoFilenames.clear();
    addNfoFilename(TvShowNfoNaming.TV_SHOW);

    posterFilenames.clear();
    addPosterFilename(TvShowPosterNaming.POSTER);

    fanartFilenames.clear();
    addFanartFilename(TvShowFanartNaming.FANART);

    bannerFilenames.clear();
    addBannerFilename(TvShowBannerNaming.BANNER);

    discartFilenames.clear();
    addDiscartFilename(TvShowDiscartNaming.DISCART);

    clearartFilenames.clear();
    addClearartFilename(TvShowClearartNaming.CLEARART);

    characterartFilenames.clear();
    addCharacterartFilename(TvShowCharacterartNaming.CHARACTERART);

    clearlogoFilenames.clear();
    addClearlogoFilename(TvShowClearlogoNaming.CLEARLOGO);

    thumbFilenames.clear();
    addThumbFilename(TvShowThumbNaming.THUMB);

    keyartFilenames.clear();
    addKeyartFilename(TvShowKeyartNaming.KEYART);

    imageScraperLanguages.clear();
    addImageScraperLanguage(MediaLanguages.en);

    seasonNfoFilenames.clear();
    // do default for NFO file namings yet (only Emby supports that)

    seasonPosterFilenames.clear();
    addSeasonPosterFilename(TvShowSeasonPosterNaming.SEASON_POSTER);

    seasonFanartFilenames.clear();
    addSeasonFanartFilename(TvShowSeasonFanartNaming.SEASON_FANART);

    seasonBannerFilenames.clear();
    addSeasonBannerFilename(TvShowSeasonBannerNaming.SEASON_BANNER);

    seasonThumbFilenames.clear();
    addSeasonThumbFilename(TvShowSeasonThumbNaming.SEASON_THUMB);

    episodeNfoFilenames.clear();
    addEpisodeNfoFilename(TvShowEpisodeNfoNaming.FILENAME);

    episodeThumbFilenames.clear();
    addEpisodeThumbFilename(TvShowEpisodeThumbNaming.FILENAME_THUMB);

    showTvShowArtworkTypes.clear();
    addShowTvShowArtworkTypes(MediaFileType.POSTER);
    addShowTvShowArtworkTypes(MediaFileType.FANART);
    addShowTvShowArtworkTypes(MediaFileType.BANNER);

    showSeasonArtworkTypes.clear();
    addShowSeasonArtworkTypes(MediaFileType.SEASON_POSTER);
    addShowSeasonArtworkTypes(MediaFileType.SEASON_THUMB);
    addShowSeasonArtworkTypes(MediaFileType.SEASON_BANNER);

    showEpisodeArtworkTypes.clear();
    addShowEpisodeArtworkTypes(MediaFileType.THUMB);

    tvShowCheckMetadata.clear();
    addTvShowCheckMetadata(TvShowScraperMetadataConfig.ID);
    addTvShowCheckMetadata(TvShowScraperMetadataConfig.TITLE);
    addTvShowCheckMetadata(TvShowScraperMetadataConfig.PLOT);
    addTvShowCheckMetadata(TvShowScraperMetadataConfig.YEAR);
    addTvShowCheckMetadata(TvShowScraperMetadataConfig.STATUS);
    addTvShowCheckMetadata(TvShowScraperMetadataConfig.GENRES);
    addTvShowCheckMetadata(TvShowScraperMetadataConfig.ACTORS);

    tvShowCheckArtwork.clear();
    addTvShowCheckArtwork(TvShowScraperMetadataConfig.POSTER);
    addTvShowCheckArtwork(TvShowScraperMetadataConfig.FANART);
    addTvShowCheckArtwork(TvShowScraperMetadataConfig.BANNER);

    seasonCheckArtwork.clear();
    addSeasonCheckArtwork(TvShowScraperMetadataConfig.SEASON_POSTER);
    addSeasonCheckArtwork(TvShowScraperMetadataConfig.SEASON_BANNER);
    addSeasonCheckArtwork(TvShowScraperMetadataConfig.SEASON_THUMB);

    episodeCheckMetadata.clear();
    addEpisodeCheckMetadata(TvShowEpisodeScraperMetadataConfig.SEASON_EPISODE);
    addEpisodeCheckMetadata(TvShowEpisodeScraperMetadataConfig.TITLE);
    addEpisodeCheckMetadata(TvShowEpisodeScraperMetadataConfig.ACTORS);

    episodeCheckArtwork.clear();
    addEpisodeCheckArtwork(TvShowEpisodeScraperMetadataConfig.THUMB);

    ratingSources.clear();
    addRatingSource(MediaMetadata.IMDB);

    trailerFilenames.clear();
    addTrailerFilename(TvShowTrailerNaming.TVSHOW_TRAILER);

    tvShowScraperMetadataConfig.addAll(Arrays.asList(TvShowScraperMetadataConfig.values()));
    episodeScraperMetadataConfig.addAll(Arrays.asList(TvShowEpisodeScraperMetadataConfig.values()));
    universalFilterFields.addAll(Arrays.asList(UniversalFilterFields.values()));
  }

  @Override
  protected ObjectWriter createObjectWriter() {
    return objectMapper.writerFor(TvShowSettings.class);
  }

  /**
   * Gets the single instance of TvShowSettings.
   *
   * @return single instance of TvShowSettings
   */
  static synchronized TvShowSettings getInstance() {
    return getInstance(Settings.getInstance().getSettingsFolder());
  }

  /**
   * Override our settings folder (defaults to "data")<br>
   * <b>Should only be used for unit testing et all!</b><br>
   *
   * @return single instance of TvShowSettings
   */
  static synchronized TvShowSettings getInstance(String folder) {
    if (instance == null) {
      instance = (TvShowSettings) getInstance(folder, CONFIG_FILE, TvShowSettings.class);
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

  @Override
  public String getConfigFilename() {
    return CONFIG_FILE;
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected void afterLoading() {
    // nothing to do here
  }

  /**
   * the tmm defaults
   */
  @Override
  protected void writeDefaultSettings() {
    addDefaultEntries();

    // activate default scrapers
    for (MediaScraper ms : MediaScraper.getMediaScrapers(ScraperType.TVSHOW_SUBTITLE)) {
      addTvShowSubtitleScraper(ms.getId());
    }
    for (MediaScraper ms : MediaScraper.getMediaScrapers(ScraperType.TVSHOW_ARTWORK)) {
      addTvShowArtworkScraper(ms.getId());
    }

    // set default languages based on java instance
    String defaultLang = Locale.getDefault().getLanguage();
    CountryCode cc = CountryCode.getByCode(defaultLang.toUpperCase(Locale.ROOT));
    if (cc != null) {
      setCertificationCountry(cc);
      setReleaseDateCountry(cc.getAlpha2());
    }
    for (MediaLanguages ml : MediaLanguages.values()) {
      if (ml.name().equals(defaultLang)) {
        setScraperLanguage(ml);
        setNfoLanguage(ml.toLocale());
        imageScraperLanguages.clear();
        addImageScraperLanguage(ml);
        setSubtitleScraperLanguage(ml);
      }
    }

    saveSettings();
  }

  public void setTvShowDataSources(Collection<String> dataSources) {
    tvShowDataSources.clear();
    tvShowDataSources.addAll(dataSources);
    firePropertyChange(TV_SHOW_DATA_SOURCE, null, tvShowDataSources);
    firePropertyChange(Constants.DATA_SOURCE, null, tvShowDataSources);
  }

  public void addTvShowDataSources(String path) {
    if (!tvShowDataSources.contains(path)) {
      tvShowDataSources.add(path);
      firePropertyChange(TV_SHOW_DATA_SOURCE, null, tvShowDataSources);
      firePropertyChange(Constants.DATA_SOURCE, null, tvShowDataSources);
    }
  }

  public void removeTvShowDataSources(String path) {
    TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();
    tvShowList.removeDatasource(path);
    tvShowDataSources.remove(path);
    firePropertyChange(TV_SHOW_DATA_SOURCE, null, tvShowDataSources);
    firePropertyChange(Constants.DATA_SOURCE, null, tvShowDataSources);
  }

  public void exchangeTvShowDatasource(String oldDatasource, String newDatasource) {
    int index = tvShowDataSources.indexOf(oldDatasource);
    if (index > -1) {
      tvShowDataSources.remove(oldDatasource);
      if (!tvShowDataSources.contains(newDatasource)) {
        // just to prevent duplicates
        tvShowDataSources.add(index, newDatasource);
      }
      TvShowModuleManager.getInstance().getTvShowList().exchangeDatasource(oldDatasource, newDatasource);
    }
    firePropertyChange(TV_SHOW_DATA_SOURCE, null, tvShowDataSources);
    firePropertyChange(Constants.DATA_SOURCE, null, tvShowDataSources);
  }

  public List<String> getTvShowDataSource() {
    return tvShowDataSources;
  }

  public void swapTvShowDataSource(int pos1, int pos2) {
    String tmp = tvShowDataSources.get(pos1);
    tvShowDataSources.set(pos1, tvShowDataSources.get(pos2));
    tvShowDataSources.set(pos2, tmp);
    firePropertyChange(TV_SHOW_DATA_SOURCE, null, tvShowDataSources);
    firePropertyChange(Constants.DATA_SOURCE, null, tvShowDataSources);
  }

  public String getScraper() {
    if (StringUtils.isBlank(scraper)) {
      return MediaMetadata.TVDB;
    }
    return scraper;
  }

  public void setScraper(String newValue) {
    String oldValue = this.scraper;
    this.scraper = newValue;
    firePropertyChange("scraper", oldValue, newValue);
  }

  public void addTvShowArtworkScraper(String newValue) {
    if (!artworkScrapers.contains(newValue)) {
      artworkScrapers.add(newValue);
      firePropertyChange(ARTWORK_SCRAPERS, null, artworkScrapers);
    }
  }

  public void removeTvShowArtworkScraper(String newValue) {
    if (artworkScrapers.contains(newValue)) {
      artworkScrapers.remove(newValue);
      firePropertyChange(ARTWORK_SCRAPERS, null, artworkScrapers);
    }
  }

  public void addTrailerFilename(TvShowTrailerNaming filename) {
    if (!trailerFilenames.contains(filename)) {
      trailerFilenames.add(filename);
      firePropertyChange(TRAILER_FILENAME, null, trailerFilenames);
    }
  }

  public void clearTrailerFilenames() {
    trailerFilenames.clear();
    firePropertyChange(TRAILER_FILENAME, null, trailerFilenames);
  }

  public List<TvShowTrailerNaming> getTrailerFilenames() {
    return Collections.unmodifiableList(this.trailerFilenames);
  }

  public boolean isUseYtDlp() {
    return useYtDlp;
  }

  public void setUseYtDlp(boolean newValue) {
    boolean oldValue = this.useYtDlp;
    this.useYtDlp = newValue;
    firePropertyChange("useYtDlp", oldValue, newValue);
  }

  public boolean isUseTrailerPreference() {
    return useTrailerPreference;
  }

  public void setUseTrailerPreference(boolean newValue) {
    boolean oldValue = this.useTrailerPreference;
    this.useTrailerPreference = newValue;
    firePropertyChange("useTrailerPreference", oldValue, newValue);
  }

  public boolean isAutomaticTrailerDownload() {
    return automaticTrailerDownload;
  }

  public void setAutomaticTrailerDownload(boolean newValue) {
    boolean oldValue = this.automaticTrailerDownload;
    this.automaticTrailerDownload = newValue;
    firePropertyChange("automaticTrailerDownload", oldValue, newValue);
  }

  public TrailerQuality getTrailerQuality() {
    return trailerQuality;
  }

  public void setTrailerQuality(TrailerQuality newValue) {
    TrailerQuality oldValue = this.trailerQuality;
    this.trailerQuality = newValue;
    firePropertyChange("trailerQuality", oldValue, newValue);
  }

  public void addTvShowTrailerScraper(String newValue) {
    if (!trailerScrapers.contains(newValue)) {
      trailerScrapers.add(newValue);
      firePropertyChange(TRAILER_SCRAPERS, null, trailerScrapers);
    }
  }

  public void removeTvShowTrailerScraper(String newValue) {
    if (trailerScrapers.contains(newValue)) {
      trailerScrapers.remove(newValue);
      firePropertyChange(TRAILER_SCRAPERS, null, trailerScrapers);
    }
  }

  public List<String> getTrailerScrapers() {
    return trailerScrapers;
  }

  public List<String> getArtworkScrapers() {
    return artworkScrapers;
  }

  /**
   * Automatic image download without choosing?
   * 
   * @return true = download w/o image chooser<br>
   *         false = display image chooser and let the user decide
   */
  public boolean isScrapeBestImage() {
    return scrapeBestImage;
  }

  public void setScrapeBestImage(boolean newValue) {
    boolean oldValue = this.scrapeBestImage;
    this.scrapeBestImage = newValue;
    firePropertyChange("scrapeBestImage", oldValue, newValue);
  }

  public MediaLanguages getScraperLanguage() {
    return scraperLanguage;
  }

  public void setScraperLanguage(MediaLanguages newValue) {
    MediaLanguages oldValue = this.scraperLanguage;
    this.scraperLanguage = newValue;
    firePropertyChange("scraperLanguage", oldValue, newValue);
  }

  public MediaLanguages getSubtitleScraperLanguage() {
    return subtitleScraperLanguage;
  }

  public void setSubtitleScraperLanguage(MediaLanguages newValue) {
    MediaLanguages oldValue = this.subtitleScraperLanguage;
    this.subtitleScraperLanguage = newValue;
    firePropertyChange("subtitleScraperLanguage", oldValue, newValue);
  }

  public CountryCode getCertificationCountry() {
    return certificationCountry;
  }

  public void setCertificationCountry(CountryCode newValue) {
    CountryCode oldValue = this.certificationCountry;
    certificationCountry = newValue;
    firePropertyChange(CERTIFICATION_COUNTRY, oldValue, newValue);
  }

  public String getReleaseDateCountry() {
    return releaseDateCountry;
  }

  public void setReleaseDateCountry(String newValue) {
    String oldValue = this.releaseDateCountry;
    this.releaseDateCountry = newValue;
    firePropertyChange("releaseDateCountry", oldValue, newValue);
  }

  public String getRenamerSeasonFoldername() {
    return renamerSeasonFoldername;
  }

  public void setRenamerSeasonFoldername(String newValue) {
    String oldValue = this.renamerSeasonFoldername;
    this.renamerSeasonFoldername = newValue;
    firePropertyChange(RENAMER_SEASON_FOLDER, oldValue, newValue);
  }

  public String getRenamerTvShowFoldername() {
    return renamerTvShowFoldername;
  }

  public void setRenamerTvShowFoldername(String newValue) {
    String oldValue = this.renamerTvShowFoldername;
    this.renamerTvShowFoldername = newValue;
    firePropertyChange("renamerTvShowFoldername", oldValue, newValue);
  }

  public String getRenamerFilename() {
    return renamerFilename;
  }

  public void setRenamerFilename(String newValue) {
    String oldValue = this.renamerFilename;
    this.renamerFilename = newValue;
    firePropertyChange("renamerFilename", oldValue, newValue);
  }

  public boolean isUpdateOnStart() {
    return this.updateOnStart;
  }

  public void setUpdateOnStart(boolean newValue) {
    boolean oldValue = this.updateOnStart;
    this.updateOnStart = newValue;
    firePropertyChange("updateOnStart", oldValue, newValue);
  }

  public boolean isBuildImageCacheOnImport() {
    return buildImageCacheOnImport;
  }

  public void setBuildImageCacheOnImport(boolean newValue) {
    boolean oldValue = this.buildImageCacheOnImport;
    this.buildImageCacheOnImport = newValue;
    firePropertyChange("buildImageCacheOnImport", oldValue, newValue);
  }

  public boolean isExtractArtworkFromVsmeta() {
    return extractArtworkFromVsmeta;
  }

  public void setExtractArtworkFromVsmeta(boolean newValue) {
    boolean oldValue = this.extractArtworkFromVsmeta;
    this.extractArtworkFromVsmeta = newValue;
    firePropertyChange("extractArtworkFromVsmeta", oldValue, newValue);
  }

  public boolean isUseMediainfoMetadata() {
    return useMediainfoMetadata;
  }

  public void setUseMediainfoMetadata(boolean newValue) {
    boolean oldValue = this.useMediainfoMetadata;
    this.useMediainfoMetadata = newValue;
    firePropertyChange("useMediainfoMetadata", oldValue, newValue);
  }

  public boolean isAsciiReplacement() {
    return asciiReplacement;
  }

  public void setAsciiReplacement(boolean newValue) {
    boolean oldValue = this.asciiReplacement;
    this.asciiReplacement = newValue;
    firePropertyChange("asciiReplacement", oldValue, newValue);
  }

  public boolean isRenamerCleanupUnwanted() {
    return renamerCleanupUnwanted;
  }

  public void setRenamerCleanupUnwanted(boolean newValue) {
    boolean oldValue = this.renamerCleanupUnwanted;
    this.renamerCleanupUnwanted = newValue;
    firePropertyChange("renamerCleanupUnwanted", oldValue, newValue);
  }

  public boolean isSpecialSeason() {
    return specialSeason;
  }

  public void setSpecialSeason(boolean newValue) {
    boolean oldValue = this.specialSeason;
    this.specialSeason = newValue;
    firePropertyChange("specialSeason", oldValue, newValue);
  }

  public boolean isCreateMissingSeasonItems() {
    return createMissingSeasonItems;
  }

  public void setCreateMissingSeasonItems(boolean newValue) {
    boolean oldValue = this.createMissingSeasonItems;
    this.createMissingSeasonItems = newValue;
    firePropertyChange("createMissingSeasonItems", oldValue, newValue);
  }

  public String getRenamerShowPathnameSpaceReplacement() {
    return renamerShowPathnameSpaceReplacement;
  }

  public void setRenamerShowPathnameSpaceReplacement(String newValue) {
    String oldValue = this.renamerShowPathnameSpaceReplacement;
    this.renamerShowPathnameSpaceReplacement = newValue;
    firePropertyChange("renamerShowPathnameSpaceReplacement", oldValue, newValue);
  }

  public String getRenamerSeasonPathnameSpaceReplacement() {
    return renamerSeasonPathnameSpaceReplacement;
  }

  public void setRenamerSeasonPathnameSpaceReplacement(String newValue) {
    String oldValue = this.renamerSeasonPathnameSpaceReplacement;
    this.renamerSeasonPathnameSpaceReplacement = newValue;
    firePropertyChange("renamerSeasonPathnameSpaceReplacement", oldValue, newValue);
  }

  public String getRenamerFilenameSpaceReplacement() {
    return renamerFilenameSpaceReplacement;
  }

  public void setRenamerFilenameSpaceReplacement(String newValue) {
    String oldValue = this.renamerFilenameSpaceReplacement;
    this.renamerFilenameSpaceReplacement = newValue;
    firePropertyChange("renamerFilenameSpaceReplacement", oldValue, newValue);
  }

  public String getRenamerColonReplacement() {
    return renamerColonReplacement;
  }

  public void setRenamerColonReplacement(String newValue) {
    String oldValue = this.renamerColonReplacement;
    this.renamerColonReplacement = newValue;
    firePropertyChange("renamerColonReplacement", oldValue, newValue);
  }

  public String getRenamerFirstCharacterNumberReplacement() {
    return renamerFirstCharacterNumberReplacement;
  }

  public void setRenamerFirstCharacterNumberReplacement(String newValue) {
    String oldValue = this.renamerFirstCharacterNumberReplacement;
    this.renamerFirstCharacterNumberReplacement = newValue;
    firePropertyChange("renamerFirstCharacterNumberReplacement", oldValue, newValue);
  }

  public boolean isRenamerShowPathnameSpaceSubstitution() {
    return renamerShowPathnameSpaceSubstitution;
  }

  public void setRenamerShowPathnameSpaceSubstitution(boolean newValue) {
    boolean oldValue = this.renamerShowPathnameSpaceSubstitution;
    this.renamerShowPathnameSpaceSubstitution = newValue;
    firePropertyChange("renamerShowPathnameSpaceSubstitution", oldValue, newValue);
  }

  public boolean isRenamerSeasonPathnameSpaceSubstitution() {
    return renamerSeasonPathnameSpaceSubstitution;
  }

  public void setRenamerSeasonPathnameSpaceSubstitution(boolean newValue) {
    boolean oldValue = this.renamerSeasonPathnameSpaceSubstitution;
    this.renamerSeasonPathnameSpaceSubstitution = newValue;
    firePropertyChange("renamereasonPathnameSpaceSubstitution", oldValue, newValue);
  }

  public boolean isRenamerFilenameSpaceSubstitution() {
    return renamerFilenameSpaceSubstitution;
  }

  public void setRenamerFilenameSpaceSubstitution(boolean newValue) {
    boolean oldValue = this.renamerFilenameSpaceSubstitution;
    this.renamerFilenameSpaceSubstitution = newValue;
    firePropertyChange("renamerFilenameSpaceSubstitution", oldValue, newValue);
  }

  public void setSyncTrakt(boolean newValue) {
    boolean oldValue = this.syncTrakt;
    this.syncTrakt = newValue;
    firePropertyChange("syncTrakt", oldValue, newValue);
  }

  public boolean getSyncTrakt() {
    return syncTrakt;
  }

  public void setSyncTraktCollection(boolean newValue) {
    boolean oldValue = this.syncTraktCollection;
    this.syncTraktCollection = newValue;
    firePropertyChange("syncTraktCollection", oldValue, newValue);
  }

  public boolean getSyncTraktCollection() {
    return syncTraktCollection;
  }

  public void setSyncTraktWatched(boolean newValue) {
    boolean oldValue = this.syncTraktWatched;
    this.syncTraktWatched = newValue;
    firePropertyChange("syncTraktWatched", oldValue, newValue);
  }

  public boolean getSyncTraktWatched() {
    return syncTraktWatched;
  }

  public void setSyncTraktRating(boolean newValue) {
    boolean oldValue = this.syncTraktRating;
    this.syncTraktRating = newValue;
    firePropertyChange("syncTraktRating", oldValue, newValue);
  }

  public boolean getSyncTraktRating() {
    return syncTraktRating;
  }

  @JsonSetter
  public void setSkipFolder(List<String> newValues) {
    skipFolders.clear();
    skipFolders.addAll(newValues);
  }

  public void addSkipFolder(String newValue) {
    if (!skipFolders.contains(newValue)) {
      skipFolders.add(newValue);
      firePropertyChange(SKIP_FOLDER, null, skipFolders);
    }
  }

  public void removeSkipFolder(String newValue) {
    if (skipFolders.contains(newValue)) {
      skipFolders.remove(newValue);
      firePropertyChange(SKIP_FOLDER, null, skipFolders);
    }
  }

  public List<String> getSkipFolder() {
    return skipFolders;
  }

  public void addBadWord(String badWord) {
    if (!badWords.contains(badWord.toLowerCase(Locale.ROOT))) {
      badWords.add(badWord.toLowerCase(Locale.ROOT));
      firePropertyChange(BAD_WORD, null, badWords);
    }
  }

  public void removeBadWord(String badWord) {
    badWords.remove(badWord.toLowerCase(Locale.ROOT));
    firePropertyChange(BAD_WORD, null, badWords);
  }

  public List<String> getBadWord() {
    // convert to lowercase for easy contains checking
    ListIterator<String> iterator = badWords.listIterator();
    while (iterator.hasNext()) {
      iterator.set(iterator.next().toLowerCase(Locale.ROOT));
    }
    return badWords;
  }

  public void addEpisodeThumbFilename(TvShowEpisodeThumbNaming filename) {
    if (!episodeThumbFilenames.contains(filename)) {
      episodeThumbFilenames.add(filename);
      firePropertyChange(EPISODE_THUMB_FILENAME, null, episodeThumbFilenames);
    }
  }

  public void clearEpisodeThumbFilenames() {
    episodeThumbFilenames.clear();
    firePropertyChange(EPISODE_THUMB_FILENAME, null, episodeThumbFilenames);
  }

  public List<TvShowEpisodeThumbNaming> getEpisodeThumbFilenames() {
    return Collections.unmodifiableList(this.episodeThumbFilenames);
  }

  public void addTvShowSubtitleScraper(String newValue) {
    if (!subtitleScrapers.contains(newValue)) {
      subtitleScrapers.add(newValue);
      firePropertyChange(SUBTITLE_SCRAPERS, null, subtitleScrapers);
    }
  }

  public void removeTvShowSubtitleScraper(String newValue) {
    if (subtitleScrapers.contains(newValue)) {
      subtitleScrapers.remove(newValue);
      firePropertyChange(SUBTITLE_SCRAPERS, null, subtitleScrapers);
    }
  }

  public List<String> getSubtitleScrapers() {
    return subtitleScrapers;
  }

  public LanguageStyle getSubtitleLanguageStyle() {
    return subtitleLanguageStyle;
  }

  public void setSubtitleLanguageStyle(LanguageStyle newValue) {
    LanguageStyle oldValue = this.subtitleLanguageStyle;
    this.subtitleLanguageStyle = newValue;
    firePropertyChange("subtitleLanguageStyle", oldValue, newValue);
  }

  public boolean getSubtitleForceBestMatch() {
    return subtitleForceBestMatch;
  }

  public void setSubtitleForceBestMatch(boolean newValue) {
    boolean oldValue = this.subtitleForceBestMatch;
    this.subtitleForceBestMatch = newValue;
    firePropertyChange("subtitleForceBestMatch", oldValue, newValue);
  }

  public Map<String, List<UIFilters>> getUiFilterPresets() {
    return uiFilterPresets;
  }

  public void setUiFilterPresets(Map<String, List<UIFilters>> newValues) {
    uiFilterPresets.clear();
    uiFilterPresets.putAll(newValues);
    firePropertyChange("uiFilterPresets", null, uiFilterPresets);
  }

  public boolean isDisplayMissingEpisodes() {
    return displayMissingEpisodes;
  }

  public void setDisplayMissingEpisodes(boolean newValue) {
    boolean oldValue = this.displayMissingEpisodes;
    this.displayMissingEpisodes = newValue;
    firePropertyChange("displayMissingEpisodes", oldValue, newValue);
  }

  public boolean isDisplayMissingSpecials() {
    return displayMissingSpecials;
  }

  public void setDisplayMissingSpecials(boolean newValue) {
    boolean oldValue = this.displayMissingSpecials;
    this.displayMissingSpecials = newValue;
    firePropertyChange("displayMissingSpecials", oldValue, newValue);
  }

  public boolean isDisplayMissingNotAired() {
    return displayMissingNotAired;
  }

  public void setDisplayMissingNotAired(boolean newValue) {
    boolean oldValue = this.displayMissingNotAired;
    this.displayMissingNotAired = newValue;
    firePropertyChange("displayMissingNotAired", oldValue, newValue);
  }

  public void setNode(boolean newValue) {
    boolean oldValue = this.node;
    this.node = newValue;
    firePropertyChange(NODE, oldValue, newValue);
  }

  public boolean getNode() {
    return this.node;
  }

  public void setTitle(boolean newValue) {
    boolean oldValue = this.node;
    this.node = newValue;
    firePropertyChange(TITLE, oldValue, newValue);
  }

  public boolean getTitle() {
    return this.title;
  }

  public void setOriginalTitle(boolean newValue) {
    boolean oldValue = this.originalTitle;
    this.originalTitle = newValue;
    firePropertyChange(ORIGINAL_TITLE, oldValue, newValue);
  }

  public boolean getOriginalTitle() {
    return this.originalTitle;
  }

  public void setNote(boolean newValue) {
    boolean oldValue = this.note;
    this.note = newValue;
    firePropertyChange(NOTE, oldValue, newValue);
  }

  public boolean getNote() {
    return this.note;
  }

  /**
   * Gets the tv show scraper metadata config.
   *
   * @return the tv show scraper metadata config
   */
  public List<TvShowScraperMetadataConfig> getTvShowScraperMetadataConfig() {
    return tvShowScraperMetadataConfig;
  }

  /**
   * Sets the tv show scraper metadata config.
   *
   * @param tvShowScraperMetadataConfig
   *          the new tv show scraper metadata config
   */
  public void setTvShowScraperMetadataConfig(List<TvShowScraperMetadataConfig> tvShowScraperMetadataConfig) {
    this.tvShowScraperMetadataConfig.clear();
    this.tvShowScraperMetadataConfig.addAll(tvShowScraperMetadataConfig);
    firePropertyChange("scraperMetadataConfig", null, tvShowScraperMetadataConfig);
  }

  /**
   * Gets the episode scraper metadata config.
   *
   * @return the episode scraper metadata config
   */
  public List<TvShowEpisodeScraperMetadataConfig> getEpisodeScraperMetadataConfig() {
    return episodeScraperMetadataConfig;
  }

  /**
   * Sets the episode scraper metadata config.
   *
   * @param scraperMetadataConfig
   *          the new episode scraper metadata config
   */
  public void setEpisodeScraperMetadataConfig(List<TvShowEpisodeScraperMetadataConfig> scraperMetadataConfig) {
    this.episodeScraperMetadataConfig.clear();
    this.episodeScraperMetadataConfig.addAll(scraperMetadataConfig);
    firePropertyChange("episodeScraperMetadataConfig", null, episodeScraperMetadataConfig);
  }

  public void setImageScraperLanguages(List<MediaLanguages> newValue) {
    imageScraperLanguages.clear();
    imageScraperLanguages.addAll(newValue);
    firePropertyChange("imageScraperLanguages", null, imageScraperLanguages);
  }

  public void addImageScraperLanguage(MediaLanguages language) {
    if (!imageScraperLanguages.contains(language)) {
      imageScraperLanguages.add(language);
      firePropertyChange("imageScraperLanguages", null, imageScraperLanguages);
    }
  }

  public void removeImageScraperLanguage(MediaLanguages language) {
    if (imageScraperLanguages.remove(language)) {
      firePropertyChange("imageScraperLanguages", null, imageScraperLanguages);
    }
  }

  public void swapImageScraperLanguage(int pos1, int pos2) {
    MediaLanguages tmp = imageScraperLanguages.get(pos1);
    imageScraperLanguages.set(pos1, imageScraperLanguages.get(pos2));
    imageScraperLanguages.set(pos2, tmp);
    firePropertyChange("imageScraperLanguages", null, imageScraperLanguages);
  }

  public List<MediaLanguages> getImageScraperLanguages() {
    return imageScraperLanguages;
  }

  public MediaLanguages getDefaultImageScraperLanguage() {
    MediaLanguages language = scraperLanguage; // fallback
    if (!imageScraperLanguages.isEmpty()) {
      language = imageScraperLanguages.get(0);
    }
    return language;
  }

  public boolean isImageScraperOtherResolutions() {
    return imageScraperOtherResolutions;
  }

  public void setImageScraperOtherResolutions(boolean newValue) {
    boolean oldValue = this.imageScraperOtherResolutions;
    this.imageScraperOtherResolutions = newValue;
    firePropertyChange("imageScraperOtherResolutions", oldValue, newValue);
  }

  public boolean isImageScraperFallback() {
    return imageScraperFallback;
  }

  public void setImageScraperFallback(boolean newValue) {
    boolean oldValue = this.imageScraperFallback;
    this.imageScraperFallback = newValue;
    firePropertyChange("imageScraperFallback", oldValue, newValue);
  }

  public boolean isImageScraperPreferFanartWoText() {
    return imageScraperPreferFanartWoText;
  }

  public void setImageScraperPreferFanartWoText(boolean newValue) {
    boolean oldValue = this.imageScraperPreferFanartWoText;
    this.imageScraperPreferFanartWoText = newValue;
    firePropertyChange("imageScraperPreferFanartWoText", oldValue, newValue);
  }

  public MediaArtwork.PosterSizes getImagePosterSize() {
    return imagePosterSize;
  }

  public void setImagePosterSize(MediaArtwork.PosterSizes newValue) {
    MediaArtwork.PosterSizes oldValue = this.imagePosterSize;
    this.imagePosterSize = newValue;
    firePropertyChange("imagePosterSize", oldValue, newValue);
  }

  public MediaArtwork.FanartSizes getImageFanartSize() {
    return imageFanartSize;
  }

  public void setImageFanartSize(MediaArtwork.FanartSizes newValue) {
    MediaArtwork.FanartSizes oldValue = this.imageFanartSize;
    this.imageFanartSize = newValue;
    firePropertyChange("imageFanartSize", oldValue, newValue);
  }

  public MediaArtwork.ThumbSizes getImageThumbSize() {
    return imageThumbSize;
  }

  public void setImageThumbSize(MediaArtwork.ThumbSizes newValue) {
    MediaArtwork.ThumbSizes oldValue = this.imageThumbSize;
    this.imageThumbSize = newValue;
    firePropertyChange("imageThumbSize", oldValue, newValue);
  }

  public void addNfoFilename(TvShowNfoNaming filename) {
    if (!nfoFilenames.contains(filename)) {
      nfoFilenames.add(filename);
      firePropertyChange(NFO_FILENAME, null, nfoFilenames);
    }
  }

  public void clearNfoFilenames() {
    nfoFilenames.clear();
    firePropertyChange(NFO_FILENAME, null, nfoFilenames);
  }

  public List<TvShowNfoNaming> getNfoFilenames() {
    return Collections.unmodifiableList(this.nfoFilenames);
  }

  public void addPosterFilename(TvShowPosterNaming filename) {
    if (!posterFilenames.contains(filename)) {
      posterFilenames.add(filename);
      firePropertyChange(POSTER_FILENAME, null, posterFilenames);
    }
  }

  public void clearPosterFilenames() {
    posterFilenames.clear();
    firePropertyChange(POSTER_FILENAME, null, posterFilenames);
  }

  public List<TvShowPosterNaming> getPosterFilenames() {
    return Collections.unmodifiableList(this.posterFilenames);
  }

  public void addFanartFilename(TvShowFanartNaming filename) {
    if (!fanartFilenames.contains(filename)) {
      fanartFilenames.add(filename);
      firePropertyChange(FANART_FILENAME, null, fanartFilenames);
    }
  }

  public void clearFanartFilenames() {
    fanartFilenames.clear();
    firePropertyChange(FANART_FILENAME, null, fanartFilenames);
  }

  public List<TvShowFanartNaming> getFanartFilenames() {
    return Collections.unmodifiableList(this.fanartFilenames);
  }

  public void addExtraFanartFilename(TvShowExtraFanartNaming filename) {
    if (!extraFanartFilenames.contains(filename)) {
      extraFanartFilenames.add(filename);
      firePropertyChange(EXTRAFANART_FILENAME, null, extraFanartFilenames);
    }
  }

  public void clearExtraFanartFilenames() {
    extraFanartFilenames.clear();
    firePropertyChange(EXTRAFANART_FILENAME, null, extraFanartFilenames);
  }

  public List<TvShowExtraFanartNaming> getExtraFanartFilenames() {
    return Collections.unmodifiableList(this.extraFanartFilenames);
  }

  public void addBannerFilename(TvShowBannerNaming filename) {
    if (!bannerFilenames.contains(filename)) {
      bannerFilenames.add(filename);
      firePropertyChange(BANNER_FILENAME, null, bannerFilenames);
    }
  }

  public void clearBannerFilenames() {
    bannerFilenames.clear();
    firePropertyChange(BANNER_FILENAME, null, bannerFilenames);
  }

  public List<TvShowBannerNaming> getBannerFilenames() {
    return Collections.unmodifiableList(this.bannerFilenames);
  }

  public void addDiscartFilename(TvShowDiscartNaming filename) {
    if (!discartFilenames.contains(filename)) {
      discartFilenames.add(filename);
      firePropertyChange(DISCART_FILENAME, null, discartFilenames);
    }
  }

  public void clearDiscartFilenames() {
    discartFilenames.clear();
    firePropertyChange(DISCART_FILENAME, null, discartFilenames);
  }

  public List<TvShowDiscartNaming> getDiscartFilenames() {
    return Collections.unmodifiableList(this.discartFilenames);
  }

  public void addClearartFilename(TvShowClearartNaming filename) {
    if (!clearartFilenames.contains(filename)) {
      clearartFilenames.add(filename);
      firePropertyChange(CLEARART_FILENAME, null, clearartFilenames);
    }
  }

  public void clearClearartFilenames() {
    clearartFilenames.clear();
    firePropertyChange(CLEARART_FILENAME, null, clearartFilenames);
  }

  public List<TvShowClearartNaming> getClearartFilenames() {
    return Collections.unmodifiableList(this.clearartFilenames);
  }

  public void addThumbFilename(TvShowThumbNaming filename) {
    if (!thumbFilenames.contains(filename)) {
      thumbFilenames.add(filename);
      firePropertyChange(THUMB_FILENAME, null, thumbFilenames);
    }
  }

  public void clearThumbFilenames() {
    thumbFilenames.clear();
    firePropertyChange(THUMB_FILENAME, null, thumbFilenames);
  }

  public List<TvShowThumbNaming> getThumbFilenames() {
    return Collections.unmodifiableList(this.thumbFilenames);
  }

  public void addCharacterartFilename(TvShowCharacterartNaming filename) {
    if (!characterartFilenames.contains(filename)) {
      characterartFilenames.add(filename);
      firePropertyChange(CHARACTERART_FILENAME, null, characterartFilenames);
    }
  }

  public void clearCharacterartFilenames() {
    characterartFilenames.clear();
  }

  public List<TvShowCharacterartNaming> getCharacterartFilenames() {
    return characterartFilenames;
  }

  public void addKeyartFilename(TvShowKeyartNaming filename) {
    if (!keyartFilenames.contains(filename)) {
      keyartFilenames.add(filename);
      firePropertyChange(KEYART_FILENAME, null, keyartFilenames);
    }
  }

  public void clearKeyartFilenames() {
    keyartFilenames.clear();
    firePropertyChange(KEYART_FILENAME, null, keyartFilenames);
  }

  public List<TvShowKeyartNaming> getKeyartFilenames() {
    return keyartFilenames;
  }

  public void addClearlogoFilename(TvShowClearlogoNaming filename) {
    if (!clearlogoFilenames.contains(filename)) {
      clearlogoFilenames.add(filename);
      firePropertyChange(CLEARLOGO_FILENAME, null, clearlogoFilenames);
    }
  }

  public void clearClearlogoFilenames() {
    clearlogoFilenames.clear();
    firePropertyChange(CLEARLOGO_FILENAME, null, clearlogoFilenames);
  }

  public List<TvShowClearlogoNaming> getClearlogoFilenames() {
    return Collections.unmodifiableList(this.clearlogoFilenames);
  }

  public void addSeasonNfoFilename(TvShowSeasonNfoNaming filename) {
    if (!seasonNfoFilenames.contains(filename)) {
      seasonNfoFilenames.add(filename);
      firePropertyChange(SEASON_NFO_FILENAME, null, seasonNfoFilenames);
    }
  }

  public void clearSeasonNfoFilenames() {
    seasonNfoFilenames.clear();
    firePropertyChange(SEASON_NFO_FILENAME, null, seasonNfoFilenames);
  }

  public List<TvShowSeasonNfoNaming> getSeasonNfoFilenames() {
    return Collections.unmodifiableList(seasonNfoFilenames);
  }

  public void addSeasonPosterFilename(TvShowSeasonPosterNaming filename) {
    if (!seasonPosterFilenames.contains(filename)) {
      seasonPosterFilenames.add(filename);
      firePropertyChange(SEASON_POSTER_FILENAME, null, seasonPosterFilenames);
    }
  }

  public void clearSeasonPosterFilenames() {
    seasonPosterFilenames.clear();
    firePropertyChange(SEASON_POSTER_FILENAME, null, seasonPosterFilenames);
  }

  public List<TvShowSeasonPosterNaming> getSeasonPosterFilenames() {
    return Collections.unmodifiableList(this.seasonPosterFilenames);
  }

  public void addSeasonFanartFilename(TvShowSeasonFanartNaming filename) {
    if (!seasonFanartFilenames.contains(filename)) {
      seasonFanartFilenames.add(filename);
      firePropertyChange(SEASON_FANART_FILENAME, null, seasonFanartFilenames);
    }
  }

  public void clearSeasonFanartFilenames() {
    seasonFanartFilenames.clear();
    firePropertyChange(SEASON_FANART_FILENAME, null, seasonFanartFilenames);
  }

  public List<TvShowSeasonFanartNaming> getSeasonFanartFilenames() {
    return Collections.unmodifiableList(this.seasonFanartFilenames);
  }

  public void addSeasonBannerFilename(TvShowSeasonBannerNaming filename) {
    if (!seasonBannerFilenames.contains(filename)) {
      seasonBannerFilenames.add(filename);
      firePropertyChange(SEASON_BANNER_FILENAME, null, seasonBannerFilenames);
    }
  }

  public void clearSeasonBannerFilenames() {
    seasonBannerFilenames.clear();
    firePropertyChange(SEASON_BANNER_FILENAME, null, seasonBannerFilenames);
  }

  public List<TvShowSeasonBannerNaming> getSeasonBannerFilenames() {
    return Collections.unmodifiableList(this.seasonBannerFilenames);
  }

  public void addSeasonThumbFilename(TvShowSeasonThumbNaming filename) {
    if (!seasonThumbFilenames.contains(filename)) {
      seasonThumbFilenames.add(filename);
      firePropertyChange(SEASON_THUMB_FILENAME, null, seasonThumbFilenames);
    }
  }

  public void clearSeasonThumbFilenames() {
    seasonThumbFilenames.clear();
    firePropertyChange(SEASON_THUMB_FILENAME, null, seasonThumbFilenames);
  }

  public List<TvShowSeasonThumbNaming> getSeasonThumbFilenames() {
    return Collections.unmodifiableList(this.seasonThumbFilenames);
  }

  public void addEpisodeNfoFilename(TvShowEpisodeNfoNaming filename) {
    if (!episodeNfoFilenames.contains(filename)) {
      episodeNfoFilenames.add(filename);
      firePropertyChange(EPISODE_NFO_FILENAME, null, episodeNfoFilenames);
    }
  }

  public void clearEpisodeNfoFilenames() {
    episodeNfoFilenames.clear();
    firePropertyChange(EPISODE_NFO_FILENAME, null, episodeNfoFilenames);
  }

  public List<TvShowEpisodeNfoNaming> getEpisodeNfoFilenames() {
    return Collections.unmodifiableList(this.episodeNfoFilenames);
  }

  public void clearTvShowCheckMetadata() {
    tvShowCheckMetadata.clear();
    firePropertyChange(TVSHOW_CHECK_METADATA, null, tvShowCheckMetadata);
  }

  public List<TvShowScraperMetadataConfig> getTvShowCheckMetadata() {
    return Collections.unmodifiableList(tvShowCheckMetadata);
  }

  public void addTvShowCheckMetadata(TvShowScraperMetadataConfig config) {
    if (!tvShowCheckMetadata.contains(config)) {
      tvShowCheckMetadata.add(config);
      firePropertyChange(TVSHOW_CHECK_METADATA, null, tvShowCheckMetadata);
    }
  }

  public void setTvShowDisplayAllMissingMetadata(boolean newValue) {
    boolean oldValue = tvShowDisplayAllMissingMetadata;
    tvShowDisplayAllMissingMetadata = newValue;
    firePropertyChange("tvShowDisplayAllMissingMetadata", oldValue, newValue);
  }

  public boolean isTvShowDisplayAllMissingMetadata() {
    return tvShowDisplayAllMissingMetadata;
  }

  public void clearTvShowCheckArtwork() {
    tvShowCheckArtwork.clear();
    firePropertyChange(TVSHOW_CHECK_ARTWORK, null, tvShowCheckArtwork);
  }

  public List<TvShowScraperMetadataConfig> getTvShowCheckArtwork() {
    return Collections.unmodifiableList(tvShowCheckArtwork);
  }

  public void addTvShowCheckArtwork(TvShowScraperMetadataConfig config) {
    if (!tvShowCheckArtwork.contains(config)) {
      tvShowCheckArtwork.add(config);
      firePropertyChange(TVSHOW_CHECK_ARTWORK, null, tvShowCheckArtwork);
    }
  }

  public void removeTvShowCheckArtwork(TvShowScraperMetadataConfig config) {
    if (tvShowCheckArtwork.remove(config)) {
      firePropertyChange(TVSHOW_CHECK_ARTWORK, null, tvShowCheckArtwork);
    }
  }

  public void setTvShowDisplayAllMissingArtwork(boolean newValue) {
    boolean oldValue = tvShowDisplayAllMissingArtwork;
    tvShowDisplayAllMissingArtwork = newValue;
    firePropertyChange("tvShowDisplayAllMissingArtwork", oldValue, newValue);
  }

  public boolean isTvShowDisplayAllMissingArtwork() {
    return tvShowDisplayAllMissingArtwork;
  }

  public void clearSeasonCheckArtwork() {
    seasonCheckArtwork.clear();
    firePropertyChange(SEASON_CHECK_ARTWORK, null, seasonCheckArtwork);
  }

  public List<TvShowScraperMetadataConfig> getSeasonCheckArtwork() {
    return Collections.unmodifiableList(seasonCheckArtwork);
  }

  public void addSeasonCheckArtwork(TvShowScraperMetadataConfig config) {
    if (!seasonCheckArtwork.contains(config)) {
      seasonCheckArtwork.add(config);
      firePropertyChange(SEASON_CHECK_ARTWORK, null, seasonCheckArtwork);
    }
  }

  public void setSeasonDisplayAllMissingArtwork(boolean newValue) {
    boolean oldValue = seasonDisplayAllMissingArtwork;
    seasonDisplayAllMissingArtwork = newValue;
    firePropertyChange("seasonDisplayAllMissingArtwork", oldValue, newValue);
  }

  public boolean isSeasonDisplayAllMissingArtwork() {
    return seasonDisplayAllMissingArtwork;
  }

  public void clearEpisodeCheckMetadata() {
    episodeCheckMetadata.clear();
    firePropertyChange(EPISODE_CHECK_METADATA, null, episodeCheckMetadata);
  }

  public List<TvShowEpisodeScraperMetadataConfig> getEpisodeCheckMetadata() {
    return Collections.unmodifiableList(episodeCheckMetadata);
  }

  public void addEpisodeCheckMetadata(TvShowEpisodeScraperMetadataConfig config) {
    if (!episodeCheckMetadata.contains(config)) {
      episodeCheckMetadata.add(config);
      firePropertyChange(EPISODE_CHECK_METADATA, null, episodeCheckMetadata);
    }
  }

  public void setEpisodeDisplayAllMissingMetadata(boolean newValue) {
    boolean oldValue = episodeDisplayAllMissingMetadata;
    episodeDisplayAllMissingMetadata = newValue;
    firePropertyChange("episodeDisplayAllMissingMetadata", oldValue, newValue);
  }

  public boolean isEpisodeDisplayAllMissingMetadata() {
    return episodeDisplayAllMissingMetadata;
  }

  public void setEpisodeSpecialsCheckMissingMetadata(boolean newValue) {
    boolean oldValue = episodeSpecialsCheckMissingMetadata;
    episodeSpecialsCheckMissingMetadata = newValue;
    firePropertyChange("episodeSpecialsCheckMissingMetadata", oldValue, newValue);
  }

  public boolean isEpisodeSpecialsCheckMissingMetadata() {
    return episodeSpecialsCheckMissingMetadata;
  }

  public void clearEpisodeCheckArtwork() {
    episodeCheckArtwork.clear();
    firePropertyChange(EPISODE_CHECK_ARTWORK, null, episodeCheckArtwork);
  }

  public List<TvShowEpisodeScraperMetadataConfig> getEpisodeCheckArtwork() {
    return Collections.unmodifiableList(episodeCheckArtwork);
  }

  public void addEpisodeCheckArtwork(TvShowEpisodeScraperMetadataConfig config) {
    if (!episodeCheckArtwork.contains(config)) {
      episodeCheckArtwork.add(config);
      firePropertyChange(EPISODE_CHECK_ARTWORK, null, episodeCheckArtwork);
    }
  }

  public void setEpisodeDisplayAllMissingArtwork(boolean newValue) {
    boolean oldValue = episodeDisplayAllMissingArtwork;
    episodeDisplayAllMissingArtwork = newValue;
    firePropertyChange("episodeDisplayAllMissingArtwork", oldValue, newValue);
  }

  public boolean isEpisodeDisplayAllMissingArtwork() {
    return episodeDisplayAllMissingArtwork;
  }

  public void setEpisodeSpecialsCheckMissingArtwork(boolean newValue) {
    boolean oldValue = episodeSpecialsCheckMissingArtwork;
    episodeSpecialsCheckMissingArtwork = newValue;
    firePropertyChange("episodeSpecialsCheckMissingArtwork", oldValue, newValue);
  }

  public boolean isEpisodeSpecialsCheckMissingArtwork() {
    return episodeSpecialsCheckMissingArtwork;
  }

  public CertificationStyle getCertificationStyle() {
    return certificationStyle;
  }

  public void setCertificationStyle(CertificationStyle newValue) {
    CertificationStyle oldValue = this.certificationStyle;
    this.certificationStyle = newValue;
    firePropertyChange("certificationStyle", oldValue, newValue);
  }

  public TvShowConnectors getTvShowConnector() {
    return tvShowConnector;
  }

  public void setTvShowConnector(TvShowConnectors newValue) {
    TvShowConnectors oldValue = this.tvShowConnector;
    this.tvShowConnector = newValue;
    firePropertyChange("tvShowConnector", oldValue, newValue);
  }

  public boolean isWriteCleanNfo() {
    return writeCleanNfo;
  }

  public void setWriteCleanNfo(boolean newValue) {
    boolean oldValue = this.writeCleanNfo;
    this.writeCleanNfo = newValue;
    firePropertyChange("writeCleanNfo", oldValue, newValue);
  }

  public DateField getNfoDateAddedField() {
    return nfoDateAddedField;
  }

  public void setNfoDateAddedField(DateField newValue) {
    DateField oldValue = nfoDateAddedField;
    this.nfoDateAddedField = newValue;
    firePropertyChange("nfoDateAddedField", oldValue, newValue);
  }

  public Locale getNfoLanguage() {
    return nfoLanguage;
  }

  public void setNfoLanguage(Locale newValue) {
    Locale oldValue = nfoLanguage;
    this.nfoLanguage = newValue;
    firePropertyChange("nfoLanguage", oldValue, newValue);
  }

  public boolean isNfoWriteEpisodeguide() {
    return nfoWriteEpisodeguide;
  }

  public void setNfoWriteEpisodeguide(boolean newValue) {
    boolean oldValue = this.nfoWriteEpisodeguide;
    this.nfoWriteEpisodeguide = newValue;
    firePropertyChange("nfoWriteEpisodeguide", oldValue, newValue);
  }

  public boolean isNfoWriteNewEpisodeguideStyle() {
    return nfoWriteNewEpisodeguideStyle;
  }

  public void setNfoWriteNewEpisodeguideStyle(boolean newValue) {
    boolean oldValue = this.nfoWriteNewEpisodeguideStyle;
    this.nfoWriteNewEpisodeguideStyle = newValue;
    firePropertyChange("nfoWriteNewEpisodeguideStyle", oldValue, newValue);
  }

  public boolean isNfoWriteDateEnded() {
    return nfoWriteDateEnded;
  }

  public void setNfoWriteDateEnded(boolean newValue) {
    boolean oldValue = this.nfoWriteDateEnded;
    this.nfoWriteDateEnded = newValue;
    firePropertyChange("nfoWriteDateEnded", oldValue, newValue);
  }

  public boolean isNfoWriteAllActors() {
    return nfoWriteAllActors;
  }

  public void setNfoWriteAllActors(boolean newValue) {
    boolean oldValue = this.nfoWriteAllActors;
    this.nfoWriteAllActors = newValue;
    firePropertyChange("nfoWriteAllActors", oldValue, newValue);
  }

  public boolean isNfoWriteSingleStudio() {
    return nfoWriteSingleStudio;
  }

  public void setNfoWriteSingleStudio(boolean newValue) {
    boolean oldValue = nfoWriteSingleStudio;
    nfoWriteSingleStudio = newValue;
    firePropertyChange("nfoWriteSingleStudio", oldValue, newValue);
  }

  public boolean isNfoWriteLockdata() {
    return nfoWriteLockdata;
  }

  public void setNfoWriteLockdata(boolean newValue) {
    boolean oldValue = this.nfoWriteLockdata;
    this.nfoWriteLockdata = newValue;
    firePropertyChange("nfoWriteLockdata", oldValue, newValue);
  }

  public String getPreferredRating() {
    return preferredRating;
  }

  public void setPreferredRating(String newValue) {
    String oldValue = this.preferredRating;
    this.preferredRating = newValue;
    firePropertyChange("preferredRating", oldValue, newValue);
  }

  public boolean isWriteActorImages() {
    return writeActorImages;
  }

  public void setWriteActorImages(boolean newValue) {
    boolean oldValue = this.writeActorImages;
    this.writeActorImages = newValue;
    firePropertyChange("writeActorImages", oldValue, newValue);
  }

  public void setRenameAfterScrape(boolean newValue) {
    boolean oldValue = this.renameAfterScrape;
    this.renameAfterScrape = newValue;
    firePropertyChange("renameAfterScrape", oldValue, newValue);
  }

  public boolean isRenameAfterScrape() {
    return this.renameAfterScrape;
  }

  public int getImageExtraFanartCount() {
    return imageExtraFanartCount;
  }

  public void setImageExtraFanartCount(int newValue) {
    int oldValue = this.imageExtraFanartCount;
    this.imageExtraFanartCount = newValue;
    firePropertyChange("imageExtraFanartCount", oldValue, newValue);
  }

  public boolean isImageExtraFanart() {
    return imageExtraFanart;
  }

  public void setImageExtraFanart(boolean newValue) {
    boolean oldValue = this.imageExtraFanart;
    this.imageExtraFanart = newValue;
    firePropertyChange("imageExtraFanart", oldValue, newValue);
  }

  public boolean getCapitalWordsInTitles() {
    return capitalWordsinTitles;
  }

  public void setCapitalWordsInTitles(boolean newValue) {
    boolean oldValue = this.capitalWordsinTitles;
    this.capitalWordsinTitles = newValue;
    firePropertyChange("capitalWordsInTitles", oldValue, newValue);
  }

  public boolean isShowTvShowTableTooltips() {
    return showTvShowTableTooltips;
  }

  public void setShowTvShowTableTooltips(boolean newValue) {
    boolean oldValue = showTvShowTableTooltips;
    showTvShowTableTooltips = newValue;
    firePropertyChange("showTvShowTableTooltips", oldValue, newValue);
  }

  public boolean isSeasonArtworkFallback() {
    return seasonArtworkFallback;
  }

  public void setSeasonArtworkFallback(boolean newValue) {
    boolean oldValue = seasonArtworkFallback;
    seasonArtworkFallback = newValue;
    firePropertyChange("seasonArtworkFallback", oldValue, newValue);
  }

  public void setUiFilters(List<UIFilters> filters) {
    uiFilters.clear();
    uiFilters.addAll(filters);
    firePropertyChange("uiFilters", null, uiFilters);
  }

  public List<UIFilters> getUiFilters() {
    if (storeUiFilters) {
      return uiFilters;
    }
    return Collections.emptyList();
  }

  public void setStoreUiFilters(boolean newValue) {
    boolean oldValue = this.storeUiFilters;
    this.storeUiFilters = newValue;
    firePropertyChange("storeUiFilters", oldValue, newValue);
  }

  public boolean isStoreUiFilters() {
    return storeUiFilters;
  }

  public void addPostProcessTvShow(PostProcess newProcess) {
    postProcessTvShow.add(newProcess);
    firePropertyChange("postProcessTvShow", null, postProcessTvShow);
  }

  public void removePostProcessTvShow(PostProcess process) {
    postProcessTvShow.remove(process);
    firePropertyChange("postProcessTvShow", null, postProcessTvShow);
  }

  public List<PostProcess> getPostProcessTvShow() {
    return postProcessTvShow;
  }

  public void setPostProcessTvShow(List<PostProcess> newValues) {
    postProcessTvShow.clear();
    postProcessTvShow.addAll(newValues);
    firePropertyChange("postProcessTvShow", null, postProcessTvShow);
  }

  public void addPostProcessEpisode(PostProcess newProcess) {
    postProcessEpisode.add(newProcess);
    firePropertyChange("postProcessEpisode", null, postProcessEpisode);
  }

  public void removePostProcessEpisode(PostProcess process) {
    postProcessEpisode.remove(process);
    firePropertyChange("postProcessEpisode", null, postProcessEpisode);
  }

  public List<PostProcess> getPostProcessEpisode() {
    return postProcessEpisode;
  }

  public void setPostProcessEpisode(List<PostProcess> newValues) {
    postProcessEpisode.clear();
    postProcessEpisode.addAll(newValues);
    firePropertyChange("postProcessEpisode", null, postProcessEpisode);
  }

  public void setUniversalFilterFields(List<UniversalFilterFields> fields) {
    universalFilterFields.clear();
    universalFilterFields.addAll(fields);
    firePropertyChange("universalFilterFields", null, universalFilterFields);
  }

  public List<UniversalFilterFields> getUniversalFilterFields() {
    return universalFilterFields;
  }

  public boolean isDoNotOverwriteExistingData() {
    return doNotOverwriteExistingData;
  }

  public void setDoNotOverwriteExistingData(boolean newValue) {
    boolean oldValue = doNotOverwriteExistingData;
    doNotOverwriteExistingData = newValue;
    firePropertyChange("doNotOverwriteExistingData", oldValue, newValue);
  }

  public boolean isFetchAllRatings() {
    return fetchAllRatings;
  }

  public void setFetchAllRatings(boolean newValue) {
    boolean oldValue = this.fetchAllRatings;
    this.fetchAllRatings = newValue;
    firePropertyChange("fetchAllRatings", oldValue, newValue);
  }

  public void addShowTvShowArtworkTypes(MediaFileType type) {
    if (!showTvShowArtworkTypes.contains(type)) {
      showTvShowArtworkTypes.add(type);
      firePropertyChange("showTvShowArtworkTypes", null, showTvShowArtworkTypes);
    }
  }

  public void setShowTvShowArtworkTypes(List<MediaFileType> newTypes) {
    showTvShowArtworkTypes.clear();
    showTvShowArtworkTypes.addAll(newTypes);
    firePropertyChange("showTvShowArtworkTypes", null, showTvShowArtworkTypes);
  }

  public List<MediaFileType> getShowTvShowArtworkTypes() {
    return Collections.unmodifiableList(showTvShowArtworkTypes);
  }

  public void addShowSeasonArtworkTypes(MediaFileType type) {
    if (!showSeasonArtworkTypes.contains(type)) {
      showSeasonArtworkTypes.add(type);
      firePropertyChange("showSeasonArtworkTypes", null, showSeasonArtworkTypes);
    }
  }

  public void setShowSeasonArtworkTypes(List<MediaFileType> newTypes) {
    showSeasonArtworkTypes.clear();
    showSeasonArtworkTypes.addAll(newTypes);
    firePropertyChange("showSeasonArtworkTypes", null, showSeasonArtworkTypes);
  }

  public List<MediaFileType> getShowSeasonArtworkTypes() {
    return Collections.unmodifiableList(showSeasonArtworkTypes);
  }

  public void addShowEpisodeArtworkTypes(MediaFileType type) {
    if (!showEpisodeArtworkTypes.contains(type)) {
      showEpisodeArtworkTypes.add(type);
      firePropertyChange("showEpisodeArtworkTypes", null, showEpisodeArtworkTypes);
    }
  }

  public void setShowEpisodeArtworkTypes(List<MediaFileType> newTypes) {
    showEpisodeArtworkTypes.clear();
    showEpisodeArtworkTypes.addAll(newTypes);
    firePropertyChange("showEpisodeArtworkTypes", null, showEpisodeArtworkTypes);
  }

  public List<MediaFileType> getShowEpisodeArtworkTypes() {
    return Collections.unmodifiableList(showEpisodeArtworkTypes);
  }

  public boolean isResetNewFlagOnUds() {
    return resetNewFlagOnUds;
  }

  public void setResetNewFlagOnUds(boolean newValue) {
    boolean oldValue = this.resetNewFlagOnUds;
    this.resetNewFlagOnUds = newValue;
    firePropertyChange("resetNewFlagOnUds", oldValue, newValue);
  }

  public List<String> getRatingSources() {
    return ratingSources;
  }

  public void setRatingSources(List<String> newValue) {
    ratingSources.clear();
    ratingSources.addAll(newValue);
    firePropertyChange("ratingSources", null, ratingSources);
  }

  public void addRatingSource(String ratingSource) {
    if (!ratingSources.contains(ratingSource)) {
      ratingSources.add(ratingSource);
      firePropertyChange("ratingSources", null, ratingSources);
    }
  }

  public void removeRatingSource(String ratingSource) {
    if (ratingSources.remove(ratingSource)) {
      firePropertyChange("ratingSources", null, ratingSources);
    }
  }

  public void swapRatingSources(int pos1, int pos2) {
    String tmp = ratingSources.get(pos1);
    ratingSources.set(pos1, ratingSources.get(pos2));
    ratingSources.set(pos2, tmp);
    firePropertyChange("ratingSources", null, ratingSources);
  }

}
