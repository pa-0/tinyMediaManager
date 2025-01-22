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
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class SearchResultRecord {
  @SerializedName("aliases")
  public List<String>        aliases         = null;

  @SerializedName("companies")
  public List<String>        companies       = null;

  @SerializedName("companyType")
  public String              companyType     = null;

  @SerializedName("country")
  public String              country         = null;

  @SerializedName("director")
  public String              director        = null;

  @SerializedName("extendedTitle")
  public String              extendedTitle   = null;

  @SerializedName("genres")
  public List<String>        genres          = null;

  @SerializedName("id")
  public String              id              = null;

  @SerializedName("image_url")
  public String              imageUrl        = null;

  @SerializedName("name")
  public String              name            = null;

  @SerializedName("name_translated")
  public String              nameTranslated  = null;

  @SerializedName("network")
  public String              network         = null;

  @SerializedName("officialList")
  public String              officialList    = null;

  @SerializedName("overview")
  public String              overview        = null;

  @SerializedName("overviews")
  public Map<String, String> overviews       = null;

  @SerializedName("posters")
  public List<String>        posters         = null;

  @SerializedName("primaryLanguage")
  public String              primaryLanguage = null;

  @SerializedName("primaryType")
  public String              primaryType     = null;

  @SerializedName("status")
  public String              status          = null;

  @SerializedName("translations")
  public Map<String, String> translations    = null;

  @SerializedName("tvdb_id")
  public String              tvdbId          = null;

  @SerializedName("type")
  public String              type            = null;

  @SerializedName("year")
  public String              year            = null;

}
