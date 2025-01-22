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

package org.tinymediamanager.core.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.tinymediamanager.core.Utils;

public class MovieSettingsTest extends BasicMovieTest {

  @Test
  public void testMovieSettings() throws Exception {
    MovieSettings settings = MovieSettings.getInstance();
    assertThat(settings).isNotNull();
    settings.setAsciiReplacement(true);

    // let the dirty flag set by the async propertychange listener
    Thread.sleep(100);

    settings.saveSettings();

    // cannot re-instantiate settings - need to check plain file
    String config = Utils.readFileToString(getSettingsFolder().resolve(MovieModuleManager.getInstance().getSettings().getConfigFilename()));
    assertTrue(config.contains("\"asciiReplacement\" : true"));
  }
}
