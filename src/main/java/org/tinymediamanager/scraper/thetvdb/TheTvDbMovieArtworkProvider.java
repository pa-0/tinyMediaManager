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
package org.tinymediamanager.scraper.thetvdb;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.thetvdb.entities.ArtworkBaseRecord;
import org.tinymediamanager.scraper.thetvdb.entities.MovieExtendedResponse;

import retrofit2.Response;

/**
 * the class {@link TheTvDbMovieArtworkProvider} offer artwork for TV shows
 *
 * @author Manuel Laggner
 */
public class TheTvDbMovieArtworkProvider extends TheTvDbArtworkProvider implements IMovieArtworkProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TheTvDbMovieArtworkProvider.class);

  @Override
  protected String getSubId() {
    return "movie_artwork";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected List<ArtworkBaseRecord> fetchArtwork(int id) throws ScrapeException {
    List<ArtworkBaseRecord> images = new ArrayList<>();
    try {
      // get all types of artwork we can get
      Response<MovieExtendedResponse> response = tvdb.getMoviesService().getMovieExtended(id).execute();
      if (!response.isSuccessful()) {
        throw new HttpException(response.code(), response.message());
      }

      if (response.body() != null && response.body().data != null && response.body().data.artworks != null) {
        images.addAll(response.body().data.artworks);
      }
    }
    catch (Exception e) {
      LOGGER.error("failed to get artwork: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    return images;
  }
}
