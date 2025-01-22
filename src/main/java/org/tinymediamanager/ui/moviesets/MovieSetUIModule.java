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
package org.tinymediamanager.ui.moviesets;

import java.awt.CardLayout;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.tinymediamanager.Globals;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.AbstractTmmUIModule;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.MainTabbedPane;
import org.tinymediamanager.ui.components.TmmMenuLabel;
import org.tinymediamanager.ui.movies.MovieSelectionModel;
import org.tinymediamanager.ui.movies.panels.MovieArtworkPanel;
import org.tinymediamanager.ui.movies.panels.MovieCastPanel;
import org.tinymediamanager.ui.movies.panels.MovieInformationPanel;
import org.tinymediamanager.ui.movies.panels.MovieMediaInformationPanel;
import org.tinymediamanager.ui.movies.panels.MovieTrailerPanel;
import org.tinymediamanager.ui.moviesets.actions.DebugDumpMovieSetAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetAddAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetBatchEditMovieAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetCleanupArtworkAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetEditAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetEditMovieAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetExportAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetExportMovieAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetMissingArtworkAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetReadMovieNfoAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetRemoveAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetRemoveMovieFromSetAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetRenameAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetRewriteNfoAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetScrapeMissingMoviesAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetSearchAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetSyncSelectedCollectionTraktTvAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetSyncSelectedRatingTraktTvAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetSyncSelectedTraktTvAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetSyncSelectedWatchedTraktTvAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetToggleWatchedFlagAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetUpdateMovieAction;
import org.tinymediamanager.ui.moviesets.dialogs.MovieSetFilterDialog;
import org.tinymediamanager.ui.moviesets.panels.MovieSetArtworkPanel;
import org.tinymediamanager.ui.moviesets.panels.MovieSetInformationPanel;
import org.tinymediamanager.ui.moviesets.panels.MovieSetMediaInformationPanel;
import org.tinymediamanager.ui.moviesets.panels.MovieSetMissingMovieInformationPanel;
import org.tinymediamanager.ui.moviesets.panels.MovieSetTreePanel;
import org.tinymediamanager.ui.moviesets.settings.MovieSetSettingsNode;
import org.tinymediamanager.ui.settings.TmmSettingsNode;
import org.tinymediamanager.ui.thirdparty.KodiRPCMenu;

/**
 * the class {@link MovieSetUIModule} is the core class to manage the movie sets in the UI
 * 
 * @author Manuel Laggner
 */
public class MovieSetUIModule extends AbstractTmmUIModule {
  private static final String          ID       = "movieSets";

  private static MovieSetUIModule      instance = null;

  private final MovieSetSelectionModel selectionModel;
  private final MovieSelectionModel    movieSelectionModel;

  private final MovieSetTreePanel      treePanel;
  private final JPanel                 detailPanel;
  private final JPanel                 dataPanel;
  private final MovieSetFilterDialog   movieSetFilterDialog;
  private final TmmSettingsNode        settingsNode;

  private MovieSetUIModule() {
    selectionModel = new MovieSetSelectionModel();
    movieSelectionModel = new MovieSelectionModel();

    treePanel = new MovieSetTreePanel(selectionModel);

    detailPanel = new JPanel();
    detailPanel.setLayout(new CardLayout());

    dataPanel = new JPanel();
    dataPanel.setLayout(new CardLayout());
    detailPanel.add(dataPanel, "cell 0 0, grow");

    // panel for movie sets
    JTabbedPane movieSetDetailPanel = new MainTabbedPane() {
      @Override
      public void updateUI() {
        putClientProperty("leftBorder", "half");
        putClientProperty("bottomBorder", Boolean.FALSE);
        putClientProperty("roundEdge", Boolean.FALSE);
        super.updateUI();
      }
    };

    movieSetDetailPanel.addTab(TmmResourceBundle.getString("metatag.details"), new MovieSetInformationPanel(selectionModel));
    movieSetDetailPanel.addTab(TmmResourceBundle.getString("metatag.mediafiles"), new MovieSetMediaInformationPanel(selectionModel));
    movieSetDetailPanel.addTab(TmmResourceBundle.getString("metatag.artwork"), new MovieSetArtworkPanel(selectionModel));
    dataPanel.add(movieSetDetailPanel, "movieSet");

    // panel for movies
    JTabbedPane movieDetailPanel = new MainTabbedPane() {
      @Override
      public void updateUI() {
        putClientProperty("leftBorder", "half");
        putClientProperty("bottomBorder", Boolean.FALSE);
        super.updateUI();
      }
    };
    movieDetailPanel.addTab(TmmResourceBundle.getString("metatag.details"), new MovieInformationPanel(movieSelectionModel));
    movieDetailPanel.addTab(TmmResourceBundle.getString("metatag.cast"), new MovieCastPanel(movieSelectionModel) {
      @Override
      public String getName() {
        return "movieset.moviecast";
      }
    });
    movieDetailPanel.addTab(TmmResourceBundle.getString("metatag.mediafiles"), new MovieMediaInformationPanel(movieSelectionModel) {
      @Override
      public String getName() {
        return "movieset.moviemediainformation";
      }
    });
    movieDetailPanel.addTab(TmmResourceBundle.getString("metatag.artwork"), new MovieArtworkPanel(movieSelectionModel));
    movieDetailPanel.addTab(TmmResourceBundle.getString("metatag.trailer"), new MovieTrailerPanel(movieSelectionModel) {
      @Override
      public String getName() {
        return "movieset.movietrailer";
      }
    });
    dataPanel.add(movieDetailPanel, "movie");

    movieSetFilterDialog = new MovieSetFilterDialog(treePanel.getTreeTable());

    // panel for missing movies
    JTabbedPane missingMovieDetailPanel = new MainTabbedPane() {
      @Override
      public void updateUI() {
        putClientProperty("leftBorder", "half");
        putClientProperty("bottomBorder", Boolean.FALSE);
        super.updateUI();
      }
    };
    missingMovieDetailPanel.addTab(TmmResourceBundle.getString("metatag.details"), new MovieSetMissingMovieInformationPanel(movieSelectionModel));
    dataPanel.add(missingMovieDetailPanel, "missingMovie");

    // create actions and menus
    createActions();
    createPopupMenu();
    registerAccelerators();

    // settings node
    settingsNode = new MovieSetSettingsNode();

    // further initializations
    init();
  }

  public static MovieSetUIModule getInstance() {
    if (instance == null) {
      instance = new MovieSetUIModule();
    }
    return instance;
  }

  private void init() {
    // re-set filters
    if (MovieModuleManager.getInstance().getSettings().isStoreMovieSetUiFilters()) {
      SwingUtilities
          .invokeLater(() -> treePanel.getTreeTable().setFilterValues(MovieModuleManager.getInstance().getSettings().getMovieSetUiFilters()));
    }
  }

  public void setFilterDialogVisible(boolean visible) {
    movieSetFilterDialog.setVisible(visible);
  }

  private void createActions() {
    updateAction = createAndRegisterAction(MovieSetAddAction.class);
    searchAction = createAndRegisterAction(MovieSetSearchAction.class);
    editAction = createAndRegisterAction(MovieSetEditAction.class);
    renameAction = createAndRegisterAction(MovieSetRenameAction.class);
  }

  private void createPopupMenu() {
    // popup menu
    JPopupMenu popupMenu = new JPopupMenu();

    // movieset actions
    popupMenu.add(new TmmMenuLabel(TmmResourceBundle.getString("metatag.movieset")));
    popupMenu.add(createAndRegisterAction(MovieSetSearchAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetScrapeMissingMoviesAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetAddAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetEditAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetRewriteNfoAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetCleanupArtworkAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetMissingArtworkAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetExportAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetRemoveAction.class));

    // movie actions
    popupMenu.addSeparator();
    popupMenu.add(new TmmMenuLabel(TmmResourceBundle.getString("metatag.movie")));
    popupMenu.add(createAndRegisterAction(MovieSetUpdateMovieAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetEditMovieAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetBatchEditMovieAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetToggleWatchedFlagAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetReadMovieNfoAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetRenameAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetExportMovieAction.class));
    popupMenu.add(createAndRegisterAction(MovieSetRemoveMovieFromSetAction.class));

    popupMenu.addSeparator();
    JMenu traktMenu = new JMenu("Trakt.tv");
    traktMenu.setIcon(IconManager.MENU);
    traktMenu.add(createAndRegisterAction(MovieSetSyncSelectedTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(MovieSetSyncSelectedCollectionTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(MovieSetSyncSelectedWatchedTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(MovieSetSyncSelectedRatingTraktTvAction.class));
    popupMenu.add(traktMenu);

    JMenu kodiRPCMenu = KodiRPCMenu.createMenuKodiMenuRightClickMovieSets();
    popupMenu.add(kodiRPCMenu);

    if (Globals.isDebug()) {
      final JMenu debugMenu = new JMenu("Debug");
      debugMenu.add(new DebugDumpMovieSetAction());
      popupMenu.addSeparator();
      popupMenu.add(debugMenu);
    }

    treePanel.setPopupMenu(popupMenu);

    // dummy popupmenu to infer the text
    updatePopupMenu = new JPopupMenu(TmmResourceBundle.getString("movieset.add"));
    updatePopupMenu.setToolTipText(TmmResourceBundle.getString("movieset.add"));
    updatePopupMenu.add(createAndRegisterAction(MovieSetAddAction.class));
  }

  @Override
  public String getModuleId() {
    return ID;
  }

  @Override
  public JPanel getTabPanel() {
    return treePanel;
  }

  @Override
  public String getTabTitle() {
    return TmmResourceBundle.getString("tmm.moviesets");
  }

  @Override
  public ImageIcon getMenuIcon() {
    return IconManager.MENU_MOVIESETS;
  }

  @Override
  public ImageIcon getMenuActiveIcon() {
    return IconManager.MENU_MOVIESETS_ACTIVE;
  }

  @Override
  public JPanel getDetailPanel() {
    return detailPanel;
  }

  @Override
  public Icon getSearchButtonIcon() {
    return IconManager.TOOLBAR_ADD_MOVIE_SET;
  }

  @Override
  public Icon getSearchButtonHoverIcon() {
    return IconManager.TOOLBAR_ADD_MOVIE_SET_HOVER;
  }

  public MovieSetSelectionModel getSelectionModel() {
    return selectionModel;
  }

  @Override
  public TmmSettingsNode getSettingsNode() {
    return settingsNode;
  }

  public void setSelectedMovieSet(MovieSet movieSet) {
    selectionModel.setSelectedMovieSet(movieSet);
    CardLayout cl = (CardLayout) (dataPanel.getLayout());
    cl.show(dataPanel, "movieSet");
  }

  public void setSelectedMovie(Movie movie) {
    movieSelectionModel.setSelectedMovie(movie);
    CardLayout cl = (CardLayout) (dataPanel.getLayout());
    if (movie instanceof MovieSet.MovieSetMovie) {
      cl.show(dataPanel, "missingMovie");
    }
    else {
      cl.show(dataPanel, "movie");
    }
  }
}
