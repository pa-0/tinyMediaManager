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
package org.tinymediamanager.scraper.opensubtitles_com.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FeatureDetails {
  @JsonProperty("feature_id")
  public int    featureId;

  @JsonProperty("feature_type")
  public String featureType = "";

  @JsonProperty("year")
  public int    year;

  @JsonProperty("title")
  public String title       = "";

  @JsonProperty("movie_name")
  public String movieName   = "";

  @JsonProperty("imdb_id")
  public int    imdbId;

  @JsonProperty("tmdb_id")
  public int    tmdbId;

  @JsonProperty("season_number")
  public int    seasonNumber;

  @JsonProperty("episode_number")
  public int    episodeNumber;

  @JsonProperty("parent_title")
  public String parentTitle = "";

  @JsonProperty("parent_imdb_id")
  public int    parentImdbId;

  @JsonProperty("parent_tmdb_id")
  public int    parentTmdbId;

  @JsonProperty("parent_feature_id")
  public int    parentFeatureId;
}
