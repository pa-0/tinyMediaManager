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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a media source movie filter
 * 
 * @author Manuel Laggner
 */
public class MovieMediaSourceFilter extends AbstractCheckComboBoxMovieUIFilter<MediaSource> {

  public MovieMediaSourceFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> s.toString().toLowerCase(Locale.ROOT).startsWith(s2.toLowerCase(Locale.ROOT)));
    // initial filling
    buildAndInstallMediaSourceArray();
    // listen to changes
    MediaSource.addListener(evt -> SwingUtilities.invokeLater(this::buildAndInstallMediaSourceArray));
  }

  @Override
  public String getId() {
    return "movieMediaSource";
  }

  @Override
  public boolean accept(Movie movie) {
    List<MediaSource> selectedItems = checkComboBox.getSelectedItems();
    return selectedItems.contains(movie.getMediaSource());
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.source"));
  }

  @Override
  protected String parseTypeToString(MediaSource type) throws Exception {
    return type.name();
  }

  @Override
  protected MediaSource parseStringToType(String string) throws Exception {
    return MediaSource.getMediaSource(string);
  }

  private void buildAndInstallMediaSourceArray() {
    List<MediaSource> mediaSources = new ArrayList<>();

    for (MediaSource mediaSource : MediaSource.values()) {
      if (StringUtils.isNotBlank(mediaSource.toString())) {
        mediaSources.add(mediaSource);
      }
    }

    setValues(mediaSources);
  }
}
