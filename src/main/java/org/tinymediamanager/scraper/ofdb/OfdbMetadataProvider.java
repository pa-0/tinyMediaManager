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
package org.tinymediamanager.scraper.ofdb;

import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.util.MetadataUtil;

/**
 * The Class OfdbMetadataProvider. A meta data provider for the site ofdb.de
 *
 * @author Myron Boyle (myron0815@gmx.net)
 */
abstract class OfdbMetadataProvider implements IMediaProvider {
  static final String             ID = "ofdb";

  private final MediaProviderInfo providerInfo;

  OfdbMetadataProvider() {
    providerInfo = createMediaProviderInfo();
  }

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  @Override
  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(null);
  }

  private MediaProviderInfo createMediaProviderInfo() {
    return new MediaProviderInfo(ID, getSubId(), "Online Filmdatenbank (OFDb.de)",
        "<html><h3>Online Filmdatenbank (OFDb)</h3><br />A german movie database driven by the community.<br /><br />Available languages: DE</html>",
        OfdbMetadataProvider.class.getResource("/org/tinymediamanager/scraper/ofdb_de.svg"));
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  /*
   * Removes all weird characters from search as well some "stopwords" as der|die|das|the|a
   */
  protected String cleanSearch(String q) {
    q = " " + MetadataUtil.removeNonSearchCharacters(q) + " "; // easier regex
    // nope - removing "the" would not find 'dawn of the dead'
    // q = q.replaceAll("(?i)( a | the | der | die | das |\\(\\d+\\))", " ");
    q = q.replaceAll("[^A-Za-z0-9äöüÄÖÜ ]", " ");
    q = q.replace("  ", "");
    return q.strip();
  }
}
