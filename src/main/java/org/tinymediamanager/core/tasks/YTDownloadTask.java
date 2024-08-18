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

import static org.tinymediamanager.core.Utils.cleanFilename;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmMuxer;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.TrailerQuality;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.scraper.http.StreamingUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.thirdparty.YtDlp;
import org.tinymediamanager.thirdparty.yt.YTDownloader;

import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.formats.VideoFormat;
import com.github.kiulian.downloader.model.videos.formats.VideoWithAudioFormat;
import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;

/**
 * A task for downloading trailers from YT
 *
 * @author Wolfgang Janes
 */
public abstract class YTDownloadTask extends TmmTask {

  private static final Logger  LOGGER            = LoggerFactory.getLogger(YTDownloadTask.class);
  private static final int     MAX_CHUNK_SIZE    = 10 * 1024 * 1024;                             // 8M

  private final MediaTrailer   mediaTrailer;
  private final TrailerQuality desiredQuality;
  private final boolean        useYtDlp;

  private VideoInfo            video;

  /* helpers for calculating the download speed */
  private long                 timestamp1        = System.nanoTime();
  private long                 length;
  private long                 bytesDone         = 0;
  private long                 bytesDonePrevious = 0;
  private double               speed             = 0;

  protected YTDownloadTask(MediaTrailer mediaTrailer, TrailerQuality desiredQuality, boolean useYtDlp) {
    super(TmmResourceBundle.getString("trailer.download") + " - " + mediaTrailer.getName(), 100, TaskType.BACKGROUND_TASK);
    this.mediaTrailer = mediaTrailer;
    this.desiredQuality = desiredQuality;
    this.useYtDlp = useYtDlp;

    setTaskDescription(mediaTrailer.getName());
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

  @Override
  protected void doInBackground() {
    if (!isFeatureEnabled()) {
      return;
    }

    try {
      String id = "";
      // get the youtube id
      Matcher matcher = Utils.YOUTUBE_PATTERN.matcher(mediaTrailer.getUrl());
      if (matcher.matches()) {
        id = matcher.group(5);
      }

      if (StringUtils.isBlank(id)) {
        LOGGER.debug("Could not download trailer: no id {}", mediaTrailer);
        return;
      }

      if (useYtDlp && YtDlp.isAvailable()) {
        downloadWithYtDlp(mediaTrailer);
        return;
      }

      YTDownloader downloader = new YTDownloader();
      Response<VideoInfo> videoInfo = downloader.getVideoInfo(new RequestVideoInfo(id));
      if (!videoInfo.ok()) {
        if (videoInfo.error() != null) {
          LOGGER.debug("Could not download trailer: '{}'", videoInfo.error().toString());
        }
        else {
          LOGGER.debug("Could not download trailer: response NOK");
        }
        return;
      }

      video = videoInfo.data();

      // search for a combined audio-video stream
      VideoWithAudioFormat audioVideoFormat = findCombinedStream();

      if (audioVideoFormat != null) {
        downloadCombinedStream(audioVideoFormat);
        return;
      }

      // no combined audio/video format found -> search for separate streams and mux it
      Format[] streams = findSeparateStreams();
      if (streams.length == 2) {
        downloadSeparateStreams((VideoFormat) streams[0], (AudioFormat) streams[1]);
        return;
      }

      // still nothing found? just try to get the best possible
      streams = findBestStreams();
      if (streams.length == 1 && streams[0] instanceof VideoWithAudioFormat) {
        downloadCombinedStream((VideoWithAudioFormat) streams[0]);
      }
      else if (streams.length == 2) {
        downloadSeparateStreams((VideoFormat) streams[0], (AudioFormat) streams[1]);
      }
    }
    catch (Exception | Error e) { // Error due to some AssertionErrors which may be thrown by the mp4parser
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "Youtube trailer downloader", "message.trailer.downloadfailed",
          new String[] { getMediaEntityToAdd().getTitle() }));
      setState(TaskState.FAILED);
      LOGGER.error("download of Trailer {} failed", mediaTrailer.getUrl());
      LOGGER.debug("trailer download - '{}'", e.getMessage());
    }
  }

  private void downloadWithYtDlp(MediaTrailer mediaTrailer) {
    MediaEntity mediaEntity = getMediaEntityToAdd();

    try {
      // delete any existing trailer files
      for (MediaFile trailerMediaFile : mediaEntity.getMediaFiles(MediaFileType.TRAILER)) {
        Utils.deleteFileSafely(trailerMediaFile.getFile());
      }

      Path trailerBasename = getDestinationWoExtension().getParent().resolve(getDestinationWoExtension().getFileName());

      int width = 0;

      try {
        VideoQuality videoQuality = getVideoQuality(mediaTrailer.getQuality());
        width = Integer.parseInt(videoQuality.name().toLowerCase().replace("hd", "").replace("p", ""));
      }
      catch (Exception ignored) {
      }

      YtDlp.downloadTrailer(mediaTrailer.getUrl(), width, trailerBasename);

      // add new trailer
      Path trailerFilename = Paths.get(trailerBasename + ".mp4");
      if (Files.exists(trailerFilename)) {
        MediaFile mf = new MediaFile(trailerFilename, MediaFileType.TRAILER);
        mf.gatherMediaInformation();
        mediaEntity.removeFromMediaFiles(mf); // remove old (possibly same) file
        mediaEntity.addToMediaFiles(mf); // add file, but maybe with other MI values
        mediaEntity.saveToDb();
      }

    }
    catch (Exception e) {
      LOGGER.error("Could not download trailer using yt-dlp - '{}'", e.getMessage());
    }
  }

  private VideoWithAudioFormat findCombinedStream() {
    VideoWithAudioFormat audioVideoFormat = null;

    List<VideoWithAudioFormat> audioVideoFormats = video.videoWithAudioFormats();

    if (!"unknown".equalsIgnoreCase(mediaTrailer.getQuality())) {
      // if there is an explicit quality in the URL
      for (VideoWithAudioFormat format : audioVideoFormats) {
        if (format.videoQuality() == getVideoQuality(mediaTrailer.getQuality()) && Extension.MPEG4 == format.extension()) {
          audioVideoFormat = format;
          break;
        }
      }
    }
    else if (desiredQuality != null) {
      // try to get in the preferred quality
      VideoQuality quality = null;
      for (String q : desiredQuality.getPossibleQualities()) {
        quality = getVideoQuality(q);
        if (quality != null) {
          break;
        }
      }

      if (quality != null) {
        for (VideoWithAudioFormat format : audioVideoFormats) {
          if (format.videoQuality() == quality && Extension.MPEG4 == format.extension()) {
            audioVideoFormat = format;
            break;
          }
        }
      }
    }

    return audioVideoFormat;
  }

  private VideoQuality getVideoQuality(String text) {
    switch (text.toLowerCase(Locale.ROOT)) {
      case "unknown":
        return VideoQuality.unknown;

      case "3072p":
        return VideoQuality.highres;

      case "2880p":
        return VideoQuality.hd2880p;

      case "2160p":
        return VideoQuality.hd2160;

      case "1440p":
        return VideoQuality.hd1440;

      case "1080p":
        return VideoQuality.hd1080;

      case "720p":
        return VideoQuality.hd720;

      case "480p":
        return VideoQuality.large;

      case "360p":
        return VideoQuality.medium;

      case "240p":
        return VideoQuality.small;

      case "144p":
        return VideoQuality.tiny;

      default:
        return null;
    }
  }

  private Format[] findSeparateStreams() {
    VideoFormat videoFormat = findVideo(video, getVideoQuality(mediaTrailer.getQuality()), Extension.MPEG4);
    AudioFormat audioFormat = findBestAudio(video, Extension.M4A);

    if (videoFormat != null && audioFormat != null) {
      return new Format[] { videoFormat, audioFormat };
    }
    return new Format[0];
  }

  private Format[] findBestStreams() {
    Format videoStreamInBestQuality = null;
    VideoQuality bestQuality = null;

    for (Format format : video.formats()) {
      if (format instanceof VideoWithAudioFormat audioVideoFormat) {
        if (bestQuality == null) {
          // just take it
          bestQuality = audioVideoFormat.videoQuality();
          videoStreamInBestQuality = format;
        }
        else if (bestQuality.ordinal() <= audioVideoFormat.videoQuality().ordinal()) {
          // <= because we prefer combined over separate streams
          bestQuality = audioVideoFormat.videoQuality();
          videoStreamInBestQuality = format;
        }
      }
      else if (format instanceof VideoFormat audioVideoFormat) {
        if (bestQuality == null) {
          // just take it
          bestQuality = audioVideoFormat.videoQuality();
          videoStreamInBestQuality = format;
        }
        else if (bestQuality.ordinal() < audioVideoFormat.videoQuality().ordinal()) {
          bestQuality = audioVideoFormat.videoQuality();
          videoStreamInBestQuality = format;
        }
      }
    }

    // now check if we got anything
    if (videoStreamInBestQuality != null) {
      // yeah - we found at least one video stream
      if (videoStreamInBestQuality instanceof VideoWithAudioFormat) {
        // yeah #2 - a combined stream
        return new Format[] { videoStreamInBestQuality };
      }
      else if (videoStreamInBestQuality instanceof VideoFormat) {
        // okay at least we have a video stream; search for a matching audio stream
        AudioFormat audioFormat = findBestAudio(video, Extension.M4A);
        if (audioFormat != null) {
          return new Format[] { videoStreamInBestQuality, audioFormat };
        }
      }
    }

    return new Format[0];
  }

  private VideoFormat findVideo(VideoInfo video, VideoQuality videoQuality, Extension extension) {
    for (VideoFormat format : video.videoFormats()) {
      if (format.videoQuality() == videoQuality && format.extension().equals(extension)) {
        return format;
      }
    }

    LOGGER.debug("could not find video with quality {} and format {}", videoQuality, extension);
    return null;
  }

  private AudioFormat findBestAudio(VideoInfo video, Extension extension) {

    // search for all audio formats in the given quality order
    for (AudioQuality quality : getAudioQualityList()) {
      for (AudioFormat format : video.audioFormats()) {
        if (format.audioQuality() == quality && format.extension().equals(extension)) {
          return format;
        }
      }
    }

    // nothing found so far
    LOGGER.debug("Could not find audio format for extension {}", extension);
    return null;
  }

  private List<AudioQuality> getAudioQualityList() {
    List<AudioQuality> list = new ArrayList<>();
    list.add(AudioQuality.high);
    list.add(AudioQuality.medium);
    list.add(AudioQuality.low);

    return list;
  }

  private List<VideoQuality> getVideoQualityList() {
    List<VideoQuality> list = new ArrayList<>();
    list.add(VideoQuality.highres);
    list.add(VideoQuality.hd2880p);
    list.add(VideoQuality.hd2160);
    list.add(VideoQuality.hd1440);
    list.add(VideoQuality.hd1080);
    list.add(VideoQuality.hd720);
    list.add(VideoQuality.large);
    list.add(VideoQuality.medium);
    list.add(VideoQuality.small);
    list.add(VideoQuality.tiny);

    return list;
  }

  private void downloadCombinedStream(VideoWithAudioFormat audioVideoFormat) throws Exception {
    MediaEntity mediaEntity = getMediaEntityToAdd();

    // we've found a combined audio/video format for the requested quality, so we don't need to mux it ;)
    Path tempFile = download(audioVideoFormat);

    if (tempFile != null) {
      // delete any existing trailer files
      for (MediaFile trailerMediaFile : mediaEntity.getMediaFiles(MediaFileType.TRAILER)) {
        Utils.deleteFileSafely(trailerMediaFile.getFile());
      }

      Path trailer = getDestinationWoExtension().getParent().resolve(getDestinationWoExtension().getFileName() + ".mp4");

      // create parent if needed
      if (!Files.exists(trailer.getParent())) {
        Files.createDirectory(trailer.getParent());
      }

      // and move the temporary file
      Utils.moveFileSafe(tempFile, trailer);

      MediaFile mf = new MediaFile(trailer, MediaFileType.TRAILER);
      mf.gatherMediaInformation();
      mediaEntity.removeFromMediaFiles(mf); // remove old (possibly same) file
      mediaEntity.addToMediaFiles(mf); // add file, but maybe with other MI values
      mediaEntity.saveToDb();
    }
  }

  private void downloadSeparateStreams(VideoFormat videoFormat, AudioFormat audioFormat) throws Exception {
    MediaEntity mediaEntity = getMediaEntityToAdd();

    if (videoFormat == null || audioFormat == null) {
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "Youtube trailer downloader", "message.trailer.unsupported",
          new String[] { mediaEntity.getTitle() }));
      LOGGER.error("Could not download movieTrailer for {}", mediaEntity.getTitle());
      setState(TaskState.FAILED);
      return;
    }

    ExecutorService executorService = Executors.newFixedThreadPool(2);

    // start Futures to download the two streams
    Future<Path> futureVideo = executorService.submit(() -> {
      try {
        LOGGER.debug("Downloading video....");
        return download(videoFormat);
      }
      catch (Exception e) {
        LOGGER.error("Could not download video stream: {}", e.getMessage());
        setState(TaskState.FAILED);
        return null;
      }

    });
    Future<Path> futureAudio = executorService.submit(() -> {
      try {
        LOGGER.debug("Downloading audio....");
        return download(audioFormat);
      }
      catch (Exception e) {
        LOGGER.error("Could not download audio stream: {}", e.getMessage());
        setState(TaskState.FAILED);
        return null;
      }
    });

    Path videoFile = futureVideo.get();
    Path audioFile = futureAudio.get();

    if (videoFile != null && audioFile != null) {

      // Mux the audio and video
      LOGGER.trace("Muxing...");
      TmmMuxer muxer = new TmmMuxer(audioFile, videoFile);
      Path trailer = getDestinationWoExtension().getParent().resolve(getDestinationWoExtension().getFileName() + ".mp4");

      // create parent if needed
      if (!Files.exists(trailer.getParent())) {
        Files.createDirectory(trailer.getParent());
      }

      muxer.mergeAudioVideo(trailer);
      LOGGER.trace("Muxing finished");

      MediaFile mf = new MediaFile(trailer, MediaFileType.TRAILER);
      mf.gatherMediaInformation();
      mediaEntity.removeFromMediaFiles(mf); // remove old (possibly same) file
      mediaEntity.addToMediaFiles(mf); // add file, but maybe with other MI values
      mediaEntity.saveToDb();
    }
    else {
      setState(TaskState.FAILED);
      return;
    }

    // Delete the temp audio and video files
    Utils.deleteFileSafely(videoFile);
    Utils.deleteFileSafely(audioFile);
  }

  /**
   * Download the given format ( either Audio or Video
   *
   * @param format
   *          Video or Audio Format
   * @return a {@link Path} object of the downloaded file
   * @throws IOException
   *           any {@link IOException} occurred while downloading
   * @throws InterruptedException
   *           a {@link InterruptedException} if the download is cancelled
   */
  private Path download(Format format) throws IOException, InterruptedException {
    String fileName;
    Path tempDir = Paths.get(Utils.getTempFolder());

    if (!Files.exists(tempDir)) {
      Files.createDirectory(tempDir);
    }
    if (format.itag().isVideo()) {
      fileName = video.details().title() + "(V)." + format.extension().value();
    }
    else {
      fileName = video.details().title() + "(A)." + format.extension().value();
    }
    Path outputFile = tempDir.resolve(cleanFilename(fileName));

    Long contentLength = format.contentLength();
    if (format.contentLength() == null) {
      // no content length - try to get it via head request
      Url head = new Url(format.url());
      head.getInputStream(true);
      contentLength = head.getContentLength();
    }

    addContentLength(contentLength);
    int rangeStart = 0;

    try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile.toFile())) {
      while (rangeStart < contentLength - 1) {
        StreamingUrl url = new StreamingUrl(format.url());

        // chunks > 10M will be throttled by yt - cap then to a random chunk between 95% - 99% of 10M
        int chunkSize;
        int remaining = (int) (contentLength - rangeStart - 1);

        if (remaining > MAX_CHUNK_SIZE) {
          chunkSize = ThreadLocalRandom.current().nextInt((int) (MAX_CHUNK_SIZE * 0.95), (int) (MAX_CHUNK_SIZE * 0.99));
        }
        else {
          chunkSize = remaining;
        }

        url.addHeader("Range", "bytes=" + rangeStart + "-" + (rangeStart + chunkSize));

        try (InputStream is = url.getInputStream(); BufferedInputStream bis = new BufferedInputStream(is)) {
          byte[] buffer = new byte[2048];
          int count;

          while ((count = bis.read(buffer, 0, buffer.length)) != -1) {
            if (cancel) {
              Thread.currentThread().interrupt();
              LOGGER.info("download of {} aborted", url);
              return null;
            }

            fileOutputStream.write(buffer, 0, count);
            addBytesDone(count);
          }

        }
        rangeStart = rangeStart + chunkSize + 1;
      }
      Utils.flushFileOutputStreamToDisk(fileOutputStream);
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (writing trailer) - '{}'", e.getMessage());
      // re-throw
      throw e;
    }

    return outputFile;
  }

  private synchronized void addContentLength(long length) {
    this.length += length;
  }

  private synchronized void addBytesDone(long count) {
    bytesDone += count;

    // we push the progress only once per 250ms (to use less performance and get a better download speed)
    long timestamp2 = System.nanoTime();
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

  private String formatBytesForOutput(long bytes) {
    return String.format("%.2fM", (double) bytes / (1000d * 1000d));
  }

  private String formatSpeedForOutput(double speed) {
    return String.format("%.2fkB/s", speed / 1000d);
  }

}
