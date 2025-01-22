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
package org.tinymediamanager.ui.tvshows.filters;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.tinymediamanager.scraper.util.MetadataUtil;

import net.miginfocom.swing.MigLayout;

public abstract class AbstractNumberTvShowUIFilter extends AbstractTvShowUIFilter {
  protected JSpinner spinnerLow;
  protected JSpinner spinnerHigh;
  protected JLabel   lblTo;

  protected JSpinner.NumberEditor prepareNumberEditor(JSpinner spinner, String pattern) {
    JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(spinner, pattern);
    numberEditor.getTextField().setColumns((int) Math.floor(pattern.length() * 0.8f));
    return numberEditor;
  }

  @Override
  protected JComponent createFilterComponent() {
    JPanel panelFilterComponent = new JPanel(new MigLayout("insets 0", "[][][]", "[]"));

    spinnerLow = new JSpinner(getNumberModel());
    spinnerLow.addChangeListener(changeListener);
    panelFilterComponent.add(spinnerLow, "cell 0 0");

    lblTo = new JLabel("-");
    panelFilterComponent.add(lblTo, "cell 1 0");

    spinnerHigh = new JSpinner(getNumberModel());
    spinnerHigh.addChangeListener(changeListener);
    panelFilterComponent.add(spinnerHigh, "cell 2 0");

    return panelFilterComponent;
  }

  @Override
  public String getFilterValueAsString() {
    return spinnerLow.getValue() + "," + spinnerHigh.getValue();
  }

  @Override
  public void setFilterValue(Object value) {
    String[] values = value.toString().split(",");
    if (values.length > 0) {
      spinnerLow.setValue(MetadataUtil.parseInt(values[0], 0));
    }
    if (values.length > 1) {
      spinnerHigh.setValue(MetadataUtil.parseInt(values[1], 0));
    }
  }

  @Override
  public void clearFilter() {
    spinnerLow.setValue(0);
    spinnerHigh.setValue(0);
  }

  protected abstract SpinnerNumberModel getNumberModel();

  @Override
  protected JComboBox<FilterOption> createOptionComboBox() {
    JComboBox<FilterOption> comboBox = new JComboBox<>(
        new FilterOption[] { FilterOption.LT, FilterOption.LE, FilterOption.EQ, FilterOption.GT, FilterOption.GE, FilterOption.BT });
    comboBox.addActionListener(l -> {
      lblTo.setVisible(comboBox.getSelectedItem() == FilterOption.BT);
      spinnerHigh.setVisible(comboBox.getSelectedItem() == FilterOption.BT);
    });
    comboBox.setSelectedItem(FilterOption.EQ);

    return comboBox;
  }

  protected boolean matchInt(int value) {
    FilterOption filterOption = getFilterOption();

    int low = (int) spinnerLow.getValue();
    int high = (int) spinnerHigh.getValue();

    if (value == 0) {
      return false;
    }

    if (filterOption == FilterOption.EQ && value == low) {
      return true;
    }
    else if (filterOption == FilterOption.LT && value < low) {
      return true;
    }
    else if (filterOption == FilterOption.LE && value <= low) {
      return true;
    }
    else if (filterOption == FilterOption.GE && value >= low) {
      return true;
    }
    else if (filterOption == FilterOption.GT && value > low) {
      return true;
    }
    else if (filterOption == FilterOption.BT && low <= value && value <= high) {
      return true;
    }

    return false;
  }
}
