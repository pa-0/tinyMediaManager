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
package org.tinymediamanager.ui.moviesets;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.tree.TmmTreeDataProvider;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;
import org.tinymediamanager.ui.components.treetable.AbstractTmmTreeTableNodeComparator;
import org.tinymediamanager.ui.components.treetable.TmmTreeTableFormat;

/**
 * The class MovieSetTreeDataProvider is used for providing and managing the data for the movie set tree
 * 
 * @author Manuel Laggner
 */
public class MovieSetTreeDataProvider extends TmmTreeDataProvider<TmmTreeNode> {
  private final TmmTreeNode                     root      = new TmmTreeNode(new Object(), this);
  private final TmmTreeTableFormat<TmmTreeNode> tableFormat;

  private final PropertyChangeListener          movieSetPropertyChangeListener;
  private final PropertyChangeListener          moviePropertyChangeListener;

  private final MovieList                       movieList = MovieModuleManager.getInstance().getMovieList();

  public MovieSetTreeDataProvider(TmmTreeTableFormat<TmmTreeNode> tableFormat) {
    this.tableFormat = tableFormat;

    PropertyChangeListener movielistPropertyChangeListener = evt -> {
      MovieSet movieSet;

      switch (evt.getPropertyName()) {
        case Constants.ADDED_MOVIE_SET:
          movieSet = (MovieSet) evt.getNewValue();
          addMovieSet(movieSet);
          break;

        case Constants.REMOVED_MOVIE_SET:
          movieSet = (MovieSet) evt.getNewValue();
          removeMovieSet(movieSet);
          break;

        default:
          nodeChanged(evt.getSource());
          break;
      }
    };
    movieList.addPropertyChangeListener(movielistPropertyChangeListener);

    movieSetPropertyChangeListener = evt -> {
      Movie movie;

      switch (evt.getPropertyName()) {
        case Constants.ADDED_MOVIE:
          movie = (Movie) evt.getNewValue();
          addMovie(movie);
          break;

        case Constants.REMOVED_MOVIE:
          movie = (Movie) evt.getNewValue();
          removeMovie(movie);
          break;

        case "dummyMovies":
          mixinDummyMovies((MovieSet) evt.getSource());
          break;

        default:
          nodeChanged(evt.getSource());
          break;
      }
    };

    moviePropertyChangeListener = evt -> {
      switch (evt.getPropertyName()) {
        default:
          nodeChanged(evt.getSource());
          break;
      }
    };

    setTreeComparator(new MovieSetTreeNodeComparator());

    MovieModuleManager.getInstance().getSettings().addPropertyChangeListener(evt -> {
      switch (evt.getPropertyName()) {
        case "displayMovieSetMissingMovies":
          mixinDummyMovies();
          break;
      }
    });
  }

  /**
   * trigger a node changed event for all other events
   * 
   * @param source
   */
  private void nodeChanged(Object source) {
    TmmTreeNode node = getNodeFromCache(source);
    if (node != null) {
      firePropertyChange(NODE_CHANGED, null, node);
    }
  }

  @Override
  public TmmTreeNode getRoot() {
    return root;
  }

  @Override
  public TmmTreeNode getParent(TmmTreeNode child) {
    if (child.getUserObject() instanceof MovieSet) {
      return root;
    }
    else if (child.getUserObject() instanceof Movie) {
      Movie movie = (Movie) child.getUserObject();
      TmmTreeNode node = getNodeFromCache(movie.getMovieSet());
      // parent movie set not yet added? add it
      if (node == null && movie.getMovieSet() != null) {
        node = addMovieSet(movie.getMovieSet());
      }
      return node;
    }
    return null;
  }

  @Override
  public List<TmmTreeNode> getChildren(TmmTreeNode parent) {
    if (parent == root) {
      ArrayList<TmmTreeNode> nodes = new ArrayList<>();
      for (MovieSet movieSet : new ArrayList<>(movieList.getMovieSetList())) {
        TmmTreeNode node = new MovieSetTreeNode(movieSet, this);
        putNodeToCache(movieSet, node);
        nodes.add(node);

        // add a propertychangelistener for this movie set
        movieSet.addPropertyChangeListener(movieSetPropertyChangeListener);
      }
      return nodes;
    }
    else if (parent.getUserObject() instanceof MovieSet) {
      MovieSet movieSet = (MovieSet) parent.getUserObject();
      ArrayList<TmmTreeNode> nodes = new ArrayList<>();
      for (Movie movie : movieSet.getMoviesForDisplay()) {
        if (movie.getMovieSet() == movieSet) {
          // cross check if that movie is also in the movie set
          TmmTreeNode node = new MovieTreeNode(movie, this);
          putNodeToCache(movie, node);
          nodes.add(node);
        }
        else if (movie instanceof MovieSet.MovieSetMovie) {
          // also add dummy movies
          TmmTreeNode node = new MovieTreeNode(movie, this);
          putNodeToCache(movie, node);
          nodes.add(node);
        }
      }
      return nodes;
    }
    return null;
  }

  @Override
  public boolean isLeaf(TmmTreeNode node) {
    return node.getUserObject() instanceof Movie;
  }

  private TmmTreeNode addMovieSet(MovieSet movieSet) {
    // check if this movie set has already been added
    TmmTreeNode cachedNode = getNodeFromCache(movieSet);
    if (cachedNode != null) {
      return cachedNode;
    }

    // add a new node
    TmmTreeNode node = new MovieSetTreeNode(movieSet, this);
    putNodeToCache(movieSet, node);
    firePropertyChange(NODE_INSERTED, null, node);

    // and also add a propertychangelistener to react on changes inside the movie set
    movieSet.addPropertyChangeListener(movieSetPropertyChangeListener);

    // and also add all existing movies
    for (Movie movie : movieSet.getMovies()) {
      addMovie(movie);
    }
    return node;
  }

  private void removeMovieSet(MovieSet movieSet) {
    TmmTreeNode cachedNode = removeNodeFromCache(movieSet);
    if (cachedNode == null) {
      return;
    }

    // remove all children from the map (the nodes will be removed by the treemodel)
    for (Movie movie : movieSet.getMovies()) {
      removeNodeFromCache(movie);
    }

    // remove the propertychangelistener from this movie set
    movieSet.removePropertyChangeListener(movieSetPropertyChangeListener);

    firePropertyChange(NODE_REMOVED, null, cachedNode);
  }

  private TmmTreeNode addMovie(Movie movie) {
    // check if this movie has already been added
    TmmTreeNode cachedNode = getNodeFromCache(movie);
    if (cachedNode != null) {
      // cross check if the parent (movie set) has the same node
      TmmTreeNode parent = getNodeFromCache(movie.getMovieSet());
      if (parent == cachedNode.getParent()) {
        return cachedNode;
      }
      else {
        removeNodeFromCache(movie);
      }
    }

    // add a new node
    TmmTreeNode node = new MovieTreeNode(movie, this);
    putNodeToCache(movie, node);
    firePropertyChange(NODE_INSERTED, null, node);

    // and also add a propertychangelistener to react on changes inside the movie
    movie.addPropertyChangeListener(moviePropertyChangeListener);

    return node;
  }

  private void removeMovie(Movie movie) {
    TmmTreeNode cachedNode = removeNodeFromCache(movie);
    if (cachedNode == null) {
      return;
    }

    // remove the propertychangelistener from this episode
    movie.removePropertyChangeListener(moviePropertyChangeListener);

    firePropertyChange(NODE_REMOVED, null, cachedNode);
  }

  private void mixinDummyMovies() {
    for (MovieSet movieSet : movieList.getMovieSetList()) {
      mixinDummyMovies(movieSet);
    }
  }

  private void mixinDummyMovies(MovieSet movieSet) {
    TmmTreeNode movieSetNode = getNodeFromCache(movieSet);
    if (movieSetNode == null) {
      return;
    }

    List<Movie> movies = movieSet.getMoviesForDisplay();

    // a) check if there is a (dummy) movie in the tree which has to be removed
    List<TmmTreeNode> nodesToRemove = new ArrayList<>();
    for (int i = 0; i < movieSetNode.getChildCount(); i++) {
      TmmTreeNode child = (TmmTreeNode) movieSetNode.getChildAt(i);
      if (!movies.contains(child.getUserObject())) {
        nodesToRemove.add(child);
      }
    }
    for (TmmTreeNode node : nodesToRemove) {
      if (node.getUserObject() instanceof Movie) {
        removeMovie((Movie) node.getUserObject());
      }
    }

    // b) mixin new movies
    for (Movie movie : movies) {
      addMovie(movie);
    }
  }

  /*
   * helper classes
   */
  class MovieSetTreeNodeComparator extends AbstractTmmTreeTableNodeComparator {
    private final Comparator stringComparator;

    private Comparator       sortComparator;

    private MovieSetTreeNodeComparator() {
      stringComparator = new TmmTableFormat.StringComparator();

      // initialize the comparator with comparing the title ascending
      sortColumn = 0;
      sortDirection = SortDirection.ASCENDING;
      sortComparator = getSortComparator();
    }

    @Override
    public int compare(TmmTreeNode o1, TmmTreeNode o2) {
      Object userObject1 = o1.getUserObject();
      Object userObject2 = o2.getUserObject();

      if (userObject1 instanceof MovieSet && userObject2 instanceof MovieSet) {
        int comparingResult = sortComparator.compare(getColumnValue(o1, sortColumn), getColumnValue(o2, sortColumn));
        if (comparingResult == 0 && sortColumn != 0) {
          comparingResult = stringComparator.compare(getColumnValue(o1, 0), getColumnValue(o2, 0));
        }
        else {
          if (sortDirection == SortDirection.DESCENDING) {
            comparingResult = comparingResult * -1;
          }
        }
        return comparingResult;
      }

      if (userObject1 instanceof Movie && userObject2 instanceof Movie) {
        Movie movie1 = (Movie) userObject1;
        Movie movie2 = (Movie) userObject2;
        if (movie1.getMovieSet() != null && movie1.getMovieSet() == movie2.getMovieSet()) {
          List<Movie> moviesInSet = movie1.getMovieSet().getMoviesForDisplay();
          return moviesInSet.indexOf(movie1) - moviesInSet.indexOf(movie2);
        }
      }

      return o1.toString().compareToIgnoreCase(o2.toString());
    }

    @Override
    public void columnClicked(int column, boolean shift, boolean control) {
      if (sortColumn == column) {
        if (sortDirection == SortDirection.ASCENDING) {
          sortDirection = SortDirection.DESCENDING;
        }
        else {
          sortDirection = SortDirection.ASCENDING;
        }
      }
      else {
        sortDirection = SortDirection.ASCENDING;
      }
      sortColumn = column;

      sortComparator = getSortComparator();
    }

    @Override
    public void fromString(String stringEncoded) {
      super.fromString(stringEncoded);
      sortComparator = getSortComparator();
    }

    private Comparator<?> getSortComparator() {
      if (sortColumn == 0) {
        // sort on the node/title
        return stringComparator;
      }
      else {
        return tableFormat.getColumnComparator(sortColumn - 1);
      }
    }

    private Object getColumnValue(TmmTreeNode treeNode, int i) {
      if (i == 0) {
        return ((MovieSet) treeNode.getUserObject()).getTitleSortable();
      }
      return tableFormat.getColumnValue(treeNode, i - 1);
    }

    public SortDirection getSortDirection(int sortColumn) {
      if (sortColumn == this.sortColumn) {
        return sortDirection;
      }

      return null;
    }
  }

  public static class MovieSetTreeNode extends TmmTreeNode {
    public MovieSetTreeNode(Object userObject, TmmTreeDataProvider dataProvider) {
      super(userObject, dataProvider);
    }

    /**
     * provides the right name of the node for display.
     * 
     * @return the string
     */
    @Override
    public String toString() {
      // return movieSet name
      if (getUserObject() instanceof MovieSet) {
        MovieSet MovieSet = (MovieSet) getUserObject();
        return MovieSet.getTitleSortable();
      }

      // fallback: call super
      return super.toString();
    }
  }

  static class MovieTreeNode extends TmmTreeNode {
    public MovieTreeNode(Object userObject, TmmTreeDataProvider dataProvider) {
      super(userObject, dataProvider);
    }

    /**
     * provides the right name of the node for display
     */
    @Override
    public String toString() {
      // return movieSet name
      if (getUserObject() instanceof Movie) {
        Movie movie = (Movie) getUserObject();
        return movie.getTitleSortable();
      }

      // fallback: call super
      return super.toString();
    }
  }
}
