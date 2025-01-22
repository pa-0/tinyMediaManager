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

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaType;

/**
 * The class TvShowEpisodeSearchAndScrapeOptions is used to hold scrape and search related data for episodes
 * 
 * @author Manuel Laggner
 */
public class TvShowEpisodeSearchAndScrapeOptions extends MediaSearchAndScrapeOptions {
  private final Map<String, Object> tvShowIds    = new HashMap<>();

  private MediaEpisodeGroup         episodeGroup = MediaEpisodeGroup.DEFAULT_AIRED;

  /**
   * a minimal constructor for special cases. Do not forget to pass the {@link org.tinymediamanager.core.tvshow.entities.TvShow} ids via
   * setTvShowIds()
   */
  public TvShowEpisodeSearchAndScrapeOptions() {
    super(MediaType.TV_EPISODE);
  }

  /**
   * the _main_ constructor to give all {@link org.tinymediamanager.core.tvshow.entities.TvShow} ids to the options
   * 
   * @param tvShowIds
   *          a {@link Map} with all TV show ids
   */
  public TvShowEpisodeSearchAndScrapeOptions(@NotNull final Map<String, Object> tvShowIds) {
    super(MediaType.TV_EPISODE);
    setTvShowIds(tvShowIds);
  }

  /**
   * copy constructor
   * 
   * @param original
   *          the original to copy
   */
  public TvShowEpisodeSearchAndScrapeOptions(TvShowEpisodeSearchAndScrapeOptions original) {
    super(original);
    setTvShowIds(original.tvShowIds);
    setEpisodeGroup(original.episodeGroup);
  }

  @Override
  public void setDataFromOtherOptions(@NotNull MediaSearchAndScrapeOptions original) {
    super.setDataFromOtherOptions(original);
    if (original instanceof TvShowEpisodeSearchAndScrapeOptions options) {
      setTvShowIds(options.tvShowIds);
      setEpisodeGroup(options.episodeGroup);
    }
    if (original instanceof TvShowSearchAndScrapeOptions) {
      setTvShowIds(original.getIds());
    }
  }

  /**
   * get all set TV show ids
   * 
   * @return a {@link Map} with all set TV show ids
   */
  public Map<String, Object> getTvShowIds() {
    return tvShowIds;
  }

  /**
   * set the TV show ids for this options
   * 
   * @param tvShowIds
   *          a {@link Map} with the TV shows ids to set
   */
  public void setTvShowIds(@NotNull final Map<String, Object> tvShowIds) {
    if (this.tvShowIds == null) { // can happen in the constructor // NOSONAR
      return;
    }

    this.tvShowIds.clear();
    this.tvShowIds.putAll(tvShowIds);
    this.ids.put(MediaMetadata.TVSHOW_IDS, tvShowIds);
  }

  /**
   * get the desired {@link MediaEpisodeGroup}
   * 
   * @return the {@link MediaEpisodeGroup}
   */
  public MediaEpisodeGroup getEpisodeGroup() {
    return episodeGroup;
  }

  /**
   * set the desired {@link MediaEpisodeGroup}
   * 
   * @param episodeGroup
   *          the {@link MediaEpisodeGroup}
   */
  public void setEpisodeGroup(@NotNull MediaEpisodeGroup episodeGroup) {
    this.episodeGroup = episodeGroup;
  }

  /**
   * create an instance of {@link TvShowSearchAndScrapeOptions} from this instance for further usage
   * 
   * @return the created {@link TvShowSearchAndScrapeOptions}
   */
  public TvShowSearchAndScrapeOptions createTvShowSearchAndScrapeOptions() {
    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    // take all data from this
    options.setDataFromOtherOptions(this);
    // but the ids from the show
    options.setIds(tvShowIds);
    options.setEpisodeGroup(episodeGroup);

    return options;
  }

  /**
   * Load default Settings.
   */
  public void loadDefaults() {
    // language
    language = TvShowModuleManager.getInstance().getSettings().getScraperLanguage();

    // metadata
    metadataScraper = TvShowModuleManager.getInstance().getTvShowList().getDefaultMediaScraper();

    // artwork
    artworkScrapers.addAll(TvShowModuleManager.getInstance().getTvShowList().getDefaultArtworkScrapers());

    // trailer
    trailerScrapers.addAll(TvShowModuleManager.getInstance().getTvShowList().getDefaultTrailerScrapers());

    // subtitle
    subtitleScrapers.addAll(TvShowModuleManager.getInstance().getTvShowList().getDefaultSubtitleScrapers());
  }
}
