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
package org.tinymediamanager.core.movie.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.rating.RatingProvider;

/**
 * The class {@link MovieFetchRatingsTask} is used to fetch ratings from the rating providers
 * 
 * @author Manuel Laggner
 */
public class MovieFetchRatingsTask extends TmmTask {
  private static final Logger                     LOGGER = LoggerFactory.getLogger(MovieFetchRatingsTask.class);

  private final List<Movie>                       movies;
  private final List<RatingProvider.RatingSource> sources;

  public MovieFetchRatingsTask(Collection<Movie> movies, Collection<RatingProvider.RatingSource> sources) {
    super(TmmResourceBundle.getString("movie.fetchratings"), movies.size(), TaskType.BACKGROUND_TASK);
    this.movies = new ArrayList<>(movies);
    this.sources = new ArrayList<>(sources);
  }

  @Override
  protected void doInBackground() {
    LOGGER.debug("fetching ratings...");
    int i = 0;

    for (Movie movie : movies) {
      List<MediaRating> ratings = RatingProvider.getRatings(movie.getIds(), sources, MediaType.MOVIE);
      ratings.forEach(movie::setRating);
      if (!ratings.isEmpty()) {
        movie.saveToDb();
        movie.writeNFO();
      }

      publishState(++i);
      if (cancel) {
        break;
      }
    }
  }
}
