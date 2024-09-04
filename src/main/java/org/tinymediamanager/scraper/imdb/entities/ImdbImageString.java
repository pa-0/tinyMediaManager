package org.tinymediamanager.scraper.imdb.entities;

public class ImdbImageString extends ImdbAll {

  public String  id        = "";
  public String  url       = "";
  public Integer height    = 0;
  public Integer width     = 0;
  public Integer maxHeight = 0;
  public Integer maxWidth  = 0;
  public String  caption   = "";

  public int getHeight() {
    return height == 0 ? maxHeight : height;
  }

  public int getWidth() {
    return width == 0 ? maxWidth : width;
  }
}
