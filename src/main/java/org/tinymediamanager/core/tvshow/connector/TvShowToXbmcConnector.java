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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.w3c.dom.Element;

/**
 * the class TvShowToXbmcConnector is used to write a legacy XBMC compatible NFO file
 *
 * @author Manuel Laggner
 */
public class TvShowToXbmcConnector extends TvShowGenericXmlConnector {
  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowToXbmcConnector.class);

  public TvShowToXbmcConnector(TvShow tvShow) {
    super(tvShow);
  }

  @Override
  protected void addOwnTags() {
    addTop250();
  }

  /**
   * add the <top250>xxx</top250> just before the <ratings>xxx</ratings>
   */
  private void addTop250() {
    Element top250 = document.createElement("top250");
    top250.setTextContent(Integer.toString(tvShow.getTop250()));
    Element set = getSingleElementByTag("ratings");
    if (set != null) {
      root.insertBefore(top250, set);
    }
  }

  @Override
  protected void addTrailer() {
    if (TvShowModuleManager.getInstance().getSettings().isNfoWriteTrailer()) {
      Element trailer = document.createElement("trailer");
      for (MediaTrailer mediaTrailer : new ArrayList<>(tvShow.getTrailer())) {
        if (mediaTrailer.getInNfo() && mediaTrailer.getUrl().startsWith("http")) {
          trailer.setTextContent(prepareTrailerForXbmc(mediaTrailer));
          break;
        }
      }
      root.appendChild(trailer);
    }
  }

  private String prepareTrailerForXbmc(MediaTrailer trailer) {
    // YouTube trailers are stored in a special notation: plugin://plugin.video.youtube/?action=play_video&videoid=<ID>
    // parse out the ID from the url and store it in the right notation
    Pattern pattern = Pattern.compile("https{0,1}://.*youtube..*/watch\\?v=(.*)$");
    Matcher matcher = pattern.matcher(trailer.getUrl());
    if (matcher.matches()) {
      return "plugin://plugin.video.youtube/?action=play_video&videoid=" + matcher.group(1);
    }

    // other urls are handled by the hd-trailers.net plugin
    pattern = Pattern.compile("https{0,1}://.*(apple.com|yahoo-redir|yahoo.com|youtube.com|moviefone.com|ign.com|hd-trailers.net|aol.com).*");
    matcher = pattern.matcher(trailer.getUrl());
    if (matcher.matches()) {
      try {
        return "plugin://plugin.video.hdtrailers_net/video/" + matcher.group(1) + "/" + URLEncoder.encode(trailer.getUrl(), "UTF-8");
      }
      catch (Exception e) {
        LOGGER.error("failed to escape '{}'", trailer.getUrl());
      }
    }
    // everything else is stored directly
    return trailer.getUrl();
  }
}
