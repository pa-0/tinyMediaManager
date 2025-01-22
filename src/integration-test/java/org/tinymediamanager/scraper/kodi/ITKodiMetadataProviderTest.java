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

package org.tinymediamanager.scraper.kodi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class ITKodiMetadataProviderTest extends BasicITest {

  @Test
  public void xmlHeaders() {
    Assert.assertFalse(
        KodiUtil.fixXmlHeader("<?xml version=\"1.0\" test=\"false\" encoding=\"UTF-8\"?>\n<scraper framework=\"1.1\" date=\"2012-01-16\">")
            .contains("test"));
    Assert.assertFalse(KodiUtil.fixXmlHeader("<?xml version=\'1.0\' encoding=\"UTF-8\" gzip=\"yes\" standalone=\"yes\" ?>").contains("gzip"));
    Assert.assertFalse(
        KodiUtil.fixXmlHeader("<?xml version=\"1.0\' encoding=\"UTF-8\" gzip=\"yes\"?><result key=\"true\">key=\"true\"</result>").contains("gzip"));
    Assert.assertFalse(KodiUtil.fixXmlHeader("<?xml version=\"1.0\' encoding=\"UTF-8\" asdf=yes?>").contains("asdf"));
    Assert.assertEquals("", KodiUtil.fixXmlHeader(""));
  }

  @Test
  public void loadXbmcScrapers() {
    try {
      KodiMetadataProvider kodiMetadataProvider = new KodiMetadataProvider();
      List<IMediaProvider> movieScrapers = kodiMetadataProvider.getPluginsForType(MediaType.MOVIE);
      assertThat(movieScrapers).isNotNull().isNotEmpty();
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testKodiTVScraper() {
    try {
      LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
      lc.getLogger("org.tinymediamanager.scraper").setLevel(Level.TRACE);

      KodiMetadataProvider kodiMetadataProvider = new KodiMetadataProvider();
      List<IMediaProvider> scraper = kodiMetadataProvider.getPluginsForType(MediaType.TV_SHOW);
      assertThat(scraper).isNotNull().isNotEmpty();

      ITvShowMetadataProvider show = null;
      for (IMediaProvider mp : scraper) {
        if (mp.getProviderInfo().getId().equals("metadata.tvdb.com")) {
          show = (ITvShowMetadataProvider) mp;
          break;
        }
      }
      assertThat(show).isNotNull();

      // search show
      TvShowSearchAndScrapeOptions searchOptions = new TvShowSearchAndScrapeOptions();
      searchOptions.setSearchQuery("21 Jump Street");
      searchOptions.setSearchYear(1987);
      searchOptions.setLanguage(MediaLanguages.de);
      searchOptions.setCertificationCountry(CountryCode.US);
      searchOptions.setReleaseDateCountry("US");

      List<MediaSearchResult> results = new ArrayList<>(show.search(searchOptions));
      for (MediaSearchResult mediaSearchResult : results) {
        System.out.println(mediaSearchResult);
      }

      // scrape show details (and cache episodes!)
      TvShowSearchAndScrapeOptions scrapeOptions = new TvShowSearchAndScrapeOptions();
      scrapeOptions.setSearchResult(results.get(0));
      MediaMetadata md = show.getMetadata(scrapeOptions);
      scrapeOptions.setMetadata(md);

      // get episode list (when cached)
      // List<MediaEpisode> epl = show.getEpisodeList(scrapeOptions);
      // for (MediaEpisode me : epl) {
      // System.out.println(me);
      // }

      // get single episode (when cached)
      TvShowEpisodeSearchAndScrapeOptions episodeOptions = new TvShowEpisodeSearchAndScrapeOptions();
      episodeOptions.setId("metadata.tvdb.com", "77585");
      episodeOptions.setId(MediaMetadata.SEASON_NR, "2");
      episodeOptions.setId(MediaMetadata.EPISODE_NR, "4");
      episodeOptions.setMetadata(md);
      MediaMetadata ep = show.getMetadata(episodeOptions);
      System.out.println(ep);

    }
    catch (Exception e) {
      fail(e.getMessage(), e);
    }
  }

  @Test
  public void testTmdbScraper() {
    try {
      KodiMetadataProvider kodiMetadataProvider = new KodiMetadataProvider();
      List<IMediaProvider> movieScrapers = kodiMetadataProvider.getPluginsForType(MediaType.MOVIE);
      assertThat(movieScrapers).isNotNull().isNotEmpty();

      IMovieMetadataProvider tmdb = null;
      for (IMediaProvider mp : movieScrapers) {
        if (mp.getProviderInfo().getId().equals("metadata.themoviedb.org")) {
          tmdb = (IMovieMetadataProvider) mp;
          break;
        }
      }

      assertThat(tmdb).isNotNull();

      // search
      MovieSearchAndScrapeOptions searchOptions = new MovieSearchAndScrapeOptions();
      searchOptions.setSearchQuery("Harry Potter and the Philosopher's Stone");
      searchOptions.setSearchYear(2001);
      searchOptions.setLanguage(MediaLanguages.en);
      searchOptions.setCertificationCountry(CountryCode.US);
      searchOptions.setReleaseDateCountry("US");

      List<MediaSearchResult> results = new ArrayList<>(tmdb.search(searchOptions));

      assertThat(results).isNotNull();
      assertThat(results.size()).isGreaterThan(0);

      // scrape
      MovieSearchAndScrapeOptions scrapeOptions = new MovieSearchAndScrapeOptions();
      scrapeOptions.setSearchResult(results.get(0));
      MediaMetadata md = tmdb.getMetadata(scrapeOptions);

      assertEquals("Harry Potter and the Philosopher's Stone", md.getTitle());
      assertEquals("Harry Potter and the Philosopher's Stone", md.getOriginalTitle());
      assertEquals(2001, md.getYear());
      assertThat(md.getPlot()).startsWith("Harry Potter has lived under the stairs");
      assertEquals(152, md.getRuntime());
      assertThat(md.getTagline()).isEqualToIgnoringCase("Let the Magic Begin.");
      assertThat(md.getCollectionName()).startsWith("Harry Potter Collection");

      assertNotNull(md.getCastMembers(ACTOR));
      assertThat(md.getCastMembers(ACTOR).size()).isGreaterThan(0);
      assertThat(md.getCastMembers(ACTOR).get(0).getName()).isNotEmpty();
      assertThat(md.getCastMembers(ACTOR).get(0).getRole()).isNotEmpty();

      assertNotNull(md.getCastMembers(DIRECTOR));
      assertThat(md.getCastMembers(DIRECTOR).size()).isGreaterThan(0);
      assertThat(md.getCastMembers(DIRECTOR).get(0).getName()).isNotEmpty();
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
