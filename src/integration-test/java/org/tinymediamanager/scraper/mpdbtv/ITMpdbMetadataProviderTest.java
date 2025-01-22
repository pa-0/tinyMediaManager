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
package org.tinymediamanager.scraper.mpdbtv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.exceptions.ScrapeException;

public class ITMpdbMetadataProviderTest extends BasicITest {

  /**
   * Testing ProviderInfo
   */
  @Test
  public void testProviderInfo() {
    try {
      MpdbMovieMetadataProvider mp = new MpdbMovieMetadataProvider();
      MediaProviderInfo providerInfo = mp.getProviderInfo();

      assertThat(providerInfo.getDescription()).isNotNull();
      assertThat(providerInfo.getId()).isNotNull();
      assertThat(providerInfo.getName()).isNotNull();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testSearch() throws ScrapeException {
    MpdbMovieMetadataProvider mp = new MpdbMovieMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("aboKey", System.getProperty("mpdb_aboKey"));
    mp.getProviderInfo().getConfig().setValue("username", System.getProperty("mpdb_username"));

    MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
    options.setSearchQuery("Batman");
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    List<MediaSearchResult> result = new ArrayList<>(mp.search(options));

    assertThat(result).isNotNull();
    assertThat(result.size()).isGreaterThan(40);

  }

  @Test
  public void testScrape() throws ScrapeException {
    MpdbMovieMetadataProvider mp = new MpdbMovieMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("aboKey", System.getProperty("mpdb_aboKey"));
    mp.getProviderInfo().getConfig().setValue("username", System.getProperty("mpdb_username"));

    MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
    options.setId("mpdbtv", "3193");
    options.setLanguage(MediaLanguages.fr);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    MediaMetadata result = mp.getMetadata(options);
    assertThat(result).isNotNull();
    assertThat(result.getOriginalTitle()).isEqualTo("Star Wars: Clone Wars ");
    assertThat(result.getId("allocine")).isEqualTo("55310");
    assertThat(result.getId("imdb")).isEqualTo("tt0361243");
    assertThat(result.getRuntime()).isEqualTo(0);
    assertThat(result.getTitle()).isEqualTo("Star Wars : La Guerre des Clones");

  }

}
