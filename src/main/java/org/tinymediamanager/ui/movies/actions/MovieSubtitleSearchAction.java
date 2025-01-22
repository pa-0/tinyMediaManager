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
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;
import org.tinymediamanager.ui.movies.dialogs.MovieSubtitleChooserDialog;

/**
 * The MovieSubtitleSearchAction - search for subtitles for all selected movies
 * 
 * @author Manuel Laggner
 */
public class MovieSubtitleSearchAction extends TmmAction {
  public MovieSubtitleSearchAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.search.subtitle"));
    putValue(SMALL_ICON, IconManager.SUBTITLE);
    putValue(LARGE_ICON_KEY, IconManager.SUBTITLE);
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.search.subtitle"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = MovieUIModule.getInstance().getSelectionModel().getSelectedMovies();

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    for (Movie movie : selectedMovies) {
      // no subtitle download for discs
      if (movie.isDisc()) {
        continue;
      }

      // show the dialog for every movie video file (multi part video files problem)
      for (MediaFile mediaFile : movie.getMediaFiles(MediaFileType.VIDEO)) {
        MovieSubtitleChooserDialog dialogMovieSubtitleChooser = new MovieSubtitleChooserDialog(movie, mediaFile, selectedMovies.size() > 1);
        if (!dialogMovieSubtitleChooser.showDialog()) {
          return;
        }
      }
    }
  }
}
