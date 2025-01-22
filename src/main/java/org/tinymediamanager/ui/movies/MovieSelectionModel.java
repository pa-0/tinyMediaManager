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

import static org.tinymediamanager.ui.TmmFontHelper.L1;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.movies.filters.IMovieUIFilter;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;

/**
 * The Class MovieSelectionModel.
 * 
 * @author Manuel Laggner
 */
public class MovieSelectionModel extends AbstractModelObject implements ListSelectionListener {
  public static final String                SELECTED_MOVIE  = "selectedMovie";
  public static final String                SELECTED_MOVIES = "selectedMovies";

  public final Movie                        initialMovie    = new Movie();

  private EventList<Movie>                  selectedMovies;
  private Movie                             selectedMovie;
  private DefaultEventSelectionModel<Movie> selectionModel;
  private MovieMatcherEditor                matcherEditor;
  private SortedList<Movie>                 sortedList;
  private PropertyChangeListener            propertyChangeListener;

  /**
   * Instantiates a new movie selection model. Usage in MoviePanel
   * 
   * @param sortedList
   *          the sorted list
   * @param source
   *          the source
   * @param matcher
   *          the matcher
   */
  public MovieSelectionModel(SortedList<Movie> sortedList, EventList<Movie> source, MovieMatcherEditor matcher) {
    constructInitialMovie();

    this.sortedList = sortedList;
    this.selectionModel = new DefaultEventSelectionModel<>(source);
    this.selectionModel.addListSelectionListener(this);
    this.matcherEditor = matcher;
    this.selectedMovies = selectionModel.getSelected();

    propertyChangeListener = evt -> {
      if (evt.getSource() == selectedMovie) {
        // wrap this event in a new event for listeners of the selection model
        firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
      }
      if (evt.getSource() instanceof IMovieUIFilter) {
        firePropertyChange("filterChanged", null, evt.getSource());
      }
    };
  }

  /**
   * Instantiates a new movie selection model. Usage in MovieSetPanel
   */
  public MovieSelectionModel() {
    constructInitialMovie();
  }

  /**
   * the initial movie needs some things set
   */
  private void constructInitialMovie() {
    MediaFile fakeVideo = new MediaFile();
    fakeVideo.setType(MediaFileType.VIDEO);
    initialMovie.addToMediaFiles(fakeVideo);
  }

  /**
   * Sets the selected movie.
   * 
   * @param movie
   *          the new selected movie
   */
  public void setSelectedMovie(Movie movie) {
    Movie oldValue = this.selectedMovie;
    if (movie == null) {
      this.selectedMovie = initialMovie;
    }
    else {
      this.selectedMovie = movie;
    }

    if (oldValue != null) {
      oldValue.removePropertyChangeListener(propertyChangeListener);
    }

    if (selectedMovie != null) {
      selectedMovie.addPropertyChangeListener(propertyChangeListener);
    }

    firePropertyChange(SELECTED_MOVIE, oldValue, selectedMovie);
  }

  /**
   * Gets the matcher editor.
   * 
   * @return the matcher editor
   */
  public MovieMatcherEditor getMatcherEditor() {
    return matcherEditor;
  }

  /**
   * Gets the selection model.
   * 
   * @return the selection model
   */
  public DefaultEventSelectionModel<Movie> getSelectionModel() {
    return selectionModel;
  }

  /**
   * Value changed.
   * 
   * @param e
   *          the e
   */
  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting()) {
      return;
    }

    // display first selected movie
    if (!selectedMovies.isEmpty() && selectedMovie != selectedMovies.get(0)) {
      Movie oldValue = selectedMovie;
      selectedMovie = selectedMovies.get(0);

      // unregister propertychangelistener
      if (oldValue != null && oldValue != initialMovie) {
        oldValue.removePropertyChangeListener(propertyChangeListener);
      }
      if (selectedMovie != null && selectedMovie != initialMovie) {
        selectedMovie.addPropertyChangeListener(propertyChangeListener);
      }
      firePropertyChange(SELECTED_MOVIE, oldValue, selectedMovie);
    }

    // display empty movie (i.e. when all movies are removed from the list)
    if (selectedMovies.isEmpty()) {
      Movie oldValue = selectedMovie;
      selectedMovie = initialMovie;
      // unregister propertychangelistener
      if (oldValue != null && oldValue != initialMovie) {
        oldValue.removePropertyChangeListener(propertyChangeListener);
      }
      firePropertyChange(SELECTED_MOVIE, oldValue, selectedMovie);
    }

    firePropertyChange(SELECTED_MOVIES, null, this.selectedMovies);
  }

  /**
   * Gets the selected movie.
   * 
   * @return the selected movie
   */
  public Movie getSelectedMovie() {
    if (selectedMovie == null) {
      return initialMovie;
    }
    return selectedMovie;
  }

  /**
   * Gets the selected movies (without locked ones).
   * 
   * @return the selected movies
   */
  public List<Movie> getSelectedMovies() {
    return getSelectedMovies(false);
  }

  /**
   * Gets the selected movies.
   * 
   * @param withLocked
   *          also get locked movies
   * @return the selected movies
   */
  public List<Movie> getSelectedMovies(boolean withLocked) {
    if (withLocked) {
      return new ArrayList<>(selectedMovies);
    }

    boolean lockedFound = selectedMovies.parallelStream().anyMatch(MediaEntity::isLocked);
    if (lockedFound && Boolean.FALSE.equals(TmmProperties.getInstance().getPropertyAsBoolean("movie.hidelockedhint"))) {
      JCheckBox checkBox = new JCheckBox(TmmResourceBundle.getString("tmm.donotshowagain"));
      TmmFontHelper.changeFont(checkBox, L1);
      checkBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

      Object[] params = { TmmResourceBundle.getString("movie.lockedfound.desc"), checkBox };
      JOptionPane.showMessageDialog(MainWindow.getInstance(), params, TmmResourceBundle.getString("movie.lockedfound"),
          JOptionPane.INFORMATION_MESSAGE);

      // the user don't want to show this dialog again
      if (checkBox.isSelected()) {
        TmmProperties.getInstance().putProperty("movie.hidelockedhint", String.valueOf(checkBox.isSelected()));
      }
    }

    return selectedMovies.stream().filter(movie -> !movie.isLocked()).collect(Collectors.toList());
  }

  /**
   * Sets the selected movies.
   * 
   * @param selectedMovies
   *          the new selected movies
   */
  public void setSelectedMovies(List<Movie> selectedMovies) {
    this.selectedMovies.clear();

    if (ListUtils.isNotEmpty(selectedMovies)) {
      this.selectedMovies.addAll(selectedMovies);
    }
  }

  /**
   * Add an UI filter
   * 
   * @param filter
   *          the new filter to be added
   */
  public void addFilter(IMovieUIFilter filter) {
    matcherEditor.addFilter(filter);
    filter.addPropertyChangeListener(propertyChangeListener);
  }

  /**
   * set whether all filters are active or not
   * 
   * @param filtersActive
   *          true if all filters should be active; false otherwise
   */
  public void setFiltersActive(boolean filtersActive) {
    matcherEditor.setFiltersActive(filtersActive);
    firePropertyChange("filterChanged", null, matcherEditor.getFilters());
  }

  /**
   * check whether all filters are active or not
   * 
   * @return true if not all filters are deactivated
   */
  public boolean isFiltersActive() {
    return matcherEditor.isFiltersActive();
  }

  /**
   * set any stored filter values
   * 
   * @param values
   *          the values to be set
   */
  public void setFilterValues(List<AbstractSettings.UIFilters> values) {
    if (values == null) {
      values = Collections.emptyList();
    }
    matcherEditor.setFilterValues(values);
    firePropertyChange("filterChanged", null, values);
  }

  /**
   * clear all active filters
   */
  public void clearFilters() {
    matcherEditor.clearFilters();
    firePropertyChange("filterChanged", null, Collections.emptyList());
  }
}
