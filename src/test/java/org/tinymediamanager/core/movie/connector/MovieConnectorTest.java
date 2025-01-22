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

package org.tinymediamanager.core.movie.connector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.BasicMovieTest;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.filenaming.MovieNfoNaming;

public class MovieConnectorTest extends BasicMovieTest {

  @Before
  public void setup() throws Exception {
    super.setup();
    copyResourceFolderToWorkFolder("movie_nfo");
    Files.createDirectories(getWorkFolder().resolve("movie_nfo_out"));
  }

  @Test
  public void testMovieToXbmcConnectorKodi() throws Exception {
    // load data from a given NFO (with unsupported tags)
    Path existingNfo = getWorkFolder().resolve("movie_nfo").resolve("kodi.nfo");
    MovieNfoParser parser = MovieNfoParser.parseNfo(existingNfo);
    Movie movie = parser.toMovie();
    movie.setPath(getWorkFolder().resolve("movie_nfo_out").toString());
    movie.addToMediaFiles(new MediaFile(existingNfo, MediaFileType.NFO));

    // and write it again
    IMovieConnector connector = new MovieToXbmcConnector(movie);
    connector.write(Collections.singletonList(MovieNfoNaming.MOVIE_NFO));
  }

  @Test
  public void testMovieToXbmcConnectorKodi2() throws Exception {
    // load data from a given NFO (with unsupported tags)
    Path existingNfo = getWorkFolder().resolve("movie_nfo").resolve("kodi2.nfo");
    MovieNfoParser parser = MovieNfoParser.parseNfo(existingNfo);
    Movie movie = parser.toMovie();
    MediaFile video = new MediaFile(getWorkFolder().resolve("movie_nfo_out").resolve("test2.avi"));
    movie.addToMediaFiles(video);
    movie.setPath(getWorkFolder().resolve("movie_nfo_out").toString());
    movie.addToMediaFiles(new MediaFile(existingNfo, MediaFileType.NFO));

    // and write it again
    IMovieConnector connector = new MovieToKodiConnector(movie);
    connector.write(Collections.singletonList(MovieNfoNaming.FILENAME_NFO));
  }

  @Test
  public void testMovieToXbmcConnectorMpLegacy() throws Exception {
    // load data from a given NFO (with unsupported tags)
    Path existingNfo = getWorkFolder().resolve("movie_nfo").resolve("mp-legacy.nfo");
    MovieNfoParser parser = MovieNfoParser.parseNfo(existingNfo);
    Movie movie = parser.toMovie();
    MediaFile video = new MediaFile(getWorkFolder().resolve("movie_nfo_out").resolve("test3.avi"));
    movie.addToMediaFiles(video);
    movie.setPath(getWorkFolder().resolve("movie_nfo_out").toString());
    movie.addToMediaFiles(new MediaFile(existingNfo, MediaFileType.NFO));

    // and write it again
    IMovieConnector connector = new MovieToMpLegacyConnector(movie);
    connector.write(Collections.singletonList(MovieNfoNaming.FILENAME_NFO));
  }
}
