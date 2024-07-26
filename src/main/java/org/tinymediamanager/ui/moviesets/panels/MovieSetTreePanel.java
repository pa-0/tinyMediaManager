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

package org.tinymediamanager.ui.moviesets.panels;

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
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
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
import org.tinymediamanager.ui.moviesets.MovieSetSelectionModel;
import org.tinymediamanager.ui.moviesets.MovieSetTableFormat;
import org.tinymediamanager.ui.moviesets.MovieSetTreeCellRenderer;
import org.tinymediamanager.ui.moviesets.MovieSetTreeDataProvider;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;
import org.tinymediamanager.ui.moviesets.actions.MovieSetEditAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

import net.miginfocom.swing.MigLayout;

public class MovieSetTreePanel extends TmmListPanel {
  private final MovieList              movieList = MovieModuleManager.getInstance().getMovieList();
  private final MovieSetSelectionModel selectionModel;

  private int                          rowcount;
  private long                         rowcountLastUpdate;

  private TmmTreeTable                 tree;
  private JLabel                       lblMovieCountFiltered;
  private JLabel                       lblMovieCountTotal;
  private JLabel                       lblMovieSetCountFiltered;
  private JLabel                       lblMovieSetCountTotal;
  private SplitButton                  btnFilter;
  private JLabel                       lblSelectedMovieCount;

  private JPopupMenu                   popupMenu;

  public MovieSetTreePanel(MovieSetSelectionModel movieSetSelectionModel) {
    initComponents();
    initDataBindings();

    this.selectionModel = movieSetSelectionModel;
    this.selectionModel.setTreeTable(tree);

    // initialize filteredCount
    updateFilteredCount();

    movieList.addPropertyChangeListener(evt -> {
      switch (evt.getPropertyName()) {
        case "movieSetCount":
        case "movieInMovieSetCount":
          updateFilteredCount();
          break;

        default:
          break;
      }
    });

    MovieModuleManager.getInstance().getSettings().addPropertyChangeListener(e -> {
      switch (e.getPropertyName()) {
        case "movieSetCheckMetadata":
        case "movieSetCheckArtwork":
          tree.invalidate();
          break;

        default:
          break;
      }
    });
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[200lp:n,grow][100lp:n,fill]", "[][400lp,grow]0[][][]"));

    final TmmTreeTextFilter<TmmTreeNode> searchField = new TmmTreeTextFilter<>();
    add(searchField, "cell 0 0,growx");

    // register global short cut for the search field
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, CTRL_DOWN_MASK), "search");
    getActionMap().put("search", new RequestFocusAction(searchField));

    btnFilter = new SplitButton(TmmResourceBundle.getString("movieextendedsearch.filter"));
    btnFilter.setToolTipText(TmmResourceBundle.getString("movieextendedsearch.options"));
    btnFilter.getActionButton().addActionListener(e -> MovieSetUIModule.getInstance().setFilterDialogVisible(true));
    btnFilter.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        JPopupMenu popupMenu = btnFilter.getPopupMenu();
        popupMenu.removeAll();

        MovieModuleManager.getInstance().getSettings().getMovieSetUiFilterPresets().keySet().stream().sorted().forEach(uiFilter -> {
          FilterPresetAction action = new FilterPresetAction(uiFilter) {
            @Override
            protected void processAction(ActionEvent e) {
              tree.setFilterValues(MovieModuleManager.getInstance().getSettings().getMovieSetUiFilterPresets().get(presetName));
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

    TmmTreeTableFormat<TmmTreeNode> tableFormat = new MovieSetTableFormat();
    tree = new TmmTreeTable(new MovieSetTreeDataProvider(tableFormat), tableFormat) {
      @Override
      public void storeFilters() {
        if (MovieModuleManager.getInstance().getSettings().isStoreMovieSetUiFilters()) {
          List<AbstractSettings.UIFilters> filterValues = new ArrayList<>();
          if (isFiltersActive()) {
            for (ITmmTreeFilter<TmmTreeNode> filter : treeFilters) {
              if (filter instanceof ITmmUIFilter) {
                ITmmUIFilter uiFilter = (ITmmUIFilter) filter;
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
          MovieModuleManager.getInstance().getSettings().setMovieSetUiFilters(filterValues);
          MovieModuleManager.getInstance().getSettings().saveSettings();
        }
      }
    };
    tree.getColumnModel().getColumn(0).setCellRenderer(new MovieSetTreeCellRenderer());
    tree.addPropertyChangeListener("filterChanged", evt -> updateFilterIndicator());

    tree.setName("movieSets.movieSetTree");
    TmmTreeTableComparatorChooser.install(tree);
    TmmUILayoutStore.getInstance().install(tree);

    tree.addFilter(searchField);
    JScrollPane scrollPane = new JScrollPane();
    tree.configureScrollPane(scrollPane);
    add(scrollPane, "cell 0 1 2 1,grow");
    tree.adjustColumnPreferredWidths(3);

    tree.setRootVisible(false);

    tree.getModel().addTableModelListener(arg0 -> {
      updateFilteredCount();

      if (tree.getTreeTableModel().getTreeModel() instanceof TmmTreeModel) {
        if (((TmmTreeModel<?>) tree.getTreeTableModel().getTreeModel()).isAdjusting()) {
          return;
        }
      }

      // select first movie set if nothing is selected
      ListSelectionModel selectionModel1 = tree.getSelectionModel();
      if (selectionModel1.isSelectionEmpty() && tree.getModel().getRowCount() > 0) {
        selectionModel1.setSelectionInterval(0, 0);
      }
    });

    tree.getSelectionModel().addListSelectionListener(arg0 -> {
      if (arg0.getValueIsAdjusting() || !(arg0.getSource() instanceof DefaultListSelectionModel)) {
        return;
      }

      // if nothing is in the tree, set the initial movie set
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
        // click on a movie set
        if (node.getUserObject() instanceof MovieSet) {
          MovieSet movieSet = (MovieSet) node.getUserObject();
          MovieSetUIModule.getInstance().setSelectedMovieSet(movieSet);
        }

        // click on a movie
        if (node.getUserObject() instanceof Movie) {
          Movie movie = (Movie) node.getUserObject();
          MovieSetUIModule.getInstance().setSelectedMovie(movie);
        }
      }
      else {
        MovieSetUIModule.getInstance().setSelectedMovieSet(null);
      }

      updateSelectionSums();
    });

    // selecting first movie set at startup
    if (movieList.getMovieSetList() != null && !movieList.getMovieSetList().isEmpty()) {
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
          new MovieSetEditAction().actionPerformed(new ActionEvent(e, 0, ""));
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
            if (model.getValueAt(i, 0) instanceof MovieSetTreeDataProvider.MovieSetTreeNode) {
              MovieSetTreeDataProvider.MovieSetTreeNode node = (MovieSetTreeDataProvider.MovieSetTreeNode) model.getValueAt(i, 0);

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

      JLabel lblMovieSetCount = new JLabel(TmmResourceBundle.getString("tmm.moviesets") + ":");
      panelTotals.add(lblMovieSetCount, "flowx,cell 0 0");

      lblMovieSetCountFiltered = new JLabel("");
      panelTotals.add(lblMovieSetCountFiltered, "cell 0 0");

      JLabel lblMovieSetCountOf = new JLabel(TmmResourceBundle.getString("tmm.of"));
      panelTotals.add(lblMovieSetCountOf, "cell 0 0");

      lblMovieSetCountTotal = new JLabel("");
      panelTotals.add(lblMovieSetCountTotal, "cell 0 0");

      JLabel lblMovieCount = new JLabel(TmmResourceBundle.getString("tmm.movies") + ":");
      panelTotals.add(lblMovieCount, "flowx,cell 0 1");

      lblMovieCountFiltered = new JLabel("");
      panelTotals.add(lblMovieCountFiltered, "cell 0 1");

      JLabel lblMovieCountOf = new JLabel(TmmResourceBundle.getString("tmm.of"));
      panelTotals.add(lblMovieCountOf, "cell 0 1");

      lblMovieCountTotal = new JLabel("");
      panelTotals.add(lblMovieCountTotal, "cell 0 1");

      lblSelectedMovieCount = new JLabel("");
      panelTotals.add(lblSelectedMovieCount, "cell 1 1");
    }
  }

  private void updateFilterIndicator() {
    boolean active = false;
    if (tree.isFiltersActive()) {
      for (ITmmTreeFilter<TmmTreeNode> filter : tree.getFilters()) {
        if (filter instanceof ITmmUIFilter) {
          ITmmUIFilter uiFilter = (ITmmUIFilter) filter;
          switch (uiFilter.getFilterState()) {
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
    }

    if (active) {
      btnFilter.getActionButton().setIcon(IconManager.FILTER_ACTIVE);
    }
    else {
      btnFilter.getActionButton().setIcon(null);
    }
  }

  private void updateFilteredCount() {
    int movieSetCount = 0;
    int movieCount = 0;

    // check rowcount if there has been a change in the display
    // if the row count from the last run matches with this, we assume that the tree did not change
    // the biggest error we can create here is to show a wrong count of filtered TV shows/episodes,
    // but we gain a ton of performance if we do not re-evaluate the count at every change
    int rowcount = tree.getTreeTableModel().getRowCount();
    long rowcountLastUpdate = System.currentTimeMillis();

    // update if the rowcount changed or at least after 2 seconds after the last update
    if (this.rowcount == rowcount && (rowcountLastUpdate - this.rowcountLastUpdate) < 2000) {
      return;
    }

    DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getTreeTableModel().getRoot();
    Enumeration<?> enumeration = root.depthFirstEnumeration();
    while (enumeration.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();

      Object userObject = node.getUserObject();

      if (userObject instanceof MovieSet) {
        movieSetCount++;
      }
      else if (userObject instanceof Movie && !(userObject instanceof MovieSet.MovieSetMovie)) {
        movieCount++;
      }
    }

    this.rowcount = rowcount;
    this.rowcountLastUpdate = rowcountLastUpdate;

    lblMovieSetCountFiltered.setText(String.valueOf(movieSetCount));
    lblMovieCountFiltered.setText(String.valueOf(movieCount));
  }

  private void updateSelectionSums() {
    List<Movie> movies = selectionModel.getSelectedMoviesRecursive();

    // episode
    String selectedMovies = TmmResourceBundle.getString("movie.selected").replace("{}", String.valueOf(movies.size()));
    double videoFileSize = movies.stream().mapToLong(Movie::getVideoFilesize).sum() / (1000.0 * 1000.0 * 1000);
    double totalFileSize = movies.stream().mapToLong(MediaEntity::getTotalFilesize).sum() / (1000.0 * 1000.0 * 1000);

    String text = String.format("%s (%.2f G)", selectedMovies, totalFileSize);
    lblSelectedMovieCount.setText(text);

    String selectedEpisodesHint = selectedMovies + " ("
        + TmmResourceBundle.getString("tmm.selected.hint1").replace("{}", String.format("%.2f G", videoFileSize)) + " / "
        + TmmResourceBundle.getString("tmm.selected.hint2").replace("{}", String.format("%.2f G", totalFileSize)) + ")";
    lblSelectedMovieCount.setToolTipText(selectedEpisodesHint);
  }

  public TmmTreeTable getTreeTable() {
    return tree;
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    this.popupMenu = popupMenu;

    if (popupMenu != null) {
      setComponentPopupMenu(popupMenu);
    }

    // add the tree menu entries on the bottom
    popupMenu.addSeparator();
    popupMenu.add(new MovieSetTreePanel.ExpandAllAction());
    popupMenu.add(new MovieSetTreePanel.CollapseAllAction());

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

  protected void initDataBindings() {
    BeanProperty<MovieList, Integer> movieListBeanProperty = BeanProperty.create("movieSetCount");
    BeanProperty<JLabel, String> jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding<MovieList, Integer, JLabel, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, movieList, movieListBeanProperty,
        lblMovieSetCountTotal, jLabelBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<MovieList, Integer> movieListBeanProperty_1 = BeanProperty.create("movieInMovieSetCount");
    AutoBinding<MovieList, Integer, JLabel, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, movieList,
        movieListBeanProperty_1, lblMovieCountTotal, jLabelBeanProperty);
    autoBinding_1.bind();
  }
}
