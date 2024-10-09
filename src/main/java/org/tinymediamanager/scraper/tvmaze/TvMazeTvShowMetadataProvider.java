package org.tinymediamanager.scraper.tvmaze;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.tvmaze.entities.AlternateList;
import org.tinymediamanager.scraper.tvmaze.entities.Cast;
import org.tinymediamanager.scraper.tvmaze.entities.Crew;
import org.tinymediamanager.scraper.tvmaze.entities.Episode;
import org.tinymediamanager.scraper.tvmaze.entities.Image;
import org.tinymediamanager.scraper.tvmaze.entities.SearchResult;
import org.tinymediamanager.scraper.tvmaze.entities.Season;
import org.tinymediamanager.scraper.tvmaze.entities.Show;

public class TvMazeTvShowMetadataProvider extends TvMazeMetadataProvider implements ITvShowMetadataProvider, ITvShowArtworkProvider {

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
    md.setId(MediaMetadata.IMDB, show.externals.imdb);
    md.setId(MediaMetadata.TVDB, show.externals.thetvdb);
    md.setId(MediaMetadata.TVRAGE, show.externals.tvrage);

    md.addEpisodeGroup(MediaEpisodeGroup.DEFAULT_AIRED);

    md.setTitle(show.name);

    try {
      md.setYear(parseYear(show.premiered));
    }
    catch (Exception e) {
      LOGGER.trace("could not parse year: {}", e.getMessage());
    }

    try {
      md.setReleaseDate(premieredFormat.parse(show.premiered));
    }
    catch (Exception e) {
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

    // Season-to-ID mapping;
    // we need this, as we scrape episodes via seasons, and we can only use the ID there...
    if (show._embedded.seasons != null) {
      Map<Integer, Integer> seasonMap = new HashMap<>();
      for (Season season : show._embedded.seasons) {
        seasonMap.put(season.number, season.id);

        // also add all the good season posters
        if (season.image != null) {
          MediaArtwork sa = new MediaArtwork(MediaMetadata.TVMAZE, MediaArtworkType.SEASON_POSTER);
          sa.setOriginalUrl(season.image.original);
          sa.setPreviewUrl(season.image.medium);
          sa.setSeason(season.number);
          sa.addImageSize(0, 0, season.image.original, 0);
          md.addMediaArt(sa);
        }
      }
      md.addExtraData("seasonMap", seasonMap);
    }

    // add default poster
    if (show.image != null) {
      MediaArtwork ma = new MediaArtwork(MediaMetadata.TVMAZE, MediaArtworkType.POSTER);
      ma.setOriginalUrl(show.image.original);
      ma.setPreviewUrl(show.image.medium);
      ma.addImageSize(0, 0, show.image.original, 0);
      md.addMediaArt(ma);
    }

    // add various images
    if (show._embedded.images != null) {
      for (Image img : show._embedded.images) {
        MediaArtwork ma = imagesToMA(img);
        if (ma != null) {
          md.addMediaArt(ma);
        }
      }
    }

    // Get Cast
    if (show._embedded.cast != null) {
      for (Cast cast : show._embedded.cast) {
        Person person = new Person(Person.Type.ACTOR);
        person.setId(MediaMetadata.TVMAZE, cast.person.id);
        person.setName(cast.person.name);
        person.setRole(cast.character.name);
        person.setProfileUrl(cast.person.url);
        if (cast.person.image != null) {
          person.setThumbUrl(cast.person.image.medium);
        }
        md.addCastMember(person);
      }
    }

    // Get Crew
    if (show._embedded.crew != null) {
      for (Crew crew : show._embedded.crew) {
        Person person = new Person();
        person.setId(MediaMetadata.TVMAZE, crew.person.id);
        person.setName(crew.person.name);
        person.setProfileUrl(crew.person.url);
        if (crew.person.image != null) {
          person.setThumbUrl(crew.person.image.medium);
        }
        person.setRole(crew.type);
        switch (crew.type) {
          // case "Executive Producer":
          case "Producer": {
            person.setType(Person.Type.PRODUCER);
            break;
          }

          case "Director Of Photography": {
            person.setType(Person.Type.DIRECTOR);
            break;
          }

          case "Creator": {
            person.setType(Person.Type.WRITER);
            break;
          }

          default:
            continue; // do not add unknown
        }
        md.addCastMember(person);
      }
    }

    // integer is the EG id for scraping - remember that
    Map<Integer, MediaEpisodeGroup> egs = getEpisodeGroups(tvMazeId);
    if (!egs.isEmpty()) {
      egs.values().forEach(md::addEpisodeGroup);
    }

    return md;
  }

  /**
   * 
   * @param alternateId
   *          EG id for scraping
   * @param group
   *          EG itself for creating MD entry
   * @return map of aired ID to MD for EG
   */
  private Map<Integer, MediaMetadata> getEpisodeListForEG(int alternateId, MediaEpisodeGroup group) {
    Map<Integer, MediaMetadata> mds = new HashMap<>();
    // get episodes in EG-style
    try {
      List<Episode> eps = controller.getAlternativeEpisodes(alternateId);
      if (eps != null && !eps.isEmpty()) {
        for (Episode ep : eps) {
          MediaMetadata md = new MediaMetadata(MediaMetadata.TVMAZE);
          md.setId(MediaMetadata.TVMAZE, ep._embedded.episodes.get(0).id); // get the underlying aired episode id
          md.setEpisodeNumber(group, ep.season, ep.number);
          mds.put(ep._embedded.episodes.get(0).id, md);
        }
      }
    }
    catch (IOException e) {
      LOGGER.trace("could not get episode groups for show: {}", e.getMessage());
    }
    return mds;
  }

  /**
   * Integer is ID of EG for scraping
   * 
   * @param showId
   * @return
   */
  private Map<Integer, MediaEpisodeGroup> getEpisodeGroups(int showId) {
    Map<Integer, MediaEpisodeGroup> egs = new HashMap<>();

    // get alternate listings aka EpisodeGroups
    // desc: https://www.tvmaze.com/faq/40/alternate-episodes
    try {
      List<AlternateList> alternates = controller.getAlternativeLists(showId);
      if (alternates != null && alternates.size() > 0) {
        for (AlternateList alt : alternates) {

          String countryPub = "";
          if (!alt.getName().isBlank()) {
            countryPub += " - " + alt.getName();
          }
          if (!alt.getCountry().isBlank()) {
            countryPub += " (" + alt.getCountry() + ")";
          }

          // dupes possible?
          if (alt.dvd_release) {
            egs.put(alt.id, MediaEpisodeGroup.DEFAULT_DVD);
          }
          else if (alt.verbatim_order) {
            MediaEpisodeGroup eg = new MediaEpisodeGroup(MediaEpisodeGroup.EpisodeGroupType.ALTERNATE, "Verbatim");
            egs.put(alt.id, eg);
          }
          else if (alt.country_premiere) {
            MediaEpisodeGroup eg = new MediaEpisodeGroup(MediaEpisodeGroup.EpisodeGroupType.ALTERNATE, "Country Premiere" + countryPub);
            egs.put(alt.id, eg);
          }
          else if (alt.streaming_premiere) {
            MediaEpisodeGroup eg = new MediaEpisodeGroup(MediaEpisodeGroup.EpisodeGroupType.ALTERNATE, "Streaming" + countryPub);
            egs.put(alt.id, eg);
          }
          else if (alt.broadcast_premiere) {
            MediaEpisodeGroup eg = new MediaEpisodeGroup(MediaEpisodeGroup.EpisodeGroupType.ALTERNATE, "Broadcast Premiere" + countryPub);
            egs.put(alt.id, eg);
          }
          else if (alt.language_premiere) {
            // https://api.tvmaze.com/alternatelists/17
            String lang = alt.language != null ? " (" + alt.language + ")" : "";
            MediaEpisodeGroup eg = new MediaEpisodeGroup(MediaEpisodeGroup.EpisodeGroupType.ALTERNATE, "Language Premiere" + lang);
            egs.put(alt.id, eg);
          }
        }
      }
    }
    catch (IOException e) {
      LOGGER.trace("could not get episode groups for show: {}", e.getMessage());
    }

    return egs;
  }

  @Override
  public MediaMetadata getMetadata(@NotNull TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata() TvShowEpisode: {}", options);

    // lazy initialization of the api
    initAPI();

    MediaEpisodeGroup episodeGroup = options.getEpisodeGroup();
    // get episode number and season number
    int seasonNr = -1;
    int episodeNr = -1;
    // new style
    if (options.getIds().get(MediaMetadata.EPISODE_NR) instanceof List<?> episodeNumbers) {
      for (Object obj : episodeNumbers) {
        if (obj instanceof MediaEpisodeNumber episodeNumber && episodeNumber.episodeGroup().equals(episodeGroup)) {
          episodeNr = episodeNumber.episode();
          seasonNr = episodeNumber.season();
          break;
        }
      }
    }
    // old style
    if (seasonNr == -1 && episodeNr == -1) {
      seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
      episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);
    }
    if (seasonNr == -1 && episodeNr == -1) {
      LOGGER.warn("cannot scrape -1/-1 episode");
      throw new MissingIdException(MediaMetadata.TVMAZE);
    }

    List<MediaMetadata> eps = getEpisodeList(options.createTvShowSearchAndScrapeOptions());
    for (MediaMetadata ep : eps) {
      MediaEpisodeNumber num = ep.getEpisodeNumber(episodeGroup);
      if (num.episode() == episodeNr && num.season() == seasonNr) {
        // found it :)
        return ep;
      }
    }

    throw new NothingFoundException();
  }

  @Override
  public SortedSet<MediaSearchResult> search(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options);

    // lazy initialization of the api
    initAPI();

    SortedSet<MediaSearchResult> searchResults = new TreeSet<>();
    List<SearchResult> searchResult;

    // implement get by MazeID first
    // int tvMazeId = options.getIdAsIntOrDefault(MediaMetadata.TVMAZE, 0);
    // if (tvMazeId > 0) {
    //
    // }
    // get show
    // then lookup by OtherID
    // URL: /lookup/shows?tvrage=:id or /lookup/shows?thetvdb=:id
    // Example: https://api.tvmaze.com/lookup/shows?thetvdb=81189
    // Example: https://api.tvmaze.com/lookup/shows?imdb=tt0944947
    // else search

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

    for (SearchResult result : searchResult) {
      MediaSearchResult msr = new MediaSearchResult(getId(), MediaType.TV_SHOW);
      msr.setScore(result.score);

      Show show = result.show;
      msr.setTitle(show.name);
      msr.setUrl(show.url);
      if (show.image != null) {
        msr.setPosterUrl(show.image.medium);
      }
      if (StringUtils.isNotBlank(show.summary)) {
        msr.setOverview(Jsoup.parse(show.summary).text());
      }
      msr.setIMDBId(show.externals.imdb);
      msr.setId(MediaMetadata.TVRAGE, String.valueOf(show.externals.tvrage));
      msr.setId(MediaMetadata.TVDB, String.valueOf(show.externals.thetvdb));
      msr.setId(MediaMetadata.TVMAZE, String.valueOf(show.id));
      msr.setOriginalLanguage(show.language);
      if (StringUtils.isNotBlank(show.premiered)) {
        try {
          msr.setYear(parseYear(show.premiered));
        }
        catch (Exception ignored) {
        }
      }

      // calculate score
      if (StringUtils.isNotBlank(options.getImdbId()) && options.getImdbId().equals(msr.getIMDBId())
          || String.valueOf(options.getTmdbId()).equals(msr.getId())) {
        LOGGER.debug("perfect match by ID - set score to 1");
        msr.setScore(1);
      }
      else {
        // calculate the score by comparing the search result with the search options
        // msr.calculateScore(options);
        msr.setScore(result.score); // use remote score
      }
      searchResults.add(msr);
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
      throw new MissingIdException(MediaMetadata.TVMAZE);
    }

    List<Episode> episodeList = new ArrayList<>();
    // Get all Episode and Season Information for the given TvShow
    // does get all episodes with ONE call, but cannot mixin guest cast
    // try {
    // episodeList.addAll(controller.getEpisodes(showId));
    // }
    // catch (IOException e) {
    // LOGGER.trace("could not get Episode information: {}", e.getMessage());
    // }

    // proven approach
    // create same list, but for each season
    try {
      MediaMetadata show = getMetadata(options);
      // seasonNumber / seasonId
      Map<Integer, Integer> seasonMap = (Map<Integer, Integer>) show.getExtraData().get("seasonMap");
      for (Map.Entry<Integer, Integer> season : seasonMap.entrySet()) {
        List<Episode> eps = controller.getSeasonEpisodes(season.getValue());
        episodeList.addAll(eps);
      }
    }
    catch (Exception e) {
      LOGGER.trace("could not get Episode information: {}", e.getMessage());
    }

    List<MediaMetadata> returnList = new ArrayList<>();
    // get the correct information
    for (Episode episode : episodeList) {
      MediaMetadata md = new MediaMetadata(getId());
      md.setScrapeOptions(options);

      md.setId(MediaMetadata.TVMAZE, episode.id);
      md.setTitle(episode.name);
      if (StringUtils.isNotBlank(episode.summary)) {
        md.setPlot(Jsoup.parse(episode.summary).text());
      }
      md.setEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, episode.season, episode.number);
      md.setRuntime(episode.runtime);
      try {
        md.setReleaseDate(premieredFormat.parse(episode.airdate));
        md.setYear(parseYear(episode.airdate));
      }
      catch (Exception ignored) {
      }
      // episode image (thumb?)
      if (episode.image != null) {
        MediaArtwork ma = new MediaArtwork(MediaMetadata.TVMAZE, MediaArtworkType.THUMB);
        ma.setOriginalUrl(episode.image.original);
        ma.setPreviewUrl(episode.image.medium);
        ma.addImageSize(0, 0, episode.image.original, 0);
        md.addMediaArt(ma);
      }
      // Get Guests
      if (episode._embedded.guestcast != null) {
        for (Cast cast : episode._embedded.guestcast) {
          Person person = new Person(Person.Type.GUEST);
          person.setId(MediaMetadata.TVMAZE, cast.person.id);
          person.setName(cast.person.name);
          person.setRole(cast.character.name);
          person.setProfileUrl(cast.person.url);
          if (cast.person.image != null) {
            person.setThumbUrl(cast.person.image.medium);
          }
          md.addCastMember(person);
        }
      }
      returnList.add(md);
    }

    // mixin EGs
    Map<Integer, MediaEpisodeGroup> egs = getEpisodeGroups(showId);
    // for every scraper EG...
    for (Map.Entry<Integer, MediaEpisodeGroup> eg : egs.entrySet()) {
      // prepare an own EP list with correct numbers
      Map<Integer, MediaMetadata> egEps = getEpisodeListForEG(eg.getKey(), eg.getValue());
      // then iterate over TMM episode
      for (MediaMetadata md : returnList) {
        MediaMetadata alternate = egEps.get(md.getId(MediaMetadata.TVMAZE));
        // and add alternate numbers to EP from scraper
        if (alternate != null) {
          MediaEpisodeNumber epNo = alternate.getEpisodeNumber(eg.getValue());
          md.setEpisodeNumber(epNo.episodeGroup(), epNo.season(), epNo.episode());
        }
      }
    }

    return returnList;
  }

  @Override
  protected String getSubId() {
    return "tvshow";
  }

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException, MissingIdException {
    LOGGER.debug("getArtwork(): {}", options);

    // lazy initialization of the api
    initAPI();

    try {
      if (options.getMediaType() != MediaType.TV_SHOW && options.getMediaType() != MediaType.TV_EPISODE) {
        return Collections.emptyList();
      }
      if (options.getMediaType() == MediaType.TV_EPISODE) {
        // episode artwork has to be scraped via the meta data scraper
        TvShowEpisodeSearchAndScrapeOptions episodeSearchAndScrapeOptions = new TvShowEpisodeSearchAndScrapeOptions();
        episodeSearchAndScrapeOptions.setDataFromOtherOptions(options);
        if (options.getIds().get(MediaMetadata.TVSHOW_IDS) instanceof Map) {
          Map<String, Object> tvShowIds = (Map<String, Object>) options.getIds().get(MediaMetadata.TVSHOW_IDS);
          episodeSearchAndScrapeOptions.setTvShowIds(tvShowIds);
        }
        MediaMetadata md = getMetadata(episodeSearchAndScrapeOptions);
        return md.getMediaArt(options.getArtworkType());
      }
      else {
        TvShowSearchAndScrapeOptions op = new TvShowSearchAndScrapeOptions();
        op.setDataFromOtherOptions(options);
        return getMetadata(op).getMediaArt(options.getArtworkType());
      }
    }
    catch (MissingIdException e) {
      // no valid ID given - just do nothing
      return Collections.emptyList();
    }
    catch (Exception e) {
      throw new ScrapeException(e);
    }
  }

  private MediaArtwork imagesToMA(Image image) {
    MediaArtworkType type = null;
    MediaArtwork ma = null;
    try {
      type = MediaArtworkType.valueOf(image.type.toUpperCase(Locale.ROOT));
      ma = new MediaArtwork(MediaMetadata.TVMAZE, type);
      ma.setPreviewUrl(image.resolutions.medium != null ? image.resolutions.medium.url : image.resolutions.original.url);
      ma.setOriginalUrl(image.resolutions.original.url);
      ma.addImageSize(image.resolutions.original.width, image.resolutions.original.height, image.resolutions.original.url,
          MediaArtwork.PosterSizes.getSizeOrder(image.resolutions.original.width));
      if (image.resolutions.medium != null) {
        ma.addImageSize(image.resolutions.medium.width, image.resolutions.medium.height, image.resolutions.medium.url,
            MediaArtwork.PosterSizes.getSizeOrder(image.resolutions.medium.width));
      }
    }
    catch (Exception e) {
      return null;
    }
    return ma;
  }
}
