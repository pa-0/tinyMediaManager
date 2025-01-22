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

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.tinymediamanager.core.TmmResourceBundle;

/**
 * an abstract template for a modal input panel
 * 
 * @author Manuel Laggner
 */
public abstract class AbstractModalInputPanel extends JPanel implements IModalPopupPanel {
  protected final JButton btnClose;
  protected final JButton btnCancel;

  protected boolean       cancel = false;

  public AbstractModalInputPanel() {
    btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
    btnCancel.addActionListener(e -> onCancel());

    btnClose = new JButton(TmmResourceBundle.getString("Button.save"));
    btnClose.addActionListener(e -> onClose());
  }

  /**
   * the on close handler
   */
  protected abstract void onClose();

  /**
   * the on cancel handler
   */
  protected void onCancel() {
    cancel = true;
    setVisible(false);
  }

  @Override
  public JComponent getContent() {
    return this;
  }

  @Override
  public JButton getCloseButton() {
    return btnClose;
  }

  @Override
  public JButton getCancelButton() {
    return btnCancel;
  }

  @Override
  public boolean isCancelled() {
    return cancel;
  }
}
