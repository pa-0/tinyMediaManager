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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.movie.tasks.MovieRenameTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;

/**
 * @author Manuel Laggner
 * 
 */
public class MovieSetRenameAction extends TmmAction {
  public MovieSetRenameAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.rename"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.rename"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Object> selectedObjects = MovieSetUIModule.getInstance().getSelectionModel().getSelectedObjects();
    Set<Movie> selectedMovies = new HashSet<>();

    for (Object obj : selectedObjects) {
      if (obj instanceof MovieSet.MovieSetMovie) {
        // do nothing
      }
      else if (obj instanceof Movie movie) {
        selectedMovies.add(movie);
      }
      else if (obj instanceof MovieSet movieSet) {
        selectedMovies.addAll(movieSet.getMovies());
      }
    }

    if (selectedMovies.isEmpty()) {
      return;
    }

    // rename
    TmmThreadPool renameTask = new MovieRenameTask(new ArrayList<>(selectedMovies));
    TmmTaskManager.getInstance().addMainTask(renameTask);
  }
}
