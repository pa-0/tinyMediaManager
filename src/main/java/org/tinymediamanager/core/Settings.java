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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jdesktop.observablecollections.ObservableCollections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.ImageCache.CacheSize;
import org.tinymediamanager.core.ImageCache.CacheType;
import org.tinymediamanager.core.http.TmmHttpServer;
import org.tinymediamanager.scraper.http.ProxySettings;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.util.StrgUtils;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * The Class Settings - holding all settings for tmm.
 *
 * @author Manuel Laggner
 */
public final class Settings extends AbstractSettings {
  private static final Logger                              LOGGER                       = LoggerFactory.getLogger(Settings.class);

  /**
   * Constants mainly for events
   */
  private static final String                              TITLE_PREFIX                 = "titlePrefixes";
  private static final String                              VIDEO_FILE_TYPE              = "videoFileType";
  private static final String                              AUDIO_FILE_TYPE              = "audioFileType";
  private static final String                              SUBTITLE_FILE_TYPE           = "subtitleFileType";
  private static final String                              CLEANUP_FILE_TYPE            = "cleanupFileType";
  private static final String                              WOL_DEVICES                  = "wolDevices";
  private static final String                              CUSTOM_ASPECT_RATIOS         = "customAspectRatios";

  /**
   * statics
   */
  private static final String                              CONFIG_FILE                  = "tmm.json";

  private static Settings                                  instance;

  private final List<String>                               titlePrefixes                = ObservableCollections.observableList(new ArrayList<>());
  private final List<String>                               videoFileTypes               = ObservableCollections.observableList(new ArrayList<>());
  private final List<String>                               audioFileTypes               = ObservableCollections.observableList(new ArrayList<>());
  private final List<String>                               subtitleFileTypes            = ObservableCollections.observableList(new ArrayList<>());
  private final List<String>                               cleanupFileTypes             = ObservableCollections.observableList(new ArrayList<>());
  private final List<WolDevice>                            wolDevices                   = ObservableCollections.observableList(new ArrayList<>());
  private final List<String>                               customAspectRatios           = ObservableCollections.observableList(new ArrayList<>());

  private String                                           version                      = "";

  private String                                           proxyHost;
  private String                                           proxyPort;
  private String                                           proxyUsername;
  private String                                           proxyPassword;
  private int                                              maximumDownloadThreads       = 2;

  private String                                           traktAccessToken             = "";
  private String                                           traktRefreshToken            = "";
  private DateField                                        traktDateField               = DateField.DATE_ADDED;

  private String                                           kodiHost                     = "";
  private int                                              kodiHttpPort                 = 8080;
  private int                                              kodiTcpPort                  = 9090;
  private String                                           kodiUsername                 = "";
  private String                                           kodiPassword                 = "";

  private boolean                                          imageCache                   = true;
  private CacheSize                                        imageCacheSize               = CacheSize.BIG;
  private CacheType                                        imageCacheType               = CacheType.QUALITY;

  // language 2 char - saved to config
  private String                                           language;
  private String                                           mediaPlayer                  = "";
  private boolean                                          useInternalMediaFramework    = true;
  private String                                           mediaFramework               = "";
  private Integer                                          ffmpegPercentage             = 50;

  private String                                           theme                        = "Light";
  private int                                              fontSize                     = 12;
  private String                                           fontFamily                   = "Dialog";

  private boolean                                          imageChooserUseEntityFolder  = false;
  private boolean                                          storeWindowPreferences       = true;
  private DateField                                        dateField                    = DateField.DATE_ADDED;
  private boolean                                          fileSizeDisplayHumanReadable = true;

  private boolean                                          enableTrash                  = true;
  private boolean                                          deleteTrashOnExit            = false;
  private boolean                                          showMemory                   = true;

  private boolean                                          enableHttpServer             = false;
  private int                                              httpServerPort               = 7878;
  private String                                           httpApiKey                   = UUID.randomUUID().toString();

  private boolean                                          upnpShareLibrary             = false;
  private boolean                                          upnpRemotePlay               = false;

  private boolean                                          ignoreSSLProblems            = true;

  private boolean                                          writeMediaInfoXml            = false;

  // aspect ratio detector
  boolean                                                  ardEnabled                   = false;
  private ArdSettings.Mode                                 ardMode                      = ArdSettings.Mode.DEFAULT;
  private Map<ArdSettings.Mode, ArdSettings.SampleSetting> ardSampleSettings            = ArdSettings.defaultSampleSettings();
  private float                                            ardIgnoreBeginningPct        = 2f;
  private float                                            ardIgnoreEndPct              = 8f;
  private boolean                                          ardRoundUp                   = false;
  private float                                            ardRoundUpThresholdPct       = 4f;
  private int                                              ardMFMode                    = 0;
  private float                                            ardMFThresholdPct            = 6f;
  private float                                            ardPlausiWidthPct            = 50f;
  private float                                            ardPlausiHeightPct           = 60f;
  private float                                            ardPlausiWidthDeltaPct       = 1.5f;
  private float                                            ardPlausiHeightDeltaPct      = 2f;
  private float                                            ardSecondaryDelta            = 0.15f;
  private float                                            ardDarkLevelPct              = 7f;
  private float                                            ardDarkLevelMaxPct           = 13f;

  private boolean                                          enableAutomaticUpdate        = true;
  private int                                              automaticUpdateInterval      = 1;
  private String                                           mdbListApiKey                = "";

  /**
   * Instantiates a new settings.
   */
  public Settings() {
    super();
    addPropertyChangeListener(evt -> setDirty());
  }

  @Override
  protected ObjectWriter createObjectWriter() {
    return objectMapper.writerFor(Settings.class);
  }

  @Override
  public String getConfigFilename() {
    return CONFIG_FILE;
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected void afterLoading() {
    // create a new HTTP client to force setting the right proxy/SSL params
    setProxy();
    System.setProperty("tmm.trustallcerts", Boolean.toString(ignoreSSLProblems));

    TmmHttpClient.recreateHttpClient();
  }

  @Override
  protected void writeDefaultSettings() {
    version = ReleaseInfo.getVersion();

    // default video file types derived from
    // http://wiki.xbmc.org/index.php?title=Advancedsettings.xml#.3Cvideoextensions.3E
    videoFileTypes.clear();
    videoFileTypes.addAll(MediaFileHelper.DEFAULT_VIDEO_FILETYPES);
    Collections.sort(videoFileTypes);
    firePropertyChange(VIDEO_FILE_TYPE, null, videoFileTypes);

    audioFileTypes.clear();
    audioFileTypes.addAll(MediaFileHelper.DEFAULT_AUDIO_FILETYPES);
    Collections.sort(audioFileTypes);
    firePropertyChange(AUDIO_FILE_TYPE, null, audioFileTypes);

    // default subtitle files
    subtitleFileTypes.clear();
    subtitleFileTypes.addAll(MediaFileHelper.DEFAULT_SUBTITLE_FILETYPES);
    Collections.sort(subtitleFileTypes);
    firePropertyChange(SUBTITLE_FILE_TYPE, null, subtitleFileTypes);

    // default custom aspect ratios
    customAspectRatios.clear();
    customAspectRatios.addAll(AspectRatio.getDefaultValues().stream().map(Object::toString).collect(Collectors.toList()));
    Collections.sort(customAspectRatios);
    firePropertyChange(CUSTOM_ASPECT_RATIOS, null, customAspectRatios);

    // default title prefix
    titlePrefixes.clear();
    addTitlePrefix("A");
    addTitlePrefix("An");
    addTitlePrefix("The");
    addTitlePrefix("Der");
    addTitlePrefix("Die");
    addTitlePrefix("Das");
    addTitlePrefix("Ein");
    addTitlePrefix("Eine");
    addTitlePrefix("Le");
    addTitlePrefix("La");
    addTitlePrefix("Les");
    addTitlePrefix("L'");
    addTitlePrefix("L´");
    addTitlePrefix("L`");
    addTitlePrefix("Un");
    addTitlePrefix("Une");
    addTitlePrefix("Des");
    addTitlePrefix("Du");
    addTitlePrefix("D'");
    addTitlePrefix("D´");
    addTitlePrefix("D`");
    Collections.sort(titlePrefixes);

    // default cleanup postfix
    cleanupFileTypes.clear();
    addCleanupFileType(".txt$");
    addCleanupFileType(".url$");
    addCleanupFileType(".html$");
    addCleanupFileType(".sfv$");
    Collections.sort(cleanupFileTypes);

    ardSampleSettings = ArdSettings.defaultSampleSettings();

    setProxyFromSystem();

    saveSettings();
  }

  /**
   * Gets the single instance of Settings.
   *
   * @return single instance of Settings
   */
  public static synchronized Settings getInstance() {
    return getInstance(Globals.DATA_FOLDER);
  }

  /**
   * Override our settings folder (defaults to "data")<br>
   * <b>Should only be used for unit testing et all!</b><br>
   *
   * @return single instance of Settings
   */
  static synchronized Settings getInstance(String folder) {
    if (instance == null) {
      instance = (Settings) getInstance(folder, CONFIG_FILE, Settings.class);
    }
    return instance;
  }

  /**
   * removes the active instance <br>
   * <b>Should only be used for unit testing et all!</b><br>
   */
  static void clearInstance() {
    instance = null;
  }

  /**
   * is our settings file up to date?
   */
  public boolean isCurrentVersion() {
    return StrgUtils.compareVersion(version, ReleaseInfo.getVersion()) == 0;
  }

  /**
   * gets the version of out settings file
   */
  public String getVersion() {
    return version;
  }

  /**
   * needed for JSON unmarshalling
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * sets the current version into settings file
   */
  public void setCurrentVersion() {
    version = ReleaseInfo.getVersion();
    setDirty();
  }

  /**
   * Clear dirty.
   */
  private void clearDirty() {
    dirty = false;
  }

  /**
   * Adds a title prefix.
   *
   * @param prfx
   *          the prefix
   */
  public void addTitlePrefix(String prfx) {
    if (!titlePrefixes.contains(prfx)) {
      titlePrefixes.add(prfx);
      firePropertyChange(TITLE_PREFIX, null, titlePrefixes);
    }
  }

  /**
   * Removes the title prefix.
   *
   * @param prfx
   *          the prfx
   */
  public void removeTitlePrefix(String prfx) {
    titlePrefixes.remove(prfx);
    firePropertyChange(TITLE_PREFIX, null, titlePrefixes);
  }

  /**
   * Gets the title prefix.
   *
   * @return the title prefix
   */
  public List<String> getTitlePrefix() {
    return titlePrefixes;
  }

  /**
   * Adds the video file types.
   *
   * @param type
   *          the type
   */
  public void addVideoFileType(String type) {
    if (!type.startsWith(".")) {
      type = "." + type;
    }
    if (!videoFileTypes.contains(type)) {
      videoFileTypes.add(type);
      firePropertyChange(VIDEO_FILE_TYPE, null, videoFileTypes);
    }
  }

  /**
   * Removes the video file type.
   *
   * @param type
   *          the type
   */
  public void removeVideoFileType(String type) {
    videoFileTypes.remove(type);
    firePropertyChange(VIDEO_FILE_TYPE, null, videoFileTypes);
  }

  /**
   * Gets the video file type.
   *
   * @return the video file type
   */
  public List<String> getVideoFileType() {
    return videoFileTypes;
  }

  /**
   * Adds the audio file types.
   *
   * @param type
   *          the type
   */
  public void addAudioFileType(String type) {
    if (!type.startsWith(".")) {
      type = "." + type;
    }
    if (!audioFileTypes.contains(type)) {
      audioFileTypes.add(type);
      firePropertyChange(AUDIO_FILE_TYPE, null, audioFileTypes);
    }
  }

  /**
   * Removes the audio file type.
   *
   * @param type
   *          the type
   */
  public void removeAudioFileType(String type) {
    audioFileTypes.remove(type);
    firePropertyChange(AUDIO_FILE_TYPE, null, audioFileTypes);
  }

  /**
   * Gets the audio file type.
   *
   * @return the audio file type
   */
  public List<String> getAudioFileType() {
    return audioFileTypes;
  }

  /**
   * Adds the subtitle file types.
   *
   * @param type
   *          the type
   */
  public void addSubtitleFileType(String type) {
    if (!type.startsWith(".")) {
      type = "." + type;
    }
    if (!subtitleFileTypes.contains(type)) {
      subtitleFileTypes.add(type);
      firePropertyChange(SUBTITLE_FILE_TYPE, null, subtitleFileTypes);
    }
  }

  /**
   * Removes the subtitle file type.
   *
   * @param type
   *          the type
   */
  public void removeSubtitleFileType(String type) {
    if (!type.startsWith(".")) {
      type = "." + type;
    }
    subtitleFileTypes.remove(type);
    firePropertyChange(SUBTITLE_FILE_TYPE, null, subtitleFileTypes);
  }

  /**
   * Gets the subtitle file type.
   *
   * @return the subtitle file type
   */
  public List<String> getSubtitleFileType() {
    return subtitleFileTypes;
  }

  /**
   * Adds the cleanup file types.
   *
   * @param type
   *          the type
   */
  public void addCleanupFileType(String type) {
    if (!cleanupFileTypes.contains(type)) {
      cleanupFileTypes.add(type);
      firePropertyChange(CLEANUP_FILE_TYPE, null, cleanupFileTypes);
    }
  }

  /**
   * Removes the cleanup file type.
   *
   * @param type
   *          the type
   */
  public void removeCleanupFileType(String type) {
    cleanupFileTypes.remove(type);
    firePropertyChange(CLEANUP_FILE_TYPE, null, cleanupFileTypes);
  }

  /**
   * Gets the cleanup file type.
   *
   * @return the cleanup file type
   */
  public List<String> getCleanupFileType() {
    return cleanupFileTypes;
  }

  /**
   * set the
   * 
   * @param newValues
   */
  public void setCleanupFileTypes(List<String> newValues) {
    cleanupFileTypes.clear();
    cleanupFileTypes.addAll(newValues);
    firePropertyChange(CLEANUP_FILE_TYPE, null, cleanupFileTypes);
  }

  /**
   * Convenience method to get all supported file extensions
   *
   * @return list
   */
  public List<String> getAllSupportedFileTypes() {
    Set<String> set = new HashSet<>();
    set.addAll(MediaFileHelper.DEFAULT_VIDEO_FILETYPES);
    set.addAll(MediaFileHelper.DEFAULT_AUDIO_FILETYPES);
    set.addAll(MediaFileHelper.DEFAULT_SUBTITLE_FILETYPES);
    MediaFileHelper.SUPPORTED_ARTWORK_FILETYPES.forEach(type -> set.add("." + type));
    set.addAll(getAudioFileType());
    set.addAll(getVideoFileType());
    set.addAll(getSubtitleFileType());
    set.add(".nfo");
    return new ArrayList<>(set);
  }

  @Override
  public void saveSettings() {
    super.saveSettings();

    // set proxy information
    setProxy();

    // set HTTP API
    setHttpApi();

    // clear dirty flag
    clearDirty();
  }

  /**
   * Gets the proxy host.
   *
   * @return the proxy host
   */
  public String getProxyHost() {
    return proxyHost;
  }

  /**
   * Sets the proxy host.
   *
   * @param newValue
   *          the new proxy host
   */
  public void setProxyHost(String newValue) {
    String oldValue = this.proxyHost;
    this.proxyHost = newValue;
    firePropertyChange("proxyHost", oldValue, newValue);
  }

  /**
   * Gets the proxy port.
   *
   * @return the proxy port
   */
  public String getProxyPort() {
    return proxyPort;
  }

  /**
   * Sets the proxy port.
   *
   * @param newValue
   *          the new proxy port
   */
  public void setProxyPort(String newValue) {
    String oldValue = this.proxyPort;
    this.proxyPort = newValue;
    firePropertyChange("proxyPort", oldValue, newValue);
  }

  /**
   * Gets the proxy username.
   *
   * @return the proxy username
   */
  public String getProxyUsername() {
    return proxyUsername;
  }

  /**
   * Sets the proxy username.
   *
   * @param newValue
   *          the new proxy username
   */
  public void setProxyUsername(String newValue) {
    String oldValue = this.proxyUsername;
    this.proxyUsername = newValue;
    firePropertyChange("proxyUsername", oldValue, newValue);
  }

  /**
   * Gets the proxy password.
   *
   * @return the proxy password
   */
  @JsonSerialize(using = EncryptedStringSerializer.class)
  @JsonDeserialize(using = EncryptedStringDeserializer.class)
  public String getProxyPassword() {
    return StringEscapeUtils.unescapeXml(proxyPassword);
  }

  /**
   * Sets the proxy password.
   *
   * @param newValue
   *          the new proxy password
   */
  public void setProxyPassword(String newValue) {
    newValue = StringEscapeUtils.escapeXml10(newValue);
    String oldValue = this.proxyPassword;
    this.proxyPassword = newValue;
    firePropertyChange("proxyPassword", oldValue, newValue);
  }

  /**
   * Sets the proxy from system settings, if empty
   */
  public void setProxyFromSystem() {
    String[] proxyEnvs = { "http.proxyHost", "https.proxyHost", "proxyHost", "socksProxyHost" };
    for (String pe : proxyEnvs) {
      if (StringUtils.isBlank(getProxyHost())) {
        String val = System.getProperty(pe, "");
        if (StringUtils.isNotBlank(val)) {
          setProxyHost(val);
        }
      }
    }

    String[] proxyPortEnvs = { "http.proxyPort", "https.proxyPort", "proxyPort", "socksProxyPort" };
    for (String ppe : proxyPortEnvs) {
      if (StringUtils.isBlank(getProxyPort())) {
        String val = System.getProperty(ppe, "");
        if (StringUtils.isNotBlank(val)) {
          setProxyPort(val);
        }
      }
    }
  }

  /**
   * Sets the TMM proxy.
   */
  private void setProxy() {
    if (useProxy()) {
      System.setProperty("proxyHost", getProxyHost());

      if (StringUtils.isNotEmpty(getProxyPort())) {
        System.setProperty("proxyPort", getProxyPort());
      }

      if (StringUtils.isNotEmpty(getProxyUsername())) {
        System.setProperty("http.proxyUser", getProxyUsername());
        System.setProperty("https.proxyUser", getProxyUsername());
      }
      if (StringUtils.isNotEmpty(getProxyPassword())) {
        System.setProperty("http.proxyPassword", getProxyPassword());
        System.setProperty("https.proxyPassword", getProxyPassword());
      }
    }
    try {
      ProxySettings.setProxySettings(getProxyHost(), getProxyPort() == null ? 0 : Integer.parseInt(getProxyPort().strip()), getProxyUsername(),
          getProxyPassword());
    }
    catch (NumberFormatException e) {
      LOGGER.error("could not parse proxy port: {}", e.getMessage());
    }
  }

  /**
   * Should we use a proxy.
   *
   * @return true, if successful
   */
  public boolean useProxy() {
    return StringUtils.isNotBlank(getProxyHost());
  }

  /**
   * Checks if is image cache.
   *
   * @return true, if is image cache
   */
  public boolean isImageCache() {
    return imageCache;
  }

  /**
   * Sets the image cache.
   *
   * @param newValue
   *          the new image cache
   */
  public void setImageCache(boolean newValue) {
    boolean oldValue = this.imageCache;
    this.imageCache = newValue;
    firePropertyChange("imageCache", oldValue, newValue);
  }

  /**
   * Gets the image cache size.
   *
   * @return the image cache size
   */
  public CacheSize getImageCacheSize() {
    return imageCacheSize;
  }

  /**
   * Sets the image cache size.
   *
   * @param newValue
   *          the new image cache size
   */
  public void setImageCacheSize(CacheSize newValue) {
    CacheSize oldValue = this.imageCacheSize;
    this.imageCacheSize = newValue;
    firePropertyChange("imageCacheSize", oldValue, newValue);
  }

  /**
   * Gets the image cache type.
   *
   * @return the image cache type
   */
  public CacheType getImageCacheType() {
    return imageCacheType;
  }

  /**
   * Sets the image cache type.
   *
   * @param newValue
   *          the new image cache type
   */
  public void setImageCacheType(CacheType newValue) {
    CacheType oldValue = this.imageCacheType;
    this.imageCacheType = newValue;
    firePropertyChange("imageCacheType", oldValue, newValue);
  }

  /**
   * is our library shared via UPNP?
   *
   * @return true/false
   */
  public boolean isUpnpShareLibrary() {
    return upnpShareLibrary;
  }

  /**
   * share library via UPNP?
   *
   * @param upnpShareLibrary
   *          share library or not
   */
  public void setUpnpShareLibrary(boolean upnpShareLibrary) {
    boolean old = this.upnpShareLibrary;
    this.upnpShareLibrary = upnpShareLibrary;
    firePropertyChange("upnpShareLibrary", old, upnpShareLibrary);
  }

  /**
   * should we search for rendering devices like Kodi, TVs, et all?
   *
   * @return true/false
   */
  public boolean isUpnpRemotePlay() {
    return upnpRemotePlay;
  }

  /**
   * should we search for rendering devices like Kodi, TVs, et all?
   *
   * @param upnpRemotePlay
   *          search for remote devices or not
   */
  public void setUpnpRemotePlay(boolean upnpRemotePlay) {
    boolean old = this.upnpRemotePlay;
    this.upnpRemotePlay = upnpRemotePlay;
    firePropertyChange("upnpRemotePlay", old, upnpRemotePlay);
  }

  /**
   * get Localge.getLanguage() 2 char from settings
   *
   * @return 2 char string - use "new Locale(getLanguage())"
   */
  public String getLanguage() {
    if (language == null || language.isEmpty()) {
      return Locale.getDefault().getLanguage();
    }
    return language;
  }

  /**
   * set Locale.toString() 5 char into settings
   *
   * @param language
   *          the language to be set
   */
  public void setLanguage(String language) {
    String oldValue = this.language;
    this.language = language;
    Locale.setDefault(Utils.getLocaleFromLanguage(language));
    firePropertyChange("language", oldValue, language);
  }

  public void addWolDevices(WolDevice newDevice) {
    wolDevices.add(newDevice);
    firePropertyChange(WOL_DEVICES, null, wolDevices);
  }

  public void removeWolDevices(WolDevice device) {
    wolDevices.remove(device);
    firePropertyChange(WOL_DEVICES, null, wolDevices);
  }

  public List<WolDevice> getWolDevices() {
    return wolDevices;
  }

  public void setWolDevices(List<WolDevice> newValues) {
    wolDevices.clear();
    wolDevices.addAll(newValues);
    firePropertyChange(WOL_DEVICES, null, wolDevices);
  }

  @JsonSerialize(using = EncryptedStringSerializer.class)
  @JsonDeserialize(using = EncryptedStringDeserializer.class)
  public String getTraktAccessToken() {
    return traktAccessToken;
  }

  public void setTraktAccessToken(String newValue) {
    String oldValue = this.traktAccessToken;
    this.traktAccessToken = newValue.strip();
    firePropertyChange("traktAccessToken", oldValue, newValue);
  }

  @JsonSerialize(using = EncryptedStringSerializer.class)
  @JsonDeserialize(using = EncryptedStringDeserializer.class)
  public String getTraktRefreshToken() {
    return traktRefreshToken;
  }

  public void setTraktRefreshToken(String newValue) {
    String oldValue = this.traktRefreshToken;
    this.traktRefreshToken = newValue;
    firePropertyChange("traktRefreshToken", oldValue, newValue);
  }

  public DateField getTraktDateField() {
    return traktDateField;
  }

  public void setTraktDateField(DateField newValue) {
    DateField oldValue = this.traktDateField;
    this.traktDateField = newValue;
    firePropertyChange("traktDateField", oldValue, newValue);
  }

  public String getKodiHost() {
    return kodiHost;
  }

  public void setKodiHost(String newValue) {
    String oldValue = this.kodiHost;
    this.kodiHost = newValue;
    firePropertyChange("kodiHost", oldValue, newValue);
  }

  /**
   * gets saved Kodi HTTP port, or default
   *
   * @return the Kodi HTTP port
   */
  public int getKodiHttpPort() {
    return kodiHttpPort == 0 ? 8080 : kodiHttpPort;
  }

  public void setKodiHttpPort(int kodiHttpPort) {
    int oldValue = this.kodiHttpPort;
    this.kodiHttpPort = kodiHttpPort;
    firePropertyChange("kodiHttpPort", oldValue, kodiHttpPort);
  }

  /**
   * gets saved Kodi TCP port, or default
   *
   * @return the Kodi TCP port
   */
  public int getKodiTcpPort() {
    return kodiTcpPort == 0 ? 9090 : kodiTcpPort;
  }

  public void setKodiTcpPort(int kodiTcpPort) {
    int oldValue = this.kodiHttpPort;
    this.kodiTcpPort = kodiTcpPort;
    firePropertyChange("kodiTcpPort", oldValue, kodiTcpPort);
  }

  public String getKodiUsername() {
    return kodiUsername;
  }

  public void setKodiUsername(String newValue) {
    String oldValue = this.kodiUsername;
    this.kodiUsername = newValue;
    firePropertyChange("kodiUsername", oldValue, newValue);
  }

  @JsonSerialize(using = EncryptedStringSerializer.class)
  @JsonDeserialize(using = EncryptedStringDeserializer.class)
  public String getKodiPassword() {
    return kodiPassword;
  }

  public void setKodiPassword(String newValue) {
    String oldValue = this.kodiPassword;
    this.kodiPassword = newValue;
    firePropertyChange("kodiPassword", oldValue, newValue);
  }

  public void setMediaPlayer(String newValue) {
    String oldValue = mediaPlayer;
    mediaPlayer = newValue;
    firePropertyChange("mediaPlayer", oldValue, newValue);
  }

  public boolean isUseInternalMediaFramework() {
    return useInternalMediaFramework;
  }

  public void setUseInternalMediaFramework(boolean newValue) {
    boolean oldvalue = useInternalMediaFramework;
    useInternalMediaFramework = newValue;
    firePropertyChange("useInternalMediaFramework", oldvalue, newValue);
  }

  public void setMediaFramework(String newValue) {
    String oldValue = mediaFramework;
    mediaFramework = newValue;
    firePropertyChange("mediaFramework", oldValue, newValue);
  }

  public String getMediaFramework() {
    return mediaFramework;
  }

  public void setFfmpegPercentage(Integer newValue) {
    Integer oldValue = ffmpegPercentage;
    ffmpegPercentage = newValue;
    firePropertyChange("ffmpegPercentage", oldValue, newValue);
  }

  public Integer getFfmpegPercentage() {
    return ffmpegPercentage;
  }

  public String getMediaPlayer() {
    return mediaPlayer;
  }

  public String getTheme() {
    return theme;
  }

  public void setTheme(String newValue) {
    String oldValue = this.theme;
    this.theme = newValue;
    firePropertyChange("theme", oldValue, newValue);
  }

  public void setFontSize(int newValue) {
    int oldValue = this.fontSize;
    this.fontSize = newValue;
    firePropertyChange("fontSize", oldValue, newValue);
  }

  public int getFontSize() {
    return this.fontSize;
  }

  public void setFontFamily(String newValue) {
    String oldValue = this.fontFamily;
    this.fontFamily = newValue;
    firePropertyChange("fontFamily", oldValue, newValue);
  }

  public String getFontFamily() {
    return this.fontFamily;
  }

  public void setEnableTrash(boolean newValue) {
    boolean oldValue = enableTrash;
    enableTrash = newValue;
    firePropertyChange("enableTrash", oldValue, newValue);
  }

  public boolean isEnableTrash() {
    return enableTrash;
  }

  public void setDeleteTrashOnExit(boolean newValue) {
    boolean oldValue = deleteTrashOnExit;
    deleteTrashOnExit = newValue;
    firePropertyChange("deleteTrashOnExit", oldValue, newValue);
  }

  public boolean isDeleteTrashOnExit() {
    return deleteTrashOnExit;
  }

  public void setStoreWindowPreferences(boolean newValue) {
    boolean oldValue = storeWindowPreferences;
    storeWindowPreferences = newValue;
    firePropertyChange("storeWindowPreferences", oldValue, newValue);
  }

  public boolean isStoreWindowPreferences() {
    return storeWindowPreferences;
  }

  public boolean isImageChooserUseEntityFolder() {
    return imageChooserUseEntityFolder;
  }

  public void setImageChooserUseEntityFolder(boolean newValue) {
    boolean oldvalue = imageChooserUseEntityFolder;
    imageChooserUseEntityFolder = newValue;
    firePropertyChange("imageChooserUseEntityFolder", oldvalue, newValue);
  }

  public boolean isShowMemory() {
    return showMemory;
  }

  public void setShowMemory(boolean newValue) {
    boolean oldValue = this.showMemory;
    this.showMemory = newValue;
    firePropertyChange("showMemory", oldValue, newValue);
  }

  /**
   * should we ignore SSL problems?
   *
   * @return
   */
  public boolean isIgnoreSSLProblems() {
    return ignoreSSLProblems;
  }

  public DateField getDateField() {
    return dateField;
  }

  public void setDateField(DateField newValue) {
    DateField oldValue = this.dateField;
    this.dateField = newValue;
    firePropertyChange("dateField", oldValue, newValue);
  }

  public boolean isFileSizeDisplayHumanReadable() {
    return fileSizeDisplayHumanReadable;
  }

  public void setFileSizeDisplayHumanReadable(boolean newValue) {
    boolean oldValue = this.fileSizeDisplayHumanReadable;
    fileSizeDisplayHumanReadable = newValue;
    firePropertyChange("fileSizeDisplayHumanReadable", oldValue, newValue);
  }

  /**
   * should we ignore SSL problems?
   *
   * @param ignoreSSLProblems
   */
  public void setIgnoreSSLProblems(boolean ignoreSSLProblems) {
    boolean old = this.ignoreSSLProblems;
    this.ignoreSSLProblems = ignoreSSLProblems;
    firePropertyChange("ignoreSSLProblems", old, ignoreSSLProblems);

    System.setProperty("tmm.trustallcerts", Boolean.toString(ignoreSSLProblems));

    // and pass this setting to the HTTP client if it has been changed
    if (old != ignoreSSLProblems) {
      TmmHttpClient.recreateHttpClient();
    }
  }

  /**
   * get the max. amount to download threads
   * 
   * @return the amount of download threads
   */
  public int getMaximumDownloadThreads() {
    return Math.max(maximumDownloadThreads, 1);
  }

  /**
   * set the maximum amount of download threads
   * 
   * @param newValue
   *          the maximum amount of download threads
   */
  public void setMaximumDownloadThreads(int newValue) {
    int oldValue = this.maximumDownloadThreads;
    this.maximumDownloadThreads = newValue;
    firePropertyChange("maximumDownloadThreads", oldValue, newValue);
  }

  /**
   * should we write mediainfo.xml after reading mediainfo?
   * 
   * @return true/false
   */
  public boolean isWriteMediaInfoXml() {
    return writeMediaInfoXml;
  }

  /**
   * should we write mediainfo.xml after reading mediainfo?
   * 
   * @param newValue
   *          true/false
   */
  public void setWriteMediaInfoXml(boolean newValue) {
    boolean oldValue = this.writeMediaInfoXml;
    this.writeMediaInfoXml = newValue;
    firePropertyChange("writeMediaInfoXml", oldValue, newValue);
  }

  // aspect ratio detector
  public void setArdEnabled(boolean newValue) {
    boolean oldValue = this.ardEnabled;
    this.ardEnabled = newValue;
    firePropertyChange("ardAfterScrape", oldValue, newValue);
  }

  public boolean isArdEnabled() {
    return this.ardEnabled;
  }

  public void setArdMode(ArdSettings.Mode newValue) {
    ArdSettings.Mode oldValue = this.ardMode;
    this.ardMode = newValue;
    firePropertyChange("ardMode", oldValue, newValue);
  }

  public ArdSettings.Mode getArdMode() {
    return this.ardMode;
  }

  public void setArdIgnoreBeginningPct(float newValue) {
    float oldValue = this.ardIgnoreBeginningPct;
    this.ardIgnoreBeginningPct = newValue;
    firePropertyChange("ardIgnoreBeginningPct", oldValue, newValue);
  }

  public float getArdIgnoreBeginningPct() {
    return this.ardIgnoreBeginningPct;
  }

  public void setArdIgnoreEndPct(float newValue) {
    float oldValue = this.ardIgnoreEndPct;
    this.ardIgnoreEndPct = newValue;
    firePropertyChange("ardIgnoreEndPct", oldValue, newValue);
  }

  public float getArdIgnoreEndPct() {
    return this.ardIgnoreEndPct;
  }

  public void setArdPlausiWidthPct(float newValue) {
    float oldValue = this.ardPlausiWidthPct;
    this.ardPlausiWidthPct = newValue;
    firePropertyChange("ardPlausiWidthPct", oldValue, newValue);
  }

  public float getArdPlausiWidthPct() {
    return this.ardPlausiWidthPct;
  }

  public void setArdPlausiHeightPct(float newValue) {
    float oldValue = this.ardPlausiHeightPct;
    this.ardPlausiHeightPct = newValue;
    firePropertyChange("ardPlausiHeightPct", oldValue, newValue);
  }

  public float getArdPlausiHeightPct() {
    return this.ardPlausiHeightPct;
  }

  public void setArdPlausiWidthDeltaPct(float newValue) {
    float oldValue = this.ardPlausiWidthDeltaPct;
    this.ardPlausiWidthDeltaPct = newValue;
    firePropertyChange("ardPlausiWidthDeltaPct", oldValue, newValue);
  }

  public float getArdPlausiWidthDeltaPct() {
    return this.ardPlausiWidthDeltaPct;
  }

  public void setArdPlausiHeightDeltaPct(float newValue) {
    float oldValue = this.ardPlausiHeightDeltaPct;
    this.ardPlausiHeightDeltaPct = newValue;
    firePropertyChange("ardPlausiHeightDeltaPct", oldValue, newValue);
  }

  public float getArdPlausiHeightDeltaPct() {
    return this.ardPlausiHeightDeltaPct;
  }

  public void setArdSecondaryDelta(float newValue) {
    float oldValue = this.ardSecondaryDelta;
    this.ardSecondaryDelta = newValue;
    firePropertyChange("ardSecondaryDelta", oldValue, newValue);
  }

  public float getArdSecondaryDelta() {
    return this.ardSecondaryDelta;
  }

  public void setArdRoundUp(boolean newValue) {
    boolean oldValue = this.ardRoundUp;
    this.ardRoundUp = newValue;
    firePropertyChange("ardRoundUp", oldValue, newValue);
  }

  public boolean isArdRoundUp() {
    return this.ardRoundUp;
  }

  public void setArdRoundUpThresholdPct(float newValue) {
    float oldValue = this.ardRoundUpThresholdPct;
    this.ardRoundUpThresholdPct = newValue;
    firePropertyChange("ardRoundUpThresholdPct", oldValue, newValue);
  }

  public float getArdRoundUpThresholdPct() {
    return this.ardRoundUpThresholdPct;
  }

  public void addCustomAspectRatios(String aspectRatio) {
    try {
      if (!customAspectRatios.contains(aspectRatio)) {
        customAspectRatios.add(aspectRatio);
        firePropertyChange(CUSTOM_ASPECT_RATIOS, null, customAspectRatios);
      }
    }
    catch (Exception ex) {
    }
  }

  public void removeCustomAspectRatios(String aspectRatio) {
    customAspectRatios.remove(aspectRatio);
    firePropertyChange(CUSTOM_ASPECT_RATIOS, null, customAspectRatios);
  }

  public void setCustomAspectRatios(List<String> newValues) {
    customAspectRatios.clear();
    customAspectRatios.addAll(newValues);
    firePropertyChange(CUSTOM_ASPECT_RATIOS, null, customAspectRatios);
  }

  public List<String> getCustomAspectRatios() {
    return customAspectRatios;
  }

  public void setArdMFMode(int newValue) {
    int oldValue = this.ardMFMode;
    this.ardMFMode = newValue;
    firePropertyChange("ardMFMode", oldValue, newValue);
  }

  public int getArdMFMode() {
    return this.ardMFMode;
  }

  public void setArdMFThresholdPct(float newValue) {
    float oldValue = this.ardMFThresholdPct;
    this.ardMFThresholdPct = newValue;
    firePropertyChange("ardMFThresholdPct", oldValue, newValue);
  }

  public float getArdMFThresholdPct() {
    return this.ardMFThresholdPct;
  }

  public void setArdDarkLevelPct(float newValue) {
    float oldValue = this.ardDarkLevelPct;
    this.ardDarkLevelPct = newValue;
    firePropertyChange("ardDarkLevelPct", oldValue, newValue);
  }

  public float getArdDarkLevelPct() {
    return this.ardDarkLevelPct;
  }

  public void setArdDarkLevelMaxPct(float newValue) {
    float oldValue = this.ardDarkLevelMaxPct;
    this.ardDarkLevelMaxPct = newValue;
    firePropertyChange("ardDarkLevelMaxPct", oldValue, newValue);
  }

  public float getArdDarkLevelMaxPct() {
    return this.ardDarkLevelMaxPct;
  }

  public Map<ArdSettings.Mode, ArdSettings.SampleSetting> getArdSampleSettings() {
    return ardSampleSettings;
  }

  public ArdSettings.SampleSetting getArdSampleSetting(ArdSettings.Mode mode) {
    return ardSampleSettings.get(mode);
  }

  public boolean isEnableHttpServer() {
    return enableHttpServer;
  }

  public void setEnableHttpServer(boolean newValue) {
    boolean oldValue = this.enableHttpServer;
    this.enableHttpServer = newValue;
    firePropertyChange("enableHttpServer", oldValue, newValue);
  }

  public int getHttpServerPort() {
    return httpServerPort;
  }

  public void setHttpServerPort(int newValue) {
    int oldValue = this.httpServerPort;
    this.httpServerPort = newValue;
    firePropertyChange("httpServerPort", oldValue, newValue);
  }

  public void setHttpServerPort(String port) {
    try {
      setHttpServerPort(Integer.parseInt(port));
    }
    catch (Exception ingored) {
      // just ignore
    }
  }

  public String getHttpApiKey() {
    return httpApiKey;
  }

  public void setHttpApiKey(String newValue) {
    String oldValue = this.httpApiKey;
    this.httpApiKey = newValue;
    firePropertyChange("httpApiKey", oldValue, newValue);
  }

  private void setHttpApi() {
    try {
      TmmHttpServer.getInstance().updateConfiguration(enableHttpServer, httpServerPort, httpApiKey);
    }
    catch (Exception e) {
      LOGGER.error("could not update HTTP server configuration - '{}'", e.getMessage());
    }
  }

  public boolean isEnableAutomaticUpdate() {
    return enableAutomaticUpdate;
  }

  public void setEnableAutomaticUpdate(boolean newValue) {
    boolean oldValue = this.enableAutomaticUpdate;
    this.enableAutomaticUpdate = newValue;
    firePropertyChange("enableAutomaticUpdate", oldValue, newValue);
  }

  public int getAutomaticUpdateInterval() {
    return automaticUpdateInterval;
  }

  public void setAutomaticUpdateInterval(int newValue) {
    int oldValue = this.automaticUpdateInterval;
    this.automaticUpdateInterval = newValue;
    firePropertyChange("automaticUpdateInterval", oldValue, newValue);
  }

  /**
   * gets saved Kodi HTTP port, or default
   *
   * @return the Kodi HTTP port
   */
  public String getMdbListApiKey() {
    return mdbListApiKey;
  }

  public void setMdbListApiKey(String mdbListApiKey) {
    String oldValue = this.mdbListApiKey;
    this.mdbListApiKey = mdbListApiKey;
    firePropertyChange("mdbListApiKey", oldValue, mdbListApiKey);
  }
}
