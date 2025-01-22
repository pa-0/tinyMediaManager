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
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;

import retrofit2.Response;

/**
 * the class {@link FanartTvTvShowArtworkProvider} is used to fetch TV show related artwork from fanart.tv
 *
 * @author Manuel Laggner
 */
public class FanartTvTvShowArtworkProvider extends FanartTvMetadataProvider implements ITvShowArtworkProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(FanartTvTvShowArtworkProvider.class);

  @Override
  protected String getSubId() {
    return "tvshow_artwork";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  // http://webservice.fanart.tv/v3/tv/79349

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {

    if (options.getMediaType() != MediaType.TV_SHOW) {
      return Collections.emptyList();
    }

    LOGGER.debug("getArtwork() - {}", options);

    // lazy initialization of the api
    initAPI();

    MediaArtwork.MediaArtworkType artworkType = options.getArtworkType();
    String language = null; // FIXME: where used?
    if (options.getLanguage() != null) {
      language = options.getLanguage().getLanguage();
      if (options.getLanguage().toLocale() != null && StringUtils.isNotBlank(options.getLanguage().toLocale().getCountry())) {
        language += "-" + options.getLanguage().toLocale().getCountry();
      }
    }

    List<MediaArtwork> returnArtwork = new ArrayList<>();

    int tvdbId = options.getIdAsInt(MediaMetadata.TVDB);

    // no ID found? try the old one
    if (tvdbId == 0) {
      tvdbId = options.getIdAsInt("tvdb"); // FIXME: same as above?
    }

    if (tvdbId == 0) {
      throw new MissingIdException(MediaMetadata.TVDB);
    }

    Response<Images> images = null;
    try {
      LOGGER.debug("getArtwork with TVDB id: {}", tvdbId);
      images = api.getTvShowService().getTvShowImages(tvdbId).execute();
    }
    catch (Exception e) {
      LOGGER.debug("failed to get artwork: {}", e.getMessage());
      // if the thread has been interrupted, to no rethrow that exception
      if (e instanceof InterruptedException || e instanceof InterruptedIOException) {
        return returnArtwork;
      }

      throw new ScrapeException(e);
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
