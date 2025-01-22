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

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.MediaIdTable;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.dialogs.ImagePreviewDialog;

import ca.odell.glazedlists.EventList;
import net.miginfocom.swing.MigLayout;

public class PersonEditorPanel extends AbstractModalInputPanel {
  private final EventList<MediaIdTable.MediaId> ids;
  private final Person                          person;

  private final JTextField                      tfName;
  private final JTextField                      tfRole;
  private final JTextField                      tfImageUrl;
  private final JTextField                      tfProfileUrl;
  private final TmmTable                        tableIds;

  public PersonEditorPanel(Person person) {
    super();

    this.person = person;
    this.ids = MediaIdTable.convertIdMapToEventList(person.getIds());

    {
      setLayout(new MigLayout("", "[][500lp:n,grow][]", "[][][][][50lp:150lp][20lp]"));
      {
        JLabel lblNameT = new TmmLabel(TmmResourceBundle.getString("metatag.name"));
        add(lblNameT, "cell 0 0,alignx trailing");

        tfName = new JTextField();
        add(tfName, "cell 1 0,growx");
        tfName.setColumns(10);
      }
      {
        JLabel lblRoleT = new TmmLabel(TmmResourceBundle.getString("metatag.role"));
        add(lblRoleT, "cell 0 1,alignx trailing");

        tfRole = new JTextField();
        add(tfRole, "cell 1 1,growx");
        tfRole.setColumns(10);
      }
      {
        JLabel lblImageUrlT = new TmmLabel(TmmResourceBundle.getString("image.url"));
        add(lblImageUrlT, "cell 0 2,alignx trailing");

        tfImageUrl = new JTextField();
        add(tfImageUrl, "cell 1 2,growx");
        tfImageUrl.setColumns(10);
      }
      {
        JButton btnShowImage = new JButton(IconManager.IMAGE_INV);
        btnShowImage.setToolTipText(TmmResourceBundle.getString("image.show"));
        btnShowImage.addActionListener(e -> {
          if (StringUtils.isNotBlank(tfImageUrl.getText())) {
            // check for valid url
            try {
              URL url = new URL(tfImageUrl.getText());
              Dialog dialog = new ImagePreviewDialog(url.toExternalForm());
              dialog.setVisible(true);
            }
            catch (Exception ignored) {
              // ignored
            }
          }
        });
        add(btnShowImage, "cell 2 2");
      }
      {
        JLabel lblProfileUrlT = new TmmLabel(TmmResourceBundle.getString("profile.url"));
        add(lblProfileUrlT, "cell 0 3,alignx trailing");

        tfProfileUrl = new JTextField();
        add(tfProfileUrl, "cell 1 3,growx");
        tfProfileUrl.setColumns(10);
      }
      {
        JLabel lblIds = new TmmLabel(TmmResourceBundle.getString("metatag.ids"));
        add(lblIds, "cell 0 4,flowy, alignx right,aligny top");

        JScrollPane scrollPaneIds = new JScrollPane();
        add(scrollPaneIds, "cell 1 4,grow");

        tableIds = new MediaIdTable(ids);
        tableIds.configureScrollPane(scrollPaneIds);
      }

      JButton btnAddId = new SquareIconButton(new AddIdAction());
      add(btnAddId, "cell 0 4,alignx right,aligny top");

      JButton btnRemoveId = new SquareIconButton(new RemoveIdAction());
      add(btnRemoveId, "cell 0 4,alignx right,aligny top");
    }

    tfName.setText(person.getName());
    tfRole.setText(person.getRole());
    tfImageUrl.setText(person.getThumbUrl());
    tfProfileUrl.setText(person.getProfileUrl());

    // set focus to the first textfield
    SwingUtilities.invokeLater(tfName::requestFocus);
  }

  @Override
  protected void onClose() {
    person.setName(tfName.getText());
    person.setRole(tfRole.getText());
    person.setThumbUrl(tfImageUrl.getText());
    person.setProfileUrl(tfProfileUrl.getText());

    // sync of media ids
    // first round -> add existing ids
    for (MediaIdTable.MediaId id : ids) {
      // first try to cast it into an Integer
      try {
        Integer value = Integer.parseInt(id.value);
        // cool, it is an Integer
        person.setId(id.key, value);
      }
      catch (NumberFormatException ex) {
        // okay, we set it as a String
        person.setId(id.key, id.value);
      }
    }
    // second round -> remove deleted ids
    List<String> removeIds = new ArrayList<>();
    for (Map.Entry<String, Object> entry : person.getIds().entrySet()) {
      MediaIdTable.MediaId id = new MediaIdTable.MediaId(entry.getKey());
      if (!ids.contains(id)) {
        removeIds.add(entry.getKey());
      }
    }
    for (String id : removeIds) {
      // set a null value causes to fire the right events
      person.setId(id, null);
    }

    setVisible(false);
  }

  /**********************
   * Helper classes
   **********************/
  private class AddIdAction extends AbstractAction {
    public AddIdAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("id.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(PersonEditorPanel.this);
      if (iModalPopupPanelProvider == null) {
        return;
      }

      MediaIdTable.MediaId mediaId = new MediaIdTable.MediaId();

      ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
      popupPanel.setTitle(TmmResourceBundle.getString("id.add"));

      popupPanel.setOnCloseHandler(() -> {
        if (StringUtils.isNoneBlank(mediaId.key, mediaId.value)) {
          ids.add(mediaId);
        }
      });

      IdEditorPanel idEditorPanel = new IdEditorPanel(mediaId, null);
      popupPanel.setContent(idEditorPanel);
      iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
    }
  }

  private class RemoveIdAction extends AbstractAction {
    public RemoveIdAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("id.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableIds.getSelectedRow();
      if (row > -1) {
        row = tableIds.convertRowIndexToModel(row);
        ids.remove(row);
      }
    }
  }
}
