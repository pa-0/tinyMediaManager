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

package org.tinymediamanager.ui.thirdparty.imageviewer;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * The default popup menu for image viewers. The contents of the menu are unspecified and may change between library versions.
 * 
 * @author Kazó Csaba, Manuel Laggner
 */
public class ImageViewerPopup extends JPopupMenu {
  private final ImageViewer viewer;

  /**
   * Creates a popup menu for use with the specified viewer.
   * 
   * @param imageViewer
   *          the viewer this popup menu belongs to
   */
  public ImageViewerPopup(ImageViewer imageViewer) {
    viewer = imageViewer;

    final JRadioButtonMenuItem zoomOriginalSize = new JRadioButtonMenuItem("Original size", viewer.getResizeStrategy() == ResizeStrategy.NO_RESIZE);
    zoomOriginalSize.addActionListener(e -> viewer.setResizeStrategy(ResizeStrategy.NO_RESIZE));

    final JRadioButtonMenuItem zoomResizeToFit = new JRadioButtonMenuItem("Resize to fit",
        viewer.getResizeStrategy() == ResizeStrategy.RESIZE_TO_FIT);
    zoomResizeToFit.addActionListener(e -> viewer.setResizeStrategy(ResizeStrategy.RESIZE_TO_FIT));

    final CustomZoomEntry[] customZoomEntries = { new CustomZoomEntry("25%", .25), new CustomZoomEntry("50%", .50), new CustomZoomEntry("75%", .75),
        new CustomZoomEntry("100%", 1), new CustomZoomEntry("150%", 1.5), new CustomZoomEntry("200%", 2), new CustomZoomEntry("300%", 3),
        new CustomZoomEntry("500%", 5) };
    final ButtonGroup group = new ButtonGroup();
    group.add(zoomOriginalSize);
    group.add(zoomResizeToFit);

    for (CustomZoomEntry cze : customZoomEntries) {
      group.add(cze.menuItem);
    }

    viewer.addPropertyChangeListener("resizeStrategy", evt -> {
      switch ((ResizeStrategy) evt.getNewValue()) {
        case NO_RESIZE:
          zoomOriginalSize.setSelected(true);
          break;

        case RESIZE_TO_FIT:
          zoomResizeToFit.setSelected(true);
          break;

        case CUSTOM_ZOOM:
          group.clearSelection();
          for (CustomZoomEntry cze : customZoomEntries) {
            if (cze.value == viewer.getZoomFactor()) {
              cze.menuItem.setSelected(true);
              break;
            }
          }
          break;

        default:
          throw new AssertionError("Unknown resize strategy: " + evt.getNewValue());
      }
    });

    viewer.addPropertyChangeListener("zoomFactor", evt -> {
      if (viewer.getResizeStrategy() == ResizeStrategy.CUSTOM_ZOOM) {
        group.clearSelection();
        for (CustomZoomEntry cze : customZoomEntries) {
          if (cze.value == viewer.getZoomFactor()) {
            cze.menuItem.setSelected(true);
            break;
          }
        }
      }
    });

    /* Pixelated zoom toggle */
    final JCheckBoxMenuItem togglePixelatedZoomItem = new JCheckBoxMenuItem("Pixelated zoom");
    togglePixelatedZoomItem.setState(viewer.isPixelatedZoom());
    viewer.addPropertyChangeListener("pixelatedZoom", evt -> togglePixelatedZoomItem.setState(viewer.isPixelatedZoom()));
    togglePixelatedZoomItem.addActionListener(e -> viewer.setPixelatedZoom(!viewer.isPixelatedZoom()));

    add(zoomOriginalSize);
    add(zoomResizeToFit);
    addSeparator();

    for (CustomZoomEntry cze : customZoomEntries) {
      add(cze.menuItem);
    }
    addSeparator();

    add(togglePixelatedZoomItem);
  }

  private class CustomZoomEntry {
    String               label;
    double               value;
    JRadioButtonMenuItem menuItem;

    private CustomZoomEntry(String label, double value) {
      this.label = label;
      this.value = value;
      menuItem = new JRadioButtonMenuItem(label, viewer.getResizeStrategy() == ResizeStrategy.CUSTOM_ZOOM && viewer.getZoomFactor() == value);
      menuItem.addActionListener(e -> {
        viewer.setResizeStrategy(ResizeStrategy.CUSTOM_ZOOM);
        viewer.setZoomFactor(CustomZoomEntry.this.value);
      });
    }
  }
}
