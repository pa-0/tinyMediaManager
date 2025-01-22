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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.dialogs.MovieBulkEditorDialog;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;

/**
 * The MovieSetBatchEditMovieAction - to start a bulk edit of movies
 * 
 * @author Manuel Laggner
 */
public class MovieSetBatchEditMovieAction extends TmmAction {
  public MovieSetBatchEditMovieAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.bulkedit"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.bulkedit.desc"));
    putValue(SMALL_ICON, IconManager.EDIT);
    putValue(LARGE_ICON_KEY, IconManager.EDIT);
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = new ArrayList<>(MovieSetUIModule.getInstance().getSelectionModel().getSelectedMovies());

    // filter out dummy movies
    selectedMovies = selectedMovies.stream().filter(movie -> !(movie instanceof MovieSet.MovieSetMovie)).collect(Collectors.toList());

    if (selectedMovies.isEmpty()) {
      return;
    }

    // get data of all files within all selected movies
    MovieBulkEditorDialog editor = new MovieBulkEditorDialog(selectedMovies);
    editor.setLocationRelativeTo(MainWindow.getInstance());
    editor.pack();
    editor.setVisible(true);
  }
}
