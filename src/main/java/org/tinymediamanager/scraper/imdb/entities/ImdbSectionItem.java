package org.tinymediamanager.scraper.imdb.entities;

import java.util.ArrayList;
import java.util.List;

public class ImdbSectionItem extends ImdbAll {
  public String             id          = "";
  public String             rowTitle    = "";
  public String             rowLink     = "";
  public List<ImdbTextType> listContent = new ArrayList<>();
}
