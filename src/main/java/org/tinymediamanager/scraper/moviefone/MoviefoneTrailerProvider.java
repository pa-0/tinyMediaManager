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
package org.tinymediamanager.scraper.moviefone;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.TrailerSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;

/**
 * A simple trailer provider for the site moviefone.com
 *
 * @author Myron Boyle
 */
public class MoviefoneTrailerProvider {
  private static final Logger LOGGER       = LoggerFactory.getLogger(MoviefoneTrailerProvider.class);
  private static final String BASE_URL     = "https://www.moviefone.com";
  private MediaProviderInfo   providerInfo = null;

  MoviefoneTrailerProvider(MediaProviderInfo info) {
    this.providerInfo = info;
  }

  List<MediaTrailer> getTrailers(TrailerSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getTrailers() - {}", options);

    List<MediaTrailer> trailers = new ArrayList<>();
    String searchType = "movies";
    if (options.getMediaType() == MediaType.TV_SHOW) {
      searchType = "shows";
    }

    MediaMetadata md = options.getMetadata();
    if (md == null) {
      LOGGER.warn("metadata null - no originalTitle served");
      throw new MissingIdException("originalTitle");
    }
    String searchString = md.getOriginalTitle(); // well, we need the English title...
    if (searchString.isEmpty() && options.getLanguage() == MediaLanguages.en) {
      searchString = md.getTitle();
    }
    if (searchString.isEmpty()) {
      LOGGER.warn("no originalTitle served");
      throw new MissingIdException("originalTitle");
    }
    String cleanSearchString = clean(searchString);

    boolean scrapeClips = providerInfo.getConfig().getValueAsBool("clips");

    // treat this like a normal scraper search
    try {
      Url u = new OnDiskCachedUrl(BASE_URL + "/search/?q=" + searchString + "&type=" + searchType);
      Document doc = Jsoup.parse(u.getInputStream(), "UTF-8", "");
      if (doc.getElementsByClass("no-results").first() != null) {
        LOGGER.debug("Search did not find anything.");
      }
      else {
        Elements items = doc.getElementsByClass("search-item");
        LOGGER.debug("Search found {} {}.", items.size(), searchType);
        for (Element item : items) {
          Element h2 = item.getElementsByClass("search-asset-title").first();
          String title = h2.getElementsByTag("a").first().text();
          // should only find ONE match?!
          if (searchString.equalsIgnoreCase(title) || cleanSearchString.equalsIgnoreCase(clean(title))) {
            LOGGER.trace("We have a match!");
            Element videoDetailPage = item.getElementsByClass("search-asset-link-trailer").first();
            if (videoDetailPage != null) {
              LOGGER.trace("parse details page...");
              u = new OnDiskCachedUrl(videoDetailPage.attr("href"));
              doc = Jsoup.parse(u.getInputStream(), "UTF-8", "");
              Element assetDiv = doc.getElementById("asset-trailers");
              Elements videos = assetDiv.getElementsByClass("movie-video"); // yes, also for TV
              for (Element vid : videos) {
                MediaTrailer trailer = new MediaTrailer();
                trailer.setId(providerInfo.getId());
                trailer.setScrapedBy(providerInfo.getId());
                trailer.setProvider(providerInfo.getId());
                title = vid.getElementsByClass("movie-video-title").first().text();
                trailer.setName(title);
                Element dur = vid.getElementsByClass("video-duration").first();
                if (dur != null) {
                  trailer.setQuality(dur.text());
                }
                String url = vid.getElementsByClass("movie-video-link").first().attr("href");
                trailer.setUrl(url);

                if (!scrapeClips && (title.contains("Clip") || title.contains("Bonus") || title.contains("Unscripted"))) {
                  continue;
                }
                trailers.add(trailer);
              }
            }
            else {
              LOGGER.trace("... but the have no trailers for it :(");
            }
          }
        }
      }
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (HttpException e) {
      LOGGER.debug("could not find a trailer on moviefone.com");
      throw new ScrapeException(e);
    }
    catch (Exception e) {
      LOGGER.debug("cannot parse moviefone trailer: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    return trailers;
  }

  private String clean(String text) {
    return text.replaceAll("\\W", ""); // a-z0-9
  }
}
