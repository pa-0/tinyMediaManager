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
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a movie trakt id filter
 * 
 * @author Wolfgang Janes
 */
public class MovieTraktIdFilter extends AbstractTextMovieUIFilter {

  @Override
  public String getId() {
    return "movieTraktIdFilter";
  }

  @Override
  public boolean accept(Movie movie) {

    String traktIdAsString = Integer.toString(movie.getTraktId());

    if (StringUtils.isEmpty(normalizedFilterText) && traktIdAsString.equals("0")) {
      return true;
    }

    try {
      if (!traktIdAsString.equals("0") && StringUtils.isNotEmpty(normalizedFilterText)) {
        Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(traktIdAsString));
        return matcher.find();
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
    return new TmmLabel(TmmResourceBundle.getString("metatag.trakt"));
  }
}
