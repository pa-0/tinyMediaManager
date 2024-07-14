package org.tinymediamanager.scraper.thesportsdb.services;

import org.tinymediamanager.scraper.thesportsdb.entities.event.EventEntity;
import org.tinymediamanager.scraper.thesportsdb.entities.league.LeagueEntity;

import org.tinymediamanager.scraper.thesportsdb.entities.player.PlayerEntity;
import org.tinymediamanager.scraper.thesportsdb.entities.season.SeasonEntity;
import org.tinymediamanager.scraper.thesportsdb.entities.season_poster.SeasonPosterEntity;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface TheSportsDbService {

  //"Tv Show" Search
  @GET("{apikey}/search/league/{query}")
  Call<LeagueEntity> searchLeagues(@Path("apikey") String apikey, @Path("query") String searchQuery);

  //"Tv Show" Information
  @GET("{apikey}/lookup/league/{leagueId}")
  Call<LeagueEntity> getLeagueInformation(@Path("apikey") String apikey, @Path("leagueId") String leagueId);

  //Get all Seasons
  @GET("{apikey}/list/seasons/{leagueId}")
  Call<SeasonEntity> getSeasonInformation(@Path("apikey") String apikey, @Path("leagueId") String leagueId);

  //Get Season Posters
  @GET("{apikey}/list/seasonposters/{leagueId}")
  Call<SeasonPosterEntity> getSeasonPoster(@Path("apikey") String apikey, @Path("leagueId") String leagueId);
  
  //Get single Episodes "events"
  @GET("{apikey}/filter/events/{leagueId}/{seasonId}")
  Call<EventEntity> getEvents(@Path("apikey") String apikey, @Path("seasonId") String seasonId);

  //Get Player Information
  @GET("{apikey}/list/players/{teamId}")
  Call<PlayerEntity> getPlayerInformation(@Path("apikey") String apikey, @Path("teamId") String teamId);

}
