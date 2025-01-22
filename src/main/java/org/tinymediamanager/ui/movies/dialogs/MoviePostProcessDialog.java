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
package org.tinymediamanager.ui.movies.dialogs;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.PostProcess;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.ui.dialogs.PostProcessDialog;

/**
 * the class {@link MoviePostProcessDialog} is used to maintain post process actions for movies
 *
 * @author Wolfgang Janes
 */
public class MoviePostProcessDialog extends PostProcessDialog {

  public MoviePostProcessDialog() {
    super();
  }

  @Override
  public void save() {
    if (StringUtils.isBlank(tfProcessName.getText()) || (StringUtils.isBlank(tfCommand.getText()) && StringUtils.isBlank(tfPath.getText()))) {

      JOptionPane.showMessageDialog(MoviePostProcessDialog.this, TmmResourceBundle.getString("message.missingitems"));
      return;
    }

    if (process == null) {
      // create a new post-process
      process = new PostProcess();
      MovieModuleManager.getInstance().getSettings().addPostProcess(process);
    }

    process.setName(tfProcessName.getText());
    process.setCommand(tfCommand.getText());
    process.setPath(tfPath.getText());

    MovieModuleManager.getInstance().getSettings().forceSaveSettings();

    firePropertyChange("postProcess", null, process);
    setVisible(false);
  }
}
