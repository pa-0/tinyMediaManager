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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonNfoNaming;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * this class is a general XML connector which suits as a base class for most xml based connectors
 *
 * @author Manuel Laggner
 */
public abstract class TvShowSeasonGenericXmlConnector implements ITvShowSeasonConnector {
  private static final Logger     LOGGER               = LoggerFactory.getLogger(TvShowSeasonGenericXmlConnector.class);

  protected static final String   ORACLE_IS_STANDALONE = "http://www.oracle.com/xml/is-standalone";

  protected final TvShowSeason    tvShowSeason;
  protected TvShowSeasonNfoParser parser               = null;

  protected Document              document;
  protected Element               root;

  protected TvShowSeasonGenericXmlConnector(TvShowSeason tvShowSeason) {
    this.tvShowSeason = tvShowSeason;
  }

  /**
   * write own tag which are not covered by this generic connector
   */
  protected abstract void addOwnTags();

  @Override
  public void write(List<TvShowSeasonNfoNaming> nfoNames) {
    // first of all, get the data from a previous written NFO file,
    // if we do not want clean NFOs
    if (!TvShowModuleManager.getInstance().getSettings().isWriteCleanNfo()) {
      for (MediaFile mf : tvShowSeason.getMediaFiles(MediaFileType.NFO)) {
        try {
          parser = TvShowSeasonNfoParser.parseNfo(mf.getFileAsPath());
          break;
        }
        catch (Exception ignored) {
          // ignored
        }
      }
    }

    List<MediaFile> newNfos = new ArrayList<>(1);

    for (TvShowSeasonNfoNaming nfoNaming : nfoNames) {
      String nfoFilename = nfoNaming.getFilename(tvShowSeason, "nfo", false);
      if (StringUtils.isBlank(nfoFilename)) {
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

        root = document.createElement("season");
        document.appendChild(root);

        // add well known tags
        addSeasonNumber();
        addTitle();
        addShowTitle();
        addSortTitle();
        addYear();
        addPlot();
        addThumb();
        addFanart();
        addTvdbId();
        addImdbid();
        addTmdbid();
        addIds();
        addPremiered();
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

        Path f = tvShowSeason.getTvShow().getPathNIO().resolve(nfoFilename);

        // compare old vs new
        boolean changed = true;
        try {
          String xmlOld = Utils.readFileToString(f).replaceAll("\\<\\!\\-\\-.*\\-\\-\\>", ""); // replace xml comments
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
          Utils.writeStringToFile(f, xml);
        }
        else {
          LOGGER.debug("NFO {} did not change - do not write it!", f);
        }
        MediaFile mf = new MediaFile(f);
        mf.gatherMediaInformation(true); // force to update filedate
        newNfos.add(mf);
      }
      catch (Exception e) {
        LOGGER.error("write '" + tvShowSeason.getTvShow().getPathNIO().resolve(nfoFilename) + "'", e);
        MessageManager.instance.pushMessage(
            new Message(Message.MessageLevel.ERROR, tvShowSeason, "message.nfo.writeerror", new String[] { ":", e.getLocalizedMessage() }));
      }
    }

    if (!newNfos.isEmpty()) {
      // remove orphaned NFO files (tmm style)
      List<MediaFile> existingNfos = tvShowSeason.getMediaFiles(MediaFileType.NFO);
      for (MediaFile nfo : existingNfos) {
        if (!TvShowConnectors.isValidTvShowSeasonNFO(nfo.getFileAsPath())) {
          // keep non tmm NFO files
          newNfos.add(nfo);
          continue;
        }

        if (!newNfos.contains(nfo)) {
          try {
            Utils.deleteFileWithBackup(nfo.getFileAsPath(), tvShowSeason.getTvShow().getDataSource());
          }
          catch (Exception e) {
            LOGGER.debug("Could not remove orphaned NFO - '{}'", e.getMessage());
          }
        }
      }

      tvShowSeason.removeAllMediaFiles(MediaFileType.NFO);
      tvShowSeason.addToMediaFiles(newNfos);
    }
  }

  /**
   * add the season number in <seasonnumber>xxx</seasonnumber>
   */
  protected void addSeasonNumber() {
    Element seasonNumber = document.createElement("seasonnumber");
    seasonNumber.setTextContent(String.valueOf(tvShowSeason.getSeason()));
    root.appendChild(seasonNumber);
  }

  /**
   * add the title in the form <title>xxx</title>
   */
  protected void addTitle() {
    Element title = document.createElement("title");
    if (StringUtils.isNotBlank(tvShowSeason.getTitle())) {
      title.setTextContent(tvShowSeason.getTitle());
    }
    else {
      title.setTextContent(String.format("%s %d", TmmResourceBundle.getString("metatag.season"), tvShowSeason.getSeason()));
    }
    root.appendChild(title);
  }

  /**
   * add the showtitle in the form <showtitle>xxx</showtitle>
   */
  protected void addShowTitle() {
    Element title = document.createElement("showtitle");
    title.setTextContent(tvShowSeason.getTvShow().getTitle());
    root.appendChild(title);
  }

  /**
   * add the sorttitle in the form <sorttitle>xxx</sorttitle>
   */
  protected void addSortTitle() {
    Element sorttitle = document.createElement("sorttitle");
    if (StringUtils.isNotBlank(tvShowSeason.getTitle())) {
      sorttitle.setTextContent(tvShowSeason.getTitle());
    }
    else {
      sorttitle.setTextContent(String.format("%s %02d", TmmResourceBundle.getString("metatag.season"), tvShowSeason.getSeason()));
    }
    root.appendChild(sorttitle);
  }

  /**
   * add the year in the form <year>xxx</year>
   */
  protected void addYear() {
    Element year = document.createElement("year");

    int lowestYear = 0;

    for (TvShowEpisode episode : tvShowSeason.getEpisodesForDisplay()) {
      if (episode.getYear() < lowestYear) {
        lowestYear = episode.getYear();
      }
    }

    if (lowestYear > 0) {
      year.setTextContent(Integer.toString(lowestYear));
    }

    root.appendChild(year);
  }

  /**
   * add the plot in the form <plot>xxx</plot>
   */
  protected void addPlot() {
    Element plot = document.createElement("plot");
    plot.setTextContent(tvShowSeason.getPlot());
    root.appendChild(plot);
  }

  /**
   * add the artwork urls<br />
   *
   * <thumb aspect="poster">xxx</thumb> <br />
   * <thumb aspect="banner">xxx</thumb> <br />
   * <thumb aspect="landscape">xxx</thumb> <br />
   *
   * we will write all supported artwork types here
   */
  protected void addThumb() {
    addThumb(MediaFileType.SEASON_POSTER, "poster");
    addThumb(MediaFileType.SEASON_BANNER, "banner");
    addThumb(MediaFileType.SEASON_THUMB, "landscape");
  }

  private void addThumb(MediaFileType type, String aspect) {
    Element thumb = document.createElement("thumb");

    String artworkUrl = tvShowSeason.getArtworkUrl(type);
    if (StringUtils.isNotBlank(artworkUrl)) {
      thumb.setAttribute("aspect", aspect);
      thumb.setTextContent(artworkUrl);
      root.appendChild(thumb);
    }
  }

  /**
   * the new fanart in the form <fanart><thumb>xxx</thumb></fanart>
   */
  protected void addFanart() {
    Element fanart = document.createElement("fanart");

    Set<String> fanartUrls = new LinkedHashSet<>();

    // main fanart
    String fanartUrl = tvShowSeason.getArtworkUrl(MediaFileType.SEASON_FANART);
    if (StringUtils.isNotBlank(fanartUrl)) {
      fanartUrls.add(fanartUrl);
    }

    for (String url : fanartUrls) {
      Element thumb = document.createElement("thumb");
      thumb.setTextContent(url);
      fanart.appendChild(thumb);
    }

    if (!fanartUrls.isEmpty()) {
      root.appendChild(fanart);
    }
  }

  /**
   * add the TV show tvdb id in <tvdbid>xxx</tvdbid>
   */
  protected void addTvdbId() {
    Element id = document.createElement("tvdbid");
    id.setTextContent(tvShowSeason.getTvShow().getTvdbId());
    root.appendChild(id);
  }

  /**
   * add the TV show imdb id in <imdbid>xxx</imdbid>
   */
  protected void addImdbid() {
    Element imdbid = document.createElement("imdbid");
    imdbid.setTextContent(tvShowSeason.getTvShow().getImdbId());
    root.appendChild(imdbid);
  }

  /**
   * add the TV show tmdb id in <tmdbid>xxx</tmdbid>
   */
  protected void addTmdbid() {
    Element tmdbid = document.createElement("tmdbid");
    if (tvShowSeason.getTvShow().getTmdbId() > 0) {
      tmdbid.setTextContent(Integer.toString(tvShowSeason.getTvShow().getTmdbId()));
    }
    root.appendChild(tmdbid);
  }

  /**
   * add our own id store in the new kodi form<br />
   * <uniqueid type="{scraper}" default="true/false">{id}</uniqueid>
   *
   * imdb should have default="true", but if no imdb ID is available, we must ensure that at least one entry has default="true"
   */
  protected void addIds() {
    for (Map.Entry<String, Object> entry : tvShowSeason.getTvShow().getIds().entrySet()) {
      Element uniqueid = document.createElement("uniqueid");
      uniqueid.setAttribute("type", entry.getKey());
      uniqueid.setTextContent(entry.getValue().toString());
      root.appendChild(uniqueid);
    }
  }

  /**
   * add the premiered date in <premiered>xxx</premiered>
   */
  protected void addPremiered() {
    Element premiered = document.createElement("premiered");
    if (tvShowSeason.getFirstAired() != null) {
      premiered.setTextContent(new SimpleDateFormat("yyyy-MM-dd").format(tvShowSeason.getFirstAired()));
    }
    root.appendChild(premiered);
  }

  /**
   * write the <lockdata> tag (mainly for Emby)<br />
   * This will protect the NFO from being modified by Emby
   */
  protected void addLockdata() {
    if (TvShowModuleManager.getInstance().getSettings().isNfoWriteLockdata()) {
      Element lockdata = document.createElement("lockdata");
      lockdata.setTextContent("true");

      root.appendChild(lockdata);
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
   * add the missing metadata for tinyMediaManager to this NFO
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
    userNote.setTextContent(tvShowSeason.getNote());
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
}
