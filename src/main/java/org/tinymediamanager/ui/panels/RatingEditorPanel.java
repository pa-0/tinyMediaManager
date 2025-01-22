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

import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.ui.components.MediaRatingTable;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link RatingEditorPanel} is used to edit {@link org.tinymediamanager.core.entities.MediaRating}s
 * 
 * @author Manuel Laggner
 */
public class RatingEditorPanel extends AbstractModalInputPanel {
  private final MediaRatingTable.Rating ratingToEdit;

  private final JComboBox<String>       cbProviderId;
  private final JSpinner                spRating;
  private final JSpinner                spMaxValue;
  private final JSpinner                spVotes;

  public RatingEditorPanel(MediaRatingTable.Rating rating) {
    super();

    this.ratingToEdit = rating;

    {
      setLayout(new MigLayout("", "[][50lp][20lp:n][][50lp]", "[][][]"));
      {
        JLabel lblProviderIdT = new JLabel(TmmResourceBundle.getString("metatag.rating.source"));
        add(lblProviderIdT, "cell 0 0,alignx trailing");

        cbProviderId = new AutocompleteComboBox(Arrays.stream(RatingProvider.RatingSource.values()).map(RatingProvider::getRatingSourceId).toList());
        add(cbProviderId, "cell 1 0 4 1,growx");
      }
      {
        JLabel lblRatingT = new JLabel(TmmResourceBundle.getString("metatag.rating"));
        add(lblRatingT, "cell 0 1,alignx trailing");

        spRating = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1000.0, 0.1));
        add(spRating, "cell 1 1,growx");
      }
      {
        JLabel lblMaxValue = new JLabel(TmmResourceBundle.getString("metatag.rating.maxvalue"));
        add(lblMaxValue, "cell 3 1");

        spMaxValue = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        add(spMaxValue, "cell 4 1,growx");
      }
      {
        JLabel lblVotes = new JLabel(TmmResourceBundle.getString("metatag.rating.votes"));
        add(lblVotes, "cell 0 2");

        spVotes = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        add(spVotes, "cell 1 2,growx");
      }
    }

    cbProviderId.setSelectedItem(ratingToEdit.key);
    spRating.setValue(ratingToEdit.value);
    spMaxValue.setValue(ratingToEdit.maxValue);
    spVotes.setValue(ratingToEdit.votes);

    // if there is not rating set (new one) enter the last remembered one
    if (StringUtils.isBlank(ratingToEdit.key) && ratingToEdit.value == 0) {
      String defaultProvider = TmmProperties.getInstance().getProperty("ratingid");
      if (StringUtils.isNotBlank(defaultProvider)) {
        cbProviderId.setSelectedItem(defaultProvider);
      }
    }

    // set focus to the first combobox
    SwingUtilities.invokeLater(cbProviderId::requestFocus);
  }

  @Override
  protected void onClose() {
    float rating = 0;
    Object obj = spRating.getValue();
    if (obj instanceof Double d) {
      rating = d.floatValue();
    }
    else if (obj instanceof Float f) {
      rating = f;
    }

    int maxValue = (int) spMaxValue.getValue();

    String key;
    obj = cbProviderId.getSelectedItem();
    if (obj == null || StringUtils.isBlank(obj.toString())) {
      JOptionPane.showMessageDialog(RatingEditorPanel.this, TmmResourceBundle.getString("id.empty"));
      return;
    }
    else {
      key = obj.toString();
    }

    if (rating > maxValue) {
      JOptionPane.showMessageDialog(RatingEditorPanel.this, TmmResourceBundle.getString("rating.rating.higher.maxvalue"));
      return;
    }

    ratingToEdit.key = key;
    ratingToEdit.value = rating;
    ratingToEdit.maxValue = maxValue;
    ratingToEdit.votes = (int) spVotes.getValue();

    // store the ID for the next usage
    TmmProperties.getInstance().putProperty("ratingid", ratingToEdit.key);

    setVisible(false);
  }
}
