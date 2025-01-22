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
package org.tinymediamanager.ui.tvshows.panels.tvshow;

import java.beans.PropertyChangeListener;

import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.scraper.imdb.ImdbTvShowTrailerProvider;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.panels.TrailerPanel;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;

/**
 * the class {@link TvShowTrailerPanel} is used to show trailers for TV shows
 * 
 * @author Manuel Laggner
 */
public class TvShowTrailerPanel extends TrailerPanel {

  private final TvShowSelectionModel tvShowSelectionModel;

  public TvShowTrailerPanel(TvShowSelectionModel model) {
    this.tvShowSelectionModel = model;

    createLayout();

    table.setName("movies.trailerTable");
    TmmUILayoutStore.getInstance().install(table);

    // install the propertychangelistener
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();

      if (source.getClass() != TvShowSelectionModel.class) {
        return;
      }

      // react on selection of a movie and change of a trailer
      if ("selectedTvShow".equals(property) || "trailer".equals(property)) {
        // this does sometimes not work. simply wrap it
        try {
          trailerEventList.getReadWriteLock().writeLock().lock();
          trailerEventList.clear();
          trailerEventList.addAll(tvShowSelectionModel.getSelectedTvShow().getTrailer());
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

    tvShowSelectionModel.addPropertyChangeListener(propertyChangeListener);
  }

  @Override
  protected void downloadTrailer(MediaTrailer trailer) {
    TvShowHelpers.downloadTrailer(tvShowSelectionModel.getSelectedTvShow(), trailer);
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
      ImdbTvShowTrailerProvider tp = new ImdbTvShowTrailerProvider();
      url = tp.getUrlForId(trailer);
      if (url.isEmpty()) {
        return "";
      }
    }
    return url;
  }
}
