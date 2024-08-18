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
package org.tinymediamanager.scraper;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.tinymediamanager.scraper.config.MediaProviderConfig;

/**
 * The class ProviderInfo is used to store provider related information for further usage.
 * 
 * @author Manuel Laggner
 * @since 1.0
 */
public class MediaProviderInfo {
  public static final String        API_KEY        = "apiKey";
  private static final URL          EMPTY_LOGO     = MediaProviderInfo.class.getResource("emtpyLogo.png");

  private final String              id;
  private final String              subId;
  private final String              name;
  private final String              description;
  private final URL                 providerLogo;
  private final MediaProviderConfig config;
  private int                       priority       = 0;
  private String                    version        = "";
  private ResourceBundle            resourceBundle = null;

  /**
   * Instantiates a new provider info for a private scraper.
   *
   * @param id
   *          the id of the provider
   * @param subId
   *          the subId of the provider (in most cases a dedicated part of the scraper)
   * @param name
   *          the name of the provider
   * @param description
   *          a description of the provider
   */
  public MediaProviderInfo(String id, String subId, String name, String description) {
    this(id, subId, name, description, null);
  }

  /**
   * Instantiates a new provider info.
   *
   * @param id
   *          the id of the provider
   * @param subId
   *          the subId of the provider (in most cases a dedicated part of the scraper)
   * @param name
   *          the name of the provider
   * @param description
   *          a description of the provider
   * @param providerLogo
   *          the URL to the (embedded) provider logo
   */
  public MediaProviderInfo(String id, String subId, String name, String description, URL providerLogo) {
    this(id, subId, name, description, providerLogo, 0);
  }

  /**
   * Instantiates a new provider info.
   *
   * @param id
   *          the id of the provider
   * @param subId
   *          the subId of the provider (in most cases a dedicated part of the scraper)
   * @param name
   *          the name of the provider
   * @param description
   *          a description of the provider
   * @param providerLogo
   *          the URL to the (embedded) provider logo
   * @param priority
   *          usually 0, but the higher value, the more important it is (for fallback scraper sorting)
   */
  public MediaProviderInfo(String id, String subId, String name, String description, URL providerLogo, int priority) {
    this.id = id;
    this.subId = subId;
    this.name = name;
    this.description = description;
    this.providerLogo = providerLogo;
    this.priority = priority;
    this.config = new MediaProviderConfig(this);
  }

  public String getId() {
    return id;
  }

  public String getSubId() {
    return subId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public URL getProviderLogo() {
    if (providerLogo != null) {
      return providerLogo;
    }
    else {
      return EMPTY_LOGO;
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  public ResourceBundle getResourceBundle() {
    return resourceBundle;
  }

  public void setResourceBundle(ResourceBundle resourceBundle) {
    this.resourceBundle = resourceBundle;
  }

  /**
   * usually 0, but the higher value, the more important it is (for fallback scraper sorting)
   * 
   * @return
   */
  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  /**
   * get the configuration for this scraper
   * 
   * @return the configuration for this scraper
   */
  public MediaProviderConfig getConfig() {
    return config;
  }

  /**
   * Handy method to get the user entered API key from the {@link org.tinymediamanager.scraper.config.MediaProviderConfigObject}
   *
   * @return the API key or an empty string
   */
  public String getUserApiKey() {
    String apiKey = config.getValue(API_KEY);
    if (StringUtils.isBlank(apiKey)) {
      return "";
    }

    return apiKey.strip();
  }
}
