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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class TvShowDeleteAction. To remove selected elements and delete it from the data source
 * 
 * @author Manuel Laggner
 */
public class TvShowDeleteAction extends TmmAction {
  public TvShowDeleteAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.delete"));
    putValue(SMALL_ICON, IconManager.DELETE_FOREVER);
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.delete.hint"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    TvShowSelectionModel.SelectedObjects selectedObjects = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects();

    if (selectedObjects.isEmpty()) {
      return;
    }

    // display warning and ask the user again
    Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
    int answer = JOptionPane.showOptionDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tvshow.delete.desc"),
        TmmResourceBundle.getString("tvshow.delete"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
    if (answer != JOptionPane.YES_OPTION) {
      return;
    }

    if (selectedObjects.isLockedFound()) {
      TvShowSelectionModel.showLockedInformation();
    }

    TmmTaskManager.getInstance().addUnnamedTask(() -> {
      for (TvShow tvShow : selectedObjects.getTvShows()) {
        TvShowModuleManager.getInstance().getTvShowList().deleteTvShow(tvShow);
      }

      for (TvShowSeason season : selectedObjects.getSeasons()) {
        for (TvShowEpisode episode : season.getEpisodes()) {
          season.getTvShow().deleteEpisode(episode);
        }
      }

      for (TvShowEpisode episode : selectedObjects.getEpisodes()) {
        episode.getTvShow().deleteEpisode(episode);
      }
    });
  }
}
