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

package org.tinymediamanager.addon;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.Globals;
import org.tinymediamanager.TmmOsUtils;

/**
 * the interface {@link IAddon} is used to provide extra addons (external tools) for tinyMediaManager
 * 
 * @author Manuel Laggner
 */
public interface IAddon {
  /**
   * get the addon name
   * 
   * @return the addon name
   */
  String getAddonName();

  /**
   * checks whether this addon is available or not
   * 
   * @return true/false
   */
  default boolean isAvailable() {
    return StringUtils.isNotBlank(getExecutablePath());
  }

  /**
   * gets the executable filename
   * 
   * @return the filename of the executable
   */
  String getExecutableFilename();

  /**
   * get the full path to the executable
   * 
   * @return the full path to the executable
   */
  default String getExecutablePath() {
    String executableFilename = getExecutableFilename();

    // 1. look in the user addon folder
    Path executable = Paths.get(Globals.CONTENT_FOLDER).resolve("addons").toAbsolutePath().resolve(executableFilename);
    if (Files.exists(executable)) {
      return executable.toString();
    }

    // 2. look in the shipped addon folder (legacy)
    executable = Paths.get(TmmOsUtils.getNativeFolderName()).resolve("addons").toAbsolutePath().resolve(executableFilename);
    if (Files.exists(executable)) {
      return executable.toString();
    }

    // 3. look in the addon folder in the tmm root (legacy)
    executable = Paths.get("addons").toAbsolutePath().resolve(executableFilename);
    if (Files.exists(executable)) {
      return executable.toString();
    }

    // 4. look in the PATH from the system
    String systemPath = System.getenv("PATH");
    String[] pathDirs = systemPath.split(File.pathSeparator);

    for (String pathDir : pathDirs) {
      try {
        executable = Paths.get(pathDir, executableFilename).toAbsolutePath();
        if (Files.exists(executable)) {
          return executable.toString();
        }
      }
      catch (Exception ignored) {
        // faulty PATH entry - just ignore
      }
    }
    return "";
  }
}
