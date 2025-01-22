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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;

import com.sun.jna.Platform;

public class ITTinyFileDialogsTest extends BasicITest {

  private void init() throws Exception {
    String nativepath = "native/";

    // windows
    if (Platform.isWindows()) {
      nativepath += "windows";
    }
    // linux
    else if (Platform.isLinux()) {
      nativepath += "linux";
    }
    // osx
    else if (Platform.isMac()) {
      nativepath += "mac";
    }

    Path tmmNativeDir = Paths.get(nativepath).toAbsolutePath();
    System.setProperty("jna.library.path", tmmNativeDir.toString());
  }

  @Test
  public void testSelectDirectoryDialog() throws Exception {
    init();
    System.out.println(new TinyFileDialogs().chooseDirectory("title", null));
  }

  @Test
  public void testOpenFileDialog() throws Exception {
    init();
    System.out.println(new TinyFileDialogs().openFile("title", Paths.get("/tmp/"), new String[] { "*.txt", "*.nfo" }, "Text/NFO files"));
  }

  @Test
  public void testSaveFileDialog() throws Exception {
    init();
    System.out.println(new TinyFileDialogs().saveFile("title", Paths.get("/tmp/foo.txt"), new String[] { "*.txt", "*.nfo" }, "Text/NFO files"));
  }
}
