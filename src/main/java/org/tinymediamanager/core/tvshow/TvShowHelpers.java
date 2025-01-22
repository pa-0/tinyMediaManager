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

package org.tinymediamanager.core.tvshow;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.tasks.TrailerDownloadTask;
import org.tinymediamanager.core.tasks.YTDownloadTask;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskChain;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.filenaming.TvShowTrailerNaming;
import org.tinymediamanager.core.tvshow.tasks.TvShowTrailerDownloadTask;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.imdb.ImdbTvShowTrailerProvider;
import org.tinymediamanager.scraper.util.MetadataUtil;

/**
 * a collection of various helpers for the TV show module
 *
 * @author Manuel Laggner
 */
public class TvShowHelpers {
  private static final Logger  LOGGER               = LoggerFactory.getLogger(TvShowHelpers.class);

  private static final Pattern SEASON_NUMBER        = Pattern.compile("(?i)season\\s?(\\d+).*");
  private static final Pattern SEASON_FOLDER_NUMBER = Pattern.compile("(?i).*?(\\d+).*");

  private TvShowHelpers() {
    throw new IllegalAccessError();
  }

  /**
   * Parses a given certification string for the localized country setup in setting.
   *
   * @param name
   *          certification string like "USA:R / UK:15 / Sweden:15"
   * @return the localized certification if found, else *ANY* language cert found
   */
  // <certification>USA:R / UK:15 / Sweden:15 / Spain:18 / South Korea:15 /
  // Singapore:NC-16 / Portugal:M/16 / Philippines:R-18 / Norway:15 / New
  // Zealand:M / Netherlands:16 / Malaysia:U / Malaysia:18PL / Ireland:18 /
  // Iceland:16 / Hungary:18 / Germany:16 / Finland:K-15 / Canada:18A /
  // Canada:18+ / Brazil:16 / Australia:M / Argentina:16</certification>
  public static MediaCertification parseCertificationStringForTvShowSetupCountry(String name) {
    MediaCertification cert = MediaCertification.UNKNOWN;
    name = name.strip();
    if (name.contains("/")) {
      // multiple countries
      String[] countries = name.split("/");
      // first try to find by setup CertLanguage
      for (String c : countries) {
        c = c.strip();
        if (c.contains(":")) {
          String[] cs = c.split(":");
          cert = MediaCertification.getCertification(TvShowModuleManager.getInstance().getSettings().getCertificationCountry(), cs[1]);
          if (cert != MediaCertification.UNKNOWN) {
            return cert;
          }
        }
        else {
          cert = MediaCertification.getCertification(TvShowModuleManager.getInstance().getSettings().getCertificationCountry(), c);
          if (cert != MediaCertification.UNKNOWN) {
            return cert;
          }
        }
      }
      // still not found localized cert? parse the name to find *ANY*
      // certificate
      for (String c : countries) {
        c = c.strip();
        if (c.contains(":")) {
          String[] cs = c.split(":");
          cert = MediaCertification.findCertification(cs[1]);
          if (cert != MediaCertification.UNKNOWN) {
            return cert;
          }
        }
        else {
          cert = MediaCertification.findCertification(c);
          if (cert != MediaCertification.UNKNOWN) {
            return cert;
          }
        }
      }
    }
    else {
      // no slash, so only one country
      if (name.contains(":")) {
        String[] cs = name.split(":");
        cert = MediaCertification.getCertification(TvShowModuleManager.getInstance().getSettings().getCertificationCountry(), cs[1].strip());
        if (cert == MediaCertification.UNKNOWN) {
          cert = MediaCertification.findCertification(cs[1].strip());
        }
      }
      else {
        // no country? try to find only by name
        cert = MediaCertification.getCertification(TvShowModuleManager.getInstance().getSettings().getCertificationCountry(), name.strip());
      }
    }
    // still not found localized cert? parse the name to find *ANY* certificate
    if (cert == MediaCertification.UNKNOWN) {
      cert = MediaCertification.findCertification(name);
    }
    return cert;
  }

  /**
   * try to detect the TV show folder by comparing the paths of the media files
   *
   * @param tvShow
   *          the TV show to analyze
   * @param season
   *          the season for what we would like to have the season folder
   * @return the path to the season folder relative to the TV show folder or the default season folder name from the renamer settings
   */
  public static String detectSeasonFolder(TvShow tvShow, int season) {
    // there is no dedicated folder for season -1
    if (season == -1) {
      return "";
    }

    List<String> subPaths = new ArrayList<>();

    Path tvShowPath = tvShow.getPathNIO();
    List<TvShowEpisode> episodes = tvShow.getEpisodesForSeason(season);

    try {
      // compare all episodes for the given season
      for (TvShowEpisode episode : episodes) {
        Path videoFilePath = episode.getMainVideoFile().getFileAsPath().getParent();

        if (!videoFilePath.startsWith(tvShowPath)) {
          // inconsistent MF path!!
          LOGGER.debug("episode MF path does not match TV show root!!");
          continue;
        }

        // split up the relative path into its path junks
        Path relativePath = tvShowPath.relativize(videoFilePath);
        int subfolders = relativePath.getNameCount();

        for (int i = 1; i <= subfolders; i++) {
          subPaths.add(relativePath.subpath(0, i).toString());
        }
      }
    }
    catch (Exception e) {
      LOGGER.debug("could not extract season folder: {}", e.getMessage());
    }

    if (subPaths.isEmpty()) {
      return "";
    }

    // group them
    Map<String, Long> subPathCounts = subPaths.stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()));

    // take the highest count
    Map.Entry<String, Long> entry = subPathCounts.entrySet().stream().max(Map.Entry.comparingByValue()).get(); // NOSONAR

    // if there are at least 80% of all episodes having this subfolder, take it
    if (entry.getValue() >= 0.8 * episodes.size()) {
      return entry.getKey();
    }

    // no common folder found -> force to write it to the TV show root
    return "";
  }

  /**
   * start the automatic trailer download for the given movie
   *
   * @param tvShow
   *          the TV show to start the trailer download for
   */
  public static void startAutomaticTrailerDownload(TvShow tvShow) {
    // start movie trailer download?
    if (TvShowModuleManager.getInstance().getSettings().isUseTrailerPreference()
        && TvShowModuleManager.getInstance().getSettings().isAutomaticTrailerDownload() && tvShow.getMediaFiles(MediaFileType.TRAILER).isEmpty()
        && !tvShow.getTrailer().isEmpty()) {
      downloadBestTrailer(tvShow);
    }
  }

  /**
   * download the best trailer for the given TV show
   *
   * @param tvShow
   *          the TV show to download the trailer for
   */
  public static void downloadBestTrailer(TvShow tvShow) {
    if (!tvShow.getTrailer().isEmpty()) {
      TmmTask task = new TvShowTrailerDownloadTask(tvShow);
      TmmTaskChain.getInstance(tvShow).add(task);
    }
  }

  /**
   * download the given trailer for the given TV show
   *
   * @param tvshow
   *          the TV show to download the trailer for
   * @param trailer
   *          the trailer to download
   */
  public static void downloadTrailer(TvShow tvshow, MediaTrailer trailer) {
    if (StringUtils.isBlank(trailer.getUrl()) || !trailer.getUrl().startsWith("http")) {
      if (StringUtils.isBlank(trailer.getId())) {
        LOGGER.debug("Could not download trailer: no url/id {}", trailer);
        return;
      }
      // we have an ID - lets check if it is a known one:
      String id = trailer.getId();
      if (!id.matches("vi\\d+")) { // IMDB
        LOGGER.debug("Could not download trailer: id not known {}", trailer);
        return;
      }

      // IMD trailer ID
      ImdbTvShowTrailerProvider tp = new ImdbTvShowTrailerProvider();
      String url = tp.getUrlForId(trailer);
      if (url.isEmpty()) {
        LOGGER.debug("Could not download trailer: could not construct url from id {}", trailer);
        return;
      }
      trailer.setUrl(url);
    }

    // get the right file name
    List<TvShowTrailerNaming> trailernames = TvShowModuleManager.getInstance().getSettings().getTrailerFilenames();

    // hmm.. at the moment we can only download ONE trailer, so both patterns won't work
    // just take the first one (or the default if there is no entry whyever)
    String filename;
    if (!trailernames.isEmpty()) {
      filename = tvshow.getTrailerFilename(trailernames.get(0));
    }
    else {
      filename = tvshow.getTrailerFilename(TvShowTrailerNaming.TVSHOW_TRAILER);
    }

    try {
      Matcher matcher = Utils.YOUTUBE_PATTERN.matcher(trailer.getUrl());
      if (matcher.matches()) {
        YTDownloadTask task = new YTDownloadTask(trailer, TvShowModuleManager.getInstance().getSettings().getTrailerQuality(),
            TvShowModuleManager.getInstance().getSettings().isUseYtDlp()) {
          @Override
          protected Path getDestinationWoExtension() {
            return tvshow.getPathNIO().resolve(filename);
          }

          @Override
          protected MediaEntity getMediaEntityToAdd() {
            return tvshow;
          }
        };
        TmmTaskChain.getInstance(tvshow).add(task);
      }
      else {
        TrailerDownloadTask task = new TrailerDownloadTask(trailer) {

          @Override
          protected Path getDestinationWoExtension() {
            return tvshow.getPathNIO().resolve(filename);
          }

          @Override
          protected MediaEntity getMediaEntityToAdd() {
            return tvshow;
          }
        };
        TmmTaskChain.getInstance(tvshow).add(task);
      }
    }
    catch (Exception e) {
      LOGGER.error("could not start trailer download: {}", e.getMessage());
      MessageManager.instance.pushMessage(
          new Message(Message.MessageLevel.ERROR, tvshow, "message.scrape.trailerfailed", new String[] { ":", e.getLocalizedMessage() }));
    }
  }

  /**
   * detect the season number of season-xxx named files
   *
   * @param filename
   *          the filename
   * @param foldername
   *          the folder name
   * @return the season number OR Integer.MIN_VALUE if not detectable
   */
  public static int detectSeasonFromFileAndFolder(String filename, String foldername) {
    int season = Integer.MIN_VALUE;

    if (filename.startsWith("season-specials") || foldername.equalsIgnoreCase("specials")) {
      season = 0;
    }
    else if (filename.startsWith("season-all")) {
      season = -1;
    }
    else {
      // parse out the season from the name
      Matcher matcher = SEASON_NUMBER.matcher(filename);
      if (matcher.matches()) {
        season = Integer.parseInt(matcher.group(1));
      }

      // try to parse out the season from the parent
      if (season == Integer.MIN_VALUE) {
        matcher = SEASON_NUMBER.matcher(foldername);
        if (matcher.matches()) {
          season = Integer.parseInt(matcher.group(1));
        }
      }
      if (season == Integer.MIN_VALUE) {
        matcher = SEASON_FOLDER_NUMBER.matcher(foldername);
        if (matcher.matches()) {
          season = Integer.parseInt(matcher.group(1));
        }
      }
    }

    // mitigation for corrupt ones
    if (season == Integer.MAX_VALUE) {
      season = Integer.MIN_VALUE;
    }

    return season;
  }

  /**
   * attempt to find the best matching episode group for the given {@link TvShow}
   *
   * @param tvShow
   *          the {@link TvShow} to find the match for
   * @param episodeGroups
   *          a {@link List} with all available {@link MediaEpisodeGroup}s
   * @param episodeList
   *          the episode list from the scraper
   * @return the best matching {@link MediaEpisodeGroup} or null (if nothing matches)
   */
  public static MediaEpisodeGroup findBestMatchingEpisodeGroup(TvShow tvShow, List<MediaEpisodeGroup> episodeGroups,
      List<MediaMetadata> episodeList) {

    // just remember the episode group with the best score and most matched episodes
    float matchedEpisodes = 0;
    float score = 0;
    MediaEpisodeGroup episodeGroup = null;

    for (MediaEpisodeGroup eg : episodeGroups) {
      int episodeCount = 0;
      float scoreSum = 0;

      // try to find the best matching episode in the given episode group
      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        for (MediaMetadata md : episodeList) {
          MediaEpisodeNumber episodeNumber = md.getEpisodeNumber(eg);
          if (episodeNumber == null) {
            continue;
          }

          if (episode.getSeason() == episodeNumber.season() && episode.getEpisode() == episodeNumber.episode()) {
            float titleScore = MetadataUtil.calculateScore(md.getTitle(), episode.getTitle());

            String cleanedFilename = TvShowEpisodeAndSeasonParser.cleanEpisodeTitle(episode.getMainVideoFile().getBasename(),
                episode.getTvShow().getTitle());
            float filenameScore = MetadataUtil.calculateScore(md.getTitle(), cleanedFilename);

            scoreSum += Math.max(titleScore, filenameScore);

            episodeCount++;
            break;
          }
        }
      }

      if (episodeCount > 0) {
        if (episodeCount > matchedEpisodes || (episodeCount == matchedEpisodes && score < scoreSum / episodeCount)) {
          episodeGroup = eg;
          score = scoreSum / episodeCount;
          matchedEpisodes = episodeCount;
        }
      }
    }

    return episodeGroup;
  }

  /**
   * should we add this dummy episode to the {@link TvShow}?
   * 
   * @param episode
   *          the dummy episode to add
   * @return true/false
   */
  public static boolean shouldAddDummyEpisode(TvShowEpisode episode) {
    if (episode.getEpisode() == -1 || episode.getSeason() == -1) {
      return false;
    }

    TvShowEpisodeType episodeType = TvShowEpisodeType.getTypeForEpisode(episode);

    return switch (episodeType) {
      case DUMMY_NORMAL -> TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes();
      case DUMMY_SPECIAL -> TvShowModuleManager.getInstance().getSettings().isDisplayMissingSpecials();
      case DUMMY_NOT_AIRED -> TvShowModuleManager.getInstance().getSettings().isDisplayMissingNotAired();
      default -> false;
    };
  }

  /**
   * Mix in the dummy episodes according to the settings - mainly for displaying, but also used in other areas
   * 
   * @param existingEpisodes
   *          all existing {@link TvShowEpisode}s
   * @param dummyEpisodes
   *          all available dummy {@link TvShowEpisode}s
   * @return a {@link List} containing all existing and all to mix in dummies
   */
  public static List<TvShowEpisode> getEpisodesForDisplay(List<TvShowEpisode> existingEpisodes, List<TvShowEpisode> dummyEpisodes) {
    List<TvShowEpisode> episodes = new ArrayList<>(existingEpisodes);

    // mix in unavailable episodes if the user wants to
    if (TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()) {
      // build up a set which holds a string representing the S/E indicator
      Set<String> availableEpisodes = new HashSet<>();

      for (TvShowEpisode episode : episodes) {
        if (episode.getSeason() > -1 && episode.getEpisode() > -1) {
          availableEpisodes.add("A" + episode.getSeason() + "." + episode.getEpisode());
        }
      }

      // and now mix in unavailable ones
      for (TvShowEpisode episode : dummyEpisodes) {
        if (!TvShowHelpers.shouldAddDummyEpisode(episode)) {
          continue;
        }

        if (!availableEpisodes.contains("A" + episode.getSeason() + "." + episode.getEpisode())) {
          episodes.add(episode);
        }
      }
    }

    episodes.sort(Comparator.comparingInt(TvShowEpisode::getEpisode));
    return episodes;
  }
}
