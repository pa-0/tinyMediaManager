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

package org.tinymediamanager.ui.moviesets.panels;

import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;
import static org.tinymediamanager.ui.moviesets.MovieSetSelectionModel.SELECTED_MOVIE_SET;

import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.ui.moviesets.MovieSetSelectionModel;
import org.tinymediamanager.ui.panels.MediaFilesPanel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieSetMediaInformationPanel is used to show movie set related media files (artwork).
 * 
 * @author Manuel Laggner
 */
public class MovieSetMediaInformationPanel extends JPanel {
  private final MovieSetSelectionModel selectionModel;
  private final EventList<MediaFile>   mediaFileEventList;

  private MediaFilesPanel              panelMediaFiles;

  public MovieSetMediaInformationPanel(MovieSetSelectionModel model) {
    this.selectionModel = model;
    mediaFileEventList = new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()), GlazedLists.beanConnector(MediaFile.class));

    initComponents();

    panelMediaFiles.installTmmUILayoutStore("moviesets.movieset");

    // install the propertychangelistener
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();
      // react on selection of a movie set and change of media files
      if (source.getClass() != MovieSetSelectionModel.class) {
        return;
      }

      if (SELECTED_MOVIE_SET.equals(property) || MEDIA_INFORMATION.equals(property) || MEDIA_FILES.equals(property)) {
        try {
          mediaFileEventList.getReadWriteLock().writeLock().lock();
          mediaFileEventList.clear();
          mediaFileEventList.addAll(selectionModel.getSelectedMovieSet().getMediaFiles());
        }
        catch (Exception ignored) {
          // nothing to do here
        }
        finally {
          mediaFileEventList.getReadWriteLock().writeLock().unlock();
        }
        panelMediaFiles.adjustColumns();
      }
    };

    selectionModel.addPropertyChangeListener(propertyChangeListener);
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[][150lp][grow]", "[80lp,grow]"));
    {
      panelMediaFiles = new MediaFilesPanel(mediaFileEventList);
      add(panelMediaFiles, "cell 0 0 3 1,grow");
    }
  }
}
