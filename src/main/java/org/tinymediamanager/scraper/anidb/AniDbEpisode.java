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
package org.tinymediamanager.scraper.anidb;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * helper class for episode extraction
 *
 * @author Manuel Laggner
 */
class AniDbEpisode {
  int                 id;
  int                 episode;
  int                 season;
  int                 runtime;
  Date                airdate;
  float               rating;
  int                 votes;
  String              summary;
  Map<String, String> titles;

  private AniDbEpisode(Builder builder) {
    id = builder.id;
    episode = builder.episode;
    season = builder.season;
    runtime = builder.runtime;
    airdate = builder.airdate;
    rating = builder.rating;
    votes = builder.votes;
    summary = builder.summary;
    titles = builder.titles;
  }

  static final class Builder {
    private int                 id      = -1;
    private int                 episode = -1;
    private int                 season  = -1;
    private int                 runtime = 0;
    private Date                airdate = null;
    private float               rating  = 0;
    private int                 votes   = 0;
    private String              summary = "";
    private Map<String, String> titles  = new HashMap<>();

    @Nonnull
    public Builder id(int val) {
      id = val;
      return this;
    }

    @Nonnull
    public Builder episode(int val) {
      episode = val;
      return this;
    }

    @Nonnull
    public Builder season(int val) {
      season = val;
      return this;
    }

    @Nonnull
    public Builder runtime(int val) {
      runtime = val;
      return this;
    }

    @Nonnull
    public Builder airdate(@Nonnull Date val) {
      airdate = val;
      return this;
    }

    @Nonnull
    public Builder rating(float val) {
      rating = val;
      return this;
    }

    @Nonnull
    public Builder votes(int val) {
      votes = val;
      return this;
    }

    @Nonnull
    public Builder summary(@Nonnull String val) {
      summary = val;
      return this;
    }

    @Nonnull
    public Builder titles(@Nonnull Map<String, String> val) {
      titles = val;
      return this;
    }

    @Nonnull
    public Builder titles(@Nonnull String language, @Nonnull String title) {
      titles.put(language, title);
      return this;
    }

    @Nonnull
    public AniDbEpisode build() {
      return new AniDbEpisode(this);
    }
  }
}
