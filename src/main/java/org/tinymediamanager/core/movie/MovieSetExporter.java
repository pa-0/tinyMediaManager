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
package org.tinymediamanager.core.movie;

import static org.tinymediamanager.core.movie.MovieSettings.DEFAULT_RENAMER_FILE_PATTERN;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ImageUtils;
import org.tinymediamanager.core.MediaEntityExporter;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;

import com.floreysoft.jmte.NamedRenderer;
import com.floreysoft.jmte.RenderFormatInfo;

/**
 * This class exports a list of movie sets to various formats according to templates.
 * 
 * @author Myron Boyle / Manuel Laggner
 */
public class MovieSetExporter extends MediaEntityExporter {
  private static final Logger LOGGER = LoggerFactory.getLogger(MovieSetExporter.class);

  public MovieSetExporter(Path pathToTemplate) throws Exception {
    super(pathToTemplate, TemplateType.MOVIE_SET);
  }

  /**
   * exports movie list according to template file.
   * 
   * @param movieSetsToExport
   *          list of movie sets
   * @param exportDir
   *          the path to export
   * @throws Exception
   *           the exception
   */
  @Override
  public <T extends MediaEntity> void export(List<T> movieSetsToExport, Path exportDir) throws Exception {
    LOGGER.info("preparing movie set export; using {}", properties.getProperty("name"));

    if (cancel) {
      return;
    }

    // register specific renderers
    engine.registerNamedRenderer(new MovieFilenameRenderer());
    engine.registerNamedRenderer(new MovieArtworkCopyRenderer(exportDir));

    // register default renderers
    registerDefaultRenderers();

    // prepare export destination
    if (!Files.exists(exportDir)) {
      Files.createDirectories(exportDir);
    }

    // prepare listfile
    Path listExportFile = exportDir.resolve("moviesets." + fileExtension);

    // load movie template
    String movieTemplateFile = properties.getProperty("movie");
    String movieTemplate = "";
    if (StringUtils.isNotBlank(movieTemplateFile)) {
      movieTemplate = Utils.readFileToString(templateDir.resolve(movieTemplateFile));
    }

    // create the list
    LOGGER.info("generating movie set list");
    Utils.deleteFileSafely(listExportFile);

    Map<String, Object> root = new HashMap<>();
    root.put("movieSets", new ArrayList<>(movieSetsToExport));

    String output = engine.transform(listTemplate, root);
    Utils.writeStringToFile(listExportFile, output);
    LOGGER.info("movie set list generated: {}", listExportFile);

    if (StringUtils.isNotBlank(detailTemplate)) {
      for (T me : movieSetsToExport) {
        if (cancel) {
          return;
        }

        MovieSet movieSet = (MovieSet) me;

        // create a movie set
        Path movieSetDir = exportDir.resolve(getFilename(movieSet));
        try {
          Files.createDirectory(movieSetDir);
        }
        catch (FileAlreadyExistsException e) {
          LOGGER.debug("Folder already exists...");
        }

        Path detailsExportFile = movieSetDir.resolve("movieset." + fileExtension);
        root = new HashMap<>();
        root.put("movieSet", movieSet);

        output = engine.transform(detailTemplate, root);
        Utils.writeStringToFile(detailsExportFile, output);

        if (StringUtils.isNotBlank(movieTemplate)) {
          for (Movie movie : movieSet.getMovies()) {
            if (cancel) {
              return;
            }

            List<MediaFile> mfs = movie.getMediaFiles(MediaFileType.VIDEO);
            if (!mfs.isEmpty()) {
              String movieFileName = getFilename(movie) + "." + fileExtension;
              Path episodeExportFile = movieSetDir.resolve(movieFileName);

              root = new HashMap<>();
              root.put("movie", movie);

              output = engine.transform(movieTemplate, root);
              Utils.writeStringToFile(episodeExportFile, output);
            }
          }
        }
      }
    }

    if (cancel) {
      return;
    }

    // copy all non .jtme/template.conf files to destination dir
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(templateDir)) {
      for (Path path : directoryStream) {
        if (Utils.isRegularFile(path)) {
          if (path.getFileName().toString().endsWith(".jmte") || path.getFileName().toString().endsWith("template.conf")) {
            continue;
          }
          Files.copy(path, exportDir.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        else if (Files.isDirectory(path)) {
          Utils.copyDirectoryRecursive(path, exportDir.resolve(path.getFileName()));
        }
      }
    }
    catch (IOException ex) {
      LOGGER.error("could not copy resources: ", ex);
    }
  }

  private static String getFilename(MediaEntity entity) {
    if (entity instanceof MovieSet movieSet) {
      return movieSet.getTitle();
    }
    if (entity instanceof Movie movie) {
      return getMovieFilename(movie);
    }
    return "";
  }

  private static String getMovieFilename(Movie movie) {
    String filename = MovieRenamer.createDestinationForFilename(MovieModuleManager.getInstance().getSettings().getRenamerFilename(), movie);
    if (StringUtils.isNotBlank(filename)) {
      return filename;
    }

    // fallback (no renamer settings)
    filename = MovieRenamer.createDestinationForFilename(DEFAULT_RENAMER_FILE_PATTERN, movie);
    if (StringUtils.isNotBlank(filename)) {
      return filename;
    }

    // fallback (should not happen, but could)
    return movie.getDbId().toString();
  }

  /*******************************************************************************
   * helper classes
   *******************************************************************************/
  private static class MovieFilenameRenderer implements NamedRenderer {
    @Override
    public RenderFormatInfo getFormatInfo() {
      return null;
    }

    @Override
    public String getName() {
      return "filename";
    }

    @Override
    public Class<?>[] getSupportedClasses() {
      return new Class[] { Movie.class };
    }

    @Override
    public String render(Object o, String pattern, Locale locale, Map<String, Object> model) {
      if (o instanceof Movie movie) {
        Map<String, Object> parameters = new HashMap<>();
        if (pattern != null) {
          parameters = parseParameters(pattern);
        }

        String filename = getMovieFilename(movie);
        if (parameters.get("escape") == Boolean.TRUE) {
          try {
            filename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
          }
          catch (Exception ignored) {
          }
        }

        return filename;
      }
      return null;
    }

    /**
     * parse the parameters out of the parameters string
     *
     * @param parameters
     *          the parameters as string
     * @return a map containing all parameters
     */
    private Map<String, Object> parseParameters(String parameters) {
      Map<String, Object> parameterMap = new HashMap<>();

      String[] details = parameters.split(",");
      for (String detail : details) {
        String key = "";
        String value = "";
        try {
          String[] d = detail.split("=");
          key = d[0].strip();
          value = d[1].strip();
        }
        catch (Exception ignored) {
          // ignored
        }

        if (StringUtils.isAnyBlank(key, value)) {
          continue;
        }

        switch (key.toLowerCase(Locale.ROOT)) {
          case "escape":
            parameterMap.put(key, Boolean.parseBoolean(value));
            break;

          default:
            break;
        }
      }

      return parameterMap;
    }

  }

  /**
   * this renderer is used to copy artwork into the exported template
   *
   * @author Manuel Laggner
   */
  private static class MovieArtworkCopyRenderer extends ArtworkCopyRenderer {

    public MovieArtworkCopyRenderer(Path pathToExport) {
      super(pathToExport);
    }

    @Override
    public Class<?>[] getSupportedClasses() {
      return new Class[] { Movie.class };
    }

    @Override
    public String render(Object o, String pattern, Locale locale, Map<String, Object> model) {
      if (o instanceof Movie movie) {
        Map<String, Object> parameters = parseParameters(pattern);

        MediaFile mf = movie.getArtworkMap().get(parameters.get("type"));
        if (mf == null || !mf.isGraphic()) {
          if (StringUtils.isNotBlank((String) parameters.get("default"))) {
            return (String) parameters.get("default");
          }
          return ""; // pass an emtpy string to prevent movie.toString() gets triggered by jmte
        }

        String filename = getMovieFilename(movie) + "-" + mf.getType();

        Path imageDir;
        if (StringUtils.isNotBlank((String) parameters.get("destination"))) {
          imageDir = pathToExport.resolve((String) parameters.get("destination"));
        }
        else {
          imageDir = pathToExport;
        }
        try {
          // create the image dir
          if (!Files.exists(imageDir)) {
            Files.createDirectory(imageDir);
          }

          // we need to rescale the image; scale factor is fixed to
          if (parameters.get("thumb") == Boolean.TRUE) {
            filename += ".thumb." + FilenameUtils.getExtension(mf.getFilename());
            int width = 150;
            if (parameters.get("width") != null) {
              width = (int) parameters.get("width");
            }
            InputStream is = ImageUtils.scaleImage(mf.getFileAsPath(), width);
            Files.copy(is, imageDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
          }
          else {
            filename += "." + FilenameUtils.getExtension(mf.getFilename());
            Files.copy(mf.getFileAsPath(), imageDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
          }
        }
        catch (Exception e) {
          LOGGER.error("could not copy artwork file: ", e);
          if (StringUtils.isNotBlank((String) parameters.get("default"))) {
            return (String) parameters.get("default");
          }
          return ""; // pass an emtpy string to prevent movie.toString() gets triggered by jmte
        }

        if (parameters.get("escape") == Boolean.TRUE) {
          try {
            filename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
          }
          catch (Exception ignored) {
          }
        }

        return filename;
      }
      return ""; // pass an empty string to prevent obj.toString() gets triggered by jmte
    }
  }
}
