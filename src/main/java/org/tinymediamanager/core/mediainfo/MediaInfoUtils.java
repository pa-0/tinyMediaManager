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

package org.tinymediamanager.core.mediainfo;

/**
 * common helpers for Mediainfo
 * 
 * @author Manuel Laggner
 */
public class MediaInfoUtils {
  private static final boolean USE_LIBMEDIAINFO = Boolean.parseBoolean(System.getProperty("tmm.uselibmediainfo", "true"));

  private MediaInfoUtils() {
    throw new IllegalAccessError();
  }

  /**
   * checks if we should use libMediaInfo
   *
   * @return true/false
   */
  public static boolean useMediaInfo() {
    return USE_LIBMEDIAINFO;
  }
}
