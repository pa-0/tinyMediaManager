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

import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.ACTOR;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.COUNTRY;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.DIRECTOR;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.FILENAME;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.NOTE;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.PLOT;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.PRODUCER;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.PRODUCTION_COMPANY;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.SPOKEN_LANGUAGE;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.TAGLINE;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.TAGS;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.WRITER;

import java.util.List;
import java.util.regex.Matcher;

import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * the class {@link MovieAllInOneFilter} implements a generic text field filter
 *
 * @author Manuel Laggner
 */
public class MovieAllInOneFilter extends AbstractTextMovieUIFilter {

  private final MovieSettings settings;

  public MovieAllInOneFilter() {
    super();
    settings = MovieModuleManager.getInstance().getSettings();
  }

  @Override
  protected JLabel createLabel() {
    TmmLabel label = new TmmLabel(TmmResourceBundle.getString("filter.universal"));
    label.setHintIcon(IconManager.HINT);
    label.setToolTipText(TmmResourceBundle.getString("filter.universal.hint2"));
    return label;
  }

  @Override
  public String getId() {
    return "movieAllInOne";
  }

  @Override
  public boolean accept(Movie movie) {
    if (StringUtils.isBlank(normalizedFilterText)) {
      return true;
    }

    List<AbstractSettings.UniversalFilterFields> filterFields = settings.getUniversalFilterFields();

    // note
    if (filterFields.contains(NOTE) && filterField(movie.getNote())) {
      return true;
    }

    // file name
    if (filterFields.contains(FILENAME)) {
      for (MediaFile mediaFile : movie.getMediaFiles(MediaFileType.VIDEO)) {
        if (filterField(mediaFile.getFilename())) {
          return true;
        }
      }

      // also have a look at the original filename
      if (filterField(movie.getOriginalFilename())) {
        return true;
      }
    }

    // production company
    if (filterFields.contains(PRODUCTION_COMPANY) && filterField(movie.getProductionCompany())) {
      return true;
    }

    // country
    if (filterFields.contains(COUNTRY) && filterField(movie.getCountry())) {
      return true;
    }

    // plot
    if (filterFields.contains(PLOT) && filterField(movie.getPlot())) {
      return true;
    }

    // tagline
    if (filterFields.contains(TAGLINE) && filterField(movie.getTagline())) {
      return true;
    }

    // spoken languages
    if (filterFields.contains(SPOKEN_LANGUAGE)) {
      if (filterField(movie.getSpokenLanguages())) {
        return true;
      }
      if (filterField(movie.getLocalizedSpokenLanguages())) {
        return true;
      }
    }

    // actors
    if (filterFields.contains(ACTOR)) {
      for (Person cast : movie.getActors()) {
        if (filterField(cast.getName())) {
          return true;
        }
      }
    }

    // producers
    if (filterFields.contains(PRODUCER)) {
      for (Person producer : movie.getProducers()) {
        if (filterField(producer.getName())) {
          return true;
        }
      }
    }

    // director
    if (filterFields.contains(DIRECTOR)) {
      for (Person director : movie.getDirectors()) {
        if (filterField(director.getName())) {
          return true;
        }
      }
    }

    // writer
    if (filterFields.contains(WRITER)) {
      for (Person writer : movie.getWriters()) {
        if (filterField(writer.getName())) {
          return true;
        }
      }
    }

    // tags
    if (filterFields.contains(TAGS)) {
      for (String tag : movie.getTags()) {
        if (filterField(tag)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean filterField(String textToFilter) {
    try {
      if (StringUtils.isNotBlank(textToFilter)) {
        Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(textToFilter));
        if (matcher.find()) {
          return true;
        }
      }
    }
    catch (Exception e) {
      // fall through
    }
    return false;
  }
}
