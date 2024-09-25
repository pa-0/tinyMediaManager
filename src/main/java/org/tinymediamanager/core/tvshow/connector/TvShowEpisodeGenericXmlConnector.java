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
import org.tinymediamanager.core.CertificationStyle;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.filenaming.TvShowEpisodeNfoNaming;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
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
public abstract class TvShowEpisodeGenericXmlConnector implements ITvShowEpisodeConnector {
  private static final Logger                 LOGGER                 = LoggerFactory.getLogger(TvShowEpisodeGenericXmlConnector.class);

  protected static final String               ORACLE_IS_STANDALONE   = "http://www.oracle.com/xml/is-standalone";
  protected static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols(Locale.US);

  protected final List<TvShowEpisode>         episodes;

  protected Document                          document;
  protected Element                           root;

  protected TvShowEpisodeGenericXmlConnector(List<TvShowEpisode> episodes) {
    this.episodes = episodes;
  }

  /**
   * write own tag which are not covered by this generic connector
   */
  protected abstract void addOwnTags(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser);

  @Override
  public void write(List<TvShowEpisodeNfoNaming> nfoNames) {
    if (episodes.isEmpty()) {
      return;
    }

    TvShowEpisode firstEpisode = episodes.get(0);
    TvShowEpisodeNfoParser parser = null;

    // first of all, get the data from a previous written NFO file,
    // if we do not want clean NFOs
    if (!TvShowModuleManager.getInstance().getSettings().isWriteCleanNfo()) {
      for (MediaFile mf : firstEpisode.getMediaFiles(MediaFileType.NFO)) {
        try {
          parser = TvShowEpisodeNfoParser.parseNfo(mf.getFileAsPath());
          break;
        }
        catch (Exception ignored) {
          // ignored
        }
      }
    }

    List<MediaFile> newNfos = new ArrayList<>(1);

    for (TvShowEpisodeNfoNaming nfoNaming : nfoNames) {
      String nfoFilename = firstEpisode.getNfoFilename(nfoNaming);
      if (StringUtils.isBlank(nfoFilename)) {
        continue;
      }

      try {
        boolean first = true;
        StringBuilder xmlString = new StringBuilder();

        // add well known tags
        for (TvShowEpisode episode : episodes) {
          // create the new NFO file according to the specifications
          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // NOSONAR
          document = factory.newDocumentBuilder().newDocument();
          document.setXmlStandalone(true);

          // tmm comment
          if (first) {
            Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dat = formatter.format(new Date());
            document.appendChild(document.createComment("created on " + dat + " - tinyMediaManager " + Settings.getInstance().getVersion()));
          }

          root = document.createElement("episodedetails");
          document.appendChild(root);

          // try to get the right episode out of the parser
          TvShowEpisodeNfoParser.Episode parserEpisode = null;
          if (parser != null) {
            for (TvShowEpisodeNfoParser.Episode ep : parser.episodes) {
              if (ep.season == episode.getSeason() && ep.episode == episode.getEpisode()) {
                parserEpisode = ep;
                break;
              }
            }
          }

          addTitle(episode, parserEpisode);
          addOriginalTitle(episode, parserEpisode);
          addShowTitle(episode, parserEpisode);
          addSeason(episode, parserEpisode);
          addEpisode(episode, parserEpisode);
          addDisplaySeason(episode, parserEpisode);
          addDisplayEpisode(episode, parserEpisode);
          addId(episode, parserEpisode);
          addIds(episode, parserEpisode);
          addRating(episode, parserEpisode);
          addUserrating(episode, parserEpisode);
          addVotes(episode, parserEpisode);
          addPlot(episode, parserEpisode);
          addRuntime(episode, parserEpisode);
          addThumb(episode, parserEpisode);
          addMpaa(episode, parserEpisode);
          addPremiered(episode, parserEpisode);
          addAired(episode, parserEpisode);
          addWatched(episode, parserEpisode);
          addPlaycount(episode, parserEpisode);
          addLastplayed(episode, parserEpisode);
          addStudios(episode, parserEpisode);
          addTags(episode, parserEpisode);
          addCredits(episode, parserEpisode);
          addDirectors(episode, parserEpisode);
          addActors(episode, parserEpisode);
          addTrailer(episode, parserEpisode);
          addDateAdded(episode, parserEpisode);
          addLockdata(episode, parserEpisode);

          // add connector specific tags
          addOwnTags(episode, parserEpisode);

          // add unsupported tags
          addUnsupportedTags(episode, parserEpisode);

          // add tinyMediaManagers own data
          addTinyMediaManagerTags(episode, parserEpisode);

          // serialize to string
          Writer out = new StringWriter();
          Transformer transformer = getTransformer();

          // suppress xml header on all episode but the first
          if (!first) {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
          }
          transformer.transform(new DOMSource(document), new StreamResult(out));

          xmlString.append(out.toString().replaceAll("(?<!\r)\n", "\r\n")); // windows conform line endings
          first = false;
        }

        String xml = xmlString.toString();
        Path f = firstEpisode.getPathNIO().resolve(nfoFilename);

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
          LOGGER.debug("NFO did not change - do not write it!");
        }

        MediaFile mf = new MediaFile(f);
        mf.gatherMediaInformation(true); // force to update filedate
        newNfos.add(mf);
      }
      catch (Exception e) {
        LOGGER.error("write '" + firstEpisode.getPathNIO().resolve(nfoFilename) + "'", e);
        MessageManager.instance.pushMessage(
            new Message(Message.MessageLevel.ERROR, firstEpisode, "message.nfo.writeerror", new String[] { ":", e.getLocalizedMessage() }));
      }
    }

    if (!newNfos.isEmpty()) {
      for (TvShowEpisode episode : episodes) {
        // remove orphaned NFO files (tmm style)
        List<MediaFile> existingNfos = episode.getMediaFiles(MediaFileType.NFO);
        for (MediaFile nfo : existingNfos) {
          if (!TvShowConnectors.isValidTvShowEpisodeNFO(nfo.getFileAsPath())) {
            // keep non tmm NFO files
            newNfos.add(nfo);
            continue;
          }

          if (!newNfos.contains(nfo)) {
            try {
              Utils.deleteFileWithBackup(nfo.getFileAsPath(), episode.getTvShow().getDataSource());
            }
            catch (Exception e) {
              LOGGER.debug("Could not remove orphaned NFO - '{}'", e.getMessage());
            }
          }
        }

        episode.removeAllMediaFiles(MediaFileType.NFO);
        episode.addToMediaFiles(newNfos);
      }
    }
  }

  /**
   * add the title in the form <title>xxx</title>
   */
  protected void addTitle(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element title = document.createElement("title");
    title.setTextContent(episode.getTitle());
    root.appendChild(title);
  }

  /**
   * add the original title in the form <originaltitle>xxx</originaltitle>
   */
  protected void addOriginalTitle(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element originaltitle = document.createElement("originaltitle");
    originaltitle.setTextContent(episode.getOriginalTitle());
    root.appendChild(originaltitle);
  }

  /**
   * add the showtitle in the form <showtitle>xxx</showtitle>
   */
  protected void addShowTitle(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element title = document.createElement("showtitle");
    title.setTextContent(episode.getTvShow().getTitle());
    root.appendChild(title);
  }

  /**
   * add the season in the form <season>xxx</season>
   */
  protected void addSeason(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element season = document.createElement("season");
    season.setTextContent(Integer.toString(episode.getSeason()));
    root.appendChild(season);
  }

  /**
   * add the episode in the form <episode>xxx</episode>
   */
  protected void addEpisode(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element episode1 = document.createElement("episode");
    episode1.setTextContent(Integer.toString(episode.getEpisode()));
    root.appendChild(episode1);
  }

  /**
   * add the display season in the form <displayseason>xxx</displayseason>, if set
   */
  protected void addDisplaySeason(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element displayseason = document.createElement("displayseason");
    if (episode.getDisplaySeason() >= 0 && episode.getDisplayEpisode() >= 0) {
      displayseason.setTextContent(Integer.toString(episode.getDisplaySeason()));
      root.appendChild(displayseason);
    }
  }

  /**
   * add the display episode in the form <displayepisode>xxx</displayepisode>, if set
   */
  protected void addDisplayEpisode(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element displayepisode = document.createElement("displayepisode");
    if (episode.getDisplaySeason() >= 0 && episode.getDisplayEpisode() >= 0) {
      displayepisode.setTextContent(Integer.toString(episode.getDisplayEpisode()));
      root.appendChild(displayepisode);
    }
  }

  /**
   * add the id (tvdb Id) in the form <id>xxx</id>
   */
  protected void addId(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element uniqueid = document.createElement("id");
    uniqueid.setTextContent(episode.getTvdbId());
    root.appendChild(uniqueid);
  }

  /**
   * add our own id store in the new kodi form<br />
   * <uniqueid type="{scraper}" default="true/false">{id}</uniqueid>
   *
   * imdb should have default="true", but if no imdb ID is available, we must ensure that at least one entry has default="true"
   */
  protected void addIds(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    String defaultScraper = detectDefaultScraper(episode);

    for (Map.Entry<String, Object> entry : episode.getIds().entrySet()) {
      Element uniqueid = document.createElement("uniqueid");
      uniqueid.setAttribute("type", entry.getKey());
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
   * add the rating in the form <rating>xxx</rating> (floating point with one decimal)
   */
  protected void addRating(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    // get main rating and calculate the rating value to a base of 10
    Float rating10;

    // the default rating
    Map<String, MediaRating> ratings = episode.getRatings();
    MediaRating mainMediaRating = ratings.get(TvShowModuleManager.getInstance().getSettings().getPreferredRating());

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
   * add the userrating in the form <userrating>xxx</userrating> (floating point with one decimal)
   */
  protected void addUserrating(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    // get main rating and calculate the rating value to a base of 10
    Float rating10;

    MediaRating mediaRating = episode.getRating(MediaRating.USER);

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
  protected void addVotes(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element votes = document.createElement("votes");
    votes.setTextContent(Integer.toString(episode.getRating().getVotes()));
    root.appendChild(votes);
  }

  /**
   * add the plot in the form <plot>xxx</plot>
   */
  protected void addPlot(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element plot = document.createElement("plot");
    plot.setTextContent(episode.getPlot());
    root.appendChild(plot);
  }

  /**
   * add the runtime in the form <runtime>xxx</runtime> (integer)
   */
  protected void addRuntime(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element runtime = document.createElement("runtime");

    // try to calculate the runtime from media files
    int runtimeInMinutes = episode.getRuntimeFromMediaFilesInMinutes();
    if (runtimeInMinutes == 0) {
      // fallback to runtime on TV show level
      runtimeInMinutes = episode.getRuntime();
    }

    runtime.setTextContent(Integer.toString(runtimeInMinutes));
    root.appendChild(runtime);
  }

  /**
   * add the thumb in the form <thumb>xxx</thumb> tags
   */
  protected void addThumb(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element thumb = document.createElement("thumb");

    String thumbUrl = episode.getArtworkUrl(MediaFileType.THUMB);
    if (StringUtils.isNotBlank(thumbUrl)) {
      thumb.setTextContent(thumbUrl);
      root.appendChild(thumb);
    }
  }

  /**
   * add the certification in <mpaa>xxx</mpaa>
   */
  protected void addMpaa(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element mpaa = document.createElement("mpaa");

    if (episode.getCertification() != null) {
      mpaa.setTextContent(CertificationStyle.formatCertification(episode.getCertification(),
          TvShowModuleManager.getInstance().getSettings().getCertificationStyle()));
    }
    root.appendChild(mpaa);
  }

  /**
   * add the premiered date in <premiered>xxx</premiered>
   */
  protected void addPremiered(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element premiered = document.createElement("premiered");
    if (episode.getFirstAired() != null) {
      premiered.setTextContent(new SimpleDateFormat("yyyy-MM-dd").format(episode.getFirstAired()));
    }
    root.appendChild(premiered);
  }

  /**
   * add the dateAdded date in <dateadded>xxx</dateadded>
   */
  protected void addDateAdded(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element dateadded = document.createElement("dateadded");
    switch (TvShowModuleManager.getInstance().getSettings().getNfoDateAddedField()) {
      case DATE_ADDED:
        if (episode.getDateAdded() != null) {
          dateadded.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(episode.getDateAdded()));
        }
        break;

      case FILE_CREATION_DATE:
        MediaFile mainMediaFile = episode.getMainFile();
        if (mainMediaFile != null && mainMediaFile.getDateCreated() != null) {
          dateadded.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(mainMediaFile.getDateCreated()));
        }
        break;

      case FILE_LAST_MODIFIED_DATE:
        mainMediaFile = episode.getMainFile();
        if (mainMediaFile != null && mainMediaFile.getDateLastModified() != null) {
          dateadded.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(mainMediaFile.getDateLastModified()));
        }
        break;

      case RELEASE_DATE:
        if (episode.getFirstAired() != null) {
          dateadded.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(episode.getFirstAired()));
        }
        else {
          // fall back to date added
          if (episode.getDateAdded() != null) {
            dateadded.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(episode.getDateAdded()));
          }
        }

    }
    root.appendChild(dateadded);
  }

  /**
   * write the <lockdata> tag (mainly for Emby)<br />
   * This will protect the NFO from being modified by Emby
   */
  protected void addLockdata(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    if (TvShowModuleManager.getInstance().getSettings().isNfoWriteLockdata()) {
      Element lockdata = document.createElement("lockdata");
      lockdata.setTextContent("true");

      root.appendChild(lockdata);
    }
  }

  /**
   * add the aired date in <aired>xxx</aired>
   */
  protected void addAired(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element premiered = document.createElement("aired");
    if (episode.getFirstAired() != null) {
      premiered.setTextContent(new SimpleDateFormat("yyyy-MM-dd").format(episode.getFirstAired()));
    }
    root.appendChild(premiered);
  }

  /**
   * add the watched flag in <watched>xxx</watched>
   */
  protected void addWatched(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element watched = document.createElement("watched");
    watched.setTextContent(Boolean.toString(episode.isWatched()));
    root.appendChild(watched);
  }

  /**
   * add the playcount in <playcount>xxx</playcount> (integer) we do not have this in tmm, but we might get it from an existing nfo
   */
  protected void addPlaycount(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element playcount = document.createElement("playcount");
    int playCountFromNFO = parser != null ? parser.playcount : 0;

    // take the higher value (tmm DB vs NFO)
    playCountFromNFO = Math.max(episode.getPlaycount(), playCountFromNFO);

    // if watched, make sure at least having a playcount of 1
    if (episode.isWatched()) {
      playCountFromNFO = Math.max(playCountFromNFO, 1);
    }

    playcount.setTextContent(Integer.toString(playCountFromNFO));
    root.appendChild(playcount);
  }

  /**
   * add the <lastplayed>xxx</lastplayed> we do not have this in tmm, but we might get it from an existing nfo
   */
  private void addLastplayed(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element lastplayed = document.createElement("lastplayed");

    // IF we have a (temp) date, write it
    if (episode.isWatched()) {
      if (episode.getLastWatched() != null) {
        lastplayed.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(episode.getLastWatched()));
        root.appendChild(lastplayed);
      }
      else {
        if (parser != null && parser.lastplayed != null) {
          lastplayed.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(parser.lastplayed));
          root.appendChild(lastplayed);
        }
      }
    }
  }

  /**
   * add studios in <studio>xxx</studio> tags (multiple)
   */
  protected void addStudios(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    for (String s : ParserUtils.split(episode.getProductionCompany())) {
      Element studio = document.createElement("studio");
      studio.setTextContent(s);
      root.appendChild(studio);

      // break here if we just want to write one studio
      if (TvShowModuleManager.getInstance().getSettings().isNfoWriteSingleStudio()) {
        break;
      }
    }
  }

  /**
   * add tags in <tag>xxx</tag> tags (multiple)
   */
  protected void addTags(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    for (String t : episode.getTags()) {
      Element tag = document.createElement("tag");
      tag.setTextContent(t);
      root.appendChild(tag);
    }
  }

  /**
   * add credits in <credits>xxx</credits> tags (mulitple)
   */
  protected void addCredits(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    for (Person writer : episode.getWriters()) {
      Element element = document.createElement("credits");
      element.setTextContent(writer.getName());

      addPersonIdsAsAttributes(element, writer);

      root.appendChild(element);
    }
  }

  /**
   * add directors in <director>xxx</director> tags (mulitple)
   */
  protected void addDirectors(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    for (Person director : episode.getDirectors()) {
      Element element = document.createElement("director");
      element.setTextContent(director.getName());

      addPersonIdsAsAttributes(element, director);

      root.appendChild(element);
    }
  }

  /**
   * add actors (guests) in <actor><name>xxx</name><role>xxx</role><thumb>xxx</thumb></actor>
   */
  protected void addActors(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    for (Person tvShowActor : episode.getActors()) {
      Element actor = document.createElement("actor");

      Element name = document.createElement("name");
      name.setTextContent(tvShowActor.getName());
      actor.appendChild(name);

      if (StringUtils.isNotBlank(tvShowActor.getRole())) {
        Element role = document.createElement("role");
        role.setTextContent(tvShowActor.getRole());
        actor.appendChild(role);
      }

      if (StringUtils.isNotBlank(tvShowActor.getThumbUrl())) {
        Element thumb = document.createElement("thumb");
        thumb.setTextContent(tvShowActor.getThumbUrl());
        actor.appendChild(thumb);
      }

      if (StringUtils.isNotBlank(tvShowActor.getProfileUrl())) {
        Element profile = document.createElement("profile");
        profile.setTextContent(tvShowActor.getProfileUrl());
        actor.appendChild(profile);
      }

      // save GuestStar information to NFO
      // https://emby.media/community/index.php?/topic/89268-actor-metadata-is-downloaded-only-for-the-people-that-tmdb-has-as-series-regulars/&do=findComment&comment=923528
      if (tvShowActor.getType() == Person.Type.GUEST) {
        Element profile = document.createElement("type");
        profile.setTextContent("GuestStar");
        actor.appendChild(profile);
      }

      addPersonIdsAsChildren(actor, tvShowActor);

      root.appendChild(actor);
    }
  }

  /**
   * add the trailer url in <trailer>xxx</trailer>
   */
  protected void addTrailer(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element trailer = document.createElement("trailer");
    if (parser != null && StringUtils.isNotBlank(parser.trailer)) {
      trailer.setTextContent(parser.trailer);
    }
    root.appendChild(trailer);
  }

  /**
   * add the missing meta data for tinyMediaManager to this NFO
   */
  protected void addTinyMediaManagerTags(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    root.appendChild(document.createComment("tinyMediaManager meta data"));
    addSource(episode, parser);
    addEdition(episode, parser);
    addOriginalFilename(episode, parser);
    addUserNote(episode, parser);
    addEpisodeGroups(episode, parser);
  }

  /**
   * add the media source <source>xxx</source>
   */
  protected void addSource(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element source = document.createElement("source");
    source.setTextContent(episode.getMediaSource().name());
    root.appendChild(source);
  }

  /**
   * add the edition in <edition>xxx</edition>
   */
  protected void addEdition(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element edition = document.createElement("edition");
    edition.setTextContent(episode.getEdition().name());
    root.appendChild(edition);
  }

  /**
   * add the original filename (which we picked up in tmm before renaming) in <original_filename>xxx</original_filename>
   */
  protected void addOriginalFilename(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element originalFilename = document.createElement("original_filename");
    if (StringUtils.isBlank(episode.getOriginalFilename())) {
      originalFilename.setTextContent(episode.getMainFile().getFilename());
    }
    else {
      originalFilename.setTextContent(episode.getOriginalFilename());
    }
    root.appendChild(originalFilename);
  }

  /**
   * add the user note in <user_note>xxx</user_note>
   */
  protected void addUserNote(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element userNote = document.createElement("user_note");
    userNote.setTextContent(episode.getNote());
    root.appendChild(userNote);
  }

  /**
   * add the episode group information <episode_groups>xxx</episode_groups>
   */
  protected void addEpisodeGroups(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element episodeGroups = document.createElement("episode_groups");

    for (MediaEpisodeNumber episodeNumber : episode.getEpisodeNumbers()) {
      Element group = document.createElement("group");
      group.setAttribute("id", episodeNumber.episodeGroup().getEpisodeGroupType().name());
      group.setAttribute("name", episodeNumber.episodeGroup().getName());
      group.setAttribute("season", String.valueOf(episodeNumber.season()));
      group.setAttribute("episode", String.valueOf(episodeNumber.episode()));
      episodeGroups.appendChild(group);
    }

    root.appendChild(episodeGroups);
  }

  /**
   * add all unsupported tags from the source file to the destination file
   */
  protected void addUnsupportedTags(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
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
  private String detectDefaultScraper(TvShowEpisode episode) {
    // IMDB first
    if (episode.getIds().containsKey(MediaMetadata.IMDB)) {
      return MediaMetadata.IMDB;
    }

    // TVDB second
    if (episode.getIds().containsKey(MediaMetadata.TVDB)) {
      return MediaMetadata.TVDB;
    }

    // TMDB third
    if (episode.getIds().containsKey(MediaMetadata.TMDB)) {
      return MediaMetadata.TMDB;
    }

    // the first found as fallback
    return episode.getIds().keySet().stream().findFirst().orElse("");
  }

  /**
   * add all well known ids for the given {@link Person} as XML attributes
   *
   * @param element
   *          the NFO {@link Element} to add the ids to
   * @param person
   *          the {@link Person} to get the ids from
   */
  protected void addPersonIdsAsAttributes(Element element, Person person) {
    // TMDB id
    int tmdbId = person.getIdAsInt(MediaMetadata.TMDB);
    if (tmdbId > 0) {
      element.setAttribute("tmdbid", String.valueOf(tmdbId));
    }

    // IMDB id
    String imdbId = person.getIdAsString(MediaMetadata.IMDB);
    if (StringUtils.isNotBlank(imdbId)) {
      element.setAttribute("imdbid", imdbId);
    }

    // TVDB id
    int tvdbId = person.getIdAsInt(MediaMetadata.TVDB);
    if (tvdbId > 0) {
      element.setAttribute("tvdbid", String.valueOf(tvdbId));
    }
  }

  /**
   * add all well known ids for the given {@link Person} as XML children
   *
   * @param element
   *          the NFO {@link Element} to add the ids to
   * @param person
   *          the {@link Person} to get the ids from
   */
  protected void addPersonIdsAsChildren(Element element, Person person) {
    // TMDB id
    int tmdbId = person.getIdAsInt(MediaMetadata.TMDB);
    if (tmdbId > 0) {
      Element id = document.createElement("tmdbid");
      id.setTextContent(String.valueOf(tmdbId));
      element.appendChild(id);
    }

    // IMDB id
    String imdbId = person.getIdAsString(MediaMetadata.IMDB);
    if (StringUtils.isNotBlank(imdbId)) {
      Element id = document.createElement("imdbid");
      id.setTextContent(imdbId);
      element.appendChild(id);
    }

    // TVDB id
    int tvdbId = person.getIdAsInt(MediaMetadata.TVDB);
    if (tvdbId > 0) {
      Element id = document.createElement("tvdbid");
      id.setTextContent(String.valueOf(tvdbId));
      element.appendChild(id);
    }
  }
}
