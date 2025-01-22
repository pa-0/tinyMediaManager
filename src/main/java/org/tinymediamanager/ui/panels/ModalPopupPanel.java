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
package org.tinymediamanager.ui.panels;

import static javax.swing.SwingConstants.CENTER;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;

import org.tinymediamanager.ui.EqualsLayout;
import org.tinymediamanager.ui.TmmFontHelper;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link ModalPopupPanel} is used to enable a Material UI like popup panel which is embedded into the surrounding *
 * {@link javax.swing.JFrame}/{@link javax.swing.JDialog}
 * 
 * @author Manuel Laggner
 */
public class ModalPopupPanel extends JPanel {
  private final IModalPopupPanelProvider popupPanelProvider;

  private final JLabel                   lblTitle;
  private final JPanel                   contentPanel;
  private final JPanel                   buttonPanel;

  private Runnable                       onCloseHandler;
  private Runnable                       onCancelHandler;

  public ModalPopupPanel(IModalPopupPanelProvider parent) {
    this(parent, null);
  }

  public ModalPopupPanel(IModalPopupPanelProvider parent, String title) {
    super(false);

    this.popupPanelProvider = parent;

    setOpaque(false);
    addMouseListener(new MouseAdapter() {
      // prevent clicks from going through the panel
    });

    setLayout(new MigLayout("", "[center, grow]", "[center, grow]"));

    JPanel layoutPanel = new JPanel() {
      @Override
      protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);

        // background
        Graphics2D g2d = (Graphics2D) g1.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        Dimension arcs = new Dimension(15, 15);
        int width = getWidth();
        int height = getHeight();

        // Draws the rounded opaque panel with borders.
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height);// paint background
        g2d.dispose();
      }
    };
    layoutPanel.setOpaque(false);
    layoutPanel.setLayout(new BorderLayout());
    layoutPanel.setBorder(new EmptyBorder(5, 20, 5, 20));
    add(layoutPanel, "cell 0 0");

    lblTitle = new JLabel(title);
    lblTitle.setHorizontalAlignment(CENTER);
    TmmFontHelper.changeFont(lblTitle, TmmFontHelper.H3, Font.BOLD);
    layoutPanel.add(lblTitle, BorderLayout.NORTH);

    contentPanel = new JPanel();
    layoutPanel.add(contentPanel, BorderLayout.CENTER);

    {
      JPanel bottomPanel = new JPanel();
      layoutPanel.add(bottomPanel, BorderLayout.SOUTH);
      bottomPanel.setLayout(new MigLayout("insets n 0 0 0, gap rel 0", "[grow][]", "[shrink 0][]"));

      JSeparator separator = new JSeparator();
      bottomPanel.add(separator, "cell 0 0 2 1,growx");

      buttonPanel = new JPanel();
      EqualsLayout layout = new EqualsLayout(5);
      layout.setMinWidth(100);
      buttonPanel.setLayout(layout);
      buttonPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
      bottomPanel.add(buttonPanel, "cell 1 1");
    }
  }

  @Override
  protected void paintComponent(Graphics g1) {
    super.paintComponent(g1);

    // background
    Graphics2D g = (Graphics2D) g1.create();
    g.setPaint(Color.black);
    Composite savedComposite = g.getComposite();
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
    g.fillRect(0, 0, getWidth(), getHeight());
    g.setComposite(savedComposite);
    g.dispose();
  }

  public void setTitle(String title) {
    lblTitle.setText(title);
  }

  public void setContent(IModalPopupPanel panel) {
    JComponent content = panel.getContent();
    content.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentHidden(ComponentEvent e) {
        popupPanelProvider.hideModalPopupPanel(ModalPopupPanel.this);
        if (panel.isCancelled()) {
          onCancel();
        }
        else {
          onClose();
        }
      }
    });
    contentPanel.add(content);

    if (panel.getCancelButton() != null) {
      buttonPanel.add(panel.getCancelButton());
    }
    if (panel.getCloseButton() != null) {
      buttonPanel.add(panel.getCloseButton());

      for (Component component : content.getComponents()) {
        component.addKeyListener(new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
              panel.getCloseButton().doClick();
            }
          }
        });
      }
    }
  }

  public void setOnCloseHandler(Runnable onCloseHandler) {
    this.onCloseHandler = onCloseHandler;
  }

  public void setOnCancelHandler(Runnable onCancelHandler) {
    this.onCancelHandler = onCancelHandler;
  }

  protected void onClose() {
    if (onCloseHandler != null) {
      onCloseHandler.run();
    }
  }

  protected void onCancel() {
    if (onCancelHandler != null) {
      onCancelHandler.run();
    }
  }
}
