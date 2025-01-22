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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel.SelectedObjects;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * the class {@link TvShowTitleToEntityMatcher} is used to match an episode title with current (scraped!) tvshow episodes, to detect a possible S/EE
 * number
 * 
 * @author Myron Boyle
 */
public class TvShowTitleToEntityMatcher extends TmmAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowTitleToEntityMatcher.class);

  public TvShowTitleToEntityMatcher() {
    putValue(NAME, TmmResourceBundle.getString("tvshowepisode.titlematching"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshowepisode.titlematching.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    SelectedObjects sel = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects(false, false);
    if (sel.getEpisodes().isEmpty()) {
      LOGGER.warn("No episode selected");
    }

    // loop over all selected episodes
    for (TvShowEpisode ep : sel.getEpisodes()) {
      if (ep.isDummy()) {
        LOGGER.warn("Cannot operate on dummy episode '{}'", ep.getTitle());
        continue;
      }
      // loop over all episodes of TvShow to find possible dummy match
      List<TvShowEpisode> eps = ep.getTvShow()
          .getDummyEpisodes()
          .stream()
          .filter(dummy -> dummy.getTitle().equalsIgnoreCase(ep.getTitle()))
          .collect(Collectors.toList());

      // not found? try to match via releaseDate
      if (eps.size() == 0 && ep.getFirstAired() != null) {
        eps = ep.getTvShow()
            .getDummyEpisodes()
            .stream()
            .filter(dummy -> dummy.getFirstAired() != null && dummy.getFirstAired().equals(ep.getFirstAired()))
            .collect(Collectors.toList());
      }

      // MUST only match ONE named episode
      if (eps.size() == 0) {
        LOGGER.warn("Did not find an episode named '{}'", ep.getTitle());
      }
      else if (eps.size() == 1) {
        TvShowEpisode wanted = eps.get(0);
        List<TvShowEpisode> assigned = ep.getTvShow().getEpisode(wanted.getSeason(), wanted.getEpisode());
        if (assigned.isEmpty()) {
          ep.merge(wanted);
          ep.saveToDb();
          ep.writeNFO();
          ep.firePropertyChange(Constants.EPISODE, null, ep);
          LOGGER.info("Episode '{}' has been matched to S{}E{}", ep.getTitle(), ep.getSeason(), ep.getEpisode());
        }
        else {
          LOGGER.warn("Episode '{}' is already assigned - skipping", ep.getTitle());
        }
      }
      else {
        LOGGER.warn("Found multiple episodes named '{}' - skipping", ep.getTitle());
      }
    }
  }
}
