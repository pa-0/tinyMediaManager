package org.tinymediamanager.scraper.imdb.entities;

import java.util.ArrayList;
import java.util.List;

import org.tinymediamanager.scraper.entities.MediaType;

public class ImdbAdvancedSearchResult extends ImdbAll {

  public String            certificate       = "";               // eg 12 or TV-14
  public List<String>      genres            = new ArrayList<>();
  public String            originalTitleText = "";
  public int               metascore         = 0;
  public String            plot              = "";
  public ImdbImageString   primaryImage      = null;
  public ImdbRatingSummary ratingSummary     = null;
  public int               releaseYear       = 0;
  /** in minutes */
  public int               runtime           = 0;
  public String            titleId           = "";
  public String            titleText         = "";
  public ImdbTitleType     titleType         = null;
  public List<String>      topCast           = new ArrayList<>();
  public String            trailerId         = "";

  /**
   * maps internal groups to our mediaTypes - if it must be parsed as movie or tvshow with episodes
   * 
   * @return MediaType or NULL if we cannot identify it
   */
  public MediaType getMediaType() {
    if (titleType != null)
      switch (titleType.id) {
        case "movie":
        case "tvMovie":
        case "tvSpecial":
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

        case "videoGame":
        default:
          break;
      }
    return null;
  }

  public String getId() {
    return titleId;
  }
}
