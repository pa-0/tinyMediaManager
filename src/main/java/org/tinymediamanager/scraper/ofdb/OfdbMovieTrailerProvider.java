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
package org.tinymediamanager.scraper.ofdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.TrailerSearchAndScrapeOptions;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieTrailerProvider;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.scraper.util.UrlUtil;

/**
 * the class {@link OfdbMovieTrailerProvider} is used to provide trailer for movies
 *
 * @author Manuel Laggner
 */
public class OfdbMovieTrailerProvider extends OfdbMetadataProvider implements IMovieTrailerProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OfdbMovieTrailerProvider.class);

  @Override
  protected String getSubId() {
    return "movie_trailer";
  }

  @Override
  public List<MediaTrailer> getTrailers(TrailerSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getTrailers(): {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    List<MediaTrailer> trailers = new ArrayList<>();
    String ofdbId = options.getIdAsString(getId());
    String imdbId = options.getImdbId();
    String detailUrl = "";

    // get from former search
    if (options.getSearchResult() != null) {
      detailUrl = options.getSearchResult().getUrl();
    }

    // get via OFDB ID
    if (detailUrl.isEmpty() && StringUtils.isNotBlank(ofdbId)) {
      try {
        detailUrl = getApiKey() + "/view.php?page=film&fid=" + ofdbId;
      }
      catch (Exception e) {
        throw new ScrapeException(e);
      }
    }

    // search via IMDB ID
    if (detailUrl.isEmpty() && MediaIdUtil.isValidImdbId(imdbId)) {
      try {
        Document doc = UrlUtil.parseDocumentFromUrl(getApiKey() + "/view.php?page=suchergebnis&Kat=IMDb&SText=" + options.getImdbId());

        Elements filme = doc.getElementsByAttributeValueMatching("href", "film\\/\\d+,");
        if (filme == null || filme.isEmpty()) {
          LOGGER.debug("Did not find movie via ImdbId");
          return Collections.emptyList();
        }
        detailUrl = getApiKey() + "/" + StrgUtils.substr(filme.first().toString(), "href=\\\"(.*?)\\\"");
      }
      catch (Exception e) {
        LOGGER.warn("failed IMDB search: {}", e.getMessage());
      }
    }

    if (detailUrl.isEmpty()) {
      LOGGER.debug("Neither OFDB, nor IMDB id not found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    try {
      Document doc = UrlUtil.parseDocumentFromUrl(detailUrl);

      // OLD STYLE
      // <b>Trailer 1</b><br><i>(xxlarge)</i><br><br>&raquo; 640px<br><br>Download:<br>&raquo; <a href=
      // "http://de.clip-1.filmtrailer.com/9507_31566_a_5.wmv?log_var=72|491100001-1|-" >wmv</a><br>&raquo; <a href=
      // "http://de.clip-1.filmtrailer.com/9507_31566_a_5.mp4?log_var=72|491100001-1|-" >mp4</a><br>&raquo; <a href=
      // "http://de.clip-1.filmtrailer.com/9507_31566_a_5.webm?log_var=72|491100001-1|-" >webm</a><br>
      Pattern regex = Pattern.compile("return '(.*?)';");
      Matcher m = regex.matcher(doc.toString());
      while (m.find()) {
        String s = m.group(1);
        String tname = StrgUtils.substr(s, "<b>(.*?)</b>");
        String tpix = StrgUtils.substr(s, "raquo; (.*?)x<br>");

        // url + format
        Pattern lr = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");
        Matcher lm = lr.matcher(s);
        while (lm.find()) {
          String turl = lm.group(1);
          // String tformat = lm.group(2);
          MediaTrailer trailer = new MediaTrailer();
          trailer.setName(tname);
          // trailer.setQuality(tpix + " (" + tformat + ")");
          trailer.setQuality(tpix);
          trailer.setProvider("filmtrailer");
          trailer.setScrapedBy(getProviderInfo().getId());
          trailer.setUrl(turl);
          LOGGER.debug(trailer.toString());
          trailers.add(trailer);
        }
      }

      // NEW STYLE (additional!)
      // <div class="clips" id="clips2" style="display: none;">
      // <img src="images/flag_de.gif" align="left" vspace="3" width="18" height="12">&nbsp;
      // <img src="images/trailer_6.gif" align="top" vspace="1" width="16" height="16" alt="freigegeben ab 6 Jahren">&nbsp;
      // <i>Trailer 1:</i>
      // <a href="http://de.clip-1.filmtrailer.com/2845_6584_a_1.flv?log_var=67|491100001-1|-">&nbsp;small&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_6584_a_2.flv?log_var=67|491100001-1|-">&nbsp;medium&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_6584_a_3.flv?log_var=67|491100001-1|-">&nbsp;large&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_6584_a_4.flv?log_var=67|491100001-1|-">&nbsp;xlarge&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_6584_a_5.flv?log_var=67|491100001-1|-">&nbsp;xxlarge&nbsp;</a> &nbsp;
      // <br>
      // <img src="images/flag_de.gif" align="left" vspace="3" width="18" height="12">&nbsp;
      // <img src="images/trailer_6.gif" align="top" vspace="1" width="16" height="16" alt="freigegeben ab 6 Jahren">&nbsp;
      // <i>Trailer 2:</i>
      // <a href="http://de.clip-1.filmtrailer.com/2845_8244_a_1.flv?log_var=67|491100001-1|-">&nbsp;small&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_8244_a_2.flv?log_var=67|491100001-1|-">&nbsp;medium&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_8244_a_3.flv?log_var=67|491100001-1|-">&nbsp;large&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_8244_a_4.flv?log_var=67|491100001-1|-">&nbsp;xlarge&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_8244_a_5.flv?log_var=67|491100001-1|-">&nbsp;xxlarge&nbsp;</a> &nbsp;
      // <br>
      // <img src="images/flag_de.gif" align="left" vspace="3" width="18" height="12">&nbsp;
      // <img src="images/trailer_6.gif" align="top" vspace="1" width="16" height="16" alt="freigegeben ab 6 Jahren">&nbsp;
      // <i>Trailer 3:</i>
      // <a href="http://de.clip-1.filmtrailer.com/2845_14749_a_1.flv?log_var=67|491100001-1|-">&nbsp;small&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_14749_a_2.flv?log_var=67|491100001-1|-">&nbsp;medium&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_14749_a_3.flv?log_var=67|491100001-1|-">&nbsp;large&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_14749_a_4.flv?log_var=67|491100001-1|-">&nbsp;xlarge&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_14749_a_5.flv?log_var=67|491100001-1|-">&nbsp;xxlarge&nbsp;</a> &nbsp;
      // <br>
      // <br>
      // </div>

      // new style size
      // 1 = 160 x 90 = small
      // 2 = 240 x 136 = medium
      // 3 = 320 x 180 = large
      // 4 = 400 x 226 = xlarge
      // 5 = 640 x 360 = xxlarge

      regex = Pattern.compile("<i>(.*?)</i>(.*?)<br>", Pattern.DOTALL); // get them as single trailer line
      m = regex.matcher(doc.getElementsByClass("clips").html());
      while (m.find()) {
        // LOGGER.info(doc.getElementsByClass("clips").html());
        // parse each line with 5 qualities
        String tname = m.group(1).strip();
        tname = tname.replaceFirst(":$", ""); // replace ending colon

        String urls = m.group(2);
        // url + format
        Pattern lr = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");
        Matcher lm = lr.matcher(urls);
        while (lm.find()) {
          String turl = lm.group(1);
          String tpix = "";
          String tformat = lm.group(2).replaceAll("&nbsp;", "").strip();
          switch (tformat) {
            case "small":
              tpix = "90p";
              break;

            case "medium":
              tpix = "136p";
              break;

            case "large":
              tpix = "180p";
              break;

            case "xlarge":
              tpix = "226p";
              break;

            case "xxlarge":
              tpix = "360p";
              break;

            default:
              break;
          }
          MediaTrailer trailer = new MediaTrailer();
          trailer.setName(tname);
          // trailer.setQuality(tpix + " (" + tformat + ")");
          trailer.setQuality(tpix);
          trailer.setProvider("filmtrailer");
          trailer.setUrl(turl);
          trailer.setScrapedBy(getProviderInfo().getId());
          LOGGER.debug(trailer.toString());
          trailers.add(trailer);
        }
      }

      // try to find YT trailers (and skip the reviews and other YT links)
      // <a href="https://www.youtube.com/watch?v=CFO0nojq-4A">Offizieller Trailer deutsch</a>
      // <a href="https://youtu.be/xAO-G1haGDc" target="_blank">YouTube: Deutscher Trailer</a>
      Elements yt = doc.getElementsByAttributeValueContaining("href", "youtu");
      for (Element el : yt) {
        if (el.text().contains("Offizieller Trailer") || el.text().contains("Deutscher Trailer")) {
          MediaTrailer trailer = new MediaTrailer();
          trailer.setName(el.text());
          trailer.setProvider("youtube");
          trailer.setUrl(el.attr("href"));
          trailer.setScrapedBy(getId());
          LOGGER.debug(trailer.toString());
          trailers.add(trailer);
        }
      }

    }
    catch (Exception e) {
      throw new ScrapeException(e);
    }
    return trailers;
  }
}
