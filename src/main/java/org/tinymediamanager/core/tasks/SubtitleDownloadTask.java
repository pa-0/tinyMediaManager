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
package org.tinymediamanager.core.tasks;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;

/**
 * This class handles the download and additional unpacking of a subtitle
 *
 * @author Manuel Laggner
 */
public class SubtitleDownloadTask extends DownloadTask {
  private static final Logger LOGGER = LoggerFactory.getLogger(SubtitleDownloadTask.class);

  private final MediaEntity   mediaEntity;
  private final Path          destinationFile;

  public SubtitleDownloadTask(String url, Path destinationFile, MediaEntity mediaEntity) {
    super(TmmResourceBundle.getString("subtitle.downloading"), url);
    this.mediaEntity = mediaEntity;
    this.destinationFile = destinationFile;
  }

  @Override
  protected void doInBackground() {
    if (!isFeatureEnabled()) {
      return;
    }
    super.doInBackground();
  }

  @Override
  protected Path getDestinationWoExtension() {
    return destinationFile;
  }

  @Override
  protected MediaEntity getMediaEntityToAdd() {
    return mediaEntity;
  }

  @Override
  protected void moveDownloadedFile(String fileExtension) throws IOException {
    Path destination = getDestinationWoExtension();
    if (!fileExtension.isEmpty()) {
      destination = destination.getParent().resolve(destination.getFileName() + "." + fileExtension);
    }

    MediaFile tempMediaFile = new MediaFile(tempFile);

    if (tempMediaFile.getType() == MediaFileType.SUBTITLE || Settings.getInstance().getSubtitleFileType().contains("." + fileExtension)) {
      // a direct subtitle download - we can just move it an add it to the movie
      Utils.deleteFileSafely(destination); // delete existing file
      boolean ok = Utils.moveFileSafe(tempFile, destination);
      if (ok) {
        Utils.deleteFileSafely(tempFile);

        if (mediaEntity != null) {
          MediaFile mf = new MediaFile(destination);
          mf.gatherMediaInformation();
          mf.detectStackingInformation();
          mediaEntity.removeFromMediaFiles(mf); // remove old (possibly same) file
          mediaEntity.addToMediaFiles(mf); // add file, but maybe with other MI values
          mediaEntity.saveToDb();
        }
      }
      else {
        LOGGER.warn("Download to '{}' was ok, but couldn't move to '{}'", tempFile, destination);
        setState(TaskState.FAILED);
      }
    }
    else {
      // no subtitle - maybe a zip file
      MediaFile mf = null;

      // try to decompress
      try (InputStream is = Files.newInputStream(tempFile); ZipInputStream zis = new ZipInputStream(is)) {

        // get the zipped file list entry
        ZipEntry ze = zis.getNextEntry();

        // we prefer well known subtitle file formats, but also remember .txt files
        SubtitleEntry firstSubtitle = null;
        SubtitleEntry firstTxt = null;

        while (ze != null) {
          String extension = FilenameUtils.getExtension(ze.getName()).toLowerCase(Locale.ROOT);

          // check is that is a valid file type
          if (Settings.getInstance().getSubtitleFileType().contains("." + extension) || "idx".equals(extension)) {
            firstSubtitle = new SubtitleEntry(extension, zis.readAllBytes());
          }

          if (firstTxt == null && "txt".equals(extension)) {
            firstTxt = new SubtitleEntry(extension, zis.readAllBytes());
          }

          ze = zis.getNextEntry();
        }

        if (firstSubtitle != null) {
          mf = copySubtitleFile(firstSubtitle);
        }
        else if (firstTxt != null) {
          mf = copySubtitleFile(firstTxt);
        }

        zis.closeEntry();
      }
      catch (Exception e) {
        LOGGER.debug("could not extract subtitle: {}", e.getMessage());
      }
      finally {
        Utils.deleteFileSafely(destinationFile);
      }

      if (mf != null && mf.getType() == MediaFileType.SUBTITLE) {
        if (mediaEntity != null) {
          mf.gatherMediaInformation();
          mf.detectStackingInformation();
          mediaEntity.removeFromMediaFiles(mf); // remove old (possibly same) file
          mediaEntity.addToMediaFiles(mf); // add file, but maybe with other MI values
          mediaEntity.saveToDb();
        }
      }
      else {
        setState(TaskState.FAILED);
      }
    }
  }

  private MediaFile copySubtitleFile(SubtitleEntry entry) throws IOException {
    String extension;
    if ("txt".equals(entry.extension)) {
      extension = "srt";
    }
    else {
      extension = entry.extension;
    }

    Path destination = getDestinationWoExtension();
    destination = destination.getParent().resolve(destination.getFileName() + "." + extension);
    try (FileOutputStream os = new FileOutputStream(destination.toFile())) {
      IOUtils.write(entry.buffer, os);
      return new MediaFile(destination);
    }
  }

  private static class SubtitleEntry {
    private final String extension;
    private final byte[] buffer;

    public SubtitleEntry(String extenstion, byte[] buffer) {
      this.extension = extenstion;
      this.buffer = buffer;
    }
  }
}
