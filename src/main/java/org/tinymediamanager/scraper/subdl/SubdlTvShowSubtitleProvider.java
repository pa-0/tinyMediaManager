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
package org.tinymediamanager.scraper.subdl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.SubtitleSearchAndScrapeOptions;
import org.tinymediamanager.scraper.SubtitleSearchResult;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowSubtitleProvider;
import org.tinymediamanager.scraper.subdl.model.SubdlModel;
import org.tinymediamanager.scraper.subdl.model.Subtitles;
import org.tinymediamanager.scraper.subdl.model.Type;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;

import static org.tinymediamanager.scraper.MediaMetadata.IMDB;

/**
 * The class {@link SubdlTvShowSubtitleProvider} offers access to the SubDL service for movies
 *
 * @author Wolfgang Janes
 */
public class SubdlTvShowSubtitleProvider extends SubdlSubtitleProvider implements ITvShowSubtitleProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubdlTvShowSubtitleProvider.class);

  @Override
  public List<SubtitleSearchResult> search(SubtitleSearchAndScrapeOptions options) throws ScrapeException {

    Map<String, Object> tvShowIds;
    initAPI(providerInfo.getConfig().getValue("secret_key"));
    LOGGER.debug("search() {}", options);

    List<SubtitleSearchResult> results = new ArrayList<>();

    SubdlModel searchResult = null;

    if (options.getIds().get("tvShowIds") instanceof Map) {
      tvShowIds = (Map<String, Object>) options.getIds().get("tvShowIds");
    }
    else {
      throw new NothingFoundException();
    }

    String tvShowImdbId = String.valueOf(tvShowIds.get(IMDB));
    if (MediaIdUtil.isValidImdbId(tvShowImdbId)) {
      searchResult = getSubdlFromImdbID(tvShowImdbId, options.getLanguage().name(), options.getSeason(), options.getEpisode());
    }

    int tvShowTmdbId = MetadataUtil.parseInt(String.valueOf(tvShowIds.get(MediaMetadata.TMDB)), 0);
    if ((searchResult == null || !searchResult.status) && tvShowTmdbId > 0) {
      searchResult = getSubdlFromTmdbID(tvShowTmdbId, options.getLanguage().name(), options.getSeason(), options.getEpisode());
    }

    if ((searchResult == null || !searchResult.status)) {
      searchResult = getSubdlFromQuery(options.getSearchQuery(), options.getLanguage().name());
    }

    if ((searchResult == null || !searchResult.status)) {
      return results;
    }

    for (Subtitles subtitles : searchResult.subtitles) {
      if (options.getSeason() == subtitles.season && options.getEpisode() == subtitles.episode) {
        SubtitleSearchResult subtitleSearchResult = new SubtitleSearchResult(options.getImdbId());
        subtitleSearchResult.setReleaseName(subtitles.releaseName);
        subtitleSearchResult.setTitle(subtitles.name);
        subtitleSearchResult.setUrl(() -> BASE_URL_DL + subtitles.url);
        results.add(subtitleSearchResult);
      }
    }

    return results;
  }

  private @Nullable SubdlModel getSubdlFromQuery(String query, String language) {
    try {
      return processResponse(controller.getResultsForQuery(query, language.toUpperCase(Locale.ROOT), Type.TV));
    }
    catch (Exception e) {
      LOGGER.warn("could not get response from subdl - '{}'", e.getMessage());
    }

    return null;
  }

  private @Nullable SubdlModel getSubdlFromImdbID(String imdbID, String language, int season, int episode) {
    try {
      return processResponse(controller.getResultsFromImdbId(imdbID, language.toUpperCase(Locale.ROOT), Type.TV, season, episode));
    }
    catch (Exception e) {
      LOGGER.warn("could not get response from subdl - '{}'", e.getMessage());
    }

    return null;
  }

  private @Nullable SubdlModel getSubdlFromTmdbID(int tmdbId, String language, int season, int episode) {
    try {
      return processResponse(controller.getResultsFromTmdbId(tmdbId, language.toUpperCase(Locale.ROOT), Type.TV, season, episode));
    }
    catch (Exception e) {
      LOGGER.warn("could not get response from subdl - '{}'", e.getMessage());
    }

    return null;
  }

  @Override
  protected String getSubId() {
    return "tvshow_subtitle";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public boolean isActive() {
    return isFeatureEnabled() && StringUtils.isNotBlank(providerInfo.getConfig().getValue("secret_key"));
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }
}
