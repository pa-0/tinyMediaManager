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
package org.tinymediamanager.scraper.tmdb;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.tmdb.entities.Configuration;
import org.tinymediamanager.scraper.tmdb.entities.ExternalIds;
import org.tinymediamanager.scraper.tmdb.entities.Genre;
import org.tinymediamanager.scraper.tmdb.entities.Translations;
import org.tinymediamanager.scraper.util.MetadataUtil;

import retrofit2.Response;

/**
 * The Class TmdbMetadataProvider. A meta data, artwork and trailer provider for the site themoviedb.org
 *
 * @author Manuel Laggner
 */
abstract class TmdbMetadataProvider implements IMediaProvider {
  static final String             ID            = "tmdb";

  // Use primary translations, not just our internal MediaLanguages (we need the country!)
  // https://api.themoviedb.org/3/configuration/primary_translations?api_key=XXXX
  // And keep on duplicate languages the main country on first position!
  protected static final String[] PT            = new String[] { "af-ZA", "ar-AE", "ar-BH", "ar-EG", "ar-IQ", "ar-JO", "ar-LY", "ar-MA", "ar-QA",
      "ar-SA", "ar-TD", "ar-YE", "be-BY", "bg-BG", "bn-BD", "br-FR", "ca-AD", "ca-ES", "ch-GU", "cs-CZ", "cy-GB", "da-DK", "de-AT", "de-CH", "de-DE",
      "el-CY", "el-GR", "en-AG", "en-AU", "en-BB", "en-BZ", "en-CA", "en-CM", "en-GB", "en-GG", "en-GH", "en-GI", "en-GY", "en-IE", "en-JM", "en-KE",
      "en-LC", "en-MW", "en-NZ", "en-PG", "en-TC", "en-US", "en-ZM", "en-ZW", "eo-EO", "es-AR", "es-CL", "es-DO", "es-EC", "es-ES", "es-GQ", "es-GT",
      "es-HN", "es-MX", "es-NI", "es-PA", "es-PE", "es-PY", "es-SV", "es-UY", "et-EE", "eu-ES", "fa-IR", "fi-FI", "fr-BF", "fr-CA", "fr-CD", "fr-CI",
      "fr-FR", "fr-GF", "fr-GP", "fr-MC", "fr-ML", "fr-MU", "fr-PF", "ga-IE", "gd-GB", "gl-ES", "he-IL", "hi-IN", "hr-HR", "hu-HU", "id-ID", "it-IT",
      "it-VA", "ja-JP", "ka-GE", "kk-KZ", "kn-IN", "ko-KR", "ku-TR", "ky-KG", "lt-LT", "lv-LV", "ml-IN", "mr-IN", "ms-MY", "ms-SG", "nb-NO", "nl-BE",
      "nl-NL", "no-NO", "pa-IN", "pl-PL", "pt-AO", "pt-BR", "pt-MZ", "pt-PT", "ro-MD", "ro-RO", "ru-RU", "si-LK", "sk-SK", "sl-SI", "so-SO", "sq-AL",
      "sq-XK", "sr-ME", "sr-RS", "sv-SE", "sw-TZ", "ta-IN", "te-IN", "th-TH", "tl-PH", "tr-TR", "uk-UA", "ur-PK", "uz-UZ", "vi-VN", "zh-CN", "zh-HK",
      "zh-SG", "zh-TW", "zu-ZA" };

  // see IT getEpisodeTranslationInEveryPossibleLanguage for creating this
  // remove all episode NRs from title, and then compare (needed for CN, where the # is inbetween :/
  protected static final String[] EPISODE_TRANS = new String[] { "Episode", "حلقة", "Епизод", "Episodi", "epizoda", "Afsnit", "Επεισόδιο", "Episodio",
      "Atala", "‫قسمت", "Jakso", "Épisode", "פרק", "Epizoda", "epizód", "Epızod", "에피소드", "Epizodas", "Epizode", "Aflevering", "Odcinek", "Episódio",
      "Episodul", "Эпизод", "Epizóda", "Xalqada", "Епизода", "Avsnitt", "Bölüm", "Серія", "第話", "第  集" };

  private final MediaProviderInfo providerInfo;

  protected TmdbController        api;
  protected Configuration         configuration;
  protected String                artworkBaseUrl;

  TmdbMetadataProvider() {
    providerInfo = createMediaProviderInfo();
  }

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "themoviedb.org",
        "<html><h3>The Movie Database (TMDb)</h3><br />The largest free movie database maintained by the community. It provides metadata and artwork<br />in many different languages. Thus it is the first choice for non english users<br /><br />Available languages: multiple</html>",
        TmdbMetadataProvider.class.getResource("/org/tinymediamanager/scraper/themoviedb_org.svg"), 50);

    info.getConfig().addText("apiKey", "", true);
    info.getConfig().addBoolean("alternateServer", false);

    return info;
  }

  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(providerInfo.getUserApiKey());
  }

  // thread safe initialization of the API
  protected synchronized void initAPI() throws ScrapeException {

    // check if the API should change from current key to another
    if (api != null) {
      String userApiKey = providerInfo.getUserApiKey();
      if (StringUtils.isNotBlank(userApiKey) && !userApiKey.equals(api.apiKey())) {
        // force re-initialization with new key
        api = null;
      }
      else if (StringUtils.isBlank(userApiKey) && !getApiKey().equals(api.apiKey())) {
        // force re-initialization with new key
        api = null;
      }
      else if (providerInfo.getConfig().getValueAsBool("alternateServer") != api.isAlternateServer()) {
        // force re-initialization with other server
        api = null;
      }
    }

    // create a new instance of the tmdb api
    if (api == null) {
      if (!isActive()) {
        throw new ScrapeException(new FeatureNotEnabledException(this));
      }

      try {
        String userApiKey = providerInfo.getUserApiKey();
        api = new TmdbController(StringUtils.isNotBlank(userApiKey) ? userApiKey : getApiKey(),
            providerInfo.getConfig().getValueAsBool("alternateServer"));
        Response<Configuration> response = api.configurationService().configuration().execute();
        if (response.code() == 401) {
          throw new ScrapeException("Invalid TMDB API key");
        }
        else if (response.body() == null) {
          throw new ScrapeException("Could not contact TMDB API - " + response.message());
        }
        configuration = response.body();
        artworkBaseUrl = configuration.images.secure_base_url;
      }
      catch (ScrapeException e) {
        getLogger().error("could not initialize the API: {}", e.getMessage());
        // force re-initialization the next time this will be called
        api = null;
        throw e;
      }
      catch (Exception e) {
        getLogger().error("could not initialize the API: {}", e.getMessage());
        // force re-initialization the next time this will be called
        api = null;
        throw new ScrapeException(e);
      }
    }
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  protected abstract Logger getLogger();

  /**
   * tries to find correct title & overview from all the translations<br>
   * everything can be null/empty
   *
   * @param translations
   *          the translations to search in
   * @param locale
   *          the locale to search for
   * @return the found translation or null
   */
  protected Translations.Translation getTranslationForLocale(Translations translations, Locale locale) {
    Translations.Translation ret = null;

    if (translations != null && translations.translations != null && !translations.translations.isEmpty()) {
      for (Translations.Translation tr : translations.translations) {
        // check with language AND country
        if (tr.iso_639_1.equals(locale.getLanguage()) && tr.iso_3166_1.equals(locale.getCountry())) {
          ret = tr;
          break;
        }
      }

      if (ret == null) {
        // did not find exact translation, check again with language OR country
        switch (locale.toString()) {
          // exceptions are here es_MX, pt_BR and fr_CA which are completely different
          case "es_MX":
          case "pt_BR":
          case "fr_CA":
            // do nothing
            break;

          default:
            for (Translations.Translation tr : translations.translations) {
              if (tr.iso_639_1.equals(locale.getLanguage()) || tr.iso_3166_1.equals(locale.getCountry())) {
                ret = tr;
                break;
              }
            }
            break;
        }
      }
    }

    return ret;
  }

  /**
   * 0 is title(movie) or name(show)<br>
   * 1 is overview<br>
   * both may be empty, but never null
   *
   * @param translations
   *          the translations to search in
   * @param locale
   *          the locale to search for
   * @return the title or null
   */
  protected String[] getValuesFromTranslation(Translations translations, Locale locale) {
    String[] ret = new String[] { "", "" };

    Translations.Translation tr = getTranslationForLocale(translations, locale);
    if (tr == null || tr.data == null) {
      return ret;
    }

    if (StringUtils.isNotBlank(tr.data.title)) {
      ret[0] = tr.data.title; // movie
    }
    if (StringUtils.isNotBlank(tr.data.name)) {
      ret[0] = tr.data.name; // show
    }

    if (StringUtils.isNotBlank(tr.data.overview)) {
      ret[1] = tr.data.overview;
    }

    return ret;
  }

  /**
   * Maps scraper Genres to internal TMM genres
   */
  static MediaGenres getTmmGenre(Genre genre) {
    if (genre == null || MetadataUtil.unboxInteger(genre.id) == 0) {
      return null;
    }

    MediaGenres g = null;
    switch (genre.id) {
      case 28:
      case 10759:
        g = MediaGenres.ACTION;
        break;

      case 12:
        g = MediaGenres.ADVENTURE;
        break;

      case 16:
        g = MediaGenres.ANIMATION;
        break;

      case 35:
        g = MediaGenres.COMEDY;
        break;

      case 80:
        g = MediaGenres.CRIME;
        break;

      case 105:
        g = MediaGenres.DISASTER;
        break;

      case 99:
        g = MediaGenres.DOCUMENTARY;
        break;

      case 18:
        g = MediaGenres.DRAMA;
        break;

      case 82:
        g = MediaGenres.EASTERN;
        break;

      case 2916:
        g = MediaGenres.EROTIC;
        break;

      case 10751:
        g = MediaGenres.FAMILY;
        break;

      case 10750:
        g = MediaGenres.FAN_FILM;
        break;

      case 14:
        g = MediaGenres.FANTASY;
        break;

      case 10753:
        g = MediaGenres.FILM_NOIR;
        break;

      case 10769:
        g = MediaGenres.FOREIGN;
        break;

      case 36:
        g = MediaGenres.HISTORY;
        break;

      case 10595:
        g = MediaGenres.HOLIDAY;
        break;

      case 27:
        g = MediaGenres.HORROR;
        break;

      case 10756:
        g = MediaGenres.INDIE;
        break;

      case 10402:
        g = MediaGenres.MUSIC;
        break;

      case 22:
        g = MediaGenres.MUSICAL;
        break;

      case 9648:
        g = MediaGenres.MYSTERY;
        break;

      case 10754:
        g = MediaGenres.NEO_NOIR;
        break;

      case 10763:
        g = MediaGenres.NEWS;
        break;

      case 10764:
        g = MediaGenres.REALITY_TV;
        break;

      case 1115:
        g = MediaGenres.ROAD_MOVIE;
        break;

      case 10749:
        g = MediaGenres.ROMANCE;
        break;

      case 878:
      case 10765:
        g = MediaGenres.SCIENCE_FICTION;
        break;

      case 10755:
        g = MediaGenres.SHORT;
        break;

      case 10766:
        g = MediaGenres.SOAP;
        break;

      case 9805:
        g = MediaGenres.SPORT;
        break;

      case 10758:
        g = MediaGenres.SPORTING_EVENT;
        break;

      case 10757:
        g = MediaGenres.SPORTS_FILM;
        break;

      case 10748:
        g = MediaGenres.SUSPENSE;
        break;

      case 10767:
        g = MediaGenres.TALK_SHOW;
        break;

      case 10770:
        g = MediaGenres.TV_MOVIE;
        break;

      case 53:
        g = MediaGenres.THRILLER;
        break;

      case 10752:
      case 10768:
        g = MediaGenres.WAR;
        break;

      case 37:
        g = MediaGenres.WESTERN;
        break;
    }
    if (g == null) {
      g = MediaGenres.getGenre(genre.name);
    }
    return g;
  }

  protected Map<String, Object> parseExternalIDs(ExternalIds ids) {
    Map<String, Object> ret = new HashMap<>();
    if (ids != null) {
      if (StringUtils.isNotBlank(ids.imdb_id)) {
        ret.put(MediaMetadata.IMDB, ids.imdb_id);
      }
      if (StringUtils.isNotBlank(ids.wikidata_id)) {
        ret.put(MediaMetadata.WIKIDATA, ids.wikidata_id);
      }
      if (ids.tvdb_id != null && ids.tvdb_id > 0) {
        ret.put(MediaMetadata.TVDB, ids.tvdb_id);
      }
      if (ids.tvrage_id != null && ids.tvrage_id > 0) {
        ret.put(MediaMetadata.TVRAGE, ids.tvrage_id);
      }
    }
    return ret;
  }

  /**
   * tmdb works better if we send a "real" language tag (containing language AND country); since we have only the language tag we do the same hack as
   * described in the tmdb api (By default, a bare ISO-639-1 language will default to its matching pair, ie. pt-PT - source
   * https://developers.themoviedb.org/3/getting-started/languages), but without the bug they have ;)
   * 
   * @param language
   *          the {@link MediaLanguages} to parse
   * @return a {@link String} containing the language and country code
   */
  static String getRequestLanguage(@NotNull MediaLanguages language) {
    String name = language.name();

    Locale locale;

    if (name.length() > 2) {
      // language tag is longer than 2 characters -> we have the country
      locale = language.toLocale();
    }
    else {
      // try to get the right locale with the language tag in front an end (e.g. de-DE)
      locale = new Locale(name, name.toUpperCase(Locale.ROOT));
      // now check if the resulting locale is valid
      if (!LocaleUtils.isAvailableLocale(locale)) {
        // no => fallback to default
        locale = language.toLocale();
      }
    }

    if (locale == null) {
      // fallback - should never be reached, but...
      locale = Locale.getDefault();
    }

    return locale.toLanguageTag();
  }
}
