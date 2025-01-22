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
package org.tinymediamanager.scraper.opensubtitles_com;

import static org.tinymediamanager.scraper.MediaMetadata.IMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TMDB;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.SubtitleSearchAndScrapeOptions;
import org.tinymediamanager.scraper.SubtitleSearchResult;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.opensubtitles_com.model.DownloadRequest;
import org.tinymediamanager.scraper.opensubtitles_com.model.DownloadResponse;
import org.tinymediamanager.scraper.opensubtitles_com.model.SearchResponse;
import org.tinymediamanager.scraper.opensubtitles_com.model.SearchResult;
import org.tinymediamanager.scraper.opensubtitles_com.model.SubtitleFile;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import retrofit2.Response;

abstract class OpenSubtitlesComSubtitleProvider implements IMediaProvider {
  private static final String                                      ID              = "opensubtitles2";
  private static final int                                         HASH_CHUNK_SIZE = 64 * 1024;

  private static final CacheMap<DownloadRequest, DownloadResponse> DOWNLOAD_CACHE  = new CacheMap<>(7200 * 3, 60);

  private final MediaProviderInfo                                  providerInfo;

  protected Controller                                             controller      = null;

  protected OpenSubtitlesComSubtitleProvider() {
    providerInfo = createMediaProviderInfo();
  }

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  protected abstract Logger getLogger();

  private MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "OpenSubtitles.com",
        "<html><h3>OpenSubtitles.com</h3><br />A subtitle scraper for OpenSubtitles.com</html>",
        OpenSubtitlesComSubtitleProvider.class.getResource("/org/tinymediamanager/scraper/opensubtitles_com.svg"));

    // configure/load settings
    info.getConfig().addText("username", "");
    info.getConfig().addText("password", "", true);
    info.getConfig().addBoolean("trustedSources", false);
    info.getConfig().addBoolean("aiTranslated", false);
    info.getConfig().addBoolean("machineTranslated", false);
    info.getConfig().addSelect("subtitleFormat", new String[] { "srt", "sub" }, "srt");

    info.getConfig().load();

    return info;
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  public boolean isActive() {
    return isFeatureEnabled() && StringUtils.isNoneBlank(getUserName(), getPassword()) && isApiKeyAvailable(null);
  }

  private synchronized void initAPI() throws ScrapeException {
    if (controller == null) {
      if (!isActive()) {
        throw new ScrapeException(new FeatureNotEnabledException(this));
      }

      controller = new Controller();
    }

    // API key check
    try {
      controller.setApiKey(getApiKey());
      controller.setUsername(getUserName());
      controller.setPassword(getPassword());
      controller.authenticate();
    }
    catch (Exception e) {
      throw new ScrapeException(e);
    }
  }

  private String getUserName() {
    return providerInfo.getConfig().getValue("username");
  }

  private String getPassword() {
    return providerInfo.getConfig().getValue("password");
  }

  public List<SubtitleSearchResult> search(SubtitleSearchAndScrapeOptions options) throws ScrapeException {
    // lazy initialization of the api
    initAPI();

    List<SubtitleSearchResult> results = new ArrayList<>();

    // according to the API we can send everything together:
    // you can combine all together (send everything what you have and we will take of rest)
    // https://opensubtitles.stoplight.io/docs/opensubtitles-api/a172317bd5ccc-search-for-subtitles
    Map<String, String> query = new TreeMap<>();

    // the API docs do not show _how_ the languages should be sent, but testing with the website we saw that _some_
    // languages need to be sent along with the country code
    String language;

    // https://opensubtitles.stoplight.io/docs/opensubtitles-api/1de776d20e873-languages

    switch (options.getLanguage()) {
      case pt -> language = "pt-PT";
      case pt_BR -> language = "pt-BR";
      case zh_CN -> language = "zh-CN";
      case zh_TW -> language = "zh-TW";
      default -> language = options.getLanguage().getLanguage().toLowerCase(Locale.ROOT);
    }

    query.put("languages", language);

    if (getProviderInfo().getConfig().getValueAsBool("aiTranslated")) {
      query.put("ai_translated", "include");
    }
    else {
      query.put("ai_translated", "exclude");
    }

    if (getProviderInfo().getConfig().getValueAsBool("trustedSources")) {
      query.put("trusted_sources", "only");
    }
    else {
      query.put("trusted_sources", "include");
    }

    if (getProviderInfo().getConfig().getValueAsBool("machineTranslated")) {
      query.put("machine_translated", "include");
    }
    else {
      query.put("machine_translated", "exclude");
    }

    // pass moviehash
    MediaFile mediaFile = options.getMediaFile();
    if (mediaFile != null && mediaFile.exists() && mediaFile.getFilesize() > 0) {
      File file = mediaFile.getFile().toFile();
      long fileSize = file.length();
      String hash = computeOpenSubtitlesHash(file);
      query.put("moviehash", hash);

      getLogger().debug("moviebytesize: {}; moviehash: {}", fileSize, hash);
    }

    // pass imdb/tmdb id
    if (MediaIdUtil.isValidImdbId(options.getImdbId()) || options.getTmdbId() > 0) {
      if (MediaIdUtil.isValidImdbId(options.getImdbId())) {
        getLogger().debug("searching subtitle for imdb id '{}'", options.getImdbId());
        query.put("imdb_id", String.valueOf(formatImdbId(options.getImdbId())));
      }
      else if (options.getTmdbId() > 0) {
        getLogger().debug("searching subtitle for tmdb id '{}'", options.getTmdbId());
        query.put("tmdb_id", String.valueOf(options.getTmdbId()));
      }
      else if (options.getIds().get("tvShowIds") instanceof Map) {
        Map<String, Object> tvShowIds = (Map<String, Object>) options.getIds().get("tvShowIds");

        String tvShowImdbId = String.valueOf(tvShowIds.get(IMDB));
        int tvShowTmdbId = MetadataUtil.parseInt(String.valueOf(tvShowIds.get(MediaMetadata.TMDB)), 0);

        if (MediaIdUtil.isValidImdbId(tvShowImdbId)) {
          getLogger().debug("searching subtitle for TV show imdb id '{}'", tvShowImdbId);
          query.put("parent_imdb_id", String.valueOf(formatImdbId(tvShowImdbId)));
        }
        else if (tvShowTmdbId > 0) {
          getLogger().debug("searching subtitle for TV show tmdb id '{}'", tvShowTmdbId);
          query.put("parent_tmdb_id", String.valueOf(tvShowTmdbId));
        }

        if (options.getEpisode() > -1) {
          query.put("episode_number", String.valueOf(options.getEpisode()));
        }
        if (options.getSeason() > -1) {
          query.put("season_number", String.valueOf(options.getSeason()));
        }
      }
    }

    getLogger().debug("searching subtitle");

    try {
      Response<SearchResponse> response = controller.getService().search(query).execute();

      if (response.isSuccessful() && response.body() != null) {
        for (SearchResult result : ListUtils.nullSafe(response.body().data)) {
          if (result.attributes == null || ListUtils.isEmpty(result.attributes.files)) {
            continue;
          }

          // requested language and response must match
          if (!language.equals(result.attributes.language)) {
            continue;
          }

          SubtitleSearchResult subtitleSearchResult = prepareSearchResult(result, options);
          if (subtitleSearchResult == null) {
            getLogger().debug("Could not detect any valid subtitle file to download");
            continue;
          }

          results.add(subtitleSearchResult);
        }
      }

      getLogger().debug("found {} results", results.size());
    }
    catch (Exception e) {
      getLogger().error("Could not search subtitle - {}", e.getMessage());
    }

    Collections.sort(results);
    Collections.reverse(results);

    return results;
  }

  private SubtitleSearchResult prepareSearchResult(SearchResult result, SubtitleSearchAndScrapeOptions options) {
    MediaFile mediaFile = options.getMediaFile();

    SubtitleSearchResult subtitleSearchResult = new SubtitleSearchResult(providerInfo.getId());
    subtitleSearchResult.setId(result.id);

    if (result.attributes.featureDetails != null) {
      subtitleSearchResult.setTitle(result.attributes.featureDetails.title);
    }
    subtitleSearchResult.setStackCount(result.attributes.files.size());
    subtitleSearchResult.setReleaseName(result.attributes.release);
    subtitleSearchResult.setRating(result.attributes.ratings);

    // create the download callback (we can take the first file here)
    SubtitleFile subtitleFile = null;
    if (result.attributes.files.size() == 1) {
      // only 1 file - no check needed
      subtitleFile = result.attributes.files.get(0);
    }
    else if (mediaFile != null) {
      // more files - need to guess the right one (stacking)
      for (SubtitleFile sf : result.attributes.files) {
        if (sf.cdNumber == options.getMediaFile().getStacking()) {
          subtitleFile = sf;
          break;
        }
      }
    }

    // we could not detect _any_ subtitle file to download
    if (subtitleFile == null) {
      return null;
    }

    // hash search will give a 100% perfect match
    float score = 1;

    // check ID
    if (!compareIds(result, options)) {
      // no ID match? subtract 0.5
      score -= 0.5;
    }

    if (!subtitleFile.moviehashMatch) {
      // no perfect match? subtract 0.1
      score -= 0.1;

      // check fps
      if (mediaFile != null && mediaFile.getFrameRate() != result.attributes.fps) {
        // no fps match? subtract 0.1
        score -= 0.1;
      }
    }

    subtitleSearchResult.setScore(score);

    SubtitleFile finalSubtitleFile = subtitleFile;
    subtitleSearchResult.setUrl(() -> download(finalSubtitleFile, options.getMediaFile().getFrameRate()));

    return subtitleSearchResult;
  }

  private boolean compareIds(SearchResult result, SubtitleSearchAndScrapeOptions options) {
    if (result.attributes.featureDetails == null) {
      return false;
    }

    // check the IDs from the objects itself
    int imdbIdFromResult = result.attributes.featureDetails.imdbId;
    int imdbIdFromOptions = MetadataUtil.unboxInteger(formatImdbId(options.getImdbId()), 0);
    if (imdbIdFromResult > 0 && imdbIdFromOptions > 0 && imdbIdFromResult != imdbIdFromOptions) {
      return false;
    }

    int tmdbIdFromResult = result.attributes.featureDetails.tmdbId;
    int tmdbIdFromOptions = options.getTmdbId();
    if (tmdbIdFromResult > 0 && tmdbIdFromOptions > 0 && tmdbIdFromResult != tmdbIdFromOptions) {
      return false;
    }

    // if this is an episode, check the parent IDs and the S/E
    if (options.getIds().get("tvShowIds") instanceof Map) {
      Map<String, Object> tvShowIds = (Map<String, Object>) options.getIds().get("tvShowIds");
      int parentImdbIdFromResult = result.attributes.featureDetails.parentImdbId;
      int parentIdFromOptions = MetadataUtil.unboxInteger(formatImdbId(String.valueOf(tvShowIds.get(IMDB))), 0);
      if (parentImdbIdFromResult > 0 && parentIdFromOptions > 0 && parentImdbIdFromResult != parentIdFromOptions) {
        return false;
      }

      int parentTmdbIdFromResult = result.attributes.featureDetails.parentTmdbId;
      int parentTmdbIdFromOptions = MetadataUtil.parseInt(String.valueOf(tvShowIds.get(TMDB)), 0);
      if (parentTmdbIdFromResult > 0 && parentTmdbIdFromOptions > 0 && parentTmdbIdFromResult != parentTmdbIdFromOptions) {
        return false;
      }

      // last but not least, compare S/E
      int seasonFromResult = result.attributes.featureDetails.seasonNumber;
      int seasonFromOptions = options.getSeason();
      if (seasonFromResult > -1 && seasonFromOptions > -1 && seasonFromResult != seasonFromOptions) {
        return false;
      }

      int episodeFromResult = result.attributes.featureDetails.episodeNumber;
      int episodeFromOptions = options.getEpisode();
      if (episodeFromResult > -1 && episodeFromOptions > -1 && episodeFromResult != episodeFromOptions) {
        return false;
      }
    }

    // nothing to check? return true to avoid false positives
    return true;
  }

  private String download(SubtitleFile file, double frameRate) throws ScrapeException {
    DownloadRequest request = new DownloadRequest();
    request.file_id = file.fileId;
    request.sub_format = getProviderInfo().getConfig().getValue("subtitleFormat");

    if (frameRate != 0) {
      request.in_fps = file.fps;
      request.out_fps = frameRate;
    }

    // check the cache
    DownloadResponse downloadResponse = DOWNLOAD_CACHE.get(request);
    if (downloadResponse != null) {
      return downloadResponse.link;
    }

    // call the download endpoint
    try {
      initAPI();

      Response<DownloadResponse> response = controller.getService().download(request).execute();
      if (response.isSuccessful() && response.body() != null) {
        DOWNLOAD_CACHE.put(request, response.body());
        return response.body().link;
      }
      else {
        if (response.code() == 406 && response.errorBody() != null) {
          Gson gson = new Gson();
          Type type = new TypeToken<DownloadResponse>() {
          }.getType();
          DownloadResponse errorResponse = gson.fromJson(response.errorBody().charStream(), type);

          // we hit the rate limit
          throw new HttpException(response.code(), errorResponse.message);
        }
        else {
          throw new HttpException(response.code(), response.message());
        }
      }
    }
    catch (Exception e) {
      getLogger().error("Could not retrieve download url - '{}'", e.getMessage());
      throw new ScrapeException(e);
    }
  }

  static Integer formatImdbId(String imdbId) {
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      return null;
    }

    try {
      return Integer.parseInt(imdbId.replace("tt", ""));
    }
    catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns OpenSubtitle hash or empty string if error
   *
   * @param file
   *          the file to compute the hash
   * @return hash
   */
  private String computeOpenSubtitlesHash(File file) {
    long size = file.length();
    long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);

    try (FileInputStream is = new FileInputStream(file); FileChannel fileChannel = is.getChannel()) {
      // do not use FileChannel.map() here because it is not releasing resources
      ByteBuffer buf = ByteBuffer.allocate((int) chunkSizeForFile);
      fileChannel.read(buf);
      long head = computeOpenSubtitlesHashForChunk(buf);

      fileChannel.read(buf, Math.max(size - HASH_CHUNK_SIZE, 0));
      long tail = computeOpenSubtitlesHashForChunk(buf);

      return String.format("%016x", size + head + tail).toLowerCase(Locale.ROOT);
    }
    catch (Exception e) {
      getLogger().error("Error computing OpenSubtitles hash", e);
    }
    return "";
  }

  private static long computeOpenSubtitlesHashForChunk(ByteBuffer buffer) {

    buffer.rewind();
    LongBuffer longBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
    long hash = 0;

    while (longBuffer.hasRemaining()) {
      hash += longBuffer.get();
    }

    return hash;
  }
}
