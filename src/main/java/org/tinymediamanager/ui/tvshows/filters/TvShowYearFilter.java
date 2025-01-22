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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * the class {@link TvShowYearFilter} is used to filter the TV shows by the year
 * 
 * @author Wolfgang Janes
 */
public class TvShowYearFilter extends AbstractCheckComboBoxTvShowUIFilter<Integer> {
  private final TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

  public TvShowYearFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> String.valueOf(s).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildYearArray();
    tvShowList.addPropertyChangeListener(Constants.YEAR, evt -> SwingUtilities.invokeLater(this::buildYearArray));
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.year"));
  }

  @Override
  public String getId() {
    return "tvShowYear";
  }

  @Override
  protected String parseTypeToString(Integer type) throws Exception {
    return type.toString();
  }

  @Override
  protected Integer parseStringToType(String string) throws Exception {
    return Integer.parseInt(string);
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    List<Integer> selectedItems = checkComboBox.getSelectedItems();
    return invert ^ selectedItems.contains(tvShow.getYear());
  }

  private void buildYearArray() {
    Set<Integer> yearSet = new HashSet<>();
    tvShowList.getTvShows().forEach(tvShow -> yearSet.add(tvShow.getYear()));
    List<Integer> years = new ArrayList<>(ListUtils.asSortedList(yearSet));
    Collections.sort(years);
    setValues(years);
  }
}
