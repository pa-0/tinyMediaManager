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

import java.net.URI;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.UIManager;

import org.jdesktop.beansbinding.Converter;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.ui.images.TmmSvgIcon;

import com.kitfox.svg.app.beans.SVGIcon;

/**
 * The Class CertificationImageConverter.
 * 
 * @author Manuel Laggner
 */
public class CertificationImageConverter extends Converter<MediaCertification, Icon> {

  @Override
  public Icon convertForward(MediaCertification cert) {
    // we have no certification here
    if (cert == null || cert == MediaCertification.UNKNOWN) {
      return null;
    }
    // try to find an image for this genre
    try {
      URI uri = getClass().getResource("/org/tinymediamanager/ui/images/certification/" + cert.name().toLowerCase(Locale.ROOT) + ".svg").toURI();
      TmmSvgIcon icon = new TmmSvgIcon(uri);
      icon.setPreferredHeight(32);
      icon.setAutosize(SVGIcon.AUTOSIZE_STRETCH);

      // only color the monochrome ones
      if (cert.getCountry() == CountryCode.US) {
        icon.setColor(UIManager.getColor("Label.foreground"), "#000000");
      }

      return icon;
    }
    catch (Exception e) {
      return null;
    }
  }

  @Override
  public MediaCertification convertReverse(Icon arg0) {
    return null;
  }
}
