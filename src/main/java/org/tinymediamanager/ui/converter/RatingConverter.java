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
package org.tinymediamanager.ui.converter;

import java.util.Locale;

import org.jdesktop.beansbinding.Converter;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaRating;

/**
 * the class {@link RatingConverter} is used to display the rating with the max value
 *
 * @author Manuel Laggner
 */
public class RatingConverter<T extends MediaRating> extends Converter<T, String> {

  private final Locale locale = Locale.getDefault();

  @Override
  public String convertForward(T rating) {
    if (rating != null) {
      // we want to display the rating in the following form
      // 7.4 / 10 (3457 Votes / imdb)
      // but we do not have the vote count and/or rating provider in all cases
      // just do not display
      // 0-1 votes
      // default/user rating

      boolean defaultUserRating;

      switch (rating.getId()) {
        case MediaRating.DEFAULT:
        case MediaRating.USER:
        case MediaRating.NFO:
          defaultUserRating = true;
          break;

        default:
          defaultUserRating = false;
          break;

      }

      if (rating.getVotes() > 1 && !defaultUserRating) {
        return String.format(locale, "%.1f / %,d (%,d %s / %s)", rating.getRating(), rating.getMaxValue(), rating.getVotes(),
            TmmResourceBundle.getString("metatag.votes"), rating.getId());
      }
      else if (rating.getVotes() > 1 && defaultUserRating) {
        return String.format(locale, "%.1f / %,d (%,d %s)", rating.getRating(), rating.getMaxValue(), rating.getVotes(),
            TmmResourceBundle.getString("metatag.votes"));
      }
      else if (!defaultUserRating && rating.getVotes() >= 1) {
        return String.format(locale, "%.1f / %,d (%s)", rating.getRating(), rating.getMaxValue(), rating.getId());
      }
      else {
        return String.format(locale, "%.1f / %,d", rating.getRating(), rating.getMaxValue());
      }
    }
    return "";
  }

  @Override
  public T convertReverse(String arg0) {
    return null;
  }
}
