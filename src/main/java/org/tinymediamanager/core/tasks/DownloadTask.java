/*
 * Copyright 2012 - 2024 Manuel Laggner
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

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.scraper.http.StreamingUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.UrlUtil;

import okhttp3.Headers;

/**
 * {@link DownloadTask} for bigger downloads with status updates
 * 
 * @author Myron Boyle, Manuel Laggner
 */
public abstract class DownloadTask extends TmmTask {
  private static final Logger LOGGER   = LoggerFactory.getLogger(DownloadTask.class);

  protected String            url;
  protected Path              tempFile;
  protected String            fileType = "";

  /**
   * Starts the download of an url to a file
   * 
   * @param description
   *          the description to set
   */
  protected DownloadTask(String description, String url) {
    super(description, 100, TaskType.BACKGROUND_TASK);
    this.url = url;
  }

  /**
   * get the url to download
   * 
   * @return the url to download
   */
  protected String getUrl() {
    return this.url;
  }

  /**
   * get a special user agent for the download
   * 
   * @return the special user agent string or null
   */
  protected String getSpecialUserAgent() {
    return null;
  }

  @Override
  protected void doInBackground() {
    String url = getUrl();
    String userAgent = getSpecialUserAgent();
    Path destination = getDestinationWoExtension();
    try {
      // verify the url is not empty and starts with at least
      if (StringUtils.isBlank(url) || !url.toLowerCase(Locale.ROOT).startsWith("http")) {
        return;
      }

      // try to get the file extension from the destination filename
      String fileExtension = FilenameUtils.getExtension(destination.getFileName().toString()).toLowerCase(Locale.ROOT);
      if (StringUtils.isNotBlank(fileExtension) && fileExtension.length() > 4
          || !Settings.getInstance().getAllSupportedFileTypes().contains("." + fileExtension)) {
        fileExtension = ""; // no extension when longer than 4 chars!
      }
      // if file extension is empty, detect from url
      if (StringUtils.isBlank(fileExtension)) {
        fileExtension = UrlUtil.getExtension(url).toLowerCase(Locale.ROOT);
        if (!fileExtension.isEmpty()) {
          if (Settings.getInstance().getAllSupportedFileTypes().contains("." + fileExtension)) {
            destination = destination.getParent().resolve(destination.getFileName() + "." + fileExtension);
          }
          else {
            // unsupported filetype, eg php/asp/cgi script
            fileExtension = "";
          }
        }
      }

      LOGGER.info("Downloading '{}'", url);
      StreamingUrl u = new StreamingUrl(UrlUtil.getURIEncoded(url).toASCIIString());
      if (StringUtils.isNotBlank(userAgent)) {
        u.setUserAgent(userAgent);
      }

      long timestamp = System.currentTimeMillis();

      try {
        // create a temp file/folder inside the temp folder or tmm folder
        Path tempFolder = Paths.get(Utils.getTempFolder());
        if (!Files.exists(tempFolder)) {
          Files.createDirectory(tempFolder);
        }
        tempFile = tempFolder.resolve(getDestinationWoExtension().getFileName() + "." + timestamp + ".part"); // multi episode same file
      }
      catch (Exception e) {
        LOGGER.warn("could not write to temp folder - {}", e.getMessage());

        // could not create the temp folder somehow - put the files into the tmm/tmp dir
        tempFile = destination.resolveSibling(destination.getFileName() + "." + timestamp + ".part"); // multi episode same file
      }

      // try to resume if the temp file exists
      boolean resume = false;
      if (Files.exists(tempFile)) {
        resume = true;
        u.addHeader("Range", "bytes=" + tempFile.toFile().length() + "-");
      }

      try (InputStream is = u.getInputStream()) {
        // trace server headers
        LOGGER.trace("Server returned: {}", u.getStatusLine());
        Headers headers = u.getHeadersResponse();
        if (headers != null) {
          for (String name : ListUtils.nullSafe(headers.names())) {
            LOGGER.trace(" < {} : {}", name, headers.get(name));
          }
        }

        if (u.isFault()) {
          handleDownloadProblem(u);
          setState(TaskState.FAILED);
          return;
        }

        String type = u.getContentEncoding();
        if (StringUtils.isBlank(fileExtension)) {
          // still empty? try to parse from mime header
          if (type.startsWith("video/") || type.startsWith("audio/") || type.startsWith("image/")) {
            fileExtension = type.split("/")[1];
            fileExtension = fileExtension.replace("x-", ""); // x-wmf and others
          }
          if ("application/zip".equals(type)) {
            fileExtension = "zip";
          }
        }

        // fileExtension still empty?
        if (StringUtils.isEmpty(fileExtension)) {
          // fallback!
          fileExtension = "dat";
        }

        fileType = fileExtension.toLowerCase(Locale.ROOT);

        LOGGER.debug("Downloading to '{}'", tempFile);

        download(u, is, resume);
      }

      checkDownloadedFile();

      if (cancel) {
        // delete half downloaded file
        Utils.deleteFileSafely(tempFile);
      }
      else {
        if (fileExtension.isEmpty()) {
          // STILL empty? hmpf...
          // now we have a chicken-egg problem:
          // MediaInfo needs MF type to correctly fetch extension
          // to detect MF type, we need the extension
          // so we are forcing to read the container type direct on tempFile
          MediaFile mf = new MediaFile(tempFile);
          mf.setContainerFormatDirect(); // force direct read of mediainfo - regardless of filename!!!
          fileExtension = mf.getContainerFormat();
        }

        moveDownloadedFile(fileExtension);

        downloadFinished();
      } // end isCancelled
    }
    catch (InterruptedException | InterruptedIOException e) {
      LOGGER.info("download of {} aborted", url);
      setState(TaskState.CANCELLED);
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "DownloadTask", e.getMessage()));
      LOGGER.error("problem downloading: ", e);
      setState(TaskState.FAILED);
    }
    finally {
      // remove temp file
      if (tempFile != null && Files.exists(tempFile)) {
        Utils.deleteFileSafely(tempFile);
      }
    }
  }

  protected void download(StreamingUrl url, InputStream is, boolean resume) throws IOException, InterruptedException {
    long length = url.getContentLength();

    try (BufferedInputStream bufferedInputStream = new BufferedInputStream(is);
        FileOutputStream outputStream = new FileOutputStream(tempFile.toFile(), resume)) {
      int count;
      byte[] buffer = new byte[2048];
      Long timestamp1 = System.nanoTime();
      Long timestamp2;
      long bytesDone = 0;
      long bytesDonePrevious = 0;
      double speed = 0;

      while ((count = bufferedInputStream.read(buffer, 0, buffer.length)) != -1) {
        if (cancel) {
          Thread.currentThread().interrupt();
        }

        outputStream.write(buffer, 0, count);
        bytesDone += count;

        // we push the progress only once per 250ms (to use less performance and get a better download speed)
        timestamp2 = System.nanoTime();
        if (timestamp2 - timestamp1 > 250000000) {
          // avg. speed between the actual and the previous
          speed = (speed + (bytesDone - bytesDonePrevious) / ((double) (timestamp2 - timestamp1) / 1000000000)) / 2;

          timestamp1 = timestamp2;
          bytesDonePrevious = bytesDone;

          if (length > 0) {
            publishState(formatBytesForOutput(bytesDone) + "/" + formatBytesForOutput(length) + " @" + formatSpeedForOutput(speed),
                (int) (bytesDone * 100 / length));
          }
          else {
            setWorkUnits(0);
            publishState(formatBytesForOutput(bytesDone) + " @" + formatSpeedForOutput(speed), 0);
          }
        }
      }
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (trailer download) - '{}'", e.getMessage());
      // re-throw
      throw e;
    }

    // we must not close the input stream on cancel(the rest will be downloaded if we close it on cancel)
    if (!cancel) {
      is.close();
    }
  }

  protected void moveDownloadedFile(String fileExtension) throws IOException {
    Path destination = getDestinationWoExtension();
    if (!fileExtension.isEmpty()) {
      destination = destination.getParent().resolve(destination.getFileName() + "." + fileExtension);
    }

    Utils.deleteFileSafely(destination); // delete existing file

    // create parent if needed
    if (!Files.exists(destination.getParent())) {
      Files.createDirectory(destination.getParent());
    }

    boolean ok = Utils.moveFileSafe(tempFile, destination);
    if (ok) {
      Utils.deleteFileSafely(tempFile);

      MediaEntity mediaEntity = getMediaEntityToAdd();
      if (mediaEntity != null) {
        MediaFile mf = new MediaFile(destination);
        mf.gatherMediaInformation();
        mediaEntity.removeFromMediaFiles(mf); // remove old (possibly same) file
        mediaEntity.addToMediaFiles(mf); // add file, but maybe with other MI values
        mediaEntity.saveToDb();
      }
    }
    else {
      LOGGER.warn("Download to '{}' was ok, but couldn't move to '{}'", tempFile, destination);
      setState(TaskState.FAILED);
    }
  }

  /**
   * optional checks for the downloaded file
   * 
   * @throws IOException
   *           any {@link IOException} that may occur while checking
   */
  protected void checkDownloadedFile() throws IOException {
    // nothing to do here
  }

  /**
   * callback for finished download
   */
  protected void downloadFinished() {
    // nothing to do here
  }

  /**
   * callback to handle any download problem
   * 
   * @param url
   *          the url containing the download problem
   */
  protected void handleDownloadProblem(Url url) {
    LOGGER.error("Could not download the url '{}' - HTTP '{}' - '{}'", url.getUrlAsString(), url.getStatusCode(), url.getStatusLine());
    MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, url.getUrlAsString(), url.getStatusLine()));
  }

  /**
   * get the desired filename without extension. The extension is inherited from the download itself
   * 
   * @return the desired filename for the downloaded file
   */
  protected abstract Path getDestinationWoExtension();

  /**
   * get the {@link MediaEntity} to add the downloaded file to
   * 
   * @return the {@link MediaEntity} to add the downloaded file to
   */
  protected abstract MediaEntity getMediaEntityToAdd();

  private String formatBytesForOutput(long bytes) {
    return String.format("%.2fM", (double) bytes / (1000d * 1000d));
  }

  private String formatSpeedForOutput(double speed) {
    return String.format("%.2fkB/s", speed / 1000d);
  }
}
