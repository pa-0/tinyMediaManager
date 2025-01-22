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

// https://api4.thetvdb.com/v4/seasons/types
public enum SeasonType {
  DEFAULT("default"),
  OFFICIAL("official"), // 1
  DVD("dvd"), // 2
  ABSOLUTE("absolute"), // 3
  ALTERNATE("alternate"), // 4
  REGIONAL("regional"), // 5
  ALT_DVD("altdvd"), // 6
  ALT_TWO("alttwo"); // 7

  private final String value;

  SeasonType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}
