package org.tinymediamanager.scraper.imdb.entities;

import java.util.ArrayList;
import java.util.List;

public class ImdbCredits extends ImdbAll {

  public ImdbIdTextType category = null;
  public List<ImdbCrew> credits  = new ArrayList<>();
}
