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
import java.util.ArrayList;
import java.util.List;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.dialogs.CleanUpUnwantedFilesDialog;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * the class {@link TvShowCleanUpFilesAction} is used to trigger a _cleanup_ to remove unwanted files
 * 
 * @author Wolfgang Janes
 */
public class TvShowCleanUpFilesAction extends TmmAction {

  public TvShowCleanUpFilesAction() {
    putValue(NAME, TmmResourceBundle.getString("cleanupfiles"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("cleanupfiles.desc"));
    putValue(SMALL_ICON, IconManager.DELETE);
    putValue(LARGE_ICON_KEY, IconManager.DELETE);
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<MediaEntity> selectedTvShows = new ArrayList<>(TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShowsRecursive());

    if (selectedTvShows.isEmpty()) {
      return;
    }

    CleanUpUnwantedFilesDialog dialog = new CleanUpUnwantedFilesDialog(selectedTvShows);
    dialog.setVisible(true);

  }
}
