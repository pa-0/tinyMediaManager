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
package org.tinymediamanager.core.entities;

import java.util.Objects;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.tinymediamanager.core.TmmToStringStyle;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The class MediaFileAudioStream
 * 
 * @author Manuel Laggner
 */
public class MediaFileAudioStream extends MediaStreamInfo {
  @JsonProperty
  private int audioChannels = 0;
  @JsonProperty
  private int bitrate       = 0;
  @JsonProperty
  private int bitDepth      = 0;

  public MediaFileAudioStream() {
    // empty constructor for jackson
  }

  public int getAudioChannels() {
    return audioChannels;
  }

  public void setAudioChannels(int audiochannels) {
    this.audioChannels = audiochannels;
  }

  public int getBitrate() {
    return bitrate;
  }

  public String getBitrateInKbps() {
    return bitrate > 0 ? bitrate + " kbps" : "";
  }

  public void setBitrate(int bitrate) {
    this.bitrate = bitrate;
  }

  public int getBitDepth() {
    return bitDepth;
  }

  public String getBitDepthAsString() {
    return bitDepth > 0 ? bitDepth + " bit" : "";
  }

  public void setBitDepth(int bitDepth) {
    this.bitDepth = bitDepth;
  }

  /**
   * <p>
   * Uses <code>ReflectionToStringBuilder</code> to generate a <code>toString</code> for the specified object.
   * </p>
   *
   * @return the String result
   * @see ReflectionToStringBuilder#toString(Object)
   */
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, TmmToStringStyle.TMM_STYLE, false, MediaFileAudioStream.class);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    MediaFileAudioStream that = (MediaFileAudioStream) o;

    return audioChannels == that.audioChannels && bitrate == that.bitrate;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), audioChannels, bitrate);
  }

  /**
   * used to migrate values to their new location
   *
   * @param property
   *          the property/value name
   * @param value
   *          the value itself
   */
  @JsonAnySetter
  public void setUnknownFields(String property, Object value) {
    if (value == null) {
      return;
    }

    // integer values
    if (property.equals("audioTitle")) {
      setTitle(value.toString());
    }
    else if (property.equals("defaultStream")) {
      setDefaultStream(Boolean.parseBoolean(value.toString()));
    }
  }
}
