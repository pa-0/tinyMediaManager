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

import java.text.RuleBasedCollator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.scraper.DynaEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * This enum represents all different types of episode editions
 *
 * @author Manuel Laggner
 */
public class TvShowEpisodeEdition extends DynaEnum<TvShowEpisodeEdition> {
  private static final Comparator<TvShowEpisodeEdition> COMPARATOR    = new TvShowEditionComparator();

  public static final TvShowEpisodeEdition              NONE          = new TvShowEpisodeEdition("NONE", 0, "", "");
  public static final TvShowEpisodeEdition              DIRECTORS_CUT = new TvShowEpisodeEdition("DIRECTORS_CUT", 1, "Director's Cut",
      ".Director.?s.(Cut|Edition|Version)");

  private final String                                  title;
  private final Pattern                                 pattern;

  private TvShowEpisodeEdition(String enumName, int ordinal, String title, String pattern) {
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
   * get all episode editions
   *
   * @return an array of all episode editions
   */
  public static TvShowEpisodeEdition[] values() {
    TvShowEpisodeEdition[] episodeEditions = values(TvShowEpisodeEdition.class);
    Arrays.sort(episodeEditions, COMPARATOR);
    return episodeEditions;
  }

  /**
   * Parse the given string for an appropriate episode edition
   *
   * @param name
   *          the string to parse out the episode edition
   * @return the found edition
   */
  public static TvShowEpisodeEdition getTvShowEpisodeEditionFromString(String name) {
    // split checks for performance
    for (TvShowEpisodeEdition edition : values()) {
      // check if the "enum" name matches
      if (edition.name().equals(name)) {
        return edition;
      }
    }

    // check if the printable name matches
    for (TvShowEpisodeEdition edition : values()) {
      if (edition.title.equalsIgnoreCase(name)) {
        return edition;
      }
    }

    // check if any regular expression matches
    for (TvShowEpisodeEdition edition : values()) {
      if (edition.pattern != null) {
        Matcher matcher = edition.pattern.matcher(name);
        if (matcher.find()) {
          return edition;
        }
      }
    }

    return NONE;
  }

  /**
   * Gets the right episode edition for the given string.
   *
   * @param name
   *          the name
   * @return the episode edition
   */
  public static TvShowEpisodeEdition getTvShowEpisodeEdition(String name) {
    // empty or strict loading of NONE
    if (StringUtils.isBlank(name) || "NONE".equals(name)) {
      return NONE;
    }

    TvShowEpisodeEdition edition = getTvShowEpisodeEditionFromString(name);
    if (edition == NONE) {
      // dynamically create new one
      edition = new TvShowEpisodeEdition(name, values().length, name, "");
    }

    return edition;
  }

  /**
   * Gets the right episode edition for the given string - strict version. Only check the enum name()
   *
   * @param name
   *          the name
   * @return the episode edition
   */
  @JsonCreator
  public static TvShowEpisodeEdition getTvShowEpisodeEditionStrict(String name) {
    // empty or strict loading of NONE
    if (StringUtils.isBlank(name) || "NONE".equals(name)) {
      return NONE;
    }

    TvShowEpisodeEdition edition = NONE;

    for (TvShowEpisodeEdition episodeEdition : values()) {
      // check if the "enum" name matches
      if (episodeEdition.name().equals(name)) {
        edition = episodeEdition;
      }
    }

    if (edition == NONE) {
      // dynamically create new one
      edition = new TvShowEpisodeEdition(name, values().length, name, "");
    }

    return edition;
  }

  /**
   * Comparator for sorting our editions in a localized fashion
   */
  public static class TvShowEditionComparator implements Comparator<TvShowEpisodeEdition> {
    private final RuleBasedCollator stringCollator = (RuleBasedCollator) RuleBasedCollator.getInstance();

    @Override
    public int compare(TvShowEpisodeEdition o1, TvShowEpisodeEdition o2) {
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
  public static void addListener(DynaEnumEventListener<TvShowEpisodeEdition> listener) {
    addListener(TvShowEpisodeEdition.class, listener);
  }

  /**
   * remove the given DynaEnumEventListener
   *
   * @param listener
   *          the listener to be removed
   */
  public static void removeListener(DynaEnumEventListener<TvShowEpisodeEdition> listener) {
    removeListener(TvShowEpisodeEdition.class, listener);
  }
}
