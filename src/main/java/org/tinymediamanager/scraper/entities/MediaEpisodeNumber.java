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

package org.tinymediamanager.scraper.entities;

import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * the record {@link MediaEpisodeNumber} is used to hold a S/E combination for the given {@link MediaEpisodeGroup.EpisodeGroupType}
 * 
 * @param episodeGroup
 *          the {@link MediaEpisodeGroup}
 * @param season
 *          the season number
 * @param episode
 *          the episode number
 */
public record MediaEpisodeNumber(@JsonProperty @NotNull MediaEpisodeGroup episodeGroup, @JsonProperty int season, @JsonProperty int episode)
    implements Comparable<MediaEpisodeNumber> {

  /**
   * checks if that season and episode number is greater than -1
   * 
   * @return true/false
   */
  public boolean isValid() {
    return season > -1 && episode > -1;
  }

  /**
   * checks if at least season OR episode is greater than -1
   * 
   * @return true/false
   */
  public boolean containsAnyNumber() {
    return season > -1 || episode > -1;
  }

  @Override
  public int compareTo(@NotNull MediaEpisodeNumber o) {
    int result = Integer.compare(season, o.season);
    if (result == 0) {
      result = Integer.compare(episode, o.episode);
    }
    return result;
  }
}
