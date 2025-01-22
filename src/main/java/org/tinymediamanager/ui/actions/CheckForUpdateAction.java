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

import static org.tinymediamanager.ui.TmmUIHelper.checkForUpdate;

import java.awt.event.ActionEvent;

import org.tinymediamanager.core.TmmResourceBundle;

/**
 * The CheckForUpdateAction is used to trigger an update check
 * 
 * @author Manuel Laggner
 */
public class CheckForUpdateAction extends TmmAction {
  public CheckForUpdateAction() {
    putValue(NAME, TmmResourceBundle.getString("tmm.updater.check"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tmm.updater.check"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    checkForUpdate(0);
  }
}
