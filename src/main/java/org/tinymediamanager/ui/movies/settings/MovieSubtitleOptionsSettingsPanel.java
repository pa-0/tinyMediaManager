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
package org.tinymediamanager.ui.movies.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import javax.swing.JCheckBox;
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
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link MovieSubtitleOptionsSettingsPanel} is used to maintain subtitle related settings
 *
 * @author Manuel Laggner
 */
class MovieSubtitleOptionsSettingsPanel extends JPanel {
  private final MovieSettings      settings = MovieModuleManager.getInstance().getSettings();

  private JComboBox                cbScraperLanguage;
  private JComboBox<LanguageStyle> cbSubtitleLanguageStyle;
  private JCheckBox                chckbxSuppressLanguageTag;
  private JHintCheckBox            chckbxForceBestMatch;

  MovieSubtitleOptionsSettingsPanel() {
    // UI init
    initComponents();
    initDataBindings();
  }

  private void initComponents() {
    setLayout(new MigLayout("hidemode 0", "[600lp,grow]", "[]"));
    {
      JPanel panelOptions = new JPanel();
      panelOptions.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][10lp!][][]")); // 16lp ~ width of the

      JLabel lblOptionsT = new TmmLabel(TmmResourceBundle.getString("Settings.advancedoptions"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelOptions, lblOptionsT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#advanced-options-3"));
      add(collapsiblePanel, "cell 0 2,growx, wmin 0");
      {
        JLabel lblScraperLanguage = new JLabel(TmmResourceBundle.getString("Settings.preferredLanguage"));
        panelOptions.add(lblScraperLanguage, "cell 1 0 2 1");

        cbScraperLanguage = new JComboBox(MediaLanguages.valuesSorted());
        panelOptions.add(cbScraperLanguage, "cell 1 0 2 1");

        chckbxForceBestMatch = new JHintCheckBox(TmmResourceBundle.getString("subtitle.download.force"));
        chckbxForceBestMatch.setToolTipText(TmmResourceBundle.getString("subtitle.download.force.desc"));
        chckbxForceBestMatch.setHintIcon(IconManager.HINT);
        panelOptions.add(chckbxForceBestMatch, "cell 1 1 2 1");

        JLabel lblSubtitleLanguageStyle = new JLabel(TmmResourceBundle.getString("Settings.renamer.language"));
        panelOptions.add(lblSubtitleLanguageStyle, "cell 1 3 2 1");

        cbSubtitleLanguageStyle = new JComboBox(LanguageStyle.values());
        panelOptions.add(cbSubtitleLanguageStyle, "cell 1 3 2 1");
      }

      chckbxSuppressLanguageTag = new JCheckBox(TmmResourceBundle.getString("Settings.renamer.withoutlanguagetag"));
      panelOptions.add(chckbxSuppressLanguageTag, "cell 2 4,wmin 0,grow");
    }
  }

  protected void initDataBindings() {
    Property movieScraperBeanProperty = BeanProperty.create("subtitleScraperLanguage");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieScraperBeanProperty, cbScraperLanguage,
        jComboBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property movieScraperBeanProperty_1 = BeanProperty.create("subtitleLanguageStyle");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieScraperBeanProperty_1, cbSubtitleLanguageStyle,
        jComboBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property movieSettingsBeanProperty = BeanProperty.create("subtitleWithoutLanguageTag");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, chckbxSuppressLanguageTag,
        jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    Property movieSettingsBeanProperty_1 = BeanProperty.create("subtitleForceBestMatch");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_1, chckbxForceBestMatch,
        jCheckBoxBeanProperty);
    autoBinding_4.bind();
  }
}
