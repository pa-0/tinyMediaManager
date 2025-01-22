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

package org.tinymediamanager.updater.getdown;

import static com.threerings.getdown.Log.log;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;

import org.tinymediamanager.scraper.http.Url;

import com.threerings.getdown.data.EnvConfig;
import com.threerings.getdown.data.Resource;

public class TmmGetdownApplication extends com.threerings.getdown.data.Application {

  public static final String UPDATE_FOLDER = "update/";

  /**
   * Creates an application instance which records the location of the {@code getdown.txt} configuration file from the supplied application directory.
   *
   * @param envc
   */
  public TmmGetdownApplication(EnvConfig envc) {
    super(envc);
  }

  @Override
  protected Resource createResource(String path, EnumSet<Resource.Attr> attrs) throws MalformedURLException {
    return new TmmGetdownResource(path, this.getRemoteURL(path), this.getLocalPath(path), attrs);
  }

  @Override
  protected File downloadFile(String path) throws IOException {
    // force the application to download with our okhttp implementation
    File target = getLocalPath(path + "_new");

    URL targetURL = null;
    try {
      targetURL = getRemoteURL(path);
    }
    catch (Exception e) {
      log.warning("Requested to download invalid control file", "appbase", _vappbase, "path", path, "error", e);
      throw new IOException("Invalid path '" + path + "'.", e);
    }

    log.info("Attempting to refetch '" + path + "' from '" + targetURL + "'.");
    // conn.download(targetURL, target); // stream the URL into our temporary file
    Url url = new Url(targetURL.toString());
    url.download(target);

    return target;
  }
}
