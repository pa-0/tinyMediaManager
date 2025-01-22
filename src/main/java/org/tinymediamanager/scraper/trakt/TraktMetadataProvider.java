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
package org.tinymediamanager.scraper.trakt;

import static org.tinymediamanager.scraper.MediaMetadata.TMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TVDB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.util.MediaIdUtil;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.TraktV2Interceptor;
import com.uwetrottmann.trakt5.entities.SearchResult;
import com.uwetrottmann.trakt5.entities.TraktError;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.IdType;
import com.uwetrottmann.trakt5.enums.Type;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;

abstract class TraktMetadataProvider implements IMediaProvider {
  static final String             ID = "trakt";

  private final MediaProviderInfo providerInfo;

  protected TraktV2               api;

  TraktMetadataProvider() {
    providerInfo = createMediaProviderInfo();
  }

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "Trakt.tv",
        "<html><h3>Trakt.tv</h3><br />Trakt.tv is a platform that does many things, but primarily keeps track of TV shows and movies you watch. It also provides meta data for movies and TV shows<br /><br />Available languages: EN</html>",
        TraktMetadataProvider.class.getResource("/org/tinymediamanager/scraper/trakt_tv.svg"), 10);

    info.getConfig().addText("clientId", "", true);
    info.getConfig().load();

    return info;
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(providerInfo.getUserApiKey());
  }

  protected abstract Logger getLogger();

  // thread safe initialization of the API
  protected synchronized void initAPI() throws ScrapeException {
    // create a new instance of the trakt api
    if (api == null) {
      if (!isActive()) {
        throw new ScrapeException(new FeatureNotEnabledException(this));
      }

      try {
        api = new TraktV2(getApiKey()) {
          // tell the trakt api to use our OkHttp client

          @Override
          protected synchronized OkHttpClient okHttpClient() {
            OkHttpClient.Builder builder = TmmHttpClient.newBuilderWithForcedCache(15, TimeUnit.MINUTES);
            builder.addInterceptor(new TraktV2Interceptor(this));
            return builder.build();
          }
        };
      }
      catch (Exception e) {
        getLogger().error("could not initialize the API: {}", e.getMessage());
        // force re-initialization the next time this will be called
        api = null;
        throw new ScrapeException(e);
      }
    }

    String userApiKey = providerInfo.getUserApiKey();

    // check if the API should change from current key to user key
    if (StringUtils.isNotBlank(userApiKey)) {
      api.apiKey(userApiKey);
    }

    // check if the API should change from current key to tmm key
    if (StringUtils.isBlank(userApiKey)) {
      api.apiKey(getApiKey());
    }
  }

  protected <T> T executeCall(Call<T> call) throws IOException {
    Response<T> response = call.execute();
    if (!response.isSuccessful() && response.code() == 401) {
      api.refreshToken();
      response = call.execute(); // retry
    }
    if (!response.isSuccessful()) {
      String message = "Request failed: " + response.code() + " " + response.message();
      TraktError error = api.checkForTraktError(response);
      if (error != null && error.message != null) {
        message += " message: " + error.message;
      }
      throw new HttpException(response.code(), message);
    }

    T body = response.body();
    if (body != null) {
      return body;
    }
    else {
      throw new IOException("Body should not be null for successful response");
    }
  }

  /**
   * Looks up a Trakt entity and provides a list of search results
   *
   * @param options
   *          the options to lookup with the id
   * @return any media search result
   */
  public List<MediaSearchResult> lookupWithId(MediaSearchAndScrapeOptions options) throws ScrapeException {
    List<SearchResult> results = new ArrayList<>();

    // get known IDs
    String imdbId = options.getImdbId().isEmpty() ? null : options.getImdbId();
    if (MediaIdUtil.isValidImdbId(options.getSearchQuery())) {
      imdbId = options.getSearchQuery();
    }
    String traktId = options.getIdAsString(providerInfo.getId());
    String tmdbId = options.getIdAsString(TMDB);
    String tvdbId = options.getIdAsString(TVDB);
    String tvrageId = options.getIdAsString(MediaMetadata.TVRAGE);

    // derive trakt type from ours
    Type type = null;
    switch (options.getMediaType()) {
      case MOVIE:
        type = Type.MOVIE;
        break;

      case TV_SHOW:
        type = Type.SHOW;
        break;

      case TV_EPISODE:
        type = Type.EPISODE;
        break;

      default:
    }

    // lookup until one has been found
    results = lookupWithId(results, IdType.TRAKT, traktId, type);
    results = lookupWithId(results, IdType.IMDB, imdbId, type);
    results = lookupWithId(results, IdType.TMDB, tmdbId, type);
    results = lookupWithId(results, IdType.TVDB, tvdbId, type);
    results = lookupWithId(results, IdType.TVRAGE, tvrageId, type);

    List<MediaSearchResult> msr = new ArrayList<>();
    for (SearchResult sr : results) {
      MediaSearchResult m = TraktUtils.morphTraktResultToTmmResult(options, sr);
      m.setScore(1.0f); // ID lookup
      msr.add(m);
    }
    return msr;
  }

  private List<SearchResult> lookupWithId(List<SearchResult> results, IdType id, String value, Type type) throws ScrapeException {
    // lazy initialization of the api
    initAPI();

    if (results.isEmpty() && value != null) {
      getLogger().debug("found {} id {} - direct lookup", id, value);
      try {
        Response<List<SearchResult>> response = api.search().idLookup(id, value, type, Extended.FULL, 1, 25).execute();
        if (!response.isSuccessful()) {
          getLogger().warn("request was NOT successful: HTTP/{} - {}", response.code(), response.message());
          return results;
        }
        results = response.body();
        getLogger().debug("Found {} result with ID lookup", results.size());
      }
      catch (Exception e) {
        getLogger().warn("request was NOT successful: {}", e.getMessage());
      }
    }
    return results;
  }

}
