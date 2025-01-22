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
package org.tinymediamanager.scraper.anidb;

import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroupType.AIRED;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.util.Similarity;

/**
 * The elements for AniDB's TV Show Metadata Provider. The majority of the work is done in {@link AniDbMetadataParser}.
 *
 * @see <a href="https://anidb.net/">https://anidb.net/</a>
 * @see <a href="https://wiki.anidb.net/API">https://wiki.anidb.net/API</a>
 * @see AniDbMetadataParser
 * @see AniDbMetadataProvider
 * @author Manuel Laggner
 */
public class AniDbTvShowMetadataProvider extends AniDbMetadataProvider implements ITvShowMetadataProvider, ITvShowArtworkProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(AniDbTvShowMetadataProvider.class);

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, "tvshow", "aniDB", "<html><h3>aniDB</h3><br />AniDB stands for Anime DataBase. "
        + "AniDB is a non-profit anime database that is open " + "freely to the public.</html>",
        AniDbTvShowMetadataProvider.class.getResource("/org/tinymediamanager/scraper/anidb_net.png"), -10);

    // configure/load settings
    info.getConfig().addInteger("numberOfTags", 10);
    info.getConfig().addInteger("minimumTagsWeight", 200);
    info.getConfig().addBoolean("characterImage", false);
    info.getConfig().load();

    return info;
  }

  @Override
  public MediaMetadata getMetadata(@NotNull TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    MediaMetadata md = null;

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    if (seasonNr == -1 || episodeNr == -1) {
      throw new MissingIdException(MediaMetadata.SEASON_NR, MediaMetadata.EPISODE_NR);
    }

    // get full episode listing
    List<MediaMetadata> episodes = getEpisodeList(options.createTvShowSearchAndScrapeOptions());

    // filter out the wanted episode
    for (MediaMetadata episode : episodes) {
      MediaEpisodeNumber episodeNumber = episode.getEpisodeNumber(AIRED);
      if (episodeNumber == null) {
        continue;
      }

      if (episodeNumber.episode() == episodeNr && episodeNumber.season() == seasonNr) {
        md = episode;
        break;
      }
    }

    if (md == null) {
      throw new NothingFoundException();
    }

    return md;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    List<MediaMetadata> episodes = new ArrayList<>();
    String language = options.getLanguage().getLanguage();

    // do we have an id from the options?
    String id = options.getIdAsString(providerInfo.getId());

    if (StringUtils.isEmpty(id)) {
      throw new MissingIdException(providerInfo.getId());
    }

    Document doc = requestAnimeDocument(options);

    if (doc == null || doc.children().isEmpty()) {
      return episodes;
    }

    // filter out the episode
    for (AniDbEpisode ep : AniDbMetadataParser.parseEpisodes(doc.getElementsByTag("episodes").first())) {
      MediaMetadata md = new MediaMetadata(providerInfo.getId());
      md.setScrapeOptions(options);
      md.setTitle(ep.titles.get(language));
      // basically only absolute order is supported by anidb. we add aired as backwards compatibility
      md.setEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, ep.season, ep.episode);
      md.setEpisodeNumber(MediaEpisodeGroup.DEFAULT_ABSOLUTE, ep.season, ep.episode);

      if (StringUtils.isBlank(md.getTitle())) {
        md.setTitle(ep.titles.get("en"));
      }
      if (StringUtils.isBlank(md.getTitle())) {
        md.setTitle(ep.titles.get("x-jat"));
      }

      md.setPlot(ep.summary);

      if (ep.rating > 0) {
        MediaRating rating = new MediaRating(providerInfo.getId());
        rating.setRating(ep.rating);
        rating.setVotes(ep.votes);
        rating.setMaxValue(10);
        md.addRating(rating);
      }
      md.setRuntime(ep.runtime);
      md.setReleaseDate(ep.airdate);
      md.setId(providerInfo.getId(), ep.id);
      episodes.add(md);
    }

    return episodes;
  }

  @Override
  public SortedSet<MediaSearchResult> search(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    synchronized (AniDbTvShowMetadataProvider.class) {
      // first run: build up the anime name list
      if (showsForLookup.isEmpty()) {
        buildTitleHashMap();
      }
    }

    // detect the string to search
    String searchString = "";
    if (StringUtils.isNotEmpty(options.getSearchQuery())) {
      searchString = options.getSearchQuery();
    }

    // return an empty search result if no query provided
    if (StringUtils.isEmpty(searchString)) {
      return new TreeSet<>();
    }

    String finalSearchString = searchString;

    return showsForLookup.entrySet()
        .stream()
        .flatMap(entry -> entry.getValue().stream())
        .map(show -> new MediaSearchResult.Builder(MediaType.TV_SHOW).providerId(providerInfo.getId())
            .id(String.valueOf(show.aniDbId))
            .title(show.title)
            .score(Similarity.compareStrings(show.title, finalSearchString))
            .build())
        .filter(msr -> msr.getScore() >= 0.75f) // use default
        .collect(Collectors.toCollection(TreeSet::new));
  }

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    List<MediaArtwork> artwork = new ArrayList<>();
    String id = "";

    // check if there is a metadata containing an id
    if (options.getMetadata() != null && getId().equals(options.getMetadata().getProviderId())) {
      id = options.getMetadata().getIdAsString(providerInfo.getId());
    }

    // get the id from the options
    if (StringUtils.isEmpty(id)) {
      id = options.getIdAsString(providerInfo.getId());
    }

    if (StringUtils.isEmpty(id)) {
      throw new MissingIdException(providerInfo.getId());
    }

    switch (options.getArtworkType()) {
      // AniDB only offers Poster
      case ALL, POSTER -> {
        MediaMetadata md;
        try {
          TvShowSearchAndScrapeOptions tvShowSearchAndScrapeOptions = new TvShowSearchAndScrapeOptions();
          tvShowSearchAndScrapeOptions.setDataFromOtherOptions(options);
          md = getMetadata(tvShowSearchAndScrapeOptions);
        }
        catch (Exception e) {
          LOGGER.error("could not get artwork: {}", e.getMessage());
          throw new ScrapeException(e);
        }
        artwork.addAll(md.getMediaArt(MediaArtworkType.POSTER));
      }
      default -> {
        return artwork;
      }
    }

    return artwork;
  }

  @Override
  public MediaMetadata getMetadata(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    Document doc = requestAnimeDocument(options);
    if (doc == null || doc.children().isEmpty()) {
      return null;
    }

    MediaMetadata md = new MediaMetadata(providerInfo.getId());
    md.setScrapeOptions(options);
    String language = options.getLanguage().getLanguage();
    String id = options.getIdAsString(providerInfo.getId());
    md.setId(providerInfo.getId(), id);
    md.addEpisodeGroup(MediaEpisodeGroup.DEFAULT_AIRED);
    md.addEpisodeGroup(MediaEpisodeGroup.DEFAULT_ABSOLUTE);

    AniDbMetadataParser.fillAnimeMetadata(md, language, doc.child(0), providerInfo);

    return md;
  }
}
