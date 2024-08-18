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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.util.MetadataUtil;

/**
 * The class {@link TvShowSeasonNfoParser} is used to parse all types of NFO/XML files
 *
 * @author Manuel Laggner
 */
public class TvShowSeasonNfoParser {
  private static final Logger       LOGGER              = LoggerFactory.getLogger(TvShowSeasonNfoParser.class);
  /**
   * ignore the following tags since they originally do not belong to a TV show NFO
   */
  private static final List<String> IGNORE              = Arrays.asList("epbookmark", "resume");

  private Element                   root;
  private final List<String>        supportedElements   = new ArrayList<>();

  public int                        season              = -1;
  public String                     title               = "";
  public String                     sortTitle           = "";
  public String                     plot                = "";

  public List<String>               posters             = new ArrayList<>();
  public List<String>               banners             = new ArrayList<>();
  public List<String>               thumbs              = new ArrayList<>();
  public List<String>               fanarts             = new ArrayList<>();

  public List<String>               unsupportedElements = new ArrayList<>();

  /* some xbmc related tags we parse, but do not use internally */

  /**
   * create a new instance by parsing the document
   *
   * @param document
   *          the document returned by JSOUP.parse()
   */
  private TvShowSeasonNfoParser(Document document) {
    // first check if there is a valid root object
    Elements elements = document.select("season");
    if (elements.isEmpty()) {
      return;
    }

    document.outputSettings().prettyPrint(false);

    this.root = elements.get(0);

    // parse all supported fields
    parseTag(TvShowSeasonNfoParser::parseSeasonNumber);
    parseTag(TvShowSeasonNfoParser::parseTitle);
    parseTag(TvShowSeasonNfoParser::parseSortTitle);
    parseTag(TvShowSeasonNfoParser::parsePlot);
    parseTag(TvShowSeasonNfoParser::parsePosters);
    parseTag(TvShowSeasonNfoParser::parseBanners);
    parseTag(TvShowSeasonNfoParser::parseThumbs);
    parseTag(TvShowSeasonNfoParser::parseFanarts);

    // MUST BE THE LAST ONE!
    parseTag(TvShowSeasonNfoParser::findUnsupportedElements);
  }

  /**
   * parse the tag in a save way
   *
   * @param function
   *          the parsing function to be executed
   */
  private Void parseTag(Function<TvShowSeasonNfoParser, Void> function) {
    try {
      function.apply(this);
    }
    catch (Exception e) {
      LOGGER.warn("problem parsing tag (line {}): {}", e.getStackTrace()[0].getLineNumber(), e.getMessage());
    }

    return null;
  }

  /**
   * parse the given file
   *
   * @param path
   *          the path to the NFO/XML to be parsed
   * @return a new instance of the parser class
   * @throws IOException
   *           any exception if parsing fails
   */
  public static TvShowSeasonNfoParser parseNfo(Path path) throws IOException {
    try (InputStream is = Files.newInputStream(path)) {
      return new TvShowSeasonNfoParser(Jsoup.parse(is, "UTF-8", "", Parser.xmlParser()));
    }
  }

  /**
   * parse the xml content
   *
   * @param content
   *          the content of the NFO/XML to be parsed
   * @return a new instance of the parser class
   */
  public static TvShowSeasonNfoParser parseNfo(String content) {
    return new TvShowSeasonNfoParser(Jsoup.parse(content, "", Parser.xmlParser()));
  }

  /**
   * determines whether this was a valid NFO or not<br />
   * we use several fields which should be filled in a valid NFO for decision
   *
   * @return true/false
   */
  public boolean isValidNfo() {
    return season > -1;
  }

  private Element getSingleElement(Element parent, String tag) {
    Elements elements = parent.select(parent.tagName() + " > " + tag);
    if (elements.size() != 1) {
      return null;
    }
    return elements.get(0);
  }

  /**
   * the season number usually comes in the seasonnumber tag
   */
  private Void parseSeasonNumber() {
    supportedElements.add("seasonnumber");

    Element element = getSingleElement(root, "seasonnumber");
    if (element != null) {
      season = MetadataUtil.parseInt(element.text(), -1);
    }

    return null;
  }

  /**
   * the title usually comes in the title tag
   */
  private Void parseTitle() {
    supportedElements.add("title");

    Element element = getSingleElement(root, "title");
    if (element != null) {
      title = element.ownText();
    }

    return null;
  }

  /**
   * the sort title usually comes in the sorttitle tag
   */
  private Void parseSortTitle() {
    supportedElements.add("sorttitle");

    Element element = getSingleElement(root, "sorttitle");
    if (element != null) {
      sortTitle = element.ownText();
    }

    return null;
  }

  /**
   * the plot usually comes in the plot tag as an integer (or empty)
   */
  private Void parsePlot() {
    supportedElements.add("plot");

    Element element = getSingleElement(root, "plot");
    if (element != null) {
      plot = element.wholeText();
    }

    return null;
  }

  /**
   * posters are usually inside <thumb>xxx</thumb> tag with an aspect of "poster"<br />
   * but there are also season poster in this tag
   */
  private Void parsePosters() {
    supportedElements.add("thumb");

    // get all thumb elements
    Elements thumb = root.select(root.tagName() + " > thumb");
    if (!thumb.isEmpty()) {
      for (Element element : thumb) {
        // if there is an aspect attribute, it has to be poster
        if (element.hasAttr("aspect") && !element.attr("aspect").equals("poster")) {
          continue;
        }

        String posterUrl = element.ownText();
        if (StringUtils.isBlank(posterUrl) || !posterUrl.matches("https?://.*")) {
          continue;
        }

        if (!posters.contains(posterUrl)) {
          posters.add(posterUrl);
        }

      }
    }

    return null;
  }

  /**
   * banners are usually inside <thumb>xxx</thumb> tag with an aspect of "banner"
   */
  private Void parseBanners() {
    // supportedElements.add("thumb"); //already registered with posters

    // get all thumb elements
    Elements thumb = root.select(root.tagName() + " > thumb");
    if (!thumb.isEmpty()) {
      for (Element element : thumb) {
        // if there is an aspect attribute, it has to be poster
        if (element.hasAttr("aspect") && !element.attr("aspect").equals("banner")) {
          continue;
        }
        if (StringUtils.isNotBlank(element.ownText()) && element.ownText().matches("https?://.*")) {
          banners.add(element.ownText());
        }
      }
    }

    return null;
  }

  /**
   * thumbs are usually inside <thumb>xxx</thumb> tag with an aspect of "landscape"
   */
  private Void parseThumbs() {
    // supportedElements.add("thumb"); //already registered with posters

    // get all thumb elements
    Elements thumb = root.select(root.tagName() + " > thumb");
    if (!thumb.isEmpty()) {
      for (Element element : thumb) {
        // if there is an aspect attribute, it has to be poster
        if (element.hasAttr("aspect") && !element.attr("aspect").equals("landscape")) {
          continue;
        }
        if (StringUtils.isNotBlank(element.ownText()) && element.ownText().matches("https?://.*")) {
          thumbs.add(element.ownText());
        }
      }
    }

    return null;
  }

  /**
   * fanarts can come in several different forms<br />
   * - xbmc had it in one single fanart tag (no nested tags)<br />
   * - kodi usually puts it into a fanart tag (in newer versions a nested thumb tag)<br />
   * - mediaportal puts it also into a fanart tag (with nested thumb tags)
   */
  private Void parseFanarts() {
    supportedElements.add("fanart");

    // get all thumb elements
    Element fanart = getSingleElement(root, "fanart");
    if (fanart != null) {
      String prefix = fanart.attr("url");
      Elements thumb = fanart.select(fanart.tagName() + " > thumb");
      // thumb children available
      if (!thumb.isEmpty()) {
        for (Element element : thumb) {
          if (StringUtils.isNotBlank(element.ownText()) && element.ownText().matches("https?://.*")) {
            fanarts.add(element.ownText());
          }
          else if (StringUtils.isNotBlank(element.ownText()) && prefix.matches("https?://.*")) {
            fanarts.add(prefix + element.ownText());
          }
        }
      }
      // no children - get own text
      else if (StringUtils.isNotBlank(fanart.ownText()) && fanart.ownText().matches("https?://.*")) {
        fanarts.add(fanart.ownText());
      }
      else if (StringUtils.isNotBlank(fanart.ownText()) && prefix.matches("https?://.*")) {
        fanarts.add(prefix + fanart.ownText());
      }
    }

    return null;
  }

  /**
   * find and store all unsupported tags
   */
  private Void findUnsupportedElements() {
    // just ignore further elements
    supportedElements.add("lockdata");
    supportedElements.add("uniqueid");
    supportedElements.add("tvdbid");
    supportedElements.add("imdbid");
    supportedElements.add("tmdbid");
    supportedElements.add("year");
    supportedElements.add("showtitle");
    supportedElements.add("premiered");

    // get all children of the root
    for (Element element : root.children()) {
      if (!IGNORE.contains(element.tagName()) && !supportedElements.contains(element.tagName())) {
        String elementText = element.toString().replaceAll(">\\r?\\n\\s*<", "><");
        unsupportedElements.add(elementText);
      }
    }

    return null;
  }

  /**
   * morph this instance to a {@link TvShowSeason} object
   *
   * @return the {@link TvShowSeason} object
   */
  public TvShowSeason toTvShowSeason() {
    TvShowSeason showSeason = new TvShowSeason(season);
    showSeason.setTitle(title);
    showSeason.setPlot(plot);

    if (!posters.isEmpty()) {
      showSeason.setArtworkUrl(posters.get(0), MediaFileType.POSTER);
    }

    if (!banners.isEmpty()) {
      showSeason.setArtworkUrl(banners.get(0), MediaFileType.BANNER);
    }

    if (!thumbs.isEmpty()) {
      showSeason.setArtworkUrl(thumbs.get(0), MediaFileType.THUMB);
    }

    if (!fanarts.isEmpty()) {
      showSeason.setArtworkUrl(fanarts.get(0), MediaFileType.FANART);
    }

    return showSeason;
  }

  private org.tinymediamanager.core.entities.Person morphPerson(org.tinymediamanager.core.entities.Person.Type type, Person nfoPerson) {
    org.tinymediamanager.core.entities.Person person = new org.tinymediamanager.core.entities.Person(type);

    person.setName(nfoPerson.name);
    person.setRole(nfoPerson.role);
    person.setThumbUrl(nfoPerson.thumb);
    person.setProfileUrl(nfoPerson.profile);

    int tmdbId = MetadataUtil.parseInt(nfoPerson.tmdbId, 0);
    if (tmdbId > 0) {
      person.setId(MediaMetadata.TMDB, tmdbId);
    }

    if (StringUtils.isNotBlank(nfoPerson.imdbId)) {
      person.setId(MediaMetadata.IMDB, nfoPerson.imdbId);
    }

    int tvdbId = MetadataUtil.parseInt(nfoPerson.tvdbId, 0);
    if (tvdbId > 0) {
      person.setId(MediaMetadata.TVDB, tvdbId);
    }

    return person;
  }

  /*
   * entity classes
   */
  static class Rating {
    static final String DEFAULT  = "default";
    static final String USER     = "user";

    String              id       = "";
    float               rating   = 0;
    int                 votes    = 0;
    int                 maxValue = 10;
  }

  public static class Person {
    String name    = "";
    String role    = "";
    String thumb   = "";
    String profile = "";
    String tmdbId  = "";
    String imdbId  = "";
    String tvdbId  = "";
  }
}
