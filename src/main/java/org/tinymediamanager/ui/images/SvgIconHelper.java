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
package org.tinymediamanager.ui.images;

import java.awt.Color;

import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.xml.StyleAttribute;

class SvgIconHelper {
  private SvgIconHelper() {
    throw new IllegalAccessError();
  }

  /**
   * Convert the {@link Color} to a HEX color code
   *
   * @param color
   *          the {@link Color} to convert
   * @return the HEX string
   */
  static String getHexString(Color color) {
    return String.format("#%06x", (0xFFFFFF & color.getRGB()));
  }

  /**
   * set the fill color for all SVG nodes beneath the given node
   * 
   * @param toColor
   *          the HEX color code to set
   * @param node
   *          the SVG node
   * @throws SVGException
   *           any {@link SVGException} thrown while setting the color
   */
  static void setFill(String toColor, SVGElement node) throws SVGException {
    if (node.hasAttribute("fill", AnimationElement.AT_XML)) {
      StyleAttribute abs = node.getPresAbsolute("fill");
      abs.setStringValue(toColor);
    }
    else if (node.hasAttribute("fill", AnimationElement.AT_CSS)) {
      StyleAttribute abs = node.getStyleAbsolute("fill");
      abs.setStringValue(toColor);

    }
    else {
      node.addAttribute("fill", AnimationElement.AT_CSS, toColor);
    }
    for (int i = 0; i < node.getNumChildren(); ++i) {
      setFill(toColor, node.getChild(i));
    }
  }

  /**
   * set the fill color for all SVG nodes beneath the given node (replacing the fromColor fills, or set a new one)
   *
   * @param toColor
   *          the HEX color code to set
   * @param fromColor
   *          the HEX color code from the original color to replace
   * @param node
   *          the SVG node
   * @throws SVGException
   *           any {@link SVGException} thrown while setting the color
   */
  static void setFill(String toColor, String fromColor, SVGElement node) throws SVGException {
    if (node.hasAttribute("fill", AnimationElement.AT_XML)) {
      StyleAttribute abs = node.getPresAbsolute("fill");
      if (fromColor.equals(abs.getStringValue())) {
        abs.setStringValue(toColor);
      }
    }
    else if (node.hasAttribute("fill", AnimationElement.AT_CSS)) {
      StyleAttribute abs = node.getStyleAbsolute("fill");
      if (fromColor.equals(abs.getStringValue())) {
        abs.setStringValue(toColor);
      }
    }
    else {
      node.addAttribute("fill", AnimationElement.AT_CSS, toColor);
    }
    for (int i = 0; i < node.getNumChildren(); ++i) {
      setFill(toColor, fromColor, node.getChild(i));
    }
  }
}
