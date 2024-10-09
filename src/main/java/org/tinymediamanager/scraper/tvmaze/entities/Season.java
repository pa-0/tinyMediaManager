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

public class Season {
  public int        id;
  public String     url;
  public int        number;
  public String     name;
  public int        episodeOrder;
  public String     premiereDate;
  public String     endDate;
  public Network    network;
  public WebChannel webChannel;
  public ImageUrls  image;
  public String     summary;
  public Embed      _embedded;
}
