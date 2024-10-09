/*
 * Copyright 2012 - 2024 Manuel Laggner
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
package org.tinymediamanager.ui.dialogs;

import static org.tinymediamanager.ui.TmmUIHelper.createLinkForImage;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JSpinner;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.LinkLabel;

/**
 * the class {@link AbstractEditorDialog} is a template for all editor dialogs
 *
 * @author Manuel Laggner
 */
public abstract class AbstractEditorDialog extends TmmDialog {
  private final MediaEntity mediaEntity;

  protected boolean         continueQueue = true;
  protected boolean         navigateBack  = false;
  protected int             queueIndex;
  protected int             queueSize;

  protected AbstractEditorDialog(String title, String id, MediaEntity mediaEntity) {
    super(title, id);
    this.mediaEntity = mediaEntity;
  }

  public boolean isContinueQueue() {
    return continueQueue;
  }

  public boolean isNavigateBack() {
    return navigateBack;
  }

  /**
   * Shows the dialog and returns whether the work on the queue should be continued.
   *
   * @return true, if successful
   */
  public boolean showDialog() {
    setLocationRelativeTo(MainWindow.getInstance());
    setVisible(true);
    return continueQueue;
  }

  protected void setImageSizeAndCreateLink(LinkLabel lblSize, ImageLabel imageLabel, JButton buttonDelete, MediaFileType type) {
    createLinkForImage(lblSize, imageLabel);

    // image has been deleted
    if (imageLabel.getOriginalImageSize().width == 0 && imageLabel.getOriginalImageSize().height == 0) {
      lblSize.setText("");
      lblSize.setVisible(false);
      buttonDelete.setVisible(false);
      return;
    }

    Dimension dimension;

    // check if there is a change in the artwork - in this case take the dimension from the imagelabel
    if (StringUtils.isNotBlank(imageLabel.getImageUrl()) && !imageLabel.getImageUrl().equals(mediaEntity.getArtworkUrl(type))) {
      dimension = imageLabel.getOriginalImageSize();
    }
    else {
      // take from the existing artwork
      dimension = mediaEntity.getArtworkDimension(type);
    }

    if (dimension.width == 0 && dimension.height == 0) {
      lblSize.setText(imageLabel.getOriginalImageSize().width + "x" + imageLabel.getOriginalImageSize().height);
    }
    else {
      lblSize.setText(dimension.width + "x" + dimension.height);
    }

    lblSize.setVisible(true);
    buttonDelete.setVisible(true);
  }

  protected void commitChanges() {
    for (Component component : getAllComponents(getContentPane())) {
      if (component instanceof JSpinner spinner) {
        try {
          spinner.commitEdit();
        }
        catch (ParseException e) {
          // ignore
        }
      }
    }
  }

  private List<Component> getAllComponents(final Container c) {
    Component[] comps = c.getComponents();
    List<Component> compList = new ArrayList<>();
    for (Component comp : comps) {
      compList.add(comp);
      if (comp instanceof Container) {
        compList.addAll(getAllComponents((Container) comp));
      }
    }
    return compList;
  }

  /*********************
   * helper classes
   *********************/
  protected class AbortQueueAction extends AbstractAction {
    public AbortQueueAction(String tooltip) {
      putValue(NAME, TmmResourceBundle.getString("Button.abortqueue"));
      putValue(SHORT_DESCRIPTION, tooltip);
      putValue(SMALL_ICON, IconManager.STOP_INV);
      putValue(LARGE_ICON_KEY, IconManager.STOP_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      continueQueue = false;
      setVisible(false);
    }
  }

  protected class NavigateBackAction extends AbstractAction {
    public NavigateBackAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.back"));
      putValue(SMALL_ICON, IconManager.BACK_INV);
      putValue(LARGE_ICON_KEY, IconManager.BACK_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      navigateBack = true;
      setVisible(false);
    }
  }
}
