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
package org.tinymediamanager.ui.components;

import java.util.Comparator;
import java.util.Locale;

import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.license.License;
import org.tinymediamanager.scraper.util.UrlUtil;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.table.TmmEditorTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.panels.IModalPopupPanelProvider;
import org.tinymediamanager.ui.panels.ModalPopupPanel;
import org.tinymediamanager.ui.panels.TrailerEditorPanel;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.GlazedListsSwing;

/**
 * the class {@link MediaTrailerTable} is used to display {@link MediaTrailer}s in a table
 * 
 * @author Manuel Laggner
 */
public class MediaTrailerTable extends TmmEditorTable {
  private static final Logger           LOGGER = LoggerFactory.getLogger(MediaTrailerTable.class);

  private final EventList<MediaTrailer> trailerEventList;
  private final boolean                 editable;

  public MediaTrailerTable(EventList<MediaTrailer> trailerEventList) {
    this(trailerEventList, false);
  }

  public MediaTrailerTable(EventList<MediaTrailer> trailerEventList, boolean editable) {
    super();

    this.trailerEventList = trailerEventList;
    this.editable = editable;

    setModel(new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(trailerEventList),
        new TrailerTableFormat(editable, License.getInstance().isValidLicense())));

    adjustColumnPreferredWidths(3);
  }

  @Override
  protected void editButtonClicked(int row) {
    IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(MediaTrailerTable.this);
    if (iModalPopupPanelProvider == null) {
      return;
    }

    MediaTrailer trailer = getTrailer(row);

    ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
    popupPanel.setTitle(TmmResourceBundle.getString("trailer.edit"));

    TrailerEditorPanel trailerEditorPanel = new TrailerEditorPanel(trailer);
    popupPanel.setContent(trailerEditorPanel);
    iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
  }

  protected void downloadTrailer(MediaTrailer mediaTrailer) {
    // callback
  }

  protected String refreshUrlFromId(MediaTrailer trailer) {
    // callback
    return trailer.getUrl();
  }

  protected void nfoButtonClicked(MediaTrailer trailer) {
    if (!this.editable) {
      return;
    }

    for (MediaTrailer mediaTrailer : trailerEventList) {
      if (mediaTrailer.equals(trailer)) {
        mediaTrailer.setInNfo(true);
      }
      else {
        mediaTrailer.setInNfo(false);
      }
    }

  }

  protected void playTrailer(MediaTrailer trailer) {
    String url = trailer.getUrl();

    if (url.isEmpty()) {
      url = refreshUrlFromId(trailer);
    }

    if (StringUtils.isNotBlank(url)) {
      try {
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception ex) {
        LOGGER.error(ex.getMessage());
        MessageManager.instance.pushMessage(
            new Message(Message.MessageLevel.ERROR, trailer.getUrl(), "message.erroropenurl", new String[] { ":", ex.getLocalizedMessage() }));
      }
    }
  }

  @Override
  protected boolean isLinkCell(int row, int column) {
    return this.editable && isNfoColumn(column) || isEditorColumn(column) || (isDownloadColumn(column) && isDownloadUrlAvailable(row))
        || isPlayColumn(column);
  }

  /**
   * check if this column is the download column
   *
   * @param column
   *          the column index
   * @return true/false
   */
  private boolean isDownloadColumn(int column) {
    if (column < 0) {
      return false;
    }

    return "download".equals(getColumnModel().getColumn(column).getIdentifier());
  }

  /**
   * checks whether a download url is available or not
   *
   * @param row
   *          the row to get the data for
   * @return true if a download url is available, false otherwise
   */
  private boolean isDownloadUrlAvailable(int row) {
    if (row >= 0 && row < trailerEventList.size()) {
      String url = getTrailer(row).getUrl();
      if (url != null && url.startsWith("http")) {
        return true;
      }
      if (!StringUtils.isBlank(getTrailer(row).getId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * check if this column is the NFO column
   *
   * @param column
   *          the column index
   * @return true/false
   */
  private boolean isNfoColumn(int column) {
    if (column < 0) {
      return false;
    }

    return "nfo".equals(getColumnModel().getColumn(column).getIdentifier());
  }

  /**
   * check if this column is the play column
   *
   * @param column
   *          the column index
   * @return true/false
   */
  private boolean isPlayColumn(int column) {
    if (column < 0) {
      return false;
    }

    return "play".equals(getColumnModel().getColumn(column).getIdentifier());
  }

  /**
   * Utility to get the right {@link MediaTrailer} for the given row
   *
   * @param row
   *          the row number to get the {@link MediaTrailer} for
   * @return the {@link MediaTrailer}
   */
  private MediaTrailer getTrailer(int row) {
    int index = convertRowIndexToModel(row);
    return trailerEventList.get(index);
  }

  @Override
  protected void linkClicked(int row, int column) {
    MediaTrailer trailer = getTrailer(row);

    if (trailer != null) {
      if (isDownloadColumn(column)) {
        downloadTrailer(trailer);
      }
      else if (isPlayColumn(column)) {
        playTrailer(trailer);
      }
      else if (isNfoColumn(column)) {
        nfoButtonClicked(trailer);
      }
    }
  }

  public void addTrailer() {
    IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(MediaTrailerTable.this);
    if (iModalPopupPanelProvider == null) {
      return;
    }

    MediaTrailer trailer = new MediaTrailer();

    ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
    popupPanel.setTitle(TmmResourceBundle.getString("trailer.add"));

    TrailerEditorPanel trailerEditorPanel = new TrailerEditorPanel(trailer);
    popupPanel.setContent(trailerEditorPanel);
    popupPanel.setOnCloseHandler(() -> trailerEventList.add(trailer));

    iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
  }

  /**
   * helper classes
   */
  private static class TrailerTableFormat extends TmmTableFormat<MediaTrailer> {
    public TrailerTableFormat(boolean editable, boolean downloadEnabled) {
      Comparator<String> stringComparator = new StringComparator();
      Comparator<Boolean> booleanComparator = new BooleanComparator();
      Comparator<ImageIcon> imageComparator = new ImageComparator();

      Column col;

      /*
       * download (note available in editor or if there is no valid license)
       */
      if (!editable && downloadEnabled) {
        col = new Column("", "download", trailer -> {
          if ((StringUtils.isNotBlank(trailer.getUrl()) && trailer.getUrl().toLowerCase(Locale.ROOT).startsWith("http"))
              || !StringUtils.isBlank(trailer.getId())) {
            return IconManager.DOWNLOAD;
          }
          return null;
        }, ImageIcon.class);
        col.setColumnResizeable(false);
        col.setColumnComparator(imageComparator);
        addColumn(col);
      }

      /*
       * play
       */
      col = new Column("", "play", trailer -> IconManager.PLAY, ImageIcon.class);
      col.setColumnResizeable(false);
      col.setColumnComparator(imageComparator);

      addColumn(col);

      /*
       * nfo
       */
      col = new Column(TmmResourceBundle.getString("metatag.nfo"), "nfo", MediaTrailer::getInNfo, Boolean.class);
      col.setColumnResizeable(false);
      col.setColumnComparator(booleanComparator);
      addColumn(col);

      /*
       * name
       */
      col = new Column(TmmResourceBundle.getString("metatag.name"), "name", MediaTrailer::getName, String.class);
      col.setCellTooltip(MediaTrailer::getName);
      col.setColumnComparator(stringComparator);
      addColumn(col);

      /*
       * source
       */
      col = new Column(TmmResourceBundle.getString("metatag.source"), "source", MediaTrailer::getProvider, String.class);
      col.setCellTooltip(MediaTrailer::getProvider);
      col.setColumnComparator(stringComparator);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * quality
       */
      col = new Column(TmmResourceBundle.getString("metatag.quality"), "quality", MediaTrailer::getQuality, String.class);
      col.setColumnResizeable(false);
      col.setColumnComparator(stringComparator);
      addColumn(col);

      if (!editable) {
        /*
         * format
         */
        col = new Column(TmmResourceBundle.getString("metatag.format"), "format", trailer -> {
          String ext = UrlUtil.getExtension(trailer.getUrl()).toLowerCase(Locale.ROOT);
          if (!Settings.getInstance().getVideoFileType().contains("." + ext)) {
            // .php redirection scripts et all
            ext = "";
          }
          return ext;
        }, String.class);
        col.setColumnComparator(stringComparator);
        col.setColumnResizeable(false);
        addColumn(col);
      }
      else {
        /*
         * url (only in the editor)
         */
        col = new Column(TmmResourceBundle.getString("metatag.url"), "url", MediaTrailer::getUrl, String.class);
        col.setCellTooltip(MediaTrailer::getUrl);
        col.setColumnComparator(stringComparator);
        col.setColumnResizeable(false);
        addColumn(col);

        /*
         * edit (only in the editor)
         */
        col = new Column(TmmResourceBundle.getString("Button.edit"), "edit", person -> IconManager.EDIT, ImageIcon.class);
        col.setColumnResizeable(false);
        col.setHeaderIcon(IconManager.EDIT_HEADER);
        addColumn(col);
      }
    }
  }
}
