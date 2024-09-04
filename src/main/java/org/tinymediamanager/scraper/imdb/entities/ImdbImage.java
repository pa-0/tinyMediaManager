package org.tinymediamanager.scraper.imdb.entities;

public class ImdbImage extends ImdbAll {

  public String      id        = "";
  public String      url       = "";
  public Integer     height    = 0;
  public Integer     width     = 0;
  public Integer     maxHeight = 0;
  public Integer     maxWidth  = 0;
  public ImdbCaption caption   = null;

  public int getHeight() {
    return height == 0 ? maxHeight : height;
  }

  public int getWidth() {
    return width == 0 ? maxWidth : width;
  }
}