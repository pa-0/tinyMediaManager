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
package org.tinymediamanager.scraper.imdb;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.PRODUCER;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.config.MediaProviderConfig;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.ImageSizeAndUrl;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.InMemoryCachedUrl;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.imdb.entities.ImdbAdvancedSearchResult;
import org.tinymediamanager.scraper.imdb.entities.ImdbCast;
import org.tinymediamanager.scraper.imdb.entities.ImdbCategory;
import org.tinymediamanager.scraper.imdb.entities.ImdbChartTitleEdge;
import org.tinymediamanager.scraper.imdb.entities.ImdbCountry;
import org.tinymediamanager.scraper.imdb.entities.ImdbCredits;
import org.tinymediamanager.scraper.imdb.entities.ImdbCrew;
import org.tinymediamanager.scraper.imdb.entities.ImdbEpisodeNumber;
import org.tinymediamanager.scraper.imdb.entities.ImdbGenre;
import org.tinymediamanager.scraper.imdb.entities.ImdbIdTextType;
import org.tinymediamanager.scraper.imdb.entities.ImdbImage;
import org.tinymediamanager.scraper.imdb.entities.ImdbKeyword;
import org.tinymediamanager.scraper.imdb.entities.ImdbPlaintext;
import org.tinymediamanager.scraper.imdb.entities.ImdbPlaybackUrl;
import org.tinymediamanager.scraper.imdb.entities.ImdbReleaseDate;
import org.tinymediamanager.scraper.imdb.entities.ImdbSearchResult;
import org.tinymediamanager.scraper.imdb.entities.ImdbSectionItem;
import org.tinymediamanager.scraper.imdb.entities.ImdbTitleKeyword;
import org.tinymediamanager.scraper.imdb.entities.ImdbTitleType;
import org.tinymediamanager.scraper.imdb.entities.ImdbVideo;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.util.JsonUtils;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.scraper.util.UrlUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The abstract class ImdbParser holds all relevant parsing logic which can be used either by the movie parser and TV show parser
 *
 * @author Manuel Laggner
 */
public abstract class ImdbParser {
  static final Pattern                IMDB_ID_PATTERN          = Pattern.compile("/title/(tt[0-9]{6,})/");
  static final Pattern                PERSON_ID_PATTERN        = Pattern.compile("/name/(nm[0-9]{6,})/");
  static final Pattern                MOVIE_PATTERN            = Pattern.compile("^.*?\\(\\d{4}\\)$");
  static final Pattern                TV_MOVIE_PATTERN         = Pattern.compile("^.*?\\(\\d{4}\\s+TV Movie\\)$");
  static final Pattern                TV_SERIES_PATTERN        = Pattern.compile("^.*?\\(\\d{4}\\)\\s+\\((TV Series|TV Mini[ -]Series)\\)$");
  static final Pattern                SHORT_PATTERN            = Pattern.compile("^.*?\\(\\d{4}\\)\\s+\\((Short|Video)\\)$");
  static final Pattern                VIDEOGAME_PATTERN        = Pattern.compile("^.*?\\(\\d{4}\\)\\s+\\(Video Game\\)$");
  static final Pattern                IMAGE_SCALING_PATTERN    = Pattern.compile("S([XY])(.*?)_CR(\\d*),(\\d*),(\\d*),(\\d*)");

  static final String                 INCLUDE_MOVIE            = "includeMovieResults";
  static final String                 INCLUDE_TV_MOVIE         = "includeTvMovieResults";
  static final String                 INCLUDE_TV_SERIES        = "includeTvSeriesResults";
  static final String                 INCLUDE_SHORT            = "includeShortResults";
  static final String                 INCLUDE_VIDEOGAME        = "includeVideogameResults";
  static final String                 INCLUDE_PODCAST          = "includePodcastResults";
  static final String                 INCLUDE_ADULT            = "includeAdultResults";
  static final String                 INCLUDE_METACRITIC       = "includeMetacritic";

  static final String                 SCRAPE_KEYWORDS_PAGE     = "scrapeKeywordsPage";
  static final String                 SCRAPE_UNCREDITED_ACTORS = "scrapeUncreditedActors";
  static final String                 SCRAPE_LANGUAGE_NAMES    = "scrapeLanguageNames";
  static final String                 LOCAL_RELEASE_DATE       = "localReleaseDate";
  static final String                 INCLUDE_PREMIERE_DATE    = "includePremiereDate";
  static final String                 MAX_KEYWORD_COUNT        = "maxKeywordCount";

  protected final IMediaProvider      metadataProvider;
  protected final MediaType           type;
  protected final MediaProviderConfig config;
  protected final ExecutorService     executor;
  private ObjectMapper                mapper                   = new ObjectMapper();

  protected ImdbParser(IMediaProvider mediaProvider, MediaType type, ExecutorService executor) {
    this.metadataProvider = mediaProvider;
    this.type = type;
    this.config = mediaProvider.getProviderInfo().getConfig();
    this.executor = executor;
  }

  protected abstract Logger getLogger();

  protected abstract MediaMetadata getMetadata(MediaSearchAndScrapeOptions options) throws ScrapeException;

  /**
   * should we include movie results
   *
   * @return true/false
   */
  protected boolean isIncludeMovieResults() {
    return config.getValueAsBool(INCLUDE_MOVIE, false);
  }

  /**
   * should we include TV movie results
   *
   * @return true/false
   */
  protected boolean isIncludeTvMovieResults() {
    return config.getValueAsBool(INCLUDE_TV_MOVIE, false);
  }

  /**
   * should we include TV series results
   *
   * @return true/false
   */
  protected boolean isIncludeTvSeriesResults() {
    return config.getValueAsBool(INCLUDE_TV_SERIES, false);
  }

  /**
   * should we include shorts
   *
   * @return true/false
   */
  protected boolean isIncludeShortResults() {
    return config.getValueAsBool(INCLUDE_SHORT, false);
  }

  /**
   * should we include video game results
   *
   * @return true/false
   */
  protected boolean isIncludeVideogameResults() {
    return config.getValueAsBool(INCLUDE_VIDEOGAME, false);
  }

  /**
   * should we include adult results
   *
   * @return true/false
   */
  protected boolean isIncludeAdultResults() {
    return config.getValueAsBool(INCLUDE_ADULT, false);
  }

  /**
   * should we include podcast series results
   *
   * @return true/false
   */
  protected boolean isIncludePodcastResults() {
    return config.getValueAsBool(INCLUDE_PODCAST, false);
  }

  /**
   * should we scrape uncredited actors
   *
   * @return true/false
   */
  protected boolean isScrapeUncreditedActors() {
    return config.getValueAsBool(SCRAPE_UNCREDITED_ACTORS, false);
  }

  /**
   * should we scrape language names rather than the iso codes
   *
   * @return true/false
   */
  protected boolean isScrapeLanguageNames() {
    return config.getValueAsBool(SCRAPE_LANGUAGE_NAMES, false);
  }

  /**
   * should we scrape the keywords page too
   *
   * @return true/false
   */
  protected boolean isScrapeKeywordsPage() {
    return config.getValueAsBool(SCRAPE_KEYWORDS_PAGE, false);
  }

  /**
   * should we scrape Metacritic ratings
   *
   * @return true/false
   */
  protected boolean isScrapeMetacriticRatings() {
    return config.getValueAsBool(INCLUDE_METACRITIC, false);
  }

  /**
   * should we scrape local release date, or the "first" one?
   *
   * @return true/false
   */
  protected boolean isScrapeLocalReleaseDate() {
    return config.getValueAsBool(LOCAL_RELEASE_DATE, true);
  }

  /**
   * get the maximum amount of keywords we should get from the keywords page
   *
   * @return the configured numer or {@link Integer}.MAX_VALUE
   */
  protected int getMaxKeywordCount() {
    Integer value = config.getValueAsInteger(MAX_KEYWORD_COUNT);
    if (value == null) {
      return 5; // as in scraper settings
    }
    return value;
  }

  @Deprecated
  protected boolean includeSearchResult(String text) {
    // strip out episodes
    if (text.contains("(TV Episode)")) {
      return false;
    }

    ResultCategory category = getResultCategory(text.strip());
    if (category == null) {
      return false;
    }

    switch (category) {
      case MOVIE:
        return isIncludeMovieResults();

      case TV_MOVIE:
        return isIncludeTvMovieResults();

      case TV_SERIES:
        return isIncludeTvSeriesResults();

      case SHORT:
        return isIncludeShortResults();

      case VIDEOGAME:
        return isIncludeVideogameResults();

      default:
        return false;
    }
  }

  @Deprecated
  protected ResultCategory getResultCategory(String text) {
    Matcher matcher = MOVIE_PATTERN.matcher(text);
    if (matcher.matches()) {
      return ResultCategory.MOVIE;
    }

    matcher = TV_MOVIE_PATTERN.matcher(text);
    if (matcher.matches()) {
      return ResultCategory.TV_MOVIE;
    }

    matcher = TV_SERIES_PATTERN.matcher(text);
    if (matcher.matches()) {
      return ResultCategory.TV_SERIES;
    }

    matcher = SHORT_PATTERN.matcher(text);
    if (matcher.matches()) {
      return ResultCategory.SHORT;
    }

    matcher = VIDEOGAME_PATTERN.matcher(text);
    if (matcher.matches()) {
      return ResultCategory.VIDEOGAME;
    }

    return null;
  }

  protected String constructUrl(String... parts) throws ScrapeException {
    try {
      return metadataProvider.getApiKey() + String.join("", parts);
    }
    catch (Exception e) {
      throw new ScrapeException(e);
    }
  }

  protected String decode(String source) {
    return new String(Base64.getDecoder().decode(source), StandardCharsets.UTF_8);
  }

  protected SortedSet<MediaSearchResult> search(MediaSearchAndScrapeOptions options) throws ScrapeException {
    getLogger().debug("search(): {}", options);
    SortedSet<MediaSearchResult> results = new TreeSet<>();

    /*
     * IMDb matches seem to come in several "flavours".
     *
     * Firstly, if there is one exact match it returns the matching IMDb page.
     *
     * If that fails to produce a unique hit then a list of possible matches are returned categorised as: Popular Titles (Displaying ? Results) Titles
     * (Exact Matches) (Displaying ? Results) Titles (Partial Matches) (Displaying ? Results)
     *
     * We should check the Exact match section first, then the poplar titles and finally the partial matches.
     *
     * Note: That even with exact matches there can be more than 1 hit, for example "Star Trek"
     */
    String searchTerm = "";

    if (StringUtils.isNotEmpty(options.getImdbId())) {
      searchTerm = options.getImdbId();
    }

    if (StringUtils.isEmpty(searchTerm)) {
      searchTerm = options.getSearchQuery();
    }

    if (StringUtils.isEmpty(searchTerm)) {
      return results;
    }

    // parse out language and country from the scraper query
    String language = options.getLanguage().getLanguage();
    String country = options.getCertificationCountry().getAlpha2(); // for passing the country to the scrape

    searchTerm = MetadataUtil.removeNonSearchCharacters(searchTerm);

    getLogger().debug("========= BEGIN IMDB Scraper Search for: {}", searchTerm);
    Document doc = null;

    Url url;

    boolean advancedSearch = false;
    // always use advSearch for TV, since we need to filter out tvMovies on TV scraping :/
    // if (options.getMediaType() == MediaType.TV_SHOW) {
    // advancedSearch = true;
    // }
    if (isIncludeShortResults() || isIncludeTvMovieResults() || isIncludeVideogameResults() || isIncludeAdultResults()) {
      advancedSearch = true;
    }

    try {
      if (advancedSearch) {

        // build advanced search queries per entity
        String cats = "";
        if (options.getMediaType() == MediaType.MOVIE) {
          cats = "&title_type=feature";
          if (isIncludeShortResults()) {
            cats += ",short,tv_short"; // tv shorts are "short movies played solely on tv, mostly ads, but also dinnerForOne"
          }
          if (isIncludeTvMovieResults()) {
            cats += ",tv_movie,tv_special";
          }
          if (isIncludeVideogameResults()) {
            cats += ",video_game";
          }
        }
        else if (options.getMediaType() == MediaType.TV_SHOW) {
          cats = "&title_type=tv_series,tv_miniseries";
          if (isIncludePodcastResults()) {
            cats += ",podcast_series";
          }
        }

        if (isIncludeAdultResults()) {
          cats += "&adult=include";
        }
        url = new InMemoryCachedUrl(constructUrl("search/title/?title=", URLEncoder.encode(searchTerm, StandardCharsets.UTF_8), cats));
      }
      else {
        String cat = "&s=tt&ttype=ft"; // movies
        if (options.getMediaType() == MediaType.TV_SHOW) {
          cat = "&s=tt&ttype=tv";
        }
        url = new InMemoryCachedUrl(constructUrl("find/?q=", URLEncoder.encode(searchTerm, StandardCharsets.UTF_8), cat));
      }
      url.addHeader("Accept-Language", getAcceptLanguage(language, country));
    }
    catch (Exception e) {
      getLogger().debug("tried to fetch search response", e);
      throw new ScrapeException(e);
    }

    try (InputStream is = url.getInputStream()) {
      doc = Jsoup.parse(is, UrlUtil.UTF_8, "");
      doc.setBaseUri(metadataProvider.getApiKey());
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      getLogger().debug("tried to fetch search response", e);
      throw new ScrapeException(e);
    }

    if (doc == null) {
      return Collections.emptySortedSet();
    }

    // parse regular search result page
    try {
      String json = doc.getElementById("__NEXT_DATA__").data();
      if (!json.isEmpty()) {
        JsonNode node = mapper.readTree(json);
        JsonNode resultsNode = JsonUtils.at(node, "/props/pageProps/titleResults/results"); // find
        if (resultsNode.isMissingNode()) {
          // advanced search
          resultsNode = JsonUtils.at(node, "/props/pageProps/searchResults/titleResults/titleListItems");
        }

        // check if we were redirected to detail page directly (when searching with id)
        if (resultsNode.isMissingNode()) {
          MediaMetadata md = new MediaMetadata(ImdbMetadataProvider.ID);
          parseDetailPageJson(doc, options, md);
          MediaSearchResult sr = md.toSearchResult(options.getMediaType());
          sr.setScore(1);
          results.add(sr);
        }
        else {
          if (advancedSearch) {
            for (ImdbAdvancedSearchResult result : JsonUtils.parseList(mapper, resultsNode, ImdbAdvancedSearchResult.class)) {
              MediaSearchResult sr = parseJsonAdvancedSearchResults(result, options);
              // only add wanted ones
              if (sr != null && options.getMediaType().equals(result.getMediaType())) {
                results.add(sr);
              }
            }
          }
          else {
            for (ImdbSearchResult result : JsonUtils.parseList(mapper, resultsNode, ImdbSearchResult.class)) {
              MediaSearchResult sr = parseJsonSearchResults(result, options);
              // only add wanted ones
              if (sr != null && options.getMediaType().equals(result.getMediaType())) {
                results.add(sr);
              }
            }
          }
        }
        if (!results.isEmpty()) {
          return results; // we found something
        }
      }
    }
    catch (

    Exception e) {
      getLogger().warn("Error parsing JSON - '{}'", e.getMessage());
    }

    // no JSON or error - also check if we have been redirected to detail page
    Elements pageType = doc.getElementsByAttributeValue("property", "imdb:pageType");
    if (!pageType.isEmpty()) {
      String content = pageType.get(0).attr("content");
      if (content.equalsIgnoreCase("title")) {
        // detail page - generate a dummy searchresult
        MediaSearchResult sr = new MediaSearchResult(ImdbMetadataProvider.ID, options.getMediaType());
        Elements pageConst = doc.getElementsByAttributeValue("property", "imdb:pageConst");
        content = pageConst.get(0).attr("content");
        if (MediaIdUtil.isValidImdbId(content)) {
          sr.setId(ImdbMetadataProvider.ID, content);
        }
        else {
          if (MediaIdUtil.isValidImdbId(searchTerm)) {
            sr.setId(ImdbMetadataProvider.ID, searchTerm);
          }
        }
        Elements titleYear = doc.getElementsByAttributeValue("property", "og:title");
        if (!titleYear.isEmpty()) {
          content = titleYear.get(0).attr("content");
          String title = StrgUtils.substr(content, "(.*?)\\("); // everything before ()
          String year = StrgUtils.substr(content, ".*?(\\d{4}).*"); // first 4 numbers as year
          sr.setTitle(title);
          sr.setYear(MetadataUtil.parseInt(year, 0));
        }

        results.add(sr);
      }
    }

    // parse results newer style
    Elements elements = doc.getElementsByClass("ipc-metadata-list-summary-item");
    for (Element tr : elements) {
      MediaSearchResult sr = parseSearchResultsNewStyle(tr, options);
      if (sr != null && options.getMediaType() == sr.getMediaType()) {
        results.add(sr);
      }
      // only get 80 results
      if (results.size() >= 80) {
        break;
      }
    }

    // parse results old style
    if (elements.isEmpty()) {
      elements = doc.getElementsByClass("findResult");
      for (Element tr : elements) {
        // we only want the tr's
        if (!"tr".equalsIgnoreCase(tr.tagName())) {
          continue;
        }
        MediaSearchResult sr = parseSearchResults(tr, options);
        if (sr != null) {
          results.add(sr);
        }
        // only get 80 results
        if (results.size() >= 80) {
          break;
        }
      }
    }

    // parse results advanced search
    if (elements.isEmpty()) {
      elements = doc.getElementsByClass("lister-item");
      for (Element tr : elements) {
        MediaSearchResult sr = parseAdvancedSearchResults(tr, options);
        if (sr != null) {
          results.add(sr);
        }
        // only get 80 results
        if (results.size() >= 80) {
          break;
        }
      }
    }

    return results;
  }

  private MediaSearchResult parseJsonSearchResults(ImdbSearchResult result, MediaSearchAndScrapeOptions options) {
    MediaSearchResult sr = new MediaSearchResult(ImdbMetadataProvider.ID, options.getMediaType());

    sr.setIMDBId(result.getId());
    sr.setTitle(result.titleNameText);
    String year = result.titleReleaseText;
    if (!year.isEmpty()) {
      if (year.length() == 4) {
        sr.setYear(MetadataUtil.parseInt(year, 0));
      }
      else {
        if (year.matches("\\d{4}-?.*")) {
          sr.setYear(MetadataUtil.parseInt(year.substring(0, 4), 0));
        }
      }
    }
    if (result.titlePosterImageModel != null) {
      sr.setPosterUrl(result.titlePosterImageModel.url);
    }

    if (sr.getIMDBId().equals(options.getImdbId())) {
      // perfect match
      sr.setScore(1);
    }
    else {
      // calculate the score by comparing the search result with the search options
      sr.calculateScore(options);
    }
    return sr;
  }

  private MediaSearchResult parseJsonAdvancedSearchResults(ImdbAdvancedSearchResult result, MediaSearchAndScrapeOptions options) {
    MediaSearchResult sr = new MediaSearchResult(ImdbMetadataProvider.ID, options.getMediaType());

    sr.setIMDBId(result.getId());
    sr.setTitle(result.titleText);
    sr.setYear(result.releaseYear);
    if (result.primaryImage != null) {
      sr.setPosterUrl(result.primaryImage.url);
    }
    sr.setOriginalTitle(result.originalTitleText);
    sr.setOverview(result.plot);

    if (sr.getIMDBId().equals(options.getImdbId())) {
      // perfect match
      sr.setScore(1);
    }
    else {
      // calculate the score by comparing the search result with the search options
      sr.calculateScore(options);
    }
    return sr;
  }

  private MediaSearchResult parseAdvancedSearchResults(Element tr, MediaSearchAndScrapeOptions options) {

    MediaSearchResult sr = new MediaSearchResult(ImdbMetadataProvider.ID, options.getMediaType());
    String movieId = "";
    Element header = null;
    try {
      header = tr.getElementsByClass("lister-item-header").get(0);
      sr.setTitle(header.getElementsByTag("a").text());
      sr.setUrl(header.getElementsByTag("a").attr("href"));
      movieId = StrgUtils.substr(sr.getUrl(), ".*?(tt\\d+).*");
      sr.setIMDBId(movieId);
    }
    catch (Exception e) {
      // basic info error?
      return null;
    }

    try {
      String yr = header.getElementsByClass("lister-item-year").get(0).text();
      String year = StrgUtils.substr(yr, ".*?(\\d{4}).*"); // first 4 nums
      sr.setYear(MetadataUtil.parseInt(year, 0));
    }
    catch (Exception e) {
      // ignore year parsing errors
    }

    try {
      Element img = tr.getElementsByClass("lister-item-image").get(0);
      // String imgurl = img.getElementsByAttribute("loadlate").get(0).attr("src");
      String imgurlSmall = img.getElementsByAttribute("loadlate").get(0).attr("loadlate");
      sr.setPosterUrl(imgurlSmall);
    }
    catch (Exception e) {
      // ignore poster parsing errors
    }

    if (movieId.equals(options.getImdbId())) {
      // perfect match
      sr.setScore(1);
    }
    else {
      // calculate the score by comparing the search result with the search options
      sr.calculateScore(options);
    }

    return sr;
  }

  private MediaSearchResult parseSearchResults(Element tr, MediaSearchAndScrapeOptions options) {
    // find the id / name
    String movieName = "";
    String movieId = "";
    int year = 0;
    Elements tds = tr.getElementsByClass("result_text");
    for (Element element : tds) {
      // we only want the td's
      if (!"td".equalsIgnoreCase(element.tagName())) {
        continue;
      }

      // filter out unwanted results
      if (!includeSearchResult(element.ownText().replace("aka", ""))) {
        continue;
      }

      // is there a localized name? (aka)
      String localizedName = "";
      Elements italics = element.getElementsByTag("i");
      if (!italics.isEmpty()) {
        localizedName = italics.text().replace("\"", "");
      }

      // get the name inside the link
      Elements anchors = element.getElementsByTag("a");
      for (Element a : anchors) {
        if (StringUtils.isNotEmpty(a.text())) {
          // movie name
          if (StringUtils.isNotBlank(localizedName) && !options.getLanguage().getLanguage().equals("en")) {
            // take AKA as title, but only if not EN
            movieName = localizedName;
          }
          else {
            movieName = a.text();
          }

          // parse id
          String href = a.attr("href");
          Matcher matcher = IMDB_ID_PATTERN.matcher(href);
          while (matcher.find()) {
            if (matcher.group(1) != null) {
              movieId = matcher.group(1);
            }
          }

          // try to parse out the year
          Pattern yearPattern = Pattern.compile("\\(([0-9]{4})|/\\)");
          matcher = yearPattern.matcher(element.text());
          while (matcher.find()) {
            if (matcher.group(1) != null) {
              try {
                year = Integer.parseInt(matcher.group(1));
                break;
              }
              catch (Exception ignored) {
                // nothing to do here
              }
            }
          }
          break;
        }
      }
    }

    // if an id/name was found - parse the poster image
    String posterUrl = "";
    tds = tr.getElementsByClass("primary_photo");
    for (Element element : tds) {
      Elements imgs = element.getElementsByTag("img");
      for (Element img : imgs) {
        posterUrl = img.attr("src");
        posterUrl = posterUrl.replaceAll("UX[0-9]{2,4}_", "");
        posterUrl = posterUrl.replaceAll("UY[0-9]{2,4}_", "");
        posterUrl = posterUrl.replaceAll("CR[0-9]{1,3},[0-9]{1,3},[0-9]{1,3},[0-9]{1,3}_", "");
      }
    }

    // if no movie name/id was found - continue
    if (StringUtils.isEmpty(movieName) || StringUtils.isEmpty(movieId)) {
      return null;
    }

    MediaSearchResult sr = new MediaSearchResult(ImdbMetadataProvider.ID, options.getMediaType());
    sr.setTitle(movieName);
    sr.setIMDBId(movieId);
    sr.setYear(year);
    sr.setPosterUrl(posterUrl);

    if (movieId.equals(options.getImdbId())) {
      // perfect match
      sr.setScore(1);
    }
    else {
      // calculate the score by comparing the search result with the search options
      sr.calculateScore(options);
    }

    return sr;
  }

  private MediaSearchResult parseSearchResultsNewStyle(Element element, MediaSearchAndScrapeOptions options) {

    MediaSearchResult sr = new MediaSearchResult(ImdbMetadataProvider.ID, options.getMediaType());

    Element titleEl = element.getElementsByClass("ipc-metadata-list-summary-item__t").first();
    if (titleEl != null) {
      sr.setTitle(titleEl.text());

      String href = titleEl.absUrl("href");
      sr.setUrl(href);

      // parse id
      Matcher matcher = IMDB_ID_PATTERN.matcher(href);
      while (matcher.find()) {
        if (matcher.group(1) != null) {
          sr.setIMDBId(matcher.group(1));
        }
      }
    }

    // parse poster
    Element img = element.getElementsByClass("ipc-image").first();
    if (img != null) {
      String posterUrl = img.attr("src");
      posterUrl = posterUrl.replaceAll("UX[0-9]{2,4}_", "");
      posterUrl = posterUrl.replaceAll("UY[0-9]{2,4}_", "");
      posterUrl = posterUrl.replaceAll("CR[0-9]{1,3},[0-9]{1,3},[0-9]{1,3},[0-9]{1,3}_", "");
      sr.setPosterUrl(posterUrl);
    }

    // parse year xxxx-yyyy
    Elements items = element.getElementsByClass("ipc-metadata-list-summary-item__li");
    for (Element span : items) {
      String text = span.text();
      if (text.matches("\\d{4}[-]?.*")) {
        int year = MetadataUtil.parseInt(text.substring(0, 4));
        sr.setYear(year);
      }
      else if (text.matches("S\\d+\\.E\\d+")) {
        // we found some S/EE values - must be episode
        sr.setMediaType(MediaType.TV_EPISODE);
      }
    }

    if (sr.getIMDBId().equals(options.getImdbId())) {
      // perfect match
      sr.setScore(1);
    }
    else {
      // calculate the score by comparing the search result with the search options
      sr.calculateScore(options);
    }

    return sr;
  }

  /**
   * generates the accept-language http header for imdb
   *
   * @param language
   *          the language code to be used
   * @param country
   *          the country to be used
   * @return the Accept-Language string
   */
  protected static String getAcceptLanguage(String language, String country) {
    List<String> languageString = new ArrayList<>();

    // first: take the preferred language from settings,
    // but validate whether it is legal or not
    if (StringUtils.isNotBlank(language) && StringUtils.isNotBlank(country) && LocaleUtils.isAvailableLocale(new Locale(language, country))) {
      String combined = language + "-" + country;
      languageString.add(combined);
    }

    // also build langu & default country
    Locale localeFromLanguage = UrlUtil.getLocaleFromLanguage(language);
    String cFromLang = localeFromLanguage.getCountry();
    if (localeFromLanguage != null) {
      String combined = language + (cFromLang.isEmpty() ? "" : "-" + cFromLang);
      if (!languageString.contains(combined)) {
        languageString.add(combined);
      }
    }

    if (StringUtils.isNotBlank(language)) {
      languageString.add(language);
    }

    // second: the JRE language
    Locale jreLocale = Locale.getDefault();
    String jreCountry = jreLocale.getCountry();
    String combined = jreLocale.getLanguage() + (jreCountry.isEmpty() ? "" : "-" + jreCountry);
    if (!languageString.contains(combined)) {
      languageString.add(combined);
    }
    if (!languageString.contains(jreLocale.getLanguage())) {
      languageString.add(jreLocale.getLanguage());
    }

    // third: fallback to en
    if (!languageString.contains("en-US")) {
      languageString.add("en-US");
    }
    if (!languageString.contains("en")) {
      languageString.add("en");
    }

    // build a http header for the preferred language
    StringBuilder languages = new StringBuilder();
    float qualifier = 1f;

    for (String line : languageString) {
      if (languages.length() > 0) {
        languages.append(",");
      }
      languages.append(line);
      if (qualifier < 1) {
        languages.append(String.format(Locale.US, ";q=%1.1f", qualifier));
      }
      qualifier -= 0.1;
    }

    return languages.toString();
  }

  /**
   * @param doc
   * @param options
   * @param md
   * @throws Exception
   *           on JSON parsing errors
   */
  protected void parseDetailPageJson(Document doc, MediaSearchAndScrapeOptions options, MediaMetadata md) throws Exception {
    try {
      String json = doc.getElementById("__NEXT_DATA__").data();
      // System.out.println(json);
      JsonNode node = mapper.readTree(json);

      // ***** REQ/RESP column *****
      String certCountry = "";
      String responseLangu = JsonUtils.at(node, "/props/pageProps/requestContext/sidecar/localizationResponse/languageForTranslations").asText();
      if (responseLangu.isEmpty()) {
        responseLangu = JsonUtils.at(node, "/props/pageProps/requestContext/sidecar/localizationResponse/userLanguage").asText();
      }
      if (!responseLangu.isEmpty()) {
        Locale l = Locale.forLanguageTag(responseLangu);
        certCountry = l.getCountry();
      }

      // ***** TOP column *****
      md.setId(ImdbMetadataProvider.ID, JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/id").asText());
      md.setTitle(JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/titleText/text").asText());
      md.setOriginalTitle(JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/originalTitleText/text").asText());
      if (md.getOriginalTitle().isEmpty()) {
        md.setOriginalTitle(md.getTitle());
      }
      md.setYear(JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/releaseYear/year").asInt(0));

      JsonNode plotNode = JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/plot/plotText");
      ImdbPlaintext plot = JsonUtils.parseObject(mapper, plotNode, ImdbPlaintext.class);
      if (plot != null) {
        md.setPlot(plot.plainText);
      }

      JsonNode releaseDateNode = JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/releaseDate");
      ImdbReleaseDate relDate = JsonUtils.parseObject(mapper, releaseDateNode, ImdbReleaseDate.class);
      if (relDate != null) {
        md.setReleaseDate(relDate.toDate());
      }

      md.setRuntime(JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/runtime/seconds").asInt(0) / 60);
      // fallback
      if (md.getRuntime() == 0) {
        md.setRuntime(JsonUtils.at(node, "/props/pageProps/mainColumnData/series/series/runtime/seconds").asInt(0) / 60);
      }

      JsonNode agg = JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/ratingsSummary/aggregateRating");
      if (!agg.isMissingNode()) {
        MediaRating rating = new MediaRating(MediaMetadata.IMDB);
        rating.setRating(agg.floatValue());
        rating.setVotes(JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/ratingsSummary/voteCount").asInt(0));
        rating.setMaxValue(10);
        if (rating.getRating() > 0) {
          md.addRating(rating);
        }
      }
      if (isScrapeMetacriticRatings()) {
        MediaRating rating = new MediaRating(MediaMetadata.METACRITIC);
        rating.setRating(JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/metacritic/metascore/score").asInt(0));
        rating.setMaxValue(100);
        if (rating.getRating() > 0) {
          md.addRating(rating);
        }
      }

      // skip certification for now because this probably returns the wrong certification
      // JsonNode certNode = JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/certificate");
      // ImdbCertificate certificate = ImdbJsonHelper.parseObject(mapper, certNode, ImdbCertificate.class);
      // if (!certCountry.isEmpty() && certificate != null) {
      // md.addCertification(MediaCertification.getCertification(certCountry, certificate.rating));
      // // TODO: parse from reference page and add all?!
      // }

      JsonNode genreNode = JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/genres/genres");
      for (ImdbGenre genre : JsonUtils.parseList(mapper, genreNode, ImdbGenre.class)) {
        md.addGenre(genre.toTmm());
      }

      if (isScrapeKeywordsPage()) {
        JsonNode keywordsNode = JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/keywords/edges");
        for (ImdbKeyword kw : JsonUtils.parseList(mapper, keywordsNode, ImdbKeyword.class)) {
          md.addTag(kw.node.text);
        }
      }

      // poster
      JsonNode primaryImage = JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/primaryImage");
      ImdbImage img = JsonUtils.parseObject(mapper, primaryImage, ImdbImage.class);
      if (img != null) {
        MediaArtwork mediaArtwork;
        int sizeOrder = 0;
        if (options.getMediaType() == MediaType.TV_EPISODE) {
          mediaArtwork = new MediaArtwork(ImdbMetadataProvider.ID, THUMB);
          sizeOrder = MediaArtwork.ThumbSizes.getSizeOrder(img.getWidth());
        }
        else {
          mediaArtwork = new MediaArtwork(ImdbMetadataProvider.ID, MediaArtworkType.POSTER);
          sizeOrder = MediaArtwork.PosterSizes.getSizeOrder(img.getWidth());
        }

        mediaArtwork.setOriginalUrl(img.url);
        mediaArtwork.setPreviewUrl(img.url); // well, yes
        mediaArtwork.setImdbId(img.id);

        // add original size
        mediaArtwork.addImageSize(img.getWidth(), img.getHeight(), img.url, sizeOrder);
        // add variants
        adoptArtworkSizes(mediaArtwork, img.getWidth());

        md.addMediaArt(mediaArtwork);
      }

      // primaryVideos for all trailers
      JsonNode primaryTrailers = JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/primaryVideos/edges");
      for (JsonNode vid : ListUtils.nullSafe(primaryTrailers)) {
        ImdbVideo video = JsonUtils.parseObject(mapper, vid.get("node"), ImdbVideo.class);
        for (ImdbPlaybackUrl vidurl : ListUtils.nullSafe(video.playbackURLs)) {
          if (vidurl.displayName.value.equalsIgnoreCase("AUTO") || vidurl.displayName.value.equalsIgnoreCase("SD")) {
            continue;
          }
          MediaTrailer trailer = new MediaTrailer();
          trailer.setProvider(ImdbMetadataProvider.ID);
          trailer.setScrapedBy(ImdbMetadataProvider.ID);
          trailer.setId(video.id);
          trailer.setDate(video.createdDate);
          trailer.setName(video.name.value);
          trailer.setQuality(vidurl.displayName.value); // SD, 480p, AUTO, ...
          // trailer.setUrl(vid.url); // IMDB urls exipre - just set ID
          md.addTrailer(trailer);
        }
      }

      JsonNode ttype = JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/titleType");
      ImdbTitleType type = JsonUtils.parseObject(mapper, ttype, ImdbTitleType.class);

      if (type != null && type.isEpisode) {
        JsonNode epNode = JsonUtils.at(node, "/props/pageProps/aboveTheFoldData/series/episodeNumber");
        ImdbEpisodeNumber ep = JsonUtils.parseObject(mapper, epNode, ImdbEpisodeNumber.class);
        if (ep != null) {
          md.setEpisodeNumber(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, ep.seasonNumber, ep.episodeNumber));
        }
      }

      // ***** MAIN column *****
      JsonNode titleMainImages = JsonUtils.at(node, "/props/pageProps/mainColumnData/titleMainImages/edges");
      for (JsonNode fanart : ListUtils.nullSafe(titleMainImages)) {
        ImdbImage i = JsonUtils.parseObject(mapper, fanart.get("node"), ImdbImage.class);
        // only parse landscape ones as fanarts
        if (i != null && i.getWidth() > i.getHeight()) {
          MediaArtwork mediaArtwork = new MediaArtwork(ImdbMetadataProvider.ID, MediaArtworkType.BACKGROUND);
          mediaArtwork.setOriginalUrl(i.url);
          mediaArtwork.setPreviewUrl(i.url); // well, yes
          mediaArtwork.setImdbId(i.id);

          // add original size
          mediaArtwork.addImageSize(i.getWidth(), i.getHeight(), i.url, MediaArtwork.FanartSizes.getSizeOrder(i.getWidth()));
          // add variants
          adoptArtworkSizes(mediaArtwork, i.getWidth());

          md.addMediaArt(mediaArtwork);
        }
      }

      JsonNode directorsNode = JsonUtils.at(node, "/props/pageProps/mainColumnData/directors");
      for (ImdbCredits directors : JsonUtils.parseList(mapper, directorsNode, ImdbCredits.class)) {
        for (ImdbCrew crew : directors.credits) {
          Person p = crew.toTmm(Person.Type.DIRECTOR);
          p.setRole(directors.category.text);
          md.addCastMember(p);
        }
      }

      JsonNode writersNode = JsonUtils.at(node, "/props/pageProps/mainColumnData/writers");
      for (ImdbCredits writers : JsonUtils.parseList(mapper, writersNode, ImdbCredits.class)) {
        for (ImdbCrew crew : writers.credits) {
          Person p = crew.toTmm(Person.Type.WRITER);
          p.setRole(writers.category.text);
          md.addCastMember(p);
        }
      }

      JsonNode arr = JsonUtils.at(node, "/props/pageProps/mainColumnData/cast/edges");
      for (JsonNode actors : ListUtils.nullSafe(arr)) {
        ImdbCast c = JsonUtils.parseObject(mapper, actors.get("node"), ImdbCast.class);
        md.addCastMember(c.toTmm(Person.Type.ACTOR));
      }

      JsonNode spokenNode = JsonUtils.at(node, "/props/pageProps/mainColumnData/spokenLanguages/spokenLanguages");
      for (ImdbIdTextType lang : JsonUtils.parseList(mapper, spokenNode, ImdbIdTextType.class)) {
        if (isScrapeLanguageNames()) {
          md.addSpokenLanguage(lang.text);
        }
        else {
          md.addSpokenLanguage(lang.id);
        }
      }

      JsonNode countriesNode = JsonUtils.at(node, "/props/pageProps/mainColumnData/countriesOfOrigin/countries");
      for (ImdbCountry country : JsonUtils.parseList(mapper, countriesNode, ImdbCountry.class)) {
        if (isScrapeLanguageNames()) {
          md.addCountry(country.text);
        }
        else {
          md.addCountry(country.id);
        }
      }

      JsonNode prods = JsonUtils.at(node, "/props/pageProps/mainColumnData/production/edges");
      for (JsonNode p : ListUtils.nullSafe(prods)) {
        md.addProductionCompany(p.at("/node/company/companyText/text").asText());
      }
    }
    catch (Exception e) {
      getLogger().warn("Error parsing JSON: '{}'", e);
      throw e;
    }
  }

  /**
   * parses the video page directly, and gets fresh encoded urls (they have an expiry in them!)
   *
   * @param trailer
   * @return
   * @throws Exception
   */
  protected String getFreshUrlForTrailer(MediaTrailer trailer, String language, String country) throws Exception {
    Callable<Document> worker = new ImdbWorker(constructUrl("video/", trailer.getId()), language, country, true);
    Future<Document> futureVid = executor.submit(worker);
    Document doc = futureVid.get();

    String json = doc.getElementById("__NEXT_DATA__").data();
    JsonNode node = mapper.readTree(json);
    JsonNode vidNode = JsonUtils.at(node, "/props/pageProps/videoPlaybackData/video");
    if (!vidNode.isMissingNode()) {
      ImdbVideo video = JsonUtils.parseObject(mapper, vidNode, ImdbVideo.class);
      for (ImdbPlaybackUrl vid : ListUtils.nullSafe(video.playbackURLs)) {
        if (vid.displayName.value.equals(trailer.getQuality())) {
          return vid.url;
        }
      }
    }
    return "";
  }

  protected void parseReferencePage(Document doc, MediaSearchAndScrapeOptions options, MediaMetadata md) {
    /*
     * title and year have the following structure
     *
     * <div id="tn15title"><h1>Merida - Legende der Highlands <span>(<a href="/year/2012/">2012</a>) <span class="pro-link">...</span> <span
     * class="title-extra">Brave <i>(original title)</i></span> </span></h1> </div>
     */

    // title
    Element title = doc.getElementsByAttributeValue("name", "title").first();
    if (title != null) {
      String movieTitle = cleanString(title.attr("content"));
      // detect mini series here
      if (movieTitle.contains("TV Mini-Series")) {
        md.addGenre(MediaGenres.MINI_SERIES);
      }

      int yearStart = movieTitle.lastIndexOf('(');
      if (yearStart > 0) {
        movieTitle = movieTitle.substring(0, yearStart - 1).strip();
        md.setTitle(movieTitle);
      }
    }

    // original title and year
    Element originalTitleYear = doc.getElementsByAttributeValue("property", "og:title").first();
    if (originalTitleYear != null) {
      String content = originalTitleYear.attr("content");
      int startOfYear = content.lastIndexOf('(');
      if (startOfYear > 0) {
        // noo - this is NOT the original title!!! (seems always english?) parse from AKAs page...
        String originalTitle = content.substring(0, startOfYear - 1).strip();
        md.setOriginalTitle(originalTitle);

        String yearText = content.substring(startOfYear);

        // search year
        Pattern yearPattern = Pattern.compile("[1-2][0-9]{3}");
        Matcher matcher = yearPattern.matcher(yearText);
        while (matcher.find()) {
          if (matcher.group(0) != null) {
            String movieYear = matcher.group(0);
            try {
              md.setYear(Integer.parseInt(movieYear));
              break;
            }
            catch (Exception ignored) {
              // nothing to do here
            }
          }
        }
      }
    }

    // poster
    Element poster = doc.getElementsByAttributeValue("property", "og:image").first();
    if (poster != null) {
      String posterUrl = poster.attr("content");

      int fileStart = posterUrl.lastIndexOf('/');
      if (fileStart > 0) {
        int parameterStart = posterUrl.indexOf('_', fileStart);
        if (parameterStart > 0) {
          int startOfExtension = posterUrl.lastIndexOf('.');
          if (startOfExtension > parameterStart) {
            posterUrl = posterUrl.substring(0, parameterStart) + posterUrl.substring(startOfExtension);
          }
        }
      }
      processMediaArt(md, MediaArtwork.MediaArtworkType.POSTER, posterUrl);
    }

    /*
     * <div class="starbar-meta"> <b>7.4/10</b> &nbsp;&nbsp;<a href="ratings" class="tn15more">52,871 votes</a>&nbsp;&raquo; </div>
     */

    // rating and rating count
    Element ratingElement = doc.getElementsByClass("ipl-rating-star__rating").first();
    if (ratingElement != null) {
      String ratingAsString = ratingElement.ownText().replace(",", ".");
      Element votesElement = doc.getElementsByClass("ipl-rating-star__total-votes").first();
      if (votesElement != null) {
        String countAsString = votesElement.ownText().replaceAll("[.,()]", "").strip();
        try {
          MediaRating rating = new MediaRating(MediaMetadata.IMDB);
          rating.setRating(Float.parseFloat(ratingAsString));
          rating.setVotes(MetadataUtil.parseInt(countAsString));
          md.addRating(rating);
        }
        catch (Exception e) {
          getLogger().trace("could not parse rating/vote count: {}", e.getMessage());
        }
      }
    }

    // top250
    Element topRatedElement = doc.getElementsByAttributeValue("href", "/chart/top").first();
    if (topRatedElement != null) {
      Pattern topPattern = Pattern.compile("Top Rated Movies: #([0-9]{1,3})");
      Matcher matcher = topPattern.matcher(topRatedElement.ownText());
      while (matcher.find()) {
        if (matcher.group(1) != null) {
          try {
            String top250Text = matcher.group(1);
            md.setTop250(Integer.parseInt(top250Text));
          }
          catch (Exception e) {
            getLogger().trace("could not parse top250: {}", e.getMessage());
          }
        }
      }
    }

    // releasedate
    Element releaseDateElement = doc.getElementsByAttributeValue("href", "/title/" + options.getImdbId().toLowerCase(Locale.ROOT) + "/releaseinfo")
        .first();
    if (releaseDateElement != null) {
      String releaseDateText = releaseDateElement.ownText();
      int startOfCountry = releaseDateText.indexOf('(');
      if (startOfCountry > 0) {
        releaseDateText = releaseDateText.substring(0, startOfCountry - 1).strip();
      }
      md.setReleaseDate(parseDate(releaseDateText));
    }

    Elements elements = doc.getElementsByClass("ipl-zebra-list__label");
    for (Element element : elements) {
      // only parse tds
      if (!"td".equals(element.tag().getName())) {
        continue;
      }

      String elementText = element.ownText();

      if (elementText.equals("Plot Keywords")) {
        parseKeywords(element, md);
      }

      if (elementText.equals("Taglines")) {
        Element taglineElement = element.nextElementSibling();
        if (taglineElement != null) {
          String tagline = cleanString(taglineElement.ownText().replace("»", ""));
          md.setTagline(tagline);
        }
      }

      if (elementText.equals("Genres")) {
        Element nextElement = element.nextElementSibling();
        if (nextElement != null) {
          Elements genreElements = nextElement.getElementsByAttributeValueStarting("href", "/genre/");

          for (Element genreElement : genreElements) {
            String genreText = genreElement.ownText();
            md.addGenre(getTmmGenre(genreText));
          }
        }
      }

      /*
       * Old HTML, but maybe the same content formart <div class="info"><h5>Runtime:</h5><div class="info-content">162 min | 171 min (special edition)
       * | 178 min (extended cut)</div></div>
       */
      if (elementText.equals("Runtime")) {
        Element nextElement = element.nextElementSibling();
        if (nextElement != null) {
          Element runtimeElement = nextElement.getElementsByClass("ipl-inline-list__item").first();
          if (runtimeElement != null) {
            String first = runtimeElement.ownText().split("\\|")[0];
            String runtimeAsString = cleanString(first.replace("min", ""));
            int runtime = 0;
            try {
              runtime = Integer.parseInt(runtimeAsString);
            }
            catch (Exception e) {
              // try to filter out the first number we find
              Pattern runtimePattern = Pattern.compile("([0-9]{2,3})");
              Matcher matcher = runtimePattern.matcher(runtimeAsString);
              if (matcher.find()) {
                runtime = Integer.parseInt(matcher.group(0));
              }
            }
            md.setRuntime(runtime);
          }
        }
      }

      if (elementText.equals("Country")) {
        Element nextElement = element.nextElementSibling();
        if (nextElement != null) {
          Elements countryElements = nextElement.getElementsByAttributeValueStarting("href", "/country/");
          Pattern pattern = Pattern.compile("/country/(.*)");

          for (Element countryElement : countryElements) {
            Matcher matcher = pattern.matcher(countryElement.attr("href"));
            if (matcher.matches()) {
              if (isScrapeLanguageNames()) {
                md.addCountry(
                    LanguageUtils.getLocalizedCountryForLanguage(options.getLanguage().getLanguage(), countryElement.text(), matcher.group(1)));
              }
              else {
                md.addCountry(matcher.group(1));
              }
            }
          }
        }
      }

      if (elementText.equals("Language")) {
        Element nextElement = element.nextElementSibling();
        if (nextElement != null) {
          Elements languageElements = nextElement.getElementsByAttributeValueStarting("href", "/language/");
          Pattern pattern = Pattern.compile("/language/(.*)");

          for (Element languageElement : languageElements) {
            Matcher matcher = pattern.matcher(languageElement.attr("href"));
            if (matcher.matches()) {
              if (isScrapeLanguageNames()) {
                md.addSpokenLanguage(LanguageUtils.getLocalizedLanguageNameFromLocalizedString(options.getLanguage().toLocale(),
                    languageElement.text(), matcher.group(1)));
              }
              else {
                md.addSpokenLanguage(matcher.group(1));
              }
            }
          }
        }
      }

      if (elementText.equals("Certification")) {
        Element nextElement = element.nextElementSibling();
        if (nextElement != null) {
          String languageCode = options.getCertificationCountry().getAlpha2();
          Elements certificationElements = nextElement.getElementsByAttributeValueStarting("href", "/search/title?certificates=" + languageCode);
          boolean done = false;
          for (Element certificationElement : certificationElements) {
            String certText = certificationElement.ownText();
            int startOfCert = certText.indexOf(':');
            if (startOfCert > 0 && certText.length() > startOfCert + 1) {
              certText = certText.substring(startOfCert + 1);
            }

            MediaCertification certification = MediaCertification.getCertification(options.getCertificationCountry(), certText);
            if (certification != null) {
              md.addCertification(certification);
              done = true;
              // break; // might be multiple, like US!
            }
          }

          if (!done && languageCode.equals("DE")) {
            certificationElements = nextElement.getElementsByAttributeValueStarting("href", "/search/title?certificates=XWG");
            for (Element certificationElement : certificationElements) {
              String certText = certificationElement.ownText();
              int startOfCert = certText.indexOf(':');
              if (startOfCert > 0 && certText.length() > startOfCert + 1) {
                certText = certText.substring(startOfCert + 1);
              }

              MediaCertification certification = MediaCertification.getCertification(options.getCertificationCountry(), certText);
              if (certification != null) {
                md.addCertification(certification);
                break;
              }
            }
          }

        }
      }
    }

    // director
    Element directorsElement = doc.getElementById("directors");
    while (directorsElement != null && !"header".equals(directorsElement.tag().getName())) {
      directorsElement = directorsElement.parent();
    }
    if (directorsElement != null) {
      directorsElement = directorsElement.nextElementSibling();
    }
    if (directorsElement != null) {
      for (Element directorElement : directorsElement.getElementsByClass("name")) {
        String director = directorElement.text().strip();

        Person cm = new Person(Person.Type.DIRECTOR, director);
        // profile path
        Element anchor = directorElement.getElementsByAttributeValueStarting("href", "/name/").first();
        if (anchor != null) {
          Matcher matcher = PERSON_ID_PATTERN.matcher(anchor.attr("href"));
          if (matcher.find()) {
            if (matcher.group(0) != null) {
              cm.setProfileUrl("http://www.imdb.com" + matcher.group(0));
            }
            if (matcher.group(1) != null) {
              cm.setId(ImdbMetadataProvider.ID, matcher.group(1));
            }
          }
        }
        md.addCastMember(cm);
      }
    }

    // actors
    Element castTableElement = doc.getElementsByClass("cast_list").first();
    if (castTableElement != null) {
      Elements castListLabel = castTableElement.getElementsByClass("castlist_label");
      Elements tr = castTableElement.getElementsByTag("tr");
      for (Element row : tr) {
        // check if we're at the uncredited cast members
        if (!isScrapeUncreditedActors() && castListLabel.size() > 1 && row.children().contains(castListLabel.get(1))) {
          break;
        }

        Person cm = parseCastMember(row);
        if (cm != null && StringUtils.isNotEmpty(cm.getName())) {
          cm.setType(ACTOR);
          md.addCastMember(cm);
        }
      }
    }

    // writers
    Element writersElement = doc.getElementById("writers");
    while (writersElement != null && !"header".equals(writersElement.tag().getName())) {
      writersElement = writersElement.parent();
    }
    if (writersElement != null) {
      writersElement = writersElement.nextElementSibling();
    }
    if (writersElement != null) {
      Elements writersElements = writersElement.getElementsByAttributeValueStarting("href", "/name/");

      for (Element writerElement : writersElements) {
        String writer = cleanString(writerElement.ownText());
        Person cm = new Person(WRITER, writer);
        // profile path
        Element anchor = writerElement.getElementsByAttributeValueStarting("href", "/name/").first();
        if (anchor != null) {
          Matcher matcher = PERSON_ID_PATTERN.matcher(anchor.attr("href"));
          if (matcher.find()) {
            if (matcher.group(0) != null) {
              cm.setProfileUrl("http://www.imdb.com" + matcher.group(0));
            }
            if (matcher.group(1) != null) {
              cm.setId(ImdbMetadataProvider.ID, matcher.group(1));
            }
          }
        }
        md.addCastMember(cm);
      }
    }

    // producers
    Element producersElement = doc.getElementById("producers");
    while (producersElement != null && !"header".equals(producersElement.tag().getName())) {
      producersElement = producersElement.parent();
    }
    if (producersElement != null) {
      producersElement = producersElement.nextElementSibling();
    }
    if (producersElement != null) {
      Elements producersElements = producersElement.getElementsByAttributeValueStarting("href", "/name/");

      for (Element producerElement : producersElements) {
        String producer = cleanString(producerElement.ownText());
        Person cm = new Person(PRODUCER, producer);
        md.addCastMember(cm);
      }
    }

    // producers
    Elements prodCompHeaderElements = doc.getElementsByClass("ipl-list-title");
    Element prodCompHeaderElement = null;

    for (Element possibleProdCompHeaderEl : prodCompHeaderElements) {
      if (possibleProdCompHeaderEl.ownText().equals("Production Companies")) {
        prodCompHeaderElement = possibleProdCompHeaderEl;
        break;
      }
    }

    while (prodCompHeaderElement != null && !"header".equals(prodCompHeaderElement.tag().getName())) {
      prodCompHeaderElement = prodCompHeaderElement.parent();
    }
    if (prodCompHeaderElement != null) {
      prodCompHeaderElement = prodCompHeaderElement.nextElementSibling();
    }
    if (prodCompHeaderElement != null) {
      Elements prodCompElements = prodCompHeaderElement.getElementsByAttributeValueStarting("href", "/company/");

      for (Element prodCompElement : prodCompElements) {
        String prodComp = prodCompElement.ownText();
        md.addProductionCompany(prodComp);
      }
    }
  }

  private void parseKeywords(Element element, MediaMetadata md) {
    // <td>
    // <ul class="ipl-inline-list">
    // <li class="ipl-inline-list__item"><a href="/keyword/male-alien">male-alien</a></li>
    // <li class="ipl-inline-list__item"><a href="/keyword/planetary-romance">planetary-romance</a></li>
    // <li class="ipl-inline-list__item"><a href="/keyword/female-archer">female-archer</a></li>
    // <li class="ipl-inline-list__item"><a href="/keyword/warrioress">warrioress</a></li>
    // <li class="ipl-inline-list__item"><a href="/keyword/original-story">original-story</a></li>
    // <li class="ipl-inline-list__item"><a href="/title/tt0499549/keywords">See All (379) »</a></li>
    // </ul>
    // </td>

    Element parent = element.nextElementSibling();
    Elements keywords = parent.getElementsByClass("ipl-inline-list__item");
    for (Element keyword : keywords) {
      Element a = keyword.getElementsByTag("a").first();
      if (a != null && !a.attr("href").contains("/keywords")) {
        md.addTag(a.ownText());
      }
    }
  }

  protected void parseKeywordsPage(Document doc, MediaSearchAndScrapeOptions options, MediaMetadata md) {
    int maxKeywordCount = getMaxKeywordCount();
    int counter = md.getTags().size(); // initialize with already scraped ones

    // new style via JSON
    try {
      String json = doc.getElementById("__NEXT_DATA__").data();
      // System.out.println(json);
      JsonNode node = mapper.readTree(json);
      JsonNode keywordsNode = JsonUtils.at(node, "/props/pageProps/contentData/section/items");
      for (ImdbTitleKeyword kw : JsonUtils.parseList(mapper, keywordsNode, ImdbTitleKeyword.class)) {
        md.addTag(kw.rowTitle);
        counter++;
        if (counter >= maxKeywordCount) {
          break;
        }
      }
    }
    catch (Exception e) {
      getLogger().warn("Error parsing JSON: '{}'", e);
    }

    // new style as of may 2023
    if (md.getTags().size() < maxKeywordCount) {
      counter = 0;
      Elements keywords = doc.getElementsByClass("ipc-metadata-list-summary-item__t");
      for (Element keyword : keywords) {
        if (StringUtils.isNotBlank(keyword.text())) {
          md.addTag(keyword.text());
          counter++;
          if (counter >= maxKeywordCount) {
            break;
          }
        }
      }
    }

    // old style
    if (md.getTags().size() < maxKeywordCount) {
      counter = 0;
      Element div = doc.getElementById("keywords_content");
      if (div != null) {
        Elements keywords = div.getElementsByClass("sodatext");
        for (Element keyword : keywords) {
          if (StringUtils.isNotBlank(keyword.text())) {
            md.addTag(keyword.text());
            counter++;
            if (counter >= maxKeywordCount) {
              break;
            }
          }
        }
      }
    }
  }

  protected void parsePlotsummaryPage(Document doc, MediaSearchAndScrapeOptions options, MediaMetadata md) {

    // NEW style as of may 2023
    // JSON is weird - do not do that
    // just take first summary
    if (md.getPlot().isEmpty()) {
      Elements sum = doc.getElementsByClass("ipc-html-content-inner-div");
      String plot = cleanString(sum.text());
      md.setPlot(plot);
    }

    // OLD style
    // just take first summary
    // <li class="ipl-zebra-list__item" id="summary-ps21700000">
    // <p>text text text text </p>
    // <div class="author-container">
    // <em>&mdash;<a href="/search/title?plot_author=author">Author Name</a></em>
    // </div>
    // </li>
    if (md.getPlot().isEmpty()) {
      Element zebraList = doc.getElementById("plot-summaries-content");
      if (zebraList != null) {
        Elements p = zebraList.getElementsByClass("ipl-zebra-list__item");
        if (!p.isEmpty()) {
          Element em = p.get(0);

          // remove author
          Elements authors = em.getElementsByClass("author-container");
          if (!authors.isEmpty()) {
            authors.get(0).remove();
          }

          if (!"no-summary-content".equals(em.id())) {
            String plot = cleanString(em.text());
            md.setPlot(plot);
          }
        }
      }
    }
  }

  protected void parseReleaseinfoPageJson(Document doc, MediaSearchAndScrapeOptions options, MediaMetadata md) throws Exception {
    try {
      String json = doc.getElementById("__NEXT_DATA__").data();
      JsonNode node = mapper.readTree(json);

      JsonNode itemsNode = JsonUtils.at(node, "/props/pageProps/contentData/categories");
      for (ImdbCategory cat : JsonUtils.parseList(mapper, itemsNode, ImdbCategory.class)) {
        if (cat.section != null) {
          if (cat.section.items.size() > 0) {
            ImdbSectionItem item = cat.section.items.get(0);
            if (item.listContent.size() > 0) {
              Date parsedDate = parseDate(item.listContent.get(0).text);
              if (parsedDate != null) {
                md.setReleaseDate(parsedDate);
                break;
              }
            }
          }
        }
      }
    }
    catch (Exception e) {
      getLogger().warn("Error parsing ReleaseinfoPageJson: '{}'", e);
      throw e;
    }
  }

  protected void parseReleaseinfoPage(Document doc, MediaSearchAndScrapeOptions options, MediaMetadata md) {
    Date releaseDate = null;
    Pattern pattern = Pattern.compile("/calendar/\\?region=(.{2})");

    String releaseDateCountry = options.getReleaseDateCountry();
    boolean parseLocalReleaseDate = Boolean.TRUE.equals(config.getValueAsBool(LOCAL_RELEASE_DATE, false));
    boolean includePremiereDate = Boolean.TRUE.equals(config.getValueAsBool(INCLUDE_PREMIERE_DATE, true));

    Element tableReleaseDates = doc.getElementById("release_dates");
    if (tableReleaseDates != null) {
      Elements rows = tableReleaseDates.getElementsByTag("tr");
      // first round: check the release date for the first one with the requested country
      for (Element row : rows) {
        // check if we want premiere dates
        if (row.text().contains("(premiere)") && !includePremiereDate) {
          continue;
        }

        if (!parseLocalReleaseDate) {
          // global first release
          Element column = row.getElementsByClass("release_date").first();
          if (column != null) {
            Date parsedDate = parseDate(column.text());
            if (parsedDate != null) {
              releaseDate = parsedDate;
              break;
            }
          }
        }
        else {
          // local release date
          // get the anchor
          Element anchor = row.getElementsByAttributeValueStarting("href", "/calendar/").first();
          if (anchor != null) {
            Matcher matcher = pattern.matcher(anchor.attr("href"));
            if (matcher.find()) {
              String country = matcher.group(1);

              Element column = row.getElementsByClass("release_date").first();
              if (column != null) {
                Date parsedDate = parseDate(column.text());
                // do not overwrite any parsed date with a null value!
                if (parsedDate != null && releaseDateCountry.equalsIgnoreCase(country)) {
                  releaseDate = parsedDate;
                  break;
                }
              }
            }
          }
        }
      }
    }

    // new way; iterating over class name items
    if (releaseDate == null) {
      Elements rows = doc.getElementsByClass("release-date-item");
      for (Element row : rows) {
        // check if we want premiere dates
        if (row.text().contains("(premiere)") && !includePremiereDate) {
          continue;
        }

        if (!parseLocalReleaseDate) {
          // global first release
          Element column = row.getElementsByClass("release-date-item__date").first();
          Date parsedDate = parseDate(column.text());
          if (parsedDate != null) {
            releaseDate = parsedDate;
            break;
          }
        }
        else {
          Element anchor = row.getElementsByAttributeValueStarting("href", "/calendar/").first();
          if (anchor != null) {
            Matcher matcher = pattern.matcher(anchor.attr("href"));
            // continue if we either do not have found any date yet or the country matches
            if (matcher.find()) {
              String country = matcher.group(1);

              Element column = row.getElementsByClass("release-date-item__date").first();
              if (column != null) {
                Date parsedDate = parseDate(column.text());
                // do not overwrite any parsed date with a null value!
                if (parsedDate != null && releaseDateCountry.equalsIgnoreCase(country)) {
                  releaseDate = parsedDate;
                  break;
                }
              }
            }
          }
        }
      }
    }

    // no matching local release date found; take the first one
    if (releaseDate == null && tableReleaseDates != null) {
      Elements rows = tableReleaseDates.getElementsByTag("tr");
      // first round: check the release date for the first one with the requested country
      for (Element row : rows) {
        // check if we want premiere dates
        if (row.text().contains("(premiere)") && !includePremiereDate) {
          continue;
        }

        // global first release
        Element column = row.getElementsByClass("release_date").first();
        Date parsedDate = parseDate(column.text());
        if (parsedDate != null) {
          releaseDate = parsedDate;
          break;
        }
      }
    }

    if (releaseDate == null) {
      Elements rows = doc.getElementsByClass("release-date-item");
      for (Element row : rows) {
        // check if we want premiere dates
        if (row.text().contains("(premiere)") && !includePremiereDate) {
          continue;
        }

        // global first release
        Element column = row.getElementsByClass("release-date-item__date").first();
        Date parsedDate = parseDate(column.text());
        if (parsedDate != null) {
          releaseDate = parsedDate;
          break;
        }
      }
    }

    if (releaseDate != null) {
      md.setReleaseDate(releaseDate);
    }
  }

  protected Person parseCastMember(Element row) {

    Element nameElement = row.getElementsByAttributeValueStarting("itemprop", "name").first();
    if (nameElement == null) {
      return null;
    }
    String name = cleanString(nameElement.ownText());
    String characterName = "";

    Element characterElement = row.getElementsByClass("character").first();
    if (characterElement != null) {
      characterName = cleanString(characterElement.text());
      // and now strip off trailing commentaries like - (120 episodes, 2006-2014)
      characterName = characterName.replaceAll("\\(.*?\\)$", "").strip();
    }

    String image = "";
    Element imageElement = row.getElementsByTag("img").first();
    if (imageElement != null) {
      String imageSrc = imageElement.attr("loadlate");

      if (StringUtils.isNotBlank(imageSrc)) {
        imageSrc = scaleImage(imageSrc, 300, 450);
      }
      image = imageSrc;
    }

    // profile path
    String profilePath = "";
    String id = "";
    Element anchor = row.getElementsByAttributeValueStarting("href", "/name/").first();
    if (anchor != null) {
      Matcher matcher = PERSON_ID_PATTERN.matcher(anchor.attr("href"));
      if (matcher.find()) {
        if (matcher.group(0) != null) {
          profilePath = "http://www.imdb.com" + matcher.group(0);
        }
        if (matcher.group(1) != null) {
          id = matcher.group(1);
        }
      }
    }

    Person cm = new Person();
    cm.setId(ImdbMetadataProvider.ID, id);
    cm.setName(name);
    cm.setRole(characterName);
    cm.setThumbUrl(image);
    cm.setProfileUrl(profilePath);
    return cm;
  }

  private String scaleImage(String url, int desiredWidth, int desiredHeight) {
    String imageSrc = url;

    // parse out the rescale/crop params
    Matcher matcher = IMAGE_SCALING_PATTERN.matcher(imageSrc);
    if (matcher.find()) {
      try {
        String direction = matcher.group(1);
        int scaling = MetadataUtil.parseInt(matcher.group(2), 0);
        int cropLeft = MetadataUtil.parseInt(matcher.group(3));
        int cropTop = MetadataUtil.parseInt(matcher.group(4));
        int actualWidth = MetadataUtil.parseInt(matcher.group(5));
        int actualHeight = MetadataUtil.parseInt(matcher.group(6));

        if (scaling > 0) {
          if ("X".equals(direction)) {
            // https://stackoverflow.com/a/73501833
            // scale horizontally
            imageSrc = imageSrc.replace("SX" + scaling, "UY" + desiredHeight);
          }
          else if ("Y".equals(direction)) {
            // scale vertically
            imageSrc = imageSrc.replace("SY" + scaling, "UY" + desiredHeight);
          }

          int newCropLeft = cropLeft;
          int newCropTop = cropTop;

          float scaleFactor;

          if (actualWidth / (float) actualHeight > desiredWidth / (float) desiredHeight) {
            scaleFactor = desiredHeight / (float) actualHeight;
          }
          else {
            scaleFactor = desiredWidth / (float) actualWidth;
          }

          if (cropLeft > 0) {
            newCropLeft = (int) (cropLeft * scaleFactor + (actualWidth * scaleFactor - desiredWidth) / 2);
          }
          if (cropTop > 0) {
            newCropTop = (int) (cropTop * scaleFactor + (actualHeight * scaleFactor - desiredHeight) / 2);
          }

          imageSrc = imageSrc.replace("CR" + cropLeft + "," + cropTop + "," + actualWidth + "," + actualHeight,
              "CR" + newCropLeft + "," + newCropTop + "," + desiredWidth + "," + desiredHeight);
        }
      }
      catch (Exception e) {
        getLogger().debug("Could not parse scaling/cropping params - '{}'", e.getMessage());
      }
    }

    return imageSrc;
  }

  protected Date parseDate(String dateAsSting) {
    try {
      return StrgUtils.parseDate(dateAsSting);
    }
    catch (ParseException e) {
      getLogger().trace("could not parse date: {}", e.getMessage());
    }
    return null;
  }

  protected Map<String, Integer> parseTop250(String url) {
    Map<String, Integer> titles = new HashMap<>();

    try {
      Callable<Document> worker = new ImdbWorker(constructUrl(url), "en", "US", true); // don't care about lang, since we only get IDs
      Future<Document> futureTop250 = executor.submit(worker);
      Document doc = futureTop250.get();
      String json = doc.getElementById("__NEXT_DATA__").data();
      if (!json.isEmpty()) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        JsonNode chartNode = JsonUtils.at(node, "/props/pageProps/pageData/chartTitles/edges");
        for (ImdbChartTitleEdge ch : JsonUtils.parseList(mapper, chartNode, ImdbChartTitleEdge.class)) {
          titles.put(ch.node.id, ch.currentRank);
        }
      }
    }
    catch (Exception e) {
      getLogger().warn("Could not get TOP250 listing - '{}'", e.getMessage());
    }

    return titles;
  }

  /****************************************************************************
   * local helper classes
   ****************************************************************************/
  protected class ImdbWorker implements Callable<Document> {
    private final String  pageUrl;
    private final String  language;
    private final String  country;
    private final boolean useCachedUrl;

    ImdbWorker(String url, String language, String country) {
      this(url, language, country, true);
    }

    ImdbWorker(String url, String language, String country, boolean useCachedUrl) {
      this.pageUrl = url;
      this.language = language;
      this.country = country;
      this.useCachedUrl = useCachedUrl;
    }

    @Override
    public Document call() throws Exception {
      Document doc = null;

      Url url;

      try {
        if (useCachedUrl) {
          url = new OnDiskCachedUrl(this.pageUrl, 15, TimeUnit.MINUTES);
        }
        else {
          url = new Url(this.pageUrl);
        }
        url.addHeader("Accept-Language", getAcceptLanguage(language, country));
      }
      catch (Exception e) {
        getLogger().debug("tried to fetch imdb page {} - {}", this.pageUrl, e);
        throw new ScrapeException(e);
      }

      try (InputStream is = url.getInputStream()) {
        doc = Jsoup.parse(is, "UTF-8", "");
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        getLogger().debug("tried to fetch imdb page {} - {}", this.pageUrl, e);
        throw e;
      }

      return doc;
    }
  }

  protected void processMediaArt(MediaMetadata md, MediaArtwork.MediaArtworkType type, String image) {
    MediaArtwork ma = new MediaArtwork(ImdbMetadataProvider.ID, type);

    ma.setOriginalUrl(image);

    // https://stackoverflow.com/a/73501833
    // create preview url (width = 342 as in TMDB)
    String extension = FilenameUtils.getExtension(image);
    String previewUrl = image.replace("." + extension, "_SX342." + extension);
    ma.setPreviewUrl(previewUrl);

    ma.addImageSize(0, 0, image, 0); // no size available

    md.addMediaArt(ma);
  }

  protected void adoptArtworkSizes(MediaArtwork artwork, int width) {
    switch (artwork.getType()) {
      case POSTER:
        for (MediaArtwork.PosterSizes posterSizes : MediaArtwork.PosterSizes.values()) {
          if (width > posterSizes.getWidth()) {
            addArtworkSize(artwork, posterSizes.getWidth(), posterSizes.getHeight(), posterSizes.getOrder());
          }
        }
        break;

      case BACKGROUND:
        for (MediaArtwork.FanartSizes fanartSizes : MediaArtwork.FanartSizes.values()) {
          if (width > fanartSizes.getWidth()) {
            addArtworkSize(artwork, fanartSizes.getWidth(), fanartSizes.getHeight(), fanartSizes.getOrder());
          }
        }
        break;

      case THUMB:
        for (MediaArtwork.ThumbSizes thumbSizes : MediaArtwork.ThumbSizes.values()) {
          if (width > thumbSizes.getWidth()) {
            addArtworkSize(artwork, thumbSizes.getWidth(), thumbSizes.getHeight(), thumbSizes.getOrder());
          }
        }
        break;

      default:
        break;
    }
  }

  private void addArtworkSize(MediaArtwork artwork, int width, int height, int sizeOrder) {
    // get the highest artwork size (from scraper)
    ImageSizeAndUrl originalSize = !artwork.getImageSizes().isEmpty() ? artwork.getImageSizes().get(0) : null;
    if (originalSize != null) {
      if (originalSize.getWidth() > width || originalSize.getHeight() > height) {
        // only downscale
        String image = artwork.getOriginalUrl();
        String extension = FilenameUtils.getExtension(image);
        // https://stackoverflow.com/a/73501833
        String defaultUrl = image.replace("." + extension, "_UX" + width + "." + extension);
        artwork.setLanguage("?"); // since we do not know which language the artwork is in, set it to a value which will never match
        artwork.addImageSize(width, height, defaultUrl, sizeOrder);
      }
    }
    else {
      // no size provided by scraper, just scale it
      String image = artwork.getOriginalUrl();
      String extension = FilenameUtils.getExtension(image);
      // https://stackoverflow.com/a/73501833
      String defaultUrl = image.replace("." + extension, "_UX" + width + "." + extension);
      artwork.setLanguage("?"); // since we do not know which language the artwork is in, set it to a value which will never match
      artwork.addImageSize(width, height, defaultUrl, sizeOrder);
    }
  }

  protected String cleanString(String oldString) {
    if (StringUtils.isEmpty(oldString)) {
      return "";
    }
    // remove non-breaking spaces
    String newString = StringUtils.strip(oldString.replace(String.valueOf((char) 160), " "));

    // if there is a leading AND trailing quotation marks (e.g. at TV shows) - remove them
    if (newString.startsWith("\"") && newString.endsWith("\"")) {
      newString = StringUtils.stripEnd(StringUtils.stripStart(newString, "\""), "\"");
    }

    // and trim
    return newString;
  }

  /*
   * Maps scraper Genres to internal TMM genres
   */
  protected MediaGenres getTmmGenre(String genre) {
    MediaGenres g = null;
    if (StringUtils.isBlank(genre)) {
      return null;
    }
    // @formatter:off
    else if (genre.equals("Action")) {
      g = MediaGenres.ACTION;
    } else if (genre.equals("Adult")) {
      g = MediaGenres.EROTIC;
    } else if (genre.equals("Adventure")) {
      g = MediaGenres.ADVENTURE;
    } else if (genre.equals("Animation")) {
      g = MediaGenres.ANIMATION;
    } else if (genre.equals("Biography")) {
      g = MediaGenres.BIOGRAPHY;
    } else if (genre.equals("Comedy")) {
      g = MediaGenres.COMEDY;
    } else if (genre.equals("Crime")) {
      g = MediaGenres.CRIME;
    } else if (genre.equals("Documentary")) {
      g = MediaGenres.DOCUMENTARY;
    } else if (genre.equals("Drama")) {
      g = MediaGenres.DRAMA;
    } else if (genre.equals("Family")) {
      g = MediaGenres.FAMILY;
    } else if (genre.equals("Fantasy")) {
      g = MediaGenres.FANTASY;
    } else if (genre.equals("Film-Noir")) {
      g = MediaGenres.FILM_NOIR;
    } else if (genre.equals("Game-Show")) {
      g = MediaGenres.GAME_SHOW;
    } else if (genre.equals("History")) {
      g = MediaGenres.HISTORY;
    } else if (genre.equals("Horror")) {
      g = MediaGenres.HORROR;
    } else if (genre.equals("Music")) {
      g = MediaGenres.MUSIC;
    } else if (genre.equals("Musical")) {
      g = MediaGenres.MUSICAL;
    } else if (genre.equals("Mystery")) {
      g = MediaGenres.MYSTERY;
    } else if (genre.equals("News")) {
      g = MediaGenres.NEWS;
    } else if (genre.equals("Reality-TV")) {
      g = MediaGenres.REALITY_TV;
    } else if (genre.equals("Romance")) {
      g = MediaGenres.ROMANCE;
    } else if (genre.equals("Sci-Fi")) {
      g = MediaGenres.SCIENCE_FICTION;
    } else if (genre.equals("Short")) {
      g = MediaGenres.SHORT;
    } else if (genre.equals("Sport")) {
      g = MediaGenres.SPORT;
    } else if (genre.equals("Talk-Show")) {
      g = MediaGenres.TALK_SHOW;
    } else if (genre.equals("Thriller")) {
      g = MediaGenres.THRILLER;
    } else if (genre.equals("War")) {
      g = MediaGenres.WAR;
    } else if (genre.equals("Western")) {
      g = MediaGenres.WESTERN;
    }
    // @formatter:on
    if (g == null) {
      g = MediaGenres.getGenre(genre);
    }
    return g;
  }
}
