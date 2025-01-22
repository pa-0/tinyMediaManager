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
package org.tinymediamanager.ui.moviesets;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.treetable.TmmTreeTableCellRenderer;

/**
 * The class MovieSetTreeCellRenderer. Just for modifying the color of dummy movies
 * 
 * @author Manuel Laggner
 */
public class MovieSetTreeCellRenderer extends TmmTreeTableCellRenderer {
  private Color colorDummy;

  @Override
  public void updateUI() {
    super.updateUI();
    colorDummy = UIManager.getColor("Component.linkColor");
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
        column);

    if (value instanceof MovieSetTreeDataProvider.MovieTreeNode) {
      if (((MovieSetTreeDataProvider.MovieTreeNode) value).getUserObject() instanceof MovieSet.MovieSetMovie) {
        renderer.setForeground(colorDummy);
      }

      if (((MovieSetTreeDataProvider.MovieTreeNode) value).getUserObject() instanceof Movie) {
        Movie movie = (Movie) ((MovieSetTreeDataProvider.MovieTreeNode) value).getUserObject();
        if (movie.isLocked()) {
          setIcon(IconManager.LOCK_BLUE);
        }
        else {
          setIcon(null);
        }
      }
    }

    return renderer;
  }

  @Override
  public String getToolTipText() {
    if (!MovieModuleManager.getInstance().getSettings().isShowMovieSetTableTooltips()) {
      return null;
    }

    return super.getToolTipText();
  }
}
