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
package org.tinymediamanager.scraper.universal_movie;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.config.MediaProviderConfig;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieTmdbMetadataProvider;
import org.tinymediamanager.scraper.util.MediaIdUtil;

import com.google.gson.Gson;

/**
 * This is a metadata provider which is highly configurable and combines the results of various other providers
 *
 * @author Manuel Laggner
 */
public class UniversalMovieMetadataProvider implements IMovieMetadataProvider {
  public static final String                               ID                  = "universal_movie";

  private static final String                              UNDEFINED           = "-";
  private static final String                              SEARCH              = "search";
  private static final String                              RATINGS             = "ratings";
  private static final String                              FALLBACK_SCRAPERS   = "fallbackScrapers";
  private static final Logger                              LOGGER              = LoggerFactory.getLogger(UniversalMovieMetadataProvider.class);
  private static final Map<String, IMovieMetadataProvider> COMPATIBLE_SCRAPERS = new LinkedHashMap<>();
  private static final ExecutorService                     EXECUTOR            = new ThreadPoolExecutor(4, 8, 5, TimeUnit.SECONDS,
      new LinkedBlockingQueue<>());

  private final MediaProviderInfo                          providerInfo;

  public UniversalMovieMetadataProvider() {
    providerInfo = createMediaProviderInfo();
    init();
  }

  private MediaProviderInfo createMediaProviderInfo() {
    return new MediaProviderInfo(ID, ID, "Universal movie scraper",
        "<html><h3>Universal movie scraper</h3><br />A meta scraper which allows to collect data from several other scrapers</html>",
        UniversalMovieMetadataProvider.class.getResource("/org/tinymediamanager/scraper/tmm_logo.svg"));
  }

  @Override
  public boolean isActive() {
    return isFeatureEnabled();
  }

  public static void addProvider(IMovieMetadataProvider provider) {
    // called for each plugin implementing that interface
    if (!provider.getId().equals(ID) && !COMPATIBLE_SCRAPERS.containsKey(provider.getId())
        && (provider instanceof IMovieTmdbMetadataProvider || provider instanceof IMovieImdbMetadataProvider)) {
      COMPATIBLE_SCRAPERS.put(provider.getId(), provider);
    }
  }

  private void init() {
    MediaProviderConfig config = providerInfo.getConfig();

    List<String> compatibleScraperIds = new ArrayList<>(COMPATIBLE_SCRAPERS.keySet());
    compatibleScraperIds.add(0, UNDEFINED); // no scraper

    config.addLabel("searchLabel", "Button.search");
    config.addSelect(SEARCH, SEARCH, compatibleScraperIds, UNDEFINED);

    // use the right key to let reflection work
    // getter in MediaMetadata must be get + Key (first letter upper case)
    config.addLabel("movieLabel", "tmm.metadata");
    config.addSelect("title", "metatag.title", compatibleScraperIds, UNDEFINED);
    config.addSelect("originalTitle", "metatag.originaltitle", compatibleScraperIds, UNDEFINED);
    config.addSelect("tagline", "metatag.tagline", compatibleScraperIds, UNDEFINED);
    config.addSelect("year", "metatag.year", compatibleScraperIds, UNDEFINED);
    config.addSelect("releaseDate", "metatag.releasedate", compatibleScraperIds, UNDEFINED);
    config.addSelect("plot", "metatag.plot", compatibleScraperIds, UNDEFINED);
    config.addSelect("runtime", "metatag.runtime", compatibleScraperIds, UNDEFINED);
    config.addSelect(RATINGS, "metatag.rating", scrapersWithout(compatibleScraperIds, MediaMetadata.TVDB), UNDEFINED); // all but tvdb

    config.addSelect("top250", "metatag.top250",
        compatibleScraperIds.contains(MediaMetadata.IMDB) ? Arrays.asList(UNDEFINED, MediaMetadata.IMDB) : Collections.singletonList(UNDEFINED),
        UNDEFINED);
    config.addSelect("genres", "metatag.genre", compatibleScraperIds, UNDEFINED);
    config.addSelect("certifications", "metatag.certification", compatibleScraperIds, UNDEFINED);
    config.addSelect("productionCompanies", "metatag.production", compatibleScraperIds, UNDEFINED);
    config.addSelect("castMembers", "metatag.cast", compatibleScraperIds, UNDEFINED);
    config.addSelect("spokenLanguages", "metatag.spokenlanguages", compatibleScraperIds, UNDEFINED);
    config.addSelect("countries", "metatag.country", compatibleScraperIds, UNDEFINED);
    config.addSelect("tags", "metatag.tags", compatibleScraperIds, UNDEFINED);
    config.addSelect("collectionName", "metatag.movieset",
        compatibleScraperIds.contains(MediaMetadata.TMDB) ? Arrays.asList(UNDEFINED, MediaMetadata.TMDB) : Collections.singletonList(UNDEFINED),
        UNDEFINED);

    config.addLabel("fallbackLabel", "scraper.universal_movie.scraperstouse");
    config.addMultiSelect(FALLBACK_SCRAPERS, "scraper.universal_movie.scrapers", scrapersWithout(compatibleScraperIds, UNDEFINED));

    config.load();
  }

  private List<String> scrapersWithout(List<String> scrapers, String... excludes) {
    List<String> newScrapers = new ArrayList<>(scrapers);

    for (String scraperToExclude : excludes) {
      newScrapers.remove(scraperToExclude);
    }

    return newScrapers;
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  @Override
  public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    SortedSet<MediaSearchResult> results = new TreeSet<>();

    IMovieMetadataProvider mp = COMPATIBLE_SCRAPERS.get(providerInfo.getConfig().getValue(SEARCH));
    if (mp == null) {
      return results;
    }

    try {
      for (MediaSearchResult result : mp.search(options)) {
        result.setId(providerInfo.getId(), result.getIdAsString(result.getProviderId()));
        result.setProviderId(providerInfo.getId());
        results.add(result);
      }
    }
    catch (ScrapeException e) {
      LOGGER.warn("Could not call search method of {} - {}", mp.getProviderInfo().getId(), e.getMessage());
      throw e;
    }

    return results;
  }

  @Override
  public MediaMetadata getMetadata(MovieSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata() - {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    MediaMetadata md = new MediaMetadata(providerInfo.getId());
    md.setScrapeOptions(options);

    // check which scrapers should be used
    Set<IMovieMetadataProvider> metadataProviders = getRelevantMetadataProviders();
    if (metadataProviders.isEmpty()) {
      return md;
    }

    // call all scrapers in different workers and wait for them to finish
    Map<String, MediaMetadata> metadataMap = getMetadataMap(metadataProviders, options);

    // and now assign the values per reflection
    assignResults(md, metadataMap);

    if (md.getIds().isEmpty()) {
      throw new NothingFoundException();
    }

    return md;
  }

  private Set<IMovieMetadataProvider> getRelevantMetadataProviders() {
    Set<IMovieMetadataProvider> metadataProviders = new HashSet<>();
    for (Map.Entry<String, String> entry : providerInfo.getConfig().getConfigKeyValuePairs().entrySet()) {
      List<String> values;

      if (FALLBACK_SCRAPERS.equals(entry.getKey())) {
        // values are in a list
        values = Arrays.asList(new Gson().fromJson(entry.getValue(), String[].class));

      }
      else {
        // plain value
        values = Collections.singletonList(entry.getValue());
      }

      for (String value : values) {
        if (!UNDEFINED.equals(value)) {
          IMovieMetadataProvider mp = COMPATIBLE_SCRAPERS.get(value);
          if (mp != null && mp.isActive()) {
            metadataProviders.add(mp);
          }
        }
      }
    }
    return metadataProviders;
  }

  private Map<String, MediaMetadata> getMetadataMap(Set<IMovieMetadataProvider> metadataProviders, MovieSearchAndScrapeOptions options) {
    // check if we have all needed IDs
    // this scraper supports scraping via TMDB id and IMDB id; but not all used providers support both
    // we may need to get the missing ones
    String imdbId = options.getImdbId();
    int tmdbId = options.getTmdbId();

    Map<String, MediaMetadata> metadataMap = new HashMap<>();

    for (IMovieMetadataProvider mp : metadataProviders) {
      if (mp instanceof IMovieImdbMetadataProvider && MediaIdUtil.isValidImdbId(imdbId)) {
        // everything is good ;)
        continue;
      }
      if (mp instanceof IMovieTmdbMetadataProvider && tmdbId > 0) {
        // everything is good ;)
        continue;
      }

      // we've come here, so we have not the needed ID
      // TMDB offers scraping by both and returns both (if available)
      if (tmdbId > 0 || MediaIdUtil.isValidImdbId(imdbId)) {
        // try to get the meta data via TMDB
        // anything cached?
        MediaMetadata md = metadataMap.get(MediaMetadata.TMDB);
        if (md == null) {
          try {
            IMovieMetadataProvider tmdb = COMPATIBLE_SCRAPERS.get(MediaMetadata.TMDB);
            if (tmdb != null) {
              md = tmdb.getMetadata(options);
              if (md != null) {
                // cache the result for later usage
                metadataMap.put(MediaMetadata.TMDB, md);
              }
            }
          }
          catch (Exception e) {
            LOGGER.warn("Could not get a result from scraper: {}", e.getMessage());
          }
        }
        // we got a response - parse out TMDB id and IMDB id if needed
        if (md != null) {
          if (tmdbId == 0) {
            Object id = md.getId(MediaMetadata.TMDB);
            if (id != null) {
              try {
                if (id instanceof Integer) {
                  // all good
                  tmdbId = (int) id;
                }
                else {
                  tmdbId = Integer.parseInt((String) md.getId(MediaMetadata.TMDB));
                }
              }
              catch (Exception ignored) {
                LOGGER.trace("could not parse tmdb id: - {}", md.getId(MediaMetadata.TMDB));
              }
            }
          }
          if (!MediaIdUtil.isValidImdbId(imdbId) && MediaIdUtil.isValidImdbId((String) md.getId(MediaMetadata.IMDB))) {
            imdbId = (String) md.getId(MediaMetadata.IMDB);
          }
        }
      }
    }

    // inject the found TMDB id and IMDB id into the search options
    if (MediaIdUtil.isValidImdbId(imdbId)) {
      options.setImdbId(imdbId);
    }
    if (tmdbId > 0) {
      options.setTmdbId(tmdbId);
    }

    // start the workers to get the metadata from the different providers
    ExecutorCompletionService<MediaMetadata> completionService = new ExecutorCompletionService<>(EXECUTOR);
    List<Future<MediaMetadata>> futures = new ArrayList<>();
    for (IMovieMetadataProvider mp : metadataProviders) {
      // look into the cache - maybe we do not need to call it again
      if (metadataMap.get(mp.getProviderInfo().getId()) == null && mp.isActive()) {
        futures.add(completionService.submit(new MetadataProviderWorker(mp, options)));
      }
    }

    // wait for all workers to finish
    for (Future<MediaMetadata> future : futures) {
      try {
        MediaMetadata mediaMetadata = future.get();
        if (mediaMetadata != null) {
          metadataMap.put(mediaMetadata.getProviderId(), mediaMetadata);
        }
      }
      catch (Exception e) {
        LOGGER.debug("Could not get a result from scraper: {}", e.getMessage());
      }
    }
    return metadataMap;
  }

  private void assignResults(MediaMetadata md, Map<String, MediaMetadata> metadataMap) {
    // take all ids we can get
    for (Map.Entry<String, MediaMetadata> entry : metadataMap.entrySet()) {
      for (Map.Entry<String, Object> id : entry.getValue().getIds().entrySet()) {
        md.setId(id.getKey(), id.getValue());
      }
    }

    // assign the requested metadata
    List<String> fallbackScrapers = Arrays.asList(new Gson().fromJson(providerInfo.getConfig().getValue(FALLBACK_SCRAPERS), String[].class));
    for (Map.Entry<String, String> entry : providerInfo.getConfig().getConfigKeyValuePairs().entrySet()) {
      if (!SEARCH.equals(entry.getKey()) && !FALLBACK_SCRAPERS.equals(entry.getKey()) && !UNDEFINED.equals(entry.getValue())) {
        List<String> scrapers = new ArrayList<>(fallbackScrapers);
        scrapers.remove(entry.getValue());
        scrapers.add(0, entry.getValue());
        assignValue(scrapers, md, metadataMap, entry.getKey());
      }
    }
  }

  private void assignValue(List<String> scrapers, MediaMetadata md, Map<String, MediaMetadata> metadataMap, String field) {
    // all specified fields should be filled from the desired scraper
    for (String scraper : scrapers) {
      MediaMetadata mediaMetadata = metadataMap.get(scraper);
      if (mediaMetadata != null) {
        try {
          Method getter = new PropertyDescriptor(field, MediaMetadata.class).getReadMethod();
          Method setter = new PropertyDescriptor(field, MediaMetadata.class).getWriteMethod();

          Object value = getter.invoke(mediaMetadata);

          if (isValueFilled(value)) {
            setter.invoke(md, value);
            return;
          }

        }
        catch (Exception e) {
          LOGGER.warn("Problem assigning {} - {}", scraper, e.getMessage());
        }
      }
    }
  }

  private boolean isValueFilled(Object value) {
    if (value == null) {
      return false;
    }

    if (value instanceof String) {
      return StringUtils.isNotBlank((String) value);
    }
    else if (value instanceof Integer) {
      return (Integer) value != 0;
    }
    else if (value instanceof Float) {
      return (Float) value != 0;
    }
    else if (value instanceof Collection) {
      return !((Collection<?>) value).isEmpty();
    }
    else if (value instanceof Date) {
      return true; // already checked with != null
    }
    else if (value instanceof MediaAiredStatus) {
      return value != MediaAiredStatus.UNKNOWN;
    }

    return false;
  }

  /****************************************************************************
   * local helper classes
   ****************************************************************************/
  protected static class MetadataProviderWorker implements Callable<MediaMetadata> {
    private final IMovieMetadataProvider      metadataProvider;
    private final MovieSearchAndScrapeOptions mediaScrapeOptions;

    MetadataProviderWorker(IMovieMetadataProvider metadataProvider, MovieSearchAndScrapeOptions mediaScrapeOptions) {
      this.metadataProvider = metadataProvider;
      this.mediaScrapeOptions = mediaScrapeOptions;
    }

    @Override
    public MediaMetadata call() throws Exception {
      return metadataProvider.getMetadata(mediaScrapeOptions);
    }
  }
}
