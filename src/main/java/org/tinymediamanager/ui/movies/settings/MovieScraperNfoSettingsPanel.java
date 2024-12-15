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

import java.awt.event.ItemListener;
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
import org.tinymediamanager.core.CertificationStyle;
import org.tinymediamanager.core.DateField;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.connector.MovieConnectors;
import org.tinymediamanager.core.movie.filenaming.MovieNfoNaming;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.LocaleComboBox;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link MovieScraperSettingsPanel} is used to display NFO related settings.
 * 
 * @author Manuel Laggner
 */
class MovieScraperNfoSettingsPanel extends JPanel {
  private final MovieSettings                  settings = MovieModuleManager.getInstance().getSettings();
  private final ItemListener                   checkBoxListener;
  private final ItemListener                   comboBoxListener;

  private JComboBox<MovieConnectors>           cbNfoFormat;
  private JCheckBox                            cbMovieNfoFilename1;
  private JCheckBox                            cbMovieNfoFilename2;
  private JComboBox<CertificationStyleWrapper> cbCertificationStyle;
  private JCheckBox                            chckbxWriteCleanNfo;
  private final List<LocaleComboBox>           locales  = new ArrayList<>();
  private JComboBox                            cbNfoLanguage;
  private JComboBox<DateField>                 cbDatefield;
  private JHintCheckBox                        chckbxCreateOutline;
  private JCheckBox                            chckbxOutlineFirstSentence;
  private JCheckBox                            chckbxSingleStudio;
  private JCheckBox                            chckbxNfoDiscKodiStyle;
  private JHintCheckBox                        chckbxLockdata;
  private JHintCheckBox                        chckbxTrailer;
  private JHintCheckBox                        chckbxFileinfo;

  /**
   * Instantiates a new movie scraper settings panel.
   */
  MovieScraperNfoSettingsPanel() {
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

    // set default certification style when changing NFO style
    cbNfoFormat.addItemListener(e -> {
      if (cbNfoFormat.getSelectedItem() == MovieConnectors.MP) {
        for (int i = 0; i < cbCertificationStyle.getItemCount(); i++) {
          CertificationStyleWrapper wrapper = cbCertificationStyle.getItemAt(i);
          if (wrapper.style == CertificationStyle.TECHNICAL) {
            cbCertificationStyle.setSelectedItem(wrapper);
            break;
          }
        }
      }
      else if (cbNfoFormat.getSelectedItem() == MovieConnectors.XBMC) {
        for (int i = 0; i < cbCertificationStyle.getItemCount(); i++) {
          CertificationStyleWrapper wrapper = cbCertificationStyle.getItemAt(i);
          if (wrapper.style == CertificationStyle.LARGE) {
            cbCertificationStyle.setSelectedItem(wrapper);
            break;
          }
        }
      }
    });

    // implement checkBoxListener for preset events
    settings.addPropertyChangeListener(evt -> {
      if ("preset".equals(evt.getPropertyName()) || "wizard".equals(evt.getPropertyName())) {
        buildCheckBoxes();
        buildComboBoxes();
      }
    });

    buildCheckBoxes();
    buildComboBoxes();
  }

  private void buildCheckBoxes() {
    cbMovieNfoFilename1.removeItemListener(checkBoxListener);
    cbMovieNfoFilename2.removeItemListener(checkBoxListener);

    clearSelection(cbMovieNfoFilename1, cbMovieNfoFilename2);

    // NFO filenames
    List<MovieNfoNaming> movieNfoFilenames = settings.getNfoFilenames();
    if (movieNfoFilenames.contains(MovieNfoNaming.FILENAME_NFO)) {
      cbMovieNfoFilename1.setSelected(true);
    }
    if (movieNfoFilenames.contains(MovieNfoNaming.MOVIE_NFO)) {
      cbMovieNfoFilename2.setSelected(true);
    }

    cbMovieNfoFilename1.addItemListener(checkBoxListener);
    cbMovieNfoFilename2.addItemListener(checkBoxListener);
  }

  private void clearSelection(JCheckBox... checkBoxes) {
    for (JCheckBox checkBox : checkBoxes) {
      checkBox.setSelected(false);
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

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[]"));
    {
      JPanel panelNfo = new JPanel();
      // 16lp ~ width of the
      panelNfo.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][15lp!][][][][][15lp!][][][][15lp!][][][]"));

      JLabel lblNfoT = new TmmLabel(TmmResourceBundle.getString("Settings.nfo"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelNfo, lblNfoT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#nfo-settings"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        JLabel lblNfoFormat = new JLabel(TmmResourceBundle.getString("Settings.nfoFormat"));
        panelNfo.add(lblNfoFormat, "cell 1 0 2 1");

        cbNfoFormat = new JComboBox(MovieConnectors.values());
        panelNfo.add(cbNfoFormat, "cell 1 0 2 1");

        JButton docsButton = new DocsButton("/movies/nfo-formats");
        panelNfo.add(docsButton, "cell 1 0 2 1");

        {
          JPanel panelNfoFormat = new JPanel();
          panelNfo.add(panelNfoFormat, "cell 1 1 2 1");
          panelNfoFormat.setLayout(new MigLayout("insets 0", "[][]", "[][]"));

          JLabel lblNfoFileNaming = new JLabel(TmmResourceBundle.getString("Settings.nofFileNaming"));
          panelNfoFormat.add(lblNfoFileNaming, "cell 0 0");

          cbMovieNfoFilename1 = new JCheckBox(TmmResourceBundle.getString("Settings.moviefilename") + ".nfo");
          panelNfoFormat.add(cbMovieNfoFilename1, "cell 1 0");

          cbMovieNfoFilename2 = new JCheckBox("movie.nfo");
          panelNfoFormat.add(cbMovieNfoFilename2, "cell 1 1");
        }

        chckbxNfoDiscKodiStyle = new JCheckBox(TmmResourceBundle.getString("Settings.nfoDiscFolder"));
        panelNfo.add(chckbxNfoDiscKodiStyle, "cell 2 2");

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

        JLabel lblCertificationStyle = new JLabel(TmmResourceBundle.getString("Settings.certificationformat"));
        panelNfo.add(lblCertificationStyle, "flowx,cell 1 8 2 1");

        cbCertificationStyle = new JComboBox();
        panelNfo.add(cbCertificationStyle, "cell 1 8 2 1,wmin 0");

        chckbxCreateOutline = new JHintCheckBox(TmmResourceBundle.getString("Settings.createoutline"));
        chckbxCreateOutline.setToolTipText(TmmResourceBundle.getString("Settings.createoutline.hint"));
        chckbxCreateOutline.setHintIcon(IconManager.HINT);
        panelNfo.add(chckbxCreateOutline, "cell 1 10 2 1");

        chckbxOutlineFirstSentence = new JCheckBox(TmmResourceBundle.getString("Settings.outlinefirstsentence"));
        panelNfo.add(chckbxOutlineFirstSentence, "cell 2 11");

        chckbxSingleStudio = new JCheckBox(TmmResourceBundle.getString("Settings.singlestudio"));
        panelNfo.add(chckbxSingleStudio, "cell 1 12 2 1");
      }

      {
        chckbxTrailer = new JHintCheckBox(TmmResourceBundle.getString("Settings.nfo.trailer"));
        chckbxTrailer.setToolTipText(TmmResourceBundle.getString("Settings.nfo.trailer.hint"));
        chckbxTrailer.setHintIcon(IconManager.HINT);
        panelNfo.add(chckbxTrailer, "cell 1 14 2 1");

        chckbxFileinfo = new JHintCheckBox(TmmResourceBundle.getString("Settings.nfo.fileinfo"));
        chckbxFileinfo.setToolTipText(TmmResourceBundle.getString("Settings.nfo.fileinfo.hint"));
        chckbxFileinfo.setHintIcon(IconManager.HINT);
        panelNfo.add(chckbxFileinfo, "cell 1 15 2 1");

        chckbxLockdata = new JHintCheckBox(TmmResourceBundle.getString("Settings.lockdata"));
        chckbxLockdata.setToolTipText(TmmResourceBundle.getString("Settings.lockdata.hint"));
        chckbxLockdata.setHintIcon(IconManager.HINT);
        panelNfo.add(chckbxLockdata, "cell 1 16 2 1");
      }
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

    // set NFO filenames
    settings.clearNfoFilenames();
    if (cbMovieNfoFilename1.isSelected()) {
      settings.addNfoFilename(MovieNfoNaming.FILENAME_NFO);
    }
    if (cbMovieNfoFilename2.isSelected()) {
      settings.addNfoFilename(MovieNfoNaming.MOVIE_NFO);
    }

    CertificationStyleWrapper wrapper = (CertificationStyleWrapper) cbCertificationStyle.getSelectedItem();
    if (wrapper != null && settings.getCertificationStyle() != wrapper.style) {
      settings.setCertificationStyle(wrapper.style);
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
    Property settingsBeanProperty_11 = BeanProperty.create("movieConnector");
    Property jComboBoxBeanProperty_1 = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_11, cbNfoFormat,
        jComboBoxBeanProperty_1);
    autoBinding_9.bind();
    //
    Property movieSettingsBeanProperty = BeanProperty.create("writeCleanNfo");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, chckbxWriteCleanNfo,
        jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property movieSettingsBeanProperty_2 = BeanProperty.create("createOutline");
    Property jHintCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_2, chckbxCreateOutline,
        jHintCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property movieSettingsBeanProperty_3 = BeanProperty.create("outlineFirstSentence");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_3,
        chckbxOutlineFirstSentence, jCheckBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property jCheckBoxBeanProperty_1 = BeanProperty.create("enabled");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxCreateOutline, jHintCheckBoxBeanProperty,
        chckbxOutlineFirstSentence, jCheckBoxBeanProperty_1);
    autoBinding_4.bind();
    //
    Property movieSettingsBeanProperty_4 = BeanProperty.create("nfoDateAddedField");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_4, cbDatefield,
        jComboBoxBeanProperty);
    autoBinding_5.bind();
    //
    Property movieSettingsBeanProperty_5 = BeanProperty.create("nfoWriteSingleStudio");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_5, chckbxSingleStudio,
        jCheckBoxBeanProperty);
    autoBinding_6.bind();
    //
    Property movieSettingsBeanProperty_6 = BeanProperty.create("nfoDiscFolderInside");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_6, chckbxNfoDiscKodiStyle,
        jCheckBoxBeanProperty);
    autoBinding_7.bind();
    //
    Property movieSettingsBeanProperty_7 = BeanProperty.create("nfoWriteLockdata");
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_7, chckbxLockdata,
        jCheckBoxBeanProperty);
    autoBinding_8.bind();
    //
    Property movieSettingsBeanProperty_1 = BeanProperty.create("nfoWriteTrailer");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_1, chckbxTrailer,
        jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    Property movieSettingsBeanProperty_8 = BeanProperty.create("nfoWriteFileinfo");
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_8, chckbxFileinfo,
        jCheckBoxBeanProperty);
    autoBinding_10.bind();
  }
}
