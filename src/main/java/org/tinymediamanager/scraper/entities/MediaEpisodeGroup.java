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

package org.tinymediamanager.scraper.entities;

import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.TmmResourceBundle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * this enum is used to set different episode groups
 *
 * @author Manuel Laggner
 */
public class MediaEpisodeGroup implements Comparable<MediaEpisodeGroup> {

  public enum EpisodeGroupType {
    AIRED,
    DVD,
    ABSOLUTE,
    ALTERNATE,
    DISPLAY
  }

  public static final MediaEpisodeGroup DEFAULT_AIRED    = new MediaEpisodeGroup(EpisodeGroupType.AIRED);
  public static final MediaEpisodeGroup DEFAULT_DVD      = new MediaEpisodeGroup(EpisodeGroupType.DVD);
  public static final MediaEpisodeGroup DEFAULT_ABSOLUTE = new MediaEpisodeGroup(EpisodeGroupType.ABSOLUTE);
  public static final MediaEpisodeGroup DEFAULT_DISPLAY  = new MediaEpisodeGroup(EpisodeGroupType.DISPLAY);

  @JsonProperty
  private final EpisodeGroupType        episodeGroupType;
  @JsonProperty
  private final String                  name;

  public MediaEpisodeGroup(String name) {
    this(EpisodeGroupType.AIRED, name);
  }

  public MediaEpisodeGroup(EpisodeGroupType episodeGroupType) {
    this(episodeGroupType, "");
  }

  @JsonCreator
  public MediaEpisodeGroup(@NotNull @JsonProperty("episodeGroupType") EpisodeGroupType episodeGroupType, @JsonProperty("name") String name) {
    this.episodeGroupType = episodeGroupType;
    this.name = name != null ? name : "";
  }

  public EpisodeGroupType getEpisodeGroupType() {
    return episodeGroupType;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    if (episodeGroupType == null) {
      return TmmResourceBundle.getString("episodeGroup.aired"); // former v5
    }
    String localizedEnumName = TmmResourceBundle.getString("episodeGroup." + episodeGroupType.name().toLowerCase(Locale.ROOT));
    if (StringUtils.isNotBlank(name)) {
      return name + " (" + localizedEnumName + ")";
    }
    else {
      return localizedEnumName;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MediaEpisodeGroup that = (MediaEpisodeGroup) o;
    boolean result = episodeGroupType == that.episodeGroupType;
    if (episodeGroupType != EpisodeGroupType.AIRED && episodeGroupType != EpisodeGroupType.ABSOLUTE && result) {
      result = Objects.equals(name, that.name);
    }
    return result;
  }

  @Override
  public int hashCode() {
    if (episodeGroupType != EpisodeGroupType.AIRED && episodeGroupType != EpisodeGroupType.ABSOLUTE) {
      return Objects.hash(episodeGroupType, name);
    }
    else {
      return Objects.hash(episodeGroupType);
    }
  }

  @Override
  public int compareTo(@NotNull MediaEpisodeGroup o) {
    int result = Integer.compare(episodeGroupType.ordinal(), o.episodeGroupType.ordinal());

    if (episodeGroupType != EpisodeGroupType.AIRED && episodeGroupType != EpisodeGroupType.ABSOLUTE && result == 0) {
      result = name.compareTo(o.name);
    }

    return result;
  }
}
