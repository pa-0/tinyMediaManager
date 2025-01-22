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
package org.tinymediamanager.ui.components.table;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.ui.components.MenuScroller;
import org.tinymediamanager.ui.components.treetable.TmmTreeTableCellRenderer;
import org.tinymediamanager.ui.components.treetable.TmmTreeTableModel;

/**
 * This popup allows to select columns to be shown/hidden in the TmmTable
 *
 * @author Manuel Laggner
 */
public class TmmTableColumnSelectionPopup {

  private TmmTableColumnSelectionPopup() {
    throw new IllegalAccessError();
  }

  /**
   * Shows the popup allowing to show/hide columns.
   */
  static void showColumnSelectionPopup(Component c, final TmmTable table) {
    // collect all checkboxes to check that at least one is checked
    final List<JCheckBoxMenuItem> checkBoxMenuItems = new ArrayList<>();

    ActionListener actionListener = e -> checkCheckBoxStates(checkBoxMenuItems);

    // build the popup menu
    JPopupMenu popup = new JPopupMenu();
    TableColumnModel columnModel = table.getColumnModel();
    if (!(columnModel instanceof TmmTableColumnModel)) {
      return;
    }

    final TmmTableColumnModel tmmTableColumnModel = (TmmTableColumnModel) columnModel;
    List<TableColumn> columns = tmmTableColumnModel.getAllColumns();
    Map<String, Object> displayNameToCheckBox = new HashMap<>();
    List<String> displayNames = new ArrayList<>();

    TableModel tableModel = table.getModel();

    for (int i = 0; i < columns.size(); i++) {
      TableColumn etc = columns.get(i);
      // prevent removing of the Nodes column in the Tree-Table
      if ("Nodes".equals(tableModel.getColumnName(etc.getModelIndex())) && etc.getModelIndex() == 0) {
        continue;
      }

      String columnName = null;

      // tooltip text
      if (table.getModel() instanceof TmmTableModel<?> tmmTableModel && StringUtils.isNotBlank(tmmTableModel.getHeaderTooltip(etc.getModelIndex()))) {
        columnName = tmmTableModel.getHeaderTooltip(etc.getModelIndex());
      }
      else if (table.getModel() instanceof TmmTreeTableModel tmmTreeTableModel
          && StringUtils.isNotBlank(tmmTreeTableModel.getHeaderTooltip(etc.getModelIndex()))) {
        columnName = tmmTreeTableModel.getHeaderTooltip(etc.getModelIndex());
      }

      // column name
      if (StringUtils.isBlank(columnName)) {
        columnName = tableModel.getColumnName(etc.getModelIndex());
      }

      // header value
      if (StringUtils.isBlank(columnName) && etc.getHeaderValue() instanceof String value) {
        columnName = value;
      }

      // fallback
      if (StringUtils.isBlank(columnName) && etc.getIdentifier() != null) {
        columnName = etc.getIdentifier().toString();
      }

      JCheckBoxMenuItem checkBox = new JCheckBoxMenuItem();
      checkBox.setText(columnName);
      checkBox.setSelected(!tmmTableColumnModel.isColumnHidden(etc));
      checkBox.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", true);
      checkBox.addActionListener(actionListener);

      // disable for the tree node in tree tables
      if (etc.getCellRenderer() instanceof TmmTreeTableCellRenderer) {
        checkBox.setSelected(true);
        checkBox.setEnabled(false);
      }

      checkBoxMenuItems.add(checkBox);

      final JCheckBoxMenuItem checkBoxMenuItem = checkBox;
      checkBox.addActionListener(evt -> tmmTableColumnModel.setColumnHidden(etc, !checkBoxMenuItem.isSelected()));

      if (!displayNames.contains(columnName)) {
        // the expected case
        displayNameToCheckBox.put(columnName, checkBox);
      }
      else {
        // the same display name is used for more columns - fuj
        ArrayList<JCheckBoxMenuItem> al = null;
        Object theFirstOne = displayNameToCheckBox.get(columnName);
        if (theFirstOne instanceof JCheckBoxMenuItem) {
          JCheckBoxMenuItem firstCheckBox = (JCheckBoxMenuItem) theFirstOne;
          al = new ArrayList<>();
          al.add(firstCheckBox);
        }
        else {
          // already a list there
          if (theFirstOne instanceof ArrayList<?> arrayList) {
            al = (ArrayList<JCheckBoxMenuItem>) theFirstOne;
          }
          else {
            throw new IllegalStateException("Wrong object theFirstOne is " + theFirstOne);
          }
        }
        al.add(checkBox);
        displayNameToCheckBox.put(columnName, al);
      }
      displayNames.add(columnName);
    }

    int index = 0;
    for (String displayName : displayNames) {
      Object obj = displayNameToCheckBox.get(displayName);
      JCheckBoxMenuItem checkBox = null;
      if (obj instanceof JCheckBoxMenuItem) {
        checkBox = (JCheckBoxMenuItem) obj;
      }
      else {
        // in case there are duplicate names we store ArrayLists
        // of JCheckBoxes
        if (obj instanceof ArrayList) {
          ArrayList<JCheckBoxMenuItem> al = (ArrayList<JCheckBoxMenuItem>) obj;
          if (index >= al.size()) {
            index = 0;
          }
          checkBox = al.get(index++);
        }
        else {
          throw new IllegalStateException("Wrong object obj is " + obj);
        }
      }
      popup.add(checkBox);
    }

    // activate the menu scroller for low resolution devices
    if (c.getGraphicsConfiguration().getDevice().getDisplayMode().getHeight() < 800) {
      MenuScroller.setScrollerFor(popup, 25, 50);
    }

    popup.show(c, 8, 8);
  }

  private static void checkCheckBoxStates(List<JCheckBoxMenuItem> checkBoxMenuItems) {
    int selectedCount = 0;
    JCheckBoxMenuItem firstSelected = null;

    for (JCheckBoxMenuItem item : checkBoxMenuItems) {
      if (item.isSelected()) {
        selectedCount++;

        if (firstSelected == null) {
          firstSelected = item;
        }
      }
    }

    if (selectedCount == 1) {
      // only 1 selected, disable the last one from being enabled
      firstSelected.setEnabled(false);
    }
    else {
      // re-enable all
      for (JCheckBoxMenuItem item : checkBoxMenuItems) {
        item.setEnabled(true);
      }
    }
  }
}
