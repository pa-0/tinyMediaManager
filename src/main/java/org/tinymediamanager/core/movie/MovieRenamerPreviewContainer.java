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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;

/**
 * The class MovieRenamerPreviewContainter. To hold all relevant data for the renamer preview
 * 
 * @author Manuel Laggner
 */
public class MovieRenamerPreviewContainer {
  final Movie           movie;
  final Path            oldPath;
  final List<MediaFile> oldMediaFiles;
  final List<MediaFile> newMediaFiles;

  Path                  newPath;
  boolean               needsRename = false;

  public MovieRenamerPreviewContainer(Movie movie) {
    this.movie = movie;
    this.oldMediaFiles = new ArrayList<>();
    this.newMediaFiles = new ArrayList<>();

    if (movie != null && !movie.getDataSource().isEmpty()) {
      this.oldPath = Paths.get(movie.getDataSource()).relativize(movie.getPathNIO());
    }
    else {
      this.oldPath = null;
    }
  }

  public Movie getMovie() {
    return movie;
  }

  public Path getOldPath() {
    return oldPath;
  }

  public Path getOldPathRelative() {
    return Paths.get(movie.getDataSource()).relativize(movie.getPathNIO());
  }

  public Path getNewPath() {
    return newPath;
  }

  public Path getNewPathRelative() {
    return Paths.get(movie.getDataSource()).relativize(newPath);
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
