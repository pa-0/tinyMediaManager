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
package org.tinymediamanager.ui.moviesets.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.ui.ITmmUIFilter;
import org.tinymediamanager.ui.components.tree.ITmmTreeFilter;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;

/**
 * The interface IMovieSetUIFilter just combines the interfaces ITmmUIFilter and ITmmTreeFilter
 * 
 * @author Manuel Laggner
 *
 * @param <E>
 */
public interface IMovieSetUIFilter<E extends TmmTreeNode> extends ITmmUIFilter<E>, ITmmTreeFilter<E> {
  /**
   * morph the given {@link IMovieSetUIFilter}s with a state != {@link org.tinymediamanager.ui.ITmmUIFilter.FilterState#INACTIVE} to the storage form
   * {@link AbstractSettings.UIFilters}
   *
   * @param movieSetUIFilters
   *          the {@link IMovieSetUIFilter}s to morph
   * @return a {@link List} of all morphed {@link AbstractSettings.UIFilters}
   */
  static List<AbstractSettings.UIFilters> morphToUiFilters(Collection<IMovieSetUIFilter<?>> movieSetUIFilters) {
    List<AbstractSettings.UIFilters> uiFilters = new ArrayList<>();

    movieSetUIFilters.forEach(filter -> {
      if (filter.getFilterState() != ITmmUIFilter.FilterState.INACTIVE) {
        AbstractSettings.UIFilters uiFilter = new AbstractSettings.UIFilters();
        uiFilter.id = filter.getId();
        uiFilter.state = filter.getFilterState();
        uiFilter.filterValue = filter.getFilterValueAsString();
        uiFilters.add(uiFilter);
      }
    });

    return uiFilters;
  }
}
