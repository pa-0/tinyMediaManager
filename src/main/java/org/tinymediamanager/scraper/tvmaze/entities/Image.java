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
package org.tinymediamanager.scraper.tvmaze.entities;

import org.tinymediamanager.scraper.entities.BaseJsonEntity;

public class Image extends BaseJsonEntity {
  public int         id;
  public String      type;
  boolean            main = false;
  public Resolutions resolutions;

  public static class Resolutions extends BaseJsonEntity {
    public Resolution original;
    public Resolution medium;
  }

  public static class Resolution extends BaseJsonEntity {
    public String url;
    public int    width;
    public int    height;
  }
}
