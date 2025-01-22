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
package org.tinymediamanager.scraper.rating;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.interfaces.IRatingProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;

/**
 * the class {@link RatingProvider} is a utility to have quick access to several various ratings
 * 
 * @author Manuel Laggner
 */
public class RatingProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(RatingProvider.class);

  public enum RatingSource {
    IMDB("IMDb"),
    TMDB("TMDB"),
    METACRITIC("Metacritic Metascore"),
    METACRITIC_USER("Metacritic Userscore"),
    ROTTEN_TOMATOES_TOMATOMETER("Rotten Tomatoes - Tomatometer"),
    ROTTEN_TOMATOES_AVG_RATING("Rotten Tomatoes - Audience Score"),
    TRAKT_TV("Trakt.tv"),
    LETTERBOXD("Letterboxd"),
    MAL("MyAnimeList"),
    ROGER_EBERT("RogerEbert.com");

    private final String title;

    RatingSource(String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }

    /**
     * get all relevant {@link RatingSource}s for movies
     * 
     * @return a {@link List} of all relevant {@link RatingSource}s
     */
    public static List<RatingSource> getRatingSourcesForMovies() {
      return Arrays.asList(values());
    }

    /**
     * get all relevant {@link RatingSource}s for TV shows
     * 
     * @return a {@link List} of all relevant {@link RatingSource}s
     */
    public static List<RatingSource> getRatingSourcesForTvShows() {
      List<RatingSource> ratingSources = new ArrayList<>();

      for (RatingSource ratingSource : values()) {
        if (ratingSource == METACRITIC || ratingSource == ROTTEN_TOMATOES_AVG_RATING || ratingSource == ROTTEN_TOMATOES_TOMATOMETER) {
          continue;
        }
        ratingSources.add(ratingSource);
      }

      return ratingSources;
    }
  }

  private RatingProvider() {
    throw new IllegalAccessError();
  }

  /**
   * get the IMDb rating
   * 
   * @param imdbId
   *          the IMDb ID
   * @return the {@link MediaRating} from IMDb
   */
  public static MediaRating getImdbRating(String imdbId) {
    List<MediaRating> ratings = getRatings(Collections.singletonMap(MediaMetadata.IMDB, imdbId), Collections.singletonList(RatingSource.IMDB), null);
    if (!ratings.isEmpty()) {
      return ratings.get(0);
    }
    return null;
  }

  public static List<MediaRating> getRatingsFromMdbList(Map<String, Object> ids, MediaType mediaType) {
    return new MdbListRating().getRatings(mediaType, ids);
  }

  /**
   * get a {@link List} of all supported rating sources
   * 
   * @param ids
   *          the IDs to get the {@link MediaRating}s for
   * @param mediaType
   *          the {@link MediaType}
   * @return a {@link List} of all found {@link MediaRating}s
   */
  public static List<MediaRating> getRatings(Map<String, Object> ids, MediaType mediaType) {
    return getRatings(ids, Arrays.asList(RatingSource.values()), mediaType);
  }

  /**
   * get a {@link List} of all supported ratings from various sources for
   *
   * @param ids
   *          the IDs to get the {@link MediaRating}s for
   * @param sources
   *          the {@link RatingSource}s to get the ratings for
   * @param mediaType
   *          the {@link MediaType}
   * @return a {@link List} of all found {@link MediaRating}s
   */
  public static List<MediaRating> getRatings(Map<String, Object> ids, List<RatingSource> sources, MediaType mediaType) {
    List<MediaRating> ratings = new ArrayList<>();
    List<RatingSource> missingRatings = new ArrayList<>(sources);

    String imdbId = MediaIdUtil.getIdAsString(ids, MediaMetadata.IMDB);

    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      // no valid imdb id here - try to fetch all relevant ids via trakt.tv
      MediaIdUtil.injectMissingIds(ids, mediaType);
    }

    // IMDB comes directly from the IMDB data dump
    if (missingRatings.contains(RatingSource.IMDB) && MediaIdUtil.isValidImdbId(imdbId)) {
      MediaRating rating = new ImdbRating().getImdbRating(imdbId);
      if (rating != null) {
        ratings.add(rating);
        missingRatings.remove(RatingSource.IMDB);
      }
    }

    // TMDB rating comes directly from TMDB
    if (missingRatings.contains(RatingSource.TMDB)) {
      callScraper(MediaMetadata.TMDB, mediaType, missingRatings, ids, ratings);
    }

    // Trakt.tv rating comes directly form Trakt.tv
    if (missingRatings.contains(RatingSource.TRAKT_TV)) {
      callScraper(MediaMetadata.TRAKT_TV, mediaType, missingRatings, ids, ratings);
    }

    // MdbList
    if (ListUtils.containsAny(missingRatings, RatingSource.METACRITIC, RatingSource.ROTTEN_TOMATOES_AVG_RATING,
        RatingSource.ROTTEN_TOMATOES_AVG_RATING, RatingSource.LETTERBOXD, RatingSource.MAL, RatingSource.ROGER_EBERT)) {
      List<MediaRating> ratingsFromMdblist = new MdbListRating().getRatings(mediaType, ids);
      for (MediaRating rating : ratingsFromMdblist) {
        RatingSource source = parseRatingSource(rating.getId());
        if (missingRatings.contains(source) && !ratings.contains(rating)) {
          ratings.add(rating);
          missingRatings.remove(source);
        }
      }
    }

    // Metacritic can be fetched from the IMDB scraper
    if (missingRatings.contains(RatingSource.METACRITIC)) {
      callScraper(MediaMetadata.IMDB, mediaType, missingRatings, ids, ratings);
    }

    // OMDB offers Rotten Tomatoes
    if (ListUtils.containsAny(missingRatings, RatingSource.ROTTEN_TOMATOES_AVG_RATING, RatingSource.ROTTEN_TOMATOES_AVG_RATING)
        && MediaIdUtil.isValidImdbId(imdbId)) {
      callScraper("omdbapi", mediaType, missingRatings, ids, ratings);
    }

    // Wikidata offers Metacritic, Rotten Tomatoes and IMDB
    if (ListUtils.containsAny(missingRatings, RatingSource.METACRITIC, RatingSource.ROTTEN_TOMATOES_AVG_RATING,
        RatingSource.ROTTEN_TOMATOES_AVG_RATING) && MediaIdUtil.isValidImdbId(imdbId)) {
      List<MediaRating> ratingsFromWikidata = new WikidataRating().getRatings(imdbId);

      for (MediaRating rating : ratingsFromWikidata) {
        RatingSource source = parseRatingSource(rating.getId());
        if (missingRatings.contains(source) && !ratings.contains(rating)) {
          ratings.add(rating);
          missingRatings.remove(source);
        }
      }
    }

    return ratings;
  }

  private static void callScraper(String scraperId, MediaType mediaType, List<RatingSource> sources, Map<String, Object> ids,
      List<MediaRating> ratings) {
    MediaScraper scraper = MediaScraper.getMediaScraperById(scraperId, MediaType.getScraperTypeForMediaType(mediaType));
    if (scraper != null && scraper.getMediaProvider() instanceof IRatingProvider ratingProvider && scraper.isEnabled()) {
      try {
        List<MediaRating> ratingsFromScraper = ratingProvider.getRatings(ids, mediaType);

        for (MediaRating rating : ratingsFromScraper) {
          RatingSource source = parseRatingSource(rating.getId());
          if (sources.contains(source) && !ratings.contains(rating)) {
            ratings.add(rating);
            sources.remove(source);
          }
        }
      }
      catch (Exception e) {
        LOGGER.debug("could not get {} ratings - '{}'", scraperId, e.getMessage());
      }
    }
  }

  /**
   * shutdown of the internal caches
   */
  public static synchronized void shutdown() {
    ImdbRating.shutdown();
  }

  /**
   * get the {@link RatingSource} from the given {@link String}
   * 
   * @param id
   *          the {@link String} to get the {@link RatingSource} for
   * @return the corresponding {@link RatingSource} or null
   */
  public static RatingSource parseRatingSource(String id) {
    if (StringUtils.isBlank(id)) {
      return null;
    }

    return switch (id) {
      case MediaMetadata.METACRITIC -> RatingSource.METACRITIC;
      case MediaMetadata.METACRITIC_USER -> RatingSource.METACRITIC_USER;
      case MediaMetadata.IMDB -> RatingSource.IMDB;
      case MediaMetadata.TMDB -> RatingSource.TMDB;
      case "tomatometerallcritics" -> RatingSource.ROTTEN_TOMATOES_TOMATOMETER;
      case "tomatometeravgcritics" -> RatingSource.ROTTEN_TOMATOES_AVG_RATING;
      case MediaMetadata.TRAKT_TV -> RatingSource.TRAKT_TV;
      case MediaMetadata.LETTERBOXD -> RatingSource.LETTERBOXD;
      case MediaMetadata.MY_ANIME_LIST -> RatingSource.MAL;
      case MediaMetadata.ROGER_EBERT -> RatingSource.ROGER_EBERT;
      default -> null;
    };
  }

  /**
   * get the string representation for the given {@link RatingSource}
   * 
   * @param ratingSource
   *          the {@link RatingSource} to get the string representation for
   * @return the string representation
   */
  public static String getRatingSourceId(@Nonnull RatingSource ratingSource) {
    return switch (ratingSource) {
      case IMDB -> MediaMetadata.IMDB;
      case TMDB -> MediaMetadata.TMDB;
      case METACRITIC -> MediaMetadata.METACRITIC;
      case METACRITIC_USER -> MediaMetadata.METACRITIC_USER;
      case ROTTEN_TOMATOES_TOMATOMETER -> "tomatometerallcritics";
      case ROTTEN_TOMATOES_AVG_RATING -> "tomatometeravgcritics";
      case TRAKT_TV -> MediaMetadata.TRAKT_TV;
      case LETTERBOXD -> MediaMetadata.LETTERBOXD;
      case MAL -> MediaMetadata.MY_ANIME_LIST;
      case ROGER_EBERT -> MediaMetadata.ROGER_EBERT;
    };
  }
}
