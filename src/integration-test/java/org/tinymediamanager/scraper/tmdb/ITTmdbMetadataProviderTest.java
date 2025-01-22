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
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.tinymediamanager.core.entities.Person.Type.ACTOR;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.MovieSetSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieSetMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;

public class ITTmdbMetadataProviderTest extends BasicITest {

  @Test
  public void testMovieSearch() {
    IMovieMetadataProvider mp = null;
    List<MediaSearchResult> results = null;
    MovieSearchAndScrapeOptions options = null;

    /********************************************************
     * movie tests in EN
     ********************************************************/

    // Harry Potter
    results = null;
    try {
      mp = new TmdbMovieMetadataProvider();
      options = new MovieSearchAndScrapeOptions();
      options.setSearchQuery("Harry Potter");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      results = new ArrayList<>(mp.search(options));
      // did we get a result?
      assertNotNull("Result", results);

      // result count
      assertThat(results.size()).isGreaterThan(0);
    }
    catch (Exception e) {
      fail(e.getMessage());
    }

    // Lucky # Slevin
    results = null;
    try {
      mp = new TmdbMovieMetadataProvider();
      options = new MovieSearchAndScrapeOptions();
      options.setSearchQuery("Slevin");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      results = new ArrayList<>(mp.search(options));
      // did we get a result?
      assertNotNull("Result", results);

      // result count
      assertEquals("Result count", 1, results.size());

      assertEquals("Lucky Number Slevin", results.get(0).getTitle());
      assertEquals(2006, results.get(0).getYear());
    }
    catch (Exception e) {
      fail(e.getMessage());
    }

    /********************************************************
     * movie tests in DE
     ********************************************************/

    // Lucky # Slevin
    results = null;
    try {
      mp = new TmdbMovieMetadataProvider();
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
      assertEquals(2006, results.get(0).getYear());
    }
    catch (Exception e) {
      fail(e.getMessage());
    }

    /********************************************************
     * movie tests in pt_BR
     ********************************************************/

    // Stripes
    results = null;
    try {
      mp = new TmdbMovieMetadataProvider();
      // FIXME: pt_BR != pt_PT, and since we only submit locale w/o country....
      options = new MovieSearchAndScrapeOptions();
      options.setSearchQuery("Recrutas da Pesada"); // O Pelotão Chanfrado
      options.setLanguage(MediaLanguages.pt_BR);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      results = new ArrayList<>(mp.search(options));
      // did we get a result?
      assertNotNull("Result", results);

      // result count
      assertEquals("Result count", 1, results.size());

      assertEquals("Recrutas da Pesada", results.get(0).getTitle());
      assertEquals(1981, results.get(0).getYear());
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testMovieScrape() {
    IMovieMetadataProvider mp = null;
    MovieSearchAndScrapeOptions options = null;
    MediaMetadata md = null;

    /********************************************************
     * movie tests in EN
     ********************************************************/

    // twelve monkeys
    try {
      mp = new TmdbMovieMetadataProvider();
      options = new MovieSearchAndScrapeOptions();
      options.setId(mp.getProviderInfo().getId(), "63");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      md = mp.getMetadata(options);

      assertEquals("Twelve Monkeys", md.getTitle());
      assertEquals(1995, md.getYear());
      assertEquals(
          "In the year 2035, convict James Cole reluctantly volunteers to be sent back in time to discover the origin of a deadly virus that wiped out nearly all of the earth's population and forced the survivors into underground communities. But when Cole is mistakenly sent to 1990 instead of 1996, he's arrested and locked up in a mental hospital. There he meets psychiatrist Dr. Kathryn Railly, and patient Jeffrey Goines, the son of a famous virus expert, who may hold the key to the mysterious rogue group, the Army of the 12 Monkeys, thought to be responsible for unleashing the killer disease.",
          md.getPlot());
      assertEquals("The future is history.", md.getTagline());

      assertNotNull(md.getCastMembers(ACTOR));
      assertThat(md.getCastMembers(ACTOR).size()).isGreaterThanOrEqualTo(65);
    }
    catch (Exception e) {
      fail(e.getMessage());
    }

    // Harry Potter #1
    try {
      mp = new TmdbMovieMetadataProvider();
      options = new MovieSearchAndScrapeOptions();
      options.setId(mp.getProviderInfo().getId(), "671");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      md = mp.getMetadata(options);

      assertEquals("Harry Potter and the Philosopher's Stone", md.getTitle());
      assertEquals(2001, md.getYear());
      assertThat(md.getPlot()).contains("Harry", "Potter", "Hogwarts");
      assertThat(md.getTagline()).isEqualToIgnoringCase("Let the Magic Begin.");
      assertEquals(1241, (int) md.getId(MediaMetadata.TMDB_SET));
      assertEquals("Harry Potter Collection", md.getCollectionName());

      assertNotNull(md.getCastMembers(ACTOR));
      assertThat(md.getCastMembers(ACTOR).size()).isGreaterThan(0);
    }
    catch (Exception e) {
      fail(e.getMessage());
    }

    /********************************************************
     * movie tests in DE
     ********************************************************/

    // Merida
    try {
      mp = new TmdbMovieMetadataProvider();
      options = new MovieSearchAndScrapeOptions();
      options.setLanguage(MediaLanguages.de);
      options.setCertificationCountry(CountryCode.DE);
      options.setReleaseDateCountry("DE");

      options.setId(mp.getProviderInfo().getId(), "62177");

      md = mp.getMetadata(options);

      assertEquals("Merida - Legende der Highlands", md.getTitle());
      assertEquals(2012, md.getYear());
      assertThat(md.getPlot()).contains("Merida", "Schottland", "König", "Prinzessin");
      assertThat(md.getTagline()).isNotEmpty();

      assertNotNull(md.getCastMembers(ACTOR));
      assertThat(md.getCastMembers(ACTOR).size()).isGreaterThan(0);
    }
    catch (Exception e) {
      fail(e.getMessage());
    }

    /********************************************************
     * movie tests in pt-BR
     ********************************************************/

    // Stripes
    try {
      mp = new TmdbMovieMetadataProvider();
      options = new MovieSearchAndScrapeOptions();
      options.setLanguage(MediaLanguages.pt_BR);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      options.setId(mp.getProviderInfo().getId(), "10890");

      md = mp.getMetadata(options);

      assertEquals("Recrutas da Pesada", md.getTitle());
    }
    catch (Exception e) {
      fail(e.getMessage());
    }

  }

  @Test
  public void testCollectionSearch() {
    IMovieSetMetadataProvider mp = null;
    List<MediaSearchResult> results = null;
    MovieSetSearchAndScrapeOptions options = null;

    /********************************************************
     * movie set tests in EN
     ********************************************************/

    // Harry Potter
    results = null;
    try {
      mp = new TmdbMovieMetadataProvider();
      options = new MovieSetSearchAndScrapeOptions();
      options.setSearchQuery("Harry Potter");
      options.setLanguage(MediaLanguages.de);
      options.setCertificationCountry(CountryCode.DE);
      options.setReleaseDateCountry("DE");

      results = mp.search(options);
      // did we get a result?
      assertNotNull("Result", results);

      assertThat(results).isNotEmpty();
      assertEquals("1st result title", "Harry Potter Filmreihe", results.get(0).getTitle());
    }
    catch (Exception e) {
      fail(e.getMessage());
    }

    /********************************************************
     * movie set tests in DE
     ********************************************************/

    // 101 Dalmatiner
    results = null;
    try {
      mp = new TmdbMovieMetadataProvider();
      options = new MovieSetSearchAndScrapeOptions();
      options.setSearchQuery("101 Dalmatiner");
      options.setLanguage(MediaLanguages.de);
      options.setCertificationCountry(CountryCode.DE);
      options.setReleaseDateCountry("DE");

      results = mp.search(options);
      // did we get a result?
      assertNotNull("Result", results);

      // result count
      assertEquals("Result count", 2, results.size());

      assertEquals("1st result title", "101 Dalmatiner Filmreihe", results.get(0).getTitle());
      assertEquals("2nd result title", "101 Dalmatiner (Animation) Filmreihe", results.get(1).getTitle());
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testCollectionScrape() {
    IMovieSetMetadataProvider mp = null;
    MovieSetSearchAndScrapeOptions options = null;
    MediaMetadata md = null;

    /********************************************************
     * movie set tests in EN
     ********************************************************/

    // Harry Potter collection
    try {
      mp = new TmdbMovieMetadataProvider();
      options = new MovieSetSearchAndScrapeOptions();
      options.setId(mp.getProviderInfo().getId(), "1241");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      md = mp.getMetadata(options);

      assertEquals("Harry Potter Collection", md.getTitle());
      assertEquals("The Harry Potter films are a fantasy series based on the series of seven Harry Potter novels by British writer J. K. Rowling.",
          md.getPlot());
      assertNotNull(md.getSubItems());
      assertEquals(8, md.getSubItems().size());
      assertEquals("Harry Potter and the Philosopher's Stone", md.getSubItems().get(0).getTitle());
      assertEquals(671, md.getSubItems().get(0).getId(MediaMetadata.TMDB));
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testTvShowSearch() {
    ITvShowMetadataProvider metadataProvider = null;
    TvShowSearchAndScrapeOptions options = null;
    List<MediaSearchResult> results = null;

    /********************************************************
     * TV show tests in EN
     ********************************************************/
    try {
      metadataProvider = new TmdbTvShowMetadataProvider();
      options = new TvShowSearchAndScrapeOptions();
      options.setSearchQuery("Psych");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      results = new ArrayList<>(metadataProvider.search(options));

      assertNotNull(results);
      assertThat(results.size()).isGreaterThanOrEqualTo(20);
      assertEquals("Psych", results.get(0).getTitle());
      assertEquals("1447", results.get(0).getId());
    }
    catch (Exception e) {
      fail(e.getMessage());
    }

    /********************************************************
     * TV show tests in DE
     ********************************************************/
    results = null;
    try {
      metadataProvider = new TmdbTvShowMetadataProvider();
      options = new TvShowSearchAndScrapeOptions();
      options.setSearchQuery("Die Simpsons");
      options.setLanguage(MediaLanguages.de);
      options.setCertificationCountry(CountryCode.DE);
      options.setReleaseDateCountry("DE");

      results = new ArrayList<>(metadataProvider.search(options));

      assertNotNull(results);
      assertEquals(1, results.size());
      assertEquals("Die Simpsons", results.get(0).getTitle());
      assertEquals("The Simpsons", results.get(0).getOriginalTitle());
      assertEquals("456", results.get(0).getId());
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testEpisodeListing() {
    ITvShowMetadataProvider mp = null;
    List<MediaMetadata> episodes = null;

    /*
     * Psych (1447)
     */
    try {
      mp = new TmdbTvShowMetadataProvider();
      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");
      options.setId(mp.getProviderInfo().getId(), "1447");

      episodes = mp.getEpisodeList(options);

      // did we get a result?
      assertNotNull("Episodes", episodes);

      // result count
      assertThat(episodes.size()).isGreaterThanOrEqualTo(140);

    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testTvShowScrape() {
    ITvShowMetadataProvider mp = null;
    TvShowSearchAndScrapeOptions options = null;
    MediaMetadata md = null;

    /*
     * Psych (1447)
     */
    try {
      mp = new TmdbTvShowMetadataProvider();
      options = new TvShowSearchAndScrapeOptions();
      options.setTmdbId(1447);
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      assertEquals("Psych", md.getTitle());
      assertEquals(
          "Thanks to his police officer father's efforts, Shawn Spencer spent his childhood developing a keen eye for detail (and a lasting dislike of his dad). Years later, Shawn's frequent tips to the police lead to him being falsely accused of a crime he solved. Now, Shawn has no choice but to use his abilities to perpetuate his cover story: psychic crime-solving powers, all the while dragging his best friend, his dad, and the police along for the ride.",
          md.getPlot());
      assertEquals(2006, md.getYear());

      assertThat(md.getRatings().size()).isGreaterThanOrEqualTo(1);
      MediaRating mediaRating = md.getRatings().get(0);
      assertThat(mediaRating.getId()).isNotEmpty();
      assertThat(mediaRating.getRating()).isGreaterThan(0);
      assertThat(mediaRating.getVotes()).isGreaterThan(0);
      assertThat(mediaRating.getMaxValue()).isEqualTo(10);

      assertEquals(MediaAiredStatus.ENDED, md.getStatus());
      assertThat(md.getProductionCompanies()).isNotEmpty();
      assertEquals(MediaCertification.US_TVPG, md.getCertifications().get(0));
      assertThat(md.getCountries()).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testEpisodeScrape() {
    ITvShowMetadataProvider mp = null;
    TvShowEpisodeSearchAndScrapeOptions options = null;
    MediaMetadata md = null;
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

    /*
     * Psych (1447)
     */
    try {
      mp = new TmdbTvShowMetadataProvider();
      options = new TvShowEpisodeSearchAndScrapeOptions();
      options.setTvShowIds(Collections.singletonMap(mp.getProviderInfo().getId(), "1447"));
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      options.setId(MediaMetadata.SEASON_NR, "1");
      options.setId(MediaMetadata.EPISODE_NR, "1");
      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      assertEquals("Pilot", md.getTitle());
      assertEquals(
          "When Shawn Spencer is arrested for calling in an accurate tip to the police because only the perpetrator would know the details, his only way out is pretending to be a psychic. It turns out Santa Barbara PD isn't done with him. They ask him to consult on a kidnapping case, and a business is born.",
          md.getPlot());
      assertEquals("07-07-2006", sdf.format(md.getReleaseDate()));
      assertThat(md.getCastMembers(ACTOR).size()).isGreaterThanOrEqualTo(7);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

}
