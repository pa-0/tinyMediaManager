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

package org.tinymediamanager.core.tvshow.connector;

import java.nio.file.Path;

/**
 * The Enum TvShowConnectors.
 *
 * @author Manuel Laggner
 */
public enum TvShowConnectors {
  KODI("Kodi"),
  XBMC("Kodi / XBMC < v16"),
  EMBY("Emby"),
  JELLYFIN("Jellyfin"),
  PLEX("Plex"),
  MEDIAPORTAL("MediaPortal");

  private final String title;

  TvShowConnectors(String title) {
    this.title = title;
  }

  @Override
  public String toString() {
    return this.title;
  }

  /**
   * checks, if current NFO file is a valid TV show NFO<br>
   * (by casting to all known NFO formats)
   *
   * @param nfo
   *          the path to the NFO
   * @return true/false
   */
  public static boolean isValidTvShowNFO(Path nfo) {
    try {
      TvShowNfoParser tvShowNfoParser = TvShowNfoParser.parseNfo(nfo);
      return tvShowNfoParser.isValidNfo();
    }
    catch (Exception e) {
      return false;
    }
  }

  /**
   * checks, if current NFO file is a valid season NFO<br>
   * (by casting to all known NFO formats)
   *
   * @param nfo
   *          the path to the NFO
   * @return true/false
   */
  public static boolean isValidTvShowSeasonNFO(Path nfo) {
    try {
      TvShowSeasonNfoParser tvShowNfoParser = TvShowSeasonNfoParser.parseNfo(nfo);
      return tvShowNfoParser.isValidNfo();
    }
    catch (Exception e) {
      return false;
    }
  }

  /**
   * checks, if current NFO file is a valid episode NFO<br>
   * (by casting to all known NFO formats)
   *
   * @param nfo
   *          the path to the NFO
   * @return true/false
   */
  public static boolean isValidTvShowEpisodeNFO(Path nfo) {
    try {
      TvShowEpisodeNfoParser tvShowNfoParser = TvShowEpisodeNfoParser.parseNfo(nfo);
      return tvShowNfoParser.isValidNfo();
    }
    catch (Exception e) {
      return false;
    }
  }
}
