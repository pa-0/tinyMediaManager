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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.TmmOsUtils;
import org.tinymediamanager.core.TmmResourceBundle;

/**
 * The {@link CreateDesktopFileAction} is used the create a .desktop file in ~/.local/share/applications on linux
 * 
 * @author Manuel Laggner
 */
public class CreateDesktopFileAction extends TmmAction {
  public CreateDesktopFileAction() {
    putValue(NAME, TmmResourceBundle.getString("tmm.createdesktopentry"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tmm.createdesktopentry.hint"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    // create in ~/.local/share/applications
    {
      String currentUsersHomeDir = System.getProperty("user.home");
      if (StringUtils.isNotBlank(currentUsersHomeDir)) {
        // build the path to the
        Path desktopFile = Paths.get(currentUsersHomeDir, ".local", "share", "applications", "tinyMediaManager.desktop").toAbsolutePath();
        if (Files.isWritable(desktopFile.getParent())) {
          TmmOsUtils.createDesktopFileForLinux(desktopFile.toFile());
        }
      }
    }

    // create in tmm folder
    {
      Path desktop = Paths.get(TmmOsUtils.DESKTOP_FILE).toAbsolutePath();
      if (Files.isWritable(desktop.getParent())) {
        TmmOsUtils.createDesktopFileForLinux(desktop.toFile());
      }
    }
  }
}
