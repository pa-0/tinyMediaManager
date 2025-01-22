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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.WolDevice;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link WolDevicePanel} is used to maintain WOL (Wake on Lan) devices
 * 
 * @author Manuel Laggner
 */
public class WolDevicePanel extends AbstractModalInputPanel {
  private final WolDevice  device;

  private final JTextField tfName;
  private final JTextField tfMacAddress;

  public WolDevicePanel(WolDevice device) {
    super();
    this.device = device;

    {
      setLayout(new MigLayout("", "[][]", "[][]"));

      JLabel lblDeviceName = new JLabel(TmmResourceBundle.getString("Settings.devicename"));
      add(lblDeviceName, "cell 0 0,alignx right");

      tfName = new JTextField();
      tfName.setColumns(20);
      add(tfName, "cell 1 0");

      JLabel lblMacAddress = new JLabel(TmmResourceBundle.getString("Settings.macaddress"));
      add(lblMacAddress, "cell 0 1,alignx right");

      tfMacAddress = new JTextField();
      tfMacAddress.setColumns(20);
      add(tfMacAddress, "cell 1 1");
    }

    this.tfName.setText(device.getName());
    this.tfMacAddress.setText(device.getMacAddress());
  }

  @Override
  protected void onClose() {
    // check whether both fields are filled
    if (StringUtils.isBlank(tfName.getText()) || StringUtils.isBlank(tfMacAddress.getText())) {
      JOptionPane.showMessageDialog(this, TmmResourceBundle.getString("message.missingitems"));
      return;
    }

    // check MAC address with regexp
    Pattern pattern = Pattern.compile("^([0-9a-fA-F]{2}[:-]){5}([0-9a-fA-F]{2})$");
    Matcher matcher = pattern.matcher(tfMacAddress.getText());
    if (!matcher.matches()) {
      JOptionPane.showMessageDialog(this, TmmResourceBundle.getString("message.invalidmac"));
      return;
    }

    device.setName(tfName.getText());
    device.setMacAddress(tfMacAddress.getText());

    setVisible(false);
  }
}
