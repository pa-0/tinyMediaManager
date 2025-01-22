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

package org.tinymediamanager.scraper.http;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

/**
 * an implementation of the url to use a local disk cache
 * 
 * @author Manuel Laggner
 */
public class OnDiskCachedUrl extends Url {

  /**
   * create an instance of the {@link Url} using the default caching values (which respect the response headers of the HTTP request)
   * 
   * @param url
   *          the url to fetch
   * @throws MalformedURLException
   */
  public OnDiskCachedUrl(String url) throws MalformedURLException {
    super(url);
    client = TmmHttpClient.newBuilder(true).build();
  }

  /**
   * create an instance of the {@link Url} using the given caching values (overwriting any available response headers of the HTTP request)
   * 
   * @param url
   *          the url to fetch
   * @throws MalformedURLException
   */
  public OnDiskCachedUrl(String url, int timeToLive, TimeUnit timeUnit) throws MalformedURLException {
    super(url);
    client = TmmHttpClient.newBuilderWithForcedCache(timeToLive, timeUnit).build();
  }
}
