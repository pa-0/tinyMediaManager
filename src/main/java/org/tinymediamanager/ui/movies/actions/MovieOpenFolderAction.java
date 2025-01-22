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
package org.tinymediamanager.ui.movies.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

/**
 * {@link MovieOpenFolderAction} - open the folder containing the selected movie
 *
 * @author Manuel Laggner
 */
public class MovieOpenFolderAction extends TmmAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(MovieOpenFolderAction.class);

  public MovieOpenFolderAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.openfolder"));
    putValue(SMALL_ICON, IconManager.FOLDER_OPEN);
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.openfolder.desc"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    Movie movie = MovieUIModule.getInstance().getSelectionModel().getSelectedMovie();
    TmmUIHelper.openFolder(movie.getPathNIO());
  }
}
