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

import static javax.swing.SwingConstants.HORIZONTAL;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiPredicate;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.WrapLayout;
import org.tinymediamanager.ui.components.FlatButton;

/**
 * the class TmmCheckComboBox extends the JComboBox by a checkbox for multiple selections
 * 
 * @author Manuel Laggner
 *
 * @param <E>
 */
public class TmmCheckComboBox<E> extends JComboBox<TmmCheckComboBoxItem<E>> {
  protected static final ResourceBundle                         BUNDLE           = ResourceBundle.getBundle("messages");

  protected final DefaultComboBoxModel<TmmCheckComboBoxItem<E>> model;
  protected final List<TmmCheckComboBoxItem<E>>                 checkComboBoxItems;
  // for faster lookup usage
  protected final Map<E, TmmCheckComboBoxItem<E>>               comboBoxItemMap;

  protected final List<TmmCheckComboBoxSelectionListener>       changedListeners = new ArrayList<>();

  protected final TmmCheckComboBoxItem<E>                       nullItem         = new TmmCheckComboBoxItem<>(null);
  protected JComponent                                          editor           = null;
  protected TmmCheckComboBoxFilterDecorator<E>                  decorator;

  /**
   * create the CheckComboBox without any items
   */
  public TmmCheckComboBox() {
    this(Collections.emptyList());
  }

  /**
   * create the CheckComboBox with the given items
   * 
   * @param items
   *          an array of initial items
   */
  public TmmCheckComboBox(final E[] items) {
    this(Arrays.asList(items));
  }

  /**
   * create the CheckComboBox with the given items
   *
   * @param items
   *          a {@link Collection} of initial items
   */
  public TmmCheckComboBox(final Collection<E> items) {
    this.model = new DefaultComboBoxModel<>();
    setModel(model);

    this.checkComboBoxItems = new ArrayList<>();
    this.comboBoxItemMap = new HashMap<>();

    init();
    setItems(items);
  }

  /**
   * initializations
   */
  protected void init() {
    setEditable(true);
    setEditor(new DefaultCheckBoxEditor());
    setRenderer();
    addActionListener(this);
  }

  public void setSingleLineEditor() {
    setEditor(new SingleLineCheckBoxEditor());
  }

  public void enableFilter(BiPredicate<E, String> userFilter) {
    decorator = TmmCheckComboBoxFilterDecorator.decorate(this, userFilter);
  }

  @Override
  public void doLayout() {
    super.doLayout();
  }

  /**
   * set new items to the CheckComboBox
   *
   * @param items
   *          the items to be set
   */
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
      this.checkComboBoxItems.add(checkComboBoxItem);
      this.comboBoxItemMap.put(item, checkComboBoxItem);
    }

    model.addAll(checkComboBoxItems);
  }

  @Override
  public void setEditor(ComboBoxEditor anEditor) {
    super.setEditor(anEditor);
    if (anEditor instanceof JComponent) {
      this.editor = (JComponent) anEditor;
    }
  }

  /**
   * adds a new CheckComboBoxSelectionChangedListener
   * 
   * @param listener
   *          the new listener
   */
  public void addSelectionChangedListener(TmmCheckComboBoxSelectionListener listener) {
    if (listener == null) {
      return;
    }
    changedListeners.add(listener);
  }

  /**
   * remove the given CheckComboBoxSelectionChangedListener
   * 
   * @param listener
   *          the listener to be removed
   */
  public void removeSelectionChangedListener(TmmCheckComboBoxSelectionListener listener) {
    changedListeners.remove(listener);
  }

  /**
   * get a {@link List} of all checked items
   * 
   * @return an array of all checked items
   */
  public List<E> getSelectedItems() {
    List<E> selected = new ArrayList<>();

    for (TmmCheckComboBoxItem<E> item : checkComboBoxItems) {
      if (item.isSelected() && item.getUserObject() != null) {
        selected.add(item.getUserObject());
      }
    }

    return selected;
  }

  /**
   * get a {@link List} of all items
   *
   * @return an array of all checked items
   */
  public List<E> getItems() {
    List<E> items = new ArrayList<>();

    for (TmmCheckComboBoxItem<E> item : checkComboBoxItems) {
      if (item.getUserObject() != null) {
        items.add(item.getUserObject());
      }
    }

    return items;
  }

  /**
   * set selected items
   * 
   * @param items
   *          the items to be set as selected
   */
  public void setSelectedItems(Collection<E> items) {
    if (items == null || items.isEmpty()) {
      return;
    }

    int n = model.getSize();
    boolean dirty = false;

    for (int i = 0; i < n; i++) {
      TmmCheckComboBoxItem<E> cb = model.getElementAt(i);
      if (cb == nullItem) {
        continue;
      }

      boolean oldState = cb.isSelected();

      if (items.contains(cb.getUserObject())) {
        cb.setSelected(true);
        if (!oldState) {
          dirty = true;
        }

      }
      else {
        cb.setSelected(false);

        if (oldState) {
          dirty = true;
        }
      }
    }

    if (dirty) {
      // update state of the "select all" and "select none" items
      // Select all
      model.getElementAt(0).setSelected(items.size() == n - 3);
      // select none
      model.getElementAt(1).setSelected(items.isEmpty());

      update();
    }
  }

  /**
   * clear all selections in this check combo box
   */
  public void clearSelection() {
    int n = model.getSize();
    boolean dirty = false;

    for (int i = 0; i < n; i++) {
      TmmCheckComboBoxItem<E> cb = model.getElementAt(i);
      if (cb == nullItem) {
        continue;
      }

      boolean oldState = cb.isSelected();
      cb.setSelected(false);

      if (oldState) {
        dirty = true;
      }
    }

    if (dirty) {
      // update state of the "select all" and "select none" items
      // Select all
      model.getElementAt(0).setSelected(false);
      // select none
      model.getElementAt(1).setSelected(true);

      update();
    }
  }

  /**
   * set selected items
   * 
   * @param items
   *          the items to be set as selected
   */
  public void setSelectedItems(E[] items) {
    setSelectedItems(Arrays.asList(items));
  }

  protected void update() {
    // force the JComboBox to re-calculate the size
    revalidate();
    repaint();

    if (isEditable()) {
      getEditor().setItem(null);
    }
  }

  /**
   * set the right renderer for this check combo box
   */
  protected void setRenderer() {
    setRenderer(new CheckBoxRenderer());
  }

  protected void checkBoxSelectionChanged(int index) {
    int n = model.getSize();
    if (index < 0 || index >= n) {
      return;
    }

    // Set selectedObj = getSelected();
    if (index > 1) {
      TmmCheckComboBoxItem<E> cb = model.getElementAt(index);
      if (cb == nullItem) {
        return;
      }

      if (cb.isSelected()) {
        cb.setSelected(false);

        // Select all
        model.getElementAt(0).setSelected(false);
        // select none
        model.getElementAt(1).setSelected(getSelectedItems() == null);
      }
      else {
        cb.setSelected(true);

        List<E> sobjs = getSelectedItems();
        // Select all
        model.getElementAt(0).setSelected(sobjs != null && sobjs.size() == n - 3);
        // select none
        model.getElementAt(1).setSelected(false);
      }
    }
    else if (index == 0) {
      for (int i = 0; i < n; i++) {
        if (model.getElementAt(i) != nullItem) {
          model.getElementAt(i).setSelected(true);
        }
      }
      model.getElementAt(1).setSelected(false);
    }
    else if (index == 1) {
      for (int i = 0; i < n; i++) {
        if (model.getElementAt(i) != nullItem) {
          model.getElementAt(i).setSelected(false);
        }
      }
      model.getElementAt(1).setSelected(true);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    int sel = getSelectedIndex();

    if (sel < 0) {
      getUI().setPopupVisible(this, false);
    }
    else {
      checkBoxSelectionChanged(sel);
      for (TmmCheckComboBoxSelectionListener listener : changedListeners) {
        listener.selectionChanged(sel);
      }
    }

    this.setSelectedIndex(-1); // clear selection
  }

  @Override
  public void setPopupVisible(boolean flag) {
    // leave empty
  }

  /*
   * helper classes
   */
  /**
   * checkbox renderer for combobox
   */
  protected class CheckBoxRenderer implements ListCellRenderer<TmmCheckComboBoxItem<E>> {
    protected final ListCellRenderer defaultRenderer;
    protected final JSeparator       separator;

    public CheckBoxRenderer() {
      separator = new JSeparator(HORIZONTAL);

      // get the default renderer from a JComboBox
      defaultRenderer = new JComboBox<E>().getRenderer();
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

  protected class DefaultCheckBoxEditor extends JPanel implements ComboBoxEditor {
    private Dimension cachedLayoutSize;

    public DefaultCheckBoxEditor() {
      super();

      setLayout(new WrapLayout(FlowLayout.LEFT, 5, 2));
      setOpaque(false);
      setBorder(BorderFactory.createEmptyBorder(-2, -5, 2, 0)); // to avoid space of the first/last item
    }

    @Override
    public Component getEditorComponent() {
      return this;
    }

    @Override
    public void doLayout() {
      super.doLayout();

      // re-layout on height change
      if (cachedLayoutSize == null || cachedLayoutSize.height != getMinimumSize().height) {
        firePropertyChange("border", false, true); // trigger re-layout via the property border
        cachedLayoutSize = getMinimumSize();
      }
    }

    @Override
    public void setItem(Object anObject) {
      removeAll();

      List<E> objs = getSelectedItems();
      if (objs.isEmpty()) {
        add(new JLabel(TmmResourceBundle.getString("ComboBox.select")));
      }
      else {
        for (E obj : objs) {
          add(getEditorItem(obj));
        }
      }

      // force the JComboBox to re-calculate the size
      firePropertyChange("border", true, false);
      revalidate();
    }

    protected JComponent getEditorItem(E userObject) {
      return new CheckBoxEditorItem(userObject);
    }

    @Override
    public Object getItem() {
      return getSelectedItems();
    }

    @Override
    public void selectAll() {

    }

    @Override
    public void addActionListener(ActionListener l) {

    }

    @Override
    public void removeActionListener(ActionListener l) {

    }
  }

  protected class CheckBoxEditorItem extends JPanel {

    public CheckBoxEditorItem(E userObject) {
      super();
      putClientProperty("class", "roundedPanel");
      putClientProperty("borderRadius", 6);
      setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
      setBorder(null);

      JLabel label = new JLabel(userObject.toString());
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
            TmmCheckComboBox.this.update();
            TmmCheckComboBox.this.setSelectedIndex(-1); // clear selection
          }
        }
      });
      add(button);
    }
  }

  protected class SingleLineCheckBoxEditor extends DefaultCheckBoxEditor {
    public SingleLineCheckBoxEditor() {
      super();

      setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
      setOpaque(false);
      setBorder(BorderFactory.createEmptyBorder(-2, -5, 2, 0)); // to avoid space of the first/last item
    }

    @Override
    public void setItem(Object anObject) {
      removeAll();

      List<E> objs = getSelectedItems();
      if (objs.isEmpty()) {
        add(new JLabel(TmmResourceBundle.getString("ComboBox.select")));
      }
      else {
        int widthAvailable = getWidth() - getFontMetrics(getFont()).stringWidth(" ... ");
        int usedWidth = 0;

        for (E obj : objs) {
          JComponent editorItem = getEditorItem(obj);

          if (usedWidth + editorItem.getMinimumSize().width < widthAvailable) {
            add(editorItem);
            usedWidth += editorItem.getMinimumSize().width;
          }
          else {
            add(new JLabel("..."));
            break;
          }
        }
      }

      // force the JComboBox to re-calculate the size
      firePropertyChange("border", true, false);
      revalidate();
    }
  }
}
