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
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * the class {@link MovieSubtitleFormatFilter} is used to provide a filter for the movie subtitle formats
 * 
 * @author Manuel Laggner
 */
public class MovieSubtitleFormatFilter extends AbstractCheckComboBoxMovieUIFilter<String> {
  private final MovieList movieList = MovieModuleManager.getInstance().getMovieList();

  public MovieSubtitleFormatFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> String.valueOf(s).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildSubtitleFormatArray();
    movieList.addPropertyChangeListener(Constants.SUBTITLE_FORMATS, evt -> SwingUtilities.invokeLater(this::buildSubtitleFormatArray));
  }

  @Override
  protected String parseTypeToString(String type) throws Exception {
    return type;
  }

  @Override
  protected String parseStringToType(String string) throws Exception {
    return string;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.subtitleformat"));
  }

  @Override
  public String getId() {
    return "movieSubtitleFormat";
  }

  @Override
  public boolean accept(Movie movie) {

    List<String> selectedItems = checkComboBox.getSelectedItems();
    List<String> codecs = movie.getMediaInfoSubtitleCodecList();
    for (String sel : selectedItems) {
      if (codecs.contains(sel)) {
        return true;
      }
    }

    return false;
  }

  public void buildSubtitleFormatArray() {
    List<String> subtitleFormats = new ArrayList<>(movieList.getSubtitleFormatsInMovies());
    Collections.sort(subtitleFormats);
    setValues(subtitleFormats);
  }
}
