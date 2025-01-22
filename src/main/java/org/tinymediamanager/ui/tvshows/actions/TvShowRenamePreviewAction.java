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
import java.util.List;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowRenamerPreviewDialog;

/**
 * The class {@link TvShowRenamePreviewAction}. This action is for creating a preview of the renamer
 *
 * @author Manuel Laggner
 */
public class TvShowRenamePreviewAction extends TmmAction {
  public TvShowRenamePreviewAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.renamepreview"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.renamepreview.hint"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    final List<TvShow> selectedTvShows = TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShowsRecursive();

    if (selectedTvShows.isEmpty()) {
      return;
    }

    TvShowRenamerPreviewDialog dialog = new TvShowRenamerPreviewDialog(selectedTvShows);
    dialog.setVisible(true);
  }
}
