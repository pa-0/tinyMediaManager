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

import java.awt.Font;
import java.text.DateFormat;
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;

import net.miginfocom.swing.MigLayout;

public class MessagePanel extends JPanel {
  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages"); // direct access to the message ids is needed here

  private JLabel                      lblTitle;
  private JTextPane                   taMessage;
  private JLabel                      lblIcon;
  private JLabel                      lblDate;

  public MessagePanel(Message message) {
    setOpaque(false);
    initComponents();
    // init data
    DateFormat dateFormat = TmmDateFormat.MEDIUM_TIME_FORMAT;
    lblDate.setText(dateFormat.format(message.getMessageDate()));

    String text = "";
    if (message.getMessageSender() instanceof MediaEntity mediaEntity) {
      // mediaEntity title: eg. Movie title
      text = mediaEntity.getTitle();
    }
    else if (message.getMessageSender() instanceof MediaFile mediaFile) {
      // mediaFile: filename
      text = mediaFile.getFilename();
    }
    else {
      try {
        text = Utils.replacePlaceholders(BUNDLE.getString(message.getMessageSender().toString()), message.getSenderParams());
      }
      catch (Exception e) {
        text = String.valueOf(message.getMessageSender());
      }
    }
    lblTitle.setText(text);

    text = "";
    try {
      // try to get a localized version
      text = Utils.replacePlaceholders(BUNDLE.getString(message.getMessageId()), message.getIdParams());
    }
    catch (Exception e) {
      // simply take the id
      text = message.getMessageId();
    }
    taMessage.setText(text);

    switch (message.getMessageLevel()) {
      case ERROR:
        lblIcon.setIcon(IconManager.ERROR);
        break;

      case WARN:
        lblIcon.setIcon(IconManager.WARN);
        break;

      case INFO:
        lblIcon.setIcon(IconManager.INFO);
        break;

      default:
        lblIcon.setIcon(null);
        break;
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[150lp:200lp,grow][]", "[][shrink 0]"));

    lblDate = new JLabel("");
    add(lblDate, "cell 1 0");

    JPanel innerPanel = new JPanel();
    add(innerPanel, "cell 0 0,growx");
    innerPanel.setLayout(new MigLayout("", "[1px][][150lp:200lp,grow]", "[][]"));

    lblIcon = new JLabel("");
    lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
    innerPanel.add(lblIcon, "cell 1 0,alignx center,aligny center");

    lblTitle = new JLabel();
    TmmFontHelper.changeFont(lblTitle, Font.BOLD);

    innerPanel.add(lblTitle, "cell 2 0,wmin 0,growx");

    taMessage = new ReadOnlyTextPane();
    innerPanel.add(taMessage, "cell 2 1,wmin 0,grow");

    add(new JSeparator(), "cell 0 1 2 1,growx");
  }
}
