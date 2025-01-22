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
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * the class {@link TvShowNoteFilter} is used to filter TV shows/episodes for their note
 *
 * @author Wolfgang Janes
 */
public class TvShowNoteFilter extends AbstractTextTvShowUIFilter {

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.note"));
  }

  @Override
  public String getId() {
    return "TvShowNote";
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    if (StringUtils.isBlank(normalizedFilterText)) {
      return true;
    }

    try {
      for (TvShowEpisode episode : episodes) {
        boolean foundEpisode = false;
        Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(episode.getNote()));
        if (matcher.find()) {
          foundEpisode = true;
        }

        if (invert && !foundEpisode) {
          return true;
        }
        else if (!invert && foundEpisode) {
          return true;
        }
      }

      boolean foundShow = false;
      Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(tvShow.getNote()));
      if (matcher.find()) {
        foundShow = true;
      }

      if (!invert && foundShow) {
        return true;
      }
      else if (invert && foundShow) {
        return false;
      }

      // also look in the seasons
      for (TvShowSeason season : tvShow.getSeasons()) {
        boolean foundSeason = false;
        matcher = filterPattern.matcher(StrgUtils.normalizeString(season.getNote()));
        if (matcher.find()) {
          foundSeason = true;
        }

        if (invert && !foundSeason) {
          return true;
        }
        else if (!invert && foundSeason) {
          return true;
        }
      }
    }
    catch (Exception e) {
      return true;
    }

    return false;
  }
}
