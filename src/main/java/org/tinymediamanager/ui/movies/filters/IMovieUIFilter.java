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
package org.tinymediamanager.ui.movies.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.AbstractSettings.UIFilters;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.ITmmUIFilter;

/**
 * The interface IMovieUIFilter is used for filtering movies in the JTable
 * 
 * @author Manuel Laggner
 */
public interface IMovieUIFilter extends ITmmUIFilter<Movie> {

  /**
   * is the given accepted by the filter
   * 
   * @param movie
   *          the movie to check
   * @return true or false
   */
  boolean accept(Movie movie);

  /**
   * morph the given {@link IMovieUIFilter}s with a state != {@link org.tinymediamanager.ui.ITmmUIFilter.FilterState#INACTIVE} to the storage form
   * {@link AbstractSettings.UIFilters}
   * 
   * @param movieUiFilters
   *          the {@link IMovieUIFilter}s to morph
   * @return a {@link List} of all morphed {@link AbstractSettings.UIFilters}
   */
  static List<UIFilters> morphToUiFilters(Collection<IMovieUIFilter> movieUiFilters) {
    List<UIFilters> uiFilters = new ArrayList<>();

    movieUiFilters.forEach(filter -> {
      if (filter.getFilterState() != ITmmUIFilter.FilterState.INACTIVE) {
        UIFilters uiFilter = new AbstractSettings.UIFilters();
        uiFilter.id = filter.getId();
        uiFilter.state = filter.getFilterState();
        uiFilter.option = filter.getFilterOption();
        uiFilter.filterValue = filter.getFilterValueAsString();
        uiFilters.add(uiFilter);
      }
    });

    return uiFilters;
  }
}
