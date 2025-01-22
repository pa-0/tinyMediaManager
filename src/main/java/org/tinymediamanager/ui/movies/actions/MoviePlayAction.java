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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

/**
 * {@link MoviePlayAction} - start playback of the selected movie
 *
 * @author Manuel Laggner
 */
public class MoviePlayAction extends TmmAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(MoviePlayAction.class);

  public MoviePlayAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.play"));
    putValue(SMALL_ICON, IconManager.PLAY);
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.play.desc"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    MediaFile mf = MovieUIModule.getInstance().getSelectionModel().getSelectedMovie().getMainVideoFile();
    if (StringUtils.isNotBlank(mf.getFilename())) {
      try {
        TmmUIHelper.openFile(MediaFileHelper.getMainVideoFile(mf));
      }
      catch (Exception ex) {
        LOGGER.error("open file", ex);
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, mf, "message.erroropenfile", new String[] { ":", ex.getLocalizedMessage() }));
      }
    }
  }
}
