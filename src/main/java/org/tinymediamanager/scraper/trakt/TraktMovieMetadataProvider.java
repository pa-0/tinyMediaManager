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
package org.tinymediamanager.scraper.trakt;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.PRODUCER;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.MediaMetadata.IMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TMDB;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaIdProvider;
import org.tinymediamanager.scraper.interfaces.IMovieImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IRatingProvider;
import org.tinymediamanager.scraper.tmdb.TmdbMovieArtworkProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;

import com.uwetrottmann.trakt5.entities.CastMember;
import com.uwetrottmann.trakt5.entities.Credits;
import com.uwetrottmann.trakt5.entities.CrewMember;
import com.uwetrottmann.trakt5.entities.Movie;
import com.uwetrottmann.trakt5.entities.MovieTranslation;
import com.uwetrottmann.trakt5.entities.SearchResult;
import com.uwetrottmann.trakt5.enums.Extended;

/**
 * The class TraktMovieMetadataProvider is used to provide metadata for movies from trakt.tv
 */

public class TraktMovieMetadataProvider extends TraktMetadataProvider
    implements IMovieMetadataProvider, IMovieImdbMetadataProvider, IRatingProvider, IMediaIdProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TraktMovieMetadataProvider.class);

  @Override
  protected String getSubId() {
    return "movie";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search() - {}", options);

    // lazy initialization of the api
    initAPI();

    String searchString = "";
    if (StringUtils.isEmpty(searchString) && StringUtils.isNotEmpty(options.getSearchQuery())) {
      searchString = options.getSearchQuery();
    }

    SortedSet<MediaSearchResult> results = new TreeSet<>();
    List<SearchResult> searchResults = null;

    // if we have a Trakt ID, try to get result direct - no need to search...
    String id = options.getIdAsString(MediaMetadata.TRAKT_TV);
    if (id != null) {
      try {
        MediaMetadata md = getMetadata(options);
        MediaSearchResult msr = new MediaSearchResult(id, options.getMediaType());
        msr.mergeFrom(md);
        results.add(msr);
        return results;
      }
      catch (Exception e) {
        LOGGER.error("Problem scraping for {} - {}", searchString, e.getMessage());
        // throw new ScrapeException(e); // continue
      }
    }

    // pass NO language here since trakt.tv returns less results when passing a language :(
    try {
      searchResults = executeCall(api.search().textQueryMovie(searchString, null, null, null, null, null, null, null, Extended.FULL, 1, 25));
    }
    catch (Exception e) {
      LOGGER.error("Problem scraping for {} - {}", searchString, e.getMessage());
      throw new ScrapeException(e);
    }

    if (searchResults == null || searchResults.isEmpty()) {
      LOGGER.info("nothing found");
      return results;
    }

    for (SearchResult result : searchResults) {
      MediaSearchResult m = TraktUtils.morphTraktResultToTmmResult(options, result);

      // calculate score
      if ((StringUtils.isNotBlank(options.getImdbId()) && options.getImdbId().equals(m.getIMDBId()))
          || String.valueOf(options.getTmdbId()).equals(m.getId()) || (id != null && id.equals(m.getId()))) {
        LOGGER.debug("perfect match by ID - set score to 1");
        m.setScore(1);
      }
      else {
        // calculate the score by comparing the search result with the search options
        m.calculateScore(options);
      }

      results.add(m);
    }

    return results;
  }

  @Override
  public MediaMetadata getMetadata(MovieSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    // lazy initialization of the api
    initAPI();

    MediaMetadata md = new MediaMetadata(getId());
    md.setScrapeOptions(options);

    String id = options.getIdAsString(getId());

    // alternatively we can take the imdbid
    if (StringUtils.isBlank(id)) {
      id = options.getIdAsString(IMDB);
    }

    if (StringUtils.isBlank(id)) {
      LOGGER.debug("no id available");
      throw new MissingIdException(MediaMetadata.IMDB, getId());
    }

    // scrape
    LOGGER.debug("Trakt.tv: getMetadata: id = {}", id);

    String lang = options.getLanguage().getLanguage();
    List<MovieTranslation> translations = null;

    Movie movie = null;
    Credits credits = null;
    try {
      movie = executeCall(api.movies().summary(id, Extended.FULL));
      if (!"en".equals(lang)) {
        // only call translation when we're not already EN ;)
        translations = api.movies().translation(id, lang).execute().body();
      }
      credits = api.movies().people(id).execute().body();
    }
    catch (Exception e) {
      LOGGER.debug("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (movie == null) {
      LOGGER.warn("nothing found");
      throw new NothingFoundException();
    }

    // if foreign language, get new values and overwrite
    MovieTranslation trans = translations == null || translations.isEmpty() ? null : translations.get(0);
    if (trans != null) {
      md.setTitle(StringUtils.isBlank(trans.title) ? movie.title : trans.title);
      md.setTagline(StringUtils.isBlank(trans.tagline) ? movie.tagline : trans.tagline);
      md.setPlot(StringUtils.isBlank(trans.overview) ? movie.overview : trans.overview);
    }
    else {
      md.setTitle(movie.title);
      md.setTagline(movie.tagline);
      md.setPlot(movie.overview);
    }

    md.setYear(movie.year);
    md.setRuntime(movie.runtime);
    md.addCertification(MediaCertification.findCertification(movie.certification));
    md.setReleaseDate(TraktUtils.toDate(movie.released));

    if (movie.rating != null && movie.votes != null) {
      try {
        MediaRating rating = new MediaRating(MediaMetadata.TRAKT_TV);
        rating.setRating(Math.round(movie.rating * 10.0) / 10.0); // hack to round to 1 decimal
        rating.setVotes(movie.votes);
        rating.setMaxValue(10);
        md.addRating(rating);
      }
      catch (Exception e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }
    }

    // ids
    if (movie.ids != null) {
      md.setId(getId(), movie.ids.trakt);
      if (movie.ids.tmdb != null && movie.ids.tmdb > 0) {
        md.setId(TMDB, movie.ids.tmdb);
      }
      if (MediaIdUtil.isValidImdbId(movie.ids.imdb)) {
        md.setId(IMDB, movie.ids.imdb);
      }
    }

    for (String genreAsString : ListUtils.nullSafe(movie.genres)) {
      md.addGenre(MediaGenres.getGenre(genreAsString));
    }

    // cast&crew
    if (credits != null) {
      for (CastMember cast : ListUtils.nullSafe(credits.cast)) {
        md.addCastMember(TraktUtils.toTmmCast(cast, ACTOR));
      }
      if (credits.crew != null) {
        for (CrewMember crew : ListUtils.nullSafe(credits.crew.directing)) {
          md.addCastMember(TraktUtils.toTmmCast(crew, DIRECTOR));
        }
        for (CrewMember crew : ListUtils.nullSafe(credits.crew.production)) {
          md.addCastMember(TraktUtils.toTmmCast(crew, PRODUCER));
        }
        for (CrewMember crew : ListUtils.nullSafe(credits.crew.writing)) {
          md.addCastMember(TraktUtils.toTmmCast(crew, WRITER));
        }
      }
    }

    // Trakt API has no images, and they state to search any of theirs IDs provider.
    // So try to get the poster url from tmdb
    TmdbMovieArtworkProvider tmdb = new TmdbMovieArtworkProvider();
    if (tmdb.isActive() && (MediaIdUtil.isValidImdbId(movie.ids.imdb) || movie.ids.tmdb > 0)) {
      try {
        ArtworkSearchAndScrapeOptions tmdbOptions = new ArtworkSearchAndScrapeOptions(options.getMediaType());
        tmdbOptions.setImdbId(movie.ids.imdb);
        tmdbOptions.setTmdbId(movie.ids.tmdb);
        tmdbOptions.setLanguage(options.getLanguage());
        tmdbOptions.setArtworkType(MediaArtwork.MediaArtworkType.POSTER);
        List<MediaArtwork> artworks = tmdb.getArtwork(tmdbOptions);
        if (ListUtils.isNotEmpty(artworks)) {
          md.addMediaArt(artworks.get(0));
        }
      }
      catch (Exception e) {
        LOGGER.warn("Could not get artwork from tmdb - {}", e.getMessage());
      }
    }

    return md;
  }

  @Override
  public List<MediaRating> getRatings(Map<String, Object> ids, MediaType mediaType) throws ScrapeException {
    if (mediaType != MediaType.MOVIE) {
      return Collections.emptyList();
    }

    LOGGER.debug("getRatings(): {}", ids);

    // lazy initialization of the api
    initAPI();

    String id = MediaIdUtil.getIdAsString(ids, getId());

    // alternatively we can take the imdbid
    if (StringUtils.isBlank(id)) {
      id = MediaIdUtil.getIdAsString(ids, IMDB);
    }

    if (StringUtils.isBlank(id)) {
      LOGGER.debug("no id available");
      throw new MissingIdException(MediaMetadata.IMDB, getId());
    }

    // scrape
    LOGGER.debug("Trakt.tv: getMetadata: id = {}", id);

    Movie movie;
    try {
      movie = executeCall(api.movies().summary(id, Extended.FULL));
    }
    catch (Exception e) {
      LOGGER.debug("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (movie == null) {
      LOGGER.debug("nothing found");
      throw new NothingFoundException();
    }

    if (movie.rating != null && movie.votes != null) {
      try {
        MediaRating rating = new MediaRating(MediaMetadata.TRAKT_TV);
        rating.setRating(Math.round(movie.rating * 10.0) / 10.0); // hack to round to 1 decimal
        rating.setVotes(movie.votes);
        rating.setMaxValue(10);
        return Collections.singletonList(rating);
      }
      catch (Exception e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }
    }

    return Collections.emptyList();
  }

  @Override
  public Map<String, Object> getMediaIds(Map<String, Object> ids, MediaType mediaType) throws ScrapeException {
    if (mediaType != MediaType.MOVIE) {
      return Collections.emptyMap();
    }

    LOGGER.debug("getMediaIds: {}", ids);

    // lazy initialization of the api
    initAPI();

    // we can get other ids via IMDB or Trakt.tv ID
    String id = MediaIdUtil.getIdAsString(ids, getId());

    // alternatively we can take the imdbid
    if (StringUtils.isBlank(id)) {
      id = MediaIdUtil.getIdAsString(ids, IMDB);
    }

    if (StringUtils.isBlank(id)) {
      LOGGER.debug("neither trakt.tv nor imdb available");
      throw new MissingIdException(MediaMetadata.IMDB, getId());
    }

    Movie movie = null;
    try {
      movie = executeCall(api.movies().summary(id, Extended.FULL));
    }
    catch (Exception e) {
      LOGGER.debug("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (movie == null) {
      LOGGER.warn("nothing found");
      throw new NothingFoundException();
    }

    // ids
    Map<String, Object> scrapedIds = new HashMap<>();

    if (movie.ids != null) {
      scrapedIds.put(getId(), movie.ids.trakt);
      if (movie.ids.tmdb != null && movie.ids.tmdb > 0) {
        scrapedIds.put(TMDB, movie.ids.tmdb);
      }
      if (MediaIdUtil.isValidImdbId(movie.ids.imdb)) {
        scrapedIds.put(IMDB, movie.ids.imdb);
      }
    }
    return scrapedIds;
  }
}
