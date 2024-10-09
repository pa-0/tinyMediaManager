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
package org.tinymediamanager.ui.movies.filters;

import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;

import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a trailer format movie filter
 * 
 * @author Manuel Laggner
 */
public class MovieTrailerFormatFilter extends AbstractCheckComboBoxMovieUIFilter<String> {

  public MovieTrailerFormatFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> s.toLowerCase(Locale.ROOT).startsWith(s2.toLowerCase(Locale.ROOT)));
    setValues(MediaFileHelper.getVideoFormats());
  }

  @Override
  public String getId() {
    return "movieTrailerFormat";
  }

  @Override
  public boolean accept(Movie movie) {
    List<String> selectedValues = checkComboBox.getSelectedItems();

    for (MediaFile trailer : movie.getMediaFiles(MediaFileType.TRAILER)) {
      for (String videoFormat : selectedValues) {
        if (MediaFileHelper.VIDEO_FORMAT_UHD.equals(videoFormat) && trailer.isVideoDefinitionUHD()) {
          return true;
        }
        else if (MediaFileHelper.VIDEO_FORMAT_HD.equals(videoFormat) && trailer.isVideoDefinitionHD()) {
          return true;
        }
        else if (MediaFileHelper.VIDEO_FORMAT_SD.equals(videoFormat) && trailer.isVideoDefinitionSD()) {
          return true;
        }
        else if (MediaFileHelper.VIDEO_FORMAT_LD.equals(videoFormat) && trailer.isVideoDefinitionLD()) {
          return true;
        }
        else if (videoFormat.equals(trailer.getVideoFormat())) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("movieextendedsearch.trailerresolution"));
  }

  @Override
  protected String parseTypeToString(String type) throws Exception {
    return type;
  }

  @Override
  protected String parseStringToType(String string) throws Exception {
    return string;
  }
}
