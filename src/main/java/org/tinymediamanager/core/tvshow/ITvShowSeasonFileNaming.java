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

import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;

/**
 * the interface ITvShowFileNaming is used for generating file names for tv show related files
 */
public interface ITvShowSeasonFileNaming {
  /**
   * get the file name for this enum
   *
   * @param tvShowSeason
   *          the TV show season
   * @param extension
   *          the file extension
   * @param forRenamer
   *          do we want to create the filename for the renamer (aka. clean season folder)
   * @return the file name or an empty string
   */
  String getFilename(TvShowSeason tvShowSeason, String extension, boolean forRenamer);

  /**
   * get the file name for this enum - renamer version (creates a clean season folder according to settings)
   *
   * @param tvShowSeason
   *          the TV show season
   * @param extension
   *          the file extension
   * @return the file name or an empty string
   */
  default String getFilename(TvShowSeason tvShowSeason, String extension) {
    return getFilename(tvShowSeason, extension, true);
  }

  /**
   * get the desired season folder
   * 
   * @param tvShowSeason
   *          the {@link TvShowSeason} to get the folder for
   * @param forRenamer
   *          do we want to create the filename for the renamer (aka. clean season folder)
   * @return the season folder name
   */
  default String getSeasonFolder(TvShowSeason tvShowSeason, boolean forRenamer) {
    TvShow tvShow = tvShowSeason.getTvShow();

    if (forRenamer) {
      // create a clean folder name
      return TvShowRenamer.getSeasonFoldername(tvShow, tvShowSeason);
    }
    else {
      // search for the best-fitting existing folder
      return TvShowHelpers.detectSeasonFolder(tvShow, tvShowSeason.getSeason());
    }
  }

  /**
   * get the enum name
   * 
   * @return the enum name
   */
  String name();
}
