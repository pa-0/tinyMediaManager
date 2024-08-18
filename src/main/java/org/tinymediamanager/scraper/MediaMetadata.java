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
package org.tinymediamanager.scraper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.util.StrgUtils;

/**
 * The Class MediaMetadata. This is the main class to transport metadata.
 * 
 * @author Manuel Laggner
 * @since 2.0
 */
public class MediaMetadata {
  // some well known ids
  public static final String                               ALLOCINE            = "allocine";
  public static final String                               ANIDB               = "anidb";
  public static final String                               IMDB                = "imdb";
  public static final String                               LETTERBOXD          = "letterboxd";
  public static final String                               METACRITIC          = "metacritic";
  public static final String                               METACRITIC_USER     = "metacriticuser";
  public static final String                               MY_ANIME_LIST       = "myanimelist";
  public static final String                               ROGER_EBERT         = "rogerebert";
  public static final String                               TMDB                = "tmdb";
  public static final String                               TMDB_SET            = "tmdbSet";
  public static final String                               TRAKT_TV            = "trakt";
  public static final String                               TVDB                = "tvdb";
  public static final String                               TVMAZE              = "tvmaze";
  public static final String                               TVRAGE              = "tvrage";
  public static final String                               WIKIDATA            = "wikidata";
  public static final String                               ZAP2IT              = "zap2it";
  // no so well-known
  // mptv
  // ofdb
  // omdb
  // eidr

  // some meta ids for TV show scraping
  public static final String                               TVSHOW_IDS          = "tvShowIds";
  public static final String                               EPISODE_NR          = "episodeNr";
  public static final String                               SEASON_NR           = "seasonNr";
  @Deprecated
  public static final String                               EPISODE_NR_DVD      = "dvdEpisodeNr";
  @Deprecated
  public static final String                               SEASON_NR_DVD       = "dvdSeasonNr";

  // the "empty" rating
  public static final MediaRating                          EMPTY_RATING        = new MediaRating("", 0);

  private final String                                     providerId;

  // this map contains all set ids
  private final Map<String, Object>                        ids                 = new HashMap<>();

  // multi value
  private final List<MediaRating>                          ratings             = new ArrayList<>();
  private final List<Person>                               castMembers         = new ArrayList<>();
  private final List<MediaArtwork>                         artwork             = new ArrayList<>();
  private final List<MediaGenres>                          genres              = new ArrayList<>();
  private final List<MediaCertification>                   certifications      = new ArrayList<>();
  private final List<String>                               productionCompanies = new ArrayList<>();
  private final List<String>                               spokenLanguages     = new ArrayList<>();
  private final List<String>                               countries           = new ArrayList<>();
  private final List<MediaTrailer>                         trailers            = new ArrayList<>();
  private final List<MediaMetadata>                        subItems            = new ArrayList<>();
  private final List<String>                               tags                = new ArrayList<>();

  // general media entity
  private String                                           title               = "";
  private String                                           originalTitle       = "";
  private String                                           originalLanguage    = "";
  private int                                              year                = 0;
  private Date                                             releaseDate         = null;
  private String                                           plot                = "";
  private String                                           tagline             = "";
  private int                                              runtime             = 0;

  // movie
  private String                                           collectionName      = "";
  private int                                              top250              = 0;

  // tv show
  private final Set<MediaEpisodeGroup>                     episodeGroups       = new HashSet<>();
  private final Map<MediaEpisodeGroup, MediaEpisodeNumber> episodeNumbers      = new LinkedHashMap<>();
  private MediaAiredStatus                                 status              = MediaAiredStatus.UNKNOWN;
  private Map<MediaEpisodeGroup, Map<Integer, String>>     seasonNames         = new HashMap<>();
  private Map<MediaEpisodeGroup, Map<Integer, String>>     seasonOverview      = new HashMap<>();

  // extra data
  private final Map<String, Object>                        extraData           = new HashMap<>();
  private MediaSearchAndScrapeOptions                      scrapeOptions       = null;

  /**
   * Instantiates a new media metadata for the given provider.
   * 
   * @param providerId
   *          the provider id
   */
  public MediaMetadata(String providerId) {
    this.providerId = providerId;
  }

  /**
   * merges all entries from other MD into ours, IF VALUES ARE EMPTY<br>
   * <b>needs testing!</b>
   * 
   * @param md
   *          other MediaMetadata
   */

  public void mergeFrom(MediaMetadata md) {
    if (md == null) {
      return;
    }

    Map<String, Object> delta = md.getIds();
    delta.keySet().removeAll(ids.keySet()); // remove all remote ones, which we have in our map

    ids.putAll(delta); // so no dupe on adding while not overwriting

    title = merge(title, md.getTitle());
    originalTitle = merge(originalTitle, md.getOriginalTitle());
    originalLanguage = merge(originalLanguage, md.getOriginalLanguage());
    year = merge(year, md.getYear());
    releaseDate = merge(releaseDate, md.getReleaseDate());
    plot = merge(plot, md.getPlot());
    tagline = merge(tagline, md.getTagline());
    runtime = merge(runtime, md.getRuntime());
    collectionName = merge(collectionName, md.getCollectionName());
    top250 = merge(top250, md.getTop250());
    status = merge(status, md.getStatus());

    // remove all local ones, which we have in other array
    // so no dupe on adding all ;)
    episodeNumbers.putAll(md.getEpisodeNumbers());

    ratings.removeAll(md.getRatings());
    ratings.addAll(md.getRatings());

    castMembers.removeAll(md.getCastMembers());
    castMembers.addAll(md.getCastMembers());

    artwork.removeAll(md.getMediaArt());
    artwork.addAll(md.getMediaArt());

    genres.removeAll(md.getGenres());
    genres.addAll(md.getGenres());

    certifications.removeAll(md.getCertifications());
    certifications.addAll(md.getCertifications());

    productionCompanies.removeAll(md.getProductionCompanies());
    productionCompanies.addAll(md.getProductionCompanies());

    spokenLanguages.removeAll(md.getSpokenLanguages());
    spokenLanguages.addAll(md.getSpokenLanguages());

    countries.removeAll(md.getCountries());
    countries.addAll(md.getCountries());

    trailers.removeAll(md.getTrailers());
    trailers.addAll(md.getTrailers());

    subItems.removeAll(md.getSubItems());
    subItems.addAll(md.getSubItems());

    tags.removeAll(md.getTags());
    tags.addAll(md.getTags());

    episodeGroups.removeAll(md.getEpisodeGroups());
    episodeGroups.addAll(md.getEpisodeGroups());

    seasonNames.keySet().removeAll(md.getSeasonNames().keySet());
    seasonNames.putAll(md.seasonNames);

    seasonOverview.keySet().removeAll(md.getSeasonOverview().keySet());
    seasonOverview.putAll(md.seasonOverview);

    delta = md.getExtraData();
    delta.keySet().removeAll(extraData.keySet());
    extraData.putAll(delta);
    scrapeOptions = merge(scrapeOptions, md.scrapeOptions);
  }

  private String merge(String val1, String val2) {
    return StringUtils.isBlank(val1) ? val2 : val1;
  }

  private int merge(int val1, int val2) {
    return val1 <= 0 ? val2 : val1;
  }

  private Date merge(Date val1, Date val2) {
    return val1 == null ? val2 : val1;
  }

  private float merge(float val1, float val2) {
    return val1 <= 0 ? val2 : val1;
  }

  private MediaSearchAndScrapeOptions merge(MediaSearchAndScrapeOptions val1, MediaSearchAndScrapeOptions val2) {
    return val1 == null ? val2 : val1;
  }

  private MediaAiredStatus merge(MediaAiredStatus val1, MediaAiredStatus val2) {
    return val1 == MediaAiredStatus.UNKNOWN ? val2 : val1;
  }

  /**
   * Gets the provider id.
   * 
   * @return the provider id
   */
  public String getProviderId() {
    return providerId;
  }

  /**
   * Gets the genres.
   * 
   * @return the genres
   */
  public List<MediaGenres> getGenres() {
    return genres;
  }

  /**
   * Sets all given genres
   * 
   * @param genres
   *          a list of all genres to be set
   */
  public void setGenres(List<MediaGenres> genres) {
    this.genres.clear();
    if (genres != null) {
      this.genres.addAll(genres);
    }
  }

  /**
   * Gets the cast members for a given type.
   * 
   * @param type
   *          the type
   * @return the cast members
   */
  public List<Person> getCastMembers(Person.Type type) {
    // get all cast members for the given type
    List<Person> ret = new ArrayList<>();
    ret.addAll(castMembers.stream().filter(person -> person.getType() == type).toList());
    if (type == Person.Type.ACTOR) {
      // if we want actors, add all guest stars too!
      ret.addAll(castMembers.stream().filter(person -> person.getType() == Person.Type.GUEST).toList());
    }
    return ret;
  }

  /**
   * Gets the media art.
   * 
   * @param type
   *          the type
   * @return the media art
   */
  public List<MediaArtwork> getMediaArt(MediaArtworkType type) {
    if (type == MediaArtworkType.ALL) {
      return artwork;
    }

    // get all artwork
    return artwork.stream().filter(ma -> ma.getType() == type).toList();
  }

  /**
   * Adds the genre.
   * 
   * @param genre
   *          the genre
   */
  public void addGenre(MediaGenres genre) {
    if (genre != null && !genres.contains(genre)) {
      genres.add(genre);
    }
  }

  /**
   * Adds the cast member.
   * 
   * @param castMember
   *          the cast member
   */
  public void addCastMember(Person castMember) {
    if (containsCastMember(castMember)) {
      return;
    }
    castMembers.add(castMember);
  }

  /**
   * get all set artwork
   * 
   * @return a list of all artworks
   */
  public List<MediaArtwork> getMediaArt() {
    return artwork;
  }

  /**
   * set all given artwork
   * 
   * @param artwork
   *          a list of all artwork to set
   */
  public void setMediaArt(List<MediaArtwork> artwork) {
    this.artwork.clear();
    if (artwork != null) {
      this.artwork.addAll(artwork);
    }
  }

  /**
   * Adds the media art.
   * 
   * @param ma
   *          the ma
   */
  public void addMediaArt(MediaArtwork ma) {
    if (ma != null) {
      artwork.add(ma);
    }
  }

  /**
   * Clear media art.
   */
  public void clearMediaArt() {
    artwork.clear();
  }

  /**
   * Adds the media art.
   * 
   * @param art
   *          the art
   */
  public void addMediaArt(List<MediaArtwork> art) {
    artwork.addAll(art);
  }

  /**
   * Get all cast members.
   * 
   * @return the cast members
   */
  public List<Person> getCastMembers() {
    return castMembers;
  }

  /**
   * set all given cast members
   * 
   * @param castMembers
   *          a list of cast members to be set
   */
  public void setCastMembers(List<Person> castMembers) {
    this.castMembers.clear();
    if (castMembers != null) {
      this.castMembers.addAll(castMembers);
    }
  }

  /**
   * Add a sub item
   *
   * @param item
   *          the subitem to be added
   */
  public void addSubItem(MediaMetadata item) {
    if (item != null) {
      subItems.add(item);
    }
  }

  /**
   * Get all subitems
   * 
   * @return a list of all sub items
   */
  public List<MediaMetadata> getSubItems() {
    return subItems;
  }

  /**
   * Contains cast member.
   * 
   * @param castMember
   *          the cm
   * @return true, if successful
   */
  private boolean containsCastMember(Person castMember) {
    return castMembers.stream().anyMatch(cm -> cm.getType() == castMember.getType() && StringUtils.equals(cm.getName(), castMember.getName()));
  }

  /**
   * Adds the certification.
   * 
   * @param certification
   *          the certification
   */
  public void addCertification(MediaCertification certification) {
    if (certification != null && !certifications.contains(certification)) {
      certifications.add(certification);
    }
  }

  /**
   * Gets the certifications.
   * 
   * @return the certifications
   */
  public List<MediaCertification> getCertifications() {
    return certifications;
  }

  /**
   * set the given certifications
   * 
   * @param certifications
   *          a list of all certifications to set
   */
  public void setCertifications(List<MediaCertification> certifications) {
    this.certifications.clear();
    if (certifications != null) {
      this.certifications.addAll(certifications);
    }
  }

  /**
   * Adds the trailer. To use only when scraping the metadata also provides the trailers
   * 
   * @param trailer
   *          the trailer
   */
  public void addTrailer(MediaTrailer trailer) {
    if (trailer != null) {
      trailers.add(trailer);
    }
  }

  /**
   * Gets the trailers.
   * 
   * @return the trailers
   */
  public List<MediaTrailer> getTrailers() {
    return trailers;
  }

  /**
   * set all given trailers
   * 
   * @param trailers
   *          a list of all trailers to be set
   */
  public void setTrailers(List<MediaTrailer> trailers) {
    this.trailers.clear();
    if (trailers != null) {
      this.trailers.addAll(trailers);
    }
  }

  /**
   * Sets an ID.
   * 
   * @param key
   *          the ID-key
   * @param object
   *          the id
   */
  public void setId(String key, Object object) {
    if (StringUtils.isNotBlank(key)) {
      String v = String.valueOf(object);
      if ("".equals(v) || "0".equals(v) || "null".equals(v)) {
        ids.remove(key);
      }
      else {
        ids.put(key, object);
      }
    }
  }

  /**
   * remove the given key from the ids table
   *
   * @param key
   *          the key to remove
   */
  public void removeId(String key) {
    ids.remove(key);
  }

  /**
   * Gets an ID.
   * 
   * @param key
   *          the ID-key
   * @return the id
   */
  public Object getId(String key) {
    Object id = ids.get(key);
    if (id == null) {
      return "";
    }
    return id;
  }

  /**
   * Get the id for the given provider id as String
   *
   * @param providerId
   *          the provider Id
   * @return the id as String or null
   */
  public String getIdAsString(String providerId) {
    Object id = ids.get(providerId);
    if (id != null) {
      return String.valueOf(id);
    }

    return null;
  }

  /**
   * Get the id for the given provider id as Integer
   *
   * @param providerId
   *          the provider id
   * @return the id as Integer or null
   */
  public Integer getIdAsInteger(String providerId) {
    Object id = ids.get(providerId);
    if (id != null) {
      if (id instanceof Integer integer) {
        return integer;
      }
      if (id instanceof String string)
        try {
          return Integer.parseInt(string);
        }
        catch (Exception ignored) {
          // nothing to be done here
        }
    }

    return null;
  }

  /**
   * Get the id for the given provider id as int
   *
   * @param providerId
   *          the provider id
   * @return the id as int or 0
   */
  public int getIdAsInt(String providerId) {
    return getIdAsIntOrDefault(providerId, 0);
  }

  /**
   * Get the id for the given provider id as int or the chosen default value
   *
   * @param providerId
   *          the provider id
   * @return the id as int or the default value
   */
  public int getIdAsIntOrDefault(String providerId, int defaultValue) {
    Integer id = getIdAsInteger(providerId);
    if (id == null) {
      return defaultValue;
    }
    return id;
  }

  /**
   * Gets all IDs.
   * 
   * @return the IDs
   */
  public Map<String, Object> getIds() {
    return ids;
  }

  /**
   * Get all production companies
   * 
   * @return a list of all production companies
   */
  public List<String> getProductionCompanies() {
    return productionCompanies;
  }

  /**
   * Set the production companies
   * 
   * @param productionCompanies
   *          set the given list of production companies
   */
  public void setProductionCompanies(List<String> productionCompanies) {
    this.productionCompanies.clear();
    if (productionCompanies != null) {
      this.productionCompanies.addAll(productionCompanies);
    }
  }

  /**
   * Add a production company
   * 
   * @param productionCompany
   *          add the given production company if it is not yet present
   */
  public void addProductionCompany(String productionCompany) {
    if (StringUtils.isBlank(productionCompany)) {
      return;
    }

    if (!productionCompanies.contains(productionCompany)) {
      productionCompanies.add(productionCompany);
    }
  }

  /**
   * Removes the given production company
   * 
   * @param productionCompany
   *          the production company to be removed
   */
  public void removeProductionCompany(String productionCompany) {
    productionCompanies.remove(productionCompany);
  }

  /**
   * Get a list of all spoken languages (2 digit: ISO 639-1)
   * 
   * @return a list of all spoken languages
   */
  public List<String> getSpokenLanguages() {
    return spokenLanguages;
  }

  /**
   * Set the spoken languages (2 digit: ISO 639-1)
   * 
   * @param spokenLanguages
   *          the spoken languages to be set
   */
  public void setSpokenLanguages(List<String> spokenLanguages) {
    this.spokenLanguages.clear();
    if (spokenLanguages != null) {
      this.spokenLanguages.addAll(spokenLanguages);
    }
  }

  /**
   * Adds the given language if it is not present (2 digit: ISO 639-1)
   * 
   * @param language
   *          the language to be set
   */
  public void addSpokenLanguage(String language) {
    if (StringUtils.isBlank(language)) {
      return;
    }

    if (!spokenLanguages.contains(language)) {
      spokenLanguages.add(language);
    }
  }

  /**
   * Removes the given language
   * 
   * @param language
   *          the language to be removed
   */
  public void removeSpokenLanguage(String language) {
    spokenLanguages.remove(language);
  }

  /**
   * Get the list of all countries
   * 
   * @return a list of all countries
   */
  public List<String> getCountries() {
    return countries;
  }

  /**
   * Set the countries
   * 
   * @param countries
   *          the countries to be set
   */
  public void setCountries(List<String> countries) {
    this.countries.clear();
    if (countries != null) {
      this.countries.addAll(countries);
    }
  }

  /**
   * Add the country if it is not present
   * 
   * @param country
   *          the country to be added
   */
  public void addCountry(String country) {
    if (StringUtils.isBlank(country)) {
      return;
    }

    if (!countries.contains(country)) {
      countries.add(country);
    }
  }

  /**
   * Remove the given country
   * 
   * @param country
   *          the country to be removed
   */
  public void removeCountry(String country) {
    countries.remove(country);
  }

  /**
   * Get the title
   * 
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Set the title
   * 
   * @param title
   *          the title to be set
   */
  public void setTitle(String title) {
    this.title = StrgUtils.getNonNullString(title);
  }

  /**
   * Get the original title
   * 
   * @return the original title
   */
  public String getOriginalTitle() {
    return originalTitle;
  }

  /**
   * Set the original title
   * 
   * @param originalTitle
   *          the origial title to be set
   */
  public void setOriginalTitle(String originalTitle) {
    this.originalTitle = StrgUtils.getNonNullString(originalTitle);
  }

  /**
   * Get the original title's language
   *
   * @return the original language
   */
  public String getOriginalLanguage() {
    return originalLanguage;
  }

  /**
   * Set the original title's language
   *
   * @param originalLanguage
   *          the original title to be set
   */
  public void setOriginalLanguage(String originalLanguage) {
    this.originalLanguage = StrgUtils.getNonNullString(originalLanguage);
  }

  /**
   * Get the year
   * 
   * @return the year
   */
  public int getYear() {
    return year;
  }

  /**
   * Set the year
   *
   * @param year
   *          the year to be set
   */
  public void setYear(int year) {
    this.year = year;
  }

  /**
   * Set the year - nullsafe
   *
   * @param year
   *          the year to be set
   */
  public void setYear(Integer year) {
    if (year != null) {
      setYear(year.intValue());
    }
  }

  /**
   * Get the release date
   * 
   * @return the release date
   */
  public Date getReleaseDate() {
    return releaseDate;
  }

  /**
   * Set the release date
   *
   * @param releaseDate
   *          the release date to be set
   */
  public void setReleaseDate(Date releaseDate) {
    if (releaseDate != null) {
      this.releaseDate = releaseDate;
    }
  }

  /**
   * Get the plot
   * 
   * @return the plot
   */
  public String getPlot() {
    return plot;
  }

  /**
   * Set the plot
   * 
   * @param plot
   *          the plot to be set
   */
  public void setPlot(String plot) {
    this.plot = StrgUtils.getNonNullString(plot);
  }

  /**
   * Get the tagline
   * 
   * @return the tagline
   */
  public String getTagline() {
    return tagline;
  }

  /**
   * Set the tagline
   * 
   * @param tagline
   *          the tagline to be set
   */
  public void setTagline(String tagline) {
    this.tagline = StrgUtils.getNonNullString(tagline);
  }

  /**
   * Get the collection name
   * 
   * @return the collection name
   */
  public String getCollectionName() {
    return collectionName;
  }

  /**
   * Set the collection name
   * 
   * @param collectionName
   *          the collection name to be set
   */
  public void setCollectionName(String collectionName) {
    this.collectionName = StrgUtils.getNonNullString(collectionName);
  }

  /**
   * Get the runtime in minutes
   * 
   * @return the runtime in minutes
   */
  public int getRuntime() {
    return runtime;
  }

  /**
   * Set the runtime in minutes (full minutes)
   *
   * @param runtime
   *          the runtime in minutes to be set
   */
  public void setRuntime(int runtime) {
    this.runtime = runtime;
  }

  /**
   * Set the runtime in minutes (full minutes) - nullsafe
   *
   * @param runtime
   *          the runtime in minutes to be set
   */
  public void setRuntime(Integer runtime) {
    if (runtime != null) {
      setRuntime(runtime.intValue());
    }
  }

  /**
   * Get the ratings
   * 
   * @return the ratings
   */
  public List<MediaRating> getRatings() {
    return ratings;
  }

  /**
   * Set the ratings
   * 
   * @param newRatings
   *          the ratings to be set
   */
  public void setRatings(List<MediaRating> newRatings) {
    for (MediaRating rating : newRatings) {
      addRating(rating);
    }
  }

  /**
   * Add a rating
   *
   * @param rating
   *          the rating to be set
   */
  public void addRating(MediaRating rating) {
    if (rating != null && StringUtils.isNotBlank(rating.getId()) && rating.getMaxValue() > 0 && (rating.getRating() > 0 || rating.getVotes() > 0)) {
      if (!ratings.contains(rating)) {
        ratings.add(rating);
      }
    }
  }

  /**
   * Get the place in the top 250 or 0 if not set
   * 
   * @return the place in top 250 or 0
   */
  public int getTop250() {
    return top250;
  }

  /**
   * Set the place in the top 250
   *
   * @param top250
   *          the place to be set
   */
  public void setTop250(int top250) {
    this.top250 = top250;
  }

  /**
   * Set the place in the top 250 - nullsafe
   *
   * @param top250
   *          the place to be set
   */
  public void setTop250(Integer top250) {
    if (top250 != null) {
      setTop250(top250.intValue());
    }
  }

  /**
   * get all available {@link MediaEpisodeNumber}s
   *
   * @return a {@link Map} with all available {@link MediaEpisodeNumber}s
   */
  public Map<MediaEpisodeGroup, MediaEpisodeNumber> getEpisodeNumbers() {
    return Collections.unmodifiableMap(episodeNumbers);
  }

  /**
   * set all given {@link MediaEpisodeNumber}s
   *
   * @param eps
   *          a {@link Map} containing all {@link MediaEpisodeNumber}s
   */
  public void setEpisodeNumbers(Map<MediaEpisodeGroup, MediaEpisodeNumber> eps) {
    if (eps != null) {
      episodeNumbers.clear();
      episodeNumbers.putAll(eps);
    }
  }

  /**
   * Get the {@link MediaEpisodeNumber}
   *
   * @param episodeGroup
   *          the {@link MediaEpisodeGroup} to get the episode number for
   * @return the {@link MediaEpisodeNumber} or null
   */
  public MediaEpisodeNumber getEpisodeNumber(@NotNull MediaEpisodeGroup episodeGroup) {
    return episodeNumbers.get(episodeGroup);
  }

  /**
   * Get the {@link MediaEpisodeNumber}
   *
   * @param episodeGroupType
   *          the {@link MediaEpisodeGroup.EpisodeGroupType} to get the episode number for
   * @return the {@link MediaEpisodeNumber} or null
   */
  public MediaEpisodeNumber getEpisodeNumber(@NotNull MediaEpisodeGroup.EpisodeGroupType episodeGroupType) {
    // match the first available episode group
    for (Map.Entry<MediaEpisodeGroup, MediaEpisodeNumber> entry : episodeNumbers.entrySet()) {
      if (entry.getKey().getEpisodeGroupType() == episodeGroupType) {
        return entry.getValue();
      }
    }

    return null;
  }

  /**
   * Set the episode number
   *
   * @param episodeGroup
   *          the {@link MediaEpisodeGroup.EpisodeGroupType} this episode number belongs to
   * @param season
   *          the season
   * @param episode
   *          the episode
   */
  public void setEpisodeNumber(@NotNull MediaEpisodeGroup episodeGroup, int season, int episode) {
    setEpisodeNumber(new MediaEpisodeNumber(episodeGroup, season, episode));
  }

  /**
   * Set the episode number
   *
   * @param episodeNumber
   *          the {@link MediaEpisodeNumber} to be set
   */
  public void setEpisodeNumber(MediaEpisodeNumber episodeNumber) {
    if (episodeNumber == null || !episodeNumber.isValid()) {
      return;
    }

    episodeNumbers.put(episodeNumber.episodeGroup(), episodeNumber);
  }

  /**
   * Get the airing status
   * 
   * @return the airing status
   */
  public MediaAiredStatus getStatus() {
    return status;
  }

  /**
   * Set the airing status
   *
   * @param status
   *          the airing status to be set
   */
  public void setStatus(MediaAiredStatus status) {
    this.status = status;
  }

  /**
   * Parse/Set the airing status
   *
   * @param statusAsText
   *          the airing status to be parsed and set
   */
  public void setStatus(String statusAsText) {
    this.status = MediaAiredStatus.findAiredStatus(statusAsText);
  }

  /**
   * Get all extra data. Handy key/value store to pass extra data inside a scraper
   * 
   * @return the key/value store
   */
  public Map<String, Object> getExtraData() {
    return extraData;
  }

  /**
   * Add an extra data. Handy key/value store to pass extra data inside a scraper
   * 
   * @param key
   *          the key
   * @param value
   *          the value
   */
  public void addExtraData(String key, Object value) {
    if (StringUtils.isNotBlank(key) && value != null) {
      extraData.put(key, value);
    }
  }

  /**
   * Get an extra data. Handy key/value store to pass extra data inside a scraper
   * 
   * @param key
   *          the key
   * @return the value or null
   */
  public Object getExtraData(String key) {
    return extraData.get(key);
  }

  /**
   * Get the tags
   * 
   * @return a list containing all tags
   */
  public List<String> getTags() {
    return tags;
  }

  /**
   * Set tags
   * 
   * @param tags
   *          the tags to be set
   */
  public void setTags(List<String> tags) {
    this.tags.clear();
    if (tags != null) {
      this.tags.addAll(tags);
    }
  }

  /**
   * Add a new tag
   * 
   * @param tag
   *          the tag
   */
  public void addTag(String tag) {
    if (StringUtils.isBlank(tag)) {
      return;
    }

    if (!tags.contains(tag)) {
      tags.add(tag);
    }
  }

  /**
   * Remove the given tag
   * 
   * @param tag
   *          the tag to be removed
   */
  public void removeTag(String tag) {
    tags.remove(tag);
  }

  /**
   * Add a season name
   *
   * @param episodeGroup
   *          the {@link MediaEpisodeGroup} to set the season name for
   * @param seasonNumber
   *          the season number
   * @param name
   *          the season name
   */
  public void addSeasonName(MediaEpisodeGroup episodeGroup, int seasonNumber, String name) {
    if (seasonNumber > -1 && StringUtils.isNotBlank(name)) {
      seasonNames.computeIfAbsent(episodeGroup, k -> new HashMap<>()).put(seasonNumber, name);
    }
  }

  /**
   * get all set season names
   * 
   * @return the season names
   */
  public Map<MediaEpisodeGroup, Map<Integer, String>> getSeasonNames() {
    return seasonNames;
  }

  /**
   * for introspection on universal scraper
   */
  public void setSeasonNames(Map<MediaEpisodeGroup, Map<Integer, String>> map) {
    this.seasonNames = map;
  }

  /**
   * add a season overview
   *
   * @param episodeGroup
   *          the {@link MediaEpisodeGroup} to set the season name for
   * @param seasonNumber
   *          the season number
   * @param overview
   *          the overview
   */
  public void addSeasonOverview(MediaEpisodeGroup episodeGroup, int seasonNumber, String overview) {
    if (seasonNumber > -1 && StringUtils.isNotBlank(overview)) {
      seasonOverview.computeIfAbsent(episodeGroup, k -> new HashMap<>()).put(seasonNumber, overview);
    }
  }

  /**
   * get the season overview/plot
   *
   * @return the season overview/plot
   */
  public Map<MediaEpisodeGroup, Map<Integer, String>> getSeasonOverview() {
    return seasonOverview;
  }

  /**
   * for introspection on universal scraper
   */
  public void setSeasonOverview(Map<MediaEpisodeGroup, Map<Integer, String>> map) {
    this.seasonOverview = map;
  }

  /**
   * add the given {@link MediaEpisodeGroup} to the available episode groups
   *
   * @param episodeGroup
   *          the {@link MediaEpisodeGroup} to add
   */
  public void addEpisodeGroup(MediaEpisodeGroup episodeGroup) {
    episodeGroups.add(episodeGroup);
  }

  /**
   * get all available {@link MediaEpisodeGroup}s
   *
   * @return a {@link Set} with all available {@link MediaEpisodeGroup}s
   */
  public Set<MediaEpisodeGroup> getEpisodeGroups() {
    return episodeGroups;
  }

  /**
   * set all given {@link MediaEpisodeGroup}s
   * 
   * @param episodeGroups
   *          a {@link Set} os {@link MediaEpisodeGroup}s to set
   */
  public void setEpisodeGroups(Set<MediaEpisodeGroup> episodeGroups) {
    this.episodeGroups.clear();
    this.episodeGroups.addAll(episodeGroups);
  }

  /**
   * get the {@link MediaSearchAndScrapeOptions} used when creating this {@link MediaMetadata}
   * 
   * @return the set {@link MediaSearchAndScrapeOptions} or null if unset
   */
  public MediaSearchAndScrapeOptions getScrapeOptions() {
    return scrapeOptions;
  }

  /**
   * set the {@link MediaSearchAndScrapeOptions}
   * 
   * @param scrapeOptions
   *          the {@link MediaSearchAndScrapeOptions}
   */
  public void setScrapeOptions(MediaSearchAndScrapeOptions scrapeOptions) {
    this.scrapeOptions = scrapeOptions;
  }

  /**
   * generates a SearchResult out of a scraped detail page (when searching via ID)
   *
   * @return
   */
  public MediaSearchResult toSearchResult(MediaType type) {
    MediaSearchResult sr = new MediaSearchResult(providerId, type);
    sr.setIds(getIds());
    sr.setTitle(getTitle());
    sr.setOriginalTitle(getOriginalTitle());
    sr.setYear(getYear());
    return sr;
  }

  /**
   * <p>
   * Uses <code>ReflectionToStringBuilder</code> to generate a <code>toString</code> for the specified object.
   * </p>
   * 
   * @return the String result
   * @see ReflectionToStringBuilder#toString(Object)
   */
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
