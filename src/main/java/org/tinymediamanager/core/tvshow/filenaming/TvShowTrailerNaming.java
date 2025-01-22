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

package org.tinymediamanager.core.tvshow.filenaming;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.IFileNaming;

/**
 * the class {@link TvShowTrailerNaming} is used to provide TV show related trailer filenames
 *
 * @author Manuel Laggner
 */
public enum TvShowTrailerNaming implements IFileNaming {
  /**
   * tvshow-trailer.*
   */
  TVSHOW_TRAILER {
    @Override
    public String getFilename(String basename, String extension) {
      return "tvshow-trailer." + extension;
    }
  },

  /**
   * <tvshow title>-trailer.*
   */
  TVSHOWNAME_TRAILER {
    @Override
    public String getFilename(String basename, String extension) {
      return StringUtils.isNotBlank(basename) ? basename + "-trailer." + extension : "";
    }
  },

  /**
   * trailers/<tvshow title>-trailer.*
   */
  TRAILERS_TVSHOWNAME_TRAILER {
    @Override
    public String getFilename(String basename, String extension) {
      return StringUtils.isNotBlank(basename) ? "trailers" + File.separator + basename + "-trailer." + extension : "";
    }
  },

  /**
   * trailer.*
   */
  TRAILER {
    @Override
    public String getFilename(String basename, String extension) {
      return "trailer." + extension;
    }
  }

}
