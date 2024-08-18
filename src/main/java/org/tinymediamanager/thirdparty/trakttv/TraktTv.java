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
package org.tinymediamanager.thirdparty.trakttv;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.license.TmmFeature;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.TmmHttpClient;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.TraktV2Interceptor;
import com.uwetrottmann.trakt5.entities.AccessToken;
import com.uwetrottmann.trakt5.entities.ListEntry;
import com.uwetrottmann.trakt5.entities.SyncErrors;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.entities.SyncStats;
import com.uwetrottmann.trakt5.entities.TraktError;
import com.uwetrottmann.trakt5.entities.TraktList;
import com.uwetrottmann.trakt5.entities.UserSlug;
import com.uwetrottmann.trakt5.enums.Audio;
import com.uwetrottmann.trakt5.enums.AudioChannels;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.Hdr;
import com.uwetrottmann.trakt5.enums.MediaType;
import com.uwetrottmann.trakt5.enums.Resolution;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Sync your collection and watched status with Trakt.tv<br>
 * Using best practice 2-way-sync according to http://trakt.tv/api-docs/sync<br>
 * https://github.com/UweTrottmann/trakt-java
 * 
 * @author Myron Boyle
 * 
 */

public class TraktTv implements TmmFeature {
  private static final Logger LOGGER = LoggerFactory.getLogger(TraktTv.class);
  private static TraktTv      instance;

  private TraktV2             api;

  enum SyncType {
    COLLECTION,
    WATCHED,
    RATING
  }

  // thread safe initialization of the API
  protected synchronized void initAPI() throws ScrapeException {
    // create a new instance of the trakt api
    if (api == null) {
      try {
        String[] keys = getApiKeys();
        api = new TraktV2(keys[0], keys[1], "urn:ietf:wg:oauth:2.0:oob") {
          // tell the trakt api to use our OkHttp client

          @Override
          protected synchronized OkHttpClient okHttpClient() {
            OkHttpClient.Builder builder = TmmHttpClient.newBuilder(true);
            builder.addInterceptor(new TraktV2Interceptor(this));
            return builder.build();
          }
        };
      }
      catch (Exception e) {
        LOGGER.error("could not initialize the API: {}", e.getMessage());
        // force re-initialization the next time this will be called
        api = null;
        throw new ScrapeException(e);
      }
    }
  }

  public static synchronized TraktTv getInstance() {
    if (instance == null) {
      instance = new TraktTv();
    }
    return instance;
  }

  private TraktTv() {
  }

  public Map<String, String> authenticateViaPin(String pin) throws Exception {
    initAPI();

    Map<String, String> result = new HashMap<>();
    Response<AccessToken> response = api.exchangeCodeForAccessToken(pin);

    if (response.isSuccessful() && response.body() != null) {
      // get tokens
      String accessToken = response.body().access_token;
      String refreshToken = response.body().refresh_token;
      if (StringUtils.isNoneBlank(accessToken, refreshToken)) {
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
      }
    }

    return result;
  }

  /**
   * get a new accessToken with the refreshToken
   */
  public void refreshAccessToken() throws Exception {
    if (StringUtils.isBlank(Settings.getInstance().getTraktRefreshToken())) {
      throw new IOException("no trakt.tv refresh token found");
    }

    initAPI();

    Response<AccessToken> response = api.refreshToken(Settings.getInstance().getTraktRefreshToken())
        .refreshAccessToken(Settings.getInstance().getTraktRefreshToken());

    if (response.isSuccessful() && response.body() != null) {
      if (StringUtils.isNoneBlank(response.body().access_token, response.body().refresh_token)) {
        Settings.getInstance().setTraktAccessToken(response.body().access_token);
        Settings.getInstance().setTraktRefreshToken(response.body().refresh_token);
        api.accessToken(Settings.getInstance().getTraktAccessToken());
      }
    }
    else {
      throw new IOException("could not get trakt.tv refresh token (HTTP " + response.code() + " - " + response.message() + ")");
    }
  }

  /**
   * do we have values for user/pass/api?!
   * 
   * @return true/false if trakt could be called
   */
  private boolean isEnabled() {
    if (!isFeatureEnabled()) {
      LOGGER.warn("Trakt.tv feature not enabled!");
      return false;
    }

    if (StringUtils.isNoneBlank(Settings.getInstance().getTraktAccessToken(), Settings.getInstance().getTraktRefreshToken())) {
      // everything seems fine; also set the access token
      api.accessToken(Settings.getInstance().getTraktAccessToken());
      return true;
    }

    LOGGER.warn("Trakt.tv connection tokens empty - try reauthenticate in settings!");
    return false;
  }

  TraktV2 getApi() {
    return api;
  }

  private <T> T executeCall(Call<T> call) throws IOException {
    Response<T> response = call.execute();
    if (!response.isSuccessful() && response.code() == 401) {
      api.refreshToken();
      response = call.execute(); // retry
    }
    if (!response.isSuccessful()) {
      String message = "Request failed: " + response.code() + " " + response.message();
      TraktError error = api.checkForTraktError(response);
      if (error != null && error.message != null) {
        message += " message: " + error.message;
      }
      throw new HttpException(response.code(), message);
    }

    T body = response.body();
    if (body != null) {
      return body;
    }
    else {
      throw new IOException("Body should not be null for successful response");
    }
  }

  public void getPersonalLists() throws ScrapeException {
    initAPI();

    if (!isEnabled()) {
      return;
    }

    try {
      // get all users lists
      List<TraktList> lists = executeCall(api.users().lists(UserSlug.ME));

      // get detail info for each list
      for (TraktList list : lists) {
        System.out.println(list.name);

        List<ListEntry> details = executeCall(api.users().listItems(UserSlug.ME, Integer.toString(list.ids.trakt), Extended.METADATA));
        for (ListEntry detail : details) {
          System.out.println(detail.id);
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("failed gettinguser lists: {}", e.getMessage());
      return;
    }
  }

  // @formatter:off
  // ███╗   ███╗ ██████╗ ██╗   ██╗██╗███████╗███████╗
  // ████╗ ████║██╔═══██╗██║   ██║██║██╔════╝██╔════╝
  // ██╔████╔██║██║   ██║██║   ██║██║█████╗  ███████╗
  // ██║╚██╔╝██║██║   ██║╚██╗ ██╔╝██║██╔══╝  ╚════██║
  // ██║ ╚═╝ ██║╚██████╔╝ ╚████╔╝ ██║███████╗███████║
  // ╚═╝     ╚═╝ ╚═════╝   ╚═══╝  ╚═╝╚══════╝╚══════╝
  // @formatter:on

  /**
   * Syncs Trakt.tv collection (specified movies)<br>
   * Gets all Trakt movies from collection, matches them to ours, and sends ONLY the new ones back to Trakt
   */
  public void syncTraktMovieCollection(List<Movie> moviesInTmm) throws ScrapeException {
    initAPI();

    if (!isEnabled()) {
      return;
    }

    new TraktTvMovie(this).syncTraktMovieCollection(moviesInTmm);
  }

  /**
   * Syncs Trakt.tv "seen" flag (all gives you have already marked as watched)<br>
   * Gets all watched movies from Trakt, and sets the "watched" flag on TMM movies.<br>
   * Then update the remaining TMM movies on Trakt as 'seen'.
   */
  public void syncTraktMovieWatched(List<Movie> moviesInTmm) throws ScrapeException {
    initAPI();

    if (!isEnabled()) {
      return;
    }

    new TraktTvMovie(this).syncTraktMovieWatched(moviesInTmm);
  }

  /**
   * Syncs Trakt.tv "personal rating"<br>
   * Gets all movies from Trakt, set the personal rating for movies without existing personal rating and send back new/changed items<br>
   */
  public void syncTraktMovieRating(List<Movie> moviesInTmm) throws ScrapeException {
    initAPI();

    if (!isEnabled()) {
      return;
    }

    new TraktTvMovie(this).syncTraktMovieRating(moviesInTmm);
  }

  /**
   * clears the whole Trakt.tv movie collection. Gets all Trakt.tv movies from your collection and removes them from the collection and the watched
   * state; a little helper to initialize the collection
   */
  void clearTraktMovies() throws Exception {
    initAPI();

    if (!isEnabled()) {
      return;
    }

    new TraktTvMovie(this).clearTraktMovies();
  }

  // @formatter:off
  //  ████████╗██╗   ██╗███████╗██╗  ██╗ ██████╗ ██╗    ██╗███████╗
  //  ╚══██╔══╝██║   ██║██╔════╝██║  ██║██╔═══██╗██║    ██║██╔════╝
  //     ██║   ██║   ██║███████╗███████║██║   ██║██║ █╗ ██║███████╗
  //     ██║   ╚██╗ ██╔╝╚════██║██╔══██║██║   ██║██║███╗██║╚════██║
  //     ██║    ╚████╔╝ ███████║██║  ██║╚██████╔╝╚███╔███╔╝███████║
  //     ╚═╝     ╚═══╝  ╚══════╝╚═╝  ╚═╝ ╚═════╝  ╚══╝╚══╝ ╚══════╝
  // @formatter:on

  /**
   * Syncs Trakt.tv collection (gets all IDs & dates, and adds all TMM shows to Trakt)<br>
   * Do not send diffs, since this is too complicated currently :|
   */
  public void syncTraktTvShowCollection(List<TvShow> tvShowsInTmm) throws Exception {
    initAPI();

    if (!isEnabled()) {
      return;
    }

    new TraktTvTvShow(this).syncTraktTvShowCollection(tvShowsInTmm);
  }

  public void syncTraktTvShowWatched(List<TvShow> tvShowsInTmm) throws Exception {
    initAPI();

    if (!isEnabled()) {
      return;
    }

    new TraktTvTvShow(this).syncTraktTvShowWatched(tvShowsInTmm);
  }

  public void syncTraktTvShowRating(List<TvShow> tvShowsInTmm) throws Exception {
    initAPI();

    if (!isEnabled()) {
      return;
    }

    new TraktTvTvShow(this).syncTraktTvShowRating(tvShowsInTmm);
  }

  /**
   * clears the whole Trakt.tv movie collection. Gets all Trakt.tv movies from your collection and removes them from the collection and the watched
   * state; a little helper to initialize the collection
   */
  public void clearTraktTvShows() throws Exception {
    initAPI();

    if (!isEnabled()) {
      return;
    }

    new TraktTvTvShow(this).clearTraktTvShows();
  }

  // @formatter:off
  // ██╗   ██╗████████╗██╗██╗     ███████╗
  // ██║   ██║╚══██╔══╝██║██║     ██╔════╝
  // ██║   ██║   ██║   ██║██║     ███████╗
  // ██║   ██║   ██║   ██║██║     ╚════██║
  // ╚██████╔╝   ██║   ██║███████╗███████║
  //  ╚═════╝    ╚═╝   ╚═╝╚══════╝╚══════╝
  // @formatter:on

  static MediaType getMediaType(MediaSource mediaSource) {
    if (mediaSource == MediaSource.BLURAY || mediaSource == MediaSource.UHD_BLURAY) {
      return MediaType.BLURAY;
    }
    if (mediaSource == MediaSource.DVD || mediaSource == MediaSource.DVDSCR) {
      return MediaType.DVD;
    }
    if (mediaSource == MediaSource.HDDVD) {
      return MediaType.HDDVD;
    }
    if (mediaSource == MediaSource.VHS || mediaSource == MediaSource.D_VHS) {
      return MediaType.VHS;
    }
    if (mediaSource == MediaSource.LASERDISC) {
      return MediaType.LASERDISC;
    }
    return MediaType.DIGITAL;
  }

  static Resolution getResolution(MediaFile mediaFile) {
    switch (mediaFile.getVideoFormat()) {
      case MediaFileHelper.VIDEO_FORMAT_4320P:
      case MediaFileHelper.VIDEO_FORMAT_2160P:
        return Resolution.UHD_4K;

      case MediaFileHelper.VIDEO_FORMAT_1440P:
      case MediaFileHelper.VIDEO_FORMAT_1080P:
        return Resolution.HD_1080P;

      case MediaFileHelper.VIDEO_FORMAT_720P:
        return Resolution.HD_720P;

      case MediaFileHelper.VIDEO_FORMAT_576P:
      case MediaFileHelper.VIDEO_FORMAT_540P:
        return Resolution.SD_576P;

      case MediaFileHelper.VIDEO_FORMAT_480P:
      case MediaFileHelper.VIDEO_FORMAT_360P:
        return Resolution.SD_480P;

      default:
        return null;
    }
  }

  static Hdr getHdr(String hdr) {
    if ("hdr10+".equalsIgnoreCase(hdr)) {
      return Hdr.HDR10_PLUS;
    }
    if ("hdr10".equalsIgnoreCase(hdr) || "hdr".equalsIgnoreCase(hdr)) {
      return Hdr.HDR10;
    }
    if ("dolby vision".equalsIgnoreCase(hdr)) {
      return Hdr.DOLBY_VISION;
    }
    if ("hlg".equalsIgnoreCase(hdr)) {
      return Hdr.HLG;
    }
    return null;
  }

  static Audio getAudio(String audioCodec) {
    if ("DTSHD-MA".equalsIgnoreCase(audioCodec)) {
      return Audio.DTS_MA;
    }
    if ("DTSHD-HRA".equalsIgnoreCase(audioCodec)) {
      return Audio.DTS_HR;
    }
    if ("DTS-X".equalsIgnoreCase(audioCodec)) {
      return Audio.DTS_X;
    }
    if ("TrueHD/Atmos".equalsIgnoreCase(audioCodec) || "Atmos".equalsIgnoreCase(audioCodec)) {
      return Audio.DOLBY_ATMOS;
    }
    if ("DTS".equalsIgnoreCase(audioCodec)) {
      return Audio.DTS;
    }
    if ("DTS-ES".equalsIgnoreCase(audioCodec)) {
      // DTS-ES is not (yet) supported. Use DTS for now
      return Audio.DTS;
    }
    if ("TrueHD".equalsIgnoreCase(audioCodec)) {
      return Audio.DOLBY_TRUEHD;
    }
    if ("EAC3".equalsIgnoreCase(audioCodec)) {
      return Audio.DOLBY_DIGITAL_PLUS;
    }
    if ("EAC3/Atmos".equalsIgnoreCase(audioCodec)) {
      return Audio.DOLBY_DIGITAL_PLUS_ATMOS;
    }
    if ("AC3".equalsIgnoreCase(audioCodec)) {
      return Audio.DOLBY_DIGITAL;
    }
    if ("MP2".equalsIgnoreCase(audioCodec)) {
      return Audio.MP2;
    }
    if ("MP3".equalsIgnoreCase(audioCodec)) {
      return Audio.MP3;
    }
    if ("OGG".equalsIgnoreCase(audioCodec)) {
      return Audio.OGG;
    }
    if ("WMA".equalsIgnoreCase(audioCodec)) {
      return Audio.WMA;
    }
    if ("AAC".equalsIgnoreCase(audioCodec)) {
      return Audio.AAC;
    }
    if ("FLAC".equalsIgnoreCase(audioCodec)) {
      return Audio.FLAC;
    }
    return null;
  }

  static AudioChannels getAudioChannels(int audioChannelCount) {
    switch (audioChannelCount) {
      case 1:
        return AudioChannels.CH1_0;

      case 2:
        return AudioChannels.CH2_0;

      case 3:
        return AudioChannels.CH2_1;

      case 4:
        return AudioChannels.CH3_1;

      case 5:
        return AudioChannels.CH4_1;

      case 6:
        return AudioChannels.CH5_1;

      case 7:
        return AudioChannels.CH6_1;

      case 8:
        return AudioChannels.CH7_1;

      case 10:
        return AudioChannels.CH9_1;

      case 11:
        return AudioChannels.CH10_1;

      default:
        return null;
    }
  }

  /**
   * prints some trakt response status
   * 
   * @param resp
   *          the response
   */
  static void printStatus(SyncResponse resp) {
    if (resp != null) {
      String info = getStatusString(resp.added);
      if (!info.isEmpty()) {
        LOGGER.debug("Added       : {}", info);
      }
      info = getStatusString(resp.existing);
      if (!info.isEmpty()) {
        LOGGER.debug("Existing    : {}", info);
      }
      info = getStatusString(resp.deleted);
      if (!info.isEmpty()) {
        LOGGER.debug("Deleted     : {}", info);
      }
      info = getErrorString(resp.not_found);
      if (!info.isEmpty()) {
        LOGGER.debug("Errors      : {}", info);
      }
    }
  }

  static String getStatusString(SyncStats ss) {
    if (ss == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(50);

    if (ss.movies != null && ss.movies > 0) {
      sb.append(ss.movies).append(" Movies ");
    }
    if (ss.shows != null && ss.shows > 0) {
      sb.append(ss.shows).append(" Shows ");
    }
    if (ss.seasons != null && ss.seasons > 0) {
      sb.append(ss.seasons).append(" Seasons ");
    }
    if (ss.episodes != null && ss.episodes > 0) {
      sb.append(ss.episodes).append(" Episodes");
    }

    return sb.toString();
  }

  static String getErrorString(SyncErrors se) {
    if (se == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(50);

    if (se.movies != null && !se.movies.isEmpty()) {
      sb.append("\n").append(se.movies.size()).append(" Movies: ");
      se.movies.forEach(movie -> sb.append(movie.ids).append(","));
    }
    if (se.shows != null && !se.shows.isEmpty()) {
      sb.append("\n").append(se.shows.size()).append(" Shows: ");
      se.shows.forEach(show -> sb.append(show.ids).append(","));
    }
    if (se.seasons != null && !se.seasons.isEmpty()) {
      sb.append("\n").append(se.seasons.size()).append(" Seasons: ");
      se.seasons.forEach(season -> sb.append(season.number).append(",")); // no clue what show
    }
    if (se.episodes != null && !se.episodes.isEmpty()) {
      sb.append("\n").append(se.episodes.size()).append(" Episodes: ");
      se.episodes.forEach(episode -> sb.append(episode.ids).append(","));
    }

    return sb.toString();
  }
}
