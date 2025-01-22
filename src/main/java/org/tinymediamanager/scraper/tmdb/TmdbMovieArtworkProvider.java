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
package org.tinymediamanager.scraper.tmdb;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.interfaces.IMovieSetArtworkProvider;

/**
 * the class {@link TmdbMovieArtworkProvider} is used to provide artwork for movies
 *
 * @author Manuel Laggner
 */
public class TmdbMovieArtworkProvider extends TmdbMetadataProvider implements IMovieArtworkProvider, IMovieSetArtworkProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TmdbMovieArtworkProvider.class);

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = super.createMediaProviderInfo();

    info.getConfig().load();

    return info;
  }

  @Override
  protected String getSubId() {
    return "movie_artwork";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getArtwork(): {}", options);
    // lazy initialization of the api
    initAPI();

    if (options.getMediaType() != MediaType.MOVIE_SET && options.getMediaType() != MediaType.MOVIE) {
      return Collections.emptyList();
    }

    return new TmdbArtworkProvider(api, artworkBaseUrl).getArtwork(options);
  }
}
