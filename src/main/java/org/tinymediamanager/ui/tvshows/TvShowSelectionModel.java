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
package org.tinymediamanager.ui.tvshows;

import static org.tinymediamanager.ui.TmmFontHelper.L1;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.treetable.TmmTreeTable;

/**
 * The Class TvShowSelectionModel.
 * 
 * @author Manuel Laggner
 */
public class TvShowSelectionModel extends AbstractModelObject {
  private static final String          SELECTED_TV_SHOW = "selectedTvShow";

  private final TvShow                 initalTvShow     = new TvShow();
  private final PropertyChangeListener propertyChangeListener;

  private TvShow                       selectedTvShow;
  private TmmTreeTable                 treeTable;

  /**
   * Instantiates a new tv show selection model. Usage in TvShowPanel
   */
  public TvShowSelectionModel() {
    selectedTvShow = initalTvShow;
    propertyChangeListener = evt -> {
      if (evt.getSource() == selectedTvShow) {
        // wrap this event in a new event for listeners of the selection model
        firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
      }
    };
  }

  public void setTreeTable(TmmTreeTable treeTable) {
    this.treeTable = treeTable;
  }

  /**
   * Sets the selected tv show.
   * 
   * @param tvShow
   *          the new selected tv show
   */
  public void setSelectedTvShow(TvShow tvShow) {
    // no need to fire events if nothing has been changed
    if (tvShow == selectedTvShow) {
      return;
    }

    TvShow oldValue = this.selectedTvShow;
    if (oldValue != null && oldValue != initalTvShow) {
      oldValue.removePropertyChangeListener(propertyChangeListener);
    }

    if (tvShow != null) {
      this.selectedTvShow = tvShow;
      selectedTvShow.addPropertyChangeListener(propertyChangeListener);
    }
    else {
      this.selectedTvShow = initalTvShow;
    }

    if (oldValue != null) {
      oldValue.removePropertyChangeListener(propertyChangeListener);
    }

    if (selectedTvShow != null) {
      selectedTvShow.addPropertyChangeListener(propertyChangeListener);
    }

    firePropertyChange(SELECTED_TV_SHOW, oldValue, this.selectedTvShow);
  }

  /**
   * Gets the selected tv show.
   * 
   * @return the selected tv show
   */
  public TvShow getSelectedTvShow() {
    return selectedTvShow;
  }

  /**
   * Gets the selected TV shows (without locked ones)
   * 
   * @return the selected TV shows
   */
  public List<TvShow> getSelectedTvShows() {
    return getSelectedTvShows(false);
  }

  /**
   * Gets the selected TV shows
   * 
   * @param withLocked
   *          also get locked TV shows
   *
   * @return the selected TV shows
   */
  public List<TvShow> getSelectedTvShows(boolean withLocked) {
    SelectedObjects selectedObjects = getSelectedObjects(false, withLocked);

    if (selectedObjects.lockedFound) {
      showLockedInformation();
    }

    return new ArrayList<>(selectedObjects.tvShows);
  }

  /**
   * Gets the selected TV shows (recursive - also from child node) - without locked ones
   *
   * @return the selected TV shows
   */
  public List<TvShow> getSelectedTvShowsRecursive() {
    return getSelectedTvShowsRecursive(false);
  }

  /**
   * Gets the selected TV shows (recursive - also from child node)
   * 
   * @param withLocked
   *          also get locked TV shows
   *
   * @return the selected TV shows
   */
  public List<TvShow> getSelectedTvShowsRecursive(boolean withLocked) {
    SelectedObjects selectedObjects = getSelectedObjects(false, withLocked);

    Set<TvShow> selectedTvShows = new LinkedHashSet<>(selectedObjects.tvShows);

    for (TvShowSeason season : selectedObjects.seasons) {
      selectedTvShows.add(season.getTvShow());
    }

    for (TvShowEpisode episode : selectedObjects.episodes) {
      selectedTvShows.add(episode.getTvShow());
    }

    if (selectedObjects.lockedFound) {
      showLockedInformation();
    }

    return new ArrayList<>(selectedTvShows);
  }

  /**
   * Get the selected episodes
   *
   * @return the selected episodes - without locked ones
   */
  public List<TvShowEpisode> getSelectedEpisodes() {
    return getSelectedEpisodes(false);
  }

  /**
   * Get the selected episodes
   * 
   * @param withLocked
   *          also get episodes from locked TV shows
   * @return the selected episodes - without locked ones
   */
  public List<TvShowEpisode> getSelectedEpisodes(boolean withLocked) {
    Set<TvShowEpisode> episodes = new LinkedHashSet<>();

    SelectedObjects selectedObjects = getSelectedObjects(false, withLocked);

    for (TvShow tvShow : selectedObjects.tvShows) {
      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        if (!episode.isDummy()) {
          episodes.add(episode);
        }
      }
    }

    for (TvShowSeason season : selectedObjects.seasons) {
      for (TvShowEpisode episode : season.getEpisodes()) {
        if (!episode.isDummy()) {
          episodes.add(episode);
        }
      }
    }

    episodes.addAll(selectedObjects.episodes);

    if (selectedObjects.lockedFound) {
      showLockedInformation();
    }

    return new ArrayList<>(episodes);
  }

  /**
   * Get all selected objects from the treeTable w/o dummies and w/o locked objects
   * 
   * @return the selected objects
   */
  public SelectedObjects getSelectedObjects() {
    return getSelectedObjects(false, false);
  }

  /**
   * Get all selected objects from the treeTable
   * 
   * @param withDummy
   *          with or without dummies
   * @param withLocked
   *          with locked objects
   * 
   * @return the selected objects
   */
  public SelectedObjects getSelectedObjects(boolean withDummy, boolean withLocked) {
    SelectedObjects selectedObjects = new SelectedObjects();

    for (int row : treeTable.getSelectedRows()) {

      DefaultMutableTreeNode node = treeTable.getTreeNode(row);
      if (node != null) {
        Object userObject = node.getUserObject();
        if (userObject instanceof TvShow) {
          TvShow tvShow = (TvShow) userObject;
          if (withLocked || !tvShow.isLocked()) {
            selectedObjects.tvShows.add(tvShow);
          }
          else {
            selectedObjects.lockedFound = true;
          }
        }
        else if (userObject instanceof TvShowSeason) {
          TvShowSeason season = (TvShowSeason) userObject;
          if (withLocked || !season.isLocked()) {
            if (!season.isDummy()) {
              selectedObjects.seasons.add(season);
            }
            else if (season.isDummy() && withDummy) {
              selectedObjects.seasons.add(season);
            }
          }
          else {
            selectedObjects.lockedFound = true;
          }
        }
        else if (userObject instanceof TvShowEpisode) {
          TvShowEpisode episode = (TvShowEpisode) userObject;
          if (withLocked || !episode.isLocked()) {
            if (!episode.isDummy()) {
              selectedObjects.episodes.add(episode);
            }
            else if (episode.isDummy() && withDummy) {
              selectedObjects.episodes.add(episode);
            }
          }
          else {
            selectedObjects.lockedFound = true;
          }
        }
      }
    }

    return selectedObjects;
  }

  /**
   * get the selected objects directly from the tree in the same order (w/o dummy entries)
   * 
   * @return a {@link List} of all selected objects
   */
  public List<Object> getSelectedTreeObjects() {
    List<Object> selectedObjects = new ArrayList<>();

    for (int row : treeTable.getSelectedRows()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeTable.getValueAt(row, 0);
      if (node != null) {
        Object userObject = node.getUserObject();
        if (userObject instanceof TvShow) {
          TvShow tvShow = (TvShow) userObject;
          selectedObjects.add(tvShow);
        }
        else if (userObject instanceof TvShowSeason) {
          TvShowSeason season = (TvShowSeason) userObject;
          if (!season.isDummy()) {
            selectedObjects.add(season);
          }
        }
        else if (userObject instanceof TvShowEpisode) {
          TvShowEpisode episode = (TvShowEpisode) userObject;

          if (!episode.isDummy()) {
            selectedObjects.add(episode);
          }
        }
      }
    }

    return selectedObjects;
  }

  public static void showLockedInformation() {
    if (Boolean.FALSE.equals(TmmProperties.getInstance().getPropertyAsBoolean("tvshow.hidelockedhint"))) {
      JCheckBox checkBox = new JCheckBox(TmmResourceBundle.getString("tmm.donotshowagain"));
      TmmFontHelper.changeFont(checkBox, L1);
      checkBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

      Object[] params = { TmmResourceBundle.getString("tvshow.lockedfound.desc"), checkBox };
      JOptionPane.showMessageDialog(MainWindow.getInstance(), params, TmmResourceBundle.getString("tvshow.lockedfound"),
          JOptionPane.INFORMATION_MESSAGE);

      // the user don't want to show this dialog again
      if (checkBox.isSelected()) {
        TmmProperties.getInstance().putProperty("tvshow.hidelockedhint", String.valueOf(checkBox.isSelected()));
      }
    }
  }

  public static class SelectedObjects {
    private final Set<TvShow>        tvShows     = new LinkedHashSet<>();
    private final Set<TvShowSeason>  seasons     = new LinkedHashSet<>();
    private final Set<TvShowEpisode> episodes    = new LinkedHashSet<>();
    private boolean                  lockedFound = false;

    public Set<TvShow> getTvShows() {
      return tvShows;
    }

    public Set<TvShowSeason> getSeasons() {
      return seasons;
    }

    public Set<TvShowSeason> getSeasonsRecursive() {
      Set<TvShowSeason> allSeasons = new LinkedHashSet<>(seasons);

      tvShows.forEach(tvShow -> allSeasons.addAll(tvShow.getSeasons()));

      return allSeasons;
    }

    public Set<TvShowEpisode> getEpisodes() {
      return episodes;
    }

    public Set<TvShowEpisode> getEpisodesRecursive() {
      Set<TvShowEpisode> allEpisodes = new LinkedHashSet<>(episodes);

      tvShows.forEach(tvShow -> allEpisodes.addAll(tvShow.getEpisodes()));
      seasons.forEach(season -> allEpisodes.addAll(season.getEpisodes()));

      return allEpisodes;
    }

    public boolean isLockedFound() {
      return lockedFound;
    }

    public boolean isEmpty() {
      return tvShows.isEmpty() && seasons.isEmpty() && episodes.isEmpty();
    }
  }
}
