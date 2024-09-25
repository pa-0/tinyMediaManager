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

import static org.tinymediamanager.core.entities.Person.Type.GUEST;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.observablecollections.ObservableCollections;
import org.jdesktop.swingbinding.JListBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowEpisodeEdition;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.kodi.KodiTvShowMetadataProvider;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.ShadowLayerUI;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.MediaIdTable;
import org.tinymediamanager.ui.components.MediaRatingTable;
import org.tinymediamanager.ui.components.PersonTable;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmObligatoryTextArea;
import org.tinymediamanager.ui.components.TmmRoundTextArea;
import org.tinymediamanager.ui.components.TmmTabbedPane;
import org.tinymediamanager.ui.components.combobox.AutoCompleteSupport;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;
import org.tinymediamanager.ui.components.combobox.MediaScraperComboBox;
import org.tinymediamanager.ui.components.datepicker.DatePicker;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.dialogs.AbstractEditorDialog;
import org.tinymediamanager.ui.dialogs.ImageChooserDialog;
import org.tinymediamanager.ui.panels.IdEditorPanel;
import org.tinymediamanager.ui.panels.MediaFileEditorPanel;
import org.tinymediamanager.ui.panels.ModalPopupPanel;
import org.tinymediamanager.ui.panels.RatingEditorPanel;
import org.tinymediamanager.ui.tvshows.EpisodeNumberTable;
import org.tinymediamanager.ui.tvshows.TvShowEpisodeNumberEditorPanel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowEpisodeScrapeDialog.
 * 
 * @author Manuel Laggner
 */
public class TvShowEpisodeEditorDialog extends AbstractEditorDialog {
  private static final Logger                        LOGGER              = LoggerFactory.getLogger(TvShowEpisodeEditorDialog.class);
  private static final String                        ORIGINAL_IMAGE_SIZE = "originalImageSize";
  private static final String                        DIALOG_ID           = "tvShowEpisodeEditor";

  private final TvShowList                           tvShowList          = TvShowModuleManager.getInstance().getTvShowList();
  private final JTabbedPane                          tabbedPane          = new TmmTabbedPane();
  private final TvShowEpisode                        episodeToEdit;
  private final List<String>                         tags                = ObservableCollections.observableList(new ArrayList<>());
  private final List<MediaFile>                      mediaFiles          = new ArrayList<>();

  private final EventList<MediaEpisodeNumber>        episodeNumbers;
  private final EventList<MediaIdTable.MediaId>      ids;
  private final EventList<MediaRatingTable.Rating>   ratings;
  private final EventList<Person>                    guests;
  private final EventList<Person>                    directors;
  private final EventList<Person>                    writers;

  private JTextArea                                  tfTitle;
  private JSpinner                                   spRating;
  private DatePicker                                 dpFirstAired;
  private JSpinner                                   spDateAdded;
  private JCheckBox                                  chckbxWatched;
  private ImageLabel                                 lblThumb;
  private JTextArea                                  taPlot;
  private AutocompleteComboBox<String>               cbTags;
  private AutoCompleteSupport<String>                cbTagsAutoCompleteSupport;
  private JList<String>                              listTags;
  private AutocompleteComboBox<MediaSource>          cbMediaSource;
  private AutocompleteComboBox<TvShowEpisodeEdition> cbEdition;
  private MediaFileEditorPanel                       mediaFilesPanel;
  private MediaScraperComboBox                       cbScraper;

  private TmmTable                                   tableIds;
  private TmmTable                                   tableRatings;
  private PersonTable                                tableGuests;
  private PersonTable                                tableDirectors;
  private PersonTable                                tableWriters;
  private JTextArea                                  tfOriginalTitle;
  private JTextField                                 tfThumb;
  private JTextArea                                  taNote;
  private TmmTable                                   tableEpisodeNumbers;

  private ScrapeTask                                 scrapeTask          = null;

  /**
   * Instantiates a new TV show episode scrape dialog.
   * 
   * @param episode
   *          the episode
   * @param queueIndex
   *          the actual index in the queue
   * @param queueSize
   *          the queue size
   */
  public TvShowEpisodeEditorDialog(TvShowEpisode episode, int queueIndex, int queueSize, int selectedTab) {
    super(TmmResourceBundle.getString("tvshowepisode.edit") + "  < " + episode.getMainVideoFile().getFilename() + " >", DIALOG_ID, episode);

    // creation of lists
    episodeNumbers = GlazedLists.threadSafeList(new BasicEventList<>());
    guests = new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()), GlazedLists.beanConnector(Person.class));
    directors = new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()), GlazedLists.beanConnector(Person.class));
    writers = new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()), GlazedLists.beanConnector(Person.class));

    for (MediaFile mf : episode.getMediaFiles()) {
      mediaFiles.add(new MediaFile(mf));
    }

    this.episodeToEdit = episode;
    this.queueIndex = queueIndex;
    this.queueSize = queueSize;
    this.ids = MediaIdTable.convertIdMapToEventList(episode.getIds());
    this.ratings = GlazedLists.threadSafeList(MediaRatingTable.convertRatingMapToEventList(episode.getRatings(), false));
    MediaRating userMediaRating = episodeToEdit.getRating(MediaRating.USER);

    initComponents();
    initDataBindings();

    // fill data
    {
      tfTitle.setText(episodeToEdit.getTitle());
      tfOriginalTitle.setText(episodeToEdit.getOriginalTitle());
      spDateAdded.setValue(episodeToEdit.getDateAdded());
      spRating.setModel(new SpinnerNumberModel(userMediaRating.getRating(), 0.0, 10.0, 0.1));

      lblThumb.setImagePath(episodeToEdit.getArtworkFilename(MediaFileType.THUMB));
      tfThumb.setText(episodeToEdit.getArtworkUrl(MediaFileType.THUMB));
      chckbxWatched.setSelected(episodeToEdit.isWatched());
      taPlot.setText(episodeToEdit.getPlot());
      taPlot.setCaretPosition(0);
      cbMediaSource.setSelectedItem(episodeToEdit.getMediaSource());
      cbEdition.setSelectedItem(episodeToEdit.getEdition());
      taNote.setText(episodeToEdit.getNote());

      episodeNumbers.addAll(episodeToEdit.getEpisodeNumbers());

      for (Person origCast : episodeToEdit.getActors()) {
        guests.add(new Person(origCast));
      }
      for (Person director : episodeToEdit.getDirectors()) {
        directors.add(new Person(director));
      }
      for (Person writer : episodeToEdit.getWriters()) {
        writers.add(new Person(writer));
      }

      tags.addAll(episodeToEdit.getTags());
    }

    tabbedPane.setSelectedIndex(selectedTab);
  }

  /**
   * Returns the tab number
   * 
   * @return 0-X
   */
  public int getSelectedTab() {
    return tabbedPane.getSelectedIndex();
  }

  private void initComponents() {
    // to draw the shadow beneath window frame, encapsulate the panel
    JLayer<JComponent> rootLayer = new JLayer(tabbedPane, new ShadowLayerUI()); // removed <> because this leads WBP to crash
    getContentPane().add(rootLayer, BorderLayout.CENTER);

    /**********************************************************************************
     * DetailsPanel 1
     **********************************************************************************/
    {
      JPanel detailsPanel = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("metatag.details"), detailsPanel);
      detailsPanel.setLayout(new MigLayout("", "[][20lp:100lp:175lp][50lp:100lp:175lp][200lp:250lp,grow][][25lp:n][200lp:250lp,grow]",
          "[][][100lp:15%:20%][][100lp:125lp:30%,grow][][][][][50lp:50lp:100lp,grow 50][50lp:50lp:100lp,grow 50]"));

      {
        JLabel lblTitle = new TmmLabel(TmmResourceBundle.getString("metatag.title"));
        detailsPanel.add(lblTitle, "cell 0 0,alignx right");

        tfTitle = new TmmObligatoryTextArea();
        detailsPanel.add(tfTitle, "flowx,cell 1 0 4 1,growx, wmin 0");

        final JButton btnPlay = new SquareIconButton(IconManager.PLAY_INV);
        btnPlay.setFocusable(false);
        btnPlay.addActionListener(e -> {
          MediaFile mf = episodeToEdit.getMainVideoFile();
          try {
            TmmUIHelper.openFile(MediaFileHelper.getMainVideoFile(mf));
          }
          catch (Exception ex) {
            LOGGER.error("open file - {}", e);
            MessageManager.instance
                .pushMessage(new Message(MessageLevel.ERROR, mf, "message.erroropenfile", new String[] { ":", ex.getLocalizedMessage() }));
          }
        });
        detailsPanel.add(btnPlay, "cell 1 0 4 1");
      }
      {
        JLabel lblOriginalTitleT = new TmmLabel(TmmResourceBundle.getString("metatag.originaltitle"));
        detailsPanel.add(lblOriginalTitleT, "cell 0 1,alignx trailing");

        tfOriginalTitle = new TmmRoundTextArea();
        detailsPanel.add(tfOriginalTitle, "cell 1 1 4 1,growx, wmin 0");
      }
      {
        JLabel lblEpisode = new TmmLabel(TmmResourceBundle.getString("metatag.episode"));
        detailsPanel.add(lblEpisode, "flowy,cell 0 2,alignx right,aligny top");

        JScrollPane scrollPaneEpisodeNumbers = new JScrollPane();

        tableEpisodeNumbers = new EpisodeNumberTable(episodeNumbers) {
          @Override
          protected void editButtonClicked(int row) {
            MediaEpisodeNumber episodeNumber = episodeNumbers.get(row);
            if (episodeNumber == null) {
              return;
            }

            ModalPopupPanel popupPanel = createModalPopupPanel();
            popupPanel.setTitle(TmmResourceBundle.getString("episodenumber.edit"));

            TvShowEpisodeNumberEditorPanel episodeNumberEditorPanel = new TvShowEpisodeNumberEditorPanel(episodeNumber,
                episodeToEdit.getTvShow().getEpisodeGroups());
            popupPanel.setContent(episodeNumberEditorPanel);
            popupPanel.setOnCloseHandler(() -> {
              MediaEpisodeNumber episodeNumber1 = episodeNumberEditorPanel.getEpisodeNumber();
              if (episodeNumber1 != null && episodeNumber1.containsAnyNumber()) {
                addOrEditEpisodeNumber(episodeNumber1);
              }
            });

            showModalPopupPanel(popupPanel);
          }
        };
        tableEpisodeNumbers.configureScrollPane(scrollPaneEpisodeNumbers);
        detailsPanel.add(scrollPaneEpisodeNumbers, "cell 1 2 3 1,grow");

        JButton btnAddEpisodeNumber = new SquareIconButton(new AddEpisodeNumberAction());
        detailsPanel.add(btnAddEpisodeNumber, "cell 0 2,alignx right");

        JButton btnRemoveEpisodeNumber = new SquareIconButton(new RemoveEpisodeNumberAction());
        detailsPanel.add(btnRemoveEpisodeNumber, "cell 0 2,alignx right");
      }
      {
        JLabel lblFirstAired = new TmmLabel(TmmResourceBundle.getString("metatag.aired"));
        detailsPanel.add(lblFirstAired, "cell 0 3,alignx right");
      }

      dpFirstAired = new DatePicker(episodeToEdit.getFirstAired());
      detailsPanel.add(dpFirstAired, "cell 1 3 2 1,growx");
      {
        JLabel lblPlot = new TmmLabel(TmmResourceBundle.getString("metatag.plot"));
        detailsPanel.add(lblPlot, "cell 0 4,alignx right,aligny top");

        JScrollPane scrollPane = new JScrollPane();
        detailsPanel.add(scrollPane, "cell 1 4 4 1,grow");

        taPlot = new JTextArea();
        taPlot.setLineWrap(true);
        taPlot.setWrapStyleWord(true);
        scrollPane.setViewportView(taPlot);
      }
      {
        detailsPanel.add(new TmmLabel(TmmResourceBundle.getString("mediafiletype.thumb")), "cell 6 0");

        LinkLabel lblThumbSize = new LinkLabel();
        detailsPanel.add(lblThumbSize, "cell 6 0");

        JButton btnDeleteThumb = new FlatButton(IconManager.DELETE_GRAY);
        btnDeleteThumb.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteThumb.addActionListener(e -> {
          lblThumb.clearImage();
          tfThumb.setText("");
        });
        detailsPanel.add(btnDeleteThumb, "cell 6 0");

        lblThumb = new ImageLabel();
        lblThumb.setDesiredAspectRatio(16 / 9f);
        lblThumb.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            Map<String, Object> newIds = new HashMap<>(episodeToEdit.getIds());
            newIds.put(MediaMetadata.EPISODE_NR, new ArrayList<>(episodeNumbers));
            newIds.put("mediaFile", episodeToEdit.getMainFile());
            newIds.put(MediaMetadata.TVSHOW_IDS, episodeToEdit.getTvShow().getIds());

            ImageChooserDialog dialog = new ImageChooserDialog(TvShowEpisodeEditorDialog.this, newIds, THUMB, tvShowList.getDefaultArtworkScrapers(),
                lblThumb, MediaType.TV_EPISODE);

            if (Settings.getInstance().isImageChooserUseEntityFolder()) {
              dialog.setOpenFolderPath(episodeToEdit.getPathNIO().toAbsolutePath().toString());
            }

            dialog.setLocationRelativeTo(MainWindow.getInstance());
            dialog.setVisible(true);
            updateArtworkUrl(lblThumb, tfThumb);
          }
        });
        lblThumb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        detailsPanel.add(lblThumb, "cell 6 1 1 4,grow");
        lblThumb.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblThumbSize, lblThumb, btnDeleteThumb, MediaFileType.THUMB));
      }
      {
        JLabel lblRating = new TmmLabel(TmmResourceBundle.getString("metatag.userrating"));
        detailsPanel.add(lblRating, "cell 0 7,alignx right");

        spRating = new JSpinner();
        detailsPanel.add(spRating, "cell 1 7 2 1,flowx");

        JLabel lblUserRatingHint = new JLabel(IconManager.HINT);
        lblUserRatingHint.setToolTipText(TmmResourceBundle.getString("edit.userrating.hint"));
        detailsPanel.add(lblUserRatingHint, "cell 1 7 2 1");
      }
      {
        JLabel lblRatingsT = new TmmLabel(TmmResourceBundle.getString("metatag.ratings"));
        detailsPanel.add(lblRatingsT, "flowy,cell 0 8,alignx right,aligny top");

        JScrollPane scrollPaneRatings = new JScrollPane();
        detailsPanel.add(scrollPaneRatings, "cell 1 8 3 2,grow");

        tableRatings = new MediaRatingTable(ratings);
        tableRatings.configureScrollPane(scrollPaneRatings);

        JButton btnAddRating = new SquareIconButton(new AddRatingAction());
        detailsPanel.add(btnAddRating, "cell 0 8,alignx right,aligny top");

        JButton btnRemoveRating = new SquareIconButton(new RemoveRatingAction());
        detailsPanel.add(btnRemoveRating, "cell 0 8,alignx right,aligny top");
      }
      {
        JLabel lblNoteT = new TmmLabel(TmmResourceBundle.getString("metatag.note"));
        detailsPanel.add(lblNoteT, "cell 0 10,alignx right,aligny top");

        JScrollPane scrollPane = new JScrollPane();
        detailsPanel.add(scrollPane, "cell 1 10 4 1,wmin 0,grow");

        taNote = new JTextArea();
        taNote.setLineWrap(true);
        taNote.setWrapStyleWord(true);
        taNote.setForeground(UIManager.getColor("TextField.foreground"));
        scrollPane.setViewportView(taNote);
      }
    }

    /**********************************************************************************
     * Detail 2 panel
     **********************************************************************************/
    {
      JPanel details2Panel = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("metatag.details2"), details2Panel);
      details2Panel.setLayout(new MigLayout("", "[][][20lp:50lp][][20lp:n][][300lp:300lp]", "[][][][20lp:n][100lp:150lp,grow][][][][grow 200]"));
      {
        JLabel lblDateAdded = new TmmLabel(TmmResourceBundle.getString("metatag.dateadded"));
        details2Panel.add(lblDateAdded, "cell 0 0,alignx right");

        spDateAdded = new JSpinner(new SpinnerDateModel());
        details2Panel.add(spDateAdded, "cell 1 0,growx");
      }
      {
        JLabel lblWatched = new TmmLabel(TmmResourceBundle.getString("metatag.watched"));
        details2Panel.add(lblWatched, "flowx,cell 3 0,alignx right");

        chckbxWatched = new JCheckBox("");
        details2Panel.add(chckbxWatched, "cell 3 0");
      }
      {
        JLabel lblMediasource = new TmmLabel(TmmResourceBundle.getString("metatag.source"));
        details2Panel.add(lblMediasource, "cell 0 1,alignx right");

        cbMediaSource = new AutocompleteComboBox(MediaSource.values());
        details2Panel.add(cbMediaSource, "cell 1 1,growx");
      }
      {
        JLabel lblEdition = new TmmLabel(TmmResourceBundle.getString("metatag.edition"));
        details2Panel.add(lblEdition, "cell 0 2,alignx right");

        cbEdition = new AutocompleteComboBox(TvShowEpisodeEdition.values());
        cbEdition.getAutoCompleteSupport().setCorrectsCase(false);
        details2Panel.add(cbEdition, "cell 1 2,growx");
      }
      {
        JLabel lblTags = new TmmLabel(TmmResourceBundle.getString("metatag.tags"));
        details2Panel.add(lblTags, "flowy,cell 0 4,alignx right,aligny top");

        JScrollPane scrollPaneTags = new JScrollPane();
        details2Panel.add(scrollPaneTags, "cell 1 4 3 1,grow");

        listTags = new JList();
        scrollPaneTags.setViewportView(listTags);

        JButton btnAddTag = new SquareIconButton(new AddTagAction());
        details2Panel.add(btnAddTag, "cell 0 4,alignx right,aligny top");

        JButton btnRemoveTag = new SquareIconButton(new RemoveTagAction());
        details2Panel.add(btnRemoveTag, "cell 0 4,alignx right,aligny top");

        JButton btnMoveTagUp = new SquareIconButton(new MoveTagUpAction());
        details2Panel.add(btnMoveTagUp, "cell 0 4,alignx right,aligny top");

        JButton btnMoveTagDown = new SquareIconButton(new MoveTagDownAction());
        details2Panel.add(btnMoveTagDown, "cell 0 4,alignx right,aligny top");

        cbTags = new AutocompleteComboBox<>(tvShowList.getTagsInEpisodes());
        cbTags.setEditable(true);
        cbTagsAutoCompleteSupport = cbTags.getAutoCompleteSupport();
        details2Panel.add(cbTags, "cell 1 5 3 1,growx");

        InputMap im = cbTags.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        Object enterAction = im.get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        cbTags.getActionMap().put(enterAction, new AddTagAction());
      }
      {
        JLabel label = new TmmLabel(TmmResourceBundle.getString("metatag.ids"));
        details2Panel.add(label, "flowy,cell 5 4,alignx right,aligny top");

        JScrollPane scrollPaneIds = new JScrollPane();
        details2Panel.add(scrollPaneIds, "cell 6 4,grow");

        tableIds = new MediaIdTable(ids, ScraperType.TV_SHOW);
        tableIds.configureScrollPane(scrollPaneIds);

        JButton btnAddId = new SquareIconButton(new AddIdAction());
        details2Panel.add(btnAddId, "cell 5 4,alignx right,aligny top");

        JButton btnRemoveId = new SquareIconButton(new RemoveIdAction());
        details2Panel.add(btnRemoveId, "cell 5 4,alignx right,aligny top");
      }

      {
        JLabel lblThumbT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.thumb"));
        details2Panel.add(lblThumbT, "cell 0 7,alignx right");

        tfThumb = new JTextField();
        details2Panel.add(tfThumb, "cell 1 7 6 1,growx");
        tfThumb.setColumns(10);
      }
    }

    /**********************************************************************************
     * CrewPanel
     **********************************************************************************/
    {
      JPanel crewPanel = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("movie.edit.castandcrew"), null, crewPanel, null);
      crewPanel.setLayout(new MigLayout("", "[][150lp:300lp,grow][20lp:n][][150lp:300lp,grow]", "[100lp:250lp][20lp:n][100lp:200lp]"));
      {
        JLabel lblGuests = new TmmLabel(TmmResourceBundle.getString("metatag.guests"));
        crewPanel.add(lblGuests, "flowy,cell 0 0,alignx right,aligny top");

        tableGuests = new PersonTable(guests);
        tableGuests.setAddTitle(TmmResourceBundle.getString("cast.guest.add"));
        tableGuests.setEditTitle(TmmResourceBundle.getString("cast.guest.edit"));

        JScrollPane scrollPane = new JScrollPane();
        tableGuests.configureScrollPane(scrollPane);
        crewPanel.add(scrollPane, "cell 1 0,grow");
      }
      {
        JLabel lblDirectorsT = new TmmLabel(TmmResourceBundle.getString("metatag.directors"));
        crewPanel.add(lblDirectorsT, "flowy,cell 0 2,alignx right,aligny top");

        tableDirectors = new PersonTable(directors);
        tableDirectors.setAddTitle(TmmResourceBundle.getString("cast.director.add"));
        tableDirectors.setEditTitle(TmmResourceBundle.getString("cast.director.edit"));

        JScrollPane scrollPane = new JScrollPane();
        tableDirectors.configureScrollPane(scrollPane);
        crewPanel.add(scrollPane, "cell 1 2,grow");
      }
      {
        JLabel lblWritersT = new TmmLabel(TmmResourceBundle.getString("metatag.writers"));
        crewPanel.add(lblWritersT, "flowy,cell 3 2,alignx right,aligny top");

        tableWriters = new PersonTable(writers);
        tableWriters.setAddTitle(TmmResourceBundle.getString("cast.writer.add"));
        tableWriters.setEditTitle(TmmResourceBundle.getString("cast.writer.edit"));

        JScrollPane scrollPane = new JScrollPane();
        tableWriters.configureScrollPane(scrollPane);
        crewPanel.add(scrollPane, "cell 4 2,grow");
      }
      {
        JButton btnAddGuest = new SquareIconButton(new AddGuestAction());
        crewPanel.add(btnAddGuest, "cell 0 0,alignx right");
      }
      {
        JButton btnRemoveGuest = new SquareIconButton(new RemoveGuestAction());
        crewPanel.add(btnRemoveGuest, "cell 0 0,alignx right");
      }
      {
        JButton btnMoveGuestUp = new SquareIconButton(new MoveGuestUpAction());
        crewPanel.add(btnMoveGuestUp, "cell 0 0,alignx right");
      }
      {
        JButton btnMoveGuestDown = new SquareIconButton(new MoveGuestDownAction());
        crewPanel.add(btnMoveGuestDown, "cell 0 0,alignx right,aligny top");
      }
      {
        JButton btnAddDirector = new SquareIconButton(new AddDirectorAction());
        crewPanel.add(btnAddDirector, "cell 0 2,alignx right");
      }
      {
        JButton btnRemoveDirector = new SquareIconButton(new RemoveDirectorAction());
        crewPanel.add(btnRemoveDirector, "cell 0 2,alignx right");
      }
      {
        JButton btnMoveDirectorUp = new SquareIconButton(new MoveDirectorUpAction());
        crewPanel.add(btnMoveDirectorUp, "cell 0 2,alignx right");
      }
      {
        JButton btnMoveDirectorDown = new SquareIconButton(new MoveDirectorDownAction());
        crewPanel.add(btnMoveDirectorDown, "cell 0 2,alignx right,aligny top");
      }
      {
        JButton btnAddWriter = new SquareIconButton(new AddWriterAction());
        crewPanel.add(btnAddWriter, "cell 3 2,alignx right");
      }
      {
        JButton btnRemoveWriter = new SquareIconButton(new RemoveWriterAction());
        crewPanel.add(btnRemoveWriter, "cell 3 2,alignx right");
      }
      {
        JButton btnMoveWriterUp = new SquareIconButton(new MoveWriterUpAction());
        crewPanel.add(btnMoveWriterUp, "cell 3 2,alignx right");
      }
      {
        JButton btnMoveWriterDown = new SquareIconButton(new MoveWriterDownAction());
        crewPanel.add(btnMoveWriterDown, "cell 3 2,alignx right,aligny top");
      }
    }

    /**********************************************************************************
     * Media Files panel
     *********************************************************************************/
    {
      mediaFilesPanel = new MediaFileEditorPanel(mediaFiles);
      tabbedPane.addTab(TmmResourceBundle.getString("metatag.mediafiles"), null, mediaFilesPanel, null); // $NON-NLS-1$
    }

    /**********************************************************************************
     * bottom panel
     *********************************************************************************/
    {
      JPanel scrapePanel = new JPanel();
      scrapePanel.setOpaque(false);

      cbScraper = new MediaScraperComboBox(tvShowList.getAvailableMediaScrapers()
          .stream()
          .filter(scraper -> !(scraper.getMediaProvider() instanceof KodiTvShowMetadataProvider))
          .collect(Collectors.toList()));
      MediaScraper defaultScraper = tvShowList.getDefaultMediaScraper();
      scrapePanel.setLayout(new MigLayout("", "[100lp:200lp][][][grow]", "[]"));
      cbScraper.setSelectedItem(defaultScraper);
      scrapePanel.add(cbScraper, "cell 0 0, growx, wmin 0");

      JButton btnScrape = new JButton(new ScrapeAction());
      scrapePanel.add(btnScrape, "cell 1 0");

      JButton btnSearch = new JButton(new SearchAction());
      scrapePanel.add(btnSearch, "cell 2 0");

      setBottomInformationPanel(scrapePanel);
    }
    {
      if (queueSize > 1) {
        JButton abortButton = new JButton(new AbortQueueAction());
        addButton(abortButton);
        if (queueIndex > 0) {
          JButton backButton = new JButton(new NavigateBackAction());
          addButton(backButton);
        }
      }

      JButton cancelButton = new JButton(new DiscardAction());
      cancelButton.addActionListener(e -> mediaFilesPanel.cancelTask());
      addButton(cancelButton);

      JButton okButton = new JButton(new ChangeEpisodeAction());
      okButton.addActionListener(e -> mediaFilesPanel.cancelTask());
      getRootPane().registerKeyboardAction(new ChangeEpisodeAction(), KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
          JComponent.WHEN_IN_FOCUSED_WINDOW);
      getRootPane().registerKeyboardAction(new ChangeEpisodeAction(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
          JComponent.WHEN_IN_FOCUSED_WINDOW);
      addButton(okButton);
    }
  }

  private void updateArtworkUrl(ImageLabel imageLabel, JTextField textField) {
    if (StringUtils.isNotBlank(imageLabel.getImageUrl())) {
      textField.setText(imageLabel.getImageUrl());
    }
  }

  private void cancelScrapeTask() {
    if (scrapeTask != null && !scrapeTask.isDone()) {
      scrapeTask.cancel(true);
    }
  }

  private class ScrapeAction extends AbstractAction {
    ScrapeAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.scrape"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      MediaScraper scraper = (MediaScraper) cbScraper.getSelectedItem();
      cancelScrapeTask();

      scrapeTask = new ScrapeTask(scraper);
      scrapeTask.execute();
    }
  }

  private class SearchAction extends AbstractAction {
    SearchAction() {
      putValue(NAME, TmmResourceBundle.getString("tvshowepisodechooser.search"));
      putValue(SMALL_ICON, IconManager.SEARCH_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      MediaScraper scraper = (MediaScraper) cbScraper.getSelectedItem();
      TvShowEpisodeChooserDialog dialog = new TvShowEpisodeChooserDialog(TvShowEpisodeEditorDialog.this, episodeToEdit, scraper);
      dialog.setLocationRelativeTo(TvShowEpisodeEditorDialog.this);
      dialog.setVisible(true);
      MediaMetadata metadata = dialog.getMetadata();
      if (metadata != null && !metadata.getEpisodeNumbers().isEmpty()) {
        tfTitle.setText(metadata.getTitle());
        tfOriginalTitle.setText(metadata.getOriginalTitle());
        taPlot.setText(metadata.getPlot());

        episodeNumbers.clear();
        for (MediaEpisodeGroup.EpisodeGroupType group : MediaEpisodeGroup.EpisodeGroupType.values()) {
          MediaEpisodeNumber episodeNumber = metadata.getEpisodeNumber(group);
          if (episodeNumber != null && episodeNumber.isValid()) {
            episodeNumbers.add(episodeNumber);
          }
        }

        ids.clear();
        ids.addAll(MediaIdTable.convertIdMapToEventList(metadata.getIds()));

        guests.clear();
        writers.clear();
        directors.clear();

        // force copy constructors here
        for (Person member : metadata.getCastMembers()) {
          switch (member.getType()) {
            case GUEST -> guests.add(new Person(member));
            case ACTOR -> guests.add(new Person(member));
            case DIRECTOR -> directors.add(new Person(member));
            case WRITER -> writers.add(new Person(member));
          }
        }

        MediaArtwork ma = metadata.getMediaArt(MediaArtworkType.THUMB).stream().findFirst().orElse(null);
        if (ma != null) {
          tfThumb.setText(ma.getOriginalUrl());
          lblThumb.setImageUrl(ma.getOriginalUrl());
        }
      }
    }
  }

  private class ChangeEpisodeAction extends AbstractAction {
    ChangeEpisodeAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.ok"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.change"));
      putValue(SMALL_ICON, IconManager.APPLY_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (StringUtils.isBlank(tfTitle.getText())) {
        tfTitle.requestFocusInWindow();
        return;
      }

      cancelScrapeTask();

      // commit all pending changes in this dialog
      commitChanges();

      episodeToEdit.setTitle(tfTitle.getText());
      episodeToEdit.setOriginalTitle(tfOriginalTitle.getText());

      Map<MediaEpisodeGroup, MediaEpisodeNumber> epNumbers = new HashMap<>();
      for (MediaEpisodeNumber episodeNumber : episodeNumbers) {
        if (episodeNumber.containsAnyNumber()) {
          epNumbers.put(episodeNumber.episodeGroup(), episodeNumber);
        }
      }
      episodeToEdit.setEpisodeNumbers(epNumbers);

      episodeToEdit.setPlot(taPlot.getText());
      episodeToEdit.setNote(taNote.getText());

      Object obj = cbMediaSource.getSelectedItem();
      if (obj instanceof MediaSource mediaSource) {
        episodeToEdit.setMediaSource(mediaSource);
      }
      else if (obj instanceof String mediaSource) {
        episodeToEdit.setMediaSource(MediaSource.getMediaSource(mediaSource));
      }
      else {
        episodeToEdit.setMediaSource(MediaSource.UNKNOWN);
      }

      Object episodeEdition = cbEdition.getSelectedItem();
      if (episodeEdition instanceof TvShowEpisodeEdition edition) {
        episodeToEdit.setEdition(edition);
      }
      else if (episodeEdition instanceof String str) {
        episodeToEdit.setEdition(TvShowEpisodeEdition.getTvShowEpisodeEditionStrict(str));
      }
      else {
        episodeToEdit.setEdition(TvShowEpisodeEdition.NONE);
      }

      // sync of media ids
      // first round -> add existing ids
      for (MediaIdTable.MediaId id : ids) {
        // only process non empty ids
        // changed; if empty/0/null value gets set, it is removed in setter ;)
        // if (StringUtils.isAnyBlank(id.key, id.value)) {
        // continue;
        // }
        // first try to cast it into an Integer
        try {
          Integer value = Integer.parseInt(id.value);
          // cool, it is an Integer
          episodeToEdit.setId(id.key, value);
        }
        catch (NumberFormatException ex) {
          // okay, we set it as a String
          episodeToEdit.setId(id.key, id.value);
        }
      }
      // second round -> remove deleted ids
      List<String> removeIds = new ArrayList<>();
      for (Entry<String, Object> entry : episodeToEdit.getIds().entrySet()) {
        MediaIdTable.MediaId id = new MediaIdTable.MediaId(entry.getKey());
        if (!ids.contains(id)) {
          removeIds.add(entry.getKey());
        }
      }
      for (String id : removeIds) {
        // set a null value causes to fire the right events
        episodeToEdit.setId(id, null);
      }

      // sync media files with the media file editor and fire the mediaFiles event
      MediaFileEditorPanel.syncMediaFiles(mediaFiles, episodeToEdit.getMediaFiles());
      episodeToEdit.fireEventForChangedMediaInformation();

      // user rating
      Map<String, MediaRating> ratings = new HashMap<>();

      double userRating = (double) spRating.getValue();
      if (userRating > 0) {
        ratings.put(MediaRating.USER, new MediaRating(MediaRating.USER, userRating, 1, 10));
      }

      // other ratings
      for (MediaRatingTable.Rating mediaRating : TvShowEpisodeEditorDialog.this.ratings) {
        if (StringUtils.isNotBlank(mediaRating.key) && mediaRating.value > 0 && mediaRating.votes > 0) {
          MediaRating rating = new MediaRating(mediaRating.key, mediaRating.value, mediaRating.votes, mediaRating.maxValue);
          ratings.put(mediaRating.key, rating);
        }
      }
      episodeToEdit.setRatings(ratings);

      // if user rating = 0, delete it
      if (userRating == 0) {
        episodeToEdit.removeRating(MediaRating.USER);
      }

      episodeToEdit.setDateAdded((Date) spDateAdded.getValue());
      episodeToEdit.setFirstAired(dpFirstAired.getDate());

      episodeToEdit.setWatched(chckbxWatched.isSelected());

      // remove cast to avoid merging
      episodeToEdit.removeActors();
      episodeToEdit.setActors(guests);
      episodeToEdit.removeDirectors();
      episodeToEdit.setDirectors(directors);
      episodeToEdit.removeWriters();
      episodeToEdit.setWriters(writers);

      // process artwork
      processArtwork(MediaFileType.THUMB, lblThumb, tfThumb);

      episodeToEdit.removeAllTags();
      episodeToEdit.setTags(tags);

      episodeToEdit.writeNFO();
      episodeToEdit.saveToDb();

      setVisible(false);
    }
  }

  private void processArtwork(MediaFileType type, ImageLabel imageLabel, JTextField textField) {
    if (StringUtils.isAllBlank(imageLabel.getImagePath(), imageLabel.getImageUrl())
        && StringUtils.isNotBlank(episodeToEdit.getArtworkFilename(type))) {
      // artwork has been explicitly deleted
      episodeToEdit.deleteMediaFiles(type);
    }

    if (StringUtils.isNotEmpty(textField.getText()) && !textField.getText().equals(episodeToEdit.getArtworkUrl(type))) {
      // artwork url and textfield do not match -> redownload
      episodeToEdit.setArtworkUrl(textField.getText(), type);
      episodeToEdit.downloadArtwork(type);
    }
    else if (StringUtils.isEmpty(textField.getText())) {
      // remove the artwork url
      episodeToEdit.removeArtworkUrl(type);
    }
    else {
      // they match, but check if there is a need to download the artwork
      if (StringUtils.isBlank(episodeToEdit.getArtworkFilename(type))) {
        episodeToEdit.downloadArtwork(type);
      }
    }
  }

  private class DiscardAction extends AbstractAction {
    DiscardAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.cancel"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("edit.discard"));
      putValue(SMALL_ICON, IconManager.CANCEL_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      cancelScrapeTask();

      setVisible(false);
    }
  }

  private class AbortQueueAction extends AbstractAction {
    AbortQueueAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.abortqueue"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.edit.abortqueue.desc"));
      putValue(SMALL_ICON, IconManager.STOP_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      continueQueue = false;
      setVisible(false);
    }
  }

  private class ScrapeTask extends SwingWorker<Void, Void> {
    private final MediaScraper mediaScraper;
    private MediaMetadata      metadata = null;
    private String             message  = null;

    ScrapeTask(MediaScraper mediaScraper) {
      this.mediaScraper = mediaScraper;
    }

    @Override
    protected Void doInBackground() {
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions(episodeToEdit.getTvShow().getIds());
      options.setLanguage(TvShowModuleManager.getInstance().getSettings().getScraperLanguage());

      for (MediaIdTable.MediaId mediaId : ids) {
        options.setId(mediaId.key, mediaId.value);
      }

      options.setId(MediaMetadata.EPISODE_NR, new ArrayList<>(episodeNumbers));
      options.setEpisodeGroup(episodeToEdit.getTvShow().getEpisodeGroup());

      try {
        LOGGER.info("=====================================================");
        LOGGER.info("Scraper metadata with scraper: {}", mediaScraper.getMediaProvider().getProviderInfo().getId());
        LOGGER.info(options.toString());
        LOGGER.info("=====================================================");
        metadata = ((ITvShowMetadataProvider) mediaScraper.getMediaProvider()).getMetadata(options);

        // also inject other ids
        metadata.setId(MediaMetadata.TVSHOW_IDS, options.getTvShowIds());
        MediaIdUtil.injectMissingIds(metadata.getIds(), MediaType.TV_EPISODE);

        // also fill other ratings if ratings are requested
        if (TvShowModuleManager.getInstance().getSettings().isFetchAllRatings()) {
          for (MediaRating rating : ListUtils.nullSafe(RatingProvider.getRatings(metadata.getIds(), MediaType.TV_EPISODE))) {
            if (!metadata.getRatings().contains(rating)) {
              metadata.addRating(rating);
            }
          }
        }
      }
      catch (MissingIdException e) {
        LOGGER.warn("missing id for scrape");
        message = TmmResourceBundle.getString("scraper.error.missingid");
      }
      catch (NothingFoundException ignored) {
        LOGGER.debug("nothing found");
        message = TmmResourceBundle.getString("message.scrape.tvshowepisodefailed");
      }
      catch (Exception e) {
        // other exception
        LOGGER.error("getMetadata", e);
        message = TmmResourceBundle.getString("message.scrape.tvshowepisodefailed");
      }
      finally {
        metadata.removeId(MediaMetadata.TVSHOW_IDS);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }

      return null;
    }

    @Override
    protected void done() {
      super.done();

      if (isCancelled()) {
        return;
      }

      if (StringUtils.isNotBlank(message)) {
        // message
        JOptionPane.showMessageDialog(TvShowEpisodeEditorDialog.this, message);
      }

      // if nothing has been found -> open the search box
      if (metadata != null && StringUtils.isNotBlank(metadata.getTitle())) {
        tfTitle.setText(metadata.getTitle());
        tfOriginalTitle.setText(metadata.getOriginalTitle());
        taPlot.setText(metadata.getPlot());
        dpFirstAired.setDate(metadata.getReleaseDate());

        episodeNumbers.clear();
        for (MediaEpisodeGroup.EpisodeGroupType group : MediaEpisodeGroup.EpisodeGroupType.values()) {
          MediaEpisodeNumber episodeNumber = metadata.getEpisodeNumber(group);
          if (episodeNumber != null && episodeNumber.isValid()) {
            episodeNumbers.add(episodeNumber);
          }
        }

        ratings.clear();
        ratings.addAll(MediaRatingTable.convertRatingMapToEventList(metadata.getRatings()));

        tags.clear();
        tags.addAll(metadata.getTags());

        // cast
        guests.clear();
        directors.clear();
        writers.clear();

        // force copy constructors here
        for (Person member : metadata.getCastMembers()) {
          switch (member.getType()) {
            case ACTOR -> guests.add(new Person(member));
            case GUEST -> guests.add(new Person(member));
            case DIRECTOR -> directors.add(new Person(member));
            case WRITER -> writers.add(new Person(member));
          }
        }

        // artwork
        MediaArtwork ma = metadata.getMediaArt(MediaArtworkType.THUMB).stream().findFirst().orElse(null);
        if (ma != null) {
          lblThumb.setImageUrl(ma.getOriginalUrl());
          tfThumb.setText(ma.getOriginalUrl());
        }
      }
    }
  }

  protected void initDataBindings() {
    JListBinding<String, List<String>, JList> jListBinding = SwingBindings.createJListBinding(UpdateStrategy.READ, tags, listTags);
    jListBinding.bind();
    //
    bindingGroup = new BindingGroup();
    //
    bindingGroup.addBinding(jListBinding);
  }

  @Override
  public void dispose() {
    if (mediaFilesPanel != null) {
      mediaFilesPanel.unbindBindings();
    }
    if (dpFirstAired != null) {
      dpFirstAired.cleanup();
    }

    super.dispose();
  }

  private class AddEpisodeNumberAction extends AbstractAction {
    AddEpisodeNumberAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("episodenumber.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      ModalPopupPanel popupPanel = createModalPopupPanel();
      popupPanel.setTitle(TmmResourceBundle.getString("episodenumber.add"));

      TvShowEpisodeNumberEditorPanel episodeNumberEditorPanel = new TvShowEpisodeNumberEditorPanel(
          new MediaEpisodeNumber(episodeToEdit.getTvShow().getEpisodeGroup(), -1, -1), episodeToEdit.getTvShow().getEpisodeGroups());
      popupPanel.setContent(episodeNumberEditorPanel);
      popupPanel.setOnCloseHandler(() -> {
        MediaEpisodeNumber episodeNumber = episodeNumberEditorPanel.getEpisodeNumber();
        if (episodeNumber != null && episodeNumber.containsAnyNumber()) {
          addOrEditEpisodeNumber(episodeNumber);
        }
      });

      showModalPopupPanel(popupPanel);
    }
  }

  private void addOrEditEpisodeNumber(MediaEpisodeNumber episodeNumber) {
    // remove the old one
    MediaEpisodeNumber existing = episodeNumbers.stream()
        .filter(ep -> ep.episodeGroup().getEpisodeGroupType() == episodeNumber.episodeGroup().getEpisodeGroupType())
        .findFirst()
        .orElse(null);
    if (existing != null) {
      int index = episodeNumbers.indexOf(existing);
      episodeNumbers.remove(existing);
      episodeNumbers.add(index, episodeNumber);
      ids.clear();
    }
    else {
      episodeNumbers.add(episodeNumber);
    }
  }

  private class RemoveEpisodeNumberAction extends AbstractAction {
    RemoveEpisodeNumberAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("episodenumber.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableEpisodeNumbers.getSelectedRow();
      if (row > -1) {
        row = tableEpisodeNumbers.convertRowIndexToModel(row);
        episodeNumbers.remove(row);
      }
    }
  }

  private class AddRatingAction extends AbstractAction {
    AddRatingAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("rating.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      MediaRatingTable.Rating rating = new MediaRatingTable.Rating("");
      // default values
      rating.maxValue = 10;
      rating.votes = 1;

      ModalPopupPanel popupPanel = createModalPopupPanel();
      popupPanel.setTitle(TmmResourceBundle.getString("rating.add"));

      popupPanel.setOnCloseHandler(() -> {
        if (StringUtils.isNotBlank(rating.key) && rating.value > 0 && rating.maxValue > 0 && rating.votes > 0) {
          ratings.add(rating);
        }
      });

      RatingEditorPanel ratingEditorPanel = new RatingEditorPanel(rating);
      popupPanel.setContent(ratingEditorPanel);
      showModalPopupPanel(popupPanel);
    }
  }

  private class RemoveRatingAction extends AbstractAction {
    RemoveRatingAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("rating.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableRatings.getSelectedRow();
      if (row > -1) {
        row = tableRatings.convertRowIndexToModel(row);
        ratings.remove(row);
      }
    }
  }

  private class AddTagAction extends AbstractAction {
    AddTagAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tag.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      Object selectedItem = cbTags.getSelectedItem();

      if (selectedItem == null) {
        return;
      }

      String newTag = (String) selectedItem;
      if (StringUtils.isBlank(newTag)) {
        return;
      }

      // check, if text is selected (from auto-completion), in this case we just
      // remove the selection
      Component editorComponent = cbTags.getEditor().getEditorComponent();
      if (editorComponent instanceof JTextField textField) {
        String selectedText = textField.getSelectedText();
        if (selectedText != null) {
          textField.setSelectionStart(0);
          textField.setSelectionEnd(0);
          textField.setCaretPosition(textField.getText().length());
          return;
        }
      }

      // search if this tag already has been added
      boolean tagFound = false;
      for (String tag : tags) {
        if (tag.equals(newTag)) {
          tagFound = true;
          break;
        }
      }

      // add tag
      if (!tagFound) {
        tags.add(newTag);

        // set text combobox text input to ""
        if (editorComponent instanceof JTextField) {
          cbTagsAutoCompleteSupport.setFirstItem("");
          cbTags.setSelectedIndex(0);
          cbTagsAutoCompleteSupport.removeFirstItem();
        }
      }
    }
  }

  private class RemoveTagAction extends AbstractAction {
    RemoveTagAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tag.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      List<String> selectedTags = listTags.getSelectedValuesList();
      for (String tag : selectedTags) {
        tags.remove(tag);
      }
    }
  }

  private class MoveTagUpAction extends AbstractAction {
    MoveTagUpAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit.movetagup"));
      putValue(SMALL_ICON, IconManager.ARROW_UP_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = listTags.getSelectedIndex();
      if (row > 0) {
        Collections.rotate(tags.subList(row - 1, row + 1), 1);
        listTags.getSelectionModel().setSelectionInterval(row - 1, row - 1);
      }
    }
  }

  private class MoveTagDownAction extends AbstractAction {
    MoveTagDownAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit.movetagdown"));
      putValue(SMALL_ICON, IconManager.ARROW_DOWN_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = listTags.getSelectedIndex();
      if (row < tags.size() - 1) {
        Collections.rotate(tags.subList(row, row + 2), -1);
        listTags.getSelectionModel().setSelectionInterval(row + 1, row + 1);
      }
    }
  }

  private class AddIdAction extends AbstractAction {
    public AddIdAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("id.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      MediaIdTable.MediaId mediaId = new MediaIdTable.MediaId();

      ModalPopupPanel popupPanel = createModalPopupPanel();
      popupPanel.setTitle(TmmResourceBundle.getString("id.add"));

      popupPanel.setOnCloseHandler(() -> {
        if (StringUtils.isNoneBlank(mediaId.key, mediaId.value)) {
          ids.add(mediaId);
        }
      });

      IdEditorPanel idEditorPanel = new IdEditorPanel(mediaId, ScraperType.TV_SHOW);
      popupPanel.setContent(idEditorPanel);
      showModalPopupPanel(popupPanel);
    }
  }

  private class RemoveIdAction extends AbstractAction {
    public RemoveIdAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("id.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableIds.getSelectedRow();
      if (row > -1) {
        row = tableIds.convertRowIndexToModel(row);
        ids.remove(row);
      }
    }
  }

  private class AddGuestAction extends AbstractAction {
    AddGuestAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("cast.guest.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      tableGuests.addPerson(GUEST);
    }
  }

  private class RemoveGuestAction extends AbstractAction {

    RemoveGuestAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("cast.guest.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      guests.removeAll(tableGuests.getSelectedPersons());
    }
  }

  private class MoveGuestUpAction extends AbstractAction {
    MoveGuestUpAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("cast.guest.moveup"));
      putValue(SMALL_ICON, IconManager.ARROW_UP_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableGuests.getSelectedRow();
      if (row > 0) {
        Collections.rotate(guests.subList(row - 1, row + 1), 1);
        tableGuests.getSelectionModel().setSelectionInterval(row - 1, row - 1);
      }
    }
  }

  private class MoveGuestDownAction extends AbstractAction {
    MoveGuestDownAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("cast.guest.movedown"));
      putValue(SMALL_ICON, IconManager.ARROW_DOWN_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableGuests.getSelectedRow();
      if (row < guests.size() - 1) {
        Collections.rotate(guests.subList(row, row + 2), -1);
        tableGuests.getSelectionModel().setSelectionInterval(row + 1, row + 1);
      }
    }
  }

  private class AddDirectorAction extends AbstractAction {
    AddDirectorAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("cast.director.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      tableDirectors.addPerson(Person.Type.DIRECTOR);
    }
  }

  private class RemoveDirectorAction extends AbstractAction {
    RemoveDirectorAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("cast.director.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      directors.removeAll(tableDirectors.getSelectedPersons());
    }
  }

  private class MoveDirectorUpAction extends AbstractAction {
    MoveDirectorUpAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit.movedirectorup"));
      putValue(SMALL_ICON, IconManager.ARROW_UP_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableDirectors.getSelectedRow();
      if (row > 0) {
        Collections.rotate(directors.subList(row - 1, row + 1), 1);
        tableDirectors.getSelectionModel().setSelectionInterval(row - 1, row - 1);
      }
    }
  }

  private class MoveDirectorDownAction extends AbstractAction {
    MoveDirectorDownAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit.movedirectordown"));
      putValue(SMALL_ICON, IconManager.ARROW_DOWN_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableDirectors.getSelectedRow();
      if (row < directors.size() - 1) {
        Collections.rotate(directors.subList(row, row + 2), -1);
        tableDirectors.getSelectionModel().setSelectionInterval(row + 1, row + 1);
      }
    }
  }

  private class AddWriterAction extends AbstractAction {
    AddWriterAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("cast.writer.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      tableWriters.addPerson(Person.Type.WRITER);
    }
  }

  private class RemoveWriterAction extends AbstractAction {
    RemoveWriterAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("cast.writer.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      writers.removeAll(tableWriters.getSelectedPersons());
    }
  }

  private class MoveWriterUpAction extends AbstractAction {
    MoveWriterUpAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit.movewriterup"));
      putValue(SMALL_ICON, IconManager.ARROW_UP_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableWriters.getSelectedRow();
      if (row > 0) {
        Collections.rotate(writers.subList(row - 1, row + 1), 1);
        tableWriters.getSelectionModel().setSelectionInterval(row - 1, row - 1);
      }
    }
  }

  private class MoveWriterDownAction extends AbstractAction {
    MoveWriterDownAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit.movewriterdown"));
      putValue(SMALL_ICON, IconManager.ARROW_DOWN_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableWriters.getSelectedRow();
      if (row < writers.size() - 1) {
        Collections.rotate(writers.subList(row, row + 2), -1);
        tableWriters.getSelectionModel().setSelectionInterval(row + 1, row + 1);
      }
    }
  }
}
