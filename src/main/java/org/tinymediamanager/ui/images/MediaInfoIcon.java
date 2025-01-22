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

import static org.tinymediamanager.ui.images.SvgIconHelper.getHexString;
import static org.tinymediamanager.ui.images.SvgIconHelper.setFill;

import java.awt.Dimension;
import java.net.URI;

import javax.swing.UIManager;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.Text;
import com.kitfox.svg.animation.AnimationElement;

public class MediaInfoIcon extends TmmSvgIcon {
  private static final String BASE_PATH = "/org/tinymediamanager/ui/images/mediainfo/";

  public MediaInfoIcon(String relativePath) throws Exception {
    super();
    setSvgURI(getFileUri(relativePath));
    setAutosize(AUTOSIZE_VERT);
    setPreferredSize(new Dimension(28, 28));
    setColor(UIManager.getColor("Label.foreground"), "#000000");
  }

  protected URI getFileUri(String relativePath) throws Exception {
    return this.getClass().getResource(BASE_PATH + relativePath).toURI();
  }

  /**
   * set the text-content for the first Text node in this SVG
   * 
   * @param text
   *          the text to set
   * @param x
   *          the x coordinate
   */
  protected void setText(String text, int x) {
    // get the text node from the root
    try {
      // clear cached SVG, because we want to modify the source
      getSvgUniverse().clear();

      SVGDiagram diagram = getSvgUniverse().getDiagram(getSvgURI());
      Text textElement = (Text) diagram.getElement("text");
      if (textElement != null) {

        textElement.addAttribute("x", AnimationElement.AT_XML, String.valueOf(x));
        textElement.appendText(text);
        setFill(getHexString(UIManager.getColor("Label.foreground")), diagram.getRoot());
        textElement.rebuild();
      }
    }
    catch (Exception ex) {
      // ignored
    }
  }
}
