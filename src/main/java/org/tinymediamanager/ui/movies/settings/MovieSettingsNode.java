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

package org.tinymediamanager.ui.movies.settings;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.settings.TmmSettingsNode;

/**
 * the class {@link MovieSettingsNode} provides all settings pages
 * 
 * @author Manuel Laggner
 */
public class MovieSettingsNode extends TmmSettingsNode {

  public MovieSettingsNode() {
    super(TmmResourceBundle.getString("Settings.movies"), new MovieSettingsPanel());
    setBoldText(true);

    addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.ui"), new MovieUiSettingsPanel()));
    addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.source"), new MovieDatasourceSettingsPanel()));
    addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.nfo"), new MovieScraperNfoSettingsPanel()));

    TmmSettingsNode scraperSettingsNode = new TmmSettingsNode(TmmResourceBundle.getString("scraper.metadata"), new MovieScraperSettingsPanel());
    scraperSettingsNode
        .addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.advancedoptions"), new MovieScraperOptionsSettingsPanel()));
    addChild(scraperSettingsNode);

    TmmSettingsNode imageSettingsNode = new TmmSettingsNode(TmmResourceBundle.getString("scraper.artwork"), new MovieImageSettingsPanel());
    imageSettingsNode.addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.advancedoptions"), new MovieImageOptionsSettingsPanel()));
    imageSettingsNode.addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.artwork.naming"), new MovieImageTypeSettingsPanel()));
    imageSettingsNode.addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.extraartwork"), new MovieImageExtraPanel()));
    addChild(imageSettingsNode);

    TmmSettingsNode trailerSettingsNode = new TmmSettingsNode(TmmResourceBundle.getString("scraper.trailer"), new MovieTrailerSettingsPanel());
    trailerSettingsNode
        .addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.advancedoptions"), new MovieTrailerOptionsSettingsPanel()));
    addChild(trailerSettingsNode);

    TmmSettingsNode subtitleSettingsNode = new TmmSettingsNode(TmmResourceBundle.getString("scraper.subtitle"), new MovieSubtitleSettingsPanel());
    subtitleSettingsNode
        .addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.advancedoptions"), new MovieSubtitleOptionsSettingsPanel()));
    addChild(subtitleSettingsNode);

    addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.renamer"), new MovieRenamerSettingsPanel()));
    addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.postprocessing"), new MoviePostProcessingSettingsPanel()));
  }
}
