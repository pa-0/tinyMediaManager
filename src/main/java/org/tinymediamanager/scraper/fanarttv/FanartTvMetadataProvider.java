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
package org.tinymediamanager.scraper.fanarttv;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.FanartSizes;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaArtwork.PosterSizes;
import org.tinymediamanager.scraper.entities.MediaArtwork.ThumbSizes;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.fanarttv.entities.Image;
import org.tinymediamanager.scraper.fanarttv.entities.Images;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.util.ListUtils;

/**
 * The Class FanartTvMetadataProvider. An artwork provider for the site fanart.tv
 *
 * @author Manuel Laggner
 */
abstract class FanartTvMetadataProvider implements IMediaProvider {
  static final String             ID  = "fanarttv";

  private final MediaProviderInfo providerInfo;

  protected FanartTv              api = null;

  FanartTvMetadataProvider() {
    providerInfo = createMediaProviderInfo();
  }

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  private MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "fanart.tv",
        "<html><h3>Fanart.tv</h3><br />Fanart.tv provides a huge library of artwork for movies, TV shows and music. This service can be consumed with the API key tinyMediaManager offers, but if you want to have faster access to the artwork, you should become a VIP at fanart.tv (https://fanart.tv/vip/).</html>",
        FanartTvMetadataProvider.class.getResource("/org/tinymediamanager/scraper/fanart_tv.png"));

    // configure/load settings
    info.getConfig().addText("clientKey", "", true);
    info.getConfig().load();

    return info;
  }

  // thread safe initialization of the API
  protected synchronized void initAPI() throws ScrapeException {
    if (api == null) {
      if (!isActive()) {
        throw new ScrapeException(new FeatureNotEnabledException(this));
      }

      try {
        api = new FanartTv();
      }
      catch (Exception e) {
        getLogger().error("FanartTvMetadataProvider", e);
        throw new ScrapeException(e);
      }
    }

    // set API key check
    try {
      api.setApiKey(getApiKey());
    }
    catch (Exception e) {
      throw new ScrapeException(e);
    }

    // check if we should set a client key
    String clientKey = providerInfo.getConfig().getValue("clientKey");
    if (!clientKey.equals(api.getClientKey())) {
      api.setClientKey(clientKey);
    }
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(null);
  }

  abstract Logger getLogger();

  protected List<MediaArtwork> getArtwork(Images images, MediaArtworkType artworkType) {
    List<MediaArtwork> artworks = new ArrayList<>();

    switch (artworkType) {
      case POSTER:
        artworks.addAll(prepareArtwork(images.movieposter, ImageType.MOVIEPOSTER));
        artworks.addAll(prepareArtwork(images.tvposter, ImageType.TVPOSTER));
        break;

      case BACKGROUND:
        artworks.addAll(prepareArtwork(images.moviebackground, ImageType.MOVIEBACKGROUND));
        artworks.addAll(prepareArtwork(images.showbackground, ImageType.SHOWBACKGROUND));
        break;

      case BANNER:
        artworks.addAll(prepareArtwork(images.moviebanner, ImageType.MOVIEBANNER));
        artworks.addAll(prepareArtwork(images.tvbanner, ImageType.TVBANNER));
        break;

      case CLEARART:
        artworks.addAll(prepareArtwork(images.hdmovieclearart, ImageType.HDMOVIECLEARART));
        artworks.addAll(prepareArtwork(images.hdclearart, ImageType.HDCLEARART));
        break;

      case DISC:
        artworks.addAll(prepareArtwork(images.moviedisc, ImageType.MOVIEDISC));
        break;

      case LOGO:
      case CLEARLOGO:
        artworks.addAll(prepareArtwork(images.hdmovielogo, ImageType.HDMOVIELOGO));
        artworks.addAll(prepareArtwork(images.hdtvlogo, ImageType.HDTVLOGO));
        break;

      case SEASON_POSTER:
        artworks.addAll(prepareArtwork(images.seasonposter, ImageType.SEASONPOSTER));
        break;

      case SEASON_BANNER:
        artworks.addAll(prepareArtwork(images.seasonbanner, ImageType.SEASONBANNER));
        break;

      case SEASON_THUMB:
        artworks.addAll(prepareArtwork(images.seasonthumb, ImageType.SEASONTHUMB));
        break;

      case THUMB:
        artworks.addAll(prepareArtwork(images.moviethumb, ImageType.MOVIETHUMB));
        artworks.addAll(prepareArtwork(images.tvthumb, ImageType.TVTHUMB));
        break;

      case CHARACTERART:
        artworks.addAll(prepareArtwork(images.characterart, ImageType.CHARACTERART));
        break;

      case KEYART:
        artworks.addAll(prepareArtwork(images.movieposter, ImageType.MOVIEKEYART));
        artworks.addAll(prepareArtwork(images.tvposter, ImageType.TVKEYART));
        break;

      case ALL:
        artworks.addAll(prepareArtwork(images.movieposter, ImageType.MOVIEPOSTER));
        artworks.addAll(prepareArtwork(images.tvposter, ImageType.TVPOSTER));

        artworks.addAll(prepareArtwork(images.moviebackground, ImageType.MOVIEBACKGROUND));
        artworks.addAll(prepareArtwork(images.showbackground, ImageType.SHOWBACKGROUND));

        artworks.addAll(prepareArtwork(images.moviebanner, ImageType.MOVIEBANNER));
        artworks.addAll(prepareArtwork(images.tvbanner, ImageType.TVBANNER));

        artworks.addAll(prepareArtwork(images.hdmovieclearart, ImageType.HDMOVIECLEARART));
        artworks.addAll(prepareArtwork(images.hdclearart, ImageType.HDCLEARART));

        artworks.addAll(prepareArtwork(images.moviedisc, ImageType.MOVIEDISC));

        artworks.addAll(prepareArtwork(images.hdmovielogo, ImageType.HDMOVIELOGO));
        artworks.addAll(prepareArtwork(images.hdtvlogo, ImageType.HDTVLOGO));

        artworks.addAll(prepareArtwork(images.seasonbanner, ImageType.SEASONBANNER));
        artworks.addAll(prepareArtwork(images.seasonposter, ImageType.SEASONPOSTER));
        artworks.addAll(prepareArtwork(images.seasonthumb, ImageType.SEASONTHUMB));

        artworks.addAll(prepareArtwork(images.moviethumb, ImageType.MOVIETHUMB));
        artworks.addAll(prepareArtwork(images.tvthumb, ImageType.TVTHUMB));

        artworks.addAll(prepareArtwork(images.characterart, ImageType.CHARACTERART));

        artworks.addAll(prepareArtwork(images.movieposter, ImageType.MOVIEKEYART));
        artworks.addAll(prepareArtwork(images.tvposter, ImageType.TVKEYART));
        break;

      default:
        break;
    }

    return artworks;
  }

  protected List<MediaArtwork> prepareArtwork(List<Image> images, ImageType type) {
    List<MediaArtwork> artworks = new ArrayList<>();

    for (Image image : ListUtils.nullSafe(images)) {
      // -keyart is actually a poster with the language '00'
      if ((type == ImageType.MOVIEKEYART || type == ImageType.TVKEYART) && !"00".equals(image.lang)) {
        continue;
      }

      MediaArtwork ma = new MediaArtwork(providerInfo.getId(), type.type);
      ma.setOriginalUrl(image.url);

      // replace the url to get the preview AND switch to assetcache.fanart.tv (as suggested in discord)
      // ma.setPreviewUrl(image.url.replace("/fanart/", "/preview/").replace("assets.fanart.tv", "assetcache.fanart.tv"));
      // not anymore - keep url, and just exchange the fanart to /preview/ (Discord Feb 2022)
      ma.setPreviewUrl(image.url.replace("/fanart/", "/preview/"));

      if ("00".equals(image.lang) || StringUtils.isBlank(image.lang)) {
        // no text
        ma.setLanguage("");
      }
      else {
        ma.setLanguage(image.lang);
      }
      ma.setLikes(image.likes);
      ma.addImageSize(type.width, type.height, image.url, type.sizeOrder);

      if ("all".equals(image.season)) {
        ma.setSeason(0);
      }
      else if (StringUtils.isNotBlank(image.season)) {
        try {
          ma.setSeason(Integer.parseInt(image.season));
        }
        catch (Exception e) {
          getLogger().trace("could not parse int: {}", e.getMessage());
        }
      }
      artworks.add(ma);
    }

    return artworks;
  }

  private enum ImageType {

    CHARACTERART(512, 512, MediaArtworkType.CHARACTERART, FanartSizes.MEDIUM.getOrder()),
    HDCLEARART(1000, 562, MediaArtworkType.CLEARART, FanartSizes.MEDIUM.getOrder()),
    HDMOVIECLEARART(1000, 562, MediaArtworkType.CLEARART, FanartSizes.MEDIUM.getOrder()),
    HDMOVIELOGO(800, 310, MediaArtworkType.CLEARLOGO, FanartSizes.MEDIUM.getOrder()),
    HDTVLOGO(800, 310, MediaArtworkType.CLEARLOGO, FanartSizes.MEDIUM.getOrder()),
    MOVIEBACKGROUND(1920, 1080, MediaArtworkType.BACKGROUND, FanartSizes.LARGE.getOrder()),
    MOVIEBANNER(1000, 185, MediaArtworkType.BANNER, FanartSizes.MEDIUM.getOrder()),
    MOVIEDISC(1000, 1000, MediaArtworkType.DISC, FanartSizes.MEDIUM.getOrder()),
    MOVIEKEYART(1000, 1426, MediaArtworkType.KEYART, PosterSizes.LARGE.getOrder()),
    MOVIEPOSTER(1000, 1426, MediaArtworkType.POSTER, PosterSizes.LARGE.getOrder()),
    MOVIETHUMB(1000, 562, MediaArtworkType.THUMB, ThumbSizes.MEDIUM.getOrder()),
    SEASONBANNER(1000, 185, MediaArtworkType.SEASON_BANNER, FanartSizes.MEDIUM.getOrder()),
    SEASONPOSTER(1000, 1426, MediaArtworkType.SEASON_POSTER, MediaArtwork.PosterSizes.LARGE.getOrder()),
    SEASONTHUMB(1000, 562, MediaArtworkType.SEASON_THUMB, ThumbSizes.MEDIUM.getOrder()),
    SHOWBACKGROUND(1920, 1080, MediaArtworkType.BACKGROUND, FanartSizes.LARGE.getOrder()),
    TVBANNER(1000, 185, MediaArtworkType.BANNER, FanartSizes.MEDIUM.getOrder()),
    TVKEYART(1000, 1426, MediaArtworkType.KEYART, PosterSizes.LARGE.getOrder()),
    TVPOSTER(1000, 1426, MediaArtworkType.POSTER, PosterSizes.LARGE.getOrder()),
    TVTHUMB(500, 281, MediaArtworkType.THUMB, ThumbSizes.MEDIUM.getOrder());

    ImageType(int width, int height, MediaArtworkType type, int sizeOrder) {
      this.width = width;
      this.height = height;
      this.type = type;
      this.sizeOrder = sizeOrder;
    }

    int              width;
    int              height;
    MediaArtworkType type;
    int              sizeOrder;
  }
}
