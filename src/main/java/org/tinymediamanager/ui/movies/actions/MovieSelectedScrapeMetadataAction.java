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

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.tasks.MovieScrapeTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;
import org.tinymediamanager.ui.movies.dialogs.MovieScrapeMetadataDialog;

/**
 * The MovieSelectedScrapeMetadataAction - to rescrape metadata of selected movies
 * 
 * @author Manuel Laggner
 */
public class MovieSelectedScrapeMetadataAction extends TmmAction {
  public MovieSelectedScrapeMetadataAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.scrape.metadata"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.scrape.metadata.desc"));
    putValue(SMALL_ICON, IconManager.SEARCH);
    putValue(LARGE_ICON_KEY, IconManager.SEARCH);
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = MovieUIModule.getInstance().getSelectionModel().getSelectedMovies();

    if (selectedMovies.isEmpty()) {
      return;
    }

    MovieScrapeMetadataDialog dialog = new MovieScrapeMetadataDialog(TmmResourceBundle.getString("movie.scrape.metadata"));
    dialog.setLocationRelativeTo(MainWindow.getInstance());
    dialog.setVisible(true);

    // get options from dialog
    MovieSearchAndScrapeOptions options = dialog.getMovieSearchAndScrapeOptions();
    List<MovieScraperMetadataConfig> config = dialog.getMovieScraperMetadataConfig();
    boolean overwrite = dialog.getOverwriteExistingItems();

    // do we want to scrape?
    if (dialog.shouldStartScrape()) {
      // scrape
      TmmThreadPool scrapeTask = new MovieScrapeTask(
          new MovieScrapeTask.MovieScrapeParams(selectedMovies, options, config).setDoSearch(false).setOverwriteExistingItems(overwrite));
      TmmTaskManager.getInstance().addMainTask(scrapeTask);
    }
  }
}
