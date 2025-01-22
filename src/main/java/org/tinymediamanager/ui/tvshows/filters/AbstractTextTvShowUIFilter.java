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
package org.tinymediamanager.ui.tvshows.filters;

import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JTextField;

import org.tinymediamanager.scraper.util.StrgUtils;

public abstract class AbstractTextTvShowUIFilter extends AbstractTvShowUIFilter {
  protected JTextField textField;
  protected String     normalizedFilterText;
  protected Pattern    filterPattern;

  @Override
  protected JComponent createFilterComponent() {
    textField = new JTextField();
    return textField;
  }

  @Override
  public String getFilterValueAsString() {
    return textField.getText();
  }

  @Override
  public void setFilterValue(Object value) {
    if (value instanceof String) {
      textField.setText((String) value);
    }
  }

  @Override
  public void clearFilter() {
    textField.setText("");
  }

  /**
   * delegate the filter changed event to the tree
   */
  @Override
  protected void filterChanged() {
    normalizedFilterText = StrgUtils.normalizeString(textField.getText());
    try {
      filterPattern = Pattern.compile(normalizedFilterText, Pattern.CASE_INSENSITIVE);
    }
    catch (Exception e) {
      // just catch illegal patterns
    }

    super.filterChanged();
  }
}
