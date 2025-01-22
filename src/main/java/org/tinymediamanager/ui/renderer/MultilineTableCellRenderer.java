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
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * the class {@link MultilineTableCellRenderer} is used to render table cells with multiple lines; ATTENTION multiple lines will always be rendered on
 * top and not centered
 * 
 * @author Manuel Laggner
 */
public class MultilineTableCellRenderer extends JTextPane implements TableCellRenderer {
  public MultilineTableCellRenderer() {
    setForeground(null);
    setBackground(null);
    setOpaque(false);
    setAlignmentY(CENTER_ALIGNMENT);
    setEditorKit(new CenteredEditorKit());
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    if (isSelected) {
      setForeground(table.getSelectionForeground());
      setBackground(table.getSelectionBackground());
    }
    else {
      setForeground(table.getForeground());
      setBackground(table.getBackground());
    }
    setFont(table.getFont());
    if (hasFocus) {
      setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
      if (table.isCellEditable(row, column)) {
        setForeground(UIManager.getColor("Table.focusCellForeground"));
        setBackground(UIManager.getColor("Table.focusCellBackground"));
      }
    }
    else {
      setBorder(new EmptyBorder(1, 2, 1, 2));
    }
    setText((value == null) ? "" : value.toString());

    return this;
  }

  private static class CenteredEditorKit extends StyledEditorKit {

    @Override
    public ViewFactory getViewFactory() {
      return new StyledViewFactory();
    }

    static class StyledViewFactory implements ViewFactory {

      public View create(Element elem) {
        String kind = elem.getName();
        if (kind != null) {
          if (kind.equals(AbstractDocument.ContentElementName)) {
            return new LabelView(elem);
          }
          else if (kind.equals(AbstractDocument.ParagraphElementName)) {
            return new ParagraphView(elem);
          }
          else if (kind.equals(AbstractDocument.SectionElementName)) {
            return new CenteredBoxView(elem, View.Y_AXIS);
          }
          else if (kind.equals(StyleConstants.ComponentElementName)) {
            return new ComponentView(elem);
          }
          else if (kind.equals(StyleConstants.IconElementName)) {
            return new IconView(elem);
          }
        }

        return new LabelView(elem);
      }
    }
  }

  private static class CenteredBoxView extends BoxView {
    public CenteredBoxView(Element elem, int axis) {
      super(elem, axis);
    }

    @Override
    protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
      super.layoutMajorAxis(targetSpan, axis, offsets, spans);
      int textBlockHeight = 0;
      int offset = 0;

      for (int i = 0; i < spans.length; i++) {
        textBlockHeight += spans[i];
      }
      offset = (targetSpan - textBlockHeight) / 2;
      for (int i = 0; i < offsets.length; i++) {
        offsets[i] += offset;
      }
    }
  }
}
