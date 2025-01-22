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
package org.tinymediamanager.scraper.interfaces;

import java.util.List;
import java.util.Map;

import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.ScrapeException;

/**
 * this interface indicates that the media provider is able to fetch ratings
 * 
 * @author Manuel Laggner
 */
public interface IRatingProvider {
  /**
   * get the supported {@link MediaRating}s for the given IDs
   * 
   * @param ids
   *          the media IDs to get the ratings for
   * @param mediaType
   *          the {@link MediaType} to get the ratings for
   * @return a {@link List} of all found {@link MediaRating}s
   * @throws ScrapeException
   *           any {@link Exception} occurred
   */
  List<MediaRating> getRatings(Map<String, Object> ids, MediaType mediaType) throws ScrapeException;
}
