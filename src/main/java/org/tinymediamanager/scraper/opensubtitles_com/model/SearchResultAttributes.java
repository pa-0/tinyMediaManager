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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchResultAttributes {
  @JsonProperty("subtitle_id")
  public String             subtitleId        = "";

  @JsonProperty("language")
  public String             language          = "";

  @JsonProperty("download_count")
  public int                downloadCount;

  @JsonProperty("new_download_count")
  public int                newDownloadCount;

  @JsonProperty("hearing_impaired")
  public boolean            hearingImpaired;

  @JsonProperty("hd")
  public boolean            hd;

  @JsonProperty("format")
  public String             format            = "";

  @JsonProperty("fps")
  public float              fps;

  @JsonProperty("votes")
  public int                votes;

  @JsonProperty("points")
  public int                points;

  @JsonProperty("ratings")
  public float              ratings;

  @JsonProperty("from_trusted")
  public boolean            fromTrusted;

  @JsonProperty("foreign_parts_only")
  public boolean            foreignPartsOnly;

  @JsonProperty("auto_translation")
  public boolean            autoTranslation;

  @JsonProperty("ai_translated")
  public boolean            aiTranslated;

  @JsonProperty("machine_translated")
  public String             machineTranslated = "";

  @JsonProperty("upload_date")
  public String             uploadDate        = "";

  @JsonProperty("release")
  public String             release           = "";

  @JsonProperty("comments")
  public String             comments          = "";

  @JsonProperty("legacy_subtitle_id")
  public int                legacySubtitleId;

  @JsonProperty("feature_details")
  public FeatureDetails     featureDetails;

  @JsonProperty("url")
  public String             url               = "";

  @JsonProperty("files")
  public List<SubtitleFile> files             = new ArrayList<>();
}
