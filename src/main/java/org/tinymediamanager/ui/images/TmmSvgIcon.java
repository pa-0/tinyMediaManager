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

import java.awt.Color;
import java.awt.Dimension;
import java.net.URI;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.app.beans.SVGIcon;

/**
 * basically a clone of {@link SVGIcon}, but with support of loading the same SVG several times with different settings (color, sizes, ...)
 * 
 * @author Manuel Laggner
 */
public class TmmSvgIcon extends SVGIcon {

  /**
   * Convenience-Constructor to set the {@link URI} afterward
   */
  protected TmmSvgIcon() {
    // create new SVG universe to suppress caching of loaded SVGs
    setSvgUniverse(new SVGUniverse());
    setAutosize(AUTOSIZE_BESTFIT);
    setAntiAlias(true);
    setInterpolation(INTERP_BICUBIC);
    setClipToViewbox(false);
  }

  public TmmSvgIcon(URI uri) {
    this();
    setSvgURI(uri);
  }

  public void setPreferredHeight(int height) {
    SVGDiagram diagram = getSvgUniverse().getDiagram(getSvgURI());
    if (diagram != null) {
      // preferredSize = new Dimension((int)diagram.getWidth(), (int)diagram.getHeight());
      setPreferredSize(new Dimension((int) (diagram.getWidth() * height / diagram.getHeight()), height));
    }
  }

  /**
   * set the color of the whole icon (set the fill on all nodes).
   * 
   * @param color
   *          the {@link Color} to set
   */
  public void setColor(Color color) {
    if (color != null) {
      try {
        SVGDiagram diagram = getSvgUniverse().getDiagram(getSvgURI());
        setFill(getHexString(color), diagram.getRoot());
      }
      catch (Exception ex) {
        // ignored
      }
    }
  }

  /**
   * set the color of the whole icon (set the fill on all nodes, where the original color matches).
   * 
   * @param color
   *          the {@link Color} to set
   * @param originalToReplace
   *          the original color in the SVG to replace
   * 
   */
  public void setColor(Color color, String originalToReplace) {
    if (color != null) {
      try {
        SVGDiagram diagram = getSvgUniverse().getDiagram(getSvgURI());
        setFill(getHexString(color), originalToReplace, diagram.getRoot());
      }
      catch (Exception ex) {
        // ignored
      }
    }
  }
}
