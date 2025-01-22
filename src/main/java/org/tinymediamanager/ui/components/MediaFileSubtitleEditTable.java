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

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.table.TmmEditorTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.panels.IModalPopupPanelProvider;
import org.tinymediamanager.ui.panels.MediaFileSubtitleEditorPanel;
import org.tinymediamanager.ui.panels.ModalPopupPanel;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.GlazedListsSwing;

/**
 * the class {@link MediaFileSubtitleEditTable} is used to display all subtitles in a table
 * 
 * @author Manuel Laggner
 */
public abstract class MediaFileSubtitleEditTable extends TmmEditorTable {
  private final EventList<MediaFileSubtitle> subtitleEventList;

  public MediaFileSubtitleEditTable(EventList<MediaFileSubtitle> subtitleEventList) {
    super();

    this.subtitleEventList = subtitleEventList;

    setModel(new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(subtitleEventList), new SubtitleTableFormat()));

    adjustColumnPreferredWidths(3);
  }

  @Override
  protected void editButtonClicked(int row) {
    IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(this);
    if (iModalPopupPanelProvider == null) {
      return;
    }

    MediaFileSubtitle subtitle = getSubtitle(row);

    ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
    popupPanel.setTitle(TmmResourceBundle.getString("subtitle.edit"));

    popupPanel.setOnCloseHandler(this::onEditSubtitle);

    MediaFileSubtitleEditorPanel subtitleEditorPanel = new MediaFileSubtitleEditorPanel(subtitle);
    popupPanel.setContent(subtitleEditorPanel);
    iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
  }

  public abstract void onEditSubtitle();

  /**
   * Utility to get the right {@link MediaFileSubtitle} for the given row
   *
   * @param row
   *          the row number to get the {@link MediaFileSubtitle} for
   * @return the {@link MediaFileSubtitle}
   */
  private MediaFileSubtitle getSubtitle(int row) {
    int index = convertRowIndexToModel(row);
    return subtitleEventList.get(index);
  }

  /**
   * get all selected {@link MediaFileSubtitle}s
   *
   * @return a {@link List} of all selected {@link MediaFileSubtitle}s
   */
  public List<MediaFileSubtitle> getSubtitleStreams() {
    List<MediaFileSubtitle> subtitles = new ArrayList<>();
    for (int i : getSelectedRows()) {
      MediaFileSubtitle subtitleStream = getSubtitle(i);
      if (subtitleStream != null) {
        subtitles.add(subtitleStream);
      }

    }
    return subtitles;
  }

  public void addSubtitle() {
    IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(this);
    if (iModalPopupPanelProvider == null) {
      return;
    }

    MediaFileSubtitle subtitle = new MediaFileSubtitle();

    ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
    popupPanel.setTitle(TmmResourceBundle.getString("subtitle.add"));

    popupPanel.setOnCloseHandler(() -> {
      subtitleEventList.add(subtitle);
      onAddSubtitle();
    });

    MediaFileSubtitleEditorPanel subtitleEditorPanel = new MediaFileSubtitleEditorPanel(subtitle);
    popupPanel.setContent(subtitleEditorPanel);
    iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
  }

  public abstract void onAddSubtitle();

  /**
   * helper classes
   */
  private static class SubtitleTableFormat extends TmmTableFormat<MediaFileSubtitle> {
    private SubtitleTableFormat() {
      /*
       * language
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.language"), "language", MediaFileSubtitle::getLanguage, String.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * forced
       */
      col = new Column(TmmResourceBundle.getString("metatag.forced"), "forced", MediaFileSubtitle::isForced, Boolean.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * sdh
       */
      col = new Column(TmmResourceBundle.getString("metatag.sdh"), "sdh", MediaFileSubtitle::isSdh, Boolean.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * format
       */
      col = new Column(TmmResourceBundle.getString("metatag.format"), "format", MediaFileSubtitle::getCodec, String.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * title
       */
      col = new Column(TmmResourceBundle.getString("metatag.title"), "title", MediaFileSubtitle::getTitle, String.class);
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
