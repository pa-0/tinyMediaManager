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
 * The interface {@link IJmteDefaultValue} is used to get a default value of the used class for the JMTE engine
 */
public interface IJmteDefaultValue {
  /**
   * The default object representation style for JMTE
   */
  ToStringStyle JMTE_STYLE = new Style();

  /**
   * A default {@link String} representation of the object
   * 
   * @return the {@link String} value representing this value
   */
  String toJmteDefaultValue();

  class Style extends ToStringStyle {
    public Style() {
      this.setUseShortClassName(true);
      this.setUseIdentityHashCode(false);
      this.setContentStart("[");
      this.setFieldSeparator(System.lineSeparator() + "  ");
      this.setFieldSeparatorAtStart(true);
      this.setContentEnd(System.lineSeparator() + "]");
    }
  }
}
