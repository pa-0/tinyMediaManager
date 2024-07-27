package org.tinymediamanager.scraper.subdl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;

abstract class SubdlSubtitleProvider implements IMediaProvider {

  public static final String      ID         = "subdl";
  protected Controller            controller = null;

  protected abstract String getSubId();

  protected abstract Logger getLogger();

  @Override
  public boolean isActive() {
    return isFeatureEnabled() && StringUtils.isNotBlank(controller.getSecretKey());
  }

  // thread safe initialization of the API
  protected void initAPI(String apiKey) throws ScrapeException {

    if (controller == null) {
      if (!isActive()) {
        throw new ScrapeException(new FeatureNotEnabledException(this));
      }
      controller = new Controller();
    }

    try {
      controller.setSecretKey(apiKey);
    }
    catch (Exception e) {
      throw new ScrapeException(e);
    }
  }
}
