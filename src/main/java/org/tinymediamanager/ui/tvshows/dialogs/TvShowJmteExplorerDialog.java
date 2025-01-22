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
package org.tinymediamanager.ui.tvshows.dialogs;

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
import java.io.File;
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

import org.apache.commons.io.FilenameUtils;
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
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowRenamer;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
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
import org.tinymediamanager.ui.renderer.MultilineTableCellRenderer;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

import com.floreysoft.jmte.Engine;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TvShowJmteExplorerDialog} is used to offer the user a JMTE playground
 * 
 * @author Manuel Laggner
 */
public class TvShowJmteExplorerDialog extends TmmDialog {

  private final Engine                       engine;
  private final boolean                      renamerMode;
  private final ButtonGroup                  buttonGroup;

  private JComboBox<TvShowPreviewContainer>  cbTvShowForPreview;
  private JComboBox<EpisodePreviewContainer> cbEpisodeForPreview;
  private JComboBox<EntityContainer>         cbEntity;

  private JTextArea                          taJmteTokens;
  private JRadioButton                       btnPureJmte;
  private JRadioButton                       btnRenamerFoldername;
  private JButton                            btnGetFolderPattern;
  private JButton                            btnSetFolderPattern;
  private JRadioButton                       btnRenamerFilename;
  private JButton                            btnGetFilePattern;
  private JButton                            btnSetFilePattern;
  private JTextArea                          taResult;
  private JTextArea                          taError;
  private JLabel                             lblEntityTemplate;

  private TmmTable                           tableExamples;

  private final EventList<RenamerExample>    exampleEventList;
  private final EventList<EntityExample>     entityExampleEventList;
  private final EventList<RendererExample>   rendererExampleList;

  public TvShowJmteExplorerDialog(Window owner) {
    super(owner, TmmResourceBundle.getString("jmteexplorer.title"), "tvshowjmteexplorer");
    setMinimumSize(new Dimension(900, 600));
    engine = TvShowRenamer.createEngine();
    buttonGroup = new ButtonGroup();

    if (owner instanceof SettingsDialog) {
      renamerMode = true;
    }
    else {
      renamerMode = false;
      setModalityType(ModalityType.MODELESS);
    }

    exampleEventList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(RenamerExample.class)));
    rendererExampleList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(RendererExample.class)));
    entityExampleEventList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(EntityExample.class)));

    setModal(false);

    initComponents();

    buildAndInstallTvShowArray();
    buildAndInstallEpisodeArray();

    setListeners();

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
            RenamerExample example = exampleEventList.get(row);
            if (StringUtils.isNotBlank(example.token)) {
              taJmteTokens.setText(taJmteTokens.getText() + example.token);
            }
            else {
              taJmteTokens.setText(taJmteTokens.getText() + example.longToken);
            }
          }
        }
      }
    });
  }

  private void initComponents() {
    {
      JPanel panelHeader = new JPanel(new MigLayout("insets 0", "[grow]", "[][shrink 0][2lp]"));

      JPanel panelTop = new JPanel(new MigLayout("", "[][][grow]", "[][]"));
      // TV show / episode
      panelTop.add(new TmmLabel(TmmResourceBundle.getString("tmm.tvshow")), "cell 0 0");

      cbTvShowForPreview = new JComboBox<>();
      panelTop.add(cbTvShowForPreview, "cell 1 0, wmin 0");

      JButton btnHelp = new JButton(TmmResourceBundle.getString("tmm.help"));
      btnHelp.addActionListener(e -> {
        String url = StringEscapeUtils.unescapeHtml4("https://www.tinymediamanager.org/docs/tvshows/renamer");
        try {
          TmmUIHelper.browseUrl(url);
        }
        catch (Exception ex) {
          MessageManager.instance
              .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", ex.getLocalizedMessage() }));
        }
      });
      panelTop.add(btnHelp, "cell 2 0, trailing");

      panelTop.add(new TmmLabel(TmmResourceBundle.getString("metatag.episode")), "cell 0 1");

      cbEpisodeForPreview = new JComboBox<>();
      panelTop.add(cbEpisodeForPreview, "cell 1 1, wmin 0");

      panelHeader.add(panelTop, "cell 0 0, growx");

      panelHeader.add(new JSeparator(), "cell 0 1 3 1, grow");
      setTopPanel(panelHeader);
    }

    JSplitPane contentPanel = new JSplitPane();
    contentPanel.setContinuousLayout(true);
    contentPanel.setResizeWeight(0.4);
    contentPanel.setOneTouchExpandable(true);
    contentPanel.setName("tvShowJmteExplorer.splitPane");
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
          taJmteTokens.setText("");
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

  private void buildAndInstallTvShowArray() {
    List<TvShow> allTvShows = new ArrayList<>(TvShowModuleManager.getInstance().getTvShowList().getTvShows());
    TvShow sel = TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShow();
    allTvShows.sort(Comparator.comparing(MediaEntity::getTitle));

    for (TvShow tvShow : allTvShows) {
      TvShowPreviewContainer container = new TvShowPreviewContainer();
      container.tvShow = tvShow;
      cbTvShowForPreview.addItem(container);
      if (tvShow.equals(sel)) {
        cbTvShowForPreview.setSelectedItem(container);
      }
    }
  }

  private void buildAndInstallEpisodeArray() {
    cbEpisodeForPreview.removeAllItems();
    Object obj = cbTvShowForPreview.getSelectedItem();
    if (obj instanceof TvShowPreviewContainer) {
      TvShowPreviewContainer c = (TvShowPreviewContainer) cbTvShowForPreview.getSelectedItem();
      List<TvShowEpisode> sel = TvShowUIModule.getInstance().getSelectionModel().getSelectedEpisodes();
      for (TvShowEpisode episode : c.tvShow.getEpisodes()) {
        EpisodePreviewContainer container = new EpisodePreviewContainer();
        container.episode = episode;
        cbEpisodeForPreview.addItem(container);
        if (sel != null && !sel.isEmpty() && episode.equals(sel.get(0))) {
          cbEpisodeForPreview.setSelectedItem(container);
        }
      }
    }
  }

  private void createRenamerExample() {
    TvShow tvShow = null;
    TvShowEpisode episode = null;

    if (cbTvShowForPreview.getSelectedItem() instanceof TvShowPreviewContainer container) {
      tvShow = container.tvShow;
    }
    if (cbEpisodeForPreview.getSelectedItem() instanceof EpisodePreviewContainer container) {
      episode = container.episode;
    }

    // result
    if (tvShow != null && episode != null) {
      String result = "";

      if (StringUtils.isNotBlank(taJmteTokens.getText())) {
        try {
          if (btnPureJmte.isSelected()) {
            result = processPattern(tvShow, episode, taJmteTokens.getText());
          }
          else if (btnRenamerFoldername.isSelected()) {
            result = TvShowRenamer.getTvShowFoldername(taJmteTokens.getText(), tvShow);
          }
          else if (btnRenamerFilename.isSelected()) {
            MediaFile episodeMf = TvShowRenamer
                .generateEpisodeFilenames(taJmteTokens.getText(), tvShow, episode.getMainVideoFile(),
                    FilenameUtils.getBaseName(episode.getMainVideoFile().getFilename()))
                .get(0);

            result = episodeMf.getFile().toString().replace(episode.getTvShow().getPath() + File.separator, "");
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
      taResult.setText("");
    }
  }

  private String processPattern(TvShow tvShow, TvShowEpisode episode, String pattern) throws Exception {
    Map<String, Object> root = new HashMap<>();

    root.put("tvShow", tvShow);
    root.put("episode", episode);
    root.put("season", episode.getTvShowSeason());

    return engine.transform(JmteUtils.morphTemplate(pattern, TvShowRenamer.getTokenMap()), root);
  }

  private JPanel createExamplesPanel() {
    // examples
    exampleEventList.add(new RenamerExample("${title}"));
    exampleEventList.add(new RenamerExample("${originalTitle}"));
    exampleEventList.add(new RenamerExample("${originalFilename}"));
    exampleEventList.add(new RenamerExample("${originalBasename}"));
    exampleEventList.add(new RenamerExample("${titleSortable}"));
    exampleEventList.add(new RenamerExample("${seasonNr}"));
    exampleEventList.add(new RenamerExample("${seasonNr2}"));
    exampleEventList.add(new RenamerExample("${episodeNr}"));
    exampleEventList.add(new RenamerExample("${episodeNr2}"));
    exampleEventList.add(new RenamerExample("${seasonNrAired}"));
    exampleEventList.add(new RenamerExample("${seasonNrAired2}"));
    exampleEventList.add(new RenamerExample("${episodeNrAired}"));
    exampleEventList.add(new RenamerExample("${episodeNrAired2}"));
    exampleEventList.add(new RenamerExample("${seasonNrDvd}"));
    exampleEventList.add(new RenamerExample("${seasonNrDvd2}"));
    exampleEventList.add(new RenamerExample("${episodeNrDvd}"));
    exampleEventList.add(new RenamerExample("${episodeNrDvd2}"));
    exampleEventList.add(new RenamerExample("${absoluteNr}"));
    exampleEventList.add(new RenamerExample("${absoluteNr2}"));
    exampleEventList.add(new RenamerExample("${airedDate}"));
    exampleEventList.add(new RenamerExample("${episodeRating}"));
    exampleEventList.add(new RenamerExample("${episodeImdb}"));
    exampleEventList.add(new RenamerExample("${episodeTmdb}"));
    exampleEventList.add(new RenamerExample("${episodeTvdb}"));
    exampleEventList.add(new RenamerExample("${episodeTags[0]}"));
    exampleEventList.add(new RenamerExample("${year}"));
    exampleEventList.add(new RenamerExample("${showYear}"));
    exampleEventList.add(new RenamerExample("${showTitle}"));
    exampleEventList.add(new RenamerExample("${showOriginalTitle}"));
    exampleEventList.add(new RenamerExample("${showTitleSortable}"));
    exampleEventList.add(new RenamerExample("${showRating}"));
    exampleEventList.add(new RenamerExample("${showImdb}"));
    exampleEventList.add(new RenamerExample("${showTmdb}"));
    exampleEventList.add(new RenamerExample("${showTvdb}"));
    exampleEventList.add(new RenamerExample("${showCertification}"));
    exampleEventList.add(new RenamerExample("${showStatus}"));
    exampleEventList.add(new RenamerExample("${showProductionCompany}"));
    exampleEventList.add(new RenamerExample("${showProductionCompanyAsArray[0]}"));
    exampleEventList.add(new RenamerExample("${showTags[0]}"));
    exampleEventList.add(new RenamerExample("${showGenres[0]}"));
    exampleEventList.add(new RenamerExample("${showGenres[0].name}"));
    exampleEventList.add(new RenamerExample("${showGenresAsString}"));
    exampleEventList.add(new RenamerExample("${videoResolution}"));
    exampleEventList.add(new RenamerExample("${aspectRatio}"));
    exampleEventList.add(new RenamerExample("${aspectRatio2}"));
    exampleEventList.add(new RenamerExample("${videoCodec}"));
    exampleEventList.add(new RenamerExample("${videoFormat}"));
    exampleEventList.add(new RenamerExample("${videoBitDepth}"));
    exampleEventList.add(new RenamerExample("${videoBitRate}"));
    exampleEventList.add(new RenamerExample("${framerate}"));
    exampleEventList.add(new RenamerExample("${audioCodec}"));
    exampleEventList.add(new RenamerExample("${audioCodecList}"));
    exampleEventList.add(new RenamerExample("${audioCodecsAsString}"));
    exampleEventList.add(new RenamerExample("${audioChannels}"));
    exampleEventList.add(new RenamerExample("${audioChannelList}"));
    exampleEventList.add(new RenamerExample("${audioChannelsAsString}"));
    exampleEventList.add(new RenamerExample("${audioChannelsDot}"));
    exampleEventList.add(new RenamerExample("${audioChannelDotList}"));
    exampleEventList.add(new RenamerExample("${audioChannelsDotAsString}"));
    exampleEventList.add(new RenamerExample("${audioLanguage}"));
    exampleEventList.add(new RenamerExample("${audioLanguageList}"));
    exampleEventList.add(new RenamerExample("${audioLanguagesAsString}"));
    exampleEventList.add(new RenamerExample("${subtitleLanguageList}"));
    exampleEventList.add(new RenamerExample("${subtitleLanguagesAsString}"));
    exampleEventList.add(new RenamerExample("${mediaSource}"));
    exampleEventList.add(new RenamerExample("${hdr}"));
    exampleEventList.add(new RenamerExample("${hdrformat}"));
    exampleEventList.add(new RenamerExample("${filesize}"));
    exampleEventList.add(new RenamerExample("${parent}"));
    exampleEventList.add(new RenamerExample("${showNote}"));
    exampleEventList.add(new RenamerExample("${note}"));
    exampleEventList.add(new RenamerExample("${crc32}"));

    tableExamples = new TmmTable(new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(exampleEventList), new RenamerExampleTableFormat()));
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
    rendererExampleList.add(new RendererExample("${tvShow.title;upper}"));
    rendererExampleList.add(new RendererExample("${tvShow.title;lower}"));
    rendererExampleList.add(new RendererExample("${tvShow.title;first}"));
    rendererExampleList.add(new RendererExample("${episode.title;upper}"));
    rendererExampleList.add(new RendererExample("${episode.title;lower}"));
    rendererExampleList.add(new RendererExample("${episode.title;first}"));
    rendererExampleList.add(new RendererExample("${tvShow.genresAsString;split(0,2)}"));
    rendererExampleList.add(new RendererExample("${tvShow.title;replace(a,XX)}"));
    rendererExampleList.add(new RendererExample("${tvShow.title;replace(umlauts.csv)}"));

    // array handling
    rendererExampleList.add(new RendererExample("--- arrays ---"));
    rendererExampleList.add(new RendererExample("${tvShow.genres}"));
    rendererExampleList.add(new RendererExample("${tvShow.genres;array}"));
    rendererExampleList.add(new RendererExample("${tvShow.genres[0]}"));
    rendererExampleList.add(new RendererExample("${tvShow.genres[0,2]}"));
    rendererExampleList.add(new RendererExample("${episode.mediaInfoAudioLanguageList;uniqueArray}"));

    // format renderers
    rendererExampleList.add(new RendererExample("--- format ---"));
    rendererExampleList.add(new RendererExample("${episode.releaseDate;date(yyyy-MM-dd)}"));
    rendererExampleList.add(new RendererExample("${episode.ratings.imdb.rating;number(%.0f)}"));
    rendererExampleList.add(new RendererExample("${episode.videofilesize;filesize(G)}"));
    rendererExampleList.add(new RendererExample("${episode.mediaInfoVideoBitrate;bitrate(Mbps)}"));
    rendererExampleList.add(new RendererExample("${episode.mediaInfoFrameRate;framerate(round)}"));

    // meta renderers
    rendererExampleList.add(new RendererExample("--- enhanced ---"));
    rendererExampleList.add(new RendererExample("${tvShow.title;chain(replace(a,XX);lower)}"));

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
    entityContainers.add(new EntityContainer());
    entityContainers.add(new EntityContainer(TvShow.class, "${tvShow}", tvShow -> tvShow));
    entityContainers.add(new EntityContainer(TvShowEpisode.class, "${episode}", episode -> episode));
    entityContainers.add(new EntityContainer(TvShowSeason.class, "${episode.tvShowSeason}", season -> season));
    entityContainers.add(new EntityContainer(MediaFile.class, "${episode.mainVideoFile}", episode -> ((TvShowEpisode) episode).getMainVideoFile()));
    entityContainers.add(new EntityContainer(Person.class, "${tvShow.actors[0]}", tvShow -> ListUtils.getFirst(((TvShow) tvShow).getActors())));
    entityContainers.add(new EntityContainer(MediaGenres.class, "${tvShow.genres[0]}", tvShow -> ListUtils.getFirst(((TvShow) tvShow).getActors())));
    entityContainers.add(new EntityContainer(MediaFileAudioStream.class, "${episode.mainVideoFile.audioStreams[0]}",
        episode -> ListUtils.getFirst(((TvShowEpisode) episode).getMainVideoFile().getAudioStreams())));
    entityContainers.add(new EntityContainer(MediaFileSubtitle.class, "${episode.mainVideoFile.subtitles[0]}",
        episode -> ListUtils.getFirst(((TvShowEpisode) episode).getMainVideoFile().getSubtitles())));
    entityContainers
        .add(new EntityContainer(MediaTrailer.class, "${tvShow.trailer[0]}", tvShow -> ListUtils.getFirst(((TvShow) tvShow).getTrailer())));

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

    taJmteTokens = new TmmRoundMultilineTextArea();
    panelTop.add(taJmteTokens, "cell 0 1 2 1, grow, wmin 0");

    btnPureJmte = new JRadioButton(TmmResourceBundle.getString("jmteexplorer.purejmte"));
    buttonGroup.add(btnPureJmte);

    btnRenamerFoldername = new JRadioButton(TmmResourceBundle.getString("jmteexplorer.foldername"));
    buttonGroup.add(btnRenamerFoldername);

    btnRenamerFilename = new JRadioButton(TmmResourceBundle.getString("jmteexplorer.filename"));
    buttonGroup.add(btnRenamerFilename);

    btnGetFolderPattern = new SquareIconButton(IconManager.FILE_IMPORT_INV);
    btnGetFolderPattern.setToolTipText(TmmResourceBundle.getString("jmteexplorer.foldername.import"));
    btnGetFolderPattern.addActionListener(e -> {
      taJmteTokens.setText(TvShowModuleManager.getInstance().getSettings().getRenamerTvShowFoldername());
      createRenamerExample();
    });
    btnSetFolderPattern = new SquareIconButton(IconManager.FILE_EXPORT_INV);
    btnSetFolderPattern.setToolTipText(TmmResourceBundle.getString("jmteexplorer.foldername.export"));
    btnSetFolderPattern.addActionListener(e -> TvShowModuleManager.getInstance().getSettings().setRenamerTvShowFoldername(taJmteTokens.getText()));

    btnGetFilePattern = new SquareIconButton(IconManager.FILE_IMPORT_INV);
    btnGetFilePattern.setToolTipText(TmmResourceBundle.getString("jmteexplorer.filename.import"));
    btnGetFilePattern.addActionListener(e -> {
      taJmteTokens.setText(TvShowModuleManager.getInstance().getSettings().getRenamerFilename());
      createRenamerExample();
    });
    btnSetFilePattern = new SquareIconButton(IconManager.FILE_EXPORT_INV);
    btnSetFilePattern.setToolTipText(TmmResourceBundle.getString("jmteexplorer.filename.export"));
    btnSetFilePattern.addActionListener(e -> TvShowModuleManager.getInstance().getSettings().setRenamerFilename(taJmteTokens.getText()));

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

    taJmteTokens.getDocument().addDocumentListener(documentListener);
    cbTvShowForPreview.addActionListener(e -> {
      buildAndInstallEpisodeArray();
      updateExamples();
    });
    cbEpisodeForPreview.addActionListener(e -> updateExamples());

    cbEntity.addActionListener(e -> {
      TvShow tvShow = null;
      TvShowEpisode episode = null;

      if (cbTvShowForPreview.getSelectedItem() instanceof TvShowPreviewContainer container) {
        tvShow = container.tvShow;
      }
      if (cbEpisodeForPreview.getSelectedItem() instanceof EpisodePreviewContainer container) {
        episode = container.episode;
      }

      createEntityExamples(tvShow, episode);
    });
  }

  private void updateExamples() {
    TvShow tvShow = null;
    TvShowEpisode episode = null;

    if (cbTvShowForPreview.getSelectedItem() instanceof TvShowPreviewContainer container) {
      tvShow = container.tvShow;
    }
    if (cbEpisodeForPreview.getSelectedItem() instanceof EpisodePreviewContainer container) {
      episode = container.episode;
    }

    // examples
    for (RenamerExample example : exampleEventList) {
      example.createExample(tvShow, episode);
    }

    for (RendererExample example : rendererExampleList) {
      example.createResult(tvShow, episode);
    }

    createEntityExamples(tvShow, episode);

    // and also re-create the renamer example
    createRenamerExample();
  }

  private void createEntityExamples(TvShow tvShow, TvShowEpisode episode) {
    entityExampleEventList.clear();

    Object selectedItem = cbEntity.getSelectedItem();
    if (!(selectedItem instanceof EntityContainer entityContainer)) {
      return;
    }

    if (entityContainer.clazz == null) {
      return;
    }

    lblEntityTemplate.setText(entityContainer.getTemplate());

    try {
      PropertyDescriptor[] pds = Introspector.getBeanInfo(entityContainer.getEntity(tvShow, episode).getClass()).getPropertyDescriptors();
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

            // suppress some huge patterns, because they may lock up the UI
            switch (title) {
              case "episodes", "episodesForDisplay", "episodesToScrape" ->
                entityExampleEventList.add(new EntityExample(title, "[TvShowEpisode, ...]"));

              case "seasons" -> entityExampleEventList.add(new EntityExample(title, "[TvShowSeason, ...]"));

              case "episodesMediaFiles, seasonMediaFiles" -> entityExampleEventList.add(new EntityExample(title, "[MediaFile, ...]"));

              case "tvShowSeason" -> entityExampleEventList.add(new EntityExample(title, "TvShowSeason[...]"));

              case "tvShow" -> entityExampleEventList.add(new EntityExample(title, "TvShow[...]"));

              default -> entityExampleEventList
                  .add(new EntityExample(title, processPattern(tvShow, episode, entityContainer.getTemplate().replace("}", "." + title + "}"))));
            }
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
  private class RenamerExample extends AbstractModelObject {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^\\$\\{(.*?)([\\}\\[;\\.]+.*)");
    private final String         token;
    private final String         completeToken;

    private String               longToken     = "";
    private String               description;
    private String               example       = "";

    private RenamerExample(String token) {
      this.token = token;
      this.completeToken = createCompleteToken();
      try {
        this.description = TmmResourceBundle.getString("Settings.tvshow.renamer." + token);
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
        String sourceToken = TvShowRenamer.getTokenMap().get(alias);

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

    private void createExample(TvShow tvShow, TvShowEpisode episode) {
      String oldValue = example;
      if (tvShow == null || episode == null) {
        example = "";
      }
      else {
        try {
          example = processPattern(tvShow, episode, token);
        }
        catch (Exception ignored) {
          // ignored
        }
      }
      firePropertyChange("example", oldValue, example);
    }
  }

  private static class RenamerExampleTableFormat extends TmmTableFormat<RenamerExample> {
    public RenamerExampleTableFormat() {
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

  private static class TvShowPreviewContainer {
    TvShow tvShow;

    @Override
    public String toString() {
      return tvShow.getTitle();
    }
  }

  private static class EpisodePreviewContainer {
    TvShowEpisode episode;

    @Override
    public String toString() {
      return episode.getTitle();
    }
  }

  private static class EntityContainer {
    private final Class<?>                      clazz;
    private final String                        template;
    private final Function<MediaEntity, Object> function;

    EntityContainer() {
      this.clazz = null;
      this.template = null;
      this.function = null;
    }

    EntityContainer(Class<?> clazz, String template, Function<MediaEntity, Object> function) {
      this.clazz = clazz;
      this.template = template;
      this.function = function;
    }

    @Override
    public String toString() {
      if (clazz != null) {
        return clazz.getSimpleName();
      }

      return null;
    }

    public String getTemplate() {
      return template;
    }

    public Object getEntity(TvShow tvShow, TvShowEpisode episode) {
      if (function == null) {
        return null;
      }

      if (template.contains("{tvShow")) {
        return function.apply(tvShow);
      }
      else if (template.contains("{episode")) {
        return function.apply(episode);
      }
      else {
        return null;
      }
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

    public void createResult(TvShow tvShow, TvShowEpisode episode) {
      String oldValue = this.result;
      if (tvShow == null || episode == null || token.startsWith("---")) {
        result = "";
      }
      else {
        try {
          result = processPattern(tvShow, episode, token);
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
        RenamerExample example = exampleEventList.get(row);
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
        RenamerExample example = exampleEventList.get(row);
        StringSelection stringSelection = new StringSelection(example.longToken);

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, stringSelection);
      }
    }
  }
}