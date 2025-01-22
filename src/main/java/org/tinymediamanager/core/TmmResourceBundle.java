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

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * this is a wrapper of the {@link java.util.ResourceBundle} to buffer loading and prevent getting exceptions on missing properties
 */
public class TmmResourceBundle {

  private static ResourceBundle instance;

  private TmmResourceBundle() {
    throw new IllegalAccessError();
  }

  /**
   * get the given {@link String} from the default {@link ResourceBundle} or "???" is the property is missing
   * 
   * @param accessKey
   *          the key to get from the resource bundle
   * @return the {@link String} from the {@link ResourceBundle} or "???"
   */
  public static String getString(String accessKey) {
    // no need for locking here, if there is multi thread access to a null value of instance,
    // the same instance will be returned by ResourceBundle
    if (instance == null) {
      instance = ResourceBundle.getBundle("messages", Locale.getDefault());
    }

    try {
      return instance.getString(accessKey);
    }
    catch (Exception e) {
      return "???";
    }
  }

  public static void clearCache() {
    ResourceBundle.clearCache();
    instance = null;
  }
}
