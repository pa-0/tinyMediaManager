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

package org.tinymediamanager.scraper.ofdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;

public class ITOfdbMetadataProviderTest extends BasicITest {

  @Test
  public void testSearchViaOfdb() throws ScrapeException {
    IMovieMetadataProvider mp = null;
    List<MediaSearchResult> results = null;
    MovieSearchAndScrapeOptions options = null;

    mp = new OfdbMovieMetadataProvider();
    options = new MovieSearchAndScrapeOptions();
    options.setId("ofdb", "188514");
    options.setLanguage(MediaLanguages.de);
    options.setCertificationCountry(CountryCode.DE);
    options.setReleaseDateCountry("DE");

    results = new ArrayList<>(mp.search(options));
    // did we get a result?
    assertNotNull("Result", results);
    // result count
    assertEquals("Result count", 1, results.size());

    assertEquals("Avatar - Aufbruch nach Pandora", results.get(0).getTitle());
    assertEquals(2009, results.get(0).getYear());
  }

  @Test
  public void testSearchViaImdb() throws ScrapeException {
    IMovieMetadataProvider mp = null;
    List<MediaSearchResult> results = null;
    MovieSearchAndScrapeOptions options = null;

    mp = new OfdbMovieMetadataProvider();
    options = new MovieSearchAndScrapeOptions();
    options.setImdbId("tt0499549");
    options.setLanguage(MediaLanguages.de);
    options.setCertificationCountry(CountryCode.DE);
    options.setReleaseDateCountry("DE");

    results = new ArrayList<>(mp.search(options));
    // did we get a result?
    assertNotNull("Result", results);
    // result count
    assertEquals("Result count", 1, results.size());

    assertEquals("Avatar - Aufbruch nach Pandora", results.get(0).getTitle());
    assertEquals(2009, results.get(0).getYear());
    System.out.println(results.get(0).getIdAsString("ofdb"));
  }

  @Test
  public void testSearchViaText() throws ScrapeException {
    IMovieMetadataProvider mp = null;
    List<MediaSearchResult> results = null;
    MovieSearchAndScrapeOptions options = null;

    mp = new OfdbMovieMetadataProvider();
    options = new MovieSearchAndScrapeOptions();
    options.setSearchQuery("Die Piefke Saga");
    options.setLanguage(MediaLanguages.de);
    options.setCertificationCountry(CountryCode.DE);
    options.setReleaseDateCountry("DE");

    results = new ArrayList<>(mp.search(options));
    // did we get a result?
    assertNotNull("Result", results);
    assertEquals("Die Piefke-Saga", results.get(0).getTitle());

    // result count
    assertEquals("Result count", 1, results.size());
  }

  @Test
  public void testSearchMixedMoviePersonResult() throws ScrapeException {
    IMovieMetadataProvider mp = null;
    List<MediaSearchResult> results = null;
    MovieSearchAndScrapeOptions options = null;

    // Lucky # Slevin
    mp = new OfdbMovieMetadataProvider();
    options = new MovieSearchAndScrapeOptions();
    options.setSearchQuery("Slevin");
    options.setLanguage(MediaLanguages.de);
    options.setCertificationCountry(CountryCode.DE);
    options.setReleaseDateCountry("DE");

    results = new ArrayList<>(mp.search(options));
    // did we get a result?
    assertNotNull("Result", results);

    // result count
    assertEquals("Result count", 1, results.size());

    assertEquals("Lucky # Slevin", results.get(0).getTitle());
    // assertEquals("Lucky Number Slevin", results.get(0).getOriginalTitle());
    assertEquals(2006, results.get(0).getYear());
  }

  @Test
  public void testScrape() {
    IMovieMetadataProvider mp = null;
    MovieSearchAndScrapeOptions options = null;
    MediaMetadata md = null;

    // Merida
    try {
      mp = new OfdbMovieMetadataProvider();
      options = new MovieSearchAndScrapeOptions();
      options.setLanguage(MediaLanguages.de);
      options.setCertificationCountry(CountryCode.DE);
      options.setReleaseDateCountry("DE");

      options.setId(mp.getProviderInfo().getId(), "226045");

      md = mp.getMetadata(options);

      assertThat(md.getTitle()).isEqualTo("Merida - Legende der Highlands");
      assertThat(md.getOriginalTitle()).isEqualTo("Brave");
      assertThat(md.getYear()).isEqualTo(2012);
      assertThat(md.getPlot()).contains("Merida", "Erstgeborene", "Fergus", "Hochland");
      assertThat(md.getTagline()).isEmpty();

      assertThat(md.getRatings().size()).isEqualTo(1);
      MediaRating mediaRating = md.getRatings().get(0);
      assertThat(mediaRating.getId()).isNotEmpty();
      assertThat(mediaRating.getRating()).isGreaterThan(0);
      assertThat(mediaRating.getVotes()).isGreaterThan(0);
      assertThat(mediaRating.getMaxValue()).isEqualTo(10);

      assertThat(md.getCastMembers(ACTOR)).isNotNull();
      assertThat(md.getCastMembers(ACTOR).size()).isGreaterThanOrEqualTo(11);
      assertThat(md.getCastMembers(ACTOR).get(0).getName()).isEqualTo("Billy Connolly");
      assertThat(md.getCastMembers(ACTOR).get(0).getRole()).isEqualTo("Fergus");
      assertThat(md.getCastMembers(DIRECTOR)).isNotNull();
      assertThat(md.getCastMembers(DIRECTOR).size()).isEqualTo(3);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    // with imdbid (Bourne Vermächtnis)
    try {
      mp = new OfdbMovieMetadataProvider();

      MovieSearchAndScrapeOptions scop = new MovieSearchAndScrapeOptions();
      scop.setId(MediaMetadata.IMDB, "tt1194173");
      md = mp.getMetadata(scop);

      assertThat(md.getTitle()).isEqualTo("Das Bourne Vermächtnis");
      assertThat(md.getOriginalTitle()).isEqualTo("The Bourne Legacy");
      assertThat(md.getYear()).isEqualTo(2012);

    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
}
