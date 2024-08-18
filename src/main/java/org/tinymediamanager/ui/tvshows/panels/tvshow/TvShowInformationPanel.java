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
package org.tinymediamanager.ui.tvshows.panels.tvshow;

import static org.tinymediamanager.core.Constants.BANNER;
import static org.tinymediamanager.core.Constants.CLEARLOGO;
import static org.tinymediamanager.core.Constants.FANART;
import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;
import static org.tinymediamanager.core.Constants.POSTER;
import static org.tinymediamanager.core.Constants.RATING;
import static org.tinymediamanager.core.Constants.THUMB;

import java.awt.Component;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.jdesktop.beansbinding.Converter;
import org.jdesktop.beansbinding.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.ui.ColumnLayout;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.LinkTextArea;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.ReadOnlyTextPaneHTML;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.converter.CertificationImageConverter;
import org.tinymediamanager.ui.converter.ZeroIdConverter;
import org.tinymediamanager.ui.panels.InformationPanel;
import org.tinymediamanager.ui.panels.MediaInformationLogosPanel;
import org.tinymediamanager.ui.panels.RatingPanel;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowInformationPanel.
 * 
 * @author Manuel Laggner
 */
public class TvShowInformationPanel extends InformationPanel {
  private static final Logger        LOGGER                 = LoggerFactory.getLogger(TvShowInformationPanel.class);

  private static final String        LAYOUT_ARTWORK_VISIBLE = "[n:100lp:20%, grow][300lp:300lp,grow 350]";
  private static final String        LAYOUT_ARTWORK_HIDDEN  = "[][300lp:300lp,grow 350]";

  private final TvShowSelectionModel tvShowSelectionModel;

  private JTextPane                  taGenres;
  private JLabel                     lblCertification;
  private LinkLabel                  lblThetvdbId;
  private LinkLabel                  lblImdbId;
  private LinkLabel                  lblTmdbId;
  private LinkTextArea               lblPath;
  private JLabel                     lblPremiered;
  private JTextPane                  taStudio;
  private JLabel                     lblStatus;
  private JLabel                     lblYear;
  private JLabel                     lblEpisodeGroup;
  private JTextPane                  taTags;
  private JTextPane                  taOtherIds;
  private JLabel                     lblCountry;
  private JLabel                     lblRuntime;
  private JTextPane                  taNote;
  private JLabel                     lblTvShowName;
  private JTextPane                  taOverview;
  private MediaInformationLogosPanel panelLogos;
  private JLabel                     lblOriginalTitle;
  private JScrollPane                scrollPane;
  private JLabel                     lblCertificationLogo;
  private RatingPanel                ratingPanel;

  /**
   * Instantiates a new tv show information panel.
   *
   * @param tvShowSelectionModel
   *          the tv show selection model
   */
  public TvShowInformationPanel(TvShowSelectionModel tvShowSelectionModel) {
    this.tvShowSelectionModel = tvShowSelectionModel;

    initComponents();

    // beansbinding init
    initDataBindings();

    // action listeners
    lblImdbId.addActionListener(arg0 -> {
      String url = "https://www.imdb.com/title/" + lblImdbId.getText();
      try {
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception e) {
        LOGGER.error("browse to imdbid", e);
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));
      }
    });

    lblThetvdbId.addActionListener(arg0 -> {
      String url = "https://thetvdb.com/?tab=series&id=" + lblThetvdbId.getText();
      try {
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception e) {
        LOGGER.error("browse to thetvdb", e);
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));
      }
    });

    lblTmdbId.addActionListener(arg0 -> {
      String url = "https://www.themoviedb.org/tv/" + lblTmdbId.getText();
      try {
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception e) {
        LOGGER.error("browse to tmdb", e);
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));
      }
    });

    lblPath.addActionListener(e -> {
      if (StringUtils.isNotBlank(lblPath.getText())) {
        // get the location from the label
        Path path = Paths.get(lblPath.getText());
        TmmUIHelper.openFolder(path);
      }
    });

    // manual coded binding
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();
      // react on selection/change of a TV show
      if (source.getClass() != TvShowSelectionModel.class) {
        return;
      }

      TvShowSelectionModel model = (TvShowSelectionModel) source;
      TvShow tvShow = model.getSelectedTvShow();

      if ("selectedTvShow".equals(property) || POSTER.equals(property)) {
        setArtwork(tvShow, MediaFileType.POSTER);
      }

      if ("selectedTvShow".equals(property) || FANART.equals(property)) {
        setArtwork(tvShow, MediaFileType.FANART);
      }

      if ("selectedTvShow".equals(property) || BANNER.equals(property)) {
        setArtwork(tvShow, MediaFileType.BANNER);
      }

      if ("selectedTvShow".equals(property) || THUMB.equals(property)) {
        setArtwork(tvShow, MediaFileType.THUMB);
      }

      if ("selectedTvShow".equals(property) || CLEARLOGO.equals(property)) {
        setArtwork(tvShow, MediaFileType.CLEARLOGO);
      }

      if ("selectedTvShow".equals(property) || MEDIA_FILES.equals(property) || MEDIA_INFORMATION.equals(property)) {
        panelLogos.setMediaInformationSource(tvShow);
      }

      if ("selectedTvShow".equals(property) || RATING.equals(property)) {
        setRating(tvShow);
      }

      if ("selectedTvShow".equals(property)) {
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
      }
    };

    tvShowSelectionModel.addPropertyChangeListener(propertyChangeListener);

    // select first entry

  }

  private void initComponents() {
    setLayout(new MigLayout("", LAYOUT_ARTWORK_VISIBLE, "[][grow]"));
    {
      JPanel panelLeft = new JPanel();
      panelLeft.setLayout(new ColumnLayout());
      add(panelLeft, "cell 0 0 1 2,grow");

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
      panelTitle.setLayout(new MigLayout("insets 0 0 n n", "[grow]", "[][][shrink 0]"));
      {
        lblTvShowName = new TmmLabel("", 1.33);
        panelTitle.add(lblTvShowName, "cell 0 0,growx,wmin 0");
      }
      {
        lblOriginalTitle = new JLabel("");
        panelTitle.add(lblOriginalTitle, "cell 0 1,growx,wmin 0");
      }
      {
        panelTitle.add(new JSeparator(), "cell 0 2,growx");
      }
    }
    {
      JPanel panelRight = new JPanel();
      panelRight.setLayout(new MigLayout("insets n 0 n n, hidemode 2", "[100lp,grow]", "[][shrink 0][][shrink 0][][shrink 0][][grow][][]"));

      scrollPane = new NoBorderScrollPane(panelRight);
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      scrollPane.getVerticalScrollBar().setUnitIncrement(8);
      add(scrollPane, "cell 1 1,grow, wmin 0");
      {
        JPanel panelTopDetails = new JPanel();
        panelRight.add(panelTopDetails, "cell 0 0,growx");
        panelTopDetails.setLayout(new MigLayout("insets 0", "[][][40lp!][][grow][]", "[]2lp[]2lp[]2lp[]2lp[]2lp[]2lp[]2lp[]"));
        {
          JLabel lblYearT = new TmmLabel(TmmResourceBundle.getString("metatag.year"));
          panelTopDetails.add(lblYearT, "flowy,cell 0 0");

          lblYear = new JLabel("");
          panelTopDetails.add(lblYear, "cell 1 0");
        }
        {
          JLabel lblImdbIdT = new TmmLabel("IMDB ID");
          panelTopDetails.add(lblImdbIdT, "cell 3 0");

          lblImdbId = new LinkLabel("");
          panelTopDetails.add(lblImdbId, "cell 4 0");
        }
        {
          lblCertificationLogo = new JLabel("");
          panelTopDetails.add(lblCertificationLogo, "cell 5 0 1 3, top");
        }
        {
          JLabel lblPremieredT = new TmmLabel(TmmResourceBundle.getString("metatag.premiered"));
          panelTopDetails.add(lblPremieredT, "cell 0 1");

          lblPremiered = new JLabel("");
          panelTopDetails.add(lblPremiered, "cell 1 1");
        }
        {
          JLabel lblThetvdbIdT = new TmmLabel("TheTVDB ID");
          panelTopDetails.add(lblThetvdbIdT, "cell 3 1");

          lblThetvdbId = new LinkLabel("");
          panelTopDetails.add(lblThetvdbId, "cell 4 1");
        }
        {
          JLabel lblCertificationT = new TmmLabel(TmmResourceBundle.getString("metatag.certification"));
          panelTopDetails.add(lblCertificationT, "cell 0 2");

          lblCertification = new JLabel("");
          panelTopDetails.add(lblCertification, "cell 1 2");
        }
        {
          JLabel lblTmdbIdT = new TmmLabel(TmmResourceBundle.getString("metatag.tmdb"));
          panelTopDetails.add(lblTmdbIdT, "cell 3 2");

          lblTmdbId = new LinkLabel();
          panelTopDetails.add(lblTmdbId, "cell 4 2");
        }
        {
          JLabel lblOtherIdsT = new TmmLabel(TmmResourceBundle.getString("metatag.otherids"));
          panelTopDetails.add(lblOtherIdsT, "cell 3 3");

          taOtherIds = new ReadOnlyTextPane();
          panelTopDetails.add(taOtherIds, "cell 4 3 2 1,growx,wmin 0");
        }
        {
          JLabel lblRuntimeT = new TmmLabel(TmmResourceBundle.getString("metatag.runtime"));
          panelTopDetails.add(lblRuntimeT, "cell 0 3,aligny top");

          lblRuntime = new JLabel("");
          panelTopDetails.add(lblRuntime, "cell 1 3,aligny top");
        }
        {
          JLabel lblGenresT = new TmmLabel(TmmResourceBundle.getString("metatag.genre"));
          panelTopDetails.add(lblGenresT, "cell 0 4");

          taGenres = new ReadOnlyTextPane();
          panelTopDetails.add(taGenres, "cell 1 4 5 1,growx,wmin 0");
        }
        {
          JLabel lblStatusT = new TmmLabel(TmmResourceBundle.getString("metatag.status"));
          panelTopDetails.add(lblStatusT, "cell 0 5");

          lblStatus = new JLabel("");
          panelTopDetails.add(lblStatus, "cell 1 5 4 1");
        }
        {
          JLabel lblStudioT = new TmmLabel(TmmResourceBundle.getString("metatag.studio"));
          panelTopDetails.add(lblStudioT, "cell 0 6,wmin 0");

          taStudio = new ReadOnlyTextPane();
          panelTopDetails.add(taStudio, "cell 1 6 5 1,growx, wmin 0");
        }
        {
          JLabel lblCountryT = new TmmLabel(TmmResourceBundle.getString("metatag.country"));
          panelTopDetails.add(lblCountryT, "cell 0 7");

          lblCountry = new JLabel("");
          panelTopDetails.add(lblCountry, "cell 1 7 5 1, wmin 0");
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
        JLabel lblPlot = new TmmLabel(TmmResourceBundle.getString("metatag.plot"));
        panelRight.add(lblPlot, "cell 0 6");
        TmmFontHelper.changeFont(lblPlot, Font.BOLD);

        taOverview = new ReadOnlyTextPaneHTML();
        panelRight.add(taOverview, "cell 0 7,growx,wmin 0,aligny top");
      }
      {
        panelRight.add(new JSeparator(), "cell 0 8,growx");
      }
      {
        JPanel panelBottomDetails = new JPanel();
        panelBottomDetails.setLayout(new MigLayout("insets 0", "[][grow]", "[]2lp[]2lp[]2lp[]"));
        panelRight.add(panelBottomDetails, "cell 0 9,grow");

        {
          JLabel lblEpisodegroupT = new TmmLabel(TmmResourceBundle.getString("metatag.episode.group"));
          panelBottomDetails.add(lblEpisodegroupT, "cell 0 0");

          lblEpisodeGroup = new JLabel();
          panelBottomDetails.add(lblEpisodeGroup, "cell 1 0,growx,wmin 0");
        }
        {
          JLabel lblTagsT = new TmmLabel(TmmResourceBundle.getString("metatag.tags"));
          panelBottomDetails.add(lblTagsT, "cell 0 1");

          taTags = new ReadOnlyTextPane();
          panelBottomDetails.add(taTags, "cell 1 1,growx,wmin 0");
        }
        {
          JLabel lblPathT = new TmmLabel(TmmResourceBundle.getString("metatag.path"));
          panelBottomDetails.add(lblPathT, "cell 0 2");

          lblPath = new LinkTextArea("");
          panelBottomDetails.add(lblPath, "cell 1 2,growx,wmin 0");
        }
        {
          JLabel lblNoteT = new TmmLabel(TmmResourceBundle.getString("metatag.note"));
          panelBottomDetails.add(lblNoteT, "cell 0 3");

          taNote = new ReadOnlyTextPaneHTML();
          panelBottomDetails.add(taNote, "cell 1 3,growx,wmin 0");
        }
      }
    }
  }

  @Override
  protected List<MediaFileType> getShowArtworkFromSettings() {
    return TvShowModuleManager.getInstance().getSettings().getShowTvShowArtworkTypes();
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

  private void setRating(TvShow tvShow) {
    Map<String, MediaRating> ratings = new HashMap<>(tvShow.getRatings());
    MediaRating customRating = tvShow.getRating();
    if (customRating != MediaMetadata.EMPTY_RATING) {
      ratings.put("custom", customRating);
    }

    ratingPanel.setRatings(ratings);
  }

  protected void initDataBindings() {
    Property tvShowSelectionModelBeanProperty = BeanProperty.create("selectedTvShow.title");
    Property jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty, lblTvShowName,
        jLabelBeanProperty);
    autoBinding.bind();
    //
    Property tvShowSelectionModelBeanProperty_1 = BeanProperty.create("selectedTvShow.plot");
    Property jTextPaneBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_1, taOverview,
        jTextPaneBeanProperty);
    autoBinding_1.bind();
    //
    Property tvShowSelectionModelBeanProperty_4 = BeanProperty.create("selectedTvShow.originalTitle");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_4,
        lblOriginalTitle, jLabelBeanProperty);
    autoBinding_5.bind();
    //
    Property tvShowSelectionModelBeanProperty_6 = BeanProperty.create("selectedTvShow.year");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_6, lblYear,
        jLabelBeanProperty);
    autoBinding_6.bind();
    //
    Property tvShowSelectionModelBeanProperty_7 = BeanProperty.create("selectedTvShow.imdbId");
    Property linkLabelBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_7, lblImdbId,
        linkLabelBeanProperty);
    autoBinding_7.bind();
    //
    Property tvShowSelectionModelBeanProperty_8 = BeanProperty.create("selectedTvShow.certification.localizedName");
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_8,
        lblCertification, jLabelBeanProperty);
    autoBinding_8.bind();
    //
    Property tvShowSelectionModelBeanProperty_9 = BeanProperty.create("selectedTvShow.tvdbId");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_9,
        lblThetvdbId, linkLabelBeanProperty);
    autoBinding_9.bind();
    //
    Property tvShowSelectionModelBeanProperty_10 = BeanProperty.create("selectedTvShow.runtime");
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_10,
        lblRuntime, jLabelBeanProperty);
    autoBinding_10.bind();
    //
    Property tvShowSelectionModelBeanProperty_11 = BeanProperty.create("selectedTvShow.otherIds");
    AutoBinding autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_11,
        taOtherIds, jTextPaneBeanProperty);
    autoBinding_11.bind();
    //
    Property tvShowSelectionModelBeanProperty_12 = BeanProperty.create("selectedTvShow.status.localizedName");
    AutoBinding autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_12, lblStatus,
        jLabelBeanProperty);
    autoBinding_12.bind();
    //
    Property tvShowSelectionModelBeanProperty_13 = BeanProperty.create("selectedTvShow.genresAsString");
    AutoBinding autoBinding_13 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_13, taGenres,
        jTextPaneBeanProperty);
    autoBinding_13.bind();
    //
    Property tvShowSelectionModelBeanProperty_14 = BeanProperty.create("selectedTvShow.firstAiredAsString");
    AutoBinding autoBinding_14 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_14,
        lblPremiered, jLabelBeanProperty);
    autoBinding_14.bind();
    //
    Property tvShowSelectionModelBeanProperty_15 = BeanProperty.create("selectedTvShow.productionCompany");
    AutoBinding autoBinding_15 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_15, taStudio,
        jTextPaneBeanProperty);
    autoBinding_15.bind();
    //
    Property tvShowSelectionModelBeanProperty_16 = BeanProperty.create("selectedTvShow.country");
    AutoBinding autoBinding_16 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_16,
        lblCountry, jLabelBeanProperty);
    autoBinding_16.bind();
    //
    Property tvShowSelectionModelBeanProperty_17 = BeanProperty.create("selectedTvShow.tagsAsString");
    AutoBinding autoBinding_17 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_17, taTags,
        jTextPaneBeanProperty);
    autoBinding_17.bind();
    //
    Property tvShowSelectionModelBeanProperty_18 = BeanProperty.create("selectedTvShow.path");
    Property linkTextAreaBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_18 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_18, lblPath,
        linkTextAreaBeanProperty);
    autoBinding_18.bind();
    //
    Property tvShowSelectionModelBeanProperty_19 = BeanProperty.create("selectedTvShow.note");
    AutoBinding autoBinding_19 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_19, taNote,
        jTextPaneBeanProperty);
    autoBinding_19.bind();
    //
    Property tvShowSelectionModelBeanProperty_20 = BeanProperty.create("selectedTvShow.certification");
    Property jLabelBeanProperty_1 = BeanProperty.create("icon");
    AutoBinding autoBinding_20 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_20,
        lblCertificationLogo, jLabelBeanProperty_1);
    autoBinding_20.setConverter(new CertificationImageConverter());
    autoBinding_20.bind();
    //
    Property tvShowSelectionModelBeanProperty_21 = BeanProperty.create("selectedTvShow.tmdbId");
    AutoBinding autoBinding_21 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_21, lblTmdbId,
        linkLabelBeanProperty);
    autoBinding_21.setConverter(new ZeroIdConverter());
    autoBinding_21.bind();
    //
    Property tvShowSelectionModelBeanProperty_2 = BeanProperty.create("selectedTvShow.episodeGroup");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel, tvShowSelectionModelBeanProperty_2,
        lblEpisodeGroup, jLabelBeanProperty);
    autoBinding_2.setConverter(new MediaEpisodeGroupConverter());
    autoBinding_2.bind();
  }

  private static class MediaEpisodeGroupConverter extends Converter<MediaEpisodeGroup, String> {
    @Override
    public String convertForward(MediaEpisodeGroup value) {
      if (value == null) {
        return null;
      }
      return value.toString();
    }

    @Override
    public MediaEpisodeGroup convertReverse(String value) {
      return null;
    }
  }
}
