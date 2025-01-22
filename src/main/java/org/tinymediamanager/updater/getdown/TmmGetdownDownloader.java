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

package org.tinymediamanager.updater.getdown;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.http.StreamingUrl;
import org.tinymediamanager.scraper.http.Url;

import com.threerings.getdown.data.Resource;

/**
 * The tinyMediaManager implementation of the {@link com.threerings.getdown.net.Downloader}.
 * 
 * @author Manuel Laggner
 */
public class TmmGetdownDownloader extends com.threerings.getdown.net.Downloader {
  private static final Logger LOGGER = LoggerFactory.getLogger(TmmGetdownDownloader.class);

  public TmmGetdownDownloader() {
    super(null);
  }

  @Override
  public boolean download(Collection<Resource> resources, int maxConcurrent) {
    // make sure all directories in the download folder has been created
    for (final Resource rsrc : resources) {
      // make sure the resource's target directory exists
      File parent = new File(rsrc.getLocalNew().getParent());
      if (!parent.exists() && !parent.mkdirs()) {
        LOGGER.debug("Failed to create target directory for resource '{}'", rsrc);
      }
    }
    // perform the download
    super.download(resources, maxConcurrent);

    LOGGER.debug("Finished downloading with state '{}'", _state);

    // return true if the download has been completed
    return _state == State.COMPLETE;
  }

  @Override
  protected void downloadProgress(int percent, long remaining) {
    if (percent == 100) {
      _state = State.COMPLETE;
    }
  }

  @Override
  protected long checkSize(Resource rsrc) throws IOException {
    Url url = new Url(rsrc.getRemote().toString());
    try (InputStream ignored = url.getInputStream(true)) {
      // make sure we got a satisfactory response code
      checkConnectOK(url, "Unable to check up-to-date for " + rsrc.getRemote());
      return url.getContentLength();
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InterruptedIOException();
    }
  }

  @Override
  protected void download(Resource rsrc) throws IOException {
    StreamingUrl url = new StreamingUrl(rsrc.getRemote().toString());

    // download the resource from the specified URL
    try (InputStream in = url.getInputStream(); FileOutputStream out = new FileOutputStream(rsrc.getLocalNew())) {
      // make sure we got a satisfactory response code
      checkConnectOK(url, "Unable to download resource " + rsrc.getRemote());

      long actualSize = url.getContentLength();
      LOGGER.debug("Downloading resource [url={}, size={}]", rsrc.getRemote(), actualSize);
      long currentSize = 0L;
      byte[] buffer = new byte[4 * 4096];

      // read in the file data
      int read;
      while ((read = in.read(buffer)) != -1) {
        // abort the download if the downloader is aborted
        if (_state == State.ABORTED) {
          break;
        }
        // write it out to our local copy
        out.write(buffer, 0, read);
        // note that we've downloaded some data
        currentSize += read;
        reportProgress(rsrc, currentSize, actualSize);
      }
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InterruptedIOException();
    }
    catch (Exception e) {
      _state = State.FAILED;
      downloadFailed(rsrc, e);
    }
  }

  /**
   * Checks that {@code conn} returned an {@code OK} response code if it is an HTTP connection. If the connection failed for proxy related reasons,
   * this changes the state of this connector to reflect the needed proxy information.
   */
  private void checkConnectOK(Url url, String errpre) throws IOException {

    int code = url.getStatusCode();
    switch (code) {
      case HttpURLConnection.HTTP_OK:
        return;

      case HttpURLConnection.HTTP_FORBIDDEN:
      case HttpURLConnection.HTTP_USE_PROXY:
        break;

      case HttpURLConnection.HTTP_PROXY_AUTH:
        break;
    }
    throw new IOException(errpre + " [code=" + code + "]");
  }

  @Override
  protected void downloadFailed(Resource rsrc, Exception cause) {
    LOGGER.error("Could not download resource '{}' - {}", rsrc.getRemote(), cause.getMessage());
  }
}
