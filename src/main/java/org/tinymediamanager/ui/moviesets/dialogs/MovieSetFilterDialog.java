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

package org.tinymediamanager.ui.moviesets.dialogs;

import static org.tinymediamanager.ui.TmmFontHelper.L1;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmTabbedPane;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;
import org.tinymediamanager.ui.components.treetable.TmmTreeTable;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.moviesets.filters.IMovieSetUIFilter;
import org.tinymediamanager.ui.moviesets.filters.MovieSetDatasourceFilter;
import org.tinymediamanager.ui.moviesets.filters.MovieSetMissingMoviesFilter;
import org.tinymediamanager.ui.moviesets.filters.MovieSetNewMoviesFilter;
import org.tinymediamanager.ui.moviesets.filters.MovieSetWithMoreThanOneMovieFilter;
import org.tinymediamanager.ui.panels.FilterSavePanel;
import org.tinymediamanager.ui.panels.ModalPopupPanel;

import net.miginfocom.swing.MigLayout;

public class MovieSetFilterDialog extends TmmDialog {
  private static final String                          PANEL_COL_CONSTRAINTS = "[][][][200lp:250lp,grow]";

  private final TmmTreeTable                           treeTable;

  // map for storing which filter is in which panel
  private final Map<JPanel, Set<IMovieSetUIFilter<?>>> filterMap;
  private final Set<IMovieSetUIFilter<?>>              filters;

  private final JTabbedPane                            tabbedPane;
  private JComboBox<String>                            cbPreset;
  private JCheckBox                                    chkbxEnableAll;

  public MovieSetFilterDialog(TmmTreeTable treeTable) {
    super(TmmResourceBundle.getString("movieextendedsearch.options") + " - " + TmmResourceBundle.getString("tmm.moviesets"), "movieSetFilter");
    setModalityType(ModalityType.MODELESS);

    this.treeTable = treeTable;

    this.filterMap = new HashMap<>();
    this.filters = new HashSet<>();
    this.treeTable.addPropertyChangeListener("filterChanged", evt -> filterChanged());

    ActionListener actionListener = e -> {
      String filterName = (String) cbPreset.getSelectedItem();
      if (StringUtils.isNotBlank(filterName)) {
        treeTable.setFilterValues(MovieModuleManager.getInstance().getSettings().getMovieSetUiFilterPresets().get(filterName));
        treeTable.storeFilters();
      }
      else {
        treeTable.clearFilter();
      }
    };

    ActionListener resetFilter = e -> SwingUtilities.invokeLater(treeTable::clearFilter);

    {
      tabbedPane = new TmmTabbedPane();
      getContentPane().add(tabbedPane, BorderLayout.CENTER);

      {
        // panel Main
        JPanel panelMain = new JPanel(new MigLayout("", PANEL_COL_CONSTRAINTS, "[]"));
        tabbedPane.addTab(TmmResourceBundle.getString("metatag.details"), panelMain);

        panelMain.add(new TmmLabel(TmmResourceBundle.getString("movieextendedsearch.filterby")), "cell 0 0 2 1");

        panelMain.add(new JLabel(TmmResourceBundle.getString("filter.reset")), "cell 2 0, right");
        panelMain.add(new FlatButton(IconManager.DELETE, resetFilter), "cell 2 0, right, wrap");
        panelMain.add(Box.createHorizontalGlue(), "wrap");

        addFilter(new MovieSetNewMoviesFilter(), panelMain);
        addFilter(new MovieSetWithMoreThanOneMovieFilter(), panelMain);
        addFilter(new MovieSetDatasourceFilter(), panelMain);
        addFilter(new MovieSetMissingMoviesFilter(), panelMain);
      }
    }

    {
      // filter preset panel
      JPanel panelFilterPreset = new JPanel();
      panelFilterPreset.setLayout(new MigLayout("insets n 0 n 0", "[5lp!][10lp][150lp,grow][5lp!]", "[]"));

      JSeparator separator = new JSeparator();
      panelFilterPreset.add(separator, "cell 0 1 4 1,growx,aligny top");

      JLabel lblEnableAllT = new TmmLabel(TmmResourceBundle.getString("filter.enableall"));
      panelFilterPreset.add(lblEnableAllT, "cell 1 2, alignx trailing");

      chkbxEnableAll = new JCheckBox();
      chkbxEnableAll.setSelected(true);
      chkbxEnableAll.addActionListener(e -> treeTable.setFiltersActive(chkbxEnableAll.isSelected()));
      panelFilterPreset.add(chkbxEnableAll, "cell 2 2");

      JLabel lblFilterPresetT = new TmmLabel(TmmResourceBundle.getString("filter.presets"));
      panelFilterPreset.add(lblFilterPresetT, "cell 1 3, alignx trailing");

      cbPreset = new JComboBox<>();
      panelFilterPreset.add(cbPreset, "cell 2 3");

      JButton btnSavePreset = new FlatButton(IconManager.SAVE);
      btnSavePreset.setToolTipText(TmmResourceBundle.getString("filter.savepreset"));
      btnSavePreset.addActionListener(e -> {
        Set<AbstractSettings.UIFilters> activeUiFilters = getActiveUiFilters();
        if (!activeUiFilters.isEmpty()) {
          Map<String, List<AbstractSettings.UIFilters>> uiFilters = new HashMap<>(
              MovieModuleManager.getInstance().getSettings().getMovieSetUiFilterPresets());

          ModalPopupPanel popupPanel = createModalPopupPanel();
          popupPanel.setTitle(TmmResourceBundle.getString("filter.savepreset"));

          FilterSavePanel filterSavePanel = new FilterSavePanel(activeUiFilters, uiFilters);

          popupPanel.setOnCloseHandler(() -> {
            String savedPreset = filterSavePanel.getSavedPreset();
            if (StringUtils.isNotBlank(savedPreset)) {
              cbPreset.removeActionListener(actionListener);
              MovieModuleManager.getInstance().getSettings().setMovieSetUiFilterPresets(uiFilters);
              MovieModuleManager.getInstance().getSettings().saveSettings();
              loadPresets();
              cbPreset.setSelectedItem(savedPreset);
              cbPreset.addActionListener(actionListener);
            }
          });

          popupPanel.setContent(filterSavePanel);
          showModalPopupPanel(popupPanel);
        }
      });
      panelFilterPreset.add(btnSavePreset, "cell 2 3");

      JButton btnDeletePreset = new FlatButton(IconManager.DELETE_GRAY);
      btnDeletePreset.setToolTipText(TmmResourceBundle.getString("filter.remove"));
      btnDeletePreset.addActionListener(e -> {
        String filterName = (String) cbPreset.getSelectedItem();
        if (StringUtils.isBlank(filterName)) {
          return;
        }

        // display warning and ask the user again
        if (!TmmProperties.getInstance().getPropertyAsBoolean("movieset.hidefilterhint")) {
          JCheckBox checkBox = new JCheckBox(TmmResourceBundle.getString("tmm.donotshowagain"));
          TmmFontHelper.changeFont(checkBox, L1);
          checkBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

          Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
          Object[] params = { TmmResourceBundle.getString("filter.remove"), checkBox };
          int answer = JOptionPane.showOptionDialog(MainWindow.getInstance(), params, TmmResourceBundle.getString("filter.remove"),
              JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);

          // the user don't want to show this dialog again
          if (checkBox.isSelected()) {
            TmmProperties.getInstance().putProperty("movieset.hidefilterhint", String.valueOf(checkBox.isSelected()));
          }

          if (answer != JOptionPane.YES_OPTION) {
            return;
          }
        }

        Map<String, List<AbstractSettings.UIFilters>> uiFilters = new HashMap<>(
            MovieModuleManager.getInstance().getSettings().getMovieSetUiFilterPresets());
        if (uiFilters.remove(filterName) != null) {
          cbPreset.removeActionListener(actionListener);
          MovieModuleManager.getInstance().getSettings().setMovieSetUiFilterPresets(uiFilters);
          MovieModuleManager.getInstance().getSettings().saveSettings();
          loadPresets();
          cbPreset.addActionListener(actionListener);
        }
      });
      panelFilterPreset.add(btnDeletePreset, "cell 2 3");

      getContentPane().add(panelFilterPreset, BorderLayout.SOUTH);
    }

    {
      // init
      loadPresets();

      cbPreset.addActionListener(actionListener);
    }
  }

  private Set<AbstractSettings.UIFilters> getActiveUiFilters() {
    return new HashSet<>(IMovieSetUIFilter.morphToUiFilters(filters));
  }

  private void loadPresets() {
    String preset = (String) cbPreset.getSelectedItem();

    cbPreset.removeAllItems();
    cbPreset.addItem("");
    MovieModuleManager.getInstance().getSettings().getMovieSetUiFilterPresets().keySet().stream().sorted().forEach(key -> cbPreset.addItem(key));

    if (StringUtils.isNotBlank(preset)) {
      cbPreset.setSelectedItem(preset);
    }
  }

  /**
   * add a new filter to the panel and selection model
   *
   * @param filter
   *          the filter to be added
   * @param panel
   *          the panel to add the filter to
   */
  private void addFilter(IMovieSetUIFilter<TmmTreeNode> filter, JPanel panel) {
    panel.add(filter.getCheckBox(), "");
    panel.add(filter.getLabel(), "right");

    if (filter.getFilterComponent() != null) {
      panel.add(filter.getFilterComponent(), "wmin 100, grow, wrap");
    }
    else {
      panel.add(Box.createGlue(), "wrap");
    }

    filterMap.computeIfAbsent(panel, k -> new HashSet<>()).add(filter);
    filters.add(filter);

    treeTable.addFilter(filter);
  }

  /**
   * re-calculate if the active filter icon should be displayed
   */
  private void filterChanged() {
    for (Map.Entry<JPanel, Set<IMovieSetUIFilter<?>>> entry : filterMap.entrySet()) {
      boolean active = false;
      if (treeTable.isFiltersActive()) {
        for (IMovieSetUIFilter<?> filter : entry.getValue()) {
          switch (filter.getFilterState()) {
            case ACTIVE:
            case ACTIVE_NEGATIVE:
              active = true;
              break;

            default:
              break;
          }

          if (active) {
            break;
          }
        }
      }

      for (int i = 0; i < tabbedPane.getTabCount(); i++) {
        if (SwingUtilities.isDescendingFrom(entry.getKey(), tabbedPane.getComponentAt(i))) {
          if (active) {
            tabbedPane.setIconAt(i, IconManager.FILTER_ACTIVE);
          }
          else {
            tabbedPane.setIconAt(i, null);
          }

          break;
        }
      }
    }
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    pack();
  }

  @Override
  protected void initBottomPanel() {
    // no bottom line needed
  }
}
