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

import static org.tinymediamanager.scraper.tmdb.enumerations.MediaType.PERSON;
import static org.tinymediamanager.scraper.tmdb.enumerations.MediaType.TV;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.tmdb.entities.BaseMovie;
import org.tinymediamanager.scraper.tmdb.entities.BasePerson;
import org.tinymediamanager.scraper.tmdb.entities.BaseTvShow;
import org.tinymediamanager.scraper.tmdb.entities.Media;
import org.tinymediamanager.scraper.tmdb.entities.PersonCastCredit;
import org.tinymediamanager.scraper.tmdb.entities.PersonCrewCredit;
import org.tinymediamanager.scraper.tmdb.enumerations.MediaType;
import org.tinymediamanager.scraper.tmdb.enumerations.Status;
import org.tinymediamanager.scraper.tmdb.enumerations.VideoType;
import org.tinymediamanager.scraper.tmdb.services.CollectionsService;
import org.tinymediamanager.scraper.tmdb.services.ConfigurationService;
import org.tinymediamanager.scraper.tmdb.services.FindService;
import org.tinymediamanager.scraper.tmdb.services.MoviesService;
import org.tinymediamanager.scraper.tmdb.services.SearchService;
import org.tinymediamanager.scraper.tmdb.services.TvEpisodeGroupsService;
import org.tinymediamanager.scraper.tmdb.services.TvEpisodesService;
import org.tinymediamanager.scraper.tmdb.services.TvSeasonsService;
import org.tinymediamanager.scraper.tmdb.services.TvService;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Helper class for easy usage of the TMDB v3 API using retrofit.
 */
class TmdbController {
  public static final String     API_HOST           = "api.themoviedb.org";
  public static final String     ALTERNATE_API_HOST = "api.tmdb.org";
  public static final String     API_VERSION        = "3";
  public static final String     PARAM_API_KEY      = "api_key";
  private static final String    TMDB_DATE_PATTERN  = "yyyy-MM-dd";

  private final String           apiUrl;
  private final SimpleDateFormat dateFormat;
  private final String           apiKey;
  private final boolean          alternateServer;

  private Retrofit               retrofit;

  TmdbController(String apiKey, boolean alternateServer) {
    this.apiKey = apiKey;
    this.alternateServer = alternateServer;

    if (alternateServer) {
      apiUrl = "https://" + ALTERNATE_API_HOST + "/" + API_VERSION + "/";
    }
    else {
      apiUrl = "https://" + API_HOST + "/" + API_VERSION + "/";
    }

    this.dateFormat = new SimpleDateFormat(TMDB_DATE_PATTERN);
  }

  public String apiKey() {
    return apiKey;
  }

  public boolean isAlternateServer() {
    return alternateServer;
  }

  protected Retrofit getRetrofit() {
    if (retrofit == null) {
      retrofit = new Retrofit.Builder().baseUrl(apiUrl)
          .addConverterFactory(GsonConverterFactory.create(getGsonBuilder().create()))
          .client(okHttpClient())
          .build();
    }
    return retrofit;
  }

  protected synchronized OkHttpClient okHttpClient() {
    // use the tmm internal okhttp client
    OkHttpClient.Builder builder = TmmHttpClient.newBuilderWithForcedCache(15, TimeUnit.MINUTES);
    builder.connectTimeout(30, TimeUnit.SECONDS);
    builder.writeTimeout(30, TimeUnit.SECONDS);
    builder.readTimeout(30, TimeUnit.SECONDS);
    builder.addInterceptor(new TmdbInterceptor(this));
    return builder.build();
  }

  protected GsonBuilder getGsonBuilder() {
    GsonBuilder builder = new GsonBuilder();

    // class types
    builder.registerTypeAdapter(Integer.class, (JsonDeserializer<Integer>) (json, typeOfT, context) -> json.getAsInt());

    builder.registerTypeAdapter(MediaType.class, (JsonDeserializer<MediaType>) (json, typeOfT, context) -> MediaType.get(json.getAsString()));

    builder.registerTypeAdapter(VideoType.class, (JsonDeserializer<VideoType>) (json, typeOfT, context) -> VideoType.get(json.getAsString()));

    builder.registerTypeAdapter(Media.class, (JsonDeserializer<Media>) (jsonElement, type, jsonDeserializationContext) -> {
      Media media = new Media();
      if (jsonElement.getAsJsonObject().get("media_type") != null) {
        media.media_type = jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("media_type"), MediaType.class);
      }
      else {
        if (jsonElement.getAsJsonObject().get("first_air_date") != null) {
          media.media_type = TV;
        }
        else if (jsonElement.getAsJsonObject().get("name") != null) {
          media.media_type = PERSON;
        }
        else if (jsonElement.getAsJsonObject().get("title") != null) {
          media.media_type = MediaType.MOVIE;
        }
      }

      switch (media.media_type) {
        case MOVIE:
          media.movie = jsonDeserializationContext.deserialize(jsonElement, BaseMovie.class);
          break;

        case TV:
          media.tvShow = jsonDeserializationContext.deserialize(jsonElement, BaseTvShow.class);
          break;

        case PERSON:
          media.person = jsonDeserializationContext.deserialize(jsonElement, BasePerson.class);
          break;
      }

      return media;
    });

    builder.registerTypeAdapter(PersonCastCredit.class, (JsonDeserializer<PersonCastCredit>) (jsonElement, type, jsonDeserializationContext) -> {
      PersonCastCredit personCastCredit = new PersonCastCredit();
      personCastCredit.media = jsonDeserializationContext.deserialize(jsonElement, Media.class);
      JsonElement character = jsonElement.getAsJsonObject().get("character");
      if (character != null) {
        personCastCredit.character = character.getAsString();
      }
      JsonElement creditId = jsonElement.getAsJsonObject().get("credit_id");
      if (creditId != null) {
        personCastCredit.credit_id = creditId.getAsString();
      }
      if (personCastCredit.media.media_type == TV) {
        personCastCredit.episode_count = jsonElement.getAsJsonObject().get("episode_count").getAsInt();
      }

      return personCastCredit;
    });

    builder.registerTypeAdapter(PersonCrewCredit.class, (JsonDeserializer<PersonCrewCredit>) (jsonElement, type, jsonDeserializationContext) -> {
      PersonCrewCredit personCrewCredit = new PersonCrewCredit();
      personCrewCredit.media = jsonDeserializationContext.deserialize(jsonElement, Media.class);
      personCrewCredit.department = jsonElement.getAsJsonObject().get("department").getAsString();
      personCrewCredit.job = jsonElement.getAsJsonObject().get("job").getAsString();
      personCrewCredit.credit_id = jsonElement.getAsJsonObject().get("credit_id").getAsString();
      if (personCrewCredit.media.media_type == TV) {
        if (jsonElement.getAsJsonObject().get("episode_count") != null) {
          personCrewCredit.episode_count = jsonElement.getAsJsonObject().get("episode_count").getAsInt();
        }
      }
      return personCrewCredit;
    });

    builder.registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> {
      try {
        return dateFormat.parse(json.getAsString());
      }
      catch (ParseException e) {
        // return null instead of failing (like default parser would)
        return null;
      }
    });

    builder.registerTypeAdapter(Status.class, (JsonDeserializer<Status>) (jsonElement, type, jsonDeserializationContext) -> {
      String value = jsonElement.getAsString();
      if (value != null) {
        return Status.fromValue(value);
      }
      else {
        return null;
      }
    });

    return builder;
  }

  CollectionsService collectionService() {
    return getRetrofit().create(CollectionsService.class);
  }

  ConfigurationService configurationService() {
    return getRetrofit().create(ConfigurationService.class);
  }

  FindService findService() {
    return getRetrofit().create(FindService.class);
  }

  MoviesService moviesService() {
    return getRetrofit().create(MoviesService.class);
  }

  SearchService searchService() {
    return getRetrofit().create(SearchService.class);
  }

  TvService tvService() {
    return getRetrofit().create(TvService.class);
  }

  TvSeasonsService tvSeasonsService() {
    return getRetrofit().create(TvSeasonsService.class);
  }

  TvEpisodesService tvEpisodesService() {
    return getRetrofit().create(TvEpisodesService.class);
  }

  TvEpisodeGroupsService tvEpisodeGroupsService() {
    return getRetrofit().create(TvEpisodeGroupsService.class);
  }
}
