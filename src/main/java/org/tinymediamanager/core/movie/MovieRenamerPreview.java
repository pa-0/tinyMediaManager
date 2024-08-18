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
package org.tinymediamanager.core.movie;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;

/**
 * The class {@link MovieRenamerPreview}. To create a preview of the movie renamer (dry run)
 * 
 * @author Manuel Laggner / Myron Boyle
 */
public class MovieRenamerPreview {

  private MovieRenamerPreview() {
    throw new IllegalAccessError();
  }

  public static MovieRenamerPreviewContainer renameMovie(Movie movie) {
    MovieRenamerPreviewContainer container = new MovieRenamerPreviewContainer(movie);

    LinkedHashMap<String, MediaFile> oldFiles = new LinkedHashMap<>();
    Set<MediaFile> newFiles = new LinkedHashSet<>();

    boolean newDestIsMultiMovieDir = movie.isMultiMovieDir();
    String pattern = MovieModuleManager.getInstance().getSettings().getRenamerPathname();
    // keep MMD setting unless renamer pattern is not empty
    if (!pattern.isEmpty()) {
      // re-evaluate multiMovieDir based on renamer settings
      // folder MUST BE UNIQUE, so we need at least a T/E-Y combo or IMDBid
      // If renaming just to a fixed pattern (eg "$S"), movie will downgrade to a MMD
      newDestIsMultiMovieDir = !MovieRenamer.isFolderPatternUnique(pattern);
    }

    // temporary for generating preview!
    Movie movieClone = new Movie();
    movieClone.merge(movie);
    movieClone.setDataSource(movie.getDataSource());
    movieClone.setPath(movie.getPath());
    movieClone.setDisc(movie.isDisc());
    movieClone.setStacked(movie.isStacked());
    movieClone.setMultiMovieDir(newDestIsMultiMovieDir);
    movieClone.addToMediaFiles(movie.getMediaFiles());

    // do not rename disc FILES - add them 1:1 without renaming
    if (movieClone.isDisc()) {
      for (MediaFile mf : movieClone.getMediaFiles()) {
        oldFiles.put(mf.getFileAsPath().toString(), new MediaFile(mf)); // clone
        MediaFile ftr = MovieRenamer.generateFilename(movieClone, mf, "").get(0); // there can be only one
        newFiles.add(ftr);
      }
    }
    else {
      String oldVideoBasename = movieClone.getVideoBasenameWithoutStacking();
      String newVideoBasename = "";
      if (MovieModuleManager.getInstance().getSettings().getRenamerFilename().strip().isEmpty()) {
        // we are NOT renaming any files, so we keep the same name on renaming ;)
        newVideoBasename = movieClone.getVideoBasenameWithoutStacking();
      }
      else {
        // since we rename, generate the new basename
        MediaFile ftr = MovieRenamer.generateFilename(movieClone, movieClone.getMainVideoFile(), newVideoBasename).get(0);
        newVideoBasename = FilenameUtils.getBaseName(ftr.getFilenameWithoutStacking());
      }

      // VIDEO needs to be renamed first, since all others depend on that name!!!
      for (MediaFile mf : movieClone.getMediaFiles(MediaFileType.VIDEO)) {
        oldFiles.put(mf.getFileAsPath().toString(), new MediaFile(mf)); // clone
        MediaFile ftr = MovieRenamer.generateFilename(movieClone, mf, newVideoBasename).get(0); // there can be only one
        newFiles.add(ftr);
      }

      // all the other MFs...
      for (MediaFile mf : movieClone.getMediaFilesExceptType(MediaFileType.VIDEO)) {
        oldFiles.put(mf.getFileAsPath().toString(), new MediaFile(mf));
        newFiles.addAll(MovieRenamer.generateFilename(movieClone, mf, newVideoBasename, oldVideoBasename)); // N:M, with data from clone
      }
    }

    // movie folder needs a rename?
    Path oldMovieFolder = movieClone.getPathNIO();
    // String pattern = MovieModuleManager.getInstance().getSettings().getRenamerPathname();
    if (pattern.isEmpty()) {
      // same
      container.newPath = movieClone.getPathNIO();
    }
    else {
      try {
        container.newPath = Paths.get(movieClone.getDataSource(), MovieRenamer.createDestinationForFoldername(pattern, movieClone));
      }
      catch (Exception e) {
        // new folder name is invalid
        container.newPath = movieClone.getPathNIO();
      }
    }

    if (!oldMovieFolder.equals(container.newPath)) {
      container.needsRename = true;
      // update already the "old" files with new path, so we can simply do a contains check ;)
      for (MediaFile omf : oldFiles.values()) {
        omf.replacePathForRenamedFolder(oldMovieFolder, container.newPath);
      }
    }

    // change status of MFs, if they have been added or not
    for (MediaFile mf : newFiles) {
      if (!oldFiles.containsKey(mf.getFileAsPath().toString())) {
        container.needsRename = true;
        break;
      }
    }

    for (MediaFile mf : oldFiles.values()) {
      if (!newFiles.contains(mf)) {
        container.needsRename = true;
        break;
      }
    }

    container.oldMediaFiles.addAll(oldFiles.values());
    container.newMediaFiles.addAll(newFiles);
    return container;
  }
}
