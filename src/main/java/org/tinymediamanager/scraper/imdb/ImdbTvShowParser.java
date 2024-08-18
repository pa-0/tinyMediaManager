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
package org.tinymediamanager.scraper.imdb;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.imdb.entities.ImdbEpisodeList;
import org.tinymediamanager.scraper.imdb.entities.ImdbIdValueType;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.JsonUtils;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The class ImdbTvShowParser is used to parse TV show site of imdb.com
 *
 * @author Manuel Laggner
 */
public class ImdbTvShowParser extends ImdbParser {
  private static final Logger                                LOGGER                 = LoggerFactory.getLogger(ImdbTvShowParser.class);
  private static final CacheMap<String, List<MediaMetadata>> EPISODE_LIST_CACHE_MAP = new CacheMap<>(60, 10);

  ImdbTvShowParser(IMediaProvider metadataProvider, ExecutorService executor) {
    super(metadataProvider, MediaType.TV_SHOW, executor);
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected boolean isIncludeTvSeriesResults() {
    return true;
  }

  @Override
  protected MediaMetadata getMetadata(MediaSearchAndScrapeOptions options) throws ScrapeException {
    switch (options.getMediaType()) {
      case TV_SHOW:
        return getTvShowMetadata((TvShowSearchAndScrapeOptions) options);

      case TV_EPISODE:
        return getEpisodeMetadata((TvShowEpisodeSearchAndScrapeOptions) options);

      default:
        return new MediaMetadata(ImdbMetadataProvider.ID);
    }
  }

  MediaMetadata getTvShowMetadata(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    MediaMetadata md = new MediaMetadata(ImdbMetadataProvider.ID);
    md.setScrapeOptions(options);

    String imdbId = "";

    // imdbId from searchResult
    if (options.getSearchResult() != null) {
      imdbId = options.getSearchResult().getIMDBId();
    }

    // imdbid from scraper option
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      imdbId = options.getImdbId();
    }

    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.warn("not possible to scrape from IMDB - no imdbId found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    LOGGER.debug("IMDB: getMetadata(imdbId): {}", imdbId);
    md.setId(ImdbMetadataProvider.ID, imdbId);
    md.addEpisodeGroup(MediaEpisodeGroup.DEFAULT_AIRED);

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

      try {
        doc = futureReference.get();
        if (doc != null) {
          parseReferencePage(doc, options, md);
        }

        doc = futurePlotsummary.get();
        if (doc != null) {
          parsePlotsummaryPage(doc, options, md);
        }

        // get the release info page
        Document releaseinfoDoc = futureReleaseinfo.get();
        if (releaseinfoDoc != null) {
          // get the date from the releaseinfo page
          parseReleaseinfoPage(releaseinfoDoc, options, md);
        }

        if (futureKeywords != null) {
          doc = futureKeywords.get();
          if (doc != null) {
            parseKeywordsPage(doc, options, md);
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

    // populate id
    md.setId(ImdbMetadataProvider.ID, imdbId);

    return md;
  }

  MediaMetadata getEpisodeMetadata(TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeMetadata(): {}", options);

    MediaMetadata md = new MediaMetadata(ImdbMetadataProvider.ID);
    md.setScrapeOptions(options);
    String showId = "" + options.getTvShowIds().get(MediaMetadata.IMDB);

    String episodeId = options.getIdAsString(MediaMetadata.IMDB);
    if (!MediaIdUtil.isValidImdbId(episodeId)) {
      episodeId = "";
    }

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    if ((seasonNr == -1 || episodeNr == -1) && StringUtils.isBlank(episodeId)) {
      throw new MissingIdException(MediaMetadata.EPISODE_NR, MediaMetadata.SEASON_NR);
    }
    // we want this, we should set this (in case of json error)
    md.setEpisodeNumber(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, seasonNr, episodeNr));

    // first get the base episode metadata which can be gathered via getEpisodeList()
    // only if we get a S/E number
    MediaMetadata wantedEpisode = null;
    if (episodeId.isEmpty() && seasonNr >= 0 && episodeNr > 0) {
      if (!MediaIdUtil.isValidImdbId(showId)) {
        LOGGER.warn("not possible to scrape from IMDB - no imdbId found");
        throw new MissingIdException(MediaMetadata.IMDB);
      }

      List<MediaMetadata> episodes = getEpisodeList(options.createTvShowSearchAndScrapeOptions());

      // search by ID
      if (StringUtils.isNotBlank(episodeId)) {
        for (MediaMetadata episode : episodes) {
          if (episodeId.equals(episode.getId(MediaMetadata.IMDB))) {
            wantedEpisode = episode;
            break;
          }
        }
      }

      // search by S/E
      if (wantedEpisode == null) {
        for (MediaMetadata episode : episodes) {
          MediaEpisodeNumber episodeNumber = episode.getEpisodeNumber(MediaEpisodeGroup.EpisodeGroupType.AIRED);

          if (episodeNumber != null && episodeNumber.season() == seasonNr && episodeNumber.episode() == episodeNr) {
            // search via season/episode number
            wantedEpisode = episode;
            break;
          }
        }
      }
    }

    // we did not find the episode; return
    if (wantedEpisode == null && StringUtils.isBlank(episodeId)) {
      LOGGER.warn("episode not found");
      throw new NothingFoundException();
    }

    // match via episodelist found
    if (wantedEpisode != null && wantedEpisode.getId(ImdbMetadataProvider.ID) instanceof String) {
      episodeId = (String) wantedEpisode.getId(ImdbMetadataProvider.ID);
      md.setEpisodeNumbers(wantedEpisode.getEpisodeNumbers());
      md.setTitle(wantedEpisode.getTitle());
      md.setPlot(wantedEpisode.getPlot());
      md.setRatings(wantedEpisode.getRatings());
      md.setReleaseDate(wantedEpisode.getReleaseDate());
    }

    // and finally the cast which needed to be fetched from the reference page (or json detail page)
    if (MediaIdUtil.isValidImdbId(episodeId)) {
      md.setId(ImdbMetadataProvider.ID, episodeId);

      // default workers which always run
      Document doc = null;
      boolean json = false;
      Callable<Document> worker = new ImdbWorker(constructUrl("title/", episodeId), options.getLanguage().getLanguage(),
          options.getCertificationCountry().getAlpha2(), true);
      Future<Document> futureDetail = executor.submit(worker);

      worker = new ImdbWorker(constructUrl("title/", episodeId, decode("L3JlZmVyZW5jZQ==")), options.getLanguage().getLanguage(),
          options.getCertificationCountry().getAlpha2(), true);
      Future<Document> futureReference = executor.submit(worker);

      Future<Document> futureKeywords = null;
      if (isScrapeKeywordsPage() && getMaxKeywordCount() > 5) {
        worker = new ImdbWorker(constructUrl("title/", episodeId, decode("L2tleXdvcmRz")), options.getLanguage().getLanguage(),
            options.getCertificationCountry().getAlpha2(), true);
        futureKeywords = executor.submit(worker);
      }

      Future<Document> futureReleaseInfo = null;
      if (!isScrapeLocalReleaseDate()) {
        worker = new ImdbWorker(constructUrl("title/", episodeId, decode("L3JlbGVhc2VpbmZv")), options.getLanguage().getLanguage(),
            options.getCertificationCountry().getAlpha2(), true);
        futureReleaseInfo = executor.submit(worker);
      }

      try {
        doc = futureDetail.get();
        parseDetailPageJson(doc, options, md);
        json = true;
      }
      catch (Exception e1) {
        LOGGER.warn("Could not get detailpage for id '{}' - '{}'", episodeId, e1.getMessage());
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
        try {
          if (futureReference != null) {
            Document docReference = futureReference.get();
            if (docReference != null) {
              parseEpisodeReference(docReference, md, episodeId);
            }
          }

          if (futureKeywords != null) {
            Document docKeywords = futureKeywords.get();
            if (docKeywords != null) {
              parseKeywordsPage(docKeywords, options, md);
            }
          }
        }
        catch (Exception e) {
          LOGGER.trace("problem parsing: {}", e.getMessage());
        }
      }
    }

    return md;
  }

  List<MediaMetadata> getEpisodeList(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeList(): {}", options);

    // parse the episodes from the ratings overview page (e.g.
    // http://www.imdb.com/title/tt0491738/episodes )
    String imdbId = options.getImdbId();
    if (StringUtils.isBlank(imdbId)) {
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    // look in the cache map if there is an entry
    List<MediaMetadata> episodes = EPISODE_LIST_CACHE_MAP.get(imdbId + "_" + options.getLanguage().getLanguage());
    if (ListUtils.isNotEmpty(episodes)) {
      // cache hit!
      return episodes;
    }

    episodes = new ArrayList<>();

    // we need to parse every season for its own _._
    // get the page for the first season (this is available in 99,9% of all cases)

    Document doc;
    Url url;
    try {
      // cache this on disk because that may be called multiple times
      url = new OnDiskCachedUrl(constructUrl("/title/", imdbId, "/episodes?season=1"), 1, TimeUnit.DAYS);
      url.addHeader("Accept-Language", getAcceptLanguage(options.getLanguage().getLanguage(), options.getCertificationCountry().getAlpha2()));
    }
    catch (Exception e) {
      LOGGER.error("problem scraping: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    List<String> availableSeasons = new ArrayList<>();

    try (InputStream is = url.getInputStream()) {
      doc = Jsoup.parse(is, "UTF-8", "");
      if (doc != null) {
        ImdbEpisodeList epList = parseEpisodeListJSON(doc);
        if (epList != null) {
          // JSON parsing worked
          episodes.addAll(epList.getEpisodes());
          for (ImdbIdValueType season : ListUtils.nullSafe(epList.seasons)) {
            if (!"1".equals(season.value)) {
              availableSeasons.add(season.value);
            }
          }
        }

        // no results via JSON? use old style...
        if (availableSeasons.isEmpty() || episodes.isEmpty()) {
          parseEpisodeList(1, episodes, doc);
          // get the other seasons out of the select option
          Element select = doc.getElementById("bySeason");
          if (select != null) {
            for (Element option : select.getElementsByTag("option")) {
              String value = option.attr("value");
              if (StringUtils.isNotBlank(value) && !"1".equals(value)) {
                availableSeasons.add(value);
              }
            }
          }
        }
      }
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.error("problem scraping: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    // then parse every season
    for (String seasonAsString : availableSeasons) {
      int season = MetadataUtil.parseInt(seasonAsString, -1);
      if (season < 0) {
        LOGGER.debug("could not parse season number - {}", seasonAsString);
        continue;
      }

      Url seasonUrl;
      try {
        seasonUrl = new OnDiskCachedUrl(constructUrl("/title/", imdbId, "/epdate?season=" + season), 1, TimeUnit.DAYS);
        seasonUrl.addHeader("Accept-Language", getAcceptLanguage(options.getLanguage().getLanguage(), options.getCertificationCountry().getAlpha2()));
      }
      catch (Exception e) {
        LOGGER.error("problem scraping: {}", e.getMessage());
        throw new ScrapeException(e);
      }

      try (InputStream is = seasonUrl.getInputStream()) {
        doc = Jsoup.parse(is, "UTF-8", "");

        ImdbEpisodeList epList = parseEpisodeListJSON(doc);
        if (epList != null && !epList.getEpisodes().isEmpty()) {
          episodes.addAll(epList.getEpisodes());
        }
        else {
          // if the given season number and the parsed one does not match, break here
          if (!parseEpisodeList(season, episodes, doc)) {
            break;
          }
        }
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.warn("problem parsing ep list: {}", e.getMessage());
      }
    }

    // cache for further fast access
    if (!episodes.isEmpty()) {
      EPISODE_LIST_CACHE_MAP.put(imdbId + "_" + options.getLanguage().getLanguage(), episodes);
    }

    return episodes;
  }

  private ImdbEpisodeList parseEpisodeListJSON(Document doc) {
    try {
      Element jsonNode = doc.getElementById("__NEXT_DATA__");
      if (jsonNode == null) {
        return null;
      }
      String json = jsonNode.data();
      // System.out.println(json);
      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(json);

      JsonNode epListNode = JsonUtils.at(node, "/props/pageProps/contentData/section");
      ImdbEpisodeList epList = JsonUtils.parseObject(mapper, epListNode, ImdbEpisodeList.class);
      return epList;
    }
    catch (Exception e) {
      getLogger().warn("Error parsing JSON: '{}'", e);
    }
    return null;
  }

  private boolean parseEpisodeList(int season, List<MediaMetadata> episodes, Document doc) {
    Pattern unknownPattern = Pattern.compile("Unknown");
    Pattern seasonEpisodePattern = Pattern.compile("S([0-9]*), Ep([0-9]*)");
    int specialEpisodeCounter = 0;

    // parse episodes
    Elements tables = doc.getElementsByClass("eplist");
    if (tables.isEmpty()) {
      // no episodes here? break
      return false;
    }

    for (Element table : tables) {
      Elements rows = table.getElementsByClass("list_item");
      for (Element row : rows) {
        Matcher matcher = season <= 0 ? unknownPattern.matcher(row.text()) : seasonEpisodePattern.matcher(row.text());
        if (matcher.find() && (season <= 0 || matcher.groupCount() >= 2)) {
          try {
            // we found a row containing episode data
            MediaMetadata ep = new MediaMetadata(ImdbMetadataProvider.ID);

            // parse season and ep number
            if (season <= 0) {
              ep.setEpisodeNumber(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 0, ++specialEpisodeCounter));
            }
            else {
              int s = Integer.parseInt(matcher.group(1));
              int e = Integer.parseInt(matcher.group(2));

              // check if we have still valid data
              if (season != s) {
                return false;
              }

              ep.setEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, s, e);
            }

            // get ep title and id
            Elements anchors = row.getElementsByAttributeValueStarting("href", "/title/tt");
            for (Element anchor : anchors) {
              if ("name".equals(anchor.attr("itemprop"))) {
                ep.setTitle(anchor.text());
                break;
              }
            }

            String id = "";
            Matcher idMatcher = IMDB_ID_PATTERN.matcher(anchors.get(0).attr("href"));
            while (idMatcher.find()) {
              if (idMatcher.group(1) != null) {
                id = idMatcher.group(1);
              }
            }

            if (StringUtils.isNotBlank(id)) {
              ep.setId(ImdbMetadataProvider.ID, id);
            }

            // plot
            Element plot = row.getElementsByClass("item_description").first();
            if (plot != null) {
              ep.setPlot(plot.ownText());
            }

            // rating and rating count
            Element ratingElement = row.getElementsByClass("ipl-rating-star__rating").first();
            if (ratingElement != null) {
              String ratingAsString = ratingElement.ownText().replace(",", ".");

              Element votesElement = row.getElementsByClass("ipl-rating-star__total-votes").first();
              if (votesElement != null) {
                String countAsString = votesElement.ownText().replaceAll("[.,()]", "").strip();
                try {
                  MediaRating rating = new MediaRating(ImdbMetadataProvider.ID);
                  rating.setRating(Float.parseFloat(ratingAsString));
                  rating.setVotes(MetadataUtil.parseInt(countAsString));
                  ep.addRating(rating);
                }
                catch (Exception e) {
                  LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
                }
              }
            }

            // release date
            Element releaseDate = row.getElementsByClass("airdate").first();
            if (releaseDate != null) {
              ep.setReleaseDate(parseDate(releaseDate.ownText()));
            }

            // poster
            Element image = row.getElementsByTag("img").first();
            if (image != null) {
              String posterUrl = image.attr("src");
              String thumb = posterUrl; // unmodified, small
              posterUrl = posterUrl.replaceAll("UX[0-9]{2,4}_", "");
              posterUrl = posterUrl.replaceAll("UY[0-9]{2,4}_", "");
              posterUrl = posterUrl.replaceAll("CR[0-9]{1,3},[0-9]{1,3},[0-9]{1,3},[0-9]{1,3}_", "");

              if (StringUtils.isNotBlank(posterUrl)) {
                MediaArtwork ma = new MediaArtwork(ImdbMetadataProvider.ID, THUMB);
                ma.setPreviewUrl(thumb);
                ma.setOriginalUrl(posterUrl);
                ep.addMediaArt(ma);
              }
            }

            episodes.add(ep);
          }
          catch (Exception e) {
            LOGGER.warn("failed parsing: {} for ep data - {}", row.text(), e.getMessage());
          }
        }
      }
    }
    return true;
  }

  private void parseEpisodeReference(Document doc, MediaMetadata md, String imdbId) {
    // title (h3 itemprop=name)
    Element header = doc.getElementsByClass("titlereference-header").first();
    if (header != null) {
      Element year = header.getElementsByClass("titlereference-title-year").first();
      if (year != null) {
        String episodeTitle = cleanString(year.parent().ownText());
        if (StringUtils.isNotBlank(episodeTitle)) {
          md.setTitle(episodeTitle);
        }
      }
    }

    // title: fallback to title from meta
    if (StringUtils.isBlank(md.getTitle())) {
      Element title = doc.getElementsByAttributeValue("name", "title").first();
      if (title != null) {
        String episodeTitle = cleanString(title.attr("content"));

        int yearStart = episodeTitle.lastIndexOf('(');
        if (yearStart > 0) {
          episodeTitle = episodeTitle.substring(0, yearStart - 1).strip();
          md.setTitle(episodeTitle);
        }
      }
    }

    // plot
    // class titlereference-section-overview -> first div without class
    Element titlereference = doc.getElementsByClass("titlereference-section-overview").first();
    if (titlereference != null) {
      for (Element child : titlereference.children()) {
        if ("div".equals(child.tagName()) && child.classNames().isEmpty()) {
          String plot = child.text();
          if (StringUtils.isBlank(md.getPlot()) && StringUtils.isNotBlank(plot)) {
            md.setPlot(plot);
            break;
          }
        }
      }
    }

    // releasedate
    Element releaseDateElement = doc.getElementsByAttributeValue("href", "/title/" + imdbId + "/releaseinfo").first();
    if (releaseDateElement != null) {
      String releaseDateText = releaseDateElement.ownText();
      int startOfCountry = releaseDateText.indexOf('(');
      if (startOfCountry > 0) {
        releaseDateText = releaseDateText.substring(0, startOfCountry - 1).strip();
      }
      md.setReleaseDate(parseDate(releaseDateText));
    }

    // poster
    Element poster = doc.getElementsByAttributeValue("property", "og:image").first();
    if (poster != null) {
      String posterUrl = poster.attr("content");

      int fileStart = posterUrl.lastIndexOf('/');
      if (fileStart > 0) {
        int parameterStart = posterUrl.indexOf('_', fileStart);
        if (parameterStart > 0) {
          int startOfExtension = posterUrl.lastIndexOf('.');
          if (startOfExtension > parameterStart) {
            posterUrl = posterUrl.substring(0, parameterStart) + posterUrl.substring(startOfExtension);
          }
        }
      }
      processMediaArt(md, THUMB, posterUrl);
    }

    // rating and rating count
    Element ratingElement = doc.getElementsByClass("ipl-rating-star__rating").first();
    if (ratingElement != null) {
      String ratingAsString = ratingElement.ownText().replace(",", ".");
      Element votesElement = doc.getElementsByClass("ipl-rating-star__total-votes").first();
      if (votesElement != null) {
        String countAsString = votesElement.ownText().replaceAll("[.,()]", "").strip();
        try {
          MediaRating rating = new MediaRating(MediaMetadata.IMDB);
          rating.setRating(Float.parseFloat(ratingAsString));
          rating.setVotes(MetadataUtil.parseInt(countAsString));
          md.addRating(rating);
        }
        catch (Exception e) {
          getLogger().trace("could not parse rating/vote count: {}", e.getMessage());
        }
      }
    }

    // director
    Element directorsElement = doc.getElementById("directors");
    while (directorsElement != null && !"header".equals(directorsElement.tag().getName())) {
      directorsElement = directorsElement.parent();
    }
    if (directorsElement != null) {
      directorsElement = directorsElement.nextElementSibling();
    }
    if (directorsElement != null) {
      for (Element directorElement : directorsElement.getElementsByClass("name")) {
        String director = directorElement.text().strip();

        Person cm = new Person(Person.Type.DIRECTOR, director);
        // profile path
        Element anchor = directorElement.getElementsByAttributeValueStarting("href", "/name/").first();
        if (anchor != null) {
          Matcher matcher = PERSON_ID_PATTERN.matcher(anchor.attr("href"));
          if (matcher.find()) {
            if (matcher.group(0) != null) {
              cm.setProfileUrl("http://www.imdb.com" + matcher.group(0));
            }
            if (matcher.group(1) != null) {
              cm.setId(ImdbMetadataProvider.ID, matcher.group(1));
            }
          }
        }
        md.addCastMember(cm);
      }
    }

    // actors
    Element castTableElement = doc.getElementsByClass("cast_list").first();
    if (castTableElement != null) {
      Elements castListLabel = castTableElement.getElementsByClass("castlist_label");
      Elements tr = castTableElement.getElementsByTag("tr");
      for (Element row : tr) {
        // check if we're at the uncredited cast members
        if (!isScrapeUncreditedActors() && castListLabel.size() > 1 && row.children().contains(castListLabel.get(1))) {
          break;
        }

        Person cm = parseCastMember(row);
        if (cm != null && StringUtils.isNotEmpty(cm.getName())) {
          cm.setType(ACTOR);
          md.addCastMember(cm);
        }
      }
    }

    // writers
    Element writersElement = doc.getElementById("writers");
    while (writersElement != null && !"header".equals(writersElement.tag().getName())) {
      writersElement = writersElement.parent();
    }
    if (writersElement != null) {
      writersElement = writersElement.nextElementSibling();
    }
    if (writersElement != null) {
      Elements writersElements = writersElement.getElementsByAttributeValueStarting("href", "/name/");

      for (Element writerElement : writersElements) {
        String writer = cleanString(writerElement.ownText());
        Person cm = new Person(WRITER, writer);
        // profile path
        Element anchor = writerElement.getElementsByAttributeValueStarting("href", "/name/").first();
        if (anchor != null) {
          Matcher matcher = PERSON_ID_PATTERN.matcher(anchor.attr("href"));
          if (matcher.find()) {
            if (matcher.group(0) != null) {
              cm.setProfileUrl("http://www.imdb.com" + matcher.group(0));
            }
            if (matcher.group(1) != null) {
              cm.setId(ImdbMetadataProvider.ID, matcher.group(1));
            }
          }
        }
        md.addCastMember(cm);
      }
    }
  }

  public List<MediaArtwork> getTvShowArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {
    String imdbId = options.getImdbId();

    // imdbid via tmdbid
    if (!MediaIdUtil.isValidImdbId(imdbId) && options.getTmdbId() > 0) {
      imdbId = MediaIdUtil.getTvShowImdbIdViaTmdbId(options.getTmdbId());
    }

    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.warn("not possible to scrape from IMDB - no imdbId found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    // just get the MediaMetadata via normal scrape and pick the poster from the result
    try {
      List<MediaArtwork> artworks = Collections.emptyList();
      if (options.getMediaType() == MediaType.TV_EPISODE) {
        TvShowEpisodeSearchAndScrapeOptions op = new TvShowEpisodeSearchAndScrapeOptions();
        op.setDataFromOtherOptions(options);
        if (options.getIds().get(MediaMetadata.TVSHOW_IDS) instanceof Map) {
          Map<String, Object> tvShowIds = (Map<String, Object>) options.getIds().get(MediaMetadata.TVSHOW_IDS);
          op.setTvShowIds(tvShowIds);
        }
        artworks = getMetadata(op).getMediaArt(options.getArtworkType());
      }
      else {
        TvShowSearchAndScrapeOptions op = new TvShowSearchAndScrapeOptions();
        op.setDataFromOtherOptions(options);
        artworks = getMetadata(op).getMediaArt(options.getArtworkType());
      }

      return artworks;
    }
    catch (NothingFoundException e) {
      LOGGER.debug("nothing found");
    }

    return Collections.emptyList();
  }

  public Map<String, Integer> getTvShowTop250() {
    return parseTop250("/chart/toptv/");
  }
}
