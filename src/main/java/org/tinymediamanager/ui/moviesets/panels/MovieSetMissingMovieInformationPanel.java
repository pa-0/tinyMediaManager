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
import static org.tinymediamanager.core.Constants.POSTER;

import java.awt.Cursor;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.ColumnLayout;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.StarRater;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.converter.CertificationImageConverter;
import org.tinymediamanager.ui.converter.RatingConverter;
import org.tinymediamanager.ui.converter.RuntimeConverter;
import org.tinymediamanager.ui.converter.ZeroIdConverter;
import org.tinymediamanager.ui.movies.MovieSelectionModel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link MovieSetMissingMovieInformationPanel} is used to display missing movies in a movie set.
 * 
 * @author Manuel Laggner
 */
public class MovieSetMissingMovieInformationPanel extends JPanel {
  private static final Logger                LOGGER                = LoggerFactory.getLogger(MovieSetMissingMovieInformationPanel.class);

  private final MovieSelectionModel          movieSelectionModel;
  private final RatingConverter<MediaRating> ratingRatingConverter = new RatingConverter<>();

  /** UI components */
  private StarRater                          starRater;
  private JLabel                             lblMovieName;
  private JLabel                             lblRating;
  private JLabel                             lblYear;
  private LinkLabel                          lblImdbid;
  private JLabel                             lblRunningTime;
  private LinkLabel                          lblTmdbid;
  private JTextPane                          taGenres;
  private JTextPane                          taPlot;
  private ImageLabel                         lblMoviePoster;
  private ImageLabel                         lblMovieFanart;
  private JLabel                             lblCertification;
  private JLabel                             lblOriginalTitle;
  private JScrollPane                        scrollPane;
  private JTextPane                          taProduction;
  private JTextPane                          taTags;
  private JLabel                             lblSpokenLanguages;
  private JLabel                             lblCountry;
  private JLabel                             lblReleaseDate;
  private JLabel                             lblCertificationLogo;

  /**
   * Instantiates a new movie information panel.
   *
   * @param movieSelectionModel
   *          the movie selection model
   */
  public MovieSetMissingMovieInformationPanel(MovieSelectionModel movieSelectionModel) {
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

      if (!(movie instanceof MovieSet.MovieSetMovie)) {
        return;
      }

      if ("selectedMovie".equals(property) || POSTER.equals(property)) {
        setPoster(movie);
      }

      if ("selectedMovie".equals(property) || FANART.equals(property)) {
        setFanart(movie);
      }

      if ("selectedMovie".equals(property)) {
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
        setRating(movie);
      }
    };

    movieSelectionModel.addPropertyChangeListener(propertyChangeListener);
  }

  private void initComponents() {
    putClientProperty("class", "roundedPanel");
    setLayout(new MigLayout("", "[100lp:100lp,grow][300lp:300lp,grow 250]", "[][grow]"));

    {
      JPanel panelLeft = new JPanel();
      panelLeft.setLayout(new ColumnLayout());
      add(panelLeft, "cell 0 0 1 2,grow");

      lblMoviePoster = new ImageLabel(false, false, true);
      lblMoviePoster.setDesiredAspectRatio(2 / 3f);
      lblMoviePoster.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      lblMoviePoster.setPreferCache(true);
      lblMoviePoster.enableLightbox();
      panelLeft.add(lblMoviePoster);

      panelLeft.add(Box.createVerticalStrut(20));

      lblMovieFanart = new ImageLabel(false, false, true);
      lblMovieFanart.setDesiredAspectRatio(16 / 9f);
      lblMovieFanart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      lblMovieFanart.setPreferCache(true);
      lblMovieFanart.enableLightbox();
      panelLeft.add(lblMovieFanart);
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
        lblOriginalTitle = new JLabel("");
        panelTitle.add(lblOriginalTitle, "cell 0 1,growx,wmin 0");
      }
      {
        panelTitle.add(new JSeparator(), "cell 0 2 2 1,growx");
      }
    }
    {
      JPanel panelRight = new JPanel();
      panelRight.setLayout(new MigLayout("insets n 0 n n, hidemode 2", "[100lp,grow]", "[shrink 0][][shrink 0][][][grow,top][][]"));

      scrollPane = new NoBorderScrollPane(panelRight);
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      scrollPane.getVerticalScrollBar().setUnitIncrement(8);
      add(scrollPane, "cell 1 1,grow, wmin 0");

      {
        JPanel panelTopDetails = new JPanel();
        panelRight.add(panelTopDetails, "cell 0 0,grow");
        panelTopDetails.setLayout(new MigLayout("insets 0", "[][][40lp][][grow][]", "[]2lp[]2lp[grow]2lp[]2lp[]2lp[]2lp[]2lp[]"));

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
          panelTopDetails.add(lblSpokenLanguagesT, "cell 0 7,wmin 0");

          lblSpokenLanguages = new JLabel("");
          panelTopDetails.add(lblSpokenLanguages, "cell 1 7 5 1");
        }
      }

      {
        panelRight.add(new JSeparator(), "cell 0 1,growx");
      }

      {
        starRater = new StarRater(10, 1);
        panelRight.add(starRater, "flowx,cell 0 2,aligny center");
        starRater.setEnabled(false);

        lblRating = new JLabel("");
        panelRight.add(lblRating, "cell 0 2,aligny center");
      }

      {
        panelRight.add(new JSeparator(), "cell 0 3,growx");
      }

      {
        JLabel lblPlotT = new TmmLabel(TmmResourceBundle.getString("metatag.plot"));
        panelRight.add(lblPlotT, "cell 0 4,alignx left,aligny top");

        taPlot = new ReadOnlyTextPane();
        panelRight.add(taPlot, "cell 0 5,growx,wmin 0,aligny top");
      }
      {
        panelRight.add(new JSeparator(), "cell 0 6,growx");
      }
      {
        JPanel panelBottomDetails = new JPanel();
        panelRight.add(panelBottomDetails, "cell 0 7,grow");
        panelBottomDetails.setLayout(new MigLayout("insets 0", "[][200lp,grow]", "[]"));
        {
          JLabel lblTagsT = new TmmLabel(TmmResourceBundle.getString("metatag.tags"));
          panelBottomDetails.add(lblTagsT, "cell 0 0");

          taTags = new ReadOnlyTextPane();
          panelBottomDetails.add(taTags, "cell 1 0,growx,wmin 0");
        }
      }
    }
  }

  private void setPoster(Movie movie) {
    lblMoviePoster.clearImage();
    lblMoviePoster.setImageUrl(movie.getArtworkUrl(MediaFileType.POSTER));
  }

  private void setFanart(Movie movie) {
    lblMovieFanart.clearImage();
    lblMovieFanart.setImageUrl(movie.getArtworkUrl(MediaFileType.FANART));
  }

  private void setRating(Movie movie) {
    MediaRating rating = movie.getRating();

    if (rating == null) {
      starRater.setRating(0);
      lblRating.setText("");
    }
    else {
      starRater.setRating(rating.getRatingNormalized());
      lblRating.setText(ratingRatingConverter.convertForward(rating));
    }
  }

  protected void initDataBindings() {
    Property movieSelectionModelBeanProperty_8 = BeanProperty.create("selectedMovie.year");
    Property jLabelBeanProperty = BeanProperty.create("text");
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
    Property movieSelectionModelBeanProperty_4 = BeanProperty.create("selectedMovie.title");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_4, lblMovieName,
        jLabelBeanProperty);
    autoBinding_5.bind();
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
    Property movieSelectionModelBeanProperty_20 = BeanProperty.create("selectedMovie.tagsAsString");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_20, taTags,
        JTextPaneBeanProperty);
    autoBinding_1.bind();
    //
    Property movieSelectionModelBeanProperty_23 = BeanProperty.create("selectedMovie.certification");
    Property jLabelBeanProperty_2 = BeanProperty.create("icon");
    AutoBinding autoBinding_23 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel, movieSelectionModelBeanProperty_23,
        lblCertificationLogo, jLabelBeanProperty_2);
    autoBinding_23.setConverter(new CertificationImageConverter());
    autoBinding_23.bind();
  }
}
