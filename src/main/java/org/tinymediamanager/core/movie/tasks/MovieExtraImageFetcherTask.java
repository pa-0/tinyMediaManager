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
package org.tinymediamanager.core.movie.tasks;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.ImageUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieArtworkHelper;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.filenaming.MovieExtraFanartNaming;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;

/**
 * The class MovieExtraImageFetcherTask. To fetch extrafanarts and extrathumbs
 * 
 * @author Manuel Laggner
 */
public class MovieExtraImageFetcherTask implements Runnable {
  private static final Logger                LOGGER = LoggerFactory.getLogger(MovieExtraImageFetcherTask.class);

  private final Movie                        movie;
  private final MediaFileType                type;

  private final List<MovieExtraFanartNaming> extraFanartNamings;

  public MovieExtraImageFetcherTask(Movie movie, MediaFileType type) {
    this.movie = movie;
    this.type = type;
    this.extraFanartNamings = MovieArtworkHelper.getExtraFanartNamesForMovie(movie);
  }

  @Override
  public void run() {
    // try/catch block in the root of the thread to log crashes
    try {
      boolean ok;
      // just for single movies
      switch (type) {
        case EXTRATHUMB:
          ok = downloadExtraThumbs();
          break;

        case EXTRAFANART:
          ok = downloadExtraFanart();
          break;

        default:
          return;
      }

      // check if tmm has been shut down
      if (Thread.interrupted()) {
        return;
      }

      if (ok) {
        movie.callbackForWrittenArtwork(MediaArtworkType.ALL);
        movie.saveToDb();
      }
    }
    catch (

    Exception e) {
      LOGGER.error("Thread crashed: ", e);
      MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, movie, "message.extraimage.threadcrashed"));
    }
  }

  private boolean downloadExtraFanart() {
    List<String> fanarts = movie.getExtraFanarts();

    // do not create extrafanarts folder, if no extrafanarts are available
    if (fanarts.isEmpty()) {
      return false;
    }

    // if we do not have any valid extrafanart filename, stop there
    if (extraFanartNamings.isEmpty()) {
      return false;
    }

    // 1. clean all old extrafanarts
    for (MediaFile mediaFile : movie.getMediaFiles(MediaFileType.EXTRAFANART)) {
      Utils.deleteFileSafely(mediaFile.getFile());
      movie.removeFromMediaFiles(mediaFile);
    }

    // at the moment, we just support 1 naming scheme here! if we decide to enhance that, we will need to enhance the renamer too
    MovieExtraFanartNaming fileNaming = extraFanartNamings.get(0);

    // create an empty extrafanarts folder if the right naming has been chosen
    Path folder;
    if (fileNaming == MovieExtraFanartNaming.FOLDER_EXTRAFANART) {
      folder = movie.getPathNIO().resolve("extrafanart");
      try {
        if (!Files.exists(folder)) {
          Files.createDirectory(folder);
        }
      }
      catch (IOException e) {
        LOGGER.error("could not create extrafanarts folder: {}", e.getMessage());
        return false;
      }
    }
    else {
      folder = movie.getPathNIO();
    }

    // fetch and store images
    int i = 1;
    for (String urlAsString : fanarts) {
      try {
        String extension = Utils.getArtworkExtensionFromUrl(urlAsString);
        String filename = MovieArtworkHelper.getArtworkFilename(movie, fileNaming, extension);

        // split the filename again and attach the counter
        String basename = FilenameUtils.getBaseName(filename);
        filename = basename + i + "." + extension;

        Path destFile = ImageUtils.downloadImage(urlAsString, folder, filename);

        MediaFile mf = new MediaFile(destFile, MediaFileType.EXTRAFANART);
        mf.gatherMediaInformation();
        movie.addToMediaFiles(mf);

        // build up image cache
        ImageCache.invalidateCachedImage(destFile);
        ImageCache.cacheImageSilently(destFile);

        i++;
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.warn("problem downloading extrafanart {} - {} ", urlAsString, e.getMessage());
      }
    }

    return true;
  }

  private boolean downloadExtraThumbs() {
    if (movie.isMultiMovieDir()) {
      LOGGER.info("Movie '{}' is within a multi-movie-directory - skip downloading of {} images.", movie.getTitle(), type);
      return false;
    }

    List<String> thumbs = movie.getExtraThumbs();

    // do not create extrathumbs folder, if no extrathumbs are selected
    if (thumbs.isEmpty()) {
      return false;
    }

    Path folder = movie.getPathNIO().resolve("extrathumbs");
    try {
      if (Files.isDirectory(folder)) {
        Utils.deleteDirectorySafely(folder, movie.getDataSource());
        movie.removeAllMediaFiles(MediaFileType.EXTRATHUMB);
      }
      Files.createDirectory(folder);
    }
    catch (IOException e) {
      LOGGER.error("could not create extrathumbs folder: {}", e.getMessage());
      return false;
    }

    // fetch and store images
    int i = 1;
    for (String urlAsString : thumbs) {
      try {
        String filename = "thumb" + i + ".";
        if (MovieModuleManager.getInstance().getSettings().isImageExtraThumbsResize()) {
          filename += "jpg";
        }
        else {
          filename += FilenameUtils.getExtension(urlAsString);
        }

        Path destFile = ImageUtils.downloadImage(urlAsString, folder, filename,
            MovieModuleManager.getInstance().getSettings().isImageExtraThumbsResize(),
            MovieModuleManager.getInstance().getSettings().getImageExtraThumbsSize());

        MediaFile mf = new MediaFile(destFile, MediaFileType.EXTRATHUMB);
        mf.gatherMediaInformation();
        movie.addToMediaFiles(mf);

        // build up image cache
        ImageCache.cacheImageSilently(destFile);

        // has tmm been shut down?
        if (Thread.interrupted()) {
          return false;
        }

        i++;
      }
      catch (Exception e) {
        LOGGER.warn("problem downloading extrathumb {} - {}", urlAsString, e.getMessage());
      }
    }

    return true;
  }
}
