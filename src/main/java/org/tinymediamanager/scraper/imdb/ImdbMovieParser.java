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
package org.tinymediamanager.scraper.imdb;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;

/**
 * The class ImdbMovieParser is used to parse the movie sites at imdb.com
 *
 * @author Manuel Laggner
 */
public class ImdbMovieParser extends ImdbParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImdbMovieParser.class);

  ImdbMovieParser(IMediaProvider mediaProvider, ExecutorService executor) {
    super(mediaProvider, MediaType.MOVIE, executor);
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected boolean isIncludeMovieResults() {
    return true;
  }

  @Override
  protected MediaMetadata getMetadata(MediaSearchAndScrapeOptions options) throws ScrapeException {
    return getMovieMetadata((MovieSearchAndScrapeOptions) options);
  }

  MediaMetadata getMovieMetadata(MovieSearchAndScrapeOptions options) throws ScrapeException {
    MediaMetadata md = new MediaMetadata(ImdbMetadataProvider.ID);
    md.setScrapeOptions(options);

    // check if there is a md in the result
    if (options.getMetadata() != null && ImdbMetadataProvider.ID.equals(options.getMetadata().getProviderId())) {
      LOGGER.debug("IMDB: got metadata from cache: {}", options.getMetadata());
      return options.getMetadata();
    }

    String imdbId = "";

    // imdbId from searchResult
    if (options.getSearchResult() != null) {
      imdbId = options.getSearchResult().getIMDBId();
    }

    // imdbid from scraper option
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      imdbId = options.getImdbId();
    }

    // imdbid via tmdbid
    if (!MediaIdUtil.isValidImdbId(imdbId) && options.getTmdbId() > 0) {
      imdbId = MediaIdUtil.getMovieImdbIdViaTmdbId(options.getTmdbId());
    }

    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.warn("not possible to scrape from IMDB - imdbId found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    LOGGER.debug("IMDB: getMetadata(imdbId): {}", imdbId);
    md.setId(ImdbMetadataProvider.ID, imdbId);

    // default workers which always run
    Document doc = null;
    boolean json = false;
    Callable<Document> worker = new ImdbWorker(constructUrl("title/", imdbId), options.getLanguage().getLanguage(),
        options.getCertificationCountry().getAlpha2(), true);
    Future<Document> futureDetail = executor.submit(worker);

    worker = new ImdbWorker(constructUrl("title/", imdbId, decode("L3JlZmVyZW5jZQ==")), options.getLanguage().getLanguage(),
        options.getCertificationCountry().getAlpha2(), true);
    Future<Document> futureReference = executor.submit(worker);

    Future<Document> futureKeywords = null;
    if (isScrapeKeywordsPage() && getMaxKeywordCount() > 5) {
      worker = new ImdbWorker(constructUrl("title/", imdbId, decode("L2tleXdvcmRz")), options.getLanguage().getLanguage(),
          options.getCertificationCountry().getAlpha2(), true);
      futureKeywords = executor.submit(worker);
    }

    Future<Document> futureReleaseInfo = null;
    if (!isScrapeLocalReleaseDate()) {
      worker = new ImdbWorker(constructUrl("title/", imdbId, decode("L3JlbGVhc2VpbmZv")), options.getLanguage().getLanguage(),
          options.getCertificationCountry().getAlpha2(), true);
      futureReleaseInfo = executor.submit(worker);
    }

    try {
      doc = futureDetail.get();
      parseDetailPageJson(doc, options, md);
      json = true;
    }
    catch (Exception e) {
      LOGGER.warn("Could not get detailpage for id '{}' - '{}'", imdbId, e.getMessage());
    }

    if (json) {
      // detail page worked, mix-in missing
      try {
        MediaMetadata md2 = new MediaMetadata(ImdbMetadataProvider.ID);
        doc = futureReference.get();
        if (doc != null) {
          parseReferencePage(doc, options, md2);
          md.setTagline(md2.getTagline());
          md.setCastMembers(md2.getCastMembers()); // overwrite all
          md.setTop250(md2.getTop250());
          md2.getCertifications().forEach(md::addCertification); // reference page has more certifications
        }

        // if we have more that 5 keywords, we need to scrape dedicated page, as only 5 on detail page...
        if (isScrapeKeywordsPage() && getMaxKeywordCount() > 5) {
          if (futureKeywords != null) {
            doc = futureKeywords.get();
            if (doc != null) {
              parseKeywordsPage(doc, options, md2);
              md.setTags(md2.getTags());// overwrite all
            }
          }
        }

        // if we want to scrape NOT the local release date, we take the FIRST from releaseinfo page
        if (!isScrapeLocalReleaseDate()) {
          if (futureReleaseInfo != null) {
            doc = futureReleaseInfo.get();
            if (doc != null) {
              parseReleaseinfoPageJson(doc, options, md2);
              md.setReleaseDate(md2.getReleaseDate());
            }
          }
        }

      }
      catch (Exception e) {
        LOGGER.warn("Could not parse page: {}", e.getMessage());
      }
    }
    else {
      // fallback old style, when json parsing was not ok
      Future<Document> futurePlotsummary;
      worker = new ImdbWorker(constructUrl("title/", imdbId, decode("L3Bsb3RzdW1tYXJ5")), options.getLanguage().getLanguage(),
          options.getCertificationCountry().getAlpha2(), true);
      futurePlotsummary = executor.submit(worker);

      Future<Document> futureReleaseinfo;
      worker = new ImdbWorker(constructUrl("title/", imdbId, decode("L3JlbGVhc2VpbmZv")), options.getLanguage().getLanguage(),
          options.getCertificationCountry().getAlpha2(), true);
      futureReleaseinfo = executor.submit(worker);

      Future<Document> futureCritics = null;
      if (Boolean.TRUE.equals(config.getValueAsBool("includeMetacritic"))) {
        worker = new ImdbWorker(constructUrl("title/", imdbId, decode("L2NyaXRpY3Jldmlld3M=")), options.getLanguage().getLanguage(),
            options.getCertificationCountry().getAlpha2(), true);
        futureCritics = executor.submit(worker);
      }

      try {
        doc = futureReference.get();
        if (doc != null) {
          parseReferencePage(doc, options, md);
        }

        doc = futurePlotsummary.get();
        if (doc != null) {
          parsePlotsummaryPage(doc, options, md);
        }

        if (futureKeywords != null) {
          doc = futureKeywords.get();
          if (doc != null) {
            parseKeywordsPage(doc, options, md);
          }
        }

        // get the release info page
        Document releaseinfoDoc = futureReleaseinfo.get();
        // parse original title here!!
        if (releaseinfoDoc != null) {
          parseReleaseinfoPageAKAs(releaseinfoDoc, md);

          // get the date from the releaseinfo page
          parseReleaseinfoPage(releaseinfoDoc, options, md);
        }

        // get critics
        if (futureCritics != null) {
          Document criticsDoc = futureCritics.get();
          if (criticsDoc != null) {
            parseCritics(criticsDoc, md);
          }
        }
      }
      catch (Exception e) {
        LOGGER.error("problem while scraping: {}", e.getMessage());
        throw new ScrapeException(e);
      }

      if (md.getIds().isEmpty()) {
        LOGGER.warn("nothing found");
        throw new NothingFoundException();
      }
    }

    // if we have still no original title, take the title
    if (StringUtils.isBlank(md.getOriginalTitle())) {
      md.setOriginalTitle(md.getTitle());
    }

    // populate id
    md.setId(ImdbMetadataProvider.ID, imdbId);

    return md;
  }

  public List<MediaRating> getRatings(Map<String, Object> ids) throws ScrapeException {
    LOGGER.debug("getRatings(): {}", ids);

    String imdbId = MediaIdUtil.getIdAsString(ids, MediaMetadata.IMDB);
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    MediaMetadata md = new MediaMetadata(ImdbMetadataProvider.ID);

    // get critics
    Callable<Document> worker = new ImdbParser.ImdbWorker(constructUrl("title/", imdbId, decode("L2NyaXRpY3Jldmlld3M=")), "en", "US", true);
    Future<Document> futureCritics = executor.submit(worker);

    try {
      Document criticsDoc = futureCritics.get();
      if (criticsDoc != null) {
        parseCritics(criticsDoc, md);
      }
    }
    catch (Exception e) {
      LOGGER.debug("Could not get ratings - '{}'", e.getMessage());
    }

    // get IMDB rating
    MediaRating imdbRating = RatingProvider.getImdbRating(imdbId);
    if (imdbRating != null) {
      md.addRating(imdbRating);
    }

    return md.getRatings();
  }

  private void parseCritics(Document doc, MediaMetadata md) {
    // <div class="metascore_block" itemprop="aggregateRating" itemscope="" itemtype="http://schema.org/AggregateRating">
    // <span itemprop="ratingValue">53</span>
    // Based on <span itemprop="ratingCount">36</span>
    // </div>
    for (Element div : doc.getElementsByClass("metascore_block")) {
      int value = 0;
      int count = 0;

      Elements spans = div.getElementsByTag("span");
      for (Element span : spans) {
        if ("ratingValue".equals(span.attr("itemprop"))) {
          value = MetadataUtil.parseInt(span.text(), 0);
        }
        if ("ratingCount".equals(span.attr("itemprop"))) {
          count = MetadataUtil.parseInt(span.text(), 0);
        }
      }

      if (value > 0) {
        MediaRating rating = new MediaRating(MediaMetadata.METACRITIC);
        rating.setRating(value);
        rating.setVotes(count);
        rating.setMaxValue(100);
        md.addRating(rating);

        break;
      }
    }
  }

  // AKAs and original title
  private void parseReleaseinfoPageAKAs(Document doc, MediaMetadata md) {
    // <table id="akas" class="subpage_data spEven2Col">
    // <tr class="even">
    // <td>(original title)</td>
    // <td>Intouchables</td>
    // </tr>

    // need to search all tables for correct ID, since the UNIQUE id is used multiple times - thanks for nothing :p
    for (Element table : doc.getElementsByTag("table")) {
      if (table.id().equalsIgnoreCase("akas")) {
        Elements rows = table.getElementsByTag("tr");
        for (Element row : rows) {
          Element c1 = row.getElementsByTag("td").get(0);
          Element c2 = row.getElementsByTag("td").get(1);
          if (c1 != null && c1.text().toLowerCase(Locale.ROOT).contains("original title")) {
            md.setOriginalTitle(c2.text());
            break;
          }
        }
      }
    }

    // alternative; new way with table classes
    // <tr class="ipl-zebra-list__item aka-item">
    // <td class="aka-item__name">Germany</td>
    // <td class="aka-item__title">Avatar - Aufbruch nach Pandora</td>
    // </tr>
    Elements rows = doc.getElementsByClass("aka-item");
    for (Element row : rows) {
      Element country = row.getElementsByClass("aka-item__name").first();
      Element title = row.getElementsByClass("aka-item__title").first();
      if (country != null && country.text().toLowerCase(Locale.ROOT).contains("original title")) {
        md.setOriginalTitle(title.text());
        break;
      }
    }
  }

  public List<MediaArtwork> getMovieArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {
    String imdbId = options.getImdbId();
    // imdbid via tmdbid
    if (!MediaIdUtil.isValidImdbId(imdbId) && options.getTmdbId() > 0) {
      imdbId = MediaIdUtil.getMovieImdbIdViaTmdbId(options.getTmdbId());
    }
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.warn("not possible to scrape from IMDB - no imdbId found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    try {
      List<MediaArtwork> artworks = getMediaArt(options);
      if (options.getArtworkType() == MediaArtworkType.ALL) {
        return artworks;
      }
      return artworks.stream().filter(ma -> ma.getType() == options.getArtworkType()).toList();
    }
    catch (Exception e) {
      LOGGER.warn("Error getting arworks: {}", e.getMessage());
    }

    return Collections.emptyList();
  }

  public Map<String, Integer> getMovieTop250() {
    return parseTop250("/chart/top/");
  }
}
