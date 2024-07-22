package org.tinymediamanager.scraper.subdl.model;

import com.google.gson.annotations.SerializedName;

public class Subtitles {

  @SerializedName("release_name")
  public String  releaseName;

  @SerializedName("name")
  public String  name;

  @SerializedName("lang")
  public String  lang;

  @SerializedName("author")
  public String  author;

  @SerializedName("url")
  public String  url;

  @SerializedName("subtitlePage")
  public String  subtitlePage;

  @SerializedName("season")
  public int     season;

  @SerializedName("episode")
  public int     episode;

  @SerializedName("language")
  public String  language;

  @SerializedName("hi")
  public boolean hi;

  @SerializedName("episode_from")
  public int     episodeFrom;

  @SerializedName("episode_end")
  public int     episodeEnd;

  @SerializedName("full_season")
  public boolean fullSeason;

}
