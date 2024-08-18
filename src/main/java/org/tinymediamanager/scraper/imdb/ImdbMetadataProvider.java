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
package org.tinymediamanager.scraper.imdb;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;

/**
 * The public ImdbMetadataProvider() {
 * 
 * }Class ImdbMetadataProvider. A meta data provider for the site imdb.com
 *
 * @author Manuel Laggner
 */
abstract class ImdbMetadataProvider implements IMediaProvider {
  protected static final ExecutorService EXECUTOR = new ThreadPoolExecutor(5, 10, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
  static final String                    ID       = "imdb";
  private final MediaProviderInfo        providerInfo;

  ImdbMetadataProvider() {
    providerInfo = createMediaProviderInfo();
  }

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  protected MediaProviderInfo createMediaProviderInfo() {
    return new MediaProviderInfo(ID, getSubId(), "IMDb.com",
        "<html><h3>Internet Movie Database (IMDb)</h3><br />The most used database for movies all over the world.<br />Does not contain plot/title/tagline in every language. You may choose to download these texts from other scrapers<br /><br />Available languages: multiple</html>",
        ImdbMetadataProvider.class.getResource("/org/tinymediamanager/scraper/imdb_com.svg"), 10);
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  @Override
  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(null);
  }
}
