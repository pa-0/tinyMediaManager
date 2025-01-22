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
package org.tinymediamanager.core.jmte;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * the class {@link JmteUtils} provides some utilities for JMTE
 *
 * @author Manuel Laggner
 */
public class JmteUtils {

  private JmteUtils() {
    throw new IllegalAccessError();
  }

  /**
   * morph the given template to the JMTE template
   *
   * @param template
   *          the given template
   * @return the JMTE compatible template
   */
  public static String morphTemplate(String template, Map<String, String> tokenMap) {
    String morphedTemplate = template;
    // replace normal template entries
    for (Map.Entry<String, String> entry : tokenMap.entrySet()) {
      Pattern pattern = Pattern.compile("\\$\\{" + entry.getKey() + "([^a-zA-Z0-9])", Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(morphedTemplate);
      while (matcher.find()) {
        morphedTemplate = morphedTemplate.replace(matcher.group(), "${" + entry.getValue() + matcher.group(1));
      }
    }

    // replace conditional template entries and loops
    for (Map.Entry<String, String> entry : tokenMap.entrySet()) {
      // ${if ... }
      Pattern pattern = Pattern.compile("\\$\\{if " + entry.getKey() + "([^a-zA-Z0-9])", Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(morphedTemplate);
      while (matcher.find()) {
        morphedTemplate = morphedTemplate.replace(matcher.group(), "${if " + entry.getValue() + matcher.group(1));
      }

      // ${x,...,y}
      pattern = Pattern.compile("\\$\\{(.*?)," + entry.getKey() + "([^a-zA-Z0-9])", Pattern.CASE_INSENSITIVE);
      matcher = pattern.matcher(morphedTemplate);
      while (matcher.find()) {
        morphedTemplate = morphedTemplate.replace(matcher.group(), "${" + matcher.group(1) + "," + entry.getValue() + matcher.group(2));
      }

      // ${foreach ... x}
      pattern = Pattern.compile("\\$\\{foreach " + entry.getKey() + "([^a-zA-Z0-9])", Pattern.CASE_INSENSITIVE);
      matcher = pattern.matcher(morphedTemplate);
      while (matcher.find()) {
        morphedTemplate = morphedTemplate.replace(matcher.group(), "${foreach " + entry.getValue() + matcher.group(1));
      }
    }

    // last but not least escape single backslashes
    morphedTemplate = morphedTemplate.replaceAll("\\\\(?![{}])", "\\\\\\\\");

    return morphedTemplate;
  }

}
