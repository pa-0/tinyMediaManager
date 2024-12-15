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

package org.tinymediamanager.core.tvshow.filenaming;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.tvshow.ITvShowSeasonFileNaming;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;

/**
 * The Enum TvShowSeasonThumbNaming.
 * 
 * @author Manuel Laggner
 */
public enum TvShowSeasonThumbNaming implements ITvShowSeasonFileNaming {
  /** seasonXX-thumb.* */
  SEASON_THUMB {
    @Override
    public String getFilename(TvShowSeason tvShowSeason, String extension, boolean forRenamer) {
      String filename;

      if (tvShowSeason.getSeason() == -1) {
        filename = "season-all-thumb." + extension;
      }
      else if (tvShowSeason.getSeason() == 0 && TvShowModuleManager.getInstance().getSettings().isSpecialSeason()) {
        filename = "season-specials-thumb." + extension;
      }
      else if (tvShowSeason.getSeason() > -1) {
        filename = String.format("season%02d-thumb.%s", tvShowSeason.getSeason(), extension);
      }
      else {
        filename = "";
      }

      return filename;
    }
  },

  /** seasonXX-landscape.* */
  SEASON_LANDSCAPE {
    @Override
    public String getFilename(TvShowSeason tvShowSeason, String extension, boolean forRenamer) {
      String filename;

      if (tvShowSeason.getSeason() == -1) {
        filename = "season-all-landscape." + extension;
      }
      else if (tvShowSeason.getSeason() == 0 && TvShowModuleManager.getInstance().getSettings().isSpecialSeason()) {
        filename = "season-specials-landscape." + extension;
      }
      else if (tvShowSeason.getSeason() > -1) {
        filename = String.format("season%02d-landscape.%s", tvShowSeason.getSeason(), extension);
      }
      else {
        filename = "";
      }

      return filename;
    }
  },

  /** season_folder/seasonXX-thumb.* */
  SEASON_FOLDER {
    @Override
    public String getFilename(TvShowSeason tvShowSeason, String extension, boolean forRenamer) {
      TvShow tvShow = tvShowSeason.getTvShow();
      if (tvShow == null) {
        return "";
      }

      String seasonFoldername = getSeasonFolder(tvShowSeason, forRenamer);

      // check whether the season folder name exists or not; do not create it just for the artwork!
      if (StringUtils.isBlank(seasonFoldername)) {
        // no season folder name in the templates found - fall back to the show base filename style
        return SEASON_THUMB.getFilename(tvShowSeason, extension, forRenamer);
      }

      String filename = String.format("season%02d-thumb.%s", tvShowSeason.getSeason(), extension);
      if (tvShowSeason.getSeason() == 0 && TvShowModuleManager.getInstance().getSettings().isSpecialSeason()) {
        filename = "season-specials-thumb." + extension;
      }

      return seasonFoldername + File.separator + filename;
    }
  },

  /** season_folder/seasonXX-landscape.* */
  SEASON_FOLDER_LANDSCAPE {
    @Override
    public String getFilename(TvShowSeason tvShowSeason, String extension, boolean forRenamer) {
      TvShow tvShow = tvShowSeason.getTvShow();
      if (tvShow == null) {
        return "";
      }

      String seasonFoldername = getSeasonFolder(tvShowSeason, forRenamer);

      // check whether the season folder name exists or not; do not create it just for the artwork!
      if (StringUtils.isBlank(seasonFoldername)) {
        // no season folder name in the templates found - fall back to the show base filename style
        return SEASON_LANDSCAPE.getFilename(tvShowSeason, extension, forRenamer);
      }

      return seasonFoldername + File.separator + String.format("season%02d-landscape.%s", tvShowSeason.getSeason(), extension);
    }
  }
}
