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

package org.tinymediamanager.thirdparty;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ITExternalToolTest {
  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  private Path           addonFolder;

  @Before
  public void setup() throws Exception {
    addonFolder = tmpFolder.newFolder("addons").toPath();
    System.setProperty("tmm.addonfolder", addonFolder.toAbsolutePath().toString());
  }

  @Test
  public void downloadToolInZip() throws Exception {
    ExternalTools externalTools = new ExternalTools(Paths.get("target/test-classes/external_tools/external-tools.json").toUri().toString());
    externalTools.downloadTool("ffmpeg");

    if (SystemUtils.IS_OS_WINDOWS) {
      assertThat(addonFolder.resolve("ffmpeg.exe")).exists();
    }
    else {
      assertThat(addonFolder.resolve("ffmpeg")).exists();
    }
    assertThat(addonFolder.resolve("ffmpeg.ver")).exists();
  }

  @Test
  public void downloadToolPlain() throws Exception {
    ExternalTools externalTools = new ExternalTools(Paths.get("target/test-classes/external_tools/external-tools.json").toUri().toString());
    externalTools.downloadTool("yt-dlp");

    if (SystemUtils.IS_OS_WINDOWS) {
      assertThat(addonFolder.resolve("yt-dlp.exe")).exists();
    }
    else {
      assertThat(addonFolder.resolve("yt-dlp")).exists();
    }
    assertThat(addonFolder.resolve("yt-dlp.ver")).exists();
  }
}
