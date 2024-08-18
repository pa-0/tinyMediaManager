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
package org.tinymediamanager.core.movie.tasks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.TrailerQuality;
import org.tinymediamanager.core.TrailerSources;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.filenaming.MovieTrailerNaming;
import org.tinymediamanager.core.tasks.TrailerDownloadTask;
import org.tinymediamanager.core.tasks.YTDownloadTask;
import org.tinymediamanager.core.threading.TmmTask;

/**
 * The class {@link MovieTrailerDownloadTask} is used to download the "best" trailer for a movie
 * 
 * @author Manuel Laggner
 */
public class MovieTrailerDownloadTask extends TmmTask {
  private static final Logger            LOGGER       = LoggerFactory.getLogger(MovieTrailerDownloadTask.class);

  private final Movie                    movie;
  private final List<MovieTrailerNaming> trailernames = new ArrayList<>();
  private final TrailerQuality           desiredQuality;
  private final TrailerSources           desiredSource;

  private TmmTask                        task;

  public MovieTrailerDownloadTask(Movie movie) {
    super(TmmResourceBundle.getString("trailer.download") + " - " + movie.getTitle(), 100, TaskType.BACKGROUND_TASK);

    this.movie = movie;

    // store the trailer settings at the start of this task (to do not suffer from changes while the task is running)
    if (movie.isMultiMovieDir()) {
      // in a MMD we can only use this naming
      trailernames.add(MovieTrailerNaming.FILENAME_TRAILER);
    }
    else {
      trailernames.addAll(MovieModuleManager.getInstance().getSettings().getTrailerFilenames());
    }

    desiredSource = TrailerSources.YOUTUBE;
    desiredQuality = MovieModuleManager.getInstance().getSettings().getTrailerQuality();
  }

  @Override
  protected void doInBackground() {
    if (!isFeatureEnabled()) {
      return;
    }

    Set<MediaTrailer> trailers = new LinkedHashSet<>();

    // prepare the list of desired trailers
    // search for quality and provider
    for (MediaTrailer trailer : movie.getTrailer()) {
      if (desiredQuality.containsQuality(trailer.getQuality()) && desiredSource.containsSource(trailer.getProvider())) {
        trailers.add(trailer);
      }
    }

    // search for quality
    for (MediaTrailer trailer : movie.getTrailer()) {
      if (desiredQuality.containsQuality(trailer.getQuality())) {
        trailers.add(trailer);
      }
    }

    // add the rest
    trailers.addAll(movie.getTrailer());

    if (trailers.isEmpty()) {
      LOGGER.warn("no trailers for '{}' available", movie.getTitle());
      return;
    }

    // now try to download the trailers until we get one ;)
    LOGGER.info("downloading trailer for '{}'", movie.getTitle());
    for (MediaTrailer trailer : trailers) {
      String url = trailer.getUrl();

      if (StringUtils.isBlank(url)) {
        // no url - no download
        continue;
      }

      try {
        LOGGER.debug("try to download trailer '{}'", url);

        Matcher matcher = Utils.YOUTUBE_PATTERN.matcher(url);
        if (matcher.matches()) {
          task = new YTDownloadTask(trailer, desiredQuality, MovieModuleManager.getInstance().getSettings().isUseYtDlp()) {
            @Override
            protected Path getDestinationWoExtension() {
              return getDestination();
            }

            @Override
            protected MediaEntity getMediaEntityToAdd() {
              return movie;
            }
          };
        }
        else {
          task = new TrailerDownloadTask(trailer) {
            @Override
            protected Path getDestinationWoExtension() {
              return getDestination();
            }

            @Override
            protected MediaEntity getMediaEntityToAdd() {
              return movie;
            }
          };
        }

        if (cancel) {
          return;
        }

        // delegate events
        task.addListener(taskEvent -> {
          setProgressDone(taskEvent.getProgressDone());
          setTaskDescription(taskEvent.getTaskDescription());
          setWorkUnits(taskEvent.getWorkUnits());
          informListeners();
        });

        task.run();

        // check if the task has been finishes successfully
        if (task.getState() == TaskState.FINISHED || task.getState() == TaskState.CANCELLED) {
          break;
        }
      }
      catch (Exception e) {
        LOGGER.debug("could download trailer - {}", e.getMessage());
      }
    }
  }

  @Override
  public void cancel() {
    super.cancel();
    if (task != null) {
      task.cancel();
    }
  }

  protected Path getDestination() {
    // hmm.. at the moment we can only download ONE trailer, so both patterns won't work
    // just take the first one (or the default if there is no entry whyever)
    String filename;
    if (!trailernames.isEmpty()) {
      filename = movie.getTrailerFilename(trailernames.get(0));
    }
    else {
      filename = movie.getTrailerFilename(MovieTrailerNaming.FILENAME_TRAILER);
    }

    // DVD/BluRay folders can have trailers within!
    // check for discFolders and/or files
    final Path outputFolder;
    if (movie.isDisc() && MovieModuleManager.getInstance().getSettings().isTrailerDiscFolderInside()) {
      if (MediaFileHelper.isDiscFolder(movie.getMainFile().getFilename())) {
        outputFolder = movie.getMainFile().getFileAsPath();
      }
      else {
        outputFolder = movie.getPathNIO(); // not a virtual "MF folder"? use default
      }
    }
    else {
      outputFolder = movie.getPathNIO(); // default
    }

    return outputFolder.resolve(filename);
  }
}
