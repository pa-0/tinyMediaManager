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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSetArtworkHelper;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.movie.filenaming.IMovieSetFileNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetNfoNaming;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.util.ParserUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * this class is a general XML connector which suits as a base class for most xml based connectors
 *
 * @author Manuel Laggner
 */
public abstract class MovieSetGenericXmlConnector implements IMovieSetConnector {
  private static final Logger                 LOGGER                 = LoggerFactory.getLogger(MovieSetGenericXmlConnector.class);
  protected static final String               ORACLE_IS_STANDALONE   = "http://www.oracle.com/xml/is-standalone";
  protected static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols(Locale.US);

  protected final MovieSet                    movieSet;
  protected MovieNfoParser                    parser                 = null;

  protected Document                          document;
  protected Element                           root;

  protected MovieSetGenericXmlConnector(MovieSet movieSet) {
    this.movieSet = movieSet;
  }

  /**
   * write own tag which are not covered by this generic connector
   */
  protected abstract void addOwnTags();

  @Override
  public void write(List<MovieSetNfoNaming> nfoNames) {

    // first of all, get the data from a previous written NFO file,
    // if we do not want clean NFOs
    if (!MovieModuleManager.getInstance().getSettings().isWriteCleanNfo()) {
      for (MediaFile mf : movieSet.getMediaFiles(MediaFileType.NFO)) {
        try {
          parser = MovieNfoParser.parseNfo(mf.getFileAsPath());
          break;
        }
        catch (Exception ignored) {
          // just ignore
        }
      }
    }

    // remove old ones
    for (MediaFile oldNfo : movieSet.getMediaFiles(MediaFileType.NFO)) {
      Utils.deleteFileSafely(oldNfo.getFileAsPath());
    }

    List<MediaFile> newNfos = new ArrayList<>(1);

    for (MovieSetNfoNaming nfoNaming : nfoNames) {
      Path nfoPath = createNfoPath(movieSet, nfoNaming);

      if (nfoPath == null) {
        continue;
      }

      try {
        // create the new NFO file according to the specifications
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // NOSONAR
        document = factory.newDocumentBuilder().newDocument();
        document.setXmlStandalone(true);

        // tmm comment
        Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dat = formatter.format(new Date());
        document.appendChild(document.createComment("created on " + dat + " - tinyMediaManager " + Settings.getInstance().getVersion()));

        root = document.createElement("collection");
        document.appendChild(root);

        // add well known tags
        addTitle();
        // addOriginaltitle();
        // addRating();
        // addUserrating();
        // addVotes();
        addPlot();
        addThumb();
        addFanart();
        addIds();
        addGenres();
        addStudios();
        addTags();
        addDateAdded();
        addLockdata();

        // add connector specific tags
        addOwnTags();

        // add unsupported tags
        addUnsupportedTags();

        // add tinyMediaManagers own data
        addTinyMediaManagerTags();

        // serialize to string
        Writer out = new StringWriter();
        getTransformer().transform(new DOMSource(document), new StreamResult(out));
        String xml = out.toString().replaceAll("(?<!\r)\n", "\r\n"); // windows conform line endings

        if (!Files.isDirectory(nfoPath.getParent())) {
          Files.createDirectory(nfoPath.getParent());
        }

        // compare old vs new
        boolean changed = true;
        try {
          String xmlOld = Utils.readFileToString(nfoPath).replaceAll("\\<\\!\\-\\-.*\\-\\-\\>", ""); // replace xml comments
          String xmlNew = xml.replaceAll("\\<\\!\\-\\-.*\\-\\-\\>", "");
          if (xmlOld.equals(xmlNew)) {
            changed = false;
          }
        }
        catch (Exception e) {
          // ignore
        }

        // write to file
        if (changed) {
          Utils.writeStringToFile(nfoPath, xml);
        }
        else {
          LOGGER.trace("NFO did not change - do not write it!");
        }
        MediaFile mf = new MediaFile(nfoPath);
        mf.gatherMediaInformation(true); // force to update filedate
        newNfos.add(mf);
      }
      catch (Exception e) {
        LOGGER.error("write '" + nfoPath + "'", e);
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, movieSet, "message.nfo.writeerror", new String[] { ":", e.getLocalizedMessage() }));
      }
    }

    if (!newNfos.isEmpty()) {
      // remove orphaned NFO files (tmm style)
      List<MediaFile> existingNfos = movieSet.getMediaFiles(MediaFileType.NFO);
      for (MediaFile nfo : existingNfos) {
        if (!MovieSetConnectors.isValidNFO(nfo.getFileAsPath())) {
          // keep non tmm NFO files
          newNfos.add(nfo);
          continue;
        }

        if (!newNfos.contains(nfo)) {
          try {
            Utils.deleteFileWithBackup(nfo.getFileAsPath(), movieSet.getDataSource());
          }
          catch (Exception e) {
            LOGGER.debug("Could not remove orphaned NFO - '{}'", e.getMessage());
          }
        }
      }

      movieSet.removeAllMediaFiles(MediaFileType.NFO);
      movieSet.addToMediaFiles(newNfos);
    }
  }

  private Path createNfoPath(MovieSet movieSet, MovieSetNfoNaming nfoNaming) {
    String dataFolder = MovieModuleManager.getInstance().getSettings().getMovieSetDataFolder();
    if (StringUtils.isBlank(dataFolder)) {
      return null;
    }

    String movieSetName = MovieSetArtworkHelper.getMovieSetTitleForStorage(movieSet);

    String nfoFilename = nfoNaming.getFilename(movieSetName, "nfo");
    if (StringUtils.isBlank(nfoFilename)) {
      return null;
    }

    if (nfoNaming.getFolderLocation() == IMovieSetFileNaming.Location.KODI_STYLE_FOLDER) {
      // Kodi style: <movie set artwork folder>/<movie set name>/<movie set name>.nfo
      return Paths.get(dataFolder, movieSetName, nfoFilename);
    }
    else if (nfoNaming.getFolderLocation() == IMovieSetFileNaming.Location.AUTOMATOR_STYLE_FOLDER) {
      // Artwork Automator style: <movie set artwork folder>/<movie set name>.nfo
      return Paths.get(dataFolder, nfoFilename);
    }

    return null;
  }

  /**
   * add the title in the form <title>xxx</title>
   */
  protected void addTitle() {
    Element title = document.createElement("title");
    title.setTextContent(movieSet.getTitle());
    root.appendChild(title);
  }

  /**
   * add the originaltitle in the form <originaltitle>xxx</originaltitle>
   */
  protected void addOriginaltitle() {
    Element originaltitle = document.createElement("originaltitle");
    originaltitle.setTextContent(movieSet.getOriginalTitle());
    root.appendChild(originaltitle);
  }

  /**
   * add the rating in the form <rating>xxx</rating> (floating point with one decimal)
   */
  protected void addRating() {
    // get main rating and calculate the rating value to a base of 10
    Float rating10;

    // the default rating
    Map<String, MediaRating> ratings = movieSet.getRatings();

    MediaRating mainMediaRating = null;
    for (String ratingSource : MovieModuleManager.getInstance().getSettings().getRatingSources()) {
      mainMediaRating = ratings.get(ratingSource);
      if (mainMediaRating != null) {
        break;
      }
    }

    // is there any rating which is not the user rating?
    if (mainMediaRating == null) {
      for (MediaRating r : ratings.values()) {
        // skip user ratings here
        if (MediaRating.USER.equals(r.getId())) {
          continue;
        }
        mainMediaRating = r;
      }
    }

    // just create one to not pass null
    if (mainMediaRating == null) {
      mainMediaRating = MediaMetadata.EMPTY_RATING;
    }

    if (mainMediaRating.getMaxValue() > 0) {
      rating10 = mainMediaRating.getRating() * 10 / mainMediaRating.getMaxValue();
    }
    else {
      rating10 = mainMediaRating.getRating();
    }

    Element rating = document.createElement("rating");
    rating.setTextContent(String.format(Locale.US, "%.1f", rating10));
    root.appendChild(rating);
  }

  /**
   * add the userrating in the form <userrating>xxx</userrating> (floating point with at max one decimal)
   */
  protected void addUserrating() {
    // get main rating and calculate the rating value to a base of 10
    float rating10;

    MediaRating mediaRating = movieSet.getRating(MediaRating.USER);

    if (mediaRating.getMaxValue() > 0) {
      rating10 = mediaRating.getRating() * 10 / mediaRating.getMaxValue();
    }
    else {
      rating10 = mediaRating.getRating();
    }

    Element UserRating = document.createElement("userrating");
    DecimalFormat df = new DecimalFormat("#.#", DECIMAL_FORMAT_SYMBOLS);
    UserRating.setTextContent(df.format(rating10));
    root.appendChild(UserRating);
  }

  /**
   * add the votes in the form <votes>xxx</votes> (integer)
   */
  protected void addVotes() {
    Element votes = document.createElement("votes");
    votes.setTextContent(Integer.toString(movieSet.getRating().getVotes()));
    root.appendChild(votes);
  }

  /**
   * add the plot in the form <plot>xxx</plot>
   */
  protected void addPlot() {
    Element plot = document.createElement("plot");
    plot.setTextContent(movieSet.getPlot());
    root.appendChild(plot);
  }

  /**
   * add the thumb (poster) url in the form <thumb>xxx</thumb>
   */
  protected void addThumb() {
    Element thumb = document.createElement("thumb");
    thumb.setTextContent(movieSet.getArtworkUrl(MediaFileType.POSTER));
    root.appendChild(thumb);
  }

  /**
   * add the fanart url in the form <fanart>xxx</fanart>
   */
  protected void addFanart() {
    Element fanart = document.createElement("fanart");
    fanart.setTextContent(movieSet.getArtworkUrl(MediaFileType.FANART));
    root.appendChild(fanart);
  }

  /**
   * add our own id store in the new kodi form<br />
   * <uniqueid type="{scraper}" default="true/false">{id}</uniqueid>
   *
   * imdb should have default="true", but if no imdb ID is available, we must ensure that at least one entry has default="true"
   */
  protected void addIds() {
    String defaultScraper = detectDefaultScraper();

    for (Map.Entry<String, Object> entry : movieSet.getIds().entrySet()) {
      Element uniqueid = document.createElement("uniqueid");

      // write tmdb rather than tmdbSet
      String key = entry.getKey();
      if (TMDB_SET.equals(key)) {
        key = TMDB;
      }

      uniqueid.setAttribute("type", key);
      if (defaultScraper.equals(entry.getKey())) {
        uniqueid.setAttribute("default", "true");
      }
      else {
        uniqueid.setAttribute("default", "false");
      }
      uniqueid.setTextContent(entry.getValue().toString());
      root.appendChild(uniqueid);
    }
  }

  /**
   * add the dateAdded date in <dateadded>xxx</dateadded>
   */
  protected void addDateAdded() {
    Element dateadded = document.createElement("dateadded");
    switch (MovieModuleManager.getInstance().getSettings().getNfoDateAddedField()) {
      case DATE_ADDED:
        if (movieSet.getDateAdded() != null) {
          dateadded.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(movieSet.getDateAdded()));
        }
        break;

      case FILE_CREATION_DATE:
        MediaFile mainMediaFile = movieSet.getMainFile();
        if (mainMediaFile != null && mainMediaFile.getDateCreated() != null) {
          dateadded.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(mainMediaFile.getDateCreated()));
        }
        break;

      case FILE_LAST_MODIFIED_DATE:
        mainMediaFile = movieSet.getMainFile();
        if (mainMediaFile != null && mainMediaFile.getDateLastModified() != null) {
          dateadded.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(mainMediaFile.getDateLastModified()));
        }
        break;

    }
    root.appendChild(dateadded);
  }

  /**
   * write the <lockdata> tag (mainly for Emby)<br />
   * This will protect the NFO from being modified by Emby
   */
  protected void addLockdata() {
    if (MovieModuleManager.getInstance().getSettings().isNfoWriteLockdata()) {
      Element lockdata = document.createElement("lockdata");
      lockdata.setTextContent("true");

      root.appendChild(lockdata);
    }
  }

  /**
   * add genres in <genre>xxx</genre> tags (multiple)
   */
  protected void addGenres() {
    for (MediaGenres mediaGenre : movieSet.getGenres()) {
      Element genre = document.createElement("genre");
      genre.setTextContent(mediaGenre.getLocalizedName(MovieModuleManager.getInstance().getSettings().getNfoLanguage()));
      root.appendChild(genre);
    }
  }

  /**
   * add studios in <studio>xxx</studio> tags (multiple)
   */
  protected void addStudios() {
    List<String> studios = ParserUtils.split(movieSet.getProductionCompany());
    for (String s : studios) {
      Element studio = document.createElement("studio");
      studio.setTextContent(s);
      root.appendChild(studio);
    }
  }

  /**
   * add tags in <tag>xxx</tag> tags (multiple)
   */
  protected void addTags() {
    for (String t : movieSet.getTags()) {
      Element tag = document.createElement("tag");
      tag.setTextContent(t);
      root.appendChild(tag);
    }
  }

  /**
   * add all unsupported tags from the source file to the destination file
   */
  protected void addUnsupportedTags() {
    if (parser != null) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // NOSONAR

      for (String unsupportedString : parser.unsupportedElements) {
        try {
          Document unsupported = factory.newDocumentBuilder().parse(new ByteArrayInputStream(unsupportedString.getBytes(StandardCharsets.UTF_8)));
          root.appendChild(document.importNode(unsupported.getFirstChild(), true));
        }
        catch (Exception e) {
          LOGGER.error("import unsupported tags: {}", e.getMessage());
        }
      }
    }
  }

  /**
   * add the missing meta data for tinyMediaManager to this NFO
   */
  protected void addTinyMediaManagerTags() {
    root.appendChild(document.createComment("tinyMediaManager meta data"));
    addUserNote();
  }

  /**
   * add the user note in <user_note>xxx</user_note>
   */
  protected void addUserNote() {
    Element userNote = document.createElement("user_note");
    userNote.setTextContent(movieSet.getNote());
    root.appendChild(userNote);
  }

  /**
   * get any single element by the tag name
   * 
   * @param tag
   *          the tag name
   * @return an element or null
   */
  protected Element getSingleElementByTag(String tag) {
    NodeList nodeList = document.getElementsByTagName(tag);
    for (int i = 0; i < nodeList.getLength(); ++i) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        return (Element) node;
      }
    }
    return null;
  }

  /**
   * get the transformer for XML output
   * 
   * @return the transformer
   * @throws Exception
   *           any Exception that has been thrown
   */
  protected Transformer getTransformer() throws Exception {
    Transformer transformer = TransformerFactory.newInstance().newTransformer(); // NOSONAR

    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
    // not supported in all JVMs
    try {
      transformer.setOutputProperty(ORACLE_IS_STANDALONE, "yes");
    }
    catch (Exception ignored) {
      // okay, seems we're not on OracleJDK, OPenJDK or AdopOpenJDK
    }
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

    return transformer;
  }

  /**
   * try to detect the default scraper by the given ids
   *
   * @return the scraper where the default should be set
   */
  private String detectDefaultScraper() {
    // IMDB first
    if (movieSet.getIds().containsKey(MediaMetadata.IMDB)) {
      return MediaMetadata.IMDB;
    }

    // TMDB second
    if (movieSet.getIds().containsKey(MediaMetadata.TMDB)) {
      return MediaMetadata.TMDB;
    }

    // the first found as fallback
    return movieSet.getIds().keySet().stream().findFirst().orElse("");
  }
}
