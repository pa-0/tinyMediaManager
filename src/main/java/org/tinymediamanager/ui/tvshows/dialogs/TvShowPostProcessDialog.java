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
package org.tinymediamanager.ui.tvshows.dialogs;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.PostProcess;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.ui.dialogs.PostProcessDialog;
import org.tinymediamanager.ui.dialogs.SettingsDialog;

/**
 * the class {@link TvShowPostProcessDialog} is used to maintain post process actions for movies
 *
 * @author Wolfgang Janes
 */
public class TvShowPostProcessDialog extends PostProcessDialog {

  enum Type {
    TV_SHOW,
    EPISODE
  }

  private final Type type;

  private TvShowPostProcessDialog(Type type) {
    super();
    this.type = type;
  }

  public static void showTvShowPostProcessDialog() {
    showTvShowPostProcessDialog(null);
  }

  public static void showTvShowPostProcessDialog(PostProcess process) {
    PostProcessDialog dialog = new TvShowPostProcessDialog(Type.TV_SHOW);
    dialog.setProcess(process);
    dialog.pack();
    dialog.setLocationRelativeTo(SettingsDialog.getInstance());
    dialog.setVisible(true);
  }

  public static void showEpisodePostProcessDialog() {
    showEpisodePostProcessDialog(null);
  }

  public static void showEpisodePostProcessDialog(PostProcess process) {
    PostProcessDialog dialog = new TvShowPostProcessDialog(Type.EPISODE);
    dialog.setProcess(process);
    dialog.pack();
    dialog.setLocationRelativeTo(SettingsDialog.getInstance());
    dialog.setVisible(true);
  }

  @Override
  protected void save() {
    if (StringUtils.isBlank(tfProcessName.getText()) || (StringUtils.isBlank(tfCommand.getText()) && StringUtils.isBlank(tfPath.getText()))) {

      JOptionPane.showMessageDialog(TvShowPostProcessDialog.this, TmmResourceBundle.getString("message.missingitems"));
      return;
    }

    // create a new post-process
    if (process == null) {
      process = new PostProcess();
      switch (type) {
        case TV_SHOW:
          TvShowModuleManager.getInstance().getSettings().addPostProcessTvShow(process);
          break;

        case EPISODE:
          TvShowModuleManager.getInstance().getSettings().addPostProcessEpisode(process);
          break;

        default:
          return;
      }
    }

    process.setName(tfProcessName.getText());
    process.setCommand(tfCommand.getText());
    process.setPath(tfPath.getText());

    TvShowModuleManager.getInstance().getSettings().forceSaveSettings();

    firePropertyChange("postProcessTvShow", null, process);
    setVisible(false);
  }
}
