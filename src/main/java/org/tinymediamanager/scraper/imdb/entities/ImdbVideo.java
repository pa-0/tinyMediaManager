package org.tinymediamanager.scraper.imdb.entities;

import java.util.ArrayList;
import java.util.List;

public class ImdbVideo extends ImdbAll {
  public String                id           = "";
  public boolean               isMature     = false;
  public String                createdDate  = "";
  public ImdbImage             thumbnail    = null;
  public ImdbLocalizedString   description  = null;
  public ImdbLocalizedString   name         = null;
  public List<ImdbPlaybackUrl> playbackURLs = new ArrayList<>();
}
