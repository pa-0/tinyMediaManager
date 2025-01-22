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
package org.tinymediamanager.ui.tvshows;

import javax.swing.ImageIcon;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.table.TmmEditorTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.GlazedListsSwing;

/**
 * the class {@link EpisodeNumberTable} is used to display season/episodes in a table format
 * 
 * @author Manuel Laggner
 */
public class EpisodeNumberTable extends TmmEditorTable {
  public EpisodeNumberTable(EventList<MediaEpisodeNumber> episodeNumbers) {
    super();

    setModel(new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(episodeNumbers), new EpisodeNumberTableFormat()));
  }

  @Override
  protected void editButtonClicked(int row) {
    // to be overridden
  }

  /**
   * helper class for the table model
   */
  private static class EpisodeNumberTableFormat extends TmmTableFormat<MediaEpisodeNumber> {
    private EpisodeNumberTableFormat() {
      /*
       * episode group
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.episode.group"), "episodegroup", MediaEpisodeNumber::episodeGroup, String.class);
      addColumn(col);

      /*
       * season
       */
      col = new Column(TmmResourceBundle.getString("metatag.season"), "season", MediaEpisodeNumber::season, Integer.class);
      addColumn(col);

      /*
       * episode
       */
      col = new Column(TmmResourceBundle.getString("metatag.episode"), "episode", MediaEpisodeNumber::episode, Integer.class);
      addColumn(col);

      /*
       * edit
       */
      col = new Column(TmmResourceBundle.getString("Button.edit"), "edit", person -> IconManager.EDIT, ImageIcon.class);
      col.setColumnResizeable(false);
      col.setHeaderIcon(IconManager.EDIT_HEADER);
      addColumn(col);
    }
  }
}
