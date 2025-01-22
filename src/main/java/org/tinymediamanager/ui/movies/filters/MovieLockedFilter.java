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
package org.tinymediamanager.ui.movies.filters;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a locked movie filter
 * 
 * @author Wolfgang Janes
 */
public class MovieLockedFilter extends AbstractMovieUIFilter {

  private JComboBox<LockedFlag> combobox;

  private enum LockedFlag {
    LOCKED(TmmResourceBundle.getString("metatag.locked")),
    UNLOCKED(TmmResourceBundle.getString("metatag.unlocked"));

    private final String title;

    private LockedFlag(String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  @Override
  public boolean accept(Movie movie) {
    return !(movie.isLocked() ^ combobox.getSelectedItem() == LockedFlag.LOCKED);
  }

  @Override
  public String getId() {
    return "movieLocked";
  }

  @Override
  public String getFilterValueAsString() {
    try {
      return ((LockedFlag) combobox.getSelectedItem()).name();
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
      combobox.setSelectedItem(value);
    }
    else if (value instanceof String) {
      LockedFlag lockedFlag = LockedFlag.valueOf((String) value);
      if (lockedFlag != null) {
        combobox.setSelectedItem(lockedFlag);
      }
    }
  }

  @Override
  public void clearFilter() {
    // just set the default value
    combobox.setSelectedItem(combobox.getItemAt(0));
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.locked"));
  }

  @Override
  protected JComponent createFilterComponent() {
    combobox = new JComboBox<>(LockedFlag.values());
    return combobox;
  }

}
