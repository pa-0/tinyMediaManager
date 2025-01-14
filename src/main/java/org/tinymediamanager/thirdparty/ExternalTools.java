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

package org.tinymediamanager.thirdparty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.util.UrlUtil;
import org.tinymediamanager.updater.UpdateCheck;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * The class {@link ExternalTools} is used to unmarshall all external tools from the JSON file
 * 
 * @author Manuel Laggner
 */
public class ExternalTools {
  private static final Logger LOGGER        = LoggerFactory.getLogger(ExternalTools.class);

  private final String        urlAsString;
  private List<ExternalTool>  externalTools = null;

  /**
   * Main-Constructor
   * 
   * @param url
   *          the url to the JSON file
   */
  public ExternalTools(String url) {
    urlAsString = url;
  }

  /**
   * Check whether this tool is installed or not
   * 
   * @param toolName
   *          the tool name
   * @return true/false
   */
  public static boolean isToolInstalled(String toolName) {
    Path versionFile = getVersionFile(toolName);
    return Files.exists(versionFile);
  }

  /**
   * Gets the version filename for the given tool
   * 
   * @param toolName
   *          the tool name
   * @return the {@link Path} of the version file
   */
  private static Path getVersionFile(String toolName) {
    return Paths.get(Globals.ADDON_FOLDER, toolName + ".ver");
  }

  /**
   * Check if an update is available for the given tool
   * 
   * @param toolName
   *          the tool name
   * @return true/false
   * @throws Exception
   *           any {@link Exception} thrown while checking
   */
  public boolean isUpdateAvailable(String toolName) throws Exception {
    // lazy loading of the JSON
    init();

    // find the tool
    ExternalTool externalTool = findExternalTool(toolName);

    // not available? just throw an exception
    if (externalTool == null) {
      throw new IllegalArgumentException();
    }

    // let's have a look if the tool is installed and
    if (isToolInstalled(toolName)) {
      String installedVersion = Utils.readFileToString(getVersionFile(toolName));

      if (installedVersion.equalsIgnoreCase(externalTool.version)) {
        return false;
      }

      return true;
    }

    return false;
  }

  /**
   * Lazy load the JSON containing the download urls
   * 
   * @throws Exception
   *           any Exception thrown while downloading
   */
  private void init() throws Exception {
    // lazy loading of the JSON
    if (externalTools == null) {
      externalTools = parseJson();
    }
  }

  /**
   * Find the desired tool in our JSON
   * 
   * @param toolName
   *          the tool name
   * @return the {@link ExternalTool} if found or null
   */
  private ExternalTool findExternalTool(String toolName) {
    for (ExternalTool entry : externalTools) {
      if (toolName.equalsIgnoreCase(entry.name)) {
        return entry;
      }
    }

    return null;
  }

  /**
   * Download the given tool to the addons folder
   * 
   * @param toolName
   *          the tool name
   * @throws Exception
   *           any {@link Exception} thrown while downloading
   */
  public void downloadTool(String toolName) throws Exception {
    // lazy loading of the JSON
    init();

    ExternalTool externalTool = findExternalTool(toolName);

    // not available? just throw an exception
    if (externalTool == null) {
      throw new IllegalArgumentException();
    }

    // find the right url for our arch
    String osArch = getOsArchString();
    ExternalToolUrl toolUrl = null;
    for (ExternalToolUrl entry : externalTool.urls) {
      if (osArch.equalsIgnoreCase(entry.arch)) {
        toolUrl = entry;
        break;
      }
    }

    if (toolUrl == null) {
      throw new Exception("no download for the given os/arch found");
    }

    Path addonFolder = Paths.get(Globals.ADDON_FOLDER);

    // download to temp folder
    Path tempFile = Paths.get(Utils.getTempFolder(), UrlUtil.getFilename(toolUrl.url));
    try {

      Url url = new Url(toolUrl.url);
      url.download(tempFile);

      if (FilenameUtils.getExtension(tempFile.getFileName().toString()).equals("zip")) {
        /*
         * ZIP file
         */

        // extract the file if it is an archive
        Path destinationFolder = tempFile.resolveSibling(UrlUtil.getBasename(toolUrl.url));

        try {
          // unzip
          Utils.unzip(tempFile, destinationFolder);

          // and move the needed file from the extract
          Path source = Paths.get(destinationFolder.toString(), toolUrl.filenameInArchive);
          Path destinationFilename = addonFolder.resolve(getOsSpecificFilename(externalTool));

          if (Files.exists(destinationFilename)) {
            Utils.deleteFileSafely(destinationFilename);
          }

          // create addon folder when needed
          if (!Files.exists(addonFolder)) {
            Files.createDirectory(addonFolder);
          }

          if (Utils.moveFileSafe(source, destinationFilename)) {
            // copying good - make executable and write the version file
            if (!destinationFilename.toFile().setExecutable(true)) {
              LOGGER.debug("Could not make '{}' executable", destinationFilename);
            }
            Utils.writeStringToFile(addonFolder.resolve(toolName + ".ver"), externalTool.version);
          }
        }
        finally {
          // cleanup
          Utils.deleteDirectorySafely(destinationFolder);
        }
      }
      else {
        /*
         * Plain executable file
         */

        // and move the needed file from the extract
        Path destinationFilename = Paths.get(Globals.ADDON_FOLDER, getOsSpecificFilename(externalTool));

        if (Files.exists(destinationFilename)) {
          Utils.deleteFileSafely(destinationFilename);
        }

        // create addon folder when needed
        if (!Files.exists(addonFolder)) {
          Files.createDirectory(addonFolder);
        }

        if (Utils.moveFileSafe(tempFile, destinationFilename)) {
          // copying good - make executable and write the version file
          if (!destinationFilename.toFile().setExecutable(true)) {
            LOGGER.debug("Could not make '{}' executable", destinationFilename);
          }
          Utils.writeStringToFile(Paths.get(Globals.ADDON_FOLDER, toolName + ".ver"), externalTool.version);
        }
      }
    }
    finally {
      Utils.deleteFileSafely(tempFile);
    }
  }

  private String getOsArchString() {
    String osArch = "";

    // windows
    if (SystemUtils.IS_OS_WINDOWS) {
      osArch += "windows-amd64";
    }
    // linux
    else if (SystemUtils.IS_OS_LINUX) {
      osArch += "linux-";
      if (System.getProperty("os.arch").contains("arm") || System.getProperty("os.arch").contains("aarch")) {
        osArch += "arm64";
      }
      else {
        osArch += "amd64";
      }
    }
    // macos
    else if (SystemUtils.IS_OS_MAC) {
      osArch += "macos-";
      if (System.getProperty("os.arch").contains("arm") || System.getProperty("os.arch").contains("aarch")) {
        osArch += "arm64";
      }
      else {
        osArch += "amd64";
      }
    }

    return osArch;
  }

  /**
   * Get the OS specific filename
   * 
   * @param externalTool
   *          the {@link ExternalTool}
   * @return the OS specific filename
   */
  private String getOsSpecificFilename(ExternalTool externalTool) {
    if (SystemUtils.IS_OS_WINDOWS) {
      return externalTool.filename + ".exe";
    }
    return externalTool.filename;
  }

  List<ExternalTool> parseJson() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectReader reader = objectMapper.readerForListOf(ExternalTool.class);

    // cache the url for 10 minutes
    Url url = new OnDiskCachedUrl(urlAsString, 10, TimeUnit.MINUTES);

    return reader.readValue(url.getInputStream());
  }

  /**
   * internal classes
   */
  public static class ExternalTool {
    @JsonProperty
    private String                name     = "";
    @JsonProperty
    private String                version  = "";
    @JsonProperty
    private String                filename = "";
    @JsonProperty
    private List<ExternalToolUrl> urls     = new ArrayList<>();
  }

  public static class ExternalToolUrl {
    @JsonProperty
    private String arch              = "";
    @JsonProperty
    private String url               = "";
    @JsonProperty
    private String filenameInArchive = "";
  }

  public static class ExternalToolsUpgradeTask extends TmmTask {
    private static final Logger LOGGER    = LoggerFactory.getLogger(ExternalToolsUpgradeTask.class);

    private final String        toolName;
    private String              updateUrl = "";

    public ExternalToolsUpgradeTask(String toolName) {
      super(TmmResourceBundle.getString("task.externaltoolupgrade") + " - " + toolName, 100, TaskType.BACKGROUND_TASK);
      this.toolName = toolName;

      if (ReleaseInfo.isGitBuild()) {
        updateUrl = Paths.get("external-tools.json").toUri().toString();
      }
      else {
        List<String> updateUrls = UpdateCheck.parseUpdateUrls();
        if (!updateUrls.isEmpty()) {
          updateUrl = updateUrls.get(0) + "external-tools.json";
        }
      }
    }

    @Override
    protected void doInBackground() {
      ExternalTools externalTools = new ExternalTools(updateUrl);
      try {
        if (!isToolInstalled(toolName) || externalTools.isUpdateAvailable(toolName)) {
          externalTools.downloadTool(toolName);
        }
      }
      catch (Exception e) {
        LOGGER.debug("Could not update the external tool '{}' - '{}", toolName, e.getMessage());
      }
    }
  }
}
