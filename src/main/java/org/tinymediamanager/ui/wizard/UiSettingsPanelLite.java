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
package org.tinymediamanager.ui.wizard;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.LocaleComboBox;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link UiSettingsPanelLite} is used to display generic UI settings<br>
 * This is the "lite" variant, used for first start wizard.W
 * 
 * @author Manuel Laggner
 */
class UiSettingsPanelLite extends JPanel {
  private static final Logger        LOGGER             = LoggerFactory.getLogger(UiSettingsPanelLite.class);

  private static final Integer[]     DEFAULT_FONT_SIZES = { 12, 14, 16, 18, 20, 22, 24, 26, 28 };

  private final Settings             settings           = Settings.getInstance();
  private final List<LocaleComboBox> locales            = new ArrayList<>();

  private JComboBox                  cbLanguage;
  private ImageLabel                 lblLight;
  private ImageLabel                 lblDark;
  private JRadioButton               rdbtnLight;
  private JRadioButton               rdbtnDark;
  private JComboBox                  cbFontSize;
  private JComboBox                  cbFontFamily;
  private JLabel                     lblUpdate;
  private JLabel                     lblUpdateInterval;
  private JSpinner                   spUpdateInterval;
  private JCheckBox                  chckbxAutomaticUpdates;
  private JLabel                     lblUpdateHint;

  public UiSettingsPanelLite() {
    LocaleComboBox actualLocale = null;
    LocaleComboBox fallbackLocale = null;
    Locale settingsLang = Utils.getLocaleFromLanguage(settings.getLanguage());
    for (Locale l : Utils.getLanguages()) {
      LocaleComboBox localeComboBox = new LocaleComboBox(l);
      locales.add(localeComboBox);
      if (l.equals(settingsLang)) {
        actualLocale = localeComboBox;
      }
      // match by langu only, if no direct match
      if (settingsLang.getLanguage().equals(l.getLanguage())) {
        fallbackLocale = localeComboBox;
      }
    }
    Collections.sort(locales);

    initComponents();

    // data init
    if (actualLocale != null) {
      cbLanguage.setSelectedItem(actualLocale);
    }
    else {
      cbLanguage.setSelectedItem(fallbackLocale);
    }

    cbFontFamily.setSelectedItem(settings.getFontFamily());
    int index = cbFontFamily.getSelectedIndex();
    if (index < 0) {
      cbFontFamily.setSelectedItem("Dialog");
      index = cbFontFamily.getSelectedIndex();
    }
    if (index < 0) {
      cbFontFamily.setSelectedIndex(0);
    }
    cbFontSize.setSelectedItem(settings.getFontSize());
    index = cbFontSize.getSelectedIndex();
    if (index < 0) {
      cbFontSize.setSelectedIndex(0);
    }

    if ("Dark".equals(settings.getTheme())) {
      rdbtnDark.setSelected(true);
    }
    else {
      rdbtnLight.setSelected(true);
    }

    chckbxAutomaticUpdates.setSelected(settings.isEnableAutomaticUpdate());
    spUpdateInterval.setValue(settings.getAutomaticUpdateInterval());

    ActionListener actionListener = e -> checkChanges();
    cbLanguage.addActionListener(actionListener);
    cbFontFamily.addActionListener(actionListener);
    cbFontSize.addActionListener(actionListener);
    rdbtnLight.addActionListener(actionListener);
    rdbtnDark.addActionListener(actionListener);
    lblLight.addBoundedMouseListener(new MouseClickEventProxy(rdbtnLight));
    lblDark.addBoundedMouseListener(new MouseClickEventProxy(rdbtnDark));
    chckbxAutomaticUpdates.addActionListener(actionListener);

    // hide update related settings if we tmm.noupdate has been set
    if (Boolean.parseBoolean(System.getProperty("tmm.noupdate")) || ReleaseInfo.isNightly()) {
      lblUpdate.setVisible(false);
      chckbxAutomaticUpdates.setSelected(false);
      chckbxAutomaticUpdates.setVisible(false);
      lblUpdateInterval.setVisible(false);
      spUpdateInterval.setVisible(false);
      lblUpdateHint.setVisible(false);
    }
  }

  /*
   * init UI components
   */
  private void initComponents() {
    setLayout(new MigLayout("", "[20lp!][200lp,grow][200lp,grow]", "[][][10lp!][][10lp!][][300lp,grow][][10lp!][][][][15lp!][][][][]"));
    {
      JLabel lblUiSettings = new JLabel(TmmResourceBundle.getString("wizard.ui"));
      TmmFontHelper.changeFont(lblUiSettings, 1.3333, Font.BOLD);
      add(lblUiSettings, "cell 0 0 3 1,growx");
    }

    JTextArea taSettingsHint = new ReadOnlyTextArea(TmmResourceBundle.getString("wizard.ui.hint"));
    add(taSettingsHint, "cell 1 1 2 1,growx");

    JLabel lblLanguageT = new JLabel(TmmResourceBundle.getString("Settings.language"));
    add(lblLanguageT, "flowx,cell 1 3 2 1");

    cbLanguage = new JComboBox(locales.toArray());
    add(cbLanguage, "cell 1 3 2 1");

    JLabel lblThemeT = new JLabel(TmmResourceBundle.getString("Settings.uitheme"));
    add(lblThemeT, "cell 1 5");

    lblLight = new ImageLabel(false);
    try (InputStream is = UiSettingsPanelLite.class.getResourceAsStream("light.png")) {
      lblLight.setOriginalImage(IOUtils.toByteArray(is));
    }
    catch (Exception e) {
      LOGGER.error("could not load image: {}", e.getMessage());
    }
    add(lblLight, "cell 1 6, grow");

    lblDark = new ImageLabel(false);
    try (InputStream is = UiSettingsPanelLite.class.getResourceAsStream("dark.png")) {
      lblDark.setOriginalImage(IOUtils.toByteArray(is));
    }
    catch (Exception e) {
      LOGGER.error("could not load image: {}", e.getMessage());
    }
    add(lblDark, "cell 2 6, grow");

    ButtonGroup buttonGroup = new ButtonGroup();

    rdbtnLight = new JRadioButton(TmmResourceBundle.getString("Settings.uitheme.light"));
    buttonGroup.add(rdbtnLight);
    add(rdbtnLight, "cell 1 7,alignx left");

    rdbtnDark = new JRadioButton(TmmResourceBundle.getString("Settings.uitheme.dark"));
    buttonGroup.add(rdbtnDark);
    add(rdbtnDark, "cell 2 7,alignx left");

    JLabel lblFontT = new JLabel(TmmResourceBundle.getString("Settings.font"));
    add(lblFontT, "flowx,cell 1 9 2 1");

    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    cbFontFamily = new JComboBox(env.getAvailableFontFamilyNames());
    add(cbFontFamily, "cell 1 9 2 1");

    JLabel lblSize = new JLabel(TmmResourceBundle.getString("Settings.fontsize"));
    add(lblSize, "flowx,cell 1 10");

    cbFontSize = new JComboBox(DEFAULT_FONT_SIZES);
    add(cbFontSize, "cell 1 10");

    JTextArea taFontHint = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.fonts.hint"));
    add(taFontHint, "cell 1 11 2 1,grow");

    {
      lblUpdate = new JLabel(TmmResourceBundle.getString("Settings.update"));
      TmmFontHelper.changeFont(lblUpdate, 1.3333, Font.BOLD);
      add(lblUpdate, "cell 0 13 3 1");
    }
    {
      chckbxAutomaticUpdates = new JCheckBox(TmmResourceBundle.getString("Settings.updatecheck"));
      add(chckbxAutomaticUpdates, "cell 0 14 3 1");
    }
    {
      lblUpdateInterval = new JLabel(TmmResourceBundle.getString("Settings.updatecheck.interval"));
      add(lblUpdateInterval, "flowx,cell 1 15 2 1");

      spUpdateInterval = new JSpinner();
      spUpdateInterval.setModel(new SpinnerNumberModel(1, 1, 30, 1));
      add(spUpdateInterval, "cell 1 15 2 1");
    }
    {
      lblUpdateHint = new JLabel("");
      TmmFontHelper.changeFont(lblUpdateHint, Font.BOLD);
      add(lblUpdateHint, "cell 0 16 3 1");
    }
  }

  /**
   * Check changes.
   */
  private void checkChanges() {
    LocaleComboBox loc = (LocaleComboBox) cbLanguage.getSelectedItem();
    if (loc != null) {
      Locale locale = loc.getLocale();
      Locale actualLocale = Utils.getLocaleFromLanguage(settings.getLanguage());
      if (!locale.equals(actualLocale)) {
        settings.setLanguage(locale.toString());
        TmmResourceBundle.clearCache();
      }
    }

    // theme
    String theme;
    if (rdbtnDark.isSelected()) {
      theme = "Dark";
    }
    else {
      theme = "Light";
    }
    if (!theme.equals(settings.getTheme())) {
      settings.setTheme(theme);
      try {
        TmmUIHelper.setTheme();
        TmmUIHelper.updateUI();
      }
      catch (Exception e) {
        // ignored
      }
    }

    // fonts
    boolean fontChanged = false;
    Integer fontSize = (Integer) cbFontSize.getSelectedItem();
    if (fontSize != null && fontSize != settings.getFontSize()) {
      settings.setFontSize(fontSize);
      fontChanged = true;
    }

    String fontFamily = (String) cbFontFamily.getSelectedItem();
    if (fontFamily != null && !fontFamily.equals(settings.getFontFamily())) {
      settings.setFontFamily(fontFamily);
      fontChanged = true;
    }

    if (fontChanged) {
      Font font = UIManager.getFont("defaultFont");
      Font newFont = new Font(settings.getFontFamily(), font.getStyle(), settings.getFontSize());
      UIManager.put("defaultFont", newFont);

      TmmUIHelper.updateUI();
    }

    // update
    settings.setEnableAutomaticUpdate(chckbxAutomaticUpdates.isSelected());
    settings.setAutomaticUpdateInterval((int) spUpdateInterval.getValue());
    if (chckbxAutomaticUpdates.isSelected()) {
      spUpdateInterval.setEnabled(true);
      lblUpdateHint.setText("");
    }
    else {
      spUpdateInterval.setEnabled(false);
      lblUpdateHint.setText(TmmResourceBundle.getString("Settings.updatecheck.hint"));
    }
  }

  /**
   * Proxy mouse click events from any {@link JComponent} to any other clickable {@link AbstractButton}.
   * <p>
   * This allows the user to click on an {@link ImageLabel} in order to select its corresponding {@link JRadioButton}, for example.
   */
  private static class MouseClickEventProxy extends MouseAdapter {
    private final AbstractButton button;

    MouseClickEventProxy(AbstractButton button) {
      this.button = button;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      this.button.doClick();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      this.button.doClick();
    }
  }
}
