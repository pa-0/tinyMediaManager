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
import java.util.Optional;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;
import org.tinymediamanager.ui.components.table.TmmTableFormat;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link MediaFileAudioStreamEditorPanel} is used to add/edit audio streams
 * 
 * @author Manuel Laggner
 */
public class MediaFileAudioStreamEditorPanel extends AbstractModalInputPanel {

  private final MediaFileAudioStream            audioStream;

  private final JTextField                      tfCodec;
  private final JSpinner                        spChannels;
  private final JSpinner                        spBitrate;
  private final JSpinner                        spBitdepth;
  private final AutocompleteComboBox            cbLanguage;
  private final JTextField                      tfTitle;

  private final TmmTableFormat.StringComparator stringComparator;

  public MediaFileAudioStreamEditorPanel(MediaFileAudioStream audioStream) {
    super();

    this.audioStream = audioStream;

    stringComparator = new TmmTableFormat.StringComparator();

    List<LanguageContainer> languages = new ArrayList<>();
    for (Locale locale : Locale.getAvailableLocales()) {
      LanguageContainer localeContainer = new LanguageContainer(locale);
      if (!languages.contains(localeContainer)) {
        languages.add(localeContainer);
      }
    }
    languages.sort((o1, o2) -> stringComparator.compare(o1.toString(), o2.toString()));

    {
      setLayout(new MigLayout("", "[][][300lp,grow]", "[][][][][][]"));
      {
        JLabel lblCodecT = new TmmLabel(TmmResourceBundle.getString("metatag.codec"));
        add(lblCodecT, "cell 0 0,alignx trailing");

        tfCodec = new JTextField();
        tfCodec.setColumns(10);
        add(tfCodec, "cell 1 0 2 1, growx");
      }
      {
        JLabel lblChannelsT = new TmmLabel(TmmResourceBundle.getString("metatag.channels"));
        add(lblChannelsT, "cell 0 1,alignx trailing");

        spChannels = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
        add(spChannels, "cell 1 1, growx");
      }
      {
        JLabel lblBitrate = new TmmLabel(TmmResourceBundle.getString("metatag.bitrate"));
        add(lblBitrate, "cell 0 2,alignx trailing");

        spBitrate = new JSpinner(new SpinnerNumberModel(0, 0, 9000, 1));
        add(spBitrate, "cell 1 2, growx");
      }
      {
        JLabel lblBitdepthT = new TmmLabel(TmmResourceBundle.getString("metatag.bitdepth"));
        add(lblBitdepthT, "cell 0 3,alignx trailing");

        spBitdepth = new JSpinner(new SpinnerNumberModel(0, 0, 20, 1));
        add(spBitdepth, "cell 1 3, growx");
      }
      {
        JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
        add(lblLanguageT, "cell 0 4,alignx trailing");

        cbLanguage = new AutocompleteComboBox(languages.toArray());
        add(cbLanguage, "cell 1 4 2 1, wmin 50%");
      }
      {
        JLabel lblTitleT = new TmmLabel(TmmResourceBundle.getString("metatag.title"));
        add(lblTitleT, "cell 0 5,alignx trailing");

        tfTitle = new JTextField();
        tfTitle.setColumns(30);
        add(tfTitle, "cell 1 5 2 1, growx");
      }
    }

    tfCodec.setText(audioStream.getCodec());
    spChannels.setValue(audioStream.getAudioChannels());
    spBitrate.setValue(audioStream.getBitrate());
    spBitdepth.setValue(audioStream.getBitDepth());
    tfTitle.setText(audioStream.getTitle());

    Optional<LanguageContainer> foundByValue = languages.stream().filter(v -> v.value.equalsIgnoreCase(audioStream.getLanguage())).findFirst();
    if (foundByValue.isPresent()) {
      cbLanguage.setSelectedItem(foundByValue.get());
    }
    else {
      cbLanguage.setSelectedItem(audioStream.getLanguage()); // but not added to langu list ;)
    }

    // set focus to the first combobox
    SwingUtilities.invokeLater(tfCodec::requestFocus);
  }

  @Override
  protected void onClose() {
    audioStream.setCodec(tfCodec.getText());
    audioStream.setAudioChannels((int) spChannels.getValue());
    audioStream.setBitrate((int) spBitrate.getValue());
    audioStream.setBitDepth((int) spBitdepth.getValue());

    Object obj = cbLanguage.getSelectedItem();
    if (obj instanceof LanguageContainer localeContainer) {
      audioStream.setLanguage(localeContainer.value.strip());
    }
    else if (obj instanceof String language) {
      audioStream.setLanguage(language.strip());
    }

    audioStream.setTitle(tfTitle.getText());

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
