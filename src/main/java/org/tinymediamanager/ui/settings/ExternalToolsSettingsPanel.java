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

import java.nio.file.Path;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.lang3.SystemUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.addon.FFmpegAddon;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The {@link ExternalToolsSettingsPanel} - a panel to configure external tools
 *
 * @author Manuel Laggner
 */
public class ExternalToolsSettingsPanel extends JPanel {
  private final Settings    settings          = Settings.getInstance();

  private final ButtonGroup buttonGroupFfmpeg = new ButtonGroup();

  private JTextField        tfMediaPlayer;
  private JTextField        tfMediaFramework;
  private JButton           btnSearchMediaPlayer;
  private JButton           btnSearchFFMpegBinary;
  private JRadioButton      rdbtnFfmpegInternal;
  private JRadioButton      rdbtnFFmpegExternal;

  ExternalToolsSettingsPanel() {
    initComponents();

    initDataBindings();

    // data init
    btnSearchMediaPlayer.addActionListener(arg0 -> {
      String path = TmmProperties.getInstance().getProperty("chooseplayer.path");
      Path file = TmmUIHelper.selectApplication(TmmResourceBundle.getString("Button.chooseplayer"), path);
      if (file != null && (Utils.isRegularFile(file) || SystemUtils.IS_OS_MAC)) {
        tfMediaPlayer.setText(file.toAbsolutePath().toString());
        TmmProperties.getInstance().putProperty("chooseplayer.path", file.getParent().toString());
      }
    });

    btnSearchFFMpegBinary.addActionListener(arg0 -> {
      String path = TmmProperties.getInstance().getProperty("chooseffmpeg.path");
      Path file = TmmUIHelper.selectFile(TmmResourceBundle.getString("Button.chooseffmpeglocation"), path, null);
      if (file != null && (Utils.isRegularFile(file) || SystemUtils.IS_OS_MAC)) {
        tfMediaFramework.setText(file.toAbsolutePath().toString());
        TmmProperties.getInstance().putProperty("chooseffmpeg.path", file.getParent().toString());
      }
    });

    // init of the radiobutton
    if (settings.isUseInternalMediaFramework()) {
      rdbtnFfmpegInternal.setSelected(true);
    }
    else {
      rdbtnFFmpegExternal.setSelected(true);
    }

    FFmpegAddon fFmpegAddon = new FFmpegAddon();
    if (fFmpegAddon.isAvailable()) {
      rdbtnFfmpegInternal.setEnabled(true);
    }
    else {
      rdbtnFFmpegExternal.setSelected(true);
      rdbtnFfmpegInternal.setEnabled(false);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][]"));
    {
      JPanel panelMediaPlayer = new JPanel();
      panelMediaPlayer.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "")); // 16lp ~ width of the

      JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("Settings.mediaplayer"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMediaPlayer, lblLanguageT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#media-player"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        tfMediaPlayer = new JTextField();
        panelMediaPlayer.add(tfMediaPlayer, "cell 1 0 2 1");
        tfMediaPlayer.setColumns(35);

        btnSearchMediaPlayer = new JButton(TmmResourceBundle.getString("Button.chooseplayer"));
        panelMediaPlayer.add(btnSearchMediaPlayer, "cell 1 0");

        JTextArea tpMediaPlayer = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.mediaplayer.hint"));
        panelMediaPlayer.add(tpMediaPlayer, "cell 1 1 2 1,growx, wmin 0");
        TmmFontHelper.changeFont(tpMediaPlayer, L2);
      }
    }
    {
      JPanel panelMediaFramework = new JPanel();
      panelMediaFramework.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][400lp,grow][]", "[][][][]"));
      JLabel lblMediaFrameworkT = new TmmLabel("FFmpeg", H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMediaFramework, lblMediaFrameworkT, true);
      add(collapsiblePanel, "cell 0 2,growx, wmin 0");

      {
        rdbtnFfmpegInternal = new JRadioButton(TmmResourceBundle.getString("Settings.mediaframework.internal"));
        buttonGroupFfmpeg.add(rdbtnFfmpegInternal);
        panelMediaFramework.add(rdbtnFfmpegInternal, "cell 1 0 2 1");
      }
      {
        rdbtnFFmpegExternal = new JRadioButton(TmmResourceBundle.getString("Settings.mediaframework.external"));
        buttonGroupFfmpeg.add(rdbtnFFmpegExternal);
        panelMediaFramework.add(rdbtnFFmpegExternal, "cell 1 1 2 1");

        tfMediaFramework = new JTextField();
        panelMediaFramework.add(tfMediaFramework, "cell 2 2");
        tfMediaFramework.setColumns(35);

        btnSearchFFMpegBinary = new JButton(TmmResourceBundle.getString("Button.chooseffmpeglocation"));
        panelMediaFramework.add(btnSearchFFMpegBinary, "cell 2 2");

        JTextArea tpFFMpegLocation = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.mediaframework.hint"));
        panelMediaFramework.add(tpFFMpegLocation, "cell 2 3,growx");
        TmmFontHelper.changeFont(tpFFMpegLocation, L2);
      }
    }
  }

  private void initDataBindings() {
    //
    Property settingsBeanProperty_6 = BeanProperty.create("mediaPlayer");
    Property jTextFieldBeanProperty_3 = BeanProperty.create("text");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_6, tfMediaPlayer,
        jTextFieldBeanProperty_3);
    autoBinding_9.bind();
    //
    Property settingsBeanProperty_7 = BeanProperty.create("mediaFramework");
    Property jTextFieldBeanProperty_4 = BeanProperty.create("text");
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_7, tfMediaFramework,
        jTextFieldBeanProperty_4);
    autoBinding_10.bind();
    //
    Property settingsBeanProperty_8 = BeanProperty.create("useInternalMediaFramework");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_8,
        rdbtnFfmpegInternal, jCheckBoxBeanProperty);
    autoBinding_6.bind();
  }
}
