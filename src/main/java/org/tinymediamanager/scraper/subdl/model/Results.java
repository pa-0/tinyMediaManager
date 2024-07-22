package org.tinymediamanager.scraper.subdl.model;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

public class Results {

  @SerializedName("sd_id")
  public int    subdlId;

  @SerializedName("type")
  public String type;

  @SerializedName("name")
  public String name;

  @SerializedName("imdb_id")
  public String imdbId;

  @SerializedName("tmdb_id")
  public int    tmdbId;

  @SerializedName("first_air_date")
  public Date   firstAirDate;

  @SerializedName("slug")
  public String slug;

  @SerializedName("release_date")
  public Date   releaseDate;

  @SerializedName("year")
  public int    year;

}
