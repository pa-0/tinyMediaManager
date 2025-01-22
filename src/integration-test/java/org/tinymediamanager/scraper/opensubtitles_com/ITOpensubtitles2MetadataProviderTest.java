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

package org.tinymediamanager.scraper.opensubtitles_com;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.TreeMap;

import org.junit.BeforeClass;
import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.scraper.opensubtitles_com.model.DownloadRequest;
import org.tinymediamanager.scraper.opensubtitles_com.model.DownloadResponse;
import org.tinymediamanager.scraper.opensubtitles_com.model.LoginRequest;
import org.tinymediamanager.scraper.opensubtitles_com.model.LoginResponse;
import org.tinymediamanager.scraper.opensubtitles_com.model.SearchResponse;

import retrofit2.Response;

public class ITOpensubtitles2MetadataProviderTest extends BasicITest {

  private static Controller controller;

  @BeforeClass
  public static void setupClass() throws Exception {
    controller = new Controller();
    controller.setApiKey(System.getenv("OPEN_SUBTITLES_COM_API_KEY"));
    controller.setUsername(System.getenv("OPEN_SUBTITLES_COM_USERNAME"));
    controller.setPassword(System.getenv("OPEN_SUBTITLES_COM_PASSWORD"));
    controller.authenticate();
  }

  @Test
  public void testLogin() throws Exception {
    Controller controller = new Controller();
    controller.setApiKey(System.getenv("OPEN_SUBTITLES_COM_API_KEY"));

    LoginRequest loginRequest = new LoginRequest();
    loginRequest.username = System.getenv("OPEN_SUBTITLES_COM_USERNAME");
    loginRequest.password = System.getenv("OPEN_SUBTITLES_COM_PASSWORD");

    Response<LoginResponse> response = controller.getService().login(loginRequest).execute();
    assertThat(response.body()).isNotNull();
    assertThat(response.body().status).isEqualTo(200);
  }

  @Test
  public void testSearchByHash() throws Exception {
    // hit by movie hash
    Map<String, String> query = new TreeMap<>();
    query.put("moviehash", "8e245d9679d31e12");
    query.put("moviehash_match", "only");
    query.put("languages", "en");

    Response<SearchResponse> response = controller.getService().search(query).execute();
    assertThat(response.body()).isNotNull();
    assertThat(response.body().totalCount).isNotZero();
    assertThat(response.body().data.get(0).attributes.featureDetails.imdbId).isEqualTo(3358048);
  }

  @Test
  public void testSearchByImdbId() throws Exception {
    // hit by imdb id (IMDB id is stored as integer on opensubtitles.com)
    Map<String, String> query = new TreeMap<>();
    query.put("imdb_id", String.valueOf(OpenSubtitlesComSubtitleProvider.formatImdbId("tt0103064")));
    query.put("languages", "en");

    Response<SearchResponse> response = controller.getService().search(query).execute();
    assertThat(response.body()).isNotNull();
    assertThat(response.body().totalCount).isNotZero();
    assertThat(response.body().data.get(0).attributes.featureDetails.imdbId).isEqualTo(OpenSubtitlesComSubtitleProvider.formatImdbId("tt0103064"));
  }

  @Test
  public void testSearchByTmdbId() throws Exception {
    // hit by tmdb id
    Map<String, String> query = new TreeMap<>();
    query.put("tmdb_id", String.valueOf(280));
    query.put("languages", "en");

    Response<SearchResponse> response = controller.getService().search(query).execute();
    assertThat(response.body()).isNotNull();
    assertThat(response.body().totalCount).isNotZero();
    assertThat(response.body().data.get(0).attributes.featureDetails.imdbId).isEqualTo(OpenSubtitlesComSubtitleProvider.formatImdbId("tt0103064"));
  }

  @Test
  public void testSearchByQuery() throws Exception {
    // hit by query
    Map<String, String> query = new TreeMap<>();
    query.put("query", "Terminator 2: Judgment Day");
    query.put("languages", "en");

    Response<SearchResponse> response = controller.getService().search(query).execute();
    assertThat(response.body()).isNotNull();
    assertThat(response.body().totalCount).isNotZero();
    assertThat(response.body().data.get(0).attributes.featureDetails.imdbId).isEqualTo(OpenSubtitlesComSubtitleProvider.formatImdbId("tt0103064"));
  }

  @Test
  public void testDownload() throws Exception {
    DownloadRequest request = new DownloadRequest();
    request.file_id = 250393;
    request.sub_format = "srt";
    request.in_fps = 25;
    request.out_fps = 30;

    Response<DownloadResponse> response = controller.getService().download(request).execute();
    assertThat(response.body()).isNotNull();
    assertThat(response.body().link).isNotEmpty();
    // assertThat(response.body().requestsConsumed).isGreaterThan(0); // zero for the time of testing
  }
}
