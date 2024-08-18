package org.tinymediamanager.scraper.imdb.entities;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ImdbImageString {

  public String               id                   = "";
  public String               url                  = "";
  public Integer              height               = 0;
  public Integer              width                = 0;
  public Integer              maxHeight            = 0;
  public Integer              maxWidth             = 0;
  public String               caption              = "";

  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public int getHeight() {
    return height == 0 ? maxHeight : height;
  }

  public int getWidth() {
    return width == 0 ? maxWidth : width;
  }

  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
