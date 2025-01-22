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
package org.tinymediamanager.ui.tvshows.panels.season;

import static org.tinymediamanager.core.Constants.MEDIA_FILES;

import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.panels.MediaFilesPanel;
import org.tinymediamanager.ui.tvshows.TvShowSeasonSelectionModel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * the class TvShowSeasonMediaFilesPanel. to display all episodes belonging to a season
 * 
 * @author Manuel Laggner
 */
public class TvShowSeasonMediaFilesPanel extends JPanel {
  private final EventList<MediaFile> mediaFileEventList;
  private MediaFilesPanel            panelMediaFiles;

  public TvShowSeasonMediaFilesPanel(final TvShowSeasonSelectionModel selectionModel) {
    mediaFileEventList = GlazedListsSwing.swingThreadProxyList(
        new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()), GlazedLists.beanConnector(MediaFile.class)));

    initComponents();
    panelMediaFiles.installTmmUILayoutStore("tvshows.season");

    // manual coded binding
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();
      if (source instanceof TvShowSeasonSelectionModel || (source instanceof TvShowSeason && (MEDIA_FILES.equals(property)))) {
        TvShowSeason selectedSeason;
        if (source instanceof TvShowSeasonSelectionModel model) {
          selectedSeason = model.getSelectedTvShowSeason();
        }
        else {
          selectedSeason = (TvShowSeason) source;
        }
        try {
          mediaFileEventList.getReadWriteLock().writeLock().lock();
          mediaFileEventList.clear();
          mediaFileEventList.addAll(selectedSeason.getMediaFilesRecursive());
        }
        catch (Exception ignored) {
          // ignored
        }
        finally {
          mediaFileEventList.getReadWriteLock().writeLock().unlock();
          panelMediaFiles.adjustColumns();
        }
      }
    };
    selectionModel.addPropertyChangeListener(propertyChangeListener);
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[grow]", "[grow]"));
    {
      panelMediaFiles = new MediaFilesPanel(mediaFileEventList);
      add(panelMediaFiles, "cell 0 0,grow");
    }
  }
}
