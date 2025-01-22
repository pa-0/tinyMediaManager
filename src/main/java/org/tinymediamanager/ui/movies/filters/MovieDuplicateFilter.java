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

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a duplicate movie filter
 * 
 * @author Manuel Laggner
 */
public class MovieDuplicateFilter extends AbstractMovieUIFilter {
  private final MovieList movieList = MovieModuleManager.getInstance().getMovieList();

  @Override
  protected void filterChanged() {
    movieList.searchDuplicates();
    super.filterChanged();
  }

  @Override
  public boolean accept(Movie movie) {
    return movie.isDuplicate();
  }

  @Override
  public String getId() {
    return "movieDuplicate";
  }

  @Override
  public String getFilterValueAsString() {
    return null;
  }

  @Override
  public void setFilterValue(Object value) {
    // nothing to do
  }

  @Override
  public void clearFilter() {
    // nothing to do
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("movieextendedsearch.duplicates"));
  }

  @Override
  protected JComponent createFilterComponent() {
    return null;
  }
}
