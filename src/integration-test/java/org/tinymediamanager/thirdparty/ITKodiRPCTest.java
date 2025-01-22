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

package org.tinymediamanager.thirdparty;

import java.nio.file.Paths;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.MovieDetail;
import org.tinymediamanager.jsonrpc.config.HostConfig;

public class ITKodiRPCTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ITKodiRPCTest.class);
  // *************************************************************************************
  // you need to enable Kodi -> remote control from OTHER machines (to open TCP port 9090)
  // and you need to enable webserver -> some calls are POSTed (not async)
  // *************************************************************************************

  @Test
  public void events() {
    KodiRPC.getInstance();
    while (true) {
      // do nothing, just wait for events...
    }
  }

  @Test
  public void getVersion() {
    System.out.println(KodiRPC.getInstance().getVersion());
  }

  @Test
  public void getAllMoviesSYNC() {
    List<MovieDetail> movies = KodiRPC.getInstance().getAllMoviesSYNC();
    if (movies == null) {
      LOGGER.error("no movies found");
    }
    else {
      LOGGER.info("found " + movies.size() + " movies");
      for (MovieDetail res : movies) {
        for (String c : res.country) {
          System.out.println(c);
        }
        LOGGER.debug(res.toString());
      }
    }
  }

  @Test
  public void getDataSources() {
    for (String ds : KodiRPC.getInstance().getVideoDataSources().keySet()) {
      System.out.println(ds);
    }
  }

  @Test
  public void getAllTvShows() {
    KodiRPC.getInstance().getAllTvShows();
  }

  @Test
  public void testEp() {
    TvShow show = TvShowModuleManager.getInstance().getTvShowList().getTvShowByPath(Paths.get("target\\test-classes\\testtvshows\\Futurama (1999)"));
    System.out.println(KodiRPC.getInstance().getEpisodeId(show.getEpisodes().get(0)));
    System.out.println(KodiRPC.getInstance().getEpisodeId(show.getEpisodes().get(1)));
    System.out.println(KodiRPC.getInstance().getEpisodeId(show.getEpisodes().get(2)));
  }

  @BeforeClass
  public static void setUp() {
    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();
    TvShowModuleManager.getInstance().startUp();

    // Upnp.getInstance().createUpnpService();
    // Upnp.getInstance().sendPlayerSearchRequest();
    try {
      HostConfig config = new HostConfig("127.0.0.1", 8080, "kodi", "kodi");
      KodiRPC.getInstance().connect(config);
      Thread.sleep(1000); // FIXME: create task
    }
    catch (Exception e) {
      System.err.println(e.getMessage());
      Assert.fail(e.getMessage());
    }
  }

  @AfterClass
  public static void tearDown() throws Exception {
    Thread.sleep(10000); // wait a bit - async
    KodiRPC.getInstance().disconnect();
    TvShowModuleManager.getInstance().shutDown();
    MovieModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
    Thread.sleep(200); // wait a bit - async
  }
}
