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

package org.tinymediamanager.core.jmte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Map;

import com.floreysoft.jmte.NamedRenderer;
import com.floreysoft.jmte.RenderFormatInfo;

/**
 * this renderer is used to render the framerate in the given form
 * 
 * @author Manuel Laggner
 */
public class NamedFramerateRenderer implements NamedRenderer {
  @Override
  public String render(Object o, String s, Locale locale, Map<String, Object> map) {
    if (!(o instanceof Number)) {
      return "";
    }

    double frameRate = (double) o;

    if ("round".equalsIgnoreCase(s)) {
      frameRate = round(frameRate, 0);
    }

    // default: with decimals
    DecimalFormat format = new DecimalFormat("0.###");

    boolean isInt = Math.floor(frameRate) == frameRate;
    if (isInt) {
      // strip decimals
      format = new DecimalFormat("0");
    }

    try {
      return format.format(o);
    }
    catch (Exception e) {
      return "";
    }
  }

  private double round(double value, int places) {
    BigDecimal bd = new BigDecimal(Double.toString(value));
    return bd.setScale(places, RoundingMode.HALF_UP).doubleValue();
  }

  @Override
  public String getName() {
    return "framerate";
  }

  @Override
  public RenderFormatInfo getFormatInfo() {
    return null;
  }

  @Override
  public Class<?>[] getSupportedClasses() {
    return new Class[] { Integer.class, Float.class, Double.class };
  }
}
