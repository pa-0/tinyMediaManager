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
package org.tinymediamanager.ui.wizard;

import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

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
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.SquareIconButton;

import net.miginfocom.swing.MigLayout;

/**
 * The class MovieSourcePanel is used to maintain the movie data sources in the wizard
 * 
 * @author Manuel Laggner
 */
class MovieSourcePanel extends JPanel {
  private final MovieSettings settings = MovieModuleManager.getInstance().getSettings();

  private JList<String>       listDataSources;

  public MovieSourcePanel() {
    initComponents();
    initDataBindings();
  }

  /*
   * init components
   */
  private void initComponents() {
    setLayout(new MigLayout("", "[grow]", "[][200lp,grow]"));
    {
      JLabel lblMovieDataSources = new JLabel(TmmResourceBundle.getString("wizard.movie.datasources"));
      TmmFontHelper.changeFont(lblMovieDataSources, 1.3333, Font.BOLD);
      add(lblMovieDataSources, "cell 0 0");
    }
    JPanel panelMovieDataSources = new JPanel();

    add(panelMovieDataSources, "cell 0 1,grow");
    panelMovieDataSources.setLayout(new MigLayout("", "[grow][]", "[][200lp,grow]"));
    {
      JTextArea tpDatasourceHint = new ReadOnlyTextArea(TmmResourceBundle.getString("wizard.datasource.hint"));
      panelMovieDataSources.add(tpDatasourceHint, "cell 0 0 2 1,grow");
    }
    {
      JScrollPane scrollPaneDataSources = new JScrollPane();
      panelMovieDataSources.add(scrollPaneDataSources, "cell 0 1,grow");

      listDataSources = new JList<>();
      scrollPaneDataSources.setViewportView(listDataSources);
    }
    {
      JButton btnAdd = new SquareIconButton(IconManager.ADD_INV);
      panelMovieDataSources.add(btnAdd, "flowy,cell 1 1,aligny top");
      btnAdd.setToolTipText(TmmResourceBundle.getString("Button.add"));
      btnAdd.addActionListener(arg0 -> {
        String path = TmmProperties.getInstance().getProperty("movie.datasource.path");
        Path file = TmmUIHelper.selectDirectory(TmmResourceBundle.getString("Settings.datasource.folderchooser"), path);
        if (file != null && Files.isDirectory(file)) {
          MovieModuleManager.getInstance().getSettings().addMovieDataSources(file.toAbsolutePath().toString());
        }
      });

      JButton btnRemove = new SquareIconButton(IconManager.REMOVE_INV);
      panelMovieDataSources.add(btnRemove, "cell 1 1");
      btnRemove.setToolTipText(TmmResourceBundle.getString("Button.remove"));
      btnRemove.addActionListener(arg0 -> {
        int row = listDataSources.getSelectedIndex();
        if (row != -1) { // nothing selected
          String path = MovieModuleManager.getInstance().getSettings().getMovieDataSource().get(row);
          String[] choices = { TmmResourceBundle.getString("Button.continue"), TmmResourceBundle.getString("Button.abort") };
          int decision = JOptionPane.showOptionDialog(this, String.format(TmmResourceBundle.getString("Settings.movie.datasource.remove.info"), path),
              TmmResourceBundle.getString("Settings.datasource.remove"), JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, choices,
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
    }
  }

  protected void initDataBindings() {
    BeanProperty<MovieSettings, List<String>> settingsBeanProperty_4 = BeanProperty.create("movieDataSource");
    JListBinding<String, MovieSettings, JList> jListBinding_1 = SwingBindings.createJListBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_4, listDataSources);
    jListBinding_1.bind();
  }
}
