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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.swingbinding.JListBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link MovieScraperSettingsPanel} shows scraper options for the artwork scraper.
 *
 * @author Manuel Laggner
 */
class MovieImageOptionsSettingsPanel extends JPanel {
  private final MovieSettings       settings = MovieModuleManager.getInstance().getSettings();

  private JCheckBox                 chckbxAutomaticallyScrapeImages;
  private JComboBox                 cbImagePosterSize;
  private JComboBox                 cbImageFanartSize;

  private JComboBox<MediaLanguages> cbScraperLanguage;

  private JList                     listLanguages;
  private JButton                   btnAddLanguage;
  private JButton                   btnRemoveLanguage;
  private JButton                   btnMoveLanguageUp;
  private JButton                   btnMoveLanguageDown;
  private JCheckBox                 chckbxResolutions;
  private JCheckBox                 chckbxFallback;
  private JCheckBox                 chckbxFanartWoText;

  /**
   * Instantiates a new movie scraper settings panel.
   */
  MovieImageOptionsSettingsPanel() {
    // UI init
    initComponents();
    initDataBindings();

    // logic initializations
    btnAddLanguage.addActionListener(arg0 -> {
      Object selectedItem = cbScraperLanguage.getSelectedItem();

      if (selectedItem instanceof MediaLanguages language) {
        MovieModuleManager.getInstance().getSettings().addImageScraperLanguage(language);
      }
    });

    btnRemoveLanguage.addActionListener(arg0 -> {
      int row = listLanguages.getSelectedIndex();
      if (row != -1) { // nothing selected
        MediaLanguages language = settings.getImageScraperLanguages().get(row);
        MovieModuleManager.getInstance().getSettings().removeImageScraperLanguage(language);
      }
    });

    btnMoveLanguageUp.addActionListener(arg0 -> {
      int row = listLanguages.getSelectedIndex();
      if (row != -1 && row != 0) {
        settings.swapImageScraperLanguage(row, row - 1);
        row = row - 1;
        listLanguages.setSelectedIndex(row);
        listLanguages.updateUI();
      }
    });

    btnMoveLanguageDown.addActionListener(arg0 -> {
      int row = listLanguages.getSelectedIndex();
      if (row != -1 && row < listLanguages.getModel().getSize() - 1) {
        settings.swapImageScraperLanguage(row, row + 1);
        row = row + 1;
        listLanguages.setSelectedIndex(row);
        listLanguages.updateUI();
      }
    });
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[700lp,grow]", "[]"));

    {
      JPanel panelOptions = new JPanel();
      panelOptions.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][15lp!][][][][15lp!][][]")); // 16lp ~ width of the

      JLabel lblOptionsT = new TmmLabel(TmmResourceBundle.getString("Settings.advancedoptions"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelOptions, lblOptionsT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#advanced-options-1"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");
      {
        chckbxAutomaticallyScrapeImages = new JCheckBox(TmmResourceBundle.getString("Settings.default.autoscrape"));
        panelOptions.add(chckbxAutomaticallyScrapeImages, "cell 1 0 2 1");

        JLabel lblImageTmdbPosterSize = new JLabel(TmmResourceBundle.getString("image.poster.size"));
        panelOptions.add(lblImageTmdbPosterSize, "cell 1 1 2 1");

        cbImagePosterSize = new JComboBox(MediaArtwork.PosterSizes.values());
        panelOptions.add(cbImagePosterSize, "cell 1 1 2 1");

        JLabel lblImageTmdbFanartSize = new JLabel(TmmResourceBundle.getString("image.fanart.size"));
        panelOptions.add(lblImageTmdbFanartSize, "cell 1 2 2 1");

        cbImageFanartSize = new JComboBox(MediaArtwork.FanartSizes.values());
        panelOptions.add(cbImageFanartSize, "cell 1 2 2 1");
      }
      {
        JLabel lblScraperLanguage = new JLabel(TmmResourceBundle.getString("Settings.preferredLanguage"));
        panelOptions.add(lblScraperLanguage, "cell 1 4 2 1");

        JPanel panelLanguagegSource = new JPanel();
        panelOptions.add(panelLanguagegSource, "cell 2 5,grow");
        panelLanguagegSource.setLayout(new MigLayout("insets 0", "[100lp][]", "[grow][]"));

        listLanguages = new JList();
        listLanguages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(listLanguages);
        panelLanguagegSource.add(scrollPane, "cell 0 0,grow");

        btnMoveLanguageUp = new SquareIconButton(IconManager.ARROW_UP_INV);
        btnMoveLanguageUp.setToolTipText(TmmResourceBundle.getString("Button.moveup"));
        panelLanguagegSource.add(btnMoveLanguageUp, "flowy,cell 1 0,aligny bottom");

        btnMoveLanguageDown = new SquareIconButton(IconManager.ARROW_DOWN_INV);
        btnMoveLanguageDown.setToolTipText(TmmResourceBundle.getString("Button.movedown"));
        panelLanguagegSource.add(btnMoveLanguageDown, "cell 1 0,aligny bottom");

        cbScraperLanguage = new JComboBox(MediaLanguages.allValuesSorted());
        panelLanguagegSource.add(cbScraperLanguage, "cell 0 1,growx");

        btnRemoveLanguage = new SquareIconButton(IconManager.REMOVE_INV);
        btnRemoveLanguage.setToolTipText(TmmResourceBundle.getString("Button.remove"));
        panelLanguagegSource.add(btnRemoveLanguage, "cell 1 0");

        btnAddLanguage = new SquareIconButton(IconManager.ADD_INV);
        btnAddLanguage.setToolTipText(TmmResourceBundle.getString("Button.add"));
        panelLanguagegSource.add(btnAddLanguage, "cell 1 1");
      }

      chckbxFanartWoText = new JCheckBox(TmmResourceBundle.getString("Settings.default.autoscrape.fanartwotext"));
      panelOptions.add(chckbxFanartWoText, "cell 1 6 2 1");

      chckbxResolutions = new JCheckBox(TmmResourceBundle.getString("Settings.default.autoscrape.resolutions"));
      panelOptions.add(chckbxResolutions, "cell 1 8 2 1");

      chckbxFallback = new JCheckBox(TmmResourceBundle.getString("Settings.default.autoscrape.fallback"));
      panelOptions.add(chckbxFallback, "cell 1 9 2 1");
    }
  }

  protected void initDataBindings() {
    Property settingsBeanProperty_5 = BeanProperty.create("imagePosterSize");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_5, cbImagePosterSize,
        jComboBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property settingsBeanProperty_6 = BeanProperty.create("imageFanartSize");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_6, cbImageFanartSize,
        jComboBoxBeanProperty);
    autoBinding_5.bind();
    //
    Property movieSettingsBeanProperty = BeanProperty.create("imageScraperLanguages");
    JListBinding jListBinding = SwingBindings.createJListBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, listLanguages);
    jListBinding.bind();
    //
    Property movieSettingsBeanProperty_1 = BeanProperty.create("imageScraperPreferFanartWoText");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_1, chckbxFanartWoText,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property movieSettingsBeanProperty_2 = BeanProperty.create("imageScraperOtherResolutions");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_2, chckbxResolutions,
        jCheckBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property movieSettingsBeanProperty_3 = BeanProperty.create("imageScraperFallback");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_3, chckbxFallback,
        jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property settingsBeanProperty = BeanProperty.create("scrapeBestImage");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty, chckbxAutomaticallyScrapeImages,
        jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
  }
}
