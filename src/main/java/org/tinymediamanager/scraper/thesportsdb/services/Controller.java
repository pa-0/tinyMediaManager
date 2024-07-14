package org.tinymediamanager.scraper.thesportsdb.services;

import java.io.IOException;
import java.util.Date;

import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.thesportsdb.entities.league.LeagueEntity;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.internal.bind.DateTypeAdapter;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Controller {

  private final Retrofit retrofit;
  private String         apiKey;

  public Controller() {
    OkHttpClient.Builder builder = TmmHttpClient.newBuilder();
    builder.addInterceptor(chain -> {
      Request request = chain.request();
      Response response = chain.proceed(request);
      return response;
    });
    retrofit = buildRetrofitInstance(builder.build());
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  private GsonBuilder getGsonBuilder() {
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
    return builder;
  }

  public retrofit2.Response<LeagueEntity> getLeagues(String query) throws IOException {
    return getService().searchLeagues(apiKey,query).execute();
  }

  public retrofit2.Response<LeagueEntity> getLeagueInformation(String leagueId) throws IOException {
    return getService().getLeagueInformation(apiKey,leagueId).execute();
  }

  private TheSportsDbService getService() {
    return retrofit.create(TheSportsDbService.class);
  }

  private Retrofit buildRetrofitInstance(OkHttpClient client) {
    return new Retrofit.Builder().client(client)
        .baseUrl("https://www.thesportsdb.com/api/v2/json/")
        .addConverterFactory(GsonConverterFactory.create(getGsonBuilder().create()))
        .build();
  }

}
