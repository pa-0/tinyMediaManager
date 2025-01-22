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
package org.tinymediamanager.ui.components.combobox;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * the class {@link TmmCheckComboBoxFilterDecorator} is used to decorate a TmmCheckComboBox with a filter
 * 
 * @param <E>
 *          the line type
 * 
 * @author Manuel Laggner
 */
class TmmCheckComboBoxFilterDecorator<E> {
  private final TmmCheckComboBox<E>                     comboBox;
  private final BiPredicate<E, String>                  userFilter;
  private final TextHandler                             textHandler                 = new TextHandler();

  private List<TmmCheckComboBoxItem<E>>                 items;
  private DefaultComboBoxModel<TmmCheckComboBoxItem<E>> model;

  private Popup                                         filterPopup;
  private JLabel                                        filterLabel;
  private Object                                        selectedItem;
  private boolean                                       doNotClearFilterOnPopupHide = false;

  public TmmCheckComboBoxFilterDecorator(TmmCheckComboBox<E> comboBox, BiPredicate<E, String> userFilter) {
    this.comboBox = comboBox;
    this.userFilter = userFilter;
  }

  public static <E> TmmCheckComboBoxFilterDecorator<E> decorate(TmmCheckComboBox<E> comboBox, BiPredicate<E, String> userFilter) {
    TmmCheckComboBoxFilterDecorator<E> decorator = new TmmCheckComboBoxFilterDecorator<>(comboBox, userFilter);
    decorator.init();
    return decorator;
  }

  private void init() {
    items = comboBox.checkComboBoxItems;
    model = (DefaultComboBoxModel<TmmCheckComboBoxItem<E>>) comboBox.getModel();

    initFilterLabel();
    initComboPopupListener();
    initComboKeyListener();
  }

  private void initComboKeyListener() {
    comboBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        char keyChar = e.getKeyChar();
        if (!Character.isDefined(keyChar)) {
          return;
        }
        int keyCode = e.getKeyCode();
        switch (keyCode) {
          case KeyEvent.VK_DELETE:
            return;

          case KeyEvent.VK_ENTER:
            resetFilterPopup();
            e.consume();
            return;

          case KeyEvent.VK_ESCAPE:
            if (selectedItem != null) {
              comboBox.setSelectedItem(selectedItem);
            }
            resetFilterPopup();
            e.consume();
            return;

          case KeyEvent.VK_BACK_SPACE:
            textHandler.removeCharAtEnd();
            break;

          default:
            textHandler.add(keyChar);
        }

        doNotClearFilterOnPopupHide = true;
        comboBox.getUI().setPopupVisible(comboBox, false);

        if (!textHandler.text.isEmpty()) {
          showFilterPopup();
          performFilter();
        }
        else {
          resetFilterPopup();
        }

        doNotClearFilterOnPopupHide = false;
        comboBox.getUI().setPopupVisible(comboBox, true);

        e.consume();
      }
    });
  }

  private void initFilterLabel() {
    filterLabel = new JLabel();
    filterLabel.setOpaque(true);
    filterLabel.setBackground(UIManager.getColor("Table.selectionBackground"));
    filterLabel.setForeground(UIManager.getColor("Table.selectionForeground"));

    Insets margin = UIManager.getInsets("TextField.margin");
    filterLabel.setBorder(BorderFactory.createCompoundBorder(UIManager.getBorder("PopupMenu.border"),
        BorderFactory.createEmptyBorder(margin.top, margin.left, margin.bottom, margin.right)));
  }

  public JLabel getFilterLabel() {
    return filterLabel;
  }

  private void initComboPopupListener() {
    comboBox.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        if (doNotClearFilterOnPopupHide) {
          return;
        }

        resetFilterPopup();
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
        if (doNotClearFilterOnPopupHide) {
          return;
        }

        resetFilterPopup();
      }
    });
  }

  private void showFilterPopup() {
    if (textHandler.getText().isEmpty()) {
      return;
    }
    if (filterPopup == null) {
      Point p = new Point(0, 0);
      SwingUtilities.convertPointToScreen(p, comboBox);
      filterLabel.setPreferredSize(new Dimension(comboBox.getSize()));
      filterPopup = PopupFactory.getSharedInstance().getPopup(comboBox, filterLabel, p.x, p.y - filterLabel.getPreferredSize().height);
      selectedItem = comboBox.getSelectedItem();

    }
    filterPopup.show();
  }

  private void resetFilterPopup() {
    if (!textHandler.isEditing()) {
      return;
    }
    if (filterPopup != null) {
      filterPopup.hide();
      filterPopup = null;
      filterLabel.setText("");
      textHandler.reset();

      // add items in the original order
      model.removeAllElements();
      model.addAll(items);
    }
  }

  private void performFilter() {
    filterLabel.setText(textHandler.getText());
    model.removeAllElements();

    // add matched items first
    List<TmmCheckComboBoxItem<E>> filteredItems = new ArrayList<>();

    for (TmmCheckComboBoxItem<E> item : items) {
      if (item.getUserObject() != null && userFilter.test(item.getUserObject(), textHandler.getText())) {
        filteredItems.add(item);
      }
      else if (item.getUserObject() == null) {
        filteredItems.add(item);
      }
    }

    model.addAll(filteredItems);
    comboBox.update();
  }

  private static class TextHandler {
    private String  text = "";
    private boolean editing;

    public void add(char c) {
      text += c;
      editing = true;
    }

    public void removeCharAtEnd() {
      if (text.length() > 0) {
        text = text.substring(0, text.length() - 1);
        editing = true;
      }
    }

    public void reset() {
      text = "";
      editing = false;
    }

    public String getText() {
      return text;
    }

    public boolean isEditing() {
      return editing;
    }
  }
}
