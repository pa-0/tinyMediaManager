/*
 * Copyright 2012 - 2024 Manuel Laggner
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

import java.awt.Dimension;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;

import org.apache.commons.lang3.SystemUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.LauncherExtraConfig;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MiscSettingsPanel.
 * 
 * @author Manuel Laggner
 */
class SystemSettingsPanel extends JPanel {
  private static final Logger  LOGGER           = LoggerFactory.getLogger(SystemSettingsPanel.class);
  private static final Pattern MEMORY_PATTERN   = Pattern.compile("-Xmx([0-9]*)(.)");
  private static final Pattern DIRECT3D_PATTERN = Pattern.compile("-Dsun.java2d.d3d=(true|false)");

  private final Settings       settings         = Settings.getInstance();

  private JTextField           tfProxyHost;
  private JTextField           tfProxyPort;
  private JTextField           tfProxyUsername;
  private JPasswordField       tfProxyPassword;

  private JSlider              sliderMemory;
  private JLabel               lblMemory;
  private JCheckBox            chckbxIgnoreSSLProblems;
  private JCheckBox            chckbxDisableD3d;
  private JSpinner             spMaximumDownloadThreads;
  private JTextField           tfHttpPort;
  private JTextField           tfHttpApiKey;
  private JCheckBox            chkbxEnableHttpServer;

  /**
   * Instantiates a new general settings panel.
   */
  SystemSettingsPanel() {

    initComponents();

    initDataBindings();

    initMemorySlider();
    initDirect3d();

    // add a listener to write the actual memory state/direct3d setting to launcher-extra.yml
    addHierarchyListener(new HierarchyListener() {
      private boolean oldState = false;

      @Override
      public void hierarchyChanged(HierarchyEvent e) {
        if (oldState != isShowing()) {
          oldState = isShowing();
          if (!isShowing()) {
            writeLauncherExtraYml();
          }
        }
      }
    });
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][][15lp!][][15lp!][]"));
    {
      JPanel panelMemory = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][][300lp][grow]", ""));

      JLabel lblMemoryT = new TmmLabel(TmmResourceBundle.getString("Settings.memoryborder"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMemory, lblMemoryT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#memory-settings"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");
      {
        lblMemoryT = new JLabel(TmmResourceBundle.getString("Settings.memory"));
        panelMemory.add(lblMemoryT, "cell 1 0,aligny top");

        sliderMemory = new JSlider();
        sliderMemory.setSnapToTicks(true);

        Dictionary<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(256, new JLabel("256"));
        labelTable.put(2048, new JLabel("2048"));
        labelTable.put(4096, new JLabel("4096"));
        labelTable.put(6144, new JLabel("6144"));
        labelTable.put(8192, new JLabel("8192"));
        sliderMemory.setLabelTable(labelTable);

        sliderMemory.setPaintTicks(true);
        sliderMemory.setPaintLabels(true);
        sliderMemory.setMinorTickSpacing(256);
        sliderMemory.setMinimum(256);
        sliderMemory.setMaximum(8192);
        sliderMemory.setValue(512);
        panelMemory.add(sliderMemory, "cell 2 0,growx,aligny top");

        lblMemory = new JLabel("512");
        panelMemory.add(lblMemory, "cell 3 0,aligny top");

        JLabel lblMb = new JLabel("MB");
        panelMemory.add(lblMb, "cell 3 0,aligny top");

        JTextArea tpMemoryHint = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.memory.hint"));
        panelMemory.add(tpMemoryHint, "cell 1 1 3 1,growx, wmin 0");
        TmmFontHelper.changeFont(tpMemoryHint, L2);
      }
    }
    {
      JPanel panelProxy = new JPanel();
      panelProxy.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][][grow]", "[][][][]")); // 16lp ~ width of the

      JLabel lblProxyT = new TmmLabel(TmmResourceBundle.getString("Settings.proxy"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelProxy, lblProxyT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#proxy-settings"));
      add(collapsiblePanel, "cell 0 2,growx,wmin 0");
      {
        JLabel lblProxyHostT = new JLabel(TmmResourceBundle.getString("Settings.proxyhost"));
        panelProxy.add(lblProxyHostT, "cell 1 0,alignx right");

        tfProxyHost = new JTextField();
        panelProxy.add(tfProxyHost, "cell 2 0");
        tfProxyHost.setColumns(20);
        lblProxyHostT.setLabelFor(tfProxyHost);

        JLabel lblProxyPortT = new JLabel(TmmResourceBundle.getString("Settings.proxyport"));
        panelProxy.add(lblProxyPortT, "cell 1 1,alignx right");
        lblProxyPortT.setLabelFor(tfProxyPort);

        tfProxyPort = new JTextField();
        panelProxy.add(tfProxyPort, "cell 2 1");
        tfProxyPort.setColumns(20);

        JLabel lblProxyUserT = new JLabel(TmmResourceBundle.getString("Settings.proxyuser"));
        panelProxy.add(lblProxyUserT, "cell 1 2,alignx right");
        lblProxyUserT.setLabelFor(tfProxyUsername);

        tfProxyUsername = new JTextField();
        panelProxy.add(tfProxyUsername, "cell 2 2");
        tfProxyUsername.setColumns(20);

        JLabel lblProxyPasswordT = new JLabel(TmmResourceBundle.getString("Settings.proxypass"));
        panelProxy.add(lblProxyPasswordT, "cell 1 3,alignx right");
        lblProxyPasswordT.setLabelFor(tfProxyPassword);

        tfProxyPassword = new JPasswordField();
        tfProxyPassword.setColumns(20);
        panelProxy.add(tfProxyPassword, "cell 2 3");
      }
    }
    {
      JPanel panelMisc = new JPanel();
      panelMisc.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][][grow]", "[][][][grow]")); // 16lp ~ width of the

      JLabel lblMiscT = new TmmLabel(TmmResourceBundle.getString("Settings.api"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMisc, lblMiscT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/http-api"));

      chkbxEnableHttpServer = new JCheckBox(TmmResourceBundle.getString("Settings.api.enable"));
      panelMisc.add(chkbxEnableHttpServer, "cell 1 0 2 1");

      JLabel lblHttpPortT = new JLabel(TmmResourceBundle.getString("Settings.api.port"));
      panelMisc.add(lblHttpPortT, "cell 1 1,alignx trailing");

      tfHttpPort = new JTextField();
      panelMisc.add(tfHttpPort, "cell 2 1");
      tfHttpPort.setColumns(10);
      tfHttpPort.addKeyListener(new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
          char c = e.getKeyChar();
          if (((c < '0') || (c > '9')) && (c != KeyEvent.VK_BACK_SPACE)) {
            e.consume(); // if it's not a number, ignore the event
          }
        }
      });

      JLabel lblHttpApiKey = new JLabel(TmmResourceBundle.getString("Settings.api.key"));
      panelMisc.add(lblHttpApiKey, "cell 1 2,alignx trailing");

      tfHttpApiKey = new JTextField();
      panelMisc.add(tfHttpApiKey, "cell 2 2");
      tfHttpApiKey.setColumns(30);

      add(collapsiblePanel, "cell 0 4,growx,wmin 0");
    }
    {
      JPanel panelMisc = new JPanel();
      panelMisc.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][]")); // 16lp ~ width of the

      JLabel lblMiscT = new TmmLabel(TmmResourceBundle.getString("Settings.misc"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMisc, lblMiscT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#misc-settings-1"));

      add(collapsiblePanel, "cell 0 6,growx,wmin 0");
      {
        JLabel lblParallelDownloadCountT = new JLabel(TmmResourceBundle.getString("Settings.paralleldownload"));
        panelMisc.add(lblParallelDownloadCountT, "cell 1 0 2 1");

        spMaximumDownloadThreads = new JSpinner(new SpinnerNumberModel(settings.getMaximumDownloadThreads(), 1, 1024, 1));
        spMaximumDownloadThreads.setMinimumSize(new Dimension(60, 20));
        panelMisc.add(spMaximumDownloadThreads, "cell 1 0 2 1");

        chckbxIgnoreSSLProblems = new JCheckBox(TmmResourceBundle.getString("Settings.ignoressl"));
        panelMisc.add(chckbxIgnoreSSLProblems, "cell 1 1 2 1");

        JTextPane tpSSLHint = new ReadOnlyTextPane();
        tpSSLHint.setText(TmmResourceBundle.getString("Settings.ignoressl.desc"));
        TmmFontHelper.changeFont(tpSSLHint, L2);
        panelMisc.add(tpSSLHint, "cell 2 2,grow");

        chckbxDisableD3d = new JCheckBox(TmmResourceBundle.getString("Settings.disabled3d"));
        if (!SystemUtils.IS_OS_WINDOWS) {
          chckbxDisableD3d.setEnabled(false);
        }
        panelMisc.add(chckbxDisableD3d, "cell 1 3 2 1");

        JTextPane tpD3dHint = new ReadOnlyTextPane();
        tpD3dHint.setText(TmmResourceBundle.getString("Settings.disabled3d.desc"));
        TmmFontHelper.changeFont(tpD3dHint, L2);
        panelMisc.add(tpD3dHint, "cell 2 4,grow");
      }
    }
  }

  private void initMemorySlider() {
    Path file = Paths.get(Globals.CONTENT_FOLDER, LauncherExtraConfig.LAUNCHER_EXTRA_YML);
    int maxMemory = 512;
    if (Files.exists(file)) {
      // parse out memory option from extra.txt
      try {
        LauncherExtraConfig extraConfig = LauncherExtraConfig.readFile(file.toFile());
        Matcher matcher = MEMORY_PATTERN.matcher(String.join("\n", extraConfig.jvmOpts));
        if (matcher.find()) {
          maxMemory = Integer.parseInt(matcher.group(1));
          String dimension = matcher.group(2);
          if ("k".equalsIgnoreCase(dimension)) {
            maxMemory /= 1024;
          }
          if ("g".equalsIgnoreCase(dimension)) {
            maxMemory *= 1024;
          }
        }
      }
      catch (Exception e) {
        maxMemory = 512;
      }
    }

    sliderMemory.setValue(maxMemory);
  }

  private void writeLauncherExtraYml() {
    int memoryAmount = sliderMemory.getValue();

    Path file = Paths.get(Globals.CONTENT_FOLDER, LauncherExtraConfig.LAUNCHER_EXTRA_YML);
    try {
      LauncherExtraConfig extraConfig = LauncherExtraConfig.readFile(file.toFile());

      // delete any old memory setting
      extraConfig.jvmOpts.removeIf(option -> MEMORY_PATTERN.matcher(option).find());

      // set the new one if it differs from the default
      if (memoryAmount != 512) {
        extraConfig.jvmOpts.add("-Xmx" + memoryAmount + "m");
      }

      // delete any old direct3d setting
      extraConfig.jvmOpts.removeIf(option -> DIRECT3D_PATTERN.matcher(option).find());

      if (chckbxDisableD3d.isSelected()) {
        extraConfig.jvmOpts.add("-Dsun.java2d.d3d=false");
      }

      // and re-write the settings
      extraConfig.save();
    }
    catch (Exception e) {
      LOGGER.warn("Could not write memory settings - {}", e.getMessage());
    }
  }

  private void initDirect3d() {
    Path file = Paths.get(Globals.CONTENT_FOLDER, LauncherExtraConfig.LAUNCHER_EXTRA_YML);
    boolean disableDirect3d = false;

    if (Files.exists(file)) {
      // parse out memory option from extra.txt
      try {
        LauncherExtraConfig extraConfig = LauncherExtraConfig.readFile(file.toFile());
        Matcher matcher = DIRECT3D_PATTERN.matcher(String.join("\n", extraConfig.jvmOpts));
        if (matcher.find()) {
          boolean valueFromYml = Boolean.parseBoolean(matcher.group(1));
          disableDirect3d = !valueFromYml;
        }
      }
      catch (Exception e) {
        // ingored
      }
    }

    chckbxDisableD3d.setSelected(disableDirect3d);
  }

  protected void initDataBindings() {
    Property settingsBeanProperty = BeanProperty.create("proxyHost");
    Property jTextFieldBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty, tfProxyHost,
        jTextFieldBeanProperty);
    autoBinding.bind();
    //
    Property settingsBeanProperty_1 = BeanProperty.create("proxyPort");
    Property jTextFieldBeanProperty_1 = BeanProperty.create("text");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_1, tfProxyPort,
        jTextFieldBeanProperty_1);
    autoBinding_1.bind();
    //
    Property settingsBeanProperty_2 = BeanProperty.create("proxyUsername");
    Property jTextFieldBeanProperty_2 = BeanProperty.create("text");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_2, tfProxyUsername,
        jTextFieldBeanProperty_2);
    autoBinding_2.bind();
    //
    Property settingsBeanProperty_3 = BeanProperty.create("proxyPassword");
    Property jPasswordFieldBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_3, tfProxyPassword,
        jPasswordFieldBeanProperty);
    autoBinding_3.bind();

    //
    Property jSliderBeanProperty = BeanProperty.create("value");
    Property jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ, sliderMemory, jSliderBeanProperty, lblMemory, jLabelBeanProperty);
    autoBinding_11.bind();
    //
    Property settingsBeanProperty_4 = BeanProperty.create("ignoreSSLProblems");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_4, chckbxIgnoreSSLProblems,
        jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property settingsBeanProperty_5 = BeanProperty.create("maximumDownloadThreads");
    Property jSpinnerBeanProperty = BeanProperty.create("value");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_5, spMaximumDownloadThreads,
        jSpinnerBeanProperty);
    autoBinding_5.bind();

    //
    Property settingsBeanProperty_9 = BeanProperty.create("enableHttpServer");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_9, chkbxEnableHttpServer,
        jCheckBoxBeanProperty);
    autoBinding_7.bind();
    //
    Property settingsBeanProperty_10 = BeanProperty.create("httpServerPort");
    Property jTextFieldBeanProperty_5 = BeanProperty.create("text");
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_10, tfHttpPort,
        jTextFieldBeanProperty_5);
    autoBinding_8.bind();
    //
    Property settingsBeanProperty_11 = BeanProperty.create("httpApiKey");
    Property jTextFieldBeanProperty_6 = BeanProperty.create("text");
    AutoBinding autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_11, tfHttpApiKey,
        jTextFieldBeanProperty_6);
    autoBinding_12.bind();
  }
}
