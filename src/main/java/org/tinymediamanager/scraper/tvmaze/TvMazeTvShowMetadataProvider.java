package org.tinymediamanager.scraper.tvmaze;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.tvmaze.entities.Cast;
import org.tinymediamanager.scraper.tvmaze.entities.Episode;
import org.tinymediamanager.scraper.tvmaze.entities.Image;
import org.tinymediamanager.scraper.tvmaze.entities.Show;
import org.tinymediamanager.scraper.tvmaze.entities.Shows;

public class TvMazeTvShowMetadataProvider extends TvMazeMetadataProvider implements ITvShowMetadataProvider {

  private static final Logger LOGGER          = LoggerFactory.getLogger(TvMazeTvShowMetadataProvider.class);

  private final DateFormat    premieredFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

  @Override
  public MediaProviderInfo getProviderInfo() {
    return super.providerInfo;
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public MediaMetadata getMetadata(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    // lazy initialization of the api
    initAPI();

    MediaMetadata md = new MediaMetadata(getId());
    md.setScrapeOptions(options);

    // do we have an id from the options?
    int tvMazeId = options.getIdAsIntOrDefault(MediaMetadata.TVMAZE, 0);
    if (tvMazeId == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(MediaMetadata.TVMAZE);
    }

    Show show = null;
    List<Cast> castList;

    // We have to search with the internal tvmaze id here to get
    // all the information :)

    // get show information
    LOGGER.debug("========= BEGIN TVMAZE Scraping");
    try {
      show = controller.getMainInformation(tvMazeId);
    }
    catch (IOException e) {
      LOGGER.trace("could not get Main TvShow information: {}", e.getMessage());
    }

    if (show == null) {
      throw new NothingFoundException();
    }

    md.setId(MediaMetadata.TVMAZE, show.id);
    md.setId(MediaMetadata.IMDB, show.tvShowIds.imdb);
    md.setId(MediaMetadata.TVDB, show.tvShowIds.thetvdb);
    md.setId(MediaMetadata.TVRAGE, show.tvShowIds.tvrage);

    md.addEpisodeGroup(MediaEpisodeGroup.DEFAULT_AIRED);

    md.setTitle(show.title);

    try {
      md.setYear(parseYear(show.premiered));
    }
    catch (ParseException e) {
      LOGGER.trace("could not parse year: {}", e.getMessage());
    }

    try {
      md.setReleaseDate(premieredFormat.parse(show.premiered));
    }
    catch (ParseException e) {
      LOGGER.trace("could not parse releasedate: {}", e.getMessage());
    }

    md.setRuntime(show.runtime);

    for (String gen : show.genres) {
      MediaGenres genre = MediaGenres.getGenre(gen);
      md.addGenre(genre);
    }

    if (StringUtils.isNotBlank(show.summary)) {
      md.setPlot(Jsoup.parse(show.summary).text());
    }
    md.setOriginalLanguage(show.language);

    MediaRating rating = new MediaRating(MediaMetadata.TVMAZE);
    rating.setRating(show.rating.average);
    rating.setMaxValue(10);
    md.addRating(rating);

    // Get Cast
    try {
      castList = controller.getCast(tvMazeId);

      if (!castList.isEmpty()) {

        for (Cast cast : castList) {
          Person person = new Person(Person.Type.ACTOR);
          person.setName(cast.person.name);
          person.setProfileUrl(cast.person.image.original);
          person.setThumbUrl(cast.person.image.medium);
          md.addCastMember(person);
        }

      }

    }
    catch (IOException e) {
      LOGGER.trace("could not get cast information: {}", e.getMessage());
    }

    return md;
  }

  @Override
  public MediaMetadata getMetadata(@NotNull TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata() TvShowEpisode: {}", options);

    // lazy initialization of the api
    initAPI();

    // do we have an id from the options?
    int showId = options.createTvShowSearchAndScrapeOptions().getIdAsIntOrDefault(MediaMetadata.TVMAZE, 0);
    if (showId == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(MediaMetadata.TVDB);
    }

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    // Get all Episode and Season Information for the given TvShow
    Episode episode = null;
    try {
      episode = controller.getEpisode(showId, seasonNr, episodeNr);
    }
    catch (Exception e) {
      LOGGER.trace("could not get Episode information: {}", e.getMessage());
    }

    if (episode == null) {
      throw new NothingFoundException();
    }

    MediaMetadata md = new MediaMetadata(getId());
    md.setScrapeOptions(options);

    // found the correct episode
    md.setId(MediaMetadata.TVMAZE, episode.id);
    md.setTitle(episode.name);
    if (StringUtils.isNotBlank(episode.summary)) {
      md.setPlot(Jsoup.parse(episode.summary).text());
    }
    md.setEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, episode.season, episode.episode);
    md.setRuntime(episode.runtime);
    try {
      md.setReleaseDate(premieredFormat.parse(episode.airdate));
      md.setYear(parseYear(episode.airdate));
    }
    catch (ParseException ignored) {
      // ignored
    }

    // Get Image Information for the given TV Show
    List<Image> imageList = new ArrayList<>();
    try {
      imageList.addAll(controller.getImages(showId));
    }
    catch (IOException e) {
      LOGGER.trace("could not get Image information: {}", e.getMessage());
    }

    MediaArtwork ma;

    for (Image image : imageList) {
      if (StringUtils.isBlank(image.type)) {
        continue;
      }

      switch (image.type) {
        case "poster":
          ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.POSTER);
          ma.setPreviewUrl(image.resolutions.medium != null ? image.resolutions.medium.url : image.resolutions.original.url);
          ma.setOriginalUrl(image.resolutions.original.url);
          ma.addImageSize(image.resolutions.original.width, image.resolutions.original.height, image.resolutions.original.url,
              MediaArtwork.PosterSizes.getSizeOrder(image.resolutions.original.width));
          md.addMediaArt(ma);
          break;

        case "background":
          ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.BACKGROUND);
          ma.setPreviewUrl(image.resolutions.medium != null ? image.resolutions.medium.url : image.resolutions.original.url);
          ma.setOriginalUrl(image.resolutions.original.url);
          ma.addImageSize(image.resolutions.original.width, image.resolutions.original.height, image.resolutions.original.url,
              MediaArtwork.FanartSizes.getSizeOrder(image.resolutions.original.width));
          md.addMediaArt(ma);
          break;

        case "banner":
          ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.BANNER);
          ma.setPreviewUrl(image.resolutions.medium != null ? image.resolutions.medium.url : image.resolutions.original.url);
          ma.setOriginalUrl(image.resolutions.original.url);
          ma.addImageSize(image.resolutions.original.width, image.resolutions.original.height, image.resolutions.original.url, 0);
          md.addMediaArt(ma);
          break;
      }
    }

    return md;
  }

  @Override
  public SortedSet<MediaSearchResult> search(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options);

    // lazy initialization of the api
    initAPI();

    SortedSet<MediaSearchResult> searchResults = new TreeSet<>();

    List<Shows> searchResult;

    try {
      searchResult = controller.getTvShowSearchResults(options.getSearchQuery());
    }
    catch (Exception e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (searchResult == null) {
      LOGGER.warn("no result from tvmaze.com");
      return searchResults;
    }

    for (Shows shows : searchResult) {
      MediaSearchResult result = new MediaSearchResult(getId(), MediaType.TV_SHOW);
      result.setProviderId(getId());
      result.setTitle(shows.show.title);
      result.setScore((float) shows.show.rating.average);
      result.setUrl(shows.show.url);
      if (shows.show.image != null) {
        result.setPosterUrl(shows.show.image.original);
      }
      if (StringUtils.isNotBlank(shows.show.summary)) {
        result.setOverview(Jsoup.parse(shows.show.summary).text());
      }
      result.setIMDBId(shows.show.tvShowIds.imdb);
      result.setId(MediaMetadata.TVRAGE, String.valueOf(shows.show.tvShowIds.tvrage));
      result.setId(MediaMetadata.TVDB, String.valueOf(shows.show.tvShowIds.thetvdb));
      result.setId(MediaMetadata.TVMAZE, String.valueOf(shows.show.id));
      result.setOriginalLanguage(shows.show.language);
      if (StringUtils.isNotBlank(shows.show.premiered)) {
        try {
          result.setYear(parseYear(shows.show.premiered));
        }
        catch (ParseException ignored) {
        }
      }

      // calculate score
      if (StringUtils.isNotBlank(options.getImdbId()) && options.getImdbId().equals(result.getIMDBId())
          || String.valueOf(options.getTmdbId()).equals(result.getId())) {
        LOGGER.debug("perfect match by ID - set score to 1");
        result.setScore(1);
      }
      else {
        // calculate the score by comparing the search result with the search options
        result.calculateScore(options);
      }

      searchResults.add(result);

    }

    return searchResults;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    initAPI();

    // do we have an id from the options?
    int showId = options.getIdAsIntOrDefault(MediaMetadata.TVMAZE, 0);
    if (showId == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(MediaMetadata.TVDB);
    }

    // Get all Episode and Season Information for the given TvShow
    List<Episode> episodeList = new ArrayList<>();
    try {
      episodeList.addAll(controller.getEpisodes(showId));
    }
    catch (IOException e) {
      LOGGER.trace("could not get Episode information: {}", e.getMessage());
    }

    List<MediaMetadata> list = new ArrayList<>();

    // get the correct information
    for (Episode episode : episodeList) {
      MediaMetadata md = new MediaMetadata(getId());
      md.setScrapeOptions(options);

      // found the correct episode
      md.setTitle(episode.name);
      if (StringUtils.isNotBlank(episode.summary)) {
        md.setPlot(Jsoup.parse(episode.summary).text());
      }
      md.setEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, episode.season, episode.episode);
      md.setRuntime(episode.runtime);
      try {
        md.setReleaseDate(premieredFormat.parse(episode.airdate));
        md.setYear(parseYear(episode.airdate));
      }
      catch (ParseException ignored) {
      }
      list.add(md);
    }

    return list;
  }

  @Override
  protected String getSubId() {
    return "tvshow";
  }
}
