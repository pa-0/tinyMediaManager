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
package org.tinymediamanager.ui.movies.dialogs;

import static java.util.Locale.ROOT;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BACKGROUND;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARLOGO;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.DISC;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.KEYART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTaskChain;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.thirdparty.trakttv.MovieSyncTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.EnhancedTextField;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.MediaScraperComboBox;
import org.tinymediamanager.ui.components.combobox.ScraperMetadataConfigCheckComboBox;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.ImageChooserDialog;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.movies.MovieChooserModel;
import org.tinymediamanager.ui.renderer.BorderTableCellRenderer;
import org.tinymediamanager.ui.renderer.IntegerTableCellRenderer;
import org.tinymediamanager.ui.renderer.RightAlignTableCellRenderer;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieChooser.
 *
 * @author Manuel Laggner
 */
public class MovieChooserDialog extends TmmDialog implements ActionListener {
  private static final Logger                                                  LOGGER                = LoggerFactory
      .getLogger(MovieChooserDialog.class);

  private final MovieList                                                      movieList             = MovieModuleManager.getInstance()
      .getMovieList();
  private final Movie                                                          movieToScrape;
  private final List<MediaScraper>                                             artworkScrapers;
  private final List<MediaScraper>                                             trailerScrapers;

  private MediaScraper                                                         mediaScraper;
  private SortedList<MovieChooserModel>                                        searchResultEventList = null;
  private EventList<Person>                                                    castMemberEventList   = null;
  private MovieChooserModel                                                    selectedResult        = null;

  private SearchTask                                                           activeSearchTask;

  private boolean                                                              continueQueue         = true;
  private boolean                                                              navigateBack          = false;

  /**
   * UI components
   */
  private final MediaScraperComboBox                                           cbScraper;
  private final TmmTable                                                       tableSearchResults;
  private final JLabel                                                         lblTitle;
  private final JTextArea                                                      taMovieDescription;
  private final ImageLabel                                                     lblMoviePoster;
  private final JLabel                                                         lblProgressAction;
  private final JLabel                                                         lblError;
  private final JProgressBar                                                   progressBar;
  private final JLabel                                                         lblTagline;
  private final JButton                                                        okButton;
  private final JLabel                                                         lblPath;
  private final JComboBox                                                      cbLanguage;
  private final JLabel                                                         lblOriginalTitle;
  private final TmmTable                                                       tableCastMembers;
  private final ScraperMetadataConfigCheckComboBox<MovieScraperMetadataConfig> cbScraperConfig;
  private final JHintCheckBox                                                  chckbxDoNotOverwrite;

  private JTextField                                                           textFieldSearchString;

  /**
   * Create the dialog.
   *
   * @param movie
   *          the movie
   * @param queueIndex
   *          the actual index in the queue
   * @param queueSize
   *          the queue size
   */
  public MovieChooserDialog(Movie movie, int queueIndex, int queueSize) {
    super(TmmResourceBundle.getString("moviechooser.search") + (queueSize > 1 ? " " + (queueIndex + 1) + "/" + queueSize : ""), "movieChooser");
    movieToScrape = movie;
    mediaScraper = movieList.getDefaultMediaScraper();
    artworkScrapers = movieList.getDefaultArtworkScrapers();
    trailerScrapers = movieList.getDefaultTrailerScrapers();

    // table format for the search result
    searchResultEventList = new SortedList<>(
        new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()), GlazedLists.beanConnector(MovieChooserModel.class)),
        new SearchResultScoreComparator());

    // table format for the castmembers
    castMemberEventList = GlazedListsSwing.swingThreadProxyList(
        GlazedLists.threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(Person.class))));

    {
      final JPanel panelPath = new JPanel();
      panelPath.setLayout(new MigLayout("", "[200lp:300lp,grow][]", "[]"));
      {
        lblPath = new JLabel("");
        TmmFontHelper.changeFont(lblPath, 1.16667, Font.BOLD);
        panelPath.add(lblPath, "cell 0 0, growx, wmin 0");
      }

      {
        final JButton btnPlay = new SquareIconButton(IconManager.PLAY_INV);
        btnPlay.setFocusable(false);
        btnPlay.addActionListener(e -> {
          MediaFile mf = movieToScrape.getMainVideoFile();
          try {
            TmmUIHelper.openFile(MediaFileHelper.getMainVideoFile(mf));
          }
          catch (Exception ex) {
            LOGGER.error("open file", ex);
            MessageManager.instance
                .pushMessage(new Message(MessageLevel.ERROR, mf, "message.erroropenfile", new String[] { ":", ex.getLocalizedMessage() }));
          }
        });
        panelPath.add(btnPlay, "cell 1 0");
      }
      setTopInformationPanel(panelPath);
    }

    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new MigLayout("insets 0 n n n", "[600lp:900lp,grow]", "[][shrink 0][250lp:350lp,grow][shrink 0][][][]"));
    getContentPane().add(contentPanel, BorderLayout.CENTER);

    {
      JPanel panelSearchField = new JPanel();
      contentPanel.add(panelSearchField, "cell 0 0,grow");
      panelSearchField.setLayout(new MigLayout("insets 0", "[][][grow][]", "[]2lp[]"));
      {
        JLabel lblScraper = new TmmLabel(TmmResourceBundle.getString("scraper"));
        panelSearchField.add(lblScraper, "cell 0 0,alignx right");
      }
      {
        cbScraper = new MediaScraperComboBox(movieList.getAvailableMediaScrapers());
        MediaScraper defaultScraper = movieList.getDefaultMediaScraper();
        if (defaultScraper != null && defaultScraper.isEnabled()) {
          cbScraper.setSelectedItem(defaultScraper);
        }
        cbScraper.setAction(new ChangeScraperAction());
        panelSearchField.add(cbScraper, "cell 1 0,growx");
      }
      {
        // also attach the actionlistener to the textfield to trigger the search on enter in the textfield
        ActionListener searchAction = arg0 -> searchMovie(textFieldSearchString.getText(), false);

        textFieldSearchString = new EnhancedTextField(TmmResourceBundle.getString("moviechooser.search.hint"));
        textFieldSearchString.setToolTipText(TmmResourceBundle.getString("moviechooser.search.hint"));
        textFieldSearchString.addActionListener(searchAction);
        panelSearchField.add(textFieldSearchString, "cell 2 0,growx");
        textFieldSearchString.setColumns(10);

        JButton btnSearch = new JButton(TmmResourceBundle.getString("Button.search"));
        panelSearchField.add(btnSearch, "cell 3 0");
        btnSearch.setIcon(IconManager.SEARCH_INV);
        btnSearch.addActionListener(searchAction);
      }
      {
        JLabel lblLanguage = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
        panelSearchField.add(lblLanguage, "cell 0 1,alignx right");
        cbLanguage = new JComboBox(MediaLanguages.valuesSorted());
        cbLanguage.setSelectedItem(MovieModuleManager.getInstance().getSettings().getScraperLanguage());
        cbLanguage.addActionListener(e -> searchMovie(textFieldSearchString.getText(), false));
        panelSearchField.add(cbLanguage, "cell 1 1");
      }
    }
    {
      contentPanel.add(new JSeparator(), "cell 0 1,growx");
    }
    {
      JSplitPane splitPane = new JSplitPane();
      splitPane.setName(getName() + ".splitPane");
      TmmUILayoutStore.getInstance().install(splitPane);
      contentPanel.add(splitPane, "cell 0 2,grow");
      {
        JPanel panelSearchResults = new JPanel();
        splitPane.setLeftComponent(panelSearchResults);
        panelSearchResults.setLayout(new MigLayout("insets 0", "[200lp:300lp,grow]", "[150lp:300lp,grow]"));
        {
          JScrollPane scrollPane = new JScrollPane();
          panelSearchResults.add(scrollPane, "cell 0 0,grow");
          tableSearchResults = new TmmTable(new TmmTableModel<>(searchResultEventList, new SearchResultTableFormat()));
          tableSearchResults.setName("moviechooser.searchResults");
          TmmUILayoutStore.getInstance().install(tableSearchResults);
          tableSearchResults.configureScrollPane(scrollPane);
        }
      }
      {
        JPanel panelSearchDetail = new JPanel();
        splitPane.setRightComponent(panelSearchDetail);
        panelSearchDetail.setLayout(new MigLayout("", "[100lp:15%:20%,grow][300lp:500lp,grow 3]", "[]2lp[]2lp[][150lp:25%:50%][50lp:100lp,grow]"));
        {
          lblTitle = new JLabel("");
          TmmFontHelper.changeFont(lblTitle, 1.167, Font.BOLD);
          panelSearchDetail.add(lblTitle, "cell 1 0, wmin 0");
        }
        {
          lblOriginalTitle = new JLabel("");
          panelSearchDetail.add(lblOriginalTitle, "cell 1 1,wmin 0");
        }
        {
          lblTagline = new JLabel("");
          panelSearchDetail.add(lblTagline, "cell 1 2, wmin 0");
        }
        {
          lblMoviePoster = new ImageLabel(false);
          lblMoviePoster.setDesiredAspectRatio(2 / 3f);
          panelSearchDetail.add(lblMoviePoster, "cell 0 0 1 4,grow");
        }
        {
          JScrollPane scrollPane = new NoBorderScrollPane();
          panelSearchDetail.add(scrollPane, "cell 1 3,grow");
          {
            taMovieDescription = new ReadOnlyTextArea();
            scrollPane.setViewportView(taMovieDescription);
          }
        }
        {
          JScrollPane scrollPane = new JScrollPane();
          panelSearchDetail.add(scrollPane, "cell 0 4 2 1,grow");
          {
            tableCastMembers = new TmmTable(new TmmTableModel<>(castMemberEventList, new CastMemberTableFormat()));
            tableCastMembers.configureScrollPane(scrollPane);
          }
        }
      }
    }
    {
      JSeparator separator = new JSeparator();
      contentPanel.add(separator, "cell 0 3,growx");
    }
    {
      JLabel lblScrapeFollowingItems = new TmmLabel(TmmResourceBundle.getString("chooser.scrape"));
      contentPanel.add(lblScrapeFollowingItems, "cell 0 4,growx");

      cbScraperConfig = new ScraperMetadataConfigCheckComboBox(MovieScraperMetadataConfig.getValuesWithout(MovieScraperMetadataConfig.ID));
      cbScraperConfig.enableFilter(
          (movieScraperMetadataConfig, s) -> movieScraperMetadataConfig.getDescription().toLowerCase(ROOT).startsWith(s.toLowerCase(ROOT)));
      contentPanel.add(cbScraperConfig, "cell 0 5,grow, wmin 0");
    }
    {
      chckbxDoNotOverwrite = new JHintCheckBox(TmmResourceBundle.getString("message.scrape.donotoverwrite"));
      chckbxDoNotOverwrite.setToolTipText(TmmResourceBundle.getString("message.scrape.donotoverwrite.desc"));
      chckbxDoNotOverwrite.setHintIcon(IconManager.HINT);
      contentPanel.add(chckbxDoNotOverwrite, "cell 0 6");
    }
    {
      {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new MigLayout("hidemode 3", "[][grow]", "[]"));

        progressBar = new JProgressBar();
        infoPanel.add(progressBar, "cell 0 0");

        lblProgressAction = new JLabel("");
        infoPanel.add(lblProgressAction, "cell 1 0");

        lblError = new JLabel("");
        TmmFontHelper.changeFont(lblError, Font.BOLD);
        lblError.setForeground(Color.RED);
        infoPanel.add(lblError, "cell 1 0");

        setBottomInformationPanel(infoPanel);
      }
      {
        if (queueSize > 1) {
          JButton abortButton = new JButton(TmmResourceBundle.getString("Button.abortqueue"));
          abortButton.setIcon(IconManager.STOP_INV);
          abortButton.setActionCommand("Abort");
          abortButton.addActionListener(this);
          addButton(abortButton);

          if (queueIndex > 0) {
            JButton backButton = new JButton(TmmResourceBundle.getString("Button.back"));
            backButton.setIcon(IconManager.BACK_INV);
            backButton.setActionCommand("Back");
            backButton.addActionListener(this);
            addButton(backButton);
          }
        }

        JButton cancelButton = new JButton(TmmResourceBundle.getString("Button.cancel"));
        cancelButton.setIcon(IconManager.CANCEL_INV);
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);
        addButton(cancelButton);

        okButton = new JButton(TmmResourceBundle.getString("Button.ok"));
        okButton.setIcon(IconManager.APPLY_INV);
        okButton.setActionCommand("OK");
        okButton.addActionListener(this);
        addButton(okButton);
      }
    }

    // install and save the comparator on the Table
    TableComparatorChooser.install(tableSearchResults, searchResultEventList, TableComparatorChooser.SINGLE_COLUMN);

    // double click to take the result
    tableSearchResults.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() >= 2 && !e.isConsumed() && e.getButton() == MouseEvent.BUTTON1 && okButton.isEnabled()) {
          actionPerformed(new ActionEvent(okButton, ActionEvent.ACTION_PERFORMED, "OK"));
        }
      }
    });

    // add a change listener for the async loaded meta data
    PropertyChangeListener listener = evt -> {
      String property = evt.getPropertyName();
      if ("scraped".equals(property)) {
        castMemberEventList.clear();
        int row = tableSearchResults.convertRowIndexToModel(tableSearchResults.getSelectedRow());
        if (row > -1) {
          MovieChooserModel model = searchResultEventList.get(row);
          castMemberEventList.addAll(model.getCastMembers());
          lblOriginalTitle.setText(model.getOriginalTitle());
          lblTagline.setText(model.getTagline());
          if (!model.getPosterUrl().equals(lblMoviePoster.getImageUrl())) {
            lblMoviePoster.setImageUrl(model.getPosterUrl());
          }
          taMovieDescription.setText(model.getOverview());
        }
      }
    };

    tableSearchResults.getSelectionModel().addListSelectionListener(e -> {
      if (e.getValueIsAdjusting()) {
        return;
      }

      int index = tableSearchResults.convertRowIndexToModel(tableSearchResults.getSelectedRow());
      castMemberEventList.clear();
      if (selectedResult != null) {
        selectedResult.removePropertyChangeListener(listener);
      }
      if (index > -1 && index < searchResultEventList.size()) {
        MovieChooserModel model = searchResultEventList.get(index);
        castMemberEventList.addAll(model.getCastMembers());
        lblMoviePoster.setImageUrl(model.getPosterUrl());
        lblTitle.setText(model.getCombinedName());
        lblOriginalTitle.setText(model.getOriginalTitle());
        lblTagline.setText(model.getTagline());
        taMovieDescription.setText(model.getOverview());

        selectedResult = model;
        selectedResult.addPropertyChangeListener(listener);
      }
      else {
        selectedResult = null;
      }

      ListSelectionModel lsm = (ListSelectionModel) e.getSource();
      if (!lsm.isSelectionEmpty()) {
        int selectedRow = lsm.getMinSelectionIndex();
        selectedRow = tableSearchResults.convertRowIndexToModel(selectedRow);
        try {
          MovieChooserModel model = searchResultEventList.get(selectedRow);
          if (model != MovieChooserModel.emptyResult && !model.isScraped()) {
            ScrapeTask task = new ScrapeTask(model);
            task.execute();
          }
        }
        catch (Exception ex) {
          LOGGER.warn(ex.getMessage());
        }
      }
    });

    {
      progressBar.setVisible(false);
      cbScraperConfig.setSelectedItems(MovieModuleManager.getInstance().getSettings().getScraperMetadataConfig());
      chckbxDoNotOverwrite.setSelected(MovieModuleManager.getInstance().getSettings().isDoNotOverwriteExistingData());

      textFieldSearchString.setText(movieToScrape.getTitle());
      lblPath.setText(movieToScrape.getPathNIO().resolve(movieToScrape.getMainFile().getFilename()).toString());
      // initial search with IDs
      searchMovie(textFieldSearchString.getText(), true);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if ("OK".equals(e.getActionCommand())) {
      int row = tableSearchResults.getSelectedRow();
      if (row >= 0) {
        MovieChooserModel model = searchResultEventList.get(row);
        if (model != MovieChooserModel.emptyResult) {
          // when scraping was not successful, abort saving
          if (!model.isScraped()) {
            MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "MovieChooser", "message.scrape.threadcrashed"));
            return;
          }

          MediaMetadata md = model.getMetadata();

          // check if there is at leat a title in the metadata -> otherwise take the title from the search result
          if (StringUtils.isBlank(md.getTitle())) {
            md.setTitle(model.getTitle());
          }

          // did the user want to choose the images?
          if (!MovieModuleManager.getInstance().getSettings().isScrapeBestImage()) {
            md.clearMediaArt();
          }

          // set scraped metadata
          List<MovieScraperMetadataConfig> scraperConfig = cbScraperConfig.getSelectedItems();
          boolean overwrite = !chckbxDoNotOverwrite.isSelected();

          // get other ratings too?
          if (MovieModuleManager.getInstance().getSettings().isFetchAllRatings()) {
            for (MediaRating rating : ListUtils.nullSafe(RatingProvider.getRatings(md.getIds(), MediaType.MOVIE))) {
              if (!md.getRatings().contains(rating)) {
                md.addRating(rating);
              }
            }
          }

          movieToScrape.setMetadata(md, scraperConfig, overwrite);
          movieToScrape.setLastScraperId(model.getMetadataProvider().getId());
          movieToScrape.setLastScrapeLanguage(model.getLanguage().name());

          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

          // get images?
          if (ScraperMetadataConfig.containsAnyArtwork(scraperConfig)) {
            // let the user choose the images
            if (!MovieModuleManager.getInstance().getSettings().isScrapeBestImage()) {
              if (scraperConfig.contains(MovieScraperMetadataConfig.POSTER)
                  && (overwrite || StringUtils.isBlank(movieToScrape.getArtworkFilename(MediaFileType.POSTER)))) {
                chooseArtwork(MediaFileType.POSTER);
              }
              if ((scraperConfig.contains(MovieScraperMetadataConfig.FANART) || scraperConfig.contains(MovieScraperMetadataConfig.EXTRAFANART))
                  && (overwrite || StringUtils.isBlank(movieToScrape.getArtworkFilename(MediaFileType.FANART)))) {
                chooseArtwork(MediaFileType.FANART);
              }
              if (scraperConfig.contains(MovieScraperMetadataConfig.BANNER)
                  && (overwrite || StringUtils.isBlank(movieToScrape.getArtworkFilename(MediaFileType.BANNER)))) {
                chooseArtwork(MediaFileType.BANNER);
              }
              if (scraperConfig.contains(MovieScraperMetadataConfig.CLEARLOGO)
                  && (overwrite || StringUtils.isBlank(movieToScrape.getArtworkFilename(MediaFileType.CLEARLOGO)))) {
                chooseArtwork(MediaFileType.CLEARLOGO);
              }
              if (scraperConfig.contains(MovieScraperMetadataConfig.CLEARART)
                  && (overwrite || StringUtils.isBlank(movieToScrape.getArtworkFilename(MediaFileType.CLEARART)))) {
                chooseArtwork(MediaFileType.CLEARART);
              }
              if (scraperConfig.contains(MovieScraperMetadataConfig.DISCART)
                  && (overwrite || StringUtils.isBlank(movieToScrape.getArtworkFilename(MediaFileType.DISC)))) {
                chooseArtwork(MediaFileType.DISC);
              }
              if ((scraperConfig.contains(MovieScraperMetadataConfig.THUMB) || scraperConfig.contains(MovieScraperMetadataConfig.EXTRATHUMB))
                  && (overwrite || StringUtils.isBlank(movieToScrape.getArtworkFilename(MediaFileType.THUMB)))) {
                chooseArtwork(MediaFileType.THUMB);
              }
              if (scraperConfig.contains(MovieScraperMetadataConfig.KEYART)
                  && (overwrite || StringUtils.isBlank(movieToScrape.getArtworkFilename(MediaFileType.KEYART)))) {
                chooseArtwork(MediaFileType.KEYART);
              }

              movieToScrape.saveToDb();
              movieToScrape.writeNFO(); // rewrite NFO to get the urls into the NFO
            }
            else {
              // get artwork asynchronous
              model.startArtworkScrapeTask(scraperConfig, overwrite);
            }
          }

          // get trailers?
          if (scraperConfig.contains(MovieScraperMetadataConfig.TRAILER)) {
            model.startTrailerScrapeTask(overwrite);
          }

          // if configured - sync with trakt.tv
          if (MovieModuleManager.getInstance().getSettings().getSyncTrakt()) {
            MovieSyncTraktTvTask task = new MovieSyncTraktTvTask(Collections.singletonList(movieToScrape));
            task.setSyncCollection(MovieModuleManager.getInstance().getSettings().getSyncTraktCollection());
            task.setSyncWatched(MovieModuleManager.getInstance().getSettings().getSyncTraktWatched());
            task.setSyncRating(MovieModuleManager.getInstance().getSettings().getSyncTraktRating());

            TmmTaskChain.getInstance(movieToScrape).add(task);
          }

          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

          setVisible(false);
        }
      }
    }

    // cancel
    if ("Cancel".equals(e.getActionCommand())) {
      setVisible(false);
    }

    // Abort queue
    if ("Abort".equals(e.getActionCommand())) {
      continueQueue = false;
      setVisible(false);
    }

    // navigate back
    if ("Back".equals(e.getActionCommand())) {
      navigateBack = true;
      setVisible(false);
    }
  }

  private void chooseArtwork(MediaFileType mediaFileType) {
    MediaArtwork.MediaArtworkType imageType;
    List<String> extrathumbs = null;
    List<String> extrafanarts = null;

    switch (mediaFileType) {
      case POSTER:
        if (MovieModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty()) {
          return;
        }
        imageType = POSTER;
        break;

      case FANART:
        if (MovieModuleManager.getInstance().getSettings().getFanartFilenames().isEmpty()) {
          return;
        }
        imageType = BACKGROUND;
        if (MovieModuleManager.getInstance().getSettings().isImageExtraThumbs()) {
          extrathumbs = new ArrayList<>();
        }
        if (MovieModuleManager.getInstance().getSettings().isImageExtraFanart()) {
          extrafanarts = new ArrayList<>();
        }
        break;

      case BANNER:
        if (MovieModuleManager.getInstance().getSettings().getBannerFilenames().isEmpty()) {
          return;
        }
        imageType = BANNER;
        break;

      case CLEARLOGO:
        if (MovieModuleManager.getInstance().getSettings().getClearlogoFilenames().isEmpty()) {
          return;
        }
        imageType = CLEARLOGO;
        break;

      case CLEARART:
        if (MovieModuleManager.getInstance().getSettings().getClearartFilenames().isEmpty()) {
          return;
        }
        imageType = CLEARART;
        break;

      case DISC:
        if (MovieModuleManager.getInstance().getSettings().getDiscartFilenames().isEmpty()) {
          return;
        }
        imageType = DISC;
        break;

      case THUMB:
        if (MovieModuleManager.getInstance().getSettings().getThumbFilenames().isEmpty()) {
          return;
        }
        imageType = THUMB;
        break;

      case KEYART:
        if (MovieModuleManager.getInstance().getSettings().getKeyartFilenames().isEmpty()) {
          return;
        }
        imageType = KEYART;
        break;

      default:
        return;
    }

    Map<String, Object> newIds = new HashMap<>(movieToScrape.getIds());
    if (movieToScrape.isStacked()) {
      ArrayList<MediaFile> mfs = new ArrayList<>();
      mfs.addAll(movieToScrape.getMediaFiles(MediaFileType.VIDEO));
      newIds.put("mediaFile", mfs);
    }
    else {
      newIds.put("mediaFile", movieToScrape.getMainFile());
    }

    String imageUrl = ImageChooserDialog.chooseImage(this, newIds, imageType, artworkScrapers, extrathumbs, extrafanarts, MediaType.MOVIE,
        movieToScrape.getPathNIO().toAbsolutePath().toString());

    movieToScrape.setArtworkUrl(imageUrl, mediaFileType);
    if (StringUtils.isNotBlank(imageUrl)) {
      movieToScrape.downloadArtwork(mediaFileType);
    }

    // set extrathumbs and extrafanarts
    if (extrathumbs != null) {
      movieToScrape.setExtraThumbs(extrathumbs);
      if (!extrathumbs.isEmpty()) {
        movieToScrape.downloadArtwork(MediaFileType.EXTRATHUMB);
      }
    }

    if (extrafanarts != null) {
      movieToScrape.setExtraFanarts(extrafanarts);
      if (!extrafanarts.isEmpty()) {
        movieToScrape.downloadArtwork(MediaFileType.EXTRAFANART);
      }
    }
  }

  private void searchMovie(String searchTerm, boolean withIds) {
    if (activeSearchTask != null && !activeSearchTask.isDone()) {
      activeSearchTask.cancel();
    }
    activeSearchTask = new SearchTask(searchTerm, movieToScrape, withIds);
    SwingUtilities.invokeLater(activeSearchTask::execute);
  }

  private void startProgressBar(final String description) {
    SwingUtilities.invokeLater(() -> {
      lblProgressAction.setText(description);
      progressBar.setVisible(true);
      progressBar.setIndeterminate(true);
      lblError.setText("");
    });
  }

  private void stopProgressBar() {
    SwingUtilities.invokeLater(() -> {
      lblProgressAction.setText("");
      progressBar.setVisible(false);
      progressBar.setIndeterminate(false);
    });
  }

  @Override
  public void dispose() {
    if (activeSearchTask != null && !activeSearchTask.isDone()) {
      activeSearchTask.cancel();
    }
    super.dispose();
  }

  public boolean isContinueQueue() {
    return continueQueue;
  }

  public boolean isNavigateBack() {
    return navigateBack;
  }

  /******************************************************************************
   * helper classes
   ******************************************************************************/
  private class ChangeScraperAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
      mediaScraper = (MediaScraper) cbScraper.getSelectedItem();
      searchMovie(textFieldSearchString.getText(), false);
    }
  }

  private class SearchTask extends SwingWorker<Void, Void> {
    private final String            searchTerm;
    private final Movie             movie;
    private final boolean           withIds;
    private final MediaLanguages    language;

    private List<MediaSearchResult> searchResult;
    private Throwable               error  = null;
    boolean                         cancel = false;

    private SearchTask(String searchTerm, Movie movie, boolean withIds) {
      this.searchTerm = searchTerm;
      this.movie = movie;
      this.withIds = withIds;
      this.language = (MediaLanguages) cbLanguage.getSelectedItem();
    }

    @Override
    public Void doInBackground() {
      startProgressBar(TmmResourceBundle.getString("chooser.searchingfor") + " " + searchTerm);
      try {
        searchResult = movieList.searchMovie(searchTerm, movie.getYear(), withIds ? movie.getIds() : null, mediaScraper, language);
      }
      catch (Exception e) {
        error = e;
      }
      return null;
    }

    public void cancel() {
      cancel = true;
      super.cancel(true);
    }

    @Override
    public void done() {
      stopProgressBar();
      searchResultEventList.clear();

      if (error != null) {
        // display empty result
        searchResultEventList.add(MovieChooserModel.emptyResult);
        SwingUtilities.invokeLater(() -> lblError.setText(error.getMessage()));
      }
      else if (!cancel) {
        if (searchResult == null || searchResult.isEmpty()) {
          // display empty result
          searchResultEventList.add(MovieChooserModel.emptyResult);
        }
        else {
          MediaScraper mpFromResult = null;
          for (MediaSearchResult result : searchResult) {
            if (mpFromResult == null) {
              mpFromResult = movieList.getMediaScraperById(result.getProviderId());
            }
            if (mpFromResult == null) {
              // still null? maybe we have a Kodi scraper here where the getProdiverId comes from the sub-scraper; take the scraper from the dropdown
              mpFromResult = (MediaScraper) cbScraper.getSelectedItem();
            }
            searchResultEventList.add(new MovieChooserModel(movieToScrape, mpFromResult, artworkScrapers, trailerScrapers, result, language));
            // get metadataProvider from searchresult
          }
        }
      }

      if (!searchResultEventList.isEmpty()) { // only one result
        tableSearchResults.setRowSelectionInterval(0, 0); // select first row
      }
    }
  }

  private class ScrapeTask extends SwingWorker<Void, Void> {
    private final MovieChooserModel model;

    private ScrapeTask(MovieChooserModel model) {
      this.model = model;
    }

    @Override
    public Void doInBackground() {
      startProgressBar(TmmResourceBundle.getString("chooser.scrapeing") + " " + model.getTitle());

      // disable button as long as its scraping
      okButton.setEnabled(false);
      model.scrapeMetaData();
      okButton.setEnabled(true);
      return null;
    }

    @Override
    public void done() {
      stopProgressBar();
    }
  }

  /**
   * inner class for representing the result table
   */
  private static class SearchResultTableFormat extends TmmTableFormat<MovieChooserModel> {
    private SearchResultTableFormat() {
      Comparator<MovieChooserModel> searchResultComparator = new SearchResultTitleComparator();
      Comparator<String> stringComparator = new StringComparator();
      Comparator<Integer> integerComparator = new IntegerComparator();

      FontMetrics fontMetrics = getFontMetrics();

      /*
       * title
       */
      Column col = new Column(TmmResourceBundle.getString("chooser.searchresult"), "title", result -> result, MovieChooserModel.class);
      col.setCellTooltip(MovieChooserModel::getTitle);
      col.setColumnComparator(searchResultComparator);
      col.setCellRenderer(new SearchResultRenderer());
      addColumn(col);

      /*
       * year
       */
      col = new Column(TmmResourceBundle.getString("metatag.year"), "year", MovieChooserModel::getYear, Integer.class);
      col.setColumnComparator(integerComparator);
      col.setCellRenderer(new IntegerTableCellRenderer());
      col.setColumnResizeable(false);
      col.setMinWidth(fontMetrics.stringWidth("2000") + getCellPadding());
      col.setMaxWidth(fontMetrics.stringWidth("2000") + getCellPadding());
      addColumn(col);

      /*
       * id
       */
      col = new Column(TmmResourceBundle.getString("metatag.id"), "id", MovieChooserModel::getId, String.class);
      col.setColumnComparator(stringComparator);
      col.setColumnResizeable(false);
      col.setCellRenderer(new RightAlignTableCellRenderer());
      col.setMinWidth(fontMetrics.stringWidth("tt7830912") + getCellPadding());
      col.setMaxWidth(fontMetrics.stringWidth("tt7830912") + getCellPadding());
      addColumn(col);
    }
  }

  /**
   * inner class for sorting the search results by score (descending)
   */
  private static class SearchResultScoreComparator implements Comparator<MovieChooserModel> {
    @Override
    public int compare(MovieChooserModel o1, MovieChooserModel o2) {
      return Float.compare(o2.getScore(), o1.getScore());
    }
  }

  /**
   * inner class for sorting the search results by name
   */
  private static class SearchResultTitleComparator implements Comparator<MovieChooserModel> {
    private Collator stringCollator;

    private SearchResultTitleComparator() {
      RuleBasedCollator defaultCollator = (RuleBasedCollator) RuleBasedCollator.getInstance();
      try {
        // default collator ignores whitespaces
        // using hack from http://stackoverflow.com/questions/16567287/java-collation-ignores-space
        stringCollator = new RuleBasedCollator(defaultCollator.getRules().replace("<'\u005f'", "<' '<'\u005f'"));
      }
      catch (Exception e) {
        stringCollator = defaultCollator;
      }
    }

    @Override
    public int compare(MovieChooserModel o1, MovieChooserModel o2) {
      if (stringCollator != null) {
        String titleMovie1 = StrgUtils.normalizeString(o1.getTitle().toLowerCase(ROOT));
        String titleMovie2 = StrgUtils.normalizeString(o2.getTitle().toLowerCase(ROOT));
        return stringCollator.compare(titleMovie1, titleMovie2);
      }
      return o1.getTitle().toLowerCase(ROOT).compareTo(o2.getTitle().toLowerCase(ROOT));
    }
  }

  /**
   * inner class for representing the cast table
   */
  private static class CastMemberTableFormat extends TmmTableFormat<Person> {
    public CastMemberTableFormat() {
      /*
       * name
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.name"), "name", Person::getName, String.class);
      col.setCellTooltip(Person::getName);
      addColumn(col);

      /*
       * role
       */
      col = new Column(TmmResourceBundle.getString("metatag.role"), "role", Person::getRole, String.class);
      col.setCellTooltip(Person::getRole);
      addColumn(col);
    }
  }

  /**
   * inner class to render the search result
   */
  public static class SearchResultRenderer extends BorderTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      if (value instanceof MovieChooserModel result) {
        String text = result.getTitle();

        if (result.isDuplicate()) {
          setHorizontalTextPosition(SwingConstants.LEADING);
          setIconTextGap(10);
          setIcon(IconManager.WARN);
          setToolTipText(TmmResourceBundle.getString("moviechooser.duplicate.desc"));
        }
        else {
          setIcon(null);
        }

        return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
      }
      return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
  }
}
