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

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.security.CodeSource;
import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.translate.UnicodeUnescaper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.scraper.util.UrlUtil;

/**
 * The Class Utils.
 *
 * @author Manuel Laggner / Myron Boyle
 */
public class Utils {
  private static final Logger       LOGGER                      = LoggerFactory.getLogger(Utils.class);
  private static final Pattern      localePattern               = Pattern.compile("messages_(.{2})_?(.{2,4})?\\.properties",
      Pattern.CASE_INSENSITIVE);

  // <cd/dvd/part/pt/disk/disc><0-N>
  private static final Pattern      stackingPattern1            = Pattern.compile("(.*)[ _.-]+((?:cd|dvd|p(?:ar)?t|dis[ck])[1-9][0-9]?)([ _.-].+)$",
      Pattern.CASE_INSENSITIVE);
  // same as above, but SOLELY as name like "disc1.iso"
  private static final Pattern      stackingPattern1a           = Pattern.compile("((?:cd|dvd|p(?:ar)?t|dis[ck])[1-9][0-9]?)([ _.-].+)$",
      Pattern.CASE_INSENSITIVE);

  // <cd/dvd/part/pt/disk/disc><a-d>
  private static final Pattern      stackingPattern2            = Pattern.compile("(.*)[ _.-]+((?:cd|dvd|p(?:ar)?t|dis[ck])[a-d])([ _.-].+)$",
      Pattern.CASE_INSENSITIVE);

  // moviename-a.avi // modified mandatory delimiter (but no space), and A-D must be at end!
  private static final Pattern      stackingPattern3            = Pattern.compile("(.*?)[_.-]+([a-d])(\\.[^.]+)$", Pattern.CASE_INSENSITIVE);

  // moviename-1of2.avi, moviename-1 of 2.avi
  private static final Pattern      stackingPattern4            = Pattern
      .compile("(.*?)[ (_.-]+([1-9][0-9]?[ .]?of[ .]?[1-9][0-9]?)[ )_-]?([ _.-].+)$", Pattern.CASE_INSENSITIVE);

  // folder stacking marker <cd/dvd/part/pt/disk/disc><0-N> - must be last part
  private static final Pattern      folderStackingPattern       = Pattern.compile("(.*?)[ _.-]*((?:cd|dvd|p(?:ar)?t|dis[ck])[1-9][0-9]?)$",
      Pattern.CASE_INSENSITIVE);

  // illegal file name characters
  private static final char[]       ILLEGAL_FILENAME_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"',
      ':' };

  // pattern for matching youtube links
  public static final Pattern       YOUTUBE_PATTERN             = Pattern
      .compile("^((?:https?:)?\\/\\/)?((?:www|m)\\.)?((?:youtube\\.com|youtu.be))(\\/(?:[\\w\\-]+\\?v=|embed\\/|v\\/)?)([\\w\\-]+)(\\S+)?$");

  public static final Pattern       SEASON_NFO_PATTERN          = Pattern.compile("^season(\\d{2,}|\\-specials)?\\.nfo$", Pattern.CASE_INSENSITIVE);
  public static final String        DISC_FOLDER_REGEX           = "(?i)(VIDEO_TS|BDMV|HVDVD_TS)$";

  private static final List<Locale> AVAILABLE_LOCALES           = new ArrayList<>();

  private static String             tempFolder;

  static {
    // get the systems default temp folder
    try {
      String temp = System.getProperty("java.io.tmpdir");
      Path tempFolder = Paths.get(temp);
      if (Files.exists(tempFolder) && Files.isWritable(tempFolder)) {
        // create a subfolder for tmm
        tempFolder = Paths.get(temp, "tmm");
        if (!Files.exists(tempFolder)) {
          Files.createDirectories(tempFolder);
        }
        if (Files.exists(tempFolder) && Files.isWritable(tempFolder)) {
          Utils.tempFolder = tempFolder.toAbsolutePath().toString();
        }
        else {
          Utils.tempFolder = temp;
        }
      }
      else {
        Utils.tempFolder = "tmp";
      }
    }
    catch (Exception | Error ignored) {
      if (tempFolder != null) {
        tempFolder = "tmp";
      }
    }
  }

  private Utils() {
    // hide public constructor for utility classes
  }

  /**
   * gets the filename part, and returns last extension
   *
   * @param path
   *          the path to get the last extension for
   * @return the last extension found
   */
  public static String getExtension(Path path) {
    String ext = "";
    String fn = path.getFileName().toString();
    int i = fn.lastIndexOf('.');
    if (i > 0) {
      ext = fn.substring(i + 1);
    }
    return ext;
  }

  /**
   * this is the TMM variant of isRegularFiles()<br>
   * because deduplication creates windows junction points, we check here if it is<br>
   * not a directory, and either a regular file or "other" one.<br>
   * see http://serverfault.com/a/667220
   *
   * @param file
   * @return
   */
  public static boolean isRegularFile(Path file) {
    try {
      BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
      return isRegularFile(attr);
    }
    catch (IOException e) {
      // maybe this is a "broken" symlink -> just try to read the link itself
      try {
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return isRegularFile(attr);
      }
      catch (IOException e1) {
        LOGGER.warn("Could not read BasicFileAttributes: {}", e1);
        return false;
      }
    }
  }

  /**
   * this is the TMM variant of isRegularFiles()<br>
   * because deduplication creates windows junction points, we check here if it is<br>
   * not a directory, and either a regular file or "other" one.<br>
   * see http://serverfault.com/a/667220<br>
   * <br>
   * <b>Changed to only check for NOT DIRECTORY, treating all others as regular</b>
   *
   * @param attr
   * @return
   */
  public static boolean isRegularFile(BasicFileAttributes attr) {
    // see windows impl http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/7u40-b43/sun/nio/fs/WindowsFileAttributes.java#451
    // symbolic link is used in git annex
    // return (attr.isRegularFile() || attr.isOther() || attr.isSymbolicLink()) && !attr.isDirectory();

    // JVM isRegular() checks for !sym, !dir, !other
    // we add isSym & isOther
    // the only portion what's left is the !dir - should be enough to check only for that ;)
    return !attr.isDirectory();
  }

  /**
   * returns the relative path of 2 absolute file paths<br>
   * "/a/b & /a/b/c/d -> c/d
   *
   * @param parent
   *          the directory
   * @param child
   *          the subdirectory
   * @return relative path
   */
  public static String relPath(String parent, String child) {
    return relPath(Paths.get(parent), Paths.get(child));
  }

  /**
   * returns the relative path of 2 absolute file paths<br>
   * "/a/b & /a/b/c/d -> c/d
   *
   * @param parent
   *          the directory
   * @param child
   *          the subdirectory
   * @return relative path
   */
  public static String relPath(Path parent, Path child) {
    return parent.relativize(child).toString();
  }

  /**
   * gets a list of all datasources, sorted by the "longest" first
   *
   * @return
   */
  public static List<String> getAllDatasources() {
    List<String> ret = new ArrayList<>();

    for (String m : MovieModuleManager.getInstance().getSettings().getMovieDataSource()) {
      ret.add(m);
    }
    for (String t : TvShowModuleManager.getInstance().getSettings().getTvShowDataSource()) {
      ret.add(t);
    }

    // sort by length (longest first)
    Comparator<String> c = Comparator.comparingInt(String::length);
    Collections.sort(ret, c);
    Collections.reverse(ret);

    return ret;
  }

  /**
   * Returns the sortable variant of title/originaltitle<br>
   * eg "The Bourne Legacy" -> "Bourne Legacy, The".
   *
   * @param title
   *          the title
   * @return the title/originaltitle in its sortable format
   */
  public static String getSortableName(String title) {
    if (StringUtils.isBlank(title)) {
      return "";
    }
    if (title.startsWith("LA ")) {
      // means Los Angeles
      return title;
    }

    if (title.toLowerCase(Locale.ROOT).matches("^die hard$") || title.toLowerCase(Locale.ROOT).matches("^die hard[:\\s].*")) {
      return title;
    }

    if (title.toLowerCase(Locale.ROOT).matches("^die another day$") || title.toLowerCase(Locale.ROOT).matches("^die another day[:\\s].*")) {
      return title;
    }

    for (String prfx : Settings.getInstance().getTitlePrefix()) {
      String delim = "\\s+"; // one or more spaces needed
      if (prfx.matches(".*['`´]$")) { // ends with hand-picked delim, so no space might be possible
        delim = "";
      }

      // only move the first found prefix
      if (title.matches("(?i)^" + Pattern.quote(prfx) + delim + "(.*)")) {
        title = title.replaceAll("(?i)^" + Pattern.quote(prfx) + delim + "(.*)", "$1, " + prfx);
        break;
      }
    }

    return title.strip();
  }

  /**
   * Returns the common name of title/originaltitle when it is named sortable<br>
   * eg "Bourne Legacy, The" -> "The Bourne Legacy".
   *
   * @param title
   *          the title
   * @return the original title
   */
  public static String removeSortableName(String title) {
    if (title == null || title.isEmpty()) {
      return "";
    }
    for (String prfx : Settings.getInstance().getTitlePrefix()) {
      String delim = " "; // one spaces as delim
      if (prfx.matches(".*['`´]$")) { // ends with hand-picked delim, so no space between prefix and title
        delim = "";
      }
      title = title.replaceAll("(?i)(.*), " + Pattern.quote(prfx) + "$", prfx + delim + "$1");
    }
    return title.strip();
  }

  /**
   * Clean stacking markers.<br>
   * Same logic as detection, but just returning string w/o
   *
   * @param filename
   *          the filename WITH extension
   * @return the string
   */
  public static String cleanStackingMarkers(String filename) {
    if (!StringUtils.isEmpty(filename)) {
      // see http://kodi.wiki/view/Advancedsettings.xml#moviestacking
      // basically returning <regexp>(Title)(Stacking)(Ignore)(Extension)</regexp>

      // <cd/dvd/part/pt/disk/disc> <0-N>
      Matcher m = stackingPattern1.matcher(filename);
      if (m.matches()) {
        return m.group(1) + m.group(3); // just return String w/o stacking
      }
      m = stackingPattern1a.matcher(filename);
      if (m.matches()) {
        return m.group(2); // just return postfix, as we started with stacking 2=3
      }

      // <cd/dvd/part/pt/disk/disc> <a-d>
      m = stackingPattern2.matcher(filename);
      if (m.matches()) {
        return m.group(1) + m.group(3); // just return String w/o stacking
      }

      // moviename-2.avi // modified mandatory delimiter, and AD must be at end!
      m = stackingPattern3.matcher(filename);
      if (m.matches()) {
        return m.group(1) + m.group(3); // just return String w/o stacking
      }

      // moviename-1of2.avi, moviename-1 of 2.avi
      m = stackingPattern4.matcher(filename);
      if (m.matches()) {
        return m.group(1) + m.group(3); // just return String w/o stacking
      }
    }
    return filename; // no cleanup, return 1:1
  }

  /**
   * Clean stacking markers.<br>
   * Same logic as detection, but just returning string w/o
   *
   * @param filename
   *          the filename WITH extension
   * @return the string
   */
  public static String cleanFolderStackingMarkers(String filename) {
    if (!StringUtils.isEmpty(filename)) {
      Matcher m = folderStackingPattern.matcher(filename);
      if (m.matches()) {
        return m.group(1); // just return String w/o stacking
      }
    }
    return filename;
  }

  /**
   * Returns the stacking information from FOLDER name
   *
   * @param filename
   *          the filename
   * @return the stacking information
   */
  public static String getFolderStackingMarker(String filename) {
    if (!StringUtils.isEmpty(filename)) {
      // see http://kodi.wiki/view/Advancedsettings.xml#moviestacking
      // basically returning <regexp>(Title)(Volume)(Ignore)(Extension)</regexp>

      // <cd/dvd/part/pt/disk/disc> <0-N> // FIXME: check for first delimiter (optional/mandatory)!
      Matcher m = folderStackingPattern.matcher(filename);
      if (m.matches()) {
        return m.group(2);
      }
    }
    return "";
  }

  /**
   * Returns the stacking information from filename
   *
   * @param filename
   *          the filename
   * @return the stacking information
   */
  public static String getStackingMarker(String filename) {
    if (!StringUtils.isEmpty(filename)) {
      // see http://kodi.wiki/view/Advancedsettings.xml#moviestacking
      // basically returning <regexp>(Title)(Stacking)(Ignore)(Extension)</regexp>

      // <cd/dvd/part/pt/disk/disc> <0-N>
      Matcher m = stackingPattern1.matcher(filename);
      if (m.matches()) {
        return m.group(2);
      }
      m = stackingPattern1a.matcher(filename);
      if (m.matches()) {
        return m.group(1); // we start with stacking param, so 2=1
      }

      // <cd/dvd/part/pt/disk/disc> <a-d>
      m = stackingPattern2.matcher(filename);
      if (m.matches()) {
        return m.group(2);
      }

      // moviename-a.avi // modified mandatory delimiter, and AD must be at end!
      m = stackingPattern3.matcher(filename);
      if (m.matches()) {
        return m.group(2);
      }

      // moviename-1of2.avi, moviename-1 of 2.avi
      m = stackingPattern4.matcher(filename);
      if (m.matches()) {
        return m.group(2);
      }
    }
    return "";
  }

  public static String substr(String str, String pattern) {
    Pattern regex = Pattern.compile(pattern);
    Matcher m = regex.matcher(str);
    if (m.find()) {
      return m.group(1);
    }
    else {
      return "";
    }
  }

  /**
   * Returns the stacking prefix
   *
   * @param filename
   *          the filename
   * @return the stacking prefix - might be empty
   */
  public static String getStackingPrefix(String filename) {
    String stack = getStackingMarker(filename).replaceAll("[0-9]", "");
    if (stack.length() == 1 || stack.contains("of")) {
      // A-D and (X of Y) - no prefix here
      stack = "";
    }
    return stack;
  }

  /**
   * Returns the stacking information from filename
   *
   * @param filename
   *          the filename
   * @return the stacking information
   */
  public static int getStackingNumber(String filename) {
    if (!StringUtils.isEmpty(filename)) {
      String stack = getStackingMarker(filename);
      if (!stack.isEmpty()) {
        if (stack.equalsIgnoreCase("a")) {
          return 1;
        }
        else if (stack.equalsIgnoreCase("b")) {
          return 2;
        }
        else if (stack.equalsIgnoreCase("c")) {
          return 3;
        }
        else if (stack.equalsIgnoreCase("d")) {
          return 4;
        }
        if (stack.contains("of")) {
          stack = stack.replaceAll("of.*", ""); // strip all after "of", so we have the first number
        }

        try {
          return Integer.parseInt(stack.replaceAll("[^0-9]", ""));
        }
        catch (Exception e) {
          return 0;
        }
      }
    }
    return 0;
  }

  /**
   * Unquote.
   *
   * @param str
   *          the str
   * @return the string
   */
  public static String unquote(String str) {
    if (str == null)
      return null;
    return str.replaceFirst("^\\\"(.*)\\\"$", "$1");
  }

  /**
   * gets the UTF-8 encoded System property.
   *
   * @param prop
   *          the property to fetch
   * @return the enc prop
   */
  private static String getEncProp(String prop) {
    String property = System.getProperty(prop);
    if (StringUtils.isBlank(property)) {
      return "";
    }

    return URLEncoder.encode(property, StandardCharsets.UTF_8);
  }

  public static void removeEmptyStringsFromList(List<String> list) {
    List<String> toFilter = list.stream().filter(StringUtils::isBlank).toList();
    list.removeAll(toFilter);
  }

  public static void removeDuplicateStringFromCollectionIgnoreCase(Collection<String> original) {
    // 1. remove duplicates
    Set<String> items = new LinkedHashSet<>(original);

    // 2. remove case insensitive duplicates
    Set<String> check = new HashSet<>();
    List<String> toRemove = new ArrayList<>();

    original.forEach(item -> {
      String upper = item.toUpperCase(Locale.ROOT);
      if (check.contains(upper)) {
        toRemove.add(item);
      }
      else {
        check.add(upper);
      }
    });

    toRemove.forEach(items::remove);

    // 3. re-add surviving entries
    original.clear();
    original.addAll(items);
  }

  /**
   * replaces a string with placeholder ({}) with the string from the replacement array the strings in the replacement array have to be in the same
   * order as the placeholder in the source string
   *
   * @param source
   *          string
   * @param replacements
   *          array
   * @return replaced string
   */
  public static String replacePlaceholders(String source, String[] replacements) {
    String result = source;
    int index = 0;

    Pattern pattern = Pattern.compile("\\{\\}");
    while (true) {
      Matcher matcher = pattern.matcher(result);
      if (matcher.find()) {
        try {
          if (replacements.length > index) {
            // we need the UnicodeUnescaper, because StringEscapeUtils.escapeJava translated unicode
            // https://stackoverflow.com/questions/59280607/stringescapeutils-not-handling-utf-8
            result = result.replaceFirst(pattern.pattern(), new UnicodeUnescaper().translate(StringEscapeUtils.escapeJava(replacements[index])));
          }
          else {
            result = result.replaceFirst(pattern.pattern(), "");
          }
        }
        catch (Exception e) {
          result = result.replaceFirst(pattern.pattern(), "");
        }
        index++;
      }
      else {
        break;
      }
    }
    return StrgUtils.removeDuplicateWhitespace(result);
  }

  /**
   * modified version of commons-io FileUtils.moveDirectory(); adapted to Java 7 NIO<br>
   * since renameTo() might not work in first place, retry it up to 5 times.<br>
   * (better wait 5 sec for success, than always copying a 50gig directory ;)<br>
   * <b>And NO, we're NOT doing a copy+delete as fallback!</b>
   *
   * @param srcDir
   *          the directory to be moved
   * @param destDir
   *          the destination directory
   * @return true, if successful
   * @throws IOException
   *           if an IO error occurs moving the file
   */
  public static boolean moveDirectorySafe(Path srcDir, Path destDir) throws IOException {
    // rip-off from
    // http://svn.apache.org/repos/asf/commons/proper/io/trunk/src/main/java/org/apache/commons/io/FileUtils.java
    if (srcDir == null) {
      throw new NullPointerException("Source must not be null"); // NOSONAR
    }
    if (destDir == null) {
      throw new NullPointerException("Destination must not be null"); // NOSONAR
    }
    if (!srcDir.toAbsolutePath().toString().equals(destDir.toAbsolutePath().toString())) {
      LOGGER.debug("try to move folder {} to {}", srcDir, destDir);
      if (!Files.isDirectory(srcDir)) {
        throw new FileNotFoundException("Source '{}" + srcDir + "' does not exist, or is not a directory"); // NOSONAR
      }
      if (Files.exists(destDir) && !Files.isSameFile(destDir, srcDir)) {
        // extra check for Windows/OSX, where the File.equals is case-insensitive
        // so we know now, that the Dir is the same, but the absolute name does not match
        throw new FileExistsException("Destination '" + destDir + "' already exists"); // NOSONAR
      }
      if (destDir.getParent() != null && !Files.exists(destDir.getParent())) {
        // create parent folder structure, else renameTo does not work
        // NULL parent means, that we just have one relative folder like Paths.get("bla") - no need to create anything
        try {
          Files.createDirectories(destDir.getParent());
        }
        catch (AccessDeniedException e) {
          // propagate to UI by logging with error
          LOGGER.error("ACCESS DENIED (create folder) - '{}'", e.getMessage());
          // but we try a move anyway...
        }
        catch (Exception e) {
          LOGGER.error("could not create directory structure {}", destDir.getParent());
          // but we try a move anyway...
        }
      }

      // rename folder; try 5 times and wait a sec
      boolean rename = false;
      for (int i = 0; i < 5; i++) {
        try {
          // need atomic fs move for changing cASE
          Files.move(srcDir, destDir, StandardCopyOption.ATOMIC_MOVE);
          rename = true;// no exception
        }
        catch (AccessDeniedException e) {
          // propagate to UI by logging with error
          LOGGER.error("ACCESS DENIED (move folder) - '{}'", e.getMessage());
          break;
        }
        catch (AtomicMoveNotSupportedException a) {
          // if it fails (b/c not on same file system) use that; original documentation
          /*
           * When moving a directory requires that its entries be moved then this method fails (by throwing an {@code IOException}). To move a <i>file
           * tree</i> may involve copying rather than moving directories and this can be done using the {@link #copy copy} method in conjunction with
           * the {@link #walkFileTree Files.walkFileTree} utility method.
           */
          // in this case we do a recursive copy & delete
          // copy all files (with re-creating symbolic links if there are some)
          try (Stream<Path> stream = Files.walk(srcDir)) {
            Iterator<Path> srcFiles = stream.iterator();
            while (srcFiles.hasNext()) {
              Path source = srcFiles.next();
              Path destination = destDir.resolve(srcDir.relativize(source));
              if (Files.isSymbolicLink(source)) {
                Files.createSymbolicLink(destination, source.toRealPath());
                continue;
              }
              if (Files.isDirectory(source)) {
                if (!Files.exists(destination)) {
                  Files.createDirectory(destination);
                }
                continue;
              }
              Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
              fixDateAttributes(source, destination);
            }

            // delete source files
            Utils.deleteDirectoryRecursive(srcDir);
            rename = true;
          }
          catch (AccessDeniedException e) {
            // propagate to UI by logging with error
            LOGGER.error("ACCESS DENIED (move folder) - '{}'", e.getMessage());
            break;
          }
          catch (IOException e) {
            LOGGER.warn("rename problem (fallback): {}", e.getMessage()); // NOSONAR
          }
        }
        catch (IOException e) {
          LOGGER.warn("rename problem: {}", e.getMessage()); // NOSONAR
        }
        if (rename) {
          break; // ok it worked, step out
        }
        try {
          LOGGER.debug("rename did not work - sleep a while and try again..."); // NOSONAR
          Thread.sleep(1000);
        }
        catch (InterruptedException e) { // NOSONAR
          // we will not let the JVM abort the thread here -> just finish the logic without waiting any longer
          LOGGER.warn("I'm so excited - could not sleep"); // NOSONAR
          break;
        }
      }

      // ok, we tried it 5 times - it still seems to be locked somehow. Continue
      // with copying as fallback
      // NOOO - we don't like to have some files copied and some not.

      if (!rename) {
        LOGGER.error("Failed to rename directory {} to {}", srcDir, destDir);
        LOGGER.error("Renaming aborted.");
        MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, srcDir, "message.renamer.failedrename")); // NOSONAR
        return false;
      }
      else {
        LOGGER.info("Successfully moved folder {} to {}", srcDir, destDir);
        return true;
      }
    }
    return true; // dir are equal
  }

  private static void fixDateAttributes(Path source, Path destination) {
    // fix date attributes in Windows due to _incomplete_ system operations
    // see https://stackoverflow.com/a/58205668
    if (SystemUtils.IS_OS_WINDOWS) {
      try {
        BasicFileAttributes srcAttrs = Files.readAttributes(source, BasicFileAttributes.class);
        BasicFileAttributeView tgtView = Files.getFileAttributeView(destination, BasicFileAttributeView.class);

        tgtView.setTimes(srcAttrs.lastModifiedTime(), srcAttrs.lastAccessTime(), srcAttrs.creationTime());
      }
      catch (Exception e) {
        LOGGER.trace("could not set date attributes for '{}' - '{}'", destination, e.getMessage());
      }
    }
  }

  /**
   * modified version of commons-io FileUtils.moveFile(); adapted to Java 7 NIO<br>
   * since renameTo() might not work in first place, retry it up to 5 times.<br>
   * (better wait 5 sec for success, than always copying a 50gig directory ;)<br>
   * <b>And NO, we're NOT doing a copy+delete as fallback!</b>
   *
   * @param srcFile
   *          the file to be moved
   * @param destFile
   *          the destination file
   * @throws NullPointerException
   *           if source or destination is {@code null}
   * @throws FileExistsException
   *           if the destination file exists
   * @throws IOException
   *           if source or destination is invalid
   * @throws IOException
   *           if an IO error occurs moving the file
   */
  public static boolean moveFileSafe(final Path srcFile, final Path destFile) throws IOException {
    if (srcFile == null) {
      throw new NullPointerException("Source must not be null");
    }
    if (destFile == null) {
      throw new NullPointerException("Destination must not be null");
    }
    if (!srcFile.toAbsolutePath().toString().equals(destFile.toAbsolutePath().toString())) {
      LOGGER.debug("try to move file '{}' to '{}'", srcFile, destFile);
      if (!Files.exists(srcFile)) {
        // allow moving of symlinks
        // https://github.com/tinyMediaManager/tinyMediaManager/issues/410
        if (!Files.isSymbolicLink(srcFile)) {
          throw new FileNotFoundException("Source '" + srcFile + "' does not exist"); // NOSONAR
        }
      }
      if (Files.isDirectory(srcFile)) {
        throw new IOException("Source '" + srcFile + "' is a directory"); // NOSONAR
      }
      if (Files.exists(destFile) && !Files.isSameFile(destFile, srcFile)) {
        // extra check for windows, where the File.equals is case insensitive
        // so we know now, that the File is the same, but the absolute name does not match
        throw new FileExistsException("Destination '" + destFile + "' already exists");
      }
      if (Files.isDirectory(destFile)) {
        throw new IOException("Destination '" + destFile + "' is a directory");
      }

      // rename folder; try 5 times and wait a sec
      boolean rename = false;
      for (int i = 0; i < 5; i++) {
        try {
          // need atomic fs move for changing cASE
          Files.move(srcFile, destFile, StandardCopyOption.ATOMIC_MOVE);
          rename = true;// no exception
        }
        catch (AccessDeniedException e) {
          // propagate to UI by logging with error
          LOGGER.error("ACCESS DENIED (move file) - '{}'", e.getMessage());
          break;
        }
        catch (AtomicMoveNotSupportedException a) {
          // if it fails (b/c not on same file system) use that
          try {
            Files.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
            fixDateAttributes(srcFile, destFile);
            Files.delete(srcFile);
            rename = true; // no exception
          }
          catch (AccessDeniedException e) {
            // propagate to UI by logging with error
            LOGGER.error("ACCESS DENIED (move file) - '{}'", e.getMessage());
            break;
          }
          catch (IOException e) {
            LOGGER.warn("rename problem (fallback): '{}' - '{}'", e.getClass().getSimpleName(), e.getMessage()); // NOSONAR
          }
        }
        catch (IOException e) {
          LOGGER.warn("rename problem: '{}' - '{}'", e.getClass().getSimpleName(), e.getMessage()); // NOSONAR
        }
        if (rename) {
          break; // ok it worked, step out
        }
        try {
          LOGGER.debug("rename did not work - sleep a while and try again...");
          Thread.sleep(1000);
        }
        catch (InterruptedException e) { // NOSONAR
          // we will not let the JVM abort the thread here -> just finish the logic without waiting any longer
          LOGGER.warn("I'm so excited - could not sleep");
          break;
        }
      }

      if (!rename) {
        LOGGER.error("Failed to rename file '{}' to '{}'", srcFile, destFile);
        MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, srcFile, "message.renamer.failedrename")); // NOSONAR
        return false;
      }
      else {
        LOGGER.debug("Successfully moved file from '{}' to '{}'", srcFile, destFile);
        return true;
      }
    }
    return true; // files are equal
  }

  /**
   * copy a file, preserving the attributes, but NOT overwrite it
   *
   * @param srcFile
   *          the file to be copied
   * @param destFile
   *          the target
   * @return true/false
   * @throws NullPointerException
   *           if source or destination is {@code null}
   * @throws FileExistsException
   *           if the destination file exists
   * @throws IOException
   *           if source or destination is invalid
   * @throws IOException
   *           if an IO error occurs moving the file
   */
  public static boolean copyFileSafe(final Path srcFile, final Path destFile) throws IOException {
    return copyFileSafe(srcFile, destFile, false);
  }

  /**
   * copy a file, preserving the attributes
   *
   * @param srcFile
   *          the file to be copied
   * @param destFile
   *          the target
   * @param overwrite
   *          overwrite the target?
   * @return true/false
   * @throws NullPointerException
   *           if source or destination is {@code null}
   * @throws FileExistsException
   *           if the destination file exists
   * @throws IOException
   *           if source or destination is invalid
   * @throws IOException
   *           if an IO error occurs moving the file
   */
  public static boolean copyFileSafe(final Path srcFile, final Path destFile, boolean overwrite) throws IOException {
    if (srcFile == null) {
      throw new NullPointerException("Source must not be null");
    }
    if (destFile == null) {
      throw new NullPointerException("Destination must not be null");
    }
    if (!srcFile.toAbsolutePath().toString().equals(destFile.toAbsolutePath().toString())) {
      LOGGER.debug("try to copy file {} to {}", srcFile, destFile);
      if (!Files.exists(srcFile)) {
        LOGGER.debug("file not found - '{}'", srcFile);
        throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
      }
      if (Files.isDirectory(srcFile)) {
        LOGGER.debug("source is a directory - '{}'", srcFile);
        throw new IOException("Source '" + srcFile + "' is a directory");
      }
      if (!overwrite) {
        if (Files.exists(destFile) && !Files.isSameFile(destFile, srcFile)) {
          // extra check for windows, where the File.equals is case-insensitive
          // so we know now, that the File is the same, but the absolute name does not match
          LOGGER.debug("destination exists - '{}'", destFile);
          throw new FileExistsException("Destination '" + destFile + "' already exists");
        }
      }
      if (Files.isDirectory(destFile)) {
        LOGGER.debug("destination is a directory - '{}'", destFile);
        throw new IOException("Destination '" + destFile + "' is a directory");
      }

      // rename folder; try 5 times and wait a sec
      boolean rename = false;
      for (int i = 0; i < 5; i++) {
        try {
          // replace existing for changing cASE
          Files.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
          fixDateAttributes(srcFile, destFile);
          rename = true;// no exception
        }
        catch (AccessDeniedException e) {
          // propagate to UI by logging with error
          LOGGER.error("ACCESS DENIED (copy file) - '{}'", e.getMessage());
          break;
        }
        catch (UnsupportedOperationException u) {
          // maybe copy with attributes does not work here (across file systems), just try without file attributes
          try {
            // replace existing for changing cASE
            Files.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
            fixDateAttributes(srcFile, destFile);
            rename = true;// no exception
          }
          catch (AccessDeniedException e) {
            // propagate to UI by logging with error
            LOGGER.error("ACCESS DENIED (copy file) - '{}'", e.getMessage());
            break;
          }
          catch (IOException e) {
            LOGGER.warn("copy did not work (fallback): {}", e.getMessage());
          }
        }
        catch (IOException e) {
          LOGGER.warn("copy did not work: {}", e.getMessage());
        }

        if (rename) {
          break; // ok it worked, step out
        }
        try {
          LOGGER.debug("copy did not work - sleep a while and try again..."); // NOSONAR
          Thread.sleep(1000);
        }
        catch (InterruptedException e) { // NOSONAR
          // we will not let the JVM abort the thread here -> just finish the logic without waiting any longer
          LOGGER.warn("I'm so excited - could not sleep"); // NOSONAR
          break;
        }
      }

      if (!rename) {
        LOGGER.error("Failed to copy file {} to {}", srcFile, destFile);
        MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, srcFile, "message.renamer.failedrename")); // NOSONAR
        return false;
      }
      else {
        LOGGER.info("Successfully copied file from {} to {}", srcFile, destFile);
        return true;
      }
    }
    return true; // files are equal
  }

  /**
   * <b>PHYSICALLY</b> deletes a file by moving it to datasource backup folder<br>
   * DS\.backup\&lt;filename&gt;<br>
   * maintaining its originating directory
   *
   * @param file
   *          the file to be deleted
   * @param datasource
   *          the data source (for the location of the backup folder)
   * @return true/false if successful
   */
  public static boolean deleteFileWithBackup(Path file, String datasource) {
    // check if the backup is activated
    if (!Settings.getInstance().isEnableTrash()) {
      // backup disabled
      return deleteFileSafely(file);
    }
    else {
      // backup enabled
      Path ds = Paths.get(datasource);

      if (!file.startsWith(ds)) { // safety
        LOGGER.warn("could not delete file '{}': datasource '{}' does not match", file, datasource);
        return false;
      }

      if (Files.isDirectory(file)) {
        LOGGER.warn("could not delete file '{}': file is a directory!", file);
        return false;
      }

      // check if the file exists; if it does not exist anymore we won't need to delete it ;)
      if (!Files.exists(file)) {
        // this file is no more here - just return "true"
        return true;
      }

      // create backup folder
      try {
        Path backup = Paths.get(ds.toAbsolutePath().toString(), Constants.DS_TRASH_FOLDER);
        if (!Files.exists(backup)) {
          Files.createDirectories(backup);
        }
        if (!Files.exists(backup.resolve(".nomedia"))) {
          Files.createFile(backup.resolve(".nomedia"));
        }
      }
      catch (AccessDeniedException e) {
        // propagate to UI by logging with error
        LOGGER.error("ACCESS DENIED (create folder) - '{}'", e.getMessage());
      }
      catch (Exception e) {
        // ignore
      }

      // backup
      try {
        // create path
        Path backup = Paths.get(ds.toAbsolutePath().toString(), Constants.DS_TRASH_FOLDER, ds.relativize(file).toString());
        if (!Files.exists(backup.getParent())) {
          Files.createDirectories(backup.getParent());
        }
        // overwrite backup file by deletion prior
        Files.deleteIfExists(backup);
        return moveFileSafe(file, backup);
      }
      catch (AccessDeniedException e) {
        // propagate to UI by logging with error
        LOGGER.error("ACCESS DENIED (delete file) - '{}'", e.getMessage());
        return false;
      }
      catch (IOException e) {
        LOGGER.warn("Could not delete file: {}", e.getMessage());
        return false;
      }
    }
  }

  /**
   * <b>PHYSICALLY</b> deletes a file (w/o backup)<br>
   * only doing a check if it is not a directory
   *
   * @param file
   *          the file to be deleted
   * @return true/false if successful
   */
  public static boolean deleteFileSafely(Path file) {
    file = file.toAbsolutePath();
    if (Files.isDirectory(file)) {
      LOGGER.warn("Will not delete file '{}': file is a directory!", file);
      return false;
    }
    try {
      Files.deleteIfExists(file);
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (delete file) - '{}'", e.getMessage());
      return false;
    }
    catch (Exception e) {
      LOGGER.warn("Could not delete file: {}", e.getMessage());
      return false;
    }
    return true;
  }

  /**
   * <b>PHYSICALLY</b> deletes a complete directory by moving it to datasource backup folder<br>
   * DS\.backup\&lt;foldername&gt;<br>
   * maintaining its originating directory
   *
   * @param folder
   *          the folder to be deleted
   * @param datasource
   *          the datasource of this folder
   * @return true/false if successful
   */
  public static boolean deleteDirectorySafely(Path folder, String datasource) {
    // check if the backup is activated
    if (!Settings.getInstance().isEnableTrash()) {
      // backup disabled
      try {
        deleteDirectoryRecursive(folder);
        return true;
      }
      catch (Exception e) {
        LOGGER.error("Could not delete folder '{}' - '{}'", folder, e.getMessage());
        return false;
      }
    }
    else {
      folder = folder.toAbsolutePath();
      Path ds = Paths.get(datasource);

      if (!Files.isDirectory(folder)) {
        LOGGER.warn("Will not delete folder '{}': folder is a file, NOT a directory!", folder);
        return false;
      }
      if (!folder.startsWith(ds)) { // safety
        LOGGER.warn("Will not delete folder '{}': datasource '{}' does not match", folder, datasource);
        return false;
      }

      // create backup folder
      try {
        Path backup = Paths.get(ds.toAbsolutePath().toString(), Constants.DS_TRASH_FOLDER);
        if (!Files.exists(backup)) {
          Files.createDirectories(backup);
        }
        if (!Files.exists(backup.resolve(".nomedia"))) {
          Files.createFile(backup.resolve(".nomedia"));
        }
      }
      catch (AccessDeniedException e) {
        // propagate to UI by logging with error
        LOGGER.error("ACCESS DENIED (read file) - '{}'", e.getMessage());
      }
      catch (Exception e) {
        // ignore
      }

      // backup
      try {
        Instant instant = Instant.now();
        long timeStampSeconds = instant.getEpochSecond();
        // create path
        Path backup = Paths.get(ds.toAbsolutePath().toString(), Constants.DS_TRASH_FOLDER, ds.relativize(folder).toString() + timeStampSeconds);
        if (!Files.exists(backup.getParent())) {
          Files.createDirectories(backup.getParent());
        }
        // overwrite backup file by deletion prior
        // deleteDirectoryRecursive(backup); // we timestamped our folder - no need for it
        return moveDirectorySafe(folder, backup);
      }
      catch (AccessDeniedException e) {
        // propagate to UI by logging with error
        LOGGER.error("ACCESS DENIED (move folder) - '{}'", e.getMessage());
        return false;
      }
      catch (IOException e) {
        LOGGER.error("could not delete directory: {}", e.getMessage());
        return false;
      }
    }
  }

  /**
   * returns a list of all available GUI languages
   *
   * @return List of Locales
   */
  public static List<Locale> getLanguages() {
    if (!AVAILABLE_LOCALES.isEmpty()) {
      // do not return the original list to avoid external manipulation
      return new ArrayList<>(AVAILABLE_LOCALES);
    }

    AVAILABLE_LOCALES.add(getLocaleFromLanguage(Locale.ENGLISH.getLanguage()));
    try {
      // list all properties files from the classpath
      InputStream is = Utils.class.getResourceAsStream("/");
      if (is != null) {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String resource;
        while ((resource = br.readLine()) != null) {
          parseLocaleFromFilename(resource);
        }
      }

      if (AVAILABLE_LOCALES.size() == 1) {
        // we may be in a .jar file
        CodeSource src = Utils.class.getProtectionDomain().getCodeSource();
        if (src != null) {
          URL jar = src.getLocation();
          try (InputStream jarInputStream = jar.openStream(); ZipInputStream zip = new ZipInputStream(jarInputStream)) {
            while (true) {
              ZipEntry e = zip.getNextEntry();
              if (e == null) {
                break;
              }
              parseLocaleFromFilename(e.getName());
            }
          }
        }
      }
    }
    catch (Exception e) {
      LOGGER.warn("could not read locales: " + e.getMessage(), e);
    }

    // do not return the original list to avoid external manipulation
    return new ArrayList<>(AVAILABLE_LOCALES);
  }

  private static void parseLocaleFromFilename(String filename) {
    Matcher matcher = localePattern.matcher(filename);
    if (matcher.matches()) {
      Locale myloc;

      String language = matcher.group(1);
      String country = matcher.group(2);

      if (StringUtils.isNotBlank(country)) {
        // found language & country
        myloc = getLocaleFromLanguage(language + "_" + country);
      }
      else {
        // found only language
        myloc = getLocaleFromLanguage(language);
      }
      if (myloc != null && !AVAILABLE_LOCALES.contains(myloc)) {
        AVAILABLE_LOCALES.add(myloc);
      }
    }
  }

  /**
   * Gets a correct Locale (language + country) from given language.
   *
   * @param language
   *          as 2char
   * @return Locale
   */
  public static Locale getLocaleFromLanguage(String language) {
    if (StringUtils.isBlank(language)) {
      return Locale.getDefault();
    }

    // don't mess around; at least fixate this
    if ("en".equalsIgnoreCase(language)) {
      return Locale.US;
    }
    // fixate Chinese ones based on script, not country!
    if ("zh_Hant".equalsIgnoreCase(language) || "zh__#Hant".equalsIgnoreCase(language) || "zh_HK".equalsIgnoreCase(language)
        || "zh_TW".equalsIgnoreCase(language)) {
      return new Locale.Builder().setLanguage("zh").setScript("Hant").build();
    }
    if ("zh".equalsIgnoreCase(language) || "zh__#Hans".equalsIgnoreCase(language) || "zh_Hans".equalsIgnoreCase(language)
        || "zh_CN".equalsIgnoreCase(language) || "zh_SG".equalsIgnoreCase(language)) {
      return new Locale.Builder().setLanguage("zh").setScript("Hans").build();
    }

    if (language.length() > 2) {
      try {
        return LocaleUtils.toLocale(language);
      }
      catch (Exception e) {
        LOGGER.warn("Could not parse language {}", language);
      }
    }

    return new Locale(language); // let java decide..?
  }

  /**
   * returns all known translations for a key, like "metatag.title"
   *
   * @param key
   * @return list of translated values
   */
  public static List<String> getAllTranslationsFor(String key) {
    List<String> ret = new ArrayList<>();
    for (Locale l : getLanguages()) {
      ResourceBundle b = ResourceBundle.getBundle("messages", l);
      try {
        String value = b.getString(key);
        if (!ret.contains(value)) {
          ret.add(value);
        }
      }
      catch (Exception e) {
        // eg not found - ignore
      }
    }
    return ret;
  }

  /**
   * creates a zipped backup of file in backup folder with yyyy-MM-dd timestamp<br>
   * <b>does overwrite already existing file from today!</b>
   *
   * @param file
   *          the file to backup
   */
  public static void createBackupFile(Path file) {
    createBackupFile(file, true);
  }

  /**
   * creates a zipped backup of file in backup folder with yyyy-MM-dd timestamp
   *
   * @param file
   *          the file to backup
   * @param overwrite
   *          if file is already there, ignore that and overwrite with new copy
   */
  public static void createBackupFile(Path file, boolean overwrite) {
    Path backup = Paths.get(Globals.BACKUP_FOLDER);
    try {
      if (!Files.exists(backup)) {
        Files.createDirectory(backup);
      }
      if (!Files.exists(file)) {
        return;
      }
      DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      String date = formatter.format(new Date());
      backup = backup.resolve(file.getFileName() + "." + date + ".zip");
      if (!Files.exists(backup) || overwrite) {
        createZip(backup, file); // just put in main dir
      }
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (create backup file) - '{}'", e.getMessage());
    }
    catch (IOException e) {
      LOGGER.error("Could not backup file {}: {}", file, e.getMessage());
    }

  }

  /**
   * Deletes old backup files in backup folder; keep only last X files
   *
   * @param file
   *          the file of backup to be deleted
   * @param keep
   *          keep last X versions
   */
  public static void deleteOldBackupFile(Path file, int keep) {
    ArrayList<Path> al = new ArrayList<>();
    String fname = file.getFileName().toString();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(Globals.BACKUP_FOLDER))) {
      for (Path path : directoryStream) {
        if (path.getFileName().toString().matches(fname + "\\.\\d{4}\\-\\d{2}\\-\\d{2}\\.zip") ||
        // name.ext.yyyy-mm-dd.zip
            path.getFileName().toString().matches(fname + "\\.\\d{4}\\-\\d{2}\\-\\d{2}")) { // old name.ext.yyyy-mm-dd
          al.add(path);
        }
      }
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (listing old backups) - '{}'", e.getMessage());
    }
    catch (IOException e) {
      LOGGER.error("could not list files from the backup folder: {}", e.getMessage());
      return;
    }

    // sort files by creation date
    al.sort((o1, o2) -> (int) (o1.toFile().lastModified() - o2.toFile().lastModified()));

    for (int i = 0; i < al.size() - keep; i++) {
      Path backupFile = al.get(i);
      LOGGER.debug("deleting old backup file {}", backupFile.getFileName());
      deleteFileSafely(backupFile);
    }

  }

  /**
   * Sends a wake-on-lan packet for specified MAC address across subnet
   *
   * @param macAddr
   *          the mac address to 'wake up'
   */
  public static void sendWakeOnLanPacket(String macAddr) {
    // Broadcast IP address
    final String IP = "255.255.255.255";
    final int port = 7;

    try {
      final byte[] MACBYTE = new byte[6];
      final String[] hex = macAddr.split("(\\:|\\-)");

      for (int i = 0; i < 6; i++) {
        MACBYTE[i] = (byte) Integer.parseInt(hex[i], 16);
      }
      final byte[] bytes = new byte[6 + 16 * MACBYTE.length];
      for (int i = 0; i < 6; i++) {
        bytes[i] = (byte) 0xff;
      }
      for (int i = 6; i < bytes.length; i += MACBYTE.length) {
        System.arraycopy(MACBYTE, 0, bytes, i, MACBYTE.length);
      }

      // Send UDP packet here
      final InetAddress address = InetAddress.getByName(IP);
      final DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
      try (DatagramSocket socket = new DatagramSocket()) {
        socket.send(packet);
      }

      LOGGER.info("Sent WOL packet to {}", macAddr);
    }
    catch (final Exception e) {
      LOGGER.error("Error sending WOL packet to {} - {}", macAddr, e.getMessage());
    }
  }

  /**
   * Deletes a complete directory recursively, using Java NIO
   *
   * @param dir
   *          directory to delete
   * @throws IOException
   */
  public static void deleteDirectoryRecursive(Path dir) throws IOException {
    if (!Files.exists(dir) || !Files.isDirectory(dir)) {
      return;
    }

    LOGGER.info("Deleting complete directory: {}", dir);
    try {
      Files.walkFileTree(dir, new FileVisitor<>() {

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
          LOGGER.warn("Could not delete {} - {}", file, exc.getMessage());
          return FileVisitResult.CONTINUE;
        }

      });
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (delete complete directory) - '{}'", e.getMessage());
      // re-trow
      throw e;
    }
  }

  /**
   * Deletes a complete directory recursively, but checking if empty (from inside out) - using Java NIO
   *
   * @param dir
   *          directory to delete
   * @throws IOException
   */
  public static void deleteEmptyDirectoryRecursive(Path dir) throws IOException {
    if (!Files.exists(dir) || !Files.isDirectory(dir)) {
      return;
    }

    LOGGER.info("Deleting empty directories in: {}", dir);
    try {
      Files.walkFileTree(dir, new FileVisitor<>() {

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          if (isFolderEmpty(dir)) {
            Files.delete(dir);
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
          return FileVisitResult.CONTINUE;
        }
      });
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (delete empty directories) - '{}'", e.getMessage());
      // re-trow
      throw e;
    }
  }

  /**
   * check whether a folder is empty or not
   *
   * @param folder
   *          the folder to be checked
   * @return true/false
   * @throws IOException
   */
  public static boolean isFolderEmpty(final Path folder) throws IOException {
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(folder)) {
      return !dirStream.iterator().hasNext();
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (checking for empty folders) - '{}'", e.getMessage());
      // re-trow
      throw e;
    }
  }

  /**
   * Creates (or adds) a file to a ZIP
   *
   * @param zipFile
   *          Path of zip file
   * @param toBeAdded
   *          Path to be added
   */
  public static void createZip(Path zipFile, Path toBeAdded) {
    List<File> filesToArchive = new ArrayList<>();

    if (Files.isDirectory(toBeAdded)) {
      filesToArchive.addAll(FileUtils.listFiles(toBeAdded.toFile(), null, true));
    }
    else {
      filesToArchive.add(toBeAdded.toFile());
    }

    try (ZipArchiveOutputStream archive = new ZipArchiveOutputStream(zipFile.toFile())) {
      for (File file : filesToArchive) {
        String entryName = getEntryName(toBeAdded.toFile(), file);
        ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
        archive.putArchiveEntry(entry);

        try (InputStream is = Files.newInputStream(file.toPath()); BufferedInputStream input = new BufferedInputStream(is)) {
          IOUtils.copy(input, archive);
        }
        archive.closeArchiveEntry();
      }
      archive.finish();
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (creating .zip) - '{}'", e.getMessage());
    }
    catch (Exception e) {
      LOGGER.error("Failed to create zip file: {}", e.getMessage()); // NOSONAR
    }
  }

  /**
   * Remove the leading part of each entry that contains the source directory name
   *
   * @param source
   *          the directory where the file entry is found
   * @param file
   *          the file that is about to be added
   * @return the name of an archive entry
   * @throws IOException
   *           if the io fails
   */
  private static String getEntryName(File source, File file) throws IOException {
    int index = source.getAbsoluteFile().getParentFile().getAbsolutePath().length() + 1;
    String path = file.getCanonicalPath();

    return path.substring(index);
  }

  /**
   * Unzips the specified zip file to the specified destination directory. Replaces any files in the destination, if they already exist.
   *
   * @param zipFile
   *          the name of the zip file to extract
   * @param destDir
   *          the directory to unzip to
   */
  public static void unzip(final Path zipFile, final Path destDir) {
    Map<String, String> env = new HashMap<>();

    if (!Files.exists(zipFile)) {
      return;
    }

    try {
      // if the destination doesn't exist, create it
      if (!Files.exists(destDir)) {
        Files.createDirectories(destDir);
      }

      env.put("create", "false");

      // use a Zip filesystem URI
      URI fileUri = zipFile.toUri(); // here
      URI zipUri = new URI("jar:" + fileUri.getScheme(), fileUri.getPath(), null);

      try (FileSystem zipfs = FileSystems.newFileSystem(zipUri, env)) {
        final Path root = zipfs.getPath("/");

        // walk the zip file tree and copy files to the destination
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            final Path destFile = Paths.get(destDir.toString(), file.toString());
            LOGGER.debug("Extracting file {} to {}", file, destFile);
            Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
            fixDateAttributes(file, destFile);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            final Path dirToCreate = Paths.get(destDir.toString(), dir.toString());
            if (!Files.exists(dirToCreate)) {
              LOGGER.debug("Creating directory {}", dirToCreate);
              Files.createDirectory(dirToCreate);
            }
            return FileVisitResult.CONTINUE;
          }
        });
      }
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (extracting .zip) - '{}'", e.getMessage());
    }
    catch (Exception e) {
      LOGGER.error("Failed to create zip file: {}", e.getMessage()); // NOSONAR
    }
  }

  /**
   * Unzips the specified file from the given zip file to the specified destination filename. Replaces if the destination already exist.
   *
   * @param zipFile
   *          the name of the zip file to extract
   * @param fileToExtract
   *          the name of the file to extract
   * @param destFile
   *          the directory to unzip to
   */
  public static void unzipFile(final Path zipFile, final Path fileToExtract, final Path destFile) {
    Map<String, String> env = new HashMap<>();

    if (!Files.exists(zipFile)) {
      return;
    }

    try {
      // if the destination doesn't exist, create it
      if (!Files.exists(destFile.getParent())) {
        Files.createDirectories(destFile.getParent());
      }

      // use a Zip filesystem URI
      URI fileUri = zipFile.toUri(); // here
      URI zipUri = new URI("jar:" + fileUri.getScheme(), fileUri.getPath(), null);

      env.put("create", "false");

      try (FileSystem zipfs = FileSystems.newFileSystem(zipUri, env)) {
        final Path root = zipfs.getPath("/");

        // walk the zip file tree and copy files to the destination
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toString().equals(fileToExtract.toString())) {
              LOGGER.debug("Extracting file {} to {}", file, destFile);
              Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
              fixDateAttributes(file, destFile);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
          }
        });
      }
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (extracting .zip) - '{}'", e.getMessage());
    }
    catch (Exception e) {
      LOGGER.error("Failed to create zip file: {}", e.getMessage()); // NOSONAR
    }
  }

  /**
   * Java NIO replacement of commons-io
   *
   * @param file
   *          the file to write the string to
   * @param text
   *          the text to be written into the file
   * @throws IOException
   *           any {@link IOException} thrown
   */
  public static void writeStringToFile(Path file, String text) throws IOException {
    try {
      // pre-delete existing file. this will be needed for CaSe insensitive file systems,
      // because truncating the existing one may result to a false filename
      // Files.deleteIfExists(file);
      // NOOO - this is the wrong place. Our renamer needs to take care of such changes...
      // deleting and recreating also destroys all set permissions & attributes...

      // write the file
      Files.writeString(file, text);
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (delete/write file) - '{}'", e.getMessage());
      // re-throw
      throw e;
    }
  }

  /**
   * Java NIO replacement of commons-io
   *
   * @param file
   *          the file to read the string from
   * @return the read string
   * @throws IOException
   *           any {@link IOException} thrown
   */
  public static String readFileToString(Path file) throws IOException {
    try {
      return Files.readString(file);
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (read file) - '{}'", e.getMessage());
      // re-throw
      throw e;
    }
  }

  /**
   * Copies a complete directory recursively, using Java NIO
   *
   * @param from
   *          source
   * @param to
   *          destination
   * @throws IOException
   *           any {@link IOException} thrown
   */
  public static void copyDirectoryRecursive(Path from, Path to) throws IOException {
    LOGGER.info("Copying complete directory from {} to {}", from, to);
    try {
      Files.walkFileTree(from, new CopyFileVisitor(to));
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (copy directory) - '{}'", e.getMessage());
      // re-throw
      throw e;
    }
  }

  /**
   * logback does not clean older log files than 32 days in the past. We have to clean the log files too
   */
  public static void cleanOldLogs() {
    Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, -30);
    Date dateBefore30Days = cal.getTime();

    // the log file pattern is logs/tmm.%d.%i.log.gz
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(Globals.LOG_FOLDER))) {
      for (Path path : directoryStream) {
        Matcher matcher = pattern.matcher(path.getFileName().toString());
        if (matcher.find()) {
          try {
            Date date = StrgUtils.parseDate(matcher.group());
            if (dateBefore30Days.after(date)) {
              Utils.deleteFileSafely(path);
            }
          }
          catch (Exception e) {
            LOGGER.debug("could not clean old logs: {}", e.getMessage());
          }
        }
      }
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (delete old logs) - '{}'", e.getMessage());
    }
    catch (IOException e) {
      LOGGER.debug("could not clean old logs: {}", e.getMessage());
    }

    // keep last 3 TRACE log files
    List<Path> traces = Utils.listFiles(Paths.get(Globals.LOG_FOLDER));
    traces.removeIf(s -> !s.getFileName().toString().startsWith("trace"));
    Collections.sort(traces);
    for (int i = 0; i < traces.size() - 3; i++) {
      Utils.deleteFileSafely(traces.get(i));
    }
  }

  /**
   * detect the artwork extension from the url
   *
   * @param url
   *          the url to analyze
   * @return the detected artwork type or jpg as fallback
   */
  public static String getArtworkExtensionFromUrl(String url) {
    if (StringUtils.isBlank(url)) {
      return "jpg";
    }

    String ext = UrlUtil.getExtension(url).toLowerCase(Locale.ROOT);
    if (StringUtils.isBlank(ext)) {
      // no extension from the url? try a head request to detect the artwork type
      try {
        Url url1 = new Url(url);
        InputStream is = url1.getInputStream(true);
        ext = Utils.getArtworkExtensionFromContentType(url1.getHeader("content-type"));
        is.close();
      }
      catch (Exception e) {
        // ignored
      }
    }

    // still blank or tbn -> fallback to jpg
    if (StringUtils.isBlank(ext) || "tbn".equals(ext)) {
      // no extension or tbn? fall back to jpg
      ext = "jpg";
    }

    // just cross-check for supported extensions (others will be written as jpg regardless of its real type)
    if (!MediaFileHelper.SUPPORTED_ARTWORK_FILETYPES.contains(ext)) {
      ext = "jpg";
    }

    return ext.toLowerCase(Locale.ROOT);
  }

  /**
   * detect the artwork extension from the content type<br />
   * taken from https://wiki.selfhtml.org/wiki/MIME-Type/%C3%9Cbersicht#I
   *
   * @param contentType
   *          the HTTP header "content type"
   * @return the artwork extension or an empty string if not detectable
   */
  public static String getArtworkExtensionFromContentType(String contentType) {
    if (StringUtils.isBlank(contentType)) {
      return "";
    }

    if (contentType.startsWith("image/")) {
      // handle our well known extensions
      switch (contentType.replace("image/", "")) {
        case "bmp":
        case "x-bmp":
        case "x-ms-bmp":
          return "bmp";

        case "gif":
          return "gif";

        case "jpeg":
          return "jpg";

        case "png":
          return "png";

        case "tiff":
          return "tif";

        case "webp":
          return "webp";

        default:
          return "";
      }
    }

    return "";
  }

  /**
   * get all files from the given path
   *
   * @param root
   *          the root folder to search files for
   * @return a list of all found files
   */
  public static List<Path> listFiles(Path root) {
    final List<Path> filesFound = new ArrayList<>();
    if (!Files.isDirectory(root)) {
      return filesFound;
    }
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(root)) {
      for (Path path : directoryStream) {
        if (Utils.isRegularFile(path)) {
          filesFound.add(path);
        }
      }
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (list files) - '{}'", e.getMessage());
    }
    catch (IOException e) {
      LOGGER.warn("could not get a file listing: {}", e.getMessage());
    }

    return filesFound;
  }

  /**
   * get all files from the given path recursive
   *
   * @param root
   *          the root folder to search files for
   * @return a list of all found files
   */
  public static List<Path> listFilesRecursive(Path root) {
    final List<Path> filesFound = new ArrayList<>();
    if (!Files.isDirectory(root)) {
      return filesFound;
    }
    try {
      Files.walkFileTree(root, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new AbstractFileVisitor() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          if (Utils.isRegularFile(file)) {
            filesFound.add(file);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (list files) - '{}'", e.getMessage());
    }
    catch (Exception e) {
      LOGGER.warn("could not get a file listing: {}", e.getMessage());
    }

    return filesFound;
  }

  /**
   * Returns the size of that disc folder<br>
   * Counts only files which are "discFiles()", so a generated NFO would not interfere.
   *
   * @param path
   * @return
   */
  public static long getDirectorySizeOfDiscFiles(Path path) {
    long size = 0;

    // need close Files.walk
    try (Stream<Path> walk = Files.walk(path)) {

      size = walk
          // .peek(System.out::println) // debug
          .filter(Utils::isRegularFile)
          .filter(f -> new MediaFile(f).isDiscFile())
          .mapToLong(p -> {
            // ugly, can pretty it with an extract method
            try {
              return Files.size(p);
            }
            catch (IOException e) {
              return 0L;
            }
          })
          .sum();

    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (calculate folder size) - '{}'", e.getMessage());
    }
    catch (IOException e) {
      LOGGER.warn("Coule not get folder size: ", e);
    }
    return size;
  }

  /**
   * flush the {@link FileOutputStream} to the disk
   */
  public static void flushFileOutputStreamToDisk(FileOutputStream fileOutputStream) {
    if (fileOutputStream == null) {
      return;
    }

    try {
      fileOutputStream.flush();
      fileOutputStream.getFD().sync(); // wait until file has been completely written
      // give it a few milliseconds
      Thread.sleep(150);
    }
    catch (Exception e) {
      LOGGER.error("could not flush to disk: {}", e.getMessage());
    }
  }

  /**
   * Visitor for copying a directory recursively<br>
   * Usage: Files.walkFileTree(sourcePath, new CopyFileVisitor(targetPath));
   */
  public static class CopyFileVisitor extends SimpleFileVisitor<Path> {
    private final Path targetPath;
    private Path       sourcePath = null;

    public CopyFileVisitor(Path targetPath) {
      this.targetPath = targetPath;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
      if (sourcePath == null) {
        sourcePath = dir;
      }
      Path target = targetPath.resolve(sourcePath.relativize(dir));
      if (!Files.exists(target)) {
        try {
          Files.createDirectories(target);
        }
        catch (FileAlreadyExistsException e) {
          // ignore
        }
        catch (IOException x) {
          return FileVisitResult.SKIP_SUBTREE;
        }
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
      Files.copy(file, targetPath.resolve(sourcePath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
      fixDateAttributes(file, targetPath);
      return FileVisitResult.CONTINUE;
    }
  }

  /**
   * get the temporary folder for this tmm instance
   *
   * @return a string to the temporary folder
   */
  public static String getTempFolder() {
    return tempFolder;
  }

  /**
   * Method to get a list of files with the given regular expression
   *
   * @param regexList
   *          list of regular expression
   * @return a list of files
   */
  public static Set<Path> getUnknownFilesByRegex(Path folder, List<String> regexList) {
    GetUnknownFilesVisitor visitor = new GetUnknownFilesVisitor(regexList);

    try {
      Files.walkFileTree(folder, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, visitor);
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (list files) - '{}'", e.getMessage());
    }
    catch (IOException e) {
      LOGGER.error("could not get unknown files: {}", e.getMessage());
    }

    return visitor.fileList;
  }

  private static class GetUnknownFilesVisitor extends AbstractFileVisitor {
    private final Set<Path>    fileList = new HashSet<>();
    private final List<String> regexList;

    GetUnknownFilesVisitor(List<String> regexList) {
      this.regexList = regexList;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
      for (String regex : regexList) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(file.getFileName().toString());
        if (m.find()) {
          fileList.add(file);
        }
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes var2) {
      // if we're in a disc folder, don't walk further
      if (dir.getFileName() != null && dir.getFileName().toString().matches(DISC_FOLDER_REGEX)) {
        return SKIP_SUBTREE;
      }

      for (String regex : regexList) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(dir.getFileName().toString());
        if (m.find()) {
          fileList.add(dir);
        }
      }

      return CONTINUE;
    }
  }

  /**
   * check if the given folder contains any of the well known skip files (tmmignore, .tmmignore, .nomedia)
   *
   * @param dir
   *          the folder to check
   * @return true/false
   */
  public static boolean containsSkipFile(Path dir) {
    return Files.exists(dir.resolve(".tmmignore")) || Files.exists(dir.resolve("tmmignore")) || Files.exists(dir.resolve(".nomedia"));
  }

  /**
   * Deletes "unwanted files/folders" according to settings. Same as the action, but w/o GUI.
   *
   * @param me
   *          the {@link MediaEntity} to clean
   */
  public static void deleteUnwantedFilesAndFoldersFor(MediaEntity me) {
    // Get Cleanup File Types from the settings
    List<String> regexPatterns = Settings.getInstance().getCleanupFileType();
    LOGGER.info("Start cleanup of unwanted file types/folders: {}", regexPatterns);

    Set<Path> fileList = new HashSet<>();
    for (Path file : Utils.getUnknownFilesByRegex(me.getPathNIO(), regexPatterns)) {
      if (fileList.contains(file)) {
        continue;
      }
      fileList.add(file);
    }

    boolean dirty = false;

    for (Path file : fileList) {
      if (Files.isDirectory(file)) {
        try {
          LOGGER.debug("Deleting folder - {}", file);
          Utils.deleteDirectoryRecursive(file);
          dirty = true;
        }
        catch (Exception e) {
          LOGGER.debug("could not delete folder - {}", e.getMessage());
        }
      }
      else {
        MediaFile mf = new MediaFile(file);
        if (mf.getType() == MediaFileType.VIDEO) {
          // prevent users from doing something stupid
          continue;
        }
        LOGGER.debug("Deleting file - {}", file);
        Utils.deleteFileWithBackup(file, me.getDataSource());
        // remove possible MediaFiles too
        if (me.getMediaFiles().contains(mf)) {
          me.removeFromMediaFiles(mf);
          dirty = true;
        }
      }
    }

    if (dirty) {
      me.saveToDb();
    }
  }

  private static Set<PosixFilePermission> parsePerms(int mode) {
    Set<PosixFilePermission> ret = new HashSet<>();
    if ((mode & 0001) > 0) {
      ret.add(PosixFilePermission.OTHERS_EXECUTE);
    }
    if ((mode & 0002) > 0) {
      ret.add(PosixFilePermission.OTHERS_WRITE);
    }
    if ((mode & 0004) > 0) {
      ret.add(PosixFilePermission.OTHERS_READ);
    }
    if ((mode & 0010) > 0) {
      ret.add(PosixFilePermission.GROUP_EXECUTE);
    }
    if ((mode & 0020) > 0) {
      ret.add(PosixFilePermission.GROUP_WRITE);
    }
    if ((mode & 0040) > 0) {
      ret.add(PosixFilePermission.GROUP_READ);
    }
    if ((mode & 0100) > 0) {
      ret.add(PosixFilePermission.OWNER_EXECUTE);
    }
    if ((mode & 0200) > 0) {
      ret.add(PosixFilePermission.OWNER_WRITE);
    }
    if ((mode & 0400) > 0) {
      ret.add(PosixFilePermission.OWNER_READ);
    }
    return ret;
  }

  public static void clearTempFolder() {
    try {
      FileUtils.forceDeleteOnExit(Paths.get(tempFolder).toFile());
    }
    catch (Exception ignored) {
      // just ignore
    }
  }

  /**
   * replace all illegal characters in a filename with an underscore
   *
   * @param filename
   *          the filename to be cleaned
   * @return the cleaned filename
   */
  public static String cleanFilename(String filename) {
    for (char c : ILLEGAL_FILENAME_CHARACTERS) {
      filename = filename.replace(c, '_');
    }
    return filename;
  }

  /**
   * handy method to return 1 when the value is filled
   *
   * @param value
   *          the value
   * @return 1 if the value is not null/empty - 0 otherwise
   */
  public static int returnOneWhenFilled(String value) {
    if (StringUtils.isNotBlank(value)) {
      return 1;
    }
    return 0;
  }

  /**
   * handy method to return 1 when the value is filled
   *
   * @param value
   *          the value
   * @return 1 if the value is not null/empty - 0 otherwise
   */
  public static int returnOneWhenFilled(int value) {
    if (value > 0) {
      return 1;
    }
    return 0;
  }

  /**
   * handy method to return 1 when the value is filled
   *
   * @param value
   *          the value
   * @return 1 if the value is not null/empty - 0 otherwise
   */
  public static int returnOneWhenFilled(Date value) {
    if (value != null && value.getTime() > 0) {
      return 1;
    }
    return 0;
  }

  /**
   * handy method to return 1 when the value is filled
   *
   * @param value
   *          the value
   * @return 1 if the value is not null/empty - 0 otherwise
   */
  public static int returnOneWhenFilled(Collection<?> value) {
    if (value != null && !value.isEmpty()) {
      return 1;
    }
    return 0;
  }

  /**
   * handy method to return 1 when the value is filled
   *
   * @param value
   *          the value
   * @return 1 if the value is not null/empty - 0 otherwise
   */
  public static int returnOneWhenFilled(Map<?, ?> value) {
    if (value != null && !value.isEmpty()) {
      return 1;
    }
    return 0;
  }

  /**
   * Format any file size according the preferred UI setting
   *
   * @param filesize
   *          the file size in bytes
   * @return the formatted file size as {@link String}
   */
  public static String formatFileSizeForDisplay(long filesize) {
    if (!Settings.getInstance().isFileSizeDisplayHumanReadable()) {
      // in MB
      double sizeInMb = filesize / (1000.0 * 1000.0);
      DecimalFormat df;

      if (sizeInMb < 1) {
        df = new DecimalFormat("#0.00");
      }
      else {
        df = new DecimalFormat("#0");
      }

      return df.format(sizeInMb) + " M";
    }

    long bytes = filesize;

    if (-1000 < bytes && bytes < 1000) {
      return bytes + " B";
    }

    CharacterIterator ci = new StringCharacterIterator("kMGTPE");
    while (bytes <= -999_950 || bytes >= 999_950) {
      bytes /= 1000;
      ci.next();
    }

    DecimalFormat df = new DecimalFormat("#0.00");
    return df.format(bytes / 1000.0) + " " + ci.current();
  }
}
