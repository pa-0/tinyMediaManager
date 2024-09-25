/*
 * Copyright 2012 - 2024 Manuel Laggner
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.BasicTvShowTest;
import org.tinymediamanager.core.tvshow.TvShowEpisodeEdition;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.core.tvshow.filenaming.TvShowEpisodeNfoNaming;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;

public class TvShowEpisodeToNfoConnectorTest extends BasicTvShowTest {

  @Before
  public void setup() throws Exception {
    super.setup();
    TvShowModuleManager.getInstance().startUp();
  }

  @Test
  public void testXbmcNfo() throws Exception {
    Files.createDirectories(getWorkFolder().resolve("xbmc_nfo"));

    TvShow tvShow = createTvShow("xbmc_nfo");
    List<TvShowEpisode> episodes = createEpisodes(tvShow, true);

    // write it
    List<TvShowEpisodeNfoNaming> nfoNames = Collections.singletonList(TvShowEpisodeNfoNaming.FILENAME);
    TvShowEpisodeToXbmcConnector connector = new TvShowEpisodeToXbmcConnector(episodes);
    connector.write(nfoNames);

    Path nfoFile = getWorkFolder().resolve("xbmc_nfo/S01E01E02.nfo");
    assertThat(Files.exists(nfoFile)).isTrue();

    // unmarshal it
    TvShowEpisodeNfoParser tvShowEpisodeNfoParser = TvShowEpisodeNfoParser.parseNfo(nfoFile);
    List<TvShowEpisode> newEpisodes = tvShowEpisodeNfoParser.toTvShowEpisodes();
    for (TvShowEpisode episode : newEpisodes) {
      episode.setTvShow(tvShow);
    }
    compareEpisodes(episodes, newEpisodes);
  }

  @Test
  public void testKodiNfo() throws Exception {
    Files.createDirectories(getWorkFolder().resolve("kodi_nfo"));

    TvShow tvShow = createTvShow("kodi_nfo");
    List<TvShowEpisode> episodes = createEpisodes(tvShow, true);

    // write it
    List<TvShowEpisodeNfoNaming> nfoNames = Collections.singletonList(TvShowEpisodeNfoNaming.FILENAME);
    TvShowEpisodeToXbmcConnector connector = new TvShowEpisodeToXbmcConnector(episodes);
    connector.write(nfoNames);

    Path nfoFile = getWorkFolder().resolve("kodi_nfo/S01E01E02.nfo");
    assertThat(Files.exists(nfoFile)).isTrue();

    // unmarshal it
    TvShowEpisodeNfoParser tvShowEpisodeNfoParser = TvShowEpisodeNfoParser.parseNfo(nfoFile);
    List<TvShowEpisode> newEpisodes = tvShowEpisodeNfoParser.toTvShowEpisodes();
    for (TvShowEpisode episode : newEpisodes) {
      episode.setTvShow(tvShow);
    }
    compareEpisodes(episodes, newEpisodes);
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

    TvShowSeason tvShowSeason = tvShow.getOrCreateSeason(1);
    tvShowSeason.setArtworkUrl("http://season1", MediaFileType.SEASON_POSTER);
    tvShowSeason.setArtworkUrl("http://season-banner1", MediaFileType.SEASON_BANNER);
    tvShowSeason.setArtworkUrl("http://season-thumb1", MediaFileType.SEASON_THUMB);

    tvShowSeason = tvShow.getOrCreateSeason(2);
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

  private void compareEpisodes(List<TvShowEpisode> episodes, List<TvShowEpisode> newEpisodes) {
    assertThat(episodes.size()).isEqualTo(newEpisodes.size());

    for (int i = 0; i < episodes.size(); i++) {
      TvShowEpisode episode = episodes.get(i);
      TvShowEpisode newEpisode = newEpisodes.get(i);

      assertThat(newEpisode.getTitle()).isEqualTo(episode.getTitle());
      assertThat(newEpisode.getSeason()).isEqualTo(episode.getSeason());
      assertThat(newEpisode.getEpisode()).isEqualTo(episode.getEpisode());
      assertThat(newEpisode.getDisplaySeason()).isEqualTo(episode.getDisplaySeason());
      assertThat(newEpisode.getDisplayEpisode()).isEqualTo(episode.getDisplayEpisode());
      assertThat(newEpisode.getIds()).isEqualTo(episode.getIds());
      assertThat(newEpisode.getPlot()).isEqualTo(episode.getPlot());
      assertThat(newEpisode.getRating().getRating()).isEqualTo(episode.getRating().getRating());
      assertThat(newEpisode.getRating().getVotes()).isEqualTo(episode.getRating().getVotes());
      assertThat(newEpisode.getArtworkUrl(MediaFileType.THUMB)).isEqualTo(episode.getArtworkUrl(MediaFileType.THUMB));
      assertThat(newEpisode.isWatched()).isEqualTo(episode.isWatched());
      assertThat(newEpisode.getFirstAired()).isEqualTo(episode.getFirstAired());
      assertThat(newEpisode.getMediaSource()).isEqualTo(episode.getMediaSource());
      assertThat(newEpisode.getEdition()).isEqualTo(episode.getEdition());
      assertThat(newEpisode.getTags()).isEqualTo(episode.getTags());

      // since we do not write show actors to the episodes, we need to adopt this test
      for (Person person : newEpisode.getActors()) {
        assertThat(episode.getActors()).contains(person);
        assertThat(newEpisode.getTvShow().getActors()).doesNotContain(person);
      }

      assertThat(newEpisode.getDirectors().size()).isEqualTo(episode.getDirectors().size());
      assertThat(newEpisode.getDirectors().get(0)).isEqualTo(episode.getDirectors().get(0));
      assertThat(newEpisode.getWriters().size()).isEqualTo(episode.getWriters().size());
      assertThat(newEpisode.getWriters().get(0)).isEqualTo(episode.getWriters().get(0));
    }
  }

  private List<TvShowEpisode> createEpisodes(TvShow tvShow, boolean multiEp) throws Exception {
    List<TvShowEpisode> episodes = new ArrayList<>();

    TvShowEpisode episode1 = new TvShowEpisode();
    episode1.setTvShow(tvShow);
    episode1.setPath(tvShow.getPathNIO().toString());
    episode1.setTitle("Pilot (1)");
    episode1.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 1));
    episode1.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_DISPLAY, 1, 1));
    episode1.setId(MediaMetadata.TVDB, 1234);
    episode1.setPlot(
        "Hanson gets assigned to the Jump Street unit, a special division of the police force which uses young cops to go undercover and stop juvenile crime, when his youthful appearance causes him to be underestimated while on patrol. His first case involves catching drug dealers.");
    episode1.setRating(new MediaRating(MediaRating.NFO, 9.0f, 8));
    episode1.setArtworkUrl("http://thumb1", MediaFileType.THUMB);
    episode1.setWatched(true);

    episode1.setMediaSource(MediaSource.BLURAY);
    episode1.setEdition(TvShowEpisodeEdition.DIRECTORS_CUT);

    episode1.addToTags(Collections.singletonList("Pilot"));

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    episode1.setFirstAired(sdf.parse("1987-04-12"));

    episode1.addToDirectors(Collections.singletonList(new Person(Person.Type.DIRECTOR, "Kim Manners", "Director")));
    episode1.addToWriters(Collections.singletonList(new Person(Person.Type.WRITER, "Patrick Hasburgh", "Writer")));

    episode1.addToActors(Arrays.asList(new Person(Person.Type.ACTOR, "Charles Payne", "Unknown", "http://thumb1"),
        new Person(Person.Type.ACTOR, "Reginald T. Dorsey", "", "http://thumb2")));

    MediaFile mf = new MediaFile();
    mf.setType(MediaFileType.VIDEO);
    mf.setPath(getWorkFolder().resolve(tvShow.getTitle()).toString());
    mf.setFilename("S01E01E02.mkv");
    mf.setVideoCodec("h264");
    mf.setVideoHeight(720);
    mf.setVideoWidth(1280);
    mf.setDuration(5403);
    mf.setVideo3DFormat(MediaFileHelper.VIDEO_3D_SBS);

    MediaFileAudioStream audio = new MediaFileAudioStream();
    audio.setCodec("AC3");
    audio.setLanguage("en");
    audio.setAudioChannels(6);
    mf.setAudioStreams(Collections.singletonList(audio));

    MediaFileSubtitle sub = new MediaFileSubtitle();
    sub.setLanguage("de");
    mf.addSubtitle(sub);

    episode1.addToMediaFiles(mf);

    episodes.add(episode1);

    if (multiEp) {
      TvShowEpisode episode2 = new TvShowEpisode();
      episode2.setTvShow(tvShow);
      episode2.setTitle("Pilot (2)");
      episode2.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 2));
      episode2.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_DISPLAY, 1, 2));
      episode2.setId(MediaMetadata.TVDB, 2345);
      episode2.setPlot(
          "Hanson gets assigned to the Jump Street unit, a special division of the police force which uses young cops to go undercover and stop juvenile crime, when his youthful appearance causes him to be underestimated while on patrol. His first case involves catching drug dealers.");
      episode2.setRating(new MediaRating(MediaRating.NFO, 8.0f, 10));
      episode2.setArtworkUrl("http://thumb1", MediaFileType.THUMB);
      episode2.setWatched(false);

      sdf = new SimpleDateFormat("yyyy-MM-dd");
      episode2.setFirstAired(sdf.parse("1987-04-19"));

      episode2.addToDirectors(Arrays.asList(new Person(Person.Type.DIRECTOR, "Kim Manners", "Director")));
      episode2.addToWriters(Arrays.asList(new Person(Person.Type.WRITER, "Patrick Hasburgh", "Writer")));

      episode2.addToActors(Arrays.asList(new Person(Person.Type.ACTOR, "Charles Payne", "Unknown", "http://thumb1"),
          new Person(Person.Type.ACTOR, "Reginald T. Dorsey", "", "http://thumb2")));

      mf = new MediaFile();
      mf.setType(MediaFileType.VIDEO);
      mf.setPath(getWorkFolder().resolve(tvShow.getTitle()).toString());
      mf.setFilename("S01E01E02.mkv");
      mf.setVideoCodec("h264");
      mf.setVideoHeight(720);
      mf.setVideoWidth(1280);
      mf.setDuration(5403);
      mf.setVideo3DFormat(MediaFileHelper.VIDEO_3D_SBS);

      audio = new MediaFileAudioStream();
      audio.setCodec("AC3");
      audio.setLanguage("en");
      audio.setAudioChannels(6);
      mf.setAudioStreams(Collections.singletonList(audio));

      sub = new MediaFileSubtitle();
      sub.setLanguage("de");
      mf.addSubtitle(sub);

      episode2.addToMediaFiles(mf);

      episodes.add(episode2);
    }

    return episodes;
  }
}
