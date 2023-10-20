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

public class Companies {
  @SerializedName("studio")
  public List<Company> studio         = null;

  @SerializedName("network")
  public List<Company> network        = null;

  @SerializedName("production")
  public List<Company> production     = null;

  @SerializedName("distributor")
  public List<Company> distributor    = null;

  @SerializedName("special_effects")
  public List<Company> specialEffects = null;
}
