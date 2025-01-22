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
import java.awt.Dimension;
import java.awt.Window;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;

import net.miginfocom.swing.MigLayout;

/**
 * The class MessageDialog. To display messages nicely
 * 
 * @author Manuel Laggner
 */
public class MessageDialog extends TmmDialog {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageDialog.class);

  private final JLabel        lblImage;
  private final JTextPane     tpText;
  private final JTextPane     tpDescription;
  private final JScrollPane   scrollPane;
  private final JTextPane     textPane;
  private final LinkLabel     lblLink;

  public MessageDialog(Window owner, String title) {
    super(owner, title, "messageDialog");
    setModal(false);
    setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);

    {
      JPanel panelContent = new JPanel();
      panelContent.setLayout(new MigLayout("hidemode 3", "[][600lp:800lp,grow]", "[][][][400lp:600lp,grow]"));
      getContentPane().add(panelContent, BorderLayout.CENTER);
      {
        lblImage = new JLabel("");
        lblImage.setVisible(false);
        panelContent.add(lblImage, "cell 0 0 1 2,grow");
      }
      {
        tpText = new ReadOnlyTextPane("");
        tpText.setVisible(false);
        panelContent.add(tpText, "cell 1 0,growx");
      }
      {
        tpDescription = new ReadOnlyTextPane("");
        tpDescription.setEditable(true);
        tpDescription.setVisible(false);
        panelContent.add(tpDescription, "cell 1 1,growx");
      }
      {
        lblLink = new LinkLabel();
        lblLink.setVisible(false);
        lblLink.addActionListener(arg0 -> {
          try {
            TmmUIHelper.browseUrl(lblLink.getText());
          }
          catch (Exception e) {
            LOGGER.error(e.getMessage());
            MessageManager.instance.pushMessage(
                new Message(Message.MessageLevel.ERROR, lblLink.getText(), "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));
          }
        });
        panelContent.add(lblLink, "cell 1 2");
      }
      {
        scrollPane = new NoBorderScrollPane();
        scrollPane.setVisible(false);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        panelContent.add(scrollPane, "cell 0 3 2 1,grow");
        {
          textPane = new JTextPane();
          textPane.setVisible(false);
          textPane.setEditable(false);
          scrollPane.setViewportView(textPane);
        }
      }
    }
    {
      JButton btnClose = new JButton(TmmResourceBundle.getString("Button.close"));
      btnClose.addActionListener(arg0 -> setVisible(false));
      addDefaultButton(btnClose);
    }
  }

  public void setImage(Icon icon) {
    lblImage.setIcon(icon);
    lblImage.setVisible(true);
  }

  public void setText(String text) {
    tpText.setText(text);
    tpText.setVisible(true);
  }

  public void setDescription(String description) {
    tpDescription.setText(description);
    tpDescription.setVisible(true);
  }

  public void setLink(String link) {
    lblLink.setText(link);
    lblLink.setVisible(true);
  }

  public void setDetails(String details) {
    textPane.setText(details);
    textPane.setVisible(true);
    textPane.setCaretPosition(0);
    scrollPane.setVisible(true);
  }

  public static void showExceptionWindow(Throwable ex) {
    MessageDialog dialog = new MessageDialog(null, TmmResourceBundle.getString("tmm.problemdetected"));

    dialog.setImage(IconManager.ERROR);
    if (ex instanceof OutOfMemoryError) {
      dialog.setDescription(TmmResourceBundle.getString("tmm.oom"));
      dialog.setLink("https://www.tinymediamanager.org/help/faq#java-heap-space-errors");
    }
    else {
      String msg = ex.getLocalizedMessage();
      dialog.setText(msg != null ? msg : "");
      dialog.setDescription(TmmResourceBundle.getString("tmm.uicrash"));
    }
    dialog.setDetails(stackStraceAsString(ex));
    dialog.setVisible(true);
  }

  private static String stackStraceAsString(Throwable ex) {
    StringWriter sw = new StringWriter();
    ex.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }
}
