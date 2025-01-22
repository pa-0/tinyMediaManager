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

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a movie runtime filter
 * 
 * @author Manuel Laggner
 */
public class MovieRuntimeFilter extends AbstractNumberMovieFilter {

  public MovieRuntimeFilter() {
    super();

    // display the size with min at the end
    spinnerLow.setEditor(prepareNumberEditor(spinnerLow, "##0 min"));
    spinnerHigh.setEditor(prepareNumberEditor(spinnerHigh, "##0 min"));
  }

  @Override
  public String getId() {
    return "movieVideoRuntime";
  }

  @Override
  protected SpinnerNumberModel getNumberModel() {
    return new SpinnerNumberModel(0, 0, 999, 1);
  }

  @Override
  public boolean accept(Movie movie) {
    return matchInt(movie.getRuntimeFromMediaFilesInMinutes());
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.runtime"));
  }
}
