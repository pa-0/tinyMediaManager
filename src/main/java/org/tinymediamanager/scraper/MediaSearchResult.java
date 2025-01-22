/*
 * Copyright 2012 - 2025 Manuel Laggner
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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.StrgUtils;

/**
 * The Class MediaSearchResult.
 *
 * @author Manuel Laggner
 * @since 1.0
 */
public class MediaSearchResult implements Comparable<MediaSearchResult> {
  private static final Logger       LOGGER           = LoggerFactory.getLogger(MediaSearchResult.class);

  private final Map<String, Object> ids              = new HashMap<>();

  private MediaType                 type;
  private String                    providerId;
  private String                    url              = "";
  private String                    title            = "";
  private String                    overview         = "";
  private int                       year             = 0;
  private String                    originalTitle    = "";
  private String                    originalLanguage = "";
  private float                     score            = 0;
  private MediaMetadata             metadata         = null;
  private String                    posterUrl        = "";

  public MediaSearchResult(String providerId, MediaType type) {
    this.providerId = providerId;
    this.type = type;
  }

  public MediaSearchResult(String providerId, MediaType type, float score) {
    this.providerId = providerId;
    this.type = type;
    this.score = score;
  }

  public MediaSearchResult(String providerId, MediaType type, String id, String title, int year, float score) {
    this.providerId = providerId;
    this.type = type;
    this.ids.put(providerId, StrgUtils.getNonNullString(id));
    this.title = StrgUtils.getNonNullString(title);
    this.year = year;
    this.score = score;
  }

  private MediaSearchResult(Builder builder) {
    type = builder.type;
    setProviderId(builder.providerId);
    setId(builder.id);
    setUrl(builder.url);
    setTitle(builder.title);
    setOverview(builder.overview);
    setYear(builder.year);
    setOriginalTitle(builder.originalTitle);
    setOriginalLanguage(builder.originalLanguage);
    setScore(builder.score);
    setMetadata(builder.metadata);
    setPosterUrl(builder.posterUrl);
  }

  /**
   * merges all entries from other MSR into ours, IF VALUES ARE EMPTY<br>
   * <b>needs testing!</b>
   *
   * @param msr
   *          other MediaSerachResult
   */
  public void mergeFrom(MediaSearchResult msr) {
    if (msr == null || !StringUtils.equals(providerId, msr.providerId) || type != msr.getMediaType()) {
      return;
    }

    url = StringUtils.isEmpty(url) ? msr.getUrl() : url;
    title = StringUtils.isEmpty(title) ? msr.getTitle() : title;
    year = year == 0 ? msr.getYear() : year;
    originalTitle = StringUtils.isEmpty(originalTitle) ? msr.getOriginalTitle() : originalTitle;
    originalLanguage = StringUtils.isEmpty(originalLanguage) ? msr.getOriginalLanguage() : originalLanguage;
    posterUrl = StringUtils.isEmpty(posterUrl) ? msr.getPosterUrl() : posterUrl;

    for (String key : msr.getIds().keySet()) {
      if (!ids.containsKey(key)) {
        ids.put(key, msr.getIds().get(key));
      }
    }

    if (metadata == null) {
      metadata = msr.getMediaMetadata();
    }
    else {
      metadata.mergeFrom(msr.getMediaMetadata());
    }
  }

  public void mergeFrom(MediaMetadata mediaMetadata) {
    if (mediaMetadata == null) {
      return;
    }

    this.metadata = mediaMetadata;

    ids.putAll(mediaMetadata.getIds());
    setTitle(mediaMetadata.getTitle());
    setOriginalTitle(mediaMetadata.getOriginalTitle());
    setYear(mediaMetadata.getYear());
    setOverview(mediaMetadata.getPlot());

    if (!mediaMetadata.getMediaArt(MediaArtwork.MediaArtworkType.POSTER).isEmpty()) {
      MediaArtwork poster = mediaMetadata.getMediaArt(MediaArtwork.MediaArtworkType.POSTER).get(0);
      setPosterUrl(poster.getPreviewUrl());
    }
  }

  /**
   * Get the original title of this search result
   *
   * @return the original title
   */
  public String getOriginalTitle() {
    return originalTitle;
  }

  /**
   * Set the original title for this search result
   *
   * @param originalTitle
   *          the original title
   */
  public void setOriginalTitle(String originalTitle) {
    this.originalTitle = StrgUtils.getNonNullString(originalTitle);
  }

  /**
   * Get the original language of this search result
   *
   * @return the original language
   */
  public String getOriginalLanguage() {
    return originalLanguage;
  }

  /**
   * Set the original language for this search result
   *
   * @param originalLanguage
   *          the original language
   */
  public void setOriginalLanguage(String originalLanguage) {
    this.originalLanguage = StrgUtils.getNonNullString(originalLanguage);
  }

  /**
   * Get the provider id
   *
   * @return the provider id
   */
  public String getProviderId() {
    return providerId;
  }

  /**
   * Set the provider id
   *
   * @param providerId
   *          the provider id
   */
  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  /**
   * Get the title of this search result
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Set the title of this search result
   *
   * @param title
   *          the title
   */
  public void setTitle(String title) {
    this.title = StrgUtils.getNonNullString(title);
  }

  public String getOverview() {
    return overview;
  }

  public void setOverview(String overview) {
    this.overview = overview;
  }

  /**
   * Get the year of this search result
   *
   * @return the year
   */
  public int getYear() {
    return year;
  }

  /**
   * Set the year of this search result
   *
   * @param year
   *          the year
   */
  public void setYear(int year) {
    this.year = year;
  }

  /**
   * Set the year of this search result (nullsafe)
   *
   * @param year
   *          the year
   */
  public void setYear(Integer year) {
    if (year != null) {
      setYear(year.intValue());
    }
  }

  /**
   * Get the score of this search result. 1.0 is perfect match
   *
   * @return the score
   */
  public float getScore() {
    return score;
  }

  /**
   * Set the score of this result
   *
   * @param score
   *          the result
   */
  public void setScore(float score) {
    this.score = score;
  }

  /**
   * Set the score of this result (nullsafe)
   *
   * @param score
   *          the result
   */
  public void setScore(Float score) {
    if (score != null) {
      setScore(score.floatValue());
    }
  }

  /**
   * Get the url to this search result
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Set the url to this search result
   *
   * @param url
   *          the url
   */
  public void setUrl(String url) {
    this.url = StrgUtils.getNonNullString(url);
  }

  /**
   * Get the media type this search result is for
   *
   * @return the media type
   */
  public MediaType getMediaType() {
    return type;
  }

  /**
   * sets the MediaType (used for filtering search results)
   */
  public void setMediaType(MediaType type) {
    this.type = type;
  }

  /**
   * Get the id of this search result
   *
   * @return the id
   */
  public Object getId() {
    return ids.get(providerId);
  }

  /**
   * Set the id of this search result
   *
   * @param id
   *          the search result id
   */
  public void setId(String id) {
    this.ids.put(providerId, StrgUtils.getNonNullString(id));
  }

  /**
   * Set an media id for a provider id
   *
   * @param providerId
   *          the provider id
   * @param id
   *          the media id
   */
  public void setId(String providerId, String id) {
    ids.put(providerId, StrgUtils.getNonNullString(id));
  }

  /**
   * set all given ids
   *
   * @param ids
   *          the ids to set
   */
  public void setIds(Map<String, Object> ids) {
    if (ids == null) {
      return;
    }

    for (Map.Entry<String, Object> entry : ids.entrySet()) {
      if (entry.getValue() == null) {
        continue;
      }

      setId(entry.getKey(), entry.getValue().toString());
    }
  }

  /**
   * Get the IMDB id
   *
   * @return the IMDB id
   */
  public String getIMDBId() {
    String imdbId = "";

    // via imdb
    Object obj = ids.get(MediaMetadata.IMDB);
    if (obj != null) {
      imdbId = obj.toString();
    }

    // legacy ID
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      obj = ids.get("imdbId");
      if (obj != null) {
        imdbId = obj.toString();
      }
    }

    // prevent NPE
    if (StringUtils.isBlank(imdbId)) {
      return "";
    }

    return imdbId;
  }

  /**
   * any ID as int or 0
   *
   * @return the ID-value as int or an empty string
   */
  public int getIdAsInt(String key) {
    int id = 0;
    try {
      id = Integer.parseInt(String.valueOf(ids.get(key)));
    }
    catch (Exception e) {
      return 0;
    }
    return id;
  }

  /**
   * any ID as String or empty
   *
   * @return the ID-value as String or an empty string
   */
  public String getIdAsString(String key) {
    Object obj = ids.get(key);
    if (obj == null) {
      return "";
    }
    return String.valueOf(obj);
  }

  /**
   * get all given ids
   *
   * @return a map full of ids
   */
  public Map<String, Object> getIds() {
    return ids;
  }

  /**
   * Set the IMDB id
   *
   * @param imdbid
   *          the IMDB id
   */
  public void setIMDBId(String imdbid) {
    if (MediaIdUtil.isValidImdbId(imdbid)) {
      ids.put(MediaMetadata.IMDB, imdbid);
    }
  }

  /**
   * Get the MediaMetadata
   *
   * @return the MediaMetadata
   */
  public MediaMetadata getMediaMetadata() {
    return metadata;
  }

  /**
   * Set the MediaMetadata if you already got the whole meta data while searching (for buffering)
   *
   * @param md
   *          the MediaMetadata
   */
  public void setMetadata(MediaMetadata md) {
    metadata = md;
  }

  /**
   * Get the poster url
   *
   * @return the poster url
   */
  public String getPosterUrl() {
    return posterUrl;
  }

  /**
   * calculate the search score by comparing the available result with the search options
   *
   * @param options
   *          the search options which have been used for searching
   */
  public void calculateScore(MediaSearchAndScrapeOptions options) {

    // compare score based on names (translated and original title)
    float calculatedScore = Math.max(MetadataUtil.calculateScore(options.getSearchQuery(), title),
        MetadataUtil.calculateScore(options.getSearchQuery(), originalTitle));

    float yearPenalty = MetadataUtil.calculateYearPenalty(options.getSearchYear(), year);
    if (yearPenalty > 0) {
      LOGGER.trace("parsed year does not match search result year - downgrading score by {}", yearPenalty);
      calculatedScore -= yearPenalty;
    }

    if (StringUtils.isBlank(posterUrl)) {
      // no poster?
      LOGGER.trace("no poster - downgrading score by 0.01");
      calculatedScore -= 0.01f;
    }

    LOGGER.debug(String.format("Similarity Score: [%s] [%s / %s]=[%s]", options.getSearchQuery(), title, originalTitle, calculatedScore));
    setScore(calculatedScore);
  }

  /**
   * Set the poster url
   *
   * @param posterUrl
   *          the poster url
   */
  public void setPosterUrl(String posterUrl) {
    this.posterUrl = StrgUtils.getNonNullString(posterUrl);
  }

  @Override
  public int compareTo(MediaSearchResult arg0) {
    if (getScore() < arg0.getScore()) {
      return 1;
    }
    else if (getScore() == arg0.getScore()) {
      // same score - rank on year
      if (year == arg0.getYear()) {
        // same year too? we just need to sort by _anything_
        return Integer.compare(hashCode(), arg0.hashCode());
      }
      else {
        return Integer.compare(getYear(), arg0.getYear());
      }
    }
    else {
      return -1;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    MediaSearchResult result = (MediaSearchResult) o;
    return Objects.equals(providerId, result.providerId) && Objects.equals(type, result.type) && Objects.equals(title, result.title)
        && ids.equals(result.ids);
  }

  @Override
  public int hashCode() {
    return Objects.hash(providerId, type, title, ids);
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
    return (new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE) {
      @Override
      protected boolean accept(Field f) {
        return super.accept(f) && !f.getName().equals("metadata");
      }
    }).toString();
  }

  public static final class Builder {

    private final MediaType type;

    private String          id;
    private String          providerId;
    private String          url;
    private String          title;
    private String          overview;
    private int             year;
    private String          originalTitle;
    private String          originalLanguage;
    private float           score;
    private MediaMetadata   metadata;
    private String          posterUrl;

    public Builder(@Nonnull MediaType type) {
      this.type = type;
    }

    @Nonnull
    public Builder providerId(@Nonnull String val) {
      providerId = val;
      return this;
    }

    @Nonnull
    public Builder id(@Nonnull String val) {
      id = val;
      return this;
    }

    @Nonnull
    public Builder url(@Nonnull String val) {
      url = val;
      return this;
    }

    @Nonnull
    public Builder title(@Nonnull String val) {
      title = val;
      return this;
    }

    @Nonnull
    public Builder overview(@Nonnull String val) {
      overview = val;
      return this;
    }

    @Nonnull
    public Builder year(int val) {
      year = val;
      return this;
    }

    @Nonnull
    public Builder originalTitle(@Nonnull String val) {
      originalTitle = val;
      return this;
    }

    @Nonnull
    public Builder originalLanguage(@Nonnull String val) {
      originalLanguage = val;
      return this;
    }

    @Nonnull
    public Builder score(float val) {
      score = val;
      return this;
    }

    @Nonnull
    public Builder metadata(@Nonnull MediaMetadata val) {
      metadata = val;
      return this;
    }

    @Nonnull
    public Builder posterUrl(@Nonnull String val) {
      posterUrl = val;
      return this;
    }

    @Nonnull
    public MediaSearchResult build() {
      return new MediaSearchResult(this);
    }
  }
}
