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

package org.tinymediamanager.ui.tvshows.filters;

import java.util.List;
import java.util.regex.Matcher;

import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * the class {@link TvShowVideoFilenameFilter} provides a filter for TV show video filenames
 * 
 * @author Wolfgang Janes
 */
public class TvShowVideoFilenameFilter extends AbstractTextTvShowUIFilter {
  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    if (StringUtils.isBlank(normalizedFilterText)) {
      return true;
    }

    try {
      // filter on the actual / original filename of the episodes
      for (TvShowEpisode episode : episodes) {
        boolean foundEpisode = false;

        // original file name
        Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(episode.getOriginalFilename()));
        if (matcher.find()) {
          foundEpisode = true;
        }
        else {
          // actual file name
          for (MediaFile mediaFile : episode.getMediaFiles(MediaFileType.VIDEO)) {
            matcher = filterPattern.matcher(StrgUtils.normalizeString(mediaFile.getFilename()));
            if (matcher.find()) {
              foundEpisode = true;
              break;
            }
          }
        }

        // if there is a match in this episode, we can stop
        if (invert && !foundEpisode) {
          return true;
        }
        else if (!invert && foundEpisode) {
          return true;
        }
      }

    }
    catch (Exception e) {
      // if any exceptions are thrown, just return true
      return true;
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.videofilename"));
  }

  @Override
  public String getId() {
    return "tvShowVideoFilename";
  }
}
