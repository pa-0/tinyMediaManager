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
package org.tinymediamanager.ui.tvshows.filters;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a locked tv show filter
 *
 * @author Wolfgang Janes
 */
public class TvShowLockedFilter extends AbstractTvShowUIFilter {

  private JComboBox<LockedFlag> comboBox;

  private enum LockedFlag {
    LOCKED(TmmResourceBundle.getString("metatag.locked")),
    NOT_LOCKED(TmmResourceBundle.getString("metatag.unlocked"));

    private final String title;

    LockedFlag(String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  @Override
  public String getId() {
    return "tvShowLocked";
  }

  @Override
  public String getFilterValueAsString() {
    try {
      return ((LockedFlag) comboBox.getSelectedItem()).name();
    }
    catch (Exception e) {
      return null;
    }
  }

  @Override
  public void setFilterValue(Object value) {
    if (value == null) {
      return;
    }
    if (value instanceof LockedFlag) {
      comboBox.setSelectedItem(value);
    }
    else if (value instanceof String) {
      LockedFlag lockedFlag = LockedFlag.valueOf((String) value);
      if (lockedFlag != null) {
        comboBox.setSelectedItem(lockedFlag);
      }
    }
  }

  @Override
  public void clearFilter() {
    comboBox.setSelectedItem(comboBox.getItemAt(0));
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    if (invert ^ tvShow.isLocked() == (comboBox.getSelectedItem() == LockedFlag.LOCKED)) {
      return true;
    }
    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.locked"));
  }

  @Override
  protected JComponent createFilterComponent() {
    comboBox = new JComboBox<>(LockedFlag.values());
    return comboBox;
  }

}
