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
package org.tinymediamanager.ui.components.combobox;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ImageUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.images.TmmSvgIcon;

/**
 * the class MediaScraperCheckComboBox is used to display a CheckCombBox with media scraper logos
 * 
 * @author Manuel Laggner
 */
public class MediaScraperCheckComboBox extends TmmCheckComboBox<MediaScraper> {
  private static final Logger LOGGER    = LoggerFactory.getLogger(MediaScraperCheckComboBox.class);

  private Map<URI, ImageIcon> imageCache;

  private int                 listWidth = 0;

  public MediaScraperCheckComboBox(final List<MediaScraper> scrapers) {
    super(scrapers);
    if (getRenderer() instanceof MediaScraperCheckBoxRenderer) {
      ((MediaScraperCheckBoxRenderer) getRenderer()).init(checkComboBoxItems);
    }
  }

  @Override
  protected void setRenderer() {
    setRenderer(new MediaScraperCheckBoxRenderer());
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (imageCache != null) {
      imageCache.clear();
    }
  }

  @Override
  public List<MediaScraper> getSelectedItems() {
    List<MediaScraper> selectedItems = new ArrayList<>();

    // filter our inactive items
    for (MediaScraper scraper : super.getSelectedItems()) {
      if (scraper.getMediaProvider().isActive()) {
        selectedItems.add(scraper);
      }
    }

    return selectedItems;
  }

  @Override
  public Dimension getSize() {
    Dimension dim = super.getSize();
    dim.width = Math.max(dim.width, getPreferredPopupSize().width);
    return dim;
  }

  /**
   * get the preferred popup size (width of the contents in the popup)
   *
   * @return the preferred popup size
   */
  private Dimension getPreferredPopupSize() {
    Dimension dimension = getPreferredSize();
    if (listWidth > 0) {
      dimension.width = listWidth;
    }
    return dimension;
  }

  private class MediaScraperCheckBoxRenderer extends CheckBoxRenderer {
    private final JPanel    panel        = new JPanel();
    private final JCheckBox checkBox     = new JCheckBox();
    private final JLabel    label        = new JLabel();

    private int             maxIconWidth = 0;

    private MediaScraperCheckBoxRenderer() {
      super();
      panel.setLayout(new FlowLayout(FlowLayout.LEFT));
      panel.add(checkBox);
      panel.add(label);
      panel.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));

      label.setOpaque(false);
      checkBox.setOpaque(false);
      imageCache = new HashMap<>();
    }

    private void init(final List<TmmCheckComboBoxItem<MediaScraper>> items) {
      // calculate the max width of the logo
      for (TmmCheckComboBoxItem<MediaScraper> item : items) {
        if (item.getUserObject() != null) {
          ImageIcon logo = getIcon(item.getUserObject().getLogoURL());
          if (logo != null) {
            maxIconWidth = Math.max(maxIconWidth, logo.getIconWidth());
          }
        }
      }
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends TmmCheckComboBoxItem<MediaScraper>> list, TmmCheckComboBoxItem<MediaScraper> value,
        int index, boolean isSelected, boolean cellHasFocus) {

      if (value != null) {
        TmmCheckComboBoxItem<MediaScraper> cb = value;
        if (cb == nullItem) {
          list.setToolTipText(null);
          return separator;
        }

        if (isSelected) {
          panel.setBackground(UIManager.getColor("ComboBox.selectionBackground"));
          panel.setForeground(UIManager.getColor("ComboBox.selectionForeground"));
        }
        else {
          panel.setBackground(UIManager.getColor("ComboBox.background"));
          panel.setForeground(UIManager.getColor("ComboBox.foreground"));
        }

        label.setText(cb.getText());
        checkBox.setSelected(cb.isSelected());

        MediaScraper scraper = cb.getUserObject();
        if (scraper != null) {
          int currentWidth = 0;
          ImageIcon logo = getIcon(scraper.getLogoURL());
          if (logo != null) {
            currentWidth = logo.getIconWidth();
          }

          label.setIcon(logo);
          label.setIconTextGap(maxIconWidth + 4 - currentWidth); // 4 = default iconTextGap

          if (!scraper.isActive()) {
            checkBox.setFocusable(false);
            checkBox.setEnabled(false);
            checkBox.setSelected(false);
            label.setFocusable(false);
            label.setEnabled(false);
            if (!scraper.isEnabled()) {
              label.setText("*PRO* " + label.getText());
            }
          }
          else {
            checkBox.setFocusable(true);
            checkBox.setEnabled(true);
            label.setFocusable(true);
            label.setEnabled(true);
          }
        }
        else {
          label.setIcon(null);
          label.setIconTextGap(4); // 4 = default iconTextGap

          checkBox.setFocusable(true);
          checkBox.setEnabled(true);
          label.setFocusable(true);
          label.setEnabled(true);
        }

        Dimension preferredSize = panel.getPreferredSize();
        if (listWidth < preferredSize.width) {
          listWidth = preferredSize.width;
        }

        if (index > -1) {
          return panel;
        }
      }

      String str;
      List<MediaScraper> objs = getSelectedItems();
      List<String> strs = new ArrayList<>();
      if (objs.isEmpty()) {
        str = TmmResourceBundle.getString("ComboBox.select.mediascraper");
      }
      else {
        for (Object obj : objs) {
          strs.add(obj.toString());
        }
        str = strs.toString();
      }

      return defaultRenderer.getListCellRendererComponent(list, str, index, isSelected, cellHasFocus);
    }

    private ImageIcon getIcon(URL url) {
      if (url == null) {
        return null;
      }

      try {
        URI uri = url.toURI();
        ImageIcon logo = imageCache.get(uri);
        if (logo == null) {
          if (url.getFile().endsWith("svg")) {
            TmmSvgIcon svgIcon = new TmmSvgIcon(uri);
            svgIcon.setPreferredHeight(calculatePreferredHeight());
            logo = svgIcon;
          }
          else {
            logo = ImageUtils.createMultiResolutionImage(IconManager.loadImageFromURL(url), calculatePreferredHeight());
          }
          imageCache.put(uri, logo);
        }
        return logo;
      }
      catch (Exception e) {
        LOGGER.debug("could not load scraper icon: {}", e.getMessage());
      }
      return null;
    }

    private int calculatePreferredHeight() {
      FontMetrics fm = getFontMetrics(getFont());
      return (int) (fm.getHeight() * 2f);
    }
  }
}
