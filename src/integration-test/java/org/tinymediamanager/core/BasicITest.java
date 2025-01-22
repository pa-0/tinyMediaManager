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

package org.tinymediamanager.core;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieEdition;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.license.License;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public abstract class BasicITest {
  private static final String LOREM     = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent vel lacus libero. Ut vel lacus erat. Maecenas maximus vestibulum ante at efficitur. Sed id ex eget purus commodo feugiat. Suspendisse ultricies felis sed interdum luctus. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Nunc et scelerisque nibh. Donec maximus nunc nunc, non commodo nulla rhoncus id. Curabitur pharetra maximus tellus non porta. Ut vehicula elit nec ante elementum, ut semper ligula consectetur.";

  @Rule
  public TemporaryFolder      tmpFolder = new TemporaryFolder();

  private Path                settingsFolder;
  private Path                workFolder;

  @Before
  public void setup() throws Exception {
    settingsFolder = tmpFolder.newFolder("data").toPath();
    workFolder = tmpFolder.newFolder("work").toPath();

    Settings.getInstance(settingsFolder.toString());

    try {
      setLicenseKey();
    }
    catch (Exception e) {
      System.out.println("COULD NOT SET LICENSE KEY");
    }
  }

  private void setLicenseKey() throws Exception {
    String key = "";
    // take the env variable for the license key (offered in CI)
    if (StringUtils.isNotBlank(System.getenv("TMM_BUILD_LICENSE"))) {
      key = System.getenv("TMM_BUILD_LICENSE");
    }
    else {
      // try to read it from a local file
      try {
        key = Utils.readFileToString(Paths.get("target/test-classes/tmm_build_license"));
      }
      catch (Exception e) {
        // just not crashing
      }
    }

    if (StringUtils.isNotBlank(key)) {
      License.getInstance().init2121();
      License.getInstance().setLicenseCode(key);
    }
  }

  protected Path getSettingsFolder() {
    return settingsFolder;
  }

  protected Path getWorkFolder() {
    return workFolder;
  }

  // own method to get some logging ;)
  protected void assertEqual(Object expected, Object actual) {
    try {
      Assert.assertEquals(expected, actual);
      // System.out.println(actual + " - passed");
    }
    catch (AssertionError e) {
      System.err.println(actual + " - FAILED: " + e.getMessage());
      throw e;
    }
  }

  protected void assertEqual(String message, Object expected, Object actual) {
    try {
      Assert.assertEquals(message, expected, actual);
      // System.out.println(expected + " - passed");
    }
    catch (AssertionError e) {
      System.err.println(expected + " - FAILED: " + message + "(" + e.getMessage() + ")");
      throw e;
    }
  }

  protected void setTraceLogging() {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    lc.getLogger("org.tinymediamanager").setLevel(Level.TRACE);
  }

  protected Date getRandomDate() {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    cal.set(1950, 1, 1, 0, 0, 0);
    Date startInclusive = cal.getTime();
    Date endExclusive = Calendar.getInstance().getTime();
    long startMillis = startInclusive.getTime();
    long endMillis = endExclusive.getTime();
    long randomMillisSinceEpoch = ThreadLocalRandom.current().nextLong(startMillis, endMillis);
    return new Date(randomMillisSinceEpoch);
  }

  protected void createFakeMovie(String title) {
    Movie movie = new Movie();
    movie.setDbId(getUUID(title)); // fixate

    Path path = workFolder.resolve(title);

    movie.setDataSource(workFolder.toString());
    movie.setTitle(title);
    movie.setPath(path.toString());
    movie.setOriginalTitle("Original " + title);
    movie.setSortTitle(title);
    movie.setRating(new MediaRating(MediaRating.NFO, (float) ThreadLocalRandom.current().nextDouble(1.0d, 9.0d), 5987, 10));
    movie.setYear(1950 + ThreadLocalRandom.current().nextInt(20, 60));
    movie.setTop250(ThreadLocalRandom.current().nextInt(250));
    movie.setPlot(LOREM);
    movie.setTagline("Wish granted");
    movie.setRuntime(70 + ThreadLocalRandom.current().nextInt(20, 50));
    movie.setImdbId("tt0103639");
    movie.setTmdbId(ThreadLocalRandom.current().nextInt(1000, 5000));
    movie.setId("trakt", ThreadLocalRandom.current().nextInt(1000, 5000));
    movie.setProductionCompany("Walt Disney");
    movie.setCountry("US");
    movie.setCertification(MediaCertification.US_G);
    movie.setReleaseDate(getRandomDate());

    MediaTrailer trailer = new MediaTrailer();
    trailer.setUrl("https://trailer");
    trailer.setInNfo(true);
    movie.addToTrailer(Collections.singletonList(trailer));

    MovieSet movieSet = new MovieSet();
    movieSet.setTitle(title + " Collection");
    movieSet.setPlot(title + " plot");
    movie.setMovieSet(movieSet);

    // MF video
    MediaFile mf = new MediaFile();
    mf.setType(MediaFileType.VIDEO);
    mf.setPath(path.toString());
    mf.setFilename(title + ".mkv");
    mf.setVideoCodec("h264");
    mf.setVideoHeight(720);
    mf.setVideoWidth(1280);
    mf.setDuration(3600);
    mf.setOverallBitRate(3500);
    mf.setVideo3DFormat(MediaFileHelper.VIDEO_3D_SBS);

    MediaFileAudioStream audio = new MediaFileAudioStream();
    audio.setCodec("AC3");
    audio.setLanguage("en");
    audio.setAudioChannels(6);
    mf.setAudioStreams(Collections.singletonList(audio));

    MediaFileSubtitle sub = new MediaFileSubtitle();
    sub.setLanguage("de");
    mf.addSubtitle(sub);
    movie.addToMediaFiles(mf);

    // MF poster
    mf = new MediaFile(path.resolve("dummy-poster.jpg"));
    movie.addToMediaFiles(mf);
    // MF fanart
    mf = new MediaFile(path.resolve("dummy-fanart.jpg"));
    movie.addToMediaFiles(mf);

    movie.setWatched(true);
    movie.addToGenres(Arrays.asList(MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)],
        MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)],
        MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)],
        MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)]));
    movie
        .addToWriters(Arrays.asList(new Person(Person.Type.WRITER, "Ted Elliott", "Writer"), new Person(Person.Type.WRITER, "Terry Rossio", "Writer"),
            new Person(Person.Type.WRITER, "Ron Clements", "Writer"), new Person(Person.Type.WRITER, "John Jusker", "Writer"),
            new Person(Person.Type.DIRECTOR, "Ron Clements", "Director"), new Person(Person.Type.DIRECTOR, "John Jusker", "Director")));
    movie.addToTags(Arrays.asList("Disney", "Oriental"));

    movie.addToActors(Arrays.asList(new Person(Person.Type.ACTOR, "Scott Weinger", "Aladdin 'Al' (voice)"),
        new Person(Person.Type.ACTOR, "Robin Williams", "Genie (voice)")));

    movie.addToProducers(
        Arrays.asList(new Person(Person.Type.PRODUCER, "Ron Clements", "Producer"), new Person(Person.Type.PRODUCER, "Donald W. Ernst", "Producer")));

    movie.setSpokenLanguages("en");
    movie.setMediaSource(MediaSource.values()[new Random().nextInt(MediaSource.values().length)]);
    movie.setEdition(MovieEdition.values()[new Random().nextInt(MovieEdition.values().length)]);

    MovieModuleManager.getInstance().getMovieList().addMovie(movie);
    movie.saveToDb();
    System.out.println("Created movie " + movie.getDbId());
  }

  protected void createFakeShow(String title) {
    TvShow tvShow = new TvShow();
    tvShow.setDbId(getUUID(title)); // fixate

    Path path = workFolder.resolve(title);

    tvShow.setTitle(title);
    tvShow.setPath(path.toString());
    tvShow.setYear(1950 + ThreadLocalRandom.current().nextInt(20, 60));
    tvShow.setRating(new MediaRating(MediaRating.NFO, (float) ThreadLocalRandom.current().nextDouble(1.0d, 9.0d), 5987, 10));
    tvShow.setCertification(MediaCertification.US_TVPG);
    tvShow.addToGenres(Arrays.asList(MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)],
        MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)],
        MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)],
        MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)]));
    tvShow.setTvdbId(String.valueOf(ThreadLocalRandom.current().nextInt(1000, 5000)));
    tvShow.setFirstAired(getRandomDate());
    tvShow.setProductionCompany("FOX (US)");
    tvShow.setPlot(LOREM);

    // show MF poster
    MediaFile mf = new MediaFile(path.resolve("dummy-poster.jpg"));
    tvShow.addToMediaFiles(mf);
    // show MF fanart
    mf = new MediaFile(path.resolve("dummy-fanart.jpg"));
    tvShow.addToMediaFiles(mf);

    // ========= EPISODE start =========
    TvShowEpisode episode = new TvShowEpisode();
    episode.setTvShow(tvShow);
    episode.setTitle(title + "-EP");
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 2));
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_DVD, 3, 4));
    episode.setTitle("Don't Pet the Teacher");
    episode.setYear(1950 + ThreadLocalRandom.current().nextInt(20, 60));
    episode.setFirstAired(getRandomDate());
    episode.setMediaSource(MediaSource.BLURAY);
    episode.setPlot(LOREM);

    mf = new MediaFile();
    mf.setType(MediaFileType.VIDEO);
    mf.setPath(path.toString());
    mf.setFilename(title + ".mkv");
    mf.setVideoCodec("h264");
    mf.setVideoHeight(720);
    mf.setVideoWidth(1280);
    mf.setDuration(70 + ThreadLocalRandom.current().nextInt(20, 60));
    mf.setOverallBitRate(3500);
    mf.setVideo3DFormat(MediaFileHelper.VIDEO_3D_SBS);

    MediaFileAudioStream audio = new MediaFileAudioStream();
    audio.setCodec("AC3");
    audio.setLanguage("en");
    audio.setAudioChannels(6);
    mf.setAudioStreams(Collections.singletonList(audio));

    MediaFileSubtitle sub = new MediaFileSubtitle();
    sub.setLanguage("de");
    mf.addSubtitle(sub);

    episode.addToMediaFiles(mf);

    // EP MF poster
    mf = new MediaFile(path.resolve("dummy-poster.jpg"), MediaFileType.THUMB);
    episode.addToMediaFiles(mf);
    episode.saveToDb();

    tvShow.addEpisode(episode);
    // ========= EPISODE end =========

    TvShowModuleManager.getInstance().getTvShowList().addTvShow(tvShow);
    tvShow.saveToDb();
    System.out.println("Created show " + tvShow.getDbId());
  }

  public static UUID getUUID(String uuid) {
    return UUID.nameUUIDFromBytes(uuid.getBytes());
  }
}
