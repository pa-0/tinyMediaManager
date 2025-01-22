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

package org.tinymediamanager.scraper.tmdb.services;

import org.tinymediamanager.scraper.tmdb.entities.TvEpisodeGroup;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TvEpisodeGroupsService {

  /**
   * Get episode group details
   *
   * @param episodeGroupId
   *          the episode group id
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/episode_group/{id}")
  Call<TvEpisodeGroup> episodeGroup(@Path("id") String episodeGroupId, @Query("language") String language);
}
