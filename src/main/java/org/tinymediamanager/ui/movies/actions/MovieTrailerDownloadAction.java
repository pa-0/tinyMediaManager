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
package org.tinymediamanager.ui.movies.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieHelpers;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

/**
 * The class MovieTrailerDownloadAction is used to trigger trailer download for selected movies
 *
 * @author Manuel Laggner
 */
public class MovieTrailerDownloadAction extends TmmAction {
  public MovieTrailerDownloadAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.downloadtrailer"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.downloadtrailer"));
    putValue(SMALL_ICON, IconManager.DOWNLOAD);
    putValue(LARGE_ICON_KEY, IconManager.DOWNLOAD);
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = MovieUIModule.getInstance().getSelectionModel().getSelectedMovies();

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    // first check if there is at least one movie containing a trailer mf
    boolean existingTrailer = false;
    for (Movie movie : selectedMovies) {
      if (!movie.getMediaFiles(MediaFileType.TRAILER).isEmpty()) {
        existingTrailer = true;
        break;
      }
    }

    // if there is any existing trailer found, show a message dialog
    boolean overwriteTrailer = false;
    if (existingTrailer) {
      Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
      int answer = JOptionPane.showOptionDialog(MainWindow.getFrame(), TmmResourceBundle.getString("movie.overwritetrailer"),
          TmmResourceBundle.getString("movie.downloadtrailer"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
      if (answer == JOptionPane.YES_OPTION) {
        overwriteTrailer = true;
      }
    }

    // start tasks
    for (Movie movie : selectedMovies) {
      if (!movie.getMediaFiles(MediaFileType.TRAILER).isEmpty() && !overwriteTrailer) {
        continue;
      }
      if (movie.getTrailer().isEmpty()) {
        continue;
      }
      MovieHelpers.downloadBestTrailer(movie);
    }
  }
}
