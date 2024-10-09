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

import java.util.List;

import org.tinymediamanager.scraper.tvmaze.entities.AlternateList;
import org.tinymediamanager.scraper.tvmaze.entities.Episode;
import org.tinymediamanager.scraper.tvmaze.entities.SearchResult;
import org.tinymediamanager.scraper.tvmaze.entities.Show;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TvMazeService {

  @GET("/search/shows")
  Call<List<SearchResult>> showSearch(@Query("q") String query);

  @GET("/shows/{id}?embed[]=seasons&embed[]=crew&embed[]=cast&embed[]=images")
  Call<Show> show_main_information(@Path("id") int id);

  @GET("/shows/{id}/alternatelists")
  Call<List<AlternateList>> alternativeLists(@Path("id") int id);

  @GET("/alternatelists/{id}/alternateepisodes?embed=episodes")
  Call<List<Episode>> alternativeEpisodes(@Path("id") int id);

  @GET("/shows/{id}/episodes?specials=1")
  Call<List<Episode>> episodeList(@Path("id") int id);

  @GET("/seasons/{id}/episodes?embed=guestcast")
  Call<List<Episode>> seasonEpisodes(@Path("id") int id);
}
