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

package org.tinymediamanager.ui.converter;

import javax.swing.ImageIcon;

import org.jdesktop.beansbinding.Converter;
import org.tinymediamanager.ui.IconManager;

/**
 * The Class LockedConverter is used to convert a boolean in the locked icon
 * 
 * @author Manuel Laggner
 */
public class LockedConverter extends Converter<Boolean, ImageIcon> {

  @Override
  public ImageIcon convertForward(Boolean arg0) {
    if (Boolean.TRUE.equals(arg0)) {
      return IconManager.LOCK_BLUE;
    }
    return null;
  }

  @Override
  public Boolean convertReverse(ImageIcon arg0) {
    return null;
  }
}
