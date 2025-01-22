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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.updater.getdown.TmmGetdownApplication;
import org.tinymediamanager.updater.getdown.TmmGetdownDownloader;

import com.threerings.getdown.data.Application;
import com.threerings.getdown.data.EnvConfig;
import com.threerings.getdown.data.Resource;
import com.threerings.getdown.net.Downloader;
import com.threerings.getdown.util.ProgressObserver;

public class ITGetdownTest extends BasicITest {

  @Test
  public void testGetdown() throws Exception {
    List<EnvConfig.Note> notes = new ArrayList<>();

    Path path = getWorkFolder();

    String[] args = { path.toString() };
    EnvConfig envc = EnvConfig.create(args, notes);
    Application app = new TmmGetdownApplication(envc);

    // reset logger from jul to slf4j
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    // first parse our application deployment file
    try {
      app.init(true);
    }
    catch (IOException ioe) {
      // no worries - if getdown.txt is missing the following code recovers by re-downloading it
      app.attemptRecovery(e -> {
      });
      // and re-initalize
      app.init(true);

    }

    ProgressObserver progobs = percent -> {
      // do nothing
    };

    // we will clean the checksum update folder at startup (this will force to re-download partially downloaded files)
    Utils.deleteDirectoryRecursive(path.resolve("update"));
    Path updateFolder = path.resolve("update");
    updateFolder.toFile().mkdirs();

    // we create this tracking counter here so that we properly note the first time through
    // the update process whether we previously had validated resources (which means this
    // is not a first time install); we may, in the course of updating, wipe out our
    // validation markers and revalidate which would make us think we were doing a fresh
    // install if we didn't specifically remember that we had validated resources the first
    // time through
    int[] alreadyValid = new int[1];

    // we'll keep track of all the resources we unpack
    Set<Resource> unpacked = new HashSet<>();

    Set<Resource> toDownload = new HashSet<>();
    Set<Resource> toInstallResources = new HashSet<>();
    app.verifyMetadata(e -> {
    });
    app.verifyResources(progobs, alreadyValid, unpacked, toInstallResources, toDownload);

    if (!toDownload.isEmpty()) {
      // we have resources to download, also note them as to-be-installed
      toInstallResources.addAll(toDownload);

      // redownload any that are corrupt or invalid...
      System.out.println(
          toDownload.size() + " of " + app.getAllActiveResources().size() + " rsrcs require update (" + alreadyValid[0] + " assumed valid).");

      download(toDownload, app);

      // and prepare for installation
      for (Resource resource : toInstallResources) {
        resource.install(true);
      }
    }
  }

  /**
   * Called if the application is determined to require resource downloads.
   */
  private void download(Collection<Resource> resources, Application app) throws IOException {
    Downloader dl = new TmmGetdownDownloader();

    if (dl.download(resources, app.maxConcurrentDownloads())) {
      // download completed; mark the completed download by putting a .ready file into the /update folder
      Files.createFile(getWorkFolder().resolve("update").resolve(".ready"));
    }
  }
}
