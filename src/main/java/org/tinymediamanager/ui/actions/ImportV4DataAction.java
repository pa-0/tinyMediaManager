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

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.UpgradeTasks;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmUIHelper;

/**
 * The {@link ImportV4DataAction} will trigger the import of v4 data
 * 
 * @author Manuel Laggner
 */
public class ImportV4DataAction extends TmmAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImportV4DataAction.class);

  public ImportV4DataAction() {
    putValue(NAME, TmmResourceBundle.getString("tmm.importv4"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tmm.importv4.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    // display warning popup
    Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
    int answer = JOptionPane.showOptionDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.importv4.hint"),
        TmmResourceBundle.getString("tmm.importv4"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
    if (answer != JOptionPane.YES_OPTION) {
      return;
    }

    Path path = null;
    if (SystemUtils.IS_OS_MAC) {
      path = TmmUIHelper.selectApplication(TmmResourceBundle.getString("tmm.importv4.installation"), "");
    }
    else {
      path = TmmUIHelper.selectDirectory(TmmResourceBundle.getString("tmm.importv4.installation"), "");
    }

    if (path != null) {
      // search for the data folder inside the path
      List<Path> files = Utils.listFilesRecursive(path);
      for (Path file : files) {
        if (file.endsWith("tmm.json") && file.getParent().endsWith("data")) {
          LOGGER.debug("found v4 installation - copying data from '{}'", file.getParent());

          MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          UpgradeTasks.copyV4Data(file.getParent());
        }
      }
    }

    // no installation data found
    JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.importv4.notfound"),
        TmmResourceBundle.getString("tmm.importv4"), JOptionPane.INFORMATION_MESSAGE);
  }
}
