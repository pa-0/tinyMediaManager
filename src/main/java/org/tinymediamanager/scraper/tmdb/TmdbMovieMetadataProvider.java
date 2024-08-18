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
package org.tinymediamanager.scraper.tmdb;

import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.PRODUCER;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.MediaMetadata.IMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TMDB_SET;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.MovieSetSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaIdProvider;
import org.tinymediamanager.scraper.interfaces.IMovieImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieSetMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieTmdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IRatingProvider;
import org.tinymediamanager.scraper.tmdb.entities.AppendToResponse;
import org.tinymediamanager.scraper.tmdb.entities.BaseCollection;
import org.tinymediamanager.scraper.tmdb.entities.BaseCompany;
import org.tinymediamanager.scraper.tmdb.entities.BaseKeyword;
import org.tinymediamanager.scraper.tmdb.entities.BaseMovie;
import org.tinymediamanager.scraper.tmdb.entities.CastMember;
import org.tinymediamanager.scraper.tmdb.entities.Collection;
import org.tinymediamanager.scraper.tmdb.entities.CollectionResultsPage;
import org.tinymediamanager.scraper.tmdb.entities.Country;
import org.tinymediamanager.scraper.tmdb.entities.CrewMember;
import org.tinymediamanager.scraper.tmdb.entities.FindResults;
import org.tinymediamanager.scraper.tmdb.entities.Genre;
import org.tinymediamanager.scraper.tmdb.entities.Movie;
import org.tinymediamanager.scraper.tmdb.entities.MovieResultsPage;
import org.tinymediamanager.scraper.tmdb.entities.ReleaseDate;
import org.tinymediamanager.scraper.tmdb.entities.ReleaseDatesResult;
import org.tinymediamanager.scraper.tmdb.entities.SpokenLanguage;
import org.tinymediamanager.scraper.tmdb.enumerations.AppendToResponseItem;
import org.tinymediamanager.scraper.tmdb.enumerations.ExternalSource;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;

import retrofit2.Response;

/**
 * The class {@link TmdbMovieMetadataProvider} is used to provide metadata for movies and movie sets from tmdb
 *
 * @author Manuel Laggner
 */
public class TmdbMovieMetadataProvider extends TmdbMetadataProvider implements IMovieMetadataProvider, IMovieSetMetadataProvider,
    IMovieTmdbMetadataProvider, IMovieImdbMetadataProvider, IRatingProvider, IMediaIdProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TmdbMovieMetadataProvider.class);

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = super.createMediaProviderInfo();

    info.getConfig().addBoolean("includeAdult", false);
    info.getConfig().addBoolean("scrapeLanguageNames", true);
    info.getConfig().addBoolean("titleFallback", false);
    info.getConfig().addSelect("titleFallbackLanguage", PT, "en-US");
    info.getConfig().addBoolean("localReleaseDate", true);
    info.getConfig().addBoolean("includePremiereDate", true);
    info.getConfig().load();

    return info;
  }

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
    LOGGER.debug("search(): {}", options);

    // lazy initialization of the api
    initAPI();

    Exception savedException = null;

    SortedSet<MediaSearchResult> results = new TreeSet<>();

    // detect the string to search
    String searchString = "";
    if (StringUtils.isNotEmpty(options.getSearchQuery())) {
      searchString = Utils.removeSortableName(options.getSearchQuery());
    }
    searchString = MetadataUtil.removeNonSearchCharacters(searchString);

    String imdbId = options.getImdbId();
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      imdbId = "";
    }
    if (MediaIdUtil.isValidImdbId(searchString)) {
      imdbId = searchString;
    }

    int tmdbId = options.getTmdbId();

    boolean adult = getProviderInfo().getConfig().getValueAsBool("includeAdult");

    String language = getRequestLanguage(options.getLanguage());

    // begin search
    LOGGER.info("========= BEGIN TMDB Scraper Search for: {}", searchString);

    // 1. try with TMDBid
    if (tmdbId != 0) {
      LOGGER.debug("found TMDB ID {} - getting direct", tmdbId);
      try {
        Response<Movie> httpResponse = api.moviesService()
            .summary(tmdbId, language, new AppendToResponse(AppendToResponseItem.TRANSLATIONS))
            .execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        Movie movie = httpResponse.body();
        injectTranslations(Locale.forLanguageTag(language), movie);
        MediaSearchResult result = morphMovieToSearchResult(movie, options);
        results.add(result);
        LOGGER.debug("found {} results with TMDB id", results.size());
      }
      catch (Exception e) {
        LOGGER.warn("problem getting data from tmdb: {}", e.getMessage());
        savedException = e;
      }
    }

    // 2. try with IMDBid
    if (results.isEmpty() && StringUtils.isNotEmpty(imdbId)) {
      LOGGER.debug("found IMDB ID {} - getting direct", imdbId);
      try {
        Response<FindResults> httpResponse = api.findService().find(imdbId, ExternalSource.IMDB_ID, language).execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        for (BaseMovie movie : ListUtils.nullSafe(httpResponse.body().movie_results)) { // should be only one
          results.add(morphMovieToSearchResult(movie, options));
        }
        LOGGER.debug("found {} results with IMDB id", results.size());
      }
      catch (Exception e) {
        LOGGER.warn("problem getting data from tmdb: {}", e.getMessage());
        savedException = e;
      }
    }

    // 3. try with search string and year
    if (results.isEmpty()) {
      try {
        int page = 1;
        int maxPage = 1;

        // get all result pages
        do {
          Response<MovieResultsPage> httpResponse = api.searchService().movie(searchString, page, language, null, adult, null, null).execute();
          if (!httpResponse.isSuccessful() || httpResponse.body() == null) {
            throw new HttpException(httpResponse.code(), httpResponse.message());
          }
          for (BaseMovie movie : ListUtils.nullSafe(httpResponse.body().results)) {
            results.add(morphMovieToSearchResult(movie, options));
          }

          maxPage = httpResponse.body().total_pages;
          page++;
        } while (page <= maxPage);

        LOGGER.debug("found {} results with search string", results.size());

      }
      catch (Exception e) {
        LOGGER.warn("problem getting data from tmdb: {}", e.getMessage());
        savedException = e;
      }
    }

    // 4. if the last token in search string seems to be a year, try without :)
    if (results.isEmpty()) {
      searchString = searchString.replaceFirst("\\s\\d{4}$", "");
      try {
        // /search/movie
        MovieResultsPage resultsPage = api.searchService().movie(searchString, 1, language, null, adult, null, null).execute().body();
        if (resultsPage != null && resultsPage.results != null) {
          for (BaseMovie movie : resultsPage.results) {
            results.add(morphMovieToSearchResult(movie, options));
          }
        }
        LOGGER.debug("found {} results with search string without year", results.size());
      }
      catch (Exception e) {
        LOGGER.warn("problem getting data from tmdb: {}", e.getMessage());
        savedException = e;
      }
    }

    // if we have not found anything and there is a saved Exception, throw it to indicate a problem
    if (results.isEmpty() && savedException != null) {
      throw new ScrapeException(savedException);
    }

    return results;
  }

  @Override
  public List<MediaSearchResult> search(MovieSetSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options);

    // lazy initialization of the api
    initAPI();

    List<MediaSearchResult> movieSetsFound = new ArrayList<>();

    String searchString = "";
    if (StringUtils.isNotEmpty(options.getSearchQuery())) {
      searchString = Utils.removeSortableName(options.getSearchQuery());
    }

    if (StringUtils.isEmpty(searchString)) {
      LOGGER.debug("TMDB Scraper: empty searchString");
      return movieSetsFound;
    }

    String language = getRequestLanguage(options.getLanguage());

    try {
      CollectionResultsPage resultsPage = api.searchService().collection(searchString, 1, language).execute().body();
      if (resultsPage != null) {
        for (BaseCollection collection : ListUtils.nullSafe(resultsPage.results)) {
          MediaSearchResult searchResult = new MediaSearchResult(getId(), MediaType.MOVIE_SET);
          searchResult.setId(Integer.toString(collection.id));
          searchResult.setTitle(collection.name);
          searchResult.setPosterUrl(artworkBaseUrl + "w342" + collection.poster_path);
          searchResult.setScore(MetadataUtil.calculateScore(searchString, collection.name));
          if (searchResult.getScore() < 0.5 && Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("titleFallback"))) {
            if (verifyMovieSetTitleLanguage(movieSetsFound, resultsPage, options)) {
              break;
            }
          }
          movieSetsFound.add(searchResult);
        }
      }
    }
    catch (Exception e) {
      LOGGER.debug("failed to search: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    LOGGER.info("found {} results ", movieSetsFound.size());
    return movieSetsFound;
  }

  public MediaMetadata getMetadata(MovieSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    // lazy initialization of the api
    initAPI();

    Exception savedException = null;

    MediaMetadata md;

    // tmdbId from option
    int tmdbId = options.getTmdbId();

    // imdbId from option
    String imdbId = options.getImdbId();

    if (tmdbId == 0 && !MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.debug("not possible to scrape from TMDB - no tmdbId/imdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    String language = getRequestLanguage(options.getLanguage());

    // scrape
    Movie movie = null;
    // we do not have the tmdbId?!? hmm.. get it from imdb...
    if (tmdbId == 0 && MediaIdUtil.isValidImdbId(imdbId)) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, options.getMediaType(), imdbId);
      }
      catch (Exception e) {
        LOGGER.debug("problem getting tmdbId from imdbId: {}", e.getMessage());
        savedException = e;
      }
    }

    if (tmdbId > 0) {
      try {
        Response<Movie> httpResponse = api.moviesService()
            .summary(tmdbId, language,
                new AppendToResponse(AppendToResponseItem.CREDITS, AppendToResponseItem.KEYWORDS, AppendToResponseItem.RELEASE_DATES,
                    AppendToResponseItem.TRANSLATIONS, AppendToResponseItem.EXTERNAL_IDS))
            .execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        movie = httpResponse.body();
        injectTranslations(Locale.forLanguageTag(language), movie);
      }
      catch (Exception e) {
        LOGGER.debug("problem getting data from tmdb: {}", e.getMessage());
        savedException = e;
      }
    }

    // if there is no result, but a saved exception, propagate it
    if (movie == null && savedException != null) {
      throw new ScrapeException(savedException);
    }

    if (movie == null) {
      LOGGER.warn("no result found");
      throw new NothingFoundException();
    }

    md = morphMovieToMediaMetadata(movie, options);

    return md;
  }

  @Override
  public MediaMetadata getMetadata(MovieSetSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    // lazy initialization of the api
    initAPI();

    MediaMetadata md = new MediaMetadata(getId());
    md.setScrapeOptions(options);

    // tmdbId from option
    int tmdbId;

    if (options.getIdAsInt(TMDB_SET) > 0) {
      tmdbId = options.getIdAsInt(TMDB_SET);
    }
    else {
      tmdbId = options.getTmdbId();
    }

    if (tmdbId == 0) {
      LOGGER.debug("not possible to scrape from TMDB - no tmdbId found");
      throw new MissingIdException(TMDB_SET);
    }

    String language = getRequestLanguage(options.getLanguage());

    Collection collection = null;

    try {
      collection = api.collectionService().summary(tmdbId, language).execute().body();
      // if collection title/overview is not availbale, rescrape in the fallback language
      if (collection != null && (StringUtils.isBlank(collection.overview) || StringUtils.isBlank(collection.name))
          && Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("titleFallback"))) {

        String fallbackLang = MediaLanguages.get(getProviderInfo().getConfig().getValue("titleFallbackLanguage")).name().replace("_", "-");
        Collection collectionInFallbackLanguage = api.collectionService().summary(tmdbId, fallbackLang).execute().body();

        if (collectionInFallbackLanguage != null) {
          Collection collectionInDefaultLanguage = null;
          if (StringUtils.isBlank(collectionInFallbackLanguage.name) || StringUtils.isBlank(collectionInFallbackLanguage.overview)) {
            collectionInDefaultLanguage = api.collectionService().summary(tmdbId, null).execute().body();

          }

          if (StringUtils.isBlank(collection.name) && StringUtils.isNotBlank(collectionInFallbackLanguage.name)) {
            collection.name = collectionInFallbackLanguage.name;
          }
          else if (StringUtils.isBlank(collection.name) && collectionInDefaultLanguage != null
              && StringUtils.isNotBlank(collectionInDefaultLanguage.name)) {
            collection.name = collectionInDefaultLanguage.name;
          }

          if (StringUtils.isBlank(collection.overview) && StringUtils.isNotBlank(collectionInFallbackLanguage.overview)) {
            collection.overview = collectionInFallbackLanguage.overview;
          }
          else if (StringUtils.isBlank(collection.overview) && collectionInDefaultLanguage != null
              && StringUtils.isNotBlank(collectionInDefaultLanguage.overview)) {
            collection.overview = collectionInDefaultLanguage.overview;
          }

          for (BaseMovie movie : collection.parts) {
            for (BaseMovie fallbackMovie : collectionInFallbackLanguage.parts) {
              if (movie.id.equals(fallbackMovie.id)) {
                if (StringUtils.isBlank(movie.overview) && !StringUtils.isBlank(fallbackMovie.overview)) {
                  movie.overview = fallbackMovie.overview;
                }
                if (movie.title.equals(movie.original_title) && !movie.original_language.equals(options.getLanguage().getLanguage())
                    && !StringUtils.isBlank(fallbackMovie.title)) {
                  movie.title = fallbackMovie.title;
                }
                break;
              }
            }
          }
        }
      }
    }
    catch (Exception e) {
      LOGGER.debug("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (collection == null) {
      throw new NothingFoundException();
    }

    md.setId(TMDB_SET, collection.id);
    md.setTitle(collection.name);
    md.setPlot(collection.overview);

    // Poster
    if (StringUtils.isNotBlank(collection.poster_path)) {
      MediaArtwork ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.POSTER);
      ma.setPreviewUrl(artworkBaseUrl + "w185" + collection.poster_path);
      ma.setOriginalUrl(artworkBaseUrl + "original" + collection.poster_path);
      ma.setLanguage(options.getLanguage().getLanguage());
      ma.setTmdbId(tmdbId);
      ma.addImageSize(0, 0, artworkBaseUrl + "original" + collection.poster_path, 0);
      md.addMediaArt(ma);
    }

    // Fanart
    if (StringUtils.isNotBlank(collection.backdrop_path)) {
      MediaArtwork ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.BACKGROUND);
      ma.setPreviewUrl(artworkBaseUrl + "w300" + collection.backdrop_path);
      ma.setOriginalUrl(artworkBaseUrl + "original" + collection.backdrop_path);
      ma.setLanguage(options.getLanguage().getLanguage());
      ma.setTmdbId(tmdbId);
      ma.addImageSize(0, 0, artworkBaseUrl + "original" + collection.backdrop_path, 0);

      md.addMediaArt(ma);
    }

    // add all movies belonging to this movie set
    for (BaseMovie part : ListUtils.nullSafe(collection.parts)) {
      if (part.release_date == null) {
        // has not been released yet?
        continue;
      }

      // get the full meta data
      try {
        MovieSearchAndScrapeOptions movieSearchAndScrapeOptions = new MovieSearchAndScrapeOptions();
        movieSearchAndScrapeOptions.setTmdbId(MetadataUtil.unboxInteger(part.id));
        movieSearchAndScrapeOptions.setLanguage(options.getLanguage());
        movieSearchAndScrapeOptions.setCertificationCountry(options.getCertificationCountry());
        movieSearchAndScrapeOptions.setReleaseDateCountry(options.getReleaseDateCountry());
        MediaMetadata mdSubItem = getMetadata(movieSearchAndScrapeOptions);

        // Poster
        if (StringUtils.isNotBlank(part.poster_path)) {
          MediaArtwork ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.POSTER);
          ma.setPreviewUrl(artworkBaseUrl + "w185" + part.poster_path);
          ma.setOriginalUrl(artworkBaseUrl + "original" + part.poster_path);
          ma.setLanguage(options.getLanguage().getLanguage());
          ma.setTmdbId(MetadataUtil.unboxInteger(part.id));
          ma.addImageSize(0, 0, artworkBaseUrl + "original" + part.poster_path, 0);
          mdSubItem.addMediaArt(ma);
        }

        // Fanart
        if (StringUtils.isNotBlank(part.backdrop_path)) {
          MediaArtwork ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.BACKGROUND);
          ma.setPreviewUrl(artworkBaseUrl + "w300" + part.backdrop_path);
          ma.setOriginalUrl(artworkBaseUrl + "original" + part.backdrop_path);
          ma.setLanguage(options.getLanguage().getLanguage());
          ma.setTmdbId(MetadataUtil.unboxInteger(part.id));
          ma.addImageSize(0, 0, artworkBaseUrl + "original" + part.backdrop_path, 0);
          mdSubItem.addMediaArt(ma);
        }

        md.addSubItem(mdSubItem);
      }
      catch (Exception e) {
        LOGGER.warn("could not get metadata for movie set movie - '{}'", e.getMessage());
        // fall back to the provided data

        MediaMetadata mdSubItem = new MediaMetadata(getId());
        mdSubItem.setScrapeOptions(options);
        mdSubItem.setId(getId(), part.id);
        mdSubItem.setTitle(part.title);
        mdSubItem.setOriginalTitle(part.original_title);
        mdSubItem.setPlot(part.overview);
        mdSubItem.setReleaseDate(part.release_date);

        // parse release date to year
        if (part.release_date != null) {
          Calendar calendar = Calendar.getInstance();
          calendar.setTime(part.release_date);
          mdSubItem.setYear(calendar.get(Calendar.YEAR));
        }

        if (part.vote_average != null) {
          mdSubItem.setRatings(
              Collections.singletonList(new MediaRating(getId(), part.vote_average.floatValue(), MetadataUtil.unboxInteger(part.vote_count))));
        }

        for (Genre genre : ListUtils.nullSafe(part.genres)) {
          mdSubItem.addGenre(getTmmGenre(genre));
        }

        // Poster
        if (StringUtils.isNotBlank(part.poster_path)) {
          MediaArtwork ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.POSTER);
          ma.setPreviewUrl(artworkBaseUrl + "w185" + part.poster_path);
          ma.setOriginalUrl(artworkBaseUrl + "original" + part.poster_path);
          ma.setLanguage(options.getLanguage().getLanguage());
          ma.setTmdbId(MetadataUtil.unboxInteger(part.id));
          ma.addImageSize(0, 0, artworkBaseUrl + "original" + part.poster_path, 0);
          mdSubItem.addMediaArt(ma);
        }

        // Fanart
        if (StringUtils.isNotBlank(part.backdrop_path)) {
          MediaArtwork ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.BACKGROUND);
          ma.setPreviewUrl(artworkBaseUrl + "w300" + part.backdrop_path);
          ma.setOriginalUrl(artworkBaseUrl + "original" + part.backdrop_path);
          ma.setLanguage(options.getLanguage().getLanguage());
          ma.setTmdbId(MetadataUtil.unboxInteger(part.id));
          ma.addImageSize(0, 0, artworkBaseUrl + "original" + part.backdrop_path, 0);
          mdSubItem.addMediaArt(ma);
        }

        md.addSubItem(mdSubItem);
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

    int tmdbId = MediaIdUtil.getIdAsInt(ids, MediaMetadata.TMDB);
    String imdbId = MediaIdUtil.getIdAsString(ids, MediaMetadata.IMDB);

    if (tmdbId == 0 && !MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.debug("not possible to scrape from TMDB - no tmdbId/imdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    // scrape
    Movie movie = null;
    // we do not have the tmdbId?!? hmm.. get it from imdb...
    if (tmdbId == 0 && MediaIdUtil.isValidImdbId(imdbId)) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.MOVIE, imdbId);
      }
      catch (Exception e) {
        LOGGER.debug("problem getting tmdbId from imdbId: {}", e.getMessage());
      }
    }

    if (tmdbId == 0) {
      LOGGER.debug("not possible to scrape from TMDB - no tmdbId/imdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    try {
      Response<Movie> httpResponse = api.moviesService().summary(tmdbId, "en", null).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      movie = httpResponse.body();
    }
    catch (Exception e) {
      LOGGER.debug("problem getting data from tmdb: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (movie == null) {
      throw new NothingFoundException();
    }

    MediaRating rating = new MediaRating(MediaMetadata.TMDB);
    rating.setRating(movie.vote_average.floatValue());
    rating.setVotes(movie.vote_count);
    rating.setMaxValue(10);

    return Collections.singletonList(rating);
  }

  @Override
  public Map<String, Object> getMediaIds(Map<String, Object> ids, MediaType mediaType) throws ScrapeException {
    if (mediaType != MediaType.MOVIE) {
      return Collections.emptyMap();
    }

    LOGGER.debug("getMediaIds(): {}", ids);

    // lazy initialization of the api
    initAPI();

    int tmdbId = MediaIdUtil.getIdAsInt(ids, MediaMetadata.TMDB);
    String imdbId = MediaIdUtil.getIdAsString(ids, MediaMetadata.IMDB);

    if (tmdbId == 0 && !MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.debug("not possible to scrape from TMDB - no tmdbId/imdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    // TMDB only offers the tmdb id and the imdb id
    Map<String, Object> scrapedIds = new HashMap<>();

    if (tmdbId == 0 && MediaIdUtil.isValidImdbId(imdbId)) {
      // we have the imdb id and just need the tmdb id
      try {
        tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.MOVIE, imdbId);
      }
      catch (Exception e) {
        LOGGER.debug("problem getting tmdbId from imdbId: {}", e.getMessage());
      }

      scrapedIds.put(IMDB, imdbId);
      if (tmdbId > 0) {
        scrapedIds.put(TMDB, tmdbId);
      }

      return scrapedIds;
    }

    // scrape
    Movie movie;

    try {
      Response<Movie> httpResponse = api.moviesService().summary(tmdbId, "en", new AppendToResponse(AppendToResponseItem.EXTERNAL_IDS)).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      movie = httpResponse.body();
    }
    catch (Exception e) {
      LOGGER.debug("problem getting data from tmdb: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (movie == null) {
      throw new NothingFoundException();
    }

    scrapedIds.put(TMDB, movie.id);
    // external IDs
    parseExternalIDs(movie.external_ids).forEach(scrapedIds::put);

    return scrapedIds;
  }

  /**
   * Fallback Language Mechanism - for direct TMDB lookup<br>
   * Title/Overview always gets returned in the original language, if translation has not been found.<br>
   * So we never know exactly what is missing.. so we just inject everything here by hand if a fallback language has been found
   */
  private void injectTranslations(Locale language, Movie movie) {
    if (Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("titleFallback"))) {
      Locale fallbackLanguage = Locale.forLanguageTag(getProviderInfo().getConfig().getValue("titleFallbackLanguage"));
      // get in desired localization
      String[] val = getValuesFromTranslation(movie.translations, language);

      // if the search language equals the original language of the movie, there may be no translation
      if (StringUtils.isBlank(val[0]) && language.getLanguage().equals(movie.original_language)) {
        val[0] = movie.original_title;
      }

      // merge empty ones with fallback
      String[] temp = getValuesFromTranslation(movie.translations, fallbackLanguage);
      if (StringUtils.isBlank(val[0])) {
        val[0] = temp[0];
      }
      if (StringUtils.isBlank(val[1])) {
        val[1] = temp[1];
      }

      // finally SET the values
      if (StringUtils.isNotBlank(val[0])) {
        movie.title = val[0];
      }
      if (StringUtils.isNotBlank(val[1])) {
        movie.overview = val[1];
      }
    }
  }

  private MediaSearchResult morphMovieToSearchResult(BaseMovie movie, MovieSearchAndScrapeOptions query) {
    MediaSearchResult searchResult = new MediaSearchResult(getProviderInfo().getId(), MediaType.MOVIE);
    searchResult.setId(Integer.toString(movie.id));
    searchResult.setTitle(movie.title);
    searchResult.setOverview(movie.overview); // empty overview tells us that we have no translation?
    searchResult.setOriginalTitle(movie.original_title);
    searchResult.setOriginalLanguage(movie.original_language);

    if (movie.poster_path != null && !movie.poster_path.isEmpty()) {
      searchResult.setPosterUrl(artworkBaseUrl + "w342" + movie.poster_path);
    }

    // parse release date to year
    if (movie.release_date != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(movie.release_date);
      searchResult.setYear(calendar.get(Calendar.YEAR));
    }

    // calculate score
    if ((StringUtils.isNotBlank(query.getImdbId()) && query.getImdbId().equals(searchResult.getIMDBId()))
        || String.valueOf(query.getTmdbId()).equals(searchResult.getId())) {
      LOGGER.debug("perfect match by ID - set score to 1");
      searchResult.setScore(1);
    }
    else {
      // calculate the score by comparing the search result with the search options
      searchResult.calculateScore(query);
    }

    return searchResult;
  }

  private MediaMetadata morphMovieToMediaMetadata(Movie movie, MovieSearchAndScrapeOptions options) {
    MediaMetadata md = new MediaMetadata(getProviderInfo().getId());
    md.setScrapeOptions(options);

    md.setId(getProviderInfo().getId(), movie.id);
    md.setTitle(movie.title);
    md.setOriginalTitle(movie.original_title);
    md.setPlot(movie.overview);
    md.setTagline(movie.tagline);
    md.setRuntime(movie.runtime);

    MediaRating rating = new MediaRating(MediaMetadata.TMDB);
    rating.setRating(movie.vote_average.floatValue());
    rating.setVotes(movie.vote_count);
    rating.setMaxValue(10);
    md.addRating(rating);

    // Poster
    if (StringUtils.isNotBlank(movie.poster_path)) {
      MediaArtwork ma = new MediaArtwork(getProviderInfo().getId(), MediaArtwork.MediaArtworkType.POSTER);
      ma.setPreviewUrl(artworkBaseUrl + "w342" + movie.poster_path);
      ma.setOriginalUrl(artworkBaseUrl + "original" + movie.poster_path);
      ma.setLanguage(options.getLanguage().getLanguage());
      ma.setTmdbId(movie.id);
      ma.addImageSize(0, 0, artworkBaseUrl + "original" + movie.poster_path, 0);
      md.addMediaArt(ma);
    }

    for (SpokenLanguage lang : ListUtils.nullSafe(movie.spoken_languages)) {
      if (Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("scrapeLanguageNames"))) {
        md.addSpokenLanguage(LanguageUtils.getLocalizedLanguageNameFromLocalizedString(options.getLanguage().toLocale(), lang.name, lang.iso_639_1));
      }
      else {
        md.addSpokenLanguage(lang.iso_639_1);
      }
    }

    for (Country country : ListUtils.nullSafe(movie.production_countries)) {
      if (Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("scrapeLanguageNames"))) {
        md.addCountry(LanguageUtils.getLocalizedCountryForLanguage(options.getLanguage().toLocale(), country.name, country.iso_3166_1));
      }
      else {
        md.addCountry(country.iso_3166_1);
      }
    }

    // external IDs
    parseExternalIDs(movie.external_ids).forEach(md::setId);

    // production companies
    for (BaseCompany company : ListUtils.nullSafe(movie.production_companies)) {
      md.addProductionCompany(company.name.strip());
    }

    // releases & certification
    if (movie.release_dates != null) {
      // only use the certification of the desired country (if any country has been chosen)
      CountryCode countryCode = options.getCertificationCountry();
      String releaseDateCountry = options.getReleaseDateCountry();

      for (ReleaseDatesResult countries : ListUtils.nullSafe(movie.release_dates.results)) {
        if (StringUtils.isBlank(countries.iso_3166_1)) {
          continue;
        }

        // certification
        if (countryCode != null && countries.iso_3166_1.equalsIgnoreCase(countryCode.getAlpha2())) {
          for (ReleaseDate countryReleaseDate : ListUtils.nullSafe(countries.release_dates)) {
            // do not use any empty certifications
            if (StringUtils.isNotBlank(countryReleaseDate.certification)) {
              md.addCertification(MediaCertification.getCertification(countries.iso_3166_1, countryReleaseDate.certification));
            }
          }
        }

        // release date
        if (Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("localReleaseDate"))) {
          if (StringUtils.isNotBlank(releaseDateCountry) && releaseDateCountry.equalsIgnoreCase(countries.iso_3166_1)) {
            for (ReleaseDate countryReleaseDate : ListUtils.nullSafe(countries.release_dates)) {
              if (countryReleaseDate.release_date == null
                  || (md.getReleaseDate() != null && countryReleaseDate.release_date.after(md.getReleaseDate()))) {
                // either null or after -> we do not need this
                continue;
              }

              // depending on the type we might want this
              int type = MetadataUtil.unboxInteger(countryReleaseDate.type);

              // 1... premiere
              // >1.. "normal" releases (theatrical, physical, ...)
              if (type > 1 || (type == 1 && Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("includePremiereDate")))) {
                md.setReleaseDate(countryReleaseDate.release_date);
              }
            }
          }
        }
      }
    }

    // if we did not find a local release date, set the global one
    if (md.getReleaseDate() == null) {
      md.setReleaseDate(movie.release_date);
    }

    // parse global release date to year
    if (movie.release_date != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(movie.release_date);
      md.setYear(calendar.get(Calendar.YEAR));
    }

    // cast & crew
    if (movie.credits != null) {
      for (CastMember castMember : ListUtils.nullSafe(movie.credits.cast)) {
        Person cm = new Person(Person.Type.ACTOR);

        cm.setId(getProviderInfo().getId(), castMember.id);
        cm.setName(castMember.name);
        cm.setRole(castMember.character);

        if (StringUtils.isNotBlank(castMember.profile_path)) {
          cm.setThumbUrl(artworkBaseUrl + "h632" + castMember.profile_path);
        }
        if (castMember.id != null) {
          cm.setProfileUrl("https://www.themoviedb.org/person/" + castMember.id);
        }
        md.addCastMember(cm);
      }

      // crew
      for (CrewMember crewMember : ListUtils.nullSafe(movie.credits.crew)) {
        Person cm = new Person();
        if ("Director".equals(crewMember.job)) {
          cm.setType(DIRECTOR);
        }
        else if ("Writing".equals(crewMember.department)) {
          cm.setType(WRITER);
        }
        else if ("Production".equals(crewMember.department)) {
          cm.setType(PRODUCER);
        }
        else {
          continue;
        }
        cm.setId(getProviderInfo().getId(), crewMember.id);
        cm.setName(crewMember.name);

        if (StringUtils.isNotBlank(crewMember.profile_path)) {
          cm.setThumbUrl(artworkBaseUrl + "h632" + crewMember.profile_path);
        }
        if (crewMember.id != null) {
          cm.setProfileUrl("https://www.themoviedb.org/person/" + crewMember.id);
        }

        md.addCastMember(cm);
      }
    }

    // Genres
    for (Genre genre : ListUtils.nullSafe(movie.genres)) {
      md.addGenre(TmdbMetadataProvider.getTmmGenre(genre));
    }
    // "adult" on TMDB is always some pr0n stuff, and not just rated 18+ content
    if (Boolean.TRUE.equals(movie.adult)) {
      md.addGenre(MediaGenres.EROTIC);
    }

    if (movie.belongs_to_collection != null) {
      md.setId(TMDB_SET, movie.belongs_to_collection.id);
      md.setCollectionName(movie.belongs_to_collection.name);
    }

    // add some special keywords as tags
    // see http://forum.kodi.tv/showthread.php?tid=254004
    if (movie.keywords != null) {
      for (BaseKeyword kw : ListUtils.nullSafe(movie.keywords.keywords)) {
        md.addTag(kw.name);
      }
    }

    return md;
  }

  /**
   * Language Fallback Mechanism - For Search Results
   *
   * @param query
   *          the query options
   * @param original
   *          the original movie set list
   * @param movieSetsFound
   *          the list that movie sets will be added.
   */
  private boolean verifyMovieSetTitleLanguage(List<MediaSearchResult> movieSetsFound, CollectionResultsPage original,
      MovieSetSearchAndScrapeOptions query) throws Exception {

    String lang = MediaLanguages.get(getProviderInfo().getConfig().getValue("titleFallbackLanguage")).name().replace("_", "-");
    CollectionResultsPage fallBackResultsPage = api.searchService().collection(query.getSearchQuery(), 1, lang).execute().body();

    if (fallBackResultsPage != null && original.results != null && fallBackResultsPage.results != null) {

      movieSetsFound.clear();

      for (int i = 0; i < original.results.size(); i++) {
        BaseCollection originalCollection = original.results.get(i);
        BaseCollection fallbackCollection = fallBackResultsPage.results.get(i);

        MediaSearchResult searchResult = new MediaSearchResult(getId(), MediaType.MOVIE_SET);

        searchResult.setId(Integer.toString(originalCollection.id));

        if (MetadataUtil.calculateScore(query.getSearchQuery(), originalCollection.name) >= MetadataUtil.calculateScore(query.getSearchQuery(),
            fallbackCollection.name)) {
          searchResult.setTitle(originalCollection.name);
          searchResult.setPosterUrl(artworkBaseUrl + "w342" + originalCollection.poster_path);
          searchResult.setScore(MetadataUtil.calculateScore(query.getSearchQuery(), originalCollection.name));
        }
        else {
          searchResult.setTitle(fallbackCollection.name);
          searchResult.setPosterUrl(artworkBaseUrl + "w342" + fallbackCollection.poster_path);
          searchResult.setScore(MetadataUtil.calculateScore(query.getSearchQuery(), fallbackCollection.name));
        }
        movieSetsFound.add(searchResult);
      }
      return true;
    }
    else {
      return false;
    }
  }
}
