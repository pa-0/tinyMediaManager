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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.util.MediaIdUtil;

/**
 * the class {@link TmdbTvShowArtworkProvider} is used to provide artwork for TV shows
 *
 * @author Manuel Laggner
 */
public class TmdbTvShowArtworkProvider extends TmdbMetadataProvider implements ITvShowArtworkProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TmdbTvShowArtworkProvider.class);

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = super.createMediaProviderInfo();

    info.getConfig().load();

    return info;
  }

  @Override
  protected String getSubId() {
    return "tvshow_artwork";
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

    if (options.getMediaType() != MediaType.TV_SHOW && options.getMediaType() != MediaType.TV_EPISODE) {
      return Collections.emptyList();
    }
    if (options.getMediaType() == MediaType.TV_EPISODE) {
      try {
        // episode artwork has to be scraped via the meta data scraper
        TvShowEpisodeSearchAndScrapeOptions episodeSearchAndScrapeOptions = new TvShowEpisodeSearchAndScrapeOptions();
        episodeSearchAndScrapeOptions.setDataFromOtherOptions(options);
        if (options.getIds().get(MediaMetadata.TVSHOW_IDS) instanceof Map) {
          Map<String, Object> tvShowIds = (Map<String, Object>) options.getIds().get(MediaMetadata.TVSHOW_IDS);
          episodeSearchAndScrapeOptions.setTvShowIds(tvShowIds);
        }
        MediaMetadata md = new TmdbTvShowMetadataProvider().getMetadata(episodeSearchAndScrapeOptions);
        return md.getMediaArt();
      }
      catch (MissingIdException e) {
        // no valid ID given - just do nothing
        return Collections.emptyList();
      }
      catch (Exception e) {
        throw new ScrapeException(e);
      }
    }

    // populate imdbid from tvdb if no tmdb/imdb id is set
    if (options.getTmdbId() == 0 && !MediaIdUtil.isValidImdbId(options.getImdbId()) && options.getIdAsInt(MediaMetadata.TVDB) > 0) {
      String imdbId = MediaIdUtil.getImdbIdFromTvdbId(options.getIdAsString(MediaMetadata.TVDB));
      if (MediaIdUtil.isValidImdbId(imdbId)) {
        options.setImdbId(imdbId);
      }
    }

    return new TmdbArtworkProvider(api, artworkBaseUrl).getArtwork(options);
  }
}
