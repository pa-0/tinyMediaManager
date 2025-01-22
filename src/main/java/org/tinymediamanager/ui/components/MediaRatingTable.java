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
package org.tinymediamanager.ui.components;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.table.TmmEditorTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.panels.IModalPopupPanelProvider;
import org.tinymediamanager.ui.panels.ModalPopupPanel;
import org.tinymediamanager.ui.panels.RatingEditorPanel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

/**
 * The class {@link MediaRatingTable} is used to display / edit ratings
 *
 * @author Manuel Laggner
 */
public class MediaRatingTable extends TmmEditorTable {
  private final EventList<Rating> ratingList;

  /**
   * this constructor is used to edit the ratings
   *
   * @param ratings
   *          an eventlist containing the ratings
   */
  public MediaRatingTable(EventList<Rating> ratings) {
    super();

    this.ratingList = ratings;

    setModel(new TmmTableModel<>(ratingList, new MediaRatingTableFormat()));

    // setTableHeader(null);
    adjustColumnPreferredWidths(3);
  }

  public static EventList<Rating> convertRatingMapToEventList(Map<String, org.tinymediamanager.core.entities.MediaRating> idMap,
      boolean withUserRating) {
    EventList<Rating> idList = new BasicEventList<>();
    for (Entry<String, org.tinymediamanager.core.entities.MediaRating> entry : idMap.entrySet()) {
      if (org.tinymediamanager.core.entities.MediaRating.USER.equals(entry.getKey()) && !withUserRating) {
        continue;
      }

      Rating id = new Rating(entry.getKey());
      org.tinymediamanager.core.entities.MediaRating mediaRating = entry.getValue();

      id.value = mediaRating.getRating();
      id.votes = mediaRating.getVotes();
      id.maxValue = mediaRating.getMaxValue();

      idList.add(id);
    }

    return idList;
  }

  public static EventList<Rating> convertRatingMapToEventList(List<org.tinymediamanager.core.entities.MediaRating> ratings) {
    EventList<Rating> idList = new BasicEventList<>();
    for (org.tinymediamanager.core.entities.MediaRating rating : ratings) {
      Rating id = new Rating(rating.getId());

      id.value = rating.getRating();
      id.votes = rating.getVotes();
      id.maxValue = rating.getMaxValue();

      idList.add(id);
    }

    return idList;
  }

  @Override
  protected void editButtonClicked(int row) {
    IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(MediaRatingTable.this);
    if (iModalPopupPanelProvider == null) {
      return;
    }

    int index = convertRowIndexToModel(row);
    Rating rating = ratingList.get(index);

    ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
    popupPanel.setTitle(TmmResourceBundle.getString("rating.edit"));

    RatingEditorPanel ratingEditorPanel = new RatingEditorPanel(rating);
    popupPanel.setContent(ratingEditorPanel);
    iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
  }

  public static class Rating {
    public String key;
    public float  value;
    public int    maxValue;
    public int    votes;

    public Rating(String key) {
      this.key = key;
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(13, 31).append(key).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Rating other)) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      return StringUtils.equals(key, other.key);
    }
  }

  /**
   * helper classes
   */
  private static class MediaRatingTableFormat extends TmmTableFormat<Rating> {
    private MediaRatingTableFormat() {
      /*
       * source
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.rating.source"), "source", rating -> rating.key, String.class);
      addColumn(col);

      /*
       * rating
       */
      col = new Column(TmmResourceBundle.getString("metatag.rating"), "rating", rating -> rating.value, Float.class);
      addColumn(col);

      /*
       * maxvalue
       */
      col = new Column(TmmResourceBundle.getString("metatag.rating.maxvalue"), "maxvalue", rating -> rating.maxValue, Integer.class);
      addColumn(col);

      /*
       * votes
       */
      col = new Column(TmmResourceBundle.getString("metatag.rating.votes"), "votes", rating -> rating.votes, Integer.class);
      addColumn(col);

      /*
       * edit
       */
      col = new Column(TmmResourceBundle.getString("Button.edit"), "edit", person -> IconManager.EDIT, ImageIcon.class);
      col.setColumnResizeable(false);
      col.setHeaderIcon(IconManager.EDIT_HEADER);
      addColumn(col);
    }
  }
}
