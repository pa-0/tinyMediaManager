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
package org.tinymediamanager.thirdparty.trakttv;

import static org.tinymediamanager.thirdparty.trakttv.TraktTv.getAudio;
import static org.tinymediamanager.thirdparty.trakttv.TraktTv.getAudioChannels;
import static org.tinymediamanager.thirdparty.trakttv.TraktTv.getHdr;
import static org.tinymediamanager.thirdparty.trakttv.TraktTv.getMediaType;
import static org.tinymediamanager.thirdparty.trakttv.TraktTv.getResolution;
import static org.tinymediamanager.thirdparty.trakttv.TraktTv.printStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.BaseEpisode;
import com.uwetrottmann.trakt5.entities.BaseSeason;
import com.uwetrottmann.trakt5.entities.BaseShow;
import com.uwetrottmann.trakt5.entities.EpisodeIds;
import com.uwetrottmann.trakt5.entities.Metadata;
import com.uwetrottmann.trakt5.entities.RatedEpisode;
import com.uwetrottmann.trakt5.entities.RatedShow;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncEpisode;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.entities.TraktError;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.Rating;
import com.uwetrottmann.trakt5.enums.RatingsFilter;

import retrofit2.Call;
import retrofit2.Response;

/**
 * the TV show implementation of the Trakt.tv interface
 *
 * @author Manuel Laggner
 */
class TraktTvTvShow {
  private static final Logger LOGGER = LoggerFactory.getLogger(TraktTvTvShow.class);

  private final TraktV2       api;

  public TraktTvTvShow(TraktTv traktTv) {
    this.api = traktTv.getApi();
  }

  private <T> T executeCall(Call<T> call) throws IOException {
    Response<T> response = call.execute();
    if (!response.isSuccessful() && response.code() == 401) {
      api.refreshAccessToken(Objects.requireNonNull(api.refreshToken()));
      response = call.clone().execute(); // retry
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

  /**
   * Syncs Trakt.tv collection (gets all IDs & dates, and adds all TMM shows to Trakt)<br>
   * Do not send diffs, since this is too complicated currently :|
   */
  void syncTraktTvShowCollection(List<TvShow> tvShowsInTmm) {

    // create a local copy of the list
    List<TvShow> tvShows = new ArrayList<>(tvShowsInTmm);

    // *****************************************************************************
    // 1) sync ALL missing show IDs & dates from trakt
    // *****************************************************************************
    List<BaseShow> traktShows;
    try {
      // Extended.DEFAULT adds url, poster, fanart, banner, genres
      // Extended.MAX adds certs, runtime, and other stuff (useful for scraper!)
      traktShows = executeCall(api.sync().collectionShows(Extended.METADATA));
    }
    catch (Exception e) {
      LOGGER.error("failed syncing trakt: {}", e.getMessage());
      return;
    }

    LOGGER.info("You have {} TvShows in your Trakt.tv collection", traktShows.size());

    for (BaseShow traktShow : traktShows) {
      List<TvShow> matchingTmmTvShows = getTmmTvShowForTraktShow(tvShows, traktShow.show);

      for (TvShow tmmShow : matchingTmmTvShows) {
        if (!tmmShow.isLocked()) {
          // update show IDs from trakt
          boolean showDirty = updateIDs(tmmShow, traktShow.show);

          // update collection date from trakt (show)
          if (traktShow.last_collected_at != null) {
            Date collectedAt = DateTimeUtils.toDate(traktShow.last_collected_at.toInstant());
            if (!collectedAt.equals(tmmShow.getDateAdded())) {
              // always set from trakt, if not matched (Trakt = master)
              LOGGER.trace("Marking TvShow '{}' as collected on {} (was {})", tmmShow.getTitle(), collectedAt, tmmShow.getDateAddedAsString());
              tmmShow.setDateAdded(collectedAt);
              showDirty = true;
            }
          }

          // update collection date from trakt (episodes)
          for (BaseSeason bs : ListUtils.nullSafe(traktShow.seasons)) {
            for (BaseEpisode be : ListUtils.nullSafe(bs.episodes)) {
              List<TvShowEpisode> matchingEpisodes = tmmShow.getEpisode(MetadataUtil.unboxInteger(bs.number, -1),
                  MetadataUtil.unboxInteger(be.number, -1));
              for (TvShowEpisode tmmEp : matchingEpisodes) {
                if (be.collected_at != null) {
                  Date collectedAt = DateTimeUtils.toDate(be.collected_at.toInstant());
                  if (!collectedAt.equals(tmmEp.getDateAdded())) {
                    tmmEp.setDateAdded(collectedAt);
                    tmmEp.writeNFO();
                    tmmEp.saveToDb();
                  }
                }
              }
            }
          }

          if (showDirty) {
            tmmShow.writeNFO();
            tmmShow.saveToDb();
          }
        }
        else {
          LOGGER.trace("Skipping show '{}' from Trak.tv collection sync, because it is locked!", tmmShow.getTitle());
        }
      }
    }

    // *****************************************************************************
    // 2) add all our shows to Trakt collection (we have the physical file)
    // *****************************************************************************
    LOGGER.debug("Adding up to {} TV shows to Trakt.tv collection", tvShows.size());
    // send show per show; sending all together may result too often in a timeout
    for (TvShow tmmShow : tvShows) {
      SyncShow syncShow = toSyncShow(tmmShow, false, traktShows);
      if (syncShow == null) {
        continue;
      }

      try {
        SyncItems items = prepareSyncItems(syncShow);
        SyncResponse response = executeCall(api.sync().addItemsToCollection(items));
        LOGGER.debug("Trakt add-to-library status: {}", tmmShow.getTitle());
        printStatus(response);
      }
      catch (Exception e) {
        LOGGER.error("failed syncing trakt: {}", e.getMessage());
        return;
      }
    }
  }

  void syncTraktTvShowWatched(List<TvShow> tvShowsInTmm) {
    // create a local copy of the list
    List<TvShow> tvShows = new ArrayList<>(tvShowsInTmm);

    List<BaseShow> traktShows;
    try {
      // Extended.DEFAULT adds url, poster, fanart, banner, genres
      // Extended.MAX adds certs, runtime, and other stuff (useful for scraper!)
      traktShows = executeCall(api.sync().watchedShows(null));
    }
    catch (Exception e) {
      LOGGER.error("failed syncing trakt: {}", e.getMessage());
      return;
    }

    LOGGER.info("You have {} TvShows marked as watched on Trakt.tv", traktShows.size());
    for (BaseShow traktShow : traktShows) {
      List<TvShow> matchingTmmTvShows = getTmmTvShowForTraktShow(tvShows, traktShow.show);

      for (TvShow tmmShow : matchingTmmTvShows) {
        if (!tmmShow.isLocked()) {
          // update show IDs from trakt
          boolean showDirty = updateIDs(tmmShow, traktShow.show);

          // update collection date from trakt (episodes)
          for (BaseSeason bs : ListUtils.nullSafe(traktShow.seasons)) {
            for (BaseEpisode be : ListUtils.nullSafe(bs.episodes)) {
              List<TvShowEpisode> matchingEpisodes = tmmShow.getEpisode(MetadataUtil.unboxInteger(bs.number, -1),
                  MetadataUtil.unboxInteger(be.number, -1));
              for (TvShowEpisode tmmEp : matchingEpisodes) {
                boolean epDirty = false;
                if (!tmmEp.isWatched()) {
                  LOGGER.trace("Marking episode '{}' as watched", tmmEp.getTitle());
                  tmmEp.setWatched(true);
                  epDirty = true;
                }
                if (tmmEp.getPlaycount() != MetadataUtil.unboxInteger(be.plays)) {
                  tmmEp.setPlaycount(MetadataUtil.unboxInteger(be.plays));
                  epDirty = true;
                }

                if (be.last_watched_at != null) {
                  Date lastWatchedAt = DateTimeUtils.toDate(be.last_watched_at.toInstant());
                  if (!lastWatchedAt.equals(tmmEp.getLastWatched())) {
                    tmmEp.setLastWatched(lastWatchedAt);
                    epDirty = true;
                  }
                }

                if (epDirty) {
                  tmmEp.writeNFO();
                  tmmEp.setLastWatched(null); // write date to NFO, but do not save it!
                  tmmEp.saveToDb();
                }
              }
            }
          }

          if (showDirty) {
            tmmShow.writeNFO();
            tmmShow.saveToDb();
          }
        }
        else {
          LOGGER.trace("Skipping show '{}' from Trak.tv watched sync, because it is locked!", tmmShow.getTitle());
        }
      }
    }

    // *****************************************************************************
    // 2) add all our shows to Trakt watched
    // *****************************************************************************
    LOGGER.debug("Adding up to {} TV shows as watched on Trakt.tv", tvShows.size());
    // send show per show; sending all together may result too often in a timeout
    for (TvShow tmmShow : tvShows) {
      // get items to sync
      SyncShow syncShow = toSyncShow(tmmShow, true, traktShows);
      if (syncShow == null) {
        continue;
      }

      try {
        SyncItems items = prepareSyncItems(syncShow);

        SyncResponse response = executeCall(api.sync().addItemsToWatchedHistory(items));
        LOGGER.debug("Trakt add-to-watched status: {}", tmmShow.getTitle());
        printStatus(response);
      }
      catch (Exception e) {
        LOGGER.error("failed syncing trakt: {}", e.getMessage());
        return;
      }
    }
  }

  /**
   * prepare the {@link SyncItems} object. Trakt.tv behaves differently when sending episodes inside a season vs directly inside the
   * {@link SyncItems}. If we have IDs for our episodes, we need to put them inside the {@link SyncItems} (otherwise Trakt.tv does not honor the IDs)
   * 
   * @param syncShow
   *          the previously prepared {@link SyncShow}
   * @return the {@link SyncItems} object
   */
  private SyncItems prepareSyncItems(SyncShow syncShow) {
    SyncItems syncItems = new SyncItems();

    // split fully qualified episodes into their own place
    List<SyncEpisode> syncEpisodes = new ArrayList<>();
    for (SyncSeason season : ListUtils.nullSafe(syncShow.seasons)) {
      for (SyncEpisode episode : ListUtils.nullSafe(season.episodes)) {
        if (episode.ids != null) {
          syncEpisodes.add(episode);
        }
      }
      if (season.episodes != null) {
        season.episodes.removeAll(syncEpisodes);
      }
    }
    if (syncShow.seasons != null) {
      syncShow.seasons
          .removeAll(syncShow.seasons.stream().filter(season -> season.episodes == null || season.episodes.isEmpty()).collect(Collectors.toList()));
    }
    if (syncShow.seasons != null && !syncShow.seasons.isEmpty()) {
      syncItems.shows(syncShow);
    }

    if (!syncEpisodes.isEmpty()) {
      syncItems.episodes(syncEpisodes);
    }

    return syncItems;
  }

  void syncTraktTvShowRating(List<TvShow> tvShowsInTmm) {
    // create a local copy of the list
    List<TvShow> tvShows = new ArrayList<>(tvShowsInTmm);

    // *****************************************************************************
    // 1) get all Trakt shows/episodes and update our items without a personal rating
    // *****************************************************************************
    List<RatedShow> traktShows;
    try {
      // Extended.DEFAULT adds url, poster, fanart, banner, genres
      // Extended.MAX adds certs, runtime, and other stuff (useful for scraper!)
      traktShows = executeCall(api.sync().ratingsShows(RatingsFilter.ALL, null, null, null));
    }
    catch (Exception e) {
      LOGGER.error("failed syncing trakt: {}", e.getMessage());
      return;
    }

    List<RatedEpisode> traktEpisodes;
    try {
      // Extended.DEFAULT adds url, poster, fanart, banner, genres
      // Extended.MAX adds certs, runtime, and other stuff (useful for scraper!)
      traktEpisodes = executeCall(api.sync().ratingsEpisodes(RatingsFilter.ALL, null, null, null));
    }
    catch (Exception e) {
      LOGGER.error("failed syncing trakt: {}", e.getMessage());
      return;
    }

    LOGGER.info("You have {} rated TV shows in your Trakt.tv collection", traktShows.size());
    LOGGER.info("You have {} rated episodes in your Trakt.tv collection", traktEpisodes.size());

    for (RatedShow traktShow : traktShows) {
      if (traktShow.show == null || traktShow.rating == null) {
        continue;
      }

      List<TvShow> matchingTmmTvShows = getTmmTvShowForTraktShow(tvShows, traktShow.show);

      for (TvShow tmmShow : matchingTmmTvShows) {
        if (!tmmShow.isLocked()) {
          // update show IDs from trakt
          boolean dirty = updateIDs(tmmShow, traktShow.show);

          MediaRating userRating = tmmShow.getUserRating();
          if (userRating == MediaMetadata.EMPTY_RATING) {
            tmmShow.setRating(new MediaRating(MediaRating.USER, traktShow.rating.value, 1, 10));
            dirty = true;
          }

          if (dirty) {
            tmmShow.writeNFO();
            tmmShow.saveToDb();
          }
        }
        else {
          LOGGER.trace("Skipping show '{}' from Trak.tv ratings sync, because it is locked!", tmmShow.getTitle());
        }
      }
    }

    for (RatedEpisode traktEpisode : traktEpisodes) {
      if (traktEpisode.show == null || traktEpisode.episode == null || traktEpisode.rating == null) {
        continue;
      }

      List<TvShow> matchingTmmTvShows = getTmmTvShowForTraktShow(tvShows, traktEpisode.show);

      for (TvShow tmmShow : matchingTmmTvShows) {
        List<TvShowEpisode> matchingEpisodes = tmmShow.getEpisode(MetadataUtil.unboxInteger(traktEpisode.episode.number, -1),
            MetadataUtil.unboxInteger(traktEpisode.episode.season, -1));

        for (TvShowEpisode tmmEpisode : matchingEpisodes) {
          if (!tmmEpisode.isLocked()) {
            // update show IDs from trakt
            boolean dirty = false;

            MediaRating userRating = tmmEpisode.getUserRating();
            if (userRating == MediaMetadata.EMPTY_RATING) {
              tmmEpisode.setRating(new MediaRating(MediaRating.USER, traktEpisode.rating.value, 1, 10));
              dirty = true;
            }

            if (dirty) {
              tmmEpisode.writeNFO();
              tmmEpisode.saveToDb();
            }
          }
          else {
            LOGGER.trace("Skipping episode '{}' from Trak.tv ratings sync, because it is locked!", tmmEpisode.getTitle());
          }
        }
      }
    }

    // *****************************************************************************
    // 2) add all our shows/episodes rating to Trakt.tv
    // not rated shows are not returned in the API even there are rated episodes
    // *****************************************************************************
    LOGGER.debug("Adding up to {} TV shows with personal rating on Trakt.tv", tvShows.size());
    // send show per show; sending all together may result too often in a timeout
    for (TvShow tmmShow : tvShows) {
      // get items to sync
      SyncShow syncShow = toSyncShow(tmmShow, traktShows, traktEpisodes);
      if (syncShow == null) {
        continue;
      }

      try {
        SyncItems items = prepareSyncItems(syncShow);
        SyncResponse response = executeCall(api.sync().addRatings(items));
        LOGGER.debug("Trakt add-ratings status: {}", tmmShow.getTitle());
        printStatus(response);
      }
      catch (Exception e) {
        LOGGER.error("failed syncing trakt: {}", e.getMessage());
        return;
      }
    }
  }

  void removeFromTraktCollection(TvShow tmmShow) {
    removeFromTraktCollection(Collections.singletonList(tmmShow));
  }

  void removeFromTraktCollection(List<TvShow> tmmShows) {
    List<SyncShow> syncShows = new ArrayList<>();
    for (TvShow tmmShow : tmmShows) {
      LOGGER.debug("Going to remove from your Trakt.tv collection: {}", tmmShow.getTitle());
      SyncShow sync = toSyncShow(tmmShow, false, Collections.emptyList());
      syncShows.add(sync);
    }

    try {
      SyncItems items = new SyncItems().shows(syncShows);
      SyncResponse response = executeCall(api.sync().deleteItemsFromCollection(items));
      printStatus(response);
    }
    catch (Exception e) {
      LOGGER.error("Failed removing from Trakt.tv collection: {}", e.getMessage());
    }
  }

  void removeFromTraktWatched(TvShow tmmShow) {
    removeFromTraktWatched(Collections.singletonList(tmmShow));
  }

  void removeFromTraktWatched(List<TvShow> tmmShows) {
    List<SyncShow> syncShows = new ArrayList<>();
    for (TvShow tmmShow : tmmShows) {
      LOGGER.debug("Going to remove from your Trakt.tv collection: {}", tmmShow.getTitle());
      SyncShow sync = toSyncShow(tmmShow, true, Collections.emptyList());
      syncShows.add(sync);
    }

    try {
      SyncItems items = new SyncItems().shows(syncShows);
      SyncResponse response = executeCall(api.sync().deleteItemsFromWatchedHistory(items));
      printStatus(response);
    }
    catch (Exception e) {
      LOGGER.error("Failed removing from Trakt.tv collection: {}", e.getMessage());
    }
  }

  /**
   * clears the whole Trakt.tv TV show collection. Gets all Trakt.tv TV shows from your collection and removes them from the collection and the
   * watched state; a little helper to initialize the collection
   */
  void clearTraktTvShows() {
    // *****************************************************************************
    // 1) get ALL Trakt shows in collection / watched
    // *****************************************************************************
    List<BaseShow> traktCollection;
    List<BaseShow> traktWatched;
    try {
      traktCollection = executeCall(api.sync().collectionShows(null));
      traktWatched = executeCall(api.sync().watchedShows(null));
    }
    catch (Exception e) {
      LOGGER.error("failed syncing trakt: {}", e.getMessage());
      return;
    }
    LOGGER.info("You have {} shows in your Trakt.tv collection", traktCollection.size());
    LOGGER.info("You have {} shows watched", traktWatched.size());

    // *****************************************************************************
    // 2) remove every shows from the COLLECTION state
    // *****************************************************************************
    List<SyncShow> showToRemove = new ArrayList<>();
    for (BaseShow traktShow : traktCollection) {
      showToRemove.add(toSyncItems(traktShow));
    }
    if (!showToRemove.isEmpty()) {
      try {
        SyncItems items = new SyncItems().shows(showToRemove);
        SyncResponse response = executeCall(api.sync().deleteItemsFromCollection(items));
        LOGGER.debug("Trakt delete-items-from-collection status:");
        printStatus(response);
      }
      catch (Exception e) {
        LOGGER.error("failed syncing trakt: {}", e.getMessage());
        return;
      }
    }

    // *****************************************************************************
    // 3) remove every shows from the WATCHED state
    // *****************************************************************************
    showToRemove.clear();
    for (BaseShow traktShow : traktWatched) {
      showToRemove.add(toSyncItems(traktShow));
    }
    if (!showToRemove.isEmpty()) {
      try {
        SyncItems items = new SyncItems().shows(showToRemove);
        SyncResponse response = executeCall(api.sync().deleteItemsFromWatchedHistory(items));
        LOGGER.debug("Trakt delete-items-from-watched-history status:");
        printStatus(response);
      }
      catch (Exception e) {
        LOGGER.error("failed syncing trakt: {}", e.getMessage());
      }
    }
  }

  private List<TvShow> getTmmTvShowForTraktShow(List<TvShow> tmmTvShows, Show traktShow) {
    return tmmTvShows.stream().filter(tvShow -> matches(tvShow, traktShow)).collect(Collectors.toList());
  }

  private boolean matches(TvShow tmmShow, Show traktShow) {
    if (traktShow == null || traktShow.ids == null) {
      return false;
    }

    ShowIds ids = traktShow.ids;

    int traktId = MetadataUtil.unboxInteger(ids.trakt);
    if (traktId > 0 && traktId == tmmShow.getIdAsInt(MediaMetadata.TRAKT_TV)) {
      return true;
    }

    if (StringUtils.isNotBlank(ids.imdb) && ids.imdb.equals(tmmShow.getImdbId())) {
      return true;
    }

    int tmdbId = MetadataUtil.unboxInteger(ids.tmdb);
    if (tmdbId > 0 && tmdbId == tmmShow.getTmdbId()) {
      return true;
    }

    int tvdbId = MetadataUtil.unboxInteger(ids.tvdb);
    if (tvdbId > 0 && tvdbId == tmmShow.getIdAsInt(MediaMetadata.TVDB)) {
      return true;
    }

    // not used atm
    int tvrageId = MetadataUtil.unboxInteger(ids.tvrage);
    if (tvrageId > 0 && tvrageId == tmmShow.getIdAsInt(MediaMetadata.TVRAGE)) {
      return true;
    }

    return false;
  }

  private boolean updateIDs(TvShow tmmShow, Show traktShow) {
    boolean dirty = false;
    if (traktShow == null || traktShow.ids == null) {
      return dirty;
    }

    ShowIds ids = traktShow.ids;

    if (StringUtils.isBlank(tmmShow.getIdAsString(MediaMetadata.IMDB)) && !StringUtils.isBlank(ids.imdb)) {
      tmmShow.setId(MediaMetadata.IMDB, ids.imdb);
      dirty = true;
    }
    if (tmmShow.getIdAsInt(MediaMetadata.TMDB) == 0 && MetadataUtil.unboxInteger(ids.tmdb) > 0) {
      tmmShow.setId(MediaMetadata.TMDB, ids.tmdb);
      dirty = true;
    }
    if (tmmShow.getIdAsInt(MediaMetadata.TRAKT_TV) == 0 && MetadataUtil.unboxInteger(ids.trakt) > 0) {
      tmmShow.setId(MediaMetadata.TRAKT_TV, ids.trakt);
      dirty = true;
    }
    if (tmmShow.getIdAsInt(MediaMetadata.TVDB) == 0 && MetadataUtil.unboxInteger(ids.tvdb) > 0) {
      tmmShow.setId(MediaMetadata.TVDB, ids.tvdb);
      dirty = true;
    }
    if (tmmShow.getIdAsInt(MediaMetadata.TVRAGE) == 0 && MetadataUtil.unboxInteger(ids.tvrage) > 0) {
      tmmShow.setId(MediaMetadata.TVRAGE, ids.tvrage);
      dirty = true;
    }

    return dirty;
  }

  private SyncShow toSyncShow(TvShow tmmShow, boolean watched, List<BaseShow> showsInTrakt) {
    boolean hasId = false;

    ShowIds ids = new ShowIds();
    if (MediaIdUtil.isValidImdbId(tmmShow.getImdbId())) {
      ids.imdb = tmmShow.getImdbId();
      hasId = true;
    }

    if (tmmShow.getTmdbId() > 0) {
      ids.tmdb = tmmShow.getTmdbId();
      hasId = true;
    }

    if (tmmShow.getIdAsInt(MediaMetadata.TVDB) > 0) {
      ids.tvdb = tmmShow.getIdAsInt(MediaMetadata.TVDB);
      hasId = true;
    }

    if (tmmShow.getIdAsInt(MediaMetadata.TRAKT_TV) > 0) {
      ids.trakt = tmmShow.getIdAsInt(MediaMetadata.TRAKT_TV);
      hasId = true;
    }

    if (tmmShow.getIdAsInt(MediaMetadata.TVRAGE) > 0) {
      ids.tvrage = tmmShow.getIdAsInt(MediaMetadata.TVRAGE);
      hasId = true;
    }

    if (!hasId) {
      return null;
    }

    // search for all seasons/episodes of this show
    List<SyncSeason> syncSeasons = new ArrayList<>();
    for (TvShowSeason tmmSeason : tmmShow.getSeasons()) {
      if (tmmSeason.getSeason() < 0) {
        continue;
      }

      // since there can be multiple versions of the same episode, we force to take the first one for trakt sync AND combine all watched states via
      // logical OR
      Map<String, SyncEpisode> syncEpisodeMap = new HashMap<>();
      for (TvShowEpisode tmmEp : tmmSeason.getEpisodes()) {
        if (tmmEp.getEpisode() < 0 || tmmEp.getSeason() < 0) {
          continue;
        }

        String episodeTag = "S" + tmmEp.getSeason() + "E" + tmmEp.getEpisode();

        // we have to decide what we send; trakt behaves differently when sending data to
        // sync collection and sync history.
        if (watched) {
          // sync history
          if (!syncEpisodeMap.containsKey(episodeTag) && tmmEp.isWatched()) {
            // watched in tmm and not in trakt -> sync
            OffsetDateTime watchedAt = OffsetDateTime.ofInstant(DateTimeUtils.toInstant(new Date()), ZoneOffset.UTC);
            syncEpisodeMap.put(episodeTag, toSyncEpisode(tmmEp).watchedAt(watchedAt));
          }
        }
        else {
          // sync collection
          if (!syncEpisodeMap.containsKey(episodeTag)) {
            OffsetDateTime collectedAt = OffsetDateTime.ofInstant(DateTimeUtils.toInstant(getDateField(tmmEp)), ZoneOffset.UTC);
            syncEpisodeMap.put(episodeTag, toSyncEpisode(tmmEp).collectedAt(collectedAt));
          }
        }
      }

      if (!syncEpisodeMap.isEmpty()) {
        // add all episodes to the season
        syncSeasons.add(new SyncSeason().number(tmmSeason.getSeason()).episodes(new ArrayList<>(syncEpisodeMap.values())));
      }
    }

    if (syncSeasons.isEmpty()) {
      return null;
    }

    // now do a match with the existing data to send a delta

    // 1. search for the show
    BaseShow showInTrakt = null;

    for (BaseShow show : showsInTrakt) {
      if (matches(tmmShow, show.show)) {
        showInTrakt = show;
        break;
      }
    }

    if (showInTrakt == null) {
      // not yet in trakt.tv -> full sync possible
      return new SyncShow().id(ids).seasons(syncSeasons);
    }
    else {
      // show already in trakt.tv -> delta sync
      List<SyncSeason> syncSeasonsDelta = new ArrayList<>();
      for (SyncSeason syncSeason : syncSeasons) {
        List<SyncEpisode> syncEpisodesDelta = new ArrayList<>();
        for (SyncEpisode syncEpisode : ListUtils.nullSafe(syncSeason.episodes)) {
          if (!containsEpisode(showInTrakt, syncEpisode, watched)) {
            syncEpisodesDelta.add(syncEpisode);
          }
        }

        if (!syncEpisodesDelta.isEmpty()) {
          syncSeasonsDelta.add(new SyncSeason().number(syncSeason.number).episodes(syncEpisodesDelta));
        }
      }

      if (!syncSeasonsDelta.isEmpty()) {
        return new SyncShow().id(ids).seasons(syncSeasonsDelta);
      }
    }

    // if nothing added, do NOT send an empty show (to add all)
    return null;
  }

  private boolean containsEpisode(BaseShow show, SyncEpisode syncEpisode, boolean watched) {
    int seasonNumber = MetadataUtil.unboxInteger(syncEpisode.season);
    int episodeNumber = MetadataUtil.unboxInteger(syncEpisode.number);

    for (BaseSeason season : ListUtils.nullSafe(show.seasons)) {
      if (seasonNumber == MetadataUtil.unboxInteger(season.number, -1)) {
        for (BaseEpisode episode : ListUtils.nullSafe(season.episodes)) {
          if (episodeNumber == MetadataUtil.unboxInteger(episode.number, -1)) {
            if (watched) {
              // for watched sync we do not need any further check
              return true;
            }
            else if (matchesMetadata(episode.metadata, syncEpisode)) {
              // for collection sync we also check the metadata
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  private SyncShow toSyncShow(TvShow tmmShow, List<RatedShow> showsInTrakt, List<RatedEpisode> episodesInTrakt) {
    boolean hasId = false;

    ShowIds ids = new ShowIds();
    if (MediaIdUtil.isValidImdbId(tmmShow.getImdbId())) {
      ids.imdb = tmmShow.getImdbId();
      hasId = true;
    }

    if (tmmShow.getTmdbId() > 0) {
      ids.tmdb = tmmShow.getTmdbId();
      hasId = true;
    }

    if (tmmShow.getIdAsInt(MediaMetadata.TVDB) > 0) {
      ids.tvdb = tmmShow.getIdAsInt(MediaMetadata.TVDB);
      hasId = true;
    }

    if (tmmShow.getIdAsInt(MediaMetadata.TRAKT_TV) > 0) {
      ids.trakt = tmmShow.getIdAsInt(MediaMetadata.TRAKT_TV);
      hasId = true;
    }

    if (tmmShow.getIdAsInt(MediaMetadata.TVRAGE) > 0) {
      ids.tvrage = tmmShow.getIdAsInt(MediaMetadata.TVRAGE);
      hasId = true;
    }

    if (!hasId) {
      return null;
    }

    // search for all seasons/episodes of this show
    List<SyncSeason> syncSeasons = new ArrayList<>();
    for (TvShowSeason tmmSeason : tmmShow.getSeasons()) {
      // since there can be multiple versions of the same episode, we force to take the first one for trakt sync AND combine all watched states via
      // logical OR
      Map<String, SyncEpisode> syncEpisodeMap = new HashMap<>();
      for (TvShowEpisode tmmEp : tmmSeason.getEpisodes()) {
        if (tmmEp.getEpisode() < 0 || tmmEp.getSeason() < 0) {
          continue;
        }

        String episodeTag = "S" + tmmEp.getSeason() + "E" + tmmEp.getEpisode();

        MediaRating userRating = tmmEp.getUserRating();
        if (!syncEpisodeMap.containsKey(episodeTag) && userRating != MediaMetadata.EMPTY_RATING) {
          // rated in tmm
          syncEpisodeMap.put(episodeTag, toSyncEpisode(tmmEp).rating(Rating.fromValue(Math.round(userRating.getRating())))
              .ratedAt(OffsetDateTime.ofInstant(DateTimeUtils.toInstant(new Date()), ZoneOffset.UTC)));
        }
      }

      // add all episodes to the season
      syncSeasons.add(new SyncSeason().number(tmmSeason.getSeason()).episodes(new ArrayList<>(syncEpisodeMap.values())));
    }

    // now do a match with the existing data to send a delta
    Map<String, RatedEpisode> episodesForShow = new HashMap<>();
    for (RatedEpisode ratedEpisode : episodesInTrakt) {
      if (ratedEpisode.episode != null && matches(tmmShow, ratedEpisode.show)) {
        String key = "S" + ratedEpisode.episode.season + "E" + ratedEpisode.episode.number;
        episodesForShow.put(key, ratedEpisode);
      }
    }

    // show already in trakt.tv -> delta sync
    List<SyncSeason> syncSeasonsDelta = new ArrayList<>();
    for (SyncSeason syncSeason : syncSeasons) {
      List<SyncEpisode> syncEpisodesDelta = new ArrayList<>();
      for (SyncEpisode syncEpisode : ListUtils.nullSafe(syncSeason.episodes)) {
        String key = "S" + syncEpisode.season + "E" + syncEpisode.number;
        RatedEpisode ratedEpisode = episodesForShow.get(key);

        if (ratedEpisode == null || ratedEpisode.rating == null || ratedEpisode.rating.value != syncEpisode.rating.value) {
          syncEpisodesDelta.add(syncEpisode);
        }
      }

      if (!syncEpisodesDelta.isEmpty()) {
        syncSeasonsDelta.add(new SyncSeason().number(syncSeason.number).episodes(syncEpisodesDelta));
      }
    }

    if (!syncSeasonsDelta.isEmpty()) {
      return new SyncShow().id(ids).seasons(syncSeasons);
    }

    // if nothing added, do NOT send an empty show (to add all)
    return null;
  }

  private SyncShow toSyncItems(BaseShow baseShow) {
    // TODO: used only on clear() - so we don't need the episodes? TBC
    ArrayList<SyncSeason> ss = new ArrayList<>();
    for (BaseSeason baseSeason : baseShow.seasons) {
      ArrayList<SyncEpisode> se = new ArrayList<>();
      for (BaseEpisode baseEp : baseSeason.episodes) {
        se.add(new SyncEpisode().number(baseEp.number).collectedAt(baseEp.collected_at).watchedAt(baseEp.collected_at));
      }
      ss.add(new SyncSeason().number(baseSeason.number).episodes(se));
    }
    return new SyncShow().id(baseShow.show.ids).collectedAt(baseShow.last_collected_at).watchedAt(baseShow.last_watched_at).seasons(ss);
  }

  private SyncEpisode toSyncEpisode(TvShowEpisode episode) {
    SyncEpisode syncEpisode = new SyncEpisode();
    EpisodeIds ids = new EpisodeIds();

    // try to sync by id
    int tmdbId = MediaIdUtil.getIdAsIntOrDefault(episode.getIds(), MediaMetadata.TMDB, 0);
    if (tmdbId > 0) {
      ids.tmdb = tmdbId;
    }

    int tvdbId = MediaIdUtil.getIdAsIntOrDefault(episode.getIds(), MediaMetadata.TVDB, 0);
    if (tvdbId > 0) {
      ids.tvdb = tvdbId;
    }

    int traktId = MediaIdUtil.getIdAsIntOrDefault(episode.getIds(), MediaMetadata.TRAKT_TV, 0);
    if (traktId > 0) {
      ids.trakt = traktId;
    }

    String imdbId = MediaIdUtil.getIdAsString(episode.getIds(), MediaMetadata.IMDB);
    if (MediaIdUtil.isValidImdbId(imdbId)) {
      ids.imdb = imdbId;
    }

    if (ids.tmdb != null || ids.tvdb != null || ids.trakt != null || ids.imdb != null) {
      syncEpisode.id(ids);
    }

    syncEpisode.number(episode.getEpisode());
    syncEpisode.season(episode.getSeason());

    // also sync mediainfo
    syncEpisode.mediaType(getMediaType(episode.getMediaInfoSource()));
    syncEpisode.resolution(getResolution(episode.getMainVideoFile()));
    syncEpisode.hdr(getHdr(episode.getVideoHDRFormat()));
    syncEpisode.audio(getAudio(episode.getMediaInfoAudioCodec()));
    syncEpisode.audioChannels(getAudioChannels(episode.getMainVideoFile().getAudioChannelCount()));
    syncEpisode.is3d(episode.isVideoIn3D());

    return syncEpisode;
  }

  static boolean matchesMetadata(Metadata metadata, SyncEpisode episode) {
    if (metadata == null) {
      return false;
    }

    if (metadata.is3d == null || !metadata.is3d.equals(episode.is3d)) {
      return false;
    }

    if (metadata.audio != episode.audio) {
      return false;
    }

    if (metadata.media_type != episode.media_type) {
      return false;
    }

    if (metadata.resolution != episode.resolution) {
      return false;
    }
    if (metadata.audio_channels != episode.audio_channels) {
      return false;
    }

    if (metadata.hdr != episode.hdr) {
      return false;
    }

    return true;
  }

  private Date getDateField(TvShowEpisode episode) {
    Date collectedAt = null;

    switch (Settings.getInstance().getTraktDateField()) {
      case DATE_ADDED:
        collectedAt = episode.getDateAdded();
        break;

      case FILE_CREATION_DATE:
        collectedAt = episode.getMainVideoFile().getDateCreated();
        break;

      case FILE_LAST_MODIFIED_DATE:
        collectedAt = episode.getMainVideoFile().getDateLastModified();
        break;

      case RELEASE_DATE:
        collectedAt = episode.getReleaseDate();
        break;
    }

    if (collectedAt == null) {
      // fallback
      collectedAt = episode.getDateAddedForUi();
    }

    // sanity check - must not sync a date in the future
    Date now = new Date();
    if (now.before(collectedAt)) {
      collectedAt = episode.getDateAdded();
    }

    return collectedAt;
  }
}
