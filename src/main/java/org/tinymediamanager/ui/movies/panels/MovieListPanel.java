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
package org.tinymediamanager.ui.movies.panels;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.movie.MovieComparator;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.HintManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TablePopupListener;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.actions.ClearFilterPresetAction;
import org.tinymediamanager.ui.actions.FilterPresetAction;
import org.tinymediamanager.ui.actions.RequestFocusAction;
import org.tinymediamanager.ui.components.EnhancedTextField;
import org.tinymediamanager.ui.components.SplitButton;
import org.tinymediamanager.ui.components.TmmListPanel;
import org.tinymediamanager.ui.components.table.MouseKeyboardSortingStrategy;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.movies.MovieMatcherEditor;
import org.tinymediamanager.ui.movies.MovieSelectionModel;
import org.tinymediamanager.ui.movies.MovieTableFormat;
import org.tinymediamanager.ui.movies.MovieTextMatcherEditor;
import org.tinymediamanager.ui.movies.MovieUIModule;
import org.tinymediamanager.ui.movies.actions.MovieEditAction;
import org.tinymediamanager.ui.movies.filters.IMovieUIFilter;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.impl.gui.SortingState;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * @author Manuel Laggner
 */
public class MovieListPanel extends TmmListPanel {
  MovieSelectionModel selectionModel;

  private MovieList   movieList;

  private TmmTable    movieTable;
  private JLabel      lblMovieCountFiltered;
  private JLabel      lblMovieCountTotal;
  private SplitButton btnExtendedFilter;
  private JLabel      lblSelectedCount;

  private JPopupMenu  popupMenu;

  public MovieListPanel() {
    initComponents();
  }

  private void initComponents() {
    movieList = MovieModuleManager.getInstance().getMovieList();
    SortedList<Movie> sortedMovies = new SortedList<>(GlazedListsSwing.swingThreadProxyList((ObservableElementList<Movie>) movieList.getMovies()),
        new MovieComparator());
    sortedMovies.setMode(SortedList.AVOID_MOVING_ELEMENTS);

    setLayout(new MigLayout("", "[200lp:n,grow][100lp:n,fill]", "[][200lp:300lp,grow]0[][]"));

    JTextField searchField = EnhancedTextField.createSearchTextField();
    add(searchField, "cell 0 0,growx");

    // register global short cut for the search field
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, CTRL_DOWN_MASK), "search");
    getActionMap().put("search", new RequestFocusAction(searchField));

    MatcherEditor<Movie> textMatcherEditor = new MovieTextMatcherEditor(searchField);
    MovieMatcherEditor movieMatcherEditor = new MovieMatcherEditor();
    FilterList<Movie> extendedFilteredMovies = new FilterList<>(sortedMovies, movieMatcherEditor);
    FilterList<Movie> textFilteredMovies = new FilterList<>(extendedFilteredMovies, textMatcherEditor);
    selectionModel = new MovieSelectionModel(sortedMovies, textFilteredMovies, movieMatcherEditor);
    final TmmTableModel<Movie> movieTableModel = new TmmTableModel<>(textFilteredMovies, new MovieTableFormat());
    // movieTableModel.setManyToOneTableModelEventAdapter();

    // build the table
    movieTable = new TmmTable(movieTableModel);
    movieTable.setName("movies.movieTable");
    movieTable.installComparatorChooser(sortedMovies, new MovieTableSortingStrategy());
    movieTable.getTableComparatorChooser().appendComparator(0, 0, false);

    movieTable.adjustColumnPreferredWidths(3);
    TmmUILayoutStore.getInstance().install(movieTable);

    movieTableModel.addTableModelListener(arg0 -> {
      lblMovieCountFiltered.setText(String.valueOf(movieTableModel.getRowCount()));
      // select first movie if nothing is selected
      ListSelectionModel selectionModel1 = movieTable.getSelectionModel();
      if (selectionModel1.isSelectionEmpty() && movieTableModel.getRowCount() > 0) {
        selectionModel1.setSelectionInterval(0, 0);
      }
    });

    JScrollPane scrollPane = new JScrollPane();
    movieTable.configureScrollPane(scrollPane);
    add(scrollPane, "cell 0 1 2 1,grow");

    btnExtendedFilter = new SplitButton(TmmResourceBundle.getString("movieextendedsearch.filter"));
    btnExtendedFilter.setToolTipText(TmmResourceBundle.getString("movieextendedsearch.options"));
    btnExtendedFilter.getActionButton().addActionListener(e -> MovieUIModule.getInstance().setFilterDialogVisible(true));
    btnExtendedFilter.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        JPopupMenu popupMenu = btnExtendedFilter.getPopupMenu();
        popupMenu.removeAll();

        MovieModuleManager.getInstance().getSettings().getMovieUiFilterPresets().keySet().stream().sorted().forEach(uiFilter -> {
          FilterPresetAction action = new FilterPresetAction(uiFilter) {
            @Override
            protected void processAction(ActionEvent e) {
              MovieUIModule.getInstance()
                  .getSelectionModel()
                  .setFilterValues(MovieModuleManager.getInstance().getSettings().getMovieUiFilterPresets().get(presetName));
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
            MovieUIModule.getInstance().getSelectionModel().setFilterValues(Collections.emptyList());
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

    // add the hint for the table
    HintManager.getInstance().addHint(TmmResourceBundle.getString("hintmanager.rightclick"), movieTable, SwingConstants.TOP);

    selectionModel.addPropertyChangeListener("filterChanged", evt -> updateFilterIndicator());
    add(btnExtendedFilter, "cell 1 0");

    JSeparator separator = new JSeparator();
    add(separator, "cell 0 2 2 1, growx");

    {
      JPanel panelTotals = new JPanel();
      add(panelTotals, "cell 0 3 2 1,grow");
      panelTotals.setLayout(new MigLayout("insets 0", "[100lp:n,grow][100lp:n,grow,right]", "[]"));

      JLabel lblMovieCount = new JLabel(TmmResourceBundle.getString("tmm.movies") + ":");
      panelTotals.add(lblMovieCount, "cell 0 0");

      lblMovieCountFiltered = new JLabel("");
      panelTotals.add(lblMovieCountFiltered, "cell 0 0");

      JLabel lblMovieCountOf = new JLabel(TmmResourceBundle.getString("tmm.of"));
      panelTotals.add(lblMovieCountOf, "cell 0 0");

      lblMovieCountTotal = new JLabel("");
      panelTotals.add(lblMovieCountTotal, "cell 0 0");

      lblSelectedCount = new JLabel("");
      panelTotals.add(lblSelectedCount, "cell 1 0");
    }

    selectionModel.addPropertyChangeListener(MovieSelectionModel.SELECTED_MOVIES, evt -> updateSelectionSums());

    // initialize filteredCount
    lblMovieCountFiltered.setText(String.valueOf(movieTableModel.getRowCount()));

    initDataBindings();

    MovieModuleManager.getInstance().getSettings().addPropertyChangeListener(e -> {
      switch (e.getPropertyName()) {
        case "movieCheckMetadata":
        case "movieCheckArtwork":
          movieTable.invalidate();
          break;

        default:
          break;
      }
    });
  }

  private void updateSelectionSums() {
    String selectedMovies = TmmResourceBundle.getString("movie.selected")
        .replace("{}", String.valueOf(selectionModel.getSelectedMovies(true).size()));
    double videoFileSize = selectionModel.getSelectedMovies(true).stream().mapToLong(Movie::getVideoFilesize).sum() / (1000.0 * 1000.0 * 1000);
    double totalFileSize = selectionModel.getSelectedMovies(true).stream().mapToLong(MediaEntity::getTotalFilesize).sum() / (1000.0 * 1000.0 * 1000);

    String text = String.format("%s (%.2f G)", selectedMovies, totalFileSize);
    lblSelectedCount.setText(text);

    String selectedMoviesHint = selectedMovies + " ("
        + TmmResourceBundle.getString("tmm.selected.hint1").replace("{}", String.format("%.2f G", videoFileSize)) + " / "
        + TmmResourceBundle.getString("tmm.selected.hint2").replace("{}", String.format("%.2f G", totalFileSize)) + ")";
    lblSelectedCount.setToolTipText(selectedMoviesHint);
  }

  private void updateFilterIndicator() {
    boolean active = false;
    if (selectionModel.isFiltersActive()) {
      for (IMovieUIFilter filter : selectionModel.getMatcherEditor().getFilters()) {
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

    if (active) {
      btnExtendedFilter.getActionButton().setIcon(IconManager.FILTER_ACTIVE);
    }
    else {
      btnExtendedFilter.getActionButton().setIcon(null);
    }
  }

  public void init() {
    // set the filter indicator
    updateFilterIndicator();

    // set the initial selection
    movieTable.setSelectionModel(selectionModel.getSelectionModel());
    // selecting first movie at startup
    if (MovieModuleManager.getInstance().getMovieList().getMovies() != null
        && !MovieModuleManager.getInstance().getMovieList().getMovies().isEmpty()) {
      ListSelectionModel selectionModel = movieTable.getSelectionModel();
      if (selectionModel.isSelectionEmpty()) {
        int selectionIndex = movieTable.convertRowIndexToModel(0);
        selectionModel.setSelectionInterval(selectionIndex, selectionIndex);
      }
    }

    // add a key listener to jump to the first movie starting with the typed character
    addKeyListener();

    SwingUtilities.invokeLater(() -> movieTable.requestFocus());
  }

  private void addKeyListener() {
    movieTable.addKeyListener(new KeyListener() {
      private long   lastKeypress = 0;
      private String searchTerm   = "";

      @Override
      public void keyTyped(KeyEvent arg0) {
        long now = System.currentTimeMillis();
        if (now - lastKeypress > 500) {
          searchTerm = "";
        }
        lastKeypress = now;

        if (arg0.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
          searchTerm += arg0.getKeyChar();
          searchTerm = searchTerm.toLowerCase();
        }

        if (StringUtils.isNotBlank(searchTerm)) {
          boolean titleColumn = true;

          // look for the first column and check if we should search in the original title
          TableColumn tableColumn = movieTable.getColumnModel().getColumn(0);
          if ("originalTitle".equals(tableColumn.getIdentifier())) {
            titleColumn = false;
          }

          TableModel model = movieTable.getModel();

          for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getValueAt(i, 0) instanceof Movie) {
              Movie movie = (Movie) model.getValueAt(i, 0);

              // search in the title/originaltitle depending on the visible column
              String title;

              if (titleColumn) {
                title = movie.getTitleSortable().toLowerCase(Locale.ROOT);
              }
              else {
                title = movie.getOriginalTitleSortable().toLowerCase(Locale.ROOT);
              }

              if (title.startsWith(searchTerm)) {
                movieTable.getSelectionModel().setSelectionInterval(i, i);
                movieTable.scrollRectToVisible(new Rectangle(movieTable.getCellRect(i, 0, true)));
                break;
              }
            }
          }
        }
      }

      @Override
      public void keyReleased(KeyEvent arg0) {
        // not needed
      }

      @Override
      public void keyPressed(KeyEvent arg0) {
        // not needed
      }
    });

    InputMap inputMap = movieTable.getInputMap(WHEN_FOCUSED);
    ActionMap actionMap = movieTable.getActionMap();

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), "popup");
    actionMap.put("popup", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (popupMenu != null) {
          Rectangle rect = movieTable.getCellRect(movieTable.getSelectedRow(), 0, false);
          popupMenu.show(movieTable, rect.x + rect.width / 2, rect.y + rect.height / 2);
        }
      }
    });
  }

  public MovieSelectionModel getSelectionModel() {
    return selectionModel;
  }

  @Override
  public void setPopupMenu(JPopupMenu popupMenu) {
    this.popupMenu = popupMenu;

    if (popupMenu != null) {
      setComponentPopupMenu(popupMenu);
    }

    movieTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() >= 2 && !e.isConsumed() && e.getButton() == MouseEvent.BUTTON1) {
          Action editAction = new MovieEditAction();
          editAction.actionPerformed(null);
        }
      }
    });
    movieTable.addMouseListener(new TablePopupListener(popupMenu, movieTable));
  }

  protected void initDataBindings() {
    BeanProperty<MovieList, Integer> movieListBeanProperty = BeanProperty.create("movieCount");
    BeanProperty<JLabel, String> jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding<MovieList, Integer, JLabel, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, movieList, movieListBeanProperty,
        lblMovieCountTotal, jLabelBeanProperty);
    autoBinding.bind();
  }

  private static class MovieTableSortingStrategy extends MouseKeyboardSortingStrategy {
    private boolean titleClicked = false;

    @Override
    public void columnClicked(SortingState sortingState, int column, int clicks, boolean shift, boolean control) {
      if (column == 0) {
        // click on title
        titleClicked = true;
      }
      else if (!control) {
        // everything else clicked without CTRL
        titleClicked = false;
      }
      super.columnClicked(sortingState, column, clicks, shift, control);
    }

    @Override
    public void finalHook(SortingState sortingState) {

      // install sorting on title too (in the last place) if it is no yet selected
      if (!titleClicked) {
        SortingState.SortingColumn sortingColumn = sortingState.getColumns().get(0);
        sortingState.getRecentlyClickedColumns().remove(sortingColumn);
        sortingState.appendComparator(0, 0, false);
      }
    }
  }
}
