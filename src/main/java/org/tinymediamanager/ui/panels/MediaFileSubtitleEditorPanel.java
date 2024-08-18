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
package org.tinymediamanager.ui.panels;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;
import org.tinymediamanager.ui.components.table.TmmTableFormat;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link MediaFileSubtitleEditorPanel} is used to add/edit subtitles
 * 
 * @author Manuel Laggner
 */
public class MediaFileSubtitleEditorPanel extends AbstractModalInputPanel {
  private final MediaFileSubtitle               subtitle;

  private final AutocompleteComboBox            cbLanguage;
  private final JCheckBox                       chkbxForced;
  private final JCheckBox                       chkbxSdh;
  private final JTextField                      tfFormat;
  private final JTextField                      tfTitle;

  private final TmmTableFormat.StringComparator stringComparator;

  public MediaFileSubtitleEditorPanel(MediaFileSubtitle subtitle) {
    super();

    this.subtitle = subtitle;

    stringComparator = new TmmTableFormat.StringComparator();

    List<LanguageContainer> languages = new ArrayList<>();
    for (Locale locale : Locale.getAvailableLocales()) {
      LanguageContainer localeContainer = new LanguageContainer(locale);
      if (!languages.contains(localeContainer)) {
        languages.add(localeContainer);
      }
    }
    languages.sort((o1, o2) -> stringComparator.compare(o1.toString(), o2.toString()));

    // add our unknown/invalid/LocaleString subtitle value to GUI list
    LanguageContainer fileLocale = new LanguageContainer(subtitle.getLanguage());
    if (fileLocale.locale == null || (fileLocale.locale != null && !languages.contains(fileLocale))) {
      // 1) locale could not be parsed (like "asdf" or similar) - add it
      // 2) locale COULD be parsed (3 chars like "asd"), but not already in our array
      // 3) Valid Locale (like de_AT) found, but since we have a language-only list, add it
      languages.add(fileLocale);
    }

    {
      setLayout(new MigLayout("", "[][][300lp,grow]", "[][][][][]"));
      {
        JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
        add(lblLanguageT, "cell 0 0,alignx trailing");

        cbLanguage = new AutocompleteComboBox(languages.toArray());
        add(cbLanguage, "cell 1 0 2 1, wmin 50%");
      }
      {
        JLabel lblForcedT = new TmmLabel(TmmResourceBundle.getString("metatag.forced"));
        add(lblForcedT, "cell 0 1,alignx trailing");

        chkbxForced = new JCheckBox();
        add(chkbxForced, "cell 1 1, growx");
      }
      {
        JLabel lblSdhT = new TmmLabel(TmmResourceBundle.getString("metatag.sdh"));
        add(lblSdhT, "cell 0 2,alignx trailing");

        chkbxSdh = new JCheckBox();
        add(chkbxSdh, "cell 1 2, growx");
      }
      {
        JLabel lblFormatT = new TmmLabel(TmmResourceBundle.getString("metatag.format"));
        add(lblFormatT, "cell 0 3,alignx trailing");

        tfFormat = new JTextField();
        tfFormat.setColumns(8);
        add(tfFormat, "cell 1 3, growx");
      }
      {
        JLabel lblTitleT = new TmmLabel(TmmResourceBundle.getString("metatag.title"));
        add(lblTitleT, "cell 0 4,alignx trailing");

        tfTitle = new JTextField();
        tfTitle.setColumns(30);
        add(tfTitle, "cell 1 4 2 1, growx");
      }
    }

    tfFormat.setText(subtitle.getCodec());
    chkbxForced.setSelected(subtitle.isForced());
    chkbxSdh.setSelected(subtitle.isSdh());
    tfTitle.setText(subtitle.getTitle());

    LanguageContainer foundByValue = languages.stream().filter(v -> v.value.equalsIgnoreCase(subtitle.getLanguage())).findFirst().get();
    cbLanguage.setSelectedItem(foundByValue);

    // set focus to the first combobox
    SwingUtilities.invokeLater(cbLanguage::requestFocus);
  }

  @Override
  protected void onClose() {
    subtitle.setCodec(tfFormat.getText());
    subtitle.setForced(chkbxForced.isSelected());
    subtitle.setSdh(chkbxSdh.isSelected());

    Object obj = cbLanguage.getSelectedItem();
    if (obj instanceof LanguageContainer localeContainer) {
      subtitle.setLanguage(localeContainer.value.strip());
    }
    else if (obj instanceof String language) {
      subtitle.setLanguage(language.strip());
    }

    subtitle.setTitle(tfTitle.getText());

    setVisible(false);
  }

  private static class LanguageContainer {
    private final String value;
    private Locale       locale;
    private final String description;

    public LanguageContainer(@NotNull Locale locale) {
      this.locale = locale;
      this.value = locale.getISO3Language();
      this.description = StringUtils.isNotBlank(locale.getDisplayLanguage()) ? locale.getDisplayLanguage() + " (" + this.value + ")" : "";
    }

    public LanguageContainer(@NotNull String locale) {
      Locale tmp = LanguageUtils.KEY_TO_LOCALE_MAP.get(locale);
      // if WE dont have it in our array, it is not valid!
      if (tmp != null) {
        this.locale = tmp;
      }
      this.value = locale;
      this.description = this.locale != null ? value + " (" + this.locale.getISO3Language() + ")" : value;
    }

    @Override
    public String toString() {
      return description;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LanguageContainer that = (LanguageContainer) o;
      return value.equals(that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }
  }
}
