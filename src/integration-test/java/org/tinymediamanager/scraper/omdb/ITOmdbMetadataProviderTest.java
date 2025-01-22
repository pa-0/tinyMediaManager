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

package org.tinymediamanager.scraper.omdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;

/**
 * @author Wolfgang Janes
 */
public class ITOmdbMetadataProviderTest extends BasicITest {

  /**
   * Testing ProviderInfo
   */
  @Test
  public void testProviderInfo() {
    try {
      OmdbMovieMetadataProvider mp = new OmdbMovieMetadataProvider();
      MediaProviderInfo providerInfo = mp.getProviderInfo();

      assertNotNull(providerInfo.getDescription());
      assertNotNull(providerInfo.getId());
      assertNotNull(providerInfo.getName());
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  // Search
  @Test
  public void testMovieSearch() throws Exception {
    IMovieMetadataProvider mp = new OmdbMovieMetadataProvider();

    // Matrix
    MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
    options.setSearchQuery("The Matrix");
    List<MediaSearchResult> resultList = new ArrayList<>(mp.search(options));
    assertNotNull(resultList);
    assertThat(resultList.size()).isGreaterThan(0);
    assertThat(resultList.get(0).getTitle()).isEqualTo("The Matrix");
    assertThat(resultList.get(0).getYear()).isEqualTo(1999);
    assertThat(resultList.get(0).getIMDBId()).isEqualTo("tt0133093");
    assertThat(resultList.get(0).getMediaType()).isEqualTo(MediaType.MOVIE);
    assertThat(resultList.get(0).getPosterUrl()).isNotEmpty();

    // Men in Black
    options.setSearchQuery("Men in Black");
    resultList = new ArrayList<>(mp.search(options));
    assertNotNull(resultList);
    assertThat(resultList.size()).isGreaterThan(0);
    assertThat(resultList.get(0).getTitle()).isEqualTo("Men in Black");
    assertThat(resultList.get(0).getYear()).isEqualTo(1997);
    assertThat(resultList.get(0).getIMDBId()).isEqualTo("tt0119654");
    assertThat(resultList.get(0).getMediaType()).isEqualTo(MediaType.MOVIE);
    assertThat(resultList.get(0).getPosterUrl()).isNotEmpty();

    // 21
    options.setSearchQuery("21");
    resultList = new ArrayList<>(mp.search(options));
    assertNotNull(resultList);
    assertThat(resultList.size()).isGreaterThan(0);
    assertThat(resultList.get(0).getTitle()).isEqualTo("21");
    assertThat(resultList.get(0).getYear()).isEqualTo(2008);
    assertThat(resultList.get(0).getIMDBId()).isEqualTo("tt0478087");
    assertThat(resultList.get(0).getMediaType()).isEqualTo(MediaType.MOVIE);
    assertThat(resultList.get(0).getPosterUrl()).isNotEmpty();
  }

  // Scrape by ID
  @Test
  public void testMovieScrape() throws Exception {
    IMovieMetadataProvider mp = new OmdbMovieMetadataProvider();

    MovieSearchAndScrapeOptions scrapeOptions = new MovieSearchAndScrapeOptions();
    scrapeOptions.setLanguage(MediaLanguages.en);
    scrapeOptions.setCertificationCountry(CountryCode.US);
    scrapeOptions.setReleaseDateCountry("US");

    MediaMetadata md;

    // Matrix
    scrapeOptions.setImdbId("tt0133093");
    md = mp.getMetadata(scrapeOptions);
    assertThat(md.getTitle()).isEqualTo("The Matrix");
    assertThat(md.getYear()).isEqualTo(Integer.valueOf(1999));
    assertThat(md.getReleaseDate()).isNotNull();
    assertThat(md.getRuntime()).isEqualTo(Integer.valueOf(136));
    assertThat(md.getCertifications()).isNotEmpty();
    assertThat(md.getCastMembers(DIRECTOR)).isNotNull();
    assertThat(md.getCastMembers(DIRECTOR).size()).isGreaterThan(0);
    assertThat(md.getCastMembers(WRITER)).isNotNull();
    assertThat(md.getCastMembers(WRITER).size()).isGreaterThan(0);
    assertThat(md.getCastMembers(ACTOR)).isNotNull();
    assertThat(md.getCastMembers(ACTOR).size()).isGreaterThan(0);
    assertThat(md.getPlot()).isNotEmpty();
    assertThat(md.getCountries()).containsAnyOf("USA", "United States", "Australia");
    assertThat(md.getSpokenLanguages()).contains("English");
    assertThat(md.getRatings()).size().isGreaterThan(2);
    assertThat(md.getGenres()).contains(MediaGenres.ACTION, MediaGenres.SCIENCE_FICTION);
    assertThat(md.getMediaArt(MediaArtwork.MediaArtworkType.POSTER)).isNotNull();

    // Men in Black
    scrapeOptions.setImdbId(""); // empty IMDB!!
    scrapeOptions.setId(mp.getProviderInfo().getId(), "tt0119654");
    md = mp.getMetadata(scrapeOptions);
    assertThat(md.getCertifications()).isNotEmpty();
    assertThat(md.getTitle()).isEqualTo("Men in Black");
    assertThat(md.getYear()).isEqualTo(Integer.valueOf(1997));
    assertThat(md.getReleaseDate()).isNotNull();
    assertThat(md.getRuntime()).isEqualTo(Integer.valueOf(98));
    assertThat(md.getCastMembers(DIRECTOR)).isNotNull();
    assertThat(md.getCastMembers(DIRECTOR).size()).isGreaterThan(0);
    assertThat(md.getCastMembers(WRITER)).isNotNull();
    assertThat(md.getCastMembers(WRITER).size()).isGreaterThan(0);
    assertThat(md.getCastMembers(ACTOR)).isNotNull();
    assertThat(md.getCastMembers(ACTOR).size()).isGreaterThan(0);
    assertThat(md.getPlot()).isNotEmpty();
    assertThat(md.getCountries()).containsAnyOf("USA", "United States", "Australia");
    assertThat(md.getSpokenLanguages()).contains("English", "Spanish");
    assertThat(md.getRatings()).size().isGreaterThan(2);
    assertThat(md.getGenres()).contains(MediaGenres.ADVENTURE, MediaGenres.COMEDY);
  }

  // Search
  @Test
  public void testTvShowSearch() throws Exception {
    ITvShowMetadataProvider mp = new OmdbTvShowMetadataProvider();

    // Game of Thrones
    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setSearchQuery("Game of Thrones");
    List<MediaSearchResult> resultList = new ArrayList<>(mp.search(options));
    assertNotNull(resultList);
    assertThat(resultList.size()).isGreaterThan(0);
    assertThat(resultList.get(0).getTitle()).isEqualTo("Game of Thrones");
    assertThat(resultList.get(0).getYear()).isEqualTo(2011);
    assertThat(resultList.get(0).getIMDBId()).isEqualTo("tt0944947");
    assertThat(resultList.get(0).getMediaType()).isEqualTo(MediaType.TV_SHOW);
    assertThat(resultList.get(0).getPosterUrl()).isNotEmpty();

    // 24
    options = new TvShowSearchAndScrapeOptions();
    options.setSearchQuery("24");
    resultList = new ArrayList<>(mp.search(options));
    assertNotNull(resultList);
    assertThat(resultList.size()).isGreaterThan(0);
    assertThat(resultList.get(0).getTitle()).isEqualTo("24");
    assertThat(resultList.get(0).getYear()).isEqualTo(2001);
    assertThat(resultList.get(0).getIMDBId()).isEqualTo("tt0285331");
    assertThat(resultList.get(0).getMediaType()).isEqualTo(MediaType.TV_SHOW);
    assertThat(resultList.get(0).getPosterUrl()).isNotEmpty();
  }

  @Test
  public void testTvShowScrape() throws Exception {
    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    OmdbTvShowMetadataProvider mp = new OmdbTvShowMetadataProvider();
    MediaMetadata md;

    options.setId(mp.getProviderInfo().getId(), "tt0944947");
    md = mp.getMetadata(options);

    assertThat(md.getCertifications()).isNotEmpty();
    assertThat(md.getTitle()).isEqualTo("Game of Thrones");
    assertThat(md.getYear()).isEqualTo(Integer.valueOf(2011));
    assertThat(md.getReleaseDate()).isNotNull();
    assertThat(md.getRuntime()).isEqualTo(Integer.valueOf(57));
    assertThat(md.getCastMembers(WRITER)).isNotNull();
    assertThat(md.getCastMembers(WRITER).size()).isGreaterThan(0);
    assertThat(md.getCastMembers(ACTOR)).isNotNull();
    assertThat(md.getCastMembers(ACTOR).size()).isGreaterThan(0);
    assertThat(md.getPlot()).isNotEmpty();
    assertThat(md.getCountries()).containsAnyOf("USA", "United States", "UK");
    assertThat(md.getSpokenLanguages()).contains("English");
    assertThat(md.getRatings()).size().isGreaterThan(0);
    assertThat(md.getGenres()).contains(MediaGenres.ACTION, MediaGenres.ADVENTURE, MediaGenres.DRAMA);
    assertThat(md.getMediaArt(MediaArtwork.MediaArtworkType.POSTER)).isNotNull();
  }

  @Test
  public void testEpisodeList() throws Exception {
    OmdbTvShowMetadataProvider mp = new OmdbTvShowMetadataProvider();
    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setId(mp.getProviderInfo().getId(), "tt0944947");

    List<MediaMetadata> mediaMetadataList = mp.getEpisodeList(options);

    assertThat(mediaMetadataList).isNotNull();
    assertThat(mediaMetadataList).size().isGreaterThanOrEqualTo(73);
  }

  @Test
  public void testEpisodeScrape() throws Exception {
    TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
    OmdbTvShowMetadataProvider mp = new OmdbTvShowMetadataProvider();
    MediaMetadata md;

    options.getTvShowIds().put(MediaMetadata.IMDB, "tt0944947");
    options.setId(MediaMetadata.SEASON_NR, 1);
    options.setId(MediaMetadata.EPISODE_NR, 1);
    md = mp.getMetadata(options);

    assertThat(md.getId(MediaMetadata.IMDB)).isEqualTo("tt1480055");
    assertThat(md.getTitle()).isEqualTo("Winter Is Coming");
    assertThat(md.getYear()).isEqualTo(Integer.valueOf(2011));
    assertThat(md.getReleaseDate()).isNotNull();
    assertThat(md.getRuntime()).isEqualTo(Integer.valueOf(62));
    assertThat(md.getCastMembers(WRITER)).isNotNull();
    assertThat(md.getCastMembers(WRITER).size()).isGreaterThan(0);
    assertThat(md.getCastMembers(DIRECTOR)).isNotNull();
    assertThat(md.getCastMembers(DIRECTOR).size()).isGreaterThan(0);
    assertThat(md.getCastMembers(ACTOR)).isNotNull();
    assertThat(md.getCastMembers(ACTOR).size()).isGreaterThan(0);
    assertThat(md.getPlot()).isNotEmpty();
    assertThat(md.getCountries()).containsAnyOf("USA", "United States", "UK");
    assertThat(md.getSpokenLanguages()).contains("English");
    assertThat(md.getRatings()).size().isGreaterThan(0);
    assertThat(md.getCertifications()).contains(MediaCertification.US_TVMA);
    assertThat(md.getGenres()).contains(MediaGenres.ACTION, MediaGenres.ADVENTURE, MediaGenres.DRAMA);
  }
}
