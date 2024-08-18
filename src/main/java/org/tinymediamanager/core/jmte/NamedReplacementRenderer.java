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

package org.tinymediamanager.core.jmte;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.scraper.util.CacheMap;

import com.floreysoft.jmte.NamedRenderer;
import com.floreysoft.jmte.RenderFormatInfo;

/**
 * this renderer is used to replace characters in the given value. The file contents are cached for 60 seconds
 * 
 * @author Manuel Laggner
 */
public class NamedReplacementRenderer implements NamedRenderer {
  private static final Logger                           LOGGER = LoggerFactory.getLogger(NamedReplacementRenderer.class);

  private static final CacheMap<String, List<String[]>> CACHE  = new CacheMap<>(60, 5);

  @Override
  public String render(Object object, String filename, Locale locale, Map<String, Object> map) {
    if (object == null || StringUtils.isBlank(filename)) {
      return null;
    }

    String result = object.toString();

    filename = filename.strip();

    List<String[]> csvContents = CACHE.get(filename);
    if (csvContents == null) {
      // try to load the csv with the replacements
      Path csv = Paths.get(Globals.DATA_FOLDER, filename);
      if (Files.exists(csv)) {
        // try to parse the csv
        List<String[]> tokens = new ArrayList<>();
        try (Reader in = new FileReader(csv.toFile())) {
          Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
          for (CSVRecord record : records) {
            if (record.size() == 2) {
              tokens.add(new String[] { record.get(0), record.get(1) });
            }
          }
          CACHE.put(filename, tokens);
          csvContents = tokens;
        }
        catch (Exception e) {
          LOGGER.error("could not read csv: '{}'", e.getMessage());
        }
      }
    }

    if (csvContents != null) {
      for (String[] csvLine : csvContents) {
        if (csvLine.length != 2) {
          continue;
        }

        result = result.replace(csvLine[0], csvLine[1]);
      }
    }
    else {
      // might be an inline string replacement ;)
      String[] rep = filename.split(",", -1);
      if (rep.length < 2) {
        return result;
      }
      if (rep.length == 2) {
        result = result.replace(rep[0], rep[1]);
      }
      else {
        // more than 2? weird stuff, like comma-to-dot ",,." replacement ^^
        if (rep[0].isEmpty()) {
          // first char was a comma - treat as a wanted replacement char, not as delim!
          // second must then also be empty, since this IS the delimiter
          // so use third...
          result = result.replace(",", rep[2]);
        }
      }
    }

    return result;
  }

  @Override
  public String getName() {
    return "replace";
  }

  @Override
  public RenderFormatInfo getFormatInfo() {
    return null;
  }

  @Override
  public Class<?>[] getSupportedClasses() {
    return new Class[] { String.class };
  }
}
