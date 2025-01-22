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
package org.tinymediamanager.ui.movies.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;
import static org.tinymediamanager.ui.TmmFontHelper.L2;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmRoundTextArea;
import org.tinymediamanager.ui.movies.MovieUIModule;
import org.tinymediamanager.ui.movies.dialogs.MovieJmteExplorerDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The class MovieRenamerSettingsPanel.
 */
public class MovieRenamerSettingsPanel extends JPanel implements HierarchyListener {
  private static final Logger LOGGER           = LoggerFactory.getLogger(MovieRenamerSettingsPanel.class);

  private final MovieSettings settings         = MovieModuleManager.getInstance().getSettings();
  private final List<String>  spaceReplacement = new ArrayList<>(Arrays.asList("_", ".", "-"));
  private final List<String>  colonReplacement = new ArrayList<>(Arrays.asList(" ", "-", "_"));

  /**
   * UI components
   */
  private JTextArea           tfMoviePath;
  private JTextArea           tfMovieFilename;
  private LinkLabel           lblExampleDatasource;
  private JLabel              lblExampleFoldername;
  private JLabel              lblExampleFilename;
  private JCheckBox           chckbxAsciiReplacement;

  private JCheckBox           chckbxFoldernameSpaceReplacement;
  private JComboBox           cbFoldernameSpaceReplacement;
  private JCheckBox           chckbxFilenameSpaceReplacement;
  private JComboBox           cbFilenameSpaceReplacement;
  private JComboBox           cbMovieForPreview;
  private JCheckBox           chckbxRemoveOtherNfos;
  private JCheckBox           chckbxCleanupUnwanted;
  private JCheckBox           chckbxMoviesetSingleMovie;

  private ReadOnlyTextArea    taWarning;
  private JComboBox           cbColonReplacement;
  private JTextField          tfFirstCharacter;
  private JCheckBox           chckbxAllowMerge;
  private JCheckBox           chckbxAutomaticRename;

  public MovieRenamerSettingsPanel() {

    // UI initializations
    initComponents();
    initDataBindings();

    // data init
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

    tfFirstCharacter.getDocument().addDocumentListener(documentListener);

    settings.addPropertyChangeListener(e -> {
      switch (e.getPropertyName()) {
        case "renamerPathname", "renamerFilename" -> createRenamerExample();
      }
    });

    // foldername space replacement
    String replacement = settings.getRenamerPathnameSpaceReplacement();
    int index = spaceReplacement.indexOf(replacement);
    if (index >= 0) {
      cbFoldernameSpaceReplacement.setSelectedIndex(index);
    }

    // filename space replacement
    replacement = settings.getRenamerFilenameSpaceReplacement();
    index = spaceReplacement.indexOf(replacement);
    if (index >= 0) {
      cbFilenameSpaceReplacement.setSelectedIndex(index);
    }

    // colon replacement
    replacement = settings.getRenamerColonReplacement();
    index = colonReplacement.indexOf(replacement);
    if (index >= 0) {
      cbColonReplacement.setSelectedIndex(index);
    }

    cbFoldernameSpaceReplacement.addActionListener(arg0 -> {
      checkChanges();
      createRenamerExample();
    });
    cbFilenameSpaceReplacement.addActionListener(arg0 -> {
      checkChanges();
      createRenamerExample();
    });
    cbColonReplacement.addActionListener(arg0 -> {
      checkChanges();
      createRenamerExample();
    });

    lblExampleFilename.putClientProperty("clipPosition", SwingConstants.LEFT);

    // event listener must be at the end
    ActionListener actionCreateRenamerExample = e -> createRenamerExample();
    cbMovieForPreview.addActionListener(actionCreateRenamerExample);
    chckbxMoviesetSingleMovie.addActionListener(actionCreateRenamerExample);
    chckbxAsciiReplacement.addActionListener(actionCreateRenamerExample);
    chckbxFilenameSpaceReplacement.addActionListener(actionCreateRenamerExample);
    chckbxFoldernameSpaceReplacement.addActionListener(actionCreateRenamerExample);
  }

  private void initComponents() {
    setLayout(new MigLayout("hidemode 1", "[600lp,grow]", "[][15lp!][][15lp!][]"));
    {
      JPanel panelPatterns = new JPanel(new MigLayout("insets 0, hidemode 1", "[20lp!][15lp][][400lp,grow][grow]", "[][][][][][]"));

      JLabel lblPatternsT = new TmmLabel(TmmResourceBundle.getString("Settings.movie.renamer.title"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelPatterns, lblPatternsT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#renamer-pattern"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        JLabel lblMoviePath = new TmmLabel(TmmResourceBundle.getString("Settings.renamer.folder"));
        panelPatterns.add(lblMoviePath, "cell 1 0 2 1,alignx right");

        tfMoviePath = new TmmRoundTextArea();
        panelPatterns.add(tfMoviePath, "cell 3 0, growx, wmin 0");

        JButton btnReset = new FlatButton(IconManager.UNDO_GREY);
        btnReset.setToolTipText(TmmResourceBundle.getString("Settings.renamer.reverttodefault"));
        btnReset.addActionListener(l -> tfMoviePath.setText(MovieSettings.DEFAULT_RENAMER_FOLDER_PATTERN));
        panelPatterns.add(btnReset, "cell 3 0, aligny top");

        JLabel lblDefault = new JLabel(TmmResourceBundle.getString("Settings.default"));
        panelPatterns.add(lblDefault, "cell 1 1 2 1,alignx right");
        TmmFontHelper.changeFont(lblDefault, L2);

        JTextArea tpDefaultFolderPattern = new ReadOnlyTextArea(MovieSettings.DEFAULT_RENAMER_FOLDER_PATTERN);
        panelPatterns.add(tpDefaultFolderPattern, "cell 3 1,growx,wmin 0");
        TmmFontHelper.changeFont(tpDefaultFolderPattern, L2);
      }
      {
        JLabel lblMovieFilename = new TmmLabel(TmmResourceBundle.getString("Settings.renamer.file"));
        panelPatterns.add(lblMovieFilename, "cell 1 2 2 1,alignx right");

        tfMovieFilename = new TmmRoundTextArea();
        panelPatterns.add(tfMovieFilename, "cell 3 2, growx, wmin 0");

        JButton btnReset = new FlatButton(IconManager.UNDO_GREY);
        btnReset.setToolTipText(TmmResourceBundle.getString("Settings.renamer.reverttodefault"));
        btnReset.addActionListener(l -> tfMovieFilename.setText(MovieSettings.DEFAULT_RENAMER_FILE_PATTERN));
        panelPatterns.add(btnReset, "cell 3 2, aligny top");

        JLabel lblDefault = new JLabel(TmmResourceBundle.getString("Settings.default"));
        panelPatterns.add(lblDefault, "cell 1 3 2 1,alignx right");
        TmmFontHelper.changeFont(lblDefault, L2);

        JTextArea tpDefaultFilePattern = new ReadOnlyTextArea(MovieSettings.DEFAULT_RENAMER_FILE_PATTERN);
        panelPatterns.add(tpDefaultFilePattern, "cell 3 3,growx,wmin 0");
        TmmFontHelper.changeFont(tpDefaultFilePattern, L2);
      }
      {

        JLabel lblRenamerHintT = new JLabel(TmmResourceBundle.getString("Settings.movie.renamer.example"));
        panelPatterns.add(lblRenamerHintT, "cell 1 4 3 1");
      }
      {
        JButton btnJmteExplorer = new JButton(TmmResourceBundle.getString("jmteexplorer.title"));
        btnJmteExplorer.addActionListener(e -> {
          MovieJmteExplorerDialog dialog = new MovieJmteExplorerDialog((JDialog) this.getTopLevelAncestor());
          dialog.setVisible(true);
        });
        panelPatterns.add(btnJmteExplorer, "cell 4 0");
      }
      {
        taWarning = new ReadOnlyTextArea();
        taWarning.setForeground(Color.red);
        panelPatterns.add(taWarning, "cell 3 5,growx,wmin 0");
      }
    }
    {
      JPanel panelAdvancedOptions = new JPanel();
      panelAdvancedOptions.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][][][][][]")); // 16lp ~ width of the

      JLabel lblAdvancedOptions = new TmmLabel(TmmResourceBundle.getString("Settings.advancedoptions"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelAdvancedOptions, lblAdvancedOptions, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#advanced-options-4"));
      add(collapsiblePanel, "cell 0 2,growx, wmin 0");

      {
        chckbxAutomaticRename = new JCheckBox(TmmResourceBundle.getString("Settings.movie.automaticrename"));
        panelAdvancedOptions.add(chckbxAutomaticRename, "cell 1 0 2 1");

        JLabel lblAutomaticRenameHint = new JLabel(IconManager.HINT);
        lblAutomaticRenameHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.automaticrename.desc"));
        panelAdvancedOptions.add(lblAutomaticRenameHint, "cell 1 0 2 1");

        chckbxFoldernameSpaceReplacement = new JCheckBox(TmmResourceBundle.getString("Settings.renamer.folderspacereplacement"));
        chckbxFoldernameSpaceReplacement.setToolTipText(TmmResourceBundle.getString("Settings.renamer.folderspacereplacement.hint"));
        panelAdvancedOptions.add(chckbxFoldernameSpaceReplacement, "cell 1 1 2 1");

        cbFoldernameSpaceReplacement = new JComboBox<>(spaceReplacement.toArray());
        panelAdvancedOptions.add(cbFoldernameSpaceReplacement, "cell 1 1 2 1");
      }
      {
        chckbxFilenameSpaceReplacement = new JCheckBox(TmmResourceBundle.getString("Settings.renamer.spacereplacement"));
        chckbxFilenameSpaceReplacement.setToolTipText(TmmResourceBundle.getString("Settings.renamer.spacereplacement.hint"));
        panelAdvancedOptions.add(chckbxFilenameSpaceReplacement, "cell 1 2 2 1");

        cbFilenameSpaceReplacement = new JComboBox<>(spaceReplacement.toArray());
        panelAdvancedOptions.add(cbFilenameSpaceReplacement, "cell 1 2 2 1");
      }
      {
        JLabel lblColonReplacement = new JLabel(TmmResourceBundle.getString("Settings.renamer.colonreplacement"));
        panelAdvancedOptions.add(lblColonReplacement, "cell 1 3 2 1");
        lblColonReplacement.setToolTipText(TmmResourceBundle.getString("Settings.renamer.colonreplacement.hint"));

        cbColonReplacement = new JComboBox<>(colonReplacement.toArray());
        panelAdvancedOptions.add(cbColonReplacement, "cell 1 3 2 1");
      }
      {
        chckbxAsciiReplacement = new JCheckBox(TmmResourceBundle.getString("Settings.renamer.asciireplacement"));
        panelAdvancedOptions.add(chckbxAsciiReplacement, "cell 1 4 2 1");

        JLabel lblAsciiHint = new JLabel(TmmResourceBundle.getString("Settings.renamer.asciireplacement.hint"));
        panelAdvancedOptions.add(lblAsciiHint, "cell 2 5");
        TmmFontHelper.changeFont(lblAsciiHint, L2);
      }
      {
        JLabel lblFirstCharacterT = new JLabel(TmmResourceBundle.getString("Settings.renamer.firstnumbercharacterreplacement"));
        panelAdvancedOptions.add(lblFirstCharacterT, "flowx,cell 1 6 2 1");

        tfFirstCharacter = new JTextField();
        panelAdvancedOptions.add(tfFirstCharacter, "cell 1 6 2 1");
        tfFirstCharacter.setColumns(2);
      }
      {
        chckbxMoviesetSingleMovie = new JCheckBox(TmmResourceBundle.getString("Settings.renamer.moviesetsinglemovie"));
        panelAdvancedOptions.add(chckbxMoviesetSingleMovie, "cell 1 7 2 1");
      }
      {
        chckbxRemoveOtherNfos = new JCheckBox(TmmResourceBundle.getString("Settings.renamer.removenfo"));
        panelAdvancedOptions.add(chckbxRemoveOtherNfos, "cell 1 8 2 1");
      }
      {
        chckbxCleanupUnwanted = new JCheckBox(TmmResourceBundle.getString("Settings.cleanupfiles"));
        panelAdvancedOptions.add(chckbxCleanupUnwanted, "cell 1 9 2 1");
      }
      {
        chckbxAllowMerge = new JCheckBox(TmmResourceBundle.getString("Settings.renamer.movie.allowmerge"));
        panelAdvancedOptions.add(chckbxAllowMerge, "cell 1 10 2 1");
      }
    }
    {
      JPanel panelExample = new JPanel();
      panelExample.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][][300lp,grow]", "[]10lp![][][]"));

      JLabel lblExampleHeader = new TmmLabel(TmmResourceBundle.getString("Settings.example"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelExample, lblExampleHeader, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#example"));
      add(collapsiblePanel, "cell 0 4, growx, wmin 0");
      {
        JLabel lblExampleT = new TmmLabel(TmmResourceBundle.getString("tmm.movie"));
        panelExample.add(lblExampleT, "cell 1 0");

        cbMovieForPreview = new JComboBox();
        panelExample.add(cbMovieForPreview, "cell 2 0, wmin 0");

        JLabel lblDatasourceT = new TmmLabel(TmmResourceBundle.getString("metatag.datasource"));
        panelExample.add(lblDatasourceT, "cell 1 1");

        lblExampleDatasource = new LinkLabel("");
        panelExample.add(lblExampleDatasource, "cell 2 1, wmin 0");

        JLabel lblFoldernameT = new TmmLabel(TmmResourceBundle.getString("Settings.renamer.folder"));
        panelExample.add(lblFoldernameT, "cell 1 2");

        lblExampleFoldername = new JLabel("");
        panelExample.add(lblExampleFoldername, "cell 2 2, wmin 0");

        JLabel lblFilenameT = new TmmLabel(TmmResourceBundle.getString("Settings.renamer.file"));
        panelExample.add(lblFilenameT, "cell 1 3");

        lblExampleFilename = new JLabel("");
        panelExample.add(lblExampleFilename, "cell 2 3, wmin 0");
      }
    }
  }

  private void buildAndInstallMovieArray() {
    cbMovieForPreview.removeAllItems();
    List<Movie> allMovies = new ArrayList<>(MovieModuleManager.getInstance().getMovieList().getMovies());
    Movie sel = MovieUIModule.getInstance().getSelectionModel().getSelectedMovie();
    allMovies.sort(new MovieComparator());
    for (Movie movie : allMovies) {
      MoviePreviewContainer container = new MoviePreviewContainer();
      container.movie = movie;
      cbMovieForPreview.addItem(container);
      if (sel != null && movie.equals(sel)) {
        cbMovieForPreview.setSelectedItem(container);
      }
    }
  }

  private void createRenamerExample() {
    Movie movie = null;

    String warning = "";
    // empty is valid (although not unique)
    if (!tfMoviePath.getText().isEmpty() && !MovieRenamer.isFolderPatternUnique(tfMoviePath.getText())) {
      warning = TmmResourceBundle.getString("Settings.renamer.folder.warning");
    }
    if (!warning.isEmpty()) {
      taWarning.setVisible(true);
      taWarning.setText(warning);
    }
    else {
      taWarning.setVisible(false);
    }

    if (cbMovieForPreview.getSelectedItem() instanceof MoviePreviewContainer container) {
      movie = container.movie;
    }

    if (movie != null) {
      String path = "";
      String filename = "";
      if (StringUtils.isNotBlank(tfMoviePath.getText())) {
        path = MovieRenamer.createDestinationForFoldername(tfMoviePath.getText(), movie);
        try {
          path = Paths.get(movie.getDataSource(), path).toString();
        }
        catch (Exception e) {
          // catch invalid paths (e.g. illegal characters in the pathname)
        }
      }
      else {
        // the old folder name
        path = movie.getPathNIO().toString();
      }

      if (StringUtils.isNotBlank(tfMovieFilename.getText())) {
        List<MediaFile> mediaFiles = movie.getMediaFiles(MediaFileType.VIDEO);
        if (!mediaFiles.isEmpty()) {
          String extension = FilenameUtils.getExtension(mediaFiles.get(0).getFilename());
          filename = MovieRenamer.createDestinationForFilename(tfMovieFilename.getText(), movie);
          // patterns are always w/o extension, but when having the originalFilename, it will be there.
          if (!filename.endsWith(extension)) {
            filename += "." + extension;
          }
        }
      }
      else {
        filename = movie.getMediaFiles(MediaFileType.VIDEO).get(0).getFilename();
      }

      lblExampleDatasource.setText(movie.getDataSource());
      lblExampleFoldername.setText(path.replace(movie.getDataSource() + File.separator, ""));
      lblExampleFilename.setText(filename);
    }
    else {
      lblExampleDatasource.setText(TmmResourceBundle.getString("Settings.movie.renamer.nomovie"));
      lblExampleFoldername.setText(TmmResourceBundle.getString("Settings.movie.renamer.nomovie"));
      lblExampleFilename.setText(TmmResourceBundle.getString("Settings.movie.renamer.nomovie"));
    }
  }

  private void checkChanges() {
    // foldername space replacement
    String replacement = (String) cbFoldernameSpaceReplacement.getSelectedItem();
    settings.setRenamerPathnameSpaceReplacement(replacement);

    // filename space replacement
    replacement = (String) cbFilenameSpaceReplacement.getSelectedItem();
    settings.setRenamerFilenameSpaceReplacement(replacement);

    // colon replacement
    replacement = (String) cbColonReplacement.getSelectedItem();
    settings.setRenamerColonReplacement(replacement);
  }

  @Override
  public void hierarchyChanged(HierarchyEvent arg0) {
    if (isShowing()) {
      buildAndInstallMovieArray();
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    addHierarchyListener(this);
  }

  @Override
  public void removeNotify() {
    removeHierarchyListener(this);
    super.removeNotify();
  }

  /*****************************************************************************
   * helper classes
   *****************************************************************************/
  private static class MoviePreviewContainer {
    Movie movie;

    @Override
    public String toString() {
      return movie.getTitle();
    }
  }

  private static class MovieComparator implements Comparator<Movie> {
    @Override
    public int compare(Movie arg0, Movie arg1) {
      return arg0.getTitle().compareTo(arg1.getTitle());
    }
  }

  protected void initDataBindings() {
    Property settingsBeanProperty_11 = BeanProperty.create("renamerPathname");
    Property jTextFieldBeanProperty_3 = BeanProperty.create("text");
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_11, tfMoviePath,
        jTextFieldBeanProperty_3);
    autoBinding_10.bind();
    //
    Property settingsBeanProperty_12 = BeanProperty.create("renamerFilename");
    Property jTextFieldBeanProperty_4 = BeanProperty.create("text");
    AutoBinding autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_12, tfMovieFilename,
        jTextFieldBeanProperty_4);
    autoBinding_11.bind();
    //
    Property settingsBeanProperty = BeanProperty.create("renamerPathnameSpaceSubstitution");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty, chckbxFoldernameSpaceReplacement,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property settingsBeanProperty_2 = BeanProperty.create("renamerFilenameSpaceSubstitution");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_2,
        chckbxFilenameSpaceReplacement, jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property settingsBeanProperty_1 = BeanProperty.create("renamerNfoCleanup");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_1, chckbxRemoveOtherNfos,
        jCheckBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property settingsBeanProperty_5 = BeanProperty.create("renamerCreateMoviesetForSingleMovie");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_5, chckbxMoviesetSingleMovie,
        jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property settingsBeanProperty_7 = BeanProperty.create("asciiReplacement");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_7, chckbxAsciiReplacement,
        jCheckBoxBeanProperty);
    autoBinding_5.bind();
    //
    Property movieSettingsBeanProperty = BeanProperty.create("renamerFirstCharacterNumberReplacement");
    Property jTextFieldBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, tfFirstCharacter,
        jTextFieldBeanProperty);
    autoBinding_3.bind();
    //
    Property movieSettingsBeanProperty_1 = BeanProperty.create("allowMultipleMoviesInSameDir");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_1, chckbxAllowMerge,
        jCheckBoxBeanProperty);
    autoBinding_6.bind();
    //
    Property movieSettingsBeanProperty_2 = BeanProperty.create("renameAfterScrape");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_2, chckbxAutomaticRename,
        jCheckBoxBeanProperty);
    autoBinding_7.bind();
    //
    Property movieSettingsBeanProperty_3 = BeanProperty.create("renamerCleanupUnwanted");
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_3, chckbxCleanupUnwanted,
        jCheckBoxBeanProperty);
    autoBinding_8.bind();
  }
}
