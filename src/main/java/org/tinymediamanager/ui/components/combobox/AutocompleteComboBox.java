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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collection;

import javax.swing.JComboBox;

import org.jetbrains.annotations.Nullable;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

/**
 * The Class AutocompleteComboBox.
 * 
 * @author Manuel Laggner
 */
public class AutocompleteComboBox<E> extends JComboBox<E> {
  private final EventList<E>     items;
  private AutocompleteSupport<E> autoCompleteSupport;

  public AutocompleteComboBox(Collection<E> items) {
    super();
    this.items = new BasicEventList<>();
    this.items.addAll(items);
    init();
  }

  public AutocompleteComboBox(E[] items) {
    super();
    this.items = GlazedLists.eventListOf(items);
    init();
  }

  private void init() {
    setEditable(true);
    this.items.sort((o1, o2) -> o1.toString().compareToIgnoreCase(o2.toString()));
    this.autoCompleteSupport = AutocompleteSupport.install(this, items);

    // fix: add a focus listener for the editor component to request the focus
    // AutocompleteSupport removed that for some reason
    getEditor().getEditorComponent().addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        repaint();
      }

      @Override
      public void focusLost(FocusEvent e) {
        // nothing to do
      }
    });
  }

  @Nullable
  @Override
  public Object getSelectedItem() {
    Object selectedItem = super.getSelectedItem();

    if (selectedItem == null && isEditable()) {
      selectedItem = getEditor().getItem();
    }

    return selectedItem;
  }

  public AutocompleteSupport<E> getAutoCompleteSupport() {
    return autoCompleteSupport;
  }
}
