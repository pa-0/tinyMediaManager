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
package org.tinymediamanager.ui.moviesets.dialogs;

import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BACKGROUND;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARLOGO;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.DISC;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.observablecollections.ObservableCollections;
import org.jdesktop.swingbinding.JTableBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSetArtworkHelper;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmObligatoryTextArea;
import org.tinymediamanager.ui.components.TmmTabbedPane;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.dialogs.AbstractEditorDialog;
import org.tinymediamanager.ui.dialogs.ImageChooserDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieSetEditorDialog. Edit movie sets
 * 
 * @author Manuel Laggner
 */
public class MovieSetEditorDialog extends AbstractEditorDialog {
  private static final Logger      LOGGER              = LoggerFactory.getLogger(MovieSetEditorDialog.class);
  private static final String      ORIGINAL_IMAGE_SIZE = "originalImageSize";
  private static final String      SPACER              = "        ";

  private final MovieList          movieList           = MovieModuleManager.getInstance().getMovieList();
  private final JTabbedPane        tabbedPane          = new TmmTabbedPane();
  private final MovieSet           movieSetToEdit;
  private final List<Movie>        moviesInSet         = ObservableCollections.observableList(new ArrayList<>());
  private final List<Movie>        removedMovies       = new ArrayList<>();
  private final List<MediaScraper> artworkScrapers     = new ArrayList<>();

  /** UI components */
  private JTextArea                tfName;
  private TmmTable                 tableMovies;
  private ImageLabel               lblPoster;
  private ImageLabel               lblFanart;
  private JTextArea                taPlot;
  private JTextField               tfTmdbId;

  private ImageLabel               lblClearlogo;
  private ImageLabel               lblBanner;
  private ImageLabel               lblClearart;
  private ImageLabel               lblDisc;
  private ImageLabel               lblThumb;

  private JTextField               tfPoster;
  private JTextField               tfFanart;
  private JTextField               tfClearLogo;
  private JTextField               tfBanner;
  private JTextField               tfClearArt;
  private JTextField               tfThumb;
  private JTextField               tfDisc;
  private JTextArea                taNote;

  /**
   * Instantiates a new movie set editor.
   * 
   * @param movieSet
   *          the movie set
   * @param queueIndex
   *          the actual index in the queue
   * @param queueSize
   *          the queue size
   */
  public MovieSetEditorDialog(MovieSet movieSet, int queueIndex, int queueSize, int selectedTab) {
    super(TmmResourceBundle.getString("movieset.edit") + (queueSize > 1 ? " " + (queueIndex + 1) + "/" + queueSize : ""), "movieSetEditor", movieSet);

    movieSetToEdit = movieSet;
    artworkScrapers.addAll(movieList.getDefaultArtworkScrapers());
    this.queueIndex = queueIndex;
    this.queueSize = queueSize;

    {
      getContentPane().add(tabbedPane, BorderLayout.CENTER);

      JPanel panelContent = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("metatag.details"), panelContent);
      panelContent.setLayout(new MigLayout("", "[][400lp,grow][150lp:200lp,grow 50]",
          "[][][100lp:25%:25%,grow][50lp:50lp:100lp,grow 50][20lp:n][pref!][][50lp:20%:30%,grow]"));

      JLabel lblName = new TmmLabel(TmmResourceBundle.getString("movieset.title"));
      panelContent.add(lblName, "cell 0 0,alignx right");

      tfName = new TmmObligatoryTextArea();
      panelContent.add(tfName, "cell 1 0,growx,aligny top, wmin 0");
      tfName.setColumns(10);

      lblPoster = new ImageLabel();
      lblPoster.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      lblPoster.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          ImageChooserDialog dialog = new ImageChooserDialog(MovieSetEditorDialog.this, extractIds(), POSTER, artworkScrapers, lblPoster,
              MediaType.MOVIE_SET);

          dialog.setLocationRelativeTo(MainWindow.getInstance());
          dialog.setVisible(true);
          updateArtworkUrl(lblPoster, tfPoster);
        }
      });
      panelContent.add(new TmmLabel(TmmResourceBundle.getString("mediafiletype.poster")), "cell 2 0");

      LinkLabel lblPosterSize = new LinkLabel();
      panelContent.add(lblPosterSize, "cell 2 0");

      JButton btnDeletePoster = new FlatButton(SPACER, IconManager.DELETE_GRAY);
      btnDeletePoster.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
      btnDeletePoster.addActionListener(e -> {
        lblPoster.clearImage();
        tfPoster.setText("");
      });
      panelContent.add(btnDeletePoster, "cell 2 0");

      panelContent.add(lblPoster, "cell 2 1 1 5,grow");
      lblPoster.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
          e -> setImageSizeAndCreateLink(lblPosterSize, lblPoster, btnDeletePoster, MediaFileType.POSTER));

      JLabel lblTmdbid = new TmmLabel(TmmResourceBundle.getString("metatag.tmdb"));
      panelContent.add(lblTmdbid, "cell 0 1,alignx right");

      tfTmdbId = new JTextField();
      panelContent.add(tfTmdbId, "flowx,cell 1 1,aligny center");
      tfTmdbId.setColumns(10);

      JLabel lblOverview = new TmmLabel(TmmResourceBundle.getString("metatag.plot"));
      panelContent.add(lblOverview, "cell 0 2,alignx right,aligny top");

      JScrollPane scrollPaneOverview = new JScrollPane();
      panelContent.add(scrollPaneOverview, "cell 1 2,grow, wmin 0");

      taPlot = new JTextArea();
      taPlot.setLineWrap(true);
      taPlot.setWrapStyleWord(true);
      taPlot.setForeground(UIManager.getColor("TextField.foreground"));
      scrollPaneOverview.setViewportView(taPlot);

      JLabel lblNoteT = new TmmLabel(TmmResourceBundle.getString("metatag.note"));
      panelContent.add(lblNoteT, "cell 0 3,alignx right,aligny top");

      JScrollPane scrollPane = new JScrollPane();
      panelContent.add(scrollPane, "cell 1 3,grow,wmin 0");

      taNote = new JTextArea();
      taNote.setLineWrap(true);
      taNote.setWrapStyleWord(true);
      taNote.setForeground(UIManager.getColor("TextField.foreground"));
      scrollPane.setViewportView(taNote);

      JLabel lblMovies = new TmmLabel(TmmResourceBundle.getString("tmm.movies"));
      panelContent.add(lblMovies, "flowy,cell 0 5,alignx right,aligny top");

      JScrollPane scrollPaneMovies = new JScrollPane();
      panelContent.add(scrollPaneMovies, "cell 1 5 1 3,grow");

      tableMovies = new TmmTable();
      scrollPaneMovies.setViewportView(tableMovies);

      lblFanart = new ImageLabel();
      lblFanart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      lblFanart.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          ImageChooserDialog dialog = new ImageChooserDialog(MovieSetEditorDialog.this, extractIds(), BACKGROUND, artworkScrapers, lblFanart,
              MediaType.MOVIE_SET);

          dialog.setLocationRelativeTo(MainWindow.getInstance());
          dialog.setVisible(true);
          updateArtworkUrl(lblFanart, tfFanart);
        }
      });
      panelContent.add(new TmmLabel(TmmResourceBundle.getString("mediafiletype.fanart")), "cell 2 6");

      LinkLabel lblFanartSize = new LinkLabel();
      panelContent.add(lblFanartSize, "cell 2 6");
      JButton btnDeleteFanart = new FlatButton(SPACER, IconManager.DELETE_GRAY);
      btnDeleteFanart.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
      btnDeleteFanart.addActionListener(e -> {
        lblFanart.clearImage();
        tfFanart.setText("");
      });
      panelContent.add(btnDeleteFanart, "cell 2 6");

      panelContent.add(lblFanart, "cell 2 7,grow");
      lblFanart.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
          e -> setImageSizeAndCreateLink(lblFanartSize, lblFanart, btnDeleteFanart, MediaFileType.FANART));

      JButton btnRemoveMovie = new JButton("");
      btnRemoveMovie.setAction(new RemoveMovieAction());
      panelContent.add(btnRemoveMovie, "cell 0 5,alignx right,aligny top");

      /**
       * Artwork pane
       */
      {
        JPanel artworkPanel = new JPanel();
        tabbedPane.addTab(TmmResourceBundle.getString("metatag.extraartwork"), null, artworkPanel, null);
        artworkPanel.setLayout(new MigLayout("", "[200lp:50%,grow][20lp:n][200lp:50%,grow]",
            "[][100lp:30%:30%,grow][20lp:n][][100lp:30%:30%,grow][20lp:n][][100lp:30%:30%,grow]"));
        {
          final JLabel lblClearlogoT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.clearlogo"));
          artworkPanel.add(lblClearlogoT, "cell 0 0");

          LinkLabel lblClearlogoSize = new LinkLabel();
          artworkPanel.add(lblClearlogoSize, "cell 0 0");

          JButton btnDeleteClearLogo = new FlatButton(SPACER, IconManager.DELETE_GRAY);
          btnDeleteClearLogo.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
          btnDeleteClearLogo.addActionListener(e -> {
            lblClearlogo.clearImage();
            tfClearLogo.setText("");
          });
          artworkPanel.add(btnDeleteClearLogo, "cell 0 0");

          lblClearlogo = new ImageLabel();
          lblClearlogo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              ImageChooserDialog dialog = new ImageChooserDialog(MovieSetEditorDialog.this, extractIds(), CLEARLOGO,
                  movieList.getDefaultArtworkScrapers(), lblClearlogo, MediaType.MOVIE_SET);

              dialog.setLocationRelativeTo(MainWindow.getInstance());
              dialog.setVisible(true);
              updateArtworkUrl(lblClearlogo, tfClearLogo);
            }
          });
          lblClearlogo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          artworkPanel.add(lblClearlogo, "cell 0 1,grow");
          lblClearlogo.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
              e -> setImageSizeAndCreateLink(lblClearlogoSize, lblClearlogo, btnDeleteClearLogo, MediaFileType.CLEARLOGO));
        }
        {
          JLabel lblBannerT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.banner"));
          artworkPanel.add(lblBannerT, "cell 0 3");

          LinkLabel lblBannerSize = new LinkLabel();
          artworkPanel.add(lblBannerSize, "cell 0 3");

          JButton btnDeleteBanner = new FlatButton(SPACER, IconManager.DELETE_GRAY);
          btnDeleteBanner.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
          btnDeleteBanner.addActionListener(e -> {
            lblBanner.clearImage();
            tfBanner.setText("");
          });
          artworkPanel.add(btnDeleteBanner, "cell 0 3");

          lblBanner = new ImageLabel();
          lblBanner.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              ImageChooserDialog dialog = new ImageChooserDialog(MovieSetEditorDialog.this, extractIds(), BANNER,
                  movieList.getDefaultArtworkScrapers(), lblBanner, MediaType.MOVIE_SET);

              dialog.setLocationRelativeTo(MainWindow.getInstance());
              dialog.setVisible(true);
              updateArtworkUrl(lblBanner, tfBanner);
            }
          });
          lblBanner.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          artworkPanel.add(lblBanner, "cell 0 4 3 1,grow");
          lblBanner.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
              e -> setImageSizeAndCreateLink(lblBannerSize, lblBanner, btnDeleteBanner, MediaFileType.BANNER));
        }
        {
          JLabel lblClearartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.clearart"));
          artworkPanel.add(lblClearartT, "cell 2 0");

          LinkLabel lblClearartSize = new LinkLabel();
          artworkPanel.add(lblClearartSize, "cell 2 0");

          JButton btnDeleteClearart = new FlatButton(SPACER, IconManager.DELETE_GRAY);
          btnDeleteClearart.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
          btnDeleteClearart.addActionListener(e -> {
            lblClearart.clearImage();
            tfClearArt.setText("");
          });
          artworkPanel.add(btnDeleteClearart, "cell 2 0");

          lblClearart = new ImageLabel();
          lblClearart.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              ImageChooserDialog dialog = new ImageChooserDialog(MovieSetEditorDialog.this, extractIds(), CLEARART,
                  movieList.getDefaultArtworkScrapers(), lblClearart, MediaType.MOVIE_SET);

              dialog.setLocationRelativeTo(MainWindow.getInstance());
              dialog.setVisible(true);
              updateArtworkUrl(lblClearart, tfClearArt);
            }
          });
          lblClearart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          artworkPanel.add(lblClearart, "cell 2 1,grow");
          lblClearart.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
              e -> setImageSizeAndCreateLink(lblClearartSize, lblClearart, btnDeleteClearart, MediaFileType.CLEARART));
        }
        {
          JLabel lblThumbT = new TmmLabel("Thumb");
          artworkPanel.add(lblThumbT, "cell 0 6");

          LinkLabel lblThumbSize = new LinkLabel();
          artworkPanel.add(lblThumbSize, "cell 0 6");

          JButton btnDeleteThumb = new FlatButton(SPACER, IconManager.DELETE_GRAY);
          btnDeleteThumb.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
          btnDeleteThumb.addActionListener(e -> {
            lblThumb.clearImage();
            tfThumb.setText("");
          });
          artworkPanel.add(btnDeleteThumb, "cell 0 6");

          lblThumb = new ImageLabel();
          lblThumb.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              ImageChooserDialog dialog = new ImageChooserDialog(MovieSetEditorDialog.this, extractIds(), THUMB,
                  movieList.getDefaultArtworkScrapers(), lblThumb, MediaType.MOVIE_SET);

              dialog.setLocationRelativeTo(MainWindow.getInstance());
              dialog.setVisible(true);
              updateArtworkUrl(lblThumb, tfThumb);
            }
          });
          lblThumb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          artworkPanel.add(lblThumb, "cell 0 7,grow");
          lblThumb.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
              e -> setImageSizeAndCreateLink(lblThumbSize, lblThumb, btnDeleteThumb, MediaFileType.THUMB));
        }
        {
          JLabel lblDiscT = new TmmLabel("Disc");
          artworkPanel.add(lblDiscT, "cell 2 6");

          LinkLabel lblDiscSize = new LinkLabel();
          artworkPanel.add(lblDiscSize, "cell 2 6");

          JButton btnDeleteDisc = new FlatButton(SPACER, IconManager.DELETE_GRAY);
          btnDeleteDisc.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
          btnDeleteDisc.addActionListener(e -> {
            lblDisc.clearImage();
            tfDisc.setText("");
          });
          artworkPanel.add(btnDeleteDisc, "cell 2 6");

          lblDisc = new ImageLabel();
          lblDisc.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              ImageChooserDialog dialog = new ImageChooserDialog(MovieSetEditorDialog.this, extractIds(), DISC, movieList.getDefaultArtworkScrapers(),
                  lblDisc, MediaType.MOVIE_SET);

              dialog.setLocationRelativeTo(MainWindow.getInstance());
              dialog.setVisible(true);
              updateArtworkUrl(lblDisc, tfDisc);
            }
          });
          lblDisc.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          artworkPanel.add(lblDisc, "cell 2 7,grow");
          lblDisc.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
              e -> setImageSizeAndCreateLink(lblDiscSize, lblDisc, btnDeleteDisc, MediaFileType.DISC));
        }
      }

      /**
       * Artwork url pane
       */
      {
        JPanel artworkAndTrailerPanel = new JPanel();
        tabbedPane.addTab(TmmResourceBundle.getString("edit.artwork"), null, artworkAndTrailerPanel, null);
        artworkAndTrailerPanel.setLayout(new MigLayout("", "[][grow]", "[][][][][][][]"));
        {
          JLabel lblPosterT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.poster"));
          artworkAndTrailerPanel.add(lblPosterT, "cell 0 0,alignx right");
        }
        {
          tfPoster = new JTextField();
          artworkAndTrailerPanel.add(tfPoster, "cell 1 0,growx");

          JLabel lblFanartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.fanart"));
          artworkAndTrailerPanel.add(lblFanartT, "cell 0 1,alignx right");

          tfFanart = new JTextField();
          artworkAndTrailerPanel.add(tfFanart, "cell 1 1,growx");
        }
        {
          JLabel lblClearLogoT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.clearlogo"));
          artworkAndTrailerPanel.add(lblClearLogoT, "cell 0 2,alignx right");

          tfClearLogo = new JTextField();
          artworkAndTrailerPanel.add(tfClearLogo, "cell 1 2,growx");
        }
        {
          JLabel lblBannerT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.banner"));
          artworkAndTrailerPanel.add(lblBannerT, "cell 0 3,alignx right");

          tfBanner = new JTextField();
          artworkAndTrailerPanel.add(tfBanner, "cell 1 3,growx");
        }
        {
          JLabel lblClearArtT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.clearart"));
          artworkAndTrailerPanel.add(lblClearArtT, "cell 0 4,alignx right");

          tfClearArt = new JTextField();
          artworkAndTrailerPanel.add(tfClearArt, "cell 1 4,growx");
        }
        {
          JLabel lblThumbT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.thumb"));
          artworkAndTrailerPanel.add(lblThumbT, "cell 0 5,alignx right");

          tfThumb = new JTextField();
          artworkAndTrailerPanel.add(tfThumb, "cell 1 5,growx");
        }
        {
          JLabel lblDiscT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.disc"));
          artworkAndTrailerPanel.add(lblDiscT, "cell 0 6,alignx trailing");

          tfDisc = new JTextField();
          artworkAndTrailerPanel.add(tfDisc, "cell 1 6,growx");
        }
      }
    }
    /**
     * Button pane
     */
    {
      if (queueSize > 1) {
        JButton btnAbort = new JButton(new AbortQueueAction(TmmResourceBundle.getString("Button.abortqueue")));
        addButton(btnAbort);
        if (queueIndex > 0) {
          JButton backButton = new JButton(new NavigateBackAction());
          addButton(backButton);
        }
      }

      JButton btnCancel = new JButton(new CancelAction());
      addButton(btnCancel);

      JButton btnOk = new JButton(new OkAction());
      getRootPane().registerKeyboardAction(new OkAction(), KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
          JComponent.WHEN_IN_FOCUSED_WINDOW);
      addDefaultButton(btnOk);
    }

    {
      tfName.setText(movieSetToEdit.getTitle());
      tfTmdbId.setText(String.valueOf(movieSetToEdit.getTmdbId()));
      taPlot.setText(movieSetToEdit.getPlot());
      taNote.setText(movieSetToEdit.getNote());
      moviesInSet.addAll(movieSetToEdit.getMovies());

      setArtworkPath(MediaFileType.POSTER, lblPoster);
      setArtworkPath(MediaFileType.FANART, lblFanart);
      setArtworkPath(MediaFileType.BANNER, lblBanner);
      setArtworkPath(MediaFileType.CLEARLOGO, lblClearlogo);
      setArtworkPath(MediaFileType.CLEARART, lblClearart);
      setArtworkPath(MediaFileType.THUMB, lblThumb);
      setArtworkPath(MediaFileType.DISC, lblDisc);

      tfPoster.setText(movieSetToEdit.getArtworkUrl(MediaFileType.POSTER));
      tfFanart.setText(movieSetToEdit.getArtworkUrl(MediaFileType.FANART));
      tfClearLogo.setText(movieSetToEdit.getArtworkUrl(MediaFileType.CLEARLOGO));
      tfClearArt.setText(movieSetToEdit.getArtworkUrl(MediaFileType.CLEARART));
      tfThumb.setText(movieSetToEdit.getArtworkUrl(MediaFileType.THUMB));
      tfDisc.setText(movieSetToEdit.getArtworkUrl(MediaFileType.DISC));
      tfBanner.setText(movieSetToEdit.getArtworkUrl(MediaFileType.BANNER));
    }

    initDataBindings();

    // adjust table columns
    // name column
    tableMovies.getTableHeader().getColumnModel().getColumn(0).setHeaderValue(TmmResourceBundle.getString("metatag.name"));

    // year column
    int width = (int) (tableMovies.getFontMetrics(tableMovies.getFont()).stringWidth("2000") * 1.3f + 10);
    int titleWidth = tableMovies.getFontMetrics(tableMovies.getFont()).stringWidth(TmmResourceBundle.getString("metatag.year"));
    if (titleWidth > width) {
      width = titleWidth;
    }
    tableMovies.getTableHeader().getColumnModel().getColumn(1).setPreferredWidth(width);
    tableMovies.getTableHeader().getColumnModel().getColumn(1).setMinWidth(width);
    tableMovies.getTableHeader().getColumnModel().getColumn(1).setMaxWidth((int) (width * 1.1f));
    tableMovies.getTableHeader().getColumnModel().getColumn(1).setResizable(false);
    tableMovies.getTableHeader().getColumnModel().getColumn(1).setHeaderValue(TmmResourceBundle.getString("metatag.year"));

    // watched column
    tableMovies.getTableHeader().getColumnModel().getColumn(2).setPreferredWidth(70);
    tableMovies.getTableHeader().getColumnModel().getColumn(2).setMinWidth(70);
    tableMovies.getTableHeader().getColumnModel().getColumn(2).setMaxWidth(85);
    tableMovies.getTableHeader().getColumnModel().getColumn(2).setResizable(false);
    tableMovies.getTableHeader().getColumnModel().getColumn(2).setHeaderValue(TmmResourceBundle.getString("metatag.watched"));

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

  private Map<String, Object> extractIds() {
    HashMap<String, Object> ids = new HashMap<>(movieSetToEdit.getIds());

    try {
      // try to parse the TMDB set id
      ids.put(MediaMetadata.TMDB_SET, Integer.parseInt(tfTmdbId.getText()));
    }
    catch (Exception ignored) {
      // nothing to do here
    }

    return ids;
  }

  private void updateArtworkUrl(ImageLabel imageLabel, JTextField textField) {
    if (StringUtils.isNotBlank(imageLabel.getImageUrl())) {
      textField.setText(imageLabel.getImageUrl());
    }
  }

  private void setArtworkPath(MediaFileType type, ImageLabel imageLabel) {
    if (StringUtils.isNotBlank(movieSetToEdit.getArtworkFilename(type))) {
      imageLabel.setImagePath(movieSetToEdit.getArtworkFilename(type));
    }
    else {
      imageLabel.setImageUrl(movieSetToEdit.getArtworkUrl(type));
    }
  }

  private class RemoveMovieAction extends AbstractAction {
    RemoveMovieAction() {
      putValue(LARGE_ICON_KEY, IconManager.REMOVE_INV);
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movieset.movie.remove"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (moviesInSet.isEmpty()) {
        return;
      }

      int row = tableMovies.getSelectedRow();
      if (row > -1) {
        Movie movie = moviesInSet.get(row);
        moviesInSet.remove(row);
        removedMovies.add(movie);
      }
    }
  }

  private class OkAction extends AbstractAction {
    OkAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.save"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("Button.save"));
      putValue(SMALL_ICON, IconManager.APPLY_INV);
      putValue(LARGE_ICON_KEY, IconManager.APPLY_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (StringUtils.isBlank(tfName.getText())) {
        tfName.requestFocusInWindow();
        return;
      }

      movieSetToEdit.setTitle(tfName.getText());
      movieSetToEdit.setPlot(taPlot.getText());
      movieSetToEdit.setNote(taNote.getText());

      // process artwork
      processArtwork(MediaFileType.POSTER, lblPoster, tfPoster);
      processArtwork(MediaFileType.FANART, lblFanart, tfFanart);
      processArtwork(MediaFileType.CLEARLOGO, lblClearlogo, tfClearLogo);
      processArtwork(MediaFileType.BANNER, lblBanner, tfBanner);
      processArtwork(MediaFileType.CLEARART, lblClearart, tfClearArt);
      processArtwork(MediaFileType.THUMB, lblThumb, tfThumb);
      processArtwork(MediaFileType.DISC, lblDisc, tfDisc);

      // delete movies
      for (int i = movieSetToEdit.getMovies().size() - 1; i >= 0; i--) {
        Movie movie = movieSetToEdit.getMovies().get(i);
        if (!moviesInSet.contains(movie)) {
          movie.setMovieSet(null);
          movie.writeNFO();
          movie.saveToDb();
          movieSetToEdit.removeMovie(movie, true);
        }
      }

      // sort movies in the right order
      for (Movie movie : moviesInSet) {
        movie.saveToDb();
      }

      // remove removed movies
      for (Movie movie : removedMovies) {
        movie.removeFromMovieSet();
        movie.saveToDb();
        movie.writeNFO();
        movieSetToEdit.removeMovie(movie, true);
      }

      // and rewrite NFO
      for (Movie movie : moviesInSet) {
        movie.writeNFO();
      }

      int tmdbId = 0;
      try {

        tmdbId = Integer.parseInt(tfTmdbId.getText());
      }
      catch (Exception ignored) {
      }
      movieSetToEdit.setTmdbId(tmdbId);
      movieSetToEdit.writeNFO();
      movieSetToEdit.saveToDb();

      MovieSetArtworkHelper.cleanupArtwork(movieSetToEdit);

      setVisible(false);
    }
  }

  private void processArtwork(MediaFileType type, ImageLabel imageLabel, JTextField textField) {
    if (StringUtils.isAllBlank(imageLabel.getImagePath(), imageLabel.getImageUrl())
        && StringUtils.isNotBlank(movieSetToEdit.getArtworkFilename(type))) {
      // artwork has been explicitly deleted - we need to remove the artwork url too, since this is a fallback
      movieSetToEdit.removeArtworkUrl(type);
      movieSetToEdit.deleteMediaFiles(type);
    }

    if (StringUtils.isNotEmpty(textField.getText()) && !textField.getText().equals(movieSetToEdit.getArtworkUrl(type))) {
      // artwork url and textfield do not match -> redownload
      movieSetToEdit.setArtworkUrl(textField.getText(), type);
    }
    else if (StringUtils.isEmpty(textField.getText())) {
      // remove the artwork url
      movieSetToEdit.removeArtworkUrl(type);
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

  protected void initDataBindings() {
    JTableBinding<Movie, List<Movie>, JTable> jTableBinding = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, moviesInSet, tableMovies);
    //
    BeanProperty<Movie, String> movieBeanProperty = BeanProperty.create("title");
    jTableBinding.addColumnBinding(movieBeanProperty).setEditable(false);
    //
    BeanProperty<Movie, Integer> movieBeanProperty_1 = BeanProperty.create("year");
    jTableBinding.addColumnBinding(movieBeanProperty_1).setEditable(false);
    //
    BeanProperty<Movie, Boolean> movieBeanProperty_2 = BeanProperty.create("watched");
    jTableBinding.addColumnBinding(movieBeanProperty_2).setEditable(false).setColumnClass(Boolean.class);
    //
    jTableBinding.setEditable(false);
    jTableBinding.bind();
    //
    bindingGroup = new BindingGroup();
    //
    bindingGroup.addBinding(jTableBinding);
  }
}
