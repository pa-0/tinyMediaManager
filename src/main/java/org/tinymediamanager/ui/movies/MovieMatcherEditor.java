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
package org.tinymediamanager.ui.movies;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.ITmmUIFilter;
import org.tinymediamanager.ui.movies.filters.IMovieUIFilter;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * The Class MovieMatcherEditor.
 * 
 * @author Manuel Laggner
 */
public class MovieMatcherEditor extends AbstractMatcherEditor<Movie> {
  private final Set<IMovieUIFilter>    filters;
  private final PropertyChangeListener filterChangeListener;
  private boolean                      filtersActive = true;

  /**
   * Instantiates a new movie matcher editor.
   */
  public MovieMatcherEditor() {
    filters = new HashSet<>();
    filterChangeListener = evt -> updateFiltering();
  }

  /**
   * Add a new UI filter to this matcher
   * 
   * @param filter
   *          the new filter to be added
   */
  public void addFilter(IMovieUIFilter filter) {
    filter.addPropertyChangeListener(filterChangeListener);
    filters.add(filter);
  }

  /**
   * set any stored filter values
   *
   * @param values
   *          the values to be set
   */
  public void setFilterValues(List<AbstractSettings.UIFilters> values) {
    for (IMovieUIFilter filter : filters) {
      AbstractSettings.UIFilters uiFilters = values.stream().filter(uiFilter -> uiFilter.id.equals(filter.getId())).findFirst().orElse(null);

      if (uiFilters != null) {
        filter.setFilterState(uiFilters.state);
        filter.setFilterValue(uiFilters.filterValue);
        filter.setFilterOption(uiFilters.option);
      }
      else {
        filter.setFilterState(ITmmUIFilter.FilterState.INACTIVE);
      }
    }

    updateFiltering();
  }

  /**
   * set whether all filters are active or not
   * 
   * @param filtersActive
   *          true if all filters should be active; false otherwise
   */
  public void setFiltersActive(boolean filtersActive) {
    this.filtersActive = filtersActive;
    updateFiltering();
  }

  /**
   * check whether all filters are active or not
   *
   * @return true if not all filters are deaktivates
   */
  public boolean isFiltersActive() {
    return filtersActive;
  }

  /**
   * clear any set filters
   */
  public void clearFilters() {
    for (IMovieUIFilter filter : filters) {
      filter.setFilterState(ITmmUIFilter.FilterState.INACTIVE);
      filter.clearFilter();
    }

    updateFiltering();
  }

  /**
   * re-filter the list
   */
  private void updateFiltering() {
    SwingUtilities.invokeLater(() -> {
      Matcher<Movie> matcher;
      if (filtersActive) {
        matcher = new MovieMatcher(new HashSet<>(filters));
      }
      else {
        matcher = new MovieMatcher(Collections.emptySet());
      }
      fireChanged(matcher);
    });

    if (MovieModuleManager.getInstance().getSettings().isStoreUiFilters()) {
      List<AbstractSettings.UIFilters> filterValues = new ArrayList<>();
      if (filtersActive) {
        for (IMovieUIFilter filter : filters) {
          if (filter.getFilterState() != ITmmUIFilter.FilterState.INACTIVE) {
            AbstractSettings.UIFilters uiFilters = new AbstractSettings.UIFilters();
            uiFilters.id = filter.getId();
            uiFilters.state = filter.getFilterState();
            uiFilters.filterValue = filter.getFilterValueAsString();
            filterValues.add(uiFilters);
          }
        }
      }
      MovieModuleManager.getInstance().getSettings().setUiFilters(filterValues);
      MovieModuleManager.getInstance().getSettings().saveSettings();
    }
  }

  /**
   * get all filters
   * 
   * @return a {@link Set<IMovieUIFilter>} of all filters
   */
  public Set<IMovieUIFilter> getFilters() {
    return filters;
  }

  /*
   * helper class for running all filters against the given movie
   */
  private static class MovieMatcher implements Matcher<Movie> {
    private final Set<IMovieUIFilter> filters;

    public MovieMatcher(Set<IMovieUIFilter> filters) {
      this.filters = filters;
    }

    @Override
    public boolean matches(Movie movie) {
      for (IMovieUIFilter filter : filters) {
        switch (filter.getFilterState()) {
          case ACTIVE:
            if (!filter.accept(movie)) {
              return false;
            }
            break;

          case ACTIVE_NEGATIVE:
            if (filter.accept(movie)) {
              return false;
            }
            break;

          default:
            break;
        }
      }

      return true;
    }
  }
}
