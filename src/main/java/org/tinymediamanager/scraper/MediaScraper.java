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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.interfaces.IKodiMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieSetMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieSubtitleProvider;
import org.tinymediamanager.scraper.interfaces.IMovieTrailerProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowSubtitleProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowTrailerProvider;

/**
 * Class representing a MediaScraper; (type, info, description...)<br>
 * replacement of MovieScrapers /TvShowScrapers ENUM
 *
 * @author Manuel Laggner
 */
public class MediaScraper {
  private final IMediaProvider mediaProvider;

  private final URL            logoUrl;

  private String               id;
  private String               version;
  private String               name;
  private String               summary;
  private String               description;
  private int                  priority = 0;
  private ScraperType          type;

  public MediaScraper(ScraperType type, IMediaProvider mediaProvider) {
    this.mediaProvider = mediaProvider;
    this.type = type;
    MediaProviderInfo mpi = mediaProvider.getProviderInfo();
    this.id = mpi.getId();
    this.name = mpi.getName();
    this.version = mpi.getVersion();
    this.description = mpi.getDescription();
    this.summary = mpi.getDescription();
    this.logoUrl = mpi.getProviderLogo();
    this.priority = mpi.getPriority();
  }

  @Override
  public String toString() {
    return name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public ScraperType getType() {
    return type;
  }

  public void setType(ScraperType type) {
    this.type = type;
  }

  public boolean isActive() {
    if (mediaProvider == null) {
      return false;
    }
    return mediaProvider.isActive();
  }

  public boolean isEnabled() {
    if (mediaProvider == null) {
      return false;
    }
    return mediaProvider.isFeatureEnabled();
  }

  public IMediaProvider getMediaProvider() {
    return this.mediaProvider;
  }

  public URL getLogoURL() {
    return this.logoUrl;
  }

  /**
   * returns a MediaScraper from a given type - found via plugins<br>
   * use .toArray() for putting this in a ComboBox
   * 
   * @param type
   *          Movie or Tv
   * @return a list of all found media scrapers
   */
  public static List<MediaScraper> getMediaScrapers(ScraperType type) {
    ArrayList<MediaScraper> scraper = new ArrayList<>();

    ArrayList<IMediaProvider> plugins = new ArrayList<>();

    Class<? extends IMediaProvider> clazz = getClassForType(type);
    if (clazz != null) {
      plugins.addAll(MediaProviders.getProvidersForInterface(clazz));
    }

    for (IMediaProvider p : plugins) {
      scraper.add(new MediaScraper(type, p));
    }

    // Kodi scrapers
    for (IKodiMetadataProvider kodi : MediaProviders.getProvidersForInterface(IKodiMetadataProvider.class)) {
      try {
        for (IMediaProvider p : kodi.getPluginsForType(MediaType.toMediaType(type.name()))) {
          scraper.add(new MediaScraper(type, p));
        }
      }
      catch (Exception ignored) {
        // ignored
      }
    }

    return scraper;
  }

  public static MediaScraper getMediaScraperById(String id, ScraperType type) {
    if (StringUtils.isBlank(id)) {
      return null;
    }

    IMediaProvider mediaProvider = MediaProviders.getProviderById(id, getClassForType(type));
    if (mediaProvider != null) {
      return new MediaScraper(type, mediaProvider);
    }

    return null;
  }

  private static Class<? extends IMediaProvider> getClassForType(ScraperType type) {
    switch (type) {
      case MOVIE:
        return IMovieMetadataProvider.class;

      case TV_SHOW:
        return ITvShowMetadataProvider.class;

      case MOVIE_SET:
        return IMovieSetMetadataProvider.class;

      case MOVIE_ARTWORK:
        return IMovieArtworkProvider.class;

      case TVSHOW_ARTWORK:
        return ITvShowArtworkProvider.class;

      case MOVIE_TRAILER:
        return IMovieTrailerProvider.class;

      case TVSHOW_TRAILER:
        return ITvShowTrailerProvider.class;

      case MOVIE_SUBTITLE:
        return IMovieSubtitleProvider.class;

      case TVSHOW_SUBTITLE:
        return ITvShowSubtitleProvider.class;

      default:
        return null;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MediaScraper other = (MediaScraper) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    }
    else if (!id.equals(other.id))
      return false;
    if (type != other.type)
      return false;
    return true;
  }
}
