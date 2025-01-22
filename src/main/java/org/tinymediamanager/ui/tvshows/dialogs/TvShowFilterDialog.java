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

package org.tinymediamanager.ui.tvshows.dialogs;

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
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmTabbedPane;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;
import org.tinymediamanager.ui.components.treetable.TmmTreeTable;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.panels.FilterSavePanel;
import org.tinymediamanager.ui.panels.ModalPopupPanel;
import org.tinymediamanager.ui.tvshows.filters.ITvShowUIFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowAllInOneFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowAspectRatioFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowAudioChannelFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowAudioCodecFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowAudioLanguageFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowAudioStreamCountFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowAudioTitleFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowBannerSizeFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowCastFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowCertificationFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowClearArtSizeFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowClearLogoSizeFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowCountryFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowDatasourceFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowDateAddedFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowDecadeFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowDiscArtSizeFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowDuplicateEpisodesFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowEmptyFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowEpisodeCountFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowEpisodeCrc32Filter;
import org.tinymediamanager.ui.tvshows.filters.TvShowFanartSizeFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowFilenameFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowFrameRateFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowGenreFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowHDRFormatFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowKeyArtSizeFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowLockedFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowMediaFilesFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowMediaSourceFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowMissingArtworkFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowMissingEpisodesFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowMissingMetadataFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowMissingSubtitlesFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowNewEpisodesFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowNoteFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowPathFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowPosterSizeFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowRuntimeFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowSeasonCountFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowStatusFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowStudioFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowSubtitleCountFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowSubtitleFormatFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowSubtitleLanguageFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowTagFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowThumbSizeFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowUncategorizedEpisodesFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowVideoBitdepthFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowVideoBitrateFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowVideoCodecFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowVideoContainerFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowVideoFilenameFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowVideoFilesizeFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowVideoFormatFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowWatchedFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowYearFilter;

import net.miginfocom.swing.MigLayout;

public class TvShowFilterDialog extends TmmDialog {
  private static final String                        PANEL_COL_CONSTRAINTS = "[][][][200lp:250lp,grow]";

  private final TmmTreeTable                         treeTable;

  // map for storing which filter is in which panel
  private final Map<JPanel, Set<ITvShowUIFilter<?>>> filterMap;
  private final Set<ITvShowUIFilter<?>>              filters;

  private final JTabbedPane                          tabbedPane;
  private JComboBox<String>                          cbPreset;

  public TvShowFilterDialog(TmmTreeTable treeTable) {
    super(TmmResourceBundle.getString("movieextendedsearch.options") + " - " + TmmResourceBundle.getString("tmm.tvshows"), "tvShowFilter");
    setModalityType(ModalityType.MODELESS);

    this.treeTable = treeTable;
    this.filterMap = new HashMap<>();
    this.filters = new HashSet<>();
    this.treeTable.addPropertyChangeListener("filterChanged", evt -> filterChanged());

    ActionListener actionListener = e -> {
      String filterName = (String) cbPreset.getSelectedItem();
      if (StringUtils.isNotBlank(filterName)) {
        treeTable.setFilterValues(TvShowModuleManager.getInstance().getSettings().getUiFilterPresets().get(filterName));
        treeTable.storeFilters();
      }
      else {
        treeTable.clearFilter();
      }
    };

    {
      tabbedPane = new TmmTabbedPane();
      getContentPane().add(tabbedPane, BorderLayout.CENTER);

      {
        // panel Main
        JPanel panelMain = new JPanel(new MigLayout("", PANEL_COL_CONSTRAINTS, "[]"));
        tabbedPane.addTab(TmmResourceBundle.getString("metatag.details"), createTabPanel(panelMain));

        addFilter(new TvShowNewEpisodesFilter(), panelMain);
        addFilter(new TvShowDatasourceFilter(), panelMain);
        addFilter(new TvShowWatchedFilter(), panelMain);
        addFilter(new TvShowLockedFilter(), panelMain);
        addFilter(new TvShowDateAddedFilter(), panelMain);
        addFilter(new TvShowDuplicateEpisodesFilter(), panelMain);
        addFilter(new TvShowStatusFilter(), panelMain);
        addFilter(new TvShowAllInOneFilter(), panelMain);
        addFilter(new TvShowEmptyFilter(), panelMain);
      }

      {
        // panel Metadata
        JPanel panelMetadata = new JPanel(new MigLayout("", PANEL_COL_CONSTRAINTS, "[]"));
        tabbedPane.addTab(TmmResourceBundle.getString("tmm.metadata"), createTabPanel(panelMetadata));

        addFilter(new TvShowYearFilter(), panelMetadata);
        addFilter(new TvShowDecadeFilter(), panelMetadata);
        addFilter(new TvShowGenreFilter(), panelMetadata);
        addFilter(new TvShowCertificationFilter(), panelMetadata);
        addFilter(new TvShowCastFilter(), panelMetadata);
        addFilter(new TvShowCountryFilter(), panelMetadata);
        addFilter(new TvShowStudioFilter(), panelMetadata);
        addFilter(new TvShowTagFilter(), panelMetadata);
        addFilter(new TvShowNoteFilter(), panelMetadata);
        addFilter(new TvShowEpisodeCountFilter(), panelMetadata);
        addFilter(new TvShowSeasonCountFilter(), panelMetadata);
      }

      {
        // panel video
        JPanel panelVideo = new JPanel(new MigLayout("", PANEL_COL_CONSTRAINTS, "[]"));
        tabbedPane.addTab(TmmResourceBundle.getString("metatag.video"), createTabPanel(panelVideo));

        addFilter(new TvShowVideoFormatFilter(), panelVideo);
        addFilter(new TvShowVideoCodecFilter(), panelVideo);
        addFilter(new TvShowVideoBitrateFilter(), panelVideo);
        addFilter(new TvShowVideoBitdepthFilter(), panelVideo);
        addFilter(new TvShowVideoContainerFilter(), panelVideo);
        addFilter(new TvShowAspectRatioFilter(), panelVideo);
        addFilter(new TvShowFrameRateFilter(), panelVideo);
        addFilter(new TvShowHDRFormatFilter(), panelVideo);
        addFilter(new TvShowEpisodeCrc32Filter(), panelVideo);
      }

      {
        // panel audio
        JPanel panelAudio = new JPanel(new MigLayout("", PANEL_COL_CONSTRAINTS, "[]"));
        tabbedPane.addTab(TmmResourceBundle.getString("metatag.audio"), createTabPanel(panelAudio));

        addFilter(new TvShowAudioCodecFilter(), panelAudio);
        addFilter(new TvShowAudioChannelFilter(), panelAudio);
        addFilter(new TvShowAudioStreamCountFilter(), panelAudio);
        addFilter(new TvShowAudioLanguageFilter(), panelAudio);
        addFilter(new TvShowAudioTitleFilter(), panelAudio);
      }

      {
        // panel artwork
        JPanel panelArtwork = new JPanel(new MigLayout("", PANEL_COL_CONSTRAINTS, "[]"));
        tabbedPane.addTab(TmmResourceBundle.getString("filter.artwork"), createTabPanel(panelArtwork));

        addFilter(new TvShowPosterSizeFilter(), panelArtwork);
        addFilter(new TvShowFanartSizeFilter(), panelArtwork);
        addFilter(new TvShowBannerSizeFilter(), panelArtwork);
        addFilter(new TvShowThumbSizeFilter(), panelArtwork);
        addFilter(new TvShowClearLogoSizeFilter(), panelArtwork);
        addFilter(new TvShowClearArtSizeFilter(), panelArtwork);
        addFilter(new TvShowDiscArtSizeFilter(), panelArtwork);
        addFilter(new TvShowKeyArtSizeFilter(), panelArtwork);
        addFilter(new TvShowMissingArtworkFilter(), panelArtwork);
      }

      {
        // panel other
        JPanel panelOthers = new JPanel(new MigLayout("", PANEL_COL_CONSTRAINTS, "[]"));
        tabbedPane.addTab(TmmResourceBundle.getString("filter.others"), createTabPanel(panelOthers));

        addFilter(new TvShowMediaSourceFilter(), panelOthers);
        addFilter(new TvShowMediaFilesFilter(), panelOthers);
        addFilter(new TvShowFilenameFilter(), panelOthers);
        addFilter(new TvShowVideoFilenameFilter(), panelOthers);
        addFilter(new TvShowVideoFilesizeFilter(), panelOthers);
        addFilter(new TvShowRuntimeFilter(), panelOthers);
        addFilter(new TvShowPathFilter(), panelOthers);
        addFilter(new TvShowSubtitleCountFilter(), panelOthers);
        addFilter(new TvShowSubtitleLanguageFilter(), panelOthers);
        addFilter(new TvShowSubtitleFormatFilter(), panelOthers);
        addFilter(new TvShowUncategorizedEpisodesFilter(), panelOthers);
        addFilter(new TvShowMissingEpisodesFilter(), panelOthers);
        addFilter(new TvShowMissingMetadataFilter(), panelOthers);
        addFilter(new TvShowMissingSubtitlesFilter(), panelOthers);
      }

      {
        // filter preset panel
        JPanel panelFilterPreset = new JPanel();
        panelFilterPreset.setLayout(new MigLayout("insets n 0 n 0", "[5lp!][10lp][150lp,grow][5lp!]", "[]"));

        JSeparator separator = new JSeparator();
        panelFilterPreset.add(separator, "cell 0 1 4 1,growx,aligny top");

        JLabel lblEnableAllT = new TmmLabel(TmmResourceBundle.getString("filter.enableall"));
        panelFilterPreset.add(lblEnableAllT, "cell 1 2, alignx trailing");

        JCheckBox chkbxEnableAll = new JCheckBox();
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
                TvShowModuleManager.getInstance().getSettings().getUiFilterPresets());

            ModalPopupPanel popupPanel = createModalPopupPanel();
            popupPanel.setTitle(TmmResourceBundle.getString("filter.savepreset"));

            FilterSavePanel filterSavePanel = new FilterSavePanel(activeUiFilters, uiFilters);

            popupPanel.setOnCloseHandler(() -> {
              String savedPreset = filterSavePanel.getSavedPreset();
              if (StringUtils.isNotBlank(savedPreset)) {
                cbPreset.removeActionListener(actionListener);
                TvShowModuleManager.getInstance().getSettings().setUiFilterPresets(uiFilters);
                TvShowModuleManager.getInstance().getSettings().saveSettings();
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
          if (!TmmProperties.getInstance().getPropertyAsBoolean("tvshow.hidefilterhint")) {
            JCheckBox checkBox = new JCheckBox(TmmResourceBundle.getString("tmm.donotshowagain"));
            TmmFontHelper.changeFont(checkBox, L1);
            checkBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

            Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
            Object[] params = { TmmResourceBundle.getString("filter.remove"), checkBox };
            int answer = JOptionPane.showOptionDialog(MainWindow.getInstance(), params, TmmResourceBundle.getString("filter.remove"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);

            // the user don't want to show this dialog again
            if (checkBox.isSelected()) {
              TmmProperties.getInstance().putProperty("tvshow.hidefilterhint", String.valueOf(checkBox.isSelected()));
            }

            if (answer != JOptionPane.YES_OPTION) {
              return;
            }
          }

          Map<String, List<AbstractSettings.UIFilters>> tvShowUiFilters = new HashMap<>(
              TvShowModuleManager.getInstance().getSettings().getUiFilterPresets());
          if (tvShowUiFilters.remove(filterName) != null) {
            cbPreset.removeActionListener(actionListener);
            TvShowModuleManager.getInstance().getSettings().setUiFilterPresets(tvShowUiFilters);
            TvShowModuleManager.getInstance().getSettings().saveSettings();
            loadPresets();
            cbPreset.addActionListener(actionListener);
          }
        });
        panelFilterPreset.add(btnDeletePreset, "cell 2 3");

        getContentPane().add(panelFilterPreset, BorderLayout.SOUTH);
      }
    }

    {
      // init
      loadPresets();

      cbPreset.addActionListener(actionListener);
    }
  }

  /**
   * create a new {@link JPanel} which holds the label/clear filter button and the scrollpane with the content
   *
   * @param content
   *          the content {@link JPanel} to add
   * @return the embedding {@link JPanel} which can be added to the {@link TmmTabbedPane}
   */
  private JPanel createTabPanel(JPanel content) {
    JPanel panel = new JPanel(new MigLayout("insets 0", "[grow][]", "[][][]"));
    panel.add(new TmmLabel(TmmResourceBundle.getString("movieextendedsearch.filterby")), "cell 0 0");

    panel.add(new JLabel(TmmResourceBundle.getString("filter.reset")), "cell 1 0, right");
    panel.add(new FlatButton(IconManager.DELETE, e -> SwingUtilities.invokeLater(treeTable::clearFilter)), "cell 1 0, right");

    JScrollPane scrollPane = new NoBorderScrollPane(content);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.getVerticalScrollBar().setUnitIncrement(4);

    panel.add(scrollPane, "cell 0 2 2 1, grow, wrap");

    return panel;
  }

  private Set<AbstractSettings.UIFilters> getActiveUiFilters() {
    return new HashSet<>(ITvShowUIFilter.morphToUiFilters(filters));
  }

  private void loadPresets() {
    String preset = (String) cbPreset.getSelectedItem();

    cbPreset.removeAllItems();
    cbPreset.addItem("");
    TvShowModuleManager.getInstance().getSettings().getUiFilterPresets().keySet().stream().sorted().forEach(key -> cbPreset.addItem(key));

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
  private void addFilter(ITvShowUIFilter<TmmTreeNode> filter, JPanel panel) {
    panel.add(filter.getCheckBox(), "");
    panel.add(filter.getLabel(), "");

    if (filter.getFilterOptionComboBox() != null) {
      panel.add(filter.getFilterOptionComboBox(), "");
    }
    else {
      panel.add(Box.createGlue(), "");
    }

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
    for (Map.Entry<JPanel, Set<ITvShowUIFilter<?>>> entry : filterMap.entrySet()) {
      boolean active = false;
      if (treeTable.isFiltersActive()) {
        for (ITvShowUIFilter<?> filter : entry.getValue()) {
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
  protected void initBottomPanel() {
    // no bottom line needed
  }
}
