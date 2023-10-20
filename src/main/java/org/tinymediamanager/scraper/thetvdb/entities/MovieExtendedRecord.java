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
package org.tinymediamanager.scraper.thetvdb.entities;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class MovieExtendedRecord {
  @SerializedName("aliases")
  public List<Alias>                 aliases              = null;

  @SerializedName("artworks")
  public List<ArtworkExtendedRecord> artworks             = null;

  @SerializedName("audioLanguages")
  public List<String>                audioLanguages       = null;

  @SerializedName("awards")
  public List<AwardBaseRecord>       awards               = null;

  @SerializedName("boxOffice")
  public String                      boxOffice            = null;

  @SerializedName("boxOfficeUS")
  public String                      boxOfficeUS          = null;

  @SerializedName("budget")
  public String                      budget               = null;

  @SerializedName("characters")
  public List<Character>             characters           = null;

  @SerializedName("companies")
  public Companies                   companies            = null;

  @SerializedName("contentRatings")
  public List<ContentRating>         contentRatings       = null;

  @SerializedName("first_release")
  public Release                     firstRelease         = null;

  @SerializedName("genres")
  public List<GenreBaseRecord>       genres               = null;

  @SerializedName("id")
  public Long                        id                   = null;

  @SerializedName("image")
  public String                      image                = null;

  @SerializedName("inspirations")
  public List<Inspiration>           inspirations         = null;

  @SerializedName("lastUpdated")
  public String                      lastUpdated          = null;

  @SerializedName("lists")
  public List<ListBaseRecord>        lists                = null;

  @SerializedName("name")
  public String                      name                 = null;

  @SerializedName("nameTranslations")
  public List<String>                nameTranslations     = null;

  @SerializedName("originalCountry")
  public String                      originalCountry      = null;

  @SerializedName("originalLanguage")
  public String                      originalLanguage     = null;

  @SerializedName("overviewTranslations")
  public List<String>                overviewTranslations = null;

  @SerializedName("production_countries")
  public List<ProductionCountry>     productionCountries  = null;

  @SerializedName("releases")
  public List<Release>               releases             = null;

  @SerializedName("remoteIds")
  public List<RemoteID>              remoteIds            = null;

  @SerializedName("runtime")
  public Integer                     runtime              = null;

  @SerializedName("score")
  public Double                      score                = null;

  @SerializedName("slug")
  public String                      slug                 = null;

  @SerializedName("spoken_languages")
  public List<String>                spokenLanguages      = null;

  @SerializedName("status")
  public Status                      status               = null;

  @SerializedName("studios")
  public List<StudioBaseRecord>      studios              = null;

  @SerializedName("subtitleLanguages")
  public List<String>                subtitleLanguages    = null;

  @SerializedName("tagOptions")
  public List<TagOption>             tagOptions           = null;

  @SerializedName("trailers")
  public List<Trailer>               trailers             = null;

  @SerializedName("translations")
  public TranslationExtended         translations         = null;

  @SerializedName("year")
  public String                      year                 = null;
}
