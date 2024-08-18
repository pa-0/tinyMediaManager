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

package org.tinymediamanager.core.movie.tasks;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.threading.TmmThreadPool;

/**
 * The class {@link MovieRemoveDatasourceTask} is used to remove a data source including all movies from tmm
 * 
 * @author Manuel Laggner
 */
public class MovieRemoveDatasourceTask extends TmmThreadPool {
  private final String datasource;

  public MovieRemoveDatasourceTask(String datasource) {
    super(TmmResourceBundle.getString("Settings.datasource.remove"));
    this.datasource = datasource;
  }

  @Override
  protected void doInBackground() {
    MovieModuleManager.getInstance().getSettings().removeMovieDataSources(datasource);
  }

  @Override
  public void callback(Object obj) {
    // not needed
  }
}
