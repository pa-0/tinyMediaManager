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
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.Globals;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.ConnectionPool;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.brotli.BrotliInterceptor;

/**
 * The class HttpClient. To construct our HTTP client for internet access
 * 
 * @author Manuel Laggner
 * @since 1.0
 */
public class TmmHttpClient {
  private static final Cache  CACHE  = new Cache(Paths.get(Globals.CACHE_FOLDER, "http").toFile(), 25L * 1024 * 1024);
  private static OkHttpClient client = createHttpClient();

  private TmmHttpClient() {
    throw new IllegalAccessError();
  }

  /**
   * instantiates a new OkHttpClient
   * 
   * @return OkHttpClient
   */
  private static OkHttpClient createHttpClient() {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();

    // add an own logging interceptor to only log text responses
    builder.addInterceptor(new TmmHttpLoggingInterceptor());
    // NEEDS TO BE SECOND LAST, TO AUTO DECODE BROTLI & GZIP
    builder.addInterceptor(BrotliInterceptor.INSTANCE);
    // NEEDS TO BE LAST, TO CATCH ALL THE HEADERS ADDED BY BROTLI
    builder.addInterceptor(new TmmHttpHeaderLoggerInterceptor());

    // pool
    builder.connectionPool(new ConnectionPool(5, 5000, TimeUnit.MILLISECONDS));

    // timeouts
    builder.connectTimeout(60, TimeUnit.SECONDS);
    builder.writeTimeout(60, TimeUnit.SECONDS);
    builder.readTimeout(60, TimeUnit.SECONDS);

    // proxy
    if ((ProxySettings.INSTANCE.useProxy())) {
      setProxy(builder);
    }

    // accept untrusted/self signed SSL certs
    if (Boolean.parseBoolean(System.getProperty("tmm.trustallcerts", "false"))) {
      try {
        final TrustManager[] trustAllCerts = new TrustManager[] { new X509ExtendedTrustManager() {
          @Override
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }

          @Override
          public void checkServerTrusted(final X509Certificate[] chain, final String authType) { // NOSONAR
            // not needed
          }

          @Override
          public void checkClientTrusted(final X509Certificate[] chain, final String authType) { // NOSONAR
            // not needed
          }

          @Override
          public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            // not needed
          }

          @Override
          public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            // not needed
          }

          @Override
          public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            // not needed
          }

          @Override
          public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            // not needed
          }
        } };
        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL"); // NOSONAR
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
      }
      catch (Exception ignored) {
        // not needed
      }
    }

    return builder.build();
  }

  /**
   * create a new OkHttpClient.Builder along with all our settings set
   *
   * @return the newly created builder
   */
  public static OkHttpClient.Builder newBuilder() {
    return newBuilder(false);
  }

  /**
   * create a new OkHttpClient.Builder WITH cache along with all our settings set
   * 
   * @param withCache
   *          create the builder with a cache set, if the server allows to cache it
   * @return the newly created builder
   */
  public static OkHttpClient.Builder newBuilder(boolean withCache) {
    OkHttpClient.Builder builder = client.newBuilder();

    if (withCache) {
      builder = builder.cache(CACHE);
    }

    return builder;
  }

  /**
   * create a new OkHttpClient.Builder with a FORCED cache (overwriting any cache response headers of the HTTP request)
   * 
   * @param timeToLive
   *          the time to live
   * @param timeUnit
   *          the time unit for the TTL
   * @return a new {@link OkHttpClient.Builder} with the given TTL settings
   */
  public static OkHttpClient.Builder newBuilderWithForcedCache(final int timeToLive, final TimeUnit timeUnit) {
    return newBuilder(true).addNetworkInterceptor(provideCacheInterceptor(timeToLive, timeUnit));
  }

  /**
   * add an interceptor which sets the header field
   * 
   * @param timeToLive
   *          the time to live in the cache
   * @param timeUnit
   *          the time unit
   * @return the interceptor
   */
  private static Interceptor provideCacheInterceptor(final int timeToLive, final TimeUnit timeUnit) {
    return chain -> {
      Response response = chain.proceed(chain.request());
      // add caching header only if it is not there! (and override some no-no's)
      String head = response.header("cache-control");

      if (head == null || head.contains("no-store") || head.contains("no-cache") || head.contains("must-revalidate") || head.contains("max-age=0")) {
        CacheControl cacheControl = new CacheControl.Builder().maxAge(timeToLive, timeUnit).build();
        return response.newBuilder()
            .removeHeader("x-amz-cf-id") // TVDB, prevent caching (304)
            .removeHeader("Pragma")
            .removeHeader("Cache-Control")
            .header("Cache-Control", cacheControl.toString())
            .build();
      }
      return response;
    };
  }

  /**
   * Gets the pre-configured http client.
   * 
   * @return the http client
   */
  public static OkHttpClient getHttpClient() {
    return client;
  }

  /**
   * re-create the http client due to settings changes
   */
  public static void recreateHttpClient() {
    // recreate a new client instance
    client = createHttpClient();
  }

  private static void setProxy(OkHttpClient.Builder builder) {
    Proxy proxyHost;

    if (ProxySettings.INSTANCE.getPort() > 0) {
      proxyHost = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(ProxySettings.INSTANCE.getHost(), ProxySettings.INSTANCE.getPort()));
    }
    else if (StringUtils.isNotBlank(ProxySettings.INSTANCE.getHost())) {
      proxyHost = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(ProxySettings.INSTANCE.getHost(), 80));
    }
    else {
      // no proxy settings found. return
      return;
    }

    builder.proxy(proxyHost);
    // authenticate
    if (StringUtils.isNotBlank(ProxySettings.INSTANCE.getUsername()) && StringUtils.isNotBlank(ProxySettings.INSTANCE.getPassword())) {
      builder.proxyAuthenticator((route, response) -> {
        String credential = Credentials.basic(ProxySettings.INSTANCE.getUsername(), ProxySettings.INSTANCE.getPassword());
        return response.request().newBuilder().header("Proxy-Authorization", credential).build();
      });
    }
  }

  /**
   * Closes the cache and deletes all of its stored values. This will delete all files in the cache directory including files that weren't created by
   * the cache.
   * 
   * @throws IOException
   *           any {@link IOException} occurred while deleting the cache
   */
  public static void clearCache() throws IOException {
    CACHE.evictAll();
  }
}
