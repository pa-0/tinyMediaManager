package org.tinymediamanager.scraper.tvmaze.entities;

public class AlternateList {
  public int        id;
  public String     url;
  public boolean    dvd_release;
  public boolean    verbatim_order;
  public boolean    country_premiere;
  public boolean    streaming_premiere;
  public boolean    broadcast_premiere;
  public boolean    language_premiere;
  public String     language;
  public Network    network;
  public WebChannel webChannel;

  /**
   * get the country code either from network or webChannel
   * 
   * @return country name or empty string
   */
  public String getCountry() {
    String ret = "";
    if (network != null && network.country != null) {
      ret = network.country.code;
    }
    else if (webChannel != null && webChannel.country != null) {
      ret = webChannel.country.code;
    }
    return ret;
  }

  /**
   * get the name either from network or webChannel
   * 
   * @return station name or empty string
   */
  public String getName() {
    String ret = "";
    if (network != null) {
      ret = network.name;
    }
    else if (webChannel != null) {
      ret = webChannel.name;
    }
    return ret;
  }
}
