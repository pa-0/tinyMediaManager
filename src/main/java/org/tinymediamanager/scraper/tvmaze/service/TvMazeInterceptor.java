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

package org.tinymediamanager.scraper.tvmaze.service;

import java.io.IOException;

import javax.annotation.Nonnull;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * {@link Interceptor} to add the API key query parameter and if available session information. As it modifies the URL and may retry requests, ensure
 * this is added as an application interceptor (never a network interceptor), otherwise caching will be broken and requests will fail.
 */
public class TvMazeInterceptor implements Interceptor {

  private final Controller controller;

  public TvMazeInterceptor(Controller tmdbController) {
    this.controller = tmdbController;
  }

  @Override
  public Response intercept(@Nonnull Chain chain) throws IOException {
    return handleIntercept(chain, controller);
  }

  public static Response handleIntercept(Chain chain, Controller controller) throws IOException {
    Request request = chain.request();
    Response response = chain.proceed(request);

    // TmmHttpHeaderLoggerInterceptor:48 - <- Headers: {cache-control=[private], content-type=[application/json; charset=UTF-8], date=[Sat, 28 Sep
    // 2024 11:10:14 GMT], retry-after=[3], server=[nginx/1.24.0 (Ubuntu)]}
    // TmmHttpLoggingInterceptor:139 - <-- 429 https://api.tvmaze.com/shows/434?embed[]=seasons&embed[]=crew&embed[]=cast&embed[]=images (35ms,
    // unknown-length body)

    if (!response.isSuccessful()) {
      // re-try if the server indicates we should
      String retryHeader = response.header("Retry-After");
      if (retryHeader != null) {
        try {
          int retry = Integer.parseInt(retryHeader);
          Thread.sleep((int) ((retry + 0.5) * 1000));

          // close body of unsuccessful response
          if (response.body() != null) {
            response.body().close();
          }
          // is fine because, unlike a network interceptor, an application interceptor can re-try requests
          return handleIntercept(chain, controller);
        }
        catch (NumberFormatException | InterruptedException ignored) {
        }
      }
    }

    return response;
  }
}
