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

import java.text.RuleBasedCollator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.scraper.DynaEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * This enum represents all different types of movie editions
 *
 * @author Manuel Laggner
 */
public class MovieEdition extends DynaEnum<MovieEdition> {
  private static final Comparator<MovieEdition> COMPARATOR           = new MovieEditionComparator();

  public static final MovieEdition              NONE                 = new MovieEdition("NONE", 0, "", "");
  public static final MovieEdition              DIRECTORS_CUT        = new MovieEdition("DIRECTORS_CUT", 1, "Director's Cut",
      ".Director.?s.(Cut|Edition|Version)");
  public static final MovieEdition              EXTENDED_EDITION     = new MovieEdition("EXTENDED_EDITION", 2, "Extended Edition",
      ".Extended.(Cut|Edition|Version)?");
  public static final MovieEdition              THEATRICAL_EDITION   = new MovieEdition("THEATRICAL_EDITION", 3, "Theatrical Edition",
      ".Theatrical.(Cut|Edition|Version)?");
  public static final MovieEdition              UNRATED              = new MovieEdition("UNRATED", 4, "Unrated", ".Unrated.(Cut|Edition|Version)?");
  public static final MovieEdition              UNCUT                = new MovieEdition("UNCUT", 5, "Uncut", ".Uncut.(Cut|Edition|Version)?");
  public static final MovieEdition              IMAX                 = new MovieEdition("IMAX", 6, "IMAX", "^(IMAX|.*?.IMAX).(Cut|Edition|Version)?");
  public static final MovieEdition              REMASTERED           = new MovieEdition("REMASTERED", 7, "Remastered",
      ".Remastered.(Cut|Edition|Version)?");
  public static final MovieEdition              COLLECTORS_EDITION   = new MovieEdition("COLLECTORS_EDITION", 8, "Collectors Edition",
      ".Collectors.(Cut|Edition|Version)");
  public static final MovieEdition              ULTIMATE_EDITION     = new MovieEdition("ULTIMATE_EDITION", 9, "Ultimate Edition",
      ".Ultimate.(Cut|Edition|Version)");
  public static final MovieEdition              FINAL_CUT            = new MovieEdition("FINAL_CUT", 10, "Final Edition",
      ".Final.(Cut|Edition|Version)");
  public static final MovieEdition              SPECIAL_EDITION      = new MovieEdition("SPECIAL_EDITION", 11, "Special Edition",
      ".Special.(Cut|Edition|Version)");
  public static final MovieEdition              CRITERION_COLLECTION = new MovieEdition("CRITERION_COLLECTION", 12, "Criterion Collection",
      ".Criterion.(Collection|Edition)?");
  public static final MovieEdition              OPEN_MATTE           = new MovieEdition("OPEN_MATTE", 13, "Open Matte",
      ".Open.Matte.(Cut|Edition|Version)?");

  private static final Pattern                  FILENAME_PATTERN     = Pattern.compile("\\{edition\\-(.*?)\\}", Pattern.CASE_INSENSITIVE);
  private final String                          title;
  private final Pattern                         pattern;

  private MovieEdition(String enumName, int ordinal, String title, String pattern) {
    super(enumName, ordinal);
    this.title = title;
    if (StringUtils.isBlank(pattern)) {
      this.pattern = null;
    }
    else {
      this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    addElement();
  }

  @Override
  public String toString() {
    return title;
  }

  public String getTitle() {
    return title;
  }

  @JsonValue
  public String getName() {
    return name();
  }

  /**
   * get all movie editions
   *
   * @return an array of all movie editions
   */
  public static MovieEdition[] values() {
    MovieEdition[] movieEditions = values(MovieEdition.class);
    Arrays.sort(movieEditions, COMPARATOR);
    return movieEditions;
  }

  /**
   * Parse the given string for an appropriate movie edition (via name & regexp)
   *
   * @param name
   *          the string to parse out the movie edition
   * @return the found edition
   */
  public static MovieEdition getMovieEditionFromString(String name) {
    // sort editions by ordinal
    List<MovieEdition> editions = Arrays.asList(values(MovieEdition.class)); // avoid sorting via text
    editions.sort(Comparator.comparingInt(o -> o.ordinal));

    // split checks for performance
    for (MovieEdition edition : editions) {
      // check if the "enum" name matches
      if (edition.name().equals(name)) {
        return edition;
      }
    }

    // check if the printable name matches
    for (MovieEdition edition : editions) {
      if (edition.title.equalsIgnoreCase(name)) {
        return edition;
      }
    }

    // check if any regular expression matches
    for (MovieEdition edition : editions) {
      if (edition.pattern != null) {
        Matcher matcher = edition.pattern.matcher(name);
        if (matcher.find()) {
          return edition;
        }
      }
    }

    // try parsing filename pattern, and create a new one
    Matcher matcher = FILENAME_PATTERN.matcher(name);
    if (matcher.find()) {
      MovieEdition me = getMovieEditionFromString(matcher.group(1)); // call ourself
      if (me == NONE) {
        return new MovieEdition(matcher.group(1), values().length, matcher.group(1), "");
      }
      else {
        return me;
      }
    }
    return NONE;
  }

  /**
   * Gets the right movie edition for the given string.
   *
   * @param name
   *          the name
   * @return the movie edition
   */
  public static MovieEdition getMovieEdition(String name) {
    // empty or strict loading of NONE
    if (StringUtils.isBlank(name) || "NONE".equals(name)) {
      return NONE;
    }

    MovieEdition edition = getMovieEditionFromString(name);
    if (edition == NONE) {
      // dynamically create new one
      edition = new MovieEdition(name, values().length, name, "");
    }

    return edition;
  }

  /**
   * Gets the right movie edition for the given string - strict version. Only check the enum name()
   *
   * @param name
   *          the name
   * @return the movie edition
   */
  @JsonCreator
  public static MovieEdition getMovieEditionStrict(String name) {
    // empty or strict loading of NONE
    if (StringUtils.isBlank(name) || "NONE".equals(name)) {
      return NONE;
    }

    MovieEdition edition = NONE;

    for (MovieEdition movieEdition : values()) {
      // check if the "enum" name matches
      if (movieEdition.name().equals(name)) {
        edition = movieEdition;
      }
    }

    if (edition == NONE) {
      // dynamically create new one
      edition = new MovieEdition(name, values().length, name, "");
    }

    return edition;
  }

  /**
   * Comparator for sorting our MovieEditions in a localized fashion
   */
  public static class MovieEditionComparator implements Comparator<MovieEdition> {
    private final RuleBasedCollator stringCollator = (RuleBasedCollator) RuleBasedCollator.getInstance();

    @Override
    public int compare(MovieEdition o1, MovieEdition o2) {
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
      return stringCollator.compare(o1.toString().toLowerCase(Locale.ROOT), o2.toString().toLowerCase(Locale.ROOT));
    }
  }

  /**
   * add a new DynaEnumEventListener. This listener will be informed if any new value has been added
   *
   * @param listener
   *          the new listener to be added
   */
  public static void addListener(DynaEnumEventListener<MovieEdition> listener) {
    addListener(MovieEdition.class, listener);
  }

  /**
   * remove the given DynaEnumEventListener
   *
   * @param listener
   *          the listener to be removed
   */
  public static void removeListener(DynaEnumEventListener<MovieEdition> listener) {
    removeListener(MovieEdition.class, listener);
  }
}
