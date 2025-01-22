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
import java.util.Collection;
import java.util.Map;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.imdb.ImdbTvShowMetadataProvider;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * the class {@link TvShowFetchImdbTop250} is used to download IMDB Top 250 listing
 *
 * @author Myron Boyle
 */
public class TvShowFetchImdbTop250 extends TmmAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowFetchImdbTop250.class);

  public TvShowFetchImdbTop250() {
    putValue(LARGE_ICON_KEY, IconManager.RATING_BLUE);
    putValue(SMALL_ICON, IconManager.RATING_BLUE);
    putValue(NAME, TmmResourceBundle.getString("tvshow.fetchtop250"));
    // putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    TvShowSelectionModel.SelectedObjects selectedObjects = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects(false, false);

    Collection<TvShow> tvShows = selectedObjects.getTvShows();
    Collection<TvShowEpisode> episodes = selectedObjects.getEpisodesRecursive();

    if (tvShows.isEmpty() && episodes.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    TmmTaskManager.getInstance()
        .addUnnamedTask(new TmmTask(TmmResourceBundle.getString("tvshow.fetchtop250"), 0, TmmTaskHandle.TaskType.BACKGROUND_TASK) {

          @Override
          protected void doInBackground() {
            try {
              ImdbTvShowMetadataProvider mp = new ImdbTvShowMetadataProvider();
              Map<String, Integer> charts = mp.getTop250();
              int cnt = 0;

              for (TvShow show : tvShows) {
                String id = show.getImdbId();
                if (!id.isEmpty()) {
                  Integer top = charts.get(id);
                  if (top != null) {
                    cnt++;
                    show.setTop250(top.intValue());
                    show.saveToDb();
                    show.writeNFO();
                  }
                  else {
                    // not there any longer? invalidate
                    int current = show.getTop250();
                    if (current > 0) {
                      show.setTop250(0);
                      show.saveToDb();
                      show.writeNFO();
                    }
                  }
                }
              }

              // did we select just an episode?
              for (TvShowEpisode ep : episodes) {
                TvShow show = ep.getTvShow();
                // rule out already processed shows
                if (!tvShows.contains(show)) {
                  String id = show.getImdbId();
                  if (!id.isEmpty()) {
                    Integer top = charts.get(id);
                    if (top != null) {
                      cnt++;
                      show.setTop250(top.intValue());
                      show.saveToDb();
                      show.writeNFO();
                    }
                    else {
                      // not there any longer? invalidate
                      int current = show.getTop250();
                      if (current > 0) {
                        show.setTop250(0);
                        show.saveToDb();
                        show.writeNFO();
                      }
                    }
                  }
                }
              }

              LOGGER.info("Matched {} Top 250 values from IMDB", cnt);
            }
            catch (Exception ignored) {
              LOGGER.warn("Error fetching Top 250: {}", ignored.getMessage());
            }
          }
        });
  }
}
