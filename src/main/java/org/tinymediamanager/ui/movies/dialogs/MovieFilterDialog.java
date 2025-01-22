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

package org.tinymediamanager.ui.movies.dialogs;

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
import org.tinymediamanager.core.AbstractSettings.UIFilters;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmTabbedPane;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.movies.MovieSelectionModel;
import org.tinymediamanager.ui.movies.filters.IMovieUIFilter;
import org.tinymediamanager.ui.movies.filters.MovieAllInOneFilter;
import org.tinymediamanager.ui.movies.filters.MovieAspectRatioFilter;
import org.tinymediamanager.ui.movies.filters.MovieAudioChannelFilter;
import org.tinymediamanager.ui.movies.filters.MovieAudioCodecFilter;
import org.tinymediamanager.ui.movies.filters.MovieAudioLanguageFilter;
import org.tinymediamanager.ui.movies.filters.MovieAudioTitleFilter;
import org.tinymediamanager.ui.movies.filters.MovieBannerSizeFilter;
import org.tinymediamanager.ui.movies.filters.MovieCastFilter;
import org.tinymediamanager.ui.movies.filters.MovieCertificationFilter;
import org.tinymediamanager.ui.movies.filters.MovieClearArtSizeFilter;
import org.tinymediamanager.ui.movies.filters.MovieClearLogoSizeFilter;
import org.tinymediamanager.ui.movies.filters.MovieCountAudioStreamFilter;
import org.tinymediamanager.ui.movies.filters.MovieCountSubtitleFilter;
import org.tinymediamanager.ui.movies.filters.MovieCountryFilter;
import org.tinymediamanager.ui.movies.filters.MovieDatasourceFilter;
import org.tinymediamanager.ui.movies.filters.MovieDateAddedFilter;
import org.tinymediamanager.ui.movies.filters.MovieDecadesFilter;
import org.tinymediamanager.ui.movies.filters.MovieDifferentRuntimeFilter;
import org.tinymediamanager.ui.movies.filters.MovieDiscArtSizeFilter;
import org.tinymediamanager.ui.movies.filters.MovieDuplicateFilter;
import org.tinymediamanager.ui.movies.filters.MovieEditionFilter;
import org.tinymediamanager.ui.movies.filters.MovieFanartSizeFilter;
import org.tinymediamanager.ui.movies.filters.MovieFilenameFilter;
import org.tinymediamanager.ui.movies.filters.MovieFrameRateFilter;
import org.tinymediamanager.ui.movies.filters.MovieGenreFilter;
import org.tinymediamanager.ui.movies.filters.MovieHDRFormatFilter;
import org.tinymediamanager.ui.movies.filters.MovieImdbIdFilter;
import org.tinymediamanager.ui.movies.filters.MovieInMovieSetFilter;
import org.tinymediamanager.ui.movies.filters.MovieKeyArtSizeFilter;
import org.tinymediamanager.ui.movies.filters.MovieLanguageFilter;
import org.tinymediamanager.ui.movies.filters.MovieLockedFilter;
import org.tinymediamanager.ui.movies.filters.MovieMediaFilesFilter;
import org.tinymediamanager.ui.movies.filters.MovieMediaSourceFilter;
import org.tinymediamanager.ui.movies.filters.MovieMissingArtworkFilter;
import org.tinymediamanager.ui.movies.filters.MovieMissingMetadataFilter;
import org.tinymediamanager.ui.movies.filters.MovieMissingSubtitlesFilter;
import org.tinymediamanager.ui.movies.filters.MovieNewMoviesFilter;
import org.tinymediamanager.ui.movies.filters.MovieNoteFilter;
import org.tinymediamanager.ui.movies.filters.MoviePathFilter;
import org.tinymediamanager.ui.movies.filters.MoviePosterSizeFilter;
import org.tinymediamanager.ui.movies.filters.MovieProductionCompanyFilter;
import org.tinymediamanager.ui.movies.filters.MovieRuntimeFilter;
import org.tinymediamanager.ui.movies.filters.MovieSubtitleFormatFilter;
import org.tinymediamanager.ui.movies.filters.MovieSubtitleLanguageFilter;
import org.tinymediamanager.ui.movies.filters.MovieTagFilter;
import org.tinymediamanager.ui.movies.filters.MovieThumbSizeFilter;
import org.tinymediamanager.ui.movies.filters.MovieTmdbIdFilter;
import org.tinymediamanager.ui.movies.filters.MovieTrailerFormatFilter;
import org.tinymediamanager.ui.movies.filters.MovieTraktIdFilter;
import org.tinymediamanager.ui.movies.filters.MovieVideo3DFilter;
import org.tinymediamanager.ui.movies.filters.MovieVideoBitdepthFilter;
import org.tinymediamanager.ui.movies.filters.MovieVideoBitrateFilter;
import org.tinymediamanager.ui.movies.filters.MovieVideoCodecFilter;
import org.tinymediamanager.ui.movies.filters.MovieVideoContainerFilter;
import org.tinymediamanager.ui.movies.filters.MovieVideoCrc32Filter;
import org.tinymediamanager.ui.movies.filters.MovieVideoExtrasFilter;
import org.tinymediamanager.ui.movies.filters.MovieVideoFilenameFilter;
import org.tinymediamanager.ui.movies.filters.MovieVideoFilesizeFilter;
import org.tinymediamanager.ui.movies.filters.MovieVideoFormatFilter;
import org.tinymediamanager.ui.movies.filters.MovieWatchedFilter;
import org.tinymediamanager.ui.movies.filters.MovieYearFilter;
import org.tinymediamanager.ui.panels.FilterSavePanel;
import org.tinymediamanager.ui.panels.ModalPopupPanel;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link MovieFilterDialog} provides easy access to all filters
 * 
 * @author Manuel Laggner
 */
public class MovieFilterDialog extends TmmDialog {
  private static final String                    PANEL_COL_CONSTRAINTS = "[][][][200lp:250lp,grow]";

  private final MovieSelectionModel              selectionModel;

  // map for storing which filter is in which panel
  private final Map<JPanel, Set<IMovieUIFilter>> filterMap;
  private final Set<IMovieUIFilter>              filters;
  private final JCheckBox                        chkbxEnableAll;
  private final JTabbedPane                      tabbedPane;

  private JComboBox<String>                      cbPreset;

  public MovieFilterDialog(MovieSelectionModel selectionModel) {
    super(TmmResourceBundle.getString("movieextendedsearch.options") + " - " + TmmResourceBundle.getString("tmm.movies"), "movieFilter");
    setModalityType(ModalityType.MODELESS);

    this.selectionModel = selectionModel;
    this.filterMap = new HashMap<>();
    this.filters = new HashSet<>();
    this.selectionModel.addPropertyChangeListener("filterChanged", evt -> filterChanged());

    ActionListener actionListener = e -> SwingUtilities.invokeLater(() -> {
      String filterName = (String) cbPreset.getSelectedItem();
      if (StringUtils.isNotBlank(filterName)) {
        selectionModel.setFilterValues(MovieModuleManager.getInstance().getSettings().getMovieUiFilterPresets().get(filterName));
      }
      else {
        // clear all filters
        selectionModel.clearFilters();
      }
    });

    {
      tabbedPane = new TmmTabbedPane();
      getContentPane().add(tabbedPane, BorderLayout.CENTER);

      {
        // panel Main
        JPanel panelMain = new JPanel(new MigLayout("", PANEL_COL_CONSTRAINTS, "[]"));
        tabbedPane.addTab(TmmResourceBundle.getString("metatag.details"), createTabPanel(panelMain));

        addFilter(new MovieNewMoviesFilter(), panelMain);
        addFilter(new MovieDatasourceFilter(), panelMain);
        addFilter(new MovieWatchedFilter(), panelMain);
        addFilter(new MovieLockedFilter(), panelMain);
        addFilter(new MovieDateAddedFilter(), panelMain);
        addFilter(new MovieDuplicateFilter(), panelMain);
        addFilter(new MovieInMovieSetFilter(), panelMain);
        addFilter(new MovieAllInOneFilter(), panelMain);
      }

      {
        // panel metadata
        JPanel panelMetadata = new JPanel(new MigLayout("insets 0", PANEL_COL_CONSTRAINTS, "[]"));
        tabbedPane.addTab(TmmResourceBundle.getString("tmm.metadata"), createTabPanel(panelMetadata));

        addFilter(new MovieYearFilter(), panelMetadata);
        addFilter(new MovieDecadesFilter(), panelMetadata);
        addFilter(new MovieGenreFilter(), panelMetadata);
        addFilter(new MovieCertificationFilter(), panelMetadata);
        addFilter(new MovieCastFilter(), panelMetadata);
        addFilter(new MovieCountryFilter(), panelMetadata);
        addFilter(new MovieLanguageFilter(), panelMetadata);
        addFilter(new MovieProductionCompanyFilter(), panelMetadata);

        panelMetadata.add(Box.createHorizontalGlue(), "wrap");

        addFilter(new MovieTagFilter(), panelMetadata);
        addFilter(new MovieEditionFilter(), panelMetadata);
        addFilter(new MovieNoteFilter(), panelMetadata);

        panelMetadata.add(Box.createHorizontalGlue(), "wrap");

        addFilter(new MovieImdbIdFilter(), panelMetadata);
        addFilter(new MovieTmdbIdFilter(), panelMetadata);
        addFilter(new MovieTraktIdFilter(), panelMetadata);
      }

      {
        // panel video
        JPanel panelVideo = new JPanel(new MigLayout("", PANEL_COL_CONSTRAINTS, "[]"));
        tabbedPane.addTab(TmmResourceBundle.getString("metatag.video"), createTabPanel(panelVideo));

        addFilter(new MovieVideoFormatFilter(), panelVideo);
        addFilter(new MovieVideoCodecFilter(), panelVideo);
        addFilter(new MovieVideoBitrateFilter(), panelVideo);
        addFilter(new MovieVideoBitdepthFilter(), panelVideo);
        addFilter(new MovieVideoContainerFilter(), panelVideo);
        addFilter(new MovieAspectRatioFilter(), panelVideo);
        addFilter(new MovieFrameRateFilter(), panelVideo);
        addFilter(new MovieVideo3DFilter(), panelVideo);
        addFilter(new MovieVideoCrc32Filter(), panelVideo);
        addFilter(new MovieHDRFormatFilter(), panelVideo);
      }

      {
        // panel audio
        JPanel panelAudio = new JPanel(new MigLayout("", PANEL_COL_CONSTRAINTS, "[]"));
        tabbedPane.addTab(TmmResourceBundle.getString("metatag.audio"), createTabPanel(panelAudio));

        addFilter(new MovieAudioCodecFilter(), panelAudio);
        addFilter(new MovieAudioChannelFilter(), panelAudio);
        addFilter(new MovieCountAudioStreamFilter(), panelAudio);
        addFilter(new MovieAudioLanguageFilter(), panelAudio);
        addFilter(new MovieAudioTitleFilter(), panelAudio);
      }

      {
        // panel artwork
        JPanel panelArtwork = new JPanel(new MigLayout("", PANEL_COL_CONSTRAINTS, "[]"));
        tabbedPane.addTab(TmmResourceBundle.getString("filter.artwork"), createTabPanel(panelArtwork));

        addFilter(new MoviePosterSizeFilter(), panelArtwork);
        addFilter(new MovieFanartSizeFilter(), panelArtwork);
        addFilter(new MovieBannerSizeFilter(), panelArtwork);
        addFilter(new MovieThumbSizeFilter(), panelArtwork);
        addFilter(new MovieClearLogoSizeFilter(), panelArtwork);
        addFilter(new MovieClearArtSizeFilter(), panelArtwork);
        addFilter(new MovieDiscArtSizeFilter(), panelArtwork);
        addFilter(new MovieKeyArtSizeFilter(), panelArtwork);
        addFilter(new MovieMissingArtworkFilter(), panelArtwork);
      }

      {
        // panel other
        JPanel panelOther = new JPanel(new MigLayout("", PANEL_COL_CONSTRAINTS, "[]"));
        tabbedPane.addTab(TmmResourceBundle.getString("filter.others"), createTabPanel(panelOther));

        addFilter(new MovieMediaSourceFilter(), panelOther);
        addFilter(new MovieMediaFilesFilter(), panelOther);
        addFilter(new MovieFilenameFilter(), panelOther);
        addFilter(new MovieVideoFilesizeFilter(), panelOther);
        addFilter(new MovieVideoFilenameFilter(), panelOther);
        addFilter(new MoviePathFilter(), panelOther);
        addFilter(new MovieCountSubtitleFilter(), panelOther);
        addFilter(new MovieSubtitleLanguageFilter(), panelOther);
        addFilter(new MovieSubtitleFormatFilter(), panelOther);
        addFilter(new MovieVideoExtrasFilter(), panelOther);
        addFilter(new MovieTrailerFormatFilter(), panelOther);
        addFilter(new MovieRuntimeFilter(), panelOther);
        addFilter(new MovieDifferentRuntimeFilter(), panelOther);

        panelOther.add(Box.createHorizontalGlue(), "wrap");

        addFilter(new MovieMissingMetadataFilter(), panelOther);
        addFilter(new MovieMissingSubtitlesFilter(), panelOther);
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
        chkbxEnableAll.addActionListener(e -> selectionModel.setFiltersActive(chkbxEnableAll.isSelected()));
        panelFilterPreset.add(chkbxEnableAll, "cell 2 2");

        JLabel lblFilterPresetT = new TmmLabel(TmmResourceBundle.getString("filter.presets"));
        panelFilterPreset.add(lblFilterPresetT, "cell 1 3, alignx trailing");

        cbPreset = new JComboBox<>();
        panelFilterPreset.add(cbPreset, "cell 2 3");

        JButton btnSavePreset = new FlatButton(IconManager.SAVE);
        btnSavePreset.setToolTipText(TmmResourceBundle.getString("filter.savepreset"));
        btnSavePreset.addActionListener(e -> {
          Set<UIFilters> activeUiFilters = getActiveUiFilters();
          if (!activeUiFilters.isEmpty()) {
            Map<String, List<UIFilters>> uiFilters = new HashMap<>(MovieModuleManager.getInstance().getSettings().getMovieUiFilterPresets());

            ModalPopupPanel popupPanel = createModalPopupPanel();
            popupPanel.setTitle(TmmResourceBundle.getString("filter.savepreset"));

            FilterSavePanel filterSavePanel = new FilterSavePanel(activeUiFilters, uiFilters);

            popupPanel.setOnCloseHandler(() -> {
              String savedPreset = filterSavePanel.getSavedPreset();
              if (StringUtils.isNotBlank(savedPreset)) {
                cbPreset.removeActionListener(actionListener);
                MovieModuleManager.getInstance().getSettings().setMovieUiFilterPresets(uiFilters);
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
          if (Boolean.FALSE.equals(TmmProperties.getInstance().getPropertyAsBoolean("movie.hidefilterhint"))) {
            JCheckBox checkBox = new JCheckBox(TmmResourceBundle.getString("tmm.donotshowagain"));
            TmmFontHelper.changeFont(checkBox, L1);
            checkBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

            Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
            Object[] params = { TmmResourceBundle.getString("filter.remove"), checkBox };
            int answer = JOptionPane.showOptionDialog(MainWindow.getInstance(), params, TmmResourceBundle.getString("filter.remove"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);

            // the user don't want to show this dialog again
            if (checkBox.isSelected()) {
              TmmProperties.getInstance().putProperty("movie.hidefilterhint", String.valueOf(checkBox.isSelected()));
            }

            if (answer != JOptionPane.YES_OPTION) {
              return;
            }
          }

          Map<String, List<UIFilters>> movieUiFilters = new HashMap<>(MovieModuleManager.getInstance().getSettings().getMovieUiFilterPresets());
          if (movieUiFilters.remove(filterName) != null) {
            cbPreset.removeActionListener(actionListener);
            MovieModuleManager.getInstance().getSettings().setMovieUiFilterPresets(movieUiFilters);
            MovieModuleManager.getInstance().getSettings().saveSettings();
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
    panel.add(new FlatButton(IconManager.DELETE, e -> SwingUtilities.invokeLater(selectionModel::clearFilters)), "cell 1 0, right");

    JScrollPane scrollPane = new NoBorderScrollPane(content);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.getVerticalScrollBar().setUnitIncrement(4);

    panel.add(scrollPane, "cell 0 2 2 1, grow, wrap");

    return panel;
  }

  private Set<UIFilters> getActiveUiFilters() {
    return new HashSet<>(IMovieUIFilter.morphToUiFilters(filters));
  }

  private void loadPresets() {
    String preset = (String) cbPreset.getSelectedItem();

    cbPreset.removeAllItems();
    cbPreset.addItem("");
    MovieModuleManager.getInstance().getSettings().getMovieUiFilterPresets().keySet().stream().sorted().forEach(key -> cbPreset.addItem(key));

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
  private void addFilter(IMovieUIFilter filter, JPanel panel) {
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

    selectionModel.addFilter(filter);
  }

  /**
   * re-calculate if the active filter icon should be displayed
   */
  private void filterChanged() {
    for (Map.Entry<JPanel, Set<IMovieUIFilter>> entry : filterMap.entrySet()) {
      boolean active = false;

      if (chkbxEnableAll.isSelected()) {
        for (IMovieUIFilter filter : entry.getValue()) {
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
