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

import java.util.regex.Matcher;

import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a movie cast filter
 * 
 * @author Manuel Laggner
 */
public class MovieCastFilter extends AbstractTextMovieUIFilter {

  @Override
  public String getId() {
    return "movieCast";
  }

  @Override
  public boolean accept(Movie movie) {
    if (StringUtils.isBlank(normalizedFilterText)) {
      return true;
    }
    try {
      // director
      for (Person director : movie.getDirectors()) {
        if (StringUtils.isNotEmpty(director.getName())) {
          Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(director.getName()));
          if (matcher.find()) {
            return true;
          }
        }
      }

      // writer
      for (Person writer : movie.getWriters()) {
        if (StringUtils.isNotEmpty(writer.getName())) {
          Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(writer.getName()));
          if (matcher.find()) {
            return true;
          }
        }
      }

      // actors
      for (Person cast : movie.getActors()) {
        if (StringUtils.isNotEmpty(cast.getName())) {
          Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(cast.getName()));
          if (matcher.find()) {
            return true;
          }
        }
      }

      // producers
      for (Person producer : movie.getProducers()) {
        if (StringUtils.isNotEmpty(producer.getName())) {
          Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(producer.getName()));
          return matcher.find();
        }
      }
    }
    catch (Exception e) {
      // if any exceptions are thrown, just return true
      return true;
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("movieextendedsearch.cast"));
  }
}
