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

import javax.swing.DefaultListSelectionModel;
import javax.swing.event.ListSelectionListener;

/**
 * the class {@link NullSelectionModel} is used to suppress selection of a row in a table
 * 
 * @author Manuel Laggner
 */
public class NullSelectionModel extends DefaultListSelectionModel {
  @Override
  public boolean isSelectionEmpty() {
    return true;
  }

  @Override
  public boolean isSelectedIndex(int index) {
    return false;
  }

  @Override
  public int getMinSelectionIndex() {
    return -1;
  }

  @Override
  public int getMaxSelectionIndex() {
    return -1;
  }

  @Override
  public int getLeadSelectionIndex() {
    return -1;
  }

  @Override
  public int getAnchorSelectionIndex() {
    return -1;
  }

  @Override
  public void setSelectionInterval(int index0, int index1) {
    // nothing to do
  }

  @Override
  public void setLeadSelectionIndex(int index) {
    // nothing to do
  }

  @Override
  public void setAnchorSelectionIndex(int index) {
    // nothing to do
  }

  @Override
  public void addSelectionInterval(int index0, int index1) {
    // nothing to do
  }

  @Override
  public void insertIndexInterval(int index, int length, boolean before) {
    // nothing to do
  }

  @Override
  public void clearSelection() {
    // nothing to do
  }

  @Override
  public void removeSelectionInterval(int index0, int index1) {
    // nothing to do
  }

  @Override
  public void removeIndexInterval(int index0, int index1) {
    // nothing to do
  }

  @Override
  public void setSelectionMode(int selectionMode) {
    // nothing to do
  }

  @Override
  public int getSelectionMode() {
    return SINGLE_SELECTION;
  }

  @Override
  public void addListSelectionListener(ListSelectionListener lsl) {
    // nothing to do
  }

  @Override
  public void removeListSelectionListener(ListSelectionListener lsl) {
    // nothing to do
  }

  @Override
  public void setValueIsAdjusting(boolean valueIsAdjusting) {
    // nothing to do
  }

  @Override
  public boolean getValueIsAdjusting() {
    return false;
  }
}
