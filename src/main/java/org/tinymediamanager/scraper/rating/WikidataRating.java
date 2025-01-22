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
package org.tinymediamanager.scraper.rating;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.UrlUtil;

/**
 * the class {@link WikidataRating} is used to get metadata from Wikidata
 * 
 * @author Manuel Laggner
 */
class WikidataRating {
  private static final Logger LOGGER = LoggerFactory.getLogger(WikidataRating.class);

  List<MediaRating> getRatings(String imdbId) {
    LOGGER.debug("getRatings(): {}", imdbId);

    StringBuilder sb = new StringBuilder();
    sb.append("SELECT ?item ?IMDb_ID ?_review_score ?_review_count ?_score_byLabel ?_score_methodLabel WHERE {");
    sb.append("?item wdt:P345 \"");
    sb.append(imdbId);
    sb.append("\".");
    sb.append("SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\". }");
    sb.append("OPTIONAL { ?item wdt:P345 ?IMDb_ID. }");
    sb.append("OPTIONAL { ?item p:P444 ?review . ?review pq:P447 ?_score_by ; ps:P444 ?_review_score }");
    sb.append("OPTIONAL { ?item p:P444 ?review . ?review pq:P7887 ?_review_count }");
    sb.append("OPTIONAL { ?item p:P444 ?review . ?review pq:P459 ?_score_method }}");

    Document doc;
    try {
      Url url = new Url("https://query.wikidata.org/sparql?query=" + URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8));
      // parse the result with JSOUP
      doc = Jsoup.parse(url.getInputStream(), UrlUtil.UTF_8, "");
    }
    catch (Exception e) {
      LOGGER.debug("could not get ratings - '{}'", e.getMessage());
      return Collections.emptyList();
    }

    List<MediaRating> ratings = new ArrayList<>();

    Elements elements = doc.getElementsByTag("result");
    for (Element element : elements) {
      MediaRating rating = parseRating(element);
      if (rating != null) {
        if (!ratings.contains(rating)) {
          ratings.add(rating);
        }
      }
    }

    return ratings;
  }

  private MediaRating parseRating(Element element) {
    MediaRating rating = null;

    try {
      String provider = "";
      String method = "";
      String score = "";
      String votes = "";

      for (Element child : element.children()) {
        switch (child.attr("name")) {
          case "_score_byLabel":
            provider = child.text();
            break;

          case "_score_methodLabel":
            method = child.text();
            break;

          case "_review_score":
            score = child.text();
            break;

          default:
            break;
        }
      }

      // all supported providers
      switch (provider) {
        case "Rotten Tomatoes":
          if ("Tomatometer score".equals(method) || StringUtils.isBlank(method)) {
            rating = new MediaRating("tomatometerallcritics");
            rating.setRating(Float.parseFloat(score.replace("%", "")));
            rating.setVotes(MetadataUtil.parseInt(votes, 0));
            rating.setMaxValue(100);
          }
          else if ("Rotten Tomatoes average rating".equals(method)) {
            rating = new MediaRating("tomatometeravgcritics");
            rating.setRating(Float.parseFloat(score.replace("/10", "")));
            rating.setVotes(MetadataUtil.parseInt(votes, 0));
            rating.setMaxValue(10);
          }
          break;

        case "Metacritic":
          rating = new MediaRating(MediaMetadata.METACRITIC);
          rating.setRating(Float.parseFloat(score.replace("/100", "")));
          rating.setVotes(MetadataUtil.parseInt(votes, 0));
          rating.setMaxValue(100);
          break;

        case "Internet Movie Database":
          rating = new MediaRating(MediaMetadata.IMDB);
          rating.setRating(Float.parseFloat(score.replace("/10", "")));
          rating.setVotes(MetadataUtil.parseInt(votes, 0));
          rating.setMaxValue(10);
          break;

        default:
          break;
      }

    }
    catch (Exception e) {
      LOGGER.debug("could not parse rating - '{}'", e.getMessage());
      return null;
    }

    return rating;
  }
}
