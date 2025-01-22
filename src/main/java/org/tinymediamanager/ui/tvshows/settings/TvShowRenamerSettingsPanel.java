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

package org.tinymediamanager.ui.tvshows.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;
import static org.tinymediamanager.ui.TmmFontHelper.L2;

import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
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
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowRenamer;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmRoundTextArea;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.renderer.MultilineTableCellRenderer;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowJmteExplorerDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The class TvShowRenamerSettingsPanel
 * 
 * @author Manuel Laggner
 */
public class TvShowRenamerSettingsPanel extends JPanel implements HierarchyListener {
  private static final Logger                      LOGGER            = LoggerFactory.getLogger(TvShowRenamerSettingsPanel.class);

  private final TvShowSettings                     settings          = TvShowModuleManager.getInstance().getSettings();
  private final List<String>                       spaceReplacements = new ArrayList<>(Arrays.asList("_", ".", "-"));
  private final List<String>                       colonReplacements = new ArrayList<>(Arrays.asList(" ", "-", "_"));

  /*
   * UI components
   */
  private LinkLabel                                lblExampleDatasource;
  private JLabel                                   lblExampleFoldername;
  private JLabel                                   lblExampleFilename;
  private JComboBox<TvShowPreviewContainer>        cbTvShowForPreview;
  private JTextArea                                tfSeasonFolderName;
  private JCheckBox                                chckbxAsciiReplacement;
  private JCheckBox                                chckbxShowFoldernameSpaceReplacement;
  private JComboBox                                cbShowFoldernameSpaceReplacement;
  private JCheckBox                                chckbxSeasonFoldernameSpaceReplacement;
  private JComboBox                                cbSeasonFoldernameSpaceReplacement;
  private JCheckBox                                chckbxFilenameSpaceReplacement;
  private JComboBox                                cbFilenameSpaceReplacement;
  private JComboBox<TvShowEpisodePreviewContainer> cbEpisodeForPreview;
  private JTextArea                                tfTvShowFolder;
  private JTextArea                                tfEpisodeFilename;
  private JComboBox                                cbColonReplacement;
  private JTextField                               tfFirstCharacter;
  private JCheckBox                                chckbxAutomaticRename;
  private JCheckBox                                chckbxCleanupUnwanted;

  public TvShowRenamerSettingsPanel() {
    // UI initializations
    initComponents();
    initDataBindings();

    // the panel renamer
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

    tfTvShowFolder.getDocument().addDocumentListener(documentListener);
    tfSeasonFolderName.getDocument().addDocumentListener(documentListener);
    tfEpisodeFilename.getDocument().addDocumentListener(documentListener);
    tfFirstCharacter.getDocument().addDocumentListener(documentListener);

    cbTvShowForPreview.addActionListener(arg0 -> {
      buildAndInstallEpisodeArray();
      createRenamerExample();
    });

    // show folder name space replacement
    String replacement = settings.getRenamerShowPathnameSpaceReplacement();
    int index = spaceReplacements.indexOf(replacement);
    if (index >= 0) {
      cbShowFoldernameSpaceReplacement.setSelectedIndex(index);
    }

    // season folder name space replacement
    replacement = settings.getRenamerSeasonPathnameSpaceReplacement();
    index = spaceReplacements.indexOf(replacement);
    if (index >= 0) {
      cbSeasonFoldernameSpaceReplacement.setSelectedIndex(index);
    }

    // filename space replacement
    replacement = settings.getRenamerFilenameSpaceReplacement();
    index = spaceReplacements.indexOf(replacement);
    if (index >= 0) {
      cbFilenameSpaceReplacement.setSelectedIndex(index);
    }

    // colon replacement
    String colonReplacement = settings.getRenamerColonReplacement();
    index = this.colonReplacements.indexOf(colonReplacement);
    if (index >= 0) {
      cbColonReplacement.setSelectedIndex(index);
    }

    // event listener must be at the end
    ActionListener renamerActionListener = arg0 -> {
      checkChanges();
      createRenamerExample();
    };

    chckbxShowFoldernameSpaceReplacement.addActionListener(renamerActionListener);
    chckbxSeasonFoldernameSpaceReplacement.addActionListener(renamerActionListener);
    chckbxFilenameSpaceReplacement.addActionListener(renamerActionListener);
    chckbxAsciiReplacement.addActionListener(renamerActionListener);
    cbEpisodeForPreview.addActionListener(arg0 -> createRenamerExample());
    cbShowFoldernameSpaceReplacement.addActionListener(renamerActionListener);
    cbSeasonFoldernameSpaceReplacement.addActionListener(renamerActionListener);
    cbFilenameSpaceReplacement.addActionListener(renamerActionListener);
    cbColonReplacement.addActionListener(renamerActionListener);
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][][15lp!][]"));
    {
      JPanel panelPatterns = new JPanel(new MigLayout("insets 0, hidemode 1", "[20lp!][15lp][][400lp,grow][grow]", "[][][][][][][]"));

      JLabel lblPatternsT = new TmmLabel(TmmResourceBundle.getString("Settings.tvshow.renamer.title"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelPatterns, lblPatternsT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#renamer"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");

      {
        JLabel lblTvShowFolder = new JLabel(TmmResourceBundle.getString("Settings.tvshowfoldername"));
        panelPatterns.add(lblTvShowFolder, "cell 1 0 2 1,alignx right");

        tfTvShowFolder = new TmmRoundTextArea();
        panelPatterns.add(tfTvShowFolder, "cell 3 0, growx, wmin 0");

        JButton btnReset = new FlatButton(IconManager.UNDO_GREY);
        btnReset.setToolTipText(TmmResourceBundle.getString("Settings.renamer.reverttodefault"));
        btnReset.addActionListener(l -> tfTvShowFolder.setText(TvShowSettings.DEFAULT_RENAMER_FOLDER_PATTERN));
        panelPatterns.add(btnReset, "cell 3 0, aligny top");

        JLabel lblDefault = new JLabel(TmmResourceBundle.getString("Settings.default"));
        panelPatterns.add(lblDefault, "cell 1 1 2 1,alignx right");
        TmmFontHelper.changeFont(lblDefault, L2);

        JTextArea tpDefaultFolderPattern = new ReadOnlyTextArea(TvShowSettings.DEFAULT_RENAMER_FOLDER_PATTERN);
        panelPatterns.add(tpDefaultFolderPattern, "cell 3 1,growx,wmin 0");
        TmmFontHelper.changeFont(tpDefaultFolderPattern, L2);
      }
      {
        JLabel lblSeasonFolderName = new JLabel(TmmResourceBundle.getString("Settings.tvshowseasonfoldername"));
        panelPatterns.add(lblSeasonFolderName, "cell 1 2 2 1,alignx right");

        tfSeasonFolderName = new TmmRoundTextArea();
        panelPatterns.add(tfSeasonFolderName, "cell 3 2, growx, wmin 0");

        JButton btnReset = new FlatButton(IconManager.UNDO_GREY);
        btnReset.setToolTipText(TmmResourceBundle.getString("Settings.renamer.reverttodefault"));
        btnReset.addActionListener(l -> tfSeasonFolderName.setText(TvShowSettings.DEFAULT_RENAMER_SEASON_PATTERN));
        panelPatterns.add(btnReset, "cell 3 2, aligny top");

        JLabel lblDefault = new JLabel(TmmResourceBundle.getString("Settings.default"));
        panelPatterns.add(lblDefault, "cell 1 3 2 1,alignx right");
        TmmFontHelper.changeFont(lblDefault, L2);

        JTextArea tpDefaultSeasonPattern = new ReadOnlyTextArea(TvShowSettings.DEFAULT_RENAMER_SEASON_PATTERN);
        panelPatterns.add(tpDefaultSeasonPattern, "cell 3 3,growx,wmin 0");
        TmmFontHelper.changeFont(tpDefaultSeasonPattern, L2);
      }
      {
        JLabel lblEpisodeFileName = new JLabel(TmmResourceBundle.getString("Settings.tvshowfilename"));
        panelPatterns.add(lblEpisodeFileName, "cell 1 4 2 1,alignx right");

        tfEpisodeFilename = new TmmRoundTextArea();
        panelPatterns.add(tfEpisodeFilename, "cell 3 4, growx, wmin 0");

        JButton btnReset = new FlatButton(IconManager.UNDO_GREY);
        btnReset.setToolTipText(TmmResourceBundle.getString("Settings.renamer.reverttodefault"));
        btnReset.addActionListener(l -> tfEpisodeFilename.setText(TvShowSettings.DEFAULT_RENAMER_FILE_PATTERN));
        panelPatterns.add(btnReset, "cell 3 4, aligny top");

        JLabel lblDefault = new JLabel(TmmResourceBundle.getString("Settings.default"));
        panelPatterns.add(lblDefault, "cell 1 5 2 1,alignx right");
        TmmFontHelper.changeFont(lblDefault, L2);

        JTextArea tpDefaultFilePattern = new ReadOnlyTextArea(TvShowSettings.DEFAULT_RENAMER_FILE_PATTERN);
        panelPatterns.add(tpDefaultFilePattern, "cell 3 5,growx,wmin 0");
        TmmFontHelper.changeFont(tpDefaultFilePattern, L2);
      }
      {
        JLabel lblRenamerHintT = new JLabel(TmmResourceBundle.getString("Settings.tvshow.renamer.hint"));
        panelPatterns.add(lblRenamerHintT, "cell 1 6 3 1");
      }
      {
        JButton btnJmteExplorer = new JButton(TmmResourceBundle.getString("jmteexplorer.title"));
        btnJmteExplorer.addActionListener(e -> {
          TvShowJmteExplorerDialog dialog = new TvShowJmteExplorerDialog((JDialog) this.getTopLevelAncestor());
          dialog.setVisible(true);
        });
        panelPatterns.add(btnJmteExplorer, "cell 4 0");
      }
    }
    {
      JPanel panelAdvancedOptions = new JPanel();
      panelAdvancedOptions.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][][][]")); // 16lp ~ width of the

      JLabel lblAdvancedOptions = new TmmLabel(TmmResourceBundle.getString("Settings.advancedoptions"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelAdvancedOptions, lblAdvancedOptions, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#advanced-options-3"));
      add(collapsiblePanel, "cell 0 2,growx");

      {
        chckbxAutomaticRename = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.automaticrename"));
        panelAdvancedOptions.add(chckbxAutomaticRename, "cell 1 0 2 1");

        JLabel lblAutomaticRenameHint = new JLabel(IconManager.HINT);
        lblAutomaticRenameHint.setToolTipText(TmmResourceBundle.getString("Settings.tvshow.automaticrename.desc"));
        panelAdvancedOptions.add(lblAutomaticRenameHint, "cell 1 0 2 1");

        chckbxShowFoldernameSpaceReplacement = new JCheckBox(TmmResourceBundle.getString("Settings.renamer.showfolderspacereplacement"));
        chckbxShowFoldernameSpaceReplacement.setToolTipText(TmmResourceBundle.getString("Settings.renamer.folderspacereplacement.hint"));
        panelAdvancedOptions.add(chckbxShowFoldernameSpaceReplacement, "cell 1 1 2 1");

        cbShowFoldernameSpaceReplacement = new JComboBox(spaceReplacements.toArray());
        panelAdvancedOptions.add(cbShowFoldernameSpaceReplacement, "cell 1 1 2 1");

        chckbxSeasonFoldernameSpaceReplacement = new JCheckBox(TmmResourceBundle.getString("Settings.renamer.seasonfolderspacereplacement"));
        chckbxSeasonFoldernameSpaceReplacement.setToolTipText(TmmResourceBundle.getString("Settings.renamer.folderspacereplacement.hint"));
        panelAdvancedOptions.add(chckbxSeasonFoldernameSpaceReplacement, "cell 1 2 2 1");

        cbSeasonFoldernameSpaceReplacement = new JComboBox(spaceReplacements.toArray());
        panelAdvancedOptions.add(cbSeasonFoldernameSpaceReplacement, "cell 1 2 2 1");
      }
      {
        chckbxFilenameSpaceReplacement = new JCheckBox(TmmResourceBundle.getString("Settings.renamer.spacereplacement"));
        chckbxFilenameSpaceReplacement.setToolTipText(TmmResourceBundle.getString("Settings.renamer.spacereplacement.hint"));
        panelAdvancedOptions.add(chckbxFilenameSpaceReplacement, "cell 1 3 2 1");

        cbFilenameSpaceReplacement = new JComboBox(spaceReplacements.toArray());
        panelAdvancedOptions.add(cbFilenameSpaceReplacement, "cell 1 3 2 1");
      }
      {
        JLabel lblColonReplacement = new JLabel(TmmResourceBundle.getString("Settings.renamer.colonreplacement"));
        panelAdvancedOptions.add(lblColonReplacement, "cell 1 4 2 1");
        lblColonReplacement.setToolTipText(TmmResourceBundle.getString("Settings.renamer.colonreplacement.hint"));

        cbColonReplacement = new JComboBox(colonReplacements.toArray());
        panelAdvancedOptions.add(cbColonReplacement, "cell 1 4 2 1");
      }
      {
        chckbxAsciiReplacement = new JCheckBox(TmmResourceBundle.getString("Settings.renamer.asciireplacement"));
        panelAdvancedOptions.add(chckbxAsciiReplacement, "cell 1 5 2 1");

        JLabel lblAsciiHint = new JLabel(TmmResourceBundle.getString("Settings.renamer.asciireplacement.hint"));
        panelAdvancedOptions.add(lblAsciiHint, "cell 2 6");
        TmmFontHelper.changeFont(lblAsciiHint, L2);
      }
      {
        chckbxCleanupUnwanted = new JCheckBox(TmmResourceBundle.getString("Settings.cleanupfiles"));
        panelAdvancedOptions.add(chckbxCleanupUnwanted, "cell 1 7 2 1");
      }
      {
        JLabel lblFirstCharacterT = new JLabel(TmmResourceBundle.getString("Settings.renamer.firstnumbercharacterreplacement"));
        panelAdvancedOptions.add(lblFirstCharacterT, "flowx,cell 1 8 2 1");

        tfFirstCharacter = new JTextField();
        panelAdvancedOptions.add(tfFirstCharacter, "cell 1 9 2 1");
        tfFirstCharacter.setColumns(2);
      }
    }
    {
      JPanel panelExample = new JPanel();
      panelExample.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][][300lp,grow]", "[][]10lp![][][]"));

      JLabel lblAdvancedOptions = new TmmLabel(TmmResourceBundle.getString("Settings.example"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelExample, lblAdvancedOptions, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#example"));
      add(collapsiblePanel, "cell 0 4,growx, wmin 0");
      {
        JLabel lblExampleTvShowT = new JLabel(TmmResourceBundle.getString("metatag.tvshow"));
        panelExample.add(lblExampleTvShowT, "cell 1 0");

        cbTvShowForPreview = new JComboBox();
        panelExample.add(cbTvShowForPreview, "cell 2 0,growx,wmin 0");
      }
      {
        JLabel lblExampleEpisodeT = new JLabel(TmmResourceBundle.getString("metatag.episode"));
        panelExample.add(lblExampleEpisodeT, "cell 1 1");

        cbEpisodeForPreview = new JComboBox();
        panelExample.add(cbEpisodeForPreview, "cell 2 1,growx,wmin 0");
      }
      {
        JLabel lblDatasourceT = new TmmLabel(TmmResourceBundle.getString("metatag.datasource"));
        panelExample.add(lblDatasourceT, "cell 1 2");

        lblExampleDatasource = new LinkLabel("");
        panelExample.add(lblExampleDatasource, "cell 2 2, wmin 0");

        JLabel lblFoldernameT = new TmmLabel(TmmResourceBundle.getString("Settings.renamer.folder"));
        panelExample.add(lblFoldernameT, "cell 1 3");

        lblExampleFoldername = new JLabel("");
        panelExample.add(lblExampleFoldername, "cell 2 3, wmin 0");

        JLabel lblFilenameT = new TmmLabel(TmmResourceBundle.getString("Settings.renamer.file"));
        panelExample.add(lblFilenameT, "cell 1 4");

        lblExampleFilename = new JLabel("");
        panelExample.add(lblExampleFilename, "cell 2 4, wmin 0");
      }
    }
  }

  @Override
  public void hierarchyChanged(HierarchyEvent arg0) {
    if (isShowing()) {
      buildAndInstallTvShowArray();
      buildAndInstallEpisodeArray();
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

  private void buildAndInstallTvShowArray() {
    cbTvShowForPreview.removeAllItems();
    List<TvShow> allTvShows = new ArrayList<>(TvShowModuleManager.getInstance().getTvShowList().getTvShows());
    allTvShows.sort(new TvShowComparator());
    TvShow sel = TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShow();
    for (TvShow tvShow : allTvShows) {
      TvShowPreviewContainer container = new TvShowPreviewContainer();
      container.tvShow = tvShow;
      cbTvShowForPreview.addItem(container);
      if (sel != null && tvShow.equals(sel)) {
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
        TvShowEpisodePreviewContainer container = new TvShowEpisodePreviewContainer();
        container.episode = episode;
        cbEpisodeForPreview.addItem(container);
        if (sel != null && sel.size() > 0 && episode.equals(sel.get(0))) {
          cbEpisodeForPreview.setSelectedItem(container);
        }
      }
    }
  }

  private void createRenamerExample() {
    // need to start it async, that binding will transfer changes to settings first
    SwingUtilities.invokeLater(() -> {
      TvShow tvShow = null;
      TvShowEpisode episode = null;

      if (cbTvShowForPreview.getSelectedItem() instanceof TvShowPreviewContainer) {
        TvShowPreviewContainer container = (TvShowPreviewContainer) cbTvShowForPreview.getSelectedItem();
        tvShow = container.tvShow;
      }

      if (cbEpisodeForPreview.getSelectedItem() instanceof TvShowEpisodePreviewContainer) {
        TvShowEpisodePreviewContainer container = (TvShowEpisodePreviewContainer) cbEpisodeForPreview.getSelectedItem();
        episode = container.episode;
      }

      if (tvShow != null && episode != null) {
        String tvShowDir = TvShowRenamer.getTvShowFoldername(tfTvShowFolder.getText(), tvShow);
        MediaFile episodeMf = TvShowRenamer
            .generateEpisodeFilenames(tfEpisodeFilename.getText(), tvShow, episode.getMainVideoFile(),
                FilenameUtils.getBaseName(episode.getMainVideoFile().getFilename()))
            .get(0);

        String newFilenameAndPath = episodeMf.getFile().toString().replace(episode.getTvShow().getPath() + File.separator, "");

        lblExampleDatasource.setText(tvShow.getDataSource());
        lblExampleFoldername.setText(tvShowDir.replace(tvShow.getDataSource() + File.separator, ""));
        lblExampleFilename.setText(newFilenameAndPath);

      }
      else {
        lblExampleDatasource.setText("");
        lblExampleFoldername.setText("");
        lblExampleFilename.setText("");
      }
    });
  }

  private void checkChanges() {
    // show folder name space replacement
    String spaceReplacement = (String) cbShowFoldernameSpaceReplacement.getSelectedItem();
    settings.setRenamerShowPathnameSpaceReplacement(spaceReplacement);

    // season folder name space replacement
    spaceReplacement = (String) cbSeasonFoldernameSpaceReplacement.getSelectedItem();
    settings.setRenamerSeasonPathnameSpaceReplacement(spaceReplacement);

    // filename space replacement
    spaceReplacement = (String) cbFilenameSpaceReplacement.getSelectedItem();
    settings.setRenamerFilenameSpaceReplacement(spaceReplacement);

    String colonReplacement = (String) cbColonReplacement.getSelectedItem();
    settings.setRenamerColonReplacement(colonReplacement);
  }

  /*************************************************************
   * helper classes
   *************************************************************/
  private static class TvShowPreviewContainer {
    TvShow tvShow;

    @Override
    public String toString() {
      return tvShow.getTitle();
    }
  }

  private static class TvShowEpisodePreviewContainer {
    TvShowEpisode episode;

    @Override
    public String toString() {
      return episode.getSeason() + "." + episode.getEpisode() + " " + episode.getTitle();
    }
  }

  private static class TvShowComparator implements Comparator<TvShow> {
    @Override
    public int compare(TvShow arg0, TvShow arg1) {
      return arg0.getTitle().compareTo(arg1.getTitle());
    }
  }

  @SuppressWarnings("unused")
  private static class TvShowRenamerExample extends AbstractModelObject {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^\\$\\{(.*?)([\\}\\[;\\.]+.*)");

    private final String         token;
    private final String         completeToken;

    private String               longToken     = "";
    private String               description;
    private String               example       = "";

    public TvShowRenamerExample(String token) {
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

    private void createExample(TvShowEpisode episode) {
      String oldValue = example;
      if (episode == null) {
        example = "";
      }
      else {
        example = TvShowRenamer.createDestination(token, Collections.singletonList(episode));
      }
      firePropertyChange("example", oldValue, example);
    }
  }

  private static class TvShowRenamerExampleTableFormat extends TmmTableFormat<TvShowRenamerExample> {
    public TvShowRenamerExampleTableFormat() {
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

  protected void initDataBindings() {
    Property settingsBeanProperty_6 = BeanProperty.create("asciiReplacement");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_6, chckbxAsciiReplacement,
        jCheckBoxBeanProperty);
    autoBinding_5.bind();
    //
    Property tvShowSettingsBeanProperty = BeanProperty.create("renamerShowPathnameSpaceSubstitution");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty,
        chckbxShowFoldernameSpaceReplacement, jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property tvShowSettingsBeanProperty_7 = BeanProperty.create("renamerSeasonPathnameSpaceSubstitution");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_7,
        chckbxSeasonFoldernameSpaceReplacement, jCheckBoxBeanProperty);
    autoBinding_6.bind();
    //
    Property tvShowSettingsBeanProperty_8 = BeanProperty.create("renamerFilenameSpaceSubstitution");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_8,
        chckbxFilenameSpaceReplacement, jCheckBoxBeanProperty);
    autoBinding_7.bind();
    //
    Property tvShowSettingsBeanProperty_1 = BeanProperty.create("renamerTvShowFoldername");
    Property jTextFieldBeanProperty_1 = BeanProperty.create("text");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_1, tfTvShowFolder,
        jTextFieldBeanProperty_1);
    autoBinding.bind();
    //
    Property tvShowSettingsBeanProperty_2 = BeanProperty.create("renamerFilename");
    Property jTextFieldBeanProperty_2 = BeanProperty.create("text");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_2, tfEpisodeFilename,
        jTextFieldBeanProperty_2);
    autoBinding_1.bind();
    //
    Property tvShowSettingsBeanProperty_3 = BeanProperty.create("renamerSeasonFoldername");
    Property jTextFieldBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_3, tfSeasonFolderName,
        jTextFieldBeanProperty);
    autoBinding_2.bind();
    //
    Property tvShowSettingsBeanProperty_4 = BeanProperty.create("renamerFirstCharacterNumberReplacement");
    Property jTextFieldBeanProperty_3 = BeanProperty.create("text");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_4, tfFirstCharacter,
        jTextFieldBeanProperty_3);
    autoBinding_3.bind();
    //
    Property tvShowSettingsBeanProperty_5 = BeanProperty.create("renameAfterScrape");
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_5, chckbxAutomaticRename,
        jCheckBoxBeanProperty);
    autoBinding_8.bind();
    //
    Property tvShowSettingsBeanProperty_6 = BeanProperty.create("renamerCleanupUnwanted");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_6, chckbxCleanupUnwanted,
        jCheckBoxBeanProperty);
    autoBinding_9.bind();
  }
}
