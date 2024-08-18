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
package org.tinymediamanager.ui.tvshows.panels.episode;

import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;
import static org.tinymediamanager.core.Constants.POSTER;
import static org.tinymediamanager.core.Constants.RATING;
import static org.tinymediamanager.core.Constants.SEASON_POSTER;
import static org.tinymediamanager.core.Constants.THUMB;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.ui.ColumnLayout;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.LinkTextArea;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.ReadOnlyTextPaneHTML;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.converter.ZeroIdConverter;
import org.tinymediamanager.ui.panels.InformationPanel;
import org.tinymediamanager.ui.panels.MediaInformationLogosPanel;
import org.tinymediamanager.ui.panels.RatingPanel;
import org.tinymediamanager.ui.tvshows.TvShowEpisodeSelectionModel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowEpisodeInformationPanel.
 * 
 * @author Manuel Laggner
 */
public class TvShowEpisodeInformationPanel extends InformationPanel {
  private static final Logger               LOGGER                 = LoggerFactory.getLogger(TvShowEpisodeInformationPanel.class);

  private static final String               LAYOUT_ARTWORK_VISIBLE = "[n:100lp:20%, grow][300lp:300lp,grow 350]";
  private static final String               LAYOUT_ARTWORK_HIDDEN  = "[][300lp:300lp,grow 350]";

  private final TvShowSettings              settings               = TvShowModuleManager.getInstance().getSettings();
  private final TvShowEpisodeSelectionModel tvShowEpisodeSelectionModel;

  /** UI components */
  private JLabel                            lblTvShowName;
  private JLabel                            lblEpisodeTitle;
  private JTextPane                         taOverview;
  private MediaInformationLogosPanel        panelLogos;
  private JSeparator                        sepLogos;
  private JLabel                            lblOriginalTitle;
  private JButton                           btnPlay;
  private JScrollPane                       scrollPane;
  private JLabel                            lblSeason;
  private JLabel                            lblEpisode;
  private JLabel                            lblAired;
  private JTextPane                         taTags;
  private LinkTextArea                      lblPath;
  private JTextPane                         taNote;
  private LinkLabel                         lblTraktTvId;
  private LinkLabel                         lblTvdbId;
  private LinkLabel                         lblImdbId;
  private LinkLabel                         lblTmdbId;
  private JTextPane                         taOtherIds;
  private RatingPanel                       ratingPanel;

  /**
   * Instantiates a new tv show information panel.
   * 
   * @param tvShowEpisodeSelectionModel
   *          the tv show selection model
   */
  public TvShowEpisodeInformationPanel(TvShowEpisodeSelectionModel tvShowEpisodeSelectionModel) {
    this.tvShowEpisodeSelectionModel = tvShowEpisodeSelectionModel;

    initComponents();
    initDataBindings();

    // manual coded binding
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();
      // react on selection/change of an episode
      if (source.getClass() != TvShowEpisodeSelectionModel.class) {
        return;
      }

      TvShowEpisodeSelectionModel model = (TvShowEpisodeSelectionModel) source;
      TvShowEpisode episode = model.getSelectedTvShowEpisode();

      if ("selectedTvShowEpisode".equals(property) || POSTER.equals(property) || SEASON_POSTER.equals(property)) {
        setSeasonPoster(episode);
      }

      if ("selectedTvShowEpisode".equals(property) || THUMB.equals(property)) {
        setArtwork(episode, MediaFileType.THUMB);
      }

      if ("selectedTvShowEpisode".equals(property) || MEDIA_FILES.equals(property) || MEDIA_INFORMATION.equals(property)) {
        panelLogos.setMediaInformationSource(episode);
      }

      if ("selectedTvShowEpisode".equals(property) || RATING.equals(property)) {
        setRating(episode);
      }

      if ("selectedTvShowEpisode".equals(property)) {
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
      }
    };

    this.tvShowEpisodeSelectionModel.addPropertyChangeListener(propertyChangeListener);

    btnPlay.addActionListener(e -> {
      MediaFile mf = this.tvShowEpisodeSelectionModel.getSelectedTvShowEpisode().getMainVideoFile();
      if (StringUtils.isNotBlank(mf.getFilename())) {
        try {
          TmmUIHelper.openFile(MediaFileHelper.getMainVideoFile(mf));
        }
        catch (Exception ex) {
          LOGGER.error("open file - '{}'", e);
          MessageManager.instance
              .pushMessage(new Message(Message.MessageLevel.ERROR, mf, "message.erroropenfile", new String[] { ":", ex.getLocalizedMessage() }));
        }
      }
    });

    lblPath.addActionListener(arg0 -> {
      if (!StringUtils.isEmpty(lblPath.getText())) {
        // get the location from the label
        Path path = Paths.get(lblPath.getText());
        TmmUIHelper.openFolder(path);
      }
    });

    // Trakt.tv
    lblTraktTvId.addActionListener(arg0 -> {
      String url = "";
      // use EP id
      int epId = tvShowEpisodeSelectionModel.getSelectedTvShowEpisode().getTraktTvId();
      if (epId > 0) {
        url = "https://trakt.tv//search/trakt/" + epId + "?id_type=episode";
      }
      else {
        // via show ID + S/EE
        int tvShowId = tvShowEpisodeSelectionModel.getSelectedTvShowEpisode().getTvShow().getTraktId();
        int seasonId = tvShowEpisodeSelectionModel.getSelectedTvShowEpisode().getAiredSeason();
        int episodeId = tvShowEpisodeSelectionModel.getSelectedTvShowEpisode().getAiredEpisode();
        url = "https://trakt.tv/shows/" + tvShowId + "/seasons/" + seasonId + "/episodes/" + episodeId;
      }
      try {
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception e) {
        LOGGER.error("browse to trakt.tv episode", e);
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));
      }
    });

    // Imdb
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

    // TheTvDB
    lblTvdbId.addActionListener(arg0 -> {
      String showId = tvShowEpisodeSelectionModel.getSelectedTvShowEpisode().getTvShow().getTvdbId();
      String url = "https://thetvdb.com/?tab=series&id=" + showId + "&tab=episode&id=" + lblTvdbId.getText();
      try {
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception e) {
        LOGGER.error("browse to thetvdb", e);
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));
      }
    });

    // TMDB
    lblTmdbId.addActionListener(arg0 -> {
      int tvShowId = tvShowEpisodeSelectionModel.getSelectedTvShowEpisode().getTvShow().getTmdbId();
      int seasonId = tvShowEpisodeSelectionModel.getSelectedTvShowEpisode().getAiredSeason();
      int episodeId = tvShowEpisodeSelectionModel.getSelectedTvShowEpisode().getAiredEpisode();
      String url = "https://www.themoviedb.org/tv/" + tvShowId + "/season/" + seasonId + "/episode/" + episodeId;
      try {
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception e) {
        LOGGER.error("browse to TMDB", e);
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));
      }
    });

  }

  private void initComponents() {
    setLayout(new MigLayout("", LAYOUT_ARTWORK_VISIBLE, "[][grow]"));

    {
      JPanel panelLeft = new JPanel();
      panelLeft.setLayout(new ColumnLayout());
      add(panelLeft, "cell 0 0 1 2,grow");

      for (Component component : generateArtworkComponents(MediaFileType.SEASON_POSTER)) {
        panelLeft.add(component);
      }

      for (Component component : generateArtworkComponents(MediaFileType.THUMB)) {
        panelLeft.add(component);
      }
    }
    {
      JPanel panelTitle = new JPanel();
      add(panelTitle, "cell 1 0,growx");
      panelTitle.setLayout(new MigLayout("insets 0 0 n n", "[grow][]", "[][][][shrink 0]"));

      {
        lblTvShowName = new TmmLabel("", 1.33);
        panelTitle.add(lblTvShowName, "flowx,cell 0 0,growx,wmin 0");
      }
      {
        btnPlay = new FlatButton(IconManager.PLAY_LARGE);
        panelTitle.add(btnPlay, "cell 1 0 1 4,aligny top");
      }
      {
        lblEpisodeTitle = new TmmLabel("", 1.16);
        panelTitle.add(lblEpisodeTitle, "cell 0 1,growx,wmin 0");
      }
      {
        lblOriginalTitle = new JLabel("");
        panelTitle.add(lblOriginalTitle, "cell 0 2,growx,wmin 0");
      }
      {
        panelTitle.add(new JSeparator(), "cell 0 3 2 1,growx");
      }
    }
    {
      JPanel panelRight = new JPanel();
      panelRight.setLayout(new MigLayout("insets 0 0 n n, hidemode 2", "[100lp,grow]", "[][shrink 0][][shrink 0][][shrink 0][][][][]"));

      scrollPane = new NoBorderScrollPane(panelRight);
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      scrollPane.getVerticalScrollBar().setUnitIncrement(8);
      add(scrollPane, "cell 1 1,grow, wmin 0");
      {
        JPanel panelTopDetails = new JPanel();
        panelTopDetails.setLayout(new MigLayout("insets 0", "[][][40lp!][][]", "[]2lp[][][][]"));

        panelRight.add(panelTopDetails, "cell 0 0,grow");
        {
          JLabel lblSeasonT = new JLabel(TmmResourceBundle.getString("metatag.season"));
          TmmFontHelper.changeFont(lblSeasonT, 1.166, Font.BOLD);
          panelTopDetails.add(lblSeasonT, "cell 0 0");

          lblSeason = new JLabel("");
          TmmFontHelper.changeFont(lblSeason, 1.166);
          panelTopDetails.add(lblSeason, "cell 1 0");
        }
        {
          JLabel lblEpisodeT = new JLabel(TmmResourceBundle.getString("metatag.episode"));
          TmmFontHelper.changeFont(lblEpisodeT, 1.166, Font.BOLD);
          panelTopDetails.add(lblEpisodeT, "cell 0 1");

          lblEpisode = new JLabel("");
          TmmFontHelper.changeFont(lblEpisode, 1.166);
          panelTopDetails.add(lblEpisode, "cell 1 1");
        }
        JLabel lblAiredT = new TmmLabel(TmmResourceBundle.getString("metatag.aired"));
        panelTopDetails.add(lblAiredT, "cell 0 2");
        {

          lblAired = new JLabel("");
          panelTopDetails.add(lblAired, "cell 1 2");
        }
        {
          JLabel lblImdbIdT = new TmmLabel("IMDB ID");
          panelTopDetails.add(lblImdbIdT, "cell 3 0");

          lblImdbId = new LinkLabel();
          panelTopDetails.add(lblImdbId, "cell 4 0");
        }
        {
          JLabel lblTvdbIdT = new TmmLabel("TheTVDB ID");
          panelTopDetails.add(lblTvdbIdT, "cell 3 1");

          lblTvdbId = new LinkLabel();
          panelTopDetails.add(lblTvdbId, "cell 4 1");
        }
        {
          JLabel lblTraktTvIdT = new TmmLabel("Trakt.tv ID");
          panelTopDetails.add(lblTraktTvIdT, "cell 3 2");

          lblTraktTvId = new LinkLabel();
          panelTopDetails.add(lblTraktTvId, "cell 4 2");
        }
        {
          JLabel lblTmdbIdT = new TmmLabel("TMDB ID");
          panelTopDetails.add(lblTmdbIdT, "cell 3 3");

          lblTmdbId = new LinkLabel();
          panelTopDetails.add(lblTmdbId, "cell 4 3");
        }
        {
          JLabel lblOtherIdsT = new TmmLabel(TmmResourceBundle.getString("metatag.otherids"));
          panelTopDetails.add(lblOtherIdsT, "cell 3 4");

          taOtherIds = new ReadOnlyTextPane();
          panelTopDetails.add(taOtherIds, "cell 4 4 2 1,growx,wmin 0");
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
        sepLogos = new JSeparator();
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

        taOverview = new ReadOnlyTextPaneHTML();
        panelRight.add(taOverview, "cell 0 7,growx,wmin 0,aligny top");
      }
      {
        panelRight.add(new JSeparator(), "cell 0 8,growx");
      }
      {
        JPanel panelBottomDetails = new JPanel();
        panelRight.add(panelBottomDetails, "cell 0 9,grow");
        panelBottomDetails.setLayout(new MigLayout("insets 0", "[][10lp][200lp,grow]", "[]2lp[]2lp[]"));
        {
          {
            JLabel lblTagsT = new TmmLabel(TmmResourceBundle.getString("metatag.tags"));
            panelBottomDetails.add(lblTagsT, "cell 0 0");

            taTags = new ReadOnlyTextPane();
            panelBottomDetails.add(taTags, "cell 2 0,growx,wmin 0");
          }
          {
            JLabel lblPathT = new TmmLabel(TmmResourceBundle.getString("metatag.path"));
            panelBottomDetails.add(lblPathT, "cell 0 1");

            lblPath = new LinkTextArea("");
            panelBottomDetails.add(lblPath, "cell 2 1,growx,wmin 0");
          }
          {
            JLabel lblNoteT = new TmmLabel(TmmResourceBundle.getString("metatag.note"));
            panelBottomDetails.add(lblNoteT, "cell 0 2");

            taNote = new ReadOnlyTextPaneHTML();
            panelBottomDetails.add(taNote, "cell 2 2,growx,wmin 0");
          }
        }
      }
    }
  }

  private void setSeasonPoster(TvShowEpisode episode) {
    String posterPath = episode.getTvShowSeason().getArtworkFilename(MediaFileType.SEASON_POSTER);
    Dimension posterSize = episode.getTvShowSeason().getArtworkDimension(MediaFileType.SEASON_POSTER);

    if (StringUtils.isBlank(posterPath) && TvShowModuleManager.getInstance().getSettings().isSeasonArtworkFallback()) {
      // fall back to the show
      posterPath = episode.getTvShowSeason().getTvShow().getArtworkFilename(MediaFileType.POSTER);
      posterSize = episode.getTvShowSeason().getTvShow().getArtworkDimension(MediaFileType.POSTER);
    }

    setArtwork(MediaFileType.SEASON_POSTER, posterPath, posterSize);
  }

  private void setArtwork(MediaFileType type, String artworkPath, Dimension artworkDimension) {
    List<Component> components = artworkComponents.get(type);
    if (ListUtils.isEmpty(components)) {
      return;
    }

    boolean visible = getShowArtworkFromSettings().contains(type);

    for (Component component : components) {
      component.setVisible(visible);

      if (component instanceof ImageLabel imageLabel) {
        imageLabel.clearImage();
        imageLabel.setImagePath(artworkPath);
      }
      else if (component instanceof JLabel sizeLabel) {
        if (artworkDimension.width > 0 && artworkDimension.height > 0) {
          sizeLabel.setText(TmmResourceBundle.getString("mediafiletype." + type.name().toLowerCase(Locale.ROOT)) + " - " + artworkDimension.width
              + "x" + artworkDimension.height);
        }
        else {
          sizeLabel.setText(TmmResourceBundle.getString("mediafiletype." + type.name().toLowerCase(Locale.ROOT)));
        }
      }
    }

    updateArtwork();
  }

  @Override
  protected List<MediaFileType> getShowArtworkFromSettings() {
    return TvShowModuleManager.getInstance().getSettings().getShowEpisodeArtworkTypes();
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

  private void setRating(TvShowEpisode episode) {
    Map<String, MediaRating> ratings = new HashMap<>(episode.getRatings());
    MediaRating customRating = episode.getRating();
    if (customRating != MediaMetadata.EMPTY_RATING) {
      ratings.put("custom", customRating);
    }

    ratingPanel.setRatings(ratings);
  }

  protected void initDataBindings() {
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty = BeanProperty
        .create("selectedTvShowEpisode.tvShow.title");
    BeanProperty<JLabel, String> jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding<TvShowEpisodeSelectionModel, String, JLabel, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty, lblTvShowName, jLabelBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty_1 = BeanProperty
        .create("selectedTvShowEpisode.titleForUi");
    AutoBinding<TvShowEpisodeSelectionModel, String, JLabel, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_1, lblEpisodeTitle, jLabelBeanProperty);
    autoBinding_1.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty_3 = BeanProperty.create("selectedTvShowEpisode.plot");
    BeanProperty<JTextPane, String> JTextPaneBeanProperty = BeanProperty.create("text");
    AutoBinding<TvShowEpisodeSelectionModel, String, JTextPane, String> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_3, taOverview, JTextPaneBeanProperty);
    autoBinding_3.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty_2 = BeanProperty
        .create("selectedTvShowEpisode.originalTitle");
    AutoBinding<TvShowEpisodeSelectionModel, String, JLabel, String> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_2, lblOriginalTitle, jLabelBeanProperty);
    autoBinding_2.bind();
    //
    BeanProperty<TvShowSettings, Boolean> tvShowSettingsBeanProperty = BeanProperty.create("showLogosPanel");
    BeanProperty<JSeparator, Boolean> jSeparatorBeanProperty = BeanProperty.create("visible");
    AutoBinding<TvShowSettings, Boolean, JSeparator, Boolean> autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ, settings,
        tvShowSettingsBeanProperty, sepLogos, jSeparatorBeanProperty);
    autoBinding_7.bind();
    //
    BeanProperty<MediaInformationLogosPanel, Boolean> mediaInformationLogosPanelBeanProperty = BeanProperty.create("visible");
    AutoBinding<TvShowSettings, Boolean, MediaInformationLogosPanel, Boolean> autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ,
        settings, tvShowSettingsBeanProperty, panelLogos, mediaInformationLogosPanelBeanProperty);
    autoBinding_8.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, Integer> tvShowEpisodeSelectionModelBeanProperty_7 = BeanProperty
        .create("selectedTvShowEpisode.season");
    AutoBinding<TvShowEpisodeSelectionModel, Integer, JLabel, String> autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_7, lblSeason, jLabelBeanProperty);
    autoBinding_9.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, Integer> tvShowEpisodeSelectionModelBeanProperty_8 = BeanProperty
        .create("selectedTvShowEpisode.episode");
    AutoBinding<TvShowEpisodeSelectionModel, Integer, JLabel, String> autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_8, lblEpisode, jLabelBeanProperty);
    autoBinding_10.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty_9 = BeanProperty
        .create("selectedTvShowEpisode.firstAiredAsString");
    AutoBinding<TvShowEpisodeSelectionModel, String, JLabel, String> autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_9, lblAired, jLabelBeanProperty);
    autoBinding_11.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty_10 = BeanProperty
        .create("selectedTvShowEpisode.tagsAsString");
    AutoBinding<TvShowEpisodeSelectionModel, String, JTextPane, String> autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_10, taTags, JTextPaneBeanProperty);
    autoBinding_12.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty_11 = BeanProperty.create("selectedTvShowEpisode.path");
    BeanProperty<LinkTextArea, String> linkTextAreaBeanProperty = BeanProperty.create("text");
    AutoBinding<TvShowEpisodeSelectionModel, String, LinkTextArea, String> autoBinding_13 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_11, lblPath, linkTextAreaBeanProperty);
    autoBinding_13.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty_12 = BeanProperty.create("selectedTvShowEpisode.note");
    AutoBinding<TvShowEpisodeSelectionModel, String, JTextPane, String> autoBinding_14 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_12, taNote, JTextPaneBeanProperty);
    autoBinding_14.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, Integer> tvShowSelectionModelBeanProperty_13 = BeanProperty.create("selectedTvShowEpisode.traktTvId");
    BeanProperty<LinkLabel, String> linkLabelBeanProperty_2 = BeanProperty.create("text");
    AutoBinding<TvShowEpisodeSelectionModel, Integer, LinkLabel, String> autoBinding_15 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowSelectionModelBeanProperty_13, lblTraktTvId, linkLabelBeanProperty_2);
    autoBinding_15.setConverter(new ZeroIdConverter());
    autoBinding_15.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowSelectionModelBeanProperty_14 = BeanProperty.create("selectedTvShowEpisode.imdbId");
    BeanProperty<LinkLabel, String> linkLabelBeanProperty_3 = BeanProperty.create("text");
    AutoBinding<TvShowEpisodeSelectionModel, String, LinkLabel, String> autoBinding_16 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowSelectionModelBeanProperty_14, lblImdbId, linkLabelBeanProperty_3);
    autoBinding_16.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowSelectionModelBeanProperty_15 = BeanProperty.create("selectedTvShowEpisode.tvdbId");
    BeanProperty<LinkLabel, String> linkLabelBeanProperty_4 = BeanProperty.create("text");
    AutoBinding<TvShowEpisodeSelectionModel, String, LinkLabel, String> autoBinding_17 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowSelectionModelBeanProperty_15, lblTvdbId, linkLabelBeanProperty_4);
    autoBinding_17.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowSelectionModelBeanProperty_16 = BeanProperty.create("selectedTvShowEpisode.tmdbId");
    BeanProperty<LinkLabel, String> linkLabelBeanProperty_5 = BeanProperty.create("text");
    AutoBinding<TvShowEpisodeSelectionModel, String, LinkLabel, String> autoBinding_18 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowSelectionModelBeanProperty_16, lblTmdbId, linkLabelBeanProperty_5);
    autoBinding_18.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, Map<String, Object>> tvShowEpisodeSelectionModelBeanProperty_17 = BeanProperty
        .create("selectedTvShowEpisode.otherIds");
    AutoBinding<TvShowEpisodeSelectionModel, Map<String, Object>, JTextPane, String> autoBinding_19 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_17, taOtherIds, JTextPaneBeanProperty);
    autoBinding_19.bind();
  }
}
