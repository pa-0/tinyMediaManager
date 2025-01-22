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

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;
import org.tinymediamanager.ui.components.treetable.TmmTreeTableFormat;
import org.tinymediamanager.ui.renderer.DateTableCellRenderer;
import org.tinymediamanager.ui.renderer.IntegerTableCellRenderer;
import org.tinymediamanager.ui.renderer.RightAlignTableCellRenderer;
import org.tinymediamanager.ui.renderer.RuntimeTableCellRenderer;

/**
 * The class TvShowTableFormat is used to define the columns for the TV show tree table
 *
 * @author Manuel Laggner
 */
public class TvShowTableFormat extends TmmTreeTableFormat<TmmTreeNode> {

  private final TvShowList     tvShowList;
  private final TvShowSettings settings;
  private final Calendar       calendar = Calendar.getInstance();

  public TvShowTableFormat() {
    tvShowList = TvShowModuleManager.getInstance().getTvShowList();
    settings = TvShowModuleManager.getInstance().getSettings();
    FontMetrics fontMetrics = getFontMetrics();

    Comparator<MediaCertification> certificationComparator = new CertificationComparator();
    Comparator<String> stringComparator = new StringComparator();
    Comparator<Integer> integerComparator = Comparator.comparingInt(o -> o);
    Comparator<String> floatComparator = (o1, o2) -> {
      float value1 = 0;
      float value2 = 0;

      try {
        value1 = Float.parseFloat(o1);
      }
      catch (Exception ignored) {
        // do nothing
      }
      try {
        value2 = Float.parseFloat(o2);
      }
      catch (Exception ignored) {
        // do nothing
      }

      return Float.compare(value1, value2);
    };
    Comparator<Date> dateComparator = new DateComparator();
    Comparator<Date> dateTimeComparator = new DateTimeComparator();
    Comparator<ImageIcon> imageComparator = new ImageComparator();
    Comparator<String> videoFormatComparator = new VideoFormatComparator();
    Comparator<String> fileSizeComparator = new FileSizeComparator();

    /*
     * Original Title
     */
    Column col = new Column(TmmResourceBundle.getString("metatag.originaltitle"), "originalTitle", this::getOriginialTitle, String.class);
    col.setDefaultHidden(true);
    col.setColumnComparator(stringComparator);
    addColumn(col);

    /*
     * year
     */
    col = new Column(TmmResourceBundle.getString("metatag.year"), "year", this::getYear, Integer.class);
    col.setCellRenderer(new IntegerTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("2000") + getCellPadding());
    col.setColumnComparator(integerComparator);
    addColumn(col);

    /*
     * season count
     */
    col = new Column(TmmResourceBundle.getString("metatag.season.count"), "seasons", this::getSeasons, Integer.class);
    col.setHeaderIcon(IconManager.SEASONS);
    col.setCellRenderer(new IntegerTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("99") + getCellPadding());
    col.setColumnComparator(integerComparator);
    addColumn(col);

    /*
     * episode count
     */
    col = new Column(TmmResourceBundle.getString("metatag.episode.count"), "episodes", this::getEpisodes, Integer.class);
    col.setHeaderIcon(IconManager.EPISODES);
    col.setCellRenderer(new IntegerTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("999") + getCellPadding());
    col.setColumnComparator(integerComparator);
    addColumn(col);

    /*
     * missing episode count (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.missingepisode.count"), "missingEpisodes", this::getMissingEpisodes, Integer.class);
    col.setHeaderIcon(IconManager.MISSING);
    col.setCellRenderer(new IntegerTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("999") + getCellPadding());
    col.setColumnComparator(integerComparator);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * TV show status (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.status"), "status", this::getStatus, String.class);
    col.setMinWidth(fontMetrics.stringWidth("Continuing") + getCellPadding());
    col.setColumnComparator(stringComparator);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * file name (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.filename"), "filename", this::getFileName, String.class);
    col.setColumnResizeable(true);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * folder name (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.path"), "path", this::getFolderPath, String.class);
    col.setColumnComparator(stringComparator);
    col.setColumnResizeable(true);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.rating"), "rating", this::getRating, String.class);
    col.setHeaderIcon(IconManager.RATING);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("99.9") + getCellPadding());
    col.setColumnComparator(floatComparator);
    addColumn(col);

    /*
     * votes (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.votes"), "votes", this::getVotes, Integer.class);
    col.setHeaderIcon(IconManager.VOTES);
    col.setCellRenderer(new IntegerTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("1000000") + getCellPadding());
    col.setDefaultHidden(true);
    col.setColumnComparator(integerComparator);
    addColumn(col);

    /*
     * user rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.userrating"), "userrating", this::getUserRating, Float.class);
    col.setHeaderIcon(IconManager.USER_RATING);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("99.9") + getCellPadding());
    col.setColumnComparator(floatComparator);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * imdb rating (hidden per default)
     */
    col = new Column("IMDb", "imdb", this::getImdbRating, Float.class);
    col.setColumnComparator(floatComparator);
    col.setHeaderTooltip(TmmResourceBundle.getString("metatag.rating") + " - IMDb");
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("IMDb") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * aired (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.aired"), "aired", this::getAiredDate, Date.class);
    col.setHeaderIcon(IconManager.DATE_AIRED);
    col.setCellRenderer(new DateTableCellRenderer());
    col.setColumnResizeable(false);
    try {
      Date date = StrgUtils.parseDate("2012-12-12");
      col.setMinWidth(fontMetrics.stringWidth(TmmDateFormat.MEDIUM_DATE_FORMAT.format(date)) + getCellPadding());
    }
    catch (Exception ignored) {
      // ignored
    }
    col.setColumnComparator(dateComparator);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * top 250 (hidden per default)
     */
    col = new Column("T250", "top250", this::getTop250, Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderTooltip(TmmResourceBundle.getString("metatag.top250"));
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("250") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * certification (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.certification"), "certification", this::getCertification, MediaCertification.class);
    col.setColumnComparator(certificationComparator);
    col.setHeaderIcon(IconManager.CERTIFICATION);
    col.setColumnResizeable(true);
    col.setMinWidth(fontMetrics.stringWidth("not rated") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * date added (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.dateadded") + " (" + TmmResourceBundle.getString("metatag.tvshow") + ")", "dateAdded",
        this::getDateAdded, Date.class);
    col.setHeaderIcon(IconManager.DATE_ADDED);
    col.setCellRenderer(new DateTableCellRenderer());
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    try {
      Date date = StrgUtils.parseDate("2012-12-12");
      col.setMinWidth(fontMetrics.stringWidth(TmmDateFormat.MEDIUM_DATE_FORMAT.format(date)) + getCellPadding());
    }
    catch (Exception ignored) {
      // ignored
    }
    col.setColumnComparator(dateTimeComparator);
    addColumn(col);

    /*
     * date added2 - sort by latest episode (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.dateadded") + " (" + TmmResourceBundle.getString("metatag.episode") + ")",
        "dateAddedEpisode", this::getDateAddedEpisode, Date.class);
    col.setHeaderIcon(IconManager.DATE_ADDED);
    col.setCellRenderer(new DateTableCellRenderer());
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    try {
      Date date = StrgUtils.parseDate("2012-12-12");
      col.setMinWidth(fontMetrics.stringWidth(TmmDateFormat.MEDIUM_DATE_FORMAT.format(date)) + getCellPadding());
    }
    catch (Exception ignored) {
      // ignored
    }
    col.setColumnComparator(dateTimeComparator);
    addColumn(col);

    /*
     * file creation date (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.filecreationdate"), "fileCreationDate", this::getFileCreationDate, Date.class);
    col.setHeaderIcon(IconManager.DATE_CREATED);
    col.setCellRenderer(new DateTableCellRenderer());
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    try {
      Date date = StrgUtils.parseDate("2012-12-12");
      col.setMinWidth(fontMetrics.stringWidth(TmmDateFormat.MEDIUM_DATE_FORMAT.format(date)) + getCellPadding());
    }
    catch (Exception ignored) {
      // ignored
    }
    col.setColumnComparator(dateTimeComparator);
    addColumn(col);

    /*
     * runtime (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.runtime") + " [min]", "runtime", this::getRuntime, Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderIcon(IconManager.RUNTIME);
    col.setCellRenderer(new RuntimeTableCellRenderer(RuntimeTableCellRenderer.FORMAT.MINUTES));
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("200") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * runtime HH:MM (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.runtime") + " [hh:mm]", "runtime2", this::getRuntime, Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderIcon(IconManager.RUNTIME);
    col.setCellRenderer(new RuntimeTableCellRenderer(RuntimeTableCellRenderer.FORMAT.HOURS_MINUTES));
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("4:00") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * video format (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.format"), "format", this::getFormat, String.class);
    col.setHeaderIcon(IconManager.VIDEO_FORMAT);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("1080p") + getCellPadding());
    col.setDefaultHidden(true);
    col.setColumnComparator(videoFormatComparator);
    addColumn(col);

    /*
     * HDR (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.hdr"), "hdr", this::isHDR, ImageIcon.class);
    col.setHeaderIcon(IconManager.HDR);
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    col.setColumnComparator(imageComparator);
    addColumn(col);

    /*
     * HDR format (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.hdrformat"), "hdrFormat", this::getVideoHDRFormat, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.HDR);
    col.setMinWidth(fontMetrics.stringWidth("HDR10") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * video codec (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videocodec"), "videoCodec", this::getVideoCodec, String.class);
    col.setHeaderIcon(IconManager.VIDEO_CODEC);
    col.setMinWidth(fontMetrics.stringWidth("MPEG-2") + getCellPadding());
    col.setDefaultHidden(true);
    col.setColumnComparator(stringComparator);
    addColumn(col);

    /*
     * Video Bitrate
     */
    col = new Column(TmmResourceBundle.getString("metatag.videobitrate"), "videoBitrate", this::getVideoBitrate, Integer.class);
    col.setHeaderIcon(IconManager.VIDEO_BITRATE);
    col.setMinWidth(fontMetrics.stringWidth("20000") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * audio codec and channels(hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.audio"), "audio", this::getAudio, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.AUDIO);
    col.setMinWidth(fontMetrics.stringWidth("DTS 7ch") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * main video file size (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videofilesize"), "fileSize", this::getVideoFileSize, String.class);
    col.setHeaderIcon(IconManager.FILE_SIZE);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("500.00 M") + getCellPadding());
    col.setDefaultHidden(true);
    col.setColumnComparator(fileSizeComparator);
    addColumn(col);

    /*
     * total file size (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.totalfilesize"), "totalFileSize", this::getTotalFileSize, String.class);
    col.setHeaderIcon(IconManager.FILE_SIZE);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("500.00 M") + getCellPadding());
    col.setDefaultHidden(true);
    col.setColumnComparator(fileSizeComparator);
    addColumn(col);

    /*
     * aspect ratio (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.aspectratio"), "aspectratio", this::getAspectRatio, Float.class);
    col.setHeaderIcon(IconManager.ASPECT_RATIO);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("1.78") + getCellPadding());
    col.setDefaultHidden(true);
    col.setColumnComparator(floatComparator);
    addColumn(col);

    /*
     * Source (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.source"), "mediaSource", this::getMediaSource, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.SOURCE);
    col.setMinWidth(fontMetrics.stringWidth("Blu-ray") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * new indicator
     */
    col = new Column(TmmResourceBundle.getString("movieextendedsearch.newepisodes"), "new", this::getNewIcon, ImageIcon.class);
    col.setHeaderIcon(IconManager.NEW);
    col.setColumnResizeable(false);
    col.setCellTooltip(this::newTooltip);
    col.setColumnComparator(imageComparator);
    addColumn(col);

    /*
     * metadata
     */
    col = new Column(TmmResourceBundle.getString("tmm.metadata"), "metadata", this::hasMetadata, ImageIcon.class);
    col.setHeaderIcon(IconManager.NFO);
    col.setColumnResizeable(false);
    col.setCellTooltip(this::hasMetadataTooltip);
    col.setColumnComparator(imageComparator);
    addColumn(col);

    /*
     * images
     */
    col = new Column(TmmResourceBundle.getString("tmm.images"), "images", this::hasArtwork, ImageIcon.class);
    col.setHeaderIcon(IconManager.IMAGES);
    col.setColumnResizeable(false);
    col.setCellTooltip(this::hasImageTooltip);
    col.setColumnComparator(imageComparator);
    addColumn(col);

    /*
     * subtitles
     */
    col = new Column(TmmResourceBundle.getString("tmm.subtitles"), "subtitles", this::hasSubtitles, ImageIcon.class);
    col.setHeaderIcon(IconManager.SUBTITLES);
    col.setColumnResizeable(false);
    col.setColumnComparator(imageComparator);
    addColumn(col);

    /*
     * watched
     */
    col = new Column(TmmResourceBundle.getString("metatag.watched"), "watched", this::isWatched, ImageIcon.class);
    col.setHeaderIcon(IconManager.WATCHED);
    col.setColumnResizeable(false);
    col.setColumnComparator(imageComparator);
    addColumn(col);

    /*
     * has downloaded theme
     */
    col = new Column(TmmResourceBundle.getString("metatag.musictheme"), "theme", this::hasDownloadedMusicTheme, ImageIcon.class);
    col.setHeaderIcon(IconManager.MUSIC_HEADER);
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    col.setColumnComparator(imageComparator);
    addColumn(col);

    /*
     * has Note (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.note"), "note", this::hasNote, ImageIcon.class);
    col.setHeaderIcon(IconManager.INFO);
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    col.setColumnComparator(imageComparator);
    addColumn(col);

  }

  @Override
  public String getColumnName(int i) {
    if (i == NODE_COLUMN) {
      return TmmResourceBundle.getString("metatag.title");
    }
    else {
      return super.getColumnName(i);
    }
  }

  private Integer getYear(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return tvShow.getYear();
    }
    else if (userObject instanceof TvShowSeason season) {
      Date firstAired = season.getFirstAired();
      if (firstAired != null) {
        calendar.setTime(firstAired);
        return calendar.get(Calendar.YEAR);
      }
      return null;
    }
    return null;
  }

  private Integer getSeasons(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return tvShow.getSeasonCount();
    }
    return null;
  }

  private Integer getEpisodes(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return tvShow.getEpisodeCount();
    }
    if (userObject instanceof TvShowSeason season) {
      if (!season.getEpisodes().isEmpty()) {
        return season.getEpisodes().size();
      }
    }
    return null;
  }

  private Integer getMissingEpisodes(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return tvShow.getDummyEpisodeCount();
    }
    if (userObject instanceof TvShowSeason season) {
      return season.getDummyEpisodeCount();
    }
    return null;
  }

  private String getStatus(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return tvShow.getStatus().getLocalizedName();
    }
    return null;
  }

  private String getFileName(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode episode) {
      return episode.getMainVideoFile().getFilename();
    }
    return null;
  }

  private String getFolderPath(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return tvShow.getPathNIO().toAbsolutePath().toString();
    }
    else if (userObject instanceof TvShowEpisode episode) {
      if (episode.getPathNIO() != null) {
        return episode.getPathNIO().toAbsolutePath().toString();
      }
    }
    return null;
  }

  private ImageIcon getNewIcon(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      if (tvShow.isNewlyAdded()) {
        return IconManager.NEW_GREEN;
      }
      else if (tvShow.hasNewlyAddedEpisodes()) {
        return IconManager.NEW_ORANGE;
      }
    }
    else if (userObject instanceof TvShowSeason season) {
      return getNewIcon(season.isNewlyAdded());
    }
    else if (userObject instanceof TvShowEpisode episode) {
      return getNewIcon(episode.isNewlyAdded());
    }
    return null;
  }

  private String newTooltip(TmmTreeNode node) {
    if (!settings.isShowTvShowTableTooltips()) {
      return null;
    }
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      if (tvShow.isNewlyAdded()) {
        return TmmResourceBundle.getString("tvshow.new");
      }
      else if (tvShow.hasNewlyAddedEpisodes()) {
        return TmmResourceBundle.getString("tvshowepisode.new");
      }
    }
    else if (userObject instanceof TvShowSeason season) {
      if (season.isNewlyAdded()) {
        return TmmResourceBundle.getString("tvshowepisode.new");
      }
    }
    else if (userObject instanceof TvShowEpisode episode) {
      if (episode.isNewlyAdded()) {
        return TmmResourceBundle.getString("tvshowepisode.new");
      }
    }
    return null;
  }

  private String getRating(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow || userObject instanceof TvShowEpisode) {
      MediaRating mediaRating = ((MediaEntity) userObject).getRating();
      if (mediaRating != null && mediaRating != MediaMetadata.EMPTY_RATING && mediaRating.getRating() > 0) {
        return String.valueOf(mediaRating.getRating());
      }
    }
    return null;
  }

  private Integer getVotes(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow || userObject instanceof TvShowEpisode) {
      MediaRating mediaRating = ((MediaEntity) userObject).getRating();
      if (mediaRating != null && mediaRating != MediaMetadata.EMPTY_RATING && mediaRating.getRating() > 0) {
        return mediaRating.getVotes();
      }
    }
    return null;
  }

  private String getUserRating(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow || userObject instanceof TvShowEpisode) {
      MediaRating mediaRating = ((MediaEntity) userObject).getUserRating();
      if (mediaRating != null && mediaRating.getRating() > 0) {
        return String.valueOf(mediaRating.getRating());
      }
    }
    return null;
  }

  private String getImdbRating(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow || userObject instanceof TvShowEpisode) {
      MediaRating mediaRating = ((MediaEntity) userObject).getRating(MediaMetadata.IMDB);
      if (mediaRating != null && mediaRating.getRating() > 0) {
        return String.valueOf(mediaRating.getRating());
      }
    }
    return null;
  }

  private Date getAiredDate(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return tvShow.getFirstAired();
    }
    if (userObject instanceof TvShowSeason season) {
      return season.getFirstAired();
    }
    if (userObject instanceof TvShowEpisode episode) {
      return episode.getFirstAired();
    }
    return null;
  }

  private Date getDateAdded(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return tvShow.getDateAddedForUi();
    }
    if (userObject instanceof TvShowEpisode episode) {
      if (!episode.isDummy()) {
        return episode.getDateAddedForUi();
      }
    }
    return null;
  }

  private Date getDateAddedEpisode(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      List<TvShowEpisode> modifyableList = new ArrayList<TvShowEpisode>(tvShow.getEpisodes());
      if (!modifyableList.isEmpty()) {
        TvShowEpisode latest = Collections.max(modifyableList, Comparator.comparing(TvShowEpisode::getDateAddedForUi));
        return latest.getDateAddedForUi();
      }
    }
    if (userObject instanceof TvShowEpisode episode) {
      if (!episode.isDummy()) {
        return episode.getDateAddedForUi();
      }
    }
    return null;
  }

  private Date getFileCreationDate(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode episode) {
      if (!episode.isDummy()) {
        return episode.getMainVideoFile().getDateCreated();
      }
    }
    return null;
  }

  private String getFormat(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode episode) {
      return episode.getMediaInfoVideoFormat();
    }
    else if (userObject instanceof TvShowSeason season) {
      return detectUniqueVideoFormat(season.getEpisodes());
    }
    else if (userObject instanceof TvShow tvShow) {
      return detectUniqueVideoFormat(tvShow.getEpisodes());
    }
    return "";
  }

  private String detectUniqueVideoFormat(List<TvShowEpisode> episodes) {
    if (episodes.isEmpty()) {
      return null;
    }

    // return the source only if _all_ episode have the same video format
    String videoFormat = episodes.get(0).getMediaInfoVideoFormat();
    for (TvShowEpisode episode : episodes) {
      if (!videoFormat.equals(episode.getMediaInfoVideoFormat())) {
        return null;
      }
    }

    return videoFormat;
  }

  private String getVideoCodec(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode episode) {
      return episode.getMediaInfoVideoCodec();
    }
    else if (userObject instanceof TvShowSeason season) {
      return detectUniqueVideoCodec(season.getEpisodes());
    }
    else if (userObject instanceof TvShow tvShow) {
      return detectUniqueVideoCodec(tvShow.getEpisodes());
    }
    return "";
  }

  private String detectUniqueVideoCodec(List<TvShowEpisode> episodes) {
    if (episodes.isEmpty()) {
      return null;
    }

    // return the source only if _all_ episode have the same video codec
    String videoCodec = episodes.get(0).getMediaInfoVideoCodec();
    for (TvShowEpisode episode : episodes) {
      if (!videoCodec.equals(episode.getMediaInfoVideoCodec())) {
        return null;
      }
    }

    return videoCodec;
  }

  private Integer getVideoBitrate(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode episode) {
      return episode.getMediaInfoVideoBitrate();
    }
    return null;
  }

  private String getAudio(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode episode) {
      return episode.getMediaInfoAudioCodec() + " " + episode.getMediaInfoAudioChannels();
    }
    else if (userObject instanceof TvShowSeason season) {
      return detectUniqueAudio(season.getEpisodes());
    }
    else if (userObject instanceof TvShow tvShow) {
      return detectUniqueAudio(tvShow.getEpisodes());
    }
    return null;
  }

  private String detectUniqueAudio(List<TvShowEpisode> episodes) {
    if (episodes.isEmpty()) {
      return null;
    }

    // return the source only if _all_ episode have the audio codec/channels
    String audio = episodes.get(0).getMediaInfoAudioCodec() + " " + episodes.get(0).getMediaInfoAudioChannels();
    for (TvShowEpisode episode : episodes) {
      String otherAudio = episode.getMediaInfoAudioCodec() + " " + episode.getMediaInfoAudioChannels();
      if (!audio.equals(otherAudio)) {
        return null;
      }
    }

    return audio;
  }

  private MediaSource getMediaSource(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode episode) {
      return episode.getMediaSource();
    }
    else if (userObject instanceof TvShowSeason season) {
      return detectUniqueSource(season.getEpisodes());
    }
    else if (userObject instanceof TvShow tvShow) {
      return detectUniqueSource(tvShow.getEpisodes());
    }
    return null;
  }

  private MediaSource detectUniqueSource(List<TvShowEpisode> episodes) {
    if (episodes.isEmpty()) {
      return null;
    }

    // return the source only if _all_ episode have the same source
    MediaSource source = episodes.get(0).getMediaSource();
    for (TvShowEpisode episode : episodes) {
      if (source != episode.getMediaSource()) {
        return null;
      }
    }

    return source;
  }

  private Integer getRuntime(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode episode) {
      return episode.getRuntimeFromMediaFilesInMinutes();
    }
    else if (userObject instanceof TvShow show) {
      // show the scraped runtime here
      return show.getRuntime();
    }
    return null;
  }

  private Integer getTop250(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow show) {
      return show.getTop250() > 0 ? show.getTop250() : null; // mostly 0, do not show
    }
    return null;
  }

  private String getVideoFileSize(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return Utils.formatFileSizeForDisplay(tvShow.getVideoFilesize());
    }
    if (userObject instanceof TvShowSeason season) {
      return Utils.formatFileSizeForDisplay(season.getVideoFilesize());
    }
    if (userObject instanceof TvShowEpisode episode) {
      return Utils.formatFileSizeForDisplay(episode.getVideoFilesize());
    }
    return null;
  }

  private String getTotalFileSize(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return Utils.formatFileSizeForDisplay(tvShow.getTotalFilesize());
    }
    if (userObject instanceof TvShowSeason season) {
      return Utils.formatFileSizeForDisplay(season.getTotalFilesize());
    }
    if (userObject instanceof TvShowEpisode episode) {
      return Utils.formatFileSizeForDisplay(episode.getTotalFilesize());
    }
    return null;
  }

  private Float getAspectRatio(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode episode) {
      return episode.getMediaInfoAspectRatio();
    }
    else if (userObject instanceof TvShowSeason season) {
      return detectUniqueAspectRatio(season.getEpisodes());
    }
    else if (userObject instanceof TvShow tvShow) {
      return detectUniqueAspectRatio(tvShow.getEpisodes());
    }
    return null;
  }

  private Float detectUniqueAspectRatio(List<TvShowEpisode> episodes) {
    if (episodes.isEmpty()) {
      return null;
    }

    // return the source only if _all_ episode have the aspect ratio
    float aspectRatio = episodes.get(0).getMediaInfoAspectRatio();
    for (TvShowEpisode episode : episodes) {
      if (aspectRatio != episode.getMediaInfoAspectRatio()) {
        return null;
      }
    }

    return aspectRatio;
  }

  private ImageIcon isHDR(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode episode) {
      return getCheckIcon(StringUtils.isNotEmpty(episode.getVideoHDRFormat()));
    }
    else if (userObject instanceof TvShowSeason season) {
      return detectUniqueIsHDR(season.getEpisodes());
    }
    else if (userObject instanceof TvShow tvShow) {
      return detectUniqueIsHDR(tvShow.getEpisodes());
    }
    return null;
  }

  private ImageIcon detectUniqueIsHDR(List<TvShowEpisode> episodes) {
    if (episodes.isEmpty()) {
      return null;
    }

    // return the source only if _all_ episode have the same HDR format
    String hdrFormat = episodes.get(0).getVideoHDRFormat();
    for (TvShowEpisode episode : episodes) {
      if (!hdrFormat.equals(episode.getVideoHDRFormat())) {
        return null;
      }
    }

    return getCheckIcon(StringUtils.isNotEmpty(hdrFormat));
  }

  private String getVideoHDRFormat(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode episode) {
      return episode.getVideoHDRFormat();
    }
    else if (userObject instanceof TvShowSeason season) {
      return detectUniqueHDRFormat(season.getEpisodes());
    }
    else if (userObject instanceof TvShow tvShow) {
      return detectUniqueHDRFormat(tvShow.getEpisodes());
    }
    return null;
  }

  private String detectUniqueHDRFormat(List<TvShowEpisode> episodes) {
    if (episodes.isEmpty()) {
      return null;
    }

    // return the source only if _all_ episode have the same HDR format
    String hdrFormat = episodes.get(0).getVideoHDRFormat();
    for (TvShowEpisode episode : episodes) {
      if (!hdrFormat.equals(episode.getVideoHDRFormat())) {
        return null;
      }
    }

    return hdrFormat;
  }

  private ImageIcon hasMetadata(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return getTriStateIcon(TRI_STATE.getState(tvShowList.detectMissingMetadata(tvShow).isEmpty(), tvShow.getHasEpisodeMetadata()));
    }
    else if (userObject instanceof TvShowSeason season) {
      return getTriStateIcon(TRI_STATE.getState(tvShowList.detectMissingMetadata(season).isEmpty(), season.getHasEpisodeMetadata()));
    }
    else if (userObject instanceof TvShowEpisode episode) {
      return getCheckIcon(tvShowList.detectMissingMetadata(episode).isEmpty());
    }
    return null;
  }

  private ImageIcon hasArtwork(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return getTriStateIcon(TRI_STATE.getState(tvShowList.detectMissingArtwork(tvShow).isEmpty(), tvShow.getHasSeasonAndEpisodeImages()));
    }
    else if (userObject instanceof TvShowSeason season) {
      return getTriStateIcon(TRI_STATE.getState(tvShowList.detectMissingArtwork(season).isEmpty(), season.getHasEpisodeImages()));
    }
    else if (userObject instanceof TvShowEpisode episode) {
      return getCheckIcon(tvShowList.detectMissingArtwork(episode).isEmpty());
    }
    return null;
  }

  private ImageIcon hasSubtitles(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return getCheckIcon(tvShow.hasEpisodeSubtitles());
    }
    else if (userObject instanceof TvShowSeason season) {
      return getCheckIcon(season.hasEpisodeSubtitles());
    }
    else if (userObject instanceof TvShowEpisode episode) {
      return getCheckIcon(episode.getHasSubtitles());
    }
    return null;
  }

  private ImageIcon isWatched(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return getCheckIcon(tvShow.isWatched());
    }
    else if (userObject instanceof TvShowSeason season) {
      return getCheckIcon(season.isWatched());
    }
    else if (userObject instanceof TvShowEpisode episode) {
      return getCheckIcon(episode.isWatched());
    }
    return null;
  }

  private MediaCertification getCertification(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return tvShow.getCertification();
    }
    return null;
  }

  private String hasMetadataTooltip(TmmTreeNode node) {
    if (!settings.isShowTvShowTableTooltips()) {
      return null;
    }

    List<TvShowScraperMetadataConfig> tvShowValues = new ArrayList<>();
    if (settings.isTvShowDisplayAllMissingMetadata()) {
      for (TvShowScraperMetadataConfig config : TvShowScraperMetadataConfig.values()) {
        if (config.isMetaData() || config.isCast()) {
          tvShowValues.add(config);
        }
      }
    }
    else {
      tvShowValues.addAll(settings.getTvShowCheckMetadata());
    }

    List<TvShowScraperMetadataConfig> seasonValues = new ArrayList<>();
    if (settings.isTvShowDisplayAllMissingMetadata()) {
      for (TvShowScraperMetadataConfig config : TvShowScraperMetadataConfig.values()) {
        if (config.isMetaData() && config.name().startsWith("SEASON")) {
          seasonValues.add(config);
        }
      }
    }
    else {
      for (TvShowScraperMetadataConfig config : settings.getTvShowCheckMetadata()) {
        if (config.isMetaData() && config.name().startsWith("SEASON")) {
          seasonValues.add(config);
        }
      }
    }

    List<TvShowEpisodeScraperMetadataConfig> episodeValues = new ArrayList<>();
    if (settings.isEpisodeDisplayAllMissingMetadata()) {
      for (TvShowEpisodeScraperMetadataConfig config : TvShowEpisodeScraperMetadataConfig.values()) {
        if (config.isMetaData() || config.isCast()) {
          episodeValues.add(config);
        }
      }
    }
    else {
      episodeValues.addAll(settings.getEpisodeCheckMetadata());
    }

    if (node.getUserObject() instanceof TvShow tvShow) {
      List<TvShowScraperMetadataConfig> missingMetadata = tvShowList.detectMissingFields(tvShow, tvShowValues);
      boolean missingEpisodeData = false;

      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        if (episode.isDummy()
            || (episode.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isEpisodeSpecialsCheckMissingMetadata())) {
          continue;
        }
        if (!tvShowList.detectMissingFields(episode, episodeValues).isEmpty()) {
          missingEpisodeData = true;
          break;
        }
      }

      String text = "";

      if (!missingMetadata.isEmpty()) {
        StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");
        for (TvShowScraperMetadataConfig metadataConfig : missingMetadata) {
          missing.append("\n").append(metadataConfig.getDescription());
        }

        text = missing.toString();
      }

      if (missingEpisodeData) {
        if (StringUtils.isNotBlank(text)) {
          text += "\n\n";
        }
        text += TmmResourceBundle.getString("tvshow.tree.episode.metadata.problem");
      }

      if (StringUtils.isNotBlank(text)) {
        return text;
      }
    }
    else if (node.getUserObject() instanceof TvShowSeason season) {
      List<TvShowScraperMetadataConfig> missingMetadata = tvShowList.detectMissingFields(season, seasonValues);
      boolean missingEpisodeData = false;

      for (TvShowEpisode episode : season.getEpisodes()) {
        if (episode.isDummy()
            || (episode.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isEpisodeSpecialsCheckMissingMetadata())) {
          continue;
        }
        if (!tvShowList.detectMissingFields(episode, episodeValues).isEmpty()) {
          missingEpisodeData = true;
          break;
        }
      }

      String text = "";

      if (!missingMetadata.isEmpty()) {
        StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");
        for (TvShowScraperMetadataConfig metadataConfig : missingMetadata) {
          missing.append("\n").append(metadataConfig.getDescription());
        }

        text = missing.toString();
      }

      if (missingEpisodeData) {
        if (StringUtils.isNotBlank(text)) {
          text += "\n\n";
        }
        text += TmmResourceBundle.getString("tvshow.tree.episode.metadata.problem");
      }

      if (StringUtils.isNotBlank(text)) {
        return text;
      }
    }
    else if (node.getUserObject() instanceof TvShowEpisode episode) {
      List<TvShowEpisodeScraperMetadataConfig> missingMetadata = tvShowList.detectMissingFields(episode, episodeValues);
      if (!missingMetadata.isEmpty()) {
        StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");
        for (TvShowEpisodeScraperMetadataConfig metadataConfig : missingMetadata) {
          missing.append("\n").append(metadataConfig.getDescription());
        }

        return missing.toString();
      }
    }
    return null;
  }

  private String hasImageTooltip(TmmTreeNode node) {
    if (!settings.isShowTvShowTableTooltips()) {
      return null;
    }

    List<TvShowScraperMetadataConfig> tvShowValues = new ArrayList<>();
    if (settings.isTvShowDisplayAllMissingArtwork()) {
      for (TvShowScraperMetadataConfig config : TvShowScraperMetadataConfig.values()) {
        if (config.isArtwork()) {
          tvShowValues.add(config);
        }
      }
    }
    else {
      tvShowValues.addAll(settings.getTvShowCheckArtwork());
    }

    List<TvShowScraperMetadataConfig> seasonValues = new ArrayList<>();
    if (settings.isSeasonDisplayAllMissingArtwork()) {
      for (TvShowScraperMetadataConfig config : TvShowScraperMetadataConfig.values()) {
        if (config.isArtwork() && config.name().startsWith("SEASON")) {
          seasonValues.add(config);
        }
      }
    }
    else {
      seasonValues.addAll(settings.getSeasonCheckArtwork());
    }

    List<TvShowEpisodeScraperMetadataConfig> episodeValues = new ArrayList<>();
    if (settings.isEpisodeDisplayAllMissingArtwork()) {
      for (TvShowEpisodeScraperMetadataConfig config : TvShowEpisodeScraperMetadataConfig.values()) {
        if (config.isArtwork()) {
          episodeValues.add(config);
        }
      }
    }
    else {
      episodeValues.addAll(settings.getEpisodeCheckArtwork());
    }

    if (node.getUserObject() instanceof TvShow tvShow) {
      List<TvShowScraperMetadataConfig> missingMetadata = tvShowList.detectMissingFields(tvShow, tvShowValues);
      boolean missingSeasonEpisodeData = false;

      for (TvShowSeason season : tvShow.getSeasons()) {
        if (!tvShowList.detectMissingFields(season, seasonValues).isEmpty()) {
          missingSeasonEpisodeData = true;
          break;
        }
        for (TvShowEpisode episode : season.getEpisodes()) {
          if (episode.isDummy()
              || (episode.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isEpisodeSpecialsCheckMissingMetadata())) {
            continue;
          }
          if (!tvShowList.detectMissingFields(episode, episodeValues).isEmpty()) {
            missingSeasonEpisodeData = true;
            break;
          }
        }

        if (missingSeasonEpisodeData) {
          break;
        }
      }

      String text = "";

      if (!missingMetadata.isEmpty()) {
        StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");
        for (TvShowScraperMetadataConfig metadataConfig : missingMetadata) {
          missing.append("\n").append(metadataConfig.getDescription());
        }

        text = missing.toString();
      }

      if (missingSeasonEpisodeData) {
        if (StringUtils.isNotBlank(text)) {
          text += "\n\n";
        }
        text += TmmResourceBundle.getString("tvshow.tree.season.artwork.problem");
      }

      if (StringUtils.isNotBlank(text)) {
        return text;
      }
    }
    else if (node.getUserObject() instanceof TvShowSeason season) {
      List<TvShowScraperMetadataConfig> missingMetadata = tvShowList.detectMissingFields(season, seasonValues);
      boolean missingEpisodeData = false;

      for (TvShowEpisode episode : season.getEpisodes()) {
        if (!episode.isDummy() && !tvShowList.detectMissingFields(episode, episodeValues).isEmpty()) {
          missingEpisodeData = true;
          break;
        }
      }

      String text = "";

      if (!missingMetadata.isEmpty()) {
        StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");
        for (TvShowScraperMetadataConfig metadataConfig : missingMetadata) {
          missing.append("\n").append(metadataConfig.getDescription());
        }

        text = missing.toString();
      }

      if (missingEpisodeData) {
        if (StringUtils.isNotBlank(text)) {
          text += "\n\n";
        }
        text += TmmResourceBundle.getString("tvshow.tree.episode.artwork.problem");
      }

      if (StringUtils.isNotBlank(text)) {
        return text;
      }
    }
    else if (node.getUserObject() instanceof TvShowEpisode episode) {
      List<TvShowEpisodeScraperMetadataConfig> missingMetadata = tvShowList.detectMissingFields(episode, episodeValues);
      if (!missingMetadata.isEmpty()) {
        StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");
        for (TvShowEpisodeScraperMetadataConfig metadataConfig : missingMetadata) {
          missing.append("\n").append(metadataConfig.getDescription());
        }

        return missing.toString();
      }
    }
    return null;
  }

  private String getOriginialTitle(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return tvShow.getOriginalTitle();
    }
    else if (userObject instanceof TvShowEpisode episode) {
      return episode.getOriginalTitle();
    }
    return null;
  }

  private ImageIcon hasDownloadedMusicTheme(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return getCheckIcon(tvShow.getHasMusicTheme());
    }
    return null;
  }

  private ImageIcon hasNote(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow tvShow) {
      return getCheckIcon(tvShow.getHasNote());
    }
    else if (userObject instanceof TvShowEpisode episode) {
      return getCheckIcon(episode.getHasNote());
    }
    return null;
  }

}
