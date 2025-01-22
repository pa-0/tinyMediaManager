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
package org.tinymediamanager.core.tasks;

import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
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
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.util.ListUtils;

/**
 * The Class MediaEntityImageFetcherTask.
 * 
 * @author Manuel Laggner
 */
public class MediaEntityImageFetcherTask implements Runnable {
  private static final Logger    LOGGER    = LoggerFactory.getLogger(MediaEntityImageFetcherTask.class);

  private final MediaEntity      entity;
  private final String           url;
  private final MediaArtworkType type;
  private final List<String>     filenames = new ArrayList<>();

  public MediaEntityImageFetcherTask(MediaEntity entity, String url, MediaArtworkType type, List<String> filenames) {
    this.entity = entity;
    this.url = url;
    this.type = type;

    if (ListUtils.isNotEmpty(filenames)) {
      this.filenames.addAll(filenames);
    }
  }

  @Override
  public void run() {
    // check for destination file names
    if (filenames.isEmpty()) {
      return;
    }

    // check for supported artwork types
    switch (type) {
      case POSTER:
      case BACKGROUND:
      case BANNER:
      case THUMB:
      case CLEARART:
      case DISC:
      case LOGO:
      case CLEARLOGO:
      case CHARACTERART:
      case KEYART:
        break;

      default:
        return;
    }

    // remember old media files
    List<MediaFile> oldMediaFiles = entity.getMediaFiles(MediaFileType.getMediaFileType(type));
    List<MediaFile> newMediaFiles = new ArrayList<>();
    try {
      // try to download the file to the first one
      String firstFilename = filenames.get(0);
      LOGGER.debug("writing {} - {}", type, firstFilename);
      Path destFile = ImageUtils.downloadImage(url, entity.getPathNIO(), firstFilename);

      // downloading worked (no exception) - so let's remove all old artworks (except the just downloaded one)
      entity.removeAllMediaFiles(MediaFileType.getMediaFileType(type));
      for (MediaFile mediaFile : oldMediaFiles) {
        ImageCache.invalidateCachedImage(mediaFile.getFile());
        if (!mediaFile.getFile().equals(destFile)) {
          Utils.deleteFileSafely(mediaFile.getFile());
        }
      }

      // and copy it to all other variants
      newMediaFiles.add(new MediaFile(destFile, MediaFileType.getMediaFileType(type)));

      for (String filename : filenames) {
        if (firstFilename.equals(filename)) {
          // already processed
          continue;
        }

        // don't write jpeg -> write jpg
        if (FilenameUtils.getExtension(filename).equalsIgnoreCase("JPEG")) {
          filename = FilenameUtils.getBaseName(filename) + ".jpg";
        }

        LOGGER.debug("writing {} - {}", type, filename);
        Path destFile2 = entity.getPathNIO().resolve(filename);
        Utils.copyFileSafe(destFile, destFile2, true);

        newMediaFiles.add(new MediaFile(destFile2, MediaFileType.getMediaFileType(type)));
      }

      // last but not least - set all media files
      boolean first = true;
      for (MediaFile artwork : newMediaFiles) {
        // build up image cache before calling the events
        ImageCache.cacheImageSilently(artwork.getFile());

        if (first) {
          // the first one needs to be processed differently (mainly for UI eventing)
          entity.setArtwork(artwork.getFile(), MediaFileType.getMediaFileType(type));
          entity.callbackForWrittenArtwork(type);
          entity.saveToDb();
          first = false;
        }
        else {
          artwork.gatherMediaInformation();
          entity.addToMediaFiles(artwork);
        }
      }
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.error("fetch image {} - {}", url, e.getMessage());
      MessageManager.instance.pushMessage(
          new Message(MessageLevel.ERROR, "ArtworkDownload", "message.artwork.threadcrashed", new String[] { ":", e.getLocalizedMessage() }));
    }
  }
}
