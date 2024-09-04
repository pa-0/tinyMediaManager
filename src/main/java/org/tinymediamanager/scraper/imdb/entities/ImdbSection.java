package org.tinymediamanager.scraper.imdb.entities;

import java.util.ArrayList;
import java.util.List;

public class ImdbSection extends ImdbAll {
  public int                   total     = 0;
  public String                endCursor = "";
  public String                rowLink   = "";
  public List<ImdbSectionItem> items     = new ArrayList<>();
}
