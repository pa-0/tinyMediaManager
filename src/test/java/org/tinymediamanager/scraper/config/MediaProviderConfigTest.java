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

package org.tinymediamanager.scraper.config;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.scraper.MediaProviderInfo;

public class MediaProviderConfigTest extends BasicTest {

  @Before
  public void setup() throws Exception {
    super.setup();

    FileUtils.copyFile(new File("src/test/resources/scraper_config.conf.tmpl"), getWorkFolder().resolve("scraper_config.conf").toFile());
  }

  @Test
  public void getSettings() throws IOException {

    MediaProviderInfo mpi = new MediaProviderInfo("config", "sub", "name", "description");

    // define defaults
    mpi.getConfig().addBoolean("filterUnwantedCategories", false);
    mpi.getConfig().addBoolean("useTmdbForMovies", false);
    mpi.getConfig().addBoolean("scrapeCollectionInfo", true);
    mpi.getConfig().addBoolean("someBool", true);
    mpi.getConfig().addText("someInput", "none");
    mpi.getConfig().addSelect("language", new String[] { "aa", "bb", "cc", "dd", "ee" }, "dd");
    mpi.getConfig()
        .addSelectIndex("languageInt", "bg|cs|da|de|el|en|es|fi|fr|he|hr|hu|it|ja|ko|nb|nl|no|pl|pt|ro|ru|sk|sl|sr|sv|th|tr|uk|zh".split("\\|"),
            "en");
    mpi.getConfig().addText("encrypted", "This is some encrypted text", true);

    // check default settings
    assertEqual(false, mpi.getConfig().getValueAsBool("filterUnwantedCategories"));
    assertEqual(false, mpi.getConfig().getValueAsBool("useTmdbForMovies"));
    assertEqual(true, mpi.getConfig().getValueAsBool("scrapeCollectionInfo"));
    assertEqual("dd", mpi.getConfig().getValue("language"));
    assertEqual("5", mpi.getConfig().getValue("languageInt"));

    // load actual values
    mpi.getConfig().loadFromDir(getWorkFolder().toString());

    // tests
    mpi.getConfig().setValue("someBool", false);
    mpi.getConfig().setValue("language", "bb");
    assertEqual("bb", mpi.getConfig().getValue("language"));
    mpi.getConfig().setValue("language", "ff");
    assertEqual("bb", mpi.getConfig().getValue("language")); // value not in rage, should stay at last known

    mpi.getConfig().setValue("languageInt", "de"); // set correct as string
    assertEqual("3", mpi.getConfig().getValue("languageInt")); // will return a int (string) index
    assertEqual(3, mpi.getConfig().getValueIndex("languageInt"));

    mpi.getConfig().setValue("languageInt", "unknown"); // not possible
    assertEqual("5", mpi.getConfig().getValue("languageInt")); // value not in rage, should stay at last known

    assertEqual(false, mpi.getConfig().getValueAsBool("languageInt")); // not a bool value
    assertEqual(true, mpi.getConfig().getValueAsBool("useTmdbForMovies"));
    assertEqual("This is some encrypted text", mpi.getConfig().getValue("encrypted"));

    System.out.println("--- current settings ---");
    System.out.println(mpi.getConfig());
    mpi.getConfig().saveToDir(getWorkFolder().toString());
  }

  @Test
  public void invalid() {
    MediaProviderInfo mpi = new MediaProviderInfo("save", "sub", "name", "description");
    mpi.getConfig().addBoolean("bool1", false);
    mpi.getConfig().addText("someInput", "none");
    mpi.getConfig().addSelect("language", new String[] { "aa", "bb", "cc", "dd", "ee" }, "dd");
    mpi.getConfig().addSelectIndex("languageInt", "bg|cs|da|de|el|en|es".split("\\|"), "en");

    mpi.getConfig().addSelect("invalid", new String[] { "aa", "bb", "cc", "dd", "ee" }, "invalidEntry");
    mpi.getConfig().addSelectIndex("invalidInt", "bg|cs|da|de|el|en|es".split("\\|"), "invalidEntry");

    mpi.getConfig().setValue("asdfasdfasdfasdf", true);
    assertEqual("", mpi.getConfig().getValue("invalid"));
    assertEqual(false, mpi.getConfig().getValueAsBool("invalid"));
    assertEqual("", mpi.getConfig().getValue("invalidInt"));
  }

  @Test
  public void emptySettingsLoadSave() {
    MediaProviderInfo mpi = new MediaProviderInfo("asdfasdf", "sub", "name", "description");
    mpi.getConfig().load();
    mpi.getConfig().saveToDir(getWorkFolder().toString());
  }

  @Test
  public void unknownConfigLoadSave() {
    MediaProviderInfo mpi = new MediaProviderInfo("asdfasdf", "sub", "name", "description");
    mpi.getConfig().addText("language", "de");
    mpi.getConfig().load();
    mpi.getConfig().saveToDir(getWorkFolder().toString());
  }

  @Test
  public void getUnknownValue() {
    MediaProviderInfo mpi = new MediaProviderInfo("config", "config", "name", "description");
    mpi.getConfig().loadFromDir(getWorkFolder().toString());
    assertEqual("", mpi.getConfig().getValue("asdfasdfasdfasdf"));
    assertEqual(false, mpi.getConfig().getValueAsBool("sdfgsdfgsdfg"));
  }

  @Test
  public void setNotAvailableConfig() {
    MediaProviderInfo mpi = new MediaProviderInfo("asdfasdf", "sub", "name", "description");
    mpi.getConfig().setValue("language", "de");
  }
}
