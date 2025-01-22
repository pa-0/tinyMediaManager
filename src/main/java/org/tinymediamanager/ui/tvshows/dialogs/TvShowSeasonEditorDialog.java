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
package org.tinymediamanager.ui.tvshows.dialogs;

import static org.tinymediamanager.core.MediaFileType.SEASON_BANNER;
import static org.tinymediamanager.core.MediaFileType.SEASON_FANART;
import static org.tinymediamanager.core.MediaFileType.SEASON_POSTER;
import static org.tinymediamanager.core.MediaFileType.SEASON_THUMB;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowArtworkHelper;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.ui.ArtworkDragAndDropListener;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.ShadowLayerUI;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmTabbedPane;
import org.tinymediamanager.ui.dialogs.AbstractEditorDialog;
import org.tinymediamanager.ui.dialogs.ImageChooserDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowSeasonEditor.
 * 
 * @author Manuel Laggner
 */
public class TvShowSeasonEditorDialog extends AbstractEditorDialog {
  private static final String ORIGINAL_IMAGE_SIZE = "originalImageSize";
  private static final String SPACER              = "        ";

  private final TvShowSeason  tvShowSeasonToEdit;
  private final TvShowList    tvShowList          = TvShowModuleManager.getInstance().getTvShowList();
  private final JTabbedPane   tabbedPane          = new TmmTabbedPane();

  /**
   * UI elements
   */
  private JTextField          tfTitle;
  private JTextArea           taPlot;
  private JTextArea           taNote;

  private ImageLabel          lblPoster;
  private ImageLabel          lblFanart;
  private ImageLabel          lblBanner;
  private ImageLabel          lblThumb;

  private JTextField          tfPoster;
  private JTextField          tfFanart;
  private JTextField          tfBanner;
  private JTextField          tfThumb;

  /**
   * Instantiates a new tv show season editor dialog.
   *
   * @param tvShowSeason
   *          the tv show season
   * @param queueIndex
   *          the actual index in the queue
   * @param queueSize
   *          the queue size
   */
  public TvShowSeasonEditorDialog(TvShowSeason tvShowSeason, int queueIndex, int queueSize, int selectedTab) {
    super(TmmResourceBundle.getString("tvshowseason.edit") + (queueSize > 1 ? " " + (queueIndex + 1) + "/" + queueSize : ""), "tvShowSeasonEditor",
        tvShowSeason);
    setMinimumSize(new Dimension(800, 600));

    this.tvShowSeasonToEdit = tvShowSeason;
    this.queueIndex = queueIndex;
    this.queueSize = queueSize;

    initComponents();

    {
      tfTitle.setText(tvShowSeason.getTitle());
      taPlot.setText(tvShowSeason.getPlot());
      taNote.setText(tvShowSeason.getNote());
      lblPoster.setImagePath(tvShowSeason.getArtworkFilename(SEASON_POSTER));
      lblFanart.setImagePath(tvShowSeason.getArtworkFilename(SEASON_FANART));
      lblThumb.setImagePath(tvShowSeason.getArtworkFilename(SEASON_THUMB));
      lblBanner.setImagePath(tvShowSeason.getArtworkFilename(SEASON_BANNER));

      tfPoster.setText(tvShowSeason.getArtworkUrl(SEASON_POSTER));
      tfFanart.setText(tvShowSeason.getArtworkUrl(SEASON_FANART));
      tfThumb.setText(tvShowSeason.getArtworkUrl(SEASON_THUMB));
      tfBanner.setText(tvShowSeason.getArtworkUrl(SEASON_BANNER));
    }

    // register dnd listener
    registerDropTarget(lblPoster, tfPoster);
    registerDropTarget(lblFanart, tfFanart);
    registerDropTarget(lblBanner, tfBanner);
    registerDropTarget(lblThumb, tfThumb);

    tabbedPane.setSelectedIndex(selectedTab);
  }

  /**
   * Returns the tab number
   * 
   * @return 0-X
   */
  public int getSelectedTab() {
    return tabbedPane.getSelectedIndex();
  }

  private void initComponents() {
    // to draw the shadow beneath window frame, encapsulate the panel
    JLayer<JComponent> rootLayer = new JLayer(tabbedPane, new ShadowLayerUI()); // removed <> because this leads WBP to crash
    getContentPane().add(rootLayer, BorderLayout.CENTER);

    /**********************************************************************************
     * DetailsPanel
     **********************************************************************************/
    {
      JPanel detailsPanel = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("metatag.details"), null, detailsPanel, null);
      detailsPanel.setLayout(new MigLayout("", "[][grow][20lp:n][200lp:250lp,grow]", "[][250lp:75%:75%,grow][75lp:20%:20%,grow]"));
      {
        JLabel lblTitleT = new TmmLabel(TmmResourceBundle.getString("metatag.title"));
        detailsPanel.add(lblTitleT, "flowx,cell 0 0,alignx right");

        tfTitle = new JTextField();
        detailsPanel.add(tfTitle, "cell 1 0,growx");
      }

      {
        JLabel lblPlot = new TmmLabel(TmmResourceBundle.getString("metatag.plot"));
        detailsPanel.add(lblPlot, "cell 0 1,alignx right,aligny top");

        JScrollPane scrollPanePlot = new JScrollPane();
        detailsPanel.add(scrollPanePlot, "cell 1 1,grow");

        taPlot = new JTextArea();
        taPlot.setLineWrap(true);
        taPlot.setWrapStyleWord(true);
        taPlot.setForeground(UIManager.getColor("TextField.foreground"));
        scrollPanePlot.setViewportView(taPlot);
      }

      {
        JLabel lblNoteT = new TmmLabel(TmmResourceBundle.getString("metatag.note"));
        detailsPanel.add(lblNoteT, "cell 0 2,alignx right,aligny top");

        JScrollPane scrollPane = new JScrollPane();
        detailsPanel.add(scrollPane, "cell 1 2,grow,wmin 0");

        taNote = new JTextArea();
        taNote.setLineWrap(true);
        taNote.setWrapStyleWord(true);
        taNote.setForeground(UIManager.getColor("TextField.foreground"));
        scrollPane.setViewportView(taNote);
      }

      {
        JLabel lblPosterT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.poster"));
        detailsPanel.add(lblPosterT, "flowx,cell 3 0");
        detailsPanel.add(new TmmLabel(TmmResourceBundle.getString("mediafiletype.poster")), "cell 3 0");

        LinkLabel lblPosterSize = new LinkLabel();
        detailsPanel.add(lblPosterSize, "cell 3 0");

        JButton btnDeletePoster = new FlatButton(SPACER, IconManager.DELETE_GRAY);
        btnDeletePoster.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeletePoster.addActionListener(e -> {
          lblPoster.clearImage();
          tfPoster.setText("");
        });
        btnDeletePoster.setFocusable(false);
        detailsPanel.add(btnDeletePoster, "cell 3 0");

        lblPoster = new ImageLabel();
        lblPoster.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblPoster.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            openImageChooser(lblPoster, tfPoster, MediaFileType.getMediaArtworkType(SEASON_POSTER));
          }
        });

        detailsPanel.add(lblPoster, "cell 3 1 1 2,grow");
        lblPoster.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblPosterSize, lblPoster, btnDeletePoster, SEASON_POSTER));
      }
    }

    /**********************************************************************************
     * local artwork pane
     **********************************************************************************/
    {
      JPanel artworkPanel = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("metatag.extraartwork"), null, artworkPanel, null);
      artworkPanel
          .setLayout(new MigLayout("", "[20lp:n][300lp:400lp,grow][20lp:n][300lp:400lp,grow]", "[][100lp:125lp,grow][20lp:n][][100lp:125lp,grow][]"));
      {
        JLabel lblFanartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.fanart"));
        artworkPanel.add(lblFanartT, "cell 1 0");

        LinkLabel lblFanartSize = new LinkLabel();
        artworkPanel.add(lblFanartSize, "cell 1 0");

        JButton btnDeleteFanart = new FlatButton(SPACER, IconManager.DELETE_GRAY);
        btnDeleteFanart.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteFanart.addActionListener(e -> {
          lblFanart.clearImage();
          tfFanart.setText("");
        });
        artworkPanel.add(btnDeleteFanart, "cell 1 0");

        lblFanart = new ImageLabel();
        lblFanart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblFanart.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            openImageChooser(lblFanart, tfFanart, MediaFileType.getMediaArtworkType(SEASON_FANART));
          }
        });

        artworkPanel.add(lblFanart, "cell 1 1,grow");
        lblFanart.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblFanartSize, lblFanart, btnDeleteFanart, SEASON_FANART));
      }
      {
        JLabel lblThumbT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.thumb"));
        artworkPanel.add(lblThumbT, "cell 3 0");

        LinkLabel lblThumbSize = new LinkLabel();
        artworkPanel.add(lblThumbSize, "cell 3 0");

        JButton btnDeleteThumb = new FlatButton(SPACER, IconManager.DELETE_GRAY);
        btnDeleteThumb.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteThumb.addActionListener(e -> {
          lblThumb.clearImage();
          tfThumb.setText("");
        });
        artworkPanel.add(btnDeleteThumb, "cell 3 0");

        lblThumb = new ImageLabel();
        lblThumb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblThumb.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            openImageChooser(lblThumb, tfThumb, MediaFileType.getMediaArtworkType(SEASON_THUMB));
          }
        });

        artworkPanel.add(lblThumb, "cell 3 1,grow");
        lblThumb.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE, e -> setImageSizeAndCreateLink(lblThumbSize, lblThumb, btnDeleteThumb, SEASON_THUMB));
      }
      {

        lblBanner = new ImageLabel();
        lblBanner.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblBanner.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            openImageChooser(lblBanner, tfBanner, MediaFileType.getMediaArtworkType(SEASON_BANNER));
          }
        });
        artworkPanel.add(lblBanner, "cell 1 4 3 1,grow");

        JLabel lblBannerT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.banner"));
        artworkPanel.add(lblBannerT, "flowx,cell 1 3");

        LinkLabel lblBannerSize = new LinkLabel();
        artworkPanel.add(lblBannerSize, "cell 1 3");

        JButton btnDeleteBanner = new FlatButton(SPACER, IconManager.DELETE_GRAY);
        btnDeleteBanner.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteBanner.addActionListener(e -> {
          lblBanner.clearImage();
          tfBanner.setText("");
        });
        artworkPanel.add(btnDeleteBanner, "cell 1 3");

        lblBanner.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblBannerSize, lblBanner, btnDeleteBanner, SEASON_BANNER));
      }
    }

    /**********************************************************************************
     * artwork urls
     **********************************************************************************/
    {
      JPanel artworkPanel = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("edit.artwork"), null, artworkPanel, null);
      artworkPanel.setLayout(new MigLayout("", "[][grow]", "[][][][]"));
      {
        JLabel lblPosterT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.poster"));
        artworkPanel.add(lblPosterT, "cell 0 0,alignx right");

        tfPoster = new JTextField();
        artworkPanel.add(tfPoster, "cell 1 0,growx");
      }
      {
        JLabel lblFanartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.fanart"));
        artworkPanel.add(lblFanartT, "cell 0 1,alignx right");

        tfFanart = new JTextField();
        artworkPanel.add(tfFanart, "cell 1 1,growx");
      }
      {
        JLabel lblBannerT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.banner"));
        artworkPanel.add(lblBannerT, "cell 0 2,alignx right");

        tfBanner = new JTextField();
        artworkPanel.add(tfBanner, "cell 1 2,growx");
      }
      {
        JLabel lblThumbT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.thumb"));
        artworkPanel.add(lblThumbT, "cell 0 3,alignx right");

        tfThumb = new JTextField();
        artworkPanel.add(tfThumb, "cell 1 3,growx");
      }
    }

    /**********************************************************************************
     * button pane
     **********************************************************************************/
    {
      if (queueSize > 1) {
        JButton btnAbort = new JButton(new AbortQueueAction(TmmResourceBundle.getString("tvshow.edit.abortqueue.desc")));
        addButton(btnAbort);
        if (queueIndex > 0) {
          JButton backButton = new JButton(new NavigateBackAction());
          addButton(backButton);
        }
      }

      JButton cancelButton = new JButton(new CancelAction());
      addButton(cancelButton);

      JButton okButton = new JButton(new OKAction());
      getRootPane().registerKeyboardAction(new OKAction(), KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
          JComponent.WHEN_IN_FOCUSED_WINDOW);
      getRootPane().registerKeyboardAction(new OKAction(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
          JComponent.WHEN_IN_FOCUSED_WINDOW);
      addDefaultButton(okButton);
    }
  }

  private void registerDropTarget(ImageLabel imageLabel, JTextField textField) {
    new DropTarget(imageLabel, new ArtworkDragAndDropListener(imageLabel) {
      @Override
      public void drop(DropTargetDropEvent dtde) {
        super.drop(dtde);
        updateArtworkUrl(imageLabel, textField);
      }
    });
  }

  private void updateArtworkUrl(ImageLabel imageLabel, JTextField textField) {
    if (StringUtils.isNotBlank(imageLabel.getImageUrl())) {
      textField.setText(imageLabel.getImageUrl());
    }
  }

  private void openImageChooser(ImageLabel label, JTextField textField, MediaArtwork.MediaArtworkType artworkType) {
    Map<String, Object> ids = new HashMap<>(tvShowSeasonToEdit.getTvShow().getIds());
    ids.put("tvShowSeason", tvShowSeasonToEdit.getSeason());
    ImageChooserDialog dialog = new ImageChooserDialog(TvShowSeasonEditorDialog.this, ids, artworkType, tvShowList.getDefaultArtworkScrapers(), label,
        MediaType.TV_SHOW);

    if (Settings.getInstance().isImageChooserUseEntityFolder()) {
      TvShow tvShow = tvShowSeasonToEdit.getTvShow();
      Path seasonPath = tvShow.getPathNIO().resolve(TvShowHelpers.detectSeasonFolder(tvShow, tvShowSeasonToEdit.getSeason())).toAbsolutePath();
      if (!Files.exists(seasonPath)) {
        seasonPath = tvShow.getPathNIO().toAbsolutePath();
      }
      dialog.setOpenFolderPath(seasonPath.toString());
    }

    dialog.setLocationRelativeTo(MainWindow.getInstance());
    dialog.setVisible(true);
    updateArtworkUrl(label, textField);
  }

  private void processArtwork(MediaFileType type, ImageLabel imageLabel, JTextField textField) {
    if (StringUtils.isAllBlank(imageLabel.getImagePath(), imageLabel.getImageUrl())
        && StringUtils.isNotBlank(tvShowSeasonToEdit.getArtworkFilename(type))) {
      // artwork has been explicitly deleted
      tvShowSeasonToEdit.deleteMediaFiles(type);
    }

    if (StringUtils.isNotEmpty(textField.getText()) && !textField.getText().equals(tvShowSeasonToEdit.getArtworkUrl(type))) {
      // artwork url and textfield do not match -> redownload
      tvShowSeasonToEdit.setArtworkUrl(textField.getText(), type);
      TvShowArtworkHelper.downloadSeasonArtwork(tvShowSeasonToEdit, type);
    }
    else if (StringUtils.isEmpty(textField.getText())) {
      // remove the artwork url
      tvShowSeasonToEdit.removeArtworkUrl(type);
    }
    else {
      // they match, but check if there is a need to download the artwork
      if (StringUtils.isBlank(tvShowSeasonToEdit.getArtworkFilename(type))) {
        TvShowArtworkHelper.downloadSeasonArtwork(tvShowSeasonToEdit, type);
      }
    }
  }

  private class OKAction extends AbstractAction {
    OKAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.ok"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.change"));
      putValue(SMALL_ICON, IconManager.APPLY_INV);
      putValue(LARGE_ICON_KEY, IconManager.APPLY_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      // commit all pending changes in this dialog
      commitChanges();

      tvShowSeasonToEdit.setTitle(tfTitle.getText());
      tvShowSeasonToEdit.setPlot(taPlot.getText());
      tvShowSeasonToEdit.setNote(taNote.getText());

      // process artwork
      processArtwork(SEASON_POSTER, lblPoster, tfPoster);
      processArtwork(SEASON_FANART, lblFanart, tfFanart);
      processArtwork(SEASON_BANNER, lblBanner, tfBanner);
      processArtwork(SEASON_THUMB, lblThumb, tfThumb);

      tvShowSeasonToEdit.writeNfo();
      tvShowSeasonToEdit.getTvShow().writeNFO();
      tvShowSeasonToEdit.getTvShow().saveToDb();

      setVisible(false);
    }
  }

  private class CancelAction extends AbstractAction {
    CancelAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.cancel"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("edit.discard"));
      putValue(SMALL_ICON, IconManager.CANCEL_INV);
      putValue(LARGE_ICON_KEY, IconManager.CANCEL_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      setVisible(false);
    }
  }
}
