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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.tinymediamanager.core.entities.Person.Type.ACTOR;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;

/**
 * @author Nikolas Mavropoylos
 */
public class ITTmdbMovieMetadataProviderTest extends BasicITest {

  @Test
  public void testMovieScrapeDataIntegrityInEnglish() throws Exception {
    IMovieMetadataProvider mp = new TmdbMovieMetadataProvider();
    MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
    options.setId(mp.getId(), "63");
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    MediaMetadata md = mp.getMetadata(options);

    assertEquals("Twelve Monkeys", md.getTitle());
    assertEquals(1995, md.getYear());
    assertThat(md.getPlot()).contains("Cole", "Railly", "mental", "disease");
    assertEquals("The future is history.", md.getTagline());

    assertNotNull(md.getCastMembers(ACTOR));
    assertThat(md.getCastMembers(ACTOR).size()).isGreaterThanOrEqualTo(65);
  }

  @Test
  public void testMovieScrapeDataIntegrityInGerman() throws Exception {
    IMovieMetadataProvider mp = new TmdbMovieMetadataProvider();
    MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();

    options.setLanguage(MediaLanguages.de);
    options.setCertificationCountry(CountryCode.DE);
    options.setReleaseDateCountry("DE");

    options.setId(mp.getId(), "62177");

    MediaMetadata md = mp.getMetadata(options);

    assertEquals("Merida - Legende der Highlands", md.getTitle());
    assertEquals(2012, md.getYear());
    assertThat(md.getPlot()).contains("Merida", "Schottland", "König", "Prinzessin");
    assertThat(md.getTagline()).isNotEmpty();

    assertNotNull(md.getCastMembers(ACTOR));
    assertThat(md.getCastMembers(ACTOR).size()).isGreaterThan(0);
  }

  @Test
  public void testMovieScrapeDataWithFallBackLanguageShouldFallbackAndReturnCorrectData() throws Exception {

    IMovieMetadataProvider mp = new TmdbMovieMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("titleFallback", true);
    mp.getProviderInfo().getConfig().setValue("titleFallbackLanguage", MediaLanguages.en.toString());

    MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
    options.setLanguage(MediaLanguages.el);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    options.setId(mp.getId(), "79553");

    MediaMetadata md = mp.getMetadata(options);

    assertThat(md.getTitle()).isEqualTo("The Front Line");
  }

  // @Test
  // public void testMovieScrapeDataWithFallBackLanguageShouldFallbackAndReturnOriginalData() throws Exception {
  //
  // providerInfo.getConfig().setValue("titleFallback",true);
  // providerInfo.getConfig().setValue("titleFallbackLanguage",MediaLanguages.ar.toString());
  //
  // scrapeOptions = new MediaScrapeOptions(MediaType.MOVIE);
  // scrapeOptions.setLanguage(LocaleUtils.toLocale(MediaLanguages.ar.name()));
  // scrapeOptions.setId(movieMetadataProvider.getProviderInfo().getId(), "79553");
  //
  // md = movieMetadataProvider.getMetadata(scrapeOptions);
  //
  // assertThat(md.getTitle()).isEqualTo("The Front Line");
  // assertThat(md.getTitle()).isEqualTo(md.getOriginalTitle());
  //
  // providerInfo.getConfig().setValue("titleFallback",false);
  // }
  //
  // @Test
  // public void testMovieScrapeDataWithFallBackLanguageSameAsQueryLanguageShouldNotFallBack() throws Exception {
  //
  // LOGGER.info("Test Case:");
  // LOGGER.info("\tWe query for a Movie in Greek with Fallback Language Greek and we verify that callback is not Initiated.");
  //
  // providerInfo.getConfig().setValue("titleFallback",true);
  // providerInfo.getConfig().setValue("titleFallbackLanguage",MediaLanguages.el.toString());
  //
  // scrapeOptions = new MediaScrapeOptions(MediaType.MOVIE);
  // scrapeOptions.setLanguage(LocaleUtils.toLocale(MediaLanguages.el.name()));
  // scrapeOptions.setId(movieMetadataProvider.getProviderInfo().getId(), "79553");
  //
  // md = movieMetadataProvider.getMetadata(scrapeOptions);
  //
  // assertThat(md.getTitle()).isEqualTo("고지전");
  // assertThat(md.getTitle()).isEqualTo(md.getOriginalTitle());
  //
  // providerInfo.getConfig().setValue("titleFallback",false);
  // }

  @Test
  public void testMovieSearchWithFallBackLanguageEnglishVerifyFallbackInitiatedAndChangedTitlesNumberIntegrity() throws Exception {
    IMovieMetadataProvider mp = new TmdbMovieMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("titleFallback", true);
    mp.getProviderInfo().getConfig().setValue("titleFallbackLanguage", MediaLanguages.en.toString());

    MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
    options.setSearchQuery("The Front Line");
    options.setLanguage(MediaLanguages.el);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    List<MediaSearchResult> results = new ArrayList<>(mp.search(options));
    // did we get a result?
    assertNotNull("Result", results);

    // result count
    assertThat(results.size()).isGreaterThan(0);

    assertThat(results.get(1).getTitle()).isEqualTo("The Front Line");
  }

  @Test
  public void testMovieSearchDataIntegrity() throws Exception {

    IMovieMetadataProvider mp = new TmdbMovieMetadataProvider();
    MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
    options.setSearchQuery("Harry Potter");
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    List<MediaSearchResult> results = new ArrayList<>(mp.search(options));
    // did we get a result?
    assertNotNull("Result", results);

    // result count
    assertThat(results.size()).isGreaterThanOrEqualTo(20);

  }

  @Test
  public void testMovieSearchDataIntegrityInGerman() throws Exception {
    IMovieMetadataProvider mp = new TmdbMovieMetadataProvider();
    MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
    options.setSearchQuery("Harry Potter");
    options.setLanguage(MediaLanguages.de);
    options.setCertificationCountry(CountryCode.DE);
    options.setReleaseDateCountry("DE");

    List<MediaSearchResult> results = new ArrayList<>(mp.search(options));
    // did we get a result?
    assertNotNull("Result", results);

    // result count
    assertThat(results.size()).isGreaterThanOrEqualTo(20);

    MediaSearchResult object = null;
    for (MediaSearchResult result : results) {
      if (result.getOriginalTitle().equals("Harry Potter and the Philosopher's Stone")) {
        object = result;
      }
    }

    assertThat(object).isNotNull();

    assertThat(object.getTitle()).isEqualTo("Harry Potter und der Stein der Weisen");
    assertThat(object.getOriginalTitle()).isEqualTo("Harry Potter and the Philosopher's Stone");

  }

  @Test
  public void testImdbSearch() throws Exception {
    IMovieMetadataProvider mp = new TmdbMovieMetadataProvider();
    MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
    options.setImdbId("tt0114746");
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    MediaMetadata md = mp.getMetadata(options);

    assertEquals("Twelve Monkeys", md.getTitle());
    assertEquals(1995, md.getYear());
    assertEquals(
        "In the year 2035, convict James Cole reluctantly volunteers to be sent back in time to discover the origin of a deadly virus that wiped out nearly all of the earth's population and forced the survivors into underground communities. But when Cole is mistakenly sent to 1990 instead of 1996, he's arrested and locked up in a mental hospital. There he meets psychiatrist Dr. Kathryn Railly, and patient Jeffrey Goines, the son of a famous virus expert, who may hold the key to the mysterious rogue group, the Army of the 12 Monkeys, thought to be responsible for unleashing the killer disease.",
        md.getPlot());
    assertEquals("The future is history.", md.getTagline());

    assertNotNull(md.getCastMembers(ACTOR));
    assertThat(md.getCastMembers(ACTOR).size()).isGreaterThanOrEqualTo(65);
  }
}
