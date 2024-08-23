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

package org.tinymediamanager.core;

import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * The class {@link TmmToStringStyle} represents a default tinyMediaManager toStringStyle for the
 * {@link org.apache.commons.lang3.builder.ToStringBuilder}
 *
 * @author Manuel Laggner
 */
public class TmmToStringStyle extends ToStringStyle {
  public static final ToStringStyle TMM_STYLE = new TmmToStringStyle();

  public TmmToStringStyle() {
    this.setUseShortClassName(true);
    this.setUseIdentityHashCode(false);
    this.setFieldSeparator(", ");
  }
}
