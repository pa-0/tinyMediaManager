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

package org.tinymediamanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * the class {@link LauncherExtraConfig} is used to read/write the custom config for the launcher
 *
 * @author Manuel Laggner
 */
public class LauncherExtraConfig {
  public static final String LAUNCHER_EXTRA_YML = "launcher-extra.yml";

  @JsonProperty
  public String              javaHome           = "";
  @JsonProperty
  public List<String>        jvmOpts            = new ArrayList<>();
  @JsonProperty
  public List<String>        env                = new ArrayList<>();

  @JsonIgnore
  public File                file;

  public static LauncherExtraConfig readFile(File file) {
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    LauncherExtraConfig extraConfig;

    try {
      extraConfig = objectMapper.readValue(file, LauncherExtraConfig.class);
    }
    catch (Exception ignored) {
      // we could somehow not read this file - just create an empty one
      extraConfig = new LauncherExtraConfig();
    }

    extraConfig.file = file;

    return extraConfig;
  }

  public void save() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    objectMapper.writeValue(file, this);
  }
}
