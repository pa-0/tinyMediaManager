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
package org.tinymediamanager.scraper.tvmaze.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.tvmaze.entities.Episode;
import org.tinymediamanager.scraper.tvmaze.entities.SearchResult;
import org.tinymediamanager.scraper.tvmaze.entities.Show;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.internal.bind.DateTypeAdapter;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Controller {

  Retrofit       retrofit;
  private String apiKey;

  /**
   * setting up the retrofit object with further debugging options if needed
   *
   * @param debug
   *          true or false
   */
  public Controller(String apiKey) {
    this.apiKey = apiKey;
    OkHttpClient.Builder builder = TmmHttpClient.newBuilderWithForcedCache(15, TimeUnit.MINUTES);
    retrofit = buildRetrofitInstance(builder.build());
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

  /**
   * Builder Class for retrofit Object
   *
   * @param client
   *          the http client
   * @return a new retrofit object.
   */
  private Retrofit buildRetrofitInstance(OkHttpClient client) {
    return new Retrofit.Builder().client(client).baseUrl(apiKey).addConverterFactory(GsonConverterFactory.create(getGsonBuilder().create())).build();
  }

  /**
   * Returns the created Retrofit Service
   *
   * @return retrofit object
   */
  private TvMazeService getService() {
    return retrofit.create(TvMazeService.class);
  }

  public List<SearchResult> getTvShowSearchResults(String query) throws IOException {
    return getService().showSearch(query).execute().body();
  }

  public Show getMainInformation(int showId) throws IOException {
    return getService().show_main_information(showId).execute().body();
  }

  public Show getAlternativeLists(int showId) throws IOException {
    return getService().alternativeLists(showId).execute().body();
  }

  public List<Episode> getEpisodes(int showId) throws IOException {
    return getService().episodeList(showId).execute().body();
  }

  public List<Episode> getSeasonEpisodes(int seasonId) throws IOException {
    return getService().seasonEpisodes(seasonId).execute().body();
  }
}
