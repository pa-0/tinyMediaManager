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
import java.util.List;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

public class MovieChecksumAction extends TmmAction {
  private static final long serialVersionUID = 1L;

  public MovieChecksumAction() {
    putValue(NAME, TmmResourceBundle.getString("checksum.crc32.calculate"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("checksum.crc32.calculate"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = MovieUIModule.getInstance().getSelectionModel().getSelectedMovies(true);

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    TmmTask task = new TmmTask(TmmResourceBundle.getString("checksum.crc32.calculate"), selectedMovies.size(),
        TmmTaskHandle.TaskType.BACKGROUND_TASK) {
      @Override
      protected void doInBackground() {
        for (Movie movie : selectedMovies) {
          MediaFile main = movie.getMainVideoFile();
          String crc = Utils.getCRC32(main.getFileAsPath());
          if (!crc.isEmpty()) {
            main.setCRC32(crc);
            movie.saveToDb();
          }
        }
      }
    };

    TmmTaskManager.getInstance().addUnnamedTask(task);
  }
}
