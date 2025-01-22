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
package org.tinymediamanager.scraper.ofdb;

import java.io.InterruptedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
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
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.interfaces.IMovieImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.StrgUtils;

/**
 * the class {@link OfdbMovieMetadataProvider} is used to gather meta data from ofdb
 * 
 * @author Manuel Laggner, Myron Boyle
 */
public class OfdbMovieMetadataProvider extends OfdbMetadataProvider
    implements IMovieMetadataProvider, IMovieImdbMetadataProvider, IMovieArtworkProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OfdbMovieMetadataProvider.class);

  @Override
  protected String getSubId() {
    return "movie";
  }

  @Override
  public MediaMetadata getMetadata(MovieSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata() {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    // we have 3 entry points here
    // a) getMetadata has been called from a previous search, get either url or ID
    // b) getMetadata has been called with an ofdbId - get detail page direct
    // c) getMetadata has been called with an imdbId - we need to search first!

    // https://www.ofdb.de/suchergebnis/?tt0499549
    // https://www.ofdb.de/film/188514,Avatar-Aufbruch-nach-Pandora/ just ID is relevant, title in url can be empty

    // trailers barely available and async loaded... search for "video_layer"

    String detailUrl = "";

    // case a)
    if (options.getSearchResult() != null) {
      detailUrl = options.getSearchResult().getUrl();
    }

    // case b)
    String ofdbId = options.getIdAsString(getId());
    if (detailUrl.isBlank() && StringUtils.isNotBlank(ofdbId)) {
      try {
        detailUrl = getApiKey() + "film/" + ofdbId;
      }
      catch (Exception e) {
        throw new ScrapeException(e);
      }
    }

    // case c)
    if (detailUrl.isBlank() && StringUtils.isNotBlank(options.getImdbId())) {
      try {
        SortedSet<MediaSearchResult> results = search(options);
        if (!results.isEmpty()) {
          options.setSearchResult(results.first());
          detailUrl = options.getSearchResult().getUrl();
        }
      }
      catch (Exception e) {
        LOGGER.warn("failed IMDB search: {}", e.getMessage());
      }
    }

    // we can only work further if we got a search result on ofdb.de
    if (StringUtils.isBlank(detailUrl)) {
      LOGGER.warn("We did not get any useful movie url");
      throw new MissingIdException(MediaMetadata.IMDB, getProviderInfo().getId());
    }

    MediaMetadata md = new MediaMetadata(getId());
    md.setScrapeOptions(options);

    // ID if not supplied
    if (StringUtils.isBlank(ofdbId)) {
      ofdbId = StrgUtils.substr(detailUrl, "film\\/(\\d+),");
    }
    md.setId(getId(), ofdbId);

    Document doc = null;
    LOGGER.trace("get details page: {}", detailUrl);
    try {
      Url u = new OnDiskCachedUrl(detailUrl, 1, TimeUnit.DAYS); // we need a forced cache here
      doc = Jsoup.parse(u.getInputStream(), "UTF-8", "");
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.error("could not fetch detail url: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (doc.getAllElements().size() < 10) {
      throw new ScrapeException(new Exception("we did not receive a valid web page"));
    }

    // **********************************************
    // parse main page
    // **********************************************

    // IMDB ID "http://www.imdb.com/title/tt1194173"
    Elements els = doc.getElementsByAttributeValueContaining("href", "imdb.com");
    if (!els.isEmpty()) {
      md.setId(MediaMetadata.IMDB, "tt" + StrgUtils.substr(els.first().attr("href"), "title/tt(\\d+)"));
    }

    // title / year
    // <h1 itemprop="name">Avatar - Aufbruch nach Pandora (2009)</h1>
    els = doc.getElementsByAttributeValue("itemprop", "name");
    if (!els.isEmpty()) {
      String[] ty = parseTitle(els.first().text());
      md.setTitle(StrgUtils.removeCommonSortableName(ty[0]));
      md.setYear(MetadataUtil.parseInt(ty[1], 0));
    }

    // FIXME: do testing with and w/o image chooser!

    // <img src="./images/film.370px/188/188514.jpg" class="img-max" itemprop="image"> ~370x530
    // <img src="./images/film/188/188514.jpg" class="img-max" itemprop="image"> ~500x700
    // <img src="./images/poster.370px/387/387637.jpg" class="img-max" itemprop="image">
    // NOTE:
    // having multiple sizes is a bit problematic in TMM, as only the previewed one is being added with correct dimension to ImageChooser dropdown
    // for all the others, we don't have any sizes, so the do not look good in GUI, and won't be choosen by automatic downloader
    // so we now just add the BEST image available (since we only have ONE poster and ONE fanart - this should be doable)
    Element el = doc.getElementsByAttributeValue("itemprop", "image").first();
    if (el != null) {
      String imgUrl = el.attr("src").replace("./", "");
      if (!imgUrl.isBlank()) {
        imgUrl = getApiKey() + imgUrl;
        MediaArtwork ma = new MediaArtwork(ID, MediaArtworkType.POSTER);
        int width = MetadataUtil.parseInt(StrgUtils.substr(imgUrl, "\\.(\\d+)px/"), 370);// usually 370px
        ma.setOriginalUrl(imgUrl.replace("." + width + "px", "")); // w/o px seems to be biggest
        ma.addImageSize(0, 0, imgUrl.replace("." + width + "px", ""), MediaArtwork.PosterSizes.BIG.getOrder());
        // ma.addImageSize(width, 530, imgUrl, MediaArtwork.PosterSizes.getSizeOrder(width));
        // ma.addImageSize(185, 278, imgUrl.replace("." + width + "px", ".185px"), MediaArtwork.PosterSizes.getSizeOrder(185));
        md.addMediaArt(ma);
      }
    }

    // <div class="header-moviedetail" id="HeaderFilmBild" style="background-image: url(./images/backdrop.870px/188/188514.jpg)">
    el = doc.getElementById("HeaderFilmBild");
    if (el != null) {
      String style = el.attr("style").replace("./", "");
      String imgUrl = getApiKey() + StrgUtils.substr(style, "\\((.*?)\\)"); // everything between CSS url parentheses
      MediaArtwork ma = new MediaArtwork(ID, MediaArtworkType.BACKGROUND);
      int width = MetadataUtil.parseInt(StrgUtils.substr(imgUrl, "\\.(\\d+)px/"), 870); // usually 870px
      ma.setOriginalUrl(imgUrl.replace("." + width + "px", ""));
      // ma.addImageSize(width, 0, imgUrl, MediaArtwork.FanartSizes.getSizeOrder(width));
      ma.addImageSize(0, 0, imgUrl.replace("." + width + "px", ""), MediaArtwork.FanartSizes.LARGE.getOrder());
      md.addMediaArt(ma);
    }

    els = doc.getElementsByAttributeValue("itemprop", "genre");
    for (Element genre : els) {
      md.addGenre(getTmmGenre(genre.text()));
    }

    els = doc.getElementsByAttributeValue("itemprop", "countryOfOrigin");
    for (Element cntr : els) {
      md.addCountry(cntr.text());
    }

    // An other weird quirk :(
    // usually you have well formed key/value pairs in form of dt/dd
    // but here we have some repeated DDs we need to skip/ignore....
    Element dl = doc.getElementsByClass("dl-horizontal").first();
    String lastTagName = "";
    String key = "";
    String val = "";
    for (Element e : dl.children()) {
      if (!e.tagName().equals(lastTagName)) {
        if (e.tagName().equalsIgnoreCase("dt")) {
          key = e.text();
        }
        else if (e.tagName().equalsIgnoreCase("dd")) {
          val = e.text();
          // if we set the val, we have already set the key, so HERE we have a valid key/value pair
          LOGGER.trace(" " + key + " | " + val);
          if (key.equals("Originaltitel:")) {
            md.setOriginalTitle(val);
          }
          else if (key.equals("Erscheinungsjahr:")) {
            if (md.getYear() == 0) {
              md.setYear(MetadataUtil.parseInt(val, 0));
            }
          }
          // else... other data already captured elswhere
        }
      }
      lastTagName = e.tagName();
    }

    // rating
    // does only work sometimes; seems to be an asnc call in Custom_film.min.js
    try {
      MediaRating rating = new MediaRating(ID);
      el = doc.getElementsByAttributeValue("itemprop", "ratingValue").first();
      if (el != null) {
        String r = el.text();
        if (!r.isEmpty()) {
          rating.setRating(Float.parseFloat(r));
          rating.setMaxValue(10);
        }
      }
      el = doc.getElementsByAttributeValue("itemprop", "ratingCount").first();
      if (el != null) {
        String r = el.attr("content");
        rating.setVotes(MetadataUtil.parseInt(r, 0));
      }
      md.addRating(rating);
    }
    catch (Exception e) {
      LOGGER.trace("could not parse rating: {}", e.getMessage());
    }

    // parse cutoff plot from main page, better than nothing
    el = doc.getElementsByAttributeValue("itemprop", "description").first();
    if (el != null) {
      md.setPlot(el.text());
    }
    // **********************************************
    // parse dedicated plot page
    // **********************************************
    // <a href="https://www.ofdb.de/film/188514,390613,Avatar-Aufbruch-nach-Pandora/plot/">Weiterlesen</a>
    LOGGER.trace("parse plot");
    el = doc.getElementsByAttributeValueMatching("href", "/plot/").first();
    if (el != null) {
      try {
        String plotUrl = el.attr("href");
        Url u = new OnDiskCachedUrl(plotUrl, 1, TimeUnit.DAYS); // we need a forced cache here
        Document plot = Jsoup.parse(u.getInputStream(), "UTF-8", "");
        el = plot.getElementsByTag("h4").first();
        String text = el.text().replace("...", "");
        el = el.nextElementSibling();
        text += el.text().replace("... ", "");
        md.setPlot(text);
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.error("failed to get plot page: {}", e.getMessage());
      }
    }

    doc = null;
    try {
      if (detailUrl.endsWith("/")) {
        detailUrl = detailUrl.substring(0, detailUrl.length() - 1);
      }
      String movieDetail = detailUrl + "/details";
      LOGGER.trace("parse movie detail: {}", movieDetail);
      Url u = new OnDiskCachedUrl(movieDetail, 1, TimeUnit.DAYS); // we need a forced cache here
      doc = Jsoup.parse(u.getInputStream(), "UTF-8", "");
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.error("failed to get detail page: {}", e.getMessage());
    }

    // **********************************************
    // parse detail page for actor/crew
    // **********************************************
    // https://www.ofdb.de/film/188514,Avatar-Aufbruch-nach-Pandora/details/
    el = doc.getElementsByClass("flagline-lg").first();
    if (el != null) {
      el = el.parent();
      // now we have the root node with all the actor DIVs, delimited by sporadically H5s...
      // parse them in a row...
      Person.Type type = Person.Type.OTHER;
      String crewRole = "";
      for (Element tag : el.children()) {
        if (tag.tagName().equals("h5")) {
          if (tag.text().contains("Regie")) {
            type = Person.Type.DIRECTOR;
            crewRole = "Regie";
          }
          else if (tag.text().contains("Darsteller") || tag.text().contains("Stimme/Sprecher")) // animation has no cast
          {
            type = Person.Type.ACTOR;
            crewRole = "";
          }
          else if (tag.text().contains("Drehbuchautor")) {
            type = Person.Type.WRITER;
            crewRole = "Drehbuchautor";
          }
          else if (tag.text().contains("Produzent")) {
            type = Person.Type.PRODUCER;
            crewRole = "Produzent";
          }
          else {
            type = Person.Type.OTHER;
            continue; // we usually do not save other crew members... as we have no place to display them yet.
            // Komponist(in)
            // Cutter (Schnitt)
            // Stunts
            // Second Unit-Regisseur(in)
            // Casting
            // Soundtrack
            // Stimme/Sprecher
            // Synchronstimme (deutsch)
          }
        }
        else if (tag.tagName().equals("div") && tag.attr("class").contains("row")) {
          // bit complicated here, but we have 2-3 different styles to parse
          Person p = new Person(type);
          el = tag.getElementsByTag("h4").first();
          if (el != null) {
            p.setName(el.text());
          }
          el = el.parent(); // a href
          if (!el.attr("href").equals("#")) {
            p.setProfileUrl(el.attr("href"));
            String id = StrgUtils.substr(el.attr("href"), "person/(\\d+),");
            p.setId(ID, id);
          }
          p.setRole(crewRole); // set our default; will be overwritten below, if other
          el = el.nextElementSibling(); // P with role (if avail)
          if (el != null) {
            p.setRole(el.ownText()); // but not surrounding tags
          }

          el = tag.getElementsByTag("img").first();
          if (el != null && !el.attr("src").contains("platzhalter")) {
            String imgUrl = getApiKey() + el.attr("src").replace("./", "");
            p.setThumbUrl(imgUrl);
          }

          md.addCastMember(p);
        }
      }
    }

    return md;
  }

  @Override
  public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options);
    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    SortedSet<MediaSearchResult> results = new TreeSet<>();

    String searchQuery = options.getSearchQuery();
    String imdb = options.getImdbId();
    Exception savedException = null;
    // https://www.ofdb.de/suchergebnis/?tt0499549 titel, personen/name, EAN nummer, IMDB Ids (tt, nm)

    // 1. do we have an OFDB id? Get metadata direct
    String ofdbId = options.getIdAsString(ID);
    if (ofdbId != null) {
      LOGGER.debug("found ofdbId {} - getting direct", ofdbId);
      MediaMetadata md = getMetadata(options);
      MediaSearchResult msr = new MediaSearchResult(ID, MediaType.MOVIE);
      msr.mergeFrom(md);
      msr.setScore(1.0f);
      results.add(msr);
      return results;
    }

    // 2. search with imdbId
    int count = 0;
    Element tbody = null;
    if (StringUtils.isNotEmpty(options.getImdbId())) {
      try {
        LOGGER.debug("search with imdbId: {}", imdb);
        Url u = new OnDiskCachedUrl(getApiKey() + "suchergebnis/?" + imdb, 1, TimeUnit.DAYS); // we need a forced cache here
        Document doc = Jsoup.parse(u.getInputStream(), "UTF-8", "");
        tbody = doc.getElementById("TabelleBody");
        count = tbody == null ? 0 : tbody.getElementsByTag("tr").size();
        LOGGER.debug("found {} search result", count);
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.error("failed to search for imdb Id {}: {}", imdb, e.getMessage());
        savedException = e;
      }
    }

    // 3. search for search string
    if (count == 0 && StringUtils.isNotBlank(options.getSearchQuery())) {
      try {
        LOGGER.debug("search for: {}", searchQuery);
        Url u = new OnDiskCachedUrl(getApiKey() + "suchergebnis/?" + URLEncoder.encode(cleanSearch(searchQuery), StandardCharsets.UTF_8), 1,
            TimeUnit.DAYS); // we need a forced cache here
        Document doc = Jsoup.parse(u.getInputStream(), "UTF-8", "");
        tbody = doc.getElementById("TabelleBody");
        count = tbody == null ? 0 : tbody.getElementsByTag("tr").size();
        LOGGER.debug("found {} search results", count);
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.error("failed to search for: {} - {}", searchQuery, e.getMessage());
        savedException = e;
      }
    }

    // if there has been a saved exception and we did not find anything - throw the exception
    if (count == 0 && savedException != null) {
      throw new ScrapeException(savedException);
    }
    if (count == 0) {
      LOGGER.debug("nothing found :(");
      return results;
    }

    // <tr>
    // <td>Match%</td>
    // <td>Titel/Name</td>
    // <td>Jahr</td>
    // </tr>
    for (Element row : tbody.getElementsByTag("tr")) {
      try {
        Elements cols = row.getElementsByTag("td");
        if (cols.size() == 3) {
          MediaSearchResult sr = new MediaSearchResult(ID, MediaType.MOVIE);

          // https://www.ofdb.de/film/188514,Avatar-Aufbruch-nach-Pandora
          String url = cols.get(1).getElementsByTag("a").first().attr("href");
          sr.setUrl(url);
          String id = StrgUtils.substr(url, "film/(\\d+),");
          if (id.isBlank()) {
            LOGGER.info("ignoring non-movie result: {}", url);
            continue;
          }
          sr.setId(ID, id);

          String yearText = cols.get(2).text();
          if (!yearText.isBlank()) {
            int year = MetadataUtil.parseInt(yearText);
            sr.setYear(year);
          }

          // weird HTML :/ tag in attr
          // <span class="tooltipster" title="<img ... src='./images/film.185px/188/188514.jpg'>">Avatar - Aufbruch nach Pandora</span>
          Element span = cols.get(1).getElementsByClass("tooltipster").first();
          String title = span.text();
          sr.setTitle(title);
          String img = span.attr("title");
          String imgUrl = StrgUtils.substr(img, "src='\\.\\/(.*?)\\'");
          if (imgUrl != null && !imgUrl.isBlank()) {
            sr.setPosterUrl(getApiKey() + imgUrl);
          }

          // use scraper score: 87% -> 0.87
          NumberFormat defaultFormat = new DecimalFormat();
          Number score = defaultFormat.parse(cols.get(0).text());
          sr.setScore(score.floatValue() / 100);

          results.add(sr);
        }
        else {
          LOGGER.trace("unexpected columns - {}", cols);
        }
      }
      catch (Exception e) {
        LOGGER.warn("error parsing movie result: {}", e.getMessage());
      }
    }

    return results;
  }

  /**
   * return a 2 element array. 0 = title; 1=date
   * <p>
   * parses the title in the format Title YEAR or Title (YEAR)
   *
   * @param title
   *          the title
   * @return the string[]
   */
  private String[] parseTitle(String title) {
    String v[] = { "", "" };
    if (title == null)
      return v;

    Pattern p = Pattern.compile("(.*)\\s+\\(?([0-9]{4})\\)?", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(title);
    if (m.find()) {
      v[0] = m.group(1);
      v[1] = m.group(2);
    }
    else {
      v[0] = title;
    }
    return v;
  }

  /*
   * Maps scraper Genres to internal TMM genres
   */
  private MediaGenres getTmmGenre(String genre) {
    MediaGenres g = null;
    if (genre.isEmpty()) {
      return g;
    }
    // @formatter:off
    else if (genre.equals("Abenteuer")) {
      g = MediaGenres.ADVENTURE;
    } else if (genre.equals("Action")) {
      g = MediaGenres.ACTION;
    } else if (genre.equals("Amateur")) {
      g = MediaGenres.INDIE;
    } else if (genre.equals("Animation")) {
      g = MediaGenres.ANIMATION;
    } else if (genre.equals("Anime")) {
      g = MediaGenres.ANIME;
    } else if (genre.equals("Biographie")) {
      g = MediaGenres.BIOGRAPHY;
    } else if (genre.equals("Dokumentation")) {
      g = MediaGenres.DOCUMENTARY;
    } else if (genre.equals("Drama")) {
      g = MediaGenres.DRAMA;
    } else if (genre.equals("Eastern")) {
      g = MediaGenres.EASTERN;
    } else if (genre.equals("Erotik")) {
      g = MediaGenres.EROTIC;
    } else if (genre.equals("Essayfilm")) {
      g = MediaGenres.INDIE;
    } else if (genre.equals("Experimentalfilm")) {
      g = MediaGenres.INDIE;
    } else if (genre.equals("Fantasy")) {
      g = MediaGenres.FANTASY;
    } else if (genre.equals("Grusel")) {
      g = MediaGenres.HORROR;
    } else if (genre.equals("Hardcore")) {
      g = MediaGenres.EROTIC;
    } else if (genre.equals("Heimatfilm")) {
      g = MediaGenres.TV_MOVIE;
    } else if (genre.equals("Historienfilm")) {
      g = MediaGenres.HISTORY;
    } else if (genre.equals("Horror")) {
      g = MediaGenres.HORROR;
    } else if (genre.equals("Kampfsport")) {
      g = MediaGenres.SPORT;
    } else if (genre.equals("Katastrophen")) {
      g = MediaGenres.DISASTER;
    } else if (genre.equals("Kinder-/Familienfilm")) {
      g = MediaGenres.FAMILY;
    } else if (genre.equals("Komödie")) {
      g = MediaGenres.COMEDY;
    } else if (genre.equals("Krieg")) {
      g = MediaGenres.WAR;
    } else if (genre.equals("Krimi")) {
      g = MediaGenres.CRIME;
    } else if (genre.equals("Kurzfilm")) {
      g = MediaGenres.SHORT;
    } else if (genre.equals("Liebe/Romantik")) {
      g = MediaGenres.ROMANCE;
    } else if (genre.equals("Mondo")) {
      g = MediaGenres.DOCUMENTARY;
    } else if (genre.equals("Musikfilm")) {
      g = MediaGenres.MUSIC;
    } else if (genre.equals("Mystery")) {
      g = MediaGenres.MYSTERY;
    } else if (genre.equals("Science-Fiction")) {
      g = MediaGenres.SCIENCE_FICTION;
    } else if (genre.equals("Serial")) {
      g = MediaGenres.SERIES;
    } else if (genre.equals("Sex")) {
      g = MediaGenres.EROTIC;
    } else if (genre.equals("Splatter")) {
      g = MediaGenres.HORROR;
    } else if (genre.equals("Sportfilm")) {
      g = MediaGenres.SPORT;
    } else if (genre.equals("Stummfilm")) {
      g = MediaGenres.SILENT_MOVIE;
    } else if (genre.equals("TV-Film")) {
      g = MediaGenres.TV_MOVIE;
    } else if (genre.equals("TV-Mini-Serie")) {
      g = MediaGenres.SERIES;
    } else if (genre.equals("TV-Pilotfilm")) {
      g = MediaGenres.TV_MOVIE;
    } else if (genre.equals("TV-Serie")) {
      g = MediaGenres.SERIES;
    } else if (genre.equals("Thriller")) {
      g = MediaGenres.THRILLER;
    } else if (genre.equals("Tierfilm")) {
      g = MediaGenres.ANIMAL;
    } else if (genre.equals("Webminiserie")) {
      g = MediaGenres.SERIES;
    } else if (genre.equals("Webserie")) {
      g = MediaGenres.SERIES;
    } else if (genre.equals("Western")) {
      g = MediaGenres.WESTERN;
    }
    if (g == null) {
      g = MediaGenres.getGenre(genre);
    }
    return g;
  }

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException, MissingIdException {
    MovieSearchAndScrapeOptions movie = new MovieSearchAndScrapeOptions();
    movie.setDataFromOtherOptions(options);
    MediaMetadata md = getMetadata(movie);
    return md.getMediaArt(options.getArtworkType());
  }
}
