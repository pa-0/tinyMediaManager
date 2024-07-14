package org.tinymediamanager.scraper.thesportsdb;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.thesportsdb.entities.league.LeagueEntity;
import org.tinymediamanager.scraper.thesportsdb.entities.league.Leagues;
import org.tinymediamanager.scraper.thesportsdb.services.Controller;
import org.tinymediamanager.scraper.util.StrgUtils;

import retrofit2.Response;

public class SportsDbTvShowMetadataProvider extends SportsDbMetadataProvider implements ITvShowMetadataProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(SportsDbMetadataProvider.class);

  protected Controller        controller;

  // thread safe initialization of the API
  protected synchronized void initAPI() throws ScrapeException {

    // create a new instance of the omdb api
    if (controller == null) {
      if (!isActive()) {
        throw new ScrapeException(new FeatureNotEnabledException(this));
      }
      controller = new Controller();
    }

    try {
      controller.setApiKey(getApiKey());
    }
    catch (Exception e) {
      throw new ScrapeException(e);
    }
  }

  @Override
  protected String getSubId() {
    return "tvshow";
  }

  @Override
  public boolean isActive() {
    return isFeatureEnabled();
  }

  @Override
  public MediaMetadata getMetadata(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {

    MediaMetadata md = new MediaMetadata(getId());
    md.setScrapeOptions(options);

    // get the "theSportsDb" ID to make a detailed scrape
    String id = options.getIdAsString("thesportsdb");
    LOGGER.debug("search detail information for League ID: {}", id);

    // Fetch data using the "league" id
    try {
      Response<LeagueEntity> response = controller.getLeagueInformation(id);
      if (response.body() != null) {
        Leagues league = response.body().leagues.get(0);

        //IDs
        md.setId("leagueID",league.idLeague);
        md.setId("cupID",league.idCup);
        md.setTitle(league.strLeague);
        md.setOriginalTitle(league.strLeague);
        md.setYear(Integer.parseInt(league.intFormedYear));
        md.setPlot(setPlot(options.getLanguage(),league));
        md.setPlot(league.strDescriptionDE);
        md.setReleaseDate(StrgUtils.parseDate(league.dateFirstEvent));
        List<MediaGenres> mediaGenres = new ArrayList<>();
        mediaGenres.add(MediaGenres.getGenre(league.strSport));
        md.setGenres(mediaGenres);
        md.setStatus(league.strComplete);
        md.setCountries(Arrays.asList(league.strCountry));
        md.setProductionCompanies(Arrays.asList(league.strTvRights));

        //Artwork
        MediaArtwork poster = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.POSTER);
        poster.setOriginalUrl(league.strPoster);
        md.addMediaArt(poster);

        MediaArtwork fanart = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.SEASON_FANART);
        fanart.setOriginalUrl(league.strFanart1);
        fanart.setPreviewUrl(league.strFanart1);

        MediaArtwork banner = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.BANNER);
        banner.setOriginalUrl(league.strBanner);
        banner.setPreviewUrl(league.strBanner);
        md.addMediaArt(banner);

        return md;
      }
    }
    catch (IOException | ParseException e) {
      LOGGER.error("error scraping: {} ", e.getMessage());
      throw new ScrapeException(e);
    }
    return md;
  }

  private String setPlot(MediaLanguages language, Leagues league) {
    switch (language) {
      case de -> {
        return league.strDescriptionDE;
      }
      default -> {
        return league.strDescriptionEN;
      }
    }
  }

  @Override
  public MediaMetadata getMetadata(@NotNull TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    return null;
  }

  @Override
  public SortedSet<MediaSearchResult> search(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {

    LOGGER.debug("search(): {}", options);
    initAPI();
    SortedSet<MediaSearchResult> mediaResult = new TreeSet<>();
    List<LeagueEntity> searchResult = new ArrayList<>();
    LOGGER.info("========= BEGIN TheSportsDB Scraper Search for League: {} ", options.getSearchQuery());

    try {
      Response<LeagueEntity> response = controller.getLeagues(options.getSearchQuery());

      if (response.isSuccessful()) {
        searchResult.add(response.body());
      }

    }
    catch (IOException e) {
      LOGGER.error("error searching: {} ", e.getMessage());
      throw new ScrapeException(e);
    }

    if (searchResult.isEmpty() || searchResult.get(0).leagues == null) {
      LOGGER.warn("no result from theSportsDB");
      return mediaResult;
    }

    for (LeagueEntity leagueEntity : searchResult) {
      for (Leagues league : leagueEntity.leagues) {

        MediaSearchResult result = new MediaSearchResult(getId(), MediaType.TV_SHOW);
        result.setTitle(league.strLeague);
        result.setPosterUrl(league.strBadge);
        result.setYear(Integer.parseInt(league.strCurrentSeason.substring(0, 4)));
        result.setId(String.valueOf(league.idLeague));

        mediaResult.add(result);
      }
    }
    return mediaResult;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {
    return List.of();
  }

}
