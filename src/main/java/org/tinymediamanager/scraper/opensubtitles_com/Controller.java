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
package org.tinymediamanager.scraper.opensubtitles_com;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.opensubtitles_com.model.LoginRequest;
import org.tinymediamanager.scraper.opensubtitles_com.model.LoginResponse;
import org.tinymediamanager.scraper.opensubtitles_com.service.Opensubtitles2Service;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.internal.bind.DateTypeAdapter;

import okhttp3.Request;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

class Controller {
  private static final String DEFAULT_API_URL     = "https://api.opensubtitles.com";
  private static final String PARAM_API_KEY       = "Api-Key";
  private static final String PARAM_AUTHORIZATION = "Authorization";

  private Retrofit            restAdapter;

  private String              apiKey;
  private String              apiUrl              = DEFAULT_API_URL;
  private String              username;
  private String              password;
  private String              token;

  Controller() {
    apiKey = "";
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public void setUsername(String username) {
    if (StringUtils.compare(username, this.username) != 0) {
      token = "";
    }
    this.username = username;
  }

  public void setPassword(String password) {
    if (StringUtils.compare(password, this.password) != 0) {
      token = "";
    }
    this.password = password;
  }

  public void authenticate() throws ScrapeException {
    if (StringUtils.isNoneBlank(username, password) && StringUtils.isBlank(token)) {
      LoginRequest loginRequest = new LoginRequest();
      loginRequest.username = username;
      loginRequest.password = password;

      try {
        Response<LoginResponse> response = getService().login(loginRequest).execute();
        if (!response.isSuccessful() || response.body() == null || response.body().status != 200) {
          int code = response.body() != null ? response.body().status : response.code();
          throw new HttpException(code, "Could not logon to Opensubtitles.com");
        }

        LoginResponse resp = response.body();
        this.token = resp.token;
        this.apiUrl = resp.baseUrl;
        if (!this.apiUrl.startsWith("http")) {
          // dunno if host ALWAYS comes wo scheme
          this.apiUrl = "https://" + this.apiUrl;
        }
        this.restAdapter = null; // to recreate this with NEW baseUrl!
      }
      catch (IOException e) {
        throw new ScrapeException(e);
      }
    }
  }

  protected Retrofit getRestAdapter() {
    if (restAdapter == null) {
      Retrofit.Builder builder = new Retrofit.Builder();
      builder.baseUrl(apiUrl);
      builder.addConverterFactory(GsonConverterFactory.create(getGsonBuilder().create()));
      builder.client(TmmHttpClient.newBuilder().addInterceptor(chain -> {
        Request original = chain.request();
        Request.Builder request = original.newBuilder().method(original.method(), original.body());
        request.addHeader(PARAM_API_KEY, apiKey);
        if (StringUtils.isNotBlank(token)) {
          request.addHeader(PARAM_AUTHORIZATION, "Bearer " + token);
        }
        request.header("User-Agent", "tinyMediaManager v5");
        return chain.proceed(request.build());
      }).build());
      restAdapter = builder.build();
    }
    return restAdapter;
  }

  protected GsonBuilder getGsonBuilder() {
    GsonBuilder builder = new GsonBuilder();
    // class types
    builder.registerTypeAdapter(Integer.class, (JsonDeserializer<Integer>) (json, typeOfT, context) -> {
      try {
        return json.getAsInt();
      }
      catch (NumberFormatException e) {
        return 0;
      }
    });
    builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
    builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
    return builder;
  }

  Opensubtitles2Service getService() {
    return getRestAdapter().create(Opensubtitles2Service.class);
  }
}
