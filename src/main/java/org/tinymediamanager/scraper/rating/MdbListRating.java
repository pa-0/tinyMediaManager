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
package org.tinymediamanager.scraper.rating;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.rating.entities.MdbListRatingEntity;
import org.tinymediamanager.scraper.rating.entities.MdbListRatings;
import org.tinymediamanager.scraper.rating.services.MdbListController;
import org.tinymediamanager.scraper.util.ListUtils;

import retrofit2.Response;

/**
 * the class {@link MdbListRating} is used to get metadata from MdbList.com
 * 
 * @author Wolfgang Janes
 */
class MdbListRating {

  public static final Logger      LOGGER = LoggerFactory.getLogger(MdbListRating.class);
  private final MdbListController controller;

  MdbListRating() {
    controller = new MdbListController();
  }

  public List<MediaRating> getRatings(MediaType mediaType, Map<String, Object> ids) {
    MdbListRatingEntity ratingEntity;
    List<MediaRating> mediaRatingList = new ArrayList<>();

    // Get API Key from the settings
    String apiKey = Settings.getInstance().getMdbListApiKey();
    if (apiKey.isBlank()) {
      // If no API Key is entered return empty rating list
      LOGGER.debug("No API KEY entered for MDBList.com - skipping");
      return mediaRatingList;
    }

    // MDBList does only has show ratings
    if (mediaType == MediaType.TV_EPISODE) {
      return mediaRatingList;
    }

    for (Map.Entry<String, Object> entry : ids.entrySet()) {
      // Fetch the ratings with the first found ID
      try {
        Response<MdbListRatingEntity> response;
        switch (entry.getKey()) {
          case MediaMetadata.IMDB:
            response = controller.getRatingsByImdbId(apiKey, entry.getValue().toString(), mediaType);
            break;

          case MediaMetadata.TRAKT_TV:
            response = controller.getRatingsByTraktId(apiKey, entry.getValue().toString(), mediaType);
            break;

          case MediaMetadata.TMDB:
            response = controller.getRatingsByTmdbId(apiKey, entry.getValue().toString(), mediaType);
            break;

          case MediaMetadata.TVDB:
            response = controller.getRatingsByTvdbId(apiKey, entry.getValue().toString(), mediaType);
            break;

          default:
            continue;
        }

        if (response == null) {
          throw new NothingFoundException();
        }
        if (!response.isSuccessful()) {
          throw new HttpException(response.code(), response.message());
        }

        ratingEntity = response.body();
        // Loop over result to get all Ratings and add them to list of media ratings
        for (MdbListRatings ratings : ListUtils.nullSafe(ratingEntity.ratings)) {
          if (ratings.source == null || ratings.value == null || (ratings.value == 0 && !ratings.source.equals(MediaMetadata.ROGER_EBERT))) {
            continue;
          }
          MediaRating mediaRating = new MediaRating(ratings.source, ratings.value, ratings.votes);
          switch (ratings.source) {
            case MediaMetadata.LETTERBOXD: {
              mediaRating.setMaxValue(5);
              break;
            }

            case MediaMetadata.ROGER_EBERT: {
              mediaRating.setMaxValue(4); // yes, 4
              break;
            }
          }
          mediaRatingList.add(mediaRating);
        }

        break;
      }
      catch (Exception e) {
        LOGGER.debug("Could not fetch ratings - '{}'", e.getMessage());
        break;
      }
    }
    return mediaRatingList;
  }
}
