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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.Utils;

public class ITAddonManagerTest extends BasicITest {

  @Test
  public void testFFmpegVersion() throws Exception {
    FFmpegAddon fFmpegAddon = new FFmpegAddon();
    // assertThat(AddonManager.getLatestVersionForAddon(fFmpegAddon)).isNotBlank();
  }

  @Test
  public void testFFmpegDownload() throws Exception {
    FFmpegAddon fFmpegAddon = new FFmpegAddon();

    Path destination = Paths.get("target/test-classes/");
    // AddonManager.downloadLatestVersionForAddon(fFmpegAddon, destination);
    assertThat(destination.resolve("ffmpeg")).exists();
    assertThat(destination.resolve("ffmpeg")).isDirectory();
    assertThat(destination.resolve("ffmpeg").resolve("ffmpeg")).exists();
    assertThat(destination.resolve("ffmpeg").resolve("ffmpeg")).isExecutable();
    assertThat(destination.resolve("ffmpeg.v")).exists();
    assertThat(Utils.readFileToString(destination.resolve("ffmpeg.v"))).isNotBlank();
  }
}
