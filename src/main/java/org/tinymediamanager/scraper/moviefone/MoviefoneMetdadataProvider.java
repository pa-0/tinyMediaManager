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
package org.tinymediamanager.scraper.moviefone;

import org.slf4j.Logger;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;

/**
 * A simple trailer provider for the site moviefone.com
 *
 * @author Myron Boyle
 */

abstract class MoviefoneMetdadataProvider implements IMediaProvider {
  private static final String       ID = "moviefone";

  protected final MediaProviderInfo providerInfo;

  MoviefoneMetdadataProvider() {
    providerInfo = createMediaProviderInfo();
  }

  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "moviefone.com",
        "<html><h3>moviefone.com</h3>find it. watch it.<br>Scraper for moviefone.com which is able to scrape trailers<br />Available languages: EN</html>",
        MoviefoneMetdadataProvider.class.getResource("/org/tinymediamanager/scraper/moviefone_logo.svg"));

    info.getConfig().addBoolean("clips", "also scrape 'unscripted', 'bonus' and other 'clips'?", false);

    return info;
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  protected abstract Logger getLogger();

  @Override
  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(null);
  }
}
