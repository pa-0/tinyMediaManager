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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle.TaskType;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.connector.TvShowNfoParser;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class TvShowReadNfoAction. To rewrite the NFOs of the selected TV shows
 * 
 * @author Manuel Laggner
 */
public class TvShowReadNfoAction extends TmmAction {
  public TvShowReadNfoAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.readnfo"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.readnfo.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    final List<TvShow> selectedTvShows = TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShowsRecursive();

    if (selectedTvShows.isEmpty()) {
      return;
    }

    // rewrite selected NFOs
    TmmTaskManager.getInstance()
        .addUnnamedTask(new TmmTask(TmmResourceBundle.getString("tvshow.readnfo"), selectedTvShows.size(), TaskType.BACKGROUND_TASK) {
          @Override
          protected void doInBackground() {
            int i = 0;
            for (TvShow tvShow : selectedTvShows) {
              TvShow tempTvShow = null;

              // process all registered NFOs
              for (MediaFile mf : tvShow.getMediaFiles(MediaFileType.NFO)) {
                // at the first NFO we get a movie object
                if (tempTvShow == null) {
                  try {
                    tempTvShow = TvShowNfoParser.parseNfo(mf.getFileAsPath()).toTvShow();
                  }
                  catch (Exception ignored) {
                  }
                  continue;
                }

                // every other NFO gets merged into that temp. movie object
                if (tempTvShow != null) {
                  try {
                    tempTvShow.merge(TvShowNfoParser.parseNfo(mf.getFileAsPath()).toTvShow());
                  }
                  catch (Exception ignored) {
                  }
                }
              }

              // no MF (yet)? try to find NFO...
              // it might have been added w/o UDS, and since we FORCE a read...
              if (tempTvShow == null) {
                Path nfo = tvShow.getPathNIO().resolve("tvshow.nfo");
                if (Files.exists(nfo)) {
                  tvShow.addToMediaFiles(new MediaFile(nfo));
                  try {
                    tempTvShow = TvShowNfoParser.parseNfo(nfo).toTvShow();
                  }
                  catch (IOException ignored) {
                  }
                }
              }

              // did we get movie data from our NFOs
              if (tempTvShow != null) {
                // force merge it to the actual movie object
                tvShow.forceMerge(tempTvShow);
                tvShow.saveToDb();
              }
              publishState(++i);
              if (cancel) {
                break;
              }
            }
          }
        });
  }
}
