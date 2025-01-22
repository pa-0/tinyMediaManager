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

import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.CertificationStyle;
import org.tinymediamanager.core.DateField;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.connector.TvShowConnectors;
import org.tinymediamanager.core.tvshow.filenaming.TvShowEpisodeNfoNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowNfoNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonNfoNaming;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.LocaleComboBox;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TvShowScraperNfoSettingsPanel} is used to display NFO related settings.
 * 
 * @author Manuel Laggner
 */
class TvShowScraperNfoSettingsPanel extends JPanel {
  private static final Logger                  LOGGER   = LoggerFactory.getLogger(TvShowScraperNfoSettingsPanel.class);

  private final TvShowSettings                 settings = TvShowModuleManager.getInstance().getSettings();
  private final ItemListener                   checkBoxListener;
  private final ItemListener                   comboBoxListener;

  private JComboBox<TvShowConnectors>          cbNfoFormat;
  private JComboBox<CertificationStyleWrapper> cbCertificationStyle;
  private JCheckBox                            chckbxWriteCleanNfo;
  private final List<LocaleComboBox>           locales  = new ArrayList<>();
  private JComboBox                            cbNfoLanguage;
  private JComboBox<DateField>                 cbDatefield;
  private JCheckBox                            chckbxEpisodeNfo1;
  private JCheckBox                            chckbxTvShowNfo1;
  private JCheckBox                            chckbxSeasonNfo1;
  private JCheckBox                            chckbxWriteEpisodeguide;
  private JCheckBox                            chckbxWriteDateEnded;
  private JCheckBox                            chckbxEmbedAllActors;
  private JCheckBox                            chckbxFirstStudio;
  private JHintCheckBox                        chckbxLockdata;
  private JCheckBox                            chckbxNewEpisodeguideFormat;
  private JHintCheckBox                        chckbxTrailer;
  private JHintCheckBox                        chckbxFileinfo;

  /**
   * Instantiates a new movie scraper settings panel.
   */
  TvShowScraperNfoSettingsPanel() {
    checkBoxListener = e -> checkChanges();
    comboBoxListener = e -> checkChanges();

    LocaleComboBox actualLocale = null;
    LocaleComboBox fallbackLocale = null;
    Locale settingsLang = Utils.getLocaleFromLanguage(settings.getNfoLanguage().toString());
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

    // UI init
    initComponents();
    initDataBindings();

    // data init
    if (actualLocale != null) {
      cbNfoLanguage.setSelectedItem(actualLocale);
    }
    else {
      cbNfoLanguage.setSelectedItem(fallbackLocale);
    }
    cbNfoLanguage.addItemListener(comboBoxListener);

    // implement checkBoxListener for preset events
    settings.addPropertyChangeListener(evt -> {
      if ("preset".equals(evt.getPropertyName()) || "wizard".equals(evt.getPropertyName())) {
        buildComboBoxes();
        buildCheckBoxes();
      }
    });

    buildCheckBoxes();
    buildComboBoxes();
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[]"));
    {
      JPanel panelNfo = new JPanel();
      // 16lp ~ width of the checkbox
      panelNfo.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][15lp!][][][][][][][15lp!][][][][][][][]"));
      JLabel lblNfoT = new TmmLabel(TmmResourceBundle.getString("Settings.nfo"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelNfo, lblNfoT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#nfo-settings"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        JLabel lblNfoFormatT = new JLabel(TmmResourceBundle.getString("Settings.nfoFormat"));
        panelNfo.add(lblNfoFormatT, "cell 1 0 2 1");

        cbNfoFormat = new JComboBox(TvShowConnectors.values());
        panelNfo.add(cbNfoFormat, "cell 1 0 2 1");

        JButton docsButton = new DocsButton("/tvshows/nfo-formats");
        panelNfo.add(docsButton, "cell 1 0 2 1");

        {
          JLabel lblNfoFileNaming = new JLabel(TmmResourceBundle.getString("Settings.nofFileNaming"));
          panelNfo.add(lblNfoFileNaming, "cell 1 1 2 1");

          JPanel panel = new JPanel();
          panelNfo.add(panel, "cell 2 2");
          panel.setLayout(new MigLayout("insets 0", "[][]", "[][][]"));

          JLabel lblTvShow = new JLabel(TmmResourceBundle.getString("metatag.tvshow"));
          panel.add(lblTvShow, "cell 0 0");

          chckbxTvShowNfo1 = new JCheckBox("tvshow.nfo");
          panel.add(chckbxTvShowNfo1, "cell 1 0");

          JLabel lblSeason = new JLabel(TmmResourceBundle.getString("metatag.season"));
          panel.add(lblSeason, "cell 0 1");

          chckbxSeasonNfo1 = new JCheckBox("<season_folder>" + File.separator + "season.nfo");
          panel.add(chckbxSeasonNfo1, "cell 1 1");

          JLabel lblEpisode = new JLabel(TmmResourceBundle.getString("metatag.episode"));
          panel.add(lblEpisode, "cell 0 2");

          chckbxEpisodeNfo1 = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.episodename") + ".nfo");
          panel.add(chckbxEpisodeNfo1, "cell 1 2");
        }

        chckbxWriteCleanNfo = new JCheckBox(TmmResourceBundle.getString("Settings.writecleannfo"));
        panelNfo.add(chckbxWriteCleanNfo, "cell 1 3 2 1");

        JLabel lblNfoDatefield = new JLabel(TmmResourceBundle.getString("Settings.dateadded"));
        panelNfo.add(lblNfoDatefield, "cell 1 5 2 1");

        cbDatefield = new JComboBox(DateField.values());
        panelNfo.add(cbDatefield, "cell 1 5 2 1");

        JLabel lblNfoLanguage = new JLabel(TmmResourceBundle.getString("Settings.nfolanguage"));
        panelNfo.add(lblNfoLanguage, "cell 1 6 2 1");

        cbNfoLanguage = new JComboBox(locales.toArray());
        panelNfo.add(cbNfoLanguage, "cell 1 6 2 1");

        JLabel lblNfoLanguageDesc = new JLabel(TmmResourceBundle.getString("Settings.nfolanguage.desc"));
        panelNfo.add(lblNfoLanguageDesc, "cell 2 7");

        JLabel lblCertificationFormatT = new JLabel(TmmResourceBundle.getString("Settings.certificationformat"));
        panelNfo.add(lblCertificationFormatT, "cell 1 8 2 1");

        cbCertificationStyle = new JComboBox();
        panelNfo.add(cbCertificationStyle, "cell 1 8 2 1,wmin 0");

        chckbxEmbedAllActors = new JCheckBox(TmmResourceBundle.getString("Settings.nfo.includeallactors"));
        panelNfo.add(chckbxEmbedAllActors, "cell 1 9 2 1");

        chckbxFirstStudio = new JCheckBox(TmmResourceBundle.getString("Settings.singlestudio"));
        panelNfo.add(chckbxFirstStudio, "cell 1 10 2 1");
      }

      chckbxWriteEpisodeguide = new JCheckBox(TmmResourceBundle.getString("Settings.writeepisodeguide"));
      panelNfo.add(chckbxWriteEpisodeguide, "cell 1 12 2 1");

      chckbxNewEpisodeguideFormat = new JCheckBox(TmmResourceBundle.getString("Settings.writeepisodeguide.newstyle"));
      panelNfo.add(chckbxNewEpisodeguideFormat, "cell 2 13");

      LinkLabel lblEpisodeGuideLink = new LinkLabel("https://forum.kodi.tv/showthread.php?tid=370489");
      lblEpisodeGuideLink.addActionListener(arg0 -> {
        try {
          TmmUIHelper.browseUrl(lblEpisodeGuideLink.getText());
        }
        catch (Exception e) {
          LOGGER.error(e.getMessage());
          MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, lblEpisodeGuideLink.getText(), "message.erroropenurl",
              new String[] { ":", e.getLocalizedMessage() }));//$NON-NLS-1$
        }
      });

      panelNfo.add(lblEpisodeGuideLink, "cell 2 14");

      chckbxTrailer = new JHintCheckBox(TmmResourceBundle.getString("Settings.nfo.trailer"));
      chckbxTrailer.setToolTipText(TmmResourceBundle.getString("Settings.nfo.trailer.hint"));
      chckbxTrailer.setHintIcon(IconManager.HINT);
      panelNfo.add(chckbxTrailer, "cell 1 15 2 1");

      chckbxFileinfo = new JHintCheckBox(TmmResourceBundle.getString("Settings.nfo.fileinfo"));
      chckbxFileinfo.setToolTipText(TmmResourceBundle.getString("Settings.nfo.fileinfo.hint"));
      chckbxFileinfo.setHintIcon(IconManager.HINT);
      panelNfo.add(chckbxFileinfo, "cell 1 16 2 1");

      chckbxWriteDateEnded = new JCheckBox(TmmResourceBundle.getString("Settings.nfo.writeenddate"));
      panelNfo.add(chckbxWriteDateEnded, "cell 1 17 2 1");

      chckbxLockdata = new JHintCheckBox(TmmResourceBundle.getString("Settings.lockdata"));
      chckbxLockdata.setToolTipText(TmmResourceBundle.getString("Settings.lockdata.hint"));
      chckbxLockdata.setHintIcon(IconManager.HINT);
      panelNfo.add(chckbxLockdata, "cell 1 18 2 1");
    }
  }

  /**
   * check changes of checkboxes
   */
  private void checkChanges() {
    LocaleComboBox loc = (LocaleComboBox) cbNfoLanguage.getSelectedItem();
    if (loc != null) {
      Locale actualLocale = settings.getNfoLanguage();
      if (!loc.getLocale().equals(actualLocale)) {
        settings.setNfoLanguage(loc.getLocale());
      }
    }

    CertificationStyleWrapper wrapper = (CertificationStyleWrapper) cbCertificationStyle.getSelectedItem();
    if (wrapper != null && settings.getCertificationStyle() != wrapper.style) {
      settings.setCertificationStyle(wrapper.style);
    }

    // set NFO filenames
    settings.clearNfoFilenames();
    if (chckbxTvShowNfo1.isSelected()) {
      settings.addNfoFilename(TvShowNfoNaming.TV_SHOW);
    }

    settings.clearSeasonNfoFilenames();
    if (chckbxSeasonNfo1.isSelected()) {
      settings.addSeasonNfoFilename(TvShowSeasonNfoNaming.SEASON_FOLDER);
    }

    settings.clearEpisodeNfoFilenames();
    if (chckbxEpisodeNfo1.isSelected()) {
      settings.addEpisodeNfoFilename(TvShowEpisodeNfoNaming.FILENAME);
    }
  }

  private void buildComboBoxes() {
    cbCertificationStyle.removeItemListener(comboBoxListener);
    cbCertificationStyle.removeAllItems();

    // certification examples
    for (CertificationStyle style : CertificationStyle.values()) {
      CertificationStyleWrapper wrapper = new CertificationStyleWrapper();
      wrapper.style = style;
      cbCertificationStyle.addItem(wrapper);
      if (style == settings.getCertificationStyle()) {
        cbCertificationStyle.setSelectedItem(wrapper);
      }
    }

    cbCertificationStyle.addItemListener(comboBoxListener);
  }

  private void buildCheckBoxes() {
    chckbxTvShowNfo1.removeItemListener(checkBoxListener);
    chckbxSeasonNfo1.removeItemListener(checkBoxListener);
    chckbxEpisodeNfo1.removeItemListener(checkBoxListener);
    clearSelection(chckbxTvShowNfo1, chckbxSeasonNfo1, chckbxEpisodeNfo1);

    // NFO filenames
    List<TvShowNfoNaming> tvShowNfoNamings = settings.getNfoFilenames();
    if (tvShowNfoNamings.contains(TvShowNfoNaming.TV_SHOW)) {
      chckbxTvShowNfo1.setSelected(true);
    }

    List<TvShowSeasonNfoNaming> seasonNfoNamings = settings.getSeasonNfoFilenames();
    if (seasonNfoNamings.contains(TvShowSeasonNfoNaming.SEASON_FOLDER)) {
      chckbxSeasonNfo1.setSelected(true);
    }

    List<TvShowEpisodeNfoNaming> TvShowEpisodeNfoNamings = settings.getEpisodeNfoFilenames();
    if (TvShowEpisodeNfoNamings.contains(TvShowEpisodeNfoNaming.FILENAME)) {
      chckbxEpisodeNfo1.setSelected(true);
    }

    chckbxTvShowNfo1.addItemListener(checkBoxListener);
    chckbxSeasonNfo1.addItemListener(checkBoxListener);
    chckbxEpisodeNfo1.addItemListener(checkBoxListener);
  }

  private void clearSelection(JCheckBox... checkBoxes) {
    for (JCheckBox checkBox : checkBoxes) {
      checkBox.setSelected(false);
    }
  }

  /*
   * helper for displaying the combobox with an example
   */
  private static class CertificationStyleWrapper {
    private CertificationStyle style;

    @Override
    public String toString() {
      String bundleTag = TmmResourceBundle.getString("Settings.certification." + style.name().toLowerCase(Locale.ROOT));
      return bundleTag.replace("{}", CertificationStyle.formatCertification(MediaCertification.DE_FSK16, style));
    }
  }

  protected void initDataBindings() {
    Property tvShowSettingsBeanProperty = BeanProperty.create("tvShowConnector");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty, cbNfoFormat,
        jComboBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property tvShowSettingsBeanProperty_1 = BeanProperty.create("writeCleanNfo");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_1, chckbxWriteCleanNfo,
        jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property tvShowSettingsBeanProperty_3 = BeanProperty.create("nfoDateAddedField");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_3, cbDatefield,
        jComboBoxBeanProperty);
    autoBinding_5.bind();
    //
    Property tvShowSettingsBeanProperty_4 = BeanProperty.create("nfoWriteEpisodeguide");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_4, chckbxWriteEpisodeguide,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property tvShowSettingsBeanProperty_5 = BeanProperty.create("nfoWriteDateEnded");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_5, chckbxWriteDateEnded,
        jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    Property tvShowSettingsBeanProperty_6 = BeanProperty.create("nfoWriteAllActors");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_6, chckbxEmbedAllActors,
        jCheckBoxBeanProperty);
    autoBinding_6.bind();
    //
    Property tvShowSettingsBeanProperty_7 = BeanProperty.create("nfoWriteSingleStudio");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_7, chckbxFirstStudio,
        jCheckBoxBeanProperty);
    autoBinding_7.bind();
    //
    Property tvShowSettingsBeanProperty_8 = BeanProperty.create("nfoWriteLockdata");
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_8, chckbxLockdata,
        jCheckBoxBeanProperty);
    autoBinding_8.bind();
    //
    Property tvShowSettingsBeanProperty_9 = BeanProperty.create("nfoWriteNewEpisodeguideStyle");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_9,
        chckbxNewEpisodeguideFormat, jCheckBoxBeanProperty);
    autoBinding_9.bind();
    //
    Property tvShowSettingsBeanProperty_2 = BeanProperty.create("nfoWriteTrailer");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_2, chckbxTrailer,
        jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property tvShowSettingsBeanProperty_10 = BeanProperty.create("nfoWriteFileinfo");
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_10, chckbxFileinfo,
        jCheckBoxBeanProperty);
    autoBinding_10.bind();
  }
}
