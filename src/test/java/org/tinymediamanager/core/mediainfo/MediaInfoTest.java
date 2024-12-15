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

package org.tinymediamanager.core.mediainfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tinymediamanager.core.MediaFileHelper.VIDEO_3D_HSBS;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.TmmOsUtils;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.entities.MediaStreamInfo;
import org.tinymediamanager.thirdparty.MediaInfo;

public class MediaInfoTest extends BasicTest {

  @Before
  public void setup() throws Exception {
    super.setup();

    TmmOsUtils.loadNativeLibs();
  }

  @Test
  public void testAudiofiles() throws Exception {
    copyResourceFolderToWorkFolder("samples");
    Path samplesFolder = getWorkFolder().resolve("samples");

    MediaFile mf = new MediaFile(samplesFolder.resolve("AAC-HE_LC_6ch.mka"));
    mf.gatherMediaInformation();
    assertThat(mf.getAudioCodec()).isEqualTo("AAC");
    assertThat(mf.getAudioChannels()).isEqualTo("6ch");

    mf = new MediaFile(samplesFolder.resolve("AAC-HE_LC_8ch.mka"));
    mf.gatherMediaInformation();
    assertThat(mf.getAudioCodec()).isEqualTo("AAC");
    assertThat(mf.getAudioChannels()).isEqualTo("8ch");

    mf = new MediaFile(samplesFolder.resolve("DTS-X.mka"));
    mf.gatherMediaInformation();
    assertThat(mf.getAudioCodec()).isEqualTo("DTS-X");
    assertThat(mf.getAudioChannels()).isEqualTo("8ch");
    assertThat(mf.getAudioLanguage()).isEqualTo("eng");

    mf = new MediaFile(samplesFolder.resolve("DTS-ES_Discrete.ts"));
    mf.gatherMediaInformation();
    assertThat(mf.getAudioCodec()).isEqualTo("DTS-ES");
    assertThat(mf.getAudioChannels()).isEqualTo("7ch");

    mf = new MediaFile(samplesFolder.resolve("DTSHD-HRA.mka"));
    mf.gatherMediaInformation();
    assertThat(mf.getAudioCodec()).isEqualTo("DTSHD-HRA");
    assertThat(mf.getAudioChannels()).isEqualTo("8ch");
    assertThat(mf.getAudioLanguage()).isEqualTo("eng");

    mf = new MediaFile(samplesFolder.resolve("DTSHD-MA.mka"));
    mf.gatherMediaInformation();
    assertThat(mf.getAudioCodec()).isEqualTo("DTSHD-MA");
    assertThat(mf.getAudioChannels()).isEqualTo("8ch");
    assertThat(mf.getAudioLanguage()).isEqualTo("eng");

    mf = new MediaFile(samplesFolder.resolve("DTS.mka"));
    mf.gatherMediaInformation();
    assertThat(mf.getAudioCodec()).isEqualTo("DTS");
    assertThat(mf.getAudioChannels()).isEqualTo("6ch");

    mf = new MediaFile(samplesFolder.resolve("TrueHD.mka"));
    mf.gatherMediaInformation();
    assertThat(mf.getAudioCodec()).isEqualTo("TrueHD");
    assertThat(mf.getAudioChannels()).isEqualTo("8ch");
    assertThat(mf.getAudioLanguage()).isEqualTo("eng");

    mf = new MediaFile(samplesFolder.resolve("TrueHD-Atmos.mka"));
    mf.gatherMediaInformation();
    assertThat(mf.getAudioCodec()).isEqualTo("TrueHD/Atmos");
    assertThat(mf.getAudioChannels()).isEqualTo("8ch");

    mf = new MediaFile(samplesFolder.resolve("EAC3-Atmos.mkv"));
    mf.gatherMediaInformation();
    assertThat(mf.getAudioCodec()).isEqualTo("EAC3/Atmos");
    assertThat(mf.getAudioChannels()).isEqualTo("8ch");

    mf = new MediaFile(samplesFolder.resolve("AC-3.mka"));
    mf.gatherMediaInformation();
    assertThat(mf.getAudioCodec()).isEqualTo("AC3");
    assertThat(mf.getAudioChannels()).isEqualTo("6ch");

    mf = new MediaFile(samplesFolder.resolve("PCM.mka"));
    mf.gatherMediaInformation();
    assertThat(mf.getAudioCodec()).isEqualTo("PCM");
    assertThat(mf.getAudioChannels()).isEqualTo("6ch");

    mf = new MediaFile(samplesFolder.resolve("E-AC3.ac3"));
    mf.gatherMediaInformation();
    assertThat(mf.getAudioCodec()).isEqualTo("EAC3");
    assertThat(mf.getAudioChannels()).isEqualTo("6ch");

    // audio file with -mediainfo.xml - will not be read anylonger!
    // mf = new MediaFile(samplesFolder.resolve("E-AC3-2.ac3"));
    // mf.gatherMediaInformation();
    // assertThat(mf.getAudioCodec()).isEqualTo("EAC3");
    // assertThat(mf.getAudioChannels()).isEqualTo("8ch");
  }

  @Test
  public void testIsoXmlOldFormat() throws Exception {
    copyResourceFolderToWorkFolder("testmovies/MediainfoXML");
    Path mediainfoXmlFolder = getWorkFolder().resolve("testmovies/MediainfoXML");

    // DVD ISO - old format
    MediaFile mf = new MediaFile(mediainfoXmlFolder.resolve("MediaInfo.0.7.99.iso"));
    mf.gatherMediaInformation();

    assertThat(mf.getVideoWidth()).isEqualTo(720);
    assertThat(mf.getVideoHeight()).isEqualTo(576);
    assertThat(mf.getVideoCodec()).isEqualTo("MPEG-2");
    assertThat(mf.getDuration()).isEqualTo(5160);

    assertThat(mf.getAudioStreams().size()).isEqualTo(8);
    // first audio stream is AC-3 english/5.1
    MediaFileAudioStream audioStream = mf.getAudioStreams().get(0);
    assertThat(audioStream.getAudioChannels()).isEqualTo(6);
    assertThat(audioStream.getCodec()).isEqualTo("AC3");
    assertThat(audioStream.getLanguage()).isEqualTo("eng");

    assertThat(mf.getSubtitles().size()).isEqualTo(32);
    MediaFileSubtitle subtitle = mf.getSubtitles().get(0);
    assertThat(subtitle.getLanguage()).isEqualTo("eng");
    assertThat(subtitle.getCodec()).isEqualTo("RLE");
  }

  @Test
  public void testIsoXmlNewFormat() throws Exception {
    copyResourceFolderToWorkFolder("testmovies/MediainfoXML");
    Path mediainfoXmlFolder = getWorkFolder().resolve("testmovies/MediainfoXML");

    // DVD ISO - new format
    MediaFile mf = new MediaFile(mediainfoXmlFolder.resolve("MediaInfo.17.10.iso"));
    mf.gatherMediaInformation();

    assertThat(mf.getVideoWidth()).isEqualTo(720);
    assertThat(mf.getVideoHeight()).isEqualTo(576);
    assertThat(mf.getVideoCodec()).isEqualTo("MPEG-2");
    assertThat(mf.getDuration()).isEqualTo(5184);

    assertThat(mf.getAudioStreams().size()).isEqualTo(8);
    // first audio stream is AC-3 english/5.1
    MediaFileAudioStream audioStream = mf.getAudioStreams().get(0);
    assertThat(audioStream.getAudioChannels()).isEqualTo(6);
    assertThat(audioStream.getCodec()).isEqualTo("AC3");
    assertThat(audioStream.getLanguage()).isEqualTo("eng");

    assertThat(mf.getSubtitles().size()).isEqualTo(32);
    MediaFileSubtitle subtitle = mf.getSubtitles().get(0);
    assertThat(subtitle.getLanguage()).isEqualTo("eng");
    assertThat(subtitle.getCodec()).isEqualTo("RLE");
  }

  @Test
  public void testBdIsoXml() throws Exception {
    copyResourceFolderToWorkFolder("testmovies/MediainfoXML");
    Path mediainfoXmlFolder = getWorkFolder().resolve("testmovies/MediainfoXML");

    // BD ISO
    MediaFile mf = new MediaFile(mediainfoXmlFolder.resolve("MediaInfo-BD.iso"));
    mf.gatherMediaInformation();

    assertThat(mf.getVideoWidth()).isEqualTo(1920);
    assertThat(mf.getVideoHeight()).isEqualTo(1080);
    assertThat(mf.getVideoCodec()).isEqualTo("h264");
    assertThat(mf.getDuration()).isEqualTo(888);

    assertThat(mf.getAudioStreams().size()).isEqualTo(1);
    // first audio stream is AC-3 english/5.1
    MediaFileAudioStream audioStream = mf.getAudioStreams().get(0);
    assertThat(audioStream.getAudioChannels()).isEqualTo(6);
    assertThat(audioStream.getCodec()).isEqualTo("AC3");
    assertThat(audioStream.getLanguage()).isEqualTo("eng");

    assertThat(mf.getSubtitles().size()).isEqualTo(10);
    MediaFileSubtitle subtitle = mf.getSubtitles().get(0);
    assertThat(subtitle.getLanguage()).isEqualTo("deu");
    assertThat(subtitle.getCodec()).isEqualTo("PGS");
  }

  @Test
  public void testBdIsoMplsXml() throws Exception {
    copyResourceFolderToWorkFolder("testmovies/MediainfoXML");
    Path mediainfoXmlFolder = getWorkFolder().resolve("testmovies/MediainfoXML");

    // BD-MPLS ISO
    MediaFile mf = new MediaFile(mediainfoXmlFolder.resolve("MediaInfo-BD-mpls.iso"));
    mf.gatherMediaInformation();

    assertThat(mf.getVideoWidth()).isEqualTo(1920);
    assertThat(mf.getVideoHeight()).isEqualTo(1080);
    assertThat(mf.getVideoCodec()).isEqualTo("h264");
    assertThat(mf.getDuration()).isEqualTo(5624);

    assertThat(mf.getAudioStreams().size()).isEqualTo(4);
    // first audio stream is AC-3 english/5.1
    MediaFileAudioStream audioStream = mf.getAudioStreams().get(0);
    assertThat(audioStream.getAudioChannels()).isEqualTo(8);
    assertThat(audioStream.getCodec()).isEqualTo("DTSHD-MA");
    assertThat(audioStream.getLanguage()).isEqualTo("eng");

    assertThat(mf.getSubtitles().size()).isEqualTo(7);
    MediaFileSubtitle subtitle = mf.getSubtitles().get(0);
    assertThat(subtitle.getLanguage()).isEqualTo("eng");
    assertThat(subtitle.getCodec()).isEqualTo("PGS");
  }

  @Test
  public void testBdIsoXmlNosize() throws Exception {
    copyResourceFolderToWorkFolder("testmovies/MediainfoXML");
    Path mediainfoXmlFolder = getWorkFolder().resolve("testmovies/MediainfoXML");

    // BD ISO without size in xml
    MediaFile mf = new MediaFile(mediainfoXmlFolder.resolve("MediaInfo-BD-nosize.iso"));
    mf.gatherMediaInformation();

    assertThat(mf.getVideoWidth()).isEqualTo(1920);
    assertThat(mf.getVideoHeight()).isEqualTo(1080);
    assertThat(mf.getVideoCodec()).isEqualTo("h264");
    assertThat(mf.getDuration()).isEqualTo(6626);

    assertThat(mf.getAudioStreams().size()).isEqualTo(1);
    // first audio stream is AC-3 english/5.1
    MediaFileAudioStream audioStream = mf.getAudioStreams().get(0);
    assertThat(audioStream.getAudioChannels()).isEqualTo(6);
    assertThat(audioStream.getCodec()).isEqualTo("DTSHD-MA");
    assertThat(audioStream.getLanguage()).isEqualTo("deu");

    assertThat(mf.getSubtitles().size()).isEqualTo(3);
    MediaFileSubtitle subtitle = mf.getSubtitles().get(0);
    assertThat(subtitle.getLanguage()).isEqualTo("deu");
    assertThat(subtitle.getCodec()).isEqualTo("PGS");
  }

  @Test
  public void testCdIsoXml() throws Exception {
    copyResourceFolderToWorkFolder("testmovies/MediainfoXML");
    Path mediainfoXmlFolder = getWorkFolder().resolve("testmovies/MediainfoXML");

    // CD ISO
    MediaFile mf = new MediaFile(mediainfoXmlFolder.resolve("MediaInfo-CD.iso"));
    mf.gatherMediaInformation();

    assertThat(mf.getVideoWidth()).isEqualTo(720);
    assertThat(mf.getVideoHeight()).isEqualTo(576);
    assertThat(mf.getVideoCodec()).isEqualTo("MPEG-2");
    assertThat(mf.getDuration()).isEqualTo(6627);

    assertThat(mf.getAudioStreams().size()).isEqualTo(1);
    // first audio stream is AC-3 english/5.1
    MediaFileAudioStream audioStream = mf.getAudioStreams().get(0);
    assertThat(audioStream.getAudioChannels()).isEqualTo(6);
    assertThat(audioStream.getCodec()).isEqualTo("AC3");
    assertThat(audioStream.getLanguage()).isEqualTo("deu");

    assertThat(mf.getSubtitles().size()).isEqualTo(0);
  }

  @Test
  public void testCdIsoXmlWoLanguage() throws Exception {
    copyResourceFolderToWorkFolder("testmovies/MediainfoXML");
    Path mediainfoXmlFolder = getWorkFolder().resolve("testmovies/MediainfoXML");

    // CD ISO without language information
    MediaFile mf = new MediaFile(mediainfoXmlFolder.resolve("MediaInfo-CD-nolang.iso"));
    mf.gatherMediaInformation();

    assertThat(mf.getVideoWidth()).isEqualTo(720);
    assertThat(mf.getVideoHeight()).isEqualTo(480);
    assertThat(mf.getVideoCodec()).isEqualTo("MPEG-2");
    assertThat(mf.getDuration()).isEqualTo(120);

    assertThat(mf.getAudioStreams().size()).isEqualTo(1);
    // first audio stream is AC-3 english/5.1
    MediaFileAudioStream audioStream = mf.getAudioStreams().get(0);
    assertThat(audioStream.getAudioChannels()).isEqualTo(2);
    assertThat(audioStream.getCodec()).isEqualTo("MP2");
    assertThat(audioStream.getLanguage()).isEmpty();

    assertThat(mf.getSubtitles().size()).isEqualTo(0);
  }

  @Test
  public void testMkvXml() throws Exception {
    copyResourceFolderToWorkFolder("testmovies/MediainfoXML");
    Path mediainfoXmlFolder = getWorkFolder().resolve("testmovies/MediainfoXML");

    // MKV FILE
    MediaFile mf = new MediaFile(mediainfoXmlFolder.resolve("MediaInfoMKV.mkv"));
    mf.gatherMediaInformation();

    assertThat(mf.getVideoWidth()).isEqualTo(720);
    assertThat(mf.getVideoHeight()).isEqualTo(302);
    assertThat(mf.getVideoCodec()).isEqualTo("h264");
    assertThat(mf.getDuration()).isEqualTo(6412);

    assertThat(mf.getAudioStreams().size()).isEqualTo(1);
    // first audio stream is AC-3 english/5.1
    MediaFileAudioStream audioStream = mf.getAudioStreams().get(0);
    assertThat(audioStream.getAudioChannels()).isEqualTo(6);
    assertThat(audioStream.getCodec()).isEqualTo("AC3");
    assertThat(audioStream.getLanguage()).isEqualTo("deu");

    assertThat(mf.getSubtitles().size()).isEqualTo(0);
  }

  @Test
  public void testBdXml2003() throws Exception {
    copyResourceFolderToWorkFolder("testmovies/MediainfoXML");
    Path mediainfoXmlFolder = getWorkFolder().resolve("testmovies/MediainfoXML");

    MediaFile mf = new MediaFile(mediainfoXmlFolder.resolve("MediaInfo.BD.20.03.iso"));
    mf.gatherMediaInformation();

    assertThat(mf.getVideoWidth()).isEqualTo(3840);
    assertThat(mf.getVideoHeight()).isEqualTo(2160);
    assertThat(mf.getVideoCodec()).isEqualTo("h265");
    assertThat(mf.getDuration()).isEqualTo(7439);

    assertThat(mf.getAudioStreams().size()).isEqualTo(3);
    // first audio stream is DTSHD-MA english/7.1
    MediaFileAudioStream audioStream = mf.getAudioStreams().get(0);
    assertThat(audioStream.getAudioChannels()).isEqualTo(8);
    assertThat(audioStream.getCodec()).isEqualTo("DTSHD-MA");
    assertThat(audioStream.getLanguage()).isEqualTo("eng");
    assertThat(audioStream.getBitrate()).isEqualTo(5847);

    assertThat(mf.getSubtitles().size()).isEqualTo(3);
    // first subtitle stream is en
    MediaFileSubtitle subtitle = mf.getSubtitles().get(0);
    assertThat(subtitle.getCodec()).isEqualTo("PGS");
    assertThat(subtitle.getLanguage()).isEqualTo("eng");
  }

  // @Test
  // public void displayVersion() {
  // System.out.println(MediaInfo.staticOption("Info_Version"));
  // }
  //
  // /**
  // * displays all known parameters you could fetch
  // */
  // @Test
  // public void displayInfoParameters() {
  // System.out.println(MediaInfo.staticOption("Info_Parameters"));
  // }
  //
  // @Test
  // public void displayInfoCapacities() {
  // System.out.println(MediaInfo.staticOption("Info_Capacities"));
  // }
  //
  // @Test
  // public void displayInfoOutputFormats() {
  // // since version 17.10
  // System.out.println(MediaInfo.staticOption("Info_OutputFormats"));
  // }
  //
  // /**
  // * displays all supported codecs
  // */
  // @Test
  // public void displayInfoCodecs() {
  // System.out.println(MediaInfo.staticOption("Info_Codecs"));
  // }

  @Test
  public void mediaFile() throws Exception {
    setTraceLogging();

    copyResourceFolderToWorkFolder("testmovies/MediainfoXML");
    Path mediainfoXmlFolder = getWorkFolder().resolve("testmovies/MediainfoXML");

    MediaFile mf = new MediaFile(mediainfoXmlFolder.resolve("tags.mkv"));
    mf.gatherMediaInformation();

    System.out.println("----------------------");
    System.out.println("filesize: " + mf.getFilesize());
    System.out.println("filedate: " + new Date(mf.getFiledate()));
    System.out.println("container: " + mf.getContainerFormat());
    System.out.println("runtime: " + mf.getDurationHHMMSS());

    System.out.println("----------------------");
    System.out.println("vres: " + mf.getVideoResolution());
    System.out.println("vwidth: " + mf.getVideoWidth());
    System.out.println("vheight: " + mf.getVideoHeight());
    System.out.println("vformat: " + mf.getVideoFormat());
    System.out.println("vid: " + mf.getExactVideoFormat());
    System.out.println("vcodec: " + mf.getVideoCodec());
    System.out.println("vdef: " + mf.getVideoDefinitionCategory());
    System.out.println("FPS: " + mf.getFrameRate());
    System.out.println("aspect: " + mf.getAspectRatio());
    System.out.println("ws?: " + mf.isWidescreen());
    System.out.println("hdr?: " + mf.getHdrFormat());

    System.out.println("----------------------");
    System.out.println("acodec: " + mf.getAudioCodec());
    System.out.println("alang: " + mf.getAudioLanguage());
    System.out.println("achan: " + mf.getAudioChannels());

    System.out.println("----------------------");
    System.out.println("subs: " + mf.getSubtitlesAsString());
    System.out.println("extra: " + mf.getExtraData());
  }

  /**
   * mediainfo direct example
   */
  @Test
  public void testDirect() throws Exception {
    copyResourceFolderToWorkFolder("samples");
    Path samplesFolder = getWorkFolder().resolve("samples");

    String FileName = samplesFolder.resolve("DTS-X.mka").toString();
    String To_Display = "";

    // Info about the library

    MediaInfo MI = new MediaInfo();

    To_Display += "\r\n\r\nOpen\r\n";
    if (MI.open(Paths.get(FileName)))
      To_Display += "is OK\r\n";
    else
      To_Display += "has a problem\r\n";

    To_Display += "\r\n\r\nInform with Complete=false\r\n";
    MI.option("Complete", "");
    To_Display += MI.inform();

    To_Display += "\r\n\r\nInform with Complete=true\r\n";
    MI.option("Complete", "1");
    To_Display += MI.inform();

    To_Display += "\r\n\r\nCustom Inform\r\n";
    MI.option("Inform", "General;Example : FileSize=%FileSize%");
    To_Display += MI.inform();

    To_Display += "\r\n\r\nGetI with Stream=General and Parameter=2\r\n";
    To_Display += MI.get(MediaInfo.StreamKind.General, 0, 2, MediaInfo.InfoKind.Text);

    To_Display += "\r\n\r\nCount_Get with StreamKind=Stream_Audio\r\n";
    To_Display += MI.parameterCount(MediaInfo.StreamKind.Audio, -1);

    To_Display += "\r\n\r\nGet with Stream=General and Parameter=\"AudioCount\"\r\n";
    To_Display += MI.get(MediaInfo.StreamKind.General, 0, "AudioCount", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);

    To_Display += "\r\n\r\nGet with Stream=Audio and Parameter=\"StreamCount\"\r\n";
    To_Display += MI.get(MediaInfo.StreamKind.Audio, 0, "StreamCount", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);

    To_Display += "\r\n\r\nGet with Stream=General and Parameter=\"FileSize\"\r\n";
    To_Display += MI.get(MediaInfo.StreamKind.General, 0, "FileSize", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);

    To_Display += "\r\n\r\nGet with Stream=General and Parameter=\"File_Modified_Date_Local\"\r\n";
    To_Display += MI.get(MediaInfo.StreamKind.General, 0, "File_Modified_Date_Local", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);

    To_Display += "\r\n\r\nClose\r\n";
    MI.close();

    System.out.println(To_Display);
  }

  @Test
  public void testLanguageDetectionFromBasename() {
    // no extension - just basename!!!
    check("moviename-cc-my title", "", "my title");// cc is no language
    check("moviename-cc-forced-my title", "", "my title");// cc is no language
    check("moviename-hi-cc-forced-my title", "hi", "my title");
    check("moviename-cc-hi-forced-my title", "hi", "my title"); // cc is no language - here NO flag!
    check("moviename-eng-hi-cc-forced-my title", "eng", "my title");
    check("moviename-eng-cc-hi-forced-my title", "eng", "my title"); // cc is no language - here IS flag!
    check("moviename.pt-BR", "pt-BR");
    check("moviename.zh_hAnS", "zh_hAnS");
    check("moviename.zh-HaNt", "zh-HaNt");
    check("en", "en");
    check("eng_sdh", "eng");
    check("moviename.en", "en");
    check("moviename.en.forced", "en");
    check("moviename.eng", "eng");
    check("moviename.german", "german");
    check("moviename.Deutsch", "deutsch");
    check("moviename.eng_hi", "eng");
    check("moviename.eng_sdh", "eng");
    check("moviename eng_sdh", "eng");
    check("movie.name.year.GERMAN.dTV.XViD", "german", "dTV XViD");
    check("movietitle.year.NLPS.XviD.DTS.3CD-WAF.German.waf.com.cn.hk", "german", "waf com cn hk");
    check("movie.name.year.pt-br", "pt-br");
    check("moviename.german-director", "german", "director");
    check("moviename.english-director", "english", "director");
    check("moviename", "");
    check("movie name (2023) pt-br subtitle title forced", "pt-br", "subtitle title");
    check("movie name (2023) pt-br forced subtitle title sdh ", "pt-br", "subtitle title");
  }

  private void check(String basename, String expectedLanguage) {
    check(basename, expectedLanguage, "");
  }

  private void check(String basename, String expectedLanguage, String expectedTitle) {
    MediaStreamInfo info = MediaFileHelper.gatherLanguageInformation(basename, "");
    assertThat(info.getLanguage()).isEqualTo(expectedLanguage);
    assertThat(info.getTitle()).isEqualTo(expectedTitle);
  }

  @Test
  public void testSubtitleLanguageDetection() throws Exception {
    copyResourceFolderToWorkFolder("subtitles");

    compareSubtitle("en.srt", "en");
    compareSubtitle("eng_sdh.srt", "eng");
    compareSubtitle("moviename.en.srt", "en");
    compareSubtitle("moviename.en.forced.srt", "en");
    compareSubtitle("moviename.eng.srt", "eng");
    compareSubtitle("moviename.german.srt", "german");
    compareSubtitle("moviename.Deutsch.srt", "deutsch");
    compareSubtitle("moviename.eng_hi.srt", "eng");
    compareSubtitle("moviename.eng_sdh.srt", "eng");
    compareSubtitle("movie.name.year.pt-br.srt", "pt-br");
    compareSubtitle("moviename.srt", "");
    compareSubtitle("multi.not.eng.sub", "de");

    compareSubtitle("movie.name.year.GERMAN.dTV.XViD.srt", "movie.name.year.GERMAN.dTV.XViD", "", "");
    compareSubtitle("movietitle.year.NLPS.XviD.DTS.3CD-WAF.German.waf.com.cn.hk.srt", "movietitle.year.NLPS.XviD.DTS.3CD-WAF", "german",
        "waf com cn hk"); // shit in, shit out
    compareSubtitle("moviename.german-director.srt", "moviename", "german", "director");
    compareSubtitle("moviename.english-director.srt", "moviename", "english", "director");
  }

  private void compareSubtitle(String filename, String expectedLanguage) throws Exception {
    compareSubtitle(filename, "", expectedLanguage, "");
  }

  private void compareSubtitle(String filename, String videoBasename, String expectedLanguage, String expectedTitle) throws Exception {
    Path subtitlesFolder = getWorkFolder().resolve("subtitles");

    MediaFile mf = new MediaFile(subtitlesFolder.resolve(filename));
    mf.gatherMediaInformation();

    assertThat(mf.getType()).isEqualTo(MediaFileType.SUBTITLE);
    assertThat(mf.getSubtitles()).isNotEmpty();

    // since the language collection part was removed from getMI (bc of video basename)
    // we need to readd it here
    MediaStreamInfo info = MediaFileHelper.gatherLanguageInformation(mf.getBasename(), videoBasename);
    if (mf.getSubtitles().get(0).getLanguage().isEmpty()) {
      assertThat(info.getLanguage()).isEqualTo(expectedLanguage);
    }
    else {
      assertThat(mf.getSubtitles().get(0).getLanguage()).isEqualTo(expectedLanguage);
    }

    if (mf.getSubtitles().get(0).getTitle().isEmpty()) {
      assertThat(info.getTitle()).isEqualTo(expectedTitle);
    }
    else {
      assertThat(mf.getSubtitles().get(0).getLanguage()).isEqualTo(expectedLanguage);
    }
  }

  // @Test
  // public void listDirectForAll() {
  // MediaInfo mi = new MediaInfo();
  //
  // DirectoryStream<Path> stream = null;
  // try {
  // stream = Files.newDirectoryStream(Paths.get("src/test/resources/samples"));
  // for (Path path : stream) {
  // if (mi.open(path)) {
  //
  // // https://github.com/MediaArea/MediaInfoLib/blob/master/Source/Resource/Text/Stream/Audio.csv
  // // Format;;;N YTY;;;Format used;;
  // // Format/String;;;Y NT;;;Format used + additional features
  // // Format/Info;;;Y NT;;;Info about the format;;
  // // Format_Commercial;;;N YT;;;Commercial name used by vendor for theses setings or Format field if there is no difference;;
  // // Format_Profile;;;Y YTY;;;Profile of the Format (old XML: 'Profile@Level' format; MIXML: 'Profile' only)
  // // Format_AdditionalFeatures;;;N YTY;;;Format features needed for fully supporting the content
  //
  // String ret = path + "\n";
  // ret += mi.get(MediaInfo.StreamKind.Audio, 0, "Format") + "\n";
  // ret += mi.get(MediaInfo.StreamKind.Audio, 0, "Format/String") + "\n";
  // ret += mi.get(MediaInfo.StreamKind.Audio, 0, "Format/Info") + "\n";
  // ret += mi.get(MediaInfo.StreamKind.Audio, 0, "Format_Commercial") + "\n";
  // ret += mi.get(MediaInfo.StreamKind.Audio, 0, "Format_Profile") + "\n";
  // ret += mi.get(MediaInfo.StreamKind.Audio, 0, "Format_AdditionalFeatures") + "\n";
  //
  // System.out.println(ret);
  // mi.close();
  // }
  // }
  // }
  // catch (Exception e) {
  // // TODO: handle exception
  // }
  // }

  @Test
  public void testHdrDetection() throws Exception {
    copyResourceFolderToWorkFolder("mediainfo");
    Path mediainfoFolder = getWorkFolder().resolve("mediainfo");
    MediaFile mf = null;

    // DV+HDR
    mf = new MediaFile(mediainfoFolder.resolve("DV+HDR (2086).mp4"));
    MediaFileHelper.gatherMediaInformation(mf, false);
    assertThat(mf.getHdrFormat()).isEqualTo("Dolby Vision, HDR10");

    // DV+HLG
    mf = new MediaFile(mediainfoFolder.resolve("DV+HLG.mov"));
    MediaFileHelper.gatherMediaInformation(mf, false);
    assertThat(mf.getHdrFormat()).isEqualTo("Dolby Vision, HLG");

    // HDR
    mf = new MediaFile(mediainfoFolder.resolve("HDR (2086).mkv"));
    MediaFileHelper.gatherMediaInformation(mf, false);
    assertThat(mf.getHdrFormat()).isEqualTo("HDR10");

    // HDR10+
    mf = new MediaFile(mediainfoFolder.resolve("HDR10+ (2094).mkv"));
    MediaFileHelper.gatherMediaInformation(mf, false);
    assertThat(mf.getHdrFormat()).isEqualTo("HDR10+");

    // Dolby Vision
    mf = new MediaFile(mediainfoFolder.resolve("dolby_vision.avi"));
    MediaFileHelper.gatherMediaInformation(mf, false);
    assertThat(mf.getHdrFormat()).isEqualTo("Dolby Vision, HDR10+");

    mf = new MediaFile(mediainfoFolder.resolve("dolby_vision2.avi"));
    MediaFileHelper.gatherMediaInformation(mf, false);
    assertThat(mf.getHdrFormat()).isEqualTo("Dolby Vision, HDR10+");

    // HDR10
    mf = new MediaFile(mediainfoFolder.resolve("hdr10.avi"));
    MediaFileHelper.gatherMediaInformation(mf, false);
    assertThat(mf.getHdrFormat()).isEqualTo("HDR10");

    mf = new MediaFile(mediainfoFolder.resolve("hdr10-2.avi"));
    MediaFileHelper.gatherMediaInformation(mf, false);
    assertThat(mf.getHdrFormat()).isEqualTo("HDR10");

    // HDR10+
    mf = new MediaFile(mediainfoFolder.resolve("hdr10plus.avi"));
    MediaFileHelper.gatherMediaInformation(mf, false);
    assertThat(mf.getHdrFormat()).isEqualTo("HDR10+");

    // HLG
    mf = new MediaFile(mediainfoFolder.resolve("hlg.avi"));
    MediaFileHelper.gatherMediaInformation(mf, false);
    assertThat(mf.getHdrFormat()).isEqualTo("HLG");
  }

  @Test
  public void test3d() throws Exception {
    copyResourceFolderToWorkFolder("mediainfo");
    Path mediainfoFolder = getWorkFolder().resolve("mediainfo");

    // Dolby Vision
    MediaFile mf = new MediaFile(mediainfoFolder.resolve("3d_sbs.avi"));
    MediaFileHelper.gatherMediaInformation(mf, false);

    assertThat(mf.getVideo3DFormat()).isEqualTo(VIDEO_3D_HSBS);
    assertThat(mf.getVideoWidth()).isEqualTo(1920);
    assertThat(mf.getVideoHeight()).isEqualTo(800);
  }

  @Test
  public void testSubtitleTitle() throws Exception {
    copyResourceFolderToWorkFolder("mediainfo");
    Path mediainfoFolder = getWorkFolder().resolve("mediainfo");

    // Dolby Vision
    MediaFile mf = new MediaFile(mediainfoFolder.resolve("subtitle-title.avi"));
    MediaFileHelper.gatherMediaInformation(mf, false);
    assertThat(mf.getSubtitles().size()).isEqualTo(2);

    MediaFileSubtitle sub1 = mf.getSubtitles().get(0);
    assertThat(sub1.getTitle()).isEqualTo("That's a DVDSUB subtitle");

    MediaFileSubtitle sub2 = mf.getSubtitles().get(1);
    assertThat(sub2.getTitle()).isEqualTo("and this a simple srt subtitle");
  }
}
