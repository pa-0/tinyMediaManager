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

import java.io.FileNotFoundException;
import java.io.InterruptedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.EmptyFileException;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.entities.MediaFile;

/**
 * The class {@link ImageCacheTask} is used to build the cache for an image
 * 
 * @author Manuel Laggner
 */
public class ImageCacheTask implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImageCacheTask.class);

  private final MediaFile     mediaFile;

  public ImageCacheTask(MediaFile mediaFile) {
    this.mediaFile = mediaFile;
  }

  @Override
  public void run() {
    try {
      ImageCache.cacheImage(mediaFile);
    }
    catch (EmptyFileException e) {
      LOGGER.debug("failed to cache file (file is empty): {}", mediaFile);
    }
    catch (FileNotFoundException e) {
      // silently ignore
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.warn("failed to cache file: {} - {}", mediaFile.getFile(), e.getMessage());
    }
  }
}
