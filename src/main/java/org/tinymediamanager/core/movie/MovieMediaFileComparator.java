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
package org.tinymediamanager.core.movie;

import java.util.Comparator;

import org.tinymediamanager.core.entities.MediaFile;

/**
 * For logical sorting of mediafiles
 * 
 * @author Manuel Laggner
 */
public class MovieMediaFileComparator implements Comparator<MediaFile> {

  @Override
  public int compare(MediaFile mf1, MediaFile mf2) {
    if (mf1.getType().ordinal() != mf2.getType().ordinal()) {
      return mf1.getType().ordinal() - mf2.getType().ordinal();
    }

    // starting from here we have the same MediaFileType
    // sort according to different criteria based on the type
    switch (mf1.getType()) {
      // sort all video files / extra artwork and by name
      case VIDEO:
      case TRAILER:
      case EXTRAFANART:
      case EXTRATHUMB:
        return mf1.getFilename().compareTo(mf2.getFilename());

      // sort the rest by filename length (descending) - assume that the kodi standard -poster, -fanart is always on top in this case
      default:
        return Integer.compare(mf2.getFilename().length(), mf1.getFilename().length());
    }
  }
}
