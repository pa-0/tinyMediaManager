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

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

/**
 * The Class Globals. used to hold global information/fields for the whole application
 * 
 * @author Manuel Laggner
 */
public final class Globals {
  private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("tmm.debug", "false"));
  private static final boolean READ_ONLY;

  public static final String   CONTENT_FOLDER;

  public static final String   DATA_FOLDER;
  public static final String   CACHE_FOLDER;
  public static final String   BACKUP_FOLDER;
  public static final String   LOG_FOLDER;
  public static final String   ADDON_FOLDER;

  static {
    // first we look for a dedicated folder property
    // after that we look for tmm.contentfolder
    String dataFolder = System.getProperty("tmm.datafolder");
    String cacheFolder = System.getProperty("tmm.cachefolder");
    String backupFolder = System.getProperty("tmm.backupfolder");
    String logFolder = System.getProperty("tmm.logfolder");
    String addonFolder = System.getProperty("tmm.addonfolder");

    READ_ONLY = !isTmmDirWritable();

    // always filled!
    String contentFolder = System.getProperty("tmm.contentfolder");
    if (StringUtils.isBlank(contentFolder)) {
      // when .userdir exists and data not exists OR tmm folder is not writable we can use the local userdir
      if ((Files.exists(Paths.get(".userdir")) && Files.notExists(Paths.get("data"))) || isReadOnly()) {
        // userdir
        contentFolder = TmmOsUtils.getUserDir().toString();
      }
      else {
        // portable - current folder
        contentFolder = ".";
      }
    }
    CONTENT_FOLDER = Paths.get(contentFolder).toAbsolutePath().normalize().toString();

    // data
    if (StringUtils.isNotBlank(dataFolder)) {
      DATA_FOLDER = dataFolder;
    }
    else {
      DATA_FOLDER = Paths.get(contentFolder, "data").toAbsolutePath().normalize().toString();
    }

    // cache
    if (StringUtils.isNotBlank(cacheFolder)) {
      CACHE_FOLDER = cacheFolder;
    }
    else {
      CACHE_FOLDER = Paths.get(contentFolder, "cache").toAbsolutePath().normalize().toString();
    }

    // backup
    if (StringUtils.isNotBlank(backupFolder)) {
      BACKUP_FOLDER = backupFolder;
    }
    else {
      BACKUP_FOLDER = Paths.get(contentFolder, "backup").toAbsolutePath().normalize().toString();
    }

    // logs
    if (StringUtils.isNotBlank(logFolder)) {
      LOG_FOLDER = logFolder;
    }
    else {
      LOG_FOLDER = Paths.get(contentFolder, "logs").toAbsolutePath().normalize().toString();
    }

    // addons
    if (StringUtils.isNotBlank(addonFolder)) {
      ADDON_FOLDER = addonFolder;
    }
    else {
      ADDON_FOLDER = Paths.get(contentFolder, "addons").toAbsolutePath().normalize().toString();
    }
  }

  private Globals() {
    throw new IllegalAccessError();
  }

  private static boolean isTmmDirWritable() {
    try {
      RandomAccessFile f = new RandomAccessFile("access.test", "rw");
      f.close();
      Files.deleteIfExists(Paths.get("access.test"));
      return true;
    }
    catch (Exception e) {
      // ignore
    }
    return false;
  }

  /**
   * are we in our internal debug mode?
   * 
   * @return true/false
   */
  public static boolean isDebug() {
    return DEBUG;
  }

  /**
   * ReadOnly TMM instance? cannot write to TMM dir...
   * 
   * @return
   */
  public static boolean isReadOnly() {
    return READ_ONLY;
  }

  /**
   * checks, if we are within a dockerized environment<br>
   * not 100% accurate!
   *
   * @return true/false
   */
  public static boolean isDocker() {
    if (Files.exists(Paths.get("/.dockerenv"))) {
      return true;
    }
    return false;
  }

  /**
   * Are we running from a webstart instance?
   * 
   * @return true/false
   */
  public static boolean isRunningJavaWebStart() {
    boolean hasJNLP = false;
    try {
      Class.forName("javax.jnlp.ServiceManager");
      hasJNLP = true;
    }
    catch (ClassNotFoundException ex) {
      hasJNLP = false;
    }
    return hasJNLP;
  }

  /**
   * Are we running on a jetty webswing instance?
   * 
   * @return true/false
   */
  public static boolean isRunningWebSwing() {
    return System.getProperty("webswing.classPath") != null;
  }

  /**
   * check if the installation is self-udpatable
   * 
   * @return true if the installation is self-updatable
   */
  public static boolean isSelfUpdatable() {
    // env param -Dtmm.noupdate=true has been set
    if (Boolean.parseBoolean(System.getProperty("tmm.noupdate"))) {
      return false;
    }

    // no on macOS (would destroy the signature)
    if (SystemUtils.IS_OS_MAC) {
      return false;
    }

    // special files exist (e.g. in docker env)
    if (isDocker() || Files.exists(Paths.get(".managed"))) {
      return false;
    }

    // tmm folder is not even writable
    if (isReadOnly()) {
      return false;
    }

    return true;
  }

  /**
   * can we check if an update is available
   * 
   * @return true if we should check for an update
   */
  public static boolean canCheckForUpdates() {
    if (ReleaseInfo.isGitBuild()) {
      return false;
    }

    // no update is forced
    if (Boolean.parseBoolean(System.getProperty("tmm.noupdate"))) {
      return false;
    }

    return true;
  }
}
