package org.tinymediamanager.scraper.imdb.entities;

import org.tinymediamanager.core.entities.MediaGenres;

public class ImdbGenre extends ImdbAll {
  public String id;
  public String text;

  public MediaGenres toTmm() {
    return MediaGenres.getGenre(id);
  }
}
