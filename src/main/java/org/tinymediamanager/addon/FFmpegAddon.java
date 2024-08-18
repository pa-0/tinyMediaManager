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
package org.tinymediamanager.addon;

import org.apache.commons.lang3.SystemUtils;

/**
 * the class {@link FFmpegAddon} provides native integration of FFmpeg
 * 
 * @author Manuel Laggner
 */
public class FFmpegAddon implements IAddon {
  private static final String ADDON_NAME = "ffmpeg";

  @Override
  public String getAddonName() {
    return ADDON_NAME;
  }

  @Override
  public String getExecutableFilename() {
    if (SystemUtils.IS_OS_WINDOWS) {
      return "ffmpeg.exe";
    }
    return "ffmpeg";
  }
}
