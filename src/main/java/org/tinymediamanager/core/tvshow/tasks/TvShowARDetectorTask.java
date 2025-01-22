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
package org.tinymediamanager.core.tvshow.tasks;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tasks.ARDetectorTask;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;

/**
 * the class {@link TvShowARDetectorTask} is used to detect aspect ratios for episodes
 *
 * @author Alex Bruns, Kai Werner
 */
public class TvShowARDetectorTask extends ARDetectorTask {

  private static final Logger       LOGGER = LoggerFactory.getLogger(TvShowARDetectorTask.class);

  private final List<TvShowEpisode> episodes;

  public TvShowARDetectorTask(List<TvShowEpisode> episodes) {
    super(TaskType.MAIN_TASK);
    this.episodes = episodes;
  }

  @Override
  protected void doInBackground() {
    if (!canRun()) {
      return;
    }

    int filesTotal = this.episodes.stream().map(episode -> episode.getMediaFiles(MediaFileType.VIDEO).size()).reduce(Integer::sum).orElse(0);

    if (filesTotal > 0) {
      setWorkUnits(filesTotal * 100);
    }

    int idx = 0;
    for (TvShowEpisode episode : this.episodes) {
      for (MediaFile mediaFile : episode.getMediaFiles(MediaFileType.VIDEO)) {
        if (cancel) {
          break;
        }
        analyze(mediaFile, idx++);
      }
      if (cancel) {
        LOGGER.info("Abort queue");
        break;
      }
      episode.saveToDb();
      episode.writeNFO();
    }
  }
}
