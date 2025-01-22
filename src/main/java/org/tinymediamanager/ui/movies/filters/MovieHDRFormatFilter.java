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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for filtering movies by HDR Format
 *
 * @author Wolfgang Janes
 */
public class MovieHDRFormatFilter extends AbstractCheckComboBoxMovieUIFilter<String> {
  private final MovieList movieList = MovieModuleManager.getInstance().getMovieList();

  public MovieHDRFormatFilter() {

    super();
    checkComboBox.enableFilter((s, s2) -> String.valueOf(s).contains(s2));
    buildHdrFormatArray();
    movieList.addPropertyChangeListener(Constants.HDR_FORMAT, evt -> SwingUtilities.invokeLater(this::buildHdrFormatArray));

  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.hdrformat"));
  }

  @Override
  public String getId() {
    return "movieHdrFormat";
  }

  @Override
  public boolean accept(Movie movie) {
    List<String> selectedItems = checkComboBox.getSelectedItems();
    if (selectedItems.isEmpty() && movie.getVideoHDRFormat().isEmpty()) {
      return true;
    }
    for (String sel : selectedItems) {
      if (!movie.getVideoHDRFormat().contains(sel)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected String parseTypeToString(String type) throws Exception {
    return type;
  }

  @Override
  protected String parseStringToType(String string) throws Exception {
    return string;
  }

  public void buildHdrFormatArray() {
    List<String> hdrFormats = new ArrayList<>(movieList.getHDRFormatInMovies());
    Collections.sort(hdrFormats);
    setValues(hdrFormats);
  }
}
