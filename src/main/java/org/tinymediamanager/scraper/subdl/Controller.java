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
import java.util.Date;
import java.util.Locale;

import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.subdl.model.SubdlModel;
import org.tinymediamanager.scraper.subdl.model.Type;
import org.tinymediamanager.scraper.subdl.service.SubdlService;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.internal.bind.DateTypeAdapter;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

class Controller {

  private final Retrofit retrofit;
  private String         secretKey;

  Controller() {
    OkHttpClient.Builder builder = TmmHttpClient.newBuilder();
    builder.addInterceptor(chain -> {
      Request request = chain.request();
      return chain.proceed(request);
    });
    retrofit = buildRetrofitInstance(builder.build());

  }

  void setSecretKey(String apiKey) {
    this.secretKey = apiKey;
  }

  String getSecretKey() {
    return secretKey;
  }

  private GsonBuilder getGsonBuilder() {
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
    return builder;
  }

  private Retrofit buildRetrofitInstance(OkHttpClient client) {
    return new Retrofit.Builder().client(client)
        .baseUrl("https://api.subdl.com/api/v1/")
        .addConverterFactory(GsonConverterFactory.create(getGsonBuilder().create()))
        .build();
  }

  private SubdlService getService() {
    return retrofit.create(SubdlService.class);
  }

  /**
   * get the Subdl results for the given query
   * 
   * @param query
   *          the search query
   * @param language
   *          the language to get the result for (ISO 639-1, uppercase)
   * @param type
   *          the video {@link Type}
   * @return the {@link Response<SubdlModel>} for the request
   * @throws IOException
   *           any {@link IOException} occurred for the request
   */
  Response<SubdlModel> getResultsForQuery(String query, String language, Type type) throws IOException {
    return getService().fetchResults(getSecretKey(), query, type.name().toLowerCase(Locale.ROOT), language).execute();
  }

  /**
   * get the Subdl results for the given IMDB id
   * 
   * @param imdbId
   *          the IMDB id to search for
   * @param language
   *          the language to get the result for (ISO 639-1, uppercase)
   * @param type
   *          the video {@link Type}
   * @return the {@link Response<SubdlModel>} for the request
   * @throws IOException
   *           any {@link IOException} occurred for the request
   */
  Response<SubdlModel> getResultsFromImdbId(String imdbId, String language, Type type) throws IOException {
    return getService().fetchResultswithImdbId(getSecretKey(), imdbId, type.name().toLowerCase(Locale.ROOT), language)
        .execute();
  }

  /**
   * get the Subdl results for the given TMDB id
   * 
   * @param tmdbId
   *          the TMDB id to search for
   * @param language
   *          the language to get the result for (ISO 639-1, uppercase)
   * @param type
   *          the video {@link Type}
   * @return the {@link Response<SubdlModel>} for the request
   * @throws IOException
   *           any {@link IOException} occurred for the request
   */
  Response<SubdlModel> getResultsFromTmdbId(int tmdbId, String language, Type type) throws IOException {
    return getService().fetchResultswithTmdbId(getSecretKey(), tmdbId, type.name().toLowerCase(Locale.ROOT), language)
        .execute();
  }

}
