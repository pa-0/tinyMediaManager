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
package org.tinymediamanager.ui.movies.panels;

import static org.tinymediamanager.core.Constants.BANNER;
import static org.tinymediamanager.core.Constants.CLEARLOGO;
import static org.tinymediamanager.core.Constants.FANART;
import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;
import static org.tinymediamanager.core.Constants.POSTER;
import static org.tinymediamanager.core.Constants.RATING;
import static org.tinymediamanager.core.Constants.THUMB;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.ui.ColumnLayout;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.LinkTextArea;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.ReadOnlyTextPaneHTML;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.converter.CertificationImageConverter;
import org.tinymediamanager.ui.converter.LockedConverter;
import org.tinymediamanager.ui.converter.RuntimeConverter;
import org.tinymediamanager.ui.converter.ZeroIdConverter;
import org.tinymediamanager.ui.movies.MovieSelectionModel;
import org.tinymediamanager.ui.panels.InformationPanel;
import org.tinymediamanager.ui.panels.MediaInformationLogosPanel;
import org.tinymediamanager.ui.panels.RatingPanel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieInformationPanel.
 * 
 * @author Manuel Laggner
 */
public class MovieInformationPanel extends InformationPanel {
  private static final Logger        LOGGER                 = LoggerFactory.getLogger(MovieInformationPanel.class);
  private static final String        LAYOUT_ARTWORK_VISIBLE = "[n:100lp:20%, grow][300lp:300lp,grow 350]";
  private static final String        LAYOUT_ARTWORK_HIDDEN  = "[][300lp:300lp,grow 350]";

  private final MovieSelectionModel  movieSelectionModel;

  /** UI components */
  private RatingPanel                ratingPanel;
  private JLabel                     lblMovieName;
  private JLabel                     lblTagline;
  private JLabel                     lblYear;
  private LinkLabel                  lblImdbid;
  private JLabel                     lblRunningTime;
  private LinkLabel                  lblTmdbid;
  private JTextPane                  taGenres;
  private JTextPane                  taPlot;
  private JLabel                     lblCertification;
  private JTextPane                  taOtherIds;
  private MediaInformationLogosPanel panelLogos;
  private JLabel                     lblOriginalTitle;
  private JButton                    btnPlay;
  private JScrollPane                scrollPane;
  private JTextPane                  taProduction;
  private JTextPane                  taTags;
  private JLabel                     lblEdition;
  private LinkTextArea               lblMoviePath;
  private JLabel                     lblMovieSet;
  private JLabel                     lblSpokenLanguages;
  private JLabel                     lblCountry;
  private JLabel                     lblReleaseDate;
  private JTextPane                  taNote;
  private JLabel                     lblCertificationLogo;
  private LinkLabel                  lblTraktTvId;
  private JLabel                     lblShowlink;

  /**
   * Instantiates a new movie information panel.
   * 
   * @param movieSelectionModel
   *          the movie selection model
   */
  public MovieInformationPanel(MovieSelectionModel movieSelectionModel) {
    this.movieSelectionModel = movieSelectionModel;

    initComponents();

    // beansbinding init
    initDataBindings();

    // action listeners
    lblTmdbid.addActionListener(arg0 -> {
      String url = "https://www.themoviedb.org/movie/" + lblTmdbid.getText();
      try {
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception e) {
        LOGGER.error("browse to tmdbid", e);
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));
      }
    });

    lblImdbid.addActionListener(arg0 -> {
      String url = "https://www.imdb.com/title/" + lblImdbid.getText();
      try {
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception e) {
        LOGGER.error("browse to imdbid", e);
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));
      }
    });

    lblTraktTvId.addActionListener(arg0 -> {
      String url = "https://trakt.tv/search/trakt/" + lblTraktTvId.getText() + "?id_type=movie";
      try {
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception e) {
        LOGGER.error("browse to trakt.tv", e);
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));
      }
    });

    lblMoviePath.addActionListener(arg0 -> {
      if (StringUtils.isNotEmpty(lblMoviePath.getLink())) {
        // get the location from the label
        Path path = Paths.get(lblMoviePath.getLink());
        TmmUIHelper.openFolder(path);
      }
    });

    // manual coded binding
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();
      // react on selection of a movie and change of a movie

      if (source.getClass() != MovieSelectionModel.class) {
        return;
      }

      MovieSelectionModel selectionModel = (MovieSelectionModel) source;
      Movie movie = selectionModel.getSelectedMovie();

      if ("selectedMovie".equals(property) || POSTER.equals(property)) {
        setArtwork(movie, MediaFileType.POSTER);
      }

      if ("selectedMovie".equals(property) || FANART.equals(property)) {
        setArtwork(movie, MediaFileType.FANART);
      }

      if ("selectedMovie".equals(property) || BANNER.equals(property)) {
        setArtwork(movie, MediaFileType.BANNER);
      }

      if ("selectedMovie".equals(property) || THUMB.equals(property)) {
        setArtwork(movie, MediaFileType.THUMB);
      }

      if ("selectedMovie".equals(property) || CLEARLOGO.equals(property)) {
        setArtwork(movie, MediaFileType.CLEARLOGO);
      }

      if ("selectedMovie".equals(property) || MEDIA_FILES.equals(property) || MEDIA_INFORMATION.equals(property)) {
        panelLogos.setMediaInformationSource(movie);
      }

      if ("selectedMovie".equals(property)) {
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
      }

      if ("selectedMovie".equals(property) || RATING.equals(property)) {
        setRating(movie);
      }
    };

    movieSelectionModel.addPropertyChangeListener(propertyChangeListener);

    btnPlay.addActionListener(e -> {
      MediaFile mf = movieSelectionModel.getSelectedMovie().getMainVideoFile();
      if (StringUtils.isNotBlank(mf.getFilename())) {
        try {
          TmmUIHelper.openFile(MediaFileHelper.getMainVideoFile(mf));
        }
        catch (Exception ex) {
          LOGGER.error("open file", ex);
          MessageManager.instance
              .pushMessage(new Message(Message.MessageLevel.ERROR, mf, "message.erroropenfile", new String[] { ":", ex.getLocalizedMessage() }));
        }
      }
    });
  }

  private void initComponents() {
    setLayout(new MigLayout("hidemode 3", LAYOUT_ARTWORK_VISIBLE, "[][grow]"));

    {
      JPanel panelLeft = new JPanel();
      panelLeft.setLayout(new ColumnLayout());
      add(panelLeft, "cell 0 0 1 2, grow");

      for (Component component : generateArtworkComponents(MediaFileType.POSTER)) {
        panelLeft.add(component);
      }

      for (Component component : generateArtworkComponents(MediaFileType.FANART)) {
        panelLeft.add(component);
      }

      for (Component component : generateArtworkComponents(MediaFileType.BANNER)) {
        panelLeft.add(component);
      }

      for (Component component : generateArtworkComponents(MediaFileType.THUMB)) {
        panelLeft.add(component);
      }

      for (Component component : generateArtworkComponents(MediaFileType.CLEARLOGO)) {
        panelLeft.add(component);
      }
    }
    {
      JPanel panelTitle = new JPanel();
      add(panelTitle, "cell 1 0,grow");
      panelTitle.setLayout(new MigLayout("insets 0 0 n n", "[grow][]", "[][][shrink 0]"));

      {
        lblMovieName = new TmmLabel("", 1.33);
        panelTitle.add(lblMovieName, "flowx,cell 0 0,wmin 0,growx");
      }
      {
        btnPlay = new FlatButton(IconManager.PLAY_LARGE);
        panelTitle.add(btnPlay, "cell 1 0 1 2,aligny top");
      }
      {
        lblOriginalTitle = new JLabel("");
        panelTitle.add(lblOriginalTitle, "cell 0 1,growx,wmin 0");
      }
      {
        panelTitle.add(new JSeparator(), "cell 0 2 2 1,growx");
      }
    }
    {
      JPanel panelRight = new JPanel();
      panelRight
          .setLayout(new MigLayout("insets n 0 n n, hidemode 3", "[100lp,grow]", "[shrink 0][][shrink 0][][][][][shrink 0][][grow,top][shrink 0][]"));

      scrollPane = new NoBorderScrollPane(panelRight);
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      scrollPane.getVerticalScrollBar().setUnitIncrement(8);
      add(scrollPane, "cell 1 1,grow, wmin 0");

      {
        JPanel panelTopDetails = new JPanel();
        panelRight.add(panelTopDetails, "cell 0 0,grow");
        panelTopDetails.setLayout(new MigLayout("insets 0", "[][][40lp!][][grow][]", "[]2lp[]2lp[grow]2lp[]2lp[]2lp[]2lp[]2lp[]"));

        {
          JLabel lblYearT = new TmmLabel(TmmResourceBundle.getString("metatag.year"));
          panelTopDetails.add(lblYearT, "cell 0 0");

          lblYear = new JLabel("");
          panelTopDetails.add(lblYear, "cell 1 0,growx");
        }
        {
          JLabel lblImdbIdT = new TmmLabel(TmmResourceBundle.getString("metatag.imdb"));
          panelTopDetails.add(lblImdbIdT, "cell 3 0");

          lblImdbid = new LinkLabel("");
          panelTopDetails.add(lblImdbid, "cell 4 0");
        }
        {
          lblCertificationLogo = new JLabel("");
          panelTopDetails.add(lblCertificationLogo, "cell 5 0 1 3, top");
        }
        {
          JLabel lblReleaseDateT = new TmmLabel(TmmResourceBundle.getString("metatag.releasedate"));
          panelTopDetails.add(lblReleaseDateT, "cell 0 1");

          lblReleaseDate = new JLabel("");
          panelTopDetails.add(lblReleaseDate, "cell 1 1");
        }
        {
          JLabel lblTmdbIdT = new TmmLabel(TmmResourceBundle.getString("metatag.tmdb"));
          panelTopDetails.add(lblTmdbIdT, "cell 3 1");

          lblTmdbid = new LinkLabel("");
          panelTopDetails.add(lblTmdbid, "cell 4 1");
        }
        {
          JLabel lblCertificationT = new TmmLabel(TmmResourceBundle.getString("metatag.certification"));
          panelTopDetails.add(lblCertificationT, "cell 0 2");

          lblCertification = new JLabel("");
          panelTopDetails.add(lblCertification, "cell 1 2,growx");
        }
        {
          JLabel lblTraktTvIdT = new TmmLabel("Trakt.tv ID");
          panelTopDetails.add(lblTraktTvIdT, "cell 3 2");

          lblTraktTvId = new LinkLabel();
          panelTopDetails.add(lblTraktTvId, "cell 4 2");
        }
        {
          JLabel lblOtherIdsT = new TmmLabel(TmmResourceBundle.getString("metatag.otherids"));
          panelTopDetails.add(lblOtherIdsT, "cell 3 3");

          taOtherIds = new ReadOnlyTextPane();
          panelTopDetails.add(taOtherIds, "cell 4 3 2 1,growx,wmin 0");
        }
        {
          JLabel lblRunningTimeT = new TmmLabel(TmmResourceBundle.getString("metatag.runtime"));
          panelTopDetails.add(lblRunningTimeT, "cell 0 3,aligny top");

          lblRunningTime = new JLabel("");
          panelTopDetails.add(lblRunningTime, "cell 1 3,aligny top");
        }
        {
          JLabel lblGenresT = new TmmLabel(TmmResourceBundle.getString("metatag.genre"));
          panelTopDetails.add(lblGenresT, "cell 0 4");

          taGenres = new ReadOnlyTextPane();
          panelTopDetails.add(taGenres, "cell 1 4 5 1,growx,wmin 0");
        }
        {
          JLabel lblProductionT = new TmmLabel(TmmResourceBundle.getString("metatag.production"));
          panelTopDetails.add(lblProductionT, "cell 0 5");

          taProduction = new ReadOnlyTextPane();
          panelTopDetails.add(taProduction, "cell 1 5 5 1,growx,wmin 0");
        }
        {
          JLabel lblCountryT = new TmmLabel(TmmResourceBundle.getString("metatag.country"));
          panelTopDetails.add(lblCountryT, "cell 0 6");

          lblCountry = new JLabel("");
          panelTopDetails.add(lblCountry, "cell 1 6 5 1,wmin 0");
        }
        {
          JLabel lblSpokenLanguagesT = new TmmLabel(TmmResourceBundle.getString("metatag.spokenlanguages"));
          panelTopDetails.add(lblSpokenLanguagesT, "cell 0 7");

          lblSpokenLanguages = new JLabel("");
          panelTopDetails.add(lblSpokenLanguages, "cell 1 7 5 1,wmin 0");
        }
      }

      {
        panelRight.add(new JSeparator(), "cell 0 1,growx");
      }

      {
        ratingPanel = new RatingPanel();
        panelRight.add(ratingPanel, "flowx,cell 0 2,aligny center");
      }

      {
        JSeparator sepLogos = new JSeparator();
        panelRight.add(sepLogos, "cell 0 3,growx");
      }

      {
        panelLogos = new MediaInformationLogosPanel();
        panelRight.add(panelLogos, "cell 0 4,growx");
      }

      {
        panelRight.add(new JSeparator(), "cell 0 5,growx");
      }

      {
        JLabel lblTaglineT = new TmmLabel(TmmResourceBundle.getString("metatag.tagline"));
        panelRight.add(lblTaglineT, "cell 0 6,alignx left,aligny top");

        lblTagline = new JLabel();
        panelRight.add(lblTagline, "cell 0 7,growx,wmin 0,aligny top");
      }

      {
        JLabel lblPlotT = new TmmLabel(TmmResourceBundle.getString("metatag.plot"));
        panelRight.add(lblPlotT, "cell 0 8,alignx left,aligny top");

        taPlot = new ReadOnlyTextPaneHTML();
        panelRight.add(taPlot, "cell 0 9,growx,wmin 0,aligny top");
      }
      {
        panelRight.add(new JSeparator(), "cell 0 10,growx");
      }
      {
        JPanel panelBottomDetails = new JPanel();
        panelRight.add(panelBottomDetails, "cell 0 11,grow");
        panelBottomDetails.setLayout(new MigLayout("insets 0", "[][200lp,grow]", "[]2lp[]2lp[]2lp[]2lp[]2lp[]"));
        {
          JLabel lblMoviesetT = new TmmLabel(TmmResourceBundle.getString("metatag.movieset"));
          panelBottomDetails.add(lblMoviesetT, "cell 0 0");

          lblMovieSet = new JLabel("");
          panelBottomDetails.add(lblMovieSet, "cell 1 0,growx,wmin 0");
        }
        {
          JLabel lblShowlinkT = new TmmLabel(TmmResourceBundle.getString("metatag.showlink"));
          panelBottomDetails.add(lblShowlinkT, "cell 0 1");

          lblShowlink = new JLabel("");
          panelBottomDetails.add(lblShowlink, "cell 1 1");
        }
        {
          JLabel lblEditionT = new TmmLabel(TmmResourceBundle.getString("metatag.edition"));
          panelBottomDetails.add(lblEditionT, "cell 0 2");

          lblEdition = new JLabel("");
          panelBottomDetails.add(lblEdition, "cell 1 2,growx,wmin 0");
        }
        {
          JLabel lblTagsT = new TmmLabel(TmmResourceBundle.getString("metatag.tags"));
          panelBottomDetails.add(lblTagsT, "cell 0 3");

          taTags = new ReadOnlyTextPane();
          panelBottomDetails.add(taTags, "cell 1 3,growx,wmin 0");
        }
        {
          JLabel lblMoviePathT = new TmmLabel(TmmResourceBundle.getString("metatag.path"));
          panelBottomDetails.add(lblMoviePathT, "cell 0 4");

          lblMoviePath = new LinkTextArea("");
          panelBottomDetails.add(lblMoviePath, "cell 1 4,growx,wmin 0");
        }
        {
          JLabel lblNoteT = new TmmLabel(TmmResourceBundle.getString("metatag.note"));
          panelBottomDetails.add(lblNoteT, "cell 0 5");

          taNote = new ReadOnlyTextPaneHTML();
          panelBottomDetails.add(taNote, "cell 1 5,growx,wmin 0");
        }
      }
    }
  }

  @Override
  protected List<MediaFileType> getShowArtworkFromSettings() {
    return MovieModuleManager.getInstance().getSettings().getShowArtworkTypes();
  }

  @Override
  protected void setColumnLayout(boolean artworkVisible) {
    if (artworkVisible) {
      ((MigLayout) getLayout()).setColumnConstraints(LAYOUT_ARTWORK_VISIBLE);
    }
    else {
      ((MigLayout) getLayout()).setColumnConstraints(LAYOUT_ARTWORK_HIDDEN);
    }
  }

  private void setRating(Movie movie) {
    Map<String, MediaRating> ratings = new HashMap<>(movie.getRatings());
    MediaRating customRating = movie.getRating();
    if (customRating != MediaMetadata.EMPTY_RATING) {
      ratings.put("custom", customRating);
    }

    ratingPanel.setRatings(ratings);
  }

  protected void initDataBindings() {
    Property jLabelBeanProperty = BeanProperty.create("text");
    Property movieSelectionModelBeanProperty_8 = BeanProperty.create("selectedMovie.year");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_8, lblYear,
        jLabelBeanProperty);
    autoBinding_9.bind();
    //
    Property movieSelectionModelBeanProperty_12 = BeanProperty.create("selectedMovie.imdbId");
    Property JTextPaneBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_12, lblImdbid,
        JTextPaneBeanProperty);
    autoBinding_10.bind();
    //
    Property movieSelectionModelBeanProperty_13 = BeanProperty.create("selectedMovie.runtime");
    AutoBinding autoBinding_14 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_13,
        lblRunningTime, jLabelBeanProperty);
    autoBinding_14.setConverter(new RuntimeConverter());
    autoBinding_14.bind();
    //
    Property movieSelectionModelBeanProperty_15 = BeanProperty.create("selectedMovie.tmdbId");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_15, lblTmdbid,
        JTextPaneBeanProperty);
    autoBinding_7.setConverter(new ZeroIdConverter());
    autoBinding_7.bind();
    //
    Property movieSelectionModelBeanProperty_16 = BeanProperty.create("selectedMovie.genresAsString");
    AutoBinding autoBinding_17 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_16, taGenres,
        JTextPaneBeanProperty);
    autoBinding_17.bind();
    //
    Property movieSelectionModelBeanProperty_14 = BeanProperty.create("selectedMovie.plot");
    AutoBinding autoBinding_18 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_14, taPlot,
        JTextPaneBeanProperty);
    autoBinding_18.bind();
    //
    Property movieSelectionModelBeanProperty_3 = BeanProperty.create("selectedMovie.tagline");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_3, lblTagline,
        jLabelBeanProperty);
    autoBinding_4.bind();
    //
    Property movieSelectionModelBeanProperty_4 = BeanProperty.create("selectedMovie.title");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_4, lblMovieName,
        jLabelBeanProperty);
    autoBinding_5.bind();
    //
    Property movieSelectionModelBeanProperty_2 = BeanProperty.create("selectedMovie.locked");
    Property jLabelBeanProperty_2 = BeanProperty.create("icon");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_2, lblMovieName,
        jLabelBeanProperty_2);
    autoBinding_2.setConverter(new LockedConverter());
    autoBinding_2.bind();
    //
    Property movieSelectionModelBeanProperty = BeanProperty.create("selectedMovie.certification.localizedName");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty, lblCertification,
        jLabelBeanProperty);
    autoBinding.bind();
    //
    Property movieSelectionModelBeanProperty_6 = BeanProperty.create("selectedMovie.originalTitle");
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_6,
        lblOriginalTitle, jLabelBeanProperty);
    autoBinding_8.bind();
    //
    Property movieSelectionModelBeanProperty_5 = BeanProperty.create("selectedMovie.otherIds");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_5, taOtherIds,
        JTextPaneBeanProperty);
    autoBinding_6.bind();
    //
    Property movieSelectionModelBeanProperty_1 = BeanProperty.create("selectedMovie.releaseDateAsString");
    AutoBinding autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_1,
        lblReleaseDate, jLabelBeanProperty);
    autoBinding_11.bind();
    //
    Property movieSelectionModelBeanProperty_10 = BeanProperty.create("selectedMovie.productionCompany");
    AutoBinding autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_10,
        taProduction, JTextPaneBeanProperty);
    autoBinding_12.bind();
    //
    Property movieSelectionModelBeanProperty_11 = BeanProperty.create("selectedMovie.country");
    AutoBinding autoBinding_13 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_11, lblCountry,
        jLabelBeanProperty);
    autoBinding_13.bind();
    //
    Property movieSelectionModelBeanProperty_17 = BeanProperty.create("selectedMovie.localizedSpokenLanguages");
    AutoBinding autoBinding_15 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_17,
        lblSpokenLanguages, jLabelBeanProperty);
    autoBinding_15.bind();
    //
    Property movieSelectionModelBeanProperty_18 = BeanProperty.create("selectedMovie.movieSetTitle");
    AutoBinding autoBinding_16 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_18, lblMovieSet,
        jLabelBeanProperty);
    autoBinding_16.bind();
    //
    Property movieSelectionModelBeanProperty_19 = BeanProperty.create("selectedMovie.edition.title");
    AutoBinding autoBinding_19 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_19, lblEdition,
        jLabelBeanProperty);
    autoBinding_19.bind();
    //
    Property movieSelectionModelBeanProperty_20 = BeanProperty.create("selectedMovie.tagsAsString");
    AutoBinding autoBinding_20 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_20, taTags,
        JTextPaneBeanProperty);
    autoBinding_20.bind();
    //
    Property movieSelectionModelBeanProperty_21 = BeanProperty.create("selectedMovie.path");
    Property linkLabelBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_21 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_21,
        lblMoviePath, linkLabelBeanProperty);
    autoBinding_21.bind();
    //
    Property movieSelectionModelBeanProperty_22 = BeanProperty.create("selectedMovie.note");
    AutoBinding autoBinding_22 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_22, taNote,
        JTextPaneBeanProperty);
    autoBinding_22.bind();
    //
    Property movieSelectionModelBeanProperty_23 = BeanProperty.create("selectedMovie.certification");
    AutoBinding autoBinding_23 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_23,
        lblCertificationLogo, jLabelBeanProperty_2);
    autoBinding_23.setConverter(new CertificationImageConverter());
    autoBinding_23.bind();
    //
    Property movieSelectionModelBeanProperty_24 = BeanProperty.create("selectedMovie.traktId");
    AutoBinding autoBinding_24 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_24,
        lblTraktTvId, JTextPaneBeanProperty);
    autoBinding_24.setConverter(new ZeroIdConverter());
    autoBinding_24.bind();
    //
    Property movieSelectionModelBeanProperty_25 = BeanProperty.create("selectedMovie.showlinksAsString");
    AutoBinding autoBinding_25 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_25, lblShowlink,
        jLabelBeanProperty);
    autoBinding_25.bind();
  }
}
