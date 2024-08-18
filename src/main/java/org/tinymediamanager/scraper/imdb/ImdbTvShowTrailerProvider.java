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

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.TrailerSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowTrailerProvider;
import org.tinymediamanager.scraper.util.MediaIdUtil;

/**
 * the class {@link ImdbTvShowTrailerProvider} is used to provide artwork for movies
 *
 * @author Manuel Laggner
 */
public class ImdbTvShowTrailerProvider extends ImdbMetadataProvider implements ITvShowTrailerProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImdbTvShowTrailerProvider.class);

  @Override
  protected String getSubId() {
    return "tvshow_trailer";
  }

  @Override
  public List<MediaTrailer> getTrailers(TrailerSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getTrailers(): {}", options);

    if (options.getMediaType() != MediaType.TV_SHOW && options.getMediaType() != MediaType.TV_EPISODE) {
      return Collections.emptyList();
    }

    String imdbId = "";
    // imdbid from scraper option
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      imdbId = options.getImdbId();
    }

    // imdbid via tmdbid
    if (!MediaIdUtil.isValidImdbId(imdbId) && options.getTmdbId() > 0) {
      imdbId = MediaIdUtil.getTvShowImdbIdViaTmdbId(options.getTmdbId());
    }

    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.warn("not possible to scrape from IMDB - no imdbId found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    LOGGER.debug("getTrailers(): {}", options);
    MovieSearchAndScrapeOptions mso = new MovieSearchAndScrapeOptions();
    mso.setImdbId(options.getImdbId()); // get metadata solely by imdbid
    MediaMetadata md = (new ImdbTvShowParser(this, EXECUTOR)).getMetadata(mso);
    return md.getTrailers();
  }

  /**
   * Freshly fetched url or empty string
   * 
   * @param trailer
   * @return
   */
  public String getUrlForId(MediaTrailer trailer) {
    try {
      return new ImdbTvShowParser(this, EXECUTOR).getFreshUrlForTrailer(trailer,
          TvShowModuleManager.getInstance().getSettings().getScraperLanguage().getLanguage(),
          TvShowModuleManager.getInstance().getSettings().getCertificationCountry().getAlpha2());
    }
    catch (Exception e) {
      LOGGER.warn("Could not fetch video: {}", e.getMessage());
    }
    return "";
  }
}
