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
package org.tinymediamanager.scraper.hdtrailersnet;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.TrailerSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.hdtrailersnet.entities.YahooMediaObject;
import org.tinymediamanager.scraper.hdtrailersnet.entities.YahooStream;
import org.tinymediamanager.scraper.http.InMemoryCachedUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.interfaces.IMovieTrailerProvider;
import org.tinymediamanager.scraper.util.JsonUtils;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.scraper.util.UrlUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Class HDTrailersNet. A trailer provider for the site hd-trailers.net
 *
 * @author Myron Boyle
 */
public class HdTrailersNetMovieTrailerProvider implements IMovieTrailerProvider {
  private static final String     ID        = "hd-trailers";
  private static final Logger     LOGGER    = LoggerFactory.getLogger(HdTrailersNetMovieTrailerProvider.class);
  public static final String      YAHOO_API = "https://video.media.yql.yahoo.com/v1/video/sapi";

  private final MediaProviderInfo providerInfo;
  private ObjectMapper            mapper    = new ObjectMapper();

  public HdTrailersNetMovieTrailerProvider() {
    providerInfo = createMediaProviderInfo();
  }

  private MediaProviderInfo createMediaProviderInfo() {
    return new MediaProviderInfo(ID, "movie_trailer", "hd-trailers.net",
        "<html><h3>hd-trailers.net</h3>Scraper for hd-trailers.net which is able to scrape trailers</html>",
        HdTrailersNetMovieTrailerProvider.class.getResource("/org/tinymediamanager/scraper/hd-trailers_net.png"));
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  @Override
  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(null);
  }

  @Override
  public List<MediaTrailer> getTrailers(TrailerSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getTrailers() - {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    List<MediaTrailer> trailers = new ArrayList<>();
    MediaMetadata md = options.getMetadata();

    if (md == null) {
      LOGGER.warn("no originalTitle served");
      throw new MissingIdException("originalTitle");
    }

    String ot = md.getOriginalTitle();
    if (ot.isEmpty() && options.getLanguage() == MediaLanguages.en) {
      ot = md.getTitle();
    }
    if (ot.isEmpty()) {
      LOGGER.warn("no originalTitle served");
      throw new MissingIdException("originalTitle");
    }

    try {
      // best guess
      String search = getApiKey() + ot.replaceAll("[^a-zA-Z0-9]", "-").replace("--", "-").toLowerCase(Locale.ROOT) + "/";
      LOGGER.debug("Guessed HD-Trailers Url: {}", search);

      Document doc = UrlUtil.parseDocumentFromUrl(search);
      Elements tr = doc.getElementsByAttributeValue("itemprop", "trailer");
      // loop over every row, to get date & title
      for (Element t : tr) {
        try {
          String date = t.select("td.bottomTableDate").first().text();
          String title = t.select("td.bottomTableName > span").first().text();

          Elements links = t.getElementsByClass("bottomTableResolution");
          for (Element link : links) {
            if (link.html().isEmpty()) {
              continue;
            }

            MediaTrailer trailer = new MediaTrailer();
            trailer.setName(title + " (" + date + ")");
            trailer.setDate(date);
            trailer.setQuality(link.text()); // eg 480p

            String url = link.selectFirst("a").attr("href");
            if (url.contains("yahoo-redir")) {
              String id = StrgUtils.substr(url, "id=(.*?)&");
              url = parseYahooUrl(id, link.text());
            }
            trailer.setUrl(url);

            trailer.setProvider(getProviderFromUrl(url));
            // all with XXX are DEAD
            if ("XXX".equalsIgnoreCase(trailer.getProvider())) {
              continue;
            }

            trailer.setScrapedBy(providerInfo.getId());
            if (!trailer.getUrl().isEmpty() && !trailer.getName().isEmpty()) {
              LOGGER.trace("found trailer: {}", trailer);
              trailers.add(trailer);
            }
          }
        }
        catch (Exception e) {
          // ignore parse errors per line
          LOGGER.debug("Error parsing HD-Trailers line. {}", e.getMessage());
        }
      }
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (HttpException e) {
      LOGGER.debug("could not find a trailer on hd-trailers.net");
    }
    catch (Exception e) {
      LOGGER.debug("cannot parse HD-Trailers movie: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    return trailers;
  }

  /**
   * Returns the "Source" for this trailer by parsing the URL.
   *
   * @param url
   *          the url
   * @return the provider from url
   */
  private static String getProviderFromUrl(String url) {
    url = url.toLowerCase(Locale.ROOT);
    String source = "unknown";
    if (url.contains("youtube.com") || url.contains("youtu.be")) {
      source = "youtube";
    }
    else if (url.contains("apple.com")) {
      source = "XXX";// "apple";
    }
    else if (url.contains("aol.com")) {
      source = "XXX";// "aol"; // pdl.stream.aol.com; former moviefone
    }
    else if (url.contains("yahoo.com") || url.contains("yahoo.net")) {
      source = "XXX";// "yahoo";
    }
    else if (url.contains("hd-trailers.net")) {
      source = "hdtrailers";
    }
    else if (url.contains("moviefone.com")) {
      source = "XXX";// "moviefone";
    }
    else if (url.contains("mtv.com")) {
      source = "mtv";
    }
    else if (url.contains("ign.com")) {
      source = "ign";
    }
    else if (url.contains("ordienetworks.com")) {
      source = "XXX";// "ordienetworks";
    }
    else if (url.contains("5min.com")) {
      source = "XXX";// "5min"; // moviefone old
    }
    return source;
  }

  // https://www.hd-trailers.net/yahoo-redir.php?id=86fa2bcd-be32-3443-acbc-e3eab50d908c&resolution=480
  // see https://static.hd-trailers.net/yahoo-redir.js
  // https://video.media.yql.yahoo.com/v1/video/sapi/streams/86fa2bcd-be32-3443-acbc-e3eab50d908c?cprotocol=http&format=mp4
  private String parseYahooUrl(String id, String quality) {
    final String q = quality.replace("p", "").replace("480", "540"); // 480p -> 480 -> 540

    try {
      String url = YAHOO_API + "/streams/" + id + "?cprotocol=http&format=mp4";
      Url u = new InMemoryCachedUrl(url);
      JsonNode node = mapper.readTree(u.getInputStream());
      JsonNode mediaObj = node.at("/query/results/mediaObj");
      YahooMediaObject media = JsonUtils.parseObject(mapper, mediaObj.get(0), YahooMediaObject.class);
      YahooStream strm = media.streams.stream().filter(stream -> q.equals(Integer.toString(stream.height))).findAny().orElse(null);
      if (strm != null) {
        return strm.host + strm.path;
      }
    }
    catch (Exception e) {
      LOGGER.error("could not fetch trailer url: {}", e.getMessage());
    }

    return "";

  }
}
