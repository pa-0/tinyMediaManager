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

package org.tinymediamanager.core.tvshow.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.BasicTvShowTest;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonNfoNaming;
import org.tinymediamanager.scraper.entities.MediaCertification;

public class TvShowSeasonToNfoConnectorTest extends BasicTvShowTest {

  @Before
  public void setup() throws Exception {
    super.setup();

    TvShowModuleManager.getInstance().startUp();
  }

  @Test
  public void testEmbyNfo() throws Exception {
    Files.createDirectories(getWorkFolder().resolve("emby_nfo"));

    try {
      TvShow tvShow = createTvShow("emby_nfo");

      TvShowSeason tvShowSeason = tvShow.getOrCreateSeason(1);
      tvShowSeason.setTitle("Season one - the first season");
      tvShowSeason.setPlot("This is the plot for the first season");
      tvShowSeason.setNote("Just a note");

      // write it
      List<TvShowSeasonNfoNaming> nfoNames = Collections.singletonList(TvShowSeasonNfoNaming.SEASON);
      TvShowSeasonToEmbyConnector connector = new TvShowSeasonToEmbyConnector(tvShowSeason);
      connector.write(nfoNames);

      Path nfoFile = getWorkFolder().resolve("emby_nfo/season01.nfo");
      assertThat(Files.exists(nfoFile)).isTrue();

      // unmarshal it
      TvShowSeasonNfoParser tvShowSeasonNfoParser = TvShowSeasonNfoParser.parseNfo(nfoFile);
      TvShowSeason newTvShowSeason = tvShowSeasonNfoParser.toTvShowSeason();
      compareTvShowSeasons(tvShowSeason, newTvShowSeason);
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  private void compareTvShowSeasons(TvShowSeason tvShowSeason, TvShowSeason newTvShowSeason) {
    assertThat(newTvShowSeason.getTitle()).isEqualTo(tvShowSeason.getTitle());
    assertThat(newTvShowSeason.getPlot()).isEqualTo(tvShowSeason.getPlot());
    assertThat(newTvShowSeason.getNote()).isEqualTo(tvShowSeason.getNote());
  }

  private TvShow createTvShow(String path) throws Exception {
    TvShow tvShow = new TvShow();
    tvShow.setPath(getWorkFolder().resolve(path).toString());
    tvShow.setTitle("21 Jump Street");
    tvShow.setRating(new MediaRating(MediaRating.NFO, 9.0f, 8));
    tvShow.setYear(1987);
    tvShow.setPlot(
        "21 Jump Street was a FOX action/drama series that ran for five seasons (1987-1991). The show revolved around a group of young cops who would use their youthful appearance to go undercover and solve crimes involving teenagers and young adults. 21 Jump Street propelled Johnny Depp to stardom and was the basis for a 2012 comedy/action film of the same name.");
    tvShow.setRuntime(45);
    tvShow.setArtworkUrl("http://poster", MediaFileType.POSTER);
    tvShow.setArtworkUrl("http://fanart", MediaFileType.FANART);
    tvShow.setArtworkUrl("http://banner", MediaFileType.BANNER);
    tvShow.setArtworkUrl("http://clearart", MediaFileType.CLEARART);
    tvShow.setArtworkUrl("http://clearlogo", MediaFileType.CLEARLOGO);
    tvShow.setArtworkUrl("http://discart", MediaFileType.DISC);
    tvShow.setArtworkUrl("http://keyart", MediaFileType.KEYART);
    tvShow.setArtworkUrl("http://thumb", MediaFileType.THUMB);
    tvShow.setArtworkUrl("http://logo", MediaFileType.LOGO);
    tvShow.setArtworkUrl("http://characterart", MediaFileType.CHARACTERART);

    TvShowSeason tvShowSeason = tvShow.getOrCreateSeason(1);
    tvShowSeason.setTitle("First Season");
    tvShowSeason.setArtworkUrl("http://season1", MediaFileType.SEASON_POSTER);
    tvShowSeason.setArtworkUrl("http://season-banner1", MediaFileType.SEASON_BANNER);
    tvShowSeason.setArtworkUrl("http://season-thumb1", MediaFileType.SEASON_THUMB);

    tvShowSeason = tvShow.getOrCreateSeason(2);
    tvShowSeason.setTitle("Second Season");
    tvShowSeason.setArtworkUrl("http://season2", MediaFileType.SEASON_POSTER);
    tvShowSeason.setArtworkUrl("http://season-banner2", MediaFileType.SEASON_BANNER);
    tvShowSeason.setArtworkUrl("http://season-thumb2", MediaFileType.SEASON_THUMB);

    tvShow.setImdbId("tt0103639");
    tvShow.setTvdbId("812");
    tvShow.setId("trakt", 655);
    tvShow.setProductionCompany("FOX (US)");
    tvShow.setCertification(MediaCertification.US_TVPG);
    tvShow.setStatus(MediaAiredStatus.ENDED);

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    tvShow.setFirstAired(sdf.parse("1987-04-12"));

    tvShow.addToGenres(Arrays.asList(MediaGenres.ACTION, MediaGenres.ADVENTURE, MediaGenres.DRAMA));

    tvShow.addToTags(Collections.singletonList("80s"));

    tvShow.addToActors(Arrays.asList(new Person(Person.Type.ACTOR, "Johnny Depp", "Officer Tom Hanson", "http://thumb1"),
        new Person(Person.Type.ACTOR, "Holly Robinson Peete", "Officer Judy Hoffs", "http://thumb2")));

    return tvShow;
  }
}
