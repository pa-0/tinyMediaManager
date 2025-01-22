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

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * the class {@link TvShowCertificationFilter} is used to provide a filter for the certifications of a TV show
 * 
 * @author Wolfgang Janes
 */
public class TvShowCertificationFilter extends AbstractCheckComboBoxTvShowUIFilter<MediaCertification> {
  private final TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

  public TvShowCertificationFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> s.toString().toLowerCase(Locale.ROOT).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildAndInstallCertificationArray();
    PropertyChangeListener propertyChangeListener = evt -> buildAndInstallCertificationArray();
    tvShowList.addPropertyChangeListener(Constants.CERTIFICATION, propertyChangeListener);
  }

  @Override
  protected String parseTypeToString(MediaCertification type) throws Exception {
    return type.name();
  }

  @Override
  protected MediaCertification parseStringToType(String string) throws Exception {
    return MediaCertification.valueOf(string);
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    List<MediaCertification> selectedItems = checkComboBox.getSelectedItems();
    if (invert) {
      return !selectedItems.contains(tvShow.getCertification());
    }
    else {
      return selectedItems.contains(tvShow.getCertification());
    }

  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.certification"));
  }

  @Override
  public String getId() {
    return "tvShowCertification";
  }

  private void buildAndInstallCertificationArray() {
    List<MediaCertification> certifications = new ArrayList<>(tvShowList.getCertification());
    Collections.sort(certifications);
    setValues(certifications);
  }
}
