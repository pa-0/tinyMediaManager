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

package org.tinymediamanager.ui.components;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.table.TmmEditorTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.ImagePreviewDialog;
import org.tinymediamanager.ui.panels.IModalPopupPanelProvider;
import org.tinymediamanager.ui.panels.ModalPopupPanel;
import org.tinymediamanager.ui.panels.PersonEditorPanel;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.GlazedListsSwing;

/**
 * This class is used to display Persons in a table
 *
 * @author Manuel Laggner
 */
public class PersonTable extends TmmEditorTable {
  private static final Logger     LOGGER    = LoggerFactory.getLogger(PersonTable.class);

  private final EventList<Person> personEventList;

  private String                  addTitle  = "";
  private String                  editTitle = "";

  /**
   * create a PersonTable for display only
   * 
   * @param personEventList
   *          the EventList containing the Persons
   */
  public PersonTable(EventList<Person> personEventList) {
    super();

    this.personEventList = personEventList;

    setModel(new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(personEventList), new PersonTableFormat()));

    adjustColumnPreferredWidths(3);
  }

  /**
   * Utility to get the right {@link Person} for the given row
   * 
   * @param row
   *          the row number to get the {@link Person} for
   * @return the {@link Person}
   */
  private Person getPerson(int row) {
    int index = convertRowIndexToModel(row);
    return personEventList.get(index);
  }

  @Override
  protected void editButtonClicked(int row) {
    IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(this);
    if (iModalPopupPanelProvider == null) {
      return;
    }

    Person person = getPerson(row);

    ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
    popupPanel.setTitle(getEditTitle());
    popupPanel.setOnCloseHandler(() -> onPersonChanged(person));

    PersonEditorPanel personEditorPanel = new PersonEditorPanel(person);
    popupPanel.setContent(personEditorPanel);
    iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
  }

  private String getEditTitle() {
    if (StringUtils.isNotBlank(editTitle)) {
      return editTitle;
    }
    else {
      return TmmResourceBundle.getString("cast.edit");
    }
  }

  public void onPersonChanged(Person person) {
    // to override
  }

  @Override
  protected boolean isLinkCell(int row, int column) {
    return isEditorColumn(column) || (isProfileColumn(column) && isProfileAvailable(row)) || (isImageColumn(column) && isImageAvailable(row));
  }

  /**
   * check if this column is the profile column
   *
   * @param column
   *          the column index
   * @return true/false
   */
  private boolean isProfileColumn(int column) {
    if (column < 0) {
      return false;
    }

    return "profileUrl".equals(getColumnModel().getColumn(column).getIdentifier());
  }

  /**
   * checks whether a profile url is available or not
   * 
   * @param row
   *          the row to get the data for
   * @return true if a profile url is available, false otherwise
   */
  private boolean isProfileAvailable(int row) {
    return StringUtils.isNotBlank(getPerson(row).getProfileUrl());
  }

  /**
   * check if this column is the image column
   *
   * @param column
   *          the column index
   * @return true/false
   */
  private boolean isImageColumn(int column) {
    if (column < 0) {
      return false;
    }

    return "imageUrl".equals(getColumnModel().getColumn(column).getIdentifier());
  }

  /**
   * checks whether an image url is available or not
   *
   * @param row
   *          the row to get the data for
   * @return true if an image url is available, false otherwise
   */
  private boolean isImageAvailable(int row) {
    return StringUtils.isNotBlank(getPerson(row).getThumbUrl());
  }

  @Override
  protected void linkClicked(int row, int column) {
    Person person = getPerson(row);

    if (person != null) {
      if (isProfileColumn(column) && StringUtils.isNotBlank(person.getProfileUrl())) {
        try {
          TmmUIHelper.browseUrl(person.getProfileUrl());
        }
        catch (Exception e1) {
          LOGGER.error("Opening actor profile", e1);
          MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, person.getProfileUrl(), "message.erroropenurl",
              new String[] { ":", e1.getLocalizedMessage() }));
        }
      }
      else if (isImageColumn(column) && StringUtils.isNotBlank(person.getThumbUrl())) {
        ImagePreviewDialog dialog = new ImagePreviewDialog(person.getThumbUrl());
        dialog.setVisible(true);
      }
    }
  }

  public void setAddTitle(String addTitle) {
    this.addTitle = addTitle;
  }

  private String getAddTitle() {
    if (StringUtils.isNotBlank(addTitle)) {
      return addTitle;
    }
    else {
      return TmmResourceBundle.getString("cast.add");
    }
  }

  public void setEditTitle(String editTitle) {
    this.editTitle = editTitle;
  }

  /**
   * get all selected {@link Person}s
   * 
   * @return a {@link List} of all selected {@link Person}s
   */
  public List<Person> getSelectedPersons() {
    List<Person> selectedPersons = new ArrayList<>();
    for (int i : getSelectedRows()) {
      Person person = getPerson(i);
      if (person != null) {
        selectedPersons.add(person);
      }

    }
    return selectedPersons;
  }

  public void addPerson(Person.Type personType) {
    IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(this);
    if (iModalPopupPanelProvider == null) {
      return;
    }

    String defaultName;
    String defaultRole;

    switch (personType) {
      case ACTOR -> {
        defaultName = TmmResourceBundle.getString("cast.actor.unknown");
        defaultRole = TmmResourceBundle.getString("cast.role.unknown");
      }
      case GUEST -> {
        defaultName = TmmResourceBundle.getString("cast.actor.unknown");
        defaultRole = TmmResourceBundle.getString("cast.role.unknown");
      }
      case DIRECTOR -> {
        defaultName = TmmResourceBundle.getString("director.name.unknown");
        defaultRole = "Director";
      }
      case WRITER -> {
        defaultName = TmmResourceBundle.getString("writer.name.unknown");
        defaultRole = "Writer";
      }
      case PRODUCER -> {
        defaultName = TmmResourceBundle.getString("producer.name.unknown");
        defaultRole = TmmResourceBundle.getString("producer.role.unknown");
      }
      default -> {
        defaultName = "";
        defaultRole = "";
      }
    }

    Person person = new Person(personType, defaultName, defaultRole);

    ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
    popupPanel.setTitle(getAddTitle());

    popupPanel.setOnCloseHandler(() -> {
      if (StringUtils.isNotBlank(person.getName()) && !person.getName().equals(defaultName)) {
        if (person.getRole().equals(defaultRole)) {
          person.setRole("");
        }
        personEventList.add(0, person);
      }
    });

    PersonEditorPanel personEditorPanel = new PersonEditorPanel(person);
    popupPanel.setContent(personEditorPanel);
    iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
  }

  /**
   * helper classes
   */
  private static class PersonTableFormat extends TmmTableFormat<Person> {
    private PersonTableFormat() {
      /*
       * name
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.name"), "name", Person::getName, String.class);
      col.setColumnResizeable(true);
      addColumn(col);

      /*
       * role
       */
      col = new Column(TmmResourceBundle.getString("metatag.role"), "role", Person::getRole, String.class);
      col.setColumnResizeable(true);
      addColumn(col);

      /*
       * image
       */
      col = new Column(TmmResourceBundle.getString("image.url"), "imageUrl", person -> {
        if (StringUtils.isNotBlank(person.getThumbUrl())) {
          return IconManager.TABLE_OK;
        }
        return IconManager.TABLE_NOT_OK;
      }, ImageIcon.class);
      col.setColumnResizeable(false);
      col.setHeaderIcon(IconManager.IMAGES);
      addColumn(col);

      /*
       * profile
       */
      col = new Column(TmmResourceBundle.getString("profile.url"), "profileUrl", person -> {
        if (StringUtils.isNotBlank(person.getProfileUrl())) {
          return IconManager.TABLE_OK;
        }
        return IconManager.TABLE_NOT_OK;
      }, ImageIcon.class);
      col.setColumnResizeable(false);
      col.setHeaderIcon(IconManager.IDCARD);
      addColumn(col);

      /*
       * edit
       */
      col = new Column(TmmResourceBundle.getString("Button.edit"), "edit", person -> IconManager.EDIT, ImageIcon.class);
      col.setColumnResizeable(false);
      col.setHeaderIcon(IconManager.EDIT_HEADER);
      addColumn(col);
    }
  }
}
