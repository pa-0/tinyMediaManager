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
package org.tinymediamanager.core.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.scraper.imdb.ImdbMovieTrailerProvider;

/**
 * A task for downloading trailers
 *
 * @author Manuel Laggner
 */
public abstract class TrailerDownloadTask extends DownloadTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(TrailerDownloadTask.class);
  private final MediaTrailer  mediaTrailer;

  protected TrailerDownloadTask(MediaTrailer trailer) {
    super(TmmResourceBundle.getString("trailer.download") + " - " + trailer.getName(), trailer.getUrl());
    this.mediaTrailer = trailer;

    setTaskDescription(trailer.getName());
  }

  @Override
  protected void doInBackground() {
    if (!isFeatureEnabled()) {
      return;
    }

    String url = mediaTrailer.getUrl();

    // we cannot download from Apple Trailer anymore (closed on 01.09.2023)
    if (url.contains("trailers.apple.com")) {
      LOGGER.error("Could not download trailer: Apple Trailers has been shut down on 01.09.2023");
      return;
    }

    if (!url.startsWith("http")) {
      // we have an ID - lets check if it is a known one:
      String id = mediaTrailer.getId();
      if (!id.matches("vi\\d+")) { // IMDB
        LOGGER.debug("Could not download trailer: id not known {}", mediaTrailer);
        return;
      }

      // IMD trailer ID
      ImdbMovieTrailerProvider tp = new ImdbMovieTrailerProvider();
      url = tp.getUrlForId(mediaTrailer);
      if (url.isEmpty()) {
        LOGGER.debug("Could not download trailer: could not construct url from id {}", mediaTrailer);
        return;
      }
      mediaTrailer.setUrl(url);
    }

    super.doInBackground();
  }

  @Override
  protected String getSpecialUserAgent() {
    if ("apple".equalsIgnoreCase(mediaTrailer.getProvider())) {
      return "QuickTime";
    }
    return null;
  }

  @Override
  protected void checkDownloadedFile() throws IOException {
    super.checkDownloadedFile();

    BasicFileAttributes view = Files.readAttributes(tempFile, BasicFileAttributes.class);

    // the trailer must not be smaller than 1MB
    if (view.size() < 1000000) {
      // just cancel - cleanup is in DownloadTask
      cancel = true;
    }
  }
}
