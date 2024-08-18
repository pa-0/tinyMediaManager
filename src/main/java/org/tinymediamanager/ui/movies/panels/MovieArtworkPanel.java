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
package org.tinymediamanager.ui.movies.panels;

import static org.tinymediamanager.core.Constants.MEDIA_FILES;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JPanel;

import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.ui.movies.MovieSelectionModel;
import org.tinymediamanager.ui.panels.ImagePanel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link MovieArtworkPanel}. To display all artwork from a movie in the UI
 * 
 * @author Manuel Laggner
 */
public class MovieArtworkPanel extends JPanel {
  private final List<MediaFile> mediaFiles;
  private ImagePanel            imagePanel;

  public MovieArtworkPanel(final MovieSelectionModel selectionModel) {
    mediaFiles = new ArrayList<>();

    initComponents();

    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();

      if (source.getClass() != MovieSelectionModel.class) {
        return;
      }

      if ("selectedMovie".equals(property) || MEDIA_FILES.equals(property)) {
        synchronized (mediaFiles) {
          mediaFiles.clear();

          mediaFiles.addAll(selectionModel.getSelectedMovie().getMediaFiles().stream().filter(MediaFile::isGraphic).toList());
          // sort them
          mediaFiles.sort(Comparator.comparing(MediaFile::getType));

          imagePanel.rebuildPanel();
        }
      }
    };
    selectionModel.addPropertyChangeListener(propertyChangeListener);
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[400lp,grow]", "[300lp:400lp,grow]"));

    {
      imagePanel = new ImagePanel(mediaFiles);
      imagePanel.setMaxWidth(400);
      imagePanel.setMaxHeight(200);
      add(imagePanel, "cell 0 0,grow");
    }
  }
}
