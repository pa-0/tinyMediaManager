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
package org.tinymediamanager.ui.panels;

import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.ui.components.MediaIdTable;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link IdEditorPanel} is used to edit IDs of an entity
 * 
 * @author Manuel Laggner
 */
public class IdEditorPanel extends AbstractModalInputPanel {

  private final MediaIdTable.MediaId idToEdit;
  private final JComboBox<String>    cbProviderId;
  private final JTextField           tfId;

  public IdEditorPanel(MediaIdTable.MediaId mediaId, ScraperType type) {
    super();

    idToEdit = mediaId;

    Set<String> providerIds = new TreeSet<>();
    if (type == null) {
      providerIds.addAll(getProviderIds(ScraperType.MOVIE));
      providerIds.addAll(getProviderIds(ScraperType.TV_SHOW));
    }
    else {
      providerIds.addAll(getProviderIds(type));
    }

    {
      setLayout(new MigLayout("", "[][100lp:n,grow]", "[][]"));
      {
        JLabel lblProviderIdT = new JLabel(TmmResourceBundle.getString("metatag.id.source"));
        add(lblProviderIdT, "cell 0 0,alignx trailing");

        cbProviderId = new AutocompleteComboBox(providerIds);
        add(cbProviderId, "cell 1 0,growx");
      }
      {
        JLabel lblIdT = new JLabel(TmmResourceBundle.getString("metatag.id"));
        add(lblIdT, "cell 0 1,alignx trailing");

        tfId = new JTextField();
        add(tfId, "cell 1 1,growx");
        tfId.setColumns(10);
      }
    }

    cbProviderId.setSelectedItem(idToEdit.key);
    tfId.setText(idToEdit.value);

    // set focus to the first combobox
    SwingUtilities.invokeLater(cbProviderId::requestFocus);
  }

  @Override
  protected void onClose() {
    if (StringUtils.isAnyBlank(tfId.getText(), (String) cbProviderId.getSelectedItem())) {
      JOptionPane.showMessageDialog(IdEditorPanel.this, TmmResourceBundle.getString("id.empty"));
      return;
    }

    idToEdit.key = (String) cbProviderId.getSelectedItem();
    idToEdit.value = tfId.getText();
    setVisible(false);
  }

  @NotNull
  private Set<String> getProviderIds(@NotNull ScraperType type) {
    Set<String> providerIds = new TreeSet<>();
    for (MediaScraper scraper : MediaScraper.getMediaScrapers(type)) {
      // all but Kodi, universal and TVDB legacy
      if (scraper.getId().startsWith("metadata") || "tvdbv3".equals(scraper.getId()) || scraper.getId().startsWith("universal_")) {
        continue;
      }
      providerIds.add(scraper.getId());
    }
    return providerIds;
  }
}
