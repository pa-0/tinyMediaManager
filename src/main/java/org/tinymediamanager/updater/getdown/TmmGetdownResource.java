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

package org.tinymediamanager.updater.getdown;

import static com.threerings.getdown.Log.log;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tinymediamanager.updater.getdown.TmmGetdownApplication.UPDATE_FOLDER;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.EnumSet;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.tinymediamanager.core.Utils;

import com.threerings.getdown.data.Digest;
import com.threerings.getdown.data.Resource;
import com.threerings.getdown.util.FileUtil;
import com.threerings.getdown.util.ProgressObserver;

public class TmmGetdownResource extends Resource {

  public TmmGetdownResource(String path, URL remote, File local, EnumSet<Attr> attrs) {
    super(path, remote, local, attrs);
    this._localNew = new File(UPDATE_FOLDER + File.separator + path);
    this._marker = new File(UPDATE_FOLDER + File.separator + ".getdown", path + "v");

    boolean unpack = attrs.contains(Attr.UNPACK);
    if (unpack && _isZip) {
      _unpacked = _localNew.getParentFile();
    }
  }

  @Override
  public String computeDigest(int version, MessageDigest md, ProgressObserver obs) throws IOException {
    // check if there is a .md5 file containing the digest (useful for resources, where the main file gets deleted after extracting)
    File file = new File(_local.getParent(), _local.getName() + ".sha256");
    if (file.exists()) {
      return FileUtils.readFileToString(file, UTF_8).strip();
    }

    return super.computeDigest(version, md, obs);
  }

  @Override
  public boolean isMarkedValid() {
    // make sure the markers parent is created
    File parent = new File(_marker.getParent());
    if (!parent.exists() && !parent.mkdirs()) {
      log.warning("Failed to create target directory for resource '" + _marker + "'.");
    }
    return super.isMarkedValid();
  }

  @Override
  public void install(boolean validate) throws IOException {
    // just unpack the downloaded files (w/o moving them to the new destination)
    applyAttrs();
  }

  @Override
  public void applyAttrs() throws IOException {
    if (shouldUnpack()) {
      unpack();
    }
    if (_attrs.contains(Attr.EXEC)) {
      FileUtil.makeExecutable(_localNew);
    }
  }

  @Override
  public void unpack() throws IOException {
    // sanity check
    if (!_isZip) {
      throw new IOException("Requested to unpack not supported archive file '" + _localNew + "'.");
    }

    // first create a .md5 file since we delete the archive afterwards
    String newFileName = _localNew.getName() + ".sha256";
    MessageDigest digest = Digest.getMessageDigest(Digest.VERSION);
    Utils.writeStringToFile(_localNew.toPath().resolve(newFileName), computeDigest(Digest.VERSION, _localNew, digest, null));

    if (_isZip) {
      try (ZipFile jar = new ZipFile(_localNew)) {
        FileUtil.unpackJar(jar, _unpacked, _attrs.contains(Attr.CLEAN));
      }
    }
  }
}
