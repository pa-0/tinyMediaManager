package org.tinymediamanager.core;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.tinymediamanager.scraper.util.ParserUtils;

public class ParserUtilsTest extends BasicTest {

  @Test
  public void testNamingDetection() {
    setTraceLogging();

    assertEqual("2012", detectTY("2012.WEB-DL.1080p.AVC.mkv"));
    assertEqual("2012 | 2008", detectTY("2012.(2008).WEB-DL.1080p.mkv"));
    assertEqual("movie | 2008", detectTY("UNRATED movie 2008 DTS something bla bla.mkv"));

    // remove AKA second titles (like Kodi)
    assertEqual("Porcile | 1969", detectTY("Porcile AkA Pigsty (1969) -- Pier Paolo Pasolini"));
    assertEqual("AkA | 2023", detectTY(" AkA (2023)"));
    assertEqual("AkA | 2023", detectTY("AkA (2023)"));
    assertEqual("AkA", detectTY("AkA"));
    assertEqual("AkA", detectTY(" AkA "));
    assertEqual("AkA whatever", detectTY(" AkA whatever"));// stripped name does not have space anylonger
    assertEqual("A K A | 2006", detectTY("A.K.A. (2006)"));

    assertEqual("Cowboys", detectTY("Cowboys.4k.WEB-DL.MicroHD.1080p.AVC.AC3.5.1.SPA-AC3.5.1.ENG.SUBS.mkv"));
    assertEqual("Cowboys | 2020", detectTY("Cowboys.(2020).WEB-DL.MicroHD.1080p.AVC.AC3.5.1.SPA-AC3.5.1.ENG.SUBS.mkv"));
    assertEqual("Rocketman | 2019", detectTY("Rocketman-2019-MULTi-UHD-BluRay-2160p-HDR-TrueHD-Atmos-7-1-HEVC-DDR.mkv"));
    assertEqual("Harry Potter 7 Part 1", detectTY("Harry Potter 7 - Part 1.mkv")); // PartX is NOT removed
    assertEqual("Harry Potter 7 Part 2", detectTY("Harry Potter 7 - Part 2 CD1.mkv"));
    assertEqual("Safety Not Guaranteed", detectTY("Safety ,,Not Guaranteed.mkv"));
    assertEqual("Safety Not Guaranteed", detectTY("Safety divx Not Guaranteed.mkv"));
    assertEqual("Safety Not Guaranteed | 2012", detectTY("Safety Not Guaranteed [2012, HEVC-1080p].mkv"));
    assertEqual("Safety Not Guaranteed | 2012", detectTY("Safety.Not_Guaranteed [2012] [HEVC-1080p].mkv"));
    assertEqual("Not Guaranteed | 2012", detectTY("[Safety] Not Guaranteed [HEVC-1080p] [2012].mkv"));
    assertEqual("Safety not guaranteed | 2012", detectTY("[Safety not [guaranteed] , , , [HEVC-, , ,,,1080p] [2012].mkv"));
    assertEqual("Safety Not Guaranteed", detectTY("[Safety Not Guaranteed].mkv"));
    assertEqual("Por fin se casa Zamora | 1926", detectTY("Por fin se casa Zamora (1926)"));

    assertEqual("Gemma Bovery | 2014", detectTY("Gemma.Bovery.2014.[1920x800].24.000fps.1080p.BRRip.x264.JYK.mkv"));
    assertEqual("Ai No Korīda | 1976", detectTY("Ai.No.Korīda.1976.[1280x772].23.976fps.720p.x264.CiNEFiLE.mkv"));
    assertEqual("In The Realm Of The Senses | 1976", detectTY("In The Realm Of The Senses (1976) - 愛のコリーダ"));

    assertEqual("my movie framerate", detectTY("my movie framerate 24.000fps bla bla"));
    assertEqual("my movie framerate", detectTY("my movie framerate 24.000 fps bla bla")); // fps as stopword
    assertEqual("framerate bla bla", detectTY("framerate 24.000 fps bla bla")); // fps NOT stopword, since too early position
    assertEqual("my movie framerate 0", detectTY("my movie framerate 24.0000 fps bla bla")); // one 0 to much, stays
    assertEqual("my movie framerate", detectTY("my movie framerate 23.976fps bla bla"));
    assertEqual("my movie framerate", detectTY("my movie framerate 23.98fps bla bla"));
    assertEqual("my movie framerate bla bla", detectTY("my movie framerate 23.98 bla bla")); // no stopwords, take all

    // replace resolution (000-9999 x 000-9999) when delimiter in front
    assertEqual("RES 1x1", detectTY("RES 1x1"));
    assertEqual("RES 10x10", detectTY("RES 10x10"));
    assertEqual("RES", detectTY("RES 100x100")); // <--- remove res
    assertEqual("RES", detectTY("RES 1000x1000")); // <--- remove res
    assertEqual("RES 10000x10000", detectTY("RES 10000x10000"));

    // RELEASE NAMES
    assertEqual("Auferstanden | 2016", detectTY("Auferstanden.2016.German.DL.DTS.1080p.BluRay.x264-CiNEViSiON"));
    assertEqual("Auferstanden | 2016", detectTY("Auferstanden.2016.German.DL.DTS.720p.BluRay.x264-CiNEViSiON"));
    assertEqual("Der Blob | 1988", detectTY("Der.Blob.REMASTERED.GERMAN.1988.DL.BDRiP.x264-GOREHOUNDS"));
    assertEqual("Die Hochzeit meiner Eltern GERMAN | 2016", detectTY("Die.Hochzeit.meiner.Eltern.GERMAN.2016.720p.WEBHD.h264-REMSG"));
    assertEqual("Die Hochzeit meiner Eltern GERMAN | 2016", detectTY("Die.Hochzeit.meiner.Eltern.GERMAN.2016.WEBRiP.x264-REMSG"));
    assertEqual("Die Hochzeit meines Vaters | 2006", detectTY("Die.Hochzeit.meines.Vaters.2006.German.720p.HDTV.x264-AIDA"));
    assertEqual("Die Hochzeit meines Vaters | 2006", detectTY("Die.Hochzeit.meines.Vaters.2006.German.HDTVRip.x264-AIDA"));
    assertEqual("Familie auf Rezept | 2015", detectTY("Familie.auf.Rezept.2015.German.1080p.BluRay.x264-ENCOUNTERS"));
    assertEqual("Familie auf Rezept | 2015", detectTY("Familie.auf.Rezept.2015.German.720p.BluRay.x264-ENCOUNTERS"));
    assertEqual("Forsaken | 2015", detectTY("Forsaken.2015.German.DL.1080p.BluRay.x264-ENCOUNTERS"));
    assertEqual("Lap Dance schnelles Geld hat seinen Preis German | 2014",
        detectTY("Lap.Dance.schnelles.Geld.hat.seinen.Preis.German.2014.AC3.DVDRiP.x264-KNT"));
    assertEqual("London Has Fallen | 2016", detectTY("London.Has.Fallen.2016.German.DL.1080p.BluRay.x264.REPACK-ENCOUNTERS"));
    assertEqual("My Big Fat Greek Wedding 2 | 2016", detectTY("My.Big.Fat.Greek.Wedding.2.2016.German.DTS.DL.1080p.BluRay.x264-COiNCiDENCE"));
    assertEqual("My Big Fat Greek Wedding 2 | 2016", detectTY("My.Big.Fat.Greek.Wedding.2.2016.German.DTS.DL.720p.BluRay.x264-COiNCiDENCE"));
    assertEqual("My Big Fat Greek Wedding 2 German | 2016", detectTY("My.Big.Fat.Greek.Wedding.2.German.2016.AC3.BDRip.x264-COiNCiDENCE"));
    assertEqual("Neues aus dem Reihenhaus GERMAN | 2016", detectTY("Neues.aus.dem.Reihenhaus.GERMAN.2016.720p.WEBHD.h264-REMSG"));
    assertEqual("Neues aus dem Reihenhaus GERMAN | 2016", detectTY("Neues.aus.dem.Reihenhaus.GERMAN.2016.WEBRiP.x264-REMSG"));
    assertEqual("Nur nicht aufregen GERMAN | 2016", detectTY("Nur.nicht.aufregen.GERMAN.2016.720p.WEBHD.h264-REMSG"));
    assertEqual("Point Break | 2015", detectTY("Point.Break.2015.German.DL.1080p.BluRay.x264-ENCOUNTERS"));
    assertEqual("The Descent Abgrund des Grauens | 2005",
        detectTY("The.Descent.Abgrund.des.Grauens.UNCUT.2005.German.DL.1080p.BluRay.x264.iNTERNAL-VideoStar"));
    assertEqual("The Descent Abgrund des Grauens | 2005",
        detectTY("The.Descent.Abgrund.des.Grauens.UNCUT.German.2005.AC3.BDRip.x264.iNTERNAL-VideoStar"));
    assertEqual("Tschiller Off Duty | 2016", detectTY("Tschiller.Off.Duty.2016.German.AC3.1080p.WebHD.h264-ENTiCEMENT"));
    assertEqual("Tschiller Off Duty | 2016", detectTY("Tschiller.Off.Duty.2016.German.AC3.720p.WebHD.h264-ENTiCEMENT"));
    assertEqual("Tschiller Off Duty | 2016", detectTY("Tschiller.Off.Duty.2016.German.AC3.WEBRip.x264-ENTiCEMENT"));
    assertEqual("Twins Zwillinge | 1988", detectTY("Twins.Zwillinge.1988.German.AC3D.DL.720p.BluRay.x264-MOONSHiNERS"));
    assertEqual("Unter Freunden | 2015", detectTY("Unter.Freunden.2015.German.1080p.BluRay.x264-ENCOUNTERS"));
    assertEqual("Unter Freunden | 2015", detectTY("Unter.Freunden.2015.German.720p.BluRay.x264-ENCOUNTERS"));
    assertEqual("Wehe wenn sie losgelassen | 1958", detectTY("Wehe.wenn.sie.losgelassen.1958.German.720p.HDTV.x264-AIDA"));
    assertEqual("Wehe wenn sie losgelassen | 1958", detectTY("Wehe.wenn.sie.losgelassen.1958.German.HDTVRip.x264-AIDA"));
    assertEqual("Z for Zachariah | 2015", detectTY("Z.for.Zachariah.3D.2015.German.DL.1080p.BluRay.x264-STEREOSCOPiC"));
    assertEqual("Transformers | 2007", detectTY("Transformers.2007.2160p.BluRay.REMUX.HEVC.DTS-HD.MA.TrueHD.7.1.Atmos.mkv"));
    // assertEqual("What the Waters Left Behind | 2017", detectTY("What.the.Waters.Left.Behind.2017.SPANISH.ENSUBBED"));

    // OTR recording naming
    assertEqual("Linkin Park Road to Revolution und Live from Madison Square Garden",
        detectTY("Linkin_Park__Road_to_Revolution_und_Live_from_Madison_Square_Garden_12.05.02_01-27_unk.HQ.avi.otrkey"));
    assertEqual("Prohibition Eine amerikanische Erfahrung",
        detectTY("Prohibition_Eine_amerikanische_Erfahrung_12.11.10_22-00_arte_55_TVOON_DE.mpg.HD.avi.otrkey"));
    assertEqual("Rockpalast Depeche Mode", detectTY("Rockpalast__Depeche_Mode_12.09.19_20-15_ardeinsfestival_60_TVOON_DE.mpg.HQ.avi.otrkey"));
    assertEqual("Rockpalast U2", detectTY("Rockpalast__U2_12.09.17_22-10_ardeinsfestival_60_TVOON_DE.mpg.HQ.avi.otrkey"));

    // check some weird TV shows names ;)
    // ok, we cannot do anything for the episode numbers..
    // assertEqual("Simon & Simon S02e13", detectTY("Simon & Simon - S02E13\\VIDEO_TS\\VTS_01_1.VOB"));
    // assertEqual("", detectTY("Dexter S01E01 S01E02 S01E03\\VIDEO_TS\\VIDEO_TS.VOB"));
    assertEqual("Sons of Anarchy S03E09", detectTY("Sons_of_Anarchy_S03E09_10bit_1080p_BluRay_6CH.mkv"));
    assertEqual("TheShowName S01E01 Episode Name", detectTY("TheShowName S01E01 Episode Name (1920x1080) [UploaderTag].mp4"));
    assertEqual("BlBlub S08E01 Messy S08E01 Messy", detectTY("BlBlub - S08E01 - Messy S08E01 - Messy.mp4"));
    assertEqual("Brooklyn Nine Nine S02E17", detectTY("Brooklyn Nine-Nine S02E17 HDTV x264 AAC E-Subs [GWC].mp4"));
    assertEqual("Its Always Sunny In Philadelphia Season 02 Episode 04 Charlie Gets Crippled 1",
        detectTY("Its Always Sunny In Philadelphia Season 02 Episode 04 Charlie Gets Crippled-1.mp4"));
    assertEqual("Season 1/04 Charlie Has Cancer 1", detectTY("Season 1/04 Charlie Has Cancer-1.mp4"));
    assertEqual("Band of Brothers 109 Wir Waren Wie Brüder Warum Wir Kämpfen | 2001",
        detectTY("Band of Brothers - 109 - Wir Waren Wie Brüder - Warum Wir Kämpfen (2001)"));
    assertEqual("Cowboy Bebop S01E25 The Real Folk Blues Part II", detectTY("Cowboy Bebop - S01E25 - The Real Folk Blues Part II.mkv"));
    assertEqual("The Odd Couple | 2015", detectTY("The.Odd.Couple.2015.S01E03.720p.HDTV"));
    assertEqual("Stargate Universe 01x01 01x02 01x03 Air 1 2 3", detectTY("Stargate Universe (01x01_01x02_01x03) - Air (1)(2)(3)"));
    assertEqual("Episode 11 Ocean Deep", detectTY("Episode.11.Ocean.Deep.BluRay.720p.x264-x264Crew.mkv"));
    assertEqual("tvs castle", detectTY("tvs-castle-dl-ituneshd-xvid-101.avi"));
    assertEqual("440 2x09", detectTY("440 - 2x09 - .avi"));
    assertEqual("Good L G 1 13 Good L G! 02 The Battle Begins",
        detectTY("\\Good L G (1 - 13)\\[CBM]_Good_L_G!_-_02_-_The_Battle_Begins_[720p]_[4A34853E].mkv"));
    assertEqual("s8 vierfrauen s03e01", detectTY("s8-vierfrauen-s03e01-repack.avi"));
    assertEqual("tvp wildesskandinavien e03", detectTY("tvp-wildesskandinavien-e03-720p.mkv"));
    assertEqual("s800The Mentalist S04E13 Die goldene Feder", detectTY("s800The Mentalist_S04E13_Die goldene Feder.avi"));
    assertEqual("AwesomeTvShow S01E01", detectTY("AwesomeTvShow.S01E01-480p.mkv"));
    assertEqual("stvs7ep9 10", detectTY("stvs7ep9-10.avi"));
    assertEqual("s01e545 Steamtown USA", detectTY("s01e545 - Steamtown USA.mkv"));
    assertEqual("Doctor Who S13 E2 Part4 Planet of Evil", detectTY("Doctor.Who.S13.E2.Part4.Planet.of.Evil.DVDRip.XviD-m00tv.avi"));
    assertEqual("vs once upon a time S03XE05", detectTY("vs-once-upon-a-time-_S03XE05_dd51-ded-dl-7p-bd-x264-305.mkv"));
    assertEqual("Live at the Apollo Series 5 Episode 1 b00p86mz default", detectTY("Live_at_the_Apollo_Series_5_-_Episode_1_b00p86mz_default"));
    assertEqual("The League S06E01", detectTY("The.League.S06E01.720p.WEB-DL.DD5.1.H.264-pcsyndicate.mkv"));
    assertEqual("Season 02/CSI Crime Scene Investigation S02E09 And Then There Were None",
        detectTY("Season 02/CSI.Crime.Scene.Investigation.S02E09.And.Then.There.Were.None.360p.DVDRip.MP3.XviD.avi"));
    assertEqual("The Big Bang Theory S07E15 Eisenbahnromantik German",
        detectTY("The.Big.Bang.Theory.S07E15.Eisenbahnromantik.German.DD51.Dubbed.DL.1080p.BD.x264-TVS.mkv"));
    assertEqual("S1946E05", detectTY("S1946E05.mkv"));
    assertEqual("Game of Thrones 3x08 Die Zweitgeborenen Second sons",
        detectTY("Game of Thrones - 3x08 - Die Zweitgeborenen (Second sons)[1080p AAC-6ch de en].avi"));
    assertEqual("Looney Tunes 10x05 Episodename", detectTY("Looney Tunes - 10x05 - Episodename"));
    assertEqual("Looney Tunes 1960x05 Episodename", detectTY("Looney Tunes - 1960x05 - Episodename"));
    assertEqual("The Big Bang Theory S04E01 31 Liebhaber aufgerundet", detectTY("The Big Bang Theory_S04E01_31 Liebhaber, aufgerundet.m4v"));
    assertEqual("Shaun das Schaf S01E02 1x04 Badetag Summen der Bienen", detectTY("Shaun das Schaf - S01E02_1x04 - Badetag_Summen der Bienen.ts"));
  }

  @Test
  public void TestNamingDetectionWithBadWords() {

    ArrayList<String> badwords = new ArrayList<>();
    badwords.add("tvs");
    assertEqual("castle", detectTYWithBadWords("tvs-castle-dl-ituneshd-xvid-101.avi", badwords));
    badwords.add("top\\d{3}");
    assertEqual("castle", detectTYWithBadWords("tvs-castle-top100-dl-ituneshd-xvid-101.avi", badwords));

    badwords.clear();
    badwords.add("tvs");
    badwords.add("top\\d+");
    assertEqual("castle", detectTYWithBadWords("tvs-castle-top5-dl-ituneshd-xvid-101.avi", badwords));

    badwords.clear();
    badwords.add("\\(\\d*-\\d*\\)");
    // no year detectable since we strip off the whole year part via badwords
    assertEqual("The Simpsons", detectTYWithBadWords("The Simpsons (1989-2022)", badwords));
    assertEqual("The Simpsons | 1989", detectTYWithBadWords("The Simpsons (1989)", badwords));
    assertEqual("4400", detectTYWithBadWords("4400", badwords));
    assertEqual("4400 | 2009", detectTYWithBadWords("4400 (2009)", badwords));
    // no year detectable since we strip off the whole year part via badwords
    assertEqual("4400", detectTYWithBadWords("4400 (2009-2012)", badwords));
  }

  private String detectTY(String filename) {
    String[] s = ParserUtils.detectCleanTitleAndYear(filename, Collections.emptyList());
    String ret = s[0];
    if (!s[1].isEmpty()) {
      ret = ret + " | " + s[1];
    }
    return ret;
  }

  private String detectTYWithBadWords(String filename, List<String> badwords) {
    String[] s = ParserUtils.detectCleanTitleAndYear(filename, badwords);
    String ret = s[0];
    if (!s[1].isEmpty()) {
      ret = ret + " | " + s[1];
    }
    return ret;
  }

  @Test
  public void imdb() throws IOException {
    String text = Utils.readFileToString(Paths.get("src/test/resources/movie_nfo/kodi.nfo"));
    System.out.println(ParserUtils.detectImdbId(text));
  }

  // @Test
  // public void getTitle() {
  // File f = new File("src/test/resources/testmovies");
  // File[] fileArray = f.listFiles();
  // for (File file : fileArray) {
  // if (file.isDirectory()) {
  // System.out.println(ParserUtils.detectCleanMoviename(file.getName()));
  // }
  // }
  // }
  //
  // @Test
  // public void testRenamedImdb() {
  // File f = new File("/media/Daten/Test_Filme/this is my [tt0123456] movie (2009)");
  // System.out.println(ParserUtils.detectCleanMoviename(f.getName()));
  // }
  //
  // @Test
  // public void testBadword() {
  // File f = new File("/media/Daten/Test_Filme/xxx.avi");
  // System.out.println(ParserUtils.detectCleanMoviename(f.getName()));
  // }
}
