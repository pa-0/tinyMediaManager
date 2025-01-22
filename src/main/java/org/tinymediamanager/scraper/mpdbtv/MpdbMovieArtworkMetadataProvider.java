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
package org.tinymediamanager.scraper.mpdbtv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.mpdbtv.entities.DiscArt;
import org.tinymediamanager.scraper.mpdbtv.entities.Fanart;
import org.tinymediamanager.scraper.mpdbtv.entities.HDClearArt;
import org.tinymediamanager.scraper.mpdbtv.entities.HDLogo;
import org.tinymediamanager.scraper.mpdbtv.entities.Languages;
import org.tinymediamanager.scraper.mpdbtv.entities.MovieEntity;
import org.tinymediamanager.scraper.mpdbtv.entities.Poster;
import org.tinymediamanager.scraper.mpdbtv.services.Controller;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ListUtils;

import retrofit2.Response;

/**
 * The Class {@link MpdbMovieArtworkMetadataProvider}. Movie metdata provider for the site MPDB.tv
 *
 * @author Wolfgang Janes
 */
public class MpdbMovieArtworkMetadataProvider extends MpdbMetadataProvider implements IMovieArtworkProvider {
  private static final Logger     LOGGER = LoggerFactory.getLogger(MpdbMovieArtworkMetadataProvider.class);

  private final MediaProviderInfo providerInfo;

  public MpdbMovieArtworkMetadataProvider() {
    this.providerInfo = createMediaProviderInfo();
    controller = new Controller();
  }

  @Override
  protected String getSubId() {
    return "movie_artwork";
  }

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {

    initAPI();

    MovieEntity scrapeResult = null;
    List<MediaArtwork> ma = new ArrayList<>();

    if (StringUtils.isAnyBlank(getAboKey(), getUserName())) {
      LOGGER.warn("no username/ABO Key found");
      throw new ScrapeException(new HttpException(401, "Unauthorized"));
    }

    // we need to force FR as language (no other language available here)
    options.setLanguage(MediaLanguages.fr);

    // search with mpdbtv id
    int id = options.getIdAsIntOrDefault(providerInfo.getId(), 0);

    if (id == 0) {
      LOGGER.debug("Cannot get artwork - no mpdb id set");
      throw new MissingIdException(getId());
    }

    LOGGER.info("========= BEGIN MPDB.tv artwork scraping");
    try {
      Response<MovieEntity> response = controller.getScrapeInformation(getEncodedUserName(), getSubscriptionKey(), id,
          options.getLanguage().toLocale(), null, FORMAT);
      if (!response.isSuccessful()) {
        String message = "";
        try {
          message = response.errorBody().string();
        }
        catch (IOException e) {
          // ignore
        }
        LOGGER.warn("request was not successful: HTTP/{} - {}", response.code(), message);
        throw new HttpException(response.code(), response.message());
      }
      if (response.isSuccessful()) {
        scrapeResult = response.body();
      }
    }
    catch (HttpException e) {
      LOGGER.debug("nothing found");
      if (e.getStatusCode() == 404) {
        return Collections.emptyList();
      }
      throw new ScrapeException(e);
    }
    catch (Exception e) {
      LOGGER.error("error searching: {} ", e.getMessage());
      throw new ScrapeException(e);
    }

    if (scrapeResult == null) {
      LOGGER.warn("no result from MPDB.tv");
      return Collections.emptyList();
    }

    // Poster
    for (Poster poster : ListUtils.nullSafe(scrapeResult.posters)) {
      MediaArtwork mediaArtwork = new MediaArtwork(providerInfo.getId(), MediaArtwork.MediaArtworkType.POSTER);
      mediaArtwork.setPreviewUrl(poster.preview);
      mediaArtwork.setOriginalUrl(poster.original);
      mediaArtwork.setLikes(poster.votes);
      mediaArtwork.addImageSize(poster.width, poster.height, poster.original, MediaArtwork.PosterSizes.getSizeOrder(poster.width));

      if (!poster.languages.isEmpty()) {
        Languages language = poster.languages.get(0);
        mediaArtwork.setLanguage(LanguageUtils.getIso2LanguageFromLocalizedString(language.language));
      }

      ma.add(mediaArtwork);
    }

    // Fanarts
    for (Fanart fanart : ListUtils.nullSafe(scrapeResult.fanarts)) {
      MediaArtwork mediaArtwork = new MediaArtwork(providerInfo.getId(), MediaArtwork.MediaArtworkType.BACKGROUND);
      mediaArtwork.setPreviewUrl(fanart.preview);
      mediaArtwork.setOriginalUrl(fanart.original);
      mediaArtwork.setLikes(fanart.votes);
      mediaArtwork.addImageSize(fanart.width, fanart.height, fanart.original, MediaArtwork.FanartSizes.getSizeOrder(fanart.width));

      if (!fanart.languages.isEmpty()) {
        Languages language = fanart.languages.get(0);
        mediaArtwork.setLanguage(LanguageUtils.getIso2LanguageFromLocalizedString(language.language));
      }

      ma.add(mediaArtwork);
    }

    // DiscArt
    for (DiscArt discArt : ListUtils.nullSafe(scrapeResult.discarts)) {
      MediaArtwork mediaArtwork = new MediaArtwork(providerInfo.getId(), MediaArtwork.MediaArtworkType.DISC);
      mediaArtwork.setPreviewUrl(discArt.preview);
      mediaArtwork.setOriginalUrl(discArt.original);
      mediaArtwork.setLikes(discArt.votes);
      mediaArtwork.addImageSize(discArt.width, discArt.height, discArt.original, 0);

      if (!discArt.languages.isEmpty()) {
        Languages language = discArt.languages.get(0);
        mediaArtwork.setLanguage(LanguageUtils.getIso2LanguageFromLocalizedString(language.language));
      }

      ma.add(mediaArtwork);
    }

    // HDClearArt
    for (HDClearArt hdClearArt : ListUtils.nullSafe(scrapeResult.hdcleararts)) {
      MediaArtwork mediaArtwork = new MediaArtwork(providerInfo.getId(), MediaArtwork.MediaArtworkType.CLEARART);
      mediaArtwork.setPreviewUrl(hdClearArt.preview);
      mediaArtwork.setOriginalUrl(hdClearArt.original);
      mediaArtwork.setLikes(hdClearArt.votes);
      mediaArtwork.addImageSize(hdClearArt.width, hdClearArt.height, hdClearArt.original, 0);

      if (!hdClearArt.languages.isEmpty()) {
        Languages language = hdClearArt.languages.get(0);
        mediaArtwork.setLanguage(LanguageUtils.getIso2LanguageFromLocalizedString(language.language));
      }

      ma.add(mediaArtwork);
    }

    // HDLogo
    for (HDLogo hdLogo : ListUtils.nullSafe(scrapeResult.hdlogos)) {
      MediaArtwork mediaArtwork = new MediaArtwork(providerInfo.getId(), MediaArtwork.MediaArtworkType.CLEARLOGO);
      mediaArtwork.setPreviewUrl(hdLogo.preview);
      mediaArtwork.setOriginalUrl(hdLogo.original);
      mediaArtwork.setLikes(hdLogo.votes);
      mediaArtwork.addImageSize(hdLogo.width, hdLogo.height, hdLogo.original, 0);

      if (!hdLogo.languages.isEmpty()) {
        Languages language = hdLogo.languages.get(0);
        mediaArtwork.setLanguage(LanguageUtils.getIso2LanguageFromLocalizedString(language.language));
      }

      ma.add(mediaArtwork);
    }
    return ma;
  }
}
