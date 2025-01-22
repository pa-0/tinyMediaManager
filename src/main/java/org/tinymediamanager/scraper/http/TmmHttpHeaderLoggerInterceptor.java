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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class is used to provide a HEADER logging interceptor <br>
 * Since the BrotliInterceptor needs to be the last/first to run, we have no chance to get all the headers accept-encoding headers added by brotli
 *
 * @author Myron Boyle
 */
public class TmmHttpHeaderLoggerInterceptor implements Interceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(TmmHttpHeaderLoggerInterceptor.class);

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    Headers headersRequest = request.headers();
    if (headersRequest.size() > 0) {
      LOGGER.trace("-> Headers: {}", headersRequest.toMultimap());
    }

    Response response = chain.proceed(request);
    Headers headersResponse = response.headers();
    if (headersResponse.size() > 0) {
      LOGGER.trace("<- Headers: {}", headersResponse.toMultimap());
    }

    return response;
  }
}
