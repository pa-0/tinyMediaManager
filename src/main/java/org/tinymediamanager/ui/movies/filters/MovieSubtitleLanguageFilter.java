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
 * the class {@link MovieSubtitleLanguageFilter} is used to provide a filter for the movie subtitle language
 * 
 * @author Wolfgang Janes
 */
public class MovieSubtitleLanguageFilter extends AbstractCheckComboBoxMovieUIFilter<String> {
  private final MovieList movieList = MovieModuleManager.getInstance().getMovieList();

  public MovieSubtitleLanguageFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> String.valueOf(s).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildSubtitleLanguageArray();
    movieList.addPropertyChangeListener(Constants.SUBTITLE_LANGUAGES, evt -> SwingUtilities.invokeLater(this::buildSubtitleLanguageArray));
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
    return new TmmLabel(TmmResourceBundle.getString("metatag.subtitlelanguage"));
  }

  @Override
  public String getId() {
    return "movieSubtitleLanguage";
  }

  @Override
  public boolean accept(Movie movie) {

    List<String> selectedItems = checkComboBox.getSelectedItems();
    List<String> lang = movie.getMediaInfoSubtitleLanguageList();
    for (String sel : selectedItems) {
      if (lang.contains(sel)) {
        return true;
      }
    }

    return false;
  }

  public void buildSubtitleLanguageArray() {
    List<String> subtitleLanguages = new ArrayList<>(movieList.getSubtitleLanguagesInMovies());
    Collections.sort(subtitleLanguages);
    setValues(subtitleLanguages);
  }
}
