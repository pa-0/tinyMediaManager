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

package org.tinymediamanager.ui.panels;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.ui.components.ImageLabel;

/**
 * the class {@link InformationPanel} is used to provide some boilerplate code for the information panels
 * 
 * @author Manuel Laggner
 */
public abstract class InformationPanel extends JPanel {

  protected final Map<MediaFileType, List<Component>> artworkComponents = new EnumMap<>(MediaFileType.class);

  protected abstract List<MediaFileType> getShowArtworkFromSettings();

  protected abstract void setColumnLayout(boolean artworkVisible);

  protected List<Component> generateArtworkComponents(MediaFileType mediaFileType) {
    List<Component> components = new ArrayList<>();

    ImageLabel imageLabel = new ImageLabel(false, false, true);

    switch (mediaFileType) {
      case POSTER:
      case SEASON_POSTER:
        imageLabel.setDesiredAspectRatio(2 / 3f);
        break;

      case FANART:
      case THUMB:
      case SEASON_FANART:
      case SEASON_THUMB:
        imageLabel.setDesiredAspectRatio(16 / 9f);
        break;

      case BANNER:
      case SEASON_BANNER:
        imageLabel.setDesiredAspectRatio(25 / 8f);
        break;

      case CLEARLOGO:
        imageLabel.setDesiredAspectRatio(2.58f); // calculated by the default resolutions of fanart.tv (800 x 310)
        break;

      default:
        return Collections.emptyList();

    }
    imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    imageLabel.enableLightbox();
    components.add(imageLabel);

    JLabel lblArtworkSize = new JLabel(TmmResourceBundle.getString("mediafiletype." + mediaFileType.name().toLowerCase(Locale.ROOT)));
    components.add(lblArtworkSize);

    components.add(Box.createVerticalStrut(20));

    artworkComponents.put(mediaFileType, components);

    return components;
  }

  protected void setArtwork(MediaEntity mediaEntity, MediaFileType type) {
    List<Component> components = artworkComponents.get(type);
    if (ListUtils.isEmpty(components)) {
      return;
    }

    boolean visible = getShowArtworkFromSettings().contains(type);

    for (Component component : components) {
      component.setVisible(visible);

      if (component instanceof ImageLabel) {
        ImageLabel imageLabel = (ImageLabel) component;
        imageLabel.clearImage();
        imageLabel.setImagePath(mediaEntity.getArtworkFilename(type));
      }
      else if (component instanceof JLabel) {
        JLabel sizeLabel = (JLabel) component;
        Dimension artworkDimension = mediaEntity.getArtworkDimension(type);

        if (artworkDimension.width > 0 && artworkDimension.height > 0) {
          sizeLabel.setText(TmmResourceBundle.getString("mediafiletype." + type.name().toLowerCase(Locale.ROOT)) + " - " + artworkDimension.width
              + "x" + artworkDimension.height);
        }
        else {
          sizeLabel.setText(TmmResourceBundle.getString("mediafiletype." + type.name().toLowerCase(Locale.ROOT)));
        }
      }
    }

    updateArtwork();
  }

  protected void updateArtwork() {
    // check if artwork is visible or not
    boolean visible = false;

    for (List<Component> components : artworkComponents.values()) {
      for (Component component : components) {
        if (component.isVisible()) {
          // at least one visible -> end
          visible = true;
          break;
        }
      }
    }

    setColumnLayout(visible);
    revalidate();
  }
}
