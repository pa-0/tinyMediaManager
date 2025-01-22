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

import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaTrailer;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link TrailerEditorPanel} is used to add/edit trailers
 *
 * @author Manuel Laggner
 */
public class TrailerEditorPanel extends AbstractModalInputPanel {
  private final MediaTrailer mediaTrailer;

  private final JTextField   tfName;
  private final JTextField   tfSource;
  private final JTextField   tfQuality;
  private final JTextField   tfUrl;
  private final JTextField   tfId;

  public TrailerEditorPanel(MediaTrailer mediaTrailer) {
    super();

    this.mediaTrailer = mediaTrailer;

    {
      setLayout(new MigLayout("", "[][800lp,grow]", "[][][][]"));
      {
        JLabel nameT = new JLabel(TmmResourceBundle.getString("metatag.name"));
        add(nameT, "cell 0 0,alignx trailing");

        tfName = new JTextField();
        add(tfName, "cell 1 0,growx,wmin 0");
      }
      {
        JLabel sourceT = new JLabel(TmmResourceBundle.getString("metatag.source"));
        add(sourceT, "cell 0 1,alignx trailing");

        tfSource = new JTextField();
        add(tfSource, "cell 1 1,growx");
      }
      {
        JLabel lblQualityT = new JLabel(TmmResourceBundle.getString("metatag.quality"));
        add(lblQualityT, "cell 0 2,alignx trailing");

        tfQuality = new JTextField();
        tfQuality.setColumns(10);
        add(tfQuality, "cell 1 2");
      }
      {
        JLabel lblUrlT = new JLabel(TmmResourceBundle.getString("metatag.url"));
        add(lblUrlT, "cell 0 3");

        tfUrl = new JTextField();
        tfUrl.setColumns(10);
        add(tfUrl, "cell 1 3,growx,wmin 0");
      }
      {
        JLabel lblIdT = new JLabel(TmmResourceBundle.getString("metatag.id"));
        add(lblIdT, "cell 0 4");

        tfId = new JTextField();
        tfId.setColumns(10);
        add(tfId, "cell 1 4,growx,wmin 0");
      }
    }

    tfName.setText(mediaTrailer.getName());
    tfSource.setText(mediaTrailer.getProvider());
    tfQuality.setText(mediaTrailer.getQuality());
    tfUrl.setText(mediaTrailer.getUrl());
    tfId.setText(mediaTrailer.getId());

    // set focus to the first textfield
    SwingUtilities.invokeLater(tfName::requestFocus);
  }

  @Override
  protected void onClose() {
    // Url OR id must be set
    if (StringUtils.isAllBlank(tfId.getText(), tfUrl.getText())) {
      JOptionPane.showMessageDialog(TrailerEditorPanel.this, TmmResourceBundle.getString("message.missingitems"));
      return;
    }
    if (StringUtils.isBlank(tfName.getText())) {
      JOptionPane.showMessageDialog(TrailerEditorPanel.this, TmmResourceBundle.getString("message.missingitems"));
      return;
    }

    mediaTrailer.setName(tfName.getText());
    mediaTrailer.setProvider(tfSource.getText());
    mediaTrailer.setQuality(tfQuality.getText());
    mediaTrailer.setUrl(tfUrl.getText());
    mediaTrailer.setId(tfId.getText());
    mediaTrailer.setDate(new Date());

    setVisible(false);
  }
}
