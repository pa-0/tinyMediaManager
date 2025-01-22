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

package org.tinymediamanager.scraper.universal_tvshow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroupType.AIRED;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviders;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;

public class ITUniversalTvShowMetadataProviderTest extends BasicITest {

  @Before
  public void setup() throws Exception {
    super.setup();
    // load all classpath plugins
    MediaProviders.loadMediaProviders();
  }

  @Test
  public void testSearchTmdb() {
    callSearch("tmdb", "Futurama");
  }

  @Test
  public void testSearchImdb() {
    callSearch("imdb", "Futurama");
  }

  @Test
  public void testSearchTvDb() {
    callSearch("tvdb", "Futurama");
  }

  @Test
  public void testSearchTrakt() {
    callSearch("trakt", "Futurama");
  }

  private void callSearch(String providerId, String searchString) {
    try {
      ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();

      mp.getProviderInfo().getConfig().setValue("search", providerId);

      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setSearchQuery(searchString);
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      List<MediaSearchResult> results = new ArrayList<>(mp.search(options));

      // did we get a result?
      assertThat(results).isNotEmpty();

      // are our results from the chosen provider?
      for (MediaSearchResult result : results) {
        // check this via the ID which must exist for the given scraper
        assertThat(result.getIdAsString(providerId)).isNotBlank();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTvdbFuturama() {
    try {
      scrapeFromTvdbFuturama("tvdb", "73871");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTvdbFuturamaWithTmdbid() {
    try {
      scrapeFromTvdbFuturama("tmdb", "615");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTvdbFuturamaWithImdbid() {
    try {
      scrapeFromTvdbFuturama("imdb", "tt0149460");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  private void scrapeFromTvdbFuturama(String providerId, String id) throws Exception {
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    createSingleScraperConfig(mp, "tvdb");

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setId(providerId, id);
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    MediaMetadata mediaMetadata = mp.getMetadata(options);

    assertThat(mediaMetadata.getIds()).containsValues(73871, "tt0149460");
    assertThat(mediaMetadata.getTitle()).isEqualTo("Futurama");
    assertThat(mediaMetadata.getOriginalTitle()).isEqualTo("Futurama");
    assertThat(mediaMetadata.getYear()).isEqualTo(1999);
    assertThat(mediaMetadata.getReleaseDate()).isNotNull();
    assertThat(mediaMetadata.getRuntime()).isGreaterThan(0);
    assertThat(mediaMetadata.getPlot()).isNotEmpty();
    assertThat(mediaMetadata.getRatings()).isNotEmpty();
    assertThat(mediaMetadata.getStatus()).isNotEqualByComparingTo(MediaAiredStatus.UNKNOWN);
    assertThat(mediaMetadata.getGenres()).isNotEmpty();
    assertThat(mediaMetadata.getCertifications()).isNotEmpty();
    assertThat(mediaMetadata.getProductionCompanies()).isNotEmpty();
    assertThat(mediaMetadata.getCastMembers()).isNotEmpty();
    assertThat(mediaMetadata.getTags()).isEmpty(); // no tags at tvdb
  }

  @Test
  public void testScrapeFromTmdbFuturama() {
    try {
      scrapeFromTmdbFuturama("tmdb", "615");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTmdbFuturamaWithTvdbId() {
    try {
      scrapeFromTmdbFuturama("tvdb", "73871");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTmdbFuturamaWithImdbId() {
    try {
      scrapeFromTmdbFuturama("imdb", "tt0149460");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  private void scrapeFromTmdbFuturama(String providerId, String id) throws Exception {
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    createSingleScraperConfig(mp, "tmdb");

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setId(providerId, id);
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    MediaMetadata mediaMetadata = mp.getMetadata(options);

    assertThat(mediaMetadata.getIds()).containsValues(73871, "tt0149460");
    assertThat(mediaMetadata.getTitle()).isEqualTo("Futurama");
    assertThat(mediaMetadata.getOriginalTitle()).isNotEmpty();
    assertThat(mediaMetadata.getYear()).isEqualTo(1999);
    assertThat(mediaMetadata.getReleaseDate()).isNotNull();
    assertThat(mediaMetadata.getRuntime()).isGreaterThan(0);
    assertThat(mediaMetadata.getPlot()).isNotEmpty();
    assertThat(mediaMetadata.getRatings()).isNotEmpty();
    assertThat(mediaMetadata.getStatus()).isNotEqualByComparingTo(MediaAiredStatus.UNKNOWN);
    assertThat(mediaMetadata.getGenres()).isNotEmpty();
    assertThat(mediaMetadata.getCertifications()).isNotEmpty();
    assertThat(mediaMetadata.getProductionCompanies()).isNotEmpty();
    assertThat(mediaMetadata.getCastMembers()).isNotEmpty();
    assertThat(mediaMetadata.getTags()).isNotEmpty();
  }

  @Test
  public void testScrapeFromImdbFuturama() {
    try {
      scrapeFromImdbFuturama("imdb", "tt0149460");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromImdbFuturamaWithTvdbId() {
    try {
      scrapeFromImdbFuturama("tvdb", "73871");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromImdbFuturamaWithTmdbId() {
    try {
      scrapeFromImdbFuturama("tmdb", "615");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  private void scrapeFromImdbFuturama(String providerId, String id) throws Exception {
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    createSingleScraperConfig(mp, "imdb");

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setId(providerId, id);
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    MediaMetadata mediaMetadata = mp.getMetadata(options);

    assertThat(mediaMetadata.getIds()).containsValues(73871, "tt0149460");
    assertThat(mediaMetadata.getTitle()).isEqualTo("Futurama");
    assertThat(mediaMetadata.getOriginalTitle()).isNotEmpty();
    assertThat(mediaMetadata.getYear()).isEqualTo(1999);
    assertThat(mediaMetadata.getReleaseDate()).isNotNull();
    assertThat(mediaMetadata.getRuntime()).isGreaterThan(0);
    assertThat(mediaMetadata.getPlot()).isNotEmpty();
    assertThat(mediaMetadata.getRatings()).isNotEmpty();
    assertThat(mediaMetadata.getStatus()).isEqualByComparingTo(MediaAiredStatus.ENDED);
    assertThat(mediaMetadata.getGenres()).isNotEmpty();
    assertThat(mediaMetadata.getCertifications()).isNotEmpty();
    assertThat(mediaMetadata.getProductionCompanies()).isNotEmpty();
    assertThat(mediaMetadata.getCastMembers()).isNotEmpty();
    assertThat(mediaMetadata.getTags()).isNotEmpty();
  }

  private void scrapeFromTraktFuturama(String providerId, String id) throws Exception {
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    createSingleScraperConfig(mp, "trakt");

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setId("tmdb", "615");
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    MediaMetadata mediaMetadata = mp.getMetadata(options);

    assertThat(mediaMetadata.getIds()).containsValues(73871, "tt0149460");
    assertThat(mediaMetadata.getTitle()).isEqualTo("Futurama");
    assertThat(mediaMetadata.getOriginalTitle()).isEqualTo("Futurama");
    assertThat(mediaMetadata.getYear()).isEqualTo(1999);
    assertThat(mediaMetadata.getReleaseDate()).isNotNull();
    assertThat(mediaMetadata.getRuntime()).isGreaterThan(0);
    assertThat(mediaMetadata.getPlot()).isNotEmpty();
    assertThat(mediaMetadata.getRatings()).isNotEmpty();
    assertThat(mediaMetadata.getStatus()).isNotEqualByComparingTo(MediaAiredStatus.UNKNOWN);
    assertThat(mediaMetadata.getGenres()).isNotEmpty();
    assertThat(mediaMetadata.getCertifications()).isNotEmpty();
    assertThat(mediaMetadata.getProductionCompanies()).isNotEmpty();
    assertThat(mediaMetadata.getCastMembers()).isNotEmpty();
    assertThat(mediaMetadata.getTags()).isEmpty(); // no tags from trakt
  }

  @Test
  public void testScrapeFromTraktFuturamaWithTmdbId() {
    try {
      scrapeFromTraktFuturama("tmdb", "615");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTraktFuturamaWithTvdbId() {
    try {
      scrapeFromTraktFuturama("tvdb", "73871");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTraktFuturamaWithImdbId() {
    try {
      scrapeFromTraktFuturama("imdb", "tt0149460");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  private void createSingleScraperConfig(ITvShowMetadataProvider mp, String providerId) {
    mp.getProviderInfo().getConfig().setValue("title", providerId);
    mp.getProviderInfo().getConfig().setValue("originalTitle", providerId);
    mp.getProviderInfo().getConfig().setValue("year", providerId);
    mp.getProviderInfo().getConfig().setValue("releaseDate", providerId);
    mp.getProviderInfo().getConfig().setValue("runtime", providerId);
    mp.getProviderInfo().getConfig().setValue("plot", providerId);
    mp.getProviderInfo().getConfig().setValue("genres", providerId);
    mp.getProviderInfo().getConfig().setValue("certifications", providerId);
    mp.getProviderInfo().getConfig().setValue("ratings", providerId);
    mp.getProviderInfo().getConfig().setValue("productionCompanies", providerId);
    mp.getProviderInfo().getConfig().setValue("castMembers", providerId);
    mp.getProviderInfo().getConfig().setValue("tags", providerId);
    mp.getProviderInfo().getConfig().setValue("status", providerId);
    mp.getProviderInfo().getConfig().setValue("countries", providerId);
  }

  @Test
  public void testGetEpisodeListingFromTvdbFuturamaWithTvdbId() throws Exception {
    getEpisodeListingTvShow("tvdb", "tvdb", "73871");
    getEpisodeListingEpisode("tvdb", "tvdb", "73871");
  }

  @Test
  public void testGetEpisodeListingFromTvdbFuturamaWithTmdbId() throws Exception {
    getEpisodeListingTvShow("tvdb", "tmdb", "615");
    getEpisodeListingEpisode("tvdb", "tmdb", "615");
  }

  @Test
  public void testGetEpisodeListingFromTvdbFuturamaWithImdbId() throws Exception {
    getEpisodeListingTvShow("tvdb", "imdb", "tt0149460");
    getEpisodeListingEpisode("tvdb", "imdb", "tt0149460");
  }

  @Test
  public void testGetEpisodeListingFromTmdbFuturamaWithTvdbId() throws Exception {
    getEpisodeListingTvShow("tmdb", "tvdb", "73871");
    getEpisodeListingEpisode("tmdb", "tvdb", "73871");
  }

  @Test
  public void testGetEpisodeListingFromTmdbFuturamaWithTmdbId() throws Exception {
    getEpisodeListingTvShow("tmdb", "tmdb", "615");
    getEpisodeListingEpisode("tmdb", "tmdb", "615");
  }

  @Test
  public void testGetEpisodeListingFromTmdbFuturamaWithImdbId() throws Exception {
    getEpisodeListingTvShow("tmdb", "imdb", "tt0149460");
    getEpisodeListingEpisode("tmdb", "imdb", "tt0149460");
  }

  @Test
  public void testGetEpisodeListingFromImdbFuturamaWithTvdbId() throws Exception {
    getEpisodeListingTvShow("imdb", "tvdb", "73871");
    getEpisodeListingEpisode("imdb", "tvdb", "73871");
  }

  @Test
  public void testGetEpisodeListingFromImdbFuturamaWithTmdbId() throws Exception {
    getEpisodeListingTvShow("imdb", "tmdb", "615");
    getEpisodeListingEpisode("imdb", "tmdb", "615");
  }

  @Test
  public void testGetEpisodeListingFromImdbFuturamaWithImdbId() throws Exception {
    getEpisodeListingTvShow("imdb", "imdb", "tt0149460");
    getEpisodeListingEpisode("imdb", "imdb", "tt0149460");
  }

  @Test
  public void testGetEpisodeListingFromTraktFuturamaWithTvdbId() throws Exception {
    getEpisodeListingTvShow("trakt", "tvdb", "73871");
    getEpisodeListingEpisode("trakt", "tvdb", "73871");
  }

  @Test
  public void testGetEpisodeListingFromTraktFuturamaWithTmdbId() throws Exception {
    getEpisodeListingTvShow("trakt", "tmdb", "615");
    getEpisodeListingEpisode("trakt", "tmdb", "615");
  }

  @Test
  public void testGetEpisodeListingFromTraktFuturamaWithImdbId() throws Exception {
    getEpisodeListingTvShow("trakt", "imdb", "tt0149460");
    getEpisodeListingEpisode("trakt", "imdb", "tt0149460");
  }

  private void getEpisodeListingTvShow(String scraperId, String providerId, String id) throws Exception {
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("episodes", scraperId);

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setId(providerId, id);
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    List<MediaMetadata> episodes = mp.getEpisodeList(options);

    assertThat(episodes.size()).isGreaterThan(100);
  }

  private void getEpisodeListingEpisode(String scraperId, String providerId, String id) throws Exception {
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("episodes", scraperId);

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setId(providerId, id);
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    List<MediaMetadata> episodes = mp.getEpisodeList(options);

    assertThat(episodes.size()).isGreaterThan(100);
  }

  @Test
  public void testScrapeEpisodeFromTvdbWithTvdbId() throws Exception {
    scrapeEpisodeSimple("tvdb", "tvdb", "79335", "307505");
  }

  @Test
  public void testScrapeEpisodeFromTvdbWithTmdbId() throws Exception {
    scrapeEpisodeSimple("tvdb", "tmdb", "1447", "66777");
  }

  @Test
  public void testScrapeEpisodeFromTvdbWithImdbId() throws Exception {
    scrapeEpisodeSimple("tvdb", "imdb", "tt0491738", "tt0836429");
  }

  @Test
  public void testScrapeEpisodeFromTmdbWithTvdbId() throws Exception {
    scrapeEpisodeSimple("tmdb", "tvdb", "79335", "307505");
  }

  @Test
  public void testScrapeEpisodeFromTmdbWithTmdbId() throws Exception {
    scrapeEpisodeSimple("tmdb", "tmdb", "1447", "66777");
  }

  @Test
  public void testScrapeEpisodeFromTmdbWithImdbId() throws Exception {
    scrapeEpisodeSimple("tmdb", "imdb", "tt0491738", "tt0836429");
  }

  @Test
  public void testScrapeEpisodeFromImdbWithTvdbId() throws Exception {
    scrapeEpisodeSimple("imdb", "tvdb", "79335", "307505");
  }

  @Test
  public void testScrapeEpisodeFromImdbWithTmdbId() throws Exception {
    scrapeEpisodeSimple("imdb", "tmdb", "1447", "66777");
  }

  @Test
  public void testScrapeEpisodeFromImdbWithImdbId() throws Exception {
    scrapeEpisodeSimple("imdb", "imdb", "tt0491738", "tt0836429");
  }

  @Test
  public void testScrapeEpisodeFromTraktWithTvdbId() throws Exception {
    scrapeEpisodeSimple("trakt", "tvdb", "79335", "307505");
  }

  @Test
  public void testScrapeEpisodeFromTraktWithTmdbId() throws Exception {
    scrapeEpisodeSimple("trakt", "tmdb", "1447", "66777");
  }

  @Test
  public void testScrapeEpisodeFromTraktWithImdbId() throws Exception {
    scrapeEpisodeSimple("trakt", "imdb", "tt0491738", "tt0836429");
  }

  private void scrapeEpisodeSimple(String scraperId, String providerId, String showId, String episodeId) throws Exception {
    // check S01/E09
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("episodes", scraperId);
    mp.getProviderInfo().getConfig().setValue("episodeTitle", scraperId);
    mp.getProviderInfo().getConfig().setValue("episodePlot", scraperId);
    mp.getProviderInfo().getConfig().setValue("episodeCastMembers", scraperId);
    mp.getProviderInfo().getConfig().setValue("episodeRatings", scraperId);

    TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
    options.getTvShowIds().put(providerId, showId);
    options.setId(providerId, episodeId);
    options.setLanguage(MediaLanguages.en);

    MediaMetadata md = mp.getMetadata(options);

    assertThat(md).isNotNull();
    assertThat(md.getEpisodeNumber(AIRED)).isNotNull();
    assertThat(md.getEpisodeNumber(AIRED).season()).isEqualTo(1);
    assertThat(md.getEpisodeNumber(AIRED).episode()).isEqualTo(9);
    assertThat(md.getTitle()).isEqualTo("Forget Me Not");
    assertThat(md.getPlot()).isNotEmpty();
    assertThat(md.getReleaseDate()).isNotNull();
    assertThat(md.getRatings()).isNotEmpty();

    if (!"trakt".equals(scraperId)) {
      // trakt does not support episode guests
      assertThat(md.getCastMembers()).isNotEmpty();
    }
  }

  @Test
  public void testScrapeEpisodeMixed() throws Exception {
    // check S01/E09
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("episodes", "tmdb");
    mp.getProviderInfo().getConfig().setValue("episodeTitle", "trakt");
    mp.getProviderInfo().getConfig().setValue("episodePlot", "tmdb");
    mp.getProviderInfo().getConfig().setValue("episodeCastMembers", "imdb");
    mp.getProviderInfo().getConfig().setValue("episodeRatings", "imdb");

    TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
    options.getTvShowIds().put("imdb", "tt0491738");
    options.setId("tmdb", "66777");
    options.setLanguage(MediaLanguages.en);

    MediaMetadata md = mp.getMetadata(options);

    assertThat(md).isNotNull();
    assertThat(md.getEpisodeNumber(AIRED)).isNotNull();
    assertThat(md.getEpisodeNumber(AIRED).season()).isEqualTo(1);
    assertThat(md.getEpisodeNumber(AIRED).episode()).isEqualTo(9);
    assertThat(md.getTitle()).isEqualTo("Forget Me Not");
    assertThat(md.getPlot()).isNotEmpty();
    assertThat(md.getReleaseDate()).isNotNull();
    assertThat(md.getRatings()).isNotEmpty();
    assertThat(md.getCastMembers()).isNotEmpty();
  }
}
