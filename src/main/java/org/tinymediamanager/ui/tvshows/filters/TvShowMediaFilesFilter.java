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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.swing.JLabel;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * This class implements a media type filter for the TV show tree
 *
 * @author Wolfgang Janes
 */
public class TvShowMediaFilesFilter extends AbstractCheckComboBoxTvShowUIFilter<TvShowMediaFilesFilter.MediaFileTypeContainer> {

  public TvShowMediaFilesFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> s.toString().toLowerCase(Locale.ROOT).startsWith(s2.toLowerCase(Locale.ROOT)));

    List<TvShowMediaFilesFilter.MediaFileTypeContainer> mediaFileTypeList = new ArrayList<>();

    for (MediaFileType type : MediaFileType.values()) {
      switch (type) {
        // explicit exclude
        case VIDEO_EXTRA:
        case GRAPHIC:
        case DOUBLE_EXT:
          break;

        default:
          mediaFileTypeList.add(new TvShowMediaFilesFilter.MediaFileTypeContainer(type));
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
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {

    boolean isValid = false;

    List<MediaFileType> selectedItems = new ArrayList<>();
    for (TvShowMediaFilesFilter.MediaFileTypeContainer container : checkComboBox.getSelectedItems()) {
      selectedItems.add(container.type);
    }

    // first: filter on the media files from episodes
    for (TvShowEpisode episode : episodes) {
      boolean foundEpisode = false;
      List<MediaFile> mfs = episode.getMediaFiles();
      for (MediaFile mf : mfs) {
        if (selectedItems.contains(mf.getType())) {
          foundEpisode = true;
          break;
        }
      }

      // if there is a match in this episode, we can stop
      if (invert && !foundEpisode) {
        return true;
      }
      else if (!invert && foundEpisode) {
        return true;
      }
    }

    // second: filter on the media files of the TV show
    boolean foundShow = false;
    for (MediaFile mf : tvShow.getMediaFiles()) {
      if (selectedItems.contains(mf.getType())) {
        foundShow = true;
        break;
      }
    }

    // if we found anything in the show we can quit here
    if (!invert && foundShow) {
      return true;
    }
    else if (invert && foundShow) {
      return false;
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.mediatype"));
  }

  @Override
  public String getId() {
    return "tvShowMediaFiles";
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
