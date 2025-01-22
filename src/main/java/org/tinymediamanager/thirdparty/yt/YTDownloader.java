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
package org.tinymediamanager.thirdparty.yt;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.tinymediamanager.license.TmmFeature;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.http.Url;

import com.github.kiulian.downloader.Config;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.Downloader;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoStreamDownload;
import com.github.kiulian.downloader.downloader.request.RequestWebpage;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.downloader.response.ResponseImpl;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * the class {@link YTDownloader} is used to pass the YT download over our HTTP client
 * 
 * @author Manuel Laggner
 */
public class YTDownloader extends YoutubeDownloader implements TmmFeature {
  public YTDownloader() throws ScrapeException {
    super(new Config.Builder().build());
    setDownloader(new DownloaderImpl());
  }

  class DownloaderImpl implements Downloader {
    @Override
    public Response<String> downloadWebpage(RequestWebpage requestWebpage) {
      if ("POST".equals(requestWebpage.getMethod())) {
        return post(requestWebpage);
      }
      else {
        return get(requestWebpage);
      }
    }

    private Response<String> get(RequestWebpage requestWebpage) {
      try {
        Url url = new Url(requestWebpage.getDownloadUrl().replace("{API_KEY}", getApiKey()));

        if (requestWebpage.getHeaders() != null) {
          for (Map.Entry<String, String> entry : requestWebpage.getHeaders().entrySet()) {
            url.addHeader(entry.getKey(), entry.getValue());
          }
        }

        String body;
        if (requestWebpage.getMaxRetries() != null) {
          body = new String(url.getBytesWithRetry(requestWebpage.getMaxRetries()), StandardCharsets.UTF_8);
        }
        else {
          body = new String(url.getBytes(), StandardCharsets.UTF_8);
        }
        return ResponseImpl.from(body);
      }
      catch (Exception e) {
        return ResponseImpl.error(e);
      }
    }

    private Response<String> post(RequestWebpage requestWebpage) {
      Call call = null;
      okhttp3.Response response = null;

      try {
        RequestBody body = RequestBody.create(requestWebpage.getBody(), MediaType.parse("application/json"));
        Request.Builder builder = new Request.Builder().url(requestWebpage.getDownloadUrl().replace("{API_KEY}", getApiKey())).post(body);

        if (requestWebpage.getHeaders() != null) {
          for (Map.Entry<String, String> entry : requestWebpage.getHeaders().entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
          }
        }

        Request request = builder.build();

        call = TmmHttpClient.getHttpClient().newCall(request);
        response = call.execute();
        int responseCode = response.code();
        String responseMessage = response.message();

        // log any "connection problems"
        if (responseCode < 200 || responseCode >= 400) {
          throw new HttpException(requestWebpage.getBody(), responseCode, responseMessage);
        }

        return ResponseImpl.from(response.body().string());
      }
      catch (Exception e) {
        if (call != null) {
          call.cancel();
        }
        if (response != null) {
          response.close();
        }

        return ResponseImpl.error(e);
      }
    }

    @Override
    public Response<File> downloadVideoAsFile(RequestVideoFileDownload requestVideoFileDownload) {
      // we handle that internally
      return null;
    }

    @Override
    public Response<Void> downloadVideoAsStream(RequestVideoStreamDownload requestVideoStreamDownload) {
      // we handle that internally
      return null;
    }
  }
}
