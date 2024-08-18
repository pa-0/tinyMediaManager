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
package org.tinymediamanager.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the class TmmProperties is used to store several UI related settings
 *
 * @author Manuel Laggner
 */
public class TmmProperties {
  private static final Logger  LOGGER          = LoggerFactory.getLogger(TmmProperties.class);
  private static final String  PROPERTIES_FILE = "tmm.prop";
  private static TmmProperties instance;

  private final Properties     properties;
  private boolean              dirty;

  private TmmProperties() {
    properties = new SortedProperties();

    try (InputStream input = Files.newInputStream(Paths.get(Settings.getInstance().getSettingsFolder(), PROPERTIES_FILE))) {
      properties.load(input);
    }
    catch (FileNotFoundException ignored) {
      // simply not here - ignore
    }
    catch (Exception e) {
      LOGGER.warn("unable to read properties file: {}", e.getMessage());
    }

    dirty = false;
  }

  /**
   * the an instance of this class
   *
   * @return an instance of this class
   */
  public static synchronized TmmProperties getInstance() {
    if (instance == null) {
      instance = new TmmProperties();
    }
    return instance;
  }

  /**
   * write the properties file to the disk
   */
  public void writeProperties() {
    if (!dirty) {
      return;
    }

    try (OutputStream output = new FileOutputStream(new File(Settings.getInstance().getSettingsFolder(), PROPERTIES_FILE))) {
      properties.store(output, null);
    }
    catch (IOException e) {
      LOGGER.warn("failed to store properties file: {}", e.getMessage());
    }
  }

  /**
   * put a key/value pair into the properties file
   *
   * @param key
   *          the key
   * @param value
   *          the value
   */
  public void putProperty(String key, String value) {
    properties.put(key, value);
    dirty = true;
  }

  /**
   * get the value for the given key
   *
   * @param key
   *          the key to search the value for
   * @return the value or null
   */
  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  /**
   * get the value for the given key
   *
   * @param key
   *          the key to search the value for
   * @param defaultValue
   *          a default value, when key not found
   * @return the value or defaultValue
   */
  public String getProperty(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }

  /**
   * get the value as boolean<br>
   * if the value is not available or not parsable, this will return {@literal false}
   *
   * @param key
   *          the key to search the value for
   * @return true or false
   */
  public boolean getPropertyAsBoolean(String key) {
    String value = properties.getProperty(key);
    if (StringUtils.isBlank(value)) {
      return false;
    }

    return Boolean.parseBoolean(value);
  }

  /**
   * get the value as Integer<br>
   * if the value is not available or not parsable, this will return zero
   *
   * @param key
   *          the key to search the value for
   * @return the value or zero
   */
  public Integer getPropertyAsInteger(String key) {
    String value = properties.getProperty(key);
    if (StringUtils.isBlank(value)) {
      return 0;
    }

    try {
      return Integer.parseInt(value);
    }
    catch (Exception ignored) {
      // ignored
    }

    return 0;
  }

  private static class SortedProperties extends Properties {
    @Override
    public Set<Object> keySet() {
      return Collections.unmodifiableSet(new TreeSet<>(super.keySet()));
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {

      Set<Map.Entry<Object, Object>> set1 = super.entrySet();
      Set<Map.Entry<Object, Object>> set2 = new LinkedHashSet<>(set1.size());

      Iterator<Map.Entry<Object, Object>> iterator = set1.stream().sorted(Comparator.comparing(o -> o.getKey().toString())).iterator();

      while (iterator.hasNext())
        set2.add(iterator.next());

      return set2;
    }

    @Override
    public synchronized Enumeration<Object> keys() {
      return Collections.enumeration(new TreeSet<>(super.keySet()));
    }
  }
}
