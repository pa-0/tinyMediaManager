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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.tasks.TvShowMissingArtworkDownloadTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowScrapeMetadataDialog;

/**
 * the class TvShowDownloadMissingArtworkAction is used to search/download missing artwork
 */
public class TvShowDownloadMissingArtworkAction extends TmmAction {
  public TvShowDownloadMissingArtworkAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.downloadmissingartwork"));
    putValue(SMALL_ICON, IconManager.IMAGE);
    putValue(LARGE_ICON_KEY, IconManager.IMAGE);
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK));
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

    TvShowScrapeMetadataDialog dialog = TvShowScrapeMetadataDialog
        .createArtworkScrapeDialog(TmmResourceBundle.getString("tvshow.downloadmissingartwork"));
    dialog.setVisible(true);

    if (!dialog.shouldStartScrape()) {
      return;
    }

    // get options from dialog
    TvShowSearchAndScrapeOptions options = dialog.getTvShowSearchAndScrapeOptions();
    List<TvShowScraperMetadataConfig> tvShowScraperMetadataConfig = dialog.getTvShowScraperMetadataConfig();
    List<TvShowEpisodeScraperMetadataConfig> episodeScraperMetadataConfig = dialog.getTvShowEpisodeScraperMetadataConfig();

    TvShowMissingArtworkDownloadTask task = new TvShowMissingArtworkDownloadTask(selectedObjects.getTvShows(), selectedObjects.getSeasons(),
        selectedObjects.getEpisodesRecursive(), options, tvShowScraperMetadataConfig, episodeScraperMetadataConfig);
    TmmTaskManager.getInstance().addDownloadTask(task);
  }
}
