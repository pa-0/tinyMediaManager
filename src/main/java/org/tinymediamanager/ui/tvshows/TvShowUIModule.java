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
package org.tinymediamanager.ui.tvshows;

import java.awt.CardLayout;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.PostProcess;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowEpisodePostProcessExecutor;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowPostProcessExecutor;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.license.License;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.ui.AbstractTmmUIModule;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.MainTabbedPane;
import org.tinymediamanager.ui.components.MenuScroller;
import org.tinymediamanager.ui.settings.TmmSettingsNode;
import org.tinymediamanager.ui.thirdparty.KodiRPCMenu;
import org.tinymediamanager.ui.tvshows.actions.DebugDumpShowAction;
import org.tinymediamanager.ui.tvshows.actions.DebugDumpShowActionWithChilds;
import org.tinymediamanager.ui.tvshows.actions.TvShowAddDatasourceAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowAspectRatioDetectAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowBulkEditAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowChangeDatasourceAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowChangeSeasonArtworkAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowChangeToAbsoluteAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowCleanUpFilesAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowClearImageCacheAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowDeleteAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowDownloadActorImagesAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowDownloadMissingArtworkAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowDownloadThemeAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowEditAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowExportAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowFetchImdbTop250;
import org.tinymediamanager.ui.tvshows.actions.TvShowFetchRatingsAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowLockAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowMediaInformationAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowMissingEpisodeListAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowOpenFolderAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowReadEpisodeNfoAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowReadNfoAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRebuildImageCacheAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRebuildMediainfoXmlAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRemoveAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRenameAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRenamePreviewAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowResetNewFlagAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRewriteEpisodeNfoAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRewriteNfoAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowScrapeEpisodesAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowScrapeMissingEpisodesAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowScrapeNewItemsAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSelectedScrapeAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSelectedScrapeMetadataAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSingleScrapeAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSubtitleDownloadAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSubtitleSearchAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSyncSelectedCollectionTraktTvAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSyncSelectedRatingTraktTvAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSyncSelectedTraktTvAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSyncSelectedWatchedTraktTvAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSyncTraktTvAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowTitleToEntityMatcher;
import org.tinymediamanager.ui.tvshows.actions.TvShowToggleWatchedFlagAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowTrailerDownloadAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowUnlockAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowUpdateAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowUpdateDatasourcesAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowUpdateSingleDatasourceAction;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowFilterDialog;
import org.tinymediamanager.ui.tvshows.panels.TvShowTreePanel;
import org.tinymediamanager.ui.tvshows.panels.episode.TvShowEpisodeCastPanel;
import org.tinymediamanager.ui.tvshows.panels.episode.TvShowEpisodeInformationPanel;
import org.tinymediamanager.ui.tvshows.panels.episode.TvShowEpisodeMediaInformationPanel;
import org.tinymediamanager.ui.tvshows.panels.season.TvShowSeasonInformationPanel;
import org.tinymediamanager.ui.tvshows.panels.season.TvShowSeasonMediaFilesPanel;
import org.tinymediamanager.ui.tvshows.panels.tvshow.TvShowArtworkPanel;
import org.tinymediamanager.ui.tvshows.panels.tvshow.TvShowCastPanel;
import org.tinymediamanager.ui.tvshows.panels.tvshow.TvShowInformationPanel;
import org.tinymediamanager.ui.tvshows.panels.tvshow.TvShowMediaInformationPanel;
import org.tinymediamanager.ui.tvshows.panels.tvshow.TvShowTrailerPanel;
import org.tinymediamanager.ui.tvshows.settings.TvShowSettingsNode;

import net.miginfocom.swing.MigLayout;

public class TvShowUIModule extends AbstractTmmUIModule {
  private static final String       ID       = "tvShows";

  private static TvShowUIModule     instance = null;

  final TvShowSelectionModel        tvShowSelectionModel;
  final TvShowSeasonSelectionModel  tvShowSeasonSelectionModel;
  final TvShowEpisodeSelectionModel tvShowEpisodeSelectionModel;

  private final TvShowTreePanel     listPanel;
  private final JPanel              detailPanel;
  private final JPanel              dataPanel;
  private final TvShowFilterDialog  tvShowFilterDialog;

  private final TmmSettingsNode     settingsNode;

  private TvShowUIModule() {

    tvShowSelectionModel = new TvShowSelectionModel();
    tvShowSeasonSelectionModel = new TvShowSeasonSelectionModel();
    tvShowEpisodeSelectionModel = new TvShowEpisodeSelectionModel();

    listPanel = new TvShowTreePanel(tvShowSelectionModel);

    detailPanel = new JPanel();
    detailPanel.setLayout(new MigLayout("insets 0", "[grow]", "[grow]"));

    dataPanel = new JPanel();
    dataPanel.setLayout(new CardLayout());
    detailPanel.add(dataPanel, "cell 0 0, grow");

    // panel for TV shows
    JTabbedPane tvShowDetailPanel = new MainTabbedPane() {
      @Override
      public void updateUI() {
        putClientProperty("leftBorder", "half");
        putClientProperty("bottomBorder", Boolean.FALSE);
        putClientProperty("roundEdge", Boolean.FALSE);
        super.updateUI();
      }
    };

    tvShowDetailPanel.add(TmmResourceBundle.getString("metatag.details"), new TvShowInformationPanel(tvShowSelectionModel));
    tvShowDetailPanel.add(TmmResourceBundle.getString("metatag.cast"), new TvShowCastPanel(tvShowSelectionModel));
    tvShowDetailPanel.add(TmmResourceBundle.getString("metatag.mediafiles"), new TvShowMediaInformationPanel(tvShowSelectionModel));
    tvShowDetailPanel.add(TmmResourceBundle.getString("metatag.artwork"), new TvShowArtworkPanel(tvShowSelectionModel));
    tvShowDetailPanel.add(TmmResourceBundle.getString("metatag.trailer"), new TvShowTrailerPanel(tvShowSelectionModel));
    dataPanel.add(tvShowDetailPanel, "tvShow");

    // panel for seasons
    JTabbedPane tvShowSeasonDetailPanel = new MainTabbedPane() {
      @Override
      public void updateUI() {
        putClientProperty("leftBorder", "half");
        putClientProperty("bottomBorder", Boolean.FALSE);
        super.updateUI();
      }
    };
    tvShowSeasonDetailPanel.add(TmmResourceBundle.getString("metatag.details"), new TvShowSeasonInformationPanel(tvShowSeasonSelectionModel));
    tvShowSeasonDetailPanel.add(TmmResourceBundle.getString("metatag.mediafiles"), new TvShowSeasonMediaFilesPanel(tvShowSeasonSelectionModel));
    dataPanel.add(tvShowSeasonDetailPanel, "tvShowSeason");

    // panel for episodes
    JTabbedPane tvShowEpisodeDetailPanel = new MainTabbedPane() {
      @Override
      public void updateUI() {
        putClientProperty("leftBorder", "half");
        putClientProperty("bottomBorder", Boolean.FALSE);
        super.updateUI();
      }
    };

    tvShowEpisodeDetailPanel.add(TmmResourceBundle.getString("metatag.details"), new TvShowEpisodeInformationPanel(tvShowEpisodeSelectionModel));
    tvShowEpisodeDetailPanel.add(TmmResourceBundle.getString("metatag.cast"), new TvShowEpisodeCastPanel(tvShowEpisodeSelectionModel));
    tvShowEpisodeDetailPanel.add(TmmResourceBundle.getString("metatag.mediafiles"),
        new TvShowEpisodeMediaInformationPanel(tvShowEpisodeSelectionModel));
    dataPanel.add(tvShowEpisodeDetailPanel, "tvShowEpisode");

    // glass pane for searching/filtering
    tvShowFilterDialog = new TvShowFilterDialog(listPanel.getTreeTable());

    // create actions and menus
    createActions();
    createMenus();
    createPopupMenu();
    registerAccelerators();

    // build settings node
    settingsNode = new TvShowSettingsNode();

    // further initializations
    init();
  }

  private void init() {
    // re-set filters
    if (TvShowModuleManager.getInstance().getSettings().isStoreUiFilters()) {
      SwingUtilities.invokeLater(() -> {
        TvShowModuleManager.getInstance().getTvShowList().searchDuplicateEpisodes();
        listPanel.getTreeTable().setFilterValues(TvShowModuleManager.getInstance().getSettings().getUiFilters());
      });
    }
  }

  public static TvShowUIModule getInstance() {
    if (instance == null) {
      instance = new TvShowUIModule();
    }
    return instance;
  }

  public void setFilterDialogVisible(boolean selected) {
    tvShowFilterDialog.setVisible(selected);
  }

  @Override
  public String getModuleId() {
    return ID;
  }

  @Override
  public JPanel getTabPanel() {
    return listPanel;
  }

  @Override
  public String getTabTitle() {
    return TmmResourceBundle.getString("tmm.tvshows");
  }

  @Override
  public ImageIcon getMenuIcon() {
    return IconManager.MENU_TV_SHOWS;
  }

  @Override
  public ImageIcon getMenuActiveIcon() {
    return IconManager.MENU_TV_SHOWS_ACTIVE;
  }

  @Override
  public JPanel getDetailPanel() {
    return detailPanel;
  }

  public TvShowSelectionModel getSelectionModel() {
    return tvShowSelectionModel;
  }

  @Override
  public TmmSettingsNode getSettingsNode() {
    return settingsNode;
  }

  private void createActions() {
    searchAction = createAndRegisterAction(TvShowSingleScrapeAction.class);
    editAction = createAndRegisterAction(TvShowEditAction.class);
    updateAction = createAndRegisterAction(TvShowUpdateDatasourcesAction.class);
    renameAction = createAndRegisterAction(TvShowRenameAction.class);
  }

  private void createMenus() {
    updatePopupMenu = new JPopupMenu(TmmResourceBundle.getString("Toolbar.update"));
    updatePopupMenu.setToolTipText(TmmResourceBundle.getString("Toolbar.update"));
    updatePopupMenu.add(createAndRegisterAction(TvShowUpdateDatasourcesAction.class));

    JMenu datasourcesMenu = new JMenu(TmmResourceBundle.getString("metatag.datasource"));
    datasourcesMenu.setIcon(IconManager.MENU);
    MenuScroller.setScrollerFor(datasourcesMenu, 20, 50, 0, 0);
    datasourcesMenu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        datasourcesMenu.removeAll();

        for (String ds : TvShowModuleManager.getInstance().getSettings().getTvShowDataSource()) {
          datasourcesMenu.add(new TvShowUpdateSingleDatasourceAction(ds));
        }
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        // nothing to do
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
        // nothing to do
      }
    });
    updatePopupMenu.add(datasourcesMenu);

    updatePopupMenu.addSeparator();
    updatePopupMenu.add(new TvShowUpdateAction());
    updatePopupMenu.addSeparator();
    updatePopupMenu.add(createAndRegisterAction(TvShowAddDatasourceAction.class));

    // scrape popup menu
    searchPopupMenu = new JPopupMenu(TmmResourceBundle.getString("Toolbar.search"));
    searchPopupMenu.setToolTipText(TmmResourceBundle.getString("Toolbar.search"));
    searchPopupMenu.add(createAndRegisterAction(TvShowSingleScrapeAction.class));
    searchPopupMenu.add(createAndRegisterAction(TvShowSelectedScrapeAction.class));
    searchPopupMenu.add(createAndRegisterAction(TvShowSelectedScrapeMetadataAction.class));
    searchPopupMenu.add(createAndRegisterAction(TvShowScrapeEpisodesAction.class));
    searchPopupMenu.add(createAndRegisterAction(TvShowScrapeNewItemsAction.class));
    searchPopupMenu.add(createAndRegisterAction(TvShowScrapeMissingEpisodesAction.class));
    searchPopupMenu.add(createAndRegisterAction(TvShowMissingEpisodeListAction.class));

    // edit popupmenu
    editPopupMenu = new JPopupMenu(TmmResourceBundle.getString("Toolbar.edit"));
    editPopupMenu.setToolTipText(TmmResourceBundle.getString("Toolbar.edit"));
    editPopupMenu.add(createAndRegisterAction(TvShowEditAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowBulkEditAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowLockAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowUnlockAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowFetchRatingsAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowFetchImdbTop250.class));
    editPopupMenu.add(createAndRegisterAction(TvShowChangeDatasourceAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowChangeSeasonArtworkAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowToggleWatchedFlagAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowRewriteNfoAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowReadNfoAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowRewriteEpisodeNfoAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowReadEpisodeNfoAction.class));

    editPopupMenu.addSeparator();
    editPopupMenu.add(createAndRegisterAction(TvShowMediaInformationAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowRebuildMediainfoXmlAction.class));
    // editPopupMenu.add(createAndRegisterAction(TvShowDeleteMediainfoXmlAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowAspectRatioDetectAction.class));

    editPopupMenu.addSeparator();
    editPopupMenu.add(createAndRegisterAction(TvShowRebuildImageCacheAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowResetNewFlagAction.class));

    editPopupMenu.addSeparator();
    JMenu traktMenu = new JMenu("Trakt.tv");
    traktMenu.setIcon(IconManager.MENU);
    traktMenu.add(createAndRegisterAction(TvShowSyncTraktTvAction.class));
    traktMenu.addSeparator();
    traktMenu.add(createAndRegisterAction(TvShowSyncSelectedTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(TvShowSyncSelectedCollectionTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(TvShowSyncSelectedWatchedTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(TvShowSyncSelectedRatingTraktTvAction.class));
    editPopupMenu.add(traktMenu);

    editPopupMenu.addSeparator();
    editPopupMenu.add(createAndRegisterAction(TvShowExportAction.class));

    // rename popup menu
    renamePopupMenu = new JPopupMenu(TmmResourceBundle.getString("Toolbar.rename"));
    renamePopupMenu.setToolTipText(TmmResourceBundle.getString("Toolbar.rename"));
    renamePopupMenu.add(createAndRegisterAction(TvShowRenameAction.class));
    renamePopupMenu.add(createAndRegisterAction(TvShowRenamePreviewAction.class));
    renamePopupMenu.addSeparator();
    renamePopupMenu.add(createAndRegisterAction(TvShowCleanUpFilesAction.class));
    renamePopupMenu.add(createAndRegisterAction(TvShowClearImageCacheAction.class));
  }

  private void createPopupMenu() {
    // popup menu
    popupMenu = new JPopupMenu();
    popupMenu.add(createAndRegisterAction(TvShowSingleScrapeAction.class));
    popupMenu.add(createAndRegisterAction(TvShowSelectedScrapeAction.class));
    popupMenu.add(createAndRegisterAction(TvShowSelectedScrapeMetadataAction.class));
    popupMenu.add(createAndRegisterAction(TvShowScrapeEpisodesAction.class));
    popupMenu.add(createAndRegisterAction(TvShowScrapeNewItemsAction.class));
    popupMenu.add(createAndRegisterAction(TvShowScrapeMissingEpisodesAction.class));
    popupMenu.add(createAndRegisterAction(TvShowMissingEpisodeListAction.class));

    popupMenu.addSeparator();
    popupMenu.add(createAndRegisterAction(TvShowUpdateAction.class));
    popupMenu.add(createAndRegisterAction(TvShowReadNfoAction.class));
    popupMenu.add(createAndRegisterAction(TvShowReadEpisodeNfoAction.class));

    JMenu updateDatasourcesMenu = new JMenu(TmmResourceBundle.getString("Toolbar.update"));
    updateDatasourcesMenu.setIcon(IconManager.MENU);
    updateDatasourcesMenu.add(createAndRegisterAction(TvShowUpdateDatasourcesAction.class));

    JMenu datasourcesMenu = new JMenu(TmmResourceBundle.getString("metatag.datasource"));
    datasourcesMenu.setIcon(IconManager.MENU);
    MenuScroller.setScrollerFor(datasourcesMenu, 20, 50, 2, 5);
    datasourcesMenu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        datasourcesMenu.removeAll();

        for (String ds : TvShowModuleManager.getInstance().getSettings().getTvShowDataSource()) {
          datasourcesMenu.add(new TvShowUpdateSingleDatasourceAction(ds));
        }
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        // nothing to do
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
        // nothing to do
      }
    });
    updateDatasourcesMenu.add(datasourcesMenu);

    updateDatasourcesMenu.addSeparator();
    updateDatasourcesMenu.add(createAndRegisterAction(TvShowAddDatasourceAction.class));
    popupMenu.add(updateDatasourcesMenu);

    popupMenu.addSeparator();
    popupMenu.add(createAndRegisterAction(TvShowEditAction.class));

    JMenu enhancededitPopupMenu = new JMenu(TmmResourceBundle.getString("edit.enhanced"));
    enhancededitPopupMenu.setIcon(IconManager.MENU);
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowBulkEditAction.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowLockAction.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowUnlockAction.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowToggleWatchedFlagAction.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowFetchRatingsAction.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowFetchImdbTop250.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowChangeDatasourceAction.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowChangeSeasonArtworkAction.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowRewriteNfoAction.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowRewriteEpisodeNfoAction.class));
    enhancededitPopupMenu.addSeparator();
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowMediaInformationAction.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowRebuildMediainfoXmlAction.class));
    // enhancededitPopupMenu.add(createAndRegisterAction(TvShowDeleteMediainfoXmlAction.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowAspectRatioDetectAction.class));
    enhancededitPopupMenu.addSeparator();
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowRebuildImageCacheAction.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowResetNewFlagAction.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowTitleToEntityMatcher.class));
    enhancededitPopupMenu.add(createAndRegisterAction(TvShowChangeToAbsoluteAction.class));
    popupMenu.add(enhancededitPopupMenu);

    JMenu downloadMenu = new JMenu(TmmResourceBundle.getString("tmm.download"));
    downloadMenu.setIcon(IconManager.MENU);
    downloadMenu.add(createAndRegisterAction(TvShowDownloadMissingArtworkAction.class));
    downloadMenu.add(createAndRegisterAction(TvShowDownloadActorImagesAction.class));
    downloadMenu.add(createAndRegisterAction(TvShowTrailerDownloadAction.class));
    downloadMenu.add(createAndRegisterAction(TvShowSubtitleSearchAction.class));
    downloadMenu.add(createAndRegisterAction(TvShowSubtitleDownloadAction.class));
    downloadMenu.add(createAndRegisterAction(TvShowDownloadThemeAction.class));
    popupMenu.add(downloadMenu);

    JMenu renamePopupMenu = new JMenu(TmmResourceBundle.getString("Toolbar.rename"));
    renamePopupMenu.setIcon(IconManager.MENU);
    renamePopupMenu.add(createAndRegisterAction(TvShowRenameAction.class));
    renamePopupMenu.add(createAndRegisterAction(TvShowRenamePreviewAction.class));
    renamePopupMenu.addSeparator();
    renamePopupMenu.add(createAndRegisterAction(TvShowCleanUpFilesAction.class));
    renamePopupMenu.add(createAndRegisterAction(TvShowClearImageCacheAction.class));
    popupMenu.add(renamePopupMenu);

    popupMenu.addSeparator();
    popupMenu.add(createAndRegisterAction(TvShowExportAction.class));
    popupMenu.add(createAndRegisterAction(TvShowOpenFolderAction.class));

    popupMenu.addSeparator();
    JMenu traktMenu = new JMenu("Trakt.tv");
    traktMenu.setIcon(IconManager.MENU);
    traktMenu.add(createAndRegisterAction(TvShowSyncSelectedTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(TvShowSyncSelectedCollectionTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(TvShowSyncSelectedWatchedTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(TvShowSyncSelectedRatingTraktTvAction.class));
    popupMenu.add(traktMenu);
    JMenu kodiRPCMenu = KodiRPCMenu.createMenuKodiMenuRightClickTvShows();
    popupMenu.add(kodiRPCMenu);

    JMenu postProcessingMenu = new JMenu(TmmResourceBundle.getString("Settings.postprocessing"));
    postProcessingMenu.setIcon(IconManager.MENU);
    popupMenu.add(postProcessingMenu);

    popupMenu.addSeparator();
    popupMenu.add(createAndRegisterAction(TvShowRemoveAction.class));
    popupMenu.add(createAndRegisterAction(TvShowDeleteAction.class));

    if (Globals.isDebug()) {
      final JMenu debugMenu = new JMenu("Debug");
      debugMenu.add(new DebugDumpShowAction());
      debugMenu.add(new DebugDumpShowActionWithChilds());
      popupMenu.addSeparator();
      popupMenu.add(debugMenu);
    }

    // activate/deactivate menu items based on some status
    popupMenu.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        if (StringUtils.isNotBlank(Settings.getInstance().getKodiHost())) {
          kodiRPCMenu.setText(KodiRPC.getInstance().getVersion());
          kodiRPCMenu.setEnabled(true);
        }
        else {
          kodiRPCMenu.setText("Kodi");
          kodiRPCMenu.setEnabled(false);
        }

        if (License.getInstance().isValidLicense() && StringUtils.isNotBlank(Settings.getInstance().getTraktAccessToken())) {
          traktMenu.setEnabled(true);
        }
        else {
          traktMenu.setEnabled(false);
        }

        // Post processing
        postProcessingMenu.removeAll();

        if (TvShowModuleManager.getInstance().getSettings().getPostProcessTvShow().isEmpty()
            && TvShowModuleManager.getInstance().getSettings().getPostProcessEpisode().isEmpty()) {
          postProcessingMenu.setEnabled(false);
        }
        else {
          for (PostProcess process : new ArrayList<>(TvShowModuleManager.getInstance().getSettings().getPostProcessTvShow())) {
            JMenuItem menuItem = new JMenuItem(TmmResourceBundle.getString("metatag.tvshow") + " - " + process.getName(), IconManager.APPLY_INV);
            menuItem.addActionListener(pp -> new TvShowPostProcessExecutor(process).execute());
            postProcessingMenu.add(menuItem);
          }

          if (postProcessingMenu.getItemCount() != 0) {
            postProcessingMenu.addSeparator();
          }

          for (PostProcess process : new ArrayList<>(TvShowModuleManager.getInstance().getSettings().getPostProcessEpisode())) {
            JMenuItem menuItem = new JMenuItem(TmmResourceBundle.getString("metatag.episode") + " - " + process.getName(), IconManager.APPLY_INV);
            menuItem.addActionListener(pp -> new TvShowEpisodePostProcessExecutor(process).execute());
            postProcessingMenu.add(menuItem);
          }

          postProcessingMenu.setEnabled(true);
        }
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

    listPanel.setPopupMenu(popupMenu);

  }

  /**
   * set the selected TV shows. This causes the right sided panel to switch to the TV show information panel
   * 
   * @param tvShow
   *          the selected TV show
   */
  public void setSelectedTvShow(TvShow tvShow) {
    tvShowSelectionModel.setSelectedTvShow(tvShow);
    CardLayout cl = (CardLayout) (dataPanel.getLayout());
    cl.show(dataPanel, "tvShow");
  }

  /**
   * set the selected TV show season. This causes the right sided panel to switch to the season information panel
   * 
   * @param tvShowSeason
   *          the selected season
   */
  public void setSelectedTvShowSeason(TvShowSeason tvShowSeason) {
    tvShowSeasonSelectionModel.setSelectedTvShowSeason(tvShowSeason);
    CardLayout cl = (CardLayout) (dataPanel.getLayout());
    cl.show(dataPanel, "tvShowSeason");
  }

  /**
   * set the selected TV show episode. This cases the right sided panel to switch to the episode information panel
   * 
   * @param tvShowEpisode
   *          the selected episode
   */
  public void setSelectedTvShowEpisode(TvShowEpisode tvShowEpisode) {
    tvShowEpisodeSelectionModel.setSelectedTvShowEpisode(tvShowEpisode);
    CardLayout cl = (CardLayout) (dataPanel.getLayout());
    cl.show(dataPanel, "tvShowEpisode");
  }
}
