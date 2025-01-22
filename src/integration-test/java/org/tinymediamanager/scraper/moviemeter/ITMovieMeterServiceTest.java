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
package org.tinymediamanager.scraper.moviemeter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.scraper.moviemeter.entities.MMFilm;
import org.tinymediamanager.scraper.moviemeter.services.FilmService;
import org.tinymediamanager.scraper.moviemeter.services.SearchService;

import retrofit2.Response;

public class ITMovieMeterServiceTest extends BasicITest {

  @Test
  public void testFilmService() throws Exception {
    MovieMeter movieMeter = new MovieMeter();
    movieMeter.setApiKey(new MovieMeterMovieMetadataProvider().getApiKey());

    try {
      FilmService filmService = movieMeter.getFilmService();

      // Avatar by MM id
      Response<MMFilm> resp = filmService.getMovieInfo(17552).execute();
      MMFilm film = resp.body();

      assertThat(film).isNotNull();
      assertThat(film.title).isEqualTo("Avatar");

      // Avatar by imdb id
      film = filmService.getMovieInfoByImdbId("tt0499549").execute().body();

      assertThat(film).isNotNull();
      assertThat(film.title).isEqualTo("Avatar");

    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSearchService() throws Exception {
    MovieMeter movieMeter = new MovieMeter();
    movieMeter.setApiKey(new MovieMeterMovieMetadataProvider().getApiKey());

    try {
      SearchService searchService = movieMeter.getSearchService();
      List<MMFilm> result = searchService.searchFilm("avatar").execute().body();

      assertThat(result).isNotNull();
      assertThat(result).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }
}
