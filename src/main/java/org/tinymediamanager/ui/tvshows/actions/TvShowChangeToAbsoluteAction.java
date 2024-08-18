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

import javax.swing.JOptionPane;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroupType;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * the class {@link TvShowChangeToAbsoluteAction} is used to treat the existing S/EE ordering as S1 EEE absolute one.
 *
 * @author Myron Boyle
 */
public class TvShowChangeToAbsoluteAction extends TmmAction {
  public TvShowChangeToAbsoluteAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.changetoabs.title"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.changetoabs.title"));
    putValue(SMALL_ICON, IconManager.ABSOLUTE);
    putValue(LARGE_ICON_KEY, IconManager.ABSOLUTE);
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

    int decision = JOptionPane.showConfirmDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tvshow.changetoabs.message"),
        TmmResourceBundle.getString("tvshow.changetoabs.title"), JOptionPane.YES_NO_OPTION);
    if (decision == JOptionPane.YES_OPTION) {
      // we always detect 3 numbers as SEE
      // now we change all to S1 EEE
      for (TvShowEpisode episode : selectedObjects.getEpisodesRecursive()) {
        int ep = episode.getEpisode(EpisodeGroupType.AIRED); // need aired, else we get the ABSOLUTE in second run
        int s = episode.getSeason(EpisodeGroupType.AIRED);
        if (s < 0) {
          s = 0;
        }
        int abs = s * 100 + ep;
        episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_ABSOLUTE, 1, abs));
        episode.saveToDb();
      }

      // second check: are the EPs in ascending order, if we ignore the season?
      // https://www.reddit.com/r/tinyMediaManager/comments/1dfr88n/episode_count_doesnt_reset_back_to_e01/

      for (TvShow show : selectedObjects.getTvShows()) {
        if (!show.getEpisodeGroups().contains(MediaEpisodeGroup.DEFAULT_ABSOLUTE)) {
          show.addEpisodeGroup(MediaEpisodeGroup.DEFAULT_ABSOLUTE);
        }
        show.setEpisodeGroup(MediaEpisodeGroup.DEFAULT_ABSOLUTE);
        show.saveToDb();
      }
    }
  }
}
