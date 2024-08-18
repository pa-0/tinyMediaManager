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

import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.entities.MediaStreamInfo;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTaskManager;

/**
 * The Class MediaFileInformationFetcherTask.
 * 
 * @author Manuel Laggner
 */
public class MediaFileInformationFetcherTask implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(MediaFileInformationFetcherTask.class);

  protected final MediaFile   mediaFile;
  protected final MediaEntity mediaEntity;
  private final long          uniqueId;
  private final boolean       forceUpdate;

  /**
   * Instantiates a new media file information fetcher task.
   * 
   * @param mediaFile
   *          the media files
   * @param mediaEntity
   *          the media entity
   * @param forceUpdate
   *          force an update
   */
  public MediaFileInformationFetcherTask(MediaFile mediaFile, MediaEntity mediaEntity, boolean forceUpdate) {
    this.mediaFile = mediaFile;
    this.mediaEntity = mediaEntity;
    this.forceUpdate = forceUpdate;
    this.uniqueId = TmmTaskManager.getInstance().GLOB_THRD_CNT.incrementAndGet();
  }

  @Override
  public void run() {
    // try/catch block in the root of the thread to log crashes
    try {
      String name = Thread.currentThread().getName();
      if (!name.contains("-G")) {
        name = name + "-G0";
      }
      name = name.replaceAll("\\-G\\d+", "-G" + uniqueId);
      Thread.currentThread().setName(name);

      mediaFile.gatherMediaInformation(forceUpdate);

      if (mediaFile.getType() == MediaFileType.SUBTITLE || mediaFile.getType() == MediaFileType.AUDIO) {
        // also re-evaluate language from NAME
        MediaFile mainVideoFile = mediaEntity.getMainFile();

        boolean goOn = true;
        if (mediaEntity instanceof Movie && ((Movie) mediaEntity).isMultiMovieDir()) {
          if (StringUtils.isNotBlank(mainVideoFile.getBasename()) && mediaFile.getFilename().startsWith(mainVideoFile.getBasename())) {
            // MMD movie MUST start with basename, for all others or TV this is fine w/o
          }
          else {
            // uh-oh - MMD but filename does not match
            goOn = false;
          }
        }

        if (goOn) {
          MediaStreamInfo info = MediaFileHelper.gatherLanguageInformation(mediaFile.getBasename(), mainVideoFile.getBasename());
          if (mediaFile.getType() == MediaFileType.SUBTITLE && !mediaFile.getSubtitles().isEmpty()) {
            MediaFileSubtitle sub = mediaFile.getSubtitles().get(0);
            // if we have detected a locale (which is more specific than language alone) us this
            if (sub.getLanguage().isEmpty() || info.getLanguage().matches("[a-zA-Z][a-zA-Z][_-].*")) {
              sub.setLanguage(info.getLanguage());
            }
            sub.setTitle(info.getTitle());
            sub.set(info.getFlags());
          }
          else if (mediaFile.getType() == MediaFileType.AUDIO && mediaFile.getAudioChannels().isEmpty()) {
            MediaFileAudioStream audio = mediaFile.getAudioStreams().get(0);
            // if we have detected a locale (which is more specific than language alone) us this
            if (StringUtils.isBlank(audio.getLanguage()) || info.getLanguage().matches("[a-zA-Z][a-zA-Z][_-].*")) {
              audio.setLanguage(info.getLanguage());
            }
            if (StringUtils.isBlank(audio.getTitle())) {
              audio.setTitle(info.getTitle());
            }
            audio.set(info.getFlags());
          }
        }
      }

      if (mediaFile.hasSubtitles()) {
        mediaEntity.firePropertyChange("hasSubtitles", false, true);
      }

      // add the -mediainfo.xml if it has been written
      if (Settings.getInstance().isWriteMediaInfoXml()) {
        Path xmlFile = Paths.get(mediaFile.getPath(), FilenameUtils.getBaseName(mediaFile.getFilename()) + "-mediainfo.xml");
        if (Files.exists(xmlFile)) {
          MediaFile xmlMf = new MediaFile(xmlFile);
          xmlMf.gatherMediaInformation();
          mediaEntity.addToMediaFiles(xmlMf);
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("Thread crashed: ", e);
      MessageManager.instance.pushMessage(
          new Message(MessageLevel.ERROR, "MediaInformation", "message.mediainfo.threadcrashed", new String[] { ":", e.getLocalizedMessage() }));
    }

    if (mediaEntity != null) {
      mediaEntity.callbackForGatheredMediainformation(mediaFile);
      mediaEntity.saveToDb();
      mediaEntity.firePropertyChange(MEDIA_INFORMATION, false, true);
    }

    callbackForGatheredMediainformation();
  }

  /**
   * a callback which could be called after finishing this task
   */
  public void callbackForGatheredMediainformation() {
    // can be overwritten to include coding
  }
}
