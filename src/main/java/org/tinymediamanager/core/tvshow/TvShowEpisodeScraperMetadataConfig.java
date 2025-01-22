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

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * The enum TvShowEpisodeScraperMetadataConfig is used to control which episode fields should be set after scraping.
 * 
 * @author Manuel Laggner
 */
public enum TvShowEpisodeScraperMetadataConfig implements ScraperMetadataConfig {
  // meta data
  TITLE(Type.METADATA),
  ORIGINAL_TITLE(Type.METADATA, "metatag.originaltitle"),
  PLOT(Type.METADATA),
  @JsonAlias({ "AIRED_SEASON_EPISODE", "DVD_SEASON_EPISODE", "DISPLAY_SEASON_EPISODE" })
  SEASON_EPISODE(Type.METADATA, "tvshow.seasonepisode"),
  AIRED(Type.METADATA, "metatag.aired"),
  RATING(Type.METADATA),
  TAGS(Type.METADATA),

  // cast
  ACTORS(Type.CAST),
  DIRECTORS(Type.CAST),
  WRITERS(Type.CAST),
  @Deprecated
  PRODUCERS(Type.DEPRECATED),

  // artwork
  THUMB(Type.ARTWORK);

  private final Type   type;
  private final String description;
  private final String tooltip;

  TvShowEpisodeScraperMetadataConfig(Type type) {
    this(type, null, null);
  }

  TvShowEpisodeScraperMetadataConfig(Type type, String description) {
    this(type, description, null);
  }

  TvShowEpisodeScraperMetadataConfig(Type type, String description, String tooltip) {
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
   * get a {@link List} of all {@link TvShowEpisodeScraperMetadataConfig} for the given {@link org.tinymediamanager.core.ScraperMetadataConfig.Type}
   *
   * @param type
   *          the {@link org.tinymediamanager.core.ScraperMetadataConfig.Type} to get all {@link TvShowEpisodeScraperMetadataConfig}s for
   * @return a {@link List} with all matching {@link TvShowEpisodeScraperMetadataConfig}s
   */
  public static List<TvShowEpisodeScraperMetadataConfig> valuesForType(Type type) {
    List<TvShowEpisodeScraperMetadataConfig> values = new ArrayList<>();

    for (TvShowEpisodeScraperMetadataConfig config : values()) {
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
  public static List<TvShowEpisodeScraperMetadataConfig> getValues() {
    List<TvShowEpisodeScraperMetadataConfig> values = new ArrayList<>();

    for (TvShowEpisodeScraperMetadataConfig value : values()) {
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
  public static List<TvShowEpisodeScraperMetadataConfig> getValuesWithout(TvShowEpisodeScraperMetadataConfig... valuesToExclude) {
    List<TvShowEpisodeScraperMetadataConfig> values = new ArrayList<>();
    List<TvShowEpisodeScraperMetadataConfig> exclude = Arrays.asList(valuesToExclude);

    for (TvShowEpisodeScraperMetadataConfig value : values()) {
      if (value.type != Type.DEPRECATED && !exclude.contains(value)) {
        values.add(value);
      }
    }

    return values;
  }
}
