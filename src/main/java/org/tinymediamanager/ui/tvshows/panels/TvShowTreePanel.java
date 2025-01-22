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
package org.tinymediamanager.ui.tvshows.panels;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultListSelectionModel;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.ITmmUIFilter;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TablePopupListener;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.actions.ClearFilterPresetAction;
import org.tinymediamanager.ui.actions.FilterPresetAction;
import org.tinymediamanager.ui.actions.RequestFocusAction;
import org.tinymediamanager.ui.components.SplitButton;
import org.tinymediamanager.ui.components.TmmListPanel;
import org.tinymediamanager.ui.components.tree.ITmmTreeFilter;
import org.tinymediamanager.ui.components.tree.TmmTreeModel;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;
import org.tinymediamanager.ui.components.tree.TmmTreeTextFilter;
import org.tinymediamanager.ui.components.treetable.TmmTreeTable;
import org.tinymediamanager.ui.components.treetable.TmmTreeTableComparatorChooser;
import org.tinymediamanager.ui.components.treetable.TmmTreeTableFormat;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowTableFormat;
import org.tinymediamanager.ui.tvshows.TvShowTreeCellRenderer;
import org.tinymediamanager.ui.tvshows.TvShowTreeDataProvider;
import org.tinymediamanager.ui.tvshows.TvShowTreeTextFilter;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;
import org.tinymediamanager.ui.tvshows.actions.TvShowEditAction;

import net.miginfocom.swing.MigLayout;

/**
 * The class TvShowTreePanel is used to display the tree for TV dhows
 *
 * @author Manuel Laggner
 */
public class TvShowTreePanel extends TmmListPanel {
  private final TvShowList           tvShowList            = TvShowModuleManager.getInstance().getTvShowList();
  private final TvShowSelectionModel selectionModel;

  private TmmTreeTable               tree;
  private JLabel                     lblEpisodeCountFiltered;
  private JLabel                     lblEpisodeCountTotal;
  private JLabel                     lblTvShowCountFiltered;
  private JLabel                     lblTvShowCountTotal;
  private SplitButton                btnFilter;
  private JLabel                     lblSelectedEpisodeCount;

  private JPopupMenu                 popupMenu;

  private Timer                      totalCalculationTimer = null;

  public TvShowTreePanel(TvShowSelectionModel selectionModel) {
    initComponents();

    this.selectionModel = selectionModel;
    this.selectionModel.setTreeTable(tree);

    // initialize totals
    updateTotals();

    tvShowList.addPropertyChangeListener(evt -> {
      switch (evt.getPropertyName()) {
        case Constants.TV_SHOW_COUNT, Constants.EPISODE_COUNT:
          updateTotals();
          break;

        default:
          break;
      }
    });
    TvShowModuleManager.getInstance().getSettings().addPropertyChangeListener(e -> {
      switch (e.getPropertyName()) {
        case "tvShowCheckMetadata", "tvShowCheckArtwork", "seasonCheckArtwork", "episodeCheckMetadata", "episodeCheckArtwork",
            "episodeSpecialsCheckMissingMetadata", "episodeSpecialsCheckMissingArtwork" ->
          tree.invalidate();
      }
    });
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[200lp:n,grow][100lp:n,fill]", "[][200lp:n,grow]0[][][]"));

    final TmmTreeTextFilter<TmmTreeNode> searchField = new TvShowTreeTextFilter<>();
    add(searchField, "cell 0 0,growx");

    // register global shortcut for the search field
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, CTRL_DOWN_MASK), "search");
    getActionMap().put("search", new RequestFocusAction(searchField));

    btnFilter = new SplitButton(TmmResourceBundle.getString("movieextendedsearch.filter"));
    btnFilter.setToolTipText(TmmResourceBundle.getString("movieextendedsearch.options"));
    btnFilter.getActionButton().addActionListener(e -> TvShowUIModule.getInstance().setFilterDialogVisible(true));
    btnFilter.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        JPopupMenu popupMenu = btnFilter.getPopupMenu();
        popupMenu.removeAll();

        TvShowModuleManager.getInstance().getSettings().getUiFilterPresets().keySet().stream().sorted().forEach(uiFilter -> {
          FilterPresetAction action = new FilterPresetAction(uiFilter) {
            @Override
            protected void processAction(ActionEvent e) {
              tree.setFilterValues(TvShowModuleManager.getInstance().getSettings().getUiFilterPresets().get(presetName));
              tree.storeFilters();
            }
          };
          popupMenu.add(action);
        });
        if (popupMenu.getSubElements().length != 0) {
          popupMenu.addSeparator();
        }

        popupMenu.add(new ClearFilterPresetAction() {
          @Override
          protected void processAction(ActionEvent e) {
            tree.setFilterValues(Collections.emptyList());
            tree.storeFilters();
          }
        });

        popupMenu.pack();
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        // do nothing
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
        // do nothing
      }
    });

    add(btnFilter, "cell 1 0");

    TmmTreeTableFormat<TmmTreeNode> tableFormat = new TvShowTableFormat();
    tree = new TmmTreeTable(new TvShowTreeDataProvider(tableFormat), tableFormat) {
      @Override
      public void storeFilters() {
        if (TvShowModuleManager.getInstance().getSettings().isStoreUiFilters()) {
          List<AbstractSettings.UIFilters> filterValues = new ArrayList<>();
          if (isFiltersActive()) {
            for (ITmmTreeFilter<TmmTreeNode> filter : treeFilters) {
              if (filter instanceof ITmmUIFilter uiFilter) {
                if (uiFilter.getFilterState() != ITmmUIFilter.FilterState.INACTIVE) {
                  AbstractSettings.UIFilters uiFilters = new AbstractSettings.UIFilters();
                  uiFilters.id = uiFilter.getId();
                  uiFilters.state = uiFilter.getFilterState();
                  uiFilters.filterValue = uiFilter.getFilterValueAsString();
                  filterValues.add(uiFilters);
                }
              }
            }
          }
          TvShowModuleManager.getInstance().getSettings().setUiFilters(filterValues);
          TvShowModuleManager.getInstance().getSettings().saveSettings();
        }
      }
    };
    tree.getColumnModel().getColumn(0).setCellRenderer(new TvShowTreeCellRenderer());
    tree.addPropertyChangeListener("filterChanged", evt -> updateFilterIndicator());
    tree.setName("tvshows.tvshowTree");
    TmmTreeTableComparatorChooser.install(tree);
    TmmUILayoutStore.getInstance().install(tree);

    tree.addFilter(searchField);
    JScrollPane scrollPane = new JScrollPane();
    tree.configureScrollPane(scrollPane);
    add(scrollPane, "cell 0 1 2 1,grow");
    add(scrollPane, "cell 0 1 2 1,grow");
    tree.adjustColumnPreferredWidths(3);

    tree.setRootVisible(false);

    tree.getModel().addTableModelListener(arg0 -> {
      updateTotals();

      if (tree.getTreeTableModel().getTreeModel() instanceof TmmTreeModel) {
        if (((TmmTreeModel<?>) tree.getTreeTableModel().getTreeModel()).isAdjusting()) {
          return;
        }
      }

      // select first Tvshow if nothing is selected
      ListSelectionModel selectionModel1 = tree.getSelectionModel();
      if (selectionModel1.isSelectionEmpty() && tree.getModel().getRowCount() > 0) {
        selectionModel1.setSelectionInterval(0, 0);
      }
      else if (tree.getModel().getRowCount() == 0) {
        TvShowUIModule.getInstance().setSelectedTvShow(null);
      }
    });

    tree.getSelectionModel().addListSelectionListener(arg0 -> {
      if (arg0.getValueIsAdjusting() || !(arg0.getSource() instanceof DefaultListSelectionModel)) {
        return;
      }

      // if nothing is in the tree, set the initial TV show
      if (tree.getModel().getRowCount() == 0) {
        TvShowUIModule.getInstance().setSelectedTvShow(null);
        return;
      }

      if (tree.isAdjusting()) {
        // prevent flickering
        return;
      }

      int index = ((DefaultListSelectionModel) arg0.getSource()).getMinSelectionIndex();
      DefaultMutableTreeNode node = tree.getTreeNode(index);
      if (node != null) {
        // click on a tv show
        if (node.getUserObject() instanceof TvShow tvShow) {
          TvShowUIModule.getInstance().setSelectedTvShow(tvShow);
        }

        // click on a season
        if (node.getUserObject() instanceof TvShowSeason tvShowSeason) {
          TvShowUIModule.getInstance().setSelectedTvShowSeason(tvShowSeason);
        }

        // click on an episode
        if (node.getUserObject() instanceof TvShowEpisode tvShowEpisode) {
          TvShowUIModule.getInstance().setSelectedTvShowEpisode(tvShowEpisode);
        }
      }
      else {
        TvShowUIModule.getInstance().setSelectedTvShow(null);
      }

      updateSelectionSums();
    });

    // selecting first TV show at startup
    if (tvShowList.getTvShows() != null && !tvShowList.getTvShows().isEmpty()) {
      SwingUtilities.invokeLater(() -> {
        ListSelectionModel selectionModel1 = tree.getSelectionModel();
        if (selectionModel1.isSelectionEmpty() && tree.getModel().getRowCount() > 0) {
          selectionModel1.setSelectionInterval(0, 0);
        }
      });
    }

    // add double click listener
    MouseListener mouseListener = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if (e.getClickCount() == 2 && !e.isConsumed() && e.getButton() == MouseEvent.BUTTON1) {
          new TvShowEditAction().actionPerformed(new ActionEvent(e, 0, ""));
        }
      }
    };
    tree.addMouseListener(mouseListener);

    // context menu by keyboard
    InputMap inputMap = tree.getInputMap(WHEN_FOCUSED);
    ActionMap actionMap = tree.getActionMap();

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), "popup");
    actionMap.put("popup", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (popupMenu != null) {
          Rectangle rect = tree.getCellRect(tree.getSelectedRow(), 0, false);
          popupMenu.show(tree, rect.x + rect.width / 2, rect.y + rect.height / 2);
        }
      }
    });

    // add key listener
    KeyListener keyListener = new KeyAdapter() {
      private long   lastKeypress = 0;
      private String searchTerm   = "";

      @Override
      public void keyTyped(KeyEvent e) {
        long now = System.currentTimeMillis();
        if (now - lastKeypress > 500) {
          searchTerm = "";
        }
        lastKeypress = now;

        if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
          searchTerm += e.getKeyChar();
          searchTerm = searchTerm.toLowerCase();
        }

        if (StringUtils.isNotBlank(searchTerm)) {
          TableModel model = tree.getModel();

          for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getValueAt(i, 0) instanceof TvShowTreeDataProvider.TvShowTreeNode node) {
              // search in the title
              String title = node.toString().toLowerCase(Locale.ROOT);
              if (title.startsWith(searchTerm)) {
                tree.getSelectionModel().setSelectionInterval(i, i);
                tree.scrollRectToVisible(new Rectangle(tree.getCellRect(i, 0, true)));
                break;
              }
            }
          }
        }
      }
    };
    tree.addKeyListener(keyListener);

    JSeparator separator = new JSeparator();
    add(separator, "cell 0 2 2 1,growx");

    {
      JPanel panelTotals = new JPanel();
      add(panelTotals, "cell 0 3 2 1,grow");
      panelTotals.setLayout(new MigLayout("insets 0", "[100lp:n,grow][100lp:n,grow,right]", "[]"));

      JLabel lblTvShowCount = new JLabel(TmmResourceBundle.getString("tmm.tvshows") + ":");
      panelTotals.add(lblTvShowCount, "flowx,cell 0 0");

      lblTvShowCountFiltered = new JLabel("");
      panelTotals.add(lblTvShowCountFiltered, "cell 0 0");

      JLabel lblTvShowCountOf = new JLabel(TmmResourceBundle.getString("tmm.of"));
      panelTotals.add(lblTvShowCountOf, "cell 0 0");

      lblTvShowCountTotal = new JLabel("");
      panelTotals.add(lblTvShowCountTotal, "cell 0 0");

      JLabel lblEpisodeCount = new JLabel(TmmResourceBundle.getString("metatag.episodes") + ":");
      panelTotals.add(lblEpisodeCount, "flowx,cell 0 1");

      lblEpisodeCountFiltered = new JLabel("");
      panelTotals.add(lblEpisodeCountFiltered, "cell 0 1");

      JLabel lblEpisodeCountOf = new JLabel(TmmResourceBundle.getString("tmm.of"));
      panelTotals.add(lblEpisodeCountOf, "cell 0 1");

      lblEpisodeCountTotal = new JLabel("");
      panelTotals.add(lblEpisodeCountTotal, "cell 0 1");

      lblSelectedEpisodeCount = new JLabel("");
      panelTotals.add(lblSelectedEpisodeCount, "cell 1 1");
    }
  }

  private void updateFilterIndicator() {
    boolean active = false;
    if (tree.isFiltersActive()) {
      for (ITmmTreeFilter<TmmTreeNode> filter : tree.getFilters()) {
        if (filter instanceof ITmmUIFilter uiFilter) {
          switch (uiFilter.getFilterState()) {
            case ACTIVE, ACTIVE_NEGATIVE -> active = true;
          }

          if (active) {
            break;
          }
        }
      }
    }

    if (active) {
      btnFilter.getActionButton().setIcon(IconManager.FILTER_ACTIVE);
    }
    else {
      btnFilter.getActionButton().setIcon(null);
    }
  }

  private void updateTotals() {
    if (this.totalCalculationTimer != null) {
      return;
    }

    totalCalculationTimer = new Timer("updateTotals");

    TimerTask task = new TimerTask() {
      public void run() {
        SwingUtilities.invokeLater(() -> {
          // sum
          lblTvShowCountTotal.setText(String.valueOf(tvShowList.getTvShowCount()));
          int dummyEpisodeCount = 0;
          if (TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()) {
            dummyEpisodeCount = tvShowList.getDummyEpisodeCount();
          }
          if (dummyEpisodeCount > 0) {
            int episodeCount = tvShowList.getEpisodeCount();
            lblEpisodeCountTotal.setText(episodeCount + " (" + (episodeCount + dummyEpisodeCount) + ")");
          }
          else {
            lblEpisodeCountTotal.setText(String.valueOf(tvShowList.getEpisodeCount()));
          }

          // filtered
          int tvShowCount = 0;
          int episodeCount = 0;
          int virtualEpisodeCount = 0;

          DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getTreeTableModel().getRoot();
          Enumeration<?> enumeration = root.depthFirstEnumeration();
          while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();

            Object userObject = node.getUserObject();

            if (userObject instanceof TvShow) {
              tvShowCount++;
            }
            else if (userObject instanceof TvShowEpisode) {
              if (((TvShowEpisode) userObject).isDummy()) {
                virtualEpisodeCount++;
              }
              else {
                episodeCount++;
              }
            }
          }

          lblTvShowCountFiltered.setText(String.valueOf(tvShowCount));

          if (tvShowList.hasDummyEpisodes()) {
            lblEpisodeCountFiltered.setText(episodeCount + " (" + (episodeCount + virtualEpisodeCount) + ")");
          }
          else {
            lblEpisodeCountFiltered.setText(String.valueOf(episodeCount));
          }

          // re-set the timer for the next run
          totalCalculationTimer = null;
        });
      }
    };

    totalCalculationTimer.schedule(task, 100L);
  }

  private void updateSelectionSums() {
    List<TvShowEpisode> episodes = selectionModel.getSelectedEpisodes(true);

    // episode
    String selectedEpisodes = TmmResourceBundle.getString("episode.selected").replace("{}", String.valueOf(episodes.size()));
    double videoFileSize = episodes.stream().mapToLong(TvShowEpisode::getVideoFilesize).sum() / (1000.0 * 1000.0 * 1000);
    double totalFileSize = episodes.stream().mapToLong(MediaEntity::getTotalFilesize).sum() / (1000.0 * 1000.0 * 1000);

    String text = String.format("%s (%.2f G)", selectedEpisodes, totalFileSize);
    lblSelectedEpisodeCount.setText(text);

    String selectedEpisodesHint = selectedEpisodes + " ("
        + TmmResourceBundle.getString("tmm.selected.hint1").replace("{}", String.format("%.2f G", videoFileSize)) + " / "
        + TmmResourceBundle.getString("tmm.selected.hint2").replace("{}", String.format("%.2f G", totalFileSize)) + ")";
    lblSelectedEpisodeCount.setToolTipText(selectedEpisodesHint);
  }

  public TmmTreeTable getTreeTable() {
    return tree;
  }

  @Override
  public void setPopupMenu(JPopupMenu popupMenu) {
    this.popupMenu = popupMenu;

    if (popupMenu != null) {
      setComponentPopupMenu(popupMenu);
    }

    // add the tree menu entries on the bottom
    popupMenu.addSeparator();
    popupMenu.add(new ExpandAllAction());
    popupMenu.add(new CollapseAllAction());

    tree.addMouseListener(new TablePopupListener(popupMenu, tree));
  }

  /**************************************************************************
   * local helper classes
   **************************************************************************/
  public class CollapseAllAction extends AbstractAction {
    public CollapseAllAction() {
      putValue(NAME, TmmResourceBundle.getString("tree.collapseall"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      for (int i = tree.getRowCount() - 1; i >= 0; i--) {
        tree.collapseRow(i);
      }
    }
  }

  public class ExpandAllAction extends AbstractAction {
    public ExpandAllAction() {
      putValue(NAME, TmmResourceBundle.getString("tree.expandall"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int i = 0;
      do {
        tree.expandRow(i++);
      } while (i < tree.getRowCount());
    }
  }
}
