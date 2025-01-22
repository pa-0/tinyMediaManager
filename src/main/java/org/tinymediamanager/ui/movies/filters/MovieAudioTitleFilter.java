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
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * the class {@link MovieAudioTitleFilter} is a filter for audio titles for movies
 *
 * @author Wolfgang Janes
 */
public class MovieAudioTitleFilter extends AbstractCheckComboBoxMovieUIFilter<String> {

  private MovieList movieList = MovieModuleManager.getInstance().getMovieList();

  public MovieAudioTitleFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> String.valueOf(s).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildAudioTitleArray();
    movieList.addPropertyChangeListener(Constants.AUDIO_TITLE, evt -> SwingUtilities.invokeLater(this::buildAudioTitleArray));
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.audiotitle"));
  }

  @Override
  public String getId() {
    return "movieAudioTitle";
  }

  @Override
  public boolean accept(Movie movie) {

    List<String> selectedItems = checkComboBox.getSelectedItems();
    List<MediaFile> mediaFileList = movie.getMediaFiles(MediaFileType.VIDEO);

    for (MediaFile mf : mediaFileList) {
      // check for explicit empty search
      if (selectedItems.isEmpty() && mf.getAudioTitleList().isEmpty()) {
        return true;
      }
      for (String title : mf.getAudioTitleList()) {
        if (selectedItems.contains(title)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  protected String parseTypeToString(String type) throws Exception {
    return type;
  }

  @Override
  protected String parseStringToType(String string) throws Exception {
    return string;
  }

  private void buildAudioTitleArray() {
    List<String> audioTitles = new ArrayList<>(movieList.getAudioTitlesInMovies());
    Collections.sort(audioTitles);
    setValues(audioTitles);
  }
}
