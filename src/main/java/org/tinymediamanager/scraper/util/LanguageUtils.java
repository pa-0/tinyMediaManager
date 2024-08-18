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
package org.tinymediamanager.scraper.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * This is a helper class for language related tasks
 *
 * @author Manuel Laggner
 * @since 2.0
 */
public class LanguageUtils {
  // Map of all known English/UserLocalized String to base locale, key is LOWERCASE
  public static final Map<String, Locale>  KEY_TO_LOCALE_MAP;
  public static final Map<String, Locale>  KEY_TO_COUNTRY_LOCALE_MAP;
  public static final Map<String, Integer> MONTH_REGIONAL_TO_NUM;
  private static final Map<Locale, String> ISO_639_2B_EXCEPTIONS;

  static {
    ISO_639_2B_EXCEPTIONS = createIso6392BExceptions();
    KEY_TO_LOCALE_MAP = generateLanguageArray();
    KEY_TO_COUNTRY_LOCALE_MAP = generateCountryArray();
    MONTH_REGIONAL_TO_NUM = generateMonthNameArray();
  }

  private LanguageUtils() {
    throw new IllegalAccessError();
  }

  /**
   * class initialization
   */
  public static void init() {
    // just to be called to initialize the class
  }

  private static Map<Locale, String> createIso6392BExceptions() {
    Map<Locale, String> exceptions = new HashMap<>();
    exceptions.put(Locale.forLanguageTag("sq"), "alb");
    exceptions.put(Locale.forLanguageTag("hy"), "arm");
    exceptions.put(Locale.forLanguageTag("eu"), "baq");
    exceptions.put(Locale.forLanguageTag("my"), "bur");
    exceptions.put(Locale.forLanguageTag("zh"), "chi");
    exceptions.put(Locale.forLanguageTag("cs"), "cze");
    exceptions.put(Locale.forLanguageTag("nl"), "dut");
    exceptions.put(Locale.forLanguageTag("fr"), "fre");
    exceptions.put(Locale.forLanguageTag("de"), "ger");
    exceptions.put(Locale.forLanguageTag("ka"), "geo");
    exceptions.put(Locale.forLanguageTag("el"), "gre");
    exceptions.put(Locale.forLanguageTag("is"), "ice");
    exceptions.put(Locale.forLanguageTag("mk"), "mac");
    exceptions.put(Locale.forLanguageTag("mi"), "mao");
    exceptions.put(Locale.forLanguageTag("ms"), "may");
    exceptions.put(Locale.forLanguageTag("fa"), "per");
    exceptions.put(Locale.forLanguageTag("ro"), "rum");
    exceptions.put(Locale.forLanguageTag("sk"), "slo");
    exceptions.put(Locale.forLanguageTag("bo"), "tib");
    exceptions.put(Locale.forLanguageTag("cy"), "wel");

    return exceptions;
  }

  private static LinkedHashMap<String, Locale> generateLanguageArray() {
    Map<String, Locale> langArray = new HashMap<>();
    LinkedHashMap<String, Locale> sortedMap = new LinkedHashMap<>();
    Locale intl = Locale.ENGLISH;

    // all possible variants of language/prefixes/non-iso style
    // all lowercase (!)
    for (String langu : Locale.getISOLanguages()) {
      Locale base = new Locale(langu); // from all, create only the base languages

      // ISO Codes have always priority
      // ISO-639-2/T
      langArray.put(base.getISO3Language().toLowerCase(Locale.ROOT), base);
      // ISO-639-2/B
      langArray.put(LanguageUtils.getIso3BLanguage(base).toLowerCase(Locale.ROOT), base);
      // ISO 639-1
      langArray.put(langu.toLowerCase(Locale.ROOT), base);

      // first put the name in the default locale
      langArray.putIfAbsent(base.getDisplayLanguage().toLowerCase(Locale.ROOT), base);
      // second in english
      langArray.putIfAbsent(base.getDisplayLanguage(intl).toLowerCase(Locale.ROOT), base);
      try {
        langArray.putIfAbsent(base.getDisplayLanguage(intl).substring(0, 3).toLowerCase(Locale.ROOT), base); // eg German -> Ger, where iso3=deu
      }
      catch (Exception ignore) {
        // nothing to be done here
      }

      // and afterwards in all other languages
      for (String displayLangu : Locale.getISOLanguages()) {
        try {
          String alternativeLanguage = base.getDisplayLanguage(new Locale(displayLangu));
          if (!alternativeLanguage.isEmpty()) {
            langArray.putIfAbsent(alternativeLanguage.toLowerCase(Locale.ROOT), base);
          }
        }
        catch (Exception ignored) {
          // nothing to be done here
        }
      }
    }

    // also add "special" languages https://de.wikipedia.org/wiki/Liste_der_ISO-639-2-Codes#FN_Anm._1
    langArray.put("mis", new Locale("mis")); // no ISO code yet (missing)
    langArray.put("mul", new Locale("mul")); // multiple languages in audio
    langArray.put("und", new Locale("und")); // undefined
    langArray.put("zxx", new Locale("zxx")); // documents w/o text

    // inoffical pt-BR codes
    langArray.put("pb", new Locale("pt", "BR"));
    langArray.put("pob", new Locale("pt", "BR"));
    langArray.put("ptb", new Locale("pt", "BR"));
    langArray.put("pt-br", new Locale("pt", "BR"));

    // some chinese exceptions
    langArray.put("chs", Locale.SIMPLIFIED_CHINESE);
    langArray.put("cht", Locale.TRADITIONAL_CHINESE);
    langArray.put("zh-chs", Locale.SIMPLIFIED_CHINESE);
    langArray.put("zh-cht", Locale.TRADITIONAL_CHINESE);

    // also sort in all language tags from available locales
    for (Locale locale : Locale.getAvailableLocales()) {
      Locale base = new Locale(locale.getLanguage());
      langArray.putIfAbsent(locale.toLanguageTag().toLowerCase(Locale.ROOT), base);
    }

    // sort from long to short
    List<String> keys = new LinkedList<>(langArray.keySet());
    keys.sort((s1, s2) -> s2.length() - s1.length());

    for (String key : keys) {
      if (!key.isEmpty()) {
        sortedMap.put(key, langArray.get(key));
      }
    }

    return sortedMap;
  }

  private static LinkedHashMap<String, Locale> generateCountryArray() {
    Map<String, Locale> langArray = new HashMap<>();
    LinkedHashMap<String, Locale> sortedMap = new LinkedHashMap<>();
    Locale intl = Locale.ENGLISH;

    for (String cc : Locale.getISOCountries()) {
      Locale l = new Locale("", cc);
      langArray.put(l.getDisplayCountry(), l); // localized name
      langArray.put(l.getDisplayCountry(intl), l); // english name

      // and afterwards in all other languages
      for (String displayLangu : Locale.getISOLanguages()) {
        try {
          String alternativeLanguage = l.getDisplayCountry(new Locale(displayLangu));
          if (!alternativeLanguage.isEmpty()) {
            langArray.putIfAbsent(alternativeLanguage, l);
          }
        }
        catch (Exception ignored) {
          // nothing to be done here
        }
      }

      langArray.put(l.getCountry().toLowerCase(Locale.ROOT), l); // country code 2 char - lowercase to overwrite possible language key (!)
      langArray.put(l.getISO3Country().toLowerCase(Locale.ROOT), l); // country code 3 char - lowercase to overwrite possible language key (!)
    }

    // sort from long to short
    List<String> keys = new LinkedList<>(langArray.keySet());
    Collections.sort(keys, (s1, s2) -> s2.length() - s1.length());

    // all lowercase (!)
    for (String key : keys) {
      if (!key.isEmpty()) {
        sortedMap.put(key.toLowerCase(Locale.ROOT), langArray.get(key));
      }
    }

    return sortedMap;
  }

  // map of month short names to their number
  // since some names have a dot appended (when using MMM pattern)
  // we cannot rely on parsing by our regexes
  // so this map can be used to replace textual month to numeral.
  private static Map<String, Integer> generateMonthNameArray() {
    Map<String, Integer> months = new HashMap<>();
    Calendar cal = Calendar.getInstance();
    for (Locale loc : Locale.getAvailableLocales()) {
      DateFormat df = new SimpleDateFormat("MMM", loc);
      DateFormat df2 = new SimpleDateFormat("MMMM", loc);
      for (int i = 0; i < 12; i++) {
        cal.set(Calendar.MONTH, i);
        String monthShort = df.format(cal.getTime());
        months.put(monthShort, i + 1);
        String monthLong = df2.format(cal.getTime());
        months.put(monthLong, i + 1);
      }
    }
    return months;
  }

  /**
   * Get the ISO 639-2/B 3 letter code
   * 
   * @param locale
   *          the locale to get the code for
   * @return the 3 letter code
   * @since 2.0
   */
  public static String getIso3BLanguage(Locale locale) {
    if (ISO_639_2B_EXCEPTIONS.containsKey(locale)) {
      return ISO_639_2B_EXCEPTIONS.get(locale);
    }
    // special handling for pt-BR since Java handles this is por instead of pob
    if ("pt-BR".equals(locale.toLanguageTag())) {
      return "pob";
    }
    return locale.getISO3Language();
  }

  /**
   * Get the ISO 639-2/B 3 letter code
   * 
   * @param language
   *          the 2 letter ISO code to get the 3 letter code for
   * @return the 3 letter code
   * @since 2.0
   */
  public static String getIso3BLanguage(String language) {
    return getIso3BLanguage(Locale.forLanguageTag(language));
  }

  /**
   * uses our localized language mapping table, to get the iso3 code
   *
   * @param text
   *          the language (as string) to get the iso3 code for
   * @return 3 chars or empty string
   * @since 2.0
   */
  public static String getIso3LanguageFromLocalizedString(String text) {
    Locale l = KEY_TO_LOCALE_MAP.get(text.toLowerCase(Locale.ROOT));
    if (l == null) {
      // fallback; on filesystem you might have underscores, but our map has them with dash
      l = KEY_TO_LOCALE_MAP.get(text.replace("_", "-").toLowerCase(Locale.ROOT));
    }
    if (l != null) {
      // special handling for pt-BR since Java handles this is por instead of pob
      if ("pt-BR".equals(l.toLanguageTag())) {
        return "pob";
      }
      return l.getISO3Language();
    }
    return "";
  }

  /**
   * get the iso3 code including exceptions
   *
   * @param locale
   *          the {@link Locale} to get the iso3 code for
   * @return 3 chars or empty string
   * @since 4.2
   */
  public static String getIso3Language(Locale locale) {
    if (locale != null) {
      // special handling for pt-BR since Java handles this is por instead of pob
      if ("pt-BR".equals(locale.toLanguageTag())) {
        return "pob";
      }
      return locale.getISO3Language();
    }
    return "";
  }

  /**
   * uses our localized language mapping table, to get the iso3B code
   *
   * @param text
   *          the language (as string) to get the iso3B code for
   * @return 3 chars or empty string
   * @since 2.0
   */
  public static String getIso3BLanguageFromLocalizedString(String text) {
    Locale l = KEY_TO_LOCALE_MAP.get(text.toLowerCase(Locale.ROOT));
    if (l == null) {
      // fallback; on filesystem you might have underscores, but our map has them with dash
      l = KEY_TO_LOCALE_MAP.get(text.replace("_", "-").toLowerCase(Locale.ROOT));
    }
    if (l != null) {
      return getIso3BLanguage(l);
    }
    return "";
  }

  /**
   * uses our localized language mapping table, to get the iso2 code
   *
   * @param text
   *          the language (as string) to get the iso2 code for
   * @return 2 chars or empty string
   * @since 2.0
   */
  public static String getIso2LanguageFromLocalizedString(String text) {
    Locale l = KEY_TO_LOCALE_MAP.get(text.toLowerCase(Locale.ROOT));
    if (l == null) {
      // fallback; on filesystem you might have underscores, but our map has them with dash
      l = KEY_TO_LOCALE_MAP.get(text.replace("_", "-").toLowerCase(Locale.ROOT));
    }
    if (l != null) {
      // special handling for pt-BR since Java handles this is por instead of pob
      if ("pt-BR".equals(l.toLanguageTag())) {
        return "pb";
      }
      return l.getLanguage();
    }
    return "";
  }

  /**
   * uses our localized language mapping table, to get the english language name
   *
   * @param text
   *          the language (as string) to get the language name for
   * @return the english language name or empty string
   * @since 2.0
   */
  public static String getEnglishLanguageNameFromLocalizedString(String text) {
    Locale l = KEY_TO_LOCALE_MAP.get(text.toLowerCase(Locale.ROOT));
    if (l == null) {
      // fallback; on filesystem you might have underscores, but our map has them with dash
      l = KEY_TO_LOCALE_MAP.get(text.replace("_", "-").toLowerCase(Locale.ROOT));
    }
    if (l != null) {
      return l.getDisplayLanguage(Locale.ENGLISH);
    }
    return "";
  }

  /**
   * uses our localized language mapping table, to get the localized language name
   *
   * @param text
   *          the language (as string) to get the language name for
   * @return the localized language name or the untranslated string 1:1
   * @since 2.0
   */
  public static String getLocalizedLanguageNameFromLocalizedString(String text) {
    return getLocalizedLanguageNameFromLocalizedString(Locale.getDefault(), text);
  }

  /**
   * uses our localized language mapping table, to get the localized language name in given language
   *
   * @param language
   *          the locale to which we translate the language (as string) to get the language name for
   * @param text
   *          the language (as string) to get the language name for
   * @return the localized language name or empty string
   * @since 2.0
   */
  public static String getLocalizedLanguageNameFromLocalizedString(Locale language, String... text) {
    String ret = "";
    if (language == null) {
      language = Locale.getDefault();
    }
    for (String s : text) {
      Locale l = KEY_TO_LOCALE_MAP.get(s.toLowerCase(Locale.ROOT));
      if (l == null) {
        // fallback; on filesystem you might have underscores, but our map has them with dash
        l = KEY_TO_LOCALE_MAP.get(s.replace("_", "-").toLowerCase(Locale.ROOT));
      }
      if (l != null) {
        ret = l.getDisplayLanguage(language); // auto fallback to english
        if (!ret.isEmpty()) {
          break;
        }
      }
    }
    if (ret.isEmpty() && text.length > 0) {
      ret = text[0]; // cannot translate - just take first param 1:1
    }
    return ret;
  }

  /**
   * tries to get local (JVM language) COUNTRY name for given parameters/variants
   * 
   * @param countries
   *          all possible names or iso codes
   * @return localized country name, or first country param 1:1 if we cannot translate
   */
  public static String getLocalizedCountry(String... countries) {
    return getLocalizedCountryForLanguage(Locale.getDefault().getLanguage(), countries);
  }

  /**
   * tries to get localized COUNTRY name (in given language) for given parameters/variants
   * 
   * @param countries
   *          all possible names or iso codes
   * @return localized country name, or first country param 1:1 if we cannot translate
   */
  public static String getLocalizedCountryForLanguage(String language, String... countries) {
    // KEY_TO_LOCALE_MAP is correct here, we want to get the language locale!!!
    Locale l = KEY_TO_LOCALE_MAP.get(language.toLowerCase(Locale.ROOT));
    if (l == null) {
      // fallback; on filesystem you might have underscores, but our map has them with dash
      l = KEY_TO_LOCALE_MAP.get(language.replace("_", "-").toLowerCase(Locale.ROOT));
    }
    return getLocalizedCountryForLanguage(l, countries);
  }

  /**
   * tries to get localized COUNTRY name (in given language) for given parameters/variants
   * 
   * @param countries
   *          all possible names or iso codes
   * @return localized country name, or first country param 1:1 if we cannot translate
   */
  public static String getLocalizedCountryForLanguage(Locale language, String... countries) {
    String ret = "";
    if (language == null) {
      language = Locale.getDefault();
    }
    for (String c : countries) {
      Locale l = KEY_TO_COUNTRY_LOCALE_MAP.get(c.toLowerCase(Locale.ROOT));
      if (l == null) {
        // fallback; on filesystem you might have underscores, but our map has them with dash
        l = KEY_TO_LOCALE_MAP.get(c.replace("_", "-").toLowerCase(Locale.ROOT));
      }
      if (l != null) {
        ret = l.getDisplayCountry(language); // auto fallback to english
        if (!ret.isEmpty()) {
          break;
        }
      }
    }
    if (ret.isEmpty() && countries.length > 0) {
      ret = countries[0]; // cannot translate - just take first param 1:1
    }
    return ret;
  }

  /**
   * checks whether the given string matches or ends with the given language
   * 
   * @param string
   *          the string to check
   * @param language
   *          the language to check
   * @return true/false
   */
  public static boolean doesStringEndWithLanguage(String string, String language) {
    String stringInLowercase = string.toLowerCase(Locale.ROOT);
    String languageInLowercase = language.toLowerCase(Locale.ROOT);

    if (stringInLowercase.equals(languageInLowercase)) {
      return true;
    }
    if (StringUtils.endsWith(stringInLowercase, "." + languageInLowercase)) {
      return true;
    }
    if (StringUtils.endsWith(stringInLowercase, "-" + languageInLowercase)) {
      return true;
    }
    if (StringUtils.endsWith(stringInLowercase, "_" + languageInLowercase)) {
      return true;
    }
    if (StringUtils.endsWith(stringInLowercase, " " + languageInLowercase)) {
      return true;
    }
    return false;
  }

  /**
   * parse the language from a given String and return the ISO 3 code
   * 
   * @param string
   *          the string to parse
   * @return the language code (ISO3) or an empty string
   */
  public static String parseLanguageFromString(String string) {
    String result = findLanguageInString(string);

    if (StringUtils.isNotBlank(result)) {
      return getIso3LanguageFromLocalizedString(result);

    }

    return "";
  }

  /**
   * find a language tag at the end of a given string
   *
   * @param string
   *          the string to parse
   * @return the language tag an empty string
   */
  public static String findLanguageInString(String string) {
    if (StringUtils.isBlank(string)) {
      return "";
    }

    try {
      Set<String> langArray = KEY_TO_LOCALE_MAP.keySet();
      string = string.replaceAll("(?i)Part [Ii]+", ""); // hardcoded; remove Part II which is no stacking marker; b/c II is a valid iso code :p
      string = string.toLowerCase(Locale.ROOT); // lang array is lowercase only!
      string = StringUtils.split(string, '/')[0].strip(); // possibly "de / de" - just take first
      for (String s : langArray) {
        try {
          if (doesStringEndWithLanguage(string, s)) {// ends with lang + delimiter prefix
            return s;
          }
        }
        catch (Exception ignored) {
          // no need to log
        }
      }
    }
    catch (Exception ignored) {
      // no need to log
    }
    return "";
  }
}
