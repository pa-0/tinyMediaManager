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
package org.tinymediamanager.scraper.entities;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.LocaleUtils;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.scraper.util.LanguageUtils;

/**
 * The Enum MediaLanguages. All languages we support for scraping
 * 
 * @author Manuel Laggner
 * @since 1.0
 * 
 */
public enum MediaLanguages {

  af,
  ar,
  az,
  be,
  bg,
  bm,
  bn,
  bs,
  ca,
  ch,
  cs,
  cy,
  da,
  de,
  ee,
  el,
  en,
  es,
  es_MX,
  et,
  eu,
  fa,
  fi,
  fr,
  fr_CA,
  ff,
  ga,
  gl,
  ha,
  he,
  hi,
  hr,
  hu,
  hy,
  id,
  is,
  it,
  ja,
  ka,
  kk,
  ko,
  la,
  lv,
  lt,
  mk,
  ms,
  mt,
  nb,
  nl,
  no,
  pa,
  pl,
  ps,
  pt,
  pt_BR,
  rn,
  ro,
  ru,
  rw,
  si,
  sk,
  sl,
  so,
  sq,
  sr,
  sv,
  sw,
  ta,
  te,
  th,
  tr,
  uk,
  ur,
  uz,
  vi,
  wo,
  yo,
  yue, // cn is invalid!
  zh,
  zh_CN,
  zh_HK,
  zh_SG,
  zh_TW,
  zu,
  none("-");

  private final String                             title;
  private final String                             displayTitle;

  private static final Map<String, MediaLanguages> lookup = prepareLookup();

  MediaLanguages() {
    Locale locale = Utils.getLocaleFromLanguage(name());
    this.title = locale.getDisplayLanguage();
    this.displayTitle = locale.getDisplayName();
  }

  MediaLanguages(String title) {
    this.title = title;
    this.displayTitle = title;
  }

  private static Map<String, MediaLanguages> prepareLookup() {
    Map<String, MediaLanguages> mlMap = new HashMap<>();
    for (MediaLanguages lang : MediaLanguages.values()) {
      if (!lang.getTitle().isBlank()) {
        mlMap.put(lang.getTitle(), lang);
        mlMap.put(lang.toString(), lang);
      }
      mlMap.put(lang.name(), lang);
    }
    return mlMap;
  }

  /**
   * Get MediaLanguage by Title
   *
   * @param title
   *          the title/name of the language
   * @return the MediaLanguages Enum Object.
   */
  public static MediaLanguages get(String title) {
    MediaLanguages entry = lookup.get(title);

    // if the entry is null (maybe localized name) try to load it via our language helper
    if (entry == null) {
      entry = lookup.get(LanguageUtils.getIso2LanguageFromLocalizedString(title));
    }

    // if the entry is still null (should not occur), take EN
    if (entry == null) {
      entry = MediaLanguages.en;
    }

    return entry;
  }

  /**
   * return the title
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * return the first 2 letters which is the language part
   * 
   * @return the language
   */
  public String getLanguage() {
    if (this == none) {
      return "";
    }
    int delim = name().indexOf("_");
    if (delim == -1) {
      return name(); // full enum name
    }
    return name().substring(0, delim); // else up to country delim TODO: check why we need to reduce this!!!
  }

  @Override
  public String toString() {
    return displayTitle + " (" + name() + ")"; // localized title, not the one from enum
  }

  public Locale toLocale() {
    if (this == none) {
      return null;
    }
    return LocaleUtils.toLocale(name());
  }

  /**
   * Usually, we sort enums based on their ordinal type, or based on name.<br>
   * This is a convenience method to sort based on toString(), which we defined to be the translated displayTitle - but without the "none" language
   * 
   * @return MediaLanguages.values() in a sorted way
   */
  public static MediaLanguages[] valuesSorted() {
    SortedMap<String, MediaLanguages> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (MediaLanguages ml : MediaLanguages.values()) {
      // we probably don't want "none" is most cases
      if (ml == none) {
        continue;
      }

      map.put(ml.toString(), ml);
    }
    return map.values().toArray(new MediaLanguages[] {});
  }

  /**
   * Usually, we sort enums based on their ordinal type, or based on name.<br>
   * This is a convenience method to sort based on toString(), which we defined to be the translated displayTitle.
   *
   * @return MediaLanguages.values() in a sorted way
   */
  public static MediaLanguages[] allValuesSorted() {
    SortedMap<String, MediaLanguages> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (MediaLanguages ml : MediaLanguages.values()) {
      map.put(ml.toString(), ml);
    }
    return map.values().toArray(new MediaLanguages[] {});
  }
}
