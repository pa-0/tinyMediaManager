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
package org.tinymediamanager.scraper.imdb;

import java.util.Collections;
import java.util.List;

import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;

/**
 * the class {@link ImdbMovieArtworkProvider} is used to provide artwork for movies
 *
 * @author Manuel Laggner
 */
public class ImdbMovieArtworkProvider extends ImdbMetadataProvider implements IMovieArtworkProvider {

  @Override
  protected String getSubId() {
    return "movie_artwork";
  }

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    if (options.getMediaType() != MediaType.MOVIE) {
      return Collections.emptyList();
    }

    return (new ImdbMovieParser(this, EXECUTOR)).getMovieArtwork(options);
  }
}
