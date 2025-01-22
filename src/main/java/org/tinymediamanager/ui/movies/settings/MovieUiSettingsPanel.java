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
package org.tinymediamanager.ui.movies.settings;

import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.ACTOR;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.COUNTRY;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.DIRECTOR;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.FILENAME;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.NOTE;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.PLOT;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.PRODUCER;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.PRODUCTION_COMPANY;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.SPOKEN_LANGUAGE;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.TAGLINE;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.TAGS;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.WRITER;
import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.swingbinding.JListBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.MovieTextMatcherList;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.JHintLabel;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;
import org.tinymediamanager.ui.components.combobox.AutocompleteSupport;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link MovieUiSettingsPanel} is used for displaying the UI settings for the movie section
 * 
 * @author Manuel Laggner
 */
class MovieUiSettingsPanel extends JPanel {
  private static final int                                 COL_COUNT = 7;

  private final MovieSettings                              settings  = MovieModuleManager.getInstance().getSettings();
  private AutocompleteComboBox<String>                     cbRating;
  private JCheckBox                                        chckbxMovieTableTooltips;

  private JList                                            listRatings;
  private JButton                                          btnAddRating;
  private JButton                                          btnRemoveRating;
  private JButton                                          btnMoveRatingUp;
  private JButton                                          btnMoveRatingDown;

  private JCheckBox                                        chckbxShowPoster;
  private JCheckBox                                        chckbxShowFanart;
  private JCheckBox                                        chckbxShowBanner;
  private JCheckBox                                        chckbxShowThumb;
  private JCheckBox                                        chckbxShowClearlogo;

  private JCheckBox                                        chckbxTitle;
  private JCheckBox                                        chckbxSortableTitle;
  private JCheckBox                                        chckbxOriginalTitle;
  private JCheckBox                                        chckbxSortableOriginalTitle;
  private JCheckBox                                        chckbxSortTitle;

  private JCheckBox                                        chckbxUniversalNote;
  private JCheckBox                                        chckbxUniversalFilename;
  private JCheckBox                                        chckbxUniversalTags;
  private JCheckBox                                        chckbxUniversalProductionCompany;
  private JCheckBox                                        chckbxUniversalCountry;
  private JCheckBox                                        chckbxUniversalLanguages;
  private JCheckBox                                        chckbxUniversalActors;
  private JCheckBox                                        chckbxUniversalProducers;
  private JCheckBox                                        chckbxUniversalDirectors;
  private JCheckBox                                        chckbxUniversalWriters;
  private JCheckBox                                        chckbxUniversalPlot;
  private JCheckBox                                        chckbxUniversalTagLine;
  private JCheckBox                                        chckbxStoreFilter;
  private JHintCheckBox                                    chckbxDisplayAllMissingMetadata;
  private JHintCheckBox                                    chckbxDisplayAllMissingArtwork;

  private final Map<MovieScraperMetadataConfig, JCheckBox> metadataCheckBoxes;
  private final Map<MovieScraperMetadataConfig, JCheckBox> artworkCheckBoxes;
  private final ItemListener                               checkBoxListener;

  public MovieUiSettingsPanel() {
    metadataCheckBoxes = new LinkedHashMap<>();
    artworkCheckBoxes = new LinkedHashMap<>();
    checkBoxListener = e -> checkChanges();

    // UI initializations
    initComponents();
    initDataBindings();

    // logic initializations
    btnAddRating.addActionListener(arg0 -> {
      Object selectedItem = cbRating.getSelectedItem();

      // check, if text is selected (from auto completion), in this case we just
      // remove the selection
      Component editorComponent = cbRating.getEditor().getEditorComponent();
      if (editorComponent instanceof JTextField) {
        JTextField tf = (JTextField) editorComponent;
        String selectedText = tf.getSelectedText();
        if (selectedText != null) {
          tf.setSelectionStart(0);
          tf.setSelectionEnd(0);
          tf.setCaretPosition(tf.getText().length());
          return;
        }
      }

      if (selectedItem instanceof String str && StringUtils.isNotBlank(str)) {
        MovieModuleManager.getInstance().getSettings().addRatingSource(str);

        // set text combobox text input to ""
        if (editorComponent instanceof JTextField) {
          AutocompleteSupport<String> autoCompleteSupport = cbRating.getAutoCompleteSupport();
          autoCompleteSupport.setFirstItem(null);
          cbRating.setSelectedIndex(0);
          autoCompleteSupport.removeFirstItem();
        }
      }

    });

    btnRemoveRating.addActionListener(arg0 -> {
      int row = listRatings.getSelectedIndex();
      if (row != -1) { // nothing selected
        String ratingSource = settings.getRatingSources().get(row);
        MovieModuleManager.getInstance().getSettings().removeRatingSource(ratingSource);
      }
    });

    btnMoveRatingUp.addActionListener(arg0 -> {
      int row = listRatings.getSelectedIndex();
      if (row != -1 && row != 0) {
        settings.swapRatingSources(row, row - 1);
        row = row - 1;
        listRatings.setSelectedIndex(row);
        listRatings.updateUI();
      }
    });

    btnMoveRatingDown.addActionListener(arg0 -> {
      int row = listRatings.getSelectedIndex();
      if (row != -1 && row < listRatings.getModel().getSize() - 1) {
        settings.swapRatingSources(row, row + 1);
        row = row + 1;
        listRatings.setSelectedIndex(row);
        listRatings.updateUI();
      }
    });

    buildCheckBoxes();
  }

  private void checkChanges() {
    // show artwork
    List<MediaFileType> artworkTypes = new ArrayList<>();
    if (chckbxShowPoster.isSelected()) {
      artworkTypes.add(MediaFileType.POSTER);
    }
    if (chckbxShowFanart.isSelected()) {
      artworkTypes.add(MediaFileType.FANART);
    }
    if (chckbxShowBanner.isSelected()) {
      artworkTypes.add(MediaFileType.BANNER);
    }
    if (chckbxShowThumb.isSelected()) {
      artworkTypes.add(MediaFileType.THUMB);
    }
    if (chckbxShowClearlogo.isSelected()) {
      artworkTypes.add(MediaFileType.CLEARLOGO);
    }
    settings.setShowArtworkTypes(artworkTypes);

    // universal filter
    List<AbstractSettings.UniversalFilterFields> universalFilterFields = new ArrayList<>();
    if (chckbxUniversalNote.isSelected()) {
      universalFilterFields.add(NOTE);
    }
    if (chckbxUniversalFilename.isSelected()) {
      universalFilterFields.add(FILENAME);
    }
    if (chckbxUniversalTags.isSelected()) {
      universalFilterFields.add(TAGS);
    }
    if (chckbxUniversalCountry.isSelected()) {
      universalFilterFields.add(COUNTRY);
    }
    if (chckbxUniversalProductionCompany.isSelected()) {
      universalFilterFields.add(PRODUCTION_COMPANY);
    }
    if (chckbxUniversalLanguages.isSelected()) {
      universalFilterFields.add(SPOKEN_LANGUAGE);
    }
    if (chckbxUniversalActors.isSelected()) {
      universalFilterFields.add(ACTOR);
    }
    if (chckbxUniversalProducers.isSelected()) {
      universalFilterFields.add(PRODUCER);
    }
    if (chckbxUniversalWriters.isSelected()) {
      universalFilterFields.add(WRITER);
    }
    if (chckbxUniversalDirectors.isSelected()) {
      universalFilterFields.add(DIRECTOR);
    }
    if (chckbxUniversalPlot.isSelected()) {
      universalFilterFields.add(PLOT);
    }
    if (chckbxUniversalTagLine.isSelected()) {
      universalFilterFields.add(TAGLINE);
    }
    settings.setUniversalFilterFields(universalFilterFields);

    // metadata
    settings.clearMovieCheckMetadata();
    for (Map.Entry<MovieScraperMetadataConfig, JCheckBox> entry : metadataCheckBoxes.entrySet()) {
      MovieScraperMetadataConfig key = entry.getKey();
      JCheckBox value = entry.getValue();
      if (value.isSelected()) {
        settings.addMovieCheckMetadata(key);
      }
    }

    // artwork
    settings.clearMovieCheckArtwork();
    for (Map.Entry<MovieScraperMetadataConfig, JCheckBox> entry : artworkCheckBoxes.entrySet()) {
      MovieScraperMetadataConfig key = entry.getKey();
      JCheckBox value = entry.getValue();
      if (value.isSelected()) {
        settings.addMovieCheckArtwork(key);
      }
    }
  }

  private void buildCheckBoxes() {
    // show artwork
    for (MediaFileType artworkType : settings.getShowArtworkTypes()) {
      switch (artworkType) {
        case POSTER:
          chckbxShowPoster.setSelected(true);
          break;

        case FANART:
          chckbxShowFanart.setSelected(true);
          break;

        case BANNER:
          chckbxShowBanner.setSelected(true);
          break;

        case THUMB:
          chckbxShowThumb.setSelected(true);
          break;

        case CLEARLOGO:
          chckbxShowClearlogo.setSelected(true);
          break;
      }
    }

    // universal filter
    for (AbstractSettings.UniversalFilterFields filterField : settings.getUniversalFilterFields()) {
      switch (filterField) {
        case NOTE:
          chckbxUniversalNote.setSelected(true);
          break;

        case FILENAME:
          chckbxUniversalFilename.setSelected(true);
          break;

        case TAGS:
          chckbxUniversalTags.setSelected(true);
          break;

        case PRODUCTION_COMPANY:
          chckbxUniversalProductionCompany.setSelected(true);
          break;

        case COUNTRY:
          chckbxUniversalCountry.setSelected(true);
          break;

        case SPOKEN_LANGUAGE:
          chckbxUniversalLanguages.setSelected(true);
          break;

        case ACTOR:
          chckbxUniversalActors.setSelected(true);
          break;

        case DIRECTOR:
          chckbxUniversalDirectors.setSelected(true);
          break;

        case PRODUCER:
          chckbxUniversalProducers.setSelected(true);
          break;

        case WRITER:
          chckbxUniversalWriters.setSelected(true);
          break;

        case PLOT:
          chckbxUniversalPlot.setSelected(true);
          break;

        case TAGLINE:
          chckbxUniversalTagLine.setSelected(true);
          break;
      }
    }

    // set the checkbox listener at the end!
    chckbxShowPoster.addItemListener(checkBoxListener);
    chckbxShowFanart.addItemListener(checkBoxListener);
    chckbxShowBanner.addItemListener(checkBoxListener);
    chckbxShowThumb.addItemListener(checkBoxListener);
    chckbxShowClearlogo.addItemListener(checkBoxListener);
    chckbxUniversalNote.addItemListener(checkBoxListener);
    chckbxUniversalFilename.addItemListener(checkBoxListener);
    chckbxUniversalTags.addItemListener(checkBoxListener);
    chckbxUniversalProductionCompany.addItemListener(checkBoxListener);
    chckbxUniversalCountry.addItemListener(checkBoxListener);
    chckbxUniversalLanguages.addItemListener(checkBoxListener);
    chckbxUniversalActors.addItemListener(checkBoxListener);
    chckbxUniversalProducers.addItemListener(checkBoxListener);
    chckbxUniversalDirectors.addItemListener(checkBoxListener);
    chckbxUniversalWriters.addItemListener(checkBoxListener);
    chckbxUniversalPlot.addItemListener(checkBoxListener);
    chckbxUniversalTagLine.addItemListener(checkBoxListener);

    // metadata
    for (MovieScraperMetadataConfig value : settings.getMovieCheckMetadata()) {
      JCheckBox checkBox = metadataCheckBoxes.get(value);
      if (checkBox != null) {
        checkBox.setSelected(true);
      }
    }

    // set the checkbox listener at the end!
    for (JCheckBox checkBox : metadataCheckBoxes.values()) {
      checkBox.addItemListener(checkBoxListener);
    }

    // artwork
    for (MovieScraperMetadataConfig value : settings.getMovieCheckArtwork()) {
      JCheckBox checkBox = artworkCheckBoxes.get(value);
      if (checkBox != null) {
        checkBox.setSelected(true);
      }
    }

    // set the checkbox listener at the end!
    for (JCheckBox checkBox : artworkCheckBoxes.values()) {
      checkBox.addItemListener(checkBoxListener);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[grow]", "[][15lp!][][15lp!][][15lp!][]"));
    {
      JPanel panelUiSettings = new JPanel();
      // 16lp ~ width of the checkbox
      panelUiSettings
          .setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][10lp!][][grow][][][][][][10lp!][][125lp,grow]"));

      JLabel lblUiSettings = new TmmLabel(TmmResourceBundle.getString("Settings.ui"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelUiSettings, lblUiSettings, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#ui-settings"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");

      {
        JLabel lblNewLabel = new JLabel(TmmResourceBundle.getString("Settings.showartworktypes"));
        panelUiSettings.add(lblNewLabel, "cell 1 0 2 1");

        chckbxShowPoster = new JCheckBox(TmmResourceBundle.getString("mediafiletype.poster"));
        panelUiSettings.add(chckbxShowPoster, "flowx,cell 2 1");

        chckbxShowFanart = new JCheckBox(TmmResourceBundle.getString("mediafiletype.fanart"));
        panelUiSettings.add(chckbxShowFanart, "cell 2 1");

        chckbxShowBanner = new JCheckBox(TmmResourceBundle.getString("mediafiletype.banner"));
        panelUiSettings.add(chckbxShowBanner, "cell 2 1");

        chckbxShowThumb = new JCheckBox(TmmResourceBundle.getString("mediafiletype.thumb"));
        panelUiSettings.add(chckbxShowThumb, "cell 2 1");

        chckbxShowClearlogo = new JCheckBox(TmmResourceBundle.getString("mediafiletype.clearlogo"));
        panelUiSettings.add(chckbxShowClearlogo, "cell 2 1");
      }
      {
        chckbxMovieTableTooltips = new JCheckBox(TmmResourceBundle.getString("Settings.movie.showtabletooltips"));
        panelUiSettings.add(chckbxMovieTableTooltips, "cell 1 2 2 1");
      }
      {
        JLabel lblCheckMetadata = new JLabel(TmmResourceBundle.getString("Settings.checkmetadata"));
        panelUiSettings.add(lblCheckMetadata, "cell 1 4 2 1");

        JPanel panelCheckMetadata = new JPanel();
        panelCheckMetadata.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.ipadx = 10;

        // Metadata
        for (MovieScraperMetadataConfig value : MovieScraperMetadataConfig.values()) {
          if (value.isMetaData()) {
            addMetadataCheckbox(panelCheckMetadata, value, metadataCheckBoxes, gbc);
          }
        }

        // cast
        gbc.gridx = 0;
        gbc.gridy++;
        for (MovieScraperMetadataConfig value : MovieScraperMetadataConfig.values()) {
          if (value.isCast()) {
            addMetadataCheckbox(panelCheckMetadata, value, metadataCheckBoxes, gbc);
          }
        }
        panelUiSettings.add(panelCheckMetadata, "cell 2 5");

        chckbxDisplayAllMissingMetadata = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkmetadata.displayall"));
        chckbxDisplayAllMissingMetadata.setToolTipText(TmmResourceBundle.getString("Settings.checkmetadata.displayall.desc"));
        chckbxDisplayAllMissingMetadata.setHintIcon(IconManager.HINT);
        panelUiSettings.add(chckbxDisplayAllMissingMetadata, "cell 2 6");
      }

      {
        JLabel lblCheckImages = new JLabel(TmmResourceBundle.getString("Settings.checkimages"));
        panelUiSettings.add(lblCheckImages, "cell 1 8 2 1");

        JPanel panelCheckImages = new JPanel();
        panelCheckImages.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.ipadx = 10;

        // Artwork
        for (MovieScraperMetadataConfig value : MovieScraperMetadataConfig.values()) {
          if (value.isArtwork()) {
            addMetadataCheckbox(panelCheckImages, value, artworkCheckBoxes, gbc);
          }
        }

        panelUiSettings.add(panelCheckImages, "cell 2 9");

        chckbxDisplayAllMissingArtwork = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkimages.displayall"));
        chckbxDisplayAllMissingArtwork.setToolTipText(TmmResourceBundle.getString("Settings.checkimages.displayall.desc"));
        chckbxDisplayAllMissingArtwork.setHintIcon(IconManager.HINT);
        panelUiSettings.add(chckbxDisplayAllMissingArtwork, "cell 2 10");
      }
      {
        JLabel lblRating = new JLabel(TmmResourceBundle.getString("Settings.preferredrating"));
        panelUiSettings.add(lblRating, "cell 1 12 2 1");

        JPanel panelRatingSource = new JPanel();
        panelUiSettings.add(panelRatingSource, "cell 2 13,grow");
        panelRatingSource.setLayout(new MigLayout("insets 0", "[100lp][]", "[grow][]"));
        {
          listRatings = new JList();
          listRatings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

          JScrollPane scrollPane = new JScrollPane();
          scrollPane.setViewportView(listRatings);
          panelRatingSource.add(scrollPane, "cell 0 0,grow");

          btnMoveRatingUp = new SquareIconButton(IconManager.ARROW_UP_INV);
          btnMoveRatingUp.setToolTipText(TmmResourceBundle.getString("Button.moveup"));
          panelRatingSource.add(btnMoveRatingUp, "flowy,cell 1 0,aligny bottom");

          btnMoveRatingDown = new SquareIconButton(IconManager.ARROW_DOWN_INV);
          btnMoveRatingDown.setToolTipText(TmmResourceBundle.getString("Button.movedown"));
          panelRatingSource.add(btnMoveRatingDown, "cell 1 0,aligny bottom");

          cbRating = new AutocompleteComboBox(
              Arrays.asList(MediaMetadata.IMDB, MediaMetadata.TMDB, MediaMetadata.METACRITIC, "tomatometerallcritics", "user"));
          panelRatingSource.add(cbRating, "cell 0 1,growx");

          btnRemoveRating = new SquareIconButton(IconManager.REMOVE_INV);
          btnRemoveRating.setToolTipText(TmmResourceBundle.getString("Button.remove"));
          panelRatingSource.add(btnRemoveRating, "cell 1 0");

          btnAddRating = new SquareIconButton(IconManager.ADD_INV);
          btnAddRating.setToolTipText(TmmResourceBundle.getString("Button.add"));
          panelRatingSource.add(btnAddRating, "cell 1 1");
        }
      }
    }
    {
      JPanel panelFilter = new JPanel();
      panelFilter.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][10lp!][]")); // 16lp ~ width of the

      JLabel lblAutomaticTasksT = new TmmLabel(TmmResourceBundle.getString("Settings.filters"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelFilter, lblAutomaticTasksT, true);
      JLabel lblMovieFilter = new JLabel(TmmResourceBundle.getString("Settings.movietitlefilter"));
      panelFilter.add(lblMovieFilter, "cell 1 0 2 1");

      chckbxTitle = new JCheckBox(MovieTextMatcherList.TITLE.toString());
      panelFilter.add(chckbxTitle, "flowx,cell 2 1");

      chckbxSortableTitle = new JCheckBox(MovieTextMatcherList.TITLE_SORTABLE.toString());
      panelFilter.add(chckbxSortableTitle, "cell 2 1");

      JLabel lblSortableTitleHint = new JLabel(IconManager.HINT);
      panelFilter.add(lblSortableTitleHint, "cell 2 1");
      lblSortableTitleHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.renamer.${titleSortable}"));

      chckbxOriginalTitle = new JCheckBox(MovieTextMatcherList.ORIGINAL_TITLE.toString());
      panelFilter.add(chckbxOriginalTitle, "cell 2 1");

      chckbxSortableOriginalTitle = new JCheckBox(MovieTextMatcherList.ORIGINAL_TITLE_SORTABLE.toString());
      panelFilter.add(chckbxSortableOriginalTitle, "cell 2 1");

      JLabel lblSortableOriginalTitleHint = new JLabel(IconManager.HINT);
      panelFilter.add(lblSortableOriginalTitleHint, "cell 2 1");
      lblSortableOriginalTitleHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.renamer.${titleSortable}"));
      {

        chckbxSortTitle = new JCheckBox(MovieTextMatcherList.SORTED_TITLE.toString());
        panelFilter.add(chckbxSortTitle, "cell 2 1");
      }
      JHintLabel lblUniversalFilter = new JHintLabel(TmmResourceBundle.getString("filter.universal"));
      panelFilter.add(lblUniversalFilter, "cell 1 2 2 1");
      lblUniversalFilter.setHintIcon(IconManager.HINT);
      lblUniversalFilter.setToolTipText(TmmResourceBundle.getString("filter.universal.hint"));

      chckbxUniversalNote = new JCheckBox(TmmResourceBundle.getString("metatag.note"));
      panelFilter.add(chckbxUniversalNote, "flowx,cell 2 3");

      chckbxUniversalFilename = new JCheckBox(TmmResourceBundle.getString("metatag.filename"));
      panelFilter.add(chckbxUniversalFilename, "cell 2 3");

      chckbxUniversalTags = new JCheckBox(TmmResourceBundle.getString("metatag.tags"));
      panelFilter.add(chckbxUniversalTags, "cell 2 3");

      chckbxUniversalProductionCompany = new JCheckBox(TmmResourceBundle.getString("metatag.production"));
      panelFilter.add(chckbxUniversalProductionCompany, "cell 2 3");

      chckbxUniversalCountry = new JCheckBox(TmmResourceBundle.getString("metatag.country"));
      panelFilter.add(chckbxUniversalCountry, "cell 2 3");

      chckbxUniversalLanguages = new JCheckBox(TmmResourceBundle.getString("metatag.spokenlanguages"));
      panelFilter.add(chckbxUniversalLanguages, "cell 2 3");

      chckbxUniversalActors = new JCheckBox(TmmResourceBundle.getString("metatag.actors"));
      panelFilter.add(chckbxUniversalActors, "flowx,cell 2 4");

      chckbxUniversalProducers = new JCheckBox(TmmResourceBundle.getString("metatag.producers"));
      panelFilter.add(chckbxUniversalProducers, "cell 2 4");

      chckbxUniversalDirectors = new JCheckBox(TmmResourceBundle.getString("metatag.directors"));
      panelFilter.add(chckbxUniversalDirectors, "cell 2 4");

      chckbxUniversalWriters = new JCheckBox(TmmResourceBundle.getString("metatag.writers"));
      panelFilter.add(chckbxUniversalWriters, "cell 2 4");

      chckbxUniversalPlot = new JCheckBox(TmmResourceBundle.getString("metatag.plot"));
      panelFilter.add(chckbxUniversalPlot, "cell 2 4");

      {

        chckbxUniversalTagLine = new JCheckBox(TmmResourceBundle.getString("metatag.tagline"));
        panelFilter.add(chckbxUniversalTagLine, "cell 2 4");
      }
      {
        chckbxStoreFilter = new JCheckBox(TmmResourceBundle.getString("Settings.movie.persistuifilter"));
        panelFilter.add(chckbxStoreFilter, "cell 1 6 2 1");
      }
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#filter"));
      add(collapsiblePanel, "cell 0 2,growx,wmin 0");
    }
  }

  private void addMetadataCheckbox(JPanel panel, MovieScraperMetadataConfig config, Map<MovieScraperMetadataConfig, JCheckBox> map,
      GridBagConstraints gbc) {
    JCheckBox checkBox;
    if (StringUtils.isNotBlank(config.getToolTip())) {
      checkBox = new JHintCheckBox(config.getDescription());
      checkBox.setToolTipText(config.getToolTip());
      ((JHintCheckBox) checkBox).setHintIcon(IconManager.HINT);
    }
    else {
      checkBox = new JCheckBox(config.getDescription());
    }
    map.put(config, checkBox);

    if (gbc.gridx >= COL_COUNT) {
      gbc.gridx = 0;
      gbc.gridy++;
    }
    panel.add(checkBox, gbc);

    gbc.gridx++;
  }

  protected void initDataBindings() {
    Property movieSettingsBeanProperty_13 = BeanProperty.create("title");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_13 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_13, chckbxTitle,
        jCheckBoxBeanProperty);
    autoBinding_13.bind();
    //
    Property movieSettingsBeanProperty_14 = BeanProperty.create("sortableTitle");
    AutoBinding autoBinding_14 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_14, chckbxSortableTitle,
        jCheckBoxBeanProperty);
    autoBinding_14.bind();
    //
    Property movieSettingsBeanProperty_15 = BeanProperty.create("originalTitle");
    AutoBinding autoBinding_15 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_15, chckbxOriginalTitle,
        jCheckBoxBeanProperty);
    autoBinding_15.bind();
    //
    Property movieSettingsBeanProperty_16 = BeanProperty.create("sortableOriginalTitle");
    AutoBinding autoBinding_16 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_16,
        chckbxSortableOriginalTitle, jCheckBoxBeanProperty);
    autoBinding_16.bind();
    //
    Property movieSettingsBeanProperty_17 = BeanProperty.create("sortTitle");
    AutoBinding autoBinding_17 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_17, chckbxSortTitle,
        jCheckBoxBeanProperty);
    autoBinding_17.bind();
    //
    Property movieSettingsBeanProperty = BeanProperty.create("ratingSources");
    JListBinding jListBinding = SwingBindings.createJListBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, listRatings);
    jListBinding.bind();
    //
    Property movieSettingsBeanProperty_5 = BeanProperty.create("showMovieTableTooltips");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_5, chckbxMovieTableTooltips,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property movieSettingsBeanProperty_19 = BeanProperty.create("storeUiFilters");
    AutoBinding autoBinding_21 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_19, chckbxStoreFilter,
        jCheckBoxBeanProperty);
    autoBinding_21.bind();
    //
    Property movieSettingsBeanProperty_20 = BeanProperty.create("movieDisplayAllMissingMetadata");
    AutoBinding autoBinding_24 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_20,
        chckbxDisplayAllMissingMetadata, jCheckBoxBeanProperty);
    autoBinding_24.bind();
    //
    Property movieSettingsBeanProperty_21 = BeanProperty.create("movieDisplayAllMissingArtwork");
    AutoBinding autoBinding_25 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_21,
        chckbxDisplayAllMissingArtwork, jCheckBoxBeanProperty);
    autoBinding_25.bind();
  }
}
