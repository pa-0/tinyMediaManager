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

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ArdSettings;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.mediainfo.MediaInfoFile;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.thirdparty.FFmpeg;

/**
 * Core aspect ratio detector class. Calculates real aspect ratio by scanning video files and detecting contained black bars.
 *
 * @author Alex Bruns, Kai Werner
 */
public abstract class ARDetectorTask extends TmmTask {

  private static final Logger LOGGER                  = LoggerFactory.getLogger(ARDetectorTask.class);

  private final Pattern       patternSampleDarkLevel  = Pattern.compile("lavfi.signalstats.YLOW=([0-9]*)");
  private final Pattern       patternSample           = Pattern
      .compile("x1:([0-9]*)\\sx2:([0-9]*)\\sy1:([0-9]*)\\sy2:([0-9]*)\\sw:([0-9]*)\\sh:([0-9]*)\\sx:");
  private final Pattern       patternDuration         = Pattern.compile("Duration:\\s(\\d\\d:\\d\\d:\\d\\d\\.\\d\\d),");
  private final Pattern       patternSAR              = Pattern.compile("SAR\\s(\\d+):(\\d+)\\s");
  private final Pattern       patternWH               = Pattern.compile(",\\s(\\d+)x(\\d+)\\s");

  private final Settings      settings                = Settings.getInstance();

  protected ArdSettings.Mode  mode                    = ArdSettings.Mode.DEFAULT;
  protected int               sampleDuration          = 2;
  protected int               sampleMinNumber         = 6;
  protected int               sampleMaxGap            = 900;
  protected float             ignoreBeginningPct      = 2f;
  protected float             ignoreEndPct            = 8f;

  protected boolean           roundUp                 = false;
  protected float             roundUpThresholdPct     = 4f;

  private float               arSecondaryDelta        = 0.15f;
  private float               plausiWidthPct          = 50f;
  private float               plausiHeightPct         = 60f;
  private float               plausiWidthDeltaPct     = 1.5f;
  private float               plausiHeightDeltaPct    = 2f;

  protected int               multiFormatMode         = 0;
  private float               multiFormatThresholdPct = 6f;

  private float               darkLevelPct            = 7f;
  private float               darkLevelMaxPct         = 13f;

  protected final List<Float> arCustomList            = new LinkedList<>();

  public ARDetectorTask(TaskType type) {
    super(TmmResourceBundle.getString("update.aspectRatio"), 100, type);
    init();
  }

  protected void init() {
    this.mode = settings.getArdMode();
    ArdSettings.SampleSetting modeSettings = settings.getArdSampleSetting(this.mode);
    if (modeSettings == null) {
      modeSettings = settings.getArdSampleSetting(ArdSettings.Mode.DEFAULT);
    }
    this.sampleDuration = modeSettings.getDuration();
    this.sampleMinNumber = modeSettings.getMinNumber();
    this.sampleMaxGap = modeSettings.getMaxGap();

    this.ignoreBeginningPct = settings.getArdIgnoreBeginningPct();
    this.ignoreEndPct = settings.getArdIgnoreEndPct();
    this.arCustomList.addAll(settings.getCustomAspectRatios().stream().map(Float::valueOf).sorted().collect(Collectors.toList()));
    this.roundUp = settings.isArdRoundUp();
    this.roundUpThresholdPct = settings.getArdRoundUpThresholdPct();
    this.multiFormatMode = settings.getArdMFMode();
    if (!ArdSettings.Mode.ACCURATE.equals(this.mode)) {
      this.multiFormatMode = 0;
    }
    this.multiFormatThresholdPct = settings.getArdMFThresholdPct();

    this.arSecondaryDelta = settings.getArdSecondaryDelta();
    this.plausiWidthPct = settings.getArdPlausiWidthPct();
    this.plausiHeightPct = settings.getArdPlausiHeightPct();
    this.plausiWidthDeltaPct = settings.getArdPlausiWidthDeltaPct();
    this.plausiHeightDeltaPct = settings.getArdPlausiHeightDeltaPct();

    this.darkLevelPct = settings.getArdDarkLevelPct();
    this.darkLevelMaxPct = settings.getArdDarkLevelMaxPct();
  }

  protected void analyze(MediaFile mediaFile) {
    analyze(mediaFile, 0);
  }

  protected void analyze(MediaFile mediaFile, int idx) {
    if (!canRun()) {
      return;
    }

    if (mediaFile.getExtension().equalsIgnoreCase("iso")) {
      LOGGER.warn("Can not execute FFMPEG on ISO files (yet)");
      return;
    }

    setTaskName(TmmResourceBundle.getString("update.aspectRatio") + ": " + mediaFile.getFilename());

    try {
      List<MediaInfoFile> relevant = MediaFileHelper.detectRelevantFiles(mediaFile);
      int totalDuration = 0;

      if (relevant.size() > 1) {
        // disc files from folder MF - remove all non video files
        for (int i = relevant.size() - 1; i >= 0; i--) {
          String ext = relevant.get(i).getFileExtension();
          // rule out non disc video files
          if (!ext.equalsIgnoreCase("vob") && !ext.equalsIgnoreCase("m2ts") && !ext.equalsIgnoreCase("evo")) {
            relevant.remove(i);
          }
        }

        // get duration for disc file - we won't have them any longer :/
        for (MediaInfoFile mif : relevant) {
          if (mif.getDuration() == 0) {
            mif.gatherMediaInformation();
          }
          totalDuration += mif.getDuration();
        }
      }

      MediaFilePosition position = getPositionInMediaFile(relevant, 0);
      if (position == null) {
        LOGGER.warn("Found no valid position for AR detection for '{}'", mediaFile.getFilename());
        return;
      }
      String result = FFmpeg.scanDarkLevel(0, position.getPath()); // first video frame (which is often black)

      VideoInfo videoInfo = new VideoInfo();
      videoInfo.bitDepth = mediaFile.getBitDepth();
      parseVideoMeta(result, mediaFile, videoInfo); // parse from FFMpeg result

      parseDarkLevel(result, videoInfo);
      if (totalDuration > 0) {
        videoInfo.duration = totalDuration;
      }

      if (videoInfo.duration <= 30) {
        LOGGER.warn("video is not long enough to be analyzed '{}' - detected length: '{}' secs", mediaFile.getFilename(), totalDuration);
        return;
      }

      int start = (int) (videoInfo.duration * this.ignoreBeginningPct / 100f);
      int end = (int) (videoInfo.duration * (1f - (this.ignoreEndPct / 100f)));

      float increment = (end - start) / (this.sampleMinNumber + 1f);
      float seconds = start + increment;

      if (videoInfo.darkLevel * 100f / Math.pow(2, videoInfo.bitDepth) > darkLevelMaxPct) {
        videoInfo.darkLevel = getDarkLevel(videoInfo);
      }

      LOGGER.debug("Filename: {}", mediaFile.getFileAsPath());
      // MF.setDuration()?
      LOGGER.trace("Metadata: Encoded size: {}x{}px, Encoded AR: {}, SAR: {}, BitDepth: {}, DarkLevel: {}, Duration: {}", videoInfo.width,
          videoInfo.height, mediaFile.getAspectRatio(), videoInfo.arSample, videoInfo.bitDepth, videoInfo.darkLevel, mediaFile.getDurationHHMMSS());

      if (increment > this.sampleMaxGap) {
        increment = this.sampleMaxGap;
        seconds = start;
      }

      int sampleCounter = 0;

      while (seconds < (end - 2)) {
        if (sampleCounter > this.sampleMinNumber * 2) {
          LOGGER.debug("we could not detect a valid result with '{}' samples", sampleCounter);
          break;
        }

        try {
          int iSec = Math.round(seconds);
          int iInc = Math.round(increment);

          if (iSec >= videoInfo.duration) {
            iSec = videoInfo.duration - this.sampleDuration;
          }
          position = getPositionInMediaFile(relevant, iSec);
          if (position != null) {
            LOGGER.trace("Scanning {} at {}s", position.getPath(), position.getPosition());
            result = FFmpeg.scanSample(position.getPosition(), sampleDuration, videoInfo.darkLevel, position.getPath());
            parseSample(result, iSec, iInc, videoInfo);
          }
          else {
            LOGGER.trace("Could not get postition {} in relevant file - maybe XML only?", iSec);
            break;
          }
        }
        catch (Exception ex) {
          LOGGER.trace("Error scanning sample - '{}'", ex.getMessage());
        }

        seconds += increment - videoInfo.sampleSkipAdjustement;
        sampleCounter++;
        if (seconds <= start) {
          seconds = Math.round(start + 0.5f * videoInfo.sampleSkipAdjustement);
        }

        if (this.cancel) {
          return;
        }

        int progress = ((int) seconds - start) * 100 / (end - start);
        publishState(idx * 100 + progress);
      }

      if (videoInfo.sampleCount == 0) {
        LOGGER.debug("No results from scanning");
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, "task.ard", "message.ard.failed", new String[] { ":", mediaFile.getFilename() }));
        return;
      }

      calculateARPrimaryAndSecondaryRaw(videoInfo);

      if (this.multiFormatMode > 0) {
        detectMultiFormat(videoInfo);
      }
      else {
        getNewHeight(videoInfo);
        videoInfo.height = videoInfo.heightPrimary;
        videoInfo.width = Math.round(videoInfo.height * videoInfo.arPrimaryRaw / videoInfo.arSample);

        LOGGER.trace("Multi format:      disabled");
      }

      // validation - only set AR when the width & height <= width/height from the mediafile itself
      if (videoInfo.width <= mediaFile.getVideoWidth() && videoInfo.height <= mediaFile.getVideoHeight()) {
        videoInfo.arPrimary = roundAR(videoInfo.arPrimaryRaw);
        mediaFile.setAspectRatio(videoInfo.arPrimary);
        LOGGER.trace("AR_Primary:        {}", String.format("%.2f", videoInfo.arPrimary));

        if (videoInfo.arSecondary > 0f && ArdSettings.Mode.ACCURATE.equals(this.mode)) {
          videoInfo.arSecondary = roundAR(videoInfo.arSecondary);
          mediaFile.setAspectRatio2(videoInfo.arSecondary);
          LOGGER.trace("AR_Secondary:      {}", String.format("%.2f", videoInfo.arSecondary));
        }

        LOGGER.info("Detected: {}x{} AR: {}{}", videoInfo.width, videoInfo.height, String.format("%.2f", videoInfo.arPrimary),
            videoInfo.arSecondary > 0f ? (" (AR2: " + String.format("%.2f", videoInfo.arSecondary)) + ")" : "");
      }
      else {
        LOGGER.debug("ARD detected a higher width/height than libmediainfo did!");
      }
    }
    catch (Exception ex) {
      LOGGER.error("Error detecting aspect ratio", ex);
      MessageManager.instance
          .pushMessage(new Message(Message.MessageLevel.ERROR, "task.ard", "message.ard.failed", new String[] { ":", mediaFile.getFilename() }));
    }
  }

  private MediaFilePosition getPositionInMediaFile(List<MediaInfoFile> mediaInfoFiles, int pos) {
    MediaFilePosition result = null;

    // we have multiple files to spread our analytics over equally
    if (mediaInfoFiles.size() > 1) {
      int totalDuration = 0;
      int duration = -1;
      for (MediaInfoFile mif : mediaInfoFiles) {
        // step out for XML based 'virtual' files
        if (!Files.exists(mif.getFileAsPath())) {
          return null;
        }
        duration = mif.getDuration();
        LOGGER.info("{}: total duration: {} - file duration: {} - filepos: {}", mif.getFilename(), totalDuration, duration, pos - totalDuration);
        if (pos <= (totalDuration + duration)) {
          result = new MediaFilePosition(mif.getFileAsPath(), pos - totalDuration);
          break;
        }
        totalDuration += duration;
      }

      if (result == null) {
        result = new MediaFilePosition(mediaInfoFiles.get(0).getFileAsPath(), duration);
      }
    }
    else {
      // single file
      MediaInfoFile mif = mediaInfoFiles.get(0);
      // step out for XML based 'virtual' files
      if (!Files.exists(mif.getFileAsPath())) {
        return null;
      }
      result = new MediaFilePosition(mif.getFileAsPath(), pos);
    }

    return result;
  }

  private int getDarkLevel(VideoInfo videoInfo) {
    return (int) (Math.round(Math.pow(2, videoInfo.bitDepth) * (darkLevelPct / 100)));
  }

  private void parseDarkLevel(String result, VideoInfo videoInfo) {
    videoInfo.darkLevel = 9999;
    if (StringUtils.isNotEmpty(result)) {
      Matcher matcher = patternSampleDarkLevel.matcher(result);
      if (matcher.find()) {
        String ylow = matcher.group(1);
        if (NumberUtils.isParsable(ylow)) {
          videoInfo.darkLevel = Integer.parseInt(ylow) + (int) (Math.pow(2, videoInfo.bitDepth - 7));
        }
      }
    }
  }

  private void parseVideoMeta(String result, MediaFile mf, VideoInfo videoInfo) {
    if (StringUtils.isNotEmpty(result)) {

      // duration
      Matcher m = patternDuration.matcher(result);
      if (m.find()) {
        try {
          DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SS");
          Date reference = dateFormat.parse("00:00:00.00");
          Date date = dateFormat.parse(m.group(1));
          videoInfo.duration = (int) ((date.getTime() - reference.getTime()) / 1000);
          LOGGER.trace("parsed duration: {}", videoInfo.duration);
        }
        catch (Exception e) {
          LOGGER.warn("Could not parse dateformat '{}'", m.group(1));
        }
      }
      else {
        videoInfo.duration = mf.getDuration();
        LOGGER.trace("MF duration: {}", videoInfo.duration);
      }

      // width/heigth
      m = patternWH.matcher(result);
      if (m.find()) {
        try {
          videoInfo.width = Integer.parseInt(m.group(1));
          videoInfo.height = Integer.parseInt(m.group(2));
          LOGGER.trace("parsed resolution: {}x{}", videoInfo.width, videoInfo.height);

        }
        catch (Exception e) {
          LOGGER.warn("Could not parse resolution '{}x{}'", m.group(1), m.group(2));
        }
      }
      else {
        videoInfo.width = mf.getVideoWidth();
        videoInfo.height = mf.getVideoHeight();
        LOGGER.trace("MF resolution: {}x{}", videoInfo.width, videoInfo.height);
      }

      // SAR
      m = patternSAR.matcher(result);
      if (m.find()) {
        try {
          float sar = Float.parseFloat(m.group(1)) / Float.parseFloat(m.group(2));
          if (sar <= 0.5f) {
            sar = 1f;
          }
          videoInfo.arSample = sar;
          LOGGER.trace("parsed SAR: {}", videoInfo.arSample);
        }
        catch (Exception e) {
          LOGGER.warn("Could not parse SAR '{}:{}'", m.group(1), m.group(2));
        }
      }
      else {
        float sar = mf.getPixelAspectRatio();
        if (sar <= 0.5f) {
          sar = 1f;
        }
        videoInfo.arSample = sar;
        LOGGER.trace("MF SAR: {}", videoInfo.arSample);
      }
    }
  }

  private void parseSample(String result, int seconds, int increment, VideoInfo videoInfo) {
    if (StringUtils.isNotEmpty(result)) {
      Matcher matcher = patternSample.matcher(result);
      if (matcher.find()) {
        LOGGER.trace("Found pattern: {}", matcher.group());
        // x1:0 x2:718 y1:73 y2:503 w:718 h:430 x:0 y:74 pts:10800 t:0.120000 crop=718:430:0:74
        // raw values without any adjustments. Those are the x1, y1 and x2, y2 co-ordinates.
        // These are the top-left and bottom-right corners of the detected crop rectangle
        int width = Integer.parseInt(matcher.group(5));
        int height = Integer.parseInt(matcher.group(6));
        int blackLeft = Integer.parseInt(matcher.group(1));
        int blackRight = Math.abs(videoInfo.width - Integer.parseInt(matcher.group(2)) - 1);
        int blackTop = Integer.parseInt(matcher.group(3));
        int blackBottom = Math.abs(videoInfo.height - Integer.parseInt(matcher.group(4)) - 1);

        // orig style (left fir debugging)
        // String sample = matcher.results()
        // .map(match -> match.group(5) + " " + match.group(6) + " " + match.group(1) + " " + match.group(2) + " " + match.group(3) + " "
        // + match.group(4))
        // .sorted(Comparator.reverseOrder())
        // .findFirst()
        // .orElse("");
        // String[] sampleData = sample.split(" ");
        // int width = Integer.parseInt(sampleData[0]);
        // int height = Integer.parseInt(sampleData[1]);
        // int blackLeft = Integer.parseInt(sampleData[2]);
        // int blackRight = videoInfo.width - Integer.parseInt(sampleData[3]) - 1;
        // int blackTop = Integer.parseInt(sampleData[4]);
        // int blackBottom = videoInfo.height - Integer.parseInt(sampleData[5]) - 1;

        if (height > 0) {
          videoInfo.arMeasured = (float) width / (float) height;
        }
        else {
          videoInfo.arMeasured = 9.99f;
        }

        videoInfo.arCalculated = (float) (Math.round(videoInfo.arMeasured * videoInfo.arSample * 10E5) / 10E5);

        String barstxt = String.format("{%4d|%4d} {%3d|%3d}", blackLeft, blackRight, blackTop, blackBottom);
        LOGGER.trace(barstxt);

        checkPlausibility(result, width, height, blackLeft, blackRight, blackTop, blackBottom, barstxt, seconds, increment, videoInfo);
      }
      else {
        // got a result - but did not match. lets see...
        LOGGER.warn("Got a result - but it did no match!");
        LOGGER.trace("TRACE: \\n{}", result);
      }
    }
    else {
      throw new RuntimeException("Sample result is empty");
    }
  }

  private void checkPlausibility(String result, int width, int height, int blackLeft, int blackRight, int blackTop, int blackBottom, String barstxt,
      int seconds, int increment, VideoInfo videoInfo) {
    if ((Math.abs(blackLeft - blackRight)) > (videoInfo.width * this.plausiWidthDeltaPct / 100d)) {
      LOGGER.debug("Analyzing {}s near {} => bars: {} => Sample skipped: More than {}% difference between left and right black bar",
          this.sampleDuration, String.format("%-8s", LocalTime.MIN.plusSeconds(seconds).toString()), barstxt, this.plausiWidthDeltaPct);
      LOGGER.trace("Plausibility error: {}", result);
      if (videoInfo.sampleSkipAdjustement == 0) {
        videoInfo.sampleSkipAdjustement = (float) increment * 1.4f;
      }
      else {
        videoInfo.sampleSkipAdjustement = 0;
      }
    }
    else if (Math.abs(blackTop - blackBottom) > (videoInfo.height * this.plausiHeightDeltaPct / 100d)) {
      LOGGER.debug("Analyzing {}s near {} => bars: {} => Sample skipped: More than {}% difference between top and bottom black bar",
          this.sampleDuration, String.format("%-8s", LocalTime.MIN.plusSeconds(seconds).toString()), barstxt, this.plausiHeightDeltaPct);
      LOGGER.trace("Plausibility error: {}", result);
      if (videoInfo.sampleSkipAdjustement == 0) {
        videoInfo.sampleSkipAdjustement = (float) increment * 1.4f;
      }
      else {
        videoInfo.sampleSkipAdjustement = 0;
      }
    }
    else if ((videoInfo.width * this.plausiWidthPct / 100d) >= width) {
      LOGGER.debug("Analyzing {}s near {} => bars: {} => Sample skipped: Cropped width ({}px) is less than {}% of video width ({}px)",
          this.sampleDuration, String.format("%-8s", LocalTime.MIN.plusSeconds(seconds).toString()), barstxt, width, this.plausiWidthPct,
          videoInfo.width);
      LOGGER.trace("Plausibility error: {}", result);
      if (videoInfo.sampleSkipAdjustement == 0) {
        videoInfo.sampleSkipAdjustement = increment * 1.4f;
      }
      else {
        videoInfo.sampleSkipAdjustement = 0;
      }
    }
    else if ((videoInfo.height * this.plausiHeightPct / 100d) >= height) {
      LOGGER.debug("Analyzing {}s near {} => bars: {} => Sample skipped: Cropped height ({}px) is less than {}% of video height ({}px)",
          this.sampleDuration, String.format("%-8s", LocalTime.MIN.plusSeconds(seconds).toString()), barstxt, height, this.plausiHeightPct,
          videoInfo.height);
      LOGGER.trace("Plausibility error: {}", result);
      if (videoInfo.sampleSkipAdjustement == 0) {
        videoInfo.sampleSkipAdjustement = increment * 1.4f;
      }
      else {
        videoInfo.sampleSkipAdjustement = 0;
      }
    }
    else {
      videoInfo.sampleSkipAdjustement = 0;
      if (!videoInfo.arMap.containsKey(videoInfo.arCalculated)) {
        videoInfo.arMap.put(videoInfo.arCalculated, 1);
      }
      else {
        videoInfo.arMap.put(videoInfo.arCalculated, videoInfo.arMap.get(videoInfo.arCalculated) + 1);
      }
      if (!videoInfo.heightMap.containsKey(height)) {
        videoInfo.heightMap.put(height, 1);
      }
      else {
        videoInfo.heightMap.put(height, videoInfo.heightMap.get(height) + 1);
      }
      if (!videoInfo.widthMap.containsKey(width)) {
        videoInfo.widthMap.put(width, 1);
      }
      else {
        videoInfo.widthMap.put(width, videoInfo.widthMap.get(width) + 1);
      }
      videoInfo.sampleCount++;
      LOGGER.debug("Analyzing {}s near {} => bars: {} crop: {}x{} ({}) * SAR => AR_Calculated = {}", this.sampleDuration,
          String.format("%-8s", LocalTime.MIN.plusSeconds(seconds).toString()), barstxt, width, height, String.format("%.5f", videoInfo.arMeasured),
          String.format("%.5f", videoInfo.arCalculated));
    }
  }

  void calculateARPrimaryAndSecondaryRaw(VideoInfo videoInfo) {
    videoInfo.arPrimaryRaw = videoInfo.arMap.entrySet()
        .stream()
        .sorted(Map.Entry.<Float, Integer> comparingByValue().reversed())
        .findFirst()
        .map(Map.Entry::getKey)
        .orElse(0f);

    videoInfo.arSecondary = videoInfo.arMap.entrySet()
        .stream()
        .filter(entry -> (entry.getKey() <= (videoInfo.arPrimaryRaw - this.arSecondaryDelta)
            || entry.getKey() >= (videoInfo.arPrimaryRaw + this.arSecondaryDelta)))
        .sorted(Map.Entry.<Float, Integer> comparingByValue().reversed())
        .findFirst()
        .map(Map.Entry::getKey)
        .orElse(0f);

    if (videoInfo.arMap.size() == 0)
      videoInfo.arSecondary = 0f;

    int arPrimarySum = videoInfo.arMap.entrySet()
        .stream()
        .filter(entry -> (entry.getKey() >= (videoInfo.arPrimaryRaw - this.arSecondaryDelta / 2)
            && entry.getKey() <= (videoInfo.arPrimaryRaw + this.arSecondaryDelta / 2)))
        .map(Map.Entry::getValue)
        .reduce(0, Integer::sum);
    int arSecondarySum = videoInfo.arMap.entrySet()
        .stream()
        .filter(entry -> (entry.getKey() >= (videoInfo.arSecondary - this.arSecondaryDelta / 2)
            && entry.getKey() <= (videoInfo.arSecondary + this.arSecondaryDelta / 2)))
        .map(Map.Entry::getValue)
        .reduce(0, Integer::sum);

    // If sums are equal arPrimaryRaw and arSecondary need to be swapped because sort order of heightPrimary is different
    if (arPrimarySum == arSecondarySum) {
      float tmp = videoInfo.arPrimaryRaw;
      videoInfo.arPrimaryRaw = videoInfo.arSecondary;
      videoInfo.arSecondary = tmp;
    }

    float arPrimaryPct = arPrimarySum * 100f / videoInfo.sampleCount;
    videoInfo.arSecondaryPct = arSecondarySum * 100f / videoInfo.sampleCount;
    float arOtherPct = (videoInfo.sampleCount - arPrimarySum - arSecondarySum) * 100f / videoInfo.sampleCount;

    LOGGER.debug("AR_PrimaryRaw:     {}, {}% of samples are within ±{}    Aspect ratio (AR) detected in most of the analyzed samples",
        String.format("%7.5f", videoInfo.arPrimaryRaw), String.format("%6.2f", arPrimaryPct), this.arSecondaryDelta);
    LOGGER.debug("AR_SecondaryRaw:   {}, {}% of samples are within ±{}    Second most frequent AR (multi format video likely at higher values)",
        String.format("%7.5f", videoInfo.arSecondary), String.format("%6.2f", videoInfo.arSecondaryPct), this.arSecondaryDelta);
    LOGGER.debug("Other ARs:                  {}% of samples                     Other ARs found, high value means bad detection",
        String.format("%6.2f", arOtherPct));
  }

  private void detectMultiFormat(VideoInfo videoInfo) {
    if (videoInfo.arSecondaryPct >= this.multiFormatThresholdPct) {
      LOGGER.debug(
          "Multi format:      yes                                             AR_Secondary ({}% of samples) >= MFV Detection Threshold ({}% of samples)",
          String.format("%.2f", videoInfo.arSecondaryPct), String.format("%.2f", this.multiFormatThresholdPct));

      if (multiFormatMode == 1) {
        float tmp = Math.min(videoInfo.arPrimaryRaw, videoInfo.arSecondary);
        videoInfo.arSecondary = Math.max(videoInfo.arPrimaryRaw, videoInfo.arSecondary);
        videoInfo.arPrimaryRaw = tmp;
        getNewHeight(videoInfo);
        videoInfo.height = Math.max(videoInfo.heightPrimary, videoInfo.heightSecondary);
        videoInfo.width = Math.round(videoInfo.height * videoInfo.arPrimaryRaw / videoInfo.arSample);

        LOGGER.debug("MFV detected, arPrimaryRaw is higher AR: {} height: {}", String.format("%.5f", videoInfo.arPrimaryRaw), videoInfo.height);
        LOGGER.debug("MFV detected, arSecondary is wider AR: {}", String.format("%.5f", videoInfo.arSecondary));
      }
      else if (multiFormatMode == 2) {
        float tmp = Math.max(videoInfo.arPrimaryRaw, videoInfo.arSecondary);
        videoInfo.arSecondary = Math.min(videoInfo.arPrimaryRaw, videoInfo.arSecondary);
        videoInfo.arPrimaryRaw = tmp;
        getNewHeight(videoInfo);
        videoInfo.height = Math.min(videoInfo.heightPrimary, videoInfo.heightSecondary);
        videoInfo.width = Math.round(videoInfo.height * videoInfo.arPrimaryRaw / videoInfo.arSample);

        LOGGER.debug("MFV detected, arPrimaryRaw is wider AR: {} height: {}", String.format("%.5f", videoInfo.arPrimaryRaw), videoInfo.height);
        LOGGER.debug("MFV detected, arSecondary is higher AR: {}", String.format("%.5f", videoInfo.arSecondary));
      }
    }
    else {
      getNewHeight(videoInfo);
      videoInfo.height = videoInfo.heightPrimary;
      videoInfo.width = Math.round(videoInfo.height * videoInfo.arPrimaryRaw / videoInfo.arSample);
      videoInfo.arSecondary = 0f;

      LOGGER.debug(
          "Multi format:      no                                              AR_Secondary ({}% of samples) < MFV Detection Threshold ({}% of samples)",
          String.format("%.2f", videoInfo.arSecondaryPct), String.format("%.2f", this.multiFormatThresholdPct));
    }
  }

  float roundAR(float ar) {
    float rounded = 999f;
    boolean roundNearest = false;

    if (this.arCustomList.isEmpty()) {
      LOGGER.info("Aspect ratio list is empty. Round to two decimal points ");
      return Math.round(ar * 100f) / 100f;
    }

    if (this.roundUp) {
      // Round up to next wider Aspect Ratio from list if delta is greater than threshold
      float arDelta = 999f;

      for (Float arProvided : this.arCustomList) {
        if (Math.abs(arProvided - ar) <= (this.roundUpThresholdPct / 100f)) {
          rounded = roundAR_nearest(ar);
          roundNearest = true;
          break;
        }
        if ((arDelta > (arProvided - ar)) && ((arProvided - ar) >= 0)) {
          arDelta = arProvided - ar;
          rounded = arProvided;
        }
      }
      if (rounded == 999f)
        rounded = this.arCustomList.get(this.arCustomList.size() - 1);
    }
    else {
      rounded = roundAR_nearest(ar);
      roundNearest = true;
    }
    if (!roundNearest)
      LOGGER.debug("Rounded to next wider Aspect Ratio from list");

    return rounded;
  }

  private float roundAR_nearest(float ar) {
    float rounded = 999f;

    // Round to nearest Aspect Ratio from list
    if (this.arCustomList.size() == 1) {
      return this.arCustomList.get(0);
    }
    else {
      for (int idx = 0; idx < this.arCustomList.size() - 1; idx++) {
        float maxAr = (float) Math.sqrt(this.arCustomList.get(idx).doubleValue() * this.arCustomList.get(idx + 1).doubleValue());
        if (ar < maxAr) {
          rounded = this.arCustomList.get(idx);
          break;
        }
      }
    }

    if (rounded == 999f)
      rounded = this.arCustomList.get(this.arCustomList.size() - 1);
    LOGGER.debug("Rounded to nearest Aspect Ratio from list");

    return rounded;
  }

  private void getNewHeight(VideoInfo videoInfo) {
    int widthPrimary = videoInfo.widthMap.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(videoInfo.width);
    videoInfo.heightPrimary = videoInfo.heightMap.entrySet()
        .stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(videoInfo.height);

    videoInfo.heightSecondary = videoInfo.heightMap.entrySet()
        .stream()
        .filter(entry -> (entry.getKey() >= widthPrimary / (videoInfo.arPrimaryRaw + this.arSecondaryDelta)
            && entry.getKey() <= widthPrimary / (videoInfo.arPrimaryRaw - this.arSecondaryDelta)))
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(videoInfo.heightPrimary);
  }

  protected boolean canRun() {
    if (!FFmpeg.isAvailable()) {
      LOGGER.warn("Would have executed aspect ration detection - unfortunately, FFMpeg could not be found.");
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "task.ard", "message.ard.ffmpegmissing"));
      return false;
    }

    // also check ARD settings
    if (this.sampleMinNumber == 0 || this.ignoreBeginningPct + this.ignoreEndPct > 90) {
      LOGGER.warn("Could not start aspect ratio detection - invalid parameters");
      return false;
    }

    return true;
  }

  static class VideoInfo {
    int                   width;
    int                   height;
    int                   duration;
    int                   bitDepth;
    int                   darkLevel;

    int                   sampleCount           = 0;

    float                 arSample              = 0f;
    float                 arMeasured            = 0f;
    float                 arCalculated          = 0f;
    float                 arPrimary             = 0f;
    float                 arPrimaryRaw          = 0f;
    float                 arSecondary           = 0f;
    float                 arSecondaryPct        = 0f;

    float                 sampleSkipAdjustement = 0f;

    int                   heightPrimary;
    int                   heightSecondary;

    Map<Float, Integer>   arMap                 = new HashMap<>();
    Map<Integer, Integer> widthMap              = new HashMap<>();
    Map<Integer, Integer> heightMap             = new HashMap<>();
  }

  static class MediaFilePosition {

    private final Path path;
    private final int  position;

    public MediaFilePosition(Path path, int position) {
      this.path = path;
      this.position = position;
    }

    public Path getPath() {
      return path;
    }

    public int getPosition() {
      return position;
    }
  }
}
