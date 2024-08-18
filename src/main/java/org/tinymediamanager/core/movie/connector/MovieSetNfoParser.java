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

package org.tinymediamanager.core.movie.connector;

import static org.tinymediamanager.scraper.MediaMetadata.TMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TMDB_SET;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.ParserUtils;

/**
 * The class MovieSetNfoParser is used to parse all types of NFO files for movie sets
 *
 * @author Manuel Laggner
 */
public class MovieSetNfoParser {
  private static final Logger LOGGER              = LoggerFactory.getLogger(MovieSetNfoParser.class);

  private Element             root;
  private final List<String>  supportedElements   = new ArrayList<>();

  public String               title               = "";
  public String               plot                = "";
  public String               userNote            = "";

  public Map<String, Object>  ids                 = new HashMap<>();
  public List<String>         tags                = new ArrayList<>();

  // supported for reading, but will not passed to tmm
  public List<MediaGenres>    genres              = new ArrayList<>();
  public List<String>         studios             = new ArrayList<>();

  public List<String>         unsupportedElements = new ArrayList<>();

  /**
   * create a new instance by parsing the document
   *
   * @param document
   *          the document returned by JSOUP.parse()
   */
  private MovieSetNfoParser(Document document) {
    document.outputSettings().prettyPrint(false);

    // first check if there is a valid root object
    Elements elements = document.select("collection");

    if (!elements.isEmpty()) {
      // parse Emby style
      parseEmbyNfo(elements.get(0));
      return;
    }
  }

  private void parseEmbyNfo(Element root) {
    this.root = root;

    // parse all supported fields
    parseTag(MovieSetNfoParser::parseTitle);
    parseTag(MovieSetNfoParser::parsePlot);
    parseTag(MovieSetNfoParser::parseIds);
    parseTag(MovieSetNfoParser::parseTags);
    parseTag(MovieSetNfoParser::parseUserNote);

    parseTag(MovieSetNfoParser::parseGenres);
    parseTag(MovieSetNfoParser::parseStudios);

    // MUST BE THE LAST ONE!
    parseTag(MovieSetNfoParser::findUnsupportedElements);
  }

  /**
   * parse the tag in a save way
   *
   * @param function
   *          the parsing function to be executed
   */
  private void parseTag(Function<MovieSetNfoParser, Void> function) {
    try {
      function.apply(this);
    }
    catch (Exception e) {
      LOGGER.warn("problem parsing tag (line {}): {}", e.getStackTrace()[0].getLineNumber(), e.getMessage());
    }
  }

  /**
   * parse the given file
   *
   * @param path
   *          the path to the NFO/XML to be parsed
   * @return a new instance of the parser class
   * @throws Exception
   *           any exception if parsing fails
   */
  public static MovieSetNfoParser parseNfo(Path path) throws Exception {
    try (InputStream is = Files.newInputStream(path)) {
      return new MovieSetNfoParser(Jsoup.parse(is, "UTF-8", "", Parser.xmlParser()));
    }
  }

  /**
   * parse the xml content
   *
   * @param content
   *          the content of the NFO/XML to be parsed
   * @return a new instance of the parser class
   * @throws Exception
   *           any exception if parsing fails
   */
  public static MovieSetNfoParser parseNfo(String content) throws Exception {
    return new MovieSetNfoParser(Jsoup.parse(content, "", Parser.xmlParser()));
  }

  /**
   * determines whether this was a valid NFO or not<br />
   * we use several fields which should be filled in a valid NFO for decision
   *
   * @return true/false
   */
  public boolean isValidNfo() {
    // we're happy if at least the title could be parsed
    return StringUtils.isNotBlank(title);
  }

  private Element getSingleElement(Element parent, String tag) {
    Elements elements = parent.select(parent.tagName() + " > " + tag);
    if (elements.size() != 1) {
      return null;
    }
    return elements.get(0);
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
   * ids can be stored either in the<br />
   * - tmdbid tag (tmdb Id> or<br />
   * - uniqueid tag (new kodi style multiple ids) or<br />
   * - in a special nested tag (tmm store)
   */
  private Void parseIds() {
    supportedElements.add("tmdbid");
    supportedElements.add("ids");
    supportedElements.add("uniqueid");

    // tmdbid tag
    Element element = getSingleElement(root, "tmdbid");
    if (element != null) {
      try {
        ids.put(MediaMetadata.TMDB, MetadataUtil.parseInt(element.ownText()));
      }
      catch (NumberFormatException ignored) {
        // just ignore
      }
    }

    // uniqueid tag
    Elements elements = root.select(root.tagName() + " > uniqueid");
    for (Element id : elements) {
      try {
        String key = id.attr("type");
        if (TMDB.equals(key)) {
          key = TMDB_SET;
        }
        String value = id.ownText();
        if (StringUtils.isNoneBlank(key, value)) {
          // check whether the id is an integer
          try {
            ids.put(key, MetadataUtil.parseInt(value));
          }
          catch (Exception e) {
            // store as string
            ids.put(key, value);
          }
        }
      }
      catch (Exception ignored) {
        // just ignore
      }
    }

    // iterate over our internal id store (new style)
    element = getSingleElement(root, "ids");
    if (element != null) {
      Elements children = element.children();
      for (Element entry : children) {
        if (StringUtils.isNoneBlank(entry.tagName(), entry.ownText())) {
          // check whether the id is an integer
          try {
            ids.put(entry.tagName(), MetadataUtil.parseInt(entry.ownText()));
          }
          catch (Exception e) {
            // store as string
            ids.put(entry.tagName(), entry.ownText());
          }
        }
      }
    }

    return null;
  }

  /**
   * tags usually come in a tag tag
   */
  private Void parseTags() {
    supportedElements.add("tag");

    Elements elements = root.select(root.tagName() + " > tag");
    for (Element element : elements) {
      if (StringUtils.isNotBlank(element.ownText())) {
        tags.add(element.ownText());
      }
    }

    return null;
  }

  /**
   * the user note is usually in the user_note tag
   */
  private Void parseUserNote() {
    supportedElements.add("user_note");

    Element element = getSingleElement(root, "user_note");
    if (element != null) {
      userNote = element.ownText();
    }
    return null;
  }

  /**
   * parse the genres tags<br />
   * - kodi has multiple genre tags<br />
   * - mediaportal as a nested genres tag
   */
  private Void parseGenres() {
    supportedElements.add("genres");
    supportedElements.add("genre");

    Elements elements = null;
    Element element = getSingleElement(root, "genres");
    if (element != null) {
      // nested genre tags
      elements = element.select(element.tagName() + " > genre");
    }
    else {
      // direct/multiple genre tags in movie root
      elements = root.select(root.tagName() + " > genre");
    }

    if (elements != null && !elements.isEmpty()) {
      for (Element genre : elements) {
        if (StringUtils.isNotBlank(genre.ownText())) {
          // old style - single tag with delimiter
          for (String sp : ParserUtils.split(genre.ownText())) {
            genres.add(MediaGenres.getGenre(sp.strip()));
          }
        }
      }
    }

    return null;
  }

  /**
   * studios come in two different flavors<br />
   * - kodi has multiple studio tags<br />
   * - mediaportal has all studios (comma separated) in one studio tag
   */
  private Void parseStudios() {
    supportedElements.add("studio");

    Elements elements = root.select(root.tagName() + " > studio");
    // if there is exactly one studio tag, split the studios at the comma
    if (elements.size() == 1) {
      try {
        studios.addAll(ParserUtils.split(elements.get(0).ownText()));
      }
      catch (Exception ignored) {
        // just ignore
      }
    }
    else {
      for (Element element : elements) {
        if (StringUtils.isNotBlank(element.ownText())) {
          studios.add(element.ownText());
        }
      }
    }

    return null;
  }

  /**
   * find and store all unsupported tags
   */
  private Void findUnsupportedElements() {
    // lockdata should not be re-written
    supportedElements.add("lockdata");

    // get all children of the root
    for (Element element : root.children()) {
      if (!supportedElements.contains(element.tagName().toLowerCase(Locale.ROOT))) {
        String elementText = element.toString().replaceAll(">\\r?\\n\\s*<", "><");
        unsupportedElements.add(elementText);
      }
    }

    return null;
  }

  /**
   * morph this instance to a movie object
   *
   * @return the movie Object
   */
  public MovieSet toMovieSet() {
    MovieSet movieSet = new MovieSet();
    movieSet.setTitle(title);
    movieSet.setPlot(plot);

    for (Map.Entry<String, Object> entry : ids.entrySet()) {
      if (entry.getKey().equalsIgnoreCase("tmdb")) {
        movieSet.setId(TMDB_SET, entry.getValue());
      }
      else {
        movieSet.setId(entry.getKey(), entry.getValue());
      }
    }

    movieSet.addToTags(tags);
    movieSet.setNote(userNote);

    return movieSet;
  }
}
