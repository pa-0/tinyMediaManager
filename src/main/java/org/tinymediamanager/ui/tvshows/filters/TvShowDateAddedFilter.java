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
package org.tinymediamanager.ui.tvshows.filters;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.datepicker.DatePicker;

/**
 * the class {@link TvShowDateAddedFilter} is used to filter TV shows/episodes for their date added
 *
 * @author Manuel Laggner
 */
public class TvShowDateAddedFilter extends AbstractTvShowUIFilter {
  private final Calendar calendar;
  private DatePicker     datePicker;

  public TvShowDateAddedFilter() {
    super();
    calendar = Calendar.getInstance();
    calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.dateadded"));
  }

  @Override
  protected JComponent createFilterComponent() {
    datePicker = new DatePicker();
    datePicker.addPropertyChangeListener("date", e -> filterChanged());
    return datePicker;
  }

  @Override
  public String getId() {
    return "tvShowDateAdded";
  }

  @Override
  public String getFilterValueAsString() {
    Date date = datePicker.getDate();
    if (date != null) {
      return String.valueOf(date.getTime());
    }

    return null;
  }

  @Override
  public void setFilterValue(Object value) {
    if (value != null && StringUtils.isNotBlank(value.toString())) {
      try {
        Date date = new Date(Long.parseLong(value.toString()));
        datePicker.setDate(date);
      }
      catch (Exception e) {
        // ignored
      }
    }
  }

  @Override
  public void clearFilter() {
    datePicker.setDate(null);
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    if (datePicker.getDate() == null) {
      return true;
    }

    Calendar datePickerCalendar = datePicker.getCalendar(); // in localtime

    try {
      for (TvShowEpisode episode : episodes) {
        if (episode.isDummy()) {
          continue;
        }

        calendar.setTime(episode.getDateAddedForUi()); // movie date in UTC
        boolean foundEpisode = DateUtils.isSameDay(datePickerCalendar, calendar);

        if (invert && !foundEpisode) {
          return true;
        }
        else if (!invert && foundEpisode) {
          return true;
        }
      }
    }
    catch (Exception e) {
      return true;
    }

    return false;
  }
}
