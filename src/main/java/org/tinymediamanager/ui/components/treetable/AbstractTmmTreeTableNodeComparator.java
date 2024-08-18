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

package org.tinymediamanager.ui.components.treetable;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;

public abstract class AbstractTmmTreeTableNodeComparator implements Comparator<TmmTreeNode>, ITmmTreeTableSortingStrategy {
  private static final Pattern FROM_STRING_PATTERN = Pattern.compile("^\\s*column\\s+(\\d+)(\\s+comparator\\s+(\\d+))?(\\s+(reversed))?\\s*$",
      Pattern.CASE_INSENSITIVE);

  protected SortDirection      sortDirection;
  protected int                sortColumn;

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();

    // write the column index
    result.append("column ");
    result.append(sortColumn);

    // write reversed
    if (sortDirection == SortDirection.DESCENDING) {
      result.append(" reversed");
    }

    return result.toString();
  }

  @Override
  public void fromString(String stringEncoded) {
    // skip empty strings
    if (StringUtils.isBlank(stringEncoded)) {
      return;
    }

    Matcher matcher = FROM_STRING_PATTERN.matcher(stringEncoded);

    if (!matcher.find()) {
      throw new IllegalArgumentException("Failed to parse column spec, \"" + stringEncoded + "\"");
    }

    int columnIndex = Integer.parseInt(matcher.group(1));
    boolean reversedComparator = matcher.group(5) != null;

    if (columnIndex >= 0) {
      sortColumn = columnIndex;
    }
    if (reversedComparator) {
      sortDirection = SortDirection.DESCENDING;
    }
    else {
      sortDirection = SortDirection.ASCENDING;
    }
  }
}
