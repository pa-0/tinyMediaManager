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
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel.SelectedObjects;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

public class TvshowChecksumsAction extends TmmAction {
  private static final long serialVersionUID = 1L;

  public TvshowChecksumsAction() {
    putValue(NAME, TmmResourceBundle.getString("checksum.crc32.calculate"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("checksum.crc32.calculate"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    SelectedObjects sel = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects(false, false);

    TmmTask task = new TmmTask(TmmResourceBundle.getString("checksum.crc32.calculate"), sel.getEpisodes().size(),
        TmmTaskHandle.TaskType.BACKGROUND_TASK) {
      @Override
      protected void doInBackground() {
        Set<TvShowEpisode> selectedEpisodes = sel.getEpisodesRecursive(); // all EPs, even when show/Season clicked!
        for (TvShowEpisode ep : selectedEpisodes) {
          MediaFile main = ep.getMainVideoFile();
          String crc = Utils.getCRC32(main.getFileAsPath());
          if (!crc.isEmpty()) {
            main.setCRC32(crc);
            ep.saveToDb();
          }
        }
      }
    };

    TmmTaskManager.getInstance().addUnnamedTask(task);
  }
}
