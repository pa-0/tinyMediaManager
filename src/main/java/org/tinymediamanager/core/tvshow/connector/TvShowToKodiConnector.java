/*
 * Copyright 2012 - 2025 Manuel Laggner
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.w3c.dom.Element;

/**
 * the class {@link TvShowToKodiConnector} is used to write a most recent Kodi compatible NFO file
 *
 * @author Manuel Laggner
 */
public class TvShowToKodiConnector extends TvShowGenericXmlConnector {
  private static final Logger  LOGGER              = LoggerFactory.getLogger(TvShowToKodiConnector.class);
  private static final Pattern HD_TRAILERS_PATTERN = Pattern
      .compile("https?://.*(apple.com|yahoo-redir|yahoo.com|youtube.com|moviefone.com|ign.com|hd-trailers.net|aol.com).*");

  public TvShowToKodiConnector(TvShow tvShow) {
    super(tvShow);
  }

  /**
   * write the new rating style<br />
   * <ratings> <rating name="default" max="10" default="true"> <value>5.800000</value> <votes>2100</votes> </rating> <rating name="imdb">
   * <value>8.9</value> <votes>12345</votes> </rating> </ratings>
   */
  @Override
  protected void addRating() {
    Element ratings = document.createElement("ratings");

    // get main rating via UI setting
    MediaRating mainMediaRating = tvShow.getRating();
    if (mainMediaRating == MediaMetadata.EMPTY_RATING) {
      // no one found? get a main rating by preferred order
      mainMediaRating = tvShow.getRating(MediaMetadata.TVDB);
      if (mainMediaRating == MediaMetadata.EMPTY_RATING) {
        mainMediaRating = tvShow.getRating(MediaMetadata.TMDB);
      }
      if (mainMediaRating == MediaMetadata.EMPTY_RATING) {
        mainMediaRating = tvShow.getRating(MediaMetadata.IMDB);
      }
    }

    for (MediaRating r : tvShow.getRatings().values()) {
      // skip user ratings here
      if (MediaRating.USER.equals(r.getId())) {
        continue;
      }

      Element rating = document.createElement("rating");
      // Kodi needs themoviedb instead of tmdb
      if (MediaMetadata.TMDB.equals(r.getId())) {
        rating.setAttribute("name", "themoviedb");
      }
      else {
        rating.setAttribute("name", r.getId());
      }
      rating.setAttribute("max", String.valueOf(r.getMaxValue()));

      rating.setAttribute("default", r == mainMediaRating ? "true" : "false");

      Element value = document.createElement("value");
      value.setTextContent(String.format(Locale.US, "%.1f", r.getRating()));
      rating.appendChild(value);

      Element votes = document.createElement("votes");
      votes.setTextContent(Integer.toString(r.getVotes()));
      rating.appendChild(votes);

      ratings.appendChild(rating);
    }

    root.appendChild(ratings);
  }

  /**
   * votes are now in the ratings tag
   */
  @Override
  protected void addVotes() {
  }

  @Override
  protected void addActors() {
    if (TvShowModuleManager.getInstance().getSettings().isNfoWriteAllActors()) {
      // combine base cast and guests
      Map<String, Person> actors = new LinkedHashMap<>();

      for (Person person : tvShow.getActors()) {
        if (StringUtils.isNotBlank(person.getName())) {
          actors.putIfAbsent(person.getName(), person);
        }
      }

      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        for (Person guest : episode.getActors()) {
          if (StringUtils.isNotBlank(guest.getName())) {
            actors.putIfAbsent(guest.getName(), guest);
          }
        }
      }

      for (Person additionalActor : actors.values()) {
        addActor(additionalActor);
      }
    }
    else {
      // write only base cast
      super.addActors();
    }
  }

  @Override
  protected void addOwnTags() {
    addTop250();
  }

  /**
   * add the <top250>xxx</top250> just before the <ratings>xxx</ratings>
   */
  protected void addTop250() {
    Element top250 = document.createElement("top250");
    top250.setTextContent(Integer.toString(tvShow.getTop250()));
    Element set = getSingleElementByTag("ratings");
    root.insertBefore(top250, set);
  }

  @Override
  protected void addTrailer() {
    if (TvShowModuleManager.getInstance().getSettings().isNfoWriteTrailer()) {
      Element trailer = document.createElement("trailer");

      // only add a trailer if there is no physical trailer due to a bug in kodi
      // https://forum.kodi.tv/showthread.php?tid=348759&pid=2900477#pid2900477
      if (tvShow.getMediaFiles(MediaFileType.TRAILER).isEmpty()) {
        for (MediaTrailer mediaTrailer : new ArrayList<>(tvShow.getTrailer())) {
          if (mediaTrailer.getInNfo() && mediaTrailer.getUrl().startsWith("http")) {
            trailer.setTextContent(prepareTrailerForKodi(mediaTrailer));
            break;
          }
        }
      }
      root.appendChild(trailer);
    }
  }

  protected String prepareTrailerForKodi(MediaTrailer trailer) {
    // youtube trailer are stored in a special notation: plugin://plugin.video.youtube/?action=play_video&videoid=<ID>
    // parse out the ID from the url and store it in the right notation
    Matcher matcher = Utils.YOUTUBE_PATTERN.matcher(trailer.getUrl());
    if (matcher.matches()) {
      return "plugin://plugin.video.youtube/play/?video_id=" + matcher.group(5);
    }

    // other urls are handled by the hd-trailers.net plugin
    matcher = HD_TRAILERS_PATTERN.matcher(trailer.getUrl());
    if (matcher.matches()) {
      try {
        return "plugin://plugin.video.hdtrailers_net/video/" + matcher.group(1) + "/" + URLEncoder.encode(trailer.getUrl(), StandardCharsets.UTF_8);
      }
      catch (Exception e) {
        LOGGER.debug("failed to escape {} - {}", trailer.getUrl(), e.getMessage());
      }
    }
    // everything else is stored directly
    return trailer.getUrl();
  }
}
