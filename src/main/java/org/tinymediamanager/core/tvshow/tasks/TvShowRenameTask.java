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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.TvShowRenamer;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;

/**
 * The class MovieRenameTask. rename all chosen movies
 * 
 * @author Manuel Laggner
 */
public class TvShowRenameTask extends TmmThreadPool {
  private static final Logger       LOGGER           = LoggerFactory.getLogger(TvShowRenameTask.class);

  private final List<TvShow>        tvShowsToRename  = new ArrayList<>();
  private final List<TvShowEpisode> episodesToRename = new ArrayList<>();

  /**
   * Rename just the given {@link TvShow} root (and {@link org.tinymediamanager.core.entities.MediaFile}s)
   *
   * @param tvShowToRename
   *          the {@link TvShow} to rename
   */
  public TvShowRenameTask(TvShow tvShowToRename) {
    this(Collections.singletonList(tvShowToRename), null);
  }

  /**
   * Rename just the given {@link TvShow} roots (and {@link org.tinymediamanager.core.entities.MediaFile}s)
   *
   * @param tvShowsToRename
   *          the {@link TvShow}s to rename
   */
  public TvShowRenameTask(Collection<TvShow> tvShowsToRename) {
    this(tvShowsToRename, null);
  }

  /**
   * Rename {@link TvShow}s and {@link TvShowEpisode}s together
   *
   * @param tvShowsToRename
   *          the {@link TvShow}s to rename (only TV show MFs and root folder)
   * @param episodesToRename
   *          the {@link TvShowEpisode}s to rename
   */
  public TvShowRenameTask(Collection<TvShow> tvShowsToRename, Collection<TvShowEpisode> episodesToRename) {
    super(TmmResourceBundle.getString("tvshow.rename"));
    if (tvShowsToRename != null) {
      this.tvShowsToRename.addAll(tvShowsToRename);
    }

    if (episodesToRename != null) {
      this.episodesToRename.addAll(episodesToRename);
    }
  }

  @Override
  protected void doInBackground() {
    try {
      start();
      initThreadPool(1, "rename");

      // 1. episodes first (to get the right season folders for moving season artwork)
      for (TvShowEpisode tvEpisodesToRename : episodesToRename) {
        if (cancel) {
          break;
        }
        submitTask(new RenameEpisodeTask(tvEpisodesToRename));
      }

      waitForCompletionOrCancel();
      if (cancel) {
        return;
      }

      // 2. rename TV show root
      for (TvShow tvShow : tvShowsToRename) {
        TvShowRenamer.renameTvShow(tvShow); // rename root and artwork and update ShowMFs
      }

      LOGGER.info("Done renaming TV shows)");
    }
    catch (Exception e) {
      LOGGER.error("Thread crashed", e);
      MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "Settings.renamer", "message.renamer.threadcrashed"));
    }
  }

  /**
   * ThreadpoolWorker to work off ONE episode
   */
  private static class RenameEpisodeTask implements Callable<Object> {
    private final TvShowEpisode episode;

    public RenameEpisodeTask(TvShowEpisode episode) {
      this.episode = episode;
    }

    @Override
    public String call() {
      TvShowRenamer.renameEpisode(episode);
      return episode.getTitle();
    }
  }

  @Override
  public void callback(Object obj) {
    publishState((String) obj, progressDone);
  }
}
