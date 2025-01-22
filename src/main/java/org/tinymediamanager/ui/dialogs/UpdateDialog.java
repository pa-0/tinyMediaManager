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
import java.awt.Font;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.updater.UpdaterTask;

import net.miginfocom.swing.MigLayout;

/**
 * The class UpdateDialog. Used to show the user that an update is available
 * 
 * @author Manuel Laggner
 */
public class UpdateDialog extends TmmDialog {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDialog.class);

  public UpdateDialog(String changelog, String baseurl) {
    super(TmmResourceBundle.getString("tmm.update.title"), "update");

    {
      JPanel panelContent = new JPanel();
      getContentPane().add(panelContent, BorderLayout.CENTER);
      panelContent.setLayout(new MigLayout("", "[600lp,grow]", "[][10lp:n][][400lp,grow]"));

      JTextPane lblUpdateInfo = new ReadOnlyTextPane();
      lblUpdateInfo.setText(TmmResourceBundle.getString("tmm.update.message"));
      panelContent.add(lblUpdateInfo, "cell 0 0,growx");

      JLabel lblChangelog = new JLabel(TmmResourceBundle.getString("whatsnew.title"));
      panelContent.add(lblChangelog, "cell 0 2,growx");

      JScrollPane scrollPane = new NoBorderScrollPane();
      panelContent.add(scrollPane, "cell 0 3,grow");
      JTextPane textPane = new JTextPane();
      textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Settings.getInstance().getFontSize() + 1));
      scrollPane.setViewportView(textPane);

      textPane.setContentType("text/html");
      textPane.setText(prepareTextAsHtml(changelog));
      textPane.setEditable(false);
      textPane.setCaretPosition(0);
      textPane.addHyperlinkListener(hle -> {
        if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
          try {
            TmmUIHelper.browseUrl(hle.getURL().toString());
          }
          catch (Exception e) {
            LOGGER.error("error browsing to \"{}\" : {}", hle.getURL(), e.getMessage());
          }
        }
      });
    }
    {
      JButton btnClose = new JButton(TmmResourceBundle.getString("Button.close"));
      btnClose.addActionListener(arg0 -> setVisible(false));
      addDefaultButton(btnClose);

      JButton btnUpdate = new JButton(TmmResourceBundle.getString("Button.update"));
      btnUpdate.addActionListener(arg0 -> {
        if (Globals.isSelfUpdatable()) {
          // the installation is self updatable
          setVisible(false);
          LOGGER.info("Updating...");

          TmmTaskManager.getInstance().addDownloadTask(new UpdaterTask());
        }
        else {
          // redirect to the downloads page for the download of the new version
          try {
            TmmUIHelper.browseUrl(baseurl);
          }
          catch (Exception e) {
            LOGGER.warn("could not open '{}' - '{}'", baseurl, e.getMessage());
          }
        }
      });

      // deactivate the update button if we cannot self update and we do have a base url
      if (Globals.isDocker() || (!Globals.isSelfUpdatable() && StringUtils.isBlank(baseurl))) {
        btnUpdate.setEnabled(false);
      }

      addButton(btnUpdate);
    }
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension superPref = super.getPreferredSize();
    return new Dimension((int) (700 > superPref.getWidth() ? superPref.getWidth() : 700),
        (int) (500 > superPref.getHeight() ? superPref.getHeight() : 500));
  }

  private String prepareTextAsHtml(String originalText) {
    // mask tags
    originalText = originalText.replace("<", "&lt;").replace(">", "&gt;");

    // links
    Pattern pattern = Pattern.compile("(http[s]?://.*?)[\\n\\r\\s)]");
    Matcher matcher = pattern.matcher(originalText);
    while (matcher.find()) {
      originalText = originalText.replace(matcher.group(1), "<a href=\"" + matcher.group(1) + "\">" + matcher.group(1) + "</a>");
    }

    // issues
    pattern = Pattern.compile("(#\\d{3,5})[\\n\\r\\s)]");
    matcher = pattern.matcher(originalText);
    while (matcher.find()) {
      originalText = originalText.replace(matcher.group(1), "<a href=\"https://gitlab.com/tinyMediaManager/tinyMediaManager/-/issues/"
          + matcher.group(1).replace("#", "") + "\">" + matcher.group(1) + "</a>");
    }

    // set the foreground color of the content
    Color foreground = UIManager.getColor("TextPane.foreground");
    String color = Integer.toHexString(foreground.getRed()) + Integer.toHexString(foreground.getGreen()) + Integer.toHexString(foreground.getBlue());

    return "<html><pre style=\"color: #" + color + "\">" + originalText + "</pre><html>";
  }
}
