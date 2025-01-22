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

public class SeriesBaseRecord {
  @SerializedName("abbreviation")
  public String       abbreviation         = null;

  @SerializedName("aliases")
  public List<Alias>  aliases              = null;

  @SerializedName("country")
  public String       country              = null;

  @SerializedName("defaultSeasonType")
  public Integer      defaultSeasonType    = null;

  @SerializedName("firstAired")
  public String       firstAired           = null;

  @SerializedName("id")
  public Integer      id                   = null;

  @SerializedName("image")
  public String       image                = null;

  @SerializedName("isOrderRandomized")
  public Boolean      isOrderRandomized    = null;

  @SerializedName("lastAired")
  public String       lastAired            = null;

  @SerializedName("name")
  public String       name                 = null;

  @SerializedName("nameTranslations")
  public List<String> nameTranslations     = null;

  @SerializedName("nextAired")
  public String       nextAired            = null;

  @SerializedName("originalCountry")
  public String       originalCountry      = null;

  @SerializedName("originalLanguage")
  public String       originalLanguage     = null;

  @SerializedName("overviewTranslations")
  public List<String> overviewTranslations = null;

  @SerializedName("score")
  public Double       score                = null;

  @SerializedName("slug")
  public String       slug                 = null;

  @SerializedName("status")
  public Status       status               = null;
}
