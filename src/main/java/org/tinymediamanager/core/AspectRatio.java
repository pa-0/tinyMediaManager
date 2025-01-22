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
package org.tinymediamanager.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AspectRatio {

  private static List<Float> predefinedValues = null;

  private AspectRatio() {
    throw new IllegalAccessError();
  }

  public static List<Float> getDefaultValues() {
    // lazy init
    if (predefinedValues == null) {
      predefinedValues = new ArrayList<>();
      predefinedValues.add(1.33f);
      predefinedValues.add(1.37f);
      predefinedValues.add(1.43f);
      predefinedValues.add(1.56f);
      predefinedValues.add(1.66f);
      predefinedValues.add(1.78f);
      predefinedValues.add(1.85f);
      predefinedValues.add(1.90f);
      predefinedValues.add(2.00f);
      predefinedValues.add(2.20f);
      predefinedValues.add(2.35f);
      predefinedValues.add(2.40f);
      predefinedValues.add(2.55f);
      predefinedValues.add(2.76f);
    }

    return predefinedValues;
  }

  public static String getDescription(Float aspectRatio) {
    String description = TmmResourceBundle.getString("aspectratio." + String.format(Locale.ENGLISH, "%.2f", aspectRatio));
    if ("???".equals(description)) {
      return String.format("%.2f", aspectRatio);
    }

    return description;
  }
}
