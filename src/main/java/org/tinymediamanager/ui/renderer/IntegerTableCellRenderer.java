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
package org.tinymediamanager.ui.renderer;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * This renderer is used to display {@link Integer}s - hide zero values
 * 
 * @author Manuel Laggner
 */
public class IntegerTableCellRenderer extends DefaultTableCellRenderer {

  public IntegerTableCellRenderer() {
    setHorizontalAlignment(RIGHT);
  }

  /**
   * Returns the component used for drawing the cell.
   */
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
    if (value instanceof Integer integer) {
      if (integer > 0) {
        setText(String.valueOf(integer));
      }
      else {
        setText(null);
      }
    }
    else {
      setText(null);
    }
    return this;
  }
}
