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
package org.tinymediamanager.ui.movies.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.swing.JLabel;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a missing metadata movie filter
 * 
 * @author Manuel Laggner
 */
public class MovieMissingMetadataFilter extends AbstractCheckComboBoxMovieUIFilter<MovieMissingMetadataFilter.MetadataField> {

  private final MovieList movieList;

  public MovieMissingMetadataFilter() {
    super();
    movieList = MovieModuleManager.getInstance().getMovieList();

    checkComboBox.enableFilter((s, s2) -> s.toString().toLowerCase(Locale.ROOT).startsWith(s2.toLowerCase(Locale.ROOT)));

    // initial filling
    List<MovieMissingMetadataFilter.MetadataField> values = new ArrayList<>();
    for (MovieScraperMetadataConfig config : MovieScraperMetadataConfig.values()) {
      if (config.isMetaData() || config.isCast()) {
        values.add(new MetadataField(config));
      }
    }
    setValues(values);
  }

  @Override
  public String getId() {
    return "movieMissingMetadata";
  }

  @Override
  protected String parseTypeToString(MetadataField type) throws Exception {
    return type.config.name();
  }

  @Override
  protected MetadataField parseStringToType(String string) throws Exception {
    try {
      return new MetadataField(MovieScraperMetadataConfig.valueOf(string));
    }
    catch (Exception e) {
      return null;
    }
  }

  @Override
  public boolean accept(Movie movie) {
    List<MovieScraperMetadataConfig> values = new ArrayList<>();
    for (MetadataField metadataField : checkComboBox.getSelectedItems()) {
      values.add(metadataField.config);
    }
    return !movieList.detectMissingFields(movie, values).isEmpty();
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("movieextendedsearch.missingmetadata"));
  }

  public static class MetadataField {
    private final MovieScraperMetadataConfig config;

    public MetadataField(MovieScraperMetadataConfig config) {
      this.config = config;
    }

    @Override
    public String toString() {
      return config.getDescription();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      MetadataField that = (MetadataField) o;
      return config == that.config;
    }

    @Override
    public int hashCode() {
      return Objects.hash(config);
    }
  }
}
