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
package org.tinymediamanager.ui.components;

import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.table.TmmEditorTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.panels.IModalPopupPanelProvider;
import org.tinymediamanager.ui.panels.IdEditorPanel;
import org.tinymediamanager.ui.panels.ModalPopupPanel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

/**
 * The class MediaIdTable is used to display / edit media ids
 *
 * @author Manuel Laggner
 */
public class MediaIdTable extends TmmEditorTable {
  private final EventList<MediaId> idList;
  private final ScraperType        scraperType;

  public MediaIdTable(EventList<MediaId> idList) {
    this(idList, null);
  }

  public MediaIdTable(EventList<MediaId> ids, ScraperType type) {
    super();

    this.idList = ids;
    this.scraperType = type;

    setModel(new TmmTableModel<>(idList, new MediaIdTableFormat()));

    setTableHeader(null);
  }

  @NotNull
  public static EventList<MediaId> convertIdMapToEventList(@NotNull Map<String, Object> idMap) {
    EventList<MediaId> idList = new BasicEventList<>();
    for (Entry<String, Object> entry : idMap.entrySet()) {
      MediaId id = new MediaId();
      id.key = entry.getKey();
      try {
        id.value = entry.getValue().toString();
      }
      catch (Exception e) {
        id.value = "";
      }
      idList.add(id);
    }

    return idList;
  }

  protected void editButtonClicked(int row) {
    IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(MediaIdTable.this);
    if (iModalPopupPanelProvider == null) {
      return;
    }

    int index = convertRowIndexToModel(row);
    MediaId mediaId = idList.get(index);

    ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
    popupPanel.setTitle(TmmResourceBundle.getString("rating.edit"));

    IdEditorPanel idEditorPanel = new IdEditorPanel(mediaId, scraperType);
    popupPanel.setContent(idEditorPanel);
    iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
  }

  /**
   * helper classes
   */
  public static class MediaId {
    public String key;
    public String value;

    public MediaId() {
    }

    public MediaId(String key) {
      this.key = key;
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(19, 31).append(key).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof MediaId other)) {
        return false;
      }

      if (obj == this) {
        return true;
      }

      return StringUtils.equals(key, other.key);
    }
  }

  private static class MediaIdTableFormat extends TmmTableFormat<MediaId> {

    public MediaIdTableFormat() {
      /*
       * key
       */
      Column col = new Column("", "key", mediaId -> mediaId.key, String.class);
      addColumn(col);

      /*
       * value
       */
      col = new Column("", "value", mediaId -> mediaId.value, String.class);
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
