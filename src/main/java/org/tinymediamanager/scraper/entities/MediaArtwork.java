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
package org.tinymediamanager.scraper.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.scraper.util.StrgUtils;

/**
 * This class is used to represent an artwork for a media
 * 
 * @author Manuel Laggner
 * @since 1.0
 */
public class MediaArtwork {
  public static final int MAX_IMAGE_SIZE_ORDER = 32;

  /**
   * The different types of artwork we know
   * 
   * @author Manuel Laggner
   * @since 1.0
   */
  public enum MediaArtworkType {
    // @formatter:off
    BACKGROUND, 
    BANNER, 
    POSTER, 
    ACTOR,
    SEASON_POSTER,
    SEASON_FANART,
    SEASON_BANNER,
    SEASON_THUMB,
    THUMB, 
    CLEARART,
    KEYART,
    CHARACTERART,
    DISC, 
    @Deprecated
    LOGO,
    CLEARLOGO,
    ALL
    // @formatter:on
  }

  /**
   * All available poster sizes
   * 
   * @author Manuel Laggner
   * @since 1.0
   */
  public enum PosterSizes {
    XLARGE(TmmResourceBundle.getString("Settings.image.xlarge"), 2000, 3000, 16),
    LARGE(TmmResourceBundle.getString("Settings.image.large"), 1000, 1500, 8),
    BIG(TmmResourceBundle.getString("Settings.image.big"), 500, 750, 4),
    MEDIUM(TmmResourceBundle.getString("Settings.image.medium"), 342, 513, 2),
    SMALL(TmmResourceBundle.getString("Settings.image.small"), 185, 278, 1);

    private final String text;
    private final int    width;
    private final int    height;
    private final int    order;

    PosterSizes(String text, int width, int height, int order) {
      this.text = text + ": ~" + width + "x" + height + "px";
      this.width = width;
      this.height = height;
      this.order = order;
    }

    @Override
    public String toString() {
      return text;
    }

    public int getOrder() {
      return order;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }

    public static int getSizeOrder(int width) {
      for (var entry : values()) {
        if (width >= entry.width) {
          return entry.order;
        }
      }
      return 0;
    }
  }

  /**
   * All available fanart sizes
   * 
   * @author Manuel Laggner
   * @since 1.0
   */
  public enum FanartSizes {
    XLARGE(TmmResourceBundle.getString("Settings.image.xlarge"), 3840, 2160, 16),
    LARGE(TmmResourceBundle.getString("Settings.image.large"), 1920, 1080, 8),
    MEDIUM(TmmResourceBundle.getString("Settings.image.medium"), 1280, 720, 2),
    SMALL(TmmResourceBundle.getString("Settings.image.small"), 300, 168, 1);

    private final String text;
    private final int    width;
    private final int    height;
    private final int    order;

    FanartSizes(String text, int width, int height, int order) {
      this.text = text + ": ~" + width + "x" + height + "px";
      this.width = width;
      this.height = height;
      this.order = order;
    }

    @Override
    public String toString() {
      return text;
    }

    public int getOrder() {
      return order;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }

    public static int getSizeOrder(int width) {
      for (var entry : values()) {
        if (width >= entry.width) {
          return entry.order;
        }
      }
      return 0;
    }
  }

  /**
   * All available thumb sizes
   *
   * @author Manuel Laggner
   * @since 1.0
   */
  public enum ThumbSizes {
    XLARGE(TmmResourceBundle.getString("Settings.image.xlarge"), 3840, 2160, 16),
    LARGE(TmmResourceBundle.getString("Settings.image.large"), 1920, 1080, 8),
    BIG(TmmResourceBundle.getString("Settings.image.big"), 1280, 720, 4),
    MEDIUM(TmmResourceBundle.getString("Settings.image.medium"), 960, 540, 2), // kodi preference
    SMALL(TmmResourceBundle.getString("Settings.image.small"), 300, 168, 1);

    private final String text;
    private final int    width;
    private final int    height;
    private final int    order;

    ThumbSizes(String text, int width, int height, int order) {
      this.text = text + ": ~" + width + "x" + height + "px";
      this.width = width;
      this.height = height;
      this.order = order;
    }

    @Override
    public String toString() {
      return text;
    }

    public int getOrder() {
      return order;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }

    public static int getSizeOrder(int width) {
      for (var entry : values()) {
        if (width >= entry.width) {
          return entry.order;
        }
      }
      return 0;
    }
  }

  private final String                providerId;
  private final MediaArtworkType      type;

  private String                      imdbId;
  private int                         tmdbId;
  private int                         season      = -1;
  private String                      previewUrl  = "";
  private String                      originalUrl = "";
  private String                      language    = "";
  private int                         likes       = 0;
  private boolean                     animated    = false;

  private final List<ImageSizeAndUrl> imageSizes  = new ArrayList<>();

  /**
   * Create a new instance of MediaArtwork for the given provider and type
   * 
   * @param providerId
   *          the provider id
   * @param type
   *          the artwork type
   */
  public MediaArtwork(String providerId, MediaArtworkType type) {
    this.providerId = providerId;
    this.type = type;
  }

  /**
   * A copy constructor for MediaArtwork - to clone an existing one for a new type
   * 
   * @param oldArtwork
   *          the instance to copy
   * @param type
   *          the new type
   */
  public MediaArtwork(MediaArtwork oldArtwork, MediaArtworkType type) {
    this.imdbId = oldArtwork.getImdbId();
    this.tmdbId = oldArtwork.getTmdbId();
    this.season = oldArtwork.getSeason();
    this.previewUrl = oldArtwork.getPreviewUrl();
    this.originalUrl = oldArtwork.getOriginalUrl();
    this.language = oldArtwork.getLanguage();
    this.providerId = oldArtwork.getProviderId();
    this.likes = oldArtwork.getLikes();

    for (ImageSizeAndUrl oldImageSizeAndUrl : oldArtwork.getImageSizes()) {
      this.imageSizes.add(new ImageSizeAndUrl(oldImageSizeAndUrl.width, oldImageSizeAndUrl.height, oldImageSizeAndUrl.url));
    }

    this.type = type;
  }

  /**
   * Get a preview url or the default url if no preview url is available
   * 
   * @return the preview url or the default url
   */
  public String getPreviewUrl() {
    // return the default url if the preview url has not been set
    if (StringUtils.isBlank(previewUrl)) {
      return originalUrl;
    }
    return previewUrl;
  }

  /**
   * Set the preview url
   * 
   * @param previewUrl
   *          the preview url
   */
  public void setPreviewUrl(String previewUrl) {
    this.previewUrl = previewUrl;
  }

  /**
   * Get the original url for this artwork
   *
   * @return the original url
   */
  public String getOriginalUrl() {
    return originalUrl;
  }

  /**
   * Set the original url
   *
   * @param originalUrl
   *          the original url
   */
  public void setOriginalUrl(String originalUrl) {
    this.originalUrl = originalUrl;
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
   * Get the artwork type
   * 
   * @return the artwork type
   */
  public MediaArtworkType getType() {
    return type;
  }

  /**
   * Get the language for this artwork if available
   * 
   * @return the language for this artwork
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Set the language for this artwork
   * 
   * @param language
   *          the language
   */
  public void setLanguage(String language) {
    this.language = StrgUtils.getNonNullString(language);
  }

  /**
   * Get the assigned TMDB id for this artwork
   * 
   * @return the tmdb id
   */
  public int getTmdbId() {
    return tmdbId;
  }

  /**
   * Set the assigned TMDB id for this artwork
   * 
   * @param tmdbId
   *          the tmdb id
   */
  public void setTmdbId(int tmdbId) {
    this.tmdbId = tmdbId;
  }

  /**
   * Get the assigned IMDB id for this artwork
   * 
   * @return the imdb id
   */
  public String getImdbId() {
    return imdbId;
  }

  /**
   * Set the assigned IMDB id for this artwork
   * 
   * @param imdbId
   *          the imdb id
   */
  public void setImdbId(String imdbId) {
    this.imdbId = StrgUtils.getNonNullString(imdbId);
  }

  /**
   * Add an image size. This is used for the image chooser to show the user the different sizes for this artwork
   * 
   * @param width
   *          thw width
   * @param height
   *          the height
   * @param url
   *          the url
   * @param sizeOrder
   *          the sizeOrder - set with the right int when known, 0 otherwise
   */
  public void addImageSize(int width, int height, String url, int sizeOrder) {
    ImageSizeAndUrl imageSizeAndUrl = new ImageSizeAndUrl(width, height, url, sizeOrder);
    if (!imageSizes.contains(imageSizeAndUrl)) {
      imageSizes.add(imageSizeAndUrl);
    }
  }

  /**
   * Get all available image sizes for this artwork
   * 
   * @return a list of all available image sizes
   */
  public List<ImageSizeAndUrl> getImageSizes() {
    List<ImageSizeAndUrl> descImageSizes = new ArrayList<>(imageSizes);

    // sort descending
    descImageSizes.sort(Collections.reverseOrder());

    return descImageSizes;
  }

  /**
   * Tries to generate a matching score, how good that artwork matches your preferences
   * 
   * @param wantedSize
   *          the various image sizes 0,1,2,4,8,16
   * @param preferredLanguages
   *          the preferred languages array
   * @return score
   */
  public int getMatchingScoreAccordingPreferences(int wantedSize, List<MediaLanguages> preferredLanguages, boolean preferFanartWoText) {
    // do not use likes here - just check against user preferences!
    int score = 0;

    boolean languageMatched = false;
    boolean sizeMatched = false;

    if (!language.isEmpty()) {
      try {
        MediaLanguages maLang = MediaLanguages.valueOf(language);
        if (preferredLanguages.contains(maLang)) {
          languageMatched = true;
        }
      }
      catch (IllegalArgumentException ignore) {
        // thrown, when we get a language, which is not in our ENUM
      }
    }
    else {
      // artwork language empty - did we prefer this?
      // (either as "none" langu entry or with checkbox)
      if (preferredLanguages.contains(MediaLanguages.none) || (preferFanartWoText && type == MediaArtworkType.BACKGROUND)) {
        languageMatched = true;
      }
    }

    if (wantedSize > 0) {
      for (MediaArtwork.ImageSizeAndUrl imageSizeAndUrl : imageSizes) {
        if (imageSizeAndUrl.getSizeOrder() == wantedSize) {
          // correct size as in settings? worth 10 points
          sizeMatched = true;
        }
      }
    }

    // ffmpeg should get at most a score for size (reduced to sort it below other same sized artwork)
    if ("ffmpeg".equals(providerId)) {
      if (sizeMatched) {
        score += 30;
      }
    }
    else {
      if (languageMatched && sizeMatched) {
        score += 90;
      }
      else if (languageMatched) {
        score += 50;
      }
      else if (sizeMatched) {
        score += 40;
      }
      else {
        // well... not a single preference matched
      }
    }

    if (isAnimated()) {
      score += 5; // why not ^^
    }

    // image chooser finally sorts them by likes,
    // so our short-sort order should be fine
    return score;
  }

  /**
   * Get the smallest artwork if different sizes are available or null
   * 
   * @return the smallest artwork or null
   */
  public ImageSizeAndUrl getSmallestArtwork() {
    if (!imageSizes.isEmpty()) {
      List<ImageSizeAndUrl> ascImageSizes = new ArrayList<>(imageSizes);

      // sort ascending
      Collections.sort(ascImageSizes);
      ImageSizeAndUrl smallestImage = ascImageSizes.get(0);
      if (smallestImage != null) {
        return smallestImage;
      }
    }
    return null;
  }

  /**
   * Get the biggest artwork if different sizes are available or null
   * 
   * @return the biggest artwork or null
   */
  public ImageSizeAndUrl getBiggestArtwork() {
    if (!imageSizes.isEmpty()) {
      List<ImageSizeAndUrl> descImageSizes = new ArrayList<>(imageSizes);

      // sort descending
      descImageSizes.sort(Collections.reverseOrder());
      ImageSizeAndUrl biggestImage = descImageSizes.get(0);
      if (biggestImage != null) {
        return biggestImage;
      }
    }
    return null;
  }

  /**
   * Get the likes of this artwork to indicate how popular this artwork is
   * 
   * @return the likes
   */
  public int getLikes() {
    return likes;
  }

  /**
   * amount of likes (or other, for ordering)
   * 
   * @param likes
   *          set the amount of likes
   */
  public void setLikes(int likes) {
    this.likes = likes;
  }

  /**
   * is this an animated graphic?
   * 
   * @return true if the graphic is animated; false otherwise
   */
  public boolean isAnimated() {
    return animated;
  }

  /**
   * set this graphic as animated
   * 
   * @param animated
   *          the animated state
   */
  public void setAnimated(boolean animated) {
    this.animated = animated;
  }

  /**
   * Get the season (useful for season artwork)
   * 
   * @return the season
   */
  public int getSeason() {
    return season;
  }

  /**
   * Set the season (useful for season artwork)
   * 
   * @param season
   *          the season
   */
  public void setSeason(int season) {
    this.season = season;
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

  /**
   * This class represents a combination of image size and the correspondin url for an artwork
   * 
   * @author Manuel Laggner
   * @since 1.0
   */
  public static class ImageSizeAndUrl implements Comparable<ImageSizeAndUrl> {
    private final int    width;
    private final int    height;
    private final int    sizeOrder;
    private final String url;

    public ImageSizeAndUrl(int width, int height, String url) {
      this(width, height, url, 0);
    }

    public ImageSizeAndUrl(int width, int height, String url, int sizeOrder) {
      this.width = width;
      this.height = height;
      this.sizeOrder = sizeOrder;
      this.url = StrgUtils.getNonNullString(url);
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }

    public int getSizeOrder() {
      return sizeOrder;
    }

    public String getUrl() {
      return url;
    }

    @Override
    public int compareTo(ImageSizeAndUrl obj) {
      if (obj == null) {
        return 1;
      }
      return width * height - obj.width * obj.height;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ImageSizeAndUrl that = (ImageSizeAndUrl) o;
      return width == that.width && height == that.height && url.equals(that.url);
    }

    @Override
    public int hashCode() {
      return Objects.hash(width, height, url);
    }

    @Override
    public String toString() {
      return width + "x" + height;
    }
  }
}
