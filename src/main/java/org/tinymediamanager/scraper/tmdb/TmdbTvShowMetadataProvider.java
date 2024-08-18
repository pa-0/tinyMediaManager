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

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.PRODUCER;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.MediaMetadata.IMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TVDB;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroupType.AIRED;
import static org.tinymediamanager.scraper.util.MediaIdUtil.isValidImdbId;

import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaIdProvider;
import org.tinymediamanager.scraper.interfaces.IRatingProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowTmdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowTvdbMetadataProvider;
import org.tinymediamanager.scraper.tmdb.entities.AppendToResponse;
import org.tinymediamanager.scraper.tmdb.entities.BaseCompany;
import org.tinymediamanager.scraper.tmdb.entities.BaseKeyword;
import org.tinymediamanager.scraper.tmdb.entities.BaseTvEpisode;
import org.tinymediamanager.scraper.tmdb.entities.BaseTvShow;
import org.tinymediamanager.scraper.tmdb.entities.CastMember;
import org.tinymediamanager.scraper.tmdb.entities.ContentRating;
import org.tinymediamanager.scraper.tmdb.entities.CrewMember;
import org.tinymediamanager.scraper.tmdb.entities.FindResults;
import org.tinymediamanager.scraper.tmdb.entities.Genre;
import org.tinymediamanager.scraper.tmdb.entities.Image;
import org.tinymediamanager.scraper.tmdb.entities.Network;
import org.tinymediamanager.scraper.tmdb.entities.Translations;
import org.tinymediamanager.scraper.tmdb.entities.TvEpisode;
import org.tinymediamanager.scraper.tmdb.entities.TvEpisodeGroup;
import org.tinymediamanager.scraper.tmdb.entities.TvEpisodeGroupEpisode;
import org.tinymediamanager.scraper.tmdb.entities.TvEpisodeGroupSeason;
import org.tinymediamanager.scraper.tmdb.entities.TvSeason;
import org.tinymediamanager.scraper.tmdb.entities.TvShow;
import org.tinymediamanager.scraper.tmdb.entities.TvShowResultsPage;
import org.tinymediamanager.scraper.tmdb.enumerations.AppendToResponseItem;
import org.tinymediamanager.scraper.tmdb.enumerations.ExternalSource;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;

import retrofit2.Response;

public class TmdbTvShowMetadataProvider extends TmdbMetadataProvider implements ITvShowMetadataProvider, ITvShowTmdbMetadataProvider,
    ITvShowImdbMetadataProvider, ITvShowTvdbMetadataProvider, IRatingProvider, IMediaIdProvider {
  private static final Logger                                LOGGER                 = LoggerFactory.getLogger(TmdbTvShowMetadataProvider.class);
  private static final CacheMap<String, List<MediaMetadata>> EPISODE_LIST_CACHE_MAP = new CacheMap<>(600, 5);

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = super.createMediaProviderInfo();

    info.getConfig().addBoolean("includeAdultShows", false);
    info.getConfig().addBoolean("scrapeLanguageNames", true);
    info.getConfig().addBoolean("titleFallback", false);
    info.getConfig().addSelect("titleFallbackLanguage", PT, "en-US");
    info.getConfig().load();

    return info;
  }

  @Override
  protected String getSubId() {
    return "tvshow";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public SortedSet<MediaSearchResult> search(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {
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
    if (!isValidImdbId(imdbId)) {
      imdbId = "";
    }
    if (isValidImdbId(searchString)) {
      imdbId = searchString;
    }

    int tmdbId = options.getTmdbId();
    int tvdbId = options.getIdAsInt(TVDB);

    boolean adult = getProviderInfo().getConfig().getValueAsBool("includeAdultShows");

    String language = getRequestLanguage(options.getLanguage());

    // begin search
    LOGGER.info("========= BEGIN TMDB Scraper Search for: {}", searchString);
    // 1. try with TMDB id
    if (tmdbId != 0) {
      LOGGER.debug("found TMDB ID {} - getting direct", tmdbId);
      try {
        Response<TvShow> httpResponse = api.tvService().tv(tmdbId, language, new AppendToResponse(AppendToResponseItem.TRANSLATIONS)).execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        TvShow show = httpResponse.body();
        injectTranslations(Locale.forLanguageTag(language), show);
        results.add(morphTvShowToSearchResult(show, options));
        LOGGER.debug("found {} results with TMDB id", results.size());
      }
      catch (Exception e) {
        LOGGER.warn("problem getting data from tmdb: {}", e.getMessage());
        savedException = e;
      }
    }

    // 2. try with IMDB id
    if (results.isEmpty() && StringUtils.isNotEmpty(imdbId)) {
      LOGGER.debug("found IMDB ID {} - getting direct", imdbId);
      try {
        Response<FindResults> httpResponse = api.findService().find(imdbId, ExternalSource.IMDB_ID, language).execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        for (BaseTvShow show : httpResponse.body().tv_results) { // should be only one
          injectTranslations(Locale.forLanguageTag(language), show);
          results.add(morphTvShowToSearchResult(show, options));
        }
        LOGGER.debug("found {} results with IMDB id", results.size());
      }
      catch (Exception e) {
        LOGGER.warn("problem getting data from tmdb: {}", e.getMessage());
        savedException = e;
      }
    }

    // 3. try with TVDB id
    if (results.isEmpty() && tvdbId > 0) {
      LOGGER.debug("found TVDB ID {} - getting direct", tvdbId);
      try {
        Response<FindResults> httpResponse = api.findService().find(tvdbId, ExternalSource.TVDB_ID, language).execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        for (BaseTvShow show : httpResponse.body().tv_results) { // should be only one
          injectTranslations(options.getLanguage().toLocale(), show);
          results.add(morphTvShowToSearchResult(show, options));
        }
        LOGGER.debug("found {} results with TVDB id", results.size());
      }
      catch (Exception e) {
        LOGGER.warn("problem getting data from tvdb: {}", e.getMessage());
        savedException = e;
      }
    }

    // 4. try with search string and year
    if (results.isEmpty()) {
      try {
        int page = 1;
        int maxPage = 1;

        // get all result pages
        do {
          Response<TvShowResultsPage> httpResponse = api.searchService().tv(searchString, page, language, null, adult).execute();
          if (!httpResponse.isSuccessful() || httpResponse.body() == null) {
            throw new HttpException(httpResponse.code(), httpResponse.message());
          }

          for (BaseTvShow show : ListUtils.nullSafe(httpResponse.body().results)) {
            injectTranslations(Locale.forLanguageTag(language), show);
            results.add(morphTvShowToSearchResult(show, options));
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

    // if we have not found anything and there is a saved Exception, throw it to indicate a problem
    if (results.isEmpty() && savedException != null) {
      throw new ScrapeException(savedException);
    }

    return results;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeList(): {} ", options);

    // lazy initialization of the api
    initAPI();

    // tmdbId from option
    int tmdbId = options.getTmdbId();

    // try to get via imdb id
    if (tmdbId == 0 && MediaIdUtil.isValidImdbId(options.getImdbId())) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.TV_SHOW, options.getImdbId());
      }
      catch (Exception e) {
        LOGGER.warn("could not get tmdb id via imdb id - {}", e.getMessage());
      }
    }

    // try to get via tvdb id
    if (tmdbId == 0 && options.getIdAsIntOrDefault(TVDB, 0) > 0) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromTvdbId(api, options.getIdAsInteger(TVDB));
      }
      catch (Exception e) {
        LOGGER.warn("could not get tmdb id via tvdb id - {}", e.getMessage());
      }
    }

    // no tmdb id, no search..
    if (tmdbId == 0) {
      throw new MissingIdException(MediaMetadata.TMDB);
    }

    String language = getRequestLanguage(options.getLanguage());

    // look in the cache map if there is an entry
    List<MediaMetadata> episodes = EPISODE_LIST_CACHE_MAP.get(tmdbId + "_" + language);
    if (ListUtils.isNotEmpty(episodes)) {
      // cache hit!
      return episodes;
    }

    episodes = new ArrayList<>();

    // the API does not provide complete access to all episodes, so we have to
    // fetch the show summary first and every season afterwards...
    try {
      // use the SAME show call, since this is already cached!
      Response<TvShow> showResponse = api.tvService()
          .tv(tmdbId, language,
              new AppendToResponse(AppendToResponseItem.TRANSLATIONS, AppendToResponseItem.CREDITS, AppendToResponseItem.EXTERNAL_IDS,
                  AppendToResponseItem.CONTENT_RATINGS, AppendToResponseItem.KEYWORDS, AppendToResponseItem.EPISODE_GROUPS))
          .execute();
      if (!showResponse.isSuccessful() || showResponse.body() == null) {
        throw new HttpException(showResponse.code(), showResponse.message());
      }

      // get episode group data
      Map<MediaEpisodeGroup, List<TvEpisodeGroupSeason>> episodeGroups = new HashMap<>();
      for (TvEpisodeGroup episodeGroup : ListUtils.nullSafe(showResponse.body().episodeGroups.episodeGroups)) {
        if (episodeGroup.episodeCount == 0) {
          continue;
        }

        try {
          Response<TvEpisodeGroup> episodeGroupsResponse = api.tvEpisodeGroupsService().episodeGroup(episodeGroup.id, language).execute();
          if (!episodeGroupsResponse.isSuccessful() || episodeGroupsResponse.body() == null) {
            throw new HttpException(episodeGroupsResponse.code(), episodeGroupsResponse.message());
          }

          MediaEpisodeGroup.EpisodeGroupType eg = mapEpisodeGroup(episodeGroup.type);
          if (eg != null) {
            List<TvEpisodeGroupSeason> seasons = episodeGroupsResponse.body().groups;
            for (int i = seasons.size() - 1; i >= 0; i--) {
              // TMDB fault - remove EG season when no episodes are returned!
              if (seasons.get(i).episodes.isEmpty()) {
                seasons.remove(i);
              }
            }
            if (!seasons.isEmpty()) {
              episodeGroups.put(new MediaEpisodeGroup(eg, episodeGroup.name), seasons);
            }
            else {
              LOGGER.warn("EpisodeGroup '{}' without episodes! {} ", episodeGroupsResponse.body().name, showResponse.body().name);
            }
          }
        }
        catch (Exception e) {
          LOGGER.info("Could not fetch episode group data - '{}'", e.getMessage());
        }
      }

      String originalLanguage = showResponse.body().original_language;

      // get season/episode data (aired)
      for (TvSeason season : ListUtils.nullSafe(showResponse.body().seasons)) {
        List<MediaMetadata> seasonEpisodes = getSeasonEpisodes(tmdbId, season.season_number, options, originalLanguage, episodeGroups);
        episodes.addAll(seasonEpisodes);
      }
    }
    catch (Exception e) {
      LOGGER.debug("failed to get episode list: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    // cache the episode list
    if (!episodes.isEmpty()) {
      EPISODE_LIST_CACHE_MAP.put(tmdbId + "_" + language, episodes);
    }

    return episodes;
  }

  private List<MediaMetadata> getSeasonEpisodes(int tmdbId, Integer seasonNumber, TvShowSearchAndScrapeOptions options, String originalLanguage,
      Map<MediaEpisodeGroup, List<TvEpisodeGroupSeason>> episodeGroups) throws IOException {
    Map<Integer, MediaMetadata> episodesInRequestedLanguage = new TreeMap<>();
    Map<Integer, MediaMetadata> episodesInFallbackLanguage = new TreeMap<>();
    Map<Integer, MediaMetadata> episodesInOriginalLanguage = new TreeMap<>();

    // requested language
    String requestedLanguage = getRequestLanguage(options.getLanguage());
    Response<TvSeason> seasonResponse = api.tvSeasonsService()
        .season(tmdbId, seasonNumber, requestedLanguage, new AppendToResponse(AppendToResponseItem.CREDITS))
        .execute();
    if (!seasonResponse.isSuccessful()) {
      throw new HttpException(seasonResponse.code(), seasonResponse.message());
    }

    for (TvEpisode episode : ListUtils.nullSafe(seasonResponse.body().episodes)) {
      // season does not send translations, get em only with full episode scrape
      MediaMetadata ep = morphTvEpisodeToMediaMetadata(episode, seasonResponse.body(), episodeGroups, options);
      episodesInRequestedLanguage.put(episode.episode_number, ep);
    }

    // fallback language
    if (Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("titleFallback"))) {
      String fallbackLanguage = getProviderInfo().getConfig().getValue("titleFallbackLanguage");
      if (!requestedLanguage.equals(fallbackLanguage)) {
        seasonResponse = api.tvSeasonsService().season(tmdbId, seasonNumber, fallbackLanguage).execute();
        if (seasonResponse.isSuccessful()) {
          for (TvEpisode episode : ListUtils.nullSafe(seasonResponse.body().episodes)) {
            // season does not send translations, get em only with full episode scrape
            MediaMetadata ep = morphTvEpisodeToMediaMetadata(episode, null, null, options);
            episodesInFallbackLanguage.put(episode.episode_number, ep);
          }
        }
      }
    }

    // original language
    if (requestedLanguage.equals(originalLanguage)) {
      episodesInOriginalLanguage.putAll(episodesInRequestedLanguage);
    }
    else {
      seasonResponse = api.tvSeasonsService().season(tmdbId, seasonNumber, originalLanguage).execute();
      if (seasonResponse.isSuccessful()) {
        for (TvEpisode episode : ListUtils.nullSafe(seasonResponse.body().episodes)) {
          // season does not send translations, get em only with full episode scrape
          MediaMetadata ep = morphTvEpisodeToMediaMetadata(episode, null, null, options);
          episodesInOriginalLanguage.put(episode.episode_number, ep);
        }
      }
    }

    // build up all episodes
    List<MediaMetadata> episodes = new ArrayList<>();
    for (MediaMetadata md : episodesInRequestedLanguage.values()) {
      int episode = md.getEpisodeNumber(AIRED).episode();
      MediaMetadata fallback = episodesInFallbackLanguage.get(episode);
      MediaMetadata original = episodesInOriginalLanguage.get(episode);

      // title
      // try to get from fallback
      if (StringUtils.isBlank(md.getTitle()) && fallback != null && StringUtils.isNotBlank(fallback.getTitle())) {
        md.setTitle(fallback.getTitle());
      }

      // try to get from original
      if (StringUtils.isBlank(md.getTitle()) && original != null && StringUtils.isNotBlank(original.getTitle())) {
        md.setTitle(original.getTitle());
      }

      // original title
      if (original != null && StringUtils.isNotBlank(original.getTitle())) {
        md.setOriginalTitle(original.getTitle());
      }

      // plot
      // try to get from fallback
      if (StringUtils.isBlank(md.getPlot()) && fallback != null && StringUtils.isNotBlank(fallback.getPlot())) {
        md.setPlot(fallback.getPlot());
      }

      // try to get from original
      if (StringUtils.isBlank(md.getPlot()) && original != null && StringUtils.isNotBlank(original.getPlot())) {
        md.setPlot(original.getPlot());
      }

      episodes.add(md);
    }

    return episodes;
  }

  @Override
  public MediaMetadata getMetadata(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    // lazy initialization of the api
    initAPI();

    MediaMetadata md = new MediaMetadata(getId());
    md.setScrapeOptions(options);

    // tmdbId from option
    int tmdbId = options.getTmdbId();

    // try to get via imdb id
    if (tmdbId == 0 && MediaIdUtil.isValidImdbId(options.getImdbId())) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.TV_SHOW, options.getImdbId());
      }
      catch (Exception e) {
        LOGGER.debug("could not get tmdb id via imdb id - {}", e.getMessage());
      }
    }

    // try to get via tvdb id
    if (tmdbId == 0 && options.getIdAsIntOrDefault(TVDB, 0) > 0) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromTvdbId(api, options.getIdAsInteger(TVDB));
      }
      catch (Exception e) {
        LOGGER.debug("could not get tmdb id via tvdb id - {}", e.getMessage());
      }
    }

    // no tmdb id, no scrape..
    if (tmdbId == 0) {
      LOGGER.warn("not possible to scrape from TMDB - no tmdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    String language = getRequestLanguage(options.getLanguage());

    TvShow complete;
    try {
      // when changing this call/params, be sure to change on line 307 too!
      Response<TvShow> httpResponse = api.tvService()
          .tv(tmdbId, language,
              new AppendToResponse(AppendToResponseItem.TRANSLATIONS, AppendToResponseItem.CREDITS, AppendToResponseItem.EXTERNAL_IDS,
                  AppendToResponseItem.CONTENT_RATINGS, AppendToResponseItem.KEYWORDS, AppendToResponseItem.EPISODE_GROUPS))
          .execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      complete = httpResponse.body();
      injectTranslations(Locale.forLanguageTag(language), complete);
    }
    catch (Exception e) {
      LOGGER.warn("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (complete == null) {
      throw new NothingFoundException();
    }

    md.setId(getId(), tmdbId);
    md.setTitle(complete.name);
    md.setOriginalTitle(complete.original_name);

    try {
      MediaRating rating = new MediaRating(MediaMetadata.TMDB);
      rating.setRating(complete.vote_average);
      rating.setVotes(complete.vote_count);
      rating.setMaxValue(10);
      md.addRating(rating);
    }
    catch (Exception e) {
      LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
    }

    md.setReleaseDate(complete.first_air_date);
    md.setPlot(complete.overview);
    for (String country : ListUtils.nullSafe(complete.origin_country)) {
      if (Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("scrapeLanguageNames"))) {
        md.addCountry(LanguageUtils.getLocalizedCountryForLanguage(options.getLanguage().toLocale(), country));
      }
      else {
        md.addCountry(country);
      }
    }

    if (complete.episode_run_time != null && !complete.episode_run_time.isEmpty()) {
      md.setRuntime(complete.episode_run_time.get(0));
    }

    // Poster
    if (StringUtils.isNotBlank(complete.poster_path)) {
      MediaArtwork ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.POSTER);
      ma.setPreviewUrl(artworkBaseUrl + "w342" + complete.poster_path);
      ma.setOriginalUrl(artworkBaseUrl + "original" + complete.poster_path);
      ma.setLanguage(options.getLanguage().getLanguage());
      ma.setTmdbId(complete.id);
      ma.addImageSize(0, 0, artworkBaseUrl + "original" + complete.poster_path, 0);
      md.addMediaArt(ma);
    }

    // scrape networks before all other production companies
    for (Network network : ListUtils.nullSafe(complete.networks)) {
      md.addProductionCompany(network.name.strip());
    }
    for (BaseCompany company : ListUtils.nullSafe(complete.production_companies)) {
      md.addProductionCompany(company.name.strip());
    }
    md.setStatus(complete.status);

    if (complete.first_air_date != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(complete.first_air_date);
      md.setYear(calendar.get(Calendar.YEAR));
    }

    if (complete.credits != null) {
      for (CastMember castMember : ListUtils.nullSafe(complete.credits.cast)) {
        Person cm = new Person(ACTOR);
        cm.setId(getProviderInfo().getId(), castMember.id);
        cm.setName(castMember.name);
        cm.setRole(castMember.character);
        if (castMember.id != null) {
          cm.setProfileUrl("https://www.themoviedb.org/person/" + castMember.id);
        }

        if (StringUtils.isNotBlank(castMember.profile_path)) {
          cm.setThumbUrl(artworkBaseUrl + "h632" + castMember.profile_path);
        }

        md.addCastMember(cm);
      }
    }

    // external IDs
    parseExternalIDs(complete.external_ids).forEach(md::setId);

    // content ratings
    if (complete.content_ratings != null) {
      // only use the certification of the desired country (if any country has been chosen)
      CountryCode countryCode = options.getCertificationCountry();
      if (countryCode != null) {
        for (ContentRating country : ListUtils.nullSafe(complete.content_ratings.results)) {
          // do not use any empty certifications
          if (StringUtils.isNotBlank(country.rating) && country.iso_3166_1.equalsIgnoreCase(countryCode.getAlpha2())) {
            md.addCertification(MediaCertification.getCertification(country.iso_3166_1, country.rating));
          }
        }
      }
    }

    // Genres
    for (Genre genre : ListUtils.nullSafe(complete.genres)) {
      md.addGenre(TmdbMetadataProvider.getTmmGenre(genre));
    }

    // episode groups
    // aired date is default
    md.addEpisodeGroup(MediaEpisodeGroup.DEFAULT_AIRED);

    // we only got the overview
    if (complete.episodeGroups != null) {
      for (TvEpisodeGroup episodeGroup : ListUtils.nullSafe(complete.episodeGroups.episodeGroups)) {
        if (episodeGroup.episodeCount == 0) {
          continue;
        }

        // now get details for every EG
        try {
          Response<TvEpisodeGroup> httpResponse = api.tvEpisodeGroupsService().episodeGroup(episodeGroup.id, language).execute();
          if (!httpResponse.isSuccessful()) {
            throw new HttpException(httpResponse.code(), httpResponse.message());
          }
          TvEpisodeGroup fullEG = httpResponse.body();
          episodeGroup.groups = fullEG.groups; // copy to ours, so the rest of our code works 1:1
        }
        catch (Exception e) {
          LOGGER.warn("failed to get episode group details: {}", e.getMessage());
          // throw new ScrapeException(e); // continue
        }

        MediaEpisodeGroup.EpisodeGroupType eg = mapEpisodeGroup(episodeGroup.type);
        if (eg != null) {
          MediaEpisodeGroup mediaEpisodeGroup = new MediaEpisodeGroup(eg, episodeGroup.name);
          md.addEpisodeGroup(mediaEpisodeGroup);
          // season titles
          for (TvEpisodeGroupSeason season : ListUtils.nullSafe(episodeGroup.groups)) {
            if (StringUtils.isNotBlank(season.name)) {
              md.addSeasonName(mediaEpisodeGroup, season.order, season.name);
            }
          }
        }
      }
    }

    // season titles
    for (TvSeason season : ListUtils.nullSafe(complete.seasons)) {
      if (season.season_number != null) {
        if (StringUtils.isAnyBlank(season.name, season.overview)) {
          // either one (or both) missing, fetch the translation endpoint to get all translations
          try {
            Response<Translations> translationsResponse = api.tvSeasonsService().translations(tmdbId, season.season_number).execute();
            if (!translationsResponse.isSuccessful()) {
              throw new HttpException(translationsResponse.code(), translationsResponse.message());
            }
            injectTranslations(Locale.forLanguageTag(language), complete, season, translationsResponse.body());
          }
          catch (Exception e) {
            LOGGER.debug("Could not get season translations - '{}'", e.getMessage());
          }
        }

        md.addSeasonName(MediaEpisodeGroup.DEFAULT_AIRED, season.season_number, season.name);
        md.addSeasonOverview(MediaEpisodeGroup.DEFAULT_AIRED, season.season_number, season.overview);
      }
    }

    // add some special keywords as tags
    // see http://forum.kodi.tv/showthread.php?tid=254004
    if (complete.keywords != null) {
      for (BaseKeyword kw : ListUtils.nullSafe(complete.keywords.keywords)) {
        md.addTag(kw.name);
      }
    }

    return md;
  }

  @Override
  public MediaMetadata getMetadata(@NotNull TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    // lazy initialization of the api
    initAPI();

    MediaMetadata md = new MediaMetadata(getId());
    md.setScrapeOptions(options);

    int showTmdbId = 0;
    int episodeTmdbId = options.getIdAsIntOrDefault(TMDB, 0);

    // every lookup in tmdb need to be done with the S/E number in AIRED order
    // any other order needs to be remapped to the aired order using the episodeGroup endpoint (every episode in an alternate order is linked to the
    // aired S/E)

    // ok, we have 2 flavors here:
    // a) we get the season and episode number -> everything is fine
    // b) we get the episode id (tmdb, imdb or tvdb) -> we need to do the lookup to get the season/episode number

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    // if we don't have season/episode yet but an imdb id -> do the find lookup
    if ((seasonNr == -1 || episodeNr == -1) && MediaIdUtil.isValidImdbId(options.getImdbId())) {
      try {
        BaseTvEpisode baseTvEpisode = getBaseTvEpisodeByImdbId(options.getImdbId());
        if (baseTvEpisode != null) {
          showTmdbId = MetadataUtil.unboxInteger(baseTvEpisode.show_id);
          seasonNr = MetadataUtil.unboxInteger(baseTvEpisode.season_number, -1);
          episodeNr = MetadataUtil.unboxInteger(baseTvEpisode.episode_number, -1);
        }
      }
      catch (Exception e) {
        LOGGER.debug("Could not get episode number by imdb id - {}", e.getMessage());
      }
    }

    // if we don't have season/episode yet but a tvdb id -> do the find lookup
    if ((seasonNr == -1 || episodeNr == -1) && options.getIds().containsKey(TVDB)) {
      try {
        BaseTvEpisode baseTvEpisode = getBaseTvEpisodeByTvdbId(options.getIdAsInt(TVDB));
        if (baseTvEpisode != null) {
          showTmdbId = MetadataUtil.unboxInteger(baseTvEpisode.show_id);
          seasonNr = MetadataUtil.unboxInteger(baseTvEpisode.season_number, -1);
          episodeNr = MetadataUtil.unboxInteger(baseTvEpisode.episode_number, -1);
        }
      }
      catch (Exception e) {
        LOGGER.debug("Could not get episode number by tvdb id - {}", e.getMessage());
      }
    }

    // get the tv show ids
    if (showTmdbId == 0) {
      TvShowSearchAndScrapeOptions tvShowSearchAndScrapeOptions = options.createTvShowSearchAndScrapeOptions();
      showTmdbId = tvShowSearchAndScrapeOptions.getTmdbId();

      // try to get via imdb id
      if (showTmdbId == 0 && MediaIdUtil.isValidImdbId(tvShowSearchAndScrapeOptions.getImdbId())) {
        try {
          showTmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.TV_SHOW, tvShowSearchAndScrapeOptions.getImdbId());
        }
        catch (Exception e) {
          LOGGER.debug("could not get tmdb id via imdb id - {}", e.getMessage());
        }
      }

      // try to get via tvdb id
      if (showTmdbId == 0 && tvShowSearchAndScrapeOptions.getIdAsIntOrDefault(TVDB, 0) > 0) {
        try {
          showTmdbId = TmdbUtils.getTmdbIdFromTvdbId(api, tvShowSearchAndScrapeOptions.getIdAsInteger(TVDB));
        }
        catch (Exception e) {
          LOGGER.debug("could not get tmdb id via tvdb id - {}", e.getMessage());
        }
      }
    }

    // no tmdb id, no scrape..
    if (showTmdbId == 0) {
      LOGGER.warn("not possible to scrape from TMDB - no TMDB/IMDB id found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    MediaMetadata episodeMediaMetadata = null;

    // get the main/translated EP data from the episodelist
    List<MediaMetadata> episodes = getEpisodeList(options.createTvShowSearchAndScrapeOptions());
    // match by ID
    if (episodeTmdbId > 0) {
      for (MediaMetadata episode : episodes) {
        if ((Integer) episode.getId(TMDB) == episodeTmdbId) {
          episodeMediaMetadata = episode;
          break;
        }
      }
    }

    // match by S/E
    if (episodeMediaMetadata == null && seasonNr > -1 && episodeNr > -1) {
      for (MediaMetadata episode : episodes) {
        MediaEpisodeNumber episodeNumber = episode.getEpisodeNumber(options.getEpisodeGroup());
        if (episodeNumber == null && options.getEpisodeGroup().getEpisodeGroupType() == AIRED) {
          // legacy
          episodeNumber = episode.getEpisodeNumber(AIRED);
        }
        if (episodeNumber == null) {
          continue;
        }

        if (seasonNr == episodeNumber.season() && episodeNr == episodeNumber.episode()) {
          episodeMediaMetadata = episode;
          break;
        }
      }
    }

    // match by first aired
    if (episodeMediaMetadata == null && options.getMetadata() != null && options.getMetadata().getReleaseDate() != null) {
      try {
        Format formatter = new SimpleDateFormat("yyyy-MM-dd");
        String aired = formatter.format(options.getMetadata().getReleaseDate());

        for (MediaMetadata episode : episodes) {
          if (episode.getReleaseDate() != null) {
            String airedEp = formatter.format(episode.getReleaseDate());

            if (aired.equals(airedEp)) {
              episodeMediaMetadata = episode;
              break;
            }
          }
        }

      }
      catch (Exception e) {
        LOGGER.debug("could not parse aired date - '{}'", e.getMessage());
      }
    }

    if (episodeMediaMetadata == null) {
      throw new NothingFoundException();
    }

    String language = getRequestLanguage(options.getLanguage());
    seasonNr = episodeMediaMetadata.getEpisodeNumber(AIRED).season();
    episodeNr = episodeMediaMetadata.getEpisodeNumber(AIRED).episode();

    // get the data from tmdb
    TvEpisode episode;
    try {
      // get full episode data
      Response<TvEpisode> episodeResponse = api.tvEpisodesService()
          .episode(showTmdbId, seasonNr, episodeNr, language,
              new AppendToResponse(AppendToResponseItem.EXTERNAL_IDS, AppendToResponseItem.CREDITS, AppendToResponseItem.IMAGES),
              Collections.singletonMap("include_image_language", "null"))
          .execute();

      if (!episodeResponse.isSuccessful()) {
        throw new HttpException(episodeResponse.code(), episodeResponse.message());
      }
      episode = episodeResponse.body();
    }
    catch (Exception e) {
      LOGGER.debug("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (episode == null) {
      throw new NothingFoundException();
    }

    md.setEpisodeNumbers(episodeMediaMetadata.getEpisodeNumbers());
    md.setId(getId(), episode.id);

    parseExternalIDs(episode.external_ids).forEach(md::setId);

    md.setTitle(episodeMediaMetadata.getTitle());
    md.setOriginalTitle(episodeMediaMetadata.getOriginalTitle());
    md.setPlot(episodeMediaMetadata.getPlot());

    if (MetadataUtil.unboxInteger(episode.vote_count, 0) > 0) {
      try {
        MediaRating rating = new MediaRating(MediaMetadata.TMDB);
        rating.setRating(MetadataUtil.unboxDouble(episode.vote_average));
        rating.setVotes(MetadataUtil.unboxInteger(episode.vote_count));
        rating.setMaxValue(10);
        md.addRating(rating);
      }
      catch (Exception e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }
    }

    md.setReleaseDate(episode.air_date);

    md.setCastMembers(episodeMediaMetadata.getCastMembers()); // already contains season cast

    // episode guests
    for (CastMember castMember : ListUtils.nullSafe(episode.guest_stars)) {
      Person cm = new Person(Person.Type.GUEST);
      cm.setId(getProviderInfo().getId(), castMember.id);
      cm.setName(castMember.name);
      cm.setRole(castMember.character);
      if (castMember.id != null) {
        cm.setProfileUrl("https://www.themoviedb.org/person/" + castMember.id);
      }

      if (StringUtils.isNotBlank(castMember.profile_path)) {
        cm.setThumbUrl(artworkBaseUrl + "h632" + castMember.profile_path);
      }

      md.addCastMember(cm);
    }

    // crew
    for (CrewMember crewMember : ListUtils.nullSafe(episode.crew)) {
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

    // Thumbs
    if (episode.images != null && ListUtils.isNotEmpty(episode.images.stills)) {
      for (Image image : episode.images.stills) {
        MediaArtwork ma = new MediaArtwork(getId(), MediaArtworkType.THUMB);
        ma.setPreviewUrl(artworkBaseUrl + "w300" + image.file_path);
        ma.setOriginalUrl(artworkBaseUrl + "original" + image.file_path);

        // add different sizes
        // original (most of the time 1920x1080)
        ma.addImageSize(image.width, image.height, artworkBaseUrl + "original" + image.file_path, MediaArtwork.ThumbSizes.getSizeOrder(image.width));
        // 1280x720
        if (1280 < image.width) {
          ma.addImageSize(1280, image.height * 1280 / image.width, artworkBaseUrl + "w1280" + image.file_path,
              MediaArtwork.ThumbSizes.getSizeOrder(1280));
        }
        // w300
        if (300 < image.width) {
          ma.addImageSize(300, image.height * 300 / image.width, artworkBaseUrl + "w300" + image.file_path,
              MediaArtwork.ThumbSizes.getSizeOrder(300));
        }

        md.addMediaArt(ma);
      }
    }
    else if (StringUtils.isNotBlank(episode.still_path)) {
      MediaArtwork ma = new MediaArtwork(getId(), MediaArtworkType.THUMB);
      ma.setPreviewUrl(artworkBaseUrl + "w300" + episode.still_path);
      ma.setOriginalUrl(artworkBaseUrl + "original" + episode.still_path);
      ma.addImageSize(0, 0, artworkBaseUrl + "original" + episode.still_path, 0);
      md.addMediaArt(ma);
    }

    return md;
  }

  @Override
  public List<MediaRating> getRatings(Map<String, Object> ids, MediaType mediaType) throws ScrapeException {
    if (mediaType == MediaType.TV_SHOW) {
      return getTvShowRatings(ids);
    }
    else if (mediaType == MediaType.TV_EPISODE) {
      return getEpisodeRatings(ids);
    }
    else {
      return Collections.emptyList();
    }
  }

  private List<MediaRating> getTvShowRatings(Map<String, Object> ids) throws ScrapeException {
    LOGGER.debug("getRatings(): {}", ids);

    // lazy initialization of the api
    initAPI();

    int tmdbId = MediaIdUtil.getIdAsInt(ids, MediaMetadata.TMDB);
    String imdbId = MediaIdUtil.getIdAsString(ids, MediaMetadata.IMDB);

    // try to get via imdb id
    if (tmdbId == 0 && MediaIdUtil.isValidImdbId(imdbId)) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.TV_SHOW, imdbId);
      }
      catch (Exception e) {
        LOGGER.debug("could not get tmdb id via imdb id - {}", e.getMessage());
      }
    }

    // try to get via tvdb id
    if (tmdbId == 0 && MediaIdUtil.getIdAsInt(ids, MediaMetadata.TVDB) > 0) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromTvdbId(api, MediaIdUtil.getIdAsInt(ids, MediaMetadata.TVDB));
      }
      catch (Exception e) {
        LOGGER.debug("could not get tmdb id via tvdb id - {}", e.getMessage());
      }
    }

    // no tmdb id, no scrape..
    if (tmdbId == 0) {
      LOGGER.warn("not possible to scrape from TMDB - no tmdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    TvShow complete;
    try {
      Response<TvShow> httpResponse = api.tvService().tv(tmdbId, "en", null).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      complete = httpResponse.body();
    }
    catch (Exception e) {
      LOGGER.debug("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (complete == null) {
      throw new NothingFoundException();
    }

    if (complete.vote_average != null && complete.vote_count != null) {
      try {
        MediaRating rating = new MediaRating(MediaMetadata.TMDB);
        rating.setRating(complete.vote_average);
        rating.setVotes(complete.vote_count);
        rating.setMaxValue(10);
        return Collections.singletonList(rating);
      }
      catch (Exception e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }
    }

    return Collections.emptyList();
  }

  private List<MediaRating> getEpisodeRatings(Map<String, Object> ids) throws ScrapeException {
    LOGGER.debug("getRatings(): {}", ids);

    Map<String, Object> showIds = new HashMap<>();
    int showTmdbId = 0;
    int episodeTmdbId = MediaIdUtil.getIdAsIntOrDefault(ids, TMDB, 0);

    try {
      showIds.putAll((Map<? extends String, ?>) ids.get(MediaMetadata.TVSHOW_IDS));
    }
    catch (Exception e) {
      LOGGER.debug("could not get TV show ids - '{}'", e.getMessage());
    }

    // ok, we have 2 flavors here:
    // a) we get the season and episode number -> everything is fine
    // b) we get the episode id (tmdb, imdb or tvdb) -> we need to do the lookup to get the season/episode number

    // get episode number and season number
    int seasonNr = MediaIdUtil.getIdAsIntOrDefault(ids, MediaMetadata.SEASON_NR, -1);
    int episodeNr = MediaIdUtil.getIdAsIntOrDefault(ids, MediaMetadata.EPISODE_NR, -1);

    // if we don't have season/episode yet but an imdb id -> do the find lookup
    if ((seasonNr == -1 || episodeNr == -1) && MediaIdUtil.isValidImdbId(MediaIdUtil.getIdAsString(ids, IMDB))) {
      try {
        BaseTvEpisode baseTvEpisode = getBaseTvEpisodeByImdbId(MediaIdUtil.getIdAsString(ids, IMDB));
        if (baseTvEpisode != null) {
          showTmdbId = MetadataUtil.unboxInteger(baseTvEpisode.show_id);
          seasonNr = MetadataUtil.unboxInteger(baseTvEpisode.season_number, -1);
          episodeNr = MetadataUtil.unboxInteger(baseTvEpisode.episode_number, -1);
        }
      }
      catch (Exception e) {
        LOGGER.debug("Could not get episode number by imdb id - {}", e.getMessage());
      }
    }

    // if we don't have season/episode yet but a tvdb id -> do the find lookup
    if ((seasonNr == -1 || episodeNr == -1) && MediaIdUtil.getIdAsInt(ids, TVDB) > 0) {
      try {
        BaseTvEpisode baseTvEpisode = getBaseTvEpisodeByTvdbId(MediaIdUtil.getIdAsInt(ids, TVDB));
        if (baseTvEpisode != null) {
          showTmdbId = MetadataUtil.unboxInteger(baseTvEpisode.show_id);
          seasonNr = MetadataUtil.unboxInteger(baseTvEpisode.season_number, -1);
          episodeNr = MetadataUtil.unboxInteger(baseTvEpisode.episode_number, -1);
        }
      }
      catch (Exception e) {
        LOGGER.debug("Could not get episode number by tvdb id - {}", e.getMessage());
      }
    }

    // get the tv show ids
    if (showTmdbId == 0 && ids.get(MediaMetadata.TVSHOW_IDS) instanceof Map) {
      showTmdbId = MediaIdUtil.getIdAsInt(showIds, TMDB);

      // try to get via imdb id
      if (showTmdbId == 0 && MediaIdUtil.isValidImdbId(MediaIdUtil.getIdAsString(showIds, IMDB))) {
        try {
          showTmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.TV_SHOW, MediaIdUtil.getIdAsString(showIds, IMDB));
        }
        catch (Exception e) {
          LOGGER.debug("could not get tmdb id via imdb id - {}", e.getMessage());
        }
      }

      // try to get via tvdb id
      if (showTmdbId == 0 && MediaIdUtil.getIdAsInt(showIds, TVDB) > 0) {
        try {
          showTmdbId = TmdbUtils.getTmdbIdFromTvdbId(api, MediaIdUtil.getIdAsInt(showIds, TVDB));
        }
        catch (Exception e) {
          LOGGER.debug("could not get tmdb id via tvdb id - {}", e.getMessage());
        }
      }
    }

    // no tmdb id, no scrape..
    if (showTmdbId == 0) {
      LOGGER.warn("not possible to scrape from TMDB - no tmdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    // get episode via episode listing -> improves caching performance
    MediaMetadata episodeFromListing = null;
    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setIds(showIds);

    List<MediaMetadata> episodes = getEpisodeList(options);
    // first match via TMDB id
    if (episodeTmdbId > 0) {
      for (MediaMetadata episode : episodes) {
        if (episodeTmdbId == episode.getIdAsIntOrDefault(TMDB, 0)) {
          episodeFromListing = episode;
          break;
        }
      }
    }

    if (episodeFromListing == null) {
      // match by aired S/E
      for (MediaMetadata episode : episodes) {
        MediaEpisodeNumber episodeNumber = episode.getEpisodeNumber(AIRED);
        if (episodeNumber == null) {
          continue;
        }

        if (seasonNr == episodeNumber.season() && episodeNr == episodeNumber.episode()) {
          episodeFromListing = episode;
          break;
        }
      }
    }

    if (episodeFromListing == null) {
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(episodeFromListing.getRatings());
  }

  @Override
  public Map<String, Object> getMediaIds(Map<String, Object> ids, MediaType mediaType) throws ScrapeException {
    if (mediaType != MediaType.TV_SHOW) {
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
        tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.TV_SHOW, imdbId);
      }
      catch (Exception e) {
        LOGGER.debug("problem getting tmdbId from imdbId: {}", e.getMessage());
      }
    }

    if (tmdbId == 0) {
      LOGGER.debug("not possible to scrape from TMDB - no tmdbId/imdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    // scrape
    TvShow tvShow;

    try {
      Response<TvShow> httpResponse = api.tvService().tv(tmdbId, "en", new AppendToResponse(AppendToResponseItem.EXTERNAL_IDS)).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      tvShow = httpResponse.body();
    }
    catch (Exception e) {
      LOGGER.debug("problem getting data from tmdb: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (tvShow == null) {
      throw new NothingFoundException();
    }

    scrapedIds.put(TMDB, tvShow.id);
    // external IDs
    parseExternalIDs(tvShow.external_ids).forEach(scrapedIds::put);

    return scrapedIds;
  }

  private BaseTvEpisode getBaseTvEpisodeByImdbId(String imdbId) throws IOException {
    FindResults findResults = api.findService().find(imdbId, ExternalSource.IMDB_ID, null).execute().body();
    if (findResults != null && ListUtils.isNotEmpty(findResults.tv_episode_results)) {
      return findResults.tv_episode_results.get(0);
    }

    return null;
  }

  private BaseTvEpisode getBaseTvEpisodeByTvdbId(int tvdbId) throws IOException {
    FindResults findResults = api.findService().find(tvdbId, ExternalSource.TVDB_ID, null).execute().body();
    if (findResults != null && ListUtils.isNotEmpty(findResults.tv_episode_results)) {
      return findResults.tv_episode_results.get(0);
    }

    return null;
  }

  /**
   * Fallback Language Mechanism - for direct TMDB lookup<br>
   * Title/Overview always gets returned in the original language, if translation has not been found.<br>
   * So we never know exactly what is missing.. so we just inject everything here by hand if a fallback language has been found
   */
  private void injectTranslations(Locale language, TvShow show) {
    if (Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("titleFallback"))) {
      Locale fallbackLanguage = Locale.forLanguageTag(getProviderInfo().getConfig().getValue("titleFallbackLanguage"));
      // get in desired localization
      String[] val = getValuesFromTranslation(show.translations, language);

      // if the search language equals the original language of the movie, there may be no translation
      if (StringUtils.isBlank(val[0]) && language.getLanguage().equals(show.original_language)) {
        val[0] = show.original_name;
      }

      // merge empty ones with fallback
      String[] temp = getValuesFromTranslation(show.translations, fallbackLanguage);
      if (StringUtils.isBlank(val[0])) {
        val[0] = temp[0];
      }
      if (StringUtils.isBlank(val[1])) {
        val[1] = temp[1];
      }

      // finally SET the values
      if (StringUtils.isNotBlank(val[0])) {
        show.name = val[0];
      }
      if (StringUtils.isNotBlank(val[1])) {
        show.overview = val[1];
      }
    }
  }

  /**
   * Fallback Language Mechanism - for direct TMDB lookup<br>
   * Title/Overview always gets returned in the original language, if translation has not been found.<br>
   * So we never know exactly what is missing.. so we just inject everything here by hand if a fallback language has been found
   */
  private void injectTranslations(Locale language, TvShow tvShow, TvSeason tvSeason, Translations translations) {
    if (Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("titleFallback"))) {
      Locale fallbackLanguage = Locale.forLanguageTag(getProviderInfo().getConfig().getValue("titleFallbackLanguage"));
      // get in desired localization
      String[] val = getValuesFromTranslation(translations, language);

      // if the search language equals the original language of the movie, there may be no translation
      if (StringUtils.isBlank(val[0]) && language.getLanguage().equals(tvShow.original_language)) {
        val[0] = tvSeason.name;
      }

      // merge empty ones with fallback
      String[] temp = getValuesFromTranslation(translations, fallbackLanguage);
      if (StringUtils.isBlank(val[0])) {
        val[0] = temp[0];
      }
      if (StringUtils.isBlank(val[1])) {
        val[1] = temp[1];
      }

      // finally SET the values
      if (StringUtils.isNotBlank(val[0])) {
        tvSeason.name = val[0];
      }
      if (StringUtils.isNotBlank(val[1])) {
        tvSeason.overview = val[1];
      }
    }
  }

  /**
   * Fallback Language Mechanism - For IMDB & searches
   *
   * @param language
   *          the language to search for
   * @param show
   *          the show
   * @throws IOException
   *           any IOException occurred
   */
  private void injectTranslations(Locale language, BaseTvShow show) throws IOException {
    // NOT doing a fallback scrape when overview empty, used only for SEARCH - unneeded!
    if (Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("titleFallback"))) {
      Locale fallbackLanguage = Locale.forLanguageTag(getProviderInfo().getConfig().getValue("titleFallbackLanguage"));

      // tmdb provides title = originalTitle if no title in the requested language has been found,
      // so get the title in a alternative language
      if ((show.name.equals(show.original_name) && !show.original_language.equals(language.getLanguage())) && !language.equals(fallbackLanguage)) {
        LOGGER.debug("checking for title fallback {}", fallbackLanguage);
        String lang = getProviderInfo().getConfig().getValue("titleFallbackLanguage").replace("_", "-");
        Response<TvShow> httpResponse = api.tvService().tv(show.id, lang, new AppendToResponse(AppendToResponseItem.TRANSLATIONS)).execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        TvShow s = httpResponse.body();

        // get in desired localization
        String[] val = getValuesFromTranslation(s.translations, language);

        // merge empty ones with fallback
        String[] temp = getValuesFromTranslation(s.translations, fallbackLanguage);
        if (StringUtils.isBlank(val[0])) {
          val[0] = temp[0];
        }
        if (StringUtils.isBlank(val[1])) {
          val[1] = temp[1];
        }

        // finally SET the values
        if (StringUtils.isNotBlank(val[0])) {
          show.name = val[0];
        }
        if (StringUtils.isNotBlank(val[1])) {
          show.overview = val[1];
        }
      }
    }
  }

  private MediaMetadata morphTvEpisodeToMediaMetadata(BaseTvEpisode episode, TvSeason tvSeason,
      Map<MediaEpisodeGroup, List<TvEpisodeGroupSeason>> episodeGroups, MediaSearchAndScrapeOptions options) {
    MediaMetadata ep = new MediaMetadata(getId());
    ep.setScrapeOptions(options);
    ep.setId(getProviderInfo().getId(), episode.id);
    ep.setEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, episode.season_number, episode.episode_number);
    ep.addEpisodeGroup(MediaEpisodeGroup.DEFAULT_AIRED);
    ep.setTitle(episode.name);
    ep.setPlot(episode.overview);

    // mix in episode groups
    if (episodeGroups != null) {
      for (Map.Entry<MediaEpisodeGroup, List<TvEpisodeGroupSeason>> entry : episodeGroups.entrySet()) {
        boolean found = false;

        for (TvEpisodeGroupSeason episodeGroup : entry.getValue()) {
          for (TvEpisodeGroupEpisode episodeInGroup : ListUtils.nullSafe(episodeGroup.episodes)) {
            if (Objects.equals(episode.id, episodeInGroup.id)) {
              ep.setEpisodeNumber(entry.getKey(), episodeGroup.order, episodeInGroup.order + 1); // +1 because TMDB counts episodes from zero!
              ep.addEpisodeGroup(entry.getKey());
              found = true;
              break;
            }
          }

          if (found) {
            break;
          }
        }
      }
    }

    // ratings
    if (episode.vote_average != null && MetadataUtil.unboxInteger(episode.vote_count, 0) > 0) {
      MediaRating rating = new MediaRating(getProviderInfo().getId());
      rating.setRating(episode.vote_average);
      rating.setVotes(episode.vote_count);
      rating.setMaxValue(10);
      ep.addRating(rating);
    }

    ep.setReleaseDate(episode.air_date);

    // credits
    if (tvSeason != null && tvSeason.credits != null) {
      // season cast
      for (CastMember castMember : ListUtils.nullSafe(tvSeason.credits.cast)) {
        Person cm = new Person(ACTOR);
        cm.setId(getProviderInfo().getId(), castMember.id);
        cm.setName(castMember.name);
        cm.setRole(castMember.character);
        if (castMember.id != null) {
          cm.setProfileUrl("https://www.themoviedb.org/person/" + castMember.id);
        }

        if (StringUtils.isNotBlank(castMember.profile_path)) {
          cm.setThumbUrl(artworkBaseUrl + "h632" + castMember.profile_path);
        }

        ep.addCastMember(cm);
      }

      // season crew
      for (CrewMember crewMember : ListUtils.nullSafe(tvSeason.credits.crew)) {
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

        ep.addCastMember(cm);
      }
    }

    return ep;
  }

  private MediaSearchResult morphTvShowToSearchResult(BaseTvShow tvShow, TvShowSearchAndScrapeOptions query) {

    MediaSearchResult result = new MediaSearchResult(getId(), MediaType.TV_SHOW);
    result.setId(Integer.toString(tvShow.id));
    result.setTitle(tvShow.name);
    result.setOriginalTitle(tvShow.original_name);
    result.setOverview(tvShow.overview);

    if (tvShow.poster_path != null && !tvShow.poster_path.isEmpty()) {
      result.setPosterUrl(artworkBaseUrl + "w342" + tvShow.poster_path);
    }

    // parse release date to year
    if (tvShow.first_air_date != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(tvShow.first_air_date);
      result.setYear(calendar.get(Calendar.YEAR));
    }

    // calculate score
    if ((StringUtils.isNotBlank(query.getImdbId()) && query.getImdbId().equals(result.getIMDBId()))
        || String.valueOf(query.getTmdbId()).equals(result.getId())) {
      LOGGER.debug("perfect match by ID - set score to 1");
      result.setScore(1);
    }
    else {
      // calculate the score by comparing the search result with the search options
      result.calculateScore(query);
    }

    return result;
  }

  private MediaEpisodeGroup.EpisodeGroupType mapEpisodeGroup(int type) {
    return switch (type) {
      // originally aired, but this results in a _second_ aired order which we cannot handle. We morph this is to alternate
      case 1 -> MediaEpisodeGroup.EpisodeGroupType.ALTERNATE;
      case 2 -> MediaEpisodeGroup.EpisodeGroupType.ABSOLUTE;
      case 3 -> MediaEpisodeGroup.EpisodeGroupType.DVD;
      case 4, 5, 6, 7 -> MediaEpisodeGroup.EpisodeGroupType.ALTERNATE;
      default -> null;
    };
  }
}
