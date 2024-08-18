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
package org.tinymediamanager.ui.movies.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.Cursor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.swingbinding.JListBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.tasks.MovieRemoveDatasourceTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.dialogs.ExchangeDatasourceDialog;
import org.tinymediamanager.ui.panels.IModalPopupPanelProvider;
import org.tinymediamanager.ui.panels.ModalPopupPanel;
import org.tinymediamanager.ui.panels.RegexInputPanel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieDatasourceSettingsPanel.
 * 
 * @author Manuel Laggner
 */
class MovieDatasourceSettingsPanel extends JPanel {
  private final MovieSettings settings = MovieModuleManager.getInstance().getSettings();

  private JTextField          tfAddBadword;
  private JList<String>       listBadWords;
  private JPanel              panelDatasources;
  private JList<String>       listDatasources;
  private JPanel              panelIgnore;
  private JList<String>       listSkipFolder;
  private JButton             btnRemoveDatasource;
  private JButton             btnAddDatasource;
  private JButton             btnAddSkipFolder;
  private JButton             btnAddSkipRegexp;
  private JButton             btnRemoveSkipFolder;
  private JButton             btnRemoveBadWord;
  private JButton             btnAddBadWord;
  private JButton             btnMoveUpDatasoure;
  private JButton             btnMoveDownDatasource;
  private JButton             btnExchangeDatasource;

  /**
   * Instantiates a new movie settings panel.
   */
  MovieDatasourceSettingsPanel() {
    // UI initializations
    initComponents();
    initDataBindings();

    // data init
    // listeners
    btnRemoveDatasource.addActionListener(arg0 -> {
      int row = listDatasources.getSelectedIndex();
      if (row != -1) { // nothing selected
        String path = MovieModuleManager.getInstance().getSettings().getMovieDataSource().get(row);
        String[] choices = { TmmResourceBundle.getString("Button.continue"), TmmResourceBundle.getString("Button.abort") };
        int decision = JOptionPane.showOptionDialog(this, String.format(TmmResourceBundle.getString("Settings.movie.datasource.remove.info"), path),
            TmmResourceBundle.getString("Settings.datasource.remove"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices,
            TmmResourceBundle.getString("Button.abort"));
        if (decision == JOptionPane.YES_OPTION) {
          // to avoid interfering with active running tasks, we remove the data source as soon as all running main tasks are finished
          if (TmmTaskManager.getInstance().isMainTaskRunning()) {
            JOptionPane.showMessageDialog(this, TmmResourceBundle.getString("Settings.datasource.remove.hint"));
          }
          TmmTaskManager.getInstance().addMainTask(new MovieRemoveDatasourceTask(path));
        }
      }
    });
    btnAddDatasource.addActionListener(arg0 -> {
      String path = TmmProperties.getInstance().getProperty("movie.datasource.path");
      Path file = TmmUIHelper.selectDirectory(TmmResourceBundle.getString("Settings.datasource.folderchooser"), path);
      if (file != null && Files.isDirectory(file)) {
        settings.addMovieDataSources(file.toAbsolutePath().toString());
        TmmProperties.getInstance().putProperty("movie.datasource.path", file.toAbsolutePath().toString());
        panelDatasources.revalidate();
      }
    });

    btnAddSkipFolder.addActionListener(e -> {
      String path = TmmProperties.getInstance().getProperty("movie.ignore.path");
      Path file = TmmUIHelper.selectDirectory(TmmResourceBundle.getString("Settings.ignore"), path);
      if (file != null && Files.isDirectory(file)) {
        settings.addSkipFolder(file.toAbsolutePath().toString());
        TmmProperties.getInstance().putProperty("movie.ignore.path", file.toAbsolutePath().toString());
        panelIgnore.revalidate();
      }
    });

    btnAddSkipRegexp.addActionListener(e -> {
      IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(this);
      if (iModalPopupPanelProvider == null) {
        return;
      }

      ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
      popupPanel.setTitle(TmmResourceBundle.getString("tmm.regexp"));

      RegexInputPanel regexInputPanel = new RegexInputPanel();

      popupPanel.setOnCloseHandler(() -> {
        if (StringUtils.isNotBlank(regexInputPanel.getRegularExpression())) {
          settings.addSkipFolder(regexInputPanel.getRegularExpression());
          panelIgnore.revalidate();
        }
      });

      popupPanel.setContent(regexInputPanel);
      iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
    });

    btnRemoveSkipFolder.addActionListener(e -> {
      int row = listSkipFolder.getSelectedIndex();
      if (row != -1) { // nothing selected
        String ignore = settings.getSkipFolder().get(row);
        settings.removeSkipFolder(ignore);
        panelIgnore.revalidate();
      }
    });

    btnAddBadWord.addActionListener(e -> {
      if (StringUtils.isNotEmpty(tfAddBadword.getText())) {
        try {
          Pattern.compile(tfAddBadword.getText());
        }
        catch (PatternSyntaxException ex) {
          JOptionPane.showMessageDialog(this, TmmResourceBundle.getString("message.regex.error"));
          return;
        }
        MovieModuleManager.getInstance().getSettings().addBadWord(tfAddBadword.getText());
        tfAddBadword.setText("");
      }
    });

    btnRemoveBadWord.addActionListener(arg0 -> {
      int row = listBadWords.getSelectedIndex();
      if (row != -1) {
        String badWord = MovieModuleManager.getInstance().getSettings().getBadWord().get(row);
        MovieModuleManager.getInstance().getSettings().removeBadWord(badWord);
      }
    });

    btnMoveUpDatasoure.addActionListener(arg0 -> {
      int row = listDatasources.getSelectedIndex();
      if (row != -1 && row != 0) {
        settings.swapMovieDataSource(row, row - 1);
        row = row - 1;
        listDatasources.setSelectedIndex(row);
        listDatasources.updateUI();
      }
    });

    btnMoveDownDatasource.addActionListener(arg0 -> {
      int row = listDatasources.getSelectedIndex();
      if (row != -1 && row < listDatasources.getModel().getSize() - 1) {
        settings.swapMovieDataSource(row, row + 1);
        row = row + 1;
        listDatasources.setSelectedIndex(row);
        listDatasources.updateUI();
      }
    });

    btnExchangeDatasource.addActionListener(arg0 -> {
      int row = listDatasources.getSelectedIndex();
      if (row != -1) { // nothing selected
        String path = MovieModuleManager.getInstance().getSettings().getMovieDataSource().get(row);
        ExchangeDatasourceDialog dialog = new ExchangeDatasourceDialog(path);
        dialog.setVisible(true);

        if (StringUtils.isNotBlank(dialog.getNewDatasource())) {
          MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          MovieModuleManager.getInstance().getSettings().exchangeMovieDatasource(path, dialog.getNewDatasource());
          MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          panelDatasources.revalidate();
        }
      }
    });
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][][15lp!][]"));
    {
      panelDatasources = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][400lp:n][][grow]", "[150lp,grow][]"));

      JLabel lblDatasourcesT = new TmmLabel(TmmResourceBundle.getString("Settings.source"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelDatasources, lblDatasourcesT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#data-sources"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        JScrollPane scrollPaneDataSources = new JScrollPane();
        scrollPaneDataSources.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panelDatasources.add(scrollPaneDataSources, "cell 1 0 1 2,grow");

        listDatasources = new JList();
        listDatasources.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollPaneDataSources.setViewportView(listDatasources);

        btnAddDatasource = new SquareIconButton(IconManager.ADD_INV);
        panelDatasources.add(btnAddDatasource, "flowy,cell 2 0,growx,aligny top");
        btnAddDatasource.setToolTipText(TmmResourceBundle.getString("Button.add"));

        btnRemoveDatasource = new SquareIconButton(IconManager.REMOVE_INV);
        panelDatasources.add(btnRemoveDatasource, "cell 2 0,growx,aligny top");
        btnRemoveDatasource.setToolTipText(TmmResourceBundle.getString("Button.remove"));

        btnMoveUpDatasoure = new SquareIconButton(IconManager.ARROW_UP_INV);
        panelDatasources.add(btnMoveUpDatasoure, "cell 2 0,growx,aligny top");
        btnMoveUpDatasoure.setToolTipText(TmmResourceBundle.getString("Button.moveup"));

        btnMoveDownDatasource = new SquareIconButton(IconManager.ARROW_DOWN_INV);
        panelDatasources.add(btnMoveDownDatasource, "cell 2 0,growx,aligny top");
        btnMoveDownDatasource.setToolTipText(TmmResourceBundle.getString("Button.movedown"));

        btnExchangeDatasource = new SquareIconButton(IconManager.EXCHANGE);
        btnExchangeDatasource.setToolTipText(TmmResourceBundle.getString("Settings.exchangedatasource.desc"));
        panelDatasources.add(btnExchangeDatasource, "cell 2 1");
      }
    }

    {
      panelIgnore = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][400lp][][grow]", "[150lp,grow]"));

      JLabel lblIgnoreT = new TmmLabel(TmmResourceBundle.getString("Settings.ignore"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelIgnore, lblIgnoreT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#exclude-folders-from-scan"));
      add(collapsiblePanel, "cell 0 2,growx,wmin 0");
      {
        JScrollPane scrollPaneIgnore = new JScrollPane();
        scrollPaneIgnore.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panelIgnore.add(scrollPaneIgnore, "cell 1 0,grow");

        listSkipFolder = new JList();
        scrollPaneIgnore.setViewportView(listSkipFolder);

        btnAddSkipFolder = new SquareIconButton(IconManager.ADD_INV);
        panelIgnore.add(btnAddSkipFolder, "flowy, cell 2 0, aligny top, growx");
        btnAddSkipFolder.setToolTipText(TmmResourceBundle.getString("Settings.addignore"));

        btnAddSkipRegexp = new SquareIconButton(IconManager.FILE_ADD_INV);
        panelIgnore.add(btnAddSkipRegexp, "flowy, cell 2 0, aligny top, growx");
        btnAddSkipRegexp.setToolTipText(TmmResourceBundle.getString("Settings.addignoreregexp"));

        btnRemoveSkipFolder = new SquareIconButton(IconManager.REMOVE_INV);
        panelIgnore.add(btnRemoveSkipFolder, "flowy, cell 2 0, aligny top, growx");
        btnRemoveSkipFolder.setToolTipText(TmmResourceBundle.getString("Settings.removeignore"));
      }
    }

    {
      JPanel panelBadWords = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][400lp:n][][grow]", "[][100lp,grow][]"));

      JLabel lblBadWordsT = new TmmLabel(TmmResourceBundle.getString("Settings.movie.badwords"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelBadWords, lblBadWordsT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#bad-words"));
      add(collapsiblePanel, "cell 0 4,growx,wmin 0");
      {
        JLabel lblBadWordsDesc = new JLabel(TmmResourceBundle.getString("Settings.movie.badwords.hint"));
        panelBadWords.add(lblBadWordsDesc, "cell 1 0 3 1");

        JScrollPane scrollPaneBadWords = new JScrollPane();
        scrollPaneBadWords.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panelBadWords.add(scrollPaneBadWords, "cell 1 1,grow");

        listBadWords = new JList();
        scrollPaneBadWords.setViewportView(listBadWords);

        btnRemoveBadWord = new SquareIconButton(IconManager.REMOVE_INV);
        panelBadWords.add(btnRemoveBadWord, "cell 2 1,aligny bottom");
        btnRemoveBadWord.setToolTipText(TmmResourceBundle.getString("Button.remove"));

        tfAddBadword = new JTextField();
        panelBadWords.add(tfAddBadword, "cell 1 2,growx");

        btnAddBadWord = new SquareIconButton(IconManager.ADD_INV);
        panelBadWords.add(btnAddBadWord, "cell 2 2, growx");
        btnAddBadWord.setToolTipText(TmmResourceBundle.getString("Button.add"));
      }
    }
  }

  protected void initDataBindings() {
    BeanProperty<MovieSettings, List<String>> settingsBeanProperty_6 = BeanProperty.create("badWord");
    JListBinding<String, MovieSettings, JList> jListBinding = SwingBindings.createJListBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_6, listBadWords);
    jListBinding.bind();
    //
    BeanProperty<MovieSettings, List<String>> settingsBeanProperty_4 = BeanProperty.create("movieDataSource");
    JListBinding<String, MovieSettings, JList> jListBinding_1 = SwingBindings.createJListBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_4, listDatasources);
    jListBinding_1.bind();
    //
    BeanProperty<MovieSettings, List<String>> settingsBeanProperty_12 = BeanProperty.create("skipFolder");
    JListBinding<String, MovieSettings, JList> jListBinding_2 = SwingBindings.createJListBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_12, listSkipFolder);
    jListBinding_2.bind();
  }
}
