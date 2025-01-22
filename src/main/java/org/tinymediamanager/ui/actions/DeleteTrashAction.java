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
package org.tinymediamanager.ui.actions;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;

/**
 * The class {@link DeleteTrashAction} is used to delete the trash (.deletedByTMM) for all connected data sources
 * 
 * @author Manuel Laggner
 */
public class DeleteTrashAction extends TmmAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(DeleteTrashAction.class);

  public DeleteTrashAction() {
    putValue(NAME, TmmResourceBundle.getString("tmm.deletetrash"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tmm.deletetrash.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    for (String ds : MovieModuleManager.getInstance().getSettings().getMovieDataSource()) {
      Path file = Paths.get(ds, Constants.DS_TRASH_FOLDER);
      try {
        Utils.deleteDirectoryRecursive(file);
      }
      catch (Exception ex) {
        LOGGER.error("Could not delete folder '{}' - '{}'", file.toAbsolutePath(), ex.getMessage());
      }
    }

    for (String ds : TvShowModuleManager.getInstance().getSettings().getTvShowDataSource()) {
      Path file = Paths.get(ds, Constants.DS_TRASH_FOLDER);
      try {
        Utils.deleteDirectoryRecursive(file);
      }
      catch (Exception ex) {
        LOGGER.error("Could not delete folder '{}' - '{}'", file.toAbsolutePath(), ex.getMessage());
      }
    }
  }
}
