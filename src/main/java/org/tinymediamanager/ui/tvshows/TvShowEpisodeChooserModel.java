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
package org.tinymediamanager.ui.tvshows;

import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroupType.AIRED;

import java.util.Date;

import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;

/**
 * The class TvShowEpisodeChooserModel
 * 
 * @author Manuel Laggner
 */
public class TvShowEpisodeChooserModel extends AbstractModelObject {

  public static final TvShowEpisodeChooserModel emptyResult = new TvShowEpisodeChooserModel();

  private MediaMetadata                         mediaMetadata;
  private String                                originalTitle;
  private String                                title;
  private String                                overview;
  private Date                                  firstAired;
  private int                                   season;
  private int                                   episode;

  public TvShowEpisodeChooserModel(MediaMetadata episode) {
    this.mediaMetadata = episode;

    setTitle(episode.getTitle());
    setOverview(mediaMetadata.getPlot());
    setSeason(mediaMetadata.getEpisodeNumber(AIRED));
    setEpisode(mediaMetadata.getEpisodeNumber(AIRED));
    setFirstAired(mediaMetadata.getReleaseDate());
  }

  private TvShowEpisodeChooserModel() {
    setTitle(TmmResourceBundle.getString("chooser.nothingfound"));
  }

  public void setTitle(String title) {
    String oldValue = this.title;
    this.title = title;
    firePropertyChange("title", oldValue, title);
  }

  public void setOverview(String overview) {
    String oldValue = this.overview;
    this.overview = overview;
    firePropertyChange("overview", oldValue, overview);
  }

  public void setSeason(MediaEpisodeNumber episodeNumber) {
    int oldValue = this.season;

    if (episodeNumber == null) {
      this.season = -1;
    }
    else {
      this.season = episodeNumber.season();
    }

    firePropertyChange("season", oldValue, season);
  }

  public void setEpisode(MediaEpisodeNumber episodeNumber) {
    int oldValue = this.episode;

    if (episodeNumber == null) {
      this.episode = -1;
    }
    else {
      this.episode = episodeNumber.episode();
    }

    firePropertyChange("episode", oldValue, episode);
  }

  public void setFirstAired(Date firstAired) {
    Date oldValue = this.firstAired;
    this.firstAired = firstAired;
    firePropertyChange("firstAired", oldValue, firstAired);
  }

  public String getTitle() {
    return title;
  }

  public String getOverview() {
    return overview;
  }

  public int getSeason() {
    return season;
  }

  public int getEpisode() {
    return episode;
  }

  public Date getFirstAired() {
    return firstAired;
  }

  public String getFirstAiredFormatted() {
    if (firstAired == null) {
      return "";
    }
    try {
      return TmmDateFormat.MEDIUM_DATE_FORMAT.format(firstAired);
    }
    catch (Exception e) {
      return "";
    }
  }

  public MediaMetadata getMediaMetadata() {
    return mediaMetadata;
  }
}
