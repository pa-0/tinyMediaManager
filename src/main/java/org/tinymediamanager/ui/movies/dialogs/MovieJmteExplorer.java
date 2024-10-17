package org.tinymediamanager.ui.movies.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.jmte.JmteUtils;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TablePopupListener;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.MainTabbedPane;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmRoundMultilineTextArea;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.SettingsDialog;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.movies.MovieUIModule;
import org.tinymediamanager.ui.renderer.MultilineTableCellRenderer;

import com.floreysoft.jmte.Engine;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * The class {@link MovieJmteExplorer} is used to offer the user a JMTE playground
 * 
 * @author Manuel Laggner, Wolfgang Janes
 */
public class MovieJmteExplorer extends TmmDialog {

  private final Engine                         engine;
  private final boolean                        renamerMode;
  private final ButtonGroup                    buttonGroup;

  private JComboBox<MoviePreviewContainer>     cbMovieForPreview;
  private JComboBox<EntityContainer>           cbEntity;

  private JTextArea                            taMovieTokens;
  private JRadioButton                         btnPureJmte;
  private JRadioButton                         btnRenamerFoldername;
  private JButton                              btnGetFolderPattern;
  private JButton                              btnSetFolderPattern;
  private JRadioButton                         btnRenamerFilename;
  private JButton                              btnGetFilePattern;
  private JButton                              btnSetFilePattern;
  private JTextArea                            taResult;
  private JTextArea                            taError;
  private JLabel                               lblEntityTemplate;

  private TmmTable                             tableExamples;

  private final EventList<MovieRenamerExample> exampleEventList;
  private final EventList<EntityExample>       entityExampleEventList;
  private final EventList<RendererExample>     rendererExampleList;

  public MovieJmteExplorer(Window owner) {
    super(owner, TmmResourceBundle.getString("jmteexplorer.title"), "moviejmteexplorer");
    setMinimumSize(new Dimension(900, 600));
    engine = MovieRenamer.createEngine();
    buttonGroup = new ButtonGroup();

    if (owner instanceof SettingsDialog) {
      renamerMode = true;
    }
    else {
      renamerMode = false;
      setModalityType(ModalityType.MODELESS);
    }

    exampleEventList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(MovieRenamerExample.class)));
    rendererExampleList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(RendererExample.class)));
    entityExampleEventList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(EntityExample.class)));

    setModal(false);

    initComponents();
    setListeners();

    buildAndInstallMovieArray();

    // make tokens copyable
    JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(new CopyShortRenamerTokenAction());
    popupMenu.add(new CopyLongRenamerTokenAction());

    tableExamples.addMouseListener(new TablePopupListener(popupMenu, tableExamples));

    final KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), false);
    tableExamples.registerKeyboardAction(new CopyShortRenamerTokenAction(), "Copy", copy, JComponent.WHEN_FOCUSED);

    // register double click
    tableExamples.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() >= 2 && !e.isConsumed() && e.getButton() == MouseEvent.BUTTON1) {
          int row = tableExamples.getSelectedRow();
          if (row > -1) {
            row = tableExamples.convertRowIndexToModel(row);
            MovieRenamerExample example = exampleEventList.get(row);
            if (StringUtils.isNotBlank(example.token)) {
              taMovieTokens.setText(taMovieTokens.getText() + example.token);
            }
            else {
              taMovieTokens.setText(taMovieTokens.getText() + example.longToken);
            }
          }
        }
      }
    });
  }

  private void initComponents() {
    {
      JPanel panelHeader = new JPanel(new MigLayout("insets 0", "[grow]", "[][shrink 0][2lp]"));

      JPanel panelTop = new JPanel(new MigLayout("", "[][][grow]", "[]"));
      // Movie / Pattern Input
      panelTop.add(new TmmLabel(TmmResourceBundle.getString("tmm.movie")), "cell 0 0");

      cbMovieForPreview = new JComboBox<>();
      panelTop.add(cbMovieForPreview, "cell 1 0, wmin 0");

      JButton btnHelp = new JButton(TmmResourceBundle.getString("tmm.help"));
      btnHelp.addActionListener(e -> {
        String url = StringEscapeUtils.unescapeHtml4("https://www.tinymediamanager.org/docs/movies/renamer");
        try {
          TmmUIHelper.browseUrl(url);
        }
        catch (Exception ex) {
          MessageManager.instance
              .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", ex.getLocalizedMessage() }));
        }
      });
      panelTop.add(btnHelp, "cell 2 0, trailing");

      panelHeader.add(panelTop, "cell 0 0, growx");

      panelHeader.add(new JSeparator(), "cell 0 1 3 1, grow");
      setTopPanel(panelHeader);
    }

    JSplitPane contentPanel = new JSplitPane();
    contentPanel.setContinuousLayout(true);
    contentPanel.setResizeWeight(0.4);
    contentPanel.setOneTouchExpandable(true);
    contentPanel.setName("movieJmteExplorer.splitPane");
    {
      // left
      {
        JTabbedPane tabbedPane = new MainTabbedPane() {
          @Override
          public void updateUI() {
            putClientProperty("rightBorder", "half");
            putClientProperty("bottomBorder", Boolean.FALSE);
            putClientProperty("roundEdge", Boolean.FALSE);
            super.updateUI();
          }
        };

        tabbedPane.add(TmmResourceBundle.getString("jmteexplorer.commontokens"), createExamplesPanel());
        tabbedPane.add(TmmResourceBundle.getString("jmteexplorer.renderer"), createRendererPanel());
        tabbedPane.add(TmmResourceBundle.getString("jmteexplorer.entities"), createEntitiesPanel());

        contentPanel.setLeftComponent(tabbedPane);
      }

      // right
      {
        JTabbedPane tabbedPane = new MainTabbedPane() {
          @Override
          public void updateUI() {
            putClientProperty("leftBorder", "half");
            putClientProperty("bottomBorder", Boolean.FALSE);
            putClientProperty("roundEdge", Boolean.FALSE);
            super.updateUI();
          }
        };

        tabbedPane.add(TmmResourceBundle.getString("jmteexplorer.tryyourself"), createPlaygroundPanel());
        contentPanel.setRightComponent(tabbedPane);
      }
    }
    add(contentPanel);

    {
      if (renamerMode) {
        JButton btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
        btnCancel.setIcon(IconManager.CANCEL_INV);
        btnCancel.addActionListener(e -> {
          taMovieTokens.setText("");
          setVisible(false);
        });
        addButton(btnCancel);

        JButton btnDone = new JButton(TmmResourceBundle.getString("Button.apply"));
        btnDone.setIcon(IconManager.APPLY_INV);
        btnDone.addActionListener(e -> setVisible(false));
        addButton(btnDone);
      }
      else {
        JButton btnDone = new JButton(TmmResourceBundle.getString("Button.close"));
        btnDone.setIcon(IconManager.APPLY_INV);
        btnDone.addActionListener(e -> setVisible(false));
        addButton(btnDone);
      }
    }

    // set the splitter position after everything has been initialized
    TmmUILayoutStore.getInstance().install(contentPanel);
  }

  private void buildAndInstallMovieArray() {
    List<Movie> allMovies = new ArrayList<>(MovieModuleManager.getInstance().getMovieList().getMovies());
    Movie sel = MovieUIModule.getInstance().getSelectionModel().getSelectedMovie();
    allMovies.sort(Comparator.comparing(MediaEntity::getTitle));

    for (Movie movie : allMovies) {
      MoviePreviewContainer container = new MoviePreviewContainer();
      container.movie = movie;
      cbMovieForPreview.addItem(container);
      if (movie.equals(sel)) {
        cbMovieForPreview.setSelectedItem(container);
      }
    }
  }

  private void createRenamerExample() {
    Movie movie = null;

    if (cbMovieForPreview.getSelectedItem() instanceof MoviePreviewContainer container) {
      movie = container.movie;
    }

    // result
    if (movie != null) {
      String result = "";

      if (StringUtils.isNotBlank(taMovieTokens.getText())) {
        try {
          if (btnPureJmte.isSelected()) {
            result = processPattern(movie, taMovieTokens.getText());
          }
          else if (btnRenamerFoldername.isSelected()) {
            result = MovieRenamer.createDestinationForFoldername(taMovieTokens.getText(), movie);
          }
          else if (btnRenamerFilename.isSelected()) {
            result = MovieRenamer.createDestinationForFilename(taMovieTokens.getText(), movie);
          }
          taError.setText(null);
        }
        catch (Exception e) {
          taError.setText(e.getMessage());
        }

      }
      else {
        result = "";
        taError.setText(null);
      }

      taResult.setText(result);
    }
    else {
      taResult.setText(TmmResourceBundle.getString("Settings.movie.renamer.nomovie"));
    }

    // examples
    for (MovieRenamerExample example : exampleEventList) {
      example.createExample(movie);
    }

    for (RendererExample example : rendererExampleList) {
      example.createResult(movie);
    }

    createEntityExamples(movie);
  }

  private String processPattern(Movie movie, String pattern) throws Exception {
    Map<String, Object> root = new HashMap<>();
    root.put("movie", movie);

    // only offer movie set for movies with more than 1 movie or if setting is set
    if (movie.getMovieSet() != null
        && (movie.getMovieSet().getMovies().size() > 1 || MovieModuleManager.getInstance().getSettings().isRenamerCreateMoviesetForSingleMovie())) {
      root.put("movieSet", movie.getMovieSet());
    }

    return engine.transform(JmteUtils.morphTemplate(pattern, MovieRenamer.getTokenMap()), root);
  }

  private JPanel createExamplesPanel() {
    // examples
    exampleEventList.add(new MovieRenamerExample("${title}"));
    exampleEventList.add(new MovieRenamerExample("${originalTitle}"));
    exampleEventList.add(new MovieRenamerExample("${originalFilename}"));
    exampleEventList.add(new MovieRenamerExample("${originalBasename}"));
    exampleEventList.add(new MovieRenamerExample("${title[0]}"));
    exampleEventList.add(new MovieRenamerExample("${title;first}"));
    exampleEventList.add(new MovieRenamerExample("${title[0,2]}"));
    exampleEventList.add(new MovieRenamerExample("${titleSortable}"));
    exampleEventList.add(new MovieRenamerExample("${releaseDate}"));
    exampleEventList.add(new MovieRenamerExample("${year}"));
    exampleEventList.add(new MovieRenamerExample("${movieSet.title}"));
    exampleEventList.add(new MovieRenamerExample("${movieSet.titleSortable}"));
    exampleEventList.add(new MovieRenamerExample("${movieSetIndex}"));
    exampleEventList.add(new MovieRenamerExample("${rating}"));
    exampleEventList.add(new MovieRenamerExample("${imdb}"));
    exampleEventList.add(new MovieRenamerExample("${tmdb}"));
    exampleEventList.add(new MovieRenamerExample("${certification}"));
    exampleEventList.add(new MovieRenamerExample("${directors[0].name}"));
    exampleEventList.add(new MovieRenamerExample("${actors[0].name}"));
    exampleEventList.add(new MovieRenamerExample("${genres[0]}"));
    exampleEventList.add(new MovieRenamerExample("${genres[0].name}"));
    exampleEventList.add(new MovieRenamerExample("${genresAsString}"));
    exampleEventList.add(new MovieRenamerExample("${tags[0]}"));
    exampleEventList.add(new MovieRenamerExample("${productionCompany}"));
    exampleEventList.add(new MovieRenamerExample("${productionCompanyAsArray[0]}"));
    exampleEventList.add(new MovieRenamerExample("${language}"));
    exampleEventList.add(new MovieRenamerExample("${videoResolution}"));
    exampleEventList.add(new MovieRenamerExample("${aspectRatio}"));
    exampleEventList.add(new MovieRenamerExample("${aspectRatio2}"));
    exampleEventList.add(new MovieRenamerExample("${videoCodec}"));
    exampleEventList.add(new MovieRenamerExample("${videoFormat}"));
    exampleEventList.add(new MovieRenamerExample("${videoBitDepth}"));
    exampleEventList.add(new MovieRenamerExample("${videoBitRate}"));
    exampleEventList.add(new MovieRenamerExample("${framerate}"));
    exampleEventList.add(new MovieRenamerExample("${audioCodec}"));
    exampleEventList.add(new MovieRenamerExample("${audioCodecList}"));
    exampleEventList.add(new MovieRenamerExample("${audioCodecsAsString}"));
    exampleEventList.add(new MovieRenamerExample("${audioChannels}"));
    exampleEventList.add(new MovieRenamerExample("${audioChannelList}"));
    exampleEventList.add(new MovieRenamerExample("${audioChannelsAsString}"));
    exampleEventList.add(new MovieRenamerExample("${audioChannelsDot}"));
    exampleEventList.add(new MovieRenamerExample("${audioChannelDotList}"));
    exampleEventList.add(new MovieRenamerExample("${audioChannelsDotAsString}"));
    exampleEventList.add(new MovieRenamerExample("${audioLanguage}"));
    exampleEventList.add(new MovieRenamerExample("${audioLanguageList}"));
    exampleEventList.add(new MovieRenamerExample("${audioLanguagesAsString}"));
    exampleEventList.add(new MovieRenamerExample("${subtitleLanguageList}"));
    exampleEventList.add(new MovieRenamerExample("${subtitleLanguagesAsString}"));
    exampleEventList.add(new MovieRenamerExample("${mediaSource}"));
    exampleEventList.add(new MovieRenamerExample("${3Dformat}"));
    exampleEventList.add(new MovieRenamerExample("${hdr}"));
    exampleEventList.add(new MovieRenamerExample("${hdrformat}"));
    exampleEventList.add(new MovieRenamerExample("${filesize}"));
    exampleEventList.add(new MovieRenamerExample("${edition}"));
    exampleEventList.add(new MovieRenamerExample("${parent}"));
    exampleEventList.add(new MovieRenamerExample("${note}"));
    exampleEventList.add(new MovieRenamerExample("${decadeShort}"));
    exampleEventList.add(new MovieRenamerExample("${decadeLong}"));

    tableExamples = new TmmTable(new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(exampleEventList), new MovieRenamerExampleTableFormat()));
    JScrollPane scrollPane = new NoBorderScrollPane();
    tableExamples.configureScrollPane(scrollPane);
    tableExamples.setRowHeight(35);

    JPanel panel = new JPanel(new MigLayout("", "[grow]", "[grow]"));
    panel.add(scrollPane, "cell 0 0, grow");

    return panel;
  }

  private JPanel createRendererPanel() {
    // string renderers
    rendererExampleList.add(new RendererExample("--- string renderers ---"));
    rendererExampleList.add(new RendererExample("${movie.title;upper}"));
    rendererExampleList.add(new RendererExample("${movie.title;lower}"));
    rendererExampleList.add(new RendererExample("${movie.title;first}"));
    rendererExampleList.add(new RendererExample("${movie.genresAsString;split(0,2)}"));
    rendererExampleList.add(new RendererExample("${movie.title;replace(a,XX)}"));
    rendererExampleList.add(new RendererExample("${movie.title;replace(umlauts.csv)}"));

    // array handling
    rendererExampleList.add(new RendererExample("--- arrays ---"));
    rendererExampleList.add(new RendererExample("${movie.genres}"));
    rendererExampleList.add(new RendererExample("${movie.genres;array}"));
    rendererExampleList.add(new RendererExample("${movie.genres[0]}"));
    rendererExampleList.add(new RendererExample("${movie.genres[0,2]}"));
    rendererExampleList.add(new RendererExample("${movie.mediaInfoAudioLanguageList;uniqueArray}"));

    // format renderers
    rendererExampleList.add(new RendererExample("--- format ---"));
    rendererExampleList.add(new RendererExample("${movie.releaseDate;date(yyyy-MM-dd)}"));
    rendererExampleList.add(new RendererExample("${movie.ratings.imdb.rating;number(%.0f)}"));
    rendererExampleList.add(new RendererExample("${movie.videofilesize;filesize(G)}"));
    rendererExampleList.add(new RendererExample("${movie.mediaInfoVideoBitrate;bitrate(Mbps)}"));
    rendererExampleList.add(new RendererExample("${movie.mediaInfoFrameRate;framerate(round)}"));

    // meta renderers
    rendererExampleList.add(new RendererExample("--- enhanced ---"));
    rendererExampleList.add(new RendererExample("${movie.title;chain(replace(a,XX);lower)}"));

    TmmTable tableRendererExamples = new TmmTable(
        new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(rendererExampleList), new RendererExampleTableFormat()));

    JScrollPane scrollPane = new JScrollPane();
    tableRendererExamples.configureScrollPane(scrollPane);

    JPanel panel = new JPanel(new MigLayout("", "[grow]", "[grow]"));
    panel.add(scrollPane, "cell 0 0, grow");

    return panel;
  }

  private JPanel createEntitiesPanel() {
    JPanel panel = new JPanel(new MigLayout("", "[grow]", "[][][grow]"));

    Vector<EntityContainer> entityContainers = new Vector<>();
    entityContainers.add(new EntityContainer(Movie.class, "${movie}", movie -> movie));
    entityContainers.add(new EntityContainer(MediaFile.class, "${movie.mainVideoFile}", Movie::getMainVideoFile));
    entityContainers.add(new EntityContainer(Person.class, "${movie.actors[0]}", movie -> ListUtils.getFirst(movie.getActors())));
    entityContainers.add(new EntityContainer(MediaTrailer.class, "${movie.trailer[0]}", movie -> ListUtils.getFirst(movie.getTrailer())));
    entityContainers.add(new EntityContainer(MediaGenres.class, "${movie.genres[0]}", movie -> ListUtils.getFirst(movie.getGenres())));
    entityContainers.add(new EntityContainer(MediaFileAudioStream.class, "${movie.mainVideoFile.audioStreams[0]}",
        movie -> ListUtils.getFirst(movie.getMainVideoFile().getAudioStreams())));
    entityContainers.add(new EntityContainer(MediaFileSubtitle.class, "${movie.mainVideoFile.subtitles[0]}",
        movie -> ListUtils.getFirst(movie.getMainVideoFile().getSubtitles())));

    panel.add(new TmmLabel(TmmResourceBundle.getString("jmteexplorer.entity")), "cell 0 0");
    cbEntity = new JComboBox<>(entityContainers);
    panel.add(cbEntity, "cell 0 0");

    panel.add(new TmmLabel(TmmResourceBundle.getString("jmteexplorer.pattern")), "cell 0 1");
    lblEntityTemplate = new JLabel("");
    panel.add(lblEntityTemplate, "cell 0 1, growx, wmin 0");

    TmmTable tableEntityExamples = new TmmTable(
        new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(entityExampleEventList), new EntityExampleTableFormat()));

    JScrollPane scrollPane = new JScrollPane();
    tableEntityExamples.configureScrollPane(scrollPane);
    panel.add(scrollPane, "cell 0 2, grow");

    return panel;
  }

  private JPanel createPlaygroundPanel() {
    JPanel panelContent = new JPanel(new MigLayout("insets 0", "[grow]", "[]10lp![shrink 0]10lp![]"));

    JPanel panelTop = new JPanel(new MigLayout("hidemode 3", "[grow][]", "[][50lp:n][][][][]10lp![]"));
    panelTop.add(new TmmLabel(TmmResourceBundle.getString("jmteexplorer.pattern")), "cell 0 0");
    JButton btnHelp = new JButton(TmmResourceBundle.getString("tmm.help.jmte"));
    btnHelp.addActionListener(e -> {
      String url = StringEscapeUtils.unescapeHtml4("https://www.tinymediamanager.org/docs/jmte");
      try {
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception ex) {
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", ex.getLocalizedMessage() }));
      }
    });
    panelTop.add(btnHelp, "cell 1 0, trailing");

    taMovieTokens = new TmmRoundMultilineTextArea();
    panelTop.add(taMovieTokens, "cell 0 1 2 1, grow, wmin 0");

    btnPureJmte = new JRadioButton(TmmResourceBundle.getString("jmteexplorer.purejmte"));
    buttonGroup.add(btnPureJmte);

    btnRenamerFoldername = new JRadioButton(TmmResourceBundle.getString("jmteexplorer.foldername"));
    buttonGroup.add(btnRenamerFoldername);

    btnRenamerFilename = new JRadioButton(TmmResourceBundle.getString("jmteexplorer.filename"));
    buttonGroup.add(btnRenamerFilename);

    btnGetFolderPattern = new SquareIconButton(IconManager.FILE_IMPORT_INV);
    btnGetFolderPattern.setToolTipText(TmmResourceBundle.getString("jmteexplorer.foldername.import"));
    btnGetFolderPattern.addActionListener(e -> {
      taMovieTokens.setText(MovieModuleManager.getInstance().getSettings().getRenamerPathname());
      createRenamerExample();
    });
    btnSetFolderPattern = new SquareIconButton(IconManager.FILE_EXPORT_INV);
    btnSetFolderPattern.setToolTipText(TmmResourceBundle.getString("jmteexplorer.foldername.export"));
    btnSetFolderPattern.addActionListener(e -> MovieModuleManager.getInstance().getSettings().setRenamerPathname(taMovieTokens.getText()));

    btnGetFilePattern = new SquareIconButton(IconManager.FILE_IMPORT_INV);
    btnGetFilePattern.setToolTipText(TmmResourceBundle.getString("jmteexplorer.filename.import"));
    btnGetFilePattern.addActionListener(e -> {
      taMovieTokens.setText(MovieModuleManager.getInstance().getSettings().getRenamerFilename());
      createRenamerExample();
    });
    btnSetFilePattern = new SquareIconButton(IconManager.FILE_EXPORT_INV);
    btnSetFilePattern.setToolTipText(TmmResourceBundle.getString("jmteexplorer.filename.export"));
    btnSetFilePattern.addActionListener(e -> MovieModuleManager.getInstance().getSettings().setRenamerFilename(taMovieTokens.getText()));

    if (renamerMode) {
      panelTop.add(new TmmLabel(TmmResourceBundle.getString("jmteexplorer.processmode")), "cell 0 2 2 1, gaptop 10lp");
      panelTop.add(btnPureJmte, "cell 0 3");

      panelTop.add(btnRenamerFoldername, "cell 0 4");
      panelTop.add(btnGetFolderPattern, "cell 1 4, trailing");
      btnGetFolderPattern.setVisible(false);
      panelTop.add(btnSetFolderPattern, "cell 1 4, trailing");
      btnSetFolderPattern.setVisible(false);

      panelTop.add(btnRenamerFilename, "cell 0 5");
      panelTop.add(btnGetFilePattern, "cell 1 5, trailing");
      btnGetFilePattern.setVisible(false);
      panelTop.add(btnSetFilePattern, "cell 1 5, trailing");
      btnSetFilePattern.setVisible(false);

      btnPureJmte.addActionListener(e -> setImportExportButtons());
      btnRenamerFoldername.addActionListener(e -> setImportExportButtons());
      btnRenamerFilename.addActionListener(e -> setImportExportButtons());
    }

    btnPureJmte.setSelected(true);

    taError = new ReadOnlyTextArea("");
    TmmFontHelper.changeFont(taError, Font.BOLD);
    taError.setForeground(Color.RED);
    panelTop.add(taError, "cell 0 5 2 1, grow, wmin 0");

    panelContent.add(panelTop, "cell 0 0, growx");

    panelContent.add(new JSeparator(), "cell 0 1, growx");

    JPanel panelBottom = new JPanel(new MigLayout("", "[grow]", "[][grow]"));
    panelBottom.add(new TmmLabel(TmmResourceBundle.getString("jmteexplorer.result")), "cell 0 0, grow, wmin 0");
    taResult = new ReadOnlyTextArea();
    panelBottom.add(taResult, "cell 0 1, grow, wmin 0");

    panelContent.add(panelBottom, "cell 0 2, grow");

    return panelContent;
  }

  private void setImportExportButtons() {
    if (btnPureJmte.isSelected()) {
      btnGetFolderPattern.setVisible(false);
      btnSetFolderPattern.setVisible(false);
      btnGetFilePattern.setVisible(false);
      btnSetFilePattern.setVisible(false);
    }
    else if (btnRenamerFoldername.isSelected()) {
      btnGetFolderPattern.setVisible(true);
      btnSetFolderPattern.setVisible(true);
      btnGetFilePattern.setVisible(false);
      btnSetFilePattern.setVisible(false);
    }
    else if (btnRenamerFilename.isSelected()) {
      btnGetFolderPattern.setVisible(false);
      btnSetFolderPattern.setVisible(false);
      btnGetFilePattern.setVisible(true);
      btnSetFilePattern.setVisible(true);
    }
    createRenamerExample();
  }

  private void setListeners() {
    DocumentListener documentListener = new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }

      @Override
      public void insertUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }

      @Override
      public void changedUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }
    };

    taMovieTokens.getDocument().addDocumentListener(documentListener);
    cbMovieForPreview.addActionListener(e -> createRenamerExample());
    cbEntity.addActionListener(e -> {
      Movie movie = null;

      if (cbMovieForPreview.getSelectedItem() instanceof MoviePreviewContainer container) {
        movie = container.movie;
      }

      createEntityExamples(movie);
    });
  }

  private void createEntityExamples(Movie movie) {
    entityExampleEventList.clear();

    Object selectedItem = cbEntity.getSelectedItem();
    if (!(selectedItem instanceof EntityContainer entityContainer)) {
      return;
    }

    lblEntityTemplate.setText(entityContainer.getTemplate());

    try {
      PropertyDescriptor[] pds = Introspector.getBeanInfo(entityContainer.getEntity(movie).getClass()).getPropertyDescriptors();
      entityExampleEventList.clear();
      for (PropertyDescriptor descriptor : pds) {

        if ("class".equals(descriptor.getDisplayName())) {
          continue;
        }

        if ("declaringClass".equals(descriptor.getDisplayName())) {
          continue;
        }

        if (descriptor.getReadMethod() != null) {
          try {
            String title = descriptor.getDisplayName();
            entityExampleEventList
                .add(new EntityExample(title, processPattern(movie, entityContainer.getTemplate().replace("}", "." + title + "}"))));
          }
          catch (Exception ignored) {
            // ignored
          }
        }
      }
    }
    catch (Exception ignored) {
      // ignored
    }
  }

  /*****************************************************************************
   * helper classes
   *****************************************************************************/
  @SuppressWarnings("unused")
  private class MovieRenamerExample extends AbstractModelObject {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^\\$\\{(.*?)([\\}\\[;\\.]+.*)");
    private final String         token;
    private final String         completeToken;
    private String               longToken     = "";
    private String               description;
    private String               example       = "";

    private MovieRenamerExample(String token) {
      this.token = token;
      this.completeToken = createCompleteToken();
      try {
        this.description = TmmResourceBundle.getString("Settings.movie.renamer." + token);
      }
      catch (Exception e) {
        this.description = "";
      }
    }

    private String createCompleteToken() {
      String result = token;

      Matcher matcher = TOKEN_PATTERN.matcher(token);
      if (matcher.find() && matcher.groupCount() > 1) {
        String alias = matcher.group(1);
        String sourceToken = MovieRenamer.getTokenMap().get(alias);

        if (StringUtils.isNotBlank(sourceToken)) {
          result = "<html>" + token + "<br>${" + sourceToken + matcher.group(2) + "</html>";
          longToken = "${" + sourceToken + matcher.group(2);
        }
      }
      return result;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getExample() {
      return example;
    }

    public void setExample(String example) {
      this.example = example;
    }

    private void createExample(Movie movie) {
      String oldValue = example;
      if (movie == null) {
        example = "";
      }
      else {
        try {
          example = processPattern(movie, token);
        }
        catch (Exception ignored) {
          // ignored
        }
      }
      firePropertyChange("example", oldValue, example);
    }
  }

  private static class MovieRenamerExampleTableFormat extends TmmTableFormat<MovieRenamerExample> {
    public MovieRenamerExampleTableFormat() {
      /*
       * token name
       */
      Column col = new Column(TmmResourceBundle.getString("Settings.renamer.token.name"), "name", token -> token.completeToken, String.class);
      addColumn(col);

      /*
       * token description
       */
      col = new Column(TmmResourceBundle.getString("Settings.renamer.token"), "description", token -> token.description, String.class);
      col.setCellRenderer(new MultilineTableCellRenderer());
      addColumn(col);

      /*
       * token value
       */
      col = new Column(TmmResourceBundle.getString("Settings.renamer.value"), "value", token -> token.example, String.class);
      col.setCellRenderer(new MultilineTableCellRenderer());
      addColumn(col);
    }
  }

  private static class MoviePreviewContainer {
    Movie movie;

    @Override
    public String toString() {
      return movie.getTitle();
    }
  }

  private static class EntityContainer {
    private final Class<?>                clazz;
    private final String                  template;
    private final Function<Movie, Object> function;

    EntityContainer(Class<?> clazz, String template, Function<Movie, Object> function) {
      this.clazz = clazz;
      this.template = template;
      this.function = function;
    }

    @Override
    public String toString() {
      return clazz.getSimpleName();
    }

    public String getTemplate() {
      return template;
    }

    public Object getEntity(Movie movie) {
      return function.apply(movie);
    }
  }

  private static class EntityExample extends AbstractModelObject {
    private final String title;
    private final String result;

    private EntityExample(String title, String result) {
      this.title = title;
      this.result = result;
    }
  }

  private static class EntityExampleTableFormat extends TmmTableFormat<EntityExample> {
    private EntityExampleTableFormat() {
      Column title = new Column(TmmResourceBundle.getString("jmteexplorer.property"), "property", entityExample -> entityExample.title, String.class);
      addColumn(title);

      Column result = new Column(TmmResourceBundle.getString("Settings.renamer.value"), "result", entityExample -> entityExample.result,
          String.class);
      addColumn(result);
    }
  }

  private class RendererExample extends AbstractModelObject {
    private final String token;

    private String       result;

    RendererExample(String token) {
      this.token = token;
    }

    public String getResult() {
      return result;
    }

    public void setResult(String result) {
      this.result = result;
    }

    public void createResult(Movie movie) {
      String oldValue = this.result;
      if (movie == null || token.startsWith("---")) {
        result = "";
      }
      else {
        try {
          result = processPattern(movie, token);
        }
        catch (Exception ignored) {
          // ignored
        }
      }
      firePropertyChange("result", oldValue, result);
    }
  }

  private static class RendererExampleTableFormat extends TmmTableFormat<RendererExample> {
    public RendererExampleTableFormat() {
      Column title = new Column(TmmResourceBundle.getString("Settings.renamer.token.name"), "token", example -> example.token, String.class);
      addColumn(title);

      Column result = new Column(TmmResourceBundle.getString("Settings.renamer.value"), "result", example -> example.result, String.class);
      addColumn(result);
    }
  }

  private class CopyShortRenamerTokenAction extends AbstractAction {
    CopyShortRenamerTokenAction() {
      putValue(LARGE_ICON_KEY, IconManager.COPY);
      putValue(SMALL_ICON, IconManager.COPY);
      putValue(NAME, TmmResourceBundle.getString("renamer.copytoken"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("renamer.copytoken"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableExamples.getSelectedRow();
      if (row > -1) {
        row = tableExamples.convertRowIndexToModel(row);
        MovieRenamerExample example = exampleEventList.get(row);
        StringSelection stringSelection = new StringSelection(example.token);

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, stringSelection);
      }
    }
  }

  private class CopyLongRenamerTokenAction extends AbstractAction {
    CopyLongRenamerTokenAction() {
      putValue(LARGE_ICON_KEY, IconManager.COPY);
      putValue(SMALL_ICON, IconManager.COPY);
      putValue(NAME, TmmResourceBundle.getString("renamer.copytoken.long"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("renamer.copytoken.long"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableExamples.getSelectedRow();
      if (row > -1) {
        row = tableExamples.convertRowIndexToModel(row);
        MovieRenamerExample example = exampleEventList.get(row);
        StringSelection stringSelection = new StringSelection(example.longToken);

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, stringSelection);
      }
    }
  }
}