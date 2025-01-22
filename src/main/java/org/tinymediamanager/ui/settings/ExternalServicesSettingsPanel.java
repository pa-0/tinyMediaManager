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
package org.tinymediamanager.ui.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;
import static org.tinymediamanager.ui.TmmFontHelper.L2;

import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.DateField;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.license.License;
import org.tinymediamanager.thirdparty.trakttv.TraktTv;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class ExternalServicesSettingsPanel. Handle all settings for the external services
 * 
 * @author Manuel Laggner
 */
class ExternalServicesSettingsPanel extends JPanel {
  private final Settings settings = Settings.getInstance();

  private JButton        btnGetTraktPin;
  private JButton        btnTestTraktConnection;
  private JLabel         lblTraktStatus;
  private JTextField     tfMdbListApiKey;
  private JComboBox      cbTraktDate;
  private AutoBinding    autobinding_1;

  ExternalServicesSettingsPanel() {
    // UI init
    initComponents();

    // data init
    if (License.getInstance().isValidLicense()
        && StringUtils.isNoneBlank(Settings.getInstance().getTraktAccessToken(), Settings.getInstance().getTraktRefreshToken())) {
      lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.good"));
    }
    else {
      lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.bad"));
    }

    btnGetTraktPin.addActionListener(e -> getTraktPin());
    btnGetTraktPin.setEnabled(License.getInstance().isValidLicense());
    btnTestTraktConnection.addActionListener(e -> {
      try {
        TraktTv.getInstance().refreshAccessToken();
        JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.trakt.testconnection.good"),
            TmmResourceBundle.getString("Settings.trakt.testconnection"), JOptionPane.INFORMATION_MESSAGE);
      }
      catch (Exception e1) {
        JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.trakt.testconnection.bad"),
            TmmResourceBundle.getString("Settings.trakt.testconnection"), JOptionPane.ERROR_MESSAGE);
      }
    });
    btnTestTraktConnection.setEnabled(License.getInstance().isValidLicense());
  }

  private void getTraktPin() {
    // open the pin url in a browser
    try {
      TmmUIHelper.browseUrl("https://trakt.tv/pin/799");
    }
    catch (Exception e1) {
      // browser could not be opened, show a dialog box
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.trakt.getpin.fallback"),
          TmmResourceBundle.getString("Settings.trakt.getpin"), JOptionPane.INFORMATION_MESSAGE);
    }

    // let the user insert the pin
    String pin = JOptionPane.showInputDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.trakt.getpin.entercode"));

    // user clicked abort
    if (pin == null || pin.isEmpty()) {
      return;
    }

    // try to get the tokens
    String accessToken = "";
    String refreshToken = "";
    try {
      Map<String, String> tokens = TraktTv.getInstance().authenticateViaPin(pin);
      accessToken = tokens.get("accessToken") == null ? "" : tokens.get("accessToken");
      refreshToken = tokens.get("refreshToken") == null ? "" : tokens.get("refreshToken");
    }
    catch (Exception ignored) {
      // ignored
    }

    if (StringUtils.isNoneBlank(accessToken, refreshToken)) {
      Settings.getInstance().setTraktAccessToken(accessToken);
      Settings.getInstance().setTraktRefreshToken(refreshToken);
      lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.good"));
    }
    else {
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.trakt.getpin.problem"),
          TmmResourceBundle.getString("Settings.trakt.getpin"), JOptionPane.ERROR_MESSAGE);

      if (StringUtils.isNoneBlank(Settings.getInstance().getTraktAccessToken(), Settings.getInstance().getTraktRefreshToken())) {
        // we got an error, but we already have old setted-up tokens, so display msg accordingly
        lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.good"));
      }
      else {
        lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.bad"));
      }
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][]"));
    {
      JPanel panelTrakt = new JPanel();
      panelTrakt.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][10lp!][]")); // 16lp ~ width of the

      JLabel lblTraktT = new TmmLabel(TmmResourceBundle.getString("Settings.trakt"), H3);

      if (!License.getInstance().isValidLicense()) {
        lblTraktT.setText("*PRO* " + lblTraktT.getText());
      }

      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelTrakt, lblTraktT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#trakttv"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        lblTraktStatus = new JLabel("");
        panelTrakt.add(lblTraktStatus, "cell 1 0 2 1");
      }
      {
        btnGetTraktPin = new JButton(TmmResourceBundle.getString("Settings.trakt.getpin"));
        panelTrakt.add(btnGetTraktPin, "cell 1 1 2 1");

        btnTestTraktConnection = new JButton(TmmResourceBundle.getString("Settings.trakt.testconnection"));
        panelTrakt.add(btnTestTraktConnection, "cell 1 1 2 1");
      }

      JLabel lblTraktDateT = new TmmLabel(TmmResourceBundle.getString("Settings.trakt.date"));
      panelTrakt.add(lblTraktDateT, "flowx,cell 1 3 2 1");

      cbTraktDate = new JComboBox(DateField.values());
      panelTrakt.add(cbTraktDate, "cell 1 3 2 1");
    }
    {
      JPanel panelMdbList = new JPanel();
      panelMdbList.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[]"));
      JLabel lblMdbListT = new TmmLabel(TmmResourceBundle.getString("Settings.external.rating.mdblist"), H3);

      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMdbList, lblMdbListT, true);
      add(collapsiblePanel, "cell 0 2,growx, wmin 0");
      {
        JLabel lblMdbListApiKeyT = new JLabel(TmmResourceBundle.getString("Settings.api.key"));
        panelMdbList.add(lblMdbListApiKeyT, "cell 1 0 2 1");

        tfMdbListApiKey = new JTextField();
        panelMdbList.add(tfMdbListApiKey, "cell 1 0 2 1");
        tfMdbListApiKey.setColumns(30);

        JTextArea tpMediaPlayer = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.external.rating.mdblist.hint"));
        panelMdbList.add(tpMediaPlayer, "cell 1 1 2 1,growx, wmin 0");
        TmmFontHelper.changeFont(tpMediaPlayer, L2);
      }
    }
    initDataBindings();
  }

  protected void initDataBindings() {
    Property settingsBeanProperty = BeanProperty.create("traktDateField");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty, cbTraktDate,
        jComboBoxBeanProperty);
    autoBinding.bind();

    Property settingsBeanProperty_1 = BeanProperty.create("mdbListApiKey");
    Property jTextFieldBeanProperty = BeanProperty.create("text");
    AutoBinding autobinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_1, tfMdbListApiKey,
        jTextFieldBeanProperty);
    autobinding_1.bind();
  }
}
