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

package org.tinymediamanager.scraper.thetvdb.entities;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class EpisodeBaseRecord {
  @SerializedName("aired")
  public String                 aired                = null;

  @SerializedName("id")
  public Integer                id                   = null;

  @SerializedName("image")
  public String                 image                = null;

  @SerializedName("imageType")
  public Integer                imageType            = null;

  @SerializedName("isMovie")
  public Integer                isMovie              = null;

  @SerializedName("name")
  public String                 name                 = null;

  // used to inject translations - not offered in the API
  public String                 originalName         = null;

  @SerializedName("nameTranslations")
  public List<String>           nameTranslations     = null;

  @SerializedName("overview")
  public String                 overview             = null;

  @SerializedName("overviewTranslations")
  public List<String>           overviewTranslations = null;

  @SerializedName("runtime")
  public Integer                runtime              = null;

  @SerializedName("seasons")
  public List<SeasonBaseRecord> seasons              = null;

  @SerializedName("seriesId")
  public Integer                seriesId             = null;

  @SerializedName("number")
  public Integer                episodeNumber        = null;

  @SerializedName("seasonNumber")
  public Integer                seasonNumber         = null;
}
