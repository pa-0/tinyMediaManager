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
package org.tinymediamanager.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.net.URISyntaxException;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicProgressBarUI;

import org.apache.commons.lang3.SystemUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.images.TmmSvgIcon;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TmmSplashScreen} is used to draw our own splash screen in swing
 * 
 * @author Manuel Laggner
 */
public class TmmSplashScreen extends JDialog {
  protected static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages");

  private static final Color            FOREGROUND_COLOR = new Color(134, 134, 134);

  private final JProgressBar            progressBar;
  private final JLabel                  lblText;
  private final JLabel                  lblVersion;

  public TmmSplashScreen(String version) throws URISyntaxException {
    ImageIcon splashscreen = new TmmSvgIcon(IconManager.class.getResource("images/svg/splashscreen.svg").toURI());

    setModal(false);
    setUndecorated(true);
    setDefaultLookAndFeelDecorated(false);

    {
      JLabel lblBackground = new JLabel(splashscreen);
      getContentPane().add(lblBackground, BorderLayout.CENTER);

      lblBackground.setLayout(new BorderLayout());

      JPanel panelSouth = new JPanel();
      panelSouth.setOpaque(false);
      lblBackground.add(panelSouth, BorderLayout.SOUTH);
      panelSouth.setLayout(new MigLayout("ins 20lp", "[grow,fill][]", "[][]"));

      progressBar = new JProgressBar();
      progressBar.setUI(new TmmSplashProgressBar());
      progressBar.setPreferredSize(new Dimension(200, 15));
      panelSouth.add(progressBar, "cell 0 0 2 1,growx");

      lblText = new JLabel(TmmResourceBundle.getString("splash.loading"));
      lblText.setForeground(FOREGROUND_COLOR);
      TmmFontHelper.changeFont(lblText, TmmFontHelper.L2);
      panelSouth.add(lblText, "cell 0 1,growx , wmin 0");

      lblVersion = new JLabel(version + "  "); // add two spaces to avoid clipping on 125% UI scaling. reason unknown
      lblVersion.setForeground(FOREGROUND_COLOR);
      lblVersion.setHorizontalAlignment(SwingConstants.TRAILING);
      TmmFontHelper.changeFont(lblVersion, TmmFontHelper.L2);
      panelSouth.add(lblVersion, "cell 1 1,alignx right");
    }

    // on linux we need to set a background to avoid flicker
    if (SystemUtils.IS_OS_LINUX) {
      getContentPane().setBackground(new Color(39, 39, 39));
    }
    else {
      setBackground(new Color(0, 0, 0, 0));
    }

    pack();
    setLocationRelativeTo(null);
  }

  /**
   * Set the progress AND the text at the same time
   * 
   * @param progress
   *          the progress value. 0 - 100
   * @param text
   *          the text to be displayed below the progress
   */
  public void setProgress(int progress, String text) {
    setProgress(progress);
    setText(text);
  }

  /**
   * Set the progress
   * 
   * @param progress
   *          the progress value. 0 - 100
   */
  public void setProgress(int progress) {
    progressBar.setValue(progress);
  }

  /**
   * Set the text
   * 
   * @param text
   *          the text to be displayed below the progress
   */
  public void setText(String text) {
    try {
      lblText.setText(BUNDLE.getString(text));
    }
    catch (Exception e) {
      lblText.setText(text);
    }
  }

  static class TmmSplashProgressBar extends BasicProgressBarUI {

    private static final int   PROGRESS_BAR_HEIGHT = 6;
    private static final int   PROGRESS_BAR_ARC    = 6;
    private static final Color BACKGROUND_COLOR    = new Color(20, 20, 20);

    @Override
    public void paint(Graphics g, JComponent c) {
      Graphics2D g2D = (Graphics2D) g.create();
      try {
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Insets b = progressBar.getInsets(); // area for border
        int w = progressBar.getWidth() - (b.right + b.left);
        int h = progressBar.getHeight() - (b.top + b.bottom);

        // amount of progress to draw
        int amountFull = getAmountFull(b, w, h);

        // calculate the origin for the progress bar
        int y = b.top + (h - PROGRESS_BAR_HEIGHT) / 2;

        // draw background
        g2D.setColor(BACKGROUND_COLOR);
        g2D.fillRoundRect(b.left, y, w, PROGRESS_BAR_HEIGHT, PROGRESS_BAR_ARC, PROGRESS_BAR_ARC);

        g2D.setColor(FOREGROUND_COLOR);
        g2D.fillRoundRect(b.left, y, amountFull, PROGRESS_BAR_HEIGHT, PROGRESS_BAR_ARC, PROGRESS_BAR_ARC);
      }
      finally {
        g2D.dispose();
      }
    }

    @Override
    protected void installDefaults() {
      super.installDefaults();
      progressBar.setOpaque(false);
      progressBar.setBorderPainted(false);
    }
  }
}
