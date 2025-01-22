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
package org.tinymediamanager.core.movie;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.tasks.MovieUpdateDatasourceTask;

public class MovieNfoWriteTest extends BasicMovieTest {

  private static final int NUMBER_OF_EXPECTED_MOVIES = 6;

  @Before
  public void setup() throws Exception {
    super.setup();

    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();

    // just a copy; we might have another movie test which uses these files
    copyResourceFolderToWorkFolder("testmovies_nfo");
    MovieModuleManager.getInstance().getSettings().addMovieDataSources(getWorkFolder().resolve("testmovies_nfo").toAbsolutePath().toString());
  }

  @After
  public void tearDownAfterTest() throws Exception {
    MovieModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
  }

  private void loadMovies() throws Exception {
    MovieUpdateDatasourceTask task = new MovieUpdateDatasourceTask();
    task.run();

    // let the propertychangeevents finish
    Thread.sleep(2000);
  }

  @Test
  public void testDefault() throws Exception {
    // load movies
    loadMovies();

    assertThat(MovieModuleManager.getInstance().getMovieList().getMovies().size()).isEqualTo(NUMBER_OF_EXPECTED_MOVIES);

    // just rewrite the NFO file
    for (Movie movie : MovieModuleManager.getInstance().getMovieList().getMovies()) {
      movie.writeNFO();
    }

    // and now check the file names
    Path base = getWorkFolder().resolve("testmovies_nfo");

    // bluray
    Path nfo = Paths.get(base.toString(), "BluRay", "BDMV", "index.nfo");
    assertThat(Files.exists(nfo)).isTrue();

    // plain dvd files
    nfo = Paths.get(base.toString(), "DVD", "VIDEO_TS.nfo");
    assertThat(Files.exists(nfo)).isTrue();

    // nested dvd files
    nfo = Paths.get(base.toString(), "DVDfolder", "VIDEO_TS", "VIDEO_TS.nfo");
    assertThat(Files.exists(nfo)).isTrue();

    // single movie dir
    nfo = Paths.get(base.toString(), "Single", "singlefile.nfo");
    assertThat(Files.exists(nfo)).isTrue();

    // multi movie dir
    nfo = Paths.get(base.toString(), "Multi1", "multifile1.nfo");
    assertThat(Files.exists(nfo)).isTrue();
    nfo = Paths.get(base.toString(), "Multi1", "multifile2.nfo");
    assertThat(Files.exists(nfo)).isTrue();
  }
}
