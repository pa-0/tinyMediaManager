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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.commons.io.FilenameUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.connector.MovieNfoParser;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle.TaskType;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

/**
 * The MovieReadNfoAction - to read the NFOs from all selected movies
 * 
 * @author Manuel Laggner
 */
public class MovieReadNfoAction extends TmmAction {
  public MovieReadNfoAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.readnfo"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.readnfo.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    final List<Movie> selectedMovies = MovieUIModule.getInstance().getSelectionModel().getSelectedMovies();

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    // rewrite selected NFOs
    TmmTaskManager.getInstance()
        .addUnnamedTask(new TmmTask(TmmResourceBundle.getString("movie.readnfo"), selectedMovies.size(), TaskType.BACKGROUND_TASK) {
          @Override
          protected void doInBackground() {
            int i = 0;
            for (Movie movie : selectedMovies) {
              Movie tempMovie = null;

              // process all registered NFOs
              for (MediaFile mf : movie.getMediaFiles(MediaFileType.NFO)) {
                // at the first NFO we get a movie object
                if (tempMovie == null) {
                  try {
                    tempMovie = MovieNfoParser.parseNfo(mf.getFileAsPath()).toMovie();
                  }
                  catch (Exception ignored) {
                  }
                  continue;
                }

                // every other NFO gets merged into that temp. movie object
                if (tempMovie != null) {
                  try {
                    tempMovie.merge(MovieNfoParser.parseNfo(mf.getFileAsPath()).toMovie());
                  }
                  catch (Exception ignored) {
                  }
                }
              }

              // no MF (yet)? try to find NFO...
              // it might have been added w/o UDS, and since we FORCE a read...
              if (tempMovie == null) {
                MediaFile vid = movie.getMainVideoFile();
                String name = vid.getFilenameWithoutStacking();
                name = FilenameUtils.getBaseName(name) + ".nfo";
                Path nfo = vid.getFileAsPath().getParent().resolve(name);
                if (Files.exists(nfo)) {
                  movie.addToMediaFiles(new MediaFile(nfo));
                  try {
                    tempMovie = MovieNfoParser.parseNfo(nfo).toMovie();
                  }
                  catch (Exception ignored) {
                  }
                }
              }

              // did we get movie data from our NFOs
              if (tempMovie != null) {
                // force merge it to the actual movie object
                movie.forceMerge(tempMovie);
                movie.saveToDb();
              }

              publishState(++i);
              if (cancel) {
                break;
              }
            }
          }
        });
  }
}
