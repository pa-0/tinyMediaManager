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
import java.util.Set;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel.SelectedObjects;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

public class DebugDumpShowAction extends TmmAction {
  private static final long serialVersionUID = 1L;

  public DebugDumpShowAction() {
    putValue(NAME, TmmResourceBundle.getString("debug.entity.dump"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("debug.entity.dump.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    SelectedObjects sel = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects(true, true);

    Set<TvShow> selectedTvShows = sel.getTvShows();
    for (TvShow tvShow : selectedTvShows) {
      TvShowModuleManager.getInstance().dump(tvShow, false);
    }

    Set<TvShowSeason> selectedSeason = sel.getSeasons();
    for (TvShowSeason se : selectedSeason) {
      TvShowModuleManager.getInstance().dump(se, false);
    }

    Set<TvShowEpisode> selectedEpisodes = sel.getEpisodes();
    for (TvShowEpisode ep : selectedEpisodes) {
      TvShowModuleManager.getInstance().dump(ep);
    }
  }
}
