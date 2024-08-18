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
package org.tinymediamanager.thirdparty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.addon.YtDlpAddon;

/**
 * the class {@link YtDlp} is used to access yt-dlp for downloading trailers from yt
 * 
 * @author Manuel Laggner
 */
public class YtDlp {
  private static final Logger LOGGER = LoggerFactory.getLogger(YtDlp.class);

  private YtDlp() {
    throw new IllegalAccessError();
  }

  /**
   * download the trailer with yt-dlp for the given video url
   *
   * @param url
   *          the url to the trailer
   * @param width
   *          the desired with
   * @param trailerFile
   *          the path to the trailer file which should be written (without extension)
   * @throws IOException
   *           any {@link IOException} occurred
   * @throws InterruptedException
   *           being thrown if the thread has been interrupted
   */
  public static void downloadTrailer(String url, int width, Path trailerFile) throws IOException, InterruptedException {
    executeCommand(createCommandForDownload(url, width, trailerFile));
  }

  /**
   * create the download command for yt-dlp for the given video url
   *
   * @param url
   *          the url to the trailer
   * @param width
   *          the desired with
   * @param trailerFile
   *          the path to the trailer file which should be written (without extension)
   * @throws IOException
   *           any {@link IOException} occurred
   */
  private static List<String> createCommandForDownload(String url, int width, Path trailerFile) throws IOException {
    List<String> cmdList = new ArrayList<>();
    cmdList.add(getYtDlpExecutable());

    if (FFmpeg.isAvailable()) {
      cmdList.add("--ffmpeg-location");
      cmdList.add(FFmpeg.getFfmpegExecutable());
    }

    cmdList.add("-f");
    cmdList.add("bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4] / bv*+ba/b");

    if (width > 0) {
      cmdList.add("-S");
      cmdList.add("res:" + width);
    }

    cmdList.add(url);
    cmdList.add("-o");
    cmdList.add(trailerFile.toAbsolutePath().toString());

    return cmdList;
  }

  private static String executeCommand(List<String> cmdline) throws IOException, InterruptedException {
    LOGGER.debug("Running command: {}", String.join(" ", cmdline));

    ProcessBuilder pb = new ProcessBuilder(cmdline.toArray(new String[0])).redirectErrorStream(true);
    final Process process = pb.start();

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      new Thread(() -> {
        try {
          IOUtils.copy(process.getInputStream(), outputStream);
        }
        catch (IOException e) {
          LOGGER.debug("could not get output from the process", e);
        }
      }).start();

      int processValue = process.waitFor();
      String response = outputStream.toString(StandardCharsets.UTF_8);
      if (processValue != 0) {
        LOGGER.warn("error at yt-dlp: '{}'", response);
        throw new IOException("error running yt-dlp - code '" + processValue + "'");
      }
      return response;
    }
    finally {
      if (process != null) {
        process.destroy();
        // Process must be destroyed before closing streams, can't use try-with-resources,
        // as resources are closing when leaving try block, before finally
        IOUtils.close(process.getErrorStream());
      }
    }
  }

  public static boolean isAvailable() {
    YtDlpAddon ytDlpAddon = new YtDlpAddon();
    return ytDlpAddon.isAvailable();
  }

  private static String getYtDlpExecutable() throws IOException {
    YtDlpAddon ytDlpAddon = new YtDlpAddon();

    if (ytDlpAddon.isAvailable()) {
      return ytDlpAddon.getExecutablePath();
    }
    else {
      throw new IOException("yt-dlp is not available");
    }
  }
}
