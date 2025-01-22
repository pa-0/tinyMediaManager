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
package org.tinymediamanager.core.movie;

import org.tinymediamanager.core.TmmResourceBundle;

/**
 * this enum is used for the user to choose which fields should be used in the movie matcher
 * 
 * @author Wolfgang Janes
 */
public enum MovieTextMatcherList {

  TITLE("metatag.title"),
  TITLE_SORTABLE("metatag.title.sortable"),
  ORIGINAL_TITLE("metatag.originaltitle"),
  ORIGINAL_TITLE_SORTABLE("metatag.originaltitle.sortable"),
  SORTED_TITLE("metatag.sorttitle"),
  NOTE("metatag.note");

  private final String description;

  MovieTextMatcherList(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return TmmResourceBundle.getString(description);
  }
}
