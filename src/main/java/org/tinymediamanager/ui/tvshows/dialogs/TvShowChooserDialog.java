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
package org.tinymediamanager.ui.tvshows.dialogs;

import static ca.odell.glazedlists.gui.AbstractTableComparatorChooser.SINGLE_COLUMN;
import static java.util.Locale.ROOT;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BACKGROUND;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CHARACTERART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARLOGO;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.KEYART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
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
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.threading.TmmTaskChain;
import org.tinymediamanager.core.tvshow.TvShowArtworkHelper;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.core.tvshow.tasks.TvShowEpisodeScrapeTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowRenameTask;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.thirdparty.trakttv.TvShowSyncTraktTvTask;
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
import org.tinymediamanager.ui.renderer.BorderTableCellRenderer;
import org.tinymediamanager.ui.renderer.IntegerTableCellRenderer;
import org.tinymediamanager.ui.renderer.RightAlignTableCellRenderer;
import org.tinymediamanager.ui.tvshows.TvShowChooserModel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowChooserDialog.
 *
 * @author Manuel Laggner
 */
public class TvShowChooserDialog extends TmmDialog implements ActionListener {
  private static final Logger                                                          LOGGER                = LoggerFactory
      .getLogger(TvShowChooserDialog.class);

  private final TvShowList                                                             tvShowList            = TvShowModuleManager.getInstance()
      .getTvShowList();
  private final List<MediaScraper>                                                     artworkScrapers;
  private final List<MediaScraper>                                                     trailerScrapers;

  private TvShow                                                                       tvShowToScrape;
  private SortedList<TvShowChooserModel>                                               searchResultEventList = null;
  private TvShowChooserModel                                                           selectedResult        = null;
  private MediaScraper                                                                 mediaScraper;
  private boolean                                                                      continueQueue         = true;
  private boolean                                                                      navigateBack          = false;

  private SearchTask                                                                   activeSearchTask;

  /**
   * UI components
   */

  private final MediaScraperComboBox                                                   cbScraper;
  private final JComboBox<MediaLanguages>                                              cbLanguage;
  private final TmmTable                                                               tableSearchResults;
  private final JLabel                                                                 lblTtitle;
  private final JTextArea                                                              taOverview;
  private final ImageLabel                                                             lblTvShowPoster;
  private final JLabel                                                                 lblProgressAction;
  private final JLabel                                                                 lblError;
  private final JProgressBar                                                           progressBar;
  private final JButton                                                                okButton;
  private final JLabel                                                                 lblPath;
  private final JLabel                                                                 lblOriginalTitle;
  private final JComboBox<MediaEpisodeGroup>                                           cbEpisodeGroup;
  private final JLabel                                                                 lblEpisodeGroup;
  private final JButton                                                                btnCompareEpisodeGroup;
  private final ScraperMetadataConfigCheckComboBox<TvShowScraperMetadataConfig>        cbTvShowScraperConfig;
  private final ScraperMetadataConfigCheckComboBox<TvShowEpisodeScraperMetadataConfig> cbEpisodeScraperConfig;
  private final JHintCheckBox                                                          chckbxDoNotOverwrite;

  private JTextField                                                                   textFieldSearchString;

  /**
   * Instantiates a new tv show chooser dialog.
   *
   * @param tvShow
   *          the tv show
   * @param queueIndex
   *          the actual index in the queue
   * @param queueSize
   *          the queue size
   */
  public TvShowChooserDialog(TvShow tvShow, int queueIndex, int queueSize) {
    super(TmmResourceBundle.getString("tvshowchooser.search") + (queueSize > 1 ? " " + (queueIndex + 1) + "/" + queueSize : ""), "tvShowChooser");

    mediaScraper = tvShowList.getDefaultMediaScraper();
    artworkScrapers = tvShowList.getDefaultArtworkScrapers();
    trailerScrapers = tvShowList.getDefaultTrailerScrapers();

    // tableSearchResults format for the search result
    searchResultEventList = new SortedList<>(
        new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()), GlazedLists.beanConnector(TvShowChooserModel.class)),
        new SearchResultScoreComparator());

    TmmTableModel<TvShowChooserModel> searchResultTableModel = new TmmTableModel<>(searchResultEventList, new SearchResultTableFormat());

    {
      final JPanel panelPath = new JPanel();
      panelPath.setLayout(new MigLayout("", "[grow][]", "[]"));
      {
        lblPath = new JLabel("");
        TmmFontHelper.changeFont(lblPath, 1.16667, Font.BOLD);
        panelPath.add(lblPath, "cell 0 0, growx, wmin 0");
      }

      {
        final JButton btnPlay = new SquareIconButton(IconManager.FILE_OPEN_INV);
        btnPlay.setFocusable(false);
        btnPlay.addActionListener(e -> {
          TmmUIHelper.openFolder(tvShowToScrape.getPathNIO());
        });
        panelPath.add(btnPlay, "cell 1 0");
      }
      setTopInformationPanel(panelPath);
    }

    /* UI components */
    JPanel contentPanel = new JPanel();
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new MigLayout("insets 0 n n n", "[600lp:900lp,grow]", "[][shrink 0][250lp:300lp,grow][shrink 0][]"));
    {
      JPanel panelSearchField = new JPanel();
      contentPanel.add(panelSearchField, "cell 0 0,grow");
      panelSearchField.setLayout(new MigLayout("", "[][][grow][]", "[23px][]"));
      {
        JLabel lblScraper = new TmmLabel(TmmResourceBundle.getString("scraper"));
        panelSearchField.add(lblScraper, "cell 0 0,alignx right");
      }
      {
        cbScraper = new MediaScraperComboBox(tvShowList.getAvailableMediaScrapers());
        MediaScraper defaultScraper = tvShowList.getDefaultMediaScraper();
        if (defaultScraper != null && defaultScraper.isEnabled()) {
          cbScraper.setSelectedItem(defaultScraper);
        }
        cbScraper.setAction(new ChangeScraperAction());
        panelSearchField.add(cbScraper, "cell 1 0,growx");
      }
      {
        // also attach the actionlistener to the textfield to trigger the search on enter in the textfield
        ActionListener searchAction = arg0 -> searchTvShow(textFieldSearchString.getText(), false);

        textFieldSearchString = new EnhancedTextField(TmmResourceBundle.getString("tvshowchooser.search.hint"));
        textFieldSearchString.setToolTipText(TmmResourceBundle.getString("tvshowchooser.search.hint"));
        textFieldSearchString.addActionListener(searchAction);
        panelSearchField.add(textFieldSearchString, "cell 2 0,growx");
        textFieldSearchString.setColumns(10);

        JButton btnSearch = new JButton(TmmResourceBundle.getString("Button.search"));
        btnSearch.setIcon(IconManager.SEARCH_INV);
        panelSearchField.add(btnSearch, "cell 3 0");
        btnSearch.addActionListener(searchAction);
        getRootPane().setDefaultButton(btnSearch);
      }
      {
        JLabel lblLanguage = new TmmLabel("Language");
        panelSearchField.add(lblLanguage, "cell 0 1,alignx right");
      }
      {
        cbLanguage = new JComboBox<>();
        cbLanguage.setModel(new DefaultComboBoxModel<>(MediaLanguages.valuesSorted()));
        cbLanguage.setSelectedItem(TvShowModuleManager.getInstance().getSettings().getScraperLanguage());
        cbLanguage.addActionListener(e -> searchTvShow(textFieldSearchString.getText(), false));
        panelSearchField.add(cbLanguage, "cell 1 1,growx");
      }
    }
    {
      JSeparator separator = new JSeparator();
      contentPanel.add(separator, "cell 0 1,growx");
    }
    {
      JSplitPane splitPane = new JSplitPane();
      splitPane.setName(getName() + ".splitPane");
      TmmUILayoutStore.getInstance().install(splitPane);
      contentPanel.add(splitPane, "cell 0 2,grow");
      {
        JPanel panelSearchResults = new JPanel();
        splitPane.setLeftComponent(panelSearchResults);
        panelSearchResults.setLayout(new MigLayout("", "[200lp:300lp,grow]", "[150lp:300lp,grow]"));
        {
          JScrollPane scrollPane = new JScrollPane();
          panelSearchResults.add(scrollPane, "cell 0 0,grow");
          tableSearchResults = new TmmTable(searchResultTableModel);
          tableSearchResults.setName("tvshowchooser.searchResults");
          TmmUILayoutStore.getInstance().install(tableSearchResults);
          tableSearchResults.configureScrollPane(scrollPane);
        }
      }
      {
        JPanel panelSearchDetail = new JPanel();
        splitPane.setRightComponent(panelSearchDetail);
        panelSearchDetail.setLayout(new MigLayout("", "[150lp:15%:25%,grow][300lp:500lp,grow]", "[][][150lp:300lp,grow][]"));
        {
          lblTtitle = new JLabel("");
          TmmFontHelper.changeFont(lblTtitle, 1.166, Font.BOLD);
          panelSearchDetail.add(lblTtitle, "cell 1 0,wmin 0");
        }
        {
          lblTvShowPoster = new ImageLabel(false);
          lblTvShowPoster.setDesiredAspectRatio(2 / 3f);
          panelSearchDetail.add(lblTvShowPoster, "cell 0 0 1 4,grow");
        }
        {
          lblOriginalTitle = new JLabel("");
          panelSearchDetail.add(lblOriginalTitle, "cell 1 1,wmin 0");
        }
        {
          JScrollPane scrollPane = new NoBorderScrollPane();
          panelSearchDetail.add(scrollPane, "cell 1 2,grow");

          taOverview = new ReadOnlyTextArea();
          scrollPane.setViewportView(taOverview);
        }
        {
          JPanel panelEpisodeGroup = new JPanel();
          panelEpisodeGroup.setLayout(new FlowLayout());

          lblEpisodeGroup = new JLabel(TmmResourceBundle.getString("tvshow.episodegroup"));
          lblEpisodeGroup.setVisible(false);
          panelEpisodeGroup.add(lblEpisodeGroup);

          cbEpisodeGroup = new JComboBox();
          cbEpisodeGroup.setVisible(false);
          panelEpisodeGroup.add(cbEpisodeGroup);

          btnCompareEpisodeGroup = new JButton(TmmResourceBundle.getString("tmm.episodegroup.compare"));
          btnCompareEpisodeGroup.setToolTipText(TmmResourceBundle.getString("tmm.episodegroup.compare.desc"));
          btnCompareEpisodeGroup.addActionListener(actionEvent -> {
            TvShowChooserEpisodeListDialog dialog = new TvShowChooserEpisodeListDialog(TvShowChooserDialog.this, tvShow, selectedResult);
            dialog.setVisible(true);
          });
          btnCompareEpisodeGroup.setVisible(false);
          panelEpisodeGroup.add(btnCompareEpisodeGroup);

          panelSearchDetail.add(panelEpisodeGroup, "cell 1 3,aligny bottom");
        }
      }
    }
    {
      JSeparator separator = new JSeparator();
      contentPanel.add(separator, "cell 0 3,growx");
    }
    {
      JPanel panelScraperConfig = new JPanel();
      contentPanel.add(panelScraperConfig, "cell 0 4,grow");
      panelScraperConfig.setLayout(new MigLayout("", "[][grow]", "[][][][]"));
      {
        JLabel lblScrapeFollowingItems = new TmmLabel(TmmResourceBundle.getString("chooser.scrape"));
        panelScraperConfig.add(lblScrapeFollowingItems, "cell 0 0 2 1");
      }
      {
        JLabel lblTvShowsT = new TmmLabel(TmmResourceBundle.getString("metatag.tvshows"));
        panelScraperConfig.add(lblTvShowsT, "cell 0 1,alignx trailing");

        cbTvShowScraperConfig = new ScraperMetadataConfigCheckComboBox(TvShowScraperMetadataConfig.getValuesWithout(TvShowScraperMetadataConfig.ID));
        cbTvShowScraperConfig.enableFilter(
            (movieScraperMetadataConfig, s) -> movieScraperMetadataConfig.getDescription().toLowerCase(ROOT).startsWith(s.toLowerCase(ROOT)));
        panelScraperConfig.add(cbTvShowScraperConfig, "cell 1 1,grow, wmin 0");
      }
      {
        JLabel lblEpisodesT = new TmmLabel(TmmResourceBundle.getString("metatag.episodes"));
        panelScraperConfig.add(lblEpisodesT, "cell 0 2,alignx trailing");

        cbEpisodeScraperConfig = new ScraperMetadataConfigCheckComboBox(TvShowEpisodeScraperMetadataConfig.getValues());
        cbEpisodeScraperConfig.enableFilter(
            (movieScraperMetadataConfig, s) -> movieScraperMetadataConfig.getDescription().toLowerCase(ROOT).startsWith(s.toLowerCase(ROOT)));
        panelScraperConfig.add(cbEpisodeScraperConfig, "cell 1 2,grow, wmin 0");
      }
      {
        chckbxDoNotOverwrite = new JHintCheckBox(TmmResourceBundle.getString("message.scrape.donotoverwrite"));
        chckbxDoNotOverwrite.setToolTipText(TmmResourceBundle.getString("message.scrape.donotoverwrite.desc"));
        chckbxDoNotOverwrite.setHintIcon(IconManager.HINT);
        panelScraperConfig.add(chckbxDoNotOverwrite, "cell 0 3 2 1");
      }
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
          abortButton.setActionCommand("Abort");
          abortButton.addActionListener(this);
          abortButton.setIcon(IconManager.STOP_INV);
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
        cancelButton.setActionCommand("Cancel");
        cancelButton.setIcon(IconManager.CANCEL_INV);
        cancelButton.addActionListener(this);
        addButton(cancelButton);

        okButton = new JButton(TmmResourceBundle.getString("Button.ok"));
        okButton.setActionCommand("OK");
        okButton.setIcon(IconManager.APPLY_INV);
        okButton.addActionListener(this);
        addDefaultButton(okButton);
      }
    }

    // install and save the comparator on the Table
    TableComparatorChooser.install(tableSearchResults, searchResultEventList, SINGLE_COLUMN);

    // double click to take the result
    tableSearchResults.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() >= 2 && !e.isConsumed() && e.getButton() == MouseEvent.BUTTON1 && okButton.isEnabled()) {
          actionPerformed(new ActionEvent(okButton, ActionEvent.ACTION_PERFORMED, "OK"));
        }
      }
    });

    // add a change listener for the async loaded metadata
    PropertyChangeListener listener = evt -> {
      String property = evt.getPropertyName();
      if ("scraped".equals(property)) {
        int row = tableSearchResults.convertRowIndexToModel(tableSearchResults.getSelectedRow());
        if (row > -1) {
          TvShowChooserModel model = searchResultEventList.get(row);
          lblOriginalTitle.setText(model.getOriginalTitle());
          if (!model.getPosterUrl().equals(lblTvShowPoster.getImageUrl())) {
            lblTvShowPoster.setImageUrl(model.getPosterUrl());
          }
          taOverview.setText(model.getOverview());

          cbEpisodeGroup.removeAllItems();
          if (model.getEpisodeGroups().isEmpty()) {
            lblEpisodeGroup.setVisible(false);
            cbEpisodeGroup.setVisible(false);
            btnCompareEpisodeGroup.setVisible(false);
          }
          else {
            lblEpisodeGroup.setVisible(true);
            cbEpisodeGroup.setVisible(true);
            btnCompareEpisodeGroup.setVisible(true);
            model.getEpisodeGroups().forEach(cbEpisodeGroup::addItem);

            cbEpisodeGroup.setSelectedItem(model.getEpisodeGroup());
          }
        }
      }
    };

    tableSearchResults.getSelectionModel().addListSelectionListener(e -> {
      if (e.getValueIsAdjusting()) {
        return;
      }

      int index = tableSearchResults.convertRowIndexToModel(tableSearchResults.getSelectedRow());
      if (selectedResult != null) {
        selectedResult.removePropertyChangeListener(listener);
      }
      if (index > -1 && index < searchResultEventList.size()) {
        TvShowChooserModel model = searchResultEventList.get(index);
        lblTvShowPoster.setImageUrl(model.getPosterUrl());
        lblTtitle.setText(model.getCombinedName());
        lblOriginalTitle.setText(model.getOriginalTitle());
        taOverview.setText(model.getOverview());

        lblEpisodeGroup.setVisible(false);
        cbEpisodeGroup.setVisible(false);
        btnCompareEpisodeGroup.setVisible(false);

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
          TvShowChooserModel model = searchResultEventList.get(selectedRow);
          if (model != TvShowChooserModel.emptyResult && !model.isScraped()) {
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
      tvShowToScrape = tvShow;
      progressBar.setVisible(false);
      cbTvShowScraperConfig.setSelectedItems(TvShowModuleManager.getInstance().getSettings().getTvShowScraperMetadataConfig());
      cbEpisodeScraperConfig.setSelectedItems(TvShowModuleManager.getInstance().getSettings().getEpisodeScraperMetadataConfig());
      chckbxDoNotOverwrite.setSelected(TvShowModuleManager.getInstance().getSettings().isDoNotOverwriteExistingData());

      lblPath.setText(tvShowToScrape.getPathNIO().toString());
      textFieldSearchString.setText(tvShowToScrape.getTitle());
      // initial search with IDs
      searchTvShow(textFieldSearchString.getText(), true);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if ("OK".equals(e.getActionCommand())) {
      int row = tableSearchResults.getSelectedRow();
      if (row >= 0) {
        TvShowChooserModel model = searchResultEventList.get(row);
        if (model != TvShowChooserModel.emptyResult) {
          // when scraping was not successful, abort saving
          if (!model.isScraped()) {
            MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "TvShowChooser", "message.scrape.threadcrashed"));
            return;
          }

          List<TvShowScraperMetadataConfig> tvShowScraperMetadataConfig = cbTvShowScraperConfig.getSelectedItems();
          List<TvShowEpisodeScraperMetadataConfig> episodeScraperMetadataConfig = cbEpisodeScraperConfig.getSelectedItems();
          boolean overwrite = !chckbxDoNotOverwrite.isSelected();

          MediaMetadata md = model.getMetadata();

          // check if there is at leat a title in the metadata -> otherwise take the title from the search result
          if (StringUtils.isBlank(md.getTitle())) {
            md.setTitle(model.getTitle());
          }

          // did the user want to choose the images?
          if (!TvShowModuleManager.getInstance().getSettings().isScrapeBestImage()) {
            md.clearMediaArt();
          }

          if (cbEpisodeGroup.getSelectedItem() instanceof MediaEpisodeGroup episodeGroup) {
            tvShowToScrape.setEpisodeGroup(episodeGroup);
          }
          else {
            tvShowToScrape.setEpisodeGroup(MediaEpisodeGroup.DEFAULT_AIRED);
          }
          tvShowToScrape.setEpisodeGroups(model.getEpisodeGroups());

          // get other ratings too?
          if (TvShowModuleManager.getInstance().getSettings().isFetchAllRatings()) {
            for (MediaRating rating : ListUtils.nullSafe(RatingProvider.getRatings(md.getIds(), MediaType.TV_SHOW))) {
              if (!md.getRatings().contains(rating)) {
                md.addRating(rating);
              }
            }
          }

          // set scraped metadata
          tvShowToScrape.setMetadata(md, tvShowScraperMetadataConfig, overwrite);
          tvShowToScrape.setLastScraperId(model.getMediaScraper().getId());
          tvShowToScrape.setLastScrapeLanguage(model.getLanguage().name());

          // get the episode list for display?
          if (TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()) {
            tvShowToScrape.setDummyEpisodes(model.getEpisodesForDisplay());
            tvShowToScrape.saveToDb();
          }

          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

          // get images?
          if (ScraperMetadataConfig.containsAnyArtwork(tvShowScraperMetadataConfig)) {
            // let the user choose the images
            if (!TvShowModuleManager.getInstance().getSettings().isScrapeBestImage()) {
              if (tvShowScraperMetadataConfig.contains(TvShowScraperMetadataConfig.POSTER)
                  && (overwrite || StringUtils.isBlank(tvShowToScrape.getArtworkFilename(MediaFileType.POSTER)))) {
                chooseArtwork(MediaFileType.POSTER);
              }
              if (tvShowScraperMetadataConfig.contains(TvShowScraperMetadataConfig.FANART)
                  && (overwrite || StringUtils.isBlank(tvShowToScrape.getArtworkFilename(MediaFileType.FANART)))) {
                chooseArtwork(MediaFileType.FANART);
              }
              if (tvShowScraperMetadataConfig.contains(TvShowScraperMetadataConfig.BANNER)
                  && (overwrite || StringUtils.isBlank(tvShowToScrape.getArtworkFilename(MediaFileType.BANNER)))) {
                chooseArtwork(MediaFileType.BANNER);
              }
              if (tvShowScraperMetadataConfig.contains(TvShowScraperMetadataConfig.CLEARLOGO)
                  && (overwrite || StringUtils.isBlank(tvShowToScrape.getArtworkFilename(MediaFileType.CLEARLOGO)))) {
                chooseArtwork(MediaFileType.CLEARLOGO);
              }
              if (tvShowScraperMetadataConfig.contains(TvShowScraperMetadataConfig.CLEARART)
                  && (overwrite || StringUtils.isBlank(tvShowToScrape.getArtworkFilename(MediaFileType.CLEARART)))) {
                chooseArtwork(MediaFileType.CLEARART);
              }
              if (tvShowScraperMetadataConfig.contains(TvShowScraperMetadataConfig.THUMB)
                  && (overwrite || StringUtils.isBlank(tvShowToScrape.getArtworkFilename(MediaFileType.THUMB)))) {
                chooseArtwork(MediaFileType.THUMB);
              }
              if (tvShowScraperMetadataConfig.contains(TvShowScraperMetadataConfig.CHARACTERART)
                  && (overwrite || StringUtils.isBlank(tvShowToScrape.getArtworkFilename(MediaFileType.CHARACTERART)))) {
                chooseArtwork(MediaFileType.CHARACTERART);
              }
              if (tvShowScraperMetadataConfig.contains(TvShowScraperMetadataConfig.KEYART)
                  && (overwrite || StringUtils.isBlank(tvShowToScrape.getArtworkFilename(MediaFileType.KEYART)))) {
                chooseArtwork(MediaFileType.KEYART);
              }

              // season artwork
              for (TvShowSeason season : tvShowToScrape.getSeasons().stream().sorted(Comparator.comparingInt(TvShowSeason::getSeason)).toList()) {
                // relevant for v5
                // if (season.isDummy()) {
                // continue;
                // }
                if (tvShowScraperMetadataConfig.contains(TvShowScraperMetadataConfig.SEASON_POSTER)
                    && (overwrite || StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_POSTER)))) {
                  chooseSeasonArtwork(season, MediaArtwork.MediaArtworkType.SEASON_POSTER);
                }
                if (tvShowScraperMetadataConfig.contains(TvShowScraperMetadataConfig.SEASON_BANNER)
                    && (overwrite || StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_BANNER)))) {
                  chooseSeasonArtwork(season, MediaArtwork.MediaArtworkType.SEASON_BANNER);
                }
                if (tvShowScraperMetadataConfig.contains(TvShowScraperMetadataConfig.SEASON_THUMB)
                    && (overwrite || StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_THUMB)))) {
                  chooseSeasonArtwork(season, MediaArtwork.MediaArtworkType.SEASON_THUMB);
                }
              }

              tvShowToScrape.writeNFO(); // rewrite NFO to get the urls into the NFO

              // also force to write all season NFO files
              tvShowToScrape.getSeasons().forEach(TvShowSeason::writeNfo);

              tvShowToScrape.saveToDb();
            }
            else {
              // get artwork asynchronous
              model.startArtworkScrapeTask(tvShowScraperMetadataConfig, overwrite);
            }
          }

          // scrape episodes
          if (!episodeScraperMetadataConfig.isEmpty()) {
            List<TvShowEpisode> episodesToScrape = tvShowToScrape.getEpisodesToScrape();
            // scrape episodes in a task
            if (!episodesToScrape.isEmpty()) {
              // create the episode scrape options
              TvShowEpisodeSearchAndScrapeOptions scrapeOptions = new TvShowEpisodeSearchAndScrapeOptions(md.getIds());
              scrapeOptions.loadDefaults();
              scrapeOptions.setMetadataScraper(model.getMediaScraper());
              scrapeOptions.setArtworkScraper(model.getArtworkScrapers());
              scrapeOptions.setLanguage(model.getLanguage());

              TmmTaskChain.getInstance(tvShowToScrape)
                  .add(new TvShowEpisodeScrapeTask(episodesToScrape, scrapeOptions, episodeScraperMetadataConfig, overwrite));
            }
          }

          // get trailers?
          if (tvShowScraperMetadataConfig.contains(TvShowScraperMetadataConfig.TRAILER)) {
            model.startTrailerScrapeTask(overwrite);
          }

          // get theme?
          if (tvShowScraperMetadataConfig.contains(TvShowScraperMetadataConfig.THEME)) {
            model.startThemeDownloadTask(overwrite);
          }

          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

          if (TvShowModuleManager.getInstance().getSettings().getSyncTrakt()) {
            TvShowSyncTraktTvTask task = new TvShowSyncTraktTvTask(Collections.singletonList(tvShowToScrape));
            task.setSyncCollection(TvShowModuleManager.getInstance().getSettings().getSyncTraktWatched());
            task.setSyncWatched(TvShowModuleManager.getInstance().getSettings().getSyncTraktWatched());
            task.setSyncRating(TvShowModuleManager.getInstance().getSettings().getSyncTraktRating());

            TmmTaskChain.getInstance(tvShowToScrape).add(task);
          }

          // last but not least call a further rename task on the TV show root to move the season fanart into the right folders
          if (TvShowModuleManager.getInstance().getSettings().isRenameAfterScrape()) {
            TmmTaskChain.getInstance(tvShowToScrape).add(new TvShowRenameTask(tvShowToScrape));
          }

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
    List<String> extrafanarts = null;

    switch (mediaFileType) {
      case POSTER:
        if (TvShowModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty()) {
          return;
        }
        imageType = POSTER;
        break;

      case FANART:
        if (TvShowModuleManager.getInstance().getSettings().getFanartFilenames().isEmpty()) {
          return;
        }
        imageType = BACKGROUND;
        extrafanarts = new ArrayList<>();
        break;

      case BANNER:
        if (TvShowModuleManager.getInstance().getSettings().getBannerFilenames().isEmpty()) {
          return;
        }
        imageType = BANNER;
        break;

      case CLEARLOGO:
        if (TvShowModuleManager.getInstance().getSettings().getClearlogoFilenames().isEmpty()) {
          return;
        }
        imageType = CLEARLOGO;
        break;

      case CLEARART:
        if (TvShowModuleManager.getInstance().getSettings().getClearartFilenames().isEmpty()) {
          return;
        }
        imageType = CLEARART;
        break;

      case CHARACTERART:
        if (TvShowModuleManager.getInstance().getSettings().getCharacterartFilenames().isEmpty()) {
          return;
        }
        imageType = CHARACTERART;
        break;

      case THUMB:
        if (TvShowModuleManager.getInstance().getSettings().getThumbFilenames().isEmpty()) {
          return;
        }
        imageType = THUMB;
        break;

      case KEYART:
        if (TvShowModuleManager.getInstance().getSettings().getKeyartFilenames().isEmpty()) {
          return;
        }
        imageType = KEYART;
        break;

      default:
        return;
    }

    String imageUrl = ImageChooserDialog.chooseImage(this, tvShowToScrape.getIds(), imageType, artworkScrapers, null, extrafanarts, MediaType.TV_SHOW,
        tvShowToScrape.getPathNIO().toAbsolutePath().toString());

    tvShowToScrape.setArtworkUrl(imageUrl, mediaFileType);
    if (StringUtils.isNotBlank(imageUrl)) {
      tvShowToScrape.downloadArtwork(mediaFileType);
    }

    // set extrafanarts
    if (mediaFileType == MediaFileType.FANART && extrafanarts != null) {
      tvShowToScrape.setExtraFanartUrls(extrafanarts);
      if (!extrafanarts.isEmpty()) {
        tvShowToScrape.downloadArtwork(MediaFileType.EXTRAFANART);
      }
    }
  }

  private void chooseSeasonArtwork(TvShowSeason season, MediaArtwork.MediaArtworkType imageType) {
    switch (imageType) {
      case SEASON_POSTER:
        if (TvShowModuleManager.getInstance().getSettings().getSeasonPosterFilenames().isEmpty()) {
          return;
        }
        break;

      case SEASON_BANNER:
        if (TvShowModuleManager.getInstance().getSettings().getSeasonBannerFilenames().isEmpty()) {
          return;
        }
        break;

      case SEASON_THUMB:
        if (TvShowModuleManager.getInstance().getSettings().getSeasonThumbFilenames().isEmpty()) {
          return;
        }
        break;

      default:
        return;
    }

    Map<String, Object> ids = new HashMap<>(tvShowToScrape.getIds());
    ids.put("tvShowSeason", season);

    String imageUrl = ImageChooserDialog.chooseImage(this, ids, imageType, artworkScrapers, null, null, MediaType.TV_SHOW,
        tvShowToScrape.getPathNIO().toAbsolutePath().toString());

    season.setArtworkUrl(imageUrl, MediaFileType.getMediaFileType(imageType));
    if (StringUtils.isNotBlank(imageUrl)) {
      TvShowArtworkHelper.downloadSeasonArtwork(season, MediaFileType.getMediaFileType(imageType));
    }
  }

  public boolean isContinueQueue() {
    return continueQueue;
  }

  public boolean isNavigateBack() {
    return navigateBack;
  }

  private void searchTvShow(String searchTerm, boolean withIds) {
    if (activeSearchTask != null && !activeSearchTask.isDone()) {
      activeSearchTask.cancel();
    }
    activeSearchTask = new SearchTask(searchTerm, tvShowToScrape, withIds);
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

  private class SearchTask extends SwingWorker<Void, Void> {
    private final String            searchTerm;
    private final TvShow            show;
    private final boolean           withIds;
    private final MediaLanguages    language;

    private List<MediaSearchResult> searchResult;
    private Throwable               error  = null;
    boolean                         cancel = false;

    private SearchTask(String searchTerm, TvShow show, boolean withIds) {
      this.searchTerm = searchTerm;
      this.show = show;
      this.withIds = withIds;
      this.language = (MediaLanguages) cbLanguage.getSelectedItem();
    }

    @Override
    public Void doInBackground() {
      startProgressBar(TmmResourceBundle.getString("chooser.searchingfor") + " " + searchTerm);
      try {
        searchResult = tvShowList.searchTvShow(searchTerm, show.getYear(), withIds ? show.getIds() : null, mediaScraper, language);
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
        searchResultEventList.add(TvShowChooserModel.emptyResult);
        SwingUtilities.invokeLater(() -> lblError.setText(error.getMessage()));
      }
      else if (!cancel) {
        if (ListUtils.isEmpty(searchResult)) {
          // display empty result
          searchResultEventList.add(TvShowChooserModel.emptyResult);
        }
        else {
          MediaScraper mpFromResult = null;
          for (MediaSearchResult result : searchResult) {
            if (mpFromResult == null) {
              mpFromResult = tvShowList.getMediaScraperById(result.getProviderId());
            }
            if (mpFromResult == null) {
              // still null? maybe we have a Kodi scraper here where the getProdiverId comes from the sub-scraper; take the scraper from the dropdown
              mpFromResult = (MediaScraper) cbScraper.getSelectedItem();
            }
            searchResultEventList.add(new TvShowChooserModel(tvShowToScrape, mpFromResult, artworkScrapers, trailerScrapers, result, language));
          }
        }
      }

      if (!searchResultEventList.isEmpty()) {
        tableSearchResults.setRowSelectionInterval(0, 0); // select first row
      }
    }
  }

  private class ScrapeTask extends SwingWorker<Void, Void> {
    private final TvShowChooserModel model;

    private ScrapeTask(TvShowChooserModel model) {
      this.model = model;
    }

    @Override
    public Void doInBackground() {
      startProgressBar(TmmResourceBundle.getString("chooser.scrapeing") + " " + model.getTitle());

      // disable ok button as long as its scraping
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
   * Shows the dialog and returns whether the work on the queue should be continued.
   * 
   * @return true, if successful
   */
  public boolean showDialog() {
    setVisible(true);
    return continueQueue;
  }

  private class ChangeScraperAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
      mediaScraper = (MediaScraper) cbScraper.getSelectedItem();
      searchTvShow(textFieldSearchString.getText(), false);
    }
  }

  /**
   * inner class for representing the result tableSearchResults
   */
  private static class SearchResultTableFormat extends TmmTableFormat<TvShowChooserModel> {
    private SearchResultTableFormat() {
      Comparator<TvShowChooserModel> searchResultComparator = new TvShowChooserDialog.SearchResultTitleComparator();
      Comparator<String> stringComparator = new StringComparator();
      Comparator<Integer> integerComparator = new IntegerComparator();

      FontMetrics fontMetrics = getFontMetrics();

      /*
       * title
       */
      Column col = new Column(TmmResourceBundle.getString("chooser.searchresult"), "title", result -> result, TvShowChooserModel.class);
      col.setColumnComparator(searchResultComparator);
      col.setCellRenderer(new SearchResultRenderer());
      addColumn(col);

      /*
       * year
       */
      col = new Column(TmmResourceBundle.getString("metatag.year"), "year", TvShowChooserModel::getYear, Integer.class);
      col.setColumnComparator(integerComparator);
      col.setCellRenderer(new IntegerTableCellRenderer());
      col.setColumnResizeable(false);
      col.setMinWidth(fontMetrics.stringWidth("2000") + getCellPadding());
      col.setMaxWidth(fontMetrics.stringWidth("2000") + getCellPadding());
      addColumn(col);

      /*
       * id
       */
      col = new Column(TmmResourceBundle.getString("metatag.id"), "id", TvShowChooserModel::getId, String.class);
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
  private static class SearchResultScoreComparator implements Comparator<TvShowChooserModel> {
    @Override
    public int compare(TvShowChooserModel o1, TvShowChooserModel o2) {
      return Float.compare(o2.getScore(), o1.getScore());
    }
  }

  /**
   * inner class for sorting the search results by name
   */
  private static class SearchResultTitleComparator implements Comparator<TvShowChooserModel> {
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
    public int compare(TvShowChooserModel o1, TvShowChooserModel o2) {
      if (stringCollator != null) {
        String titleTvShow1 = StrgUtils.normalizeString(o1.getTitle().toLowerCase(Locale.ROOT));
        String titleTvShow2 = StrgUtils.normalizeString(o2.getTitle().toLowerCase(Locale.ROOT));
        return stringCollator.compare(titleTvShow1, titleTvShow2);
      }
      return o1.getTitle().toLowerCase(Locale.ROOT).compareTo(o2.getTitle().toLowerCase(Locale.ROOT));
    }
  }

  /**
   * inner class to render the search result
   */
  public static class SearchResultRenderer extends BorderTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      if (value instanceof TvShowChooserModel) {
        TvShowChooserModel result = (TvShowChooserModel) value;
        return super.getTableCellRendererComponent(table, result.getTitle(), isSelected, hasFocus, row, column);
      }
      return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
  }
}
