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
package org.tinymediamanager.ui.tvshows.panels.tvshow;

import static org.tinymediamanager.core.Constants.MEDIA_FILES;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JPanel;

import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.panels.ImagePanel;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;

import net.miginfocom.swing.MigLayout;

/**
 * The class TvShowArtworkPanel. To display all artwork from a TV show in the UI
 * 
 * @author Manuel Laggner
 */
public class TvShowArtworkPanel extends JPanel {
  private final List<MediaFile> mediaFiles;
  private ImagePanel            imagePanel;

  public TvShowArtworkPanel(final TvShowSelectionModel selectionModel) {
    mediaFiles = new ArrayList<>();

    initComponents();

    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();

      if (source.getClass() != TvShowSelectionModel.class) {
        return;
      }
      if ("selectedTvShow".equals(property) || MEDIA_FILES.equals(property)) {
        synchronized (mediaFiles) {
          mediaFiles.clear();
          // TV show
          mediaFiles.addAll(selectionModel.getSelectedTvShow().getMediaFiles().stream().filter(MediaFile::isGraphic).toList());

          // season
          for (TvShowSeason season : selectionModel.getSelectedTvShow().getSeasons()) {
            mediaFiles.addAll(season.getMediaFiles().stream().filter(MediaFile::isGraphic).toList());
          }

          // sort them
          mediaFiles.sort(Comparator.comparing(MediaFile::getType));

          imagePanel.rebuildPanel();
        }
      }
    };
    selectionModel.addPropertyChangeListener(propertyChangeListener);
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[grow]", "[grow]"));
    {
      imagePanel = new ImagePanel(mediaFiles);

      imagePanel.setMaxWidth(500);
      imagePanel.setMaxHeight(200);
      add(imagePanel, "cell 0 0,grow");
    }
  }
}
