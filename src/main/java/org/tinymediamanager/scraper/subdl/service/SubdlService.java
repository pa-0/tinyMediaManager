package org.tinymediamanager.scraper.subdl.service;

import org.tinymediamanager.scraper.subdl.model.SubdlModel;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SubdlService {

  @GET("subtitles")
  Call<SubdlModel> fetchResults(@Query("api_key") String apiKey, @Query("film_name") String query, @Query("type") String type);

  @GET("subtitles")
  Call<SubdlModel> fetchResultswithImdbId(@Query("api_key") String apiKey, @Query("imdb_id") String imdbId, @Query("type") String type, @Query("languages") String languages);

  @GET("subtitles")
  Call<SubdlModel> fetchResultswithTmDbId(@Query("api_key") String apiKey, @Query("tmdb_id") int tmdbId, @Query("type") String type, @Query("languages") String languages);

}
