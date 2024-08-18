/*
 * Copyright 2012 - 2024 Manuel Laggner
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

package org.tinymediamanager.scraper.omdb;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.ParserUtils;

/**
 * Central metadata provider class
 *
 * @author Wolfgang Janes
 */
abstract class OmdbMetadataProvider implements IMediaProvider {
  private static final String     ID = "omdbapi";

  private final MediaProviderInfo providerInfo;

  OmdbMetadataProvider() {
    providerInfo = createMediaProviderInfo();
  }

  /**
   * get the sub id of this scraper (for dedicated storage)
   * 
   * @return the sub id
   */
  protected abstract String getSubId();

  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "omdbapi.com",
        "<html><h3>Omdbapi.com</h3><br />The OMDb API is a RESTful web service to obtain movie information. All content and images on the site are contributed and maintained by our users. <br /><br />TinyMediaManager offers a limited access to OMDb (10 calls per 15 seconds). If you want to use OMDb with more than this restricted access, you should become a patron of OMDb (https://www.patreon.com/join/omdb)<br /><br />Available languages: EN</html>",
        OmdbMetadataProvider.class.getResource("/org/tinymediamanager/scraper/omdbapi.svg"), 10);

    info.getConfig().addText("apiKey", "", true);
    info.getConfig().load();

    return info;
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  @Override
  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(providerInfo.getUserApiKey());
  }

  @Override
  public String getApiKey() {
    String userApiKey = providerInfo.getUserApiKey();
    if (StringUtils.isNotBlank(userApiKey)) {
      return userApiKey;
    }

    return IMediaProvider.super.getApiKey();
  }

  protected abstract Logger getLogger();

  protected List<MediaSearchResult> parseSearchResults(Document document, MediaType mediaType) {
    Elements results = document.getElementsByTag("result");
    if (results.isEmpty()) {
      return Collections.emptyList();
    }

    List<MediaSearchResult> searchResults = new ArrayList<>();

    for (Element result : results) {
      MediaSearchResult searchResult = new MediaSearchResult(getId(), mediaType);

      if (MediaIdUtil.isValidImdbId(result.attr("imdbID"))) {
        searchResult.setId(MediaMetadata.IMDB, result.attr("imdbID"));
      }

      searchResult.setTitle(result.attr("title"));
      searchResult.setYear(MetadataUtil.parseInt(result.attr("year"), 0));

      // if year is still zero, try to get it in a different way
      if (searchResult.getYear() <= 0 && result.attr("year").length() >= 4) {
        searchResult.setYear(MetadataUtil.parseInt(result.attr("year").substring(0, 4)));
      }

      if (StringUtils.isNotBlank(result.attr("poster"))) {
        searchResult.setPosterUrl(result.attr("poster"));
      }

      searchResults.add(searchResult);
    }

    return searchResults;
  }

  protected MediaMetadata parseDetail(Document document, String baseElement) {
    Elements movies = document.getElementsByTag(baseElement);
    if (movies.isEmpty()) {
      return null;
    }

    DateFormat format = new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH);

    MediaMetadata md = new MediaMetadata(getId());

    Element movie = movies.first();

    if (MediaIdUtil.isValidImdbId(movie.attr("imdbID"))) {
      md.setId(MediaMetadata.IMDB, movie.attr("imdbID"));
    }

    md.setTitle(movie.attr("title"));
    md.setPlot(movie.attr("plot"));
    md.setYear(MetadataUtil.parseInt(movie.attr("year"), 0));

    // if year is still zero, try to get it in a different way
    if (md.getYear() <= 0 && movie.attr("year").length() >= 4) {
      md.setYear(MetadataUtil.parseInt(movie.attr("year").substring(0, 4)));
    }

    MediaCertification certification = MediaCertification.findCertification(movie.attr("rated"));
    if (certification != MediaCertification.UNKNOWN) {
      md.addCertification(certification);
    }

    try {
      md.setReleaseDate(format.parse(movie.attr("released")));
    }
    catch (Exception ignored) {
      // can be ignored
    }

    Pattern p = Pattern.compile("\\d+");
    Matcher m = p.matcher(movie.attr("runtime"));
    while (m.find()) {
      try {
        md.setRuntime(Integer.parseInt(m.group()));
        break;
      }
      catch (NumberFormatException ignored) {
        // not able to parse
      }
    }

    String[] genres = movie.attr("genre").split(",");
    for (String genre : genres) {
      genre = genre.strip();
      MediaGenres mediaGenres = MediaGenres.getGenre(genre);
      md.addGenre(mediaGenres);
    }

    String[] directors = movie.attr("director").split(",");
    for (String d : directors) {
      Person director = new Person(DIRECTOR);
      director.setName(d.strip());
      md.addCastMember(director);
    }

    String[] writers = movie.attr("writer").split(",");
    for (String w : writers) {
      Person writer = new Person(WRITER);
      writer.setName(w.strip());
      md.addCastMember(writer);
    }

    String[] actors = movie.attr("actors").split(",");
    for (String a : actors) {
      Person actor = new Person(ACTOR);
      actor.setName(a.strip());
      md.addCastMember(actor);
    }

    md.setSpokenLanguages(ParserUtils.split(movie.attr("language")));
    md.setCountries(ParserUtils.split(movie.attr("country")));

    // IMDB rating
    try {
      MediaRating rating = new MediaRating(MediaMetadata.IMDB);
      rating.setRating(Float.parseFloat(movie.attr("imdbRating")));
      rating.setVotes(MetadataUtil.parseInt(movie.attr("imdbVotes")));
      rating.setMaxValue(10);
      md.addRating(rating);
    }
    catch (NumberFormatException e) {
      getLogger().trace("could not parse IMDB rating/vote count: {}", e.getMessage());
    }

    try {
      MediaRating rating = new MediaRating(MediaMetadata.METACRITIC);
      rating.setRating(Float.parseFloat(movie.attr("metascore")));
      rating.setMaxValue(100);
      md.addRating(rating);
    }
    catch (NumberFormatException e) {
      getLogger().trace("could not parse metacritic rating/vote count: {}", e.getMessage());
    }

    // Tomatoratings
    try {
      MediaRating rating = new MediaRating("tomatometerallcritics");
      rating.setRating(Float.parseFloat(movie.attr("tomatoMeter")));
      rating.setVotes(MetadataUtil.parseInt(movie.attr("tomatoReviews")));
      rating.setMaxValue(100);
      md.addRating(rating);
    }
    catch (NumberFormatException e) {
      getLogger().trace("could not parse tomatometerallcritics rating/vote count: {}", e.getMessage());
    }

    try {
      MediaRating rating = new MediaRating("tomatometerallaudience");
      rating.setRating(Float.parseFloat(movie.attr("tomatoUserMeter")));
      rating.setVotes(MetadataUtil.parseInt(movie.attr("tomatoUserReviews")));
      rating.setMaxValue(100);
      md.addRating(rating);
    }
    catch (NumberFormatException e) {
      getLogger().trace("could not parse tomatometerallaudience rating/vote count: {}", e.getMessage());
    }

    try {
      MediaRating rating = new MediaRating("tomatometeravgcritics");
      rating.setRating(Float.parseFloat(movie.attr("tomatoRating")));
      rating.setVotes(MetadataUtil.parseInt(movie.attr("tomatoReviews")));
      rating.setMaxValue(10);
      md.addRating(rating);
    }
    catch (NumberFormatException e) {
      getLogger().trace("could not parse tomatometeravgcritics rating/vote count: {}", e.getMessage());
    }

    try {
      MediaRating rating = new MediaRating("tomatometeravgaudience");
      rating.setRating(Float.parseFloat(movie.attr("tomatoUserRating")));
      rating.setVotes(MetadataUtil.parseInt(movie.attr("tomatoUserReviews")));
      rating.setMaxValue(5);
      md.addRating(rating);
    }
    catch (NumberFormatException e) {
      getLogger().trace("could not parse tomatometeravgaudience rating/vote count: {}", e.getMessage());
    }

    // get the imdb rating from the imdb dataset too (and probably replace an
    // outdated rating from omdb)
    if (md.getId(MediaMetadata.IMDB) instanceof String) {
      MediaRating omdbRating = md.getRatings().stream().filter(rating -> MediaMetadata.IMDB.equals(rating.getId())).findFirst().orElse(null);
      MediaRating imdbRating = RatingProvider.getImdbRating((String) md.getId(MediaMetadata.IMDB));
      if (imdbRating != null && (omdbRating == null || imdbRating.getVotes() > omdbRating.getVotes())) {
        md.getRatings().remove(omdbRating);
        md.addRating(imdbRating);
      }
    }

    if (StringUtils.isNotBlank(movie.attr("poster"))) {
      MediaArtwork artwork = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.POSTER);
      artwork.setOriginalUrl(movie.attr("poster"));
      artwork.addImageSize(300, 445, movie.attr("poster"), MediaArtwork.PosterSizes.getSizeOrder(300));
      md.addMediaArt(artwork);
    }

    return md;
  }

  protected String getImdbId(MediaSearchAndScrapeOptions options) {
    // id from the options
    String imdbId = options.getImdbId();

    // id from omdb proxy?
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      imdbId = options.getIdAsString(getProviderInfo().getId());
    }

    // still no imdb id but tvdb id? get it from tmdb
    if (!MediaIdUtil.isValidImdbId(imdbId) && options.getIdAsIntOrDefault(MediaMetadata.TVDB, 0) > 0) {
      int tvdbId = options.getIdAsInt(MediaMetadata.TVDB);
      imdbId = MediaIdUtil.getImdbIdFromTvdbId(String.valueOf(tvdbId));
    }

    // still no imdb id but tmdb id? get it from tmdb
    if (!MediaIdUtil.isValidImdbId(imdbId) && options.getTmdbId() > 0) {
      imdbId = MediaIdUtil.getTvShowImdbIdViaTmdbId(options.getTmdbId());
    }

    return imdbId;
  }
}
