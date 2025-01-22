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
package org.tinymediamanager.core.entities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.scraper.DynaEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The enum {@link MediaSource} - to represent all possible media sources
 * 
 * @author Manuel Laggner
 */
public class MediaSource extends DynaEnum<MediaSource> {
  private static final Comparator<MediaSource> COMPARATOR  = new MediaSourceComparator();
  private static final Comparator<MediaSource> COMP_LENGTH = new MediaSourceLengthComparator();

  // the well known and XBMC/Kodi compatible sources
  // tokens taken from http://en.wikipedia.org/wiki/Pirated_movie_release_types
  public static final MediaSource              UHD_BLURAY  = new MediaSource("UHD_BLURAY", 0, "UHD Blu-ray",
      "(uhd|ultrahd)[ .\\-]?(bluray|blueray|bdrip|brrip|dbrip|bd25|bd50|bdmv|blu\\-ray)");
  public static final MediaSource              BLURAY      = new MediaSource("BLURAY", 1, "Blu-ray",
      "(bluray|blueray|bdrip|brrip|dbrip|bd25|bd50|bdmv|blu\\-ray)");
  public static final MediaSource              DVD         = new MediaSource("DVD", 2, "DVD", "(dvd|video_ts|dvdrip|dvdr)");
  public static final MediaSource              HDDVD       = new MediaSource("HDDVD", 3, "HDDVD", "(hddvd|hddvdrip)");
  public static final MediaSource              TV          = new MediaSource("TV", 4, "TV",
      "(tv|hdtv|pdtv|dsr|dtb|dtt|dttv|dtv|hdtvrip|tvrip|dvbrip)");
  public static final MediaSource              VHS         = new MediaSource("VHS", 5, "VHS", "(vhs|vhsrip)");
  public static final MediaSource              LASERDISC   = new MediaSource("LASERDISC", 6, "LaserDisc", "(laserdisc|ldrip)");
  public static final MediaSource              D_VHS       = new MediaSource("D_VHS", 7, "D-VHS");

  // other sources
  public static final MediaSource              HDRIP       = new MediaSource("HDRIP", 8, "HDRip", "(hdrip)");
  public static final MediaSource              CAM         = new MediaSource("CAM", 9, "Cam", "(cam)");
  public static final MediaSource              TS          = new MediaSource("TS", 10, "Telesync", "(ts|telesync|hdts|ht\\-ts)");
  public static final MediaSource              TC          = new MediaSource("TC", 11, "Telecine", "(tc|telecine|hdtc|ht\\-tc)");
  public static final MediaSource              DVDSCR      = new MediaSource("DVDSCR", 12, "DVD Screener", "(dvdscr)");
  public static final MediaSource              R5          = new MediaSource("R5", 13, "R5", "(r5)");
  public static final MediaSource              WEBRIP      = new MediaSource("WEBRIP", 14, "Webrip", "(webrip)");
  public static final MediaSource              WEB_DL      = new MediaSource("WEB_DL", 15, "Web-DL", "(web-dl|webdl|web)");
  public static final MediaSource              STREAM      = new MediaSource("STREAM", 16, "Stream");

  // and our fallback
  public static final MediaSource              UNKNOWN     = new MediaSource("UNKNOWN", 17, "");

  private static final String                  START_TOKEN = "[\\/\\\\ _,.()\\[\\]-]";
  private static final String                  END_TOKEN   = "([\\/\\\\ _,.()\\[\\]-]|$)";

  private final String                         title;
  private final Pattern                        pattern;
  private final Pattern                        patternWoDelim;

  private MediaSource(String enumName, int ordinal, String title) {
    this(enumName, ordinal, title, "");
  }

  private MediaSource(String enumName, int ordinal, String title, String pattern) {
    super(enumName, ordinal);
    this.title = title;
    if (StringUtils.isNotBlank(pattern)) {
      this.pattern = Pattern.compile(START_TOKEN + pattern + END_TOKEN, Pattern.CASE_INSENSITIVE);
      this.patternWoDelim = Pattern.compile("^" + pattern + "$", Pattern.CASE_INSENSITIVE);
    }
    else {
      this.pattern = null;
      this.patternWoDelim = null;
    }

    addElement();
  }

  @Override
  public String toString() {
    return title;
  }

  @JsonValue
  public String getName() {
    return name();
  }

  /**
   * get all media sources
   *
   * @return an array of all media sources
   */
  public static MediaSource[] values() {
    MediaSource[] mediaSources = values(MediaSource.class);
    Arrays.sort(mediaSources, COMPARATOR);
    return mediaSources;
  }

  /**
   * Gets the right media source for the given string.
   *
   * @param name
   *          the name
   * @return the media source
   */
  @JsonCreator
  public static synchronized MediaSource getMediaSource(String name) {
    for (MediaSource mediaSource : values()) {
      // check if the "enum" name matches
      if (mediaSource.name().equals(name)) {
        return mediaSource;
      }
      // check if the printable name matches
      if (mediaSource.title.equalsIgnoreCase(name)) {
        return mediaSource;
      }
    }

    // dynamically create new one
    return new MediaSource(name, values(MediaSource.class).length, name, "");
  }

  /**
   * returns the MediaSource if found in file name
   * 
   * @param filename
   *          the filename
   * @return the matching MediaSource or UNKNOWN
   */
  public static MediaSource parseMediaSource(String filename) {
    String fn = filename.toLowerCase(Locale.ROOT);
    MediaSource[] s = MediaSource.values();
    Arrays.sort(s, MediaSource.COMP_LENGTH);

    // convert to path, and start parsing from filename upto base directory
    try {
      Path p = Paths.get(fn);
      for (MediaSource mediaSource : s) {
        Path work = p;
        while (work != null) {
          String name = work.getName(work.getNameCount() - 1).toString();
          if (mediaSource.pattern != null && mediaSource.pattern.matcher(name).find()) {
            return mediaSource;
          }
          if (mediaSource.patternWoDelim != null && mediaSource.patternWoDelim.matcher(name).find()) {
            return mediaSource;
          }
          // maybe file? try w/o extension to better match woDelims ;)
          name = FilenameUtils.getBaseName(name);
          if (name != null && mediaSource.patternWoDelim != null && mediaSource.patternWoDelim.matcher(name).find()) {
            return mediaSource;
          }
          work = work.getParent();
        }
      }
    }
    catch (Exception e) {
      // does not work? parse as string as before
      for (MediaSource mediaSource : s) {
        if (mediaSource.pattern != null && mediaSource.pattern.matcher(filename).find()) {
          return mediaSource;
        }
      }
    }

    String ext = "";
    try {
      ext = FilenameUtils.getExtension(fn);
    }
    catch (Exception e) {
      // eg : on windows (see unit test)
      int i = filename.lastIndexOf('.');
      if (i > 0) {
        ext = filename.substring(i + 1);
      }
    }
    if (ext.equals("strm")) {
      return STREAM;
    }

    return UNKNOWN;
  }

  /**
   * add a new DynaEnumEventListener. This listener will be informed if any new value has been added
   *
   * @param listener
   *          the new listener to be added
   */
  public static void addListener(DynaEnumEventListener<MediaSource> listener) {
    addListener(MediaSource.class, listener);
  }

  /**
   * remove the given DynaEnumEventListener
   *
   * @param listener
   *          the listener to be removed
   */
  public static void removeListener(DynaEnumEventListener<MediaSource> listener) {
    removeListener(MediaSource.class, listener);
  }

  /**
   * Comparator for sorting our MediaSource in a localized fashion
   */
  private static class MediaSourceComparator implements Comparator<MediaSource> {
    @Override
    public int compare(MediaSource o1, MediaSource o2) {
      // toString is localized name
      if (o1.toString() == null && o2.toString() == null) {
        return 0;
      }
      if (o1.toString() == null) {
        return 1;
      }
      if (o2.toString() == null) {
        return -1;
      }
      return o1.toString().compareTo(o2.toString());
    }
  }

  /**
   * Comparator for sorting our MediaSource from longest to shortest
   */
  private static class MediaSourceLengthComparator implements Comparator<MediaSource> {
    @Override
    public int compare(MediaSource o1, MediaSource o2) {
      if (o1 == null && o2 == null) {
        return 0;
      }
      if (o1.name() == null) {
        return 1;
      }
      if (o2.name() == null) {
        return -1;
      }
      return Integer.compare(o2.name().length(), o1.name().length());
    }
  }
}
