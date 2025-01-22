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
import java.nio.file.Paths;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.TmmOsUtils;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmUIHelper;

/**
 * The class ClearDatabaseAction. Used to clear the whole database (to start with a new one)
 * 
 * @author Manuel Laggner
 */
public class ClearDatabaseAction extends TmmAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClearDatabaseAction.class);

  public ClearDatabaseAction() {
    putValue(NAME, TmmResourceBundle.getString("tmm.cleardatabase"));
  }

  @Override
  protected void processAction(ActionEvent arg0) {
    // display warning popup
    Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
    int answer = JOptionPane.showOptionDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.cleardatabase.hint"),
        TmmResourceBundle.getString("tmm.cleardatabase"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
    if (answer != JOptionPane.YES_OPTION) {
      return;
    }

    MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    // delete the database
    try {
      TmmModuleManager.getInstance().shutDown();
      TmmModuleManager.getInstance().initializeDatabase();
      MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.cleardatabase.info"));
    }
    catch (Exception e) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.cleardatabase.error"));
      // open the tmm folder
      Path path = Paths.get("");
      TmmUIHelper.openFolder(path);
    }
    MainWindow.getInstance().closeTmmAndStart(TmmOsUtils.getPBforTMMrestart());
  }
}
