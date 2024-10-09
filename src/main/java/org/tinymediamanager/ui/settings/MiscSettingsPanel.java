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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MiscSettingsPanel.
 * 
 * @author Manuel Laggner
 */
class MiscSettingsPanel extends JPanel {
  private final Settings settings = Settings.getInstance();
  private JComboBox      cbImageCacheQuality;
  private JCheckBox      chckbxImageCache;
  private JCheckBox      chckbxDeleteTrash;
  private JCheckBox      chckbxMediaInfoXml;
  private JComboBox      cbImageCacheSize;
  private JCheckBox      chckbxEnableTrash;

  /**
   * Instantiates a new general settings panel.
   */
  MiscSettingsPanel() {
    initComponents();
    initDataBindings();

  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][]"));
    {
      JPanel panelCache = new JPanel();
      panelCache.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][]")); // 16lp ~ width of the

      JLabel lblCacheT = new TmmLabel(TmmResourceBundle.getString("Settings.cache"), H3);
      CollapsiblePanel collapsiblePanelMisc = new CollapsiblePanel(panelCache, lblCacheT, true);
      collapsiblePanelMisc.addExtraTitleComponent(new DocsButton("/settings#misc-settings-2"));
      add(collapsiblePanelMisc, "cell 0 0,growx, wmin 0");
      {
        chckbxImageCache = new JCheckBox(TmmResourceBundle.getString("Settings.imagecache"));
        panelCache.add(chckbxImageCache, "cell 1 0 2 1");

        JLabel lblImageCacheSize = new JLabel(TmmResourceBundle.getString("Settings.imagecachesize"));
        panelCache.add(lblImageCacheSize, "flowx,cell 2 1");

        cbImageCacheSize = new JComboBox(ImageCache.CacheSize.values());
        panelCache.add(cbImageCacheSize, "cell 2 1");

        {
          JPanel panel = new JPanel();
          panelCache.add(panel, "cell 2 2,grow");
          panel.setLayout(new MigLayout("", "[10lp:n][grow]", "[]"));

          JTextArea lblImageCacheSizeSmallT = new ReadOnlyTextArea("SMALL - " + TmmResourceBundle.getString("Settings.imagecachesize.small"));
          panel.add(lblImageCacheSizeSmallT, "flowy,cell 1 0,growx, wmin 0");

          JTextArea lblImageCacheSizeBigT = new ReadOnlyTextArea("BIG - " + TmmResourceBundle.getString("Settings.imagecachesize.big"));
          panel.add(lblImageCacheSizeBigT, "cell 1 0,growx, wmin 0");

          JTextArea lblImageCacheSizeOriginalT = new ReadOnlyTextArea(
              "ORIGINAL - " + TmmResourceBundle.getString("Settings.imagecachesize.original"));
          panel.add(lblImageCacheSizeOriginalT, "cell 1 0,growx, wmin 0");
        }
        JLabel lblImageCacheQuality = new JLabel(TmmResourceBundle.getString("Settings.imagecachetype"));
        panelCache.add(lblImageCacheQuality, "cell 2 3");

        cbImageCacheQuality = new JComboBox(ImageCache.CacheType.values());
        panelCache.add(cbImageCacheQuality, "cell 2 3");

        {
          JPanel panel = new JPanel();
          panelCache.add(panel, "cell 2 4,grow");
          panel.setLayout(new MigLayout("", "[10lp:n][grow]", "[]"));

          JTextArea lblImageCacheTypeBalancedT = new ReadOnlyTextArea(
              "BALANCED - " + TmmResourceBundle.getString("Settings.imagecachetype.balanced"));
          panel.add(lblImageCacheTypeBalancedT, "flowy,cell 1 0,growx, wmin 0");

          JTextArea lblImageCacheTypeQualityT = new ReadOnlyTextArea("QUALITY - " + TmmResourceBundle.getString("Settings.imagecachetype.quality"));
          panel.add(lblImageCacheTypeQualityT, "cell 1 0,growx, wmin 0");

          JTextArea lblImageCacheTypeUltraQualityT = new ReadOnlyTextArea(
              "ULTRA_QUALITY - " + TmmResourceBundle.getString("Settings.imagecachetype.ultra_quality"));
          panel.add(lblImageCacheTypeUltraQualityT, "cell 1 0,growx, wmin 0");
        }
      }
    }
    {
      JPanel Misc = new JPanel();
      Misc.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][]")); // 16lp ~ width of the

      JLabel lblCacheT = new TmmLabel(TmmResourceBundle.getString("Settings.misc"), H3);
      CollapsiblePanel collapsiblePanelMisc = new CollapsiblePanel(Misc, lblCacheT, true);
      collapsiblePanelMisc.addExtraTitleComponent(new DocsButton("/settings#misc-settings-2"));
      add(collapsiblePanelMisc, "cell 0 2,growx, wmin 0");
      {
        chckbxEnableTrash = new JCheckBox(TmmResourceBundle.getString("Settings.enabletrash"));
        chckbxEnableTrash.setToolTipText(TmmResourceBundle.getString("Settings.enabletrash.desc"));
        Misc.add(chckbxEnableTrash, "cell 1 0 2 1");

        chckbxDeleteTrash = new JCheckBox(TmmResourceBundle.getString("Settings.deletetrash"));
        Misc.add(chckbxDeleteTrash, "cell 1 1 2 1");

        chckbxMediaInfoXml = new JCheckBox(TmmResourceBundle.getString("Settings.writemediainfoxml"));
        Misc.add(chckbxMediaInfoXml, "cell 1 2 2 1");
      }
    }
  }

  protected void initDataBindings() {
    Property settingsBeanProperty_7 = BeanProperty.create("imageCacheType");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_7, cbImageCacheQuality,
        jComboBoxBeanProperty);
    autoBinding_5.bind();
    //
    Property settingsBeanProperty_9 = BeanProperty.create("imageCache");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_9, chckbxImageCache,
        jCheckBoxBeanProperty);
    autoBinding_7.bind();
    //
    Property settingsBeanProperty_10 = BeanProperty.create("deleteTrashOnExit");
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_10, chckbxDeleteTrash,
        jCheckBoxBeanProperty);
    autoBinding_10.bind();
    //
    Property settingsBeanProperty = BeanProperty.create("writeMediaInfoXml");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty, chckbxMediaInfoXml,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property settingsBeanProperty_1 = BeanProperty.create("imageCacheSize");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_1, cbImageCacheSize,
        jComboBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property settingsBeanProperty_2 = BeanProperty.create("enableTrash");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_2, chckbxEnableTrash,
        jCheckBoxBeanProperty);
    autoBinding_2.bind();
  }
}
