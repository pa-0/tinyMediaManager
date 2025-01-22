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

package org.tinymediamanager.thirdparty.trakttv;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;

public class ITTraktTvTest extends BasicITest {

  private static final TraktTv t = TraktTv.getInstance();

  @Before
  public void setup() throws Exception {
    super.setup();

    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();
    TvShowModuleManager.getInstance().startUp();
  }

  @After
  public void tearDown() throws Exception {
    TmmModuleManager.getInstance().shutDown();
    MovieModuleManager.getInstance().shutDown();
    TvShowModuleManager.getInstance().shutDown();
  }

  @Test
  public void auth() {
    System.out.println();
  }

  @Test
  public void syncTraktMovieCollection() throws Exception {
    t.syncTraktMovieCollection(MovieModuleManager.getInstance().getMovieList().getMovies());
  }

  @Test
  public void syncTraktMovieWatched() throws Exception {
    t.syncTraktMovieWatched(MovieModuleManager.getInstance().getMovieList().getMovies());
  }

  @Test
  public void syncTraktTvShowCollection() throws Exception {
    t.syncTraktTvShowCollection(TvShowModuleManager.getInstance().getTvShowList().getTvShows());
  }

  @Test
  public void syncTraktTvShowWatched() throws Exception {
    t.syncTraktTvShowWatched(TvShowModuleManager.getInstance().getTvShowList().getTvShows());
  }

  // @Test
  // public void getTvLib() {
  // List<TvShow> shows = t.getManager().userService().libraryShowsWatched(Settings.getInstance().getTraktUsername(), Extended.MIN);
  // System.out.println(shows.size());
  // }
  //
  // @Test
  // public void getGenres() {
  // List<Genre> mg = t.getManager().genreService().movies();
  // mg.addAll(t.getManager().genreService().shows());
  // for (Genre genre : mg) {
  // System.out.println(genre.name);
  // }
  // }

  @Test
  public void clearTvShows() throws Exception {
    t.clearTraktTvShows();
  }

  @Test
  public void clearMovies() throws Exception {
    t.clearTraktMovies();
  }
}
