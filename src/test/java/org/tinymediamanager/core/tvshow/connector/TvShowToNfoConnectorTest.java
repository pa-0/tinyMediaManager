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
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.BasicTvShowTest;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.core.tvshow.filenaming.TvShowNfoNaming;
import org.tinymediamanager.scraper.entities.MediaCertification;

public class TvShowToNfoConnectorTest extends BasicTvShowTest {

  @Before
  public void setup() throws Exception {
    super.setup();

    TvShowModuleManager.getInstance().startUp();
  }

  @Test
  public void testXbmcNfo() throws Exception {
    Files.createDirectories(getWorkFolder().resolve("xbmc_nfo"));

    try {
      TvShow tvShow = createTvShow("xbmc_nfo");

      // write it
      List<TvShowNfoNaming> nfoNames = Collections.singletonList(TvShowNfoNaming.TV_SHOW);
      TvShowToXbmcConnector connector = new TvShowToXbmcConnector(tvShow);
      connector.write(nfoNames);

      Path nfoFile = getWorkFolder().resolve("xbmc_nfo/tvshow.nfo");
      assertThat(Files.exists(nfoFile)).isTrue();

      // unmarshal it
      TvShowNfoParser tvShowNfoParser = TvShowNfoParser.parseNfo(nfoFile);
      TvShow newTvShow = tvShowNfoParser.toTvShow();
      compareTvShows(tvShow, newTvShow);
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testKodiNfo() throws Exception {
    Files.createDirectories(getWorkFolder().resolve("kodi_nfo"));

    try {
      TvShow tvShow = createTvShow("kodi_nfo");

      // write it
      List<TvShowNfoNaming> nfoNames = Collections.singletonList(TvShowNfoNaming.TV_SHOW);
      TvShowToKodiConnector connector = new TvShowToKodiConnector(tvShow);
      connector.write(nfoNames);

      Path nfoFile = getWorkFolder().resolve("kodi_nfo/tvshow.nfo");
      assertThat(Files.exists(nfoFile)).isTrue();

      // unmarshal it
      TvShowNfoParser tvShowNfoParser = TvShowNfoParser.parseNfo(nfoFile);
      TvShow newTvShow = tvShowNfoParser.toTvShow();
      compareTvShows(tvShow, newTvShow);
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testWriteUnsupportedTags() throws Exception {
    copyResourceFolderToWorkFolder("tvshow_nfo");

    TvShowModuleManager.getInstance().getSettings().setWriteCleanNfo(false);
    Files.createDirectories(getWorkFolder().resolve("kodi_nfo_unsupported"));

    // copy the existing NFO to the target folder
    Path targetNfo = getWorkFolder().resolve("kodi_nfo_unsupported/tvshow.nfo");
    Files.copy(getWorkFolder().resolve("tvshow_nfo/kodi17.0.nfo"), targetNfo);

    TvShowNfoParser parser = TvShowNfoParser.parseNfo(targetNfo);
    TvShow tvShow = parser.toTvShow();
    tvShow.setPath(getWorkFolder().resolve("kodi_nfo_unsupported").toString());
    tvShow.addToMediaFiles(new MediaFile(targetNfo));

    // write it
    List<TvShowNfoNaming> nfoNames = Collections.singletonList(TvShowNfoNaming.TV_SHOW);
    TvShowToKodiConnector connector = new TvShowToKodiConnector(tvShow);
    connector.write(nfoNames);

    Path nfoFile = getWorkFolder().resolve("kodi_nfo_unsupported/tvshow.nfo");
    assertThat(Files.exists(nfoFile)).isTrue();

    // unmarshal it
    TvShowNfoParser tvShowNfoParser = TvShowNfoParser.parseNfo(nfoFile);
    assertThat(tvShowNfoParser.episodeguide).isNotEmpty();
  }

  private void compareTvShows(TvShow tvShow, TvShow newTvShow) {
    assertThat(newTvShow.getTitle()).isEqualTo(tvShow.getTitle());
    assertThat(newTvShow.getSortTitle()).isEqualTo(tvShow.getSortTitle());
    assertThat(newTvShow.getRating().getRating()).isEqualTo(tvShow.getRating().getRating());
    assertThat(newTvShow.getRating().getVotes()).isEqualTo(tvShow.getRating().getVotes());
    assertThat(newTvShow.getYear()).isEqualTo(tvShow.getYear());
    assertThat(newTvShow.getPlot()).isEqualTo(tvShow.getPlot());
    assertThat(newTvShow.getRuntime()).isEqualTo(tvShow.getRuntime());
    assertThat(newTvShow.getArtworkUrl(MediaFileType.POSTER)).isEqualTo(tvShow.getArtworkUrl(MediaFileType.POSTER));
    assertThat(newTvShow.getArtworkUrl(MediaFileType.FANART)).isEqualTo(tvShow.getArtworkUrl(MediaFileType.FANART));

    for (TvShowSeason oldSeason : tvShow.getSeasons()) {
      TvShowSeason newSeason = newTvShow.getSeason(oldSeason.getSeason());

      assertThat(newSeason).isNotNull();
      assertThat(newSeason.getTitle()).isEqualTo(oldSeason.getTitle());
      assertThat(newSeason.getArtworkUrl(MediaFileType.SEASON_POSTER)).isEqualTo(oldSeason.getArtworkUrl(MediaFileType.SEASON_POSTER));
      assertThat(newSeason.getArtworkUrl(MediaFileType.SEASON_BANNER)).isEqualTo(oldSeason.getArtworkUrl(MediaFileType.SEASON_BANNER));
      assertThat(newSeason.getArtworkUrl(MediaFileType.SEASON_THUMB)).isEqualTo(oldSeason.getArtworkUrl(MediaFileType.SEASON_THUMB));
    }

    assertThat(newTvShow.getImdbId()).isEqualTo(tvShow.getImdbId());
    assertThat(newTvShow.getTvdbId()).isEqualTo(tvShow.getTvdbId());
    assertThat(newTvShow.getProductionCompany()).isEqualTo(tvShow.getProductionCompany());
    assertThat(newTvShow.getCertification()).isEqualTo(tvShow.getCertification());
    assertThat(newTvShow.getIds().size()).isEqualTo(tvShow.getIds().size());
    assertThat(newTvShow.getId("trakt")).isEqualTo(tvShow.getId("trakt"));
    assertThat(newTvShow.getFirstAired()).isEqualTo(tvShow.getFirstAired());
    assertThat(newTvShow.getStatus()).isEqualTo(tvShow.getStatus());
    assertThat(newTvShow.getGenres().size()).isEqualTo(tvShow.getGenres().size());
    assertThat(newTvShow.getGenres().get(0)).isEqualTo(tvShow.getGenres().get(0));
    assertThat(newTvShow.getTags().size()).isEqualTo(tvShow.getTags().size());
    if (!newTvShow.getTags().isEmpty()) {
      assertThat(newTvShow.getTags().get(0)).isEqualTo(tvShow.getTags().get(0));
    }
    assertThat(newTvShow.getActors().size()).isEqualTo(tvShow.getActors().size());
    assertThat(newTvShow.getActors().get(0)).isEqualTo(tvShow.getActors().get(0));
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
