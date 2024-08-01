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
package org.tinymediamanager.scraper.subdl.model;

import com.google.gson.annotations.SerializedName;

public class Subtitles {

  @SerializedName("release_name")
  public String  releaseName;

  @SerializedName("name")
  public String  name;

  @SerializedName("lang")
  public String  lang;

  @SerializedName("author")
  public String  author;

  @SerializedName("url")
  public String  url;

  @SerializedName("subtitlePage")
  public String  subtitlePage;

  @SerializedName("season")
  public int     season;

  @SerializedName("episode")
  public int     episode;

  @SerializedName("language")
  public String  language;

  @SerializedName("hi")
  public boolean hi;

  @SerializedName("episode_from")
  public int     episodeFrom;

  @SerializedName("episode_end")
  public int     episodeEnd;

  @SerializedName("full_season")
  public boolean fullSeason;

}
