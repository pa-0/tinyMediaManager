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

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.jmte.JmteUtils;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowRenamer;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.thirdparty.trakttv.TvShowSyncTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.MediaRatingTable;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmTabbedPane;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;
import org.tinymediamanager.ui.components.table.NullSelectionModel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.panels.MediaFileSubtitleEditorPanel;
import org.tinymediamanager.ui.panels.ModalPopupPanel;
import org.tinymediamanager.ui.panels.PersonEditorPanel;
import org.tinymediamanager.ui.panels.RatingEditorPanel;

import com.floreysoft.jmte.Engine;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowBulkEditorDialog.
 * 
 * @author Manuel Laggner
 */
public class TvShowBulkEditorDialog extends TmmDialog {
  private final TvShowList                tvShowList      = TvShowModuleManager.getInstance().getTvShowList();
  private final Collection<TvShow>        tvShowsToEdit;
  private final Collection<TvShowEpisode> tvShowEpisodesToEdit;

  private JComboBox<BulkEditorProperty>   cbTvShowProperty;
  private JComboBox<BulkEditorProperty>   cbEpisodeProperty;

  private final EventList<TvShowValues>   tvShowValuesEventList;
  private final EventList<EpisodeValues>  episodeValuesEventList;

  private boolean                         episodesChanged = false;
  private boolean                         tvShowsChanged  = false;

  /**
   * Instantiates a new movie batch editor.
   * 
   * @param tvShows
   *          the tv shows
   * @param episodes
   *          the episodes
   */
  public TvShowBulkEditorDialog(final Collection<TvShow> tvShows, final Collection<TvShowEpisode> episodes) {
    super(TmmResourceBundle.getString("tvshow.bulkedit"), "tvShowBulkEditor");

    tvShowsToEdit = tvShows;
    tvShowEpisodesToEdit = episodes;

    tvShowValuesEventList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(TvShowValues.class)));
    episodeValuesEventList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(EpisodeValues.class)));

    initComponents();

    for (TvShow tvShow : tvShowsToEdit) {
      TvShowValues tvShowValues = new TvShowValues(tvShow);
      tvShowValues.changeProperty(((BulkEditorProperty) cbTvShowProperty.getSelectedItem()).property);
      tvShowValuesEventList.add(tvShowValues);
    }

    for (TvShowEpisode episode : tvShowEpisodesToEdit) {
      EpisodeValues episodeValues = new EpisodeValues(episode);
      episodeValues.changeProperty(((BulkEditorProperty) cbEpisodeProperty.getSelectedItem()).property);
      episodeValuesEventList.add(episodeValues);
    }
  }

  private void initComponents() {
    TmmTabbedPane tabbedPane = new TmmTabbedPane();
    getContentPane().add(tabbedPane, BorderLayout.CENTER);

    {
      /********************
       * TVShow Tab - simple
       ********************/
      JPanel panelContent = new JPanel();
      panelContent.setLayout(new MigLayout("", "[][200lp:350lp,grow][]", "[][][][][][][][]"));
      tabbedPane.add(TmmResourceBundle.getString("metatag.tvshow"), panelContent);
      {
        JLabel lblGenres = new TmmLabel(TmmResourceBundle.getString("metatag.genre"));
        panelContent.add(lblGenres, "cell 0 0,alignx right");

        JComboBox<MediaGenres> cbGenres = new AutocompleteComboBox(MediaGenres.values());
        panelContent.add(cbGenres, "cell 1 0,growx,wmin 0");
        cbGenres.setEditable(true);

        JButton btnAddGenre = new SquareIconButton(IconManager.ADD_INV);
        panelContent.add(btnAddGenre, "flowx,cell 2 0");
        btnAddGenre.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          MediaGenres genre = null;
          Object item = cbGenres.getSelectedItem();

          // genre
          if (item instanceof MediaGenres mediaGenres) {
            genre = mediaGenres;
          }
          else if (item instanceof String string) {
            // newly created genre?
            genre = MediaGenres.getGenre(string);
          }

          if (genre != null) {
            for (TvShow tvShow : tvShowsToEdit) {
              tvShow.addToGenres(Collections.singletonList(genre));
            }
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });

        JButton btnRemoveGenre = new SquareIconButton(IconManager.REMOVE_INV);
        panelContent.add(btnRemoveGenre, "cell 2 0");
        btnRemoveGenre.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          MediaGenres genre = (MediaGenres) cbGenres.getSelectedItem();
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.removeGenre(genre);
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblTags = new TmmLabel(TmmResourceBundle.getString("metatag.tags"));
        panelContent.add(lblTags, "cell 0 1,alignx right");

        JComboBox<String> cbTags = new AutocompleteComboBox<>(ListUtils.asSortedList(tvShowList.getTagsInTvShows()));
        panelContent.add(cbTags, "cell 1 1,growx,wmin 0");
        cbTags.setEditable(true);

        JButton btnAddTag = new SquareIconButton(IconManager.ADD_INV);
        panelContent.add(btnAddTag, "flowx,cell 2 1");
        btnAddTag.addActionListener(e -> {
          tvShowsChanged = true;

          Object selectedItem = cbTags.getSelectedItem();

          if (selectedItem == null) {
            return;
          }

          String tag = (String) selectedItem;
          if (StringUtils.isBlank(tag)) {
            return;
          }

          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.addToTags(Collections.singletonList(tag));
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });

        JButton btnRemoveTag = new SquareIconButton(IconManager.REMOVE_INV);
        panelContent.add(btnRemoveTag, "cell 2 1");
        btnRemoveTag.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          String tag = (String) cbTags.getSelectedItem();
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.removeFromTags(tag);
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblCountry = new TmmLabel(TmmResourceBundle.getString("metatag.country"));
        panelContent.add(lblCountry, "cell 0 2,alignx right");

        JTextField tfCountry = new JTextField();
        panelContent.add(tfCountry, "cell 1 2,growx,wmin 0");

        JButton btnChgCountry = new SquareIconButton(IconManager.APPLY_INV);
        panelContent.add(btnChgCountry, "flowx,cell 2 2");
        btnChgCountry.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.setCountry(tfCountry.getText());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblStudio = new TmmLabel(TmmResourceBundle.getString("metatag.studio"));
        panelContent.add(lblStudio, "cell 0 3,alignx right");

        JTextField tfStudio = new JTextField();
        panelContent.add(tfStudio, "cell 1 3,growx,wmin 0");

        JButton btnChgStudio = new SquareIconButton(IconManager.APPLY_INV);
        panelContent.add(btnChgStudio, "flowx,cell 2 3");
        btnChgStudio.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.setProductionCompany(tfStudio.getText());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblTvShowNoteT = new TmmLabel(TmmResourceBundle.getString("metatag.note"));
        panelContent.add(lblTvShowNoteT, "cell 0 4,alignx right");

        JTextField tfTvShowNote = new JTextField();
        panelContent.add(tfTvShowNote, "cell 1 4,growx");

        JButton btnTvShowNote = new SquareIconButton(IconManager.APPLY_INV);
        btnTvShowNote.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.setNote(tfTvShowNote.getText());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnTvShowNote, "cell 2 4");
      }
      {
        JLabel lblCertificationT = new TmmLabel(TmmResourceBundle.getString("metatag.certification"));
        panelContent.add(lblCertificationT, "cell 0 5, alignx right");

        JComboBox<MediaCertification> cbCertification = new JComboBox<>();
        for (MediaCertification cert : MediaCertification
            .getCertificationsforCountry(TvShowModuleManager.getInstance().getSettings().getCertificationCountry())) {
          cbCertification.addItem(cert);
        }
        panelContent.add(cbCertification, "cell 1 5, growx");

        JButton btnCertification = new SquareIconButton(IconManager.APPLY_INV);
        btnCertification.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          MediaCertification cert = (MediaCertification) cbCertification.getSelectedItem();
          for (TvShow tvshow : tvShowsToEdit) {
            tvshow.setCertification(cert);
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnCertification, "cell 2 5");
      }
      {
        JLabel lblStatus = new TmmLabel(TmmResourceBundle.getString("metatag.status"));
        panelContent.add(lblStatus, "cell 0 6,alignx right");

        JComboBox<MediaAiredStatus> cbStatus = new JComboBox<>();
        for (MediaAiredStatus s : MediaAiredStatus.values()) {
          cbStatus.addItem(s);
        }
        panelContent.add(cbStatus, "cell 1 6,growx");

        JButton btnStatus = new SquareIconButton(IconManager.APPLY_INV);
        panelContent.add(btnStatus, "cell 2 6");
        btnStatus.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          MediaAiredStatus status = (MediaAiredStatus) cbStatus.getSelectedItem();
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.setStatus(status);
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }
      {
        JLabel lblDateAddedT = new TmmLabel(TmmResourceBundle.getString("metatag.dateadded"));
        panelContent.add(lblDateAddedT, "cell 0 7,alignx right");

        JSpinner spDateAdded = new JSpinner(new SpinnerDateModel());
        panelContent.add(spDateAdded, "cell 1 7");

        JButton btnDateAdded = new SquareIconButton(IconManager.APPLY_INV);
        btnDateAdded.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.setDateAdded((Date) spDateAdded.getValue());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnDateAdded, "cell 2 7");
      }
      {
        JButton btnAddRating = new JButton(TmmResourceBundle.getString("rating.add"));
        panelContent.add(btnAddRating, "cell 1 8");
        btnAddRating.addActionListener(e -> {
          tvShowsChanged = true;
          // Open Rating Dialog
          MediaRatingTable.Rating rating = new MediaRatingTable.Rating("");
          rating.maxValue = 10;
          rating.votes = 1;

          ModalPopupPanel popupPanel = createModalPopupPanel();
          popupPanel.setTitle(TmmResourceBundle.getString("rating.add"));

          popupPanel.setOnCloseHandler(() -> {
            if (StringUtils.isNotBlank(rating.key) && rating.value > 0 && rating.maxValue > 0 && rating.votes > 0) {
              setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
              for (TvShow tvShow : tvShowsToEdit) {
                MediaRating mr = new MediaRating(rating.key);
                mr.setVotes(rating.votes);
                mr.setRating(rating.value);
                mr.setMaxValue(rating.maxValue);
                tvShow.setRating(mr);
              }
              setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
          });

          RatingEditorPanel ratingEditorPanel = new RatingEditorPanel(rating);
          popupPanel.setContent(ratingEditorPanel);
          showModalPopupPanel(popupPanel);
        });
      }
    }

    /********************
     * TV show Tab - expert
     ********************/
    {
      JPanel panelContent = new JPanel();

      tabbedPane.addTab(TmmResourceBundle.getString("metatag.tvshow") + " - " + TmmResourceBundle.getString("bulkedit.expert"), panelContent);
      panelContent.setLayout(new MigLayout("", "[][100lp:200lp,grow]", "[grow][20lp!][][50lp][][20lp!][200lp:250lp,grow]"));

      {
        JTextPane tpDescription = new ReadOnlyTextPane(TmmResourceBundle.getString("bulkedit.description"));
        panelContent.add(tpDescription, "cell 0 0 2 1,grow");
      }
      {
        JLabel lblPropertyT = new TmmLabel(TmmResourceBundle.getString("bulkedit.field"));
        panelContent.add(lblPropertyT, "cell 0 2,alignx right");

        cbTvShowProperty = new JComboBox();
        cbTvShowProperty.addItem(new BulkEditorProperty("title", TmmResourceBundle.getString("metatag.title")));
        cbTvShowProperty.addItem(new BulkEditorProperty("originalTitle", TmmResourceBundle.getString("metatag.originaltitle")));
        cbTvShowProperty.addItem(new BulkEditorProperty("sortTitle", TmmResourceBundle.getString("metatag.sorttitle")));
        cbTvShowProperty.addItem(new BulkEditorProperty("plot", TmmResourceBundle.getString("metatag.plot")));
        cbTvShowProperty.addItem(new BulkEditorProperty("productionCompany", TmmResourceBundle.getString("metatag.production")));
        cbTvShowProperty.addItem(new BulkEditorProperty("country", TmmResourceBundle.getString("metatag.country")));
        cbTvShowProperty.addItem(new BulkEditorProperty("note", TmmResourceBundle.getString("metatag.note")));

        cbTvShowProperty.addItemListener(e -> tvShowValuesEventList
            .forEach(tvShowValues -> tvShowValues.changeProperty(((BulkEditorProperty) cbTvShowProperty.getSelectedItem()).property)));
        panelContent.add(cbTvShowProperty, "cell 1 2,growx,wmin 0");
      }

      {
        JLabel lblPatternT = new TmmLabel(TmmResourceBundle.getString("bulkedit.value"));
        panelContent.add(lblPatternT, "cell 0 3,alignx right");

        JTextArea taPattern = new JTextArea();
        taPattern.setBorder(UIManager.getBorder("ScrollPane.border"));
        taPattern.getDocument().addDocumentListener(new DocumentListener() {
          @Override
          public void insertUpdate(DocumentEvent e) {
            changePattern();
          }

          @Override
          public void removeUpdate(DocumentEvent e) {
            changePattern();
          }

          @Override
          public void changedUpdate(DocumentEvent e) {
            changePattern();
          }

          private void changePattern() {
            tvShowValuesEventList.forEach(tvShowValues -> tvShowValues.changePattern(taPattern.getText()));
          }
        });

        taPattern.addFocusListener(new FocusListener() {
          @Override
          public void focusGained(FocusEvent e) {
            taPattern.repaint();
          }

          @Override
          public void focusLost(FocusEvent e) {
            taPattern.repaint();
          }
        });

        panelContent.add(taPattern, "flowx,cell 1 3,wmin 0,grow");
      }
      {
        JButton btnApply = new JButton(TmmResourceBundle.getString("Button.apply"));
        btnApply.setIcon(IconManager.APPLY_INV);
        btnApply.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          tvShowValuesEventList.forEach(TvShowValues::applyValue);
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnApply, "cell 1 4,alignx right");
      }
      {
        TmmTable tableValues = new TmmTable(new TmmTableModel<>(tvShowValuesEventList, new TvShowValuesTableFormat()));
        tableValues.setSelectionModel(new NullSelectionModel());

        JScrollPane scrollPane = new JScrollPane();
        tableValues.configureScrollPane(scrollPane);
        panelContent.add(scrollPane, "cell 0 6 2 1,grow");

        scrollPane.setViewportView(tableValues);
      }
    }

    /********************
     * Episode Tab - simple
     ********************/
    {
      JPanel panelContent = new JPanel();
      panelContent.setLayout(new MigLayout("", "[][200lp:350lp,grow][]", "[][][][][][][][][][][]"));
      tabbedPane.add(TmmResourceBundle.getString("metatag.episode"), panelContent);

      JTextArea textArea = new ReadOnlyTextArea(TmmResourceBundle.getString("tvshow.bulkedit.episodesfromshows"));
      textArea.setWrapStyleWord(true);
      panelContent.add(textArea, "cell 0 0 2 1,wmin 0,grow");

      {
        JLabel lblWatched = new TmmLabel(TmmResourceBundle.getString("metatag.watched"));
        panelContent.add(lblWatched, "cell 0 1,alignx right");

        JCheckBox chckbxWatched = new JCheckBox("");
        panelContent.add(chckbxWatched, "cell 1 1");

        JButton btnWatched = new SquareIconButton(IconManager.APPLY_INV);
        panelContent.add(btnWatched, "cell 2 1");
        btnWatched.addActionListener(e -> {
          episodesChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShowEpisode episode : tvShowEpisodesToEdit) {
            if (chckbxWatched.isSelected()) {
              // set the watched flag along with playcount = 1 and lastplayed = now
              episode.setWatched(true);
              if (episode.getPlaycount() == 0) {
                episode.setPlaycount(1);
                episode.setLastWatched(new Date());
              }
            }
            else {
              episode.setWatched(false);
              episode.setPlaycount(0);
              episode.setLastWatched(null);
            }
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblSeason = new TmmLabel(TmmResourceBundle.getString("metatag.season"));
        panelContent.add(lblSeason, "cell 0 2,alignx right");

        JComboBox<MediaEpisodeGroup.EpisodeGroupType> cbEpisodeGroup = new JComboBox(MediaEpisodeGroup.EpisodeGroupType.values());
        panelContent.add(cbEpisodeGroup, "cell 1 2");

        JSpinner spSeason = new JSpinner();
        spSeason.setModel(new SpinnerNumberModel(0, -1, 9999, 1));
        panelContent.add(spSeason, "cell 1 2");

        JButton btnSeason = new SquareIconButton(IconManager.APPLY_INV);
        panelContent.add(btnSeason, "cell 2 2");
        btnSeason.addActionListener(arg0 -> {
          episodesChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShowEpisode episode : tvShowEpisodesToEdit) {
            Integer season = (Integer) spSeason.getValue();
            MediaEpisodeGroup.EpisodeGroupType eg = (MediaEpisodeGroup.EpisodeGroupType) cbEpisodeGroup.getSelectedItem();
            MediaEpisodeGroup episodeGroup = null;

            for (MediaEpisodeGroup meg : episode.getTvShow().getEpisodeGroups()) {
              if (meg.getEpisodeGroupType() == eg) {
                episodeGroup = meg;
                break;
              }
            }

            if (episodeGroup != null) {
              MediaEpisodeNumber existingEpisodeNumber = episode.getEpisodeNumber(episodeGroup);
              if (existingEpisodeNumber != null) {
                episode.setEpisode(new MediaEpisodeNumber(episodeGroup, season, existingEpisodeNumber.episode()));
              }
              else {
                episode.setEpisode(new MediaEpisodeNumber(episodeGroup, season, -1));
              }
            }
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblTagsEpisode = new TmmLabel(TmmResourceBundle.getString("metatag.tags"));
        panelContent.add(lblTagsEpisode, "cell 0 5,alignx right");

        JComboBox<String> cbTagsEpisode = new AutocompleteComboBox(tvShowList.getTagsInEpisodes().toArray());
        panelContent.add(cbTagsEpisode, "cell 1 5,growx,wmin 0");
        cbTagsEpisode.setEditable(true);

        JButton btnAddTagEpisode = new SquareIconButton(IconManager.ADD_INV);
        panelContent.add(btnAddTagEpisode, "flowx,cell 2 5");
        btnAddTagEpisode.addActionListener(e -> {
          episodesChanged = true;

          Object selectedItem = cbTagsEpisode.getSelectedItem();

          if (selectedItem == null) {
            return;
          }

          String tag = (String) selectedItem;
          if (StringUtils.isBlank(tag)) {
            return;
          }

          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShowEpisode episode : tvShowEpisodesToEdit) {
            episode.addToTags(Collections.singletonList(tag));
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });

        JButton btnRemoveTagEpisode = new SquareIconButton(IconManager.REMOVE_INV);
        panelContent.add(btnRemoveTagEpisode, "cell 2 5");
        btnRemoveTagEpisode.addActionListener(e -> {
          episodesChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          String tag = (String) cbTagsEpisode.getSelectedItem();
          for (TvShowEpisode episode : tvShowEpisodesToEdit) {
            episode.removeFromTags(tag);
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblMediasourceEpisode = new TmmLabel(TmmResourceBundle.getString("metatag.source"));
        panelContent.add(lblMediasourceEpisode, "cell 0 6,alignx right");

        JComboBox<MediaSource> cbMediaSourceEpisode = new JComboBox(MediaSource.values());
        panelContent.add(cbMediaSourceEpisode, "cell 1 6,growx,wmin 0");

        JButton btnMediaSourceEpisode = new SquareIconButton(IconManager.APPLY_INV);
        panelContent.add(btnMediaSourceEpisode, "cell 2 6");
        btnMediaSourceEpisode.addActionListener(e -> {
          episodesChanged = true;
          Object obj = cbMediaSourceEpisode.getSelectedItem();
          if (obj instanceof MediaSource mediaSource) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            for (TvShowEpisode episode : tvShowEpisodesToEdit) {
              episode.setMediaSource(mediaSource);
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }
        });
      }

      {
        JLabel lblEpisodeNoteT = new TmmLabel(TmmResourceBundle.getString("metatag.note"));
        panelContent.add(lblEpisodeNoteT, "cell 0 7,alignx right");

        JTextField tfEpisodeNote = new JTextField();
        panelContent.add(tfEpisodeNote, "cell 1 7,growx");

        JButton btnEpisodeNote = new SquareIconButton(IconManager.APPLY_INV);
        btnEpisodeNote.addActionListener(e -> {
          episodesChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShowEpisode episode : tvShowEpisodesToEdit) {
            episode.setNote(tfEpisodeNote.getText());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnEpisodeNote, "cell 2 7");
      }
      {
        JLabel lblEpisodePlotT = new TmmLabel(TmmResourceBundle.getString("metatag.plot"));
        panelContent.add(lblEpisodePlotT, "cell 0 8,alignx right");

        JTextField tfEpisodePlot = new JTextField();
        panelContent.add(tfEpisodePlot, "cell 1 8,growx");

        JButton btnEpisodePlot = new SquareIconButton(IconManager.APPLY_INV);
        btnEpisodePlot.addActionListener(e -> {
          episodesChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShowEpisode episode : tvShowEpisodesToEdit) {
            episode.setPlot(tfEpisodePlot.getText());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnEpisodePlot, "cell 2 8");
      }
      {
        JLabel lblDateAdded = new TmmLabel(TmmResourceBundle.getString("metatag.dateadded"));
        panelContent.add(lblDateAdded, "cell 0 9,alignx right");

        JSpinner spDateAdded = new JSpinner(new SpinnerDateModel());
        panelContent.add(spDateAdded, "cell 1 9");

        JButton btnDateAdded = new SquareIconButton(IconManager.APPLY_INV);
        btnDateAdded.addActionListener(e -> {
          episodesChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShowEpisode episode : tvShowEpisodesToEdit) {
            episode.setDateAdded((Date) spDateAdded.getValue());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnDateAdded, "cell 2 9");

      }
      {
        JButton btnAddActors = new JButton(TmmResourceBundle.getString("cast.actor.add"));
        panelContent.add(btnAddActors, "cell 1 10");
        btnAddActors.addActionListener(e -> {
          episodesChanged = true;
          // Open Person Dialog
          Person person = new Person(Person.Type.ACTOR, TmmResourceBundle.getString("cast.actor.unknown"),
              TmmResourceBundle.getString("cast.role.unknown"));

          ModalPopupPanel popupPanel = createModalPopupPanel();
          popupPanel.setTitle(TmmResourceBundle.getString("cast.actor.add"));

          popupPanel.setOnCloseHandler(() -> {
            if (StringUtils.isNotBlank(person.getName()) && !person.getName().equals(TmmResourceBundle.getString("cast.actor.unknown"))) {
              if (person.getRole().equals(TmmResourceBundle.getString("cast.role.unknown"))) {
                person.setRole("");
              }

              setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
              for (TvShowEpisode episode : tvShowEpisodesToEdit) {

                List<Person> actors = new ArrayList<>();
                actors.add(new Person(person)); // force copy constructor
                episode.addToActors(actors);
              }
              setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
          });

          PersonEditorPanel personEditorPanel = new PersonEditorPanel(person);
          popupPanel.setContent(personEditorPanel);
          showModalPopupPanel(popupPanel);
        });

      }
      {
        JButton btnAddDirectors = new JButton(TmmResourceBundle.getString("cast.director.add"));
        panelContent.add(btnAddDirectors, "cell 1 10");
        btnAddDirectors.addActionListener(e -> {
          episodesChanged = true;
          // Open Director Dialog
          Person person = new Person(Person.Type.DIRECTOR, TmmResourceBundle.getString("director.name.unknown"), "Director");

          ModalPopupPanel popupPanel = createModalPopupPanel();
          popupPanel.setTitle(TmmResourceBundle.getString("cast.director.add"));

          popupPanel.setOnCloseHandler(() -> {
            if (StringUtils.isNotBlank(person.getName()) && !person.getName().equals(TmmResourceBundle.getString("director.name.unknown"))) {
              if (person.getRole().equals(TmmResourceBundle.getString("cast.role.unknown"))) {
                person.setRole("");
              }

              setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
              for (TvShowEpisode episode : tvShowEpisodesToEdit) {

                List<Person> directors = new ArrayList<>();
                directors.add(new Person(person)); // force copy constructor
                episode.addToDirectors(directors);
              }
              setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
          });

          PersonEditorPanel personEditorPanel = new PersonEditorPanel(person);
          popupPanel.setContent(personEditorPanel);
          showModalPopupPanel(popupPanel);
        });
      }

      {
        JButton btnAddRating = new JButton(TmmResourceBundle.getString("rating.add"));
        panelContent.add(btnAddRating, "cell 1 11");
        btnAddRating.addActionListener(e -> {
          // Open Rating Dialog
          MediaRatingTable.Rating rating = new MediaRatingTable.Rating("");
          rating.maxValue = 10;
          rating.votes = 1;

          ModalPopupPanel popupPanel = createModalPopupPanel();
          popupPanel.setTitle(TmmResourceBundle.getString("rating.add"));

          popupPanel.setOnCloseHandler(() -> {
            if (StringUtils.isNotBlank(rating.key) && rating.value > 0 && rating.maxValue > 0 && rating.votes > 0) {
              setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
              for (TvShowEpisode episode : tvShowEpisodesToEdit) {
                MediaRating mr = new MediaRating(rating.key);
                mr.setVotes(rating.votes);
                mr.setRating(rating.value);
                mr.setMaxValue(rating.maxValue);
                episode.setRating(mr);
              }
              setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
          });

          RatingEditorPanel ratingEditorPanel = new RatingEditorPanel(rating);
          popupPanel.setContent(ratingEditorPanel);
          showModalPopupPanel(popupPanel);
        });
      }
      {
        JButton btnAddSubtitle = new JButton(TmmResourceBundle.getString("subtitle.add"));
        panelContent.add(btnAddSubtitle, "cell 1 11");
        btnAddSubtitle.addActionListener(e -> {
          MediaFileSubtitle subtitle = new MediaFileSubtitle();

          ModalPopupPanel popupPanel = createModalPopupPanel();
          popupPanel.setTitle(TmmResourceBundle.getString("subtitle.add"));

          popupPanel.setOnCloseHandler(() -> {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            for (TvShowEpisode episode : tvShowEpisodesToEdit) {
              episode.getMainFile().addSubtitle(subtitle);
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          });

          MediaFileSubtitleEditorPanel subtitleEditorPanel = new MediaFileSubtitleEditorPanel(subtitle);
          popupPanel.setContent(subtitleEditorPanel);
          showModalPopupPanel(popupPanel);
        });
      }

      {
        JButton btnClose = new JButton(TmmResourceBundle.getString("Button.close"));
        btnClose.setIcon(IconManager.APPLY_INV);
        btnClose.addActionListener(arg0 -> {
          // rewrite tv show if anything changed
          if (tvShowsChanged) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            for (TvShow tvShow : tvShowsToEdit) {
              tvShow.writeNFO();
              tvShow.saveToDb();
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }

          // rewrite episodes if anything changed
          if (episodesChanged) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            for (TvShowEpisode episode : tvShowEpisodesToEdit) {
              episode.writeNFO();
              episode.saveToDb();
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }

          if (TvShowModuleManager.getInstance().getSettings().getSyncTrakt()) {
            Set<TvShow> tvShows1 = new HashSet<>();
            for (TvShowEpisode episode : tvShowEpisodesToEdit) {
              tvShows1.add(episode.getTvShow());
            }
            tvShows1.addAll(tvShowsToEdit);
            TvShowSyncTraktTvTask task = new TvShowSyncTraktTvTask(new ArrayList<>(tvShows1));
            task.setSyncCollection(TvShowModuleManager.getInstance().getSettings().getSyncTraktCollection());
            task.setSyncWatched(TvShowModuleManager.getInstance().getSettings().getSyncTraktWatched());
            task.setSyncRating(TvShowModuleManager.getInstance().getSettings().getSyncTraktRating());

            TmmTaskManager.getInstance().addUnnamedTask(task);
          }

          setVisible(false);
        });
        addDefaultButton(btnClose);

        // add window listener to write changes (if the window close button "X" is pressed)
        addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            // rewrite tv show if anything changed
            if (tvShowsChanged) {
              setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
              for (TvShow tvShow : tvShowsToEdit) {
                tvShow.writeNFO();
                tvShow.saveToDb();
              }
              setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }

            // rewrite episodes if anything changed
            if (episodesChanged) {
              setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
              for (TvShowEpisode episode : tvShowEpisodesToEdit) {
                episode.writeNFO();
                episode.saveToDb();
              }
              setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
          }
        });
      }
    }

    /********************
     * Episode Tab - expert
     ********************/
    {
      JPanel panelContent = new JPanel();

      tabbedPane.addTab(TmmResourceBundle.getString("metatag.episode") + " - " + TmmResourceBundle.getString("bulkedit.expert"), panelContent);
      panelContent.setLayout(new MigLayout("", "[][100lp:200lp,grow]", "[grow][20lp!][][50lp][][20lp!][200lp:250lp,grow]"));

      {
        JTextPane tpDescription = new ReadOnlyTextPane(TmmResourceBundle.getString("bulkedit.description"));
        panelContent.add(tpDescription, "cell 0 0 2 1,grow");
      }
      {
        JLabel lblPropertyT = new TmmLabel(TmmResourceBundle.getString("bulkedit.field"));
        panelContent.add(lblPropertyT, "cell 0 2,alignx right");

        cbEpisodeProperty = new JComboBox();
        cbEpisodeProperty.addItem(new BulkEditorProperty("title", TmmResourceBundle.getString("metatag.title")));
        cbEpisodeProperty.addItem(new BulkEditorProperty("originalTitle", TmmResourceBundle.getString("metatag.originaltitle")));
        cbEpisodeProperty.addItem(new BulkEditorProperty("sortTitle", TmmResourceBundle.getString("metatag.sorttitle")));
        cbEpisodeProperty.addItem(new BulkEditorProperty("tagline", TmmResourceBundle.getString("metatag.tagline")));
        cbEpisodeProperty.addItem(new BulkEditorProperty("plot", TmmResourceBundle.getString("metatag.plot")));
        cbEpisodeProperty.addItem(new BulkEditorProperty("productionCompany", TmmResourceBundle.getString("metatag.production")));
        cbEpisodeProperty.addItem(new BulkEditorProperty("spokenLanguages", TmmResourceBundle.getString("metatag.spokenlanguages")));
        cbEpisodeProperty.addItem(new BulkEditorProperty("country", TmmResourceBundle.getString("metatag.country")));
        cbEpisodeProperty.addItem(new BulkEditorProperty("note", TmmResourceBundle.getString("metatag.note")));
        cbEpisodeProperty.addItem(new BulkEditorProperty("originalFilename", TmmResourceBundle.getString("metatag.originalfile")));

        cbEpisodeProperty.addItemListener(e -> episodeValuesEventList
            .forEach(episodeValues -> episodeValues.changeProperty(((BulkEditorProperty) cbEpisodeProperty.getSelectedItem()).property)));
        panelContent.add(cbEpisodeProperty, "cell 1 2,growx,wmin 0");
      }

      {
        JLabel lblPatternT = new TmmLabel(TmmResourceBundle.getString("bulkedit.value"));
        panelContent.add(lblPatternT, "cell 0 3,alignx right");

        JTextArea taPattern = new JTextArea();
        taPattern.setBorder(UIManager.getBorder("ScrollPane.border"));
        taPattern.getDocument().addDocumentListener(new DocumentListener() {
          @Override
          public void insertUpdate(DocumentEvent e) {
            changePattern();
          }

          @Override
          public void removeUpdate(DocumentEvent e) {
            changePattern();
          }

          @Override
          public void changedUpdate(DocumentEvent e) {
            changePattern();
          }

          private void changePattern() {
            episodeValuesEventList.forEach(episodeValues -> episodeValues.changePattern(taPattern.getText()));
          }
        });

        taPattern.addFocusListener(new FocusListener() {
          @Override
          public void focusGained(FocusEvent e) {
            taPattern.repaint();
          }

          @Override
          public void focusLost(FocusEvent e) {
            taPattern.repaint();
          }
        });

        panelContent.add(taPattern, "flowx,cell 1 3,wmin 0,grow");
      }
      {
        JButton btnApply = new JButton(TmmResourceBundle.getString("Button.apply"));
        btnApply.setIcon(IconManager.APPLY_INV);
        btnApply.addActionListener(e -> {
          episodesChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          episodeValuesEventList.forEach(EpisodeValues::applyValue);
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnApply, "cell 1 4,alignx right");
      }
      {
        TmmTable tableValues = new TmmTable(new TmmTableModel<>(episodeValuesEventList, new EpisodeValuesTableFormat()));
        tableValues.setSelectionModel(new NullSelectionModel());

        JScrollPane scrollPane = new JScrollPane();
        tableValues.configureScrollPane(scrollPane);
        panelContent.add(scrollPane, "cell 0 6 2 1,grow");

        scrollPane.setViewportView(tableValues);
      }
    }
  }

  private record BulkEditorProperty(String property, String description) {

    @Override
    public String toString() {
      return description;
    }
  }

  private static class TvShowValues extends AbstractModelObject {
    private final TvShow tvShow;

    private String       property;
    private String       propertyValue = "";
    private String       patternValue  = "";

    public TvShowValues(TvShow tvShow) {
      this.tvShow = tvShow;
    }

    public void changeProperty(String property) {
      this.property = property;
      String oldValue = propertyValue;
      propertyValue = "";

      try {
        PropertyDescriptor descriptor = new PropertyDescriptor(property, Movie.class);
        Object value = descriptor.getReadMethod().invoke(tvShow);
        if (value != null) {
          propertyValue = value.toString();
        }
      }
      catch (Exception e) {
        // just do nothing
      }

      firePropertyChange("propertyValue", oldValue, propertyValue);
    }

    public void changePattern(String pattern) {
      String oldValue = patternValue;
      patternValue = getPatternValue(pattern);

      firePropertyChange("patternValue", oldValue, patternValue);
    }

    private String getPatternValue(String pattern) {
      try {
        Engine engine = TvShowRenamer.createEngine();
        Map<String, Object> root = new HashMap<>();
        root.put("tvShow", tvShow);
        return engine.transform(JmteUtils.morphTemplate(pattern, TvShowRenamer.getTokenMap()), root);
      }
      catch (Exception e) {
        return pattern;
      }
    }

    public void applyValue() {
      try {
        // set the property in the movie
        PropertyDescriptor descriptor = new PropertyDescriptor(property, TvShow.class);
        descriptor.getWriteMethod().invoke(tvShow, patternValue);

        // and update old value
        String oldValue = propertyValue;
        propertyValue = patternValue;
        firePropertyChange("propertyValue", oldValue, propertyValue);
      }
      catch (Exception e) {
        // just do nothing
      }
    }

    public String getTvShowTitle() {
      return tvShow.getTitle();
    }

    public String getPropertyValue() {
      return propertyValue;
    }

    public String getPatternValue() {
      return patternValue;
    }
  }

  private static class TvShowValuesTableFormat extends TmmTableFormat<TvShowValues> {
    public TvShowValuesTableFormat() {
      /*
       * TV show title
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.title"), "title", TvShowValues::getTvShowTitle, String.class);
      addColumn(col);

      /*
       * old value
       */
      col = new Column(TmmResourceBundle.getString("bulkedit.oldvalue"), "oldValue", TvShowValues::getPropertyValue, String.class);
      addColumn(col);

      /*
       * new value
       */
      col = new Column(TmmResourceBundle.getString("bulkedit.newvalue"), "newValue", TvShowValues::getPatternValue, String.class);
      addColumn(col);
    }
  }

  private static class EpisodeValues extends AbstractModelObject {
    private final TvShowEpisode episode;

    private String              property;
    private String              propertyValue = "";
    private String              patternValue  = "";

    public EpisodeValues(TvShowEpisode tvShowEpisode) {
      this.episode = tvShowEpisode;
    }

    public void changeProperty(String property) {
      this.property = property;
      String oldValue = propertyValue;
      propertyValue = "";

      try {
        PropertyDescriptor descriptor = new PropertyDescriptor(property, TvShowEpisode.class);
        Object value = descriptor.getReadMethod().invoke(episode);
        if (value != null) {
          propertyValue = value.toString();
        }
      }
      catch (Exception e) {
        // just do nothing
      }

      firePropertyChange("propertyValue", oldValue, propertyValue);
    }

    public void changePattern(String pattern) {
      String oldValue = patternValue;
      patternValue = getPatternValue(pattern);

      firePropertyChange("patternValue", oldValue, patternValue);
    }

    private String getPatternValue(String pattern) {
      try {
        Engine engine = MovieRenamer.createEngine();
        Map<String, Object> root = new HashMap<>();
        root.put("tvShow", episode.getTvShow());
        root.put("episode", episode);
        return engine.transform(JmteUtils.morphTemplate(pattern, TvShowRenamer.getTokenMap()), root);
      }
      catch (Exception e) {
        return pattern;
      }
    }

    public void applyValue() {
      try {
        // set the property in the movie
        PropertyDescriptor descriptor = new PropertyDescriptor(property, TvShowEpisode.class);
        descriptor.getWriteMethod().invoke(episode, patternValue);

        // and update old value
        String oldValue = propertyValue;
        propertyValue = patternValue;
        firePropertyChange("propertyValue", oldValue, propertyValue);
      }
      catch (Exception e) {
        // just do nothing
      }
    }

    public String getEpisodeTitle() {
      return String.format("S%2dE%2d - %s", episode.getSeason(), episode.getEpisode(), episode.getTitle());
    }

    public String getPropertyValue() {
      return propertyValue;
    }

    public String getPatternValue() {
      return patternValue;
    }
  }

  private static class EpisodeValuesTableFormat extends TmmTableFormat<EpisodeValues> {
    public EpisodeValuesTableFormat() {
      /*
       * episode show title
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.title"), "title", EpisodeValues::getEpisodeTitle, String.class);
      addColumn(col);

      /*
       * old value
       */
      col = new Column(TmmResourceBundle.getString("bulkedit.oldvalue"), "oldValue", EpisodeValues::getPropertyValue, String.class);
      addColumn(col);

      /*
       * new value
       */
      col = new Column(TmmResourceBundle.getString("bulkedit.newvalue"), "newValue", EpisodeValues::getPatternValue, String.class);
      addColumn(col);
    }
  }
}
