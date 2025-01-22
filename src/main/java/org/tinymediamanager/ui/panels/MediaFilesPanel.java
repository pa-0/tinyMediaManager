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
package org.tinymediamanager.ui.panels;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.TextDialog;
import org.tinymediamanager.ui.renderer.RightAlignTableCellRenderer;

import ca.odell.glazedlists.EventList;
import net.miginfocom.swing.MigLayout;

/**
 * The class {@link MediaFilesPanel} holds a {@link JTable} which contains all {@link MediaFile}s of a
 * {@link org.tinymediamanager.core.entities.MediaEntity}.
 * 
 * @author Manuel Laggner
 */
public class MediaFilesPanel extends JPanel {
  private static final Logger        LOGGER = LoggerFactory.getLogger(MediaFilesPanel.class);

  private TmmTable                   tableFiles;

  private final EventList<MediaFile> mediaFileEventList;

  public MediaFilesPanel(EventList<MediaFile> mediaFiles) {
    this.mediaFileEventList = mediaFiles;

    initComponents();
  }

  private void initComponents() {
    setLayout(new MigLayout("insets 0", "[200lp:450lp,grow]", "[100lp:300lp,grow]"));
    {
      TmmTableModel<MediaFile> tableModel = new TmmTableModel<>(mediaFileEventList, new MediaTableFormat());
      tableFiles = new TmmTable(tableModel);
      tableFiles.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

      LinkListener linkListener = new LinkListener();
      tableFiles.addMouseListener(linkListener);
      tableFiles.addMouseMotionListener(linkListener);

      JScrollPane scrollPaneFiles = new JScrollPane();
      tableFiles.configureScrollPane(scrollPaneFiles);
      add(scrollPaneFiles, "cell 0 0,grow");

      scrollPaneFiles.setViewportView(tableFiles);
    }
  }

  public void installTmmUILayoutStore(String parentId) {
    if (StringUtils.isNotBlank(parentId)) {
      tableFiles.setName(parentId + ".mediaFilesTable");
      TmmUILayoutStore.getInstance().install(tableFiles);
    }
  }

  public void adjustColumns() {
    tableFiles.adjustColumnPreferredWidths(6);
  }

  private static class MediaTableFormat extends TmmTableFormat<MediaFile> {
    MediaTableFormat() {
      /*
       * play/view
       */
      Column col = new Column("", "open", mediaFile -> {
        if (mediaFile.isVideo()) {
          return IconManager.PLAY;
        }
        if (mediaFile.isGraphic()) {
          return IconManager.SEARCH;
        }
        if (mediaFile.isMusicTheme()) {
          return IconManager.MUSIC;
        }
        if (mediaFile.isTextual()) {
          return IconManager.NFO_BLUE;
        }
        return null;
      }, ImageIcon.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * filename
       */
      col = new Column(TmmResourceBundle.getString("metatag.filename"), "filename", MediaFile::getFilename, String.class);
      addColumn(col);

      /*
       * filetype
       */
      col = new Column(TmmResourceBundle.getString("metatag.mediafiletype"), "filetype", mediaFile -> getMediaFileTypeLocalized(mediaFile.getType()),
          String.class);
      addColumn(col);

      /*
       * file size
       */
      col = new Column(TmmResourceBundle.getString("metatag.size"), "filesize", mediaFile -> Utils.formatFileSizeForDisplay(mediaFile.getFilesize()),
          String.class);
      col.setCellRenderer(new RightAlignTableCellRenderer());
      addColumn(col);

      /*
       * title
       */
      col = new Column(TmmResourceBundle.getString("metatag.title"), "title", MediaFile::getTitle, String.class);
      col.setDefaultHidden(true);
      addColumn(col);

      /*
       * video codec
       */
      col = new Column(TmmResourceBundle.getString("metatag.videocodec"), "videocodec", MediaFile::getVideoCodec, String.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * audio codec
       */
      col = new Column(TmmResourceBundle.getString("metatag.audiocodec"), "audiocodec", MediaFile::getAudioCodecListAsAstring, String.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * resolution
       */
      col = new Column(TmmResourceBundle.getString("metatag.resolution"), "resolution", MediaFile::getVideoResolution, String.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * runtime
       */
      col = new Column(TmmResourceBundle.getString("metatag.runtime"), "runtime", MediaFile::getDurationHMS, String.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * subtitle
       */
      col = new Column(TmmResourceBundle.getString("metatag.subtitle"), "subtitle", MediaFile::getSubtitlesAsString, String.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * creation date
       */
      col = new Column(TmmResourceBundle.getString("metatag.filecreationdate"), "filecreationdate",
          mediaFile -> formatDate(mediaFile.getDateCreated()), String.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * last modified date
       */
      col = new Column(TmmResourceBundle.getString("metatag.filelastmodifieddate"), "filelastmodifieddate",
          mediaFile -> formatDate(mediaFile.getDateLastModified()), String.class);
      col.setColumnResizeable(false);
      addColumn(col);
    }

    private String getMediaFileTypeLocalized(MediaFileType type) {
      String prop = "mediafiletype." + type.name().toLowerCase(Locale.ROOT);
      return TmmResourceBundle.getString(prop);
    }

    private String formatDate(Date date) {
      if (date == null) {
        return "";
      }

      return TmmDateFormat.MEDIUM_DATE_SHORT_TIME_FORMAT.format(date);
    }
  }

  private class LinkListener implements MouseListener, MouseMotionListener {
    @Override
    public void mouseClicked(MouseEvent arg0) {
      int col = tableFiles.columnAtPoint(arg0.getPoint());
      if (col == 0) {
        int row = tableFiles.rowAtPoint(arg0.getPoint());
        row = tableFiles.convertRowIndexToModel(row);
        MediaFile mf = mediaFileEventList.get(row);
        if (mf.isVideo()) {
          // open the video file in the desired player
          try {
            TmmUIHelper.openFile(MediaFileHelper.getMainVideoFile(mf));
          }
          catch (Exception e) {
            LOGGER.error("open file", e);
            MessageManager.instance
                .pushMessage(new Message(MessageLevel.ERROR, mf, "message.erroropenfile", new String[] { ":", e.getLocalizedMessage() }));
          }
        }
        else if (mf.isMusicTheme()) {
          try {
            TmmUIHelper.openFile(mf.getFileAsPath());
          }
          catch (Exception e) {
            LOGGER.error("open file", e);
            MessageManager.instance
                .pushMessage(new Message(MessageLevel.ERROR, mf, "message.erroropenfile", new String[] { ":", e.getLocalizedMessage() }));
          }
        }
        else if (mf.isGraphic()) {
          // open the graphic in the lightbox
          MainWindow.getInstance().createLightbox(mf.getFileAsPath().toString(), "");
        }
        else if (mf.isTextual()) {
          // open TextFile in Dialog
          String text;
          try {
            text = Utils.readFileToString(mf.getFileAsPath());
          }
          catch (IOException e) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingfound"));
            return;
          }
          JDialog textDialog = new TextDialog(mf.getFilename(), text);
          textDialog.setLocationRelativeTo(MainWindow.getInstance());
          textDialog.setVisible(true);
        }
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      JTable table = (JTable) e.getSource();
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
      if (col == 0) {
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      JTable table = (JTable) e.getSource();
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
      if (col != 0) {
        table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      JTable table = (JTable) e.getSource();
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
      if (col != 0 && table.getCursor().getType() == Cursor.HAND_CURSOR) {
        table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
      if (col == 0 && table.getCursor().getType() == Cursor.DEFAULT_CURSOR) {
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      // nothing to do here
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      // nothing to do here
    }

    @Override
    public void mouseDragged(MouseEvent arg0) {
      // nothing to do here
    }
  }
}
