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

package org.tinymediamanager.ui;

import java.awt.Canvas;
import java.awt.FontMetrics;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.ImageUtils;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.ui.images.TmmSvgIcon;

/**
 * The class {@link ScraperInTable} is used to display scrapers in a table
 */
public class ScraperInTable extends AbstractModelObject {

  protected static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

  protected MediaScraper                scraper;
  protected ImageIcon                   scraperLogo;
  protected boolean                     active;
  protected boolean                     enabled;

  public ScraperInTable(MediaScraper scraper) {
    this.scraper = scraper;
    if (scraper.getMediaProvider() == null || scraper.getMediaProvider().getProviderInfo() == null
        || scraper.getMediaProvider().getProviderInfo().getProviderLogo() == null) {
      scraperLogo = new ImageIcon();
    }
    else {
      scraperLogo = getIcon(scraper.getMediaProvider().getProviderInfo().getProviderLogo());
    }
    enabled = scraper.isEnabled();
  }

  protected ImageIcon getIcon(URL url) {
    try {
      URI uri = url.toURI();

      if (url.getFile().endsWith("svg")) {
        TmmSvgIcon svgIcon = new TmmSvgIcon(uri);
        svgIcon.setPreferredHeight(calculatePreferredHeight());
        return svgIcon;
      }
      else {
        return ImageUtils.createMultiResolutionImage(IconManager.loadImageFromURL(url), calculatePreferredHeight());
      }
    }
    catch (Exception e) {
      return null;
    }
  }

  private int calculatePreferredHeight() {
    Canvas c = new Canvas();
    FontMetrics fm = c.getFontMetrics(new JPanel().getFont());
    return (int) (fm.getHeight() * 1.8f);
  }

  public String getScraperId() {
    return scraper.getId();
  }

  public String getScraperName() {
    String scraperName;
    if (StringUtils.isNotBlank(scraper.getVersion())) {
      scraperName = scraper.getName() + " - " + scraper.getVersion();
    }
    else {
      scraperName = scraper.getName();
    }

    if (!enabled) {
      scraperName = "*PRO* " + scraperName;
    }

    return scraperName;
  }

  public String getScraperDescription() {
    // first try to get the localized version
    String description = null;
    try {
      description = BUNDLE.getString("scraper." + scraper.getId() + ".hint");
    }
    catch (Exception ignored) {
    }

    if (StringUtils.isBlank(description)) {
      // try to get a scraper text
      description = scraper.getDescription();
    }

    return description;
  }

  public Icon getScraperLogo() {
    return scraperLogo;
  }

  public Boolean getActive() {
    return active;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setActive(Boolean newValue) {
    Boolean oldValue = this.active;
    this.active = newValue;
    firePropertyChange("active", oldValue, newValue);
  }

  public IMediaProvider getMediaProvider() {
    return scraper.getMediaProvider();
  }
}
