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

package org.tinymediamanager.core.movie.jmte;

import java.util.Locale;
import java.util.Map;

import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;

import com.floreysoft.jmte.NamedRenderer;
import com.floreysoft.jmte.RenderFormatInfo;

/**
 * The class {@link MovieNamedIndexOfMovieSetWithDummyRenderer} is a JMTE renderer which is used to get the movie set index of this movie respecting
 * all dummy movies too
 * 
 * @author Manuel Laggner
 */
public class MovieNamedIndexOfMovieSetWithDummyRenderer implements NamedRenderer {

  @Override
  public String render(Object o, String s, Locale locale, Map<String, Object> map) {
    if (o instanceof Movie movie) {
      MovieSet movieSet = movie.getMovieSet();
      if (movieSet == null) {
        return null;
      }

      return String.valueOf(movieSet.getMovieIndexWithDummy(movie) + 1);
    }

    return null;
  }

  @Override
  public String getName() {
    return "indexOfMovieSetWithDummy";
  }

  @Override
  public RenderFormatInfo getFormatInfo() {
    return null;
  }

  @Override
  public Class<?>[] getSupportedClasses() {
    return new Class[] { Movie.class };
  }
}
