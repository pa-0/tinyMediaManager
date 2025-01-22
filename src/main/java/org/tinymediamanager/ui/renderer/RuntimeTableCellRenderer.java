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
import java.util.concurrent.TimeUnit;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * This renderer is used to display runtimes in a customizeable way
 * 
 * @author Manuel Laggner
 */
public class RuntimeTableCellRenderer extends DefaultTableCellRenderer {
  public enum FORMAT {
    MINUTES,
    HOURS_MINUTES
  }

  private final FORMAT format;

  /**
   * Create a new RuntimeTableCellRenderer that renders runtimes as formatted Strings.
   */
  public RuntimeTableCellRenderer(FORMAT format) {
    this.format = format;
    setHorizontalAlignment(RIGHT);
  }

  /**
   * Returns the component used for drawing the cell.
   */
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
    if (value instanceof Integer) {
      int runtime = (int) value;
      long h = TimeUnit.MINUTES.toHours(runtime);
      long m = TimeUnit.MINUTES.toMinutes(runtime - TimeUnit.HOURS.toMinutes(h));

      if (format == FORMAT.MINUTES) {
        setText(value.toString());
      }
      else if (format == FORMAT.HOURS_MINUTES) {
        setText(String.format("%2d:%02d", h, m));
      }
    }
    else {
      setText("");
    }
    return this;
  }
}
