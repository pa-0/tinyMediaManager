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

package org.tinymediamanager.core.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tinymediamanager.core.jmte.JmteUtils.morphTemplate;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.jmte.JmteUtils;
import org.tinymediamanager.core.jmte.NamedArrayRenderer;
import org.tinymediamanager.core.jmte.NamedArrayUniqueRenderer;
import org.tinymediamanager.core.jmte.NamedBitrateRenderer;
import org.tinymediamanager.core.jmte.NamedDateRenderer;
import org.tinymediamanager.core.jmte.NamedFilesizeRenderer;
import org.tinymediamanager.core.jmte.NamedFramerateRenderer;
import org.tinymediamanager.core.jmte.NamedLowerCaseRenderer;
import org.tinymediamanager.core.jmte.NamedNumberRenderer;
import org.tinymediamanager.core.jmte.NamedReplacementRenderer;
import org.tinymediamanager.core.jmte.NamedSplitRenderer;
import org.tinymediamanager.core.jmte.NamedTitleCaseRenderer;
import org.tinymediamanager.core.jmte.NamedUpperCaseRenderer;
import org.tinymediamanager.core.jmte.TmmModelAdaptor;
import org.tinymediamanager.core.jmte.TmmOutputAppender;
import org.tinymediamanager.core.jmte.ZeroNumberRenderer;
import org.tinymediamanager.core.movie.MovieRenamer.MovieNamedFirstCharacterRenderer;
import org.tinymediamanager.core.movie.MovieRenamer.MovieNamedIndexOfMovieSetRenderer;
import org.tinymediamanager.core.movie.MovieRenamer.MovieNamedIndexOfMovieSetWithDummyRenderer;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.scraper.entities.MediaCertification;

import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.extended.ChainedNamedRenderer;

public class MovieJmteTests extends BasicMovieTest {

  private Engine              engine;
  private Map<String, Object> root;

  @Test
  public void testMoviePatterns() {
    try {
      Movie movie = createMovie();

      engine = Engine.createEngine();
      engine.registerRenderer(Number.class, new ZeroNumberRenderer());
      engine.registerNamedRenderer(new MovieNamedFirstCharacterRenderer());
      engine.registerNamedRenderer(new MovieNamedIndexOfMovieSetRenderer());
      engine.registerNamedRenderer(new MovieNamedIndexOfMovieSetWithDummyRenderer());
      engine.registerNamedRenderer(new NamedArrayRenderer());
      engine.registerNamedRenderer(new NamedArrayUniqueRenderer());
      engine.registerNamedRenderer(new NamedBitrateRenderer());
      engine.registerNamedRenderer(new NamedDateRenderer());
      engine.registerNamedRenderer(new NamedFilesizeRenderer());
      engine.registerNamedRenderer(new NamedFramerateRenderer());
      engine.registerNamedRenderer(new NamedLowerCaseRenderer());
      engine.registerNamedRenderer(new NamedNumberRenderer());
      engine.registerNamedRenderer(new NamedReplacementRenderer());
      engine.registerNamedRenderer(new NamedSplitRenderer());
      engine.registerNamedRenderer(new NamedTitleCaseRenderer());
      engine.registerNamedRenderer(new NamedUpperCaseRenderer());
      engine.registerNamedRenderer(new ChainedNamedRenderer(engine.getAllNamedRenderers()));

      engine.setModelAdaptor(new TmmModelAdaptor());
      engine.setOutputAppender(new TmmOutputAppender() {
        @Override
        protected String replaceInvalidCharacters(String text) {
          return MovieRenamer.replaceInvalidCharacters(text);
        }
      });

      root = new HashMap<>();
      root.put("movie", movie);
      root.put("movieSet", movie.getMovieSet());

      // test single tokens
      compare("${title}", "Aladdin");
      compare("${title[0]}", "A");
      compare("${title;first}", "A");
      compare("${title[0,2]}", "Al");
      compare("${originalTitle}", "Disneys Aladdin");
      compare("${sortTitle}", "Aladdin");
      compare("${year}", "1992");
      compare("${releaseDate}", "1992-11-25");
      // compare("${rating}", "7.2"); // will not work unless you have the right settings
      compare("${movieSet.title}", "Aladdin Collection");
      compare("${movieSet.title[0]}", "A");
      compare("${movieSet.titleSortable}", "Aladdin Collection");
      compare("${titleSortable}", "Aladdin");
      compare("${imdb}", "tt0103639");
      compare("${certification}", "G");
      compare("${language}", "en");

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
      compare("${edition}", "Director's Cut");
      compare("${edition.name}", "DIRECTORS_CUT");
      compare("${3Dformat}", "3D SBS");

      compare("${genres[0].name}", "Adventure");
      compare("${tags[0]}", "Disney");
      compare("${actors[0].name}", "Scott Weinger");
      compare("${producers[0].name}", "Ron Clements");
      compare("${directors[0].name}", "Ron Clements");
      compare("${writers[0].name}", "Ted Elliott");

      // test raw movie access
      compare("${movie.top250}", "199");
      compare("${movie.videoFiles[0].overallBitRate}", "3500");

      // test combined tokens
      compare("${title} (${year})", "Aladdin (1992)");
      compare("${titleSortable[0]}/${title} (${year})", "A/Aladdin (1992)");

      // tests that should not failure
      compare("${tags[100]}", "");

      // test empty brackets
      compare("{ ${tags[100]} }", "{  }");

      // test conditional output
      compare("${- ,edition,}", "- Director's Cut");
      // compare("${- ,edition[0,2],}", "- Di"); // does not work at the moment in JMTE

      // test parent and space separator expressions
      compare("${parent}", "A" + File.separator + "1992");
      compare("${movie.country}", "US DE, Vereinigte Staaten, Germany");

      // string-to-array test
      compare("${movie.country;split}", "US DE, Vereinigte Staaten, Germany");
      compare("${movie.country;split()}", "US DE, Vereinigte Staaten, Germany");
      compare("${movie.country;split(asdf)}", "US DE, Vereinigte Staaten, Germany");
      compare("${movie.country;split(-1)}", "US DE, Vereinigte Staaten, Germany");
      compare("${movie.country;split(0)}", "US DE");
      compare("${movie.country;split(1)}", "Vereinigte Staaten");
      compare("${movie.country;split(2)}", "Germany");
      compare("${movie.country;split(3)}", "US DE, Vereinigte Staaten, Germany"); // ioob
      compare("${movie.country;chain(split(2);upper)}", "GERMANY");
      // from/to
      compare("${movie.country;split(0,0)}", "US DE, Vereinigte Staaten, Germany"); // invalid
      compare("${movie.country;split(1,0)}", "US DE, Vereinigte Staaten, Germany"); // invalid
      compare("${movie.country;split(a,Z)}", "US DE, Vereinigte Staaten, Germany"); // invalid
      compare("${movie.country;split(0,1)}", "US DE, Vereinigte Staaten");
      compare("${movie.country;split(2,3)}", "Germany");
      compare("${movie.country;split(2 , 3)}", "Germany");
      compare("${movie.country;split(1,2)}", "Vereinigte Staaten, Germany");
      compare("${movie.country;split(0,17)}", "US DE, Vereinigte Staaten, Germany");
      compare("${movie.country;split(1,17)}", "Vereinigte Staaten, Germany");
      compare("${movie.country;split(2,17)}", "Germany");
      compare("${movie.country;split(3,17)}", ""); // empty or all?

    }
    catch (Exception e) {
      e.printStackTrace();
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  public void testAllMovieTemplates() throws Exception {
    createFakeMovie("Movie 1");
    createFakeMovie("Another Movie");
    createFakeMovie("Cool Movie");

    MovieList ml = MovieModuleManager.getInstance().getMovieList();
    for (ExportTemplate t : MovieExporter.findTemplates(MediaEntityExporter.TemplateType.MOVIE)) {
      System.out.println("\nTEMPLATE: " + t.getPath());
      MovieExporter ex = new MovieExporter(Paths.get(t.getPath()));
      ex.export(ml.getMovies(), getWorkFolder().resolve(t.getName()));
    }
  }

  @Test
  public void testMovieReplacements() throws Exception {
    Map<String, String> tokenMap = MovieRenamer.getTokenMap();

    assertThat(morphTemplate("${movie.title}", tokenMap)).isEqualTo("${movie.title}");
    assertThat(morphTemplate("${title}", tokenMap)).isEqualTo("${movie.title}");
    assertThat(morphTemplate("${title[2]}", tokenMap)).isEqualTo("${movie.title[2]}");
    assertThat(morphTemplate("${movie.title[2]}", tokenMap)).isEqualTo("${movie.title[2]}");
    assertThat(morphTemplate("${if title}${title[2]}${end}", tokenMap)).isEqualTo("${if movie.title}${movie.title[2]}${end}");
    assertThat(morphTemplate("${foreach genres genre}${genre}${end}", tokenMap)).isEqualTo("${foreach movie.genres genre}${genre}${end}");
    assertThat(morphTemplate("${title;lower}", tokenMap)).isEqualTo("${movie.title;lower}");
  }

  private void compare(String template, String expectedValue) {
    String actualValue = engine.transform(JmteUtils.morphTemplate(template, MovieRenamer.getTokenMap()), root);
    assertThat(actualValue).isEqualTo(expectedValue);
  }

  private Movie createMovie() throws Exception {
    Movie movie = new Movie();
    movie.setDataSource(getWorkFolder().toString());
    movie.setPath(getWorkFolder().resolve("A/1992/Aladdin").toString());
    movie.setTitle("Aladdin");
    movie.setOriginalTitle("Disneys Aladdin");
    movie.setSortTitle("Aladdin");
    movie.setRating(new MediaRating(MediaRating.NFO, 7.2f, 5987));
    movie.setYear(1992);
    movie.setTop250(199);
    movie.setPlot("Princess Jasmine grows tired of being forced to remain in the...");
    movie.setTagline("Wish granted");
    movie.setRuntime(90);
    movie.setArtworkUrl("http://poster", MediaFileType.POSTER);
    movie.setArtworkUrl("http://fanart", MediaFileType.FANART);
    movie.setImdbId("tt0103639");
    movie.setTmdbId(812);
    movie.setId("trakt", 655);
    movie.setProductionCompany("Walt Disney");
    movie.setCountry("US/DE, Vereinigte Staaten, Germany");
    movie.setCertification(MediaCertification.US_G);

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    movie.setReleaseDate(sdf.parse("1992-11-25"));

    MediaTrailer trailer = new MediaTrailer();
    trailer.setUrl("https://trailer");
    trailer.setInNfo(true);
    movie.addToTrailer(Collections.singletonList(trailer));

    MovieSet movieSet = new MovieSet();
    movieSet.setTitle("Aladdin Collection");
    movieSet.setPlot("Aladdin plot");
    movie.setMovieSet(movieSet);

    // ToDo fileinfo
    MediaFile mf = new MediaFile();
    mf.setType(MediaFileType.VIDEO);
    mf.setPath(getWorkFolder().resolve("A/1992/Aladdin").toString());
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

    movie.addToMediaFiles(mf);

    movie.setWatched(true);
    movie.setGenres(Arrays.asList(MediaGenres.ADVENTURE, MediaGenres.FAMILY));
    movie
        .addToWriters(Arrays.asList(new Person(Person.Type.WRITER, "Ted Elliott", "Writer"), new Person(Person.Type.WRITER, "Terry Rossio", "Writer"),
            new Person(Person.Type.WRITER, "Ron Clements", "Writer"), new Person(Person.Type.WRITER, "John Jusker", "Writer")));
    movie.addToDirectors(
        Arrays.asList(new Person(Person.Type.DIRECTOR, "Ron Clements", "Director"), new Person(Person.Type.DIRECTOR, "John Jusker", "Director")));
    movie.addToTags(Arrays.asList("Disney", "Oriental"));

    movie.addToActors(Arrays.asList(new Person(Person.Type.ACTOR, "Scott Weinger", "Aladdin 'Al' (voice)"),
        new Person(Person.Type.ACTOR, "Robin Williams", "Genie (voice)")));

    movie.addToProducers(
        Arrays.asList(new Person(Person.Type.PRODUCER, "Ron Clements", "Producer"), new Person(Person.Type.PRODUCER, "Donald W. Ernst", "Producer")));

    movie.setSpokenLanguages("en");
    movie.setMediaSource(MediaSource.BLURAY);
    movie.setEdition(MovieEdition.DIRECTORS_CUT);
    return movie;
  }
}
