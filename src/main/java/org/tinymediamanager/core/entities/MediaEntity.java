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
package org.tinymediamanager.core.entities;

import static org.tinymediamanager.core.Constants.BANNER;
import static org.tinymediamanager.core.Constants.CHARACTERART;
import static org.tinymediamanager.core.Constants.CLEARART;
import static org.tinymediamanager.core.Constants.CLEARLOGO;
import static org.tinymediamanager.core.Constants.DATA_SOURCE;
import static org.tinymediamanager.core.Constants.DATE_ADDED;
import static org.tinymediamanager.core.Constants.DATE_ADDED_AS_STRING;
import static org.tinymediamanager.core.Constants.DISC;
import static org.tinymediamanager.core.Constants.FANART;
import static org.tinymediamanager.core.Constants.HAS_IMAGES;
import static org.tinymediamanager.core.Constants.KEYART;
import static org.tinymediamanager.core.Constants.LOCKED;
import static org.tinymediamanager.core.Constants.LOGO;
import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;
import static org.tinymediamanager.core.Constants.NEWLY_ADDED;
import static org.tinymediamanager.core.Constants.ORIGINAL_FILENAME;
import static org.tinymediamanager.core.Constants.ORIGINAL_TITLE;
import static org.tinymediamanager.core.Constants.PATH;
import static org.tinymediamanager.core.Constants.PLOT;
import static org.tinymediamanager.core.Constants.POSTER;
import static org.tinymediamanager.core.Constants.PRODUCTION_COMPANY;
import static org.tinymediamanager.core.Constants.RATING;
import static org.tinymediamanager.core.Constants.TAGS;
import static org.tinymediamanager.core.Constants.TAGS_AS_STRING;
import static org.tinymediamanager.core.Constants.THUMB;
import static org.tinymediamanager.core.Constants.TITLE;
import static org.tinymediamanager.core.Constants.YEAR;
import static org.tinymediamanager.core.Utils.returnOneWhenFilled;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.IPrintable;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.ParserUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * The Class MediaEntity. The base class for all entities
 * 
 * @author Manuel Laggner
 */
public abstract class MediaEntity extends AbstractModelObject implements IPrintable {
  private static final Logger          LOGGER             = LoggerFactory.getLogger(MediaEntity.class);

  /** The id for the database. */
  protected UUID                       dbId               = UUID.randomUUID();

  @JsonProperty
  protected boolean                    locked             = false;

  @JsonProperty
  protected String                     dataSource         = "";

  /** The ids to store the ID from several metadataproviders. */
  @JsonProperty
  protected Map<String, Object>        ids                = new ConcurrentHashMap<>(0);

  @JsonProperty
  protected String                     title              = "";
  @JsonProperty
  protected String                     originalTitle      = "";
  @JsonProperty
  protected int                        year               = 0;
  @JsonProperty
  protected String                     plot               = "";
  @JsonProperty
  protected String                     path               = "";
  @JsonProperty
  protected Date                       dateAdded          = new Date();
  @JsonProperty
  protected String                     productionCompany  = "";
  @JsonProperty
  protected String                     note               = "";

  @JsonProperty
  protected Map<String, MediaRating>   ratings            = new ConcurrentHashMap<>(0);
  @JsonProperty
  private final List<MediaFile>        mediaFiles         = new ArrayList<>();
  @JsonProperty
  protected final List<String>         tags               = new CopyOnWriteArrayList<>();
  @JsonProperty
  protected Map<MediaFileType, String> artworkUrlMap      = new EnumMap<>(MediaFileType.class);

  @JsonProperty
  protected String                     originalFilename   = "";
  @JsonProperty
  protected String                     lastScraperId      = "";
  @JsonProperty
  protected String                     lastScrapeLanguage = "";

  protected boolean                    newlyAdded         = false;
  protected boolean                    duplicate          = false;
  protected final ReadWriteLock        readWriteLock      = new ReentrantReadWriteLock();

  /**
   * get the main file for this entity
   * 
   * @return the main file
   */
  public abstract MediaFile getMainFile();

  /**
   * get the release date for this entity
   *
   * @return the release/first aired date
   */
  public abstract Date getReleaseDate();

  /**
   * Overwrites all null/empty elements with "other" value (but might be empty also)<br>
   * For lists, check with 'contains' and add.<br>
   * Do NOT merge path, dateAdded, scraped, mediaFiles and other crucial properties!
   * 
   * @param other
   *          the media entity to merge in
   */
  public void merge(MediaEntity other) {
    merge(other, false);
  }

  /**
   * Overwrites all elements with "other" value<br>
   * Do NOT merge path, dateAdded, scraped, mediaFiles and other crucial properties!
   *
   * @param other
   *          the media entity to merge in
   */
  public void forceMerge(MediaEntity other) {
    merge(other, true);
  }

  protected void merge(MediaEntity other, boolean force) {
    if (locked || other == null) {
      return;
    }

    setTitle(StringUtils.isBlank(title) || force ? other.title : title);
    setOriginalTitle(StringUtils.isBlank(originalTitle) || force ? other.originalTitle : originalTitle);
    setYear(year == 0 || force ? other.year : year);
    setPlot(StringUtils.isBlank(plot) || force ? other.plot : plot);
    setProductionCompany(StringUtils.isBlank(productionCompany) || force ? other.productionCompany : productionCompany);
    setOriginalFilename(StringUtils.isBlank(originalFilename) || force ? other.originalFilename : originalFilename);
    setLastScraperId(StringUtils.isBlank(lastScraperId) || force ? other.lastScraperId : lastScraperId);
    setLastScrapeLanguage(StringUtils.isBlank(lastScrapeLanguage) || force ? other.lastScrapeLanguage : lastScrapeLanguage);

    // when force is set, clear the lists/maps and add all other values
    if (force) {
      ids.clear();
      ratings.clear();
      tags.clear();

      artworkUrlMap.clear();
    }

    setRatings(other.ratings);
    setTags(other.tags);

    for (String key : other.getIds().keySet()) {
      if (!ids.containsKey(key)) {
        ids.put(key, other.getId(key));
      }
    }
    for (MediaFileType key : other.getArtworkUrls().keySet()) {
      if (!artworkUrlMap.containsKey(key)) {
        artworkUrlMap.put(key, other.getArtworkUrl(key));
      }
    }

    setNote(StringUtils.isBlank(note) || force ? other.note : note);
  }

  /**
   * Initialize after loading from database.
   */
  public void initializeAfterLoading() {
    sortMediaFiles();

    // remove empty tag, null values and case insensitive duplicates
    Utils.removeEmptyStringsFromList(tags);
    Utils.removeDuplicateStringFromCollectionIgnoreCase(tags);
  }

  protected void sortMediaFiles() {
    Comparator<MediaFile> mediaFileComparator = getMediaFileComparator();
    if (mediaFileComparator != null) {
      mediaFiles.sort(mediaFileComparator);
    }
    else {
      Collections.sort(mediaFiles);
    }
  }

  /**
   * get the INTERNAL ID of this object. Do not confuse it with the IDs from the metadata provider!
   * 
   * @return internal id
   */
  public UUID getDbId() {
    return dbId;
  }

  public void setDbId(UUID id) {
    this.dbId = id;
  }

  /**
   * checks whether this {@link MediaEntity} is locked or not
   * 
   * @return true/false
   */
  public boolean isLocked() {
    return locked;
  }

  /**
   * set the locked status for this {@link MediaEntity}
   * 
   * @param newValue
   *          the locked status
   */
  public void setLocked(boolean newValue) {
    boolean oldValue = this.locked;
    this.locked = newValue;
    firePropertyChange(LOCKED, oldValue, newValue);
  }

  /**
   * Gets the data source.
   *
   * @return the data source
   */
  public String getDataSource() {
    return dataSource;
  }

  /**
   * Sets the data source.
   *
   * @param newValue
   *          the new data source
   */
  public void setDataSource(String newValue) {
    String oldValue = this.dataSource;
    this.dataSource = newValue;
    firePropertyChange(DATA_SOURCE, oldValue, newValue);
  }

  /**
   * get all ID for this object. These are the IDs from the various scraper
   * 
   * @return a map of all IDs
   */
  public Map<String, Object> getIds() {
    return Collections.unmodifiableMap(ids);
  }

  /**
   * get the title of this entity
   * 
   * @return the title or an empty string
   */
  public String getTitle() {
    return title;
  }

  /**
   * get the original title of this entity
   * 
   * @return the original title or an empty string
   */
  public String getOriginalTitle() {
    return originalTitle;
  }

  /**
   * get the plot of this entity
   * 
   * @return the plot or an empty string
   */
  public String getPlot() {
    return plot;
  }

  /**
   * needed as getter<br>
   * better use {@link #getPathNIO()}<br>
   * 
   * @return
   */
  public String getPath() {
    return this.path;
  }

  public String getOriginalFilename() {
    return this.originalFilename;
  }

  /**
   * @return filesystem path or NULL
   */
  public Path getPathNIO() {
    if (StringUtils.isBlank(path)) {
      return null;
    }
    return Paths.get(path).toAbsolutePath();
  }

  /**
   * get the folder name of this {@link MediaEntity}
   * 
   * @return the folder name or an empty string
   */
  public String getFoldername() {
    Path pathNIO = getPathNIO();
    if (pathNIO == null) {
      return "";
    }

    return pathNIO.getFileName().toString();
  }

  /**
   * get the parent relative to the data source as string
   *
   * @return a string which represents the parent relative to the data source
   */
  public String getParent() {
    Path pathNIO = getPathNIO();
    if (pathNIO == null) {
      return "";
    }

    Path parent = Paths.get(this.dataSource).toAbsolutePath().relativize(pathNIO.getParent());

    return parent.toString();
  }

  /**
   * Gets the dimension of the (first) artwork of the given type
   * 
   * @param type
   *          the artwork type
   * @return the dimension of the artwork or a zero dimension if no artwork has been found
   */
  public Dimension getArtworkDimension(MediaFileType type) {
    List<MediaFile> artworks = getMediaFiles(type);
    if (!artworks.isEmpty()) {
      MediaFile mediaFile = artworks.get(0);
      return new Dimension(mediaFile.getVideoWidth(), mediaFile.getVideoHeight());
    }
    return new Dimension(0, 0);
  }

  /**
   * Gets the file name of the (first) artwork of the given type
   * 
   * @param type
   *          the artwork type
   * @return the file name of the artwork or an empty string if nothing has been found
   */
  public String getArtworkFilename(MediaFileType type) {
    List<MediaFile> artworks = getMediaFiles(type);
    if (!artworks.isEmpty()) {
      return artworks.get(0).getFile().toString();
    }
    return "";
  }

  public Map<String, MediaRating> getRatings() {
    return ratings;
  }

  public MediaRating getRating(String id) {
    return ratings.getOrDefault(id, MediaMetadata.EMPTY_RATING);
  }

  /**
   * get the "main" rating
   * 
   * @return the main (preferred) rating
   */
  public abstract MediaRating getRating();

  public MediaRating getUserRating() {
    MediaRating mediaRating = ratings.get(MediaRating.USER);

    if (mediaRating == null) {
      mediaRating = MediaMetadata.EMPTY_RATING;
    }

    return mediaRating;

  }

  public int getYear() {
    return year;
  }

  /**
   * 1980s, 1990s, 2000s, 2010s, ...
   * 
   * @return
   */
  public String getDecadeShort() {
    String decade = "";
    if (year > 0) {
      int len = String.valueOf(year).length();
      String pre = String.valueOf(year).substring(0, len - 1);
      decade = pre + "0s";
    }
    return decade;
  }

  /**
   * 1980-1989, 1990-1999, 2000-2009, ...
   * 
   * @return
   */
  public String getDecadeLong() {
    String decade = "";
    if (year > 0) {
      int len = String.valueOf(year).length();
      String pre = String.valueOf(year).substring(0, len - 1);
      decade = pre + "0-" + pre + "9";
    }
    return decade;
  }

  public void setIds(Map<String, Object> ids) {
    for (Entry<String, Object> entry : ids.entrySet()) {
      if (StringUtils.isNotBlank(entry.getKey()) && entry.getValue() != null) {
        setId(entry.getKey(), entry.getValue());
      }
    }
  }

  public void setTitle(String newValue) {
    String oldValue = title;
    title = newValue == null ? "" : newValue.strip();
    firePropertyChange(TITLE, oldValue, newValue);
  }

  public void setOriginalTitle(String newValue) {
    String oldValue = originalTitle;
    originalTitle = newValue == null ? "" : newValue.strip();
    firePropertyChange(ORIGINAL_TITLE, oldValue, newValue);
  }

  public void setPlot(String newValue) {
    String oldValue = plot;
    plot = newValue == null ? "" : newValue.strip();
    firePropertyChange(PLOT, oldValue, newValue);
  }

  public void setPath(String newValue) {
    String oldValue = path;
    path = newValue;
    firePropertyChange(PATH, oldValue, newValue);
  }

  public void setOriginalFilename(String newValue) {
    String oldValue = originalFilename;
    originalFilename = newValue;
    firePropertyChange(ORIGINAL_FILENAME, oldValue, newValue);
  }

  public void removeRating(String id) {
    MediaRating removedMediaRating = ratings.remove(id);
    if (removedMediaRating != null) {
      firePropertyChange(RATING, null, ratings);
    }
  }

  public void clearRatings() {
    ratings.clear();
    firePropertyChange(RATING, null, ratings);
  }

  public void setRatings(Map<String, MediaRating> newRatings) {
    // preserve the user rating here
    MediaRating userMediaRating = ratings.get(MediaRating.USER);

    ratings.clear();
    for (Entry<String, MediaRating> entry : newRatings.entrySet()) {
      if (entry.getValue() != MediaMetadata.EMPTY_RATING && entry.getValue().getRating() >= 0) {
        setRating(entry.getValue());
      }
    }

    if (userMediaRating != null && !newRatings.containsKey(MediaRating.USER)) {
      setRating(userMediaRating);
    }
  }

  public void setRating(MediaRating mediaRating) {
    if (mediaRating != null && StringUtils.isNotBlank(mediaRating.getId()) && mediaRating.getRating() >= 0) {
      ratings.put(mediaRating.getId(), mediaRating);
      firePropertyChange(RATING, null, mediaRating);
    }
  }

  public void setYear(int newValue) {
    int oldValue = year;
    year = newValue;
    firePropertyChange(YEAR, oldValue, newValue);
  }

  public void setArtworkUrl(String url, MediaFileType type) {
    String oldValue = getArtworkUrl(type);

    switch (type) {
      case POSTER:
      case FANART:
      case BANNER:
      case THUMB:
      case CLEARART:
      case DISC:
      case LOGO:
      case CLEARLOGO:
      case CHARACTERART:
      case KEYART:
      case SEASON_BANNER:
      case SEASON_FANART:
      case SEASON_POSTER:
      case SEASON_THUMB:
        if (StringUtils.isBlank(url)) {
          artworkUrlMap.remove(type);
        }
        else {
          artworkUrlMap.put(type, url);
        }
        break;

      default:
        return;
    }

    firePropertyChange(type.name().toLowerCase(Locale.ROOT) + "Url", oldValue, url);
  }

  /**
   * get the artwork url for the desired type
   * 
   * @param type
   *          the artwork type
   * @return the url to the artwork type or an empty string
   */
  public String getArtworkUrl(MediaFileType type) {
    String url = artworkUrlMap.get(type);
    return url == null ? "" : url;
  }

  /**
   * removed the artwork url for the desired type
   * 
   * @param type
   *          the artwork type
   */
  public void removeArtworkUrl(MediaFileType type) {
    artworkUrlMap.remove(type);
  }

  /**
   * get all artwork urls
   * 
   * @return a map containing all urls
   */
  public Map<MediaFileType, String> getArtworkUrls() {
    return artworkUrlMap;
  }

  public void setArtwork(Path file, MediaFileType type) {
    List<MediaFile> images = getMediaFiles(type);
    MediaFile mediaFile;
    if (!images.isEmpty()) {
      mediaFile = images.get(0);
      mediaFile.setFile(file);
      mediaFile.gatherMediaInformation(true);
      fireAddedEventForMediaFile(mediaFile);
    }
    else {
      mediaFile = new MediaFile(file, type);
      mediaFile.gatherMediaInformation();
      addToMediaFiles(mediaFile);
    }

    firePropertyChange(MEDIA_INFORMATION, false, true);
  }

  /**
   * Get a map of all primary artworks. If there are multiple media files for one artwork type, only the first is returned in the map
   * 
   * @return a map of all found artworks
   */
  public Map<MediaFileType, MediaFile> getArtworkMap() {
    Map<MediaFileType, MediaFile> artworkMap = new EnumMap<>(MediaFileType.class);
    List<MediaFile> mfs = getMediaFiles();
    for (MediaFile mf : mfs) {
      if (!mf.isGraphic()) {
        continue;
      }
      if (!artworkMap.containsKey(mf.getType())) {
        artworkMap.put(mf.getType(), mf);
      }
    }
    return artworkMap;
  }

  public Date getDateAdded() {
    return dateAdded;
  }

  public Date getDateAddedForUi() {
    Date date = null;

    switch (Settings.getInstance().getDateField()) {
      case FILE_CREATION_DATE:
        MediaFile mainMediaFile = getMainFile();
        if (mainMediaFile != null) {
          date = mainMediaFile.getDateCreated();
        }
        break;

      case FILE_LAST_MODIFIED_DATE:
        mainMediaFile = getMainFile();
        if (mainMediaFile != null) {
          date = mainMediaFile.getDateLastModified();
        }
        break;

      case RELEASE_DATE:
        date = getReleaseDate();
        break;

      default:
        date = dateAdded;
        break;

    }

    // sanity check - must not be null - fall back to date added otherwise
    if (date == null) {
      date = dateAdded;
    }

    // sanity check - must not return a date in the future
    Date now = new Date();
    if (now.before(date)) {
      date = dateAdded;
    }

    return date;
  }

  public String getDateAddedAsString() {
    Date date = getDateAddedForUi();
    if (date == null) {
      return "";
    }

    return TmmDateFormat.MEDIUM_DATE_SHORT_TIME_FORMAT.format(dateAdded);
  }

  public void setDateAdded(Date newValue) {
    Date oldValue = this.dateAdded;
    this.dateAdded = newValue;
    firePropertyChange(DATE_ADDED, oldValue, newValue);
    firePropertyChange(DATE_ADDED_AS_STRING, oldValue, newValue);
  }

  public String getProductionCompany() {
    return productionCompany;
  }

  /**
   * Gets the productionCompany as array.
   *
   * @return the productionCompany as array.
   */
  public List<String> getProductionCompanyAsArray() {
    return ParserUtils.split(productionCompany);
  }

  public void setProductionCompany(String newValue) {
    String oldValue = this.productionCompany;
    this.productionCompany = newValue;

    firePropertyChange(PRODUCTION_COMPANY, oldValue, newValue);
  }

  /**
   * checks if this {@link MediaEntity} has been scraped.<br>
   * detect minimum of filled values as to classify this {@link MediaEntity} as "scraped"<br>
   * we do calculate a score of all "filled" values and if that is above a certain level we're rather sure this is scraped<br>
   * some values might come from just reading filenames/external NFO files, but other values can only be filled when scraping inside tmm or importing
   * a complete NFO file
   *
   * @return isScraped
   */
  public boolean isScraped() {
    return calculateScrapeScore() > 5;
  }

  /**
   * this method takes local values/properties and checks if they are "filled" to calculate a score which indicated whether this {@link MediaEntity}
   * has been scraped or not
   * 
   * @return the calculated score
   */
  protected float calculateScrapeScore() {
    float score = 0;

    // all properties from this class - except "title" which is always filled (at leas with the filename)
    score = score + ids.size(); // each given ID increases the score by 1
    score = score + returnOneWhenFilled(originalTitle);
    score = score + returnOneWhenFilled(plot);
    score = score + returnOneWhenFilled(productionCompany);
    score = score + returnOneWhenFilled(ratings);
    score = score + returnOneWhenFilled(artworkUrlMap);
    if (!getMediaFiles(MediaFileType.NFO).isEmpty()) {
      score++;
    }
    score = score + returnOneWhenFilled(lastScraperId);

    return score;
  }

  public void setNote(String newValue) {
    String oldValue = this.note;
    this.note = newValue;
    firePropertyChange("note", oldValue, newValue);
  }

  public String getNote() {
    return this.note;
  }

  public String getLastScraperId() {
    return lastScraperId;
  }

  public void setLastScraperId(String newValue) {
    String oldValue = lastScraperId;
    lastScraperId = newValue;
    firePropertyChange("lastScraperId", oldValue, newValue);
  }

  public String getLastScrapeLanguage() {
    return lastScrapeLanguage;
  }

  public void setLastScrapeLanguage(String newValue) {
    String oldValue = lastScrapeLanguage;
    lastScrapeLanguage = newValue;
    firePropertyChange("lastScrapeLanguage", oldValue, newValue);
  }

  public void setDuplicate() {
    this.duplicate = true;
  }

  public void clearDuplicate() {
    this.duplicate = false;
  }

  public boolean isDuplicate() {
    return this.duplicate;
  }

  /**
   * set the given ID; if the value is zero/"" or null, the key is removed from the existing keys
   *
   * @param key
   *          the ID-key
   * @param value
   *          the ID-value
   */
  public void setId(String key, Object value) {
    // remove ID, if empty/0/null
    // if we only skipped it, the existing entry will stay although someone changed it to empty.
    String v = String.valueOf(value);
    if ("".equals(v) || "0".equals(v) || "null".equals(v)) {
      ids.remove(key);
    }
    else {
      // if the given ID is an imdb id but is not valid, then do not add
      if (MediaMetadata.IMDB.equals(key) && !MediaIdUtil.isValidImdbId(v)) {
        return;
      }

      ids.put(key, value);
    }
    firePropertyChange(key, null, value);

    // fire special events for our well known IDs
    if (MediaMetadata.TMDB.equals(key) || MediaMetadata.IMDB.equals(key) || MediaMetadata.TVDB.equals(key) || MediaMetadata.TRAKT_TV.equals(key)) {
      firePropertyChange(key + "Id", null, value);
    }
  }

  /**
   * remove the given ID
   * 
   * @param key
   *          the ID-key
   */
  public void removeId(String key) {
    Object obj = ids.remove(key);
    if (obj != null) {
      firePropertyChange(key, obj, null);
    }
  }

  /**
   * remove all IDs
   */
  public void removeAllIds() {
    new ArrayList<>(ids.keySet()).forEach(this::removeId);
  }

  /**
   * get the given id
   *
   * @param key
   *          the ID-key
   * @return
   */
  public Object getId(String key) {
    return ids.get(key);
  }

  /**
   * any ID as String or empty
   * 
   * @return the ID-value as String or an empty string
   */
  public String getIdAsString(String key) {
    return MediaIdUtil.getIdAsString(ids, key);
  }

  /**
   * any ID as int or 0
   * 
   * @return the ID-value as int or an empty string
   */
  public int getIdAsInt(String key) {
    return MediaIdUtil.getIdAsInt(ids, key);
  }

  public void addToMediaFiles(MediaFile mediaFile) {
    if (mediaFile == null) {
      return;
    }

    boolean changed = false;

    try {
      readWriteLock.writeLock().lock();
      // only store the MF if it is not in the list or if the type has been changed
      if (mediaFiles.contains(mediaFile)) {
        int i = mediaFiles.indexOf(mediaFile);
        if (i >= 0) {
          MediaFile oldMf = mediaFiles.get(i);
          if (oldMf.getType() != mediaFile.getType()) {
            mediaFiles.remove(i);
          }
        }
      }
      if (!mediaFiles.contains(mediaFile)) {
        mediaFiles.add(mediaFile);
        sortMediaFiles();
        changed = true;
      }
    }
    finally {
      readWriteLock.writeLock().unlock();
    }

    if (changed) {
      firePropertyChange(MEDIA_FILES, null, mediaFiles);
      fireAddedEventForMediaFile(mediaFile);
    }
  }

  public void addToMediaFiles(List<MediaFile> mediaFiles) {
    for (MediaFile mediaFile : ListUtils.nullSafe(mediaFiles)) {
      addToMediaFiles(mediaFile);
    }
  }

  protected void fireAddedEventForMediaFile(MediaFile mediaFile) {
    if (mediaFile == null) {
      return;
    }

    switch (mediaFile.getType()) {
      case FANART, SEASON_FANART:
        firePropertyChange(FANART, null, mediaFile.getPath());
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case POSTER, SEASON_POSTER:
        firePropertyChange(POSTER, null, mediaFile.getPath());
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case BANNER, SEASON_BANNER:
        firePropertyChange(BANNER, null, mediaFile.getPath());
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case THUMB, SEASON_THUMB:
        firePropertyChange(THUMB, null, mediaFile.getPath());
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case CLEARART:
        firePropertyChange(CLEARART, null, mediaFile.getPath());
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case DISC:
        firePropertyChange(DISC, null, mediaFile.getPath());
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case LOGO:
        firePropertyChange(LOGO, null, mediaFile.getPath());
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case CLEARLOGO:
        firePropertyChange(CLEARLOGO, null, mediaFile.getPath());
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case CHARACTERART:
        firePropertyChange(CHARACTERART, null, mediaFile.getPath());
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case KEYART:
        firePropertyChange(KEYART, null, mediaFile.getPath());
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      default:
        break;
    }
  }

  protected void fireRemoveEventForMediaFile(MediaFile mediaFile) {
    if (mediaFile == null) {
      return;
    }

    switch (mediaFile.getType()) {
      case FANART, SEASON_FANART:
        firePropertyChange(FANART, null, "");
        firePropertyChange(HAS_IMAGES, true, false);
        break;

      case POSTER, SEASON_POSTER:
        firePropertyChange(POSTER, null, "");
        firePropertyChange(HAS_IMAGES, true, false);
        break;

      case BANNER, SEASON_BANNER:
        firePropertyChange(BANNER, null, "");
        firePropertyChange(HAS_IMAGES, true, false);
        break;

      case THUMB, SEASON_THUMB:
        firePropertyChange(THUMB, null, "");
        firePropertyChange(HAS_IMAGES, true, false);
        break;

      case CLEARART:
        firePropertyChange(CLEARART, null, "");
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case DISC:
        firePropertyChange(DISC, null, "");
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case LOGO:
        firePropertyChange(LOGO, null, "");
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case CLEARLOGO:
        firePropertyChange(CLEARLOGO, null, "");
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case CHARACTERART:
        firePropertyChange(CHARACTERART, null, "");
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      case KEYART:
        firePropertyChange(KEYART, null, "");
        firePropertyChange(HAS_IMAGES, false, true);
        break;

      default:
        break;
    }
  }

  public List<MediaFile> getMediaFiles() {
    List<MediaFile> mf = new ArrayList<>();

    try {
      readWriteLock.readLock().lock();
      mf.addAll(mediaFiles);
    }
    finally {
      readWriteLock.readLock().unlock();
    }
    return mf;
  }

  public boolean hasMediaFiles() {
    return !mediaFiles.isEmpty();
  }

  /**
   * gets all MediaFiles from specific type<br>
   * <b>Can be one or multiple types!</b>
   * 
   * @param types
   *          1-N types
   * @return list of MF (may be empty, but never null)
   */
  public List<MediaFile> getMediaFiles(MediaFileType... types) {
    List<MediaFile> mf = new ArrayList<>();

    try {
      readWriteLock.readLock().lock();
      for (MediaFile mediaFile : mediaFiles) {
        boolean match = false;
        for (MediaFileType type : types) {
          if (mediaFile.getType().equals(type)) {
            match = true;
          }
        }
        if (match) {
          mf.add(mediaFile);
        }
      }
      return mf;
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * gets the BIGGEST MediaFile of type(s)<br>
   * useful for getting the right MF for displaying mediaInformation
   * 
   * @return biggest MF
   */
  public MediaFile getBiggestMediaFile(MediaFileType... types) {
    MediaFile mf = null;

    try {
      readWriteLock.readLock().lock();
      for (MediaFile mediaFile : mediaFiles) {
        for (MediaFileType type : types) {
          if (mediaFile.getType().equals(type)) {
            if (mf == null || mediaFile.getFilesize() >= mf.getFilesize()) {
              mf = mediaFile;
            }
          }
        }
      }
      return mf;
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  public long getTotalFilesize() {
    try {
      readWriteLock.readLock().lock();
      long result = 0;
      for (MediaFile mediaFile : mediaFiles) {
        result += mediaFile.getFilesize();
      }
      return result;
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * From all MediaFiles of specified type, get the newest one (according to MI filedate)
   * 
   * @param types
   *          the MediaFileTypes to get the MediaFile for
   * @return NULL or MF
   */
  public MediaFile getNewestMediaFilesOfType(MediaFileType... types) {
    MediaFile mf = null;

    try {
      readWriteLock.readLock().lock();
      for (MediaFile mediaFile : mediaFiles) {
        for (MediaFileType type : types) {
          if (mediaFile.getType().equals(type)) {
            if (mf == null || mediaFile.getFiledate() >= mf.getFiledate()) {
              // get the latter one
              mf = new MediaFile(mediaFile);
            }
          }
        }
      }
      return mf;
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * gets all MediaFiles EXCEPT from specific type<br>
   * <b>Can be one or multiple types!</b>
   * 
   * @param types
   *          1-N types
   * @return list of MF (may be empty, but never null)
   */
  public List<MediaFile> getMediaFilesExceptType(MediaFileType... types) {
    List<MediaFile> mf = new ArrayList<>();

    try {
      readWriteLock.readLock().lock();
      for (MediaFile mediaFile : mediaFiles) {
        boolean match = false;
        for (MediaFileType type : types) {
          if (mediaFile.getType().equals(type)) {
            match = true;
          }
        }
        if (!match) {
          mf.add(mediaFile);
        }
      }
      return mf;
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  public void removeAllMediaFiles() {
    List<MediaFile> changedMediafiles = new ArrayList<>(mediaFiles);

    try {
      readWriteLock.writeLock().lock();
      for (int i = mediaFiles.size() - 1; i >= 0; i--) {
        mediaFiles.remove(i);
      }
    }
    finally {
      readWriteLock.writeLock().unlock();
    }

    for (MediaFile mediaFile : changedMediafiles) {
      fireRemoveEventForMediaFile(mediaFile);
    }
  }

  public void removeFromMediaFiles(MediaFile mediaFile) {
    boolean changed = false;

    try {
      readWriteLock.writeLock().lock();
      changed = mediaFiles.remove(mediaFile);
    }
    finally {
      readWriteLock.writeLock().unlock();
    }

    if (changed) {
      firePropertyChange(MEDIA_FILES, null, mediaFiles);
      fireRemoveEventForMediaFile(mediaFile);
    }
  }

  public void removeAllMediaFilesExceptType(MediaFileType type) {
    List<MediaFile> changedMediafiles = new ArrayList<>();

    try {
      readWriteLock.writeLock().lock();
      for (int i = mediaFiles.size() - 1; i >= 0; i--) {
        MediaFile mediaFile = mediaFiles.get(i);
        if (!mediaFile.getType().equals(type)) {
          mediaFiles.remove(i);
          changedMediafiles.add(mediaFile);
        }
      }
    }
    finally {
      readWriteLock.writeLock().unlock();
    }

    if (!changedMediafiles.isEmpty()) {
      for (MediaFile mediaFile : changedMediafiles) {
        fireRemoveEventForMediaFile(mediaFile);
      }
      firePropertyChange(MEDIA_FILES, null, mediaFiles);
    }
  }

  public void removeAllMediaFiles(MediaFileType type) {
    List<MediaFile> changedMediafiles = new ArrayList<>();

    try {
      readWriteLock.writeLock().lock();
      for (int i = mediaFiles.size() - 1; i >= 0; i--) {
        MediaFile mediaFile = mediaFiles.get(i);
        if (mediaFile.getType().equals(type)) {
          mediaFiles.remove(i);
          changedMediafiles.add(mediaFile);
        }
      }
    }
    finally {
      readWriteLock.writeLock().unlock();
    }

    if (!changedMediafiles.isEmpty()) {
      for (MediaFile mediaFile : changedMediafiles) {
        fireRemoveEventForMediaFile(mediaFile);
      }
      firePropertyChange(MEDIA_FILES, null, mediaFiles);
    }
  }

  /**
   * <b>PHYSICALLY</b> deletes all {@link MediaFile}s of the given type
   *
   * @param type
   *          the {@link MediaFileType} for all {@link MediaFile}s to delete
   */
  public void deleteMediaFiles(MediaFileType type) {
    getMediaFiles(type).forEach(mediaFile -> {
      mediaFile.deleteSafely(getDataSource());
      removeFromMediaFiles(mediaFile);
    });
  }

  public void updateMediaFilePath(Path oldPath, Path newPath) {
    List<MediaFile> mfs = new ArrayList<>();

    try {
      readWriteLock.readLock().lock();
      mfs.addAll(this.mediaFiles);
    }
    finally {
      readWriteLock.readLock().unlock();
    }

    for (MediaFile mf : mfs) {
      // update the cached image by just MOVEing it around
      Path oldCache = ImageCache.getAbsolutePath(mf);
      mf.replacePathForRenamedFolder(oldPath, newPath);

      if (mf.isGraphic()) {
        if (Files.exists(oldCache)) {
          Path newCache = ImageCache.getAbsolutePath(mf);
          LOGGER.trace("updating imageCache {} -> {}", oldCache, newCache);
          // just use plain move here, since we do not need all the safety checks done in our method
          try {
            Files.move(oldCache, newCache);
          }
          catch (IOException e) {
            LOGGER.warn("Error moving cached file - '{}'", e.getMessage());
          }
        }
      }
    }
  }

  public void cacheImages() {
    // re-build the image cache afterwards in an own thread
    List<MediaFile> imageFiles = getImagesToCache();
    imageFiles.forEach(ImageCache::cacheImageAsync);
  }

  public List<MediaFile> getImagesToCache() {
    if (!Settings.getInstance().isImageCache()) {
      return Collections.emptyList();
    }
    return getMediaFiles().stream().filter(MediaFile::isGraphic).toList();
  }

  public void gatherMediaFileInformation(boolean force) {
    List<MediaFile> mfs = new ArrayList<>();

    try {
      readWriteLock.readLock().lock();
      mfs.addAll(this.mediaFiles);
    }
    finally {
      readWriteLock.readLock().unlock();
    }

    for (MediaFile mediaFile : mfs) {
      mediaFile.gatherMediaInformation(force);
    }

    // for all subtitle and audio tracks, evaluate language from FILENAME (since we need the video basename here!!!)
    String video = FilenameUtils.getBaseName(getMainFile().getFilenameWithoutStacking());
    for (MediaFile mediaFile : mfs) {
      if (mediaFile.getType() == MediaFileType.SUBTITLE || mediaFile.getType() == MediaFileType.AUDIO) {

        MediaStreamInfo info = MediaFileHelper.gatherLanguageInformation(mediaFile.getBasename(), video);
        if (mediaFile.getType() == MediaFileType.SUBTITLE && !mediaFile.getSubtitles().isEmpty()) {
          MediaFileSubtitle sub = mediaFile.getSubtitles().get(0);
          // if we have detected a locale (which is more specific than language alone) us this
          if (sub.getLanguage().isEmpty() || info.getLanguage().matches("[a-zA-Z][a-zA-Z][_-].*")) {
            sub.setLanguage(info.getLanguage());
          }
          sub.setTitle(info.getTitle());
          sub.set(info.getFlags());
        }
        else if (mediaFile.getType() == MediaFileType.AUDIO && mediaFile.getAudioChannels().isEmpty()) {
          MediaFileAudioStream audio = mediaFile.getAudioStreams().get(0);
          // if we have detected a locale (which is more specific than language alone) us this
          if (StringUtils.isBlank(audio.getLanguage()) || info.getLanguage().matches("[a-zA-Z][a-zA-Z][_-].*")) {
            audio.setLanguage(info.getLanguage());
          }
          if (StringUtils.isBlank(audio.getTitle())) {
            audio.setTitle(info.getTitle());
          }
          audio.set(info.getFlags());
        }

      }
    }

    firePropertyChange(MEDIA_INFORMATION, false, true);
  }

  public void fireEventForChangedMediaInformation() {
    firePropertyChange(MEDIA_INFORMATION, false, true);
  }

  public boolean isNewlyAdded() {
    return this.newlyAdded;
  }

  public void setNewlyAdded(boolean newValue) {
    boolean oldValue = this.newlyAdded;
    this.newlyAdded = newValue;
    firePropertyChange(NEWLY_ADDED, oldValue, newValue);
  }

  /**
   * Adds the given {@link Collection} the to tags.
   *
   * @param newTags
   *          the new tags
   */
  public void addToTags(Collection<String> newTags) {
    Set<String> newItems = new LinkedHashSet<>();

    // do not accept duplicates or empty tags
    for (String tag : ListUtils.nullSafe(newTags)) {
      if (StringUtils.isBlank(tag) || tags.stream().anyMatch(tag::equalsIgnoreCase)) {
        continue;
      }
      newItems.add(tag);
    }

    if (newItems.isEmpty()) {
      return;
    }

    tags.addAll(newItems);
    firePropertyChange(TAGS, null, tags);
    firePropertyChange(TAGS_AS_STRING, null, tags);
  }

  /**
   * Removes the from tags.
   *
   * @param removeTag
   *          the remove tag
   */
  public void removeFromTags(String removeTag) {
    if (tags.remove(removeTag)) {
      firePropertyChange(TAGS, null, removeTag);
      firePropertyChange(TAGS_AS_STRING, null, removeTag);
    }
  }

  /**
   * Sets the tags.
   *
   * @param newTags
   *          the new tags
   */
  @JsonSetter
  public void setTags(List<String> newTags) {
    // two way sync of tags
    ListUtils.mergeLists(tags, newTags);
    Utils.removeEmptyStringsFromList(tags);
    Utils.removeDuplicateStringFromCollectionIgnoreCase(tags);

    firePropertyChange(TAGS, null, newTags);
    firePropertyChange(TAGS_AS_STRING, null, newTags);
  }

  /**
   * Gets the tag as string.
   *
   * @return the tag as string
   */
  public String getTagsAsString() {
    StringBuilder sb = new StringBuilder();
    for (String tag : tags) {
      if (!StringUtils.isBlank(sb)) {
        sb.append(", ");
      }
      sb.append(tag);
    }
    return sb.toString();
  }

  /**
   * Gets the tags.
   *
   * @return the tags
   */
  public List<String> getTags() {
    return this.tags;
  }

  /**
   * Remove all Tags from List
   */
  public void removeAllTags() {
    tags.clear();
    firePropertyChange(TAGS, null, tags);
    firePropertyChange(TAGS_AS_STRING, null, tags);
  }

  public abstract void saveToDb();

  public abstract void callbackForGatheredMediainformation(MediaFile mediaFile);

  public abstract void callbackForWrittenArtwork(MediaArtworkType type);

  protected abstract Comparator<MediaFile> getMediaFileComparator();

  protected void mergePersons(List<Person> baseList, List<Person> newItems) {
    // if any of these lists is null, we cannot do anything here
    if (baseList == null || newItems == null) {
      return;
    }

    // add new ones in the right order
    for (int i = 0; i < newItems.size(); i++) {
      Person entry = newItems.get(i);
      if (!baseList.contains(entry)) {
        try {
          baseList.add(i, entry);
        }
        catch (IndexOutOfBoundsException e) {
          baseList.add(entry);
        }
      }
      else {
        // or update existing ones
        int indexOldList = baseList.indexOf(entry);

        // merge the entries (e.g. use thumb url/profile/ids from both)
        Person oldPerson = baseList.get(indexOldList);
        oldPerson.merge(entry);

        if (i != indexOldList) {
          Person oldEntry = baseList.remove(indexOldList); // NOSONAR
          try {
            baseList.add(i, oldEntry);
          }
          catch (IndexOutOfBoundsException e) {
            baseList.add(oldEntry);
          }
        }
      }
    }
  }

  @Override
  public String toPrintable() {
    return getTitle();
  }
}
