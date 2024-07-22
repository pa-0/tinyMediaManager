package org.tinymediamanager.scraper.subdl;

import java.io.IOException;
import java.util.Date;

import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.subdl.model.SubdlModel;
import org.tinymediamanager.scraper.subdl.service.SubdlService;

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
  private String         secretKey;

  public Controller() {
    OkHttpClient.Builder builder = TmmHttpClient.newBuilder();
    builder.addInterceptor(chain -> {
      Request request = chain.request();
      return chain.proceed(request);
    });
    retrofit = buildRetrofitInstance(builder.build());

  }

  public void setSecretKey(String apiKey) {
    this.secretKey = apiKey;
  }

  public String getSecretKey() {
    return secretKey;
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

  private Retrofit buildRetrofitInstance(OkHttpClient client) {
    return new Retrofit.Builder().client(client)
        .baseUrl("https://api.subdl.com/api/v1/")
        .addConverterFactory(GsonConverterFactory.create(getGsonBuilder().create()))
        .build();
  }

  private SubdlService getService() {
    return retrofit.create(SubdlService.class);
  }

  public retrofit2.Response<SubdlModel> getResults(String query) throws IOException {
    return getService().fetchResults(getSecretKey(), query, "movie").execute();
  }

  public retrofit2.Response<SubdlModel> getResultsFromImdbId(String imdbId, String language) throws IOException {
    return getService().fetchResultswithImdbId(getSecretKey(), imdbId, "movie", language).execute();
  }

  public retrofit2.Response<SubdlModel> getResultsFromTmdbId(int tmdbId, String language) throws IOException {
    return getService().fetchResultswithTmDbId(getSecretKey(), tmdbId, "movie", language).execute();
  }

}
