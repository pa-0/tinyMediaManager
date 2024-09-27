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
package org.tinymediamanager.scraper.tvmaze.entities;

import java.util.List;

public class Show {
  public int          id;
  public String       url;
  public String       name;
  public String       type;
  public String       language;
  public List<String> genres;
  public String       status;
  public int          runtime;
  public int          averageRuntime;
  public String       premiered;
  public String       ended;
  public Network      network;
  public String       officialSite;
  public Rating       rating;
  public ExternalIds  externals;
  public ImageUrls    image;
  public String       summary;
  public Embed        _embedded;
}
