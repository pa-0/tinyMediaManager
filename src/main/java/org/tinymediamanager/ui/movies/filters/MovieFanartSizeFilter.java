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
package org.tinymediamanager.ui.movies.filters;

import javax.swing.JLabel;
import javax.swing.SpinnerNumberModel;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a movie fanart size filter
 * 
 * @author Manuel Laggner
 */
public class MovieFanartSizeFilter extends AbstractNumberMovieFilter {

  public MovieFanartSizeFilter() {
    super();

    // display the size with px at the end
    spinnerLow.setEditor(prepareNumberEditor(spinnerLow, "####0 px"));
    spinnerHigh.setEditor(prepareNumberEditor(spinnerHigh, "####0 px"));
  }

  @Override
  public String getId() {
    return "movieFanartSize";
  }

  @Override
  protected SpinnerNumberModel getNumberModel() {
    return new SpinnerNumberModel(0, 0, 99999, 1);
  }

  @Override
  public boolean accept(Movie movie) {
    FilterOption filterOption = getFilterOption();

    int low = (int) spinnerLow.getValue();
    int high = (int) spinnerHigh.getValue();

    int fanartWidth = movie.getArtworkDimension(MediaFileType.FANART).width;

    if (fanartWidth == 0) {
      return false;
    }

    if (filterOption == FilterOption.EQ && fanartWidth == low) {
      return true;
    }
    else if (filterOption == FilterOption.LT && fanartWidth < low) {
      return true;
    }
    else if (filterOption == FilterOption.LE && fanartWidth <= low) {
      return true;
    }
    else if (filterOption == FilterOption.GE && fanartWidth >= low) {
      return true;
    }
    else if (filterOption == FilterOption.GT && fanartWidth > low) {
      return true;
    }
    else if (filterOption == FilterOption.BT && low <= fanartWidth && fanartWidth <= high) {
      return true;
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("filter.fanart.width"));
  }
}
