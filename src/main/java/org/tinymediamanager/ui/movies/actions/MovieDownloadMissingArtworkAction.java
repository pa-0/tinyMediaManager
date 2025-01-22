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

import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.tasks.MovieMissingArtworkDownloadTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;
import org.tinymediamanager.ui.movies.dialogs.MovieDownloadMissingArtworkDialog;

/**
 * The class {@link MovieDownloadMissingArtworkAction} is used to download missing artwork (via scraping) for the selected movies
 * 
 * @author Manuel Laggner
 */
public class MovieDownloadMissingArtworkAction extends TmmAction {
  public MovieDownloadMissingArtworkAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.downloadmissingartwork"));
    putValue(SMALL_ICON, IconManager.IMAGE);
    putValue(LARGE_ICON_KEY, IconManager.IMAGE);
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = MovieUIModule.getInstance().getSelectionModel().getSelectedMovies();

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    MovieDownloadMissingArtworkDialog dialog = new MovieDownloadMissingArtworkDialog();
    dialog.setVisible(true);

    // get options from dialog
    MovieSearchAndScrapeOptions options = dialog.getMovieSearchAndScrapeOptions();

    List<MovieScraperMetadataConfig> config = dialog.getMovieScraperMetadataConfig();

    // do we want to scrape?
    if (dialog.shouldStartScrape() && ScraperMetadataConfig.containsAnyArtwork(config)) {
      MovieMissingArtworkDownloadTask task = new MovieMissingArtworkDownloadTask(selectedMovies, options, config);
      TmmTaskManager.getInstance().addDownloadTask(task);
    }
  }
}
