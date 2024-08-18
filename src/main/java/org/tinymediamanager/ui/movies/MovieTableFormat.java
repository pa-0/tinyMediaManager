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
package org.tinymediamanager.ui.movies;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieComparator;
import org.tinymediamanager.core.movie.MovieEdition;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.renderer.DateTableCellRenderer;
import org.tinymediamanager.ui.renderer.IntegerTableCellRenderer;
import org.tinymediamanager.ui.renderer.RightAlignTableCellRenderer;
import org.tinymediamanager.ui.renderer.RuntimeTableCellRenderer;

/**
 * The MovieTableFormat. Used as definition for the movie table in the movie module
 *
 * @author Manuel Laggner
 */
public class MovieTableFormat extends TmmTableFormat<Movie> {

  private final MovieList movieList;

  public MovieTableFormat() {

    movieList = MovieModuleManager.getInstance().getMovieList();

    Comparator<Movie> movieComparator = new MovieComparator();
    Comparator<Movie> originalTitleComparator = new MovieComparator() {
      @Override
      public int compare(Movie movie1, Movie movie2) {
        if (stringCollator != null) {
          String titleMovie1 = StrgUtils.normalizeString(movie1.getOriginalTitleSortable().toLowerCase(Locale.ROOT));
          String titleMovie2 = StrgUtils.normalizeString(movie2.getOriginalTitleSortable().toLowerCase(Locale.ROOT));
          return stringCollator.compare(titleMovie1, titleMovie2);
        }
        return movie1.getOriginalTitleSortable().toLowerCase(Locale.ROOT).compareTo(movie2.getOriginalTitleSortable().toLowerCase(Locale.ROOT));
      }
    };
    Comparator<String> stringComparator = new StringComparator();
    Comparator<Float> floatComparator = new FloatComparator();
    Comparator<ImageIcon> imageComparator = new ImageComparator();
    Comparator<Date> dateComparator = new DateComparator();
    Comparator<Date> dateTimeComparator = new DateTimeComparator();
    Comparator<String> videoFormatComparator = new VideoFormatComparator();
    Comparator<String> fileSizeComparator = new FileSizeComparator();
    Comparator<Integer> integerComparator = new IntegerComparator();
    Comparator<MediaCertification> certificationComparator = new CertificationComparator();

    FontMetrics fontMetrics = getFontMetrics();

    /*
     * title
     */
    Column col = new Column(TmmResourceBundle.getString("metatag.title"), "title", movie -> movie, Movie.class);
    col.setColumnComparator(movieComparator);
    col.setCellRenderer(new MovieBorderTableCellRenderer());
    col.setCellTooltip(showTooltip(Movie::getTitleSortable));
    addColumn(col);

    /*
     * original title (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.originaltitle"), "originalTitle", movie -> movie, Movie.class);
    col.setColumnComparator(originalTitleComparator);
    col.setCellRenderer(new MovieBorderTableCellRenderer());
    col.setCellTooltip(showTooltip(Movie::getOriginalTitleSortable));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * sorttitle (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.sorttitle"), "sortTitle", Movie::getSortTitle, String.class);
    col.setColumnComparator(stringComparator);
    col.setColumnResizeable(true);
    col.setCellTooltip(showTooltip(Movie::getSortTitle));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * year
     */
    col = new Column(TmmResourceBundle.getString("metatag.year"), "year", MediaEntity::getYear, Integer.class);
    col.setColumnComparator(integerComparator);
    col.setCellRenderer(new IntegerTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("2000") + getCellPadding());
    addColumn(col);

    /*
     * release date (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.releasedate"), "releaseDate", Movie::getReleaseDate, Date.class);
    col.setColumnComparator(dateComparator);
    col.setHeaderIcon(IconManager.DATE_AIRED);
    col.setCellRenderer(new DateTableCellRenderer());
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    try {
      Date date = StrgUtils.parseDate("2012-12-12");
      col.setMinWidth(fontMetrics.stringWidth(TmmDateFormat.MEDIUM_DATE_FORMAT.format(date)) + getCellPadding());
    }
    catch (Exception ignored) {
    }
    addColumn(col);

    /*
     * file name (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.filename"), "filename", movie -> movie.getMainVideoFile().getFilename(), String.class);
    col.setColumnComparator(stringComparator);
    col.setColumnResizeable(true);
    col.setCellTooltip(showTooltip(movie -> movie.getMainVideoFile().getFilename()));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * folder name (hidden per default)
     */
    Function<Movie, String> pathFunction = movie -> movie.getPathNIO().toString();
    col = new Column(TmmResourceBundle.getString("metatag.path"), "path", pathFunction, String.class);
    col.setColumnComparator(stringComparator);
    col.setColumnResizeable(true);
    col.setCellTooltip(showTooltip(pathFunction));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * movie set (hidden per default)
     */
    Function<Movie, String> movieSetFunction = movie -> movie.getMovieSet() == null ? null : movie.getMovieSet().getTitleSortable();
    col = new Column(TmmResourceBundle.getString("metatag.movieset"), "movieset", movieSetFunction, String.class);
    col.setColumnComparator(stringComparator);
    col.setColumnResizeable(true);
    col.setCellTooltip(showTooltip(movieSetFunction));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.rating"), "rating", movie -> getRating(movie.getRating()), Float.class);
    col.setColumnComparator(floatComparator);
    col.setHeaderIcon(IconManager.RATING);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("99.9") + getCellPadding());
    col.setCellTooltip(showTooltip(
        movie -> movie.getRating().getRating() + " (" + movie.getRating().getVotes() + " " + TmmResourceBundle.getString("metatag.votes") + ")"));
    addColumn(col);

    /*
     * votes (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.votes"), "votes",
        movie -> movie.getRating() == MediaMetadata.EMPTY_RATING ? null : movie.getRating().getVotes(), Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderIcon(IconManager.VOTES);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("1000000") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * user rating (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.userrating"), "userrating", movie -> getRating(movie.getUserRating()), Float.class);
    col.setColumnComparator(floatComparator);
    col.setHeaderIcon(IconManager.USER_RATING);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("10.0") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * imdb rating (hidden per default)
     */
    col = new Column("IMDb", "imdb", movie -> getRating(movie.getRating(MediaMetadata.IMDB)), Float.class);
    col.setColumnComparator(floatComparator);
    col.setHeaderTooltip(TmmResourceBundle.getString("metatag.rating") + " - IMDb");
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("IMDb") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * rotten tomatoes rating (hidden per default)
     */
    col = new Column("RT", "rottenTomatoes", movie -> getRatingInteger(movie.getRating("tomatometerallcritics")), Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderTooltip(TmmResourceBundle.getString("metatag.rating") + " - Rotten Tomatoes");
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("100") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * metascore rating (hidden per default)
     */
    col = new Column("MS", "metacritic", movie -> getRatingInteger(movie.getRating(MediaMetadata.METACRITIC)), Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderTooltip(TmmResourceBundle.getString("metatag.rating") + " - Metascore");
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("100") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * tmdb rating (hidden per default)
     */
    col = new Column("TMDB", "tmdb", movie -> getRating(movie.getRating(MediaMetadata.TMDB)), Float.class);
    col.setColumnComparator(floatComparator);
    col.setHeaderTooltip(TmmResourceBundle.getString("metatag.rating") + " - TMDB");
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("TMDB") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * top 250 (hidden per default)
     */
    col = new Column("T250", "top250", movie -> {
      if (movie.getTop250() == 0) {
        return null;
      }
      return movie.getTop250();
    }, Integer.class);
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
    col = new Column(TmmResourceBundle.getString("metatag.certification"), "certification", Movie::getCertification, MediaCertification.class);
    col.setColumnComparator(certificationComparator);
    col.setHeaderIcon(IconManager.CERTIFICATION);
    col.setColumnResizeable(true);
    col.setMinWidth(fontMetrics.stringWidth("not rated") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * date added (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.dateadded"), "dateAdded", MediaEntity::getDateAddedForUi, Date.class);
    col.setColumnComparator(dateTimeComparator);
    col.setHeaderIcon(IconManager.DATE_ADDED);
    col.setCellRenderer(new DateTableCellRenderer());
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    try {
      Date date = StrgUtils.parseDate("2012-12-12");
      col.setMinWidth(fontMetrics.stringWidth(TmmDateFormat.MEDIUM_DATE_FORMAT.format(date)) + getCellPadding());
    }
    catch (Exception ignored) {
    }
    addColumn(col);

    /*
     * file creation date (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.filecreationdate"), "fileCreationDate", movie -> movie.getMainVideoFile().getDateCreated(),
        Date.class);
    col.setColumnComparator(dateTimeComparator);
    col.setHeaderIcon(IconManager.DATE_CREATED);
    col.setCellRenderer(new DateTableCellRenderer());
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    try {
      Date date = StrgUtils.parseDate("2012-12-12");
      col.setMinWidth(fontMetrics.stringWidth(TmmDateFormat.MEDIUM_DATE_FORMAT.format(date)) + getCellPadding());
    }
    catch (Exception ignored) {
    }
    addColumn(col);

    /*
     * runtime (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.runtime") + " [min]", "runtime", Movie::getRuntime, Integer.class);
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
    col = new Column(TmmResourceBundle.getString("metatag.runtime") + " [hh:mm]", "runtime2", Movie::getRuntime, Integer.class);
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
    col = new Column(TmmResourceBundle.getString("metatag.format"), "videoFormat", Movie::getMediaInfoVideoFormat, String.class);
    col.setColumnComparator(videoFormatComparator);
    col.setHeaderIcon(IconManager.VIDEO_FORMAT);
    col.setColumnResizeable(false);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setMinWidth(fontMetrics.stringWidth("1080p") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * aspect ratio (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.aspectratio"), "aspectRatio", Movie::getMediaInfoAspectRatio, Float.class);
    col.setColumnComparator(floatComparator);
    col.setHeaderIcon(IconManager.ASPECT_RATIO);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("1.78") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * 2nd aspect ratio (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.aspectratio2"), "aspectRatio2", Movie::getMediaInfoAspectRatio2, Float.class);
    col.setColumnComparator(floatComparator);
    col.setHeaderIcon(IconManager.ASPECT_RATIO_2);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("1.78") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * HDR (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.hdr"), "hdr", movie -> getCheckIcon(movie.isVideoInHDR()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.HDR);
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * HDR format (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.hdrformat"), "hdrFormat", Movie::getVideoHDRFormat, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.HDR);
    col.setMinWidth(fontMetrics.stringWidth("HDR10") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * video codec (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videocodec"), "videoCodec", Movie::getMediaInfoVideoCodec, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.VIDEO_CODEC);
    col.setMinWidth(fontMetrics.stringWidth("MPEG-2") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * video bitrate (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videobitrate"), "videoBitrate", Movie::getMediaInfoVideoBitrate, Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderIcon(IconManager.VIDEO_BITRATE);
    col.setCellRenderer(new IntegerTableCellRenderer());
    col.setMinWidth(fontMetrics.stringWidth("20000") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * audio codec and channels(hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.audio"), "audio", movie -> {
      List<MediaFile> videos = movie.getMediaFiles(MediaFileType.VIDEO);
      if (!videos.isEmpty()) {
        MediaFile mediaFile = videos.get(0);
        if (StringUtils.isNotBlank(mediaFile.getAudioCodec())) {
          return mediaFile.getAudioCodec() + " " + mediaFile.getAudioChannels();
        }
      }
      return "";
    }, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.AUDIO);
    col.setMinWidth(fontMetrics.stringWidth("DTS 7ch") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * main video file size (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videofilesize"), "fileSize",
        movie -> Utils.formatFileSizeForDisplay(movie.getVideoFilesize()), String.class);
    col.setColumnComparator(fileSizeComparator);
    col.setHeaderIcon(IconManager.FILE_SIZE);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("500.00 M") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * total file size (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.totalfilesize"), "totalFileSize",
        movie -> Utils.formatFileSizeForDisplay(movie.getTotalFilesize()), String.class);
    col.setColumnComparator(fileSizeComparator);
    col.setHeaderIcon(IconManager.FILE_SIZE);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("500.00 M") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * Edition (hidden per default)
     */
    Function<Movie, String> movieEditionFunction = movie -> movie.getEdition() == null || movie.getEdition() == MovieEdition.NONE ? null
        : movie.getEdition().toString();
    col = new Column(TmmResourceBundle.getString("metatag.edition"), "edition", movieEditionFunction, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.EDITION);
    col.setCellTooltip(showTooltip(movieEditionFunction));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * Source (hidden per default)
     */
    Function<Movie, String> mediaSourceFunction = movie -> movie.getMediaSource() == null ? null : movie.getMediaSource().toString();
    col = new Column(TmmResourceBundle.getString("metatag.source"), "mediaSource", mediaSourceFunction, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.SOURCE);
    col.setCellTooltip(showTooltip(mediaSourceFunction));
    col.setMinWidth(fontMetrics.stringWidth("Blu-ray") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * new indicator
     */
    col = new Column(TmmResourceBundle.getString("movieextendedsearch.newmovies"), "new", movie -> getNewIcon(movie.isNewlyAdded()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.NEW);
    col.setColumnResizeable(false);
    addColumn(col);

    /*
     * 3D (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.3d"), "video3d", movie -> getCheckIcon(movie.isVideoIn3D()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.VIDEO_3D);
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * Metadata
     */
    Function<Movie, String> nfoFunction = movie -> {
      List<MovieScraperMetadataConfig> values = new ArrayList<>();

      if (MovieModuleManager.getInstance().getSettings().isMovieDisplayAllMissingMetadata()) {
        for (MovieScraperMetadataConfig config : MovieScraperMetadataConfig.values()) {
          if (config.isMetaData() || config.isCast()) {
            values.add(config);
          }
        }
      }
      else {
        values.addAll(MovieModuleManager.getInstance().getSettings().getMovieCheckMetadata());
      }
      List<MovieScraperMetadataConfig> missingMetadata = movieList.detectMissingFields(movie, values);

      if (!missingMetadata.isEmpty()) {
        StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");

        for (MovieScraperMetadataConfig metadataConfig : missingMetadata) {
          missing.append("\n").append(metadataConfig.getDescription());
        }

        return missing.toString();
      }

      return null;
    };
    col = new Column(TmmResourceBundle.getString("tmm.metadata"), "metadata", movie -> getCheckIcon(movieList.detectMissingMetadata(movie).isEmpty()),
        ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.NFO);
    col.setColumnResizeable(false);
    col.setCellTooltip(showTooltip(nfoFunction));
    addColumn(col);

    /*
     * images
     */
    Function<Movie, String> imageFunction = movie -> {
      List<MovieScraperMetadataConfig> values = new ArrayList<>();
      if (MovieModuleManager.getInstance().getSettings().isMovieDisplayAllMissingArtwork()) {
        for (MovieScraperMetadataConfig config : MovieScraperMetadataConfig.values()) {
          if (config.isArtwork()) {
            values.add(config);
          }
        }
      }
      else {
        values.addAll(MovieModuleManager.getInstance().getSettings().getMovieCheckArtwork());
      }
      List<MovieScraperMetadataConfig> missingMetadata = movieList.detectMissingFields(movie, values);

      if (!missingMetadata.isEmpty()) {
        StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");

        for (MovieScraperMetadataConfig metadataConfig : missingMetadata) {
          missing.append("\n").append(metadataConfig.getDescription());
        }

        return missing.toString();
      }

      return null;
    };
    col = new Column(TmmResourceBundle.getString("tmm.images"), "images", movie -> getCheckIcon(movieList.detectMissingArtwork(movie).isEmpty()),
        ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.IMAGES);
    col.setColumnResizeable(false);
    col.setCellTooltip(showTooltip(imageFunction));
    addColumn(col);

    /*
     * trailer
     */
    col = new Column(TmmResourceBundle.getString("tmm.trailer"), "trailer", movie -> getCheckIcon(movie.getHasTrailer()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.TRAILER);
    col.setColumnResizeable(false);
    addColumn(col);

    /*
     * subtitles
     */
    col = new Column(TmmResourceBundle.getString("tmm.subtitles"), "subtitles", movie -> getCheckIcon(movie.getHasSubtitles()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.SUBTITLES);
    col.setColumnResizeable(false);
    addColumn(col);

    /*
     * watched
     */
    col = new Column(TmmResourceBundle.getString("metatag.watched"), "watched", movie -> getCheckIcon(movie.isWatched()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.WATCHED);
    col.setColumnResizeable(false);
    addColumn(col);

    /*
     * Note (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.note"), "note", movie -> getCheckIcon(movie.getHasNote()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.INFO);
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    addColumn(col);
  }

  private Float getRating(MediaRating rating) {
    if (rating == MediaMetadata.EMPTY_RATING) {
      return null;
    }
    return rating.getRating();
  }

  private Integer getRatingInteger(MediaRating rating) {
    if (rating == MediaMetadata.EMPTY_RATING) {
      return null;
    }
    return Math.round(rating.getRating());
  }

  private <E> Function<E, String> showTooltip(Function<E, String> tooltipFunction) {
    return movie -> {
      if (MovieModuleManager.getInstance().getSettings().isShowMovieTableTooltips()) {
        return tooltipFunction.apply(movie);
      }
      else {
        return null;
      }
    };
  }
}
