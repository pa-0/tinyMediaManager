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
package org.tinymediamanager.thirdparty.trakttv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTask;

/**
 * Clear your TV show data from trakt.tv
 * 
 * @author Manuel Laggner
 */
public class TvShowClearTraktTvTask extends TmmTask {
  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowClearTraktTvTask.class);

  public TvShowClearTraktTvTask() {
    super(TmmResourceBundle.getString("trakt.clear"), 0, TaskType.BACKGROUND_TASK);
  }

  @Override
  protected void doInBackground() {
    if (!isFeatureEnabled()) {
      return;
    }

    TraktTv traktTV = TraktTv.getInstance();

    publishState(TmmResourceBundle.getString("trakt.clear.tvshows"), 0);
    try {
      traktTV.clearTraktTvShows();
    }
    catch (Exception e) {
      LOGGER.error("Could not sync to trakt - '{}'", e.getMessage());
    }
  }
}
