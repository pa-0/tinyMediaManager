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

import java.awt.Dimension;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.filenaming.TvShowExtraFanartNaming;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TvShowImageExtraPanel} shows scraper options for extra artwork
 *
 * @author Manuel Laggner
 */
class TvShowImageExtraPanel extends JPanel {
  private final TvShowSettings settings = TvShowModuleManager.getInstance().getSettings();

  private final ItemListener   checkBoxListener;

  private JCheckBox            cbActorImages;
  private JSpinner             spDownloadCountExtrafanart;
  private JCheckBox            chckbxEnableExtrafanart;
  private JCheckBox            chckbxExtraFanart1;
  private JCheckBox            chckbxExtraFanart2;
  private JCheckBox            chckbxExtraFanart3;

  TvShowImageExtraPanel() {
    checkBoxListener = e -> checkChanges();

    // UI init
    initComponents();
    initDataBindings();

    // further init
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(chckbxExtraFanart1);
    buttonGroup.add(chckbxExtraFanart2);
    buttonGroup.add(chckbxExtraFanart3);

    settings.addPropertyChangeListener(evt -> {
      if ("preset".equals(evt.getPropertyName())) {
        buildCheckBoxes();
      }
    });

    buildCheckBoxes();
  }

  private void buildCheckBoxes() {
    // initialize
    clearSelection(chckbxExtraFanart1, chckbxExtraFanart2, chckbxExtraFanart3);

    // extrafanart filenames
    for (TvShowExtraFanartNaming fanart : settings.getExtraFanartFilenames()) {
      switch (fanart) {
        case EXTRAFANART:
          chckbxExtraFanart1.setSelected(true);
          break;

        case FOLDER_EXTRAFANART:
          chckbxExtraFanart2.setSelected(true);
          break;

        case EXTRABACKDROP:
          chckbxExtraFanart3.setSelected(true);
          break;
      }
    }

    // listen to changes of the checkboxes
    chckbxExtraFanart1.addItemListener(checkBoxListener);
    chckbxExtraFanart2.addItemListener(checkBoxListener);
    chckbxExtraFanart3.addItemListener(checkBoxListener);
  }

  private void clearSelection(JCheckBox... checkBoxes) {
    for (JCheckBox checkBox : checkBoxes) {
      checkBox.removeItemListener(checkBoxListener);
      checkBox.setSelected(false);
    }
  }

  /**
   * Check changes.
   */
  private void checkChanges() {
    // set poster filenames
    settings.clearExtraFanartFilenames();

    if (chckbxExtraFanart1.isSelected()) {
      settings.addExtraFanartFilename(TvShowExtraFanartNaming.EXTRAFANART);
    }
    if (chckbxExtraFanart2.isSelected()) {
      settings.addExtraFanartFilename(TvShowExtraFanartNaming.FOLDER_EXTRAFANART);
    }
    if (chckbxExtraFanart3.isSelected()) {
      settings.addExtraFanartFilename(TvShowExtraFanartNaming.EXTRABACKDROP);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[]"));
    {
      JPanel panelOptions = new JPanel();
      panelOptions.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][grow][10lp!][][15lp!][]")); // 16lp ~ width
      // of the

      JLabel lblOptionsT = new TmmLabel(TmmResourceBundle.getString("Settings.extraartwork"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelOptions, lblOptionsT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#advanced-options-1"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");
      {

        chckbxEnableExtrafanart = new JCheckBox(TmmResourceBundle.getString("Settings.enable.extrafanart"));
        panelOptions.add(chckbxEnableExtrafanart, "cell 1 0 2 1");

        JPanel panel = new JPanel();
        panelOptions.add(panel, "cell 2 1,growx");
        panel.setLayout(new MigLayout("insets 0", "[][20lp!][]", "[][]"));

        chckbxExtraFanart1 = new JCheckBox("fanartX." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panel.add(chckbxExtraFanart1, "cell 0 0");

        chckbxExtraFanart2 = new JCheckBox("extrafanart/fanartX." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panel.add(chckbxExtraFanart2, "cell 2 0");

        chckbxExtraFanart3 = new JCheckBox("backdropX." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panel.add(chckbxExtraFanart3, "cell 0 1");

        JLabel lblDownloadCount = new JLabel(TmmResourceBundle.getString("Settings.amount.autodownload"));
        panelOptions.add(lblDownloadCount, "cell 2 3");

        spDownloadCountExtrafanart = new JSpinner();
        spDownloadCountExtrafanart.setMinimumSize(new Dimension(60, 20));
        panelOptions.add(spDownloadCountExtrafanart, "cell 2 3");
      }

      cbActorImages = new JCheckBox(TmmResourceBundle.getString("Settings.actor.download"));
      panelOptions.add(cbActorImages, "cell 1 5 2 1");
    }
  }

  protected void initDataBindings() {
    Property tvShowSettingsBeanProperty = BeanProperty.create("writeActorImages");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty, cbActorImages,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property tvShowSettingsBeanProperty_1 = BeanProperty.create("imageExtraFanartCount");
    Property jSpinnerBeanProperty_1 = BeanProperty.create("value");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_1,
        spDownloadCountExtrafanart, jSpinnerBeanProperty_1);
    autoBinding_3.bind();
    //
    Property tvShowSettingsBeanProperty_2 = BeanProperty.create("imageExtraFanart");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_2, chckbxEnableExtrafanart,
        jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property jSpinnerBeanProperty = BeanProperty.create("enabled");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, chckbxEnableExtrafanart, jCheckBoxBeanProperty,
        spDownloadCountExtrafanart, jSpinnerBeanProperty);
    autoBinding_2.bind();
    //
    Property jCheckBoxBeanProperty_2 = BeanProperty.create("enabled");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxEnableExtrafanart, jCheckBoxBeanProperty, chckbxExtraFanart1,
        jCheckBoxBeanProperty_2);
    autoBinding_9.bind();
    //
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxEnableExtrafanart, jCheckBoxBeanProperty, chckbxExtraFanart2,
        jCheckBoxBeanProperty_2);
    autoBinding_10.bind();
  }
}
