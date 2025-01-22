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

import static org.tinymediamanager.core.Constants.FANART;
import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.POSTER;
import static org.tinymediamanager.ui.moviesets.MovieSetSelectionModel.SELECTED_MOVIE_SET;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.ColumnLayout;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.LinkTextArea;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.ReadOnlyTextPaneHTML;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.moviesets.MovieInMovieSetTableFormat;
import org.tinymediamanager.ui.moviesets.MovieSetSelectionModel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieSetInformationPanel.
 * 
 * @author Manuel Laggner
 */
public class MovieSetInformationPanel extends JPanel {
  private static final Logger          LOGGER = LoggerFactory.getLogger(MovieSetInformationPanel.class);

  private final MovieSetSelectionModel selectionModel;
  private final EventList<Movie>       movieEventList;

  private JLabel                       lblMovieSetName;
  private ImageLabel                   lblFanart;
  private JLabel                       lblFanartSize;
  private ImageLabel                   lblPoster;
  private JLabel                       lblPosterSize;
  private JTextPane                    taOverview;
  private JLabel                       lblYear;
  private LinkLabel                    lblTmdbid;
  private LinkTextArea                 taArtworkPath1;
  private LinkTextArea                 taArtworkPath2;
  private JTextPane                    tpNote;

  public MovieSetInformationPanel(MovieSetSelectionModel setSelectionModel) {
    this.selectionModel = setSelectionModel;

    movieEventList = new ObservableElementList<>(GlazedListsSwing.swingThreadProxyList(new BasicEventList<>()),
        GlazedLists.beanConnector(Movie.class));

    initComponents();

    // beansbinding init
    initDataBindings();

    // manual coded binding
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();
      // react on selection/change of a movie set

      if (source.getClass() != MovieSetSelectionModel.class) {
        return;
      }

      MovieSet movieSet = selectionModel.getSelectedMovieSet();

      if (SELECTED_MOVIE_SET.equals(property) || Constants.ADDED_MOVIE.equals(property) || Constants.REMOVED_MOVIE.equals(property)) {
        movieEventList.clear();
        movieEventList.addAll(movieSet.getMovies());
        lblYear.setText(movieSet.getYears());
      }

      if (SELECTED_MOVIE_SET.equals(property) || MEDIA_FILES.equals(property)) {
        setArtworkPath(movieSet);
      }

      if (SELECTED_MOVIE_SET.equals(property) || POSTER.equals(property)) {
        setPoster(movieSet);
      }

      if (SELECTED_MOVIE_SET.equals(property) || FANART.equals(property)) {
        setFanart(movieSet);
      }

    };

    selectionModel.addPropertyChangeListener(propertyChangeListener);

    // action listeners
    lblTmdbid.addActionListener(arg0 -> {
      String url = "https://www.themoviedb.org/collection/" + lblTmdbid.getText();
      try {
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception e) {
        LOGGER.error("browse to tmdbid", e);
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));
      }
    });
    taArtworkPath1.addActionListener(arg0 -> {
      if (!StringUtils.isEmpty(taArtworkPath1.getText())) {
        // get the location from the label
        Path path = Paths.get(taArtworkPath1.getText());
        TmmUIHelper.openFolder(path);
      }
    });
    taArtworkPath2.addActionListener(arg0 -> {
      if (!StringUtils.isEmpty(taArtworkPath2.getText())) {
        // get the location from the label
        Path path = Paths.get(taArtworkPath2.getText());
        TmmUIHelper.openFolder(path);
      }
    });
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[n:100lp:20%, grow][300lp:300lp,grow 350]", "[grow]"));
    {
      JPanel panelLeft = new JPanel();
      panelLeft.setLayout(new ColumnLayout());
      add(panelLeft, "cell 0 0,grow");

      lblPoster = new ImageLabel(false, false, true);
      lblPoster.setDesiredAspectRatio(2 / 3f);
      lblPoster.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      panelLeft.add(lblPoster);
      lblPoster.enableLightbox();
      lblPosterSize = new JLabel(TmmResourceBundle.getString("mediafiletype.poster"));
      panelLeft.add(lblPosterSize);
      panelLeft.add(Box.createVerticalStrut(20));

      lblFanart = new ImageLabel(false, false, true);
      lblFanart.setDesiredAspectRatio(16 / 9f);
      lblFanart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      panelLeft.add(lblFanart);
      lblFanart.enableLightbox();
      lblFanartSize = new JLabel(TmmResourceBundle.getString("mediafiletype.fanart"));
      panelLeft.add(lblFanartSize);
      panelLeft.add(Box.createVerticalStrut(20));
    }
    {
      JPanel panelRight = new JPanel();
      add(panelRight, "cell 1 0,grow");
      panelRight.setLayout(new MigLayout("insets 0 n n n, hidemode 2", "[450lp,grow]",
          "[][shrink 0][][shrink 0][][250lp:350lp,grow][shrink 0][][shrink 0][20lp!][][350lp,grow]"));

      {
        lblMovieSetName = new JLabel("");
        panelRight.add(lblMovieSetName, "cell 0 0, wmin 0");
        TmmFontHelper.changeFont(lblMovieSetName, 1.33, Font.BOLD);
      }
      {
        panelRight.add(new JSeparator(), "cell 0 1,growx");
      }
      {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("insets 0", "[][grow]", "[][]"));
        panelRight.add(panel, "cell 0 2,grow");

        JLabel lblYearT = new TmmLabel(TmmResourceBundle.getString("metatag.year"));
        panel.add(lblYearT, "cell 0 0");

        lblYear = new JLabel("");
        panel.add(lblYear, "cell 1 0");

        JLabel lblTmdbidT = new TmmLabel(TmmResourceBundle.getString("metatag.tmdb"));
        panel.add(lblTmdbidT, "cell 0 1");

        lblTmdbid = new LinkLabel();
        panel.add(lblTmdbid, "cell 1 1");
      }
      {
        JSeparator separator = new JSeparator();
        panelRight.add(separator, "cell 0 3,growx");
      }
      {
        JLabel lblPlot = new JLabel(TmmResourceBundle.getString("metatag.plot"));
        panelRight.add(lblPlot, "cell 0 4");
        TmmFontHelper.changeFont(lblPlot, Font.BOLD);

        JScrollPane scrollPaneOverview = new NoBorderScrollPane();
        panelRight.add(scrollPaneOverview, "cell 0 5,grow");

        taOverview = new ReadOnlyTextPaneHTML();
        scrollPaneOverview.setViewportView(taOverview);
      }

      JSeparator separator = new JSeparator();
      panelRight.add(separator, "cell 0 6,growx");
      {
        JPanel panelBottom = new JPanel();
        panelRight.add(panelBottom, "cell 0 7,grow");
        panelBottom.setLayout(new MigLayout("insets 0, hidemode 3", "[][grow]", "[][grow]"));

        JLabel lblArtworkT = new TmmLabel(TmmResourceBundle.getString("metatag.artwork"));
        panelBottom.add(lblArtworkT, "cell 0 0,aligny top");

        taArtworkPath1 = new LinkTextArea();
        panelBottom.add(taArtworkPath1, "flowy,cell 1 0,growx,wmin 0");

        taArtworkPath2 = new LinkTextArea();
        panelBottom.add(taArtworkPath2, "cell 1 0,growx,wmin 0");

        JLabel lblNoteT = new TmmLabel(TmmResourceBundle.getString("metatag.note"));
        panelBottom.add(lblNoteT, "cell 0 1");

        tpNote = new ReadOnlyTextPaneHTML();
        panelBottom.add(tpNote, "cell 1 1,growx,wmin 0");
      }
      {
        panelRight.add(new JSeparator(), "cell 0 8,growx");
      }
      {
        TmmTable tableAssignedMovies = new TmmTable(new TmmTableModel<>(movieEventList, new MovieInMovieSetTableFormat()));
        tableAssignedMovies.setName("movieSets.movieTable");
        TmmUILayoutStore.getInstance().install(tableAssignedMovies);
        tableAssignedMovies.adjustColumnPreferredWidths(3);

        JLabel lblMoviesT = new TmmLabel(TmmResourceBundle.getString("tmm.movies"));
        panelRight.add(lblMoviesT, "cell 0 10");

        JScrollPane scrollPane = new JScrollPane();
        tableAssignedMovies.configureScrollPane(scrollPane);
        panelRight.add(scrollPane, "cell 0 11,grow");
      }
    }
  }

  private void setPoster(MovieSet movieSet) {
    lblPoster.clearImage();
    lblPoster.setImagePath(movieSet.getArtworkFilename(MediaFileType.POSTER));
    Dimension posterSize = movieSet.getArtworkDimension(MediaFileType.POSTER);
    if (posterSize.width > 0 && posterSize.height > 0) {
      lblPosterSize.setText(TmmResourceBundle.getString("mediafiletype.poster") + " - " + posterSize.width + "x" + posterSize.height);
    }
    else {
      if (StringUtils.isNotBlank(lblPoster.getImagePath())) {
        // do this async to prevent lockups of the UI
        ImageSizeLoader loader = new ImageSizeLoader(lblPoster.getImagePath(), "poster", lblPosterSize);
        loader.execute();
      }
      else {
        lblPosterSize.setText(TmmResourceBundle.getString("mediafiletype.poster"));
      }
    }
  }

  private void setFanart(MovieSet movieSet) {
    lblFanart.clearImage();
    lblFanart.setImagePath(movieSet.getArtworkFilename(MediaFileType.FANART));
    Dimension fanartSize = movieSet.getArtworkDimension(MediaFileType.FANART);
    if (fanartSize.width > 0 && fanartSize.height > 0) {
      lblFanartSize.setText(TmmResourceBundle.getString("mediafiletype.fanart") + " - " + fanartSize.width + "x" + fanartSize.height);
    }
    else {
      if (StringUtils.isNotBlank(lblFanart.getImagePath())) {
        // do this async to prevent lockups of the UI
        ImageSizeLoader loader = new ImageSizeLoader(lblFanart.getImagePath(), "fanart", lblFanartSize);
        loader.execute();
      }
      else {
        lblFanartSize.setText(TmmResourceBundle.getString("mediafiletype.fanart"));
      }
    }
  }

  private void setArtworkPath(MovieSet movieSet) {
    String artworkPath = MovieModuleManager.getInstance().getSettings().getMovieSetDataFolder();
    if (StringUtils.isBlank(artworkPath)) {
      taArtworkPath1.setVisible(false);
      taArtworkPath2.setVisible(false);
      return;
    }

    String artworkPath1 = "";
    String artworkPath2 = "";

    for (MediaFile mediaFile : movieSet.getMediaFiles()) {
      if (!mediaFile.isGraphic()) {
        continue;
      }

      String mfPath = mediaFile.getFileAsPath().getParent().toAbsolutePath().toString();

      if (mfPath.startsWith(artworkPath)) {
        if (StringUtils.isBlank(artworkPath1)) {
          artworkPath1 = mfPath;
          continue;
        }
        if (StringUtils.isBlank(artworkPath2) && !mfPath.equals(artworkPath1)) {
          artworkPath2 = mfPath;
          break;
        }
      }
    }

    if (StringUtils.isNotBlank(artworkPath1)) {
      taArtworkPath1.setVisible(true);
      taArtworkPath1.setText(artworkPath1);
    }
    else {
      taArtworkPath1.setVisible(false);
    }

    if (StringUtils.isNotBlank(artworkPath2)) {
      taArtworkPath2.setVisible(true);
      taArtworkPath2.setText(artworkPath2);
    }
    else {
      taArtworkPath2.setVisible(false);
    }
  }

  private static class ImageSizeLoader extends SwingWorker<Void, Void> {
    private final String path;
    private final String type;
    private final JLabel label;

    private int          width;
    private int          height;

    public ImageSizeLoader(String path, String type, JLabel label) {
      this.path = path;
      this.type = type;
      this.label = label;
    }

    @Override
    protected Void doInBackground() {
      if (!Files.exists(Paths.get(path))) {
        return null;
      }

      try {
        BufferedImage img = ImageIO.read(new File(path));
        this.width = img.getWidth();
        this.height = img.getHeight();
      }
      catch (Exception e) {
        LOGGER.warn("Could not read {} dimensions: {}", "mediafiletype." + type, e.getMessage());
      }
      return null;
    }

    @Override
    protected void done() {
      super.done();
      if (width > 0 && height > 0) {
        label.setText(TmmResourceBundle.getString("mediafiletype." + type) + " - " + width + "x" + height);
      }
      else {
        label.setText(TmmResourceBundle.getString("mediafiletype." + type)); // $NON-NLS-1$
      }
    }
  }

  protected void initDataBindings() {
    Property tvShowSelectionModelBeanProperty = BeanProperty.create("selectedMovieSet.title");
    Property jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, selectionModel, tvShowSelectionModelBeanProperty, lblMovieSetName,
        jLabelBeanProperty);
    autoBinding.bind();
    //
    Property tvShowSelectionModelBeanProperty_1 = BeanProperty.create("selectedMovieSet.plot");
    Property JTextPaneBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, selectionModel, tvShowSelectionModelBeanProperty_1, taOverview,
        JTextPaneBeanProperty);
    autoBinding_1.bind();
    //
    Property movieSetSelectionModelBeanProperty = BeanProperty.create("selectedMovieSet.tmdbId");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ, selectionModel, movieSetSelectionModelBeanProperty, lblTmdbid,
        jLabelBeanProperty);
    autoBinding_2.bind();
    //
    Property movieSetSelectionModelBeanProperty_1 = BeanProperty.create("selectedMovieSet.note");
    Property jTextAreaBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ, selectionModel, movieSetSelectionModelBeanProperty_1, tpNote,
        jTextAreaBeanProperty);
    autoBinding_3.bind();
  }
}
