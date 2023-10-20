/*
 * Copyright 2012 - 2023 Manuel Laggner
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
package org.tinymediamanager.scraper.thetvdb;

import static org.tinymediamanager.scraper.MediaMetadata.TVDB;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroup.ABSOLUTE;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroup.AIRED;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroup.DVD;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaIdProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowTvdbMetadataProvider;
import org.tinymediamanager.scraper.thetvdb.entities.ArtworkExtendedRecord;
import org.tinymediamanager.scraper.thetvdb.entities.ArtworkTypeRecord;
import org.tinymediamanager.scraper.thetvdb.entities.Company;
import org.tinymediamanager.scraper.thetvdb.entities.ContentRating;
import org.tinymediamanager.scraper.thetvdb.entities.EpisodeBaseRecord;
import org.tinymediamanager.scraper.thetvdb.entities.EpisodeExtendedRecord;
import org.tinymediamanager.scraper.thetvdb.entities.EpisodeExtendedResponse;
import org.tinymediamanager.scraper.thetvdb.entities.GenreBaseRecord;
import org.tinymediamanager.scraper.thetvdb.entities.SearchResultRecord;
import org.tinymediamanager.scraper.thetvdb.entities.SearchResultResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SearchType;
import org.tinymediamanager.scraper.thetvdb.entities.SeasonBaseRecord;
import org.tinymediamanager.scraper.thetvdb.entities.SeasonType;
import org.tinymediamanager.scraper.thetvdb.entities.SeasonTypeRecord;
import org.tinymediamanager.scraper.thetvdb.entities.SeriesEpisodesRecord;
import org.tinymediamanager.scraper.thetvdb.entities.SeriesEpisodesResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SeriesExtendedRecord;
import org.tinymediamanager.scraper.thetvdb.entities.SeriesExtendedResponse;
import org.tinymediamanager.scraper.thetvdb.entities.Translation;
import org.tinymediamanager.scraper.thetvdb.entities.TranslationResponse;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.StrgUtils;

import retrofit2.Response;

/**
 * the class {@link TheTvDbTvShowMetadataProvider} offers the metadata provider for TheTvDb
 *
 * @author Manuel Laggner
 */
public class TheTvDbTvShowMetadataProvider extends TheTvDbMetadataProvider
    implements ITvShowMetadataProvider, ITvShowTvdbMetadataProvider, IMediaIdProvider {
  private static final Logger                                LOGGER                 = LoggerFactory.getLogger(TheTvDbTvShowMetadataProvider.class);

  private static final CacheMap<String, List<MediaMetadata>> EPISODE_LIST_CACHE_MAP = new CacheMap<>(600, 5);
  private static final CacheMap<String, MediaMetadata>       EPISODE_CACHE_MAP      = new CacheMap<>(600, 5);

  private static final MediaEpisodeGroup                     ALTERNATE              = new MediaEpisodeGroup(MediaEpisodeGroup.EpisodeGroup.ALTERNATE);

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = super.createMediaProviderInfo();

    info.getConfig().addText("apiKey", "", true);
    info.getConfig().addText("pin", "", true);

    ArrayList<String> fallbackLanguages = new ArrayList<>();
    for (MediaLanguages mediaLanguages : MediaLanguages.values()) {
      fallbackLanguages.add(mediaLanguages.toString());
    }
    info.getConfig().addSelect(FALLBACK_LANGUAGE, fallbackLanguages.toArray(new String[0]), MediaLanguages.en.toString());
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
  public MediaMetadata getMetadata(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {
    // lazy initialization of the api
    initAPI();

    LOGGER.debug("getMetadata(): {}", options);
    MediaMetadata md = new MediaMetadata(getId());
    md.setScrapeOptions(options);

    // do we have an id from the options?
    int id = options.getIdAsInt(getId());
    if (id == 0 && MediaIdUtil.isValidImdbId(options.getImdbId())) {
      id = getTvdbIdViaImdbId(options.getImdbId());
    }
    if (id == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(getId());
    }

    SeriesExtendedRecord show;
    Translation baseTranslation = null;
    Translation fallbackTranslation = null;

    // language in 3 char
    String baseLanguage = LanguageUtils.getIso3Language(options.getLanguage().toLocale());
    String fallbackLanguage = LanguageUtils.getIso3Language(MediaLanguages.get(getProviderInfo().getConfig().getValue(FALLBACK_LANGUAGE)).toLocale());
    // pt-BR is pt at tvdb...
    if ("pob".equals(baseLanguage)) {
      baseLanguage = "pt";
    }
    if ("pob".equals(fallbackLanguage)) {
      fallbackLanguage = "pt";
    }

    try {
      Response<SeriesExtendedResponse> httpResponse = tvdb.getSeriesService().getSeriesExtended(id).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      show = httpResponse.body().data;

      // base translation (needed for overview)
      if (show.nameTranslations.contains(baseLanguage) || show.overviewTranslations.contains(baseLanguage)) {
        Response<TranslationResponse> translationResponse = tvdb.getSeriesService().getSeriesTranslation(id, baseLanguage).execute();
        if (translationResponse.isSuccessful()) {
          baseTranslation = translationResponse.body().data;
        }
      }

      // also get fallback is either title or overview of the base translation is missing
      if ((baseTranslation == null || StringUtils.isAnyBlank(baseTranslation.name, baseTranslation.overview))
          && (show.nameTranslations.contains(fallbackLanguage) || show.overviewTranslations.contains(fallbackLanguage))) {
        Response<TranslationResponse> translationResponse = tvdb.getSeriesService().getSeriesTranslation(id, fallbackLanguage).execute();
        if (translationResponse.isSuccessful()) {
          fallbackTranslation = translationResponse.body().data;
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    // populate metadata
    md.setId(getId(), show.id);
    parseRemoteIDs(show.remoteIds).forEach((k, v) -> {
      md.setId(k, v);
    });

    if (baseTranslation != null && StringUtils.isNotBlank(baseTranslation.name)) {
      md.setTitle(baseTranslation.name);
    }
    else if (fallbackTranslation != null && StringUtils.isNotBlank(fallbackTranslation.overview)) {
      md.setTitle(fallbackTranslation.name);
    }
    else {
      md.setTitle(show.name);
    }

    md.setOriginalTitle(show.name);

    if (baseTranslation != null && StringUtils.isNotBlank(baseTranslation.overview)) {
      md.setPlot(baseTranslation.overview);
    }
    else if (fallbackTranslation != null && StringUtils.isNotBlank(fallbackTranslation.overview)) {
      md.setPlot(fallbackTranslation.overview);
    }

    try {
      md.setReleaseDate(StrgUtils.parseDate(show.firstAired));
    }
    catch (ParseException e) {
      LOGGER.debug("could not parse date: {}", e.getMessage());
    }

    try {
      Date date = StrgUtils.parseDate(show.firstAired);
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      int y = calendar.get(Calendar.YEAR);
      md.setYear(y);
      if (y != 0 && md.getTitle().contains(String.valueOf(y))) {
        LOGGER.debug("Weird TVDB entry - removing date {} from title", y);
        md.setTitle(clearYearFromTitle(md.getTitle(), y));
      }
    }
    catch (Exception e) {
      LOGGER.debug("could not parse date: {}", e.getMessage());
    }

    if (show.status != null && show.status.id != null) {
      if (show.status.id == 1) {
        md.setStatus(MediaAiredStatus.CONTINUING);
      }
      else if (show.status.id == 2) {
        md.setStatus(MediaAiredStatus.ENDED);
      }
    }

    md.setRuntime(MetadataUtil.unboxInteger(show.averageRuntime, 0));

    // scrape networks before all other production companies
    if (show.originalNetwork != null) {
      md.addProductionCompany(show.originalNetwork.name);
    }
    if (show.latestNetwork != null) {
      md.addProductionCompany(show.latestNetwork.name);
    }
    for (Company company : ListUtils.nullSafe(show.companies)) {
      md.addProductionCompany(company.name);
    }

    if (show.characters != null) {
      for (Person member : parseCastMembers(show.characters.stream().filter(character -> character.episodeId == null).collect(Collectors.toList()))) {
        md.addCastMember(member);
      }
    }

    // genres
    for (GenreBaseRecord genreBaseRecord : ListUtils.nullSafe(show.genres)) {
      md.addGenre(MediaGenres.getGenre(genreBaseRecord.name));
    }

    // certifications
    for (ContentRating contentRating : ListUtils.nullSafe(show.contentRatings)) {
      if (options.getCertificationCountry().getAlpha3().equalsIgnoreCase(contentRating.country)) {
        MediaCertification mediaCertification = MediaCertification.findCertification(contentRating.name);
        if (mediaCertification != MediaCertification.UNKNOWN) {
          md.addCertification(mediaCertification);
        }
      }
    }

    // season names/plot
    for (SeasonBaseRecord season : show.seasons) {
      baseTranslation = null;
      fallbackTranslation = null;
      try {
        Response<TranslationResponse> translationResponse = null;
        if (fakeListToList(season.nameTranslations).contains(baseLanguage) || fakeListToList(season.overviewTranslations).contains(baseLanguage)) {
          translationResponse = tvdb.getSeasonsService().getSeasonTranslation(season.id, baseLanguage).execute();
          if (translationResponse.isSuccessful()) {
            baseTranslation = translationResponse.body().data;
          }
        }
        // also get fallback is either title or overview of the base translation is missing
        if ((baseTranslation == null || StringUtils.isAnyBlank(baseTranslation.name, baseTranslation.overview))
            && (fakeListToList(season.nameTranslations).contains(fallbackLanguage)
                || fakeListToList(season.overviewTranslations).contains(fallbackLanguage))) {
          translationResponse = tvdb.getSeasonsService().getSeasonTranslation(season.id, fallbackLanguage).execute();
          if (translationResponse.isSuccessful()) {
            fallbackTranslation = translationResponse.body().data;
          }
        }
        // season title
        if (baseTranslation != null && StringUtils.isNotBlank(baseTranslation.name)) {
          md.addSeasonName(season.number, baseTranslation.name);
        }
        else if (fallbackTranslation != null && StringUtils.isNotBlank(fallbackTranslation.overview)) {
          md.addSeasonName(season.number, fallbackTranslation.name);
        }
        // season overview
        if (baseTranslation != null && StringUtils.isNotBlank(baseTranslation.overview)) {
          md.addSeasonOverview(season.number, baseTranslation.overview);
        }
        else if (fallbackTranslation != null && StringUtils.isNotBlank(fallbackTranslation.overview)) {
          md.addSeasonOverview(season.number, fallbackTranslation.overview);
        }
      }
      catch (IOException e) {
        LOGGER.error("failed to get season meta data: {}", e.getMessage());
        throw new ScrapeException(e);
      }

    }

    // episode groups
    for (SeasonTypeRecord seasonTypeRecord : ListUtils.nullSafe(show.seasonTypes)) {
      boolean seasonAvailable = false;

      // first check, if there is any episode mapped in this season type
      for (SeasonBaseRecord seasonBaseRecord : ListUtils.nullSafe(show.seasons)) {
        if (seasonBaseRecord.type.type == seasonTypeRecord.type) {
          seasonAvailable = true;
          break;
        }
      }

      if (seasonAvailable) {
        MediaEpisodeGroup.EpisodeGroup episodeGroup = mapEpisodeGroup(seasonTypeRecord.type);
        if (episodeGroup != null) {
          md.addEpisodeGroup(new MediaEpisodeGroup(episodeGroup, seasonTypeRecord.name));
        }
      }
    }

    // artwork
    for (ArtworkExtendedRecord artworkBaseRecord : ListUtils.nullSafe(show.artworks)) {
      MediaArtwork mediaArtwork = parseArtwork(artworkBaseRecord);
      if (mediaArtwork != null) {
        md.addMediaArt(mediaArtwork);
      }
    }

    return md;
  }

  /**
   * TVDB does return season translations, as a list with ONE entry, comma separated. W.T.F.?!???
   * 
   * @param entries
   * @return
   */
  private List<String> fakeListToList(List<String> entries) {
    ArrayList<String> ret = new ArrayList<>();
    if (entries == null) {
      return ret;
    }
    for (String entry : entries) {
      ret.addAll(Stream.of(entry.split(",", -1)).collect(Collectors.toList()));
    }
    return ret;
  }

  @Override
  public MediaMetadata getMetadata(@NotNull TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    // lazy initialization of the api
    initAPI();

    LOGGER.debug("getMetadata(): {}", options);

    // do we have an id from the options?
    int showId = options.createTvShowSearchAndScrapeOptions().getIdAsIntOrDefault(getId(), 0);

    if (showId == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(getId());
    }

    int episodeTvdbId = options.getIdAsIntOrDefault(TVDB, 0);

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);
    MediaEpisodeGroup episodeGroup = options.getEpisodeGroup();

    Date releaseDate = null;
    if (options.getMetadata() != null && options.getMetadata().getReleaseDate() != null) {
      releaseDate = options.getMetadata().getReleaseDate();
    }
    if (releaseDate == null && (seasonNr == -1 || episodeNr == -1) && episodeTvdbId == 0) {
      LOGGER.warn("no aired date/season number/episode number found");
      throw new MissingIdException(MediaMetadata.EPISODE_NR);
    }

    // get the episode via the episodesList() (is cached and contains all data with 1 call per 100 eps)
    List<MediaMetadata> episodes = getEpisodeList(options.createTvShowSearchAndScrapeOptions());

    // now search for the right episode in this list
    MediaMetadata foundEpisode = null;
    // first run - search with EP number
    if (episodeTvdbId > 0) {
      for (MediaMetadata episode : episodes) {
        if (episodeTvdbId == episode.getIdAsIntOrDefault(TVDB, 0)) {
          foundEpisode = episode;
          break;
        }
      }
    }

    // search with S/E
    if (foundEpisode == null) {
      for (MediaMetadata episode : episodes) {
        MediaEpisodeNumber episodeNumber = episode.getEpisodeNumber(episodeGroup);
        if (episodeNumber == null && episodeGroup.getEpisodeGroup() == AIRED) {
          // legacy
          episodeNumber = episode.getEpisodeNumber(AIRED);
        }
        if (episodeNumber != null && episodeNumber.season() == seasonNr && episodeNumber.episode() == episodeNr) {
          foundEpisode = episode;
          break;
        }
      }
    }

    // search with date
    if (foundEpisode == null && releaseDate != null) {
      // we did not find the episode via season/episode number - search via release date
      for (MediaMetadata episode : episodes) {
        if (episode.getReleaseDate() == releaseDate) {
          foundEpisode = episode;
          break;
        }
      }
    }

    if (foundEpisode == null) {
      throw new NothingFoundException();
    }

    // look in the cache map if there is an entry
    try {
      MediaMetadata cachedEpisode = EPISODE_CACHE_MAP.get(foundEpisode.getId(getId()) + "_" + options.getLanguage().getLanguage());
      if (cachedEpisode != null) {
        // cache hit!
        return cachedEpisode;
      }
    }
    catch (Exception ignored) {
      // ignore
    }

    EpisodeExtendedRecord episode;

    try {
      int id = (int) foundEpisode.getId(getId());

      Response<EpisodeExtendedResponse> httpResponse = tvdb.getEpisodesService().getEpisodeExtended(id).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }

      episode = httpResponse.body().data;
    }
    catch (Exception e) {
      LOGGER.error("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    // enrich the data
    MediaMetadata md = new MediaMetadata(getId());
    md.setScrapeOptions(options);
    md.setId(getId(), episode.id);
    parseRemoteIDs(episode.remoteIds).forEach((k, v) -> {
      md.setId(k, v);
    });

    md.setEpisodeNumber(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, episode.seasonNumber, episode.number));
    if (foundEpisode.getEpisodeNumber(DVD) != null) {
      md.setEpisodeNumber(foundEpisode.getEpisodeNumber(DVD));
    }
    if (foundEpisode.getEpisodeNumber(ABSOLUTE) != null) {
      md.setEpisodeNumber(foundEpisode.getEpisodeNumber(ABSOLUTE));
    }

    if (MetadataUtil.unboxInteger(episode.airsBeforeSeason, -1) > -1 || MetadataUtil.unboxInteger(episode.airsBeforeEpisode, -1) > -1) {
      md.setEpisodeNumber(MediaEpisodeGroup.DEFAULT_DISPLAY, MetadataUtil.unboxInteger(episode.airsBeforeSeason),
          MetadataUtil.unboxInteger(episode.airsBeforeEpisode));
    }
    if (MetadataUtil.unboxInteger(episode.airsAfterSeason, -1) > -1) {
      md.setEpisodeNumber(MediaEpisodeGroup.DEFAULT_DISPLAY, MetadataUtil.unboxInteger(episode.airsAfterSeason), 4096); // like emm
    }

    // we get all translations from the episodelist
    md.setTitle(foundEpisode.getTitle());
    md.setOriginalTitle(foundEpisode.getOriginalTitle());
    md.setPlot(foundEpisode.getPlot());

    if (episode.runtime != null) {
      md.setRuntime(episode.runtime.intValue());
    }

    try {
      md.setReleaseDate(StrgUtils.parseDate(episode.aired));
    }
    catch (Exception e) {
      LOGGER.debug("could not parse date: {}", e.getMessage());
    }

    for (Person member : parseCastMembers(episode.characters)) {
      md.addCastMember(member);
    }

    // certifications
    for (ContentRating contentRating : ListUtils.nullSafe(episode.contentRatings)) {
      MediaCertification mediaCertification = MediaCertification.findCertification(contentRating.name);
      if (mediaCertification != MediaCertification.UNKNOWN) {
        md.addCertification(mediaCertification);
      }
    }

    // artwork
    if (StringUtils.isNotBlank(episode.image)) {
      MediaArtwork ma = new MediaArtwork(getProviderInfo().getId(), MediaArtwork.MediaArtworkType.THUMB);
      ma.setPreviewUrl(episode.image);
      ma.setOriginalUrl(episode.image);

      ArtworkTypeRecord artworkTypeRecord = getArtworkType(episode.imageType);
      if (artworkTypeRecord != null) {
        int sizeOrder = getSizeOrder(ma.getType(), artworkTypeRecord.width);
        ma.addImageSize(artworkTypeRecord.width, artworkTypeRecord.height, episode.image, sizeOrder);
      }
      else {
        ma.addImageSize(0, 0, episode.image, 0);
      }

      md.addMediaArt(ma);
    }

    EPISODE_CACHE_MAP.put(foundEpisode.getId(getId()) + "_" + options.getLanguage().getLanguage(), md);

    return md;
  }

  @Override
  public SortedSet<MediaSearchResult> search(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {
    // lazy initialization of the api
    initAPI();

    LOGGER.debug("search() {}", options);
    SortedSet<MediaSearchResult> results = new TreeSet<>();

    // detect the string to search
    String searchString = "";
    if (StringUtils.isNotBlank(options.getSearchQuery())) {
      searchString = options.getSearchQuery();
    }

    int tvdbId = options.getIdAsInt(getId());
    // if we have an TVDB id, use that!
    if (tvdbId != 0) {
      LOGGER.debug("found TvDb ID {} - getting direct", tvdbId);
      try {
        MediaMetadata md = getMetadata(options);
        if (md != null) {
          results.add(morphMediaMetadataToSearchResult(md, MediaType.TV_SHOW));
          return results;
        }
      }
      catch (Exception e) {
        LOGGER.error("problem getting data vom tvdb via ID: {}", e.getMessage());
      }
    }

    List<SearchResultRecord> searchResults = null;

    // only search when we did not find something by ID (and search string or IMDB is present)
    if (StringUtils.isNotBlank(searchString)) {
      try {
        Response<SearchResultResponse> httpResponse = tvdb.getSearchService().getSearch(searchString, SearchType.SERIES).execute();

        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }

        searchResults = httpResponse.body().data;

        // nothing found - but maybe only the tvdb id entered?
        if (ListUtils.isEmpty(searchResults) && ID_PATTERN.matcher(searchString).matches()) {
          LOGGER.debug("nothing found, but search term '{}' looks like a TvDb ID - getting direct", searchString);
          try {
            MediaMetadata md = getMetadata(options);
            if (md != null) {
              results.add(morphMediaMetadataToSearchResult(md, MediaType.TV_SHOW));
              return results;
            }
          }
          catch (Exception e) {
            LOGGER.error("problem getting data vom tvdb via ID: {}", e.getMessage());
          }
        }
      }
      catch (Exception e) {
        LOGGER.error("problem getting data vom tvdb: {}", e.getMessage());
        throw new ScrapeException(e);
      }
    }

    if (ListUtils.isEmpty(searchResults)) {
      return results;
    }

    // make sure there are no duplicates (e.g. if a show has been found in both languages)
    Map<String, MediaSearchResult> resultMap = new HashMap<>();

    for (SearchResultRecord searchResultRecord : searchResults) {
      // build up a new result
      MediaSearchResult result = new MediaSearchResult(getId(), options.getMediaType());

      String id = "";
      if (StringUtils.isNotBlank(searchResultRecord.tvdbId)) {
        // the TVDB should be here
        id = searchResultRecord.tvdbId;
      }
      else if (StringUtils.isNotBlank(searchResultRecord.id)) {
        // we can parse it out here too
        id = searchResultRecord.id.replace("series-", "");
      }

      if (StringUtils.isBlank(id)) {
        // no valid if found? need to go to the next result
        continue;
      }

      result.setId(id);

      MediaLanguages baseLanguage = options.getLanguage();
      MediaLanguages fallbackLanguage = null;
      if (StringUtils.isNotBlank(getProviderInfo().getConfig().getValue(FALLBACK_LANGUAGE))) {
        fallbackLanguage = MediaLanguages.get(getProviderInfo().getConfig().getValue(FALLBACK_LANGUAGE));
      }

      String title = parseLocalizedText(baseLanguage, searchResultRecord.translations);
      if (StringUtils.isNotBlank(title)) {
        result.setTitle(title);
      }
      else {
        // try fallback
        title = parseLocalizedText(fallbackLanguage, searchResultRecord.translations);
        if (StringUtils.isNotBlank(title)) {
          result.setTitle(title);
        }
        else {
          result.setTitle(searchResultRecord.name);
        }
      }

      String overview = parseLocalizedText(baseLanguage, searchResultRecord.overviews);
      if (StringUtils.isNotBlank(overview)) {
        result.setOverview(overview);
      }
      else {
        // try fallback
        overview = parseLocalizedText(fallbackLanguage, searchResultRecord.overviews);
        if (StringUtils.isNotBlank(overview)) {
          result.setOverview(overview);
        }
        else {
          result.setOverview(searchResultRecord.overview);
        }
      }
      result.setYear(MetadataUtil.parseInt(searchResultRecord.year, 0));
      result.setPosterUrl(searchResultRecord.imageUrl);

      // calculate score
      result.calculateScore(options);
      resultMap.put(id, result);
    }

    // and convert all entries from the map to a list
    results.addAll(resultMap.values());

    return results;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeList(): {}", options);

    // lazy initialization of the api
    initAPI();

    // do we have a show id from the options?
    Integer showId = options.getIdAsInteger(getProviderInfo().getId());
    if (showId == null || showId == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(getProviderInfo().getId());
    }

    // look in the cache map if there is an entry
    List<MediaMetadata> episodes = EPISODE_LIST_CACHE_MAP.get(showId + "_" + options.getLanguage().getLanguage());
    if (ListUtils.isNotEmpty(episodes)) {
      // cache hit!
      return episodes;
    }

    Map<MediaEpisodeGroup, List<EpisodeBaseRecord>> eps = new HashMap<>();
    // paginated results
    int pageSize = 500;

    // get all available episode groups
    // MediaMetadata tvShowMd = getMetadata(options);
    try {
      Response<SeriesExtendedResponse> httpResponse = tvdb.getSeriesService().getSeriesExtended(showId).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      SeriesExtendedRecord show = httpResponse.body().data;

      for (SeasonTypeRecord seasonTypeRecord : ListUtils.nullSafe(show.seasonTypes)) {
        int counter = 0;
        while (true) {
          SeriesEpisodesRecord seriesEpisodesRecord = getSeriesEpisodesRecord(showId, seasonTypeRecord.type, counter);
          if (seriesEpisodesRecord == null || ListUtils.isEmpty(seriesEpisodesRecord.episodes)) {
            break;
          }

          if (ListUtils.isNotEmpty(seriesEpisodesRecord.episodes)) {
            // also inject the plots / translations
            injectEpisodeTranslations(options, showId, counter, seriesEpisodesRecord);
            MediaEpisodeGroup episodeGroup = new MediaEpisodeGroup(mapEpisodeGroup(seasonTypeRecord.type), seasonTypeRecord.name);
            eps.computeIfAbsent(episodeGroup, type -> new ArrayList<>()).addAll(seriesEpisodesRecord.episodes);
          }

          if (seriesEpisodesRecord.episodes.size() < pageSize) {
            break;
          }

          counter++;
        }
      }

      // now merge all episode records by the ids (to merge the different episode numbers)
      Map<Integer, MediaMetadata> episodeMap = new HashMap<>();

      for (Map.Entry<MediaEpisodeGroup, List<EpisodeBaseRecord>> entry : eps.entrySet()) {
        MediaEpisodeGroup episodeGroup = entry.getKey();
        for (EpisodeBaseRecord ep : entry.getValue()) {
          MediaMetadata fromMap = episodeMap.get(ep.id);
          if (fromMap == null) {
            MediaMetadata episode = new MediaMetadata(getProviderInfo().getId());
            episode.setScrapeOptions(options);
            episode.setId(getProviderInfo().getId(), ep.id);
            episode.setEpisodeNumber(episodeGroup, ep.seasonNumber, ep.number);
            episode.setTitle(ep.name);
            episode.setPlot(ep.overview);
            episode.setRuntime(ep.runtime);

            try {
              episode.setReleaseDate(StrgUtils.parseDate(ep.aired));
            }
            catch (Exception ignored) {
              LOGGER.trace("Could not parse date: {}", ep.aired);
            }

            episodeMap.put(MetadataUtil.unboxInteger(ep.id), episode);
          }
          else {
            fromMap.setEpisodeNumber(episodeGroup, ep.seasonNumber, ep.number);
          }
        }
      }

      episodes = new ArrayList<>(episodeMap.values());
      episodes.sort((o1, o2) -> {
        MediaEpisodeNumber m1 = o1.getEpisodeNumber(AIRED);
        MediaEpisodeNumber m2 = o2.getEpisodeNumber(AIRED);

        if (m1 == null && m2 == null) {
          return 0;
        }
        else if (m1 == null) {
          return -1;
        }
        else if (m2 == null) {
          return 1;
        }
        else {
          return m1.compareTo(m2);
        }
      });
    }
    catch (Exception e) {
      LOGGER.debug("failed to get episode list: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    // cache for further fast access
    if (!episodes.isEmpty()) {
      EPISODE_LIST_CACHE_MAP.put(showId + "_" + options.getLanguage().getLanguage(), episodes);
    }

    return episodes;
  }

  @Override
  public Map<String, Object> getMediaIds(Map<String, Object> ids, MediaType mediaType) throws ScrapeException {
    // tvdb only makes sense with TV show ids
    if (mediaType != MediaType.TV_SHOW) {
      return Collections.emptyMap();
    }

    // lazy initialization of the api
    initAPI();

    LOGGER.debug("getMediaIds(): {}", ids);

    // do we have an id from the options?
    int id = MediaIdUtil.getIdAsInt(ids, getId());
    if (id == 0 && MediaIdUtil.isValidImdbId(MediaIdUtil.getIdAsString(ids, MediaMetadata.IMDB))) {
      id = getTvdbIdViaImdbId(MediaIdUtil.getIdAsString(ids, MediaMetadata.IMDB));
    }
    if (id == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(getId());
    }

    SeriesExtendedRecord show;

    try {
      Response<SeriesExtendedResponse> httpResponse = tvdb.getSeriesService().getSeriesExtended(id).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      show = httpResponse.body().data;
    }
    catch (Exception e) {
      LOGGER.error("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (show == null) {
      throw new NothingFoundException();
    }

    // populate metadata
    Map<String, Object> showIds = new HashMap<>();
    showIds.put(getId(), show.id);
    parseRemoteIDs(show.remoteIds).forEach((k, v) -> {
      showIds.put(k, v);
    });

    return showIds;
  }

  private SeriesEpisodesRecord getSeriesEpisodesRecord(int showId, SeasonType seasonType, int counter) {
    try {
      Response<SeriesEpisodesResponse> httpResponse = tvdb.getSeriesService().getSeriesEpisodes(showId, seasonType, counter).execute();
      if (httpResponse.isSuccessful()) {
        SeriesEpisodesResponse response = httpResponse.body();
        if (response != null) {
          return response.data;
        }
      }
      else if (counter == 0) {
        // error at the first fetch will result in an exception
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
    }
    catch (Exception e) {
      LOGGER.debug("Could not get episode listing for season type '{}' - '{}'  ", seasonType, e.getMessage());
    }

    return null;
  }

  private void injectEpisodeTranslations(TvShowSearchAndScrapeOptions options, int showId, int counter, SeriesEpisodesRecord seriesEpisodesRecord) {
    Map<EpisodeBaseRecord, String> titleMap = new HashMap<>();
    Map<EpisodeBaseRecord, String> overviewMap = new HashMap<>();

    // store original titles & remove all texts
    for (EpisodeBaseRecord toInject : ListUtils.nullSafe(seriesEpisodesRecord.episodes)) {
      // store for fallback
      titleMap.put(toInject, toInject.name);
      overviewMap.put(toInject, toInject.overview);

      // the title in the original call is the original title
      toInject.originalName = toInject.name;

      // remove
      toInject.name = null;
      toInject.overview = null;
    }

    // 1. in desired language
    String language = LanguageUtils.getIso3Language(options.getLanguage().toLocale());
    // pt-BR is pt at tvdb...
    if ("pob".equals(language)) {
      language = "pt";
    }

    if (StringUtils.isNotBlank(language)) {
      if (language.equals(seriesEpisodesRecord.series.originalLanguage)) {
        // original title and requested title is the same - just copy
        for (EpisodeBaseRecord toInject : ListUtils.nullSafe(seriesEpisodesRecord.episodes)) {
          toInject.name = toInject.originalName;
          toInject.overview = overviewMap.get(toInject);
        }
      }
      else {
        try {
          Response<SeriesEpisodesResponse> httpResponse = tvdb.getSeriesService()
              .getSeriesEpisodes(showId, SeasonType.DEFAULT, language, counter)
              .execute();
          if (httpResponse.isSuccessful()) {
            SeriesEpisodesResponse response = httpResponse.body();
            if (response != null && response.data != null) {
              for (EpisodeBaseRecord toInject : ListUtils.nullSafe(seriesEpisodesRecord.episodes)) {
                // find the corresponding episode in the response
                for (EpisodeBaseRecord translation : ListUtils.nullSafe(response.data.episodes)) {
                  if (Objects.equals(toInject.id, translation.id)) {
                    if (language.equals(seriesEpisodesRecord.series.originalLanguage)) {
                      toInject.originalName = translation.name;
                    }
                    if (StringUtils.isNotBlank(translation.name)) {
                      toInject.name = translation.name;
                    }
                    if (StringUtils.isNotBlank(translation.overview)) {
                      toInject.overview = translation.overview;
                    }
                    break;
                  }
                }
              }
            }
          }
        }
        catch (Exception e) {
          LOGGER.debug("Could not get episode translations - '{}'  ", e.getMessage());
        }
      }
    }

    // 2. in fallback language
    String fallbackLanguage = LanguageUtils.getIso3Language(MediaLanguages.get(getProviderInfo().getConfig().getValue(FALLBACK_LANGUAGE)).toLocale());
    if ("pob".equals(fallbackLanguage)) {
      fallbackLanguage = "pt";
    }

    if (StringUtils.isNotBlank(fallbackLanguage) && !fallbackLanguage.equals(language)) {
      if (fallbackLanguage.equals(seriesEpisodesRecord.series.originalLanguage)) {
        // original title and requested title is the same - just copy
        for (EpisodeBaseRecord toInject : ListUtils.nullSafe(seriesEpisodesRecord.episodes)) {
          if (StringUtils.isBlank(toInject.name)) {
            toInject.name = toInject.originalName;
          }
          if (StringUtils.isBlank(toInject.overview)) {
            toInject.overview = overviewMap.get(toInject);
          }
        }
      }
      else {
        try {
          Response<SeriesEpisodesResponse> httpResponse = tvdb.getSeriesService()
              .getSeriesEpisodes(showId, SeasonType.DEFAULT, fallbackLanguage, counter)
              .execute();
          if (httpResponse.isSuccessful()) {
            SeriesEpisodesResponse response = httpResponse.body();
            if (response != null && response.data != null) {
              for (EpisodeBaseRecord toInject : ListUtils.nullSafe(seriesEpisodesRecord.episodes)) {
                // find the corresponding episode in the response
                for (EpisodeBaseRecord translation : ListUtils.nullSafe(response.data.episodes)) {
                  if (Objects.equals(toInject.id, translation.id)) {
                    if (StringUtils.isBlank(toInject.name) && StringUtils.isNotBlank(translation.name)) {
                      toInject.name = translation.name;
                    }
                    if (StringUtils.isBlank(toInject.overview) && StringUtils.isNotBlank(translation.overview)) {
                      toInject.overview = translation.overview;
                    }
                    break;
                  }
                }
              }
            }
          }
        }
        catch (Exception e) {
          LOGGER.debug("Could not get episode translations - '{}'  ", e.getMessage());
        }
      }
    }

    // last but not least - make sure that no empty title is offered. We just re-insert the original value from the scraper
    for (EpisodeBaseRecord toInject : ListUtils.nullSafe(seriesEpisodesRecord.episodes)) {
      if (StringUtils.isBlank(toInject.name)) {
        toInject.name = titleMap.get(toInject);
      }
      if (StringUtils.isBlank(toInject.name)) {
        // still empty? we never got anything from TVDB, so add the same fallback as in TMDB
        toInject.name = "Episode " + toInject.number;
      }

      if (StringUtils.isBlank(toInject.originalName)) {
        // still empty? we never got anything from TVDB, so add the same fallback as in TMDB
        toInject.originalName = "Episode " + toInject.number;
      }
    }
  }

  private MediaEpisodeGroup.EpisodeGroup mapEpisodeGroup(SeasonType type) {
    return switch (type) {
      case DEFAULT, OFFICIAL -> MediaEpisodeGroup.EpisodeGroup.AIRED;
      case ABSOLUTE -> MediaEpisodeGroup.EpisodeGroup.ABSOLUTE;
      case DVD -> MediaEpisodeGroup.EpisodeGroup.DVD;
      case ALTERNATE, REGIONAL -> MediaEpisodeGroup.EpisodeGroup.ALTERNATE;
    };
  }
}
