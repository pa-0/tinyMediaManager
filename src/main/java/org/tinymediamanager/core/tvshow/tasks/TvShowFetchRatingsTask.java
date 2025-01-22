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
package org.tinymediamanager.core.tvshow.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.rating.RatingProvider;

/**
 * The class {@link TvShowFetchRatingsTask} is used to fetch ratings from the rating providers
 * 
 * @author Manuel Laggner
 */
public class TvShowFetchRatingsTask extends TmmTask {
  private static final Logger                     LOGGER = LoggerFactory.getLogger(TvShowFetchRatingsTask.class);

  private final List<TvShow>                      tvShows;
  private final List<TvShowEpisode>               episodes;
  private final List<RatingProvider.RatingSource> sources;

  public TvShowFetchRatingsTask(Collection<TvShow> tvShows, Collection<TvShowEpisode> episodes, Collection<RatingProvider.RatingSource> sources) {
    super(TmmResourceBundle.getString("tvshow.fetchratings"), tvShows.size() + episodes.size(), TaskType.BACKGROUND_TASK);
    this.tvShows = new ArrayList<>(tvShows);
    this.episodes = new ArrayList<>(episodes);
    this.sources = new ArrayList<>(sources);
  }

  @Override
  protected void doInBackground() {
    LOGGER.debug("fetching ratings...");
    int i = 0;

    // TV shows
    for (TvShow tvShow : tvShows) {
      Map<String, Object> ids = new HashMap<>(tvShow.getIds());
      ids.put(MediaMetadata.TVSHOW_IDS, tvShow.getIds());
      List<MediaRating> ratings = RatingProvider.getRatings(ids, sources, MediaType.TV_SHOW);
      ratings.forEach(tvShow::setRating);
      if (!ratings.isEmpty()) {
        tvShow.saveToDb();
        tvShow.writeNFO();
      }

      publishState(++i);
      if (cancel) {
        break;
      }
    }

    // episodes
    for (TvShowEpisode episode : episodes) {
      Map<String, Object> ids = new HashMap<>(episode.getIds());
      ids.put(MediaMetadata.TVSHOW_IDS, episode.getTvShow().getIds());
      ids.put(MediaMetadata.SEASON_NR, episode.getSeason());
      ids.put(MediaMetadata.EPISODE_NR, episode.getEpisode());
      List<MediaRating> ratings = RatingProvider.getRatings(ids, sources, MediaType.TV_EPISODE);
      ratings.forEach(episode::setRating);
      if (!ratings.isEmpty()) {
        episode.saveToDb();
        episode.writeNFO();
      }

      publishState(++i);
      if (cancel) {
        break;
      }
    }
  }
}
