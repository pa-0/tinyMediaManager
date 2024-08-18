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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.LanguageStyle;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tasks.DownloadTask;
import org.tinymediamanager.core.tasks.SubtitleDownloadTask;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.SubtitleSearchAndScrapeOptions;
import org.tinymediamanager.scraper.SubtitleSearchResult;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieSubtitleProvider;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TableColumnResizer;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.MediaScraperCheckComboBox;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.movies.MovieSubtitleChooserModel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * This dialog is used to show a chooser for subtitles found with the subtitle scrapers
 * 
 * @author Manuel Laggner
 */
public class MovieSubtitleChooserDialog extends TmmDialog {
  private static final Logger                        LOGGER           = LoggerFactory.getLogger(MovieSubtitleChooserDialog.class);

  private final MovieList                            movieList        = MovieModuleManager.getInstance().getMovieList();
  private final Movie                                movieToScrape;
  private final MediaFile                            fileToScrape;
  private SearchTask                                 activeSearchTask = null;

  private final EventList<MovieSubtitleChooserModel> subtitleEventList;

  private final boolean                              inQueue;
  private boolean                                    continueQueue    = true;

  // UI components
  private TmmTable                                   tableSubs;
  private JComboBox<MediaLanguages>                  cbLanguage;
  private MediaScraperCheckComboBox                  cbScraper;
  private JLabel                                     lblProgressAction;
  private JProgressBar                               progressBar;
  private JButton                                    btnSearch;

  public MovieSubtitleChooserDialog(Movie movie, MediaFile mediaFile, boolean inQueue) {
    super(TmmResourceBundle.getString("moviesubtitlechooser.search"), "movieSubtitleChooser");

    this.movieToScrape = movie;
    this.fileToScrape = mediaFile;
    this.inQueue = inQueue;

    subtitleEventList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(MovieSubtitleChooserModel.class)));

    initComponents();

    // initializations
    LinkListener linkListener = new LinkListener();
    tableSubs.addMouseListener(linkListener);
    tableSubs.addMouseMotionListener(linkListener);
    tableSubs.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    TableColumnResizer.adjustColumnPreferredWidths(tableSubs, 7);

    // Subtitle scraper
    List<MediaScraper> selectedSubtitleScrapers = new ArrayList<>();
    for (MediaScraper subtitleScraper : MovieModuleManager.getInstance().getMovieList().getAvailableSubtitleScrapers()) {
      if (MovieModuleManager.getInstance().getSettings().getSubtitleScrapers().contains(subtitleScraper.getId())) {
        selectedSubtitleScrapers.add(subtitleScraper);
      }
    }
    if (!selectedSubtitleScrapers.isEmpty()) {
      cbScraper.setSelectedItems(selectedSubtitleScrapers);
    }

    for (MediaLanguages language : MediaLanguages.valuesSorted()) {
      cbLanguage.addItem(language);
      if (language == MovieModuleManager.getInstance().getSettings().getSubtitleScraperLanguage()) {
        cbLanguage.setSelectedItem(language);
      }
    }

    // action listeners
    btnSearch.addActionListener(e -> searchSubtitle());
    cbLanguage.addActionListener(e -> searchSubtitle());

    // start initial search
    searchSubtitle();
  }

  private void initComponents() {
    {
      final JPanel panelTitle = new JPanel();
      panelTitle.setLayout(new MigLayout("", "[grow]", "[]"));

      final JLabel lblMovieTitle = new JLabel(movieToScrape.getTitle());
      TmmFontHelper.changeFont(lblMovieTitle, 1.33, Font.BOLD);
      panelTitle.add(lblMovieTitle, "cell 0 0 5 1,growx, wmin 0");

      setTopInformationPanel(panelTitle);
    }
    {
      final JPanel panelContent = new JPanel();
      getContentPane().add(panelContent, BorderLayout.CENTER);
      panelContent.setLayout(new MigLayout("", "[][250lp][][150lp,grow][]", "[][][][][shrink 0][200lp,grow]"));

      final JLabel lblMediaFileNameT = new TmmLabel(TmmResourceBundle.getString("metatag.filename"));
      panelContent.add(lblMediaFileNameT, "cell 0 0,alignx right");

      final JLabel lblMediaFileName = new JLabel(fileToScrape.getFile().toString());
      panelContent.add(lblMediaFileName, "cell 1 0 4 1,growx, wmin 0");

      final JLabel lblRuntimeT = new TmmLabel(TmmResourceBundle.getString("metatag.runtime"));
      panelContent.add(lblRuntimeT, "cell 0 1,alignx right");

      final JLabel lblRuntime = new JLabel(fileToScrape.getDurationHHMMSS());
      panelContent.add(lblRuntime, "cell 1 1");

      final JLabel lblImdbIdT = new TmmLabel(TmmResourceBundle.getString("metatag.imdb"));
      panelContent.add(lblImdbIdT, "cell 2 1");

      final JLabel lblImdbId = new JLabel(movieToScrape.getImdbId());
      panelContent.add(lblImdbId, "cell 3 1");

      final JLabel lblScraperT = new TmmLabel(TmmResourceBundle.getString("scraper"));
      panelContent.add(lblScraperT, "cell 0 2,alignx right");

      cbScraper = new MediaScraperCheckComboBox(movieList.getAvailableSubtitleScrapers());
      panelContent.add(cbScraper, "cell 1 2,growx");

      btnSearch = new JButton(TmmResourceBundle.getString("Button.search"));
      panelContent.add(btnSearch, "cell 2 2,alignx left,aligny top");

      final JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
      panelContent.add(lblLanguageT, "cell 0 3,alignx right");

      cbLanguage = new JComboBox<>();
      panelContent.add(cbLanguage, "cell 1 3,growx");

      JSeparator separator = new JSeparator();
      panelContent.add(separator, "cell 0 4 5 1,growx");

      final JScrollPane scrollPaneSubs = new JScrollPane();
      panelContent.add(scrollPaneSubs, "cell 0 5 5 1,grow");

      tableSubs = new TmmTable(new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(subtitleEventList), new SubtitleTableFormat()));
      tableSubs.setDefaultRenderer(ImageIcon.class, new Renderer());
      tableSubs.configureScrollPane(scrollPaneSubs);
    }
    {
      JPanel infoPanel = new JPanel();
      infoPanel.setLayout(new MigLayout("hidemode 3", "[][grow]", "[]"));

      progressBar = new JProgressBar();
      infoPanel.add(progressBar, "cell 0 0");

      lblProgressAction = new JLabel("");
      infoPanel.add(lblProgressAction, "cell 1 0, wmin 0");

      setBottomInformationPanel(infoPanel);
    }
    {
      if (inQueue) {
        JButton btnAbortQueue = new JButton(TmmResourceBundle.getString("Button.abortqueue"));
        btnAbortQueue.setIcon(IconManager.STOP_INV);
        btnAbortQueue.addActionListener(e -> {
          continueQueue = false;
          setVisible(false);
        });
        addButton(btnAbortQueue);
      }

      JButton btnDone = new JButton(TmmResourceBundle.getString("Button.close"));
      btnDone.setIcon(IconManager.APPLY_INV);
      btnDone.addActionListener(e -> setVisible(false));
      addDefaultButton(btnDone);
    }
  }

  private void searchSubtitle() {
    if (activeSearchTask != null && !activeSearchTask.isDone()) {
      activeSearchTask.cancel();
    }

    // scrapers
    List<MediaScraper> scrapers = new ArrayList<>(cbScraper.getSelectedItems());

    activeSearchTask = new SearchTask(fileToScrape, movieToScrape.getIds(), movieToScrape.getTitle(), scrapers);
    activeSearchTask.execute();
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
    // pack();
    // setLocationRelativeTo(MainWindow.getActiveInstance());
    setVisible(true);
    return continueQueue;
  }

  private class SearchTask extends SwingWorker<Void, Void> {
    private final MediaFile                  mediaFile;
    private final String                     searchTerm;
    private final Map<String, Object>        ids;
    private final List<SubtitleSearchResult> searchResults;
    private final MediaLanguages             language;
    private final List<MediaScraper>         scrapers;

    boolean                                  cancel;
    private String                           message;

    public SearchTask(MediaFile mediaFile, String searchTerm, List<MediaScraper> scrapers) {
      this(mediaFile, Collections.emptyMap(), searchTerm, scrapers);
    }

    public SearchTask(MediaFile mediaFile, Map<String, Object> ids, String searchTerm, List<MediaScraper> scrapers) {
      this.mediaFile = mediaFile;
      this.searchTerm = searchTerm;
      this.ids = ids;
      this.language = (MediaLanguages) cbLanguage.getSelectedItem();
      this.searchResults = new ArrayList<>();
      this.scrapers = scrapers;
      this.cancel = false;
    }

    @Override
    public Void doInBackground() {
      startProgressBar(TmmResourceBundle.getString("chooser.searchingfor") + " " + searchTerm);
      for (MediaScraper scraper : scrapers) {
        try {
          IMovieSubtitleProvider subtitleProvider = (IMovieSubtitleProvider) scraper.getMediaProvider();
          SubtitleSearchAndScrapeOptions options = new SubtitleSearchAndScrapeOptions(MediaType.MOVIE);
          options.setMediaFile(mediaFile);
          options.setSearchQuery(searchTerm);
          options.setIds(ids);
          options.setLanguage(language);
          searchResults.addAll(subtitleProvider.search(options));
        }
        catch (MissingIdException ignored) {
          LOGGER.debug("missing id for scraper {}", scraper.getId());
        }
        catch (ScrapeException e) {
          LOGGER.error("getSubtitles", e);
          message = e.getMessage();
        }
      }

      Collections.sort(searchResults);
      Collections.reverse(searchResults);

      return null;
    }

    public void cancel() {
      cancel = true;
    }

    @Override
    public void done() {
      if (!cancel) {
        subtitleEventList.clear();
        if (searchResults == null || searchResults.isEmpty()) {
          // display empty result
          subtitleEventList.add(MovieSubtitleChooserModel.EMPTY_RESULT);
        }
        else {
          for (SubtitleSearchResult result : searchResults) {
            subtitleEventList.add(new MovieSubtitleChooserModel(result, language));
            // get metadataProvider from searchresult
          }
        }
        if (!subtitleEventList.isEmpty()) {
          tableSubs.setRowSelectionInterval(0, 0); // select first row
        }
        TableColumnResizer.adjustColumnPreferredWidths(tableSubs, 15);
      }
      stopProgressBar();

      if (!cancel && StringUtils.isNotBlank(message)) {
        SwingUtilities.invokeLater(() -> {
          lblProgressAction.setVisible(true);
          lblProgressAction.setText(message);
        });
      }
    }
  }

  private static class SubtitleTableFormat extends TmmTableFormat<MovieSubtitleChooserModel> {
    public SubtitleTableFormat() {
      /*
       * download
       */
      Column col = new Column("", "icon", model -> IconManager.DOWNLOAD, ImageIcon.class);
      addColumn(col);

      /*
       * title
       */
      col = new Column(TmmResourceBundle.getString("metatag.title"), "title", MovieSubtitleChooserModel::getName, String.class);
      col.setCellTooltip(MovieSubtitleChooserModel::getName);
      addColumn(col);

      /*
       * release name
       */
      col = new Column(TmmResourceBundle.getString("metatag.releasename"), "releasename", MovieSubtitleChooserModel::getReleaseName, String.class);
      col.setCellTooltip(MovieSubtitleChooserModel::getReleaseName);
      addColumn(col);
    }
  }

  private static class Renderer extends DefaultTableCellRenderer {
    private final JLabel downloadLabel;

    Renderer() {
      downloadLabel = new JLabel(TmmResourceBundle.getString("Button.download"), IconManager.DOWNLOAD, SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      if (value == IconManager.DOWNLOAD) {
        return downloadLabel;
      }
      return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
  }

  private class LinkListener implements MouseListener, MouseMotionListener {
    @Override
    public void mouseClicked(MouseEvent e) {
      JTable table = (JTable) e.getSource();
      int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));

      // click on the download button
      if (col == 0) {
        row = table.convertRowIndexToModel(row);
        MovieSubtitleChooserModel model = subtitleEventList.get(row);

        try {
          if (StringUtils.isNotBlank(model.getDownloadUrl())) {
            // the right language tag from the renamer settings
            String lang = LanguageStyle.getLanguageCodeForStyle(model.getLanguage().name(),
                MovieModuleManager.getInstance().getSettings().getSubtitleLanguageStyle());
            if (StringUtils.isBlank(lang)) {
              lang = model.getLanguage().name();
            }
            String filename = FilenameUtils.getBaseName(fileToScrape.getFilename()) + "." + lang;

            DownloadTask task = new SubtitleDownloadTask(model.getDownloadUrl(), movieToScrape.getPathNIO().resolve(filename), movieToScrape);

            // blocking
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            lblProgressAction.setVisible(true);
            lblProgressAction.setText(TmmResourceBundle.getString("subtitle.downloading"));

            task.run();

            lblProgressAction.setText(TmmResourceBundle.getString("subtitle.downloaded") + " - " + model.getReleaseName());
          }
        }
        catch (Exception ex) {
          lblProgressAction.setVisible(true);
          lblProgressAction.setText(TmmResourceBundle.getString("message.scrape.subtitlefaileddownload") + " - " + ex.getLocalizedMessage());
        }
        finally {
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      JTable table = (JTable) e.getSource();
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
      if (col == 0 || col == 1) {
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      JTable table = (JTable) e.getSource();
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
      if (col != 0 && col != 1) {
        table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      JTable table = (JTable) e.getSource();
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
      if (col != 0 && col != 1 && table.getCursor().getType() == Cursor.HAND_CURSOR) {
        table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
      if ((col == 0 || col == 1) && table.getCursor().getType() == Cursor.DEFAULT_CURSOR) {
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent arg0) {
    }
  }
}
