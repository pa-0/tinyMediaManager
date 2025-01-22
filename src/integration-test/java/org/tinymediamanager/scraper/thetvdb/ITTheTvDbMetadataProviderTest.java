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

package org.tinymediamanager.scraper.thetvdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.PRODUCER;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroupType.AIRED;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroupType.DVD;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;

public class ITTheTvDbMetadataProviderTest extends BasicITest {

  @Test
  public void testSearch() throws Exception {
    ITvShowMetadataProvider metadataProvider = new TheTvDbTvShowMetadataProvider();

    // searchShow(metadataProvider, "Un village français", "fr", "211941", 2009); // not returning any result - reported
    searchShow(metadataProvider, "Der Mondbár", "de", "81049", 2007);
    searchShow(metadataProvider, "Psych", "en", "79335", 2006);
    searchShow(metadataProvider, "You're the Worst", "en", "281776", 2014);
    searchShow(metadataProvider, "America's Book of Secrets", "en", "256002", 2012);
    searchShow(metadataProvider, "Rich Man, Poor Man", "en", "77151", 1976);
    searchShow(metadataProvider, "Drugs, Inc", "en", "174501", 2010);
    searchShow(metadataProvider, "Yu-Gi-Oh!", "en", "113561", 1998);
    // searchShow(metadataProvider, "What's the Big Idea?", "en", "268282", 2013); // not returning a valid year - reported
    searchShow(metadataProvider, "Wallace & Gromit", "en", "78996", 1989);
    searchShow(metadataProvider, "SOKO Kitzbühel", "de", "101241", 2001);

    // searchShow(metadataProvider, "tt1288631", "fr", "", "211941", 0); // IMDB id entered as search term // not implemented yet
    searchShow(metadataProvider, "", "fr", "211941", "211941", 2009); // empty searchString, but valid ID!
  }

  private void searchShow(ITvShowMetadataProvider metadataProvider, String title, String language, String checkId, int year) {
    // this does not SET the id for lookup, but keeps if for assertion....
    searchShow(metadataProvider, title, language, null, checkId, year);
  }

  private void searchShow(ITvShowMetadataProvider metadataProvider, String title, String language, String setId, String checkId, int year) {
    try {
      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setSearchQuery(title);
      options.setLanguage(MediaLanguages.get(language));
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");
      if (setId != null) {
        options.setId(metadataProvider.getProviderInfo().getId(), setId); // when set, just lookup, no search
      }
      // options.setCountry(CountryCode.valueOf(language.toUpperCase(Locale.ROOT)));
      // options.setYear(year);

      List<MediaSearchResult> results = new ArrayList<>(metadataProvider.search(options));
      if (results.isEmpty()) {
        Assert.fail("Result empty!");
      }

      MediaSearchResult result = results.get(0);
      assertThat(result.getTitle()).isNotEmpty();
      if (!checkId.isEmpty()) {
        assertThat(result.getId()).isEqualTo(checkId);
      }
      if (year > 0) {
        assertThat(result.getYear()).isEqualTo(year);
      }
      else {
        assertThat(result.getYear()).isGreaterThan(0);
      }
      assertThat(result.getPosterUrl()).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testSpecialCharacters() throws Exception {
    // (semi)colons are not removed anylonger
    ITvShowMetadataProvider metadataProvider = new TheTvDbTvShowMetadataProvider();
    searchShow(metadataProvider, "X-Men: The Animated Series", "de", "76115", 1992);
    searchShow(metadataProvider, "ChäoS;Child", "de", "320459", 2017);
    searchShow(metadataProvider, "Steins:;Gate", "de", "244061", 2011);
  }

  @Test
  public void testSearchWithFallback() throws Exception {
    ITvShowMetadataProvider metadataProvider = new TheTvDbTvShowMetadataProvider();
    metadataProvider.getProviderInfo().getConfig().setValue("fallbackLanguage", MediaLanguages.el.toString());
    searchShow(metadataProvider, "Wonderfalls", "de", "78845", 2004); // 404 with DE, but found with EN

    metadataProvider.getProviderInfo().getConfig().setValue("fallbackLanguage", MediaLanguages.de.toString());
    searchShow(metadataProvider, "SOKO Kitzbühel", "en", "101241", 2001);
  }

  @Test
  public void testTvShowScrape() {
    /*
     * Psych (79335)
     */
    try {
      ITvShowMetadataProvider metadataProvider = new TheTvDbTvShowMetadataProvider();
      TvShowModuleManager.getInstance().getSettings().setCertificationCountry(CountryCode.US);
      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setId(metadataProvider.getProviderInfo().getId(), "79335");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      MediaMetadata md = metadataProvider.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      assertThat(md.getIds().size()).isGreaterThanOrEqualTo(2); // at least tvdb and imdb
      assertEquals("Psych", md.getTitle());
      assertEquals(
          "Thanks to his police officer father's efforts, Shawn Spencer spent his childhood developing a keen eye for detail (and a lasting dislike of his dad).  Years later, Shawn's frequent tips to the police lead to him being falsely accused of a crime he solved.  Now, Shawn has no choice but to use his abilities to perpetuate his cover story: psychic crime-solving powers, all the while dragging his best friend, his dad, and the police along for the ride.",
          md.getPlot());
      assertEquals(2006, md.getYear());
      assertThat(md.getCertifications()).isNotEmpty();

      assertEquals(MediaAiredStatus.ENDED, md.getStatus());
      assertThat(md.getProductionCompanies()).isNotEmpty();
      assertThat(md.getGenres().size()).isGreaterThan(0);
      assertThat(md.getRuntime()).isGreaterThan(0);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testTvShowScrapeWithFallback() {
    /*
     * Psych (79335)
     */
    try {
      ITvShowMetadataProvider metadataProvider = new TheTvDbTvShowMetadataProvider();
      metadataProvider.getProviderInfo().getConfig().setValue("fallbackLanguage", MediaLanguages.de.toString());

      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setId(metadataProvider.getProviderInfo().getId(), "79335");
      options.setLanguage(MediaLanguages.tr);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      MediaMetadata md = metadataProvider.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      assertThat(md.getIds().size()).isGreaterThanOrEqualTo(2); // at least tvdb and imdb
      assertEquals("Psych", md.getTitle());
      assertEquals(
          "Shawn Spencer ist selbsternannter Detektiv. Von seinem Vater Henry, einem angesehenen Polizisten, wurde er trainiert, sich alle Dinge in seinem Umfeld genau einzuprägen, seien sie auch noch so klein oder unwichtig. Über seine Erziehung unzufrieden kehrte Shawn seinem Vater jedoch den Rücken. Nach einigen misslungenen Lebensabschnitten erkennt er seine Gabe, ungelöste Fälle der Polizei mithilfe seines fotografischen Gedächtnisses lösen zu können. Dabei gibt Shawn aber stets vor ein Hellseher zu sein. Nachdem er der Polizei in mehreren Fällen helfen konnte und diese ihn immer wieder als Unterstützung anfordert, gründet Shawn schließlich mit seinem Freund Burton Guster eine eigene Detektei.",
          md.getPlot());
      assertEquals(2006, md.getYear());
      assertThat(md.getCertifications()).isNotEmpty();

      assertEquals(MediaAiredStatus.ENDED, md.getStatus());
      assertThat(md.getProductionCompanies()).isNotEmpty();
      assertThat(md.getGenres()).contains(MediaGenres.COMEDY, MediaGenres.CRIME, MediaGenres.DRAMA, MediaGenres.MYSTERY);
      assertThat(md.getRuntime()).isGreaterThan(0);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testEpisodeScrape() {
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

    /*
     * Psych (79335)
     */
    try {
      ITvShowMetadataProvider metadataProvider = new TheTvDbTvShowMetadataProvider();

      TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
      options.setTvShowIds(Collections.singletonMap(metadataProvider.getProviderInfo().getId(), "79335"));
      TvShowModuleManager.getInstance().getSettings().setCertificationCountry(CountryCode.US);
      options.setLanguage(MediaLanguages.en);
      options.setId(MediaMetadata.SEASON_NR, "1");
      options.setId(MediaMetadata.EPISODE_NR, "2");
      MediaMetadata md = metadataProvider.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      assertThat(md.getIds().size()).isGreaterThanOrEqualTo(1); // at least tvdb
      assertThat(md.getEpisodeNumber(AIRED)).isNotNull();
      assertThat(md.getEpisodeNumber(AIRED).episode()).isEqualTo(2);
      assertThat(md.getEpisodeNumber(AIRED).season()).isEqualTo(1);
      assertThat(md.getEpisodeNumber(DVD)).isNotNull();
      assertThat(md.getEpisodeNumber(DVD).episode()).isEqualTo(2);
      assertThat(md.getEpisodeNumber(DVD).season()).isEqualTo(1);
      assertThat(md.getTitle()).isEqualTo("The Spellingg Bee");
      assertThat(md.getPlot()).startsWith("When what begins as a little competitive sabotage in a regional spelling");
      assertEquals("14-07-2006", sdf.format(md.getReleaseDate()));
      assertEquals(18, md.getCastMembers(ACTOR).size());
      assertThat(md.getCertifications()).isNotEmpty();
      assertThat(md.getCertifications()).doesNotContain(MediaCertification.UNKNOWN);
      assertThat(md.getCastMembers(DIRECTOR)).isNotEmpty();
      assertThat(md.getCastMembers(WRITER)).isNotEmpty();
      assertThat(md.getMediaArt(MediaArtwork.MediaArtworkType.THUMB)).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testEpisodeScrapeWithFallback() {
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

    /*
     * Psych (79335)
     */
    try {
      ITvShowMetadataProvider metadataProvider = new TheTvDbTvShowMetadataProvider();
      metadataProvider.getProviderInfo().getConfig().setValue("fallbackLanguage", MediaLanguages.de.toString());

      TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
      options.setTvShowIds(Collections.singletonMap(metadataProvider.getProviderInfo().getId(), "79335"));
      TvShowModuleManager.getInstance().getSettings().setCertificationCountry(CountryCode.US);
      options.setLanguage(MediaLanguages.tr);
      options.setId(MediaMetadata.SEASON_NR, "1");
      options.setId(MediaMetadata.EPISODE_NR, "2");
      MediaMetadata md = metadataProvider.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      assertThat(md.getIds().size()).isGreaterThanOrEqualTo(1); // at least tvdb
      assertThat(md.getEpisodeNumber(AIRED)).isNotNull();
      assertThat(md.getEpisodeNumber(AIRED).episode()).isEqualTo(2);
      assertThat(md.getEpisodeNumber(AIRED).season()).isEqualTo(1);
      assertThat(md.getEpisodeNumber(DVD)).isNotNull();
      assertThat(md.getEpisodeNumber(DVD).episode()).isEqualTo(2);
      assertThat(md.getEpisodeNumber(DVD).season()).isEqualTo(1);
      assertThat(md.getTitle()).isEqualTo("So spannend kann ein Buchstabierwettbewerb sein!");
      assertThat(md.getPlot()).startsWith(
          "In Santa Barbara findet der alljährliche Buchstabier-Wettbewerb statt. Gus, der als Knirps selbst einmal an dieser Veranstaltung teilgenommen hatte, aber ausgeschieden war, weil der im Publikum sitzende Shawn ihm falsch vorgesagt hatte, ist vor Begeisterung kaum zu halten und verfolgt die Veranstaltung mangels Tickets als Live-Übertragung im Fernsehen. Shawn dagegen hält die Veranstaltung für eine reine Freakshow und ist von Gus' Enthusiasmus mehr als genervt. Als Brendan Vu, der haushohe Favorit der Veranstaltung, auf Grund eines mysteriösen Ohnmachtsanfalles ausscheiden muss, werden Shawn und Gus mit der Untersuchung des Falles beauftragt. Kurz nachdem sie am Veranstaltungsort eintreffen, stürzt der altgediente Spielleiter des Buchstabierwettbewerbes, Elvin Cavanaugh, bewusstlos aus seiner Loge in das Publikum und ist auf der Stelle tot. Für die Polizei, insbesondere den zynischen Detective Carlton Lassiter scheint dieser Fall sonnenklar zu sein: Der stark übergewichtige Cavanaugh habe einen Herzinfarkt erlitten und sei deshalb in den Tod gestürzt. Lassiters neue Kollegin Juliet O'Hara, auf die Shawn sofort ein Auge wirft, zweifelt jedoch an dieser Theorie. Auch Shawn und Gus vermuten mehr dahinter, für sie deuten die Zeichen eindeutig auf ein Fremdverschulden hin.");
      assertEquals("14-07-2006", sdf.format(md.getReleaseDate()));
      assertEquals(18, md.getCastMembers(ACTOR).size());
      assertThat(md.getCastMembers(DIRECTOR)).isNotEmpty();
      assertThat(md.getCastMembers(WRITER)).isNotEmpty();
      assertThat(md.getCertifications()).isNotEmpty();
      assertThat(md.getCertifications()).doesNotContain(MediaCertification.UNKNOWN);
      assertThat(md.getMediaArt(MediaArtwork.MediaArtworkType.THUMB)).isNotEmpty();

    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testArtworkScrape() {
    /*
     * Psych (79335)
     */
    try {
      ITvShowArtworkProvider artworkProvider = new TheTvDbTvShowArtworkProvider();

      // all scrape
      ArtworkSearchAndScrapeOptions options = new ArtworkSearchAndScrapeOptions(MediaType.TV_SHOW);
      options.setId(artworkProvider.getProviderInfo().getId(), "79335");
      options.setArtworkType(MediaArtwork.MediaArtworkType.ALL);

      List<MediaArtwork> artwork = artworkProvider.getArtwork(options);
      assertThat(artwork).isNotEmpty();

      MediaArtwork ma = artwork.get(0);
      assertThat(ma.getImageSizes()).isNotEmpty();
      assertThat(ma.getType()).isIn(MediaArtwork.MediaArtworkType.BANNER, MediaArtwork.MediaArtworkType.POSTER,
          MediaArtwork.MediaArtworkType.BACKGROUND, MediaArtwork.MediaArtworkType.SEASON_POSTER, MediaArtwork.MediaArtworkType.SEASON_BANNER,
          MediaArtwork.MediaArtworkType.SEASON_THUMB);
      assertThat(ma.getImageSizes()).isNotEmpty();

      // season poster scrape
      options.setArtworkType(MediaArtwork.MediaArtworkType.SEASON_POSTER);

      artwork = artworkProvider.getArtwork(options);
      assertThat(artwork).isNotEmpty();

      ma = artwork.get(0);
      assertThat(ma.getImageSizes()).isNotEmpty();
      assertThat(ma.getType()).isEqualTo(MediaArtwork.MediaArtworkType.SEASON_POSTER);
      assertThat(ma.getSeason()).isGreaterThan(-1);

      // season banner scrape
      options.setArtworkType(MediaArtwork.MediaArtworkType.SEASON_BANNER);

      artwork = artworkProvider.getArtwork(options);
      assertThat(artwork).isNotEmpty();

      ma = artwork.get(0);
      assertThat(ma.getImageSizes()).isNotEmpty();
      assertThat(ma.getType()).isEqualTo(MediaArtwork.MediaArtworkType.SEASON_BANNER);
      assertThat(ma.getSeason()).isGreaterThan(-1);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testEpisodeListScrape() {
    /*
     * Psych (79335)
     */
    try {
      ITvShowMetadataProvider metadataProvider = new TheTvDbTvShowMetadataProvider();

      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setId(metadataProvider.getProviderInfo().getId(), "79335");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      List<MediaMetadata> episodes = metadataProvider.getEpisodeList(options);

      // did we get metadata?
      assertNotNull("episodes", episodes);

      assertThat(episodes.size()).isGreaterThanOrEqualTo(126);

      MediaMetadata episode = null;
      for (MediaMetadata ep : episodes) {
        if (ep.getEpisodeNumber(AIRED).season() == 1 && ep.getEpisodeNumber(AIRED).episode() == 2) {
          episode = ep;
          break;
        }
      }

      assertThat(episode).isNotNull();
      assertThat(episode.getEpisodeNumber(AIRED)).isNotNull();
      assertThat(episode.getEpisodeNumber(AIRED).episode()).isEqualTo(2);
      assertThat(episode.getEpisodeNumber(AIRED).season()).isEqualTo(1);
      assertThat(episode.getEpisodeNumber(DVD)).isNotNull();
      assertThat(episode.getEpisodeNumber(DVD).episode()).isEqualTo(2);
      assertThat(episode.getEpisodeNumber(DVD).season()).isEqualTo(1);
      assertThat(episode.getTitle()).isEqualTo("The Spellingg Bee");
      assertThat(episode.getReleaseDate()).isEqualTo("2006-07-14");

    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testMovieSearch() throws Exception {
    IMovieMetadataProvider mp;
    List<MediaSearchResult> results;
    MovieSearchAndScrapeOptions options;

    /********************************************************
     * movie tests in EN
     ********************************************************/

    // Harry Potter
    mp = new TheTvDbMovieMetadataProvider();
    options = new MovieSearchAndScrapeOptions();
    options.setSearchQuery("Harry Potter");
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    results = new ArrayList<>(mp.search(options));

    // did we get a result?
    assertThat(results).isNotNull();
    assertThat(results).isNotEmpty();

    // Lucky # Slevin
    mp = new TheTvDbMovieMetadataProvider();
    options = new MovieSearchAndScrapeOptions();
    options.setSearchQuery("Lucky Number Slevin");
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    results = new ArrayList<>(mp.search(options));

    // did we get a result?
    assertThat(results).isNotNull();
    assertThat(results).isNotEmpty();

    assertEquals("Lucky Number Slevin", results.get(0).getTitle());
    assertEquals(2006, results.get(0).getYear());

    /********************************************************
     * movie tests in DE
     ********************************************************/

    // Lucky # Slevin
    mp = new TheTvDbMovieMetadataProvider();
    options = new MovieSearchAndScrapeOptions();
    options.setSearchQuery("Lucky # Slevin");
    options.setLanguage(MediaLanguages.de);
    options.setCertificationCountry(CountryCode.DE);
    options.setReleaseDateCountry("DE");

    results = new ArrayList<>(mp.search(options));

    // did we get a result?
    assertThat(results).isNotNull();
    assertThat(results).isNotEmpty();

    assertThat(results.get(0).getTitle()).contains("Lucky", "Slevin");
    assertEquals(2006, results.get(0).getYear());
  }

  @Test
  public void testMovieScrape() throws Exception {
    IMovieMetadataProvider mp = null;
    MovieSearchAndScrapeOptions options = null;
    MediaMetadata md = null;

    /********************************************************
     * movie tests in EN
     ********************************************************/

    // twelve monkeys
    mp = new TheTvDbMovieMetadataProvider();
    options = new MovieSearchAndScrapeOptions();
    options.setId(mp.getProviderInfo().getId(), "706");
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    md = mp.getMetadata(options);

    assertThat(md.getTitle()).isEqualTo("Twelve Monkeys");
    assertThat(md.getYear()).isEqualTo(1995);
    assertThat(md.getPlot()).startsWith("In a future world devastated by disease,");
    assertThat(md.getCertifications()).contains(MediaCertification.DE_FSK16);

    assertThat(md.getCastMembers(ACTOR).size()).isGreaterThan(10);
    assertThat(md.getCastMembers(DIRECTOR).size()).isGreaterThan(1);
    assertThat(md.getCastMembers(WRITER).size()).isGreaterThan(1);
    assertThat(md.getCastMembers(PRODUCER).size()).isGreaterThan(1);

    // Harry Potter #1
    mp = new TheTvDbMovieMetadataProvider();
    options = new MovieSearchAndScrapeOptions();
    options.setId(mp.getProviderInfo().getId(), "66");
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    md = mp.getMetadata(options);

    assertThat(md.getTitle()).isEqualTo("Harry Potter and the Philosopher's Stone");
    assertThat(md.getYear()).isEqualTo(2001);
    assertThat(md.getPlot()).startsWith("An orphaned boy named Harry Potter discovers on his 11th birthday");

    assertThat(md.getCertifications()).contains(MediaCertification.US_PG);

    assertThat(md.getCastMembers(ACTOR).size()).isGreaterThan(10);
    assertThat(md.getCastMembers(DIRECTOR).size()).isGreaterThan(0);
    assertThat(md.getCastMembers(WRITER).size()).isGreaterThan(0);
    assertThat(md.getCastMembers(PRODUCER).size()).isGreaterThan(0);

    /********************************************************
     * movie tests in DE
     ********************************************************/

    // Star Wars: Der Aufstieg Skywalkers
    mp = new TheTvDbMovieMetadataProvider();
    options = new MovieSearchAndScrapeOptions();
    options.setLanguage(MediaLanguages.de);
    options.setCertificationCountry(CountryCode.DE);
    options.setReleaseDateCountry("DE");

    options.setId(mp.getProviderInfo().getId(), "12879");

    md = mp.getMetadata(options);

    assertThat(md.getTitle()).isEqualTo("Star Wars: Der Aufstieg Skywalkers");
    assertThat(md.getYear()).isEqualTo(2019);
    assertThat(md.getPlot()).contains("Star", "Wars", "Skywalker", "Abschluss");
    assertThat(md.getCertifications()).contains(MediaCertification.US_PG13);

    assertNotNull(md.getCastMembers(ACTOR));
    assertThat(md.getCastMembers(ACTOR).size()).isGreaterThan(10);
    assertThat(md.getCastMembers(DIRECTOR).size()).isGreaterThan(0);
    assertThat(md.getCastMembers(WRITER).size()).isGreaterThan(0);
    assertThat(md.getCastMembers(PRODUCER).size()).isGreaterThan(0);

    /********************************************************
     * movie tests in pt-BR
     ********************************************************/

    // Avengers: Endgame / Vingadores: Ultimato
    mp = new TheTvDbMovieMetadataProvider();
    options = new MovieSearchAndScrapeOptions();
    options.setLanguage(MediaLanguages.pt_BR);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    options.setId(mp.getProviderInfo().getId(), "148");

    md = mp.getMetadata(options);

    assertThat(md.getTitle()).isEqualTo("Vingadores: Ultimato");
    assertThat(md.getYear()).isEqualTo(2019);
    assertThat(md.getPlot()).startsWith("Após os eventos de \"Vingadores: Guerra Infinita\"");
    assertThat(md.getCertifications()).contains(MediaCertification.US_PG13);

    assertNotNull(md.getCastMembers(ACTOR));
    assertThat(md.getCastMembers(ACTOR).size()).isGreaterThan(10);
    assertThat(md.getCastMembers(DIRECTOR).size()).isGreaterThan(0);
    assertThat(md.getCastMembers(WRITER).size()).isGreaterThan(0);
    assertThat(md.getCastMembers(PRODUCER).size()).isGreaterThan(0);
  }

  @Test
  public void testEpisodeGroups() throws Exception {
    ITvShowMetadataProvider mp = new TheTvDbTvShowMetadataProvider();

    {
      // house of money - aired and netflix (alternate) order
      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setId(mp.getProviderInfo().getId(), "327417");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      MediaMetadata mediaMetadata = mp.getMetadata(options);

      assertThat(mediaMetadata).isNotNull();
      assertThat(mediaMetadata.getEpisodeGroups()).hasSize(2);
      assertThat(mediaMetadata.getEpisodeGroups()).anyMatch(episodeGroup -> episodeGroup.getEpisodeGroupType() == AIRED);
      assertThat(mediaMetadata.getEpisodeGroups())
          .anyMatch(episodeGroup -> episodeGroup.getEpisodeGroupType() == MediaEpisodeGroup.EpisodeGroupType.ALTERNATE);

      options.setEpisodeGroup(MediaEpisodeGroup.DEFAULT_AIRED);
      List<MediaMetadata> episodesInAiredOrder = mp.getEpisodeList(options);

    }

    {
      // firefly - aired, dvd and absolute
      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setLanguage(MediaLanguages.en);
      options.setId(mp.getProviderInfo().getId(), 78874);

      MediaMetadata mediaMetadata = mp.getMetadata(options);

      assertThat(mediaMetadata).isNotNull();
      assertThat(mediaMetadata.getEpisodeGroups()).hasSize(3);
      assertThat(mediaMetadata.getEpisodeGroups()).anyMatch(episodeGroup -> episodeGroup.getEpisodeGroupType() == AIRED);
      assertThat(mediaMetadata.getEpisodeGroups()).anyMatch(episodeGroup -> episodeGroup.getEpisodeGroupType() == DVD);
      assertThat(mediaMetadata.getEpisodeGroups())
          .anyMatch(episodeGroup -> episodeGroup.getEpisodeGroupType() == MediaEpisodeGroup.EpisodeGroupType.ABSOLUTE);
    }

    {
      // futurama - aired, dvd, absolute, digital and production (unused yet)
      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setLanguage(MediaLanguages.en);
      options.setId(mp.getProviderInfo().getId(), 73871);

      MediaMetadata mediaMetadata = mp.getMetadata(options);

      assertThat(mediaMetadata).isNotNull();
      assertThat(mediaMetadata.getEpisodeGroups()).hasSize(4);
      assertThat(mediaMetadata.getEpisodeGroups()).anyMatch(episodeGroup -> episodeGroup.getEpisodeGroupType() == AIRED);
      assertThat(mediaMetadata.getEpisodeGroups()).anyMatch(episodeGroup -> episodeGroup.getEpisodeGroupType() == DVD);
      assertThat(mediaMetadata.getEpisodeGroups())
          .anyMatch(episodeGroup -> episodeGroup.getEpisodeGroupType() == MediaEpisodeGroup.EpisodeGroupType.ABSOLUTE);
      assertThat(mediaMetadata.getEpisodeGroups())
          .anyMatch(episodeGroup -> episodeGroup.getEpisodeGroupType() == MediaEpisodeGroup.EpisodeGroupType.ALTERNATE);
    }
  }
}
