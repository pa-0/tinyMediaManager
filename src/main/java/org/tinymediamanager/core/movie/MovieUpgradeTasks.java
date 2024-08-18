/*
 * Copyright 2012 - 2020 Manuel Laggner
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
package org.tinymediamanager.core.movie;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.UpgradeTasks;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;

/**
 * the class {@link MovieUpgradeTasks} is used to perform actions on {@link Movie}s and {@link MovieSet}s
 * 
 * @author Manuel Laggner
 */
public class MovieUpgradeTasks extends UpgradeTasks {
  private static final Logger LOGGER = LoggerFactory.getLogger(MovieUpgradeTasks.class);

  public MovieUpgradeTasks() {
    super();
  }

  /**
   * Each DB version can only be executed once!<br>
   * Do not make changes to existing versions, use a new number!
   */
  @Override
  public void performDbUpgrades() {
    MovieModuleManager module = MovieModuleManager.getInstance();
    MovieList movieList = module.getMovieList();
    if (module.getDbVersion() == 0) {
      module.setDbVersion(5000);
    }

    LOGGER.info("Current movie DB version: {}", module.getDbVersion());

    if (module.getDbVersion() < 5001) {
      LOGGER.info("performing upgrade to ver: {}", 5001);
      for (Movie movie : movieList.getMovies()) {
        // migrate logo to clearlogo
        for (MediaFile mf : movie.getMediaFiles(MediaFileType.LOGO)) {
          // remove
          movie.removeFromMediaFiles(mf);
          // change type
          mf.setType(MediaFileType.CLEARLOGO);
          // and add ad the end
          movie.addToMediaFiles(mf);
        }

        String logoUrl = movie.getArtworkUrl(MediaFileType.LOGO);
        if (StringUtils.isNotBlank(logoUrl)) {
          movie.removeArtworkUrl(MediaFileType.LOGO);
          String clearlogoUrl = movie.getArtworkUrl(MediaFileType.CLEARLOGO);
          if (StringUtils.isBlank(clearlogoUrl)) {
            movie.setArtworkUrl(logoUrl, MediaFileType.CLEARLOGO);
          }
        }
        registerForSaving(movie);
      }
      module.setDbVersion(5001);
    }

    // fix ratings
    if (module.getDbVersion() < 5002) {
      LOGGER.info("performing upgrade to ver: {}", 5002);
      for (Movie movie : movieList.getMovies()) {
        if (fixRatings(movie)) {
          registerForSaving(movie);
        }
      }
      module.setDbVersion(5002);
    }

    saveAll();
  }

  @Override
  protected void saveAll() {
    for (MediaEntity mediaEntity : entitiesToSave) {
      if (mediaEntity instanceof Movie movie) {
        MovieModuleManager.getInstance().persistMovie(movie);
      }
      else if (mediaEntity instanceof MovieSet movieSet) {
        MovieModuleManager.getInstance().persistMovieSet(movieSet);
      }
    }
  }
}
