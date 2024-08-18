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
package org.tinymediamanager.ui.movies.filters;

import java.time.LocalDate;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a year movie filter
 * 
 * @author Manuel Laggner
 */
public class MovieYearFilter extends AbstractNumberMovieFilter {

  public MovieYearFilter() {
    super();

    // display the year without any formatting
    spinnerLow.setEditor(new JSpinner.NumberEditor(spinnerLow, "###0"));
    spinnerHigh.setEditor(new JSpinner.NumberEditor(spinnerHigh, "###0"));
  }

  @Override
  public String getId() {
    return "movieYear";
  }

  @Override
  public void clearFilter() {
    spinnerLow.setValue(LocalDate.now().getYear());
    spinnerHigh.setValue(LocalDate.now().getYear());
  }

  @Override
  public void setFilterValue(Object value) {
    String[] values = value.toString().split(",");
    if (values.length > 0) {
      spinnerLow.setValue(MetadataUtil.parseInt(values[0], LocalDate.now().getYear()));
    }
    if (values.length > 1) {
      spinnerHigh.setValue(MetadataUtil.parseInt(values[1], LocalDate.now().getYear()));
    }
  }

  @Override
  protected SpinnerNumberModel getNumberModel() {
    return new SpinnerNumberModel(LocalDate.now().getYear(), 1800, 2100, 1);
  }

  @Override
  public boolean accept(Movie movie) {
    return matchInt(movie.getYear());
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.year"));
  }
}
