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

package org.tinymediamanager.core;

import static org.tinymediamanager.scraper.util.LanguageUtils.parseLanguageFromString;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.entities.MediaStreamInfo;
import org.tinymediamanager.core.entities.MediaStreamInfo.Flags;
import org.tinymediamanager.core.mediainfo.MediaInfoFile;
import org.tinymediamanager.core.mediainfo.MediaInfoUtils;
import org.tinymediamanager.core.mediainfo.MediaInfoXMLParser;
import org.tinymediamanager.core.mediainfo.MediaInfoXmlCreator;
import org.tinymediamanager.core.tasks.MediaFileARDetectorTask;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.library.bluray.playlist.MPLSObject;
import org.tinymediamanager.library.bluray.playlist.MPLSReader;
import org.tinymediamanager.library.bluray.playlist.PlayItem;
import org.tinymediamanager.library.dvd.DvdTitle;
import org.tinymediamanager.library.dvd.IfoReader;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.ParserUtils;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.thirdparty.MediaInfo;

import com.github.stephenc.javaisotools.loopfs.iso9660.Iso9660FileEntry;
import com.github.stephenc.javaisotools.loopfs.iso9660.Iso9660FileSystem;
import com.github.stephenc.javaisotools.loopfs.udf.UDFFileEntry;
import com.github.stephenc.javaisotools.loopfs.udf.UDFFileSystem;

/**
 * the class {@link MediaFileHelper} is used to extract all unneeded logic/variables from the media file (for lower memory consumption)
 *
 * @author Manuel Laggner
 */
public class MediaFileHelper {
  private static final Logger      LOGGER             = LoggerFactory.getLogger(MediaFileHelper.class);

  public static final List<String> TRAILER_FOLDERS    = List.of("trailer", "trailers");
  // lower case
  public static final List<String> EXTRA_FOLDERS      = List.of("extra", "extras", "behind the scenes", "behindthescenes", "deleted scenes",
      "deletedscenes", "deleted", "featurette", "featurettes", "interview", "interviews", "scene", "scenes", "short", "shorts", "other", "others",
      "bloopers");
  // for structure detection
  public static final List<String> BLURAY_FOLDERS     = List.of("BDMV", "PLAYLIST", "CLIPINF", "STREAM");

  public static final List<String> SUPPORTED_ARTWORK_FILETYPES;
  public static final List<String> DEFAULT_VIDEO_FILETYPES;
  public static final List<String> DEFAULT_AUDIO_FILETYPES;
  public static final List<String> DEFAULT_SUBTITLE_FILETYPES;
  public static final List<String> BINARY_FILETYPES;

  public static final Pattern      MOVIESET_ARTWORK_PATTERN;
  public static final Pattern      POSTER_PATTERN;
  public static final Pattern      FANART_PATTERN;
  public static final Pattern      EXTRAFANART_PATTERN;
  public static final Pattern      BANNER_PATTERN;
  public static final Pattern      THUMB_PATTERN;
  public static final Pattern      SEASON_POSTER_PATTERN;
  public static final Pattern      SEASON_FANART_PATTERN;
  public static final Pattern      SEASON_BANNER_PATTERN;
  public static final Pattern      SEASON_THUMB_PATTERN;
  public static final Pattern      LOGO_PATTERN;
  public static final Pattern      CLEARLOGO_PATTERN;
  public static final Pattern      CHARACTERART_PATTERN;
  public static final Pattern      DISCART_PATTERN;
  public static final Pattern      CLEARART_PATTERN;
  public static final Pattern      KEYART_PATTERN;

  public static final String       VIDEO_FORMAT_96P   = "96p";
  public static final String       VIDEO_FORMAT_120P  = "120p";
  public static final String       VIDEO_FORMAT_144P  = "144p";
  public static final String       VIDEO_FORMAT_240P  = "240p";
  public static final String       VIDEO_FORMAT_288P  = "288p";
  public static final String       VIDEO_FORMAT_360P  = "360p";
  public static final String       VIDEO_FORMAT_480P  = "480p";
  public static final String       VIDEO_FORMAT_540P  = "540p";
  public static final String       VIDEO_FORMAT_576P  = "576p";
  public static final String       VIDEO_FORMAT_720P  = "720p";
  public static final String       VIDEO_FORMAT_1080P = "1080p";
  public static final String       VIDEO_FORMAT_1440P = "1440p";
  public static final String       VIDEO_FORMAT_2160P = "2160p";
  public static final String       VIDEO_FORMAT_4320P = "4320p";

  // meta formats
  public static final String       VIDEO_FORMAT_LD    = "LD";
  public static final String       VIDEO_FORMAT_SD    = "SD";
  public static final String       VIDEO_FORMAT_HD    = "HD";
  public static final String       VIDEO_FORMAT_UHD   = "UHD";

  // 3D / side-by-side / top-and-bottom / H=half - MVC=Multiview Video Coding-http://wiki.xbmc.org/index.php?title=3D#Video_filenames_flags
  public static final String       VIDEO_3D           = "3D";
  public static final String       VIDEO_3D_SBS       = "3D SBS";
  public static final String       VIDEO_3D_TAB       = "3D TAB";
  public static final String       VIDEO_3D_HSBS      = "3D HSBS";
  public static final String       VIDEO_3D_HTAB      = "3D HTAB";
  public static final String       VIDEO_3D_MVC       = "3D MVC";

  // disc folders
  public static final String       VIDEO_TS           = "VIDEO_TS";
  public static final String       BDMV               = "BDMV";
  public static final String       HVDVD_TS           = "HVDVD_TS";

  static {
    SUPPORTED_ARTWORK_FILETYPES = List.of("jpg", "jpeg,", "png", "tbn", "gif", "bmp", "webp");

    // .disc = video stubs
    // .evo = hd-dvd
    // .ifo = DVD; only needed for KodiRPC
    DEFAULT_VIDEO_FILETYPES = List.of(".3gp", ".asf", ".asx", ".avc", ".avi", ".bdmv", ".bin", ".bivx", ".braw", ".dat", ".divx", ".dv", ".dvr-ms",
        ".disc", ".evo", ".fli", ".flv", ".h264", ".ifo", ".img", ".iso", ".mts", ".mt2s", ".m2ts", ".m2v", ".m4v", ".mkv", ".mk3d", ".mov", ".mp4",
        ".mpeg", ".mpg", ".nrg", ".nsv", ".nuv", ".ogm", ".pva", ".qt", ".rm", ".rmvb", ".strm", ".svq3", ".ts", ".ty", ".viv", ".vob", ".vp3",
        ".wmv", ".webm", ".xvid");

    DEFAULT_AUDIO_FILETYPES = List.of(".a52", ".aa3", ".aac", ".ac3", ".adt", ".adts", ".aif", ".aiff", ".alac", ".ape", ".at3", ".atrac", ".au",
        ".dts", ".dtshd", ".flac", ".m4a", ".m4b", ".m4p", ".mid", ".midi", ".mka", ".mp3", ".mpa", ".mlp", ".oga", ".ogg", ".pcm", ".ra", ".ram",
        ".tta", ".thd", ".wav", ".wave", ".wma");

    DEFAULT_SUBTITLE_FILETYPES = List.of(".aqt", ".cvd", ".dks", ".jss", ".sub", ".sup", ".ttxt", ".mpl", ".pjs", ".psb", ".rt", ".srt", ".smi",
        ".ssf", ".ssa", ".svcd", ".usf", ".ass", ".pgs", ".vobsub", ".vtt");

    BINARY_FILETYPES = List.of("bin", "dat", "img", "nrg", "disc");

    String extensions = String.join("|", SUPPORTED_ARTWORK_FILETYPES);

    MOVIESET_ARTWORK_PATTERN = Pattern
        .compile("(?i)movieset-(poster|fanart|backdrop|banner|disc|discart|logo|clearlogo|clearart|thumb)\\.(" + extensions + ")$");
    POSTER_PATTERN = Pattern.compile("(?i)(.*-poster|poster|folder|movie|.*-cover|cover)\\.(" + extensions + ")$");
    FANART_PATTERN = Pattern.compile("(?i)(.*-fanart|.*-backdrop|.*\\.fanart|.*\\.backdrop|fanart|backdrop)\\.(" + extensions + ")$");
    EXTRAFANART_PATTERN = Pattern.compile("(?i)(.*-fanart|.*-backdrop|.*\\.fanart|.*\\.backdrop|fanart|backdrop)[0-9]+\\.(" + extensions + ")$");
    BANNER_PATTERN = Pattern.compile("(?i)(.*-banner|banner)\\.(" + extensions + ")$");
    THUMB_PATTERN = Pattern.compile("(?i)(.*-thumb|thumb|.*-landscape|landscape)[0-9]{0,2}\\.(" + extensions + ")$");
    SEASON_POSTER_PATTERN = Pattern.compile("(?i)season([0-9]{1,6}|-specials|-all)(-poster)?\\.(" + extensions + ")$");
    SEASON_FANART_PATTERN = Pattern.compile("(?i)season([0-9]{1,6}|-specials|-all)(-fanart)?\\.(" + extensions + ")$");
    SEASON_BANNER_PATTERN = Pattern.compile("(?i)season([0-9]{1,6}|-specials|-all)-banner\\.(" + extensions + ")$");
    SEASON_THUMB_PATTERN = Pattern.compile("(?i)season([0-9]{1,6}|-specials|-all)-(thumb|landscape)\\.(" + extensions + ")$");
    LOGO_PATTERN = Pattern.compile("(?i)(.*-logo|logo)\\.(" + extensions + ")$");
    CLEARLOGO_PATTERN = Pattern.compile("(?i)(.*-clearlogo|clearlogo)\\.(" + extensions + ")$");
    CHARACTERART_PATTERN = Pattern.compile("(?i)(.*-characterart|characterart)[0-9]{0,2}\\.(" + extensions + ")$");
    DISCART_PATTERN = Pattern.compile("(?i)(.*-discart|discart|.*-disc|disc)\\.(" + extensions + ")$");
    CLEARART_PATTERN = Pattern.compile("(?i)(.*-clearart|clearart)\\.(" + extensions + ")$");
    KEYART_PATTERN = Pattern.compile("(?i)(.*-keyart|keyart)\\.(" + extensions + ")$");
  }

  private MediaFileHelper() {
    throw new IllegalAccessError();
  }

  /**
   * get a list of all available video formats
   *
   * @return a list of all available video formats
   */
  public static List<String> getVideoFormats() {
    List<String> videoFormats = new ArrayList<>();

    Field[] declaredFields = MediaFileHelper.class.getDeclaredFields();
    for (Field field : declaredFields) {
      if (Modifier.isStatic(field.getModifiers()) && field.getName().startsWith("VIDEO_FORMAT_") && !field.isAnnotationPresent(Deprecated.class)) {
        try {
          videoFormats.add((String) field.get(null));
        }
        catch (Exception ignored) {
          // no need to log here
        }
      }
    }
    return videoFormats;
  }

  /**
   * converts audion channels to "dot" notation (6 -> 5.1)
   *
   * @param channels
   * @return
   */
  public static String audioChannelInDotNotation(int channels) {
    if (channels < 6) {
      return channels + ".0";
    }
    else {
      return channels - 1 + ".1";
    }
  }

  private static Path detectDatasource(Path file) {
    for (String ds : Utils.getAllDatasources()) {
      if (file.toAbsolutePath().startsWith(ds)) {
        return Paths.get(ds);
      }
    }
    // should not happen, else we would have some orphaned entries...?
    return null;
  }

  /**
   * Parses the media file type out of the given path
   *
   * @param pathToFile
   *          the path/file to parse
   * @return the detected media file type or MediaFileType.UNKNOWN
   */
  public static MediaFileType parseMediaFileType(Path pathToFile) {
    return parseMediaFileType(pathToFile, detectDatasource(pathToFile));
  }

  /**
   * Parses the media file type out of the given path
   *
   * @param pathToFile
   *          the path/file to parse
   * @param datasource
   *          to not evaluate higher than that!
   * @return the detected media file type or MediaFileType.UNKNOWN
   */
  public static MediaFileType parseMediaFileType(Path pathToFile, Path datasource) {
    String filename = pathToFile.getFileName().toString();

    String ext = FilenameUtils.getExtension(filename).toLowerCase(Locale.ROOT);
    String basename = FilenameUtils.getBaseName(filename);
    // just path w/o filename
    String foldername = FilenameUtils.getBaseName(pathToFile.getParent() == null ? "" : pathToFile.getParent().toString().toLowerCase(Locale.ROOT));

    Path relative;
    if (datasource != null) {
      // set path for evaluation not higher than datasource!
      relative = datasource.relativize(pathToFile);
    }
    else {
      relative = pathToFile;
    }

    // okay, we've got the relative path between (hopefully) the datasource and the media file itself
    // the first subfolder of this relative path cannot/must not an extra/trailer/whatsoever folder (because that would mean that the DS itself is a
    // MMD and there are not such folders allowed)
    // we just ignore that and search forward in the path for extra folders
    List<String> relativePathJunks = new ArrayList<>();
    if (relative.getNameCount() > 2) {
      relative = relative.subpath(1, relative.getNameCount() - 1); // -1 because we're not interested in the file name itself
      for (int i = 1; i <= relative.getNameCount(); i++) {
        relativePathJunks.add(relative.subpath(i - 1, i).toString().toLowerCase(Locale.ROOT));
      }
    }

    // check EXTRAS first
    if (filename.contains(".EXTRAS.") // scene file naming (need to check first! upper case!)
        || basename.matches("(?i).*[_.-]+extra[s]?\\d?$") // end with "-extra[s]"
        || basename.matches("(?i).*[-]+extra[s]?[-].*") // extra[s] just with surrounding dash (other delims problem)
        || foldername.equalsIgnoreCase("extras") // preferred folder name
        || foldername.equalsIgnoreCase("extra") // preferred folder name
        || basename.matches("(?i).*[-](behindthescenes|deleted|featurette|interview|scene|short|other|bloopers)\\d?$")
        // Plex (w/o trailer)
        || EXTRA_FOLDERS.stream().anyMatch(relativePathJunks::contains)) // extra folders
    {
      return MediaFileType.EXTRA;
    }

    if (ext.equals("nfo")) {
      return MediaFileType.NFO;
    }

    if (ext.equals("vsmeta")) {
      return MediaFileType.VSMETA;
    }

    if (basename.endsWith("-mediainfo") && "xml".equalsIgnoreCase(ext)) {
      return MediaFileType.MEDIAINFO;
    }
    if (filename.endsWith(".mediainfo.xml")) { // MI export sidecar file naming
      return MediaFileType.MEDIAINFO;
    }

    if (SUPPORTED_ARTWORK_FILETYPES.contains(ext)) {
      return parseImageType(pathToFile);
    }

    if (basename.matches("(?i).*[_.-]+(theme|soundtrack)\\d*$") || basename.matches("(?i)(theme|soundtrack)\\d*")) {
      return MediaFileType.THEME;
    }

    if (Settings.getInstance().getAudioFileType().contains("." + ext)) {
      return MediaFileType.AUDIO;
    }

    if (Settings.getInstance().getSubtitleFileType().contains("." + ext)) {
      return MediaFileType.SUBTITLE;
    }

    if (Settings.getInstance().getVideoFileType().contains("." + ext)) {
      // is this maybe a trailer?
      if (basename.matches("(?i).*[\\[\\]\\(\\)_.-]+trailer[\\[\\]\\(\\)_.-]?(\\d)*$") || basename.equalsIgnoreCase("movie-trailer")
          || basename.equalsIgnoreCase("trailer") || TRAILER_FOLDERS.contains(foldername)) {
        return MediaFileType.TRAILER;
      }

      // we have some false positives too - make a more precise check
      if (basename.matches("(?i).*[\\[\\]\\(\\)_.-]+sample[\\[\\]\\(\\)_.-]?$") || basename.equalsIgnoreCase("sample")
          || foldername.equalsIgnoreCase("sample")) { // sample folder name
        return MediaFileType.SAMPLE;
      }

      // ok, it's the main video
      return MediaFileType.VIDEO;
    }

    // is it is a DISC-like structure, handle it as a video file
    if (isDiscFolder(filename)) {
      return MediaFileType.VIDEO;
    }

    if (ext.equals("txt")) {
      return MediaFileType.TEXT;
    }

    return MediaFileType.UNKNOWN;
  }

  /**
   * Parses the image type out of the given path
   *
   * @param pathToFile
   *          the path/file to parse
   * @return the detected media file type or MediaFileType.GRAPHIC
   */
  public static MediaFileType parseImageType(Path pathToFile) {
    String filename = pathToFile.getFileName().toString();
    String foldername = pathToFile.getParent() == null ? "" : pathToFile.getParent().toString().toLowerCase(Locale.ROOT); // just path w/o filename
    String parentName = FilenameUtils.getBaseName(pathToFile.getParent() == null ? "" : pathToFile.getParent().toString());

    // movieset artwork
    Matcher matcher = MediaFileHelper.MOVIESET_ARTWORK_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.GRAPHIC;
    }

    // season(XX|-specials)-poster.*
    // seasonXX.*
    matcher = MediaFileHelper.SEASON_POSTER_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.SEASON_POSTER;
    }

    // season(XX|-specials)-fanart.*
    // seasonXX.*
    matcher = MediaFileHelper.SEASON_FANART_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.SEASON_FANART;
    }

    // season(XX|-specials)-banner.*
    matcher = MediaFileHelper.SEASON_BANNER_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.SEASON_BANNER;
    }

    // season(XX|-specials)-thumb.*
    matcher = MediaFileHelper.SEASON_THUMB_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.SEASON_THUMB;
    }

    // *-poster.* or poster.* or folder.* or movie.*
    matcher = MediaFileHelper.POSTER_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.POSTER;
    }

    // *-fanartXX.* or fanartXX.*
    matcher = MediaFileHelper.EXTRAFANART_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.EXTRAFANART;
    }

    // *-fanart.* or fanart.*
    matcher = MediaFileHelper.FANART_PATTERN.matcher(filename);
    if (matcher.matches()) {
      // decide between fanart and extrafanart
      if (foldername.endsWith("extrafanart")) {
        return MediaFileType.EXTRAFANART;
      }
      return MediaFileType.FANART;
    }

    // *-banner.* or banner.*
    matcher = MediaFileHelper.BANNER_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.BANNER;
    }

    // *-thumb.* or thumb.* or *-thumbXX.* or thumbXX.*
    matcher = MediaFileHelper.THUMB_PATTERN.matcher(filename);
    if (matcher.matches()) {
      // decide between thumb and extrathumb
      if (foldername.endsWith("extrathumbs")) {
        return MediaFileType.EXTRATHUMB;
      }
      return MediaFileType.THUMB;
    }

    // clearart.*
    matcher = MediaFileHelper.CLEARART_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.CLEARART;
    }

    // logo.* is now CLEARLOGO
    matcher = MediaFileHelper.LOGO_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.CLEARLOGO;
    }

    // clearlogo.*
    matcher = MediaFileHelper.CLEARLOGO_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.CLEARLOGO;
    }

    // discart.* / disc.*
    matcher = MediaFileHelper.DISCART_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.DISC;
    }

    // characterart.*
    matcher = MediaFileHelper.CHARACTERART_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.CHARACTERART;
    }
    if (foldername.endsWith("characterart")) {
      // own characterart folder (as seen in some skins/plugins)
      return MediaFileType.CHARACTERART;
    }

    // keyart.*
    matcher = MediaFileHelper.KEYART_PATTERN.matcher(filename);
    if (matcher.matches()) {
      return MediaFileType.KEYART;
    }

    // folder style as last chance
    if (foldername.equalsIgnoreCase("extrafanarts") || foldername.equalsIgnoreCase("extrafanart")) {
      return MediaFileType.EXTRAFANART;
    }
    if (foldername.equalsIgnoreCase("extrathumbs") || foldername.equalsIgnoreCase("extrathumb")) {
      return MediaFileType.EXTRATHUMB;
    }

    // same name as the parent -> poster (Zidoo)
    if (parentName.equals(FileNameUtils.getBaseName(filename))) {
      return MediaFileType.POSTER;
    }

    return MediaFileType.GRAPHIC;
  }

  /**
   * checks, if MediaFile is in its own dedicated "extras" folder, and not just an extras file...
   *
   * @param mf
   *          the mediafile to check
   * @param entity
   *          needed for relative path
   * @return treu/false
   */
  public static boolean isExtraInDedicatedFolder(MediaFile mf, MediaEntity entity) {
    if (mf.getType() != MediaFileType.EXTRA) {
      return false;
    }
    Path rel = entity.getPathNIO().relativize(mf.getFileAsPath());
    rel = rel.getParent();
    if (rel == null) {
      return false;
    }

    Iterator<Path> elements = rel.iterator();
    while (elements.hasNext()) {
      Path p = elements.next();
      if (MediaFileHelper.EXTRA_FOLDERS.contains(p.getFileName().toString().toLowerCase(Locale.ROOT))) {
        return true;
      }
    }

    return false;
  }

  /**
   * gets the "common" video format for the given {@link MediaFile}
   *
   * @param mediaFile
   *          the media file to parse
   * @return 1080p 720p 480p... or SD if too small
   */
  public static String getVideoFormat(MediaFile mediaFile) {
    int w = mediaFile.getVideoWidth();
    int h = mediaFile.getVideoHeight();

    // use XBMC implementation https://github.com/xbmc/xbmc/blob/master/xbmc/utils/StreamDetails.cpp#L559
    if (w == 0 || h == 0) {
      return "";
    }
    // https://en.wikipedia.org/wiki/Low-definition_television
    else if (w <= blur(128) && h <= blur(96)) { // MMS-Small 96p 128×96 4:3
      return VIDEO_FORMAT_96P;
    }
    else if (w <= blur(160) && h <= blur(120)) { // QQVGA 120p 160×120 4:3
      return VIDEO_FORMAT_120P;
    }
    else if (w <= blur(176) && h <= blur(144)) { // QCIF Webcam 144p 176×144 11:9
      return VIDEO_FORMAT_144P;
    }
    else if (w <= blur(256) && h <= blur(144)) { // YouTube 144p 144p 256×144 16:9
      return VIDEO_FORMAT_144P;
    }
    else if (w <= blur(320) && h <= blur(240)) { // NTSC square pixel 240p 320×240 4:3
      return VIDEO_FORMAT_240P;
    }
    else if (w <= blur(352) && h <= blur(240)) { // SIF (525) 240p 352×240 4:3
      return VIDEO_FORMAT_240P;
    }
    else if (w <= blur(426) && h <= blur(240)) { // NTSC widescreen 240p 426×240 16:9
      return VIDEO_FORMAT_240P;
    }
    else if (w <= blur(480) && h <= blur(272)) { // PSP 288p 480×272 30:17
      return VIDEO_FORMAT_288P;
    }
    else if (w <= blur(480) && h <= blur(360)) { // 360p 360p 480×360 4:3
      return VIDEO_FORMAT_360P;
    }
    else if (w <= blur(640) && h <= blur(360)) { // Wide 360p 360p 640×360 16:9
      return VIDEO_FORMAT_360P;
    }
    // https://en.wikipedia.org/wiki/480p
    else if (w <= blur(640) && h <= blur(480)) { // 480p 640×480 4:3
      return VIDEO_FORMAT_480P;
    }
    else if (w <= blur(720) && h <= blur(480)) { // Rec. 601 720×480 3:2
      return VIDEO_FORMAT_480P;
    }
    else if (w <= blur(800) && h <= blur(480)) { // Rec. 601 plus a quarter 800×480 5:3
      return VIDEO_FORMAT_480P;
    }
    else if (w <= blur(853) && h <= blur(480)) { // Wide 480p 853.33×480 16:9 (unscaled)
      return VIDEO_FORMAT_480P;
    }
    else if (w <= blur(776) && h <= blur(592)) {
      // 720x576 (PAL) (handbrake sometimes encode it to a max of 776 x 592)
      return VIDEO_FORMAT_576P;
    }
    else if (w <= blur(1024) && h <= blur(576)) { // Wide 576p 1024×576 16:9
      return VIDEO_FORMAT_576P;
    }
    else if (w <= blur(960) && h <= blur(544)) {
      // 960x540 (sometimes 544 which is multiple of 16)
      return VIDEO_FORMAT_540P;
    }
    else if (w <= blur(1280) && h <= blur(720)) { // 720p Widescreen 16:9
      return VIDEO_FORMAT_720P;
    }
    else if (w <= blur(960) && h <= blur(720)) { // 720p Widescreen 4:3
      return VIDEO_FORMAT_720P;
    }
    else if (w <= blur(1080) && h <= blur(720)) { // 720p Rec. 601 3:2
      return VIDEO_FORMAT_720P;
    }
    else if (w <= blur(1920) && h <= blur(1080)) { // 1080p HD Widescreen 16:9
      return VIDEO_FORMAT_1080P;
    }
    else if (w <= blur(1440) && h <= blur(1080)) { // 1080p SD 4:3
      return VIDEO_FORMAT_1080P;
    }
    else if (w <= blur(1620) && h <= blur(1080)) { // 1080p Rec. 601 3:2
      return VIDEO_FORMAT_1080P;
    }
    else if (w <= blur(1920) && h <= blur(1440)) { // 1440p HD Widescreen 4:3
      return VIDEO_FORMAT_1440P;
    }
    else if (w <= blur(2160) && h <= blur(1440)) { // 1440p Rec. 601 3:2
      return VIDEO_FORMAT_1440P;
    }
    else if (w <= blur(2560) && h <= blur(1440)) { // 1440p HD Widescreen 16:9
      return VIDEO_FORMAT_1440P;
    }
    else if (w <= blur(3840) && h <= blur(2160)) { // 4K Ultra-high-definition television
      return VIDEO_FORMAT_2160P;
    }
    else if (w <= blur(3840) && h <= blur(1600)) { // 4K Ultra-wide-television
      return VIDEO_FORMAT_2160P;
    }
    else if (w <= blur(4096) && h <= blur(2160)) { // DCI 4K (native resolution)
      return VIDEO_FORMAT_2160P;
    }
    else if (w <= blur(4096) && h <= blur(1716)) { // DCI 4K (CinemaScope cropped)
      return VIDEO_FORMAT_2160P;
    }
    else if (w <= blur(3996) && h <= blur(2160)) { // DCI 4K (flat cropped)
      return VIDEO_FORMAT_2160P;
    }

    return VIDEO_FORMAT_4320P;
  }

  /**
   * get the video definition category for the given {@link MediaFile}<br/>
   * LD (<=360 lines), SD (>360 and <720 lines) or HD (720+ lines).
   *
   * @param mediaFile
   *          the media file
   * @return LD, SD or HD
   */
  public static String getVideoDefinitionCategory(MediaFile mediaFile) {
    String vf = getVideoFormat(mediaFile);
    if (vf.isEmpty()) {
      return "";
    }
    switch (vf) {
      case VIDEO_FORMAT_96P:
      case VIDEO_FORMAT_120P:
      case VIDEO_FORMAT_144P:
      case VIDEO_FORMAT_240P:
      case VIDEO_FORMAT_288P:
      case VIDEO_FORMAT_360P:
        return VIDEO_FORMAT_LD;

      case VIDEO_FORMAT_480P:
      case VIDEO_FORMAT_576P:
      case VIDEO_FORMAT_540P:
        return VIDEO_FORMAT_SD;

      case VIDEO_FORMAT_720P:
      case VIDEO_FORMAT_1080P:
      case VIDEO_FORMAT_1440P:
        return VIDEO_FORMAT_HD;

      case VIDEO_FORMAT_2160P:
      case VIDEO_FORMAT_4320P:
        return VIDEO_FORMAT_UHD;

      default:
        return "";
    }
  }

  /**
   * is that the given {@link MediaFile} a video in format LD?
   *
   * @param mediaFile
   *          the media file
   * @return true/false
   */
  public static boolean isVideoDefinitionLD(MediaFile mediaFile) {
    if (!mediaFile.isVideo()) {
      return false;
    }

    return VIDEO_FORMAT_LD.equals(getVideoDefinitionCategory(mediaFile));
  }

  /**
   * is that the given {@link MediaFile} a video in format SD?
   *
   * @param mediaFile
   *          the media file
   * @return true/false
   */
  public static boolean isVideoDefinitionSD(MediaFile mediaFile) {
    if (!mediaFile.isVideo()) {
      return false;
    }

    return VIDEO_FORMAT_SD.equals(getVideoDefinitionCategory(mediaFile));
  }

  /**
   * is that the given {@link MediaFile} a video in format HD?
   *
   * @param mediaFile
   *          the media file
   * @return true/false
   */
  public static boolean isVideoDefinitionHD(MediaFile mediaFile) {
    if (!mediaFile.isVideo()) {
      return false;
    }

    return VIDEO_FORMAT_HD.equals(getVideoDefinitionCategory(mediaFile));
  }

  /**
   * is that the given {@link MediaFile} a video in format UHD?
   *
   * @param mediaFile
   *          the media file
   * @return true/false
   */
  public static boolean isVideoDefinitionUHD(MediaFile mediaFile) {
    if (!mediaFile.isVideo()) {
      return false;
    }

    return VIDEO_FORMAT_UHD.equals(getVideoDefinitionCategory(mediaFile));
  }

  /**
   * add 1% to the given value
   *
   * @param value
   *          the value to blur
   * @return the blurred value
   */
  private static int blur(int value) {
    return value + (value / 100);
  }

  /**
   * gather basic file information like file size, creation date and last modified date
   *
   * @param mediaFile
   *          the {@link MediaFile} to gather the information for
   * @return true if the filesize changed, false otherwise
   */
  public static boolean gatherFileInformation(MediaFile mediaFile) {
    boolean dirty = false;

    // get basic infos; file size, creation date and last modified
    try {
      BasicFileAttributes view = Files.readAttributes(mediaFile.getFileAsPath(), BasicFileAttributes.class);
      if (view.creationTime().toMillis() > 100000) {
        Date creDat = new Date(view.creationTime().toMillis());
        mediaFile.setDateCreated(creDat);
      }
      if (view.lastModifiedTime().toMillis() > 100000) {
        Date modDat = new Date(view.lastModifiedTime().toMillis());
        mediaFile.setDateLastModified(modDat);
        mediaFile.setFiledate(view.lastModifiedTime().toMillis());
      }

      long size = view.size();
      if (view.isDirectory()) {
        size = Utils.getDirectorySizeOfDiscFiles(mediaFile.getFile());
      }
      if (size > 0 && mediaFile.getFilesize() > 0 && size != mediaFile.getFilesize()) {
        dirty = true;
      }
      mediaFile.setFilesize(size);
    }
    catch (Exception e) {
      LOGGER.debug("could not get file information (size/date): {}", e.getMessage());
    }

    return dirty;
  }

  /**
   * Gathers the media information for the given {@link MediaFile} via libmediainfo
   *
   * @param mediaFile
   *          the media file
   * @param force
   *          forces the execution, will not stop on already imported files
   */
  public static void gatherMediaInformation(MediaFile mediaFile, boolean force) {
    String extension = mediaFile.getExtension();

    if (StringUtils.isNotBlank(extension)) {
      extension = extension.toLowerCase(Locale.ROOT);
    }

    // get basic infos; file size, creation date and last modified
    boolean fileSizeChanged = gatherFileInformation(mediaFile);

    // check for supported filetype
    if (!mediaFile.isValidMediainfoFormat()) {
      // okay, we have no valid MI file, be sure it will not be triggered any more
      if (StringUtils.isBlank(mediaFile.getContainerFormat())) {
        mediaFile.setContainerFormat(extension);
      }
      return;
    }

    // mediainfo already gathered
    if (!fileSizeChanged && !force && !mediaFile.getContainerFormat().isEmpty()) {
      return;
    }

    // do not work further on 0 byte files
    if (mediaFile.getFilesize() == 0 && StringUtils.isBlank(mediaFile.getContainerFormat())) {
      LOGGER.debug("0 Byte file detected: {}", mediaFile.getFilename());
      // set container format to do not trigger it again
      mediaFile.setContainerFormat(extension);
      // return; // let em run... we get some info out of filenames...
    }

    // do not work further on non media files files
    switch (mediaFile.getType()) {
      case NFO:
      case TEXT:
      case MEDIAINFO:
      case VSMETA:
      case UNKNOWN:
      case DOUBLE_EXT:
        // set container format to do not trigger it again
        mediaFile.setContainerFormat(mediaFile.getExtension());
        return;

      default:
        break;
    }

    // get media info
    LOGGER.debug("start MediaInfo for {}", mediaFile.getFileAsPath());

    List<MediaInfoFile> mediaInfoFiles = new ArrayList<>();

    // read mediainfo.xml only if the file size has not been changed,
    // and ONLY for VIDEO files (else external audio files would ALSO read XML)
    if (!fileSizeChanged && mediaFile.getType() == MediaFileType.VIDEO) {
      try {
        // just parse via XML
        Path xmlFile = Paths.get(mediaFile.getPath(), FilenameUtils.getBaseName(mediaFile.getFilename()) + "-mediainfo.xml");
        mediaInfoFiles.addAll(detectRelevantFiles(parseMediaInfoXml(xmlFile)));

        if (!mediaInfoFiles.isEmpty()) {
          LOGGER.trace("mediainfo.xml found - '{}'", xmlFile.getFileName());
          parseMediainfoSnapshot(mediaFile, mediaInfoFiles); // FIXME: only the first!
          // sanity check of invalid XMLs
          if (mediaInfoFiles.get(0).getSnapshot() == null || mediaInfoFiles.get(0).getSnapshot().isEmpty()) {
            LOGGER.warn("Reading MediaInfoXML did not return something useful...");
            mediaInfoFiles.clear();
          }
        }
      }
      catch (Exception e) {
        mediaInfoFiles.clear();
        // reading mediainfo failed; re-read without XML
        LOGGER.debug("could not read -mediainfo.xml data - maybe a broken XML? {}", e.getMessage());
      }
    }

    // read mediainfo directly
    if (mediaInfoFiles.isEmpty()) {
      if (mediaFile.isISO()) {
        mediaInfoFiles = getMediaInfoSnapshotFromISO(mediaFile);
      }
      else {
        mediaInfoFiles = getMediaInfoFromSingleFile(mediaFile);
      }

      if (!mediaInfoFiles.isEmpty()) {
        parseMediainfoSnapshot(mediaFile, mediaInfoFiles);
      }
    }

    LOGGER.debug("end MediaInfo for {}", mediaFile.getFileAsPath());
  }

  /**
   * if you have an MI snapshot prepared, parse it
   *
   * @param mediaFile
   *          the {@link MediaFile} for which the snapshot should be parsed
   * @param mediaInfoFiles
   *          a {@link List} of all files to be considered for mediainfo
   */
  private static void parseMediainfoSnapshot(MediaFile mediaFile, List<MediaInfoFile> mediaInfoFiles) {
    if (mediaInfoFiles.isEmpty()) {
      LOGGER.debug("no mediainfo data provided");
      return;
    }

    // special handling for "disc" files
    // but only if we have more that one MIF detected...
    if (mediaInfoFiles.size() > 1 && (mediaFile.isISO() || mediaFile.isDiscFile())) {
      if (isHDDVDStructure(mediaInfoFiles)) {
        gatherMediaInformationFromHdDvdFile(mediaFile, mediaInfoFiles);
      }
      else if (isDVDStructure(mediaInfoFiles)) {
        gatherMediaInformationFromDvdFile(mediaFile, mediaInfoFiles);
      }
      else if (isBlurayStructure(mediaInfoFiles)) {
        gatherMediaInformationFromBluRayFile(mediaFile, mediaInfoFiles);
      }
      else {
        // no file informations - just handle it as a normal file
        gatherMediaInformationFromFile(mediaFile, mediaInfoFiles);
      }
    }
    else {
      gatherMediaInformationFromFile(mediaFile, mediaInfoFiles);
    }
  }

  /**
   * is the given filename/foldername from a DVD/BR/HD-DVD "disc file/folder"?
   *
   * @param filename
   *          the filename to check
   * @param path
   *          the path to check
   * @return true/false
   */
  public static boolean isDiscFile(String filename, String path) {
    return isDVDFile(filename) || isBlurayFile(filename) || isHDDVDFile(filename);
  }

  /**
   * does this path end with a disc folder; so the file is within?
   *
   * @param folder
   * @return
   */
  public static boolean isDiscFolder(String folder) {
    return folder.endsWith(BDMV) || folder.endsWith(VIDEO_TS) || folder.endsWith(HVDVD_TS);
  }

  /**
   * is the given filename/foldername from a DVD "disc file/folder"? (video_ts, vts...)
   *
   * @param filename
   *          the filename to check
   * @return true/false
   */
  public static boolean isDVDFile(String filename) {
    if (StringUtils.isBlank(filename)) {
      return false;
    }

    // Folder MF only!
    if (VIDEO_TS.equalsIgnoreCase(filename)) {
      return true;
    }

    return filename.toLowerCase(Locale.ROOT).matches("(video_ts|vts_\\d\\d_\\d)\\.(vob|bup|ifo)");
  }

  /**
   * is this a DVD "disc file/folder"? (video_ts, vts...)
   *
   * @return true/false
   */
  private static boolean isDVDStructure(List<MediaInfoFile> files) {
    for (MediaInfoFile mif : files) {

      // structure MUST be in some folder, not only loose DVD files...
      if (mif.getPath().endsWith(VIDEO_TS)) {
        return true;
      }
    }
    return false;
  }

  /**
   * is the given filename/foldername from a HD-DVD "disc file/folder"? (hvdvd_ts, hv...)
   *
   * @param filename
   *          the filename to check
   * @return true/false
   */
  public static boolean isHDDVDFile(String filename) {
    if (StringUtils.isBlank(filename)) {
      return false;
    }

    // Folder MF only!
    if (HVDVD_TS.equalsIgnoreCase(filename)) {
      return true;
    }

    // https://pt.slideshare.net/mvasu22/introduction-tohd-dvdsystemmodel?next_slideshow=true
    return filename.toLowerCase(Locale.ROOT).matches("(hv\\d{3}[imt]\\d{2}|hv[as]\\d{5}|title\\d{3})\\.(evo|bup|ifo|vti|map)");
  }

  /**
   * HD-DVD "disc file/folder"? (video_ts, vts...)
   *
   * @return true/false
   */
  private static boolean isHDDVDStructure(List<MediaInfoFile> files) {
    for (MediaInfoFile mif : files) {

      // structure MUST be in some folder, not only loose DVD files...
      if (mif.getPath().endsWith(HVDVD_TS)) {
        return true;
      }
    }
    return false;
  }

  /**
   * is the given filename/foldername from Bluray "disc file/folder"? (index, movieobject, bdmv, ...) for movierenamer
   *
   * @param filename
   *          the filename to check
   * @return true/false
   */
  public static boolean isBlurayFile(String filename) {
    if (StringUtils.isBlank(filename)) {
      return false;
    }

    // Folder MF only!
    if (BDMV.equalsIgnoreCase(filename)) {
      return true;
    }

    return filename.toLowerCase(Locale.ROOT).matches("(index\\.bdmv|movieobject\\.bdmv|\\d{5}\\.m2ts|\\d{5}\\.clpi|\\d{5}\\.mpls)");
  }

  /**
   * checks whether the given file is a main disc identifier file
   *
   * @param filename
   *          the filename to check
   * @return true/false
   */
  public static boolean isMainDiscIdentifierFile(String filename) {
    if (filename.equalsIgnoreCase("video_ts.ifo") || filename.equalsIgnoreCase("index.bdmv") || filename.equalsIgnoreCase("hv000i01.ifo")) {
      return true;
    }
    return false;
  }

  /**
   * Bluray "disc file/folder"? (video_ts, vts...)
   *
   * @return true/false
   */
  private static boolean isBlurayStructure(List<MediaInfoFile> files) {
    for (MediaInfoFile mif : files) {
      Path p = mif.getFileAsPath();
      // MI xml for Blurays might not always have a correct filename set
      if (p.getParent() == null || p.getParent().equals(p.getRoot())) {
        continue;
      }
      String filename = mif.getFileAsPath().getFileName().toString();
      String foldername = mif.getFileAsPath().getParent().getFileName().toString().toUpperCase(Locale.ROOT);
      // structure MUST be in some folder, not only loose m2ts files...
      if (BLURAY_FOLDERS.contains(foldername) && isBlurayFile(filename)) {
        return true;
      }
    }
    return false;
  }

  /**
   * get the libmediainfo snapshot of all data for the given {@link MediaFile}
   *
   * @param mediaFile
   *          the media file
   * @return a {@link List} of all associated files along with libmediainfo data
   */
  private static synchronized List<MediaInfoFile> getMediaInfoFromSingleFile(MediaFile mediaFile) {
    if (!MediaInfoUtils.useMediaInfo()) {
      return Collections.emptyList();
    }

    // open mediaInfo directly on file/folder
    List<MediaInfoFile> mediaInfoFiles = new ArrayList<>();

    if (Files.isDirectory(mediaFile.getFileAsPath())) {
      for (Path path : Utils.listFilesRecursive(mediaFile.getFileAsPath())) {
        try {
          mediaInfoFiles.add(new MediaInfoFile(path, Files.size(path)));
        }
        catch (Exception e) {
          LOGGER.debug("could not parse filesize of {} - {}", path, e.getMessage());
        }
      }
      mediaInfoFiles = detectRelevantFiles(mediaInfoFiles);
    }
    else {
      mediaInfoFiles.add(new MediaInfoFile(mediaFile.getFile()));
    }

    for (MediaInfoFile mif : mediaInfoFiles) {
      mif.gatherMediaInformation();
    }

    // at this point there is no valid XML file - write a new one if configured
    if (Settings.getInstance().isWriteMediaInfoXml() && mediaFile.getType() == MediaFileType.VIDEO) {
      try {
        MediaInfoXmlCreator mediaInfoXmlCreator = new MediaInfoXmlCreator(mediaFile, mediaInfoFiles);
        mediaInfoXmlCreator.write();
      }
      catch (Exception e) {
        LOGGER.debug("could not write mediainfo xml - {}", e.getMessage());
      }
    }

    return mediaInfoFiles;
  }

  private static List<MediaInfoFile> parseMediaInfoXml(Path xmlFile) {
    List<MediaInfoFile> miFiles = null;

    if (Files.exists(xmlFile)) {
      try {
        LOGGER.trace("try to parse mediainfo xml - {}", xmlFile);
        miFiles = new MediaInfoXMLParser(xmlFile).parseXML();
      }
      catch (Exception e) {
        LOGGER.debug("unable to parse mediainfo xml - {} - {}", xmlFile, e.getMessage());
      }
    }

    if (miFiles == null) {
      return Collections.emptyList();
    }

    return miFiles;
  }

  /**
   * get the libmediainfo snapshot of all data for the given {@link MediaFile} is it is an ISO file
   *
   * @param mediaFile
   *          the media file
   * @return a {@link List} of all associated files along with libmediainfo data
   */
  private static synchronized List<MediaInfoFile> getMediaInfoSnapshotFromISO(MediaFile mediaFile) {
    List<MediaInfoFile> miFiles;

    if (!MediaInfoUtils.useMediaInfo()) {
      return Collections.emptyList();
    }

    // try parse ISO as DVD directly...
    miFiles = parseIso9660(mediaFile);

    // still empty? try parse ISO as UDF directly, taking the biggest file (for now)...
    if (miFiles.isEmpty()) {
      miFiles = parseIsoUdf(mediaFile);
    }

    // at this point there is no valid XML file - write a new one if configured
    if (Settings.getInstance().isWriteMediaInfoXml()) {
      try {
        MediaInfoXmlCreator mediaInfoXmlCreator = new MediaInfoXmlCreator(mediaFile, miFiles);
        mediaInfoXmlCreator.write();
      }
      catch (Exception e) {
        LOGGER.debug("could not write mediainfo xml - {}", e.getMessage());
      }
    }

    return miFiles;
  }

  static List<MediaInfoFile> parseIso9660(MediaFile mediaFile) {
    List<MediaInfoFile> miFiles = new ArrayList<>();

    int bufferSize = 64 * 1024;
    try (Iso9660FileSystem image = new Iso9660FileSystem(mediaFile.getFileAsPath().toFile(), true)) {
      LOGGER.trace("ISO: Open");

      // find all relevant files to parse at the beginning to avoid unnecessary IO
      List<MediaInfoFile> allFiles = new ArrayList<>();
      List<Iso9660FileEntry> fileEntries = new ArrayList<>();
      for (Iso9660FileEntry entry : image) {
        if (entry.isDirectory()) {
          continue;
        }
        fileEntries.add(entry);
        MediaInfoFile mif = new MediaInfoFile(Paths.get(entry.getPath()), entry.getSize());
        // read IFO file directly, to use it later in detectRelevantFiles()
        if (entry.getName().toUpperCase(Locale.ROOT).endsWith(".IFO")) {
          byte[] contents = new byte[(int) entry.getSize()];
          image.readBytes(entry, 0L, contents, 0, (int) entry.getSize());
          mif.setContents(contents);
        }

        allFiles.add(mif);
      }

      List<MediaInfoFile> relevantFiles = detectRelevantFiles(allFiles);

      for (Iso9660FileEntry entry : fileEntries) {
        MediaInfoFile mif = new MediaInfoFile(Paths.get(entry.getPath()), entry.getSize());
        if (!relevantFiles.contains(mif)) {
          continue;
        }

        LOGGER.trace("ISO: got entry {}, size : {}", entry.getName(), entry.getSize());

        MediaFile mf = new MediaFile(Paths.get(mediaFile.getFileAsPath().toString(), entry.getPath())); // set ISO as MF path
        if (mf.isDiscFile()) { // count all known DVD/BR/HDDVD files!

          try (MediaInfo fileMI = new MediaInfo()) {
            byte[] fromBuffer = new byte[bufferSize];
            int fromBufferSize; // The size of the read file buffer
            long fileSize = entry.getSize();

            // Preparing to fill MediaInfo with a buffer
            fileMI.openBufferInit(fileSize, 0);

            long pos = 0L;
            // The parsing loop
            do {
              // limit read to maxBuffer, or to end of file size (cannot determine file end in stream!!)
              long toread = pos + bufferSize > fileSize ? fileSize - pos : bufferSize;

              // Reading data somewhere, do what you want for this.
              fromBufferSize = image.readBytes(entry, pos, fromBuffer, 0, (int) toread);
              if (fromBufferSize > 0) {
                pos += fromBufferSize; // add bytes read to file position

                // Sending the buffer to MediaInfo
                int result = fileMI.openBufferContinue(fromBuffer, fromBufferSize);
                if ((result & 8) == 8) { // Status.Finalized
                  break;
                }

                // Testing if MediaInfo request to go elsewhere
                if (fileMI.openBufferContinueGoToGet() != -1) {
                  pos = fileMI.openBufferContinueGoToGet();
                  LOGGER.trace("ISO: Seek to {}", pos);
                  fileMI.openBufferInit(fileSize, pos); // Informing MediaInfo we have seek
                }
              }
            } while (fromBufferSize > 0);

            // Finalizing
            LOGGER.trace("ISO: finalize entry");
            fileMI.openBufferFinalize(); // This is the end of the stream, MediaInfo must finish some work

            mif.setSnapshot(fileMI.snapshot());
            miFiles.add(mif);
          }
          // sometimes also an error is thrown
          catch (Exception | Error e) {
            LOGGER.debug("Mediainfo could not open file STREAM for file {}", entry.getName(), e);
          }
        } // end VIDEO
      } // end entry
    }
    catch (Exception e) {
      LOGGER.debug("Mediainfo could not open as ISO9660 - {}", e.getMessage());
    }

    return miFiles;
  }

  static List<MediaInfoFile> parseIsoUdf(MediaFile mediaFile) {
    List<MediaInfoFile> miFiles = new ArrayList<>();

    try (UDFFileSystem image = new UDFFileSystem(mediaFile.getFileAsPath().toFile(), true)) {
      int bufferSize = 64 * 1024;

      // find all relevant files to parse at the beginning to avoid unnecessary IO
      List<MediaInfoFile> allFiles = new ArrayList<>();
      List<UDFFileEntry> fileEntries = new ArrayList<>();
      for (UDFFileEntry entry : image) {
        if (entry.isDirectory()) {
          continue;
        }
        // ignore BACKUP folders 2 levels deep
        Path folder = Paths.get(entry.getPath()).getParent();
        if (folder != null && folder.getFileName() != null && folder.getFileName().toString().equalsIgnoreCase("BACKUP")) {
          continue;
        }
        if (folder.getParent() != null && folder.getParent().getFileName() != null
            && folder.getParent().getFileName().toString().equalsIgnoreCase("BACKUP")) {
          continue;
        }

        fileEntries.add(entry);
        MediaInfoFile mif = new MediaInfoFile(Paths.get(entry.getPath()), entry.getSize());

        // read playlist file directly, to use it later in detectRelevantFiles()
        if (entry.getName().toUpperCase(Locale.ROOT).endsWith(".MPLS")) {
          byte[] contents = new byte[(int) entry.getSize()];
          image.readFileContent(entry, 0L, contents, 0, (int) entry.getSize());
          mif.setContents(contents);
        }

        allFiles.add(mif);
      }

      List<MediaInfoFile> relevantFiles = detectRelevantFiles(allFiles);

      for (UDFFileEntry entry : fileEntries) {
        MediaInfoFile mif = new MediaInfoFile(Paths.get(entry.getPath()), entry.getSize());
        if (!relevantFiles.contains(mif)) {
          continue;
        }

        LOGGER.trace("ISO: got entry {}, size : {}", entry.getPath(), entry.getSize());

        try (MediaInfo fileMI = new MediaInfo()) {
          byte[] fromBuffer = new byte[bufferSize];
          int fromBufferSize; // The size of the read file buffer
          long fileSize = entry.getSize();

          // Preparing to fill MediaInfo with a buffer
          fileMI.openBufferInit(fileSize, 0);

          long pos = 0L;
          // The parsing loop
          do {
            // limit read to maxBuffer, or to end of file size (cannot determine file end in stream!!)
            long toread = pos + bufferSize > fileSize ? fileSize - pos : bufferSize;

            // Reading data somewhere, do what you want for this.
            fromBufferSize = image.readFileContent(entry, pos, fromBuffer, 0, (int) toread);
            if (fromBufferSize > 0) {
              pos += fromBufferSize; // add bytes read to file position

              // Sending the buffer to MediaInfo
              int result = fileMI.openBufferContinue(fromBuffer, fromBufferSize);
              if ((result & 8) == 8) { // Status.Finalized
                break;
              }

              // Testing if MediaInfo request to go elsewhere
              if (fileMI.openBufferContinueGoToGet() != -1) {
                pos = fileMI.openBufferContinueGoToGet();
                LOGGER.trace("ISO: Seek to {}", pos);
                fileMI.openBufferInit(fileSize, pos); // Informing MediaInfo we have seek
              }
            }
          } while (fromBufferSize > 0);

          // Finalizing
          LOGGER.trace("ISO: finalize entry");
          fileMI.openBufferFinalize(); // This is the end of the stream, MediaInfo must finish some work

          mif.setSnapshot(fileMI.snapshot());
          miFiles.add(mif);
        }
        // sometimes also an error is thrown
        catch (Exception | Error e) {
          LOGGER.debug("Mediainfo could not open file UDF for file {} - {}", entry.getPath(), e);
        }
      }
    }
    catch (Exception e) {
      LOGGER.debug("Mediainfo could not open as UDF - {}", e.getMessage());
    }

    return miFiles;
  }

  /**
   * uses a list of all 'relevant' files, and reduces them to only contain the 'needed' ones<br>
   * Like DVD IFO and associated VOBs, Bluray MPLS, CLPINF, SSIF, M2TS and other files.<br>
   * Everything we want to analyze somewhere should be in here,<br>
   * <br>
   * <b>YOU NEED TO FILTER FURTHER, WHAT FILES ARE INTERESTING FOR YOU!!!</b>
   *
   * @param mediaInfoFiles
   * @return
   */
  static List<MediaInfoFile> detectRelevantFiles(List<MediaInfoFile> mediaInfoFiles) {
    if (mediaInfoFiles.isEmpty()) {
      return Collections.emptyList();
    }

    if (isHDDVDStructure(mediaInfoFiles)) {
      return detectRelevantHdDvdFiles(mediaInfoFiles);
    }
    else if (isDVDStructure(mediaInfoFiles)) {
      return detectRelevantDvdFiles(mediaInfoFiles);
    }
    else if (isBlurayStructure(mediaInfoFiles)) {
      return detectRelevantBlurayFiles(mediaInfoFiles);
    }

    return mediaInfoFiles;
  }

  /**
   * Returns the mediafile, or, in case of a disc structure, a list of all 'relevant' files, and reduces them to only contain the 'needed' ones<br>
   * Like DVD IFO and associated VOBs, Bluray MPLS, CLPINF, SSIF, M2TS and other files.<br>
   * Everything we want to analyze somewhere should be in here
   *
   * @param mediaFile
   * @return
   */
  public static List<MediaInfoFile> detectRelevantFiles(MediaFile mediaFile) {
    List<MediaInfoFile> mediaInfoFiles = new ArrayList<>();

    if (Files.isDirectory(mediaFile.getFileAsPath())) {
      Path folder = mediaFile.getFileAsPath();
      // looks like a disc structure
      for (Path path : Utils.listFilesRecursive(folder)) {
        try {
          mediaInfoFiles.add(new MediaInfoFile(path, Files.size(path)));
        }
        catch (Exception e) {
          LOGGER.debug("could not parse filesize of {} - {}", path, e.getMessage());
        }
      }
      mediaInfoFiles = detectRelevantFiles(mediaInfoFiles);
    }
    else {
      if (mediaFile.getFilesize() == 0) {
        try {
          mediaFile.setFilesize(Files.size(mediaFile.getFileAsPath()));
        }
        catch (IOException e) {
          // ignore - at least, we tried ;)
        }
      }
      mediaInfoFiles.add(new MediaInfoFile(mediaFile));
    }

    return mediaInfoFiles;
  }

  /**
   * detect all relevant DVD files for parsing
   *
   * @param mediaInfoFiles
   *          all found DVD files
   * @return a {@link List} of all relevant DVD files
   */
  private static List<MediaInfoFile> detectRelevantDvdFiles(List<MediaInfoFile> mediaInfoFiles) {
    List<MediaInfoFile> relevantFiles = new ArrayList<>();

    // find VIDEO_TS.IFO - no not work further if not present
    MediaInfoFile ifomif = mediaInfoFiles.stream()
        .filter(mediaInfoFile -> mediaInfoFile.getFilename().equalsIgnoreCase("VIDEO_TS.IFO"))
        .findAny()
        .orElse(null);
    if (ifomif == null) {
      LOGGER.debug("Could not find a valid VIDEO_TS.IFO file");
      return relevantFiles;
    }

    String prefix = "XXXXXXXX"; // just not to leave empty on errors
    try {
      // read VIDEO_TS.IFO
      DataInputStream din = null;
      if (ifomif.getContents() == null) {
        InputStream is = Files.newInputStream(ifomif.getFileAsPath());
        din = new DataInputStream(new BufferedInputStream(is));
      }
      else {
        din = new DataInputStream(new ByteArrayInputStream(ifomif.getContents()));
      }
      IfoReader dvd = new IfoReader();
      // parse
      dvd.readVideoTsIfo(din);
      din.close();

      // get now all unique titleSetNumbers
      List<Integer> sets = dvd.getTitles().stream().map(DvdTitle::getVtsn).distinct().toList();
      for (Integer vtsn : sets) {
        String file = String.format("VTS_%02d_0.IFO", vtsn);
        LOGGER.debug("Reading file {}", file);

        // find it
        MediaInfoFile vts = mediaInfoFiles.stream()
            .filter(mediaInfoFile -> mediaInfoFile.getFilename().equalsIgnoreCase(file))
            .findAny()
            .orElse(null);
        if (vts == null) {
          continue;
        }

        if (vts.getContents() == null) {
          FileInputStream fin = new FileInputStream(vts.getFileAsPath().toString());
          din = new DataInputStream(new BufferedInputStream(fin));
        }
        else {
          din = new DataInputStream(new ByteArrayInputStream(vts.getContents()));
        }
        dvd.readVtsIfo(din, vtsn);
        din.close();
      }
      // DVD/IFO files completely read

      // detected "main" movie
      DvdTitle main = dvd.getTitles()
          .stream()
          .filter(t -> t.getTotalTimeMs() / 1000 < 9800) // limit duration
          .max(Comparator.comparingLong(DvdTitle::getTotalTimeMs))
          .orElse(null);
      if (main == null) {
        // second try, w/o limitation of duration
        main = dvd.getTitles().stream().max(Comparator.comparingLong(DvdTitle::getTotalTimeMs)).orElse(null);
      }
      if (main == null) {
        throw new IOException("Could not identify main DVD files - using fallback.");
      }
      prefix = "VTS_" + String.format("%02d", main.getVtsn());
    }
    catch (IOException e) {
      LOGGER.warn("Error parsing DVD: {} - Maybe just a MediaIfno XML?", ifomif.getFileAsPath(), e.getMessage());
      // try our proven fallback
      // maybe we got the data from XML, so no real files here (but already with MI)
      // so we have to find the biggest VOB
      MediaInfoFile vob = mediaInfoFiles.stream()
          .filter(mediaInfoFile -> mediaInfoFile.getFileExtension().equalsIgnoreCase("VOB"))
          .filter(mediaInfoFile -> mediaInfoFile.getDuration() < 9800) // limit duration
          .max(Comparator.comparingLong(MediaInfoFile::getFilesize))
          .orElse(null);
      if (vob == null) {
        LOGGER.debug("Could not find a valid VOB file");
        return relevantFiles;
      }
      prefix = StrgUtils.substr(vob.getFilename(), "(?i)^(VTS_\\d+).*");
    }

    // now we have the prefix - add them all
    for (MediaInfoFile mif : mediaInfoFiles) {
      if (!mif.getFilename().toUpperCase(Locale.ROOT).startsWith(prefix)) {
        continue;
      }
      // do not use the menu
      // according to https://en.wikibooks.org/wiki/Inside_DVD-Video/Directory_Structure
      // the menu is always in the VTS_nn_0.VOB file
      if (mif.getFilename().toUpperCase(Locale.ROOT).endsWith("_0.VOB")) {
        continue;
      }
      if (mif.getFileExtension().equalsIgnoreCase("VOB") || mif.getFileExtension().equalsIgnoreCase("IFO")) {
        relevantFiles.add(mif);
      }
    }

    return relevantFiles;
  }

  /**
   * detect all relevant Bluray files for parsing
   *
   * @param mediaInfoFiles
   *          all found Bluray files
   * @return a {@link List} of all relevant Bluray files
   */
  private static List<MediaInfoFile> detectRelevantBlurayFiles(List<MediaInfoFile> mediaInfoFiles) {
    List<MediaInfoFile> relevantFiles = new ArrayList<>();

    // find longest playlist
    MPLSObject longestPlaylist = new MPLSObject();
    for (MediaInfoFile mif : mediaInfoFiles) {
      // ignore BACKUP folders 2 levels deep
      Path folder = mif.getFileAsPath().getParent();
      if (folder != null && folder.getFileName() != null && folder.getFileName().toString().equalsIgnoreCase("BACKUP")) {
        continue;
      }
      if (folder.getParent() != null && folder.getParent().getFileName() != null
          && folder.getParent().getFileName().toString().equalsIgnoreCase("BACKUP")) {
        continue;
      }

      if ("mpls".equalsIgnoreCase(mif.getFileExtension())) {
        try {
          DataInputStream din = null;
          if (mif.getContents() == null) {
            FileInputStream fin = new FileInputStream(mif.getFileAsPath().toString());
            din = new DataInputStream(new BufferedInputStream(fin));
          }
          else {
            din = new DataInputStream(new ByteArrayInputStream(mif.getContents()));
          }
          MPLSObject mplsFile = new MPLSReader().readBinary(din);
          din.close();

          if (mplsFile.getDuration() < 120) {
            LOGGER.trace("Playlist {} is too short - ignoring", mif.getFilename());
            continue;
          }
          // we completely ignore playlists with duplicate tracks/streams
          if (!hasDupeTracks(mplsFile)) {
            if (mplsFile.getDuration() > longestPlaylist.getDuration()) {
              longestPlaylist = mplsFile;
              relevantFiles.clear(); // there should only be the last in...
              relevantFiles.add(mif);
              LOGGER.trace("Considering {} as longest playlist (for now)", mif.getFilename());
            }
          }
          else {
            LOGGER.trace("Playlist {} has duplicate streams - ignoring", mif.getFilename());
          }
        }
        catch (Exception e) {
          LOGGER.warn("Could not parse Bluray playlist file: {} - maybe a -mediainfo.xml?", mif.getFileAsPath(), e.getMessage());
        }
      }
    }

    if (longestPlaylist.getDuration() > 0) {
      List<String> items = new ArrayList<>();
      List<Long> durations = new ArrayList<>();

      // get all the needed clips/durations in correct order
      for (PlayItem item : longestPlaylist.getPlayList().getPlayItems()) {
        items.add(item.getAngles()[0].getClipName());
        durations.add((item.getOutTime() - item.getInTime()) / 45000);
      }

      // loop over items (in correct order), and add all files with matching clip numbers
      for (int i = 0; i < items.size(); i++) {
        String item = items.get(i);
        for (MediaInfoFile mif : mediaInfoFiles) {
          // ignore BACKUP folders 2 levels deep
          Path folder = mif.getFileAsPath().getParent();
          if (folder != null && folder.getFileName() != null && folder.getFileName().toString().equalsIgnoreCase("BACKUP")) {
            continue;
          }
          if (folder != null && folder.getParent() != null && folder.getParent().getFileName() != null
              && folder.getParent().getFileName().toString().equalsIgnoreCase("BACKUP")) {
            continue;
          }
          // do not add all matching playlists - we have ours already in
          if (mif.getFileExtension().equalsIgnoreCase("mpls")) {
            continue;
          }
          if (mif.getFilename().startsWith(item)) {
            mif.setDuration(durations.get(i).intValue());
            relevantFiles.add(mif);
          }
        }
      }
    }
    else {
      // MediaInfo XML of a Bluray (or a Bluray file) has only one MIF
      if (mediaInfoFiles.size() == 1 && mediaInfoFiles.get(0).getSnapshot() != null) {
        // xml was parsed - return as relevant
        relevantFiles.add(mediaInfoFiles.get(0));
        return relevantFiles;
      }

      // no? just use our traditional way of finding the "biggest" file...
      MediaInfoFile mainVideo = mediaInfoFiles.stream()
          .filter(mediaInfoFile -> mediaInfoFile.getFileExtension().equalsIgnoreCase("m2ts"))
          .max(Comparator.comparingLong(MediaInfoFile::getFilesize))
          .orElse(null);

      if (mainVideo == null || !mainVideo.getFilename().matches("^\\d{5}.*")) {
        return Collections.emptyList();
      }

      String prefix = mainVideo.getFilename().substring(0, 5);
      for (MediaInfoFile mif : mediaInfoFiles) {
        if (mif.getFilename().startsWith(prefix)) {
          relevantFiles.add(mif);
        }
      }
    }

    return relevantFiles;
  }

  /**
   * Some playlists have set the same streams over and over.<br>
   * This is probably not a correct one(?) (or how should a HW player play this?!
   *
   * @param mplsObject
   * @return true or false
   */
  private static boolean hasDupeTracks(MPLSObject mplsObject) {
    List<String> streams = new ArrayList<>();
    for (PlayItem item : mplsObject.getPlayList().getPlayItems()) {
      String itemKey = item.getAngles()[0].getClipName();
      if (!streams.contains(itemKey)) {
        streams.add(itemKey);
      }
      else {
        return true;
      }
    }
    return false;
  }

  /**
   * detect all relevant HD-DVD files for parsing
   *
   * @param mediaInfoFiles
   *          all found HD-DVD files
   * @return a {@link List} of all relevant HD-DVD files
   */
  private static List<MediaInfoFile> detectRelevantHdDvdFiles(List<MediaInfoFile> mediaInfoFiles) {
    // a) find the "main" title (biggest evo file)
    MediaInfoFile evo = mediaInfoFiles.stream()
        .filter(mediaInfoFile -> mediaInfoFile.getFileExtension().equalsIgnoreCase("evo"))
        .max(Comparator.comparingLong(MediaInfoFile::getFilesize))
        .orElse(null);

    if (evo == null) {
      return Collections.emptyList();
    }

    List<MediaInfoFile> relevantFiles = new ArrayList<>();
    relevantFiles.add(evo);

    return relevantFiles;
  }

  /**
   * Gets the real mediainfo values as {@link List}. If you pass multiple keys, every found value will be added (in the same order) into the
   * {@link List}
   *
   * @param miSnapshot
   *          the mediainfo snapshot to load the data from
   * @param streamKind
   *          MediaInfo.StreamKind.(General|Video|Audio|Text|Chapters|Image|Menu )
   * @param streamNumber
   *          the stream number (0 for first)
   * @param keys
   *          the information you want to fetch
   * @return a {@link List} of the media information you asked<br>
   *         <b>OR AN EMPTY LIST IF MEDIAINFO COULD NOT BE LOADED</b> (never NULL)
   */
  public static List<String> getMediaInfoValues(Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot, MediaInfo.StreamKind streamKind,
      int streamNumber, String... keys) {
    // prevent NPE
    if (miSnapshot == null) {
      return Collections.emptyList();
    }

    List<Map<String, String>> stream = miSnapshot.get(streamKind);
    if (stream == null) {
      return Collections.emptyList();
    }

    Map<String, String> info = stream.get(streamNumber);
    if (info == null) {
      return Collections.emptyList();
    }

    List<String> values = new ArrayList<>();

    // normalize keys
    Map<String, String> normalizedMap = normalizeKeys(info);
    List<String> normalizedKeys = normalizeKeys(keys);

    for (String key : normalizedKeys) {
      String value = normalizedMap.get(key);
      if (StringUtils.isNotBlank(value)) {
        values.add(value);
      }
    }

    return values;
  }

  /**
   * Gets the real mediainfo value
   *
   * @param miSnapshot
   *          the mediainfo snapshot to load the data from
   * @param streamKind
   *          MediaInfo.StreamKind.(General|Video|Audio|Text|Chapters|Image|Menu )
   * @param streamNumber
   *          the stream number (0 for first)
   * @param keys
   *          the information you want to fetch
   * @return the media information you asked<br>
   *         <b>OR AN EMPTY STRING IF MEDIAINFO COULD NOT BE LOADED</b> (never NULL)
   */
  public static String getMediaInfoValue(Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot, MediaInfo.StreamKind streamKind,
      int streamNumber, String... keys) {
    return getMediaInfoValues(miSnapshot, streamKind, streamNumber, keys).stream().findFirst().orElse("");
  }

  /**
   * Checks, if a specific string can be found in one or multiple values<br>
   * comes handy for different MI versions, where something changed....
   *
   * @param miSnapshot
   *          the mediainfo snapshot to load the data from
   * @param streamKind
   *          MediaInfo.StreamKind.(General|Video|Audio|Text|Chapters|Image|Menu )
   * @param streamNumber
   *          the stream number (0 for first)
   * @param search
   *          the information to search for
   * @param keys
   *          the information you want to fetch
   * @return the search value you asked for, or empty string
   */
  public static String getMediaInfoContains(Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot, MediaInfo.StreamKind streamKind,
      int streamNumber, String search, String... keys) {
    // prevent NPE
    if (miSnapshot == null) {
      return "";
    }

    List<Map<String, String>> stream = miSnapshot.get(streamKind);
    if (stream == null) {
      return "";
    }

    Map<String, String> info = stream.get(streamNumber);
    if (info == null) {
      return "";
    }

    // normalize keys
    Map<String, String> normalizedMap = normalizeKeys(info);
    List<String> normalizedKeys = normalizeKeys(keys);

    for (String key : normalizedKeys) {
      String value = normalizedMap.get(key);
      if (StringUtils.isNotBlank(value) && value.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))) {
        return search;
      }
    }

    return "";
  }

  private static Map<String, String> normalizeKeys(Map<String, String> originalMap) {
    Map<String, String> normalizedMap = new HashMap<>();

    for (Map.Entry<String, String> entry : originalMap.entrySet()) {
      normalizedMap.put(normalizeKey(entry.getKey()), entry.getValue());
    }

    return normalizedMap;
  }

  private static List<String> normalizeKeys(String... keys) {
    List<String> normalizedKeys = new ArrayList<>();

    for (String key : keys) {
      normalizedKeys.add(normalizeKey(key));
    }

    return normalizedKeys;
  }

  /**
   * normalized the mediainfo key for better support of different sources (libmediainfo, XML from mediainfo, XML from tmm, ...)
   *
   * @param key
   *          the key to be normalized
   * @return the normalized key
   */
  public static String normalizeKey(String key) {
    // remove ()
    String normalizedKey = key.replace("(", ""); // faster than replaceAll
    normalizedKey = normalizedKey.replace(")", ""); // faster than replaceAll
    // replace /*:. with _
    normalizedKey = normalizedKey.replace('/', '_'); // faster than replaceAll
    normalizedKey = normalizedKey.replace('*', '_'); // faster than replaceAll
    normalizedKey = normalizedKey.replace(':', '_'); // faster than replaceAll
    normalizedKey = normalizedKey.replace('.', '_'); // faster than replaceAll

    // make everything lowercase
    normalizedKey = normalizedKey.toLowerCase(Locale.ROOT);

    return normalizedKey;
  }

  /**
   * gets a mediainfo value directly by calling libmediainfo.<br />
   * ATTENTION: this causes libmediainfo to open the file
   *
   * @param mediaFile
   *          the {@link MediaFile} to analyze
   * @param streamKind
   *          the stream kind
   * @param streamNumber
   *          the stream number
   * @param keys
   *          the key
   * @return the requested value or an empty string
   */
  public static String getMediaInfoDirect(MediaFile mediaFile, MediaInfo.StreamKind streamKind, int streamNumber, String... keys) {
    List<MediaInfoFile> mediaInfoFiles = getMediaInfoFromSingleFile(mediaFile);
    if (!mediaInfoFiles.isEmpty()) {
      return getMediaInfoValue(mediaInfoFiles.get(0).getSnapshot(), streamKind, streamNumber, keys);
    }
    return "";
  }

  /**
   * gather the subtitle information for the given {@link MediaFile}, but solely from the file naming.<br />
   * usable for subtitle files
   *
   * @param mediaFile
   *          the media file
   */
  public static MediaFileSubtitle gatherSubtitleLanguageFromFile(MediaFile mediaFile) {
    String filename = mediaFile.getFilename();
    String path = mediaFile.getPath();
    MediaFileSubtitle sub = new MediaFileSubtitle();

    // parse FIRST from idx
    if (filename.endsWith(".sub")) {
      Path idx = Paths.get(path, filename.replaceFirst("sub$", "idx"));

      try (FileReader fr = new FileReader(idx.toFile()); BufferedReader br = new BufferedReader(fr)) {
        String line;
        while ((line = br.readLine()) != null) {
          String lang = "";
          if (line.startsWith("id:")) {
            // first entry wins
            lang = StrgUtils.substr(line, "id: (.*?),");
          }
          if (!lang.isEmpty()) {
            sub.setLanguage(LanguageUtils.findLanguageInString(lang));
            break;
          }
        }
      }
      catch (IOException e) {
        LOGGER.debug("could not read idx file: {}", e.getMessage());
      }
    }
    sub.setCodec(mediaFile.getExtension());
    return sub;
  }

  private static MediaFileSubtitle gatherSubtitleInformationFromMediainfo(Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot, int i) {

    MediaFileSubtitle stream = new MediaFileSubtitle();
    stream.id = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Text, i, "StreamKindPos");

    String codec = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Text, i, "CodecID/Hint", "Format");
    stream.setCodec(codec.replaceAll("\\p{Punct}", ""));
    String lang = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Text, i, "Language", "Language/String");
    stream.setLanguage(parseLanguageFromString(lang));

    String forced = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Text, i, "Forced");
    boolean b = forced.equalsIgnoreCase("true") || forced.equalsIgnoreCase("yes");
    stream.setForced(b);

    String title = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Text, i, "Title");
    if (StringUtils.isNotBlank(title)) {
      stream.setTitle(title);
    }

    // "default" subtitle stream?
    String def = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Text, i, "Default");
    if (def.equalsIgnoreCase("true") || def.equalsIgnoreCase("yes")) {
      stream.setDefaultStream(true);
    }
    return stream;
  }

  /**
   * gather the subtitle information for the given {@link MediaFile}, but with libmediainfo<br />
   * usable for video files with embedded subtitles
   *
   * @param mediaFile
   *          the media file
   */
  private static void gatherSubtitleInformation(MediaFile mediaFile, Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot) {
    mediaFile.clearSubtitleInformation();

    int streams = getSubtitleStreamCount(miSnapshot);

    // subtitle FILE, can have ONE stream, or just parse filename
    if (mediaFile.getType() == MediaFileType.SUBTITLE) {
      // 1. MediaInfo
      MediaFileSubtitle sub = new MediaFileSubtitle();
      if (streams > 0) {
        MediaFileSubtitle stream = gatherSubtitleInformationFromMediainfo(miSnapshot, 0);
        sub = stream;
      }

      // 2. IDX file
      if (sub.getLanguage().isEmpty()) {
        MediaFileSubtitle file = MediaFileHelper.gatherSubtitleLanguageFromFile(mediaFile);
        sub.setLanguage(file.getLanguage());
      }

      // 3. Language from Filename // NOOO, not here - we need the video basename for that!
      // MediaStreamInfo info = MediaFileHelper.gatherLanguageInformation(mediaFile.getBasename(), mainVideoFile.getBasename());

      mediaFile.setSubtitles(Collections.singletonList(sub));
    }
    else {
      // embedded
      List<MediaFileSubtitle> subtitles = new ArrayList<>();
      for (int i = 0; i < streams; i++) {
        MediaFileSubtitle stream = gatherSubtitleInformationFromMediainfo(miSnapshot, i);
        subtitles.add(stream);
      }
      mediaFile.setSubtitles(subtitles);
    }
  }

  /**
   * gather the audio information for the given {@link MediaFile}
   *
   * @param mediaFile
   *          the media file
   * @param miSnapshot
   *          the mediainfo snapshot to load the data from
   */
  private static void gatherAudioInformation(MediaFile mediaFile, Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot) {
    mediaFile.clearAudioInformation();

    // https://github.com/MediaArea/MediaInfoLib/tree/master/Source/MediaInfo/Audio
    List<MediaFileAudioStream> audioStreams = new ArrayList<>();

    for (int i = 0; i < getAudioStreamCount(miSnapshot); i++) {
      // workaround for DTS & TrueHD variant detection
      // search for well known String in defined keys (changes between different MI versions!)
      String[] acSearch = new String[] { "Format", "Format_Profile", "Format_Commercial", "Format_Commercial_IfAny", "CodecID", "Codec" };
      String audioCodec = getMediaInfoContains(miSnapshot, MediaInfo.StreamKind.Audio, i, "TrueHD", acSearch);
      if (StringUtils.isBlank(audioCodec)) {
        audioCodec = getMediaInfoContains(miSnapshot, MediaInfo.StreamKind.Audio, i, "DTS", acSearch);
      }

      // else just take format
      if (StringUtils.isBlank(audioCodec)) {
        audioCodec = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "Format");
        audioCodec = audioCodec.replaceAll("\\p{Punct}", "");
      }

      // E-AC-3 in Format_String
      if ("ac3".equalsIgnoreCase(audioCodec)) {
        String formatString = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "Format_String");
        if ("e-ac-3".equalsIgnoreCase(formatString)) {
          audioCodec = "EAC3";
        }
      }

      // https://github.com/Radarr/Radarr/blob/develop/src/NzbDrone.Core/MediaFiles/MediaInfo/MediaInfoFormatter.cs#L35
      String addFeature = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "Format_AdditionalFeatures");
      if (!addFeature.isEmpty()) {
        if ("dts".equalsIgnoreCase(audioCodec)) {
          if (addFeature.startsWith("XLL")) {
            if (addFeature.endsWith("X")) {
              audioCodec = "DTS-X";
            }
            else if (addFeature.endsWith("IMAX")) {
              audioCodec = "DTS-X-IMAX";
            }
            else {
              audioCodec = "DTSHD-MA";
            }
          }
          if (addFeature.equals("ES")) {
            audioCodec = "DTS-ES";
          }
          if (addFeature.equals("XBR")) {
            audioCodec = "DTSHD-HRA";
          }
          if (addFeature.equals("X96")) {
            audioCodec = "DTS-96/24";
          }
          // stays DTS
        }
        if ("TrueHD".equalsIgnoreCase(audioCodec)) {
          if (addFeature.equalsIgnoreCase("16-ch")) {
            audioCodec = "TrueHD/Atmos";
          }
        }
      }

      // old 18.05 style
      String audioProfile = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "Format_Profile");
      if (!audioProfile.isEmpty()) {
        if ("dts".equalsIgnoreCase(audioCodec)) {
          // <Format_Profile>X / MA / Core</Format_Profile>
          if (audioProfile.contains("ES")) {
            audioCodec = "DTS-ES";
          }
          if (audioProfile.contains("HRA")) {
            audioCodec = "DTSHD-HRA";
          }
          if (audioProfile.contains("MA")) {
            audioCodec = "DTSHD-MA";
          }
          if (audioProfile.contains("X")) {
            audioCodec = "DTS-X";
          }
        }
        if ("TrueHD".equalsIgnoreCase(audioCodec)) {
          if (audioProfile.contains("Atmos")) {
            audioCodec = "TrueHD/Atmos";
          }
        }
        if ("MPEG Audio".equalsIgnoreCase(audioCodec)) {
          String codecId = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "CodecID");
          if ("55".equals(codecId) || "A_MPEG/L3".equalsIgnoreCase(codecId) || "Layer 3".equalsIgnoreCase(audioProfile)) {
            audioCodec = "MP3";
          }
          else if ("A_MPEG/L2".equalsIgnoreCase(codecId) || "Layer 2".equalsIgnoreCase(audioProfile)) {
            audioCodec = "MP2";
          }
        }
      }

      // newer 18.12 style
      if ("ac3".equalsIgnoreCase(audioCodec) || "eac3".equalsIgnoreCase(audioCodec) || "dts".equalsIgnoreCase(audioCodec)
          || "TrueHD".equalsIgnoreCase(audioCodec)) {
        String commName = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "Format_Commercial", "Format_Commercial_IfAny")
            .toLowerCase(Locale.ROOT);

        if (!commName.isEmpty()) {
          if (commName.contains("master audio")) {
            audioCodec = "DTSHD-MA";
          }
          if (commName.contains("high resolution audio")) {
            audioCodec = "DTSHD-HRA";
          }
          if (commName.contains("extended") || commName.contains("es matrix") || commName.contains("es discrete")) {
            audioCodec = "DTS-ES";
          }
          if (commName.contains("truehd") && commName.contains("atmos")) {
            audioCodec = "TrueHD/Atmos";
          }
          if (commName.contains("dolby digital plus") && commName.contains("atmos")) {
            audioCodec = "EAC3/Atmos";
          }
          // Dolby Digital EX
          if (commName.contains("ex audio")) {
            audioCodec = "AC3EX";
          }
        }
      }

      MediaFileAudioStream stream = new MediaFileAudioStream();
      stream.id = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "StreamKindPos");
      stream.setCodec(audioCodec);

      // AAC sometimes codes channels into Channel(s)_Original
      // and DTS-ES has an additional core channel
      int ch = parseChannelsAsInt(getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "Channel(s)"));
      int ch2 = parseChannelsAsInt(getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "Channel(s)_Original"));
      if (ch2 > ch) {
        ch = ch2;
      }
      stream.setAudioChannels(ch);

      String br = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "BitRate", "BitRate_Maximum", "BitRate_Minimum", "BitRate_Nominal");
      if (StringUtils.isNotBlank(br)) {
        try {
          String[] brMode = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "BitRate_Mode").split("/");
          if (brMode.length > 1) {
            String[] brChunks = br.split("/");
            int brMult = 0;
            for (String brChunk : brChunks) {
              brMult += MetadataUtil.parseInt(brChunk.strip(), 0);
            }
            stream.setBitrate(brMult / 1000);
          }
          else {
            br = br.replace("kb/s", "");// 448 / 1000 = 0
            stream.setBitrate(Integer.parseInt(br.strip()) / 1000);
          }
        }
        catch (Exception e) {
          LOGGER.debug("could not parse bitrate: {}", e.getMessage());
        }
      }

      String bd = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "BitDepth");
      stream.setBitDepth(MetadataUtil.parseInt(bd, 0));

      String language = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "Language", "Language/String");
      if (language.isEmpty()) {
        if (!mediaFile.isDiscFile()) { // video_ts parsed 'ts' as Tsonga
          // try to parse from filename
          String shortname = mediaFile.getBasename().toLowerCase(Locale.ROOT);
          stream.setLanguage(parseLanguageFromString(shortname));
        }
      }
      else {
        stream.setLanguage(parseLanguageFromString(language));
      }

      // "default" audio stream?
      String def = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "Default");
      if (def.equalsIgnoreCase("yes")) {
        stream.setDefaultStream(true);
      }

      // Title of audiostream
      String title = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Audio, i, "Title");
      stream.setTitle(title);

      audioStreams.add(stream);
    }

    // 3. Language from Filename // NOOO, not here - we need the video basename for that!
    // we might parse the language from file, IF we have only ONE stream
    // if (audioStreams.size() == 1) {
    // MediaFileAudioStream audio = audioStreams.get(0);
    // MediaStreamInfo info = gatherLanguageInformation(mediaFile.getBasename(), "");
    // if (StringUtils.isBlank(audio.getLanguage())) {
    // audio.setLanguage(info.getLanguage());
    // }
    // // only set filename detected "title" on external file!
    // if (mediaFile.getType() == MediaFileType.AUDIO && StringUtils.isBlank(audio.getTitle())) {
    // audio.setTitle(info.getTitle());
    // }
    // }

    mediaFile.setAudioStreams(audioStreams);
  }

  /**
   * how many streams of chosen kind do we have gathered?
   *
   * @param miSnapshot
   *          the media info snapshot
   * @param kind
   *          the stream kind
   * @return the stream count
   */
  private static int getStreamCount(Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot, MediaInfo.StreamKind kind) {
    List<Map<String, String>> map = miSnapshot.get(kind);
    if (map != null) {
      return map.size();
    }
    return 0;
  }

  /**
   * get the audio stream count (can be either a field in mediainfo or just the count of the streams)
   *
   * @param miSnapshot
   *          the snapshot to parse
   * @return the stream count
   */
  private static int getAudioStreamCount(Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot) {
    int streamCount = 0;
    try {
      streamCount = MetadataUtil.parseInt(getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "AudioCount"));
    }
    catch (Exception ignored) {
      // ignore
    }
    if (streamCount == 0) {
      streamCount = getStreamCount(miSnapshot, MediaInfo.StreamKind.Audio);
    }

    return streamCount;
  }

  /**
   * get the subtitle stream count (can be either a field in mediainfo or just the count of the streams)
   *
   * @param miSnapshot
   *          the snapshot to parse
   * @return the stream count
   */
  private static int getSubtitleStreamCount(Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot) {
    int streamsTextCount = MetadataUtil.parseInt(getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "TextCount"), 0);
    int streamsStreamCount = MetadataUtil.parseInt(getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Text, 0, "StreamCount"), 0);

    return streamsTextCount > 0 ? streamsTextCount : streamsStreamCount;
  }

  /**
   * channels usually filled like "5.1ch" or "8 / 6". Take the higher
   *
   * @return channels as int
   */
  public static int parseChannelsAsInt(String channels) {
    int highest = 0;
    if (!channels.isEmpty()) {
      try {
        String[] parts = channels.split("/");
        for (String p : parts) {
          if (p.toLowerCase(Locale.ROOT).contains("object")) {
            // "11 objects / 6 channels" - ignore objects
            continue;
          }
          p = p.replaceAll("[a-zA-Z]", ""); // remove now all characters

          int ch = 0;
          String[] c = p.split("[^0-9]"); // split on not-numbers and count all; so 5.1 -> 6
          for (String s : c) {
            if (s.matches("[0-9]+")) {
              ch += Integer.parseInt(s);
            }
          }
          if (ch > highest) {
            highest = ch;
          }
        }
      }
      catch (NumberFormatException e) {
        highest = 0;
      }
    }
    return highest;
  }

  private static void gatherExtraData(MediaFile mediaFile, Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot) {
    String imdbId = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "id");
    if (MediaIdUtil.isValidImdbId(imdbId)) {
      mediaFile.addExtraData("imdbId", imdbId);
    }

    String title = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "Title", "Movie");
    if (StringUtils.isNotBlank(title)) {
      mediaFile.addExtraData("title", title);
    }

    String originalTitle = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "Original");
    if (StringUtils.isNotBlank(originalTitle)) {
      mediaFile.addExtraData("originalTitle", originalTitle);
    }

    String plot = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "extra/LongDescription", "Summary", "Description", "Comment");
    if (StringUtils.isNotBlank(plot)) {
      mediaFile.addExtraData("plot", plot);
    }

    String genre = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "Genre");
    if (StringUtils.isNotBlank(genre)) {
      mediaFile.addExtraData("genre", genre);
    }

    String dateAsString = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "Released_Date", "Recorded_Date", "Date");
    if (StringUtils.isNotBlank(dateAsString)) {
      try {
        Date date = StrgUtils.parseDate(dateAsString);
        if (date != null) {
          Calendar calendar = Calendar.getInstance();
          calendar.setTime(date);
          mediaFile.addExtraData("year", String.valueOf(calendar.get(Calendar.YEAR)));

          // is parsable - just readd as string (will be used later)
          mediaFile.addExtraData("releaseDate", dateAsString);
        }
      }
      catch (Exception ignored) {
        // ignored
      }
    }

    String season = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "Season");
    if (StringUtils.isNotBlank(season)) {
      mediaFile.addExtraData("season", season);
    }

    String episode = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "Recorded_Date", "Date");
    if (StringUtils.isNotBlank(episode)) {
      mediaFile.addExtraData("episode", episode);
    }
  }

  /**
   * gather the video information for the given {@link MediaFile}
   *
   * @param mediaFile
   *          the media file
   * @param miSnapshot
   *          the mediainfo snapshot to load the data from
   */
  private static void gatherVideoInformation(MediaFile mediaFile, Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot) {
    mediaFile.clearVideoInformation();

    int height = 0;
    int width = 0;

    try {
      height = MetadataUtil.parseInt(getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "Height"));
      width = MetadataUtil.parseInt(getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "Width"));
    }
    catch (Exception e) {
      LOGGER.trace("could not parse width/height: {}", e.getMessage());
    }

    // calculate the "aspect" ratio. If it is too high, that might be a SBS video
    String mvc = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "MultiView_Count");
    if ("2".equals(mvc) && width > 0 && height > 0) {
      float calculatedAspect = width / (float) height;
      if (calculatedAspect > 3) {
        width = width / 2;
      }
      else if (calculatedAspect < 1.5) {
        height = height / 2;
      }
    }

    mediaFile.setVideoWidth(width);
    mediaFile.setVideoHeight(height);

    // video bitrate (BitRate)
    String br = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "BitRate");
    if (StringUtils.isNotBlank(br)) {
      try {
        mediaFile.setVideoBitRate(Integer.parseInt(br) / 1000); // in kbps
      }
      catch (NumberFormatException e) {
        mediaFile.setVideoBitRate(0);
      }
    }

    String scanType = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "ScanType");

    String codecId = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "CodecID");

    String videoCodec = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "CodecID/Hint", "Format");

    // fix for Microsoft VC-1
    if (StringUtils.containsIgnoreCase(videoCodec, "Microsoft")) {
      videoCodec = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "Format");
    }

    // workaround for XVID
    if (codecId.equalsIgnoreCase("XVID")) {
      // XVID is open source variant MP4, only detectable through codecId
      videoCodec = "XVID";
    }

    // detect the right MPEG version
    if (StringUtils.containsIgnoreCase(videoCodec, "MPEG")) {
      // search for the version
      try {
        // Version 2
        int version = Integer.parseInt(getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "Format_Version").replaceAll("\\D*", ""));
        videoCodec = "MPEG-" + version;
      }
      catch (Exception e) {
        LOGGER.trace("could not parse MPEG version: {}", e.getMessage());
      }
    }
    mediaFile.setVideoCodec(getFirstEntryViaScanner(videoCodec));

    String bd = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "BitDepth");
    mediaFile.setBitDepth(MetadataUtil.parseInt(bd, 0));

    try {
      String fr = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "FrameRate", "FrameRate_Original");
      mediaFile.setFrameRate(Double.parseDouble(fr));
    }
    catch (Exception e) {
      LOGGER.trace("could not parse frame rate: {}", e.getMessage());
    }

    if (height == 0 || scanType.isEmpty()) {
      mediaFile.setExactVideoFormat("");
    }
    else {
      mediaFile.setExactVideoFormat(height + "" + Character.toLowerCase(scanType.charAt(0)));
    }

    String containerFormat = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "Menu_Format_List", "Format");
    if (StringUtils.isBlank(containerFormat)) {
      containerFormat = getFirstEntryViaScanner(getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "Format/Extensions"));
    }
    if (StringUtils.isNotBlank(containerFormat)) {
      if ("BDAV".equalsIgnoreCase(containerFormat)) {
        // special handling for Blu-ray Video
        containerFormat = "Blu-Ray Video";
      }
      else if ("DVD-Video".equalsIgnoreCase(containerFormat)) {
        // string the dash from DVD-Video
        containerFormat = "DVD Video";
      }
      mediaFile.setContainerFormat(containerFormat);
    }
    else {
      // if container format is still empty -> insert the extension
      mediaFile.setContainerFormat(mediaFile.getExtension());
    }

    // we use the storage ratio (width / heigth) and multiply by PAR
    // we do not care about the DAR
    String parString = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "PixelAspectRatio", "Pixel_aspect_ratio");
    if (parString.isEmpty()) {
      parString = "1.0";
    }
    try {
      float par = Float.parseFloat(parString);
      mediaFile.setPixelAspectRatio(par);
      mediaFile.setAspectRatio((float) mediaFile.getVideoWidth() * par / mediaFile.getVideoHeight());
    }
    catch (Exception e) {
      LOGGER.warn("Could not parse AspectRatio '{}'", parString);
    }

    mediaFile.setVideo3DFormat(parse3DFormat(mediaFile, miSnapshot));

    // prefer official HDR namings (see https://de.wikipedia.org/wiki/High_Dynamic_Range_Video) over technical
    String hdrFormat = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "HDR_Format_String", "HDR_Format");
    String hdrCompat = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "HDR_Format_Compatibility");
    // detect them combined!
    hdrFormat = detectHdrFormat(hdrFormat + " / " + hdrCompat);

    if (StringUtils.isBlank(hdrFormat)) {
      // no HDR format found? try another mediainfo field
      hdrFormat = detectHdrFormat(getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "transfer_characteristics"));
    }
    if (StringUtils.isBlank(hdrFormat)) {
      // STILL no HDR format found? check color space
      String col = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "colour_primaries");
      if (col.contains("2020") || col.contains("2100")) {
        hdrFormat = "HDR";
      }
    }
    if (StringUtils.isBlank(hdrFormat)) {
      // STILL no HDR format found? check known HDR transfer protocols
      String trans = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "transfer_characteristics");
      if (trans.contains("2020") || trans.contains("2100") || trans.equals("PQ") || trans.equals("HLG")) {
        hdrFormat = "HDR";
      }
    }
    mediaFile.setHdrFormat(hdrFormat);

    if (Settings.getInstance().isArdEnabled()) {
      TmmTask task = new MediaFileARDetectorTask(mediaFile);
      task.run();
    }
  }

  // MediaInfo values: https://github.com/MediaArea/MediaInfoLib/blob/master/Source/MediaInfo/Video/File_Mpegv.cpp#L34
  private static String detectHdrFormat(String source) {
    source = source.toLowerCase(Locale.ROOT);
    ArrayList<String> ret = new ArrayList<>();

    if (source.contains("dolby vision")) {
      ret.add("Dolby Vision");
    }
    if (source.contains("hlg")) {
      ret.add("HLG");
    }
    if (source.contains("2094") || source.contains("hdr10+")) {
      ret.add("HDR10+");
    }
    source = source.replaceAll("hdr10\\+", "");
    if ((source.contains("2086") || source.contains("hdr10"))) {
      ret.add("HDR10");
    }
    return String.join(", ", ret);
  }

  /**
   * gather image information for the given {@link MediaFile}
   *
   * @param mediaFile
   *          the media file
   * @param miSnapshot
   *          the mediainfo snapshot to load the data from
   */
  public static void gatherImageInformation(MediaFile mediaFile, Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot) {
    int height = MetadataUtil.parseInt(getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Image, 0, "Height"), 0);
    int width = MetadataUtil.parseInt(getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Image, 0, "Width"), 0);
    String videoCodec = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Image, 0, "CodecID/Hint", "Format");
    mediaFile.clearVideoInformation();

    mediaFile.checkForAnimation();

    mediaFile.setVideoHeight(height);
    mediaFile.setVideoWidth(width);
    mediaFile.setVideoCodec(getFirstEntryViaScanner(videoCodec));

    String extensions = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "Codec/Extensions", "Format");
    // get first extension
    mediaFile.setContainerFormat(getFirstEntryViaScanner(extensions).toLowerCase(Locale.ROOT));

    String bd = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Image, 0, "BitDepth");
    mediaFile.setBitDepth(MetadataUtil.parseInt(bd, 0));

    // if container format is still empty -> insert the extension
    if (StringUtils.isBlank(mediaFile.getContainerFormat())) {
      mediaFile.setContainerFormat(mediaFile.getExtension());
    }
  }

  /**
   * use a scanner to get the first entry
   *
   * @param string
   *          the string to parse
   * @return the first entry or an empty string
   */
  public static String getFirstEntryViaScanner(String string) {
    if (StringUtils.isBlank(string)) {
      return "";
    }
    try (Scanner scanner = new Scanner(string)) {
      return scanner.next();
    }
    catch (Exception e) {
      LOGGER.error("could not parse string {} with a Scanner: {}", string, e.getMessage());
    }
    return "";
  }

  private static void gatherMediaInformationFromFile(MediaFile mediaFile, List<MediaInfoFile> mediaInfoFiles) {
    Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot = mediaInfoFiles.get(0).getSnapshot();

    if (miSnapshot == null) {
      // MI could not be opened
      LOGGER.error("error getting MediaInfo for {}", mediaFile.getFilename());
      // set container format to do not trigger it again
      mediaFile.setContainerFormat(mediaFile.getExtension());
      return;
    }
    LOGGER.trace("got mediainfo for '{}'", mediaFile.getFileAsPath());

    switch (mediaFile.getType()) {
      case VIDEO:
        // *****************
        // get extra data
        // *****************
        gatherExtraData(mediaFile, miSnapshot);
        // fall through

      case EXTRA:
      case SAMPLE:
      case TRAILER:
        // *****************
        // get video stream
        // *****************
        gatherVideoInformation(mediaFile, miSnapshot);

        // *****************
        // get audio streams
        // *****************
        gatherAudioInformation(mediaFile, miSnapshot);

        // ********************
        // get subtitle streams
        // ********************
        gatherSubtitleInformation(mediaFile, miSnapshot);

        break;

      case AUDIO:
        gatherAudioInformation(mediaFile, miSnapshot);
        gatherSubtitleInformation(mediaFile, miSnapshot); // eg .MKA with subtitles see #2180
        break;

      case POSTER:
      case BANNER:
      case FANART:
      case THUMB:
      case EXTRAFANART:
      case GRAPHIC:
      case SEASON_POSTER:
      case SEASON_FANART:
      case SEASON_BANNER:
      case SEASON_THUMB:
      case LOGO:
      case CLEARLOGO:
      case CLEARART:
      case CHARACTERART:
      case DISC:
      case EXTRATHUMB:
      case KEYART:
        gatherImageInformation(mediaFile, miSnapshot);
        break;

      case NFO: // do nothing here, but do not display default warning (since we got the filedate)
        break;

      case SUBTITLE:
        gatherSubtitleInformation(mediaFile, miSnapshot);
        break;

      default:
        LOGGER.debug("no mediainformation handling for MediaFile type {} yet.", mediaFile.getType());
        break;
    }

    // container format for all except subtitles (subtitle container format is handled another way)
    if (mediaFile.getType() == MediaFileType.SUBTITLE) {
      mediaFile.setContainerFormat(mediaFile.getExtension());
    }
    else {
      String extensions = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "Codec/Extensions", "Format");
      // get first extension
      mediaFile.setContainerFormat(getFirstEntryViaScanner(extensions).toLowerCase(Locale.ROOT));

      // if container format is still empty -> insert the extension
      if (StringUtils.isBlank(mediaFile.getContainerFormat())) {
        mediaFile.setContainerFormat(mediaFile.getExtension());
      }
    }

    switch (mediaFile.getType()) {
      case VIDEO:
      case EXTRA:
      case SAMPLE:
      case TRAILER:
      case AUDIO:
        // overall bitrate (OverallBitRate/String)
        String br = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "OverallBitRate");
        // if no OverallBitRate is available, parse the maximum bitrate
        if (StringUtils.isBlank(br)) {
          br = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "OverallBitRate_Maximum");
        }
        if (StringUtils.isNotBlank(br)) {
          try {
            mediaFile.setOverallBitRate(Integer.parseInt(br) / 1000); // in kbps
          }
          catch (NumberFormatException e) {
            mediaFile.setOverallBitRate(0);
          }
        }

        // get embedded title from general info
        String miTitle = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "Title");
        if (!miTitle.isEmpty()) {
          mediaFile.setTitle(miTitle);
        }

        // try getting some real file dates from MI
        // try {
        // @formatter:off
        //    Released_Date             : The date/year that the item was released.
        //    Original/Released_Date    : The date/year that the item was originaly released.
        //    Recorded_Date             : The time/date/year that the recording began.
        //    Encoded_Date              : The time/date/year that the encoding of this item was completed began.
        //    Tagged_Date               : The time/date/year that the tags were done for this item.
        //    Written_Date              : The time/date/year that the composition of the music/script began.
        //    Mastered_Date             : The time/date/year that the item was tranfered to a digitalmedium.
        //    File_Created_Date         : The time that the file was created on the file system
        //    File_Modified_Date        : The time that the file was modified on the file system
        // @formatter:on
        // String embeddedDate = getMediaInfo(StreamKind.General, 0, "Released_Date", "Original/Released_Date", "Recorded_Date", "Encoded_Date",
        // "Mastered_Date");
        // Date d = StrgUtils.parseDate(embeddedDate);
        // if (d.toInstant().toEpochMilli() < filedate) {
        // // so this is older than our file date - use it :)
        // filedate = d.toInstant().toEpochMilli();
        // }
        // }
        // catch (ParseException e) {
        // // could not parse MI date... ignore
        // }

        // Duration;Play time of the stream in ms
        // Duration/String;Play time in format : XXx YYy only, YYy omited if zero
        // Duration/String1;Play time in format : HHh MMmn SSs MMMms, XX om.if.z.
        // Duration/String2;Play time in format : XXx YYy only, YYy omited if zero
        // Duration/String3;Play time in format : HH:MM:SS.MMM

        mediaFile.setDuration(parseDuration(miSnapshot));

        break;

      default:
        break;
    }
  }

  public static int parseDuration(Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot) {
    // MI API delivers integer, but GUI+XML double - parse hms string first
    String dur = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "Duration/String3");
    if (StringUtils.isNotBlank(dur)) {
      try {
        String[] split = dur.split("[:.]");
        if (split.length >= 3) {
          int hours = MetadataUtil.parseInt(split[0]);
          int minutes = MetadataUtil.parseInt(split[1]);
          int seconds = MetadataUtil.parseInt(split[2]);
          return hours * 3600 + minutes * 60 + seconds;
        }
      }
      catch (Exception ignored) {
        LOGGER.trace("Could not parse duration as h:m:s: {}", dur);
      }
    }
    // fallback
    dur = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.General, 0, "Duration");
    if (StringUtils.isNotBlank(dur)) {
      try {
        double ddur = Double.parseDouble(dur);
        if (ddur > 25000) {
          return (int) (ddur / 1000f);
        }
        else {
          return (int) ddur;
        }
      }
      catch (NumberFormatException ignored) {
        LOGGER.trace("Could not parse duration as double {}", dur);
      }
    }

    return 0;
  }

  private static String parse3DFormat(MediaFile mediaFile, Map<MediaInfo.StreamKind, List<Map<String, String>>> miSnapshot) {
    int height = mediaFile.getVideoHeight();
    int width = mediaFile.getVideoWidth();

    String mvc = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "MultiView_Count");
    String video3DFormat = "";
    if (!StringUtils.isEmpty(mvc) && mvc.equals("2")) {
      video3DFormat = MediaFileHelper.VIDEO_3D;
      String mvl = getMediaInfoValue(miSnapshot, MediaInfo.StreamKind.Video, 0, "MultiView_Layout").toLowerCase(Locale.ROOT);
      LOGGER.trace("3D detected :) - {}", mvl);
      if (!StringUtils.isEmpty(mvl) && mvl.contains("top") && mvl.contains("bottom")) {
        video3DFormat = MediaFileHelper.VIDEO_3D_HTAB; // assume HalfTAB as default
        if (height > width) {
          video3DFormat = MediaFileHelper.VIDEO_3D_TAB;// FullTAB eg 1920x2160
        }
      }
      if (!StringUtils.isEmpty(mvl) && mvl.contains("side")) {
        video3DFormat = MediaFileHelper.VIDEO_3D_HSBS;// assume HalfSBS as default
        if (mediaFile.getAspectRatio() > 3) {
          video3DFormat = MediaFileHelper.VIDEO_3D_SBS;// FullSBS eg 3840x1080
        }
      }
      if (!StringUtils.isEmpty(mvl) && mvl.contains("laced")) { // Both Eyes laced in one block
        video3DFormat = MediaFileHelper.VIDEO_3D_MVC;
      }
    }
    else {
      // not detected as 3D by MI - BUT: if we've got some known resolutions, we can at least find the "full" ones ;)
      if (width == 3840 && height <= 1080) {
        video3DFormat = MediaFileHelper.VIDEO_3D_SBS;
      }
      else if (width == 1920 && height == 2160) {
        video3DFormat = MediaFileHelper.VIDEO_3D_TAB;
      }
    }

    return video3DFormat;
  }

  private static void gatherMediaInformationFromDvdFile(MediaFile mediaFile, List<MediaInfoFile> mediaInfoFiles) {

    MediaInfoFile ifo = null;
    // FIXME: since we now have multiple files, each will overwrite the former :/
    // parse VOBs first
    int videoDur = 0;
    for (MediaInfoFile mif : mediaInfoFiles) {
      mif.gatherMediaInformation();
      if (mif.getFileExtension().equalsIgnoreCase("vob")) {
        gatherVideoInformation(mediaFile, mif.getSnapshot());
        gatherAudioInformation(mediaFile, mif.getSnapshot());

        // there is no exact overall bitrate for the whole DVD, so we just take the one from the biggest VOB
        String br = getMediaInfoValue(mif.getSnapshot(), MediaInfo.StreamKind.General, 0, "OverallBitRate");
        if (!br.isEmpty()) {
          try {
            mediaFile.setOverallBitRate(Integer.parseInt(br) / 1000); // in kbps
          }
          catch (NumberFormatException e) {
            mediaFile.setOverallBitRate(0);
          }
        }
        videoDur += mif.getDuration();
      }
      else if (mif.getFileExtension().equalsIgnoreCase("ifo")) {
        ifo = mif;
      }
    }
    mediaFile.setDuration(videoDur); // accumulated, maybe its right

    if (ifo == null) {
      return; // ;(
    }

    // do IFO last
    gatherSubtitleInformation(mediaFile, ifo.getSnapshot());
    // mix in language information
    for (int i = 0; i < getAudioStreamCount(ifo.getSnapshot()); i++) {
      String id = getMediaInfoValue(ifo.getSnapshot(), MediaInfo.StreamKind.Audio, i, "StreamKindPos");

      // look for the corresponding audio stream in the media file
      MediaFileAudioStream audioStream = null;
      for (MediaFileAudioStream stream : mediaFile.getAudioStreams()) {
        if (id.equals(stream.id)) {
          audioStream = stream;
          break;
        }
      }

      if (audioStream == null) {
        // we could not find the corresponding audio stream; the IFO has some more references to audio files where no one is in the VOB..
        continue;
      }

      String language = getMediaInfoValue(ifo.getSnapshot(), MediaInfo.StreamKind.Audio, i, "Language", "Language/String");
      if (language.isEmpty()) {
        if (!mediaFile.isDiscFile()) { // video_ts parsed 'ts' as Tsonga
          // try to parse from filename
          String shortname = mediaFile.getBasename().toLowerCase(Locale.ROOT);
          audioStream.setLanguage(parseLanguageFromString(shortname));
        }
      }
      else {
        audioStream.setLanguage(parseLanguageFromString(language));
      }
    }

    // vob resets wrong duration - take from ifo
    mediaFile.setDuration(ifo.getDuration());
  }

  private static void gatherMediaInformationFromBluRayFile(MediaFile mediaFile, List<MediaInfoFile> mediaInfoFiles) {
    // FIXME: since this method is called with getRelevantFiles(), we need to take them all!

    // find the M2TS/MPLS with longest duration
    MediaInfoFile m2ts = mediaInfoFiles.stream()
        .filter(mediaInfoFile -> mediaInfoFile.getFileExtension().equals("m2ts") || mediaInfoFile.getFileExtension().equals("mpls"))
        .max(Comparator.comparingInt(MediaInfoFile::getDuration))
        .orElse(null);

    if (m2ts == null) {
      LOGGER.debug("Could not find a valid M2TS file");
      return;
    }

    LOGGER.trace("Considering M2TS file: {}", m2ts.getFilename());

    // get base information from the m2ts file
    gatherMediaInformationFromFile(mediaFile, Collections.singletonList(m2ts));

    // get additional information of the clpi file
    MediaInfoFile clpi = mediaInfoFiles.stream()
        .filter(mediaInfoFile -> mediaInfoFile.getFileExtension().equals("clpi"))
        .max(Comparator.comparingInt(MediaInfoFile::getDuration))
        .orElse(null);
    if (clpi != null) {
      if (mediaFile.getDuration() == 0) {
        mediaFile.setDuration(parseDuration(clpi.getSnapshot()));
      }

      // mix in language information (audio)
      for (int i = 0; i < getAudioStreamCount(clpi.getSnapshot()); i++) {
        String id = getMediaInfoValue(clpi.getSnapshot(), MediaInfo.StreamKind.Audio, i, "StreamKindPos");

        // look for the corresponding audio stream in the media file
        MediaFileAudioStream audioStream = null;
        for (MediaFileAudioStream stream : mediaFile.getAudioStreams()) {
          if (id.equals(stream.id)) {
            audioStream = stream;
            break;
          }
        }

        if (audioStream == null) {
          // we could not find the corresponding audio stream; the IFO has some more references to audio files where no one is in the VOB..
          continue;
        }

        String language = getMediaInfoValue(clpi.getSnapshot(), MediaInfo.StreamKind.Audio, i, "Language", "Language/String");
        if (language.isEmpty()) {
          if (!mediaFile.isDiscFile()) { // video_ts parsed 'ts' as Tsonga
            // try to parse from filename
            String shortname = mediaFile.getBasename().toLowerCase(Locale.ROOT);
            audioStream.setLanguage(parseLanguageFromString(shortname));
          }
        }
        else {
          audioStream.setLanguage(parseLanguageFromString(language));
        }
      }

      // mix in language information (subtitle)
      for (int i = 0; i < getSubtitleStreamCount(clpi.getSnapshot()); i++) {
        String id = getMediaInfoValue(clpi.getSnapshot(), MediaInfo.StreamKind.Text, i, "StreamKindPos");

        // look for the corresponding subtitle stream in the media file
        MediaFileSubtitle subtitle = null;
        for (MediaFileSubtitle stream : mediaFile.getSubtitles()) {
          if (id.equals(stream.id)) {
            subtitle = stream;
            break;
          }
        }

        if (subtitle == null) {
          // we could not find the corresponding audio stream; the IFO has some more references to audio files where no one is in the VOB..
          continue;
        }

        String language = getMediaInfoValue(clpi.getSnapshot(), MediaInfo.StreamKind.Text, i, "Language", "Language/String");
        if (language.isEmpty()) {
          if (!mediaFile.isDiscFile()) { // video_ts parsed 'ts' as Tsonga
            // try to parse from filename
            String shortname = mediaFile.getBasename().toLowerCase(Locale.ROOT);
            subtitle.setLanguage(parseLanguageFromString(shortname));
          }
        }
        else {
          subtitle.setLanguage(parseLanguageFromString(language));
        }
      }
    }

    // get additional information of the ssif file
    MediaInfoFile ssif = mediaInfoFiles.stream()
        .filter(mediaInfoFile -> mediaInfoFile.getFileExtension().equals("ssif"))
        .max(Comparator.comparingInt(MediaInfoFile::getDuration))
        .orElse(null);
    if (ssif != null) {
      if (mediaFile.getDuration() == 0) {
        mediaFile.setDuration(parseDuration(ssif.getSnapshot()));
      }

      mediaFile.setVideo3DFormat(parse3DFormat(mediaFile, ssif.getSnapshot()));
    }
  }

  private static void gatherMediaInformationFromHdDvdFile(MediaFile mediaFile, List<MediaInfoFile> mediaInfoFiles) {
    // find the EVO with longest duration
    MediaInfoFile evo = mediaInfoFiles.stream()
        .filter(mediaInfoFile -> mediaInfoFile.getFileExtension().equals("evo"))
        .max(Comparator.comparingInt(MediaInfoFile::getDuration))
        .orElse(null);

    if (evo == null) {
      LOGGER.debug("Could not find a valid EVO file");
      return;
    }

    LOGGER.trace("Considering EVO file: {}", evo.getFilename());
    gatherMediaInformationFromFile(mediaFile, Collections.singletonList(evo));
  }

  /**
   * get the main video file for the given {@link MediaFile}. Handy if the {@link MediaFile} is a disc structure<br />
   * partly taken from detectRelevantDvdFiles, detectRelevantBlurayFiles and detectRelevantHdDvdFiles
   *
   * @param mediaFile
   *          the {@link MediaFile} to check
   * @return the {@link Path} to the main video file
   */
  public static Path getMainVideoFile(MediaFile mediaFile) {
    // extracted DISC folder - no MFs inside TMM
    if (Files.isDirectory(mediaFile.getFileAsPath())) {
      // looks like a disc structure
      List<MediaInfoFile> mediaInfoFiles = new ArrayList<>();
      for (Path path : Utils.listFilesRecursive(mediaFile.getFileAsPath())) {
        try {
          mediaInfoFiles.add(new MediaInfoFile(path, Files.size(path)));
        }
        catch (Exception e) {
          LOGGER.debug("could not parse filesize of {} - {}", path, e.getMessage());
        }
      }
      mediaInfoFiles = detectRelevantFiles(mediaInfoFiles);

      if (!mediaInfoFiles.isEmpty()) {
        // take the first which is > 200 mb
        MediaInfoFile mediaInfoFile = mediaInfoFiles.stream().filter(file -> file.getFilesize() > 200000000).findFirst().orElse(null);
        if (mediaInfoFile == null) {
          mediaInfoFile = mediaInfoFiles.get(0);
        }
        Path path = Paths.get(mediaInfoFile.getPath(), mediaInfoFile.getFilename());
        if (Files.exists(path)) {
          return path;
        }
      }
    }

    return mediaFile.getFileAsPath();
  }

  /**
   * Try to parse the language out of the filename. This happens (like in Kodi) to chop the filename into different chunks and search in the chunks
   * for possible language tags.<br />
   * To make this work flawless we need to chop out the "main" filename part (movie/episode video filename) and look into the rest. For this we need
   * to pass the basename of the main video file to this method too.<br />
   * This is only usable for audio and subtitle files
   *
   * @param basename
   *          the basename of {@link MediaFile} to work with (no extension!)
   * @param commonPart
   *          the common part of the filename which is shared with the video file
   */
  public static MediaStreamInfo gatherLanguageInformation(String basename, String commonPart) {
    String shortname = basename.replace(commonPart, "");
    String language = "";
    String title = "";
    List<Flags> flags = new ArrayList<>();

    // FIRST - remove known flags
    boolean hicc = false;
    if (shortname.contains("forced")) {
      flags.add(Flags.FLAG_FORCED);
      shortname = shortname.replaceAll("[\\p{Punct} ]+forced", ""); // punct + space
    }
    if (shortname.contains("sdh")) {
      flags.add(Flags.FLAG_HEARING_IMPAIRED);
      shortname = shortname.replaceAll("[\\p{Punct} ]+sdh", ""); // punct + space
    }
    if (shortname.contains("cc") || shortname.contains("hi")) {
      // remember this - if we don't find other langu, this IS the langu (and not a flag)
      hicc = true;
    }

    // SECOND - try to find country-lang Locales ONLY
    String[] splitted = shortname.split("[\\p{Punct} &&[^_-]]"); // punctuation (+space), but WITHOUT dash and underscore
    for (String token : splitted) {
      // does not work, if it is ALSO delimited with dash like movie-es-419
      // lets assume, the language token MUST be exact 2 chars
      if (!token.matches("[a-zA-Z][a-zA-Z][_-].*")) {
        token = token.replaceFirst("[^_-]+[_-]", ""); // remove all up to first delimiter
      }

      // now?
      if (token.matches("[a-zA-Z][a-zA-Z][_-].*")) {
        // if WE dont have it in our array, it is some xx_YY locale, but not valid
        if (LanguageUtils.KEY_TO_LOCALE_MAP.containsKey(token.toLowerCase())) {
          language = token;
          break; // step out
        }
        // try with replace (zh_hant -> zh-hant)
        if (LanguageUtils.KEY_TO_LOCALE_MAP.containsKey(token.toLowerCase().replace("_", "-"))) {
          language = token;
          break; // step out
        }
      }
    }

    // THIRD - tokenize even more
    if (language.isEmpty()) {
      // look with the whole term first
      String languWoHiCC = "";
      if (hicc) {
        languWoHiCC = shortname;
        languWoHiCC = languWoHiCC.replaceAll("\\p{Punct}+hi", "");
        languWoHiCC = languWoHiCC.replaceAll("\\p{Punct}+cc", "");
        languWoHiCC = LanguageUtils.findLanguageInString(languWoHiCC);
      }
      if (StringUtils.isNotBlank(languWoHiCC)) {
        // found trailing language code - just need to remove it from the title
        language = languWoHiCC; // LanguageUtils.getIso3LanguageFromLocalizedString(foundToken);
        // no title, since we parsed the whole string with langu at end!
      }
      else {
        // split the shortname into chunks
        List<String> chunks = ParserUtils.splitByPunctuation(shortname);
        int languageIndex = -1;
        // parse forward, since language is more significant
        for (int i = 0; i < chunks.size(); i++) {
          if (chunks.get(i).equalsIgnoreCase("und")) {
            // we need to keep "und" as undefined, but valid language within our array.
            // but we must ignore that at some places, since this is a very common German word
            // FIXME: there are more tokens, like "xxx Die_Tam-Turbulenzen.mp4 -> tam no langu!
            continue;
          }
          language = LanguageUtils.findLanguageInString(chunks.get(i));
          if (StringUtils.isNotBlank(language)) {
            languageIndex = i;
            break;
          }
        }
        if (hicc) {
          if (!language.equalsIgnoreCase("hi")) {

            // remove CC
            int poscc = chunks.indexOf("cc");
            if (poscc > 0 && poscc < languageIndex) {
              languageIndex--; // just decrement by one, since we removed a former tag
            }
            else {
              languageIndex = poscc; // langu not yet set
            }
            chunks.remove("cc");

            // remove HI
            int poshi = chunks.indexOf("hi");
            if (poshi > 0 && poshi < languageIndex) {
              languageIndex--; // just decrement by one, since we removed a former tag
            }
            chunks.remove("hi");

            // set flag
            flags.add(Flags.FLAG_HEARING_IMPAIRED);

            // languageIndex++; // increment index to be after langu
          }
          else {
            // HI langu
            chunks.remove("hi");
            if (chunks.indexOf("cc") >= languageIndex) {
              languageIndex = chunks.indexOf("cc"); // for title
              // CC is no language code, but a country!
              // if CC present and AFTER language, it is a flag to be removed.
              // if it is BEFORE language, it might be from filename/movie title - keep
              chunks.remove("cc");
              flags.add(Flags.FLAG_HEARING_IMPAIRED);
            }
          }
        }
        else {
          // no hicc found (but langu) - increment to next pos
          if (languageIndex >= 0) {
            languageIndex++;
          }
        }
        if (languageIndex < 0 && !hicc) {
          // no langu AND no hicc?
          title = "";
        }
        else if (languageIndex >= 0 && languageIndex < chunks.size()) {
          // the language index was not the last chunk. Save the part between the language index and the last chunk as title
          title = String.join(" ", chunks.subList(languageIndex, chunks.size()));
        }
      }
    }
    else {
      // we already found our language, just check for title
      int pos = shortname.indexOf(language) + language.length(); // end
      if (pos + 1 <= shortname.length()) {
        title = shortname.substring(pos + 1); // plus delimiter
      }
    }
    title = title.strip();

    MediaStreamInfo info = new MediaStreamInfo();
    info.setLanguage(language);
    info.setTitle(title);
    info.set(flags);

    return info;
  }
}
