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

package org.tinymediamanager.core.movie.filenaming;

import org.apache.commons.lang3.StringUtils;

/**
 * The enum {@link MovieBannerNaming} - used to generate the banner filename
 * 
 * @author Manuel Laggner
 */
public enum MovieSetBannerNaming implements IMovieSetFileNaming {

  /**
   * movieset-banner.ext - in movie folder
   */
  MOVIE_BANNER {
    @Override
    public String getFilename(String basename, String extension) {
      return "movieset-banner." + extension;
    }

    @Override
    public Location getFolderLocation() {
      return Location.MOVIE_FOLDER;
    }
  },

  /**
   * [movieset name]-banner.ext - in movie folder
   */
  MOVIESET_BANNER {
    @Override
    public String getFilename(String basename, String extension) {
      return StringUtils.isNotBlank(basename) ? basename + "-banner." + extension : "";
    }

    @Override
    public Location getFolderLocation() {
      return Location.MOVIE_FOLDER;
    }
  },

  /**
   * banner.ext - in artwork folder
   */
  KODI_BANNER {
    @Override
    public String getFilename(String basename, String extension) {
      return "banner." + extension;
    }

    @Override
    public Location getFolderLocation() {
      return Location.KODI_STYLE_FOLDER;
    }
  },

  /**
   * [movie set name]-banner.ext - in artwork folder
   */
  AUTOMATOR_BANNER {
    @Override
    public String getFilename(String basename, String extension) {
      return StringUtils.isNotBlank(basename) ? basename + "-banner." + extension : "";
    }

    @Override
    public Location getFolderLocation() {
      return Location.AUTOMATOR_STYLE_FOLDER;
    }
  }
}
