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
import java.util.Objects;

import javax.swing.JLabel;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * This class implements a media type filter for movies
 *
 * @author Wolfgang Janes
 */
public class MovieMediaFilesFilter extends AbstractCheckComboBoxMovieUIFilter<MovieMediaFilesFilter.MediaFileTypeContainer> {

  public MovieMediaFilesFilter() {
    super();

    checkComboBox.enableFilter((s, s2) -> s.toString().toLowerCase(Locale.ROOT).startsWith(s2.toLowerCase(Locale.ROOT)));

    List<MediaFileTypeContainer> mediaFileTypeList = new ArrayList<>();

    for (MediaFileType type : MediaFileType.values()) {
      switch (type) {
        // explicit exclude
        case VIDEO_EXTRA:
        case GRAPHIC:
        case DOUBLE_EXT:
          break;

        default:
          mediaFileTypeList.add(new MediaFileTypeContainer(type));
          break;

      }
    }
    setValues(mediaFileTypeList);
  }

  @Override
  protected String parseTypeToString(MediaFileTypeContainer container) throws Exception {
    return container.type.name();
  }

  @Override
  protected MediaFileTypeContainer parseStringToType(String string) throws Exception {
    return new MediaFileTypeContainer(MediaFileType.valueOf(string));
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.mediatype"));
  }

  @Override
  public String getId() {
    return "movieMediaFiles";
  }

  @Override
  public boolean accept(Movie movie) {

    boolean isValid = false;

    List<MediaFileType> selectedItems = new ArrayList<>();
    for (MediaFileTypeContainer container : checkComboBox.getSelectedItems()) {
      selectedItems.add(container.type);
    }

    for (MediaFile mf : movie.getMediaFiles()) {
      if (selectedItems.contains(mf.getType())) {
        isValid = true;
        break;
      }
    }

    return isValid;
  }

  public static class MediaFileTypeContainer {
    private final MediaFileType type;

    public MediaFileTypeContainer(MediaFileType type) {
      this.type = type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      MediaFileTypeContainer that = (MediaFileTypeContainer) o;
      return type == that.type;
    }

    @Override
    public int hashCode() {
      return Objects.hash(type);
    }

    @Override
    public String toString() {
      try {
        return TmmResourceBundle.getString("mediafiletype." + type.name().toLowerCase(Locale.ROOT));
      }
      catch (Exception e) {
        return type.toString();
      }
    }
  }
}
