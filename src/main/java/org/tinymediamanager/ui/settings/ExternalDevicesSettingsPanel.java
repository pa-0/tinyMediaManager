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

import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.swingbinding.JTableBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.WolDevice;
import org.tinymediamanager.jsonrpc.config.HostConfig;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.panels.IModalPopupPanelProvider;
import org.tinymediamanager.ui.panels.ModalPopupPanel;
import org.tinymediamanager.ui.panels.WolDevicePanel;

import net.miginfocom.swing.MigLayout;

/**
 * The {@link ExternalDevicesSettingsPanel} - a panel to configure external devices
 * 
 * @author Manuel Laggner
 */
class ExternalDevicesSettingsPanel extends JPanel {
  private static final Logger LOGGER   = LoggerFactory.getLogger(ExternalDevicesSettingsPanel.class);

  private final Settings      settings = Settings.getInstance();

  private JTable              tableWolDevices;
  private JTextField          tfKodiHost;
  private JTextField          tfKodiTcpPort;
  private JTextField          tfKodiHttpPort;
  private JTextField          tfKodiUsername;
  private JPasswordField      tfKodiPassword;
  private JButton             btnRemoveWolDevice;
  private JButton             btnAddWolDevice;
  private JButton             btnEditWolDevice;
  private JCheckBox           chckbxUpnpShareLibrary;
  private JCheckBox           chckbxUpnpRemotePlay;

  ExternalDevicesSettingsPanel() {

    // UI init
    initComponents();
    initDataBindings();

    // button listeners
    btnAddWolDevice.addActionListener(arg0 -> {
      IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(this);
      if (iModalPopupPanelProvider == null) {
        return;
      }

      ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
      popupPanel.setTitle(TmmResourceBundle.getString("tmm.regexp"));

      WolDevice wolDevice = new WolDevice();
      WolDevicePanel wolDevicePanel = new WolDevicePanel(wolDevice);

      popupPanel.setOnCloseHandler(() -> settings.addWolDevices(wolDevice));

      popupPanel.setContent(wolDevicePanel);
      iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
    });

    btnRemoveWolDevice.addActionListener(e -> {
      int row = tableWolDevices.getSelectedRow();
      row = tableWolDevices.convertRowIndexToModel(row);
      if (row != -1) {
        WolDevice device = Settings.getInstance().getWolDevices().get(row);
        Settings.getInstance().removeWolDevices(device);
      }
    });

    btnEditWolDevice.addActionListener(e -> {
      int row = tableWolDevices.getSelectedRow();
      row = tableWolDevices.convertRowIndexToModel(row);
      if (row != -1) {
        WolDevice device = Settings.getInstance().getWolDevices().get(row);
        if (device != null) {
          IModalPopupPanelProvider iModalPopupPanelProvider = IModalPopupPanelProvider.findModalProvider(this);
          if (iModalPopupPanelProvider == null) {
            return;
          }

          ModalPopupPanel popupPanel = iModalPopupPanelProvider.createModalPopupPanel();
          popupPanel.setTitle(TmmResourceBundle.getString("tmm.regexp"));

          WolDevice wolDevice = new WolDevice(device);
          WolDevicePanel wolDevicePanel = new WolDevicePanel(wolDevice);

          popupPanel.setOnCloseHandler(() -> {
            device.setName(wolDevice.getName());
            device.setMacAddress(wolDevice.getMacAddress());
          });

          popupPanel.setContent(wolDevicePanel);
          iModalPopupPanelProvider.showModalPopupPanel(popupPanel);
        }
      }
    });

    // set column titles
    tableWolDevices.getColumnModel().getColumn(0).setHeaderValue(TmmResourceBundle.getString("Settings.devicename"));
    tableWolDevices.getColumnModel().getColumn(1).setHeaderValue(TmmResourceBundle.getString("Settings.macaddress"));
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][][15lp!][]"));
    {
      JPanel panelWol = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][400lp][]", "[100lp]"));

      JLabel lblWolT = new TmmLabel(TmmResourceBundle.getString("tmm.wakeonlan"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelWol, lblWolT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#wake-on-lan"));
      add(collapsiblePanel, "growx,wmin 0");
      {
        JScrollPane spWolDevices = new JScrollPane();
        panelWol.add(spWolDevices, "cell 1 0,grow");

        tableWolDevices = new TmmTable();
        spWolDevices.setViewportView(tableWolDevices);

        btnAddWolDevice = new JButton(TmmResourceBundle.getString("Button.add"));
        panelWol.add(btnAddWolDevice, "flowy,cell 2 0,growx,aligny top");

        btnEditWolDevice = new JButton(TmmResourceBundle.getString("Button.edit"));
        panelWol.add(btnEditWolDevice, "cell 2 0,growx");

        btnRemoveWolDevice = new JButton(TmmResourceBundle.getString("Button.remove"));
        panelWol.add(btnRemoveWolDevice, "cell 2 0,growx");
      }
    }
    {
      JPanel panelKodi = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][15lp][][]", "[]"));

      JLabel lblKodiT = new TmmLabel("Kodi / XBMC", H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelKodi, lblKodiT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#kodixbmc"));
      add(collapsiblePanel, "cell 0 2,growx,wmin 0");
      {
        JLabel lblKodiHostT = new JLabel(TmmResourceBundle.getString("Settings.kodi.host"));
        panelKodi.add(lblKodiHostT, "cell 1 0");

        tfKodiHost = new JTextField();
        panelKodi.add(tfKodiHost, "cell 2 0");
        tfKodiHost.setColumns(20);

        JButton btnKodiConnect = new JButton(TmmResourceBundle.getString("Settings.kodi.connect"));
        btnKodiConnect.addActionListener(e -> {
          HostConfig c = new HostConfig(tfKodiHost.getText(), tfKodiHttpPort.getText(), tfKodiTcpPort.getText(), tfKodiUsername.getText(),
              new String(tfKodiPassword.getPassword()));
          try {
            KodiRPC.getInstance().connect(c);
          }
          catch (Exception cex) {
            LOGGER.error("Error connecting to Kodi instance! {}", cex.getMessage());
            MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "KodiRPC", "Could not connect to Kodi: " + cex.getMessage()));
          }
        });
        panelKodi.add(btnKodiConnect, "cell 3 0,growx");

        JLabel lblKodiHttpPortT = new JLabel(TmmResourceBundle.getString("Settings.kodi.httpport"));
        panelKodi.add(lblKodiHttpPortT, "cell 1 1");

        tfKodiHttpPort = new JTextField();
        panelKodi.add(tfKodiHttpPort, "cell 2 1");
        tfKodiHttpPort.setColumns(20);

        JButton btnKodiDisconnect = new JButton(TmmResourceBundle.getString("Settings.kodi.disconnect"));
        btnKodiDisconnect.addActionListener(e -> KodiRPC.getInstance().disconnect());
        panelKodi.add(btnKodiDisconnect, "cell 3 1,growx");

        JLabel lblKodiTcpPortT = new JLabel(TmmResourceBundle.getString("Settings.kodi.tcpport"));
        panelKodi.add(lblKodiTcpPortT, "cell 1 2");

        tfKodiTcpPort = new JTextField();
        panelKodi.add(tfKodiTcpPort, "cell 2 2");
        tfKodiTcpPort.setColumns(20);

        JLabel lblKodiUsernameT = new JLabel(TmmResourceBundle.getString("Settings.kodi.user"));
        panelKodi.add(lblKodiUsernameT, "cell 1 3");

        tfKodiUsername = new JTextField();
        panelKodi.add(tfKodiUsername, "cell 2 3");
        tfKodiUsername.setColumns(20);

        JLabel lblKodiPasswordT = new JLabel(TmmResourceBundle.getString("Settings.kodi.pass"));
        panelKodi.add(lblKodiPasswordT, "cell 1 4");

        tfKodiPassword = new JPasswordField();
        panelKodi.add(tfKodiPassword, "cell 2 4");
        tfKodiPassword.setColumns(20);
      }
    }
    {
      JPanel panelUpnp = new JPanel();
      panelUpnp.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "")); // 16lp ~ width of the

      JLabel lblUpnp = new TmmLabel("UPnP", H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelUpnp, lblUpnp, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#upnp"));
      add(collapsiblePanel, "cell 0 4,growx,wmin 0");
      {
        chckbxUpnpShareLibrary = new JCheckBox(TmmResourceBundle.getString("Settings.upnp.share"));
        panelUpnp.add(chckbxUpnpShareLibrary, "cell 1 0 2 1");

        chckbxUpnpRemotePlay = new JCheckBox(TmmResourceBundle.getString("Settings.upnp.play"));
        panelUpnp.add(chckbxUpnpRemotePlay, "cell 1 1 2 1");
      }
    }
  }

  protected void initDataBindings() {
    BeanProperty<Settings, List<WolDevice>> settingsBeanProperty = BeanProperty.create("wolDevices");
    JTableBinding<WolDevice, Settings, JTable> jTableBinding = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty, tableWolDevices);
    //
    BeanProperty<WolDevice, String> wolBeanProperty_1 = BeanProperty.create("name");
    jTableBinding.addColumnBinding(wolBeanProperty_1);
    //
    BeanProperty<WolDevice, String> wolBeanProperty_2 = BeanProperty.create("macAddress");
    jTableBinding.addColumnBinding(wolBeanProperty_2);
    //
    jTableBinding.setEditable(false);
    jTableBinding.bind();
    //
    BeanProperty<Settings, String> settingsBeanProperty_2 = BeanProperty.create("kodiUsername");
    BeanProperty<JTextField, String> jTextFieldBeanProperty_1 = BeanProperty.create("text");
    AutoBinding<Settings, String, JTextField, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_2, tfKodiUsername, jTextFieldBeanProperty_1);
    autoBinding_1.bind();
    //
    BeanProperty<Settings, String> settingsBeanProperty_3 = BeanProperty.create("kodiPassword");
    BeanProperty<JPasswordField, String> jPasswordFieldBeanProperty = BeanProperty.create("text");
    AutoBinding<Settings, String, JPasswordField, String> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_3, tfKodiPassword, jPasswordFieldBeanProperty);
    autoBinding_2.bind();
    //
    BeanProperty<Settings, Boolean> settingsBeanProperty_4 = BeanProperty.create("upnpRemotePlay");
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding<Settings, Boolean, JCheckBox, Boolean> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_4, chckbxUpnpRemotePlay, jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    BeanProperty<Settings, Boolean> settingsBeanProperty_5 = BeanProperty.create("upnpShareLibrary");
    AutoBinding<Settings, Boolean, JCheckBox, Boolean> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_5, chckbxUpnpShareLibrary, jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    BeanProperty<Settings, String> settingsBeanProperty_1 = BeanProperty.create("kodiHost");
    BeanProperty<JTextField, String> jTextFieldBeanProperty = BeanProperty.create("text");
    AutoBinding<Settings, String, JTextField, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_1, tfKodiHost, jTextFieldBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<Settings, Integer> settingsBeanProperty_6 = BeanProperty.create("kodiHttpPort");
    BeanProperty<JTextField, String> jTextFieldBeanProperty_2 = BeanProperty.create("text");
    AutoBinding<Settings, Integer, JTextField, String> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_6, tfKodiHttpPort, jTextFieldBeanProperty_2);
    autoBinding_5.bind();
    //
    BeanProperty<Settings, Integer> settingsBeanProperty_7 = BeanProperty.create("kodiTcpPort");
    BeanProperty<JTextField, String> jTextFieldBeanProperty_3 = BeanProperty.create("text");
    AutoBinding<Settings, Integer, JTextField, String> autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_7, tfKodiTcpPort, jTextFieldBeanProperty_3);
    autoBinding_6.bind();
  }
}
