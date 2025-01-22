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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.sql.Date;
import java.time.LocalDate;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.license.License;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.actions.UnlockAction;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.images.Logo;

import net.miginfocom.swing.MigLayout;

/**
 * The Class AboutDialog.
 * 
 * @author Manuel Laggner
 */
public class AboutDialog extends TmmDialog {
  private static final Logger LOGGER = LoggerFactory.getLogger(AboutDialog.class);

  public AboutDialog() {
    super(TmmResourceBundle.getString("tmm.about"), "aboutDialog");

    LocalDate validUntil = License.getInstance().validUntil();

    JPanel contentPanel = new JPanel();
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new MigLayout("", "[][20lp:n][300lp,grow]", "[][10lp:n][][20lp:n][][][][10lp:n][][10lp:n][][][][][]"));
    {
      JLabel lblLogo = new JLabel("");
      lblLogo.setIcon(new Logo(96));
      contentPanel.add(lblLogo, "cell 0 0 1 8,alignx left,aligny top");
    }
    {
      JLabel lblTinymediamanager = new JLabel("tinyMediaManager");
      TmmFontHelper.changeFont(lblTinymediamanager, 1.5, Font.BOLD);
      contentPanel.add(lblTinymediamanager, "cell 2 0,alignx center");
    }
    {
      JLabel lblByManuel = new JLabel("\u00A9 2012 - 2025 by Manuel Laggner");
      contentPanel.add(lblByManuel, "cell 2 2,alignx center");
    }
    {
      String version = TmmResourceBundle.getString("tmm.version") + ": " + ReleaseInfo.getRealVersion();

      if (validUntil != null) {
        version += " / " + License.getInstance().ref();
      }

      JLabel lblVersion = new JLabel(version);
      contentPanel.add(lblVersion, "cell 2 4");
    }
    {
      JLabel lblBuild = new JLabel(TmmResourceBundle.getString("tmm.builddate") + ": " + ReleaseInfo.getRealBuildDate());
      contentPanel.add(lblBuild, "cell 2 5");
    }
    {
      JLabel lblLicense = new JLabel("");
      TmmFontHelper.changeFont(lblLicense, Font.BOLD);
      contentPanel.add(lblLicense, "cell 2 6");

      if (validUntil != null) {
        lblLicense.setText(
            TmmResourceBundle.getString("tmm.license.validuntil") + ": " + TmmDateFormat.MEDIUM_DATE_FORMAT.format(Date.valueOf(validUntil)));
        JButton btnLicenseInfo = new FlatButton("");
        btnLicenseInfo.setAction(new UnlockAction());
        btnLicenseInfo.setText(null);
        btnLicenseInfo.setIcon(IconManager.INFO);
        btnLicenseInfo.setToolTipText(TmmResourceBundle.getString("tmm.license.view"));
        contentPanel.add(btnLicenseInfo, "cell 2 6");
      }
      else {
        lblLicense.setText(TmmResourceBundle.getString("tmm.license.unregistered"));
      }
    }
    {
      JLabel lblHomepage = new TmmLabel(TmmResourceBundle.getString("tmm.homepage"));
      contentPanel.add(lblHomepage, "cell 0 8,alignx right");
    }
    {
      final LinkLabel lblHomepage = new LinkLabel("https://www.tinymediamanager.org/");
      lblHomepage.addActionListener(arg0 -> {
        try {
          TmmUIHelper.browseUrl(lblHomepage.getText());
        }
        catch (Exception e) {
          LOGGER.error(e.getMessage());
          MessageManager.instance.pushMessage(
              new Message(MessageLevel.ERROR, lblHomepage.getText(), "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));
        }
      });
      contentPanel.add(lblHomepage, "cell 2 8");
    }
    {
      JLabel lblThanksTo = new TmmLabel(TmmResourceBundle.getString("tmm.thanksto"));
      contentPanel.add(lblThanksTo, "cell 0 10,alignx right");
    }
    {
      JLabel lblMyronForHelping = new JLabel("Myron for helping me with coding, scrapers, localization, setup, everything...");
      contentPanel.add(lblMyronForHelping, "cell 2 10");
    }
    {
      JLabel lblJoostzilla = new JLabel("Joostzilla for the UI design");
      contentPanel.add(lblJoostzilla, "cell 2 11");
    }
    {
      JLabel lblTranslators = new JLabel("All our translators");
      contentPanel.add(lblTranslators, "cell 2 12");
    }
    {
      JLabel lblLibs = new JLabel("The creators of all libs we've used");
      contentPanel.add(lblLibs, "cell 2 13");
    }
    {
      JLabel lblTester = new JLabel("Everyone who tested and provided feedback");
      contentPanel.add(lblTester, "cell 2 14");
    }
    {
      JButton okButton = new JButton();
      Action action = new CloseAction();
      okButton.setAction(action);
      addDefaultButton(okButton);
    }
  }

  private class CloseAction extends AbstractAction {
    CloseAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.close"));
      putValue(SMALL_ICON, IconManager.APPLY_INV);
      putValue(LARGE_ICON_KEY, IconManager.APPLY_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      setVisible(false);
    }
  }
}
