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
package org.tinymediamanager.core.tvshow;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.ParserUtils;

/**
 * The Class TvShowEpisodeAndSeasonParser.
 * 
 * @author Manuel Laggner
 */
public class TvShowEpisodeAndSeasonParser {
  private static final Logger  LOGGER            = LoggerFactory.getLogger(TvShowEpisodeAndSeasonParser.class);

  // foo.yyyy.mm.dd.*
  private static final Pattern DATE_1            = Pattern.compile("([0-9]{4})[.-]([0-9]{2})[.-]([0-9]{2})", Pattern.CASE_INSENSITIVE);

  // foo.mm.dd.yyyy.*
  private static final Pattern DATE_2            = Pattern.compile("([0-9]{2})[.-]([0-9]{2})[.-]([0-9]{4})", Pattern.CASE_INSENSITIVE);

  // new parsing logic
  public static final Pattern  SEASON_LONG       = Pattern.compile("(staffel|season|saison|series|temporada)[\\s_.-]?(\\d{1,4})",
      Pattern.CASE_INSENSITIVE);
  // must start with a delimiter!
  public static final Pattern  SEASON_ONLY       = Pattern.compile("[\\s_.-]s[\\s_.-]?(\\d{1,4})", Pattern.CASE_INSENSITIVE);
  public static final Pattern  EPISODE_ONLY      = Pattern.compile("[\\s_.-]ep?[\\s_.-]?(\\d{1,4})", Pattern.CASE_INSENSITIVE);
  private static final Pattern EPISODE_PATTERN   = Pattern.compile("[epx_-]+(\\d{1,4})", Pattern.CASE_INSENSITIVE);
  private static final Pattern EPISODE_PATTERN_2 = Pattern.compile("(?:episode|ep)[\\. _-]*(\\d{1,4})", Pattern.CASE_INSENSITIVE);
  private static final Pattern ROMAN_PATTERN     = Pattern.compile("(part|pt)[\\._\\s]+([MDCLXVI]+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern SEASON_MULTI_EP   = Pattern.compile("s(\\d{1,4})[ ]?((?:([epx.-]+\\d{1,4})+))", Pattern.CASE_INSENSITIVE);
  private static final Pattern SEASON_MULTI_EP_2 = Pattern.compile("(\\d{1,4})(?=x)((?:([epx]+\\d{1,4})+))", Pattern.CASE_INSENSITIVE);
  private static final Pattern NUMBERS_2_PATTERN = Pattern.compile("([0-9]{2})", Pattern.CASE_INSENSITIVE);
  private static final Pattern NUMBERS_3_PATTERN = Pattern.compile("([0-9])([0-9]{2})", Pattern.CASE_INSENSITIVE);

  // https://kodi.wiki/view/Anime - PREP should run before any default Kodi regex
  private static final Pattern ANIME_PREPEND1    = Pattern.compile(
      "(Special|SP|OVA|OAV|Picture Drama)(?:[ _.-]*(?:ep?[ .]?)?(\\d{1,3})(?:[_ ]?v\\d+)?)+(?=\\b|_)[^])}]*?(?:[\\[({][^])}]+[\\])}][ _.-]*)*?(?:[\\[({][\\da-f]{8}[\\])}])",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern ANIME_PREPEND2    = Pattern.compile(
      "(?:S(?:eason)?\\s*(?=\\d))?(Specials|\\d{1,3})[\\/](?:[^\\/]+[\\/])*[^\\/]+(?:\\b|_)(?:[ _.-]*(?:ep?[ .]?)?(\\d{1,3})(?:[_ ]?v\\d+)?)+(?=\\b|_)[^])}]*?(?:[\\[({][^])}]+[\\])}][ _.-]*)*?(?:[\\[({][\\da-f]{8}[\\])}])",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern ANIME_PREPEND3    = Pattern.compile(
      "[-._ ]+S(?:eason ?)?(\\d{1,3})(?:[ _.-]*(?:ep?[ .]?)?(\\d{1,3})(?:[_ ]?v\\d+)?)+(?=\\b|_)[^])}]*?(?:[\\[({][^])}]+[\\])}][_.-]*)*?(?:[\\[({][\\da-f]{8}[\\])}])",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern ANIME_PREPEND4    = Pattern.compile(
      "((?=\\b|_))(?:[ _.-]*(?:ep?[ .]?)?(\\d{1,3})(?:[_ ]?v\\d+)?)+(?=\\b|_)[^])}]*?(?:[\\[({][^])}]+[\\])}][ _.-]*)*?(?:[\\[({][\\da-f]{8}[\\])}])",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern ANIME_APPEND1     = Pattern.compile(
      "(Special|SP|OVA|OAV|Picture Drama)(?:[ _.-]*(?:ep?[ .]?)?(\\d{1,3})(?:[_ ]?v\\d+)?)+(?=\\b|_)[^\\])}]*?(?:[\\[({][^\\])}]+[\\])}][ _.-]*)*?[^\\]\\[)(}{\\\\/]*$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern ANIME_APPEND2     = Pattern.compile(
      "(?:S(?:eason)?\\s*(?=\\d))?(Specials|\\d{1,3})[\\\\/](?:[^\\\\/]+[\\\\/])*[^\\\\/]+(?:\\b|_)[ _.-]*(?:ep?[ .]?)?(\\d{1,3})(?:[_ ]?v\\d+)?(?:\\b|_)[^\\])}]*?(?:[\\[({][^\\])}]+[\\])}][ _.-]*)*?[^\\]\\[)(}{\\\\/]*?$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern ANIME_APPEND3     = Pattern.compile(
      "[-._ ]+S(?:eason ?)?(\\d{1,3})(?:[ _.-]*(?:ep?[ .]?)?(\\d{1,3})(?:[_ ]?v\\d+)?)+(?=\\b|_)[^\\])}]*?(?:[\\[({][^\\])}]+[\\])}][ _.-]*)*?[^\\]\\[)(}{\\\\/]*$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern ANIME_APPEND4     = Pattern.compile(
      "((?=\\b|_))(?:[ _.-]*(?:ep?[ .]?)?(\\d{1,3})(?:[_ ]?v\\d+)?)+(?=\\b|_)[^\\])}]*?(?:[\\[({][^\\])}]+[\\])}][ _.-]*)*?[^\\]\\[)(}{\\\\/]*$",
      Pattern.CASE_INSENSITIVE);

  private TvShowEpisodeAndSeasonParser() {
    throw new IllegalAccessError();
  }

  public static String cleanEpisodeTitle(String titleToClean, String tvShowName) {
    String basename = ParserUtils.removeStopwordsAndBadwordsFromTvEpisodeName(titleToClean.replaceAll("([\":<>|?*])", ""));
    basename = Utils.cleanFolderStackingMarkers(basename);// i know, but this needs no extension ;)

    // parse foldername
    Pattern regex = Pattern.compile("(.*[\\/\\\\])");
    Matcher m = regex.matcher(basename);
    if (m.find()) {
      basename = basename.replaceAll(regex.pattern(), "");
    }
    basename = basename + " ";

    // remove show name
    if (tvShowName != null && !tvShowName.isEmpty()) {
      // remove string like tvshow name (440, 24, ...)
      basename = basename.replaceAll("(?i)^" + Pattern.quote(tvShowName) + "", "");
    }
    basename = basename.replaceFirst("\\.\\w{1,4}$", ""); // remove extension if 1-4 chars
    basename = basename.replaceFirst("[\\(\\[]\\d{4}[\\)\\]]", ""); // remove (xxxx) or [xxxx] as year
    basename = basename.replaceFirst("[\\(\\[][A-Fa-f0-9]{8}[\\)\\]]", ""); // remove (xxxxxxxx) or [xxxxxxxx] as 8 byte crc

    return removeEpisodeVariantsFromTitle(basename);
  }

  private static String removeEpisodeVariantsFromTitle(String title) {
    StringBuilder backup = new StringBuilder(title);
    StringBuilder ret = new StringBuilder();

    // quite same patters as above, minus the last ()
    title = title.replaceAll("[Ss]([0-9]+)[\\]\\[ _.-]*[Ee]([0-9]+)", "");
    title = title.replaceAll("[ _.-]()[Ee][Pp]?_?([0-9]+)", "");
    title = title.replaceAll("([0-9]{4})[.-]([0-9]{2})[.-]([0-9]{2})", "");
    title = title.replaceAll("([0-9]{2})[.-]([0-9]{2})[.-]([0-9]{4})", "");
    title = title.replaceAll("[\\\\/\\._ \\[\\(-]([0-9]+)x([0-9]+)", "");
    title = title.replaceAll("[\\/ _.-]p(?:ar)?t[ _.-]()([ivx]+)", "");
    title = title.replaceAll("[epx_-]+(\\d{1,3})", "");
    title = title.replaceAll("episode[\\. _-]*(\\d{1,3})", "");
    title = title.replaceAll("(part|pt)[\\._\\s]+([MDCLXVI]+)", "");
    title = title.replaceAll("(staffel|season|saison|series|temporada)[\\s_.-]*(\\d{1,4})", "");
    title = title.replaceAll("s(\\d{1,4})[ ]?((?:([epx_.-]+\\d{1,3})+))", "");
    title = title.replaceAll("(\\d{1,4})(?=x)((?:([epx]+\\d{1,3})+))", "");

    // split and reassemble
    String[] splitted = StringUtils.split(title, "[\\[\\]() _,.-]");
    for (String s : splitted) {
      if (MediaIdUtil.isValidImdbId(s)) {
        s = ""; // remove IMDB ID from title
      }
      ret.append(" ").append(s);
    }
    ret = new StringBuilder(ret.toString().strip());

    // uh-oh - we removed too much
    // also split and reassemble backup
    if (StringUtils.isEmpty(ret.toString())) {
      String[] b = StringUtils.split(backup.toString(), "[\\[\\]() _,.-]");
      backup = new StringBuilder();
      for (String s : b) {
        backup.append(" ").append(s);
      }
      ret = new StringBuilder(backup.toString().strip());
    }
    return ret.toString();
  }

  /**
   * Does all the season/episode detection
   * 
   * @param name
   *          the RELATIVE filename (like /dir2/seas1/fname.ext) from the TvShowRoot
   * @param showname
   *          the show name
   * @return result the calculated result
   */
  public static EpisodeMatchingResult detectEpisodeFromFilename(String name, String showname) {

    // parse ANIME exclusively in front, unmodified
    EpisodeMatchingResult result = new EpisodeMatchingResult();
    String nameNoExt = name.replaceFirst("\\.\\w{1,4}$", ""); // remove extension if 1-4 chars
    result = parseAnimeExclusive(result, nameNoExt);
    if (!result.episodes.isEmpty()) {
      LOGGER.debug("parsed as Anime '{}'", name);
      return result;
    }

    // first check ONLY filename!
    result = detect(FilenameUtils.getName(name), showname);

    // only EPs found, but no season
    if (!result.episodes.isEmpty() && result.season == -1) {
      // try parsing whole string,
      EpisodeMatchingResult result2 = detect(name, showname);
      // and IF the detected episodes (from filename) are same AMOUNT, take it
      // (so a multifile folder pattern wont override a single file in there)
      if (result2.season != -1 && result2.episodes.size() == result.episodes.size()) {
        result = result2;
      }
      else {
        // take season only - rely on former filename only detection
        result.season = result2.season; // could also be -1, so here's a good point for checking Anime2
        // parse ANIME in workflow - w/o hashes regex
        result = parseAnimeNoHash(result, name);
      }
    }
    else if (result.episodes.isEmpty() && result.date == null) {
      // nothing found - check whole string as such
      result = detect(name, showname);
    }

    // we have found some valid episodes, but w/o season -> upgrade them for season 1
    if (!result.episodes.isEmpty() && !result.episodes.contains(-1) && result.season == -1) {
      result.season = 1;
    }

    return result;
  }

  /**
   * Does all the season/episode detection
   * 
   * @param name
   *          the RELATIVE filename (like /dir2/seas1/fname.ext) from the TvShowRoot
   * @param showname
   *          the show name
   * @return result the calculated result
   */
  private static EpisodeMatchingResult detect(String name, String showname) {
    LOGGER.debug("parsing '{}'", name);
    EpisodeMatchingResult result = new EpisodeMatchingResult();
    Pattern regex;
    Matcher m;

    // remove problematic strings from name
    String filename = FilenameUtils.getName(name);

    // check for disc files and remove!!
    if (filename.toLowerCase(Locale.ROOT).matches("(video_ts|vts_\\d\\d_\\d)\\.(vob|bup|ifo)") || // dvd
        filename.toLowerCase(Locale.ROOT).matches("(index\\.bdmv|movieobject\\.bdmv|\\d{5}\\.m2ts)")) { // bluray
      name = FilenameUtils.getPath(name);
    }

    String basename = ParserUtils.removeStopwordsAndBadwordsFromTvEpisodeName(name);
    String foldername = "";

    // parse foldername
    regex = Pattern.compile("(.*[\\/\\\\])");
    m = regex.matcher(basename);
    if (m.find()) {
      foldername = m.group(1);
      basename = basename.replaceAll(regex.pattern(), "");
    }

    // happens, when we only parse filename, but it completely gets stripped out.
    if (basename.isEmpty() && foldername.isEmpty()) {
      return result;
    }

    basename = basename.replaceFirst("\\.\\w{1,4}$", ""); // remove extension if 1-4 chars
    basename = basename.replaceFirst("[\\(\\[]\\d{4}[\\)\\]]", ""); // remove (xxxx) or [xxxx] as year
    basename = basename.replaceFirst("[\\(\\[][A-Fa-f0-9]{8}[\\)\\]]", ""); // remove (xxxxxxxx) or [xxxxxxxx] as 8 byte crc

    basename = " " + basename + " "; // ease regex parsing w/o ^$
    foldername = " " + foldername + " "; // ease regex parsing w/o ^$

    result.stackingMarkerFound = !Utils.getStackingMarker(filename).isEmpty();
    result.name = basename.strip();

    // parse all long named season names, and remove
    result = parseSeasonLong(result, basename + foldername);
    if (result.season != -1) {
      basename = basename.replaceAll("(?i)" + SEASON_LONG.toString(), "");
      foldername = foldername.replaceAll("(?i)" + SEASON_LONG.toString(), "");
    }
    result = parseSeasonMultiEP(result, basename + foldername);
    result = parseSeasonMultiEP2(result, basename + foldername);
    result = parseEpisodePattern(result, basename);

    if (!result.episodes.isEmpty()) {
      return postClean(result);
    }

    // since we parsed all long variants, now it is a good time to remove the show name, even something like "24"
    if (showname != null && !showname.isEmpty()) {
      // remove string like tvshow name (440, 24, ...)
      basename = basename.replaceAll("(?i)[^ES]" + Pattern.quote(showname), ""); // with our added space, but not prefixed with S/E
      foldername = foldername.replaceAll("(?i)[^ES]" + Pattern.quote(showname), ""); // with our added space, but not prefixed with S/E
      try {
        // Since this is the title, change all spaces to delimiter pattern!
        // "some fine show" would match with "some.fine-show"
        // since we generate a dynamic pattern, guard that with try/catch - the quote() from above would not work
        showname = showname.replaceAll("[ _.-]", "[ _.-]"); // replace all delimiters, with delimiters pattern ;)
        foldername = foldername.replaceAll("(?i)" + showname, "");
      }
      catch (Exception e) {
        // ignore
      }
    }

    // ======================================================================
    // After here are some weird detections
    // run them only, when we have NO result!!!
    // so we step out here...
    // ======================================================================
    result = parseRoman(result, basename);
    if (!result.episodes.isEmpty()) {
      return postClean(result);
    }
    result = parseDatePattern(result, basename);
    if (result.date != null) {
      // since we have a matching date, we wont find episodes solely by number
      return postClean(result);
    }

    // ======================================================================
    // After here are some really, REALLY generic detections.
    // Just take 3, 2 or 1 single number for episode
    // strip as many as we know... it's the last change to do some detection!
    // ======================================================================

    // ignore disc files
    MediaFile mf = new MediaFile();
    mf.setFilename(filename); // cant use Paths.get()
    if (mf.isDiscFile()) {
      return postClean(result);
    }

    // parse season short (S 01), but only if we do not have already one!
    if (result.season == -1) {
      result = parseSeasonOnly(result, basename + foldername);
      if (result.season != -1) {
        foldername = foldername.replaceAll("(?i)" + SEASON_ONLY.toString(), "");
        basename = basename.replaceAll("(?i)" + SEASON_ONLY.toString(), "");
      }
    }

    // parse episode short (EP 01), but only if we do not have already one!
    if (result.episodes.isEmpty()) {
      result = parseEpisodeOnly(result, basename);
    }
    if (!result.episodes.isEmpty()) {
      return postClean(result);
    }

    List<String> numbersOnly = new ArrayList<>();
    // strip all [optionals]!
    String optionals = "[\\[\\{](.*?)[\\]\\}]";
    String woOptionals = basename.replaceAll(optionals, "");
    numbersOnly.addAll(Arrays.asList(woOptionals.split("[\\s\\|_.-]"))); // split on our delimiters
    // now we should have numbers only in there - if we find something different - remove
    for (int i = numbersOnly.size() - 1; i >= 0; i--) {
      if (numbersOnly.get(i).isEmpty() || numbersOnly.get(i).matches(".*?\\D.*?")) {
        numbersOnly.remove(i);
      }
    }

    // nothing found removing []? try with optionals...
    if (numbersOnly.size() == 0) {
      regex = Pattern.compile(optionals); // only optionals
      m = regex.matcher(basename);
      while (m.find()) {
        String delimitedNumbers = " " + m.group(1) + " "; // ease regex
        numbersOnly.addAll(Arrays.asList(delimitedNumbers.split("[\\s\\|_.-]"))); // split on our delimiters
      }
    }
    // now we should have numbers only in there - if we find something different - remove
    for (int i = numbersOnly.size() - 1; i >= 0; i--) {
      if (numbersOnly.get(i).isEmpty() || numbersOnly.get(i).matches(".*?\\D.*?")) {
        numbersOnly.remove(i);
      }
    }

    // reverse array, so that the latter number is more significant (episode numbers are mostly at end)
    // (when having 2 or more equal length numbers)
    Collections.reverse(numbersOnly);

    result = parseNumbers4(result, numbersOnly);
    if (!result.episodes.isEmpty()) {
      return postClean(result);
    }
    result = parseNumbers3(result, numbersOnly);
    if (!result.episodes.isEmpty()) {
      return postClean(result);
    }
    result = parseNumbers2(result, numbersOnly);
    if (!result.episodes.isEmpty()) {
      return postClean(result);
    }
    result = parseNumbers1(result, numbersOnly);
    return postClean(result);
  }

  private static EpisodeMatchingResult parseNumbers1(EpisodeMatchingResult result, List<String> numbersOnly) {
    for (String num : numbersOnly) {
      if (num.length() == 1) {
        int ep = Integer.parseInt(num); // just one :P
        if (ep > 0 && !result.episodes.contains(ep)) {
          result.episodes.add(ep);
          LOGGER.trace("add found EP '{}'", ep);
        }
        return result;
      }
    }
    return result;
  }

  private static EpisodeMatchingResult parseNumbers2(EpisodeMatchingResult result, List<String> numbersOnly) {
    for (String num : numbersOnly) {
      if (num.length() == 2) {
        // Filename contains only 2 subsequent numbers; parse this as EE
        int ep = Integer.parseInt(num);
        if (ep > 0 && !result.episodes.contains(ep)) {
          result.episodes.add(ep);
          LOGGER.trace("add found EP '{}'", ep);
        }
        return result;
      }
    }
    return result;
  }

  private static EpisodeMatchingResult parseNumbers3(EpisodeMatchingResult result, List<String> numbersOnly) {
    for (String num : numbersOnly) {
      if (num.length() == 3) {
        // Filename contains only 3 subsequent numbers; parse this as SEE
        int s = Integer.parseInt(num.substring(0, 1));
        int ep = Integer.parseInt(num.substring(1));
        if (result.season == -1 || result.season == s) {
          if (ep > 0 && !result.episodes.contains(ep)) {
            result.episodes.add(ep);
            LOGGER.trace("add found EP '{}'", ep);
          }
          LOGGER.trace("add found season '{}'", s);
          result.season = s;
        }
        // for 3 character numbers, we iterate multiple times (with same season)!
        // do not stop on first one"
        // return result;
      }
    }
    return result;
  }

  private static EpisodeMatchingResult parseNumbers4(EpisodeMatchingResult result, List<String> numbersOnly) {
    for (String num : numbersOnly) {
      if (num.length() == 4) {
        // Filename contains only 4 subsequent numbers; parse this as SSEE
        int s = Integer.parseInt(num.substring(0, 2));
        int ep = Integer.parseInt(num.substring(2));
        if (result.season == s) { // we NEED to have a season set, else every year would be a valid SSEE number!!
          if (ep > 0 && !result.episodes.contains(ep)) {
            result.episodes.add(ep);
            LOGGER.trace("add found EP '{}'", ep);
          }
        }
        // for 4 character numbers, we iterate multiple times (with same season)!
        // do not stop on first one"
        // return result;
      }
    }
    return result;
  }

  private static EpisodeMatchingResult parseDatePattern(EpisodeMatchingResult result, String name) {
    Matcher m;
    // Date1 pattern yyyy-mm-dd
    m = DATE_1.matcher(name);
    if (m.find()) {
      int s = result.season;
      try {
        s = Integer.parseInt(m.group(1));
        result.date = new SimpleDateFormat("yyyy-MM-dd").parse(m.group(1) + "-" + m.group(2) + "-" + m.group(3));
      }
      catch (NumberFormatException | ParseException nfe) {
        // can not happen from regex since we only come here with max 2 numeric chars
      }
      result.season = s;
      LOGGER.trace("add found year as season '{}', date: '{}'", s, result.date);
      return result; // since we have a matching year, we wont find episodes solely by number
    }
    // Date2 pattern dd-mm-yyyy
    m = DATE_2.matcher(name);
    if (m.find()) {
      int s = result.season;
      try {
        s = Integer.parseInt(m.group(3));
        result.date = new SimpleDateFormat("dd-MM-yyyy").parse(m.group(1) + "-" + m.group(2) + "-" + m.group(3));
      }
      catch (NumberFormatException | ParseException nfe) {
        // can not happen from regex since we only come here with max 2 numeric chars
      }
      result.season = s;
      LOGGER.trace("add found year as season '{}', date: '{}'", s, result.date);
      return result; // since we have a matching year, we wont find episodes solely by number
    }

    return result;
  }

  private static EpisodeMatchingResult parseRoman(EpisodeMatchingResult result, String name) {
    Pattern regex;
    Matcher m;
    // parse Roman only when not found anything else!!
    if (result.episodes.isEmpty()) {
      regex = ROMAN_PATTERN;
      m = regex.matcher(name);
      while (m.find()) {
        int ep = 0;
        ep = decodeRoman(m.group(2));
        if (ep > 0 && !result.episodes.contains(ep)) {
          result.episodes.add(ep);
          LOGGER.trace("add found EP '{}'", ep);
        }
      }
    }
    return result;
  }

  private static EpisodeMatchingResult parseEpisodePattern(EpisodeMatchingResult result, String name) {
    Pattern regex;
    Matcher m;
    // Episode-only parsing, when previous styles didn't find anything!
    if (result.episodes.isEmpty()) {
      regex = EPISODE_PATTERN_2;
      m = regex.matcher(name);
      while (m.find()) {
        int ep = 0;
        try {
          ep = Integer.parseInt(m.group(1));
        }
        catch (NumberFormatException nfe) {
          // can not happen from regex since we only come here with max 2 numeric chars
        }
        if (ep > 0 && !result.episodes.contains(ep)) {
          result.episodes.add(ep);
          LOGGER.trace("add found EP '{}'", ep);
        }
      }
    }
    return result;
  }

  private static EpisodeMatchingResult parseSeasonMultiEP2(EpisodeMatchingResult result, String name) {
    Pattern regex;
    Matcher m;
    // parse XYY or XX_YY 1-N
    regex = SEASON_MULTI_EP_2;
    m = regex.matcher(name);
    while (m.find()) {
      int s = -1;
      try {
        // for the case of name.1x02x03.ext
        if (m.group(2) != null && result.season == -1) {
          s = Integer.parseInt(m.group(1));
        }
        String eps = m.group(2); // name.s01"ep02-02-04".ext
        // now we have a string of 1-N episodes - parse them
        Pattern regex2 = EPISODE_PATTERN; // episode fixed to 1-2 chars
        Matcher m2 = regex2.matcher(eps);
        while (m2.find()) {
          int ep = 0;
          try {
            ep = Integer.parseInt(m2.group(1));
          }
          catch (NumberFormatException nfe) {
            // can not happen from regex since we only come here with max 2 numeric chars
          }
          if (ep > 0 && !result.episodes.contains(ep)) {
            result.episodes.add(ep);
            LOGGER.trace("add found EP '{}'", ep);
          }
        }
      }
      catch (NumberFormatException nfe) {
        // can not happen from regex since we only come here with max 2 numeric chars
      }
      if (s >= 0) {
        result.season = s;
        LOGGER.trace("add found season '{}'", s);
      }
    }
    return result;
  }

  private static EpisodeMatchingResult parseSeasonMultiEP(EpisodeMatchingResult result, String name) {
    Pattern regex;
    Matcher m;
    // parse SxxEPyy 1-N
    regex = SEASON_MULTI_EP;
    m = regex.matcher(name);
    while (m.find()) {
      int s = -1;
      try {
        s = Integer.parseInt(m.group(1));
        String eps = m.group(2); // name.s01"ep02-02-04".ext
        // now we have a string of 1-N episodes - parse them
        Pattern regex2 = EPISODE_PATTERN; // episode fixed to 1-2 chars
        Matcher m2 = regex2.matcher(eps);
        while (m2.find()) {
          int ep = -1;
          try {
            ep = Integer.parseInt(m2.group(1));
          }
          catch (NumberFormatException nfe) {
            // can not happen from regex since we only come here with max 2 numeric chars
          }
          // check if the found episode is not -1 (0 allowed!), not already in the list and if multi episode
          if (ep > -1 && !result.episodes.contains(ep)) {
            result.episodes.add(ep);
            LOGGER.trace("add found EP '{}'", ep);
          }
        }
      }
      catch (NumberFormatException nfe) {
        // can not happen from regex since we only come here with max 2 numeric chars
      }
      if (s >= 0) {
        result.season = s;
        LOGGER.trace("add found season '{}", s);
      }
    }
    return result;
  }

  private static EpisodeMatchingResult parseEpisodeOnly(EpisodeMatchingResult result, String name) {
    Matcher m;
    m = EPISODE_ONLY.matcher(name);
    if (m.find()) {
      try {
        int e = Integer.parseInt(m.group(1));
        result.episodes.add(e);
        LOGGER.trace("add found episode '{}'", e);
      }
      catch (NumberFormatException nfe) {
        // ignore
      }
    }
    return result;
  }

  private static EpisodeMatchingResult parseSeasonLong(EpisodeMatchingResult result, String name) {
    Matcher m;
    // season detection
    if (result.season == -1) {
      m = SEASON_LONG.matcher(name);
      if (m.find()) {
        int s = result.season;
        try {
          s = Integer.parseInt(m.group(2));
        }
        catch (NumberFormatException nfe) {
          // can not happen from regex since we only come here with max 2 numeric chars
        }
        result.season = s;
        LOGGER.trace("add found season '{}'", s);
      }
    }
    return result;
  }

  private static EpisodeMatchingResult parseSeasonOnly(EpisodeMatchingResult result, String name) {
    Matcher m;
    // season detection
    if (result.season == -1) {
      m = SEASON_ONLY.matcher(name);
      if (m.find()) {
        int s = result.season;
        try {
          s = Integer.parseInt(m.group(1));
        }
        catch (NumberFormatException nfe) {
          // can not happen from regex since we only come here with max 2 numeric chars
        }
        result.season = s;
        LOGGER.trace("add found season '{}'", s);
      }
    }
    return result;
  }

  private static EpisodeMatchingResult parseAnimeExclusive(EpisodeMatchingResult result, String name) {
    Matcher m;
    // Anything with the filename marked as Special/OVA/OAV/etc goes to season 0, regardless of what the directory may say
    m = ANIME_PREPEND1.matcher(name);
    if (m.find()) {
      try {
        int ep = Integer.parseInt(m.group(2));
        result.episodes.add(ep);
        result.season = 0;
      }
      catch (NumberFormatException nfe) {
      }
    }

    if (result.episodes.isEmpty()) {
      // Inside a directory that specifies the season. May include any number of subdirectories. Doesn't try to find season markers in the file name
      m = ANIME_PREPEND2.matcher(name);
      if (m.find()) {
        try {
          int ep = Integer.parseInt(m.group(2));
          result.episodes.add(ep);
          result.season = Integer.parseInt(m.group(1));
        }
        catch (NumberFormatException nfe) {
        }
      }
    }

    if (result.episodes.isEmpty()) {
      // Include season marker in the filename
      m = ANIME_PREPEND3.matcher(name);
      if (m.find()) {
        try {
          int ep = Integer.parseInt(m.group(2));
          result.episodes.add(ep);
          result.season = Integer.parseInt(m.group(1));
        }
        catch (NumberFormatException nfe) {
        }
      }
    }

    if (result.episodes.isEmpty()) {
      // Anything else gets the default blank first capture, which sets the file to season 1
      m = ANIME_PREPEND4.matcher(name);
      if (m.find()) {
        try {
          int ep = Integer.parseInt(m.group(2));
          result.episodes.add(ep);
          result.season = 1;
        }
        catch (NumberFormatException nfe) {
        }
      }
    }
    return result;
  }

  private static EpisodeMatchingResult parseAnimeNoHash(EpisodeMatchingResult result, String name) {
    Matcher m;
    // Anything with the filename marked as Special/OVA/OAV/etc goes to season 0, regardless of what the directory may say
    m = ANIME_APPEND1.matcher(name);
    if (m.find()) {
      try {
        int ep = Integer.parseInt(m.group(2));
        if (!result.episodes.contains(ep)) {
          result.episodes.add(ep);
        }
        result.season = 0;
      }
      catch (NumberFormatException nfe) {
      }
    }

    if (result.episodes.isEmpty() || result.season == -1) {
      // Inside a directory that specifies the season. May include any number of subdirectories. Doesn't try to find season markers in the file name
      m = ANIME_APPEND2.matcher(name);
      if (m.find()) {
        try {
          int ep = Integer.parseInt(m.group(2));
          if (!result.episodes.contains(ep)) {
            result.episodes.add(ep);
          }
          result.season = Integer.parseInt(m.group(1));
        }
        catch (NumberFormatException nfe) {
        }
      }
    }

    if (result.episodes.isEmpty() || result.season == -1) {
      // Include season marker in the filename
      m = ANIME_APPEND3.matcher(name);
      if (m.find()) {
        try {
          int ep = Integer.parseInt(m.group(2));
          if (!result.episodes.contains(ep)) {
            result.episodes.add(ep);
          }
          result.season = Integer.parseInt(m.group(1));
        }
        catch (NumberFormatException nfe) {
        }
      }
    }

    // as ANINE it would set episode 1 on undetectable
    // but that interferes with our -1 approach
    // so we must not use the append4 pattern!
    // changed as of 20240803
    if (result.episodes.isEmpty() || result.season == -1) {
      // Anything else gets the default blank first capture, which sets the file to season 1
      m = ANIME_APPEND4.matcher(name);
      if (m.find()) {
        try {
          int ep = Integer.parseInt(m.group(2));
          if (!result.episodes.contains(ep)) {
            result.episodes.add(ep);
          }
          result.season = 1;
        }
        catch (NumberFormatException nfe) {
        }
      }
    }
    return result;
  }

  private static EpisodeMatchingResult postClean(EpisodeMatchingResult emr) {
    // try to clean the filename
    emr.cleanedName = cleanFilename(emr.name, new Pattern[] { SEASON_LONG, SEASON_MULTI_EP, SEASON_MULTI_EP_2, EPISODE_PATTERN, EPISODE_PATTERN_2,
        NUMBERS_3_PATTERN, NUMBERS_2_PATTERN, ROMAN_PATTERN, DATE_1, DATE_2, SEASON_ONLY });
    Collections.sort(emr.episodes);
    LOGGER.trace("returning result '{}'", emr);
    return emr;
  }

  private static String cleanFilename(String name, Pattern[] patterns) {
    String result = name;
    for (Pattern pattern : patterns) {
      Matcher matcher = pattern.matcher(result);
      if (matcher.find()) {
        result = matcher.replaceFirst("");
      }
    }

    // last but not least, clean all leading/trailing separators
    result = result.replaceAll("^[ \\.\\-_]+", "");
    result = result.replaceAll("[ \\.\\-_]+$", "");

    return result;
  }

  /**
   * Decode single roman.
   * 
   * @param letter
   *          the letter
   * @return the int
   */
  private static int decodeSingleRoman(char letter) {
    switch (letter) {
      case 'M':
        return 1000;

      case 'D':
        return 500;

      case 'C':
        return 100;

      case 'L':
        return 50;

      case 'X':
        return 10;

      case 'V':
        return 5;

      case 'I':
        return 1;

      default:
        return 0;
    }
  }

  /**
   * Decode roman.
   * 
   * @param roman
   *          the roman
   * @return the int
   */
  public static int decodeRoman(String roman) {
    int result = 0;
    String uRoman = roman.toUpperCase(Locale.ROOT); // case-insensitive
    for (int i = 0; i < uRoman.length() - 1; i++) {// loop over all but the last
                                                   // character
      // if this character has a lower value than the next character
      if (decodeSingleRoman(uRoman.charAt(i)) < decodeSingleRoman(uRoman.charAt(i + 1))) {
        // subtract it
        result -= decodeSingleRoman(uRoman.charAt(i));
      }
      else {
        // add it
        result += decodeSingleRoman(uRoman.charAt(i));
      }
    }
    // decode the last character, which is always added
    result += decodeSingleRoman(uRoman.charAt(uRoman.length() - 1));
    return result;
  }

  /******************************************************************************************
   * helper classes
   ******************************************************************************************/
  public static class EpisodeMatchingResult {

    public int           season              = -1;
    public List<Integer> episodes            = new ArrayList<>();
    public String        name                = "";
    public String        cleanedName         = "";
    public Date          date                = null;
    public boolean       stackingMarkerFound = false;

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
  }
}
