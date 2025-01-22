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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.WString;

/**
 * the interface {@link TinyFileDialogsLibrary} is the bridge between Java and the native library tinyfiledoalogs
 * 
 * @author Manuel Laggner
 */
interface TinyFileDialogsLibrary extends Library {
  TinyFileDialogsLibrary INSTANCE = Native.load("tinyfiledialogs", TinyFileDialogsLibrary.class);

  /**
   * open the directory chooser
   *
   * @param title
   *          the dialog title or null
   * @param defaultPath
   *          the default path or null
   * @return the selected path or null
   */
  String tinyfd_selectFolderDialog(String title, String defaultPath);

  /**
   * open the directory chooser - Windows version
   *
   * @param title
   *          the dialog title or null
   * @param defaultPath
   *          the default path or null
   * @return the selected path or null
   */
  WString tinyfd_selectFolderDialogW(WString title, WString defaultPath);

  /**
   * open the file open dialog
   * 
   * @param title
   *          the dialog title or null
   * @param defaultPathAndFile
   *          the default path AND filename or null
   * @param filterPatternCount
   *          the count of filter patterns
   * @param filterPatterns
   *          an array of all filter patterns (e.g. {"*.png","*.jpg"}) or null
   * @param singleFilterDescription
   *          a single description (e.g. "image files") or null
   * @param allowMultiSelections
   *          allow multiple selections (0 = false or 1 = true)
   * @return the absolute path (in case of multiple files, the separator is |) to the selected file or null
   */
  String tinyfd_openFileDialog(String title, String defaultPathAndFile, int filterPatternCount, String[] filterPatterns,
      String singleFilterDescription, int allowMultiSelections);

  /**
   * open the file open dialog - Windows version
   *
   * @param title
   *          the dialog title or null
   * @param defaultPathAndFile
   *          the default path AND filename or null
   * @param filterPatternCount
   *          the count of filter patterns
   * @param filterPatterns
   *          an array of all filter patterns (e.g. {"*.png","*.jpg"}) or null
   * @param singleFilterDescription
   *          a single description (e.g. "image files") or null
   * @param allowMultiSelections
   *          allow multiple selections (0 = false or 1 = true)
   * @return the absolute path (in case of multiple files, the separator is |) to the selected file or null
   */
  WString tinyfd_openFileDialogW(WString title, WString defaultPathAndFile, int filterPatternCount, WString[] filterPatterns,
      WString singleFilterDescription, int allowMultiSelections);

  /**
   * open the file save dialog
   *
   * @param title
   *          the dialog title or null
   * @param defaultPathAndFile
   *          the default path AND filename or null
   * @param filterPatternCount
   *          the count of filter patterns
   * @param filterPatterns
   *          an array of all filter patterns (e.g. {"*.png","*.jpg"}) or null
   * @param singleFilterDescription
   *          a single description (e.g. "image files") or null
   * @return the absolute path (in case of multiple files, the separator is |) to the selected file or null
   */
  String tinyfd_saveFileDialog(String title, String defaultPathAndFile, int filterPatternCount, String[] filterPatterns,
      String singleFilterDescription);

  /**
   * open the file save dialog - Windows version
   *
   * @param title
   *          the dialog title or null
   * @param defaultPathAndFile
   *          the default path AND filename or null
   * @param filterPatternCount
   *          the count of filter patterns
   * @param filterPatterns
   *          an array of all filter patterns (e.g. {"*.png","*.jpg"}) or null
   * @param singleFilterDescription
   *          a single description (e.g. "image files") or null
   * @return the absolute path (in case of multiple files, the separator is |) to the selected file or null
   */
  WString tinyfd_saveFileDialogW(WString title, WString defaultPathAndFile, int filterPatternCount, WString[] filterPatterns,
      WString singleFilterDescription);
}
