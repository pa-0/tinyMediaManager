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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.floreysoft.jmte.AnnotationProcessor;
import com.floreysoft.jmte.TemplateContext;
import com.floreysoft.jmte.token.AnnotationToken;

/**
 * the class {@link RegexpProcessor} offers an annotation which can be used to offer a regular expression based processing in JMTE
 * 
 * @author Manuel Laggner
 */
public class RegexpProcessor implements AnnotationProcessor<String> {
  private final Pattern argumentPattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

  @Override
  public String getType() {
    return "regexp";
  }

  @Override
  public String eval(AnnotationToken token, TemplateContext context) {
    // get all parameters
    try {
      List<String> arguments = new ArrayList<>();
      Matcher matcher = argumentPattern.matcher(token.getArguments());

      while (matcher.find()) {
        arguments.add(matcher.group(1));
      }

      if (arguments.size() == 3) {
        String pattern = prepareArgument(arguments.get(0));
        String source = prepareArgument(arguments.get(1));
        String destination = prepareArgument(arguments.get(2));

        if (StringUtils.isNoneBlank(pattern, source, destination)) {
          Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
          Matcher m = p.matcher(context.engine.transform("${" + source + "}", context.model));

          if (m.find()) {
            int index = 1;
            while (index <= m.groupCount()) {
              destination = destination.replace("$" + index, m.group(index));
              index++;
            }
            return destination;
          }
        }
      }
    }
    catch (Exception ignored) {
      // do nothing
    }

    return "";
  }

  private String prepareArgument(String original) {
    return original.replaceAll("(^\"|\"$)", "").replace("\\\\", "\\"); // NOSONAR
  }
}
