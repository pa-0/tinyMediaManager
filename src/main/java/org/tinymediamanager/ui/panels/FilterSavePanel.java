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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.TmmResourceBundle;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link FilterSavePanel} is used to save/overwrite filters
 * 
 * @author Manuel Laggner
 */
public class FilterSavePanel extends AbstractModalInputPanel {
  private final JTextField                                    tfPresetName;
  private final JRadioButton                                  rdbtnNewPreset;
  private final JRadioButton                                  rdbtnOverwritePreset;
  private final JComboBox<String>                             cbPresets;

  private final Set<AbstractSettings.UIFilters>               filter;
  private final Map<String, List<AbstractSettings.UIFilters>> existingFilters;

  private String                                              savedPreset = "";

  public FilterSavePanel(Set<AbstractSettings.UIFilters> filter, Map<String, List<AbstractSettings.UIFilters>> existingFilters) {
    this.filter = filter;
    this.existingFilters = existingFilters;

    {
      setLayout(new MigLayout("", "[][100lp:n,grow]", "[][]"));

      ButtonGroup buttonGroup = new ButtonGroup();

      rdbtnNewPreset = new JRadioButton(TmmResourceBundle.getString("filter.savenew"));
      buttonGroup.add(rdbtnNewPreset);
      rdbtnNewPreset.setSelected(true);
      add(rdbtnNewPreset, "cell 0 0");

      tfPresetName = new JTextField();
      add(tfPresetName, "cell 1 0,growx");
      tfPresetName.setColumns(10);

      rdbtnOverwritePreset = new JRadioButton(TmmResourceBundle.getString("filter.overwrite"));
      buttonGroup.add(rdbtnOverwritePreset);
      add(rdbtnOverwritePreset, "cell 0 1");

      cbPresets = new JComboBox(existingFilters.keySet().toArray(new String[0]));
      if (!existingFilters.isEmpty()) {
        cbPresets.setSelectedIndex(0);
      }
      else {
        rdbtnOverwritePreset.setEnabled(false);
        cbPresets.setEnabled(false);
      }

      add(cbPresets, "cell 1 1,growx");
    }

    SwingUtilities.invokeLater(tfPresetName::requestFocus);
  }

  @Override
  protected void onClose() {
    String name;
    if (rdbtnOverwritePreset.isSelected()) {
      name = (String) cbPresets.getSelectedItem();
    }
    else {
      name = tfPresetName.getText();
    }

    if (StringUtils.isBlank(name)) {
      JOptionPane.showMessageDialog(this, TmmResourceBundle.getString("filter.emptyname"));
      return;
    }

    existingFilters.put(name, new ArrayList<>(filter));
    savedPreset = name;

    setVisible(false);
  }

  public String getSavedPreset() {
    return savedPreset;
  }
}
