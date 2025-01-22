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

package org.tinymediamanager.core.tvshow;

import java.util.Date;

import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;

/**
 * This enum represents the {@link org.tinymediamanager.core.tvshow.entities.TvShowEpisode} type
 *
 * @author Manuel Laggner, Totto16
 */
public enum TvShowEpisodeType {
  // existing episodes
  UNCATEGORIZED, // S/E = -1
  SPECIAL, // S = 0
  NORMAL, // S > 0, E >= 0

  // dummy episodes
  DUMMY_SPECIAL, // S = 0
  DUMMY_NORMAL, // S > 0, E >= 0
  DUMMY_NOT_AIRED; // aired date > now or aired date = null

  /**
   * get the {@link TvShowEpisodeType} for the given {@link TvShowEpisode}
   * 
   * @param episode
   *          the {@link TvShowEpisode} to get the {@link TvShowEpisodeType} for
   * @return the found {@link TvShowEpisodeType}
   */
  public static TvShowEpisodeType getTypeForEpisode(TvShowEpisode episode) {
    if (!episode.isDummy()) {
      // normal episode
      if (episode.getSeason() < 0 || episode.getEpisode() < 0) {
        return UNCATEGORIZED;
      }

      if (episode.getSeason() == 0) {
        return SPECIAL;
      }

      return NORMAL;

    }
    else {
      // dummy episode
      if (episode.getFirstAired() == null || episode.getFirstAired().compareTo(new Date()) > 0) {
        return DUMMY_NOT_AIRED;
      }

      if (episode.getSeason() == 0) {
        return DUMMY_SPECIAL;
      }

      return DUMMY_NORMAL;
    }
  }
}
