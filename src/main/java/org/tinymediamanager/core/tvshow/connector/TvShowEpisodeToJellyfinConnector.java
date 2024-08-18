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
package org.tinymediamanager.core.tvshow.connector;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.w3c.dom.Element;

/**
 * the class {@link TvShowEpisodeToJellyfinConnector} is used to write a most recent Jellyfin compatible NFO file
 *
 * @author Manuel Laggner
 */
public class TvShowEpisodeToJellyfinConnector extends TvShowEpisodeToKodiConnector {

  public TvShowEpisodeToJellyfinConnector(List<TvShowEpisode> episodes) {
    super(episodes);
  }

  @Override
  protected void addThumb(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    // do not write any artwork urls
  }

  /**
   * add actors (guests) in <actor><name>xxx</name><role>xxx</role></actor> --> without thumb
   */
  @Override
  protected void addActors(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    for (Person tvShowActor : episode.getActors()) {
      Element actor = document.createElement("actor");

      Element name = document.createElement("name");
      name.setTextContent(tvShowActor.getName());
      actor.appendChild(name);

      if (StringUtils.isNotBlank(tvShowActor.getRole())) {
        Element role = document.createElement("role");
        role.setTextContent(tvShowActor.getRole());
        actor.appendChild(role);
      }

      if (StringUtils.isNotBlank(tvShowActor.getProfileUrl())) {
        Element profile = document.createElement("profile");
        profile.setTextContent(tvShowActor.getProfileUrl());
        actor.appendChild(profile);
      }

      // save GuestStar information to NFO
      // https://emby.media/community/index.php?/topic/89268-actor-metadata-is-downloaded-only-for-the-people-that-tmdb-has-as-series-regulars/&do=findComment&comment=923528
      if (tvShowActor.getType() == Person.Type.GUEST) {
        Element profile = document.createElement("type");
        profile.setTextContent("GuestStar");
        actor.appendChild(profile);
      }

      addPersonIdsAsChildren(actor, tvShowActor);

      root.appendChild(actor);
    }
  }
}
