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
package org.tinymediamanager.ui.tvshows.dialogs;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.TmmCheckComboBox;
import org.tinymediamanager.ui.dialogs.TmmDialog;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TvShowFetchRatingsDialog} is used to choose which sources should be used for rating fetching.
 * 
 * @author Manuel Laggner
 */
public class TvShowFetchRatingsDialog extends TmmDialog {
  private static final ObjectMapper                           OBJECT_MAPPER = new ObjectMapper();
  private static final String                                 PROPERTY_NAME = "ratingSources";
  private final TmmCheckComboBox<RatingProvider.RatingSource> cbSources;

  public TvShowFetchRatingsDialog() {
    super(TmmResourceBundle.getString("tvshow.fetchratings"), "tvShowFetchRatings");

    JPanel panelContent = new JPanel();
    getContentPane().add(panelContent, BorderLayout.CENTER);
    panelContent.setLayout(new MigLayout("", "[][250lp]", "[][50lp]"));

    JLabel lblRatingSource = new TmmLabel(TmmResourceBundle.getString("metatag.rating.source"));
    panelContent.add(lblRatingSource, "cell 0 0,alignx right");

    cbSources = new TmmCheckComboBox<>(RatingProvider.RatingSource.getRatingSourcesForTvShows());
    panelContent.add(cbSources, "cell 1 0,growx");

    {
      JButton cancelButton = new JButton(TmmResourceBundle.getString("Button.cancel"));
      cancelButton.setIcon(IconManager.CANCEL_INV);
      cancelButton.addActionListener(e -> {
        cbSources.clearSelection();
        setVisible(false);
      });
      addButton(cancelButton);

      JButton btnOk = new JButton(TmmResourceBundle.getString("Button.ok"));
      btnOk.setIcon(IconManager.APPLY_INV);
      btnOk.addActionListener(e -> {
        storeDefaults();
        setVisible(false);
      });
      addButton(btnOk);
    }
  }

  public List<RatingProvider.RatingSource> getSelectedRatingSources() {
    return cbSources.getSelectedItems();
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      // init defaults
      loadDefaults();
    }

    // super
    super.setVisible(visible);
  }

  private void loadDefaults() {
    List<RatingProvider.RatingSource> selectedItems = new ArrayList<>();

    try {
      String storedValues = TmmProperties.getInstance().getProperty(getName() + "." + PROPERTY_NAME);
      if (StringUtils.isNotBlank(storedValues)) {
        List<String> values = OBJECT_MAPPER.readValue(storedValues, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));

        for (String valueAsString : values) {
          selectedItems.add(RatingProvider.RatingSource.valueOf(valueAsString));
        }
      }
    }
    catch (Exception ignored) {
      // just do not crash
    }

    cbSources.setSelectedItems(selectedItems);
  }

  private void storeDefaults() {
    try {
      List<String> values = new ArrayList<>();

      for (RatingProvider.RatingSource source : getSelectedRatingSources()) {
        values.add(source.name());
      }

      TmmProperties.getInstance().putProperty(getName() + "." + PROPERTY_NAME, OBJECT_MAPPER.writeValueAsString(values));
    }
    catch (Exception e) {
      // just catch
    }
  }
}
