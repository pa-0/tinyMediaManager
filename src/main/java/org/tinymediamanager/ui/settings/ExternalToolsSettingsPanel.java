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
  private final ButtonGroup buttonGroupYtDlp  = new ButtonGroup();

  private JTextField        tfMediaPlayerPath;
  private JButton           btnSearchMediaPlayer;

  private JRadioButton      rdbtnFFmpegInternal;
  private JRadioButton      rdbtnFFmpegExternal;
  private JTextField        tfFFmpegPath;
  private JButton           btnSearchFFMpegBinary;

  private JRadioButton      rdbtnYtDlpInternal;
  private JRadioButton      rdbtnYtDlpExternal;
  private JTextField        tfYtDlpPath;
  private JButton           btnSearchYtDlpBinary;

  ExternalToolsSettingsPanel() {
    initComponents();

    initDataBindings();

    // data init
    btnSearchMediaPlayer.addActionListener(arg0 -> {
      String path = TmmProperties.getInstance().getProperty("chooseplayer.path");
      Path file = TmmUIHelper.selectApplication(TmmResourceBundle.getString("Button.chooseplayer"), path);
      if (file != null && (Utils.isRegularFile(file) || SystemUtils.IS_OS_MAC)) {
        tfMediaPlayerPath.setText(file.toAbsolutePath().toString());
        TmmProperties.getInstance().putProperty("chooseplayer.path", file.getParent().toString());
      }
    });

    btnSearchFFMpegBinary.addActionListener(arg0 -> {
      String path = TmmProperties.getInstance().getProperty("chooseffmpeg.path");
      Path file = TmmUIHelper.selectFile(TmmResourceBundle.getString("Button.chooseffmpeglocation"), path, null);
      if (file != null && (Utils.isRegularFile(file) || SystemUtils.IS_OS_MAC)) {
        tfFFmpegPath.setText(file.toAbsolutePath().toString());
        TmmProperties.getInstance().putProperty("chooseffmpeg.path", file.getParent().toString());
      }
    });

    btnSearchYtDlpBinary.addActionListener(arg0 -> {
      String path = TmmProperties.getInstance().getProperty("chooseytdlp.path");
      Path file = TmmUIHelper.selectFile(TmmResourceBundle.getString("Button.chooseytdlplocation"), path, null);
      if (file != null && (Utils.isRegularFile(file) || SystemUtils.IS_OS_MAC)) {
        tfYtDlpPath.setText(file.toAbsolutePath().toString());
        TmmProperties.getInstance().putProperty("chooseytdlp.path", file.getParent().toString());
      }
    });

    // init of the radiobutton
    if (settings.isUseInternalMediaFramework()) {
      rdbtnFFmpegInternal.setSelected(true);
    }
    else {
      rdbtnFFmpegExternal.setSelected(true);
    }

    if (settings.isUseInternalYtDlp()) {
      rdbtnYtDlpInternal.setSelected(true);
    }
    else {
      rdbtnYtDlpExternal.setSelected(true);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][][15lp!][]"));
    {
      JPanel panelMediaPlayer = new JPanel();
      panelMediaPlayer.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "")); // 16lp ~ width of the

      JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("Settings.mediaplayer"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMediaPlayer, lblLanguageT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#media-player"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        tfMediaPlayerPath = new JTextField();
        panelMediaPlayer.add(tfMediaPlayerPath, "cell 1 0 2 1");
        tfMediaPlayerPath.setColumns(35);

        btnSearchMediaPlayer = new JButton(TmmResourceBundle.getString("Button.chooseplayer"));
        panelMediaPlayer.add(btnSearchMediaPlayer, "cell 1 0");

        JTextArea tpMediaPlayer = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.mediaplayer.hint"));
        panelMediaPlayer.add(tpMediaPlayer, "cell 1 1 2 1,growx, wmin 0");
        TmmFontHelper.changeFont(tpMediaPlayer, L2);
      }
    }
    {
      JPanel panelFFmpeg = new JPanel();
      panelFFmpeg.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][400lp,grow][]", "[][][][]"));
      JLabel lblFFmpegT = new TmmLabel("FFmpeg", H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelFFmpeg, lblFFmpegT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#ffmpeg"));
      add(collapsiblePanel, "cell 0 2,growx, wmin 0");

      {
        rdbtnFFmpegInternal = new JRadioButton(TmmResourceBundle.getString("Settings.mediaframework.internal"));
        buttonGroupFfmpeg.add(rdbtnFFmpegInternal);
        panelFFmpeg.add(rdbtnFFmpegInternal, "cell 1 0 2 1");
      }
      {
        rdbtnFFmpegExternal = new JRadioButton(TmmResourceBundle.getString("Settings.mediaframework.external"));
        buttonGroupFfmpeg.add(rdbtnFFmpegExternal);
        panelFFmpeg.add(rdbtnFFmpegExternal, "cell 1 1 2 1");

        tfFFmpegPath = new JTextField();
        panelFFmpeg.add(tfFFmpegPath, "cell 2 2");
        tfFFmpegPath.setColumns(35);

        btnSearchFFMpegBinary = new JButton(TmmResourceBundle.getString("Button.chooseffmpeglocation"));
        panelFFmpeg.add(btnSearchFFMpegBinary, "cell 2 2");

        JTextArea tpFFMpegLocation = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.mediaframework.hint"));
        panelFFmpeg.add(tpFFMpegLocation, "cell 2 3,growx");
        TmmFontHelper.changeFont(tpFFMpegLocation, L2);
      }
    }
    {
      JPanel panelYtDlp = new JPanel();
      panelYtDlp.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][400lp,grow][]", "[][][][]"));
      JLabel lblYtDlpT = new TmmLabel("yt-dlp", H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelYtDlp, lblYtDlpT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#yt-dlp"));
      add(collapsiblePanel, "cell 0 4,growx, wmin 0");

      {
        rdbtnYtDlpInternal = new JRadioButton(TmmResourceBundle.getString("Settings.ytdlp.internal"));
        buttonGroupYtDlp.add(rdbtnYtDlpInternal);
        panelYtDlp.add(rdbtnYtDlpInternal, "cell 1 0 2 1");
      }
      {
        rdbtnYtDlpExternal = new JRadioButton(TmmResourceBundle.getString("Settings.ytdlp.external"));
        buttonGroupYtDlp.add(rdbtnYtDlpExternal);
        panelYtDlp.add(rdbtnYtDlpExternal, "cell 1 1 2 1");

        tfYtDlpPath = new JTextField();
        panelYtDlp.add(tfYtDlpPath, "cell 2 2");
        tfYtDlpPath.setColumns(35);

        btnSearchYtDlpBinary = new JButton(TmmResourceBundle.getString("Button.chooseytdlplocation"));
        panelYtDlp.add(btnSearchYtDlpBinary, "cell 2 2");

        JTextArea tpYtDlpLocation = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.ytdlp.hint"));
        panelYtDlp.add(tpYtDlpLocation, "cell 2 3,growx");
        TmmFontHelper.changeFont(tpYtDlpLocation, L2);
      }
    }
  }

  private void initDataBindings() {
    //
    Property settingsBeanProperty_6 = BeanProperty.create("mediaPlayer");
    Property jTextFieldBeanProperty_3 = BeanProperty.create("text");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_6, tfMediaPlayerPath,
        jTextFieldBeanProperty_3);
    autoBinding_9.bind();
    //
    Property settingsBeanProperty_7 = BeanProperty.create("mediaFramework");
    Property jTextFieldBeanProperty_4 = BeanProperty.create("text");
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_7, tfFFmpegPath,
        jTextFieldBeanProperty_4);
    autoBinding_10.bind();
    //
    Property settingsBeanProperty_8 = BeanProperty.create("useInternalMediaFramework");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_8,
        rdbtnFFmpegInternal, jCheckBoxBeanProperty);
    autoBinding_6.bind();
    //
    Property settingsBeanProperty_9 = BeanProperty.create("externalYtDlpPath");
    Property jTextFieldBeanProperty_5 = BeanProperty.create("text");
    AutoBinding autoBinding_11 = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_9, tfYtDlpPath,
        jTextFieldBeanProperty_5);
    autoBinding_11.bind();
    //
    Property settingsBeanProperty_12 = BeanProperty.create("useInternalYtDlp");
    Property jCheckBoxBeanProperty_1 = BeanProperty.create("selected");
    AutoBinding autoBinding_12 = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_12,
        rdbtnYtDlpInternal, jCheckBoxBeanProperty_1);
    autoBinding_12.bind();
  }
}
