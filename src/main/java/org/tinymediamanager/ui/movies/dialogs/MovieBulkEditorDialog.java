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
import java.awt.Cursor;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SpinnerDateModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.jmte.JmteUtils;
import org.tinymediamanager.core.movie.MovieEdition;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.thirdparty.trakttv.MovieSyncTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmTabbedPane;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;
import org.tinymediamanager.ui.components.datepicker.YearSpinner;
import org.tinymediamanager.ui.components.table.NullSelectionModel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.moviesets.actions.MovieSetAddAction;

import com.floreysoft.jmte.Engine;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import net.miginfocom.swing.MigLayout;

/**
 * The class MovieBulkEditor is used for bulk editing movies
 * 
 * @author Manuel Laggner
 */
public class MovieBulkEditorDialog extends TmmDialog {
  private final MovieList                movieList    = MovieModuleManager.getInstance().getMovieList();
  private final List<Movie>              moviesToEdit = new ArrayList<>();

  private boolean                        changed      = false;

  private final JComboBox                cbMovieSet;
  private final JComboBox<MovieProperty> cbProperty;

  private final EventList<MovieValues>   movieValuesEventList;

  /**
   * Instantiates a new movie batch editor.
   * 
   * @param movies
   *          the movies
   */
  public MovieBulkEditorDialog(final List<Movie> movies) {
    super(TmmResourceBundle.getString("movie.edit"), "movieBulkEditor");

    movieValuesEventList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(MovieValues.class)));

    TmmTabbedPane tabbedPane = new TmmTabbedPane();
    getContentPane().add(tabbedPane, BorderLayout.CENTER);
    {
      JPanel panelContent = new JPanel();
      panelContent.setLayout(new MigLayout("", "[20lp:n][200lp:350lp,grow][][][]", "[][][][][][][][][][][][][][]"));

      {
        JLabel lblYearT = new TmmLabel(TmmResourceBundle.getString("metatag.year"));
        panelContent.add(lblYearT, "cell 0 0,alignx right");

        JSpinner spYear = new YearSpinner();
        panelContent.add(spYear, "cell 1 0");

        JButton btnYear = new SquareIconButton(IconManager.APPLY_INV);
        btnYear.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (Movie movie : moviesToEdit) {
            movie.setYear((Integer) spYear.getValue());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnYear, "cell 2 0");
      }
      {
        JLabel lblGenresT = new TmmLabel(TmmResourceBundle.getString("metatag.genre"));
        panelContent.add(lblGenresT, "cell 0 1,alignx right");
        tabbedPane.addTab(TmmResourceBundle.getString("bulkedit.basic"), panelContent);

        JComboBox cbGenres = new AutocompleteComboBox(MediaGenres.values());
        cbGenres.setEditable(true);
        panelContent.add(cbGenres, "cell 1 1,growx,wmin 0");

        JButton btnAddGenre = new SquareIconButton(IconManager.ADD_INV);
        btnAddGenre.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          MediaGenres genre = null;
          Object item = cbGenres.getSelectedItem();

          // genre
          if (item instanceof MediaGenres) {
            genre = (MediaGenres) item;
          }

          // newly created genre?
          if (item instanceof String) {
            genre = MediaGenres.getGenre((String) item);
          }

          if (genre != null) {
            for (Movie movie : moviesToEdit) {
              movie.addToGenres(Collections.singletonList(genre));
            }
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnAddGenre, "cell 2 1");

        JButton btnRemoveGenre = new SquareIconButton(IconManager.REMOVE_INV);
        btnRemoveGenre.addActionListener(e -> {
          changed = true;
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
            for (Movie movie : moviesToEdit) {
              movie.removeGenre(genre);
            }
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnRemoveGenre, "cell 3 1");

        JButton btnRemoveAllGenres = new SquareIconButton(IconManager.DELETE);
        btnRemoveAllGenres.addActionListener(e -> {
          if (isDeleteConfirmed(TmmResourceBundle.getString("metatag.genre"))) {
            changed = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            for (Movie movie : moviesToEdit) {
              movie.removeAllGenres();
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }
        });
        panelContent.add(btnRemoveAllGenres, "cell 4 1");
      }
      {
        JLabel lblTagsT = new TmmLabel(TmmResourceBundle.getString("metatag.tags"));
        panelContent.add(lblTagsT, "cell 0 2,alignx right");

        JComboBox cbTags = new AutocompleteComboBox(ListUtils.asSortedList(movieList.getTagsInMovies()));
        cbTags.setEditable(true);
        panelContent.add(cbTags, "cell 1 2,growx,wmin 0");

        JButton btnAddTag = new SquareIconButton(IconManager.ADD_INV);
        btnAddTag.addActionListener(e -> {
          changed = true;

          Object selectedItem = cbTags.getSelectedItem();

          if (selectedItem == null) {
            return;
          }

          String tag = (String) selectedItem;
          if (StringUtils.isBlank(tag)) {
            return;
          }

          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (Movie movie : moviesToEdit) {
            movie.addToTags(Collections.singletonList(tag));
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnAddTag, "cell 2 2");

        JButton btnRemoveTag = new SquareIconButton(IconManager.REMOVE_INV);
        btnRemoveTag.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          String tag = (String) cbTags.getSelectedItem();
          for (Movie movie : moviesToEdit) {
            movie.removeFromTags(tag);
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnRemoveTag, "cell 3 2");

        JButton btnRemoveAllTags = new SquareIconButton(IconManager.DELETE);
        btnRemoveAllTags.addActionListener(e -> {
          if (isDeleteConfirmed(TmmResourceBundle.getString("metatag.tags"))) {
            changed = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            for (Movie movie : moviesToEdit) {
              movie.removeAllTags();
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }
        });
        panelContent.add(btnRemoveAllTags, "cell 4 2");
      }
      {
        JLabel lblEditionT = new TmmLabel(TmmResourceBundle.getString("metatag.edition"));
        panelContent.add(lblEditionT, "cell 0 3,alignx right");

        JComboBox cbEdition = new AutocompleteComboBox(MovieEdition.values());
        panelContent.add(cbEdition, "cell 1 3,growx");

        JButton btnMovieEdition = new SquareIconButton(IconManager.APPLY_INV);
        btnMovieEdition.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          MovieEdition edition = null;
          Object item = cbEdition.getSelectedItem();

          // edition
          if (item instanceof MovieEdition movieEdition) {
            edition = movieEdition;
          }
          else if (item instanceof String string) {
            // newly created edition?
            edition = MovieEdition.getMovieEdition(string);
          }

          if (edition != null) {
            for (Movie movie : moviesToEdit) {
              movie.setEdition(edition);
            }
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnMovieEdition, "cell 2 3");
      }
      {
        JLabel lblCertificationT = new TmmLabel(TmmResourceBundle.getString("metatag.certification"));
        panelContent.add(lblCertificationT, "cell 0 4,alignx right");

        final JComboBox cbCertification = new JComboBox();
        for (MediaCertification cert : MediaCertification
            .getCertificationsforCountry(MovieModuleManager.getInstance().getSettings().getCertificationCountry())) {
          cbCertification.addItem(cert);
        }
        panelContent.add(cbCertification, "cell 1 4,growx");

        JButton btnCertification = new SquareIconButton(IconManager.APPLY_INV);
        btnCertification.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          MediaCertification cert = (MediaCertification) cbCertification.getSelectedItem();
          for (Movie movie : moviesToEdit) {
            movie.setCertification(cert);

          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnCertification, "cell 2 4");
      }
      {
        JLabel lblMovieSetT = new TmmLabel(TmmResourceBundle.getString("metatag.movieset"));
        panelContent.add(lblMovieSetT, "cell 0 5,alignx right");

        cbMovieSet = new JComboBox();
        panelContent.add(cbMovieSet, "cell 1 5,growx,wmin 0");

        JButton btnSetMovieSet = new SquareIconButton(IconManager.APPLY_INV);
        btnSetMovieSet.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          // movie set
          Object obj = cbMovieSet.getSelectedItem();
          for (Movie movie : moviesToEdit) {
            if (obj instanceof String) {
              movie.removeFromMovieSet();
            }
            else if (obj instanceof MovieSet movieSet) {
              if (movie.getMovieSet() != movieSet) {
                movie.removeFromMovieSet();
                movie.setMovieSet(movieSet);
                movieSet.insertMovie(movie);
              }
            }
          }

          if (obj instanceof MovieSet movieSet) {
            movieSet.saveToDb();
          }

          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnSetMovieSet, "cell 2 5");

        JButton btnNewMovieset = new JButton(new MovieSetAddAction());
        panelContent.add(btnNewMovieset, "cell 3 5 2 1,growx");
      }
      {
        JLabel lblWatchedT = new TmmLabel(TmmResourceBundle.getString("metatag.watched"));
        panelContent.add(lblWatchedT, "cell 0 6,alignx right");

        JCheckBox chckbxWatched = new JCheckBox("");
        panelContent.add(chckbxWatched, "cell 1 6,aligny top");

        JButton btnWatched = new SquareIconButton(IconManager.APPLY_INV);
        btnWatched.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (Movie movie : moviesToEdit) {
            if (chckbxWatched.isSelected()) {
              // set the watched flag along with playcount = 1 and lastplayed = now
              movie.setWatched(true);
              if (movie.getPlaycount() == 0) {
                movie.setPlaycount(1);
                movie.setLastWatched(new Date());
              }
            }
            else {
              movie.setWatched(false);
              movie.setPlaycount(0);
              movie.setLastWatched(null);
            }
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnWatched, "cell 2 6");
      }
      {
        JLabel lblVideo3DT = new TmmLabel(TmmResourceBundle.getString("metatag.3d"));
        panelContent.add(lblVideo3DT, "cell 0 7,alignx right");

        final JCheckBox chckbxVideo3D = new JCheckBox("");
        panelContent.add(chckbxVideo3D, "cell 1 7");

        JButton btnVideo3D = new SquareIconButton(IconManager.APPLY_INV);
        btnVideo3D.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (Movie movie : moviesToEdit) {
            movie.setVideoIn3D(chckbxVideo3D.isSelected());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnVideo3D, "cell 2 7");
      }
      {
        JLabel lblMediasourceT = new TmmLabel(TmmResourceBundle.getString("metatag.source"));
        panelContent.add(lblMediasourceT, "cell 0 8,alignx right");

        final JComboBox cbMediaSource = new JComboBox(MediaSource.values());
        panelContent.add(cbMediaSource, "cell 1 8,growx");

        JButton btnMediaSource = new SquareIconButton(IconManager.APPLY_INV);
        btnMediaSource.addActionListener(e -> {
          changed = true;
          Object obj = cbMediaSource.getSelectedItem();
          if (obj instanceof MediaSource) {
            MediaSource mediaSource = (MediaSource) obj;
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            for (Movie movie : moviesToEdit) {
              movie.setMediaSource(mediaSource);
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }
        });
        panelContent.add(btnMediaSource, "cell 2 8");
      }
      {
        JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
        panelContent.add(lblLanguageT, "cell 0 9,alignx right");

        JTextField tfLanguage = new JTextField();
        panelContent.add(tfLanguage, "cell 1 9,growx");

        JButton btnLanguage = new SquareIconButton(IconManager.APPLY_INV);
        btnLanguage.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (Movie movie : moviesToEdit) {
            movie.setSpokenLanguages(tfLanguage.getText());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnLanguage, "cell 2 9");
      }
      {
        JLabel lblCountryT = new TmmLabel(TmmResourceBundle.getString("metatag.country"));
        panelContent.add(lblCountryT, "cell 0 10,alignx trailing");

        JTextField tfCountry = new JTextField();
        panelContent.add(tfCountry, "cell 1 10,growx");

        JButton btnCountry = new SquareIconButton(IconManager.APPLY_INV);
        btnCountry.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (Movie movie : moviesToEdit) {
            movie.setCountry(tfCountry.getText());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnCountry, "cell 2 10");
      }
      {
        JLabel lblNoteT = new TmmLabel(TmmResourceBundle.getString("metatag.note"));
        panelContent.add(lblNoteT, "cell 0 11,alignx trailing");

        JTextField tfNote = new JTextField();
        panelContent.add(tfNote, "cell 1 11,growx");

        JButton btnNote = new SquareIconButton(IconManager.APPLY_INV);
        btnNote.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (Movie movie : moviesToEdit) {
            movie.setNote(tfNote.getText());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnNote, "cell 2 11");
      }
      {
        JLabel lblDateAddedT = new TmmLabel(TmmResourceBundle.getString("metatag.dateadded"));
        panelContent.add(lblDateAddedT, "cell 0 12,alignx trailing");

        JSpinner spDateAdded = new JSpinner(new SpinnerDateModel());
        panelContent.add(spDateAdded, "cell 1 12");

        JButton btnDateAdded = new SquareIconButton(IconManager.APPLY_INV);
        btnDateAdded.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (Movie movie : moviesToEdit) {
            movie.setDateAdded((Date) spDateAdded.getValue());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnDateAdded, "cell 2 12");

      }
      {
        JLabel lblSorttitleT = new TmmLabel(TmmResourceBundle.getString("metatag.sorttitle"));
        panelContent.add(lblSorttitleT, "flowx,cell 0 13,alignx right");

        JLabel lblSorttitleInfo = new JLabel(IconManager.HINT);
        lblSorttitleInfo.setToolTipText(TmmResourceBundle.getString("edit.setsorttitle.desc"));
        panelContent.add(lblSorttitleInfo, "cell 0 13");

        JButton btnSetSorttitle = new JButton(TmmResourceBundle.getString("edit.setsorttitle"));
        btnSetSorttitle.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (Movie movie : moviesToEdit) {
            movie.setSortTitle(movie.getTitleSortable());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnSetSorttitle, "flowx,cell 1 13 4 1");
      }
      {
        JLabel lblSpokenLanguages = new TmmLabel(TmmResourceBundle.getString("metatag.spokenlanguages"));
        panelContent.add(lblSpokenLanguages, "cell 0 14,alignx right");

        JButton btnFirstAudioStream = new JButton(TmmResourceBundle.getString("edit.audio.first"));
        btnFirstAudioStream.setToolTipText(TmmResourceBundle.getString("edit.audio.first.desc"));
        btnFirstAudioStream.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (Movie movie : moviesToEdit) {
            movie.setSpokenLanguages(movie.getMediaInfoAudioLanguageList().stream().findFirst().orElse(""));
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnFirstAudioStream, "flowx,cell 1 14 4 1");

        JButton btnBestAudioStream = new JButton(TmmResourceBundle.getString("edit.audio.best"));
        btnBestAudioStream.setToolTipText(TmmResourceBundle.getString("edit.audio.best.desc"));
        btnBestAudioStream.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (Movie movie : moviesToEdit) {
            movie.setSpokenLanguages(movie.getMediaInfoAudioLanguage());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnBestAudioStream, "cell 1 14");

        JButton btnAllAudioStreams = new JButton(TmmResourceBundle.getString("edit.audio.all"));
        btnAllAudioStreams.setToolTipText(TmmResourceBundle.getString("edit.audio.all.desc"));
        btnAllAudioStreams.addActionListener(e -> {
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (Movie movie : moviesToEdit) {
            movie.setSpokenLanguages(String.join(", ", movie.getMediaInfoAudioLanguageList()));
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnAllAudioStreams, "cell 1 14");
      }

      JButton btnClearSorttitle = new JButton(TmmResourceBundle.getString("edit.clearsorttitle"));
      btnClearSorttitle.addActionListener(e -> {
        changed = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        for (Movie movie : moviesToEdit) {
          movie.setSortTitle("");
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      });
      panelContent.add(btnClearSorttitle, "cell 1 13 4 1");
    }

    /*
     * EXPERT MODE
     */
    {
      JPanel panelContent = new JPanel();

      tabbedPane.addTab(TmmResourceBundle.getString("bulkedit.expert"), panelContent);
      panelContent.setLayout(new MigLayout("", "[][100lp:200lp,grow]", "[grow][20lp!][][50lp][][20lp!][200lp:250lp,grow]"));

      {
        JTextPane tpDescription = new ReadOnlyTextPane(TmmResourceBundle.getString("bulkedit.description"));
        panelContent.add(tpDescription, "cell 0 0 2 1,grow");
      }
      {
        JLabel lblPropertyT = new TmmLabel(TmmResourceBundle.getString("bulkedit.field"));
        panelContent.add(lblPropertyT, "cell 0 2,alignx right");

        cbProperty = new JComboBox();
        cbProperty.addItem(new MovieProperty("title", TmmResourceBundle.getString("metatag.title")));
        cbProperty.addItem(new MovieProperty("originalTitle", TmmResourceBundle.getString("metatag.originaltitle")));
        cbProperty.addItem(new MovieProperty("sortTitle", TmmResourceBundle.getString("metatag.sorttitle")));
        cbProperty.addItem(new MovieProperty("tagline", TmmResourceBundle.getString("metatag.tagline")));
        cbProperty.addItem(new MovieProperty("plot", TmmResourceBundle.getString("metatag.plot")));
        cbProperty.addItem(new MovieProperty("productionCompany", TmmResourceBundle.getString("metatag.production")));
        cbProperty.addItem(new MovieProperty("spokenLanguages", TmmResourceBundle.getString("metatag.spokenlanguages")));
        cbProperty.addItem(new MovieProperty("country", TmmResourceBundle.getString("metatag.country")));
        cbProperty.addItem(new MovieProperty("note", TmmResourceBundle.getString("metatag.note")));
        cbProperty.addItem(new MovieProperty("originalFilename", TmmResourceBundle.getString("metatag.originalfile")));

        cbProperty.addItemListener(
            e -> movieValuesEventList.forEach(movieValues -> movieValues.changeProperty(((MovieProperty) cbProperty.getSelectedItem()).property)));
        panelContent.add(cbProperty, "cell 1 2,growx,wmin 0");
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
            movieValuesEventList.forEach(movieValues -> movieValues.changePattern(taPattern.getText()));
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
          changed = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          movieValuesEventList.forEach(MovieValues::applyValue);
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnApply, "cell 1 4,alignx right");
      }
      {
        TmmTable tableValues = new TmmTable(new TmmTableModel<>(movieValuesEventList, new MovieValuesTableFormat()));
        tableValues.setSelectionModel(new NullSelectionModel());

        JScrollPane scrollPane = new JScrollPane();
        tableValues.configureScrollPane(scrollPane);
        panelContent.add(scrollPane, "cell 0 6 2 1,grow");

        scrollPane.setViewportView(tableValues);
      }
    }
    {
      JButton btnClose = new JButton(TmmResourceBundle.getString("Button.close"));
      btnClose.setIcon(IconManager.APPLY_INV);
      btnClose.addActionListener(arg0 -> {
        // rewrite movies, if anything changed
        if (changed) {
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (Movie movie : moviesToEdit) {
            movie.writeNFO();
            movie.saveToDb();
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        setVisible(false);
      });
      addDefaultButton(btnClose);

      // add window listener to write changes (if the window close button "X" is pressed)
      addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          // rewrite movies, if anything changed
          if (changed) {
            for (Movie movie : moviesToEdit) {
              movie.writeNFO();
              movie.saveToDb();
            }
            // if configured - sync with trakt.tv
            if (MovieModuleManager.getInstance().getSettings().getSyncTrakt()) {
              MovieSyncTraktTvTask task = new MovieSyncTraktTvTask(moviesToEdit);
              task.setSyncCollection(MovieModuleManager.getInstance().getSettings().getSyncTraktCollection());
              task.setSyncWatched(MovieModuleManager.getInstance().getSettings().getSyncTraktWatched());
              task.setSyncRating(MovieModuleManager.getInstance().getSettings().getSyncTraktRating());

              TmmTaskManager.getInstance().addUnnamedTask(task);
            }
          }
        }
      });
    }

    {
      setMovieSets();
      moviesToEdit.addAll(movies);

      for (Movie movie : moviesToEdit) {
        MovieValues movieValues = new MovieValues(movie);
        movieValues.changeProperty(((MovieProperty) cbProperty.getSelectedItem()).property);
        movieValuesEventList.add(movieValues);
      }

      PropertyChangeListener listener = evt -> {
        if ("addedMovieSet".equals(evt.getPropertyName())) {
          setMovieSets();
        }
      };
      movieList.addPropertyChangeListener(listener);
    }
  }

  private void setMovieSets() {
    MovieSet selectedMovieSet = null;

    Object obj = cbMovieSet.getSelectedItem();
    if (obj instanceof MovieSet) {
      selectedMovieSet = (MovieSet) obj;
    }

    cbMovieSet.removeAllItems();

    cbMovieSet.addItem("");

    for (MovieSet movieSet : movieList.getSortedMovieSetList()) {
      cbMovieSet.addItem(movieSet);
    }

    if (selectedMovieSet != null) {
      cbMovieSet.setSelectedItem(selectedMovieSet);
    }
  }

  private boolean isDeleteConfirmed(String attribute) {
    Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
    int dialogResult = JOptionPane.showOptionDialog(MovieBulkEditorDialog.this,
        MessageFormat.format(TmmResourceBundle.getString("message.bulkedit.delete"), attribute),
        TmmResourceBundle.getString("message.bulkedit.warning"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
    if (dialogResult == JOptionPane.YES_OPTION) {
      return true;
    }
    else {
      return false;
    }
  }

  private static class MovieValues extends AbstractModelObject {
    private final Movie movie;

    private String      property;
    private String      propertyValue = "";
    private String      patternValue  = "";

    public MovieValues(Movie movie) {
      this.movie = movie;
    }

    public void changeProperty(String property) {
      this.property = property;
      String oldValue = propertyValue;
      propertyValue = "";

      try {
        PropertyDescriptor descriptor = new PropertyDescriptor(property, Movie.class);
        Object value = descriptor.getReadMethod().invoke(movie);
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
        root.put("movie", movie);
        return engine.transform(JmteUtils.morphTemplate(pattern, MovieRenamer.getTokenMap()), root);
      }
      catch (Exception e) {
        return pattern;
      }
    }

    public void applyValue() {
      try {
        // set the property in the movie
        PropertyDescriptor descriptor = new PropertyDescriptor(property, Movie.class);
        descriptor.getWriteMethod().invoke(movie, patternValue);

        // and update old value
        String oldValue = propertyValue;
        propertyValue = patternValue;
        firePropertyChange("propertyValue", oldValue, propertyValue);
      }
      catch (Exception e) {
        // just do nothing
      }
    }

    public String getMovieTitle() {
      return movie.getTitle();
    }

    public String getPropertyValue() {
      return propertyValue;
    }

    public String getPatternValue() {
      return patternValue;
    }
  }

  private static class MovieValuesTableFormat extends TmmTableFormat<MovieValues> {
    public MovieValuesTableFormat() {
      /*
       * movie title
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.title"), "title", MovieValues::getMovieTitle, String.class);
      addColumn(col);

      /*
       * old value
       */
      col = new Column(TmmResourceBundle.getString("bulkedit.oldvalue"), "oldValue", MovieValues::getPropertyValue, String.class);
      addColumn(col);

      /*
       * new value
       */
      col = new Column(TmmResourceBundle.getString("bulkedit.newvalue"), "newValue", MovieValues::getPatternValue, String.class);
      addColumn(col);
    }
  }

  private static class MovieProperty {
    private final String property;
    private final String description;

    public MovieProperty(String property, String description) {
      this.property = property;
      this.description = description;
    }

    @Override
    public String toString() {
      return description;
    }
  }
}
