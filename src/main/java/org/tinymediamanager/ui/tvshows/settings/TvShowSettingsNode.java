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

package org.tinymediamanager.ui.tvshows.settings;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.settings.TmmSettingsNode;

/**
 * the class {@link TvShowSettingsNode} provides all settings pages
 *
 * @author Manuel Laggner
 */
public class TvShowSettingsNode extends TmmSettingsNode {

  public TvShowSettingsNode() {
    super(TmmResourceBundle.getString("Settings.tvshow"), new TvShowSettingsPanel());
    setBoldText(true);

    addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.ui"), new TvShowUiSettingsPanel()));
    addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.source"), new TvShowDatasourceSettingsPanel()));
    addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.nfo"), new TvShowScraperNfoSettingsPanel()));

    TmmSettingsNode scraperSettingsNode = new TmmSettingsNode(TmmResourceBundle.getString("Settings.scraper"), new TvShowScraperSettingsPanel());
    scraperSettingsNode
        .addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.scraper.options"), new TvShowScraperOptionsSettingsPanel()));
    addChild(scraperSettingsNode);

    TmmSettingsNode imageSettingsNode = new TmmSettingsNode(TmmResourceBundle.getString("Settings.images"), new TvShowImageSettingsPanel());
    imageSettingsNode.addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.scraper.options"), new TvShowImageOptionsSettingsPanel()));
    imageSettingsNode.addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.artwork.naming"), new TvShowImageTypeSettingsPanel()));
    imageSettingsNode.addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.extraartwork"), new TvShowImageExtraPanel()));
    addChild(imageSettingsNode);

    TmmSettingsNode trailerSettingsNode = new TmmSettingsNode(TmmResourceBundle.getString("Settings.trailer"), new TvShowTrailerSettingsPanel());
    trailerSettingsNode
        .addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.scraper.options"), new TvShowTrailerOptionsSettingsPanel()));
    addChild(trailerSettingsNode);

    TmmSettingsNode subtitleSettingsNode = new TmmSettingsNode(TmmResourceBundle.getString("Settings.subtitle"), new TvShowSubtitleSettingsPanel());
    subtitleSettingsNode
        .addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.scraper.options"), new TvShowSubtitleOptionsSettingsPanel()));
    addChild(subtitleSettingsNode);

    addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.renamer"), new TvShowRenamerSettingsPanel()));
    addChild(new TmmSettingsNode(TmmResourceBundle.getString("Settings.postprocessing"), new TvShowPostProcessingSettingsPanel()));
  }
}
