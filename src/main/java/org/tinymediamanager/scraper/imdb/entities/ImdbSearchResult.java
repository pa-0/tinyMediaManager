package org.tinymediamanager.scraper.imdb.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.tinymediamanager.scraper.entities.MediaType;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ImdbSearchResult {

  public String              id                    = "";
  public String              titleNameText         = "";
  public String              titleReleaseText      = "";
  public String              titleTypeText         = "";
  public ImdbImageString     titlePosterImageModel = null;
  public List<String>        topCredits            = new ArrayList<>();
  public String              imageType             = "";
  public String              seriesId              = "";
  public String              seriesNameText        = "";
  public String              seriesReleaseText     = "";
  public String              seriesTypeText        = "";
  public String              seriesSeasonText      = "";
  public String              seriesEpisodeText     = "";
  @JsonIgnore
  public Map<String, Object> additionalProperties  = new HashMap<>();

  /**
   * maps internal groups to our mediaTypes - if it must be parsed as movie or tvshow with episodes
   * 
   * @return MediaType or NULL if we cannot identify it
   */
  public MediaType getMediaType() {
    switch (imageType) {
      case "movie":
      case "tvMovie":
      case "tvSpecial":
      case "documentary":
      case "short":
      case "tvShort":
      case "musicVideo":
      case "video":
        return MediaType.MOVIE;

      case "tvSeries":
      case "tvMiniSeries":
      case "podcastSeries":
        return MediaType.TV_SHOW;

      case "tvEpisode":
      case "podcastEpisode":
        return MediaType.TV_EPISODE;

      default:
        break;
    }
    return null;
  }

  public String getId() {
    return id;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
