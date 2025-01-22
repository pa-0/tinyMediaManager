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
package org.tinymediamanager.scraper.anidb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;

public class ITAniDBMovieMetadataProviderTest extends BasicITest {

  private IMovieMetadataProvider      metadataProvider;
  private MovieSearchAndScrapeOptions scrapeOptions;

  @Before
  public void setup() throws Exception {
    super.setup();

    metadataProvider = new AniDbMovieMetadataProvider();
    scrapeOptions = new MovieSearchAndScrapeOptions();
  }

  /**
   * This isn't testing anything that the other Search test isnt already testing, so can maybe be removed.
   * 
   * @see ITAniDBTvShowMetadataProviderTest#testSearch()
   */
  @Test
  public void testSearch() throws Exception {
    scrapeOptions.setSearchQuery("Patlabor the Movie");
    List<MediaSearchResult> results = new ArrayList<>(metadataProvider.search(scrapeOptions));

    for (MediaSearchResult result : results) {
      System.out.println(result.getTitle() + " " + result.getId() + " " + result.getScore());
    }

    scrapeOptions.setSearchQuery("Spice and Wolf");
    results = new ArrayList<>(metadataProvider.search(scrapeOptions));

    assertThat(results).isNotEmpty();
  }

  /**
   * Using Patlabor the Movie for testing.
   *
   * <a href="https://anidb.net/anime/777">https://anidb.net/anime/777</a>
   */
  @Test
  public void testScrapeMovie() throws Exception {
    scrapeOptions.setId("anidb", "777");
    scrapeOptions.setLanguage(MediaLanguages.en);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    MediaMetadata md = metadataProvider.getMetadata(scrapeOptions);

    assertEquals("1989-07-15", sdf.format(md.getReleaseDate()));
    assertEquals(1989, md.getYear());
    assertEquals("Patlabor the Movie", md.getTitle());
    assertEquals("Kidou Keisatsu Patlabor", md.getOriginalTitle());
    assertEquals("At the top of a large structure in Tokyo Bay, a scientist walks to the edge and, despite the cries "
        + "of onlookers, throws himself into the sea to his death. Weeks later, the Special Vehicle "
        + "Division (SVD) has had little respite due to a recent rash of Labor malfunctions resulting "
        + "in massive citywide destruction. No one seems to know the cause of the malfunctions; the "
        + "only hint points to a cover-up involving a military-class Labor going on a rampage \u2014 without"
        + " a pilot. It is soon evident that a disaster of epic proportions is taking shape, and only "
        + "http://anidb.net/ch9016 [Gotou], http://anidb.net/ch11745 [Shinobu], and the SVD can counter"
        + " it. If they succeed, they will be heroes... but if they fail, they will be the most hated " + "villains in history.", md.getPlot());

    assertThat(md.getRatings().size()).isEqualTo(1);
    MediaRating mediaRating = md.getRatings().get(0);
    assertThat(mediaRating.getId()).isNotEmpty();
    assertThat(mediaRating.getId()).isNotEmpty();
    assertThat(mediaRating.getRating()).isGreaterThan(0);
    assertThat(mediaRating.getVotes()).isGreaterThanOrEqualTo(56);
    assertEquals("http://img7.anidb.net/pics/anime/83834.jpg", md.getMediaArt(MediaArtworkType.POSTER).get(0).getBiggestArtwork().getUrl());
    assertEquals("Anime", md.getGenres().get(0).toString());

    // first actor
    Person member = md.getCastMembers().get(0);
    assertEquals("Tominaga Miina", member.getName());
    assertEquals("Izumi Noa", member.getRole());
    assertEquals("http://img7.anidb.net/pics/anime/54324.jpg", member.getThumbUrl());
  }
}
