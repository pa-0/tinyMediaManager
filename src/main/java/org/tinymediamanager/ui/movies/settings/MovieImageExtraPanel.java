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
package org.tinymediamanager.ui.movies.settings;

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
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.filenaming.MovieExtraFanartNaming;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieImageSettingsPanel.
 * 
 * @author Manuel Laggner
 */
class MovieImageExtraPanel extends JPanel {
  private final MovieSettings settings = MovieModuleManager.getInstance().getSettings();
  private final ItemListener  checkBoxListener;

  private JCheckBox           cbActorImages;
  private JCheckBox           chckbxEnableExtrathumbs;
  private JCheckBox           chckbxEnableExtrafanart;
  private JCheckBox           chckbxResizeExtrathumbsTo;
  private JSpinner            spExtrathumbWidth;
  private JSpinner            spDownloadCountExtrathumbs;
  private JSpinner            spDownloadCountExtrafanart;
  private JCheckBox           chckbxExtrafanart1;
  private JCheckBox           chckbxExtrafanart2;
  private JCheckBox           chckbxExtrafanart3;
  private JCheckBox           chckbxExtrafanart4;
  private JCheckBox           chckbxExtrafanart5;
  private JCheckBox           chckbxExtrafanart6;
  private JCheckBox           chckbxExtrafanart7;

  /**
   * Instantiates a new movie image settings panel.
   */
  MovieImageExtraPanel() {
    checkBoxListener = e -> checkChanges();

    // UI init
    initComponents();
    initDataBindings();

    // further init
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(chckbxExtrafanart1);
    buttonGroup.add(chckbxExtrafanart2);
    buttonGroup.add(chckbxExtrafanart3);
    buttonGroup.add(chckbxExtrafanart4);
    buttonGroup.add(chckbxExtrafanart5);
    buttonGroup.add(chckbxExtrafanart6);
    buttonGroup.add(chckbxExtrafanart7);

    settings.addPropertyChangeListener(evt -> {
      if ("preset".equals(evt.getPropertyName())) {
        buildCheckBoxes();
      }
    });

    buildCheckBoxes();
  }

  private void buildCheckBoxes() {
    // initialize
    clearSelection(chckbxExtrafanart1, chckbxExtrafanart2, chckbxExtrafanart3, chckbxExtrafanart4, chckbxExtrafanart5, chckbxExtrafanart6,
        chckbxExtrafanart7);

    // extrafanart filenames
    for (MovieExtraFanartNaming fanart : settings.getExtraFanartFilenames()) {
      switch (fanart) {
        case FILENAME_EXTRAFANART:
          chckbxExtrafanart1.setSelected(true);
          break;

        case FILENAME_EXTRAFANART2:
          chckbxExtrafanart2.setSelected(true);
          break;

        case EXTRAFANART:
          chckbxExtrafanart3.setSelected(true);
          break;

        case FOLDER_EXTRAFANART:
          chckbxExtrafanart4.setSelected(true);
          break;

        case FILENAME_EXTRABACKDROP:
          chckbxExtrafanart5.setSelected(true);
          break;

        case FILENAME_EXTRABACKDROP2:
          chckbxExtrafanart6.setSelected(true);
          break;

        case EXTRABACKDROP:
          chckbxExtrafanart7.setSelected(true);
          break;
      }
    }

    // listen to changes of the checkboxes
    chckbxExtrafanart1.addItemListener(checkBoxListener);
    chckbxExtrafanart2.addItemListener(checkBoxListener);
    chckbxExtrafanart3.addItemListener(checkBoxListener);
    chckbxExtrafanart4.addItemListener(checkBoxListener);
    chckbxExtrafanart5.addItemListener(checkBoxListener);
    chckbxExtrafanart6.addItemListener(checkBoxListener);
    chckbxExtrafanart7.addItemListener(checkBoxListener);
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

    if (chckbxExtrafanart1.isSelected()) {
      settings.addExtraFanartFilename(MovieExtraFanartNaming.FILENAME_EXTRAFANART);
    }
    if (chckbxExtrafanart2.isSelected()) {
      settings.addExtraFanartFilename(MovieExtraFanartNaming.FILENAME_EXTRAFANART2);
    }
    if (chckbxExtrafanart3.isSelected()) {
      settings.addExtraFanartFilename(MovieExtraFanartNaming.EXTRAFANART);
    }
    if (chckbxExtrafanart4.isSelected()) {
      settings.addExtraFanartFilename(MovieExtraFanartNaming.FOLDER_EXTRAFANART);
    }
    if (chckbxExtrafanart5.isSelected()) {
      settings.addExtraFanartFilename(MovieExtraFanartNaming.FILENAME_EXTRABACKDROP);
    }
    if (chckbxExtrafanart6.isSelected()) {
      settings.addExtraFanartFilename(MovieExtraFanartNaming.FILENAME_EXTRABACKDROP2);
    }
    if (chckbxExtrafanart7.isSelected()) {
      settings.addExtraFanartFilename(MovieExtraFanartNaming.EXTRABACKDROP);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][]"));
    {
      JPanel panelExtra = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][20lp!][][][10lp!][][20lp!][]"));

      JLabel lblExtra = new TmmLabel(TmmResourceBundle.getString("Settings.extraartwork"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelExtra, lblExtra, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#enable-extra-artwork"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        chckbxEnableExtrathumbs = new JCheckBox(TmmResourceBundle.getString("Settings.enable.extrathumbs"));
        panelExtra.add(chckbxEnableExtrathumbs, "cell 1 0 2 1");

        chckbxResizeExtrathumbsTo = new JCheckBox(TmmResourceBundle.getString("Settings.resize.extrathumbs"));
        panelExtra.add(chckbxResizeExtrathumbsTo, "cell 2 1");

        spExtrathumbWidth = new JSpinner();
        spExtrathumbWidth.setMinimumSize(new Dimension(60, 20));
        panelExtra.add(spExtrathumbWidth, "cell 2 1");

        JLabel lblDownload = new JLabel(TmmResourceBundle.getString("Settings.amount.autodownload"));
        panelExtra.add(lblDownload, "cell 2 2");

        spDownloadCountExtrathumbs = new JSpinner();
        spDownloadCountExtrathumbs.setMinimumSize(new Dimension(60, 20));
        panelExtra.add(spDownloadCountExtrathumbs, "cell 2 2");

        chckbxEnableExtrafanart = new JCheckBox(TmmResourceBundle.getString("Settings.enable.extrafanart"));
        panelExtra.add(chckbxEnableExtrafanart, "cell 1 4 2 1");

        JPanel panelExtraFanart = new JPanel();
        panelExtra.add(panelExtraFanart, "cell 2 5,grow");
        panelExtraFanart.setLayout(new MigLayout("insets 0", "[][]", "[][][][]"));

        chckbxExtrafanart1 = new JCheckBox(
            TmmResourceBundle.getString("Settings.moviefilename") + "-fanartX." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelExtraFanart.add(chckbxExtrafanart1, "flowy,cell 0 0");

        chckbxExtrafanart2 = new JCheckBox(
            TmmResourceBundle.getString("Settings.moviefilename") + ".fanartX." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelExtraFanart.add(chckbxExtrafanart2, "cell 0 1");

        chckbxExtrafanart3 = new JCheckBox("fanartX." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelExtraFanart.add(chckbxExtrafanart3, "cell 0 2");

        chckbxExtrafanart4 = new JCheckBox("extrafanart/fanartX." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelExtraFanart.add(chckbxExtrafanart4, "cell 0 3");

        chckbxExtrafanart5 = new JCheckBox(
            TmmResourceBundle.getString("Settings.moviefilename") + "-backdropX." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelExtraFanart.add(chckbxExtrafanart5, "cell 1 0");

        chckbxExtrafanart6 = new JCheckBox(
            TmmResourceBundle.getString("Settings.moviefilename") + ".backdropX." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelExtraFanart.add(chckbxExtrafanart6, "cell 1 1");

        chckbxExtrafanart7 = new JCheckBox("backdropX." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panelExtraFanart.add(chckbxExtrafanart7, "cell 1 2");

        JLabel lblDownloadCount = new JLabel(TmmResourceBundle.getString("Settings.amount.autodownload"));
        panelExtra.add(lblDownloadCount, "cell 2 7");

        spDownloadCountExtrafanart = new JSpinner();
        spDownloadCountExtrafanart.setMinimumSize(new Dimension(60, 20));
        panelExtra.add(spDownloadCountExtrafanart, "cell 2 7");

        cbActorImages = new JCheckBox(TmmResourceBundle.getString("Settings.actor.download"));
        panelExtra.add(cbActorImages, "cell 1 9 2 1");
      }
    }
  }

  protected void initDataBindings() {
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_2 = BeanProperty.create("writeActorImages");
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_2, cbActorImages, jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_3 = BeanProperty.create("imageExtraFanart");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_3, chckbxEnableExtrafanart, jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_4 = BeanProperty.create("imageExtraThumbs");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_4, chckbxEnableExtrathumbs, jCheckBoxBeanProperty);
    autoBinding_7.bind();
    //
    BeanProperty<MovieSettings, Integer> settingsBeanProperty_8 = BeanProperty.create("imageExtraThumbsSize");
    BeanProperty<JSpinner, Object> jSpinnerBeanProperty_1 = BeanProperty.create("value");
    AutoBinding<MovieSettings, Integer, JSpinner, Object> autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_8, spExtrathumbWidth, jSpinnerBeanProperty_1);
    autoBinding_10.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_9 = BeanProperty.create("imageExtraThumbsResize");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_9, chckbxResizeExtrathumbsTo, jCheckBoxBeanProperty);
    autoBinding_11.bind();
    //
    BeanProperty<MovieSettings, Integer> settingsBeanProperty_10 = BeanProperty.create("imageExtraThumbsCount");
    AutoBinding<MovieSettings, Integer, JSpinner, Object> autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_10, spDownloadCountExtrathumbs, jSpinnerBeanProperty_1);
    autoBinding_12.bind();
    //
    BeanProperty<MovieSettings, Integer> settingsBeanProperty_11 = BeanProperty.create("imageExtraFanartCount");
    AutoBinding<MovieSettings, Integer, JSpinner, Object> autoBinding_13 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_11, spDownloadCountExtrafanart, jSpinnerBeanProperty_1);
    autoBinding_13.bind();
    //
    BeanProperty<JSpinner, Boolean> jSpinnerBeanProperty = BeanProperty.create("enabled");
    AutoBinding<JCheckBox, Boolean, JSpinner, Boolean> autoBinding_14 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, chckbxEnableExtrafanart,
        jCheckBoxBeanProperty, spDownloadCountExtrafanart, jSpinnerBeanProperty);
    autoBinding_14.bind();
    //
    AutoBinding<JCheckBox, Boolean, JSpinner, Boolean> autoBinding_15 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, chckbxEnableExtrathumbs,
        jCheckBoxBeanProperty, spDownloadCountExtrathumbs, jSpinnerBeanProperty);
    autoBinding_15.bind();
    //
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty_1 = BeanProperty.create("enabled");
    AutoBinding<JCheckBox, Boolean, JCheckBox, Boolean> autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, chckbxEnableExtrathumbs,
        jCheckBoxBeanProperty, chckbxResizeExtrathumbsTo, jCheckBoxBeanProperty_1);
    autoBinding_8.bind();
    //
    AutoBinding<JCheckBox, Boolean, JSpinner, Boolean> autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, chckbxEnableExtrathumbs,
        jCheckBoxBeanProperty, spExtrathumbWidth, jSpinnerBeanProperty);
    autoBinding_9.bind();
    //
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty_2 = BeanProperty.create("enabled");

    AutoBinding<JCheckBox, Boolean, JCheckBox, Boolean> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxEnableExtrafanart,
        jCheckBoxBeanProperty, chckbxExtrafanart1, jCheckBoxBeanProperty_2);
    autoBinding.bind();
    //
    AutoBinding<JCheckBox, Boolean, JCheckBox, Boolean> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxEnableExtrafanart,
        jCheckBoxBeanProperty, chckbxExtrafanart2, jCheckBoxBeanProperty_2);
    autoBinding_1.bind();
    //
    AutoBinding<JCheckBox, Boolean, JCheckBox, Boolean> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxEnableExtrafanart,
        jCheckBoxBeanProperty, chckbxExtrafanart3, jCheckBoxBeanProperty_2);
    autoBinding_4.bind();
    //
    AutoBinding<JCheckBox, Boolean, JCheckBox, Boolean> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxEnableExtrafanart,
        jCheckBoxBeanProperty, chckbxExtrafanart4, jCheckBoxBeanProperty_2);
    autoBinding_5.bind();
  }
}
