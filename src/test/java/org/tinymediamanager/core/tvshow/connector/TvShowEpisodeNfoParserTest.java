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
import static org.assertj.core.api.Assertions.entry;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.tvshow.BasicTvShowTest;
import org.tinymediamanager.scraper.entities.MediaCertification;

public class TvShowEpisodeNfoParserTest extends BasicTvShowTest {

  @Before
  public void setup() throws Exception {
    super.setup();
    copyResourceFolderToWorkFolder("tvshowepisode_nfo");
  }

  @Test
  public void testKodi17_0() {
    // Kodi version 17.0
    try {
      TvShowEpisodeNfoParser parser = TvShowEpisodeNfoParser.parseNfo(getWorkFolder().resolve("tvshowepisode_nfo/kodi17.0.nfo"));

      assertThat(parser).isNotNull();
      assertThat(parser.episodes.size()).isEqualTo(2);

      TvShowEpisodeNfoParser.Episode episode = parser.episodes.get(1);

      assertThat(episode.title).isNotEmpty();
      assertThat(episode.showTitle).isNotEmpty();

      assertThat(episode.season).isEqualTo(1);
      assertThat(episode.episode).isEqualTo(2);
      assertThat(episode.displayseason).isEqualTo(1);
      assertThat(episode.displayepisode).isEqualTo(2);

      assertThat(episode.ratings).hasSize(1);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).id).isEqualTo(TvShowEpisodeNfoParser.Rating.DEFAULT);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).rating).isEqualTo(9f);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).votes).isEqualTo(1);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).maxValue).isEqualTo(10);

      assertThat(episode.year).isEqualTo(1987);
      assertThat(episode.top250).isEqualTo(0);
      assertThat(episode.plot).isNotEmpty();
      assertThat(episode.outline).isEmpty();
      assertThat(episode.tagline).isEmpty();
      assertThat(episode.runtime).isGreaterThan(0);

      assertThat(episode.thumbs).hasSize(1);
      for (String fanart : episode.thumbs) {
        assertThat(fanart).isNotEmpty();
      }
      assertThat(episode.certification).isEqualTo(MediaCertification.US_TVPG);
      assertThat(episode.ids).contains(entry("tvdb", 250800));
      assertThat(episode.releaseDate).isEqualTo("1987-04-12");
      assertThat(episode.watched).isEqualTo(true);
      assertThat(episode.playcount).isEqualTo(1);
      assertThat(episode.genres).contains(MediaGenres.ACTION, MediaGenres.ADVENTURE, MediaGenres.DRAMA);
      assertThat(episode.studios).hasSize(1);
      for (String studio : episode.studios) {
        assertThat(studio).isNotEmpty();
      }
      assertThat(episode.credits).hasSize(1);
      for (TvShowEpisodeNfoParser.Person credit : episode.credits) {
        assertThat(credit.name).isNotEmpty();
      }
      assertThat(episode.directors).hasSize(1);
      for (TvShowEpisodeNfoParser.Person director : episode.directors) {
        assertThat(director.name).isNotEmpty();
      }

      assertThat(episode.actors).hasSize(13);
      assertThat(episode.actors.get(3).thumb).isNotEmpty();
      assertThat(episode.actors.get(3).role).isNotEmpty();
      for (TvShowEpisodeNfoParser.Person actor : episode.actors) {
        assertThat(actor.name).isNotEmpty();
        assertThat(actor.role).isNotNull();
        assertThat(actor.thumb).isNotNull();
      }

      assertThat(episode.unsupportedElements.size()).isEqualTo(1);
      assertThat(episode.unsupportedElements.get(0)).isEqualTo("<resume><position>0.000000</position><total>0.000000</total></resume>");
      assertThat(episode.trailer).isEmpty();

      // xbmc tags
      assertThat(episode.lastplayed).isNull();
      assertThat(episode.dateadded).isNotNull();
      assertThat(episode.fileinfo).isNull();
    }
    catch (Exception e) {
      e.printStackTrace();
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  public void testKodi16_1() {
    // Kodi version 16.1
    try {
      TvShowEpisodeNfoParser parser = TvShowEpisodeNfoParser.parseNfo(getWorkFolder().resolve("tvshowepisode_nfo/kodi16.1.nfo"));

      assertThat(parser).isNotNull();
      assertThat(parser.episodes.size()).isEqualTo(1);

      TvShowEpisodeNfoParser.Episode episode = parser.episodes.get(0);

      assertThat(episode.title).isNotEmpty();
      assertThat(episode.showTitle).isNotEmpty();

      assertThat(episode.season).isEqualTo(1);
      assertThat(episode.episode).isEqualTo(2);
      assertThat(episode.displayseason).isEqualTo(-1);
      assertThat(episode.displayepisode).isEqualTo(-1);

      assertThat(episode.ratings).hasSize(1);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).id).isEqualTo(TvShowEpisodeNfoParser.Rating.DEFAULT);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).rating).isEqualTo(9f);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).votes).isEqualTo(1);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).maxValue).isEqualTo(10);

      assertThat(episode.year).isEqualTo(0);
      assertThat(episode.top250).isEqualTo(0);
      assertThat(episode.plot).isNotEmpty();
      assertThat(episode.outline).isEmpty();
      assertThat(episode.tagline).isEmpty();
      assertThat(episode.runtime).isGreaterThan(0);

      assertThat(episode.thumbs).hasSize(1);
      for (String fanart : episode.thumbs) {
        assertThat(fanart).isNotEmpty();
      }
      assertThat(episode.certification).isEqualTo(MediaCertification.US_TVPG);
      assertThat(episode.ids).contains(entry("tvdb", 250800));
      assertThat(episode.releaseDate).isEqualTo("1987-04-12");
      assertThat(episode.watched).isEqualTo(false);
      assertThat(episode.playcount).isEqualTo(0);
      assertThat(episode.genres).contains(MediaGenres.ACTION, MediaGenres.ADVENTURE, MediaGenres.DRAMA);
      assertThat(episode.studios).hasSize(1);
      for (String studio : episode.studios) {
        assertThat(studio).isNotEmpty();
      }
      assertThat(episode.credits).hasSize(1);
      for (TvShowEpisodeNfoParser.Person credit : episode.credits) {
        assertThat(credit.name).isNotEmpty();
      }
      assertThat(episode.directors).hasSize(1);
      for (TvShowEpisodeNfoParser.Person director : episode.directors) {
        assertThat(director.name).isNotEmpty();
      }

      assertThat(episode.actors).hasSize(13);
      assertThat(episode.actors.get(3).thumb).isNotEmpty();
      assertThat(episode.actors.get(3).role).isNotEmpty();
      for (TvShowEpisodeNfoParser.Person actor : episode.actors) {
        assertThat(actor.name).isNotEmpty();
        assertThat(actor.role).isNotNull();
        assertThat(actor.thumb).isNotNull();
      }

      assertThat(episode.unsupportedElements.size()).isEqualTo(1);
      assertThat(episode.unsupportedElements.get(0)).isEqualTo("<resume><position>0.000000</position><total>0.000000</total></resume>");
      assertThat(episode.trailer).isEmpty();

      // xbmc tags
      assertThat(episode.lastplayed).isNull();
      assertThat(episode.dateadded).isNotNull();
      assertThat(episode.fileinfo).isNull();
    }
    catch (Exception e) {
      e.printStackTrace();
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  public void testKodi15_2() {
    // Kodi version 15.2
    try {
      TvShowEpisodeNfoParser parser = TvShowEpisodeNfoParser.parseNfo(getWorkFolder().resolve("tvshowepisode_nfo/kodi15.2.nfo"));

      assertThat(parser).isNotNull();
      assertThat(parser.episodes.size()).isEqualTo(1);

      TvShowEpisodeNfoParser.Episode episode = parser.episodes.get(0);

      assertThat(episode.title).isNotEmpty();
      assertThat(episode.showTitle).isNotEmpty();

      assertThat(episode.season).isEqualTo(1);
      assertThat(episode.episode).isEqualTo(2);
      assertThat(episode.displayseason).isEqualTo(-1);
      assertThat(episode.displayepisode).isEqualTo(-1);

      assertThat(episode.ratings).hasSize(1);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).id).isEqualTo(TvShowEpisodeNfoParser.Rating.DEFAULT);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).rating).isEqualTo(9f);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).votes).isEqualTo(1);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).maxValue).isEqualTo(10);

      assertThat(episode.year).isEqualTo(0);
      assertThat(episode.top250).isEqualTo(0);
      assertThat(episode.plot).isNotEmpty();
      assertThat(episode.outline).isEmpty();
      assertThat(episode.tagline).isEmpty();
      assertThat(episode.runtime).isGreaterThan(0);

      assertThat(episode.thumbs).hasSize(1);
      for (String fanart : episode.thumbs) {
        assertThat(fanart).isNotEmpty();
      }
      assertThat(episode.certification).isEqualTo(MediaCertification.US_TVPG);
      assertThat(episode.ids).contains(entry("tvdb", 250800));
      assertThat(episode.releaseDate).isEqualTo("1987-04-12");
      assertThat(episode.watched).isEqualTo(false);
      assertThat(episode.playcount).isEqualTo(0);
      assertThat(episode.genres).isEmpty();
      assertThat(episode.studios).hasSize(1);
      for (String studio : episode.studios) {
        assertThat(studio).isNotEmpty();
      }
      assertThat(episode.credits).hasSize(1);
      for (TvShowEpisodeNfoParser.Person credit : episode.credits) {
        assertThat(credit.name).isNotEmpty();
      }
      assertThat(episode.directors).hasSize(1);
      for (TvShowEpisodeNfoParser.Person director : episode.directors) {
        assertThat(director.name).isNotEmpty();
      }

      assertThat(episode.actors).hasSize(13);
      assertThat(episode.actors.get(3).thumb).isNotEmpty();
      assertThat(episode.actors.get(3).role).isNotEmpty();
      for (TvShowEpisodeNfoParser.Person actor : episode.actors) {
        assertThat(actor.name).isNotEmpty();
        assertThat(actor.role).isNotNull();
        assertThat(actor.thumb).isNotNull();
      }

      assertThat(episode.unsupportedElements.size()).isEqualTo(1);
      assertThat(episode.unsupportedElements.get(0)).isEqualTo("<resume><position>0.000000</position><total>0.000000</total></resume>");
      assertThat(episode.trailer).isEmpty();

      // xbmc tags
      assertThat(episode.lastplayed).isNull();
      assertThat(episode.dateadded).isNotNull();
      assertThat(episode.fileinfo).isNull();
    }
    catch (Exception e) {
      e.printStackTrace();
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  public void testKodi14_2() {
    // Kodi version 14.2
    try {
      TvShowEpisodeNfoParser parser = TvShowEpisodeNfoParser.parseNfo(getWorkFolder().resolve("tvshowepisode_nfo/kodi14.2.nfo"));

      assertThat(parser).isNotNull();
      assertThat(parser.episodes.size()).isEqualTo(2);

      TvShowEpisodeNfoParser.Episode episode = parser.episodes.get(0);

      assertThat(episode.title).isNotEmpty();
      assertThat(episode.showTitle).isNotEmpty();

      assertThat(episode.season).isEqualTo(1);
      assertThat(episode.episode).isEqualTo(1);
      assertThat(episode.displayseason).isEqualTo(-1);
      assertThat(episode.displayepisode).isEqualTo(-1);

      assertThat(episode.ratings).hasSize(1);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).id).isEqualTo(TvShowEpisodeNfoParser.Rating.DEFAULT);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).rating).isEqualTo(9f);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).votes).isEqualTo(1);
      assertThat(episode.ratings.get(TvShowEpisodeNfoParser.Rating.DEFAULT).maxValue).isEqualTo(10);

      assertThat(episode.year).isEqualTo(0);
      assertThat(episode.top250).isEqualTo(0);
      assertThat(episode.plot).isNotEmpty();
      assertThat(episode.outline).isEmpty();
      assertThat(episode.tagline).isEmpty();
      assertThat(episode.runtime).isGreaterThan(0);

      assertThat(episode.thumbs).hasSize(1);
      for (String fanart : episode.thumbs) {
        assertThat(fanart).isNotEmpty();
      }
      assertThat(episode.certification).isEqualTo(MediaCertification.US_TVPG);
      assertThat(episode.ids).contains(entry("tvdb", 250800));
      assertThat(episode.releaseDate).isEqualTo("1987-04-12");
      assertThat(episode.watched).isEqualTo(false);
      assertThat(episode.playcount).isEqualTo(0);
      assertThat(episode.genres).isEmpty();
      assertThat(episode.studios).hasSize(1);
      for (String studio : episode.studios) {
        assertThat(studio).isNotEmpty();
      }
      assertThat(episode.credits).hasSize(1);
      for (TvShowEpisodeNfoParser.Person credit : episode.credits) {
        assertThat(credit.name).isNotEmpty();
      }
      assertThat(episode.directors).hasSize(1);
      for (TvShowEpisodeNfoParser.Person director : episode.directors) {
        assertThat(director.name).isNotEmpty();
      }

      assertThat(episode.actors).hasSize(13);
      assertThat(episode.actors.get(3).thumb).isNotEmpty();
      assertThat(episode.actors.get(3).role).isNotEmpty();
      for (TvShowEpisodeNfoParser.Person actor : episode.actors) {
        assertThat(actor.name).isNotEmpty();
        assertThat(actor.role).isNotNull();
        assertThat(actor.thumb).isNotNull();
      }

      assertThat(episode.unsupportedElements.size()).isEqualTo(1);
      assertThat(episode.unsupportedElements.get(0)).isEqualTo("<resume><position>0.000000</position><total>0.000000</total></resume>");
      assertThat(episode.trailer).isEmpty();

      // xbmc tags
      assertThat(episode.lastplayed).isNull();
      assertThat(episode.dateadded).isNotNull();
      assertThat(episode.fileinfo).isNull();
    }
    catch (Exception e) {
      e.printStackTrace();
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  public void testNetpvrXml() throws Exception {
    TvShowEpisodeNfoParser parser = TvShowEpisodeNfoParser.parseNfo(getWorkFolder().resolve("tvshowepisode_nfo/nextpvr.xml"));

    List<TvShowEpisodeNfoParser.Episode> episodes = parser.episodes;
    assertThat(episodes.size()).isEqualTo(1);

    TvShowEpisodeNfoParser.Episode episode = episodes.get(0);

    assertThat(episode.title).isEqualTo("Hartes Training");
    assertThat(episode.plot).startsWith("Aang versucht, das Erdbändigen von Toph zu lernen, aber");
    assertThat(episode.certification).isEqualTo(MediaCertification.DE_FSK6);
    assertThat(episode.releaseDate).isEqualTo("2005-01-01");
    assertThat(episode.genres.size()).isEqualTo(2);
  }

  @Test
  public void testNetpvrXml2() throws Exception {
    TvShowEpisodeNfoParser parser = TvShowEpisodeNfoParser.parseNfo(getWorkFolder().resolve("tvshowepisode_nfo/nextpvr2.xml"));

    List<TvShowEpisodeNfoParser.Episode> episodes = parser.episodes;
    assertThat(episodes.size()).isEqualTo(1);

    TvShowEpisodeNfoParser.Episode episode = episodes.get(0);

    assertThat(episode.title).isEqualTo("Die Unwissenden");
    assertThat(episode.plot).startsWith("Josh kämpft darum, Nora zurückzugewinnen.");
    assertThat(episode.certification).isEqualTo(MediaCertification.DE_FSK12);
    assertThat(episode.releaseDate).isEqualTo("2014-10-17");
    assertThat(episode.genres.size()).isEqualTo(2);
  }

  @Test
  public void testNetpvrXml3() throws Exception {
    TvShowEpisodeNfoParser parser = TvShowEpisodeNfoParser.parseNfo(getWorkFolder().resolve("tvshowepisode_nfo/nextpvr3.xml"));

    List<TvShowEpisodeNfoParser.Episode> episodes = parser.episodes;
    assertThat(episodes.size()).isEqualTo(1);

    TvShowEpisodeNfoParser.Episode episode = episodes.get(0);

    assertThat(episode.title).isEqualTo("Die Wissenschafts-Meemaw und der Skandal im Kopierraum");
    assertThat(episode.plot).isEqualTo(episode.title);
    assertThat(episode.certification).isEqualTo(MediaCertification.UNKNOWN);
    assertThat(episode.releaseDate).isNull();
    assertThat(episode.genres.size()).isEqualTo(3);
  }
}
