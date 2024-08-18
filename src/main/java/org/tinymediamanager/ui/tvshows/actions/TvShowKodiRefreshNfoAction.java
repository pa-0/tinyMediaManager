/*
 * Copyright 2012 - 2024 Manuel Laggner
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The Class TvShowEditAction. To edit TV shows/episodes
 * 
 * @author Manuel Laggner
 */
public class TvShowKodiRefreshNfoAction extends TmmAction {
  public TvShowKodiRefreshNfoAction() {
    putValue(LARGE_ICON_KEY, IconManager.MEDIAINFO);
    putValue(SMALL_ICON, IconManager.MEDIAINFO);
    putValue(NAME, TmmResourceBundle.getString("kodi.rpc.refreshnfo"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    TvShowSelectionModel.SelectedObjects selectedObjects = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects();

    if (selectedObjects.isLockedFound()) {
      TvShowSelectionModel.showLockedInformation();
    }

    if (selectedObjects.isEmpty()) {
      return;
    }

    TmmTaskManager.getInstance()
        .addUnnamedTask(new TmmTask(TmmResourceBundle.getString("kodi.rpc.refreshnfo"),
            selectedObjects.getTvShows().size() + selectedObjects.getEpisodesRecursive().size(), TmmTaskHandle.TaskType.BACKGROUND_TASK) {

          @Override
          protected void doInBackground() {
            KodiRPC kodiRPC = KodiRPC.getInstance();
            int i = 0;

            // cache of all processed DbIds (better than whole objects)
            List<UUID> processed = new ArrayList<UUID>(selectedObjects.getEpisodesRecursive().size());

            // update show + all EPs
            for (TvShow tvShow : selectedObjects.getTvShows()) {
              kodiRPC.refreshFromNfo(tvShow);
              processed.addAll(tvShow.getEpisodes().stream().map(ep -> ep.getDbId()).collect(Collectors.toList()));
              publishState(++i);
              if (cancel) {
                return;
              }
            }

            // update single EP only, but not if we already had it via show...
            for (TvShowEpisode episode : selectedObjects.getEpisodesRecursive()) {
              if (!processed.contains(episode.getDbId())) {
                kodiRPC.refreshFromNfo(episode);

                publishState(++i);
                if (cancel) {
                  return;
                }
              }
            }

            // if we have updated at least one movie, we need to re-match the movies
            if (progressDone > 0) {
              try {
                // need some time to propagate the new movieId
                Thread.sleep(1000);
              }
              catch (InterruptedException e) {
                // ignore
              }
              kodiRPC.updateTvShowMappings();
            }
          }
        });
  }
}
