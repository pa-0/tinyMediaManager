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

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.scraper.thetvdb.entities.AllSeriesResponse;
import org.tinymediamanager.scraper.thetvdb.entities.EpisodeBaseResponse;
import org.tinymediamanager.scraper.thetvdb.entities.EpisodeExtendedResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SeasonBaseResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SeasonExtendedResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SeasonType;
import org.tinymediamanager.scraper.thetvdb.entities.SeriesBaseResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SeriesEpisodesResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SeriesExtendedResponse;

public class ITTheTvDbApiTest extends BasicITest {
  private TheTvDbController theTvDbController;

  @Before
  public void setup() throws Exception {
    super.setup();
    TheTvDbMetadataProvider md = new TheTvDbTvShowMetadataProvider();

    theTvDbController = new TheTvDbController();
    theTvDbController.setAuthToken(md.getAuthToken());
  }

  @Test
  public void testAllSeries() throws Exception {
    AllSeriesResponse response = theTvDbController.getSeriesService().getAllSeries(1).execute().body();
    assertThat(response.data).isNotEmpty();
  }

  @Test
  public void testSeriesBase() throws Exception {
    SeriesBaseResponse response = theTvDbController.getSeriesService().getSeriesBase(79335).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.id).isEqualTo(79335);
    assertThat(response.data.name).isEqualTo("Psych");
  }

  @Test
  public void testSeriesExtended() throws Exception {
    SeriesExtendedResponse response = theTvDbController.getSeriesService().getSeriesExtended(79335).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.id).isEqualTo(79335);
    assertThat(response.data.name).isEqualTo("Psych");
  }

  @Test
  public void testSeriesEpisodes() throws Exception {
    SeriesEpisodesResponse response = theTvDbController.getSeriesService().getSeriesEpisodes(79335, SeasonType.DEFAULT, 0).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.series).isNotNull();
    assertThat(response.data.series.id).isEqualTo(79335);
    assertThat(response.data.series.name).isEqualTo("Psych");
    assertThat(response.data.episodes).isNotEmpty();
  }

  @Test
  public void testSeasonsBase() throws Exception {
    SeasonBaseResponse response = theTvDbController.getSeasonsService().getSeasonBase(16284).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.number).isEqualTo(1);
    assertThat(response.data.seriesId).isEqualTo(79335);
  }

  @Test
  public void testSeasonsExtended() throws Exception {
    SeasonExtendedResponse response = theTvDbController.getSeasonsService().getSeasonExtended(16284).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.number).isEqualTo(1);
    assertThat(response.data.seriesId).isEqualTo(79335);
    assertThat(response.data.episodes).isNotEmpty();
    assertThat(response.data.episodes.get(0).episodeNumber).isEqualTo(1);
    assertThat(response.data.episodes.get(0).seasonNumber).isEqualTo(1);
  }

  @Test
  public void testEpisodesBase() throws Exception {
    EpisodeBaseResponse response = theTvDbController.getEpisodesService().getEpisodeBase(307497).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.episodeNumber).isEqualTo(1);
    assertThat(response.data.seasonNumber).isEqualTo(1);
  }

  @Test
  public void testEpisodesExtended() throws Exception {
    EpisodeExtendedResponse response = theTvDbController.getEpisodesService().getEpisodeExtended(307497).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.episodeNumber).isEqualTo(1);
    assertThat(response.data.seasonNumber).isEqualTo(1);
  }
}
