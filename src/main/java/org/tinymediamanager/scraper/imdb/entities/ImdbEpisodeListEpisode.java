package org.tinymediamanager.scraper.imdb.entities;

public class ImdbEpisodeListEpisode extends ImdbAll {

  public String          id              = "";
  public String          episode         = "";
  public String          season          = "";
  public String          titleText       = "";
  public String          plot            = "";
  public double          aggregateRating = 0.0;
  public int             voteCount       = 0;
  public ImdbReleaseDate releaseDate     = null;
  public int             releaseYear     = 0;
  public ImdbImageString image           = null;
}
