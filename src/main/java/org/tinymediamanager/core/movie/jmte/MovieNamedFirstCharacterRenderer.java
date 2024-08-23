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

package org.tinymediamanager.core.movie.jmte;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.scraper.util.StrgUtils;

import com.floreysoft.jmte.NamedRenderer;
import com.floreysoft.jmte.RenderFormatInfo;

/**
 * The class {@link MovieNamedFirstCharacterRenderer} is a JMTE renderer which is used to return the first character of the given source
 * 
 * @author Manuel Laggner
 */
public class MovieNamedFirstCharacterRenderer implements NamedRenderer {
  private static final Pattern FIRST_ALPHANUM_PATTERN = Pattern.compile("[\\p{L}\\d]");

  @Override
  public String render(Object o, String s, Locale locale, Map<String, Object> map) {
    if (o instanceof String source && StringUtils.isNotBlank(source)) {
      source = StrgUtils.convertToAscii(source, false);
      Matcher matcher = FIRST_ALPHANUM_PATTERN.matcher(source);
      if (matcher.find()) {
        String first = matcher.group();

        if (first.matches("\\p{L}")) {
          return first.toUpperCase(Locale.ROOT);
        }
        else {
          return MovieModuleManager.getInstance().getSettings().getRenamerFirstCharacterNumberReplacement();
        }
      }
    }
    if (o instanceof Number) {
      return MovieModuleManager.getInstance().getSettings().getRenamerFirstCharacterNumberReplacement();
    }
    if (o instanceof Date) {
      return MovieModuleManager.getInstance().getSettings().getRenamerFirstCharacterNumberReplacement();
    }
    return "";
  }

  @Override
  public String getName() {
    return "first";
  }

  @Override
  public RenderFormatInfo getFormatInfo() {
    return null;
  }

  @Override
  public Class<?>[] getSupportedClasses() {
    return new Class[] { Date.class, String.class, Integer.class, Long.class };
  }
}
