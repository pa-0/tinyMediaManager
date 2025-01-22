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
package org.tinymediamanager.core.tvshow;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.entities.TvShow;

/**
 * The class {@link TvShowRenamerPreviewContainer}. To hold all relevant data for the renamer preview
 * 
 * @author Manuel Laggner
 */
public class TvShowRenamerPreviewContainer {
  final TvShow          tvShow;
  final Path            oldPath;
  final List<MediaFile> oldMediaFiles;
  final List<MediaFile> newMediaFiles;

  Path                  newPath;
  boolean               needsRename = false;

  public TvShowRenamerPreviewContainer(TvShow tvShow) {
    this.tvShow = tvShow;
    this.newMediaFiles = new ArrayList<>();
    this.oldMediaFiles = new ArrayList<>();

    if (!tvShow.getDataSource().isEmpty()) {
      this.oldPath = tvShow.getPathNIO();
    }
    else {
      this.oldPath = null;
    }
  }

  public TvShow getTvShow() {
    return tvShow;
  }

  public Path getOldPath() {
    return oldPath;
  }

  public Path getOldPathRelative() {
    return Paths.get(tvShow.getDataSource()).relativize(tvShow.getPathNIO());
  }

  public Path getNewPath() {
    return newPath;
  }

  public Path getNewPathRelative() {
    return Paths.get(tvShow.getDataSource()).relativize(newPath);
  }

  public List<MediaFile> getOldMediaFiles() {
    return oldMediaFiles;
  }

  public List<MediaFile> getNewMediaFiles() {
    return newMediaFiles;
  }

  public boolean isNeedsRename() {
    return needsRename;
  }
}
