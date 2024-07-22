package org.tinymediamanager.scraper.subdl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.SubtitleSearchAndScrapeOptions;
import org.tinymediamanager.scraper.SubtitleSearchResult;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieSubtitleProvider;
import org.tinymediamanager.scraper.subdl.model.SubdlModel;
import org.tinymediamanager.scraper.subdl.model.Subtitles;

import retrofit2.Response;

public class SubdlMovieSubtitleProvider extends SubdlSubtitleProvider implements IMovieSubtitleProvider {

  private final MediaProviderInfo providerInfo;
  private static final String     BASE_URL_DL = "https://dl.subdl.com";
  private static final Logger     LOGGER      = LoggerFactory.getLogger(SubdlMovieSubtitleProvider.class);

  public SubdlMovieSubtitleProvider() {
    providerInfo = createMediaProviderInfo();
  }

  private MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "Subdl", "<html><h3>Subdl.com</h3><br />A subtitle scraper for Subdl.com</html>",
        SubdlSubtitleProvider.class.getResource("/org/tinymediamanager/scraper/subdl.png"));
    info.getConfig().addText("secret_key", "");
    info.getConfig().load();
    return info;
  }

  @Override
  protected String getSubId() {
    return "movie_subtitle";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  @Override
  public boolean isActive() {
    return isFeatureEnabled() && StringUtils.isNotBlank(providerInfo.getConfig().getValue("secret_key"));
  }

  public List<SubtitleSearchResult> search(SubtitleSearchAndScrapeOptions options) throws ScrapeException {

    initAPI(providerInfo.getConfig().getValue("secret_key"));

    List<SubtitleSearchResult> results = new ArrayList<>();
    SubdlModel searchResult;

    // first lets try with imdbId
    try {
      searchResult = getSubdlFromImdbID(options.getImdbId(), options.getLanguage().name());
      if (searchResult == null) {
        searchResult = getSubdlFromTmdbID(options.getTmdbId(), options.getLanguage().name());
      }
    }
    catch (IOException e) {
      throw new RuntimeException();
    }

    for (Subtitles subtitles : searchResult.subtitles) {

      SubtitleSearchResult subtitleSearchResult = new SubtitleSearchResult(options.getImdbId());
      subtitleSearchResult.setReleaseName(subtitles.releaseName);
      subtitleSearchResult.setTitle(subtitles.name);
      subtitleSearchResult.setUrl(() -> BASE_URL_DL + subtitles.url);
      results.add(subtitleSearchResult);

    }

    return results;
  }

  private @Nullable SubdlModel getSubdlFromImdbID(String imdbID, String language) throws IOException {
    SubdlModel searchResult;
    Response<SubdlModel> response = controller.getResultsFromImdbId(imdbID, language.toUpperCase(Locale.ROOT));
    if (!response.isSuccessful()) {
      String message = "";
      try {
        message = response.errorBody().string();
      }
      catch (IOException e) {
        // ignore
      }
      LOGGER.warn("getSubdlFromImdbID: request was not successful: HTTP/{} - {}", response.code(), message);
      throw new HttpException(response.code(), response.message());
    }
    searchResult = response.body();
    return searchResult;
  }

  private @Nullable SubdlModel getSubdlFromTmdbID(int tmdbId, String language) throws IOException {
    SubdlModel searchResult;
    Response<SubdlModel> response = controller.getResultsFromTmdbId(tmdbId, language.toUpperCase(Locale.ROOT));
    if (!response.isSuccessful()) {
      String message = "";
      try {
        message = response.errorBody().string();
      }
      catch (IOException e) {
        // ignore
      }
      LOGGER.warn("getSubdlFromTmdbID: request was not successful: HTTP/{} - {}", response.code(), message);
      throw new HttpException(response.code(), response.message());
    }
    searchResult = response.body();
    return searchResult;
  }

}
