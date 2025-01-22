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
package org.tinymediamanager.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.ui.FlatDropShadowBorder;
import com.formdev.flatlaf.ui.FlatEmptyBorder;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link HintManager} is used to show balloon like tooltips to help the user starting with tinyMediaManager
 *
 * @author Karl Tauber
 * @author Manuel Laggner
 */
public class HintManager {
  private static HintManager instance;
  private final Queue<Hint>  hints;

  private HintManager() {
    hints = new LinkedList<>();
  }

  public static HintManager getInstance() {
    if (instance == null) {
      instance = new HintManager();
    }

    return instance;
  }

  public void addHint(String message, Component owner, int position) {
    if (Settings.getInstance().isNewConfig()) { // to avoid direct references
      hints.add(new Hint(message, owner, position));
    }
  }

  public void showHints() {
    nextHint();
  }

  private void nextHint() {
    Hint hint = hints.poll();
    if (hint != null) {
      showHint(hint);
    }
  }

  private void showHint(Hint hint) {
    HintPanel hintPanel = new HintPanel(hint);
    hintPanel.showHint();
  }

  // ---- class HintPanel ----------------------------------------------------

  static class Hint {
    private final String    message;
    private final Component owner;
    private final int       position;

    Hint(String message, Component owner, int position) {
      this.message = message;
      this.owner = owner;
      this.position = position;
    }
  }

  // ---- class HintPanel ----------------------------------------------------

  private class HintPanel extends JPanel {
    private final Hint hint;

    private JPanel     popup;
    private JLabel     hintLabel;

    private HintPanel(Hint hint) {
      this.hint = hint;

      initComponents();

      setOpaque(false);
      updateBalloonBorder();

      hintLabel.setText("<html>" + hint.message + "</html>");

      // grab all mouse events to avoid that components overlapped
      // by the hint panel receive them
      addMouseListener(new MouseAdapter() {
      });
    }

    @Override
    public void updateUI() {
      super.updateUI();

      if (UIManager.getLookAndFeel() instanceof FlatLaf)
        setBackground(UIManager.getColor("HintPanel.backgroundColor"));
      else {
        // using nonUIResource() because otherwise Nimbus does not fill the background
        setBackground(FlatUIUtils.nonUIResource(UIManager.getColor("info")));
      }

      if (hint != null)
        updateBalloonBorder();
    }

    private void updateBalloonBorder() {
      int direction;
      switch (hint.position) {
        case SwingConstants.LEFT:
          direction = SwingConstants.RIGHT;
          break;

        case SwingConstants.TOP:
          direction = SwingConstants.BOTTOM;
          break;

        case SwingConstants.RIGHT:
          direction = SwingConstants.LEFT;
          break;

        case SwingConstants.BOTTOM:
          direction = SwingConstants.TOP;
          break;

        default:
          throw new IllegalArgumentException();
      }

      setBorder(new BalloonBorder(direction, FlatUIUtils.getUIColor("PopupMenu.borderColor", Color.gray)));
    }

    void showHint() {
      JRootPane rootPane = SwingUtilities.getRootPane(hint.owner);
      if (rootPane == null) {
        return;
      }

      JLayeredPane layeredPane = rootPane.getLayeredPane();

      // create a popup panel that has a drop shadow
      popup = new JPanel(new BorderLayout()) {
        @Override
        public void updateUI() {
          super.updateUI();

          // use invokeLater because at this time the UI delegates
          // of child components are not yet updated
          EventQueue.invokeLater(() -> {
            validate();
            setSize(getPreferredSize());
          });
        }
      };
      popup.setOpaque(false);
      popup.add(this);

      // calculate x/y location for hint popup
      Point pt = SwingUtilities.convertPoint(hint.owner, 0, 0, layeredPane);
      int x = pt.x;
      int y = pt.y;
      Dimension size = popup.getPreferredSize();
      int gap = UIScale.scale(6);

      switch (hint.position) {
        case SwingConstants.LEFT:
          x -= size.width + gap;
          break;

        case SwingConstants.TOP:
          y -= size.height + gap;
          break;

        case SwingConstants.RIGHT:
          x += hint.owner.getWidth() + gap;
          break;

        case SwingConstants.BOTTOM:
          if (hint.owner instanceof JTabbedPane tabbedPane) {
            Rectangle rect = tabbedPane.getUI().getTabBounds(tabbedPane, 0);
            y += rect.height + gap;
            x += rect.width - 2 * gap;
          }
          else if (hint.owner instanceof JButton) {
            y += hint.owner.getHeight() - gap;
            x += hint.owner.getWidth() / 2 - (UIScale.scale(BalloonBorder.ARROW_XY) + UIScale.scale(BalloonBorder.ARROW_SIZE) + gap);
          }
          else {
            y += hint.owner.getHeight() + gap;
          }
          break;

        default:
          throw new IllegalStateException();
      }

      // set hint popup size and show it
      popup.setBounds(x, y, size.width, size.height);
      layeredPane.add(popup, JLayeredPane.POPUP_LAYER);
    }

    void hideHint() {
      if (popup != null) {
        Container parent = popup.getParent();
        if (parent != null) {
          parent.remove(popup);
          parent.repaint(popup.getX(), popup.getY(), popup.getWidth(), popup.getHeight());
        }
      }
    }

    private void close() {
      // hide hint
      hideHint();

      // show next hint (if any)
      nextHint();
    }

    private void initComponents() {
      // JFormDesigner - Component initialization - DO NOT MODIFY //GEN-BEGIN:initComponents
      hintLabel = new JLabel();
      JButton btnClose = new JButton();

      // ======== this ========
      setLayout(new MigLayout("insets dialog,hidemode 3", "[::200,fill]", "[]para" + "[]"));

      // ---- hintLabel ----
      hintLabel.setText("hint");
      add(hintLabel, "cell 0 0");

      // ---- gotItButton ----
      btnClose.setText(TmmResourceBundle.getString("hintmanager.gotit"));
      btnClose.setFocusable(false);
      btnClose.addActionListener(e -> close());
      add(btnClose, "cell 0 1,alignx right,growx 0");
      // JFormDesigner - End of component initialization //GEN-END:initComponents
    }

  }

  // ---- class BalloonBorder ------------------------------------------------

  private static class BalloonBorder extends FlatEmptyBorder {
    private static final int ARC             = 8;
    private static final int ARROW_XY        = 16;
    private static final int ARROW_SIZE      = 8;
    private static final int SHADOW_SIZE     = 6;
    private static final int SHADOW_TOP_SIZE = 3;

    private final int        direction;
    private final Color      borderColor;

    private final Border     shadowBorder;

    public BalloonBorder(int direction, Color borderColor) {
      super(1 + SHADOW_TOP_SIZE, 1 + SHADOW_SIZE, 1 + SHADOW_SIZE, 1 + SHADOW_SIZE);

      this.direction = direction;
      this.borderColor = borderColor;

      switch (direction) {
        case SwingConstants.LEFT:
          left += ARROW_SIZE;
          break;

        case SwingConstants.TOP:
          top += ARROW_SIZE;
          break;

        case SwingConstants.RIGHT:
          right += ARROW_SIZE;
          break;

        case SwingConstants.BOTTOM:
          bottom += ARROW_SIZE;
          break;

        default:
          throw new IllegalStateException();
      }

      int SHADOW_SIZE2 = SHADOW_SIZE + 2;
      shadowBorder = UIManager.getLookAndFeel() instanceof FlatLaf ? new FlatDropShadowBorder(UIManager.getColor("Popup.dropShadowColor"),
          new Insets(SHADOW_SIZE2, SHADOW_SIZE2, SHADOW_SIZE2, SHADOW_SIZE2), FlatUIUtils.getUIFloat("Popup.dropShadowOpacity", 0.5f)) : null;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      Graphics2D g2 = (Graphics2D) g.create();
      try {
        FlatUIUtils.setRenderingHints(g2);
        g2.translate(x, y);

        // shadow coordinates
        int sx = 0;
        int sy = 0;
        int sw = width;
        int sh = height;
        int arrowSize = UIScale.scale(ARROW_SIZE);
        switch (direction) {
          case SwingConstants.LEFT:
            sx += arrowSize;
            sw -= arrowSize;
            break;

          case SwingConstants.TOP:
            sy += arrowSize;
            sh -= arrowSize;
            break;

          case SwingConstants.RIGHT:
            sw -= arrowSize;
            break;

          case SwingConstants.BOTTOM:
            sh -= arrowSize;
            break;
        }

        // paint shadow
        if (shadowBorder != null) {
          shadowBorder.paintBorder(c, g2, sx, sy, sw, sh);
        }

        // create balloon shape
        int bx = UIScale.scale(SHADOW_SIZE);
        int by = UIScale.scale(SHADOW_TOP_SIZE);
        int bw = width - UIScale.scale(SHADOW_SIZE + SHADOW_SIZE);
        int bh = height - UIScale.scale(SHADOW_TOP_SIZE + SHADOW_SIZE);
        g2.translate(bx, by);
        Shape shape = createBalloonShape(bw, bh);

        // fill balloon background
        g2.setColor(c.getBackground());
        g2.fill(shape);

        // paint balloon border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(UIScale.scale(1f)));
        g2.draw(shape);
      }
      finally {
        g2.dispose();
      }
    }

    private Shape createBalloonShape(int width, int height) {
      int arc = UIScale.scale(ARC);
      int xy = UIScale.scale(ARROW_XY);
      int awh = UIScale.scale(ARROW_SIZE);

      Shape rect;
      Shape arrow;
      switch (direction) {
        case SwingConstants.LEFT:
          rect = new RoundRectangle2D.Float(awh, 0, width - 1 - awh, height - 1, arc, arc);
          arrow = FlatUIUtils.createPath(awh, xy, 0, xy + awh, awh, xy + awh + awh);
          break;

        case SwingConstants.TOP:
          rect = new RoundRectangle2D.Float(0, awh, width - 1, height - 1 - awh, arc, arc);
          arrow = FlatUIUtils.createPath(xy, awh, xy + awh, 0, xy + awh + awh, awh);
          break;

        case SwingConstants.RIGHT:
          rect = new RoundRectangle2D.Float(0, 0, width - 1 - awh, height - 1, arc, arc);
          int x = width - 1 - awh;
          arrow = FlatUIUtils.createPath(x, xy, x + awh, xy + awh, x, xy + awh + awh);
          break;

        case SwingConstants.BOTTOM:
          rect = new RoundRectangle2D.Float(0, 0, width - 1, height - 1 - awh, arc, arc);
          int y = height - 1 - awh;
          arrow = FlatUIUtils.createPath(xy, y, xy + awh, y + awh, xy + awh + awh, y);
          break;

        default:
          throw new IllegalStateException();
      }

      Area area = new Area(rect);
      area.add(new Area(arrow));
      return area;
    }
  }
}
