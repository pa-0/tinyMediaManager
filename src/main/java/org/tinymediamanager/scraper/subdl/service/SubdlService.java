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
package org.tinymediamanager.scraper.subdl.service;

import org.tinymediamanager.scraper.subdl.model.SubdlModel;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SubdlService {

  /**
   * Fetch all results for a query with the video name
   * 
   * @param apiKey
   *          the API key
   * @param query
   *          the query
   * @param type
   *          the video type (`movie` or `tv`)
   * @param languages
   *          the languages to get the response for (separated by comma)
   * @return the response as {@link Call<SubdlModel>}
   */
  @GET("subtitles")
  Call<SubdlModel> fetchResults(@Query("api_key") String apiKey, @Query("film_name") String query, @Query("type") String type,
      @Query("languages") String languages);

  /**
   * Fetch all results for a query with the IMDB id
   * 
   * @param apiKey
   *          the API key
   * @param imdbId
   *          the IMDB id
   * @param type
   *          the video type (`movie` or `tv`)
   * @param languages
   *          the languages to get the response for (separated by comma)
   * @return the response as {@link Call<SubdlModel>}
   */
  @GET("subtitles")
  Call<SubdlModel> fetchResultswithImdbId(@Query("api_key") String apiKey, @Query("imdb_id") String imdbId, @Query("type") String type,
      @Query("languages") String languages);

  /**
   * Fetch all results for a query with the TMDB id
   * 
   * @param apiKey
   *          the API key
   * @param tmdbId
   *          the TMDB id
   * @param type
   *          the video type (`movie` or `tv`)
   * @param languages
   *          the languages to get the response for (separated by comma)
   * @return the response as {@link Call<SubdlModel>}
   */
  @GET("subtitles")
  Call<SubdlModel> fetchResultswithTmdbId(@Query("api_key") String apiKey, @Query("tmdb_id") int tmdbId, @Query("type") String type,
      @Query("languages") String languages);
}
