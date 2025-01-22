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
import org.tinymediamanager.core.tvshow.TvShowRenamer;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class {@link TvShowUndoRenameAction} is used to undo renaming of TV shows/episodes
 * 
 * @author Manuel Laggner
 */
public class TvShowUndoRenameAction extends TmmAction {
  public TvShowUndoRenameAction() {
    putValue(SMALL_ICON, IconManager.UNDO);
    putValue(NAME, TmmResourceBundle.getString("tvshow.rename.undo"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.rename.undo.desc"));
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

    // undo
    for (TvShow tvShow : selectedObjects.getTvShows()) {
      TvShowRenamer.undoRename(tvShow);
    }

    for (TvShowEpisode episode : selectedObjects.getEpisodesRecursive()) {
      TvShowRenamer.undoRename(episode);
    }
  }
}
