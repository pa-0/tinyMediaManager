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
package org.tinymediamanager.ui.components.combobox;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.FlatButton;

/**
 * the class ScraperMetadataConfigCheckComboBox is used to display a CheckCombBox for the scraper metadata config
 * 
 * @author Manuel Laggner
 */
public class ScraperMetadataConfigCheckComboBox<E extends ScraperMetadataConfig> extends TmmCheckComboBox<E> {
  public ScraperMetadataConfigCheckComboBox(final List<E> scrapers) {
    super(scrapers);
  }

  public ScraperMetadataConfigCheckComboBox(E[] scrapers) {
    super(scrapers);
  }

  @Override
  protected void setRenderer() {
    setRenderer(new ScraperMetadataConfigRenderer());
  }

  @Override
  protected void init() {
    super.init();
    setEditor(new ScraperMetadataConfigEditor());
  }

  /**
   * set new items to the CheckComboBox
   *
   * @param items
   *          the items to be set
   */
  @Override
  public void setItems(Collection<E> items) {
    model.removeAllElements();
    checkComboBoxItems.clear();
    comboBoxItemMap.clear();

    TmmCheckComboBoxItem<E> checkComboBoxItem;

    checkComboBoxItem = new TmmCheckComboBoxItem<>(TmmResourceBundle.getString("Button.selectall"));
    checkComboBoxItem.setSelected(false);
    checkComboBoxItems.add(checkComboBoxItem);

    checkComboBoxItem = new TmmCheckComboBoxItem<>(TmmResourceBundle.getString("Button.selectnone"));
    checkComboBoxItem.setSelected(true);
    checkComboBoxItems.add(checkComboBoxItem);

    checkComboBoxItems.add(nullItem);

    for (E item : items) {
      checkComboBoxItem = new TmmCheckComboBoxItem<>(item);
      checkComboBoxItem.setText(item.getDescription());
      checkComboBoxItem.setToolTipText(item.getToolTip());
      this.checkComboBoxItems.add(checkComboBoxItem);
      this.comboBoxItemMap.put(item, checkComboBoxItem);
    }

    model.addAll(checkComboBoxItems);
  }

  private class ScraperMetadataConfigRenderer extends CheckBoxRenderer {
    private ScraperMetadataConfigRenderer() {
      super();
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends TmmCheckComboBoxItem<E>> list, TmmCheckComboBoxItem<E> value, int index,
        boolean isSelected, boolean cellHasFocus) {
      if (index >= 0 && index <= model.getSize()) {
        TmmCheckComboBoxItem<E> cb = model.getElementAt(index);
        if (cb == nullItem) {
          list.setToolTipText(null);
          return separator;
        }

        if (isSelected) {
          cb.setBackground(UIManager.getColor("ComboBox.selectionBackground"));
          cb.setForeground(UIManager.getColor("ComboBox.selectionForeground"));
          list.setToolTipText(cb.getText());
        }
        else {
          cb.setBackground(UIManager.getColor("ComboBox.background"));
          cb.setForeground(UIManager.getColor("ComboBox.foreground"));
        }

        return cb;
      }

      list.setToolTipText(null);
      return defaultRenderer.getListCellRendererComponent(list, TmmResourceBundle.getString("ComboBox.select"), index, isSelected, cellHasFocus);
    }
  }

  private class ScraperMetadataConfigEditor extends DefaultCheckBoxEditor {
    @Override
    protected JComponent getEditorItem(E userObject) {
      return new ScraperMetadataConfigEditorItem(userObject);
    }
  }

  private class ScraperMetadataConfigEditorItem extends JPanel {

    public ScraperMetadataConfigEditorItem(E userObject) {
      super();
      putClientProperty("class", "roundedPanel");
      putClientProperty("borderRadius", 6);
      setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
      setBorder(null);

      JLabel label = new JLabel(userObject.getDescription());
      label.setToolTipText(userObject.getToolTip());
      label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
      add(label);

      JButton button = new FlatButton(IconManager.DELETE);
      button.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
      button.addActionListener(e -> {
        TmmCheckComboBoxItem<E> item = comboBoxItemMap.get(userObject);
        if (item != null) {
          // get index in the model
          int index = model.getIndexOf(item);
          if (index > -1) {
            // change the checked state
            checkBoxSelectionChanged(index);
            for (TmmCheckComboBoxSelectionListener listener : changedListeners) {
              listener.selectionChanged(index);
            }
          }
          ScraperMetadataConfigCheckComboBox.this.update();
        }
      });
      add(button);
    }
  }
}
