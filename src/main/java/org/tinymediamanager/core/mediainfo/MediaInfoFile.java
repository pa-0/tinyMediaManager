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

package org.tinymediamanager.core.mediainfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.thirdparty.MediaInfo;
import org.tinymediamanager.thirdparty.MediaInfo.StreamKind;

/**
 * convenience class to ease the file<->mediainfo mapping
 * 
 * @author Myron Boyle
 *
 */
public class MediaInfoFile implements Comparable<MediaInfoFile> {

  private static final Logger                        LOGGER   = LoggerFactory.getLogger(MediaInfoFile.class);
  private Map<StreamKind, List<Map<String, String>>> snapshot;
  private int                                        duration = 0;
  private long                                       filesize = 0;
  private String                                     path     = "";
  private String                                     filename = "";
  private byte[]                                     contents = null;

  public MediaInfoFile(Path file) {
    this.path = file.toAbsolutePath().getParent().toString();
    this.filename = file.getFileName().toString();
  }

  public MediaInfoFile(MediaFile mf) {
    this.path = mf.getFileAsPath().getParent().toString();
    this.filename = mf.getFileAsPath().getFileName().toString();
    this.duration = mf.getDuration();
    this.filesize = mf.getFilesize();
  }

  public MediaInfoFile(Path file, long filesize) {
    this(file);
    this.filesize = filesize;
  }

  public void setFilename(String filename) {
    this.filename = FilenameUtils.getName(filename);
  }

  public String getFilename() {
    return filename;
  }

  public String getPath() {
    return path;
  }

  public Path getFileAsPath() {
    return Paths.get(getPath(), getFilename());
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getFileExtension() {
    return FilenameUtils.getExtension(filename).toLowerCase(Locale.ROOT);
  }

  public void setFilesize(long filesize) {
    this.filesize = filesize;
  }

  public long getFilesize() {
    return filesize;
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }

  public int getDuration() {
    return duration;
  }

  public Map<StreamKind, List<Map<String, String>>> getSnapshot() {
    if (snapshot == null) {
      snapshot = Collections.emptyMap();
    }
    return snapshot;
  }

  public void setSnapshot(Map<StreamKind, List<Map<String, String>>> snapshot) {
    this.snapshot = snapshot;
    setDuration(MediaFileHelper.parseDuration(snapshot));

    if (this.filesize == 0) {
      String siz = MediaFileHelper.getMediaInfoValue(snapshot, MediaInfo.StreamKind.General, 0, "FileSize");
      if (!siz.isEmpty()) {
        try {
          this.filesize = Long.parseLong(siz);
        }
        catch (NumberFormatException e) {
          LOGGER.debug("Could not parse filezize: {}", siz);
        }
      }
    }
  }

  /**
   * get the file contents, if set. Only used in ISO parsing - usually NULL;
   * 
   * @return
   */
  public byte[] getContents() {
    return contents;
  }

  public void setContents(byte[] contents) {
    this.contents = contents;
  }

  private Path dereferenceStreamFile(Path file) {
    try (FileReader fr = new FileReader(file.toFile()); BufferedReader br = new BufferedReader(fr)) {
      String readLine;
      while ((readLine = br.readLine()) != null) {
        readLine = readLine.strip();
        if (readLine.startsWith("#")) {
          // comment/property line
          continue;
        }
        if (readLine.isEmpty()) {
          // empty line
          continue;
        }
        break;
      }

      if (readLine == null || readLine.isBlank()) {
        // Didn't find anything...
        return file;
      }

      // referenced path is now in readLine
      if (readLine.contains("://")) {
        // This has a protocol in it, so it's not something we should go trying to open directly
        return file;
      }

      if (readLine.startsWith("/") || readLine.startsWith("\\")) {
        LOGGER.trace("Chasing absolute stream reference: {} = {}", file, readLine);
        return Paths.get(readLine);
      }

      LOGGER.trace("Chasing relative stream reference: {} = {}/{}", file, path, readLine);
      return Paths.get(path, readLine);
    }
    catch (Exception | Error e) {
      LOGGER.error("Mediainfo could not read stream file: {} - {}", file, e.getMessage());
    }

    return file;
  }

  public void gatherMediaInformation() {
    if (snapshot != null) {
      return; // already gathered
    }

    Path file = Paths.get(path, filename);
    if (filename.endsWith(".strm")) {
      // If this is a local-file stream, we should get media info from its target
      file = dereferenceStreamFile(file);
    }

    try (MediaInfo mediaInfo = new MediaInfo()) {
      if (!mediaInfo.open(file)) {
        LOGGER.error("Mediainfo could not open file: {}", file);
      }
      else {
        setSnapshot(mediaInfo.snapshot());
      }
    }
    // sometimes also an error is thrown
    catch (Exception | Error e) {
      LOGGER.error("Mediainfo could not open file: {} - {}", file, e.getMessage());
    }
  }

  /**
   * <p>
   * Uses <code>ReflectionToStringBuilder</code> to generate a <code>toString</code> for the specified object.
   * </p>
   *
   * @return the String result
   * @see ReflectionToStringBuilder#toString(Object)
   */
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    MediaInfoFile that = (MediaInfoFile) o;
    return Objects.equals(path, that.path) && Objects.equals(filename, that.filename);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, filename);
  }

  @Override
  public int compareTo(@NotNull MediaInfoFile o) {
    return getFilename().compareTo(o.getFilename());
  }
}
