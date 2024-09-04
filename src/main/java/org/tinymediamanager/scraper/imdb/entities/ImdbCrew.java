package org.tinymediamanager.scraper.imdb.entities;

import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.scraper.MediaMetadata;

public class ImdbCrew extends ImdbAll {

  public ImdbName name = null;

  public Person toTmm(Person.Type type) {
    if (name == null || name.nameText == null || name.nameText.text.isEmpty()) {
      return null;
    }

    Person p = new Person(type);
    p.setId(MediaMetadata.IMDB, name.id);
    p.setName(name.nameText.text);

    if (name.primaryImage != null) {
      p.setThumbUrl(name.primaryImage.url);
    }

    p.setProfileUrl("https://www.imdb.com/name/" + name.id);
    return p;
  }
}
