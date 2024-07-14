package org.tinymediamanager.scraper.thesportsdb;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.mpdbtv.MpdbMovieMetadataProvider;

abstract class SportsDbMetadataProvider implements IMediaProvider {

  private final MediaProviderInfo mediaProviderInfo;
  private static final String     ID = "thesportsdb";

  SportsDbMetadataProvider() {
    mediaProviderInfo = createMediaProviderInfo();
  }

  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "theSportsDb",
        "<html><h3>The Sports DB</h3><br />An open, crowd-sourced sports database of artwork and metadata</html>",
        MpdbMovieMetadataProvider.class.getResource("/org/tinymediamanager/scraper/thesportsdb.svg"), -10);

    info.getConfig().addText("apiKey", "3", false);
    info.getConfig().load();

    return info;
  }

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  @Override
  public String getApiKey() {
    String userApiKey = mediaProviderInfo.getUserApiKey();
    if (StringUtils.isNotBlank(userApiKey)) {
      return userApiKey;
    }
    return IMediaProvider.super.getApiKey();
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return mediaProviderInfo;
  }

  @Override
  public boolean isActive() {
    return false;
  }
}
