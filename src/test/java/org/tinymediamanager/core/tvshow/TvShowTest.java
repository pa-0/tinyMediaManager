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

import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.TvShowEpisodeAndSeasonParser.EpisodeMatchingResult;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;

/**
 * The Class TvShowTest.
 * 
 * @author Manuel Laggner
 */
public class TvShowTest extends BasicTvShowTest {

  @Before
  public void setup() throws Exception {
    super.setup();
    setTraceLogging();
  }

  @Test
  public void testTvShows() {
    try {
      TmmModuleManager.getInstance().startUp();
      TvShowModuleManager.getInstance().startUp();
      createFakeShow("Show 1");
      createFakeShow("Show 2");
      createFakeShow("Show 3");

      TvShowList instance = TvShowModuleManager.getInstance().getTvShowList();

      for (TvShow show : instance.getTvShows()) {
        System.out.println(show.getTitle());
        for (TvShowSeason season : show.getSeasons()) {
          System.out.println("Season " + season.getSeason());
          for (MediaFile mf : season.getMediaFiles()) {
            System.out.println(mf.toString());
          }
        }
      }

      TvShowModuleManager.getInstance().shutDown();
      TmmModuleManager.getInstance().shutDown();
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  /**
   * Test TV renamer
   * 
   * @throws Exception
   */
  @Test
  public void testRenamerParams() throws Exception {
    // setup dummy
    MediaFile dmf = new MediaFile(getWorkFolder().resolve("video.avi"));

    TmmModuleManager.getInstance().startUp();
    TvShowModuleManager.getInstance().startUp();

    TvShow show = new TvShow();
    show.setPath(getWorkFolder().toString());
    show.setTitle("showname");

    TvShowEpisode ep = new TvShowEpisode();
    ep.setTitle("episodetitle2");
    ep.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 2));
    ep.addToMediaFiles(dmf);
    ep.setTvShow(show);
    show.addEpisode(ep);

    ep = new TvShowEpisode();
    ep.setTitle("3rd episodetitle");
    ep.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 3));
    ep.addToMediaFiles(dmf);
    ep.setTvShow(show);
    show.addEpisode(ep);

    TvShowModuleManager.getInstance().getTvShowList().addTvShow(show);
    // setup done

    // display renamed EP name :)
    System.out.println(TvShowRenamer.createDestination(TvShowModuleManager.getInstance().getSettings().getRenamerFilename(), show.getEpisodes()));
    System.out.println(TvShowRenamer.generateEpisodeFilenames(show, dmf, "").get(0).getFilename());

    TvShowModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
  }

  /**
   * Test episode matching.
   */
  @Test
  public void testEpisodeMatching() {
    // assertEqual("S: E:", detectEpisode(""));

    // ************************************************************************
    // various real world examples
    assertEqual("S:4 E:12", detectEpisode("Die Heiland – Wir sind Anwalt - Folge 37_ Kompromisslos (S04_E12) - 20241121 - 220000.mkv"));
    assertEqual("S:2024", detectEpisode("The.Daily.Show.2024.04.23.Stephanie.Kelton.1080p.HEVC.x265"));
    assertEqual("S:4 E:11", detectEpisode("TV French\\Unite 9\\Season 04\\S04E11 - Episode 84.mkv"));
    assertEqual("S:1 E:1", detectEpisode("[DB]Haven't You Heard I'm Sakamoto_-_01_(Dual Audio_10bit_BD1080p_x265).mkv")); // Anime
    assertEqual("S:3 E:50", detectEpisode("[HorribleSubs] Shingeki no Kyojin S3 - 50 [1080p].mkv"));
    assertEqual("S:5 E:22", detectEpisode("Quincy.S05E22.Gefaehrlicher.Krankentransport.GERMAN.FS.dTV.X264-SQUiZZiEs\\s-quincy-s0522.mkv"));
    assertEqual("S:-1", detectEpisode("Specials\\Bye.Bye.Big.Bang.Theory.Das.Special.GERMAN.1080p.HDTV.x264-VoDTv.mkv"));
    assertEqual("S:1 E:11 E:12", detectEpisode(
        "Zoo.S01.German.DD51.Dubbed.DL.1080p.BD.x264-TVS\\Zoo.S01E11E12.Angriff.der.Leoparden.Das.Heilmittel.German.DD51.Dubbed.DL.1080p.BD.x264-TVS\\tvs-zoo-dd51-ded-dl-18p-bd-x264-11112.mkv"));
    assertEqual("S:17 E:0", detectEpisode("S17\\Die.Rosenheim.Cops.S17E00.German.1080p.WebHD.h264-FKKTV\\fkktv-die.rosenheim.cops.s17e00-1080p.mkv"));
    assertEqual("S:1 E:1",
        detectEpisode("S01\\9-1-1.S01E01.Echte.Helden.German.DD51.Dubbed.DL.1080p.AmazonHD.x264-TVS\\tvs-911-dd51-ded-dl-18p-azhd-x264-101.mkv"));
    assertEqual("S:2 E:3", detectEpisode("S02\\Scooby.Doo.S02E03.DL.FS.WEBRiP.h264-BET\\bet_snsd203_we48h2.mkv"));
    assertEqual("S:1 E:3", detectEpisode(
        "Escape.at.Dannemora.S01.German.DD51.Synced.DL.1080p.AmazonHD.x264-TVS\\Escape.at.Dannemora.S01E03.German.DD51.Synced.DL.1080p.AmazonHD.x264-TVS\\tvs-escape-at-dannemora-dd51-sed-dl-18p-azhd-x264-103.mkv"));
    assertEqual("S:2 E:8", detectEpisode(
        "The.Affair.S02.German.DD+51.DL.720p.AmazonHD.x264-TVS\\The.Affair.S02E08.German.DD+51.DL.720p.AmazonHD.x264-TVS\\tvs-affair-dd51-dl-7p-azhd-x264-208.mkv"));
    assertEqual("S:1 E:1", detectEpisode("showname S01E01\\ijfi38jsoid88939859283j.mkv"));
    assertEqual("S:8 E:4", detectEpisode("Homeland - Temporada 8 [HDTV][Cap.804][Castellano][www.descargas2020.org].avi"));
    assertEqual("S:10 E:4", detectEpisode("Homeland - Temporada 10 [HDTV][Cap.1004][Castellano][www.descargas2020.org].avi"));
    assertEqual("S:3 E:5", detectEpisode("S03 EP05 The Bed of Nails.avi"));
    assertEqual("S:3 E:105", detectEpisode("S03 EP105 The Bed of Nails.avi"));
    assertEqual("S:3 E:5", detectEpisode("S03.EP05.The.Bed.of.Nails.avi"));
    assertEqual("S:1 E:101", detectEpisode("Eisenbahn-Romantik.S01.E101.mp4"));
    assertEqual("S:5 E:1001", detectEpisode("S05.E1001.El.beso.de.la.mujer.veneno.mp4"));
    assertEqual("S:2011", detectEpisode("Game of Thrones\\2011-04-17 - Winter Is Coming.avi"));
    assertEqual("S:2011", detectEpisode("Game of Thrones\\17.04.2011 - Winter Is Coming.avi"));
    assertEqual("S:5 E:1", detectEpisode("Breaking Bad S05E01 S05E02 HDTV XViD-xyz\\E01 - Live Free or Die.avi"));
    assertEqual("S:5 E:1", detectEpisode("Breaking Bad S05E01 S05E02 HDTV XViD-xyz\\S05E01 - Live Free or Die.avi"));
    assertEqual("S:2 E:13", detectEpisode("Simon & Simon\\Season 2\\Simon & Simon - S02E13\\VIDEO_TS\\VTS_01_1.VOB"));
    assertEqual("S:1 E:1 E:2 E:3", detectEpisode("Dexter S01E01 S01E02 S01E03\\VIDEO_TS\\VIDEO_TS.VOB"));
    assertEqual("S:1 E:1", detectEpisode("TheShowName S01E01 Episode Name (1920x1080) [UploaderTag].mp4"));
    assertEqual("S:8 E:1", detectEpisode("BlBlub - S08E01 - Messy S08E01 - Messy.mp4"));
    assertEqual("S:2 E:17", detectEpisode("Brooklyn Nine-Nine S02E17 HDTV x264 AAC E-Subs [GWC].mp4"));
    assertEqual("S:2 E:4", detectEpisode("Its Always Sunny In Philadelphia Season 02 Episode 04 Charlie Gets Crippled-1.mp4"));
    assertEqual("S:1 E:4", detectEpisode("Season 1/04 Charlie Has Cancer-1.mp4"));
    assertEqual("S:1 E:9", detectEpisode("Band of Brothers - 109 - Wir Waren Wie Brüder - Warum Wir Kämpfen (2001)"));
    assertEqual("S:1 E:25", detectEpisode("Cowboy Bebop - S01E25 - The Real Folk Blues Part II.mkv")); // roman mixed with normal
    assertEqual("S:1 E:3", detectEpisode("The.Odd.Couple.2015.S01E03.720p.HDTV"));
    assertEqual("S:1 E:1 E:2 E:3", detectEpisode("Stargate Universe (01x01_01x02_01x03) - Air (1)(2)(3)"));
    assertEqual("S:1 E:11", detectEpisode("Episode.11.Ocean.Deep.BluRay.720p.x264-x264Crew.mkv"));
    assertEqual("S:1 E:1", detectEpisode("tvs-castle-dl-ituneshd-xvid-101.avi"));
    assertEqual("S:2 E:9", detectEpisode("440 - 2x09 - .avi"));
    assertEqual("S:1 E:2", detectEpisode("\\Good L G (1 - 13)\\[CBM]_Good_L_G!_-_02_-_The_Battle_Begins_[720p]_[4A34853E].mkv")); // Anime
    assertEqual("S:3 E:1", detectEpisode("s8-vierfrauen-s03e01-repack.avi"));
    assertEqual("S:1 E:3", detectEpisode("tvp-wildesskandinavien-e03-720p.mkv"));
    assertEqual("S:4 E:13", detectEpisode("s800The Mentalist_S04E13_Die goldene Feder.avi"));
    assertEqual("S:1 E:1", detectEpisode("AwesomeTvShow.S01E01-480p.mkv"));
    assertEqual("S:7 E:9 E:10", detectEpisode("stvs7ep9-10.avi"));
    assertEqual("S:1 E:545", detectEpisode("s01e545 - Steamtown USA.mkv")); // http://thetvdb.com/?tab=season&seriesid=188331&seasonid=311381&lid=7
    assertEqual("S:13 E:2 Split", detectEpisode("Doctor.Who.S13.E2.Part4.Planet.of.Evil.DVDRip.XviD-m00tv.avi"));
    assertEqual("S:3 E:5", detectEpisode("vs-once-upon-a-time-_S03XE05_dd51-ded-dl-7p-bd-x264-305.mkv"));
    assertEqual("S:5 E:1", detectEpisode("Live_at_the_Apollo_Series_5_-_Episode_1_b00p86mz_default"));
    assertEqual("S:6 E:1", detectEpisode("The.League.S06E01.720p.WEB-DL.DD5.1.H.264-pcsyndicate.mkv"));
    assertEqual("S:2 E:9", detectEpisode("Season 02/CSI.Crime.Scene.Investigation.S02E09.And.Then.There.Were.None.360p.DVDRip.MP3.XviD.avi"));
    assertEqual("S:7 E:15", detectEpisode("The.Big.Bang.Theory.S07E15.Eisenbahnromantik.German.DD51.Dubbed.DL.1080p.BD.x264-TVS.mkv"));
    assertEqual("S:1946 E:5", detectEpisode("S1946E05.mkv"));
    assertEqual("S:3 E:8", detectEpisode("Game of Thrones - 3x08 - Die Zweitgeborenen (Second sons)[1080p AAC-6ch de en].avi"));
    assertEqual("S:10 E:5", detectEpisode("Looney Tunes - 10x05 - Episodename"));
    assertEqual("S:1960 E:5", detectEpisode("Looney Tunes - 1960x05 - Episodename"));
    assertEqual("S:4 E:1", detectEpisode("The Big Bang Theory_S04E01_31 Liebhaber, aufgerundet.m4v"));
    assertEqual("S:1 E:2 E:4", detectEpisode("Shaun das Schaf - S01E02_1x04 - Badetag_Summen der Bienen.ts"));
    assertEqual("S:3 E:3", detectEpisode("Supergirl - S03E03 S03E03 - Far From the Tree - Far From the Tree.mkv"));
    assertEqual("S:3 E:9", detectEpisode("Vikings_S03E09_10bit_x265_1080p_BluRay_6CH_30nama_30NAMA.mkv"));
    assertEqual("S:1 E:5", detectEpisode("S01/05 Gray Matter.avi"));
    assertEqual("S:1 E:5", detectEpisode("S 01/05 Gray Matter.avi"));
    assertEqual("S:4 E:101", detectEpisode("Season 4 Episode 101.avi"));
    assertEqual("S:4 E:204", detectEpisode("4x204.avi"));
    assertEqual("S:1 E:13 E:14 E:15", detectEpisode("Peter Pan S01E13_1x14_1x15 - El Hookato.ts"));
    assertEqual("S:1 E:1", detectEpisode("Better Call Saul - S01E01 - Uno - [2160p-WEB.DL-HEVC.x265-10bit.SDR-DTS.DTS5.1-TrollUHD].mkv"));

    // ANIME - https://kodi.wiki/view/Anime
    // PREPEND
    assertEqual("S:0 E:1", detectEpisode("[SHiN-gx] Fight Ippatsu! Juuden-chan!! - Special 1 [720x480 AR h.264 FLAC][v2][FF09021F].mkv"));
    assertEqual("S:0 E:1", detectEpisode("[gleam] Kurenai OVA - 01 [OAD][0e73f000].mkv"));
    assertEqual("S:0 E:1", detectEpisode("[Jarzka] Saki Picture Drama 1 [480p 10bit DVD FLAC] [BA3CE364]"));
    assertEqual("S:1 E:1", detectEpisode("Saki/Season 1/Saki [Jarzka]/[Jarzka] Saki 01 - Encounter [480p 10bit DVD FLAC] [9EED32CB]"));
    assertEqual("S:3 E:1", detectEpisode("Saki/Season 3/[Underwater-FFF] Saki Zenkoku-hen - The Nationals - 01 (720p) [AF65724D]"));
    assertEqual("S:2 E:5", detectEpisode("[CoalGuys] K-ON!! S2 - 05 [4B19B10F]"));
    assertEqual("S:1 E:1", detectEpisode("[avatar-nyanko] Koikoi 7 - 01 (DVD) [5E95FA4A]"));
    assertEqual("S:1 E:1", detectEpisode("[gg]_Chuunibyou_Demo_Koi_ga_Shitai!_-_01_[5B6EFD1F]"));
    assertEqual("S:1 E:1", detectEpisode("[Eclipse] Akane-iro ni Somaru Saka - 01 (1024x576 h264) [39920E63].mkv"));
    assertEqual("S:1 E:1", detectEpisode("[gg]_Bakemonogatari_-_01_[CC0CF5D2].mkv "));
    assertEqual("S:1 E:3", detectEpisode("[Doki]_Asobi_ni_Iku_yo!_-_03v2_(1280x720_h264_AAC)_[B5B9C6F3].mkv"));
    assertEqual("S:1 E:2", detectEpisode("[Coalgirls]_Yuru_Yuri_02_(1280x720_Blu-Ray_FLAC)_[43E5A6B4]"));
    assertEqual("S:1 E:1", detectEpisode("Touch 01(DVD) - (112ceb61) Central Anime"));
    assertEqual("S:1 E:2", detectEpisode("Cross Game 02 - Central Anime (1280x720) [BF23052D].mp4"));
    assertEqual("S:1 E:135", detectEpisode("[Taka]_Naruto_Shippuuden_135_[480p][9073B8C2]"));
    assertEqual("S:2 E:4", detectEpisode("Season 2/[Erai-raws] Shy 2nd Season - 04 [1080p][Multiple Subtitle][79770C75].mkv"));
    assertEqual("S:13 E:2", detectEpisode("Season 13/[Erai-raws] Yami Shibai 13 - 02 [1080p][Multiple Subtitle][D7E29CA3].mkv"));
    // APPEND (no hash!)
    assertEqual("S:2 E:14", detectEpisode("Saki/Season 2/[HorribleSubs] Saki Episode of Side A - 14 [720p]"));
    assertEqual("S:2 E:1", detectEpisode("[DeadFish] Toaru Kagaku no Railgun S - S2 - 01 [720p][AAC].mp4"));
    assertEqual("S:0 E:1", detectEpisode("[DeadFish] Jinrui wa Suitai Shimashita - Special 01 [BD][720p][AAC].mp4"));
    // should be Season 1 as per Anime definition - unfortunately this clashes with our -1 approach
    assertEqual("S:1 E:1", detectEpisode("[a.f.k.] Lucky Star - 01.avi"));
    assertEqual("S:1 E:4", detectEpisode("Air Master - 04 [HQA&N!].avi"));
    assertEqual("S:1 E:1", detectEpisode("[ANE] Yosuga no Sora - Ep01v2 [BDRip 1080p x264 FLAC]"));

    // special case - show with only number like "24"
    assertEqual("S:1 E:1", detectEpisode("24 S01E01 1080p BluRay.mkv", "24"));
    assertEqual("S:1 E:24", detectEpisode("24 S01E24 1080p BluRay.mkv", "24"));
    assertEqual("S:1 E:24", detectEpisode("24 S01EP24 1080p BluRay.mkv", "24"));
    assertEqual("S:24 E:1", detectEpisode("24 S24E01 1080p BluRay.mkv", "24"));
    assertEqual("S:1 E:24", detectEpisode("S01E24 1080p BluRay.mkv", "24"));
    assertEqual("S:24 E:1", detectEpisode("S24E01 1080p BluRay.mkv", "24"));
    assertEqual("S:1 E:24", detectEpisode("24 Season 1 Episode 24 1080p BluRay.mkv", "24"));
    assertEqual("S:24 E:1", detectEpisode("24 Season 24 Episode 1 1080p BluRay.mkv", "24"));

    // subtitles in folder, named with numbers
    assertEqual("S:1 E:1", detectEpisode("Subs/subtitle folder for S01E01 and so/1_german.srt"));
    assertEqual("S:1 E:1", detectEpisode("Subs/subtitle folder for S01E01 and so/23_german.srt"));

    // ************************************************************************
    // 1-3 chars, if they are the ONLY numbers in file
    assertEqual("S:1 E:2", detectEpisode("2.mkv"));
    assertEqual("S:1 E:2", detectEpisode("2 name.mkv"));
    assertEqual("S:1 E:2", detectEpisode("name 2.mkv"));

    assertEqual("S:1 E:2", detectEpisode("02.mkv"));
    assertEqual("S:1 E:2", detectEpisode("02 name.mkv"));
    assertEqual("S:1 E:2", detectEpisode("name 02.mkv"));

    assertEqual("S:1 E:2", detectEpisode("102.mkv"));
    assertEqual("S:1 E:2", detectEpisode("102 name.mkv"));
    assertEqual("S:1 E:2", detectEpisode("name 102.mkv"));

    assertEqual("S:1 E:2", detectEpisode("season 1\\nam.e.2.mkv"));
    assertEqual("S:1 E:2", detectEpisode("season 1/nam.e.2.mkv"));
    assertEqual("S:1 E:1", detectEpisode("Wisher.2021.E01.V2.WEB-DL.4k.H265.DDP.AAC-HDCTV.mp4"));

    assertEqual("S:1 E:1", detectEpisode("[01].ext")); // just optionals!
    assertEqual("S:1 E:2", detectEpisode("[01] 02.ext")); // ignore optionals

    // TODO: currently we take the LAST number and treat it as episode
    // NO multi matching for just numbers!!
    assertEqual("S:1 E:6", detectEpisode("2 3 6.mkv"));
    assertEqual("S:1 E:4", detectEpisode("02 03 04 name.mkv"));
    // except for 3 char ones ;)
    assertEqual("S:1 E:1 E:2 E:3", detectEpisode("101 102 103.mkv"));
    assertEqual("S:1 E:3", detectEpisode("1 12 103 25 7.mkv")); // start with highest number

    // ************************************************************************
    // http://wiki.xbmc.org/index.php?title=Video_library/Naming_files/TV_shows
    // with season
    assertEqual("S:1 E:2", detectEpisode("name.s01e02.ext"));
    assertEqual("S:1 E:2", detectEpisode("name.s01.e02.ext"));
    assertEqual("S:1 E:2", detectEpisode("name.s1e2.ext"));
    assertEqual("S:1 E:2", detectEpisode("name.s01_e02.ext"));
    assertEqual("S:1 E:2", detectEpisode("name.1x02.blablubb.ext"));
    assertEqual("S:1 E:2", detectEpisode("name.1x02.ext"));
    assertEqual("S:1 E:2", detectEpisode("name.102.ext"));

    // without season
    // Even if there is no "season", Kodi will just assume all episodes are "Season 1" when using EP##.
    assertEqual("S:1 E:2", detectEpisode("name.ep02.ext"));
    assertEqual("S:1 E:2", detectEpisode("name.ep_02.ext"));
    assertEqual("S:1 E:2", detectEpisode("name.part.II.ext"));
    assertEqual("S:1 E:2", detectEpisode("name.pt.II.ext"));
    assertEqual("S:1 E:2", detectEpisode("name.pt_II.ext"));
    assertEqual("S:1 E:1", detectEpisode("EP01.avi"));
    assertEqual("S:3 E:1", detectEpisode("season 3/EP01.avi"));
    assertEqual("S:3 E:3", detectEpisode("season 3 EP01 EP02 EP03/EP03.avi"));

    // multi episode
    assertEqual("S:1 E:1", detectEpisode("name.s01e01.s01e01.ext")); // double EP
    assertEqual("S:1 E:1 E:2", detectEpisode("name.s01e01.s01e02.ext"));
    assertEqual("S:1 E:1 E:3", detectEpisode("name.s01e01.s01e03.ext"));// second EP must NOT be subsequent number (ascending)!
    assertEqual("S:1 E:1 E:2", detectEpisode("name.s01e02.s01e01.ext"));// second EP must NOT be subsequent number (ascending)!
    assertEqual("S:1 E:1 E:2", detectEpisode("name.s01e01.episode1.title.s01e02.episode2.title.ext"));
    assertEqual("S:1 E:1 E:2 E:3", detectEpisode("name.s01e01.s01e02.s01e03.ext"));
    assertEqual("S:1 E:1 E:2", detectEpisode("name.1x01_1x02.ext")); // works but shouldn't ;) _1 is detected as e1
    assertEqual("S:2 E:11 E:12 E:13", detectEpisode("name.2x11_2x12_2x13.ext")); // worst case: _2 is always being detected as e2
    assertEqual("S:1 E:1 E:2", detectEpisode("name.s01e01 1x02.ext"));
    assertEqual("S:1 E:1 E:2", detectEpisode("name.ep01.ep02.ext"));
    assertEqual("S:1 E:2 E:4 E:345", detectEpisode("name.s01e02e04ep345.ext")); // non consecutive episodes
    // multi with season change - invalid
    assertEqual("S:1 E:1", detectEpisode("name.s01e01.s05e02.ext"));
    assertEqual("S:1 E:1 E:3", detectEpisode("name.01x01_03x02_01x03.ext"));

    // multi episode short
    assertEqual("S:1 E:1 E:2", detectEpisode("name.s01e01e02.ext"));
    assertEqual("S:1 E:1 E:2 E:3", detectEpisode("name.s01e01e02e03.ext"));
    assertEqual("S:1 E:1 E:2 E:3", detectEpisode("name.s01e01-02-03.ext"));
    assertEqual("S:1 E:1 E:2", detectEpisode("name.1x01x02.ext"));
    assertEqual("S:1 E:1 E:2", detectEpisode("name.ep01_02.ext"));

    // multi episode mixed; weird, but valid :p - we won't detect that now because the
    // regexp would cause too much false positives
    // assertEqual("S:1 E:1 E:2 E:3 E:4", detectEpisode("name.1x01e02_03-x-04.ext"));

    // split episode
    // TODO: detect split?
    assertEqual("S:1 E:1 Split", detectEpisode("name.s01e01.CD1.ext"));
    assertEqual("S:1 E:1 Split", detectEpisode("name.s01e01.a.ext"));
    assertEqual("S:1 E:1 Split", detectEpisode("name.1x01.part1.ext"));
    assertEqual("S:1 E:1 Split", detectEpisode("name.1x01.pt1.ext"));
    assertEqual("S:1 E:1", detectEpisode("name.ep01.1.ext")); // do not detect that one
    assertEqual("S:1 E:1", detectEpisode("name.101.1.ext"));
    assertEqual("S:1 E:1 Split", detectEpisode("name.ep01a_01.discb.ext"));
    assertEqual("S:1 E:1 Split", detectEpisode("name.s01e01.1.s01e01.2.of.2.ext"));
    assertEqual("S:1 E:1", detectEpisode("name.1x01.1x01.2.ext")); // do not detect that one
  }

  /**
   * Detect episode.
   * 
   * @param name
   *          the name
   * @return the string
   */
  private String detectEpisode(String name) {
    return detectEpisode(name, "asdf[.*]asdf");
  }

  /**
   * Detect episode.
   *
   * @param name
   *          the name
   * @return the string
   */
  private String detectEpisode(String name, String show) {
    StringBuilder sb = new StringBuilder();
    // EpisodeMatchingResult result = TvShowEpisodeAndSeasonParser.detectEpisodeFromFilename(new File(name));
    EpisodeMatchingResult result = TvShowEpisodeAndSeasonParser.detectEpisodeFromFilename(name, show);
    sb.append("S:");
    sb.append(result.season);
    for (int ep : result.episodes) {
      sb.append(" E:");
      sb.append(ep);
    }
    if (result.stackingMarkerFound) {
      sb.append(" Split");
    }
    System.out.println(padRight(sb.toString().strip(), 40) + name);
    return sb.toString().strip();
  }

  private String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);
  }

  /**
   * Test the removal of season/episode string for clean title
   */
  @Test
  public void testRemoveEpisodeString() {
    // assertEqual("S: E:", detectEpisode(""));

    // ************************************************************************
    // various real world examples
    assertEqual("Der Weg nach Uralia", cleanTitle("Die Gummibärenbande - S05E02 - Der Weg nach Uralia.avi", "Die Gummibärenbande"));
    assertEqual("BlBlub Messy", cleanTitle("BlBlub - S08E01 - Messy.mp4", ""));
    assertEqual("Messy", cleanTitle("BlBlub - S08E01 - Messy.mp4", "BlBlub"));
    // assertEqual("episode1 title episode2 title", cleanTitle("name.s01e01.episode1.title.s01e02.episode2.title.ext", "name")); // E1 removed!
    assertEqual("my first title my second title", cleanTitle("name.s01e01.my.first.title.s01e02.my.second.title.ext", "name"));
    assertEqual("ep01 ep02", cleanTitle("name.ep01.ep02.ext", "name")); // no title
    assertEqual("Franklin Juega El Partido & Franklin Quiere Una Mascota",
        cleanTitle("Franklin - S01E01 - Franklin Juega El Partido & Franklin Quiere Una Mascota.avi", "Franklin"));
  }

  private String cleanTitle(String filename, String showname) {
    String basename = FilenameUtils.getBaseName(filename);
    return TvShowEpisodeAndSeasonParser.cleanEpisodeTitle(basename, showname);
  }

  @Test
  public void testSeasonFolderDetection() {
    TvShowSettings.getInstance().setRenamerSeasonFoldername("S${seasonNr}");
    TvShow tvShow = new TvShow();
    tvShow.setPath(getWorkFolder().resolve("show").toString());

    // Season 1: 80% of the episodes are in the subfolder "Season 01"
    TvShowEpisode episode = new TvShowEpisode();
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 1));
    MediaFile mf = new MediaFile(getWorkFolder().resolve("show/Season 01/s01e01.avi"));
    episode.addToMediaFiles(mf);
    tvShow.addEpisode(episode);

    episode = new TvShowEpisode();
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 2));
    mf = new MediaFile(getWorkFolder().resolve("show/Season 01/ep2/s01e02.avi"));
    episode.addToMediaFiles(mf);
    tvShow.addEpisode(episode);

    episode = new TvShowEpisode();
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 3));
    mf = new MediaFile(getWorkFolder().resolve("show/Season 01/ep3/extract/s01e03.avi"));
    episode.addToMediaFiles(mf);
    tvShow.addEpisode(episode);

    episode = new TvShowEpisode();
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 4));
    mf = new MediaFile(getWorkFolder().resolve("show/Season 1/s01e04.avi"));
    episode.addToMediaFiles(mf);
    tvShow.addEpisode(episode);

    episode = new TvShowEpisode();
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 5));
    mf = new MediaFile(getWorkFolder().resolve("show/Season 01/s01e05.avi"));
    episode.addToMediaFiles(mf);
    tvShow.addEpisode(episode);

    assertThat(TvShowHelpers.detectSeasonFolder(tvShow, 1)).isEqualTo("Season 01");

    // Season 2: every EP is in another subfolder
    episode = new TvShowEpisode();
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 2, 1));
    mf = new MediaFile(getWorkFolder().resolve("show/Season 2/s02e01.avi"));
    episode.addToMediaFiles(mf);
    tvShow.addEpisode(episode);

    episode = new TvShowEpisode();
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 2, 2));
    mf = new MediaFile(getWorkFolder().resolve("show/Season 02/ep2/s02e02.avi"));
    episode.addToMediaFiles(mf);
    tvShow.addEpisode(episode);

    episode = new TvShowEpisode();
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 2, 3));
    mf = new MediaFile(getWorkFolder().resolve("show/S2/ep3/extract/s02e03.avi"));
    episode.addToMediaFiles(mf);
    tvShow.addEpisode(episode);

    episode = new TvShowEpisode();
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 2, 4));
    mf = new MediaFile(getWorkFolder().resolve("show/s02e04.avi"));
    episode.addToMediaFiles(mf);
    tvShow.addEpisode(episode);

    episode = new TvShowEpisode();
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 2, 5));
    mf = new MediaFile(getWorkFolder().resolve("show/s02e05/s02e05.avi"));
    episode.addToMediaFiles(mf);
    tvShow.addEpisode(episode);

    // should be in TV show root
    assertThat(TvShowHelpers.detectSeasonFolder(tvShow, 2)).isEqualTo("");
  }
}
