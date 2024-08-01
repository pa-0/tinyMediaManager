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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.SubtitleSearchAndScrapeOptions;
import org.tinymediamanager.scraper.SubtitleSearchResult;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieSubtitleProvider;
import org.tinymediamanager.scraper.subdl.model.SubdlModel;
import org.tinymediamanager.scraper.subdl.model.Subtitles;
import org.tinymediamanager.scraper.subdl.model.Type;
import org.tinymediamanager.scraper.util.MediaIdUtil;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * The class {@link SubdlMovieSubtitleProvider} offers access to the SubDL service for movies
 * 
 * @author Wolfgang Janes
 */
public class SubdlMovieSubtitleProvider extends SubdlSubtitleProvider implements IMovieSubtitleProvider {
  private static final String     BASE_URL_DL = "https://dl.subdl.com";
  private static final Logger     LOGGER      = LoggerFactory.getLogger(SubdlMovieSubtitleProvider.class);

  private final MediaProviderInfo providerInfo;

  public SubdlMovieSubtitleProvider() {
    providerInfo = createMediaProviderInfo();
  }

  private MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "Subdl", "<html><h3>Subdl.com</h3><br />A subtitle scraper for Subdl.com</html>",
        SubdlSubtitleProvider.class.getResource("/org/tinymediamanager/scraper/subdl.png"));
    info.getConfig().addText("secret_key", "");
    info.getConfig().load();
    return info;
  }

  @Override
  protected String getSubId() {
    return "movie_subtitle";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  @Override
  public boolean isActive() {
    return isFeatureEnabled() && StringUtils.isNotBlank(providerInfo.getConfig().getValue("secret_key"));
  }

  public List<SubtitleSearchResult> search(SubtitleSearchAndScrapeOptions options) throws ScrapeException {

    initAPI(providerInfo.getConfig().getValue("secret_key"));
    LOGGER.debug("search() {}", options);

    List<SubtitleSearchResult> results = new ArrayList<>();

    SubdlModel searchResult = null;

    if (MediaIdUtil.isValidImdbId(options.getImdbId())) {
      searchResult = getSubdlFromImdbID(options.getImdbId(), options.getLanguage().name());
    }

    if (searchResult == null && options.getTmdbId() > 0) {
      searchResult = getSubdlFromTmdbID(options.getTmdbId(), options.getLanguage().name());
    }

    if (searchResult == null) {
      searchResult = getSubdlFromQuery(options.getSearchQuery(), options.getLanguage().name());
    }

    if (searchResult == null) {
      throw new NothingFoundException();
    }

    for (Subtitles subtitles : searchResult.subtitles) {
      SubtitleSearchResult subtitleSearchResult = new SubtitleSearchResult(options.getImdbId());
      subtitleSearchResult.setReleaseName(subtitles.releaseName);
      subtitleSearchResult.setTitle(subtitles.name);
      subtitleSearchResult.setUrl(() -> BASE_URL_DL + subtitles.url);
      results.add(subtitleSearchResult);

    }

    return results;
  }

  private @Nullable SubdlModel getSubdlFromQuery(String query, String language) {
    try {
      return processResponse(controller.getResultsForQuery(query, language.toUpperCase(Locale.ROOT), Type.MOVIE));
    }
    catch (Exception e) {
      LOGGER.warn("could not get response from subdl - '{}'", e.getMessage());
    }

    return null;
  }

  private @Nullable SubdlModel getSubdlFromImdbID(String imdbID, String language) {
    try {
      return processResponse(controller.getResultsFromImdbId(imdbID, language.toUpperCase(Locale.ROOT), Type.MOVIE));
    }
    catch (Exception e) {
      LOGGER.warn("could not get response from subdl - '{}'", e.getMessage());
    }

    return null;
  }

  private @Nullable SubdlModel getSubdlFromTmdbID(int tmdbId, String language) {
    try {
      return processResponse(controller.getResultsFromTmdbId(tmdbId, language.toUpperCase(Locale.ROOT), Type.MOVIE));
    }
    catch (Exception e) {
      LOGGER.warn("could not get response from subdl - '{}'", e.getMessage());
    }

    return null;
  }

  private @Nullable SubdlModel processResponse(Response<SubdlModel> response) throws IOException {
    SubdlModel searchResult;

    if (!response.isSuccessful()) {
      String message = response.message();
      try (ResponseBody body = response.errorBody()) {
        message = body.string();
      }
      catch (Exception e) {
        // ignore
      }

      throw new HttpException(response.code(), message);
    }
    searchResult = response.body();
    return searchResult;
  }

}
