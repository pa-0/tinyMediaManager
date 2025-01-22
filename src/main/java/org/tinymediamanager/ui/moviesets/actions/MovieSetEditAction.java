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

import javax.swing.JOptionPane;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.dialogs.MovieEditorDialog;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;
import org.tinymediamanager.ui.moviesets.dialogs.MovieSetEditorDialog;

/**
 * MovieEditAction - edit movies from within moviesets
 * 
 * @author Manuel Laggner
 */
public class MovieSetEditAction extends TmmAction {
  public MovieSetEditAction() {
    putValue(NAME, TmmResourceBundle.getString("movieset.edit"));
    putValue(LARGE_ICON_KEY, IconManager.EDIT);
    putValue(SMALL_ICON, IconManager.EDIT);
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movieset.edit"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Object> selectedObjects = MovieSetUIModule.getInstance().getSelectionModel().getSelectedObjects();

    if (selectedObjects.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    // filter out dummy movies
    selectedObjects = selectedObjects.stream().filter(obj -> !(obj instanceof MovieSet.MovieSetMovie)).collect(Collectors.toList());

    int selectedCount = selectedObjects.size();
    int index = 0;
    int selectedMovieTab = 0;
    int selectedMoviesetTab = 0;

    if (selectedCount == 0) {
      return;
    }

    do {
      Object object = selectedObjects.get(index);

      if (object instanceof MovieSet) {
        MovieSet movieSet = (MovieSet) object;
        MovieSetEditorDialog editor = new MovieSetEditorDialog(movieSet, index, selectedCount, selectedMoviesetTab);
        editor.setVisible(true);
        selectedMoviesetTab = editor.getSelectedTab();
        if (!editor.isContinueQueue()) {
          break;
        }

        if (editor.isNavigateBack()) {
          index -= 1;
        }
        else {
          index += 1;
        }
      }
      else if (object instanceof Movie) {
        Movie movie = (Movie) object;
        MovieEditorDialog editor = new MovieEditorDialog(movie, index, selectedCount, selectedMovieTab);
        editor.setVisible(true);
        selectedMovieTab = editor.getSelectedTab();
        if (!editor.isContinueQueue()) {
          break;
        }

        if (editor.isNavigateBack()) {
          index -= 1;
        }
        else {
          index += 1;
        }
      }
    } while (index < selectedCount);
  }
}
