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
package org.tinymediamanager.core;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

public class ArdSettings {

  public enum Mode {
    FAST,
    DEFAULT,
    ACCURATE
  }

  public static Map<Mode, SampleSetting> defaultSampleSettings() {
    Map<Mode, SampleSetting> settings = new EnumMap<>(Mode.class);

    SampleSetting settingFast = new SampleSetting();
    settingFast.duration = 1;
    settingFast.minNumber = 4;
    settingFast.maxGap = 1800;

    SampleSetting settingDefault = new SampleSetting();
    settingDefault.duration = 2;
    settingDefault.minNumber = 6;
    settingDefault.maxGap = 900;

    SampleSetting settingAccurate = new SampleSetting();
    settingAccurate.duration = 2;
    settingAccurate.minNumber = 30;
    settingAccurate.maxGap = 900;

    settings.put(Mode.FAST, settingFast);
    settings.put(Mode.DEFAULT, settingDefault);
    settings.put(Mode.ACCURATE, settingAccurate);

    return settings;
  }

  public static class SampleSetting implements Serializable {

    private int duration  = 2;
    private int minNumber = 6;
    private int maxGap    = 900;

    public int getDuration() {
      return duration;
    }

    public void setDuration(int ardSampleDuration) {
      this.duration = ardSampleDuration;
    }

    public int getMinNumber() {
      return minNumber;
    }

    public void setMinNumber(int ardSampleMinNumber) {
      this.minNumber = ardSampleMinNumber;
    }

    public int getMaxGap() {
      return maxGap;
    }

    public void setMaxGap(int ardSampleMaxGap) {
      this.maxGap = ardSampleMaxGap;
    }
  }

}
