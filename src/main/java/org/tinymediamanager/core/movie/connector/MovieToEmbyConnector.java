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
package org.tinymediamanager.core.movie.connector;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.w3c.dom.Element;

/**
 * the class {@link MovieToEmbyConnector} is used to write a most recent Emby compatible NFO file
 *
 * @author Manuel Laggner
 */
public class MovieToEmbyConnector extends MovieToKodiConnector {

  public MovieToEmbyConnector(Movie movie) {
    super(movie);
  }

  /**
   * Emby stores custom lists as own sets. We need to preserve the ones _without_ a "tmdbcolid"
   */
  @Override
  protected void addSet() {
    // write the tmm set
    Element set = document.createElement("set");

    MovieSet movieSet = movie.getMovieSet();
    if (movieSet != null) {
      if (movieSet.getTmdbId() > 0) {
        set.setAttribute("tmdbcolid", String.valueOf(movieSet.getTmdbId()));
      }

      Element name = document.createElement("name");
      name.setTextContent(movie.getMovieSet().getTitle());
      set.appendChild(name);

      Element overview = document.createElement("overview");
      overview.setTextContent(movie.getMovieSet().getPlot());
      set.appendChild(overview);
    }

    root.appendChild(set);

    // mix in old sets
    if (parser != null && !parser.sets.isEmpty()) {
      for (MovieNfoParser.Set parserSet : parser.sets) {
        // check if we just added the same set as in the existing NFO
        if (movieSet != null
            && (StringUtils.equals(movieSet.getTitle(), parserSet.name) || (movieSet.getTmdbId() > 0 && movieSet.getTmdbId() == parserSet.tmdbId))) {
          // already added; either a match on title or tmdb id
          continue;
        }

        set = document.createElement("set");

        if (parserSet.tmdbId > 0) {
          set.setAttribute("tmdbcolid", String.valueOf(parserSet));
        }

        Element name = document.createElement("name");
        name.setTextContent(parserSet.name);
        set.appendChild(name);

        Element overview = document.createElement("overview");
        overview.setTextContent(parserSet.overview);
        set.appendChild(overview);

        root.appendChild(set);
      }
    }
  }
}
