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
package org.tinymediamanager.scraper.mpdbtv;

import java.util.Base64;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.mpdbtv.services.Controller;

abstract class MpdbMetadataProvider implements IMediaProvider {

  static final String             ID     = "mpdbtv";
  static final String             FORMAT = "json";

  private final MediaProviderInfo providerInfo;

  protected Controller            controller;

  MpdbMetadataProvider() {
    providerInfo = createMediaProviderInfo();
  }

  protected abstract String getSubId();

  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "mpdb.tv",
        "<html><h3>MPDb.TV</h3><br />MPDb.TV is a private meta data provider for French speaking users - you may need to become a member there to use this service (more infos at http://www.mpdb.tv/)<br /><br />Available languages: FR</html>",
        MpdbMovieMetadataProvider.class.getResource("/org/tinymediamanager/scraper/mpdbtv.png"), -10);

    info.getConfig().addText("aboKey", "", false);
    info.getConfig().addText("username", "", false);
    info.getConfig().load();

    return info;
  }

  public boolean isActive() {
    return isFeatureEnabled() && StringUtils.isNoneBlank(getAboKey(), getUserName()) && isApiKeyAvailable(null);
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  // thread safe initialization of the API
  protected synchronized void initAPI() throws ScrapeException {

    // create a new instance of the omdb api
    if (controller == null) {
      if (!isActive()) {
        throw new ScrapeException(new FeatureNotEnabledException(this));
      }

      controller = new Controller();
    }

    try {
      controller.setApiKey(getApiKey());
    }
    catch (Exception e) {
      throw new ScrapeException(e);
    }
  }

  String getAboKey() {
    return providerInfo.getConfig().getValue("aboKey");
  }

  String getUserName() {
    return providerInfo.getConfig().getValue("username");
  }

  String getEncodedUserName() {
    return Base64.getUrlEncoder().encodeToString(getUserName().getBytes());
  }

  String getSubscriptionKey() throws Exception {
    return DigestUtils.sha1Hex(getUserName() + getApiKey() + getAboKey());
  }
}
