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
package org.tinymediamanager.scraper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.anidb.AniDbMovieMetadataProvider;
import org.tinymediamanager.scraper.anidb.AniDbTvShowMetadataProvider;
import org.tinymediamanager.scraper.fanarttv.FanartTvMovieArtworkProvider;
import org.tinymediamanager.scraper.fanarttv.FanartTvTvShowArtworkProvider;
import org.tinymediamanager.scraper.ffmpeg.FFmpegMovieArtworkProvider;
import org.tinymediamanager.scraper.ffmpeg.FFmpegTvShowArtworkProvider;
import org.tinymediamanager.scraper.hdtrailersnet.HdTrailersNetMovieTrailerProvider;
import org.tinymediamanager.scraper.imdb.ImdbMovieArtworkProvider;
import org.tinymediamanager.scraper.imdb.ImdbMovieMetadataProvider;
import org.tinymediamanager.scraper.imdb.ImdbMovieTrailerProvider;
import org.tinymediamanager.scraper.imdb.ImdbTvShowArtworkProvider;
import org.tinymediamanager.scraper.imdb.ImdbTvShowMetadataProvider;
import org.tinymediamanager.scraper.imdb.ImdbTvShowTrailerProvider;
import org.tinymediamanager.scraper.interfaces.IKodiMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieSubtitleProvider;
import org.tinymediamanager.scraper.interfaces.IMovieTrailerProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowSubtitleProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowTrailerProvider;
import org.tinymediamanager.scraper.kodi.KodiMetadataProvider;
import org.tinymediamanager.scraper.moviemeter.MovieMeterMovieMetadataProvider;
import org.tinymediamanager.scraper.mpdbtv.MpdbMovieArtworkMetadataProvider;
import org.tinymediamanager.scraper.mpdbtv.MpdbMovieMetadataProvider;
import org.tinymediamanager.scraper.ofdb.OfdbMovieMetadataProvider;
import org.tinymediamanager.scraper.omdb.OmdbMovieMetadataProvider;
import org.tinymediamanager.scraper.omdb.OmdbTvShowMetadataProvider;
import org.tinymediamanager.scraper.opensubtitles_com.OpenSubtitlesComMovieSubtitleProvider;
import org.tinymediamanager.scraper.opensubtitles_com.OpenSubtitlesComTvShowSubtitleProvider;
import org.tinymediamanager.scraper.spi.IAddonProvider;
import org.tinymediamanager.scraper.thetvdb.TheTvDbMovieArtworkProvider;
import org.tinymediamanager.scraper.thetvdb.TheTvDbMovieMetadataProvider;
import org.tinymediamanager.scraper.thetvdb.TheTvDbTvShowArtworkProvider;
import org.tinymediamanager.scraper.thetvdb.TheTvDbTvShowMetadataProvider;
import org.tinymediamanager.scraper.tmdb.TmdbMovieArtworkProvider;
import org.tinymediamanager.scraper.tmdb.TmdbMovieMetadataProvider;
import org.tinymediamanager.scraper.tmdb.TmdbMovieTrailerProvider;
import org.tinymediamanager.scraper.tmdb.TmdbTvShowArtworkProvider;
import org.tinymediamanager.scraper.tmdb.TmdbTvShowMetadataProvider;
import org.tinymediamanager.scraper.tmdb.TmdbTvShowTrailerProvider;
import org.tinymediamanager.scraper.trakt.TraktMovieMetadataProvider;
import org.tinymediamanager.scraper.trakt.TraktTvShowMetadataProvider;
import org.tinymediamanager.scraper.tvmaze.TvMazeTvShowMetadataProvider;
import org.tinymediamanager.scraper.universal_movie.UniversalMovieMetadataProvider;
import org.tinymediamanager.scraper.universal_tvshow.UniversalTvShowMetadataProvider;
import org.tinymediamanager.scraper.util.ListUtils;

/**
 * the class {@link MediaProviders} is used to manage all loaded {@link IMediaProvider}s.
 *
 * @author Manuel Laggner
 */
public class MediaProviders {
  private static final Logger                            LOGGER          = LoggerFactory.getLogger(MediaProviders.class);
  private static final Map<String, List<IMediaProvider>> MEDIA_PROVIDERS = new LinkedHashMap<>();

  private MediaProviders() {
    throw new IllegalAccessError();
  }

  /**
   * load all media providers
   */
  public static void loadMediaProviders() {
    // just call it once -> if the array has been filled before then exit
    if (!MEDIA_PROVIDERS.isEmpty()) {
      return;
    }

    // load the addons
    Set<Class<? extends IMediaProvider>> addons = new LinkedHashSet<>();
    Iterator<IAddonProvider> addonIterator = ServiceLoader.load(IAddonProvider.class).iterator();
    while (addonIterator.hasNext()) {
      try {
        addons.addAll(addonIterator.next().getAddonClasses());
      }
      catch (Exception | Error e) {
        LOGGER.error("Could not load addons - '{}'", e.getMessage());
      }
    }

    /////////////////////////////////////////////
    // MOVIE
    /////////////////////////////////////////////
    loadProvider(TmdbMovieMetadataProvider.class);
    loadProvider(ImdbMovieMetadataProvider.class);
    loadProvider(OmdbMovieMetadataProvider.class);
    loadProvider(TraktMovieMetadataProvider.class);
    loadProvider(MovieMeterMovieMetadataProvider.class);
    loadProvider(TheTvDbMovieMetadataProvider.class);
    loadProvider(AniDbMovieMetadataProvider.class);
    loadProvider(OfdbMovieMetadataProvider.class);
    loadProvider(MpdbMovieMetadataProvider.class);
    loadProvider(KodiMetadataProvider.class);

    // addons
    loadAddonsForInterface(addons, IMovieMetadataProvider.class);

    // register all compatible scrapers in the universal scraper
    MEDIA_PROVIDERS.forEach((key, value) -> {
      for (IMediaProvider mediaProvider : ListUtils.nullSafe(value)) {
        if (mediaProvider instanceof IMovieMetadataProvider) {
          UniversalMovieMetadataProvider.addProvider((IMovieMetadataProvider) mediaProvider);
        }
      }
    });

    // and finally add the universal scraper itself
    loadProvider(UniversalMovieMetadataProvider.class);

    /////////////////////////////////////////////
    // MOVIE ARTWORK
    /////////////////////////////////////////////
    loadProvider(TmdbMovieArtworkProvider.class);
    loadProvider(FanartTvMovieArtworkProvider.class);
    loadProvider(ImdbMovieArtworkProvider.class);
    loadProvider(MpdbMovieArtworkMetadataProvider.class);
    loadProvider(FFmpegMovieArtworkProvider.class);
    loadProvider(TheTvDbMovieArtworkProvider.class);

    // addons
    loadAddonsForInterface(addons, IMovieArtworkProvider.class);

    /////////////////////////////////////////////
    // MOVIE TRAILER
    /////////////////////////////////////////////
    loadProvider(TmdbMovieTrailerProvider.class);
    loadProvider(HdTrailersNetMovieTrailerProvider.class);
    loadProvider(ImdbMovieTrailerProvider.class);

    // addons
    loadAddonsForInterface(addons, IMovieTrailerProvider.class);

    /////////////////////////////////////////////
    // MOVIE SUBTITLES
    /////////////////////////////////////////////
    // loadProvider(OpenSubtitlesMovieSubtitleProvider.class); // obsolete as of 2024-01-01
    loadProvider(OpenSubtitlesComMovieSubtitleProvider.class);

    // addons
    loadAddonsForInterface(addons, IMovieSubtitleProvider.class);

    /////////////////////////////////////////////
    // TV SHOWS
    /////////////////////////////////////////////
    loadProvider(TheTvDbTvShowMetadataProvider.class);
    loadProvider(TmdbTvShowMetadataProvider.class);
    loadProvider(ImdbTvShowMetadataProvider.class);
    loadProvider(TraktTvShowMetadataProvider.class);
    loadProvider(AniDbTvShowMetadataProvider.class);
    loadProvider(TvMazeTvShowMetadataProvider.class);
    loadProvider(OmdbTvShowMetadataProvider.class);
    // loadProvider(TvdbV3TvShowMetadataProvider.class);

    // addons
    loadAddonsForInterface(addons, ITvShowMetadataProvider.class);

    // register all compatible scrapers in the universal scraper
    // FIX: to put tvdb in the front
    List<IMediaProvider> tvdb = MEDIA_PROVIDERS.get(MediaMetadata.TVDB);
    for (IMediaProvider mediaProvider : ListUtils.nullSafe(tvdb)) {
      if (mediaProvider instanceof ITvShowMetadataProvider) {
        UniversalTvShowMetadataProvider.addProvider((ITvShowMetadataProvider) mediaProvider);
      }
    }

    MEDIA_PROVIDERS.forEach((key, value) -> {
      for (IMediaProvider mediaProvider : ListUtils.nullSafe(value)) {
        if (mediaProvider instanceof ITvShowMetadataProvider) {
          UniversalTvShowMetadataProvider.addProvider((ITvShowMetadataProvider) mediaProvider);
        }
      }
    });

    // and finally add the universal scraper itself
    loadProvider(UniversalTvShowMetadataProvider.class);

    /////////////////////////////////////////////
    // TV SHOW ARTWORK
    /////////////////////////////////////////////
    loadProvider(TheTvDbTvShowArtworkProvider.class);
    loadProvider(FanartTvTvShowArtworkProvider.class);
    loadProvider(TmdbTvShowArtworkProvider.class);
    loadProvider(ImdbTvShowArtworkProvider.class);
    loadProvider(FFmpegTvShowArtworkProvider.class);
    // loadProvider(TvdbV3TvShowArtworkProvider.class);

    // addons
    loadAddonsForInterface(addons, ITvShowArtworkProvider.class);

    /////////////////////////////////////////////
    // TV SHOW TRAILER
    /////////////////////////////////////////////
    loadProvider(TmdbTvShowTrailerProvider.class);
    loadProvider(ImdbTvShowTrailerProvider.class);

    // addons
    loadAddonsForInterface(addons, ITvShowTrailerProvider.class);

    /////////////////////////////////////////////
    // TV SHOW SUBTITLES
    /////////////////////////////////////////////
    // loadProvider(OpenSubtitlesTvShowSubtitleProvider.class); // obsolete as of 2024-01-01
    loadProvider(OpenSubtitlesComTvShowSubtitleProvider.class);

    // addons
    loadAddonsForInterface(addons, ITvShowSubtitleProvider.class);
  }

  private static void loadAddonsForInterface(Collection<Class<? extends IMediaProvider>> addons, Class<? extends IMediaProvider> iface) {
    for (Class<? extends IMediaProvider> addon : addons) {
      if (iface.isAssignableFrom(addon)) {
        try {
          loadProvider(addon);
        }
        catch (Exception | Error e) {
          LOGGER.error("Could not load addon '{}' - '{}'", addon.getName(), e.getMessage());
        }
      }
    }
  }

  private static void loadProvider(Class<? extends IMediaProvider> clazz) {
    try {
      IMediaProvider provider = clazz.getDeclaredConstructor().newInstance();

      // add the provider to our list of supported providers
      List<IMediaProvider> providers = MEDIA_PROVIDERS.computeIfAbsent(provider.getId(), k -> new ArrayList<>());
      providers.add(provider);
    }
    catch (Exception e) {
      LOGGER.error("could not load media provider {} - {}", clazz.getName(), e.getMessage());
    }
  }

  /**
   * get a list of all available media providers for the given interface
   *
   * @param clazz
   *          the interface which needs to be implemented
   * @param <T>
   *          the type of the interface
   * @return a list of all media providers which implements the given interface
   */
  public static <T extends IMediaProvider> List<T> getProvidersForInterface(Class<T> clazz) {
    List<T> providers = new ArrayList<>();

    MEDIA_PROVIDERS.forEach((key, value) -> {
      for (IMediaProvider provider : value) {
        if (clazz.isAssignableFrom(provider.getClass())) {
          providers.add((T) provider);
        }
      }
    });

    return providers;
  }

  /**
   * get the media provider by the given id
   *
   * @param id
   *          the id of the media provider
   * @return the {@link IMediaProvider} or null
   */
  public static <T extends IMediaProvider> T getProviderById(String id, Class<T> clazz) {
    if (StringUtils.isBlank(id)) {
      return null;
    }

    List<IMediaProvider> providers = MEDIA_PROVIDERS.get(id);

    IMediaProvider mp = null;
    for (IMediaProvider mediaProvider : ListUtils.nullSafe(providers)) {
      if (clazz.isAssignableFrom(mediaProvider.getClass())) {
        return (T) mediaProvider;
      }
    }

    // no media provider found? maybe a kodi one
    if (mp == null) {
      for (IKodiMetadataProvider kodi : MediaProviders.getProvidersForInterface(IKodiMetadataProvider.class)) {
        mp = kodi.getPluginById(id);
        if (mp != null) {
          break;
        }
      }
    }

    return (T) mp;
  }
}
