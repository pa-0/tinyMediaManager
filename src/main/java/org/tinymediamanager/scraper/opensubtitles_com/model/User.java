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

public class User {
  @JsonProperty("user_id")
  public int     userId;

  @JsonProperty("allowed_downloads")
  public int     allowedDownloads;

  @JsonProperty("allowed_translations")
  public int     allowedTranslations;

  @JsonProperty("level")
  public String  level;

  @JsonProperty("ext_installed")
  public boolean extensionInstalled;

  @JsonProperty("vip")
  public boolean vip;
}
