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

package org.tinymediamanager.scraper.tmdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.movie.MovieSetSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.interfaces.IMovieSetMetadataProvider;

/**
 * @author Nikolas Mavropoylos
 */
public class ITTmdbMovieSetMetadataProviderTest extends BasicITest {

  @Test
  public void testCollectionSearchDataIntegrity() throws Exception {
    IMovieSetMetadataProvider mp = new TmdbMovieMetadataProvider();

    MovieSetSearchAndScrapeOptions searchOptions = new MovieSetSearchAndScrapeOptions();
    searchOptions.setSearchQuery("F*ck You, Goethe Collection");
    searchOptions.setLanguage(MediaLanguages.en);

    List<MediaSearchResult> searchResults = mp.search(searchOptions);
    // did we get a result?
    assertNotNull("Result", searchResults);

    assertThat(searchResults.size()).isGreaterThanOrEqualTo(1);

    assertThat(searchResults.get(0).getTitle()).isEqualTo("F*ck You, Goethe Collection");
    assertThat(searchResults.get(0).getId()).isEqualTo("344555");
  }

  @Test
  public void testCollectionSearchDataIntegrityInGerman() throws Exception {
    IMovieSetMetadataProvider mp = new TmdbMovieMetadataProvider();

    MovieSetSearchAndScrapeOptions options = new MovieSetSearchAndScrapeOptions();
    options.setSearchQuery("F*ck You, Goethe Collection");
    options.setLanguage(MediaLanguages.de);

    List<MediaSearchResult> searchResults = mp.search(options);
    // did we get a result?
    assertNotNull("Result", searchResults);

    assertThat(searchResults.size()).isGreaterThanOrEqualTo(1);

    assertThat(searchResults.get(0).getTitle()).isEqualTo("Fack ju Göhte Filmreihe");
  }

  @Test
  public void testCollectionSearchDataIntegrityInGreek() throws Exception {
    IMovieSetMetadataProvider mp = new TmdbMovieMetadataProvider();

    MovieSetSearchAndScrapeOptions options = new MovieSetSearchAndScrapeOptions();
    options.setSearchQuery("F*ck You, Goethe Collection");
    options.setLanguage(MediaLanguages.el);

    List<MediaSearchResult> searchResults = mp.search(options);
    // did we get a result?
    assertNotNull("Result", searchResults);

    assertThat(searchResults.size()).isGreaterThanOrEqualTo(1);

    assertThat(searchResults.get(0).getTitle()).isEqualTo("Fack ju Göhte Filmreihe");
  }
}
