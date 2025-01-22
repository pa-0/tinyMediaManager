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
package org.tinymediamanager.ui.tvshows.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.LanguageStyle;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TvShowSubtitleOptionsSettingsPanel} is used to maintain subtitle related settings
 * 
 * @author Manuel Laggner
 */
class TvShowSubtitleOptionsSettingsPanel extends JPanel {
  private final TvShowSettings     settings = TvShowModuleManager.getInstance().getSettings();

  private JComboBox                cbScraperLanguage;
  private JComboBox<LanguageStyle> cbLanguageStyle;
  private JHintCheckBox            chckbxForceBestMatch;

  TvShowSubtitleOptionsSettingsPanel() {
    // UI init
    initComponents();
    initDataBindings();

  }

  private void initComponents() {
    setLayout(new MigLayout("hidemode 0", "[600lp,grow]", "[]"));
    {
      JPanel panelOptions = new JPanel();
      panelOptions.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][10lp!][]")); // 16lp ~ width of the

      JLabel lblOptionsT = new TmmLabel(TmmResourceBundle.getString("Settings.advancedoptions"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelOptions, lblOptionsT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#advanced-options-3"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        JLabel lblScraperLanguage = new JLabel(TmmResourceBundle.getString("Settings.preferredLanguage"));
        panelOptions.add(lblScraperLanguage, "cell 1 0 2 1");

        cbScraperLanguage = new JComboBox(MediaLanguages.valuesSorted());
        panelOptions.add(cbScraperLanguage, "cell 1 0 2 1");

        chckbxForceBestMatch = new JHintCheckBox(TmmResourceBundle.getString("subtitle.download.force"));
        chckbxForceBestMatch.setToolTipText(TmmResourceBundle.getString("subtitle.download.force.desc"));
        chckbxForceBestMatch.setHintIcon(IconManager.HINT);
        panelOptions.add(chckbxForceBestMatch, "cell 1 1 2 1");

        JLabel lblLanguageStyle = new JLabel(TmmResourceBundle.getString("Settings.renamer.language"));
        panelOptions.add(lblLanguageStyle, "cell 1 3 2 1");

        cbLanguageStyle = new JComboBox(LanguageStyle.values());
        panelOptions.add(cbLanguageStyle, "cell 1 3 2 1");
      }
    }
  }

  protected void initDataBindings() {
    Property tvShowSettingsBeanProperty = BeanProperty.create("subtitleScraperLanguage");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty, cbScraperLanguage,
        jComboBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property tvShowSettingsBeanProperty_1 = BeanProperty.create("subtitleLanguageStyle");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_1, cbLanguageStyle,
        jComboBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property tvShowSettingsBeanProperty_2 = BeanProperty.create("subtitleForceBestMatch");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_2, chckbxForceBestMatch,
        jCheckBoxBeanProperty);
    autoBinding_3.bind();
  }
}
