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

package org.tinymediamanager.scraper.thetvdb.service;

import org.tinymediamanager.scraper.thetvdb.entities.SearchResultResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SearchType;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SearchService {
  /**
   * Returns a search response
   * 
   * @param query
   *          the search query
   * @param searchType
   *          the type to search for
   * @param year
   *          the year
   * @return Call&lt;SearchResultResponse&gt;
   */
  @GET("search")
  Call<SearchResultResponse> getSearch(@Query("query") String query, @Query("type") SearchType searchType, @Query("year") Integer year);

  /**
   * Returns a search response
   * 
   * @param query
   *          the search query
   * @param searchType
   *          the type to search for
   * @return Call&lt;SearchResultResponse&gt;
   */
  @GET("search")
  Call<SearchResultResponse> getSearch(@Query("query") String query, @Query("type") SearchType searchType);

  /**
   * Search for a remote id (IMDB)
   * 
   * @param remoteId
   *          the remote id (IMDB)
   * @return Call&lt;SearchResultResponse&gt;
   */
  @GET("search")
  Call<SearchResultResponse> getSearch(@Query("query") String query, @Query("remote_id") String remoteId);
}
