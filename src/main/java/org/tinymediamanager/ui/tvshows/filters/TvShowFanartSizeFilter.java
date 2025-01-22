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

import javax.swing.JLabel;
import javax.swing.SpinnerNumberModel;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * the class {@link TvShowFanartSizeFilter} provides a filter for TV show fanart width
 * 
 * @author Manuel Laggner
 */
public class TvShowFanartSizeFilter extends AbstractNumberTvShowUIFilter {
  public TvShowFanartSizeFilter() {
    super();

    // display the size with px at the end
    spinnerLow.setEditor(prepareNumberEditor(spinnerLow, "####0 px"));
    spinnerHigh.setEditor(prepareNumberEditor(spinnerHigh, "####0 px"));
  }

  @Override
  protected SpinnerNumberModel getNumberModel() {
    return new SpinnerNumberModel(0, 0, 99999, 1);
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    try {
      // filter on the poster height of the TV shows/seasons/episodes

      // TV show
      boolean foundShow = matchInt(tvShow.getArtworkDimension(MediaFileType.FANART).width);

      if (!invert && foundShow) {
        return true;
      }
      else if (invert && foundShow) {
        return false;
      }

      // episode
      for (TvShowEpisode episode : episodes) {
        boolean foundEpisode = matchInt(episode.getArtworkDimension(MediaFileType.FANART).width);

        // if there is a match in this episode, we can stop
        if (invert && !foundEpisode) {
          return true;
        }
        else if (!invert && foundEpisode) {
          return true;
        }

        boolean foundSeason = matchInt(episode.getTvShowSeason().getArtworkDimension(MediaFileType.SEASON_FANART).width);
        // if there is a match in this season, we can stop
        if (invert && !foundSeason) {
          return true;
        }
        else if (!invert && foundSeason) {
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
    return new TmmLabel(TmmResourceBundle.getString("filter.fanart.width"));
  }

  @Override
  public String getId() {
    return "tvShowFanartSize";
  }
}
