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

package org.tinymediamanager.ui.panels;

import java.awt.Component;

/**
 * the interface {@link IModalPopupPanelProvider} is used to manage displaying of the modal popups inside a
 * {@link javax.swing.JFrame}/{@link javax.swing.JDialog}
 * 
 * @author Manuel Laggner
 */
public interface IModalPopupPanelProvider {
  /**
   * create a {@link ModalPopupPanel} for this {@link IModalPopupPanelProvider}
   * 
   * @return a new instance of {@link ModalPopupPanel}
   */
  default ModalPopupPanel createModalPopupPanel() {
    return new ModalPopupPanel(this);
  }

  /**
   * finds the {@link IModalPopupPanelProvider} which can be used for the given {@link Component}
   * 
   * @param parent
   *          the {@link Component} to search the {@link IModalPopupPanelProvider} for
   * @return the found {@link IModalPopupPanelProvider} or null if there is no one in the component hierarchy
   */
  static IModalPopupPanelProvider findModalProvider(Component parent) {
    Component comp = parent;
    while (true) {
      comp = comp.getParent();
      if (comp == null) {
        return null;
      }
      else if (comp instanceof IModalPopupPanelProvider iModalPopupPanelProvider) {
        return iModalPopupPanelProvider;
      }
    }
  }

  void showModalPopupPanel(ModalPopupPanel popupPanel);

  void hideModalPopupPanel(ModalPopupPanel popupPanel);
}
