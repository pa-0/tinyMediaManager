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

package org.tinymediamanager.scraper.trakt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.tinymediamanager.core.entities.Person.Type.ACTOR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;

public class ITTraktMetadataProviderTest extends BasicITest {

  @Test
  public void testLookup() {
    TraktMetadataProvider mp;
    List<MediaSearchResult> results;

    try {
      mp = new TraktMovieMetadataProvider();
      MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
      options.setSearchQuery("Harry Potter and the Philosopher's Stone");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");
      options.setImdbId("tt0241527");
      options.setTmdbId(671);
      // options.setId("trakt", "-2");
      mp.lookupWithId(options);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testMovieSearch() {
    IMovieMetadataProvider mp;
    List<MediaSearchResult> results;

    // Harry Potter and the Philosopher's Stone
    try {
      mp = new TraktMovieMetadataProvider();
      MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
      options.setSearchQuery("Harry Potter and the Philosopher's Stone");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");
      results = new ArrayList<>(mp.search(options));

      // did we get a result?
      assertThat(results).isNotNull().isNotEmpty();
      // are there all fields filled in the result?
      MediaSearchResult result = results.get(0);
      assertThat(result.getTitle()).isNotEmpty();
      assertThat(result.getYear()).isEqualTo(2001);
      assertThat(result.getId()).isEqualTo("545");
      assertThat(result.getScore()).isGreaterThan(0);
      assertThat(result.getIMDBId()).isEqualTo("tt0241527");
      assertThat(result.getProviderId()).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testMovieScrape() {

    MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
    IMovieMetadataProvider mp = new TraktMovieMetadataProvider();
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");
    options.setId(mp.getProviderInfo().getId(), "545"); // Harry Potter and the Philosopher's Stone

    try {
      /**
       * Harry Potter and the Philosopher's Stone
       */
      MediaMetadata md = mp.getMetadata(options);
      assertNotNull(md);
      assertThat(md.getTitle()).isEqualTo("Harry Potter and the Philosopher's Stone");
      assertThat(md.getTagline()).isNotEmpty();
      assertThat(md.getPlot()).isNotEmpty();
      assertThat(md.getYear()).isEqualTo(2001);
      assertThat(md.getReleaseDate()).isInSameDayAs("2001-11-16");
      assertThat(md.getRatings().size()).isGreaterThanOrEqualTo(1);
      MediaRating mediaRating = md.getRatings().get(0);
      assertThat(mediaRating.getId()).isNotEmpty();
      assertThat(mediaRating.getRating()).isGreaterThan(0);
      assertThat(mediaRating.getVotes()).isGreaterThan(0);
      assertThat(mediaRating.getMaxValue()).isEqualTo(10);
      assertThat(md.getRuntime()).isGreaterThan(0);
      assertThat(md.getCertifications()).containsOnly(MediaCertification.US_PG);
      assertThat(md.getGenres()).containsOnly(MediaGenres.ADVENTURE, MediaGenres.FANTASY);

      // ids
      assertThat(md.getId(md.getProviderId())).isEqualTo(545);
      assertThat(md.getId(MediaMetadata.IMDB)).isEqualTo("tt0241527");
      assertThat(md.getId(MediaMetadata.TMDB)).isEqualTo(671);
      // assertThat(md.getMediaArt(MediaArtwork.MediaArtworkType.POSTER)).isNotEmpty();

      // crew
      assertThat(md.getCastMembers()).isNotEmpty();
      Person castMember = md.getCastMembers(ACTOR).get(0);
      assertThat(castMember.getName()).isNotEmpty();
      assertThat(castMember.getRole()).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

  }

  @Test
  public void testTvShowEpisodeList() {
    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    ITvShowMetadataProvider mp = new TraktTvShowMetadataProvider();

    // Game of Thrones
    options.setId(mp.getProviderInfo().getId(), "1390");
    List<MediaMetadata> episodeList;
    MediaMetadata episode;

    try {
      episodeList = mp.getEpisodeList(options);

      assertThat(episodeList).isNotEmpty();
      assertThat(episodeList.get(0)).isNotNull();

      episode = episodeList.get(0);
      assertThat(episode.getTitle()).isNotEmpty();
      assertThat(episode.getPlot()).isNotNull(); // can be empty for some eps
      assertThat(episode.getIds()).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testTvShowScrape() {
    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    ITvShowMetadataProvider mp = new TraktTvShowMetadataProvider();

    // Game of Thrones
    options.setId(mp.getId(), "1390");
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    try {
      MediaMetadata md = mp.getMetadata(options);

      assertThat(md).isNotNull();
      assertThat(md.getTitle()).isEqualTo("Game of Thrones");
      assertThat(md.getYear()).isEqualTo(2011);
      assertThat(md.getPlot()).isNotEmpty();
      // assertThat(md.getReleaseDate()).isInSameDayAs("2011-04-17");
      assertThat(md.getRuntime()).isGreaterThanOrEqualTo(52);
      assertThat(md.getProductionCompanies()).containsOnly("HBO");
      assertThat(md.getCertifications()).containsOnly(MediaCertification.US_TVMA);
      assertThat(md.getCountries()).containsOnly("us");
      assertThat(md.getStatus()).isEqualTo(MediaAiredStatus.ENDED);
      assertThat(md.getRatings().size()).isGreaterThanOrEqualTo(1);
      MediaRating mediaRating = md.getRatings().get(0);
      assertThat(mediaRating.getId()).isNotEmpty();
      assertThat(mediaRating.getRating()).isGreaterThan(0);
      assertThat(mediaRating.getVotes()).isGreaterThan(0);
      assertThat(mediaRating.getMaxValue()).isEqualTo(10);

      assertThat(md.getGenres().size()).isGreaterThan(0);

      // ids
      assertThat(md.getId(MediaMetadata.TRAKT_TV)).isEqualTo(1390);
      assertThat(md.getId(MediaMetadata.TVDB)).isEqualTo(121361);
      assertThat(md.getId(MediaMetadata.IMDB)).isEqualTo("tt0944947");
      assertThat(md.getId(MediaMetadata.TMDB)).isEqualTo(1399);

      // crew
      assertThat(md.getCastMembers()).isNotEmpty();
      Person castMember = md.getCastMembers(ACTOR).get(0);
      assertThat(castMember.getName()).isNotEmpty();
      assertThat(castMember.getRole()).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testTvShowEpisodeScrape() {
    TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
    ITvShowMetadataProvider mp = new TraktTvShowMetadataProvider();

    // Game of Thrones
    options.setTvShowIds(Collections.singletonMap(mp.getProviderInfo().getId(), "1390"));
    options.setId(MediaMetadata.SEASON_NR, "1");
    options.setId(MediaMetadata.EPISODE_NR, "1");

    try {
      MediaMetadata md = mp.getMetadata(options);

      assertThat(md).isNotNull();
      assertThat(md.getTitle()).isEqualTo("Winter Is Coming");
      assertThat(md.getPlot()).isNotEmpty();
      assertThat(md.getReleaseDate()).isInSameDayAs("2011-04-18");
      assertThat(md.getRatings().size()).isGreaterThanOrEqualTo(1);
      MediaRating mediaRating = md.getRatings().get(0);
      assertThat(mediaRating.getId()).isNotEmpty();
      assertThat(mediaRating.getRating()).isGreaterThan(0);
      assertThat(mediaRating.getVotes()).isGreaterThan(0);
      assertThat(mediaRating.getMaxValue()).isEqualTo(10);

      // ids
      assertThat(md.getId(MediaMetadata.TRAKT_TV)).isEqualTo(73640);
      assertThat(md.getId(MediaMetadata.TVDB)).isEqualTo(3254641);
      assertThat(md.getId(MediaMetadata.IMDB)).isEqualTo("tt1480055");
      assertThat(md.getId(MediaMetadata.TMDB)).isEqualTo(63056);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }
}
