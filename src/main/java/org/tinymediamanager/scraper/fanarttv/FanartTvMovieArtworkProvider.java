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
package org.tinymediamanager.scraper.fanarttv;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.fanarttv.entities.Images;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.interfaces.IMovieSetArtworkProvider;
import org.tinymediamanager.scraper.util.MediaIdUtil;

import retrofit2.Response;

/**
 * the class {@link FanartTvMovieArtworkProvider} is used to fetch movie related artwork from fanart.tv
 *
 * @author Manuel Laggner
 */
public class FanartTvMovieArtworkProvider extends FanartTvMetadataProvider implements IMovieArtworkProvider, IMovieSetArtworkProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(FanartTvMovieArtworkProvider.class);

  @Override
  protected String getSubId() {
    return "movie_artwork";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  // http://webservice.fanart.tv/v3/movies/559
  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {

    if (options.getMediaType() != MediaType.MOVIE && options.getMediaType() != MediaType.MOVIE_SET) {
      return Collections.emptyList();
    }

    LOGGER.debug("getArtwork() - {}", options);

    // lazy initialization of the api
    initAPI();

    MediaArtwork.MediaArtworkType artworkType = options.getArtworkType();
    String language = null;
    if (options.getLanguage() != null) {
      language = options.getLanguage().getLanguage();
    }

    List<MediaArtwork> returnArtwork = new ArrayList<>();

    Response<Images> images = null;
    String imdbId = options.getImdbId();
    int tmdbId = options.getTmdbId();

    // for movie sets we need another if
    if (options.getMediaType() == MediaType.MOVIE_SET && options.getIdAsInt(MediaMetadata.TMDB_SET) > 0) {
      tmdbId = options.getIdAsInt(MediaMetadata.TMDB_SET);
    }

    if (tmdbId == 0 && !MediaIdUtil.isValidImdbId(imdbId)) {
      throw new MissingIdException(MediaMetadata.IMDB, MediaMetadata.TMDB);
    }

    Exception savedException = null;

    if (StringUtils.isNotBlank(imdbId)) {
      try {
        LOGGER.debug("getArtwork with IMDB id: {}", imdbId);
        images = api.getMovieService().getMovieImages(imdbId).execute();
      }
      catch (Exception e) {
        LOGGER.debug("failed to get artwork: {}", e.getMessage());
        savedException = e;
      }
    }

    if ((images == null || images.body() == null) && tmdbId != 0) {
      try {
        LOGGER.debug("getArtwork with TMDB id: {}", tmdbId);
        images = api.getMovieService().getMovieImages(Integer.toString(tmdbId)).execute();
      }
      catch (Exception e) {
        LOGGER.debug("failed to get artwork: {}", e.getMessage());
        savedException = e;
      }
    }

    // if there has been an exception and nothing has been found, throw this exception
    if ((images == null || !images.isSuccessful()) && savedException != null) {
      // if the thread has been interrupted, to no rethrow that exception
      if (savedException instanceof InterruptedException || savedException instanceof InterruptedIOException) {
        return returnArtwork;
      }
      throw new ScrapeException(savedException);
    }

    if (images == null) {
      LOGGER.info("got no result");
      return returnArtwork;
    }
    if (!images.isSuccessful()) {
      String message = "";
      try {
        message = images.errorBody().string();
      }
      catch (IOException e) {
        // ignore
      }
      LOGGER.warn("request was not successful: HTTP/{} - {}", images.code(), message);
      return returnArtwork;
    }

    returnArtwork = getArtwork(images.body(), artworkType);

    // buffer the artwork
    MediaMetadata md = options.getMetadata();
    if (md != null && !returnArtwork.isEmpty()) {
      md.addMediaArt(returnArtwork);
    }

    return returnArtwork;
  }
}
