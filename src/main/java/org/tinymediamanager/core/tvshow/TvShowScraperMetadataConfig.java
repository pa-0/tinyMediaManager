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
package org.tinymediamanager.core.tvshow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;

/**
 * The enum TvShowScraperMetadataConfig is used to control which TV show fields should be set after scraping.
 * 
 * @author Manuel Laggner
 */
public enum TvShowScraperMetadataConfig implements ScraperMetadataConfig {
  // meta data
  ID(Type.METADATA),
  TITLE(Type.METADATA),
  ORIGINAL_TITLE(Type.METADATA, "metatag.originaltitle"),
  PLOT(Type.METADATA),
  YEAR(Type.METADATA),
  AIRED(Type.METADATA, "metatag.aired"),
  STATUS(Type.METADATA),
  RATING(Type.METADATA),
  TOP250(Type.METADATA),
  RUNTIME(Type.METADATA),
  CERTIFICATION(Type.METADATA),
  GENRES(Type.METADATA, "metatag.genre"),
  COUNTRY(Type.METADATA),
  STUDIO(Type.METADATA, "metatag.studio"),
  TAGS(Type.METADATA),
  TRAILER(Type.METADATA),
  SEASON_NAMES(Type.METADATA, "metatag.seasonname"),
  SEASON_OVERVIEW(Type.METADATA, "metatag.seasonoverview"),

  // cast
  ACTORS(Type.CAST),

  // artwork
  POSTER(Type.ARTWORK),
  FANART(Type.ARTWORK),
  BANNER(Type.ARTWORK),
  CLEARART(Type.ARTWORK),
  THUMB(Type.ARTWORK),
  LOGO(Type.DEPRECATED),
  CLEARLOGO(Type.ARTWORK),
  DISCART(Type.ARTWORK, "mediafiletype.disc"),
  KEYART(Type.ARTWORK),
  CHARACTERART(Type.ARTWORK),
  EXTRAFANART(Type.ARTWORK),

  SEASON_POSTER(Type.ARTWORK),
  SEASON_FANART(Type.ARTWORK),
  SEASON_BANNER(Type.ARTWORK),
  SEASON_THUMB(Type.ARTWORK),

  // theme
  THEME(Type.THEME);

  private final Type   type;
  private final String description;
  private final String tooltip;

  TvShowScraperMetadataConfig(Type type) {
    this(type, null, null);
  }

  TvShowScraperMetadataConfig(Type type, String description) {
    this(type, description, null);
  }

  TvShowScraperMetadataConfig(Type type, String description, String tooltip) {
    this.type = type;
    this.description = description;
    this.tooltip = tooltip;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String getDescription() {
    if (StringUtils.isBlank(description)) {
      try {
        if (type == Type.ARTWORK || type == Type.THEME) {
          return TmmResourceBundle.getString("mediafiletype." + name().toLowerCase(Locale.ROOT));
        }
        else {
          return TmmResourceBundle.getString("metatag." + name().toLowerCase(Locale.ROOT));
        }
      }
      catch (Exception ignored) {
        // just not crash
      }
    }
    else {
      try {
        return TmmResourceBundle.getString(description);
      }
      catch (Exception ignored) {
        // just not crash
      }
    }
    return "";
  }

  @Override
  public String getToolTip() {
    if (StringUtils.isBlank(tooltip)) {
      return null;
    }
    try {
      return TmmResourceBundle.getString(tooltip);
    }
    catch (Exception ignored) {
      // just not crash
    }
    return null;
  }

  /**
   * get a {@link List} of all {@link TvShowScraperMetadataConfig} for the given {@link org.tinymediamanager.core.ScraperMetadataConfig.Type}
   * 
   * @param type
   *          the {@link org.tinymediamanager.core.ScraperMetadataConfig.Type} to get all {@link TvShowScraperMetadataConfig}s for
   * @return a {@link List} with all matching {@link TvShowScraperMetadataConfig}s
   */
  public static List<TvShowScraperMetadataConfig> valuesForType(Type type) {
    List<TvShowScraperMetadataConfig> values = new ArrayList<>();

    for (TvShowScraperMetadataConfig config : values()) {
      if (config.type == type) {
        values.add(config);
      }
    }

    return values;
  }

  /**
   * get all values except the deprecated ones
   *
   * @return a {@link List} of all values except deprecated ones
   */
  public static List<TvShowScraperMetadataConfig> getValues() {
    List<TvShowScraperMetadataConfig> values = new ArrayList<>();

    for (TvShowScraperMetadataConfig value : values()) {
      if (value.type != Type.DEPRECATED) {
        values.add(value);
      }
    }

    return values;
  }

  /**
   * get all values except the given and deprecated ones
   *
   * @param valuesToExclude
   *          values to exclude from the list
   *
   * @return a {@link List} of all values except the given and deprecated ones
   */
  public static List<TvShowScraperMetadataConfig> getValuesWithout(TvShowScraperMetadataConfig... valuesToExclude) {
    List<TvShowScraperMetadataConfig> values = new ArrayList<>();
    List<TvShowScraperMetadataConfig> exclude = Arrays.asList(valuesToExclude);

    for (TvShowScraperMetadataConfig value : values()) {
      if (value.type != Type.DEPRECATED && !exclude.contains(value)) {
        values.add(value);
      }
    }

    return values;
  }
}
