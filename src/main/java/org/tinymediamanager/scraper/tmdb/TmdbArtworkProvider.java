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
package org.tinymediamanager.scraper.tmdb;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.tmdb.entities.AppendToResponse;
import org.tinymediamanager.scraper.tmdb.entities.Image;
import org.tinymediamanager.scraper.tmdb.entities.Images;
import org.tinymediamanager.scraper.tmdb.entities.TvShow;
import org.tinymediamanager.scraper.tmdb.enumerations.AppendToResponseItem;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MetadataUtil;

/**
 * The class TmdbArtworkProvider. For managing all artwork provided tasks with tmdb
 */
class TmdbArtworkProvider {
  private static final Logger  LOGGER = LoggerFactory.getLogger(TmdbArtworkProvider.class);

  private final TmdbController api;
  private final String         baseUrl;

  public TmdbArtworkProvider(TmdbController api, String baseUrl) {
    this.api = api;
    this.baseUrl = baseUrl;
  }

  /**
   * get the artwork for the given type/id
   *
   * @param options
   *          the options for getting the artwork
   * @return a list of all found artworks
   * @throws ScrapeException
   *           any exception which can be thrown while scraping
   */
  List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getArtwork(): {}", options);
    MediaArtwork.MediaArtworkType artworkType = options.getArtworkType();

    int tmdbId = options.getTmdbId();
    String imdbId = options.getImdbId();

    // for movie sets we need another if
    if (options.getMediaType() == MediaType.MOVIE_SET && options.getIdAsInt(MediaMetadata.TMDB_SET) > 0) {
      tmdbId = options.getIdAsInt(MediaMetadata.TMDB_SET);
    }

    if (tmdbId == 0 && StringUtils.isNotEmpty(imdbId)) {
      // try to get tmdbId via imdbId
      try {
        tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, options.getMediaType(), imdbId);
      }
      catch (Exception e) {
        LOGGER.debug("could not get tmdb from imdb - '{}'", e.getMessage());
      }
    }

    if (tmdbId == 0) {
      LOGGER.warn("Cannot get artwork - neither imdb/tmdb set");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    List<MediaArtwork> artwork = null;
    try {
      // posters and fanart
      switch (options.getMediaType()) {
        case MOVIE:
          artwork = prepareArtwork(api.moviesService().images(tmdbId, null).execute().body(), artworkType, tmdbId, options);
          break;

        case MOVIE_SET:
          artwork = prepareArtwork(api.collectionService().images(tmdbId, null).execute().body(), artworkType, tmdbId, options);
          break;

        case TV_SHOW:
          // here we need to do a fetch of the base details to get the season count for all season related artwork
          if (artworkType == MediaArtworkType.ALL || artworkType == MediaArtworkType.SEASON_POSTER) {
            TvShow tvShow = api.tvService().tv(tmdbId, null, new AppendToResponse(AppendToResponseItem.IMAGES)).execute().body();
            if (tvShow != null) {
              artwork = prepareArtwork(tvShow.images, artworkType, tmdbId, options);
              for (int i = 0; i <= MetadataUtil.unboxInteger(tvShow.number_of_seasons); i++) {
                try {
                  artwork.addAll(prepareArtwork(api.tvSeasonsService().images(tmdbId, i, null).execute().body(), artworkType, tmdbId, i, options));
                }
                catch (Exception e) {
                  LOGGER.debug("could not net season artwork: '{}'", e.getMessage());
                }
              }
            }
          }
          else {
            // no season artwork requested - just use the easy call
            artwork = prepareArtwork(api.tvService().images(tmdbId, null).execute().body(), artworkType, tmdbId, options);
          }

          break;

        case TV_EPISODE:
          int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
          int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

          if (seasonNr > -1 && episodeNr > -1) {
            artwork = prepareArtwork(api.tvEpisodesService().images(tmdbId, seasonNr, episodeNr).execute().body(), artworkType, tmdbId, options);
          }
          break;
      }
    }
    catch (InterruptedIOException e) {
      return Collections.emptyList();
    }
    catch (Exception e) {
      LOGGER.debug("failed to get artwork: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (ListUtils.isEmpty(artwork)) {
      return Collections.emptyList();
    }

    // buffer the artwork
    MediaMetadata md = options.getMetadata();
    if (md != null) {
      md.addMediaArt(artwork);
    }

    return artwork;
  }

  private List<MediaArtwork> prepareArtwork(Images tmdbArtwork, MediaArtwork.MediaArtworkType artworkType, int tmdbId,
      ArtworkSearchAndScrapeOptions options) {
    return prepareArtwork(tmdbArtwork, artworkType, tmdbId, -1, options);
  }

  private List<MediaArtwork> prepareArtwork(Images tmdbArtwork, MediaArtwork.MediaArtworkType artworkType, int tmdbId, int season,
      ArtworkSearchAndScrapeOptions options) {
    List<MediaArtwork> artwork = new ArrayList<>();

    if (tmdbArtwork == null) {
      return artwork;
    }

    // first sort the artwork
    if (tmdbArtwork.posters != null) {
      tmdbArtwork.posters.sort(new ImageComparator(options.getLanguage().toLocale()));
    }
    if (tmdbArtwork.backdrops != null) {
      tmdbArtwork.backdrops.sort(new ImageComparator(options.getLanguage().toLocale()));
    }
    if (tmdbArtwork.stills != null) {
      tmdbArtwork.stills.sort(new ImageComparator(options.getLanguage().toLocale()));
    }
    if (tmdbArtwork.logos != null) {
      tmdbArtwork.logos.sort(new ImageComparator(options.getLanguage().toLocale()));
    }

    // prepare posters
    if (artworkType == MediaArtwork.MediaArtworkType.POSTER || artworkType == MediaArtworkType.SEASON_POSTER
        || artworkType == MediaArtwork.MediaArtworkType.ALL) {
      for (Image image : ListUtils.nullSafe(tmdbArtwork.posters)) {
        MediaArtwork ma;
        if (season < 0) {
          ma = new MediaArtwork(TmdbMetadataProvider.ID, MediaArtworkType.POSTER);
        }
        else {
          ma = new MediaArtwork(TmdbMetadataProvider.ID, MediaArtworkType.SEASON_POSTER);
        }

        ma.setPreviewUrl(baseUrl + "w342" + image.file_path);
        ma.setOriginalUrl(baseUrl + "original" + image.file_path);
        if (StringUtils.isBlank(image.iso_639_1)) {
          // no text
          ma.setLanguage("");
        }
        else {
          ma.setLanguage(image.iso_639_1);
        }

        ma.setLikes(image.vote_count);
        ma.setTmdbId(tmdbId);

        if (season > -1) {
          ma.setSeason(season);
        }

        // add different sizes
        // original
        ma.addImageSize(image.width, image.height, baseUrl + "original" + image.file_path, MediaArtwork.PosterSizes.getSizeOrder(image.width));

        // w780
        if (780 < image.width) {
          ma.addImageSize(780, Math.round(image.height * 780f / image.width), baseUrl + "w780" + image.file_path,
              MediaArtwork.PosterSizes.getSizeOrder(780));
        }
        // w500
        if (500 < image.width) {
          ma.addImageSize(500, Math.round(image.height * 500f / image.width), baseUrl + "w500" + image.file_path,
              MediaArtwork.PosterSizes.getSizeOrder(500));
        }
        // w342
        if (342 < image.width) {
          ma.addImageSize(342, Math.round(image.height * 342f / image.width), baseUrl + "w342" + image.file_path,
              MediaArtwork.PosterSizes.getSizeOrder(342));
        }
        // w185
        if (185 < image.width) {
          ma.addImageSize(185, Math.round(image.height * 185f / image.width), baseUrl + "w185" + image.file_path,
              MediaArtwork.PosterSizes.getSizeOrder(185));
        }

        artwork.add(ma);
      }
    }

    if (artworkType == MediaArtwork.MediaArtworkType.BACKGROUND || artworkType == MediaArtwork.MediaArtworkType.ALL) {
      for (Image image : ListUtils.nullSafe(tmdbArtwork.backdrops)) {
        MediaArtwork ma = new MediaArtwork(TmdbMetadataProvider.ID, MediaArtworkType.BACKGROUND);
        ma.setPreviewUrl(baseUrl + "w1280" + image.file_path);
        ma.setOriginalUrl(baseUrl + "original" + image.file_path);
        ma.setLanguage(image.iso_639_1);
        ma.setTmdbId(tmdbId);

        if (season > -1) {
          ma.setSeason(season);
        }

        // add different sizes
        // original (most of the time 1920x1080)
        ma.addImageSize(image.width, image.height, baseUrl + "original" + image.file_path, MediaArtwork.FanartSizes.getSizeOrder(image.width));

        // 1280x720
        if (1280 < image.width) {
          ma.addImageSize(1280, Math.round(image.height * 1280f / image.width), baseUrl + "w1280" + image.file_path,
              MediaArtwork.FanartSizes.getSizeOrder(1280));
        }
        // w300
        if (300 < image.width) {
          ma.addImageSize(300, Math.round(image.height * 300f / image.width), baseUrl + "w300" + image.file_path,
              MediaArtwork.FanartSizes.getSizeOrder(300));
        }

        artwork.add(ma);
      }
    }

    // stills
    if (artworkType == MediaArtworkType.THUMB || artworkType == MediaArtwork.MediaArtworkType.ALL) {
      for (Image image : ListUtils.nullSafe(tmdbArtwork.stills)) {
        MediaArtwork ma = new MediaArtwork(TmdbMetadataProvider.ID, MediaArtworkType.THUMB);
        ma.setPreviewUrl(baseUrl + "w300" + image.file_path);
        ma.setOriginalUrl(baseUrl + "original" + image.file_path);
        ma.setLanguage(image.iso_639_1);
        ma.setTmdbId(tmdbId);

        // add different sizes
        // original
        ma.addImageSize(image.width, image.height, baseUrl + "original" + image.file_path, MediaArtwork.ThumbSizes.getSizeOrder(image.width));

        // w300
        if (300 < image.width) {
          ma.addImageSize(300, Math.round(image.height * 300f / image.width), baseUrl + "w300" + image.file_path,
              MediaArtwork.ThumbSizes.getSizeOrder(300));
        }

        artwork.add(ma);
      }
    }

    // logos
    if (artworkType == MediaArtworkType.CLEARLOGO || artworkType == MediaArtwork.MediaArtworkType.ALL) {
      for (Image image : ListUtils.nullSafe(tmdbArtwork.logos)) {
        MediaArtwork ma = new MediaArtwork(TmdbMetadataProvider.ID, MediaArtworkType.CLEARLOGO);
        ma.setPreviewUrl(baseUrl + "w500" + image.file_path);
        ma.setOriginalUrl(baseUrl + "original" + image.file_path);
        ma.setLanguage(image.iso_639_1);
        ma.setTmdbId(tmdbId);

        // add different sizes
        // original
        ma.addImageSize(image.width, image.height, baseUrl + "original" + image.file_path, 0);

        // w500
        if (500 < image.width) {
          ma.addImageSize(500, Math.round(image.height * 500f / image.width), baseUrl + "w500" + image.file_path, 0);
        }

        // w300
        if (300 < image.width) {
          ma.addImageSize(300, Math.round(image.height * 300f / image.width), baseUrl + "w300" + image.file_path, 0);
        }

        artwork.add(ma);
      }
    }

    return artwork;
  }

  /*****************************************************************************************
   * local helper classes
   *****************************************************************************************/
  private static class ImageComparator implements Comparator<Image> {
    private final String preferredLangu;

    private ImageComparator(Locale locale) {
      if (locale == null) {
        this.preferredLangu = null;
      }
      else {
        this.preferredLangu = locale.getLanguage();
      }
    }

    /*
     * sort artwork: primary by language: preferred lang (ie de), en, others; then: score
     */
    @Override
    public int compare(Image arg0, Image arg1) {
      // check if first image is preferred langu
      if (Objects.equals(preferredLangu, arg0.iso_639_1) && !Objects.equals(preferredLangu, arg1.iso_639_1)) {
        return -1;
      }

      // check if second image is preferred langu
      if (!Objects.equals(preferredLangu, arg0.iso_639_1) && Objects.equals(preferredLangu, arg1.iso_639_1)) {
        return 1;
      }

      // check if the first image is en
      if ("en".equals(arg0.iso_639_1) && !"en".equals(arg1.iso_639_1)) {
        return -1;
      }

      // check if the second image is en
      if (!"en".equals(arg0.iso_639_1) && "en".equals(arg1.iso_639_1)) {
        return 1;
      }

      // if rating is the same, return 0
      if (arg0.vote_average.equals(arg1.vote_average)) {
        return 0;
      }

      // we did not sort until here; so lets sort with the rating
      return arg0.vote_average > arg1.vote_average ? -1 : 1;
    }
  }
}
