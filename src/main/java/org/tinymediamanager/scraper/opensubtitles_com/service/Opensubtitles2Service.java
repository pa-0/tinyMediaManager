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

package org.tinymediamanager.scraper.opensubtitles_com.service;

import java.util.Map;

import org.tinymediamanager.scraper.opensubtitles_com.model.DownloadRequest;
import org.tinymediamanager.scraper.opensubtitles_com.model.DownloadResponse;
import org.tinymediamanager.scraper.opensubtitles_com.model.LoginRequest;
import org.tinymediamanager.scraper.opensubtitles_com.model.LoginResponse;
import org.tinymediamanager.scraper.opensubtitles_com.model.SearchResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

public interface Opensubtitles2Service {

  /**
   * from the opensubtitles.com API docs: <br/>
   * send GET parameters alphabetically sorted <br/>
   * send GET parameters and values in lowercase <br/>
   * in GET values remove "tt" from IMDB ID & remove leading 0 in any "ID" parameters, <br/>
   * there is no need to send default values, they will be deleted <br/>
   * use "+" instead "%20" for space in URL encoding
   */

  @POST("/api/v1/login")
  Call<LoginResponse> login(@Body LoginRequest loginRequest);

  /**
   * https://opensubtitles.stoplight.io/docs/opensubtitles-api/b3A6Mjc1MTk3MjY-search-for-subtitles
   * 
   * @param query
   *          a {@link Map} containing all needed query parameters
   * @return the search response
   */
  @GET("/api/v1/subtitles")
  Call<SearchResponse> search(@QueryMap Map<String, String> query);

  /**
   * prepare the download
   * 
   * @param body
   *          the {@link DownloadRequest} body
   * @return the response
   */
  @Headers({ "Content-Type: application/json", "Accept: */*" })
  @POST("/api/v1/download")
  Call<DownloadResponse> download(@Body DownloadRequest body);
}
