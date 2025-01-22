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
package org.tinymediamanager.thirdparty;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.sun.jna.WString;

/**
 * the class {@link TinyFileDialogs} is the wrapper to the native part of tinyfiledialogs
 * 
 * @author Manuel Laggner
 */
public class TinyFileDialogs {

  public Path chooseDirectory(String title, Path defaultPath) throws Exception {
    if (TinyFileDialogsLibrary.INSTANCE == null) {
      throw new Exception("Could not access tinyfiledialogs");
    }

    String initialPath = null;

    if (defaultPath != null) {
      initialPath = defaultPath.toAbsolutePath().toString();
      if (defaultPath.toFile().isDirectory()) {
        initialPath += File.separator;
      }
    }

    String path = null;

    if (SystemUtils.IS_OS_WINDOWS) {
      WString titleW = title != null ? new WString(title) : null;
      WString initialPathW = initialPath != null ? new WString(initialPath) : null;

      WString result = TinyFileDialogsLibrary.INSTANCE.tinyfd_selectFolderDialogW(titleW, initialPathW);
      if (result != null) {
        path = result.toString();
      }
    }
    else {
      path = TinyFileDialogsLibrary.INSTANCE.tinyfd_selectFolderDialog(title, initialPath);
    }

    if (StringUtils.isBlank(path)) {
      return null;
    }

    return Paths.get(path);
  }

  public Path openFile(String title, Path defaultFile, String[] filterPatterns, String singleFilterDescription) throws Exception {
    if (TinyFileDialogsLibrary.INSTANCE == null) {
      throw new Exception("Could not access tinyfiledialogs");
    }

    String initialFile = null;

    if (defaultFile != null) {
      initialFile = defaultFile.toAbsolutePath().toString();
      if (defaultFile.toFile().isDirectory()) {
        initialFile += File.separator;
      }
    }

    int filterPatternCount = 0;
    if (filterPatterns != null) {
      filterPatternCount = filterPatterns.length;
    }

    String path = null;

    if (SystemUtils.IS_OS_WINDOWS) {
      WString titleW = title != null ? new WString(title) : null;
      WString initialFileW = initialFile != null ? new WString(initialFile) : null;
      WString singleFilterDescriptionW = singleFilterDescription != null ? new WString(singleFilterDescription) : null;
      WString[] filterPatternsW = null;

      if (filterPatterns != null) {
        filterPatternsW = new WString[filterPatterns.length];
        for (int i = 0; i < filterPatterns.length; i++) {
          filterPatternsW[i] = new WString(filterPatterns[i]);
        }
      }

      WString result = TinyFileDialogsLibrary.INSTANCE.tinyfd_openFileDialogW(titleW, initialFileW, filterPatternCount, filterPatternsW,
          singleFilterDescriptionW, 0);
      if (result != null) {
        path = result.toString();
      }
    }
    else {
      path = TinyFileDialogsLibrary.INSTANCE.tinyfd_openFileDialog(title, initialFile, filterPatternCount, filterPatterns, singleFilterDescription,
          0);
    }

    if (StringUtils.isBlank(path)) {
      return null;
    }

    return Paths.get(path);
  }

  public Path saveFile(String title, Path defaultFile, String[] filterPatterns, String singleFilterDescription) throws Exception {
    if (TinyFileDialogsLibrary.INSTANCE == null) {
      throw new Exception("Could not access tinyfiledialogs");
    }

    String initialFile = defaultFile != null ? defaultFile.toAbsolutePath().toString() : null;
    int filterPatternCount = 0;
    if (filterPatterns != null) {
      filterPatternCount = filterPatterns.length;
    }

    String path = null;

    if (SystemUtils.IS_OS_WINDOWS) {
      WString titleW = title != null ? new WString(title) : null;
      WString initialFileW = initialFile != null ? new WString(initialFile) : null;
      WString singleFilterDescriptionW = singleFilterDescription != null ? new WString(singleFilterDescription) : null;
      WString[] filterPatternsW = null;

      if (filterPatterns != null) {
        filterPatternsW = new WString[filterPatterns.length];
        for (int i = 0; i < filterPatterns.length; i++) {
          filterPatternsW[i] = new WString(filterPatterns[i]);
        }
      }

      WString result = TinyFileDialogsLibrary.INSTANCE.tinyfd_saveFileDialogW(titleW, initialFileW, filterPatternCount, filterPatternsW,
          singleFilterDescriptionW);
      if (result != null) {
        path = result.toString();
      }
    }
    else {
      path = TinyFileDialogsLibrary.INSTANCE.tinyfd_saveFileDialog(title, initialFile, filterPatternCount, filterPatterns, singleFilterDescription);
    }

    if (StringUtils.isBlank(path)) {
      return null;
    }

    return Paths.get(path);
  }
}
