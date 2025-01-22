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

package org.tinymediamanager.core.movie.tasks;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.BasicMovieTest;
import org.tinymediamanager.core.movie.MovieComparator;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;

/**
 * This class cannot run, since Settings() is STATIC<br>
 * run these test individually (for now)
 * 
 * @author Myron Boyle
 *
 */
public class MovieUpdateDatasourceTaskTest extends BasicMovieTest {

  private static final int NUMBER_OF_EXPECTED_MOVIES     = 103;
  private static final int NUMBER_OF_STACKED_MOVIES      = 14;
  private static final int NUMBER_OF_DISC_MOVIES         = 7;
  private static final int NUMBER_OF_MOVIES_IN_MMD       = 59;
  private static final int NUMBER_OF_EXPECTED_MEDIAFILES = 227;

  @Before
  public void setup() throws Exception {
    super.setup();

    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();

    // just a copy; we might have another movie test which uses these files
    copyResourceFolderToWorkFolder("testmovies");
    MovieModuleManager.getInstance().getSettings().addMovieDataSources(getWorkFolder().resolve("testmovies").toAbsolutePath().toString());
  }

  @After
  public void tearDownAfterTest() throws Exception {
    MovieModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
  }

  @Test
  public void udsNew() throws Exception {
    MovieUpdateDatasourceTask task = new MovieUpdateDatasourceTask();
    task.run();

    showEntries();
  }

  private void showEntries() throws Exception {
    // wait until all movies have been added (let propertychanges finish)
    for (int i = 0; i < 3; i++) {
      if (MovieModuleManager.getInstance().getMovieList().getMovieCount() == NUMBER_OF_EXPECTED_MOVIES) {
        break;
      }

      // not all here yet? wait for a second
      System.out.println("waiting for 1000 ms");
      Thread.sleep(1000);
    }

    int stack = 0;
    int disc = 0;
    int mmd = 0;
    int mfCnt = 0;
    List<Movie> movies = MovieModuleManager.getInstance().getMovieList().getMovies();
    movies.sort(new MovieComparator());
    for (Movie m : movies) {
      System.out.println(rpad(m.getTitle(), 30) + "(Disc:" + rpad(m.isDisc(), 5) + " Stack:" + rpad(m.isStacked(), 5) + " Multi:"
          + rpad(m.isMultiMovieDir(), 5) + ")\t" + m.getPathNIO());
      if (m.isStacked()) {
        stack++;
      }
      if (m.isDisc()) {
        disc++;
      }
      if (m.isMultiMovieDir()) {
        mmd++;
      }
      mfCnt += m.getMediaFiles().size();
    }

    assertEqual("Amount of movies does not match!", NUMBER_OF_EXPECTED_MOVIES, MovieModuleManager.getInstance().getMovieList().getMovieCount());
    assertEqual("Amount of stacked movies does not match!", NUMBER_OF_STACKED_MOVIES, stack);
    assertEqual("Amount of disc folders does not match!", NUMBER_OF_DISC_MOVIES, disc);
    assertEqual("Amount of movies in multimoviedirs does not match!", NUMBER_OF_MOVIES_IN_MMD, mmd);
    assertEqual("Amount of mediafiles does not match!", NUMBER_OF_EXPECTED_MEDIAFILES, mfCnt);
  }

  public static String rpad(Object s, int n) {
    return String.format("%1$-" + n + "s", s);
  }
}
