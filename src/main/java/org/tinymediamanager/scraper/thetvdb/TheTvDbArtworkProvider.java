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
package org.tinymediamanager.scraper.thetvdb;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaArtworkProvider;
import org.tinymediamanager.scraper.thetvdb.entities.ArtworkBaseRecord;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;

/**
 * the class {@link TheTvDbArtworkProvider} is the superclass for TVDB artwork providers
 *
 * @author Manuel Laggner
 */
public abstract class TheTvDbArtworkProvider extends TheTvDbMetadataProvider implements IMediaArtworkProvider {

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = super.createMediaProviderInfo();

    info.getConfig().addText("apiKey", "", true);
    info.getConfig().addText("pin", "", true);
    info.getConfig().load();

    return info;
  }

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {
    // lazy initialization of the api
    initAPI();

    getLogger().debug("getting artwork: {}", options);
    List<MediaArtwork> artwork = new ArrayList<>();

    // do we have an id from the options?
    int id = options.getIdAsInt(getProviderInfo().getId());

    if (id == 0 && MediaIdUtil.isValidImdbId(options.getImdbId())) {
      id = getTvdbIdViaImdbId(options.getImdbId());
    }

    if (id == 0) {
      getLogger().warn("no id available");
      throw new MissingIdException(getProviderInfo().getId());
    }

    // get artwork from thetvdb
    List<ArtworkBaseRecord> images = fetchArtwork(id);

    if (ListUtils.isEmpty(images)) {
      return artwork;
    }

    // sort it
    images.sort(new TheTvDbArtworkProvider.ImageComparator(LanguageUtils.getIso3Language(options.getLanguage().toLocale())));

    // get base show artwork
    for (ArtworkBaseRecord image : images) {
      MediaArtwork ma = parseArtwork(image);

      if (ma == null) {
        continue;
      }

      if (options.getArtworkType() == MediaArtwork.MediaArtworkType.ALL || options.getArtworkType() == ma.getType()) {
        artwork.add(ma);
      }
    }

    return artwork;
  }

  /**
   * let the concrete implementation fetch the artwork
   * 
   * @param id
   *          the tvdbid to fetch the artwork for
   * @return a {@link List} of {@link ArtworkBaseRecord}
   * @throws ScrapeException
   *           any exception occurred while scraping
   */
  protected abstract List<ArtworkBaseRecord> fetchArtwork(int id) throws ScrapeException;

  /**********************************************************************
   * local helper classes
   **********************************************************************/
  protected static class ImageComparator implements Comparator<ArtworkBaseRecord> {
    private final String preferredLangu;
    private final String english;

    protected ImageComparator(String language) {
      preferredLangu = language;
      english = "eng";
    }

    /*
     * sort artwork: primary by language: preferred lang (ie de), en, others; then: score
     */
    @Override
    public int compare(ArtworkBaseRecord arg0, ArtworkBaseRecord arg1) {
      if (preferredLangu.equals(arg0.language) && !preferredLangu.equals(arg1.language)) {
        return -1;
      }

      // check if second image is preferred langu
      if (!preferredLangu.equals(arg0.language) && preferredLangu.equals(arg1.language)) {
        return 1;
      }

      // check if the first image is en
      if (english.equals(arg0.language) && !english.equals(arg1.language)) {
        return -1;
      }

      // check if the second image is en
      if (!english.equals(arg0.language) && english.equals(arg1.language)) {
        return 1;
      }

      int result = 0;

      if (arg0.score != null && arg1.score != null) {
        // swap arg0 and arg1 to sort reverse
        result = Long.compare(arg1.score, arg0.score);
      }

      // if the result is still 0, we need to compare by ID (returning a zero here will treat it as a duplicate and remove the previous one)
      if (result == 0) {
        result = Long.compare(arg0.id, arg1.id);
      }

      return result;
    }
  }
}
