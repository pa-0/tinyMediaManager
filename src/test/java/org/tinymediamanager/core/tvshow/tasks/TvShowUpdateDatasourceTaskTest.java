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

package org.tinymediamanager.core.tvshow.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.tvshow.BasicTvShowTest;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;

public class TvShowUpdateDatasourceTaskTest extends BasicTvShowTest {
  private static final int NUMBER_OF_EXPECTED_SHOWS      = 17;
  private static final int NUMBER_OF_EXPECTED_EPISODES   = 164;
  private static final int NUMBER_OF_EXPECTED_MEDIAFILES = 408;

  @Before
  public void setup() throws Exception {
    super.setup();

    TmmModuleManager.getInstance().startUp();
    TvShowModuleManager.getInstance().startUp();

    // just a copy; we might have another movie test which uses these files
    copyResourceFolderToWorkFolder("testtvshows");
    TvShowModuleManager.getInstance().getSettings().addTvShowDataSources(getWorkFolder().resolve("testtvshows").toAbsolutePath().toString());
  }

  @After
  public void tearDownAfterTest() throws Exception {
    TvShowModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
  }

  @Test
  public void udsNew() throws Exception {
    TvShowUpdateDatasourceTask task = new TvShowUpdateDatasourceTask();
    task.run();

    check();
  }

  private void check() throws Exception {
    TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

    // wait until all TV shows have been added (let propertychanges finish)
    for (int i = 0; i < 3; i++) {
      if (tvShowList.getTvShows().size() == NUMBER_OF_EXPECTED_SHOWS) {
        break;
      }

      // not all here yet? wait for a second
      System.out.println("waiting for 1000 ms");
      Thread.sleep(1000);
    }

    int episodeCnt = 0;
    int mfCnt = 0;
    // do some checks before shutting down the database
    for (TvShow show : tvShowList.getTvShows()) {
      System.out.println(show.getPath());
      mfCnt += show.getMediaFiles().size();

      for (TvShowSeason season : show.getSeasons()) {
        mfCnt += season.getMediaFiles().size();
      }

      // check for every found episode that it has at least one VIDEO file
      for (TvShowEpisode episode : show.getEpisodes()) {
        assertThat(episode.getMediaFiles(MediaFileType.VIDEO)).isNotEmpty();
        episodeCnt++;
        mfCnt += episode.getMediaFiles().size();
      }
    }

    assertThat(tvShowList.getTvShows().size()).isEqualTo(NUMBER_OF_EXPECTED_SHOWS);
    assertThat(episodeCnt).isEqualTo(NUMBER_OF_EXPECTED_EPISODES);
    assertThat(mfCnt).isEqualTo(NUMBER_OF_EXPECTED_MEDIAFILES);

    ///////////////////////////////////////////////////////////////////////////////////////
    // Breaking Bad
    ///////////////////////////////////////////////////////////////////////////////////////
    TvShow show = tvShowList.getTvShowByPath(getWorkFolder().resolve("testtvshows/Breaking Bad"));
    assertThat(show).isNotNull();
    assertThat(show.getTitle()).isEqualTo("Breaking Bad");
    assertThat(show.getEpisodes().size()).isEqualTo(62);
    assertThat(show.getSeasons().size()).isEqualTo(5);

    List<TvShowSeason> seasons = new ArrayList<>(show.getSeasons());
    // Collections.sort(seasons, seasonComparator);
    Object[] a = seasons.toArray();
    Arrays.sort(a);
    for (int i = 0; i < a.length; i++) {
      seasons.set(i, (TvShowSeason) a[i]);
    }

    assertThat(seasons.get(0).getSeason()).isEqualTo(1);
    assertThat(seasons.get(0).getEpisodes().size()).isEqualTo(7);
    assertThat(seasons.get(1).getSeason()).isEqualTo(2);
    assertThat(seasons.get(1).getEpisodes().size()).isEqualTo(13);
    assertThat(seasons.get(2).getSeason()).isEqualTo(3);
    assertThat(seasons.get(2).getEpisodes().size()).isEqualTo(13);
    assertThat(seasons.get(3).getSeason()).isEqualTo(4);
    assertThat(seasons.get(3).getEpisodes().size()).isEqualTo(13);
    assertThat(seasons.get(4).getSeason()).isEqualTo(5);
    assertThat(seasons.get(4).getEpisodes().size()).isEqualTo(16);

    ///////////////////////////////////////////////////////////////////////////////////////
    // Firefly
    ///////////////////////////////////////////////////////////////////////////////////////
    show = tvShowList.getTvShowByPath(getWorkFolder().resolve("testtvshows/Firefly"));
    assertThat(show).isNotNull();
    assertThat(show.getTitle()).isEqualTo("Firefly");
    assertThat(show.getEpisodes().size()).isEqualTo(14);
    assertThat(show.getSeasons().size()).isEqualTo(1);

    seasons = new ArrayList<>(show.getSeasons());
    // Collections.sort(seasons, seasonComparator);
    a = seasons.toArray();
    Arrays.sort(a);
    for (int i = 0; i < a.length; i++) {
      seasons.set(i, (TvShowSeason) a[i]);
    }

    assertThat(seasons.get(0).getSeason()).isEqualTo(1);
    assertThat(seasons.get(0).getEpisodes().size()).isEqualTo(14);

    ///////////////////////////////////////////////////////////////////////////////////////
    // Futurama
    ///////////////////////////////////////////////////////////////////////////////////////
    show = tvShowList.getTvShowByPath(getWorkFolder().resolve("testtvshows/Futurama (1999)"));
    assertThat(show).isNotNull();
    assertThat(show.getTitle()).isEqualTo("Futurama");
    assertThat(show.getEpisodes().size()).isEqualTo(44);
    assertThat(show.getSeasons().size()).isEqualTo(5); // 3 with episodes, 2 more with only artwork

    seasons = new ArrayList<>(show.getSeasons());
    // Collections.sort(seasons, seasonComparator);
    a = seasons.toArray();
    Arrays.sort(a);
    for (int i = 0; i < a.length; i++) {
      seasons.set(i, (TvShowSeason) a[i]);
    }

    assertThat(seasons.get(1).getSeason()).isEqualTo(1);
    assertThat(seasons.get(1).getEpisodes().size()).isEqualTo(9);
    assertThat(seasons.get(2).getSeason()).isEqualTo(2);
    assertThat(seasons.get(2).getEpisodes().size()).isEqualTo(20);
    assertThat(seasons.get(3).getSeason()).isEqualTo(3);
    assertThat(seasons.get(3).getEpisodes().size()).isEqualTo(15);

    ///////////////////////////////////////////////////////////////////////////////////////
    // unknown -1/-1 detection
    ///////////////////////////////////////////////////////////////////////////////////////
    show = tvShowList.getTvShowByPath(getWorkFolder().resolve("testtvshows/unknown"));
    assertThat(show).isNotNull();
    assertThat(show.getTitle()).isEqualTo("unknown");
    assertThat(show.getEpisodes().size()).isEqualTo(1);
    assertThat(show.getEpisodes().get(0).getSeason() == -1);
    assertThat(show.getEpisodes().get(0).getEpisode() == -1);
  }
}
