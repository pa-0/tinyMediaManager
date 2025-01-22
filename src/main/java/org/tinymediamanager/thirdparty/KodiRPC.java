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

package org.tinymediamanager.thirdparty;

import java.io.File;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.jsonrpc.api.AbstractCall;
import org.tinymediamanager.jsonrpc.api.call.Application;
import org.tinymediamanager.jsonrpc.api.call.AudioLibrary;
import org.tinymediamanager.jsonrpc.api.call.Files;
import org.tinymediamanager.jsonrpc.api.call.System;
import org.tinymediamanager.jsonrpc.api.call.VideoLibrary;
import org.tinymediamanager.jsonrpc.api.model.ApplicationModel;
import org.tinymediamanager.jsonrpc.api.model.FilesModel;
import org.tinymediamanager.jsonrpc.api.model.GlobalModel;
import org.tinymediamanager.jsonrpc.api.model.ListModel;
import org.tinymediamanager.jsonrpc.api.model.VideoModel;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.EpisodeDetail;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.EpisodeFields;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.MovieDetail;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.MovieFields;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.TVShowDetail;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.TVShowFields;
import org.tinymediamanager.jsonrpc.config.HostConfig;
import org.tinymediamanager.jsonrpc.io.ApiCallback;
import org.tinymediamanager.jsonrpc.io.ApiException;
import org.tinymediamanager.jsonrpc.io.ConnectionListener;
import org.tinymediamanager.jsonrpc.io.JavaConnectionManager;
import org.tinymediamanager.jsonrpc.io.JsonApiRequest;
import org.tinymediamanager.jsonrpc.notification.AbstractEvent;
import org.tinymediamanager.scraper.util.StrgUtils;

public class KodiRPC {
  private static final Logger         LOGGER            = LoggerFactory.getLogger(KodiRPC.class);
  private static KodiRPC              instance;
  private static final String         SEPARATOR_REGEX   = "[\\/\\\\]+";

  private final JavaConnectionManager connectionManager = new JavaConnectionManager();

  private final Map<String, String>   videodatasources  = new LinkedHashMap<>();                 // dir, label
  private final List<String>          audiodatasources  = new ArrayList<>();

  // TMM DbId-to-KodiId mappings
  private final Map<UUID, Integer>    moviemappings     = new HashMap<>();
  private final Map<UUID, Integer>    tvshowmappings    = new HashMap<>();
  private final Map<UUID, Integer>    episodemappings   = new HashMap<>();                       // on demand

  private String                      kodiVersion       = "";

  private KodiRPC() {
    connectionManager.registerConnectionListener(new ConnectionListener() {

      @Override
      public void notificationReceived(AbstractEvent event) {
        LOGGER.debug("Event received: {}", event);
      }

      @Override
      public void disconnected() {
        LOGGER.info("Event: Disconnected");
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.INFO, "Kodi disconnected"));
      }

      @Override
      public void connected() {
        LOGGER.info("Event: Connected to {}", connectionManager.getHostConfig().getAddress());
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.INFO, "Kodi connected"));
      }
    });
  }

  public static synchronized KodiRPC getInstance() {
    if (instance == null) {
      instance = new KodiRPC();
    }

    return instance;
  }

  public boolean isConnected() {
    return connectionManager.isConnected();
  }

  // -----------------------------------------------------------------------------------

  /**
   * gets the Kodi version (cached on connect)
   * 
   * @return
   */
  public String getVersion() {
    return "Kodi " + kodiVersion;
  }

  // -----------------------------------------------------------------------------------

  public void cleanVideoLibrary() {
    final VideoLibrary.Clean call = new VideoLibrary.Clean(true);
    sendWoResponse(call);
  }

  public void scanVideoLibrary() {
    final VideoLibrary.Scan call = new VideoLibrary.Scan(null, true);
    sendWoResponse(call);
  }

  public void scanVideoLibrary(String dir) {
    final VideoLibrary.Scan call = new VideoLibrary.Scan(dir, true);
    sendWoResponse(call);
  }

  public Map<String, String> getVideoDataSources() {
    return this.videodatasources;
  }

  private void getAndSetVideoDataSources() {
    final Files.GetSources call = new Files.GetSources(FilesModel.Media.VIDEO); // movies + tv !!!
    send(call);
    if (call.getResults() != null && !call.getResults().isEmpty()) {
      this.videodatasources.clear();
      try {
        for (ListModel.SourceItem res : call.getResults()) {
          LOGGER.debug("Kodi datasource: {}", res.file);
          if (res.file.startsWith("multipath")) {
            // more than one source mapped to a single Kodi datasource
            // multipath://%2fmedia%2f8TB%2fFilme%2fKino%2f/%2fmedia%2fWD-4TB%2f!Kino2%2f/
            String mp = res.file.replace("multipath://", ""); // remove prefix
            String[] source = mp.split("/"); // split on slash
            for (String ds : source) {
              String s = URLDecoder.decode(ds, "UTF-8");
              this.videodatasources.put(s, res.label);
            }
          }
          else {
            this.videodatasources.put(res.file, res.label);
          }
        }
      }
      catch (Exception e) {
        LOGGER.debug("could not process Kodi RPC response - '{}'", e.getMessage());
      }
    }
  }

  private String detectDatasource(String file) {
    for (String ds : this.videodatasources.keySet()) {
      if (file.startsWith(ds)) {
        return ds;
      }
    }
    return "";
  }

  /**
   * builds the moviemappings: DBid -> Kodi ID
   */
  protected void getAndSetMovieMappings() {
    final VideoLibrary.GetMovies call = new VideoLibrary.GetMovies(MovieFields.FILE);
    send(call);
    if (call.getResults() != null && !call.getResults().isEmpty()) {
      moviemappings.clear();

      // KODI ds|file=id
      Map<String, Integer> kodiDsAndFolder = new HashMap<>();
      for (MovieDetail movie : call.getResults()) {
        if (movie.file == null || movie.file.isEmpty()) {
          continue;
        }

        // stacking only supported on movies
        if (movie.file.startsWith("stack")) {
          String[] files = movie.file.split(" , ");
          for (String s : files) {
            s = s.replaceFirst("^stack://", "");
            String ds = detectDatasource(s);
            String rel = s.replace(ds, ""); // remove ds, to have a relative folder
            rel = rel.replaceAll(SEPARATOR_REGEX, "/"); // normalize separators
            ds = ds.replaceAll(SEPARATOR_REGEX + "$", ""); // replace ending separator
            ds = ds.replaceAll(".*" + SEPARATOR_REGEX, ""); // replace everything till last separator
            if (!kodiDsAndFolder.containsKey(rel)) {
              kodiDsAndFolder.put(rel, movie.movieid);
            }
            else {
              // no putIfAbsent since i wanna have a log!
              LOGGER.warn("Kodi movie {} already attached to another datasource - skipping", rel);
            }
          }
        }
        else {
          // Kodi return full path of video file
          String ds = detectDatasource(movie.file); // detect datasource of show dir
          String rel = movie.file.replace(ds, ""); // remove ds, to have a relative folder
          rel = rel.replaceAll(SEPARATOR_REGEX, "/"); // normalize separators
          ds = ds.replaceAll(SEPARATOR_REGEX + "$", ""); // replace ending separator
          ds = ds.replaceAll(".*" + SEPARATOR_REGEX, ""); // replace everything till last separator
          if (!kodiDsAndFolder.containsKey(rel)) {
            kodiDsAndFolder.put(rel, movie.movieid);
          }
          else {
            // no putIfAbsent since i wanna have a log!
            LOGGER.warn("Kodi movie {} already attached to another datasource - skipping", rel);
          }
        }
      }
      LOGGER.debug("KODI {} movies", call.getResults().size()); // stacked movies are multiple times in here

      // TMM ds|dir=id
      Map<String, UUID> tmmDsAndFolder = prepareMovieFileMap(MovieModuleManager.getInstance().getMovieList().getMovies());
      LOGGER.debug("TMM {} movies", tmmDsAndFolder.size());

      // map em'
      for (Map.Entry<String, UUID> entry : tmmDsAndFolder.entrySet()) {
        String key = entry.getKey();
        UUID value = entry.getValue();
        Integer kodiId = kodiDsAndFolder.get(key);
        if (kodiId != null && kodiId > 0) {
          // we have a match!
          moviemappings.put(value, kodiId);
        }
        else {
          LOGGER.trace("Could not map: {}", key);
        }
      }
      LOGGER.debug("mapped {} movies", moviemappings.size());
    }
  }

  private Map<String, UUID> prepareMovieFileMap(List<Movie> movies) {
    Map<String, UUID> fileMap = new HashMap<>();
    for (Movie movie : movies) {
      fileMap.putAll(parseEntity(movie, movie.isDisc()));
    }
    return fileMap;
  }

  private Map<String, UUID> prepareEpisodeFileMap(TvShow show) {
    Map<String, UUID> fileMap = new HashMap<>();
    for (TvShowEpisode ep : show.getEpisodes()) {
      fileMap.putAll(parseEntity(ep, ep.isDisc()));
    }
    return fileMap;
  }

  private String parseDatasourceName(Path ds) {
    // get the name of the datasource folder
    // unfortunately, for UNC paths like \\server\share i cannot get the share name from Path
    // and URI is so slow
    String dsName = "";
    if (ds.getFileName() != null) {
      dsName = ds.getFileName().toString();
    }
    else {
      // try with good old file, which is not so bitchy
      File f = ds.toFile();
      dsName = f.getName();
    }
    if (dsName.isEmpty()) {
      // happens when only a drive letter like M:\ is set - return 1:1
      dsName = ds.toString();
    }
    return dsName;
  }

  private Map<String, UUID> parseEntity(MediaEntity entity, boolean isDisc) {
    Map<String, UUID> fileMap = new HashMap<>();
    Path ds = Paths.get(entity.getDataSource());
    if (ds == null || ds.toString().isBlank()) {
      LOGGER.warn("Datasource was null? Ignoring {}", entity.toString());
      return fileMap;
    }

    MediaFile main = entity.getMainFile();
    if (isDisc) {
      // Kodi RPC sends what we call the main disc identifier, but we have disc folder only
      for (MediaFile mf : entity.getMediaFiles(MediaFileType.VIDEO)) {

        Path file = null;
        // append MainDiscIdentifier to our folder MF
        if (mf.getFilename().equalsIgnoreCase(MediaFileHelper.VIDEO_TS)) {
          file = mf.getFileAsPath().resolve("VIDEO_TS.IFO");
        }
        else if (mf.getFilename().equalsIgnoreCase(MediaFileHelper.HVDVD_TS)) {
          file = mf.getFileAsPath().resolve("HV000I01.IFO");
        }
        else if (mf.getFilename().equalsIgnoreCase(MediaFileHelper.BDMV)) {
          file = mf.getFileAsPath().resolve("index.bdmv");
        }
        else if (mf.isMainDiscIdentifierFile()) {
          // just add MainDiscIdentifier
          file = mf.getFileAsPath();
        }

        if (file != null) {
          String rel = Utils.relPath(ds, file); // file relative from datasource
          rel = rel.replaceAll(SEPARATOR_REGEX, "/"); // normalize separators
          if (!fileMap.containsKey(rel)) {
            fileMap.put(rel, entity.getDbId());
          }
          else {
            // no putIfAbsent since i wanna have a log!
            LOGGER.warn("File {} already attached to another datasource - skipping", rel);
          }
        }
      }
    }
    else {
      String rel = Utils.relPath(ds, main.getFileAsPath()); // file relative from datasource
      rel = rel.replaceAll(SEPARATOR_REGEX, "/"); // normalize separators
      if (!fileMap.containsKey(rel)) {
        fileMap.put(rel, entity.getDbId());
      }
      else {
        // no putIfAbsent since i wanna have a log!
        LOGGER.warn("File {} already attached to another datasource - skipping", rel);
      }
    }
    return fileMap;
  }

  /**
   * builds the show/episode mappings: DBid -> Kodi ID
   */
  protected void getAndSetTvShowMappings() {
    final VideoLibrary.GetTVShows tvShowCall = new VideoLibrary.GetTVShows(TVShowFields.FILE);
    send(tvShowCall);
    if (tvShowCall.getResults() != null && !tvShowCall.getResults().isEmpty()) {
      tvshowmappings.clear();
      episodemappings.clear();

      // KODI ds|dir=id
      Map<String, Integer> kodiDsAndFolder = new HashMap<>();
      for (TVShowDetail show : tvShowCall.getResults()) {
        if (show.file == null || show.file.isEmpty()) {
          continue;
        }
        // Kodi return full path of show dir
        String ds = detectDatasource(show.file); // detect datasource of show dir
        String rel = show.file.replace(ds, ""); // remove ds, to have a relative folder
        rel = rel.replaceAll(SEPARATOR_REGEX + "$", ""); // remove ending separator
        rel = rel.replaceAll(SEPARATOR_REGEX, "/"); // normalize separators
        ds = ds.replaceAll(SEPARATOR_REGEX + "$", ""); // replace ending separator
        ds = ds.replaceAll(".*" + SEPARATOR_REGEX, ""); // replace everything till last separator
        if (!kodiDsAndFolder.containsKey(rel)) {
          kodiDsAndFolder.put(rel, show.tvshowid);
        }
        else {
          // no putIfAbsent since i wanna have a log!
          LOGGER.warn("Kodi show {} already attached to another datasource - skipping", rel);
        }
      }
      LOGGER.debug("KODI {} shows", kodiDsAndFolder.size());

      // TMM ds|dir=id
      LOGGER.debug("TMM {} shows", TvShowModuleManager.getInstance().getTvShowList().getTvShows().size());
      for (TvShow tmmShow : TvShowModuleManager.getInstance().getTvShowList().getTvShows()) {
        try {
          Path ds = Paths.get(tmmShow.getDataSource());
          String rel = Utils.relPath(ds, tmmShow.getPathNIO());
          rel = rel.replaceAll(SEPARATOR_REGEX, "/"); // normalize separators

          Integer kodiId = kodiDsAndFolder.get(rel);
          if (kodiId != null && kodiId > 0) {
            // we have a match!
            tvshowmappings.put(tmmShow.getDbId(), kodiId);
          }
          else {
            LOGGER.trace("Could not map: {}", rel);
          }
        }
        catch (Exception e) {
          LOGGER.error("Error mapping TvShow: {} on {}", e.getMessage(), tmmShow);
        }
      }
      LOGGER.debug("mapped {} shows", tvshowmappings.size());
    }
  }

  public void refreshFromNfo(Movie movie) {
    Integer kodiID = moviemappings.get(movie.getDbId());

    if (kodiID != null) {
      List<MediaFile> nfo = movie.getMediaFiles(MediaFileType.NFO);
      if (!nfo.isEmpty()) {
        LOGGER.info("Refreshing from NFO: {}", nfo.get(0).getFileAsPath());
      }
      else {
        LOGGER.error("No NFO file found to refresh! {}", movie.getTitle());
        // we do NOT return here, maybe Kodi will do something even w/o nfo...
      }

      final VideoLibrary.RefreshMovie call = new VideoLibrary.RefreshMovie(kodiID, false); // always refresh from NFO
      sendWoResponse(call);
    }
    else {
      LOGGER.error("Unable to refresh - could not map '{}' to Kodi library! {}", movie.getTitle(), movie.getDbId());
    }
  }

  public void refreshFromNfo(TvShow tvShow) {
    Integer kodiID = tvshowmappings.get(tvShow.getDbId());

    if (kodiID != null) {
      List<MediaFile> nfo = tvShow.getMediaFiles(MediaFileType.NFO);
      if (!nfo.isEmpty()) {
        LOGGER.info("Refreshing from NFO: {}", nfo.get(0).getFileAsPath());
      }
      else {
        LOGGER.error("No NFO file found to refresh! {}", tvShow.getTitle());
        // we do NOT return here, maybe Kodi will do something even w/o nfo...
      }

      final VideoLibrary.RefreshTVShow call = new VideoLibrary.RefreshTVShow(kodiID, false, true); // always refresh from NFO, recursive
      sendWoResponse(call);
    }
    else {
      LOGGER.error("Unable to refresh - could not map '{}' to Kodi library! {}", tvShow.getTitle(), tvShow.getDbId());
    }
  }

  public void refreshFromNfo(TvShowEpisode episode) {
    Integer kodiID = getEpisodeId(episode);

    if (kodiID != null) {
      List<MediaFile> nfo = episode.getMediaFiles(MediaFileType.NFO);
      if (!nfo.isEmpty()) {
        LOGGER.info("Refreshing from NFO: {}", nfo.get(0).getFileAsPath());
      }
      else {
        LOGGER.error("No NFO file found to refresh! {}", episode.getTitle());
        // we do NOT return here, maybe Kodi will do something even w/o nfo...
      }

      final VideoLibrary.RefreshEpisode call = new VideoLibrary.RefreshEpisode(kodiID, false); // always refresh from NFO
      sendWoResponse(call);
    }
    else {
      LOGGER.error("Unable to refresh - could not map '{}' to Kodi library! {}", episode.getTitle(), episode.getDbId());
    }
  }

  public void readWatchedState(Movie movie) {
    Integer kodiID = moviemappings.get(movie.getDbId());

    if (kodiID != null) {
      final VideoLibrary.GetMovieDetails call = new VideoLibrary.GetMovieDetails(kodiID, VideoModel.MovieDetail.PLAYCOUNT,
          VideoModel.MovieDetail.LASTPLAYED);
      send(call);
      if (call.getResult() != null && call.getResult().playcount != null) {
        movie.setPlaycount(call.getResult().playcount);
        if (call.getResult().playcount > 0) {
          movie.setWatched(true);
          try {
            movie.setLastWatched(StrgUtils.parseDate(call.getResult().lastplayed));
          }
          catch (Exception e) {
            movie.setLastWatched(new Date());
          }
        }
        else {
          // Kodi saids so
          movie.setWatched(false);
          movie.setLastWatched(null);
        }

        movie.writeNFO();
        movie.setLastWatched(null); // write date to NFO, but do not save it, not even in session!
        movie.saveToDb();
      }
    }
    else {
      LOGGER.error("Unable get playcount - could not map '{}' to Kodi library! {}", movie.getTitle(), movie.getDbId());
    }
  }

  public void readWatchedState(TvShowEpisode episode) {
    Integer kodiID = getEpisodeId(episode);

    if (kodiID != null) {
      final VideoLibrary.GetEpisodeDetails call = new VideoLibrary.GetEpisodeDetails(kodiID, VideoModel.EpisodeDetail.PLAYCOUNT,
          VideoModel.EpisodeDetail.LASTPLAYED);
      send(call);
      if (call.getResult() != null && call.getResult().playcount != null) {
        episode.setPlaycount(call.getResult().playcount);
        if (call.getResult().playcount > 0) {
          episode.setWatched(true);
          try {
            episode.setLastWatched(StrgUtils.parseDate(call.getResult().lastplayed));
          }
          catch (Exception e) {
            episode.setLastWatched(new Date());
          }
        }
        else {
          // Kodi saids so
          episode.setWatched(false);
          episode.setLastWatched(null);
        }

        episode.writeNFO();
        episode.setLastWatched(null); // write date to NFO, but do not save it, not even in session!
        episode.saveToDb();
      }
    }
    else {
      LOGGER.error("Unable get playcount - could not map '{}' to Kodi library! {}", episode.getTitle(), episode.getDbId());
    }
  }

  public Integer getEpisodeId(TvShowEpisode episode) {
    Integer kodiShowId = tvshowmappings.get(episode.getTvShowDbId());
    if (kodiShowId == null) {
      return null;
    }

    Integer kodiEpId = episodemappings.get(episode.getDbId());
    if (kodiEpId == null) {
      // cache show
      getAndSetTvShowEpisodeMappings(episode.getTvShow(), kodiShowId);
      // retry
      kodiEpId = episodemappings.get(episode.getDbId());
    }

    return kodiEpId;
  }

  protected synchronized void getAndSetTvShowEpisodeMappings(TvShow tmmShow, Integer kodiShowId) {
    // tvshow has not been cached - do it once
    final VideoLibrary.GetEpisodes episodeCall = new VideoLibrary.GetEpisodes(kodiShowId, EpisodeFields.FILE);
    send(episodeCall);
    if (episodeCall.getResults() != null && !episodeCall.getResults().isEmpty()) {
      Map<String, Integer> kodiDsAndFolder = new HashMap<>();
      for (EpisodeDetail ep : episodeCall.getResults()) {
        if (ep.file == null || ep.file.isEmpty()) {
          continue;
        }
        // KODI ds|file=id
        // Kodi return full path of show dir
        String ds = detectDatasource(ep.file); // detect datasource of show dir
        String rel = ep.file.replace(ds, ""); // remove ds, to have a relative folde
        rel = rel.replaceAll(SEPARATOR_REGEX, "/"); // normalize separators
        ds = ds.replaceAll(SEPARATOR_REGEX + "$", ""); // replace ending separator
        ds = ds.replaceAll(".*" + SEPARATOR_REGEX, ""); // replace everything till last separator
        if (!kodiDsAndFolder.containsKey(rel)) {
          kodiDsAndFolder.put(rel, ep.episodeid);
        }
        else {
          // no putIfAbsent since i wanna have a log!
          LOGGER.warn("Kodi episode {} already attached to another datasource - skipping", rel);
        }
      }
      LOGGER.debug("KODI {} episodes", kodiDsAndFolder.size());

      // TMM ds|dir=id
      Map<String, UUID> tmmDsAndFolder = prepareEpisodeFileMap(tmmShow);
      LOGGER.debug("TMM {} episodes", tmmDsAndFolder.size());

      // map em
      for (Map.Entry<String, UUID> entry : tmmDsAndFolder.entrySet()) {
        String key = entry.getKey();
        UUID value = entry.getValue();
        Integer kodiId = kodiDsAndFolder.get(key);
        if (kodiId != null && kodiId > 0) {
          // we have a match!
          episodemappings.put(value, kodiId);
        }
      }
      LOGGER.debug("mapped {} episodes for {}", episodemappings.size(), tmmShow.getTitle());
    }
  }

  // -----------------------------------------------------------------------------------

  public void cleanAudioLibrary() {
    final AudioLibrary.Clean call = new AudioLibrary.Clean(true);
    sendWoResponse(call);
  }

  public void scanAudioLibrary() {
    final AudioLibrary.Scan call = new AudioLibrary.Scan(null);
    sendWoResponse(call);
  }

  public void scanAudioLibrary(String dir) {
    final AudioLibrary.Scan call = new AudioLibrary.Scan(dir);
    sendWoResponse(call);
  }

  public List<String> getAudioDataSources() {
    return this.audiodatasources;
  }

  private void getAndSetAudioDataSources() {
    final Files.GetSources call = new Files.GetSources(FilesModel.Media.MUSIC);
    send(call);
    if (call.getResults() != null && !call.getResults().isEmpty()) {
      this.audiodatasources.clear();
      try {
        for (ListModel.SourceItem res : call.getResults()) {
          this.audiodatasources.add(res.file);
        }
      }
      catch (Exception e) {
        LOGGER.debug("could not process Kodi RPC response - '{}'", e.getMessage());
      }
    }
  }

  // -----------------------------------------------------------------------------------

  /**
   * Kodi version
   */
  public String getKodiVersion() {
    final Application.GetProperties call = new Application.GetProperties("version");
    send(call);
    try {
      ApplicationModel.PropertyValue res = call.getResult();
      int maj = res.version.major;
      int min = res.version.minor;
      return maj + "." + min;
    }
    catch (Exception ignored) {
      // just ignore
    }
    return "";
  }

  /**
   * quit remote Kodi instance
   */
  public void quitApplication() {
    final Application.Quit call = new Application.Quit();
    sendWoResponse(call);
  }

  /**
   * Toggles mute on/off
   */
  public void muteApplication() {
    final Application.GetProperties props = new Application.GetProperties("muted");
    send(props); // get current
    if (props.getResults() != null && !props.getResults().isEmpty()) {
      final Application.SetMute call = new Application.SetMute(new GlobalModel.Toggle(!props.getResult().muted));
      sendWoResponse(call); // toggle true/false
    }
  }

  /**
   * set volume 0-100
   * 
   * @param vol
   */
  public void setVolume(int vol) {
    final Application.SetVolume call = new Application.SetVolume(vol);
    sendWoResponse(call);
  }

  // -----------------------------------------------------------------------------------

  public void SystemEjectOpticalDrive() {
    final System.EjectOpticalDrive call = new System.EjectOpticalDrive();
    sendWoResponse(call);
  }

  public void SystemHibernate() {
    final System.EjectOpticalDrive call = new System.EjectOpticalDrive();
    sendWoResponse(call);
  }

  public void SystemShutdown() {
    final System.Shutdown call = new System.Shutdown();
    sendWoResponse(call);
  }

  public void SystemReboot() {
    final System.Reboot call = new System.Reboot();
    sendWoResponse(call);
  }

  public void SystemSuspend() {
    final System.Suspend call = new System.Suspend();
    sendWoResponse(call);
  }

  // -----------------------------------------------------------------------------------

  /**
   * Sends a call to Kodi and waits for the response.<br />
   * Call getResult() / getResults() afterwards
   * 
   * @param call
   *          the call to send
   */
  public void send(AbstractCall<?> call) {
    if (!isConnected()) {
      LOGGER.warn("Cannot send RPC call - not connected");
      return;
    }
    try {
      call.setResponse(JsonApiRequest.execute(connectionManager.getHostConfig(), call.getRequest()));
    }
    catch (ApiException e) {
      LOGGER.error("Error calling Kodi: {}", e.getMessage());
    }
  }

  /**
   * Sends the call to Kodi without waiting for a response (fire and forget)
   * 
   * @param call
   *          the call to send
   */
  public void sendWoResponse(AbstractCall<?> call) {
    if (!isConnected()) {
      LOGGER.warn("Cannot send RPC call - not connected");
      return;
    }

    try {
      JsonApiRequest.execute(connectionManager.getHostConfig(), call.getRequest());
    }
    catch (ApiException e) {
      LOGGER.error("Error calling Kodi: {}", e.getMessage());
    }
  }

  /**
   * Connect to Kodi with specified TCP port
   * 
   * @param config
   *          Host configuration
   * @throws Exception
   *           Throws {@link Exception} when something goes wrong with the initialization of the API.
   */
  public void connect(HostConfig config) throws Exception {
    if (isConnected()) {
      connectionManager.disconnect();
    }

    new Thread(() -> {
      try {
        LOGGER.info("Connecting to {}...", config.getAddress());
        connectionManager.connect(config);

        if (isConnected()) {
          this.kodiVersion = getKodiVersion();
          getAndSetVideoDataSources();
          getAndSetAudioDataSources();
          getAndSetMovieMappings();
          getAndSetTvShowMappings();
        }
      }
      catch (Exception e) {
        LOGGER.error("Error connecting to Kodi - '{}'", e.getMessage());
      }
    }).start();
  }

  public void connect() {
    Settings s = Settings.getInstance();
    if (s.getKodiHost().isEmpty()) {
      return;
    }

    try {
      connect(new HostConfig(s.getKodiHost(), s.getKodiHttpPort(), s.getKodiTcpPort(), s.getKodiUsername(), s.getKodiPassword()));
    }
    catch (Exception cex) {
      LOGGER.error("Error connecting to Kodi instance! {}", cex.getMessage());
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "KodiRPC", "Could not connect to Kodi: " + cex.getMessage()));
    }
  }

  public void disconnect() {
    connectionManager.disconnect();
    this.kodiVersion = "";
  }

  public void updateMovieMappings() {
    if (isConnected()) {
      getAndSetMovieMappings();
    }
  }

  public void updateTvShowMappings() {
    if (isConnected()) {
      getAndSetTvShowMappings();
    }
  }

  /**
   * @return json movie list or NULL
   */
  public List<MovieDetail> getAllMoviesSYNC() {
    final VideoLibrary.GetMovies call = new VideoLibrary.GetMovies(MovieFields.FILE);
    send(call);
    return call.getResults();
  }

  public void getAllMoviesASYNC() {
    // MovieFields.values.toArray(new String[0]) // all values
    final VideoLibrary.GetMovies vl = new VideoLibrary.GetMovies(MovieFields.FILE); // ID & label are always set; just add additional
    connectionManager.call(vl, new ApiCallback<>() {

      @Override
      public void onResponse(AbstractCall<MovieDetail> call) {
        LOGGER.info("found " + call.getResults().size() + " movies");
        for (MovieDetail res : call.getResults()) {
          LOGGER.debug(res.toString());
        }
      }

      @Override
      public void onError(int code, String message, String hint) {
        LOGGER.error("Error {}: {}", code, message);
      }
    });
  }

  /**
   * Forces Kodi to reload movie from NFO
   * 
   * @param movie
   */
  public void triggerReload(Movie movie) {
    // MovieFields.values.toArray(new String[0]) // all values
    final VideoLibrary.GetMovies vl = new VideoLibrary.GetMovies(MovieFields.FILE); // ID & label are always set; just add additional
    connectionManager.call(vl, new ApiCallback<>() {

      @Override
      public void onResponse(AbstractCall<MovieDetail> call) {
        LOGGER.info("found " + call.getResults().size() + " movies");
        for (MovieDetail res : call.getResults()) {
          LOGGER.debug(res.toString());
        }
      }

      @Override
      public void onError(int code, String message, String hint) {
        LOGGER.error("Error {}: {}", code, message);
      }
    });
  }

  public void getAllTvShows() {
    final VideoLibrary.GetTVShows vl = new VideoLibrary.GetTVShows();
    connectionManager.call(vl, new ApiCallback<>() {

      @Override
      public void onResponse(AbstractCall<TVShowDetail> call) {
        LOGGER.info("found " + call.getResults().size() + " shows");
        for (TVShowDetail res : call.getResults()) {
          LOGGER.debug(res.toString());
        }
      }

      @Override
      public void onError(int code, String message, String hint) {
        LOGGER.error("Error {}: {}", code, message);
      }
    });
  }
}
