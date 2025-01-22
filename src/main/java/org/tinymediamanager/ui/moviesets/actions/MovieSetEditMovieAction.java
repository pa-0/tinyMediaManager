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
package org.tinymediamanager.ui.moviesets.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.stream.Collectors;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.dialogs.MovieEditorDialog;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;

/**
 * MovieEditAction - edit movies from within moviesets
 * 
 * @author Manuel Laggner
 */
public class MovieSetEditMovieAction extends TmmAction {
  public MovieSetEditMovieAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.edit"));
    putValue(LARGE_ICON_KEY, IconManager.EDIT);
    putValue(SMALL_ICON, IconManager.EDIT);
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = MovieSetUIModule.getInstance().getSelectionModel().getSelectedMovies();

    // filter out dummy movies
    selectedMovies = selectedMovies.stream().filter(movie -> !(movie instanceof MovieSet.MovieSetMovie)).collect(Collectors.toList());

    int selectedCount = selectedMovies.size();
    int index = 0;
    int selectedTab = 0;

    if (selectedCount == 0) {
      return;
    }

    do {
      Movie movie = selectedMovies.get(index);
      MovieEditorDialog dialogMovieEditor = new MovieEditorDialog(movie, index, selectedCount, selectedTab);
      dialogMovieEditor.setVisible(true);
      selectedTab = dialogMovieEditor.getSelectedTab();

      if (!dialogMovieEditor.isContinueQueue()) {
        break;
      }

      if (dialogMovieEditor.isNavigateBack()) {
        index -= 1;
      }
      else {
        index += 1;
      }

    } while (index < selectedCount);
  }
}
