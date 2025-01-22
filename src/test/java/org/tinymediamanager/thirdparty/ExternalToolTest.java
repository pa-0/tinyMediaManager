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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tinymediamanager.core.Utils;

public class ExternalToolTest {
  private static Path addonFolder;

  @BeforeClass
  public static void setup() throws Exception {
    TemporaryFolder tmpFolder = new TemporaryFolder();
    tmpFolder.create();
    addonFolder = tmpFolder.newFolder("addons").toPath();
    System.setProperty("tmm.addonfolder", addonFolder.toAbsolutePath().toString());
  }

  @Test
  public void readJson() throws Exception {
    ExternalTools externalTools = new ExternalTools(Paths.get("target/test-classes/external_tools/external-tools.json").toUri().toString());
    List<ExternalTools.ExternalTool> externalToolsList = externalTools.parseJson();
    assertThat(externalToolsList).isNotEmpty();
  }

  @Test
  public void checkToolNotInstalled() {
    assertThat(ExternalTools.isToolInstalled("ffmpeg")).isEqualTo(false);
  }

  @Test
  public void checkToolInstalled() throws Exception {
    Utils.deleteDirectorySafely(addonFolder);
    Files.createDirectory(addonFolder);

    Files.copy(Paths.get("target/test-classes/external_tools/yt-dlp.ver"), addonFolder.resolve("yt-dlp.ver"));
    assertThat(ExternalTools.isToolInstalled("yt-dlp")).isEqualTo(true);
  }

  @Test
  public void detectUpdateYtDlp() throws Exception {
    Utils.deleteDirectorySafely(addonFolder);
    Files.createDirectory(addonFolder);

    Files.copy(Paths.get("target/test-classes/external_tools/yt-dlp.ver"), addonFolder.resolve("yt-dlp.ver"));

    ExternalTools externalTools = new ExternalTools(Paths.get("target/test-classes/external_tools/external-tools.json").toUri().toString());
    List<ExternalTools.ExternalTool> externalToolsList = externalTools.parseJson();
    assertThat(externalToolsList).isNotEmpty();

    assertThat(externalTools.isUpdateAvailable("yt-dlp")).isEqualTo(true);
  }
}
