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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.swingbinding.JListBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

class FileTypesSettingsPanel extends JPanel {
  private final Settings settings = Settings.getInstance();

  private JTextField     tfVideoFiletype;
  private JList<String>  listVideoFiletypes;
  private JTextField     tfSubtitleFiletype;
  private JList<String>  listSubtitleFiletypes;
  private JTextField     tfAudioFiletype;
  private JList<String>  listAudioFiletypes;
  private JTextField     tfCleanupFiletype;
  private JList<String>  listCleanupFiletypes;

  private JButton        btnAddAudioFiletype;
  private JButton        btnAddSubtitleFiletype;
  private JButton        btnAddVideoFiletype;
  private JButton        btnAddCleanupFiletype;
  private JButton        btnRemoveCleanupFiletype;
  private JButton        btnRemoveAudioFiletype;
  private JButton        btnRemoveSubtitleFiletype;
  private JButton        btnRemoveVideoFiletype;

  /**
   * Instantiates a new general settings panel.
   */
  FileTypesSettingsPanel() {
    // UI init
    initComponents();
    initDataBindings();

    // data init
    btnAddVideoFiletype.addActionListener(e -> {
      if (StringUtils.isNotEmpty(tfVideoFiletype.getText())) {
        Settings.getInstance().addVideoFileType(tfVideoFiletype.getText());
        tfVideoFiletype.setText("");
      }
    });
    btnRemoveVideoFiletype.addActionListener(arg0 -> {
      int row = listVideoFiletypes.getSelectedIndex();
      if (row != -1) {
        String prefix = Settings.getInstance().getVideoFileType().get(row);
        Settings.getInstance().removeVideoFileType(prefix);
      }
    });
    btnAddSubtitleFiletype.addActionListener(e -> {
      if (StringUtils.isNotEmpty(tfSubtitleFiletype.getText())) {
        Settings.getInstance().addSubtitleFileType(tfSubtitleFiletype.getText());
        tfSubtitleFiletype.setText("");
      }
    });
    btnRemoveSubtitleFiletype.addActionListener(arg0 -> {
      int row = listSubtitleFiletypes.getSelectedIndex();
      if (row != -1) {
        String prefix = Settings.getInstance().getSubtitleFileType().get(row);
        Settings.getInstance().removeSubtitleFileType(prefix);
      }
    });
    btnAddAudioFiletype.addActionListener(e -> {
      if (StringUtils.isNotEmpty(tfAudioFiletype.getText())) {
        Settings.getInstance().addAudioFileType(tfAudioFiletype.getText());
        tfAudioFiletype.setText("");
      }
    });
    btnRemoveAudioFiletype.addActionListener(arg0 -> {
      int row = listAudioFiletypes.getSelectedIndex();
      if (row != -1) {
        String prefix = Settings.getInstance().getAudioFileType().get(row);
        Settings.getInstance().removeAudioFileType(prefix);
      }
    });
    btnAddCleanupFiletype.addActionListener(e -> {
      if (StringUtils.isNotEmpty(tfCleanupFiletype.getText())) {
        try {
          Pattern.compile(tfCleanupFiletype.getText());
        }
        catch (PatternSyntaxException ex) {
          JOptionPane.showMessageDialog(this, TmmResourceBundle.getString("message.regex.error"));
          return;
        }
        Settings.getInstance().addCleanupFileType(tfCleanupFiletype.getText());
        tfCleanupFiletype.setText("");
      }
    });
    btnRemoveCleanupFiletype.addActionListener(arg0 -> {
      int row = listCleanupFiletypes.getSelectedIndex();
      if (row != -1) {
        String prefix = Settings.getInstance().getCleanupFileType().get(row);
        Settings.getInstance().removeCleanupFileType(prefix);
      }
    });
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][]"));
    {
      JPanel panelFiletypes = new JPanel(new MigLayout("hidemode 1", "[20lp!][150lp][][50lp][150lp][][50lp][150lp][]", "[][250lp][]"));

      JLabel lblFiletypesT = new TmmLabel(TmmResourceBundle.getString("Settings.filetypesimport"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelFiletypes, lblFiletypesT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#file-types"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        JLabel lblVideoFiletypesT = new TmmLabel(TmmResourceBundle.getString("Settings.videofiletypes"));
        panelFiletypes.add(lblVideoFiletypesT, "cell 1 0 3 1");

        JScrollPane scrollPaneVideoFiletypes = new JScrollPane();
        panelFiletypes.add(scrollPaneVideoFiletypes, "cell 1 1,grow");

        listVideoFiletypes = new JList<>();
        scrollPaneVideoFiletypes.setViewportView(listVideoFiletypes);

        btnRemoveVideoFiletype = new SquareIconButton(IconManager.REMOVE_INV);
        panelFiletypes.add(btnRemoveVideoFiletype, "cell 2 1,aligny bottom");
        btnRemoveVideoFiletype.setToolTipText(TmmResourceBundle.getString("Button.remove"));

        tfVideoFiletype = new JTextField();
        panelFiletypes.add(tfVideoFiletype, "cell 1 2,growx");

        btnAddVideoFiletype = new SquareIconButton(IconManager.ADD_INV);
        panelFiletypes.add(btnAddVideoFiletype, "cell 2 2,growx");
        btnAddVideoFiletype.setToolTipText(TmmResourceBundle.getString("Button.add"));
      }

      {
        JLabel lblAudioFiletypesT = new TmmLabel(TmmResourceBundle.getString("Settings.audiofiletypes"));
        panelFiletypes.add(lblAudioFiletypesT, "cell 4 0 3 1");

        JScrollPane scrollPaneAudioFiletypes = new JScrollPane();
        panelFiletypes.add(scrollPaneAudioFiletypes, "cell 4 1,grow");

        listAudioFiletypes = new JList<>();
        scrollPaneAudioFiletypes.setViewportView(listAudioFiletypes);

        btnRemoveAudioFiletype = new SquareIconButton(IconManager.REMOVE_INV);
        panelFiletypes.add(btnRemoveAudioFiletype, "cell 5 1,aligny bottom");
        btnRemoveAudioFiletype.setToolTipText(TmmResourceBundle.getString("Button.remove"));

        tfAudioFiletype = new JTextField();
        panelFiletypes.add(tfAudioFiletype, "cell 4 2,growx");

        btnAddAudioFiletype = new SquareIconButton(IconManager.ADD_INV);
        panelFiletypes.add(btnAddAudioFiletype, "cell 5 2, growx");
        btnAddAudioFiletype.setToolTipText(TmmResourceBundle.getString("Button.add"));
      }

      {
        JLabel lblVideoFiletypesT = new TmmLabel(TmmResourceBundle.getString("Settings.extrafiletypes"));
        panelFiletypes.add(lblVideoFiletypesT, "cell 7 0 3 1");

        JScrollPane scrollPaneSubtitleFiletypes = new JScrollPane();
        panelFiletypes.add(scrollPaneSubtitleFiletypes, "cell 7 1,grow");

        listSubtitleFiletypes = new JList<>();
        scrollPaneSubtitleFiletypes.setViewportView(listSubtitleFiletypes);

        btnRemoveSubtitleFiletype = new SquareIconButton(IconManager.REMOVE_INV);
        panelFiletypes.add(btnRemoveSubtitleFiletype, "cell 8 1,aligny bottom");
        btnRemoveSubtitleFiletype.setToolTipText(TmmResourceBundle.getString("Button.remove"));

        tfSubtitleFiletype = new JTextField();
        panelFiletypes.add(tfSubtitleFiletype, "cell 7 2,growx");

        btnAddSubtitleFiletype = new SquareIconButton(IconManager.ADD_INV);
        panelFiletypes.add(btnAddSubtitleFiletype, "cell 8 2");
        btnAddSubtitleFiletype.setToolTipText(TmmResourceBundle.getString("Button.add"));
      }
    }
    {
      JPanel panelCleanupFiletypes = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][250lp][][grow]", "[][][]"));

      JLabel lblCleanupFiletypesT = new TmmLabel(TmmResourceBundle.getString("Settings.unwantedfiletypes"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelCleanupFiletypes, lblCleanupFiletypesT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#unwanted-file-types"));
      add(collapsiblePanel, "cell 0 2 3 1,growx,wmin 0");
      {
        JScrollPane scrollPaneCleanupFiletypes = new JScrollPane();
        panelCleanupFiletypes.add(scrollPaneCleanupFiletypes, "cell 1 0,grow");

        listCleanupFiletypes = new JList<>();
        scrollPaneCleanupFiletypes.setViewportView(listCleanupFiletypes);

        btnRemoveCleanupFiletype = new SquareIconButton(IconManager.REMOVE_INV);
        panelCleanupFiletypes.add(btnRemoveCleanupFiletype, "cell 2 0,aligny bottom");
        btnRemoveCleanupFiletype.setToolTipText(TmmResourceBundle.getString("Button.remove"));

        tfCleanupFiletype = new JTextField();
        panelCleanupFiletypes.add(tfCleanupFiletype, "cell 1 1,growx");

        btnAddCleanupFiletype = new SquareIconButton(IconManager.ADD_INV);
        panelCleanupFiletypes.add(btnAddCleanupFiletype, "cell 2 1");
        btnAddCleanupFiletype.setToolTipText(TmmResourceBundle.getString("Button.add"));

      }
      JLabel lblCleanupFiletypesHelpT = new TmmLabel(TmmResourceBundle.getString("Settings.cleanupfiles.help"));
      panelCleanupFiletypes.add(lblCleanupFiletypesHelpT, "cell 1 2 2 1");

      JButton btnHelp = new JButton(TmmResourceBundle.getString("tmm.help"));
      panelCleanupFiletypes.add(btnHelp, "cell 3 2");
      btnHelp.addActionListener(e -> {
        String url = StringEscapeUtils.unescapeHtml4("https://www.tinymediamanager.org/docs/settings#file-types");
        try {
          TmmUIHelper.browseUrl(url);
        }
        catch (Exception e1) {
          MessageManager.instance
              .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e1.getLocalizedMessage() }));
        }
      });
    }
  }

  @SuppressWarnings("rawtypes")
  protected void initDataBindings() {
    BeanProperty<Settings, List<String>> settingsBeanProperty_5 = BeanProperty.create("videoFileType");
    JListBinding<String, Settings, JList> jListBinding_1 = SwingBindings.createJListBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_5, listVideoFiletypes);
    jListBinding_1.bind();
    //
    BeanProperty<Settings, List<String>> settingsBeanProperty_6 = BeanProperty.create("subtitleFileType");
    JListBinding<String, Settings, JList> jListBinding_2 = SwingBindings.createJListBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_6, listSubtitleFiletypes);
    jListBinding_2.bind();
    //
    BeanProperty<Settings, List<String>> settingsBeanProperty_11 = BeanProperty.create("audioFileType");
    JListBinding<String, Settings, JList> jListBinding_3 = SwingBindings.createJListBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_11, listAudioFiletypes);
    jListBinding_3.bind();
    //
    BeanProperty<Settings, List<String>> settingsBeanProperty_12 = BeanProperty.create("cleanupFileType");
    JListBinding<String, Settings, JList> jListBinding_4 = SwingBindings.createJListBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_12, listCleanupFiletypes);
    jListBinding_4.bind();
  }
}
