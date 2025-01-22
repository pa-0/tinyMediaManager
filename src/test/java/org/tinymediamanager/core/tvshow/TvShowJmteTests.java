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

package org.tinymediamanager.core.tvshow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tinymediamanager.core.jmte.JmteUtils.morphTemplate;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.tinymediamanager.core.ExportTemplate;
import org.tinymediamanager.core.MediaEntityExporter;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.jmte.JmteUtils;
import org.tinymediamanager.core.jmte.NamedArrayRenderer;
import org.tinymediamanager.core.jmte.NamedDateRenderer;
import org.tinymediamanager.core.jmte.NamedNumberRenderer;
import org.tinymediamanager.core.jmte.NamedUpperCaseRenderer;
import org.tinymediamanager.core.jmte.TmmModelAdaptor;
import org.tinymediamanager.core.jmte.TmmOutputAppender;
import org.tinymediamanager.core.jmte.ZeroNumberRenderer;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;

import com.floreysoft.jmte.Engine;

public class TvShowJmteTests extends BasicTvShowTest {
  private Engine              engine;
  private Map<String, Object> root;

  @Test
  public void testTvshowPatterns() {
    try {
      TvShow tvShow = createTvShow();

      engine = Engine.createEngine();
      engine.registerRenderer(Number.class, new ZeroNumberRenderer());
      engine.registerNamedRenderer(new NamedDateRenderer());
      engine.registerNamedRenderer(new NamedNumberRenderer());
      engine.registerNamedRenderer(new NamedUpperCaseRenderer());
      engine.registerNamedRenderer(new TvShowRenamer.TvShowNamedFirstCharacterRenderer());
      engine.registerNamedRenderer(new NamedArrayRenderer());

      engine.setModelAdaptor(new TmmModelAdaptor());
      engine.setOutputAppender(new TmmOutputAppender() {
        @Override
        protected String replaceInvalidCharacters(String text) {
          return TvShowRenamer.replaceInvalidCharacters(text);
        }
      });

      root = new HashMap<>();
      root.put("tvShow", tvShow);

      // test single tokens
      compare("${showTitle}", "The 4400");
      compare("${showTitleSortable}", "4400, The");
      compare("${showYear}", "1987");

      // test combined tokens
      compare("${showTitle} (${showYear})", "The 4400 (1987)");

      // test empty brackets
      compare("{ ${showTitle[100]} }", "{ The 4400 }");

      // direct access
      compare("${tvShow.year}/${tvShow.title}", "1987/The 4400");
      compare("${tvShow.year}/${showTitle[0,2]}", "1987/Th");

      // test parent and space separator expressions
      compare("${parent}", "#" + File.separator + "1987");
      compare("${tvShow.productionCompany}", "FOX (US) HBO");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  public void testEpisodePatterns() {
    try {
      TvShowEpisode episode = createEpisode();

      engine = Engine.createEngine();
      engine.registerRenderer(Number.class, new ZeroNumberRenderer());
      engine.registerNamedRenderer(new NamedDateRenderer());
      engine.registerNamedRenderer(new NamedNumberRenderer());
      engine.registerNamedRenderer(new NamedUpperCaseRenderer());
      engine.registerNamedRenderer(new TvShowRenamer.TvShowNamedFirstCharacterRenderer());
      engine.registerNamedRenderer(new NamedArrayRenderer());

      engine.setModelAdaptor(new TmmModelAdaptor());
      engine.setOutputAppender(new TmmOutputAppender() {
        @Override
        protected String replaceInvalidCharacters(String text) {
          return TvShowRenamer.replaceInvalidCharacters(text);
        }
      });

      root = new HashMap<>();
      root.put("episode", episode);
      root.put("tvShow", episode.getTvShow());

      // test single tokens from the TV show
      compare("${showTitle}", "The 4400");
      compare("${showTitleSortable}", "4400, The");
      compare("${showYear}", "1987");

      // test single tokens from the episode
      compare("${episodeNr}", "3");
      compare("${episodeNr2}", "03");
      compare("${episodeNrDvd}", "5");
      compare("${seasonNr}", "1");
      compare("${seasonNr2}", "01");
      compare("${seasonNrDvd}", "1");
      compare("${title}", "Don't Pet the Teacher");
      compare("${year}", "1987");
      compare("${airedDate}", "1987-04-26");

      compare("${videoResolution}", "1280x720");
      compare("${videoFormat}", "720p");
      compare("${videoCodec}", "h264");
      compare("${audioCodec}", "AC3");
      compare("${audioCodecList[1]}", "MP3");
      compare("${audioCodecList[2]}", "");
      compare("${audioChannels}", "6ch");
      compare("${audioChannelsDot}", "5.1");
      compare("${audioChannelList[1]}", "2ch");
      compare("${audioChannelList[2]}", "");
      compare("${audioLanguage}", "en");
      compare("${audioLanguageList[1]}", "de");
      compare("${audioLanguageList[1];upper}", "DE");
      compare("${audioLanguageList[2]}", "");

      compare("${mediaSource}", "Blu-ray");
      compare("${mediaSource.name}", "BLURAY");

      // test combined tokens
      compare("${showTitle} - S${seasonNr2}E${episodeNr2} - ${title}", "The 4400 - S01E03 - Don't Pet the Teacher");

      // test empty brackets
      compare("{ ${showTitle[100]} }", "{ The 4400 }");

      // test direct access
      compare("${episode.firstAired;date(yyyy - MM - dd)} - ${episode.title}", "1987 - 04 - 26 - Don't Pet the Teacher");
      compare("S${episode.season}E${episodeNr} - ${title[0,2]}", "S1E3 - Do");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  public void testAllTvShowTemplates() throws Exception {
    createFakeShow("Best Show");
    createFakeShow("THE show");
    createFakeShow("Show 3");

    TvShowList tv = TvShowModuleManager.getInstance().getTvShowList();
    for (ExportTemplate t : TvShowExporter.findTemplates(MediaEntityExporter.TemplateType.TV_SHOW)) {
      System.out.println("\nTEMPLATE: " + t.getPath());
      TvShowExporter ex = new TvShowExporter(Paths.get(t.getPath()));
      ex.export(tv.getTvShows(), getWorkFolder().resolve(t.getName()));
    }
  }

  @Test
  public void testTvShowReplacements() throws Exception {
    Map<String, String> tokenMap = TvShowRenamer.getTokenMap();

    assertThat(morphTemplate("${episode.title}", tokenMap)).isEqualTo("${episode.title}");
    assertThat(morphTemplate("${title}", tokenMap)).isEqualTo("${episode.title}");
    assertThat(morphTemplate("${title[2]}", tokenMap)).isEqualTo("${episode.title[2]}");
    assertThat(morphTemplate("${episode.title[2]}", tokenMap)).isEqualTo("${episode.title[2]}");
    assertThat(morphTemplate("${if title}${title[2]}${end}", tokenMap)).isEqualTo("${if episode.title}${episode.title[2]}${end}");
    assertThat(morphTemplate("${foreach audioCodecList codec}${codec}${end}", tokenMap))
        .isEqualTo("${foreach episode.mediaInfoAudioCodecList codec}${codec}${end}");
    assertThat(morphTemplate("${title;lower}", tokenMap)).isEqualTo("${episode.title;lower}");
  }

  private void compare(String template, String expectedValue) {
    String actualValue = engine.transform(JmteUtils.morphTemplate(template, TvShowRenamer.getTokenMap()), root);
    assertThat(actualValue).isEqualTo(expectedValue);
  }

  @Test
  public void testMultiEpisodePatterns() {
    try {
      // create two episodes with the same file
      TvShow tvShow = createTvShow();
      TvShowEpisode episode1 = createEpisode();
      tvShow.addEpisode(episode1);
      TvShowEpisode episode2 = createEpisode();
      episode2.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 4));
      episode2.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_DVD, 1, 6));
      episode2.setTitle("Part 2");
      episode2.setFirstAired("1987-04-27");
      tvShow.addEpisode(episode2);

      List<TvShowEpisode> episodes = Arrays.asList(episode1, episode2);

      // test single tokens from the TV show
      compare2("${showTitle}", "The 4400", episodes);
      compare2("${showTitleSortable}", "4400, The", episodes);
      compare2("${showYear}", "1987", episodes);

      // test single tokens from the episode
      compare2("${episodeNr}", "3 4", episodes);
      compare2("${episode.episode}", "3 4", episodes);
      compare2("${episodeNr2}", "03 04", episodes);
      compare2("${episodeNrDvd}", "5 6", episodes);
      compare2("${episode.dvdEpisode}", "5 6", episodes);
      compare2("${seasonNr}", "1 1", episodes);
      compare2("${episode.season}", "1 1", episodes);
      compare2("${seasonNr2}", "01 01", episodes);
      compare2("${seasonNrDvd}", "1 1", episodes);
      compare2("${episode.dvdSeason}", "1 1", episodes);
      compare2("${title}", "Don't Pet the Teacher - Part 2", episodes);
      compare2("${episode.title}", "Don't Pet the Teacher - Part 2", episodes);
      compare2("${year}", "1987", episodes);
      compare2("${airedDate}", "1987-04-26 - 1987-04-27", episodes);
      compare2("${episode.firstAired;date(yyyy-MM-dd)}", "1987-04-26 - 1987-04-27", episodes);

      compare2("${videoResolution}", "1280x720", episodes);
      compare2("${videoFormat}", "720p", episodes);
      compare2("${videoCodec}", "h264", episodes);
      compare2("${audioCodec}", "AC3", episodes);
      compare2("${audioCodecList[1]}", "MP3", episodes);
      compare2("${audioCodecList[2]}", "", episodes);
      compare2("${audioChannels}", "6ch", episodes);
      compare2("${audioChannelList[1]}", "2ch", episodes);
      compare2("${audioChannelList[2]}", "", episodes);
      compare2("${audioLanguage}", "en", episodes);
      compare2("${audioLanguageList[1]}", "de", episodes);
      compare2("${audioLanguageList[1];upper}", "DE", episodes);
      compare2("${audioLanguageList[2]}", "", episodes);

      compare2("${mediaSource}", "Blu-ray", episodes);
      compare2("${mediaSource.name}", "BLURAY", episodes);

      // test combined tokens
      compare2("${showTitle} - S${seasonNr2}E${episodeNr2} - ${title}", "The 4400 - S01E03 S01E04 - Don't Pet the Teacher - Part 2", episodes);

      // test empty brackets
      compare2("{ ${showTitle[100]} }", "{ The 4400 }", episodes);

      // test direct access
      compare2("${episode.firstAired;date(yyyy - MM - dd)} - ${episode.title}", "1987 - 04 - 26 - 1987 - 04 - 27 - Don't Pet the Teacher - Part 2",
          episodes);
      compare2("S${episode.season}E${episodeNr} - ${title[0,2]}", "S1E3 S1E4 - Do - Pa", episodes);
    }
    catch (Exception e) {
      e.printStackTrace();
      Assertions.fail(e.getMessage());
    }
  }

  private void compare2(String template, String expectedValue, List<TvShowEpisode> episodes) {
    String actualValue = TvShowRenamer.createDestination(template, episodes);
    assertThat(actualValue).isEqualTo(expectedValue);
  }

  private TvShow createTvShow() throws Exception {
    TvShow tvShow = new TvShow();
    tvShow.setDataSource("/media/tvshows/");
    tvShow.setPath("/media/tvshows/#/1987/21 Jump Street");
    tvShow.setTitle("The 4400");
    tvShow.setYear(1987);
    tvShow.setRating(new MediaRating(MediaRating.NFO, 7.4f, 8));
    tvShow.setCertification(MediaCertification.US_TVPG);
    tvShow.setGenres(Arrays.asList(MediaGenres.ACTION, MediaGenres.ADVENTURE, MediaGenres.DRAMA));
    tvShow.setTvdbId("77585");
    tvShow.setFirstAired("1987-04-12");
    tvShow.setProductionCompany("FOX (US)/HBO");
    return tvShow;
  }

  private TvShowEpisode createEpisode() throws Exception {
    TvShowEpisode episode = new TvShowEpisode();
    episode.setTvShow(createTvShow());

    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 3));
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_DVD, 1, 5));
    episode.setTitle("Don't Pet the Teacher");
    episode.setYear(1987);
    episode.setFirstAired("1987-04-26");
    episode.setMediaSource(MediaSource.BLURAY);

    MediaFile mf = new MediaFile();
    mf.setType(MediaFileType.VIDEO);
    mf.setFilename("Aladdin.mkv");
    mf.setVideoCodec("h264");
    mf.setVideoHeight(720);
    mf.setVideoWidth(1280);
    mf.setDuration(3600);
    mf.setOverallBitRate(3500);
    mf.setVideo3DFormat(MediaFileHelper.VIDEO_3D_SBS);

    ArrayList<MediaFileAudioStream> audl = new ArrayList<>();
    MediaFileAudioStream audio = new MediaFileAudioStream();
    audio.setCodec("AC3");
    audio.setLanguage("en");
    audio.setAudioChannels(6);
    audl.add(audio);

    audio = new MediaFileAudioStream();
    audio.setCodec("MP3");
    audio.setLanguage("de");
    audio.setAudioChannels(2);
    audl.add(audio);

    mf.setAudioStreams(audl);

    MediaFileSubtitle sub = new MediaFileSubtitle();
    sub.setLanguage("de");
    mf.addSubtitle(sub);

    episode.addToMediaFiles(mf);

    return episode;
  }
}
