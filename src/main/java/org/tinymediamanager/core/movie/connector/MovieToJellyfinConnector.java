/*
 * Copyright 2012 - 2024 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.core.movie.connector;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.entities.Movie;
import org.w3c.dom.Element;

/**
 * the class {@link MovieToJellyfinConnector} is used to write a most recent Jellyfin compatible NFO file
 *
 * @author Manuel Laggner
 */
public class MovieToJellyfinConnector extends MovieToKodiConnector {

  public MovieToJellyfinConnector(Movie movie) {
    super(movie);
  }

  @Override
  protected void addThumb() {
    // do not write any artwork urls
  }

  @Override
  protected void addThumb(MediaFileType type, String aspect) {
    // do not write any artwork urls
  }

  @Override
  protected void addFanart() {
    // do not write any artwork urls
  }

  /**
   * add actors in <actor><name>xxx</name><role>xxx</role></actor> --> without thumb
   */
  @Override
  protected void addActors() {
    for (Person movieActor : movie.getActors()) {
      Element actor = document.createElement("actor");

      Element name = document.createElement("name");
      name.setTextContent(movieActor.getName());
      actor.appendChild(name);

      if (StringUtils.isNotBlank(movieActor.getRole())) {
        Element role = document.createElement("role");
        role.setTextContent(movieActor.getRole());
        actor.appendChild(role);
      }

      if (StringUtils.isNotBlank(movieActor.getProfileUrl())) {
        Element profile = document.createElement("profile");
        profile.setTextContent(movieActor.getProfileUrl());
        actor.appendChild(profile);
      }

      addPersonIdsAsChildren(actor, movieActor);

      root.appendChild(actor);
    }
  }

  /**
   * add producers in <producer><name>xxx</name><role>xxx</role></producer> --> without thumb
   */
  protected void addProducers() {
    for (Person movieProducer : movie.getProducers()) {
      Element producer = document.createElement("producer");

      Element name = document.createElement("name");
      name.setTextContent(movieProducer.getName());
      producer.appendChild(name);

      if (StringUtils.isNotBlank(movieProducer.getRole())) {
        Element role = document.createElement("role");
        role.setTextContent(movieProducer.getRole());
        producer.appendChild(role);
      }

      if (StringUtils.isNotBlank(movieProducer.getProfileUrl())) {
        Element profile = document.createElement("profile");
        profile.setTextContent(movieProducer.getProfileUrl());
        producer.appendChild(profile);
      }

      // add ids
      addPersonIdsAsAttributes(producer, movieProducer);

      root.appendChild(producer);
    }
  }
}
