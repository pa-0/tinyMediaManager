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

import java.awt.Cursor;
import java.awt.event.ActionEvent;

import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class TvShowClearImageCacheAction. To clear the image cache for all TV shows/episodes
 * 
 * @author Manuel Laggner
 */
public class TvShowClearImageCacheAction extends TmmAction {
  public TvShowClearImageCacheAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.clearimagecache"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    TvShowSelectionModel.SelectedObjects selectedObjects = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects(false, true);

    if (selectedObjects.isEmpty()) {
      return;
    }

    // clear the cache
    MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    for (TvShow tvShow : selectedObjects.getTvShows()) {
      ImageCache.clearImageCacheForMediaEntity(tvShow);
    }

    for (TvShowSeason tvShowSeason : selectedObjects.getSeasonsRecursive()) {
      ImageCache.clearImageCache(tvShowSeason.getMediaFiles());
    }

    for (TvShowEpisode episode : selectedObjects.getEpisodesRecursive()) {
      ImageCache.clearImageCacheForMediaEntity(episode);
    }
    MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }
}
