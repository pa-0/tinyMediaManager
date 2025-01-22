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
package org.tinymediamanager.ui.wizard;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.html.HTMLEditorKit;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.observablecollections.ObservableCollections;
import org.jdesktop.swingbinding.JTableBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.connector.MovieConnectors;
import org.tinymediamanager.core.movie.filenaming.MovieNfoNaming;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.TableColumnResizer;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.movies.MovieScraper;
import org.tinymediamanager.ui.panels.MediaScraperConfigurationPanel;
import org.tinymediamanager.ui.panels.ScrollablePanel;

import net.miginfocom.swing.MigLayout;

/**
 * The class MovieScraperPanel is used to maintain the movie scrapers in the wizard
 * 
 * @author Manuel Laggner
 */
class MovieScraperPanel extends JPanel {
  private final MovieSettings        settings = MovieModuleManager.getInstance().getSettings();
  private final List<MovieScraper>   scrapers = ObservableCollections.observableList(new ArrayList<>());

  private JTable                     tableScraper;
  private JComboBox<MediaLanguages>  cbScraperLanguage;
  private JComboBox<CountryCode>     cbCertificationCountry;
  private JComboBox<MovieConnectors> cbNfoFormat;
  private JTextPane                  tpScraperDescription;
  private JCheckBox                  chckbxScraperFallback;
  private JCheckBox                  cbMovieNfoFilename1;
  private JCheckBox                  cbMovieNfoFilename2;
  private JPanel                     panelScraperOptions;

  public MovieScraperPanel() {
    // data init before UI init
    MediaScraper defaultMediaScraper = MovieModuleManager.getInstance().getMovieList().getDefaultMediaScraper();
    int selectedIndex = 0;
    int counter = 0;
    for (MediaScraper scraper : MovieModuleManager.getInstance().getMovieList().getAvailableMediaScrapers()) {
      MovieScraper movieScraper = new MovieScraper(scraper);
      if (scraper.equals(defaultMediaScraper)) {
        movieScraper.setDefaultScraper(true);
        selectedIndex = counter;
      }
      scrapers.add(movieScraper);
      counter++;
    }

    // UI init
    initComponents();
    initDataBindings();

    // init data after UI init
    // NFO filenames
    List<MovieNfoNaming> movieNfoFilenames = settings.getNfoFilenames();
    if (movieNfoFilenames.contains(MovieNfoNaming.FILENAME_NFO)) {
      cbMovieNfoFilename1.setSelected(true);
    }
    if (movieNfoFilenames.contains(MovieNfoNaming.MOVIE_NFO)) {
      cbMovieNfoFilename2.setSelected(true);
    }

    // item listener
    cbMovieNfoFilename1.addItemListener(e -> checkChanges());
    cbMovieNfoFilename2.addItemListener(e -> checkChanges());

    // adjust table columns
    // Checkbox and Logo shall have minimal width
    TableColumnResizer.setMaxWidthForColumn(tableScraper, 0, 2);
    TableColumnResizer.setMaxWidthForColumn(tableScraper, 1, 2);
    TableColumnResizer.adjustColumnPreferredWidths(tableScraper, 5);

    // implement listener to simulate button group
    tableScraper.getModel().addTableModelListener(arg0 -> {
      // click on the checkbox
      if (arg0.getColumn() == 0) {
        int row = arg0.getFirstRow();
        MovieScraper changedScraper = scrapers.get(row);
        // if flag default scraper was changed, change all other flags
        if (changedScraper.getDefaultScraper()) {
          settings.setMovieScraper(changedScraper.getScraperId());
          for (MovieScraper scraper : scrapers) {
            if (scraper != changedScraper) {
              scraper.setDefaultScraper(Boolean.FALSE);
            }
          }
        }
      }
    });

    // implement selection listener to load settings
    tableScraper.getSelectionModel().addListSelectionListener(e -> {
      int index = tableScraper.convertRowIndexToModel(tableScraper.getSelectedRow());
      if (index > -1) {
        panelScraperOptions.removeAll();
        if (scrapers.get(index).getMediaProvider().getProviderInfo().getConfig().hasConfig()) {
          panelScraperOptions.add(new MediaScraperConfigurationPanel(scrapers.get(index).getMediaProvider()));
        }
        panelScraperOptions.revalidate();
      }
    });

    // select default movie scraper
    if (counter > 0) {
      tableScraper.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
    }
  }

  /*
   * init components
   */
  private void initComponents() {
    setLayout(new MigLayout("", "[grow]", "[][400lp,grow]"));

    JLabel lblMovieScraper = new JLabel(TmmResourceBundle.getString("wizard.movie.scraper"));
    add(lblMovieScraper, "cell 0 0");
    TmmFontHelper.changeFont(lblMovieScraper, 1.3333, Font.BOLD);
    JPanel panelMovieScrapers = new JPanel();

    add(panelMovieScrapers, "cell 0 1,grow");
    panelMovieScrapers.setLayout(new MigLayout("", "[][][600lp,grow]", "[150lp:200lp,grow][][][][][20lp][][]"));

    JScrollPane scrollPaneScraper = new JScrollPane();
    panelMovieScrapers.add(scrollPaneScraper, "cell 0 0 2 1,grow");

    tableScraper = new TmmTable() {
      @Override
      public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        java.awt.Component comp = super.prepareRenderer(renderer, row, column);
        MovieScraper scraper = scrapers.get(row);
        comp.setEnabled(scraper.isEnabled());
        return comp;
      }
    };
    tableScraper.setRowHeight(29);
    tableScraper.setShowGrid(true);
    scrollPaneScraper.setViewportView(tableScraper);

    {
      JScrollPane scrollPaneScraperDetails = new NoBorderScrollPane();
      panelMovieScrapers.add(scrollPaneScraperDetails, "cell 2 0 1 8,grow");
      scrollPaneScraperDetails.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

      JPanel panelScraperDetails = new ScrollablePanel();
      scrollPaneScraperDetails.setViewportView(panelScraperDetails);
      panelScraperDetails.setLayout(new MigLayout("", "[grow]", "[][]"));

      tpScraperDescription = new JTextPane();
      tpScraperDescription.setOpaque(false);
      tpScraperDescription.setEditorKit(new HTMLEditorKit());
      panelScraperDetails.add(tpScraperDescription, "cell 0 0,growx,aligny top");

      panelScraperOptions = new JPanel();
      panelScraperOptions.setLayout(new FlowLayout(FlowLayout.LEFT));
      panelScraperDetails.add(panelScraperOptions, "cell 0 1,growx,aligny top");
    }

    JLabel lblScraperLanguage = new JLabel(TmmResourceBundle.getString("Settings.preferredLanguage"));
    panelMovieScrapers.add(lblScraperLanguage, "cell 0 2");

    cbScraperLanguage = new JComboBox();
    cbScraperLanguage.setModel(new DefaultComboBoxModel<>(MediaLanguages.valuesSorted()));
    panelMovieScrapers.add(cbScraperLanguage, "cell 1 2");

    JLabel lblCountry = new JLabel(TmmResourceBundle.getString("Settings.certificationCountry"));
    panelMovieScrapers.add(lblCountry, "cell 0 3");

    cbCertificationCountry = new JComboBox(CountryCode.values());
    cbCertificationCountry.setModel(new DefaultComboBoxModel<>(CountryCode.values()));
    panelMovieScrapers.add(cbCertificationCountry, "cell 1 3");

    chckbxScraperFallback = new JCheckBox(TmmResourceBundle.getString("Settings.scraperfallback"));
    panelMovieScrapers.add(chckbxScraperFallback, "cell 0 4 2 1");
    {
      JLabel lblNfoFormat = new JLabel("NFO format");
      panelMovieScrapers.add(lblNfoFormat, "flowx,cell 0 6 2 1");
    }
    {
      JPanel panel = new JPanel();
      panelMovieScrapers.add(panel, "cell 0 7 2 1,growx");
      panel.setLayout(new MigLayout("insets 0", "[][][][]", "[][]"));

      JLabel lblNfoFileNaming = new JLabel(TmmResourceBundle.getString("Settings.nofFileNaming"));
      panel.add(lblNfoFileNaming, "cell 0 0");

      cbMovieNfoFilename1 = new JCheckBox(TmmResourceBundle.getString("Settings.moviefilename") + ".nfo");
      panel.add(cbMovieNfoFilename1, "cell 1 0");

      cbMovieNfoFilename2 = new JCheckBox("movie.nfo");
      panel.add(cbMovieNfoFilename2, "cell 1 1");
    }
    {
      cbNfoFormat = new JComboBox(MovieConnectors.values());
      panelMovieScrapers.add(cbNfoFormat, "cell 0 6 2 1");
    }
  }

  private void checkChanges() {
    // set NFO filenames
    settings.clearNfoFilenames();
    if (cbMovieNfoFilename1.isSelected()) {
      settings.addNfoFilename(MovieNfoNaming.FILENAME_NFO);
    }
    if (cbMovieNfoFilename2.isSelected()) {
      settings.addNfoFilename(MovieNfoNaming.MOVIE_NFO);
    }
  }

  protected void initDataBindings() {
    BeanProperty<MovieSettings, MediaLanguages> settingsBeanProperty_8 = BeanProperty.create("scraperLanguage");
    BeanProperty<JComboBox, Object> jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding<MovieSettings, MediaLanguages, JComboBox, Object> autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_8, cbScraperLanguage, jComboBoxBeanProperty);
    autoBinding_7.bind();
    //
    BeanProperty<MovieSettings, CountryCode> settingsBeanProperty_9 = BeanProperty.create("certificationCountry");
    AutoBinding<MovieSettings, CountryCode, JComboBox, Object> autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_9, cbCertificationCountry, jComboBoxBeanProperty);
    autoBinding_8.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_1 = BeanProperty.create("scraperFallback");
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_1, chckbxScraperFallback, jCheckBoxBeanProperty);
    autoBinding_1.bind();
    //
    JTableBinding<MovieScraper, List<MovieScraper>, JTable> jTableBinding = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, scrapers,
        tableScraper);
    //
    BeanProperty<MovieScraper, Boolean> movieScraperBeanProperty = BeanProperty.create("defaultScraper");
    jTableBinding.addColumnBinding(movieScraperBeanProperty)
        .setColumnName(TmmResourceBundle.getString("Settings.default"))
        .setColumnClass(Boolean.class);
    //
    BeanProperty<MovieScraper, Icon> movieScraperBeanProperty_1 = BeanProperty.create("scraperLogo");
    jTableBinding.addColumnBinding(movieScraperBeanProperty_1).setColumnClass(Icon.class).setEditable(false);
    //
    BeanProperty<MovieScraper, String> movieScraperBeanProperty_2 = BeanProperty.create("scraperName");
    jTableBinding.addColumnBinding(movieScraperBeanProperty_2).setColumnName(TmmResourceBundle.getString("metatag.name")).setEditable(false);
    //
    jTableBinding.bind();
    //
    BeanProperty<JTable, String> jTableBeanProperty = BeanProperty.create("selectedElement.scraperDescription");
    BeanProperty<JTextPane, String> jTextPaneBeanProperty = BeanProperty.create("text");
    AutoBinding<JTable, String, JTextPane, String> autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ, tableScraper, jTableBeanProperty,
        tpScraperDescription, jTextPaneBeanProperty);
    autoBinding_12.bind();
    //
    BeanProperty<MovieSettings, MovieConnectors> movieSettingsBeanProperty = BeanProperty.create("movieConnector");
    AutoBinding<MovieSettings, MovieConnectors, JComboBox, Object> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        movieSettingsBeanProperty, cbNfoFormat, jComboBoxBeanProperty);
    autoBinding.bind();
  }
}
