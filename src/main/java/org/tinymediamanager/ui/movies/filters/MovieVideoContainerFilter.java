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

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a video container movie filter
 * 
 * @author Manuel Laggner
 */
public class MovieVideoContainerFilter extends AbstractCheckComboBoxMovieUIFilter<String> {
  private final MovieList movieList = MovieModuleManager.getInstance().getMovieList();

  public MovieVideoContainerFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> s.toLowerCase(Locale.ROOT).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildAndInstallContainerArray();
    PropertyChangeListener propertyChangeListener = evt -> buildAndInstallContainerArray();
    movieList.addPropertyChangeListener(Constants.VIDEO_CONTAINER, propertyChangeListener);
  }

  @Override
  public String getId() {
    return "movieVideoContainer";
  }

  @Override
  public boolean accept(Movie movie) {
    List<String> selectedValues = checkComboBox.getSelectedItems();

    String container = movie.getMediaInfoContainerFormat();

    for (String value : selectedValues) {
      if (value.equalsIgnoreCase(container)) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.container"));
  }

  private void buildAndInstallContainerArray() {
    List<String> containers = new ArrayList<>(movieList.getVideoContainersInMovies());
    Collections.sort(containers);

    setValues(containers);
  }

  @Override
  protected String parseTypeToString(String type) throws Exception {
    return type;
  }

  @Override
  protected String parseStringToType(String string) throws Exception {
    return string;
  }
}
