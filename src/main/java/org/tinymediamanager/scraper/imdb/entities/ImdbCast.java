package org.tinymediamanager.scraper.imdb.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.scraper.MediaMetadata;

public class ImdbCast extends ImdbAll {

  public ImdbName            name       = null;
  public List<ImdbCharacter> characters = new ArrayList<>();

  public Person toTmm(Person.Type type) {
    if (name == null || name.nameText == null || name.nameText.text.isEmpty()) {
      return null;
    }

    Person p = new Person(type);
    p.setId(MediaMetadata.IMDB, name.id);
    p.setName(name.nameText.text);
    if (characters != null) {
      String chars = characters.stream().map(character -> character.name).collect(Collectors.joining(" / "));
      p.setRole(chars);
    }

    if (name.primaryImage != null) {
      p.setThumbUrl(name.primaryImage.url);
    }

    p.setProfileUrl("https://www.imdb.com/name/" + name.id);
    return p;
  }
}
