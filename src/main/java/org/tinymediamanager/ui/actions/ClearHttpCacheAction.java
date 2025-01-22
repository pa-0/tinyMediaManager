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
package org.tinymediamanager.ui.actions;

import java.awt.event.ActionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.scraper.http.InMemoryCachedUrl;
import org.tinymediamanager.scraper.http.TmmHttpClient;

/**
 * The ClearHttpCacheAction is used to completely clear the HTTP cache
 * 
 * @author Manuel Laggner
 */
public class ClearHttpCacheAction extends TmmAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClearHttpCacheAction.class);

  public ClearHttpCacheAction() {
    putValue(NAME, TmmResourceBundle.getString("tmm.clearhttpcache"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tmm.clearhttpcache"));
  }

  @Override
  protected void processAction(ActionEvent arg0) {
    try {
      TmmHttpClient.clearCache();
      InMemoryCachedUrl.clearCache();
    }
    catch (Exception e) {
      LOGGER.warn("could not delete HTTP cache: {}", e.getMessage());
    }
  }
}
