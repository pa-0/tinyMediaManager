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
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.table.TmmEditorTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.panels.IModalPopupPanelProvider;
import org.tinymediamanager.ui.panels.MediaFileAudioStreamEditorPanel;
import org.tinymediamanager.ui.panels.ModalPopupPanel;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.GlazedListsSwing;

/**
 * the class {@link MediaFileAudioStreamEditTable} is used to display all audio streams in a table
 * 
 * @author Manuel Laggner
 */
public abstract class MediaFileAudioStreamEditTable extends TmmEditorTable {
  private final EventList<MediaFileAudioStream> audioStreamEventList;

  public MediaFileAudioStreamEditTable(EventList<MediaFileAudioStream> audioStreamEventList) {
    super();

    this.audioStreamEventList = audioStreamEventList;

    setModel(new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(audioStreamEventList), new AudioStreamTableFormat()));

    adjustColumnPreferredWidths(3);
  }

  @Override
  protected void editButtonClicked(int row) {
    IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(this);
    if (iModalPopupPanelProvider == null) {
      return;
    }

    MediaFileAudioStream audioStream = getAudioStream(row);

    ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
    popupPanel.setTitle(TmmResourceBundle.getString("audiostream.edit"));

    popupPanel.setOnCloseHandler(this::onEditAudioStream);

    MediaFileAudioStreamEditorPanel audioStreamEditorPanel = new MediaFileAudioStreamEditorPanel(audioStream);
    popupPanel.setContent(audioStreamEditorPanel);
    iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
  }

  public abstract void onEditAudioStream();

  /**
   * Utility to get the right {@link MediaFileAudioStream} for the given row
   *
   * @param row
   *          the row number to get the {@link MediaFileAudioStream} for
   * @return the {@link MediaFileAudioStream}
   */
  private MediaFileAudioStream getAudioStream(int row) {
    int index = convertRowIndexToModel(row);
    return audioStreamEventList.get(index);
  }

  /**
   * get all selected {@link MediaFileAudioStream}s
   *
   * @return a {@link List} of all selected {@link MediaFileAudioStream}s
   */
  public List<MediaFileAudioStream> getAudioStreams() {
    List<MediaFileAudioStream> audioStreams = new ArrayList<>();
    for (int i : getSelectedRows()) {
      MediaFileAudioStream audioStream = getAudioStream(i);
      if (audioStream != null) {
        audioStreams.add(audioStream);
      }

    }
    return audioStreams;
  }

  public void addAudioStream() {
    IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(this);
    if (iModalPopupPanelProvider == null) {
      return;
    }

    MediaFileAudioStream audioStream = new MediaFileAudioStream();

    ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
    popupPanel.setTitle(TmmResourceBundle.getString("audiostream.add"));

    popupPanel.setOnCloseHandler(() -> {
      audioStreamEventList.add(audioStream);
      onAddAudioStream();
    });

    MediaFileAudioStreamEditorPanel audioStreamEditorPanel = new MediaFileAudioStreamEditorPanel(audioStream);
    popupPanel.setContent(audioStreamEditorPanel);
    iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
  }

  public abstract void onAddAudioStream();

  /**
   * helper classes
   */
  private static class AudioStreamTableFormat extends TmmTableFormat<MediaFileAudioStream> {
    private AudioStreamTableFormat() {
      /*
       * codec
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.codec"), "codec", MediaFileAudioStream::getCodec, String.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * channels
       */
      col = new Column(TmmResourceBundle.getString("metatag.channels"), "channels", MediaFileAudioStream::getAudioChannels, Integer.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * bitrate
       */
      col = new Column(TmmResourceBundle.getString("metatag.bitrate"), "bitrate", MediaFileAudioStream::getBitrate, Integer.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * bitdepth
       */
      col = new Column(TmmResourceBundle.getString("metatag.bitdepth"), "bitdepth", MediaFileAudioStream::getBitDepth, Integer.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * language
       */
      col = new Column(TmmResourceBundle.getString("metatag.language"), "language", MediaFileAudioStream::getLanguage, String.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * title
       */
      col = new Column(TmmResourceBundle.getString("metatag.title"), "title", MediaFileAudioStream::getTitle, String.class);
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
