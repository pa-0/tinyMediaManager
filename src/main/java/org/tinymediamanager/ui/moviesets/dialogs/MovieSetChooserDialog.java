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

import static java.util.Locale.ROOT;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BACKGROUND;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARLOGO;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.DISC;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.observablecollections.ObservableCollections;
import org.jdesktop.swingbinding.JTableBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSetScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSetSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.interfaces.IMovieSetMetadataProvider;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.ScraperMetadataConfigCheckComboBox;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.dialogs.ImageChooserDialog;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.moviesets.MovieSetChooserModel;
import org.tinymediamanager.ui.moviesets.MovieSetChooserModel.MovieInSet;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieSetChooserPanel.
 * 
 * @author Manuel Laggner
 */
public class MovieSetChooserDialog extends TmmDialog implements ActionListener {
  private static final Logger                                                     LOGGER         = LoggerFactory
      .getLogger(MovieSetChooserDialog.class);

  private final MovieSet                                                          movieSetToScrape;
  private final List<MovieSetChooserModel>                                        movieSetsFound = ObservableCollections
      .observableList(new ArrayList<>());
  private final JLabel                                                            lblProgressAction;
  private final JProgressBar                                                      progressBar;
  private final JTextField                                                        tfMovieSetName;
  private final TmmTable                                                          tableMovieSets;
  private final JLabel                                                            lblMovieSetName;
  private final ImageLabel                                                        lblMovieSetPoster;
  private final TmmTable                                                          tableMovies;
  private final JCheckBox                                                         cbAssignMovies;
  private final JButton                                                           btnOk;
  private final JTextPane                                                         tpPlot;
  private final ScraperMetadataConfigCheckComboBox<MovieSetScraperMetadataConfig> cbScraperConfig;

  private boolean                                                                 continueQueue  = true;

  /**
   * Instantiates a new movie set chooser panel.
   * 
   * @param movieSet
   *          the movie set
   */
  public MovieSetChooserDialog(MovieSet movieSet, boolean inQueue) {
    super(TmmResourceBundle.getString("movieset.search"), "movieSetChooser");
    setMinimumSize(new Dimension(800, 600));

    movieSetToScrape = movieSet;

    {
      JPanel panelHeader = new JPanel();
      panelHeader.setLayout(new MigLayout("", "[grow][]", "[]"));

      // also attach the actionlistener to the textfield to trigger the search on enter in the textfield
      Action searchAction = new SearchAction();

      tfMovieSetName = new JTextField();
      tfMovieSetName.addActionListener(searchAction);
      panelHeader.add(tfMovieSetName, "cell 0 0,growx");
      tfMovieSetName.setColumns(10);

      JButton btnSearch = new JButton(searchAction);
      panelHeader.add(btnSearch, "cell 1 0");

      setTopInformationPanel(panelHeader);
    }
    {
      JPanel panelContent = new JPanel();
      getContentPane().add(panelContent, BorderLayout.CENTER);
      panelContent.setLayout(new MigLayout("", "[950lp,grow]", "[500,grow][][][]"));

      JSplitPane splitPane = new JSplitPane();
      splitPane.setName(getName() + ".splitPane");
      TmmUILayoutStore.getInstance().install(splitPane);
      panelContent.add(splitPane, "cell 0 0,grow");
      {
        JPanel panelResults = new JPanel();
        panelResults.setLayout(new MigLayout("", "[200lp:300lp,grow]", "[300lp,grow]"));
        JScrollPane panelSearchResults = new JScrollPane();
        panelResults.add(panelSearchResults, "cell 0 0,grow");
        splitPane.setLeftComponent(panelResults);
        {
          tableMovieSets = new TmmTable();
          panelSearchResults.setViewportView(tableMovieSets);
          tableMovieSets.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
          tableMovieSets.configureScrollPane(panelSearchResults);
          ListSelectionModel rowSM = tableMovieSets.getSelectionModel();
          rowSM.addListSelectionListener(e -> {
            // Ignore extra messages.
            if (e.getValueIsAdjusting()) {
              return;
            }

            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            if (!lsm.isSelectionEmpty()) {
              int selectedRow = lsm.getMinSelectionIndex();
              selectedRow = tableMovieSets.convertRowIndexToModel(selectedRow);
              try {
                MovieSetChooserModel model = movieSetsFound.get(selectedRow);
                if (model != MovieSetChooserModel.EMPTY_RESULT && !model.isScraped()) {
                  ScrapeTask task = new ScrapeTask(model);
                  task.execute();
                }
              }
              catch (Exception ex) {
                LOGGER.warn(ex.getMessage());
              }
            }
          });
          tableMovieSets.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              if (e.getClickCount() >= 2 && !e.isConsumed() && e.getButton() == MouseEvent.BUTTON1) {
                actionPerformed(new ActionEvent(btnOk, ActionEvent.ACTION_PERFORMED, "Save"));
              }
            }
          });
        }
      }
      {
        JPanel panelSearchDetail = new JPanel();
        splitPane.setRightComponent(panelSearchDetail);
        panelSearchDetail.setLayout(new MigLayout("", "[150lp:15%:25%,grow][300lp:500lp,grow 3]", "[][250lp,grow][150lp][]"));
        {
          lblMovieSetName = new JLabel("");
          TmmFontHelper.changeFont(lblMovieSetName, 1.166, Font.BOLD);
          panelSearchDetail.add(lblMovieSetName, "cell 0 0 2 1,growx");
        }
        {
          lblMovieSetPoster = new ImageLabel();
          lblMovieSetPoster.setDesiredAspectRatio(2 / 3f);
          panelSearchDetail.add(lblMovieSetPoster, "cell 0 1,grow");
        }
        {
          JScrollPane scrollPane = new NoBorderScrollPane();
          panelSearchDetail.add(scrollPane, "cell 1 1,grow");

          tpPlot = new ReadOnlyTextPane();
          scrollPane.setViewportView(tpPlot);
        }
        {
          JScrollPane scrollPane = new JScrollPane();
          panelSearchDetail.add(scrollPane, "cell 0 2 2 1,grow");

          tableMovies = new TmmTable();
          tableMovies.configureScrollPane(scrollPane);
          scrollPane.setViewportView(tableMovies);
        }
        {
          cbAssignMovies = new JCheckBox(TmmResourceBundle.getString("movieset.movie.assign"));
          cbAssignMovies.setSelected(true);
          panelSearchDetail.add(cbAssignMovies, "cell 0 3 2 1,growx,aligny top");
        }
      }
      {
        JSeparator separator = new JSeparator();
        panelContent.add(separator, "cell 0 1,growx");
      }
      {
        JLabel lblScrapeFollowingItems = new TmmLabel(TmmResourceBundle.getString("chooser.scrape"));
        panelContent.add(lblScrapeFollowingItems, "cell 0 2");

        cbScraperConfig = new ScraperMetadataConfigCheckComboBox(MovieSetScraperMetadataConfig.values());
        cbScraperConfig.enableFilter(
            (movieScraperMetadataConfig, s) -> movieScraperMetadataConfig.getDescription().toLowerCase(ROOT).startsWith(s.toLowerCase(ROOT)));
        panelContent.add(cbScraperConfig, "cell 0 3,growx, wmin 0");
      }
    }

    {
      JPanel infoPanel = new JPanel();
      infoPanel.setLayout(new MigLayout("hidemode 3", "[][grow]", "[]"));

      progressBar = new JProgressBar();
      infoPanel.add(progressBar, "cell 0 0");

      lblProgressAction = new JLabel("");
      infoPanel.add(lblProgressAction, "cell 1 0");

      setBottomInformationPanel(infoPanel);
    }
    {
      if (inQueue) {
        JButton btnAbort = new JButton(TmmResourceBundle.getString("Button.abortqueue"));
        btnAbort.setActionCommand("Abort");
        btnAbort.setToolTipText(TmmResourceBundle.getString("Button.abortqueue"));
        btnAbort.setIcon(IconManager.STOP_INV);
        btnAbort.addActionListener(this);
        addButton(btnAbort);
      }

      JButton btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
      btnCancel.setActionCommand("Cancel");
      btnCancel.setToolTipText(TmmResourceBundle.getString("Button.cancel"));
      btnCancel.setIcon(IconManager.CANCEL_INV);
      btnCancel.addActionListener(this);
      addButton(btnCancel);

      btnOk = new JButton(TmmResourceBundle.getString("Button.ok"));
      btnOk.setActionCommand("Save");
      btnOk.setToolTipText(TmmResourceBundle.getString("Button.ok"));
      btnOk.setIcon(IconManager.APPLY_INV);
      btnOk.addActionListener(this);
      addDefaultButton(btnOk);
    }

    bindingGroup = initDataBindings();

    // adjust table columns
    tableMovies.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tableMovies.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

    cbScraperConfig.setSelectedItems(MovieSetScraperMetadataConfig.values());

    tableMovieSets.getColumnModel().getColumn(0).setHeaderValue(TmmResourceBundle.getString("chooser.searchresult"));
    tfMovieSetName.setText(movieSet.getTitle());
    searchMovieSet();
  }

  private void searchMovieSet() {
    SearchTask task = new SearchTask(tfMovieSetName.getText());
    task.execute();
  }

  private class SearchTask extends SwingWorker<Void, Void> {
    private final String searchTerm;

    public SearchTask(String searchTerm) {
      this.searchTerm = searchTerm;
    }

    @Override
    public Void doInBackground() {
      startProgressBar(TmmResourceBundle.getString("chooser.searchingfor") + " " + searchTerm);
      try {
        List<MediaScraper> sets = MediaScraper.getMediaScrapers(ScraperType.MOVIE_SET);
        if (sets != null && !sets.isEmpty()) {
          MediaScraper first = sets.get(0); // just get first
          IMovieSetMetadataProvider mp = (IMovieSetMetadataProvider) first.getMediaProvider();

          MovieSetSearchAndScrapeOptions options = new MovieSetSearchAndScrapeOptions();
          options.setSearchQuery(searchTerm);
          options.setLanguage(MovieModuleManager.getInstance().getSettings().getScraperLanguage());

          List<MediaSearchResult> movieSets = mp.search(options);
          movieSetsFound.clear();
          if (movieSets.isEmpty()) {
            movieSetsFound.add(MovieSetChooserModel.EMPTY_RESULT);
          }
          else {
            for (MediaSearchResult collection : movieSets) {
              MovieSetChooserModel model = new MovieSetChooserModel(collection);
              movieSetsFound.add(model);
            }
          }
        }

        if (!movieSetsFound.isEmpty()) {
          tableMovieSets.setRowSelectionInterval(0, 0); // select first row
          // adjust columns - dirty hack, but unless we refactor everything here, the column model is known from here on
          tableMovies.getColumnModel()
              .getColumn(1)
              .setMaxWidth((int) (tableMovies.getFontMetrics(tableMovies.getFont()).stringWidth("2000") * 1.5f + 10));
        }
      }
      catch (Exception e1) {
        LOGGER.warn("SearchTask", e1);
      }

      return null;
    }

    @Override
    public void done() {
      stopProgressBar();
    }
  }

  private class SearchAction extends AbstractAction {
    SearchAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.search"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movieset.search"));
      putValue(SMALL_ICON, IconManager.SEARCH_INV);
      putValue(LARGE_ICON_KEY, IconManager.SEARCH_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      searchMovieSet();
    }
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    if ("Cancel".equals(arg0.getActionCommand())) {
      // cancel
      setVisible(false);
    }

    if ("Save".equals(arg0.getActionCommand())) {
      // save it
      int row = tableMovieSets.getSelectedRow();
      if (row >= 0) {
        MovieSetChooserModel model = movieSetsFound.get(row);
        if (model != MovieSetChooserModel.EMPTY_RESULT) {
          // when scraping was not successful, abort saving
          if (!model.isScraped()) {
            MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "MovieSetChooser", "message.scrape.threadcrashed"));
            return;
          }

          MediaMetadata md = model.getMetadata();

          // set scraped metadata
          List<MovieSetScraperMetadataConfig> scraperConfig = cbScraperConfig.getSelectedItems();
          movieSetToScrape.setMetadata(md, scraperConfig);
          movieSetToScrape.setDummyMovies(model.getMovieSetMovies());

          // assign movies
          if (cbAssignMovies.isSelected()) {
            movieSetToScrape.removeAllMovies();
            for (int i = 0; i < model.getMovies().size(); i++) {
              MovieInSet movieInSet = model.getMovies().get(i);
              Movie movie = movieInSet.getMovie();
              if (movie == null) {
                continue;
              }

              // check if the found movie contains a matching set
              if (movie.getMovieSet() != null) {
                // unassign movie from set
                MovieSet mSet = movie.getMovieSet();
                mSet.removeMovie(movie, true);
              }

              movie.setMovieSet(movieSetToScrape);
              movie.writeNFO();
              movie.saveToDb();
              movieSetToScrape.insertMovie(movie);
            }

            // and finally save assignments
            movieSetToScrape.saveToDb();
          }

          // get images?
          if (ScraperMetadataConfig.containsAnyArtwork(scraperConfig)) {
            // let the user choose the images
            if (!MovieModuleManager.getInstance().getSettings().isScrapeBestImageMovieSet()) {
              if (scraperConfig.contains(MovieSetScraperMetadataConfig.POSTER)) {
                chooseArtwork(MediaFileType.POSTER);
              }
              if (scraperConfig.contains(MovieSetScraperMetadataConfig.FANART)) {
                chooseArtwork(MediaFileType.FANART);
              }
              if (scraperConfig.contains(MovieSetScraperMetadataConfig.BANNER)) {
                chooseArtwork(MediaFileType.BANNER);
              }
              if (scraperConfig.contains(MovieSetScraperMetadataConfig.LOGO)) {
                chooseArtwork(MediaFileType.LOGO);
              }
              if (scraperConfig.contains(MovieSetScraperMetadataConfig.CLEARLOGO)) {
                chooseArtwork(MediaFileType.CLEARLOGO);
              }
              if (scraperConfig.contains(MovieSetScraperMetadataConfig.CLEARART)) {
                chooseArtwork(MediaFileType.CLEARART);
              }
              if (scraperConfig.contains(MovieSetScraperMetadataConfig.DISCART)) {
                chooseArtwork(MediaFileType.DISC);
              }
              if (scraperConfig.contains(MovieSetScraperMetadataConfig.THUMB)) {
                chooseArtwork(MediaFileType.THUMB);
              }
              // write artwork urls to the NFO
              movieSetToScrape.writeNFO();
            }
            else {
              // get artwork asynchronous
              model.startArtworkScrapeTask(movieSetToScrape, scraperConfig);
            }
          }
        }
        setVisible(false);
      }
    }

    // Abort queue
    if ("Abort".equals(arg0.getActionCommand())) {
      continueQueue = false;
      setVisible(false);
    }
  }

  private void chooseArtwork(MediaFileType mediaFileType) {
    MediaArtwork.MediaArtworkType imageType;

    switch (mediaFileType) {
      case POSTER:
        if (MovieModuleManager.getInstance().getSettings().getMovieSetPosterFilenames().isEmpty()) {
          return;
        }
        imageType = POSTER;
        break;

      case FANART:
        if (MovieModuleManager.getInstance().getSettings().getMovieSetFanartFilenames().isEmpty()) {
          return;
        }
        imageType = BACKGROUND;
        break;

      case BANNER:
        if (MovieModuleManager.getInstance().getSettings().getMovieSetBannerFilenames().isEmpty()) {
          return;
        }
        imageType = BANNER;
        break;

      case CLEARLOGO:
      case LOGO:
        if (MovieModuleManager.getInstance().getSettings().getMovieSetClearlogoFilenames().isEmpty()) {
          return;
        }
        imageType = CLEARLOGO;
        break;

      case CLEARART:
        if (MovieModuleManager.getInstance().getSettings().getMovieSetClearartFilenames().isEmpty()) {
          return;
        }
        imageType = CLEARART;
        break;

      case DISC:
        if (MovieModuleManager.getInstance().getSettings().getMovieSetDiscartFilenames().isEmpty()) {
          return;
        }
        imageType = DISC;
        break;

      case THUMB:
        if (MovieModuleManager.getInstance().getSettings().getMovieSetThumbFilenames().isEmpty()) {
          return;
        }
        imageType = THUMB;
        break;

      default:
        return;
    }

    Map<String, Object> newIds = new HashMap<>(movieSetToScrape.getIds());
    String imageUrl = ImageChooserDialog.chooseImage(this, newIds, imageType,
        MovieModuleManager.getInstance().getMovieList().getDefaultArtworkScrapers(), MediaType.MOVIE_SET,
        MovieModuleManager.getInstance().getSettings().getMovieSetDataFolder());

    movieSetToScrape.setArtworkUrl(imageUrl, mediaFileType);
  }

  private class ScrapeTask extends SwingWorker<Void, Void> {
    private final MovieSetChooserModel model;

    ScrapeTask(MovieSetChooserModel model) {
      this.model = model;
    }

    @Override
    public Void doInBackground() {
      startProgressBar(TmmResourceBundle.getString("chooser.scrapeing") + " " + model.getName());

      // disable ok button as long as its scraping
      btnOk.setEnabled(false);
      model.scrapeMetadata();
      btnOk.setEnabled(true);

      return null;
    }

    @Override
    public void done() {
      stopProgressBar();
    }
  }

  private void startProgressBar(final String description) {
    SwingUtilities.invokeLater(() -> {
      lblProgressAction.setText(description);
      progressBar.setVisible(true);
      progressBar.setIndeterminate(true);
    });
  }

  private void stopProgressBar() {
    SwingUtilities.invokeLater(() -> {
      lblProgressAction.setText("");
      progressBar.setVisible(false);
      progressBar.setIndeterminate(false);
    });
  }

  /**
   * Shows the dialog and returns whether the work on the queue should be continued.
   * 
   * @return true, if successful
   */
  public boolean showDialog() {
    setVisible(true);
    return continueQueue;
  }

  protected BindingGroup initDataBindings() {
    JTableBinding jTableBinding = SwingBindings.createJTableBinding(UpdateStrategy.READ, movieSetsFound, tableMovieSets);
    //
    Property movieSetChooserModelBeanProperty = BeanProperty.create("name");
    jTableBinding.addColumnBinding(movieSetChooserModelBeanProperty).setEditable(false);
    //
    jTableBinding.bind();
    //
    Property jTableBeanProperty = BeanProperty.create("selectedElement.movies");
    JTableBinding jTableBinding_1 = SwingBindings.createJTableBinding(UpdateStrategy.READ, tableMovieSets, jTableBeanProperty, tableMovies);
    //
    Property movieInSetBeanProperty = BeanProperty.create("name");
    jTableBinding_1.addColumnBinding(movieInSetBeanProperty).setColumnName(TmmResourceBundle.getString("metatag.title")).setEditable(false);
    //
    Property movieInSetBeanProperty_1 = BeanProperty.create("year");
    jTableBinding_1.addColumnBinding(movieInSetBeanProperty_1).setColumnName(TmmResourceBundle.getString("metatag.year")).setEditable(false);
    //
    Property movieInSetBeanProperty_2 = BeanProperty.create("movie.title");
    jTableBinding_1.addColumnBinding(movieInSetBeanProperty_2)
        .setColumnName(TmmResourceBundle.getString("movieset.movie.matched"))
        .setEditable(false);
    //
    jTableBinding_1.bind();
    //
    Property jTableBeanProperty_1 = BeanProperty.create("selectedElement.name");
    Property jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, tableMovieSets, jTableBeanProperty_1, lblMovieSetName,
        jLabelBeanProperty);
    autoBinding.bind();
    //
    Property jTableBeanProperty_2 = BeanProperty.create("selectedElement.posterUrl");
    Property imageLabelBeanProperty = BeanProperty.create("imageUrl");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, tableMovieSets, jTableBeanProperty_2, lblMovieSetPoster,
        imageLabelBeanProperty);
    autoBinding_1.bind();
    //
    Property jTableBeanProperty_3 = BeanProperty.create("selectedElement.overview");
    Property readOnlyTextPaneBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ, tableMovieSets, jTableBeanProperty_3, tpPlot,
        readOnlyTextPaneBeanProperty);
    autoBinding_2.bind();
    //
    BindingGroup bindingGroup = new BindingGroup();
    //
    bindingGroup.addBinding(jTableBinding);
    bindingGroup.addBinding(jTableBinding_1);
    bindingGroup.addBinding(autoBinding);
    bindingGroup.addBinding(autoBinding_1);
    bindingGroup.addBinding(autoBinding_2);
    return bindingGroup;
  }
}
