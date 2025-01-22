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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.tinymediamanager.core.IPrintable;

import com.floreysoft.jmte.NamedRenderer;
import com.floreysoft.jmte.RenderFormatInfo;

/**
 * same as the array renderer, but do not add existing values (unique!)
 * 
 * @author Manuel Laggner
 */
public class NamedArrayUniqueRenderer implements NamedRenderer {
  @Override
  public String render(Object o, String s, Locale locale, Map<String, Object> map) {
    String delimiter = s != null ? s : ", ";
    if (o instanceof List<?>) {
      Set<String> values = new LinkedHashSet<>();
      for (Object obj : (List<?>) o) {
        if (obj instanceof IPrintable) {
          values.add(((IPrintable) obj).toPrintable());
        }
        else if (obj != null) {
          values.add(obj.toString());
        }
      }
      return String.join(delimiter, values);
    }
    if (o instanceof String) {
      return (String) o;
    }
    return "";
  }

  @Override
  public String getName() {
    return "uniqueArray";
  }

  @Override
  public RenderFormatInfo getFormatInfo() {
    return null;
  }

  @Override
  public Class<?>[] getSupportedClasses() {
    return new Class[] { List.class, String.class };
  }
}
