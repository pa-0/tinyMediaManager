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
package org.tinymediamanager.scraper.thetvdb;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.GUEST;
import static org.tinymediamanager.core.entities.Person.Type.PRODUCER;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BACKGROUND;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_THUMB;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.thetvdb.entities.ArtworkBaseRecord;
import org.tinymediamanager.scraper.thetvdb.entities.ArtworkTypeRecord;
import org.tinymediamanager.scraper.thetvdb.entities.ArtworkTypeResponse;
import org.tinymediamanager.scraper.thetvdb.entities.Character;
import org.tinymediamanager.scraper.thetvdb.entities.RemoteID;
import org.tinymediamanager.scraper.thetvdb.entities.SearchResultResponse;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;

import retrofit2.Response;

/**
 * The Class TheTvDbMetadataProvider.
 *
 * @author Manuel Laggner
 */
abstract class TheTvDbMetadataProvider implements IMediaProvider {
  private static final Logger                   LOGGER            = LoggerFactory.getLogger(TheTvDbMetadataProvider.class);
  private static final String                   ID                = "tvdb";

  protected static final String                 FALLBACK_LANGUAGE = "fallbackLanguage";
  protected static final Pattern                ID_PATTERN        = Pattern.compile("\\d{3,}");

  private final MediaProviderInfo               providerInfo;
  private final Map<Integer, ArtworkTypeRecord> artworkTypes      = new HashMap<>();

  protected TheTvDbController                   tvdb;

  TheTvDbMetadataProvider() {
    providerInfo = createMediaProviderInfo();
  }

  protected abstract Logger getLogger();

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  protected MediaProviderInfo createMediaProviderInfo() {
    return new MediaProviderInfo(ID, getSubId(), "thetvdb.com",
        "<html><h3>The TVDB</h3><br />An open database for television fans. This scraper is able to scrape TV series metadata and artwork</html>",
        TheTvDbMetadataProvider.class.getResource("/org/tinymediamanager/scraper/thetvdb_com.svg"), 30);
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(providerInfo.getUserApiKey());
  }

  String getAuthToken() throws Exception {
    String userApiKey = providerInfo.getUserApiKey();
    String userPin = providerInfo.getConfig().getValue("pin");

    if (StringUtils.isNotBlank(userApiKey)) {
      // always remember to avoid a force-reinit at every call
      tvdb.setUserApiKey(userApiKey);
      tvdb.setUserPin(userPin);

      try {
        return TheTvDbController.login(userApiKey, userPin);
      }
      catch (Exception e) {
        LOGGER.debug("Could not get auth token for user key/pin - '{}", e.getMessage());
      }
    }

    return getApiKey();
  }

  protected synchronized void initAPI() throws ScrapeException {

    // check if the API should change from current key to another
    if (tvdb != null) {
      String userApiKey = providerInfo.getUserApiKey();
      String userPin = providerInfo.getConfig().getValue("pin");
      if (StringUtils.isNotBlank(userApiKey) && (!userApiKey.equals(tvdb.getUserApiKey()) || !userPin.equals(tvdb.getUserPin()))) {
        // force re-initialization with new key
        tvdb = null;
      }
      else if (StringUtils.isBlank(userApiKey) && StringUtils.isNotBlank(tvdb.getUserApiKey())) {
        // force re-initialization with new key
        tvdb = null;
      }
    }

    if (tvdb == null) {
      if (!isActive()) {
        throw new ScrapeException(new FeatureNotEnabledException(this));
      }

      try {
        tvdb = new TheTvDbController();
        tvdb.setAuthToken(getAuthToken());

        artworkTypes.clear();
        Response<ArtworkTypeResponse> response = tvdb.getConfigService().getArtworkTypes().execute();
        if (response.isSuccessful()) {
          for (ArtworkTypeRecord artworkTypeRecord : response.body().data) {
            if (artworkTypeRecord.width > 0 && artworkTypeRecord.height > 0) {
              artworkTypes.put(artworkTypeRecord.id, artworkTypeRecord);
            }
          }
        }
        else {
          String msg = response.message().isBlank() ? response.errorBody().string() : response.message();
          throw new HttpException(response.code(), msg);
        }
      }
      catch (Exception e) {
        getLogger().warn("could not initialize API: {}", e.getMessage());
        // force re-initialization the next time this will be called
        tvdb = null;
        throw new ScrapeException(e);
      }
    }
  }

  /**
   * try to strip out the year from the title
   * 
   * @param title
   *          the title to strip out the year
   * @param year
   *          the year to compare
   * @return the cleaned title or the original title if there is nothing to clean
   */
  protected String clearYearFromTitle(String title, int year) {
    return title.replaceAll("\\(" + year + "\\)$", "").strip();
  }

  /**
   * get the {@link ArtworkTypeRecord} for the given id
   * 
   * @param id
   *          the id to get the artwork type for
   * @return the {@link ArtworkTypeRecord} or null
   */
  protected ArtworkTypeRecord getArtworkType(Integer id) {
    if (id == null) {
      return null;
    }

    return artworkTypes.get(id);
  }

  /**
   * parse the localized text from the given texts
   * 
   * @param desiredLanguage
   *          the desired {@link MediaLanguages} to get the text in
   * @param localizedTexts
   *          a {@link Map} of translated texts
   * @return the translated text or an empty {@link String}
   */
  protected String parseLocalizedText(MediaLanguages desiredLanguage, Map<String, String> localizedTexts) {
    if (localizedTexts == null) {
      return "";
    }

    String iso3LanguageTag = LanguageUtils.getIso3Language(desiredLanguage.toLocale());

    // pt-BR is pt at tvdb...
    if ("pob".equals(iso3LanguageTag)) {
      iso3LanguageTag = "pt";
    }

    String text = localizedTexts.get(iso3LanguageTag);
    if (StringUtils.isNotBlank(text)) {
      return text;
    }

    return "";
  }

  protected List<Person> parseCastMembers(List<Character> characters) {
    if (characters == null) {
      return Collections.emptyList();
    }

    // sort by the field sort
    characters.sort(Comparator.comparingInt(o -> o.sort));

    List<Person> members = new ArrayList<>();

    for (Character character : characters) {
      Person member;

      switch (character.type) {

        case 1:
          member = new Person(DIRECTOR);
          member.setRole(character.peopleType);
          break;

        case 2:
          member = new Person(WRITER);
          member.setRole(character.peopleType);
          break;

        case 3:
          member = new Person(ACTOR);
          member.setRole(character.name);
          break;

        case 4:
          member = new Person(GUEST);
          member.setRole(character.name);
          break;

        case 7:
          member = new Person(PRODUCER);
          member.setRole(character.peopleType);
          break;

        default:
          continue;
      }

      member.setId(getId(), character.peopleId);
      member.setName(character.personName);

      if (StringUtils.isNotBlank(character.image)) {
        member.setThumbUrl(character.image);
      }
      if (StringUtils.isNotBlank(character.url)) {
        // add https://thetvdb.com/people/ in the front in case this is missing
        String profileUrl = character.url;
        if (!profileUrl.startsWith("http")) {
          profileUrl = "https://thetvdb.com/people/" + profileUrl;
        }
        member.setProfileUrl(profileUrl);
      }

      members.add(member);
    }

    return members;
  }

  /**
   * Parses used remote IDs into our format<br>
   * Feel free to extend this!
   * 
   * @param ids
   * @return
   */
  protected Map<String, Object> parseRemoteIDs(List<RemoteID> ids) {
    Map<String, Object> ret = new HashMap<>();

    for (RemoteID remote : ListUtils.nullSafe(ids)) {
      switch (remote.type) {
        case 1: // ???
          break;

        case 2: // IMDB
          if (MediaIdUtil.isValidImdbId(remote.id)) {
            ret.put(MediaMetadata.IMDB, remote.id);
          }
          break;

        case 3: // TMS (Zap2It)
          ret.put(MediaMetadata.ZAP2IT, remote.id);
          break;

        case 4: // Official Website
        case 5: // Facebook
        case 6: // X (Twitter)
        case 7: // Reddit
        case 8: // Fan Site
        case 9: // Instagram
        case 10: // ???
        case 11: // Youtube
          break;

        case 12: // TheMovieDB.com
          int tid = MetadataUtil.parseInt(remote.id, -1);
          if (tid > 0) {
            ret.put(MediaMetadata.TMDB, tid); // save as INT
          }
          break;

        case 13: // EIDR
          ret.put("eidr", remote.id);
          break;

        case 14: // ???
        case 15: // ???
        case 16: // ???
        case 17: // ???
          break;

        case 18: // Wikidata
          ret.put(MediaMetadata.WIKIDATA, remote.id);
          break;

        case 19: // TV Maze
          int mid = MetadataUtil.parseInt(remote.id, -1);
          if (mid > 0) {
            ret.put(MediaMetadata.TVMAZE, mid); // save as INT
          }
          break;

        case 20: // ???
        case 21: // ???
        case 22: // ???
        case 23: // ???
        case 24: // Wikipedia
        default:
          break;
      }
    }

    return ret;
  }

  /**
   * parse the {@link ArtworkBaseRecord} and morph it to {@link MediaArtwork}
   * 
   * @param image
   *          the {@link ArtworkBaseRecord} from TVDB
   * @return the parsed {@link MediaArtwork}
   */
  protected MediaArtwork parseArtwork(ArtworkBaseRecord image) {
    if (image.id == null) {
      return null;
    }

    MediaArtwork ma = null;

    // set artwork type
    switch (image.type) {
      case 1:
      case 16:
        ma = new MediaArtwork(getProviderInfo().getId(), BANNER);
        break;

      case 2:
      case 14:
        ma = new MediaArtwork(getProviderInfo().getId(), POSTER);
        break;

      case 3:
      case 15:
        ma = new MediaArtwork(getProviderInfo().getId(), BACKGROUND);
        break;

      case 6:
        ma = new MediaArtwork(getProviderInfo().getId(), SEASON_BANNER);
        break;

      case 7:
        ma = new MediaArtwork(getProviderInfo().getId(), SEASON_POSTER);
        break;

      case 8:
        ma = new MediaArtwork(getProviderInfo().getId(), SEASON_THUMB);
        break;

      case 11:
      case 12:
        ma = new MediaArtwork(getProviderInfo().getId(), THUMB);
        break;

      default:
        return null;
    }

    // extract image sizes
    ArtworkTypeRecord artworkType = getArtworkType(image.type);
    if (artworkType != null) {
      int width = artworkType.width;
      int height = artworkType.height;
      int sizeOrder = getSizeOrder(ma.getType(), width);

      ma.addImageSize(width, height, image.image, sizeOrder);
    }

    ma.setOriginalUrl(image.image);
    if (StringUtils.isNotBlank(image.thumbnail)) {
      ma.setPreviewUrl(image.thumbnail);
    }
    else {
      ma.setPreviewUrl(ma.getOriginalUrl());
    }

    if (StringUtils.isBlank(image.language)) {
      // no text
      ma.setLanguage("");
    }
    else {
      ma.setLanguage(LanguageUtils.getIso2LanguageFromLocalizedString(image.language));
    }

    // TVDB is offering something weird here - when the score is > 100000, we substract 100000
    if (image.score >= 100000) {
      ma.setLikes(image.score - 100000);
    }
    else {
      ma.setLikes(image.score);
    }

    // get the season number
    if ((ma.getType() == SEASON_BANNER || ma.getType() == SEASON_POSTER || ma.getType() == SEASON_THUMB) && image.season != null) {
      ma.setSeason(image.season);
    }

    return ma;
  }

  protected MediaSearchResult morphMediaMetadataToSearchResult(MediaMetadata md, MediaType type) {
    MediaSearchResult searchResult = new MediaSearchResult(getId(), type);
    searchResult.setTitle(md.getTitle());
    searchResult.setYear(md.getYear());
    searchResult.setIds(md.getIds());
    searchResult.setMetadata(md);
    for (MediaArtwork artwork : md.getMediaArt()) {
      if (artwork.getType() == MediaArtwork.MediaArtworkType.POSTER) {
        searchResult.setPosterUrl(artwork.getPreviewUrl());
        break;
      }
    }
    searchResult.setScore(1);

    return searchResult;
  }

  protected int getTvdbIdViaImdbId(String imdbId) {
    // try to get it via service call
    try {
      Response<SearchResultResponse> httpResponse = tvdb.getSearchService().getSearch(imdbId, imdbId).execute();
      if (httpResponse.isSuccessful() && httpResponse.body() != null) {
        return Integer.parseInt(httpResponse.body().data.get(0).tvdbId);
      }
    }
    catch (Exception e) {
      getLogger().debug("could not fetch TVDB via IMDB - '{}'", e.getMessage());
    }

    return 0;
  }

  /**
   * get the size order of the given artwork
   *
   * @param type
   *          the {@link MediaArtwork.MediaArtworkType}
   * @param width
   *          the width
   * @return the size order
   */
  protected int getSizeOrder(MediaArtwork.MediaArtworkType type, int width) {
    int sizeOrder = 0;

    // set image size
    switch (type) {
      case POSTER:
        if (width >= 1000) {
          sizeOrder = MediaArtwork.PosterSizes.LARGE.getOrder();
        }
        else if (width >= 500) {
          sizeOrder = MediaArtwork.PosterSizes.BIG.getOrder();
        }
        else if (width >= 342) {
          sizeOrder = MediaArtwork.PosterSizes.MEDIUM.getOrder();
        }
        else {
          sizeOrder = MediaArtwork.PosterSizes.SMALL.getOrder();
        }
        break;

      case BACKGROUND:
        if (width >= 3840) {
          sizeOrder = MediaArtwork.FanartSizes.XLARGE.getOrder();
        }
        else if (width >= 1920) {
          sizeOrder = MediaArtwork.FanartSizes.LARGE.getOrder();
        }
        else if (width >= 1280) {
          sizeOrder = MediaArtwork.FanartSizes.MEDIUM.getOrder();
        }
        else {
          sizeOrder = MediaArtwork.FanartSizes.SMALL.getOrder();
        }
        break;

      case THUMB:
        if (width >= 3840) {
          sizeOrder = MediaArtwork.ThumbSizes.XLARGE.getOrder();
        }
        else if (width >= 1920) {
          sizeOrder = MediaArtwork.ThumbSizes.LARGE.getOrder();
        }
        else if (width >= 1280) {
          sizeOrder = MediaArtwork.ThumbSizes.BIG.getOrder();
        }
        else if (width >= 960) {
          sizeOrder = MediaArtwork.ThumbSizes.MEDIUM.getOrder();
        }
        else {
          sizeOrder = MediaArtwork.ThumbSizes.SMALL.getOrder();
        }
        break;

      default:
        break;
    }

    // set size for banner & season poster (resolution not in api)
    if (type == SEASON_BANNER || type == SEASON_POSTER) {
      sizeOrder = MediaArtwork.FanartSizes.LARGE.getOrder();
    }
    else if (type == BANNER) {
      sizeOrder = MediaArtwork.FanartSizes.MEDIUM.getOrder();
    }

    return sizeOrder;
  }
}
