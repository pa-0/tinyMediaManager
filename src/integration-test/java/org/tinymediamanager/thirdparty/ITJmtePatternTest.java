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
package org.tinymediamanager.thirdparty;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.tvshow.TvShowRenamer;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.DynaEnum;

public class ITJmtePatternTest extends BasicITest {

  @Test
  public void getProperties() throws Exception {
    printBeanInfo(Movie.class, "movie.");
    printBeanInfo(MovieSet.class, "movieSet.");
    printBeanInfo(TvShow.class, "tvShow.");
    printBeanInfo(TvShowSeason.class, "season.");
    printBeanInfo(TvShowEpisode.class, "episode.");

    printBeanInfo(Person.class, ".");
    printBeanInfo(MediaRating.class, ".");
    printBeanInfo(MediaFile.class, ".");
    printBeanInfo(MediaFileAudioStream.class, ".");
    printBeanInfo(MediaFileSubtitle.class, ".");
    printBeanInfo(MediaTrailer.class, ".");
    printBeanInfo(MediaSource.class, ".");
  }

  private void printBeanInfo(Class<?> clazz, String prefix) throws Exception {
    System.out.println("\n\n" + clazz.getName() + "\n");

    // access properties as Map
    BeanInfo info = Introspector.getBeanInfo(clazz);
    PropertyDescriptor[] pds = info.getPropertyDescriptors();

    for (PropertyDescriptor descriptor : pds) {
      if ("class".equals(descriptor.getDisplayName())) {
        continue;
      }

      if ("declaringClass".equals(descriptor.getDisplayName())) {
        continue;
      }

      if (descriptor.getReadMethod() != null) {
        String shortToken = getShort(prefix, descriptor.getDisplayName());
        String fullToken = getFull(prefix, descriptor.getDisplayName());

        final Type type = descriptor.getReadMethod().getGenericReturnType();
        if (type instanceof ParameterizedType) {
          ParameterizedType pt = (ParameterizedType) type;

          String typeAsString;
          Class<?> rawTypeClass = (Class<?>) pt.getRawType();
          typeAsString = rawTypeClass.getSimpleName() + "\\<";

          int index = 0;
          for (Type arg : pt.getActualTypeArguments()) {
            Class<?> argClass = (Class<?>) arg;
            typeAsString += getTypeName(argClass);

            index++;

            if (index < pt.getActualTypeArguments().length) {
              typeAsString += ",";
            }
          }
          typeAsString += "\\>";
          System.out.println("| " + typeAsString + " | " + fullToken + " | " + shortToken);
        }
        else {
          System.out.println("| " + getTypeName(descriptor.getReadMethod().getReturnType()) + " | " + fullToken + " | " + shortToken);
        }
      }
    }
  }

  private String getFull(String prefix, String name) {
    String fullToken = name;
    if (prefix.length() > 3) {
      fullToken = "${" + prefix + fullToken + "}";
    }
    return fullToken;
  }

  private String getShort(String prefix, String name) {
    String shortToken = MovieRenamer.getTokenMapReversed().get(prefix + name);
    if (shortToken == null) {
      shortToken = TvShowRenamer.getTokenMapReversed().get(prefix + name);
    }
    if (shortToken != null && prefix.length() > 3) {
      shortToken = "${" + shortToken + "} |";
    }
    else {
      shortToken = " | ";
    }
    return shortToken;
  }

  private String getTypeName(Class<?> clazz) {
    String typeAsString;

    if (clazz.isEnum()) {
      typeAsString = "String";
    }
    else if (DynaEnum.class.isAssignableFrom(clazz)) {
      typeAsString = "String";
    }
    else {
      typeAsString = clazz.getSimpleName();
    }
    return typeAsString;
  }

}
