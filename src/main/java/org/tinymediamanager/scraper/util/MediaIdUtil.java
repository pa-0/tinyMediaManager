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
package org.tinymediamanager.scraper.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviders;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaIdProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;

/**
 * The class MediaIdUtil is a helper class for managing ids
 *
 * @author Manuel Laggner
 */
public class MediaIdUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(MediaIdUtil.class);

  private MediaIdUtil() {
    throw new IllegalAccessError();
  }

  /**
   * get the imdb id from thetvdb by a given tvdb id
   * 
   * @param tvdbId
   *          the tvdb id
   * @return the imdb id or an empty string
   */
  public static String getImdbIdFromTvdbId(String tvdbId) {
    if (StringUtils.isBlank(tvdbId)) {
      return "";
    }

    String imdbId = "";
    try {
      MediaScraper scraper = MediaScraper.getMediaScraperById(MediaMetadata.TVDB, ScraperType.TV_SHOW);
      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setId(MediaMetadata.TVDB, tvdbId);
      MediaMetadata md = ((ITvShowMetadataProvider) scraper.getMediaProvider()).getMetadata(options);
      imdbId = (String) md.getId(MediaMetadata.IMDB);
    }
    catch (Exception e) {
      LOGGER.error("could not get imdb id from tvdb id: {}", e.getMessage());
    }

    if (StringUtils.isBlank(imdbId)) {
      return ""; // do not pass null
    }

    return imdbId;
  }

  /**
   * gets the movie imdb id via tmdb id
   *
   * @param tmdbId
   *          the tmdb id
   * @return the imdb id or an empty String
   */
  public static String getMovieImdbIdViaTmdbId(int tmdbId) {
    if (tmdbId == 0) {
      return "";
    }

    try {
      // call the tmdb metadata provider
      IMovieMetadataProvider tmdb = MediaProviders.getProviderById(MediaMetadata.TMDB, IMovieMetadataProvider.class);
      if (tmdb == null) {
        return "";
      }

      // we just need to "scrape" this movie
      MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
      options.setId(MediaMetadata.TMDB, Integer.toString(tmdbId));
      MediaMetadata md = tmdb.getMetadata(options);
      return md.getId(MediaMetadata.IMDB).toString();
    }
    catch (Exception ignored) {
      // nothing to be done here
    }

    return "";
  }

  /**
   * gets the TV show imdb id via tmdb id
   *
   * @param tmdbId
   *          the tmdb id
   * @return the imdb id or an empty String
   */
  public static String getTvShowImdbIdViaTmdbId(int tmdbId) {
    if (tmdbId == 0) {
      return "";
    }

    try {
      // call the tmdb metadata provider
      ITvShowMetadataProvider tmdb = MediaProviders.getProviderById(MediaMetadata.TMDB, ITvShowMetadataProvider.class);
      if (tmdb == null) {
        return "";
      }

      // we just need to "scrape" this movie
      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setId(MediaMetadata.TMDB, Integer.toString(tmdbId));
      MediaMetadata md = tmdb.getMetadata(options);
      return md.getId(MediaMetadata.IMDB).toString();
    }
    catch (Exception ignored) {
      // nothing to be done here
    }

    return "";
  }

  /**
   * inject missing ids into the given {@link Map} of ids for the given {@link MediaType}
   * 
   * @param ids
   *          the {@link Map} to fill
   * @param mediaType
   *          the {@link MediaType}
   */
  public static void injectMissingIds(Map<String, Object> ids, MediaType mediaType) {
    if (mediaType == null) {
      return;
    }

    switch (mediaType) {
      case MOVIE:
        injectMovieIds(ids);
        break;

      case TV_SHOW:
        injectTvShowIds(ids);
        break;

      case TV_EPISODE:
        injectEpisodeIds(ids);
        break;

      default:
        break;
    }
  }

  /**
   * inject movie ids
   * 
   * @param ids
   *          the {@link Map} to fill
   */
  private static void injectMovieIds(Map<String, Object> ids) {
    List<String> missingIds = getMissingMovieIds(ids);

    if (missingIds.isEmpty()) {
      // all good - we cannot get more (yet)
      return;
    }

    // we can get the ids from different providers.
    // imdb/tmdb can be fetched (vice versa) via tmdb
    if (!missingIds.contains(MediaMetadata.IMDB) || !missingIds.contains(MediaMetadata.TMDB)) {
      callScraper(MediaMetadata.TMDB, MediaType.MOVIE, ids);

      // re-evaluate the missing ids
      missingIds = getMissingMovieIds(ids);
      if (missingIds.isEmpty()) {
        // all good - we cannot get more (yet)
        return;
      }
    }

    // // call trakt.tv to get the trakt.tv id
    // if (!missingIds.contains(MediaMetadata.IMDB) && missingIds.contains(MediaMetadata.TRAKT_TV)) {
    // callScraper(MediaMetadata.TRAKT_TV, MediaType.MOVIE, ids);
    //
    // // re-evaluate the missing ids
    // missingIds = getMissingMovieIds(ids);
    // if (missingIds.isEmpty()) {
    // // all good - we cannot get more (yet)
    // return;
    // }
    // }
  }

  private static List<String> getMissingMovieIds(Map<String, Object> ids) {
    // get a list of missing, well known ids
    List<String> missingIds = new ArrayList<>();

    if (!isValidImdbId(getIdAsString(ids, MediaMetadata.IMDB))) {
      missingIds.add(MediaMetadata.IMDB);
    }
    if (getIdAsInt(ids, MediaMetadata.TMDB) <= 0) {
      missingIds.add(MediaMetadata.TMDB);
    }
    // if (getIdAsInt(ids, MediaMetadata.TRAKT_TV) <= 0) {
    // missingIds.add(MediaMetadata.TRAKT_TV);
    // }

    return missingIds;
  }

  private static void injectTvShowIds(Map<String, Object> ids) {
    List<String> missingIds = getMissingTvShowIds(ids);

    if (missingIds.isEmpty()) {
      // all good - we cannot get more (yet)
      return;
    }

    // we can get the ids from different providers.
    // tvdb can get tvdb, imdb and tmdb
    if (!missingIds.contains(MediaMetadata.IMDB) || !missingIds.contains(MediaMetadata.TVDB)) {
      callScraper(MediaMetadata.TVDB, MediaType.TV_SHOW, ids);

      // re-evaluate the missing ids
      missingIds = getMissingTvShowIds(ids);
      if (missingIds.isEmpty()) {
        // all good - we cannot get more (yet)
        return;
      }
    }

    // imdb/tmdb can be fetched (vice versa) via tmdb
    if (!missingIds.contains(MediaMetadata.IMDB) || !missingIds.contains(MediaMetadata.TMDB)) {
      callScraper(MediaMetadata.TMDB, MediaType.TV_SHOW, ids);

      // re-evaluate the missing ids
      missingIds = getMissingTvShowIds(ids);
      if (missingIds.isEmpty()) {
        // all good - we cannot get more (yet)
        return;
      }
    }

    // // trakt.tv will get the rest
    // if (!missingIds.contains(MediaMetadata.IMDB) || !missingIds.contains(MediaMetadata.TRAKT_TV)) {
    // callScraper(MediaMetadata.TRAKT_TV, MediaType.TV_SHOW, ids);
    //
    // // re-evaluate the missing ids
    // missingIds = getMissingTvShowIds(ids);
    // if (missingIds.isEmpty()) {
    // // all good - we cannot get more (yet)
    // return;
    // }
    // }
  }

  private static List<String> getMissingTvShowIds(Map<String, Object> ids) {
    // get a list of missing, well known ids
    List<String> missingIds = new ArrayList<>();

    if (!isValidImdbId(getIdAsString(ids, MediaMetadata.IMDB))) {
      missingIds.add(MediaMetadata.IMDB);
    }
    if (getIdAsInt(ids, MediaMetadata.TMDB) <= 0) {
      missingIds.add(MediaMetadata.TMDB);
    }
    if (getIdAsInt(ids, MediaMetadata.TRAKT_TV) <= 0) {
      missingIds.add(MediaMetadata.TRAKT_TV);
    }
    if (getIdAsInt(ids, MediaMetadata.TVDB) <= 0) {
      missingIds.add(MediaMetadata.TVDB);
    }

    return missingIds;
  }

  private static void injectEpisodeIds(Map<String, Object> ids) {
    Map<String, Object> showIds = new HashMap<>();
    try {
      showIds.putAll((Map<? extends String, ?>) ids.get(MediaMetadata.TVSHOW_IDS));
    }
    catch (Exception e) {
      // ignored
    }

    if (showIds.isEmpty()) {
      // without show ids we cannot do more here
      return;
    }

    List<String> missingTvShowIds = getMissingTvShowIds(showIds);
    // we need at least the imdb id from the show
    if (missingTvShowIds.contains(MediaMetadata.IMDB)) {
      return;
    }

    List<String> missingEpisodeIds = getMissingTvShowIds(ids);

    if (missingEpisodeIds.isEmpty()) {
      // all good - we cannot get more (yet)
      return;
    }

    // trakt.tv will get all needed ids
    if (!missingTvShowIds.contains(MediaMetadata.IMDB) || !missingTvShowIds.contains(MediaMetadata.TRAKT_TV)) {
      callScraper(MediaMetadata.TRAKT_TV, MediaType.TV_EPISODE, ids);

      // re-evaluate the missing ids
      missingEpisodeIds = getMissingTvShowIds(ids);
      if (missingEpisodeIds.isEmpty()) {
        // all good - we cannot get more (yet)
        return;
      }
    }
  }

  private static void callScraper(String scraperId, MediaType mediaType, Map<String, Object> ids) {
    MediaScraper scraper = MediaScraper.getMediaScraperById(scraperId, MediaType.getScraperTypeForMediaType(mediaType));
    if (scraper != null && scraper.isEnabled() && scraper.getMediaProvider() instanceof IMediaIdProvider) {
      try {
        Map<String, Object> idsFromScraper = ((IMediaIdProvider) scraper.getMediaProvider()).getMediaIds(ids, mediaType);
        idsFromScraper.forEach(ids::putIfAbsent);
      }
      catch (ScrapeException ignored) {
        // we already logged that in the scraper
      }
      catch (Exception e) {
        LOGGER.debug("could not get {} ratings - '{}'", scraperId, e.getMessage());
      }
    }
  }

  /**
   * Checks if is valid imdb id.
   * 
   * @param imdbId
   *          the imdb id
   * @return true, if is valid imdb id
   */
  public static boolean isValidImdbId(String imdbId) {
    if (StringUtils.isBlank(imdbId)) {
      return false;
    }

    return imdbId.matches("tt\\d{6,}");
  }

  /**
   * any ID as String or empty
   *
   * @return the ID-value as String or an empty string
   */
  public static String getIdAsString(Map<String, Object> ids, String key) {
    if (ids == null) {
      return "";
    }

    Object obj = ids.get(key);
    if (obj == null) {
      return "";
    }
    return String.valueOf(obj);
  }

  /**
   * any ID as int or 0
   * 
   * @param ids
   *          a {@link Map} of all available IDs
   * @param key
   *          the provider ID
   * 
   * @return the ID-value as int or 0
   */
  public static int getIdAsInt(Map<String, Object> ids, String key) {
    return getIdAsIntOrDefault(ids, key, 0);
  }

  /**
   * any ID as int or the default value
   * 
   * @param ids
   *          a {@link Map} of all available IDs
   * @param key
   *          the provider ID
   * @param defaultValue
   *          the default value to return
   *
   * @return the ID-value as int or the default value
   */
  public static int getIdAsIntOrDefault(Map<String, Object> ids, String key, int defaultValue) {
    if (ids == null) {
      return defaultValue;
    }

    Object obj = ids.get(key);
    if (obj == null) {
      return defaultValue;
    }
    if (obj instanceof Integer) {
      return (Integer) obj;
    }

    if (obj instanceof String) {
      try {
        return Integer.parseInt((String) obj);
      }
      catch (Exception e) {
        LOGGER.trace("could not parse int: {}", e.getMessage());
      }
    }

    return defaultValue;
  }
}
