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

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * the class {@link TvShowResetNewFlagAction} is used to remove the newlyAdded flag from selected TV shows/episodes
 */
public class TvShowResetNewFlagAction extends TmmAction {
  public TvShowResetNewFlagAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.resetnew"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.resetnew"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    TvShowSelectionModel.SelectedObjects selectedObjects = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects();

    if (selectedObjects.isEmpty()) {
      return;
    }

    selectedObjects.getTvShows().forEach(tvShow -> tvShow.setNewlyAdded(false));
    selectedObjects.getEpisodesRecursive().forEach(episode -> episode.setNewlyAdded(false));
  }
}
