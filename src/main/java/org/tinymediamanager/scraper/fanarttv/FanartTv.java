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

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.scraper.fanarttv.services.MovieService;
import org.tinymediamanager.scraper.fanarttv.services.TvShowService;
import org.tinymediamanager.scraper.http.TmmHttpClient;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.internal.bind.DateTypeAdapter;

import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * The class FanartTv is the abstraction of the Fanart.tv API
 */
public class FanartTv {
  // the base API url
  public static final String API_URL          = "https://webservice.fanart.tv/v3/";
  // the api key query parameter; hast to be supplied at all calls
  public static final String PARAM_API_KEY    = "api-key";
  public static final String PARAM_CLIENT_KEY = "client-key";

  private Retrofit           restAdapter;
  private String             apiKey;
  private String             clientKey;

  /**
   * Creates a new instance of the API
   */
  public FanartTv() {
    this.apiKey = "";
    this.clientKey = "";
  }

  /**
   * set an API key
   *
   * @param apiKey
   *          the API key to be set
   */
  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  /**
   * get the current API key
   *
   * @return the current API key
   */
  public String getApiKey() {
    return this.apiKey;
  }

  /**
   * set the fanart.tv client key
   *
   * @param clientKey
   *          the client key
   */
  public void setClientKey(String clientKey) {
    this.clientKey = clientKey;
  }

  /**
   * get the current client key
   *
   * @return the current client key
   */
  public String getClientKey() {
    return this.clientKey;
  }

  /**
   * Create a new {@link Retrofit.Builder} instance. Override this to e.g. set your own client or executor.
   *
   * @return A {@link Retrofit.Builder} with no modifications.
   */
  protected Retrofit.Builder newRestAdapterBuilder() {
    return new Retrofit.Builder();
  }

  /**
   * Return the current {@link Retrofit} instance. If none exists (first call), builds a new one.
   */
  protected Retrofit getRestAdapter() {
    if (restAdapter == null) {
      Retrofit.Builder builder = newRestAdapterBuilder();
      builder.baseUrl(API_URL);
      builder.addConverterFactory(GsonConverterFactory.create(getGsonBuilder().create()));
      builder.client(TmmHttpClient.newBuilder(true).addInterceptor(chain -> {
        Request original = chain.request();
        Request.Builder request = original.newBuilder().method(original.method(), original.body());
        request.addHeader(PARAM_API_KEY, apiKey);
        if (StringUtils.isNotBlank(clientKey)) {
          request.addHeader(PARAM_CLIENT_KEY, clientKey);
        }
        return chain.proceed(request.build());
      }).build());

      restAdapter = builder.build();
    }
    return restAdapter;
  }

  protected GsonBuilder getGsonBuilder() {
    GsonBuilder builder = new GsonBuilder();
    // class types
    builder.registerTypeAdapter(Integer.class, (JsonDeserializer<Integer>) (json, typeOfT, context) -> {
      try {
        return json.getAsInt();
      }
      catch (NumberFormatException e) {
        return 0;
      }
    });
    builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
    builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
    return builder;
  }

  public MovieService getMovieService() {
    return getRestAdapter().create(MovieService.class);
  }

  public TvShowService getTvShowService() {
    return getRestAdapter().create(TvShowService.class);
  }
}
