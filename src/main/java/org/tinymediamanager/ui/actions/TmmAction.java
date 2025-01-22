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

import javax.swing.AbstractAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.license.TmmFeature;

/**
 * The class TmmAction is an abstract action-wrapper to provide base logging
 */
public abstract class TmmAction extends AbstractAction implements TmmFeature {
  private static final Logger LOGGER = LoggerFactory.getLogger(TmmAction.class);

  @Override
  public final void actionPerformed(ActionEvent e) {
    LOGGER.debug("action fired: {}", this.getClass().getSimpleName());

    // inform the statistics timer that tmm is active
    TmmModuleManager.getInstance().setActive();

    if (isEnabled()) {
      processAction(e);
    }
  }

  @Override
  public final Object getValue(String key) {
    switch (key) {
      case "enabled":
        return isEnabled();

      case NAME:
      case SHORT_DESCRIPTION:
        Object value = super.getValue(key);
        if (value != null && !isEnabled()) {
          return "*PRO* " + value.toString();
        }
        return value;

      default:
        return super.getValue(key);
    }
  }

  @Override
  public final boolean isEnabled() {
    return isFeatureEnabled();
  }

  /**
   * the inheriting class should process the action in this method rather than actionPerformed()
   * 
   * @param e
   *          the ActionEvent from actionPerformed
   */
  protected abstract void processAction(ActionEvent e);
}
