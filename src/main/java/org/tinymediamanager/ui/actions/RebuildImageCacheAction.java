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
package org.tinymediamanager.ui.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.ui.MainWindow;

/**
 * The RebuildImageCacheAction to rebuild the whole image cache
 * 
 * @author Manuel Laggner
 */
public class RebuildImageCacheAction extends TmmAction {
  public RebuildImageCacheAction() {
    putValue(NAME, TmmResourceBundle.getString("tmm.rebuildimagecache"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tmm.rebuildimagecache"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    if (!Settings.getInstance().isImageCache()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.imagecache.notactivated"));
      return;
    }

    TmmTask task = new TmmTask(TmmResourceBundle.getString("tmm.rebuildimagecache"), 0, TmmTaskHandle.TaskType.BACKGROUND_TASK) {
      @Override
      protected void doInBackground() {
        Set<MediaFile> imageFiles = new HashSet<>();

        // movie list
        List<Movie> movies = new ArrayList<>(MovieModuleManager.getInstance().getMovieList().getMovies());
        for (Movie movie : movies) {
          imageFiles.addAll(movie.getImagesToCache());
        }

        // moviesets
        List<MovieSet> movieSets = new ArrayList<>(MovieModuleManager.getInstance().getMovieList().getMovieSetList());
        for (MovieSet movieSet : movieSets) {
          imageFiles.addAll(movieSet.getImagesToCache());
        }

        // tv shows
        List<TvShow> tvShows = new ArrayList<>(TvShowModuleManager.getInstance().getTvShowList().getTvShows());
        for (TvShow tvShow : tvShows) {
          imageFiles.addAll(tvShow.getImagesToCache());
        }

        ImageCache.clearImageCache(imageFiles);

        imageFiles.forEach(ImageCache::cacheImageAsync);
      }
    };

    TmmTaskManager.getInstance().addUnnamedTask(task);
  }
}
