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

package org.tinymediamanager.scraper.tmdb;

import java.io.IOException;

import javax.annotation.Nonnull;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * {@link Interceptor} to add the API key query parameter and if available session information. As it modifies the URL and may retry requests, ensure
 * this is added as an application interceptor (never a network interceptor), otherwise caching will be broken and requests will fail.
 */
public class TmdbInterceptor implements Interceptor {

  private final TmdbController tmdbController;

  public TmdbInterceptor(TmdbController tmdbController) {
    this.tmdbController = tmdbController;
  }

  @Override
  public Response intercept(@Nonnull Chain chain) throws IOException {
    return handleIntercept(chain, tmdbController);
  }

  /**
   * If the host matches {@link TmdbController#API_HOST} adds a query parameter with the API key.
   */
  public static Response handleIntercept(Chain chain, TmdbController tmdbController) throws IOException {
    Request request = chain.request();

    if (!TmdbController.API_HOST.equals(request.url().host()) && !TmdbController.ALTERNATE_API_HOST.equals(request.url().host())) {
      // do not intercept requests for other hosts
      // this allows the interceptor to be used on a shared okhttp client
      return chain.proceed(request);
    }

    // add (or replace) the API key query parameter
    HttpUrl.Builder urlBuilder = request.url().newBuilder();
    urlBuilder.setEncodedQueryParameter(TmdbController.PARAM_API_KEY, tmdbController.apiKey());

    Request.Builder builder = request.newBuilder();
    builder.url(urlBuilder.build());
    Response response = chain.proceed(builder.build());

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
          return handleIntercept(chain, tmdbController);
        }
        catch (NumberFormatException | InterruptedException ignored) {
        }
      }
    }

    return response;
  }
}
