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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.tinymediamanager.core.TmmResourceBundle;

/**
 * the class {@link MemoryUsagePanel} is used to print the memory statistics to the status bar
 * 
 * @author Manuel Laggner
 */
class MemoryUsagePanel extends JPanel {
  private static final int MB = 1048576;

  private final Timer      timer;
  private final JLabel     lblMemory;

  private long             maxMem;
  private long             totalMem;
  private long             freeMem;
  private long             usedMem;

  public MemoryUsagePanel() {
    setLayout(new FlowLayout());

    setOpaque(false);
    setFocusable(false);

    getMemory();

    lblMemory = new JLabel();
    lblMemory.setHorizontalTextPosition(SwingConstants.CENTER);
    lblMemory.setMinimumSize(getLabelMinimumSize());
    lblMemory.setOpaque(false);
    add(lblMemory);

    setMemoryText();

    timer = new Timer(1000, null);
    timer.addActionListener(evt -> {
      getMemory();
      setMemoryText();
      repaint();
    });

    ToolTipManager.sharedInstance().registerComponent(this);
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      timer.start();
    }
    else {
      timer.stop();
    }
  }

  private void getMemory() {
    Runtime rt = Runtime.getRuntime();
    totalMem = rt.totalMemory();
    maxMem = rt.maxMemory(); // = Xmx
    freeMem = rt.freeMemory();

    // see http://stackoverflow.com/a/18375641
    usedMem = totalMem - freeMem;
  }

  private void setMemoryText() {
    lblMemory.setText(usedMem / MB + " / " + maxMem / MB + "M");
  }

  @Override
  public void paintComponent(Graphics g) {
    Dimension size = getSize();
    int barWidth = size.width;

    int usedBarLength = (int) (barWidth * usedMem / maxMem);

    // gauge (used)
    g.setColor(UIManager.getColor("Panel.tmmAlternateBackground"));
    g.fillRect(0, 0, usedBarLength, size.height - 1);

    // text
    super.paintComponent(g);
  }

  @Override
  public String getToolTipText() {
    long megs = 1048576;

    // see http://stackoverflow.com/a/18375641
    long used = totalMem - freeMem;
    long free = maxMem - used;

    String phys = "";
    return TmmResourceBundle.getString("tmm.memoryused") + " " + used / megs + " MiB  /  " + TmmResourceBundle.getString("tmm.memoryfree") + " "
        + free / megs + " MiB  /  " + TmmResourceBundle.getString("tmm.memorymax") + " " + maxMem / megs + " MiB" + phys;
  }

  private Dimension getLabelMinimumSize() {
    String text = maxMem * 10 / MB + " / " + maxMem / MB + "M";
    Insets insets = lblMemory.getInsets();
    int width = insets.left + insets.right + lblMemory.getFontMetrics(lblMemory.getFont()).stringWidth(text);
    int height = lblMemory.getMinimumSize().height;
    return new Dimension(width, height);
  }
}
