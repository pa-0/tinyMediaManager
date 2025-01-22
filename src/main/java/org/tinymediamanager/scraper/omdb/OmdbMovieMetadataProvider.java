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

import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IRatingProvider;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.UrlUtil;

/**
 * the class {@link OmdbMovieMetadataProvider} is used to provide meta data for movies
 *
 * @author Manuel Laggner
 */
public class OmdbMovieMetadataProvider extends OmdbMetadataProvider implements IMovieMetadataProvider, IMovieImdbMetadataProvider, IRatingProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OmdbMovieMetadataProvider.class);

  @Override
  protected String getSubId() {
    return "movie";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public MediaMetadata getMetadata(MovieSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): '{}'", options);

    if (options.getSearchResult() != null && options.getSearchResult().getMediaMetadata() != null
        && getId().equals(options.getSearchResult().getMediaMetadata().getProviderId())) {
      return options.getSearchResult().getMediaMetadata();
    }

    // get imdbid
    String imdbId = getImdbId(options);

    // imdbid check
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.warn("no imdb id found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    Document doc = null;
    try {
      doc = UrlUtil
          .parseDocumentFromUrl("https://www.omdbapi.com/?apikey=" + getApiKey() + "&i=" + imdbId + "&type=movie&plot=full&tomatoes=true&r=xml");
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

    MediaMetadata metadata = parseDetail(doc, "movie");
    if (metadata == null) {
      LOGGER.warn("no result found");
      throw new NothingFoundException();
    }

    metadata.setScrapeOptions(options);
    return metadata;
  }

  @Override
  public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions query) throws ScrapeException {
    LOGGER.debug("search(): '{}'", query);

    SortedSet<MediaSearchResult> mediaResult = new TreeSet<>();

    // if the imdb id is given, directly fetch the result
    if (MediaIdUtil.isValidImdbId(query.getImdbId())) {
      try {
        MediaMetadata md = getMetadata(query);

        // create the search result from the metadata
        MediaSearchResult result = new MediaSearchResult(getId(), MediaType.MOVIE);
        result.setMetadata(md);
        result.setTitle(md.getTitle());
        result.setIMDBId(query.getImdbId());
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
      doc = UrlUtil.parseDocumentFromUrl(
          "https://www.omdbapi.com/?apikey=" + getApiKey() + "&s=" + UrlUtil.encode(query.getSearchQuery()) + "&type=movie&page=1&r=xml");
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

    List<MediaSearchResult> searchResults = parseSearchResults(doc, MediaType.MOVIE);

    // nothing found? try a direct lookup
    if (searchResults.isEmpty()) {
      try {
        doc = UrlUtil.parseDocumentFromUrl(
            "https://www.omdbapi.com/?apikey=" + getApiKey() + "&t=" + UrlUtil.encode(query.getSearchQuery()) + "&type=movie&r=xml");
        if (doc != null && doc.childrenSize() != 0) {
          MediaMetadata md = parseDetail(doc, "movie");
          if (md != null) {
            md.setScrapeOptions(query);
            MediaSearchResult searchResult = new MediaSearchResult(getId(), MediaType.MOVIE);
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
      searchResult.calculateScore(query);
      mediaResult.add(searchResult);
    }

    return mediaResult;
  }

  @Override
  public List<MediaRating> getRatings(Map<String, Object> ids, MediaType mediaType) throws ScrapeException {
    if (mediaType != MediaType.MOVIE) {
      return Collections.emptyList();
    }
    LOGGER.debug("getRatings(): {}", ids);

    MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
    options.setIds(ids);

    try {
      return getMetadata(options).getRatings();
    }
    catch (Exception e) {
      return Collections.emptyList();
    }
  }
}
