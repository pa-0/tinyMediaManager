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
package org.tinymediamanager.core.tvshow.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.LanguageStyle;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tasks.SubtitleDownloadTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.SubtitleSearchAndScrapeOptions;
import org.tinymediamanager.scraper.SubtitleSearchResult;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowSubtitleProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;

/**
 * The class TvShowSubtitleSearchAndDownloadTask is used to search and download subtitles by hash
 * 
 * @author Manuel Laggner
 */
public class TvShowSubtitleSearchAndDownloadTask extends TmmThreadPool {
  private static final Logger       LOGGER = LoggerFactory.getLogger(TvShowSubtitleSearchAndDownloadTask.class);

  private final List<TvShowEpisode> episodes;
  private final List<MediaScraper>  subtitleScrapers;
  private final MediaLanguages      language;

  private boolean                   forceBestMatch;

  public TvShowSubtitleSearchAndDownloadTask(List<TvShowEpisode> episodes, MediaLanguages language) {
    super(TmmResourceBundle.getString("tvshow.download.subtitles"));
    this.episodes = episodes;
    this.language = language;

    // get scrapers
    this.subtitleScrapers = new ArrayList<>(TvShowModuleManager.getInstance().getTvShowList().getDefaultSubtitleScrapers());
  }

  public TvShowSubtitleSearchAndDownloadTask(List<TvShowEpisode> episodes, List<MediaScraper> subtitleScrapers, MediaLanguages language) {
    super(TmmResourceBundle.getString("tvshow.download.subtitles"));
    this.episodes = episodes;
    this.subtitleScrapers = subtitleScrapers;
    this.language = language;
  }

  public void setForceBestMatch(boolean forceBestMatch) {
    this.forceBestMatch = forceBestMatch;
  }

  @Override
  protected void doInBackground() {
    initThreadPool(3, "searchAndDownloadSubtitles");
    start();

    for (TvShowEpisode episode : episodes) {
      submitTask(new Worker(episode));
    }

    waitForCompletionOrCancel();

    LOGGER.info("Done searching and downloading subtitles");
  }

  @Override
  public void callback(Object obj) {
    // do not publish task description here, because with different workers the text is never right
    publishState(progressDone);
  }

  /****************************************************************************************
   * Helper classes
   ****************************************************************************************/
  private class Worker implements Runnable {
    private final TvShowEpisode episode;

    Worker(TvShowEpisode episode) {
      this.episode = episode;
    }

    @Override
    public void run() {
      try {
        for (MediaScraper scraper : subtitleScrapers) {
          if (!scraper.isEnabled()) {
            continue;
          }

          try {
            MediaFile mf = episode.getMediaFiles(MediaFileType.VIDEO).get(0);

            ITvShowSubtitleProvider subtitleProvider = (ITvShowSubtitleProvider) scraper.getMediaProvider();
            SubtitleSearchAndScrapeOptions options = new SubtitleSearchAndScrapeOptions(MediaType.TV_EPISODE);
            options.setMediaFile(mf);
            options.setLanguage(language);
            options.setSeason(episode.getAiredSeason());
            options.setEpisode(episode.getAiredEpisode());

            options.setIds(episode.getIds());

            // get the IMDB id for the TV show if necessary
            Map<String, Object> tvShowIds = new HashMap<>(episode.getTvShow().getIds());
            String tvShowImdbId = episode.getTvShow().getImdbId();
            if (!MediaIdUtil.isValidImdbId(tvShowImdbId) && MediaIdUtil.getIdAsInt(tvShowIds, MediaMetadata.TVDB) > 0) {
              // try to get the IMDB Id via TheTVDB
              tvShowImdbId = MediaIdUtil.getImdbIdFromTvdbId(String.valueOf(MediaIdUtil.getIdAsInt(tvShowIds, MediaMetadata.TVDB)));
            }

            if (MediaIdUtil.isValidImdbId(tvShowImdbId)) {
              tvShowIds.put(MediaMetadata.IMDB, tvShowImdbId);
            }

            options.setId(MediaMetadata.TVSHOW_IDS, tvShowIds);

            List<SubtitleSearchResult> searchResults = subtitleProvider.search(options);
            if (searchResults.isEmpty()) {
              continue;
            }

            Collections.sort(searchResults);
            Collections.reverse(searchResults);

            SubtitleSearchResult result = getBestResult(searchResults);
            if (result == null) {
              // do not continue without a valid result
              continue;
            }

            // the right language tag from the renamer settings
            String lang = LanguageStyle.getLanguageCodeForStyle(language.name(),
                TvShowModuleManager.getInstance().getSettings().getSubtitleLanguageStyle());
            if (StringUtils.isBlank(lang)) {
              lang = language.name();
            }

            String filename = FilenameUtils.getBaseName(mf.getFilename()) + "." + lang;
            TmmTaskManager.getInstance()
                .addDownloadTask(new SubtitleDownloadTask(result.getUrl(), episode.getPathNIO().resolve(filename), episode, lang));
          }
          catch (MissingIdException ignored) {
          }
          catch (ScrapeException e) {
            LOGGER.error("getSubtitles", e);
            MessageManager.instance.pushMessage(
                new Message(MessageLevel.ERROR, episode, "message.scrape.subtitlefailed", new String[] { ":", e.getLocalizedMessage() }));
          }
        }
      }
      catch (Exception e) {
        LOGGER.error("Thread crashed", e);
        MessageManager.instance.pushMessage(
            new Message(MessageLevel.ERROR, "SubtitleDownloader", "message.scrape.threadcrashed", new String[] { ":", e.getLocalizedMessage() }));
      }
    }

    private SubtitleSearchResult getBestResult(List<SubtitleSearchResult> searchResults) {
      if (ListUtils.isEmpty(searchResults)) {
        return null;
      }

      SubtitleSearchResult hashMatch = searchResults.stream().filter(result -> result.getScore() == 1).findFirst().orElse(null);
      // if not forceBestMatch, we take only 100% (hash matched) results
      if (hashMatch != null || !forceBestMatch) {
        return hashMatch;
      }

      // otherwise we try to get the best result
      // 1. filter out all subtitles with a different stack size (we only decide between stacked an non stacked)
      List<SubtitleSearchResult> filteredResults = searchResults.stream().filter(result -> {
        if (episode.isStacked() && result.getStackCount() > 1) {
          return true;
        }
        else if (!episode.isStacked() && result.getStackCount() == 1) {
          return true;
        }
        return false;
      }).toList();

      if (filteredResults.isEmpty()) {
        return null;
      }

      // now compare the release names (if available in the movie)
      // if there is a > 80% match, we take it
      if (StringUtils.isNotBlank(episode.getOriginalFilename())) {
        String basename = FilenameUtils.getBaseName(episode.getOriginalFilename());
        for (SubtitleSearchResult result : filteredResults) {
          float score = MetadataUtil.calculateScore(result.getReleaseName(), basename);
          if (score > 0.8f) {
            return result;
          }
        }
      }

      // last but not least take the first result
      return filteredResults.get(0);
    }
  }
}
