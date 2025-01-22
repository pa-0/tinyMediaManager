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
package org.tinymediamanager.ui.movies.panels;

import java.beans.PropertyChangeListener;

import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.movie.MovieHelpers;
import org.tinymediamanager.scraper.imdb.ImdbMovieTrailerProvider;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.movies.MovieSelectionModel;
import org.tinymediamanager.ui.panels.TrailerPanel;

/**
 * the class {@link MovieTrailerPanel} is used to show all trailers for movies
 * 
 * @author Manuel Laggner
 */
public class MovieTrailerPanel extends TrailerPanel {

  private final MovieSelectionModel movieSelectionModel;

  public MovieTrailerPanel(MovieSelectionModel model) {
    this.movieSelectionModel = model;

    createLayout();

    table.setName(getName() + ".table");
    TmmUILayoutStore.getInstance().install(table);

    // install the propertychangelistener
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();

      if (source.getClass() != MovieSelectionModel.class) {
        return;
      }

      // react on selection of a movie and change of a trailer
      if ("selectedMovie".equals(property) || "trailer".equals(property)) {
        // this does sometimes not work. simply wrap it
        try {
          trailerEventList.getReadWriteLock().writeLock().lock();
          trailerEventList.clear();
          trailerEventList.addAll(movieSelectionModel.getSelectedMovie().getTrailer());
        }
        catch (Exception ignored) {
          // ignored
        }
        finally {
          trailerEventList.getReadWriteLock().writeLock().unlock();
          table.adjustColumnPreferredWidths(7);
        }
      }
    };

    movieSelectionModel.addPropertyChangeListener(propertyChangeListener);
  }

  @Override
  public String getName() {
    return "movie.movietrailer";
  }

  @Override
  protected void downloadTrailer(MediaTrailer trailer) {
    MovieHelpers.downloadTrailer(movieSelectionModel.getSelectedMovie(), trailer);
  }

  @Override
  protected String refreshUrlFromId(MediaTrailer trailer) {
    String url = trailer.getUrl();
    if (!url.startsWith("http")) {
      // we have an ID - lets check if it is a known one:
      String id = trailer.getId();
      if (!id.matches("vi\\d+")) { // IMDB
        return "";
      }

      // IMDB trailer ID
      ImdbMovieTrailerProvider tp = new ImdbMovieTrailerProvider();
      url = tp.getUrlForId(trailer);
      if (url.isEmpty()) {
        return "";
      }
    }
    return url;
  }
}
