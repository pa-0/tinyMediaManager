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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.tasks.TvShowEpisodeScrapeTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowScrapeMetadataDialog;

/**
 * The class TvShowScrapeEpisodesAction. To Scrape episode data with the default scraper
 *
 * @author Manuel Laggner
 */
public class TvShowScrapeEpisodesAction extends TmmAction {
  public TvShowScrapeEpisodesAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshowepisode.scrape"));
    putValue(LARGE_ICON_KEY, IconManager.SEARCH);
    putValue(SMALL_ICON, IconManager.SEARCH);
  }

  @Override
  protected void processAction(ActionEvent e) {
    final List<TvShowEpisode> episodesToScrape = TvShowUIModule.getInstance().getSelectionModel().getSelectedEpisodes();

    if (episodesToScrape.isEmpty()) {
      return;
    }

    TvShowScrapeMetadataDialog dialog = TvShowScrapeMetadataDialog.createEpisodeScrapeDialog(TmmResourceBundle.getString("tvshowepisode.scrape"));
    dialog.setLocationRelativeTo(MainWindow.getInstance());
    dialog.setVisible(true);

    if (!dialog.shouldStartScrape()) {
      return;
    }

    // get options from dialog
    TvShowEpisodeSearchAndScrapeOptions options = dialog.getTvShowEpisodeSearchAndScrapeOptions();
    List<TvShowEpisodeScraperMetadataConfig> episodeScraperMetadataConfig = dialog.getTvShowEpisodeScraperMetadataConfig();
    boolean overwrite = dialog.getOverwriteExistingItems();

    // re-group the episodes. If there is a "last used" scraper set for the show also take this into account for the episode
    Map<TvShow, List<TvShowEpisode>> newEpisodes = new HashMap<>();
    for (TvShowEpisode episode : episodesToScrape) {
      List<TvShowEpisode> episodes = newEpisodes.computeIfAbsent(episode.getTvShow(), k -> new ArrayList<>());
      episodes.add(episode);
    }

    // scrape
    for (Map.Entry<TvShow, List<TvShowEpisode>> entry : newEpisodes.entrySet()) {
      TvShowEpisodeScrapeTask task = new TvShowEpisodeScrapeTask(entry.getValue(), options, episodeScraperMetadataConfig, overwrite);
      TmmTaskManager.getInstance().addUnnamedTask(task);
    }
  }
}
