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

import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.ACTOR;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.COUNTRY;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.DIRECTOR;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.FILENAME;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.NOTE;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.PRODUCTION_COMPANY;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.TAGS;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.WRITER;

import java.util.List;
import java.util.regex.Matcher;

import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * the class {@link TvShowAllInOneFilter} implements a generic text field filter
 *
 * @author Manuel Laggner
 */
public class TvShowAllInOneFilter extends AbstractTextTvShowUIFilter {

  private final TvShowSettings       settings;

  private final TvShowNoteFilter     tvShowNoteFilter;
  private final TvShowFilenameFilter tvShowFilenameFilter;
  private final TvShowStudioFilter   tvShowStudioFilter;
  private final TvShowCountryFilter  tvShowCountryFilter;

  public TvShowAllInOneFilter() {
    super();
    settings = TvShowModuleManager.getInstance().getSettings();

    tvShowNoteFilter = new TvShowNoteFilter();
    tvShowFilenameFilter = new TvShowFilenameFilter();
    tvShowStudioFilter = new TvShowStudioFilter();
    tvShowCountryFilter = new TvShowCountryFilter();
  }

  @Override
  protected JLabel createLabel() {
    TmmLabel label = new TmmLabel(TmmResourceBundle.getString("filter.universal"));
    label.setHintIcon(IconManager.HINT);
    label.setToolTipText(TmmResourceBundle.getString("filter.universal.hint2"));
    return label;
  }

  @Override
  public String getId() {
    return "tvShowAllInOne";
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    if (StringUtils.isBlank(normalizedFilterText)) {
      return true;
    }

    List<AbstractSettings.UniversalFilterFields> filterFields = settings.getUniversalFilterFields();

    // note
    if (filterFields.contains(NOTE)) {
      setFields(tvShowNoteFilter);
      if (tvShowNoteFilter.accept(tvShow, episodes, invert)) {
        return true;
      }
    }

    // file name
    if (filterFields.contains(FILENAME)) {
      setFields(tvShowFilenameFilter);
      if (tvShowFilenameFilter.accept(tvShow, episodes, invert)) {
        return true;
      }
    }

    // production company
    if (filterFields.contains(PRODUCTION_COMPANY)) {
      setFields(tvShowStudioFilter);
      if (tvShowStudioFilter.accept(tvShow, episodes, invert)) {
        return true;
      }
    }

    // country
    if (filterFields.contains(COUNTRY)) {
      setFields(tvShowCountryFilter);
      if (tvShowCountryFilter.accept(tvShow, episodes, invert)) {
        return true;
      }
    }

    // actors
    if (filterFields.contains(ACTOR) && filterActors(tvShow, episodes, invert)) {
      return true;
    }

    // directors
    if (filterFields.contains(DIRECTOR) && filterDirectors(episodes, invert)) {
      return true;
    }

    // writers
    if (filterFields.contains(WRITER) && filterWriters(episodes, invert)) {
      return true;
    }

    // tags
    if (filterFields.contains(TAGS) && filterTags(tvShow, episodes, invert)) {
      return true;
    }

    return false;
  }

  private void setFields(AbstractTextTvShowUIFilter filter) {
    filter.textField = this.textField;
    filter.filterPattern = this.filterPattern;
    filter.normalizedFilterText = this.normalizedFilterText;
  }

  private boolean filterActors(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    try {
      // first: filter on the base cast of the TV show
      boolean foundShow = false;
      for (Person actor : tvShow.getActors()) {
        Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(actor.getName()));
        if (matcher.find()) {
          foundShow = true;
        }
      }

      // if we found anything in the show we can quit here
      if (!invert && foundShow) {
        return true;
      }
      else if (invert && foundShow) {
        return false;
      }

      // second: filter director/writer and guests from episodes
      for (TvShowEpisode episode : episodes) {
        boolean found = false;

        for (Person actor : episode.getActors()) {
          if (StringUtils.isNotBlank(actor.getName())) {
            Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(actor.getName()));
            if (matcher.find()) {
              found = true;
              break;
            }
          }
        }

        // if there is a match in this episode, we can stop
        if (invert && !found) {
          return true;
        }
        else if (!invert && found) {
          return true;
        }
      }
    }
    catch (Exception e) {
      // fall through
    }

    return false;
  }

  private boolean filterDirectors(List<TvShowEpisode> episodes, boolean invert) {
    try {
      // filter director from episodes
      for (TvShowEpisode episode : episodes) {
        boolean found = false;

        for (Person director : episode.getDirectors()) {
          if (StringUtils.isNotBlank(director.getName())) {
            Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(director.getName()));
            if (matcher.find()) {
              found = true;
              break;
            }
          }
        }

        // if there is a match in this episode, we can stop
        if (invert && !found) {
          return true;
        }
        else if (!invert && found) {
          return true;
        }
      }
    }
    catch (Exception e) {
      // fall through
    }

    return false;
  }

  private boolean filterWriters(List<TvShowEpisode> episodes, boolean invert) {
    try {
      // filter director from episodes
      for (TvShowEpisode episode : episodes) {
        boolean found = false;

        for (Person writer : episode.getWriters()) {
          if (StringUtils.isNotBlank(writer.getName())) {
            Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(writer.getName()));
            if (matcher.find()) {
              found = true;
              break;
            }
          }
        }

        // if there is a match in this episode, we can stop
        if (invert && !found) {
          return true;
        }
        else if (!invert && found) {
          return true;
        }
      }
    }
    catch (Exception e) {
      // fall through
    }

    return false;
  }

  protected boolean filterTags(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    try {
      // search tags of the show
      boolean foundShow = false;
      for (String tag : tvShow.getTags()) {
        if (StringUtils.isNotBlank(tag)) {
          Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(tag));
          if (matcher.find()) {
            foundShow = true;
            break;
          }
        }
      }

      // if we found anything in the show we can quit here
      if (!invert && foundShow) {
        return true;
      }
      else if (invert && foundShow) {
        return false;
      }

      for (TvShowEpisode episode : episodes) {
        boolean foundEpisode = false;
        for (String tag : episode.getTags()) {
          if (StringUtils.isNotBlank(tag)) {
            Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(tag));
            if (matcher.find()) {
              foundEpisode = true;
              break;
            }
          }
        }

        // if there is a match in this episode, we can stop
        if (invert && !foundEpisode) {
          return true;
        }
        else if (!invert && foundEpisode) {
          return true;
        }
      }
    }
    catch (Exception e) {
      // fall through
    }

    return false;
  }
}
