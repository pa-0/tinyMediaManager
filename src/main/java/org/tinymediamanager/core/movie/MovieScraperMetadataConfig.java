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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;

/**
 * The enum MovieScraperMetadataConfig is used to control which fields will be set after scraping
 * 
 * @author Manuel Laggner
 */
public enum MovieScraperMetadataConfig implements ScraperMetadataConfig {
  // meta data
  ID(Type.METADATA, "metatag.id"),
  TITLE(Type.METADATA),
  ORIGINAL_TITLE(Type.METADATA, "metatag.originaltitle"),
  TAGLINE(Type.METADATA),
  PLOT(Type.METADATA),
  YEAR(Type.METADATA),
  RELEASE_DATE(Type.METADATA, "metatag.releasedate"),
  RATING(Type.METADATA),
  TOP250(Type.METADATA),
  RUNTIME(Type.METADATA),
  CERTIFICATION(Type.METADATA),
  GENRES(Type.METADATA, "metatag.genre"),
  SPOKEN_LANGUAGES(Type.METADATA, "metatag.language"),
  COUNTRY(Type.METADATA),
  PRODUCTION_COMPANY(Type.METADATA, "metatag.studio"),
  TAGS(Type.METADATA),
  COLLECTION(Type.METADATA, "metatag.movieset", "Settings.movieset.scraper.hint"),
  TRAILER(Type.METADATA),

  // cast
  ACTORS(Type.CAST),
  PRODUCERS(Type.CAST),
  DIRECTORS(Type.CAST),
  WRITERS(Type.CAST),

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
  EXTRAFANART(Type.ARTWORK),
  EXTRATHUMB(Type.ARTWORK);

  private final Type   type;
  private final String description;
  private final String tooltip;

  MovieScraperMetadataConfig(Type type) {
    this(type, null, null);
  }

  MovieScraperMetadataConfig(Type type, String description) {
    this(type, description, null);
  }

  MovieScraperMetadataConfig(Type type, String description, String tooltip) {
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
        if (type == Type.ARTWORK) {
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
   * get a {@link List} of all {@link MovieScraperMetadataConfig} for the given {@link org.tinymediamanager.core.ScraperMetadataConfig.Type}
   *
   * @param type
   *          the {@link org.tinymediamanager.core.ScraperMetadataConfig.Type} to get all {@link MovieScraperMetadataConfig}s for
   * @return a {@link List} with all matching {@link MovieScraperMetadataConfig}s
   */
  public static List<MovieScraperMetadataConfig> valuesForType(Type type) {
    List<MovieScraperMetadataConfig> values = new ArrayList<>();

    for (MovieScraperMetadataConfig config : values()) {
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
  public static List<MovieScraperMetadataConfig> getValues() {
    List<MovieScraperMetadataConfig> values = new ArrayList<>();

    for (MovieScraperMetadataConfig value : values()) {
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
  public static List<MovieScraperMetadataConfig> getValuesWithout(MovieScraperMetadataConfig... valuesToExclude) {
    List<MovieScraperMetadataConfig> values = new ArrayList<>();
    List<MovieScraperMetadataConfig> exclude = Arrays.asList(valuesToExclude);

    for (MovieScraperMetadataConfig value : values()) {
      if (value.type != Type.DEPRECATED && !exclude.contains(value)) {
        values.add(value);
      }
    }

    return values;
  }
}
