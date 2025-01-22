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
package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class TvShowRebuildImageCacheAction. To rebuild the image cache selected TV shows/episodes
 * 
 * @author Manuel Laggner
 */
public class TvShowRebuildImageCacheAction extends TmmAction {
  public TvShowRebuildImageCacheAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.rebuildimagecache"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.rebuildimagecache"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    if (!Settings.getInstance().isImageCache()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.imagecache.notactivated"));
      return;
    }

    TvShowSelectionModel.SelectedObjects selectedObjects = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects();

    if (selectedObjects.isLockedFound()) {
      TvShowSelectionModel.showLockedInformation();
    }

    if (selectedObjects.isEmpty()) {
      return;
    }

    TmmTask task = new TmmTask(TmmResourceBundle.getString("tmm.rebuildimagecache"), 0, TmmTaskHandle.TaskType.BACKGROUND_TASK) {
      @Override
      protected void doInBackground() {
        Set<MediaFile> imageFiles = new HashSet<>();

        // get data of all files within all selected TV shows/episodes
        for (TvShow tvShow : selectedObjects.getTvShows()) {
          imageFiles.addAll(tvShow.getImagesToCache());
        }

        for (TvShowSeason season : selectedObjects.getSeasonsRecursive()) {
          imageFiles.addAll(season.getImagesToCache());
        }

        for (TvShowEpisode episode : selectedObjects.getEpisodesRecursive()) {
          imageFiles.addAll(episode.getImagesToCache());
        }

        ImageCache.clearImageCache(imageFiles);

        imageFiles.forEach(ImageCache::cacheImageAsync);
      }
    };

    TmmTaskManager.getInstance().addUnnamedTask(task);
  }
}
