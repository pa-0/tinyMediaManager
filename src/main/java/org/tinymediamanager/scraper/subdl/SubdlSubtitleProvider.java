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
package org.tinymediamanager.scraper.subdl;

import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.subdl.model.SubdlModel;
import retrofit2.Response;

import java.io.IOException;

/**
 * The base class for the Subdl scraper
 * 
 * @author Wolfgang Janes
 */
abstract class SubdlSubtitleProvider implements IMediaProvider {
  public static final String    ID          = "subdl";
  protected static final String BASE_URL_DL = "https://dl.subdl.com";

  protected Controller          controller  = null;

  protected abstract String getSubId();

  protected abstract Logger getLogger();

  protected final MediaProviderInfo providerInfo;

  SubdlSubtitleProvider() {
    providerInfo = createMediaProviderInfo();
  }

  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "Subdl", "<html><h3>Subdl.com</h3><br />A subtitle scraper for Subdl.com</html>",
        SubdlSubtitleProvider.class.getResource("/org/tinymediamanager/scraper/subdl.png"));
    info.getConfig().addText("secret_key", "");
    info.getConfig().load();
    return info;
  }

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

  protected @Nullable SubdlModel processResponse(Response<SubdlModel> response) throws IOException {
    SubdlModel searchResult;

    if (!response.isSuccessful()) {
      String message = response.message();
      try (ResponseBody body = response.errorBody()) {
        message = body.string();
      }
      catch (Exception e) {
        // ignore
      }

      throw new HttpException(response.code(), message);
    }
    searchResult = response.body();
    return searchResult;
  }
}
