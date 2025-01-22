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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.thetvdb.entities.ArtworkBaseRecord;
import org.tinymediamanager.scraper.thetvdb.entities.SeasonBaseRecord;
import org.tinymediamanager.scraper.thetvdb.entities.SeriesExtendedResponse;
import org.tinymediamanager.scraper.util.ListUtils;

import retrofit2.Response;

/**
 * the class {@link TheTvDbTvShowArtworkProvider} offer artwork for TV shows
 *
 * @author Manuel Laggner
 */
public class TheTvDbTvShowArtworkProvider extends TheTvDbArtworkProvider implements ITvShowArtworkProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TheTvDbTvShowArtworkProvider.class);

  @Override
  protected String getSubId() {
    return "tvshow_artwork";
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
      Response<SeriesExtendedResponse> response = tvdb.getSeriesService().getSeriesExtended(id).execute();
      if (!response.isSuccessful()) {
        throw new HttpException(response.code(), response.message());
      }

      if (response.body() != null && response.body().data != null) {
        for (ArtworkBaseRecord image : ListUtils.nullSafe(response.body().data.artworks)) {
          // mix in the season number for season artwork
          if (image.season != null) {
            try {
              SeasonBaseRecord season = response.body().data.seasons.stream()
                  .filter(seasonBaseRecord -> seasonBaseRecord.id.equals(image.season))
                  .findFirst()
                  .orElse(null);
              if (season != null) {
                image.season = season.number;
              }
              else {
                image.season = null;
              }
            }
            catch (Exception e) {
              // just do not crash
              image.season = null;
            }
          }

          images.add(image);
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("failed to get artwork: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    return images;
  }

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {
    // lazy initialization of the api
    initAPI();

    if (options.getMediaType() == MediaType.TV_EPISODE) {
      LOGGER.debug("getting artwork: {}", options);
      try {
        // episode artwork has to be scraped via the meta data scraper
        TvShowEpisodeSearchAndScrapeOptions episodeSearchAndScrapeOptions = new TvShowEpisodeSearchAndScrapeOptions();
        episodeSearchAndScrapeOptions.setDataFromOtherOptions(options);
        if (options.getIds().get(MediaMetadata.TVSHOW_IDS) instanceof Map) {
          Map<String, Object> tvShowIds = (Map<String, Object>) options.getIds().get(MediaMetadata.TVSHOW_IDS);
          episodeSearchAndScrapeOptions.setTvShowIds(tvShowIds);
        }
        MediaMetadata md = new TheTvDbTvShowMetadataProvider().getMetadata(episodeSearchAndScrapeOptions);
        return md.getMediaArt();
      }
      catch (MissingIdException e) {
        // no valid ID given - just do nothing
        return Collections.emptyList();
      }
      catch (NothingFoundException e) {
        throw e;
      }
      catch (Exception e) {
        throw new ScrapeException(e);
      }
    }

    // return TV show artwork
    return super.getArtwork(options);
  }
}
