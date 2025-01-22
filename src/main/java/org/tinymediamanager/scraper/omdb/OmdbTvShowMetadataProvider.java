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
package org.tinymediamanager.scraper.omdb;

import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroupType.AIRED;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.scraper.util.TvUtils;
import org.tinymediamanager.scraper.util.UrlUtil;

public class OmdbTvShowMetadataProvider extends OmdbMetadataProvider implements ITvShowMetadataProvider {

  private static final Logger                                LOGGER                 = LoggerFactory.getLogger(OmdbTvShowMetadataProvider.class);
  private static final CacheMap<String, List<MediaMetadata>> EPISODE_LIST_CACHE_MAP = new CacheMap<>(60, 10);

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
    LOGGER.debug("getMetadata() - TvShow: '{}'", options);

    // id from the options
    String imdbId = getImdbId(options);

    // imdbid check
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.warn("no imdb id found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    Document doc = null;
    try {
      doc = UrlUtil
          .parseDocumentFromUrl("https://www.omdbapi.com/?apikey=" + getApiKey() + "&i=" + imdbId + "&type=series&plot=full&tomatoes=true&r=xml");
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (doc == null || doc.childrenSize() == 0) {
      LOGGER.warn("no result found");
      throw new NothingFoundException();
    }

    MediaMetadata metadata = parseDetail(doc, "movie"); // is also movie for series... weird
    if (metadata == null) {
      LOGGER.warn("no result found");
      throw new NothingFoundException();
    }

    metadata.setScrapeOptions(options);
    return metadata;
  }

  @Override
  public MediaMetadata getMetadata(@NotNull TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata() - episode: '{}'", options);

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    // first get the base episode metadata which can be gathered via getEpisodeList()
    List<MediaMetadata> episodeList = getEpisodeList(options.createTvShowSearchAndScrapeOptions());

    if (episodeList.isEmpty()) {
      LOGGER.warn("EpisodeList is empty, cannot fetch episode information");
      throw new NothingFoundException();
    }

    String imdbId = "";
    for (MediaMetadata metadata : episodeList) {
      MediaEpisodeNumber episodeNumber = metadata.getEpisodeNumber(AIRED);
      if (episodeNumber == null) {
        continue;
      }

      if (seasonNr == episodeNumber.season() && episodeNr == episodeNumber.episode()) {
        imdbId = metadata.getId(MediaMetadata.IMDB).toString();
        break;
      }
    }

    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.debug("no imdb id found for season '{}' episode '{}'", seasonNr, episodeNr);
      throw new MissingIdException();
    }

    Document doc = null;
    try {
      doc = UrlUtil
          .parseDocumentFromUrl("https://www.omdbapi.com/?apikey=" + getApiKey() + "&i=" + imdbId + "&type=episode&plot=full&tomatoes=true&r=xml");
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (doc == null || doc.childrenSize() == 0) {
      LOGGER.warn("no result found");
      throw new NothingFoundException();
    }

    MediaMetadata metadata = parseDetail(doc, "movie"); // is also movie for series... weird
    if (metadata == null) {
      LOGGER.warn("no result found");
      throw new NothingFoundException();
    }

    metadata.setScrapeOptions(options);
    return metadata;
  }

  @Override
  public SortedSet<MediaSearchResult> search(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options.getSearchQuery());

    SortedSet<MediaSearchResult> mediaResult = new TreeSet<>();

    // if the imdb id is given, directly fetch the result
    if (MediaIdUtil.isValidImdbId(options.getImdbId())) {
      try {
        MediaMetadata md = getMetadata(options);

        // create the search result from the metadata
        MediaSearchResult result = new MediaSearchResult(getId(), MediaType.TV_SHOW);
        result.setMetadata(md);
        result.setTitle(md.getTitle());
        result.setIMDBId(options.getImdbId());
        result.setYear(md.getYear());

        for (MediaArtwork artwork : md.getMediaArt(MediaArtwork.MediaArtworkType.POSTER)) {
          result.setPosterUrl(artwork.getPreviewUrl());
        }

        result.setScore(1);
        mediaResult.add(result);

        return mediaResult;
      }
      catch (Exception e) {
        LOGGER.debug("could not fetch data with imdb id - '{}'", e.getMessage());
      }
    }

    Document doc = null;
    try {
      doc = UrlUtil
          .parseDocumentFromUrl("https://www.omdbapi.com/?apikey=" + getApiKey() + "&s=" + options.getSearchQuery() + "&type=series&page=1&r=xml");
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (doc == null || doc.childrenSize() == 0) {
      LOGGER.warn("no result found");
      throw new NothingFoundException();
    }

    List<MediaSearchResult> searchResults = parseSearchResults(doc, MediaType.TV_SHOW);

    // nothing found? try a direct lookup
    if (searchResults.isEmpty()) {
      try {
        doc = UrlUtil
            .parseDocumentFromUrl("https://www.omdbapi.com/?apikey=" + getApiKey() + "&t=" + options.getSearchQuery() + "&type=series&r=xml");
        if (doc != null && doc.childrenSize() != 0) {
          MediaMetadata md = parseDetail(doc, "movie");
          if (md != null) {
            md.setScrapeOptions(options);
            MediaSearchResult searchResult = new MediaSearchResult(getId(), MediaType.TV_SHOW);
            searchResult.mergeFrom(md);
            searchResults = Collections.singletonList(searchResult);
          }
        }
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.error("error searching: {}", e.getMessage());
        throw new ScrapeException(e);
      }
    }

    // calculate score
    for (MediaSearchResult searchResult : searchResults) {
      searchResult.calculateScore(options);
      mediaResult.add(searchResult);
    }

    return mediaResult;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeList(): {}", options.getSearchQuery());

    String imdbId = getImdbId(options);

    if (StringUtils.isBlank(imdbId)) {
      throw new MissingIdException("imdbId");
    }

    // look in the cache map if there is an entry
    List<MediaMetadata> episodeList = EPISODE_LIST_CACHE_MAP.get(imdbId + "_" + options.getLanguage().getLanguage());

    if (ListUtils.isNotEmpty(episodeList)) {
      // cache hit!
      return episodeList;
    }

    episodeList = new ArrayList<>();

    if (StringUtils.isBlank(getApiKey())) {
      LOGGER.warn("no API Key found");
      throw new ScrapeException(new HttpException(401, "Unauthorized"));
    }

    Document doc = null;
    try {
      doc = UrlUtil.parseDocumentFromUrl("https://www.omdbapi.com/?apikey=" + getApiKey() + "&i=" + imdbId + "&type=series&r=xml&Season=1");
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (doc == null || doc.childrenSize() == 0) {
      LOGGER.warn("no result found");
      throw new NothingFoundException();
    }

    // get the total # of seasons
    Element root = doc.getElementsByTag("root").first();
    if (root == null) {
      throw new NothingFoundException();
    }

    int seasons = MetadataUtil.parseInt(root.attr("totalSeasons"), 0);

    if (seasons <= 0) {
      throw new NothingFoundException();
    }

    // we already have season #1
    episodeList.addAll(parseEpisodes(doc, options));

    int i = 2;
    while (i <= seasons) {
      try {
        doc = UrlUtil.parseDocumentFromUrl("https://www.omdbapi.com/?apikey=" + getApiKey() + "&i=" + imdbId + "&type=series&r=xml&Season=" + i);
        episodeList.addAll(parseEpisodes(doc, options));
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        break;
      }
      i++;
    }

    // cache for further fast access
    if (!episodeList.isEmpty()) {
      EPISODE_LIST_CACHE_MAP.put(imdbId + "_" + options.getLanguage().getLanguage(), episodeList);
    }

    return episodeList;
  }

  private List<MediaMetadata> parseEpisodes(Document document, MediaSearchAndScrapeOptions options) {
    Element root = document.getElementsByTag("root").first();
    if (root == null) {
      return Collections.emptyList();
    }

    int season = MetadataUtil.parseInt(root.attr("season"), -1);

    if (season < 0) {
      return Collections.emptyList();
    }

    Elements results = document.getElementsByTag("result");
    if (results.isEmpty()) {
      return Collections.emptyList();
    }

    List<MediaMetadata> episodes = new ArrayList<>();

    for (Element result : results) {
      MediaMetadata md = new MediaMetadata(getId());
      md.setScrapeOptions(options);

      if (MediaIdUtil.isValidImdbId(result.attr("imdbID"))) {
        md.setId(MediaMetadata.IMDB, result.attr("imdbID"));
      }

      md.setEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, season, TvUtils.getEpisodeNumber(result.attr("episode"), -1));

      md.setTitle(result.attr("Title"));
      try {
        md.setReleaseDate(StrgUtils.parseDate(result.attr("released")));
      }
      catch (Exception ignored) {
        // just ignore
      }

      // IMDB rating
      try {
        MediaRating rating = new MediaRating(MediaMetadata.IMDB);
        rating.setRating(Float.parseFloat(result.attr("imdbRating")));
        rating.setMaxValue(10);
        md.addRating(rating);
      }
      catch (NumberFormatException e) {
        getLogger().trace("could not parse rating/vote count: {}", e.getMessage());
      }

      episodes.add(md);
    }

    return episodes;
  }
}
