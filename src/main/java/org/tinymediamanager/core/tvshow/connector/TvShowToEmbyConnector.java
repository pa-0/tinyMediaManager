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
package org.tinymediamanager.core.tvshow.connector;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.w3c.dom.Element;

/**
 * the class TvShowToKodiConnector is used to write a most recent Emby compatible NFO file
 *
 * @author Manuel Laggner
 */
public class TvShowToEmbyConnector extends TvShowToKodiConnector {
  public TvShowToEmbyConnector(TvShow tvShow) {
    super(tvShow);
  }

  @Override
  protected void addOwnTags() {
    super.addOwnTags();
    addEnddate();
  }

  /**
   * write the <enddate> tag for Emby<br />
   * This will only be set if the status of the TV show is ENDED
   */
  protected void addEnddate() {
    if (!TvShowModuleManager.getInstance().getSettings().isNfoWriteDateEnded() || tvShow.getStatus() != MediaAiredStatus.ENDED) {
      return;
    }

    Date latestAiredDate = null;

    for (TvShowEpisode episode : tvShow.getEpisodes()) {
      if (episode.getFirstAired() != null && (latestAiredDate == null || latestAiredDate.before(episode.getFirstAired()))) {
        latestAiredDate = episode.getFirstAired();
      }
    }

    for (TvShowEpisode episode : tvShow.getDummyEpisodes()) {
      if (episode.getFirstAired() != null && (latestAiredDate == null || latestAiredDate.before(episode.getFirstAired()))) {
        latestAiredDate = episode.getFirstAired();
      }
    }

    if (latestAiredDate != null) {
      Element enddate = document.createElement("enddate");
      enddate.setTextContent(new SimpleDateFormat("yyyy-MM-dd").format(latestAiredDate));
      root.appendChild(enddate);
    }
  }
}
