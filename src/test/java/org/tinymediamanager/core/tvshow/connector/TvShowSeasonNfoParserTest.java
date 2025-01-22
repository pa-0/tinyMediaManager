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

package org.tinymediamanager.core.tvshow.connector;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.tvshow.BasicTvShowTest;

public class TvShowSeasonNfoParserTest extends BasicTvShowTest {

  @Before
  public void setup() throws Exception {
    super.setup();

    copyResourceFolderToWorkFolder("tvshowseason_nfo");
  }

  @Test
  public void testNfoParserEmby() {
    // Kodi version 14.2
    try {
      TvShowSeasonNfoParser parser = TvShowSeasonNfoParser.parseNfo(getWorkFolder().resolve("tvshowseason_nfo/emby.nfo"));

      assertThat(parser).isNotNull();
      assertThat(parser.title).contains("Staffel", "Zeichen", "Wunder");
      assertThat(parser.plot).contains("Geschichte", "2225", "Minbari");
      assertThat(parser.userNote).contains("note");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assertions.fail(e.getMessage());
    }
  }
}
